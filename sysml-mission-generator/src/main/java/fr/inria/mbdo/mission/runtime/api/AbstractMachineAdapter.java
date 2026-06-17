package fr.inria.mbdo.mission.runtime.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import fr.inria.mbdo.mission.runtime.rtc.event.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractMachineAdapter implements MachineAdapter {
    protected final String id;
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> subscribers = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(AbstractMachineAdapter.class);

    public AbstractMachineAdapter(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public void shutdown() {
        logger.info("{}: manual shutdown", id);
    }

    @Override
    public void publish(Event event) {
        logger.info("{}: publishing event {}", id, event.getClass().getSimpleName());
        boolean foundSubscriber = false;
        for (Map.Entry<Class<?>, CopyOnWriteArrayList<Consumer<?>>> entry : subscribers.entrySet()) {
            if (!entry.getKey().isInstance(event))
                continue;
            for (Consumer<?> handler : entry.getValue()) {
                foundSubscriber = true;
                try {
                    ((Consumer) handler).accept(event);
                } catch (Throwable t) {
                    logger.warn("handler threw", t);
                }
            }
        }

        if (!foundSubscriber) {
            logger.warn("{}: no subscribers found", id);
        }
    }

    public <E extends Event> void subscribe(Class<E> eventType, Consumer<E> handler) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
    }
}
