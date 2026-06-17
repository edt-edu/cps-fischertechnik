package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.AbstractConveyorBeltMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltcommands.ConveyorCommandKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.CBCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadHelper;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for the Conveyor Belt machine.
 *
 * <p>
 * Subscribes to {@code PLC/+/ConveyorBelt/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class ConveyorBeltAdapterImpl extends AbstractConveyorBeltMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(ConveyorBeltAdapterImpl.class);

    @Getter
    private boolean isExecuting = false;

    private ConveyorBeltMachine realMachine;

    public ConveyorBeltAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    public void bindRealMachine(ConveyorBeltMachine machine) {
        this.realMachine = machine;
    }

    public void commandFeedback(boolean done, String status) {
        log.info("[{}] commandFeedback({}, {})", id, done, status);
        if (done) {
            isExecuting = false;
            publish(new CBCommandSuccessEventMessage());
        }
    }

    protected void onMqttMessage(String topic, String payload) {
        MqttPayloadHelper.MqttMessage message = MqttPayloadHelper.parseMqttMessage(topic, payload);
        if (message != null) {
            String measurementKind = message.measurementKind();
            String inputName = message.inputName();
            JsonNode value = message.value();

            switch (measurementKind) {
                case "internal" -> {
                    if ("isExecuting".equals(inputName)) {
                        log.debug("Updating idle state for machine {} to {}", id, !value.asBoolean());
                        isExecuting = !value.asBoolean();
                    }
                }
                case "input" -> {
                    switch (inputName) {
                        case "conveyorSensFeed" -> setConveyorSensFeed(value.asBoolean());
                        case "conveyorSensSwap" -> setConveyorSensSwap(value.asBoolean());
                        case "conveyorSensImpulse" -> setConveyorSensImpulse(value.asInt());
                        default -> log.debug("Ignoring input {}/{}", id, inputName);
                    }
                }
                default -> log.warn("Unknown MQTT measurement kind: {}", measurementKind);
            }
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
        isExecuting = true;
        isExecuting = true;
        realMachine.stop();
    }

    @Override
    public void moveNbSteps() {
        log.info("[{}] moveNbSteps()", id);
        isExecuting = true;
        isExecuting = true;
        realMachine.moveNbSteps();
    }

    @Override
    public void moveToSensor() {
        log.info("[{}] moveToSensor()", id);
        isExecuting = true;
        isExecuting = true;
        realMachine.moveToSensor();
    }

    @Override
    public void moveOut() {
        log.info("[{}] moveOut()", id);
        isExecuting = true;
        isExecuting = true;
        realMachine.moveOut();
    }

    @Override
    public void statusRequest() {
        log.warn("[{}] statusRequest() — not implemented in domain machine", id);
    }
}
