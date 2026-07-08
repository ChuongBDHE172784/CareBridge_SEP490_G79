# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC165 — Filter Expert

**Document ID:** `CB-EXP-TDD-002`
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
- `02_Requirements/SRS/3_Functional_Specification.md §3.3.9.2` — Functional requirements (UC-165)
- `04_Implement/UC165_FilterExpert/UC165_FilterExpert_TDS.md` (`CB-EXP-IMP-002`) — Technical Specification
- `04_Implement/UC149_FindNearbyAvailableExperts/UC149_FindNearbyAvailableExperts_TDS.md` — consent-gated distance pattern reference
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
| 2026-07-03 | AI Agent — Test Designer | Khởi tạo tài liệu — TDD spec cho UC165 Filter Expert |

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
| **Feature / Gap ID** | `UC165` |
| **Module** | `Filter Expert — expert bounded context` |
| **Spec gốc** | `CB-EXP-IMP-002` |
| **Priority** | 🟠 P1 (High — theo SRS Priority field) |
| **Sprint** | `TV4-Lâm ownership — Verified Expert Network & Map/Location, sprint TBD` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` (khi distance filter được dùng — vị trí Mother + vị trí Expert) |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `IMapProviderService (UC129)`, `expert_profiles`, `expert_availability`, `expert_consultation_prices`, `expert_location_shares` |
| **Downstream Consumers** | Mobile `expertDirectory` feature, tương lai booking flow |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXP-IMP-002 §17`, `ADR-EXP-201`, `ADR-EXP-202`, `ADR-EXP-203`, `ADR-EXP-204` |
| **Constraints Injected** | C1 (Haversine delegate to IMapProviderService), C2 (consent-gated distance: soft vs hard filter), C3 (no TrackAsia call), C4 (userId from JWT), C5 (empty state = 200) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS §3.3.9.2 dùng template chung, không nêu số cụ thể cho default page size, sort order, hay enum values | TDS `CB-EXP-IMP-002` §2 Open Item (RG-2): đề xuất `pageSize` default 20, `sortBy` default `RATING_DESC` | Test dùng giá trị default đã "Proposed" trong TDS §8.1 DTO, KHÔNG bịa số khác |
| L2 | SRS không nói rõ enum values cho `modality`/`onlineStatus` (cột free-text, không CHECK constraint) | `expert_availability.channel_type varchar(30)`, `expert_location_shares.availability_status varchar(20)` — free text | Test dùng giá trị literal ví dụ (`VIDEO`, `AVAILABLE`) làm test data nhưng KHÔNG assert enum closed-set — chỉ assert exact-match filter behavior |
| L3 | TDS ADR-EXP-202 phân biệt "soft filter" (distance là nice-to-have, `maxDistanceKm` không set) và "hard filter" (`maxDistanceKm` set) — dễ nhầm lẫn nếu implement sai | Test tách rõ 2 test case riêng (EXP165-TC-005 vs EXP165-TC-006) để không lẫn 2 nhánh logic | Oracle Source ghi rõ "ADR-EXP-202 nhánh A (soft)" vs "nhánh B (hard)" |
| L4 | `expert_profiles.rating_avg` là pre-aggregated column — nếu implement sai (JOIN `expert_reviews` runtime để tính lại), sẽ chậm và có thể lệch với giá trị đã lưu | TDS ADR-EXP-201 xác nhận rõ dùng `rating_avg` trực tiếp | Test mock/seed `rating_avg` khác với tổng hợp thủ công từ `expert_reviews` để phát hiện nếu code tính lại sai |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Filter Expert bao gồm các layer:
├── Domain (ExpertFilterItem/Response mapping — pure logic, no deps)
├── Service (ExpertFilterService — mock IExpertFilterRepository, IMapProviderService, ILocationSnapshotRepository với Mockito)
├── Repository (IExpertFilterRepository — Testcontainers PostgreSQL cho combined WHERE query thực tế)
├── Controller (ExpertFilterController — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL, @SpringBootTest, full HTTP flow)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-165 §3.3.9.2` | Trigger, Normal Flow, AF1/AF2/AF3, E1/E2/E3, Business Rules BR-RBAC/BR-PRIVACY/BR-SAFETY/BR-CONSULTATION |
| `CB-EXP-IMP-002 ADR-EXP-201` | Combined single query cho 6 tiêu chí phi-vị-trí, Haversine post-filter delegate IMapProviderService |
| `CB-EXP-IMP-002 ADR-EXP-202` | Consent-gated distance — soft filter (không maxDistanceKm) vs hard filter (có maxDistanceKm) |
| `CB-EXP-IMP-002 ADR-EXP-203` | KHÔNG gọi calculateRoute()/TrackAsia |
| `CB-EXP-IMP-002 ADR-EXP-204` | RBAC (mọi role hợp lệ), userId từ JWT |
| `V1__init_schema.sql` (dòng 786-975) | expert_profiles/expert_availability/expert_consultation_prices/expert_location_shares structure |
| PDPA / BR-PRIVACY | Minimum-necessary access — distance chỉ dùng khi Mother chủ động cung cấp toạ độ |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Filter theo `specialty` exact-match | `ExpertFilterService.filterExperts()` | `EXP165-TC-001` |
| TC-COND-002 | Filter theo `feeMin`/`feeMax` chỉ áp dụng giá `status='ACTIVE'` | `ExpertFilterService.filterExperts()` | `EXP165-TC-002` |
| TC-COND-003 | Filter theo `ratingMin` dùng `expert_profiles.rating_avg` (pre-aggregated), KHÔNG tính lại từ `expert_reviews` | `ExpertFilterService.filterExperts()` | `EXP165-TC-003` |
| TC-COND-004 | Distance calculation delegate sang `IMapProviderService.calculateHaversineDistance()`, KHÔNG tự tính | `ExpertFilterService.filterExperts()` | `EXP165-TC-004` |
| TC-COND-005 | Soft filter: `maxDistanceKm` KHÔNG set → expert thiếu consent vẫn xuất hiện, `distanceKm=null` | `ExpertFilterService.filterExperts()` | `EXP165-TC-005` |
| TC-COND-006 | Hard filter: `maxDistanceKm` SET → expert thiếu consent hợp lệ bị loại | `ExpertFilterService.filterExperts()` | `EXP165-TC-006` |
| TC-COND-007 | `IMapProviderService.calculateRoute()` KHÔNG bao giờ được gọi trong UC165 | `ExpertFilterService.filterExperts()` | `EXP165-TC-007` |
| TC-COND-008 | `userId` lấy từ JWT SecurityContext, KHÔNG từ query param | `ExpertFilterController` | `EXP165-TC-008` |
| TC-COND-009 | Filter combination boundary: `feeMin == feeMax` (khoảng giá đơn điểm) | `ExpertFilterService.filterExperts()` | `EXP165-TC-009` |
| TC-COND-010 | Filter combination boundary: `ratingMin = 5.0` (giá trị max hợp lệ) | `ExpertFilterService.filterExperts()` | `EXP165-TC-010` |
| TC-COND-011 | Filter combination boundary: `maxDistanceKm` đúng bằng `distanceKm` tính được (boundary <=) | `ExpertFilterService.filterExperts()` | `EXP165-TC-011` |
| TC-COND-012 | Không có expert nào match → HTTP 200 với `items:[]` (AF2), KHÔNG 404 | `ExpertFilterController` | `EXP165-TC-012` |
| TC-COND-013 | `feeMin > feeMax` hoặc `maxDistanceKm` set nhưng thiếu lat/lng → HTTP 400 EXP-201 | `ExpertFilterController` | `EXP165-TC-013`, `EXP165-TC-014` |
| TC-COND-014 | Không có JWT → 401 | `ExpertFilterController` (security filter) | `EXP165-TC-015` |
| TC-COND-015 | Full HTTP flow end-to-end với DB thực (Testcontainers) — kết hợp cả 7 tiêu chí cùng lúc | `ExpertFilterController` + `ExpertFilterService` + Repository | `EXP165-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `feeMin/feeMax` (valid range vs invalid range), `ratingMin` (0-5 vs out-of-range) | Phân vùng input hợp lệ/không hợp lệ theo `@DecimalMin/@DecimalMax` |
| Boundary Value Analysis | `ratingMin=5.0`, `feeMin==feeMax`, `distanceKm == maxDistanceKm` | Đảm bảo filter chính xác tại biên (`<=` không phải `<`) |
| State Transition Testing | `expert_availability.status` (AVAILABLE/BOOKED/CANCELLED — free text nhưng dùng cho `availableOnly` filter) | Chỉ slot hợp lệ được tính vào `hasAvailableSlot` |
| Error Guessing | JWT thiếu, `feeMin>feeMax`, `maxDistanceKm` không kèm lat/lng, SQL injection qua `specialty` param | Security/Validation coverage |
| Decision Table | Kết hợp `maxDistanceKm` present/absent × consent present/absent (4 tổ hợp — ADR-EXP-202) | Đảm bảo cover đầy đủ soft/hard filter logic |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-165-001` | DB seed | `expert_profiles { specialty: 'Pediatrics', ratingAvg: 4.8, verificationStatus: 'VERIFIED' }` | Happy path — expert hợp lệ |
| `FX-165-002` | DB seed | `expert_consultation_prices { priceAmount: 150000, status: 'ACTIVE' }` | Happy path — giá đang hiệu lực |
| `FX-165-003` | DB seed | `expert_consultation_prices { priceAmount: 999999, status: 'INACTIVE' }` (giá cũ, hết hiệu lực) | Verify filter loại giá không ACTIVE (TC-COND-002) |
| `FX-165-004` | DB seed | `expert_availability { channelType: 'VIDEO', status: 'AVAILABLE', startAt: now()+1h }` | Happy path — modality + availability |
| `FX-165-005` | DB seed | `expert_location_shares { latitude: 10.7769, longitude: 106.7009, consentReference: <uuid>, expiresAt: now()+1h }` | Consent hợp lệ — dùng cho distance test |
| `FX-165-006` | DB seed | `expert_location_shares { consentReference: null }` (thiếu consent) | Verify soft/hard filter behavior (TC-COND-005/006) |
| `FX-165-007` | DB seed | `expert_profiles { ratingAvg: 3.5 }` (thấp hơn threshold test) | Verify `ratingMin` loại expert không đạt |
| `FX-165-008` | JWT | `{ sub: 'mother-001', role: 'ROLE_MOTHER' }` | Auth context hợp lệ |
| `FX-165-009` | Mock | `IMapProviderService.calculateHaversineDistance() → 8.5` | Stub cho distance calculation |
| `FX-165-010` | Request | `ExpertFilterRequest { specialty: 'Pediatrics', feeMin: 100000, feeMax: 500000, ratingMin: 4.0, latitude: 10.7769, longitude: 106.7009, maxDistanceKm: 10.0 }` | Combined filter happy path |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeXxx()
// ═══════════════════════════════════════════════════════════

// ExpertFilterTestFactory.java
class ExpertFilterTestFactory {

    static final UUID VERIFIED_EXPERT_ID   = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID LOW_RATING_EXPERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");
    static final UUID NO_CONSENT_EXPERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000303");
    static final UUID MOTHER_USER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000401");

    static ExpertFilterCandidate makeCandidate() {
        ExpertFilterCandidate c = new ExpertFilterCandidate();
        c.setExpertProfileId(VERIFIED_EXPERT_ID);
        c.setSpecialty("Pediatrics");
        c.setProfessionalTitle("BS. Nguyễn Văn A");
        c.setRatingAvg(new BigDecimal("4.8"));
        c.setVerificationStatus("VERIFIED");
        c.setChannelTypes(List.of("VIDEO", "CHAT"));
        c.setMinFee(new BigDecimal("150000"));
        c.setHasAvailableSlot(true);
        c.setLocationLatitude(new BigDecimal("10.7769"));
        c.setLocationLongitude(new BigDecimal("106.7009"));
        c.setLocationAccuracyMeters(50.0);
        c.setLocationConsentValid(true);
        c.setOnlineStatus("AVAILABLE");
        return c;
    }

    static ExpertFilterCandidate makeCandidate(Consumer<ExpertFilterCandidate> overrides) {
        ExpertFilterCandidate c = makeCandidate();
        overrides.accept(c);
        return c;
    }

    static ExpertFilterCandidate makeCandidateWithoutConsent() {
        return makeCandidate(c -> {
            c.setExpertProfileId(NO_CONSENT_EXPERT_ID);
            c.setLocationConsentValid(false);
            c.setLocationLatitude(null);
            c.setLocationLongitude(null);
        });
    }

    static ExpertFilterRequest makeRequest() {
        ExpertFilterRequest r = new ExpertFilterRequest();
        r.setSpecialty("Pediatrics");
        r.setSortBy("RATING_DESC");
        r.setPage(0);
        r.setPageSize(20);
        return r;
    }

    static ExpertFilterRequest makeRequest(Consumer<ExpertFilterRequest> overrides) {
        ExpertFilterRequest r = makeRequest();
        overrides.accept(r);
        return r;
    }

    static ExpertFilterRequest makeRequestWithDistance(Double maxDistanceKm) {
        return makeRequest(r -> {
            r.setLatitude(10.7769);
            r.setLongitude(106.7009);
            r.setMaxDistanceKm(maxDistanceKm);
        });
    }
}
```

