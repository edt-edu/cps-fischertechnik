package io.github.mbdo.factoryscada.service;

import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.AbstractVacuumGripperMachineAdapter;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands.Position3D;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ConveyorBeltAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.ZoneAdapterImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.VacuumGripper2NominalMissionActionsImpl;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.ZoneMissionCBNominalActionsImpl;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.config.FactoryMissionExtension;
import fr.inria.mbdo.mission.runtime.config.MachineMissionBinding;
import fr.inria.mbdo.mission.runtime.config.MissionExtensionConfig;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import io.github.mbdo.factoryscada.core.dtos.Parameter;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import io.github.mbdo.factoryscada.frontend.dto.MissionChangeEventDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionExtensionConfigurationDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionMachineDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MachineCommandRequestDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MachineCommandResultDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MachineRequestDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MissionChangeDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MissionStateMismatchException;
import io.github.mbdo.factoryscada.frontend.dto.replanning.MissionSwapRequestDTO;
import io.github.mbdo.factoryscada.frontend.dto.replanning.ReplanningMachineDTO;
import io.github.mbdo.factoryscada.socket.Protocol;
import io.github.mbdo.factoryscada.utilities.CommandIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The replanning API of the SCADA: the missions of each machine and their states, mission swaps (possibly starting the
 * new mission in a given state), and direct commands waiting for their feedback.
 */
class ReplanningServiceTest {

    private static final String VGR2 = "REN_MISSION_01/VGR02";

    /** The PLC: records the commands sent, and runs {@link #onSend} on each (e.g. to answer it). */
    static final class FakePlc implements Protocol {
        final List<String> sent = new ArrayList<>();
        Consumer<String> onSend = json -> { };

        @Override public void send(String message) { sent.add(message); onSend.accept(message); }
        @Override public String receive() { return null; }
        @Override public void start() { }
        @Override public void stop() { }
        @Override public boolean isSendChannelConnected() { return true; }
        @Override public boolean isReceivedChannelConnected() { return true; }
    }

    /** The mission side of VGR2: its commands are not observed here. */
    static final class IdleGripperAdapter extends AbstractVacuumGripperMachineAdapter {
        IdleGripperAdapter() { super(VGR2); }
        @Override public void goToPosition(Position3D targetPosition) { }
        @Override public void move(Position3D startPosition, Position3D endPosition) { }
        @Override public void pick(Position3D targetPosition) { }
        @Override public void place(Position3D targetPosition) { }
        @Override public void goToNamedPosition(String positionName) { }
        @Override public void pickNamed(String positionName) { }
        @Override public void placeNamed(String positionName) { }
        @Override public void setup() { }
        @Override public void statusRequest() { }
        @Override public void grip() { }
        @Override public void release() { }
        @Override public void stop() { }
        @Override public void moveToSafePosition() { }
        @Override public void retractArm() { }
    }

    private FakePlc plc;
    private VacuumGripperMachine physicalVgr2;
    private MissionExtensionService missions;
    private ReplanningService service;
    private VacuumGripper2NominalMission vgr2Nominal;
    private ZoneMissionCBNominal zoneCBMission;

