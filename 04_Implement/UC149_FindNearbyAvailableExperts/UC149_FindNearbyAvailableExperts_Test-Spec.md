# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC149 — Find Nearby Available Experts

**Document ID:** `CB-MAP-TDD-005`
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
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.7.1` — Functional requirements (UC-149)
- `04_Implement/UC149_FindNearbyAvailableExperts/UC149_FindNearbyAvailableExperts_TDS.md` (`CB-MAP-IMP-005`) — Technical Specification
- `04_Implement/UC63_FindNearbyCareFacility/UC63_FindNearbyCareFacility_TDS.md` — structural pattern reference
- `04_Implement/UC129_CalculateDistanceRouteAndETA/UC129_CalculateDistanceRouteAndETA_TDS.md` — `IMapProviderService` contract
- PDPA / Luật 91/2025 — Legal basis for location PII handling

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC149 Find Nearby Available Experts |

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
| **Feature / Gap ID** | `UC149` |
| **Module** | `Find Nearby Available Experts — map bounded context` |
| **Spec gốc** | `CB-MAP-IMP-005` |
| **Priority** | 🟡 P2 (Medium — theo SRS Priority field) |
| **Sprint** | `TV4-Lâm ownership — Expert Consultation & Map/Location, sprint TBD` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` (vị trí Mother + vị trí Expert) |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IMapProviderService (UC129)`, `expert_profiles`, `expert_location_shares` |
| **Downstream Consumers** | `UC153 Contact Nearby Expert`, `UC155 View Nearby Experts on Map` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MAP-IMP-005 §17`, `ADR-MAP-201`, `ADR-MAP-202`, `ADR-MAP-203`, `ADR-MAP-204` |
| **Constraints Injected** | C1 (bounding-box + VERIFIED + not-expired filter), C2 (delegate to IMapProviderService), C3 (accuracy_meters pass-through), C4 (userId from JWT), C5 (empty state = 200) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.7.1 dùng template chung, không nêu số cụ thể cho bán kính/max results | TDS `CB-MAP-IMP-005` §2 Open Item: đề xuất `radiusKm` default 5.0, `maxResults` default 20, kế thừa UC63 | Test dùng giá trị default đã "Proposed" trong TDS §8.1 DTO, KHÔNG bịa số khác |
| L2 | SRS không nói rõ "available" nghĩa là gì (availability_status enum không có CHECK constraint trong schema) | `expert_location_shares.availability_status varchar(20)` — free text, không enum cố định | Test KHÔNG assert giá trị cụ thể của `availabilityStatus` ngoài việc nó được pass-through nguyên trạng từ DB |
| L3 | TDS ADR-MAP-202 (precision display) là quyết định tạm — phụ thuộc UC147/UC148 chưa xác nhận được | Test chỉ verify hành vi ĐÃ CHỐT trong TDS hiện tại (pass-through `accuracy_meters`), đánh dấu rõ trong Oracle Source rằng đây là quyết định "Proposed", không phải "Accepted" | TC-COND-004 ghi rõ Oracle Source = `ADR-MAP-202 (Proposed, pending UC147/148 cross-check)` |
| L4 | UC63 dùng field tên `mapServiceDegraded` ở response cấp cao; UC129 dùng field `degraded` trong `RouteEstimate` — 2 tên khác nhau cho cùng khái niệm ở 2 cấp | TDS UC149 §9.2 xác nhận theo đúng UC63 pattern: response cấp cao dùng `mapServiceDegraded`, còn `RouteEstimate.degraded()` (UC129) chỉ dùng nội bộ trong Service để derive field đó | Test assert response JSON field `mapServiceDegraded` (không phải `degraded`) ở API/Controller level |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Find Nearby Available Experts bao gồm các layer:
├── Domain (NearbyExpertItem/Response mapping — pure logic, no deps)
├── Service (NearbyExpertService — mock IExpertLocationShareRepository, ILocationSnapshotRepository, IMapProviderService với Mockito)
├── Repository (IExpertLocationShareRepository — Testcontainers PostgreSQL cho bounding-box + JOIN query thực tế)
├── Controller (NearbyExpertController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, @SpringBootTest, full HTTP flow)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-149 §3.3.7.1` | Trigger, Normal Flow, AF1/AF2/AF3, E1/E2/E3, Business Rules BR-RBAC/BR-SAFETY |
| `CB-MAP-IMP-005 ADR-MAP-201` | Bounding-box + VERIFIED + not-expired filter, delegate Haversine to IMapProviderService |
| `CB-MAP-IMP-005 ADR-MAP-202` | accuracy_meters pass-through (Proposed, pending cross-check) |
| `CB-MAP-IMP-005 ADR-MAP-203` | IMapProviderService degraded handling, mapServiceDegraded field |
| `CB-MAP-IMP-005 ADR-MAP-204` | RBAC ROLE_MOTHER, userId from JWT, best-effort location_snapshots |
| `V1__init_schema.sql` (dòng 786-840) | expert_profiles.verification_status, expert_location_shares.expires_at/accuracy_meters/consent_reference structure |
| PDPA / BR-PRIVACY | Minimum-necessary access — expired shares excluded, no location fuzz beyond what's stored |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Search chỉ trả expert có `verification_status='VERIFIED'` | `NearbyExpertService.findNearby()` | `MAP149-TC-001` |
| TC-COND-002 | Search chỉ trả expert trong bounding-box/radiusKm | `NearbyExpertService.findNearby()` | `MAP149-TC-002` |
| TC-COND-003 | Distance calculation delegate sang `IMapProviderService.calculateHaversineDistance()`, KHÔNG tự tính | `NearbyExpertService.findNearby()` | `MAP149-TC-003` |
| TC-COND-004 | `accuracyMeters` trong response = pass-through từ `expert_location_shares.accuracy_meters` | `NearbyExpertMapper` | `MAP149-TC-004` |
| TC-COND-005 | Khi `IMapProviderService.calculateRoute()` trả `degraded=true` → response `mapServiceDegraded=true`, `estimatedTravelTimeMinutes=null`, vẫn 200 | `NearbyExpertService.findNearby()` | `MAP149-TC-005` |
| TC-COND-006 | `userId` lấy từ JWT SecurityContext, KHÔNG từ query param | `NearbyExpertController` | `MAP149-TC-006` |
| TC-COND-007 | `location_snapshots` ghi best-effort với `context_type='NEARBY_EXPERT_SEARCH'`; lỗi ghi KHÔNG chặn response | `NearbyExpertService.findNearby()` | `MAP149-TC-007` |
| TC-COND-008 | Không có expert nào match → HTTP 200 với `items:[]` (AF2), KHÔNG 404 | `NearbyExpertController` | `MAP149-TC-008` |
| TC-COND-009 | latitude/longitude thiếu hoặc invalid → HTTP 400 MAP-201 | `NearbyExpertController` | `MAP149-TC-009` |
| TC-COND-010 | Expert đã hết hạn share (`expires_at <= now()`) KHÔNG xuất hiện trong kết quả | `NearbyExpertService.findNearby()` | `MAP149-TC-010` |
| TC-COND-011 | Không có JWT / JWT không có ROLE_MOTHER → 401/403 | `NearbyExpertController` (security filter) | `MAP149-TC-011`, `MAP149-TC-012` |
| TC-COND-012 | Full HTTP flow end-to-end với DB thực (Testcontainers) | `NearbyExpertController` + `NearbyExpertService` + Repository | `MAP149-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | latitude/longitude (valid range vs out-of-range), radiusKm | Phân vùng input hợp lệ/không hợp lệ theo `@DecimalMin/@DecimalMax` |
| Boundary Value Analysis | `expires_at` = now() ± ε (boundary của "còn hiệu lực") | Đảm bảo filter `expires_at > now()` chính xác tại biên |
| State Transition Testing | `verification_status` (PENDING → VERIFIED → REJECTED) | Chỉ VERIFIED được hiển thị — test các trạng thái khác bị loại |
| Error Guessing | JWT thiếu, JWT sai role, query param injection | Security/RBAC coverage |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-149-001` | DB seed | `expert_profiles { verificationStatus: 'VERIFIED', specialty: 'Pediatrics', ratingAvg: 4.8 }` | Happy path — expert hợp lệ |
| `FX-149-002` | DB seed | `expert_profiles { verificationStatus: 'PENDING' }` | Verify filter loại bỏ expert chưa verify |
| `FX-149-003` | DB seed | `expert_location_shares { latitude: 10.7769, longitude: 106.7009, expiresAt: now()+1h, accuracyMeters: 50.0 }` | Happy path — share còn hiệu lực trong bán kính |
| `FX-149-004` | DB seed | `expert_location_shares { expiresAt: now()-1h }` (đã hết hạn) | Verify filter loại bỏ share hết hạn (TC-COND-010) |
| `FX-149-005` | DB seed | `expert_location_shares { latitude: 21.0285, longitude: 105.8542 }` (Hà Nội — ngoài bán kính khi search từ TP.HCM) | Verify bounding-box loại bỏ expert quá xa |
| `FX-149-006` | JWT | `{ sub: 'mother-001', role: 'ROLE_MOTHER' }` | Auth context hợp lệ |
| `FX-149-007` | JWT | `{ sub: 'expert-001', role: 'ROLE_EXPERT' }` | Auth context sai role (403 test) |
| `FX-149-008` | Mock | `IMapProviderService.calculateHaversineDistance() → 1.2` | Stub cho distance calculation |
| `FX-149-009` | Mock | `IMapProviderService.calculateRoute() → RouteEstimate(1.2, null, true)` (degraded) | Stub cho TC-COND-005 |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// NearbyExpertTestFactory.java
class NearbyExpertTestFactory {

    static final UUID VERIFIED_EXPERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID PENDING_EXPERT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000102");
    static final UUID MOTHER_USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000201");

    // Giá trị baseline hợp lệ — đồng bộ với FX-149-001/003
    static ExpertProfile makeVerifiedExpertProfile() {
        ExpertProfile p = new ExpertProfile();
        p.setExpertProfileId(VERIFIED_EXPERT_ID);
        p.setSpecialty("Pediatrics");
        p.setProfessionalTitle("BS. Nguyễn Văn A");
        p.setVerificationStatus("VERIFIED");
        p.setRatingAvg(new BigDecimal("4.8"));
        return p;
    }

    static ExpertProfile makeVerifiedExpertProfile(Consumer<ExpertProfile> overrides) {
        ExpertProfile p = makeVerifiedExpertProfile();
        overrides.accept(p);
        return p;
    }

    static ExpertLocationShare makeActiveLocationShare(UUID expertProfileId) {
        ExpertLocationShare s = new ExpertLocationShare();
        s.setLocationShareId(UUID.randomUUID());
        s.setExpertProfileId(expertProfileId);
        s.setLatitude(new BigDecimal("10.7769"));
        s.setLongitude(new BigDecimal("106.7009"));
        s.setAccuracyMeters(new BigDecimal("50.0"));
        s.setAvailabilityStatus("AVAILABLE");
        s.setSharedAt(Instant.now());
        s.setExpiresAt(Instant.now().plusSeconds(3600)); // +1h — still active
        return s;
    }

    static ExpertLocationShare makeActiveLocationShare(UUID expertProfileId, Consumer<ExpertLocationShare> overrides) {
        ExpertLocationShare s = makeActiveLocationShare(expertProfileId);
        overrides.accept(s);
        return s;
    }

    static ExpertLocationShare makeExpiredLocationShare(UUID expertProfileId) {
        return makeActiveLocationShare(expertProfileId, s -> s.setExpiresAt(Instant.now().minusSeconds(3600))); // -1h
    }

    static NearbyExpertSearchRequest makeSearchRequest() {
        NearbyExpertSearchRequest r = new NearbyExpertSearchRequest();
        r.setLatitude(10.7769);
        r.setLongitude(106.7009);
        r.setRadiusKm(5.0);
        r.setMaxResults(20);
        return r;
    }

    static NearbyExpertSearchRequest makeSearchRequest(Consumer<NearbyExpertSearchRequest> overrides) {
        NearbyExpertSearchRequest r = makeSearchRequest();
        overrides.accept(r);
        return r;
    }

    static RouteEstimate makeHappyRouteEstimate() {
        return new RouteEstimate(1.2, 6, false);
    }

    static RouteEstimate makeDegradedRouteEstimate() {
        return new RouteEstimate(1.2, null, true);
    }
}
```

