package fr.inria.mbdo.mission.extensions.ren_mission_01;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.inria.mbdo.mission.extensions.ren_mission_01.adapters.TestMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinemissions.TestMissionAlpha;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinemissions.TestMissionBeta;
import fr.inria.mbdo.mission.runtime.api.MachineMissionStrategy;

@Configuration
public class MissionAutoConfiguration {

    private final TestMachineAdapter testMachine1 = new TestMachineAdapter("TestMachine1");
    private final TestMachineAdapter testMachine2 = new TestMachineAdapter("TestMachine2");

    @Bean
    public MachineMissionStrategy missionAlpha() {
        return new TestMissionAlpha(testMachine1, testMachine2);
    }

    @Bean
    public MachineMissionStrategy missionBeta() {
        return new TestMissionBeta(testMachine2, testMachine1);
    }
}
