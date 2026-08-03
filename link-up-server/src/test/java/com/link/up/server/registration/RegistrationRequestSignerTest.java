package com.link.up.server.registration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegistrationRequestSignerTest {

    @Test
    public void shouldProduceStableHmacSignature() {
        String signature = RegistrationRequestSigner.sign(
                "POST",
                "/api/v1/offline/worker-registration/register",
                1785700000123L,
                "1234567890abcdef1234567890abcdef",
                "{\"nodeId\":\"worker-a\"}",
                "0123456789abcdef/");

        assertEquals(
                "3a82a2ca22da29a61be4df9184039be15e7b4ad10c43f50acc50d72bd79cbb23",
                signature);
    }
}
