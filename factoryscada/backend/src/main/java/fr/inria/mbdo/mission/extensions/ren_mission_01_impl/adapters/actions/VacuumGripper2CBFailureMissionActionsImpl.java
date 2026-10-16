package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippercbfailuremissions.vacuumgripper2cbfailuremission.VacuumGripper2CBFailureMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBTokenRequestEventMessage;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * VacuumGripper2CBFailureMission actions: VGR2 takes the tokens from the slot of the failed conveyor belt the dead CB
 * zone gives, to the MPS input.
 *
 * <p>Named positions of VGR2 on the PLC: "CB" is the conveyor belt feed, "ALT_CB" its swap. "CB" is not calibrated on
 * the REN setup yet (see dt-setups vgr_named_positions.md).
 */
public class VacuumGripper2CBFailureMissionActionsImpl implements VacuumGripper2CBFailureMissionActions {

    private static final String CB_FEED = "CB";
    private static final String CB_SWAP = "ALT_CB";
    private static final String MPS_INPUT = "MPS_INPUT";

    @Override
    public void askCBForToken(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        zoneCB.publish(new CBTokenRequestEventMessage());
    }

    @Override
    public void pickCBfeed(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.pickNamed(CB_FEED);
    }

    @Override
    public void pickCBswap(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.pickNamed(CB_SWAP);
    }

    @Override
    public void requestMPSZone(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        zoneMPS.publish(new AcquireRequestEventMessage());
    }

    @Override
    public void placeMPSin(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.placeNamed(MPS_INPUT);
    }

    @Override
    public void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.moveToSafePosition();
    }
}
