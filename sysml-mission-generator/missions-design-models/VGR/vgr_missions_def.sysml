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

        // VGR1 takes the sorted tokens to the conveyor belt feed. It never stacks a token on the feed: holding one, it
        // asks the CB zone (ZoneMissionCBNominal) for the feed, granted only if the feed is free and VGR2 is not over
        // the belt. Else it waits in standby, holding its token, until the zone tells it the feed is usable.
        @MainMachineMetadata about vacuumGripper;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in sortingLine : SortingLineSystem::SortingLine::SortingLineMachine;
        in zoneCB : ZonesSystem::Zone;

        action gotoStandby : GoToPosition;
        action pickBlue : Pick;
        action pickWhite : Pick;
        action pickRed : Pick;
        action placeConveyoBeltFeed : Place;
        // sends TokenRequest to sortingLine (the generator doesn't support a send on a completion transition)
        action askSortingLine;
        // sends CBSlotRequest to zoneCB
        action askCBSlot;

        entry vacuumGripper.setup;
        then SetupCMD;

        state SetupCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then StandbyCMD;

        state StandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper then AskSortingLine;

        // Free: ask the sorting line for the colors waiting at its outputs (sorted while VGR1 was busy: the
        // *TokenAvailable pushed at the end of those sorts was dropped). Idle, the SL answers one *TokenAvailable per
        // output holding a token (VGR1 takes the first, the others are dropped while it picks: it asks again once
        // back), or NoTokenAvailable (ignored in Idle). A request received while the SL sorts is dropped: the sort ends
        // by pushing the color to VGR1, waiting in Idle.
        state AskSortingLine;
        transition first AskSortingLine do askSortingLine then Idle;

        state Idle;
        accept BlueTokenAvailableEventMessage do pickBlue then PickColorCMD;
        accept WhiteTokenAvailableEventMessage do pickWhite then PickColorCMD;
        accept RedTokenAvailableEventMessage do pickRed then PickColorCMD;

        // Holding the token: ask the CB zone for the feed.
        state PickColorCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new CBSlotRequestEventMessage() to zoneCB
            then IdlePicked;

        // CBSlotFree: the request was lost (the CB zone was not running yet), ask again.
        state IdlePicked;
        accept PlaceOnFeedEventMessage via zoneCB do placeConveyoBeltFeed then PlaceConveyorBeltFeed;
        accept NoCBSlotFreeEventMessage via zoneCB do gotoStandby then StandbyWithTokenCMD;
        accept CBSlotFreeEventMessage via zoneCB
            do send new CBSlotRequestEventMessage() to zoneCB
            then IdlePicked;

        // A token is still on the feed, or VGR2 is over the belt: wait in standby, holding the token. A CBSlotFree
        // received while moving is dropped: once in standby, VGR1 asks again.
        state StandbyWithTokenCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper then AskCBSlotInStandby;

        state AskCBSlotInStandby;
        transition first AskCBSlotInStandby do askCBSlot then SelectCBSlotInStandby;

        state SelectCBSlotInStandby;
        accept PlaceOnFeedEventMessage via zoneCB do placeConveyoBeltFeed then PlaceConveyorBeltFeed;
        accept NoCBSlotFreeEventMessage via zoneCB then WaitForFreeCBSlot;
        accept CBSlotFreeEventMessage via zoneCB then AskCBSlotInStandby;

        state WaitForFreeCBSlot;
        accept CBSlotFreeEventMessage via zoneCB then AskCBSlotInStandby;

        // After the place: retract the arm (out of the belt), tell the CB zone, then go to the safe position.
        state PlaceConveyorBeltFeed;
        accept VGRCommandSuccessEventMessage via vacuumGripper do vacuumGripper.retractArm then RetractFromCBCMD;

        state RetractFromCBCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new TokenPlacedOnCBEventMessage() to zoneCB
            then CBZoneReleased;

        state CBZoneReleased;
        transition first CBZoneReleased do gotoStandby then ReturnToStandbyCMD;

        state ReturnToStandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper then AskSortingLine;
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
            then PlaceConveyorBeltFeedCMD;

        state PlaceConveyorBeltFeedCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then StandbyCMD;
    }

    state def VacuumGripper2NominalMission {
        doc
        /* Nominal mission scenario for VacuumGripper n°2 */

        @MainMachineMetadata about vacuumGripper;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in conveyorBelt : ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine;
        in zoneCB : ZonesSystem::Zone;
        in zoneMPS : ZonesSystem::Zone;

        action gotoStandby : GoToPosition;
        action pickCBswap : Pick;
        action placeMPSin : Place;
        // publishes SwapBusy on vacuumGripper if a token waits at the CB swap (light barrier active-low)
        action checkSwap;

        entry vacuumGripper.setup;
        then SetupCMD;

        state SetupCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then StandbyCMD;

        state StandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper then CheckSwap;

        // Free: check the CB swap light barrier. The conveyor belt sends SwapBusy only once, when it brings a token
        // to the swap: if VGR2 was busy then (at the MPS, or holding a token waiting for the MPS zone), the message
        // was dropped. If a token waits at the swap, the check sends SwapBusy again, received in Idle.
        state CheckSwap;
        transition first CheckSwap do checkSwap then Idle;

        // The conveyor belt tells VGR2 that a token arrived at the swap (ConveyorBeltNominalMission). VGR2 then asks
        // the CB zone (ZoneMissionCBNominal), so that it never works over the belt at the same time as VGR1: refused
        // while VGR1 is over the belt, it is told by the zone once VGR1 left it (TokenOnCB).
        state Idle;
        accept SwapBusyEventMessage via vacuumGripper
            do send new CBTokenRequestEventMessage() to zoneCB
            then WaitForCBZoneAcquisition;
        accept TokenOnCBEventMessage via zoneCB
            do send new CBTokenRequestEventMessage() to zoneCB
            then WaitForCBZoneAcquisition;

        // TokenOnCB: the request was lost (the CB zone was not running yet), ask again.
        state WaitForCBZoneAcquisition;
        accept PickFromSwapEventMessage via zoneCB do pickCBswap then PickCBswapCMD;
        accept NoTokenOnCBEventMessage via zoneCB then Idle;
        accept TokenOnCBEventMessage via zoneCB
            do send new CBTokenRequestEventMessage() to zoneCB
            then WaitForCBZoneAcquisition;

        // After the pick: retract the arm (out of the belt), tell the CB zone, then go to the safe position.
        state PickCBswapCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do vacuumGripper.retractArm then RetractFromCBCMD;

        state RetractFromCBCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new TokenPickedFromCBEventMessage() to zoneCB
            then CBZoneReleased;

        state CBZoneReleased;
        transition first CBZoneReleased do gotoStandby then StandbyBeforeMPSCMD;

        // In standby: request the MPS zone, shared with the MPS (MultiProcessingStationNominalMission).
        state StandbyBeforeMPSCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new AcquireRequestEventMessage() to zoneMPS
            then WaitForMPSZoneAcquisition;

        state WaitForMPSZoneAcquisition;
        accept AcquireResponseEventMessage via zoneMPS do placeMPSin then PlaceMPSinCMD;

        // After the place: retract the arm (out of the MPS), release the MPS zone, then go to the safe position.
        state PlaceMPSinCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do vacuumGripper.retractArm then RetractFromMPSCMD;

        state RetractFromMPSCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new ReleaseRequestEventMessage() to zoneMPS
            then MPSZoneReleased;

        state MPSZoneReleased;
        transition first MPSZoneReleased do gotoStandby then ReturnToStandbyCMD;

        // Back in standby: VGR2 holds no zone any more; it takes the token already at the swap, or waits for the next one.
        state ReturnToStandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper then CheckSwap;
    }
} // package VacuumGripperMissions
