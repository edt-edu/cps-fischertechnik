package io.github.mbdo.factoryscada;

import io.github.mbdo.factoryscada.mqtt.MqttPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@ComponentScan(basePackages = {
        "io.github.mbdo.factoryscada",
        "fr.inria.mbdo.mission.extensions.ren_mission_01_impl"
})
public class Application {

    private final MqttPublisherService mqttPublisher;

    @Autowired
    public Application(MqttPublisherService mqttPublisher) {
        this.mqttPublisher = mqttPublisher;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // Send "started" on boot
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        mqttPublisher.publish("internal/backendStatus", "started");
    }

    // Send "stopped" on shutdown
    @EventListener
    public void onShutdown(ContextClosedEvent event) {
        mqttPublisher.publish("internal/backendStatus", "stopped");
    }
}
