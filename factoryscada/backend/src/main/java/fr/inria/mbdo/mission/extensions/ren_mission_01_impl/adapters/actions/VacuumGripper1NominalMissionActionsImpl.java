package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.Position3D;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

public class VacuumGripper1NominalMissionActionsImpl implements VacuumGripper1NominalMissionActions {

    private static final Position3D SL_OUTPUT_WHITE = new Position3D(1400, 500, 2400);
    private static final Position3D SL_OUTPUT_RED = new Position3D(1400, 900, 2270);
    private static final Position3D SL_OUTPUT_BLUE = new Position3D(1400, 1500, 2135);
    private static final Position3D CB_FEED = new Position3D(1400, 1400, 1870);

    @Override
    public void pickBlue(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        vacuumGripper.pick(SL_OUTPUT_BLUE);
    }

    @Override
    public void pickWhite(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        vacuumGripper.pick(SL_OUTPUT_WHITE);
    }

    @Override
    public void pickRed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        vacuumGripper.pick(SL_OUTPUT_RED);
    }

    @Override
    public void placeConveyoBeltFeed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                                     Zone zoneCB) {
        vacuumGripper.place(CB_FEED);
    }

    @Override
    public void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        vacuumGripper.moveToSafePosition();
    }
}
