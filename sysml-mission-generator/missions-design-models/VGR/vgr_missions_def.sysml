package VacuumGripperMissions {
    private import Common::*;
    private import VacuumGripperSystem::*;
    private import VacuumGripperSystem::VacuumGripperCommands::*;
    private import VacuumGripperSystem::VacuumGripperMessages::*;
    private import SortingLineSystem::SortingLineCommands::*;
    private import SortingLineSystem::SortingLineMessages::*;
    private import ZonesSystem::Zone;
    private import ZonesSystem::ZonesMessages::*;
    private import MultiProcessingStationSystem::MultiProcessingStationMessages::*;
    private import ConveyorBeltSystem::ConveyorBeltMessages::*;
    private import ScalarValues::*;

    state def VacuumGripper1NominalMission {
        doc
        /* Secondary mission scenario for VacuumGripper n°1 */

        @MainMachineMetadata about vacuumGripper;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in sortingLine : SortingLineSystem::SortingLine::SortingLineMachine;
        in zoneCB : ZonesSystem::Zone;

        action gotoStandby : GoToPosition;
        action pickBlue : Pick;
        action pickWhite : Pick;
        action pickRed : Pick;
        action placeConveyoBeltFeed : Place;

        entry vacuumGripper.setup;
        then SetupCMD;

        state SetupCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then StandbyCMD;

        state StandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new ReleaseRequestEventMessage() to zoneCB
            then Idle;

        state Idle;
        accept BlueTokenAvailableEventMessage do pickBlue then PickColorCMD;
        accept WhiteTokenAvailableEventMessage do pickWhite then PickColorCMD;
        accept RedTokenAvailableEventMessage do pickRed then PickColorCMD;

        state PickColorCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new AcquireRequestEventMessage() to zoneCB
            then IdlePicked;

        state IdlePicked;
        accept AcquireResponseEventMessage via zoneCB
            do placeConveyoBeltFeed
            then PlaceConveyorBeltFeed;

        state PlaceConveyorBeltFeed;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then StandbyCMD;
    }

    state def VacuumGripper1SecondaryMission {
        doc
        /* Nominal mission scenario for VacuumGripper n°1 */

        @MainMachineMetadata about vacuumGripper;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in sortingLine : SortingLineSystem::SortingLine::SortingLineMachine;
        in zoneCB : ZonesSystem::Zone;

        action gotoStandby : GoToPosition;
        action gotoBluePosition : GoToPosition;
        action gotoWhitePosition : GoToPosition;
        action gotoRedPosition : GoToPosition;
        action pickColor : Pick;
        action placeConveyoBeltFeed : Place;

        entry vacuumGripper.setup;
        then SetupCMD;

        state SetupCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then StandbyCMD;

        state StandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new ReleaseRequestEventMessage() to zoneCB
            then Idle;

        state Idle;
        accept BlueTokenAvailableEventMessage do gotoBluePosition then GotoColorCMD;
        accept WhiteTokenAvailableEventMessage do gotoWhitePosition then GotoColorCMD;
        accept RedTokenAvailableEventMessage do gotoRedPosition then GotoColorCMD;

        state GotoColorCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do pickColor then IdlePicked;

        state PickColorCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new AcquireRequestEventMessage() to zoneCB
            then IdlePicked;

        state IdlePicked;
        accept AcquireResponseEventMessage via zoneCB
            do placeConveyoBeltFeed
            then PlaceConveyorBeltFeed;

        state PlaceConveyorBeltFeed;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then StandbyCMD;
    }

    state def VacuumGripper2NominalMission {
        doc
        /* Nominal mission scenario for VacuumGripper n°2 */

        @MainMachineMetadata about vacuumGripper;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in zoneCB : ZonesSystem::Zone;
        in zoneMPS : ZonesSystem::Zone;

        action gotoStandby : GoToPosition;
        action pickCBswap : Pick;
        action placeMPSin : Place;

        action releaseCBZoneAndAcquireMPSZone {
            send new ReleaseRequestEventMessage() to zoneCB;
            send new AcquireRequestEventMessage() to zoneMPS;
        }

        entry vacuumGripper.setup;
        then SetupIdle;

        state SetupIdle;
        accept VGRCommandSuccessEventMessage do gotoStandby then GotoStandbyCMD;

        state GotoStandbyCMD;
        accept VGRCommandSuccessEventMessage then Idle;

        // The conveyor belt tells VGR2 that a token arrived at the swap (ConveyorBeltNominalMission).
        // VGR2 then waits for the CB zone, so that it never works over the belt at the same time as VGR1.
        state Idle;
        accept SwapBusyEventMessage via vacuumGripper
            do send new AcquireRequestEventMessage() to zoneCB
            then WaitForCBZoneAcquisition;

        state WaitForCBZoneAcquisition;
        accept AcquireResponseEventMessage via zoneCB do pickCBswap then PickCBswapCMD;

        state PickCBswapCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then ReleaseCBZone;

        state ReleaseCBZone;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do releaseCBZoneAndAcquireMPSZone
            then WaitForMPSZoneAcquisition;

        state WaitForMPSZoneAcquisition;
        accept AcquireResponseEventMessage via zoneMPS do placeMPSin then PlaceMPSinCMD;

        state PlaceMPSinCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then ReleaseMPSZone;

        state ReleaseMPSZone;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new ReleaseRequestEventMessage() to zoneMPS
            then GotoStandbyCMD;
    }
} // package VacuumGripperMissions
