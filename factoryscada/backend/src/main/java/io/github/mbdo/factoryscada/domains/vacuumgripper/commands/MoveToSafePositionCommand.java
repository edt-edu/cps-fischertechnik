package io.github.mbdo.factoryscada.domains.vacuumgripper.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveToSafePositionCommand extends AbstractCommand<VacuumGripperMachine> {

    public MoveToSafePositionCommand(VacuumGripperMachine machine, GenericMachineCommandDTO<VacuumGripperMachine> move_to_safe_positionDTO) {
        super(machine, move_to_safe_positionDTO);
    }

	@Override
	public GenericMachineCommandDTO<VacuumGripperMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override
//    public void execute() {
//        log.info("conveyor, `stop` command called`");
//    }
}