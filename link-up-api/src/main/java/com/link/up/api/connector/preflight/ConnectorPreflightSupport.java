package com.link.up.api.connector.preflight;

import com.link.up.api.configuration.ReadonlyConfig;

/**
 * Connector 可选的只读连通性预检能力。
 *
 * <p>实现必须只验证配置和外部系统连接，不得创建任务、写入业务数据或修改外部结构。
 */
public interface ConnectorPreflightSupport {

    /**
     * 从当前 Worker 进程视角验证 Connector 配置和外部系统可达性。
     *
     * @param options Connector 运行配置
     * @param classLoader Connector 所属类加载器
     */
    void preflight(
            ReadonlyConfig options,
            ClassLoader classLoader)
            throws Exception;
}
