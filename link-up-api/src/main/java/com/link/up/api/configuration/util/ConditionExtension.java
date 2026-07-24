package com.link.up.api.configuration.util;

import com.link.up.api.configuration.ReadonlyConfig;

/**
 * 自定义条件校验。
 *
 * @param <T> 配置值类型
 */
public interface ConditionExtension<T> {

    /**
     * 返回校验规则说明。
     */
    String description();

    /**
     * 执行校验。
     *
     * @param config 完整配置
     * @param value  当前配置值
     * @return 是否通过
     */
    boolean evaluate(
            ReadonlyConfig config,
            T value) throws OptionValidationException;
}