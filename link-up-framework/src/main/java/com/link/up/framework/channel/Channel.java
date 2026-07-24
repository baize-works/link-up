package com.link.up.framework.channel;

public interface Channel<T> {
    void put(T value) throws InterruptedException;

    T take() throws InterruptedException;
}
