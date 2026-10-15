package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.SwapBusyEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ConveyorBeltAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * VacuumGripper2NominalMission actions: VGR2 takes the token from the conveyor belt swap to the MPS input.
 *
 * <p>It uses the named positions of VGR2 on the PLC (VacuumGripperParameters.named_positions), calibrated for each
 * setup: "ALT_CB" is the conveyor belt swap on the VGR2 side.
 */
public class VacuumGripper2NominalMissionActionsImpl implements VacuumGripper2NominalMissionActions {

    private static final String CB_SWAP = "ALT_CB";
    private static final String MPS_INPUT = "MPS_INPUT";

    /**
     * The conveyor belt sends SwapBusy once, when it brings a token to the swap: VGR2, if busy then, missed it. Once
     * free, VGR2 resends it to itself if a token waits at the swap.
     */
    @Override
    public void checkSwap(Event event, VacuumGripperMachine vacuumGripper, ConveyorBeltMachine conveyorBelt, Zone zoneCB, Zone zoneMPS) {
        if (ConveyorBeltAdapterImpl.tokenAtSwap(conveyorBelt)) {
            vacuumGripper.publish(new SwapBusyEventMessage());
        }
    }

    @Override
    public void pickCBswap(Event event, VacuumGripperMachine vacuumGripper, ConveyorBeltMachine conveyorBelt, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.pickNamed(CB_SWAP);
    }

    @Override
    public void placeMPSin(Event event, VacuumGripperMachine vacuumGripper, ConveyorBeltMachine conveyorBelt, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.placeNamed(MPS_INPUT);
    }

    @Override
    public void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, ConveyorBeltMachine conveyorBelt, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.moveToSafePosition();
    }
}
