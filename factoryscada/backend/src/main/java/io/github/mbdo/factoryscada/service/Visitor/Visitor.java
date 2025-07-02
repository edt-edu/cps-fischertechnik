package io.github.mbdo.factoryscada.service.Visitor;

import io.github.mbdo.factoryscada.domains.mission.dtos.EntryNode_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.Fork_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.Join_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.RawMachineCommand_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.WaitAction_dto;

public abstract class Visitor {

    public abstract void visit(Fork_dto node);

    public abstract void visit(Join_dto node);

    public abstract void visit(RawMachineCommand_dto node);

    public abstract void visit(WaitAction_dto node);

    public abstract void visit(EntryNode_dto node);

}
