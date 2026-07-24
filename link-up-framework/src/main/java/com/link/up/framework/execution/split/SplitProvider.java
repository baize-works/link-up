package com.link.up.framework.execution.split;

import com.link.up.api.source.SourceSplit;
import com.link.up.framework.execution.CancellationToken;

/**
 * Thread-safe ownership lifecycle for source splits.
 */
public interface SplitProvider<SplitT extends SourceSplit> {
    SplitT acquire(CancellationToken cancellationToken) throws InterruptedException;

    void complete(SplitT split);

    void returnSplit(SplitT split);

    void fail(SplitT split, Throwable cause);

    void cancel(Throwable cause);

    long getTotalSplitCount();

    long getPendingSplitCount();

    long getRunningSplitCount();

    long getCompletedSplitCount();

    long getFailedSplitCount();
}
