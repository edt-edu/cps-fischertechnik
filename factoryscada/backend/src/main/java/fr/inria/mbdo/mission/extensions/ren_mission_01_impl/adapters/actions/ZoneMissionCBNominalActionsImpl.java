package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions;

import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominalActions;

/**
 * Default implementation of ZoneMissionCBNominal actions.
 *
 * <p>
 * The mission has no custom action anymore: on an acquire request it publishes the
 * AcquireResponseEventMessage on the zone adapter itself, where the requesting gripper waits for it.
 */
public class ZoneMissionCBNominalActionsImpl implements ZoneMissionCBNominalActions {
}
