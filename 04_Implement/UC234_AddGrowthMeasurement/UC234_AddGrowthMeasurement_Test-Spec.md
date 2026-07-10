# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-234 Add Growth Measurement

**Document ID:** `CB-BABY-IMP-009-TEST`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented - 2026-07-10 (service coverage PASS; full TC matrix pending)`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC234_AddGrowthMeasurement/UC234_AddGrowthMeasurement_TDS.md` (`CB-BABY-IMP-009`)
- SRS: `02_Requirements/SRS/3_Functional_Specification.md` §3.3.19.7 (Table 256)
- Exemplar TDD: `04_Implement/UC38_ViewGrowthChart/UC38_ViewGrowthChart_Test-Spec.md` (`CB-BABY-IMP-008-TEST`)
- Precedent TDD: `04_Implement/UC32_UpdateBabyProfile/UC32_UpdateBabyProfile_Test-Spec.md` (`CB-BABY-IMP-002-TEST`)

---

## CHANGELOG

| Ngay | Nguoi thuc hien | Noi dung thay doi |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Truthful sync after backend test pass: service-level UC234 cases added to `GrowthServiceTest`; `GrowthServiceTest` 19/19 PASS and `com.carebridge.backend.carejourney.**.*Test` 65/65 PASS. Red Gate/controller/integration/full TC matrix not reconstructed yet. |
| 2026-07-03 | AI Agent | Khoi tao TDD spec cho UC-234 Add Growth Measurement |

---

## MUC LUC

