package io.github.mbdo.factoryscada.core.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public abstract class Node{
    @JsonProperty("id")
    private String id;

    @JsonProperty("outputs")
    private List<Node>    
}