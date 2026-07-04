# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC151 — Contact Nearby User

**Document ID:** `FPT-EDU-TDD-UC151-001`
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
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.5.6` — UC-151 Functional requirements
- `04_Implement/UC151_ContactNearbyUser/UC151_ContactNearbyUser_TDS.md` (`CB-MAP-IMP-004`) — Technical Specification
- `04_Implement/UC150_ViewNearbySupportRequests/UC150_ViewNearbySupportRequests_TDS.md` (`CB-MAP-IMP-003`) — upstream gating mechanism
- `04_Implement/UC64_QuickCallOrNavigate/UC64_QuickCallOrNavigate_TDS.md` (`CB-MAP-IMP-002`) — native dialer pattern reference
- `Luật 91/2025` — PDPA legal basis

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent — Tech Lead` | Khởi tạo tài liệu — TDD spec cho UC151 Contact Nearby User |
| `2026-07-02` | `AI Agent — Tech Lead` | **Đóng Open Item (gating/accept mechanism):** Product Owner đã CONFIRMED cơ chế "accept" (`selected_expert_id`/`status`) — xem TDS §1/§2 CHANGELOG (ADR-MAP-206 Accepted). L1 (§2) cập nhật trạng thái; Suspension Criteria liên quan đã gỡ bỏ. |

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
| **Feature / Gap ID** | `GAP-UC151` |
| **Module** | `Contact Nearby User — Bounded Context: map` |
| **Spec gốc** | `CB-MAP-IMP-004` |
| **Priority** | 🟠 P1 |
| **Sprint** | `Sprint 3` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `Luật 91/2025 (PDPA) — minimum-necessary/gating` |
| **Upstream Dependencies** | `emergency_events`, `location_snapshots`, `users`, `expert_profiles`, `notifications` (tất cả bảng có sẵn); `IEmergencyEventRepository`/`ILocationSnapshotRepository` (tái sử dụng từ UC150) |
| **Downstream Consumers** | `UC152 Navigate to Support Location` (dùng cùng gating check) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MAP-IMP-004 §17`, `ADR-MAP-206`, `ADR-MAP-207`, `ADR-MAP-208`, `ADR-MAP-209` |
| **Constraints Injected** | Optimistic conditional UPDATE (C1); PII gating theo `selected_expert_id` (C2); native `tel:` dialer, không ZegoCloud (C3); domain event transactional bắt buộc (C4); userId từ JWT (C5); 403 không tiết lộ danh tính Expert đã accept (C6) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.5.6 không định nghĩa cơ chế "accept" cụ thể | **[RESOLVED 2026-07-02]** `emergency_events` KHÔNG có bảng/cột `accepted_by`/`support_request_acceptances` — TDS dùng `selected_expert_id IS NULL` làm điều kiện, kế thừa UC150, đã CONFIRMED bởi Product Owner (ADR-MAP-206 Accepted) | Test cases dùng conditional UPDATE trên `selected_expert_id`, KHÔNG giả định bảng mới không tồn tại |
| L2 | SRS liệt kê `BR-CONSULTATION` cho UC151 nhưng nội dung BR-CONSULTATION nói về booking/payment/refund, không khớp trực tiếp | TDS áp dụng tinh thần "auditable lifecycle" thay vì nghĩa đen booking/payment (Open item, xem TDS §2) | Test cases xác minh domain event `RequestAccepted`/`NearbyUserContacted` publish transactional (không assert booking/payment logic không tồn tại) |
| L3 | UC64 dùng ZegoCloud secondary actor conflict tương tự — UC151 KHÔNG liệt kê ZegoCloud trong SRS nhưng cần xác nhận không dùng | UC64 TDS đã kết luận native `tel:` dialer cho tất cả cuộc gọi PSTN ngoài booking `consultation_sessions` | Test cases xác minh Mobile service KHÔNG import/gọi ZegoCloud class nào |
| L4 | `users` table không có cột riêng cho "trạng thái đã accept" — full detail phải JOIN nhiều bảng (`emergency_events` + `users` + `location_snapshots`) | Xác nhận từ `V1__init_schema.sql` dòng 532-544 (`users`), 1081-1095 (`emergency_events`), 1097-1108 (`location_snapshots`) | Integration test seed đủ 3 bảng, assert JOIN đúng |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Contact Nearby User (map bounded context) bao gồm các layer:
├── Domain (pure logic — conditional accept, gating check — no deps)
├── Service (mock JPA Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
├── Integration (Testcontainers PostgreSQL với @SpringBootTest — accept race condition, gating)
└── Mobile (Dart unit test — ContactNearbyUserService, mock url_launcher)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-151 §3.3.5.6` | Accept → contact flow, E1 (access denied), E2 (conflicting data — double accept), E3 (external/server failure) |
| `ADR-MAP-206` | Optimistic conditional UPDATE — race condition safety |
| `ADR-MAP-207` | Contact mechanism — full detail unlock + native dialer + notification |
| `ADR-MAP-208` | RBAC + VERIFIED + accepted-by-self gating |
| `ADR-MAP-209` | Domain event transactional (không best-effort) |
| PDPA / Luật 91/2025 | Minimum-necessary gating, no PII leak to unauthorized Expert |
| `CB-MAP-IMP-004` §9-10 | API contract, error codes |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Accept thành công khi request đang OPEN, chưa ai accept | `NearbySupportContactService.acceptRequest()` | `MAP151-TC-001` |
| TC-COND-002 | Accept thất bại (409) khi request đã bị accept bởi Expert khác | `NearbySupportContactService.acceptRequest()` | `MAP151-TC-002` |
| TC-COND-003 | Race condition: 2 Expert cùng accept đồng thời — chỉ 1 thành công | `IEmergencyEventRepository.conditionalAccept()` | `MAP151-TC-INT-001` |
| TC-COND-004 | `getContactDetail()` trả full PII đúng cho Expert đã accept | `NearbySupportContactService.getContactDetail()` | `MAP151-TC-003` |
| TC-COND-005 | `logContact()` ghi notification cho Mother + publish event | `NearbySupportContactService.logContact()` | `MAP151-TC-004` |
| TC-COND-006 | Mobile: `call()` dùng `tel:` dialer, KHÔNG ZegoCloud; log fire-and-forget SAU khi launch dialer | `ContactNearbyUserService.call()`/`logContactFireAndForget()` | `MAP151-TC-005` |
| TC-COND-007 | `getContactDetail()`/`logContact()` reject (403) Expert chưa VERIFIED | `NearbySupportContactController` | `MAP151-TC-006` |
| TC-COND-008 | `getContactDetail()`/`logContact()` reject (403) Expert VERIFIED nhưng chưa accept request này (CRITICAL — PII leak prevention) | `NearbySupportContactService` | `MAP151-TC-007, MAP151-TC-008` |
| TC-COND-009 | 403 response KHÔNG tiết lộ danh tính Expert đã accept | `NearbySupportContactController` | `MAP151-TC-009` |
| TC-COND-010 | Domain events publish trong cùng transaction — rollback nếu publish lỗi | `NearbySupportContactService` | `MAP151-TC-010` |
| TC-COND-011 | DB unavailable → 503 MAP-210, không duplicate action | `NearbySupportContactController` | `MAP151-TC-011` |
| TC-COND-012 | `emergencyEventId` không tồn tại → 404 MAP-209 | `NearbySupportContactService` | `MAP151-TC-012` |
| TC-COND-013 | `channel` không hợp lệ trong `POST .../contact` → 400 MAP-207 | `NearbySupportContactController` | `MAP151-TC-013` |
| TC-COND-014 | E2E: full flow accept → contact-detail → contact log | Full stack | `MAP151-TC-E2E-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `channel` (CALL/IN_APP_MESSAGE vs invalid) | Phân vùng input hợp lệ/không hợp lệ cho `ContactLogRequest` |
| Boundary Value Analysis | `selected_expert_id` NULL vs non-NULL vs matching vs non-matching | Kiểm tra chính xác biên gating logic (CRITICAL cho PDPA) |
| State Transition Testing | `emergency_events.status`/`selected_expert_id` (OPEN→ACCEPTED-by-X) | Xác minh transition chỉ xảy ra 1 lần, đúng invariant UC150 §6.3 |
| Concurrency/Race Testing | `conditionalAccept()` với 2 thread đồng thời | Đảm bảo atomic UPDATE hoạt động đúng dưới tải song song |
| Error Guessing | PII leak vectors — Expert B cố truy cập request đã accept bởi Expert A | Security test cho gating boundary |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `emergency_events { status:'OPEN', selected_expert_id: NULL, user_id: FX-MOTHER-001 }` | Happy path accept |
| `FX-002` | DB seed | `emergency_events { status:'OPEN', selected_expert_id: FX-EXPERT-A }` | Test double-accept reject |
| `FX-003` | DB seed | `users { user_id: FX-MOTHER-001, full_name:'Nguyễn Thị Test', phone:'+84900000001', email:'mother-test@example.com' }` | Contact detail response |
| `FX-004` | DB seed | `location_snapshots { context_type:'EMERGENCY_EVENT', context_id: FX-001.emergency_event_id, latitude: 10.776889, longitude: 106.700912 }` | Exact coordinates |
| `FX-005` | DB seed | `expert_profiles { user_id: FX-EXPERT-A, verification_status:'VERIFIED' }` | Verified expert A (accepted) |
| `FX-006` | DB seed | `expert_profiles { user_id: FX-EXPERT-B, verification_status:'VERIFIED' }` | Verified expert B (not accepted — gating test) |
| `FX-007` | DB seed | `expert_profiles { user_id: FX-EXPERT-C, verification_status:'PENDING' }` | Unverified expert (RBAC test) |
| `FX-008` | JWT | `{ sub: FX-EXPERT-A.user_id, role: 'EXPERT' }` | Auth context Expert A |
| `FX-009` | JWT | `{ sub: FX-EXPERT-B.user_id, role: 'EXPERT' }` | Auth context Expert B |
| `FX-010` | JWT | `{ sub: FX-EXPERT-C.user_id, role: 'EXPERT' }` | Auth context unverified Expert C |

---

## 4. Test Case Specification

> **TC ID format:** `MAP151-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW (theo CVSS)
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeEntity()
// ═══════════════════════════════════════════════════════════

