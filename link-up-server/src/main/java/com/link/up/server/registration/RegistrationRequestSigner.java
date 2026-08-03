package com.link.up.server.registration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA256 registration request signer shared by register, heartbeat and deregister calls. */
public final class RegistrationRequestSigner {

    private RegistrationRequestSigner() {
    }

    public static String sign(
            String method,
            String path,
            long timestamp,
            String nonce,
            String body,
            String secret) {

        String payload = body == null ? "" : body;
        String canonical = method.toUpperCase()
                + "\n" + path
                + "\n" + timestamp
                + "\n" + nonce
                + "\n" + sha256(payload);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not sign control-plane registration request",
                    exception);
        }
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not calculate SHA-256", exception);
        }
    }
}
