package io.github.mbdo.factoryscada.frontend.dto;

import lombok.Data;

@Data
public class MachineStatusDTO {
	private String machineName;
	
	private String machineFeedbackRawJSON;
	private String machineFeedbackStatus;
	private String machineFeedbackTimestamp;
	private String machineFeedbackInfo;
}