1. [Thong tin Module](#1-thong-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thong tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-234` |
| **Module** | `AddGrowthMeasurement -- carejourney` |
| **Spec goc** | `CB-BABY-IMP-009` |
| **Priority** | P1 |
| **Data Classification** | `PII` (health data) |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY / PDPA — health data of a minor (baby), access must remain owner-scoped (Mother only), no third-party read/write without consent (out of scope for this write-only UC)` |
| **Upstream Dependencies** | `auth (JWT), baby (baby_profiles -- cross-context repo, see L5), growth_measurements` |
| **Downstream Consumers** | `UC-38 ViewGrowthChart (renders new point), UC-237 ViewGrowthMeasurementHistory` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-BABY-IMP-009 §17` |
| **Constraints Injected** | C1 (ownership), C2 (ACTIVE required), C3 (at-least-one value), C4 (non-negative), C5 (measuredDate required), C6 (new GrowthMeasurementController) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 -> T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec goc (sai / thieu) | Thuc te (schema / policy) | Fix ap dung trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UI mockup CB-176 hien thi truong "Thoi gian" (gio ghi nhan, vd "08:30 Sang") | Schema `growth_measurements.measured_date` la kieu `date`, khong co cot luu gio/phut rieng | Khong co test nao assert mot truong "time" trong request/response DTO. `measuredDate` chi la `LocalDate`. Danh dau **Open** — "Thoi gian" tren UI la hien thi tu `created_at` (audit timestamp) hoac client-side only, khong phai input field cua backend |
| L2 | UI mockup CB-176 hien thi "Nguoi ghi nhan" (vd: "Me be") nhu mot truong du lieu | Schema `growth_measurements` KHONG co cot `recorded_by` (khac voi `development_milestones` co cot `recorded_by uuid`) | Khong co test assert `recordedBy` trong `GrowthMeasurementResponse`. Nguoi ghi duoc suy ra tu ownership (`baby.ownerUserId == JWT userId`) — vi UC-234 chi cho phep Mother ghi cho baby cua chinh minh, khong can luu rieng |
| L3 | SRS Table 256: "Records baby weight, height, or head circumference" — tu "or" co the doc la exclusive (chinh xac 1) | Schema co 3 cot doc lap nullable — ho tro ghi dong thoi ca 3 (vd kham dinh ky) | `ADR-BABY-009-001` dien giai la inclusive-OR (it nhat 1). Test BABY234-TC-001..004 cover ca 4 to hop: weight-only, height-only, head-circumference-only, all-three |
| L4 | `CB-BABY-IMP-008` (UC-38, exemplar) tham chieu class `ForbiddenException` cho loi BABY-071 | **RESOLVED 2026-07-04** — xac nhan truc tiep tren dia: `com.carebridge.backend.common.exception` KHONG co class `ForbiddenException`. Class thuc te la `AccessDeniedBusinessException` (file: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/AccessDeniedBusinessException.java`) | Ke thua nguyen trang tu UC-38 nhu mot ten conceptual (khong phai TDS nay dinh nghia lai). Khi implement, map `ForbiddenException` (ten dung trong TDS S8) -> `AccessDeniedBusinessException` (ten class that trong code). Test case chi assert HTTP status + error code (`403`, `BABY-071`), khong assert ten class exception cu the — dung quyet dinh nay, khong con phu thuoc vao ten chua xac nhan |
| L5 | `CB-BABY-IMP-009` dat toan bo class (bao gom `BabyProfileRepository` usage) trong package `carejourney` | **RESOLVED 2026-07-04** — xac nhan truc tiep tren dia: `BabyProfileRepository`/`BabyProfile`/`BabyProfileStatus` nam trong package `com.carebridge.backend.baby.{repository,entity}` (file: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/entity/BabyProfile.java`), khong phai `carejourney`. Entity field ten that la `id` (`@Column(name="baby_id") private UUID id`, KHONG phai `babyId`) va `ownerUserId`; `status` la enum `BabyProfileStatus` (ACTIVE/ARCHIVED), khong phai `String` | Test import path ghi ro `com.carebridge.backend.baby.repository.BabyProfileRepository` (cross-context dependency). Factory methods duoi day (§4) da dung dung `.id(BABY_ID)` va `BabyProfileStatus.ACTIVE` — day la mau chuan de UC-235/236/237 doi chieu theo (xem ghi chu cross-cutting trong cac Test-Spec do) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
AddGrowthMeasurement bao gom cac layer:
├── Policy (pure unit -- GrowthMeasurementPolicy, no deps)
├── Service (mock BabyProfileRepository + GrowthMeasurementRepository + GrowthMeasurementPolicy voi Mockito)
├── Controller (mock IGrowthService voi @WebMvcTest)
└── Integration (Testcontainers PostgreSQL voi @SpringBootTest)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-234 (Table 256)` | Mother ghi nhan chi so tang truong moi cho baby |
| `ADR-BABY-009-001` | It nhat 1 trong 3 gia tri (weightKg/heightCm/headCircumferenceCm) phai co mat |
| `ADR-BABY-009-002` (Accepted 2026-07-04) | Baby phai ACTIVE truoc khi ghi -- tu choi neu ARCHIVED |
| `ADR-BABY-009-003` | Tai su dung ownership check pattern cua UC-38 |
| `ADR-BABY-009-004` (Proposed) | Endpoint nam trong `GrowthMeasurementController` moi |
| `BR-RBAC` | Chi Mother so huu baby moi duoc ghi |
| `CB-BABY-IMP-009 §8` | Interface contract: `addGrowthMeasurement(userId, babyId, request)` |
| `V1__init_schema.sql` ~L647 | `growth_measurements` schema — `measured_date NOT NULL`, 3 cot do luong nullable |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Add weight-only measurement (valid) | `GrowthService.addGrowthMeasurement()` | `BABY234-TC-001` |
| TC-COND-002 | Add height-only measurement (valid) | `GrowthService.addGrowthMeasurement()` | `BABY234-TC-002` |
| TC-COND-003 | Add head-circumference-only measurement (valid) | `GrowthService.addGrowthMeasurement()` | `BABY234-TC-003` |
| TC-COND-004 | Add all-three measurement (valid) | `GrowthService.addGrowthMeasurement()` | `BABY234-TC-004` |
| TC-COND-005 | All 3 values missing -> rejected | `GrowthMeasurementPolicy.validateAtLeastOneValuePresent()` | `BABY234-TC-005` |
| TC-COND-006 | `measuredDate` missing -> rejected | Bean Validation on `AddGrowthMeasurementRequest` | `BABY234-TC-006` |
| TC-COND-007 | Negative measurement value -> rejected | `GrowthMeasurementPolicy.validateNonNegative()` | `BABY234-TC-007` |
| TC-COND-008 | Baby not found | `GrowthService.addGrowthMeasurement()` | `BABY234-TC-008` |
| TC-COND-009 | Baby not owned by caller | `GrowthService.addGrowthMeasurement()` | `BABY234-TC-009` |
| TC-COND-010 | Baby ARCHIVED -> rejected (write gate) | `GrowthService.addGrowthMeasurement()` | `BABY234-TC-010` |
| TC-COND-011 | Unauthenticated access blocked | Spring Security filter | `BABY234-TC-011` |
| TC-COND-012 | Full flow persists correct DB row | Controller -> Service -> Repository -> PostgreSQL | `BABY234-TC-INT-001` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Presence combinations of weightKg/heightCm/headCircumferenceCm | Valid partitions: 1, 2, or 3 values present. Invalid partition: 0 values present (BABY-072) |
| Boundary Value Analysis | Measurement value at 0 vs negative | `0` is a valid boundary (non-negative), `-0.01` triggers BABY-074 |
| State Transition Testing | `baby.status` ACTIVE vs ARCHIVED | Verifies write-gate enforced only for write (contrast with UC-38 read, which has no gate) |
| Error Guessing | Missing JWT, missing `measuredDate`, malformed date string | Security (CWE-306) and robustness edge cases |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Muc dich |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: 'MOTHER_ID', role: 'MOTHER'}` | Happy path -- Mother authenticated |
| `FX-002` | DB seed | `BabyProfile(babyId=BABY_ID, ownerUserId=MOTHER_ID, status=ACTIVE)` | Target baby, write-eligible |
| `FX-003` | DB seed | `BabyProfile(status=ARCHIVED)` | Archived-baby-rejected test |
| `FX-004` | DB seed | `BabyProfile(ownerUserId=OTHER_USER_ID)` | Ownership-denied test |
| `FX-005` | Request payload | `{measuredDate: 2026-02-15, weightKg: 4.2}` | Weight-only happy path |
| `FX-006` | Request payload | `{measuredDate: 2026-02-15, heightCm: 52}` | Height-only happy path |
| `FX-007` | Request payload | `{measuredDate: 2026-02-15, headCircumferenceCm: 35}` | Head-circumference-only happy path |
| `FX-008` | Request payload | `{measuredDate: 2026-02-15, weightKg: 4.2, heightCm: 52, headCircumferenceCm: 35}` | All-three happy path |
| `FX-009` | Request payload | `{measuredDate: 2026-02-15}` (all 3 measurement fields null) | All-fields-missing rejected |
| `FX-010` | Request payload | `{weightKg: 4.2}` (measuredDate null) | measuredDate-missing rejected |
| `FX-011` | Request payload | `{measuredDate: 2026-02-15, weightKg: -1.0}` | Negative-value rejected |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BAT BUOC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 -- Props Isolation Pattern
// Moi @Test dung factory methods -- khong shared mutable state
// ═══════════════════════════════════════════════════════════

class GrowthMeasurementTestFactory {
    static UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000234");
    static UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static UUID BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000234");
    static UUID NON_EXISTENT_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-999999999999");
    static UUID NEW_MEASUREMENT_ID = UUID.fromString("aaaa0003-0000-0000-0000-000000000003");

    // -- BabyProfile fixtures (cross-context: com.carebridge.backend.baby.entity.BabyProfile -- see L5) --

    static BabyProfile makeActiveBaby() {
        return BabyProfile.builder()
            .id(BABY_ID)
            .ownerUserId(MOTHER_ID)
            .nickname("Growth Baby")
            .status(BabyProfileStatus.ACTIVE)
            .build();
    }

    static BabyProfile makeArchivedBaby() {
        BabyProfile baby = makeActiveBaby();
        baby.setStatus(BabyProfileStatus.ARCHIVED);
        return baby;
    }

    static BabyProfile makeBabyOwnedByOther() {
        BabyProfile baby = makeActiveBaby();
        baby.setOwnerUserId(OTHER_USER_ID);
        return baby;
    }

    // -- Request DTO fixtures --

    static AddGrowthMeasurementRequest makeWeightOnlyRequest() {
        AddGrowthMeasurementRequest req = new AddGrowthMeasurementRequest();
        req.setMeasuredDate(LocalDate.of(2026, 2, 15));
        req.setWeightKg(new BigDecimal("4.2"));
        req.setSourceType("HOME_SCALE");
        req.setNote("1 month checkup");
        return req;
    }

    static AddGrowthMeasurementRequest makeHeightOnlyRequest() {
        AddGrowthMeasurementRequest req = new AddGrowthMeasurementRequest();
        req.setMeasuredDate(LocalDate.of(2026, 2, 15));
        req.setHeightCm(new BigDecimal("52"));
        return req;
    }

    static AddGrowthMeasurementRequest makeHeadCircumferenceOnlyRequest() {
        AddGrowthMeasurementRequest req = new AddGrowthMeasurementRequest();
        req.setMeasuredDate(LocalDate.of(2026, 2, 15));
        req.setHeadCircumferenceCm(new BigDecimal("35"));
        return req;
    }

    static AddGrowthMeasurementRequest makeAllThreeRequest() {
        AddGrowthMeasurementRequest req = new AddGrowthMeasurementRequest();
        req.setMeasuredDate(LocalDate.of(2026, 2, 15));
        req.setWeightKg(new BigDecimal("4.2"));
        req.setHeightCm(new BigDecimal("52"));
        req.setHeadCircumferenceCm(new BigDecimal("35"));
        req.setSourceType("CLINIC");
        return req;
    }

    static AddGrowthMeasurementRequest makeAllValuesMissingRequest() {
        AddGrowthMeasurementRequest req = new AddGrowthMeasurementRequest();
        req.setMeasuredDate(LocalDate.of(2026, 2, 15));
        // weightKg, heightCm, headCircumferenceCm all left null
        return req;
    }

    static AddGrowthMeasurementRequest makeMissingMeasuredDateRequest() {
        AddGrowthMeasurementRequest req = new AddGrowthMeasurementRequest();
        req.setWeightKg(new BigDecimal("4.2"));
        // measuredDate left null
        return req;
    }

    static AddGrowthMeasurementRequest makeNegativeWeightRequest() {
        AddGrowthMeasurementRequest req = new AddGrowthMeasurementRequest();
        req.setMeasuredDate(LocalDate.of(2026, 2, 15));
        req.setWeightKg(new BigDecimal("-1.0"));
        return req;
    }

    // -- Persisted entity fixture (repository.save() mock return) --

    static GrowthMeasurement makeSavedEntity(AddGrowthMeasurementRequest req) {
        return GrowthMeasurement.builder()
            .growthMeasurementId(NEW_MEASUREMENT_ID)
            .babyId(BABY_ID)
            .measuredDate(req.getMeasuredDate())
            .weightKg(req.getWeightKg())
            .heightCm(req.getHeightCm())
            .headCircumferenceCm(req.getHeadCircumferenceCm())
            .sourceType(req.getSourceType())
            .note(req.getNote())
            .createdAt(Instant.parse("2026-07-03T08:00:00Z"))
            .updatedAt(Instant.parse("2026-07-03T08:00:00Z"))
            .build();
    }
}
```

---

### BABY234-TC-001 — Happy path: add weight-only measurement

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthService.addGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-BABY-IMP-009 §8 / §9.2 (Variant A) / ADR-BABY-009-001`

**Preconditions:**
- Baby profile exists, status ACTIVE, owned by MOTHER_ID (`makeActiveBaby()`)
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeActiveBaby()`
- Mock: `growthMeasurementRepository.save(any())` returns `makeSavedEntity(request)`

**Test Steps:**
1. Arrange: `request = makeWeightOnlyRequest()` (weightKg=4.2, heightCm=null, headCircumferenceCm=null)
2. Act: Call `growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request)`
3. Assert: response is not null; `growthMeasurementId` = NEW_MEASUREMENT_ID; `weightKg` = 4.2; `heightCm` = null; `headCircumferenceCm` = null; `measuredDate` = 2026-02-15

**Expected Result (PASS):**
- `GrowthMeasurementResponse` returned with weightKg populated, heightCm/headCircumferenceCm null
- `growthMeasurementRepository.save()` invoked exactly once

**Expected Result (FAIL):**
- Exception thrown for a valid single-value request, or `save()` never invoked

**Current Status:** 🔴 Not written

---

### BABY234-TC-002 — Happy path: add height-only measurement

**Severity:** `HIGH`
**Feature Under Test:** `GrowthService.addGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-BABY-009-001 (inclusive-OR)`

**Preconditions:**
- Baby profile ACTIVE, owned by MOTHER_ID
- Mock: `growthMeasurementRepository.save(any())` returns `makeSavedEntity(request)`

**Test Steps:**
1. Arrange: `request = makeHeightOnlyRequest()` (heightCm=52, weightKg=null, headCircumferenceCm=null)
2. Act: Call `growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request)`
3. Assert: response contains heightCm=52, weightKg=null, headCircumferenceCm=null

**Expected Result (PASS):**
- 201-equivalent response returned, no `BABY-072` exception raised despite weightKg/headCircumferenceCm being absent

**Expected Result (FAIL):**
- `BusinessException(BABY-072)` incorrectly thrown for a single valid field (over-strict validation bug)

**Current Status:** 🔴 Not written

---

### BABY234-TC-003 — Happy path: add head-circumference-only measurement

**Severity:** `HIGH`
**Feature Under Test:** `GrowthService.addGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-BABY-009-001 (inclusive-OR)`

**Preconditions:**
- Baby profile ACTIVE, owned by MOTHER_ID
- Mock: `growthMeasurementRepository.save(any())` returns `makeSavedEntity(request)`

**Test Steps:**
1. Arrange: `request = makeHeadCircumferenceOnlyRequest()` (headCircumferenceCm=35, others null)
2. Act: Call `growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request)`
3. Assert: response contains headCircumferenceCm=35, weightKg=null, heightCm=null

**Expected Result (PASS):**
- 201-equivalent response returned

**Expected Result (FAIL):**
- `BusinessException(BABY-072)` incorrectly thrown

**Current Status:** 🔴 Not written

---

### BABY234-TC-004 — Happy path: add all-three measurement (weight + height + head circumference)

**Severity:** `HIGH`
**Feature Under Test:** `GrowthService.addGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-BABY-009-001 §Decision (Option B — cho phep ca 3 cung luc)`

**Preconditions:**
- Baby profile ACTIVE, owned by MOTHER_ID
- Mock: `growthMeasurementRepository.save(any())` returns `makeSavedEntity(request)`

**Test Steps:**
1. Arrange: `request = makeAllThreeRequest()` (weightKg=4.2, heightCm=52, headCircumferenceCm=35, sourceType="CLINIC")
2. Act: Call `growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request)`
3. Assert: response contains all three values populated correctly, sourceType="CLINIC"

**Expected Result (PASS):**
- All 3 fields persisted and returned unchanged

**Expected Result (FAIL):**
- Any field dropped, or exception thrown when all 3 are legitimately present

**Current Status:** 🔴 Not written

---

### BABY234-TC-005 — All 3 measurement values missing -> 400 BABY-072

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementPolicy.validateAtLeastOneValuePresent()` (invoked via `GrowthService.addGrowthMeasurement()`)
**Test File:** `src/test/java/com/carebridge/backend/carejourney/policy/GrowthMeasurementPolicyTest.java` + `GrowthServiceTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-BABY-IMP-009 §10 BABY-072 / ADR-BABY-009-001`

