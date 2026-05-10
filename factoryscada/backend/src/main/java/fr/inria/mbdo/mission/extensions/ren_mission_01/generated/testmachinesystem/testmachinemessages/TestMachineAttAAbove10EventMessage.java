package fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import fr.inria.mbdo.mission.runtime.rtc.event.DomainEvent;

/**
 * How to generate this ? TODO
 */
public record TestMachineAttAAbove10EventMessage(
        int attA,
        Instant timestamp,
        UUID correlationId,
        Optional<UUID> causationId) implements DomainEvent {
    public static TestMachineAttAAbove10EventMessage now(int attA) {
        return new TestMachineAttAAbove10EventMessage(attA, Instant.now(), UUID.randomUUID(), Optional.empty());
    }
}
