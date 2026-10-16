package io.github.mbdo.factoryscada.service;

import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.api.MachineAdapter;
import fr.inria.mbdo.mission.runtime.config.FactoryMissionExtension;
import fr.inria.mbdo.mission.runtime.config.MachineMissionBinding;
import fr.inria.mbdo.mission.runtime.config.MissionExtensionConfig;
import io.github.mbdo.factoryscada.frontend.dto.*;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MissionChangeDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MissionStateMismatchException;
import io.github.mbdo.factoryscada.utilities.BoundedLogBuffer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MissionExtensionService {

    private final Map<String, FactoryMissionExtension> availableFactoryMissions;
    private final Map<String, AbstractMissionStrategy> availableMachineMissions;
    private final Map<String, AbstractMissionStrategy> activeMachineMissions = new ConcurrentHashMap<>();
    private final Map<String, BoundedLogBuffer<String>> missionLogBuffers;
    private final Map<String, MachineMissionBinding> machineBindings = new LinkedHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private String activeFactoryMissionName;

    public enum ChangeOrigin {
        OPERATOR,
        REPLANNING
    }

    private static final int MAX_MISSION_CHANGES = 50;
    private final Deque<MissionChangeEventDTO> missionChanges = new ArrayDeque<>();
    private final Map<String, String> lastChangeTimes = new ConcurrentHashMap<>();

    private record Running(String mission, String state) {
    }

    public MissionExtensionService(MissionExtensionConfig missionExtensionConfig,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;

        this.availableFactoryMissions = new LinkedHashMap<>();
        for (FactoryMissionExtension fme : missionExtensionConfig.getFactoryMissions()) {
            availableFactoryMissions.put(fme.name(), fme);
        }

        this.availableMachineMissions = new LinkedHashMap<>();
        for (AbstractMissionStrategy m : missionExtensionConfig.getMachineMissions()) {
            availableMachineMissions.put(m.getName(), m);
        }

        for (MachineMissionBinding binding : missionExtensionConfig.getMachineMissionBindings()) {
            machineBindings.put(binding.machineName(), binding);
        }

        this.missionLogBuffers = new LinkedHashMap<>();
        for (AbstractMissionStrategy mission : availableMachineMissions.values()) {
            BoundedLogBuffer<String> buf = new BoundedLogBuffer<>(100);
            missionLogBuffers.put(mission.getName(), buf);
            mission.setLogListener(entry -> {
                buf.add(entry);
                messagingTemplate.convertAndSend("/topic/mission-extension/logs", getMissionLogsDTO());
                messagingTemplate.convertAndSend("/topic/mission-extension/configuration", getConfiguration());
            });
        }
    }

    public MissionExtensionConfigurationDTO getConfiguration() {
        List<MachineMissionExtensionDTO> missions = activeMachineMissions.values().stream()
            .map(this::toMissionDTO)
            .sorted(Comparator.comparing(MachineMissionExtensionDTO::name))
            .toList();

        List<GlobalMissionDTO> globalMissions = availableFactoryMissions.values().stream()
            .map(fme -> new GlobalMissionDTO(
                fme.name(),
                fme.description(),
                fme.machineMissions().stream().map(AbstractMissionStrategy::getName).toList()))
            .sorted(Comparator.comparing(GlobalMissionDTO::name))
            .toList();

        List<MissionMachineDTO> machines = machineBindings.values().stream()
            .map(binding -> new MissionMachineDTO(
                binding.machineName(),
                binding.machineType(),
                binding.missionNames().stream()
                    .map(availableMachineMissions::get)
                    .filter(Objects::nonNull)
                    .map(m -> new MissionExtensionMachineOptionDTO(m.getName(), m.getDescription()))
                    .toList(),
                runningMissionOf(binding.machineName()).map(AbstractMissionStrategy::getName).orElse(null),
                runningMissionOf(binding.machineName()).map(AbstractMissionStrategy::getActiveStateName).orElse(null),
                binding.missionNames().isEmpty() ? null : binding.missionNames().get(0),
                lastChangeTimes.get(binding.machineName())))
            .toList();

        List<MissionChangeEventDTO> changes;
        synchronized (missionChanges) {
            changes = List.copyOf(missionChanges);
        }

        return new MissionExtensionConfigurationDTO(
            missions,
            machines,
            globalMissions,
            activeFactoryMissionName,
            new ArrayList<>(activeMachineMissions.keySet()),
            isConfigurationModified(),
            changes);
    }

    private boolean isConfigurationModified() {
        FactoryMissionExtension global = activeFactoryMissionName == null
            ? null : availableFactoryMissions.get(activeFactoryMissionName);
        if (global == null) {
            return false;
        }
        Set<String> expected = new HashSet<>();
        global.machineMissions().forEach(m -> expected.add(m.getName()));
        Set<String> running = new HashSet<>();
        availableMachineMissions.values().stream()
            .filter(AbstractMissionStrategy::isRunning)
            .forEach(m -> running.add(m.getName()));
        return !expected.equals(running);
    }

    public MissionLogsDTO getMissionLogsDTO() {
        Map<String, List<String>> snapshot = new LinkedHashMap<>();
        missionLogBuffers.forEach((name, buf) -> snapshot.put(name, new ArrayList<>(buf.snapshot())));
        return new MissionLogsDTO(snapshot);
    }

    public synchronized MissionCommandResponseDTO startGlobalMission(String globalMissionName,
                                                                     Map<String, String> machineOverrides) {
        return tracked(ChangeOrigin.OPERATOR, () -> doStartGlobalMission(globalMissionName, machineOverrides));
    }

    private MissionCommandResponseDTO doStartGlobalMission(String globalMissionName,
                                                           Map<String, String> machineOverrides) {
        doStopActiveMission();

        FactoryMissionExtension factoryMission = availableFactoryMissions.get(globalMissionName);
        if (factoryMission == null) {
            throw new IllegalArgumentException("Unknown global mission: " + globalMissionName);
        }

        activeFactoryMissionName = globalMissionName;
        List<String> started = new ArrayList<>();

        for (AbstractMissionStrategy mission : factoryMission.machineMissions()) {
            mission.start();
            activeMachineMissions.put(mission.getName(), mission);
            started.add(mission.getName());
        }

        if (machineOverrides != null) {
            machineOverrides.forEach((machineName, missionName) -> {
                doSwapMachineMission(machineName, missionName, null, null, null);
                started.add(missionName + " (override for " + machineName + ")");
            });
        }

        return new MissionCommandResponseDTO(
            "Global mission " + globalMissionName + " started: " + String.join(", ", started),
            null, null, globalMissionName);
    }

    public synchronized MissionCommandResponseDTO startMission(MissionExecutionCommandDTO command) {
        return tracked(ChangeOrigin.OPERATOR, () -> doStartMission(command));
    }

    private MissionCommandResponseDTO doStartMission(MissionExecutionCommandDTO command) {
        String missionName = command.missionName();
        AbstractMissionStrategy mission = availableMachineMissions.get(missionName);
        if (mission == null) {
            throw new IllegalArgumentException("Unknown mission: " + missionName);
        }

        if (command.machineName() != null && machineBindings.containsKey(command.machineName())) {
            // a machine runs one mission at a time: replaces the one running on it
            doSwapMachineMission(command.machineName(), missionName, null, null, null);
            return new MissionCommandResponseDTO("Mission " + missionName + " started on " + command.machineName(),
                command.machineName(), missionName, activeFactoryMissionName);
        }

        AbstractMissionStrategy existing = activeMachineMissions.remove(missionName);
        if (existing != null) {
            existing.stop();
        }

        mission.start();
        activeMachineMissions.put(missionName, mission);

        return new MissionCommandResponseDTO(
            "Mission " + missionName + " started",
            command.machineName(), missionName, activeFactoryMissionName);
    }

    public synchronized MissionCommandResponseDTO stopMission(String missionName) {
        return tracked(ChangeOrigin.OPERATOR, () -> doStopMission(missionName));
    }

    private MissionCommandResponseDTO doStopMission(String missionName) {
        AbstractMissionStrategy mission = activeMachineMissions.remove(missionName);
        if (mission == null) {
            return new MissionCommandResponseDTO(
                "No active mission: " + missionName, null, missionName, null);
        }
        mission.stop();
        return new MissionCommandResponseDTO(
            "Mission " + missionName + " stopped", null, missionName, activeFactoryMissionName);
    }

    public synchronized MissionCommandResponseDTO stopActiveMission() {
        return tracked(ChangeOrigin.OPERATOR, this::doStopActiveMission);
    }

    private MissionCommandResponseDTO doStopActiveMission() {
        List<String> stopped = new ArrayList<>();
        AbstractMissionStrategy.atomically(() -> {
            Set<AbstractMissionStrategy> toStop = new LinkedHashSet<>(activeMachineMissions.values());
            availableMachineMissions.values().stream()
                .filter(AbstractMissionStrategy::isRunning)
                .forEach(toStop::add);
            for (AbstractMissionStrategy mission : toStop) {
                mission.stop();
                stopped.add(mission.getName());
            }
            activeMachineMissions.clear();
            activeFactoryMissionName = null;
        });

        if (stopped.isEmpty()) {
            return new MissionCommandResponseDTO("No active mission to stop", null, null, null);
        }
        return new MissionCommandResponseDTO(
            "Stopped: " + String.join(", ", stopped), null, null, null);
    }

    public synchronized MissionCommandResponseDTO stopGlobalMission() {
        String name = activeFactoryMissionName;
        MissionCommandResponseDTO stopped = stopActiveMission();
        return new MissionCommandResponseDTO(
            name == null ? stopped.message() : "Global mission " + name + " stopped. " + stopped.message(),
            null, null, null);
    }

    public synchronized MissionCommandResponseDTO setGlobalMissionMachineOverride(String machineName,
                                                                                  String missionName) {
        if (activeFactoryMissionName == null) {
            throw new IllegalArgumentException("No active global mission");
        }
        swapMachineMission(machineName, missionName, null, null, null, ChangeOrigin.OPERATOR);
        return new MissionCommandResponseDTO(
            "Mission " + missionName + " started (override for " + machineName + ")",
            machineName, missionName, activeFactoryMissionName);
    }

    public Collection<MachineMissionBinding> getMachineBindings() {
        return Collections.unmodifiableCollection(machineBindings.values());
    }

    public Optional<MachineMissionBinding> getMachineBinding(String machineName) {
        return Optional.ofNullable(machineBindings.get(machineName));
    }

    public Optional<AbstractMissionStrategy> getAvailableMission(String missionName) {
        return Optional.ofNullable(availableMachineMissions.get(missionName));
    }

    public Optional<AbstractMissionStrategy> runningMissionOf(String machineName) {
        MachineMissionBinding binding = machineBindings.get(machineName);
        if (binding == null) {
            return Optional.empty();
        }
        return binding.missionNames().stream()
            .map(availableMachineMissions::get)
            .filter(Objects::nonNull)
            .filter(AbstractMissionStrategy::isRunning)
            .findFirst();
    }

    public synchronized MissionChangeDTO swapMachineMission(String machineName, String missionName, String stateName,
                                                            String expectedMission, String expectedState,
                                                            ChangeOrigin origin) {
        MissionChangeDTO change = doSwapMachineMission(machineName, missionName, stateName, expectedMission,
            expectedState);
        record(change, origin);
        publishConfiguration();
        return change;
    }

    private MissionChangeDTO doSwapMachineMission(String machineName, String missionName, String stateName,
                                                  String expectedMission, String expectedState) {
        MachineMissionBinding binding = requireBinding(machineName);
        if (!binding.missionNames().contains(missionName)) {
            throw new IllegalArgumentException("Machine " + machineName + " can't run mission " + missionName
                + " (available: " + binding.missionNames() + ")");
        }
        AbstractMissionStrategy mission = availableMachineMissions.get(missionName);
        if (mission == null) {
            throw new IllegalArgumentException("Unknown mission: " + missionName);
        }
        if (stateName != null && !mission.getStateNames().contains(stateName)) {
            throw new IllegalArgumentException("Mission " + missionName + " has no state " + stateName
                + " (states: " + mission.getStateNames() + ")");
        }

        MissionChangeDTO[] change = new MissionChangeDTO[1];
        AbstractMissionStrategy.atomically(() -> {
            AbstractMissionStrategy previous = runningMissionOf(machineName).orElse(null);
            String previousState = previous == null ? null : previous.getActiveStateName();
            checkExpected(machineName, previous, previousState, expectedMission, expectedState);

            stopMachineMissions(binding);
            if (stateName == null) {
                mission.start();
            } else {
                mission.startAt(stateName);
            }
            activeMachineMissions.put(missionName, mission);
            change[0] = new MissionChangeDTO(machineName, previous == null ? null : previous.getName(), previousState,
                missionName, mission.getActiveStateName());
        });
        return change[0];
    }

    public MissionCommandResponseDTO swapMachineMission(String machineName, String missionName, String stateName) {
        MissionChangeDTO change = swapMachineMission(machineName, missionName, stateName, null, null,
            ChangeOrigin.OPERATOR);
        return new MissionCommandResponseDTO(
            "Machine " + machineName + ": "
                + (change.previousMission() == null ? "" : change.previousMission() + " stopped, ")
                + missionName + " started" + (stateName == null ? "" : " in " + stateName),
            machineName, missionName, activeFactoryMissionName);
    }

    public synchronized MissionChangeDTO stopMachineMission(String machineName, String expectedMission,
                                                            String expectedState, ChangeOrigin origin) {
        MissionChangeDTO change = doStopMachineMission(machineName, expectedMission, expectedState);
        if (change.previousMission() != null) {
            record(change, origin);
            publishConfiguration();
        }
        return change;
    }

    private MissionChangeDTO doStopMachineMission(String machineName, String expectedMission,
                                                  String expectedState) {
        MachineMissionBinding binding = requireBinding(machineName);
        MissionChangeDTO[] change = new MissionChangeDTO[1];
        AbstractMissionStrategy.atomically(() -> {
            AbstractMissionStrategy previous = runningMissionOf(machineName).orElse(null);
            String previousState = previous == null ? null : previous.getActiveStateName();
            checkExpected(machineName, previous, previousState, expectedMission, expectedState);

            stopMachineMissions(binding);
            change[0] = new MissionChangeDTO(machineName, previous == null ? null : previous.getName(), previousState,
                null, null);
        });
        return change[0];
    }

    private <T> T tracked(ChangeOrigin origin, java.util.function.Supplier<T> operation) {
        Map<String, Running> before = runningByMachine();
        T result = operation.get();
        Map<String, Running> after = runningByMachine();
        boolean changed = false;
        for (String machine : machineBindings.keySet()) {
            Running was = before.get(machine);
            Running is = after.get(machine);
            if (!Objects.equals(was.mission(), is.mission())) {
                record(new MissionChangeDTO(machine, was.mission(), was.state(), is.mission(), is.state()), origin);
                changed = true;
            }
        }
        if (changed) {
            publishConfiguration();
        }
        return result;
    }

    private Map<String, Running> runningByMachine() {
        Map<String, Running> running = new HashMap<>();
        for (String machine : machineBindings.keySet()) {
            AbstractMissionStrategy mission = runningMissionOf(machine).orElse(null);
            running.put(machine, mission == null
                ? new Running(null, null)
                : new Running(mission.getName(), mission.getActiveStateName()));
        }
        return running;
    }

    private void record(MissionChangeDTO change, ChangeOrigin origin) {
        String time = Instant.now().toString();
        lastChangeTimes.put(change.machine(), time);
        synchronized (missionChanges) {
            missionChanges.addFirst(new MissionChangeEventDTO(time, change.machine(), change.previousMission(),
                change.previousState(), change.mission(), change.state(), origin.name()));
            while (missionChanges.size() > MAX_MISSION_CHANGES) {
                missionChanges.removeLast();
            }
        }
    }

    private void publishConfiguration() {
        messagingTemplate.convertAndSend("/topic/mission-extension/configuration", getConfiguration());
    }

    private MachineMissionBinding requireBinding(String machineName) {
        MachineMissionBinding binding = machineBindings.get(machineName);
        if (binding == null) {
            throw new IllegalArgumentException("Unknown machine: " + machineName);
        }
        return binding;
    }

    private static void checkExpected(String machineName, AbstractMissionStrategy running, String state,
                                      String expectedMission, String expectedState) {
        String runningName = running == null ? "" : running.getName();
        if (expectedMission != null && !expectedMission.equals(runningName)) {
            throw new MissionStateMismatchException("Machine " + machineName + " runs "
                + (running == null ? "no mission" : runningName) + ", not " + expectedMission);
        }
        if (expectedState != null && !expectedState.equals(state)) {
            throw new MissionStateMismatchException("Machine " + machineName + " is in state " + state
                + " of " + runningName + ", not " + expectedState);
        }
    }

    private void stopMachineMissions(MachineMissionBinding binding) {
        for (String name : binding.missionNames()) {
            AbstractMissionStrategy mission = availableMachineMissions.get(name);
            activeMachineMissions.remove(name);
            if (mission != null && mission.isRunning()) {
                mission.stop();
            }
        }
    }

    private MachineMissionExtensionDTO toMissionDTO(AbstractMissionStrategy strategy) {
        List<String> machines;
        try {
            machines = strategy.getMachines() != null
                ? strategy.getMachines().stream().map(MachineAdapter::getId).toList()
                : List.of();
        } catch (Exception e) {
            machines = List.of();
        }

        return new MachineMissionExtensionDTO(
            strategy.getName(),
            strategy.getDescription(),
            strategy.getActiveStateName(),
            strategy.isAwaitingCommandResult(),
            machines,
            strategy.getDotGraph());
    }
}
