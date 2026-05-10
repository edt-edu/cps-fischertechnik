package io.github.mbdo.factoryscada.mission.extension;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import fr.inria.mbdo.mission.runtime.api.MachineMissionStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * Stores the MachineMissionStrategy declared in the application
 */
@Service
@Slf4j
public class MissionExtensionRegistry {
    private final List<MachineMissionStrategy> extensions;

    public MissionExtensionRegistry(List<MachineMissionStrategy> extensions) {
        this.extensions = extensions;
        log.info("Registered mission extensions: {}",
                extensions.stream().map(ext -> ext.getClass().getSimpleName()).collect(Collectors.joining(", ")));
    }

    public List<MachineMissionStrategy> getExtensions() {
        return extensions;
    }
}
