package io.github.mbdo.factoryscada.mqtt;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;


/**
 * Configuration for 2 Mqtt clients,
 *  one for Inbound (currently used for listening messages from other services)
 *  one for outbound (currently used for publishing this application telemetry)
 */
@Getter
@Slf4j
@Configuration
public class MqttConfig {
    @Value("${configuration.mqtt.inbound.host:tcp://localhost:1883}")
    String mqttInboundHost;

    @Value("${configuration.mqtt.inbound.topics:#}")
    String[] mqttTopics;

    @Value("${configuration.mqtt.outbound.host:tcp://localhost:1883}")
    String mqttOutboundHost;

    @Value("${configuration.mqtt.outbound.root-topic:FactorySCADABackend}")
    private String outboundRootTopic;

    @Value("${configuration.mqtt.outbound.topic-include-hostname:True}")
    private Boolean outboundTopicIncludeHostname;

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MqttPahoMessageHandler mqttOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler("clientId-outbound", mqttOutboundClientFactory());
        handler.setAsync(true);
        handler.setDefaultQos(1);
        handler.setDefaultRetained(true);
        return handler;
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter("clientId-inbound", mqttInboundClientFactory(), mqttTopics);
        adapter.setCompletionTimeout(5000);
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel());
        adapter.setAutoStartup(Boolean.FALSE);      // ensure we do not process message before the app is ready cf. MqttStartup.java
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public MessageHandler mqttInboundHandler(MqttInboundRouterService mqttInboundRouterService) {
        return message -> {
            String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
            if (topic == null) {
                return;
            }
            mqttInboundRouterService.onMessage(topic, toBytes(message));
        };
    }

    @Bean
    public MqttPahoClientFactory mqttInboundClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        log.info("MQTT Inbound Connecting to {}", mqttInboundHost);
        options.setServerURIs(new String[] {mqttInboundHost});
        factory.setConnectionOptions(options);
        return factory;
    }
    @Bean
    public MqttPahoClientFactory mqttOutboundClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        log.info("MQTT Outbound Connecting to {}", mqttOutboundHost);
        options.setServerURIs(new String[] {mqttOutboundHost});
        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * Use the configuration to compute the base topic for outbound mqtt messages sent by MqttPublisher
     * @return
     * @throws UnknownHostException
     */
    @Bean("mqttPublisherBaseTopic")
    public String mqttPublisherBaseTopic() throws UnknownHostException {
        String baseTopic = "";
        if (outboundRootTopic != null && !outboundRootTopic.isBlank()) {
            baseTopic = outboundRootTopic;
        }

        if (outboundTopicIncludeHostname) {
            String hostname = InetAddress.getLocalHost().getHostName();
            baseTopic = baseTopic + (!baseTopic.isBlank()? "/":"") + hostname;
        }

        return baseTopic;
    }
    private static byte[] toBytes(Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof byte[] bytes) {
            return bytes;
        }
        return payload.toString().getBytes(StandardCharsets.UTF_8);
    }
}
