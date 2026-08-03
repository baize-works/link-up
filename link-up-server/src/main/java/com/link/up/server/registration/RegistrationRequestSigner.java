package com.link.up.server.registration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA256 registration request signer shared by register, heartbeat and deregister calls. */
public final class RegistrationRequestSigner {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

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
            return hex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not sign control-plane registration request",
                    exception);
        }
    }

    public static String sha256(String value) {
        try {
            return hex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not calculate SHA-256", exception);
        }
    }

    private static String hex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            result[index * 2] = HEX[value >>> 4];
            result[index * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(result);
    }
}
