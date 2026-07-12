# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Test Design & Test Case Specification â€” UC-195 Delete Baby Daily Log

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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` â€” primary CareBridge database schema source
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260707111000__add_baby_daily_log_status.sql` â€” companion migration created by this TDS (see TDS Â§5.2)
- `02_Requirements/SRS/3_Functional_Specification.md` Â§3.3.12.4 (UC-195, Table 217)
- `04_Implement/UC195_DeleteBabyDailyLog/UC195_DeleteBabyDailyLog_TDS.md` â€” Technical Design Specification (this feature)
- `04_Implement/UC194_ViewBabyDailyLogDetail/UC194_ViewBabyDailyLogDetail_TDS.md` â€” companion TDS whose classes are EXTENDED here (no Test-Spec exists yet for UC194 at time of writing â€” see Â§3 Props Isolation note)
- `04_Implement/UC192_ViewBabyProfile/UC192_ViewBabyProfile_TDS.md` â€” `BabyAccessPolicy` origin (Approved, shipped code)

> **Quy Æ°á»›c TDD:** TÃ i liá»‡u nÃ y mÃ´ táº£ test cases TRÆ¯á»šC khi viáº¿t production code.
> Thá»© tá»± báº¯t buá»™c: viáº¿t test (`.java`) â†’ cháº¡y â†’ xÃ¡c nháº­n FAIL ðŸ”´ â†’ implement â†’ PASS ðŸŸ¢ â†’ refactor ðŸ”µ.
> KhÃ´ng mark test lÃ  âœ… náº¿u `./mvnw test` (backend) hoáº·c `flutter test` (mobile) chÆ°a xanh.
> KhÃ´ng dÃ¹ng PII tháº­t trong test data â€” chá»‰ dÃ¹ng SYNTHETIC data.

---

## CHANGELOG

