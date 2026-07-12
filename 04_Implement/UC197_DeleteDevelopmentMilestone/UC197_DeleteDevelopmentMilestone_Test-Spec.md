# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-197 Delete Development Milestone

**Document ID:** `CB-BABY-TDD-005`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented — 2026-07-10 (targeted baby/carejourney backend tests PASS; full regression blocked by non-baby Family/Exercise/Auth/Triage failures)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 â€” Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] â€” Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `TV2-BÃ¡ch`
**Classification:** `Internal â€” Confidential`

**References:**
- TDS: `04_Implement/UC197_DeleteDevelopmentMilestone/UC197_DeleteDevelopmentMilestone_TDS.md` (CB-BABY-IMP-005)
- Companion TDS: `04_Implement/UC196_UpdateDevelopmentMilestone/UC196_UpdateDevelopmentMilestone_TDS.md` (entity/migration/canManage() owner)
- SRS: Â§3.3.12.6
- Schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` + `V20260707120000__add_development_milestone_status_columns.sql`

> **Quy Æ°á»›c TDD:** Viáº¿t test (`.java`) â†’ cháº¡y â†’ xÃ¡c nháº­n FAIL ðŸ”´ â†’ implement â†’ PASS ðŸŸ¢ â†’ refactor ðŸ”µ.
> KhÃ´ng dÃ¹ng PII tháº­t â€” chá»‰ dÃ¹ng SYNTHETIC data.

---

## CHANGELOG

| NgÃ y | NgÆ°á»i thá»±c hiá»‡n | Ná»™i dung thay Ä‘á»•i |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented after targeted baby/carejourney backend test pass; full regression remains blocked outside baby scope. |
| 2026-07-03 | AI Agent | Khá»Ÿi táº¡o TDD spec cho UC-197 Delete Development Milestone |

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
| **Feature / Gap ID** | `UC-197` |
| **Module** | `DeleteDevelopmentMilestone â€” baby` |
| **Spec gá»‘c** | `CB-BABY-IMP-005` |
| **Priority** | ðŸŸ¡ P2 (Medium theo SRS) |
| **Sprint** | `Sprint 4 (Device Sync And Care Edge Cases)` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `baby (BabyProfile, BabyAccessPolicy.canManage() â€” UC196)`, `DevelopmentMilestone` entity/repository (UC196, shared) |
| **Downstream Consumers** | â€” |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC197 TDS Â§17`, `ADR-BABY-006`, `ADR-BABY-007`, `ADR-BABY-008` |
| **Constraints Injected** | C1 (recordStatus-only write), C2 (soft-delete only, no hard-delete), C3 (canManage strict ownership), C4 (double-delete â†’ 404), C5 (no new migration) |
| **Model** | `Claude (Sonnet 5)` |
| **Trust Level** | `T1 â†’ T2 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gá»‘c (sai / thiáº¿u) | Thá»±c táº¿ (schema / policy) | Fix Ã¡p dá»¥ng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Â§3.3.12.6: "Soft-deletes a Mother-recorded development milestone" â€” khÃ´ng Ä‘á»‹nh nghÄ©a cÆ¡ cháº¿ soft-delete cá»¥ thá»ƒ | `development_milestones` khÃ´ng cÃ³ cá»™t soft-delete nÃ o trÆ°á»›c migration `V20260707120000`. ADR-BABY-008 Ä‘á»‹nh nghÄ©a `record_status` (ACTIVE/DELETED), TÃCH BIá»†T khá»i `milestone_status` (UC196) | Test PHáº¢I verify delete chá»‰ ghi `record_status`, khÃ´ng Ä‘á»¥ng `milestone_status` (Â§4 DISAMB test) |
| L2 | SRS khÃ´ng nÃ³i rÃµ hÃ nh vi double-delete | ADR-BABY-008 quyáº¿t Ä‘á»‹nh: double-delete â†’ 404 (nháº¥t quÃ¡n UC194/195 "treat as not-found" pattern), KHÃ”NG 409 | Test PHáº¢I verify gá»i delete láº§n 2 tráº£ 404, khÃ´ng lá»—i 500 |
| L3 | SRS khÃ´ng nÃ³i rÃµ cÃ³ hard-delete hay khÃ´ng | ADR-BABY-008: LUÃ”N soft-delete, row khÃ´ng bao giá» bá»‹ xoÃ¡ váº­t lÃ½ (BR-PRIVACY retention) | Test PHáº¢I verify `COUNT(*)` khÃ´ng Ä‘á»•i trÆ°á»›c/sau khi xoÃ¡ |

---

## 3. Test Design Specification (TDS)

### TDS-01 â€” Scope

```
DeleteDevelopmentMilestone bao gá»“m cÃ¡c layer:
â”œâ”€â”€ Domain (MilestoneRecordStatus â€” pure enum, shared vá»›i UC196)
â”œâ”€â”€ Services (DevelopmentMilestoneServiceImpl.deleteMilestone() â€” mock JPA Repository + BabyAccessPolicy vá»›i Mockito)
â”œâ”€â”€ Policy (BabyAccessPolicy.canManage() â€” reuse test double tá»« UC196)
â”œâ”€â”€ Controller (DevelopmentMilestoneController.deleteMilestone() â€” mock Service vá»›i @WebMvcTest)
â””â”€â”€ Integration (Testcontainers PostgreSQL vá»›i @SpringBootTest â€” verify soft-delete row retention)
```

### TDS-02 â€” Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-197 Â§3.3.12.6` | Soft-delete behavior |
| `ADR-BABY-006` | milestone_status vs record_status disambiguation |
| `ADR-BABY-007` | canManage() strict ownership |
| `ADR-BABY-008` | soft-delete only, double-delete â†’ 404 |
| `BR-RBAC` | Owner-only mutation |
| `BR-PRIVACY` | Row retention â€” no hard-delete |

