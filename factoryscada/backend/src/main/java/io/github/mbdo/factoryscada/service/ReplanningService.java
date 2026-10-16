package io.github.mbdo.factoryscada.service;

import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.config.MachineMissionBinding;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.core.dtos.CommandMessage;
import io.github.mbdo.factoryscada.core.dtos.Parameter;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MachineCommandRequestDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MachineCommandResultDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MachineRequestDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MissionChangeDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MissionSwapRequestDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.ReplanningMachineDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.ReplanningMissionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * What the replanning (the digital twin) can ask the SCADA, per machine of the mission extension: the missions it can
 * run and the state of the one it runs, replacing that mission (possibly starting the new one in a given state), and
 * sending it a command directly, to reconcile its state with the state of the mission it migrates to.
 */
@Slf4j
@Service
public class ReplanningService {

    static final long DEFAULT_COMMAND_TIMEOUT_MS = 60_000;
    private static final long IDLE_POLL_MS = 50;

    private final MissionExtensionService missionExtensionService;
    private final MachineNameMappingService machineNameMapping;
    private final Function<String, AbstractMachine> physicalMachines;
    private final LongSupplier commandIds;

    @Autowired
    public ReplanningService(MissionExtensionService missionExtensionService,
                             MachineNameMappingService machineNameMapping,
                             FactoryScada factoryScada) {
        this(missionExtensionService, machineNameMapping,
            name -> factoryScada.getFactoryScadaInstance().machines().get(name),
            () -> factoryScada.getCommandIdGenerator().generateId());
    }

    ReplanningService(MissionExtensionService missionExtensionService,
                      MachineNameMappingService machineNameMapping,
                      Function<String, AbstractMachine> physicalMachines,
                      LongSupplier commandIds) {
        this.missionExtensionService = missionExtensionService;
        this.machineNameMapping = machineNameMapping;
        this.physicalMachines = physicalMachines;
        this.commandIds = commandIds;
    }

    public List<ReplanningMachineDTO> getMachines() {
        return missionExtensionService.getMachineBindings().stream().map(this::toMachineDTO).toList();
    }

    public MissionChangeDTO swapMission(MissionSwapRequestDTO request) {
        return missionExtensionService.swapMachineMission(request.machine(), request.mission(),
            blankToNull(request.state()), request.expectedMission(), request.expectedState(),
            MissionExtensionService.ChangeOrigin.REPLANNING);
    }

    public MissionChangeDTO stopMission(MachineRequestDTO request) {
        return missionExtensionService.stopMachineMission(request.machine(), request.expectedMission(),
            request.expectedState(), MissionExtensionService.ChangeOrigin.REPLANNING);
    }

