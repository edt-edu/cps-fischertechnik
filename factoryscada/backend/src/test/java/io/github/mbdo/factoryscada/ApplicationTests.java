package io.github.mbdo.factoryscada;

import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.service.FactoryScadaConfigurationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import io.github.mbdo.factoryscada.mqtt.RawMqttOutboundGateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(args = "--configuration.path=classpath:test-configuration.yml")
class ApplicationTests {

    @MockBean
    private RawMqttOutboundGateway rawMqttOutboundGateway; // this mocks your MQTT gateway for tests

    @Autowired
    private FactoryScada factoryScada;

    @Autowired
    private FactoryScadaConfigurationProvider factoryScadaConfigurationProvider;

    @Test
    void contextLoads() {
        // test will pass without needing a real MQTT broker

        assertNotNull(factoryScada);
        assertNotNull(factoryScada.getFactoryScadaInstance());
    }

    @Test
    void testFactoryScadaConfiguration() {
        FactoryScadaConfiguration conf = factoryScadaConfigurationProvider.getFactoryScadaConfiguration();
        assertEquals(1, conf.controllers().size());
        assertEquals(8, conf.controllers().getFirst().machines().size());

        assertEquals(5, conf.machineNameMappings().size());
    }
}
