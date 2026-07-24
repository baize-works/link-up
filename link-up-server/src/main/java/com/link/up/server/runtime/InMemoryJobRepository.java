package com.link.up.server.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * 有界内存历史记录仓库。
 */
public final class InMemoryJobRepository
        implements JobRepository {

    private final int historyLimit;

    private final ConcurrentMap<String, JobSnapshot>
            snapshots =
            new ConcurrentHashMap<String, JobSnapshot>();

    private final ConcurrentLinkedDeque<String>
            orderedJobIds =
            new ConcurrentLinkedDeque<String>();

    public InMemoryJobRepository(int historyLimit) {
        if (historyLimit <= 0) {
            throw new IllegalArgumentException(
                    "historyLimit must be greater than 0");
        }

        this.historyLimit = historyLimit;
    }

    public void save(JobSnapshot snapshot) {
        JobSnapshot previous =
                snapshots.put(
                        snapshot.getJobId(),
                        snapshot);

        if (previous == null) {
            orderedJobIds.addFirst(
                    snapshot.getJobId());
        }

        trim();
    }

    public JobSnapshot get(String jobId) {
        return snapshots.get(jobId);
    }

    public List<JobSnapshot> list() {
        List<JobSnapshot> result =
                new ArrayList<JobSnapshot>();

        for (String jobId : orderedJobIds) {
            JobSnapshot snapshot =
                    snapshots.get(jobId);

            if (snapshot != null) {
                result.add(snapshot);
            }
        }

        Collections.sort(
                result,
                new Comparator<JobSnapshot>() {
                    public int compare(
                            JobSnapshot left,
                            JobSnapshot right) {

                        return Long.compare(
                                right.getCreateTimeMillis(),
                                left.getCreateTimeMillis());
                    }
                });

        return result;
    }

    private void trim() {
        while (snapshots.size() > historyLimit) {
            String oldestJobId =
                    orderedJobIds.pollLast();

            if (oldestJobId == null) {
                return;
            }

            snapshots.remove(oldestJobId);
        }
    }
}