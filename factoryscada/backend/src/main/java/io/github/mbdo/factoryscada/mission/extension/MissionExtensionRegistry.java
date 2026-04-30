package io.github.mbdo.factoryscada.mission.extension;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


/**
 * Stores the MissionSpringExtension declared in the application
 */
@Service
@Slf4j
public class MissionExtensionRegistry {
	
	private final List<MissionSpringExtension> extensions;
	
	public MissionExtensionRegistry(List<MissionSpringExtension> extensions) {
		log.info("Discovered {} MissionSpringExtension(s): ",extensions.size(), extensions.stream().map(e -> e.getName()).collect(Collectors.joining(", ")));
		this.extensions = extensions;
	}

	public List<MissionSpringExtension> getExtensions() {
		return extensions;
	}
}
