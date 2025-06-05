package io.github.mbdo.factoryscada.domains.conveyorbelt.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveNbStepsCommand extends AbstractCommand<ConveyorBeltMachine> {

    public MoveNbStepsCommand(ConveyorBeltMachine conveyorBeltMachine, GenericMachineCommandDTO<ConveyorBeltMachine> gotoConfigDTO) {
        super(conveyorBeltMachine, gotoConfigDTO);
    }

	@Override
	public GenericMachineCommandDTO<ConveyorBeltMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}


}

