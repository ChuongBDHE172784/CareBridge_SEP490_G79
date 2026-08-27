package com.carebridge.backend.expertverification.registry;

import com.carebridge.backend.expertverification.registry.LicenseNumberNormalizer.NormalizedLicense;
import java.util.List;
import java.util.Optional;

/**
 * Compares what the expert declared against the rows the registry returned (MF-05 Spec 05 §7).
 *
 * <p>Only the handful of rows returned for that one licence number are examined — there is no
 * copy of the dataset to scan.
 */
public final class RegistryMatcher {

    /** Below this the two names are treated as different people. */
    private static final double NAME_MATCH_THRESHOLD = 0.9;

    private RegistryMatcher() {
    }

    public enum MatchResult {
        MATCHED,
        FUZZY,
        NOT_FOUND
    }

    /**
     * @param nameSimilarity 0.0–1.0, or null when there was no row to compare against
     * @param redFlag        true when the licence number exists but belongs to someone else — the
     *                       single most suspicious outcome this feature can produce
     */
    public record MatchOutcome(
            MatchResult result,
            double confidence,
            RegistryRow matchedRow,
            Double nameSimilarity,
            boolean redFlag) {

        static MatchOutcome notFound() {
            return new MatchOutcome(MatchResult.NOT_FOUND, 0.0, null, null, false);
        }
    }

    public static MatchOutcome match(List<RegistryRow> rows, NormalizedLicense declared, String declaredName) {
        if (rows == null || rows.isEmpty() || declared == null) {
            return MatchOutcome.notFound();
        }

        String canonical = declared.canonical();
        String expectedName = LicenseNumberNormalizer.normalizeName(declaredName);

        // Rule 1 — exact match on the canonical key. CCHN/GPHN already collapse inside canonical(),
        // which is what §7 step 3 asks for.
        Optional<RegistryRow> exact = rows.stream()
                .filter(row -> canonicalOf(row).map(canonical::equals).orElse(false))
                .findFirst();
        if (exact.isPresent()) {
            RegistryRow row = exact.get();
            double similarity = similarity(expectedName, LicenseNumberNormalizer.normalizeName(row.fullName()));
            if (similarity >= NAME_MATCH_THRESHOLD) {
                return new MatchOutcome(MatchResult.MATCHED, 0.95, row, similarity, false);
            }
            return new MatchOutcome(MatchResult.FUZZY, 0.60, row, similarity, true);
        }

        // Rule 2 — the source masks the leading characters of some serials, so fall back to a
        // suffix comparison. Without a name match this is a guess, and we do not guess.
        for (RegistryRow row : rows) {
            Optional<NormalizedLicense> parsed = LicenseNumberNormalizer.normalize(row.licenseNo());
            if (parsed.isEmpty() || !parsed.get().masked()) {
                continue;
            }
            NormalizedLicense sourceLicense = parsed.get();
            boolean sameProvince = sourceLicense.provinceCode().equals(declared.provinceCode());
            boolean serialTailMatches = declared.serial().endsWith(sourceLicense.serial());
            if (!sameProvince || !serialTailMatches) {
                continue;
            }
            double similarity = similarity(expectedName, LicenseNumberNormalizer.normalizeName(row.fullName()));
            if (similarity >= NAME_MATCH_THRESHOLD) {
                return new MatchOutcome(MatchResult.FUZZY, 0.70, row, similarity, false);
            }
        }

        return MatchOutcome.notFound();
    }

    private static Optional<String> canonicalOf(RegistryRow row) {
        return LicenseNumberNormalizer.normalize(row.licenseNo()).map(license -> license.canonical());
    }

    /** Levenshtein ratio; 1.0 for identical strings, 0.0 when both are empty is treated as no match. */
    static double similarity(String left, String right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        if (left.equals(right)) {
            return 1.0;
        }
        int distance = levenshtein(left, right);
        int longest = Math.max(left.length(), right.length());
        return 1.0 - ((double) distance / longest);
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int substitution = previous[j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}
