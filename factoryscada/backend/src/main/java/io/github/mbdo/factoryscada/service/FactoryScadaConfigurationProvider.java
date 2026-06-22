package io.github.mbdo.factoryscada.service;

import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import io.github.mbdo.factoryscada.utilities.AppEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import static io.github.mbdo.factoryscada.utilities.Utilities.convertYamlToObject;

/**
 * Class in charge of loading and providing the data of the FactoryScadaConfiguration file
 */
@Service
public class FactoryScadaConfigurationProvider {


    private final ApplicationContext applicationContext;
    private final AppEnvironment appEnvironment;
    private FactoryScadaConfiguration cached;

    public FactoryScadaConfigurationProvider(ApplicationContext applicationContext, AppEnvironment appEnvironment) {
        this.applicationContext = applicationContext;
        this.appEnvironment = appEnvironment;
    }

    public synchronized FactoryScadaConfiguration getFactoryScadaConfiguration() {
        if (cached != null) {
            return cached;
        }

        cached = readYaml();
        return cached;
    }
    private FactoryScadaConfiguration readYaml() {
        return convertYamlToObject(applicationContext, appEnvironment.getConfigurationFilePath(),
            FactoryScadaConfiguration.class);
    }

}
