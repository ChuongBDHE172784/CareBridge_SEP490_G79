# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC150 — View Nearby Support Requests

**Document ID:** `CB-MAP-TDD-003`
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
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.6.3` — Functional requirements (UC-150)
- `04_Implement/UC150_ViewNearbySupportRequests/UC150_ViewNearbySupportRequests_TDS.md` (`CB-MAP-IMP-003`) — Technical Specification
- `04_Implement/UC129_CalculateDistanceRouteAndETA/UC129_CalculateDistanceRouteAndETA_TDS.md` — `IMapProviderService` contract
- `04_Implement/UC63_FindNearbyCareFacility/UC63_FindNearbyCareFacility_Test-Spec.md` — pattern reference (bounding-box/minimum-necessary test style)
- Luật 91/2025 (PDPA Việt Nam) — minimum-necessary principle

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-02` | `AI Agent — Test Designer` | Khởi tạo tài liệu — TDD spec cho UC150 View Nearby Support Requests |
| `2026-07-02` | `AI Agent — Test Designer` | **Đóng Open Item (gating mechanism):** Product Owner đã CONFIRMED cơ chế "accept" (`selected_expert_id`/`status`) — xem TDS §2 CHANGELOG. `MAP150-TC-011` không còn phụ thuộc giả định chờ xác nhận; Suspension Criteria liên quan đã gỡ bỏ. L3 (§2) cập nhật trạng thái. |

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
| **Feature / Gap ID** | `GAP-UC150` |
| **Module** | `View Nearby Support Requests — Bounded Context: map` |
| **Spec gốc** | `CB-MAP-IMP-003` |
| **Priority** | 🟠 P1 (privacy-critical — minimum-necessary boundary) |
| **Sprint** | `Sprint 3 (TV4 - Lâm)` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025 — minimum-necessary` |
| **Upstream Dependencies** | `emergency_events`, `location_snapshots`, `expert_profiles`, `IMapProviderService (UC129)` |
| **Downstream Consumers** | `UC151 Contact Nearby User`, `UC152 Navigate to Support Location` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MAP-IMP-003 §17`, `ADR-MAP-201..205` |
| **Constraints Injected** | C1 (no PII fields), C2 (coordinate rounding), C3 (Haversine reuse), C4 (RBAC+VERIFIED), C5 (empty state 200), C6 (best-effort event) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.6.3 Business Rules chỉ liệt kê `BR-RBAC`, không minh thị `BR-PRIVACY` | Description "with minimum necessary data" ngụ ý PDPA minimum-necessary tinh thần | Test cases TC-COND-003/004/005 verify DTO fields bằng reflection/JSON schema check, coi đây là behavior bắt buộc dù không trích BR trực tiếp — ghi rõ oracle là `TDS ADR-MAP-202`, không phải `BR-PRIVACY` SRS field |
| L2 | `emergency_events` không có cột lat/lng trực tiếp | Toạ độ lấy qua JOIN quy ước `location_snapshots.context_type/context_id` (chưa xác nhận giá trị `context_type` chính thức) | Test dùng fixture với `context_type = 'EMERGENCY_EVENT'` (giá trị đề xuất) — đánh dấu test là `🟡 Assumption-dependent`, cần re-verify khi UC141 xác nhận convention thật |
| L3 | Không có bảng/cột chính thức đánh dấu "accepted" | **[RESOLVED 2026-07-02]** `selected_expert_id IS NULL AND status='OPEN'` = chưa accept — CONFIRMED bởi Product Owner (TDS §2 CHANGELOG), không còn là suy luận chờ xác nhận | Test chỉ verify UC150 luôn trả `MINIMUM_NECESSARY` cho các record thoả điều kiện trên — KHÔNG test cơ chế "accept" (thuộc UC151, ngoài phạm vi UC150) |
| L4 | `expert_profiles.verification_status` không có CHECK constraint liệt kê enum đầy đủ trong `V1__init_schema.sql` | Giá trị `'VERIFIED'` dùng theo convention đã thấy ở default `'PENDING'` | Test dùng literal string `"VERIFIED"`/`"PENDING"` — nếu enum đổi cần cập nhật fixture |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Module View Nearby Support Requests bao gồm các layer:
├── Domain (pure logic — coordinate rounding, minimum-necessary mapping — no deps)
├── Service (mock IEmergencyEventRepository, ILocationSnapshotRepository,
│            IExpertProfileRepository, IMapProviderService với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest — seed emergency_events + location_snapshots)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-150 §3.3.6.3` | Minimum-necessary display, BR-RBAC, AF2 empty state, E1/E2/E3 exceptions |
| `ADR-MAP-201` | Bounding-box + JOIN `location_snapshots` via `context_type`/`context_id` |
| `ADR-MAP-202` | Minimum-necessary DTO fields, coordinate rounding 2 decimals |
| `ADR-MAP-203` | Best-effort `SupportRequestViewed` publish |
| `ADR-MAP-204` | RBAC `ROLE_EXPERT` + `verification_status='VERIFIED'` |
| `ADR-MAP-205` | Reuse `IMapProviderService.calculateHaversineDistance()` |
| `V1__init_schema.sql` | `emergency_events`, `location_snapshots`, `expert_profiles` structure/constraints |
| PDPA / Luật 91/2025 | Minimum-necessary access rules |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Bounding-box query trả đúng `emergency_events` có `status='OPEN'` trong bán kính | `NearbySupportRequestService.findNearbyRequests()` | `MAP150-TC-001` |
| TC-COND-002 | Toạ độ lấy đúng qua JOIN `location_snapshots` theo `context_type`/`context_id` | `NearbySupportRequestService` | `MAP150-TC-002` |
| TC-COND-003 | **CRITICAL** — DTO response KHÔNG chứa `userId`/`fullName`/`phone`/`email` | `NearbySupportRequestItem` | `MAP150-TC-003` |
| TC-COND-004 | **CRITICAL** — Toạ độ trong DTO được làm tròn 2 chữ số thập phân | `NearbySupportRequestService.roundCoordinate()` | `MAP150-TC-004` |
| TC-COND-005 | **CRITICAL** — API response JSON (end-to-end) không leak PII field qua bất kỳ đường nào (kể cả nested/error) | `NearbySupportRequestController` | `MAP150-TC-005` |
| TC-COND-006 | `SupportRequestViewed` publish best-effort, lỗi publish không chặn response | `NearbySupportRequestService` | `MAP150-TC-006` |
| TC-COND-007 | RBAC: user không có `ROLE_EXPERT` → 403 | `NearbySupportRequestController` | `MAP150-TC-007` |
| TC-COND-008 | RBAC: `ROLE_EXPERT` nhưng `verification_status != 'VERIFIED'` → 403 `MAP-204` | `NearbySupportRequestService` | `MAP150-TC-008` |
| TC-COND-009 | `distanceKm` tính bằng `IMapProviderService.calculateHaversineDistance()`, không tự viết Haversine | `NearbySupportRequestService` | `MAP150-TC-009` |
| TC-COND-010 | Danh sách rỗng (AF2) → HTTP 200 `items:[]`, không 404 | `NearbySupportRequestController` | `MAP150-TC-010` |
| TC-COND-011 | State-transition boundary: request đã có `selected_expert_id` KHÔNG xuất hiện trong danh sách UC150 (đã "accepted", ngoài phạm vi minimum-necessary view) | `NearbySupportRequestService` | `MAP150-TC-011` |
| TC-COND-012 | Invalid coordinates (lat/lng out of range) → 400 `MAP-201` | `NearbySupportRequestController` | `MAP150-TC-012` |
| TC-COND-013 | Unauthenticated (no JWT) → 401 | `NearbySupportRequestController` | `MAP150-TC-013` |
| TC-COND-INT-001 | Integration: full flow DB seed → API call → assert DB unchanged (read-only) + DTO minimum-necessary | `NearbySupportRequestController` + Testcontainers | `MAP150-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | latitude/longitude domain (valid/invalid ranges) | Boundary giữa hợp lệ (-90..90/-180..180) và không hợp lệ |
| Boundary Value Analysis | `radiusKm` (0.1/50.0), `maxResults` (1/50), coordinate rounding boundary | Biên giá trị mặc định/giới hạn |
| Security/Field-level Assertion | DTO JSON schema (TC-COND-003/005) | Privacy-critical — phải verify bằng reflection/JSON path, không chỉ "trust the code" |
| Error Guessing | Missing `location_snapshots` record cho 1 `emergency_events`, RBAC bypass attempt | Robustness/security |
| State Transition Testing | `selected_expert_id` null vs non-null (TC-COND-011) | Visibility gating boundary |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-150-001` | DB seed | `emergency_events{status:'OPEN', selected_expert_id:NULL, risk_level:'HIGH'}` + `location_snapshots{context_type:'EMERGENCY_EVENT', context_id:<eventId>, lat:10.7769, lng:106.7009}` | Happy path — request hiển thị được |
| `FX-150-002` | DB seed | `emergency_events{status:'OPEN', selected_expert_id:<someExpertId>}` | TC-COND-011 — request đã accept, KHÔNG hiển thị |
| `FX-150-003` | DB seed | `expert_profiles{verification_status:'VERIFIED'}` | RBAC happy path |
| `FX-150-004` | DB seed | `expert_profiles{verification_status:'PENDING'}` | TC-COND-008 — unverified expert |
| `FX-150-005` | JWT | `{ sub: expertUserId, role: 'EXPERT' }` | Auth context (verified expert) |
| `FX-150-006` | JWT | `{ sub: motherUserId, role: 'MOTHER' }` | TC-COND-007 — wrong role |
| `FX-150-007` | Mock | `IMapProviderService.calculateHaversineDistance()` returns `1.8` | Isolate distance calc từ UC129 impl |

### TDS-06 — Applicability Matrix

| Layer | Backend Unit | Backend Integration | Web | Mobile |
|-------|-------------|----------------------|-----|--------|
| Service logic (mapping, rounding, RBAC) | ✅ | ✅ (Testcontainers) | N/A | N/A |
| Controller/API contract | ✅ (@WebMvcTest) | ✅ (MockMvc + Testcontainers) | N/A (Expert App = Mobile only theo SRS "Platform: Expert App") | 🟡 Widget test cho list/map rendering — Open, phạm vi UI ngoài Draft này |
| Security (RBAC/PII leak) | ✅ | ✅ | N/A | N/A |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// NearbySupportRequestTestFactory.java
// ═══════════════════════════════════════════════════════════
class NearbySupportRequestTestFactory {

    static EmergencyEvent makeOpenUnassignedEvent() {
        return makeOpenUnassignedEvent(e -> {});
    }

    static EmergencyEvent makeOpenUnassignedEvent(Consumer<EmergencyEvent> overrides) {
        EmergencyEvent event = new EmergencyEvent();
        event.setEmergencyEventId(UUID.randomUUID());
        event.setUserId(UUID.randomUUID());          // Mother — synthetic
        event.setRiskLevel("HIGH");
        event.setStatus("OPEN");
        event.setSelectedExpertId(null);
        event.setOpenedAt(Instant.parse("2026-07-02T08:00:00Z"));
        overrides.accept(event);
        return event;
    }

    static LocationSnapshot makeSnapshotFor(UUID emergencyEventId) {
        LocationSnapshot snapshot = new LocationSnapshot();
        snapshot.setLocationSnapshotId(UUID.randomUUID());
        snapshot.setContextType("EMERGENCY_EVENT");
        snapshot.setContextId(emergencyEventId);
        snapshot.setLatitude(new BigDecimal("10.776900"));
        snapshot.setLongitude(new BigDecimal("106.700900"));
        snapshot.setCapturedAt(Instant.parse("2026-07-02T08:00:00Z"));
        return snapshot;
    }

    static ExpertProfile makeVerifiedExpert(UUID userId) {
        ExpertProfile profile = new ExpertProfile();
        profile.setExpertProfileId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setVerificationStatus("VERIFIED");
        return profile;
    }

    static ExpertProfile makePendingExpert(UUID userId) {
        ExpertProfile profile = makeVerifiedExpert(userId);
        profile.setVerificationStatus("PENDING");
        return profile;
    }

    static NearbySupportRequestSearchRequest makeSearchRequest() {
        return makeSearchRequest(r -> {});
    }

    static NearbySupportRequestSearchRequest makeSearchRequest(Consumer<NearbySupportRequestSearchRequest> overrides) {
        NearbySupportRequestSearchRequest request = new NearbySupportRequestSearchRequest();
        request.setLatitude(10.7769);
        request.setLongitude(106.7009);
        request.setRadiusKm(5.0);
        request.setMaxResults(20);
        overrides.accept(request);
        return request;
    }
}
```

