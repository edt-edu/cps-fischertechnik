package fr.inria.mbdo.mission.runtime.api;

import fr.inria.mbdo.mission.runtime.rtc.event.Event;

import java.util.function.Consumer;

public interface MachineAdapter {

    String getId();

    void shutdown();

    void publish(Event event);

    <E extends Event> void subscribe(Class<E> eventType, Consumer<E> handler);
}
