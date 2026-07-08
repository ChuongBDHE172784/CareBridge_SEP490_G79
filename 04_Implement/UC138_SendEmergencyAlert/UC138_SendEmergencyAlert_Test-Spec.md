# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC138 — Send Emergency Alert

**Document ID:** `CB-SAFETY-IMP-006-TDD`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627000004__create_family_alert_log.sql` — existing `family_alert_log` table
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260705090100__create_family_alert_recipients.sql` — new migration (this feature)
- `04_Implement/UC138_SendEmergencyAlert/UC138_SendEmergencyAlert_TDS.md` (`CB-SAFETY-IMP-006`)
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/service/impl/FamilyAlertService.java` (existing, extended by this feature)
- `04_Implement/UC137_ConfirmSafetyCheck/UC137_ConfirmSafetyCheck_TDS.md` (upstream trigger)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC138 Send Emergency Alert |

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
| **Feature / Gap ID** | `GAP-UC138` |
| **Module** | `Send Emergency Alert — emergency bounded context (extends FamilyAlertService)` |
| **Spec gốc** | `CB-SAFETY-IMP-006` |
| **Priority** | 🔴 P0 — life-safety critical (family notification path) |
| **Sprint** | `Sprint 2 — IMU Detection And Emergency Alert` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC62 EmergencySessionOpened (existing)`, `UC137 EmergencyEscalationTriggered` |
| **Downstream Consumers** | `EmergencyAlertStatusController` (new GET endpoint), future `UC139 ViewSafetyEventHistory` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SAFETY-IMP-006 §17` |
| **Constraints Injected** | C1 (no duplicate alert service — extend `FamilyAlertService`), C2 (minimal payload per ADR-SAFETY-011), C3 (location only with consent), C4 (`sendAlert()` must never throw), C5 (FCM tokens hashed before persist), C6 (idempotency guard preserved) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Task brief referenced `safety_alerts(recipient_user_id, delivery_status, sent_at, acknowledged_at)` | This table does NOT exist. Real table is `family_alert_log` (1 row/session aggregate, no per-recipient columns) — verified in `V20260627000004__create_family_alert_log.sql` | Tests assert against the NEW `family_alert_recipients` child table (via `V20260705090100` migration), never a non-existent `safety_alerts` table |
| L2 | Assumed UC138 needs a brand-new alert service | Actual: `FamilyAlertService.sendAlert()` already exists and is auto-triggered by `EmergencySessionOpenedHandler` (UC65-equivalent) | Tests verify the EXTENDED `FamilyAlertService`, never a new/duplicate `EmergencyAlertService` class (see AP-AI-001 below — hard gate) |
| L3 | SRS text does not enumerate exact "minimal alert" field list | `ADR-SAFETY-011` formalizes INCLUDE (`type`, `sessionId`, `triggerSource`, `lat/lon` if consented) and EXCLUDE (IMU raw data, diagnosis language, address/location history) lists | Tests assert payload composition exactly matches ADR-SAFETY-011's INCLUDE list and assert absence of EXCLUDE fields |
| L4 | `FcmNotificationPort.sendBatch()` is all-or-nothing (no per-token result) | `delivery_status` therefore can only reflect "batch call succeeded/failed", not true per-device delivery — documented Open item in TDS §Phụ lục B | Tests assert `delivery_status` reflects batch-outcome semantics only; do NOT assert per-token delivery guarantees the current `FcmNotificationPort` contract cannot provide |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Send Emergency Alert bao gồm các layer:
├── Domain (FamilyAlertRecipient entity, DeliveryStatus enum — pure logic)
├── Services (FamilyAlertService — mock IFamilyAlertLogRepository, IFamilyAlertRecipientRepository,
│              FamilyMemberPort, LocationConsentPort, FcmNotificationPort, ApplicationEventPublisher với Mockito)
├── Controller (EmergencyAlertStatusController — mock IFamilyAlertService/query service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — full flow EmergencySessionOpened → family_alert_log → family_alert_recipients)
└── (No dedicated Mobile screen new in this TDS beyond the existing alert-status read — UC138 is primarily
     an event-driven backend extension; any Mobile display of alert-status is out of this Test-Spec's scope
     per TDS §9 "no direct HTTP trigger from Mother")
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-138 §3.3.4.6` | Minimal alert sent to configured family members; delivery status recorded |
| `ADR-SAFETY-010` | Extend existing `FamilyAlertService`, do not duplicate |
| `ADR-SAFETY-011` | Minimal alert content INCLUDE/EXCLUDE list (PDPA minimization) |
| `ADR-SAFETY-012` | Per-recipient `family_alert_recipients` table, hashed FCM tokens |
| `BR-SAFETY` | `sendAlert()` MUST NEVER throw — never block emergency routing |
| `BR-PRIVACY` | Location only with consent; minimal payload |
| `BR-RBAC` | Only owning Mother may view her own alert-status |
| `CB-SAFETY-IMP-006 §8` | Service/Repository interface contracts |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Alert dispatched to all family members, per-recipient rows created | `FamilyAlertService.sendAlert()` | `ALERT138-TC-001` |
| TC-COND-002 | Minimal payload — no IMU/diagnosis data ever included | `FamilyAlertService.sendAlert()` (payload builder) | `ALERT138-TC-002` (CRITICAL — PDPA) |
| TC-COND-003 | FCM batch failure recorded as FAILED, does not throw | `FamilyAlertService.sendAlert()` | `ALERT138-TC-003` (CRITICAL) |
| TC-COND-004 | No family members configured — no alert log/recipients written | `FamilyAlertService.sendAlert()` | `ALERT138-TC-004` |
| TC-COND-005 | Idempotency — duplicate `EmergencySessionOpened` does not double-send | `FamilyAlertService.sendAlert()` | `ALERT138-TC-005` |
| TC-COND-006 | Location excluded when consent=false | `FamilyAlertService.sendAlert()` | `ALERT138-TC-006` |
| TC-COND-007 | FCM tokens persisted as SHA-256 hash, never raw | `FamilyAlertService.sendAlert()` | `ALERT138-TC-007` |
| TC-COND-008 | `EmergencyAlertSent` published alongside existing `FamilyAlertSent` (backward compat) | `FamilyAlertService.sendAlert()` | `ALERT138-TC-008` |
| TC-COND-009 | `EmergencyAlertDeliveryFailed` published on whole-batch FCM failure | `FamilyAlertService.sendAlert()` | `ALERT138-TC-009` |
| TC-COND-010 | Non-owner cannot view another Mother's alert status | `EmergencyAlertStatusController` | `ALERT138-TC-010` |
| TC-COND-011 | No alert found for session → 404 EMERG-004 | `EmergencyAlertStatusController` | `ALERT138-TC-011` |
| TC-COND-012 | Full E2E: EmergencySessionOpened → family_alert_log + family_alert_recipients reconciled | Integration | `ALERT138-TC-INT-001` (CRITICAL) |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `DeliveryStatus` (SENT / FAILED) | Ensure enum boundary validated at persistence layer |
| Boundary Value Analysis | `familyFcmTokens` list size 0, 1, N | No-family edge case vs. batch dispatch correctness |
| Decision Table | `hasLocationConsent` × `sendBatch outcome` (4 combinations) | Verify payload/consent and delivery-status logic independently combine correctly |
| Error Guessing | FCM adapter throws mid-batch, DB save fails after successful FCM call | Verify no partial/inconsistent state (log written without recipients, or vice versa) |
| Security Testing (IDOR) | `GET /alert-status` cross-tenant read attempt | Ownership boundary (`EMERG-005`) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | In-memory | `EmergencySessionOpened{sessionId, userId=MOTHER_ID, triggerSource="FALL_DETECTION_UNCONFIRMED"}` | Happy path trigger event |
| `FX-002` | Mock | `FamilyMemberPort.getFamilyFcmTokens(MOTHER_ID)` → `["tokenA","tokenB"]` | 2-recipient batch |
| `FX-003` | Mock | `FamilyMemberPort.getFamilyFcmTokens(MOTHER_ID)` → `[]` | No-family edge case |
| `FX-004` | Mock | `LocationConsentPort.hasLocationConsent(MOTHER_ID)` → `true` / `false` | Consent branch |
| `FX-005` | Mock | `FcmNotificationPort.sendBatch(...)` throws `RuntimeException("FCM outage")` | Batch failure path |
| `FX-006` | DB seed | `family_alert_log{sessionId=SESSION_ID, recipientCount=2}` (pre-existing) | Idempotency guard test |
| `FX-007` | JWT | `{ sub: 'mother-001', role: 'ROLE_MOTHER' }` | Auth context, owner |
| `FX-008` | JWT | `{ sub: 'mother-002', role: 'ROLE_MOTHER' }` | Non-owner attacker |

