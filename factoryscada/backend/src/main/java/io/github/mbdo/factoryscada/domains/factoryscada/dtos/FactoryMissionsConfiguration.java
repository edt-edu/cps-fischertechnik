package io.github.mbdo.factoryscada.domains.factoryscada.dtos;

import java.io.Serializable;
import java.util.List;

/**
 * Class holding the content coming from the factory yaml configuration file
 */
public record FactoryMissionsConfiguration(
        String name,
        List<MissionConfiguration> missions
) implements Serializable {
    public record MissionConfiguration (
        String name,
        String description,
        List<Command> commands
    ) implements Serializable {}
    public record Command(
        String name, 
        String description,
        String placeholder) implements Serializable {}
}