| NgÃ y | NgÆ°á»i thá»±c hiá»‡n | Ná»™i dung thay Ä‘á»•i |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented after targeted baby/carejourney backend test pass; full regression remains blocked outside baby scope. |
| 2026-07-03 | AI Agent | Khá»Ÿi táº¡o tÃ i liá»‡u â€” TDD spec cho UC195 Delete Baby Daily Log, scoped ONLY tá»›i method `deleteBabyDailyLog()` vÃ  `BabyAccessPolicy.canManage()` (methods Má»šI) â€” KHÃ”NG cover láº¡i UC194's `getDailyLogDetail()`/`canView()` |

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
9. [Mobile Widget Tests (Flutter)](#9-mobile-widget-tests-flutter)

---

## 1. ThÃ´ng tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-195` |
| **Module** | `DeleteBabyDailyLog` â€” Bounded Context `baby` |
| **Spec gá»‘c** | `CB-BABY-IMP-004` (`04_Implement/UC195_DeleteBabyDailyLog/UC195_DeleteBabyDailyLog_TDS.md`) |
| **Priority** | ðŸŸ  P1 (destructive action on Sensitive-PII infant health data) |
| **Sprint** | `Sprint 4 (Device Sync And Care Edge Cases)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, BR-SAFETY` |
| **Upstream Dependencies** | `UC194 (BabyDailyLog entity, BabyDailyLogRepository, BabyDailyLogServiceImpl, BabyDailyLogController)`, `UC192 (BabyProfileRepository, BabyAccessPolicy)`, `audit (AuditService, AuditAction)` |
| **Downstream Consumers** | `UC194 GET endpoint (must 404 on DELETED â€” regression-tested here)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC195_DeleteBabyDailyLog_TDS.md Â§17` (C1-C6), `ADR-BABY-006/007/008` |
| **Constraints Injected** | Soft-delete only (C4), owner-only via `canManage()` (C2), idempotent 404 on double-delete (C6), mandatory synchronous audit (C5), no parallel class creation (C1) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 â†’ T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gá»‘c (sai / thiáº¿u) | Thá»±c táº¿ (schema / policy) | Fix Ã¡p dá»¥ng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Â§3.3.12.4 mÃ´ táº£ generic flow ("actor confirms action... system applies business rules") khÃ´ng nÃ³i rÃµ soft vs hard delete | TDS ADR-BABY-006 xÃ¡c Ä‘á»‹nh rÃµ: soft-delete qua `status` column, KHÃ”NG hard DELETE | Má»i test case assert `status == DELETED` sau khi gá»i, KHÃ”NG assert `repository.findById()` tráº£ `Optional.empty()` |
| L2 | UC194's TDS pre-designed `BabyDailyLog.status` field nhÆ°ng field nÃ y `nullable` (chÆ°a cÃ³ DB column) táº¡i thá»i Ä‘iá»ƒm UC194 viáº¿t | Migration `V20260707111000` (táº¡o bá»Ÿi UC195) thÃªm cá»™t `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'` â€” sau migration, field KHÃ”NG cÃ²n null cho record cÅ© | Integration test `DAILYLOG-TC-INT-002` verify record insert TRÆ¯á»šC migration (náº¿u seed qua raw SQL khÃ´ng set status) váº«n nháº­n default `'ACTIVE'` |
| L3 | `BabyAccessPolicy.canView()` (UC192/UC194) cho phÃ©p ACCEPTED care member â€” náº¿u Ã¡p dá»¥ng nháº§m cho delete sáº½ lÃ  lá»— há»•ng IDOR/broken-access-control | TDS ADR-BABY-007 tÃ¡ch riÃªng `canManage()` owner-only | `DAILYLOG-TC-002` lÃ  test CRITICAL riÃªng biá»‡t, PHáº¢I FAIL náº¿u implementation dÃ¹ng nháº§m `canView()` cho path xoÃ¡ |

---

## 3. Test Design Specification (TDS)

### TDS-01 â€” Scope / Pháº¡m vi

```
DeleteBabyDailyLog bao gá»“m cÃ¡c layer, SCOPED ONLY tá»›i method/logic Má»šI cá»§a UC195:
â”œâ”€â”€ Domain â€” BabyAccessPolicy.canManage() (pure logic, no deps ngoÃ i BabyProfile)
â”œâ”€â”€ Services â€” BabyDailyLogServiceImpl.deleteBabyDailyLog() (mock BabyDailyLogRepository,
â”‚              BabyProfileRepository, BabyAccessPolicy, AuditService vá»›i Mockito)
â”œâ”€â”€ Controller â€” BabyDailyLogController.deleteBabyDailyLog() (mock IBabyDailyLogService vá»›i @WebMvcTest)
â””â”€â”€ Integration â€” Testcontainers PostgreSQL (@SpringBootTest), full DELETE + GET regression flow

KHÃ”NG re-test: getDailyLogDetail() / canView() (Ä‘Ã£ spec bá»Ÿi UC194 â€” coverage Ä‘Ã³ thuá»™c vá»
UC194's OWN Test-Spec khi Ä‘Æ°á»£c táº¡o; test file nÃ y chá»‰ IMPORT UC194's factory pattern náº¿u tá»“n táº¡i).
```

### TDS-02 â€” Test Basis / CÆ¡ sá»Ÿ Kiá»ƒm thá»­

| Source | Items Derived |
|--------|--------------|
| `SRS UC-195` (Â§3.3.12.4, Table 217) | Soft-delete hÃ nh vi ngÆ°á»i dÃ¹ng, Preconditions PRE-1..4, Exceptions E1-E3 |
| `ADR-BABY-006` | Soft-delete-only invariant (C4), idempotent double-delete â†’ 404 (C6) |
| `ADR-BABY-007` | Owner-only `canManage()` â€” care member EXCLUDED (C2) |
| `ADR-BABY-008` | Mandatory synchronous audit event on delete (C5) |
| `BR-RBAC / BR-PRIVACY / BR-SAFETY` | ToÃ n bá»™ security test cases (IDOR, no-hard-delete guard, audit completeness) |
| `CB-BABY-IMP-004 Â§10` | Error code assertions: `DAILYLOG-001`, `DAILYLOG-003` |
| `CB-BABY-IMP-004 Â§16` | Authorization Matrix â€” owner âœ…, care member âŒ, expert âŒ, admin âŒ |

### TDS-03 â€” Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Owner gá»i delete trÃªn log ACTIVE cá»§a baby mÃ¬nh sá»Ÿ há»¯u | `BabyDailyLogServiceImpl.deleteBabyDailyLog()` | `DAILYLOG-TC-001` |
| TC-COND-002 | ACCEPTED care member (khÃ´ng pháº£i owner) gá»i delete | `BabyAccessPolicy.canManage()` | `DAILYLOG-TC-002`, `DAILYLOG-TC-007` |
| TC-COND-003 | NgÆ°á»i dÃ¹ng hoÃ n toÃ n khÃ´ng liÃªn quan gá»i delete | `BabyAccessPolicy.canManage()` | `DAILYLOG-TC-003` |
| TC-COND-004 | `babyLogId` khÃ´ng tá»“n táº¡i | `BabyDailyLogServiceImpl.deleteBabyDailyLog()` | `DAILYLOG-TC-004` |
| TC-COND-005 | Log Ä‘Ã£ `status=DELETED`, gá»i xoÃ¡ láº§n 2 | `BabyDailyLogServiceImpl.deleteBabyDailyLog()` | `DAILYLOG-TC-005` |
| TC-COND-006 | `babyId` trong URL path khÃ´ng khá»›p `dailyLog.getBabyId()` thá»±c táº¿ (path tampering) | `BabyDailyLogController.deleteBabyDailyLog()` | `DAILYLOG-TC-006` |
| TC-COND-007 | Verify `canManage()` owner=true / owner=false á»Ÿ má»©c unit thuáº§n | `BabyAccessPolicy.canManage()` | `DAILYLOG-TC-007`, `DAILYLOG-TC-008` |
| TC-COND-008 | Verify KHÃ”NG bao giá» gá»i hard-delete API cá»§a repository | `BabyDailyLogServiceImpl.deleteBabyDailyLog()` | `DAILYLOG-TC-009` |
| TC-COND-009 | Verify audit payload Ä‘Ãºng field, khÃ´ng leak `note` (ná»™i dung log) | `AuditService.log()` call site | `DAILYLOG-TC-010` |
| TC-COND-010 | IDOR attack simulation qua nhiá»u `babyId`/`logId` káº¿t há»£p | API layer | `DAILYLOG-TC-SEC-001` |
| TC-COND-011 | KhÃ´ng cÃ³ JWT / JWT háº¿t háº¡n | API layer (Spring Security filter) | `DAILYLOG-TC-SEC-002`, `DAILYLOG-TC-SEC-003` |
| TC-COND-012 | Full E2E: DELETE thÃ nh cÃ´ng â†’ GET (UC194) tráº£ 404 | Integration | `DAILYLOG-TC-INT-001` |
| TC-COND-013 | Migration backfill default `status='ACTIVE'` cho record cÅ© | Integration/Flyway | `DAILYLOG-TC-INT-002` |

### TDS-04 â€” Test Techniques / Ká»¹ thuáº­t Kiá»ƒm thá»­

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Caller identity: owner / ACCEPTED member / unrelated user / no-auth | 4 lá»›p tÆ°Æ¡ng Ä‘Æ°Æ¡ng quyá»n truy cáº­p rÃµ rá»‡t theo Auth Matrix Â§16 TDS |
| Boundary Value Analysis | `status` transition ACTIVEâ†’DELETED, DELETEDâ†’(no-op, 404) | Boundary duy nháº¥t cá»§a state machine 2 tráº¡ng thÃ¡i |
| State Transition Testing | `BabyDailyLogStatus` enum (ACTIVE â†” DELETED) | Äáº£m báº£o transition 1 chiá»u há»£p lá»‡, khÃ´ng "un-delete" qua endpoint nÃ y |
| Error Guessing | Path `babyId` tampering, double-delete race, hard-delete regression | CÃ¡c attack vector cá»¥ thá»ƒ tá»« ADR-BABY-006/007 |

### TDS-05 â€” Test Data Requirements

| Fixture ID | Type | Value / Logic | Má»¥c Ä‘Ã­ch |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `BabyProfile{id=BABY-001, ownerUserId=MOTHER-001, status=ACTIVE}` | Baseline owner context |
| `FX-002` | DB seed | `BabyDailyLog{id=LOG-001, babyId=BABY-001, status=ACTIVE, recordedBy=MOTHER-001}` | Happy path delete target |
| `FX-003` | DB seed | `BabyDailyLog{id=LOG-002, babyId=BABY-001, status=DELETED}` | Double-delete idempotency test |
| `FX-004` | DB seed | `CareGroupMember{careGroupId=..., userId=MOTHER-002, inviteStatus=ACCEPTED}` linked to `BABY-001`'s care group | Non-owner-but-accepted-member IDOR case |
| `FX-005` | JWT | `{sub: 'MOTHER-001', role: 'MOTHER'}` | Owner auth context |
| `FX-006` | JWT | `{sub: 'MOTHER-002', role: 'MOTHER'}` | Care member auth context |
| `FX-007` | JWT | `{sub: 'MOTHER-003', role: 'MOTHER'}` | Unrelated user auth context |

---

## 4. Test Case Specification

> **TC ID format:** `DAILYLOG-TC-[NNN]` (backend) / `DAILYLOG-TC-SEC-[NNN]` (security) / `DAILYLOG-TC-INT-[NNN]` (integration) / `DAILYLOG-TC-MOB-[NNN]` (mobile, Â§9)
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** ðŸ”´ Not written

### Props Isolation Boilerplate (CASE 2.0 â€” Báº®T BUá»˜C)

> â­ ÄÃ¢y lÃ  factory Äáº¦U TIÃŠN cho bounded context `baby.dailylog` test suite â€” UC194 chÆ°a cÃ³ Test-Spec/factory táº¡i thá»i Ä‘iá»ƒm viáº¿t tÃ i liá»‡u nÃ y. **Khi UC194's Test-Spec Ä‘Æ°á»£c táº¡o, nÃ³ PHáº¢I reuse factory nÃ y** (khÃ´ng táº¡o `BabyDailyLogTestFactory` thá»© 2) Ä‘á»ƒ trÃ¡nh phÃ¢n máº£nh test fixtures.

```java
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// CASE 2.0 â€” Props Isolation Pattern
// Äáº·t á»Ÿ Ä‘áº§u file test â€” má»—i @Test dÃ¹ng makeXxx()
// File: src/test/java/com/carebridge/backend/baby/BabyDailyLogTestFactory.java
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

class BabyDailyLogTestFactory {

    static final UUID OWNER_ID   = UUID.fromString("00000000-0000-0000-0000-0000000000A1"); // MOTHER-001
    static final UUID MEMBER_ID  = UUID.fromString("00000000-0000-0000-0000-0000000000A2"); // MOTHER-002 (ACCEPTED, non-owner)
    static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A3"); // MOTHER-003 (unrelated)
    static final UUID BABY_ID    = UUID.fromString("00000000-0000-0000-0000-0000000000B1"); // BABY-001

    static BabyProfile makeBabyProfile() {
        return makeBabyProfile(p -> {});
    }

    static BabyProfile makeBabyProfile(Consumer<BabyProfile> overrides) {
        BabyProfile profile = BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OWNER_ID)
                .nickname("BÃ© Bo")
                .status(BabyProfileStatus.ACTIVE)
                .build();
        overrides.accept(profile);
        return profile;
    }

    // Baseline ACTIVE log â€” synced with FX-002 (Â§3 TDS-05)
    static BabyDailyLog makeActiveLog() {
        return makeActiveLog(l -> {});
    }

    static BabyDailyLog makeActiveLog(Consumer<BabyDailyLog> overrides) {
        BabyDailyLog log = new BabyDailyLog();
        log.setId(UUID.fromString("00000000-0000-0000-0000-0000000000C1")); // LOG-001
        log.setBabyId(BABY_ID);
        log.setLogType("feeding");
        log.setNote("BÃº bÃ¬nh 120ml"); // SYNTHETIC â€” never real infant data
        log.setRecordedBy(OWNER_ID);
        log.setStatus(BabyDailyLogStatus.ACTIVE);
        overrides.accept(log);
        return log;
    }

    // Already-deleted log â€” synced with FX-003, for double-delete tests
    static BabyDailyLog makeDeletedLog() {
        return makeActiveLog(l -> {
            l.setId(UUID.fromString("00000000-0000-0000-0000-0000000000C2")); // LOG-002
            l.setStatus(BabyDailyLogStatus.DELETED);
        });
    }
}
```

---

### DAILYLOG-TC-001 â€” Owner xoÃ¡ log ACTIVE thÃ nh cÃ´ng â†’ soft-delete + audit

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog(UUID, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** ðŸ”´ RED â€” chÆ°a implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-BABY-006 (soft-delete)`, `ADR-BABY-008 (audit)` â€” TDS Â§6.1 sequence diagram

