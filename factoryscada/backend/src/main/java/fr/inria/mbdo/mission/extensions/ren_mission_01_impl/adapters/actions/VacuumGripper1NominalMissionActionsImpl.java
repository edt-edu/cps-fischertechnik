package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

public class VacuumGripper1NominalMissionActionsImpl implements VacuumGripper1NominalMissionActions {

    // Named positions of VGR1 on the PLC (VacuumGripperParameters.named_positions), calibrated for each setup
    private static final String SL_OUTPUT_WHITE = "SL_OUTPUT_WHITE";
    private static final String SL_OUTPUT_RED = "SL_OUTPUT_RED";
    private static final String SL_OUTPUT_BLUE = "SL_OUTPUT_BLUE";
    private static final String CB_FEED = "CB";

    @Override
    public void pickBlue(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        vacuumGripper.pickNamed(SL_OUTPUT_BLUE);
    }

    @Override
    public void pickWhite(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        vacuumGripper.pickNamed(SL_OUTPUT_WHITE);
    }

    @Override
    public void pickRed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        vacuumGripper.pickNamed(SL_OUTPUT_RED);
    }

    @Override
    public void placeConveyoBeltFeed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                                     Zone zoneCB) {
        vacuumGripper.placeNamed(CB_FEED);
    }

    @Override
    public void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine, Zone zoneCB) {
        vacuumGripper.moveToSafePosition();
    }
}
