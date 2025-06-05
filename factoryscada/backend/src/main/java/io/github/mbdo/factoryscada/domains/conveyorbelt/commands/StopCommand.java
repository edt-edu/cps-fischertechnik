package io.github.mbdo.factoryscada.domains.conveyorbelt.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StopCommand extends AbstractCommand<ConveyorBeltMachine> {

    public StopCommand(ConveyorBeltMachine conveyorBeltMachine, GenericMachineCommandDTO<ConveyorBeltMachine> stopDTO) {
        super(conveyorBeltMachine, stopDTO);
    }

	@Override
	public GenericMachineCommandDTO<ConveyorBeltMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override
//    public void execute() {
//        log.info("conveyor, `stop` command called`");
//    }
}
