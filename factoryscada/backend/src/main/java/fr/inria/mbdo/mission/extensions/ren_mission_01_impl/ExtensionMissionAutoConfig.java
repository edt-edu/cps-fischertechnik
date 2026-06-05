package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.ConveyorBeltNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.ConveyorBeltNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbelt.ConveyorBeltMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.MultiProcessingStationNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.MultiProcessingStationNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstation.MultiProcessingStationMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.SortingLineNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.SortingLineNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortingline.SortingLineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.VacuumGripper1NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.VacuumGripper1NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.VacuumGripper2NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgripper.VacuumGripperMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.ZoneMissionCBNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonessystem.Zone;
import fr.inria.mbdo.mission.runtime.config.GlobalMission;
import fr.inria.mbdo.mission.runtime.config.MachineMissionBinding;
import fr.inria.mbdo.mission.runtime.config.MissionBindingCatalog;
import fr.inria.mbdo.mission.runtime.config.MissionTemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring configuration that assembles the {@link MissionBindingCatalog} for the
 * ren_mission_01 physical platform.
 *
 * <h2>Architecture</h2>
 * <p>
 * This configuration creates a shared set of machine adapters (stubs extending
 * {@link fr.inria.mbdo.mission.runtime.api.AbstractAdapter}) and wires them
 * into
 * mission state machines. Each adapter is exposed as a named Spring bean so
 * that
 * bridge code (connecting to the legacy {@code FactoryScada} TCP/socket layer
 * or
 * to MQTT) can inject them and forward hardware events.
 * </p>
 *
 * <h2>Machines declared</h2>
 * <ul>
 * <li><b>ConveyorBelt</b> — belt transport with feed/swap sensors</li>
 * <li><b>SortingLine</b> — color token detection and sorting</li>
 * <li><b>MultiProcessingStation</b> — multi-step processing (oven, saw,
 * arm)</li>
 * <li><b>VacuumGripper1</b> — picks from SortingLine, places on
 * ConveyorBelt</li>
 * <li><b>VacuumGripper2</b> — transfers from ConveyorBelt to MPS</li>
 * <li><b>ZoneCB</b> — mutual-exclusion zone for conveyor belt area</li>
 * <li><b>ZoneMPS</b> — mutual-exclusion zone for MPS input area</li>
 * </ul>
 */
@Configuration
public class ExtensionMissionAutoConfig {

    // ──────────────────────────────────────────────────────────────────────────
    // Machine Adapter Beans
    // ──────────────────────────────────────────────────────────────────────────

    @Bean("conveyorBeltAdapter")
    public ConveyorBeltMachine conveyorBeltAdapter() {
        return new ConveyorBeltAdapterImpl("ConveyorBelt");
    }

    @Bean("sortingLineAdapter")
    public SortingLineMachine sortingLineAdapter() {
        return new SortingLineAdapterImpl("SortingLine");
    }

    @Bean("multiProcessingStationAdapter")
    public MultiProcessingStationMachine multiProcessingStationAdapter() {
        return new MultiProcessingStationAdapterImpl("MultiProcessingStation");
    }

    @Bean("vacuumGripper1Adapter")
    public VacuumGripperMachine vacuumGripper1Adapter() {
        return new VacuumGripperAdapterImpl("VacuumGripper1");
    }

    @Bean("vacuumGripper2Adapter")
    public VacuumGripperMachine vacuumGripper2Adapter() {
        return new VacuumGripperAdapterImpl("VacuumGripper2");
    }

    @Bean("zoneCBAdapter")
    public Zone zoneCBAdapter() {
        return new ZoneAdapterImpl("ZoneCB");
    }

