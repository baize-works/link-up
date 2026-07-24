package com.link.up.framework.job;

/**
 * Determines how source batches are distributed to sink tasks.
 */
public enum SinkPartitionStrategy {TABLE_AFFINITY, SPLIT_HASH}
