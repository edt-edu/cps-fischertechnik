package io.github.mbdo.factoryscada.domains.mission.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.service.Visitor;
import lombok.Data;

@Data
public class WaitAction_dto extends ActionNode_dto{
    @JsonProperty("time")
    private int time;
}