**Preconditions:**
- Baby profile ACTIVE, owned by MOTHER_ID
- `request = makeAllValuesMissingRequest()` (measuredDate set, weightKg/heightCm/headCircumferenceCm all null)

**Test Steps:**
1. Arrange: baby exists and is ACTIVE/owned; request has all 3 measurement values null
2. Act: Call `growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request)`
3. Assert: `BusinessException` thrown with code `BABY-072`; `growthMeasurementRepository.save()` never invoked

**Expected Result (PASS):**
- `BusinessException(BABY-072)` thrown, no DB write attempted

**Expected Result (FAIL):**
- Request succeeds and an empty measurement row is persisted (data integrity violation)

**Current Status:** 🔴 Not written
**Implementation Note:** Validation must run before `repository.save()` is called — verify via `verify(growthMeasurementRepository, never()).save(any())`.

---

### BABY234-TC-006 — `measuredDate` missing -> 400 BABY-075

**Severity:** `HIGH`
**Feature Under Test:** `AddGrowthMeasurementRequest` Bean Validation (`@NotNull` on `measuredDate`)
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementControllerTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-BABY-IMP-009 §10 BABY-075 / Schema V1__init_schema.sql L650 (measured_date NOT NULL)`

**Preconditions:**
- Valid JWT for MOTHER_ID
- Request body has `weightKg=4.2` but `measuredDate` omitted (`makeMissingMeasuredDateRequest()`)

**Test Steps:**
1. Arrange: Build POST request with JSON body missing `measuredDate`
2. Act: Call `POST /api/v1/babies/{babyId}/growth-measurements` via MockMvc
3. Assert: response status 400, error code `BABY-075`; service method never invoked

**Expected Result (PASS):**
- HTTP 400 with code `BABY-075`, Controller layer rejects before reaching Service

**Expected Result (FAIL):**
- Request reaches Service with `measuredDate = null`, or a 500 error instead of 400

**Current Status:** 🔴 Not written

---

### BABY234-TC-007 — Negative measurement value -> 400 BABY-074

**Severity:** `MEDIUM`
**Feature Under Test:** `GrowthMeasurementPolicy.validateNonNegative()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/policy/GrowthMeasurementPolicyTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-BABY-IMP-009 §10 BABY-074 / §4.2 Data Integrity (non-negative sanity check only — no clinical range, Open)`

**Preconditions:**
- `request = makeNegativeWeightRequest()` (weightKg = -1.0, measuredDate valid)

**Test Steps:**
1. Arrange: baby ACTIVE/owned; request has weightKg = -1.0
2. Act: Call `growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request)`
3. Assert: `BusinessException` thrown with code `BABY-074`; no DB write attempted

**Expected Result (PASS):**
- `BusinessException(BABY-074)` thrown for weightKg = -1.0
- Boundary check: weightKg = 0 (exact boundary) does NOT throw — separate assertion in same test class

**Expected Result (FAIL):**
- Negative value silently persisted, or 0 incorrectly rejected as negative

**Current Status:** 🔴 Not written
**Implementation Note:** No clinical range validation (e.g., "weight must be < 50kg") is in scope — only a `>= 0` sanity check, per `CB-BABY-IMP-009 §4.2` (marked Open/out-of-scope for exact clinical ranges).

---

### BABY234-TC-008 — Baby not found -> 404 BABY-070

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthService.addGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-BABY-IMP-009 §10 BABY-070 (reused from CB-BABY-IMP-008 §10)`

