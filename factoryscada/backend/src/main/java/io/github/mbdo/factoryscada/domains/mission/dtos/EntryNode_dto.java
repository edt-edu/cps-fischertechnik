package io.github.mbdo.factoryscada.domains.mission.dtos;

import io.github.mbdo.factoryscada.service.Visitor.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntryNode_dto extends Node_dto{
    public void accept(Visitor v){
        v.visit(this);
    }
}
