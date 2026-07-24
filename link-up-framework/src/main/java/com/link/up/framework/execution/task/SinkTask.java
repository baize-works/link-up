package com.link.up.framework.execution.task;

import com.link.up.api.dirtydata.DirtyDataContext;
import com.link.up.api.dirtydata.DirtyDataSummary;
import com.link.up.api.sink.CommitScope;
import com.link.up.api.sink.DirtyDataAwareSinkWriter;
import com.link.up.api.sink.SinkWriter;
import com.link.up.api.table.type.FluxRow;
import com.link.up.framework.channel.InputGate;
import com.link.up.framework.channel.RecordEnvelope;
import com.link.up.framework.execution.TaskContext;
import com.link.up.framework.execution.TaskId;
import com.link.up.framework.planner.SinkTaskPlan;

import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Sink 数据写入任务。
 */
public final class SinkTask
        implements ExecutionTask {

    private final SinkTaskPlan plan;

    private final InputGate<RecordEnvelope<FluxRow>>
            inputGate;

    private volatile boolean committed;
    private volatile CommitScope commitScope = CommitScope.TASK_LOCAL;
    private volatile DirtyDataSummary dirtyDataSummary = DirtyDataSummary.empty();
    private volatile String retryAdvice = "This sink commits per task; verify already committed targets before retrying.";

    public SinkTask(
            SinkTaskPlan plan,
            InputGate<RecordEnvelope<FluxRow>>
                    inputGate) {

        this.plan =
                Objects.requireNonNull(
                        plan,
                        "plan must not be null");

        this.inputGate =
                Objects.requireNonNull(
                        inputGate,
                        "inputGate must not be null");
    }

    private static RuntimeException propagate(
            Throwable throwable)
            throws Exception {

        if (throwable instanceof Exception) {
            throw (Exception) throwable;
        }

        if (throwable instanceof Error) {
            throw (Error) throwable;
        }

        return new RuntimeException(
                throwable);
    }

    @Override
    public TaskId getTaskId() {
        return plan.getTaskId();
    }

    @Override
    public void execute(
            TaskContext context)
            throws Exception {

        try (com.link.up.framework.classloading.ClassLoaderScope ignored =
                     com.link.up.framework.classloading.ClassLoaderScope.open(plan.getPreparedSink().getClassLoader())) {
            SinkWriter<FluxRow> writer = null;

            Throwable failure = null;

            try {
                writer =
                        plan.getPreparedSink()
                                .createWriter();

                if (writer instanceof DirtyDataAwareSinkWriter)
                    ((DirtyDataAwareSinkWriter) writer).configureDirtyData(new DirtyDataContext(plan.getTaskId().getStageName(), getTaskId().toString(), plan.getPreparedSink().getClass().getName(), null, null));
                writer.open();
                commitScope = writer.getCommitScope();
                retryAdvice = writer.getRetryAdvice();

                while (true) {
                    /*
                     * 不能因为其他 Task 触发取消，就正常退出并提交部分数据。
                     */
                    if (context
                            .getCancellationToken()
                            .isCancelled()) {

                        throw new CancellationException(
                                "SinkTask was cancelled: "
                                        + getTaskId());
                    }

                    RecordEnvelope<FluxRow> envelope =
                            inputGate.read();

                    /*
                     * 所有 SourceTask 均已结束。
                     */
                    if (envelope == null) {
                        break;
                    }

                    long recordCount = envelope.getBatch().getRecords().size();
                    context.getMetrics().incrementReceivedBatchCount();
                    context.getMetrics().addAttemptedRecords(recordCount);
                    context.getMetrics().setCurrentPosition(envelope.getBatch().getDataSetId(), envelope.getBatch().getSplitId());
                    long sqlStart = System.nanoTime();
                    try {
                        writer.write(envelope.getBatch(), envelope.getCatalogTable());
                        context.getMetrics().incrementSuccessfulBatchCount();
                        context.getMetrics().incrementBatchCount();
                        context.getMetrics().addSinkWriteSuccessRecords(recordCount);
                    } catch (Throwable writeFailure) {
                        context.getMetrics().incrementFailedBatchCount();
                        // Connector-specific failures may refine this later; JDBC outcome is otherwise unknown.
                        context.getMetrics().addUnknownStateRecords(recordCount);
                        throw writeFailure;
                    } finally {
                        context.getMetrics().addSqlExecutionNanos(System.nanoTime() - sqlStart);
                    }
                }

                /*
                 * 在读取结束与提交之间再次检查取消状态，
                 * 防止最后一批处理完成时其他任务刚好失败。
                 */
                if (context
                        .getCancellationToken()
                        .isCancelled()) {

                    throw new CancellationException(
                            "SinkTask was cancelled before commit: "
                                    + getTaskId());
                }

                long commitStart = System.nanoTime();
                writer.prepareCommit();
                writer.commit();
                context.getMetrics().addDatabaseCommitNanos(System.nanoTime() - commitStart);

                committed = true;

            } catch (Throwable throwable) {
                failure = throwable;

                if (writer != null) {
                    try {
                        writer.abort();
                    } catch (Throwable rollbackFailure) {
                        throwable.addSuppressed(
                                rollbackFailure);
                    }
                }

                throw propagate(throwable);

            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                        if (writer instanceof DirtyDataAwareSinkWriter)
                            dirtyDataSummary = ((DirtyDataAwareSinkWriter) writer).getDirtyDataSummary();
                    } catch (Throwable closeFailure) {
                        if (failure != null) {
                            failure.addSuppressed(
                                    closeFailure);

                        } else if (!committed) {
                            /*
                             * 事务尚未提交时，关闭异常需要让任务失败。
                             */
                            throw propagate(
                                    closeFailure);
                        }

                        /*
                         * 数据已经提交后，单纯连接关闭异常不能再把任务标记为失败。
                         * 否则上层重试可能造成数据重复。
                         */
                    }
                }
            }
        }
    }

    public DirtyDataSummary getDirtyDataSummary() {
        return dirtyDataSummary;
    }

    public boolean isCommitted() {
        return committed;
    }

    public CommitScope getCommitScope() {
        return commitScope;
    }

    public String getRetryAdvice() {
        return retryAdvice;
    }
}
