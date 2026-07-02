# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC63 — Find Nearby Care Facility

**Document ID:** `CB-MAP-IMP-001-TEST`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC63_FindNearbyCareFacility/UC63_FindNearbyCareFacility_TDS.md` (CB-MAP-IMP-001)
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.1.40`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-01 | AI Agent — Tech Lead | Khởi tạo tài liệu — Test-Spec cho UC63 Find Nearby Care Facility |

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
| **Feature / Gap ID** | `CB-MAP-IMP-001` |
| **Module** | `Find Nearby Care Facility — map` |
| **Spec gốc** | `CB-MAP-IMP-001` |
| **Priority** | 🟠 P1 *(SRS Priority = High, không phải Critical như UC65)* |
| **Sprint** | `S? — Open (chưa gán sprint cụ thể trong task allocation doc)` |
| **Milestone** | `Open` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IAM (JWT ROLE_MOTHER)`, `care_facilities`, `location_snapshots`, `TrackAsia Map Service (external, optional path)` |
| **Downstream Consumers** | `UC64 Quick Call or Navigate` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MAP-IMP-001 §17`, `ADR-MAP-001/002/003/004` |
| **Constraints Injected** | C1 (DB-first search), C2 (TrackAsia timeout/fallback), C3 (best-effort snapshot), C4 (JWT userId), C5 (empty state = 200) |
| **Model** | `claude-sonnet-5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.1.40 là văn bản template chung, không có số cụ thể cho bán kính/max results/timeout | TDS đề xuất `radiusKm=5.0` default, `maxResults=20`, TrackAsia timeout 3000ms (đánh dấu Open trong TDS §2, §4) | Test dùng giá trị default TDS làm oracle **tạm thời**; TC ghi rõ `Oracle Source: TDS ADR-MAP-00x (Proposed, chưa Approved)` — không giả định đây là AC cứng |
| L2 | SRS không nói rõ hành vi khi TrackAsia lỗi có làm fail toàn bộ request hay không | ADR-MAP-003: KHÔNG được fail — trả 200 kèm `mapServiceDegraded:true` | Test verify response luôn 200 khi TrackAsia timeout, KHÔNG 5xx |
| L3 | SRS AF2 nói "empty state" nhưng không chỉ rõ HTTP status | Team convention (theo UC62/65 style + REST best practice): empty result set = 200 với mảng rỗng, không phải 404 | Test verify HTTP 200 + `items: []` khi bounding-box không có facility nào |
| L4 | `care_facilities.facility_type` là `varchar(50)` tự do, không phải enum ràng buộc trong DB (không có CHECK constraint theo V1__init_schema.sql) | Test KHÔNG giả định enum cố định (HOSPITAL/CLINIC/...) — chỉ test filter theo string match chính xác | Test dùng giá trị filter tùy ý do fixture định nghĩa, không hardcode enum values ngoài schema |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC63 Find Nearby Care Facility bao gồm các layer:
├── Domain (Haversine distance calculation — pure logic, no deps)
├── Service (NearbyFacilityService — mock ICareFacilityRepository, ILocationSnapshotRepository, TrackAsiaMapClient với Mockito)
├── Controller (NearbyFacilityController — @WebMvcTest, mock Service)
└── Integration (Testcontainers PostgreSQL — verify care_facilities query + location_snapshots persistence)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-63 §3.3.1.40` | Mother tìm cơ sở gần bằng vị trí hiện tại; AF1 cancel; AF2 empty state; E1 unauthorized; E2 invalid data; E3 external service failure |
| `SRS UC-129 §3.1.3.1` | Map/location là capability dùng chung — không cần logic riêng cho ETA ngoài TrackAsia client |
| `ADR-MAP-001` | DB-first bounding-box + Haversine search |
| `ADR-MAP-002` | Location snapshot best-effort write |
| `ADR-MAP-003` | TrackAsia timeout 3000ms + 1 retry, fallback không fail request |
| `ADR-MAP-004` | ROLE_MOTHER only, userId từ JWT |
| BR-RBAC / BR-PRIVACY | Role check + location PII minimum-necessary handling |
| `V1__init_schema.sql` | `care_facilities`, `location_snapshots` cấu trúc cột, FK, index — oracle cho persistence assertions |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — trả danh sách facility sắp xếp theo khoảng cách tăng dần | `NearbyFacilityService.findNearby()` | `MAP-TC-001` |
| TC-COND-002 | Search KHÔNG gọi TrackAsia trước khi query DB (ADR-MAP-001 — search chính độc lập TrackAsia) | `NearbyFacilityService.findNearby()` | `MAP-TC-002` |
| TC-COND-003 | Ghi `location_snapshots` best-effort — lỗi khi ghi KHÔNG làm fail response | `NearbyFacilityService.findNearby()` | `MAP-TC-003` |
| TC-COND-004 | TrackAsia timeout → response vẫn 200, `mapServiceDegraded=true`, `estimatedTravelTimeMinutes=null` | `TrackAsiaMapClient` error handling | `MAP-TC-004` |
| TC-COND-005 | TrackAsia thành công → `estimatedTravelTimeMinutes` populated, `mapServiceDegraded=false` | `TrackAsiaMapClient` happy path | `MAP-TC-005` |
| TC-COND-006 | RBAC — ROLE_MOTHER only; role khác → 403 MAP-004 | `NearbyFacilityController` auth | `MAP-TC-006` |
| TC-COND-007 | Empty state — bounding-box không có facility nào → 200 với `items:[]` | Controller + Service | `MAP-TC-007` |
| TC-COND-008 | Invalid params — latitude/longitude thiếu hoặc ngoài range → 400 MAP-001 | Controller validation | `MAP-TC-008` |
| TC-COND-009 | Không có JWT → 401 IAM-001 | Security filter chain | `MAP-TC-009` |
| TC-COND-010 | `facilityType` filter áp dụng đúng — chỉ trả facility khớp type khi filter được truyền | `ICareFacilityRepository.findWithinBoundingBox()` | `MAP-TC-010` |
| TC-COND-011 | `maxResults` giới hạn đúng số lượng kết quả trả về | `NearbyFacilityService.findNearby()` | `MAP-TC-011` |
| TC-COND-012 | userId dùng để ghi snapshot PHẢI lấy từ JWT/SecurityContext, không phải từ request body/param | `NearbyFacilityController` | `MAP-TC-012` |
| TC-COND-013 (Integration) | Full flow API → DB — facility trả về khớp dữ liệu seed trong Testcontainers | End-to-end | `MAP-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | radiusKm (trong/ngoài phạm vi 0.1-50), facilityType (match/no-match/null) | Cover input domain hợp lệ/không hợp lệ |
| Boundary Value Analysis | latitude/longitude tại ±90/±180; maxResults tại 1/50/51 | Biên giá trị hợp lệ theo `@DecimalMin/@Max` |
| Error Guessing | TrackAsia timeout, TrackAsia throws, DB connection lỗi | External-service-failure paths — trọng tâm vì emergency-adjacent |
| State-less verification | Không có state machine cho UC63 (read-only) — bỏ qua State Transition Testing | UC63 không có entity trạng thái |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `care_facilities { name:'Bệnh viện Test A', lat:10.7769, lng:106.7009, facilityType:'HOSPITAL', verificationStatus:'VERIFIED' }` | Happy path facility gần |
| `FX-002` | DB seed | `care_facilities { name:'Phòng khám Test B', lat:10.9000, lng:106.9000, facilityType:'CLINIC' }` | Facility ngoài bounding-box (không match) |
| `FX-003` | Mock | `TrackAsiaMapClient.estimateRoute() → RouteEstimate(1.8, 7)` | TrackAsia thành công |
| `FX-004` | Mock | `TrackAsiaMapClient.estimateRoute() → throws TrackAsiaTimeoutException` | TrackAsia timeout/fallback |
| `FX-005` | Mock | `ILocationSnapshotRepository.save() → throws DataAccessException` | Best-effort write failure |
| `FX-006` | JWT | `{ sub: 'mother-001', role: 'ROLE_MOTHER' }` | Auth context hợp lệ |
| `FX-007` | JWT | `{ sub: 'partner-001', role: 'ROLE_PARTNER' }` | Auth context sai role |
| `FX-008` | Request | `{ latitude: 10.7769, longitude: 106.7009, radiusKm: 5.0, maxResults: 20 }` | Baseline request hợp lệ |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

class NearbyFacilityTestFactory {

    static CareFacility makeFacility() {
        return makeFacility(f -> {});
    }

    static CareFacility makeFacility(Consumer<CareFacility> overrides) {
        CareFacility facility = new CareFacility();
        facility.setFacilityId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        facility.setName("Bệnh viện Test A");
        facility.setFacilityType("HOSPITAL");
        facility.setAddress("123 Test St");
        facility.setLatitude(new BigDecimal("10.7769"));
        facility.setLongitude(new BigDecimal("106.7009"));
        facility.setPhone("+84000000000");
        facility.setVerificationStatus("VERIFIED");
        overrides.accept(facility);
        return facility;
    }

    static NearbyFacilitySearchRequest makeRequest() {
        return makeRequest(r -> {});
    }

    static NearbyFacilitySearchRequest makeRequest(Consumer<NearbyFacilitySearchRequest> overrides) {
        NearbyFacilitySearchRequest request = new NearbyFacilitySearchRequest();
        request.setLatitude(10.7769);
        request.setLongitude(106.7009);
        request.setRadiusKm(5.0);
        request.setMaxResults(20);
        overrides.accept(request);
        return request;
    }

    static UUID makeMotherUserId() {
        return UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    }
}
```

