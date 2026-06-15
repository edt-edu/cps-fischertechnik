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

        entry;
        then Idle;

        state Idle;
        accept when sortingLine.sensor_SL_in == true then SortCMD;

        state SortCMD;
        accept SLCommandSuccessEventMessage then SendColorUpdate;

        state SendColorUpdate;
        accept when sortingLine.sensor_SL_blue == true
            do send new BlueTokenAvailableEventMessage() to vacuumGripper
            then Idle;
        accept when sortingLine.sensor_SL_white == true
            do send new WhiteTokenAvailableEventMessage() to vacuumGripper
            then Idle;
        accept when sortingLine.sensor_SL_red == true
            do send new RedTokenAvailableEventMessage() to vacuumGripper
            then Idle;
    }
} // package SortingLineMissions
