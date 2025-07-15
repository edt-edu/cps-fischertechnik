package io.github.mbdo.factoryscada.domains.mission.dtos;

import io.github.mbdo.factoryscada.service.Visitor.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Join_dto extends ControlNode_dto{
    public Join_dto(){
        this.type = "Join";
    }

    public void accept(Visitor v){
        v.visit(this);
    }

    // This attributes will be initialized during the visitor's pass.
    // It contains the number of nodes pointing to this join.
    private Integer numberInputs;
}