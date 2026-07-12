# TEST-DRIVEN DEVELOPMENT SPECIFICATION â€” UC193 Switch Active Baby Profile

**Document ID:** `CB-BABY-TDD-193`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented — 2026-07-10 (targeted baby/carejourney backend tests PASS; full regression blocked by non-baby Family/Exercise/Auth/Triage failures)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `Not required` *(is_active is a non-PII boolean flag â€” see TDS Â§11.1)*
**Approved by:** `TV2-BÃ¡ch`
**Classification:** `Internal â€” Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260703100100__add_baby_profile_active_flag.sql` (new, per TDS Â§5.2)
- `02_Requirements/SRS/3_Functional_Specification.md Â§3.3.12.2`
- `04_Implement/UC193_SwitchActiveBabyProfile/UC193_SwitchActiveBabyProfile_TDS.md` (`CB-BABY-IMP-003`)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/service/impl/BabyServiceImpl.java` (existing, verified â€” no `switchActiveBabyProfile()` today)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/controller/BabyController.java` (existing, verified â€” no `PATCH .../active` today)
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/baby/BabyServiceImplTest.java` (existing 7 tests â€” UC192, must remain green)

> **Quy Æ°á»›c TDD:** Test-first. Thá»© tá»±: viáº¿t test â†’ cháº¡y â†’ xÃ¡c nháº­n FAIL ðŸ”´ â†’ implement â†’ PASS ðŸŸ¢ â†’ refactor ðŸ”µ.
> KhÃ´ng dÃ¹ng PII tháº­t â€” chá»‰ SYNTHETIC data (UUID literals).

---

## CHANGELOG

| NgÃ y | NgÆ°á»i thá»±c hiá»‡n | Ná»™i dung thay Ä‘á»•i |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented after targeted baby/carejourney backend test pass; full regression remains blocked outside baby scope. |
| 2026-07-03 | AI Agent | Khá»Ÿi táº¡o tÃ i liá»‡u â€” Test-Spec cho UC193 Switch Active Baby Profile (Draft) |

---

## Má»¤C Lá»¤C

1. [ThÃ´ng tin Module](#1-thÃ´ng-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. ThÃ´ng tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-193` |
| **Module** | Baby â€” Switch Active Baby Profile |
| **Spec gá»‘c** | `CB-BABY-IMP-003` |
| **Priority** | ðŸŸ  P1 (Medium priority per TDS, but touches Sensitive-PII row + new DB invariant) |
| **Sprint** | Sprint 4 â€” Device Sync And Care Edge Cases |
| **Milestone** | M3 Alpha |
| **Data Classification** | `Sensitive-PII` (extends `baby_profiles`) |
| **Compliance Scope** | BR-RBAC, BR-PRIVACY |
| **Upstream Dependencies** | `BabyProfile`, `BabyProfileRepository`, `BabyController`, `IBabyService`/`BabyServiceImpl`, `BabyAccessPolicy` (all existing â€” UC192/UC32/UC31) |
| **Downstream Consumers** | Mobile home/dashboard screen (default baby context), baby daily log, vaccination, growth tracking |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | Yes |
| **Constraint Source** | `CB-BABY-IMP-003 Â§17`, ADR-BABY-004 |
| **Constraints Injected** | C1-C6 per TDS Â§17.1 |
| **Model** | Claude Sonnet 5 |
| **Trust Level** | T2 â†’ T3 (pending Red Gate) |

---

## 2. Logic Issues Resolved

| # | Spec gá»‘c (sai / thiáº¿u) | Thá»±c táº¿ (schema / policy) | Fix Ã¡p dá»¥ng trong test |
|---|---------------------------|-------------------------------|------------------------------|
| L1 | UC-192 TDS's class diagram documented an `isActive: Boolean` attribute on `BabyProfile` as if it already existed | Real shipped `BabyProfile.java` (verified) has NO `isActive` field â€” the column is genuinely new, introduced for the first time by this UC's migration `V20260703100100` (ADR-BABY-004 Â§Context) | Tests must NOT assume any pre-existing `isActive` behavior in `getBabyProfile()`/`listBabyProfiles()` before this feature ships; `TC-INT-004` explicitly verifies these two UC192 endpoints correctly report `isActive` only AFTER the DTO-mapping side-effect edit (TDS Â§11.3 Cháº·ng 5) is applied |
| L2 | A naive implementation might set the new profile's `is_active=true` before clearing the old one, risking a transient partial-unique-index violation | ADR-BABY-004 Decision mandates a clear-then-set sequence inside ONE transaction (`clearActiveForOwner()` then `save()`) | `TC-003` and `TC-INT-001` assert BOTH the old profile becomes `isActive=false` AND the new profile becomes `isActive=true` in the same call, and the integration test additionally asserts the DB-level partial unique index (`ux_baby_profiles_owner_active`) never rejects a legitimate switch |
| L3 | UC-192's Authorization Matrix documents `ADMIN âœ… All`, which could be wrongly copy-pasted into this endpoint's `@PreAuthorize` | Real shipped `BabyAccessPolicy.canView()` has NO ADMIN branch; this TDS's own Â§16 flags this as Open Item OI-2 and explicitly states `BabyOwnershipPolicy` does NOT bypass for ADMIN either | `TC-005` asserts ADMIN role receives the SAME 403 as any other non-owner caller â€” no admin bypass is implemented, consistent with real code, not the aspirational UC192 doc |
| L4 | Care group members (even ACCEPTED, who CAN view per UC192) might be wrongly assumed to also be able to switch active, since `BabyAccessPolicy.canView()` allows them | ADR-BABY-004 deliberately introduces a NARROWER `BabyOwnershipPolicy.assertOwner()` â€” owner-only, no care-group exception | `TC-002` explicitly tests an ACCEPTED care-group member (non-owner) attempting switch â†’ 403 `BABY-006`, proving the new policy is strictly narrower than the reused view policy |

