package fr.inria.mbdo.mission.runtime.api;

import java.util.function.Consumer;

import fr.inria.mbdo.mission.runtime.rtc.event.Event;

public interface MachineAdapter {

    public void shutdown();

    public void publish(Event event);

    public <E extends Event> void subscribe(Class<E> eventType, Consumer<E> handler);
}