---

### EXP165-TC-001 — Filter theo specialty exact-match

**Severity:** `HIGH`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-201 (Proposed)` / `V1__init_schema.sql` dòng 789 `expert_profiles.specialty`

**Preconditions:**
- Fixture `FX-165-001` (specialty=Pediatrics) và 1 candidate khác `specialty=Cardiology`

**Test Steps:**
1. Arrange: mock `IExpertFilterRepository.findByCriteria()` — repository query đã filter `specialty='Pediatrics'` ở JPQL/Criteria (§8.2 TDS), trả về CHỈ candidate Pediatrics
2. Act: gọi `expertFilterService.filterExperts(makeRequest(), MOTHER_USER_ID)`
3. Assert: response `items` chỉ chứa candidate `specialty='Pediatrics'`

**Expected Result (PASS — hành vi đúng):**
- `items.size() == 1`, `items.get(0).getSpecialty().equals("Pediatrics")`

**Expected Result (FAIL — dấu hiệu lỗi):**
- Response chứa candidate Cardiology, hoặc repository method không được gọi với đúng filter criteria

**Current Status:** 🔴 Not written
**Implementation Note:** Filter `specialty` PHẢI nằm trong query repository (§8.2), không filter ở Service layer bằng Java stream.

---

### EXP165-TC-002 — Filter theo feeMin/feeMax chỉ áp dụng giá ACTIVE

**Severity:** `HIGH`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-201 (Proposed)` / `BR-CONSULTATION` / `V1__init_schema.sql` dòng 859-874 `expert_consultation_prices`

