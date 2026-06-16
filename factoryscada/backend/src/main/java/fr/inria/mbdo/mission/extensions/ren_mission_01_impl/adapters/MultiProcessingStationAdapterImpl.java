package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.AbstractMultiProcessingStationMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MultiProcessingStationCommandKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadHelper;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Adapter for the Multi-Processing Station machine.
 *
 * <p>
 * Subscribes to {@code PLC/+/MultiProcessing/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class MultiProcessingStationAdapterImpl extends AbstractMultiProcessingStationMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(MultiProcessingStationAdapterImpl.class);
    private boolean isExecuting = false;
    private Map<String, Runnable> commands = Map.of();

    public MultiProcessingStationAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
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
    public void armMove() {
        log.info("[{}] armMove()", id);
        dispatch("armMove");
    }

    @Override
    public void ovenLoad() {
        log.info("[{}] ovenLoad()", id);
        dispatch("ovenLoad");
    }

    @Override
    public void turntableEject() {
        log.info("[{}] turntableEject()", id);
        dispatch("turntableEject");
    }

    @Override
    public void turntableRotate() {
        log.info("[{}] turntableRotate()", id);
        dispatch("turntableRotate");
    }

    @Override
    public void armPick() {
        log.info("[{}] armPick()", id);
        dispatch("armPick");
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
        dispatch("stop");
    }

    @Override
    public void ovenHeat() {
        log.info("[{}] ovenHeat()", id);
        dispatch("ovenHeat");
    }

    @Override
    public void conveyorMoveOut() {
        log.info("[{}] conveyorMoveOut()", id);
        dispatch("conveyorMoveOut");
    }

    @Override
    public void ovenUnload() {
        log.info("[{}] ovenUnload()", id);
        dispatch("ovenUnload");
    }

    @Override
    public void ovenProcess() {
        log.info("[{}] ovenProcess()", id);
        dispatch("ovenProcess");
    }

    @Override
    public void conveyorMoveToSensor() {
        log.info("[{}] conveyorMoveToSensor()", id);
        dispatch("conveyorMoveToSensor");
    }

    @Override
    public void process1() {
        log.info("[{}] process1()", id);
        dispatch("process1");
    }

    @Override
    public void sawCut() {
        log.info("[{}] sawCut()", id);
        dispatch("sawCut");
    }

    @Override
    public void armPlace() {
        log.info("[{}] armPlace()", id);
        dispatch("armPlace");
    }

    @Override
    public void setup() {
        log.info("[{}] setup()", id);
        dispatch("setup");
    }

    @Override
    public void process() {
        log.info("[{}] process()", id);
        dispatch("process");
    }

    @Override
    public void moveToSafePosition() {
        log.info("[{}] moveToSafePosition()", id);
        dispatch("moveToSafePosition");
    }
}
