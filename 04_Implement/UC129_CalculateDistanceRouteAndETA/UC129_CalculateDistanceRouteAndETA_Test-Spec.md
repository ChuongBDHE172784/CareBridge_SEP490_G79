# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC129 — Calculate Distance, Route and ETA

**Document ID:** `CB-MAP-TDD-000`
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
- `02_Requirements/SRS/3_Functional_Specification.md §3.1.3.1` — UC-129 functional requirements
- `04_Implement/UC129_CalculateDistanceRouteAndETA/UC129_CalculateDistanceRouteAndETA_TDS.md` (`CB-MAP-IMP-000`) — Technical Specification (this Test-Spec verifies)
- `04_Implement/UC63_FindNearbyCareFacility/UC63_FindNearbyCareFacility_TDS.md` (`CB-MAP-IMP-001`) — consumer #1, source of `TrackAsiaMapClient` assumed contract
- `04_Implement/UC64_QuickCallOrNavigate/UC64_QuickCallOrNavigate_TDS.md` (`CB-MAP-IMP-002`) — consumer #2
- `CLAUDE.md` — CareBridge architecture & delivery rules

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data (toạ độ giả lập).

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC129 Calculate Distance, Route and ETA |

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
| **Feature / Gap ID** | `GAP-UC129` |
| **Module** | `Calculate Distance, Route and ETA — Bounded Context: map (map.service, map.adapter)` |
| **Spec gốc** | `CB-MAP-IMP-000` (`UC129_CalculateDistanceRouteAndETA_TDS.md`) |
| **Priority** | 🔴 P0 *(High priority theo SRS §3.1.3.1; shared capability chặn UC63/UC64)* |
| **Sprint** | `S1 → S4 (theo function-spec-task-allocation.md — rollout incremental theo TV4-Lâm)` |
| **Milestone** | *(chưa xác nhận — Open, theo dự án chung)* |
| **Data Classification** | `Internal` (module tự thân) / `Sensitive-PII gián tiếp` (qua caller truyền toạ độ) |
| **Compliance Scope** | `PDPA / Luật 91/2025` (gián tiếp — không log toạ độ chính xác ở mức INFO) |
| **Upstream Dependencies** | `TrackAsia Map Service (external HTTP API)` |
| **Downstream Consumers** | `UC63 Find Nearby Care Facility (NearbyFacilityService)`, `UC64 Quick Call or Navigate (QuickActionService/Mobile)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MAP-IMP-000 §17`, ADR-MAP-101 → 105 |
| **Constraints Injected** | C1 (chữ ký `TrackAsiaMapClient.estimateRoute()` bất biến), C2 (timeout 3000ms + 1 retry), C3 (never throw — luôn fallback Haversine), C4 (không tự check RBAC), C5 (cache Caffeine, TTL 10 phút, rounding 4 decimal) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> **Bắt buộc điền trước khi viết test.** Liệt kê mọi sai lệch giữa spec thiết kế (UC63/UC64 đã giả định) và thiết kế chính thức của UC129 (TDS này). Test cases sẽ encode hành vi **đã sửa/formal hoá**, không phải hành vi giả định ban đầu của UC63/UC64.

| # | Spec gốc (UC63/UC64 giả định) | Thực tế (TDS UC129 — CB-MAP-IMP-000) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC63 §8.3 khai báo `record RouteEstimate(double distanceKm, int etaMinutes)` — `etaMinutes` kiểu `int` primitive, không nullable | ADR-MAP-103: mở rộng thành `record RouteEstimate(double distanceKm, Integer etaMinutes, boolean degraded)` — `etaMinutes` kiểu `Integer` nullable, thêm field `degraded` | Test PHẢI assert `result.etaMinutes()` có thể là `null` khi `degraded=true`; test PHẢI assert field `degraded` tồn tại và đúng giá trị boolean |
| L2 | UC63/UC64 gọi thẳng `TrackAsiaMapClient.estimateRoute()` không qua lớp facade nào | UC129 thêm `IMapProviderService.calculateRoute()` làm entrypoint khuyến nghị, bao bọc `TrackAsiaMapClient` + Haversine fallback | Test PHẢI cover cả 2 tầng: `TrackAsiaMapClientImplTest` (adapter, không tự fallback — chỉ throw `TrackAsiaTimeoutException` khi lỗi) VÀ `MapProviderServiceTest` (facade, bắt exception + fallback Haversine) — KHÔNG merge 2 lớp trách nhiệm vào 1 test class |
| L3 | UC63 §6.2 mô tả TrackAsia lỗi → `NearbyFacilityService` tự set `mapServiceDegraded=true` ở response cấp cao (`NearbyFacilityListResponse`), KHÔNG phải ở `RouteEstimate` | UC129's `RouteEstimate.degraded` là field MỚI ở cấp thấp hơn (per-call), không thay thế `NearbyFacilityListResponse.mapServiceDegraded` (đó vẫn là trách nhiệm của UC63) | Test UC129 CHỈ verify `RouteEstimate.degraded` — KHÔNG test `NearbyFacilityListResponse` (ngoài phạm vi UC129, thuộc UC63 Test-Spec) |
| L4 | Không có cache nào được đề cập trong UC63/UC64 TDS | ADR-MAP-104: `TrackAsiaMapClientImpl` cache Caffeine in-memory, TTL 10 phút, key rounding 4 chữ số thập phân | Test PHẢI verify: (a) cache hit không gọi lại TrackAsia, (b) cache miss gọi TrackAsia đúng 1 lần, (c) 2 cặp toạ độ khác nhau ở chữ số thập phân thứ 5 trở đi coi là cùng 1 cache key (rounding), (d) sau TTL hết hạn, gọi lại TrackAsia |
| L5 | UC63 §6.2 dùng Haversine riêng trong `NearbyFacilityService` cho search sort, khác mục đích với Haversine fallback của UC129 (per-pair ETA fallback) — dễ nhầm là trùng lặp | Đây là 2 usage khác nhau, KHÔNG xung đột — UC129's `calculateHaversineDistance()` là public method độc lập, `NearbyFacilityService`'s Haversine (nếu tồn tại) là implementation riêng của UC63 | Test UC129 CHỈ verify `IMapProviderService.calculateHaversineDistance()` đúng công thức chuẩn (Earth radius 6371km) — KHÔNG test/assert hành vi nội bộ của `NearbyFacilityService` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Module `map.service` + `map.adapter` bao gồm các layer:
├── Domain (pure logic — Haversine formula, no external deps) → Unit test (JUnit, không mock)
├── Application / Facade (MapProviderService — mock TrackAsiaMapClient với Mockito)
├── Adapter (TrackAsiaMapClientImpl — mock HTTP layer với WireMock)
└── Integration (MapProviderService + TrackAsiaMapClientImpl thực tế + WireMock server @SpringBootTest)

