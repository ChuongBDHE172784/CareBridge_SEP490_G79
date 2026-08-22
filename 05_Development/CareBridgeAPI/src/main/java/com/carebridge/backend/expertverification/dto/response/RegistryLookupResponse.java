package com.carebridge.backend.expertverification.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * What the admin screen receives after a registry lookup (MF-05 Spec 05 §4.2).
 *
 * <p>Everything here is already decided by the backend, including {@code advisory}: the wording
 * that explains a {@code NOT_FOUND} is a business rule, not presentation, and the UI must not
 * invent its own (§4.4). The source URL is intentionally absent — only {@code sourceViewUrl},
 * which points back at CareBridge, is exposed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistryLookupResponse {

    private UUID credentialId;
    private String declaredLicenseNo;
    private String normalizedLicenseNo;

    /** MATCHED | FUZZY | NOT_FOUND | SOURCE_ERROR | DISABLED | NOT_APPLICABLE */
    private String result;

    private double confidence;
    private Instant queriedAt;
    private boolean fromCache;

    /** True when the licence number exists at the source but under a different name. */
    private boolean redFlag;

    private MatchedRecord matched;
    private NameComparison nameComparison;
    private SourceInfo source;

    /** Backend-authored guidance shown verbatim to the admin. Never null for non-MATCHED results. */
    private String advisory;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchedRecord {
        private String fullName;
        private String licenseNo;
        private String practiceScope;
        private String statusText;
        private String sourceRecordId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NameComparison {
        private String declared;
        private String registry;
        private Double similarity;
        private boolean consistent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceInfo {
        private String name;
        /** CareBridge-internal path; the registry has no per-record deep link (§3.7). */
        private String sourceViewUrl;
        private Instant sourceViewExpiresAt;
        private String note;
    }
}