### TDS-03 â€” Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner soft-deletes milestone | `DevelopmentMilestoneServiceImpl.deleteMilestone()` | `MILESTONE-DEL-TC-001` |
| TC-COND-002 | Row NOT hard-deleted (still queryable) | `save()` not `delete()` | `MILESTONE-DEL-TC-002` |
| TC-COND-003 | Care group member (ACCEPTED, non-owner) â†’ 403 | `BabyAccessPolicy.canManage()` | `MILESTONE-DEL-TC-003` |
| TC-COND-004 | Unrelated user â†’ 403 | `canManage()` | `MILESTONE-DEL-TC-004` |
| TC-COND-005 | Non-existent milestone â†’ 404 | repo lookup | `MILESTONE-DEL-TC-005` |
| TC-COND-006 | Double-delete (already DELETED) â†’ 404 | `recordStatus` guard | `MILESTONE-DEL-TC-006` |
| TC-COND-007 | [CRITICAL] Delete does NOT touch milestoneStatus | ADR-BABY-006 disambiguation | `MILESTONE-DEL-TC-DISAMB-001` |
| TC-COND-008 | IDOR â€” path babyId spoofed, ownership from DB only | ADR-BABY-007 / BR-RBAC | `MILESTONE-DEL-TC-SEC-001` |

### TDS-04 â€” Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | `recordStatus` FSM: ACTIVEâ†’DELETED (terminal) | Â§6.3 State Machine trong TDS |
| Error Guessing | Double-delete, IDOR via path babyId mismatch, care group ACCEPTED privilege escalation | Security/robustness-focused |
| Equivalence Partitioning | recordStatus âˆˆ {ACTIVE, DELETED} â€” 2 classes | Guard condition coverage |

### TDS-05 â€” Test Data Requirements

| Fixture ID | Type | Value / Logic | Má»¥c Ä‘Ã­ch |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{ milestoneId: MILESTONE-001, babyId: BABY-001, milestoneStatus: ACHIEVED, recordStatus: ACTIVE }` | Happy path |
| `FX-002` | DB seed | `{ milestoneId: MILESTONE-002, babyId: BABY-001, milestoneStatus: PENDING, recordStatus: DELETED }` | Double-delete reject |
| `FX-003` | DB seed | `{ babyId: BABY-001, ownerUserId: MOTHER-001 }` (BabyProfile) | Ownership chain |
| `FX-004` | DB seed | `{ careGroupId: CG-001, ownerAccountId: MOTHER-001, memberUserId: MOTHER-002, inviteStatus: ACCEPTED }` | Care group non-owner reject |
| `FX-005` | JWT | `{ sub: MOTHER-001, role: MOTHER }` | Auth context â€” owner |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 â€” Báº®T BUá»˜C)

```java
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// CASE 2.0 â€” Props Isolation Pattern
// Reuse DevelopmentMilestoneTestFactory tá»« UC196 test suite,
// má»Ÿ rá»™ng thÃªm factory riÃªng cho cÃ¡c case xoÃ¡.
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
class DevelopmentMilestoneDeleteTestFactory {

