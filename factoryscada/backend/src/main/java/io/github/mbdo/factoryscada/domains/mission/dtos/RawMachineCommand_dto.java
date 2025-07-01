package io.github.mbdo.factoryscada.domains.mission.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.service.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RawMachineCommand_dto extends ActionNode_dto{
    public void accept(Visitor v){
        v.visitRawMachineCommand(this);
    }

    @JsonProperty("placeholder")
    private String placeholder;
}