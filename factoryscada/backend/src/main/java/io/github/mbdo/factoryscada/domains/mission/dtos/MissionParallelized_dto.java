package io.github.mbdo.factoryscada.domains.mission.dtos;

import java.util.List;
import lombok.Data;

@Data
public class MissionParallelized_dto{
    String name;
    String description;
    List<Node_dto> nodes;
}