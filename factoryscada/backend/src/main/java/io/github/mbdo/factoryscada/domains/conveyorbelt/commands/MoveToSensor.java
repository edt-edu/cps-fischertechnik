package io.github.mbdo.factoryscada.domains.conveyorbelt.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveToSensor extends AbstractCommand<ConveyorBeltMachine> {

    public MoveToSensor(ConveyorBeltMachine conveyorBeltMachine, GenericMachineCommandDTO<ConveyorBeltMachine> forwardLeaveDTO) {
        super(conveyorBeltMachine, forwardLeaveDTO);
    }


	@Override
	public GenericMachineCommandDTO<ConveyorBeltMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}
}