---

### MAP-TC-001 — Happy path: trả danh sách facility sắp xếp theo khoảng cách tăng dần

**Severity:** `HIGH`
**Feature Under Test:** `NearbyFacilityService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyFacilityServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-MAP-001 (TDS §3, Proposed)`

**Preconditions:**
- Mock `ICareFacilityRepository.findWithinBoundingBox()` trả về 2 facility với khoảng cách khác nhau (FX-001, một facility thứ hai gần hơn)
- Fixture: `FX-001`, `FX-006`

**Test Steps:**
1. Arrange: mock repository trả 2 CareFacility với lat/lng khác nhau
2. Act: gọi `findNearby(makeRequest(), makeMotherUserId())`
3. Assert: `response.getItems()` sắp xếp theo `distanceKm` tăng dần

**Expected Result (PASS):**
- `items.get(0).distanceKm() <= items.get(1).distanceKm()`

**Expected Result (FAIL):**
- Danh sách không sắp xếp hoặc thiếu field `distanceKm`

**Current Status:** 🔴 Not written

---

### MAP-TC-002 — Search KHÔNG gọi TrackAsia trước khi query DB

**Severity:** `HIGH`
**Feature Under Test:** `NearbyFacilityService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyFacilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-MAP-001`

**Preconditions:**
- Mock `ICareFacilityRepository` và `TrackAsiaMapClient` với Mockito `InOrder` verification