---

### MAP149-TC-001 — Search chỉ trả expert có verification_status = VERIFIED

**Severity:** `HIGH`
**Feature Under Test:** `NearbyExpertService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-MAP-IMP-005 ADR-MAP-201 (Proposed)` / `V1__init_schema.sql` dòng 794 `expert_profiles.verification_status`

**Preconditions:**
- Fixture `FX-149-001` (VERIFIED expert) và `FX-149-002` (PENDING expert) đều có `expert_location_shares` còn hiệu lực trong bán kính

**Test Steps:**
1. Arrange: mock `IExpertLocationShareRepository.findActiveWithinBoundingBox()` — repository query đã filter `verification_status='VERIFIED'` ở JPQL (§8.2 TDS), trả về CHỈ record của `VERIFIED_EXPERT_ID`
2. Act: gọi `nearbyExpertService.findNearby(makeSearchRequest(), MOTHER_USER_ID)`
3. Assert: response `items` chỉ chứa 1 phần tử với `expertProfileId = VERIFIED_EXPERT_ID`

**Expected Result (PASS — hành vi đúng):**
- `items.size() == 1`, `items.get(0).getExpertProfileId().equals(VERIFIED_EXPERT_ID)`, `items.get(0).getVerificationStatus().equals("VERIFIED")`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Response chứa expert PENDING, hoặc repository method không được gọi với đúng filter

