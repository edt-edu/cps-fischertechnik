package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireResponseEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.ReleaseRequestEventMessage;
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
 * PickColorCMD --VGRCommandSuccess / send AcquireRequest to zoneCB--> IdlePicked --AcquireResponse via zoneCB--> ...
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
        zoneMission = new ZoneMissionCBNominal(zoneCB, new ZoneMissionCBNominalActionsImpl());
        zoneMission.start();

        // the requesting side of VacuumGripper1NominalMission
        RuntimeState pickColorCMD = new RuntimeState("PickColorCMD");
        RuntimeState idlePicked = new RuntimeState("IdlePicked");
        RuntimeState placeConveyorBeltFeed = new RuntimeState("PlaceConveyorBeltFeed");
        gripper = new RuntimeInstance();
        gripper.setEntryTransition(new RuntimeTransition(CompletionEvent.class, e -> true, e -> { }, pickColorCMD));
        pickColorCMD.addTransition(new RuntimeTransition(PickDone.class, e -> true,
                e -> zoneCB.publish(new AcquireRequestEventMessage()), idlePicked));
        idlePicked.addTransition(new RuntimeTransition(AcquireResponseEventMessage.class, e -> true, e -> { },
                placeConveyorBeltFeed));
        zoneCB.subscribe(AcquireResponseEventMessage.class, gripper::dispatch);
        gripper.start();
    }

    @Test
    void theZoneAnswersAnAcquireRequestSentFromInsideTheRequestersTransition() {
        gripper.dispatch(new PickDone());

        assertEquals("PlaceConveyorBeltFeed", gripper.activeState().getName(),
                "the synchronous answer is processed once the gripper is in IdlePicked");
        assertEquals("IdleBusy", zoneMission.getActiveStateName());
    }

    @Test
    void releaseFreesTheZoneAgain() {
        gripper.dispatch(new PickDone());
        zoneCB.publish(new ReleaseRequestEventMessage());

        assertEquals("IdleFree", zoneMission.getActiveStateName());
    }

    @Test
    void aReleaseWhileFreeIsIgnored() {
        zoneCB.publish(new ReleaseRequestEventMessage()); // VGR1 releases the zone after its setup

        assertEquals("IdleFree", zoneMission.getActiveStateName());
    }
}
