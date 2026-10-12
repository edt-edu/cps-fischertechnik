package VacuumGripperCBFailureMissions {
    private import Common::*;
    private import VacuumGripperSystem::*;
    private import VacuumGripperSystem::VacuumGripperCommands::*;
    private import VacuumGripperSystem::VacuumGripperMessages::*;
    private import SortingLineSystem::SortingLineMessages::*;
    private import ZonesSystem::Zone;
    private import ZonesSystem::ZonesMessages::*;
    private import ScalarValues::*;

    // Alternative missions of the grippers when the conveyor belt fails (its belt doesn't move any more). The belt's feed
    // and swap positions become two slots shared by both grippers. The dead CB zone (ZoneMissionDeadCB) knows which
    // slots hold a token, in which order: VGR1, once it holds a token, asks it where to place it; VGR2, once free, asks
    // it where to pick one. Each tells the zone when it is out of the belt again; only one gripper is over the belt at
    // a time. VGR1 also asks the sorting line, when it is free, which colors wait at its outputs.

    state def VacuumGripper1CBFailureMission {
        doc
        /* VGR1 when the conveyor belt fails: takes the tokens at the sorting line outputs to the slot of the belt the dead CB zone gives */

        @MainMachineMetadata about vacuumGripper;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in sortingLine : SortingLineSystem::SortingLine::SortingLineMachine;
        in zoneCB : ZonesSystem::Zone;

        action gotoStandby : GoToPosition;
        action pickBlue : Pick;
        action pickWhite : Pick;
        action pickRed : Pick;
        action placeConveyorBeltFeed : Place;
        action placeConveyorBeltSwap : Place;
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

        // Free: ask the sorting line for the colors waiting at its outputs. It answers when idle; else the sort it
        // runs ends by announcing the color.
        state AskSortingLine;
        transition first AskSortingLine do askSortingLine then Idle;

        state Idle;
        accept BlueTokenAvailableEventMessage do pickBlue then PickColorCMD;
        accept WhiteTokenAvailableEventMessage do pickWhite then PickColorCMD;
        accept RedTokenAvailableEventMessage do pickRed then PickColorCMD;

        state PickColorCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper then AskCBSlot;

        // Holding the token: ask the dead CB zone where to place it.
        state AskCBSlot;
        transition first AskCBSlot do askCBSlot then SelectCBSlot;

        state SelectCBSlot;
        accept PlaceOnFeedEventMessage via zoneCB do placeConveyorBeltFeed then PlaceConveyorBeltCMD;
        accept PlaceOnSwapEventMessage via zoneCB do placeConveyorBeltSwap then PlaceConveyorBeltCMD;
        accept NoCBSlotFreeEventMessage via zoneCB then WaitForFreeCBSlot;

        // Both slots are taken, or VGR2 is over the belt: wait, keeping the token in the gripper, until VGR2 is done.
        state WaitForFreeCBSlot;
        accept CBSlotFreeEventMessage via zoneCB then AskCBSlot;

        state PlaceConveyorBeltCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do vacuumGripper.retractArm then RetractFromCBCMD;

        // Out of the belt: tell the zone (it tells VGR2 a token waits), then go back to standby.
        state RetractFromCBCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new TokenPlacedOnCBEventMessage() to zoneCB
            then CBZoneReleased;

        state CBZoneReleased;
        transition first CBZoneReleased do gotoStandby then ReturnToStandbyCMD;

        state ReturnToStandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper then AskSortingLine;
    }

    state def VacuumGripper2CBFailureMission {
        doc
        /* VGR2 when the conveyor belt fails: takes the tokens from the slot of the belt the dead CB zone gives, oldest first, to the MPS */

        @MainMachineMetadata about vacuumGripper;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in zoneCB : ZonesSystem::Zone;
        in zoneMPS : ZonesSystem::Zone;

        action gotoStandby : GoToPosition;
        action pickCBfeed : Pick;
        action pickCBswap : Pick;
        action placeMPSin : Place;
        // sends CBTokenRequest to zoneCB (the generator doesn't support a send on a completion transition)
        action askCBForToken;
        // sends AcquireRequest to zoneMPS
        action requestMPSZone;

        entry vacuumGripper.setup;
        then SetupCMD;

        state SetupCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do gotoStandby then StandbyCMD;

        state StandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper then CheckCB;

        // Free: ask the dead CB zone where to pick a token.
        state CheckCB;
        transition first CheckCB do askCBForToken then SelectCBSlot;

        state SelectCBSlot;
        accept PickFromFeedEventMessage via zoneCB do pickCBfeed then PickCBCMD;
        accept PickFromSwapEventMessage via zoneCB do pickCBswap then PickCBCMD;
        accept NoTokenOnCBEventMessage via zoneCB then Idle;

        // No token on the belt, or VGR1 is over it: wait until VGR1 is done.
        state Idle;
        accept TokenOnCBEventMessage via zoneCB then CheckCB;

        state PickCBCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do vacuumGripper.retractArm then RetractFromCBCMD;

        // Out of the belt: tell the zone (it tells VGR1 a slot is free), then go to the MPS from there, the arm
        // retracted (no detour by the safe position).
        state RetractFromCBCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new TokenPickedFromCBEventMessage() to zoneCB
            then CBZoneReleased;

        state CBZoneReleased;
        transition first CBZoneReleased do requestMPSZone then WaitForMPSZoneAcquisition;

        state WaitForMPSZoneAcquisition;
        accept AcquireResponseEventMessage via zoneMPS do placeMPSin then PlaceMPSinCMD;

        state PlaceMPSinCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do vacuumGripper.retractArm then RetractFromMPSCMD;

        state RetractFromMPSCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new ReleaseRequestEventMessage() to zoneMPS
            then MPSZoneReleased;

        state MPSZoneReleased;
        transition first MPSZoneReleased do gotoStandby then ReturnToStandbyCMD;

        // Back in standby: ask for the next token.
        state ReturnToStandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper then CheckCB;
    }
} // package VacuumGripperCBFailureMissions
