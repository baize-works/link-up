package com.link.up.framework.execution;

import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.type.FluxRow;
import com.link.up.framework.channel.*;
import com.link.up.framework.connector.PreparedSink;
import com.link.up.framework.execution.split.SplitProvider;
import com.link.up.framework.execution.task.ExecutionTask;
import com.link.up.framework.execution.task.SinkTask;
import com.link.up.framework.execution.task.SourceTask;
import com.link.up.framework.job.CommitSummary;
import com.link.up.framework.job.PipelineResult;
import com.link.up.framework.job.PipelineStatus;
import com.link.up.framework.metrics.JobMetrics;
import com.link.up.framework.planner.PipelinePlan;
import com.link.up.framework.planner.SinkTaskPlan;
import com.link.up.framework.planner.SourceTaskPlan;
import com.link.up.framework.routing.Partitioner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Executes one Pipeline with channels and writers owned exclusively by that Pipeline.
 */
final class PipelineExecution {
    private final PipelinePlan plan;
    private final com.link.up.framework.job.ExecutionConfig config;
    private final CancellationToken token;
    private final JobMetrics metrics;
    private final ClassLoader loader;
    private final String jobName;
    private final long runId;

    PipelineExecution(PipelinePlan plan, com.link.up.framework.job.ExecutionConfig config, CancellationToken token, JobMetrics metrics, ClassLoader loader, String jobName, long runId) {
        this.plan = plan;
        this.config = config;
        this.token = token;
        this.metrics = metrics;
        this.loader = loader;
        this.jobName = jobName;
        this.runId = runId;
    }

    private static void fail(List<DataChannel<RecordEnvelope<FluxRow>>> channels, Throwable cause) {
        for (DataChannel<RecordEnvelope<FluxRow>> c : channels)
            c.fail(cause == null ? new IllegalStateException("Pipeline cancelled") : cause);
    }

    PipelineResult execute() {
        List<DataChannel<RecordEnvelope<FluxRow>>> channels = new ArrayList<DataChannel<RecordEnvelope<FluxRow>>>();
        try {
            for (int i = 0; i < plan.getSinkTaskPlans().size(); i++) {
                LocalDataChannel<RecordEnvelope<FluxRow>> c = new LocalDataChannel<RecordEnvelope<FluxRow>>(plan.getPipelineId() + "-source-to-sink-" + i, config.getMaxBufferedBatches(), config.getMaxBufferedRecords(), config.getMaxBufferedBytes(), config.getMaxRecordsPerSecond(), config.getMaxBytesPerSecond(), plan.getSourceTaskPlans().size());
                channels.add(c);
                metrics.registerChannel(c.getMetrics());
            }
            List<ExecutionTask> sinks = new ArrayList<ExecutionTask>();
            for (int i = 0; i < channels.size(); i++)
                sinks.add(new SinkTask(plan.getSinkTaskPlans().get(i), new InputGate<RecordEnvelope<FluxRow>>(channels.get(i).openReader())));
            List<ExecutionTask> sources = new ArrayList<ExecutionTask>();
            for (SourceTaskPlan<?> source : plan.getSourceTaskPlans()) {
                metrics.registerSplitProvider(source.getSplitProvider());
                sources.add(createSource(source, channels));
            }
            try (TaskExecutor executor = new TaskExecutor(sinks.size() + sources.size(), "link-up-" + plan.getPipelineId(), jobName, runId)) {
                ExecutionCoordinator outcomeCoordinator = new ExecutionCoordinator(executor, token, metrics, loader, new Runnable() {
                    public void run() {
                        fail(channels, token.getCause());
                        cancelProviders();
                    }
                });
                ExecutionCoordinator.ExecutionOutcome outcome = outcomeCoordinator.execute(sinks, sources);
                if (outcome.getFailure() != null) {
                    return createPipelineResult(
                            token.isCancelled()
                                    ? PipelineStatus.CANCELED
                                    : PipelineStatus.FAILED,
                            outcome.getCommitSummary(),
                            outcome.getFailure());
                }

                return createPipelineResult(
                        PipelineStatus.SUCCEEDED,
                        outcome.getCommitSummary(),
                        null);
            }
        } finally {
            for (DataChannel<RecordEnvelope<FluxRow>> c : channels)
                try {
                    c.close();
                } catch (Throwable ignored) {
                }
        }
    }

    /**
     * 创建包含 Source、Sink 和目标表信息的 Pipeline 结果。
     */
    private PipelineResult createPipelineResult(
            PipelineStatus status,
            CommitSummary commitSummary,
            Throwable failure) {

        SourceTaskPlan<?> sourceTaskPlan =
                plan.getSourceTaskPlans()
                        .get(0);

        SinkTaskPlan sinkTaskPlan =
                plan.getSinkTaskPlans()
                        .get(0);

        String sourceIdentifier =
                sourceTaskPlan
                        .getPreparedSource()
                        .getFactoryIdentifier();

        PreparedSink preparedSink =
                sinkTaskPlan.getPreparedSink();

        String sinkIdentifier =
                preparedSink.getFactoryIdentifier();

        CatalogTable targetTable =
                preparedSink
                        .getMetadata()
                        .getTargetTable(
                                plan.getDataSetPath());

        String sinkTablePath = "-";

        if (targetTable != null
                && targetTable.getTablePath() != null) {

            sinkTablePath =
                    targetTable
                            .getTablePath()
                            .toString();
        }

        return new PipelineResult(
                plan.getPipelineId(),
                plan.getDataSetId(),
                sourceIdentifier,
                plan.getDataSetPath().toString(),
                plan.getSourceTaskPlans().size(),
                sinkIdentifier,
                sinkTablePath,
                plan.getSinkTaskPlans().size(),
                status,
                commitSummary,
                failure);
    }

    private void cancelProviders() {
        Set<SplitProvider<?>> set = new HashSet<SplitProvider<?>>();
        for (SourceTaskPlan<?> p : plan.getSourceTaskPlans())
            if (p.getSplitProvider() != null) set.add(p.getSplitProvider());
        for (SplitProvider<?> p : set) p.cancel(token.getCause());
    }

    private ExecutionTask createSource(SourceTaskPlan<?> plan, List<DataChannel<RecordEnvelope<FluxRow>>> channels) {
        return typed(plan, channels);
    }

    private <S extends SourceSplit> ExecutionTask typed(SourceTaskPlan<S> source, List<DataChannel<RecordEnvelope<FluxRow>>> channels) {
        List<ChannelWriter<RecordEnvelope<FluxRow>>> writers = new ArrayList<ChannelWriter<RecordEnvelope<FluxRow>>>();
        for (DataChannel<RecordEnvelope<FluxRow>> c : channels) writers.add(c.openWriter());
        Partitioner<RecordEnvelope<FluxRow>> partitioner = new Partitioner<RecordEnvelope<FluxRow>>() {
            public int selectChannel(RecordEnvelope<FluxRow> v, int count) {
                return count == 1 ? 0 : Math.floorMod(v.getBatch().getSplitId().hashCode(), count);
            }
        };
        return new SourceTask<S>(source, new OutputGate<RecordEnvelope<FluxRow>>(writers, partitioner));
    }

    static final class PipelineFailure extends RuntimeException {
        final CommitSummary summary;

        PipelineFailure(Throwable cause, CommitSummary summary) {
            super(cause);
            this.summary = summary;
        }
    }
}
