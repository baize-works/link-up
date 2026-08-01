package com.link.up.connector.jdbc.sink;

import com.google.auto.service.AutoService;
import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.configuration.util.OptionRule;
import com.link.up.api.connector.schema.ConnectorCapability;
import com.link.up.api.factory.SinkFactory;
import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.api.sink.SinkPreparer;
import com.link.up.api.sink.SinkWriter;
import com.link.up.api.table.type.FluxRow;
import com.link.up.connector.jdbc.config.JdbcCommonOptions;
import com.link.up.connector.jdbc.config.JdbcSinkConfig;
import com.link.up.connector.jdbc.config.JdbcSinkOptions;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * JDBC Sink SPI factory.
 */
@AutoService(SinkFactory.class)
public final class JdbcSinkFactory
        implements SinkFactory {

    @Override
    public String factoryIdentifier() {
        return "jdbc";
    }

    @Override
    public Set<ConnectorCapability> capabilities() {
        return Collections.unmodifiableSet(
                EnumSet.of(
                        ConnectorCapability.MULTI_TABLE,
                        ConnectorCapability.CUSTOM_SQL,
                        ConnectorCapability.UPSERT,
                        ConnectorCapability.AUTO_CREATE_TABLE,
                        ConnectorCapability.DIRTY_DATA_HANDLING));
    }

    @Override
    public OptionRule optionRule() {
        return JdbcCommonOptions.baseConnectionRule()
                .optional(
                        JdbcSinkOptions.TABLE_PATH,
                        JdbcSinkOptions.SCHEMA_SAVE_MODE,
                        JdbcSinkOptions.DATA_SAVE_MODE,
                        JdbcSinkOptions.WRITE_MODE,
                        JdbcSinkOptions.CUSTOM_SQL,
                        JdbcSinkOptions.PRIMARY_KEYS,
                        JdbcSinkOptions.BATCH_SIZE,
                        JdbcSinkOptions.PREPARED_STATEMENT_CACHE_SIZE,
                        JdbcSinkOptions.QUERY_TIMEOUT_SEC,
                        JdbcSinkOptions.MAX_RETRIES,
                        JdbcSinkOptions.DIRTY_DATA_POLICY,
                        JdbcSinkOptions.CREATE_PRIMARY_KEY)
                .build();
    }

    @Override
    public SinkPreparer createPreparer(
            ReadonlyConfig config) {

        return new JdbcSinkPreparer(
                JdbcSinkConfig.of(
                        config));
    }

    @Override
    public SinkWriter<FluxRow> createSink(
            ReadonlyConfig config,
            PreparedSinkMetadata metadata) {

        return new JdbcSinkWriter(
                JdbcSinkConfig.of(
                        config),
                metadata);
    }
}
