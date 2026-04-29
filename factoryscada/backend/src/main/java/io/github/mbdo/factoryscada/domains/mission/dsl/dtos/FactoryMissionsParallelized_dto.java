package io.github.mbdo.factoryscada.domains.mission.dsl.dtos;

import java.util.List;
import lombok.Data;

/**
 * Class holding the nodes for parallelized missions.
 * Content is extracted from the missions yaml configuration file
 */

@Data
public class FactoryMissionsParallelized_dto{
    String name;
    List<MissionParallelized_dto> missions;
}