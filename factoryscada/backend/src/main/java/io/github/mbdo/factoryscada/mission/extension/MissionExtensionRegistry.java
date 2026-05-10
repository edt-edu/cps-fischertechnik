package io.github.mbdo.factoryscada.mission.extension;

import fr.inria.mbdo.mission.extensions.ren_mission_01.GlobalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.MachineMissionBinding;
import fr.inria.mbdo.mission.extensions.ren_mission_01.MissionBindingCatalog;
import fr.inria.mbdo.mission.extensions.ren_mission_01.MissionTemplate;
import fr.inria.mbdo.mission.runtime.api.MachineMissionStrategy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Stores the MachineMissionStrategy declared in the application
 */
@Service
@Slf4j
public class MissionExtensionRegistry {
    private final MissionBindingCatalog catalog;
    private final Map<String, MissionTemplate> missionTemplatesByName;

    public MissionExtensionRegistry(MissionBindingCatalog catalog) {
        this.catalog = catalog;
        this.missionTemplatesByName = catalog.missionTemplates().stream()
                .collect(Collectors.toMap(MissionTemplate::name, template -> template, (left, right) -> left,
                        LinkedHashMap::new));
        log.info("Registered mission extensions: {}",
                catalog.missionTemplates().stream().map(MissionTemplate::name).collect(Collectors.joining(", ")));
    }

    public List<MissionTemplate> getMissionTemplates() {
        return catalog.missionTemplates();
    }

    public List<MachineMissionBinding> getMachineBindings() {
        return catalog.machineBindings();
    }

    public List<GlobalMission> getGlobalMissions() {
        return catalog.globalMissions();
    }

    public Optional<MissionTemplate> getMissionTemplate(String missionName) {
        return Optional.ofNullable(missionTemplatesByName.get(missionName));
    }

    public Optional<MachineMissionBinding> getBindingForMachine(String machineName) {
        return catalog.machineBindings().stream()
                .filter(binding -> binding.machineName().equals(machineName))
                .findFirst();
    }

    public MachineMissionStrategy createMission(String missionName) {
        return getMissionTemplate(missionName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown mission template: " + missionName))
                .factory()
                .get();
    }
}