---

## 3. Test Design Specification (TDS)

### TDS-01 â€” Scope / Pháº¡m vi

```
Baby (Switch Active Profile, UC-193) bao gá»“m cÃ¡c layer:
â”œâ”€â”€ Policy (BabyOwnershipPolicy.assertOwner() â€” pure logic, no deps, new class)
â”œâ”€â”€ Services (BabyServiceImpl.switchActiveBabyProfile() â€” mock BabyProfileRepository +
â”‚             BabyOwnershipPolicy + AuditService vá»›i Mockito)
â”œâ”€â”€ Controller (BabyController.switchActiveBabyProfile() â€” mock IBabyService vá»›i @WebMvcTest)
â””â”€â”€ Integration (Testcontainers PostgreSQL, full stack PATCH /api/v1/babies/{id}/active,
                 including the partial unique index invariant)

LÆ°u Ã½ pháº¡m vi: createBabyProfile(), listBabyProfiles(), getBabyProfile() (UC31/UC32/UC192, Ä‘Ã£
shipped, 7 tests PASSING trong BabyServiceImplTest.java) KHÃ”NG náº±m trong pháº¡m vi Red Gate cá»§a
Test-Spec nÃ y â€” chá»‰ switchActiveBabyProfile() (method má»›i) + BabyOwnershipPolicy (class má»›i) lÃ 
ðŸ”´ RED. Xem Â§5.1 Red Gate Protocol. Ngoáº¡i lá»‡ duy nháº¥t: TC-INT-004 dÃ¹ng láº¡i getBabyProfile()/
listBabyProfiles() Ä‘á»ƒ verify DTO side-effect (L1) â€” Ä‘Ã¢y lÃ  verification, khÃ´ng pháº£i re-stub.
```

### TDS-02 â€” Test Basis

| Source | Items Derived |
|--------|------------------|
| SRS Â§3.3.12.2 UC-193 | Owner selects one baby profile as "active" among multiple |
| ADR-BABY-004 | New `is_active` column + partial unique index; owner-only; clear-then-set transaction; ARCHIVED cannot become active |
| BR-RBAC | Only `profile.ownerUserId == callerId` may switch â€” care-group members (even ACCEPTED) rejected |
| BR-PRIVACY | `is_active` itself non-PII, but scoped to Sensitive-PII `baby_profiles` row |
| SRS POST-3 | Sensitive/state-changing action recorded via `AuditService.log(BABY_ACTIVE_PROFILE_SWITCHED, ...)` |
| TDS Â§10 Error Codes | `BABY-001` (404, reused from UC192), `BABY-006` (403, NEW), `BABY-007` (409, NEW) |
| TDS Â§6.3 State Machine | Invariant: at most one `is_active=true` per owner; ARCHIVED never active |

### TDS-03 â€” Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|--------------------|--------------------|----------------|
| TC-COND-001 | Owner switches active baby (happy path) â€” old active cleared, new one set | `BabyServiceImpl.switchActiveBabyProfile()` | `BABY-SW-TC-001` |
| TC-COND-002 | ACCEPTED care-group member (non-owner) attempts switch â†’ 403 | `BabyOwnershipPolicy.assertOwner()` | `BABY-SW-TC-002` |
| TC-COND-003 | Clear-then-set sequencing: exactly one active row after switch | `BabyServiceImpl.switchActiveBabyProfile()` | `BABY-SW-TC-003` |
| TC-COND-004 | Non-existent babyId â†’ 404 `BABY-001` | `BabyServiceImpl.switchActiveBabyProfile()` | `BABY-SW-TC-004` |
| TC-COND-005 | ADMIN role does NOT bypass ownership check | `BabyOwnershipPolicy.assertOwner()` | `BABY-SW-TC-005` |
| TC-COND-006 | Target profile status=ARCHIVED â†’ 409 `BABY-007` | `BabyServiceImpl.switchActiveBabyProfile()` | `BABY-SW-TC-006` |
| TC-COND-007 | Idempotent re-switch to already-active baby â†’ 200, no-op state | `BabyServiceImpl.switchActiveBabyProfile()` | `BABY-SW-TC-007` |
| TC-COND-008 | Happy-path switch emits `BABY_ACTIVE_PROFILE_SWITCHED` audit exactly once | `BabyServiceImpl.switchActiveBabyProfile()` | `BABY-SW-TC-008` |
| TC-COND-009 | Denied/blocked paths never emit audit | `BabyServiceImpl.switchActiveBabyProfile()` | `BABY-SW-TC-009` |
| TC-COND-010 | No JWT â†’ 401 (controller layer) | `BabyController.switchActiveBabyProfile()` | `BABY-SW-TC-010` |
| TC-COND-011 | IDOR â€” Mother B switches Mother A's baby via full auth chain | `BabyController` + `BabyServiceImpl` | `BABY-SW-TC-SEC-001` |
| TC-COND-012 | Full stack PATCH happy path, DB invariant holds (exactly 1 active/owner) | Integration | `BABY-SW-TC-INT-001` |
| TC-COND-013 | Full stack PATCH 409 (archived target), DB state unchanged | Integration | `BABY-SW-TC-INT-002` |
| TC-COND-014 | Full stack PATCH 403 (non-owner), DB state unchanged | Integration | `BABY-SW-TC-INT-003` |
| TC-COND-015 | Post-switch, `getBabyProfile()`/`listBabyProfiles()` (UC192) correctly report `isActive` | Integration (side-effect verification, L1) | `BABY-SW-TC-INT-004` |
| TC-COND-016 | Mobile: switching active baby updates selected-baby UI state | Mobile widget test | `BABY-SW-TC-MOB-001` |

