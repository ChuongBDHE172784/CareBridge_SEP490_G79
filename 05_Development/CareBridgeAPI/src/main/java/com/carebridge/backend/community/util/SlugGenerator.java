package com.carebridge.backend.community.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

// ADR-COM-018: single source of truth for slug generation — mirrors the (now removed)
// client-side generateSlug() previously in ManageTopicsPage.tsx.
public final class SlugGenerator {

    private static final Pattern COMBINING_DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    private SlugGenerator() {}

    public static String generate(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        normalized = COMBINING_DIACRITICS.matcher(normalized).replaceAll("");
        normalized = normalized.replace('đ', 'd').replace('Đ', 'D');
        normalized = normalized.toLowerCase();
        normalized = NON_SLUG_CHARS.matcher(normalized).replaceAll("");
        normalized = normalized.trim();
        normalized = WHITESPACE.matcher(normalized).replaceAll("-");
        normalized = MULTI_DASH.matcher(normalized).replaceAll("-");
        return normalized;
    }
}
