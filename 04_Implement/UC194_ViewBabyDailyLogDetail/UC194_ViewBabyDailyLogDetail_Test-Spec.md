# TEST-DRIVEN DEVELOPMENT SPECIFICATION â€” UC194 View Baby Daily Log Detail

**Document ID:** `CB-BABY-TDD-194`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented — 2026-07-10 (targeted baby/carejourney backend tests PASS; full regression blocked by non-baby Family/Exercise/Auth/Triage failures)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(infant health/feeding/sleep data â€” Sensitive-PII)*
**Approved by:** `TV2-BÃ¡ch`
**Classification:** `Internal â€” Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (`baby_daily_logs`, lines 621-633)
- `02_Requirements/SRS/3_Functional_Specification.md Â§3.3.12.3`
- `04_Implement/UC194_ViewBabyDailyLogDetail/UC194_ViewBabyDailyLogDetail_TDS.md` (`CB-BABY-IMP-003`)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/policy/BabyAccessPolicy.java` (existing, reused â€” UC192)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/repository/BabyProfileRepository.java` (existing, reused â€” UC192)
- `04_Implement/UC195_DeleteBabyDailyLog/UC195_DeleteBabyDailyLog_TDS.md` (companion â€” `status` column origin)

> **Quy Æ°á»›c TDD:** Test-first. Thá»© tá»±: viáº¿t test â†’ cháº¡y â†’ xÃ¡c nháº­n FAIL ðŸ”´ â†’ implement â†’ PASS ðŸŸ¢ â†’ refactor ðŸ”µ.
> KhÃ´ng dÃ¹ng PII tháº­t â€” chá»‰ SYNTHETIC data (UUID literals).

---

## CHANGELOG

| NgÃ y | NgÆ°á»i thá»±c hiá»‡n | Ná»™i dung thay Ä‘á»•i |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Implementation status updated to Partially Implemented after targeted baby/carejourney backend test pass; full regression remains blocked outside baby scope. |
| 2026-07-03 | AI Agent | Khá»Ÿi táº¡o tÃ i liá»‡u â€” Test-Spec cho UC194 View Baby Daily Log Detail (Draft) |

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
| **Feature / Gap ID** | `UC-194` |
| **Module** | Baby â€” View Baby Daily Log Detail |
| **Spec gá»‘c** | `CB-BABY-IMP-003` |
| **Priority** | ðŸŸ  P1 (Medium priority per TDS, Sensitive-PII infant data) |
| **Sprint** | Sprint 4 â€” Device Sync And Care Edge Cases |
| **Milestone** | M3 Alpha |
| **Data Classification** | `Sensitive-PII` (infant feeding/sleep/diaper data) |
| **Compliance Scope** | BR-RBAC, BR-PRIVACY, BR-SAFETY |
| **Upstream Dependencies** | `BabyProfile`, `BabyAccessPolicy`, `BabyProfileRepository` (all existing â€” UC192); `baby_daily_logs` table (existing schema, unused by app code today) |
| **Downstream Consumers** | Baby Daily Log List (future UC), UC195 Delete Baby Daily Log |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | Yes |
| **Constraint Source** | `CB-BABY-IMP-003 Â§17`, ADR-BABY-004 (this TDS's own numbering), ADR-BABY-005 |
| **Constraints Injected** | C1-C5 per TDS Â§17.1 |
| **Model** | Claude Sonnet 5 |
| **Trust Level** | T2 â†’ T3 (pending Red Gate) |

---

## 2. Logic Issues Resolved

| # | Spec gá»‘c (sai / thiáº¿u) | Thá»±c táº¿ (schema / policy) | Fix Ã¡p dá»¥ng trong test |
|---|---------------------------|-------------------------------|------------------------------|
| L1 | **RESOLVED (2026-07-03)** â€” TDS UC192 Â§9-10 previously documented error codes `BABY-002`/`BABY-004`, while real shipped `BabyServiceImpl` uses `BABY-001`/`BABY-003` (confirmed by reading the real file â€” flagged as OI-3 in this TDS, now closed) | UC192's TDS has been corrected to match the real code (`BABY-001`=404, `BABY-003`=403) across its error-code tables, JSON examples, and Gherkin scenarios. UC194 uses an entirely separate `DAILYLOG-` error-code prefix (per TDS Â§10), so this mismatch never directly affected UC194's own codes | No test in this spec asserts `BABY-002`/`BABY-004` anywhere; all ownership/access assertions for baby-log-specific denial use `DAILYLOG-001`/`DAILYLOG-002` exclusively |
| L2 | A naive read of "view detail" could assume the `babyId` URL path segment is authoritative for authorization | TDS Â§9.1 explicitly forbids trusting `babyId` from the path â€” authorization must be derived from `babyDailyLog.getBabyId()` read from DB (Constraint C2 Â§17) | `TC-003` explicitly seeds a MISMATCHED path `babyId` (different from the log's real `baby_id` FK) and asserts the service still resolves ownership correctly via the DB-read `babyId`, not the path param â€” proving path-param tampering cannot be used for a confused-deputy attack |
| L3 | **RESOLVED (2026-07-03)** â€” `log_type` vocabulary was initially thought undocumented; sibling spec `UC34_AddFeedingSleepDiaperLog` (ADR-BABY-007) actually defines it for this same column: `FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE` (enforced write-side via `BABY-033`) | TDS Â§5.2 Open Item OI-1 (now closed) keeps `String logType` (NOT `@Enumerated`) on the read path deliberately â€” UC194 is read-only and must not reject legacy/out-of-whitelist values, whitelist enforcement is UC34's responsibility | `TC-001` (happy path) uses `logType="FEEDING"` (a real UC34 vocabulary value) and additionally asserts the read path tolerates an out-of-whitelist string without validation error â€” proving the entity does not `@Enumerated`-reject unknown-but-well-formed values |
| L4 | Since `status` column on `baby_daily_logs` may not exist yet if UC195 has not shipped (TDS Â§5.2), a naive test suite might hard-fail if the column is absent | TDS Â§8.2 documents `status` as nullable "`@since UC195`" â€” read path must null-check and treat legacy NULL as ACTIVE | `TC-006` explicitly covers a log row with `status = NULL` (pre-UC195 legacy state) and asserts it is treated as viewable (NOT 404), while `TC-005` covers `status = DELETED` (post-UC195) and asserts 404 |

---

## 3. Test Design Specification (TDS)

### TDS-01 â€” Scope / Pháº¡m vi

```
Baby (View Daily Log Detail, UC-194) bao gá»“m cÃ¡c layer:
â”œâ”€â”€ Services (BabyDailyLogServiceImpl.getDailyLogDetail() â€” mock BabyDailyLogRepository +
â”‚             BabyProfileRepository + BabyAccessPolicy vá»›i Mockito; BabyProfileRepository vÃ 
â”‚             BabyAccessPolicy Ä‘á»u lÃ  REUSE-ONLY mocks, khÃ´ng viáº¿t logic má»›i cho chÃºng)
â”œâ”€â”€ Controller (BabyDailyLogController.getDailyLogDetail() â€” mock IBabyDailyLogService vá»›i @WebMvcTest)
â””â”€â”€ Integration (Testcontainers PostgreSQL, full stack GET /api/v1/babies/{babyId}/daily-logs/{logId})

