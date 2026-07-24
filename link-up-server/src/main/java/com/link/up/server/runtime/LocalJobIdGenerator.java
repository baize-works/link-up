package com.link.up.server.runtime;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 当前进程内唯一的 Job ID 生成器。
 */
public final class LocalJobIdGenerator
        implements JobIdGenerator {

    private final AtomicLong sequence =
            new AtomicLong();

    public String nextId() {
        return "flux-"
                + System.currentTimeMillis()
                + "-"
                + sequence.incrementAndGet();
    }
}