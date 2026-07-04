# TEST-DRIVEN DEVELOPMENT SPECIFICATION TEMPLATE
# UC141 — Open Emergency Support from Safety Alert — Test Specification

**Document ID:** `FPT-EDU-TDD-UC141-001`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627000003__create_emergency_sessions.sql`, `V20260627000004__create_family_alert_log.sql` — primary CareBridge database schema source (`emergency_sessions`, `family_alert_log`) — **corrected 2026-07-02, see UC141 TDS ADR-SAFETY-013; the V1 `safety_alerts`/`location_snapshots` tables referenced in the original Draft are NOT the live data source**
- `04_Implement/UC141_OpenEmergencySupportFromSafetyAlert/UC141_OpenEmergencySupportFromSafetyAlert_TDS.md` (`CB-SAFETY-IMP-009`) — Technical Specification
- `04_Implement/UC138_SendEmergencyAlert/UC138_SendEmergencyAlert_TDS.md` (`CB-SAFETY-IMP-006`) — real data source (`family_alert_log`/`family_alert_recipients`)
- `04_Implement/UC63_FindNearbyCareFacility/UC63_FindNearbyCareFacility_Test-Spec.md` — reused capability test basis (if exists)
- `04_Implement/UC64_QuickCallOrNavigate/UC64_QuickCallOrNavigate_TDS.md` — reused capability contract
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.4.9` — Functional requirements
- Luật 91/2025 — Legal basis (PDPA)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`/`.dart`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `flutter test` (mobile) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent — Tech Lead` | Khởi tạo tài liệu — TDD spec cho UC141 Open Emergency Support from Safety Alert |
| `2026-07-02` | `AI Agent — Technical Architect (reconciliation pass)` | **Cross-batch schema correction (UC137/138/139/140/141):** replaced all references to the non-existent-consumer V1 table `safety_alerts` (and its `location_snapshot_id`-based coordinate lookup) with `emergency_sessions`/`family_alert_log`/`family_alert_recipients` (UC138's real output — see UC141 TDS ADR-SAFETY-013). Renamed `safetyAlertId`→`emergencySessionId` throughout, `SafetyAlert` entity→`EmergencySession`/`FamilyAlertLog`/`FamilyAlertRecipient` (reused, not newly created), `ISafetyAlertRepository`/`ILocationSnapshotRepository`→`IEmergencySessionRepository`/`IFamilyAlertLogRepository`/`IFamilyAlertRecipientRepository`. Updated Logic Issue L3, `SafetyAlertTestFactory`→now builds `EmergencySession`/`FamilyAlertLog`/`FamilyAlertRecipient` fixtures, and all TC preconditions/test steps/DB assertions referencing the old schema. Status remains Draft. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `GAP-UC141` |
| **Module** | `Open Emergency Support from Safety Alert — Bounded Context: safety (orchestration entry point)` |
| **Spec gốc** | `CB-SAFETY-IMP-009` (`04_Implement/UC141_OpenEmergencySupportFromSafetyAlert/UC141_OpenEmergencySupportFromSafetyAlert_TDS.md`) |
| **Priority** | 🔴 P0 |
| **Sprint** | `S2 (Sprint 2 — theo function-spec-task-allocation.md, TV5-Chương)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `Luật 91/2025 (PDPA)` |
| **Upstream Dependencies** | `emergency_sessions` (existing, UC62), `family_alert_log`/`family_alert_recipients` (UC138 — real "Send Emergency Alert" output, corrected 2026-07-02 — see ADR-SAFETY-013), `UC63 Find Nearby Care Facility`, `UC64 Quick Call or Navigate`, `UC129 Calculate Distance/Route/ETA` |
| **Downstream Consumers** | Mobile `safetyMonitoring` screens (Mother-facing) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-SAFETY-IMP-009 §17`, `ADR-SAFETY-009/010/011/012/013` |
| **Constraints Injected** | C1 (no map service coupling), C2 (hotline 0-dependency), C3 (location fallback no-error), C4 (IDOR guard via emergency_sessions.user_id), C5 (Mobile reuse UC63/UC64, no re-implementation), C6 (data source must be emergency_sessions/family_alert_log/family_alert_recipients, never safety_alerts) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.4.9 liệt kê "Secondary Actors: Firebase Cloud Messaging, TrackAsia Map Service, ZegoCloud Realtime Service" — gợi ý UC141 tự gọi cả 3 service | UC141 backend KHÔNG gọi TrackAsia/ZegoCloud trực tiếp — TrackAsia được UC63/UC129 sở hữu (đọc gián tiếp qua Mobile→UC63), ZegoCloud không áp dụng cho luồng call PSTN (giống lý do UC64 loại trừ, `ADR-MAP-005`). Firebase Cloud Messaging là nguồn phát sinh notification dẫn Mother vào màn hình này (ngoài phạm vi UC141 — thuộc "Send Emergency Alert" 3.3.4.6), KHÔNG phải thứ UC141 tự gọi. | Test PHẢI xác nhận `SafetyAlertEmergencySupportService` KHÔNG import/gọi bất kỳ class nào từ `com.carebridge.backend.map.service`/`.adapter` hay ZegoCloud SDK — chỉ đọc `emergency_sessions`/`family_alert_log`/`family_alert_recipients` qua repository (`UC141-TC-009` compile/dependency check). |
| L2 | TDS `EmergencySupportContextResponse.latitude/longitude` giả định `numeric` (BigDecimal) trong DB nhưng DTO dùng `Double` | `emergency_sessions.user_latitude`/`user_longitude` là kiểu `DECIMAL(10,7)` (PostgreSQL) — JPA map thành `BigDecimal`. Test phải convert đúng và verify không mất độ chính xác quá 6 chữ số thập phân (đủ cho geolocation). | `UC141-TC-INT-001` assert `Double.valueOf(entity.getUserLatitude().doubleValue())` khớp giá trị seed, không dùng so sánh `==` trực tiếp trên float (dùng `assertThat(...).isCloseTo(expected, within(0.000001))`) — moved from the retired `UC141-TC-003` (see §4). |
| L3 | **(Corrected 2026-07-02 — ADR-SAFETY-013)** Bản Draft ban đầu giả định cần tạo entity JPA mới `SafetyAlert` map bảng `safety_alerts` (V1) — nhưng bảng đó KHÔNG có consumer nào trong toàn bộ batch UC136-140 (UC138, tức "Send Emergency Alert" SRS 3.3.4.6, ghi vào `emergency_sessions`/`family_alert_log`/`family_alert_recipients`, KHÔNG PHẢI `safety_alerts`) | UC141 tái sử dụng entity `EmergencySession` (đã tồn tại, owned bởi UC62/`emergency` package) và `FamilyAlertLog`/`FamilyAlertRecipient` (owned bởi UC138, cùng package `emergency`) — KHÔNG tạo entity `SafetyAlert` mới, KHÔNG đọc bảng `safety_alerts`. | `UC141-TC-INT-001` dùng Testcontainers PostgreSQL, seed trực tiếp qua SQL insert vào `emergency_sessions`/`family_alert_log`/`family_alert_recipients` (schema thật) rồi verify entity JPA đọc đúng field-by-field. |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC141 (Open Emergency Support from Safety Alert) bao gồm các layer:
├── Domain (EmergencySession/FamilyAlertLog/FamilyAlertRecipient — existing entities, reused, pure mapping in DTO)
├── Application / Service (SafetyAlertEmergencySupportService — mock JPA Repository với Mockito)
├── Controller (SafetyAlertEmergencySupportController — mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL — full flow GET .../emergency-support)
└── Mobile (Flutter widget tests — SafetyAlertEmergencySupportScreen, mock UC63/UC64 repositories)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-141 §3.3.4.9` | E1 (unauthorized), E2 (invalid data), E3 (external/service failure), AF1 (cancel/back), AF2 (empty/no location) |
| `ADR-SAFETY-009` | Orchestration-only boundary — no `map` service coupling |
| `ADR-SAFETY-010` | Hotline "115" — 0-dependency call path |
| `ADR-SAFETY-011` | Location fallback on `emergency_sessions` missing coordinates |
| `ADR-SAFETY-012` | RBAC + IDOR guard (`emergency_sessions.user_id == userId`) |
| `ADR-SAFETY-013` | Data source correction — `emergency_sessions`/`family_alert_log`/`family_alert_recipients`, never `safety_alerts` |
| Luật 91/2025 (PDPA) | Data minimization — không trả `payload_json`/raw FCM token thô; read-only, không mutate `emergency_sessions`/`family_alert_log` |
| `CB-SAFETY-IMP-009` §8, §9, §10, §16 | Interface contract / API schema / error codes / auth matrix |
| `CB-MAP-IMP-001`/`CB-MAP-IMP-002` (UC63/UC64) | Reuse contract — Mobile tests verify delegation, KHÔNG re-test UC63/64 internal logic (đã có Test-Spec riêng) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Backend service KHÔNG gọi `INearbyFacilityService`/`IMapProviderService` | `SafetyAlertEmergencySupportService` (dependency/compile check) | `UC141-TC-009` |
| TC-COND-002 | Mobile "Tìm cơ sở y tế" delegate đúng sang `NearbyFacilityRepository` (UC63) | `SafetyAlertEmergencySupportScreen.onTapFindFacility()` | `UC141-TC-WIDGET-001` |
| TC-COND-003 | Mobile "Gọi ngay"/"Chỉ đường" trên facility delegate đúng sang `QuickActionService` (UC64) | `SafetyAlertEmergencySupportScreen.onTapCallFacility()/onTapNavigate()` | `UC141-TC-WIDGET-002, 003` |
| TC-COND-004 | "Gọi Cấp cứu 115" mở dialer KHÔNG chờ network | `SafetyAlertEmergencySupportScreen.onTapCallHotline()` | `UC141-TC-WIDGET-004` |
| TC-COND-005 | Hotline fallback local `"115"` khi API context lỗi (SAFETY-106) | Mobile screen error handling | `UC141-TC-WIDGET-005` |
| TC-COND-006 | `emergency_sessions.user_latitude`/`user_longitude` NULL → `locationAvailable=false`, HTTP 200 | `SafetyAlertEmergencySupportService.getEmergencySupportContext()` | `UC141-TC-002` |
| TC-COND-007 | *(retired — no longer applicable; toạ độ đọc trực tiếp từ `emergency_sessions`, không có khái niệm "expired" nữa — xem ADR-SAFETY-013)* | — | — |
| TC-COND-008 | `emergency_sessions.user_id == userId` → 200 OK trả context | `SafetyAlertEmergencySupportService.getEmergencySupportContext()` | `UC141-TC-001` |
| TC-COND-009 | `emergency_sessions.user_id != userId` → 403 SAFETY-104 (IDOR guard) | `SafetyAlertEmergencySupportService.getEmergencySupportContext()` | `UC141-TC-004`, `UC141-TC-SEC-001` |
| TC-COND-010 | Không có JWT / role khác ROLE_MOTHER → 401/403 | `SafetyAlertEmergencySupportController` | `UC141-TC-005, 006` |
| TC-COND-011 | `emergencySessionId` không tồn tại → 404 SAFETY-105; UUID không hợp lệ → 400 SAFETY-103 | `SafetyAlertEmergencySupportController` | `UC141-TC-007, 008` |
| TC-COND-012 | DB lỗi (SAFETY-106) → Mobile vẫn hiển thị nút hotline khả dụng | Mobile error state handling | `UC141-TC-WIDGET-006` |
| TC-COND-013 | Full integration flow: seed `emergency_sessions` + `family_alert_log`/`family_alert_recipients` → GET trả đúng context | Testcontainers full stack | `UC141-TC-INT-001, 002` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `locationAvailable` (true/false), role (MOTHER own / MOTHER other / FAMILY / unauthenticated) | Phân hoạch input domain cho auth + location state |
| Boundary Value Analysis | *(retired for §TTL — no longer applicable, see TC-COND-007)*; retained for `alertDeliveryStatus` (present/absent `family_alert_log` row) | Xác định đúng nullable-vs-present transitions |
| State Transition Testing | N/A — UC141 không có state machine riêng (§6.4 TDS) | Bỏ qua, ghi nhận lý do |
| Error Guessing | IDOR (đổi `emergencySessionId` sang session của user khác), SQL injection qua `emergencySessionId` path param, network-down khi tap hotline | Security/attack vector cho module safety-critical |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `emergency_sessions { id: '11111111-...', user_id: MOTHER_UUID, trigger_source: 'FALL_DETECTION', user_latitude: 10.7769, user_longitude: 106.7009 }` | Happy path — location available |
| `FX-002` | DB seed | `emergency_sessions { ..., user_latitude: NULL, user_longitude: NULL }` | AF2 — location unavailable (NULL) |
| `FX-003` | DB seed | `family_alert_log { session_id: FX-001.id, recipient_count: 1 }` + `family_alert_recipients { family_alert_log_id: <that log>, delivery_status: 'SENT' }` | Alert delivery context present |
| `FX-004` | DB seed | `emergency_sessions { ..., user_id: OTHER_MOTHER_UUID }` | IDOR test — emergencySessionId thuộc user khác |
| `FX-005` | JWT | `{ sub: MOTHER_UUID, role: 'ROLE_MOTHER' }` | Auth context — owner |
| `FX-006` | JWT | `{ sub: OTHER_MOTHER_UUID, role: 'ROLE_MOTHER' }` | Auth context — non-owner (IDOR attempt) |
| `FX-007` | JWT | `{ sub: FAMILY_UUID, role: 'ROLE_FAMILY' }` | Auth context — wrong role |
| `FX-008` | env | `carebridge.emergency.hotline-ambulance=115` | Config test cho hotline |
| `FX-009` | Mobile mock | `MockNearbyFacilityRepository`, `MockQuickActionService` | Verify delegation không re-implement |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeEmergencySession()
// (Corrected 2026-07-02 — ADR-SAFETY-013: factory now builds EmergencySession/
//  FamilyAlertLog/FamilyAlertRecipient fixtures, not the non-existent-consumer SafetyAlert/LocationSnapshot.)
// ═══════════════════════════════════════════════════════════

// SafetyAlertTestFactory.java (class name kept for continuity with other UC141 references — builds emergency_sessions-family fixtures)
class SafetyAlertTestFactory {

    static final UUID MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID DEFAULT_EMERGENCY_SESSION_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
    static final UUID DEFAULT_FAMILY_ALERT_LOG_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    // Giá trị baseline hợp lệ — đồng bộ với FX-001 (§3 TDS-05)
    static EmergencySession makeEmergencySession() {
        EmergencySession session = new EmergencySession();
        session.setId(DEFAULT_EMERGENCY_SESSION_ID);
        session.setUserId(MOTHER_USER_ID);
        session.setStatus("ACTIVE");
        session.setTriggerSource("FALL_DETECTION");
        session.setUserLatitude(new BigDecimal("10.776900"));
        session.setUserLongitude(new BigDecimal("106.700900"));
        session.setCreatedAt(Instant.now());
        return session;
    }

    // Overload để override specific fields
    static EmergencySession makeEmergencySession(Consumer<EmergencySession> overrides) {
        EmergencySession session = makeEmergencySession();
        overrides.accept(session);
        return session;
    }

    static FamilyAlertLog makeFamilyAlertLog(UUID sessionId) {
        return makeFamilyAlertLog(sessionId, l -> {});
    }

    static FamilyAlertLog makeFamilyAlertLog(UUID sessionId, Consumer<FamilyAlertLog> overrides) {
        FamilyAlertLog log = new FamilyAlertLog();
        log.setId(DEFAULT_FAMILY_ALERT_LOG_ID);
        log.setSessionId(sessionId);
        log.setSentAt(Instant.now());
        log.setRecipientCount(1);
        log.setLocationIncluded(true);
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
                .sentAt(Instant.now())
                .createdBy("SYSTEM")
                .build();
        overrides.accept(recipient);
        return recipient;
    }
}
```

---

### UC141-TC-001 — GET emergency-support trả 200 với location available (Happy Path)

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyAlertEmergencySupportService.getEmergencySupportContext()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyAlertEmergencySupportServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-SAFETY-012 §Decision`, `CB-SAFETY-IMP-009 §9.2` response schema

**Preconditions:**
- `IEmergencySessionRepository.findById(DEFAULT_EMERGENCY_SESSION_ID)` mock trả `SafetyAlertTestFactory.makeEmergencySession()` (FX-001)
- `IFamilyAlertLogRepository.findBySessionId(DEFAULT_EMERGENCY_SESSION_ID)` mock trả `Optional.empty()` (no alert log yet — optional context, not required for happy path)

**Test Steps:**
1. Arrange: mock repository như trên
2. Act: gọi `service.getEmergencySupportContext(DEFAULT_EMERGENCY_SESSION_ID, MOTHER_USER_ID)`
3. Assert: response fields

**Expected Result (PASS — hành vi đúng):**
- `response.getLocationAvailable() == true`
- `response.getLatitude()` gần bằng `10.7769` (within 0.000001)
- `response.getHotlineNumber().equals("115")`
- `response.getEmergencySessionId().equals(DEFAULT_EMERGENCY_SESSION_ID)`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Ném exception không mong muốn, hoặc `locationAvailable=false` dù `emergency_sessions` có toạ độ hợp lệ

**Current Status:** 🔴 Not written
**Implementation Note:** Không được gọi `INearbyFacilityService` — verify qua Mockito `verifyNoInteractions()` nếu bean đó bị inject nhầm.

---

### UC141-TC-002 — `emergency_sessions.user_latitude`/`user_longitude` NULL → locationAvailable=false (AF2)

**Severity:** `HIGH`
**Feature Under Test:** `SafetyAlertEmergencySupportService.getEmergencySupportContext()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyAlertEmergencySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-SAFETY-011 §Decision` (FX-002)

**Preconditions:**
- `SafetyAlertTestFactory.makeEmergencySession(s -> { s.setUserLatitude(null); s.setUserLongitude(null); })`

**Test Steps:**
1. Arrange: seed session với `userLatitude`/`userLongitude = null`
2. Act: gọi `getEmergencySupportContext()`
3. Assert: response

**Expected Result (PASS):**
- HTTP semantics: service trả object bình thường (không throw)
- `response.getLocationAvailable() == false`
- `response.getLatitude() == null`, `response.getLongitude() == null`
- `response.getHotlineNumber().equals("115")` (vẫn có — độc lập với location)

**Expected Result (FAIL):**
- Service throw `NullPointerException` hoặc lỗi 500 khi toạ độ null

**Current Status:** 🔴 Not written

---

### UC141-TC-003 — *(RETIRED — 2026-07-02, ADR-SAFETY-013)*

> Test case này đã bị loại bỏ vì tiền đề của nó (`location_snapshots.expires_at`) không còn áp dụng — UC141 không còn đọc `location_snapshots` cho toạ độ cơ bản; toạ độ đến từ `emergency_sessions.user_latitude`/`user_longitude` (không có khái niệm TTL/expiry). Xem UC141-TC-002 (toạ độ NULL) là test case thay thế duy nhất còn cần cho nhánh "thiếu toạ độ". `UC141-TC-INT-001`'s numeric-precision assertion (originally under `TC-003` per Logic Issue L2) is preserved there instead.

---

### UC141-TC-004 — `emergency_sessions.user_id != userId` → SafetyException SAFETY-104 (IDOR Guard)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `SafetyAlertEmergencySupportService.getEmergencySupportContext()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyAlertEmergencySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-SAFETY-012 §Decision`, `CB-SAFETY-IMP-009 §10 SAFETY-104`

**Preconditions:**
- `SafetyAlertTestFactory.makeEmergencySession()` có `userId = MOTHER_USER_ID` (FX-001)
- Gọi service với `userId = OTHER_MOTHER_USER_ID` (FX-006)

**Test Steps:**
1. Arrange: seed session thuộc `MOTHER_USER_ID`
2. Act: gọi `service.getEmergencySupportContext(DEFAULT_EMERGENCY_SESSION_ID, OTHER_MOTHER_USER_ID)`
3. Assert: exception ném ra đúng loại + mã lỗi

**Expected Result (PASS — hệ thống an toàn):**
- Ném `SafetyException` với code `SAFETY-104`
- `IFamilyAlertLogRepository`/`IFamilyAlertRecipientRepository` KHÔNG được gọi (fail-fast trước khi lộ thêm dữ liệu)

**Expected Result (FAIL — lỗ hổng tồn tại):**
- Service trả context của session thuộc user khác (IDOR) — data leak vị trí Mother khác

**Current Status:** 🔴 Not written
**Implementation Note:** Check ownership TRƯỚC khi query `family_alert_log`/`family_alert_recipients` — không query thừa dữ liệu nhạy cảm nếu đã biết sẽ reject.

---

### UC141-TC-005 — Không có JWT → 401 IAM-001

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyAlertEmergencySupportController` (`@WebMvcTest`)
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyAlertEmergencySupportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `SRS UC-141 §Exceptions E1`

**Test Steps:**
1. `mockMvc.perform(get("/api/v1/safety/emergency-sessions/{id}/emergency-support", DEFAULT_EMERGENCY_SESSION_ID))` (không set `Authorization` header)
2. Assert status + body

**Expected Result (PASS):** `401 Unauthorized`, body chứa `error.code == "IAM-001"`
**Expected Result (FAIL):** Endpoint trả 200 hoặc lỗi khác

**Current Status:** 🔴 Not written

---

### UC141-TC-006 — JWT hợp lệ nhưng role != ROLE_MOTHER → 403 SAFETY-104

**Severity:** `HIGH`
**Feature Under Test:** `SafetyAlertEmergencySupportController`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyAlertEmergencySupportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-SAFETY-IMP-009 §16 Authorization Matrix`

**Preconditions:** JWT với `role: ROLE_FAMILY` (FX-007)

**Test Steps:**
1. `mockMvc.perform(get(...).header("Authorization", "Bearer " + familyJwt))`
2. Assert

**Expected Result (PASS):** `403 Forbidden`, `error.code == "SAFETY-104"`
**Expected Result (FAIL):** Endpoint cho phép ROLE_FAMILY truy cập

**Current Status:** 🔴 Not written

---

### UC141-TC-007 — `emergencySessionId` không tồn tại → 404 SAFETY-105

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyAlertEmergencySupportService.getEmergencySupportContext()`
**Test File:** `src/test/java/com/carebridge/backend/safety/service/SafetyAlertEmergencySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-SAFETY-IMP-009 §10 SAFETY-105`

**Preconditions:** `IEmergencySessionRepository.findById(anyUUID)` mock trả `Optional.empty()`

**Test Steps:**
1. Act: gọi `getEmergencySupportContext(randomNonExistentId, MOTHER_USER_ID)`
2. Assert

**Expected Result (PASS):** `SafetyException` code `SAFETY-105`
**Expected Result (FAIL):** `NoSuchElementException` không được bắt, lộ 500 error thay vì 404 rõ ràng

**Current Status:** 🔴 Not written

---

### UC141-TC-008 — `emergencySessionId` không phải UUID hợp lệ → 400 SAFETY-103

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyAlertEmergencySupportController`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyAlertEmergencySupportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-SAFETY-IMP-009 §10 SAFETY-103`

**Test Steps:**
1. `mockMvc.perform(get("/api/v1/safety/emergency-sessions/not-a-uuid/emergency-support").header("Authorization", "Bearer " + motherJwt))`
2. Assert

**Expected Result (PASS):** `400 Bad Request`, `error.code == "SAFETY-103"`
**Expected Result (FAIL):** 500 Internal Server Error do unhandled `MethodArgumentTypeMismatchException`

**Current Status:** 🔴 Not written

---

### UC141-TC-009 — Service KHÔNG phụ thuộc `map` bounded context business logic (Architecture Guard)

**Severity:** `HIGH`
**Feature Under Test:** `SafetyAlertEmergencySupportService` (class dependency inspection)
**Test File:** `src/test/java/com/carebridge/backend/safety/architecture/UC141BoundaryTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-SAFETY-009 §Decision`, Logic Issue L1 (§2)

**Test Steps:**
1. Dùng ArchUnit (nếu đã có dependency trong `pom.xml`) hoặc reflection-based check: liệt kê tất cả field types của `SafetyAlertEmergencySupportService`
2. Assert không có field nào có type thuộc package `com.carebridge.backend.map.service.*` hay `com.carebridge.backend.map.adapter.*`

**Expected Result (PASS):**
- `SafetyAlertEmergencySupportService` chỉ inject `IEmergencySessionRepository`/`IFamilyAlertLogRepository`/`IFamilyAlertRecipientRepository` (bounded context `emergency`, KHÔNG `map`) — không inject `INearbyFacilityService`/`IMapProviderService`/`TrackAsiaMapClient`

**Expected Result (FAIL):**
- Phát hiện field kiểu `INearbyFacilityService` hoặc `IMapProviderService` → vi phạm ADR-SAFETY-009

**Current Status:** 🔴 Not written
**Implementation Note:** Nếu chưa có ArchUnit trong `pom.xml`, dùng test đơn giản qua `Class.getDeclaredFields()` + kiểm tra `getType().getPackageName()`. KHÔNG thêm dependency mới không được duyệt (CLAUDE.md) — ưu tiên reflection thay vì thêm ArchUnit nếu chưa có sẵn.

---

### SECURITY TEST CASES

---

### UC141-TC-SEC-001 — IDOR: đổi `emergencySessionId` trong URL để xem session của user khác

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `Luật 91/2025 — lộ vị trí/PII của Mother khác`
**Feature Under Test:** `GET /api/v1/safety/emergency-sessions/{emergencySessionId}/emergency-support`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyAlertEmergencySupportControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Seed `emergency_sessions` FX-001 (`user_id = MOTHER_USER_ID`)
- JWT hợp lệ nhưng `sub = OTHER_MOTHER_USER_ID` (FX-006)

**Test Steps (Attack Simulation):**
1. Đăng nhập với tài khoản `OTHER_MOTHER_USER_ID`
2. Gọi `GET /api/v1/safety/emergency-sessions/{emergencySessionId của MOTHER_USER_ID}/emergency-support`
3. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden`, `error.code == "SAFETY-104"`
- Response body KHÔNG chứa `latitude`/`longitude`/`alertReason` của `MOTHER_USER_ID`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Trả `200 OK` với dữ liệu vị trí của `MOTHER_USER_ID` — data leak nghiêm trọng

**Current Status:** 🔴 Not written

---

### UC141-TC-SEC-002 — SQL Injection attempt qua `emergencySessionId` path param

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `GET /api/v1/safety/emergency-sessions/{emergencySessionId}/emergency-support`
**Test File:** `src/test/java/com/carebridge/backend/safety/controller/SafetyAlertEmergencySupportControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. `GET /api/v1/safety/emergency-sessions/' OR '1'='1/emergency-support`
2. Kiểm tra response + DB state

**Expected Result (PASS = hệ thống an toàn):**
- `400 Bad Request SAFETY-103` (UUID parse fail trước khi chạm DB — Spring `@PathVariable UUID` tự reject)
- Không có query bất thường trong `pg_stat_activity`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Query thực thi với input độc hại (không áp dụng thực tế nhờ JPA parameterized query + UUID type-safety, nhưng vẫn test để xác nhận)

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### UC141-TC-INT-001 — Full flow: seed DB → GET trả context đúng (location available)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request → Controller → Service → Repository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyAlertEmergencySupportIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:**
- PostgreSQL Testcontainer chạy (`@Testcontainers` auto-start)
- Flyway migration áp dụng tự động (bao gồm `V20260627000003__create_emergency_sessions.sql`, `V20260627000004__create_family_alert_log.sql`, và UC138's Draft `V20260705090100__create_family_alert_recipients.sql`)
- Seed trực tiếp qua SQL: `INSERT INTO emergency_sessions (...) VALUES (...)`, `INSERT INTO family_alert_log (...) VALUES (...)`, `INSERT INTO family_alert_recipients (...) VALUES (...)` (FX-001, FX-003 hợp lệ)

**Test Steps:**
1. Seed `users` (Mother test account), `emergency_sessions`, `family_alert_log`, `family_alert_recipients` qua raw SQL/JdbcTemplate
2. `GET /api/v1/safety/emergency-sessions/{seededId}/emergency-support` với JWT Mother hợp lệ
3. Assert response

**Expected Result (PASS):**
- `200 OK`
- `locationAvailable: true`, `latitude`/`longitude` khớp giá trị đã seed (within tolerance — see Logic Issue L2, use `assertThat(...).isCloseTo(expected, within(0.000001))`)
- `hotlineNumber: "115"`
- `alertDeliveryStatus: "SENT"` (from seeded `family_alert_recipients`)

**Expected Result (FAIL):**
- 500 error do entity mapping sai cột (xem Logic Issue L3, §2) — ví dụ JPA cố map vào bảng V1 `safety_events`/`safety_alerts` thay vì `emergency_sessions`

**DB Assertion:**
```java
EmergencySession session = emergencySessionRepository.findById(seededId).orElseThrow();
assertThat(session.getUserId()).isEqualTo(MOTHER_USER_ID);
assertThat(session.getTriggerSource()).isEqualTo("FALL_DETECTION");
// Verify read-only: resolved_at không đổi sau khi GET
assertThat(session.getResolvedAt()).isEqualTo(originalResolvedAt);
```

**Current Status:** 🔴 Not written

---

### UC141-TC-INT-002 — Full flow: `emergency_sessions` thiếu toạ độ → locationAvailable false, vẫn 200

**Severity:** `MEDIUM`
**Feature Under Test:** `Full flow — AF2 path`
**Test File:** `src/test/java/com/carebridge/backend/safety/SafetyAlertEmergencySupportIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006, 013`

**Preconditions:** Seed `emergency_sessions` (FX-002) với `user_latitude`/`user_longitude = NULL`

**Test Steps:**
1. Seed data
2. `GET .../emergency-support`
3. Assert

**Expected Result (PASS):**
- `200 OK`, `locationAvailable: false`, `latitude: null`, `longitude: null`, `hotlineNumber: "115"` vẫn có mặt

**Expected Result (FAIL):**
- 500 error hoặc `locationAvailable` sai giá trị

**Current Status:** 🔴 Not written

---

### MOBILE WIDGET TEST CASES (Flutter)

---

### UC141-TC-WIDGET-001 — Tap "Tìm cơ sở y tế gần nhất" gọi đúng `NearbyFacilityRepository` (UC63), KHÔNG tự implement search

**Severity:** `HIGH`
**Feature Under Test:** `SafetyAlertEmergencySupportScreen.onTapFindFacility()`
**Test File:** `test/features/safetyMonitoring/screens/safety_alert_emergency_support_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-SAFETY-009 §Decision`, RG-3 delegation table (TDS §1)

**Preconditions:**
- `MockNearbyFacilityRepository` (FX-009) injected qua Provider/DI trong widget test harness
- Screen đã load context (`locationAvailable: true`, lat/lng cố định)

**Test Steps:**
1. `pumpWidget(SafetyAlertEmergencySupportScreen(...))` với mock context + mock repository
2. `tester.tap(find.text('Tìm cơ sở y tế gần nhất'))`
3. `await tester.pumpAndSettle()`

**Expected Result (PASS):**
- `verify(mockNearbyFacilityRepository.findNearby(latitude: 10.7769, longitude: 106.7009)).called(1)`
- Không có code local nào tính bounding-box/Haversine trong widget (grep source xác nhận không import `dart:math` cho mục đích search riêng)

**Expected Result (FAIL):**
- Widget tự implement logic tìm kiếm riêng (duplicate UC63) hoặc không gọi repository nào cả

**Current Status:** 🔴 Not written

---

### UC141-TC-WIDGET-002 — Tap "Gọi ngay" trên facility card gọi đúng `QuickActionService.call()` (UC64)

**Severity:** `HIGH`
**Feature Under Test:** `SafetyAlertEmergencySupportScreen.onTapCallFacility()`
**Test File:** `test/features/safetyMonitoring/screens/safety_alert_emergency_support_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-SAFETY-009`, `CB-MAP-IMP-002 §8.3`

**Preconditions:** `MockQuickActionService` injected; 1 facility item hiển thị với `phone: "0281234567"`

**Test Steps:**
1. Pump widget với facility list đã có 1 item
2. `tester.tap(find.text('Gọi ngay').first)`
3. `await tester.pump()`

**Expected Result (PASS):**
- `verify(mockQuickActionService.call('0281234567')).called(1)`
- KHÔNG gọi `tel:` trực tiếp trong widget code của UC141 cho facility (chỉ hotline 115 mới gọi trực tiếp — xem `UC141-TC-WIDGET-004`)

**Expected Result (FAIL):**
- Widget tự parse `Uri.parse('tel:...')` cho facility thay vì gọi qua `QuickActionService` — vi phạm reuse constraint C5

**Current Status:** 🔴 Not written

---

### UC141-TC-WIDGET-003 — Tap "Chỉ đường" gọi đúng `QuickActionService.navigate()` (UC64)

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyAlertEmergencySupportScreen.onTapNavigate()`
**Test File:** `test/features/safetyMonitoring/screens/safety_alert_emergency_support_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`

**Test Steps:**
1. Pump widget với facility có `latitude: 10.78, longitude: 106.69`
2. `tester.tap(find.text('Chỉ đường').first)`

**Expected Result (PASS):**
- `verify(mockQuickActionService.navigate(latitude: 10.78, longitude: 106.69, label: any)).called(1)`

**Expected Result (FAIL):**
- Widget mở URL trực tiếp thay vì qua `QuickActionService`

**Current Status:** 🔴 Not written

---

### UC141-TC-WIDGET-004 — Tap "Gọi Cấp cứu 115" mở dialer NGAY LẬP TỨC, không `await` network trước

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyAlertEmergencySupportScreen.onTapCallHotline()`
**Test File:** `test/features/safetyMonitoring/screens/safety_alert_emergency_support_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-SAFETY-010 §Decision`, C2 (§17.1 TDS)

**Preconditions:**
- Mock `url_launcher` platform channel (`launchUrl`) để capture lời gọi
- Context đã load với `hotlineNumber: "115"`

**Test Steps:**
1. Pump widget
2. `tester.tap(find.text('Gọi Cấp cứu 115'))`
3. `await tester.pump(Duration.zero)` (KHÔNG `pumpAndSettle` chờ network — verify hành động xảy ra ngay khung hình kế tiếp)

**Expected Result (PASS):**
- `launchUrl` được gọi với `Uri.parse('tel:115')` ngay trong frame đầu tiên sau tap — không có `FutureBuilder`/`await` network call chặn trước đó
- Test đo thời gian giữa tap và launchUrl call < 1 frame (không có `Future.delayed` hay HTTP call ở giữa)

**Expected Result (FAIL — vi phạm an toàn):**
- Có `await apiService.refreshContext()` hoặc tương tự TRƯỚC `launchUrl` — vi phạm BR-SAFETY tuyệt đối (AP-AI-006)

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test CRITICAL nhất trong toàn bộ UC141 — nút gọi cấp cứu phải là hành động đồng bộ/tức thời.

---

### UC141-TC-WIDGET-005 — Backend context API lỗi (SAFETY-106) → nút hotline vẫn dùng fallback local "115"

**Severity:** `CRITICAL`
**Feature Under Test:** `SafetyAlertEmergencySupportScreen` error state
**Test File:** `test/features/safetyMonitoring/screens/safety_alert_emergency_support_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005, 012`
**Oracle Source:** `ADR-SAFETY-011 §Decision`, C2

**Preconditions:** `MockSafetyAlertEmergencySupportRepository.getContext()` throws `SocketException`/timeout

**Test Steps:**
1. Pump widget, mock repository ném lỗi khi load context
2. `await tester.pumpAndSettle()` — verify error state UI hiển thị nhưng nút hotline vẫn active
3. `tester.tap(find.text('Gọi Cấp cứu 115'))`

**Expected Result (PASS):**
- Nút "Gọi Cấp cứu 115" vẫn hiển thị và **enabled** dù context API lỗi hoàn toàn
- `launchUrl` gọi với `Uri.parse('tel:115')` (local fallback constant, không phải giá trị từ API vì API đã lỗi)
- "Tìm cơ sở y tế" có thể disabled/hiện thông báo lỗi riêng — nhưng KHÔNG được làm mất nút hotline

**Expected Result (FAIL):**
- Toàn màn hình hiện error/loading spinner chặn hết, không cho tap nút hotline — vi phạm BR-SAFETY

**Current Status:** 🔴 Not written

---

### UC141-TC-WIDGET-006 — Location unavailable → "Tìm cơ sở y tế" fallback dùng GPS thiết bị thay vì snapshot

**Severity:** `MEDIUM`
**Feature Under Test:** `SafetyAlertEmergencySupportScreen.onTapFindFacility()` khi `locationAvailable=false`
**Test File:** `test/features/safetyMonitoring/screens/safety_alert_emergency_support_screen_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-SAFETY-011 §Decision`

**Preconditions:** Context trả `locationAvailable: false, latitude: null, longitude: null`; mock `Geolocator.getCurrentPosition()` trả toạ độ giả `(10.80, 106.65)`

**Test Steps:**
1. Pump widget với context `locationAvailable: false`
2. `tester.tap(find.text('Tìm cơ sở y tế gần nhất'))`
3. `await tester.pumpAndSettle()`

**Expected Result (PASS):**
- `verify(mockNearbyFacilityRepository.findNearby(latitude: 10.80, longitude: 106.65)).called(1)` — dùng GPS hiện tại, không dùng `null`

**Expected Result (FAIL):**
- Gọi `findNearby(latitude: null, longitude: null)` gây lỗi 400 ở UC63, hoặc nút bị disable hoàn toàn (chặn tính năng không cần thiết)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `UC141-TC-001` | `SafetyAlertEmergencySupportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-002` | `SafetyAlertEmergencySupportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-003` | *(retired 2026-07-02 — ADR-SAFETY-013; numeric-precision assertion moved to `UC141-TC-INT-001`)* | — | — | — |
| `UC141-TC-004` | `SafetyAlertEmergencySupportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-005` | `SafetyAlertEmergencySupportControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-006` | `SafetyAlertEmergencySupportControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-007` | `SafetyAlertEmergencySupportServiceTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-008` | `SafetyAlertEmergencySupportControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-009` | `UC141BoundaryTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-SEC-001` | `SafetyAlertEmergencySupportControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-SEC-002` | `SafetyAlertEmergencySupportControllerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-INT-001` | `SafetyAlertEmergencySupportIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-INT-002` | `SafetyAlertEmergencySupportIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-WIDGET-001` | `safety_alert_emergency_support_screen_test.dart:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-WIDGET-002` | `safety_alert_emergency_support_screen_test.dart:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-WIDGET-003` | `safety_alert_emergency_support_screen_test.dart:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-WIDGET-004` | `safety_alert_emergency_support_screen_test.dart:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-WIDGET-005` | `safety_alert_emergency_support_screen_test.dart:TBD` | `[ ]` | `[ ]` | |
| `UC141-TC-WIDGET-006` | `safety_alert_emergency_support_screen_test.dart:TBD` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class SafetyAlertEmergencySupportService implements ISafetyAlertEmergencySupportService {

    @Override
    public EmergencySupportContextResponse getEmergencySupportContext(UUID emergencySessionId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```dart
// Red Phase — Mobile stub (PHẢI throw)
class SafetyAlertEmergencySupportScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    throw UnimplementedError('Not implemented — Red Phase stub');
  }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `UC141-TC-001` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `UC141-TC-002` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC141-TC-003` | *(retired 2026-07-02 — ADR-SAFETY-013, see §4)* | — | — | — |
| `UC141-TC-004` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC141-TC-005` | Controller stub returns 501/exception | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC141-TC-006` | Controller stub | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC141-TC-007` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC141-TC-008` | Controller stub | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `UC141-TC-009` | N/A — architecture test runs against stub class, should still detect 0 imports (trivially PASS acceptable if stub has no map imports; re-verify after real impl) | ⚠️ Special case | ☐ Checked | Architecture test is an invariant check, not a behavior test — document exception in PIR if it passes at stub stage |
| `UC141-TC-WIDGET-001..006` | `throw UnimplementedError` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS bất thường (ngoại trừ UC141-TC-009 đã ghi chú đặc biệt):** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-SAFETY-IMP-009` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect (đặc biệt L3 — tái sử dụng entity `EmergencySession`/`FamilyAlertLog`/`FamilyAlertRecipient`, KHÔNG tạo entity `SafetyAlert`/đọc bảng V1 `safety_alerts`/`safety_events`)
- [ ] Không cần Flyway migration mới (§5.2 TDS) — N/A cho bước này
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị
- [ ] UC63 (`CB-MAP-IMP-001`)/UC64 (`CB-MAP-IMP-002`) Mobile code (hoặc ít nhất interface `NearbyFacilityRepository`/`QuickActionService`) đã tồn tại để mock trong widget tests

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] `flutter test` — tất cả widget tests xanh
- [ ] Test coverage ≥ 80% lines cho `SafetyAlertEmergencySupportService`
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] `UC141-TC-009` (architecture guard) xanh — xác nhận không có coupling sang `com.carebridge.backend.map.service`/`.adapter`
- [ ] `UC141-TC-WIDGET-004` (hotline instant call) xanh — xác nhận 0 network dependency

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement (trừ ngoại lệ đã ghi chú ở `UC141-TC-009`)
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (dùng `SafetyAlertTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR-SAFETY-009/010/011/012)

### Suspension Criteria (Điều kiện tạm dừng)

- UC63/UC64 Mobile repository/service interface chưa tồn tại — widget tests (`UC141-TC-WIDGET-*`) phải chờ hoặc dùng interface giả định tạm thời (ghi rõ trong test comment)
- Phát hiện lỗi kiến trúc mới (vd: cần Redis/infra mới) cần Principal Architect review
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Không có migration để revert (UC141 không tạo bảng mới)

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/safety/controller/SafetyAlertEmergencySupportController.java
git checkout -- src/main/java/com/carebridge/backend/safety/service/
# Note (Corrected 2026-07-02, ADR-SAFETY-013): UC141 no longer creates ISafetyAlertRepository/SafetyAlert entity —
# it reuses existing IEmergencySessionRepository (emergency package) and UC138's IFamilyAlertLogRepository/
# IFamilyAlertRecipientRepository, none of which are owned/created by UC141 itself, so nothing to revert there.
git checkout -- src/test/java/com/carebridge/backend/safety/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/safetyMonitoring/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md (nếu áp dụng)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Đã review — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ Red Gate thực thi | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Đã review — mọi decision trace về ADR-SAFETY-009/010/011/012 | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ `UC141-TC-005/006/008` chỉ test HTTP semantics, business logic ở `UC141-TC-001..004` (Service layer) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Đã review §8 TDS — mọi type reference khớp Interface Specification | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn thiết kế spec → TDD spec approved (pending Red Gate execution)
- [ ] Phát hiện AP trong quá trình thực thi Red Gate → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(để trống — điền khi Red Gate chạy thực tế)_ | | | | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Status: Draft — chưa Approved. Chờ review TDS `CB-SAFETY-IMP-009` trước.*
