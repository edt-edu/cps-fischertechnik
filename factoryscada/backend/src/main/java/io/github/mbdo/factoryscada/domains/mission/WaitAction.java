package io.github.mbdo.factoryscada.domains.mission;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.service.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WaitAction extends ActionNode{
    @JsonProperty("time")
    private int time;

    public void accept(Visitor v){
        v.visitWaitAction(this);
    }
}