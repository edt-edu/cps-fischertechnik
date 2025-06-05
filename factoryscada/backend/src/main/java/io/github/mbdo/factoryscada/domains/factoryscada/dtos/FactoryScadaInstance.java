package io.github.mbdo.factoryscada.domains.factoryscada.dtos;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.socket.Protocol;

import java.io.Serializable;
import java.util.Map;

public record FactoryScadaInstance(
        Map<String, Protocol> controllers,
        Map<String, AbstractMachine> machines
) implements Serializable {
}
