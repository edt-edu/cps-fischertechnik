package SortingLineMissions {
    private import Common::Messages::*;
    private import SortingLineSystem::*;
    private import SortingLineSystem::SortingLineCommands::*;
    private import SortingLineSystem::SortingLineMessages::*;

    doc
    /* Nominal mission scenario for SortingLine */
    state def SortingLineNominalMission {
        in sortingLine : SortingLineSystem::SortingLine::SortingLineMachine;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;

        entry;
        then Idle;

        state Idle;
        accept when sortingLine.sensor_SL_in == true then SortCMD;

        state SortCMD;
        accept CommandSuccessEventMessage then SendColorUpdate;

        state SendColorUpdate;
        accept when sortingLine.sensor_SL_blue == true
            do send new BlueTokenAvailableBufferedMessage() to vacuumGripper
            then Idle;
        accept when sortingLine.sensor_SL_white == true
            do send new WhiteTokenAvailableBufferedMessage() to vacuumGripper
            then Idle;
        accept when sortingLine.sensor_SL_red == true
            do send new RedTokenAvailableBufferedMessage() to vacuumGripper
            then Idle;
    }
}