**Preconditions:**
- Mock: `babyProfileRepository.findById(NON_EXISTENT_BABY_ID)` returns `Optional.empty()`

**Test Steps:**
1. Arrange: non-existent babyId, valid weight-only request
2. Act: Call `growthService.addGrowthMeasurement(MOTHER_ID, NON_EXISTENT_BABY_ID, request)`
3. Assert: `ResourceNotFoundException` thrown with code `BABY-070`

**Expected Result (PASS):**
- `ResourceNotFoundException(BABY-070)` thrown, no DB write attempted

**Expected Result (FAIL):**
- `NullPointerException` or unhandled 500 error

**Current Status:** 🔴 Not written

---

### BABY234-TC-009 — Baby not owned by caller -> 403 BABY-071

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthService.addGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-RBAC / CB-BABY-IMP-009 §10 BABY-071 (reused) / ADR-BABY-009-003`

**Preconditions:**
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBabyOwnedByOther()`

**Test Steps:**
1. Arrange: baby owned by OTHER_USER_ID, MOTHER_ID attempts to write
2. Act: Call `growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request)`
3. Assert: exception thrown with code `BABY-071` (class name per L4 — Open, assert on error code not on exception class name); `growthMeasurementRepository.save()` never invoked

**Expected Result (PASS):**
- 403-equivalent exception with code `BABY-071`, no data written