// NearbySupportContactTestFactory.java
class NearbySupportContactTestFactory {

    static final UUID MOTHER_USER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID EXPERT_A_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    static final UUID EXPERT_A_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    static final UUID EXPERT_B_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_B_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");

    // Giá trị baseline hợp lệ — đồng bộ với FX-001 (§3 TDS-05)
    static EmergencyEvent makeOpenUnassignedEvent() {
        EmergencyEvent e = new EmergencyEvent();
        e.setEmergencyEventId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        e.setUserId(MOTHER_USER_ID);
        e.setStatus("OPEN");
        e.setSelectedExpertId(null);
        e.setRiskLevel("HIGH");
        return e;
    }

    // Overload để override specific fields
    static EmergencyEvent makeOpenUnassignedEvent(Consumer<EmergencyEvent> overrides) {
        EmergencyEvent e = makeOpenUnassignedEvent();
        overrides.accept(e);
        return e;
    }

    static EmergencyEvent makeAlreadyAcceptedEvent(UUID acceptingExpertProfileId) {
        return makeOpenUnassignedEvent(e -> e.setSelectedExpertId(acceptingExpertProfileId));
    }

    static User makeMotherUser() {
        User u = new User();
        u.setUserId(MOTHER_USER_ID);
        u.setFullName("Nguyễn Thị Test");
        u.setPhone("+84900000001");
        u.setEmail("mother-test@example.com");
        return u;
    }

