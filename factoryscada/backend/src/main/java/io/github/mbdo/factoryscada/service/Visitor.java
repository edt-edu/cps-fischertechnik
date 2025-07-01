package io.github.mbdo.factoryscada.service;

import io.github.mbdo.factoryscada.domains.mission.EntryNode;
import io.github.mbdo.factoryscada.domains.mission.Fork;
import io.github.mbdo.factoryscada.domains.mission.Join;
import io.github.mbdo.factoryscada.domains.mission.RawMachineCommand;
import io.github.mbdo.factoryscada.domains.mission.WaitAction;

public abstract class Visitor {

    public abstract void visitFork(Fork node);

    public abstract void visitJoin(Join node);

    public abstract void visitRawMachineCommand(RawMachineCommand node);

    public abstract void visitWaitAction(WaitAction node);

    public abstract void visitEntryNode(EntryNode node);

}