---

### MAP150-TC-001 — Bounding-box query trả đúng OPEN emergency_events trong bán kính

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportRequestService.findNearbyRequests()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportRequestServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-MAP-201 §Decision`

**Preconditions:**
- Mock `IEmergencyEventRepository` trả `FX-150-001` (1 record OPEN + unassigned)
- Mock `ILocationSnapshotRepository` trả snapshot tương ứng
- Mock `IExpertProfileRepository` trả `FX-150-003` (verified expert)

**Test Steps:**
1. Arrange: dùng `NearbySupportRequestTestFactory.makeOpenUnassignedEvent()` + `makeSnapshotFor()`, mock repo trả về
2. Act: gọi `service.findNearbyRequests(makeSearchRequest(), expertUserId)`
3. Assert: `response.getItems()` có đúng 1 item, `emergencyEventId` khớp với fixture

**Expected Result (PASS):**
- `items.size() == 1`, `items.get(0).getEmergencyEventId()` == fixture ID

**Expected Result (FAIL):**
- Trả về danh sách rỗng hoặc sai ID — chứng tỏ query logic sai

**Current Status:** 🔴 Not written

---

### MAP150-TC-002 — Toạ độ lấy đúng qua JOIN location_snapshots convention

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportRequestService.findNearbyRequests()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-MAP-201 §Bối cảnh (Open Item — context_type convention)`