    static LocationSnapshot makeExactSnapshot(UUID emergencyEventId) {
        LocationSnapshot s = new LocationSnapshot();
        s.setContextType("EMERGENCY_EVENT");
        s.setContextId(emergencyEventId);
        s.setLatitude(new BigDecimal("10.776889"));
        s.setLongitude(new BigDecimal("106.700912"));
        return s;
    }

    static ExpertProfile makeVerifiedExpert(UUID userId, UUID profileId) {
        ExpertProfile p = new ExpertProfile();
        p.setExpertProfileId(profileId);
        p.setUserId(userId);
        p.setVerificationStatus("VERIFIED");
        return p;
    }

    static ExpertProfile makeUnverifiedExpert(UUID userId, UUID profileId) {
        ExpertProfile p = makeVerifiedExpert(userId, profileId);
        p.setVerificationStatus("PENDING");
        return p;
    }
}
```

---

### MAP151-TC-001 — Accept thành công khi request đang OPEN, chưa ai accept

**Severity:** `CRITICAL`
**Feature Under Test:** `NearbySupportContactService.acceptRequest()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportContactServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-MAP-206 §Decision`

**Preconditions:**
- `FX-001` — `emergency_events` OPEN, `selected_expert_id IS NULL`
- `FX-005` — Expert A VERIFIED

**Test Steps:**
1. Arrange: mock `emergencyEventRepository.conditionalAccept(eventId, expertProfileId)` trả `1` (rowsAffected)
2. Act: gọi `acceptRequest(eventId, expertAUserId)`
3. Assert: response `status == "ACCEPTED"`, `acceptedAt` không null

**Expected Result (PASS — hành vi đúng):**
- `SupportRequestAcceptResponse{status:"ACCEPTED"}` trả về; `RequestAccepted` event được publish đúng 1 lần với `payload.expertProfileId = expertAProfileId`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception ném ra hoặc `status != "ACCEPTED"` hoặc event không publish

**Current Status:** 🔴 Not written
**Implementation Note:** Dùng `@Modifying @Query` conditional UPDATE — KHÔNG SELECT trước rồi UPDATE riêng (vi phạm C1).

---

### MAP151-TC-002 — Accept thất bại (409) khi request đã bị accept bởi Expert khác

**Severity:** `CRITICAL`
**Feature Under Test:** `NearbySupportContactService.acceptRequest()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-MAP-206 §Decision`, `SRS §3.3.5.6 Exceptions E2`

**Preconditions:**
- `FX-002` — `emergency_events` đã có `selected_expert_id = FX-EXPERT-A.profileId`

**Test Steps:**
1. Arrange: mock `conditionalAccept()` trả `0` (rowsAffected — điều kiện WHERE không khớp)
2. Act: gọi `acceptRequest(eventId, expertBUserId)`
3. Assert: `SupportRequestAlreadyAcceptedException` được ném, mã lỗi `MAP-206`

**Expected Result (PASS):**
- Exception `SupportRequestAlreadyAcceptedException` với `errorCode = "MAP-206"`; HTTP layer map sang `409 Conflict`

**Expected Result (FAIL):**
- Exception khác được ném, hoặc silent success (accept "giả" không thực sự ghi DB)

**Current Status:** 🔴 Not written

---

### MAP151-TC-INT-001 — Race condition: 2 Expert cùng accept đồng thời

**Severity:** `CRITICAL`
**Feature Under Test:** `IEmergencyEventRepository.conditionalAccept()` (Testcontainers, real Postgres, 2 concurrent threads)
**Test File:** `src/test/java/com/carebridge/backend/map/repository/EmergencyEventRepositoryConcurrencyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-MAP-206 §Decision`, `CB-MAP-IMP-004 §4.1 Concurrency SLA`

**Preconditions:**
- PostgreSQL Testcontainer chạy, Flyway migration applied
- Seed `FX-001` (OPEN, unassigned)
- 2 Expert profile: Expert A (`FX-005`), Expert B (`FX-006`)

**Test Steps:**
1. Arrange: 2 thread cùng gọi `conditionalAccept(eventId, expertAProfileId)` và `conditionalAccept(eventId, expertBProfileId)` gần như đồng thời (dùng `CountDownLatch` để đồng bộ start)
2. Act: chờ cả 2 thread hoàn thành
3. Assert: tổng `rowsAffected` của 2 lần gọi = 1 (chỉ 1 thành công); query lại DB xác nhận `selected_expert_id` chỉ chứa 1 trong 2 giá trị

**Expected Result (PASS):**
- Đúng 1 UPDATE thành công (`rowsAffected=1`), UPDATE còn lại trả `rowsAffected=0`; DB state cuối cùng nhất quán (không có giá trị NULL hay giá trị thứ 3 lạ)

**Expected Result (FAIL):**
- Cả 2 UPDATE đều thành công (race condition thực sự xảy ra — BUG NGHIÊM TRỌNG) hoặc deadlock/timeout

**DB Assertion:**
```java
EmergencyEvent record = emergencyEventRepository.findById(eventId).orElseThrow();
assertThat(record.getSelectedExpertId()).isIn(expertAProfileId, expertBProfileId);
assertThat(totalRowsAffected).isEqualTo(1);
```

**Current Status:** 🔴 Not written

---

### MAP151-TC-003 — `getContactDetail()` trả full PII đúng cho Expert đã accept

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportContactService.getContactDetail()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-MAP-207 §Decision`

