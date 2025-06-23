package io.github.mbdo.factoryscada.domains.vacuumgripper.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderedMoveToCommand extends AbstractCommand<VacuumGripperMachine> {

    public OrderedMoveToCommand(VacuumGripperMachine vacuumGripperMachine, GenericMachineCommandDTO<VacuumGripperMachine> ordered_move_toDTO) {
        super(vacuumGripperMachine, ordered_move_toDTO);
    }

	@Override
	public GenericMachineCommandDTO<VacuumGripperMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}
}
