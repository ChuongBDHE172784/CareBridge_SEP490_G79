# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-172 Suspend Expert — Test Specification

**Document ID:** `CB-EXPGOV-TDD-172`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (lines 786-800)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.4.1 (UC-172, lines 1579-1598)
- `04_Implement/UC172_SuspendExpert/UC172_SuspendExpert_TDS.md` (CB-EXPGOV-IMP-172) — Technical Specification
- `04_Implement/UC104_RevokeExpertBadge/UC104_RevokeExpertBadge_TDS.md` — reused state machine/service source (ADR-EXP-301/302/303/304)
- `04_Implement/UC104_RevokeExpertBadge/UC104_RevokeExpertBadge_Test-Spec.md` — sibling Test-Spec, `ExpertGovernanceTestFactory` reference (this Test-Spec extends the SAME factory, no duplicate)
- `CLAUDE.md` — BR-RBAC, audit requirements for expert/moderation/safety workflows

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`/`.test.tsx`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm run test:run` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khởi tạo Test-Spec cho UC-172; tái sử dụng `ExpertGovernanceTestFactory` (extend, không tạo factory song song) từ UC-103/UC-104 Test-Spec |

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
| **Feature / Gap ID** | `UC-172` |
| **Module** | `SuspendExpert — Bounded Context: expert` |
| **Spec gốc** | `CB-EXPGOV-IMP-172` |
| **Priority** | 🔴 P0 (High per SRS Table 94) |
| **Sprint** | `S3 Consultation Lifecycle, Expert Governance, And Location Visibility — TV4-Lâm` |
| **Data Classification** | `PII` |
| **Compliance Scope** | `PDPA`, `BR-RBAC`, `BR-CONSULTATION` |
| **Upstream Dependencies** | UC-104 Revoke Expert Badge (REUSED `ExpertBadgeServiceImpl`/`ExpertBadgePolicy`/`ExpertVerificationStatus`, per ADR-SUSP-401 — this UC's backend tests are largely a SCOPED SUBSET of UC-104's already-specified tests, re-asserted here for UC-172's own regression net) |
| **Downstream Consumers** | UC-80/81 View Expert Directory/Profile (informational, out of scope), UC-112 View Expert Dashboard |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXPGOV-IMP-172 §17`, ADR-SUSP-401/402/403/404 |
| **Constraints Injected** | C1-C8 per TDS §17.1 (no new enum value, no new backend class, frontend restricted to LOCK/REINSTATE only, SYSTEM_ADMIN-only reused authorization, reason required for both actions, zero read/write on out-of-scope tables, no new migration, no new domain event without Tech Lead sign-off) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS UC-172 wording ("temporarily suspends... after violations, serious complaints, or invalid documents") textually parallels UC-104's SRS wording for `LOCKED`, with no separate state named anywhere in the SRS | TDS §3 ADR-SUSP-401 concludes "suspend" = the EXISTING `LOCKED` state; no new enum value is created | Tests assert `ExpertVerificationStatus` still has exactly 6 values (unchanged from UC-104) after this UC's implementation — a 7th value (e.g., `SUSPENDED`) appearing is a FAIL |
| L2 | Task instruction names `ExpertSuspended`/`ExpertReinstated` as expected domain events | TDS §7.1 deliberately reuses UC-104's `ExpertBadgeLocked`/`ExpertBadgeReinstated` events (no Tech-Lead-approved rename exists yet — see TDS §18 OI-4) | Tests assert the PUBLISHED event class is `ExpertBadgeLocked`/`ExpertBadgeReinstated` (not a hypothetical `ExpertSuspended`), unless/until a rename ADR is approved; a test importing a non-existent `ExpertSuspended` class would itself be a `AP-AI-005` (Hallucinated Contract) violation |
| L3 | SRS UC-172 gives System Admin no explicit endpoint of its own — only a generic "opens Suspend Expert" trigger step | TDS §9.1 confirms ZERO new endpoints; UC-172's UI calls the SAME `POST /api/v1/admin/expert-profiles/{id}/badge-actions` UC-104 already exposes | Backend/API-level tests for this UC are SCOPED DUPLICATES of a subset of UC-104's already-passing test suite (LOCK + REINSTATE only) — re-specified here to (a) give UC-172 its own regression net independent of UC-104's file location, and (b) explicitly test the "REVOKE is never used by this UI surface" boundary, which UC-104's own Test-Spec does not test (UC-104 assumes REVOKE IS offered) |
| L4 | No SRS-explicit rule preventing an admin from suspending an already-`REVOKED` (permanently sanctioned) expert | TDS §ADR-SUSP-401 / state machine (§6.4): `REVOKED` has no outgoing edge, `LOCK` is only valid from `VERIFIED` | Test explicitly covers "suspend a REVOKED profile" → `EXPB-102`, with an assertion message distinguishing this from the generic UC-104 test (this is the SRS-172-relevant error UX: "cannot suspend an already-revoked expert") |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
SuspendExpert bao gồm các layer:
├── Domain (ExpertBadgePolicy — REUSED from UC-104, re-tested here for LOCK/REINSTATE subset only)
├── Services (ExpertBadgeServiceImpl — REUSED, re-tested for the suspend/reinstate action pair)
├── Controller (ExpertBadgeController — REUSED, no new endpoint)
├── Web (SuspendExpertPage.tsx — NEW, Vitest + Testing Library, mock API client)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — full suspend/reinstate flow)
```

> **Note on backend test duplication:** Because ADR-SUSP-401 mandates ZERO new backend classes, this Test-Spec's backend test cases (`SUSP-TC-001` through `SUSP-TC-012`) intentionally cover the SAME production code paths already tested by UC-104's `EXPB-TC-001`–`EXPB-TC-024`. This is not redundant busywork — CareBridge delivery rules and the CASE 2.0 Red Gate protocol require every UC's own Test-Spec to be independently falsifiable; a regression that breaks UC-172's suspend/reinstate flow without breaking UC-104's generic panel (e.g., a future refactor that accidentally couples UC-104's REVOKE-specific logic into the LOCK path) should be caught by BOTH test suites independently.

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-172` (§3.2.4.1, lines 1579-1598) | Suspend/lift-suspension actions; PRE-3/PRE-4 auth + existing-record guard; POST-2/POST-3 status update + audit; E1/E2 exceptions |
| `ADR-SUSP-401` | "Suspend" = `LOCKED` (no new enum value); UC-172 restricted to `LOCK`/`REINSTATE` subset of UC-104's 4-edge transition table; `REVOKE` never offered |
| `ADR-SUSP-402` | Prospective-only — zero reads/writes against `consultation_bookings`, `consultation_sessions`, `expert_reviews`, `payment_transactions`, `commission_records` (inherited) |
| `ADR-SUSP-403` | SYSTEM_ADMIN-only authorization; no MODERATOR access (inherited) |
| `ADR-SUSP-404` | New `SuspendExpertPage.tsx` UI surface calling the EXISTING endpoint with a restricted action set |
| `BR-RBAC` | Role-scoped access enforcement |
| `V1__init_schema.sql` (lines 786-800) | Real column names/types for persistence assertions |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Admin suspends a VERIFIED profile (LOCK) | `ExpertBadgeServiceImpl.applyAction()` | `SUSP-TC-001` |
| TC-COND-002 | Admin lifts suspension on a LOCKED profile (REINSTATE) | `ExpertBadgeServiceImpl.applyAction()` | `SUSP-TC-002` |
| TC-COND-003 | Suspend without reason rejected | `ExpertBadgePolicy.assertReasonRequired()` | `SUSP-TC-003` |
| TC-COND-004 | Lift suspension without reason rejected | `ExpertBadgePolicy.assertReasonRequired()` | `SUSP-TC-004` |
| TC-COND-005 | Suspend an already-LOCKED profile rejected (idempotency guard) | `ExpertBadgePolicy.assertTransitionAllowed()` | `SUSP-TC-005` |
| TC-COND-006 | Suspend a REVOKED (terminal) profile rejected | `ExpertBadgePolicy.assertTransitionAllowed()` | `SUSP-TC-006` |
| TC-COND-007 | Suspend a PENDING/REJECTED/NEEDS_MORE_INFO profile rejected (UC-103-exclusive states) | `ExpertBadgePolicy.assertTransitionAllowed()` | `SUSP-TC-007`, `SUSP-TC-008`, `SUSP-TC-009` |
| TC-COND-008 | Unknown expertProfileId on suspend/reinstate | `ExpertBadgeServiceImpl.applyAction()` | `SUSP-TC-010` |
| TC-COND-009 | Role-based access (SYSTEM_ADMIN vs others) on suspend/reinstate | `ExpertBadgeController` + Spring Security | `SUSP-TC-011` to `SUSP-TC-015` |
| TC-COND-010 | Audit + domain event emitted on suspend/reinstate (reused event classes) | `ExpertBadgeServiceImpl.applyAction()` | `SUSP-TC-016`, `SUSP-TC-017` |
| TC-COND-011 | Non-retroactivity for suspend/reinstate actions | `ExpertBadgeServiceImpl.applyAction()` (structural) | `SUSP-TC-018` |
| TC-COND-012 | Enum integrity — no 7th `SUSPENDED` value exists (ADR-SUSP-401 regression guard) | `ExpertVerificationStatus` | `SUSP-TC-019` |
| TC-COND-013 | Web: `SuspendExpertPage` shows Suspend when VERIFIED, Lift Suspension when LOCKED, and NEVER a Revoke control in any state | `SuspendExpertPage.tsx` | `SUSP-TC-WEB-001`, `SUSP-TC-WEB-002`, `SUSP-TC-WEB-003` |
| TC-COND-014 | Full integration: VERIFIED → LOCKED (suspend) → VERIFIED (lift) via real API + DB | `ExpertBadgeController` E2E | `SUSP-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `action` restricted set (`LOCK`/`REINSTATE` valid for this UI; `REVOKE` invalid-for-this-surface) | Confirms UC-172's narrower action scope vs UC-104's full set |
| State Transition Testing | `ExpertVerificationStatus` FSM, UC-172 slice (2 of the 4 edges) | Core of this UC — must prove the restricted scope, not just that the edges work |
| Error Guessing | Suspend-a-REVOKED-profile, suspend-an-already-LOCKED-profile, role bypass attempts | Security/idempotency assurance specific to UC-172's SRS-named triggers |
| Negative/Structural Testing | Confirm `ExpertVerificationStatus` enum has NOT grown a 7th value; confirm no new event class `ExpertSuspended` exists unless approved | Enforces ADR-SUSP-401 at the architecture level |
| Regression Testing | Reuse (not duplicate) `ExpertGovernanceTestFactory` from UC-103/104 | Confirms Props Isolation Pattern compliance across sibling UCs |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-SUSP-001` | DB seed | REUSED `FX-EXPB-001` — `expert_profiles` row with `verification_status='VERIFIED'` | Happy path suspend from VERIFIED |
| `FX-SUSP-002` | DB seed | REUSED `FX-EXPB-002` — `expert_profiles` row with `verification_status='LOCKED'` | Happy path lift-suspension from LOCKED |
| `FX-SUSP-003` | DB seed | REUSED `FX-EXPB-003` — `expert_profiles` row with `verification_status='REVOKED'` | Invalid-transition test (cannot suspend a permanently revoked expert) |
| `FX-SUSP-004` | DB seed | REUSED `FX-EXPB-004` — `expert_profiles` row with `verification_status='PENDING'` | Invalid-transition test (UC-103-exclusive state) |
| `FX-SUSP-005` | DB seed | REUSED `FX-EXPB-005` — 1x `consultation_bookings` (`status='COMPLETED'`) + 1x `expert_reviews`, linked to `FX-SUSP-001`'s `expert_profile_id` | ADR-SUSP-402 non-retroactivity integration assertion |
| `FX-SUSP-006` | JWT | REUSED `FX-EXPB-006` — `{ sub: 'admin-001', role: 'SYSTEM_ADMIN' }` | Auth context for admin actions |
| `FX-SUSP-007` | JWT | REUSED `FX-EXPB-007` — `{ sub: 'expert-001', role: 'EXPERT' }` | Negative auth test |
| `FX-SUSP-008` | JWT | REUSED `FX-EXPB-008` — `{ sub: 'mod-001', role: 'MODERATOR' }` | Negative auth test (no MODERATOR access) |
| `FX-SUSP-009` | JWT | REUSED `FX-EXPB-009` — `{ sub: 'mother-001', role: 'MOTHER' }` | Negative auth test |

---

## 4. Test Case Specification

> **TC ID format:** `SUSP-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ExpertGovernanceTestFactory.java — SHARED across UC-103/UC-104/UC-172 test suites
// UC-172 REUSES the UC-103/UC-104 factory AS-IS. No new factory class is created;
// only new SuspendExpertRequest-style helper methods are ADDED to the SAME class
// (illustrative — actual file is extended, not forked).
// ═══════════════════════════════════════════════════════════

