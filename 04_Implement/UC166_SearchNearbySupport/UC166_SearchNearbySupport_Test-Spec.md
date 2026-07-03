# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC166 — Search Nearby Support

**Document ID:** `CB-MAP-TDD-008`
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
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.7.4` — Functional requirements (UC-166)
- `04_Implement/UC166_SearchNearbySupport/UC166_SearchNearbySupport_TDS.md` (`CB-MAP-IMP-008`) — Technical Specification
- `04_Implement/UC63_FindNearbyCareFacility/UC63_FindNearbyCareFacility_TDS.md` — facility data source (`INearbyFacilityService`)
- `04_Implement/UC149_FindNearbyAvailableExperts/UC149_FindNearbyAvailableExperts_TDS.md` — expert data source (`INearbyExpertService`)
- `04_Implement/UC155_ViewNearbyExpertsOnMap/UC155_ViewNearbyExpertsOnMap_TDS.md` — reuse-not-rewrite pattern reference
- PDPA / Luật 91/2025 — Legal basis for location PII handling

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC166 Search Nearby Support |

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
| **Feature / Gap ID** | `UC166` |
| **Module** | `Search Nearby Support — map bounded context (aggregator)` |
| **Spec gốc** | `CB-MAP-IMP-008` |
| **Priority** | 🟠 P1 (High — theo SRS Priority field) |
| **Sprint** | `TV4-Lâm ownership — Location/map/nearby care domain, sprint TBD` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` (phần expert chứa location PII; phần facility là Public/Internal) |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `INearbyFacilityService (UC63)`, `INearbyExpertService (UC149)` |
| **Downstream Consumers** | Mobile `nearbySupport` feature |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MAP-IMP-008 §17`, `ADR-MAP-401`, `ADR-MAP-402`, `ADR-MAP-403`, `ADR-MAP-404` |
| **Constraints Injected** | C1 (delegate to UC63+UC149, no re-implementation), C2 (merge sort by distance), C3 (partial degradation), C4 (userId from JWT, no new snapshot context), C5 (empty state = 200) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.7.4 dùng từ "or" (facility OR expert) — có thể hiểu nhầm là chỉ trả 1 loại tại 1 thời điểm | TDS `CB-MAP-IMP-008` ADR-MAP-401/402 xác nhận: mặc định trả CẢ HAI (merged), `supportType` param cho phép Mother chọn giới hạn 1 loại | Test cover cả 3 trường hợp: mặc định (both), `supportType=FACILITY`, `supportType=EXPERT` |
| L2 | 2 service con (UC63, UC149) có 2 cơ chế "degraded" khác nhau (`mapServiceDegraded` cấp response) — dễ nhầm lẫn khi propagate lên UC166's 2 cờ riêng (`facilityServiceDegraded`/`expertServiceDegraded`) | TDS ADR-MAP-403 phân biệt rõ: degraded ở UC166 nghĩa là "toàn bộ nguồn lỗi" (exception), KHÔNG phải "1 phần TrackAsia ETA lỗi bên trong 1 item" (đó là field `estimatedTravelTimeMinutes=null` pass-through, khác biệt) | Test tách rõ 2 test case: total-source-failure (UC166-level flag) vs item-level ETA degraded (pass-through, không set cờ UC166) |
| L3 | UC166 gọi 2 service con — nếu implement sai bằng cách gọi tuần tự (sequential) thay vì song song, latency sẽ cộng dồn vi phạm NFR §4.1 | TDS §4.1 quy định `< 1500ms` = max(2 latency), không phải tổng | Test đo thời gian mock delay của 2 service con (mỗi mock delay 1000ms) và assert tổng thời gian gọi < 1500ms (không phải ~2000ms) — chứng minh gọi song song |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Search Nearby Support bao gồm các layer:
├── Domain (NearbySupportItem mapping — pure logic, no deps)
├── Service (NearbySupportService — mock INearbyFacilityService, INearbyExpertService với Mockito)
├── Controller (NearbySupportController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, @SpringBootTest, full HTTP flow qua CẢ 2 service con thật)
```

