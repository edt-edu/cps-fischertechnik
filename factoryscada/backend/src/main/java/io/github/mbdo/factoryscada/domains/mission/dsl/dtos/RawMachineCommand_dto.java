package io.github.mbdo.factoryscada.domains.mission.dsl.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.service.Visitor.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RawMachineCommand_dto extends ActionNode_dto{
    public RawMachineCommand_dto(){
        this.type = "RawMachineCommand";
    }

    public void accept(Visitor v){
        v.visit(this);
    }

    @JsonProperty("placeholder")
    private String placeholder;
}