class ExpertGovernanceTestFactory {

    // --- Inherited from UC-103/UC-104 (unchanged, reused as-is) ---
    // makeVerifiedProfile(), makeVerifiedProfile(overrides), makePendingProfile(),
    // makeLockedProfile(), makeRevokedProfile(), makeRejectedProfile(),
    // makeLockRequest(reason), makeRevokeRequest(reason), makeReinstateRequest(reason),
    // makeCompletedBooking(expertProfileId), makeReview(expertProfileId), ADMIN_USER_ID
    // — see UC104_RevokeExpertBadge_Test-Spec.md §4 for full definitions, NOT reproduced here.

    // --- NEW for UC-172 (thin aliases — same underlying objects as UC-104's factory,
    //     named to match UC-172's SRS vocabulary for readability in this Test-Spec only) ---

    static BadgeActionRequest makeSuspendRequest() {
        return makeSuspendRequest("Nhận được khiếu nại nghiêm trọng từ người dùng, tạm đình chỉ để xác minh.");
    }

    static BadgeActionRequest makeSuspendRequest(String reason) {
        return makeLockRequest(reason); // alias — SUSPEND (UC-172) == LOCK (UC-104), ADR-SUSP-401
    }

    static BadgeActionRequest makeLiftSuspensionRequest() {
        return makeLiftSuspensionRequest("Đã xác minh không có vi phạm, khôi phục quyền tư vấn.");
    }

