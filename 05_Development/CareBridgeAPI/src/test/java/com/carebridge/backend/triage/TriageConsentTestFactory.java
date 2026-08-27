package com.carebridge.backend.triage;

import com.carebridge.backend.triage.dto.request.AcceptTriageConsentRequest;
import com.carebridge.backend.triage.entity.TriageDisclaimerConsent;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * CB-TRIAGE-CONSENT-IMP-001-TEST §4 — CASE 2.0 Props Isolation Pattern.
 * Every test builds fresh instances via this factory — no shared mutable state (anti AP-AI-002).
 *
 * <p>Note: the Test-Spec's sample series UUID {@code ...0000000000S1} is not a valid UUID
 * ('S' is not a hex digit); {@code ...000000000051} is used instead (documented deviation).
 */
class TriageConsentTestFactory {

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID MOTHER2_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    static final String V1 = "AI_TRIAGE_DISCLAIMER_V1";
    static final String V2 = "AI_TRIAGE_DISCLAIMER_V2";
    static final String TEXT_V1 = "SYNTHETIC DISCLAIMER TEXT V1";

    /** Baseline valid ACTIVE consent — sync with FX-007. */
    static TriageDisclaimerConsent makeActiveConsent() {
        TriageDisclaimerConsent c = new TriageDisclaimerConsent();
        c.setPermissionId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
        c.setOwnerUserId(MOTHER_ID);
        c.setPolicyVersion(V1);
        c.setStatus("ACTIVE");
        c.setGrantedAt(Instant.parse("2026-07-26T08:00:00Z"));
        c.setPermissionSeriesId(UUID.fromString("00000000-0000-0000-0000-000000000051"));
        c.setVersionNumber(1);
        c.setLocale("vi");
        c.setConsentEvidenceKey("synthetic-sha256-of-text-v1");
        return c;   // expires_at, revoked_at, revoked_by, supersedes left null by design (L5)
    }

    static TriageDisclaimerConsent makeActiveConsent(Consumer<TriageDisclaimerConsent> overrides) {
        TriageDisclaimerConsent c = makeActiveConsent();
        overrides.accept(c);
        return c;
    }

    /** FX-008 — revoked row. */
    static TriageDisclaimerConsent makeRevokedConsent() {
        return makeActiveConsent(c -> {
            c.setStatus("REVOKED");
            c.setRevokedAt(Instant.parse("2026-07-26T09:00:00Z"));
            c.setRevokedBy(MOTHER_ID);
        });
    }

    static AcceptTriageConsentRequest makeAcceptRequest() {
        AcceptTriageConsentRequest r = new AcceptTriageConsentRequest();
        r.setPolicyVersion(V1);
        r.setLocale("vi");
        return r;
    }

    static AcceptTriageConsentRequest makeAcceptRequest(Consumer<AcceptTriageConsentRequest> overrides) {
        AcceptTriageConsentRequest r = makeAcceptRequest();
        overrides.accept(r);
        return r;
    }
}
