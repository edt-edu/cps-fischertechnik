package io.github.mbdo.factoryscada.domains.mission.dsl.dtos;

import io.github.mbdo.factoryscada.mission.dsl.visitor.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntryNode_dto extends Node_dto{
    public EntryNode_dto(){
        this.type = "EntryNode";
    }

    public void accept(Visitor v){
        v.visit(this);
    }
}
