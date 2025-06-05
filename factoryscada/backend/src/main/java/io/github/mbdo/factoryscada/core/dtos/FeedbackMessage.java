package io.github.mbdo.factoryscada.core.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FeedbackMessage {
    @JsonProperty("jsonType")
    private String jsonType;

    @JsonProperty("commandId")
    private String commandId;


    @JsonProperty("status")
    private String status;
    
    @JsonProperty("info")
    private String info;
    
}
