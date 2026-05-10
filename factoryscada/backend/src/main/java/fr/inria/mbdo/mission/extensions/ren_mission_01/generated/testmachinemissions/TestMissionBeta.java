package fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinemissions;

import java.util.List;

import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachine.TestMachineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.EventMessage2;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.CommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.EventMessage1;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import fr.inria.mbdo.mission.runtime.rtc.exec.*;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From TestMachineMissions::TestMissionBeta
 */
public class TestMissionBeta extends AbstractMissionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(TestMissionBeta.class);

    public TestMissionBeta(TestMachineMachine testMachine1, TestMachineMachine testMachine2) {

        // Actions
        RuntimeAction testAction = evt -> testMachine1.actionB();
        RuntimeAction sendEvent2 = evt -> testMachine1.publish(EventMessage2.now());

        // State refs
        StateBox standbyRef = new StateBox();
        StateBox waitingRef = new StateBox();

        RuntimeTransition initToStandby = new RuntimeTransition(
                CompletionEvent.class, RuntimeGuards.always(), RuntimeActions.noop(), () -> standbyRef.get());

        RuntimeTransition standbyToWaiting = new RuntimeTransition(
                EventMessage1.class, RuntimeGuards.always(), testAction, () -> waitingRef.get());

        RuntimeTransition waitingToStandby = new RuntimeTransition(
                CommandSuccessEventMessage.class, RuntimeGuards.always(), sendEvent2, () -> standbyRef.get());

        RuntimeState init = new RuntimeState("Init", List.of(initToStandby), List.of(), List.of(), false);
        RuntimeState standby = new RuntimeState("Standby", List.of(standbyToWaiting), List.of(), List.of(), false);
        RuntimeState waiting = new RuntimeState("WaitingForA", List.of(waitingToStandby), List.of(), List.of(), false);

        standbyRef.set(standby);
        waitingRef.set(waiting);

        this.runtime.setInitialState(init);

        testMachine1.subscribe(EventMessage1.class, super::onEvent);
        testMachine1.subscribe(CommandSuccessEventMessage.class, this::onEvent);

        logger.info("TestMissionBeta initialized and subscribed to events");
    }
}
