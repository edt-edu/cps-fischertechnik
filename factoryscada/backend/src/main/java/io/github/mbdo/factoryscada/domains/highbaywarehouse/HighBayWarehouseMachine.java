package io.github.mbdo.factoryscada.domains.highbaywarehouse;

import java.util.List;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.socket.Protocol;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HighBayWarehouseMachine extends AbstractMachine {

    public HighBayWarehouseMachine(String name, Protocol protocol, List<String> rawCommandNames) {
        super(name, protocol, rawCommandNames);
    }

    public static String getType() {
        return "highBayWarehouse";
    }

//    public void eject(@Valid @NotNull final EjectDTO ejectDTO) {
//        log.info("Eject sortingLine {}", ejectDTO);
//        new EjectCommand(this, ejectDTO);
//    }

}
