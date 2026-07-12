# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-196 Update Development Milestone

**Document ID:** `CB-BABY-TDD-004`
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
- TDS: `04_Implement/UC196_UpdateDevelopmentMilestone/UC196_UpdateDevelopmentMilestone_TDS.md` (CB-BABY-IMP-004)
- SRS: Â§3.3.12.5
- Schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` + `V20260707120000__add_development_milestone_status_columns.sql`

> **Quy Æ°á»›c TDD:** Viáº¿t test (`.java`) â†’ cháº¡y â†’ xÃ¡c nháº­n FAIL ðŸ”´ â†’ implement â†’ PASS ðŸŸ¢ â†’ refactor ðŸ”µ.
> KhÃ´ng dÃ¹ng PII tháº­t â€” chá»‰ dÃ¹ng SYNTHETIC data.

---

## CHANGELOG

| NgÃ y | NgÆ°á»i thá»±c hiá»‡n | Ná»™i dung thay Ä‘á»•i |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented after targeted baby/carejourney backend test pass; full regression remains blocked outside baby scope. |
| 2026-07-03 | AI Agent | Khá»Ÿi táº¡o TDD spec cho UC-196 Update Development Milestone |

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
| **Feature / Gap ID** | `UC-196` |
| **Module** | `UpdateDevelopmentMilestone â€” baby` |
| **Spec gá»‘c** | `CB-BABY-IMP-004` |
| **Priority** | ðŸŸ¡ P2 (Medium theo SRS) |
| **Sprint** | `Sprint 4 (Device Sync And Care Edge Cases)` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY` |
| **Upstream Dependencies** | `baby (BabyProfile, BabyAccessPolicy â€” UC192)`, `development_milestones` table |
| **Downstream Consumers** | `UC197 Delete Development Milestone` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC196 TDS Â§17`, `ADR-BABY-006`, `ADR-BABY-007` |
| **Constraints Injected** | C1 (milestoneStatus-only write), C2 (canManage strict ownership), C3 (DELETED â†’ 404), C4 (path param not trusted), C5 (validation rules) |
| **Model** | `Claude (Sonnet 5)` |
| **Trust Level** | `T1 â†’ T2 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gá»‘c (sai / thiáº¿u) | Thá»±c táº¿ (schema / policy) | Fix Ã¡p dá»¥ng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Â§3.3.12.5: "Updates date, notes, or status" â€” khÃ´ng Ä‘á»‹nh nghÄ©a "status" lÃ  gÃ¬ | `development_milestones` KHÃ”NG cÃ³ cá»™t `status` nÃ o trong `V1__init_schema.sql`. ADR-BABY-006 Ä‘á»‹nh nghÄ©a `milestone_status` (achievement) tÃ¡ch biá»‡t `record_status` (soft-delete, do UC197 sá»Ÿ há»¯u) | Test PHáº¢I verify update chá»‰ ghi `milestone_status`, khÃ´ng Ä‘á»¥ng `record_status` (Â§4 DISAMB test) |
| L2 | SRS khÃ´ng nÃ³i rÃµ ai Ä‘Æ°á»£c sá»­a â€” chá»‰ ghi Primary Actor = Mother | `BabyAccessPolicy.canView()` (UC192, Ä‘Ã£ ship) cho phÃ©p cáº£ care group ACCEPTED member â€” náº¿u tÃ¡i sá»­ dá»¥ng nguyÃªn cho mutation sáº½ over-permission | Test PHáº¢I verify care group member (ACCEPTED, non-owner) nháº­n 403 khi update â€” dÃ¹ng `canManage()` má»›i, KHÃ”NG `canView()` |
| L3 | SRS khÃ´ng Ä‘á»‹nh nghÄ©a validation rule cho "status" | Business rule tá»± suy luáº­n: `status=ACHIEVED` cáº§n cÃ³ `achievedDate` Ä‘á»ƒ cÃ³ Ã½ nghÄ©a nghiá»‡p vá»¥ | Test PHáº¢I verify `status=ACHIEVED` thiáº¿u `achievedDate` (cáº£ cÅ© vÃ  má»›i) â†’ 400 |

---

## 3. Test Design Specification (TDS)

### TDS-01 â€” Scope

