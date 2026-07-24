package com.link.up.framework.channel;

import com.link.up.api.channel.FluxRowBatch;
import com.link.up.api.source.RecordBatch;
import com.link.up.api.source.RecordSizeEstimator;
import com.link.up.framework.metrics.ChannelMetrics;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A local channel with atomic batch, record and byte backpressure.
 */
public final class LocalDataChannel<T> implements DataChannel<T> {
    private final String channelId;
    private final int maxBatches, expectedProducers;
    private final long maxRecords, maxBytes, maxRecordsPerSecond, maxBytesPerSecond;
    private final Deque<Entry<T>> buffer = new ArrayDeque<Entry<T>>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition(), notFull = lock.newCondition();
    private final ChannelMetrics metrics;
    private int createdWriters, remainingProducers;
    private long bufferedRecords, bufferedBytes;
    private boolean readerOpened, cancelled;
    private Throwable failure;
    private double recordTokens, byteTokens;
    private long lastRefillNanos = System.nanoTime();

    public LocalDataChannel(String id, int capacity, int producers) {
        this(id, capacity, -1, -1, -1, -1, producers);
    }

    public LocalDataChannel(String id, int batches, long records, long bytes, long recordsPerSecond, long bytesPerSecond, int producers) {
        if (batches <= 0 && records <= 0 && bytes <= 0)
            throw new IllegalArgumentException("at least one buffer limit must be greater than 0");
        if (producers <= 0) throw new IllegalArgumentException("expectedProducers must be greater than 0");
        checkOptional(records, "maxRecords");
        checkOptional(bytes, "maxBytes");
        checkOptional(recordsPerSecond, "maxRecordsPerSecond");
        checkOptional(bytesPerSecond, "maxBytesPerSecond");
        channelId = Objects.requireNonNull(id, "channelId must not be null");
        maxBatches = batches;
        maxRecords = records;
        maxBytes = bytes;
        maxRecordsPerSecond = recordsPerSecond;
        maxBytesPerSecond = bytesPerSecond;
        expectedProducers = producers;
        remainingProducers = producers;
        recordTokens = recordsPerSecond > 0 ? recordsPerSecond : 0;
        byteTokens = bytesPerSecond > 0 ? bytesPerSecond : 0;
        metrics = new ChannelMetrics(id);
    }

    private static void checkOptional(long v, String n) {
        if (v == 0 || v < -1) throw new IllegalArgumentException(n + " must be positive or -1");
    }

    private static long records(Object value) {
        if (value instanceof RecordEnvelope) return ((RecordEnvelope<?>) value).getBatch().size();
        if (value instanceof RecordBatch) return ((RecordBatch<?>) value).size();
        if (value instanceof FluxRowBatch) return ((FluxRowBatch) value).size();
        return 1;
    }

    private static long bytes(Object value) {
        if (value instanceof RecordEnvelope) return batchBytes(((RecordEnvelope<?>) value).getBatch());
        if (value instanceof RecordBatch) return batchBytes((RecordBatch<?>) value);
        if (value instanceof FluxRowBatch) {
            long n = 32;
            for (Object row : ((FluxRowBatch) value).getRows()) n += RecordSizeEstimator.estimateObjectSizeBytes(row);
            return n;
        }
        return RecordSizeEstimator.estimateObjectSizeBytes(value);
    }

    private static long batchBytes(RecordBatch<?> batch) {
        return batch.getEstimatedBytes();
    }

    public ChannelWriter<T> openWriter() {
        lock.lock();
        try {
            ensureOpen();
            if (createdWriters >= expectedProducers)
                throw new IllegalStateException("Too many writers created for channel '" + channelId + "'");
            createdWriters++;
            return new Writer();
        } finally {
            lock.unlock();
        }
    }

    public ChannelReader<T> openReader() {
        lock.lock();
        try {
            if (readerOpened) throw new IllegalStateException("Channel '" + channelId + "' only supports one reader");
            readerOpened = true;
            return new Reader();
        } finally {
            lock.unlock();
        }
    }