    static DevelopmentMilestone makeActiveAchievedMilestone() {
        DevelopmentMilestone m = new DevelopmentMilestone();
        m.setId(UUID.fromString("00000000-0000-0000-0000-000000000200"));
        m.setBabyId(UUID.fromString("00000000-0000-0000-0000-000000000060"));
        m.setMilestoneType("first_smile");
        m.setAchievedDate(LocalDate.of(2026, 5, 1));
        m.setNote("first documented smile");
        m.setMilestoneStatus(MilestoneAchievementStatus.ACHIEVED);
        m.setRecordStatus(MilestoneRecordStatus.ACTIVE);
        return m;
    }

    static DevelopmentMilestone makeAlreadyDeletedMilestone() {
        DevelopmentMilestone m = makeActiveAchievedMilestone();
        m.setId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        m.setMilestoneStatus(MilestoneAchievementStatus.PENDING);
        m.setRecordStatus(MilestoneRecordStatus.DELETED);
        return m;
    }

    static BabyProfile makeOwnerProfile() {
        BabyProfile p = new BabyProfile();
        p.setId(UUID.fromString("00000000-0000-0000-0000-000000000060"));
        p.setOwnerUserId(UUID.fromString("00000000-0000-0000-0000-000000000001")); // MOTHER-001
        p.setNickname("Bean");
        return p;
    }
}
```

---

### MILESTONE-DEL-TC-001 â€” Owner soft-deletes milestone â†’ 200

**Severity:** `CRITICAL`
**Feature Under Test:** `DevelopmentMilestoneServiceImpl.deleteMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/DevelopmentMilestoneServiceImplTest.java`
**TDD Phase:** ðŸ”´ RED â€” chÆ°a implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS Â§3.3.12.6`

**Preconditions:** FX-001, FX-003, FX-005

**Test Steps:**
1. Mock `milestoneRepository.findById(MILESTONE-001)` â†’ `makeActiveAchievedMilestone()`
2. Mock `babyProfileRepository.findById(BABY-001)` â†’ `makeOwnerProfile()`
3. Mock `babyAccessPolicy.canManage(profile, MOTHER-001)` â†’ true
4. Call `deleteMilestone(MILESTONE-001, MOTHER-001)`

**Expected Result (PASS):** No exception; `milestoneRepository.save()` called exactly once with entity where `recordStatus == DELETED`

**Expected Result (FAIL):** Exception thrown, or `save()` not called, or `milestoneRepository.delete()`/`deleteById()` called instead (hard-delete violation)

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-DEL-TC-002 â€” Row is NOT hard-deleted

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-BABY-008 (soft-delete only)
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-BABY-008`

**Test Steps:**
1. Same setup as `MILESTONE-DEL-TC-001`
2. Call `deleteMilestone(MILESTONE-001, MOTHER-001)`
3. Verify `milestoneRepository.delete(any())` â€” **never invoked**
4. Verify `milestoneRepository.deleteById(any())` â€” **never invoked**

**Expected Result (PASS):** `verify(milestoneRepository, never()).delete(any())` and `verify(milestoneRepository, never()).deleteById(any())` both pass; only `save()` is invoked

**Expected Result (FAIL):** If `delete()`/`deleteById()` is called, this is a **direct violation of ADR-BABY-008** (BR-PRIVACY retention breach)

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-DEL-TC-003 â€” Care group member (ACCEPTED, non-owner) â†’ 403

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyAccessPolicy.canManage()` â€” strict ownership
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-BABY-007`

**Preconditions:** FX-004 (MOTHER-002 is ACCEPTED care group member, NOT owner)

**Test Steps:**
1. Mock `babyProfileRepository.findById(BABY-001)` â†’ profile owned by MOTHER-001
2. Mock `babyAccessPolicy.canManage(profile, MOTHER-002)` â†’ **false**
3. Call `deleteMilestone(MILESTONE-001, MOTHER-002)`

**Expected Result (PASS):** throws `BusinessException` `MILESTONE-002` (403); `milestoneRepository.save()` never invoked

**Current Status:** ðŸ”´ Not written
**Implementation Note:** Same pattern as UC196's `MILESTONE-UPD-TC-003` â€” validates `canManage()` reuse consistency across both mutation UCs.

---

### MILESTONE-DEL-TC-004 â€” Unrelated user â†’ 403

**Severity:** `CRITICAL`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-004`

