package io.github.mbdo.factoryscada.domains.factoryscada.dtos;

import java.io.Serializable;
import java.util.List;

/**
 * Class holding the content coming from the factory yaml configuration file
 */
public record FactoryScadaConfiguration(
        String name,
        String mqttHost,
        List<ControllerConfiguration> controllers,
        List<MachineNameMapping> machineNameMappings
) implements Serializable {
    public record ControllerConfiguration(
            String name,
            String host,
            int commandPort,
            int notificationPort,
            List<MachineConfiguration> machines
    ) implements Serializable {
        public record MachineConfiguration(
                String name,
                String type
        ) implements Serializable {
        }
    }
    public record MachineNameMapping(
            String logicalName,
            String machineName
    ) implements Serializable {}
}
