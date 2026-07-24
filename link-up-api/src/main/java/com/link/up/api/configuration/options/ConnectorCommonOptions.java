package com.link.up.api.configuration.options;


import com.link.up.api.configuration.Option;
import com.link.up.api.configuration.Options;

import java.io.Serializable;
import java.util.List;

public class ConnectorCommonOptions implements
        Serializable {

    public static Option<String> PLUGIN_NAME =
            Options.key("plugin_name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Name of the SPI plugin class.");

    public static Option<String> PLUGIN_OUTPUT =
            Options.key("plugin_output")
                    .stringType()
                    .noDefaultValue()
                    .withFallbackKeys("result_table_name")
                    .withDescription(
                            "When plugin_output is not specified, "
                                    + "the data processed by this plugin will not be registered as a data set (dataStream/dataset) "
                                    + "that can be directly accessed by other plugins, or called a temporary table (table)"
                                    + "When plugin_output is specified, "
                                    + "the data processed by this plugin will be registered as a data set (dataStream/dataset) "
                                    + "that can be directly accessed by other plugins, or called a temporary table (table) . "
                                    + "The data set (dataStream/dataset) registered here can be directly accessed by other plugins "
                                    + "by specifying plugin_input .");

    public static Option<List<String>> PLUGIN_INPUT =
            Options.key("plugin_input")
                    .listType()
                    .noDefaultValue()
                    .withFallbackKeys("source_table_name")
                    .withDescription(
                            "When plugin_input is not specified, "
                                    + "the current plug-in processes the data set dataset output by the previous plugin in the configuration file. "
                                    + "When plugin_input is specified, the current plug-in is processing the data set corresponding to this parameter.");

    public static Option<String> METADATA_DATASOURCE_ID =
            Options.key("metadata_datasource_id")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "The data source ID for retrieving connection configuration from MetaData Center. "
                                    + "When specified, the connector will fetch connection details (e.g., URL, username, password) "
                                    + "from the external metadata service instead of using direct configuration.");
}