> UC166 KHÔNG có Repository layer riêng (ADR-MAP-401) — không cần test repository.

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-166 §3.3.7.4` | Trigger, Normal Flow, AF1/AF2/AF3, E1/E2/E3, Business Rules BR-RBAC/BR-SAFETY |
| `CB-MAP-IMP-008 ADR-MAP-401` | Gọi song song 2 service con, KHÔNG viết lại query |
| `CB-MAP-IMP-008 ADR-MAP-402` | Merge/sort theo distanceKm, `supportType` filter |
| `CB-MAP-IMP-008 ADR-MAP-403` | Partial degradation — 1 nguồn lỗi không chặn nguồn còn lại |
| `CB-MAP-IMP-008 ADR-MAP-404` | RBAC ROLE_MOTHER, không double-write location_snapshots |
| `CB-MAP-IMP-001 §8.1` (UC63) | `INearbyFacilityService.findNearby()` contract |
| `CB-MAP-IMP-005 §8.1` (UC149) | `INearbyExpertService.findNearby()` contract |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | UC166 gọi `INearbyFacilityService.findNearby()` (UC63) — KHÔNG tự query `care_facilities` | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-001` |
| TC-COND-002 | UC166 gọi `INearbyExpertService.findNearby()` (UC149) — KHÔNG tự query `expert_location_shares` | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-002` |
| TC-COND-003 | Cả 2 service con được gọi SONG SONG (latency không cộng dồn) | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-003` |
| TC-COND-004 | Merge kết quả sort theo `distanceKm` tăng dần, xen kẽ facility/expert | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-004` |
| TC-COND-005 | `supportType=FACILITY` → chỉ gọi UC63, KHÔNG gọi UC149 | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-005` |
| TC-COND-006 | `supportType=EXPERT` → chỉ gọi UC149, KHÔNG gọi UC63 | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-006` |
| TC-COND-007 | Facility service lỗi → `facilityServiceDegraded=true`, expert items vẫn đầy đủ, HTTP 200 | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-007` |
| TC-COND-008 | Expert service lỗi → `expertServiceDegraded=true`, facility items vẫn đầy đủ, HTTP 200 | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-008` |
| TC-COND-009 | CẢ HAI service lỗi → HTTP 503 MAP-403 | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-009` |
| TC-COND-010 | `userId` từ JWT SecurityContext; UC166 KHÔNG tự ghi `location_snapshots` mới | `NearbySupportController`, `NearbySupportService` | `SUP166-TC-010` |
| TC-COND-011 | Không có kết quả nào (cả 2 nguồn rỗng) → HTTP 200 với `items:[]` (AF2) | `NearbySupportController` | `SUP166-TC-011` |
| TC-COND-012 | Consistency: `resultType=FACILITY` items khớp 1:1 với UC63's `NearbyFacilityItem` list | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-012` |
| TC-COND-013 | Consistency: `resultType=EXPERT` items khớp 1:1 với UC149's `NearbyExpertItem` list (bao gồm consent-gating đã áp dụng bởi UC149) | `NearbySupportService.searchNearbySupport()` | `SUP166-TC-013` |
| TC-COND-014 | Không có JWT / sai role → 401/403 | `NearbySupportController` (security filter) | `SUP166-TC-014`, `SUP166-TC-015` |
| TC-COND-015 | Full HTTP flow end-to-end với DB thực, cả 2 service con thật (Testcontainers) | `NearbySupportController` + `NearbySupportService` + UC63 + UC149 | `SUP166-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `supportType` (FACILITY / EXPERT / null=both / invalid) | Phân vùng giá trị hợp lệ/không hợp lệ |
| Decision Table | Facility service (OK/FAIL) × Expert service (OK/FAIL) — 4 tổ hợp | Đảm bảo cover đầy đủ partial degradation matrix (ADR-MAP-403) |
| Boundary Value Analysis | `maxResults` biên trên (đúng bằng tổng số item merge được) | Đảm bảo limit áp dụng đúng sau merge, không trước |
| Error Guessing | JWT thiếu, cả 2 service con throw cùng lúc, timeout race condition | Security/resilience coverage |
| Performance Testing | Latency khi gọi song song vs tuần tự | Verify NFR §4.1 (CompletableFuture parallelism) |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-166-001` | Mock | `INearbyFacilityService.findNearby() → NearbyFacilityListResponse(items=[facility1], mapServiceDegraded=false)` | Happy path facility |
| `FX-166-002` | Mock | `INearbyExpertService.findNearby() → NearbyExpertListResponse(items=[expert1], mapServiceDegraded=false)` | Happy path expert |
| `FX-166-003` | Mock | facility1: `distanceKm=0.9`; expert1: `distanceKm=1.2` | Dùng cho merge/sort test — facility phải đứng trước expert khi sort ASC |
| `FX-166-004` | Mock | `INearbyFacilityService.findNearby()` throws `DataAccessException` | Partial degradation — facility source lỗi |
| `FX-166-005` | Mock | `INearbyExpertService.findNearby()` throws `DataAccessException` | Partial degradation — expert source lỗi |
| `FX-166-006` | JWT | `{ sub: 'mother-001', role: 'ROLE_MOTHER' }` | Auth context hợp lệ |
| `FX-166-007` | Mock | Cả 2 service con delay 1000ms (dùng `Thread.sleep` trong mock answer) | Verify parallel call (TC-COND-003) |
| `FX-166-008` | Request | `NearbySupportSearchRequest { latitude: 10.7769, longitude: 106.7009, radiusKm: 5.0, supportType: null, maxResults: 20 }` | Happy path combined request |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// NearbySupportTestFactory.java
class NearbySupportTestFactory {

