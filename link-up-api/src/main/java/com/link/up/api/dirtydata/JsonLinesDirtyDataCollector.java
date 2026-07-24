package com.link.up.api.dirtydata;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JsonLinesDirtyDataCollector extends BoundedMemoryDirtyDataCollector {
    private final Path output;
    private final ObjectMapper mapper = new ObjectMapper();
    private Path temporary;
    private BufferedWriter writer;

    public JsonLinesDirtyDataCollector(String task, int samples, long count, double pct, Path output) {
        super(task, samples, count, pct);
        this.output = output;
    }

    @Override
    public void open() throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        temporary = Files.createTempFile(output.toAbsolutePath().getParent(), output.getFileName().toString() + ".", ".tmp");
        writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8);
    }

    @Override
    public void collect(DirtyRecord r) throws IOException {
        super.collect(r);
        writer.write(mapper.writeValueAsString(r));
        writer.newLine();
    }

    @Override
    public void close(boolean successful) throws IOException {
        if (writer != null) writer.close();
        if (successful && temporary != null)
            Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        else if (temporary != null) Files.deleteIfExists(temporary);
    }

    @Override
    public DirtyDataSummary summary() {
        DirtyDataSummary s = super.summary();
        return new DirtyDataSummary(s.getDirtyCount(), s.getAttemptedCount(), s.getTaskCounts(), s.isCountThresholdExceeded(), s.isPercentageThresholdExceeded(), s.getSampleCount(), output.toString());
    }
}
