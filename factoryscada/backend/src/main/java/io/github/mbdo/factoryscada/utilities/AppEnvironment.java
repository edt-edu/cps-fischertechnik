package io.github.mbdo.factoryscada.utilities;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

/**
 * The {@link AppEnvironment} class provides static variables that represent
 * file paths and package names used throughout the application. This class
 * centralizes configuration settings and file paths that are used globally
 * in the application.
 */
@Component
@Getter
@Slf4j
public class AppEnvironment {

    /**
     * The file path for the command placeholder configuration file.
     * This is set to "command-placeholder.yml" by default.
     */
    private final String commandPlaceholderConfigFile = "classpath:command-placeholder.yml";
    /**
     * The package name for machine domains in the application.
     * This is set to "io.github.mbdo.factoryscada.domains" by default.
     */
    private final String machineDomainsPackageName = "io.github.mbdo.factoryscada.domains";
    /**
     * The file path for the main configuration file.
     * This is set to "configuration.yml" by default.
     */
    private String configurationFilePath = "configuration.yml";

    /**
     * The file path for the main configuration file.
     * This is set to "configuration.yml" by default.
     */
    private String missionsConfigurationFilePath = "missions-configuration.yml";


    @Autowired
    public AppEnvironment(ApplicationArguments args) {
        String argumentName = "configuration.path";
        if (args.containsOption(argumentName)) {
            configurationFilePath = "file:" + args.getOptionValues(argumentName).getFirst();
            System.out.println("--> " + configurationFilePath);
        } else {
            log.warn("No configuration file provided. The default configuration file is being used. (ie. \"classpath:configuration.yml\")");
            log.info("you can specify the configuration file using the option --configuration.path=/app/config/configuration.yml");
            configurationFilePath = "classpath:configuration.yml";
        }

        argumentName = "missions.configuration.path";
        if (args.containsOption(argumentName)) {
            missionsConfigurationFilePath = "file:" + args.getOptionValues(argumentName).getFirst();
            System.out.println("--> " + missionsConfigurationFilePath);
        } else {
            log.warn("No mission configuration file provided. The default missions configuration file is being used. (ie. \"classpath:missions-configuration.yml\")");
            log.info("you can specify the mission configuration file using the option --missions.configuration.path=/app/config/missions-configuration.yml");
            missionsConfigurationFilePath = "classpath:missions-configuration.yml";
        }
    }

}
