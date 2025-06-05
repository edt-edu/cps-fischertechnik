package io.github.mbdo.factoryscada.core;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mbdo.factoryscada.core.dtos.FeedbackMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandFeedbackDTO implements Serializable {
    private static final long serialVersionUID = 1L;

	@JsonProperty("topicName")
    protected String topicName;

    @JsonProperty("timestamp")
    protected String timestamp;

    @JsonProperty("message")
    protected FeedbackMessage message;
}
