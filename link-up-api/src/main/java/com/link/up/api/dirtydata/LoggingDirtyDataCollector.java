package com.link.up.api.dirtydata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class LoggingDirtyDataCollector extends BoundedMemoryDirtyDataCollector {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingDirtyDataCollector.class);

    public LoggingDirtyDataCollector(String task, int samples, long count, double percentage) {
        super(task, samples, count, percentage);
    }

    @Override
    public void collect(DirtyRecord record) throws IOException {
        super.collect(record);
        LOG.warn("Dirty record: type={}, message={}, task={}", record.getErrorType(), record.getErrorMessage(), record.getContext().getTaskId());
    }
}