### TDS-04 â€” Test Techniques

| Technique | Applied To | Rationale |
|--------------|----------------|---------------|
| Equivalence Partitioning | Caller identity: owner / ACCEPTED care-member / ADMIN / unauthenticated | 4 partitions, only "owner" is in the accept partition per ADR-BABY-004 |
| State Transition Testing | `is_active`: falseâ†’true (target), trueâ†’false (previous), ARCHIVED never â†’true | Encodes Â§6.3 State Machine invariant |
| Boundary/Idempotency | Re-switch to already-active baby (TC-007) | "no-op but still 200" boundary between "state changed" and "state confirmed" |
| Invariant Testing | Exactly one `is_active=true` row per owner after any switch (TC-003, TC-INT-001) | ADR-BABY-004 DB-level invariant via partial unique index |
| Error Guessing / Security | IDOR via guessed/known `babyId` belonging to another Mother | OWASP A01:2021 |
| Negative Testing | No audit on denial paths; DB state unchanged on 403/404/409 | BR-PRIVACY audit-trail integrity |

### TDS-05 â€” Test Data Requirements

| Fixture ID | Type | Value / Logic | Má»¥c Ä‘Ã­ch |
|------------|------|--------------------|--------------|
| `FX-193-001` | In-memory | `BabyProfile{id=BABY_1_ID, ownerUserId=OWNER_ID, status=ACTIVE, isActive=false}` | Switch target (happy path) |
| `FX-193-002` | In-memory | `BabyProfile{id=BABY_2_ID, ownerUserId=OWNER_ID, status=ACTIVE, isActive=true}` | Previously-active sibling profile |
| `FX-193-003` | In-memory | `BabyProfile{id=BABY_3_ID, ownerUserId=OWNER_ID, status=ARCHIVED, isActive=false}` | 409 archived-target case |
| `FX-193-004` | In-memory | `babyRepository.findById(NONEXISTENT_ID)` â†’ `Optional.empty()` | 404 case |
| `FX-193-005` | JWT/Auth | `{sub: OWNER_ID, roles: [ROLE_MOTHER]}` | Owner caller |
| `FX-193-006` | JWT/Auth | `{sub: CARE_MEMBER_ID, roles: [ROLE_FAMILY]}` ACCEPTED in care group for `BABY_1_ID` | Non-owner care-member rejection |
| `FX-193-007` | JWT/Auth | `{sub: ADMIN_ID, roles: [ROLE_SYSTEM_ADMIN]}` | Confirms NO admin bypass |
| `FX-193-008` | JWT/Auth | `{sub: OTHER_MOTHER_ID, roles: [ROLE_MOTHER]}` | IDOR attacker (owns no baby in scope) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 â€” Báº®T BUá»˜C)

```java
// SwitchActiveBabyTestFactory.java
class SwitchActiveBabyTestFactory {

    static final UUID OWNER_ID        = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID CARE_MEMBER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID ADMIN_ID        = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID OTHER_MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID BABY_1_ID       = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID BABY_2_ID       = UUID.fromString("00000000-0000-0000-0000-000000000011");
    static final UUID BABY_3_ID       = UUID.fromString("00000000-0000-0000-0000-000000000012");
    static final UUID NONEXISTENT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");

    static BabyProfile makeInactiveBaby() {
        return BabyProfile.builder()
                .id(BABY_1_ID)
                .ownerUserId(OWNER_ID)
                .nickname("Bean")
                .status(BabyProfileStatus.ACTIVE)
                .isActive(false)
                .build();
    }

    static BabyProfile makeCurrentlyActiveSibling() {
        return BabyProfile.builder()
                .id(BABY_2_ID)
                .ownerUserId(OWNER_ID)
                .nickname("Sprout")
                .status(BabyProfileStatus.ACTIVE)
                .isActive(true)
                .build();
    }

    static BabyProfile makeArchivedBaby() {
        return BabyProfile.builder()
                .id(BABY_3_ID)
                .ownerUserId(OWNER_ID)
                .nickname("Old")
                .status(BabyProfileStatus.ARCHIVED)
                .isActive(false)
                .build();
    }
}
```