**Preconditions:**
- `FX-001` (BabyProfile, owner=OWNER_ID), `FX-002` (BabyDailyLog LOG-001, status=ACTIVE) mocked qua factory
- `babyDailyLogRepository.findById(LOG-001)` mock tráº£ `Optional.of(makeActiveLog())`
- `babyProfileRepository.findById(BABY_ID)` mock tráº£ `Optional.of(makeBabyProfile())`

**Test Steps:**
1. Arrange: mock `babyDailyLogRepository.save(any())` tráº£ vá» argument nÃ³ nháº­n (captured qua `ArgumentCaptor<BabyDailyLog>`)
2. Act: gá»i `service.deleteBabyDailyLog(LOG-001, OWNER_ID)`
3. Assert: captured entity cÃ³ `status == BabyDailyLogStatus.DELETED`; `auditService.log(BABY_DAILY_LOG_DELETED, OWNER_ID, "BabyDailyLog", LOG-001.toString(), any())` Ä‘Æ°á»£c gá»i Ä‘Ãºng 1 láº§n

**Expected Result (PASS â€” hÃ nh vi Ä‘Ãºng):**
- KhÃ´ng throw exception; `babyDailyLogRepository.save()` Ä‘Æ°á»£c gá»i Ä‘Ãºng 1 láº§n vá»›i `status=DELETED`; audit log gá»i Ä‘Ãºng 1 láº§n

**Expected Result (FAIL â€” dáº¥u hiá»‡u lá»—i):**
- `status` váº«n `ACTIVE` sau save â†’ thiáº¿u logic set status; hoáº·c audit khÃ´ng Ä‘Æ°á»£c gá»i â†’ vi pháº¡m ADR-BABY-008

**Current Status:** ðŸ”´ Not written
**Implementation Note:** Method PHáº¢I: `findById` â†’ check status â†’ `canManage` â†’ `setStatus(DELETED)` â†’ `save()` â†’ `auditService.log()`, TRONG CÃ™NG `@Transactional`.

