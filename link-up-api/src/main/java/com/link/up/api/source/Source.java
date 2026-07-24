package com.link.up.api.source;

import com.link.up.api.table.catalog.CatalogTable;
import com.link.up.api.table.catalog.TablePath;
import com.link.up.api.table.type.FluxRow;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 离线数据源。
 *
 * @param <SplitT> Source 分片类型
 */
public interface Source<SplitT extends SourceSplit>
        extends Serializable {

    /**
     * 根据表结构生成读取分片。
     */
    List<SplitT> createSplits(
            Map<TablePath, CatalogTable> tables)
            throws Exception;

    /**
     * Creates the splits for an execution with the supplied reader parallelism.
     * <p>
     * Connectors that can use the parallelism while planning (for example to
     * cap JDBC range splits) should override this method. The default keeps
     * existing connectors source-compatible and lets the framework distribute
     * their returned splits among reader tasks.
     */
    default List<SplitT> createSplits(
            Map<TablePath, CatalogTable> tables,
            int parallelism)
            throws Exception {

        if (parallelism <= 0) {
            throw new IllegalArgumentException(
                    "parallelism must be greater than 0");
        }
        return createSplits(tables);
    }

    /**
     * Validates execution reader parallelism before source tasks are created.
     * Connectors may override this to reject unsupported execution modes.
     */
    default void validateParallelism(int parallelism) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException(
                    "parallelism must be greater than 0");
        }
    }

    /**
     * 创建 SourceReader。
     * <p>
     * 每个执行任务必须使用独立 Reader，
     * Reader 不应在多个线程之间共享。
     */
    SourceReader<FluxRow, SplitT> createReader(
            Map<TablePath, CatalogTable> tables,
            int batchSize);
}