```
UpdateDevelopmentMilestone bao gá»“m cÃ¡c layer:
â”œâ”€â”€ Domain (MilestoneAchievementStatus/MilestoneRecordStatus â€” pure enums)
â”œâ”€â”€ Services (DevelopmentMilestoneServiceImpl â€” mock JPA Repository + BabyAccessPolicy vá»›i Mockito)
â”œâ”€â”€ Policy (BabyAccessPolicy.canManage() â€” unit test riÃªng, isolate khá»i canView())
â”œâ”€â”€ Controller (DevelopmentMilestoneController â€” mock Service vá»›i @WebMvcTest)
â””â”€â”€ Integration (Testcontainers PostgreSQL vá»›i @SpringBootTest, verify Flyway migration V20260707120000)
```

### TDS-02 â€” Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-196 Â§3.3.12.5` | Update date/notes/status behavior |
| `ADR-BABY-006` | milestone_status vs record_status disambiguation |
| `ADR-BABY-007` | canManage() strict ownership vs canView() |
| `BR-RBAC` | Owner-only mutation |
| `BR-PRIVACY` | Response minimum-necessary fields |

### TDS-03 â€” Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner updates status+achievedDate | `DevelopmentMilestoneServiceImpl.updateMilestone()` | `MILESTONE-UPD-TC-001` |
| TC-COND-002 | Owner updates note only (partial) | Partial update logic | `MILESTONE-UPD-TC-002` |
| TC-COND-003 | Care group member (ACCEPTED, non-owner) â†’ 403 | `BabyAccessPolicy.canManage()` | `MILESTONE-UPD-TC-003` |
| TC-COND-004 | Unrelated user â†’ 403 | `canManage()` | `MILESTONE-UPD-TC-004` |
| TC-COND-005 | Non-existent milestone â†’ 404 | repo lookup | `MILESTONE-UPD-TC-005` |
| TC-COND-006 | Soft-deleted milestone (recordStatus=DELETED) â†’ 404 | recordStatus guard | `MILESTONE-UPD-TC-006` |
| TC-COND-007 | Empty request body â†’ 400 | validation | `MILESTONE-UPD-TC-007` |
| TC-COND-008 | status=ACHIEVED without achievedDate â†’ 400 | validation | `MILESTONE-UPD-TC-008` |
| TC-COND-009 | [CRITICAL] Update status does NOT touch recordStatus | ADR-BABY-006 disambiguation | `MILESTONE-UPD-TC-DISAMB-001` |
| TC-COND-010 | IDOR â€” path babyId spoofed, ownership from DB only | ADR-BABY-007 / BR-RBAC | `MILESTONE-UPD-TC-SEC-001` |

### TDS-04 â€” Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | achievedDate (past/present/future), note (empty/max-length/null) | Boundary validation coverage |
| Boundary Value Analysis | `note` length = 2000/2001 chars | `@Size(max=2000)` boundary |
| State Transition Testing | `milestoneStatus` FSM: PENDINGâ†’ACHIEVEDâ†’DELAYED | Â§6.3 State Machine trong TDS |
| Error Guessing | IDOR via path babyId mismatch, care group ACCEPTED privilege escalation attempt | Security-focused |

### TDS-05 â€” Test Data Requirements

| Fixture ID | Type | Value / Logic | Má»¥c Ä‘Ã­ch |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{ milestoneId: MILESTONE-001, babyId: BABY-001, milestoneStatus: PENDING, recordStatus: ACTIVE, achievedDate: null }` | Happy path |
| `FX-002` | DB seed | `{ milestoneId: MILESTONE-002, babyId: BABY-001, milestoneStatus: ACHIEVED, recordStatus: DELETED }` | Soft-deleted reject |
| `FX-003` | DB seed | `{ babyId: BABY-001, ownerUserId: MOTHER-001 }` (BabyProfile) | Ownership chain |
| `FX-004` | DB seed | `{ careGroupId: CG-001, ownerAccountId: MOTHER-001, memberUserId: MOTHER-002, inviteStatus: ACCEPTED }` | Care group non-owner reject |
| `FX-005` | JWT | `{ sub: MOTHER-001, role: MOTHER }` | Auth context â€” owner |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 â€” Báº®T BUá»˜C)

```java
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// CASE 2.0 â€” Props Isolation Pattern
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
class DevelopmentMilestoneTestFactory {

