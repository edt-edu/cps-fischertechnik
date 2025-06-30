package io.github.mbdo.factoryscada.domains.mission;

import java.util.List;
import lombok.Data;

/**
 * Class holding the nodes for parallelized missions.
 * Content is extracted from the missions yaml configuration file
 */

@Data
public class FactoryMissionsParallelized{
    String name;
    List<MissionParallelized> missions;
}