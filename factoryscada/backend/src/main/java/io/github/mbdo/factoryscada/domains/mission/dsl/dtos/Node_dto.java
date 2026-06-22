package io.github.mbdo.factoryscada.domains.mission.dsl.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.github.mbdo.factoryscada.mission.dsl.visitor.Visitor;

import java.util.List;

import lombok.Data;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EntryNode_dto.class, name = "entryNode"),
    @JsonSubTypes.Type(value = Fork_dto.class, name = "fork"),
    @JsonSubTypes.Type(value = RawMachineCommand_dto.class, name = "rawMachineCommand"),
    @JsonSubTypes.Type(value = WaitAction_dto.class, name = "waitAction"),
    @JsonSubTypes.Type(value = Join_dto.class, name = "join"),
})
@Data
public abstract class Node_dto{
    @JsonProperty("id")
    private String id;

    protected String type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("outputs")
    private List<String> outputs;

    // This attributes will be initialized during the visitor's pass.
    // It contains the references to the nodes that are given (by id) in the yaml conf file.
    List<Node_dto> outputNodes;

    public abstract void accept(Visitor v);
}