---

### BABY-SW-TC-001 â€” Owner switches active baby (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `BabyServiceImpl.switchActiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyServiceSwitchActiveTest.java`
**TDD Phase:** ðŸ”´ RED â€” genuinely new method
**Condition Ref:** `TC-COND-001`
**Oracle Source:** ADR-BABY-004 Decision, TDS Â§6.1 Sequence Diagram

**Preconditions:** `FX-193-001` (`BABY_1_ID`, `isActive=false`) mocked from `babyRepository.findById(BABY_1_ID)`.

**Test Steps:**
1. Arrange mocks; `babyOwnershipPolicy.assertOwner()` is a real (non-mocked) bean or verified separately.
2. Act: `babyService.switchActiveBabyProfile(BABY_1_ID, OWNER_ID)`.
3. Assert: `verify(babyRepository).clearActiveForOwner(OWNER_ID)` called BEFORE `save()`.
4. Assert: `verify(babyRepository).save(argThat(p -> p.getId().equals(BABY_1_ID) && p.isActive()))`.
5. Assert: returned `BabyProfileDetailResponse.isActive() == true`.

**Expected Result (PASS):** Target profile set active via `save()`, response reflects `isActive=true`.
**Expected Result (FAIL):** Exception thrown, or `save()` not called, or `isActive` still false.

**Current Status:** ðŸ”´ Not written
**Implementation Note:** Must call `assertOwner()` and the ARCHIVED-status check BEFORE `clearActiveForOwner()` to avoid mutating state on a rejected request.

---

### BABY-SW-TC-002 â€” ACCEPTED care-group member (non-owner) attempts switch â†’ 403

**Severity:** `CRITICAL`
**CWE:** `CWE-639 â€” Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `BabyOwnershipPolicy.assertOwner()`
**Test File:** `src/test/java/com/carebridge/backend/baby/policy/BabyOwnershipPolicyTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** ADR-BABY-004 (narrower than `BabyAccessPolicy.canView()`), Logic Issue L4

**Preconditions:** `FX-193-001` owned by `OWNER_ID`; caller is `CARE_MEMBER_ID` who WOULD pass `BabyAccessPolicy.canView()` (ACCEPTED care-group member) but is NOT the owner.

**Test Steps:**
1. Act: `babyOwnershipPolicy.assertOwner(makeInactiveBaby(), CARE_MEMBER_ID)`.
2. Assert: throws `BusinessException(403, BABY-006)`.
3. Assert: no care-group-member repository is queried by `BabyOwnershipPolicy` at all (verify no such dependency exists â€” the policy is single-rule ownership-only, unlike `BabyAccessPolicy`).

