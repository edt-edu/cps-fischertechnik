package io.github.mbdo.factoryscada.service;

import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.api.MachineMissionStrategy;
import fr.inria.mbdo.mission.runtime.config.GlobalMission;
import fr.inria.mbdo.mission.runtime.config.MachineMissionBinding;
import fr.inria.mbdo.mission.runtime.config.MissionTemplate;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionDTO;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionMachineDTO;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionNodeDTO;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionOptionDTO;
import io.github.mbdo.factoryscada.frontend.dto.BetterMissionsConfigurationDTO;
import io.github.mbdo.factoryscada.frontend.dto.GlobalMissionDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionCommandResponseDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionExecutionCommandDTO;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MissionExtensionRegistry;
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

    // Active missions keyed by mission name
    private final Map<String, MissionRuntime> activeMissions = new ConcurrentHashMap<>();
    private String activeGlobalMissionName;

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
                .map(gm -> new GlobalMissionDTO(gm.name(), gm.description(), gm.missionNames()))
                .sorted(Comparator.comparing(GlobalMissionDTO::name))
                .toList();

        List<String> activeMissionNames = new ArrayList<>(activeMissions.keySet());

        return new BetterMissionsConfigurationDTO(
                "StateMachineMissions",
                missions,
                machines,
                globalMissions,
                activeGlobalMissionName,
                activeMissionNames);
    }

    public MissionCommandResponseDTO startMission(MissionExecutionCommandDTO command) {
        String missionName = command.missionName();

        // Validate mission exists
        missionExtensionRegistry.getMissionTemplate(missionName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown mission: " + missionName));

        // Stop if already running
        stopMissionByName(missionName);

        // Start the mission
        MachineMissionStrategy mission = missionExtensionRegistry.createMission(missionName);
        mission.start();
        activeMissions.put(missionName, new MissionRuntime(missionName, mission));

        return new MissionCommandResponseDTO(
                "Mission " + missionName + " started",
                command.machineName(),
                missionName,
                missionName);
    }

    public MissionCommandResponseDTO stopMission(String machineName) {
        // For backward compatibility: find mission running on this machine
        // A machine is involved in a mission if it appears in involvedMachines
        String missionName = findActiveMissionForMachine(machineName);
        if (missionName == null) {
            return new MissionCommandResponseDTO("No active mission involving machine " + machineName,
                    machineName, null, null);
        }

        stopMissionByName(missionName);
        return new MissionCommandResponseDTO(
                "Mission " + missionName + " stopped",
                machineName, missionName, null);
    }

    public MissionCommandResponseDTO stopActiveMission() {
        if (activeMissions.isEmpty()) {
            return new MissionCommandResponseDTO("No active mission to stop", null, null, null);
        }

        List<String> stopped = new ArrayList<>();
        for (Map.Entry<String, MissionRuntime> entry : new ArrayList<>(activeMissions.entrySet())) {
            entry.getValue().strategy().stop();
            stopped.add(entry.getKey());
        }
        activeMissions.clear();

        return new MissionCommandResponseDTO(
                "Stopped missions: " + String.join(", ", stopped), null, null, null);
    }

    public MissionCommandResponseDTO startGlobalMission(String globalMissionName,
            Map<String, String> machineOverrides) {
        // Stop any currently active missions
        stopActiveMission();

        GlobalMission globalMission = missionExtensionRegistry.getGlobalMissions().stream()
                .filter(gm -> gm.name().equals(globalMissionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown global mission: " + globalMissionName));

        activeGlobalMissionName = globalMissionName;

        // Start each mission in the global mission
        List<String> started = new ArrayList<>();
        for (String missionName : globalMission.missionNames()) {
            missionExtensionRegistry.getMissionTemplate(missionName)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown mission template: " + missionName));

            MachineMissionStrategy mission = missionExtensionRegistry.createMission(missionName);
            mission.start();
            activeMissions.put(missionName, new MissionRuntime(missionName, mission));
            started.add(missionName);
        }

        return new MissionCommandResponseDTO(
                "Global mission " + globalMissionName + " started: " + String.join(", ", started),
                null, null, null);
    }

    public MissionCommandResponseDTO setGlobalMissionMachineOverride(String machineName, String missionName) {
        if (activeGlobalMissionName == null) {
            throw new IllegalArgumentException("No active global mission");
        }

        // Validate mission exists
        missionExtensionRegistry.getMissionTemplate(missionName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown mission: " + missionName));

        // Stop the old mission for this machine if any
        String currentMission = findActiveMissionForMachine(machineName);
        if (currentMission != null) {
            stopMissionByName(currentMission);
        }

        // Start the new mission
        MachineMissionStrategy mission = missionExtensionRegistry.createMission(missionName);
        mission.start();
        activeMissions.put(missionName, new MissionRuntime(missionName, mission));

        return new MissionCommandResponseDTO(
                "Mission " + missionName + " started (override for " + machineName + ")",
                machineName, missionName, activeGlobalMissionName);
    }

    public MissionCommandResponseDTO stopGlobalMission() {
        activeGlobalMissionName = null;
        return stopActiveMission();
    }

    // --- Private helpers ---

    private void stopMissionByName(String missionName) {
        MissionRuntime runtime = activeMissions.remove(missionName);
        if (runtime != null) {
            runtime.strategy().stop();
        }
    }

    private String findActiveMissionForMachine(String machineName) {
        for (String missionName : activeMissions.keySet()) {
            Optional<MissionTemplate> template = missionExtensionRegistry.getMissionTemplate(missionName);
            if (template.isPresent() && template.get().involvedMachines().contains(machineName)) {
                return missionName;
            }
        }
        return null;
    }

    private BetterMissionMachineDTO toMachineDTO(MachineMissionBinding binding) {
        List<BetterMissionOptionDTO> missions = binding.missionNames().stream()
                .map(missionExtensionRegistry::getMissionTemplate)
                .flatMap(Optional::stream)
                .map(template -> new BetterMissionOptionDTO(template.name(), template.description()))
                .toList();

        // A machine's "active mission" is any running mission that involves it
        String activeMissionName = findActiveMissionForMachine(binding.machineName());

        return new BetterMissionMachineDTO(binding.machineName(), binding.machineType(), missions,
                activeMissionName);
    }

    private BetterMissionDTO toMissionDTO(MissionTemplate template) {
        MachineMissionStrategy strategy = template.previewStrategy();
        List<String> involvedMachines = template.involvedMachines() != null ? template.involvedMachines() : List.of();

        // Use the running instance if available, otherwise fall back to preview
        MissionRuntime runtime = activeMissions.get(template.name());
        MachineMissionStrategy activeStrategy = runtime != null ? runtime.strategy() : strategy;

        if (!(activeStrategy instanceof AbstractMissionStrategy missionStrategy)) {
            return new BetterMissionDTO(
                    template.name(),
                    template.description(),
                    null,
                    involvedMachines,
                    List.of());
        }

        RuntimeState initialState = missionStrategy.getRuntime().initialState();
        String activeStateName = missionStrategy.getActiveStateName();

        if (initialState == null) {
            return new BetterMissionDTO(template.name(), template.description(), activeStateName,
                    involvedMachines, List.of());
        }

        Map<String, RuntimeState> visitedStates = new LinkedHashMap<>();
        ArrayDeque<RuntimeState> queue = new ArrayDeque<>();
        queue.add(initialState);

        while (!queue.isEmpty()) {
            RuntimeState current = queue.poll();
            if (current == null || visitedStates.containsKey(current.getName())) {
                continue;
            }

            visitedStates.put(current.getName(), current);
            if (current.getTransitions() != null) {
                current.getTransitions().stream()
                        .map(RuntimeTransition::targetState)
                        .filter(Objects::nonNull)
                        .forEach(queue::add);
            }
        }

        List<BetterMissionNodeDTO> nodes = new ArrayList<>();
        for (RuntimeState state : visitedStates.values()) {
            List<String> outputs = state.getTransitions() != null
                    ? state.getTransitions().stream()
                            .map(RuntimeTransition::targetState)
                            .filter(Objects::nonNull)
                            .map(RuntimeState::getName)
                            .distinct()
                            .toList()
                    : List.of();

            String type = state.getName().equals(initialState.getName()) ? "EntryNode" : "State";
            boolean isFinal = state.getTransitions() == null || state.getTransitions().isEmpty();
            String description = isFinal ? state.getName() + " (final)" : state.getName();

            nodes.add(new BetterMissionNodeDTO(
                    state.getName(),
                    type,
                    description,
                    outputs,
                    "",
                    List.of(),
                    state.getName().equals(activeStateName)));
        }

        return new BetterMissionDTO(template.name(), template.description(), activeStateName,
                involvedMachines, nodes);
    }

    private record MissionRuntime(String missionName, MachineMissionStrategy strategy) {
    }
}
