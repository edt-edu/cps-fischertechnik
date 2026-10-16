package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.SortingLineNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.BlueTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.NoTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.RedTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.WhiteTokenAvailableEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * SortingLineNominalMission actions.
 */
public class SortingLineNominalMissionActionsImpl implements SortingLineNominalMissionActions {

    /**
     * Tells the gripper which colors wait at the outputs: one *TokenAvailable message per output whose light barrier
     * (active-low) sees a token, else NoTokenAvailable. The gripper takes the first one. The messages are published on
     * the sorting line, where the gripper missions subscribe to the sorting line messages.
     */
    @Override
    public void answerTokenRequest(Event event, SortingLineMachine sortingLine, VacuumGripperMachine vacuumGripper) {
        boolean any = false;
        if (!sortingLine.getSensor_SL_blue()) {
            sortingLine.publish(new BlueTokenAvailableEventMessage());
            any = true;
        }
        if (!sortingLine.getSensor_SL_white()) {
            sortingLine.publish(new WhiteTokenAvailableEventMessage());
            any = true;
        }
        if (!sortingLine.getSensor_SL_red()) {
            sortingLine.publish(new RedTokenAvailableEventMessage());
            any = true;
        }
        if (!any) {
            sortingLine.publish(new NoTokenAvailableEventMessage());
        }
    }
}
