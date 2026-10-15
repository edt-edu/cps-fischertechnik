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

    /** Whether each light barrier was received at least once: before that, its field holds a default value. */
    @Getter
    private volatile boolean feedSensorKnown = false;
    @Getter
    private volatile boolean swapSensorKnown = false;

    public ConveyorBeltAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    // A value set through the setters (by MQTT or by a test) is known.
    @Override
    public void setConveyorSensFeed(boolean conveyorSensFeed) {
        feedSensorKnown = true;
        super.setConveyorSensFeed(conveyorSensFeed);
    }

    @Override
    public void setConveyorSensSwap(boolean conveyorSensSwap) {
        swapSensorKnown = true;
        super.setConveyorSensSwap(conveyorSensSwap);
    }

    /**
     * Whether a token is at the feed light barrier (active-low: false = token present). A light barrier never received
     * yet reads as free: its field holds a default value, which would read as a token.
     */
    public static boolean tokenAtFeed(
            fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine belt) {
        boolean known = !(belt instanceof ConveyorBeltAdapterImpl impl) || impl.isFeedSensorKnown();
        return known && !belt.getConveyorSensFeed();
    }

    /** Whether a token is at the swap light barrier (active-low: false = token present); unknown reads as free. */
    public static boolean tokenAtSwap(
            fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine belt) {
        boolean known = !(belt instanceof ConveyorBeltAdapterImpl impl) || impl.isSwapSensorKnown();
        return known && !belt.getConveyorSensSwap();
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
        commandSent("stop()");
        isExecuting = true;
        isExecuting = true;
        realMachine.stop();
    }

    @Override
    public void moveNbSteps() {
        log.info("[{}] moveNbSteps()", id);
        commandSent("moveNbSteps()");
        isExecuting = true;
        isExecuting = true;
        realMachine.moveNbSteps();
    }

    @Override
    public void moveToSensor() {
        log.info("[{}] moveToSensor()", id);
        commandSent("moveToSensor()");
        isExecuting = true;
        isExecuting = true;
        realMachine.moveToSensor();
    }

    @Override
    public void moveOut() {
        log.info("[{}] moveOut()", id);
        commandSent("moveOut()");
        isExecuting = true;
        isExecuting = true;
        realMachine.moveOut();
    }

    @Override
    public void statusRequest() {
        log.warn("[{}] statusRequest() — not implemented in domain machine", id);
    }
}