    static final UUID FACILITY_ID    = UUID.fromString("00000000-0000-0000-0000-000000000501");
    static final UUID EXPERT_ID      = UUID.fromString("00000000-0000-0000-0000-000000000502");
    static final UUID MOTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");

    static NearbyFacilityItem makeFacilityItem() {
        NearbyFacilityItem f = new NearbyFacilityItem();
        f.setFacilityId(FACILITY_ID);
        f.setName("Bệnh viện Từ Dũ");
        f.setFacilityType("HOSPITAL");
        f.setAddress("284 Cống Quỳnh, Q1, TP.HCM");
        f.setPhone("+842854042829");
        f.setDistanceKm(0.9);
        f.setEstimatedTravelTimeMinutes(4);
        f.setVerificationStatus("VERIFIED");
        return f;
    }

    static NearbyExpertItem makeExpertItem() {
        NearbyExpertItem e = new NearbyExpertItem();
        e.setExpertProfileId(EXPERT_ID);
        e.setSpecialty("Pediatrics");
        e.setProfessionalTitle("BS. Nguyễn Văn A");
        e.setRatingAvg(new BigDecimal("4.8"));
        e.setVerificationStatus("VERIFIED");
        e.setAvailabilityStatus("AVAILABLE");
        e.setDistanceKm(1.2);
        e.setAccuracyMeters(50.0);
        e.setEstimatedTravelTimeMinutes(6);
        return e;
    }

    static NearbyFacilityListResponse makeFacilityResponse() {
        return new NearbyFacilityListResponse(List.of(makeFacilityItem()), false);
    }

    static NearbyFacilityListResponse makeEmptyFacilityResponse() {
        return new NearbyFacilityListResponse(List.of(), false);
    }

    static NearbyExpertListResponse makeExpertResponse() {
        return new NearbyExpertListResponse(List.of(makeExpertItem()), false);
    }

    static NearbyExpertListResponse makeEmptyExpertResponse() {
        return new NearbyExpertListResponse(List.of(), false);
    }

    static NearbySupportSearchRequest makeRequest() {
        NearbySupportSearchRequest r = new NearbySupportSearchRequest();
        r.setLatitude(10.7769);
        r.setLongitude(106.7009);
        r.setRadiusKm(5.0);
        r.setMaxResults(20);
        return r;
    }

    static NearbySupportSearchRequest makeRequest(Consumer<NearbySupportSearchRequest> overrides) {
        NearbySupportSearchRequest r = makeRequest();
        overrides.accept(r);
        return r;
    }
}
```

---

### SUP166-TC-001 — UC166 gọi INearbyFacilityService.findNearby() (UC63), KHÔNG tự query care_facilities

**Severity:** `CRITICAL`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-401 (Proposed)` / `CB-MAP-IMP-001 §8.1 INearbyFacilityService`

**Preconditions:**
- Mock `INearbyFacilityService` (FX-166-001), `INearbyExpertService` (FX-166-002)

**Test Steps:**
1. Arrange: mock cả 2 service con
2. Act: gọi `nearbySupportService.searchNearbySupport(makeRequest(), MOTHER_USER_ID)`
3. Assert: `verify(nearbyFacilityService, times(1)).findNearby(any(), eq(MOTHER_USER_ID)))`; response `items` chứa `resultType=FACILITY` item với `referenceId=FACILITY_ID`