LÆ°u Ã½ pháº¡m vi: TOÃ€N Bá»˜ module (BabyDailyLog entity, BabyDailyLogStatus enum, BabyDailyLogRepository,
IBabyDailyLogService, BabyDailyLogServiceImpl, BabyDailyLogDetailResponse, BabyDailyLogController) lÃ 
GREENFIELD â€” khÃ´ng cÃ³ file nÃ o trong sá»‘ nÃ y tá»“n táº¡i trong codebase hiá»‡n táº¡i (xÃ¡c nháº­n qua glob search
`baby/**/*.java` â€” khÃ´ng cÃ³ káº¿t quáº£ nÃ o chá»©a "DailyLog"). Do Ä‘Ã³ Red Gate (Â§5.1) bao phá»§ TOÃ€N Bá»˜
controller/service/repository má»›i, KHÃ”NG chá»‰ má»™t method Ä‘Æ¡n láº» â€” khÃ¡c vá»›i UC188/UC193 vá»‘n extend code
Ä‘Ã£ shipped. BabyProfileRepository vÃ  BabyAccessPolicy (dependency, UC192) KHÃ”NG bá»‹ re-stub.
```

### TDS-02 â€” Test Basis

| Source | Items Derived |
|--------|------------------|
| SRS Â§3.3.12.3 UC-194 | Mother/care-member views full detail of one baby daily log |
| ADR-BABY-004 (this TDS) | Reuse `BabyAccessPolicy.canView()` via ownership chain `baby_daily_logs.baby_id â†’ baby_profiles.owner_user_id`; no new access policy class |
| ADR-BABY-005 (this TDS) | Read-only, no default audit event; `BabyDailyLogViewed` is an Open item, not wired |
| BR-RBAC | Owner OR ACCEPTED care-group member may view; anyone else â†’ 403 |
| BR-PRIVACY | Response contains only content/timestamp/type fields â€” minimum necessary |
| BR-SAFETY | Response DTO must NEVER contain `diagnosis`/`interpretation`/`condition` fields |
| TDS Â§10 Error Codes | `DAILYLOG-001` (404, not found/deleted/orphan), `DAILYLOG-002` (403, access denied) â€” new prefix, does not collide with `BABY-xxx` |

### TDS-03 â€” Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|--------------------|--------------------|----------------|
| TC-COND-001 | Owner views detail of own baby's log (happy path) | `BabyDailyLogServiceImpl.getDailyLogDetail()` | `DAILYLOG-TC-001` |
| TC-COND-002 | ACCEPTED care-group member views detail â†’ 200 | `BabyDailyLogServiceImpl.getDailyLogDetail()` | `DAILYLOG-TC-002` |
| TC-COND-003 | Path `babyId` mismatched/tampered with real `baby_id` â€” service ignores path, uses DB value | `BabyDailyLogServiceImpl.getDailyLogDetail()` | `DAILYLOG-TC-003` |
| TC-COND-004 | Non-owner, non-member â†’ 403 | `BabyDailyLogServiceImpl.getDailyLogDetail()` | `DAILYLOG-TC-004` |
| TC-COND-005 | Log soft-deleted (`status=DELETED`, post-UC195) â†’ 404 | `BabyDailyLogServiceImpl.getDailyLogDetail()` | `DAILYLOG-TC-005` |
| TC-COND-006 | Log `status=NULL` (legacy, pre-UC195) â†’ treated as ACTIVE, viewable | `BabyDailyLogServiceImpl.getDailyLogDetail()` | `DAILYLOG-TC-006` |
| TC-COND-007 | Log does not exist â†’ 404 | `BabyDailyLogServiceImpl.getDailyLogDetail()` | `DAILYLOG-TC-007` |
| TC-COND-008 | Orphan `baby_id` (FK data-integrity edge, `BabyProfile` missing) â†’ 404, defense-in-depth | `BabyDailyLogServiceImpl.getDailyLogDetail()` | `DAILYLOG-TC-008` |
| TC-COND-009 | Response DTO never contains `diagnosis`/`interpretation`/`condition` | `BabyDailyLogDetailResponse` (structural) | `DAILYLOG-TC-009` |
| TC-COND-010 | Read-only â€” no DB write, no audit event emitted on any path | `BabyDailyLogServiceImpl.getDailyLogDetail()` | `DAILYLOG-TC-010` |
| TC-COND-011 | No JWT â†’ 401 (controller layer) | `BabyDailyLogController.getDailyLogDetail()` | `DAILYLOG-TC-011` |
| TC-COND-012 | Non-`MOTHER`/non-care-role â†’ still routed to service (role gate is `isAuthenticated()`, per UC192 pattern) â€” access enforced at service, not controller | `BabyDailyLogController.getDailyLogDetail()` | `DAILYLOG-TC-012` |
| TC-COND-013 | IDOR â€” unrelated user requests known/guessed `logId` via full auth chain | `BabyDailyLogController` + `BabyDailyLogServiceImpl` | `DAILYLOG-TC-SEC-001` |
| TC-COND-014 | Full stack GET happy path | Integration | `DAILYLOG-TC-INT-001` |
| TC-COND-015 | Full stack GET 403 (unrelated user), no data leaked in error body | Integration | `DAILYLOG-TC-INT-002` |
| TC-COND-016 | Full stack GET 404 (non-existent log) | Integration | `DAILYLOG-TC-INT-003` |
| TC-COND-017 | Mobile: daily log detail screen renders content/type/timestamp | Mobile widget test | `DAILYLOG-TC-MOB-001` |

### TDS-04 â€” Test Techniques

| Technique | Applied To | Rationale |
|--------------|----------------|---------------|
| Equivalence Partitioning | Caller identity: owner / ACCEPTED care-member / unrelated user / unauthenticated | 4 partitions, only owner+member are in the accept partition per Â§16 Auth Matrix |
| Boundary Value Analysis | `status`: NULL (legacy) vs `DELETED` (post-UC195) vs `ACTIVE` | Exercises the nullable-until-UC195 boundary explicitly documented in TDS Â§8.2 |
| Security / Confused Deputy | Path `babyId` deliberately mismatched from the log's real `baby_id` FK | Directly tests Constraint C2 (Â§17) â€” "path param is routing-only, not authorization input" |
| Error Guessing / Security | IDOR via guessed/known `logId` for a baby the caller has no relation to | OWASP A01:2021 |
| Negative Testing | No DB write / no audit event on ANY path (read-only invariant, ADR-BABY-005) | Confirms UC194 does not silently introduce a side effect UC192's `getBabyProfile()` also avoids |
| Structural/Contract Testing | DTO field allow-list â€” response never contains medical-interpretation fields | BR-SAFETY â€” AI/system must never appear to diagnose |

### TDS-05 â€” Test Data Requirements

| Fixture ID | Type | Value / Logic | Má»¥c Ä‘Ã­ch |
|------------|------|--------------------|--------------|
| `FX-194-001` | In-memory | `BabyDailyLog{id=LOG_ID, babyId=BABY_ID, logType="feeding", note="BÃº bÃ¬nh 120ml", status=ACTIVE}` | Happy path |
| `FX-194-002` | In-memory | Same log, `status=DELETED` | 404 soft-deleted case |
| `FX-194-003` | In-memory | Same log, `status=null` | 200 legacy-NULL case (backward compatible) |
| `FX-194-004` | In-memory | `BabyProfile{id=BABY_ID, ownerUserId=OWNER_ID, status=ACTIVE}` | Ownership chain resolution |
| `FX-194-005` | In-memory | `babyDailyLogRepository.findById(NONEXISTENT_ID)` â†’ `Optional.empty()` | 404 never-existed case |
| `FX-194-006` | In-memory | `babyProfileRepository.findById(BABY_ID)` â†’ `Optional.empty()` (orphan FK) | 404 defense-in-depth case |
| `FX-194-007` | JWT/Auth | `{sub: OWNER_ID, roles: [ROLE_MOTHER]}` | Owner caller |
| `FX-194-008` | JWT/Auth | `{sub: CARE_MEMBER_ID, roles: [ROLE_FAMILY]}` ACCEPTED in care group of `BABY_ID` | Care-member caller |
| `FX-194-009` | JWT/Auth | `{sub: UNRELATED_ID, roles: [ROLE_MOTHER]}` | IDOR / non-related caller |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 â€” Báº®T BUá»˜C)

```java
// BabyDailyLogTestFactory.java
class BabyDailyLogTestFactory {