**Preconditions:**
- Fixture `FX-165-002` (giá 150000, ACTIVE) và `FX-165-003` (giá 999999, INACTIVE) thuộc CÙNG 1 expert

**Test Steps:**
1. Arrange: mock repository — query JOIN `expert_consultation_prices WHERE status='ACTIVE'`, trả về `minFee=150000` (bỏ qua giá INACTIVE 999999)
2. Act: gọi `filterExperts(makeRequest(r -> {r.setFeeMin(BigDecimal.valueOf(100000)); r.setFeeMax(BigDecimal.valueOf(500000));}), MOTHER_USER_ID)`
3. Assert: expert xuất hiện trong kết quả (vì giá ACTIVE 150000 nằm trong range, mặc dù giá INACTIVE 999999 nằm ngoài range)

**Expected Result (PASS):**
- Expert match filter dựa trên giá ACTIVE, KHÔNG bị ảnh hưởng bởi giá INACTIVE

**Expected Result (FAIL):**
- Expert bị loại nhầm (query dùng giá INACTIVE để so sánh) hoặc `minFee` trả về giá trị 999999 sai

**Current Status:** 🔴 Not written
**Implementation Note:** WHERE clause repository PHẢI có `status='ACTIVE'` explicit trong JOIN condition với `expert_consultation_prices`.

