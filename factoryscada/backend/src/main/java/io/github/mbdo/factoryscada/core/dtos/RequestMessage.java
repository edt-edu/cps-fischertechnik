package io.github.mbdo.factoryscada.core.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RequestMessage {
    @JsonProperty("jsonType")
    private String jsonType;

    @JsonProperty("type")
    private String type;

    @JsonProperty("requestId")
    private String requestId;


    @JsonProperty("params")
    private List<RequestedParameter> parameters;
}