**Test Steps:**
1. Arrange: mock cả 2 dependency
2. Act: gọi `findNearby(...)`
3. Assert: `InOrder` verify `facilityRepository.findWithinBoundingBox()` được gọi TRƯỚC `trackAsiaMapClient.estimateRoute()`

**Expected Result (PASS):**
- Thứ tự gọi đúng: repository trước, TrackAsia sau (hoặc TrackAsia không gọi nếu không cần ETA)

**Expected Result (FAIL):**
- TrackAsia được gọi trước hoặc là điều kiện tiên quyết để query DB

**Current Status:** 🔴 Not written

---

### MAP-TC-003 — Ghi location_snapshots thất bại KHÔNG làm fail response

**Severity:** `CRITICAL`
**Feature Under Test:** `NearbyFacilityService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyFacilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-MAP-002`

**Preconditions:**
- Mock `ILocationSnapshotRepository.save()` → `FX-005` (throws DataAccessException)

**Test Steps:**
1. Arrange: mock snapshot repo throws exception; mock facility repo trả FX-001
2. Act: gọi `findNearby(...)`
3. Assert: method KHÔNG throw exception; response vẫn trả `items` hợp lệ

**Expected Result (PASS):**
- Response 200 với facility list đầy đủ, exception từ snapshot write bị nuốt/log, không lan truyền

