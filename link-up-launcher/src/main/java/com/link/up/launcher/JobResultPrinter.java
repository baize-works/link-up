package com.link.up.launcher;

import com.link.up.framework.execution.TaskType;
import com.link.up.framework.job.CommitSummary;
import com.link.up.framework.job.JobResult;
import com.link.up.framework.job.PipelineResult;
import com.link.up.framework.metrics.ChannelMetrics;
import com.link.up.framework.metrics.JobMetrics;
import com.link.up.framework.metrics.TaskMetrics;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToLongFunction;

/**
 * Concise default result output; callers may request task/channel detail explicitly.
 */
public final class JobResultPrinter {
    private JobResultPrinter() {
    }

    public static void print(JobResult r) {
        print(r, System.out, DetailLevel.SUMMARY);
    }

    static void print(JobResult r, PrintStream out) {
        print(r, out, DetailLevel.SUMMARY);
    }

    public static void print(JobResult r, PrintStream out, DetailLevel detail) {
        JobMetrics m = r.getMetrics();
        CommitSummary c = r.getCommitSummary();
        out.println(r.isSuccess() ? "Flux 作业执行完成：" : "Flux 作业执行失败：");
        line(out, "作业名称", r.getJobName());
        line(out, "执行状态", r.getStatus());
        line(out, "执行耗时（毫秒）", r.getDurationMillis());
        line(out, "Pipeline 总数", r.getPipelineResults().size());
        line(out, "读取批次数", m.getSourceBatchCount());
        line(out, "读取记录数", m.getSourceRecordCount());
        line(out, "源端平均读取速率（条/秒）", rate(m.getSourceAverageQps()));
        line(out, "接收批次数", m.getSinkReceivedBatchCount());
        line(out, "尝试写入记录数", m.getSinkAttemptedRecordCount());
        line(out, "确认写入成功记录数", m.getSinkRecordCount());
        line(out, "写入状态未知记录数", m.getUnknownStateRecordCount());
        line(out, "成功提交数据记录数", c.getSuccessfullyCommittedRecordCount());
        line(out, "有效数据提交 Task 数", c.getDataCommittedTaskCount());
        line(out, "空事务提交 Task 数", c.getEmptyCommittedTaskCount());
        line(out, "失败或未提交 Task 数", c.getFailedOrUncommittedTaskCount());
        line(out, "是否存在部分 Task 提交", c.isPartialTaskCommit());
        line(out, "是否存在部分数据提交", c.isPartialDataCommit());
        if (c.isPartialDataCommit()) line(out, "提交风险", c.getWarning());
        line(out, "安全重跑建议", c.getRetryAdvice());
        for (PipelineResult p : r.getPipelineResults())
            if (p.getFailure() != null) {
                Throwable root = root(p.getFailure());
                out.println("  失败摘要：Pipeline=" + p.getPipelineId() + "，表=" + p.getDataSetId() + "，异常=" + root.getClass().getSimpleName() + "，根因=" + String.valueOf(root.getMessage()));
            }
        if (detail == DetailLevel.PIPELINE
                || detail == DetailLevel.FULL) {

            printPipelines(
                    r,
                    m,
                    out,
                    detail == DetailLevel.FULL);
        }

        if (detail == DetailLevel.TASK) {
            tasks(m, out);
        }

        if (detail == DetailLevel.CHANNEL
                || detail == DetailLevel.FULL) {

            channels(m, out);
        }
    }

    /**
     * 按 Pipeline 展示 Source、Sink 和对应 Task 指标。
     */
    private static void printPipelines(
            JobResult result,
            JobMetrics metrics,
            PrintStream out,
            boolean printTaskDetail) {

        List<PipelineResult> pipelines =
                new ArrayList<PipelineResult>(
                        result.getPipelineResults());

        pipelines.sort(
                Comparator.comparing(
                        PipelineResult::getPipelineId));

        out.println(
                "  Pipeline 明细（"
                        + pipelines.size()
                        + "）：");

        int index = 1;

        for (PipelineResult pipeline : pipelines) {

            List<TaskMetrics> sourceTasks =
                    findTasks(
                            metrics,
                            pipeline.getPipelineId(),
                            TaskType.SOURCE);

            List<TaskMetrics> sinkTasks =
                    findTasks(
                            metrics,
                            pipeline.getPipelineId(),
                            TaskType.SINK);

            out.println(
                    "    ["
                            + index++
                            + "] "
                            + pipeline.getPipelineId()
                            + "：");

            pipelineLine(
                    out,
                    "状态",
                    pipeline.getStatus());

            pipelineLine(
                    out,
                    "数据集",
                    pipeline.getDataSetId());

            printSource(
                    pipeline,
                    sourceTasks,
                    out,
                    printTaskDetail);

            printSink(
                    pipeline,
                    sinkTasks,
                    out,
                    printTaskDetail);

            if (pipeline.getFailure() != null) {

                Throwable root =
                        root(
                                pipeline.getFailure());

                pipelineLine(
                        out,
                        "失败原因",
                        root.getClass().getSimpleName()
                                + ": "
                                + String.valueOf(
                                root.getMessage()));
            }
        }
    }

