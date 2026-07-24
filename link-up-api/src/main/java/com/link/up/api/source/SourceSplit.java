package com.link.up.api.source;

import java.io.Serializable;

/**
 * Source 数据分片。
 * <p>
 * 离线任务可以把一张表、一个文件或者一个查询范围拆分成多个分片，
 * 然后由多个 Reader 并行读取。
 */
public interface SourceSplit extends Serializable {

    /**
     * 当前分片的唯一标识。
     */
    String splitId();

    /**
     * 当前分片所属的数据集。
     * <p>
     * JDBC Source 可以返回表名；
     * File Source 可以返回文件组；
     * HTTP Source 可以返回接口资源标识。
     * <p>
     * 数据集信息放在批次级别，不放进每一条 FluxRow。
     */
    default String dataSetId() {
        return "default";
    }
}