**Expected Result (FAIL):**
- Exception từ snapshot write lan ra ngoài, làm fail toàn bộ request

**Current Status:** 🔴 Not written
**Implementation Note:** Dùng try-catch quanh lời gọi `locationSnapshotRepository.save()`, log warning, không rethrow.

---

### MAP-TC-004 — TrackAsia timeout → response 200, mapServiceDegraded=true

**Severity:** `CRITICAL`
**Feature Under Test:** `NearbyFacilityService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyFacilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-MAP-003`

**Preconditions:**
- Mock `TrackAsiaMapClient.estimateRoute()` → `FX-004` (throws timeout exception)
- Mock facility repo trả `FX-001`

**Test Steps:**
1. Arrange: mock TrackAsia throws timeout
2. Act: gọi `findNearby(...)`
3. Assert: response `mapServiceDegraded == true`, `items[0].estimatedTravelTimeMinutes == null`, `items[0].distanceKm` vẫn có giá trị (Haversine)

**Expected Result (PASS):**
- HTTP-equivalent 200, không throw, `distanceKm` vẫn chính xác

**Expected Result (FAIL):**
- Method throws exception ra ngoài, hoặc response thiếu `items`

**Current Status:** 🔴 Not written

---

### MAP-TC-005 — TrackAsia thành công → estimatedTravelTimeMinutes populated

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyFacilityService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyFacilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MAP-003`

**Preconditions:**
- Mock `TrackAsiaMapClient.estimateRoute()` → `FX-003` (RouteEstimate(1.8, 7))

**Test Steps:**
1. Arrange: mock TrackAsia trả RouteEstimate hợp lệ
2. Act: gọi `findNearby(...)`
3. Assert: `items[0].estimatedTravelTimeMinutes == 7`, `mapServiceDegraded == false`

**Expected Result (PASS):**
- Field populated đúng giá trị mock

**Expected Result (FAIL):**
- `mapServiceDegraded == true` dù TrackAsia thành công, hoặc field null

**Current Status:** 🔴 Not written

---

### MAP-TC-006 — RBAC: role khác ROLE_MOTHER → 403 MAP-004

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `NearbyFacilityController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyFacilityControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-MAP-004`

**Preconditions:**
- JWT `FX-007` (ROLE_PARTNER)

**Test Steps:**
1. Arrange: `@WebMvcTest` với mock Service, JWT ROLE_PARTNER
2. Act: `GET /api/v1/map/facilities/nearby?latitude=10.77&longitude=106.70`
3. Assert: response 403, body chứa `error.code == "MAP-004"`

**Expected Result (PASS — hệ thống an toàn):**
- 403 Forbidden

**Expected Result (FAIL = lỗ hổng tồn tại):**
- 200 OK trả dữ liệu cho role không được phép

**Current Status:** 🔴 Not written

---

### MAP-TC-007 — Empty state: bounding-box không có facility → 200 items:[]

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyFacilityService.findNearby()` + Controller
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyFacilityControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `SRS UC-63 AF2`

**Preconditions:**
- Mock repository trả `List.of()` (danh sách rỗng)

**Test Steps:**
1. Arrange: mock facility repo trả rỗng
2. Act: `GET /api/v1/map/facilities/nearby?latitude=0&longitude=0`
3. Assert: HTTP 200, body `{"items": [], "mapServiceDegraded": false}`

**Expected Result (PASS):**
- 200 với mảng rỗng, KHÔNG phải 404

**Expected Result (FAIL):**
- 404 trả về, hoặc 200 nhưng thiếu field `items`

**Current Status:** 🔴 Not written

---

### MAP-TC-008 — Invalid params: latitude ngoài range → 400 MAP-001

