package com.karpenko.onlineshop.util;

import java.text.Normalizer;

/**
 * Hilfsklasse zur Generierung von URL-freundlichen Slugs aus Zeichenketten.
 */
public final class SlugUtil {

    private SlugUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    public static String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        // 1. Normalisierung: Wandelt Akzentzeichen (ä, ö, ü, é) in Basisbuchstaben (a, o, u, e) um.
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // 2. In Kleinbuchstaben umwandeln und alles, was kein Buchstabe oder keine Zahl ist, durch einen Bindestrich ersetzen
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}