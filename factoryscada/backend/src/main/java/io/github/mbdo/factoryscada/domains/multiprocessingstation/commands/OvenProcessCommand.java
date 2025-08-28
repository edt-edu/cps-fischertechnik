package io.github.mbdo.factoryscada.domains.multiprocessingstation.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OvenProcessCommand extends AbstractCommand<MultiProcessingStationMachine> {

    public OvenProcessCommand(MultiProcessingStationMachine machine, GenericMachineCommandDTO<MultiProcessingStationMachine> DTO) {
        super(machine, DTO);
    }

	@Override
	public GenericMachineCommandDTO<MultiProcessingStationMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override
//    public void execute() {
//        log.info("conveyor, `stop` command called`");
//    }
}