**Expected Result:** throws `BusinessException` `MILESTONE-002` (403)

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-DEL-TC-005 â€” Non-existent milestone â†’ 404

**Severity:** `MEDIUM`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Mock `milestoneRepository.findById(NONEXISTENT)` â†’ `Optional.empty()`
2. Call `deleteMilestone(NONEXISTENT, MOTHER-001)`

**Expected Result:** throws `BusinessException` `MILESTONE-001` (404)

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-DEL-TC-006 â€” Double-delete (already DELETED) â†’ 404

**Severity:** `HIGH`
**Feature Under Test:** `recordStatus` guard, idempotency
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-BABY-008`

**Preconditions:** FX-002 (`makeAlreadyDeletedMilestone()`)

**Test Steps:**
1. Mock `milestoneRepository.findById(MILESTONE-002)` â†’ entity with `recordStatus=DELETED`
2. Call `deleteMilestone(MILESTONE-002, MOTHER-001)`

**Expected Result (PASS):** throws `BusinessException` `MILESTONE-001` (404) â€” **NOT** 409, **NOT** 500; `save()` never invoked (no further side-effect)

**Expected Result (FAIL):** 500 error, or successful 200 (would mean the guard is missing and `save()` is called redundantly)

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-DEL-TC-DISAMB-001 â€” [CRITICAL] Delete does NOT touch milestoneStatus

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-BABY-006 disambiguation invariant
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-BABY-006`

**Test Steps:**
1. Entity fixture: `milestoneStatus=ACHIEVED, recordStatus=ACTIVE`
2. Call `deleteMilestone(MILESTONE-001, MOTHER-001)`
3. Capture the entity passed to `milestoneRepository.save(entityCaptor.capture())`

**Expected Result (PASS):**
- `entityCaptor.getValue().getRecordStatus() == DELETED`
- `entityCaptor.getValue().getMilestoneStatus() == ACHIEVED` (**UNCHANGED** â€” this is the disambiguation assertion)

**Expected Result (FAIL):** If `getMilestoneStatus()` is anything other than the original `ACHIEVED` value, or is null, or reset to a default â€” the service is incorrectly conflating the two status concepts, a **direct violation of ADR-BABY-006**.

**Current Status:** ðŸ”´ Not written
**Implementation Note:** This is the mirror test to UC196's `MILESTONE-UPD-TC-DISAMB-001` â€” together they prove the two status concepts are write-isolated in BOTH directions (UC196 never sets recordStatus; UC197 never sets milestoneStatus).

---

### SECURITY TEST CASES

---

### MILESTONE-DEL-TC-SEC-001 â€” IDOR: spoofed path babyId does not bypass ownership check

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 â€” Broken Access Control`
**CWE:** `CWE-639 â€” Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC`
**Feature Under Test:** `DevelopmentMilestoneController.deleteMilestone()` + `DevelopmentMilestoneServiceImpl`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/DevelopmentMilestoneControllerTest.java`
**TDD Phase:** ðŸ”´ RED

**Preconditions:**
- MILESTONE-001 actually belongs to BABY-001 (owned by MOTHER-001)
- Attacker MOTHER-999 sends request with path `babyId = BABY-999` (a baby MOTHER-999 owns) but `milestoneId = MILESTONE-001` (belongs to a different baby)

**Test Steps (Attack Simulation):**
1. `DELETE /api/v1/babies/BABY-999/milestones/MILESTONE-001` with JWT for MOTHER-999
2. Service resolves `milestone.getBabyId()` from DB (= BABY-001), NOT from the path
3. Load `BabyProfile` for BABY-001 â†’ owner = MOTHER-001
4. `canManage(profile, MOTHER-999)` â†’ false

**Expected Result (PASS = há»‡ thá»‘ng an toÃ n):** `403 Forbidden` `MILESTONE-002` â€” path `babyId` mismatch does not grant access; MILESTONE-001 remains `recordStatus=ACTIVE` (unaffected)

