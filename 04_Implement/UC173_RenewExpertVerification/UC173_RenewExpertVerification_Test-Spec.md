# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-173 Renew Expert Verification — Test Specification

**Document ID:** `CB-EXPGOV-TDD-173`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (lines 786-800 `expert_profiles`, 802-815 `expert_credentials`)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.4.2 (UC-173, lines 1600-1619)
- `04_Implement/UC173_RenewExpertVerification/UC173_RenewExpertVerification_TDS.md` (CB-EXPGOV-IMP-173) — Technical Specification
- `04_Implement/UC172_SuspendExpert/UC172_SuspendExpert_TDS.md` — RG-3 reconciliation source (`LOCKED`/`REINSTATE` state machine)
- `04_Implement/UC172_SuspendExpert/UC172_SuspendExpert_Test-Spec.md` — sibling Test-Spec, `ExpertGovernanceTestFactory` origin (this Test-Spec extends the SAME factory, no duplicate)
- `04_Implement/UC104_RevokeExpertBadge/UC104_RevokeExpertBadge_TDS.md` — `REINSTATE` action source (two-step recovery, ADR-REN-502)
- `04_Implement/UC103_VerifyExpertProfile/UC103_VerifyExpertProfile_TDS.md` — `VerificationDecisionAction` vocabulary source (ADR-REN-503)
- `CLAUDE.md` — BR-RBAC, audit requirements for expert/moderation/safety workflows

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`/`.test.tsx`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo Test-Spec cho UC-173; mở rộng `ExpertGovernanceTestFactory` (không tạo factory song song) từ UC-103/UC-104/UC-172 Test-Spec |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-173` |
| **Module** | `RenewExpertVerification — Bounded Context: expert` |
| **Spec gốc** | `CB-EXPGOV-IMP-173` |
| **Priority** | 🔴 P0 (High per SRS Table 95) |
| **Sprint** | `S3 Consultation Lifecycle, Expert Governance, And Location Visibility — TV4-Lâm` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA`, `BR-RBAC` |
| **Upstream Dependencies** | UC-103 (`VerificationDecisionAction` REUSED), UC-104 (`REINSTATE` action — two-step recovery, ADR-REN-502), UC-172 (RG-3 `LOCKED` eligibility reconciliation) |
| **Downstream Consumers** | UC-80/81 View Expert Directory/Profile (informational, out of scope), UC-112 View Expert Dashboard |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXPGOV-IMP-173 §17`, ADR-REN-501/502/503/504 |
| **Constraints Injected** | C1-C9 per TDS §17.1 (never write `verification_status`, VERIFIED/LOCKED eligibility only, reuse `VerificationDecisionAction`, no client-writable `reviewStatus`/ids, JWT-derived userId, EXPERT vs SYSTEM_ADMIN role split, no new migration, no scheduled job/`ExpertVerificationExpired` event, enum integrity) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Task instruction names `ExpertVerificationExpired` as an expected domain event | TDS §7.1 explicitly does NOT implement this event or any scheduled job (no schema/cron support found, RG-6/OI-4) | Tests assert NO `@Scheduled` method exists in `ExpertRenewalServiceImpl`/`ExpertCredentialReviewServiceImpl`, and that `ExpertVerificationExpired` is never imported/published — importing a non-existent class would itself be `AP-AI-005` |
| L2 | SRS UC-173 says "Verified Expert submits..." — could be read as "only from VERIFIED" | ADR-REN-502 (RG-3 resolution) concludes `LOCKED` is ALSO eligible, since UC-104's own rationale for `LOCKED` names "document expiry pending renewal" as the trigger — refusing `LOCKED` submissions would create a dead-end no other UC can resolve | Tests explicitly cover BOTH `VERIFIED` and `LOCKED` as eligible starting states (not just `VERIFIED`), plus all 4 ineligible states rejected |
| L3 | "Tracks renewal results" (SRS line 1607) has no explicit mechanism named | TDS §ADR-REN-501 resolves this as `GET .../renewals` reading `expert_credentials.review_status` per submission row (no dedicated renewal-history table exists in schema) | Tests assert the list endpoint returns `reviewStatus` per submission, ordered most-recent-first, sourced from the SAME `expert_credentials` table (not a hallucinated separate table) |
| L4 | Approving a renewal could naively be implemented to also reinstate a `LOCKED` expert (intuitive but NOT what ADR-REN-502/503 specify) | TDS explicit non-goal: `ExpertCredentialReviewServiceImpl.decide()` NEVER writes `expert_profiles.verification_status` — reinstatement remains UC-104's `REINSTATE` action, a SEPARATE Admin call | Tests assert `expert_profiles` row is byte-for-byte unchanged after an APPROVE decision on a renewal submitted by a `LOCKED` expert — this is UC-173's highest-value regression guard, analogous to UC-172's `SUSP-TC-019` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
RenewExpertVerification bao gồm các layer:
├── Domain (ExpertRenewalPolicy, ExpertCredentialReviewPolicy — NEW, pure logic)
├── Services (ExpertRenewalServiceImpl, ExpertCredentialReviewServiceImpl — NEW, mock JPA Repository với Mockito)
├── Controller (ExpertRenewalController, ExpertCredentialReviewController — NEW, @WebMvcTest)
├── Web (ExpertRenewalSubmitPage.tsx, ExpertRenewalHistoryPage.tsx, ExpertRenewalReviewQueuePage.tsx — NEW, Vitest + Testing Library)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full submit → review → (optional) reinstate flow)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-173` (§3.2.4.2, lines 1600-1619) | Submit renewal action; PRE-3/PRE-4 auth + existing-record guard; POST-2/POST-3 status update + audit; E1/E2 exceptions |
| `ADR-REN-501` | Renewal = new `expert_credentials` row, `review_status='PENDING'`; no dedicated history table |
| `ADR-REN-502` | Eligibility: `VERIFIED` or `LOCKED` only; `submitRenewal()` NEVER writes `expert_profiles` |
| `ADR-REN-503` | Admin decision reuses `VerificationDecisionAction`; `decide()` NEVER writes `expert_profiles` |
| `ADR-REN-504` | EXPERT-only self-service submission (`/me`), SYSTEM_ADMIN-only review, no MODERATOR either side |
| `BR-RBAC` | Role-scoped access enforcement |
| `V1__init_schema.sql` (lines 786-800, 802-815) | Real column names/types for persistence assertions |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Expert submits renewal from `VERIFIED` | `ExpertRenewalServiceImpl.submitRenewal()` | `REN-TC-001` |
| TC-COND-002 | Expert submits renewal from `LOCKED` (RG-3 scenario) | `ExpertRenewalServiceImpl.submitRenewal()` | `REN-TC-002` |
| TC-COND-003 | Submission rejected for `PENDING`/`REJECTED`/`NEEDS_MORE_INFO`/`REVOKED` | `ExpertRenewalPolicy.assertEligible()` | `REN-TC-003` to `REN-TC-006` |
| TC-COND-004 | Structural guard: `submitRenewal()` never calls `expertProfileRepository.save()` | `ExpertRenewalServiceImpl` (structural) | `REN-TC-007` |
| TC-COND-005 | Caller has no `expert_profiles` row | `ExpertRenewalServiceImpl.submitRenewal()` | `REN-TC-008` |
| TC-COND-006 | Validation: missing `expiryDate`/`fileUrl` rejected | `RenewalSubmissionRequest` Bean Validation | `REN-TC-009` |
| TC-COND-007 | Mass-assignment: `reviewStatus`/`expertProfileId` not client-settable | `RenewalSubmissionRequest` (structural) | `REN-TC-010` |
| TC-COND-008 | `GET .../renewals` lists own submissions, most-recent-first, correct `reviewStatus` | `ExpertRenewalServiceImpl.listMyRenewals()` | `REN-TC-011` |
| TC-COND-009 | Admin APPROVE decision on `PENDING` credential | `ExpertCredentialReviewServiceImpl.decide()` | `REN-TC-012` |
| TC-COND-010 | Admin REJECT decision requires note | `ExpertCredentialReviewPolicy.assertNoteRequiredIfNeeded()` | `REN-TC-013` |
| TC-COND-011 | Admin REQUEST_MORE_INFO maps to `review_status='PENDING'` with note | `ExpertCredentialReviewPolicy.assertTransitionAllowed()` | `REN-TC-014` |
| TC-COND-012 | Re-deciding an already-decided credential rejected | `ExpertCredentialReviewPolicy.assertTransitionAllowed()` | `REN-TC-015` |
| TC-COND-013 | Unknown `credentialId` on review decision | `ExpertCredentialReviewServiceImpl.decide()` | `REN-TC-016` |
| TC-COND-014 | **CRITICAL** — Admin APPROVE on a `LOCKED` expert's renewal does NOT change `expert_profiles.verification_status` | `ExpertCredentialReviewServiceImpl.decide()` (structural + integration) | `REN-TC-017`, `REN-TC-INT-002` |
| TC-COND-015 | Role-based access on submission endpoints (EXPERT vs others) | `ExpertRenewalController` + Spring Security | `REN-TC-018` to `REN-TC-021` |
| TC-COND-016 | Role-based access on review endpoints (SYSTEM_ADMIN vs others) | `ExpertCredentialReviewController` + Spring Security | `REN-TC-022` to `REN-TC-024` |
| TC-COND-017 | Cross-account submission structurally impossible (`/me` scoping) | `ExpertRenewalController` | `REN-TC-025` |
| TC-COND-018 | Audit + domain events emitted on submit/approve/reject | `ExpertRenewalServiceImpl`/`ExpertCredentialReviewServiceImpl` | `REN-TC-026`, `REN-TC-027`, `REN-TC-028` |
| TC-COND-019 | Enum integrity — `ExpertVerificationStatus` still exactly 6 values (no 7th) after this UC's implementation | `ExpertVerificationStatus` | `REN-TC-029` |
| TC-COND-020 | Structural guard — no `@Scheduled` job, no `ExpertVerificationExpired` class/import exists (OI-4 boundary) | `ExpertRenewalServiceImpl`/`ExpertCredentialReviewServiceImpl` (structural) | `REN-TC-030` |
| TC-COND-021 | Web: submit form validation + submission | `ExpertRenewalSubmitPage.tsx` | `REN-TC-WEB-001`, `REN-TC-WEB-002` |
| TC-COND-022 | Web: history page renders `reviewStatus` per submission | `ExpertRenewalHistoryPage.tsx` | `REN-TC-WEB-003` |
| TC-COND-023 | Full integration: `LOCKED` expert submits → Admin approves → profile still `LOCKED` → separate UC-104 REINSTATE → `VERIFIED` | `ExpertRenewalController` + `ExpertCredentialReviewController` + UC-104 `ExpertBadgeController` E2E | `REN-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `expert_profiles.verification_status` (eligible: `VERIFIED`/`LOCKED`; ineligible: `PENDING`/`REJECTED`/`NEEDS_MORE_INFO`/`REVOKED`) | Core RG-3 resolution coverage |
| Boundary Value Analysis | `review_status` transition (`PENDING` → terminal; already-terminal → reject) | Prevents double-decision on the same credential |
| State Transition Testing | `expert_credentials.review_status` FSM (3 practical values) | Confirms ADR-REN-503's `REQUEST_MORE_INFO` → `PENDING` mapping is deliberate, not a bug |
| Error Guessing | LOCKED-expert-approved-but-not-reinstated, cross-account submission, self-approval-of-verification_status | Security/authority-boundary assurance specific to UC-173's write-scope constraint |
| Negative/Structural Testing | Confirm `expertProfileRepository.save()` never called by this UC's services; confirm no `@Scheduled`/`ExpertVerificationExpired` exists | Enforces ADR-REN-502/503 and OI-4 boundary at the architecture level |
| Regression Testing | Reuse (not duplicate) `ExpertGovernanceTestFactory` from UC-103/104/172 | Confirms Props Isolation Pattern compliance across sibling UCs |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-REN-001` | DB seed | REUSED `FX-EXPB-001`/`FX-SUSP-001` — `expert_profiles` row, `verification_status='VERIFIED'` | Happy path submission from VERIFIED |
| `FX-REN-002` | DB seed | REUSED `FX-EXPB-002`/`FX-SUSP-002` — `expert_profiles` row, `verification_status='LOCKED'` | Happy path submission from LOCKED (RG-3) |
| `FX-REN-003` | DB seed | REUSED `FX-EXPB-003`/`FX-SUSP-003` — `expert_profiles` row, `verification_status='REVOKED'` | Ineligibility test |
| `FX-REN-004` | DB seed | REUSED `FX-EXPB-004`/`FX-SUSP-004` — `expert_profiles` row, `verification_status='PENDING'` | Ineligibility test |
| `FX-REN-005` | DB seed | NEW — `expert_profiles` row, `verification_status='REJECTED'` | Ineligibility test |
| `FX-REN-006` | DB seed | NEW — `expert_profiles` row, `verification_status='NEEDS_MORE_INFO'` | Ineligibility test |
| `FX-REN-007` | DB seed | NEW — `expert_credentials` row, `expert_profile_id=FX-REN-001.id`, `review_status='PENDING'` | Admin review happy path |
| `FX-REN-008` | DB seed | NEW — `expert_credentials` row, `expert_profile_id=FX-REN-002.id` (LOCKED expert), `review_status='PENDING'` | REN-TC-017/REN-TC-INT-002 critical guard |
| `FX-REN-009` | DB seed | NEW — `expert_credentials` row, `review_status='APPROVED'` (already decided) | Re-decision rejection test |
| `FX-REN-010` | JWT | REUSED `FX-EXPB-006`/`FX-SUSP-006` — `{ sub: 'admin-001', role: 'SYSTEM_ADMIN' }` | Auth context for admin review |
| `FX-REN-011` | JWT | REUSED `FX-EXPB-007`/`FX-SUSP-007` — `{ sub: 'expert-001', role: 'EXPERT' }` | Auth context for expert submission |
| `FX-REN-012` | JWT | REUSED `FX-EXPB-008`/`FX-SUSP-008` — `{ sub: 'mod-001', role: 'MODERATOR' }` | Negative auth test (no MODERATOR access either side) |
| `FX-REN-013` | JWT | REUSED `FX-EXPB-009`/`FX-SUSP-009` — `{ sub: 'mother-001', role: 'MOTHER' }` | Negative auth test |

---

## 4. Test Case Specification

> **TC ID format:** `REN-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ExpertGovernanceTestFactory.java — SHARED across UC-103/UC-104/UC-172/UC-173 test suites
// UC-173 EXTENDS the SAME factory (adds new methods only). No new factory class.
// ═══════════════════════════════════════════════════════════