---

## 4. Test Case Specification

> **TC ID format:** `ALERT138-TC-[NNN]`

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// SafetyEventTestFactory.java (reused/extended — see UC137/UC136 usage)
// ═══════════════════════════════════════════════════════════
class SafetyEventTestFactory {

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    static final UUID OTHER_MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000B2");
    static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000C3");
    static final UUID FAMILY_ALERT_LOG_ID = UUID.fromString("00000000-0000-0000-0000-0000000000D4");

    static EmergencySessionOpened makeEmergencySessionOpened() {
        return makeEmergencySessionOpened(e -> {});
    }

    static EmergencySessionOpened makeEmergencySessionOpened(Consumer<EmergencySessionOpenedBuilder> overrides) {
        EmergencySessionOpenedBuilder b = EmergencySessionOpened.builder()
                .eventId(UUID.randomUUID())
                .sessionId(SESSION_ID)
                .userId(MOTHER_ID)
                .triggerSource("FALL_DETECTION_UNCONFIRMED")
                .occurredAt(Instant.now());
        overrides.accept(b);
        return b.build();
    }

    static FamilyAlertLog makeFamilyAlertLog() {
        return FamilyAlertLog.builder()
                .id(FAMILY_ALERT_LOG_ID)
                .sessionId(SESSION_ID)
                .sentAt(Instant.now())
                .recipientCount(2)
                .locationIncluded(true)
                .build();
    }

