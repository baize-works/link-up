package com.link.up.api.table.factory;

import com.link.up.api.factory.SourceFactory;
import com.link.up.api.source.Source;
import com.link.up.api.source.SourceFactoryContext;
import com.link.up.api.source.SourceSplit;
import com.link.up.api.table.catalog.CatalogTable;

import java.util.List;

/**
 * 支持表结构发现的 Source 工厂。
 *
 * @param <SplitT> Source 分片类型
 */
public interface TableSourceFactory<SplitT extends SourceSplit>
        extends SourceFactory {

    /**
     * 创建 Source。
     */
    Source<SplitT> createSource(
            SourceFactoryContext context)
            throws Exception;

    /**
     * 发现 Source 输出表结构。
     */
    List<CatalogTable> discoverTableSchemas(
            SourceFactoryContext context)
            throws Exception;
}