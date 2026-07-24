package com.link.up.server.runtime;

public final class JobStateConflictException
        extends RuntimeException {

    public JobStateConflictException(
            String jobId,
            ServerJobStatus status) {

        super(
                "Job "
                        + jobId
                        + " cannot be cancelled in state "
                        + status);
    }
}