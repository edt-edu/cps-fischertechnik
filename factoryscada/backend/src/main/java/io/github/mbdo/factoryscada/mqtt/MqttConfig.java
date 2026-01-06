package io.github.mbdo.factoryscada.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageChannel;

@Configuration
public class MqttConfig {

    @Value("${configuration.mqttHost:tcp://localhost:1883}")
    String mqttHost;

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MqttPahoMessageHandler mqttOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler("clientId", mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultQos(1);
        handler.setDefaultRetained(false);
        return handler;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { mqttHost });
        factory.setConnectionOptions(options);
        return factory;
    }
}
