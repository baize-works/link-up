package com.link.up.api.sink;

/**
 * Scope at which a sink can make data durable.
 *
 * <p>{@link #TASK_LOCAL} means each SinkTask commits independently. It does not
 * provide Job-wide atomicity or a global rollback guarantee.
 */
public enum CommitScope {

    TASK_LOCAL
}
