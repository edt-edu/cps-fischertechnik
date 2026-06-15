package io.github.mbdo.factoryscada;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import io.github.mbdo.factoryscada.mqtt.RawMqttOutboundGateway;

@SpringBootTest
class ApplicationTests {

    @MockBean
    private RawMqttOutboundGateway rawMqttOutboundGateway; // this mocks your MQTT gateway for tests

    @Test
    void contextLoads() {
        // test will pass without needing a real MQTT broker
    }
}
