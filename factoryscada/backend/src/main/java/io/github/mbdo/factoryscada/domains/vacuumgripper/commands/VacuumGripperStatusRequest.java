package io.github.mbdo.factoryscada.domains.vacuumgripper.commands;

import io.github.mbdo.factoryscada.core.GenericMachineStatusRequestDTO;
import io.github.mbdo.factoryscada.core.StatusRequest;
import io.github.mbdo.factoryscada.domains.vacuumgripper.VacuumGripperMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VacuumGripperStatusRequest extends StatusRequest<VacuumGripperMachine> {
    public VacuumGripperStatusRequest(VacuumGripperMachine vacuumGripperMachine, GenericMachineStatusRequestDTO<VacuumGripperMachine> requestDTO) {
        super(vacuumGripperMachine, requestDTO);
    }

	@Override
	public GenericMachineStatusRequestDTO<VacuumGripperMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}
}
