package io.github.mbdo.factoryscada.domains.mission.dsl.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.service.Visitor.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WaitAction_dto extends ActionNode_dto{
    public WaitAction_dto(){
        this.type = "WaitAction";
    }

    public void accept(Visitor v){
        v.visit(this);
    }

    //time wait in seconds
    @JsonProperty("time")
    private int time;
}