package com.link.up.framework.metrics;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Channel occupancy, backpressure and rate-limit metrics.
 */
public final class ChannelMetrics {
    private final String channelId;
    private final AtomicLong enqueuedCount = new AtomicLong(), dequeuedCount = new AtomicLong();
    private final AtomicLong currentBatches = new AtomicLong(), currentRecords = new AtomicLong(), currentBytes = new AtomicLong();
    private final AtomicLong maximumBatches = new AtomicLong(), maximumRecords = new AtomicLong(), maximumBytes = new AtomicLong();
    private final AtomicLong writeBlockedNanos = new AtomicLong(), readBlockedNanos = new AtomicLong(), rateLimitedNanos = new AtomicLong(), oversizedBatches = new AtomicLong();
    private final long createdNanos = System.nanoTime();

    public ChannelMetrics(String channelId) {
        this.channelId = Objects.requireNonNull(channelId, "channelId must not be null");
    }

    private static void set(AtomicLong current, long value, AtomicLong max) {
        current.set(value);
        long old;
        do {
            old = max.get();
            if (value <= old) return;
        } while (!max.compareAndSet(old, value));
    }

    public void recordEnqueued(long batches, long records, long bytes) {
        enqueuedCount.incrementAndGet();
        set(currentBatches, batches, maximumBatches);
        set(currentRecords, records, maximumRecords);
        set(currentBytes, bytes, maximumBytes);
    }

    public void recordDequeued(long batches, long records, long bytes) {
        dequeuedCount.incrementAndGet();
        currentBatches.set(batches);
        currentRecords.set(records);
        currentBytes.set(bytes);
    }

    /**
     * Legacy batch-only metric API.
     */
    public void recordEnqueued(int size) {
        recordEnqueued(size, 0, 0);
    }

    public void recordDequeued(int size) {
        recordDequeued(size, 0, 0);
    }

    public void addWriteBlockedNanos(long n) {
        if (n > 0) writeBlockedNanos.addAndGet(n);
    }

    public void addReadBlockedNanos(long n) {
        if (n > 0) readBlockedNanos.addAndGet(n);
    }

    public void addRateLimitedNanos(long n) {
        if (n > 0) rateLimitedNanos.addAndGet(n);
    }

    public void recordOversizedBatch() {
        oversizedBatches.incrementAndGet();
    }

    public String getChannelId() {
        return channelId;
    }

    public long getEnqueuedCount() {
        return enqueuedCount.get();
    }

    public long getDequeuedCount() {
        return dequeuedCount.get();
    }

    public int getCurrentSize() {
        return (int) currentBatches.get();
    }

    public int getMaximumSize() {
        return (int) maximumBatches.get();
    }

    public long getCurrentBatches() {
        return currentBatches.get();
    }

    public long getCurrentRecords() {
        return currentRecords.get();
    }

    public long getCurrentBytes() {
        return currentBytes.get();
    }

    public long getMaximumBatches() {
        return maximumBatches.get();
    }

    public long getMaximumRecords() {
        return maximumRecords.get();
    }

    public long getMaximumBytes() {
        return maximumBytes.get();
    }

    public long getOversizedBatches() {
        return oversizedBatches.get();
    }

    public long getWriteBlockedMillis() {
        return writeBlockedNanos.get() / 1_000_000L;
    }

    public long getReadBlockedMillis() {
        return readBlockedNanos.get() / 1_000_000L;
    }

    public long getRateLimitedMillis() {
        return rateLimitedNanos.get() / 1_000_000L;
    }

    public long getProducerBackpressureMillis() {
        return getWriteBlockedMillis();
    }

    public long getConsumerIdleMillis() {
        return getReadBlockedMillis();
    }

    private double ratio(long nanos) {
        return Math.min(1D, nanos / (double) Math.max(1, System.nanoTime() - createdNanos));
    }

    public double getProducerBackpressureRatio() {
        return ratio(writeBlockedNanos.get());
    }

    public double getConsumerIdleRatio() {
        return ratio(readBlockedNanos.get());
    }

    public double getRateLimitedRatio() {
        return ratio(rateLimitedNanos.get());
    }

    /**
     * @deprecated combines producer backpressure, consumer idle time and rate limiting.
     */
    public double getBlockedRatio() {
        long elapsed = Math.max(1, System.nanoTime() - createdNanos);
        return Math.min(1D, (writeBlockedNanos.get() + readBlockedNanos.get() + rateLimitedNanos.get()) / (double) elapsed);
    }
}