    static final UUID OWNER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID CARE_MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID UNRELATED_ID   = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID BABY_ID        = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID OTHER_BABY_ID  = UUID.fromString("00000000-0000-0000-0000-000000000011");
    static final UUID LOG_ID         = UUID.fromString("00000000-0000-0000-0000-000000000020");
    static final UUID NONEXISTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    static BabyDailyLog makeActiveLog() {
        return BabyDailyLog.builder()
                .id(LOG_ID)
                .babyId(BABY_ID)
                .logType("feeding")
                .startedAt(Instant.parse("2026-07-03T08:00:00Z"))
                .endedAt(Instant.parse("2026-07-03T08:20:00Z"))
                .quantity(new BigDecimal("120"))
                .unit("ml")
                .note("BÃº bÃ¬nh Ä‘á»§ 120ml, khÃ´ng quáº¥y khÃ³c.")
                .recordedBy(OWNER_ID)
                .status(BabyDailyLogStatus.ACTIVE)
                .build();
    }

    static BabyDailyLog makeDeletedLog() {
        BabyDailyLog log = makeActiveLog();
        log.setStatus(BabyDailyLogStatus.DELETED);
        return log;
    }

    static BabyDailyLog makeLegacyNullStatusLog() {
        BabyDailyLog log = makeActiveLog();
        log.setStatus(null); // pre-UC195 legacy row
        return log;
    }

