package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.AbstractMultiProcessingStationMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MultiProcessingStationCommandKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationmessages.MPSCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadHelper;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for the Multi-Processing Station machine.
 *
 * <p>
 * Subscribes to {@code PLC/+/MultiProcessing/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class MultiProcessingStationAdapterImpl extends AbstractMultiProcessingStationMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(MultiProcessingStationAdapterImpl.class);

    @Getter
    private boolean isExecuting = false;

    private MultiProcessingStationMachine realMachine;

    public MultiProcessingStationAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    public void bindRealMachine(MultiProcessingStationMachine machine) {
        this.realMachine = machine;
    }

    public void commandFeedback(boolean done, String status) {
        log.info("[{}] commandFeedback({}, {})", id, done, status);
        if (done) {
            isExecuting = false;
            publish(new MPSCommandSuccessEventMessage());
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
                        case "multiProcessingSensTurntablePosVacuum" -> setSensor_MPS_in(value.asBoolean());
                        case "multiProcessingSensEndConveyor" -> setSensor_MPS_out(value.asBoolean());
                        default -> log.debug("Ignoring input {}/{}", id, inputName);
                    }
                }
                default -> log.warn("Unknown MQTT measurement kind: {}", measurementKind);
            }
        }
    }

    @Override
    public void setCurrentCommand(MultiProcessingStationCommandKind currentCommand) {
        super.setCurrentCommand(currentCommand);
        log.info("[{}] setCurrentCommand({})", id, currentCommand);
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
    public void process1() {
        log.info("[{}] process1()", id);
        commandSent("process1()");
        isExecuting = true;
        realMachine.process1();
    }

    @Override
    public void process() {
        log.info("[{}] process()", id);
        commandSent("process()");
        isExecuting = true;
        realMachine.process();
    }

    @Override
    public void moveToSafePosition() {
        log.info("[{}] moveToSafePosition()", id);
        commandSent("moveToSafePosition()");
        isExecuting = true;
        realMachine.moveToSafePosition();
    }

    @Override
    public void ovenLoad() {
        log.info("[{}] ovenLoad()", id);
        commandSent("ovenLoad()");
        isExecuting = true;
        realMachine.ovenLoad();
    }

    @Override
    public void ovenUnload() {
        log.info("[{}] ovenUnload()", id);
        commandSent("ovenUnload()");
        isExecuting = true;
        realMachine.ovenUnload();
    }

    @Override
    public void ovenHeat() {
        log.info("[{}] ovenHeat()", id);
        commandSent("ovenHeat()");
        isExecuting = true;
        realMachine.ovenHeat();
    }

    @Override
    public void ovenProcess() {
        log.info("[{}] ovenProcess()", id);
        commandSent("ovenProcess()");
        isExecuting = true;
        realMachine.ovenProcess();
    }

    @Override
    public void armMove() {
        log.info("[{}] armMove()", id);
        commandSent("armMove()");
        isExecuting = true;
        realMachine.armMove();
    }

    @Override
    public void armPick() {
        log.info("[{}] armPick()", id);
        commandSent("armPick()");
        isExecuting = true;
        realMachine.armPick();
    }

    @Override
    public void armPlace() {
        log.info("[{}] armPlace()", id);
        commandSent("armPlace()");
        isExecuting = true;
        realMachine.armPlace();
    }

    @Override
    public void turntableRotate() {
        log.info("[{}] turntableRotate()", id);
        commandSent("turntableRotate()");
        isExecuting = true;
        realMachine.turntableRotate();
    }

    @Override
    public void turntableEject() {
        log.info("[{}] turntableEject()", id);
        commandSent("turntableEject()");
        isExecuting = true;
        realMachine.turntableEject();
    }

    @Override
    public void conveyorMoveToSensor() {
        log.info("[{}] conveyorMoveToSensor()", id);
        commandSent("conveyorMoveToSensor()");
        isExecuting = true;
        realMachine.conveyorMoveToSensor();
    }

    @Override
    public void conveyorMoveOut() {
        log.info("[{}] conveyorMoveOut()", id);
        commandSent("conveyorMoveOut()");
        isExecuting = true;
        realMachine.conveyorMoveOut();
    }

    @Override
    public void sawCut() {
        log.info("[{}] sawCut()", id);
        commandSent("sawCut()");
        isExecuting = true;
        realMachine.sawCut();
    }
}
