package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

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

    @Override
    public void pickCBswap(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.pickNamed(CB_SWAP);
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
