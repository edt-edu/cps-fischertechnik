package fr.inria.mbdo.mission.runtime.api;

import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireRequestEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.zonesmessages.AcquireResponseEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Commands sent by adapters are logged in the mission whose transition sent them. */
class MissionCommandLogTest {

    static final class CommandingAdapter extends AbstractMachineAdapter {
        CommandingAdapter() {
            super("VacuumGripper01");
        }

        void pick() {
            commandSent("pick(SL_OUTPUT_BLUE)");
        }
    }

    @Test
    void aCommandSentFromATransitionIsLoggedInItsMission() {
        ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");
        ZoneMissionCBNominal zoneMission = new ZoneMissionCBNominal(zoneCB, new ZoneMissionCBNominalActionsImpl());
        List<String> logs = new ArrayList<>();
        zoneMission.setLogListener(logs::add);
        zoneMission.start();

        // sent while the zone mission executes its IdleFree -> IdleBusy transition
        CommandingAdapter vgr1 = new CommandingAdapter();
        zoneCB.subscribe(AcquireResponseEventMessage.class, response -> vgr1.pick());
        zoneCB.publish(new AcquireRequestEventMessage());

        assertTrue(logs.stream().anyMatch(entry -> entry.endsWith("[ZoneMissionCBNominal] command: VacuumGripper01 pick(SL_OUTPUT_BLUE)")),
                () -> "logs: " + logs);
    }

    @Test
    void aCommandSentOutsideAnyMissionIsNotLogged() {
        ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");
        ZoneMissionCBNominal zoneMission = new ZoneMissionCBNominal(zoneCB, new ZoneMissionCBNominalActionsImpl());
        List<String> logs = new ArrayList<>();
        zoneMission.setLogListener(logs::add);
        zoneMission.start();
        int before = logs.size();

        new CommandingAdapter().pick();

        assertEquals(before, logs.size());
    }
}
