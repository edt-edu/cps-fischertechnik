package io.github.mbdo.factoryscada.domains.multiprocessingstation.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SetupCommand extends AbstractCommand<MultiProcessingStationMachine> {

    public SetupCommand(MultiProcessingStationMachine machine, GenericMachineCommandDTO<MultiProcessingStationMachine> setupDTO) {
        super(machine, setupDTO);
    }

	@Override
	public GenericMachineCommandDTO<MultiProcessingStationMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}
}