    static BadgeActionRequest makeLiftSuspensionRequest(String reason) {
        return makeReinstateRequest(reason); // alias — LIFT SUSPENSION (UC-172) == REINSTATE (UC-104)
    }
}
```

---

### SUSP-TC-001 — Suspend action transitions VERIFIED → LOCKED

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()` (REUSED from UC-104, called via UC-172's restricted scope)
**Test File:** `src/test/java/com/carebridge/backend/expert/service/SuspendExpertServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SUSP-401` — "Suspend" (UC-172) = `LOCK` action, `VERIFIED → LOCKED` (inherits `ADR-EXP-301`)

**Preconditions:**
- `FX-SUSP-001` seeded (mocked repository returns `makeVerifiedProfile()`)

**Test Steps:**
1. Arrange: `expertProfileRepository.findById(id)` returns `Optional.of(makeVerifiedProfile())`
2. Act: call `service.applyAction(id, makeSuspendRequest(), ADMIN_USER_ID)`
3. Assert: returned DTO has `verificationStatus == LOCKED`; `save()` called with entity where `verificationStatus == LOCKED`

**Expected Result (PASS):** Status transitions to `LOCKED` — proving UC-172's "suspend" concept concretely maps to the existing `LOCKED` state, not a new one.
**Expected Result (FAIL):** Status unchanged, wrong target state, or a `SUSPENDED` value appears (would indicate an unauthorized enum addition, violating ADR-SUSP-401).

**Current Status:** 🔴 Not written

---

### SUSP-TC-002 — Lift Suspension action transitions LOCKED → VERIFIED

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/SuspendExpertServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SUSP-401` — "Lift Suspension" = `REINSTATE` action, `LOCKED → VERIFIED`

**Test Steps:**
1. Arrange: mocked repo returns `makeLockedProfile()`
2. Act: `service.applyAction(id, makeLiftSuspensionRequest(), ADMIN_USER_ID)`
3. Assert: result `verificationStatus == VERIFIED`

**Expected Result (PASS):** Status restored to `VERIFIED`.
**Expected Result (FAIL):** Status stuck at `LOCKED` or transitions to an unrelated state.

**Current Status:** 🔴 Not written

---

### SUSP-TC-003 — Suspend without reason is rejected (EXPB-101)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgePolicy.assertReasonRequired()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/SuspendExpertPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SUSP-401` (inherits `ADR-EXP-302`), `TDS §10 EXPB-101`

**Test Steps:**
1. Act: `service.applyAction(id, makeSuspendRequest(""), ADMIN_USER_ID)` (mocked repo returns `makeVerifiedProfile()`)
2. Assert: throws `ValidationException("EXPB-101")`; `save()` never called

**Expected Result (PASS):** Exception thrown before any persistence.
**Expected Result (FAIL):** Save invoked despite missing reason.

**Current Status:** 🔴 Not written

---

### SUSP-TC-004 — Lift Suspension without reason is rejected (EXPB-101)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgePolicy.assertReasonRequired()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/SuspendExpertPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SUSP-401` — reason required for BOTH actions, no relaxed rule for UC-172's UI

**Test Steps:**
1. Act: `service.applyAction(id, makeLiftSuspensionRequest(null), ADMIN_USER_ID)` (mocked repo returns `makeLockedProfile()`)
2. Assert: throws `ValidationException("EXPB-101")`

**Expected Result (PASS):** Exception thrown — proves UC-172's UI cannot bypass UC-104's stricter reason-required rule for REINSTATE.
**Expected Result (FAIL):** Lift-suspension silently succeeds with no reason.

**Current Status:** 🔴 Not written

---

### SUSP-TC-005 — Suspend an already-LOCKED profile rejected (EXPB-102, idempotency)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgePolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/SuspendExpertPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SUSP-401` — `LOCK` only valid FROM `VERIFIED`, not from `LOCKED`

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(LOCKED, LOCK)`
2. Assert: throws `ConflictException("EXPB-102")`

**Expected Result (PASS):** Exception thrown — re-suspending an already-suspended expert is rejected, not silently re-applied.
**Expected Result (FAIL):** Transition silently allowed (idempotency/audit-trail integrity risk).

**Current Status:** 🔴 Not written

---

### SUSP-TC-006 — Suspend a REVOKED (permanently sanctioned) profile rejected (EXPB-102)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertBadgePolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/SuspendExpertPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SUSP-401` — `REVOKED` is terminal, no outgoing edge

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(REVOKED, LOCK)`
2. Assert: throws `ConflictException("EXPB-102")`

**Expected Result (PASS):** Exception thrown — this is the SRS-172-specific UX case ("cannot suspend an already-revoked expert"), distinct from UC-104's generic terminal-state test (which tests REINSTATE-on-REVOKED, not LOCK-on-REVOKED).
**Expected Result (FAIL):** Transition silently allowed, breaking the terminal-state invariant.

**Current Status:** 🔴 Not written

---

### SUSP-TC-007 — Suspend a PENDING profile rejected (EXPB-102, UC-103-exclusive state)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgePolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/SuspendExpertPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SUSP-401`

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(PENDING, LOCK)`
2. Assert: throws `ConflictException("EXPB-102")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** Transition silently allowed.

**Current Status:** 🔴 Not written

---

### SUSP-TC-008 — Suspend a REJECTED profile rejected (EXPB-102)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertBadgePolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/SuspendExpertPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SUSP-401`

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(REJECTED, LOCK)`
2. Assert: throws `ConflictException("EXPB-102")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** Transition silently allowed.

**Current Status:** 🔴 Not written

---

### SUSP-TC-009 — Suspend a NEEDS_MORE_INFO profile rejected (EXPB-102)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertBadgePolicy.assertTransitionAllowed()`
**Test File:** `src/test/java/com/carebridge/backend/expert/policy/SuspendExpertPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SUSP-401`

**Test Steps:**
1. Act: `policy.assertTransitionAllowed(NEEDS_MORE_INFO, LOCK)`
2. Assert: throws `ConflictException("EXPB-102")`

**Expected Result (PASS):** Exception thrown.
**Expected Result (FAIL):** Transition silently allowed.

**Current Status:** 🔴 Not written

---

### SUSP-TC-010 — Suspend/lift-suspension on unknown expertProfileId returns 404 (EXPB-104)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/SuspendExpertServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §10 EXPB-104`

**Test Steps:**
1. Arrange: `expertProfileRepository.findById(unknownId)` returns `Optional.empty()`
2. Act/Assert: `service.applyAction(unknownId, makeSuspendRequest(), ADMIN_USER_ID)` throws `NotFoundException("EXPB-104")`

**Expected Result (PASS):** `NotFoundException("EXPB-104")` thrown; `save()` never called.
**Expected Result (FAIL):** NPE or wrong error code.

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### SUSP-TC-011 — SYSTEM_ADMIN can suspend an expert

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertBadgeController.applyAction()` + Spring Security chain
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/SuspendExpertControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-SUSP-006` (SYSTEM_ADMIN JWT).

**Test Steps:**
1. Send `POST /api/v1/admin/expert-profiles/{id}/badge-actions` with SYSTEM_ADMIN JWT and `{action:"LOCK", reason:"..."}` body
2. Assert `200 OK`

**Expected Result (PASS):** `200 OK`.
**Expected Result (FAIL):** `403 Forbidden` incorrectly returned for a valid admin.

**Current Status:** 🔴 Not written

---

### SUSP-TC-012 — EXPERT role forbidden from self-suspending

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `ExpertBadgeController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/SuspendExpertControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-SUSP-007` (EXPERT JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../badge-actions` with EXPERT JWT against the expert's own profile, `{action:"LOCK"}`
2. Assert `403 Forbidden`, body `error.code == "EXPB-103"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` — an expert can never self-suspend to (e.g.) temporarily hide from moderation review.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK`.

**Current Status:** 🔴 Not written

---

### SUSP-TC-013 — MODERATOR role forbidden from suspend action (ADR-SUSP-403)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`, `ADR-SUSP-403`
**Feature Under Test:** `ExpertBadgeController` + `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/SuspendExpertControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `SRS §3.2.4.1 Primary Actor field ("System Admin", Secondary Actors: "None")`, `ADR-SUSP-403`

**Preconditions:** `FX-SUSP-008` (MODERATOR JWT).

**Test Steps (Attack Simulation):**
1. Send `POST .../badge-actions` with MODERATOR JWT, `{action:"LOCK"}`
2. Assert `403 Forbidden`, body `error.code == "EXPB-103"`

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden`.
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK` — privilege scope creep beyond SRS.

**Current Status:** 🔴 Not written

---

### SUSP-TC-014 — Unauthenticated suspend request rejected (401)

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** Spring Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/SuspendExpertControllerSecurityTest.java`
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

### SUSP-TC-015 — MOTHER role forbidden from GET badge-status (read path)

**Severity:** `MEDIUM`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `ExpertBadgeController.getBadgeStatus()`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/SuspendExpertControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §16 Authorization Matrix`

**Preconditions:** `FX-SUSP-009` (MOTHER JWT).

**Test Steps (Attack Simulation):**
1. Send `GET /api/v1/admin/expert-profiles/{id}/badge-status` with MOTHER JWT
2. Assert `403 Forbidden`

**Expected Result (PASS):** `403 Forbidden`.
**Expected Result (FAIL):** `200 OK` — leaks an expert's sanction status to a non-admin.

**Current Status:** 🔴 Not written

---

### SUSP-TC-016 — Successful suspend emits audit log and ExpertBadgeLocked event

**Severity:** `CRITICAL`
**Legal:** `PDPA`, `BR-CONSULTATION`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/SuspendExpertServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-SUSP-401` (inherits `ADR-EXP-302`), `AuditAction.EXPERT_VERIFICATION`

**Test Steps:**
1. Act: `service.applyAction(id, makeSuspendRequest(), ADMIN_USER_ID)`
2. Assert: `verify(auditService).log(eq(AuditAction.EXPERT_VERIFICATION), eq(ADMIN_USER_ID), eq("expert_profiles"), eq(id.toString()), any())`
3. Assert: `verify(applicationEventPublisher).publishEvent(argThat(e -> e instanceof ExpertBadgeLocked))` — NOT `ExpertSuspended` (no such class exists per TDS §18 OI-4, unless a rename ADR is later approved)

**Expected Result (PASS):** Audit log + `ExpertBadgeLocked` event fire exactly once.
**Expected Result (FAIL):** Missing audit call, or code imports/publishes a non-existent `ExpertSuspended` class (would itself fail compilation — `AP-AI-005` Hallucinated Contract).

**Current Status:** 🔴 Not written

---

### SUSP-TC-017 — Successful lift-suspension emits ExpertBadgeReinstated event

**Severity:** `HIGH`
**Legal:** `PDPA`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/SuspendExpertServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §7.1 Domain Event Catalog`

**Test Steps:**
1. Act: `service.applyAction(id, makeLiftSuspensionRequest(), ADMIN_USER_ID)` (from `makeLockedProfile()`)
2. Assert: `publishEvent(argThat(e -> e instanceof ExpertBadgeReinstated))`

**Expected Result (PASS):** `ExpertBadgeReinstated` event published.
**Expected Result (FAIL):** Wrong/missing event type.

**Current Status:** 🔴 Not written

---

### SUSP-TC-018 — Non-retroactivity: suspend/reinstate never touch out-of-scope repositories

**Severity:** `CRITICAL`
**Legal:** `BR-CONSULTATION`, `PDPA`
**Feature Under Test:** `ExpertBadgeServiceImpl.applyAction()` (structural test)
**Test File:** `src/test/java/com/carebridge/backend/expert/service/SuspendExpertServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-SUSP-402` (inherits `ADR-EXP-304`)

**Test Steps:**
1. Act: `service.applyAction(id, makeSuspendRequest(), ADMIN_USER_ID)` against mocks for ONLY the 4 allowed collaborators (`IExpertProfileRepository`, `ExpertBadgePolicy`, `AuditService`, `ApplicationEventPublisher`)
2. Assert: call completes successfully using only the allowed mocks; no `ConsultationBookingRepository`/`ExpertReviewRepository`/`PaymentTransactionRepository`/`CommissionRecordRepository` field exists on `ExpertBadgeServiceImpl`

**Expected Result (PASS):** Structural scan finds zero forbidden repository fields.
**Expected Result (FAIL):** A forbidden repository field exists — `AP-AI-006` violation (inherited from UC-104), CRITICAL, must block merge.

**Current Status:** 🔴 Not written

---

### SUSP-TC-019 — ExpertVerificationStatus enum integrity: exactly 6 values, no SUSPENDED added (ADR-SUSP-401 regression guard)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertVerificationStatus` enum (structural test)
**Test File:** `src/test/java/com/carebridge/backend/expert/entity/ExpertVerificationStatusTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-SUSP-401` — "no new enum value" is this UC's single most important constraint

**Test Steps:**
1. Act: `ExpertVerificationStatus.values()`
2. Assert: array has EXACTLY 6 elements: `PENDING`, `VERIFIED`, `REJECTED`, `NEEDS_MORE_INFO`, `LOCKED`, `REVOKED` (order-independent set comparison)

**Expected Result (PASS):** Exactly the 6 UC-103/UC-104-defined values, no `SUSPENDED` or other 7th value.
**Expected Result (FAIL):** A 7th value exists — direct violation of ADR-SUSP-401, CRITICAL, must block merge. This is UC-172's highest-value regression guard, analogous to UC-104's `EXPB-TC-020` (non-retroactivity) in criticality.

**Current Status:** 🔴 Not written
**Implementation Note:** This test has NO UC-104 analogue — it exists specifically because UC-172 is the UC most likely to tempt an implementer into adding a `SUSPENDED` value by following the SRS wording literally instead of consulting ADR-SUSP-401.

---

### WEB TEST CASES (Vitest + Testing Library)

---

### SUSP-TC-WEB-001 — SuspendExpertPage shows "Suspend" button when status is VERIFIED

**Severity:** `HIGH`
**Feature Under Test:** `SuspendExpertPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/__tests__/SuspendExpertPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §11.3 Chặng 2` — renders only when `verificationStatus` is `VERIFIED` (show "Suspend") or `LOCKED` (show "Lift Suspension")

**Test Steps:**
1. Render `SuspendExpertPage` with a profile fixture `verificationStatus: "VERIFIED"`
2. Assert: "Suspend" button present; "Lift Suspension" button ABSENT

**Expected Result (PASS):** Only the applicable action for `VERIFIED` is rendered.
**Expected Result (FAIL):** Wrong button set, or both shown simultaneously.

**Current Status:** 🔴 Not written

---

### SUSP-TC-WEB-002 — SuspendExpertPage shows "Lift Suspension" button when status is LOCKED

**Severity:** `HIGH`
**Feature Under Test:** `SuspendExpertPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/__tests__/SuspendExpertPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §11.3 Chặng 2`

**Test Steps:**
1. Render `SuspendExpertPage` with a profile fixture `verificationStatus: "LOCKED"`
2. Assert: "Lift Suspension" button present; "Suspend" button ABSENT

**Expected Result (PASS):** Correct single action offered.
**Expected Result (FAIL):** Wrong button set.

**Current Status:** 🔴 Not written

---

### SUSP-TC-WEB-003 — SuspendExpertPage NEVER renders a Revoke control, in any status (C3, AP-AI-009 guard)

**Severity:** `CRITICAL`
**Feature Under Test:** `SuspendExpertPage.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/__tests__/SuspendExpertPage.test.tsx`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `TDS §17.1 C3`, `§17.4 AP-AI-009`

**Test Steps:**
1. Render `SuspendExpertPage` once for EACH of `VERIFIED`, `LOCKED`, `REVOKED`, `PENDING`, `REJECTED`, `NEEDS_MORE_INFO` fixtures
2. Assert: in ALL 6 renders, no element with accessible name matching `/revoke/i` exists anywhere in the rendered output

**Expected Result (PASS):** Zero "Revoke" controls across all 6 states — proves UC-172's UI structurally cannot issue a permanent sanction.
**Expected Result (FAIL):** A "Revoke" control appears in any state — `AP-AI-009` violation, CRITICAL, must block merge (this is UC-172's UI-layer equivalent of `SUSP-TC-019`'s backend enum guard).

**Current Status:** 🔴 Not written
**Implementation Note:** Highest-value UI regression guard for this UC — prevents scope creep from UC-104's generic panel being copy-pasted wholesale into UC-172's restricted surface.

---

### INTEGRATION TEST CASES

---

### SUSP-TC-INT-001 — Full flow: VERIFIED → suspend → LOCKED → lift suspension → VERIFIED via real API + DB

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `ExpertBadgeController` → `ExpertBadgeServiceImpl` → PostgreSQL (Testcontainers)
**Test File:** `src/test/java/com/carebridge/backend/expert/SuspendExpertIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically on Spring context start (no new migration for this UC — regression check that existing migrations still apply cleanly)
- Seed: 1x `expert_profiles` row `verification_status='VERIFIED'` (`FX-SUSP-001`), 1x `consultation_bookings` + 1x `expert_reviews` row linked to it (`FX-SUSP-005`)

**Test Steps:**
1. `POST /api/v1/admin/expert-profiles/{id}/badge-actions` with SYSTEM_ADMIN JWT, `{action:"LOCK", reason:"..."}`
2. Assert `200 OK`, response `verificationStatus == "LOCKED"`
3. `GET /api/v1/admin/expert-profiles/{id}/badge-status` — assert `LOCKED`
4. `POST .../badge-actions` with `{action:"REINSTATE", reason:"..."}`
5. Assert `200 OK`, response `verificationStatus == "VERIFIED"`
6. Assert DB: `consultation_bookings` and `expert_reviews` rows for this expert are byte-for-byte unchanged from step 0 (ADR-SUSP-402)

**Expected Result (PASS):** Full suspend → lift-suspension round trip succeeds via the real HTTP + DB stack; unrelated tables untouched.
**Expected Result (FAIL):** Any step fails, or a booking/review row is mutated.

**DB Assertion:**
```java
ExpertProfile record = expertProfileRepository.findById(savedId).orElseThrow();
assertThat(record.getVerificationStatus()).isEqualTo(ExpertVerificationStatus.VERIFIED);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SUSP-TC-001` | `SuspendExpertServiceImplTest.java` | `[ ]` | | |
| `SUSP-TC-002` | `SuspendExpertServiceImplTest.java` | `[ ]` | | |
| `SUSP-TC-003` | `SuspendExpertPolicyTest.java` | `[ ]` | | |
| `SUSP-TC-004` | `SuspendExpertPolicyTest.java` | `[ ]` | | |
| `SUSP-TC-005` | `SuspendExpertPolicyTest.java` | `[ ]` | | |
| `SUSP-TC-006` | `SuspendExpertPolicyTest.java` | `[ ]` | | |
| `SUSP-TC-007` | `SuspendExpertPolicyTest.java` | `[ ]` | | |
| `SUSP-TC-008` | `SuspendExpertPolicyTest.java` | `[ ]` | | |
| `SUSP-TC-009` | `SuspendExpertPolicyTest.java` | `[ ]` | | |
| `SUSP-TC-010` | `SuspendExpertServiceImplTest.java` | `[ ]` | | |
| `SUSP-TC-011` | `SuspendExpertControllerSecurityTest.java` | `[ ]` | | |
| `SUSP-TC-012` | `SuspendExpertControllerSecurityTest.java` | `[ ]` | | |
| `SUSP-TC-013` | `SuspendExpertControllerSecurityTest.java` | `[ ]` | | |
| `SUSP-TC-014` | `SuspendExpertControllerSecurityTest.java` | `[ ]` | | |
| `SUSP-TC-015` | `SuspendExpertControllerSecurityTest.java` | `[ ]` | | |
| `SUSP-TC-016` | `SuspendExpertServiceImplTest.java` | `[ ]` | | |
| `SUSP-TC-017` | `SuspendExpertServiceImplTest.java` | `[ ]` | | |
| `SUSP-TC-018` | `SuspendExpertServiceImplTest.java` | `[ ]` | | |
| `SUSP-TC-019` | `ExpertVerificationStatusTest.java` | `[ ]` | | |
| `SUSP-TC-WEB-001` | `SuspendExpertPage.test.tsx` | `[ ]` | | |
| `SUSP-TC-WEB-002` | `SuspendExpertPage.test.tsx` | `[ ]` | | |
| `SUSP-TC-WEB-003` | `SuspendExpertPage.test.tsx` | `[ ]` | | |
| `SUSP-TC-INT-001` | `SuspendExpertIntegrationTest.java` | `[ ]` | | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase (backend — illustrative; production `ExpertBadgeServiceImpl` already exists per UC-104, so the Red Gate for UC-172's backend tests exercises the REAL implementation restricted to LOCK/REINSTATE inputs; only the NEW `SuspendExpertPage.tsx` needs a literal throw-stub):**

```tsx
// SuspendExpertPage.tsx — Red Phase stub
export function SuspendExpertPage() {
  throw new Error("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SUSP-TC-001` to `SUSP-TC-019` | Exercises EXISTING UC-104 `ExpertBadgeServiceImpl`/`ExpertBadgePolicy` (no stub needed — code already implemented per UC-104 dependency) | 🔴 FAIL if UC-104 not yet deployed to this environment; 🟢 PASS if UC-104 already implemented (expected, since this is a scoped reuse, not new logic) | ☐ FAIL ☐ PASS | ☐ UC-104 dependency missing ☐ N/A (reuse case) |
| `SUSP-TC-WEB-001` to `SUSP-TC-WEB-003` | `throw("Not implemented")` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `SUSP-TC-INT-001` | Exercises EXISTING endpoint | 🟢 PASS if UC-104 backend exists (expected) | ☐ FAIL ☐ PASS | |

> **Important Red Gate nuance for this UC:** Because ADR-SUSP-401 mandates zero new backend classes, `SUSP-TC-001`–`SUSP-TC-019`/`SUSP-TC-INT-001` are NOT expected to fail against an "empty stub" in the traditional CASE 2.0 sense — they exercise UC-104's ALREADY-IMPLEMENTED code. Their Red Gate purpose is instead to confirm they **currently pass against UC-104's real implementation** (proving the reuse claim is accurate) and would **FAIL if a regression decoupled UC-172's expected behavior from UC-104's shared code path**. Only `SUSP-TC-WEB-001` through `SUSP-TC-WEB-003` (the genuinely NEW `SuspendExpertPage.tsx`) undergo the standard empty/throw-stub Red Gate.

**Red Gate Evidence:**

- Stub commit hash: `___` (Web stub only)
- Tất cả FAIL (Web) / PASS-against-UC-104 confirmed (Backend)? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-EXPGOV-IMP-172` đã được review và approve
- [ ] UC-104's `ExpertBadgeController`/`ExpertBadgeServiceImpl`/`ExpertBadgePolicy` đã được implement và tests đã xanh (HARD DEPENDENCY — UC-172 has no independent backend)
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị (reused from UC-104's fixtures)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit/policy/security tests xanh, bao gồm `SUSP-TC-001` đến `SUSP-TC-019`
- [ ] `./mvnw verify` — `SUSP-TC-INT-001` xanh (Testcontainers)
- [ ] `npm run test:run` — `SUSP-TC-WEB-001` đến `SUSP-TC-WEB-003` xanh
- [ ] Không có business logic mới trong Controller (đã REUSED nguyên vẹn từ UC-104)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] **`SUSP-TC-019` xanh** — `ExpertVerificationStatus` vẫn đúng 6 giá trị (regression guard quan trọng nhất của UC này)
- [ ] **`SUSP-TC-WEB-003` xanh** — không có control "Revoke" nào xuất hiện trên `SuspendExpertPage.tsx`

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — Web tests FAIL với throw-stub trước khi implement `SuspendExpertPage.tsx`; backend tests confirmed PASS against UC-104's existing implementation (reuse verification)
- [ ] **Contract Existence** — `ExpertBadgeController`, `ExpertBadgeServiceImpl`, `ExpertBadgePolicy`, `ExpertVerificationStatus`, `BadgeActionRequest` all exist and are UNMODIFIED by this UC's changes:
  ```bash
  git diff --stat -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/
  # Expected: no output (zero backend changes for UC-172)
  ```
- [ ] **Props Isolation** — `SuspendExpertRequest`/related test data created via `ExpertGovernanceTestFactory`, no shared mutable state
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR-SUSP-40x)

### Suspension Criteria (Điều kiện tạm dừng)

- UC-104 chưa được implement (blocker dependency)
- Phát hiện lỗi kiến trúc mới cần Principal Architect review (vd: OI-4 event-naming decision chưa resolve)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# No migration to revert (zero schema changes). Revert web-only implementation files:
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/SuspendExpertPage.tsx
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/models/suspendExpert.ts
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/services/suspendExpertApi.ts
git checkout -- 05_Development/CareBridgeWebApp/src/features/adminExpertGovernance/pages/__tests__/SuspendExpertPage.test.tsx
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/service/SuspendExpertServiceImplTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/policy/SuspendExpertPolicyTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/SuspendExpertControllerSecurityTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/entity/ExpertVerificationStatusTest.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/SuspendExpertIntegrationTest.java

# Gap vẫn OPEN → giữ nguyên entry trong tracking backlog
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Web test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic mới (thay vì REUSED) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import `ExpertSuspended`/`SuspendedExpertService` hoặc type không tồn tại trong codebase | ☐ | G-3 |
| AP-AI-008 *(custom, inherited from TDS §17.4)* | State Machine Duplication | Test file references a `SUSPENDED` enum value or a parallel policy/service class | ☐ | G-1 ★★ |
| AP-AI-009 *(custom, inherited from TDS §17.4)* | Scope Creep — REVOKE Exposure | `SUSP-TC-WEB-003` fails because a Revoke control was rendered | ☐ | G-2 ★★ |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | [mô tả issue] | [hành động fix] | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — pending Tech Lead / TV4-Lâm review, đặc biệt §5.1 Red Gate nuance (backend reuse case) và §18 (TDS) Open Items.*
