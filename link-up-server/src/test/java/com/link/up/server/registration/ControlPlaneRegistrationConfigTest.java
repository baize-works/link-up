package com.link.up.server.registration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ControlPlaneRegistrationConfigTest {

    @Test
    public void shouldLoadPropertiesAndPreserveSecretSuffix() {
        set("link.up.registration.enabled", "true");
        set("link.up.registration.control-plane-url", "http://yak-ops:8080/");
        set("link.up.registration.secret", "0123456789abcdef/");
        set("link.up.registration.advertised-base-url", "http://worker-a:18080/");
        set("link.up.registration.labels", "region=south,zone=az-1");
        try {
            ControlPlaneRegistrationConfig config =
                    ControlPlaneRegistrationConfig.load();

            assertTrue(config.isEnabled());
            assertEquals("http://yak-ops:8080", config.getControlPlaneUrl());
            assertEquals("http://worker-a:18080", config.getAdvertisedBaseUrl());
            assertEquals("0123456789abcdef/", config.getSecret());
            assertEquals("south", config.getLabels().get("region"));
            assertEquals("az-1", config.getLabels().get("zone"));
        } finally {
            clear("link.up.registration.enabled");
            clear("link.up.registration.control-plane-url");
            clear("link.up.registration.secret");
            clear("link.up.registration.advertised-base-url");
            clear("link.up.registration.labels");
        }
    }

    private void set(String name, String value) {
        System.setProperty(name, value);
    }

    private void clear(String name) {
        System.clearProperty(name);
    }
}
