package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.AbstractConveyorBeltMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltcommands.ConveyorCommandKind;
import io.github.mbdo.factoryscada.mqtt.MqttMessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadParser.*;

/**
 * Adapter for the Conveyor Belt machine.
 *
 * <p>
 * Subscribes to {@code PLC/+/ConveyorBelt/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class ConveyorBeltAdapterImpl extends AbstractConveyorBeltMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(ConveyorBeltAdapterImpl.class);

    public ConveyorBeltAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    private void onMqttMessage(String topic, String payload) {
        String measurement = extractMeasurement(topic);
        log.debug("[{}] MQTT input: {}={}", id, measurement, payload);

        switch (measurement) {
            case "conveyorSensFeed" -> setConveyorSensFeed(parseBool(payload));
            case "conveyorSensSwap" -> setConveyorSensSwap(parseBool(payload));
            case "conveyorSensImpulse" -> setConveyorSensImpulse(parseInt(payload));
            default -> log.warn("[{}] Unknown MQTT measurement: {}", id, measurement);
        }
    }

    @Override
    public void setCurrentCommand(ConveyorCommandKind currentCommand) {
        super.setCurrentCommand(currentCommand);
        log.info("[{}] setCurrentCommand({})", id, currentCommand);
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
    }

    @Override
    public void moveNbSteps() {
        log.info("[{}] moveNbSteps()", id);
    }

    @Override
    public void moveToSensor() {
        log.info("[{}] moveToSensor()", id);
    }

    @Override
    public void moveOut() {
        log.info("[{}] moveOut()", id);
    }

    @Override
    public void statusRequest() {
        log.info("[{}] statusRequest()", id);
    }
}
