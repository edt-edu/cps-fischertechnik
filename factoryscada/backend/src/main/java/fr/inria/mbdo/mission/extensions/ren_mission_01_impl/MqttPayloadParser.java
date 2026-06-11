package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

/**
 * Shared utility for parsing MQTT payload values into typed Java fields.
 */
public final class MqttPayloadParser {

    private MqttPayloadParser() {
    }

    public static boolean parseBool(String payload) {
        return "true".equalsIgnoreCase(payload.trim()) || "1".equals(payload.trim());
    }

    public static int parseInt(String payload) {
        try {
            return Integer.parseInt(payload.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static float parseFloat(String payload) {
        try {
            return Float.parseFloat(payload.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    /**
     * Extracts the last segment from an MQTT topic path.
     * E.g.
     * {@code "PLC/plc-1/ConveyorBelt/CB01/measurements/input/conveyorSensFeed"} →
     * {@code "conveyorSensFeed"}
     */
    public static String extractMeasurement(String topic) {
        return topic.substring(topic.lastIndexOf('/') + 1);
    }
}