**Severity:** `HIGH`
**Feature Under Test:** `NearbyFacilityController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyFacilityControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `SRS UC-63 E2`, `TDS §8.1 @DecimalMin/@DecimalMax`

**Preconditions:** Không cần mock Service (validation fail trước khi vào Service)

**Test Steps:**
1. Act: `GET /api/v1/map/facilities/nearby?latitude=999&longitude=106.70`
2. Assert: 400, `error.code == "MAP-001"`

**Expected Result (PASS):**
- 400 Bad Request với error code MAP-001

**Expected Result (FAIL):**
- Request được xử lý tiếp xuống Service dù latitude không hợp lệ

**Current Status:** 🔴 Not written

---

### MAP-TC-009 — Không có JWT → 401 IAM-001

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyFacilityControllerTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** Không có JWT header

**Test Steps (Attack Simulation):**
1. Act: `GET /api/v1/map/facilities/nearby?latitude=10.77&longitude=106.70` không có `Authorization` header
2. Assert: 401, `error.code == "IAM-001"`

**Expected Result (PASS = hệ thống an toàn):**
- 401 Unauthorized

**Expected Result (FAIL = lỗ hổng tồn tại):**
- 200 OK trả dữ liệu không cần auth

**Current Status:** 🔴 Not written

---

### MAP-TC-010 — facilityType filter áp dụng đúng

**Severity:** `MEDIUM`
**Feature Under Test:** `ICareFacilityRepository.findWithinBoundingBox()`
**Test File:** `src/test/java/com/carebridge/backend/map/repository/CareFacilityRepositoryIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS §8.1 facilityType filter`, `V1__init_schema.sql care_facilities.facility_type`

**Preconditions:**
- Testcontainers PostgreSQL với seed `FX-001` (HOSPITAL) và `FX-002` (CLINIC, nhưng ngoài bounding-box — dùng biến thể trong bounding-box với type khác để test riêng filter)

**Test Steps:**
1. Seed 2 facility cùng bounding-box, khác `facility_type`
2. Act: `findWithinBoundingBox(..., facilityType="HOSPITAL")`
3. Assert: chỉ facility có `facility_type='HOSPITAL'` được trả về

**Expected Result (PASS):**
- Kết quả chỉ chứa facility khớp type

**Expected Result (FAIL):**
- Trả về cả facility không khớp type khi filter được truyền

**Current Status:** 🔴 Not written

---

### MAP-TC-011 — maxResults giới hạn đúng số lượng

**Severity:** `LOW`
**Feature Under Test:** `NearbyFacilityService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyFacilityServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS §8.1 maxResults default=20 (Open — Proposed)`

**Preconditions:**
- Mock repository trả 5 facility

**Test Steps:**
1. Arrange: request với `maxResults=2`
2. Act: gọi `findNearby(...)`
3. Assert: `items.size() == 2`

**Expected Result (PASS):**
- Đúng 2 items trả về (2 gần nhất)

**Expected Result (FAIL):**
- Trả về nhiều hơn maxResults

**Current Status:** 🔴 Not written

---

### MAP-TC-012 — userId cho location_snapshots PHẢI từ JWT, không từ input

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `NearbyFacilityController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyFacilityControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `ADR-MAP-004`

**Preconditions:**
- JWT với `sub = mother-001`; request không chứa `userId` field nào (API không expose param này — xem §9 TDS)

**Test Steps (Attack Simulation):**
1. Act: gọi endpoint với JWT hợp lệ, mock Service để capture `userId` argument
2. Assert: `userId` truyền vào `Service.findNearby()` khớp với `sub` trong JWT, KHÔNG thể override qua query string

**Expected Result (PASS = hệ thống an toàn):**
- `userId` luôn bằng giá trị từ JWT bất kể query string chứa gì

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Endpoint chấp nhận `userId` từ query param và ghi đè giá trị JWT

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### MAP-TC-INT-001 — Full flow: API → DB (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: GET /api/v1/map/facilities/nearby → care_facilities query → location_snapshots write`
**Test File:** `src/test/java/com/carebridge/backend/map/NearbyFacilityIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:**
- PostgreSQL Testcontainer running, Flyway migration (V1 + tất cả migration hiện có) applied tự động
- Seed `care_facilities` với `FX-001` qua JPA hoặc SQL script trực tiếp trong test setup

**Test Steps:**
1. Seed `care_facilities` với 1 record trong bán kính 5km
2. Call `GET /api/v1/map/facilities/nearby?latitude=10.7769&longitude=106.7009&radiusKm=5` với JWT ROLE_MOTHER hợp lệ
3. Assert DB: `location_snapshots` có 1 record mới với `context_type='NEARBY_FACILITY_SEARCH'`, `expires_at` set

**Expected Result (PASS):**
- Response 200 chứa facility đã seed
- `location_snapshots` có bản ghi mới đúng `user_id` từ JWT

**Expected Result (FAIL):**
- Facility không xuất hiện trong response, hoặc `location_snapshots` không được ghi

**DB Assertion:**
```java
List<CareFacility> results = careFacilityRepository.findWithinBoundingBox(...);
assertThat(results).extracting(CareFacility::getFacilityId).contains(seededFacilityId);