**Current Status:** 🔴 Not written
**Implementation Note:** Filter `verification_status='VERIFIED'` PHẢI nằm trong JPQL repository query (§8.2), không filter ở Service layer bằng Java stream (tránh load toàn bộ bảng).

---

### MAP149-TC-002 — Search chỉ trả expert trong bán kính radiusKm

**Severity:** `HIGH`
**Feature Under Test:** `NearbyExpertService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-MAP-IMP-005 ADR-MAP-201 (Proposed)`

**Preconditions:**
- Fixture `FX-149-003` (TP.HCM, trong bán kính) và `FX-149-005` (Hà Nội, ngoài bán kính) đều thuộc expert VERIFIED

**Test Steps:**
1. Arrange: mock repository trả cả 2 candidate (bounding-box coarse filter đã pass ở query, nhưng Hà Nội cách xa 1000+km); mock `IMapProviderService.calculateHaversineDistance()` trả `1.2` cho TP.HCM candidate, `1150.0` cho Hà Nội candidate
2. Act: gọi `findNearby(makeSearchRequest(r -> r.setRadiusKm(5.0)), MOTHER_USER_ID)`
3. Assert: response chỉ chứa candidate TP.HCM (distanceKm=1.2 <= 5.0), loại candidate Hà Nội (distanceKm=1150.0 > 5.0)