---

### DAILYLOG-TC-002 â€” ACCEPTED care member (KHÃ”NG pháº£i owner) xoÃ¡ â†’ 403 DAILYLOG-003 (IDOR guard â€” critical)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 â€” Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog(UUID, UUID)` + `BabyAccessPolicy.canManage()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-BABY-007` â€” explicitly excludes care members from delete, unlike UC194's `canView()`

**Preconditions:**
- `FX-002` (LOG-001, status=ACTIVE), `FX-001` (BabyProfile owner=OWNER_ID)
- `FX-004`: `MEMBER_ID` lÃ  ACCEPTED member trong care group cá»§a `BABY_ID` (would pass `canView()` if wrongly reused)

**Test Steps:**
1. Arrange: mock repos as in TC-001
2. Act: gá»i `service.deleteBabyDailyLog(LOG-001, MEMBER_ID)`
3. Assert: throws `BusinessException` vá»›i `getHttpStatus() == FORBIDDEN`, `getCode() == "DAILYLOG-003"`
4. Assert: `babyDailyLogRepository.save()` KHÃ”NG Ä‘Æ°á»£c gá»i (no side effect); `auditService.log()` KHÃ”NG Ä‘Æ°á»£c gá»i

**Expected Result (PASS â€” há»‡ thá»‘ng an toÃ n):**
- 403 `DAILYLOG-003` â€” chá»©ng minh `canManage()` Ä‘Æ°á»£c dÃ¹ng, KHÃ”NG pháº£i `canView()`

**Expected Result (FAIL = lá»— há»•ng tá»“n táº¡i):**
- Náº¿u implementation nháº§m dÃ¹ng `canView()`, test nÃ y sáº½ PASS xoÃ¡ thÃ nh cÃ´ng (200) thay vÃ¬ 403 â†’ **AP-AI-003 detected** theo ADR-BABY-007

**Current Status:** ðŸ”´ Not written
**Implementation Note:** ÄÃ¢y lÃ  test case quan trá»ng nháº¥t cá»§a toÃ n bá»™ UC195 â€” báº£o vá»‡ trá»±c tiáº¿p ADR-BABY-007's decision.

---

### DAILYLOG-TC-003 â€” NgÆ°á»i dÃ¹ng khÃ´ng liÃªn quan xoÃ¡ â†’ 403 DAILYLOG-003

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-BABY-007`

**Preconditions:** `FX-001`, `FX-002`; `STRANGER_ID` khÃ´ng cÃ³ báº¥t ká»³ quan há»‡ nÃ o vá»›i `BABY_ID`

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(LOG-001, STRANGER_ID)`
2. Assert: throws `BusinessException(403, "DAILYLOG-003")`

**Expected Result (PASS):** 403 DAILYLOG-003
**Expected Result (FAIL):** 200 hoáº·c exception khÃ¡c code

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-004 â€” Log khÃ´ng tá»“n táº¡i â†’ 404 DAILYLOG-001

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-BABY-IMP-004 Â§10 Error Codes`

**Preconditions:** `babyDailyLogRepository.findById(NONEXISTENT)` mock tráº£ `Optional.empty()`

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(NONEXISTENT, OWNER_ID)`
2. Assert: throws `BusinessException(404, "DAILYLOG-001")`
3. Assert: `babyProfileRepository.findById()` KHÃ”NG Ä‘Æ°á»£c gá»i (short-circuit trÆ°á»›c khi resolve ownership â€” hiá»‡u nÄƒng)

**Expected Result (PASS):** 404 DAILYLOG-001
**Expected Result (FAIL):** NullPointerException hoáº·c 500

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-005 â€” Double-delete (log Ä‘Ã£ DELETED) â†’ 404 idempotent-safe, KHÃ”NG audit trÃ¹ng

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-BABY-006` Â§Consequences â€” idempotent-safe repeat calls

**Preconditions:** `FX-003` (LOG-002, status=DELETED) mocked

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(LOG-002, OWNER_ID)`
2. Assert: throws `BusinessException(404, "DAILYLOG-001")` â€” KHÃ”NG mÃ£ lá»—i riÃªng "already deleted"
3. Assert: `babyDailyLogRepository.save()` KHÃ”NG Ä‘Æ°á»£c gá»i; `auditService.log()` KHÃ”NG Ä‘Æ°á»£c gá»i

**Expected Result (PASS):** 404, no duplicate audit entry, no 500
**Expected Result (FAIL):** 500 (NPE trÃªn status transition) hoáº·c 200 (silently re-deletes, táº¡o audit trÃ¹ng)

**Current Status:** ðŸ”´ Not written
**Implementation Note:** Check `if (log.getStatus() == DELETED) throw 404` PHáº¢I xáº£y ra TRÆ¯á»šC khi load `BabyProfile`/gá»i `canManage()`.

---

### DAILYLOG-TC-006 â€” `babyId` path param tampering khÃ´ng áº£nh hÆ°á»Ÿng authorization (defense-in-depth)

**Severity:** `HIGH`
**CWE:** `CWE-639`
**Feature Under Test:** `BabyDailyLogController.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/BabyDailyLogControllerDeleteTest.java` (`@WebMvcTest`)
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-BABY-004 (UC194) Constraint C3 â€” reused C3 in UC195 TDS Â§17.1`

**Preconditions:** Mock `IBabyDailyLogService.deleteBabyDailyLog(logId, callerId)` â€” verify method KHÃ”NG nháº­n `babyId` tá»« path nhÆ° 1 tham sá»‘ dÃ¹ng cho authorization (chá»‰ dÃ¹ng routing)

**Test Steps:**
1. Act: `DELETE /api/v1/babies/{RANDOM_UNRELATED_BABY_ID}/daily-logs/{LOG-001}` vá»›i JWT cá»§a `OWNER_ID` (owner tháº­t cá»§a `BABY_ID`, khÃ´ng pháº£i `RANDOM_UNRELATED_BABY_ID`)
2. Assert: Controller váº«n gá»i `service.deleteBabyDailyLog(LOG-001, OWNER_ID)` â€” KHÃ”NG dÃ¹ng `RANDOM_UNRELATED_BABY_ID` Ä‘á»ƒ authorize; káº¿t quáº£ phá»¥ thuá»™c hoÃ n toÃ n vÃ o service logic (mÃ  service Ä‘Ã£ tá»± resolve `babyId` tháº­t tá»« `dailyLog.getBabyId()`)

**Expected Result (PASS):** Controller khÃ´ng cÃ³ logic authorization riÃªng dá»±a trÃªn path `babyId` â€” chá»‰ pass-through
**Expected Result (FAIL):** Controller tá»± cháº·n/tá»± cho phÃ©p dá»±a trÃªn path `babyId` mÃ  khÃ´ng qua service â€” vi pháº¡m layering rule (`CLAUDE.md`: "Controller: validation, request/response mapping only; no business logic")

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-007 â€” `BabyAccessPolicy.canManage()` tráº£ `false` cho ACCEPTED care member (unit thuáº§n)

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyAccessPolicy.canManage(BabyProfile, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/baby/policy/BabyAccessPolicyTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-BABY-007`

