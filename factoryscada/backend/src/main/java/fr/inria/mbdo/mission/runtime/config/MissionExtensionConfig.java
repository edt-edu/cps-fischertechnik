package fr.inria.mbdo.mission.runtime.config;

import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import io.github.mbdo.factoryscada.core.AbstractMachine;

import java.util.List;
import java.util.Map;

public interface MissionExtensionConfig {
    List<AbstractMissionStrategy> getMachineMissions();

    List<FactoryMissionExtension> getFactoryMissions();

    /**
     * list the logical Names of the Machines handled by this mission configuration
     * physical machine name can be then retrieved later via the {@link io.github.mbdo.factoryscada.service.MachineNameMappingService}
     * @return a list of machine logical names
     */
    List<String> missionMachinesLogicalNames();

    void bindMachines(Map<String, AbstractMachine> machines);

    MissionExtensionConfig withMachineMapping(Map<String, AbstractMachine> machines);
}