List<LocationSnapshot> snapshots = locationSnapshotRepository.findAll();
assertThat(snapshots).anyMatch(s -> s.getContextType().equals("NEARBY_FACILITY_SEARCH"));
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MAP-TC-001` | `NearbyFacilityServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-002` | `NearbyFacilityServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-003` | `NearbyFacilityServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-004` | `NearbyFacilityServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-005` | `NearbyFacilityServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-006` | `NearbyFacilityControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-007` | `NearbyFacilityControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-008` | `NearbyFacilityControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-009` | `NearbyFacilityControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-010` | `CareFacilityRepositoryIntegrationTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-011` | `NearbyFacilityServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-012` | `NearbyFacilityControllerTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-INT-001` | `NearbyFacilityIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class NearbyFacilityService implements INearbyFacilityService {

    @Override
    public NearbyFacilityListResponse findNearby(NearbyFacilitySearchRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MAP-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-006` | `throw('Not implemented')` (via Controller → Service) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-008` | N/A — validation fails before Service call | 🔴 FAIL (nếu chưa có `@Valid`) | ☐ FAIL ☐ PASS | |
| `MAP-TC-009` | N/A — Security filter chain | 🔴 FAIL (nếu security config chưa có) | ☐ FAIL ☐ PASS | |
| `MAP-TC-010` | Repository chưa implement method | 🔴 FAIL (compile error hoặc empty result) | ☐ FAIL ☐ PASS | |
| `MAP-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (điền khi bắt đầu implement)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MAP-IMP-001` đã được review và approve (hiện tại `Draft` — CHƯA đủ điều kiện bắt đầu implement)
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect / TV4-Lâm
- [ ] Không cần migration mới (§5.2 TDS xác nhận) — bỏ qua điều kiện migration
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị
- [ ] Open items trong TDS §2, §4, §16 đã được Product Owner xác nhận hoặc chấp nhận giữ nguyên default đề xuất

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `NearbyFacilityService`
- [ ] Không có business logic trong `NearbyFacilityController` (chỉ validation + mapping)
- [ ] Không có PII (lat/lng thực) xuất hiện plaintext trong logs
- [ ] Mobile: `flutter test` xanh cho `emergencyMap` feature (nếu mobile song song implement trong sprint này)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại: `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — không có shared mutable state giữa tests
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (ADR/BR/SRS)

### Suspension Criteria (Điều kiện tạm dừng)

- TrackAsia API credentials chưa sẵn sàng trong môi trường test/staging
- Open items (bán kính mặc định, maxResults, timeout) chưa được xác nhận — có thể tạm dùng giá trị đề xuất trong TDS nhưng phải ghi rõ trong PR

---

## 7. Rollback Plan

```bash
# Không có migration mới để rollback (TDS §5.2)
git checkout -- src/main/java/com/carebridge/backend/map/
git checkout -- src/test/java/com/carebridge/backend/map/

# Mobile
git checkout -- lib/features/emergencyMap/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR (vd: enum cứng cho facilityType không có trong schema) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic (vd: tính Haversine trong Controller) | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (vd: `TrackAsiaMapClient` chưa định nghĩa đúng §8.3 TDS) | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | ☐ |

---

*Test-Spec v1.0 — Draft. Chưa Approved. Xem TDS §2 (Ma trận Truy vết) và §17 cho Open Items.*
