package io.github.mbdo.factoryscada.domain;

import lombok.Data;

@Data
public class CommandStatus {
	
	private String machineName;
	
	private String currentCommandRawJSON;
	private String currentCommandId;
	private String currentCommandName;
	private String currentCommandTimestamp;
	
	
	private String commandFeedbackRawJSON;
	private String commandFeedbackStatus;
	private String commandFeedbackTimestamp;
	private String commandFeedbackInfo;
	
	
}