**Preconditions:**
- `FX-002` — `emergency_events.selected_expert_id = FX-EXPERT-A.profileId`
- `FX-003` — `users` record cho Mother
- `FX-004` — `location_snapshots` toạ độ chính xác

**Test Steps:**
1. Arrange: mock repositories trả đúng fixtures
2. Act: gọi `getContactDetail(eventId, expertAUserId)`
3. Assert: response chứa `motherFullName`, `motherPhone` đúng `FX-003`; `exactLatitude`/`exactLongitude` KHÔNG làm tròn (so với UC150's rounding)

**Expected Result (PASS):**
- `SupportRequestContactDetailResponse{motherFullName:"Nguyễn Thị Test", motherPhone:"+84900000001", exactLatitude:10.776889, exactLongitude:106.700912}`

**Expected Result (FAIL):**
- Field PII bị thiếu/null, hoặc toạ độ bị làm tròn (nhầm dùng logic UC150 thay vì exact)

**Current Status:** 🔴 Not written

---

### MAP151-TC-004 — `logContact()` ghi notification cho Mother + publish event

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportContactService.logContact()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MAP-207 §Decision`, `ADR-MAP-209 §Decision`

**Preconditions:**
- `FX-002` — accepted by Expert A

**Test Steps:**
1. Act: gọi `logContact(eventId, expertAUserId, new ContactLogRequest(CALL))`
2. Assert: `notificationRepository.save()` được gọi đúng 1 lần với `recipient_user_id = motherUserId`, `notification_type` phù hợp; `NearbyUserContacted` event publish đúng 1 lần

**Expected Result (PASS):**
- `ContactLogResponse{notificationId, loggedAt}` trả về; notification + event đều được ghi

**Expected Result (FAIL):**
- Thiếu notification hoặc thiếu event publish

**Current Status:** 🔴 Not written

---

### MAP151-TC-005 — Mobile: `call()` dùng `tel:` dialer, log fire-and-forget SAU khi launch

**Severity:** `HIGH`
**Feature Under Test:** `ContactNearbyUserService.call()` / `.logContactFireAndForget()`
**Test File:** `05_Development/CareBridgeMobileApp/test/features/nearbySupport/services/contact_nearby_user_service_test.dart`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-MAP-207 §Decision` (mirror `ADR-MAP-005`/`ADR-MAP-007` UC64)

**Preconditions:**
- Mock `url_launcher` package

**Test Steps:**
1. Arrange: mock `canLaunchUrl`/`launchUrl` trả `true` cho `tel:+84900000001`
2. Act: gọi `call("+84900000001")` rồi `logContactFireAndForget(...)`
3. Assert: `launchUrl(Uri.parse('tel:+84900000001'))` được gọi TRƯỚC `logContactFireAndForget` (thứ tự); KHÔNG có bất kỳ import/reference nào tới `ZegoCloud*` class trong file

**Expected Result (PASS):**
- Dialer launch trước, log API gọi sau (fire-and-forget, không `await` chặn); source code không chứa `ZegoCloud`

**Expected Result (FAIL):**
- Thứ tự đảo ngược (chờ log API trước khi gọi), hoặc phát hiện import ZegoCloud

**Current Status:** 🔴 Not written

---

### MAP151-TC-006 — Reject (403) Expert chưa VERIFIED

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `NearbySupportContactController` (accept / contact-detail / contact)
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportContactControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-MAP-208 §Decision`

**Preconditions:**
- `FX-007` — Expert C `verification_status='PENDING'`, JWT `FX-010`

**Test Steps (Attack Simulation):**
1. Gọi cả 3 endpoint (`accept`, `contact-detail`, `contact`) với JWT của Expert C (unverified)
2. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden` với `error.code = "MAP-208"` cho cả 3 endpoint

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Bất kỳ endpoint nào trả `200`/`201`/`409` (nghĩa là logic nghiệp vụ chạy trước khi kiểm tra VERIFIED)

**Current Status:** 🔴 Not written

---

### MAP151-TC-007 — Reject (403) Expert VERIFIED nhưng chưa accept request này — `contact-detail`

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `PDPA / Luật 91/2025 — minimum-necessary`
**Feature Under Test:** `NearbySupportContactService.getContactDetail()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-MAP-208 §Decision`

**Preconditions:**
- `FX-002` — accepted bởi Expert A (`FX-005`)
- Expert B (`FX-006`) VERIFIED nhưng KHÔNG phải người accept

**Test Steps (Attack Simulation):**
1. Gọi `getContactDetail(eventId, expertBUserId)` — Expert B cố truy cập contact detail của request Expert A đã accept
2. Kiểm tra response/exception

**Expected Result (PASS = hệ thống an toàn):**
- `AccessDeniedException` với `errorCode="MAP-208"`; KHÔNG có `motherFullName`/`motherPhone`/toạ độ nào bị trả về trong exception message hay log

**Expected Result (FAIL = lỗ hổng tồn tại — PII LEAK):**
- Response trả về đầy đủ PII của Mother cho Expert B (KHÔNG được accept) — đây là vi phạm PDPA nghiêm trọng nhất có thể xảy ra trong module này

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test case QUAN TRỌNG NHẤT của toàn bộ UC151 — PHẢI pass trước khi merge bất kỳ thay đổi nào liên quan `getContactDetail()`.

---

### MAP151-TC-008 — Reject (403) Expert VERIFIED nhưng chưa accept request này — `contact`

**Severity:** `CRITICAL`
**CWE:** `CWE-639`
**Legal:** `PDPA / Luật 91/2025`
**Feature Under Test:** `NearbySupportContactService.logContact()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-MAP-208 §Decision`

**Preconditions:**
- Giống `MAP151-TC-007`

**Test Steps (Attack Simulation):**
1. Gọi `logContact(eventId, expertBUserId, request)` — Expert B chưa accept

**Expected Result (PASS = hệ thống an toàn):**
- `AccessDeniedException` `MAP-208`; KHÔNG có `notifications` record nào bị tạo, KHÔNG event publish

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Notification được tạo/event publish dù Expert B chưa được uỷ quyền

**Current Status:** 🔴 Not written

---

### MAP151-TC-009 — 403 response KHÔNG tiết lộ danh tính Expert đã accept

**Severity:** `HIGH`
**CWE:** `CWE-209 — Information Exposure Through an Error Message`
**Legal:** `PDPA`
**Feature Under Test:** `NearbySupportContactController` (error response body)
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportContactControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-MAP-208 §Decision`

**Test Steps (Attack Simulation):**
1. Expert B gọi `contact-detail` cho request đã accept bởi Expert A
2. Inspect response body chi tiết (JSON toàn bộ, không chỉ status code)

**Expected Result (PASS = hệ thống an toàn):**
- `error.message` generic ("You have not accepted this support request") — KHÔNG chứa `expertProfileId`, tên, hay bất kỳ định danh nào của Expert A

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Message tiết lộ "already accepted by expert <uuid/name>"

**Current Status:** 🔴 Not written

---

### MAP151-TC-010 — Domain events publish trong cùng transaction — rollback nếu publish lỗi

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportContactService.acceptRequest()` (transactional behavior)
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportContactServiceTransactionTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-MAP-209 §Decision`

**Preconditions:**
- Testcontainers Postgres; mock `ApplicationEventPublisher` để throw exception khi publish

**Test Steps:**
1. Arrange: mock event publisher ném `RuntimeException` khi publish `RequestAccepted`
2. Act: gọi `acceptRequest(eventId, expertUserId)`
3. Assert: transaction rollback — query lại DB xác nhận `selected_expert_id` VẪN LÀ NULL (không có state thay đổi một phần)

**Expected Result (PASS):**
- Toàn bộ transaction rollback; `emergency_events.selected_expert_id` không đổi; exception propagate lên caller

**Expected Result (FAIL):**
- DB đã ghi `selected_expert_id` NHƯNG event không publish (audit trail thiếu — vi phạm ADR-MAP-209)

**DB Assertion:**
```java
EmergencyEvent record = emergencyEventRepository.findById(eventId).orElseThrow();
assertThat(record.getSelectedExpertId()).isNull(); // rollback xác nhận
```

**Current Status:** 🔴 Not written

---

### MAP151-TC-011 — DB unavailable → 503 MAP-210, không duplicate action

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbySupportContactController` (exception handling)
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportContactControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `SRS §3.3.5.6 Exceptions E3`

**Test Steps:**
1. Arrange: mock repository throw `DataAccessException`
2. Act: gọi `acceptRequest()`
3. Assert: response `503` với `error.code="MAP-210"`

**Expected Result (PASS):**
- `503 Service Unavailable`, `MAP-210`, không có state thay đổi (transaction rollback tự động)

**Expected Result (FAIL):**
- Exception không handled đúng (500 generic, hoặc leak stack trace ra response)

**Current Status:** 🔴 Not written

---

### MAP151-TC-012 — `emergencyEventId` không tồn tại → 404 MAP-209

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbySupportContactService`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportContactServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-MAP-IMP-004 §10 Error Codes`

**Test Steps:**
1. Act: gọi `getContactDetail(nonExistentEventId, expertUserId)`
2. Assert: `EntityNotFoundException` mã `MAP-209`

**Expected Result (PASS):**
- `404 Not Found`, `MAP-209`

**Current Status:** 🔴 Not written

---

### MAP151-TC-013 — `channel` không hợp lệ trong `POST .../contact` → 400 MAP-207

**Severity:** `LOW`
**Feature Under Test:** `NearbySupportContactController.logContact()` (DTO validation)
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportContactControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-MAP-IMP-004 §10 Error Codes`

**Test Steps:**
1. Gọi `POST .../contact` với `{"channel": "SMS"}` (không thuộc enum `ContactChannelType`)
2. Assert: `400 Bad Request`, `MAP-207`

**Expected Result (PASS):**
- `400`, `error.code="MAP-207"`, `details` chỉ rõ field `channel`

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### MAP151-TC-E2E-001 — Full flow: accept → contact-detail → contact log (Happy Path E2E)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST accept → GET contact-detail → POST contact`
**Test File:** `src/test/java/com/carebridge/backend/map/NearbySupportContactE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tự động khi Spring context start
- Seed: `FX-001` (OPEN event), `FX-003` (Mother user), `FX-004` (location snapshot), `FX-005` (Expert A VERIFIED)

**Test Steps:**
1. `POST /api/v1/map/support-requests/{id}/accept` với JWT Expert A → expect `200`
2. `GET /api/v1/map/support-requests/{id}/contact-detail` với JWT Expert A → expect `200` + full PII đúng fixture
3. `POST /api/v1/map/support-requests/{id}/contact` với `{"channel":"CALL"}`, JWT Expert A → expect `201`
4. Query DB: `emergency_events.selected_expert_id = expertAProfileId`; `notifications` có 1 record `reference_id = eventId`

**Expected Result (PASS):**
- Toàn bộ 3 bước trả đúng status code; DB state nhất quán; 2 domain events (`RequestAccepted`, `NearbyUserContacted`) đã publish

**Expected Result (FAIL):**
- Bất kỳ bước nào lỗi, hoặc DB state không khớp

**DB Assertion:**
```java
EmergencyEvent record = emergencyEventRepository.findById(eventId).orElseThrow();
assertThat(record.getSelectedExpertId()).isEqualTo(expertAProfileId);
List<Notification> notifications = notificationRepository.findByReferenceId(eventId);
assertThat(notifications).hasSize(1);
assertThat(notifications.get(0).getRecipientUserId()).isEqualTo(motherUserId);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MAP151-TC-001` | `NearbySupportContactServiceTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-002` | `NearbySupportContactServiceTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-INT-001` | `EmergencyEventRepositoryConcurrencyTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-003` | `NearbySupportContactServiceTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-004` | `NearbySupportContactServiceTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-005` | `contact_nearby_user_service_test.dart:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-006` | `NearbySupportContactControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-007` | `NearbySupportContactServiceTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-008` | `NearbySupportContactServiceTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-009` | `NearbySupportContactControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-010` | `NearbySupportContactServiceTransactionTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-011` | `NearbySupportContactControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-012` | `NearbySupportContactServiceTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-013` | `NearbySupportContactControllerTest.java:TBD` | `[ ]` | `[ ]` | — |
| `MAP151-TC-E2E-001` | `NearbySupportContactE2ETest.java:TBD` | `[ ]` | `[ ]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class NearbySupportContactService implements INearbySupportContactService {

    @Override
    public SupportRequestAcceptResponse acceptRequest(UUID emergencyEventId, UUID expertUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public SupportRequestContactDetailResponse getContactDetail(UUID emergencyEventId, UUID expertUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ContactLogResponse logContact(UUID emergencyEventId, UUID expertUserId, ContactLogRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MAP151-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `MAP151-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP151-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP151-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP151-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP151-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MAP-IMP-004` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] UC150 (`CB-MAP-IMP-003`) implementation đã sẵn sàng — `IEmergencyEventRepository`/`ILocationSnapshotRepository` phải tồn tại để tái sử dụng
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers), đặc biệt `MAP151-TC-INT-001` (race condition)
- [ ] `flutter test` — Mobile `ContactNearbyUserService` tests xanh
- [ ] Test coverage ≥ 80% lines cho `NearbySupportContactService`
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] `MAP151-TC-007`/`MAP151-TC-008` (gating CRITICAL) PASS — bắt buộc trước khi merge bất kỳ PR nào chạm `getContactDetail()`/`logContact()`

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria (Điều kiện tạm dừng)

- UC150 implementation chưa sẵn sàng (blocker dependency cho `IEmergencyEventRepository`)
- Phát hiện lỗi kiến trúc mới cần Principal Architect review (gating mechanism đã CONFIRMED 2026-07-02 — chỉ áp dụng nếu phát sinh vấn đề kiến trúc MỚI, không liên quan Open Item đã đóng)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — KHÔNG chạy trên production)
# N/A — UC151 không có migration mới (xem TDS §5.2)

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/map/controller/NearbySupportContactController.java
git checkout -- src/main/java/com/carebridge/backend/map/service/
git checkout -- src/main/java/com/carebridge/backend/map/dto/
git checkout -- src/main/java/com/carebridge/backend/map/event/RequestAccepted.java
git checkout -- src/main/java/com/carebridge/backend/map/event/NearbyUserContacted.java
git checkout -- src/test/java/com/carebridge/backend/map/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/nearbySupport/services/
git checkout -- 05_Development/CareBridgeMobileApp/test/features/nearbySupport/

# Gap vẫn OPEN → giữ nguyên entry trong tracking
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR (vd: giả định bảng `support_request_acceptances` tồn tại) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic (vd: kiểm tra gating logic trực tiếp trong Controller test thay vì Service test) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (vd: `SupportRequestAcceptanceRepository` không có trong §8 TDS) | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Draft. Chưa Approved. Gating/accept mechanism đã RESOLVED (Confirmed by Product Owner 2026-07-02). Xem TDS `CB-MAP-IMP-004` §2 cho Open Items còn lại cần resolve trước khi Approve.*
