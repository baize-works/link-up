package com.link.up.api.factory;

import com.link.up.api.configuration.ReadonlyConfig;
import com.link.up.api.configuration.util.OptionRule;
import com.link.up.api.sink.PreparedSinkMetadata;
import com.link.up.api.sink.SinkPreparer;
import com.link.up.api.sink.SinkWriter;
import com.link.up.api.table.type.FluxRow;

public interface SinkFactory extends Factory {

    OptionRule optionRule();

    /**
     * Creates the runtime writer only after preparation has completed.
     */
    default SinkWriter<FluxRow> createSink(ReadonlyConfig config, PreparedSinkMetadata metadata) {
        return createSink(config);
    }

    /**
     * @deprecated implement the metadata-aware overload for prepared sinks.
     */
    @Deprecated
    default SinkWriter<FluxRow> createSink(ReadonlyConfig config) {
        throw new UnsupportedOperationException("Sink factory must implement createSink(config, metadata)");
    }

    /**
     * Returns the preparation contract. The default preserves legacy sinks.
     */
    default SinkPreparer createPreparer(ReadonlyConfig config) {
        return context -> new PreparedSinkMetadata(context.getSourceTables());
    }
}
