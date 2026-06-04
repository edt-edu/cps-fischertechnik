package fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.transitionusage.AcceptWhenIdleToMovingToSensorEvent;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.CBCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import java.lang.Override;
import java.lang.String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From ConveyorBeltMissions::ConveyorBeltNominalMission
 */
public class ConveyorBeltNominalMission extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger(ConveyorBeltNominalMission.class);

  private final ConveyorBeltNominalMissionActions actions;

  private final ConveyorBeltMachine conveyorBelt;

  private final VacuumGripperMachine vacuumGripper1;

  private final VacuumGripperMachine vacuumGripper2;

  public ConveyorBeltNominalMission(ConveyorBeltMachine conveyorBelt,
      VacuumGripperMachine vacuumGripper1, VacuumGripperMachine vacuumGripper2,
      ConveyorBeltNominalMissionActions actions) {
    this.conveyorBelt = conveyorBelt;
    this.vacuumGripper1 = vacuumGripper1;
    this.vacuumGripper2 = vacuumGripper2;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState idle = new RuntimeState("Idle");
    RuntimeState movingToSensor = new RuntimeState("MovingToSensor");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> { }, idle));
    idle.addTransition(new RuntimeTransition(AcceptWhenIdleToMovingToSensorEvent.class, event -> true, event -> this.conveyorBelt.moveToSensor(), movingToSensor));
    movingToSensor.addTransition(new RuntimeTransition(CBCommandSuccessEventMessage.class, event -> true, event -> this.actions.performActionUsage(event, this.conveyorBelt, this.vacuumGripper1, this.vacuumGripper2), idle));

    // Subscribe each mission machine to trigger event types used by this mission.
    conveyorBelt.subscribe(CBCommandSuccessEventMessage.class, this::onEvent);
  }

  @Override
  public String getName() {
    return "ConveyorBeltNominalMission";
  }
}