    @Bean("zoneMPSAdapter")
    public Zone zoneMPSAdapter() {
        return new ZoneAdapterImpl("ZoneMPS");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mission Actions Beans (no-op implementations for now)
    // ──────────────────────────────────────────────────────────────────────────

    @Bean
    public ConveyorBeltNominalMissionActions conveyorBeltNominalMissionActions() {
        return new ConveyorBeltNominalMissionActionsImpl();
    }

    @Bean
    public SortingLineNominalMissionActions sortingLineNominalMissionActions() {
        return new SortingLineNominalMissionActionsImpl();
    }

    @Bean
    public MultiProcessingStationNominalMissionActions multiProcessingStationNominalMissionActions() {
        return new MultiProcessingStationNominalMissionActionsImpl();
    }

    @Bean
    public VacuumGripper1NominalMissionActions vacuumGripper1NominalMissionActions() {
        return new VacuumGripper1NominalMissionActionsImpl();
    }

    @Bean
    public VacuumGripper2NominalMissionActions vacuumGripper2NominalMissionActions() {
        return new VacuumGripper2NominalMissionActionsImpl();
    }

    @Bean
    public ZoneMissionCBNominalActions zoneMissionCBNominalActions() {
        return new ZoneMissionCBNominalActionsImpl();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mission Binding Catalog
    // ──────────────────────────────────────────────────────────────────────────

    @Bean
    public MissionBindingCatalog missionBindingCatalog(
            ConveyorBeltMachine conveyorBeltAdapter,
            SortingLineMachine sortingLineAdapter,
            MultiProcessingStationMachine multiProcessingStationAdapter,
            @org.springframework.beans.factory.annotation.Qualifier("vacuumGripper1Adapter") VacuumGripperMachine vacuumGripper1Adapter,
            @org.springframework.beans.factory.annotation.Qualifier("vacuumGripper2Adapter") VacuumGripperMachine vacuumGripper2Adapter,
            @org.springframework.beans.factory.annotation.Qualifier("zoneCBAdapter") Zone zoneCBAdapter,
            @org.springframework.beans.factory.annotation.Qualifier("zoneMPSAdapter") Zone zoneMPSAdapter,
            ConveyorBeltNominalMissionActions conveyorBeltActions,
            SortingLineNominalMissionActions sortingLineActions,
            MultiProcessingStationNominalMissionActions multiProcessingStationActions,
            VacuumGripper1NominalMissionActions vacuumGripper1Actions,
            VacuumGripper2NominalMissionActions vacuumGripper2Actions,
            ZoneMissionCBNominalActions zoneCBActions) {

        // --- Mission Templates ---

        MissionTemplate conveyorBeltNominal = new MissionTemplate(
                "ConveyorBeltNominalMission",
                "Nominal mission for the Conveyor Belt: moves items to sensor and notifies grippers.",
                List.of("ConveyorBelt", "VacuumGripper1", "VacuumGripper2"),
                new ConveyorBeltNominalMission(conveyorBeltAdapter, vacuumGripper1Adapter,
                        vacuumGripper2Adapter, conveyorBeltActions),
                () -> new ConveyorBeltNominalMission(conveyorBeltAdapter, vacuumGripper1Adapter,
                        vacuumGripper2Adapter, conveyorBeltActions));

        MissionTemplate sortingLineNominal = new MissionTemplate(
                "SortingLineNominalMission",
                "Nominal mission for the Sorting Line: sorts tokens by color and notifies VacuumGripper1.",
                List.of("SortingLine", "VacuumGripper1"),
                new SortingLineNominalMission(sortingLineAdapter, vacuumGripper1Adapter,
                        sortingLineActions),
                () -> new SortingLineNominalMission(sortingLineAdapter, vacuumGripper1Adapter,
                        sortingLineActions));

        MissionTemplate multiProcessingStationNominal = new MissionTemplate(
                "MultiProcessingStationNominalMission",
                "Nominal mission for the Multi-Processing Station: acquires zone, processes workpiece, broadcasts completion.",
                List.of("MultiProcessingStation", "ZoneMPS"),
                new MultiProcessingStationNominalMission(multiProcessingStationAdapter, zoneMPSAdapter,
                        multiProcessingStationActions),
                () -> new MultiProcessingStationNominalMission(multiProcessingStationAdapter,
                        zoneMPSAdapter, multiProcessingStationActions));

        MissionTemplate vacuumGripper1Nominal = new MissionTemplate(
                "VacuumGripper1NominalMission",
                "Nominal mission for Vacuum Gripper 1: picks color tokens from Sorting Line and places on Conveyor Belt feed.",
                List.of("VacuumGripper1", "SortingLine", "ZoneCB"),
                new VacuumGripper1NominalMission(vacuumGripper1Adapter, sortingLineAdapter,
                        zoneCBAdapter, vacuumGripper1Actions),
                () -> new VacuumGripper1NominalMission(vacuumGripper1Adapter, sortingLineAdapter,
                        zoneCBAdapter, vacuumGripper1Actions));

        MissionTemplate vacuumGripper2Nominal = new MissionTemplate(
                "VacuumGripper2NominalMission",
                "Nominal mission for Vacuum Gripper 2: transfers workpieces from Conveyor Belt to Multi-Processing Station.",
                List.of("VacuumGripper2", "ZoneCB", "ZoneMPS"),
                new VacuumGripper2NominalMission(vacuumGripper2Adapter, zoneCBAdapter, zoneMPSAdapter,
                        vacuumGripper2Actions),
                () -> new VacuumGripper2NominalMission(vacuumGripper2Adapter, zoneCBAdapter,
                        zoneMPSAdapter, vacuumGripper2Actions));

        MissionTemplate zoneCBNominal = new MissionTemplate(
                "ZoneMissionCBNominal",
                "Nominal mission for the Conveyor Belt Zone: manages mutual exclusion between VacuumGripper1 and VacuumGripper2.",
                List.of("ZoneCB", "VacuumGripper1", "VacuumGripper2"),
                new ZoneMissionCBNominal(vacuumGripper1Adapter, vacuumGripper2Adapter, zoneCBActions),
                () -> new ZoneMissionCBNominal(vacuumGripper1Adapter, vacuumGripper2Adapter,
                        zoneCBActions));

        List<MissionTemplate> templates = List.of(
                conveyorBeltNominal,
                sortingLineNominal,
                multiProcessingStationNominal,
                vacuumGripper1Nominal,
                vacuumGripper2Nominal,
                zoneCBNominal);

        // --- Machine Bindings ---

        List<MachineMissionBinding> bindings = List.of(
                new MachineMissionBinding("ConveyorBelt", "ConveyorBeltMachine",
                        List.of("ConveyorBeltNominalMission")),
                new MachineMissionBinding("SortingLine", "SortingLineMachine",
                        List.of("SortingLineNominalMission")),
                new MachineMissionBinding("MultiProcessingStation", "MultiProcessingStationMachine",
                        List.of("MultiProcessingStationNominalMission")),
                new MachineMissionBinding("VacuumGripper1", "VacuumGripperMachine",
                        List.of("VacuumGripper1NominalMission")),
                new MachineMissionBinding("VacuumGripper2", "VacuumGripperMachine",
                        List.of("VacuumGripper2NominalMission")),
                new MachineMissionBinding("ZoneCB", "Zone",
                        List.of("ZoneMissionCBNominal")));

        // --- Global Missions ---

        List<GlobalMission> globalMissions = List.of(
                new GlobalMission(
                        "NominalProduction",
                        "Full nominal production: all machines run their nominal missions in coordination.",
                        List.of(
                                "ConveyorBeltNominalMission",
                                "SortingLineNominalMission",
                                "MultiProcessingStationNominalMission",
                                "VacuumGripper1NominalMission",
                                "VacuumGripper2NominalMission",
                                "ZoneMissionCBNominal")));

        return new MissionBindingCatalog(templates, bindings, globalMissions);
    }
}
