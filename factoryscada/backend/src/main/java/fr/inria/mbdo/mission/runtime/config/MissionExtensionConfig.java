package fr.inria.mbdo.mission.runtime.config;

import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import io.github.mbdo.factoryscada.core.AbstractMachine;

import java.util.List;
import java.util.Map;

public interface MissionExtensionConfig {
    List<AbstractMissionStrategy> getMachineMissions();

    List<FactoryMissionExtension> getFactoryMissions();

    /**
     * The missions each machine can run: its nominal mission and its alternatives (e.g. when another machine fails).
     * A machine runs at most one of them at a time.
     *
     * @return one binding per machine, keyed by the machine logical name
     */
    default List<MachineMissionBinding> getMachineMissionBindings() {
        return List.of();
    }

    /**
     * list the logical Names of the Machines handled by this mission configuration
     * physical machine name can be then retrieved later via the {@link io.github.mbdo.factoryscada.service.MachineNameMappingService}
     * @return a list of machine logical names
     */
    List<String> missionMachinesLogicalNames();

    void bindMachines(Map<String, AbstractMachine> machines);

    MissionExtensionConfig withMachineMapping(Map<String, AbstractMachine> machines);
}