**Preconditions:** `profile.ownerUserId = OWNER_ID`; caller = `MEMBER_ID` (khÃ¡c `OWNER_ID`)

**Test Steps:**
1. Act: `policy.canManage(makeBabyProfile(), MEMBER_ID)`
2. Assert: káº¿t quáº£ `== false` â€” **KHÃ”NG** query `CareGroupMemberRepository` (khÃ¡c `canView()`, `canManage()` khÃ´ng cáº§n check care group membership)

**Expected Result (PASS):** `false`, khÃ´ng cÃ³ Mockito interaction vá»›i `memberRepository`
**Expected Result (FAIL):** `true` (bug â€” Ä‘ang tÃ¡i sá»­ dá»¥ng logic cá»§a `canView()`)

**Current Status:** ðŸ”´ Not written
**Implementation Note:** `canManage()` = `profile.getOwnerUserId().equals(callerId)` â€” 1 dÃ²ng, KHÃ”NG gá»i `memberRepository`.

---

### DAILYLOG-TC-008 â€” `BabyAccessPolicy.canManage()` tráº£ `true` cho owner

**Severity:** `HIGH`
**Feature Under Test:** `BabyAccessPolicy.canManage(BabyProfile, UUID)`
**Test File:** `src/test/java/com/carebridge/backend/baby/policy/BabyAccessPolicyTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-BABY-007`

**Test Steps:**
1. Act: `policy.canManage(makeBabyProfile(), OWNER_ID)`
2. Assert: káº¿t quáº£ `== true`

**Expected Result (PASS):** `true`
**Expected Result (FAIL):** `false`

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-009 â€” Verify KHÃ”NG bao giá» gá»i hard-delete (`delete()`/`deleteById()`)

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-BABY-006` â€” soft-delete-only invariant

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(LOG-001, OWNER_ID)` (happy path)
2. Assert: `Mockito.verify(babyDailyLogRepository, never()).delete(any())`
3. Assert: `Mockito.verify(babyDailyLogRepository, never()).deleteById(any())`
4. Assert: `Mockito.verify(babyDailyLogRepository, times(1)).save(any())`

**Expected Result (PASS):** hard-delete methods never invoked
**Expected Result (FAIL):** `delete()`/`deleteById()` called â€” CRITICAL violation of ADR-BABY-006 & BR-PRIVACY (unrecoverable data loss on Sensitive-PII)

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-010 â€” Audit payload Ä‘Ãºng field, khÃ´ng leak `note` (ná»™i dung log)

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogServiceImpl.deleteBabyDailyLog()` â†’ `AuditService.log()` call
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyDailyLogServiceImplDeleteTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-BABY-008`, BR-PRIVACY minimum-necessary

**Test Steps:**
1. Act: `service.deleteBabyDailyLog(LOG-001, OWNER_ID)`
2. Assert (via `ArgumentCaptor` trÃªn `auditService.log(action, userId, resourceType, resourceId, details)`): `action == BABY_DAILY_LOG_DELETED`; `userId == OWNER_ID`; `resourceType == "BabyDailyLog"`; `resourceId == LOG-001.toString()`
3. Assert: `details` object KHÃ”NG chá»©a field `note`/ná»™i dung nháº­t kÃ½ gá»‘c (chá»‰ metadata: babyLogId, babyId, deletedByUserId â€” theo Payload schema TDS Â§7.3)

**Expected Result (PASS):** audit payload minimum-necessary, khÃ´ng cÃ³ PII content
**Expected Result (FAIL):** `details` chá»©a `log.getNote()` plaintext â€” vi pháº¡m BR-PRIVACY

**Current Status:** ðŸ”´ Not written

---

### SECURITY TEST CASES

---

