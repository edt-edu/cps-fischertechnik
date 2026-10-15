package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.CBSlotRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.PlaceOnFeedEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.TokenPlacedOnCBEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import fr.inria.mbdo.mission.runtime.rtc.exec.RuntimeInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The CB zone mission answers the grippers through the ZoneCB adapter, like VacuumGripper1NominalMission:
 * PickColorCMD --VGRCommandSuccess / send CBSlotRequest to zoneCB--> IdlePicked --PlaceOnFeed via zoneCB--> ...
 */
class ZoneSynchronizationTest {

    /** Stands for the VGRCommandSuccessEventMessage that ends the pick. */
    static final class PickDone implements Event {
    }

    private ZoneAdapterImpl zoneCB;
    private ZoneMissionCBNominal zoneMission;
    private RuntimeInstance gripper;

    @BeforeEach
    void setUp() {
        zoneCB = new ZoneAdapterImpl("ZoneCB");
        zoneMission = new ZoneMissionCBNominal(zoneCB, new ConveyorBeltMissionTest.RecordingConveyorBelt(),
                new ZoneMissionCBNominalActionsImpl());
        zoneMission.start();

        // the requesting side of VacuumGripper1NominalMission
        RuntimeState pickColorCMD = new RuntimeState("PickColorCMD");
        RuntimeState idlePicked = new RuntimeState("IdlePicked");
        RuntimeState placeConveyorBeltFeed = new RuntimeState("PlaceConveyorBeltFeed");
        gripper = new RuntimeInstance();
        gripper.setEntryTransition(new RuntimeTransition(CompletionEvent.class, e -> true, e -> { }, pickColorCMD));
        pickColorCMD.addTransition(new RuntimeTransition(PickDone.class, e -> true,
                e -> zoneCB.publish(new CBSlotRequestEventMessage()), idlePicked));
        idlePicked.addTransition(new RuntimeTransition(PlaceOnFeedEventMessage.class, e -> true, e -> { },
                placeConveyorBeltFeed));
        zoneCB.subscribe(PlaceOnFeedEventMessage.class, gripper::dispatch);
        gripper.start();
    }

    @Test
    void theZoneAnswersARequestSentFromInsideTheRequestersTransition() {
        gripper.dispatch(new PickDone());

        assertEquals("PlaceConveyorBeltFeed", gripper.activeState().getName(),
                "the synchronous answer is processed once the gripper is in IdlePicked");
        assertEquals("VGR1PlacingOnFeed", zoneMission.getActiveStateName());
    }

    @Test
    void theTokenPlacedOccupiesTheFeed() {
        gripper.dispatch(new PickDone());
        zoneCB.publish(new TokenPlacedOnCBEventMessage());

        assertEquals("TokenOnFeed", zoneMission.getActiveStateName());
    }

    @Test
    void anUnexpectedTokenPlacedOnAnEmptyBeltIsIgnored() {
        zoneCB.publish(new TokenPlacedOnCBEventMessage());

        assertEquals("Empty", zoneMission.getActiveStateName());
    }
}
