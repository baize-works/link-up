package com.link.up.server.registration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ControlPlaneRegistrationAgentRetryTest {

    @Test
    public void shouldRetryRegistrationConflictsWithoutExponentialBackoff() {
        assertEquals(
                5_000L,
                ControlPlaneRegistrationAgent.retryDelayForStatus(409, 32_000L));
        assertEquals(
                32_000L,
                ControlPlaneRegistrationAgent.retryDelayForStatus(500, 32_000L));
    }

    @Test
    public void shouldClassifyRestartConflictAsRegistrationRequired() {
        ControlPlaneRegistrationAgent.ControlPlaneException conflict =
                new ControlPlaneRegistrationAgent.ControlPlaneException(
                        409,
                        "old lease is still active");
        ControlPlaneRegistrationAgent.ControlPlaneException serverError =
                new ControlPlaneRegistrationAgent.ControlPlaneException(
                        500,
                        "server error");

        assertTrue(conflict.requiresRegistration());
        assertTrue(conflict.isRegistrationConflict());
        assertFalse(serverError.requiresRegistration());
        assertFalse(serverError.isRegistrationConflict());
    }
}
