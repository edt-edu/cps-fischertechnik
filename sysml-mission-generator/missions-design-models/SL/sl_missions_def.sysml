package SortingLineMissions {
    private import Common::*;
    private import SortingLineSystem::*;
    private import SortingLineSystem::SortingLineCommands::*;
    private import SortingLineSystem::SortingLineMessages::*;

    state def SortingLineNominalMission {
        doc
        /* Nominal mission scenario for SortingLine */

        @MainMachineMetadata about sortingLine;
        in sortingLine : SortingLineSystem::SortingLine::SortingLineMachine;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;

        // publishes on vacuumGripper one *TokenAvailable message per output whose light barrier sees a token, or
        // NoTokenAvailable
        action answerTokenRequest;

        entry;
        then Idle;

        // Idle, the sorting line answers the gripper asking which colors wait at its outputs (a request received while
        // it sorts is dropped: the sort ends by announcing the color to the gripper).
        // It also announces a token seen at an output: an accept when fires on each light barrier update while its
        // condition holds, so a sort can end on a token already waiting at another output (when the token being
        // sorted passes the input light barrier), before the token reaches its output, the sorting line then idle. A
        // gripper free (asking, then waiting in Idle) gets that token too. Repeated announcements of a waiting token
        // are dropped by a busy gripper.
        state Idle;
        accept when sortingLine.sensor_SL_in == false do sortingLine.eject then SortCMD;
        accept TokenRequestEventMessage via sortingLine do answerTokenRequest then Idle;
        accept when sortingLine.sensor_SL_blue == false
            do send new BlueTokenAvailableEventMessage() to vacuumGripper
            then Idle;
        accept when sortingLine.sensor_SL_white == false
            do send new WhiteTokenAvailableEventMessage() to vacuumGripper
            then Idle;
        accept when sortingLine.sensor_SL_red == false
            do send new RedTokenAvailableEventMessage() to vacuumGripper
            then Idle;

        state SortCMD;
        accept when sortingLine.sensor_SL_blue == false
            do send new BlueTokenAvailableEventMessage() to vacuumGripper
            then Idle;
        accept when sortingLine.sensor_SL_white == false
            do send new WhiteTokenAvailableEventMessage() to vacuumGripper
            then Idle;
        accept when sortingLine.sensor_SL_red == false
            do send new RedTokenAvailableEventMessage() to vacuumGripper
            then Idle;
    }
} // package SortingLineMissions
