package io.github.mbdo.factoryscada.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mbdo.factoryscada.core.dtos.CommandMessage;
import io.github.mbdo.factoryscada.core.dtos.Parameter;
import io.github.mbdo.factoryscada.socket.Protocol;
import io.github.mbdo.factoryscada.socket.exception.ProtocolException;
import io.github.mbdo.factoryscada.utilities.CommandIdGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
@AllArgsConstructor
public abstract class AbstractMachine {

	/**
	 * Name of the Machine (also referred to as topicName in GenericMachineCommandDTO)
	 */
    protected final String name;

	  protected final Protocol protocol;

    /**
     * List of the command names defined in the command-placeholder.yml for this machine
     */
    protected final List<String> rawCommandNames;

    protected final CommandIdGenerator commandIdGenerator;

    @Getter
    @Setter
    protected boolean idle = true;

    public AbstractMachine(Parameters parameters) {
        this.name = parameters.name;
        this.protocol = parameters.protocol;
        this.rawCommandNames = parameters.rawCommandNames;
        this.commandIdGenerator = parameters.commandIdGenerator;
    }

    /**
     * Used by {@link io.github.mbdo.factoryscada.utilities.Utilities} to map the string identifier to a concrete class.
     * Do not use in other contexts.
     */
    @Deprecated
    public static String getType() {
        throw new UnsupportedOperationException("Subclasses must implement getType");
    }

    /**
     * Gets the machine type which is used in a command payload.
     *
     * <p>Those type names are usually uppercase. E.g., for the SortingLine it would be {@code SORTING}.
     */
    public abstract String getCommandMachineType();

    protected <T extends AbstractMachine> GenericMachineCommandDTO<T> createCommandDTO(String commandName,
                                                                                       Parameter... parameters) {
        var outputId = commandIdGenerator.generateId();
        var commandMessage = new CommandMessage("COMMAND",
                                                getCommandMachineType(),
                                                Long.toString(outputId),
                                                commandName,
                                                List.of(parameters));
        var timestamp = System.currentTimeMillis();
        return new GenericMachineCommandDTO<>(getName(), Long.toString(timestamp), commandMessage);
    }

    /**
     * Executes a method on the current class instance with the given method name and parameter.
     * The method to be executed must be a public method with the specified name and parameter type.
     *
     * @param methodName the name of the method to be invoked
     * @param parameter  the parameter to be passed to the method, which must extend {@link GenericMachineCommandDTO}
     * @throws NullPointerException     if methodName or parameter is null
     * @throws IllegalArgumentException if the method with the specified name and parameter type is not found,
     *                                  or if the method cannot be accessed or invoked
     */
    public void executeCommand(@NotNull String methodName, @NotNull GenericMachineCommandDTO<? extends AbstractMachine> parameter) {
        try {
            // Retrieve the method from the current class with the specified name and parameter type
            Method method = this.getClass().getMethod(methodName, parameter.getClass());
            // Invoke the method with the provided parameter
            method.invoke(this, parameter);
        } catch (NoSuchMethodException e) {
            log.error("Method {} with parameter type {} not found in class {}.",
                    methodName, parameter.getClass().getName(), this.getClass().getName(), e);
        } catch (IllegalAccessException e) {
            log.error("Cannot access method {} in class {}.",
                    methodName, this.getClass().getName(), e);
        } catch (InvocationTargetException e) {
            log.error("Error occurred while invoking method {} in class {}.",
                    methodName, this.getClass().getName(), e);
        }
    }

    public void executeRequest(@NotNull GenericMachineStatusRequestDTO<? extends AbstractMachine> parameter) {
    	ObjectMapper mapper = new ObjectMapper();
        Protocol protocol = this.getProtocol();
        try {
            protocol.send(mapper.writeValueAsString(parameter));
            log.info("Request to controller");
        } catch (JsonProcessingException e) {
            log.error("Cannot send request to controller; Conversion error {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (ProtocolException e) {
            log.error("Communication error with controller {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of names of all the commands (methods) in the current machine class
     * that accept a single parameter of type {@link GenericMachineCommandDTO} or its subclasses.
     * This method scans all declared methods in the class, checks their parameter types,
     * and adds the method name to the list if it has exactly one parameter which is a subclass of {@link GenericMachineCommandDTO}.
     *
     * @return a list of command names in the current machine class that accept an AbstractDTO parameter
     */
    public List<String> getCommandNames() {
        List<String> commandNames = new ArrayList<>();
        Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1 && GenericMachineCommandDTO.class.isAssignableFrom(parameterTypes[0])) {
                commandNames.add(method.getName());
            }
        }
        return commandNames;
    }

    public abstract void stop();

    public record Parameters(
        String name, Protocol protocol, List<String> rawCommandNames, CommandIdGenerator commandIdGenerator
    ) {}
}