KHÔNG có Controller layer (UC129 không có HTTP endpoint — xem TDS §9 RG-4).
KHÔNG có Testcontainers PostgreSQL cần thiết cho UC129 (stateless, không có entity/repository riêng)
— khác với UC63/UC64 vốn cần Testcontainers cho `care_facilities`/`quick_action_logs`.
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-129 §3.1.3.1` | Exception E3 (external service failure → retry guidance, no unsafe duplicate action) |
| `CB-MAP-IMP-000 §3 ADR-MAP-101` | Facade `IMapProviderService` bao bọc `TrackAsiaMapClient`, chữ ký bất biến |
| `CB-MAP-IMP-000 §3 ADR-MAP-102` | Timeout 3000ms, 1 retry, backoff 500ms |
| `CB-MAP-IMP-000 §3 ADR-MAP-103` | Haversine fallback, `degraded=true`, `etaMinutes=null`, KHÔNG throw ra ngoài |
| `CB-MAP-IMP-000 §3 ADR-MAP-104` | Cache Caffeine, key rounding 4 decimal, TTL 10 phút |
| `CB-MAP-IMP-000 §3 ADR-MAP-105` | Không tự check RBAC — delegated to caller |
| `CB-MAP-IMP-000 §8` | `TrackAsiaMapClient`, `IMapProviderService`, `RouteEstimate`, `AddressResult` contract |
| `CB-MAP-IMP-000 §10` | Internal exception mapping (`MAP-101-TIMEOUT`, `MAP-102-INVALID-COORD`, `MAP-103-PROVIDER-UNCONFIGURED`) |
| PDPA / BR-RBAC (project compliance scope) | Không log toạ độ chính xác đầy đủ ở mức INFO |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | `IMapProviderService.calculateRoute()` uỷ quyền đúng cho `TrackAsiaMapClient.estimateRoute()` khi thành công | `MapProviderService.calculateRoute()` | `MAP-TC-001` |
| TC-COND-002 | `TrackAsiaMapClient` timeout đúng 3000ms khi TrackAsia không phản hồi | `TrackAsiaMapClientImpl.estimateRoute()` | `MAP-TC-002` |
| TC-COND-003 | `TrackAsiaMapClient` retry đúng 1 lần với backoff 500ms trước khi throw | `TrackAsiaMapClientImpl.estimateRoute()` | `MAP-TC-003` |
| TC-COND-004 | `MapProviderService.calculateRoute()` fallback Haversine khi `TrackAsiaTimeoutException`, KHÔNG throw ra ngoài | `MapProviderService.calculateRoute()` | `MAP-TC-004` |
| TC-COND-005 | Kết quả fallback có `etaMinutes=null`, `degraded=true`, `distanceKm` đúng công thức Haversine | `MapProviderService.calculateRoute()` | `MAP-TC-005` |
| TC-COND-006 | Cache hit: 2 lời gọi liên tiếp cùng toạ độ (trong TTL) → TrackAsia chỉ được gọi 1 lần | `TrackAsiaMapClientImpl.estimateRoute()` | `MAP-TC-006` |
| TC-COND-007 | Cache key rounding: 2 cặp toạ độ khác nhau ở chữ số thập phân thứ 5 → coi là cùng 1 cache entry | `TrackAsiaMapClientImpl` (cache key builder) | `MAP-TC-007` |
| TC-COND-008 | Cache TTL expiry: sau 10 phút (giả lập bằng injectable Clock/Ticker), gọi lại → TrackAsia được gọi lại | `TrackAsiaMapClientImpl` (cache) | `MAP-TC-008` |
| TC-COND-009 | `IMapProviderService`/`TrackAsiaMapClientImpl` KHÔNG chứa bất kỳ tham chiếu nào tới `SecurityContextHolder`/`@PreAuthorize` (static code check) | `MapProviderService`, `TrackAsiaMapClientImpl` (source inspection) | `MAP-TC-009` |
| TC-COND-010 | `RouteEstimate`/`TrackAsiaMapClient.estimateRoute()` giữ đúng chữ ký tương thích với những gì UC63 (`CB-MAP-IMP-001 §8.3`) và UC64 (`CB-MAP-IMP-002 §2`) đã giả định — reconciliation check | `TrackAsiaMapClient` interface (contract test) | `MAP-TC-010` |
| TC-COND-011 | `calculateHaversineDistance()` trả kết quả đúng công thức chuẩn (Earth radius 6371km) cho các cặp toạ độ đã biết trước (known-answer test) | `MapProviderService.calculateHaversineDistance()` | `MAP-TC-011` |
| TC-COND-012 | Input invalid (`latitude` ngoài [-90,90]) → `IllegalArgumentException` với message rõ ràng | `MapProviderService.calculateRoute()` | `MAP-TC-012` |
| TC-COND-013 | Không log toạ độ chính xác đầy đủ (>4 chữ số thập phân) ở log level INFO | `TrackAsiaMapClientImpl` (log inspection) | `MAP-TC-013` |
| TC-COND-014 | `reverseGeocode()` timeout cũng fallback an toàn (trả `AddressResult(null, degraded=true)`, không throw) | `MapProviderService`/`TrackAsiaMapClientImpl.reverseGeocode()` | `MAP-TC-014` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Toạ độ hợp lệ vs. ngoài phạm vi [-90,90]/[-180,180] | Đảm bảo validate coordinate boundary đúng theo `MAP-102-INVALID-COORD` |
| Boundary Value Analysis | `latitude = -90.0, 90.0, -90.0001, 90.0001`; timeout tại đúng 3000ms/2999ms/3001ms | Test boundary chính xác của validate + timeout config |
| State Transition Testing | Cache: `EMPTY → HIT → EXPIRED → MISS(refetch)` | Verify cache lifecycle đúng theo ADR-MAP-104 |
| Error Guessing | Simulate TrackAsia trả JSON malformed, HTTP 500, connection refused | Đảm bảo mọi lỗi TrackAsia đều fallback, không chỉ timeout |
| Contract Testing | So sánh chữ ký method với `CB-MAP-IMP-001`/`CB-MAP-IMP-002` đã "Proposed" | Reconciliation — L1/L2/L3 Logic Issues |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | Coordinate pair | `origin(10.7769, 106.7009)` [Bệnh viện Từ Dũ — HCMC, SYNTHETIC theo UC63 sample], `dest(10.7580, 106.6822)` | Happy path — cặp toạ độ hợp lệ, khoảng cách ~3.1km known-answer |
| `FX-002` | Coordinate pair (invalid) | `origin(-91.0, 106.7009)` | Boundary — latitude ngoài phạm vi |
| `FX-003` | Coordinate pair (rounding test) | `origin(10.77690001, 106.70090001)` vs `origin(10.77694999, 106.70094999)` | Cache key rounding — cùng 4 chữ số thập phân đầu |
| `FX-004` | WireMock stub | `GET /route` → `200 {"distanceKm": 3.1, "etaMinutes": 12}` | TrackAsia thành công |
| `FX-005` | WireMock stub | `GET /route` → `withFixedDelay(5000)` (> 3000ms timeout) | TrackAsia timeout simulation |
| `FX-006` | WireMock stub | `GET /route` → `500 Internal Server Error` | TrackAsia server error simulation |
| `FX-007` | env | `TRACKASIA_API_KEY=test-api-key-synthetic` | Test config, không phải key thật |
| `FX-008` | Known-answer pair | `origin(0,0), dest(0,1)` → Haversine expected `~111.19 km` (1 độ kinh độ tại xích đạo) | Known-answer test cho Haversine formula |
| `FX-009` | Clock/Ticker | Injectable `Ticker` (Caffeine test support) hoặc `Clock` mock để advance thời gian giả lập TTL 10 phút | Test cache TTL expiry mà không cần chờ thực tế 10 phút |

---

## 4. Test Case Specification

> **TC ID format:** `MAP-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// MapProviderTestFactory.java
class MapProviderTestFactory {