    static DevelopmentMilestone makePendingMilestone() {
        DevelopmentMilestone m = new DevelopmentMilestone();
        m.setId(UUID.fromString("00000000-0000-0000-0000-000000000100"));
        m.setBabyId(UUID.fromString("00000000-0000-0000-0000-000000000060"));
        m.setMilestoneType("crawling");
        m.setAchievedDate(null);
        m.setNote("initial note");
        m.setSourceType("manual");
        m.setRecordedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        m.setMilestoneStatus(MilestoneAchievementStatus.PENDING);
        m.setRecordStatus(MilestoneRecordStatus.ACTIVE);
        return m;
    }

    static DevelopmentMilestone makeSoftDeletedMilestone() {
        DevelopmentMilestone m = makePendingMilestone();
        m.setId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
        m.setMilestoneStatus(MilestoneAchievementStatus.ACHIEVED);
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

    static UpdateDevelopmentMilestoneRequest makeAchievedRequest() {
        UpdateDevelopmentMilestoneRequest r = new UpdateDevelopmentMilestoneRequest();
        r.setStatus(MilestoneAchievementStatus.ACHIEVED);
        r.setAchievedDate(LocalDate.of(2026, 7, 1));
        return r;
    }

    static UpdateDevelopmentMilestoneRequest makeEmptyRequest() {
        return new UpdateDevelopmentMilestoneRequest(); // all fields null
    }
}
```

---

### MILESTONE-UPD-TC-001 â€” Owner updates status + achievedDate â†’ 200

**Severity:** `CRITICAL`
**Feature Under Test:** `DevelopmentMilestoneServiceImpl.updateMilestone()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/DevelopmentMilestoneServiceImplTest.java`
**TDD Phase:** ðŸ”´ RED â€” chÆ°a implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS Â§3.3.12.5`

**Preconditions:** FX-001, FX-003, FX-005

**Test Steps:**
1. Mock `milestoneRepository.findById(MILESTONE-001)` â†’ `makePendingMilestone()`
2. Mock `babyProfileRepository.findById(BABY-001)` â†’ `makeOwnerProfile()`
3. Mock `babyAccessPolicy.canManage(profile, MOTHER-001)` â†’ true
4. Call `updateMilestone(MILESTONE-001, makeAchievedRequest(), MOTHER-001)`

**Expected Result (PASS):** Response 200 vá»›i `status=ACHIEVED`, `achievedDate=2026-07-01`; `milestoneRepository.save()` Ä‘Æ°á»£c gá»i Ä‘Ãºng 1 láº§n vá»›i entity cÃ³ `milestoneStatus=ACHIEVED`

**Expected Result (FAIL):** Exception nÃ©m ra, hoáº·c `save()` khÃ´ng Ä‘Æ°á»£c gá»i

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-UPD-TC-002 â€” Owner updates note only (partial update) â†’ 200

**Severity:** `HIGH`
**Feature Under Test:** `updateMilestone()` â€” partial update
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-002`

**Test Steps:**
1. Request chá»‰ cÃ³ `note="new note"` (achievedDate, status = null)
2. Call `updateMilestone(MILESTONE-001, request, MOTHER-001)`

**Expected Result (PASS):** Response 200, `note` Ä‘á»•i; `milestoneStatus` GIá»® NGUYÃŠN giÃ¡ trá»‹ cÅ© (`PENDING`) â€” khÃ´ng bá»‹ null hoÃ¡ bá»Ÿi request field null

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-UPD-TC-003 â€” Care group member (ACCEPTED, non-owner) â†’ 403

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyAccessPolicy.canManage()` â€” strict ownership
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-BABY-007`

**Preconditions:** FX-004 (MOTHER-002 is ACCEPTED care group member, NOT owner)

**Test Steps:**
1. Mock `babyProfileRepository.findById(BABY-001)` â†’ profile owned by MOTHER-001
2. Mock `babyAccessPolicy.canManage(profile, MOTHER-002)` â†’ **false** (real impl: `ownerUserId.equals(callerId)` â†’ false vÃ¬ MOTHER-002 â‰  MOTHER-001)
3. Call `updateMilestone(MILESTONE-001, request, MOTHER-002)`

**Expected Result (PASS):** throws `BusinessException` `MILESTONE-002` (403) â€” **dÃ¹ MOTHER-002 cÃ³ quyá»n VIEW (canView=true) qua care group, váº«n KHÃ”NG cÃ³ quyá»n MANAGE**

**Expected Result (FAIL):** Náº¿u code dÃ¹ng nháº§m `canView()` thay vÃ¬ `canManage()`, test nÃ y sáº½ PASS sai (response 200) â€” Ä‘Ã¢y chÃ­nh lÃ  regression cáº§n cháº·n

**Current Status:** ðŸ”´ Not written
**Implementation Note:** ÄÃ¢y lÃ  test quan trá»ng nháº¥t Ä‘á»ƒ phÃ¡t hiá»‡n AP-AI-003 (dÃ¹ng nháº§m policy method cÃ³ sáºµn thay vÃ¬ method má»›i theo ADR-BABY-007).

---

### MILESTONE-UPD-TC-004 â€” Unrelated user â†’ 403

**Severity:** `CRITICAL`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-004`

**Expected Result:** throws `BusinessException` `MILESTONE-002` (403)

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-UPD-TC-005 â€” Non-existent milestone â†’ 404

**Severity:** `MEDIUM`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Mock `milestoneRepository.findById(NONEXISTENT)` â†’ `Optional.empty()`
2. Call `updateMilestone(NONEXISTENT, request, MOTHER-001)`

**Expected Result:** throws `BusinessException` `MILESTONE-001` (404)

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-UPD-TC-006 â€” Soft-deleted milestone (recordStatus=DELETED) â†’ 404

**Severity:** `CRITICAL`
**Feature Under Test:** `recordStatus` guard
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-BABY-006`

**Preconditions:** FX-002 (`makeSoftDeletedMilestone()`)

**Test Steps:**
1. Mock `milestoneRepository.findById(MILESTONE-002)` â†’ soft-deleted entity (`recordStatus=DELETED`)
2. Call `updateMilestone(MILESTONE-002, request, MOTHER-001)`

**Expected Result (PASS):** throws `BusinessException` `MILESTONE-001` (404) â€” treat as not-found, **KHÃ”NG cho phÃ©p "há»“i sinh" record qua update**

**Expected Result (FAIL):** Náº¿u code cho phÃ©p update thÃ nh cÃ´ng â†’ record bá»‹ "há»“i sinh" trÃ¡i phÃ©p, vi pháº¡m ADR-BABY-006

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-UPD-TC-007 â€” Empty request body â†’ 400

**Severity:** `MEDIUM`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-007`

**Test Steps:**
1. Call `updateMilestone(MILESTONE-001, makeEmptyRequest(), MOTHER-001)`

**Expected Result:** throws `BusinessException` `MILESTONE-003` (400)

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-UPD-TC-008 â€” status=ACHIEVED without achievedDate â†’ 400

**Severity:** `HIGH`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `SRS Â§3.3.12.5` (business inference â€” achievement needs a date)

**Test Steps:**
1. Entity fixture: `achievedDate=null` (FX-001, `makePendingMilestone()`)
2. Request: `{status: ACHIEVED}` (no `achievedDate` provided)
3. Call `updateMilestone(MILESTONE-001, request, MOTHER-001)`

**Expected Result:** throws `BusinessException` `MILESTONE-003` (400) â€” since neither existing nor new `achievedDate` is present

**Sub-case:** If entity already has `achievedDate` set (from prior update) and request only sends `status=ACHIEVED` without a new date â†’ should SUCCEED (uses existing date). Cover as `MILESTONE-UPD-TC-008b`.

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-UPD-TC-DISAMB-001 â€” [CRITICAL] Update status does NOT touch recordStatus

**Severity:** `CRITICAL`
**Feature Under Test:** ADR-BABY-006 disambiguation invariant
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-BABY-006`

**Test Steps:**
1. Entity fixture: `milestoneStatus=PENDING, recordStatus=ACTIVE`
2. Request: `{status: DELAYED}`
3. Call `updateMilestone(MILESTONE-001, request, MOTHER-001)`
4. Capture the entity passed to `milestoneRepository.save(entityCaptor.capture())`

**Expected Result (PASS):**
- `entityCaptor.getValue().getMilestoneStatus() == DELAYED`
- `entityCaptor.getValue().getRecordStatus() == ACTIVE` (**UNCHANGED** â€” this is the disambiguation assertion)

**Expected Result (FAIL):** If `getRecordStatus()` is anything other than `ACTIVE`, or is null, or `DELETED` â€” the service is incorrectly conflating the two status concepts, a **direct violation of ADR-BABY-006**.

**Current Status:** ðŸ”´ Not written
**Implementation Note:** This is the single most important test in this Test-Spec per task requirements â€” it proves UC196's status update never soft-deletes.

---

### SECURITY TEST CASES

---

### MILESTONE-UPD-TC-SEC-001 â€” IDOR: spoofed path babyId does not bypass ownership check

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 â€” Broken Access Control`
**CWE:** `CWE-639 â€” Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC`
**Feature Under Test:** `DevelopmentMilestoneController.updateMilestone()` + `DevelopmentMilestoneServiceImpl`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/DevelopmentMilestoneControllerTest.java`
**TDD Phase:** ðŸ”´ RED

**Preconditions:**
- MILESTONE-001 actually belongs to BABY-001 (owned by MOTHER-001)
- Attacker MOTHER-999 sends request with path `babyId = BABY-999` (a baby MOTHER-999 owns) but `milestoneId = MILESTONE-001` (belongs to a different baby)

**Test Steps (Attack Simulation):**
1. `PATCH /api/v1/babies/BABY-999/milestones/MILESTONE-001` with JWT for MOTHER-999
2. Service resolves `milestone.getBabyId()` from DB (= BABY-001), NOT from the path
3. Load `BabyProfile` for BABY-001 â†’ owner = MOTHER-001
4. `canManage(profile, MOTHER-999)` â†’ false

**Expected Result (PASS = há»‡ thá»‘ng an toÃ n):** `403 Forbidden` `MILESTONE-002` â€” path `babyId` mismatch is irrelevant; authorization is always derived from `milestone.getBabyId()` read from DB

**Expected Result (FAIL = lá»— há»•ng tá»“n táº¡i):** If the service trusts path `babyId` for authorization instead of the DB-resolved value, an attacker could potentially manipulate scoping logic

**Current Status:** ðŸ”´ Not written

---

### INTEGRATION TEST CASES

---

### MILESTONE-UPD-TC-INT-001 â€” Full flow: PATCH updates DB, record_status untouched

**Severity:** `HIGH`
**Feature Under Test:** Full flow: HTTP PATCH â†’ Service â†’ Repository â†’ PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/baby/DevelopmentMilestoneIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-001, TC-COND-009`

**Preconditions:**
- PostgreSQL Testcontainer running
- Flyway migrations applied automatically (including `V20260707120000`)
- Seed: BABY-001 owned by MOTHER-001; MILESTONE-001 with `milestoneStatus=PENDING, recordStatus=ACTIVE`

**Test Steps:**
1. `PATCH /api/v1/babies/{babyId}/milestones/{milestoneId}` with `{status: ACHIEVED, achievedDate: "2026-07-01"}`, JWT for MOTHER-001
2. Assert response 200, body `status=ACHIEVED`
3. Query DB directly: `SELECT milestone_status, record_status FROM development_milestones WHERE milestone_id = ?`

**Expected Result (PASS):**
- `milestone_status = 'ACHIEVED'`
- `record_status = 'ACTIVE'` (unchanged)
- `updated_at` bumped forward

**DB Assertion:**
```java
DevelopmentMilestone record = milestoneRepository.findById(savedId).orElseThrow();
assertThat(record.getMilestoneStatus()).isEqualTo(MilestoneAchievementStatus.ACHIEVED);
assertThat(record.getRecordStatus()).isEqualTo(MilestoneRecordStatus.ACTIVE);
```

**Current Status:** ðŸ”´ Not written

---

### MOBILE WIDGET TEST CASES (Flutter/Dart)

---

### MILESTONE-UPD-TC-MOBILE-001 â€” Update form validates and submits successfully

**Severity:** `HIGH`
**Feature Under Test:** `UpdateMilestoneScreen` / `MilestoneFormWidget` (Flutter)
**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/update_milestone_screen_test.dart`
**TDD Phase:** ðŸ”´ RED

**Test Steps:**
1. Pump `UpdateMilestoneScreen` with an existing milestone (status=PENDING)
2. Select status dropdown â†’ `ACHIEVED`
3. Enter `achievedDate` via date picker
4. Tap "Save"

**Expected Result (PASS):** API call fired with `{status: "ACHIEVED", achievedDate: "..."}`; success snackbar shown; screen pops back to milestone detail with updated status displayed

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-UPD-TC-MOBILE-002 â€” Selecting ACHIEVED without a date shows inline validation error

**Severity:** `MEDIUM`
**Feature Under Test:** `MilestoneFormWidget` client-side validation
**TDD Phase:** ðŸ”´ RED

**Test Steps:**
1. Pump form, select status = `ACHIEVED`, leave date field empty
2. Tap "Save"

**Expected Result (PASS):** Inline validation error shown ("Please select the achieved date"); no API call fired (mirrors backend `MILESTONE-003` rule client-side)

**Current Status:** ðŸ”´ Not written

---

### MILESTONE-UPD-TC-MOBILE-003 â€” 403 response from server surfaces a permission error

**Severity:** `MEDIUM`
**Feature Under Test:** Error handling â€” mock HTTP client returns 403 `MILESTONE-002`
**TDD Phase:** ðŸ”´ RED

**Expected Result (PASS):** UI shows a permission-denied message; no crash; form remains editable for retry (or navigates back per UX spec)

**Current Status:** ðŸ”´ Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | ðŸ”´ RED confirmed | ðŸŸ¢ GREEN (commit) | ðŸ”µ REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MILESTONE-UPD-TC-001` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-002` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-003` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-004` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-005` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-006` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-007` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-008` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-DISAMB-001` | `DevelopmentMilestoneServiceImplTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-SEC-001` | `DevelopmentMilestoneControllerTest.java:__` | `[ ]` | `___` | â€” |
| `MILESTONE-UPD-TC-INT-001` | `DevelopmentMilestoneIntegrationTest.java:__` | `[ ]` | `___` | â€” |

### 5.1 Red Gate Protocol (CASE 2.0 â€” GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class DevelopmentMilestoneServiceImpl implements IDevelopmentMilestoneService {
    @Override
    public DevelopmentMilestoneDetailResponse updateMilestone(
            UUID milestoneId, UpdateDevelopmentMilestoneRequest request, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (náº¿u PASS báº¥t thÆ°á»ng) |
|-------|-------------|----------|--------|----------------------------------|
| `MILESTONE-UPD-TC-001` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `MILESTONE-UPD-TC-003` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `MILESTONE-UPD-TC-DISAMB-001` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Táº¥t cáº£ FAIL? â˜ Yes â†’ **GATE-2 PASS** (T1â†’T2) â†’ tiáº¿p tá»¥c implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-BABY-IMP-004` Ä‘Ã£ Ä‘Æ°á»£c review vÃ  approve
- [ ] Logic Issues (Â§2) Ä‘Ã£ confirm vá»›i Tech Lead
- [ ] Migration `V20260707120000` Ä‘Ã£ approved (companion cho cáº£ UC196 vÃ  UC197)

### Exit Criteria (DoD)
- [ ] `./mvnw test` â€” táº¥t cáº£ unit tests xanh
- [ ] `./mvnw verify` â€” integration tests xanh (Testcontainers)
- [ ] Test coverage â‰¥ 80% cho `DevelopmentMilestoneServiceImpl`
- [ ] `MILESTONE-UPD-TC-DISAMB-001` PASS báº¯t buá»™c (gate riÃªng â€” khÃ´ng Ä‘Æ°á»£c skip)
- [ ] `MILESTONE-UPD-TC-003` (care group non-owner 403) PASS báº¯t buá»™c
- [ ] KhÃ´ng cÃ³ business logic trong `DevelopmentMilestoneController`
- [ ] `flutter test` xanh cho mobile widget tests

**Exit Criteria bá»• sung â€” CASE 2.0:**
- [ ] Red Gate (Â§5.1) â€” táº¥t cáº£ tests FAIL vá»›i throw stub trÆ°á»›c khi implement
- [ ] Contract Existence: `./mvnw compile 2>&1 | grep "error:"` â†’ no output
- [ ] Props Isolation: má»i test dÃ¹ng `DevelopmentMilestoneTestFactory`, khÃ´ng shared mutable state

### Suspension Criteria
- Migration `V20260707120000` chÆ°a approved/cháº¡y Ä‘Æ°á»£c trÃªn staging
- UC197 migration conflict phÃ¡t hiá»‡n (cÃ¹ng file migration, cáº§n coordinate)

---

## 7. Rollback Plan

```bash
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/
git checkout -- 05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260707120000__add_development_milestone_status_columns.sql
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/baby/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u trong TDD spec | Check | Gate cháº·n |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC khÃ´ng reference ADR/TDS constraint | â˜ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS vá»›i throw stub (Â§5.1) | â˜ | G-2 â˜… |
| AP-AI-003 | Implicit Decision | Test dÃ¹ng `canView()` thay vÃ¬ `canManage()` Ä‘á»ƒ mock 403 case, hoáº·c dÃ¹ng 1 cá»™t status chung | â˜ | G-1 |
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
