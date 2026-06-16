package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.AbstractSortingLineMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadHelper;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Adapter for the Sorting Line machine.
 *
 * <p>
 * Subscribes to {@code PLC/+/SortingLine/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class SortingLineAdapterImpl extends AbstractSortingLineMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(SortingLineAdapterImpl.class);
    private boolean isExecuting = false;
    private Map<String, Runnable> commands = Map.of();

    public SortingLineAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    public void bindCommands(Map<String, Runnable> commands) {
        this.commands = Map.copyOf(commands);
    }

    private void dispatch(String cmd) {
        commands.getOrDefault(cmd, () -> log.debug("[{}] {} not bound to machine", id, cmd)).run();
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
                        this.isExecuting = !value.asBoolean();
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
        dispatch("eject");
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
        dispatch("stop");
    }
}
