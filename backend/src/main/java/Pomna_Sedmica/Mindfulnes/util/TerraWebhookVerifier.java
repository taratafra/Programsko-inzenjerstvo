package Pomna_Sedmica.Mindfulnes.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Manual verification of the Terra `terra-signature` header.
 *
 * Based on Terra Docs (payload signing):
 *  - signature header format: `t=...,v1=...`
 *  - signed_payload: `{timestamp}.{raw_body}`
 *  - expected signature = HMAC_SHA256(signing_secret, signed_payload) hex
 */
public final class TerraWebhookVerifier {

    private TerraWebhookVerifier() {}

    public static boolean verify(String signatureHeader, byte[] rawBody, String signingSecret, long toleranceSeconds) {
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        if (signingSecret == null || signingSecret.isBlank()) return false;

        ParsedSig parsed = parse(signatureHeader);
        if (parsed == null) return false;

        long now = Instant.now().getEpochSecond();
        long delta = Math.abs(now - parsed.timestamp);
        if (toleranceSeconds > 0 && delta > toleranceSeconds) {
            return false;
        }

        String payload = parsed.timestamp + "." + new String(rawBody, StandardCharsets.UTF_8);
        String expected = hmacSha256Hex(signingSecret, payload);
        return constantTimeEquals(expected, parsed.v1);
    }

    private static ParsedSig parse(String header) {
        String[] parts = header.split(",");
        Long ts = null;
        String v1 = null;
        for (String part : parts) {
            String p = part.trim();
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String key = p.substring(0, eq).trim();
            String value = p.substring(eq + 1).trim();
            if ("t".equals(key)) {
                try {
                    ts = Long.parseLong(value);
                } catch (NumberFormatException ignored) {}
            } else if ("v1".equals(key)) {
                v1 = value;
            }
        }
        if (ts == null || v1 == null || v1.isBlank()) return null;
        return new ParsedSig(ts, v1);
    }

    private static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] out = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte bt : bytes) {
            sb.append(Character.forDigit((bt >> 4) & 0xF, 16));
            sb.append(Character.forDigit(bt & 0xF, 16));
        }
        return sb.toString();
    }

    private record ParsedSig(long timestamp, String v1) {}
}
