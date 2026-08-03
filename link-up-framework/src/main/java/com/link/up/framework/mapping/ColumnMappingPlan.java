package com.link.up.framework.mapping;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.type.FluxRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable runtime plan for projecting and reordering one dataset. */
public final class ColumnMappingPlan {

    private final int[] sourceIndexes;
    private final CatalogTable outputTable;

    ColumnMappingPlan(int[] sourceIndexes, CatalogTable outputTable) {
        this.sourceIndexes = sourceIndexes.clone();
        this.outputTable = Objects.requireNonNull(outputTable, "outputTable must not be null");
    }

    public CatalogTable getOutputTable() {
        return outputTable;
    }

    public List<FluxRow> project(List<FluxRow> rows) {
        Objects.requireNonNull(rows, "rows must not be null");
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<FluxRow> result = new ArrayList<FluxRow>(rows.size());
        for (FluxRow row : rows) {
            if (row == null) {
                throw new IllegalArgumentException("source rows must not contain null values");
            }
            result.add(row.project(sourceIndexes));
        }
        return result;
    }
}