    static BabyProfile makeOwnedProfile() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OWNER_ID)
                .nickname("Bean")
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }
}
```

---

### DAILYLOG-TC-001 â€” Owner views own baby's log detail (happy path)

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogServiceImpl.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED â€” greenfield
**Condition Ref:** `TC-COND-001`
**Oracle Source:** ADR-BABY-004 (this TDS), TDS Â§6.1 Sequence Diagram, Â§8.1 Interface Spec

**Preconditions:** `FX-194-001` mocked from `babyDailyLogRepository.findById(LOG_ID)`; `FX-194-004` mocked from `babyProfileRepository.findById(BABY_ID)`; `babyAccessPolicy.canView(profile, OWNER_ID)` â†’ `true` (real bean or stub).

**Test Steps:**
1. Arrange mocks as above.
2. Act: `babyDailyLogService.getDailyLogDetail(LOG_ID, OWNER_ID)`.
3. Assert: no exception; response has `logType="feeding"`, `note="BÃº bÃ¬nh Ä‘á»§ 120ml, khÃ´ng quáº¥y khÃ³c."`, `babyId=BABY_ID`.

**Expected Result (PASS):** Full detail returned, matching entity fields 1:1 per TDS Â§8.1 DTO shape.
**Expected Result (FAIL):** Exception thrown, or fields missing/mismatched.

**Current Status:** ðŸ”´ Not written
**Implementation Note:** Must load `BabyProfile` via `dailyLog.getBabyId()`, then call `babyAccessPolicy.canView()` â€” reused from UC192, no new policy class (Constraint C1 Â§17).

---

### DAILYLOG-TC-002 â€” ACCEPTED care-group member views log detail â†’ 200

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogServiceImpl.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** TDS Â§16 Auth Matrix (`MOTHER (care member, ACCEPTED)` âœ…)

**Preconditions:** `FX-194-001`, `FX-194-004`; `babyAccessPolicy.canView(profile, CARE_MEMBER_ID)` â†’ `true`.

**Test Steps:**
1. Act: `babyDailyLogService.getDailyLogDetail(LOG_ID, CARE_MEMBER_ID)`.
2. Assert: no exception; response returned successfully.

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-003 â€” Path `babyId` tampered/mismatched â†’ service uses DB-derived `baby_id`, not path param

**Severity:** `CRITICAL`
**CWE:** `CWE-441 â€” Unintended Proxy or Intermediary (Confused Deputy)`
**Feature Under Test:** `BabyDailyLogServiceImpl.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** TDS Â§17 Constraint C2, Â§9.1 Path design note, Logic Issue L2

**Preconditions:** `FX-194-001` (`babyId=BABY_ID` â€” real FK value); the service method signature does NOT accept a separate `babyId` parameter at all (per TDS Â§8.1 â€” only `getDailyLogDetail(UUID babyLogId, UUID callerId)`), so this test operates at the **controller** boundary instead, verifying the service is never even given the path `babyId` to trust.

**Test Steps:**
1. `@WebMvcTest(BabyDailyLogController.class)`; mock `babyDailyLogService.getDailyLogDetail(LOG_ID, OWNER_ID)` to return a valid response.
2. `mockMvc.perform(get("/api/v1/babies/{badBabyId}/daily-logs/{logId}", OTHER_BABY_ID, LOG_ID).header("Authorization", ownerJwt))` â€” path `babyId` deliberately does NOT match the log's real `baby_id` (`BABY_ID`).
3. Assert: `verify(babyDailyLogService).getDailyLogDetail(eq(LOG_ID), eq(OWNER_ID))` â€” controller invoked the service WITHOUT passing `OTHER_BABY_ID` as an authorization input (only `logId`/`callerId` reach the service layer).
4. Assert: response status 200 (request succeeds based on log ownership, irrespective of the mismatched path segment) â€” proving the path `babyId` is routing-only.

**Expected Result (PASS):** Controller never threads the path `babyId` into any authorization decision; service resolves ownership purely from the log's real `baby_id`.
**Expected Result (FAIL):** Controller passes path `babyId` into the service and the service uses it for a shortcut ownership check bypassing the DB-derived value â€” the confused-deputy risk C2 exists to prevent.

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-004 â€” Non-owner, non-member â†’ 403

**Severity:** `CRITICAL`
**CWE:** `CWE-639 â€” Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `BabyDailyLogServiceImpl.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** TDS Â§10 Error Codes (`DAILYLOG-002`), Â§16 Auth Matrix

**Preconditions:** `FX-194-001`, `FX-194-004`; `babyAccessPolicy.canView(profile, UNRELATED_ID)` â†’ `false`.

**Test Steps:**
1. Act: `babyDailyLogService.getDailyLogDetail(LOG_ID, UNRELATED_ID)`.
2. Assert: throws `BusinessException(403, DAILYLOG-002)`.

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-005 â€” Soft-deleted log (`status=DELETED`, post-UC195) â†’ 404

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogServiceImpl.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** TDS Â§6.2 Sequence Diagram â€” Error Path note; Â§17 Constraint C3

**Preconditions:** `FX-194-002` (log with `status=DELETED`) mocked from `findById(LOG_ID)`.

