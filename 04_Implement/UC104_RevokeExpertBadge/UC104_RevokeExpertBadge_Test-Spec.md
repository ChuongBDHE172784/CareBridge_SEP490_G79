# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-104 Revoke Expert Badge — Test Specification

**Document ID:** `CB-EXPGOV-TDD-104`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (lines 786-800, 876-967, 923-955)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.2.6 (UC-104, lines 1113-1132)
- `04_Implement/UC104_RevokeExpertBadge/UC104_RevokeExpertBadge_TDS.md` (CB-EXPGOV-IMP-104) — Technical Specification
- `04_Implement/UC103_VerifyExpertProfile/UC103_VerifyExpertProfile_TDS.md` — shared `ExpertVerificationStatus` enum source
- `04_Implement/UC103_VerifyExpertProfile/UC103_VerifyExpertProfile_Test-Spec.md` — sibling Test-Spec, style/structure and `ExpertGovernanceTestFactory` reference
- `CLAUDE.md` — BR-RBAC, audit requirements for expert/moderation/safety workflows

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`/`.test.tsx`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo Test-Spec cho UC-104, tái sử dụng `ExpertGovernanceTestFactory` (extend) từ UC-103 Test-Spec |

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
| **Feature / Gap ID** | `UC-104` |
| **Module** | `RevokeExpertBadge — Bounded Context: expert` |
| **Spec gốc** | `CB-EXPGOV-IMP-104` |
| **Priority** | 🔴 P0 (High per SRS) |
| **Sprint** | `S3 Cross-Domain Integration — TV4-Lâm` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | UC-103 Verify Expert Profile (`ExpertVerificationStatus` enum + `expert_profiles` rows reaching `VERIFIED`) |
| **Downstream Consumers** | UC-80/81 View Expert Directory/Profile (informational, out of scope), UC-75 Book Private Consultation (informational, out of scope), UC-112 View Expert Dashboard |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXPGOV-IMP-104 §17`, ADR-EXP-301/302/303/304 |
| **Constraints Injected** | C1-C9 per TDS §17.1 (JWT-derived adminUserId, SYSTEM_ADMIN-only, SHARED 6-value `ExpertVerificationStatus` enum, policy-centralized 4-edge transition table, reason required on ALL 3 actions, zero read/write on `consultation_bookings`/`expert_reviews`/`payment_transactions`/`commission_records`, `verified_at`/`verified_by` untouched, reuse existing `AuditAction.EXPERT_VERIFICATION`, no new migration) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS description names two outcomes — "temporarily locks **or** revokes" — with no explicit target-state naming | TDS §3 ADR-EXP-301 derives exactly two sanction states (`LOCKED` reversible, `REVOKED` terminal) plus a `REINSTATE` reversal action, giving 3 total `BadgeAction` values | Tests assert exactly `LOCK`/`REVOKE`/`REINSTATE` are the only accepted actions; no 4th action (e.g., a "suspend" synonym) exists |
| L2 | `expert_profiles.verification_status varchar(30)` has no DB CHECK constraint (confirmed `V1__init_schema.sql` lines 786-800, 1799-1809) — nothing in the schema prevents an invalid transition string from being written | Enum + transition enforcement is application-layer only via `ExpertBadgePolicy.TRANSITIONS` map | Tests assert invalid transitions are rejected at the SERVICE/POLICY layer (`EXPB-102`), never relying on a DB constraint that does not exist |
| L3 | SRS UC-104 Postconditions use the generic POST-2 template ("related records/statuses/notifications updated when applicable") — does NOT explicitly say whether past `consultation_bookings`/`expert_reviews` are altered | TDS §ADR-EXP-304 explicitly decides **prospective-only**: `ExpertBadgeServiceImpl.applyAction()` writes ONLY `expert_profiles`, never `consultation_bookings`/`consultation_sessions`/`expert_reviews`/`payment_transactions`/`commission_records` | Dedicated non-retroactivity test suite: unit test via `verifyNoInteractions()`/no-injection structural check, plus an integration test asserting seeded past rows are byte-for-byte unchanged after a REVOKE |
| L4 | UC-103's `ExpertVerificationStatus` enum only has 4 values in that TDS's original class definition — UC-104 must extend, not redefine | TDS §5.1/§8.1 confirms `ExpertVerificationStatus` is a SINGLE shared enum class (package `com.carebridge.backend.expert.entity`) now carrying 6 values total (`PENDING`, `VERIFIED`, `REJECTED`, `NEEDS_MORE_INFO`, `LOCKED`, `REVOKED`) | Tests assert no second/parallel enum or status column exists; `ExpertGovernanceTestFactory` (shared with UC-103) is extended, not duplicated, with `makeLockedProfile()`/`makeRevokedProfile()` |
| L5 | ADR-EXP-302 requires `reason` for ALL 3 actions, including `REINSTATE` — stricter than UC-103's conditional `note` requirement (only REJECT/REQUEST_MORE_INFO) | TDS §8.3 `ExpertBadgePolicy.assertReasonRequired()` has no action-conditional branch — it is unconditionally required | Tests explicitly cover the REINSTATE-without-reason case (`EXPB-TC-004`), which has no UC-103 analogue, to avoid silently copying UC-103's conditional pattern |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
RevokeExpertBadge bao gồm các layer:
├── Domain (ExpertBadgePolicy — pure logic, no deps)
├── Services (ExpertBadgeServiceImpl — mock JPA Repository + AuditService + ApplicationEventPublisher với Mockito)
├── Controller (ExpertBadgeController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL với @SpringBootTest)
└── Web (ExpertBadgeActionPage / badge-action panel — Vitest + Testing Library, mock API client)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-104` (§3.2.2.6, lines 1113-1132) | Lock/Revoke sanction actions; PRE-3/PRE-4 auth + existing-record guard; POST-2/POST-3 status update + audit; E1/E2 exceptions |
| `ADR-EXP-301` | 4-edge state machine (`VERIFIED→LOCKED`, `VERIFIED→REVOKED`, `LOCKED→VERIFIED`, `LOCKED→REVOKED`); `REVOKED` terminal; rejection of any non-listed transition |
| `ADR-EXP-302` | `reason` mandatory for ALL 3 actions; audit log + domain event on every action; `verified_at`/`verified_by` NOT touched |
| `ADR-EXP-303` | SYSTEM_ADMIN-only authorization; no MODERATOR access |
| `ADR-EXP-304` | Prospective-only — zero reads/writes against `consultation_bookings`, `consultation_sessions`, `expert_reviews`, `payment_transactions`, `commission_records` |
| `BR-RBAC` | Role-scoped access enforcement |
| `V1__init_schema.sql` (lines 786-800, 876-967, 923-955) | Real column names/types for persistence assertions; confirms tables that must remain untouched |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Admin locks a VERIFIED profile (temporary) | `ExpertBadgeServiceImpl.applyAction()` | `EXPB-TC-001` |
| TC-COND-002 | Admin revokes a VERIFIED profile (permanent) | `ExpertBadgeServiceImpl.applyAction()` | `EXPB-TC-002` |
| TC-COND-003 | Admin revokes a LOCKED profile (escalation) | `ExpertBadgeServiceImpl.applyAction()` | `EXPB-TC-003` |
| TC-COND-004 | Admin reinstates a LOCKED profile back to VERIFIED | `ExpertBadgeServiceImpl.applyAction()` | `EXPB-TC-004` |
| TC-COND-005 | Reason missing/blank on any of the 3 actions | `ExpertBadgePolicy.assertReasonRequired()` | `EXPB-TC-005`, `EXPB-TC-006`, `EXPB-TC-007` |
| TC-COND-006 | Invalid transition — REINSTATE on REVOKED (terminal) | `ExpertBadgePolicy.assertTransitionAllowed()` | `EXPB-TC-008` |
| TC-COND-007 | Invalid transition — LOCK/REVOKE on PENDING/REJECTED/NEEDS_MORE_INFO (UC-103-exclusive states) | `ExpertBadgePolicy.assertTransitionAllowed()` | `EXPB-TC-009`, `EXPB-TC-010`, `EXPB-TC-011` |
| TC-COND-008 | Unknown expertProfileId | `ExpertBadgeServiceImpl.applyAction()`/`getBadgeStatus()` | `EXPB-TC-012` |
| TC-COND-009 | Role-based access (SYSTEM_ADMIN vs others) | `ExpertBadgeController` + Spring Security | `EXPB-TC-013` to `EXPB-TC-017` |
| TC-COND-010 | Audit + domain event emitted on every successful action | `ExpertBadgeServiceImpl.applyAction()` | `EXPB-TC-018`, `EXPB-TC-019` |
| TC-COND-011 | Non-retroactivity — no read/write on consultation/review/payment tables | `ExpertBadgeServiceImpl.applyAction()` (structural) | `EXPB-TC-020` |
| TC-COND-012 | `verified_at`/`verified_by` unchanged across a badge action | `ExpertBadgeServiceImpl.applyAction()` | `EXPB-TC-021` |
| TC-COND-013 | Boundary: `reason` length (0, 1, 2000, 2001 chars) | `BadgeActionRequest` Bean Validation + `ExpertBadgePolicy` | `EXPB-TC-022`, `EXPB-TC-023` |
| TC-COND-014 | Full lifecycle escalation/de-escalation state walk | `ExpertBadgePolicy` + `ExpertBadgeServiceImpl` | `EXPB-TC-024` |
| TC-COND-015 | Web: badge-action buttons hidden/shown per current status | `ExpertBadgeActionPage.tsx` | `EXPB-TC-WEB-001`, `EXPB-TC-WEB-002` |
| TC-COND-016 | Full integration: VERIFIED → LOCKED via real API + DB, past records untouched | `ExpertBadgeController` E2E | `EXPB-TC-INT-001`, `EXPB-TC-INT-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `action` enum values (valid `LOCK`/`REVOKE`/`REINSTATE` vs invalid strings) | Confirms only the 3 documented actions are accepted |
| Boundary Value Analysis | `reason` length (0, 1, 2000, 2001 chars) | Confirms `@Size(max=2000)` + `@NotBlank` boundary (TDS §8.1) |
| State Transition Testing | `ExpertVerificationStatus` FSM, UC-104 slice (§ADR-EXP-301) | Core of this UC — every edge (4) and a representative sample of non-edges must be tested |
| Error Guessing | Role bypass attempts, re-applying an already-terminal action, escalation-then-reinstate misuse | Security/idempotency assurance |
| Negative/Structural Testing | Mockito `verifyNoInteractions()` on out-of-scope repositories never injected | Enforces ADR-EXP-304 at the architecture level, not just behaviorally |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-EXPB-001` | DB seed | `expert_profiles` row with `verification_status='VERIFIED'` | Happy path LOCK/REVOKE from VERIFIED |
| `FX-EXPB-002` | DB seed | `expert_profiles` row with `verification_status='LOCKED'` | Happy path REINSTATE/escalation-REVOKE from LOCKED |
| `FX-EXPB-003` | DB seed | `expert_profiles` row with `verification_status='REVOKED'` | Invalid-transition test (terminal state, no REINSTATE) |
| `FX-EXPB-004` | DB seed | `expert_profiles` row with `verification_status='PENDING'` | Invalid-transition test (UC-103-exclusive state, no badge action edge) |
| `FX-EXPB-005` | DB seed | 1x `consultation_bookings` row (`status='COMPLETED'`) + 1x `expert_reviews` row, both linked to `FX-EXPB-001`'s `expert_profile_id` | ADR-EXP-304 non-retroactivity integration assertion — snapshot before/after |
| `FX-EXPB-006` | JWT | `{ sub: 'admin-001', role: 'SYSTEM_ADMIN' }` | Auth context for admin actions |
| `FX-EXPB-007` | JWT | `{ sub: 'expert-001', role: 'EXPERT' }` | Negative auth test |
| `FX-EXPB-008` | JWT | `{ sub: 'mod-001', role: 'MODERATOR' }` | Negative auth test (no MODERATOR access, ADR-EXP-303) |
| `FX-EXPB-009` | JWT | `{ sub: 'mother-001', role: 'MOTHER' }` | Negative auth test |

---

## 4. Test Case Specification

> **TC ID format:** `EXPB-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ExpertGovernanceTestFactory.java — SHARED across UC-103/UC-104 test suites
// UC-104 EXTENDS the UC-103 factory with LOCKED/REVOKED profile builders
// and BadgeActionRequest builders. Do NOT create a second competing factory.
// ═══════════════════════════════════════════════════════════