**Expected Result (PASS — hành vi đúng):**
- `INearbyFacilityService.findNearby()` được gọi đúng 1 lần với `userId` truyền qua

**Expected Result (FAIL — dấu hiệu lỗi):**
- Service không gọi `INearbyFacilityService`, hoặc tự query `ICareFacilityRepository` (vi phạm ADR-MAP-401)

**Current Status:** 🔴 Not written
**Implementation Note:** ARCHITECTURE COMPLIANCE test — `NearbySupportService` KHÔNG được inject `ICareFacilityRepository` trực tiếp.

---

### SUP166-TC-002 — UC166 gọi INearbyExpertService.findNearby() (UC149), KHÔNG tự query expert_location_shares

**Severity:** `CRITICAL`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-401 (Proposed)` / `CB-MAP-IMP-005 §8.1 INearbyExpertService`

**Test Steps:**
1. Arrange: mock cả 2 service con
2. Act: gọi `searchNearbySupport(makeRequest(), MOTHER_USER_ID)`
3. Assert: `verify(nearbyExpertService, times(1)).findNearby(any(), eq(MOTHER_USER_ID)))`; response `items` chứa `resultType=EXPERT` item với `referenceId=EXPERT_ID`

**Expected Result (PASS):**
- `INearbyExpertService.findNearby()` được gọi đúng 1 lần

**Expected Result (FAIL):**
- Service tự inject `IExpertLocationShareRepository` trực tiếp (vi phạm ADR-MAP-401)

**Current Status:** 🔴 Not written
**Implementation Note:** ARCHITECTURE COMPLIANCE test — mirror SUP166-TC-001 cho phần expert.

---

### SUP166-TC-003 — Cả 2 service con được gọi song song, latency không cộng dồn

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-MAP-IMP-008 §4.1 NFR (Proposed)` / `ADR-MAP-401`

**Preconditions:**
- Mock cả 2 service con delay 1000ms mỗi cái (FX-166-007, dùng `Mockito.doAnswer` với `Thread.sleep(1000)`)

**Test Steps:**
1. Arrange: mock delay như trên
2. Act: đo thời gian gọi `searchNearbySupport(makeRequest(), MOTHER_USER_ID)` bằng `System.nanoTime()` trước/sau
3. Assert: tổng thời gian < 1500ms (chứng minh gọi song song — nếu tuần tự sẽ ~2000ms+)

**Expected Result (PASS):**
- Tổng thời gian đo được xấp xỉ `max(1000ms, 1000ms) + overhead` (~1000-1200ms), KHÔNG phải `1000+1000=2000ms`

**Expected Result (FAIL):**
- Tổng thời gian ≥ 1900ms — chứng tỏ code gọi tuần tự (`.join()` ngay sau từng call thay vì `CompletableFuture.allOf()`)

**Current Status:** 🔴 Not written
**Implementation Note:** Dùng `CompletableFuture.supplyAsync()` cho cả 2 lời gọi TRƯỚC KHI `.join()`/`.get()` — không gọi `.join()` ngay sau future thứ nhất.

---

### SUP166-TC-004 — Merge kết quả sort theo distanceKm tăng dần, xen kẽ facility/expert

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-402 (Proposed)`

**Preconditions:**
- facility1 `distanceKm=0.9` (FX-166-003), expert1 `distanceKm=1.2`; thêm expert2 `distanceKm=0.5` (gần hơn facility1)

**Test Steps:**
1. Arrange: mock facility service trả `[facility1(0.9km)]`; mock expert service trả `[expert2(0.5km), expert1(1.2km)]`
2. Act: gọi `searchNearbySupport(makeRequest(), MOTHER_USER_ID)`
3. Assert: `items` theo đúng thứ tự: `expert2(0.5)`, `facility1(0.9)`, `expert1(1.2)` — xen kẽ resultType theo distance, KHÔNG nhóm riêng theo loại

**Expected Result (PASS):**
- Thứ tự `items` đúng theo `distanceKm` tăng dần bất kể `resultType`

**Expected Result (FAIL):**
- `items` nhóm riêng (tất cả facility trước, rồi tất cả expert sau) — vi phạm ADR-MAP-402

**Current Status:** 🔴 Not written
**Implementation Note:** Dùng `Stream.concat(facilityItems, expertItems).sorted(Comparator.comparing(NearbySupportItem::getDistanceKm))`.

---

### SUP166-TC-005 — supportType=FACILITY: chỉ gọi UC63, KHÔNG gọi UC149

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-402 (Proposed)`

