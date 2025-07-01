package io.github.mbdo.factoryscada.domains.mission.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.service.Visitor;

import java.util.List;

import lombok.Data;

@Data
public abstract class Node_dto{
    @JsonProperty("id")
    private String id;

    @JsonProperty("description")
    private String description;

    @JsonProperty("outputs")
    private List<String> outputs;
}