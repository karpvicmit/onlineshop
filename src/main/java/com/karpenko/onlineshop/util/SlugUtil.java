package com.karpenko.onlineshop.util;

import java.text.Normalizer;

/**
 * Utility class for generating URL-friendly slugs from strings.
 */
public final class SlugUtil {

    private SlugUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        // 1. Replace German umlauts with their two-letter equivalents
        String input = name
                .replace("ä", "ae").replace("Ä", "ae")
                .replace("ö", "oe").replace("Ö", "oe")
                .replace("ü", "ue").replace("Ü", "ue")
                .replace("ß", "ss");

        // 2. Normalize remaining accented characters (é -> e, etc.)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // 3. Lowercase and replace non-alphanumeric characters with a hyphen
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}