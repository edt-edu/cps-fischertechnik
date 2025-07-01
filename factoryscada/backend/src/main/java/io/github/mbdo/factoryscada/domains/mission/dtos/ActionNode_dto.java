package io.github.mbdo.factoryscada.domains.mission.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public abstract class ActionNode_dto extends Node_dto{
    @JsonProperty("placeholder")
    private String placeholder;
}