class ExpertGovernanceTestFactory {

    // --- Inherited from UC-103/UC-104/UC-172 (unchanged, reused as-is) ---
    // makeVerifiedProfile(), makeVerifiedProfile(overrides), makePendingProfile(),
    // makeLockedProfile(), makeRevokedProfile(), makeRejectedProfile(),
    // makeNeedsMoreInfoProfile(), makeLockRequest(reason), makeReinstateRequest(reason),
    // ADMIN_USER_ID, EXPERT_USER_ID
    // — see UC104_RevokeExpertBadge_Test-Spec.md / UC172_SuspendExpert_Test-Spec.md §4, NOT reproduced here.

    // --- NEW for UC-173 ---

    static RenewalSubmissionRequest makeRenewalRequest() {
        return makeRenewalRequest("MEDICAL_LICENSE", LocalDate.of(2026, 8, 1));
    }

    static RenewalSubmissionRequest makeRenewalRequest(String credentialType, LocalDate expiryDate) {
        RenewalSubmissionRequest req = new RenewalSubmissionRequest();
        req.setCredentialType(credentialType);
        req.setExpiryDate(expiryDate);
        req.setFileUrl("https://storage.carebridge.dev/test/renewed-credential.pdf");
        return req;
    }

    static ExpertCredential makePendingRenewalCredential(UUID expertProfileId) {
        ExpertCredential c = new ExpertCredential();
        c.setCredentialId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
        c.setExpertProfileId(expertProfileId);
        c.setCredentialType("MEDICAL_LICENSE");
        c.setExpiryDate(LocalDate.of(2026, 8, 1));
        c.setReviewStatus("PENDING");
        c.setCreatedAt(Instant.parse("2026-07-03T10:00:00Z"));
        return c;
    }

