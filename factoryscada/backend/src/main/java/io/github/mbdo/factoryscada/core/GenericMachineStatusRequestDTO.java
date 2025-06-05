package io.github.mbdo.factoryscada.core;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.core.dtos.RequestMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericMachineStatusRequestDTO<T extends AbstractMachine> implements Serializable {
    private static final long serialVersionUID = 1L;

	@JsonProperty("topicName")
    protected String topicName;

    @JsonProperty("timestamp")
    protected String timestamp;

    @JsonProperty("message")
    protected RequestMessage message;
}
