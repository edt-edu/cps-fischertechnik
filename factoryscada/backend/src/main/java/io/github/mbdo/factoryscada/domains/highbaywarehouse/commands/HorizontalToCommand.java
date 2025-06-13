package io.github.mbdo.factoryscada.domains.highbaywarehouse.commands;

import io.github.mbdo.factoryscada.core.AbstractCommand;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.highbaywarehouse.HighBayWarehouseMachine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HorizontalToCommand extends AbstractCommand<HighBayWarehouseMachine> {

    public HorizontalToCommand(HighBayWarehouseMachine machine, GenericMachineCommandDTO<HighBayWarehouseMachine> horizontal_toDTO) {
        super(machine, horizontal_toDTO);
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