package com.link.up.connector.jdbc.config;

import com.link.up.api.configuration.Option;
import com.link.up.api.configuration.Options;
import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.configuration.util.ConditionExtension;
import com.link.up.api.configuration.util.Conditions;
import com.link.up.api.configuration.util.OptionRule;

import java.util.Map;

/**
 * JDBC Source、Sink、Catalog 共用配置。
 * <p>
 * 这里只保留真正属于 JDBC 连接层的配置，
 * 不放 Source 分片、Sink 批次或数据库专属能力。
 */
public class JdbcCommonOptions {


    public static final Option<String> URL =
            Options.key("url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("JDBC 连接地址");

    public static final Option<String> DRIVER =
            Options.key("driver")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("JDBC Driver 类名");

    public static final Option<String> USERNAME =
            Options.key("username")
                    .stringType()
                    .noDefaultValue()
                    .withFallbackKeys("user")
                    .withDescription("数据库用户名");

    public static final Option<String> PASSWORD =
            Options.key("password")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("数据库密码");

    /**
     * 显式指定数据库方言。
     * <p>
     * 未配置时，可以根据 JDBC URL 自动识别。
     */
    public static final Option<String> DIALECT =
            Options.key("dialect")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("数据库方言，例如 mysql、postgresql");

    /**
     * OceanBase 等兼容多种数据库协议的数据库可使用该配置。
     */
    public static final Option<String> COMPATIBLE_MODE =
            Options.key("compatible_mode")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("数据库兼容模式");

    /**
     * 默认 Schema。
     * <p>
     * MySQL 中通常不使用；
     * PostgreSQL、Oracle 等数据库可能需要。
     */
    public static final Option<String> SCHEMA =
            Options.key("schema")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("默认 Schema");

    public static final Option<Integer> CONNECTION_CHECK_TIMEOUT_SEC =
            Options.key("connection_check_timeout_sec")
                    .intType()
                    .defaultValue(30)
                    .withDescription("连接校验超时时间，单位秒");

    public static final Option<Integer> CONNECT_TIMEOUT_MS =
            Options.key("connect_timeout_ms")
                    .intType()
                    .defaultValue(30_000)
                    .withDescription("建立 JDBC 连接的超时时间，单位毫秒");

    /**
     * 0 表示由 JDBC Driver 决定，适合长时间离线读取。
     */
    public static final Option<Integer> SOCKET_TIMEOUT_MS =
            Options.key("socket_timeout_ms")
                    .intType()
                    .defaultValue(0)
                    .withDescription("Socket 读取超时时间，0 表示不主动限制");

    public static final Option<Map<String, String>> PROPERTIES =
            Options.key("properties")
                    .mapType()
                    .noDefaultValue()
                    .withDescription("附加 JDBC 连接参数");

    /**
     * MySQL 类型映射选项。
     * <p>
     * 该配置虽然不是连接参数，但 Source、Catalog 都可能使用，
     * 暂时放在 JDBC 公共选项中。
     */
    public static final Option<Boolean> INT_TYPE_NARROWING =
            Options.key("int_type_narrowing")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "是否缩小 MySQL 整数类型，例如将 tinyint 映射为 Byte");

    /**
     * Source、Sink、Catalog 工厂都可以复用的基础规则。
     */
    public static OptionRule.Builder baseConnectionRule() {
        return OptionRule.builder()
                .required(
                        URL,
                        Conditions.extension(
                                URL,
                                new JdbcUrlValidator()))
                .required(DRIVER)
                .optional(
                        USERNAME,
                        PASSWORD,
                        DIALECT,
                        COMPATIBLE_MODE,
                        SCHEMA,
                        CONNECTION_CHECK_TIMEOUT_SEC,
                        CONNECT_TIMEOUT_MS,
                        SOCKET_TIMEOUT_MS,
                        PROPERTIES,
                        INT_TYPE_NARROWING);
    }

    /**
     * 这里只做 JDBC URL 基础格式校验。
     * <p>
     * MySQL、Oracle 等数据库的精确格式由各自 Factory 校验。
     */
    static final class JdbcUrlValidator
            implements ConditionExtension<String> {

        @Override
        public String description() {
            return "JDBC URL 必须以 jdbc: 开头";
        }

        @Override
        public boolean evaluate(
                ReadonlyConfig config,
                String value) {

            return value != null
                    && value.trim().startsWith("jdbc:");
        }
    }
}