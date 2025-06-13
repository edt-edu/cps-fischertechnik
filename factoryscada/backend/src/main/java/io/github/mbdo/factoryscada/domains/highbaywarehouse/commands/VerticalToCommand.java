package io.github.mbdo.factoryscada.domains.highbaywarehouse.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.HighBayWarehouseMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerticalToCommand extends AbstractCommand<HighBayWarehouseMachine> {

    public VerticalToCommand(HighBayWarehouseMachine machine, GenericMachineCommandDTO<HighBayWarehouseMachine> vertical_toDTO) {
        super(machine, vertical_toDTO);
    }

	@Override
	public GenericMachineCommandDTO<HighBayWarehouseMachine> buildDTO() {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override
//    public void execute() {
//        log.info("conveyor, `stop` command called`");
//    }
}