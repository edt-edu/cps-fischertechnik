package io.github.mbdo.factoryscada;

import io.github.mbdo.factoryscada.mqtt.RawMqttOutboundGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class Application {

    private final RawMqttOutboundGateway rawMqttOutboundGateway;

    @Autowired
    public Application(RawMqttOutboundGateway rawMqttOutboundGateway) {
        this.rawMqttOutboundGateway = rawMqttOutboundGateway;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // Send "started"  on boot
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        rawMqttOutboundGateway.sendToMqtt("started", "FactoryScada/Backend/internal/backendStatus");
    }

    // Send "stopped" on shutdown
    @EventListener
    public void onShutdown(ContextClosedEvent event) {
        rawMqttOutboundGateway.sendToMqtt("stopped", "FactoryScada/Backend/internal/backendStatus");
    }
}