    private static void printSource(
            PipelineResult pipeline,
            List<TaskMetrics> tasks,
            PrintStream out,
            boolean printTaskDetail) {

        out.println("      Source：");

        detailLine(
                out,
                "Connector",
                pipeline.getSourceIdentifier());

        detailLine(
                out,
                "源表",
                pipeline.getSourceTable());

        detailLine(
                out,
                "Task 数",
                taskCount(
                        pipeline.getSourceTaskCount(),
                        tasks));

        detailLine(
                out,
                "读取批次数",
                sum(
                        tasks,
                        new ToLongFunction<TaskMetrics>() {
                            @Override
                            public long applyAsLong(
                                    TaskMetrics value) {

                                return value.getBatchCount();
                            }
                        }));

        detailLine(
                out,
                "读取记录数",
                sum(
                        tasks,
                        new ToLongFunction<TaskMetrics>() {
                            @Override
                            public long applyAsLong(
                                    TaskMetrics value) {

                                return value
                                        .getSourceReadRecordCount();
                            }
                        }));

        detailLine(
                out,
                "完成分片数",
                sum(
                        tasks,
                        new ToLongFunction<TaskMetrics>() {
                            @Override
                            public long applyAsLong(
                                    TaskMetrics value) {

                                return value
                                        .getCompletedSplitCount();
                            }
                        }));

        detailLine(
                out,
                "平均读取速率（条/秒）",
                rate(
                        averageQps(
                                tasks,
                                TaskType.SOURCE)));

        if (printTaskDetail) {
            printSourceTasks(
                    tasks,
                    out);
        }
    }

    private static void printSink(
            PipelineResult pipeline,
            List<TaskMetrics> tasks,
            PrintStream out,
            boolean printTaskDetail) {

        CommitSummary summary =
                pipeline.getCommitSummary();

        out.println("      Sink：");

        detailLine(
                out,
                "Connector",
                pipeline.getSinkIdentifier());

        detailLine(
                out,
                "目标表",
                pipeline.getSinkTable());

        detailLine(
                out,
                "Task 数",
                taskCount(
                        pipeline.getSinkTaskCount(),
                        tasks));

        detailLine(
                out,
                "接收批次数",
                sum(
                        tasks,
                        new ToLongFunction<TaskMetrics>() {
                            @Override
                            public long applyAsLong(
                                    TaskMetrics value) {

                                return value
                                        .getReceivedBatchCount();
                            }
                        }));

        detailLine(
                out,
                "尝试写入记录数",
                sum(
                        tasks,
                        new ToLongFunction<TaskMetrics>() {
                            @Override
                            public long applyAsLong(
                                    TaskMetrics value) {

                                return value
                                        .getAttemptedRecordCount();
                            }
                        }));

        detailLine(
                out,
                "确认写入成功记录数",
                sum(
                        tasks,
                        new ToLongFunction<TaskMetrics>() {
                            @Override
                            public long applyAsLong(
                                    TaskMetrics value) {

                                return value
                                        .getSinkWriteSuccessRecordCount();
                            }
                        }));

        detailLine(
                out,
                "写入状态未知记录数",
                sum(
                        tasks,
                        new ToLongFunction<TaskMetrics>() {
                            @Override
                            public long applyAsLong(
                                    TaskMetrics value) {

                                return value
                                        .getUnknownStateRecordCount();
                            }
                        }));

        detailLine(
                out,
                "成功提交记录数",
                summary.getSuccessfullyCommittedRecordCount());

        detailLine(
                out,
                "有效提交 Task 数",
                summary.getDataCommittedTaskCount());

        detailLine(
                out,
                "空事务提交 Task 数",
                summary.getEmptyCommittedTaskCount());

        detailLine(
                out,
                "失败或未提交 Task 数",
                summary.getFailedOrUncommittedTaskCount());

        detailLine(
                out,
                "平均写入速率（条/秒）",
                rate(
                        averageQps(
                                tasks,
                                TaskType.SINK)));

        if (printTaskDetail) {
            printSinkTasks(
                    tasks,
                    out);
        }
    }

    private static void printSourceTasks(
            List<TaskMetrics> tasks,
            PrintStream out) {

        for (TaskMetrics task : tasks) {

            out.println(
                    "        Task="
                            + task.getTaskId()
                            + "，状态="
                            + task.getState()
                            + "，批次数="
                            + task.getBatchCount()
                            + "，读取记录数="
                            + task.getSourceReadRecordCount()
                            + "，完成分片数="
                            + task.getCompletedSplitCount()
                            + "，执行耗时（毫秒）="
                            + task.getDurationMillis()
                            + position(task));
        }
    }