**Test Steps:**
1. Act: gọi `searchNearbySupport(makeRequest(r -> r.setSupportType("FACILITY")), MOTHER_USER_ID)`
2. Assert: `verify(nearbyFacilityService, times(1)).findNearby(any(), any())`; `verify(nearbyExpertService, never()).findNearby(any(), any())`

**Expected Result (PASS):**
- Chỉ facility service được gọi — tối ưu, không lãng phí round-trip DB cho expert

**Expected Result (FAIL):**
- Cả 2 service đều được gọi dù Mother chỉ cần facility (lãng phí tài nguyên, có thể cũng vi phạm minimum-necessary PII access nếu expert data không cần thiết bị đọc)

**Current Status:** 🔴 Not written
**Implementation Note:** Đây cũng là PRIVACY-relevant test — không đọc `expert_location_shares` khi Mother không yêu cầu.

---

### SUP166-TC-006 — supportType=EXPERT: chỉ gọi UC149, KHÔNG gọi UC63

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-402 (Proposed)`

**Test Steps:**
1. Act: gọi `searchNearbySupport(makeRequest(r -> r.setSupportType("EXPERT")), MOTHER_USER_ID)`
2. Assert: `verify(nearbyExpertService, times(1)).findNearby(any(), any())`; `verify(nearbyFacilityService, never()).findNearby(any(), any())`

**Expected Result (PASS):**
- Chỉ expert service được gọi

**Expected Result (FAIL):**
- Facility service vẫn được gọi không cần thiết

**Current Status:** 🔴 Not written
**Implementation Note:** Mirror SUP166-TC-005.

---

### SUP166-TC-007 — Facility service lỗi → facilityServiceDegraded=true, expert items vẫn đầy đủ, HTTP 200

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-403 (Proposed)`

**Preconditions:**
- Mock `INearbyFacilityService.findNearby()` throws `DataAccessException` (FX-166-004); mock `INearbyExpertService` OK (FX-166-002)

**Test Steps:**
1. Arrange: như trên
2. Act: gọi `searchNearbySupport(makeRequest(), MOTHER_USER_ID)`
3. Assert: method KHÔNG ném exception; response `facilityServiceDegraded == true`, `expertServiceDegraded == false`, `items` chứa CHỈ expert item(s)

**Expected Result (PASS):**
- Response 200 hợp lệ, `items` có expert data đầy đủ, cờ degraded set đúng cho phần facility

**Expected Result (FAIL):**
- Exception propagate ra ngoài (response 500), hoặc `items` rỗng dù expert service vẫn OK

**Current Status:** 🔴 Not written
**Implementation Note:** Mỗi `CompletableFuture` PHẢI có `.exceptionally()` handler riêng — không dùng try-catch bao ngoài cả 2 lời gọi (sẽ làm exception của 1 nguồn chặn luôn kết quả của nguồn kia).

---

