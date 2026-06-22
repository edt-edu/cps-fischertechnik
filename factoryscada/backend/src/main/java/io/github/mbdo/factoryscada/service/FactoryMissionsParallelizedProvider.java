package io.github.mbdo.factoryscada.service;

import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.FactoryMissionsParallelized_dto;
import io.github.mbdo.factoryscada.utilities.AppEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import static io.github.mbdo.factoryscada.utilities.Utilities.convertYamlToObject;

@Service
public class FactoryMissionsParallelizedProvider {

    private final ApplicationContext applicationContext;
    private final AppEnvironment appEnvironment;
    private FactoryMissionsParallelized_dto cached;

    public FactoryMissionsParallelizedProvider(ApplicationContext applicationContext, AppEnvironment appEnvironment) {
        this.applicationContext = applicationContext;
        this.appEnvironment = appEnvironment;
    }

    public synchronized FactoryMissionsParallelized_dto getMissionsParallelized() {
        if (cached != null) {
            return cached;
        }

        cached = missionsParallelized();
        return cached;
    }

    /**
     * Retrieves the missions parallelized configuration by converting a YAML file
     * located at the specified path
     * into an instance of {@link FactoryScadaConfiguration} using Jackson
     * ObjectMapper.
     *
     * @return The missions parallelizedConfiguration instance parsed from the YAML
     * file.
     * @throws RuntimeException If there is an error during YAML parsing or file
     *                          reading.
     */
    private FactoryMissionsParallelized_dto missionsParallelized() {
        return convertYamlToObject(applicationContext, appEnvironment.getMissionsConfigurationParallelizedFilePath(),
            FactoryMissionsParallelized_dto.class);
    }

}