    static ExpertCredential makeApprovedRenewalCredential(UUID expertProfileId) {
        ExpertCredential c = makePendingRenewalCredential(expertProfileId);
        c.setReviewStatus("APPROVED");
        return c;
    }

    static RenewalReviewDecisionRequest makeApproveDecision() {
        RenewalReviewDecisionRequest r = new RenewalReviewDecisionRequest();
        r.setDecision(VerificationDecisionAction.APPROVE); // REUSED from UC-103
        return r;
    }

    static RenewalReviewDecisionRequest makeRejectDecision(String note) {
        RenewalReviewDecisionRequest r = new RenewalReviewDecisionRequest();
        r.setDecision(VerificationDecisionAction.REJECT);
        r.setNote(note);
        return r;
    }

    static RenewalReviewDecisionRequest makeRequestMoreInfoDecision(String note) {
        RenewalReviewDecisionRequest r = new RenewalReviewDecisionRequest();
        r.setDecision(VerificationDecisionAction.REQUEST_MORE_INFO);
        r.setNote(note);
        return r;
    }
}
```

---

### REN-TC-001 — Submit renewal from VERIFIED creates a new PENDING expert_credentials row

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertRenewalServiceImpl.submitRenewal()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertRenewalServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-REN-501`, `ADR-REN-502`

**Preconditions:**
- `FX-REN-001` (mocked `expertProfileRepository.findByUserId()` returns `makeVerifiedProfile()`)

**Test Steps:**
1. Act: `service.submitRenewal(EXPERT_USER_ID, makeRenewalRequest())`
2. Assert: `expertCredentialRepository.save(...)` called once with an entity where `reviewStatus == null` (schema default `'PENDING'` applies at DB level, not set by mapper) and `expertProfileId` matches the profile
3. Assert: response `reviewStatus == "PENDING"`

**Expected Result (PASS):** New credential row created, correctly linked.
**Expected Result (FAIL):** No save invoked, or wrong `expertProfileId`.

**Current Status:** 🔴 Not written

---

### REN-TC-002 — Submit renewal from LOCKED is accepted (RG-3 resolution)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertRenewalServiceImpl.submitRenewal()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertRenewalServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-REN-502` (RG-3 resolution — reconciled with UC-172's `LOCKED` state)

**Preconditions:**
- `FX-REN-002` (mocked repo returns `makeLockedProfile()`)

**Test Steps:**
1. Act: `service.submitRenewal(EXPERT_USER_ID, makeRenewalRequest())`
2. Assert: no exception thrown; `expertCredentialRepository.save(...)` invoked

**Expected Result (PASS):** Submission succeeds — proves a suspended (document-expiry) expert has a path to resolve their own suspension trigger.
**Expected Result (FAIL):** `ConflictException` thrown for `LOCKED` — would recreate the dead-end scenario ADR-REN-502 exists to prevent.

**Current Status:** 🔴 Not written
**Implementation Note:** This is UC-173's single most important test — it is the concrete proof of the RG-3 reconciliation decision.

---

### REN-TC-003 — Submit renewal from PENDING rejected (EXPR-102)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertRenewalPolicy.assertEligible()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertRenewalPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-REN-502`

**Test Steps:**
1. Act/Assert: `policy.assertEligible(ExpertVerificationStatus.PENDING)` throws `ConflictException("EXPR-102")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** No exception — would allow UC-89's pre-verification flow to be bypassed via UC-173.

**Current Status:** 🔴 Not written

---

### REN-TC-004 — Submit renewal from REJECTED rejected (EXPR-102)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertRenewalPolicy.assertEligible()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertRenewalPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-REN-502`