### DAILYLOG-TC-SEC-001 â€” IDOR attack simulation: enumerate `logId` across nhiá»u `babyId` khÃ´ng sá»Ÿ há»¯u

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 â€” Broken Access Control`
**CWE:** `CWE-639`
**Legal:** `BR-RBAC, PDPA minimum-necessary access`
**Feature Under Test:** `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}` (full stack)
**Test File:** `src/test/java/com/carebridge/backend/baby/DeleteBabyDailyLogSecurityTest.java` (`@SpringBootTest`, Testcontainers)
**TDD Phase:** ðŸ”´ RED

**Preconditions:** Seed 3 babies thuá»™c 3 owner khÃ¡c nhau, má»—i baby cÃ³ 1 log ACTIVE; attacker = `STRANGER_ID` vá»›i JWT há»£p lá»‡ nhÆ°ng khÃ´ng sá»Ÿ há»¯u baby nÃ o

**Test Steps (Attack Simulation):**
1. Attacker láº·p `DELETE /api/v1/babies/{babyId_N}/daily-logs/{logId_N}` cho cáº£ 3 cáº·p `(babyId, logId)` vá»›i JWT cá»§a mÃ¬nh
2. Kiá»ƒm tra response vÃ  DB state sau má»—i láº§n gá»i

**Expected Result (PASS = há»‡ thá»‘ng an toÃ n):**
- Cáº£ 3 láº§n Ä‘á»u tráº£ `403 Forbidden` vá»›i `code: DAILYLOG-003`
- DB: cáº£ 3 log váº«n `status=ACTIVE` sau attack (no side effect)

**Expected Result (FAIL = lá»— há»•ng tá»“n táº¡i):**
- Báº¥t ká»³ log nÃ o bá»‹ `status=DELETED` bá»Ÿi attacker

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-SEC-002 â€” KhÃ´ng cÃ³ JWT â†’ 401

**Severity:** `HIGH`
**OWASP:** `A07:2021 â€” Identification and Authentication Failures`
**Feature Under Test:** `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/BabyDailyLogControllerDeleteTest.java`
**TDD Phase:** ðŸ”´ RED

**Test Steps:**
1. `DELETE` khÃ´ng cÃ³ header `Authorization`
2. Assert: `401 Unauthorized`

**Expected Result (PASS):** 401
**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-SEC-003 â€” JWT háº¿t háº¡n/invalid â†’ 401

**Severity:** `MEDIUM`
**OWASP:** `A07:2021`
**Feature Under Test:** `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/BabyDailyLogControllerDeleteTest.java`
**TDD Phase:** ðŸ”´ RED

**Test Steps:**
1. `DELETE` vá»›i `Authorization: Bearer <expired-or-malformed-token>`
2. Assert: `401 Unauthorized`

**Expected Result (PASS):** 401
**Current Status:** ðŸ”´ Not written

---

### INTEGRATION TEST CASES

---

### DAILYLOG-TC-INT-001 â€” Full flow: DELETE thÃ nh cÃ´ng â†’ UC194's GET tráº£ 404 (cross-UC regression)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: seed log ACTIVE â†’ DELETE (UC195) â†’ GET (UC194) â†’ DB assert`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogDeleteIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tá»± Ä‘á»™ng khi Spring context start (bao gá»“m `V20260707111000`)
- Seed: `BabyProfile` (owner=OWNER_ID) + `BabyDailyLog` (status=ACTIVE) qua JPA repository trá»±c tiáº¿p

