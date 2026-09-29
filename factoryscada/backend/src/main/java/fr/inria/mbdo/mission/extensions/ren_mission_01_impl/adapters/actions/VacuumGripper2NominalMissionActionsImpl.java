package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.Position3D;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.ReleaseRequestEventMessage;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * VacuumGripper2NominalMission actions: VGR2 takes the token from the conveyor belt swap to the MPS input.
 *
 * <p>Positions come from the REN_1S1V_1C1V_1H1M_02 RevPi02 controller (VGR2 named positions CB and MPS_INPUT), as
 * Position3D(vertical, horizontal, rot).
 */
public class VacuumGripper2NominalMissionActionsImpl implements VacuumGripper2NominalMissionActions {

    private static final Position3D CB_SWAP = new Position3D(1050, 1810, 1155);
    private static final Position3D MPS_INPUT = new Position3D(1000, 1890, 2050);

    @Override
    public void pickCBswap(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.pick(CB_SWAP);
    }

    @Override
    public void placeMPSin(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.place(MPS_INPUT);
    }

    @Override
    public void releaseCBZoneAndAcquireMPSZone(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB,
                                               Zone zoneMPS) {
        zoneCB.publish(new ReleaseRequestEventMessage());
        zoneMPS.publish(new AcquireRequestEventMessage());
    }

    @Override
    public void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, Zone zoneCB, Zone zoneMPS) {
        vacuumGripper.moveToSafePosition();
    }
}