**Test Steps:**
1. Act/Assert: `policy.assertEligible(ExpertVerificationStatus.REJECTED)` throws `ConflictException("EXPR-102")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** No exception.

**Current Status:** 🔴 Not written

---

### REN-TC-005 — Submit renewal from NEEDS_MORE_INFO rejected (EXPR-102)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertRenewalPolicy.assertEligible()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertRenewalPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-REN-502`

**Test Steps:**
1. Act/Assert: `policy.assertEligible(ExpertVerificationStatus.NEEDS_MORE_INFO)` throws `ConflictException("EXPR-102")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** No exception.

**Current Status:** 🔴 Not written

---

### REN-TC-006 — Submit renewal from REVOKED rejected (EXPR-102, terminal state)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertRenewalPolicy.assertEligible()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertRenewalPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-REN-502` — `REVOKED` is terminal per UC-104's ADR-EXP-301, no recovery path in this UC

**Test Steps:**
1. Act/Assert: `policy.assertEligible(ExpertVerificationStatus.REVOKED)` throws `ConflictException("EXPR-102")`

**Expected Result (PASS):** Exception thrown — a permanently revoked expert cannot use UC-173 as a backdoor re-application path.
**Expected Result (FAIL):** No exception — CRITICAL, would let a revoked expert re-enter the verification pipeline without a proper new UC.

**Current Status:** 🔴 Not written

---

### REN-TC-007 — Structural guard: submitRenewal() never calls expertProfileRepository.save()

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertRenewalServiceImpl.submitRenewal()` (structural test)
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertRenewalServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-REN-502` explicit non-goal

**Test Steps:**
1. Act: `service.submitRenewal(EXPERT_USER_ID, makeRenewalRequest())` (mocked repo returns `makeVerifiedProfile()`)
2. Assert: `verify(expertProfileRepository, never()).save(any())`

**Expected Result (PASS):** `expertProfileRepository.save()` never invoked.
**Expected Result (FAIL):** `save()` invoked — `AP-AI-010` violation, CRITICAL, must block merge.

**Current Status:** 🔴 Not written

---

### REN-TC-008 — Submit renewal with no expert_profiles row returns 404 (EXPR-104)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertRenewalServiceImpl.submitRenewal()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertRenewalServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `TDS §10 EXPR-104`

**Test Steps:**
1. Arrange: `expertProfileRepository.findByUserId(userId)` returns `Optional.empty()`
2. Act/Assert: `service.submitRenewal(userId, makeRenewalRequest())` throws `NotFoundException("EXPR-104")`

**Expected Result (PASS):** `NotFoundException` thrown; `expertCredentialRepository.save()` never called.
**Expected Result (FAIL):** NPE or wrong error code.

**Current Status:** 🔴 Not written

---

### REN-TC-009 — Missing expiryDate rejected at Bean Validation (EXPR-101)

**Severity:** `MEDIUM`
**Feature Under Test:** `RenewalSubmissionRequest` Bean Validation
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertRenewalControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-REN-501` — `expiryDate` is `@NotNull`

**Test Steps:**
1. Send `POST /api/v1/expert-profiles/me/renewals` with `expiryDate` omitted
2. Assert `400 Bad Request`, `error.code == "EXPR-101"`

**Expected Result (PASS):** `400` with field-level detail.
**Expected Result (FAIL):** `500` or silent acceptance with null expiry.

**Current Status:** 🔴 Not written

---

### REN-TC-010 — Mass-assignment: client-supplied reviewStatus/expertProfileId ignored (structural)

**Severity:** `CRITICAL`
**Feature Under Test:** `RenewalSubmissionRequest` (structural test, mirrors UC-88's EXP-TC-006 pattern)
**Test File:** `src/test/java/com/carebridge/backend/expert/dto/RenewalSubmissionRequestTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-REN-501`, `ADR-REN-504`, `TDS §17.1 C4`

**Test Steps:**
1. Reflect over `RenewalSubmissionRequest.class` declared fields
2. Assert: no field named `reviewStatus`, `expertProfileId`, or `credentialId` exists

**Expected Result (PASS):** Zero matching fields.
**Expected Result (FAIL):** A locked field exists — CRITICAL, self-escalation risk (an expert could set their own credential to `APPROVED`).

**Current Status:** 🔴 Not written

---

### REN-TC-011 — listMyRenewals returns own submissions ordered most-recent-first

**Severity:** `HIGH`
**Feature Under Test:** `ExpertRenewalServiceImpl.listMyRenewals()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertRenewalServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-REN-501` — "tracks renewal results" resolution

**Test Steps:**
1. Arrange: `expertCredentialRepository.findByExpertProfileIdOrderByCreatedAtDesc(id)` returns `[approvedOld, pendingNew]` in that DB order
2. Act: `service.listMyRenewals(EXPERT_USER_ID)`
3. Assert: response list preserves order; each item's `reviewStatus` matches source row

**Expected Result (PASS):** Correct order and status mapping.
**Expected Result (FAIL):** Wrong order or missing `reviewStatus`.

**Current Status:** 🔴 Not written

---

### REN-TC-012 — Admin APPROVE on a PENDING credential transitions to APPROVED

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertCredentialReviewServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertCredentialReviewServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-REN-503`

**Preconditions:** `FX-REN-007` (mocked repo returns `makePendingRenewalCredential(verifiedProfileId)`)

**Test Steps:**
1. Act: `service.decide(credentialId, makeApproveDecision(), ADMIN_USER_ID)`
2. Assert: response `reviewStatus == "APPROVED"`; `save()` called with entity `reviewStatus == "APPROVED"`

**Expected Result (PASS):** Status transitions correctly.
**Expected Result (FAIL):** Wrong/missing transition.

**Current Status:** 🔴 Not written

---

### REN-TC-013 — Admin REJECT without note rejected (EXPR-101)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertCredentialReviewPolicy.assertNoteRequiredIfNeeded()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertCredentialReviewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-REN-503` (inherits UC-103's ADR-EXP-202 conditional-note rule)

**Test Steps:**
1. Act/Assert: `policy.assertNoteRequiredIfNeeded(VerificationDecisionAction.REJECT, "")` throws `ValidationException("EXPR-101")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** No exception — reviewer reason not captured, audit gap.

**Current Status:** 🔴 Not written

---

### REN-TC-014 — Admin REQUEST_MORE_INFO maps to review_status='PENDING' with note (ADR-REN-503 mapping)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertCredentialReviewPolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertCredentialReviewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-REN-503` — no dedicated 4th schema value