    static FamilyAlertRecipient makeSentRecipient(UUID recipientUserId) {
        return FamilyAlertRecipient.builder()
                .id(UUID.randomUUID())
                .familyAlertLogId(FAMILY_ALERT_LOG_ID)
                .recipientUserId(recipientUserId)
                .fcmTokenHash("A".repeat(64)) // synthetic 64-char hex placeholder
                .deliveryStatus(DeliveryStatus.SENT)
                .sentAt(Instant.now())
                .createdBy("SYSTEM")
                .build();
    }

    static List<String> makeFcmTokens(int count) {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tokens.add("fcm-token-synthetic-" + i);
        }
        return tokens;
    }
}
```

---

### ALERT138-TC-001 — Alert dispatched to all family members with per-recipient tracking

**Severity:** `CRITICAL`
**Feature Under Test:** `FamilyAlertService.sendAlert(EmergencySessionOpened)`
**Test File:** `src/test/java/com/carebridge/backend/emergency/service/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SAFETY-012 §Decision`, `SRS §3.3.4.6`

**Preconditions:**
- `FX-001` event; `FX-002` 2 FCM tokens; `FX-004` consent=true; `familyAlertLogRepository.existsBySessionId()` returns `false`

**Test Steps:**
1. Arrange: mocks as above; `fcmNotificationPort.sendBatch()` succeeds (void, no throw)
2. Act: call `sendAlert(makeEmergencySessionOpened())`
3. Assert: `familyAlertLogRepository.save()` called once with `recipientCount=2`
4. Assert: `familyAlertRecipientRepository.saveAll()` called with a list of exactly 2 `FamilyAlertRecipient`, each `deliveryStatus=SENT`

**Expected Result (PASS):** `family_alert_log` + 2 `family_alert_recipients` rows created; counts reconcile.
**Expected Result (FAIL):** Recipient count mismatch, or `saveAll()` not invoked.

**Current Status:** 🔴 Not written

---

### ALERT138-TC-002 — CRITICAL: Minimal payload — no IMU/diagnosis data ever included

**Severity:** `CRITICAL`
**CWE:** `CWE-359 — Exposure of Private Personal Information`
**Feature Under Test:** `FamilyAlertService.sendAlert()` (payload builder)
**Test File:** `src/test/java/com/carebridge/backend/emergency/service/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SAFETY-011 §Decision (INCLUDE/EXCLUDE list)`, `BR-PRIVACY`

**Preconditions:** `FX-001` event with `triggerSource="FALL_DETECTION_UNCONFIRMED"`; consent=true

**Test Steps:**
1. Act: `sendAlert(event)`
2. Capture the payload map/object passed to `fcmNotificationPort.sendBatch(tokens, payload)` via `ArgumentCaptor`
3. Assert payload contains ONLY: `type`, `sessionId`, `triggerSource`, `latitude`, `longitude`
4. Assert payload does NOT contain any key matching `accelerometer|gyroscope|magnitude|diagnosis|imu`

**Expected Result (PASS = compliant):** Payload keys are exactly the ADR-SAFETY-011 INCLUDE list; no EXCLUDE-listed field present.
**Expected Result (FAIL = PDPA violation):** Any raw IMU field or diagnosis-language string leaks into the FCM payload — reportable PDPA incident per TDS §12.3.

**Current Status:** 🔴 Not written
**Implementation Note:** This is the payload-minimization guard test — treat with the same rigor as a life-safety test; a regression here is a compliance incident, not just a bug.

---

### ALERT138-TC-003 — CRITICAL: FCM batch failure recorded as FAILED, does not throw

**Severity:** `CRITICAL`
**Feature Under Test:** `FamilyAlertService.sendAlert()`
**Test File:** `src/test/java/com/carebridge/backend/emergency/service/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SAFETY-010 §Decision`, `BR-SAFETY (CRITICAL)`

**Preconditions:** `FX-005` — `fcmNotificationPort.sendBatch()` throws `RuntimeException`

**Test Steps:**
1. Act: call `sendAlert(makeEmergencySessionOpened())`
2. Assert: no exception propagates out of `sendAlert()` (wrap call in try/catch in the test and fail if caught)
3. Assert: `familyAlertRecipientRepository.saveAll()` called with recipients where `deliveryStatus=FAILED`
4. Assert: `eventPublisher.publishEvent()` called with `EmergencyAlertDeliveryFailed`

**Expected Result (PASS):** Method completes normally; failure recorded, not thrown.
**Expected Result (FAIL):** Exception bubbles up to `EmergencySessionOpenedHandler` — violates "never delay/block emergency routing" (BR-SAFETY).

**Current Status:** 🔴 Not written
**Implementation Note:** Equal-severity sibling to UC137's `SAFETY137-TC-007` (FCM failure must never block safety flow) — same architectural invariant applied to the alert-dispatch side.

---

### ALERT138-TC-004 — No family members configured — no alert log/recipients written

**Severity:** `MEDIUM`
**Feature Under Test:** `FamilyAlertService.sendAlert()`
**Test File:** `src/test/java/com/carebridge/backend/emergency/service/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §6.3 Sequence Diagram — No family members configured`

**Preconditions:** `FX-003` — `familyMemberPort.getFamilyFcmTokens()` returns `[]`

**Test Steps:**
1. Act: `sendAlert(event)`
2. Assert: `familyAlertLogRepository.save()` never invoked
3. Assert: `familyAlertRecipientRepository.saveAll()` never invoked
4. Assert: `fcmNotificationPort.sendBatch()` never invoked

**Expected Result (PASS):** No-op persisted state; behavior matches existing (preserved) `FamilyAlertService` logic.
**Expected Result (FAIL):** Empty-token batch call attempted, or a phantom `family_alert_log` row created with `recipientCount=0`.

**Current Status:** 🔴 Not written

---

### ALERT138-TC-005 — Idempotency: duplicate EmergencySessionOpened does not double-send

**Severity:** `HIGH`
**Feature Under Test:** `FamilyAlertService.sendAlert()`
**Test File:** `src/test/java/com/carebridge/backend/emergency/service/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-SAFETY-010 (C6 inherited UC65 guard)`

**Preconditions:** `familyAlertLogRepository.existsBySessionId(SESSION_ID)` returns `true` (already sent)

**Test Steps:**
1. Act: `sendAlert(makeEmergencySessionOpened())`
2. Assert: `familyMemberPort.getFamilyFcmTokens()` never invoked (short-circuits before lookup)
3. Assert: no new `family_alert_log`/`family_alert_recipients` rows created

**Expected Result (PASS):** Existing idempotency guard (`existsBySessionId`) preserved — no duplicate dispatch.
**Expected Result (FAIL):** Second FCM batch sent for the same session — family receives duplicate alerts, violates C6.

**Current Status:** 🔴 Not written

---

### ALERT138-TC-006 — Location excluded when consent=false

**Severity:** `CRITICAL`
**CWE:** `CWE-359 — Exposure of Private Personal Information`
**Feature Under Test:** `FamilyAlertService.sendAlert()`
**Test File:** `src/test/java/com/carebridge/backend/emergency/service/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-PRIVACY`, `ADR-SAFETY-011 (C3)`

**Preconditions:** `locationConsentPort.hasLocationConsent(MOTHER_ID)` returns `false`

**Test Steps:**
1. Act: `sendAlert(event)`
2. Capture payload via `ArgumentCaptor`
3. Assert payload does NOT contain `latitude`/`longitude` keys
4. Assert `family_alert_log.locationIncluded == false`

**Expected Result (PASS = compliant):** No location data leaves the backend without consent.
**Expected Result (FAIL = privacy violation):** Location present in payload despite `hasLocationConsent()==false`.

**Current Status:** 🔴 Not written

---

### ALERT138-TC-007 — FCM tokens persisted as SHA-256 hash, never raw

**Severity:** `HIGH`
**CWE:** `CWE-312 — Cleartext Storage of Sensitive Information`
**Feature Under Test:** `FamilyAlertService.sendAlert()`
**Test File:** `src/test/java/com/carebridge/backend/emergency/service/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-SAFETY-012 §Decision (C5)`

**Preconditions:** `FX-002` 2 raw FCM tokens

**Test Steps:**
1. Act: `sendAlert(event)`
2. Capture `List<FamilyAlertRecipient>` passed to `saveAll()`
3. Assert each `fcmTokenHash` is a 64-char hex string (SHA-256 format) and does NOT equal the raw token value

**Expected Result (PASS):** All persisted hashes are 64-char hex, distinct from raw tokens.
**Expected Result (FAIL):** Raw token string persisted verbatim in `fcm_token_hash` column — audit-table exposure risk.

**Current Status:** 🔴 Not written

---

### ALERT138-TC-008 — EmergencyAlertSent published alongside existing FamilyAlertSent

**Severity:** `MEDIUM`
**Feature Under Test:** `FamilyAlertService.sendAlert()`
**Test File:** `src/test/java/com/carebridge/backend/emergency/service/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §7.1 Domain Event Catalog`

**Preconditions:** Happy path, `FX-001`/`FX-002`/`FX-004`(true)

**Test Steps:**
1. Act: `sendAlert(event)`
2. Assert: `eventPublisher.publishEvent()` invoked with an instance of `FamilyAlertSent` (existing, unchanged payload)
3. Assert: `eventPublisher.publishEvent()` ALSO invoked with an instance of `EmergencyAlertSent` (new, SRS-aligned alias)

**Expected Result (PASS):** Both events published — backward compatibility preserved, SRS traceability satisfied.
**Expected Result (FAIL):** `FamilyAlertSent` removed/renamed (breaks any future consumer already planned against the old name) — violates TDS §7.3 note.

**Current Status:** 🔴 Not written

---

### ALERT138-TC-009 — EmergencyAlertDeliveryFailed published on whole-batch FCM failure

**Severity:** `HIGH`
**Feature Under Test:** `FamilyAlertService.sendAlert()`
**Test File:** `src/test/java/com/carebridge/backend/emergency/service/FamilyAlertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §6.2 Sequence Diagram — FCM Batch Failure`

**Preconditions:** `FX-005` FCM throws

**Test Steps:**
1. Act: `sendAlert(event)`
2. Assert: `EmergencyAlertDeliveryFailed{sessionId, attemptedRecipientCount=2, failureReason}` published
3. Assert `failureReason` does NOT contain a raw stack trace or PII (truncated message only, per payload schema §7.3)

**Expected Result (PASS):** Event published with sanitized failure reason.
**Expected Result (FAIL):** No event published (ops blind to failure), or `failureReason` leaks stack trace/PII.

**Current Status:** 🔴 Not written

---

### ALERT138-TC-010 — Non-owner cannot view another Mother's alert status

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key (IDOR)`
**Feature Under Test:** `GET /api/v1/emergency/sessions/{sessionId}/alert-status`
**Test File:** `src/test/java/com/carebridge/backend/emergency/controller/EmergencyAlertStatusControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §16 Authorization Matrix`, `EMERG-005`

