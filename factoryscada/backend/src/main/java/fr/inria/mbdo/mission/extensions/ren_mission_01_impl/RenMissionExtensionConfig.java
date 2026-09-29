package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.SortingLineNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.SortingLineNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissionmpsnominal.ZoneMissionMPSNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.*;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.*;
import fr.inria.mbdo.mission.runtime.api.AbstractMachineAdapter;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.config.FactoryMissionExtension;
import fr.inria.mbdo.mission.runtime.config.MissionExtensionConfig;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.HighBayWarehouseMachine;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import io.github.mbdo.factoryscada.service.MachineNameMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class RenMissionExtensionConfig implements MissionExtensionConfig {

    private final MqttMessageRouter mqttRouter;
    private final MachineNameMappingService machineNameMapping;

    private final ConveyorBeltNominalMissionActions cbActions;
    private final SortingLineNominalMissionActions slActions;
    private final MultiProcessingStationNominalMissionActions mpsActions;
    private final VacuumGripper1NominalMissionActions vgr1Actions;
    private final VacuumGripper2NominalMissionActions vgr2Actions;
    private final ZoneMissionCBNominalActions zoneCBActions;
    private final ZoneMissionMPSNominalActions zoneMPSActions;

    private List<AbstractMissionStrategy> machineMissions;
    private List<FactoryMissionExtension> factoryMissions;
    private Map<String, AbstractMachineAdapter> machineAdapters;

    @Autowired
    public RenMissionExtensionConfig(MqttMessageRouter mqttRouter, MachineNameMappingService machineNameMapping) {
        this.mqttRouter = mqttRouter;
        this.machineNameMapping = machineNameMapping;

        this.cbActions = new ConveyorBeltNominalMissionActionsImpl();
        this.slActions = new SortingLineNominalMissionActionsImpl();
        this.mpsActions = new MultiProcessingStationNominalMissionActionsImpl();
        this.vgr1Actions = new VacuumGripper1NominalMissionActionsImpl();
        this.vgr2Actions = new VacuumGripper2NominalMissionActionsImpl();
        this.zoneCBActions = new ZoneMissionCBNominalActionsImpl();
        this.zoneMPSActions = new ZoneMissionMPSNominalActions() { };

        this.machineAdapters = Map.of();
        this.machineMissions = List.of();
        this.factoryMissions = List.of();
    }

    @Override
    public List<AbstractMissionStrategy> getMachineMissions() {
        return machineMissions;
    }

    @Override
    public List<FactoryMissionExtension> getFactoryMissions() {
        return factoryMissions;
    }

    private <T extends AbstractMachineAdapter> T adapter(String id) {
        @SuppressWarnings("unchecked")
        T a = (T) machineAdapters.get(id);
        return a;
    }

    private List<AbstractMissionStrategy> buildMachineMissions() {


        ConveyorBeltAdapterImpl cb = adapter("REN_MISSION_01/CB01");
        SortingLineAdapterImpl sl = adapter("REN_MISSION_01/SL01");
        MultiProcessingStationAdapterImpl mps = adapter("REN_MISSION_01/MPS01");
        VacuumGripperAdapterImpl vgr1 = adapter("REN_MISSION_01/VGR01");
        VacuumGripperAdapterImpl vgr2 = adapter("REN_MISSION_01/VGR02");
        ZoneAdapterImpl zoneCB = adapter("ZoneCB");
        ZoneAdapterImpl zoneMPS = adapter("ZoneMPS");

        return List.of(
            new ConveyorBeltNominalMission(cb, vgr1, vgr2, cbActions),
            new SortingLineNominalMission(sl, vgr1, slActions),
            new MultiProcessingStationNominalMission(mps, zoneMPS, mpsActions),
            new VacuumGripper1NominalMission(vgr1, sl, zoneCB, vgr1Actions),
            new VacuumGripper2NominalMission(vgr2, zoneCB, zoneMPS, vgr2Actions),
            new ZoneMissionCBNominal(zoneCB, zoneCBActions),
            new ZoneMissionMPSNominal(zoneMPS, zoneMPSActions));
    }

    private List<FactoryMissionExtension> buildFactoryMissions() {
        return List.of(new FactoryMissionExtension(
            "NominalProduction",
            "Full nominal production: all machines run their nominal missions in coordination.",
            machineMissions));
    }

    @Override
    public void bindMachines(Map<String, AbstractMachine> machines) {
        // adapters are keyed by logical name, machines by physical name: translate before lookup
        for (String machineLogicalName : missionMachinesLogicalNames()) {
            AbstractMachineAdapter adapter = machineAdapters.get(machineLogicalName);
            String machineId = machineNameMapping.getMachineForLogicalName(machineLogicalName);
            AbstractMachine machine = machines.get(machineId);
            if (machine == null || adapter == null) {
                log.warn("Adapter {} not bound to a real machine ({} not found)", machineLogicalName, machineId);
                continue;
            }
            switch (adapter) {
                case ConveyorBeltAdapterImpl cb when machine instanceof ConveyorBeltMachine cbm -> {
                    cb.bindRealMachine(cbm);
                    cbm.addCommandFeedbackListener(cb::commandFeedback);
                }
                case SortingLineAdapterImpl sl when machine instanceof SortingLineMachine slm -> {
                    sl.bindRealMachine(slm);
                    slm.addCommandFeedbackListener(sl::commandFeedback);
                }
                case
                    MultiProcessingStationAdapterImpl mps when machine instanceof MultiProcessingStationMachine mpsm -> {
                    mps.bindRealMachine(mpsm);
                    mpsm.addCommandFeedbackListener(mps::commandFeedback);
                }
                case VacuumGripperAdapterImpl vgr when machine instanceof VacuumGripperMachine vgrm -> {
                    vgr.bindRealMachine(vgrm);
                    vgrm.addCommandFeedbackListener(vgr::commandFeedback);
                }
                default -> {
                }
            }
        }
    }

    @Override
    public MissionExtensionConfig withMachineMapping(Map<String, AbstractMachine> machines) {
        Map<String, AbstractMachineAdapter> adapters = new HashMap<>();

        final List<String> missionsMachinesLogicalNames = missionMachinesLogicalNames();
        // for machines in list of machine logical names used by the mission
        for(String machineLogicalName : missionsMachinesLogicalNames) {
            // find the corresponding machine in the scada configuration
            String machineId = machineNameMapping.getMachineForLogicalName(machineLogicalName);
            AbstractMachine machine = machines.get(machineId);
            if (machine == null) {
                throw new IllegalStateException(machineLogicalName+"->"+machineId+" machine missing in configuration");
            }
            AbstractMachineAdapter adapter = switch (machine) {
                case ConveyorBeltMachine cb -> new ConveyorBeltAdapterImpl(machineLogicalName, mqttRouter,
                    "PLC/+/ConveyorBelt/" + machineId + "/measurements/input/#");
                case SortingLineMachine sl -> new SortingLineAdapterImpl(machineLogicalName, mqttRouter,
                    "PLC/+/SortingLine/" + machineId + "/measurements/input/#");
                case MultiProcessingStationMachine mps -> new MultiProcessingStationAdapterImpl(machineLogicalName, mqttRouter,
                    "PLC/+/MultiProcessing/" + machineId + "/measurements/input/#");
                case VacuumGripperMachine vg -> new VacuumGripperAdapterImpl(machineLogicalName, mqttRouter,
                    "PLC/+/VacuumGripper/" + machineId + "/measurements/input/#");
                case HighBayWarehouseMachine hbw -> null;
                default -> null;
            };
            adapters.put(machineLogicalName, adapter);
        }
/*
        for (Map.Entry<String, AbstractMachine> entry : machines.entrySet()) {
            String machineId = entry.getKey();
            AbstractMachine machine = entry.getValue();
            AbstractMachineAdapter adapter = switch (machine) {
                case ConveyorBeltMachine cb -> new ConveyorBeltAdapterImpl(machineId, mqttRouter,
                    "PLC/+/ConveyorBelt/" + machineId + "/measurements/input/#");
                case SortingLineMachine sl -> new SortingLineAdapterImpl(machineId, mqttRouter,
                    "PLC/+/SortingLine/" + machineId + "/measurements/input/#");
                case MultiProcessingStationMachine mps -> new MultiProcessingStationAdapterImpl(machineId, mqttRouter,
                    "PLC/+/MultiProcessing/" + machineId + "/measurements/input/#");
                case VacuumGripperMachine vg -> new VacuumGripperAdapterImpl(machineId, mqttRouter,
                    "PLC/+/VacuumGripper/" + machineId + "/measurements/input/#");
                case HighBayWarehouseMachine hbw -> null;
                default -> null;
            };
            adapters.put(entry.getKey(), adapter);
        }*/

        // Fixed adapters
        adapters.put("ZoneCB", new ZoneAdapterImpl("ZoneCB"));
        adapters.put("ZoneMPS", new ZoneAdapterImpl("ZoneMPS"));

        this.machineAdapters = adapters;
        this.machineMissions = buildMachineMissions();
        this.factoryMissions = buildFactoryMissions();

        return this;
    }

    /**
     * list the logical Names of the Machines handled by this mission configuration
     * physical machine name can be then retrieved later via the {@link io.github.mbdo.factoryscada.service.MachineNameMappingService}
     * @return a list of machine logical names
     */
    public List<String> missionMachinesLogicalNames() {
        return List.of("REN_MISSION_01/CB01",
            "REN_MISSION_01/SL01",
            "REN_MISSION_01/MPS01",
            "REN_MISSION_01/VGR01",
            "REN_MISSION_01/VGR02");
    }
}
