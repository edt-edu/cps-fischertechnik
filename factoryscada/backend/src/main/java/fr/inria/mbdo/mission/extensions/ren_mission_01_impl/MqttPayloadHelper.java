package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Shared utility for parsing MQTT payload values into typed Java fields.
 */
public final class MqttPayloadHelper {
    private static final Logger logger = LoggerFactory.getLogger(MqttPayloadHelper.class);

    /**
     * PLC/<island>/<type>/<machineName>/measurements/<measurementKind>/<inputName>
     */
    public static MqttMessage parseMqttMessage(String topic, String payload) {
        String[] parts = topic.split("/");
        if (parts.length < 7)
            return null;
        String measurementKind = parts[5];
        String inputName = parts[6];
        JsonNode value;
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(payload);
            if (!node.isObject() || !node.has("value"))
                return null;
            value = node.get("value");
        } catch (IOException e) {
            logger.debug("Failed reading MQTT payload for {}: {}", topic, e.getMessage());
            return null;
        }
        return new MqttMessage(measurementKind, inputName, value);

    }

    public record MqttMessage(String measurementKind, String inputName, JsonNode value) {
    }
}