**Preconditions:** `sessionId` belongs to `MOTHER_ID`; request JWT is `OTHER_MOTHER_ID`

**Test Steps (Attack Simulation):**
1. `GET /api/v1/emergency/sessions/{sessionId}/alert-status` with `OTHER_MOTHER_ID` JWT
2. Assert `403 Forbidden`, `{code: "EMERG-005"}`

**Expected Result (PASS = hệ thống an toàn):** 403 EMERG-005; no recipient data leaked in response body.
**Expected Result (FAIL = lỗ hổng tồn tại):** Attacker can view another Mother's family alert recipients (family member identities) — cross-tenant PII leak.

**Current Status:** 🔴 Not written

---

### ALERT138-TC-011 — No alert found for session → 404 EMERG-004

**Severity:** `LOW`
**Feature Under Test:** `GET /api/v1/emergency/sessions/{sessionId}/alert-status`
**Test File:** `src/test/java/com/carebridge/backend/emergency/controller/EmergencyAlertStatusControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS §10 Error Codes`

**Preconditions:** `familyAlertLogRepository.existsBySessionId(sessionId)` returns `false`; owning Mother JWT

**Test Steps:**
1. `GET /api/v1/emergency/sessions/{sessionId}/alert-status` as owning Mother
2. Assert `404 Not Found`, `{code: "EMERG-004"}`

