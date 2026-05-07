package io.github.mbdo.factoryscada.extensions.ren_mission_01;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.mbdo.factoryscada.mission.extension.MissionSpringExtension;

@Configuration
public class SampleMissionAutoConfiguration {

	@Bean
	public MissionSpringExtension missionA() {
		return new SampleMissionA();
	}
}