---

### EXP165-TC-003 — ratingMin dùng rating_avg pre-aggregated, KHÔNG tính lại từ expert_reviews

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-201 (Proposed)` / `V1__init_schema.sql` dòng 797 `expert_profiles.rating_avg`

**Preconditions:**
- Fixture `FX-165-001` với `rating_avg=4.8` đã lưu sẵn trên `expert_profiles` (giả lập trường hợp `expert_reviews` runtime aggregate ra giá trị khác, ví dụ 4.5, để phát hiện nếu code tính lại sai)

**Test Steps:**
1. Arrange: mock repository trả `ratingAvg=4.8` (từ cột `expert_profiles.rating_avg`) — KHÔNG mock/stub bất kỳ JOIN nào tới `expert_reviews`
2. Act: gọi `filterExperts(makeRequest(r -> r.setRatingMin(BigDecimal.valueOf(4.5))), MOTHER_USER_ID)`
3. Assert: `verify(expertFilterRepository, never()).findByCriteria(argThat(c -> /* criteria chứa reference tới expert_reviews join */))`; response item `ratingAvg == 4.8`

**Expected Result (PASS):**
- Response dùng đúng `rating_avg` cột đã pre-aggregate, không có JOIN runtime `expert_reviews`

**Expected Result (FAIL):**
- Code JOIN `expert_reviews` để tính lại rating trung bình runtime — vi phạm ADR-EXP-201, hiệu năng kém

**Current Status:** 🔴 Not written
**Implementation Note:** `IExpertFilterRepository.findByCriteria()` KHÔNG được JOIN `expert_reviews` bảng — chỉ đọc `expert_profiles.rating_avg`.

---

### EXP165-TC-004 — Distance calculation delegate sang IMapProviderService, KHÔNG tự tính Haversine

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-201 (Proposed)` / `CB-MAP-IMP-000 §8.1 IMapProviderService.calculateHaversineDistance()`

