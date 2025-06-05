package io.github.mbdo.factoryscada.frontend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import io.github.mbdo.factoryscada.domain.CommandStatus;
import io.github.mbdo.factoryscada.frontend.dto.CommandStatusDTO;

@Mapper
public interface CommandStatusMapper {

	CommandStatusMapper INSTANCE = Mappers.getMapper(CommandStatusMapper.class);
	
	CommandStatusDTO commandStatusToCommandStatusDTO(CommandStatus commandStatus);
	CommandStatus commandStatusDTOToCommandStatus(CommandStatusDTO commandStatusDTO);
	
}