**Expected Result (PASS):** Clean 404 with correct error code.
**Expected Result (FAIL):** 500 error or wrong error code returned for a legitimately-missing alert (e.g., no family configured case).

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### ALERT138-TC-INT-001 — Full E2E: EmergencySessionOpened → family_alert_log + family_alert_recipients reconciled

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: EmergencySessionOpened → EmergencySessionOpenedHandler → FamilyAlertService.sendAlert() → family_alert_log + family_alert_recipients`
**Test File:** `src/test/java/com/carebridge/backend/emergency/FamilyAlertServiceIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migrations applied automatically (incl. `V20260705090100`)
- `FamilyMemberPortAdapter` stubbed (via `@MockBean`) to return 2 FCM tokens (real adapter returns empty list — known gap, TDS §Phụ lục B)
- `LocationConsentPort` stubbed to return `true`
- `FcmNotificationPort` stubbed (test double) to succeed

**Test Steps:**
1. Publish `EmergencySessionOpened` for a seeded `emergency_sessions` row
2. Wait for async event handling (`awaitility`, timeout 5s)
3. Assert `family_alert_log` row exists with `recipient_count=2`
4. Assert exactly 2 `family_alert_recipients` rows exist, `family_alert_log_id` matching, `delivery_status='SENT'`
5. Run the reconciliation query from TDS §14 — assert 0 mismatched rows
6. Assert `fcm_token_hash` values are all 64-char hex (no raw token stored)