    /**
     * Sends a command to the physical machine behind a mission-extension machine, once it is idle, and waits for its
     * feedback. A stop is sent at once: the PLC interrupts the command in progress (e.g. a belt moving to a sensor
     * it never reaches), whose INTERRUPTED feedback is then not the stop's.
     *
     * @throws IllegalArgumentException if the machine or the command is unknown
     * @throws IllegalStateException    if a mission runs on the machine: it would react to the command feedback
     */
    public MachineCommandResultDTO executeCommand(MachineCommandRequestDTO request) {
        MachineMissionBinding binding = missionExtensionService.getMachineBinding(request.machine())
            .orElseThrow(() -> new IllegalArgumentException("Unknown machine: " + request.machine()));
        missionExtensionService.runningMissionOf(binding.machineName()).ifPresent(mission -> {
            throw new IllegalStateException("Mission " + mission.getName() + " runs on " + binding.machineName()
                + ": stop it before sending commands to the machine");
        });
        AbstractMachine machine = physicalMachine(binding)
            .orElseThrow(() -> new IllegalArgumentException(
                "Machine " + binding.machineName() + " has no physical machine to send commands to"));
        String method = commandMethod(machine, request.command());

        long timeoutMs = request.timeoutMs() != null && request.timeoutMs() > 0
            ? request.timeoutMs() : DEFAULT_COMMAND_TIMEOUT_MS;
        long start = System.currentTimeMillis();
        long deadline = start + timeoutMs;
        String commandId = Long.toString(commandIds.getAsLong());

        boolean stop = isStop(request.command());
        if (!stop && !waitUntilIdle(machine, deadline)) {
            return result(binding, machine, request, commandId, "BUSY", start);
        }

        CompletableFuture<String> feedback = new CompletableFuture<>();
        BiConsumer<Boolean, String> listener = (done, status) -> {
            if (status != null && !status.contains("MUST_CONTINUE") && !(stop && status.contains("INTERRUPTED"))) {
                feedback.complete(status);
            }
        };
        machine.addCommandFeedbackListener(listener);
        try {
            send(binding, machine, method, request.command(),
                request.parameters() == null ? List.of() : request.parameters(), commandId);

            String status = feedback.get(Math.max(1, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
            return result(binding, machine, request, commandId, status, start);
        } catch (TimeoutException e) {
            return result(binding, machine, request, commandId, "TIMEOUT", start);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return result(binding, machine, request, commandId, "INTERRUPTED", start);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } finally {
            machine.removeCommandFeedbackListener(listener);
        }
    }

    /**
     * Sends a stop to the physical machine of every mission-extension machine, at once and without waiting for its
     * feedback: the PLCs interrupt the commands in progress. Stop the missions first, or they react to the
     * interrupted commands' feedback.
     *
     * @return the physical machines sent a stop
     */
    public List<String> stopAllMachines() {
        List<String> stopped = new ArrayList<>();
        for (MachineMissionBinding binding : missionExtensionService.getMachineBindings()) {
            physicalMachine(binding).ifPresent(machine -> {
                try {
                    send(binding, machine, commandMethod(machine, "stop"), "stop", List.of(),
                        Long.toString(commandIds.getAsLong()));
                    stopped.add(machine.getName());
                } catch (RuntimeException e) {
                    log.error("Could not send a stop to {} ({})", machine.getName(), binding.machineName(), e);
                }
            });
        }
        return stopped;
    }

    private void send(MachineMissionBinding binding, AbstractMachine machine, String method, String command,
                      List<Parameter> parameters, String commandId) {
        CommandMessage message = new CommandMessage("COMMAND", machine.getCommandMachineType(), commandId,
            wireCommandName(command), parameters);
        log.info("Command {} {} on {} ({})", message.getName(), parameters, machine.getName(),
            binding.machineName());
        machine.executeCommand(method,
            new GenericMachineCommandDTO<>(machine.getName(), Long.toString(System.currentTimeMillis()), message));
    }

    private ReplanningMachineDTO toMachineDTO(MachineMissionBinding binding) {
        AbstractMachine machine = physicalMachine(binding).orElse(null);
        AbstractMissionStrategy running = missionExtensionService.runningMissionOf(binding.machineName()).orElse(null);
        List<ReplanningMissionDTO> missions = binding.missionNames().stream()
            .map(missionExtensionService::getAvailableMission)
            .flatMap(Optional::stream)
            .map(m -> new ReplanningMissionDTO(m.getName(), m.getDescription(), m.getStateNames(), m.isRunning(),
                m.isRunning() ? m.getActiveStateName() : null))
            .toList();
        return new ReplanningMachineDTO(
            binding.machineName(),
            machine == null ? null : machine.getName(),
            binding.machineType(),
            machine == null ? null : machine.isIdle(),
            running == null ? null : running.getName(),
            running == null ? null : running.getActiveStateName(),
            missions);
    }

    private Optional<AbstractMachine> physicalMachine(MachineMissionBinding binding) {
        String name = machineNameMapping.getMachineForLogicalName(binding.machineName());
        return Optional.ofNullable(name).map(physicalMachines);
    }

    private boolean waitUntilIdle(AbstractMachine machine, long deadline) {
        while (!machine.isIdle()) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(IDLE_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    private static MachineCommandResultDTO result(MachineMissionBinding binding, AbstractMachine machine,
                                                  MachineCommandRequestDTO request, String commandId, String status,
                                                  long start) {
        return new MachineCommandResultDTO(binding.machineName(), machine.getName(), request.command(), commandId,
            status, status.contains("DONE"), System.currentTimeMillis() - start);
    }

    /** The machine method running {@code command}: "retract_arm", "retractArm" and "RETRACT_ARM" are the same. */
    static String commandMethod(AbstractMachine machine, String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("No command given");
        }
        String wanted = normalize(command);
        return machine.getCommandNames().stream()
            .filter(name -> normalize(name).equals(wanted))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Machine " + machine.getName() + " has no command "
                + command + " (commands: " + machine.getCommandNames() + ")"));
    }

    /** The command name in the PLC payload: upper snake case ("retractArm" becomes "RETRACT_ARM"). */
    static String wireCommandName(String command) {
        return command.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }

    /** Whether the command is a stop: it interrupts the command the machine executes, instead of waiting for it. */
    static boolean isStop(String command) {
        return command != null && normalize(command).equals("stop");
    }

    private static String normalize(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
