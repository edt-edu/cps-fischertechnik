package io.github.mbdo.factoryscada.domains.mission;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.service.Visitor;
import lombok.Data;

@Data
public class Join extends ControlNode{

    public void accept(Visitor v){
        v.visitJoin(this);
    }
}