**Test Steps:**
1. Act: `policy.assertTransitionAllowed("PENDING", VerificationDecisionAction.REQUEST_MORE_INFO)`
2. Assert: returns `"PENDING"` (stays pending, not a new terminal value)

**Expected Result (PASS):** Returns `"PENDING"` — proves the deliberate schema-constrained mapping, not a missing implementation.
**Expected Result (FAIL):** Returns a non-schema value (e.g., `"NEEDS_MORE_INFO"`) that would violate `varchar(30)` semantic parity with the rest of the module, or throws unexpectedly.

**Current Status:** 🔴 Not written

---

### REN-TC-015 — Re-deciding an already-decided credential rejected (EXPR-103)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertCredentialReviewPolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertCredentialReviewPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §10 EXPR-103`

**Preconditions:** `FX-REN-009` (credential with `review_status='APPROVED'`)

**Test Steps:**
1. Act/Assert: `policy.assertTransitionAllowed("APPROVED", VerificationDecisionAction.APPROVE)` throws `ConflictException("EXPR-103")`

**Expected Result (PASS):** Exception thrown — idempotency/audit-trail integrity preserved.
**Expected Result (FAIL):** Silent re-approval, duplicate audit/event noise.

**Current Status:** 🔴 Not written

---

### REN-TC-016 — Review decision on unknown credentialId returns 404 (EXPR-105)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertCredentialReviewServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertCredentialReviewServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §10 EXPR-105`

**Test Steps:**
1. Arrange: `expertCredentialRepository.findById(unknownId)` returns `Optional.empty()`
2. Act/Assert: `service.decide(unknownId, makeApproveDecision(), ADMIN_USER_ID)` throws `NotFoundException("EXPR-105")`

**Expected Result (PASS):** `NotFoundException("EXPR-105")` thrown.
**Expected Result (FAIL):** NPE or wrong error code.

**Current Status:** 🔴 Not written

---

