package com.link.up.framework.planner;

import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.framework.connector.PreparedJob;
import com.link.up.framework.connector.PreparedSink;
import com.link.up.framework.connector.PreparedSource;
import com.link.up.framework.execution.TaskId;
import com.link.up.framework.execution.split.LocalSplitQueue;
import com.link.up.framework.execution.split.SplitProvider;
import com.link.up.framework.job.ExecutionConfig;
import com.link.up.framework.job.SplitAssignmentMode;

import java.util.*;

/**
 * 按 SourceSplit.dataSetId 构建独立 Pipeline 的计划生成器。
 */
public final class JobPlanner {
    private final SplitAssigner splitAssigner;

    public JobPlanner() {
        this(new SplitAssigner());
    }

    public JobPlanner(SplitAssigner splitAssigner) {
        this.splitAssigner = Objects.requireNonNull(splitAssigner, "splitAssigner must not be null");
    }

    public ExecutionPlan plan(PreparedJob preparedJob) throws Exception {
        return createPlan(Objects.requireNonNull(preparedJob, "preparedJob must not be null"), preparedJob.getSource());
    }

    private <SplitT extends SourceSplit> ExecutionPlan createPlan(PreparedJob job, PreparedSource<SplitT> preparedSource) throws Exception {
        ExecutionConfig config = job.getExecutionConfig();
        List<SplitT> splits = preparedSource.getSource().createSplits(preparedSource.getTables(), config.getSourceParallelism());
        if (splits == null) throw new IllegalStateException("Source returned null splits");
        Map<String, List<SplitT>> byDataSet = new LinkedHashMap<String, List<SplitT>>();
        for (SplitT split : splits) {
            String id = split.dataSetId();
            if (id == null || id.trim().isEmpty())
                throw new IllegalStateException("SourceSplit dataSetId must not be blank");
            List<SplitT> group = byDataSet.get(id);
            if (group == null) {
                group = new ArrayList<SplitT>();
                byDataSet.put(id, group);
            }
            group.add(split);
        }
        List<PipelinePlan> pipelines = new ArrayList<PipelinePlan>();
        for (Map.Entry<String, List<SplitT>> entry : byDataSet.entrySet()) {
            String dataSetId = entry.getKey();
            TablePath path = TablePath.parse(dataSetId);
            CatalogTable table = preparedSource.getTables().get(path);
            if (table == null) throw new IllegalStateException("No catalog table for data set: " + dataSetId);
            List<List<SplitT>> assignments = splitAssigner.assign(entry.getValue(), config.getSourceParallelism());
            SplitProvider<SplitT> provider = config.getSplitAssignmentMode() == SplitAssignmentMode.DYNAMIC ? new LocalSplitQueue<SplitT>(entry.getValue()) : null;
            String pipelineId = "pipeline-" + dataSetId;
            List<SourceTaskPlan<?>> sources = new ArrayList<SourceTaskPlan<?>>();
            for (int i = 0; i < assignments.size(); i++)
                sources.add(new SourceTaskPlan<SplitT>(new TaskId(pipelineId, com.link.up.framework.execution.TaskType.SOURCE, i, assignments.size()), preparedSource, assignments.get(i), config.getBatchSize(), provider));
            List<PreparedSink> sinks = job.getSinks(dataSetId);
            int sinkCount = Math.min(Math.min(config.getSinkParallelism(), sinks.size()), Math.max(1, entry.getValue().size()));
            List<SinkTaskPlan> sinkPlans = new ArrayList<SinkTaskPlan>();
            for (int i = 0; i < sinkCount; i++)
                sinkPlans.add(new SinkTaskPlan(new TaskId(pipelineId, com.link.up.framework.execution.TaskType.SINK, i, sinkCount), sinks.get(i)));
            pipelines.add(new PipelinePlan(pipelineId, dataSetId, table, sources, sinkPlans));
        }
        return new ExecutionPlan(job.getJobName(), config, pipelines);
    }
}
