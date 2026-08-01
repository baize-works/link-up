package com.link.up.framework.job;

import com.link.up.api.job.JobSpec;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JobSpecCompilerTest {

    @Test
    public void shouldCompileStructuredConnectorOptions() {
        JobSpec spec = new JobSpec();
        spec.setName("structured-sync");
        spec.setSource(connector("jdbc", sourceOptions()));
        spec.setSink(connector("doris", sinkOptions()));

        JobSpec.Runtime runtime = new JobSpec.Runtime();
        runtime.setBatchSize(512);
        runtime.setSourceParallelism(2);
        runtime.setSinkParallelism(3);
        runtime.setPipelineParallelism(4);
        runtime.setMaxBufferedBatches(16);
        runtime.setMaxRecordsPerSecond(1000L);
        spec.setRuntime(runtime);

        JobDefinition definition = new JobSpecCompiler().compile(spec);

        assertEquals("structured-sync", definition.getName());
        assertEquals("jdbc", definition.getSource().getType());
        assertEquals("doris", definition.getSink().getType());
        assertEquals("orders", definition.getSource().getOptions().getSourceMap().get("table_path"));
        assertEquals(Arrays.asList("id", "tenant_id"),
                definition.getSink().getOptions().getSourceMap().get("primary_keys"));
        assertEquals(512, definition.getExecutionConfig().getBatchSize());
        assertEquals(2, definition.getExecutionConfig().getSourceParallelism());
        assertEquals(3, definition.getExecutionConfig().getSinkParallelism());
        assertEquals(4, definition.getExecutionConfig().getPipelineParallelism());
        assertEquals(1000L, definition.getExecutionConfig().getMaxRecordsPerSecond());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnsupportedProtocolVersion() {
        JobSpec spec = new JobSpec();
        spec.setApiVersion("link-up/v99");
        spec.setName("invalid");
        spec.setSource(connector("jdbc", sourceOptions()));
        spec.setSink(connector("jdbc", sinkOptions()));
        new JobSpecCompiler().compile(spec);
    }

    @Test
    public void shouldCopyOptionMaps() {
        Map<String, Object> options = sourceOptions();
        JobSpec spec = new JobSpec();
        spec.setName("copy-check");
        spec.setSource(connector("jdbc", options));
        spec.setSink(connector("jdbc", sinkOptions()));

        JobDefinition definition = new JobSpecCompiler().compile(spec);
        options.put("late_mutation", true);

        assertTrue(!definition.getSource().getOptions().getSourceMap().containsKey("late_mutation"));
    }

    private static JobSpec.Connector connector(String id, Map<String, Object> options) {
        JobSpec.Connector connector = new JobSpec.Connector();
        connector.setConnectorId(id);
        connector.setOptions(options);
        return connector;
    }

    private static Map<String, Object> sourceOptions() {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("url", "jdbc:mysql://127.0.0.1:3306/demo");
        options.put("table_path", "orders");
        return options;
    }

    private static Map<String, Object> sinkOptions() {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("table_path", "orders_archive");
        options.put("primary_keys", Arrays.asList("id", "tenant_id"));
        return options;
    }
}