**Expected Result (FAIL):**
- Measurement persisted for a baby not owned by caller — RBAC violation

**Current Status:** 🔴 Not written

---

### BABY234-TC-010 — Baby ARCHIVED -> 400 BABY-073 (write gate rejected)

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthService.addGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-BABY-009-002 (Accepted 2026-07-04) — precedent: CB-BABY-IMP-002 §3 ADR-BABY-004`

**Preconditions:**
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeArchivedBaby()` (status=ARCHIVED, owned by MOTHER_ID)

**Test Steps:**
1. Arrange: baby ARCHIVED, owned by caller, valid weight-only request
2. Act: Call `growthService.addGrowthMeasurement(MOTHER_ID, BABY_ID, request)`
3. Assert: `BusinessException` thrown with code `BABY-073`; no DB write attempted

**Expected Result (PASS):**
- `BusinessException(BABY-073)` thrown — differs from UC-38 (read), which allows ARCHIVED

**Expected Result (FAIL):**
- Measurement persisted for an ARCHIVED baby profile — violates ADR-BABY-009-002 write-gate

**Current Status:** 🔴 Not written
**Implementation Note:** This ADR is now `Accepted` (2026-07-04, by user/product owner) — no longer conditional/pending. This test case is fully confirmed/required for Red Gate + Green implementation, not an optional/speculative scenario.

