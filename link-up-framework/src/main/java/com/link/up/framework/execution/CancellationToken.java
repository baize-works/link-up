package com.link.up.framework.execution;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Job 级取消信号。
 */
public final class CancellationToken {

    private final AtomicBoolean cancelled =
            new AtomicBoolean(false);

    private final AtomicReference<Throwable> cause =
            new AtomicReference<Throwable>();

    public boolean cancel(Throwable cancellationCause) {
        if (cancellationCause != null) {
            cause.compareAndSet(
                    null,
                    cancellationCause);
        }

        return cancelled.compareAndSet(
                false,
                true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public Throwable getCause() {
        return cause.get();
    }

    public void throwIfCancelled() {
        if (!isCancelled()) {
            return;
        }

        CancellationException exception =
                new CancellationException(
                        "Job execution has been cancelled");

        Throwable cancellationCause =
                cause.get();

        if (cancellationCause != null) {
            exception.initCause(
                    cancellationCause);
        }

        throw exception;
    }
}