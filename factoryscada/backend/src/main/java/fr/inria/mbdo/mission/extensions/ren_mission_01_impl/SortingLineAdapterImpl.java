package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.AbstractSortingLineMachineAdapter;
import io.github.mbdo.factoryscada.mqtt.MqttMessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadParser.*;

/**
 * Adapter for the Sorting Line machine.
 *
 * <p>
 * Subscribes to {@code PLC/+/SortingLine/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class SortingLineAdapterImpl extends AbstractSortingLineMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(SortingLineAdapterImpl.class);

    public SortingLineAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    private void onMqttMessage(String topic, String payload) {
        String measurement = extractMeasurement(topic);
        log.debug("[{}] MQTT input: {}={}", id, measurement, payload);

        switch (measurement) {
            case "sortingLineSensInputLightBarrier" -> setSensor_SL_in(parseBool(payload));
            case "sortingLineSensMiddleLightBarrier" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            case "sortingLineSensWhiteLightBarrier" -> setSensor_SL_white(parseBool(payload));
            case "sortingLineSensBlueLightBarrier" -> setSensor_SL_blue(parseBool(payload));
            case "sortingLineSensRedLightBarrier" -> setSensor_SL_red(parseBool(payload));
            case "sortingLineSensImpulseCounterRaw" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            default -> log.warn("[{}] Unknown MQTT measurement: {}", id, measurement);
        }
    }

    @Override
    public void eject() {
        log.info("[{}] eject()", id);
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
    }
}
