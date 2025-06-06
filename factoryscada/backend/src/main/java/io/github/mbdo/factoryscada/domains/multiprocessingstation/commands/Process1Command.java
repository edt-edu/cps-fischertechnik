package io.github.mbdo.factoryscada.domains.multiprocessingstation.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.multiprocessingstation.MultiProcessingStationMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Process1Command extends AbstractCommand<MultiProcessingStationMachine> {

    public Process1Command(MultiProcessingStationMachine machine, GenericMachineCommandDTO<MultiProcessingStationMachine> stopDTO) {
        super(machine, stopDTO);
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
