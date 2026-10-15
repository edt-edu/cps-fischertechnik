package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.FeedFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBTokenRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.NoCBSlotFreeEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.NoTokenOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PickFromSwapEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PlaceOnFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenPickedFromCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenPlacedOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.ConveyorBeltMissionTest.RecordingConveyorBelt;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The nominal CB zone knows whether a token is on the feed and on the swap of the conveyor belt, and lets one gripper
 * at a time over it: VGR1 gets the feed only if it is free (the belt moved the last token to the swap) and VGR2 is not
 * over the belt; VGR2 gets the swap only if a token is there and VGR1 is not over the belt. It starts in the state the
 * light barriers (active-low) give.
 */
class ZoneMissionCBNominalTest {

    private RecordingConveyorBelt cb;
    private ZoneAdapterImpl zoneCB;
    private ZoneMissionCBNominal zone;
    /** The answers and notifications of the zone, in order. */
    private final List<String> answers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cb = new RecordingConveyorBelt();
        zoneCB = new ZoneAdapterImpl("ZoneCB");
        zone = new ZoneMissionCBNominal(zoneCB, cb, new ZoneMissionCBNominalActionsImpl());
        record(PlaceOnFeedEventMessage.class);
        record(NoCBSlotFreeEventMessage.class);
        record(CBSlotFreeEventMessage.class);
        record(PickFromSwapEventMessage.class);
        record(NoTokenOnCBEventMessage.class);
        record(TokenOnCBEventMessage.class);
    }

    private <E extends Event> void record(Class<E> type) {
        zoneCB.subscribe(type, e -> answers.add(type.getSimpleName().replace("EventMessage", "")));
    }

    private void startEmpty() {
        cb.tokens(false, false);
        zone.start();
        answers.clear();
    }

    private void vgr1Asks() {
        zoneCB.publish(new CBSlotRequestEventMessage());
    }

    private void vgr2Asks() {
        zoneCB.publish(new CBTokenRequestEventMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, Empty",
        "true,  false, TokenOnFeed",
        "false, true,  TokenOnSwap",
        "true,  true,  SwapThenFeed"})
    void startsInTheStateTheLightBarriersGive(boolean atFeed, boolean atSwap, String state) {
        cb.tokens(atFeed, atSwap);

        zone.start();

        assertEquals(state, zone.getActiveStateName());
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, Empty",
        "true,  false, TokenOnFeed",
        "false, true,  TokenOnSwap",
        "true,  true,  SwapThenFeed"})
    void migratedToItsCheckStateItTakesTheStateTheLightBarriersGive(boolean atFeed, boolean atSwap, String state) {
        cb.tokens(atFeed, atSwap);

        zone.startAt("CheckBelt"); // e.g. from the dead CB zone, once the belt is repaired

        assertEquals(state, zone.getActiveStateName());
    }

    @Test
    void lightBarriersNotReceivedYetReadAsFree() {
        zone.start();

        assertEquals("Empty", zone.getActiveStateName());
    }

    @Test
    void theCheckTellsTheGrippersWaitingAfterAMigration() {
        cb.tokens(false, true);

        zone.start();

        assertEquals(List.of("CBSlotFree", "TokenOnCB"), answers, "the feed is free, a token is at the swap");
    }

    @Test
    void vgr1GetsTheFeedOnlyOnceTheBeltMovedTheLastTokenToTheSwap() {
        startEmpty();
        vgr1Asks();
        zoneCB.publish(new TokenPlacedOnCBEventMessage());
        assertEquals("TokenOnFeed", zone.getActiveStateName());

        vgr1Asks();
        assertEquals("NoCBSlotFree", answers.get(answers.size() - 1), "the belt still moves the token");

        zoneCB.publish(new FeedFreeEventMessage()); // the belt brought it to the swap
        assertEquals("CBSlotFree", answers.get(answers.size() - 1));
        vgr1Asks();

        assertEquals(List.of("PlaceOnFeed", "NoCBSlotFree", "CBSlotFree", "PlaceOnFeed"), answers);
        assertEquals("VGR1PlacingOnFeedAfterSwap", zone.getActiveStateName());
    }

    @Test
    void neverBothGrippersOverTheBelt() {
        startEmpty();
        vgr1Asks();
        zoneCB.publish(new TokenPlacedOnCBEventMessage());
        zoneCB.publish(new FeedFreeEventMessage()); // a token on the swap
        vgr1Asks();                                 // VGR1 over the belt
        vgr2Asks();
        assertEquals(List.of("PlaceOnFeed", "CBSlotFree", "PlaceOnFeed", "NoTokenOnCB"), answers);

        zoneCB.publish(new TokenPlacedOnCBEventMessage()); // VGR1 out: VGR2 told
        vgr2Asks();                                        // VGR2 over the belt
        vgr1Asks();
        assertEquals(List.of("PlaceOnFeed", "CBSlotFree", "PlaceOnFeed", "NoTokenOnCB", "TokenOnCB", "PickFromSwap",
                "NoCBSlotFree"), answers);
        assertEquals("VGR2PickingSwapBeforeFeed", zone.getActiveStateName());
    }

    @Test
    void vgr2IsRefusedWhenNoTokenIsAtTheSwap() {
        startEmpty();
        vgr2Asks();

        assertEquals(List.of("NoTokenOnCB"), answers);
        assertEquals("Empty", zone.getActiveStateName());
    }

    @Test
    void theBeltMovesTheTokenVgr1PlacedBeforeVgr1Retracted() {
        startEmpty();
        vgr1Asks();
        zoneCB.publish(new FeedFreeEventMessage()); // the token VGR1 placed is already at the swap
        assertEquals("VGR1PlacingOnFeedTokenMovedToSwap", zone.getActiveStateName());
        vgr2Asks();

        zoneCB.publish(new TokenPlacedOnCBEventMessage()); // VGR1 out of the belt

        assertEquals(List.of("PlaceOnFeed", "NoTokenOnCB", "TokenOnCB"), answers);
        assertEquals("TokenOnSwap", zone.getActiveStateName());
    }

    @Test
    void theBeltMovesTheTokenOnTheFeedToTheSwapBeforeVgr2Retracted() {
        cb.tokens(true, true);
        zone.start();
        answers.clear();
        vgr2Asks();
        zoneCB.publish(new FeedFreeEventMessage()); // VGR2 lifted the swap token: the belt moved the next one there
        assertEquals("VGR2PickingSwapFeedMovedToSwap", zone.getActiveStateName());
        vgr1Asks();

        zoneCB.publish(new TokenPickedFromCBEventMessage()); // VGR2 out of the belt

        assertEquals(List.of("PickFromSwap", "NoCBSlotFree", "CBSlotFree"), answers);
        assertEquals("TokenOnSwap", zone.getActiveStateName());
    }

    @Test
    void theFeedTokenStaysUntilTheBeltMovedItAfterVgr2TookTheSwapOne() {
        cb.tokens(true, true);
        zone.start();
        answers.clear();
        vgr2Asks();
        zoneCB.publish(new TokenPickedFromCBEventMessage());
        assertEquals("TokenOnFeed", zone.getActiveStateName());
        vgr1Asks();

        zoneCB.publish(new FeedFreeEventMessage());

        assertEquals(List.of("PickFromSwap", "NoCBSlotFree", "CBSlotFree"), answers);
        assertEquals("TokenOnSwap", zone.getActiveStateName());
    }
}