**Preconditions:**
- `FX-150-001` + `FX-150-002`-style snapshot với `context_type='EMERGENCY_EVENT'`

**Test Steps:**
1. Arrange: seed snapshot với toạ độ cụ thể (10.7769, 106.7009)
2. Act: gọi `findNearbyRequests()`
3. Assert: `approximateLatitude`/`approximateLongitude` trong response khớp giá trị đã làm tròn từ snapshot

**Expected Result (PASS):**
- `approximateLatitude == 10.78` (làm tròn từ 10.7769)

**Expected Result (FAIL):**
- Toạ độ null hoặc không khớp — chứng tỏ JOIN logic sai hoặc `context_type` convention không đúng

**Current Status:** 🔴 Not written
**Implementation Note:** 🟡 Test này phụ thuộc giả định `context_type='EMERGENCY_EVENT'` (L2 §2) — re-verify khi UC141 xác nhận giá trị thật.

---

### MAP150-TC-003 — CRITICAL: DTO KHÔNG chứa PII định danh trực tiếp

**Severity:** `CRITICAL`
**CWE:** `CWE-359 — Exposure of Private Personal Information to an Unauthorized Actor`
**Legal:** `PDPA / Luật 91/2025 — minimum-necessary principle`
**Feature Under Test:** `NearbySupportRequestItem` (DTO structure)
**Test File:** `src/test/java/com/carebridge/backend/map/dto/NearbySupportRequestItemTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-MAP-202 §Decision (Phương án A)`

