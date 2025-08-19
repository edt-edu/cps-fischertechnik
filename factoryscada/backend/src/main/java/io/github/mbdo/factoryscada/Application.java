package io.github.mbdo.factoryscada;

import io.github.mbdo.factoryscada.mqtt.MqttGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class Application {

    private final MqttGateway mqttGateway;

    @Autowired
    public Application(MqttGateway mqttGateway) {
        this.mqttGateway = mqttGateway;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // Send "started" on boot
    @Bean
    public CommandLineRunner sendMqttMessage(MqttGateway mqttGateway) {
        return args -> mqttGateway.sendToMqtt("started", "FactoryScada/Backend/internal/backendStatus");
    }

    // Send "stopped" on shutdown
    @EventListener
    public void onShutdown(ContextClosedEvent event) {
        mqttGateway.sendToMqtt("stopped", "FactoryScada/Backend/internal/backendStatus");
    }
}
