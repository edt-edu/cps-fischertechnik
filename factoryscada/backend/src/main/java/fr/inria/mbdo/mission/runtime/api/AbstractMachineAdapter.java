package fr.inria.mbdo.mission.runtime.api;

import fr.inria.mbdo.mission.runtime.rtc.event.Event;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class AbstractMachineAdapter implements MachineAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMachineAdapter.class);

    @Getter
    protected final String id;
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> subscribers = new ConcurrentHashMap<>();

    public AbstractMachineAdapter(String id) {
        this.id = id;
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