    public void fail(Throwable cause) {
        Objects.requireNonNull(cause, "cause must not be null");
        lock.lock();
        try {
            if (failure == null) failure = cause;
            signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void cancel() {
        lock.lock();
        try {
            cancelled = true;
            signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void signalAll() {
        notEmpty.signalAll();
        notFull.signalAll();
    }

    public ChannelMetrics getMetrics() {
        return metrics;
    }

    public void close() {
        cancel();
    }

    private void writeInternal(T value) throws Exception {
        Objects.requireNonNull(value, "channel value must not be null");
        Entry<T> entry = new Entry<T>(value, records(value), bytes(value));
        lock.lockInterruptibly();
        try {
            boolean oversized = isOversized(entry);
            while (!fits(entry, oversized)) {
                long start = System.nanoTime();
                try {
                    notFull.await();
                } finally {
                    metrics.addWriteBlockedNanos(System.nanoTime() - start);
                }
                ensureOpen();
            }
            if (oversized) metrics.recordOversizedBatch();
            awaitRate(entry);
            ensureOpen();
            buffer.addLast(entry);
            bufferedRecords += entry.records;
            bufferedBytes += entry.bytes;
            metrics.recordEnqueued(buffer.size(), bufferedRecords, bufferedBytes);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    private boolean fits(Entry<T> e, boolean oversized) {
        if (failure != null || cancelled) return true;
        if (maxBatches > 0 && buffer.size() >= maxBatches) return false;
        if (oversized) return buffer.isEmpty();
        return (maxRecords <= 0 || bufferedRecords + e.records <= maxRecords) && (maxBytes <= 0 || bufferedBytes + e.bytes <= maxBytes);
    }

    private boolean isOversized(Entry<T> e) {
        return (maxRecords > 0 && e.records > maxRecords) || (maxBytes > 0 && e.bytes > maxBytes);
    }

    private void awaitRate(Entry<T> e) throws InterruptedException {
        while (true) {
            ensureOpen();
            refill();
            double recordDeficit = maxRecordsPerSecond > 0 && e.records <= maxRecordsPerSecond ? e.records - recordTokens : 0, byteDeficit = maxBytesPerSecond > 0 && e.bytes <= maxBytesPerSecond ? e.bytes - byteTokens : 0;
            if (recordDeficit <= 0 && byteDeficit <= 0) {
                if (maxRecordsPerSecond > 0 && e.records <= maxRecordsPerSecond) recordTokens -= e.records;
                if (maxBytesPerSecond > 0 && e.bytes <= maxBytesPerSecond) byteTokens -= e.bytes;
                return;
            }
            long wait = Math.max(maxRecordsPerSecond > 0 ? (long) Math.ceil(recordDeficit * 1_000_000_000D / maxRecordsPerSecond) : 0, maxBytesPerSecond > 0 ? (long) Math.ceil(byteDeficit * 1_000_000_000D / maxBytesPerSecond) : 0);
            long start = System.nanoTime();
            notFull.awaitNanos(Math.max(1L, wait));
            metrics.addRateLimitedNanos(System.nanoTime() - start);
        }
    }

    private void refill() {
        long now = System.nanoTime(), elapsed = now - lastRefillNanos;
        lastRefillNanos = now;
        if (maxRecordsPerSecond > 0)
            recordTokens = Math.min(maxRecordsPerSecond, recordTokens + elapsed * (double) maxRecordsPerSecond / 1_000_000_000D);
        if (maxBytesPerSecond > 0)
            byteTokens = Math.min(maxBytesPerSecond, byteTokens + elapsed * (double) maxBytesPerSecond / 1_000_000_000D);
    }

    private T readInternal() throws Exception {
        lock.lockInterruptibly();
        try {
            while (buffer.isEmpty() && remainingProducers > 0 && failure == null && !cancelled) {
                long start = System.nanoTime();
                try {
                    notEmpty.await();
                } finally {
                    metrics.addReadBlockedNanos(System.nanoTime() - start);
                }
            }
            if (!buffer.isEmpty()) {
                Entry<T> e = buffer.removeFirst();
                bufferedRecords -= e.records;
                bufferedBytes -= e.bytes;
                metrics.recordDequeued(buffer.size(), bufferedRecords, bufferedBytes);
                notFull.signalAll();
                return e.value;
            }
            ensureOpen();
            return null;
        } finally {
            lock.unlock();
        }
    }

    private void producerFinished() {
        lock.lock();
        try {
            if (remainingProducers > 0) remainingProducers--;
            if (remainingProducers == 0) notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void ensureOpen() {
        if (failure != null) throw new IllegalStateException("Channel '" + channelId + "' execution failed", failure);
        if (cancelled) throw new CancellationException("Channel '" + channelId + "' has been cancelled");
    }

    private static final class Entry<E> {
        final E value;
        final long records, bytes;

        Entry(E value, long records, long bytes) {
            this.value = value;
            this.records = records;
            this.bytes = bytes;
        }
    }

    private final class Writer implements ChannelWriter<T> {
        private final AtomicBoolean closed = new AtomicBoolean();

        public void write(T value) throws Exception {
            if (closed.get()) throw new IllegalStateException("Channel writer has already been closed");
            writeInternal(value);
        }

        public void fail(Throwable cause) {
            LocalDataChannel.this.fail(cause);
        }

        public void close() {
            if (closed.compareAndSet(false, true)) producerFinished();
        }
    }

    private final class Reader implements ChannelReader<T> {
        public T read() throws Exception {
            return readInternal();
        }
    }
}
