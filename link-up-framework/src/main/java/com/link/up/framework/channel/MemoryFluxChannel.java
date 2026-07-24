package com.link.up.framework.channel;

import java.util.concurrent.ArrayBlockingQueue;

public final class MemoryFluxChannel<T> implements Channel<T> {
    private final ArrayBlockingQueue<T> queue;

    public MemoryFluxChannel(int capacity) {
        queue = new ArrayBlockingQueue<T>(capacity);
    }

    public void put(T value) throws InterruptedException {
        queue.put(value);
    }

    public T take() throws InterruptedException {
        return queue.take();
    }
}
