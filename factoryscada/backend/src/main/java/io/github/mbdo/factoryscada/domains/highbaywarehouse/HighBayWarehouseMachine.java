package io.github.mbdo.factoryscada.domains.highbaywarehouse;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.socket.Protocol;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HighBayWarehouseMachine extends AbstractMachine {

    public HighBayWarehouseMachine(String name, Protocol protocol) {
        super(name, protocol);
    }

    public static String getType() {
        return "highBayWarehouse";
    }

//    public void eject(@Valid @NotNull final EjectDTO ejectDTO) {
//        log.info("Eject sortingLine {}", ejectDTO);
//        new EjectCommand(this, ejectDTO);
//    }

}
