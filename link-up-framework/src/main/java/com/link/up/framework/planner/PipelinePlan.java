package com.link.up.framework.planner;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 一个逻辑数据集的不可变执行边界。
 */
public final class PipelinePlan {
    private final String pipelineId;
    private final String dataSetId;
    private final TablePath dataSetPath;
    private final CatalogTable catalogTable;
    private final List<SourceTaskPlan<?>> sourceTaskPlans;
    private final List<SinkTaskPlan> sinkTaskPlans;

    public PipelinePlan(String pipelineId, String dataSetId, CatalogTable catalogTable,
                        List<SourceTaskPlan<?>> sourceTaskPlans, List<SinkTaskPlan> sinkTaskPlans) {
        this.pipelineId = requireText(pipelineId, "pipelineId");
        this.dataSetId = requireText(dataSetId, "dataSetId");
        this.catalogTable = Objects.requireNonNull(catalogTable, "catalogTable must not be null");
        this.dataSetPath = Objects.requireNonNull(catalogTable.getTablePath(), "catalogTable.tablePath must not be null");
        this.sourceTaskPlans = immutable(sourceTaskPlans, "sourceTaskPlans");
        this.sinkTaskPlans = immutable(sinkTaskPlans, "sinkTaskPlans");
        if (this.sourceTaskPlans.isEmpty() || this.sinkTaskPlans.isEmpty())
            throw new IllegalArgumentException("pipeline tasks must not be empty");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static <T> List<T> immutable(List<T> values, String name) {
        return Collections.unmodifiableList(new ArrayList<T>(Objects.requireNonNull(values, name + " must not be null")));
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public TablePath getDataSetPath() {
        return dataSetPath;
    }

    public CatalogTable getCatalogTable() {
        return catalogTable;
    }

    public List<SourceTaskPlan<?>> getSourceTaskPlans() {
        return sourceTaskPlans;
    }

    public List<SinkTaskPlan> getSinkTaskPlans() {
        return sinkTaskPlans;
    }
}
