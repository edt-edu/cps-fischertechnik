package io.github.mbdo.factoryscada.mission.extension;

import fr.inria.mbdo.mission.extensions.ren_mission_01.GlobalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.MachineMissionBinding;
import fr.inria.mbdo.mission.extensions.ren_mission_01.MissionBindingCatalog;
import fr.inria.mbdo.mission.extensions.ren_mission_01.MissionTemplate;
import fr.inria.mbdo.mission.extensions.ren_mission_01.adapters.TestMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinemissions.TestMissionAlpha;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinemissions.TestMissionBeta;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class ExtensionMissionAutoConfig {

    @Bean
    public MissionBindingCatalog missionBindingCatalog() {
        // create adapters locally (not exposing them as beans)
        TestMachineAdapter testMachine1 = new TestMachineAdapter("TestMachine1");
        TestMachineAdapter testMachine2 = new TestMachineAdapter("TestMachine2");

        MissionTemplate missionAlpha = new MissionTemplate(
                "missionAlpha",
                "Mission that moves from the first test machine to the second one.",
                new TestMissionAlpha(testMachine1, testMachine2),
                () -> new TestMissionAlpha(testMachine1, testMachine2));

        MissionTemplate missionBeta = new MissionTemplate(
                "missionBeta",
                "Mission that reacts on the second test machine and feeds the first one.",
                new TestMissionBeta(testMachine2, testMachine1),
                () -> new TestMissionBeta(testMachine2, testMachine1));

        List<MissionTemplate> templates = List.of(missionAlpha, missionBeta);
        List<MachineMissionBinding> bindings = List.of(
                new MachineMissionBinding("TestMachine1", "TestMachineMachine", List.of("missionAlpha", "missionBeta")),
                new MachineMissionBinding("TestMachine2", "TestMachineMachine", List.of("missionBeta")));

        // Define global missions: high-level orchestrations across all machines
        List<GlobalMission> globalMissions = List.of(
                new GlobalMission(
                        "globalSequential",
                        "Orchestration: TestMachine1 runs Alpha, TestMachine2 runs Beta",
                        Map.of("TestMachine1", "missionAlpha", "TestMachine2", "missionBeta")),
                new GlobalMission(
                        "globalBetaOnly",
                        "Orchestration: Both machines run Beta",
                        Map.of("TestMachine1", "missionBeta", "TestMachine2", "missionBeta")));

        return new MissionBindingCatalog(templates, bindings, globalMissions);
    }
}
