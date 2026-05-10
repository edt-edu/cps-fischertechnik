package fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinecommands.TestCommandKind;
import fr.inria.mbdo.mission.runtime.rtc.event.DomainEvent;

public record TestMachineCommandChanged(
        TestCommandKind currentCommand,
        Instant timestamp,
        UUID correlationId,
        Optional<UUID> causationId) implements DomainEvent {
    public static TestMachineCommandChanged now(TestCommandKind currentCommand) {
        return new TestMachineCommandChanged(currentCommand, Instant.now(), UUID.randomUUID(), Optional.empty());
    }
}