    @BeforeEach
    void setUp() {
        plc = new FakePlc();
        physicalVgr2 = new VacuumGripperMachine(
            new AbstractMachine.Parameters("VacuumGripper02", plc, List.of(), new CommandIdGenerator(), null));
        FactoryScada factoryScada = mock(FactoryScada.class);
        when(factoryScada.getMachineLastCommandStatusMap()).thenReturn(new HashMap<>());
        when(factoryScada.getTemplate()).thenReturn(mock(SimpMessagingTemplate.class));
        physicalVgr2.setFactoryScada(factoryScada);

        ZoneAdapterImpl zoneCB = new ZoneAdapterImpl("ZoneCB");
        ZoneAdapterImpl zoneMPS = new ZoneAdapterImpl("ZoneMPS");
        ConveyorBeltAdapterImpl cb =
            new ConveyorBeltAdapterImpl("REN_MISSION_01/CB01", mock(MqttMessageRouter.class), "PLC/+/ConveyorBelt/x/#");
        vgr2Nominal = new VacuumGripper2NominalMission(new IdleGripperAdapter(), cb, zoneCB, zoneMPS,
            new VacuumGripper2NominalMissionActionsImpl());
        zoneCBMission = new ZoneMissionCBNominal(zoneCB, cb, new ZoneMissionCBNominalActionsImpl());

        MissionExtensionConfig config = new FixedConfig(List.of(vgr2Nominal, zoneCBMission), List.of(
            new MachineMissionBinding(VGR2, "VacuumGripperMachine", List.of("VacuumGripper2NominalMission")),
            new MachineMissionBinding("ZoneCB", "Zone", List.of("ZoneMissionCBNominal"))));
        missions = new MissionExtensionService(config, mock(SimpMessagingTemplate.class));

        MachineNameMappingService names = mock(MachineNameMappingService.class);
        when(names.getMachineForLogicalName(VGR2)).thenReturn("VacuumGripper02");
        when(names.getMachineForLogicalName("ZoneCB")).thenReturn("ZoneCB");
        Map<String, AbstractMachine> physicalMachines = Map.of("VacuumGripper02", physicalVgr2);
        service = new ReplanningService(missions, names, physicalMachines::get, new CommandIdGenerator()::generateId);
    }

    @Test
    void listsTheMissionsOfEachMachineWithTheirStates() {
        List<ReplanningMachineDTO> machines = service.getMachines();

        assertEquals(List.of(VGR2, "ZoneCB"), machines.stream().map(ReplanningMachineDTO::name).toList());
        ReplanningMachineDTO vgr2 = machines.get(0);
        assertEquals("VacuumGripper02", vgr2.physicalMachine());
        assertNull(vgr2.activeMission());
        assertEquals("VacuumGripper2NominalMission", vgr2.availableMissions().get(0).name());
        assertTrue(vgr2.availableMissions().get(0).states().contains("PickCBswapCMD"));
        assertNull(machines.get(1).physicalMachine(), "a zone has no physical machine");
    }

    @Test
    void swapsTheMissionOfAMachineAndStartsItInTheGivenState() {
        swap("VacuumGripper2NominalMission", "Idle");

        ReplanningMachineDTO vgr2 = service.getMachines().get(0);
        assertEquals("VacuumGripper2NominalMission", vgr2.activeMission());
        assertEquals("Idle", vgr2.activeState());
    }

    @Test
    void refusesAMissionTheMachineIsNotBoundTo() {
        assertThrows(IllegalArgumentException.class, () -> swap("ZoneMissionCBNominal", null));
    }

    @Test
    void refusesAStateTheMissionDoesNotHave() {
        assertThrows(IllegalArgumentException.class, () -> swap("VacuumGripper2NominalMission", "NoSuchState"));
        assertFalse(vgr2Nominal.isRunning());
    }

    @Test
    void stopsTheMissionOfAMachine() {
        swap("VacuumGripper2NominalMission", "Idle");

        MissionChangeDTO change = service.stopMission(new MachineRequestDTO(VGR2, null, null));

        assertEquals("VacuumGripper2NominalMission", change.previousMission());
        assertEquals("Idle", change.previousState(), "the state it was stopped in");
        assertFalse(vgr2Nominal.isRunning());
        assertNull(service.getMachines().get(0).activeMission());
    }