class ExpertGovernanceTestFactory {

    // --- Inherited from UC-103 (unchanged, reused as-is) ---

    static ExpertProfile makeVerifiedProfile() {
        return makeVerifiedProfile(p -> {});
    }

    static ExpertProfile makeVerifiedProfile(Consumer<ExpertProfile> overrides) {
        ExpertProfile profile = new ExpertProfile();
        profile.setExpertProfileId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        profile.setUserId(UUID.fromString("00000000-0000-0000-0000-0000000000f1"));
        profile.setSpecialty("Obstetrics");
        profile.setProfessionalTitle("BS. CKI");
        profile.setExperienceYears((short) 5);
        profile.setWorkplace("Bệnh viện Từ Dũ");
        profile.setConsultationScope("Tư vấn thai kỳ");
        profile.setVerificationStatus(ExpertVerificationStatus.VERIFIED);
        profile.setVerifiedAt(Instant.parse("2026-06-25T09:00:00Z"));
        profile.setVerifiedBy(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
        profile.setCreatedAt(Instant.parse("2026-06-20T09:00:00Z"));
        profile.setUpdatedAt(Instant.parse("2026-06-25T09:00:00Z"));
        overrides.accept(profile);
        return profile;
    }

    static ExpertProfile makePendingProfile() {
        return makeVerifiedProfile(p -> {
            p.setVerificationStatus(ExpertVerificationStatus.PENDING);
            p.setVerifiedAt(null);
            p.setVerifiedBy(null);
        });
    }

    // --- NEW for UC-104 ---

    static ExpertProfile makeLockedProfile() {
        return makeVerifiedProfile(p -> {
            p.setVerificationStatus(ExpertVerificationStatus.LOCKED);
            // verifiedAt/verifiedBy INTENTIONALLY retained from the original
            // UC-103 approval — ADR-EXP-302/C7: UC-104 never touches these.
        });
    }

    static ExpertProfile makeRevokedProfile() {
        return makeVerifiedProfile(p -> p.setVerificationStatus(ExpertVerificationStatus.REVOKED));
    }

    static ExpertProfile makeRejectedProfile() {
        return makeVerifiedProfile(p -> {
            p.setVerificationStatus(ExpertVerificationStatus.REJECTED);
            p.setVerifiedAt(Instant.parse("2026-06-22T09:00:00Z"));
        });
    }

    static BadgeActionRequest makeLockRequest() {
        return makeLockRequest("Chứng chỉ hết hạn, chờ gia hạn");
    }

    static BadgeActionRequest makeLockRequest(String reason) {
        BadgeActionRequest req = new BadgeActionRequest();
        req.setAction(BadgeAction.LOCK);
        req.setReason(reason);
        return req;
    }

    static BadgeActionRequest makeRevokeRequest() {
        return makeRevokeRequest("Vi phạm quy tắc tư vấn nhiều lần");
    }

    static BadgeActionRequest makeRevokeRequest(String reason) {
        BadgeActionRequest req = new BadgeActionRequest();
        req.setAction(BadgeAction.REVOKE);
        req.setReason(reason);
        return req;
    }

    static BadgeActionRequest makeReinstateRequest() {
        return makeReinstateRequest("Đã gia hạn chứng chỉ thành công");
    }

    static BadgeActionRequest makeReinstateRequest(String reason) {
        BadgeActionRequest req = new BadgeActionRequest();
        req.setAction(BadgeAction.REINSTATE);
        req.setReason(reason);
        return req;
    }

    // consultation_bookings / expert_reviews snapshot rows for ADR-EXP-304 assertions
    static ConsultationBooking makeCompletedBooking(UUID expertProfileId) {
        ConsultationBooking booking = new ConsultationBooking();
        booking.setBookingId(UUID.randomUUID());
        booking.setExpertProfileId(expertProfileId);
        booking.setStatus("COMPLETED");
        booking.setScheduledAt(Instant.parse("2026-05-01T09:00:00Z"));
        return booking;
    }

    static ExpertReview makeReview(UUID expertProfileId) {
        ExpertReview review = new ExpertReview();
        review.setReviewId(UUID.randomUUID());
        review.setExpertProfileId(expertProfileId);
        review.setRating((short) 5);
        review.setComment("Tư vấn rất tận tâm");
        return review;
    }

    static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
}
```

---

### EXPB-TC-001 — Lock action transitions VERIFIED → LOCKED

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-EXP-301 §Allowed transitions table` — `VERIFIED → LOCKED` on LOCK

**Preconditions:**
- `FX-EXPB-001` seeded (mocked repository returns `makeVerifiedProfile()`)

**Test Steps:**
1. Arrange: `expertProfileRepository.findById(id)` returns `Optional.of(makeVerifiedProfile())`
2. Act: call `service.applyAction(id, makeLockRequest(), ADMIN_USER_ID)`
3. Assert: returned DTO has `verificationStatus == LOCKED`; `expertProfileRepository.save()` called with entity where `verificationStatus == LOCKED` and `updatedAt` bumped

**Expected Result (PASS):** Status transitions to `LOCKED`.
**Expected Result (FAIL):** Status unchanged, or wrong target state.

**Current Status:** 🔴 Not written
**Implementation Note:** Must call `ExpertBadgePolicy.assertTransitionAllowed(VERIFIED, LOCK)` before mutation.

---

### EXPB-TC-002 — Revoke action transitions VERIFIED → REVOKED

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-EXP-301 §Allowed transitions table` — `VERIFIED → REVOKED` on REVOKE

**Preconditions:** `FX-EXPB-001` seeded.

**Test Steps:**
1. Arrange: mocked repo returns `makeVerifiedProfile()`
2. Act: `service.applyAction(id, makeRevokeRequest("Vi phạm quy tắc tư vấn nhiều lần"), ADMIN_USER_ID)`
3. Assert: result `verificationStatus == REVOKED`

**Expected Result (PASS):** Status = `REVOKED` (terminal).
**Expected Result (FAIL):** Status unchanged or non-terminal.

**Current Status:** 🔴 Not written

---

### EXPB-TC-003 — Revoke action escalates LOCKED → REVOKED

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-EXP-301 §Allowed transitions table` — `LOCKED → REVOKED` (escalation)

**Test Steps:**
1. Arrange: mocked repo returns `makeLockedProfile()`
2. Act: `service.applyAction(id, makeRevokeRequest(), ADMIN_USER_ID)`
3. Assert: result `verificationStatus == REVOKED`

**Expected Result (PASS):** Escalation from a temporary lock to a permanent revocation succeeds.
**Expected Result (FAIL):** Escalation rejected, or profile left at `LOCKED`.

**Current Status:** 🔴 Not written

---

### EXPB-TC-004 — Reinstate action transitions LOCKED → VERIFIED

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-EXP-301 §Allowed transitions table` — `LOCKED → VERIFIED` on REINSTATE

**Test Steps:**
1. Arrange: mocked repo returns `makeLockedProfile()`
2. Act: `service.applyAction(id, makeReinstateRequest(), ADMIN_USER_ID)`
3. Assert: result `verificationStatus == VERIFIED`

**Expected Result (PASS):** Status restored to `VERIFIED`.
**Expected Result (FAIL):** Status stuck at `LOCKED` or transitions to an unrelated state.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the ONLY reversal path in this UC's state machine — must not be confused with UC-103's `PENDING`-origin transitions.

---

### EXPB-TC-005 — Lock without reason is rejected (EXPB-101)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgePolicy.assertReasonRequired()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertBadgePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-EXP-302`, `TDS §10 EXPB-101`

**Test Steps:**
1. Act: `policy.assertReasonRequired(null)` and `policy.assertReasonRequired("   ")` (blank)
2. Assert: both throw `ValidationException` with code `EXPB-101`

**Expected Result (PASS):** `ValidationException("EXPB-101")` thrown for both null and blank reason.
**Expected Result (FAIL):** No exception thrown, or wrong error code.

**Current Status:** 🔴 Not written

---

### EXPB-TC-006 — Revoke without reason is rejected (EXPB-101)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgePolicy.assertReasonRequired()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertBadgePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-EXP-302`

**Test Steps:**
1. Act: `service.applyAction(id, makeRevokeRequest(null), ADMIN_USER_ID)` (mocked repo returns `makeVerifiedProfile()`)
2. Assert: throws `ValidationException("EXPB-101")`; `expertProfileRepository.save()` NEVER called

**Expected Result (PASS):** Exception thrown before any persistence occurs.
**Expected Result (FAIL):** Save is invoked despite missing reason.

**Current Status:** 🔴 Not written

---

### EXPB-TC-007 — Reinstate without reason is rejected (EXPB-101) — stricter than UC-103

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgePolicy.assertReasonRequired()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertBadgePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-EXP-302` — reason required for ALL 3 actions, including REINSTATE (Logic Issue L5)

**Test Steps:**
1. Act: `service.applyAction(id, makeReinstateRequest(""), ADMIN_USER_ID)` (mocked repo returns `makeLockedProfile()`)
2. Assert: throws `ValidationException("EXPB-101")`

**Expected Result (PASS):** Exception thrown — this is the test that would FAIL if the implementer incorrectly copies UC-103's conditional-note pattern (where APPROVE does not require a note).
**Expected Result (FAIL):** REINSTATE silently succeeds with no reason, violating ADR-EXP-302's stricter requirement.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the highest-value regression guard against L5 (Logic Issues §2).

---

### EXPB-TC-008 — Reinstate on REVOKED profile rejected (EXPB-102, terminal state)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgePolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertBadgePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-EXP-301` — `REVOKED` has no outgoing edge

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(REVOKED, REINSTATE)`
2. Assert: throws `ConflictException("EXPB-102")`

**Expected Result (PASS):** Exception thrown; `REVOKED` proven terminal.
**Expected Result (FAIL):** Transition silently allowed, breaking the terminal-state invariant (§6.5 of TDS).

**Current Status:** 🔴 Not written

---

### EXPB-TC-009 — Lock on PENDING profile rejected (EXPB-102, UC-103-exclusive state)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgePolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertBadgePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-EXP-301` — `PENDING` has no badge-action edge; must be `VERIFIED` via UC-103 first

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(PENDING, LOCK)`
2. Assert: throws `ConflictException("EXPB-102")`

**Expected Result (PASS):** Exception thrown — proves UC-104's policy structurally cannot reach into UC-103's state space.
**Expected Result (FAIL):** Transition allowed, violating ADR-EXP-301's boundary with UC-103.

**Current Status:** 🔴 Not written

---

### EXPB-TC-010 — Revoke on REJECTED profile rejected (EXPB-102)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgePolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertBadgePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-EXP-301`

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(REJECTED, REVOKE)`
2. Assert: throws `ConflictException("EXPB-102")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** Transition silently allowed.

**Current Status:** 🔴 Not written

---

### EXPB-TC-011 — Lock on NEEDS_MORE_INFO profile rejected (EXPB-102)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertBadgePolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/ExpertBadgePolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-EXP-301`

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(NEEDS_MORE_INFO, LOCK)`
2. Assert: throws `ConflictException("EXPB-102")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** Transition silently allowed.

**Current Status:** 🔴 Not written

---

### EXPB-TC-012 — Action on unknown expertProfileId returns 404 (EXPB-104)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()` / `getBadgeStatus()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §10 EXPB-104`, `TDS §8.1 @throws NotFoundException`

**Test Steps:**
1. Arrange: `expertProfileRepository.findById(unknownId)` returns `Optional.empty()`
2. Act/Assert: `service.applyAction(unknownId, makeLockRequest(), ADMIN_USER_ID)` throws `NotFoundException("EXPB-104")`
3. Act/Assert (read path): `service.getBadgeStatus(unknownId)` also throws `NotFoundException("EXPB-104")`

**Expected Result (PASS):** `NotFoundException("EXPB-104")` thrown for both write and read paths; `save()` never called.
**Expected Result (FAIL):** NPE or wrong error code.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### EXPB-TC-013 — SYSTEM_ADMIN can call badge-actions endpoint

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertBadgeController.applyAction()` + Spring Security chain
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertBadgeControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-EXPB-006` (SYSTEM_ADMIN JWT).

**Test Steps (Attack Simulation):** N/A — positive case.
1. Send `POST /api/v1/admin/expert-profiles/{id}/badge-actions` with SYSTEM_ADMIN JWT and valid `{action:"LOCK", reason:"..."}` body
2. Assert `200 OK`

**Expected Result (PASS):** `200 OK`.
**Expected Result (FAIL):** `403 Forbidden` incorrectly returned for a valid admin.

**Current Status:** 🔴 Not written

---

### EXPB-TC-014 — EXPERT role forbidden from badge-actions endpoint

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertBadgeController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertBadgeControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-EXPB-007` (EXPERT JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../badge-actions` with EXPERT JWT (even against the expert's own profile)
2. Assert `403 Forbidden`, body `error.code == "EXPB-103"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` — an expert can never self-lock/self-revoke/self-reinstate their own badge.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK`.

**Current Status:** 🔴 Not written

---

### EXPB-TC-015 — MODERATOR role forbidden from badge-actions endpoint (ADR-EXP-303)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`, `ADR-EXP-303`
**Feature Under Test:** `ExpertBadgeController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertBadgeControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `SRS §3.2.2.6 Primary Actor field ("System Admin", Secondary Actors: "None")`, `ADR-EXP-303`

**Preconditions:** `FX-EXPB-008` (MODERATOR JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../badge-actions` with MODERATOR JWT
2. Assert `403 Forbidden`, body `error.code == "EXPB-103"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden`. KEY test distinguishing UC-104's stricter boundary — MODERATOR must NOT be treated as equivalent to SYSTEM_ADMIN for badge sanctions, even though MODERATOR may have adjacent moderation powers elsewhere.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK` — privilege scope creep beyond SRS.

**Current Status:** 🔴 Not written

---

### EXPB-TC-016 — Unauthenticated request rejected (401)

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertBadgeControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `SRS E1`

**Test Steps (Attack Simulation):**
1. Send `POST .../badge-actions` with no `Authorization` header
2. Assert `401 Unauthorized`

**Expected Result (PASS):** `401 Unauthorized`.
**Expected Result (FAIL):** Request processed without authentication.

**Current Status:** 🔴 Not written

---

### EXPB-TC-017 — GET badge-status endpoint also SYSTEM_ADMIN-only

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `ExpertBadgeController.getBadgeStatus()`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertBadgeControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-EXPB-009` (MOTHER JWT).

**Test Steps (Attack Simulation):**
1. Send `GET /api/v1/admin/expert-profiles/{id}/badge-status` with MOTHER JWT
2. Assert `403 Forbidden`

**Expected Result (PASS):** `403 Forbidden` — read-only status view is equally protected, not just the write endpoint.
**Expected Result (FAIL):** `200 OK` — leaks an expert's sanction status/history context to a non-admin.

**Current Status:** 🔴 Not written

---

### EXPB-TC-018 — Successful LOCK action emits audit log and domain event

**Severity:** `CRITICAL`
**Legal:** `PDPA`, `BR-CONSULTATION` (auditable lifecycle)
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-EXP-302`, `AuditAction.EXPERT_VERIFICATION` (existing, reused enum value — C8)

**Test Steps:**
1. Act: `service.applyAction(id, makeLockRequest(), ADMIN_USER_ID)`
2. Assert: `verify(auditService).log(eq(AuditAction.EXPERT_VERIFICATION), eq(ADMIN_USER_ID), eq("expert_profiles"), eq(id.toString()), any())`
3. Assert: `verify(applicationEventPublisher).publishEvent(argThat(e -> e instanceof ExpertBadgeLocked))`

**Expected Result (PASS):** Both audit log call and domain event publish occur exactly once, with `EXPERT_VERIFICATION` (no new `AuditAction` value invented, per C8).
**Expected Result (FAIL):** Missing audit call (PDPA gap), missing/wrong event type, or a new `AuditAction` value used instead of the existing one.

**Current Status:** 🔴 Not written

---

### EXPB-TC-019 — Successful REVOKE and REINSTATE emit their respective distinct events

**Severity:** `HIGH`
**Legal:** `PDPA`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §7.1 Domain Event Catalog` — `ExpertBadgeRevoked`, `ExpertBadgeReinstated`

**Test Steps:**
1. Act: `service.applyAction(id, makeRevokeRequest(), ADMIN_USER_ID)` (from `makeVerifiedProfile()`)
2. Assert: `publishEvent(argThat(e -> e instanceof ExpertBadgeRevoked))`
3. Act: `service.applyAction(id2, makeReinstateRequest(), ADMIN_USER_ID)` (from `makeLockedProfile()`)
4. Assert: `publishEvent(argThat(e -> e instanceof ExpertBadgeReinstated))`
5. Assert both event payloads carry non-null `previousStatus`/`newStatus` matching the observed transition (TDS §7.3 `Payload` record)

**Expected Result (PASS):** Each action type publishes its OWN distinct event class with correct `previousStatus`/`newStatus`.
**Expected Result (FAIL):** All 3 actions collapse into one generic event type, losing sanction-type distinction required for downstream `notification` consumers.

**Current Status:** 🔴 Not written

---

### EXPB-TC-020 — Non-retroactivity: applyAction() never touches out-of-scope repositories (ADR-EXP-304, C6)

**Severity:** `CRITICAL`
**Legal:** `BR-CONSULTATION`, `PDPA`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()` (structural/architectural test)
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-EXP-304`, `TDS §17.4 AP-AI-006` (most critical anti-pattern of this UC)

**Preconditions:**
- `ExpertBadgeServiceImpl` is constructed via its constructor with ONLY the collaborators declared in TDS §5.1 class diagram: `IExpertProfileRepository`, `ExpertBadgePolicy`, `AuditService`, `ApplicationEventPublisher`.

**Test Steps:**
1. Reflectively enumerate `ExpertBadgeServiceImpl`'s declared fields
2. Assert: NO field is of type `ConsultationBookingRepository`, `ExpertReviewRepository`, `PaymentTransactionRepository`, `CommissionRecordRepository`, or any repository/service touching those 4 tables
3. Act: `service.applyAction(id, makeRevokeRequest(), ADMIN_USER_ID)` against mocks for the 4 ALLOWED collaborators only
4. Assert: the call completes successfully using ONLY the allowed mocks (proves no hidden dependency was needed)

**Expected Result (PASS):** Structural scan finds zero forbidden repository fields; behavioral call succeeds with only the 4 allowed collaborators mocked.
**Expected Result (FAIL):** A forbidden repository field exists (even if unused) — flagged as `AP-AI-006` violation, CRITICAL, must block merge.

**Current Status:** 🔴 Not written
**Implementation Note:** This test is INTENTIONALLY stronger than a simple `verifyNoInteractions()` — it also prevents the anti-pattern of injecting a forbidden repository "just in case" without calling it.

---

### EXPB-TC-021 — verified_at / verified_by remain unchanged across a badge action (C7)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-EXP-302 implementation note`, `TDS §17.4 AP-AI-007`, `TDS §18 OI-2`

**Test Steps:**
1. Arrange: mocked repo returns `makeVerifiedProfile()` with fixed `verifiedAt = 2026-06-25T09:00:00Z`, `verifiedBy = admin-A-uuid`
2. Act: `service.applyAction(id, makeRevokeRequest(), ADMIN_USER_ID_B)` (a DIFFERENT admin performing the revoke)
3. Assert: saved entity's `verifiedAt` and `verifiedBy` are IDENTICAL to the pre-action values (still `admin-A-uuid` / original timestamp), NOT overwritten with `ADMIN_USER_ID_B`/now()

**Expected Result (PASS):** `verified_at`/`verified_by` untouched — proves UC-104 does not conflate "who revoked" with "who verified."
**Expected Result (FAIL):** `verifiedBy` overwritten with the revoking admin's ID — `AP-AI-007` violation.

**Current Status:** 🔴 Not written

---

### EXPB-TC-022 — Reason at exactly 2000 chars accepted (boundary)

**Severity:** `MEDIUM`
**Feature Under Test:** `BadgeActionRequest` Bean Validation (`@Size(max=2000)`)
**Test File:** `src/test/java/com/carebridge/backend/expert/dto/BadgeActionRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §8.1 @Size(max = 2000)`

**Test Steps:**
1. Arrange: `reason` = a string of exactly 2000 characters
2. Act: validate `BadgeActionRequest` via `jakarta.validation.Validator`
3. Assert: zero constraint violations

**Expected Result (PASS):** No violation at the upper boundary.
**Expected Result (FAIL):** False-positive violation at exactly 2000 chars.

**Current Status:** 🔴 Not written

---

### EXPB-TC-023 — Reason at 2001 chars rejected (boundary)

**Severity:** `MEDIUM`
**Feature Under Test:** `BadgeActionRequest` Bean Validation (`@Size(max=2000)`)
**Test File:** `src/test/java/com/carebridge/backend/expert/dto/BadgeActionRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §8.1 @Size(max = 2000)`

**Test Steps:**
1. Arrange: `reason` = a string of exactly 2001 characters
2. Act: validate `BadgeActionRequest`
3. Assert: exactly one constraint violation on `reason`

**Expected Result (PASS):** Violation raised at 2001 chars, confirming the upper boundary is enforced (not silently truncated/accepted).
**Expected Result (FAIL):** No violation — an over-long reason could exceed downstream storage/audit-log expectations.

**Current Status:** 🔴 Not written

---

### EXPB-TC-024 — Full lifecycle walk: VERIFIED → LOCKED → VERIFIED → LOCKED → REVOKED → terminal

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgePolicy` + `ExpertBadgeServiceImpl.applyAction()` (chained)
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertBadgeServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `TDS §13.3 E2E/Security Tests`, `ADR-EXP-301` state machine

**Test Steps:**
1. Start from `makeVerifiedProfile()`. LOCK → assert `LOCKED`.
2. REINSTATE → assert `VERIFIED`.
3. LOCK again → assert `LOCKED`.
4. REVOKE (escalation) → assert `REVOKED`.
5. Attempt REINSTATE on the now-`REVOKED` profile → assert `ConflictException("EXPB-102")`.
6. Attempt LOCK on the now-`REVOKED` profile → assert `ConflictException("EXPB-102")`.

**Expected Result (PASS):** Every step matches the documented state machine (§6.5 of TDS); terminal state is provably unreachable-from after reaching `REVOKED`.
**Expected Result (FAIL):** Any step deviates from the documented FSM, or a post-terminal action succeeds.

**Current Status:** 🔴 Not written

---

### WEB TEST CASES (Vitest + Testing Library)

---

### EXPB-TC-WEB-001 — Badge-action panel offers only LOCK/REVOKE when status is VERIFIED

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgeActionPage.tsx` (or embedded panel in `ExpertVerificationDetailPage.tsx`)
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/__tests__/ExpertBadgeActionPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §11.3 Chặng 5` — "client UI MUST only offer LOCK/REVOKE when VERIFIED, REVOKE/REINSTATE when LOCKED, hide all when REVOKED"

**Test Steps:**
1. Render the badge-action panel with a profile fixture `verificationStatus: "VERIFIED"`
2. Assert: `LOCK` and `REVOKE` buttons are present; `REINSTATE` button is ABSENT

**Expected Result (PASS):** Only the 2 valid actions for `VERIFIED` are rendered.
**Expected Result (FAIL):** `REINSTATE` shown for a `VERIFIED` profile (no such edge exists — would cause a client-side dead-end 409).

**Current Status:** 🔴 Not written

---

### EXPB-TC-WEB-002 — Badge-action panel hides all action buttons when status is REVOKED (terminal)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgeActionPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/__tests__/ExpertBadgeActionPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §11.3 Chặng 5`, `ADR-EXP-301` (REVOKED terminal)

**Test Steps:**
1. Render the badge-action panel with a profile fixture `verificationStatus: "REVOKED"`
2. Assert: NO badge-action buttons (`LOCK`/`REVOKE`/`REINSTATE`) are rendered; a terminal-state indicator/message is shown instead

**Expected Result (PASS):** UI mirrors the server-side terminal invariant — no dead-end action is ever offered.
**Expected Result (FAIL):** Any action button rendered for a `REVOKED` profile, which would always 409 server-side.

**Current Status:** 🔴 Not written

---

### EXPB-TC-WEB-003 — Submit button disabled until reason is provided (all 3 actions)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertBadgeActionPage.tsx` — `badgeActionRequestSchema`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/__tests__/ExpertBadgeActionPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `TDS §11.3 Chặng 5 badgeActionRequestSchema` (`reason: z.string().min(1)...`)

**Test Steps:**
1. Render panel with `VERIFIED` profile, select `LOCK`, leave reason empty, attempt submit
2. Assert: Zod validation error shown, `applyBadgeAction` API call NOT invoked
3. Repeat for a `LOCKED` profile selecting `REINSTATE` with empty reason — same assertion (this is the web-layer counterpart of `EXPB-TC-007`)

**Expected Result (PASS):** Form blocks submission for ALL 3 actions when reason is empty — including REINSTATE, unlike UC-103's conditional note UX.
**Expected Result (FAIL):** Form submits with empty reason for any action, mismatching backend's `EXPB-101` rule.

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### EXPB-TC-INT-001 — Full flow: VERIFIED profile locked via real API + DB

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST /api/v1/admin/expert-profiles/{id}/badge-actions → DB update`
**Test File:** `src/test/java/com/carebridge/backend/expert/ExpertBadgeIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically on Spring context start
- Seed: one `users` row (role SYSTEM_ADMIN), one `users` row (role EXPERT), one `expert_profiles` row with `verification_status='VERIFIED'` referencing the EXPERT user

**Test Steps:**
1. Seed data as above
2. `POST /api/v1/admin/expert-profiles/{id}/badge-actions` with SYSTEM_ADMIN JWT, body `{action:"LOCK", reason:"Document expiring, pending renewal"}`
3. Assert response `200`, `data.verificationStatus == "LOCKED"`
4. Query DB directly for the row

**Expected Result (PASS):**
- API response `200` with `verificationStatus: "LOCKED"`
- DB row: `verification_status = 'LOCKED'`, `verified_at`/`verified_by` UNCHANGED from the pre-seeded values, `updated_at` bumped

**Expected Result (FAIL):** DB row not updated, or `verified_at`/`verified_by` incorrectly overwritten.

**DB Assertion:**
```java
ExpertProfile record = expertProfileRepository.findById(savedId).orElseThrow();
assertThat(record.getVerificationStatus()).isEqualTo(ExpertVerificationStatus.LOCKED);
assertThat(record.getVerifiedBy()).isEqualTo(preSeededVerifiedBy);   // unchanged
assertThat(record.getVerifiedAt()).isEqualTo(preSeededVerifiedAt);  // unchanged
```

**Current Status:** 🔴 Not written

---

### EXPB-TC-INT-002 — ADR-EXP-304 core guarantee: past consultation_bookings/expert_reviews remain byte-for-byte unchanged after REVOKE

**Severity:** `CRITICAL`
**Legal:** `BR-CONSULTATION`, `PDPA`
**Feature Under Test:** `Full flow: POST .../badge-actions {action:"REVOKE"} → DB verification of untouched tables`
**Test File:** `src/test/java/com/carebridge/backend/expert/ExpertBadgeIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `ADR-EXP-304`, `TDS §13.2 Integration Tests`, `TDS §14.1 DB Inspection`

**Preconditions:**
- `FX-EXPB-001` (VERIFIED expert profile) + `FX-EXPB-005` (1x `consultation_bookings` COMPLETED row + 1x `expert_reviews` row, both linked to the same `expert_profile_id`)

**Test Steps:**
1. Snapshot (full row hash/serialization) of the seeded `consultation_bookings` and `expert_reviews` rows BEFORE the action
2. `POST /api/v1/admin/expert-profiles/{id}/badge-actions` with SYSTEM_ADMIN JWT, body `{action:"REVOKE", reason:"Vi phạm quy tắc tư vấn nhiều lần"}`
3. Assert response `200`, `data.verificationStatus == "REVOKED"`
4. Re-query `consultation_bookings` and `expert_reviews` for the same `expert_profile_id` AFTER the action
5. Assert: row counts identical AND every column value identical to the pre-action snapshot (no `status`, `rating`, `comment`, or any other field mutated; no rows deleted)

**Expected Result (PASS):** Zero drift in `consultation_bookings`/`expert_reviews` — proves ADR-EXP-304 holds end-to-end, not just at the unit-mock level.
**Expected Result (FAIL):** Any column value or row count differs — CRITICAL data-integrity regression, maps to the P0 incident trigger in `TDS §12.1`.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the single most important integration test in this Test-Spec — it is the only test that proves ADR-EXP-304 against a REAL database rather than a mock's absence of interaction.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EXPB-TC-001` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-002` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-003` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-004` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-005` | `ExpertBadgePolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-006` | `ExpertBadgePolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-007` | `ExpertBadgePolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-008` | `ExpertBadgePolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-009` | `ExpertBadgePolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-010` | `ExpertBadgePolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-011` | `ExpertBadgePolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-012` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-013` | `ExpertBadgeControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-014` | `ExpertBadgeControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-015` | `ExpertBadgeControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-016` | `ExpertBadgeControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-017` | `ExpertBadgeControllerSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-018` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-019` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-020` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-021` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-022` | `BadgeActionRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-023` | `BadgeActionRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-024` | `ExpertBadgeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-WEB-001` | `ExpertBadgeActionPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-WEB-002` | `ExpertBadgeActionPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-WEB-003` | `ExpertBadgeActionPage.test.tsx:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-INT-001` | `ExpertBadgeIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPB-TC-INT-002` | `ExpertBadgeIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class ExpertBadgeServiceImpl implements IExpertBadgeService {

    @Override
    public ExpertBadgeStatusResponse applyAction(UUID expertProfileId, BadgeActionRequest request, UUID adminUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ExpertBadgeStatusResponse getBadgeStatus(UUID expertProfileId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class ExpertBadgePolicy {
    public ExpertVerificationStatus assertTransitionAllowed(ExpertVerificationStatus current, BadgeAction action) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    public void assertReasonRequired(String reason) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EXPB-TC-001` to `004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-005` to `011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-013` to `017` | `403/401 forced by missing controller wiring` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-018`, `019` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-020` | `reflective scan fails — class not yet wired with correct collaborators` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-021` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-022`, `023` | `DTO class not yet annotated` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-024` | `throw('Not implemented')` (chained calls fail at step 1) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-WEB-001/002/003` | `component not implemented / API not wired` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPB-TC-INT-001`, `002` | `500 from stub exception` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-EXPGOV-IMP-104` đã được review và approve
- [ ] UC-103's `ExpertVerificationStatus` enum và `ExpertProfile` entity đã implement TRƯỚC (UC-104 extend, không redefine)
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead
- [ ] `expert` package skeleton confirmed present (`.gitkeep` in all layers, verified)
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị, bao gồm `FX-EXPB-005` (consultation/review seed cho non-retroactivity test)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers), đặc biệt `EXPB-TC-INT-002`
- [ ] `npm run test:run` — web tests xanh
- [ ] Test coverage ≥ 80% lines cho `ExpertBadgeServiceImpl`, `ExpertBadgePolicy`
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] Cả 4 transition edge trong ADR-EXP-301 (`VERIFIED→LOCKED`, `VERIFIED→REVOKED`, `LOCKED→VERIFIED`, `LOCKED→REVOKED`) có test case tương ứng, cộng với representative non-edges
- [ ] `EXPB-TC-020` (structural non-injection check) và `EXPB-TC-INT-002` (real-DB non-retroactivity) đều xanh — ADR-EXP-304 là non-negotiable cho UC này

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` không lỗi
- [ ] **Props Isolation** — mọi test dùng `ExpertGovernanceTestFactory` (shared, extended từ UC-103), không shared mutable state
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn (ADR/SRS/schema)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-103 chưa implement `ExpertVerificationStatus`/`ExpertProfile` — UC-104 không thể bắt đầu vì extend cùng enum/entity (hard blocker, không phải soft dependency)
- Phát hiện lỗi kiến trúc mới cần Tech Lead review (đặc biệt bất kỳ đề xuất nào inject repository ngoài phạm vi ADR-EXP-304)

---

## 7. Rollback Plan

```bash
# Revert implementation files (no migration exists for this UC)
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/policy/ExpertBadgePolicy.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/service/impl/ExpertBadgeServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertBadgeController.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/

# CAUTION: do NOT revert ExpertVerificationStatus.java wholesale if UC-103 has
# already merged its 4 base values — only remove the LOCKED/REVOKED additions
# this UC introduced, to avoid breaking UC-103's already-shipped functionality.

# Gap vẫn OPEN → giữ nguyên Status: Draft trong UC104_RevokeExpertBadge_TDS.md
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-EXP-301/302/303/304 | ☑ Not detected — every TC cites an Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Pending Red Gate run | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assumes MODERATOR access without ADR | ☑ Not detected — `EXPB-TC-015` explicitly tests MODERATOR denial per ADR-EXP-303 | G-1 |
| AP-AI-004 | Layer Violation | Test verifies controller has business logic (transition rules inline in controller) | ☑ Not detected — transition logic tested exclusively against `ExpertBadgePolicy`, not controller | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports service/type not in TDS §8, or invents a 4th `BadgeAction` value | ☑ Not detected — all types match TDS §8.1/8.2/8.3; only `LOCK`/`REVOKE`/`REINSTATE` tested | G-3 |
| AP-AI-006 *(custom, project-specific)* | Retroactive Mutation | Test suite fails to assert `ExpertBadgeServiceImpl` has zero dependency on `ConsultationBookingRepository`/`ExpertReviewRepository`/etc. | ☑ Not detected — `EXPB-TC-020` (structural) + `EXPB-TC-INT-002` (real-DB) both directly target this, per ADR-EXP-304 | G-2 ★★ CRITICAL |
| AP-AI-007 *(custom, project-specific)* | Verification-Field Overreach | Test suite fails to assert `verified_at`/`verified_by` remain untouched by badge actions | ☑ Not detected — `EXPB-TC-021` (unit) + `EXPB-TC-INT-001` (integration DB assertion) both directly target this, per C7 | G-2 |
| AP-AI-008 *(custom, project-specific)* | Reason-Requirement Erosion | Test suite copies UC-103's conditional-note pattern instead of UC-104's unconditional-reason rule, silently letting REINSTATE skip validation | ☑ Not detected — `EXPB-TC-007` (unit) + `EXPB-TC-WEB-003` (web) explicitly cover REINSTATE-without-reason, distinct from UC-103's APPROVE-without-note allowance | G-1 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec-authoring → TDD spec approved for Red Gate execution
- [ ] AP-AI-002 (Green-from-Birth) check pending actual Red Gate run once stubs are committed
- [ ] AP-AI-006 (Retroactive Mutation) — CRITICAL, treat as a release-blocking gate distinct from the standard G-2 severity, given this is the single most important guarantee of UC-104 per its own TDS §12.1 P0 incident trigger

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none at spec time)_ | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — pending Tech Lead / TV4-Lâm review and Red Gate execution.*
