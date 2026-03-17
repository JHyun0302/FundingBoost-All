package kcs.funding.fundingboost.payment.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public final class PaymentIntentKeyResolver {

    private static final String IDEMPOTENCY_KEY_PREFIX = "pi_idem_";

    private PaymentIntentKeyResolver() {
    }

    public static Optional<String> resolveFromIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }

        String trimmed = idempotencyKey.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(IDEMPOTENCY_KEY_PREFIX + sha256Hex(trimmed));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