**Expected Result (FAIL = lá»— há»•ng tá»“n táº¡i):** MILESTONE-001 gets deleted despite the caller not owning its actual baby

**Current Status:** ðŸ”´ Not written

---

### INTEGRATION TEST CASES

---

### MILESTONE-DEL-TC-INT-001 â€” Full flow: DELETE soft-deletes, row retained, milestoneStatus untouched

**Severity:** `HIGH`
**Feature Under Test:** Full flow: HTTP DELETE â†’ Service â†’ Repository â†’ PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/baby/DevelopmentMilestoneIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-001, TC-COND-002, TC-COND-007`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migrations applied automatically (including `V20260707120000`)
- Seed: BABY-001 owned by MOTHER-001; MILESTONE-001 with `milestoneStatus=ACHIEVED, recordStatus=ACTIVE`

**Test Steps:**
1. `SELECT COUNT(*) FROM development_milestones WHERE baby_id = ?` â€” capture `countBefore`
2. `DELETE /api/v1/babies/{babyId}/milestones/{milestoneId}` with JWT for MOTHER-001
3. Assert response 200
4. `SELECT COUNT(*) FROM development_milestones WHERE baby_id = ?` â€” capture `countAfter`
5. `SELECT milestone_status, record_status FROM development_milestones WHERE milestone_id = ?`

**Expected Result (PASS):**
- `countAfter == countBefore` (row retained, NOT hard-deleted)
- `record_status = 'DELETED'`
- `milestone_status = 'ACHIEVED'` (unchanged from seed value)

**DB Assertion:**
```java
long countBefore = milestoneRepository.count();
// ... perform DELETE via MockMvc/WebTestClient ...
long countAfter = milestoneRepository.count();
assertThat(countAfter).isEqualTo(countBefore);

DevelopmentMilestone record = milestoneRepository.findById(milestoneId).orElseThrow();
assertThat(record.getRecordStatus()).isEqualTo(MilestoneRecordStatus.DELETED);
assertThat(record.getMilestoneStatus()).isEqualTo(MilestoneAchievementStatus.ACHIEVED);
```

**Current Status:** ðŸ”´ Not written

---

### MOBILE WIDGET TEST CASES (Flutter/Dart)

---

### MILESTONE-DEL-TC-MOBILE-001 â€” Delete requires confirmation dialog

**Severity:** `HIGH`
**Feature Under Test:** `MilestoneListItem` / `MilestoneDetailScreen` (Flutter) â€” delete action
**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/delete_milestone_test.dart`
**TDD Phase:** ðŸ”´ RED

**Test Steps:**
1. Pump `MilestoneDetailScreen` for an existing milestone
2. Tap "Delete" button
3. Verify a confirmation `AlertDialog` appears
4. Tap "Cancel" in dialog

**Expected Result (PASS):** No API call fired; dialog dismissed; milestone still shown

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-DEL-TC-MOBILE-002 â€” Confirmed delete removes item from list and shows success

**Severity:** `HIGH`
**Feature Under Test:** Delete confirmation flow â€” success path
**TDD Phase:** ðŸ”´ RED

**Test Steps:**
1. Pump `MilestoneDetailScreen`, tap "Delete", tap "Confirm" in dialog
2. Mock API returns 200

**Expected Result (PASS):** API DELETE call fired with correct `milestoneId`; success snackbar shown; navigates back to milestone list; deleted item no longer visible in list

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-DEL-TC-MOBILE-003 â€” 403 response from server surfaces a permission error

**Severity:** `MEDIUM`
**Feature Under Test:** Error handling â€” mock HTTP client returns 403 `MILESTONE-002`
**TDD Phase:** ðŸ”´ RED

**Expected Result (PASS):** UI shows a permission-denied message; item is NOT removed from the local list (state remains consistent with server); no crash

**Current Status:** ðŸ”´ Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | ðŸ”´ RED confirmed | ðŸŸ¢ GREEN (commit) | ðŸ”µ REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MILESTONE-DEL-TC-001` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-DEL-TC-002` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-DEL-TC-003` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-DEL-TC-004` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-DEL-TC-005` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-DEL-TC-006` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-DEL-TC-DISAMB-001` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-DEL-TC-SEC-001` | `DevelopmentMilestoneControllerTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-DEL-TC-INT-001` | `DevelopmentMilestoneIntegrationTest.java:__` | `[ ]` | `___` | â€” |