---

### BABY234-TC-011 — No JWT -> 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementControllerTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- No JWT token in request headers

**Test Steps:**
1. Arrange: Build POST request without `Authorization` header
2. Act: Call `POST /api/v1/babies/{babyId}/growth-measurements` via MockMvc
3. Assert: response status 401; service method never invoked

**Expected Result (PASS):**
- HTTP 401 Unauthorized, request never reaches Controller logic

**Expected Result (FAIL):**
- Request reaches Controller/Service without authentication

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### BABY234-TC-INT-001 — Integration: POST persists correct DB row (weight + height, head circumference null)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller -> Service -> Policy -> Repository -> PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/GrowthMeasurementIntegrationTest.java`
**TDD Phase:** 🔴 RED — chua implement
**Condition Ref:** `TC-COND-012`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically at Spring context start
- Seed: `baby_profiles` row with BABY_ID, MOTHER_ID, status=ACTIVE

**Test Steps:**
1. Seed `baby_profiles` via JPA repository
2. Call `POST /api/v1/babies/{BABY_ID}/growth-measurements` with valid JWT (MOTHER_ID), body `{measuredDate: 2026-02-15, weightKg: 4.2, heightCm: 52}`
3. Assert response and DB state

**Expected Result (PASS):**
- Response status 201
- Response body contains `growthMeasurementId` (server-generated UUID), `weightKg=4.2`, `heightCm=52`, `headCircumferenceCm=null`
- `growth_measurements` table contains exactly 1 new row for `baby_id = BABY_ID` with matching values
- `created_at`/`updated_at` populated (non-null)