**Expected Result (PASS):** Exception thrown even for a caller who has legitimate VIEW access â€” proves switch is strictly narrower than view.
**Expected Result (FAIL):** No exception (incorrectly reused `BabyAccessPolicy`'s sharing-chain logic for switch).

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-003 â€” Clear-then-set sequencing: exactly one active row after switch

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyServiceImpl.switchActiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyServiceSwitchActiveTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** ADR-BABY-004 Decision, Â§6.4 Concurrency Note, Logic Issue L2

**Preconditions:** `FX-193-001` (target, inactive) + `FX-193-002` (currently active sibling), both owned by `OWNER_ID`.

**Test Steps:**
1. Act: `babyService.switchActiveBabyProfile(BABY_1_ID, OWNER_ID)`.
2. Assert: `verify(babyRepository, times(1)).clearActiveForOwner(OWNER_ID)` is invoked exactly once, and its Mockito invocation order is BEFORE `save(argThat(p -> p.getId().equals(BABY_1_ID)))` (use `InOrder`).

**Expected Result (PASS):** Calls occur in clear-then-set order, matching the sequence diagram Â§6.1 â€” prevents a transient double-active window.
**Expected Result (FAIL):** `save()` called before `clearActiveForOwner()`, or `clearActiveForOwner()` never called.

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-004 â€” Non-existent babyId â†’ 404 `BABY-001`

**Severity:** `MEDIUM`
**Feature Under Test:** `BabyServiceImpl.switchActiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyServiceSwitchActiveTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** TDS Â§10 Error Codes (reused `BABY-001`)

**Test Steps:**
1. Mock `babyRepository.findById(NONEXISTENT_ID)` â†’ `Optional.empty()`.
2. Act: `babyService.switchActiveBabyProfile(NONEXISTENT_ID, OWNER_ID)`.
3. Assert: throws `BusinessException(404, BABY-001)`.
4. Assert: `verify(babyRepository, never()).clearActiveForOwner(any())`; `verify(babyRepository, never()).save(any())`.

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-005 â€” ADMIN role does NOT bypass ownership check

**Severity:** `HIGH`
**Feature Under Test:** `BabyOwnershipPolicy.assertOwner()`
**Test File:** `src/test/java/com/carebridge/backend/baby/policy/BabyOwnershipPolicyTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** TDS Â§16 Auth Matrix Open Item OI-2, Logic Issue L3 (real code has no ADMIN branch)

**Test Steps:**
1. Act: `babyOwnershipPolicy.assertOwner(makeInactiveBaby(), ADMIN_ID)` (profile owned by `OWNER_ID` â‰  `ADMIN_ID`).
2. Assert: throws `BusinessException(403, BABY-006)`.

**Expected Result (PASS):** Exception thrown for the ADMIN caller â€” proves NO admin bypass, consistent with real `BabyAccessPolicy.canView()` behavior (no ADMIN branch), NOT with UC192 TDS's aspirational "ADMIN âœ… All" documentation.
**Expected Result (FAIL):** No exception (would silently implement an override that doesn't exist elsewhere in the codebase â€” AP-AI-003 Implicit Decision).

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-006 â€” Target profile status=ARCHIVED â†’ 409 `BABY-007`

**Severity:** `HIGH`
**Feature Under Test:** `BabyServiceImpl.switchActiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyServiceSwitchActiveTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** Â§6.3 State Machine invariant #2, TDS Â§10 Error Codes (`BABY-007`)

**Preconditions:** `FX-193-003` (`BABY_3_ID`, `status=ARCHIVED`, owned by `OWNER_ID`).

**Test Steps:**
1. Act: `babyService.switchActiveBabyProfile(BABY_3_ID, OWNER_ID)`.
2. Assert: throws `BusinessException(409, BABY-007)`.
3. Assert: `verify(babyRepository, never()).clearActiveForOwner(any())`; `verify(babyRepository, never()).save(any())`.

**Expected Result (PASS):** Exception thrown; no clearing of siblings' active state occurs for a rejected switch.
**Expected Result (FAIL):** No exception, or siblings incorrectly cleared before the archived-check fails.

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-007 â€” Idempotent re-switch to already-active baby â†’ 200, no-op state

**Severity:** `MEDIUM`
**Feature Under Test:** `BabyServiceImpl.switchActiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyServiceSwitchActiveTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** TDS Â§13 Gherkin scenario "Idempotent re-switch"

**Preconditions:** `FX-193-002` (`BABY_2_ID`, already `isActive=true`, owned by `OWNER_ID`).

**Test Steps:**
1. Act: `babyService.switchActiveBabyProfile(BABY_2_ID, OWNER_ID)`.
2. Assert: no exception; response `isActive == true` for `BABY_2_ID`.
3. Assert: `clearActiveForOwner()` and `save()` are still invoked (implementation does not special-case "already active" â€” it is safe/idempotent by construction per ADR-BABY-004, not by an early-return branch), and no OTHER sibling's `isActive` changes as a result.

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-008 â€” Happy-path switch emits `BABY_ACTIVE_PROFILE_SWITCHED` audit exactly once

**Severity:** `HIGH`
**Feature Under Test:** `BabyServiceImpl.switchActiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyServiceSwitchActiveTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** TDS Â§7.1 Domain Event Catalog, SRS POST-3

**Test Steps:**
1. Act: `babyService.switchActiveBabyProfile(BABY_1_ID, OWNER_ID)`.
2. Assert: `verify(auditService, times(1)).log(eq(AuditAction.BABY_ACTIVE_PROFILE_SWITCHED), eq(OWNER_ID), eq("BabyProfile"), eq(BABY_1_ID.toString()), any())`.

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-009 â€” Denied/blocked paths never emit audit

**Severity:** `HIGH`
**Feature Under Test:** `BabyServiceImpl.switchActiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyServiceSwitchActiveTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** Â§7.1 Domain Event Catalog; BR-PRIVACY audit-trail integrity

**Test Steps:**
1. Trigger the 403 path (`TC-002`) â€” assert `verify(auditService, never()).log(eq(AuditAction.BABY_ACTIVE_PROFILE_SWITCHED), any(), any(), any(), any())`.
2. Trigger the 404 path (`TC-004`) â€” assert same.
3. Trigger the 409 path (`TC-006`) â€” assert same.

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-010 â€” No JWT â†’ 401 (controller layer)

**Severity:** `HIGH`
**Feature Under Test:** `BabyController.switchActiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyControllerSwitchActiveTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-010`

**Test Steps:**
1. `@WebMvcTest(BabyController.class)`, no `Authorization` header.
2. `mockMvc.perform(patch("/api/v1/babies/{id}/active", BABY_1_ID))`.
3. Assert status 401.

**Current Status:** ðŸ”´ Not written

---

### SECURITY TEST CASES

---

### BABY-SW-TC-SEC-001 â€” IDOR: Mother B switches Mother A's baby (full auth chain)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 â€” Broken Access Control`
**CWE:** `CWE-639`
**Legal:** PDPA â€” unauthorized state mutation of another data subject's baby profile
**Feature Under Test:** `BabyController.switchActiveBabyProfile()` + `BabyServiceImpl.switchActiveBabyProfile()` (full chain)
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyControllerSwitchActiveTest.java`
**TDD Phase:** ðŸ”´ RED

**Preconditions:** Authenticated as `OTHER_MOTHER_ID` (role `MOTHER`, passes controller `isAuthenticated()` gate) targeting `BABY_1_ID` owned by `OWNER_ID`.

**Test Steps (Attack Simulation):**
1. Authenticate as `OTHER_MOTHER_ID`.
2. `PATCH /api/v1/babies/{BABY_1_ID}/active`.
3. Assert response is `403 BABY-006`.
4. Assert target profile's DB/mock state unchanged (`isActive` unchanged, no `save()`/`clearActiveForOwner()` invoked, no audit emitted).

**Expected Result (PASS = safe):** `403`, no state mutation.
**Expected Result (FAIL = vulnerability):** `200` returned, or another Mother's baby-profile active flag is mutated.

**Current Status:** ðŸ”´ Not written

---

### INTEGRATION TEST CASES

---

### BABY-SW-TC-INT-001 â€” Full stack: owner switches active baby, DB invariant holds

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `PATCH /api/v1/babies/{id}/active` â†’ `BabyController` â†’ `BabyServiceImpl` â†’ `BabyOwnershipPolicy` â†’ `BabyProfileRepository` â†’ PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/baby/integration/BabySwitchActiveIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-012`

**Preconditions:** PostgreSQL Testcontainer running; Flyway migrated (includes `V20260703100100` â€” new `is_active` column + `ux_baby_profiles_owner_active` partial unique index); seed two `baby_profiles` rows for the same owner: one `isActive=true`, one `isActive=false`.

**Test Steps:**
1. Seed both profiles via JPA.
2. `mockMvc.perform(patch("/api/v1/babies/{id}/active", inactiveBabyId).header("Authorization", ownerJwt))`.
3. Assert status 200, response body `isActive: true`.

**DB Assertion:**
```java
BabyProfile switched = babyRepository.findById(inactiveBabyId).orElseThrow();
BabyProfile sibling  = babyRepository.findById(previouslyActiveBabyId).orElseThrow();
assertThat(switched.isActive()).isTrue();
assertThat(sibling.isActive()).isFalse();

long activeCount = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM baby_profiles WHERE owner_user_id = ? AND is_active = true",
    Long.class, ownerId);
assertThat(activeCount).isEqualTo(1L); // ADR-BABY-004 invariant
```

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-INT-002 â€” Full stack: archived target â†’ 409, DB state unchanged

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow, ARCHIVED-rejection path
**Test File:** `src/test/java/com/carebridge/backend/baby/integration/BabySwitchActiveIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-013`

**Test Steps:**
1. Seed `baby_profiles` row `status=ARCHIVED`, `isActive=false`, owned by test Mother.
2. `PATCH /api/v1/babies/{id}/active` authenticated as owner.
3. Assert 409, error code `BABY-007`.
4. DB assertion: `is_active` still `false` for the target row.

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-INT-003 â€” Full stack: non-owner â†’ 403, DB state unchanged

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow, ownership denial path
**Test File:** `src/test/java/com/carebridge/backend/baby/integration/BabySwitchActiveIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-014`

**Test Steps:**
1. Seed `baby_profiles` row owned by Mother A, `isActive=false`.
2. `PATCH /api/v1/babies/{id}/active` authenticated as Mother B (different seeded `MOTHER`-role user).
3. Assert 403, error code `BABY-006`.
4. DB assertion: `is_active` unchanged.

**Current Status:** ðŸ”´ Not written

---

### BABY-SW-TC-INT-004 â€” Post-switch, UC192's `getBabyProfile()`/`listBabyProfiles()` correctly report `isActive`

**Severity:** `MEDIUM`
**Feature Under Test:** Cross-check between new `switchActiveBabyProfile()` and existing UC192 read endpoints (Logic Issue L1)
**Test File:** `src/test/java/com/carebridge/backend/baby/integration/BabySwitchActiveIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-015`
**Oracle Source:** TDS Â§11.3 Cháº·ng 5 (required DTO side-effect edit)

**Test Steps:**
1. Seed two profiles for the same owner.
2. `PATCH /api/v1/babies/{id}/active` for one of them.
3. `GET /api/v1/babies/{id}` (UC192, unchanged endpoint) for both profiles.
4. `GET /api/v1/babies` (UC32 list, unchanged endpoint).

**Expected Result (PASS):** The switched profile's `GET` response and its entry in the list response both show `isActive: true`; the sibling shows `isActive: false` in both. This confirms the required (not scope-creep) mapping-code edit in `BabyServiceImpl.getBabyProfile()`/`listBabyProfiles()` was applied.
**Expected Result (FAIL):** `isActive` field missing or always `false` in `GET`/list responses despite a successful switch (regression risk explicitly flagged by TDS Â§11.3 Cháº·ng 5).

**Current Status:** ðŸ”´ Not written

---

### MOBILE TEST CASES (flutter_test)

---

### BABY-SW-TC-MOB-001 â€” Switching active baby updates selected-baby widget state

**Severity:** `MEDIUM`
**Feature Under Test:** Mobile baby-switcher widget calling `PATCH /api/v1/babies/{id}/active` and updating local state
**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_switch_active_widget_test.dart`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-016`

**Preconditions:** Mock `BabyService.switchActiveBabyProfile()` (mobile service layer, analogous to `HealthMetricService` pattern already used in UC188's mobile client) returns a `BabyProfileDetailResponse` with `isActive: true`.

**Test Steps:**
1. `pumpWidget` a baby-list/switcher widget showing 2 baby profile chips.
2. Tap the inactive baby's chip.
3. `await tester.pump()`.
4. Assert the tapped chip now renders with the "active" visual indicator (e.g., highlighted border/checkmark) and the previously-active chip no longer does.

**Expected Result (PASS):** Exactly one chip shows the active indicator after the tap, matching the mocked API response.
**Expected Result (FAIL):** Both chips show active, neither does, or the UI does not update after the API call resolves.

**Current Status:** ðŸ”´ Not written
**Implementation Note:** Requires the mobile `BabyService` to expose a `switchActiveBabyProfile(String babyId)` method calling `PATCH /api/v1/babies/{babyId}/active` â€” does not exist yet, must be added alongside the backend change.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | ðŸ”´ RED confirmed | ðŸŸ¢ GREEN (commit) | ðŸ”µ REFACTOR note |
|-------|--------------|----------------------|------------------------|------------------------|
| `BABY-SW-TC-001` | `BabyServiceSwitchActiveTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-002` | `BabyOwnershipPolicyTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-003` | `BabyServiceSwitchActiveTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-004` | `BabyServiceSwitchActiveTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-005` | `BabyOwnershipPolicyTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-006` | `BabyServiceSwitchActiveTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-007` | `BabyServiceSwitchActiveTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-008` | `BabyServiceSwitchActiveTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-009` | `BabyServiceSwitchActiveTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-010` | `BabyControllerSwitchActiveTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-SEC-001` | `BabyControllerSwitchActiveTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-INT-001` | `BabySwitchActiveIntegrationTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-INT-002` | `BabySwitchActiveIntegrationTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-INT-003` | `BabySwitchActiveIntegrationTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-INT-004` | `BabySwitchActiveIntegrationTest.java` | `[ ]` | `[ ]` | |
| `BABY-SW-TC-MOB-001` | `baby_switch_active_widget_test.dart` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 â€” GATE-2)

> **Scope note:** `BabyServiceImpl.createBabyProfile()`, `listBabyProfiles()`, `getBabyProfile()`, and `BabyController`'s existing 3 endpoints are **pre-existing, shipped code from UC31/UC32/UC192** (confirmed by reading the real files â€” `IBabyService` currently declares exactly these 3 methods; `BabyController` currently declares exactly these 3 mappings; `BabyOwnershipPolicy` does not exist anywhere yet; `BabyProfileRepository` has no `clearActiveForOwner()`/`findByIdAndOwnerUserId()`). The `BabyServiceImplTest.java` file has 7 existing PASSING tests for UC192/UC32/UC31 that **must remain green throughout** â€” they are NOT re-stubbed. The Red Gate below is scoped **exclusively to the new `switchActiveBabyProfile()` method, the new `BabyOwnershipPolicy` class, and the new controller endpoint**.

**Stub cho Red Phase:**

```java
// IBabyService.java â€” add new method signature (existing 3 methods UNCHANGED)
public interface IBabyService {
    CreateBabyProfileResponse createBabyProfile(CreateBabyProfileRequest request, UUID callerId); // unchanged
    List<BabyProfileDetailResponse> listBabyProfiles(UUID callerId); // unchanged
    BabyProfileDetailResponse getBabyProfile(UUID profileId, UUID callerId); // unchanged

    BabyProfileDetailResponse switchActiveBabyProfile(UUID babyId, UUID callerId); // NEW â€” UC193
}

// BabyServiceImpl.java â€” add stub, 3 existing method bodies UNCHANGED
@Service
@Transactional
@RequiredArgsConstructor
public class BabyServiceImpl implements IBabyService {
    // ... existing createBabyProfile(), listBabyProfiles(), getBabyProfile() unchanged ...

    @Override
    public BabyProfileDetailResponse switchActiveBabyProfile(UUID babyId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub");
    }
}

// BabyOwnershipPolicy.java â€” NEW file, entirely new class
@Component
public class BabyOwnershipPolicy {
    public void assertOwner(BabyProfile profile, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub");
    }
}

// BabyController.java â€” add new endpoint, existing 3 mappings UNCHANGED
@RestController
@RequestMapping("/api/v1/babies")
@RequiredArgsConstructor
public class BabyController {
    // ... existing createBabyProfile(), listBabyProfiles(), getBabyProfile() unchanged ...

    @PatchMapping("/{babyId}/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BabyProfileDetailResponse>> switchActiveBabyProfile(
            @PathVariable UUID babyId, Principal principal) {
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (náº¿u PASS báº¥t thÆ°á»ng) |
|-------|-----------------|--------------|-------------|-----------------------------------------|
| `BABY-SW-TC-001` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `BABY-SW-TC-002` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `BABY-SW-TC-003` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `BABY-SW-TC-INT-001` | `throw` (via controller 500) | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| Existing `BabyServiceImplTest.java` (7 tests) | unchanged code | ðŸŸ¢ stays PASS | â˜ PASS â˜ FAIL | If FAIL â†’ stub touched existing methods, revert |

> Note: `BabyOwnershipPolicy` tests fail at **compile time** initially since the class does not exist yet â€” this is an acceptable/expected Red Gate signal.

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled during implementation)
- Táº¥t cáº£ FAIL (new tests) vÃ  PASS (existing 7 tests)? â˜ Yes â†’ **GATE-2 PASS** (T2â†’T3)
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-BABY-IMP-003` reviewed and Approved
- [ ] ADR-BABY-004 confirmed Accepted (TDS Â§3)
- [ ] Logic Issues (Â§2) confirmed with Tech Lead, especially L1 (isActive field genuinely new) and L3 (no ADMIN bypass, contradicts UC192 doc)
- [ ] Flyway migration `V20260703100100` reviewed/approved on staging (TDS Â§11.2)
- [ ] Confirmed existing 7 `BabyServiceImplTest.java` tests (UC192/UC32/UC31) pass BEFORE this feature's changes

### Exit Criteria
- [ ] `./mvnw test` â€” all unit tests green, including the 7 pre-existing UC192/UC32/UC31 tests
- [ ] `./mvnw verify` â€” integration tests green (Testcontainers), including partial-unique-index invariant check
- [ ] Test coverage â‰¥ 80% lines for `switchActiveBabyProfile()`, `BabyOwnershipPolicy`, new controller endpoint
- [ ] No business logic in `BabyController` (validation/mapping only)
- [ ] Mobile: `flutter test` green for `baby_switch_active_widget_test.dart`
- [ ] Migration applied successfully; `\d baby_profiles` confirms `ux_baby_profiles_owner_active` index present

**Exit Criteria bá»• sung â€” CASE 2.0:**
- [ ] Red Gate (Â§5.1) â€” all new tests FAIL against throwing stub; existing 7 tests unaffected
- [ ] Contract Existence â€” `./mvnw compile` clean, no hallucinated imports
- [ ] Props Isolation â€” all entities built via `SwitchActiveBabyTestFactory`, no shared mutable state
- [ ] Oracle Source â€” every assert traces to ADR-BABY-004 or existing schema fact
- [ ] Negative-mutation checks present: DB `is_active`/`clearActiveForOwner()` unchanged on every denial path (403/404/409); no audit event on denial

### Suspension Criteria
- Product has not resolved Open Item OI-1 (auto-activation of first baby) â€” does NOT block this TDS's scope (explicitly out of scope), but a follow-up TDS may be needed later
- Tech Lead disagrees with ADR-BABY-004's "no ADMIN bypass" stance (Open Item OI-2) â€” would require TDS revision first

---

## 7. Rollback Plan

```bash
# Revert migration
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS ux_baby_profiles_owner_active;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.baby_profiles DROP COLUMN IF EXISTS is_active;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260703100100';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/baby/
git checkout -- src/main/resources/db/migration/V20260703100100__add_baby_profile_active_flag.sql
git checkout -- src/test/java/com/carebridge/backend/baby/
# NOTE: do NOT blanket-revert createBabyProfile()/listBabyProfiles()/getBabyProfile() (UC31/UC32/UC192)
# if they were already merged independently â€” revert only UC193-specific additions.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u | Check | Gate cháº·n |
|-------|--------------|--------------|-------|---------------|
| AP-AI-001 | Unconstrained Generation | TC khÃ´ng reference ADR-BABY-004 | â˜ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS vá»›i throwing stub | â˜ | G-2 â˜… |
| AP-AI-003 | Implicit Decision | Test assumes ADMIN bypass exists (no ADR/code support) | â˜ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies `BabyController` doing ownership/state-invariant logic directly | â˜ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `BabyActiveService`/`BabyController2` or other class not in TDS Â§8 | â˜ | G-3 |
| AP-AI-006 (custom) | Duplicated Controller/Service | Test expects a second `@RequestMapping("/api/v1/baby...")` instead of extending existing `BabyController` | â˜ | G-1 |
| AP-AI-007 (custom) | Ownership-scope creep | Test grants care-group member switch access (reuses `BabyAccessPolicy.canView()` semantics instead of `BabyOwnershipPolicy`) | â˜ | G-1 |

**Káº¿t quáº£ review:**
- [ ] KhÃ´ng phÃ¡t hiá»‡n anti-pattern nÃ o â†’ Test-Spec approved
- [ ] PhÃ¡t hiá»‡n AP â†’ ghi vÃ o báº£ng dÆ°á»›i

| AP detected | TC ID | MÃ´ táº£ | Fix action | Fixed? |
|--------------|-------|-------|------------|--------|
| â€” | â€” | â€” | â€” | â˜ |

---

*Test-Spec for UC193 Switch Active Baby Profile â€” Status: Draft. Awaiting review before Approved.*
