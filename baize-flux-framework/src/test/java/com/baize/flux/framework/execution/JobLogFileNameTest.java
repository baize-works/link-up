package com.baize.flux.framework.execution;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JobLogFileNameTest {

    @Test
    public void createsOneSafeFileNameForOneJobRun() {
        assertEquals(
                "job-orders_sync-128392984.log",
                JobLogFileName.create(
                        "orders sync",
                        128392984L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeRunId() {
        JobLogFileName.create(
                "orders",
                -1L);
    }
}
