package io.github.mbdo.factoryscada;

import io.github.mbdo.factoryscada.mqtt.MqttGateway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // MQTT startup
    @Bean
    public CommandLineRunner sendMqttMessage(MqttGateway mqttGateway) {
        return args -> {
            mqttGateway.sendToMqtt("Hello MQTT from Application.java!");
            System.out.println("MQTT message sent!");
        };
    }
}
