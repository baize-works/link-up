package com.link.up.server.runtime;

public final class JobNotFoundException
        extends RuntimeException {

    public JobNotFoundException(String jobId) {
        super("Job not found: " + jobId);
    }
}