**Expected Result (FAIL):**
- Row not persisted, or persisted with incorrect/missing values

**DB Assertion:**
```java
GrowthMeasurementResponse response = objectMapper.readValue(
    result.getResponse().getContentAsString(),
    new TypeReference<ApiResponse<GrowthMeasurementResponse>>() {}
).getData();

assertThat(response.getGrowthMeasurementId()).isNotNull();
assertThat(response.getWeightKg()).isEqualByComparingTo(new BigDecimal("4.2"));
assertThat(response.getHeightCm()).isEqualByComparingTo(new BigDecimal("52"));
assertThat(response.getHeadCircumferenceCm()).isNull();

List<GrowthMeasurement> rows = growthMeasurementRepository.findByBabyIdOrderByMeasuredDateAsc(BABY_ID);
assertThat(rows).hasSize(1);
assertThat(rows.get(0).getCreatedAt()).isNotNull();
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `BABY234-TC-001` | `GrowthServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-002` | `GrowthServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-003` | `GrowthServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-004` | `GrowthServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-005` | `GrowthMeasurementPolicyTest.java` / `GrowthServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-006` | `GrowthMeasurementControllerTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-007` | `GrowthMeasurementPolicyTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-008` | `GrowthServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-009` | `GrowthServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-010` | `GrowthServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-011` | `GrowthMeasurementControllerTest.java` | `[ ]` | `[hash]` | |
| `BABY234-TC-INT-001` | `GrowthMeasurementIntegrationTest.java` | `[ ]` | `[hash]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase -- implementation stub (PHAI throw)
@Service
public class GrowthService implements IGrowthService {

    @Override
    public GrowthChartResponse getGrowthChart(UUID userId, UUID babyId) {
        throw new UnsupportedOperationException("Not implemented -- Red Phase stub");
    }

    @Override
    public GrowthMeasurementResponse addGrowthMeasurement(UUID userId, UUID babyId, AddGrowthMeasurementRequest request) {
        throw new UnsupportedOperationException("Not implemented -- Red Phase stub");
    }
}

@Component
public class GrowthMeasurementPolicy {

    public void validateAtLeastOneValuePresent(AddGrowthMeasurementRequest request) {
        throw new UnsupportedOperationException("Not implemented -- Red Phase stub");
    }

