package com.link.up.api.table.factory;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.factory.Factory;
import com.link.up.api.table.catalog.Catalog;

/**
 * Catalog 插件工厂。
 * <p>
 * 每一种数据库连接器可以提供自己的 CatalogFactory，
 * 由 SPI 根据 factoryIdentifier 进行加载。
 */
public interface CatalogFactory extends Factory {

    /**
     * 创建 Catalog。
     *
     * @param catalogName Catalog 实例名称
     * @param options     Catalog 配置
     * @return Catalog 实例
     */
    Catalog createCatalog(
            String catalogName,
            ReadonlyConfig options);
}