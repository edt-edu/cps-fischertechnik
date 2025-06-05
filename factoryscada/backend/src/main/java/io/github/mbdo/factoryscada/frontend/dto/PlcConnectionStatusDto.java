package io.github.mbdo.factoryscada.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlcConnectionStatusDto {
	String controllerName;
	boolean commandPortConnected;
	boolean notificationPortConnected;
}