    @Test
    void theConfigurationShowsEachMachinesMissionStateAndChanges() {
        swap("VacuumGripper2NominalMission", "Idle");
        service.stopMission(new MachineRequestDTO(VGR2, null, null));

        MissionExtensionConfigurationDTO configuration = missions.getConfiguration();
        assertEquals(2, configuration.missionChanges().size());
        MissionChangeEventDTO stop = configuration.missionChanges().get(0);
        assertEquals(VGR2, stop.machine());
        assertEquals("VacuumGripper2NominalMission", stop.previousMission());
        assertEquals("Idle", stop.previousState());
        assertNull(stop.mission());
        assertEquals("REPLANNING", stop.origin(), "done through the replanning API");
        assertEquals("Idle", configuration.missionChanges().get(1).state(), "the swap, before the stop");

        swap("VacuumGripper2NominalMission", "PickCBswapCMD");
        MissionMachineDTO vgr2 = missions.getConfiguration().machines().stream()
            .filter(m -> m.name().equals(VGR2)).findFirst().orElseThrow();
        assertEquals("VacuumGripper2NominalMission", vgr2.activeMissionName());
        assertEquals("PickCBswapCMD", vgr2.activeState());
        assertEquals("VacuumGripper2NominalMission", vgr2.nominalMissionName());
        assertTrue(vgr2.lastChangeTime() != null);
    }

    @Test
    void swapsOnlyFromTheExpectedMissionAndState() {
        swap("VacuumGripper2NominalMission", "Idle");

        assertThrows(MissionStateMismatchException.class, () -> service.swapMission(new MissionSwapRequestDTO(
            VGR2, "VacuumGripper2NominalMission", "SetupCMD", "VacuumGripper2NominalMission", "PickCBswapCMD")));
        assertEquals("Idle", vgr2Nominal.getActiveStateName(), "left as it was");

        MissionChangeDTO change = service.swapMission(new MissionSwapRequestDTO(
            VGR2, "VacuumGripper2NominalMission", "PickCBswapCMD", "VacuumGripper2NominalMission", "Idle"));
        assertEquals("Idle", change.previousState());
        assertEquals("PickCBswapCMD", change.state());
    }

    @Test
    void stopsOnlyFromTheExpectedMission() {
        assertThrows(MissionStateMismatchException.class,
            () -> service.stopMission(new MachineRequestDTO(VGR2, "VacuumGripper2NominalMission", null)));

        MissionChangeDTO change = service.stopMission(new MachineRequestDTO(VGR2, "", null));
        assertNull(change.previousMission(), "\"\" expects no mission running");
    }

    @Test
    void stoppingTheGlobalMissionStopsEveryRunningMission() {
        swap("VacuumGripper2NominalMission", "Idle");
        zoneCBMission.start(); // running, but not started through the service

        missions.stopGlobalMission();

        assertFalse(vgr2Nominal.isRunning());
        assertFalse(zoneCBMission.isRunning());
        assertTrue(missions.getConfiguration().activeMissionNames().isEmpty());
    }

    @Test
    void sendsACommandAndWaitsForItsFeedback() {
        plc.onSend = json -> physicalVgr2.notifyCommandFeedback(true, "DONE");

        MachineCommandResultDTO result = service.executeCommand(new MachineCommandRequestDTO(
            VGR2, "retractArm", List.of(), 1000L));

        assertTrue(result.done());
        assertEquals("DONE", result.status());
        assertEquals(1, plc.sent.size());
        assertTrue(plc.sent.get(0).contains("\"name\":\"RETRACT_ARM\""), plc.sent.get(0));
        assertTrue(plc.sent.get(0).contains("\"topicName\":\"VacuumGripper02\""), plc.sent.get(0));
    }

    @Test
    void sendsTheParametersOfTheCommand() {
        plc.onSend = json -> physicalVgr2.notifyCommandFeedback(true, "DONE");

        service.executeCommand(new MachineCommandRequestDTO(VGR2, "pick", List.of(Parameter.namedPosition("CB")), 1000L));

        assertTrue(plc.sent.get(0).contains("\"name\":\"PICK\""), plc.sent.get(0));
        assertTrue(plc.sent.get(0).contains("NAMEDPOSITION"), plc.sent.get(0));
    }

    @Test
    void reportsAFailedCommand() {
        plc.onSend = json -> physicalVgr2.notifyCommandFeedback(false, "ABORTED_ERROR");

        MachineCommandResultDTO result = service.executeCommand(new MachineCommandRequestDTO(
            VGR2, "pick", List.of(Parameter.namedPosition("NOWHERE")), 1000L));

        assertFalse(result.done());
        assertEquals("ABORTED_ERROR", result.status());
    }

