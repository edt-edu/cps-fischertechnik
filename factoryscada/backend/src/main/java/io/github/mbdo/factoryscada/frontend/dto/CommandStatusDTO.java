package io.github.mbdo.factoryscada.frontend.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;

@Data
public class CommandStatusDTO {
	@JsonPropertyDescription("Name of the machine")
	private String machineName;

	@JsonPropertyDescription("Raw JSON of the last command sent to the machine controller")
	private String currentCommandRawJSON;

	@JsonPropertyDescription("Id of the last command sent (its outputId)")
	private String currentCommandId;
	@JsonPropertyDescription("Name of the last command sent")
	private String currentCommandName;
	@JsonPropertyDescription("Timestamp of the last command sent")
	private String currentCommandTimestamp;


	@JsonPropertyDescription("Raw JSON of the last command feedback received from the machine controller")
	private String commandFeedbackRawJSON;

	@JsonPropertyDescription("Status reported by the last command feedback")
	private String commandFeedbackStatus;
	@JsonPropertyDescription("Timestamp of the last command feedback")
	private String commandFeedbackTimestamp;
	@JsonPropertyDescription("Additional information reported by the last command feedback")
	private String commandFeedbackInfo;
}
