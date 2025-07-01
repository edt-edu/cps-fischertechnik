package io.github.mbdo.factoryscada.domains.mission;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.service.Visitor;
import lombok.Data;

@Data
public class EntryNode extends Node{

    public void accept(Visitor v){
        v.visitEntryNode(this);
    }
}
