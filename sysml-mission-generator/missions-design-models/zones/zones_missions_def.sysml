package ZoneMissions {
    private import Common::*;
    private import ZonesSystem::*;
    private import ZonesSystem::ZonesMessages::*;

    state def ZoneMissionCBNominal {
        doc
        /* Mission for managing zone acquirement on Conveyor Belt between VGR1 and VGR2 for nominal mission */

        // The zone adapter is the channel shared with the grippers: they send their acquire / release requests to
        // it and wait for the acquire response on it (see VacuumGripper missions: "... to zoneCB", "via zoneCB").
        // A request received while the zone is busy is kept, and answered when the zone is released.
        @MainMachineMetadata about zone;
        in zone : ZonesSystem::Zone;

        entry;
        then IdleFree;

        state IdleFree;
        accept AcquireRequestEventMessage via zone
            do send new AcquireResponseEventMessage() to zone
            then IdleBusy;

        state IdleBusy;
        accept ReleaseRequestEventMessage via zone then IdleFree;
        accept AcquireRequestEventMessage via zone then IdleBusyRequested;

        state IdleBusyRequested;
        accept ReleaseRequestEventMessage via zone
            do send new AcquireResponseEventMessage() to zone
            then IdleBusy;
    }

    state def ZoneMissionMPSNominal {
        doc
        /* Mission for managing zone acquirement on the Multi Processing Station between VGR2 and the MPS for nominal mission */

        @MainMachineMetadata about zone;
        in zone : ZonesSystem::Zone;

        entry;
        then IdleFree;

        state IdleFree;
        accept AcquireRequestEventMessage via zone
            do send new AcquireResponseEventMessage() to zone
            then IdleBusy;

        state IdleBusy;
        accept ReleaseRequestEventMessage via zone then IdleFree;
        accept AcquireRequestEventMessage via zone then IdleBusyRequested;

        state IdleBusyRequested;
        accept ReleaseRequestEventMessage via zone
            do send new AcquireResponseEventMessage() to zone
            then IdleBusy;
    }

    state def ZoneMissionDeadCB {
        doc
        /* Zone of the dead conveyor belt: knows which of its two slots (feed, swap) hold a token, and in which order, and
           lets one gripper at a time over the belt */

        @MainMachineMetadata about zone;
        in zone : ZonesSystem::Zone;

        entry;
        then Empty;

        state Empty;
        accept CBSlotRequestEventMessage via zone
            do send new PlaceOnSwapEventMessage() to zone
            then VGR1PlacingOnSwap;
        accept CBTokenRequestEventMessage via zone
            do send new NoTokenOnCBEventMessage() to zone
            then Empty;

        state TokenOnFeed;
        accept CBSlotRequestEventMessage via zone
            do send new PlaceOnSwapEventMessage() to zone
            then VGR1PlacingOnSwapAfterFeed;
        accept CBTokenRequestEventMessage via zone
            do send new PickFromFeedEventMessage() to zone
            then VGR2PickingLastToken;

        state TokenOnSwap;
        accept CBSlotRequestEventMessage via zone
            do send new PlaceOnFeedEventMessage() to zone
            then VGR1PlacingOnFeedAfterSwap;
        accept CBTokenRequestEventMessage via zone
            do send new PickFromSwapEventMessage() to zone
            then VGR2PickingLastToken;

        state FeedThenSwap;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then FeedThenSwap;
        accept CBTokenRequestEventMessage via zone
            do send new PickFromFeedEventMessage() to zone
            then VGR2PickingFeedBeforeSwap;

        state SwapThenFeed;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then SwapThenFeed;
        accept CBTokenRequestEventMessage via zone
            do send new PickFromSwapEventMessage() to zone
            then VGR2PickingSwapBeforeFeed;

        // VGR1 over the belt: VGR2 is refused until VGR1 is done, then told a token waits.
        state VGR1PlacingOnSwap;
        accept TokenPlacedOnCBEventMessage via zone
            do send new TokenOnCBEventMessage() to zone
            then TokenOnSwap;
        accept CBTokenRequestEventMessage via zone
            do send new NoTokenOnCBEventMessage() to zone
            then VGR1PlacingOnSwap;

        state VGR1PlacingOnSwapAfterFeed;
        accept TokenPlacedOnCBEventMessage via zone
            do send new TokenOnCBEventMessage() to zone
            then FeedThenSwap;
        accept CBTokenRequestEventMessage via zone
            do send new NoTokenOnCBEventMessage() to zone
            then VGR1PlacingOnSwapAfterFeed;

        state VGR1PlacingOnFeedAfterSwap;
        accept TokenPlacedOnCBEventMessage via zone
            do send new TokenOnCBEventMessage() to zone
            then SwapThenFeed;
        accept CBTokenRequestEventMessage via zone
            do send new NoTokenOnCBEventMessage() to zone
            then VGR1PlacingOnFeedAfterSwap;

        // VGR2 over the belt: VGR1 is refused until VGR2 is done, then told a slot is free.
        state VGR2PickingLastToken;
        accept TokenPickedFromCBEventMessage via zone
            do send new CBSlotFreeEventMessage() to zone
            then Empty;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then VGR2PickingLastToken;

        state VGR2PickingFeedBeforeSwap;
        accept TokenPickedFromCBEventMessage via zone
            do send new CBSlotFreeEventMessage() to zone
            then TokenOnSwap;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then VGR2PickingFeedBeforeSwap;

        state VGR2PickingSwapBeforeFeed;
        accept TokenPickedFromCBEventMessage via zone
            do send new CBSlotFreeEventMessage() to zone
            then TokenOnFeed;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then VGR2PickingSwapBeforeFeed;
    }
} // package ZoneMissions