    @Test
    void reportsATimeoutWithoutFeedback() {
        MachineCommandResultDTO result = service.executeCommand(new MachineCommandRequestDTO(
            VGR2, "move_to_safe_position", List.of(), 200L));

        assertFalse(result.done());
        assertEquals("TIMEOUT", result.status());
    }

    @Test
    void waitsForABusyMachineBeforeACommand() {
        physicalVgr2.setIdle(false);

        MachineCommandResultDTO result = service.executeCommand(new MachineCommandRequestDTO(
            VGR2, "retractArm", List.of(), 200L));

        assertEquals("BUSY", result.status());
        assertTrue(plc.sent.isEmpty());
    }

    @Test
    void aStopInterruptsTheCommandInProgressAtOnce() {
        physicalVgr2.setIdle(false); // e.g. a belt moving to a sensor it never reaches
        plc.onSend = json -> {
            physicalVgr2.notifyCommandFeedback(false, "INTERRUPTED"); // the command in progress
            physicalVgr2.notifyCommandFeedback(true, "DONE"); // the stop
        };

        MachineCommandResultDTO result = service.executeCommand(new MachineCommandRequestDTO(
            VGR2, "stop", List.of(), 1000L));

        assertTrue(result.done());
        assertEquals("DONE", result.status());
        assertTrue(plc.sent.get(0).contains("\"name\":\"STOP\""), plc.sent.get(0));
    }

    @Test
    void aForceStopStopsTheMissionsInTheirStateThenStopsEveryMachineAtOnce() {
        swap("VacuumGripper2NominalMission", "PickCBswapCMD");
        physicalVgr2.setIdle(false); // the pick in progress; no feedback comes

        missions.stopGlobalMission();
        List<String> stopped = service.stopAllMachines();

        assertFalse(vgr2Nominal.isRunning(), "stopped without waiting for the pick to end");
        assertEquals(List.of("VacuumGripper02"), stopped, "the zones have no machine to stop");
        assertEquals(1, plc.sent.size());
        assertTrue(plc.sent.get(0).contains("\"name\":\"STOP\""), plc.sent.get(0));
    }

    @Test
    void refusesACommandWhileAMissionRunsOnTheMachine() {
        swap("VacuumGripper2NominalMission", "Idle");

        assertThrows(IllegalStateException.class, () -> service.executeCommand(
            new MachineCommandRequestDTO(VGR2, "retract_arm", List.of(), 200L)));
        assertTrue(plc.sent.isEmpty());
    }

    @Test
    void refusesAnUnknownCommand() {
        assertThrows(IllegalArgumentException.class, () -> service.executeCommand(
            new MachineCommandRequestDTO(VGR2, "fly", List.of(), 200L)));
    }

    @Test
    void wireNamesAreUpperSnakeCase() {
        assertEquals("RETRACT_ARM", ReplanningService.wireCommandName("retractArm"));
        assertEquals("RETRACT_ARM", ReplanningService.wireCommandName("retract_arm"));
        assertEquals("MOVE_TO_SENSOR", ReplanningService.wireCommandName("moveToSensor"));
        assertEquals("PICK", ReplanningService.wireCommandName("PICK"));
    }

    private MissionChangeDTO swap(String mission, String state) {
        return service.swapMission(new MissionSwapRequestDTO(VGR2, mission, state, null, null));
    }

    private record FixedConfig(List<AbstractMissionStrategy> missions, List<MachineMissionBinding> bindings)
        implements MissionExtensionConfig {
        @Override public List<AbstractMissionStrategy> getMachineMissions() { return missions; }
        @Override public List<FactoryMissionExtension> getFactoryMissions() { return List.of(); }
        @Override public List<MachineMissionBinding> getMachineMissionBindings() { return bindings; }
        @Override public List<String> missionMachinesLogicalNames() { return List.of(); }
        @Override public void bindMachines(Map<String, AbstractMachine> machines) { }
        @Override public MissionExtensionConfig withMachineMapping(Map<String, AbstractMachine> machines) { return this; }
    }
}
