package io.github.mbdo.factoryscada.domains.mission;

import io.github.mbdo.factoryscada.service.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Fork extends ControlNode{

    public void accept(Visitor v){
        v.visitFork(this);
    }
}