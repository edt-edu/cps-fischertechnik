package io.github.mbdo.factoryscada.domain;

import lombok.Data;

@Data
public class MachineStatus {
	
	private String machineName;
	
	private String machineFeedbackRawJSON;
	private String machineFeedbackStatus;
	private String machineFeedbackTimestamp;
	private String machineFeedbackInfo;
	
	
}
