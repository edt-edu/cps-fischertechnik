package io.github.mbdo.factoryscada.utilities;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedLogBuffer<T> {
    private final int maxSize;
    private final Deque<T> deque = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    public BoundedLogBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    public void add(T msg) {
        lock.lock();
        try {
            if (deque.size() == maxSize) {
                deque.removeFirst(); // remove oldest
            }
            deque.addLast(msg);
        } finally {
            lock.unlock();
        }
    }

    public Deque<T> snapshot() {
        lock.lock();
        try {
            return new ArrayDeque<>(deque);
        } finally {
            lock.unlock();
        }
    }
}
