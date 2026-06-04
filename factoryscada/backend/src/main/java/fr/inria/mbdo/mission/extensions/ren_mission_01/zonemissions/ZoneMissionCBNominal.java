package fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.ReleaseRequestEventMessage;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeState;
import fr.inria.mbdo.mission.runtime.rtc.def.RuntimeTransition;
import fr.inria.mbdo.mission.runtime.rtc.event.CompletionEvent;
import java.lang.Override;
import java.lang.String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From ZoneMissions::ZoneMissionCBNominal
 */
public class ZoneMissionCBNominal extends AbstractMissionStrategy {
  private static final Logger logger = LoggerFactory.getLogger(ZoneMissionCBNominal.class);

  private final ZoneMissionCBNominalActions actions;

  private final VacuumGripperMachine vacuumGripper1;

  private final VacuumGripperMachine vacuumGripper2;

  public ZoneMissionCBNominal(VacuumGripperMachine vacuumGripper1,
      VacuumGripperMachine vacuumGripper2, ZoneMissionCBNominalActions actions) {
    this.vacuumGripper1 = vacuumGripper1;
    this.vacuumGripper2 = vacuumGripper2;
    this.actions = actions;

    // States are built from the collected transitions.
    RuntimeState idleFree = new RuntimeState("IdleFree");
    RuntimeState idleBusy = new RuntimeState("IdleBusy");

    // Transitions connect triggers, runtime actions, and next-state targets.
    this.runtime.setEntryTransition(new RuntimeTransition(CompletionEvent.class, event -> true, event -> { }, idleFree));
    idleFree.addTransition(new RuntimeTransition(AcquireRequestEventMessage.class, event -> true, event -> this.actions.sendAcquireResponseEventMessage(event, this.vacuumGripper1, this.vacuumGripper2), idleBusy));
    idleBusy.addTransition(new RuntimeTransition(ReleaseRequestEventMessage.class, event -> true, event -> { }, idleFree));

    // Subscribe each mission machine to trigger event types used by this mission.
  }

  @Override
  public String getName() {
    return "ZoneMissionCBNominal";
  }
}