    private static void printSinkTasks(
            List<TaskMetrics> tasks,
            PrintStream out) {

        for (TaskMetrics task : tasks) {

            out.println(
                    "        Task="
                            + task.getTaskId()
                            + "，状态="
                            + task.getState()
                            + "，接收批次数="
                            + task.getReceivedBatchCount()
                            + "，尝试写入记录数="
                            + task.getAttemptedRecordCount()
                            + "，成功写入记录数="
                            + task.getSinkWriteSuccessRecordCount()
                            + "，状态未知记录数="
                            + task.getUnknownStateRecordCount()
                            + "，执行耗时（毫秒）="
                            + task.getDurationMillis()
                            + position(task));
        }
    }

    private static List<TaskMetrics> findTasks(
            JobMetrics metrics,
            String pipelineId,
            TaskType taskType) {

        List<TaskMetrics> result =
                new ArrayList<TaskMetrics>();

        for (TaskMetrics task
                : metrics.getTaskMetrics().values()) {

            if (pipelineId.equals(
                    task.getTaskId()
                            .getPipelineId())
                    && taskType
                    == task.getTaskId()
                    .getTaskType()) {

                result.add(task);
            }
        }

        result.sort(
                new Comparator<TaskMetrics>() {
                    @Override
                    public int compare(
                            TaskMetrics left,
                            TaskMetrics right) {

                        return Integer.compare(
                                left.getTaskId()
                                        .getSubtaskIndex(),
                                right.getTaskId()
                                        .getSubtaskIndex());
                    }
                });

        return result;
    }

    private static long sum(
            List<TaskMetrics> tasks,
            ToLongFunction<TaskMetrics> getter) {

        long total = 0L;

        for (TaskMetrics task : tasks) {
            total += getter.applyAsLong(task);
        }

        return total;
    }

    private static double averageQps(
            List<TaskMetrics> tasks,
            TaskType type) {

        long records = 0L;
        long start = Long.MAX_VALUE;
        long end = 0L;

        for (TaskMetrics task : tasks) {

            if (type == TaskType.SOURCE) {
                records +=
                        task.getSourceReadRecordCount();
            } else {
                records +=
                        task.getSinkWriteSuccessRecordCount();
            }

            if (task.getStartTimeMillis() > 0L) {

                start =
                        Math.min(
                                start,
                                task.getStartTimeMillis());

                long taskEnd =
                        task.getEndTimeMillis() > 0L
                                ? task.getEndTimeMillis()
                                : System.currentTimeMillis();

                end =
                        Math.max(
                                end,
                                taskEnd);
            }
        }

        if (start == Long.MAX_VALUE
                || end <= start) {

            return 0D;
        }

        return records
                * 1000D
                / (end - start);
    }

    private static int taskCount(
            int plannedTaskCount,
            List<TaskMetrics> tasks) {

        return plannedTaskCount > 0
                ? plannedTaskCount
                : tasks.size();
    }

    private static String position(
            TaskMetrics task) {

        if (task.getCurrentTable() == null
                && task.getCurrentSplit() == null) {

            return "";
        }

        return "，表="
                + valueOrDash(
                task.getCurrentTable())
                + "，分片="
                + valueOrDash(
                task.getCurrentSplit());
    }

    private static String valueOrDash(
            String value) {

        return value == null
                || value.trim().isEmpty()
                ? "-"
                : value;
    }

    private static void pipelineLine(
            PrintStream out,
            String key,
            Object value) {

        out.println(
                "      "
                        + key
                        + "："
                        + value);
    }

    private static void detailLine(
            PrintStream out,
            String key,
            Object value) {

        out.println(
                "        "
                        + key
                        + "："
                        + value);
    }

    private static Throwable root(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t;
    }

    private static void tasks(JobMetrics m, PrintStream o) {
        for (TaskMetrics t : m.getTaskMetrics().values())
            o.println("  Task=" + t.getTaskId() + "，状态=" + t.getState() + "，received=" + t.getReceivedBatchCount() + "，attempted=" + t.getAttemptedRecordCount() + "，successful=" + t.getSinkWriteSuccessRecordCount() + "，unknown=" + t.getUnknownStateRecordCount());
    }

    private static void channels(JobMetrics m, PrintStream o) {
        for (ChannelMetrics c : m.getChannelMetrics())
            o.println("  Channel=" + c.getChannelId() + "，入队批次数=" + c.getEnqueuedCount() + "，出队批次数=" + c.getDequeuedCount() + "，生产者反压占比=" + ratio(c.getProducerBackpressureRatio()) + "，消费者等待占比=" + ratio(c.getConsumerIdleRatio()) + "，限流等待占比=" + ratio(c.getRateLimitedRatio()));
    }

    private static String rate(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String ratio(double v) {
        return String.format(Locale.ROOT, "%.2f%%", v * 100D);
    }

    private static void line(PrintStream o, String k, Object v) {
        o.println("  " + k + "：" + v);
    }

    public enum DetailLevel {
        SUMMARY,
        PIPELINE,
        TASK,
        CHANNEL,
        FULL
    }
}
