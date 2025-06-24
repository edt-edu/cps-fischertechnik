package io.github.mbdo.factoryscada.domains.multiprocessingstation.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessCommand extends AbstractCommand<MultiProcessingStationMachine> {

    public ProcessCommand(MultiProcessingStationMachine machine, GenericMachineCommandDTO<MultiProcessingStationMachine> processDTO) {
        super(machine, processDTO);
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
