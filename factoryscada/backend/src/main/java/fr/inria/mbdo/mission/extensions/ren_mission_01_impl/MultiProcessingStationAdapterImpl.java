package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.AbstractMultiProcessingStationMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MultiProcessingStationCommandKind;
import io.github.mbdo.factoryscada.mqtt.MqttMessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fr.inria.mbdo.mission.extensions.ren_mission_01_impl.MqttPayloadParser.*;

/**
 * Adapter for the Multi-Processing Station machine.
 *
 * <p>
 * Subscribes to {@code PLC/+/MultiProcessing/<instanceId>/#} and maps
 * input measurement topics to internal state fields.
 */
public class MultiProcessingStationAdapterImpl extends AbstractMultiProcessingStationMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(MultiProcessingStationAdapterImpl.class);

    public MultiProcessingStationAdapterImpl(String id, MqttMessageRouter mqttRouter, String mqttTopicFilter) {
        super(id);
        mqttRouter.subscribe(mqttTopicFilter, this::onMqttMessage);
    }

    private void onMqttMessage(String topic, String payload) {
        String measurement = extractMeasurement(topic);
        log.debug("[{}] MQTT input: {}={}", id, measurement, payload);

        switch (measurement) {
            case "multiProcessingSensTurntablePosVacuum" ->
                setSensor_MPS_in(parseBool(payload));
            case "multiProcessingSensTurntablePosBelt" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            case "multiProcessingSensTurntablePosSaw" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            case "multiProcessingSensEndConveyor" -> setSensor_MPS_out(parseBool(payload));
            case "multiProcessingSensOven" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            case "multiProcessingSensVacuumGripperAtTurntable" ->
                log.warn("[{}] measurement '{}' not mapped", id, measurement);
            case "multiProcessingSensVacuumGripperAtOven" ->
                log.warn("[{}] measurement '{}' not mapped", id, measurement);
            case "multiProcessingSensOvenFeederIn" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            case "multiProcessingSensOvenFeederOut" -> log.warn("[{}] measurement '{}' not mapped", id, measurement);
            default -> log.warn("[{}] Unknown MQTT measurement: {}", id, measurement);
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
    }

    @Override
    public void ovenLoad() {
        log.info("[{}] ovenLoad()", id);
    }

    @Override
    public void turntableEject() {
        log.info("[{}] turntableEject()", id);
    }

    @Override
    public void turntableRotate() {
        log.info("[{}] turntableRotate()", id);
    }

    @Override
    public void armPick() {
        log.info("[{}] armPick()", id);
    }

    @Override
    public void stop() {
        log.info("[{}] stop()", id);
    }

    @Override
    public void ovenHeat() {
        log.info("[{}] ovenHeat()", id);
    }

    @Override
    public void conveyorMoveOut() {
        log.info("[{}] conveyorMoveOut()", id);
    }

    @Override
    public void ovenUnload() {
        log.info("[{}] ovenUnload()", id);
    }

    @Override
    public void ovenProcess() {
        log.info("[{}] ovenProcess()", id);
    }

    @Override
    public void conveyorMoveToSensor() {
        log.info("[{}] conveyorMoveToSensor()", id);
    }

    @Override
    public void process1() {
        log.info("[{}] process1()", id);
    }

    @Override
    public void sawCut() {
        log.info("[{}] sawCut()", id);
    }

    @Override
    public void armPlace() {
        log.info("[{}] armPlace()", id);
    }

    @Override
    public void setup() {
        log.info("[{}] setup()", id);
    }

    @Override
    public void process() {
        log.info("[{}] process()", id);
    }

    @Override
    public void moveToSafePosition() {
        log.info("[{}] moveToSafePosition()", id);
    }
}