### SUP166-TC-008 — Expert service lỗi → expertServiceDegraded=true, facility items vẫn đầy đủ, HTTP 200

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-403 (Proposed)`

**Preconditions:**
- Mock `INearbyExpertService.findNearby()` throws `DataAccessException` (FX-166-005); mock `INearbyFacilityService` OK (FX-166-001)

**Test Steps:**
1. Act: gọi `searchNearbySupport(makeRequest(), MOTHER_USER_ID)`
2. Assert: response `expertServiceDegraded == true`, `facilityServiceDegraded == false`, `items` chứa CHỈ facility item(s)

**Expected Result (PASS):**
- Response 200, facility data đầy đủ, cờ đúng

**Expected Result (FAIL):**
- Toàn bộ response fail dù facility service vẫn hoạt động bình thường

**Current Status:** 🔴 Not written
**Implementation Note:** Mirror SUP166-TC-007 — test tính đối xứng của partial degradation handling.

---

### SUP166-TC-009 — Cả 2 service lỗi → HTTP 503 MAP-403

**Severity:** `CRITICAL`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()` + `NearbySupportController`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`, `NearbySupportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-403 (Proposed)` / `§10 MAP-403`

**Preconditions:**
- Mock CẢ HAI service con throw exception (FX-166-004 + FX-166-005 cùng lúc)

**Test Steps:**
1. Act: gọi `searchNearbySupport(makeRequest(), MOTHER_USER_ID)`
2. Assert: `NearbySupportUnavailableException` được ném ra; ở Controller level, `GET /api/v1/map/support/nearby` trả HTTP `503` với `error.code == "MAP-403"`

**Expected Result (PASS):**
- 503 với error code đúng khi CẢ HAI nguồn không khả dụng — không có dữ liệu hữu ích nào để trả

**Expected Result (FAIL):**
- Trả 200 với `items:[]` (nhầm lẫn với AF2 empty state hợp lệ) khi thực ra là lỗi hệ thống, gây hiểu lầm cho Mother

**Current Status:** 🔴 Not written
**Implementation Note:** Phân biệt rõ "empty vì không có data" (AF2, 200) vs "empty vì hệ thống lỗi" (503) — 2 tình huống khác nhau về mặt UX/error handling.

---

### SUP166-TC-010 — userId từ JWT SecurityContext; UC166 KHÔNG tự ghi location_snapshots mới

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `NearbySupportController`, `NearbySupportService`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-404 (Proposed)`

**Test Steps:**
1. Arrange: `@WebMvcTest` với mock `INearbySupportService`; JWT có `sub=mother-001`
2. Act: `GET /api/v1/map/support/nearby?latitude=10.77&longitude=106.70&userId=00000000-0000-0000-0000-000000009999`
3. Assert: `verify(nearbySupportService).searchNearbySupport(any(), eq(mother001UUID))`; đồng thời verify `NearbySupportService` KHÔNG inject `ILocationSnapshotRepository` nào (kiểm tra qua code review/reflection — không có dependency mới)

**Expected Result (PASS):**
- Service dùng userId từ JWT; UC166 không có side-effect ghi DB riêng ngoài việc pass-through userId cho 2 service con

**Expected Result (FAIL):**
- userId từ query param được dùng, hoặc `NearbySupportService` tự ghi thêm 1 `location_snapshots` record với context_type mới không có trong ADR

**Current Status:** 🔴 Not written
**Implementation Note:** `NearbySupportService` constructor CHỈ nhận `INearbyFacilityService` + `INearbyExpertService` — không có `ILocationSnapshotRepository` injected.

---

### SUP166-TC-011 — Cả 2 nguồn rỗng → HTTP 200 với items:[] (AF2)

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbySupportController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `SRS §3.3.7.4 AF2`

