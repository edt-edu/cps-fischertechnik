package io.github.mbdo.factoryscada.service;

import fr.inria.mbdo.mission.extensions.ren_mission_01.GlobalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.MachineMissionBinding;
import fr.inria.mbdo.mission.extensions.ren_mission_01.MissionTemplate;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.api.MachineMissionStrategy;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionDTO;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionMachineDTO;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionNodeDTO;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionOptionDTO;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionsConfigurationDTO;
import io.github.mbdo.factoryscada.frontend.dto.GlobalMissionDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionCommandResponseDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionExecutionCommandDTO;
import io.github.mbdo.factoryscada.mission.extension.MissionExtensionRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BetterMissionService {

    private final MissionExtensionRegistry missionExtensionRegistry;
    private final Map<String, MissionRuntime> activeMissionsByMachine = new ConcurrentHashMap<>();
    private String activeGlobalMissionName;
    private Map<String, String> globalMissionMachineOverrides = new ConcurrentHashMap<>();

    public BetterMissionService(MissionExtensionRegistry missionExtensionRegistry) {
        this.missionExtensionRegistry = missionExtensionRegistry;
    }

    public BetterMissionsConfigurationDTO getConfiguration() {
        List<BetterMissionDTO> missions = missionExtensionRegistry.getMissionTemplates().stream()
                .map(this::toMissionDTO)
                .sorted(Comparator.comparing(BetterMissionDTO::name))
                .toList();

        List<BetterMissionMachineDTO> machines = missionExtensionRegistry.getMachineBindings().stream()
                .map(this::toMachineDTO)
                .sorted(Comparator.comparing(BetterMissionMachineDTO::name))
                .toList();

        List<GlobalMissionDTO> globalMissions = missionExtensionRegistry.getGlobalMissions().stream()
                .map(gm -> new GlobalMissionDTO(gm.name(), gm.description(), gm.machineDefaultMissions()))
                .sorted(Comparator.comparing(GlobalMissionDTO::name))
                .toList();

        return new BetterMissionsConfigurationDTO(
                "StateMachineMissions",
                missions,
                machines,
                globalMissions,
                activeGlobalMissionName,
                new ConcurrentHashMap<>(globalMissionMachineOverrides));
    }

    public MissionCommandResponseDTO startMission(MissionExecutionCommandDTO command) {
        MachineMissionBinding binding = missionExtensionRegistry.getBindingForMachine(command.machineName())
                .orElseThrow(() -> new IllegalArgumentException("Unknown machine: " + command.machineName()));

        if (!binding.missionNames().contains(command.missionName())) {
            throw new IllegalArgumentException(
                    "Mission " + command.missionName() + " is not available for machine " + command.machineName());
        }

        stopActiveMissionIfAny(command.machineName());

        MachineMissionStrategy mission = missionExtensionRegistry.createMission(command.missionName());
        mission.start();
        activeMissionsByMachine.put(command.machineName(), new MissionRuntime(command.missionName(), mission));

        return new MissionCommandResponseDTO(
                "Mission " + command.missionName() + " started on " + command.machineName(),
                command.machineName(),
                command.missionName(),
                command.missionName());
    }

    public MissionCommandResponseDTO stopMission(String machineName) {
        MissionRuntime activeMission = activeMissionsByMachine.remove(machineName);
        if (activeMission == null) {
            return new MissionCommandResponseDTO("No active mission for machine " + machineName, machineName, null,
                    null);
        }

        activeMission.strategy().stop();
        return new MissionCommandResponseDTO(
                "Mission " + activeMission.missionName() + " stopped on " + machineName,
                machineName,
                activeMission.missionName(),
                null);
    }

    public MissionCommandResponseDTO stopActiveMission() {
        if (activeMissionsByMachine.isEmpty()) {
            return new MissionCommandResponseDTO("No active mission to stop", null, null, null);
        }

        List<String> stoppedMachines = new ArrayList<>();
        for (Map.Entry<String, MissionRuntime> entry : new ArrayList<>(activeMissionsByMachine.entrySet())) {
            entry.getValue().strategy().stop();
            stoppedMachines.add(entry.getKey());
            activeMissionsByMachine.remove(entry.getKey());
        }

        return new MissionCommandResponseDTO("Active missions stopped on " + String.join(", ", stoppedMachines), null,
                null, null);
    }

    public MissionCommandResponseDTO startGlobalMission(String globalMissionName,
            Map<String, String> machineOverrides) {
        // Stop any active global or individual missions
        stopActiveMission();

        GlobalMission globalMission = missionExtensionRegistry.getGlobalMissions().stream()
                .filter(gm -> gm.name().equals(globalMissionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown global mission: " + globalMissionName));

        // Start missions on each machine (apply overrides if provided)
        activeGlobalMissionName = globalMissionName;
        globalMissionMachineOverrides = machineOverrides != null ? new ConcurrentHashMap<>(machineOverrides)
                : new ConcurrentHashMap<>();

        List<String> startedMachines = new ArrayList<>();
        for (Map.Entry<String, String> machineDefault : globalMission.machineDefaultMissions().entrySet()) {
            String machineName = machineDefault.getKey();
            String missionName = globalMissionMachineOverrides.getOrDefault(machineName, machineDefault.getValue());

            // Validate mission is available for this machine
            MachineMissionBinding binding = missionExtensionRegistry.getBindingForMachine(machineName)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown machine: " + machineName));

            if (!binding.missionNames().contains(missionName)) {
                throw new IllegalArgumentException(
                        "Mission " + missionName + " is not available for machine " + machineName);
            }

            // Start the mission
            MachineMissionStrategy mission = missionExtensionRegistry.createMission(missionName);
            mission.start();
            activeMissionsByMachine.put(machineName, new MissionRuntime(missionName, mission));
            startedMachines.add(machineName + ":" + missionName);
        }

        return new MissionCommandResponseDTO(
                "Global mission " + globalMissionName + " started on machines: " + String.join(", ", startedMachines),
                null,
                null,
                null);
    }

    public MissionCommandResponseDTO setGlobalMissionMachineOverride(String machineName, String missionName) {
        if (activeGlobalMissionName == null) {
            throw new IllegalArgumentException("No active global mission");
        }

        // Validate mission is available for this machine
        MachineMissionBinding binding = missionExtensionRegistry.getBindingForMachine(machineName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown machine: " + machineName));

        if (!binding.missionNames().contains(missionName)) {
            throw new IllegalArgumentException(
                    "Mission " + missionName + " is not available for machine " + machineName);
        }

        // Stop current mission on this machine and start the new one
        MissionRuntime currentMission = activeMissionsByMachine.remove(machineName);
        if (currentMission != null) {
            currentMission.strategy().stop();
        }

        MachineMissionStrategy mission = missionExtensionRegistry.createMission(missionName);
        mission.start();
        activeMissionsByMachine.put(machineName, new MissionRuntime(missionName, mission));
        globalMissionMachineOverrides.put(machineName, missionName);

        return new MissionCommandResponseDTO(
                "Machine " + machineName + " mission changed to " + missionName,
                machineName,
                missionName,
                activeGlobalMissionName);
    }

    public MissionCommandResponseDTO stopGlobalMission() {
        activeGlobalMissionName = null;
        globalMissionMachineOverrides.clear();
        return stopActiveMission();
    }

    private void stopActiveMissionIfAny(String machineName) {
        MissionRuntime activeMission = activeMissionsByMachine.remove(machineName);
        if (activeMission != null) {
            activeMission.strategy().stop();
        }
    }

    private BetterMissionMachineDTO toMachineDTO(MachineMissionBinding binding) {
        List<BetterMissionOptionDTO> missions = binding.missionNames().stream()
                .map(missionExtensionRegistry::getMissionTemplate)
                .flatMap(Optional::stream)
                .map(template -> new BetterMissionOptionDTO(template.name(), template.description()))
                .toList();

        MissionRuntime activeMission = activeMissionsByMachine.get(binding.machineName());
        return new BetterMissionMachineDTO(binding.machineName(), binding.machineType(), missions,
                activeMission == null ? null : activeMission.missionName());
    }

    private BetterMissionDTO toMissionDTO(MissionTemplate template) {
        MachineMissionStrategy strategy = template.previewStrategy();
        if (!(strategy instanceof AbstractMissionStrategy missionStrategy)) {
            return new BetterMissionDTO(
                    template.name(),
                    template.description(),
                    null,
                    List.of());
        }

        RuntimeState initialState = missionStrategy.getRuntime().initialState();
        String activeStateName = missionStrategy.getActiveStateName();

        if (initialState == null) {
            return new BetterMissionDTO(template.name(), template.description(), activeStateName, List.of());
        }

        Map<String, RuntimeState> visitedStates = new LinkedHashMap<>();
        ArrayDeque<RuntimeState> queue = new ArrayDeque<>();
        queue.add(initialState);

        while (!queue.isEmpty()) {
            RuntimeState current = queue.poll();
            if (current == null || visitedStates.containsKey(current.name())) {
                continue;
            }

            visitedStates.put(current.name(), current);
            current.transitions().stream()
                    .map(transition -> transition.targetState())
                    .filter(Objects::nonNull)
                    .forEach(queue::add);
        }

        List<BetterMissionNodeDTO> nodes = new ArrayList<>();
        for (RuntimeState state : visitedStates.values()) {
            List<String> outputs = state.transitions().stream()
                    .map(transition -> transition.targetState().name())
                    .distinct()
                    .toList();

            String type = state.name().equals(initialState.name()) ? "EntryNode" : "State";
            String description = state.isFinal() ? state.name() + " (final)" : state.name();

            nodes.add(new BetterMissionNodeDTO(
                    state.name(),
                    type,
                    description,
                    outputs,
                    "",
                    List.of(),
                    state.name().equals(activeStateName)));
        }

        return new BetterMissionDTO(template.name(), template.description(), activeStateName, nodes);
    }

    private record MissionRuntime(String missionName, MachineMissionStrategy strategy) {
    }
}
