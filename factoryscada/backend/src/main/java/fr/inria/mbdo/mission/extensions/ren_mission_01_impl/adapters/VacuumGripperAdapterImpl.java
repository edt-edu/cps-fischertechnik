package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.AbstractVacuumGripperMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.VacuumGripperCommandKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadHelper;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Adapter for a Vacuum Gripper machine (VacuumGripper1 or VacuumGripper2).
 *
 * <p>
 * Subscribes to {@code PLC/+/VacuumGripper/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class VacuumGripperAdapterImpl extends AbstractVacuumGripperMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(VacuumGripperAdapterImpl.class);

    @Getter
    private boolean isExecuting = false;

    private Map<String, Runnable> commands = Map.of();

    public VacuumGripperAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
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
                        case "vacuumSensVerticalEncoderCounter" -> setVerticalEncoder(value.floatValue());
                        case "vacuumSensRotEncoderCounter" -> setRotEncoder(value.floatValue());
                        case "vacuumSensArmEncoderCounter" -> setArmEncoder(value.floatValue());
                        default -> log.debug("Ignoring input {}/{}", id, inputName);
                    }
                }
                default -> log.warn("Unknown MQTT measurement kind: {}", measurementKind);
            }
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
        dispatch("release");
    }

    @Override
    public void statusRequest() {
        log.info("[{}] statusRequest()", id);
        dispatch("statusRequest");
    }

    @Override
    public void pick() {
        log.info("[{}] pick()", id);
        dispatch("pick");
    }

    @Override
    public void move() {
        log.info("[{}] move()", id);
        dispatch("move");
    }

    @Override
    public void grip() {
        log.info("[{}] grip()", id);
        dispatch("grip");
    }

    @Override
    public void goToPosition() {
        log.info("[{}] goToPosition()", id);
        dispatch("goToPosition");
    }

    @Override
    public void place() {
        log.info("[{}] place()", id);
        dispatch("place");
    }

    @Override
    public void moveToSafePosition() {
        log.info("[{}] moveToSafePosition()", id);
        dispatch("moveToSafePosition");
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
        dispatch("stop");
    }

    @Override
    public void setup() {
        log.info("[{}] setup()", id);
        dispatch("setup");
    }

    @Override
    public void retractArm() {
        log.info("[{}] retractArm()", id);
        dispatch("retractArm");
    }
}
