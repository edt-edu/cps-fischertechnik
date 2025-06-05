package io.github.mbdo.factoryscada.domains.conveyorbelt.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveOutCommand extends AbstractCommand<ConveyorBeltMachine> {

    public MoveOutCommand(ConveyorBeltMachine conveyorBeltMachine, GenericMachineCommandDTO<ConveyorBeltMachine> moveDTO) {
        super(conveyorBeltMachine, moveDTO);
    }

	@Override
	public GenericMachineCommandDTO<ConveyorBeltMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}

}
