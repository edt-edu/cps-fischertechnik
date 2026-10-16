package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.MultiProcessingStationMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands.MPSOutput;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationmessages.InputFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationmessages.OutputBusyEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationmessages.PayloadAtInputEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * Default (logging) implementation of MultiProcessingStationNominalMission
 * actions.
 * Replace with real logic when connecting to hardware.
 */
public class MultiProcessingStationNominalMissionActionsImpl implements MultiProcessingStationNominalMissionActions {

    // the process run on each payload: 6 s in the oven, 5 s under the saw, delivered at the end of the conveyor
    private static final int OVEN_TIME_S = 6;
    private static final int SAW_TIME_S = 5;
    private static final MPSOutput OUTPUT = MPSOutput.CONVEYOR;

    @Override
    public void processPayload(Event event, MultiProcessingStationMachine multiProcessingStation, Zone zoneMPS) {
        multiProcessingStation.process(OVEN_TIME_S, SAW_TIME_S, OUTPUT);
    }

    /**
     * InputFree: nothing at the input; OutputBusy: a payload at the input, but the output still holds the previous
     * one; PayloadAtInput: a payload at the input and the output free, ready to be processed.
     */
    @Override
    public void checkPayloadAtInput(Event event, MultiProcessingStationMachine multiProcessingStation, Zone zoneMPS) {
        if (!multiProcessingStation.getSensor_MPS_in()) {
            multiProcessingStation.publish(new InputFreeEventMessage());
        } else if (multiProcessingStation.getSensor_MPS_out()) {
            multiProcessingStation.publish(new OutputBusyEventMessage());
        } else {
            multiProcessingStation.publish(new PayloadAtInputEventMessage());
        }
    }
}
