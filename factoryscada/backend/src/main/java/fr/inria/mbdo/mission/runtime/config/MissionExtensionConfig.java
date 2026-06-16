package fr.inria.mbdo.mission.runtime.config;

import fr.inria.mbdo.mission.runtime.api.AbstractMissionStrategy;
import io.github.mbdo.factoryscada.core.AbstractMachine;

import java.util.List;
import java.util.Map;

public interface MissionExtensionConfig {
    List<AbstractMissionStrategy> getMachineMissions();

    List<FactoryMissionExtension> getFactoryMissions();

    default void bindMachines(Map<String, AbstractMachine> machines) {}
}