    // Cặp toạ độ hợp lệ baseline — đồng bộ với FX-001
    static CoordinatePair makeValidCoordinatePair() {
        return new CoordinatePair(10.7769, 106.7009, 10.7580, 106.6822);
    }

    static CoordinatePair makeValidCoordinatePair(Consumer<CoordinatePairBuilder> overrides) {
        CoordinatePairBuilder builder = new CoordinatePairBuilder(makeValidCoordinatePair());
        overrides.accept(builder);
        return builder.build();
    }

    static CoordinatePair makeInvalidLatitudePair() {
        return new CoordinatePair(-91.0, 106.7009, 10.7580, 106.6822); // FX-002
    }

    static RouteEstimate makeSuccessfulRouteEstimate() {
        return new RouteEstimate(3.1, 12, false); // FX-004
    }

    static RouteEstimate makeDegradedRouteEstimate(double haversineDistanceKm) {
        return new RouteEstimate(haversineDistanceKm, null, true);
    }

    // Helper record cho test — KHÔNG phải production DTO
    record CoordinatePair(double originLat, double originLng, double destLat, double destLng) {}

    static class CoordinatePairBuilder {
        private double originLat, originLng, destLat, destLng;
        CoordinatePairBuilder(CoordinatePair base) {
            this.originLat = base.originLat(); this.originLng = base.originLng();
            this.destLat = base.destLat(); this.destLng = base.destLng();
        }
        CoordinatePairBuilder originLat(double v) { this.originLat = v; return this; }
        CoordinatePair build() { return new CoordinatePair(originLat, originLng, destLat, destLng); }
    }
}
```

---

### MAP-TC-001 — `calculateRoute()` uỷ quyền thành công cho `TrackAsiaMapClient`

**Severity:** `CRITICAL`
**Feature Under Test:** `MapProviderService.calculateRoute()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/MapProviderServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-MAP-IMP-000 §8.1 IMapProviderService.calculateRoute()` Javadoc contract

**Preconditions:**
- Mock `TrackAsiaMapClient` trả `FX-004` (RouteEstimate thành công) cho `FX-001` coordinate pair

**Test Steps:**
1. Arrange: `TrackAsiaMapClient mockClient = mock(TrackAsiaMapClient.class)`; `when(mockClient.estimateRoute(10.7769, 106.7009, 10.7580, 106.6822)).thenReturn(MapProviderTestFactory.makeSuccessfulRouteEstimate())`
2. Act: `RouteEstimate result = mapProviderService.calculateRoute(10.7769, 106.7009, 10.7580, 106.6822)`
3. Assert: `result.equals(new RouteEstimate(3.1, 12, false))`; `verify(mockClient, times(1)).estimateRoute(10.7769, 106.7009, 10.7580, 106.6822)`

**Expected Result (PASS):** `result.distanceKm() == 3.1`, `result.etaMinutes() == 12`, `result.degraded() == false`
**Expected Result (FAIL):** `NullPointerException`, hoặc giá trị không khớp `TrackAsiaMapClient` mock trả về

**Current Status:** 🔴 Not written

---

### MAP-TC-002 — `TrackAsiaMapClientImpl` timeout đúng 3000ms

**Severity:** `CRITICAL`
**Feature Under Test:** `TrackAsiaMapClientImpl.estimateRoute()`
**Test File:** `src/test/java/com/carebridge/backend/map/adapter/TrackAsiaMapClientImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-MAP-IMP-000 §3 ADR-MAP-102` — "timeout kết nối + đọc 3000ms"

**Preconditions:**
- WireMock server chạy local, stub `FX-005` (delay 5000ms > timeout)

**Test Steps:**
1. Arrange: WireMock `stubFor(get(urlPathEqualTo("/route")).willReturn(aResponse().withFixedDelay(5000)))`
2. Act: gọi `trackAsiaMapClientImpl.estimateRoute(...)`, đo thời gian bắt đầu/kết thúc
3. Assert: exception `TrackAsiaTimeoutException` được throw trong khoảng `[3000ms, 3000ms + 1 retry(3000ms) + backoff(500ms) + margin(500ms)] = [3000ms, 7000ms]` — KHÔNG chờ đủ 5000ms x 2 lần (10000ms) nếu retry logic đúng

**Expected Result (PASS):** `TrackAsiaTimeoutException` thrown, tổng thời gian < 7000ms
**Expected Result (FAIL):** Test chờ > 7000ms (timeout config sai) hoặc không throw exception nào

**Current Status:** 🔴 Not written
**Implementation Note:** Dùng `RestClient` với `ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofMillis(3000)).withReadTimeout(Duration.ofMillis(3000))`

---

### MAP-TC-003 — Retry đúng 1 lần với backoff 500ms trước khi throw

**Severity:** `CRITICAL`
**Feature Under Test:** `TrackAsiaMapClientImpl.estimateRoute()`
**Test File:** `src/test/java/com/carebridge/backend/map/adapter/TrackAsiaMapClientImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-MAP-IMP-000 §3 ADR-MAP-102` — "tối đa 1 retry, backoff 500ms"

**Preconditions:**
- WireMock stub `FX-005` cho TẤT CẢ request tới `/route` (luôn timeout)

**Test Steps:**
1. Arrange: WireMock stub luôn delay > 3000ms
2. Act: gọi `estimateRoute(...)`
3. Assert: `verify(exactly(2), getRequestedFor(urlPathEqualTo("/route")))` — đúng 2 lần gọi HTTP (1 lần đầu + 1 retry), KHÔNG nhiều hơn

**Expected Result (PASS):** WireMock ghi nhận đúng 2 request tới `/route`
**Expected Result (FAIL):** 1 request (không retry) hoặc > 2 request (retry vô hạn/nhiều hơn 1 lần)

**Current Status:** 🔴 Not written

---

### MAP-TC-004 — `calculateRoute()` KHÔNG throw ra ngoài khi TrackAsia lỗi — fallback Haversine

**Severity:** `CRITICAL`
**Feature Under Test:** `MapProviderService.calculateRoute()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/MapProviderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-MAP-IMP-000 §3 ADR-MAP-103` — "KHÔNG BAO GIỜ throw exception ra ngoài"; SRS E3 "no duplicate unsafe action"

**Preconditions:**
- Mock `TrackAsiaMapClient.estimateRoute(...)` throw `TrackAsiaTimeoutException`

**Test Steps:**
1. Arrange: `when(mockClient.estimateRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenThrow(new TrackAsiaTimeoutException("timeout", null))`
2. Act: `RouteEstimate result = mapProviderService.calculateRoute(10.7769, 106.7009, 10.7580, 106.6822)` — bọc trong `assertDoesNotThrow(...)`
3. Assert: không có exception nào propagate ra ngoài; `result` không null

**Expected Result (PASS — Red Gate anti-pattern check):** `assertDoesNotThrow()` pass, `result.degraded() == true`
**Expected Result (FAIL):** `TrackAsiaTimeoutException`/bất kỳ exception nào lộ ra khỏi `calculateRoute()` — vi phạm nghiêm trọng C3, phải reject implementation

**Current Status:** 🔴 Not written
**Implementation Note:** `MapProviderService` PHẢI có `try { return trackAsiaMapClient.estimateRoute(...); } catch (TrackAsiaTimeoutException e) { return buildFallbackEstimate(...); }`

---

### MAP-TC-005 — Kết quả fallback đúng: `etaMinutes=null`, `degraded=true`, `distanceKm` = Haversine

**Severity:** `CRITICAL`
**Feature Under Test:** `MapProviderService.calculateRoute()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/MapProviderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-MAP-IMP-000 §3 ADR-MAP-103` decision block + `CB-MAP-IMP-001 §9.2` "Response — 200 OK (TrackAsia degraded)" (`estimatedTravelTimeMinutes: null`)

**Preconditions:**
- Mock `TrackAsiaMapClient` throw `TrackAsiaTimeoutException` cho `FX-001` pair

**Test Steps:**
1. Arrange: mock throw exception cho `origin(10.7769,106.7009)`, `dest(10.7580,106.6822)`
2. Act: `RouteEstimate result = mapProviderService.calculateRoute(10.7769, 106.7009, 10.7580, 106.6822)`
3. Assert: `result.etaMinutes() == null`; `result.degraded() == true`; `result.distanceKm()` xấp xỉ Haversine thực tính = `~3.15 km` (dung sai ±0.05km — known-answer thủ công cho toạ độ Từ Dũ↔Nguyễn Chí Thanh HCMC)

**Expected Result (PASS):** Tất cả 3 assertion đúng
**Expected Result (FAIL):** `etaMinutes` không null (bịa số ước lượng — vi phạm ADR-MAP-103 lựa chọn Phương án A), hoặc `distanceKm` sai công thức

**Current Status:** 🔴 Not written

---

### MAP-TC-006 — Cache hit: gọi 2 lần cùng toạ độ trong TTL → TrackAsia chỉ gọi 1 lần

**Severity:** `HIGH`
**Feature Under Test:** `TrackAsiaMapClientImpl.estimateRoute()` (cache layer)
**Test File:** `src/test/java/com/carebridge/backend/map/adapter/TrackAsiaMapClientImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-MAP-IMP-000 §3 ADR-MAP-104` — "giảm số lượt gọi TrackAsia trùng lặp"

**Preconditions:**
- WireMock stub `FX-004` (thành công)

**Test Steps:**
1. Arrange: WireMock stub `/route` → 200 OK
2. Act: gọi `estimateRoute(10.7769, 106.7009, 10.7580, 106.6822)` hai lần liên tiếp (cùng process, trong TTL)
3. Assert: `verify(exactly(1), getRequestedFor(urlPathEqualTo("/route")))` — chỉ 1 request thực sự tới TrackAsia; cả 2 kết quả trả về giống hệt nhau

**Expected Result (PASS):** WireMock ghi nhận đúng 1 request, 2 kết quả method call giống nhau
**Expected Result (FAIL):** WireMock ghi nhận 2 request (cache không hoạt động)

**Current Status:** 🔴 Not written

---

### MAP-TC-007 — Cache key rounding: toạ độ khác ở chữ số thập phân thứ 5 → cùng cache entry

**Severity:** `MEDIUM`
**Feature Under Test:** `TrackAsiaMapClientImpl` (cache key builder / `CoordinatePairKey`)
**Test File:** `src/test/java/com/carebridge/backend/map/adapter/TrackAsiaMapClientImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-MAP-IMP-000 §3 ADR-MAP-104` — "key rounding 4 chữ số thập phân"; `FX-003`

**Preconditions:**
- WireMock stub `FX-004`

**Test Steps:**
1. Arrange: WireMock stub `/route` → 200 OK
2. Act: gọi `estimateRoute(10.77690001, 106.70090001, 10.7580, 106.6822)` rồi `estimateRoute(10.77694999, 106.70094999, 10.7580, 106.6822)` (khác nhau chỉ ở chữ số thập phân thứ 5+, cùng làm tròn 4 chữ số → `10.7769`)
3. Assert: `verify(exactly(1), getRequestedFor(urlPathEqualTo("/route")))` — 2 lời gọi coi là cùng 1 cache entry

**Expected Result (PASS):** Chỉ 1 request tới TrackAsia cho cả 2 lời gọi Java
**Expected Result (FAIL):** 2 request riêng biệt (rounding logic sai hoặc thiếu)

**Current Status:** 🔴 Not written

---

### MAP-TC-008 — Cache TTL expiry: sau 10 phút, gọi lại TrackAsia

**Severity:** `MEDIUM`
**Feature Under Test:** `TrackAsiaMapClientImpl` (cache TTL)
**Test File:** `src/test/java/com/carebridge/backend/map/adapter/TrackAsiaMapClientImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-MAP-IMP-000 §3 ADR-MAP-104` — "TTL 10 phút"; `FX-009` injectable Ticker

**Preconditions:**
- Cache configured với injectable `Ticker` (Caffeine test API `Ticker.systemTicker()` thay bằng `FakeTicker`/manual advance) để test không cần chờ 10 phút thực tế
- WireMock stub `FX-004`

**Test Steps:**
1. Arrange: build `TrackAsiaMapClientImpl` với test `Ticker` cho phép `advance(Duration)`
2. Act bước 1: gọi `estimateRoute(FX-001)` → cache miss, gọi TrackAsia lần 1
3. Act bước 2: `ticker.advance(Duration.ofMinutes(11))` (vượt TTL 10 phút)
4. Act bước 3: gọi lại `estimateRoute(FX-001)` với cùng toạ độ
5. Assert: `verify(exactly(2), getRequestedFor(urlPathEqualTo("/route")))` — TrackAsia được gọi lại lần 2 sau khi cache hết hạn

**Expected Result (PASS):** 2 request riêng biệt tới TrackAsia (trước và sau TTL expiry)
**Expected Result (FAIL):** Chỉ 1 request (cache không bao giờ hết hạn — memory leak risk) hoặc luôn > 1 request kể cả trong TTL (cache không hoạt động — trùng với MAP-TC-006 failure mode)

**Current Status:** 🔴 Not written
**Implementation Note:** Nếu Caffeine builder không hỗ trợ inject `Ticker` dễ dàng qua constructor, cân nhắc wrap trong 1 factory method cho phép test override — KHÔNG dùng `Thread.sleep(600000)` (10 phút thực tế) trong test.

---

### MAP-TC-009 — Không có tham chiếu `SecurityContextHolder`/`@PreAuthorize` trong module

**Severity:** `HIGH`
**Feature Under Test:** `MapProviderService`, `TrackAsiaMapClientImpl` (source inspection)
**Test File:** `src/test/java/com/carebridge/backend/map/ArchitectureRuleTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-MAP-IMP-000 §3 ADR-MAP-105` — "KHÔNG tự inject SecurityContext"

**Preconditions:**
- Dùng ArchUnit (nếu đã có dependency trong `pom.xml`; nếu chưa có, dùng static grep test đơn giản qua `Files.readString`)

**Test Steps:**
1. Arrange: quét toàn bộ source file trong package `com.carebridge.backend.map.service` và `com.carebridge.backend.map.adapter`
2. Act: kiểm tra import statement
3. Assert: KHÔNG có import `org.springframework.security.core.context.SecurityContextHolder` hoặc annotation `@PreAuthorize` trong 2 class `MapProviderService`, `TrackAsiaMapClientImpl`

**Expected Result (PASS):** Không tìm thấy tham chiếu nào
**Expected Result (FAIL):** Tìm thấy `SecurityContextHolder`/`@PreAuthorize` — vi phạm C4, RBAC bị nhân đôi sai vị trí

**Current Status:** 🔴 Not written

---

### MAP-TC-010 — Contract reconciliation: chữ ký `TrackAsiaMapClient.estimateRoute()` tương thích UC63/UC64

**Severity:** `CRITICAL`
**Feature Under Test:** `TrackAsiaMapClient` interface (reflection-based contract test)
**Test File:** `src/test/java/com/carebridge/backend/map/adapter/TrackAsiaMapClientContractTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-MAP-IMP-001 §8.3` (UC63) + `CB-MAP-IMP-002 §2` (UC64) — "estimateRoute(originLat, originLng, destLat, destLng)"

**Preconditions:**
- `TrackAsiaMapClient` interface compiled và có trên classpath

**Test Steps:**
1. Arrange: `Method method = TrackAsiaMapClient.class.getMethod("estimateRoute", double.class, double.class, double.class, double.class)`
2. Act: reflection kiểm tra `method.getReturnType()`
3. Assert: `method` tồn tại (không throw `NoSuchMethodException`); `method.getReturnType() == RouteEstimate.class`; method có đúng 4 tham số kiểu `double`

**Expected Result (PASS):** Method tồn tại đúng chữ ký `(double, double, double, double): RouteEstimate` — khớp UC63/UC64 đã giả định (ngoại trừ nội dung field bên trong `RouteEstimate` được mở rộng theo L1, nhưng TÊN method + tham số + kiểu trả về interface KHÔNG đổi)
**Expected Result (FAIL):** `NoSuchMethodException` — nghĩa là chữ ký đã bị đổi, phá vỡ hợp đồng UC63/UC64 đã dựa vào, VI PHẠM C1

**Current Status:** 🔴 Not written

---

### MAP-TC-011 — Known-answer test cho `calculateHaversineDistance()`

**Severity:** `HIGH`
**Feature Under Test:** `MapProviderService.calculateHaversineDistance()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/MapProviderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `FX-008` — công thức Haversine chuẩn, Earth radius = 6371km; 1 độ kinh độ tại xích đạo ≈ 111.19km (giá trị toán học đã biết, không phải business assumption)

**Preconditions:** Không cần mock — pure function test

**Test Steps:**
1. Arrange: `origin(0.0, 0.0)`, `dest(0.0, 1.0)` (FX-008)
2. Act: `double distance = mapProviderService.calculateHaversineDistance(0.0, 0.0, 0.0, 1.0)`
3. Assert: `assertThat(distance).isCloseTo(111.19, within(0.5))` (dung sai 0.5km do làm tròn Earth radius)

**Expected Result (PASS):** Kết quả trong khoảng `[110.69, 111.69]` km
**Expected Result (FAIL):** Kết quả lệch xa (sai công thức, sai đơn vị độ/radian, hoặc dùng sai Earth radius)

**Current Status:** 🔴 Not written

---

### MAP-TC-012 — Invalid latitude → `IllegalArgumentException`

**Severity:** `HIGH`
**Feature Under Test:** `MapProviderService.calculateRoute()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/MapProviderServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-MAP-IMP-000 §10` — `MAP-102-INVALID-COORD`

**Preconditions:** `FX-002` (`origin.lat = -91.0`)

**Test Steps:**
1. Arrange: dùng `MapProviderTestFactory.makeInvalidLatitudePair()`
2. Act: `mapProviderService.calculateRoute(-91.0, 106.7009, 10.7580, 106.6822)`
3. Assert: `assertThrows(IllegalArgumentException.class, () -> ...)`, message chứa "latitude"

**Expected Result (PASS):** `IllegalArgumentException` thrown với message rõ ràng
**Expected Result (FAIL):** Không throw (silent bad data) hoặc throw `NullPointerException`/generic exception không rõ nguyên nhân

**Current Status:** 🔴 Not written

---

### MAP-TC-013 — Log KHÔNG chứa toạ độ chính xác đầy đủ ở mức INFO

**Severity:** `HIGH`
**CWE:** `CWE-532 — Insertion of Sensitive Information into Log File`
**Legal:** `PDPA / Luật 91/2025 — minimum necessary logging`
**Feature Under Test:** `TrackAsiaMapClientImpl` (log output inspection)
**Test File:** `src/test/java/com/carebridge/backend/map/adapter/TrackAsiaMapClientImplLogTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-MAP-IMP-000 §4.3` — "No PII in logs"; `CB-MAP-IMP-000 §14.2`

**Preconditions:**
- Log appender capture (`ListAppender` cho Logback) attach vào logger `com.carebridge.backend.map.adapter.TrackAsiaMapClientImpl`, level INFO

**Test Steps:**
1. Arrange: attach `ListAppender` để capture log events
2. Act: gọi `estimateRoute(10.7769, 106.7009, 10.7580, 106.6822)` với mock/WireMock thành công
3. Assert: duyệt tất cả log message ở level INFO — KHÔNG có regex match toạ độ full-precision (vd: `10\.7769\d*,\s*106\.7009\d*`)

**Expected Result (PASS):** Không tìm thấy toạ độ full-precision trong log INFO (có thể log ở DEBUG hoặc log dạng đã làm tròn/ẩn 1 phần)
**Expected Result (FAIL):** Tìm thấy toạ độ chính xác trong log INFO — vi phạm minimum-necessary logging

**Current Status:** 🔴 Not written

---

### MAP-TC-014 — `reverseGeocode()` timeout cũng fallback an toàn

**Severity:** `MEDIUM`
**Feature Under Test:** `MapProviderService`/`TrackAsiaMapClientImpl.reverseGeocode()`
**Test File:** `src/test/java/com/carebridge/backend/map/adapter/TrackAsiaMapClientImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-MAP-IMP-000 §8.2` — `reverseGeocode()` Javadoc; consistency với ADR-MAP-103 (never throw pattern áp dụng đồng nhất cho cả 2 method của `TrackAsiaMapClient`)

**Preconditions:** WireMock stub `/geocode` → delay > 3000ms

**Test Steps:**
1. Arrange: WireMock stub timeout cho endpoint reverse-geocode
2. Act: gọi `reverseGeocode(10.7769, 106.7009)` qua `TrackAsiaMapClientImpl` trực tiếp (method này CHỈ throw `TrackAsiaTimeoutException`, không tự fallback — fallback là trách nhiệm của caller nếu có, theo cùng pattern `estimateRoute`)
3. Assert: `assertThrows(TrackAsiaTimeoutException.class, ...)` ở tầng `TrackAsiaMapClientImpl` (adapter không tự fallback — xem Logic Issue L2)

**Expected Result (PASS):** `TrackAsiaTimeoutException` thrown đúng tại tầng adapter (nhất quán với `estimateRoute()` — adapter luôn throw khi lỗi, facade mới là nơi fallback)
**Expected Result (FAIL):** Method silently trả `null` hoặc trả `AddressResult` rỗng không rõ ràng thay vì throw — vi phạm nguyên tắc nhất quán 2 method trong cùng interface

**Current Status:** 🔴 Not written
**Implementation Note:** Nếu `MapProviderService` có wrapper method cho `reverseGeocode` tương tự `calculateRoute`, cần thêm test riêng ở `MapProviderServiceTest` cho hành vi fallback ở tầng facade — hiện TDS §8.1 chỉ khai báo `calculateRoute`/`calculateHaversineDistance` cho `IMapProviderService`, KHÔNG có `reverseGeocode` wrapper — ghi nhận **Open**: nếu UC63 cần gọi `reverseGeocode` qua facade, cần bổ sung `IMapProviderService.reverseGeocodeAddress()` trong lần cập nhật TDS sau (ngoài phạm vi Draft hiện tại).

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MAP-TC-001` | `MapProviderServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-002` | `TrackAsiaMapClientImplTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-003` | `TrackAsiaMapClientImplTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-004` | `MapProviderServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-005` | `MapProviderServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-006` | `TrackAsiaMapClientImplTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-007` | `TrackAsiaMapClientImplTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-008` | `TrackAsiaMapClientImplTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-009` | `ArchitectureRuleTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-010` | `TrackAsiaMapClientContractTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-011` | `MapProviderServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-012` | `MapProviderServiceTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-013` | `TrackAsiaMapClientImplLogTest.java` | `[ ]` | `[ ]` | |
| `MAP-TC-014` | `TrackAsiaMapClientImplTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL. Nếu test PASS ngay → **AP-AI-002 detected** → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class MapProviderService implements IMapProviderService {

    @Override
    public RouteEstimate calculateRoute(double originLat, double originLng, double destLat, double destLng) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public double calculateHaversineDistance(double originLat, double originLng, double destLat, double destLng) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class TrackAsiaMapClientImpl implements TrackAsiaMapClient {

    @Override
    public RouteEstimate estimateRoute(double originLat, double originLng, double destLat, double destLng) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public AddressResult reverseGeocode(double lat, double lng) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MAP-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `MAP-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-004` | `throw('Not implemented')` — **QUAN TRỌNG:** stub throw `UnsupportedOperationException`, test assert `assertDoesNotThrow` sẽ FAIL đúng vì stub throw exception khác loại mong đợi | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-009` | N/A — static/source inspection test, không phụ thuộc stub runtime | 🔴 FAIL (nếu source chưa tồn tại → compile error, coi là FAIL hợp lệ) | ☐ FAIL ☐ PASS | |
| `MAP-TC-010` | `NoSuchMethodException` nếu interface method chưa tồn tại | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-012` | `throw('Not implemented')` (thay vì `IllegalArgumentException` mong đợi) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP-TC-013` | `throw('Not implemented')` — log capture rỗng, assertion về "không tìm thấy toạ độ" có thể PASS SAI (vacuously true vì không có log nào cả) — **CẦN REVIEW ĐẶC BIỆT** | 🔴 FAIL (test phải assert CẢ việc method thực thi thành công trước khi check log content, để tránh vacuous pass) | ☐ FAIL ☐ PASS | ☐ Tautology (nếu PASS do log rỗng — phải sửa test để fail rõ ràng khi method throw) |
| `MAP-TC-014` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` *(điền khi implement)*
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `___` *(điền khi implement)*

> **Lưu ý đặc biệt cho `MAP-TC-013`:** Test kiểm tra "log KHÔNG chứa X" có rủi ro vacuous-pass nếu method chưa chạy được (không sinh log nào). Khi viết test thật, PHẢI kết hợp assert method thực thi KHÔNG throw (hoặc catch riêng exception của Red Phase) TRƯỚC KHI assert nội dung log, để đảm bảo Red Gate bắt được lỗi đúng cách.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MAP-IMP-000` đã được review và approve (đặc biệt ADR-MAP-101/103 Open Items)
- [ ] Logic Issues (Section 2 — L1 đến L5) đã được confirm với TV4-Lâm / Principal Architect
- [ ] Xác nhận với UC63/UC64 owner về `RouteEstimate` signature mở rộng (L1) TRƯỚC khi viết test — nếu không đồng ý, cần điều chỉnh Test-Spec này
- [ ] Không cần Flyway migration nào (§5.2 TDS) — N/A cho entry criteria migration
- [ ] Test fixtures (Section 3 TDS-05, `FX-001` → `FX-009`) đã được chuẩn bị
- [ ] WireMock dependency xác nhận có trong `pom.xml` (scope test) — nếu chưa có, cần thêm (kiểm tra CLAUDE.md dependency approval trước)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration/WireMock tests xanh
- [ ] Test coverage ≥ 80% lines cho `MapProviderService` và `TrackAsiaMapClientImpl`
- [ ] Không có business logic trong bất kỳ Controller nào (N/A — UC129 không có Controller)
- [ ] Không có PII/toạ độ chính xác xuất hiện plaintext trong logs INFO (MAP-TC-013 pass)
- [ ] `TrackAsiaMapClient` interface chữ ký xác nhận tương thích UC63/UC64 (MAP-TC-010 pass)
- [ ] `calculateRoute()` xác nhận KHÔNG BAO GIỜ throw exception ra ngoài trong mọi kịch bản lỗi đã test (MAP-TC-004 pass)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mọi coordinate pair tạo qua `MapProviderTestFactory`)
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR/TDS section) — đã đối chiếu ở mỗi TC §4

### Suspension Criteria (Điều kiện tạm dừng)

- ADR-MAP-101/103 Open Items chưa được TV4-Lâm/Product Owner xác nhận (đặc biệt breaking change `RouteEstimate.etaMinutes: int → Integer`)
- UC63/UC64 implementation (nếu chạy song song) đã tạo `TrackAsiaMapClient`/`RouteEstimate` với chữ ký khác — cần đồng bộ trước khi tiếp tục
- WireMock dependency chưa được duyệt thêm vào `pom.xml`

---

## 7. Rollback Plan

```bash
# UC129 không có migration nào để rollback (stateless service)

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/service/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/adapter/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/map/

# Nếu UC63/UC64 đã phụ thuộc vào các file này, kiểm tra compile trước khi revert:
./mvnw compile
# Nếu lỗi biên dịch phát sinh ở NearbyFacilityService/QuickActionService (UC63/UC64),
# PHẢI khôi phục lại thay vì rollback hoàn toàn — feature này là dependency chặn (blocking dependency).

# Gap vẫn OPEN → giữ nguyên entry trong theo dõi sprint chung
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Đã kiểm tra — mọi TC đều có `Oracle Source` trỏ về `CB-MAP-IMP-000` hoặc SRS | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Cần verify khi implement — đặc biệt lưu ý `MAP-TC-013` (rủi ro vacuous pass đã ghi chú) | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Đã kiểm tra — mọi assumption trace về ADR-MAP-101→105 | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ N/A — không có Controller trong UC129 | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ Cần verify khi implement — `TrackAsiaMapClient`/`RouteEstimate` PHẢI dùng lại file đã tồn tại nếu UC63/UC64 implement trước (xem TDS §11.3 lưu ý) | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn Draft spec (chỉ có 2 mục cần verify runtime khi implement — không phải lỗi spec)
- [ ] Phát hiện AP khi implement → ghi vào bảng dưới → fix trước khi tiếp tục

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| *(để trống — điền khi review runtime)* | | | | ☐ |

---

*TDD Spec v1.0 — Draft. Chưa Approved. Xem §2 Logic Issues (L1-L5, đặc biệt L1 breaking change) và §6 Entry Criteria (xác nhận UC63/UC64 owner) trước khi chuyển Status sang `Approved`.*
