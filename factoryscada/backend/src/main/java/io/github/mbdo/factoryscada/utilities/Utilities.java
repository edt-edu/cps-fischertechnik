package io.github.mbdo.factoryscada.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

@Slf4j
@SuppressWarnings("unchecked")
public class Utilities {

    /**
     * Converts a YAML file to an object of the specified class.
     * The YAML file is read from the classpath, and the first key's value
     * in the YAML structure is converted to a JSON string, which is then
     * mapped to an object of the specified class.
     *
     * @param yamlFilePath the path to the YAML file on the classpath
     * @param objectClass  the class of the object to be returned
     * @param <T>          the type of the object to be returned
     * @return an object of the specified class, populated with the data from the YAML file
     * @throws NullPointerException if yamlFilePath or objectClass is null
     * @throws RuntimeException     if an error occurs while reading the YAML file or converting it to the object
     */
    public static <T> T convertYamlToObject(final ApplicationContext applicationContext, @NotNull String yamlFilePath, @NotNull Class<T> objectClass) {
        Resource resource = applicationContext.getResource(yamlFilePath);

        try (InputStream inputStream = resource.getInputStream()) {
            // Create an ObjectMapper for YAML
            ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
            // Read the YAML content into a JsonNode
            JsonNode jsonNode = yamlReader.readTree(inputStream);
            // Get the value of the first key from the JsonNode
            JsonNode firstKeyValue = jsonNode.fields().next().getValue();
            // Convert the JsonNode to a JSON string
            String jsonString = new ObjectMapper().writeValueAsString(firstKeyValue);
            // Convert the JSON string to an object of the specified class
            return new ObjectMapper().readValue(jsonString, objectClass);
        } catch (IOException e) {
            // Log the error and throw a RuntimeException in case of an exception
            log.error("Error converting YAML file to object: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds the concrete machine class that corresponds to the given machine type.
     *
     * @param machineType    The type of the machine to find.
     * @param domainsPackage The package to scan for machine classes.
     * @return The concrete machine class that matches the given type.
     */
    public static Class<? extends AbstractMachine> findMachineClass(String machineType, String domainsPackage) {
        try (ScanResult scanResult = new ClassGraph().enableClassInfo().acceptPackages(domainsPackage).scan()) {

            List<Class<?>> concreteSubclasses = scanResult.getSubclasses(AbstractMachine.class.getName()).loadClasses().stream().filter(cls -> !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())).toList();

            for (Class<?> aClass : concreteSubclasses) {
                try {
                    // Get the method "getType"
                    Method method = aClass.getMethod("getType");

                    // Invoke the method to get the type value
                    String value = (String) method.invoke(null);

                    if (value != null && value.equals(machineType)) {
                        return (Class<? extends AbstractMachine>) aClass;
                    }
                } catch (IllegalAccessException e) {
                    log.error("Error invoking method 'getType' in class {}: {}", aClass.getName(), e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            throw new ClassNotFoundException("Could not find class for machine type: " + machineType);

        } catch (Exception e) {
            log.error("Error during class scanning: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
