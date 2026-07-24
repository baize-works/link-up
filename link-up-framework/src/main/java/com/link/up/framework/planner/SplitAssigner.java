package com.link.up.framework.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Split 分配器。
 *
 * <p>当前使用 Round-Robin 将 Split 分配给 SourceTask。
 */
public final class SplitAssigner {

    public <T> List<List<T>> assign(
            List<T> splits,
            int parallelism) {

        Objects.requireNonNull(
                splits,
                "splits must not be null");

        if (parallelism <= 0) {
            throw new IllegalArgumentException(
                    "parallelism must be greater than 0");
        }

        if (splits.isEmpty()) {
            return Collections.emptyList();
        }

        int taskCount =
                Math.min(
                        splits.size(),
                        parallelism);

        List<List<T>> assignments =
                new ArrayList<List<T>>(taskCount);

        for (int i = 0; i < taskCount; i++) {
            assignments.add(
                    new ArrayList<T>());
        }

        for (int i = 0; i < splits.size(); i++) {
            assignments.get(
                    i % taskCount)
                    .add(splits.get(i));
        }

        List<List<T>> immutable =
                new ArrayList<List<T>>(taskCount);

        for (List<T> assignment : assignments) {
            immutable.add(
                    Collections.unmodifiableList(
                            new ArrayList<T>(
                                    assignment)));
        }

        return Collections.unmodifiableList(
                immutable);
    }
}