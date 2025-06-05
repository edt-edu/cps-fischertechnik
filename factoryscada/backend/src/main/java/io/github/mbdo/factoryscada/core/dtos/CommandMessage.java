package io.github.mbdo.factoryscada.core.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CommandMessage {
    @JsonProperty("jsonType")
    private String jsonType;

    @JsonProperty("type")
    private String type;

    @JsonProperty("outputId")
    private String outputId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("parameters")
    private List<Parameter> parameters;
}
