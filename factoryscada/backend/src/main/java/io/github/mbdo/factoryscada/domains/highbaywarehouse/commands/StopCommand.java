package io.github.mbdo.factoryscada.domains.highbaywarehouse.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.HighBayWarehouseMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StopCommand extends AbstractCommand<HighBayWarehouseMachine> {

    public StopCommand(HighBayWarehouseMachine machine, GenericMachineCommandDTO<HighBayWarehouseMachine> stopDTO) {
        super(machine, stopDTO);
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