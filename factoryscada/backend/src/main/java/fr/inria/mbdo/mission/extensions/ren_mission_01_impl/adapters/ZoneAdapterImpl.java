package fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters;

import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.AbstractZoneAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub adapter for a Zone (mutual-exclusion area).
 *
 * <p>
 * Zones are software-only constructs that arbitrate access between grippers.
 * This adapter maintains occupancy state and publishes/subscribes events
 * through
 * the standard {@link AbstractZoneAdapter} event bus.
 */
public class ZoneAdapterImpl extends AbstractZoneAdapter {

    private static final Logger log = LoggerFactory.getLogger(ZoneAdapterImpl.class);

    public ZoneAdapterImpl(String id) {
        super(id);
    }

    @Override
    public void setIsOccupied(boolean isOccupied) {
        super.setIsOccupied(isOccupied);
        log.info("[{}] setIsOccupied({})", id, isOccupied);
    }
}