**Expected Result (PASS):**
- `items.size() == 1`, item còn lại có `distanceKm <= radiusKm`

**Expected Result (FAIL):**
- Response chứa cả candidate ngoài bán kính (thiếu post-filter sau khi tính Haversine chính xác)

**Current Status:** 🔴 Not written
**Implementation Note:** Bounding-box ở DB chỉ là coarse filter — Service PHẢI áp dụng post-filter `distanceKm <= radiusKm` sau khi có Haversine chính xác từ `IMapProviderService`.

---

### MAP149-TC-003 — Distance calculation delegate sang IMapProviderService, KHÔNG tự tính Haversine

**Severity:** `CRITICAL`
**Feature Under Test:** `NearbyExpertService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-MAP-IMP-005 ADR-MAP-201 (Proposed)` / `CB-MAP-IMP-000 §8.1 IMapProviderService.calculateHaversineDistance()`

**Preconditions:**
- Fixture `FX-149-001` + `FX-149-003`

**Test Steps:**
1. Arrange: mock `IMapProviderService` bean; mock `calculateHaversineDistance(...)` trả `1.2`
2. Act: gọi `findNearby(makeSearchRequest(), MOTHER_USER_ID)`
3. Assert: `verify(mapProviderService, atLeastOnce()).calculateHaversineDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble())`; response `distanceKm == 1.2` (giá trị đến từ mock, không phải tính lại trong Service)