**Expected Result (PASS):**
- All rows created and reconciled 1:1
- No raw FCM token found anywhere in `family_alert_recipients`

**Expected Result (FAIL):**
- Any reconciliation mismatch — indicates a partial-write bug (log written without recipients, or vice versa)

**DB Assertion:**
```java
FamilyAlertLog log = familyAlertLogRepository.findBySessionId(sessionId).orElseThrow();
assertThat(log.getRecipientCount()).isEqualTo(2);

List<FamilyAlertRecipient> recipients = familyAlertRecipientRepository.findByFamilyAlertLogId(log.getId());
assertThat(recipients).hasSize(2);
assertThat(recipients).allSatisfy(r -> assertThat(r.getFcmTokenHash()).matches("^[a-f0-9]{64}$"));
assertThat(recipients).allSatisfy(r -> assertThat(r.getDeliveryStatus()).isEqualTo(DeliveryStatus.SENT));
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `ALERT138-TC-001` | `FamilyAlertServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `ALERT138-TC-002` | `FamilyAlertServiceTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL — PDPA minimization |
| `ALERT138-TC-003` | `FamilyAlertServiceTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL — never block emergency routing |
| `ALERT138-TC-004` | `FamilyAlertServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `ALERT138-TC-005` | `FamilyAlertServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `ALERT138-TC-006` | `FamilyAlertServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `ALERT138-TC-007` | `FamilyAlertServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `ALERT138-TC-008` | `FamilyAlertServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `ALERT138-TC-009` | `FamilyAlertServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `ALERT138-TC-010` | `EmergencyAlertStatusControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `ALERT138-TC-011` | `EmergencyAlertStatusControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `ALERT138-TC-INT-001` | `FamilyAlertServiceIntegrationTest.java:TBD` | `[ ]` | `[ ]` | CRITICAL E2E |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class FamilyAlertService implements IFamilyAlertService {

    @Override
    public void sendAlert(EmergencySessionOpened event) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```java
@RestController
public class EmergencyAlertStatusController {

    @GetMapping("/api/v1/emergency/sessions/{sessionId}/alert-status")
    public AlertStatusResponse getAlertStatus(@PathVariable UUID sessionId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `ALERT138-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `ALERT138-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `ALERT138-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` *(fill during implementation phase)*
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-SAFETY-IMP-006` reviewed and approved (Status field = `Approved`)
- [ ] Logic Issues (Section 2) confirmed against actual codebase (`family_alert_log`, `FamilyAlertService` — done above)
- [ ] Flyway migration `V20260705090100` reviewed and staged
- [ ] `ADR-SAFETY-010/011/012` reviewed; DPO sign-off on minimal-alert-content list obtained or explicitly tracked as blocking
- [ ] Existing `FamilyAlertService`/`EmergencySessionOpenedHandler` wiring verified present in codebase

### Exit Criteria (DoD)

- [ ] `./mvnw test` — all unit tests green
- [ ] `./mvnw verify` — integration test (`ALERT138-TC-INT-001`) green with Testcontainers
- [ ] Test coverage ≥ 80% lines for `FamilyAlertService`
- [ ] No business logic in `EmergencyAlertStatusController` (validation + mapping only)
- [ ] No PII/secret in plaintext logs; no raw FCM token in DB (`ALERT138-TC-007` green)
- [ ] **CRITICAL**: `ALERT138-TC-002` (payload minimization), `ALERT138-TC-003` (never throw), and `ALERT138-TC-INT-001` all green before merge

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — all tests FAIL against throw-stub before implementation begins
- [ ] **Contract Existence** — `./mvnw compile` clean, no hallucinated imports (no `safety_alerts` table/entity)
- [ ] **Props Isolation** — verified via `SafetyEventTestFactory`, no shared mutable state
- [ ] **Oracle Source** — every assert traces to BR-SAFETY/BR-PRIVACY/BR-RBAC/ADR-SAFETY-010/011/012
- [ ] **No duplicate service** — confirm no `EmergencyAlertService` class created independent of `FamilyAlertService` (AP-AI-001 gate)

### Suspension Criteria

- `V20260705090100` migration blocked by DBA review
- DPO has not signed off on `motherDisplayName`/minimal-content residual (RG-6) — implementation may proceed with the CURRENT (already-compliant) payload shape without `motherDisplayName`; adding that field requires separate DPO review before go-live
- `FamilyMemberPortAdapter.getFamilyFcmTokens()` stub-only status (pre-existing, out of scope) means production dispatch will not occur until the `family`/`care-group` domain team implements it — non-blocking for THIS TDS's own test suite, which mocks the port
- CI pipeline broken by unrelated change

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS family_alert_recipients CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260705090100';"

git checkout -- src/main/java/com/carebridge/backend/emergency/service/impl/FamilyAlertService.java
git checkout -- src/main/java/com/carebridge/backend/emergency/controller/EmergencyAlertStatusController.java
git checkout -- src/main/resources/db/migration/V20260705090100__create_family_alert_recipients.sql
git checkout -- src/test/java/com/carebridge/backend/emergency/

# family_alert_log (existing, pre-UC138) untouched — no rollback needed for that table.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Duplicate Service (project-specific, TDS §17.4) | A new `EmergencyAlertService` class is created independent of `FamilyAlertService` | ☐ | G-0 — **BLOCK**, violates C1/ADR-SAFETY-010 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw-stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Payload Overreach (project-specific, TDS §17.4) | FCM payload includes `magnitude`, `notes`, or health-status text | ☐ | G-3 — **BLOCK**, violates C2/PDPA |
| AP-AI-004 | Blocking Alert Path (project-specific, TDS §17.4) | `sendAlert()` allowed to throw and propagate to `EmergencySessionOpenedHandler` | ☐ | G-4 — **BLOCK**, violates C4/BR-SAFETY |
| AP-AI-005 | Raw Token Storage (project-specific, TDS §17.4) | `family_alert_recipients.fcm_token_hash` receives unhashed token | ☐ | G-3 — **BLOCK**, violates C5 |
| AP-AI-006 | Hallucinated Contract | Test imports non-existent `safety_alerts` entity/table | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
