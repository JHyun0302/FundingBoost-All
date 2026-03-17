package kcs.funding.fundingboost.domain.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ItemOptionParser {

    private ItemOptionParser() {
    }

    public static List<String> parseOptions(String rawOptionValue) {
        if (rawOptionValue == null || rawOptionValue.isBlank()) {
            return List.of();
        }

        String[] tokens = rawOptionValue.split("\\r?\\n|\\|\\|?|,");
        Set<String> deduplicated = new LinkedHashSet<>();

        for (String token : tokens) {
            String normalized = normalizeToken(token);
            if (normalized == null) {
                continue;
            }
            deduplicated.add(normalized);
        }

        return new ArrayList<>(deduplicated);
    }

    private static String normalizeToken(String token) {
        if (token == null) {
            return null;
        }

        String normalized = token.trim()
                .replaceAll("\\s+", " ")
                .replaceFirst("^(option\\s*[=:]\\s*)", "")
                .replaceFirst("^\\[[^\\]]+\\]\\s*", "")
                .replaceFirst("^옵션\\s*[=:]\\s*", "")
                .trim();

        if (normalized.isBlank()) {
            return null;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("상품 옵션을 선택")
                || lower.contains("선택해주세요")
                || lower.equals("옵션")
                || lower.equals("선택")
                || lower.equals("품절")) {
            return null;
        }

        return normalized;
    }
}
