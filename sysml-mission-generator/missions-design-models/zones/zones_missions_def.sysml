package ZoneMissions {
    private import Common::*;
    private import ZonesSystem::*;
    private import ZonesSystem::ZonesMessages::*;
    private import ConveyorBeltSystem::ConveyorBeltMessages::*;

    state def ZoneMissionCBNominal {
        doc
        /* Zone of the conveyor belt in the nominal production: knows whether a token is on the feed and on the swap, and
           lets one gripper at a time over the belt. VGR1 only places on a free feed, VGR2 only picks at the swap. */

        // The grippers ask the zone (on the zone adapter) before going over the belt, and tell it when they are out of
        // it again, like with the dead CB zone (ZoneMissionDeadCB, same messages, same state names where the meaning
        // matches): VGR1 holding a token sends CBSlotRequest, answered PlaceOnFeed (feed free, nobody over the belt)
        // or NoCBSlotFree, then CBSlotFree once the feed is usable again; it sends TokenPlacedOnCB once retracted.
        // VGR2, told by the belt of a token at the swap, sends CBTokenRequest, answered PickFromSwap or NoTokenOnCB,
        // then TokenOnCB once VGR1 left the belt; it sends TokenPickedFromCB once retracted. The belt
        // (ConveyorBeltNominalMission) sends FeedFree once it moved the token from the feed to the swap: while it
        // moves it, the feed is not free. On the swap, the oldest token: a token on both means SwapThenFeed.
        @MainMachineMetadata about zone;
        in zone : ZonesSystem::Zone;
        in conveyorBelt : ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine;

        // reads the light barriers of the belt (active-low, not received yet = free) and publishes on zone: SwapBusy
        // for a token at the swap, then TokenAtFeed for a token at the feed (they lead Empty to the right state); then
        // CBSlotFree if the feed is free, TokenOnCB if a token is at the swap, for a gripper waiting (after a migration)
        action checkBelt;

        entry;
        then CheckBelt;

        // Started (or migrated) with tokens maybe already on the belt: the light barriers give the state.
        state CheckBelt;
        transition first CheckBelt do checkBelt then Empty;

        // Nobody over the belt.
        state Empty;
        accept CBSlotRequestEventMessage via zone
            do send new PlaceOnFeedEventMessage() to zone
            then VGR1PlacingOnFeed;
        accept CBTokenRequestEventMessage via zone
            do send new NoTokenOnCBEventMessage() to zone
            then Empty;
        accept SwapBusyEventMessage via zone then TokenOnSwap;
        accept TokenAtFeedEventMessage via zone then TokenOnFeed;

        // The belt moves the token to the swap: the feed is not free until it is there.
        state TokenOnFeed;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then TokenOnFeed;
        accept CBTokenRequestEventMessage via zone
            do send new NoTokenOnCBEventMessage() to zone
            then TokenOnFeed;
        accept FeedFreeEventMessage via zone
            do send new CBSlotFreeEventMessage() to zone
            then TokenOnSwap;

        state TokenOnSwap;
        accept CBSlotRequestEventMessage via zone
            do send new PlaceOnFeedEventMessage() to zone
            then VGR1PlacingOnFeedAfterSwap;
        accept CBTokenRequestEventMessage via zone
            do send new PickFromSwapEventMessage() to zone
            then VGR2PickingLastToken;
        accept TokenAtFeedEventMessage via zone then SwapThenFeed;

        // The token on the feed waits for the swap to be free.
        state SwapThenFeed;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then SwapThenFeed;
        accept CBTokenRequestEventMessage via zone
            do send new PickFromSwapEventMessage() to zone
            then VGR2PickingSwapBeforeFeed;

        // VGR1 over the belt, placing on the feed: VGR2 is refused until VGR1 is done, then told if a token waits.
        state VGR1PlacingOnFeed;
        accept TokenPlacedOnCBEventMessage via zone then TokenOnFeed;
        accept CBTokenRequestEventMessage via zone
            do send new NoTokenOnCBEventMessage() to zone
            then VGR1PlacingOnFeed;
        accept FeedFreeEventMessage via zone then VGR1PlacingOnFeedTokenMovedToSwap;

        // The belt already moved the token VGR1 placed to the swap, VGR1 still retracting.
        state VGR1PlacingOnFeedTokenMovedToSwap;
        accept TokenPlacedOnCBEventMessage via zone
            do send new TokenOnCBEventMessage() to zone
            then TokenOnSwap;
        accept CBTokenRequestEventMessage via zone
            do send new NoTokenOnCBEventMessage() to zone
            then VGR1PlacingOnFeedTokenMovedToSwap;

        state VGR1PlacingOnFeedAfterSwap;
        accept TokenPlacedOnCBEventMessage via zone
            do send new TokenOnCBEventMessage() to zone
            then SwapThenFeed;
        accept CBTokenRequestEventMessage via zone
            do send new NoTokenOnCBEventMessage() to zone
            then VGR1PlacingOnFeedAfterSwap;

        // VGR2 over the belt, picking at the swap: VGR1 is refused until VGR2 is done, then told if the feed is free.
        state VGR2PickingLastToken;
        accept TokenPickedFromCBEventMessage via zone
            do send new CBSlotFreeEventMessage() to zone
            then Empty;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then VGR2PickingLastToken;

        // The swap freed, the belt moves the token on the feed to it: VGR1 is told once it is there (TokenOnFeed).
        state VGR2PickingSwapBeforeFeed;
        accept TokenPickedFromCBEventMessage via zone then TokenOnFeed;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then VGR2PickingSwapBeforeFeed;
        accept FeedFreeEventMessage via zone then VGR2PickingSwapFeedMovedToSwap;

        // The belt already moved the token on the feed to the swap, VGR2 still retracting from it.
        state VGR2PickingSwapFeedMovedToSwap;
        accept TokenPickedFromCBEventMessage via zone
            do send new CBSlotFreeEventMessage() to zone
            then TokenOnSwap;
        accept CBSlotRequestEventMessage via zone
            do send new NoCBSlotFreeEventMessage() to zone
            then VGR2PickingSwapFeedMovedToSwap;
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
