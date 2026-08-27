package com.carebridge.backend.triage;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.triage.dto.response.TriageConsentAcceptOutcome;
import com.carebridge.backend.triage.dto.response.TriageConsentStatusResponse;
import com.carebridge.backend.triage.entity.TriageDisclaimerConsent;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.policy.TriageDisclaimerPolicy;
import com.carebridge.backend.triage.repository.TriageDisclaimerConsentRepository;
import com.carebridge.backend.triage.service.impl.TriageConsentService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import static com.carebridge.backend.triage.TriageConsentTestFactory.MOTHER_ID;
import static com.carebridge.backend.triage.TriageConsentTestFactory.TEXT_V1;
import static com.carebridge.backend.triage.TriageConsentTestFactory.V1;
import static com.carebridge.backend.triage.TriageConsentTestFactory.V2;
import static com.carebridge.backend.triage.TriageConsentTestFactory.makeAcceptRequest;
import static com.carebridge.backend.triage.TriageConsentTestFactory.makeActiveConsent;
import static com.carebridge.backend.triage.TriageConsentTestFactory.makeRevokedConsent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CB-TRIAGE-CONSENT-IMP-001-TEST — unit tests for {@code TriageConsentService}
 * (mocked repository/policy/audit — Test-Spec TDS-01). Props isolation: every test creates
 * fresh mocks + service via {@link #newService()} and fresh fixtures via
 * {@link TriageConsentTestFactory} (anti AP-AI-002).
 */
class TriageConsentServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-26T10:00:00Z");

    private TriageDisclaimerConsentRepository repository;
    private TriageDisclaimerPolicy policy;
    private AuditService auditService;

    private TriageConsentService newService() {
        repository = mock(TriageDisclaimerConsentRepository.class);
        policy = mock(TriageDisclaimerPolicy.class);
        auditService = mock(AuditService.class);
        return new TriageConsentService(repository, policy, auditService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** FX-005 — current version V1, canonical text TEXT_V1. */
    private void stubPolicyV1() {
        when(policy.currentVersion()).thenReturn(V1);
        when(policy.disclaimerText()).thenReturn(TEXT_V1);
        when(policy.evidenceKeyFor(TEXT_V1)).thenReturn("synthetic-sha256-of-text-v1");
    }

    /** FX-006 — current version bumped to V2 (same synthetic text). */
    private void stubPolicyV2() {
        when(policy.currentVersion()).thenReturn(V2);
        when(policy.disclaimerText()).thenReturn(TEXT_V1);
        when(policy.evidenceKeyFor(TEXT_V1)).thenReturn("synthetic-sha256-of-text-v1");
    }

    /** Repository with no AI_TRIAGE_DISCLAIMER rows at all. */
    private void stubNoRows() {
        when(repository.existsByOwnerUserIdAndPolicyVersionAndStatus(any(), anyString(), anyString()))
                .thenReturn(false);
        when(repository.findFirstByOwnerUserIdAndPolicyVersionAndStatus(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(repository.findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(any(), anyString()))
                .thenReturn(Optional.empty());
        when(repository.findFirstByOwnerUserIdOrderByGrantedAtDesc(any()))
                .thenReturn(Optional.empty());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-01 — GET status, never accepted → REQUIRED / NOT_ACCEPTED
    // Oracle: TDS §9.2 GET schema / US-TDC-001 / roadmap Part III.4
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc01_getStatus_neverAccepted_shouldReturnRequiredNotAccepted() {
        TriageConsentService service = newService();
        stubPolicyV1();
        stubNoRows();

        TriageConsentStatusResponse status = service.getStatus(MOTHER_ID);

        assertThat(status.getStatus()).isEqualTo("REQUIRED");
        assertThat(status.getReason()).isEqualTo("NOT_ACCEPTED");
        assertThat(status.getCurrentVersion()).isEqualTo(V1);
        assertThat(status.getAcceptedVersion()).isNull();
        assertThat(status.getAcceptedAt()).isNull();
        assertThat(status.getDisclaimerText()).isEqualTo(TEXT_V1);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-02 — First accept persists exact data_permissions values → created=true
    // Oracle: TDS §5.2 column mapping + baseline DDL B20260724111500 / BR-TDC-001/007
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc02_accept_firstTime_shouldPersistExactColumnValues_andAuditOnce() {
        TriageConsentService service = newService();
        stubPolicyV1();
        stubNoRows();
        when(repository.acquireDisclaimerConsentLock(MOTHER_ID)).thenReturn(1);
        when(repository.save(any(TriageDisclaimerConsent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TriageConsentAcceptOutcome outcome = service.accept(makeAcceptRequest(), MOTHER_ID);

        ArgumentCaptor<TriageDisclaimerConsent> captor =
                ArgumentCaptor.forClass(TriageDisclaimerConsent.class);
        verify(repository, times(1)).save(captor.capture());
        TriageDisclaimerConsent saved = captor.getValue();

        // Expected persistence side effects — exact data_permissions column values (TDC-TC-02)
        assertThat(saved.getOwnerUserId()).isEqualTo(MOTHER_ID);
        assertThat(saved.getPermissionKind()).isEqualTo("AI_TRIAGE_DISCLAIMER");
        assertThat(saved.getScopeType()).isEqualTo("TRIAGE");
        assertThat(saved.getScopeText()).isEqualTo("ELECTIVE_AI_TRIAGE_INTAKE_ONLY");
        assertThat(saved.getPurpose()).isEqualTo("AI_TRIAGE_GUIDANCE");
        assertThat(saved.getPolicyVersion()).isEqualTo(V1);
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getGrantedAt()).isNotNull();
        assertThat(saved.getExpiresAt()).isNull();                       // L5
        assertThat(saved.getRevokedAt()).isNull();
        assertThat(saved.getRevokedBy()).isNull();
        assertThat(saved.getVersionNumber()).isEqualTo(1);
        assertThat(saved.getPermissionSeriesId()).isNotNull();
        assertThat(saved.getPermissionSeriesId()).isEqualTo(saved.getPermissionId()); // chain seed
        assertThat(saved.getSupersedesPermissionId()).isNull();
        assertThat(saved.getConsentEvidenceKey()).isEqualTo("synthetic-sha256-of-text-v1");
        assertThat(saved.getLocale()).isEqualTo("vi");

        assertThat(outcome.created()).isTrue();
        assertThat(outcome.status().getStatus()).isEqualTo("ACCEPTED");
        verify(auditService, times(1)).log(
                eq(AuditAction.CONSENT_GRANTED), eq(MOTHER_ID), eq("TriageDisclaimerConsent"),
                eq(saved.getPermissionId().toString()), any());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-03 — GET status after accept → ACCEPTED
    // Oracle: TDS §9.2 GET (accepted) schema
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc03_getStatus_afterAccept_shouldReturnAccepted() {
        TriageConsentService service = newService();
        stubPolicyV1();
        when(repository.findFirstByOwnerUserIdAndPolicyVersionAndStatus(MOTHER_ID, V1, "ACTIVE"))
                .thenReturn(Optional.of(makeActiveConsent()));

        TriageConsentStatusResponse status = service.getStatus(MOTHER_ID);

        assertThat(status.getStatus()).isEqualTo("ACCEPTED");
        assertThat(status.getReason()).isNull();
        assertThat(status.getAcceptedVersion()).isEqualTo(V1);
        assertThat(status.getAcceptedAt()).isEqualTo(Instant.parse("2026-07-26T08:00:00Z"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-04 (service part) — Idempotent re-accept same version → created=false, no write
    // Oracle: TDS §9.1 (accept idempotent) / ADR-TDC-004 / ITriageConsentService.accept javadoc
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc04_accept_sameVersionAgain_shouldBeIdempotent_noSave_noAudit() {
        TriageConsentService service = newService();
        stubPolicyV1();
        TriageDisclaimerConsent existing = makeActiveConsent();
        when(repository.acquireDisclaimerConsentLock(MOTHER_ID)).thenReturn(1);
        when(repository.findFirstByOwnerUserIdAndPolicyVersionAndStatus(MOTHER_ID, V1, "ACTIVE"))
                .thenReturn(Optional.of(existing));

        TriageConsentAcceptOutcome outcome = service.accept(makeAcceptRequest(), MOTHER_ID);

        assertThat(outcome.created()).isFalse();
        assertThat(outcome.status().getStatus()).isEqualTo("ACCEPTED");
        assertThat(outcome.status().getAcceptedAt()).isEqualTo(existing.getGrantedAt());
        verify(repository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-05 — Accept with stale displayed version → 409 VERSION_MISMATCH, no write
    // Oracle: TDS §10 (TRIAGE_CONSENT_VERSION_MISMATCH, 409) / BR-TDC-002
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc05_accept_staleDisplayedVersion_shouldThrow409VersionMismatch_noWrite() {
        TriageConsentService service = newService();
        stubPolicyV2();                       // config bumped to V2
        when(repository.acquireDisclaimerConsentLock(MOTHER_ID)).thenReturn(1);

        assertThatThrownBy(() -> service.accept(makeAcceptRequest(), MOTHER_ID)) // request carries V1
                .isInstanceOfSatisfying(TriageException.class, ex -> {
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("TRIAGE_CONSENT_VERSION_MISMATCH");
                });

        verify(repository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-09 — Version bump: previously ACTIVE consent no longer effective
    // Oracle: BR-TDC-002 / ADR-TDC-003 / TriageExpertHandoffPolicy precedent
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc09_versionBump_shouldGateAndReportVersionChanged() {
        TriageConsentService service = newService();
        stubPolicyV2();                       // current V2; user only holds ACTIVE V1
        when(repository.existsByOwnerUserIdAndPolicyVersionAndStatus(MOTHER_ID, V2, "ACTIVE"))
                .thenReturn(false);
        when(repository.findFirstByOwnerUserIdAndPolicyVersionAndStatus(MOTHER_ID, V2, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(repository.findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(MOTHER_ID, "ACTIVE"))
                .thenReturn(Optional.of(makeActiveConsent()));

        assertThatThrownBy(() -> service.ensureActiveConsent(MOTHER_ID))
                .isInstanceOfSatisfying(TriageException.class, ex -> {
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("TRIAGE_CONSENT_REQUIRED");
                });

        TriageConsentStatusResponse status = service.getStatus(MOTHER_ID);
        assertThat(status.getStatus()).isEqualTo("REQUIRED");
        assertThat(status.getReason()).isEqualTo("VERSION_CHANGED");
        assertThat(status.getCurrentVersion()).isEqualTo(V2);
        assertThat(status.getAcceptedVersion()).isEqualTo(V1);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-10 — Re-accept after version bump: supersession chain persisted
    // Oracle: TDS §6.3 state machine (ACTIVE → SUPERSEDED) / baseline chain columns
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc10_reacceptAfterBump_shouldSupersedeOldRowAndChainNewRow() {
        TriageConsentService service = newService();
        stubPolicyV2();
        TriageDisclaimerConsent old = makeActiveConsent();
        when(repository.acquireDisclaimerConsentLock(MOTHER_ID)).thenReturn(1);
        when(repository.findFirstByOwnerUserIdAndPolicyVersionAndStatus(MOTHER_ID, V2, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(repository.findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(MOTHER_ID, "ACTIVE"))
                .thenReturn(Optional.of(old));
        when(repository.save(any(TriageDisclaimerConsent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TriageConsentAcceptOutcome outcome =
                service.accept(makeAcceptRequest(r -> r.setPolicyVersion(V2)), MOTHER_ID);

        ArgumentCaptor<TriageDisclaimerConsent> captor =
                ArgumentCaptor.forClass(TriageDisclaimerConsent.class);
        verify(repository, times(2)).save(captor.capture());
        TriageDisclaimerConsent supersededOld = captor.getAllValues().get(0);
        TriageDisclaimerConsent created = captor.getAllValues().get(1);

        // old row: SUPERSEDED (not REVOKED, not deleted — BR-TDC-005)
        assertThat(supersededOld.getPermissionId()).isEqualTo(old.getPermissionId());
        assertThat(supersededOld.getStatus()).isEqualTo("SUPERSEDED");
        assertThat(supersededOld.getPolicyVersion()).isEqualTo(V1); // history not rewritten

        // new row: ACTIVE V2 chained to the old row
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        assertThat(created.getPolicyVersion()).isEqualTo(V2);
        assertThat(created.getVersionNumber()).isEqualTo(2);
        assertThat(created.getSupersedesPermissionId()).isEqualTo(old.getPermissionId());
        assertThat(created.getPermissionSeriesId()).isEqualTo(old.getPermissionSeriesId());

        assertThat(outcome.created()).isTrue();
        verify(auditService, times(1)).log(
                eq(AuditAction.CONSENT_GRANTED), eq(MOTHER_ID), eq("TriageDisclaimerConsent"),
                eq(created.getPermissionId().toString()), any());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-11 — Revoke: ACTIVE → REVOKED with revoked_at / revoked_by / audit
    // Oracle: BR-TDC-003 / ConsentServiceImpl.revokeConsent precedent
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc11_revoke_shouldTransitionToRevoked_withAudit_noDelete() {
        TriageConsentService service = newService();
        stubPolicyV1();
        TriageDisclaimerConsent active = makeActiveConsent();
        when(repository.acquireDisclaimerConsentLock(MOTHER_ID)).thenReturn(1);
        when(repository.findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(MOTHER_ID, "ACTIVE"))
                .thenReturn(Optional.of(active));
        when(repository.save(any(TriageDisclaimerConsent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TriageConsentStatusResponse status = service.revoke(MOTHER_ID);

        ArgumentCaptor<TriageDisclaimerConsent> captor =
                ArgumentCaptor.forClass(TriageDisclaimerConsent.class);
        verify(repository, times(1)).save(captor.capture());
        TriageDisclaimerConsent revoked = captor.getValue();
        assertThat(revoked.getStatus()).isEqualTo("REVOKED");
        assertThat(revoked.getRevokedAt()).isNotNull();
        assertThat(revoked.getRevokedBy()).isEqualTo(MOTHER_ID);
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());

        assertThat(status.getStatus()).isEqualTo("REQUIRED");
        assertThat(status.getReason()).isEqualTo("REVOKED");
        verify(auditService, times(1)).log(
                eq(AuditAction.CONSENT_REVOKED), eq(MOTHER_ID), eq("TriageDisclaimerConsent"),
                eq(active.getPermissionId().toString()), any());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-12 — Revoke with no ACTIVE consent → 404 TRIAGE_CONSENT_NOT_FOUND
    // Oracle: TDS §10 (TRIAGE_CONSENT_NOT_FOUND, 404)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc12_revoke_withoutActiveRow_shouldThrow404_noWrite() {
        TriageConsentService service = newService();
        when(repository.acquireDisclaimerConsentLock(MOTHER_ID)).thenReturn(1);
        when(repository.findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(MOTHER_ID, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(repository.findFirstByOwnerUserIdOrderByGrantedAtDesc(MOTHER_ID))
                .thenReturn(Optional.of(makeRevokedConsent()));

        assertThatThrownBy(() -> service.revoke(MOTHER_ID))
                .isInstanceOfSatisfying(TriageException.class, ex -> {
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getCode()).isEqualTo("TRIAGE_CONSENT_NOT_FOUND");
                });

        verify(repository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-13 — After revocation, next elective intake requires re-accept
    // Oracle: BR-TDC-003 / roadmap Part III.4
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc13_afterRevocation_gateBlocksAgain_withReasonRevoked() {
        TriageConsentService service = newService();
        stubPolicyV1();
        when(repository.existsByOwnerUserIdAndPolicyVersionAndStatus(MOTHER_ID, V1, "ACTIVE"))
                .thenReturn(false);
        when(repository.findFirstByOwnerUserIdAndPolicyVersionAndStatus(MOTHER_ID, V1, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(repository.findFirstByOwnerUserIdAndStatusOrderByGrantedAtDesc(MOTHER_ID, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(repository.findFirstByOwnerUserIdOrderByGrantedAtDesc(MOTHER_ID))
                .thenReturn(Optional.of(makeRevokedConsent()));

        assertThatThrownBy(() -> service.ensureActiveConsent(MOTHER_ID))
                .isInstanceOfSatisfying(TriageException.class, ex -> {
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("TRIAGE_CONSENT_REQUIRED");
                });

        TriageConsentStatusResponse status = service.getStatus(MOTHER_ID);
        assertThat(status.getStatus()).isEqualTo("REQUIRED");
        assertThat(status.getReason()).isEqualTo("REVOKED");
    }
}