**Preconditions:**
- Class `NearbySupportRequestItem` đã compile (stub)

**Test Steps:**
1. Arrange: dùng Java Reflection để liệt kê tất cả field names của `NearbySupportRequestItem`
2. Act: kiểm tra danh sách field names
3. Assert: KHÔNG có field nào tên chứa (case-insensitive) `userId`, `fullName`, `phone`, `email`, `name` (ngoại trừ không áp dụng — class không có field liên quan danh tính)

**Expected Result (PASS — hành vi đúng):**
```java
Set<String> forbiddenPatterns = Set.of("userid", "fullname", "phone", "email");
List<Field> fields = Arrays.asList(NearbySupportRequestItem.class.getDeclaredFields());
boolean hasViolation = fields.stream()
    .map(f -> f.getName().toLowerCase())
    .anyMatch(name -> forbiddenPatterns.stream().anyMatch(name::contains));
assertThat(hasViolation).isFalse();
```

**Expected Result (FAIL — dấu hiệu lỗi):**
- Reflection tìm thấy field `userId`/`fullName`/`phone`/`email` → vi phạm C1, phải reject implementation

**Current Status:** 🔴 Not written
**Implementation Note:** Test này PHẢI FAIL nếu implementation stub trả `UnsupportedOperationException` — tự động fail vì exception, không phải vì assertion. Đảm bảo test setup không silently pass khi method throw (dùng `assertThatThrownBy` riêng cho Red Gate, không lẫn với assertion field này).

---

### MAP150-TC-004 — CRITICAL: Toạ độ làm tròn 2 chữ số thập phân

**Severity:** `CRITICAL`
**Legal:** `PDPA — minimum-necessary (ADR-MAP-202)`
**Feature Under Test:** `NearbySupportRequestService.roundCoordinate()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-MAP-202 §Decision`

**Preconditions:**
- `FX-150-001` snapshot với `latitude=10.776900`, `longitude=106.700900` (6 chữ số thập phân — độ chính xác gốc)

**Test Steps:**
1. Arrange: seed snapshot với toạ độ chính xác cao
2. Act: gọi `findNearbyRequests()`
3. Assert: `approximateLatitude` == `10.78` (BigDecimal scale = 2), `approximateLongitude` == `106.70`

**Expected Result (PASS):**
- `item.getApproximateLatitude().scale() == 2` và giá trị khớp làm tròn HALF_UP từ `10.776900`

**Expected Result (FAIL):**
- Toạ độ trả về giữ nguyên độ chính xác gốc (scale > 2) — vi phạm C2

**Current Status:** 🔴 Not written

---

### MAP150-TC-005 — CRITICAL: End-to-end API response không leak PII (JSON path)

**Severity:** `CRITICAL`
**CWE:** `CWE-359`
**OWASP:** `A01:2021 — Broken Access Control (excessive data exposure)`
**Feature Under Test:** `GET /api/v1/map/support-requests/nearby`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportRequestControllerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MAP-202`, `CB-MAP-IMP-003 §17 C1`

**Preconditions:**
- Testcontainers PostgreSQL running, Flyway migrated
- Seed `FX-150-001` + `users` record với `full_name='Nguyễn Thị Test'`, `phone='+84900000001'`, `email='mother-test@example.com'` (SYNTHETIC data)
- `FX-150-005` JWT (verified expert)

**Test Steps:**
1. Seed DB: `emergency_events` + `location_snapshots` + `users` (Mother với PII synthetic)
2. Call `GET /api/v1/map/support-requests/nearby?latitude=10.7769&longitude=106.7009&radiusKm=5`
3. Assert response JSON string KHÔNG chứa `"Nguyễn Thị Test"`, `"+84900000001"`, `"mother-test@example.com"` ở bất kỳ đâu (kể cả nested)

**Expected Result (PASS):**
```java
String responseBody = result.getResponse().getContentAsString();
assertThat(responseBody).doesNotContain("Nguyễn Thị Test", "+84900000001", "mother-test@example.com");
assertThat(responseBody).doesNotContainPattern("(?i)\"fullName\"|\"phone\"|\"email\"|\"userId\"");
```

**Expected Result (FAIL):**
- Response JSON chứa bất kỳ chuỗi PII nào ở trên — CRITICAL FAIL, chặn release

**DB Assertion:**
```java
// Verify DB vẫn còn nguyên PII gốc (chỉ response bị lọc, không phải data bị xóa)
User motherRecord = userRepository.findById(motherUserId).orElseThrow();
assertThat(motherRecord.getFullName()).isEqualTo("Nguyễn Thị Test");
```

**Current Status:** 🔴 Not written

---

### MAP150-TC-006 — Publish SupportRequestViewed best-effort, không chặn response khi lỗi

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbySupportRequestService.findNearbyRequests()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-MAP-203 §Decision`

**Preconditions:**
- Mock `ApplicationEventPublisher` để throw `RuntimeException` khi `publishEvent()` được gọi

**Test Steps:**
1. Arrange: mock event publisher throw exception
2. Act: gọi `findNearbyRequests()`
3. Assert: method vẫn trả về `NearbySupportRequestListResponse` hợp lệ (KHÔNG propagate exception ra ngoài)

**Expected Result (PASS):**
- Response trả về bình thường, exception bị catch nội bộ (log warning, không rethrow)

**Expected Result (FAIL):**
- `findNearbyRequests()` throw exception ra caller — vi phạm "best-effort, không chặn response"

**Current Status:** 🔴 Not written

---

### MAP150-TC-007 — RBAC: user không có ROLE_EXPERT → 403

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**OWASP:** `A01:2021`
**Feature Under Test:** `GET /api/v1/map/support-requests/nearby`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportRequestControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-MAP-204`, SRS E1

**Preconditions:**
- `FX-150-006` JWT với `role='MOTHER'`

**Test Steps (Attack Simulation):**
1. Chuẩn bị JWT Mother hợp lệ (nhưng sai role)
2. Gọi `GET /api/v1/map/support-requests/nearby` với JWT đó
3. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- `200 OK` với dữ liệu trả về — Mother có thể xem support requests của Expert domain

**Current Status:** 🔴 Not written

---

### MAP150-TC-008 — RBAC: ROLE_EXPERT nhưng chưa VERIFIED → 403 MAP-204

**Severity:** `CRITICAL`
**CWE:** `CWE-862`
**Feature Under Test:** `NearbySupportRequestService.findNearbyRequests()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-MAP-204`

**Preconditions:**
- `FX-150-004` (`expert_profiles.verification_status='PENDING'`)

**Test Steps:**
1. Arrange: mock `IExpertProfileRepository` trả `makePendingExpert()`
2. Act: gọi `findNearbyRequests(request, pendingExpertUserId)`
3. Assert: throws `AccessDeniedException` với error code `MAP-204`

**Expected Result (PASS):**
```java
assertThatThrownBy(() -> service.findNearbyRequests(request, pendingExpertUserId))
    .isInstanceOf(AccessDeniedException.class)
    .hasMessageContaining("MAP-204");
```

**Expected Result (FAIL):**
- Method trả về danh sách bình thường cho expert chưa verified — vi phạm C4

**Current Status:** 🔴 Not written

---

### MAP150-TC-009 — distanceKm tính qua IMapProviderService, không tự viết Haversine

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbySupportRequestService.findNearbyRequests()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-MAP-205`

**Preconditions:**
- Mock `IMapProviderService.calculateHaversineDistance(...)` để trả giá trị cố định `1.8` (`FX-150-007`)

**Test Steps:**
1. Arrange: mock `IMapProviderService`
2. Act: gọi `findNearbyRequests()`
3. Assert: `mapProviderService.calculateHaversineDistance(...)` được gọi đúng 1 lần với đúng tham số (originLat/Lng = Expert's request lat/lng, destLat/Lng = snapshot's lat/lng); `item.getDistanceKm() == 1.8` (giá trị từ mock, không phải tính riêng)

**Expected Result (PASS):**
```java
verify(mapProviderService, times(1)).calculateHaversineDistance(
    eq(10.7769), eq(106.7009), eq(10.7769), eq(106.7009));
assertThat(item.getDistanceKm()).isEqualTo(1.8);
```

**Expected Result (FAIL):**
- `mapProviderService` không được gọi (0 interactions) — chứng tỏ code tự viết Haversine riêng, vi phạm C3

**Current Status:** 🔴 Not written

---

### MAP150-TC-010 — AF2: danh sách rỗng trả 200, không 404

**Severity:** `MEDIUM`
**Feature Under Test:** `GET /api/v1/map/support-requests/nearby`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportRequestControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `SRS AF2`

**Preconditions:**
- Mock repository trả danh sách rỗng (không có `emergency_events` OPEN trong bounding box)

**Test Steps:**
1. Arrange: mock trả `List.of()`
2. Act: gọi endpoint
3. Assert: `200 OK`, `items: []`

**Expected Result (PASS):**
- Status 200, `$.items` là mảng rỗng

**Expected Result (FAIL):**
- Status 404 — vi phạm C5

**Current Status:** 🔴 Not written

---

### MAP150-TC-011 — State transition: request đã accepted (selected_expert_id SET) KHÔNG xuất hiện

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportRequestService.findNearbyRequests()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportRequestServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS §6.3 State Machine (CONFIRMED by Product Owner 2026-07-02, xem §2 L3)`

**Preconditions:**
- `FX-150-002`: `emergency_events{status:'OPEN', selected_expert_id:<otherExpertId>}`

**Test Steps:**
1. Arrange: seed 1 event đã có `selected_expert_id` SET (accepted bởi Expert khác)
2. Act: gọi `findNearbyRequests()` với `expertUserId` khác với `selected_expert_id`
3. Assert: event đó KHÔNG xuất hiện trong `items`

**Expected Result (PASS):**
- `items` không chứa `emergencyEventId` của record đã accepted

**Expected Result (FAIL):**
- Record vẫn xuất hiện — vi phạm visibility gating (Open mechanism nhưng vẫn phải test theo suy luận TDS đã ghi)

**Current Status:** 🔴 Not written
**Implementation Note:** Cơ chế này đã được Product Owner CONFIRMED (2026-07-02, TDS §2 CHANGELOG) — không còn Open. Nếu UC141 owner sau này xác nhận `context_type` convention khác (Open Item riêng, không liên quan gating), chỉ TC-COND-002 cần re-verify, KHÔNG ảnh hưởng test case này.

---

### MAP150-TC-012 — Invalid coordinates → 400 MAP-201

**Severity:** `MEDIUM`
**Feature Under Test:** `GET /api/v1/map/support-requests/nearby`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportRequestControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `SRS E2`, `TDS §8.1 @DecimalMin/@DecimalMax`

**Test Steps:**
1. Act: gọi endpoint với `latitude=91.0` (ngoài phạm vi hợp lệ)
2. Assert: `400 Bad Request`, `error.code == "MAP-201"`

**Expected Result (PASS):** 400 với `MAP-201`
**Expected Result (FAIL):** 200 hoặc 500 — validation không hoạt động

**Current Status:** 🔴 Not written

---

### MAP150-TC-013 — Unauthenticated request → 401

**Severity:** `HIGH`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `GET /api/v1/map/support-requests/nearby`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportRequestControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** SRS E1

**Test Steps (Attack Simulation):**
1. Gọi endpoint không kèm `Authorization` header
2. Assert `401 Unauthorized`

**Expected Result (PASS = an toàn):** 401 `IAM-001`
**Expected Result (FAIL = lỗ hổng):** 200 — endpoint không được bảo vệ

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### MAP150-TC-INT-001 — Full flow: seed DB → API call → assert DB unchanged (read-only) + minimum-necessary

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: DB seed → GET /api/v1/map/support-requests/nearby → response assertion`
**Test File:** `src/test/java/com/carebridge/backend/map/NearbySupportRequestIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, 002, 003, 011`

**Preconditions:**
- PostgreSQL Testcontainer running (`@Testcontainers` auto-start)
- Flyway migration applied tự động khi Spring context start
- Seed: `FX-150-001`, `FX-150-002`, `FX-150-003`

**Test Steps:**
1. Seed `emergency_events` (1 OPEN unassigned + 1 OPEN assigned), `location_snapshots`, `expert_profiles` (VERIFIED)
2. Call `GET /api/v1/map/support-requests/nearby?latitude=10.7769&longitude=106.7009&radiusKm=5`
3. Assert response chỉ chứa event unassigned, KHÔNG chứa event đã assigned
4. Assert DB row count `emergency_events`/`location_snapshots` không đổi (read-only — no INSERT/UPDATE/DELETE ngoài event log)

**Expected Result (PASS):**
- Response 200, `items.size()==1`, DB row counts unchanged (trừ bảng audit event nếu có persist — hiện tại thiết kế dùng `ApplicationEventPublisher` in-process, không có bảng riêng)

**Expected Result (FAIL):**
- Response chứa cả 2 events, hoặc DB bị mutate ngoài ý muốn

**DB Assertion:**
```java
long emergencyEventCountBefore = emergencyEventRepository.count();
// ... call API ...
long emergencyEventCountAfter = emergencyEventRepository.count();
assertThat(emergencyEventCountAfter).isEqualTo(emergencyEventCountBefore);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MAP150-TC-001` | `NearbySupportRequestServiceTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-002` | `NearbySupportRequestServiceTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-003` | `NearbySupportRequestItemTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-004` | `NearbySupportRequestServiceTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-005` | `NearbySupportRequestControllerIntegrationTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-006` | `NearbySupportRequestServiceTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-007` | `NearbySupportRequestControllerTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-008` | `NearbySupportRequestServiceTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-009` | `NearbySupportRequestServiceTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-010` | `NearbySupportRequestControllerTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-011` | `NearbySupportRequestServiceTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-012` | `NearbySupportRequestControllerTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-013` | `NearbySupportRequestControllerTest.java` | `[ ]` | `—` | — |
| `MAP150-TC-INT-001` | `NearbySupportRequestIntegrationTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class NearbySupportRequestService implements INearbySupportRequestService {

    @Override
    public NearbySupportRequestListResponse findNearbyRequests(
            NearbySupportRequestSearchRequest request, UUID expertUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MAP150-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-003` | N/A (pure DTO reflection test — không gọi service) | 🔴 FAIL (class chưa có field constraint verified) | ☐ FAIL ☐ PASS | ☐ Tautology (nếu DTO stub rỗng, test có thể pass giả — verify DTO stub có ĐỦ field hợp lệ trừ field cấm) |
| `MAP150-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-005` | `throw('Not implemented')` (Controller trả 500) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-007` | Security filter chain (không phụ thuộc service stub) | 🔴 FAIL (nếu `@PreAuthorize` chưa cấu hình) | ☐ FAIL ☐ PASS | ☐ Security config đã có sẵn từ base project → có thể PASS sớm, verify không phải tautology |
| `MAP150-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-012` | Validation (Bean Validation, không phụ thuộc service) | 🔴 FAIL (nếu DTO validation chưa cấu hình) | ☐ FAIL ☐ PASS | |
| `MAP150-TC-013` | Security filter chain | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP150-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (điền khi implement)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MAP-IMP-003` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect — L3 (accept mechanism) đã CONFIRMED bởi Product Owner 2026-07-02 (RESOLVED); L2 (context_type convention) vẫn còn Open, cần xác nhận với UC141 owner
- [ ] `IMapProviderService` (UC129) đã tồn tại/sẵn sàng inject (mock được nếu chưa implement thật)
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `NearbySupportRequestService`
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] **MAP150-TC-003/004/005 (CRITICAL privacy tests) PASS 100%** — release-blocking nếu FAIL

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase (`IMapProviderService`, `IEmergencyEventRepository`, `ILocationSnapshotRepository`, `IExpertProfileRepository`)
- [ ] **Props Isolation** — không có shared mutable state giữa tests (dùng `NearbySupportRequestTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/TDS §)

### Suspension Criteria (Điều kiện tạm dừng)

- `IMapProviderService` (UC129) chưa sẵn sàng và không thể mock hợp lý
- UC141 chưa xác nhận `context_type` convention — rủi ro TC-COND-002 phải viết lại (TC-COND-011 KHÔNG còn rủi ro này — gating mechanism đã CONFIRMED 2026-07-02, xem TDS §2)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert implementation files (dev only — KHÔNG chạy trên production)
git checkout -- src/main/java/com/carebridge/backend/map/controller/NearbySupportRequestController.java
git checkout -- src/main/java/com/carebridge/backend/map/service/
git checkout -- src/main/java/com/carebridge/backend/map/dto/NearbySupportRequestSearchRequest.java
git checkout -- src/main/java/com/carebridge/backend/map/dto/NearbySupportRequestListResponse.java
git checkout -- src/main/java/com/carebridge/backend/map/dto/NearbySupportRequestItem.java
git checkout -- src/test/java/com/carebridge/backend/map/

# Không có migration để rollback (UC150 không tạo bảng mới)
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ (đã kiểm — mọi TC có Oracle Source) | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ (kiểm khi implement) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ (TC-COND-011 dựa trên gating mechanism đã CONFIRMED bởi Product Owner 2026-07-02 — không còn implicit) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ (Controller test chỉ verify HTTP status/JSON shape, không verify business rule) | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ (verify khi implement — `IEmergencyEventRepository` có thể đã tồn tại từ UC141, cần kiểm tra thực tế) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec-writing (chờ verify tại implementation)
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec — Draft. Chưa Approved. Gating mechanism (L3) đã RESOLVED (Confirmed by Product Owner 2026-07-02). Xem §2 (Logic Issues L1, L2, L4 còn Open) và §6 Suspension Criteria trước khi bắt đầu Red Phase.*
