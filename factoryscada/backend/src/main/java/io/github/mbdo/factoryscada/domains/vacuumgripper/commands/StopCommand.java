package io.github.mbdo.factoryscada.domains.vacuumgripper.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StopCommand extends AbstractCommand<VacuumGripperMachine> {

    public StopCommand(VacuumGripperMachine vacuumGripperMachine, GenericMachineCommandDTO<VacuumGripperMachine> stopDTO) {
        super(vacuumGripperMachine, stopDTO);
    }

	@Override
	public GenericMachineCommandDTO<VacuumGripperMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}
}
