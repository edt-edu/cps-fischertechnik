package fr.inria.mbdo.mission.extensions.ren_mission_01;

import java.util.List;

public record MissionBindingCatalog(
        List<MissionTemplate> missionTemplates,
        List<MachineMissionBinding> machineBindings,
        List<GlobalMission> globalMissions) {
}