**Expected Result (PASS):**
- Mock `calculateHaversineDistance()` được gọi; `distanceKm` trong response khớp giá trị mock trả về

**Expected Result (FAIL):**
- `IMapProviderService` không được gọi (Service tự implement Haversine riêng — vi phạm ADR-MAP-201)

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test kiểm tra ARCHITECTURE COMPLIANCE, không chỉ correctness — mock verify là bắt buộc, không chỉ assert giá trị output.

---

### MAP149-TC-004 — accuracyMeters trong response = pass-through từ DB

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyExpertMapper` (hoặc logic mapping trong `NearbyExpertService`)
**Test File:** `src/test/java/com/carebridge/backend/map/mapper/NearbyExpertMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-MAP-IMP-005 ADR-MAP-202 (Proposed, pending UC147/148 cross-check)`

**Preconditions:**
- Fixture `FX-149-003` với `accuracyMeters = 50.0`

**Test Steps:**
1. Arrange: `ExpertLocationShare` entity với `accuracyMeters = new BigDecimal("50.0")`
2. Act: map sang `NearbyExpertItem` qua `NearbyExpertMapper`
3. Assert: `item.getAccuracyMeters() == 50.0` — KHÔNG bị làm tròn/fuzz thêm

**Expected Result (PASS):**
- Giá trị `accuracyMeters` trong DTO khớp chính xác giá trị đã lưu trong entity

**Expected Result (FAIL):**
- Giá trị bị làm tròn/thay đổi so với DB (vi phạm ADR-MAP-202 hiện tại — lưu ý: nếu UC147/148 xác nhận cần fuzz, TC này phải được rewrite sau khi ADR-MAP-202 Supersede)

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ Test này gắn với quyết định TẠM THỜI (Proposed) — flag review lại nếu ADR-MAP-202 đổi sau khi UC147/148 công bố.

---

### MAP149-TC-005 — mapServiceDegraded=true khi IMapProviderService degraded

**Severity:** `HIGH`
**Feature Under Test:** `NearbyExpertService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-MAP-IMP-005 ADR-MAP-203 (Proposed)` / `CB-MAP-IMP-000 §8.1 RouteEstimate.degraded()`

**Preconditions:**
- Fixture `FX-149-001` + `FX-149-003`; mock `IMapProviderService.calculateRoute()` trả `RouteEstimate(1.2, null, true)` (FX-149-009)

**Test Steps:**
1. Arrange: mock `calculateHaversineDistance()` trả `1.2` (bình thường); mock `calculateRoute()` trả degraded estimate
2. Act: gọi `findNearby(makeSearchRequest(), MOTHER_USER_ID)`
3. Assert: response `mapServiceDegraded == true`; `items.get(0).getEstimatedTravelTimeMinutes() == null`; HTTP status vẫn phải là 200 (kiểm tra ở Controller test MAP149-TC-INT-001)

**Expected Result (PASS):**
- `mapServiceDegraded=true`, `estimatedTravelTimeMinutes=null`, response vẫn có đầy đủ `items` (không rỗng, không lỗi)

**Expected Result (FAIL):**
- Exception ném ra ngoài Service, hoặc response trả `items:[]` khi lẽ ra vẫn có data (chỉ thiếu ETA)

**Current Status:** 🔴 Not written
**Implementation Note:** Never let `IMapProviderService` failure abort the whole search — mirror UC63 §6.2 fallback behavior.

---

### MAP149-TC-006 — userId lấy từ JWT SecurityContext, không từ query param

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `NearbyExpertController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-MAP-IMP-005 ADR-MAP-204 (Proposed)`

**Preconditions:**
- JWT hợp lệ `FX-149-006` với `sub=mother-001`

**Test Steps:**
1. Arrange: `@WebMvcTest` với mock `INearbyExpertService`; JWT có `sub=mother-001`
2. Act: `GET /api/v1/map/experts/nearby?latitude=10.77&longitude=106.70&userId=00000000-0000-0000-0000-000000009999` (userId giả mạo trong query param, nếu client cố tình gửi)
3. Assert: `verify(nearbyExpertService).findNearby(any(), eq(UUID.fromString("mother-001-uuid-mapped")))` — controller PHẢI dùng userId từ JWT, bỏ qua query param `userId` nếu có

**Expected Result (PASS):**
- Service được gọi với userId từ SecurityContext, KHÔNG phải giá trị query param

**Expected Result (FAIL):**
- Service nhận userId từ query param (lỗ hổng cho phép giả mạo identity)

**Current Status:** 🔴 Not written
**Implementation Note:** DTO `NearbyExpertSearchRequest` (§8.1 TDS) KHÔNG có field `userId` — nếu Controller code có bind userId từ request param, đó là bug nghiêm trọng.

---

### MAP149-TC-007 — location_snapshots ghi best-effort, lỗi ghi không chặn response

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyExpertService.findNearby()`
**Test File:** `src/test/java/com/carebridge/backend/map/service/NearbyExpertServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-MAP-IMP-005 ADR-MAP-204 (Proposed)` / `CB-MAP-IMP-001 ADR-MAP-002 (mirror pattern)`