**Test Steps:**
1. `POST`/seed baby + log trá»±c tiáº¿p qua repository (khÃ´ng qua API, vÃ¬ UC195 khÃ´ng cÃ³ create endpoint)
2. `DELETE /api/v1/babies/{babyId}/daily-logs/{logId}` vá»›i JWT owner â†’ assert `200`
3. DB assert: `SELECT status FROM baby_daily_logs WHERE baby_log_id=?` == `'DELETED'`
4. DB assert: `SELECT COUNT(*) FROM audit_logs WHERE action='BABY_DAILY_LOG_DELETED' AND resource_id=?` == `1`
5. `GET /api/v1/babies/{babyId}/daily-logs/{logId}` (UC194's endpoint) vá»›i cÃ¹ng JWT â†’ assert `404` vá»›i `code: DAILYLOG-001`

**Expected Result (PASS):**
- BÆ°á»›c 2: 200; BÆ°á»›c 3-4: DB state Ä‘Ãºng; BÆ°á»›c 5: 404 (chá»©ng minh UC194's read path tÃ´n trá»ng `status` filter Ä‘Ã£ activate bá»Ÿi migration nÃ y)

**Expected Result (FAIL):**
- BÆ°á»›c 5 váº«n tráº£ 200 (UC194's `getDailyLogDetail` chÆ°a filter `status <> DELETED`) â†’ cáº§n fix UC194's service code khi implement song song, ghi vÃ o Logic Issues náº¿u phÃ¡t sinh

**DB Assertion:**
```java
BabyDailyLog record = babyDailyLogRepository.findById(savedId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(BabyDailyLogStatus.DELETED);

long auditCount = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM audit_logs WHERE action = ? AND resource_id = ?",
    Long.class, "BABY_DAILY_LOG_DELETED", savedId.toString());
assertThat(auditCount).isEqualTo(1L);
```

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-INT-002 â€” Migration backfill: record cÅ© (pre-migration) nháº­n `status='ACTIVE'` máº·c Ä‘á»‹nh

**Severity:** `MEDIUM`
**Feature Under Test:** `V20260707111000__add_baby_daily_log_status.sql`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogMigrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-013`

**Preconditions:** Testcontainers PostgreSQL, insert 1 row qua raw SQL (`INSERT INTO baby_daily_logs (baby_id, log_type, ...) VALUES (...)` â€” KHÃ”NG set `status`) TRÆ¯á»šC KHI Flyway apply `V20260707111000` (hoáº·c: insert sau migration nhÆ°ng qua raw SQL khÃ´ng set status, verify `DEFAULT 'ACTIVE'` Ã¡p dá»¥ng)

**Test Steps:**
1. Insert raw row khÃ´ng cÃ³ `status`
2. Query láº¡i: `SELECT status FROM baby_daily_logs WHERE baby_log_id=?`
3. Assert: `status == 'ACTIVE'`

**Expected Result (PASS):** `DEFAULT 'ACTIVE'` hoáº¡t Ä‘á»™ng Ä‘Ãºng â€” record cÅ© KHÃ”NG bá»‹ coi lÃ  DELETED
**Expected Result (FAIL):** `status` lÃ  `NULL` (migration thiáº¿u `NOT NULL DEFAULT`) â†’ UC194's GET path cÃ³ thá»ƒ lá»—i NPE khi map DTO

**Current Status:** ðŸ”´ Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | ðŸ”´ RED confirmed | ðŸŸ¢ GREEN (commit) | ðŸ”µ REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `DAILYLOG-TC-001` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-002` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-003` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-004` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-005` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-006` | `BabyDailyLogControllerDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-007` | `BabyAccessPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-008` | `BabyAccessPolicyTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-009` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-010` | `BabyDailyLogServiceImplDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-SEC-001` | `DeleteBabyDailyLogSecurityTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-SEC-002` | `BabyDailyLogControllerDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-SEC-003` | `BabyDailyLogControllerDeleteTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-INT-001` | `BabyDailyLogDeleteIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-INT-002` | `BabyDailyLogMigrationTest.java:TBD` | `[ ]` | `[ ]` | |

**Total test cases:** 15 (10 unit/component + 3 security + 2 integration), plus 4 mobile widget tests (Â§9) = **19 total**.

### 5.1 Red Gate Protocol (CASE 2.0 â€” GATE-2)

> Scoped ONLY tá»›i method Má»šI cá»§a UC195 (`deleteBabyDailyLog`, `canManage`) â€” KHÃ”NG Ã¡p dá»¥ng láº¡i lÃªn UC194's `getDailyLogDetail`/`canView` (Ä‘Ã£ ngoÃ i pháº¡m vi tÃ i liá»‡u nÃ y).

**Stub cho Red Phase:**

```java
// Red Phase â€” implementation stub (PHáº¢I throw). Applies ONLY to the two NEW methods.

// IBabyDailyLogService.java â€” deleteBabyDailyLog() stub added alongside UC194's EXISTING
// getDailyLogDetail() (which is assumed already implemented if UC194 ships first; if not,
// getDailyLogDetail() keeps its own UC194 Red Gate, untouched here).
@Service
public class BabyDailyLogServiceImpl implements IBabyDailyLogService {

    @Override
    public BabyDailyLogDetailResponse getDailyLogDetail(UUID babyLogId, UUID callerId) {
        // UC194 scope â€” not re-stubbed here
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub (UC194 scope)");
    }

    @Override
    public void deleteBabyDailyLog(UUID babyLogId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub (UC195 scope)");
    }
}

// BabyAccessPolicy.java â€” canManage() stub added alongside UC192's EXISTING canView()
@Component
public class BabyAccessPolicy {

    public boolean canView(BabyProfile profile, UUID callerId) {
        // UC192 â€” already shipped, NOT re-stubbed
        return /* existing shipped logic, untouched */ false;
    }

    public boolean canManage(BabyProfile profile, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub (UC195 scope)");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (náº¿u PASS báº¥t thÆ°á»ng) |
|-------|-------------|----------|--------|----------------------------------|
| `DAILYLOG-TC-001` | `throw('Not implemented â€” UC195 scope')` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | â˜ Tautology â˜ Shared state â˜ Hallucinated import |
| `DAILYLOG-TC-002` | `throw(...)` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-003` | `throw(...)` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-004` | `throw(...)` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-005` | `throw(...)` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-007` | `throw(...)` (canManage stub) | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-008` | `throw(...)` (canManage stub) | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-009` | `throw(...)` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (to be filled when implementation begins)
- Táº¥t cáº£ FAIL? â˜ Yes â†’ **GATE-2 PASS** (T2â†’T3) â†’ tiáº¿p tá»¥c implement
- Log file: `[path to red-gate-evidence.log]` (to be generated by `./mvnw test` run against stub)

> **Náº¿u báº¥t ká»³ test PASS báº¥t thÆ°á»ng trÃªn stub:** Dá»«ng láº¡i â€” dáº¥u hiá»‡u tautology hoáº·c test khÃ´ng thá»±c sá»± gá»i vÃ o `deleteBabyDailyLog()`/`canManage()`. Rewrite theo Props Isolation Pattern (Â§4).

---

## 6. Entry / Exit Criteria

### Entry Criteria (Äiá»u kiá»‡n báº¯t Ä‘áº§u)

- [ ] TDS `CB-BABY-IMP-004` Ä‘Ã£ Ä‘Æ°á»£c review vÃ  approve
- [ ] Logic Issues (Â§2) Ä‘Ã£ Ä‘Æ°á»£c confirm vá»›i Tech Lead
- [ ] Flyway migration `V20260707111000__add_baby_daily_log_status.sql` Ä‘Ã£ Ä‘Æ°á»£c approved vÃ  cháº¡y thÃ nh cÃ´ng trÃªn staging
- [ ] UC194's `BabyDailyLog`, `BabyDailyLogRepository`, `IBabyDailyLogService`, `BabyDailyLogServiceImpl`, `BabyDailyLogController` tá»“n táº¡i trong codebase (implement trÆ°á»›c hoáº·c song song vá»›i UC195 â€” coordination cáº§n thiáº¿t vÃ¬ UC195 EXTENDS cÃ¡c file nÃ y)
- [ ] Test fixtures (Â§3 TDS-05) Ä‘Ã£ Ä‘Æ°á»£c chuáº©n bá»‹ qua `BabyDailyLogTestFactory`

### Exit Criteria (Äiá»u kiá»‡n káº¿t thÃºc â€” DoD)

- [ ] `./mvnw test` â€” táº¥t cáº£ 13 unit/security tests xanh (khÃ´ng cÃ³ skip)
- [ ] `./mvnw verify` â€” 2 integration tests xanh (Testcontainers)
- [ ] Test coverage â‰¥ 80% lines cho `deleteBabyDailyLog()` method má»›i vÃ  `canManage()` method má»›i
- [ ] KhÃ´ng cÃ³ business logic trong `BabyDailyLogController` (chá»‰ validation + mapping) â€” verify qua `DAILYLOG-TC-006`
- [ ] KhÃ´ng cÃ³ PII (`note` content) xuáº¥t hiá»‡n plaintext trong audit logs â€” verify qua `DAILYLOG-TC-010`
- [ ] `flutter test` â€” 4 mobile widget tests (Â§9) xanh

**Exit Criteria bá»• sung â€” CASE 2.0:**

- [ ] **Red Gate (Â§5.1)** â€” táº¥t cáº£ tests FAIL vá»›i `UnsupportedOperationException` stub trÆ°á»›c khi implement `deleteBabyDailyLog()`/`canManage()`
- [ ] **Contract Existence** â€” má»i class Ä‘Æ°á»£c inject Ä‘á»u tá»“n táº¡i trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** â€” khÃ´ng cÃ³ shared mutable state giá»¯a tests:
  ```bash
  grep -n "^    [A-Z].*=.*new \|^    [a-z].*=.*new " BabyDailyLogServiceImplDeleteTest.java
  # Má»i instance PHáº¢I náº±m trong @Test hoáº·c dÃ¹ng BabyDailyLogTestFactory
  ```
- [ ] **Oracle Source** â€” má»i expected value trong assert cÃ³ ghi rÃµ nguá»“n (BR/ADR) â€” Ä‘Ã£ thá»±c hiá»‡n á»Ÿ má»—i TC trÃªn
- [ ] **No hard-delete regression** â€” `DAILYLOG-TC-009` PASS xÃ¡c nháº­n `repository.delete()`/`deleteById()` never called

### Suspension Criteria (Äiá»u kiá»‡n táº¡m dá»«ng)

- UC194's classes chÆ°a tá»“n táº¡i trong codebase (blocker dependency â€” UC195 EXTENDS chÃºng, khÃ´ng thá»ƒ compile trÆ°á»›c)
- Migration `V20260707111000` bá»‹ conflict vá»›i migration khÃ¡c trong cÃ¹ng version range
- PhÃ¡t hiá»‡n lá»—i kiáº¿n trÃºc má»›i (vd: `permission_json` model cáº§n cho OI-1) cáº§n Tech Lead review

---

## 7. Rollback Plan

```bash
# Revert migration thá»§ cÃ´ng (dev only â€” KHÃ”NG cháº¡y trÃªn production náº¿u Ä‘Ã£ cÃ³ status=DELETED data)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.baby_daily_logs DROP COLUMN IF EXISTS status;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260707111000';"

# Revert implementation files (CHá»ˆ cÃ¡c Ä‘oáº¡n UC195 thÃªm vÃ o, KHÃ”NG revert UC194's existing code)
git diff HEAD -- src/main/java/com/carebridge/backend/baby/controller/BabyDailyLogController.java
git diff HEAD -- src/main/java/com/carebridge/backend/baby/service/impl/BabyDailyLogServiceImpl.java
git diff HEAD -- src/main/java/com/carebridge/backend/baby/policy/BabyAccessPolicy.java
# Revert chá»‰ cÃ¡c hunk liÃªn quan deleteBabyDailyLog()/canManage() â€” dÃ¹ng git apply -R vá»›i patch cá»¥ thá»ƒ,
# KHÃ”NG git checkout toÃ n file (sáº½ xoÃ¡ luÃ´n UC194's code náº¿u Ä‘Ã£ merge chung file)

git checkout -- src/main/resources/db/migration/V20260707111000__add_baby_daily_log_status.sql
git checkout -- src/test/java/com/carebridge/backend/baby/*Delete*Test.java

# Gap váº«n OPEN â†’ giá»¯ nguyÃªn entry trong PHASE_GAP_ANALYSIS.md (náº¿u cÃ³)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u trong TDD spec | Check | Gate cháº·n |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC khÃ´ng reference ADR/TDS constraint nÃ o | â˜‘ (má»i TC cÃ³ Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS vá»›i `UnsupportedOperationException` stub (Â§5.1) | â˜ (verify khi implement) | G-2 â˜… |
| AP-AI-003 | Implicit Decision | Test assume `canManage()` == `canView()` semantics khÃ´ng cÃ³ ADR | â˜‘ (`DAILYLOG-TC-002`/`007` explicitly guard against this) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller cÃ³ business logic (path `babyId` used for auth) | â˜‘ (`DAILYLOG-TC-006` explicitly guards this) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type khÃ´ng tá»“n táº¡i trong codebase | â˜ (verify khi implement â€” `AuditService`/`AuditAction.BABY_DAILY_LOG_DELETED` cáº§n Ä‘Æ°á»£c thÃªm trÆ°á»›c, xem TDS Â§8.3) | G-3 |

**Káº¿t quáº£ review:**

- [x] Constraint traceability confirmed cho toÃ n bá»™ 15 backend test cases â€” chÆ°a phÃ¡t hiá»‡n anti-pattern á»Ÿ giai Ä‘oáº¡n spec (pre-implementation)
- [ ] Cáº§n re-review sau khi Red Gate (Â§5.1) cháº¡y thá»±c táº¿

| AP detected | TC ID | MÃ´ táº£ | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none táº¡i thá»i Ä‘iá»ƒm viáº¿t â€” pending implementation)_ | | | | |

---

## 9. Mobile Widget Tests (Flutter)

> Package: `05_Development/CareBridgeMobileApp/lib/features/baby/` (screens/, services/, models/) â€” greenfield cho delete action, KHÃ”NG dÃ¹ng `babyCare/` stub folder, nháº¥t quÃ¡n vá»›i UC194 TDS's note.

### DAILYLOG-TC-MOB-001 â€” XoÃ¡ log qua confirmation dialog â†’ gá»i API DELETE â†’ thÃ nh cÃ´ng

**Feature Under Test:** `BabyDailyLogService.deleteDailyLog()` + UI confirmation flow (Flutter widget, screen name TBD â€” extends UC194's daily-log-detail screen with a delete action)
**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_daily_log_delete_test.dart`
**TDD Phase:** ðŸ”´ RED

```dart
testWidgets('tapping delete + confirm calls DELETE endpoint and pops with success', (tester) async {
  // Arrange: mock BabyService/BabyDailyLogService.deleteDailyLog() returns success
  // Act: pump daily log detail screen, tap delete icon, confirm in AlertDialog
  // Assert: service.deleteDailyLog(babyId, logId) called once; success SnackBar shown; screen pops
});
```

**Expected Result (PASS):** API called exactly once after explicit confirmation (no accidental delete on single tap â€” dialog required)
**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-MOB-002 â€” Huá»· confirmation dialog â†’ KHÃ”NG gá»i API

**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_daily_log_delete_test.dart`
**TDD Phase:** ðŸ”´ RED

```dart
testWidgets('cancelling the confirmation dialog does not call delete API', (tester) async {
  // Act: tap delete icon, tap "Cancel" in dialog
  // Assert: service.deleteDailyLog() never called; screen state unchanged
});
```

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-MOB-003 â€” API tráº£ 403 â†’ hiá»ƒn thá»‹ thÃ´ng bÃ¡o "not permitted", KHÃ”NG pop screen

**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_daily_log_delete_test.dart`
**TDD Phase:** ðŸ”´ RED

```dart
testWidgets('403 DAILYLOG-003 response shows permission-denied message, screen stays', (tester) async {
  // Arrange: mock service throws ApiException(403, 'DAILYLOG-003')
  // Act: confirm delete
  // Assert: error SnackBar/dialog shown with permission message; screen NOT popped
});
```

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-MOB-004 â€” Network failure â†’ retry-able error state, khÃ´ng crash

**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_daily_log_delete_test.dart`
**TDD Phase:** ðŸ”´ RED

```dart
testWidgets('network failure during delete shows retry option, does not crash', (tester) async {
  // Arrange: mock service throws SocketException/timeout
  // Act: confirm delete
  // Assert: error state with retry action displayed; no unhandled exception in widget tree
});
```

**Current Status:** ðŸ”´ Not written

---

*TDD Spec v1.0 â€” CASE 2.0 Anti-Pattern Detection & Red Gate Protocol, scoped tá»›i UC195's NEW methods only.*
