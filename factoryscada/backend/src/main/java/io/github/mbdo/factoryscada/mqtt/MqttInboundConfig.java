package io.github.mbdo.factoryscada.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Configures MQTT inbound message handling.
 *
 * <p>
 * Subscribes to all topics under {@code PLC/#} and routes received
 * messages to the appropriate machine adapter via {@link MqttMessageRouter}.
 *
 * <h2>Configuration</h2>
 * Set the broker URL in {@code application.yaml}:
 *
 * <pre>
 * configuration:
 *   mqttHost: tcp://your-broker-host:1883
 * </pre>
 */
@Slf4j
@Configuration
public class MqttInboundConfig {

    @Value("${configuration.mqttHost:tcp://localhost:1883}")
    private String mqttHost;

    private final MqttMessageRouter mqttMessageRouter;

    public MqttInboundConfig(MqttMessageRouter mqttMessageRouter) {
        this.mqttMessageRouter = mqttMessageRouter;
    }

    @Bean
    public MqttPahoClientFactory mqttInboundClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { mqttHost });
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                "factoryscada-inbound-" + System.currentTimeMillis(),
                mqttInboundClientFactory(),
                "PLC/#"); // Subscribe to all PLC topics
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public MessageHandler mqttInboundHandler() {
        return message -> {
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            String payload = message.getPayload().toString();
            log.debug("[MQTT IN] topic={} payload={}", topic, payload);
            mqttMessageRouter.route(topic, payload);
        };
    }
}
