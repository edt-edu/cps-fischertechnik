package io.github.mbdo.factoryscada.mission.dsl.visitor;

import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.EntryNode_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.Fork_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.Join_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.RawMachineCommand_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.WaitAction_dto;

public abstract class Visitor {

    public abstract void visit(Fork_dto node);

    public abstract void visit(Join_dto node);

    public abstract void visit(RawMachineCommand_dto node);

    public abstract void visit(WaitAction_dto node);

    public abstract void visit(EntryNode_dto node);

}
