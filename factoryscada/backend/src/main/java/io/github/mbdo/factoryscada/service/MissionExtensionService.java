package io.github.mbdo.factoryscada.service;

import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.api.MachineAdapter;
import fr.inria.mbdo.mission.runtime.config.FactoryMissionExtension;
import fr.inria.mbdo.mission.runtime.config.MissionExtensionConfig;
import io.github.mbdo.factoryscada.frontend.dto.*;
import io.github.mbdo.factoryscada.utilities.BoundedLogBuffer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MissionExtensionService {

    private final Map<String, FactoryMissionExtension> availableFactoryMissions;
    private final Map<String, AbstractMissionStrategy> availableMachineMissions;
    private final Map<String, AbstractMissionStrategy> activeMachineMissions = new ConcurrentHashMap<>();
    private final Map<String, BoundedLogBuffer<String>> missionLogBuffers;
    private final SimpMessagingTemplate messagingTemplate;
    private String activeFactoryMissionName;

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

        return new MissionExtensionConfigurationDTO(
            missions,
            List.of(),
            globalMissions,
            activeFactoryMissionName,
            new ArrayList<>(activeMachineMissions.keySet()));
    }

    public MissionLogsDTO getMissionLogsDTO() {
        Map<String, List<String>> snapshot = new LinkedHashMap<>();
        missionLogBuffers.forEach((name, buf) -> snapshot.put(name, new ArrayList<>(buf.snapshot())));
        return new MissionLogsDTO(snapshot);
    }

    public MissionCommandResponseDTO startGlobalMission(String globalMissionName,
                                                        Map<String, String> machineOverrides) {
        stopActiveMission();

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

        return new MissionCommandResponseDTO(
            "Global mission " + globalMissionName + " started: " + String.join(", ", started),
            null, null, globalMissionName);
    }

    public MissionCommandResponseDTO startMission(MissionExecutionCommandDTO command) {
        String missionName = command.missionName();
        AbstractMissionStrategy mission = availableMachineMissions.get(missionName);
        if (mission == null) {
            throw new IllegalArgumentException("Unknown mission: " + missionName);
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

    public MissionCommandResponseDTO stopMission(String missionName) {
        AbstractMissionStrategy mission = activeMachineMissions.remove(missionName);
        if (mission == null) {
            return new MissionCommandResponseDTO(
                "No active mission: " + missionName, null, missionName, null);
        }
        mission.stop();
        return new MissionCommandResponseDTO(
            "Mission " + missionName + " stopped", null, missionName, activeFactoryMissionName);
    }

    public MissionCommandResponseDTO stopActiveMission() {
        if (activeMachineMissions.isEmpty()) {
            return new MissionCommandResponseDTO("No active mission to stop", null, null, null);
        }

        List<String> stopped = new ArrayList<>(activeMachineMissions.keySet());
        activeMachineMissions.values().forEach(AbstractMissionStrategy::stop);
        activeMachineMissions.clear();
        activeFactoryMissionName = null;

        return new MissionCommandResponseDTO(
            "Stopped: " + String.join(", ", stopped), null, null, null);
    }

    public MissionCommandResponseDTO stopGlobalMission() {
        String name = activeFactoryMissionName;
        activeFactoryMissionName = null;
        stopActiveMission();
        return new MissionCommandResponseDTO(
            "Global mission " + name + " stopped", null, null, null);
    }

    public MissionCommandResponseDTO setGlobalMissionMachineOverride(String machineName, String missionName) {
        if (activeFactoryMissionName == null) {
            throw new IllegalArgumentException("No active global mission");
        }
        AbstractMissionStrategy mission = availableMachineMissions.get(missionName);
        if (mission == null) {
            throw new IllegalArgumentException("Unknown mission: " + missionName);
        }

        AbstractMissionStrategy current = activeMachineMissions.remove(missionName);
        if (current != null) {
            current.stop();
        }

        mission.start();
        activeMachineMissions.put(missionName, mission);

        return new MissionCommandResponseDTO(
            "Mission " + missionName + " started (override for " + machineName + ")",
            machineName, missionName, activeFactoryMissionName);
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
            machines,
            strategy.getDotGraph());
    }
}
