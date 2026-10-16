package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.TokenRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippercbfailuremissions.vacuumgripper1cbfailuremission.VacuumGripper1CBFailureMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotRequestEventMessage;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * VacuumGripper1CBFailureMission actions: VGR1 takes the tokens at the sorting line outputs to the slot of the failed
 * conveyor belt the dead CB zone gives.
 *
 * <p>Named positions of VGR1 on the PLC: "CB" is the conveyor belt feed, "ALT_CB" its swap.
 */
public class VacuumGripper1CBFailureMissionActionsImpl implements VacuumGripper1CBFailureMissionActions {

    private static final String SL_OUTPUT_WHITE = "SL_OUTPUT_WHITE";
    private static final String SL_OUTPUT_RED = "SL_OUTPUT_RED";
    private static final String SL_OUTPUT_BLUE = "SL_OUTPUT_BLUE";
    private static final String CB_FEED = "CB";
    private static final String CB_SWAP = "ALT_CB";

    @Override
    public void askSortingLine(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                               Zone zoneCB) {
        sortingLine.publish(new TokenRequestEventMessage());
    }

    @Override
    public void pickBlue(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                         Zone zoneCB) {
        vacuumGripper.pickNamed(SL_OUTPUT_BLUE);
    }

    @Override
    public void pickWhite(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                          Zone zoneCB) {
        vacuumGripper.pickNamed(SL_OUTPUT_WHITE);
    }

    @Override
    public void pickRed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                        Zone zoneCB) {
        vacuumGripper.pickNamed(SL_OUTPUT_RED);
    }

    @Override
    public void askCBSlot(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                          Zone zoneCB) {
        zoneCB.publish(new CBSlotRequestEventMessage());
    }

    @Override
    public void placeConveyorBeltFeed(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                                      Zone zoneCB) {
        vacuumGripper.placeNamed(CB_FEED);
    }

    @Override
    public void placeConveyorBeltSwap(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                                      Zone zoneCB) {
        vacuumGripper.placeNamed(CB_SWAP);
    }

    @Override
    public void gotoStandby(Event event, VacuumGripperMachine vacuumGripper, SortingLineMachine sortingLine,
                            Zone zoneCB) {
        vacuumGripper.moveToSafePosition();
    }
}
