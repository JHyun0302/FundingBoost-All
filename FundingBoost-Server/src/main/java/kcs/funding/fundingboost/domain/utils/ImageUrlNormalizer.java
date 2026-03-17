package kcs.funding.fundingboost.domain.utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class ImageUrlNormalizer {

    private ImageUrlNormalizer() {
    }

    public static String normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }

        String normalized = rawUrl.trim();
        if (normalized.startsWith("//")) {
            normalized = "https:" + normalized;
        }

        for (int i = 0; i < 2; i++) {
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (lower.startsWith("http%3a") || lower.startsWith("https%3a")) {
                normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
                continue;
            }
            break;
        }

        return normalized;
    }
}