**Preconditions:**
- Fixture `FX-165-001` với `locationConsentValid=true` (`FX-165-005`)

**Test Steps:**
1. Arrange: mock `IMapProviderService` bean; mock `calculateHaversineDistance(...)` trả `8.5` (FX-165-009)
2. Act: gọi `filterExperts(makeRequestWithDistance(10.0), MOTHER_USER_ID)`
3. Assert: `verify(mapProviderService, atLeastOnce()).calculateHaversineDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble())`; response `distanceKm == 8.5` (giá trị đến từ mock)

**Expected Result (PASS):**
- Mock `calculateHaversineDistance()` được gọi; `distanceKm` trong response khớp giá trị mock trả về

**Expected Result (FAIL):**
- `IMapProviderService` không được gọi (Service tự implement Haversine riêng — vi phạm ADR-EXP-201)

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test kiểm tra ARCHITECTURE COMPLIANCE — mock verify là bắt buộc, không chỉ assert giá trị output.

---

### EXP165-TC-005 — Soft filter: maxDistanceKm không set, expert thiếu consent vẫn xuất hiện với distanceKm=null

**Severity:** `HIGH`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-202 nhánh A — soft filter (Proposed)`

**Preconditions:**
- Candidate `NO_CONSENT_EXPERT_ID` (`makeCandidateWithoutConsent()`) match tất cả 6 tiêu chí phi-vị-trí khác

**Test Steps:**
1. Arrange: mock repository trả về candidate không có consent hợp lệ (`locationConsentValid=false`)
2. Act: gọi `filterExperts(makeRequest(r -> {r.setLatitude(10.7769); r.setLongitude(106.7009);}), MOTHER_USER_ID)` — **KHÔNG** set `maxDistanceKm`
3. Assert: response `items` VẪN chứa candidate này, với `distanceKm == null`

**Expected Result (PASS):**
- Expert thiếu consent vẫn xuất hiện trong kết quả — chỉ thiếu `distanceKm` (null), không bị loại

**Expected Result (FAIL):**
- Expert bị loại khỏi kết quả chỉ vì thiếu location consent dù các tiêu chí khác đều match (vi phạm ADR-EXP-202 nhánh A)

**Current Status:** 🔴 Not written
**Implementation Note:** `IMapProviderService.calculateHaversineDistance()` CHỈ được gọi cho candidate có `locationConsentValid=true` — candidate không có consent giữ nguyên `distanceKm=null`, không gọi calculation.

---

### EXP165-TC-006 — Hard filter: maxDistanceKm set, expert thiếu consent hợp lệ bị loại

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-202 nhánh B — hard filter (Proposed)` / PDPA minimum-necessary

**Preconditions:**
- 2 candidates: `VERIFIED_EXPERT_ID` (consent hợp lệ, `FX-165-005`) và `NO_CONSENT_EXPERT_ID` (thiếu consent)

**Test Steps:**
1. Arrange: mock repository trả cả 2 candidates; mock `calculateHaversineDistance()` trả `8.5` (trong bán kính) cho candidate có consent
2. Act: gọi `filterExperts(makeRequestWithDistance(10.0), MOTHER_USER_ID)` — `maxDistanceKm=10.0` ĐƯỢC set
3. Assert: response `items` CHỈ chứa `VERIFIED_EXPERT_ID` — `NO_CONSENT_EXPERT_ID` bị loại hoàn toàn

**Expected Result (PASS):**
- `items.size() == 1`, chỉ candidate có consent hợp lệ xuất hiện

**Expected Result (FAIL):**
- Candidate thiếu consent vẫn xuất hiện (dù không xác minh được nằm trong bán kính) — vi phạm PDPA minimum-necessary/ADR-EXP-202 nhánh B

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test PRIVACY-CRITICAL — phải PASS trước khi Approve, vì lỗi ở đây có thể dẫn tới hiển thị sai thông tin location cho Mother.

---

### EXP165-TC-007 — IMapProviderService.calculateRoute() KHÔNG BAO GIỜ được gọi trong UC165

