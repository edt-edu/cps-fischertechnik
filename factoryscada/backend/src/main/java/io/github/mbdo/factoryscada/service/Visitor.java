package io.github.mbdo.factoryscada.service;

import io.github.mbdo.factoryscada.domains.mission.dtos.EntryNode_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.Fork_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.Join_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.RawMachineCommand_dto;
import io.github.mbdo.factoryscada.domains.mission.dtos.WaitAction_dto;

public abstract class Visitor {

    public abstract void visitFork(Fork_dto node);

    public abstract void visitJoin(Join_dto node);

    public abstract void visitRawMachineCommand(RawMachineCommand_dto node);

    public abstract void visitWaitAction(WaitAction_dto node);

    public abstract void visitEntryNode(EntryNode_dto node);

}
