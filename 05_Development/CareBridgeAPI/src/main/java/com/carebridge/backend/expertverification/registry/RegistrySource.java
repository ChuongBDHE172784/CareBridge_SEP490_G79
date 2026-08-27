package com.carebridge.backend.expertverification.registry;

/**
 * A public practice-licence registry that can be queried for a single licence number.
 *
 * <p>Only {@link HcmMedinetRegistrySource} implements this today (MF-05 Spec 05 §13 keeps other
 * provinces out of scope). The interface exists so a second province can be added without
 * touching the matcher or the service layer.
 */
public interface RegistrySource {

    /** Stable identifier stored in the audit payload, e.g. {@code HCM_MEDINET}. */
    String sourceCode();

    /** Human-readable name shown to admins. The source URL is never exposed to clients (§4.4). */
    String displayName();

    /** @return false when the integration is switched off by configuration */
    boolean isEnabled();

    /**
     * Runs one lookup. Never throws for source-side problems — network failures, timeouts and
     * unexpected markup all come back as {@link RegistryQueryResult.Status#SOURCE_ERROR} so the
     * admin screen degrades instead of erroring.
     */
    RegistryQueryResult lookup(String licenseNumber);
}
