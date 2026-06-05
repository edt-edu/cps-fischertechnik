package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.api.AbstractAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub adapter for a Zone (mutual-exclusion area).
 *
 * <p>
 * Zones are software-only constructs that arbitrate access between grippers.
 * This adapter maintains occupancy state and publishes/subscribes events
 * through
 * the standard {@link AbstractAdapter} event bus.
 */
public class ZoneAdapterImpl extends AbstractAdapter implements Zone {

    private static final Logger log = LoggerFactory.getLogger(ZoneAdapterImpl.class);

    private volatile boolean isOccupied;

    public ZoneAdapterImpl(String id) {
        super(id);
    }

    @Override
    public boolean getIsOccupied() {
        return isOccupied;
    }

    @Override
    public void setIsOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
        log.info("[{}] setIsOccupied({})", id, isOccupied);
    }
}
