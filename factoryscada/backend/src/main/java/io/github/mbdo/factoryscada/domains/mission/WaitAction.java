package io.github.mbdo.factoryscada.domains.mission;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class WaitAction extends ActionNode{
    @JsonProperty("time")
    private int time;
}