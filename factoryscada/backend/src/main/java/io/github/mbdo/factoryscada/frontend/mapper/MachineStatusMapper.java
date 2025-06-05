package io.github.mbdo.factoryscada.frontend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import io.github.mbdo.factoryscada.domain.MachineStatus;
import io.github.mbdo.factoryscada.frontend.dto.MachineStatusDTO;

@Mapper
public interface MachineStatusMapper {

	MachineStatusMapper INSTANCE = Mappers.getMapper(MachineStatusMapper.class);
	
	MachineStatusDTO machineStatusToMachineStatusDTO(MachineStatus machineStatus);
	MachineStatus machineStatusDTOToMachineStatus(MachineStatusDTO machineStatusDTO);
	
}
