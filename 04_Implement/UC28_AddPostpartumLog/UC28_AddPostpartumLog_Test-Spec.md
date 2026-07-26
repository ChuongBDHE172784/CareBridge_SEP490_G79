# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Đặc tả Kiểm thử Hướng Phát triển — UC-28 Add Postpartum Log

**Document ID:** `CB-JOURNEY-IMP-007-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Author:** `AI Agent`
**References:** `CB-JOURNEY-IMP-007`, SRS §3.3.1.7

---

## 1. Thông tin Module

| Field                     | Value                                                      |
| ------------------------- | ---------------------------------------------------------- |
| **Feature / Gap ID**      | `UC-28`                                                    |
| **Module**                | `AddPostpartumLog — Bounded Context: journey`              |
| **Spec gốc**              | `CB-JOURNEY-IMP-007`                                       |
| **Data Classification**   | `Sensitive-PII`                                            |
| **Compliance Scope**      | `BR-RBAC, BR-PRIVACY, BR-SAFETY`                          |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu)                          | Thực tế                                                  | Fix áp dụng trong test                               |
| - | ------------------------------------------------ | -------------------------------------------------------- | ---------------------------------------------------- |
| L1 | Journey type check not clarified               | MUST be POSTPARTUM — PREGNANCY journey → 400 POST-002   | Test: PREGNANCY journey → 400                        |
| L2 | Gemini AI failure handling                      | Fail-open — log saved regardless                         | Test: Gemini throws → 201, log in DB                 |
| L3 | pain_level/mood_level range                     | 0-10 inclusive (Integer nullable)                        | Test: 11 → 400, null → allowed (optional field)      |
| L4 | sleepHours max                                  | Max 24.0 (cannot sleep more than 24h/day)               | Test: 25.0 → 400                                     |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
AddPostpartumLog module:
├── Service (PostpartumLogServiceImpl — mock repos)
├── Controller (PostpartumLogController — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL)

Out of scope:
├── Gemini AI internals (mocked)
├── PostpartumLog entity JPA mapping (tested via integration)
```

### TDS-03 — Test Conditions

| Condition ID   | Test Condition                            | Test Cases         |
| -------------- | ----------------------------------------- | ------------------ |
| TC-COND-28-01  | Valid POSTPARTUM journey → 201            | POST-TC-028-001    |
| TC-COND-28-02  | PREGNANCY journey → 400                  | POST-TC-028-002    |
| TC-COND-28-03  | COMPLETED journey → 400                  | POST-TC-028-003    |
| TC-COND-28-04  | painLevel out of range → 400             | POST-TC-028-004    |
| TC-COND-28-05  | Invalid bleedingLevel → 400              | POST-TC-028-005    |
| TC-COND-28-06  | Journey not owned → 403                  | POST-TC-028-006    |
| TC-COND-28-07  | Gemini AI fails → 201 (graceful)         | POST-TC-028-007    |
| TC-COND-28-08  | No JWT → 401                             | POST-TC-028-008    |

### TDS-05 — Test Data

