package com.link.up.framework.execution.split;

import com.link.up.api.source.SourceSplit;
import com.link.up.framework.execution.CancellationToken;

import java.util.*;

/**
 * Local blocking split provider. A split has exactly one terminal completion.
 */
public final class LocalSplitQueue<SplitT extends SourceSplit> implements SplitProvider<SplitT> {
    private final ArrayDeque<SplitT> pending = new ArrayDeque<SplitT>();
    private final Map<String, State> states = new HashMap<String, State>();
    private boolean cancelled;

    public LocalSplitQueue(Collection<SplitT> splits) {
        Objects.requireNonNull(splits, "splits must not be null");
        for (SplitT split : splits) {
            if (split == null || split.splitId() == null || states.put(split.splitId(), State.PENDING) != null) {
                throw new IllegalArgumentException("splits must have unique non-null IDs");
            }
            pending.add(split);
        }
    }

    @Override
    public synchronized SplitT acquire(CancellationToken token) throws InterruptedException {
        while (pending.isEmpty() && !isTerminal() && !cancelled && !token.isCancelled()) wait();
        if (cancelled || token.isCancelled() || Thread.currentThread().isInterrupted()) return null;
        SplitT split = pending.poll();
        if (split != null) states.put(split.splitId(), State.RUNNING);
        return split;
    }

    @Override
    public synchronized void complete(SplitT split) {
        transition(split, State.COMPLETED);
    }

    @Override
    public synchronized void fail(SplitT split, Throwable cause) {
        transition(split, State.FAILED);
    }

    @Override
    public synchronized void returnSplit(SplitT split) {
        requireRunning(split);
        states.put(split.splitId(), State.PENDING);
        pending.addFirst(split);
        notifyAll();
    }

    @Override
    public synchronized void cancel(Throwable cause) {
        cancelled = true;
        notifyAll();
    }

    private void transition(SplitT split, State target) {
        requireRunning(split);
        states.put(split.splitId(), target);
        notifyAll();
    }

    private void requireRunning(SplitT split) {
        if (split == null || states.get(split.splitId()) != State.RUNNING)
            throw new IllegalStateException("Split is not running: " + (split == null ? null : split.splitId()));
    }

    private boolean isTerminal() {
        for (State state : states.values()) if (state == State.PENDING || state == State.RUNNING) return false;
        return true;
    }

    @Override
    public synchronized long getTotalSplitCount() {
        return states.size();
    }

    @Override
    public synchronized long getPendingSplitCount() {
        return count(State.PENDING);
    }

    @Override
    public synchronized long getRunningSplitCount() {
        return count(State.RUNNING);
    }

    @Override
    public synchronized long getCompletedSplitCount() {
        return count(State.COMPLETED);
    }

    @Override
    public synchronized long getFailedSplitCount() {
        return count(State.FAILED);
    }

    private long count(State state) {
        long n = 0;
        for (State value : states.values()) if (value == state) n++;
        return n;
    }

    private enum State {PENDING, RUNNING, COMPLETED, FAILED}
}
