package com.link.up.api.sink;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable input for sink preparation before tasks are created.
 */
public final class SinkPrepareContext {
    private final ReadonlyConfig options;
    private final Map<TablePath, CatalogTable> sourceTables;

    public SinkPrepareContext(ReadonlyConfig options, Map<TablePath, CatalogTable> sourceTables) {
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.sourceTables = Collections.unmodifiableMap(new LinkedHashMap<TablePath, CatalogTable>(
                Objects.requireNonNull(sourceTables, "sourceTables must not be null")));
    }

    public ReadonlyConfig getOptions() {
        return options;
    }

    public Map<TablePath, CatalogTable> getSourceTables() {
        return sourceTables;
    }
}
