package com.link.up.server.service;

import com.link.up.server.dto.JobLogEntry;
import com.link.up.server.dto.JobLogPageResponse;
import com.link.up.server.runtime.JobExecutionMetadata;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the routed per-job Log4j file without exposing arbitrary file paths. */
final class JobLogReader {

    private static final int MAX_LIMIT = 1000;

    private static final Pattern EVENT_PATTERN =
            Pattern.compile(
                    "^(\\d{4}-\\d{2}-\\d{2} "
                            + "\\d{2}:\\d{2}:\\d{2}\\.\\d{3})"
                            + "\\s+(\\S+)"
                            + "\\s+\\[([^]]*)]"
                            + "\\s+\\[(?:run=)?([^]]*)]"
                            + "\\s+(\\S+)\\s+-\\s?(.*)$");

    JobLogPageResponse read(
            String jobId,
            JobExecutionMetadata metadata,
            boolean terminal,
            long cursor,
            int limit) {

        if (cursor < 0L) {
            throw new IllegalArgumentException(
                    "cursor must not be negative");
        }

        if (limit <= 0 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and " + MAX_LIMIT);
        }

        String externalExecutionId =
                metadata == null
                        ? null
                        : metadata.getExternalExecutionId();

        String runId =
                metadata == null
                        ? null
                        : metadata.getRunId();

        String jobLogFile =
                metadata == null
                        ? null
                        : metadata.getJobLogFile();

        if (!hasText(jobLogFile)) {
            return empty(
                    jobId,
                    externalExecutionId,
                    runId,
                    cursor,
                    terminal);
        }

        Path file = resolveJobLogFile(jobLogFile);
        File logFile = file.toFile();

        if (!logFile.isFile()) {
            return empty(
                    jobId,
                    externalExecutionId,
                    runId,
                    cursor,
                    terminal);
        }

        try (RandomAccessFile input =
                     new RandomAccessFile(logFile, "r")) {

            long length = input.length();
            long safeCursor = Math.min(cursor, length);
            input.seek(safeCursor);

            List<JobLogEntry> items =
                    new ArrayList<JobLogEntry>();

            MutableEntry current = null;
            long nextCursor = safeCursor;

            while (true) {
                long lineStart = input.getFilePointer();
                String line = readUtf8Line(input);

                if (line == null) {
                    break;
                }

                long lineEnd = input.getFilePointer();
                MutableEntry next = parse(lineStart, line);

                if (next != null) {
                    if (current != null) {
                        items.add(current.toEntry());

                        if (items.size() >= limit) {
                            nextCursor = lineStart;
                            current = null;
                            break;
                        }
                    }

                    current = next;
                } else if (current == null) {
                    current = MutableEntry.unparsed(
                            lineStart,
                            line);
                } else {
                    current.append(line);
                }

                nextCursor = lineEnd;
            }

            if (current != null && items.size() < limit) {
                items.add(current.toEntry());
                nextCursor = input.getFilePointer();
            }

            boolean completed =
                    terminal
                            && nextCursor >= input.length();

            return new JobLogPageResponse(
                    jobId,
                    externalExecutionId,
                    runId,
                    items,
                    nextCursor,
                    completed);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "读取 Link-Up Job 日志失败，jobId=" + jobId,
                    exception);
        }
    }

    private JobLogPageResponse empty(
            String jobId,
            String externalExecutionId,
            String runId,
            long cursor,
            boolean completed) {

        return new JobLogPageResponse(
                jobId,
                externalExecutionId,
                runId,
                Collections.<JobLogEntry>emptyList(),
                cursor,
                completed);
    }

    private MutableEntry parse(
            long sequence,
            String line) {

        Matcher matcher =
                EVENT_PATTERN.matcher(line);

        if (!matcher.matches()) {
            return null;
        }

        return new MutableEntry(
                sequence,
                timestamp(matcher.group(1)),
                matcher.group(2),
                matcher.group(3),
                matcher.group(5),
                matcher.group(6));
    }

    private Long timestamp(String value) {
        try {
            Date parsed =
                    new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss.SSS")
                            .parse(value);

            return parsed == null
                    ? null
                    : parsed.getTime();
        } catch (ParseException ignored) {
            return null;
        }
    }

    private String readUtf8Line(
            RandomAccessFile input)
            throws IOException {

        String line = input.readLine();

        if (line == null) {
            return null;
        }

        return new String(
                line.getBytes(
                        StandardCharsets.ISO_8859_1),
                StandardCharsets.UTF_8);
    }

    private Path resolveJobLogFile(
            String jobLogFile) {

        if (jobLogFile.contains("/")
                || jobLogFile.contains("\\")
                || !jobLogFile.equals(
                Paths.get(jobLogFile)
                        .getFileName()
                        .toString())) {

            throw new IllegalStateException(
                    "非法 Job 日志文件名");
        }

        Path directory = jobLogDirectory()
                .toAbsolutePath()
                .normalize();

        Path resolved = directory.resolve(jobLogFile)
                .normalize();

        if (!resolved.startsWith(directory)) {
            throw new IllegalStateException(
                    "Job 日志路径越界");
        }

        return resolved;
    }

    private Path jobLogDirectory() {
        String value =
                firstText(
                        System.getProperty(
                                "link.up.job.log.dir"),
                        System.getenv(
                                "LINK_UP_JOB_LOG_DIR"));

        if (hasText(value)) {
            return Paths.get(value.trim());
        }

        String logDirectory =
                firstText(
                        System.getProperty(
                                "link.up.log.dir"),
                        System.getenv(
                                "LINK_UP_LOG_DIR"));

        if (!hasText(logDirectory)) {
            logDirectory = "logs";
        }

        return Paths.get(
                logDirectory.trim(),
                "jobs");
    }

    private String firstText(
            String first,
            String second) {

        return hasText(first)
                ? first
                : second;
    }

    private static boolean hasText(
            String value) {

        return value != null
                && !value.trim().isEmpty();
    }

    private static final class MutableEntry {
        private final long sequence;
        private final Long timestampMillis;
        private final String level;
        private final String thread;
        private final String logger;
        private final StringBuilder message;

        private MutableEntry(
                long sequence,
                Long timestampMillis,
                String level,
                String thread,
                String logger,
                String message) {

            this.sequence = sequence;
            this.timestampMillis = timestampMillis;
            this.level = level;
            this.thread = thread;
            this.logger = logger;
            this.message =
                    new StringBuilder(
                            message == null ? "" : message);
        }

        private static MutableEntry unparsed(
                long sequence,
                String line) {

            return new MutableEntry(
                    sequence,
                    null,
                    "INFO",
                    null,
                    null,
                    line);
        }

        private void append(String line) {
            message.append('\n')
                    .append(line == null ? "" : line);
        }

        private JobLogEntry toEntry() {
            return new JobLogEntry(
                    sequence,
                    timestampMillis,
                    level,
                    thread,
                    logger,
                    message.toString());
        }
    }
}