**Preconditions:**
- Fixture `FX-149-001` + `FX-149-003`; mock `ILocationSnapshotRepository.save()` throw `DataAccessException`

**Test Steps:**
1. Arrange: mock snapshot repository ném exception khi `save()` được gọi
2. Act: gọi `findNearby(makeSearchRequest(), MOTHER_USER_ID)`
3. Assert: method KHÔNG ném exception ra ngoài; response vẫn trả `items` đầy đủ như happy path

**Expected Result (PASS):**
- Response thành công dù `location_snapshots` ghi thất bại — lỗi được catch nội bộ (best-effort, log warning)

**Expected Result (FAIL):**
- `findNearby()` ném exception ra ngoài, response 500 dù search logic chính vẫn hoạt động bình thường

**Current Status:** 🔴 Not written
**Implementation Note:** Wrap `locationSnapshotRepository.save()` trong try-catch riêng, không để propagate lên Controller.

---

### MAP149-TC-008 — Empty state trả HTTP 200 với items:[], không 404

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyExpertController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `SRS §3.3.7.1 AF2` / `CB-MAP-IMP-005 §10 (MAP-203 note)`

**Preconditions:**
- Mock Service trả `NearbyExpertListResponse(items=[], mapServiceDegraded=false)`

**Test Steps:**
1. Arrange: mock `INearbyExpertService.findNearby()` trả response rỗng
2. Act: `GET /api/v1/map/experts/nearby?latitude=10.77&longitude=106.70`
3. Assert: HTTP status `200`; response body `{"items":[],"mapServiceDegraded":false}`

**Expected Result (PASS):**
- Status 200, body `items` là mảng rỗng

**Expected Result (FAIL):**
- Status 404 (vi phạm SRS AF2 — "system displays an empty state", không phải lỗi)

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### MAP149-TC-009 — Invalid latitude/longitude trả 400 MAP-201

**Severity:** `MEDIUM`
**Feature Under Test:** `NearbyExpertController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-MAP-IMP-005 §9.2, §10 MAP-201`

**Preconditions:**
- Không cần seed DB

**Test Steps:**
1. Act: `GET /api/v1/map/experts/nearby?latitude=999&longitude=106.70` (latitude ngoài [-90,90])
2. Assert: HTTP `400`, body `error.code == "MAP-201"`

**Expected Result (PASS):**
- 400 với error code chính xác, `details` chỉ rõ field `latitude`

**Expected Result (FAIL):**
- Request được chấp nhận (thiếu `@DecimalMin/@DecimalMax` validation) hoặc trả sai error code

**Current Status:** 🔴 Not written
**Implementation Note:** —

---

### MAP149-TC-010 — Expert đã hết hạn share KHÔNG xuất hiện trong kết quả

**Severity:** `CRITICAL`
**Legal:** `PDPA — minimum-necessary access, không hiển thị vị trí quá thời hạn cho phép`
**Feature Under Test:** `IExpertLocationShareRepository.findActiveWithinBoundingBox()`
**Test File:** `src/test/java/com/carebridge/backend/map/repository/ExpertLocationShareRepositoryIT.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-MAP-IMP-005 ADR-MAP-201 (Proposed)` / `V1__init_schema.sql` dòng 836 `expert_location_shares.expires_at`