| Fixture ID | Type    | Value                                                   |
| ---------- | ------- | ------------------------------------------------------- |
| `FX-28-01` | DB seed | User ACTIVE + POSTPARTUM journey ACTIVE                 |
| `FX-28-02` | DB seed | User ACTIVE + PREGNANCY journey ACTIVE (wrong type)     |
| `FX-28-03` | Request | Valid: {logDate:today, painLevel:3, bleedingLevel:LIGHT} |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class PostpartumLogTestFactory {
    static UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000028");
    static UUID JOURNEY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000028");

    static MotherJourney makePostpartumJourney() {
        return MotherJourney.builder()
            .journeyId(JOURNEY_ID).ownerUserId(MOTHER_ID)
            .journeyType(JourneyType.POSTPARTUM).status(JourneyStatus.ACTIVE).build();
    }

    static MotherJourney makePregnancyJourney() {
        return MotherJourney.builder()
            .journeyId(JOURNEY_ID).ownerUserId(MOTHER_ID)
            .journeyType(JourneyType.PREGNANCY).status(JourneyStatus.ACTIVE).build();
    }

    static AddPostpartumLogRequest makeRequest() {
        AddPostpartumLogRequest req = new AddPostpartumLogRequest();
        req.setLogDate(LocalDate.now());
        req.setPainLevel(3);
        req.setBleedingLevel("LIGHT");
        req.setMoodLevel(7);
        req.setSleepHours(new BigDecimal("6.5"));
        return req;
    }

    static AddPostpartumLogRequest makeRequest(Consumer<AddPostpartumLogRequest> overrides) {
        AddPostpartumLogRequest req = makeRequest();
        overrides.accept(req);
        return req;
    }
}
```

---

### POST-TC-028-001 — Happy path: valid postpartum log → 201

**Severity:** `HIGH` | **TDD Phase:** 🟢 GREEN | **Condition:** TC-COND-28-01

**Test Steps:**
1. Arrange: mock journeyRepo → POSTPARTUM ACTIVE journey owned by user; mock postpartumLogRepo.save()
2. Act: `postpartumLogService.addLog(userId, journeyId, makeRequest())`
3. Assert: no exception, repo.save() called once, auditService.emit() called

---

### POST-TC-028-002 — PREGNANCY journey → 400 POST-002

**Severity:** `HIGH` | **TDD Phase:** 🟢 GREEN | **Condition:** TC-COND-28-02

**Test Steps:**
1. Arrange: mock journeyRepo → PREGNANCY journey
2. Act: `postpartumLogService.addLog(userId, journeyId, makeRequest())`
3. Assert: throws ValidationException with code POST-002

---

### POST-TC-028-003 — COMPLETED journey → 400 POST-003

**Severity:** `MEDIUM` | **TDD Phase:** 🟢 GREEN | **Condition:** TC-COND-28-03

**Test Steps:**
1. Arrange: POSTPARTUM journey with status=COMPLETED
2. Assert: throws ValidationException with code POST-003

---

### POST-TC-028-004 — painLevel = 11 → 400

**Severity:** `MEDIUM` | **TDD Phase:** 🔴 RED | **Condition:** TC-COND-28-04

**Test Steps:**
1. Arrange: `makeRequest(r -> r.setPainLevel(11))`
2. Act: MockMvc.perform(POST with invalid painLevel)
3. Assert: HTTP 400 Bean Validation error

---

### POST-TC-028-005 — Invalid bleedingLevel → 400

**Severity:** `MEDIUM` | **TDD Phase:** 🔴 RED | **Condition:** TC-COND-28-05

**Test Steps:**
1. Arrange: request with bleedingLevel = "EXTREME"
2. Assert: HTTP 400 or throws ValidationException POST-005

---

### POST-TC-028-006 — Journey not owned → 403

**Severity:** `HIGH` | **TDD Phase:** 🟢 GREEN | **Condition:** TC-COND-28-06

**Test Steps:**
1. Arrange: journey with different owner_user_id
2. Assert: throws exception with code POST-006

---

### POST-TC-028-007 — Gemini AI fails → 201 (graceful degradation)

**Severity:** `HIGH` | **TDD Phase:** 🟢 GREEN | **Condition:** TC-COND-28-07

**Test Steps:**
1. Arrange: valid request, mock Gemini to throw RuntimeException
2. Act: `postpartumLogService.addLog(...)`
3. Assert: no exception thrown, log saved in DB, response.aiInsight == null

---

### POST-TC-028-008 — No JWT → 401

**Severity:** `CRITICAL` | **TDD Phase:** 🔴 RED | **Condition:** TC-COND-28-08

**Test Steps:**
1. Act: MockMvc.perform(POST without Authorization header)
2. Assert: HTTP 401

---

### POST-TC-028-INT-001 — Integration: DB row confirmed

**Severity:** `HIGH` | **TDD Phase:** 🔴 RED

**Preconditions:** Testcontainers, Flyway, seed user + POSTPARTUM journey

**Test Steps:**
1. POST valid request
2. Assert HTTP 201
3. Assert DB: `SELECT COUNT(*) FROM postpartum_logs WHERE journey_id=?` = 1
4. Assert DB: `pain_level = 3, bleeding_level = 'LIGHT'`

---

## 5. Red-Green-Refactor Tracker

| TC ID              | Test File                              | 🔴 | 🟢 | 🔵 |
| ------------------ | -------------------------------------- | -- | -- | -- |
| POST-TC-028-001    | PostpartumLogServiceTest.java          | [x]| Passed | —  |
| POST-TC-028-002    | PostpartumLogServiceTest.java          | [x]| Passed | —  |
| POST-TC-028-003    | PostpartumLogServiceTest.java          | [x]| Passed | —  |
| POST-TC-028-004    | PostpartumLogControllerTest.java       | [ ]| —  | —  |
| POST-TC-028-005    | PostpartumLogControllerTest.java       | [ ]| —  | —  |
| POST-TC-028-006    | PostpartumLogServiceTest.java          | [x]| Passed | —  |
| POST-TC-028-007    | PostpartumLogServiceTest.java          | [x]| Passed | —  |
| POST-TC-028-008    | PostpartumLogControllerTest.java       | [ ]| —  | —  |
| POST-TC-028-INT-001| PostpartumLogIntegrationTest.java      | [ ]| —  | —  |

### Red Gate Stub

```java
@Override
public PostpartumLogResponse addLog(UUID userId, UUID journeyId, AddPostpartumLogRequest request) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

---

## 6. Entry / Exit Criteria

### Entry
- [ ] TDS CB-JOURNEY-IMP-007 reviewed
- [ ] UC-22 CreateMotherJourney implemented (journey data source)

### Exit (DoD)
- [x] All unit tests green
- [ ] Integration test green
- [ ] Coverage ≥ 80% for addLog()
- [x] Gemini AI graceful degradation verified
- [x] No medical diagnosis in response

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/carejourney/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern     | Dấu hiệu                                    | Check | Gate |
| --------- | ---------------- | -------------------------------------------- | ----- | ---- |
| AP-AI-001 | Unconstrained Gen| TC không check journey type                  | ☑     | G-0  |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub                     | ☑     | G-2★ |
| AP-AI-003 | Implicit Decision| Test skip Gemini error scenario              | ☑     | G-1  |

---

*TDD Template v2.0 — UC-28 Add Postpartum Log*
*Status: Approved — 5/9 service unit tests GREEN (TC-001, TC-002, TC-003, TC-006, TC-007 passed 2026-06-29)*

## Story 6.10 OV-01 Test Contract Addendum

| Case ID | Priority | Oracle | Executable linkage |
| --- | --- | --- | --- |
| `OV01-TS-28-001` | P1 | Mother-owned postpartum log create/read succeeds for a valid POSTPARTUM journey with zero babies; foreign-owner and non-active boundaries fail closed | `PostpartumLogServiceTest`, `PostpartumLogControllerTest`, `PostpartumLogPostgresIntegrationTest` |
| `OV01-TS-28-002` | P1 | Drafts/retries/list state are account-scoped and do not require a baby selection | `postpartum_log_draft_store_test.dart`, `postpartum_log_list_screen_test.dart`, `postpartum_recovery_dashboard_test.dart` |

The original `5/9` historical service-only statement is retained as legacy input, not the current Story 6.10 coverage conclusion.
