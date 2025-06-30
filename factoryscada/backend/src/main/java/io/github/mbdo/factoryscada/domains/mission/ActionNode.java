package io.github.mbdo.factoryscada.domains.mission;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public abstract class ActionNode extends Node{
    @JsonProperty("placeholder")
    private String placeholder;
}