**Preconditions:**
- Testcontainers PostgreSQL; seed `FX-149-001` (VERIFIED expert) với `FX-149-004` (`expiresAt = now()-1h`, đã hết hạn)

**Test Steps:**
1. Arrange: insert expert VERIFIED + location share đã hết hạn qua JPA
2. Act: gọi `expertLocationShareRepository.findActiveWithinBoundingBox(minLat, maxLat, minLng, maxLng, null)`
3. Assert: kết quả KHÔNG chứa record của expert đã hết hạn

**Expected Result (PASS):**
- Query result rỗng hoặc không chứa share hết hạn

**Expected Result (FAIL):**
- Share hết hạn vẫn xuất hiện trong kết quả (thiếu điều kiện `expires_at > CURRENT_TIMESTAMP` trong JPQL)

**Current Status:** 🔴 Not written
**Implementation Note:** Test dùng DB thực (Testcontainers) vì đây là behavior phụ thuộc SQL `CURRENT_TIMESTAMP` — không mock được chính xác.

---

### SECURITY TEST CASES

---

### MAP149-TC-011 — Không có JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** `BR-RBAC`
**Feature Under Test:** `Spring Security filter chain trên /api/v1/map/experts/nearby`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertControllerSecurityIT.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- Không có Authorization header

**Test Steps (Attack Simulation):**
1. Gửi `GET /api/v1/map/experts/nearby?latitude=10.77&longitude=106.70` KHÔNG kèm `Authorization` header
2. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- `401 Unauthorized`, body `error.code == "IAM-001"`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Request được xử lý bình thường, trả data mà không cần auth

**Current Status:** 🔴 Not written

---

### MAP149-TC-012 — JWT hợp lệ nhưng sai role (ROLE_EXPERT) → 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-863 — Incorrect Authorization`
**Legal:** `BR-RBAC`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')") trên NearbyExpertController`
**Test File:** `src/test/java/com/carebridge/backend/map/controller/NearbyExpertControllerSecurityIT.java`
**TDD Phase:** 🔴 RED

**Preconditions:**
- JWT hợp lệ `FX-149-007` với `role=ROLE_EXPERT`

**Test Steps (Attack Simulation):**
1. Gửi `GET /api/v1/map/experts/nearby?latitude=10.77&longitude=106.70` với JWT role `ROLE_EXPERT`
2. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden`, body `error.code == "MAP-204"`

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Request được xử lý, Expert account nhận được danh sách expert khác (data không dành cho role này)

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### MAP149-TC-INT-001 — Full flow: Mother search → 200 với expert hợp lệ, DB thực

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: HTTP request → Controller → Service → Repository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/map/NearbyExpertIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, 002, 008, 010`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tự động khi Spring context start
- Seed: `FX-149-001` (VERIFIED expert), `FX-149-003` (active share, TP.HCM), `FX-149-002` (PENDING expert) + active share (để verify bị loại), `FX-149-004` (expired share cho VERIFIED expert khác)

**Test Steps:**
1. Seed 3 expert: 1 VERIFIED+active-share (phải xuất hiện), 1 PENDING+active-share (phải bị loại), 1 VERIFIED+expired-share (phải bị loại)
2. Gọi `GET /api/v1/map/experts/nearby?latitude=10.7769&longitude=106.7009&radiusKm=5` với JWT `ROLE_MOTHER` hợp lệ
3. Assert DB state và response

**Expected Result (PASS):**
- HTTP 200; `items.size() == 1`; item duy nhất khớp expert VERIFIED + active-share; `location_snapshots` có 1 record mới với `context_type='NEARBY_EXPERT_SEARCH'`

**Expected Result (FAIL):**
- Response chứa expert PENDING hoặc expert với share hết hạn; hoặc `location_snapshots` không được ghi

