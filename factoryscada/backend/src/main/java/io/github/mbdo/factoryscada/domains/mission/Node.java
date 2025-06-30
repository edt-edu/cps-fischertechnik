package io.github.mbdo.factoryscada.domains.mission;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Data;

@Data
public abstract class Node{
    @JsonProperty("id")
    private String id;

    @JsonProperty("description")
    private String description;

    @JsonProperty("outputs")
    private List<Node> outputs;
}