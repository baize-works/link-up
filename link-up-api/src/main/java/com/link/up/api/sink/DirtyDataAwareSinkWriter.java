package com.link.up.api.sink;

import com.link.up.api.dirtydata.DirtyDataContext;
import com.link.up.api.dirtydata.DirtyDataSummary;

/**
 * Optional sink extension for task-scoped dirty-data reporting.
 */
public interface DirtyDataAwareSinkWriter {
    void configureDirtyData(DirtyDataContext context) throws Exception;

    DirtyDataSummary getDirtyDataSummary();
}