**DB Assertion:**
```java
List<LocationSnapshot> snapshots = locationSnapshotRepository.findByContextType("NEARBY_EXPERT_SEARCH");
assertThat(snapshots).hasSize(1);
assertThat(snapshots.get(0).getUserId()).isEqualTo(MOTHER_USER_ID);
assertThat(snapshots.get(0).getExpiresAt()).isAfter(Instant.now());
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MAP149-TC-001` | `NearbyExpertServiceTest.java` | `[ ]` | `—` | — |
| `MAP149-TC-002` | `NearbyExpertServiceTest.java` | `[ ]` | `—` | — |
| `MAP149-TC-003` | `NearbyExpertServiceTest.java` | `[ ]` | `—` | — |
| `MAP149-TC-004` | `NearbyExpertMapperTest.java` | `[ ]` | `—` | — |
| `MAP149-TC-005` | `NearbyExpertServiceTest.java` | `[ ]` | `—` | — |
| `MAP149-TC-006` | `NearbyExpertControllerTest.java` | `[ ]` | `—` | — |
| `MAP149-TC-007` | `NearbyExpertServiceTest.java` | `[ ]` | `—` | — |
| `MAP149-TC-008` | `NearbyExpertControllerTest.java` | `[ ]` | `—` | — |
| `MAP149-TC-009` | `NearbyExpertControllerTest.java` | `[ ]` | `—` | — |
| `MAP149-TC-010` | `ExpertLocationShareRepositoryIT.java` | `[ ]` | `—` | — |
| `MAP149-TC-011` | `NearbyExpertControllerSecurityIT.java` | `[ ]` | `—` | — |
| `MAP149-TC-012` | `NearbyExpertControllerSecurityIT.java` | `[ ]` | `—` | — |
| `MAP149-TC-INT-001` | `NearbyExpertIntegrationTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class NearbyExpertService implements INearbyExpertService {

    @Override
    public NearbyExpertListResponse findNearby(NearbyExpertSearchRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```java
// Red Phase — Repository stub (nếu cần custom impl thay vì derived query)
@Repository
public class ExpertLocationShareRepositoryImpl {
    public List<ExpertLocationShareProjection> findActiveWithinBoundingBox(
            BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng, String specialty) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MAP149-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `MAP149-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-009` | Validation stub absent | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-011` | Security filter not configured | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-012` | Security filter not configured | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MAP149-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___` (điền khi implement)
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-MAP-IMP-005` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với TV4-Lâm / Tech Lead
- [ ] UC129 (`IMapProviderService`) đã implement — dependency bắt buộc trước khi UC149 có thể compile
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị
- [ ] **ADR-MAP-202 đã được review lại sau khi UC147/UC148 công bố TDS** (xem Open Item trong TDS §2)

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `NearbyExpertService`
- [ ] Không có business logic trong `NearbyExpertController` (chỉ có validation + mapping)
- [ ] Không có PII (toạ độ chính xác) xuất hiện plaintext trong logs mức INFO
- [ ] Verify Haversine calculation delegate 100% sang `IMapProviderService`, không có implementation riêng trong `map.expert` package

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

- UC129 (`IMapProviderService`) chưa deploy/available
- UC147/UC148 chưa xác nhận precision-display policy (ADR-MAP-202) — có thể tiếp tục implement với giả định hiện tại nhưng PHẢI ghi rõ trong PR description là "pending cross-check"
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Không có migration mới cho Draft này — không cần revert schema

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/map/controller/NearbyExpertController.java
git checkout -- src/main/java/com/carebridge/backend/map/service/
git checkout -- src/main/java/com/carebridge/backend/map/repository/IExpertLocationShareRepository.java
git checkout -- src/main/java/com/carebridge/backend/map/dto/
git checkout -- src/test/java/com/carebridge/backend/map/

# Nếu migration V20260705150000 (geo-index) đã được tạo và chạy:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_expert_location_shares_geo;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260705150000';"
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ verify khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — MAP149-TC-004 đánh dấu rõ "Proposed, pending cross-check" | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — Controller tests chỉ verify HTTP mapping/security | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — mọi type tham chiếu §8 TDS `CB-MAP-IMP-005` hoặc UC129 `CB-MAP-IMP-000` | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (chờ Red Gate verify khi implement) → TDD spec approved cho giai đoạn Draft

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none tại thời điểm viết Test-Spec)_ | — | — | — | — |

---

*TDD Spec v1.0 — Draft. Chưa Approved. Chờ TDS `CB-MAP-IMP-005` Approved trước khi bắt đầu Red Phase.*
