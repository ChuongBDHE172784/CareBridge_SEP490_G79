# TEST-DRIVEN DEVELOPMENT SPECIFICATION TEMPLATE
# UC139 — View Safety Event History

**Document ID:** `FPT-EDU-TDD-UC139-001`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627000007__create_safety_events.sql` — primary schema source (`imu_safety_events`)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627000003__create_emergency_sessions.sql`, `V20260627000004__create_family_alert_log.sql`
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.4.7`
- `04_Implement/UC139_ViewSafetyEventHistory/UC139_ViewSafetyEventHistory_TDS.md` (`CB-SAFETY-IMP-005`)
- `04_Implement/UC136_DetectSuspectedFallOrImpact/UC136_DetectSuspectedFallOrImpact_TDS.md`

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent — Tech Lead` | Khởi tạo tài liệu — TDD spec cho UC139 View Safety Event History (Draft) |
| `2026-07-02` | `AI Agent — Technical Architect (reconciliation pass)` | Cross-batch schema reconciliation (UC137/138/139/140/141): updated `SafetyEventHistoryTestFactory` with `makeSafetyCheckPrompt()`/`makeFamilyAlertLog()`/`makeFamilyAlertRecipient()` factories matching UC137/UC138's confirmed entities. Rewrote `SAFETY-HIST-TC-010`/`SAFETY-HIST-TC-011` to assert exact FK-based `confirmationResult` (was: vague time-window correlation) and to verify the short-circuit behavior when no `safety_check_prompts` row exists. Status remains Draft. |

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
| **Feature / Gap ID** | `UC139-VIEW-HISTORY` |
| **Module** | `Safety Event History — safety bounded context` |
| **Spec gốc** | `CB-SAFETY-IMP-005` |
| **Priority** | 🔴 P0 (Critical, per SRS) |
| **Sprint** | `S2 (per function-spec-task-allocation.md, TV5-Chương)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `imu_safety_events` (UC136), `emergency_sessions` (existing) |
| **Downstream Consumers** | Mobile `safetyMonitoring` feature (list + detail screens) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `UC139_ViewSafetyEventHistory_TDS.md §17`, `ADR-SAFETY-007`, `ADR-SAFETY-008` |
| **Constraints Injected** | Read-only (C1), JWT-derived userId only (C2), 404-not-403 IDOR guard (C3), empty=200 (C4), "suspected" language (C5), no new migration (C6) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC136 TDS references table `safety_events` and endpoint `GET /api/v1/safety/events` as if already implemented | Actual table is `imu_safety_events` (migration `V20260627000007`); `GET /api/v1/safety/events` does NOT exist in current `FallDetectionController` — only `enable`/`disable`/`imu-data` POST endpoints exist | Tests assert against `imu_safety_events` table name and treat `GET /api/v1/safety/events*` as NEW endpoints to be created by this UC, not pre-existing ones |
| L2 | SRS generic template implies UC139 may update records ("The system applies the relevant business rules and processes the request") | `imu_safety_events` has `REVOKE UPDATE, DELETE` at DB level (append-only, confirmed in migration) | Tests assert 405/no route exists for PUT/PATCH/DELETE on `/api/v1/safety/events/**`; only GET verbs tested |
| L3 | No `confirmationResult`/`alertStatus` columns exist yet (sibling UC137/UC138 not merged) | `imu_safety_events` has no `user_response`, `response_at` columns; no `safety_alerts` table exists | Tests treat `confirmationResult`/`alertStatus` as nullable fields that MAY be `null` in the response; no test asserts a non-null value from sibling-UC-owned columns |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Safety Event History bao gồm các layer:
├── Domain (SafetyEventHistoryResponse mapping — pure logic)
├── Repository (ISafetyEventRepository new methods — mock JPA với Mockito for unit; Testcontainers for integration)
├── Service (SafetyEventHistoryService — mock Repository với Mockito)
├── Controller (SafetyEventHistoryController — @WebMvcTest, mock Service)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest, real imu_safety_events rows)
└── Mobile (flutter_test widget tests for list/detail/empty-state screens)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-139` | Happy path listing, AF2 empty state, E1 access denial |
| `ADR-SAFETY-007` | Read-only guarantee, no migration, nullable correlation fields |
| `ADR-SAFETY-008` | Ownership scoping — 404 not 403 for non-owned records |
| `BR-RBAC` | Only `ROLE_MOTHER` can call these endpoints |
| `BR-SAFETY` | "Suspected" language preserved in eventType passthrough |
| `CB-SAFETY-IMP-005 §8, §9, §10` | DTO shape, endpoint contract, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother has N events, requests default page | `SafetyEventHistoryService.getHistory()` | `SAFETY-HIST-TC-001` |
| TC-COND-002 | Mother has 0 events | `SafetyEventHistoryService.getHistory()` | `SAFETY-HIST-TC-002` |
| TC-COND-003 | Mother requests page beyond available data | `SafetyEventHistoryService.getHistory()` | `SAFETY-HIST-TC-003` |
| TC-COND-004 | Mother filters by `eventType=SUSPECTED_FALL` | `ISafetyEventRepository.findByUserIdAndEventTypeOrderByDetectedAtDesc` | `SAFETY-HIST-TC-004` |
| TC-COND-005 | Mother views own event detail | `SafetyEventHistoryService.getEventDetail()` | `SAFETY-HIST-TC-005` |
| TC-COND-006 | User B attempts to view User A's event detail (IDOR) | `SafetyEventHistoryService.getEventDetail()`, `ISafetyEventRepository.findByIdAndUserId` | `SAFETY-HIST-TC-006` |
| TC-COND-007 | Unauthenticated request | `SafetyEventHistoryController` security filter chain | `SAFETY-HIST-TC-007` |
| TC-COND-008 | Non-MOTHER role (e.g. ROLE_EXPERT) requests own-role events | `SafetyEventHistoryController` `@PreAuthorize` | `SAFETY-HIST-TC-008` |
| TC-COND-009 | `size` param exceeds max (100) | `SafetyEventHistoryController` validation | `SAFETY-HIST-TC-009` |
| TC-COND-010 | Event has correlated `emergency_sessions` row within time window | `SafetyEventHistoryService` correlation logic | `SAFETY-HIST-TC-010` |
| TC-COND-011 | Event has NO correlated emergency session (fields stay null) | `SafetyEventHistoryService` correlation logic | `SAFETY-HIST-TC-011` |
| TC-COND-012 | Full flow via Testcontainers | End-to-end DB → API | `SAFETY-HIST-TC-INT-001` |
| TC-COND-013 | Mobile empty-state widget renders | `SafetyHistoryScreen` | `SAFETY-HIST-TC-MOB-001` |
| TC-COND-014 | Mobile list renders items with confirmation/alert badges | `SafetyHistoryScreen` | `SAFETY-HIST-TC-MOB-002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | eventType filter values (valid enum vs invalid string) | Ensure only 3 valid enum values accepted, others rejected as SAFETY-010 |
| Boundary Value Analysis | page size (0, 1, 100, 101) | Confirm max=100 boundary enforced |
| State Transition Testing | N/A (read-only, no FSM) | Not applicable — module has no state machine |
| Error Guessing | IDOR via eventId enumeration, missing JWT, wrong role | Security-focused coverage for ownership scope |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `imu_safety_events` row: `{userId: MOTHER_A, eventType: 'SUSPECTED_FALL', magnitude: 14.2, detectedAt: now-1h}` | Happy path single item |
| `FX-002` | DB seed | 3x `imu_safety_events` rows for `MOTHER_A`, varying `detectedAt` | Ordering/pagination test |
| `FX-003` | DB seed | `imu_safety_events` row for `MOTHER_B` (different user) | IDOR cross-user guard |
| `FX-004` | DB seed | `emergency_sessions` row: `{userId: MOTHER_A, triggerSource: 'FALL_DETECTION', createdAt: FX-001.detectedAt + 1min}` | Correlation happy path (TC-010) |
| `FX-005` | JWT | `{sub: MOTHER_A_uuid, role: 'MOTHER'}` | Auth context for owner |
| `FX-006` | JWT | `{sub: MOTHER_B_uuid, role: 'MOTHER'}` | Auth context for non-owner (IDOR attacker) |
| `FX-007` | JWT | `{sub: EXPERT_uuid, role: 'EXPERT'}` | Wrong-role rejection test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// SafetyEventHistoryTestFactory.java
class SafetyEventHistoryTestFactory {

    static final UUID MOTHER_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID MOTHER_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B1");

    static SafetyEvent makeSafetyEvent() {
        return makeSafetyEvent(e -> {});
    }

    static SafetyEvent makeSafetyEvent(Consumer<SafetyEvent> overrides) {
        SafetyEvent event = SafetyEvent.builder()
                .id(UUID.randomUUID())
                .userId(MOTHER_A_ID)
                .imuSessionId(UUID.randomUUID())
                .eventType(SafetyEventType.SUSPECTED_FALL)
                .magnitude(new BigDecimal("14.2000"))
                .detectedAt(Instant.now().minusSeconds(3600))
                .createdBy("SYSTEM")
                .build();
        overrides.accept(event);
        return event;
    }

    static EmergencySession makeEmergencySession(Consumer<EmergencySession> overrides) {
        EmergencySession session = EmergencySession.builder()
                .id(UUID.randomUUID())
                .userId(MOTHER_A_ID)
                .status("RESOLVED")
                .triggerSource("FALL_DETECTION")
                .createdAt(Instant.now().minusSeconds(3540))
                .build();
        overrides.accept(session);
        return session;
    }

    // NEW (cross-batch reconciliation, 2026-07-02) — UC137's SafetyCheckPrompt, exact FK to imu_safety_events
    static SafetyCheckPrompt makeSafetyCheckPrompt(UUID safetyEventId, Consumer<SafetyCheckPrompt> overrides) {
        SafetyCheckPrompt prompt = SafetyCheckPrompt.builder()
                .id(UUID.randomUUID())
                .safetyEventId(safetyEventId)
                .userId(MOTHER_A_ID)
                .countdownSeconds(30)
                .promptSentAt(Instant.now().minusSeconds(3595))
                .expiresAt(Instant.now().minusSeconds(3565))
                .responseType(null)
                .autoEscalated(false)
                .createdBy("SYSTEM")
                .build();
        overrides.accept(prompt);
        return prompt;
    }

    // NEW (cross-batch reconciliation, 2026-07-02) — UC138's FamilyAlertLog + FamilyAlertRecipient
    static FamilyAlertLog makeFamilyAlertLog(UUID sessionId, Consumer<FamilyAlertLog> overrides) {
        FamilyAlertLog log = FamilyAlertLog.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .sentAt(Instant.now().minusSeconds(3530))
                .recipientCount(1)
                .locationIncluded(true)
                .build();
        overrides.accept(log);
        return log;
    }

    static FamilyAlertRecipient makeFamilyAlertRecipient(UUID familyAlertLogId, Consumer<FamilyAlertRecipient> overrides) {
        FamilyAlertRecipient recipient = FamilyAlertRecipient.builder()
                .id(UUID.randomUUID())
                .familyAlertLogId(familyAlertLogId)
                .recipientUserId(UUID.randomUUID())
                .fcmTokenHash("A".repeat(64))
                .deliveryStatus(DeliveryStatus.SENT)
                .sentAt(Instant.now().minusSeconds(3529))
                .createdBy("SYSTEM")
                .build();
        overrides.accept(recipient);
        return recipient;
    }
}
```

---

### SAFETY-HIST-TC-001 — List history happy path returns items ordered by detectedAt DESC

**Severity:** `HIGH`
**Feature Under Test:** `SafetyEventHistoryService.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyEventHistoryServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS-3.3.4.7 Normal Flow / CB-SAFETY-IMP-005 §6.1`

**Preconditions:**
- FX-002: 3 `imu_safety_events` rows seeded for `MOTHER_A_ID` with distinct `detectedAt`

**Test Steps:**
1. Mock `safetyEventRepository.findByUserIdOrderByDetectedAtDesc(MOTHER_A_ID, pageable)` to return the 3 fixtures in DESC order
2. Call `service.getHistory(MOTHER_A_ID, 0, 20, null)`
3. Assert returned `Page<SafetyEventHistoryResponse>` has 3 elements in the same DESC order

**Expected Result (PASS):**
- `content.size() == 3`, `content.get(0).getDetectedAt()` is the most recent

**Expected Result (FAIL):**
- Wrong order, missing items, or exception thrown

**Current Status:** 🔴 Not written
**Implementation Note:** Reuses existing `findByUserIdOrderByDetectedAtDesc` — no new repository method needed for this case.

---

### SAFETY-HIST-TC-002 — Empty history returns 200 with empty content (AF2)

**Severity:** `HIGH`
**Feature Under Test:** `SafetyEventHistoryController.listHistory()`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyEventHistoryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `SRS-3.3.4.7 AF2`

**Preconditions:**
- Mocked service returns `Page.empty()` for a user with no events

**Test Steps:**
1. `GET /api/v1/safety/events` with FX-005 JWT (no seeded events for this user)
2. Assert HTTP status
3. Assert response body `data.content` is `[]`

**Expected Result (PASS):**
- `200 OK`, `data.content == []`, `data.totalElements == 0`

**Expected Result (FAIL):**
- `404` returned instead of `200` (violates AF2 — empty state must not be an error)

**Current Status:** 🔴 Not written
**Implementation Note:** This is the primary AP-AI-003 anti-pattern regression guard (§17.4 TDS).

---

### SAFETY-HIST-TC-003 — Pagination beyond available data returns empty last page, not error

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyEventHistoryService.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyEventHistoryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `Standard pagination semantics (Spring Data Page contract)`

**Preconditions:** FX-001 seeded (1 record total)

**Test Steps:**
1. Call `service.getHistory(MOTHER_A_ID, page=5, size=20, null)`
2. Assert no exception, `content` is empty, `totalElements == 1`

**Expected Result (PASS):** Empty page returned gracefully
**Expected Result (FAIL):** `IndexOutOfBoundsException` or 500 error

**Current Status:** 🔴 Not written

---

### SAFETY-HIST-TC-004 — Filter by eventType returns only matching records

**Severity:** `MEDIUM`
**Feature Under Test:** `ISafetyEventRepository.findByUserIdAndEventTypeOrderByDetectedAtDesc`
**Test File:** `src/test/java/com/carebridge/backend/safety/repository/SafetyEventHistoryRepositoryTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `SRS-3.3.4.7 AF3 (optional filters)`

**Preconditions:**
- Testcontainers PostgreSQL; seed 2x `SUSPECTED_FALL` and 1x `FALSE_ALARM` for `MOTHER_A_ID`

**Test Steps:**
1. Call `repository.findByUserIdAndEventTypeOrderByDetectedAtDesc(MOTHER_A_ID, SafetyEventType.SUSPECTED_FALL, pageable)`
2. Assert only 2 records returned, both `SUSPECTED_FALL`

**Expected Result (PASS):** 2 records, correct type
**Expected Result (FAIL):** Includes `FALSE_ALARM` record or returns 0

**Current Status:** 🔴 Not written

---

### SAFETY-HIST-TC-005 — Get event detail happy path (owner)

**Severity:** `HIGH`
**Feature Under Test:** `SafetyEventHistoryService.getEventDetail()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyEventHistoryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-SAFETY-IMP-005 §8.1`

**Preconditions:** FX-001 seeded, owned by `MOTHER_A_ID`

**Test Steps:**
1. Mock `repository.findByIdAndUserId(FX-001.id, MOTHER_A_ID)` returns FX-001
2. Call `service.getEventDetail(MOTHER_A_ID, FX-001.id)`
3. Assert returned DTO fields match FX-001

**Expected Result (PASS):** DTO returned with correct `id`, `eventType`, `magnitude`
**Expected Result (FAIL):** Exception or null returned

**Current Status:** 🔴 Not written

---

### SAFETY-HIST-TC-006 — IDOR guard: non-owner gets 404, not 403 or 200 (SECURITY)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `PDPA — unauthorized access to another user's health-safety data`
**Feature Under Test:** `SafetyEventHistoryController.getDetail()` / `SafetyEventHistoryService.getEventDetail()`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyEventHistoryControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SAFETY-008`

**Preconditions:**
- FX-001 (event owned by `MOTHER_A_ID`) seeded
- FX-006 JWT (`MOTHER_B_ID`) available

**Test Steps (Attack Simulation):**
1. Authenticate as `MOTHER_B_ID` (FX-006)
2. `GET /api/v1/safety/events/{FX-001.id}` (event actually belongs to `MOTHER_A_ID`)
3. Assert response status and body

**Expected Result (PASS = hệ thống an toàn):**
- `404 Not Found`, error code `SAFETY-009`, message does NOT reveal the record exists for another user

**Expected Result (FAIL = lỗ hổng tồn tại):**
- `200 OK` with `MOTHER_A_ID`'s data leaked to `MOTHER_B_ID`, OR `403 Forbidden` (which confirms record existence — also a minor info leak per ADR-SAFETY-008 design intent)

**Current Status:** 🔴 Not written

---

### SAFETY-HIST-TC-007 — Unauthenticated request rejected

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `SafetyEventHistoryController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyEventHistoryControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** None (no JWT header)

**Test Steps (Attack Simulation):**
1. `GET /api/v1/safety/events` with no `Authorization` header
2. Assert response status

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`
**Expected Result (FAIL = lỗ hổng tồn tại):** `200 OK` or `500` (unhandled)

**Current Status:** 🔴 Not written

---

### SAFETY-HIST-TC-008 — Wrong role (EXPERT) rejected with 403

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-863 — Incorrect Authorization`
**Feature Under Test:** `SafetyEventHistoryController` `@PreAuthorize("hasRole('MOTHER')")`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyEventHistoryControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-SAFETY-IMP-005 §16 Authorization Matrix`

**Preconditions:** FX-007 JWT (`ROLE_EXPERT`)

**Test Steps:**
1. `GET /api/v1/safety/events` with FX-007 JWT
2. Assert response status

**Expected Result (PASS):** `403 Forbidden`, error code `SAFETY-004`
**Expected Result (FAIL):** `200 OK` — role check bypassed

**Current Status:** 🔴 Not written

---

### SAFETY-HIST-TC-009 — size param exceeding max (100) rejected

**Severity:** `LOW`
**Feature Under Test:** `SafetyEventHistoryController` request validation
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyEventHistoryControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-SAFETY-IMP-005 §4.1 (Pagination NFR)`

**Preconditions:** Valid FX-005 JWT

**Test Steps:**
1. `GET /api/v1/safety/events?size=500`
2. Assert response status and error code

**Expected Result (PASS):** `400 Bad Request`, code `SAFETY-010`
**Expected Result (FAIL):** Request silently accepted / server attempts to load 500 rows

**Current Status:** 🔴 Not written

---

### SAFETY-HIST-TC-010 — Exact FK join to safety_check_prompts populates confirmationResult; correlated alert populates alertStatus

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyEventHistoryService` join logic
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyEventHistoryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-SAFETY-007 §Decision (updated 2026-07-02 — exact FK for confirmationResult, best-effort time-window for alertStatus only)`

**Preconditions:**
- FX-001 (`imu_safety_events` row) seeded
- `SafetyEventHistoryTestFactory.makeSafetyCheckPrompt(FX-001.id, p -> p.setAutoEscalated(true))` — UC137's `safety_check_prompts` row with exact FK `safety_event_id = FX-001.id`
- FX-004 (`emergency_sessions` row within a 2-minute window of the prompt's `escalationTriggeredAt`, `triggerSource=FALL_DETECTION`)
- `SafetyEventHistoryTestFactory.makeFamilyAlertLog(FX-004.id, ...)` + `makeFamilyAlertRecipient(log.id, r -> r.setDeliveryStatus(DeliveryStatus.SENT))`

**Test Steps:**
1. Mock `safetyCheckRepository.findBySafetyEventId(FX-001.id)` to return the seeded `SafetyCheckPrompt`
2. Mock `emergencySessionRepository.findFirstByUserIdAndTriggerSourceAndCreatedAtBetween(...)` to return FX-004 (window anchored on `escalationTriggeredAt`, NOT `detectedAt`)
3. Mock `familyAlertLogRepository.findBySessionId(FX-004.id)` and `familyAlertRecipientRepository.findByFamilyAlertLogId(log.id)` to return the seeded recipient
4. Call `service.getHistory(MOTHER_A_ID, 0, 20, null)`
5. Assert `confirmationResult` on the mapped DTO reflects `"NOT_RESPONDED"` (derived from `autoEscalated=true`)
6. Assert `alertStatus` on the mapped DTO equals `"ALERT_SENT"` (at least one recipient with `deliveryStatus=SENT`)

**Expected Result (PASS):** `confirmationResult` is an EXACT mapping of `safety_check_prompts` state (no ambiguity — verify via `verify(safetyCheckRepository).findBySafetyEventId(FX-001.id)`, not a time-window mock); `alertStatus="ALERT_SENT"` reflecting the correlated `family_alert_recipients` row.
**Expected Result (FAIL):** `confirmationResult` derived from anything other than the exact `safety_check_prompts` FK row, or `alertStatus` stays null despite a correlated, `SENT` recipient existing.

**Current Status:** 🔴 Not written
**Implementation Note:** `confirmationResult` must assert exact correctness (FK-based, not best-effort) — this is a behavior change from the original Draft. `alertStatus` remains best-effort per ADR-SAFETY-007 Trade-offs; do not assert it is "guaranteed correct" beyond what the time-window join (anchored on `escalationTriggeredAt`) produces.

---

### SAFETY-HIST-TC-011 — No safety_check_prompts row / no correlated alert leaves fields null (not error)

**Severity:** `LOW`
**Feature Under Test:** `SafetyEventHistoryService` join logic
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyEventHistoryServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-SAFETY-007`

**Preconditions:** FX-001 seeded, no `safety_check_prompts` row exists for `FX-001.id` (UC137 never triggered for this event, e.g. `FALSE_ALARM` event type), no `emergency_sessions` row correlated

**Test Steps:**
1. Mock `safetyCheckRepository.findBySafetyEventId(FX-001.id)` returns `Optional.empty()`
2. Mock `emergencySessionRepository` lookup returns `Optional.empty()` (or is never called, since no escalation occurred — see Implementation Note)
3. Call `service.getHistory(...)`
4. Assert `confirmationResult` and `alertStatus` are `null` on the DTO, no exception thrown
5. Assert `falsePositiveLabel`/`falsePositiveReason` reflect `imu_safety_events.status`/`false_positive_reason` directly (still populated correctly even when UC137/UC138 data is absent — these two fields never depended on `safety_check_prompts`/`family_alert_log` in the first place)

**Expected Result (PASS):** DTO returned with `confirmationResult=null`, `alertStatus=null`, `200 OK` overall response unaffected; `falsePositiveLabel`/`falsePositiveReason` unaffected by the absence of UC137/UC138 data (UC140's columns are same-row, independent of the other two joins)
**Expected Result (FAIL):** `NullPointerException` or 500 error; OR `falsePositiveLabel`/`falsePositiveReason` incorrectly return null due to a bug that couples them to the `safety_check_prompts`/`family_alert_log` lookups

**Current Status:** 🔴 Not written
**Implementation Note:** Per the updated ADR-SAFETY-007, when `safetyCheckRepository.findBySafetyEventId()` returns empty, the service SHOULD short-circuit and never call `emergencySessionRepository` (no escalation could have occurred without a `safety_check_prompts` row) — assert this short-circuit via `verifyNoInteractions(emergencySessionRepository)` for a stronger regression guard than the original Draft's looser "field stays null" assertion.

---

### INTEGRATION TEST CASES

---

### SAFETY-HIST-TC-INT-001 — Full flow: seed DB → GET list → GET detail

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: imu_safety_events row -> GET /api/v1/safety/events -> GET /api/v1/safety/events/{id}`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyEventHistoryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migrations applied automatically at Spring context start (uses existing `V20260627000007` — no new migration for this UC)
- Seed: insert 2 `imu_safety_events` rows for `MOTHER_A_ID` directly via `ISafetyEventRepository.save()`

**Test Steps:**
1. Seed 2 rows for `MOTHER_A_ID`, 1 row for `MOTHER_B_ID`
2. `GET /api/v1/safety/events` as `MOTHER_A_ID` → assert exactly 2 items, both belong to `MOTHER_A_ID`
3. `GET /api/v1/safety/events/{eventId}` for one of `MOTHER_A_ID`'s events → assert 200 with correct data
4. `GET /api/v1/safety/events/{eventId}` for `MOTHER_B_ID`'s event, authenticated as `MOTHER_A_ID` → assert 404

**Expected Result (PASS):**
- Step 2 returns 2, step 3 returns 200 with matching id, step 4 returns 404 `SAFETY-009`

**Expected Result (FAIL):**
- Step 2 returns 3 (cross-user leak) or step 4 returns 200/403

**DB Assertion:**
```java
Page<SafetyEvent> allForA = safetyEventRepository.findByUserIdOrderByDetectedAtDesc(MOTHER_A_ID, Pageable.ofSize(20));
assertThat(allForA.getTotalElements()).isEqualTo(2);
assertThat(allForA.getContent()).allMatch(e -> e.getUserId().equals(MOTHER_A_ID));
```

**Current Status:** 🔴 Not written

---

### MOBILE WIDGET TEST CASES (Flutter)

---

### SAFETY-HIST-TC-MOB-001 — Empty state widget renders when history is empty

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyHistoryScreen` (mobile)
**Test File:** `05_Development/CareBridgeMobileApp/test/features/safetyMonitoring/screens/safety_history_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:** Mocked repository returns empty list

**Test Steps:**
1. Pump `SafetyHistoryScreen` with a mocked repository/provider returning `[]`
2. Assert an empty-state widget (e.g. `EmptySafetyHistoryPlaceholder`) is found
3. Assert no error/exception widget is shown

**Expected Result (PASS):** Empty-state illustration/text shown, no crash
**Expected Result (FAIL):** Blank screen or error boundary triggered

**Current Status:** 🔴 Not written

---

### SAFETY-HIST-TC-MOB-002 — List renders items with confirmation/alert/false-positive badges

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyHistoryScreen` list item widget
**Test File:** `05_Development/CareBridgeMobileApp/test/features/safetyMonitoring/screens/safety_history_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:** Mocked repository returns 2 items — one with `falsePositiveLabel: true`, one with `null`

**Test Steps:**
1. Pump `SafetyHistoryScreen` with mocked data
2. Assert 2 list tiles rendered
3. Assert the false-positive-labeled item shows a "False positive" badge/chip; the other does not

**Expected Result (PASS):** Correct conditional badge rendering per item
**Expected Result (FAIL):** Badge missing, shown on wrong item, or crash on null field

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SAFETY-HIST-TC-001` | `SafetyEventHistoryServiceTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-002` | `SafetyEventHistoryControllerTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-003` | `SafetyEventHistoryServiceTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-004` | `SafetyEventHistoryRepositoryTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-005` | `SafetyEventHistoryServiceTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-006` | `SafetyEventHistoryControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-007` | `SafetyEventHistoryControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-008` | `SafetyEventHistoryControllerSecurityTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-009` | `SafetyEventHistoryControllerTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-010` | `SafetyEventHistoryServiceTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-011` | `SafetyEventHistoryServiceTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-INT-001` | `SafetyEventHistoryIntegrationTest.java` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-MOB-001` | `safety_history_screen_test.dart` | `[ ]` | `[ ]` | |
| `SAFETY-HIST-TC-MOB-002` | `safety_history_screen_test.dart` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class SafetyEventHistoryService implements ISafetyEventHistoryService {

    @Override
    public Page<SafetyEventHistoryResponse> getHistory(UUID userId, int page, int size, String eventType) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public SafetyEventHistoryResponse getEventDetail(UUID userId, UUID eventId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SAFETY-HIST-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SAFETY-HIST-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SAFETY-HIST-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (pending implementation phase)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-SAFETY-IMP-005` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm
- [ ] No new Flyway migration required (confirmed — see TDS §5.2, §18)
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] `flutter test` — mobile widget tests xanh
- [ ] Test coverage ≥ 80% lines cho `SafetyEventHistoryService`
- [ ] Không có business logic trong Controller (chỉ validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] IDOR guard (`SAFETY-HIST-TC-006`) và unauthenticated guard (`TC-007`) đều PASS trước khi merge

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với stub trước khi implement
- [ ] **Contract Existence** — `./mvnw compile` không lỗi
- [ ] **Props Isolation** — factory pattern dùng nhất quán, không shared mutable state
- [ ] **Oracle Source** — mọi expected value ghi rõ nguồn (đã điền ở mỗi TC)

### Suspension Criteria (Điều kiện tạm dừng)

- UC136 `imu_safety_events` schema thay đổi bất ngờ (breaking)
- Phát hiện UC137/UC138 đã merge cột mới vào `imu_safety_events` → cần re-plan correlation logic (ADR-SAFETY-007 exception path)

---

## 7. Rollback Plan

```bash
# No migration to revert for UC139.
git checkout -- src/main/java/com/carebridge/backend/safety/controller/SafetyEventHistoryController.java
git checkout -- src/main/java/com/carebridge/backend/safety/service/SafetyEventHistoryService.java
git checkout -- src/main/java/com/carebridge/backend/safety/service/ISafetyEventHistoryService.java
git checkout -- src/test/java/com/carebridge/backend/safety/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/safetyMonitoring/
git checkout -- 05_Development/CareBridgeMobileApp/test/features/safetyMonitoring/

# Gap vẫn OPEN → giữ nguyên entry trong TDS §18 Open Items
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (tất cả TC reference §17 TDS constraints) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ *(pending Red Gate run during implementation)* | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (correlation behavior traced to ADR-SAFETY-007) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (controller tests only check HTTP status/auth, not business rules) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ (`IEmergencySessionRepository` flagged as NEW in TDS §8.2 — not assumed pre-existing) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở mức spec (Draft) — TDD spec pending human approval
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |
