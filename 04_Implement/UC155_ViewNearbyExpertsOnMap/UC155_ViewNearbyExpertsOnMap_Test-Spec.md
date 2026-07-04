# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC155 — View Nearby Experts on Map

**Document ID:** `CB-MAP-TDD-007`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.7.3` — Functional requirements (UC-155)
- `04_Implement/UC155_ViewNearbyExpertsOnMap/UC155_ViewNearbyExpertsOnMap_TDS.md` (`CB-MAP-IMP-007`) — Technical Specification
- `04_Implement/UC149_FindNearbyAvailableExperts/UC149_FindNearbyAvailableExperts_TDS.md` (`CB-MAP-IMP-005`) — data source owner
- `04_Implement/UC129_CalculateDistanceRouteAndETA/UC129_CalculateDistanceRouteAndETA_TDS.md` — `IMapProviderService` contract (indirect)
- PDPA / Luật 91/2025 — Legal basis for location PII handling

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC155 View Nearby Experts on Map |

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
| **Feature / Gap ID** | `UC155` |
| **Module** | `View Nearby Experts on Map — map bounded context` |
| **Spec gốc** | `CB-MAP-IMP-007` |
| **Priority** | 🟡 P2 (Medium — theo SRS Priority field) |
| **Sprint** | `TV4-Lâm ownership — Expert Consultation & Map/Location, sprint TBD` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` (vị trí Mother + vị trí Expert) |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `INearbyExpertService (UC149)` |
| **Downstream Consumers** | Mobile `nearbyExpert` map feature |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MAP-IMP-007 §17`, `ADR-MAP-301`, `ADR-MAP-302`, `ADR-MAP-303`, `ADR-MAP-304` |
| **Constraints Injected** | C1 (delegate to INearbyExpertService), C2 (pass-through accuracy/lat/lng), C3 (no duplicate calculateRoute call), C4 (userId from JWT), C5 (empty state = 200) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.7.3 dùng template chung, không chỉ rõ dữ liệu marker cần field gì | TDS `CB-MAP-IMP-007` §8.1 định nghĩa `NearbyExpertMapMarker` — test dùng đúng field set này, không bịa thêm | Test chỉ assert field đã liệt kê trong §8.1 |
| L2 | `NearbyExpertItem` (UC149) hiện KHÔNG có `latitude`/`longitude` — TDS §18 RG-7 đánh dấu Open, chọn Phương án A (mở rộng UC149's DTO) | Test giả định UC149's `NearbyExpertItem` ĐÃ được mở rộng thêm `latitude`/`longitude` tại thời điểm implement UC155 — nếu chưa, test PHẢI fail rõ ràng ở bước mapping (không silently trả null) | `MAP155-TC-002` assert marker có `latitude`/`longitude` non-null khi item nguồn có toạ độ |
| L3 | Không rõ UC155 có ghi `location_snapshots` riêng hay không | TDS ADR-MAP-304 quyết định KHÔNG ghi thêm — kế thừa nguyên hành vi ghi bên trong `findNearby()` (UC149) | Test verify `NearbyExpertMapService` KHÔNG tự inject `ILocationSnapshotRepository` — chỉ verify qua mock `INearbyExpertService` được gọi đúng 1 lần |
| L4 | Có nguy cơ AI tự viết lại query bounding-box/Haversine trong `NearbyExpertMapService` (trùng lặp UC149) | ADR-MAP-301 cấm tuyệt đối — test bắt buộc mock `INearbyExpertService` và verify không có `IExpertLocationShareRepository`/`IMapProviderService` nào được inject vào `NearbyExpertMapService` | `MAP155-TC-001` dùng mock verify + kiểm tra constructor signature của `NearbyExpertMapService` qua reflection (chỉ có 1 dependency) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
View Nearby Experts on Map bao gồm các layer:
├── Service (NearbyExpertMapService — mock INearbyExpertService với Mockito, KHÔNG mock repository/IMapProviderService vì UC155 không inject trực tiếp)
├── Mapper (NearbyExpertMapMarkerMapper — pure logic, no deps)
├── Controller (NearbyExpertMapController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, @SpringBootTest, full HTTP flow qua cả UC149 + UC155)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-155 §3.3.7.3` | Trigger, Normal Flow, AF1/AF2/AF3, E1/E2/E3, Business Rules BR-RBAC/BR-SAFETY |
| `CB-MAP-IMP-007 ADR-MAP-301` | Delegate hoàn toàn sang `INearbyExpertService.findNearby()` — không viết lại query |
| `CB-MAP-IMP-007 ADR-MAP-302` | Pass-through `accuracyMeters`/`latitude`/`longitude` |
| `CB-MAP-IMP-007 ADR-MAP-303` | Không gọi lại `calculateRoute()` — dùng ETA có sẵn |
| `CB-MAP-IMP-007 ADR-MAP-304` | RBAC ROLE_MOTHER, không double-audit `location_snapshots` |
| `CB-MAP-IMP-005` (UC149, data source contract) | Cấu trúc `NearbyExpertListResponse`/`NearbyExpertItem` làm input cho mapping |
| PDPA / BR-PRIVACY | Consistency giữa list view và map view — không leak expert bị lọc ở UC149 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | `NearbyExpertMapService` gọi `INearbyExpertService.findNearby()` đúng 1 lần với đúng request/userId, KHÔNG tự query DB | `NearbyExpertMapService.getMarkers()` | `MAP155-TC-001` |
| TC-COND-002 | Mỗi `NearbyExpertItem` được map đúng sang `NearbyExpertMapMarker` (field pass-through đầy đủ, kể cả `latitude`/`longitude`) | `NearbyExpertMapMarkerMapper` | `MAP155-TC-002` |
| TC-COND-003 | `accuracyMeters` trong marker = pass-through từ `NearbyExpertItem.accuracyMeters`, không làm tròn thêm | `NearbyExpertMapMarkerMapper` | `MAP155-TC-003` |
| TC-COND-004 | `estimatedTravelTimeMinutes`/`mapServiceDegraded` pass-through nguyên trạng, KHÔNG gọi thêm `IMapProviderService` | `NearbyExpertMapService.getMarkers()` | `MAP155-TC-004` |
| TC-COND-005 | `userId` lấy từ JWT SecurityContext, KHÔNG từ query param | `NearbyExpertMapController` | `MAP155-TC-005` |
| TC-COND-006 | UC155 KHÔNG tự ghi `location_snapshots` — chỉ có 1 lượt ghi (bên trong `findNearby()` của UC149) cho mỗi lượt gọi UC155 | `NearbyExpertMapService.getMarkers()` (integration) | `MAP155-TC-006`, `MAP155-TC-INT-002` |
| TC-COND-007 | Không có expert nào match → HTTP 200 với `markers:[]` (AF2), KHÔNG 404 | `NearbyExpertMapController` | `MAP155-TC-007` |
| TC-COND-008 | Marker list khớp 1:1 với UC149's item list cho cùng request params (consistency) | Full flow integration | `MAP155-TC-INT-001` |
| TC-COND-009 | Không có JWT / JWT sai role → 401/403 | `NearbyExpertMapController` (security filter) | `MAP155-TC-008`, `MAP155-TC-009` |
| TC-COND-010 | latitude/longitude invalid → HTTP 400 MAP-201 | `NearbyExpertMapController` | `MAP155-TC-010` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | latitude/longitude valid vs invalid range | Cùng phân vùng đã dùng ở UC149, tái sử dụng |
| Boundary Value Analysis | `accuracyMeters` = 0 vs rất lớn (fuzz threshold client-side, backend chỉ pass-through) | Đảm bảo backend không tự áp business rule mới ngoài phạm vi |
| Error Guessing | JWT thiếu, JWT sai role, mismatched marker vs list count | Security + consistency coverage |
| Mock Verification (Architecture Compliance) | `NearbyExpertMapService` chỉ có 1 dependency (`INearbyExpertService`) | Chống AP-AI-001 (viết lại query trùng UC149) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-155-001` | Mock | `INearbyExpertService.findNearby() → NearbyExpertListResponse(items=[item1], mapServiceDegraded=false)` với `item1` có `latitude=10.7769, longitude=106.7009, accuracyMeters=50.0, distanceKm=1.2, estimatedTravelTimeMinutes=6` | Happy path mapping |
| `FX-155-002` | Mock | `INearbyExpertService.findNearby() → NearbyExpertListResponse(items=[], mapServiceDegraded=false)` | Empty state (AF2) |
| `FX-155-003` | Mock | `INearbyExpertService.findNearby() → NearbyExpertListResponse(items=[itemDegraded], mapServiceDegraded=true)` với `itemDegraded.estimatedTravelTimeMinutes=null` | Degraded pass-through |
| `FX-155-004` | JWT | `{ sub: 'mother-001', role: 'ROLE_MOTHER' }` | Auth context hợp lệ |
| `FX-155-005` | JWT | `{ sub: 'expert-001', role: 'ROLE_EXPERT' }` | Auth context sai role (403 test) |
| `FX-155-006` | DB seed (integration) | Giống hệt `FX-149-001` + `FX-149-003` (UC149's fixture) — tái sử dụng để đảm bảo consistency test | `MAP155-TC-INT-001` |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// NearbyExpertMapTestFactory.java
class NearbyExpertMapTestFactory {

    static final UUID VERIFIED_EXPERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID MOTHER_USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000201");

    static NearbyExpertItem makeNearbyExpertItem() {
        NearbyExpertItem item = new NearbyExpertItem();
        item.setExpertProfileId(VERIFIED_EXPERT_ID);
        item.setSpecialty("Pediatrics");
        item.setProfessionalTitle("BS. Nguyễn Văn A");
        item.setRatingAvg(new BigDecimal("4.8"));
        item.setVerificationStatus("VERIFIED");
        item.setAvailabilityStatus("AVAILABLE");
        item.setDistanceKm(1.2);
        item.setAccuracyMeters(50.0);
        item.setEstimatedTravelTimeMinutes(6);
        item.setLatitude(new BigDecimal("10.7769"));   // xem TDS §18 RG-7 — field mở rộng
        item.setLongitude(new BigDecimal("106.7009"));
        return item;
    }

    static NearbyExpertItem makeNearbyExpertItem(Consumer<NearbyExpertItem> overrides) {
        NearbyExpertItem item = makeNearbyExpertItem();
        overrides.accept(item);
        return item;
    }

    static NearbyExpertListResponse makeHappyResponse() {
        return new NearbyExpertListResponse(List.of(makeNearbyExpertItem()), false);
    }

    static NearbyExpertListResponse makeEmptyResponse() {
        return new NearbyExpertListResponse(List.of(), false);
    }

    static NearbyExpertListResponse makeDegradedResponse() {
        NearbyExpertItem degraded = makeNearbyExpertItem(i -> i.setEstimatedTravelTimeMinutes(null));
        return new NearbyExpertListResponse(List.of(degraded), true);
    }

    static NearbyExpertSearchRequest makeSearchRequest() {
        NearbyExpertSearchRequest r = new NearbyExpertSearchRequest();
        r.setLatitude(10.7769);
        r.setLongitude(106.7009);
        r.setRadiusKm(5.0);
        r.setMaxResults(20);
        return r;
    }
}
```

---

### MAP155-TC-001 — NearbyExpertMapService chỉ gọi INearbyExpertService, KHÔNG tự query DB

**Severity:** `CRITICAL`
**Feature Under Test:** `NearbyExpertMapService.getMarkers()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyExpertMapServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-MAP-IMP-007 ADR-MAP-301 (Proposed)`

**Preconditions:**
- Mock `INearbyExpertService` trả `FX-155-001` (happy response)

**Test Steps:**
1. Arrange: mock `INearbyExpertService.findNearby(any(), any())` trả `makeHappyResponse()`
2. Act: gọi `nearbyExpertMapService.getMarkers(makeSearchRequest(), MOTHER_USER_ID)`
3. Assert: `verify(nearbyExpertService, times(1)).findNearby(eq(request), eq(MOTHER_USER_ID))`; kiểm tra qua reflection rằng `NearbyExpertMapService` constructor chỉ nhận `INearbyExpertService` (không có `IExpertLocationShareRepository`/`IMapProviderService`/`ILocationSnapshotRepository` nào khác)

**Expected Result (PASS — hành vi đúng):**
- Mock được gọi đúng 1 lần; constructor field chỉ chứa `INearbyExpertService`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Service gọi thêm repository/IMapProviderService khác — vi phạm ADR-MAP-301 (viết lại query trùng UC149)

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test kiểm tra ARCHITECTURE COMPLIANCE — bắt buộc chặn AP-AI-001.

---

### MAP155-TC-002 — Mapping NearbyExpertItem sang NearbyExpertMapMarker đầy đủ field

**Severity:** `HIGH`
**Feature Under Test:** `NearbyExpertMapMarkerMapper`
**Test File:** `src/test/java/com/carebridge/backend/map/mapper/NearbyExpertMapMarkerMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-MAP-IMP-007 §8.1 (Proposed)` / `CB-MAP-IMP-007 §18 RG-7`

**Preconditions:**
- `FX-155-001` — `NearbyExpertItem` với đầy đủ field bao gồm `latitude`/`longitude`

**Test Steps:**
1. Arrange: `NearbyExpertItem item = makeNearbyExpertItem()`
2. Act: `NearbyExpertMapMarker marker = mapper.toMarker(item)`
3. Assert: tất cả field khớp 1:1 (`expertProfileId`, `specialty`, `professionalTitle`, `ratingAvg`, `availabilityStatus`, `latitude`, `longitude`, `accuracyMeters`, `distanceKm`, `estimatedTravelTimeMinutes`)

**Expected Result (PASS):**
- Mọi field marker khớp chính xác với item nguồn — không mất dữ liệu, không đổi giá trị

**Expected Result (FAIL):**
- Field bị null/sai giá trị, đặc biệt `latitude`/`longitude` null (dấu hiệu UC149's `NearbyExpertItem` chưa được mở rộng theo RG-7)

**Current Status:** 🔴 Not written
**Implementation Note:** Nếu test này FAIL vì `item.getLatitude()` không tồn tại (compile error), đó là tín hiệu RG-7 chưa được UC149 owner xác nhận — dừng lại, không tự ý sửa UC149.

---

### MAP155-TC-003 — accuracyMeters pass-through, không làm tròn thêm

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyExpertMapMarkerMapper`
**Test File:** `src/test/java/com/carebridge/backend/map/mapper/NearbyExpertMapMarkerMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-MAP-IMP-007 ADR-MAP-302 (Proposed)`

**Preconditions:**
- `FX-155-001` với `accuracyMeters = 50.0`

**Test Steps:**
1. Arrange: item với `accuracyMeters = 50.0`
2. Act: map sang marker
3. Assert: `marker.getAccuracyMeters() == 50.0` chính xác

**Expected Result (PASS):**
- Giá trị khớp chính xác, không làm tròn/thay đổi

**Expected Result (FAIL):**
- Giá trị bị làm tròn hoặc thay đổi (vi phạm ADR-MAP-302)

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### MAP155-TC-004 — estimatedTravelTimeMinutes/mapServiceDegraded pass-through, không gọi lại calculateRoute

**Severity:** `HIGH`
**Feature Under Test:** `NearbyExpertMapService.getMarkers()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyExpertMapServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-MAP-IMP-007 ADR-MAP-303 (Proposed)`

**Preconditions:**
- `FX-155-003` — degraded response từ mock `INearbyExpertService`

**Test Steps:**
1. Arrange: mock `findNearby()` trả `makeDegradedResponse()` (`mapServiceDegraded=true`, item có `estimatedTravelTimeMinutes=null`)
2. Act: gọi `getMarkers(makeSearchRequest(), MOTHER_USER_ID)`
3. Assert: response `mapServiceDegraded == true`; `markers.get(0).getEstimatedTravelTimeMinutes() == null`; verify KHÔNG có `IMapProviderService` nào được mock/inject (kiểm tra constructor — đã cover ở TC-001, test này chỉ cần assert giá trị pass-through)

**Expected Result (PASS):**
- Giá trị degraded truyền qua nguyên trạng, không tính toán lại

**Expected Result (FAIL):**
- Service tự gọi thêm route calculation (vi phạm ADR-MAP-303), hoặc mất field `mapServiceDegraded`

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### MAP155-TC-005 — userId lấy từ JWT SecurityContext, không từ query param

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `NearbyExpertMapController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertMapControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-MAP-IMP-007 ADR-MAP-304 (Proposed)`

**Preconditions:**
- JWT hợp lệ `FX-155-004` với `sub=mother-001`

**Test Steps:**
1. Arrange: `@WebMvcTest` với mock `INearbyExpertMapService`; JWT có `sub=mother-001`
2. Act: `GET /api/v1/map/experts/nearby/markers?latitude=10.77&longitude=106.70&userId=00000000-0000-0000-0000-000000009999` (userId giả mạo trong query param)
3. Assert: `verify(nearbyExpertMapService).getMarkers(any(), eq(mother-001-userId))` — controller PHẢI dùng userId từ JWT

**Expected Result (PASS):**
- Service được gọi với userId từ SecurityContext, KHÔNG phải giá trị query param

**Expected Result (FAIL):**
- Service nhận userId từ query param (lỗ hổng giả mạo identity)

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### MAP155-TC-006 — UC155 không tự ghi location_snapshots, chỉ 1 lượt ghi qua UC149

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyExpertMapService.getMarkers()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyExpertMapServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-MAP-IMP-007 ADR-MAP-304 (Proposed)`

**Preconditions:**
- Mock `INearbyExpertService` (không mock `ILocationSnapshotRepository` — không nên tồn tại trong dependency graph của `NearbyExpertMapService`)

**Test Steps:**
1. Arrange: khởi tạo `NearbyExpertMapService` chỉ với mock `INearbyExpertService`
2. Act: gọi `getMarkers(makeSearchRequest(), MOTHER_USER_ID)`
3. Assert: không có lỗi biên dịch/injection nào phát sinh từ việc thiếu `ILocationSnapshotRepository` — xác nhận `NearbyExpertMapService` không có field đó (reflection hoặc compile-time constructor check)

**Expected Result (PASS):**
- Service khởi tạo thành công chỉ với 1 dependency

**Expected Result (FAIL):**
- Service yêu cầu thêm `ILocationSnapshotRepository` (double-audit, vi phạm ADR-MAP-304 Phương án A)

**Current Status:** 🔴 Not written
**Implementation Note:** Verify đầy đủ ở mức integration test (`MAP155-TC-INT-002`) bằng cách đếm số row `location_snapshots` sau khi gọi UC155.

---

### MAP155-TC-007 — Empty state trả HTTP 200 với markers:[], không 404

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyExpertMapController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertMapControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `SRS §3.3.7.3 AF2`

**Preconditions:**
- Mock Service trả `NearbyExpertMapResponse(markers=[], mapServiceDegraded=false)`

**Test Steps:**
1. Arrange: mock `INearbyExpertMapService.getMarkers()` trả response rỗng
2. Act: `GET /api/v1/map/experts/nearby/markers?latitude=10.77&longitude=106.70`
3. Assert: HTTP status `200`; response body `{"markers":[],"mapServiceDegraded":false}`

**Expected Result (PASS):**
- Status 200, body `markers` là mảng rỗng

**Expected Result (FAIL):**
- Status 404

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### SECURITY TEST CASES

---

### MAP155-TC-008 — Không có JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** `BR-RBAC`
**Feature Under Test:** `Spring Security filter chain trên /api/v1/map/experts/nearby/markers`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertMapControllerSecurityIT.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Không có Authorization header

**Test Steps (Attack Simulation):**
1. Gửi `GET /api/v1/map/experts/nearby/markers?latitude=10.77&longitude=106.70` KHÔNG kèm `Authorization` header
2. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- `401 Unauthorized`, body `error.code == "IAM-001"`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Request được xử lý, trả marker mà không cần auth

**Current Status:** 🔴 Not written

---

### MAP155-TC-009 — JWT hợp lệ nhưng sai role (ROLE_EXPERT) → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-863 — Incorrect Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')") trên NearbyExpertMapController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertMapControllerSecurityIT.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- JWT hợp lệ `FX-155-005` với `role=ROLE_EXPERT`

**Test Steps (Attack Simulation):**
1. Gửi `GET /api/v1/map/experts/nearby/markers?latitude=10.77&longitude=106.70` với JWT role `ROLE_EXPERT`
2. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden`, body `error.code == "MAP-204"`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Request được xử lý bình thường cho role không hợp lệ

**Current Status:** 🔴 Not written

---

### MAP155-TC-010 — Invalid latitude/longitude trả 400 MAP-201

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyExpertMapController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertMapControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-MAP-IMP-007 §9.2, §10 MAP-201`

**Preconditions:**
- Không cần seed DB

**Test Steps:**
1. Act: `GET /api/v1/map/experts/nearby/markers?latitude=999&longitude=106.70`
2. Assert: HTTP `400`, body `error.code == "MAP-201"`

**Expected Result (PASS):**
- 400 với error code chính xác

**Expected Result (FAIL):**
- Request được chấp nhận hoặc trả sai error code

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### INTEGRATION TEST CASES

---

### MAP155-TC-INT-001 — Marker list khớp 1:1 với UC149's item list (consistency)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request → NearbyExpertMapController → NearbyExpertMapService → INearbyExpertService (UC149) → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/map/NearbyExpertMapIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Seed: `FX-155-006` (giống hệt UC149's `FX-149-001` + `FX-149-003`) — 1 expert VERIFIED + active share

**Test Steps:**
1. Seed 1 expert VERIFIED + active location share trong bán kính
2. Gọi cả `GET /api/v1/map/experts/nearby` (UC149) và `GET /api/v1/map/experts/nearby/markers` (UC155) với cùng params
3. Assert `items` (UC149) và `markers` (UC155) chứa cùng `expertProfileId` set, cùng `distanceKm`, cùng `accuracyMeters`

**Expected Result (PASS):**
- 2 response nhất quán 100% cho cùng dữ liệu — không có expert nào chỉ xuất hiện ở 1 trong 2 view

**Expected Result (FAIL):**
- Danh sách lệch nhau (dấu hiệu UC155 có filter khác UC149 — vi phạm ADR-MAP-301)

**DB Assertion:**
```java
// Không cần DB assertion riêng — assertion chính là so sánh 2 HTTP response
```

**Current Status:** 🔴 Not written

---

### MAP155-TC-INT-002 — Gọi UC155 chỉ tạo 1 location_snapshots row (không double-write)

**Severity:** `MEDIUM`
**Feature Under Test:** `Full flow — audit side-effect`
**Test File:** `src/test/java/com/carebridge/backend/map/NearbyExpertMapIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Preconditions:**
- PostgreSQL container running; seed `FX-155-006`

**Test Steps:**
1. Đếm số row `location_snapshots` với `context_type='NEARBY_EXPERT_SEARCH'` TRƯỚC khi gọi
2. Gọi `GET /api/v1/map/experts/nearby/markers` 1 lần
3. Đếm lại số row SAU khi gọi

**Expected Result (PASS):**
- Số row tăng đúng **1** (không phải 2) — chứng minh UC155 không tự ghi thêm ngoài lượt ghi bên trong `findNearby()` của UC149

**Expected Result (FAIL):**
- Số row tăng 2 hoặc 0 (thiếu/ghi trùng)

**DB Assertion:**
```java
long before = locationSnapshotRepository.countByContextType("NEARBY_EXPERT_SEARCH");
// ... call endpoint ...
long after = locationSnapshotRepository.countByContextType("NEARBY_EXPERT_SEARCH");
assertThat(after - before).isEqualTo(1L);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MAP155-TC-001` | `NearbyExpertMapServiceTest.java` | `[ ]` | `—` | — |
| `MAP155-TC-002` | `NearbyExpertMapMarkerMapperTest.java` | `[ ]` | `—` | — |
| `MAP155-TC-003` | `NearbyExpertMapMarkerMapperTest.java` | `[ ]` | `—` | — |
| `MAP155-TC-004` | `NearbyExpertMapServiceTest.java` | `[ ]` | `—` | — |
| `MAP155-TC-005` | `NearbyExpertMapControllerTest.java` | `[ ]` | `—` | — |
| `MAP155-TC-006` | `NearbyExpertMapServiceTest.java` | `[ ]` | `—` | — |
| `MAP155-TC-007` | `NearbyExpertMapControllerTest.java` | `[ ]` | `—` | — |
| `MAP155-TC-008` | `NearbyExpertMapControllerSecurityIT.java` | `[ ]` | `—` | — |
| `MAP155-TC-009` | `NearbyExpertMapControllerSecurityIT.java` | `[ ]` | `—` | — |
| `MAP155-TC-010` | `NearbyExpertMapControllerTest.java` | `[ ]` | `—` | — |
| `MAP155-TC-INT-001` | `NearbyExpertMapIntegrationTest.java` | `[ ]` | `—` | — |
| `MAP155-TC-INT-002` | `NearbyExpertMapIntegrationTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class NearbyExpertMapService implements INearbyExpertMapService {

    @Override
    public NearbyExpertMapResponse getMarkers(NearbyExpertSearchRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MAP155-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `MAP155-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-008` | Security filter not configured | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-009` | Security filter not configured | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-010` | Validation stub absent | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP155-TC-INT-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (điền khi implement)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MAP-IMP-007` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm, đặc biệt L2 (RG-7 — mở rộng `NearbyExpertItem` của UC149)
- [ ] UC149 (`INearbyExpertService`) đã implement và deploy — dependency bắt buộc
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `NearbyExpertMapService`
- [ ] Không có business logic trong `NearbyExpertMapController` (chỉ có validation + mapping)
- [ ] Verify `NearbyExpertMapService` chỉ có 1 dependency (`INearbyExpertService`) — không có repository/IMapProviderService trực tiếp

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase
- [ ] **Props Isolation** — không có shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria (Điều kiện tạm dừng)

- UC149 (`INearbyExpertService`) chưa deploy/available
- RG-7 (mở rộng `NearbyExpertItem`) chưa được UC149 owner xác nhận
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Không có migration mới cho Draft này — không cần revert schema

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/map/controller/NearbyExpertMapController.java
git checkout -- src/main/java/com/carebridge/backend/map/service/impl/NearbyExpertMapService.java
git checkout -- src/main/java/com/carebridge/backend/map/dto/response/NearbyExpertMapResponse.java
git checkout -- src/main/java/com/carebridge/backend/map/dto/response/NearbyExpertMapMarker.java
git checkout -- src/main/java/com/carebridge/backend/map/mapper/NearbyExpertMapMarkerMapper.java
git checkout -- src/test/java/com/carebridge/backend/map/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ verify khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — MAP155-TC-002 đánh dấu rõ dependency trên RG-7 | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — Controller tests chỉ verify HTTP mapping/security | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — mọi type tham chiếu §8 TDS `CB-MAP-IMP-007` hoặc UC149 `CB-MAP-IMP-005` | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (chờ Red Gate verify khi implement) → TDD spec approved cho giai đoạn Draft

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none tại thời điểm viết Test-Spec)_ | — | — | — | — |

---

*TDD Spec v1.0 — Draft. Chưa Approved. Chờ TDS `CB-MAP-IMP-007` Approved trước khi bắt đầu Red Phase.*
