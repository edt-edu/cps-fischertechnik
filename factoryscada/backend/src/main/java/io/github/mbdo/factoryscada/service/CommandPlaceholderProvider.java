package io.github.mbdo.factoryscada.service;

import io.github.mbdo.factoryscada.utilities.AppEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.mbdo.factoryscada.utilities.Utilities.convertYamlToObject;

/**
 * Class in charge of loading and providing the command place holder provided by the configuration files
 */
@Service
public class CommandPlaceholderProvider {

    private final ApplicationContext applicationContext;
    private final AppEnvironment appEnvironment;
    private Map<String, Map<String, String>> cached;

    public CommandPlaceholderProvider(ApplicationContext applicationContext, AppEnvironment appEnvironment) {
        this.applicationContext = applicationContext;
        this.appEnvironment = appEnvironment;
    }

    public synchronized Map<String, Map<String, String>> getCommandPlaceholderMap() {
        if (cached != null) {
            return cached;
        }

        cached = commandPlaceholder();
        return cached;
    }

    /**
     * Converts YAML data from the specified file path into a structured map of
     * machine types and their commands with placeholders.
     *
     * @return A map where keys are machine type names and values are maps of
     * command names to placeholders.
     * Returns an empty map if the YAML data is invalid or cannot be parsed.
     */
    private Map<String, Map<String, String>> commandPlaceholder() {
        // Define the CommandPlaceholder record with nested records for MachinesType and
        // Command
        record CommandPlaceholder(List<MachinesType> machinesType) implements Serializable {
            record MachinesType(String name, List<Command> commands) {
                record Command(String name, String placeholder) {
                }
            }
        }
        // Convert the YAML file at the specified path to a CommandPlaceholder object
        CommandPlaceholder commandPlaceholder = convertYamlToObject(applicationContext,
            appEnvironment.getCommandPlaceholderConfigFile(), CommandPlaceholder.class);
        // Initialize the result map
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        // Check if the CommandPlaceholder object and its machinesType list are not null
        if (commandPlaceholder != null && commandPlaceholder.machinesType() != null) {
            // Iterate over each MachinesType in the machinesType list
            for (CommandPlaceholder.MachinesType machinesType : commandPlaceholder.machinesType()) {
                // Initialize a map to store command names and their placeholders for the
                // current MachinesType
                Map<String, String> commandMap = new LinkedHashMap<>();
                // Iterate over each Command in the commands list of the current MachinesType
                for (CommandPlaceholder.MachinesType.Command command : machinesType.commands()) {
                    // Add the command name and placeholder to the command map
                    commandMap.put(command.name(), command.placeholder());
                }
                // Add the current MachinesType name and its corresponding command map to the
                // result map
                result.put(machinesType.name(), commandMap);
            }
        }
        // Return the result map
        return result;
    }

}
