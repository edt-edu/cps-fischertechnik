package io.github.mbdo.factoryscada.domains.sortingline.commands;

import java.time.Instant;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.sortingline.SortingLineMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StopCommand extends AbstractCommand<SortingLineMachine> {

    public StopCommand(SortingLineMachine machine, GenericMachineCommandDTO<SortingLineMachine> stopDTO) {
        super(machine, stopDTO);
    }

	@Override
	public GenericMachineCommandDTO<SortingLineMachine> buildDTO() {
		GenericMachineCommandDTO<SortingLineMachine> dto = new GenericMachineCommandDTO<SortingLineMachine>(
				this.getMachine().getName(),
				Instant.now().toString(), 
				null); // TODO implement message for this command
		return dto;
	}

}