**Test Steps:**
1. Act: `babyDailyLogService.getDailyLogDetail(LOG_ID, OWNER_ID)`.
2. Assert: throws `BusinessException(404, DAILYLOG-001)` â€” NOT 403, regardless of caller's actual access rights (soft-deleted records must not leak existence via a different status code, per Constraint C3).

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-006 â€” Legacy `status=NULL` log (pre-UC195) â†’ treated as ACTIVE, viewable

**Severity:** `MEDIUM`
**Feature Under Test:** `BabyDailyLogServiceImpl.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** TDS Â§8.2 entity comment `@since UC195` â€” "null-check and treat legacy NULL as ACTIVE", Logic Issue L4

**Preconditions:** `FX-194-003` (log with `status=null`) mocked from `findById(LOG_ID)`.

**Test Steps:**
1. Act: `babyDailyLogService.getDailyLogDetail(LOG_ID, OWNER_ID)`.
2. Assert: no exception; response returned successfully (backward-compatible with rows created before UC195's migration lands).

**Expected Result (PASS):** `NULL` status does not throw a `NullPointerException` and is NOT treated as `DELETED`.
**Expected Result (FAIL):** `NullPointerException`, or the row is incorrectly treated as deleted (404) purely because `status` is unset.

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-007 â€” Log does not exist â†’ 404

**Severity:** `MEDIUM`
**Feature Under Test:** `BabyDailyLogServiceImpl.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-007`

**Test Steps:**
1. Mock `babyDailyLogRepository.findById(NONEXISTENT_ID)` â†’ `Optional.empty()`.
2. Act: `babyDailyLogService.getDailyLogDetail(NONEXISTENT_ID, OWNER_ID)`.
3. Assert: throws `BusinessException(404, DAILYLOG-001)`.

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-008 â€” Orphan `baby_id` FK (data-integrity edge) â†’ 404, defense-in-depth

**Severity:** `MEDIUM`
**Feature Under Test:** `BabyDailyLogServiceImpl.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** TDS Â§10 Error Codes â€” `DAILYLOG-001` trigger condition "orphan â€” treat as 404, defense-in-depth"

**Preconditions:** `FX-194-001` (log present, `babyId=BABY_ID`); `FX-194-006` â€” `babyProfileRepository.findById(BABY_ID)` â†’ `Optional.empty()`.

**Test Steps:**
1. Act: `babyDailyLogService.getDailyLogDetail(LOG_ID, OWNER_ID)`.
2. Assert: throws `BusinessException(404, DAILYLOG-001)` â€” NOT a 500, even though this represents unexpected FK-integrity corruption.

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-009 â€” Response DTO never contains diagnosis/interpretation/condition fields

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogDetailResponse` (structural contract)
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** BR-SAFETY, TDS Â§17 Constraint C5

**Test Steps:**
1. Act: `babyDailyLogService.getDailyLogDetail(LOG_ID, OWNER_ID)` (happy path).
2. Assert (reflection-based): `Arrays.stream(BabyDailyLogDetailResponse.class.getDeclaredFields()).map(Field::getName).noneMatch(n -> List.of("diagnosis", "interpretation", "condition").contains(n.toLowerCase()))`.
3. Assert (serialization-based): JSON-serialize the response and assert the resulting string does not contain the substrings `"diagnosis"`, `"interpretation"`, `"condition"`.

**Expected Result (PASS):** Neither the class shape nor the serialized JSON exposes any medical-interpretation field.
**Expected Result (FAIL):** Any such field present â€” would violate BR-SAFETY ("AI provides guidance only; never diagnose" â€” CLAUDE.md).

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-010 â€” Read-only: no DB write, no audit event emitted on any path

**Severity:** `MEDIUM`
**Feature Under Test:** `BabyDailyLogServiceImpl.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogServiceTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** ADR-BABY-005 (this TDS) â€” no default audit-on-read

**Test Steps:**
1. Act: `babyDailyLogService.getDailyLogDetail(LOG_ID, OWNER_ID)` (happy path), then repeat on the 403 path (`TC-004`) and 404 path (`TC-007`).
2. Assert: `verifyNoInteractions(babyDailyLogRepository's save/delete methods)` â€” i.e., `verify(babyDailyLogRepository, never()).save(any())`.
3. Assert: if an `AuditService`/event-publisher bean is injected at all, `verifyNoInteractions()` on it â€” `BabyDailyLogViewed` must NOT be auto-wired/emitted by default.

**Expected Result (PASS):** Zero write-side calls on any path.
**Expected Result (FAIL):** Any `save()`/audit call â€” would contradict ADR-BABY-005's explicit "not activated by default" decision.

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-011 â€” No JWT â†’ 401 (controller layer)

**Severity:** `HIGH`
**Feature Under Test:** `BabyDailyLogController.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogControllerTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-011`

**Test Steps:**
1. `@WebMvcTest(BabyDailyLogController.class)`, no `Authorization` header.
2. `mockMvc.perform(get("/api/v1/babies/{babyId}/daily-logs/{logId}", BABY_ID, LOG_ID))`.
3. Assert status 401.

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-012 â€” Any authenticated role reaches the service (access enforced at service, not controller role-gate)

