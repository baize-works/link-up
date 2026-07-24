package com.link.up.connector.jdbc.sink;

import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.connector.jdbc.config.JdbcSinkConfig;

import java.util.Objects;

/**
 * Builder for the JDBC output format.
 */
public final class JdbcOutputFormatBuilder {
    private JdbcSinkConfig config;
    private PreparedSinkMetadata metadata;

    public JdbcOutputFormatBuilder withConfig(JdbcSinkConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        return this;
    }

    public JdbcOutputFormatBuilder withMetadata(PreparedSinkMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public JdbcOutputFormat build() {
        if (config == null) throw new IllegalStateException("JdbcSinkConfig must be configured before build");
        return new JdbcOutputFormat(config, metadata);
    }
}
