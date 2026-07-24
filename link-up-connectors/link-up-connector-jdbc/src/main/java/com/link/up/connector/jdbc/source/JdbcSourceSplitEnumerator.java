package com.link.up.connector.jdbc.source;


import com.link.up.api.source.SourceSplitEnumerator;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.connector.jdbc.config.JdbcSourceConfig;
import com.link.up.connector.jdbc.core.split.JdbcSplitPlanner;

import java.util.List;
import java.util.Map;

/**
 * JDBC 静态分片生成器。
 * <p>
 * 任务启动时一次性计算所有分片，
 * 不再与 Reader 动态通信，也不保存 Checkpoint 状态。
 */
public final class JdbcSourceSplitEnumerator
        implements SourceSplitEnumerator<JdbcSourceSplit> {

    private final JdbcSourceConfig config;

    private final Map<TablePath, JdbcSourceTable> sourceTables;

    private final int parallelism;

    public JdbcSourceSplitEnumerator(
            JdbcSourceConfig config,
            Map<TablePath, JdbcSourceTable> sourceTables,
            int parallelism) {

        this.config = config;
        this.sourceTables = sourceTables;
        this.parallelism = parallelism;
    }

    @Override
    public List<JdbcSourceSplit> enumerateSplits()
            throws Exception {

        /*
         * SeaTunnel 原 JdbcSourceSplitEnumerator 中：
         *
         * 1. 查询最大值和最小值；
         * 2. 根据 partition_num 计算区间；
         * 3. 生成 splitQuery；
         * 4. 处理多表；
         *
         * 这些纯分片计算逻辑可以迁移到 JdbcSplitPlanner。
         */
        return JdbcSplitPlanner.plan(
                config,
                sourceTables,
                parallelism);
    }
}
