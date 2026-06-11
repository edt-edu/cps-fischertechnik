package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.AbstractVacuumGripperMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.VacuumGripperCommandKind;
import io.github.mbdo.factoryscada.mqtt.MqttMessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadParser.*;

/**
 * Adapter for a Vacuum Gripper machine (VacuumGripper1 or VacuumGripper2).
 *
 * <p>
 * Subscribes to {@code PLC/+/VacuumGripper/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class VacuumGripperAdapterImpl extends AbstractVacuumGripperMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(VacuumGripperAdapterImpl.class);

    public VacuumGripperAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    private void onMqttMessage(String topic, String payload) {
        String measurement = extractMeasurement(topic);
        log.debug("[{}] MQTT input: {}={}", id, measurement, payload);

        switch (measurement) {
            case "vacuumSensVerticalEncoderCounter" -> setVerticalEncoder(parseFloat(payload));
            case "vacuumSensRotEncoderCounter" -> setRotEncoder(parseFloat(payload));
            case "vacuumSensArmEncoderCounter" -> setArmEncoder(parseFloat(payload));
            case "vacuumSensVerticalEndUp" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            case "vacuumSensRotEnd" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            case "vacuumSensArmEndIn" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            default -> log.warn("[{}] Unknown MQTT measurement: {}", id, measurement);
        }
    }

    @Override
    public void setCurrentCommand(VacuumGripperCommandKind currentCommand) {
        super.setCurrentCommand(currentCommand);
        log.info("[{}] setCurrentCommand({})", id, currentCommand);
    }

    @Override
    public void release() {
        log.info("[{}] release()", id);
    }

    @Override
    public void statusRequest() {
        log.info("[{}] statusRequest()", id);
    }

    @Override
    public void pick() {
        log.info("[{}] pick()", id);
    }

    @Override
    public void move() {
        log.info("[{}] move()", id);
    }

    @Override
    public void grip() {
        log.info("[{}] grip()", id);
    }

    @Override
    public void goToPosition() {
        log.info("[{}] goToPosition()", id);
    }

    @Override
    public void place() {
        log.info("[{}] place()", id);
    }

    @Override
    public void moveToSafePosition() {
        log.info("[{}] moveToSafePosition()", id);
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
    }

    @Override
    public void setup() {
        log.info("[{}] setup()", id);
    }

    @Override
    public void retractArm() {
        log.info("[{}] retractArm()", id);
    }
}