### 5.1 Red Gate Protocol (CASE 2.0 â€” GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class DevelopmentMilestoneServiceImpl implements IDevelopmentMilestoneService {
    // ... updateMilestone() stub tá»« UC196 ...

    @Override
    public void deleteMilestone(UUID milestoneId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (náº¿u PASS báº¥t thÆ°á»ng) |
|-------|-------------|----------|--------|----------------------------------|
| `MILESTONE-DEL-TC-001` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `MILESTONE-DEL-TC-003` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `MILESTONE-DEL-TC-DISAMB-001` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Táº¥t cáº£ FAIL? â˜ Yes â†’ **GATE-2 PASS** (T1â†’T2) â†’ tiáº¿p tá»¥c implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-BABY-IMP-005` Ä‘Ã£ Ä‘Æ°á»£c review vÃ  approve
- [ ] Logic Issues (Â§2) Ä‘Ã£ confirm vá»›i Tech Lead
- [ ] UC196 Ä‘Ã£ implement `DevelopmentMilestone` entity, `DevelopmentMilestoneRepository`, `BabyAccessPolicy.canManage()`, vÃ  migration `V20260707120000` (dependency cá»©ng â€” UC197 KHÃ”NG thá»ƒ test Ä‘á»™c láº­p náº¿u thiáº¿u cÃ¡c thÃ nh pháº§n nÃ y)

### Exit Criteria (DoD)
- [ ] `./mvnw test` â€” táº¥t cáº£ unit tests xanh
- [ ] `./mvnw verify` â€” integration tests xanh (Testcontainers)
- [ ] Test coverage â‰¥ 80% cho method `deleteMilestone()`
- [ ] `MILESTONE-DEL-TC-DISAMB-001` PASS báº¯t buá»™c (gate riÃªng â€” khÃ´ng Ä‘Æ°á»£c skip)
- [ ] `MILESTONE-DEL-TC-002` (no hard-delete) PASS báº¯t buá»™c
- [ ] `MILESTONE-DEL-TC-006` (double-delete idempotency) PASS báº¯t buá»™c
- [ ] KhÃ´ng cÃ³ business logic trong `DevelopmentMilestoneController`
- [ ] `flutter test` xanh cho mobile widget tests

**Exit Criteria bá»• sung â€” CASE 2.0:**
- [ ] Red Gate (Â§5.1) â€” táº¥t cáº£ tests FAIL vá»›i throw stub trÆ°á»›c khi implement
- [ ] Contract Existence: `./mvnw compile 2>&1 | grep "error:"` â†’ no output
- [ ] Props Isolation: má»i test dÃ¹ng `DevelopmentMilestoneDeleteTestFactory`, khÃ´ng shared mutable state

### Suspension Criteria
- UC196 chÆ°a deploy (entity/migration/`canManage()` chÆ°a sáºµn sÃ ng)
- Migration `V20260707120000` conflict phÃ¡t hiá»‡n giá»¯a 2 PR song song

---

## 7. Rollback Plan

```bash
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/baby/
# Migration KHÃ”NG revert riÃªng á»Ÿ Ä‘Ã¢y â€” sá»Ÿ há»¯u bá»Ÿi UC196, xem UC196 Test-Spec Â§7
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u trong TDD spec | Check | Gate cháº·n |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC khÃ´ng reference ADR/TDS constraint | â˜ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS vá»›i throw stub (Â§5.1) | â˜ | G-2 â˜… |
| AP-AI-003 | Implicit Decision | Test mock `repository.delete()` thay vÃ¬ `save()` vá»›i recordStatus=DELETED (hard-delete assumption), hoáº·c dÃ¹ng `canView()` thay vÃ¬ `canManage()` | â˜ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller cÃ³ business logic | â˜ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type khÃ´ng tá»“n táº¡i trong codebase | â˜ | G-3 |

**Káº¿t quáº£ review:**

- [ ] KhÃ´ng phÃ¡t hiá»‡n anti-pattern nÃ o â†’ TDD spec approved
- [ ] PhÃ¡t hiá»‡n AP â†’ ghi vÃ o báº£ng dÆ°á»›i â†’ fix trÆ°á»›c khi implement

| AP detected | TC ID | MÃ´ táº£ | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| â€” | â€” | â€” | â€” | â˜ |

---

*TDD Template v2.0 â€” TÃ­ch há»£p CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