    public void validateNonNegative(AddGrowthMeasurementRequest request) {
        throw new UnsupportedOperationException("Not implemented -- Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (neu PASS bat thuong) |
|-------|-------------|----------|--------|----------------------------------|
| `BABY234-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `BABY234-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-007` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-008` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-011` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `BABY234-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tat ca FAIL? ☐ Yes -> **GATE-2 PASS** (T2->T3) -> tiep tuc implement
- Log file: `[path to red-gate-evidence.log]`

> **Neu bat ky test PASS:** Dung lai. Xac dinh root cause tu bang tren. Rewrite test tu TC-ID spec voi Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-BABY-IMP-009` da duoc review va approve
- [ ] Logic Issues (§2) da duoc confirm voi Principal Architect — dac biet L4 (exception class ten) va L5 (package boundary)
- [x] `ADR-BABY-009-002` da `Accepted` (2026-07-04) — khong con blocker; `ADR-BABY-009-004` van `Proposed`, can Accepted hoac Superseded truoc khi merge
- [ ] Table `growth_measurements` da ton tai trong DB (xac nhan, khong can migration moi)
- [ ] Test fixtures (§3 TDS-05) da duoc chuan bi

### Exit Criteria

- [ ] `./mvnw test` — tat ca unit tests xanh (khong co skip)
- [ ] `./mvnw verify` — tat ca integration tests xanh (Testcontainers)
- [ ] Test coverage >= 80% lines cho `GrowthService.addGrowthMeasurement()` va `GrowthMeasurementPolicy`
- [ ] Khong co business logic trong `GrowthMeasurementController` (chi validation + mapping)
- [ ] Khong co PII/secret xuat hien plaintext trong logs
- [ ] Tat ca 4 to hop happy-path (weight-only/height-only/head-circumference-only/all-three) pass

**Exit Criteria bo sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tat ca tests FAIL voi empty/throw stub truoc khi implement
- [ ] **Contract Existence** — moi class duoc inject deu ton tai trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — khong co shared mutable state giua tests
- [ ] **Oracle Source** — moi expected value trong assert co ghi ro nguon (ADR/BR/Schema)

### Suspension Criteria

- `ADR-BABY-009-004` bi Principal Architect bac bo — can redesign truoc khi tiep tuc (`ADR-BABY-009-002` da Accepted, khong con la dieu kien suspension)
- Blocker dependency chua san sang (vd: `IGrowthService`/`GrowthService` cua UC-38 chua ton tai de mo rong)
- CI pipeline bi broken boi thay doi khac

---

## 7. Rollback Plan

```bash
# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementController.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/GrowthService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/IGrowthService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/policy/GrowthMeasurementPolicy.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/mapper/GrowthMeasurementMapper.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/request/AddGrowthMeasurementRequest.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/response/GrowthMeasurementResponse.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/

# No migration rollback needed -- growth_measurements table already existed before this UC
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dau hieu trong TDD spec | Check | Gate chan |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC khong reference ADR/TDS constraint nao | [x] OK — moi TC co Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS voi empty/throw stub (§5.1) | [ ] Pending — can chay Red Gate that su | G-2 |
| AP-AI-003 | Implicit Decision | Test assume architecture decision khong co ADR | [x] OK — ADR-BABY-009-002 (Accepted 2026-07-04) va ADR-BABY-009-004 (con Proposed) deu duoc trich dan ro | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller co business logic | [x] OK — BABY234-TC-006/011 chi test validation/auth tai Controller layer, khong test business rule | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type khong ton tai trong codebase | [ ] Pending — `ForbiddenException` (L4) chua xac nhan ton tai, `BabyProfileRepository` o package khac (L5) | G-3 |

**Ket qua review:**

- [ ] Khong phat hien anti-pattern nao -> TDD spec approved
- [x] Phat hien AP -> ghi vao bang duoi -> fix truoc khi implement

| AP detected | TC ID | Mo ta | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-005` | `BABY234-TC-009` | Exception class ten `ForbiddenException` (ke thua tu UC-38) — **RESOLVED**: class that la `AccessDeniedBusinessException` (`com.carebridge.backend.common.exception`) | Assert tren HTTP status + error code (`403`/`BABY-071`) thay vi ten class cu the; khi implement, throw `AccessDeniedBusinessException` | ☑ |
| `AP-AI-005` | `BABY234-TC-008/009/010` | `BabyProfileRepository` duoc gia dinh trong package `carejourney` — **RESOLVED**: thuc te nam trong `com.carebridge.backend.baby.repository` (L5) | Import path xac nhan: `com.carebridge.backend.baby.repository.BabyProfileRepository`, `com.carebridge.backend.baby.entity.{BabyProfile,BabyProfileStatus}` | ☑ |

---

*TDD Template v2.0 -- Tich hop CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
