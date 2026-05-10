package fr.inria.mbdo.mission.extensions.ren_mission_01;

import java.util.List;

public record MachineMissionBinding(
        String machineName,
        String machineType,
        List<String> missionNames) {
}