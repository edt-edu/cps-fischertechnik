package fr.inria.mbdo.mission.extensions.ren_mission_01_impl;

import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltmissions.conveyorbeltnominalmission.ConveyorBeltNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.conveyorbeltsystem.conveyorbeltmessages.CBCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationmissions.multiprocessingstationnominalmission.MultiProcessingStationNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationmessages.MPSCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.SortingLineNominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinemissions.sortinglinenominalmission.SortingLineNominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.sortinglinesystem.sortinglinemessages.SLCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper1nominalmission.VacuumGripper1NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMission;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippermissions.vacuumgripper2nominalmission.VacuumGripper2NominalMissionActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippermessages.VGRCommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominal;
import fr.inria.mbdo.mission.extensions.ren_mission_01.zonemissions.zonemissioncbnominal.ZoneMissionCBNominalActions;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.*;
import fr.inria.mbdo.mission.extensions.ren_mission_01_impl.adapters.actions.*;
import fr.inria.mbdo.mission.runtime.api.AbstractMachineAdapter;
import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import fr.inria.mbdo.mission.runtime.config.FactoryMissionExtension;
import fr.inria.mbdo.mission.runtime.config.MissionExtensionConfig;
import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.MqttMessageRouter;
import io.github.mbdo.factoryscada.core.enums.Color;
import io.github.mbdo.factoryscada.core.enums.DirectionKind;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class RenMissionExtensionConfig implements MissionExtensionConfig {

    private final ConveyorBeltNominalMissionActions cbActions;
    private final SortingLineNominalMissionActions slActions;
    private final MultiProcessingStationNominalMissionActions mpsActions;
    private final VacuumGripper1NominalMissionActions vgr1Actions;
    private final VacuumGripper2NominalMissionActions vgr2Actions;
    private final ZoneMissionCBNominalActions zoneCBActions;

    // Keyed by the AbstractMachine name they correspond to in FactoryScada
    private final Map<String, AbstractMachineAdapter> machineAdapters;

    private final List<AbstractMissionStrategy> machineMissions;
    private final List<FactoryMissionExtension> factoryMissions;

    @Autowired
    public RenMissionExtensionConfig(MqttMessageRouter mqttRouter) {
        this.cbActions = new ConveyorBeltNominalMissionActionsImpl();
        this.slActions = new SortingLineNominalMissionActionsImpl();
        this.mpsActions = new MultiProcessingStationNominalMissionActionsImpl();
        this.vgr1Actions = new VacuumGripper1NominalMissionActionsImpl();
        this.vgr2Actions = new VacuumGripper2NominalMissionActionsImpl();
        this.zoneCBActions = new ZoneMissionCBNominalActionsImpl();

        this.machineAdapters = Map.of(
            "ConveyorBelt01", new ConveyorBeltAdapterImpl("ConveyorBelt01", mqttRouter,
                "PLC/+/ConveyorBelt/ConveyorBelt01/measurements/input/#"),
            "SortingLine01", new SortingLineAdapterImpl("SortingLine01", mqttRouter,
                "PLC/+/SortingLine/SortingLine01/measurements/input/#"),
            "MultiProcessing01", new MultiProcessingStationAdapterImpl("MultiProcessing01", mqttRouter,
                "PLC/+/MultiProcessing/MultiProcessing01/measurements/input/#"),
            "VacuumGripper01", new VacuumGripperAdapterImpl("VacuumGripper01", mqttRouter,
                "PLC/+/VacuumGripper/VacuumGripper01/measurements/input/#"),
            "VacuumGripper02", new VacuumGripperAdapterImpl("VacuumGripper02", mqttRouter,
                "PLC/+/VacuumGripper/VacuumGripper02/measurements/input/#"),
            "ZoneCB", new ZoneAdapterImpl("ZoneCB"),
            "ZoneMPS", new ZoneAdapterImpl("ZoneMPS"));

        this.machineMissions = buildMachineMissions();
        this.factoryMissions = buildFactoryMissions();
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
        ConveyorBeltAdapterImpl cb = adapter("ConveyorBelt01");
        SortingLineAdapterImpl sl = adapter("SortingLine01");
        MultiProcessingStationAdapterImpl mps = adapter("MultiProcessing01");
        VacuumGripperAdapterImpl vgr1 = adapter("VacuumGripper01");
        VacuumGripperAdapterImpl vgr2 = adapter("VacuumGripper02");
        ZoneAdapterImpl zoneCB = adapter("ZoneCB");
        ZoneAdapterImpl zoneMPS = adapter("ZoneMPS");

        return List.of(
            new ConveyorBeltNominalMission(cb, vgr1, vgr2, cbActions),
            new SortingLineNominalMission(sl, vgr1, slActions),
            new MultiProcessingStationNominalMission(mps, zoneMPS, mpsActions),
            new VacuumGripper1NominalMission(vgr1, sl, zoneCB, vgr1Actions),
            new VacuumGripper2NominalMission(vgr2, zoneCB, zoneMPS, vgr2Actions),
            new ZoneMissionCBNominal(vgr1, vgr2, zoneCBActions));
    }

    private List<FactoryMissionExtension> buildFactoryMissions() {
        return List.of(new FactoryMissionExtension(
            "NominalProduction",
            "Full nominal production: all machines run their nominal missions in coordination.",
            machineMissions));
    }

    @Override
    public void bindMachines(Map<String, AbstractMachine> machines) {
        machineAdapters.forEach((id, adapter) -> {
            AbstractMachine machine = machines.get(id);
            if (machine == null) {
                // ZoneAdapterImpl entries have no AbstractMachine counterpart — expected
                return;
            }
            switch (adapter) {
                case ConveyorBeltAdapterImpl cb when machine instanceof ConveyorBeltMachine cbm -> {
                    cb.bindCommands(Map.of(
                        "stop", cbm::stop,
                        "moveToSensor", () -> cbm.moveToSensor(DirectionKind.FORWARD)));
                    cbm.addCommandFeedbackListener((done, status) -> {
                        if (done)
                            cb.publish(new CBCommandSuccessEventMessage());
                    });
                }
                case SortingLineAdapterImpl sl when machine instanceof SortingLineMachine slm -> {
                    sl.bindCommands(Map.of(
                        "stop", slm::stop,
                        "eject", () -> slm.eject(Color.AUTO)));
                    slm.addCommandFeedbackListener((done, status) -> {
                        if (done)
                            sl.publish(new SLCommandSuccessEventMessage());
                    });
                }
                case
                    MultiProcessingStationAdapterImpl mps when machine instanceof MultiProcessingStationMachine mpsm -> {
                    mps.bindCommands(Map.of(
                        "stop", mpsm::stop,
                        "setup", mpsm::setup));
                    mpsm.addCommandFeedbackListener((done, status) -> {
                        if (done)
                            mps.publish(new MPSCommandSuccessEventMessage());
                    });
                }
                case VacuumGripperAdapterImpl vgr when machine instanceof VacuumGripperMachine vgrm -> {
                    vgr.bindCommands(Map.of(
                        "stop", vgrm::stop,
                        "setup", vgrm::setup,
                        "moveToSafePosition", vgrm::go_to_safe_position,
                        "retractArm", vgrm::retract_arm));
                    vgrm.addCommandFeedbackListener((done, status) -> {
                        if (done)
                            vgr.publish(new VGRCommandSuccessEventMessage());
                    });
                }
                default -> {
                }
            }
        });
    }
}
