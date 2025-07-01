package io.github.mbdo.factoryscada.domains.mission;

import io.github.mbdo.factoryscada.service.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntryNode extends Node{

    public void accept(Visitor v){
        v.visitEntryNode(this);
    }
}