**Severity:** `HIGH`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-203 (Proposed)`

**Preconditions:**
- Fixture `FX-165-001` + `FX-165-005`, request có `maxDistanceKm` set

**Test Steps:**
1. Arrange: mock `IMapProviderService` đầy đủ (cả `calculateHaversineDistance()` và `calculateRoute()`)
2. Act: gọi `filterExperts(makeRequestWithDistance(10.0), MOTHER_USER_ID)`
3. Assert: `verify(mapProviderService, never()).calculateRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble())`

**Expected Result (PASS):**
- `calculateRoute()` không bao giờ được gọi — chỉ `calculateHaversineDistance()`

**Expected Result (FAIL):**
- Service gọi `calculateRoute()` (TrackAsia network call) — vi phạm ADR-EXP-203, thêm external dependency không cần thiết

**Current Status:** 🔴 Not written
**Implementation Note:** Đây là test ARCHITECTURE COMPLIANCE — đảm bảo UC165 không vô tình copy pattern ETA của UC63/UC149.

---

### EXP165-TC-008 — userId lấy từ JWT SecurityContext, không từ query param

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExpertFilterController`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertFilterControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-204 (Proposed)`

**Preconditions:**
- JWT hợp lệ `FX-165-008` với `sub=mother-001`

**Test Steps:**
1. Arrange: `@WebMvcTest` với mock `IExpertFilterService`; JWT có `sub=mother-001`
2. Act: `GET /api/v1/experts/filter?specialty=Pediatrics&userId=00000000-0000-0000-0000-000000009999` (userId giả mạo trong query param)
3. Assert: `verify(expertFilterService).filterExperts(any(), eq(mother001UUID))` — controller PHẢI dùng userId từ JWT, bỏ qua query param

**Expected Result (PASS):**
- Service được gọi với userId từ SecurityContext, KHÔNG phải giá trị query param

**Expected Result (FAIL):**
- Service nhận userId từ query param (lỗ hổng cho phép giả mạo identity)

**Current Status:** 🔴 Not written
**Implementation Note:** DTO `ExpertFilterRequest` (§8.1 TDS) KHÔNG có field `userId` — nếu Controller code bind userId từ request param, đó là bug nghiêm trọng.

---

### EXP165-TC-009 — Boundary: feeMin == feeMax (khoảng giá đơn điểm)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-EXP-IMP-002 §8.1 ExpertFilterRequest validation (Proposed)`

**Preconditions:**
- Candidate với `minFee = 150000` chính xác

**Test Steps:**
1. Arrange: mock repository trả candidate `minFee=150000`
2. Act: gọi `filterExperts(makeRequest(r -> {r.setFeeMin(BigDecimal.valueOf(150000)); r.setFeeMax(BigDecimal.valueOf(150000));}), MOTHER_USER_ID)`
3. Assert: candidate vẫn match (boundary `>=` và `<=`, không phải `>`/`<`)

**Expected Result (PASS):**
- `items.size() == 1` — candidate với giá đúng bằng feeMin=feeMax vẫn match

**Expected Result (FAIL):**
- Candidate bị loại (off-by-one boundary bug, dùng `>`/`<` thay vì `>=`/`<=`)

**Current Status:** 🔴 Not written
**Implementation Note:** Repository WHERE clause PHẢI dùng `BETWEEN` hoặc `>= AND <=`, không dùng strict `>`/`<`.

---

### EXP165-TC-010 — Boundary: ratingMin = 5.0 (giá trị max hợp lệ)

**Severity:** `LOW`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-EXP-IMP-002 §8.1 @DecimalMax("5") (Proposed)`

**Preconditions:**
- Candidate với `ratingAvg = 5.0` chính xác

**Test Steps:**
1. Arrange: mock repository trả candidate `ratingAvg=5.0`
2. Act: gọi `filterExperts(makeRequest(r -> r.setRatingMin(BigDecimal.valueOf(5.0))), MOTHER_USER_ID)`
3. Assert: candidate vẫn match (`ratingAvg >= ratingMin`, cả 2 đều = 5.0)

**Expected Result (PASS):**
- `items.size() == 1`

**Expected Result (FAIL):**
- Candidate bị loại dù rating tối đa đúng bằng threshold

**Current Status:** 🔴 Not written
**Implementation Note:** Boundary Value Analysis chuẩn ISO 29119-4 — test giá trị biên trên của domain hợp lệ [0,5].

---

### EXP165-TC-011 — Boundary: maxDistanceKm đúng bằng distanceKm tính được

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertFilterService.filterExperts()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertFilterServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-202 nhánh B (Proposed)`

