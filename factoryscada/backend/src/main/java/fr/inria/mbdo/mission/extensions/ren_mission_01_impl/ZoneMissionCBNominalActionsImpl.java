package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.ZoneMissionCBNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireResponseEventMessage;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ZoneMissionCBNominal actions.
 *
 * <p>
 * When the zone receives an acquire request, it responds with an
 * {@link AcquireResponseEventMessage} to the requesting gripper.
 */
public class ZoneMissionCBNominalActionsImpl implements ZoneMissionCBNominalActions {

    private static final Logger log = LoggerFactory.getLogger(ZoneMissionCBNominalActionsImpl.class);

    @Override
    public void sendAcquireResponseEventMessage(Event event, VacuumGripperMachine vacuumGripper1,
            VacuumGripperMachine vacuumGripper2) {
        log.info("ZoneCB sendAcquireResponseEventMessage triggered by {}", event.getClass().getSimpleName());
    }
}
