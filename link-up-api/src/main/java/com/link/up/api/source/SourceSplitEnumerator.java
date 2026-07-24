package com.link.up.api.source;

import java.util.List;

/**
 * 离线 Source 分片生成器。
 */
public interface SourceSplitEnumerator<SplitT extends SourceSplit>
        extends AutoCloseable {

    /**
     * 生成当前 Source 的全部数据分片。
     */
    List<SplitT> enumerateSplits() throws Exception;

    @Override
    default void close() throws Exception {
        // 默认没有需要关闭的资源。
    }
}