**Preconditions:**
- Candidate có consent hợp lệ; mock `calculateHaversineDistance()` trả đúng `10.0`

**Test Steps:**
1. Arrange: mock `calculateHaversineDistance()` trả `10.0`
2. Act: gọi `filterExperts(makeRequestWithDistance(10.0), MOTHER_USER_ID)` — `maxDistanceKm=10.0`, `distanceKm` tính được cũng `=10.0`
3. Assert: candidate vẫn xuất hiện trong kết quả (boundary `<=`, không phải `<`)

**Expected Result (PASS):**
- `items.size() == 1` — candidate đúng ở biên bán kính vẫn match

**Expected Result (FAIL):**
- Candidate bị loại (dùng `<` strict thay vì `<=`)

**Current Status:** 🔴 Not written
**Implementation Note:** Post-filter distance PHẢI dùng `distanceKm <= maxDistanceKm`, mirror UC149 MAP149-TC-002 boundary convention.

---

### EXP165-TC-012 — Empty state trả HTTP 200 với items:[], không 404

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertFilterController`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertFilterControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `SRS §3.3.9.2 AF2`

**Preconditions:**
- Mock Service trả `ExpertFilterResponse(items=[], totalCount=0)`

**Test Steps:**
1. Arrange: mock `IExpertFilterService.filterExperts()` trả response rỗng
2. Act: `GET /api/v1/experts/filter?specialty=NonExistentSpecialty`
3. Assert: HTTP status `200`; response body `{"items":[],"totalCount":0,...}`

**Expected Result (PASS):**
- Status 200, body `items` là mảng rỗng

**Expected Result (FAIL):**
- Status 404 (vi phạm SRS AF2)

**Current Status:** 🔴 Not written
**Implementation Note:** Mirror UC63/UC149 convention — không bao giờ trả 404 cho kết quả rỗng của 1 search/filter hợp lệ.

---

### EXP165-TC-013 — feeMin > feeMax → HTTP 400 EXP-201

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertFilterController`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertFilterControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-EXP-IMP-002 §8.1 @AssertTrue (Proposed)` / `SRS E2`

**Test Steps:**
1. Act: `GET /api/v1/experts/filter?feeMin=500000&feeMax=100000`
2. Assert: HTTP `400`, body `error.code == "EXP-201"`, `details` chứa field `feeMin`

**Expected Result (PASS):**
- 400 với error code đúng, service KHÔNG được gọi (`verify(expertFilterService, never()).filterExperts(any(), any())`)

**Expected Result (FAIL):**
- Request pass validation, service được gọi với params sai logic

**Current Status:** 🔴 Not written
**Implementation Note:** `@AssertTrue` custom validator trên `ExpertFilterRequest` — validation phải chạy TRƯỚC khi vào Controller method body.

---

### EXP165-TC-014 — maxDistanceKm set nhưng thiếu latitude/longitude → HTTP 400 EXP-201

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertFilterController`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertFilterControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `CB-EXP-IMP-002 §8.1 @AssertTrue (Proposed)` / `ADR-EXP-202`

**Test Steps:**
1. Act: `GET /api/v1/experts/filter?maxDistanceKm=10` (thiếu `latitude`/`longitude`)
2. Assert: HTTP `400`, body `error.code == "EXP-201"`

**Expected Result (PASS):**
- 400 — request không hợp lệ vì thiếu toạ độ cần thiết để tính distance hard filter

**Expected Result (FAIL):**
- Request pass, service nhận `maxDistanceKm` mà không có toạ độ (NullPointerException tiềm ẩn ở tầng dưới)

**Current Status:** 🔴 Not written
**Implementation Note:** Validation phải chặn sớm ở DTO layer, không để lỗi rơi xuống Service/Repository.

---