**Severity:** `MEDIUM`
**Feature Under Test:** `BabyDailyLogController.getDailyLogDetail()`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogControllerTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** TDS Â§9.1 Endpoints Table (`Auth Level: JWT Bearer`, no `hasRole` restriction â€” matches UC192's `isAuthenticated()` pattern, not a role-specific `@PreAuthorize`)

**Test Steps:**
1. `@WebMvcTest(BabyDailyLogController.class)` with a valid JWT for role `FAMILY` (any authenticated role â€” the endpoint does not gate by role at the controller level).
2. `mockMvc.perform(get(...).header("Authorization", familyJwt))`, with `babyDailyLogService.getDailyLogDetail()` mocked to throw `DAILYLOG-002` (service-level denial).
3. Assert: request reaches the service (service method invoked), and the resulting 403 comes from the SERVICE's `BusinessException`, not a controller-level `@PreAuthorize` rejection.

**Expected Result (PASS):** Confirms access control for this endpoint lives entirely in `BabyDailyLogServiceImpl` (via `BabyAccessPolicy`), consistent with UC192's controller pattern â€” no role-based `@PreAuthorize` restriction to bypass/misconfigure.
**Expected Result (FAIL):** Controller has an unintended `@PreAuthorize("hasRole(...)")` that blocks a legitimately-authorized `FAMILY` care-group member before the service can even evaluate `BabyAccessPolicy`.

**Current Status:** ðŸ”´ Not written

---

### SECURITY TEST CASES

---

### DAILYLOG-TC-SEC-001 â€” IDOR: unrelated user requests a known/guessed `logId`

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 â€” Broken Access Control`
**CWE:** `CWE-639`
**Legal:** PDPA â€” unauthorized access to another data subject's infant health/feeding/sleep PII
**Feature Under Test:** `BabyDailyLogController.getDailyLogDetail()` + `BabyDailyLogServiceImpl.getDailyLogDetail()` (full chain)
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyDailyLogControllerTest.java`
**TDD Phase:** ðŸ”´ RED

**Preconditions:** Authenticated as `UNRELATED_ID` (any authenticated role, passes controller gate) targeting `LOG_ID` which belongs to `BABY_ID` owned by `OWNER_ID`; `UNRELATED_ID` has no care-group relationship to `BABY_ID`.

**Test Steps (Attack Simulation):**
1. Authenticate as `UNRELATED_ID`.
2. `GET /api/v1/babies/{BABY_ID}/daily-logs/{LOG_ID}`.
3. Assert response is `403 DAILYLOG-002`.
4. Assert response body does NOT leak any log content (e.g., `note`/`logType` fields absent from the error payload).

**Expected Result (PASS = safe):** `403`, no content leaked in the error response.
**Expected Result (FAIL = vulnerability):** `200` returned with the unrelated baby's daily log content exposed to the attacker.

**Current Status:** ðŸ”´ Not written

---

### INTEGRATION TEST CASES

---

### DAILYLOG-TC-INT-001 â€” Full stack: owner GETs own baby's log detail

**Severity:** `HIGH`
**Feature Under Test:** Full flow: `GET /api/v1/babies/{babyId}/daily-logs/{logId}` â†’ `BabyDailyLogController` â†’ `BabyDailyLogServiceImpl` â†’ `BabyDailyLogRepository`/`BabyProfileRepository`/`BabyAccessPolicy` â†’ PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/baby/integration/BabyDailyLogIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-014`

**Preconditions:** PostgreSQL Testcontainer running; Flyway migrated; seed one `baby_profiles` row (`owner_user_id=OWNER_ID`) and one `baby_daily_logs` row referencing it (`log_type='feeding'`, `note='BÃº bÃ¬nh 120ml'`).

**Test Steps:**
1. Seed profile + log via JPA.
2. `mockMvc.perform(get("/api/v1/babies/{babyId}/daily-logs/{logId}", babyId, logId).header("Authorization", ownerJwt))`.
3. Assert status 200; response body contains `logType`, `note`, `startedAt`.

**DB Assertion:**
```java
// Read-only endpoint â€” assert no row was mutated as a side effect
BabyDailyLog record = babyDailyLogRepository.findById(logId).orElseThrow();
assertThat(record.getUpdatedAt()).isEqualTo(seededUpdatedAt); // unchanged â€” no write side effect
```

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-INT-002 â€” Full stack: unrelated user â†’ 403, no data leaked

**Severity:** `CRITICAL`
**Feature Under Test:** Full flow, access-denial path
**Test File:** `src/test/java/com/carebridge/backend/baby/integration/BabyDailyLogIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-015`

**Test Steps:**
1. Seed profile owned by Mother A + a daily log under it.
2. `GET /api/v1/babies/{babyId}/daily-logs/{logId}` authenticated as an unrelated seeded user.
3. Assert 403, error code `DAILYLOG-002`.
4. Assert response body JSON does not contain the seeded log's `note` text anywhere.

**Current Status:** ðŸ”´ Not written

---

### DAILYLOG-TC-INT-003 â€” Full stack: non-existent log â†’ 404

**Severity:** `MEDIUM`
**Feature Under Test:** Full flow, not-found path
**Test File:** `src/test/java/com/carebridge/backend/baby/integration/BabyDailyLogIntegrationTest.java`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-016`

**Test Steps:**
1. `GET /api/v1/babies/{babyId}/daily-logs/{randomUUID}` authenticated as any seeded Mother.
2. Assert 404, error code `DAILYLOG-001`.

**Current Status:** ðŸ”´ Not written

---

### MOBILE TEST CASES (flutter_test)

---

### DAILYLOG-TC-MOB-001 â€” Daily log detail screen renders content/type/timestamp

**Severity:** `MEDIUM`
**Feature Under Test:** Mobile baby daily log detail screen consuming `GET /api/v1/babies/{babyId}/daily-logs/{logId}`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/baby/baby_daily_log_detail_widget_test.dart`
**TDD Phase:** ðŸ”´ RED
**Condition Ref:** `TC-COND-017`

**Preconditions:** Mock `BabyDailyLogService.getDailyLogDetail()` (new mobile service, analogous to existing `HealthMetricService` pattern under `lib/features/healthRecords/services/`) returns a fixture with `logType: "feeding"`, `note: "BÃº bÃ¬nh 120ml"`, `startedAt: <timestamp>`.

**Test Steps:**
1. `pumpWidget` the daily log detail screen with the mocked service.
2. `await tester.pumpAndSettle()`.
3. Assert `find.text("feeding")` (or its localized label) exists.
4. Assert `find.text("BÃº bÃ¬nh 120ml")` exists.
5. Assert NO widget on screen renders any string containing "diagnosis"/"interpretation"/"condition" (BR-SAFETY, structural mirror of `DAILYLOG-TC-009`).

**Expected Result (PASS):** Content, type, and timestamp render; no medical-interpretation text present.
**Expected Result (FAIL):** Missing fields, or a widget attempts to render a diagnosis-like label (would indicate the mobile layer added logic beyond passthrough display).

**Current Status:** ðŸ”´ Not written
**Implementation Note:** Requires a new mobile `BabyDailyLogService.getDailyLogDetail(String babyId, String logId)` method and detail screen widget â€” neither exists yet; must be added alongside the backend change.

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | ðŸ”´ RED confirmed | ðŸŸ¢ GREEN (commit) | ðŸ”µ REFACTOR note |
|-------|--------------|----------------------|------------------------|------------------------|
| `DAILYLOG-TC-001` | `BabyDailyLogServiceTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-002` | `BabyDailyLogServiceTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-003` | `BabyDailyLogControllerTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-004` | `BabyDailyLogServiceTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-005` | `BabyDailyLogServiceTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-006` | `BabyDailyLogServiceTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-007` | `BabyDailyLogServiceTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-008` | `BabyDailyLogServiceTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-009` | `BabyDailyLogServiceTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-010` | `BabyDailyLogServiceTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-011` | `BabyDailyLogControllerTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-012` | `BabyDailyLogControllerTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-SEC-001` | `BabyDailyLogControllerTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-INT-001` | `BabyDailyLogIntegrationTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-INT-002` | `BabyDailyLogIntegrationTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-INT-003` | `BabyDailyLogIntegrationTest.java` | `[ ]` | `[ ]` | |
| `DAILYLOG-TC-MOB-001` | `baby_daily_log_detail_widget_test.dart` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 â€” GATE-2)

> **Scope note:** UC194 is **entirely greenfield** â€” confirmed by full-text search of `com.carebridge.backend.baby` for any file/class containing "DailyLog": zero results. `BabyDailyLog` entity, `BabyDailyLogStatus` enum, `BabyDailyLogRepository`, `IBabyDailyLogService`, `BabyDailyLogServiceImpl`, `BabyDailyLogDetailResponse`, and `BabyDailyLogController` ALL need to be created. Unlike UC188/UC193 (which extend shipped code), the Red Gate here must stub the **entire new controller/service pair** â€” there is no pre-existing sibling method to leave untouched. `BabyProfileRepository` and `BabyAccessPolicy` (UC192 dependencies) are injected but NOT re-stubbed â€” they keep their real, already-shipped implementations.

**Stub cho Red Phase:**

```java
// BabyDailyLog.java, BabyDailyLogStatus.java, BabyDailyLogRepository.java â€” created per TDS Â§8.2, no stub needed
// (entity/enum/repository carry no business logic to stub; findById() is inherited from JpaRepository)

// IBabyDailyLogService.java â€” NEW interface
public interface IBabyDailyLogService {
    BabyDailyLogDetailResponse getDailyLogDetail(UUID babyLogId, UUID callerId);
}

// BabyDailyLogServiceImpl.java â€” NEW class, entire method stubbed (greenfield â€” no existing method to preserve)
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BabyDailyLogServiceImpl implements IBabyDailyLogService {

    private final BabyDailyLogRepository babyDailyLogRepository;
    private final BabyProfileRepository babyProfileRepository; // reused, UC192 â€” NOT stubbed
    private final BabyAccessPolicy babyAccessPolicy;           // reused, UC192 â€” NOT stubbed

    @Override
    public BabyDailyLogDetailResponse getDailyLogDetail(UUID babyLogId, UUID callerId) {
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub");
    }
}

// BabyDailyLogController.java â€” NEW file, entire endpoint stubbed
@RestController
@RequestMapping("/api/v1/babies/{babyId}/daily-logs")
@RequiredArgsConstructor
public class BabyDailyLogController {

    private final IBabyDailyLogService babyDailyLogService;

    @GetMapping("/{logId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BabyDailyLogDetailResponse>> getDailyLogDetail(
            @PathVariable UUID babyId, @PathVariable UUID logId, Principal principal) {
        throw new UnsupportedOperationException("Not implemented â€” Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (náº¿u PASS báº¥t thÆ°á»ng) |
|-------|-----------------|--------------|-------------|-----------------------------------------|
| `DAILYLOG-TC-001` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-005` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-006` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-009` | `throw` | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |
| `DAILYLOG-TC-INT-001` | `throw` (via controller 500) | ðŸ”´ FAIL | â˜ FAIL â˜ PASS | |

> Note: ALL test files (`BabyDailyLogServiceTest.java`, `BabyDailyLogControllerTest.java`, `BabyDailyLogIntegrationTest.java`) fail at **compile time** initially since none of the classes under test exist yet â€” this is the expected Red Gate signal for a fully greenfield feature, to be confirmed once the stub skeletons above are added.

**Red Gate Evidence:**
- Stub commit hash: `___` (to be filled during implementation)
- Táº¥t cáº£ FAIL? â˜ Yes â†’ **GATE-2 PASS** (T2â†’T3)
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-BABY-IMP-003` (UC194 document) reviewed and Approved
- [ ] ADR-BABY-004/ADR-BABY-005 (this TDS's own numbering) confirmed Accepted (TDS Â§3)
- [ ] Logic Issues (Â§2) confirmed with Tech Lead, especially L1 (stale UC192 error-code mismatch does not leak into UC194) and L4 (nullable `status` handling)
- [ ] Open Item OI-1 (`log_type` vocabulary) acknowledged â€” tests use plain `String`, not `@Enumerated`, per Tech Lead's temporary proposal
- [ ] No migration required for UC-194 itself (confirmed TDS Â§5.2) â€” UC195's migration is a soft dependency for the `status` column, but UC194 must work correctly whether or not it has landed yet (TC-006)

### Exit Criteria
- [ ] `./mvnw test` â€” all unit tests green
- [ ] `./mvnw verify` â€” integration tests green (Testcontainers)
- [ ] Test coverage â‰¥ 80% lines for `BabyDailyLogServiceImpl` and `BabyDailyLogController`
- [ ] No business logic in `BabyDailyLogController` (validation/mapping only)
- [ ] Response DTO does not contain `diagnosis`/`interpretation`/`condition` fields (BR-SAFETY)
- [ ] Mobile: `flutter test` green for `baby_daily_log_detail_widget_test.dart`
- [ ] IDOR test (unrelated user â†’ 403) passes

**Exit Criteria bá»• sung â€” CASE 2.0:**
- [ ] Red Gate (Â§5.1) â€” all tests FAIL against throwing stub before implement (entire module is new)
- [ ] Contract Existence â€” `./mvnw compile` clean, no hallucinated imports
- [ ] Props Isolation â€” all entities built via `BabyDailyLogTestFactory`, no shared mutable state
- [ ] Oracle Source â€” every assert traces to this TDS's ADR-BABY-004/005 or existing schema fact
- [ ] Negative-mutation checks present: no `save()`/audit call on any path (read-only invariant, ADR-BABY-005)
- [ ] No new `BabyAccessPolicy`-equivalent class created â€” confirmed reuse of the existing UC192 policy (AP-AI-003 guard)

### Suspension Criteria
- Product has not confirmed `log_type` vocabulary (Open Item OI-1) â€” does not block THIS Test-Spec (design already accounts for it via plain `String`), but blocks a future whitelist-validation follow-up
- UC195's `status` column migration conflicts with an in-flight parallel migration â€” would require TDS/migration-number coordination first

---

## 7. Rollback Plan

```bash
# No migration to revert for UC-194 itself (the "status" column, if present, belongs to UC195's migration).
git checkout -- src/main/java/com/carebridge/backend/baby/entity/BabyDailyLog.java
git checkout -- src/main/java/com/carebridge/backend/baby/entity/BabyDailyLogStatus.java
git checkout -- src/main/java/com/carebridge/backend/baby/repository/BabyDailyLogRepository.java
git checkout -- src/main/java/com/carebridge/backend/baby/service/IBabyDailyLogService.java
git checkout -- src/main/java/com/carebridge/backend/baby/service/impl/BabyDailyLogServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/baby/dto/BabyDailyLogDetailResponse.java
git checkout -- src/main/java/com/carebridge/backend/baby/controller/BabyDailyLogController.java
git checkout -- src/test/java/com/carebridge/backend/baby/
# NOTE: do NOT touch BabyProfile/BabyAccessPolicy/BabyProfileRepository/BabyController/BabyServiceImpl
# (UC31/UC32/UC192/UC193) â€” this feature only ADDS new files, it does not modify any existing ones.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dáº¥u hiá»‡u | Check | Gate cháº·n |
|-------|--------------|--------------|-------|---------------|
| AP-AI-001 | Unconstrained Generation | TC khÃ´ng reference ADR-BABY-004/005 (this TDS) | â˜ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS vá»›i throwing stub | â˜ | G-2 â˜… |
| AP-AI-003 | Implicit Decision | Test writes/expects a new `BabyDailyLogAccessPolicy` class instead of reusing `BabyAccessPolicy` | â˜ | G-1 |
| AP-AI-004 | Layer Violation | Test verifies `BabyDailyLogController` doing ownership/access logic directly | â˜ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports a repository method not declared in TDS Â§8.2 (e.g., a custom `findByBabyIdAndStatus` not in the TDS) | â˜ | G-3 |
| AP-AI-006 (custom) | Path-param trust | Test/implementation trusts the URL `babyId` for authorization instead of the DB-derived `dailyLog.getBabyId()` | â˜ | G-1 |
| AP-AI-007 (custom) | BR-SAFETY leak | Test/DTO exposes `diagnosis`/`interpretation`/`condition` field | â˜ | G-1 |
| AP-AI-008 (custom) | Stale error-code copy | Test asserts `BABY-002`/`BABY-004` (stale UC192 TDS text) instead of `DAILYLOG-001`/`DAILYLOG-002` | â˜ | G-1 |

**Káº¿t quáº£ review:**
- [ ] KhÃ´ng phÃ¡t hiá»‡n anti-pattern nÃ o â†’ Test-Spec approved
- [ ] PhÃ¡t hiá»‡n AP â†’ ghi vÃ o báº£ng dÆ°á»›i

| AP detected | TC ID | MÃ´ táº£ | Fix action | Fixed? |
|--------------|-------|-------|------------|--------|
| â€” | â€” | â€” | â€” | â˜ |

---

*Test-Spec for UC194 View Baby Daily Log Detail â€” Status: Draft. Awaiting review before Approved.*
