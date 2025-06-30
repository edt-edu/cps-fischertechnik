package io.github.mbdo.factoryscada.core.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class WaitAction extends ActionNode{
    @JsonProperty("time")
    private Int time;
}