package fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * From ZoneMissions::ZoneMissionCBNominal
 * Zone of the conveyor belt in the nominal production: knows whether a token is on the feed and on the swap, and
 *            lets one gripper at a time over the belt. VGR1 only places on a free feed, VGR2 only picks at the swap.
 */
public interface ZoneMissionCBNominalActions {
  /**
   * TODO: implement transition action for fr.inria.mbdo.mission.ir.TransitionActionCustomIR:ZoneMissions::ZoneMissionCBNominal::checkBelt
   */
  void checkBelt(Event event, Zone zone, ConveyorBeltMachine conveyorBelt);
}
