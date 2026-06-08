package io.github.mbdo.factoryscada.mqtt;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Component;

/**
 * ensures that the MqttListener will process message only when the app is ready
 */
@Component
public class MqttStartup {

    private final MqttPahoMessageDrivenChannelAdapter adapter;

    public MqttStartup(MqttPahoMessageDrivenChannelAdapter adapter) {
        this.adapter = adapter;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startMqtt() {
        adapter.start();
    }
}
