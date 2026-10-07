package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.AbstractVacuumGripperMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.Position3D;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.VacuumGripperCommandKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippermessages.VGRCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadHelper;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import io.github.mbdo.factoryscada.core.enums.PositionMeaning;
import io.github.mbdo.factoryscada.core.passable.NamedPosition;
import io.github.mbdo.factoryscada.core.passable.PositionParameterThreeD;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private VacuumGripperMachine realMachine;

    public VacuumGripperAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    public void bindRealMachine(VacuumGripperMachine machine) {
        this.realMachine = machine;
    }

    public void commandFeedback(boolean done, String status) {
        log.info("[{}] commandFeedback({}, {})", id, done, status);
        if (done) {
            isExecuting = false;
            publish(new VGRCommandSuccessEventMessage());
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
    public void goToPosition(Position3D targetPosition) {
        log.info("[{}] goToPosition({})", id, targetPosition);
        commandSent(String.format("goToPosition(%s)", targetPosition));

        PositionParameterThreeD positionThreeD = new PositionParameterThreeD(
                PositionMeaning.OTHER,
                Math.round(targetPosition.vertical()),
                Math.round(targetPosition.horizontal()),
                Math.round(targetPosition.rot()));

        isExecuting = true;
        realMachine.goToPosition(positionThreeD);
    }

    /** The named positions are resolved by the PLC (VacuumGripperParameters.named_positions of the machine). */
    @Override
    public void goToNamedPosition(String positionName) {
        log.info("[{}] goToNamedPosition({})", id, positionName);
        commandSent(String.format("goToNamedPosition(%s)", positionName));
        isExecuting = true;
        realMachine.goToPosition(new NamedPosition(positionName));
    }

    @Override
    public void pickNamed(String positionName) {
        log.info("[{}] pickNamed({})", id, positionName);
        commandSent(String.format("pickNamed(%s)", positionName));
        isExecuting = true;
        realMachine.pick(new NamedPosition(positionName));
    }

    @Override
    public void placeNamed(String positionName) {
        log.info("[{}] placeNamed({})", id, positionName);
        commandSent(String.format("placeNamed(%s)", positionName));
        isExecuting = true;
        realMachine.place(new NamedPosition(positionName));
    }

    @Override
    public void move(Position3D startPosition, Position3D endPosition) {
        log.info("[{}] move({}, {})", id, startPosition, endPosition);
        commandSent(String.format("move(%s, %s)", startPosition, endPosition));

        PositionParameterThreeD startPositionThreeD = new PositionParameterThreeD(
                PositionMeaning.START,
                Math.round(startPosition.vertical()),
                Math.round(startPosition.horizontal()),
                Math.round(startPosition.rot()));
        PositionParameterThreeD endPositionThreeD = new PositionParameterThreeD(
                PositionMeaning.END,
                Math.round(endPosition.vertical()),
                Math.round(endPosition.horizontal()),
                Math.round(endPosition.rot()));

        isExecuting = true;
        realMachine.move(startPositionThreeD, endPositionThreeD);
    }

    @Override
    public void pick(Position3D targetPosition) {
        log.info("[{}] pick({})", id, targetPosition);
        commandSent(String.format("pick(%s)", targetPosition));

        PositionParameterThreeD positionThreeD = new PositionParameterThreeD(
                PositionMeaning.OTHER,
                Math.round(targetPosition.vertical()),
                Math.round(targetPosition.horizontal()),
                Math.round(targetPosition.rot()));

        isExecuting = true;
        realMachine.pick(positionThreeD);
    }

    @Override
    public void place(Position3D targetPosition) {
        log.info("[{}] place({})", id, targetPosition);
        commandSent(String.format("place(%s)", targetPosition));

        PositionParameterThreeD positionThreeD = new PositionParameterThreeD(
                PositionMeaning.OTHER,
                Math.round(targetPosition.vertical()),
                Math.round(targetPosition.horizontal()),
                Math.round(targetPosition.rot()));

        isExecuting = true;
        realMachine.place(positionThreeD);
    }

    @Override
    public void release() {
        log.info("[{}] release()", id);
        commandSent("release()");
        isExecuting = true;
        realMachine.release();
    }

    @Override
    public void statusRequest() {
        log.info("[{}] statusRequest()", id);
        commandSent("statusRequest()");
        isExecuting = true;
        realMachine.statusRequest();
    }

    @Override
    public void grip() {
        log.info("[{}] grip()", id);
        commandSent("grip()");
        isExecuting = true;
        realMachine.grip();
    }

    @Override
    public void moveToSafePosition() {
        log.info("[{}] moveToSafePosition()", id);
        commandSent("moveToSafePosition()");
        isExecuting = true;
        realMachine.go_to_safe_position();
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
        commandSent("stop()");
        isExecuting = true;
        realMachine.stop();
    }

    @Override
    public void setup() {
        log.info("[{}] setup()", id);
        commandSent("setup()");
        isExecuting = true;
        realMachine.setup();
    }

    @Override
    public void retractArm() {
        log.info("[{}] retractArm()", id);
        commandSent("retractArm()");
        isExecuting = true;
        realMachine.retract_arm();
    }
}
