package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.AbstractSortingLineMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.SLCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadHelper;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for the Sorting Line machine.
 *
 * <p>
 * Subscribes to {@code PLC/+/SortingLine/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class SortingLineAdapterImpl extends AbstractSortingLineMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(SortingLineAdapterImpl.class);

    @Getter
    private boolean isExecuting = false;

    private SortingLineMachine realMachine;

    public SortingLineAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    public void bindRealMachine(SortingLineMachine machine) {
        this.realMachine = machine;
    }

    public void commandFeedback(boolean done, String status) {
        log.info("[{}] commandFeedback({}, {})", id, done, status);
        if (done) {
            isExecuting = false;
            publish(new SLCommandSuccessEventMessage());
        }
    }

    private void onMqttMessage(String topic, String payload) {
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
                        case "sortingLineSensInputLightBarrier" -> setSensor_SL_in(value.asBoolean());
                        case "sortingLineSensWhiteLightBarrier" -> setSensor_SL_white(value.asBoolean());
                        case "sortingLineSensBlueLightBarrier" -> setSensor_SL_blue(value.asBoolean());
                        case "sortingLineSensRedLightBarrier" -> setSensor_SL_red(value.asBoolean());
                        default -> log.debug("Ignoring input {}/{}", id, inputName);
                    }
                }
                default -> log.warn("Unknown MQTT measurement kind: {}", measurementKind);
            }
        }
    }

    @Override
    public void eject() {
        log.info("[{}] eject()", id);
        isExecuting = true;
        realMachine.eject();
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
        isExecuting = true;
        realMachine.stop();

    }
}
