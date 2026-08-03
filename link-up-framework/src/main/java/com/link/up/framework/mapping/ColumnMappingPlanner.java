package com.link.up.framework.mapping;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.Column;
import com.link.up.api.table.catalog.PrimaryKey;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.catalog.TableSchema;
import com.link.up.framework.job.ColumnMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds fixed column projection plans after source schema discovery. */
public final class ColumnMappingPlanner {

    public Result plan(
            Map<TablePath, CatalogTable> sourceTables,
            ColumnMapping mapping) {

        Objects.requireNonNull(sourceTables, "sourceTables must not be null");
        ColumnMapping safeMapping = mapping == null ? ColumnMapping.empty() : mapping;

        if (safeMapping.isEmpty()) {
            return new Result(sourceTables, Collections.<TablePath, ColumnMappingPlan>emptyMap());
        }

        if (sourceTables.size() != 1) {
            throw new IllegalArgumentException(
                    "Explicit column mapping currently requires exactly one source table");
        }

        Map.Entry<TablePath, CatalogTable> entry = sourceTables.entrySet().iterator().next();
        TablePath tablePath = entry.getKey();
        CatalogTable sourceTable = entry.getValue();
        TableSchema sourceSchema = sourceTable.getTableSchema();
        TableSchema.Builder outputSchema = TableSchema.builder();
        int[] sourceIndexes = new int[safeMapping.getColumns().size()];
        Map<String, String> targetBySource = new LinkedHashMap<String, String>();

        for (int i = 0; i < safeMapping.getColumns().size(); i++) {
            ColumnMapping.Item item = safeMapping.getColumns().get(i);
            int sourceIndex = sourceSchema.indexOf(item.getSource());
            if (sourceIndex < 0) {
                throw new IllegalArgumentException(
                        "Mapped source column does not exist: " + item.getSource());
            }

            Column sourceColumn = sourceSchema.getColumn(sourceIndex);
            sourceIndexes[i] = sourceIndex;
            outputSchema.column(sourceColumn.rename(item.getTarget()));
            targetBySource.put(item.getSource(), item.getTarget());
        }

        PrimaryKey primaryKey = sourceSchema.getPrimaryKey();
        if (primaryKey != null) {
            List<String> mappedPrimaryKeys = new ArrayList<String>();
            boolean complete = true;
            for (String sourcePrimaryKey : primaryKey.getColumnNames()) {
                String targetPrimaryKey = targetBySource.get(sourcePrimaryKey);
                if (targetPrimaryKey == null) {
                    complete = false;
                    break;
                }
                mappedPrimaryKeys.add(targetPrimaryKey);
            }
            if (complete) {
                outputSchema.primaryKey(
                        PrimaryKey.of(primaryKey.getName(), mappedPrimaryKeys));
            }
        }

        CatalogTable outputTable = sourceTable.withSchema(outputSchema.build());
        ColumnMappingPlan mappingPlan = new ColumnMappingPlan(sourceIndexes, outputTable);

        Map<TablePath, CatalogTable> outputTables =
                new LinkedHashMap<TablePath, CatalogTable>();
        outputTables.put(tablePath, outputTable);

        Map<TablePath, ColumnMappingPlan> plans =
                new LinkedHashMap<TablePath, ColumnMappingPlan>();
        plans.put(tablePath, mappingPlan);

        return new Result(outputTables, plans);
    }

    public static final class Result {
        private final Map<TablePath, CatalogTable> outputTables;
        private final Map<TablePath, ColumnMappingPlan> plans;

        Result(
                Map<TablePath, CatalogTable> outputTables,
                Map<TablePath, ColumnMappingPlan> plans) {
            this.outputTables = Collections.unmodifiableMap(
                    new LinkedHashMap<TablePath, CatalogTable>(outputTables));
            this.plans = Collections.unmodifiableMap(
                    new LinkedHashMap<TablePath, ColumnMappingPlan>(plans));
        }

        public Map<TablePath, CatalogTable> getOutputTables() {
            return outputTables;
        }

        public Map<TablePath, ColumnMappingPlan> getPlans() {
            return plans;
        }
    }
}