### REN-TC-017 — CRITICAL: Approving a LOCKED expert's renewal does NOT change expert_profiles.verification_status

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertCredentialReviewServiceImpl.decide()` (structural test)
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertCredentialReviewServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `ADR-REN-503` explicit non-goal; RG-3 two-step recovery design

**Preconditions:** `FX-REN-008` (credential submitted by a `LOCKED` expert, `review_status='PENDING'`)

**Test Steps:**
1. Act: `service.decide(credentialId, makeApproveDecision(), ADMIN_USER_ID)`
2. Assert: `verify(expertProfileRepository, never()).save(any())` — `ExpertCredentialReviewServiceImpl` has no dependency on `IExpertProfileRepository` at all (constructor injection scan)
3. Assert: response confirms `reviewStatus == "APPROVED"` (the credential itself IS approved)

**Expected Result (PASS):** Credential approved; profile untouched — expert remains `LOCKED` until a SEPARATE UC-104 `REINSTATE` call.
**Expected Result (FAIL):** `expert_profiles.verification_status` silently flips to `VERIFIED` — `AP-AI-010` violation, CRITICAL, directly contradicts ADR-SUSP-403/ADR-EXP-303's Admin-only authority over the sanction state machine.

**Current Status:** 🔴 Not written
**Implementation Note:** This is UC-173's single highest-value regression guard, analogous to UC-172's `SUSP-TC-019` (enum integrity) and `SUSP-TC-WEB-003` (no Revoke exposure).

---

### SECURITY TEST CASES

---

### REN-TC-018 — EXPERT can submit a renewal for own profile

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertRenewalController.submitRenewal()` + Spring Security chain
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertRenewalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-REN-011` (EXPERT JWT).

**Test Steps:**
1. Send `POST /api/v1/expert-profiles/me/renewals` with EXPERT JWT and a valid body
2. Assert `201 Created`

**Expected Result (PASS):** `201 Created`.
**Expected Result (FAIL):** `403 Forbidden` incorrectly returned for a valid expert.

**Current Status:** 🔴 Not written

---

### REN-TC-019 — MOTHER role forbidden from submitting a renewal

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertRenewalController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertRenewalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-REN-013` (MOTHER JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../renewals` with MOTHER JWT
2. Assert `403 Forbidden`, body `error.code == "EXPR-106"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden`.
**Expected Result (FAIL = lỗ hổng tồn tại):** `201 Created`.

**Current Status:** 🔴 Not written

---

### REN-TC-020 — SYSTEM_ADMIN (without EXPERT role) forbidden from submitting a renewal on the submission endpoint

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Legal:** `BR-RBAC`, `ADR-REN-504`
**Feature Under Test:** `ExpertRenewalController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertRenewalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §16 Authorization Matrix` — submission is EXPERT-only, even a System Admin cannot submit on an expert's behalf via this endpoint (no admin-override path in this UC)

**Preconditions:** `FX-REN-010` (SYSTEM_ADMIN JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../renewals` with SYSTEM_ADMIN JWT
2. Assert `403 Forbidden`

**Expected Result (PASS):** `403 Forbidden` — confirms no admin-override submission path exists (matches TDS §18 scope boundary).
**Expected Result (FAIL):** `201 Created`.

**Current Status:** 🔴 Not written

---

### REN-TC-021 — Unauthenticated submission request rejected (401)

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertRenewalControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `SRS E1`

**Test Steps (Attack Simulation):**
1. Send `POST .../renewals` with no `Authorization` header
2. Assert `401 Unauthorized`

**Expected Result (PASS):** `401 Unauthorized`.
**Expected Result (FAIL):** Request processed without authentication.

**Current Status:** 🔴 Not written

---

### REN-TC-022 — SYSTEM_ADMIN can decide a renewal review

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertCredentialReviewController.decide()` + Spring Security chain
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertCredentialReviewControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-REN-010` (SYSTEM_ADMIN JWT).

**Test Steps:**
1. Send `POST /api/v1/admin/expert-credentials/{id}/review-decisions` with SYSTEM_ADMIN JWT, `{decision:"APPROVE"}`
2. Assert `200 OK`

**Expected Result (PASS):** `200 OK`.
**Expected Result (FAIL):** `403 Forbidden` incorrectly returned for a valid admin.

**Current Status:** 🔴 Not written

---

### REN-TC-023 — EXPERT role forbidden from reviewing renewals (including own)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertCredentialReviewController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertCredentialReviewControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-REN-011` (EXPERT JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../review-decisions` with EXPERT JWT against the expert's own submitted credential, `{decision:"APPROVE"}`
2. Assert `403 Forbidden`, body `error.code == "EXPR-106"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` — an expert can never self-approve their own renewal.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK`.

**Current Status:** 🔴 Not written

---

### REN-TC-024 — MODERATOR role forbidden from review decision (ADR-REN-503/504)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`, `ADR-REN-503`
**Feature Under Test:** `ExpertCredentialReviewController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertCredentialReviewControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `SRS §3.2.4.2 Primary Actor field ("Verified Expert", Secondary Actors: "None")`, consistent with UC-103's Admin-only review precedent

**Preconditions:** `FX-REN-012` (MODERATOR JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../review-decisions` with MODERATOR JWT, `{decision:"APPROVE"}`
2. Assert `403 Forbidden`

**Expected Result (PASS):** `403 Forbidden`.
**Expected Result (FAIL):** `200 OK` — privilege scope creep beyond SRS.

**Current Status:** 🔴 Not written

---

### REN-TC-025 — Cross-account submission structurally impossible (/me scoping)

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `ExpertRenewalController` (design/structural test)
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertRenewalControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `ADR-REN-504`, mirrors UC-88 §6.4 design note

**Test Steps:**
1. Inspect `@RequestMapping`/`@PostMapping` annotations on `ExpertRenewalController`
2. Assert: no `{id}`/`{expertProfileId}` path variable exists on the submission or list endpoints — both resolve `userId` exclusively via `SecurityUtils.requireCurrentUserId(principal)`

**Expected Result (PASS):** No path parameter allowing another expert's profile to be targeted.
**Expected Result (FAIL):** A path variable exists, opening a cross-account write vector.

**Current Status:** 🔴 Not written

---

### REN-TC-026 — Successful submission emits audit log and ExpertVerificationRenewalSubmitted event

**Severity:** `CRITICAL`
**Legal:** `PDPA`
**Feature Under Test:** `ExpertRenewalServiceImpl.submitRenewal()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertRenewalServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `ADR-REN-501`, `TDS §7.1`

**Test Steps:**
1. Act: `service.submitRenewal(EXPERT_USER_ID, makeRenewalRequest())`
2. Assert: `verify(auditService).log(eq(AuditAction.EXPERT_VERIFICATION), eq(EXPERT_USER_ID), eq("expert_credentials"), any(), any())`
3. Assert: `verify(applicationEventPublisher).publishEvent(argThat(e -> e instanceof ExpertVerificationRenewalSubmitted))`

**Expected Result (PASS):** Audit log + event fire exactly once.
**Expected Result (FAIL):** Missing audit call or wrong event type.

**Current Status:** 🔴 Not written

---

### REN-TC-027 — Successful APPROVE emits ExpertVerificationRenewed event

**Severity:** `HIGH`
**Legal:** `PDPA`
**Feature Under Test:** `ExpertCredentialReviewServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertCredentialReviewServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `TDS §7.1`

**Test Steps:**
1. Act: `service.decide(credentialId, makeApproveDecision(), ADMIN_USER_ID)`
2. Assert: `publishEvent(argThat(e -> e instanceof ExpertVerificationRenewed))`

**Expected Result (PASS):** `ExpertVerificationRenewed` event published.
**Expected Result (FAIL):** Wrong/missing event type.

**Current Status:** 🔴 Not written

---

### REN-TC-028 — Successful REJECT emits ExpertVerificationRenewalRejected event

**Severity:** `HIGH`
**Legal:** `PDPA`
**Feature Under Test:** `ExpertCredentialReviewServiceImpl.decide()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertCredentialReviewServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `TDS §7.1`

**Test Steps:**
1. Act: `service.decide(credentialId, makeRejectDecision("Ảnh mờ"), ADMIN_USER_ID)`
2. Assert: `publishEvent(argThat(e -> e instanceof ExpertVerificationRenewalRejected))`

**Expected Result (PASS):** `ExpertVerificationRenewalRejected` event published.
**Expected Result (FAIL):** Wrong/missing event type, or `ExpertVerificationRenewed` incorrectly published for a REJECT decision.

**Current Status:** 🔴 Not written

---

### REN-TC-029 — ExpertVerificationStatus enum integrity: exactly 6 values, unchanged by this UC

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertVerificationStatus` enum (structural test)
**Test File:** `src/test/java/com/carebridge/backend/expert/entity/ExpertVerificationStatusTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `TDS §17.1 C9` — this UC must not add/remove/rename any value

**Test Steps:**
1. Act: `ExpertVerificationStatus.values()`
2. Assert: array has EXACTLY 6 elements: `PENDING`, `VERIFIED`, `REJECTED`, `NEEDS_MORE_INFO`, `LOCKED`, `REVOKED` (order-independent set comparison)

**Expected Result (PASS):** Exactly the 6 UC-103/UC-104/UC-172-defined values.
**Expected Result (FAIL):** A 7th value exists or a value was renamed — CRITICAL, must block merge. This test may already exist from UC-172 (`SUSP-TC-019`) — if so, this UC's version asserts the SAME invariant still holds after UC-173's own changes (regression net, not a duplicate obligation).

**Current Status:** 🔴 Not written

---

### REN-TC-030 — Structural guard: no scheduled job or ExpertVerificationExpired class exists (OI-4 boundary)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertRenewalServiceImpl`, `ExpertCredentialReviewServiceImpl` (structural test)
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertRenewalScopeGuardTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-020`
**Oracle Source:** `TDS §7.1 OI-4`, `§17.1 C8`

**Test Steps:**
1. Reflect over `ExpertRenewalServiceImpl.class` and `ExpertCredentialReviewServiceImpl.class` declared methods
2. Assert: no method is annotated `@Scheduled`
3. Assert: no class named `ExpertVerificationExpired` exists anywhere under `com.carebridge.backend.expert.event` (compile-time absence; a `ClassNotFoundException` on lookup is the expected/passing outcome)

**Expected Result (PASS):** No scheduled job, no `ExpertVerificationExpired` class — confirms this UC stayed within its expert-initiated (not system-initiated) scope.
**Expected Result (FAIL):** A `@Scheduled` method or `ExpertVerificationExpired` class exists without a Tech-Lead-approved follow-up ADR — `AP-AI-011` violation.

**Current Status:** 🔴 Not written

---

### WEB TEST CASES (Vitest + Testing Library)

---

### REN-TC-WEB-001 — ExpertRenewalSubmitPage rejects submission with missing expiryDate

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertRenewalSubmitPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/expert/pages/__tests__/ExpertRenewalSubmitPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `TDS §11.3 Chặng 4`, Zod schema mirroring §8.1

**Test Steps:**
1. Render `ExpertRenewalSubmitPage`, fill `credentialType` only, submit
2. Assert: inline validation error for `expiryDate`; `apiClient.post` NOT called

**Expected Result (PASS):** Client-side validation blocks submission.
**Expected Result (FAIL):** Form submits with missing required field.

**Current Status:** 🔴 Not written

---

### REN-TC-WEB-002 — ExpertRenewalSubmitPage submits valid form and shows PENDING confirmation

**Severity:** `HIGH`
**Feature Under Test:** `ExpertRenewalSubmitPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/expert/pages/__tests__/ExpertRenewalSubmitPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `TDS §9.2` response shape

**Test Steps:**
1. Render page, fill all required fields, submit
2. Mock `submitRenewal` API resolves with `{reviewStatus: "PENDING", ...}`
3. Assert: confirmation banner/toast shows "pending review" state

**Expected Result (PASS):** Correct success state rendered.
**Expected Result (FAIL):** Wrong/missing confirmation.

**Current Status:** 🔴 Not written

---

### REN-TC-WEB-003 — ExpertRenewalHistoryPage renders reviewStatus per submission

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertRenewalHistoryPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/expert/pages/__tests__/ExpertRenewalHistoryPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-022`
**Oracle Source:** `TDS §9.2 GET .../renewals` response shape

**Test Steps:**
1. Mock `getMyRenewals` API resolves with 2 items (`PENDING`, `APPROVED`)
2. Render `ExpertRenewalHistoryPage`
3. Assert: both `reviewStatus` values rendered as distinguishable chips/labels ("tracks renewal results" SRS requirement)

**Expected Result (PASS):** Both statuses visibly distinguishable.
**Expected Result (FAIL):** Missing/incorrect status display.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### REN-TC-INT-001 — Full flow: LOCKED expert submits → Admin approves → still LOCKED → UC-104 REINSTATE → VERIFIED

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow: `ExpertRenewalController` → `ExpertCredentialReviewController` → UC-104 `ExpertBadgeController` → PostgreSQL (Testcontainers)
**Test File:** `src/test/java/com/carebridge/backend/expert/RenewExpertVerificationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-023`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically (no new migration for this UC — regression check)
- Seed: 1x `expert_profiles` row `verification_status='LOCKED'` (`FX-REN-002`)

**Test Steps:**
1. `POST /api/v1/expert-profiles/me/renewals` with EXPERT JWT (owner of the LOCKED profile), valid body
2. Assert `201 Created`, `reviewStatus == "PENDING"`
3. `POST /api/v1/admin/expert-credentials/{credentialId}/review-decisions` with SYSTEM_ADMIN JWT, `{decision:"APPROVE"}`
4. Assert `200 OK`, `reviewStatus == "APPROVED"`
5. Assert DB: `expert_profiles.verification_status` is STILL `'LOCKED'` (proves ADR-REN-503's explicit non-goal end-to-end)
6. `POST /api/v1/admin/expert-profiles/{expertProfileId}/badge-actions` with SYSTEM_ADMIN JWT, `{action:"REINSTATE", reason:"Renewed documents approved"}` (UC-104's EXISTING, unmodified endpoint)
7. Assert `200 OK`, `verificationStatus == "VERIFIED"`

**Expected Result (PASS):** Two-step recovery flow works end-to-end via the real HTTP + DB stack; UC-173 never independently reinstates.
**Expected Result (FAIL):** Any step fails, OR step 5 finds `verification_status` already `VERIFIED` (would mean UC-173 illegally auto-reinstated).

**DB Assertion:**
```java
ExpertProfile afterApprove = expertProfileRepository.findById(profileId).orElseThrow();
assertThat(afterApprove.getVerificationStatus()).isEqualTo(ExpertVerificationStatus.LOCKED); // step 5

ExpertProfile afterReinstate = expertProfileRepository.findById(profileId).orElseThrow();
assertThat(afterReinstate.getVerificationStatus()).isEqualTo(ExpertVerificationStatus.VERIFIED); // step 7
```

**Current Status:** 🔴 Not written

---

### REN-TC-INT-002 — Integration: expert_credentials row persists correctly with real DB constraints

**Severity:** `HIGH`
**Feature Under Test:** `ExpertRenewalController` → `ExpertRenewalServiceImpl` → PostgreSQL (Testcontainers)
**Test File:** `src/test/java/com/carebridge/backend/expert/RenewExpertVerificationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:**
- Seed: 1x `expert_profiles` row `verification_status='VERIFIED'` (`FX-REN-001`)

**Test Steps:**
1. `POST /api/v1/expert-profiles/me/renewals` with valid body
2. Assert `201 Created`
3. `GET /api/v1/expert-profiles/me/renewals` — assert the new row appears with `reviewStatus == "PENDING"`
4. Assert DB: `expert_credentials` row has correct `expert_profile_id` FK, `credential_id` PK auto-generated, `created_at`/`updated_at` populated by schema defaults

**Expected Result (PASS):** Row persists with correct FK linkage and schema defaults.
**Expected Result (FAIL):** FK violation, missing defaults, or wrong `expert_profile_id`.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `REN-TC-001` | `ExpertRenewalServiceImplTest.java` | `[ ]` | | |
| `REN-TC-002` | `ExpertRenewalServiceImplTest.java` | `[ ]` | | |
| `REN-TC-003` | `ExpertRenewalPolicyTest.java` | `[ ]` | | |
| `REN-TC-004` | `ExpertRenewalPolicyTest.java` | `[ ]` | | |
| `REN-TC-005` | `ExpertRenewalPolicyTest.java` | `[ ]` | | |
| `REN-TC-006` | `ExpertRenewalPolicyTest.java` | `[ ]` | | |
| `REN-TC-007` | `ExpertRenewalServiceImplTest.java` | `[ ]` | | |
| `REN-TC-008` | `ExpertRenewalServiceImplTest.java` | `[ ]` | | |
| `REN-TC-009` | `ExpertRenewalControllerTest.java` | `[ ]` | | |
| `REN-TC-010` | `RenewalSubmissionRequestTest.java` | `[ ]` | | |
| `REN-TC-011` | `ExpertRenewalServiceImplTest.java` | `[ ]` | | |
| `REN-TC-012` | `ExpertCredentialReviewServiceImplTest.java` | `[ ]` | | |
| `REN-TC-013` | `ExpertCredentialReviewPolicyTest.java` | `[ ]` | | |
| `REN-TC-014` | `ExpertCredentialReviewPolicyTest.java` | `[ ]` | | |
| `REN-TC-015` | `ExpertCredentialReviewPolicyTest.java` | `[ ]` | | |
| `REN-TC-016` | `ExpertCredentialReviewServiceImplTest.java` | `[ ]` | | |
| `REN-TC-017` | `ExpertCredentialReviewServiceImplTest.java` | `[ ]` | | |
| `REN-TC-018` | `ExpertRenewalControllerSecurityTest.java` | `[ ]` | | |
| `REN-TC-019` | `ExpertRenewalControllerSecurityTest.java` | `[ ]` | | |
| `REN-TC-020` | `ExpertRenewalControllerSecurityTest.java` | `[ ]` | | |
| `REN-TC-021` | `ExpertRenewalControllerSecurityTest.java` | `[ ]` | | |
| `REN-TC-022` | `ExpertCredentialReviewControllerSecurityTest.java` | `[ ]` | | |
| `REN-TC-023` | `ExpertCredentialReviewControllerSecurityTest.java` | `[ ]` | | |
| `REN-TC-024` | `ExpertCredentialReviewControllerSecurityTest.java` | `[ ]` | | |
| `REN-TC-025` | `ExpertRenewalControllerTest.java` | `[ ]` | | |
| `REN-TC-026` | `ExpertRenewalServiceImplTest.java` | `[ ]` | | |
| `REN-TC-027` | `ExpertCredentialReviewServiceImplTest.java` | `[ ]` | | |
| `REN-TC-028` | `ExpertCredentialReviewServiceImplTest.java` | `[ ]` | | |
| `REN-TC-029` | `ExpertVerificationStatusTest.java` | `[ ]` | | |
| `REN-TC-030` | `ExpertRenewalScopeGuardTest.java` | `[ ]` | | |
| `REN-TC-WEB-001` | `ExpertRenewalSubmitPage.test.tsx` | `[ ]` | | |
| `REN-TC-WEB-002` | `ExpertRenewalSubmitPage.test.tsx` | `[ ]` | | |
| `REN-TC-WEB-003` | `ExpertRenewalHistoryPage.test.tsx` | `[ ]` | | |
| `REN-TC-INT-001` | `RenewExpertVerificationIntegrationTest.java` | `[ ]` | | |
| `REN-TC-INT-002` | `RenewExpertVerificationIntegrationTest.java` | `[ ]` | | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// ExpertRenewalServiceImpl.java — Red Phase stub
@Service
public class ExpertRenewalServiceImpl implements IExpertRenewalService {
    @Override
    public RenewalSubmissionResponse submitRenewal(UUID userId, RenewalSubmissionRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public List<RenewalListItemResponse> listMyRenewals(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

// ExpertCredentialReviewServiceImpl.java — Red Phase stub
@Service
public class ExpertCredentialReviewServiceImpl implements IExpertCredentialReviewService {
    @Override
    public Page<RenewalReviewQueueItemResponse> listQueue(String status, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public RenewalListItemResponse decide(UUID credentialId, RenewalReviewDecisionRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```tsx
// ExpertRenewalSubmitPage.tsx / ExpertRenewalHistoryPage.tsx — Red Phase stubs
export function ExpertRenewalSubmitPage() {
  throw new Error("Not implemented — Red Phase stub");
}
export function ExpertRenewalHistoryPage() {
  throw new Error("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `REN-TC-001` to `REN-TC-017`, `REN-TC-026` to `REN-TC-030` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `REN-TC-018` to `REN-TC-025` | `throw('Not implemented')` propagates as 500, not the expected 401/403/201 | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Mock misconfigured ☐ Security filter bypassed stub |
| `REN-TC-WEB-001` to `REN-TC-WEB-003` | `throw("Not implemented")` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `REN-TC-INT-001`, `REN-TC-INT-002` | Exercises stub via real HTTP — `500` responses | 🔴 FAIL (expects `201`/`200`) | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-EXPGOV-IMP-173` đã được review và approve
- [ ] UC-103's `VerificationDecisionAction` enum, UC-104's `REINSTATE` action, UC-172's `LOCKED` state machine đã tồn tại và tests đã xanh (dependencies for ADR-REN-502/503 reuse)
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị (mostly reused from UC-103/104/172's fixtures)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit/policy/security tests xanh, bao gồm `REN-TC-001` đến `REN-TC-030`
- [ ] `./mvnw verify` — `REN-TC-INT-001`, `REN-TC-INT-002` xanh (Testcontainers)
- [ ] `npm run test:run` — `REN-TC-WEB-001` đến `REN-TC-WEB-003` xanh
- [ ] Không có business logic trong Controller (chỉ validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] **`REN-TC-002` xanh** — LOCKED expert CAN submit a renewal (RG-3 resolution proof)
- [ ] **`REN-TC-017` xanh** — Approving a LOCKED expert's renewal does NOT change `verification_status` (highest-value regression guard)
- [ ] **`REN-TC-029` xanh** — `ExpertVerificationStatus` vẫn đúng 6 giá trị
- [ ] **`REN-TC-030` xanh** — no scheduled job / `ExpertVerificationExpired` exists (OI-4 boundary respected)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — test data tạo qua `ExpertGovernanceTestFactory`, không có shared mutable state
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR-REN-50x)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-103's `VerificationDecisionAction` hoặc UC-104's `REINSTATE` chưa được implement (blocker dependency)
- Phát hiện lỗi kiến trúc mới cần Principal Architect review (vd: OI-2 two-step recovery UX decision chưa resolve)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# No migration to revert (zero schema changes). Revert implementation files:
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/service/impl/ExpertRenewalServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/service/impl/ExpertCredentialReviewServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertRenewalController.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertCredentialReviewController.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/policy/ExpertRenewalPolicy.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/policy/ExpertCredentialReviewPolicy.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/
git checkout -- 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertRenewalSubmitPage.tsx
git checkout -- 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertRenewalHistoryPage.tsx
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/ExpertRenewalReviewQueuePage.tsx

# Gap vẫn OPEN → giữ nguyên entry trong tracking backlog
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `RenewalDecisionAction`/`ExpertVerificationExpired` hoặc type không tồn tại trong codebase | ☐ | G-3 |
| AP-AI-010 *(custom, inherited from TDS §17.4)* | Unauthorized State-Machine Write | `REN-TC-017`/`REN-TC-007` fail because a service wrote `expert_profiles.verification_status` | ☐ | G-2 ★★ |
| AP-AI-011 *(custom, inherited from TDS §17.4)* | Unscoped Automation | `REN-TC-030` fails because a `@Scheduled` job or `ExpertVerificationExpired` class was added | ☐ | G-1 ★★ |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | [mô tả issue] | [hành động fix] | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — pending Tech Lead / TV4-Lâm review, đặc biệt §5.1 Red Gate, `REN-TC-002`/`REN-TC-017` (RG-3 two-step recovery proof), và §18 (TDS) Open Items (OI-1 through OI-6).*
