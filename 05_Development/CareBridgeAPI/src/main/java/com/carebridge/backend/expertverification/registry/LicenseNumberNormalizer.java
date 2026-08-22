package com.carebridge.backend.expertverification.registry;

import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalises Vietnamese practice-licence numbers so the registry matcher can compare values that
 * were typed by an expert against values rendered by the HCM health-department portal.
 *
 * <p>Two source-side quirks drive the rules (MF-05 Spec 05 §3.3, §3.5): the portal masks the
 * leading characters of some serials ({@code ..8146/ĐL-CCHN}), and the 2023 Law on Medical
 * Examination and Treatment renamed CCHN to GPHN, so both suffixes denote the same document.
 * The canonical form therefore drops the document-type suffix on purpose.
 */
public final class LicenseNumberNormalizer {

    /** Trailing document type, after dashes and spaces have been squeezed out of the suffix. */
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("^(.+?)(CCHN|GPHN)$");

    private static final Pattern DIACRITIC_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private LicenseNumberNormalizer() {
    }

    /**
     * @param serial       digits only, leading zeros removed ({@code 001563} → {@code 1563})
     * @param provinceCode province letters with Vietnamese diacritics folded ({@code ĐL} → {@code DL})
     * @param type         {@code CCHN} or {@code GPHN}, kept for display only — never for matching
     * @param masked       true when the source hid the leading characters of the serial
     */
    public record NormalizedLicense(String serial, String provinceCode, String type, boolean masked) {

        /**
         * Canonical comparison key. Deliberately excludes {@link #type()}: CCHN and GPHN are the
         * same document under the 2023 law, so {@code 1563/HP-CCHN} and {@code 1563/HP-GPHN} must
         * compare equal.
         */
        public String canonical() {
            return serial + "/" + provinceCode;
        }
    }

    /**
     * @return empty when the input cannot be read as a licence number at all — callers treat that
     *         as "nothing to look up", never as a validation failure against the expert.
     */
    public static Optional<NormalizedLicense> normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String cleaned = raw.trim().toUpperCase();
        int separator = cleaned.indexOf('/');
        if (separator < 0 || separator == cleaned.length() - 1) {
            return Optional.empty();
        }

        String serialPart = cleaned.substring(0, separator).trim();
        String suffixPart = cleaned.substring(separator + 1);

        boolean masked = serialPart.startsWith("..");
        String serial = serialPart.replace(".", "").replaceAll("\\s+", "");
        if (serial.isEmpty()) {
            return Optional.empty();
        }
        serial = stripLeadingZeros(serial);

        String squeezedSuffix = suffixPart.replaceAll("[-\\s]", "");
        Matcher matcher = SUFFIX_PATTERN.matcher(squeezedSuffix);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String provinceCode = foldDiacritics(matcher.group(1));
        String type = matcher.group(2);
        if (provinceCode.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new NormalizedLicense(serial, provinceCode, type, masked));
    }

    /** {@code 001563} → {@code 1563}, but {@code 000} → {@code 0} rather than an empty string. */
    private static String stripLeadingZeros(String serial) {
        int firstNonZero = 0;
        while (firstNonZero < serial.length() - 1 && serial.charAt(firstNonZero) == '0') {
            firstNonZero++;
        }
        return serial.substring(firstNonZero);
    }

    /**
     * {@code Đ} has no combining-mark decomposition, so it needs an explicit replacement before
     * the generic diacritic strip. Province codes such as {@code ĐL} (Đắk Lắk) depend on this.
     */
    private static String foldDiacritics(String value) {
        String withoutD = value.replace("Đ", "D").replace("đ", "d");
        String decomposed = Normalizer.normalize(withoutD, Normalizer.Form.NFD);
        return DIACRITIC_PATTERN.matcher(decomposed).replaceAll("");
    }

    /**
     * Name comparison used by the matcher: lowercase, diacritics folded, whitespace collapsed.
     * Vietnamese names differ in accents and casing between the two sources far more often than
     * they differ in spelling.
     */
    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return foldDiacritics(name).toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