**Preconditions:**
- Mock cả 2 service con trả empty response (FX-166's empty variants)

**Test Steps:**
1. Act: `GET /api/v1/map/support/nearby?latitude=10.77&longitude=106.70`
2. Assert: HTTP `200`; body `{"items":[],"facilityServiceDegraded":false,"expertServiceDegraded":false}`

**Expected Result (PASS):**
- Status 200, `items` mảng rỗng, cả 2 cờ degraded đều `false` (phân biệt rõ với SUP166-TC-009's 503)

**Expected Result (FAIL):**
- Status 404, hoặc nhầm lẫn với response lỗi (degraded=true dù thực ra chỉ là không có kết quả)

**Current Status:** 🔴 Not written
**Implementation Note:** "Không có kết quả" (data rỗng nhưng service hoạt động bình thường) PHẢI phân biệt rõ với "service lỗi" — 2 field `Degraded` chỉ set `true` khi có exception, KHÔNG set `true` chỉ vì `items` rỗng.

---

### SUP166-TC-012 — Consistency: FACILITY items khớp 1:1 với UC63's list

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-MAP-IMP-008 §4.2 NFR (Proposed)` / `ADR-MAP-401`

**Test Steps:**
1. Arrange: mock `INearbyFacilityService.findNearby()` trả 3 facility items cụ thể
2. Act: gọi `searchNearbySupport(makeRequest(r -> r.setSupportType("FACILITY")), MOTHER_USER_ID)`
3. Assert: mỗi `NearbySupportItem` (resultType=FACILITY) map 1:1 field-by-field từ `NearbyFacilityItem` gốc (facilityId→referenceId, name, distanceKm, estimatedTravelTimeMinutes, address, phone, facilityType) — không mất/thêm field, không sai giá trị

**Expected Result (PASS):**
- Mapping chính xác 100%, không có data loss hoặc transformation sai

**Expected Result (FAIL):**
- Field bị map sai (ví dụ `distanceKm` bị làm tròn khác, hoặc `facilityId` không map đúng sang `referenceId`)

**Current Status:** 🔴 Not written
**Implementation Note:** `NearbySupportMapper.toSupportItem(NearbyFacilityItem)` cần unit test riêng cho mapping logic.

---

### SUP166-TC-013 — Consistency: EXPERT items khớp 1:1 với UC149's list (bao gồm consent-gating)

**Severity:** `HIGH`
**Feature Under Test:** `NearbySupportService.searchNearbySupport()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbySupportServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-MAP-IMP-008 §4.2 NFR (Proposed)` / `ADR-MAP-401`

**Test Steps:**
1. Arrange: mock `INearbyExpertService.findNearby()` trả 2 expert items (đã được UC149 tự lọc theo consent/verification — UC166 KHÔNG biết/không cần biết chi tiết lọc)
2. Act: gọi `searchNearbySupport(makeRequest(r -> r.setSupportType("EXPERT")), MOTHER_USER_ID)`
3. Assert: mỗi `NearbySupportItem` (resultType=EXPERT) map 1:1 từ `NearbyExpertItem` gốc (expertProfileId→referenceId, specialty, ratingAvg, availabilityStatus, distanceKm, estimatedTravelTimeMinutes) — số lượng item khớp chính xác với những gì UC149 trả (không thêm/bớt)

**Expected Result (PASS):**
- Mapping chính xác 100%, số lượng item = số lượng UC149 trả về (UC166 không tự lọc thêm/bớt)

**Expected Result (FAIL):**
- UC166 vô tình lọc thêm hoặc bỏ sót expert item nào đó so với UC149's raw response — vi phạm nguyên tắc "pure aggregator, không tự filter"

**Current Status:** 🔴 Not written
**Implementation Note:** Test này đảm bảo UC166 KHÔNG re-filter kết quả của UC149 (ví dụ không tự loại expert theo tiêu chí riêng) — chỉ map và merge.

---

### SUP166-TC-014 — Không có JWT → 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `NearbySupportController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-404 (Proposed)` / `SRS E1`

**Test Steps (Attack Simulation):**
1. Act: `GET /api/v1/map/support/nearby?latitude=10.77&longitude=106.70` (không có `Authorization` header)
2. Assert: HTTP `401`, body `error.code == "IAM-001"`

**Expected Result (PASS = hệ thống an toàn):**
- `401 Unauthorized`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Request pass mà không cần xác thực

**Current Status:** 🔴 Not written

---

### SUP166-TC-015 — JWT sai role (không phải ROLE_MOTHER) → 403

**Severity:** `HIGH`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `NearbySupportController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbySupportControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-MAP-IMP-008 ADR-MAP-404 (Proposed)` / `§16 Auth Matrix`

**Test Steps:**
1. Arrange: JWT hợp lệ nhưng `role=ROLE_EXPERT`
2. Act: `GET /api/v1/map/support/nearby?latitude=10.77&longitude=106.70`
3. Assert: HTTP `403`, body `error.code == "MAP-404"`

**Expected Result (PASS):**
- 403 Forbidden — chỉ ROLE_MOTHER được phép

**Expected Result (FAIL):**
- Request pass cho role không được phép

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### SUP166-TC-INT-001 — Full HTTP flow end-to-end với DB thực, cả 2 service con thật

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: NearbySupportController → NearbySupportService → INearbyFacilityService (UC63, real) + INearbyExpertService (UC149, real) → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/map/NearbySupportIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tự động khi Spring context start
- Seed: 1 `care_facilities` record trong bán kính, 1 `expert_profiles`+`expert_location_shares` (VERIFIED, consent hợp lệ) trong bán kính

**Test Steps:**
1. Seed dữ liệu như trên
2. Call `GET /api/v1/map/support/nearby?latitude=10.7769&longitude=106.7009&radiusKm=5` với JWT ROLE_MOTHER
3. Assert response chứa CẢ HAI item (1 facility, 1 expert), sort theo distanceKm, cả 2 cờ degraded = false

**Expected Result (PASS):**
- `items.size() == 2`, 1 item `resultType=FACILITY`, 1 item `resultType=EXPERT`, thứ tự đúng theo distance
- Không có exception, cả 2 service con thật (UC63 + UC149) hoạt động đúng qua DB thực

**Expected Result (FAIL):**
- Thiếu 1 trong 2 loại item, hoặc thứ tự sai, hoặc exception ở tầng tích hợp (ví dụ Spring DI wiring sai)

**DB Assertion:**
```java
// Verify cả 2 nguồn dữ liệu đều tồn tại và đúng
List<CareFacility> facilities = careFacilityRepository.findAll();
assertThat(facilities).hasSize(1);
List<ExpertLocationShare> shares = expertLocationShareRepository.findAll();
assertThat(shares).hasSize(1);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `SUP166-TC-001` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-002` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-003` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-004` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-005` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-006` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-007` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-008` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-009` | `NearbySupportServiceTest.java` / `NearbySupportControllerTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-010` | `NearbySupportControllerTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-011` | `NearbySupportControllerTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-012` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-013` | `NearbySupportServiceTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-014` | `NearbySupportControllerTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-015` | `NearbySupportControllerTest.java` | `[ ]` | `[ ]` | |
| `SUP166-TC-INT-001` | `NearbySupportIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class NearbySupportService implements INearbySupportService {

    @Override
    public NearbySupportResponse searchNearbySupport(NearbySupportSearchRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `SUP166-TC-001` → `SUP166-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `SUP166-TC-009` → `SUP166-TC-015` | Controller mock trả stub / exception | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `SUP166-TC-INT-001` | Full stack throws | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MAP-IMP-008` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] **UC63 (`INearbyFacilityService`) VÀ UC149 (`INearbyExpertService`) đã implement và deploy** — UC166 không thể implement độc lập trước
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers, cả UC63+UC149 thật)
- [ ] Test coverage ≥ 80% lines cho `NearbySupportService`
- [ ] Không có business logic trong `NearbySupportController` (chỉ có validation + mapping)
- [ ] Không có PII (toạ độ chính xác) xuất hiện plaintext trong logs mức INFO
- [ ] Verify UC166 KHÔNG tự query `care_facilities`/`expert_location_shares` trực tiếp (SUP166-TC-001/002 pass)
- [ ] Verify gọi song song 2 service con (SUP166-TC-003 pass, latency đúng target)

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

- UC63 hoặc UC149 chưa deploy/available
- Product Owner chưa xác nhận giá trị enum `supportType` chính thức (§18 RG-7 của TDS)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Không có migration mới cho Draft này — không cần revert schema

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/map/controller/NearbySupportController.java
git checkout -- src/main/java/com/carebridge/backend/map/service/INearbySupportService.java
git checkout -- src/main/java/com/carebridge/backend/map/service/impl/NearbySupportService.java
git checkout -- src/main/java/com/carebridge/backend/map/dto/request/NearbySupportSearchRequest.java
git checkout -- src/main/java/com/carebridge/backend/map/dto/response/NearbySupportResponse.java
git checkout -- src/main/java/com/carebridge/backend/map/dto/response/NearbySupportItem.java
git checkout -- src/main/java/com/carebridge/backend/map/exception/NearbySupportUnavailableException.java
git checkout -- src/main/java/com/carebridge/backend/map/mapper/NearbySupportMapper.java
git checkout -- src/test/java/com/carebridge/backend/map/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ verify khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — SUP166-TC-007/008/009 tách rõ decision table của ADR-MAP-403 | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — Controller tests chỉ verify HTTP mapping/security | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — mọi type tham chiếu §8 TDS `CB-MAP-IMP-008`, UC63 `CB-MAP-IMP-001`, hoặc UC149 `CB-MAP-IMP-005` | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (chờ Red Gate verify khi implement) → TDD spec approved cho giai đoạn Draft

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none tại thời điểm viết Test-Spec)_ | — | — | — | — |

---

*TDD Spec v1.0 — Draft. Chưa Approved. Chờ TDS `CB-MAP-IMP-008` Approved (và UC63/UC149 implemented) trước khi bắt đầu Red Phase.*
