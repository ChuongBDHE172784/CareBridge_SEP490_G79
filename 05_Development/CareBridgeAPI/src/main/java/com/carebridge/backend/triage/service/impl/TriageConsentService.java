package com.carebridge.backend.triage.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.triage.dto.request.AcceptTriageConsentRequest;
import com.carebridge.backend.triage.dto.response.TriageConsentAcceptOutcome;
import com.carebridge.backend.triage.dto.response.TriageConsentStatusResponse;
import com.carebridge.backend.triage.entity.TriageDisclaimerConsent;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.policy.TriageDisclaimerPolicy;
import com.carebridge.backend.triage.repository.TriageDisclaimerConsentRepository;
import com.carebridge.backend.triage.service.ITriageConsentService;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CB-TRIAGE-CONSENT-IMP-001 — AI-triage disclaimer consent workflows.
 *
 * <p>Constraints (TDS §17): C1 canonical {@code data_permissions} only; C4 semantic error codes
 * via {@link TriageException}; C6 accept/revoke are {@code @Transactional} and first acquire the
 * per-user advisory lock (ADR-TDC-004); C7 append-style lifecycle + audit on every transition.
 */
@Service
public class TriageConsentService implements ITriageConsentService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_SUPERSEDED = "SUPERSEDED";
    private static final String RESOURCE_TYPE = "TriageDisclaimerConsent";

    private final TriageDisclaimerConsentRepository repository;
    private final TriageDisclaimerPolicy policy;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public TriageConsentService(
            TriageDisclaimerConsentRepository repository,
            TriageDisclaimerPolicy policy,
            AuditService auditService) {
        this(repository, policy, auditService, Clock.systemUTC());
    }

    public TriageConsentService(
            TriageDisclaimerConsentRepository repository,
            TriageDisclaimerPolicy policy,
            AuditService auditService,
            Clock clock) {
        this.repository = repository;
        this.policy = policy;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public TriageConsentStatusResponse getStatus(UUID userId) {
        String currentVersion = policy.currentVersion();
        Optional<TriageDisclaimerConsent> effective =
                repository.findFirstByOwnerUserIdAndPolicyVersionAndStatus(
                        userId, currentVersion, STATUS_ACTIVE);
        if (effective.isPresent()) {
            return acceptedStatus(effective.get(), currentVersion);
        }
        // No effective consent — derive the REQUIRED reason (never throws: BR — REQUIRED is a
        // normal state for the dialog, TDC-TC-01).
        Optional<TriageDisclaimerConsent> staleActive =
                repository.findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(userId, STATUS_ACTIVE);
        if (staleActive.isPresent()) {
            TriageDisclaimerConsent stale = staleActive.get();
            return requiredStatus("VERSION_CHANGED", currentVersion,
                    stale.getPolicyVersion(), stale.getGrantedAt());
        }
        Optional<TriageDisclaimerConsent> newestAny =
                repository.findFirstByOwnerUserIdOrderByGrantedAtDesc(userId);
        if (newestAny.isPresent() && STATUS_REVOKED.equals(newestAny.get().getStatus())) {
            return requiredStatus("REVOKED", currentVersion, null, null);
        }
        return requiredStatus("NOT_ACCEPTED", currentVersion, null, null);
    }

    @Override
    @Transactional
    public TriageConsentAcceptOutcome accept(AcceptTriageConsentRequest request, UUID userId) {
        repository.acquireDisclaimerConsentLock(userId);   // ADR-TDC-004 — serialize per user
        String currentVersion = policy.currentVersion();
        if (!currentVersion.equals(request.getPolicyVersion())) {
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE_CONSENT_VERSION_MISMATCH",
                    "The disclaimer has been updated. Reload the consent dialog and accept the current version.");
        }
        Optional<TriageDisclaimerConsent> existing =
                repository.findFirstByOwnerUserIdAndPolicyVersionAndStatus(
                        userId, currentVersion, STATUS_ACTIVE);
        if (existing.isPresent()) {
            // Idempotent re-accept: no new row, no audit spam (TDC-TC-04).
            return new TriageConsentAcceptOutcome(false, acceptedStatus(existing.get(), currentVersion));
        }

        TriageDisclaimerConsent consent = new TriageDisclaimerConsent();
        consent.setPermissionId(UUID.randomUUID());
        consent.setOwnerUserId(userId);
        consent.setPermissionKind(TriageDisclaimerPolicy.PERMISSION_KIND);
        consent.setScopeType(TriageDisclaimerPolicy.SCOPE_TYPE);
        consent.setScopeText(TriageDisclaimerPolicy.SCOPE_TEXT);
        consent.setPurpose(TriageDisclaimerPolicy.PURPOSE);
        consent.setPolicyVersion(currentVersion);
        consent.setStatus(STATUS_ACTIVE);
        consent.setGrantedAt(Instant.now(clock));
        consent.setConsentEvidenceKey(policy.evidenceKeyFor(policy.disclaimerText()));
        consent.setLocale(request.getLocale());

        // Re-consent chain (BR-TDC-005 / TDC-TC-10): supersede the newest ACTIVE row of an
        // older version; first accept seeds the series with its own permission id (TDC-TC-02).
        Optional<TriageDisclaimerConsent> priorActive =
                repository.findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(userId, STATUS_ACTIVE);
        if (priorActive.isPresent()) {
            TriageDisclaimerConsent old = priorActive.get();
            old.setStatus(STATUS_SUPERSEDED);
            repository.save(old);
            consent.setPermissionSeriesId(old.getPermissionSeriesId() != null
                    ? old.getPermissionSeriesId() : old.getPermissionId());
            consent.setVersionNumber(old.getVersionNumber() != null
                    ? old.getVersionNumber() + 1 : 2);
            consent.setSupersedesPermissionId(old.getPermissionId());
        } else {
            consent.setPermissionSeriesId(consent.getPermissionId());
            consent.setVersionNumber(1);
        }

        TriageDisclaimerConsent saved = repository.save(consent);
        auditService.log(AuditAction.CONSENT_GRANTED, userId, RESOURCE_TYPE,
                saved.getPermissionId().toString(), auditPayload(saved));
        return new TriageConsentAcceptOutcome(true, acceptedStatus(saved, currentVersion));
    }

    @Override
    @Transactional
    public TriageConsentStatusResponse revoke(UUID userId) {
        repository.acquireDisclaimerConsentLock(userId);   // ADR-TDC-004
        TriageDisclaimerConsent active =
                repository.findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(userId, STATUS_ACTIVE)
                        .orElseThrow(() -> new TriageException(HttpStatus.NOT_FOUND,
                                "TRIAGE_CONSENT_NOT_FOUND",
                                "No active disclaimer consent to revoke."));
        active.setStatus(STATUS_REVOKED);
        active.setRevokedAt(Instant.now(clock));
        active.setRevokedBy(userId);
        TriageDisclaimerConsent saved = repository.save(active);
        auditService.log(AuditAction.CONSENT_REVOKED, userId, RESOURCE_TYPE,
                saved.getPermissionId().toString(),
                Map.of("policyVersion", String.valueOf(saved.getPolicyVersion())));
        return requiredStatus("REVOKED", policy.currentVersion(), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureActiveConsent(UUID userId) {
        // GATE (BR-TDC-004 / C3): canonical elective start/non-emergency continuation only.
        boolean effective = repository.existsByOwnerUserIdAndPolicyVersionAndStatus(
                userId, policy.currentVersion(), STATUS_ACTIVE);
        if (!effective) {
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE_CONSENT_REQUIRED",
                    "You must accept the AI triage disclaimer before starting an AI triage session.");
        }
    }

    private TriageConsentStatusResponse acceptedStatus(
            TriageDisclaimerConsent consent, String currentVersion) {
        TriageConsentStatusResponse status = new TriageConsentStatusResponse();
        status.setStatus("ACCEPTED");
        status.setReason(null);
        status.setCurrentVersion(currentVersion);
        status.setAcceptedVersion(consent.getPolicyVersion());
        status.setAcceptedAt(consent.getGrantedAt());
        status.setDisclaimerText(policy.disclaimerText());
        return status;
    }

    private TriageConsentStatusResponse requiredStatus(
            String reason, String currentVersion, String acceptedVersion, Instant acceptedAt) {
        TriageConsentStatusResponse status = new TriageConsentStatusResponse();
        status.setStatus("REQUIRED");
        status.setReason(reason);
        status.setCurrentVersion(currentVersion);
        status.setAcceptedVersion(acceptedVersion);
        status.setAcceptedAt(acceptedAt);
        status.setDisclaimerText(policy.disclaimerText());
        return status;
    }

    private Map<String, Object> auditPayload(TriageDisclaimerConsent saved) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("policyVersion", saved.getPolicyVersion());
        payload.put("versionNumber", saved.getVersionNumber());
        if (saved.getLocale() != null) {
            payload.put("locale", saved.getLocale());
        }
        return payload;
    }
}