### EXP165-TC-015 — Không có JWT → 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `ExpertFilterController` (Spring Security filter chain)
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertFilterControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-EXP-IMP-002 ADR-EXP-204 (Proposed)` / `SRS E1`

**Test Steps (Attack Simulation):**
1. Act: `GET /api/v1/experts/filter?specialty=Pediatrics` (không có `Authorization` header)
2. Assert: HTTP `401`, body `error.code == "IAM-001"`

**Expected Result (PASS = hệ thống an toàn):**
- `401 Unauthorized`, Controller method KHÔNG được thực thi (filter chain reject trước)

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Request pass, trả về data mà không cần xác thực

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### EXP165-TC-INT-001 — Full HTTP flow: kết hợp cả 7 tiêu chí cùng lúc với DB thực

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: ExpertFilterController → ExpertFilterService → IExpertFilterRepository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/expert/ExpertFilterIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tự động khi Spring context start
- Seed: 3 experts — (1) match toàn bộ 7 tiêu chí, (2) match 6/7 (thiếu consent, dùng cho hard-filter exclusion), (3) không match specialty

**Test Steps:**
1. Seed `expert_profiles`/`expert_availability`/`expert_consultation_prices`/`expert_location_shares` cho 3 expert theo mô tả trên
2. Call `GET /api/v1/experts/filter` với `specialty`, `modality`, `feeMin/feeMax`, `ratingMin`, `onlineStatus`, `latitude/longitude`, `maxDistanceKm` — TẤT CẢ 7 tiêu chí cùng lúc
3. Assert response chỉ chứa expert (1) — 2 expert còn lại bị loại đúng lý do

**Expected Result (PASS):**
- `items.size() == 1`, expert đúng match tất cả 7 tiêu chí
- Expert (2) bị loại vì thiếu consent (hard filter, `maxDistanceKm` được set trong test)
- Expert (3) bị loại vì sai specialty

**Expected Result (FAIL):**
- Expert (2) hoặc (3) xuất hiện sai, hoặc expert (1) bị loại nhầm

**DB Assertion:**
```java
List<ExpertFilterCandidate> raw = expertFilterRepository.findByCriteria(criteria);
assertThat(raw).extracting(ExpertFilterCandidate::getExpertProfileId)
    .containsExactly(expert1Id);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EXP165-TC-001` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-002` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-003` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-004` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-005` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-006` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-007` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-008` | `ExpertFilterControllerTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-009` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-010` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-011` | `ExpertFilterServiceTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-012` | `ExpertFilterControllerTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-013` | `ExpertFilterControllerTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-014` | `ExpertFilterControllerTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-015` | `ExpertFilterControllerTest.java` | `[ ]` | `[ ]` | |
| `EXP165-TC-INT-001` | `ExpertFilterIntegrationTest.java` | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertFilterService implements IExpertFilterService {

    @Override
    public ExpertFilterResponse filterExperts(ExpertFilterRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EXP165-TC-001` → `EXP165-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `EXP165-TC-012` → `EXP165-TC-015` | Controller mock trả stub | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXP165-TC-INT-001` | Full stack throws | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-EXP-IMP-002` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] UC129 (`IMapProviderService`) đã implement và deploy
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ExpertFilterService`
- [ ] Không có business logic trong `ExpertFilterController` (chỉ có validation + mapping)
- [ ] Không có PII (toạ độ chính xác) xuất hiện plaintext trong logs mức INFO
- [ ] Verify Haversine calculation delegate 100% sang `IMapProviderService`, không có implementation riêng trong `expert` package
- [ ] Verify `calculateRoute()`/TrackAsia KHÔNG bao giờ được gọi (EXP165-TC-007 pass)

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
- Product Owner chưa xác nhận enum values cho `modality`/`onlineStatus` (§18 RG-2 của TDS) — có thể tiếp tục implement với giá trị literal test data nhưng PHẢI ghi rõ "pending enum confirmation"
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Không có migration mới cho Draft này — không cần revert schema

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/expert/controller/ExpertFilterController.java
git checkout -- src/main/java/com/carebridge/backend/expert/service/
git checkout -- src/main/java/com/carebridge/backend/expert/repository/IExpertFilterRepository.java
git checkout -- src/main/java/com/carebridge/backend/expert/dto/
git checkout -- src/test/java/com/carebridge/backend/expert/

# Nếu migration V20260706101000 (composite index) đã được tạo và chạy:
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP INDEX IF EXISTS idx_expert_profiles_specialty_verification;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260706101000';"
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ verify khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — EXP165-TC-005/006 tách rõ 2 nhánh ADR-EXP-202 | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — Controller tests chỉ verify HTTP mapping/security | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — mọi type tham chiếu §8 TDS `CB-EXP-IMP-002` hoặc UC129 `CB-MAP-IMP-000` | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở giai đoạn spec (chờ Red Gate verify khi implement) → TDD spec approved cho giai đoạn Draft

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| _(none tại thời điểm viết Test-Spec)_ | — | — | — | — |

---

*TDD Spec v1.0 — Draft. Chưa Approved. Chờ TDS `CB-EXP-IMP-002` Approved trước khi bắt đầu Red Phase.*
