package com.link.up.server.runtime;

/**
 * 相同幂等键或外部执行 ID 被用于不同提交内容。
 */
public final class JobSubmissionConflictException
        extends RuntimeException {

    public JobSubmissionConflictException(
            String message) {
        super(message);
    }
}
