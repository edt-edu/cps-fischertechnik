package io.github.mbdo.factoryscada.domains.mission;

import java.util.List;
import lombok.Data;

@Data
public class MissionParallelized{
    String name;
    String description;
    List<Node> nodes;
}