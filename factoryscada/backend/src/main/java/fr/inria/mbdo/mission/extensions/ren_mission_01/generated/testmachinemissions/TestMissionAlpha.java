package fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinemissions;

import java.util.List;

import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachine.TestMachineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.TestMachineAttAAbove10EventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.CommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.EventMessage1;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.EventMessage2;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import fr.inria.mbdo.mission.runtime.rtc.exec.*;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From TestMachineMissions::TestMissionAlpha
 */
public class TestMissionAlpha extends AbstractMissionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(TestMissionAlpha.class);

    public TestMissionAlpha(TestMachineMachine testMachine1, TestMachineMachine testMachine2) {

        // Actions
        RuntimeAction entryInit = evt -> testMachine1.actionA();
        RuntimeAction sendEventMessage1 = evt -> testMachine2.publish(EventMessage1.now());

        // Guards
        // State refs
        StateBox setupRef = new StateBox();
        StateBox idleRef = new StateBox();
        StateBox waitingRef = new StateBox();

        RuntimeTransition initToSetup = new RuntimeTransition(
                CompletionEvent.class,
                RuntimeGuards.always(),
                entryInit,
                () -> setupRef.get());

        RuntimeTransition setupToIdle = new RuntimeTransition(
                CommandSuccessEventMessage.class,
                RuntimeGuards.always(),
                RuntimeActions.noop(),
                () -> idleRef.get());

        RuntimeTransition idleToWaiting = new RuntimeTransition(
                TestMachineAttAAbove10EventMessage.class,
                RuntimeGuards.always(),
                sendEventMessage1,
                () -> waitingRef.get());

        RuntimeTransition waitingToIdle = new RuntimeTransition(
                EventMessage2.class,
                RuntimeGuards.always(),
                RuntimeActions.noop(),
                () -> idleRef.get());

        RuntimeState init = new RuntimeState("Init", List.of(initToSetup), List.of(), List.of(), false);
        RuntimeState setup = new RuntimeState("Setup", List.of(setupToIdle), List.of(), List.of(), false);
        RuntimeState idle = new RuntimeState("Idle", List.of(idleToWaiting), List.of(), List.of(), false);
        RuntimeState waiting = new RuntimeState("WaitingForB", List.of(waitingToIdle), List.of(), List.of(), false);

        setupRef.set(setup);
        idleRef.set(idle);
        waitingRef.set(waiting);

        this.runtime.setInitialState(init);

        testMachine1.subscribe(CommandSuccessEventMessage.class, this::onEvent);
        testMachine1.subscribe(TestMachineAttAAbove10EventMessage.class, this::onEvent);
        testMachine2.subscribe(EventMessage2.class, this::onEvent);

        logger.info("TestMissionAlpha initialized and subscribed to events");
    }

}
