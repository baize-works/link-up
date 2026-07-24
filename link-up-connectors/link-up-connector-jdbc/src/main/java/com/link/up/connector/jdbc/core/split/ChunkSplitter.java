package com.link.up.connector.jdbc.core.split;

import java.util.List;

/**
 * 将已知范围切分成互不重叠的分片。
 */
public interface ChunkSplitter<T> {

    /**
     * @param lowerBound 范围下界
     * @param upperBound 范围上界
     * @param chunkCount 期望分片数
     */
    List<Chunk<T>> split(T lowerBound, T upperBound, int chunkCount);
}