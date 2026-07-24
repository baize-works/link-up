package com.link.up.connector.jdbc.options;


import com.link.up.api.configuration.Option;
import com.link.up.api.configuration.Options;

import java.util.List;

public class MultiTableCommonOptions {

    public static final Option<MultiTableFailurePolicy> MULTI_TABLE_FAILURE_POLICY =
            Options.key("multi_table.failure_policy")
                    .enumType(MultiTableFailurePolicy.class)
                    .defaultValue(MultiTableFailurePolicy.FAIL_FAST)
                    .withDescription(
                            "Failure handling policy for multi-table jobs. "
                                    + "FAIL_FAST aborts the whole job on the first table error. "
                                    + "CONTINUE_OTHER_TABLES isolates failed tables and keeps healthy tables running when the error can be attributed to a single table.");

    public static final Option<List<String>> MULTI_TABLE_INITIAL_FAILED_TABLES =
            Options.key("seatunnel.multi_table.initial_failed_tables")
                    .listType()
                    .noDefaultValue()
                    .withDescription(
                            "Internal option used to propagate pre-runtime failed-table metadata into MultiTableSink.");

    private MultiTableCommonOptions() {
    }
}
