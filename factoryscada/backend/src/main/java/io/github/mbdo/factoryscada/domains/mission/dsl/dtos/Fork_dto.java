package io.github.mbdo.factoryscada.domains.mission.dsl.dtos;

import io.github.mbdo.factoryscada.mission.dsl.visitor.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Fork_dto extends ControlNode_dto{
    public Fork_dto(){
        this.type = "Fork";
    }

    public void accept(Visitor v){
        v.visit(this);
    }
}