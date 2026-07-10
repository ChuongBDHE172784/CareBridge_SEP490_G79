# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-235 Update Growth Measurement

**Document ID:** `CB-BABY-IMP-010-TEST`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented - 2026-07-10 (service coverage PASS; full TC matrix pending)`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC235_UpdateGrowthMeasurement/UC235_UpdateGrowthMeasurement_TDS.md` (`CB-BABY-IMP-010`)
- SRS: Table 257, `02_Requirements/SRS/3_Functional_Specification.md` line ~5050-5069 (UC-235)
- Sibling pattern reused: `04_Implement/UC38_ViewGrowthChart/UC38_ViewGrowthChart_TDS.md` (`CB-BABY-IMP-008`)

---

## CHANGELOG

| Ngay | Nguoi thuc hien | Noi dung thay doi |
|------|-----------------|-------------------|
| 2026-07-10 | AI Agent | Truthful sync after backend test pass: service-level UC235 cases added to `GrowthServiceTest`; `GrowthServiceTest` 19/19 PASS and `com.carebridge.backend.carejourney.**.*Test` 65/65 PASS. Red Gate/controller/integration/full TC matrix not reconstructed yet. |
| 2026-07-03 | AI Agent | Khoi tao TDD spec cho UC-235 Update Growth Measurement |

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
| **Feature / Gap ID** | `UC-235` |
| **Module** | `UpdateGrowthMeasurement -- carejourney` |
| **Spec goc** | `CB-BABY-IMP-010` |
| **Priority** | P1 |
| **Data Classification** | `PII` |
| **Upstream Dependencies** | `auth, baby (baby_profiles), growth_measurements` |
| **Downstream Consumers** | `Mobile app growth chart (UC-38), growth history list (UC-237)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-BABY-IMP-010 S17` |
| **Constraints Injected** | C1 (ownership reuse), C2 (at-least-one-value reuse), C3 (babyId/measurement.babyId cross-check), C4 (no new audit table, event only), C5 (no dual-path) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 -> T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec goc (sai / thieu) | Thuc te (schema / policy) | Fix ap dung trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Table 257 khong noi ro co cho phep update mot growth measurement khi baby o status ARCHIVED hay khong | **RESOLVED 2026-07-04 (reconciliation + acceptance):** UC-234 (Add) da chinh thuc de xuat ADR-BABY-009-002 yeu cau baby ACTIVE cho moi write. TDS UC-235 truoc day (S16) tu y gia dinh NGUOC LAI (khong co status gate) -- day la mot divergence khong duoc phep giua 2 UC ghi cung 1 bang trong cung batch. Da sua TDS de UC-235 THEO CUNG mot rule (ADR-BABY-010-006, reuse ma loi `BABY-073`), dam bao UC-234/235/236 khong con silent-diverge. Quyet dinh nghiep vu goc da duoc user/product owner Accept ngay 2026-07-04 — ca ADR-BABY-009-002 va ADR-BABY-010-006 deu la `Accepted` | Them `BABY235-TC-010` (duoi day) test ARCHIVED -> `BABY-073`, mirror `BABY234-TC-010` — test case nay nay la fully confirmed/required, khong con conditional/pending |
| L2 | UC-234 (Add) TDS chua ton tai tai thoi diem viet UC-235 TDS -- khong the doi chieu ten field truc tiep | **RESOLVED 2026-07-04:** UC-234 (`CB-BABY-IMP-009`) nay da hoan thanh; field naming (`weightKg`, `heightCm`, `headCircumferenceCm`, `measuredDate`, `sourceType`, `note`) tren `AddGrowthMeasurementRequest`/`GrowthMeasurementResponse` khop 100% voi gia dinh cua UC-235 -- khong con Open | Test factory (§ Props Isolation Boilerplate) da dung dung cac ten field nay, xac nhan khop voi UC-234 |
| L3 | SRS khong de cap ro rang co API PATCH partial hay full-replace | ADR-BABY-010-003 chon full-replace-style (client luon gui lai 3 gia tri do hien tai, dua theo mockup CB-278 co pre-fill san) | Tat ca test case gia dinh request luon chua ca 3 field do (khong test truong hop "field bi omit khoi JSON body" vi service khong phan biet duoc omitted vs null neu khong dung `JsonNullable`) |
| L4 | `BABY-078` (measurement thuoc baby khac) va `BABY-076` (measurement khong ton tai) tra cung HTTP status + message (theo ADR-BABY-010-004, CWE-639 mitigation) | Test phai assert status + code rieng biet o unit level (service throws exception khac nhau) nhung KHONG assert message khac nhau o API/controller level | `BABY235-TC-007` va `BABY235-TC-008` assert exception type/code o Service layer; API-level message duoc test la giong nhau (khong lo thong tin) |
| L5 | `BABY-079` duoc cap phat nhung TDS S10 ghi ro la "Reserved -- Open, khong co use case xac nhan" | Khong co rule nao trong SRS/schema doi hoi ma loi nay | Khong viet test cho `BABY-079` -- tranh AP-AI-001 (test khong duoc gan voi constraint khong ton tai) |
| L6 | Mockup CB-278 khong co UI input cho `sourceType` va `note`; chi CB-176 (detail screen) hien thi `note` o dang read-only | ADR-BABY-010-005 + TDS S8.1 van giu 2 field nay la mutable qua API (theo chi dinh cua coordinator) du UI mobile hien tai chua expose | Test factory gom san `sourceType`/`note` trong `makeUpdateRequest()` de dam bao API contract van hoat dong dung ngay ca khi UI chua goi toi (forward-compat cho web client tuong lai) -- danh dau `Open` ve viec UI thuc su co dung 2 field nay hay khong |

---

## 3. Test Design Specification (TDS)

### TDS-01 -- Scope

```
UpdateGrowthMeasurement bao gom cac layer:
+-- Service (mock BabyProfileRepository + GrowthMeasurementRepository voi Mockito)
+-- Controller (mock IGrowthMeasurementService voi @WebMvcTest)
+-- Integration (Testcontainers PostgreSQL voi @SpringBootTest)
```

### TDS-02 -- Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-235 (Table 257)` | Mother cap nhat mot growth measurement da ton tai |
| `ADR-BABY-010-001` | Khong can dual-path (Mother-entered only) |
| `ADR-BABY-010-002` | Reuse owner-only check pattern tu UC-38 (`BABY-070`/`BABY-071`) |
| `ADR-BABY-010-003` | Reuse "at least 1 measurement value required" rule tu UC-234, ap dung full-replace-style |
| `ADR-BABY-010-004` | Cross-check `measurement.babyId == path babyId` -> `BABY-078` |
| `ADR-BABY-010-005` | Khong tao bang audit moi -- chi phat `GrowthMeasurementUpdated` event |
| `ADR-BABY-010-006` *(Accepted, 2026-07-04)* | Baby phai ACTIVE cho write -- reconciled voi UC-234 ADR-BABY-009-002, ca hai deu Accepted |
| `BR-RBAC / BR-PRIVACY` | Chi Mother so huu baby moi duoc cap nhat measurement cua baby do |
| `CB-BABY-IMP-010 S8` | Interface contract: `updateGrowthMeasurement(userId, babyId, growthMeasurementId, request)` |
| `CB-BABY-IMP-010 S10` | Bang ma loi: `BABY-070`, `BABY-071` (reused), `BABY-076`, `BABY-077`, `BABY-078` (new), `BABY-079` (reserved, no test) |

### TDS-03 -- Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Update single field (others resent unchanged) | `GrowthMeasurementService.updateGrowthMeasurement()` | `BABY235-TC-001` |
| TC-COND-002 | Update all 6 mutable fields; identity fields remain unchanged | `GrowthMeasurementService.updateGrowthMeasurement()` | `BABY235-TC-002` |
| TC-COND-003 | Reject when weightKg/heightCm/headCircumferenceCm all null | `GrowthMeasurementService.updateGrowthMeasurement()` | `BABY235-TC-003` |
| TC-COND-004 | Reject when measuredDate is null | `GrowthMeasurementService.updateGrowthMeasurement()` | `BABY235-TC-004` |
| TC-COND-005 | Baby not owned by user | `GrowthMeasurementService.updateGrowthMeasurement()` | `BABY235-TC-005` |
| TC-COND-006 | Baby not found | `GrowthMeasurementService.updateGrowthMeasurement()` | `BABY235-TC-006` |
| TC-COND-007 | Growth measurement not found | `GrowthMeasurementService.updateGrowthMeasurement()` | `BABY235-TC-007` |
| TC-COND-008 | Measurement belongs to a different baby (IDOR) | `GrowthMeasurementService.updateGrowthMeasurement()` | `BABY235-TC-008` |
| TC-COND-009 | Unauthenticated access blocked | Spring Security filter | `BABY235-TC-009` |
| TC-COND-010 | Baby ARCHIVED -> rejected (write gate, reconciled with UC-234) | `GrowthMeasurementService.updateGrowthMeasurement()` | `BABY235-TC-010` |
| TC-COND-011 | Full flow via API persists update correctly | Testcontainers | `BABY235-TC-INT-001` |

### TDS-04 -- Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Valid vs invalid combinations of weightKg/heightCm/headCircumferenceCm | Partition: all-present, 1-of-3-present, all-null (rejected) |
| Boundary Value Analysis | Exactly 1 of 3 measurement fields non-null (boundary of the "at least one" rule) | Confirms rule triggers only when ALL 3 are null, not when 1 or 2 are null |
| State Transition Testing | N/A -- no status/lifecycle column on `growth_measurements` | Confirms no state machine exists (contrast with vaccination domain) |
| Error Guessing | Path `babyId` swapped with a different owned baby's ID while measurement ID belongs to another baby | IDOR attack simulation (CWE-639) |

### TDS-05 -- Test Data Requirements

| Fixture ID | Type | Value / Logic | Muc dich |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: 'MOTHER_ID', role: 'MOTHER'}` | Happy path -- Mother authenticated |
| `FX-002` | DB seed | `BabyProfile(babyId=BABY_ID, ownerUserId=MOTHER_ID, status=ACTIVE)` | Target baby |
| `FX-003` | DB seed | `GrowthMeasurement(growthMeasurementId=MEASUREMENT_ID, babyId=BABY_ID, weightKg=8.4, heightCm=72, headCircumferenceCm=44)` | Target measurement (pre-update state) |
| `FX-004` | DB seed | `BabyProfile(babyId=OTHER_BABY_ID, ownerUserId=OTHER_USER_ID)` | Ownership-denied test |
| `FX-005` | DB seed | `GrowthMeasurement(growthMeasurementId=OTHER_MEASUREMENT_ID, babyId=OTHER_BABY_ID)` | IDOR test -- measurement belongs to a baby the caller does not own |
| `FX-006` | Request DTO | `UpdateGrowthMeasurementRequest(measuredDate=2023-11-20, weightKg=8.5, heightCm=72, headCircumferenceCm=44, sourceType="MOTHER_ENTERED", note="...")` | Happy path -- all fields |
| `FX-007` | Request DTO | `UpdateGrowthMeasurementRequest(measuredDate=2023-11-20, weightKg=null, heightCm=null, headCircumferenceCm=null)` | Validation-reject test (`BABY-077`) |
| `FX-008` | Request DTO | `UpdateGrowthMeasurementRequest(measuredDate=null, weightKg=8.5, heightCm=72, headCircumferenceCm=44)` | Validation-reject test (`BABY-077`, measuredDate required) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 -- BAT BUOC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 -- Props Isolation Pattern
// Moi @Test dung factory methods -- khong shared mutable state
// ═══════════════════════════════════════════════════════════

class GrowthMeasurementUpdateTestFactory {
    static UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000235");
    static UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static UUID BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000235");
    static UUID OTHER_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000236");
    static UUID MEASUREMENT_ID = UUID.fromString("aaaa0235-0000-0000-0000-000000000001");
    static UUID OTHER_MEASUREMENT_ID = UUID.fromString("aaaa0236-0000-0000-0000-000000000002");
    static UUID NON_EXISTENT_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-999999999999");
    static UUID NON_EXISTENT_MEASUREMENT_ID = UUID.fromString("aaaa0235-0000-0000-0000-999999999999");

    static BabyProfile makeBaby() {
        return BabyProfile.builder()
            .id(BABY_ID)
            .ownerUserId(MOTHER_ID)
            .nickname("Growth Baby")
            .birthDate(LocalDate.of(2022, 1, 15))
            .status(BabyProfileStatus.ACTIVE)
            .build();
    }

    static BabyProfile makeArchivedBaby() {
        BabyProfile baby = makeBaby();
        baby.setStatus(BabyProfileStatus.ARCHIVED);
        return baby;
    }

    static BabyProfile makeBabyOwnedByOther() {
        BabyProfile baby = makeBaby();
        baby.setId(OTHER_BABY_ID);
        baby.setOwnerUserId(OTHER_USER_ID);
        return baby;
    }

    // Baseline pre-update measurement state -- synced with FX-003
    static GrowthMeasurement makeMeasurement() {
        return GrowthMeasurement.builder()
            .growthMeasurementId(MEASUREMENT_ID)
            .babyId(BABY_ID)
            .measuredDate(LocalDate.of(2023, 10, 20))
            .weightKg(new BigDecimal("8.4"))
            .heightCm(new BigDecimal("72"))
            .headCircumferenceCm(new BigDecimal("44"))
            .sourceType("MOTHER_ENTERED")
            .note("1 month checkup")
            .createdAt(Instant.parse("2023-10-20T08:15:00Z"))
            .updatedAt(Instant.parse("2023-10-20T08:15:00Z"))
            .build();
    }

    static GrowthMeasurement makeMeasurement(Consumer<GrowthMeasurement> overrides) {
        GrowthMeasurement m = makeMeasurement();
        overrides.accept(m);
        return m;
    }

    // Measurement that belongs to a DIFFERENT baby -- used for IDOR test (BABY-078)
    static GrowthMeasurement makeMeasurementOwnedByOtherBaby() {
        return makeMeasurement(m -> {
            m.setGrowthMeasurementId(OTHER_MEASUREMENT_ID);
            m.setBabyId(OTHER_BABY_ID);
        });
    }

    // Happy path -- full request, synced with FX-006
    static UpdateGrowthMeasurementRequest makeUpdateRequest() {
        UpdateGrowthMeasurementRequest req = new UpdateGrowthMeasurementRequest();
        req.setMeasuredDate(LocalDate.of(2023, 11, 20));
        req.setWeightKg(new BigDecimal("8.5"));
        req.setHeightCm(new BigDecimal("72"));
        req.setHeadCircumferenceCm(new BigDecimal("44"));
        req.setSourceType("MOTHER_ENTERED");
        req.setNote("Do buoi sang sau khi thuc day");
        return req;
    }

    static UpdateGrowthMeasurementRequest makeUpdateRequest(Consumer<UpdateGrowthMeasurementRequest> overrides) {
        UpdateGrowthMeasurementRequest req = makeUpdateRequest();
        overrides.accept(req);
        return req;
    }

    // All 3 measurement values nulled -- synced with FX-007 (BABY-077 trigger)
    static UpdateGrowthMeasurementRequest makeUpdateRequestAllValuesNulled() {
        return makeUpdateRequest(req -> {
            req.setWeightKg(null);
            req.setHeightCm(null);
            req.setHeadCircumferenceCm(null);
        });
    }

    // measuredDate null -- synced with FX-008 (BABY-077 trigger)
    static UpdateGrowthMeasurementRequest makeUpdateRequestMissingDate() {
        return makeUpdateRequest(req -> req.setMeasuredDate(null));
    }
}
```

---

### BABY235-TC-001 -- Happy path: update single field (weightKg), other fields resent unchanged

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementService.updateGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-BABY-IMP-010 S6.1 (Sequence Diagram -- Happy Path Single Field) / S8.1`

**Preconditions:**
- Baby profile exists, status ACTIVE, owned by MOTHER_ID
- Measurement exists via `makeMeasurement()` (weightKg=8.4, heightCm=72, headCircumferenceCm=44)
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBaby()`
- Mock: `growthMeasurementRepository.findById(MEASUREMENT_ID)` returns `makeMeasurement()`
- Request: `makeUpdateRequest()` with weightKg changed to 8.5, heightCm/headCircumferenceCm resent unchanged (72/44)

**Test Steps:**
1. Arrange: baby + measurement mocked as above
2. Act: call `growthMeasurementService.updateGrowthMeasurement(MOTHER_ID, BABY_ID, MEASUREMENT_ID, request)`
3. Assert: response returned, `weightKg == 8.5`, `heightCm == 72`, `headCircumferenceCm == 44`
4. Assert: `growthMeasurementRepository.save(...)` invoked exactly once with updated entity
5. Assert: `updatedAt` on saved entity is refreshed (not equal to original `2023-10-20T08:15:00Z`)

**Expected Result (PASS):**
- `GrowthMeasurementResponse` with `weightKg = 8.5`, other fields as sent
- `updatedAt` newer than original

**Expected Result (FAIL):**
- Exception thrown, or `heightCm`/`headCircumferenceCm` incorrectly reset to null, or `updatedAt` unchanged

**Current Status:** RED Not written

---

### BABY235-TC-002 -- Happy path: update all 6 mutable fields; identity fields immutable

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementService.updateGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-BABY-IMP-010 S6.2 (Sequence Diagram -- Happy Path All Fields) / S5.2 (identity fields immutable)`

**Preconditions:**
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBaby()`
- Mock: `growthMeasurementRepository.findById(MEASUREMENT_ID)` returns `makeMeasurement()`
- Request: `makeUpdateRequest()` (full body: measuredDate, weightKg, heightCm, headCircumferenceCm, sourceType, note all changed from baseline)

**Test Steps:**
1. Arrange as above
2. Act: call `updateGrowthMeasurement(MOTHER_ID, BABY_ID, MEASUREMENT_ID, makeUpdateRequest())`
3. Assert: all 6 fields in response match request values
4. Assert: `response.getGrowthMeasurementId() == MEASUREMENT_ID` (unchanged)
5. Assert: `response.getBabyId() == BABY_ID` (unchanged)
6. Assert (via captured entity passed to `save()`): `createdAt` unchanged from `2023-10-20T08:15:00Z`

**Expected Result (PASS):**
- All 6 mutable fields updated; `growthMeasurementId`, `babyId`, `createdAt` unchanged

**Expected Result (FAIL):**
- Identity fields (`growthMeasurementId`, `babyId`, `createdAt`) mutated, or any of the 6 mutable fields not applied

**Current Status:** RED Not written

---

### BABY235-TC-003 -- All measurement values nulled -> 400 BABY-077

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementService.updateGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-BABY-IMP-010 ADR-BABY-010-003 / S6.3 (Sequence Diagram -- Error Path All Nulled) / S10 BABY-077`

**Preconditions:**
- Mock: baby + measurement found and owned (pass ownership/not-found gates)
- Request: `makeUpdateRequestAllValuesNulled()` -- weightKg=null, heightCm=null, headCircumferenceCm=null, measuredDate still set

**Test Steps:**
1. Arrange as above
2. Act: call `updateGrowthMeasurement(MOTHER_ID, BABY_ID, MEASUREMENT_ID, makeUpdateRequestAllValuesNulled())`
3. Assert: `ValidationException` thrown with error code `BABY-077`
4. Assert: `growthMeasurementRepository.save(...)` is NEVER invoked (no partial write)

**Expected Result (PASS):**
- `ValidationException` with code `BABY-077`, no DB write side effect

**Expected Result (FAIL):**
- Update proceeds and persists a measurement with all 3 values null -- violates ADR-BABY-010-003 invariant

**Current Status:** RED Not written
**Implementation Note:** Validation must run BEFORE `save()` is called -- verify via `verify(growthMeasurementRepository, never()).save(any())`.

---

### BABY235-TC-004 -- measuredDate null -> 400 BABY-077

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementService.updateGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-BABY-IMP-010 S8.1 (@NotNull measuredDate) / S10 BABY-077`

**Preconditions:**
- Mock: baby + measurement found and owned
- Request: `makeUpdateRequestMissingDate()` -- measuredDate=null, other fields valid

**Test Steps:**
1. Arrange as above
2. Act: call `updateGrowthMeasurement(MOTHER_ID, BABY_ID, MEASUREMENT_ID, makeUpdateRequestMissingDate())`
3. Assert: `ValidationException` thrown with code `BABY-077`

**Expected Result (PASS):**
- `ValidationException` with code `BABY-077` (or Bean Validation 400 if enforced at controller `@Valid` layer -- either layer acceptable per TDS S8.1, must document which layer in implementation)

**Expected Result (FAIL):**
- Update proceeds with `measuredDate = null` persisted

**Current Status:** RED Not written

---

### BABY235-TC-005 -- Baby not owned by user -> 403 BABY-071

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementService.updateGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-RBAC / CB-BABY-IMP-010 ADR-BABY-010-002 / S10 BABY-071 (reused from CB-BABY-IMP-008)`

**Preconditions:**
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBaby()` but caller is `OTHER_USER_ID` (not `MOTHER_ID`)

**Test Steps:**
1. Arrange: baby owned by `MOTHER_ID`, caller is `OTHER_USER_ID`
2. Act: call `updateGrowthMeasurement(OTHER_USER_ID, BABY_ID, MEASUREMENT_ID, makeUpdateRequest())`
3. Assert: `ForbiddenException` thrown with code `BABY-071`
4. Assert: `growthMeasurementRepository.findById(...)` is NEVER invoked (ownership fails before measurement lookup, per TDS S6.4 sequence order)

**Expected Result (PASS):**
- `ForbiddenException` with code `BABY-071`, no measurement lookup attempted

**Expected Result (FAIL):**
- Update succeeds for non-owned baby -- RBAC violation

**Current Status:** RED Not written

---

### BABY235-TC-006 -- Baby not found -> 404 BABY-070

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementService.updateGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-BABY-IMP-010 S6.5 (Sequence Diagram -- Error Path Not Found) / S10 BABY-070 (reused)`

**Preconditions:**
- Mock: `babyProfileRepository.findById(NON_EXISTENT_BABY_ID)` returns `Optional.empty()`

**Test Steps:**
1. Arrange: non-existent babyId
2. Act: call `updateGrowthMeasurement(MOTHER_ID, NON_EXISTENT_BABY_ID, MEASUREMENT_ID, makeUpdateRequest())`
3. Assert: `ResourceNotFoundException` thrown with code `BABY-070`

**Expected Result (PASS):**
- `ResourceNotFoundException` with code `BABY-070`

**Expected Result (FAIL):**
- `NullPointerException` or unhandled error

**Current Status:** RED Not written

---

### BABY235-TC-007 -- Growth measurement not found -> 404 BABY-076

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementService.updateGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-BABY-IMP-010 S6.5 / S10 BABY-076`

**Preconditions:**
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBaby()` (owned)
- Mock: `growthMeasurementRepository.findById(NON_EXISTENT_MEASUREMENT_ID)` returns `Optional.empty()`

**Test Steps:**
1. Arrange as above
2. Act: call `updateGrowthMeasurement(MOTHER_ID, BABY_ID, NON_EXISTENT_MEASUREMENT_ID, makeUpdateRequest())`
3. Assert: `ResourceNotFoundException` thrown with code `BABY-076`

**Expected Result (PASS):**
- `ResourceNotFoundException` with code `BABY-076`

**Expected Result (FAIL):**
- Exception not thrown, or wrong error code (e.g. confused with `BABY-070`)

**Current Status:** RED Not written

---

### BABY235-TC-008 -- Measurement belongs to a different baby -> 404 BABY-078 (IDOR defense)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 -- Authorization Bypass Through User-Controlled Key`
**OWASP:** `A01:2021 -- Broken Access Control`
**Legal:** `BR-PRIVACY -- growth data must not leak across baby boundaries`
**Feature Under Test:** `GrowthMeasurementService.updateGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-BABY-IMP-010 ADR-BABY-010-004 / S6.5 (IDOR scenario) / S10 BABY-078`

**Preconditions:**
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBaby()` (owned by MOTHER_ID -- ownership check passes)
- Mock: `growthMeasurementRepository.findById(OTHER_MEASUREMENT_ID)` returns `makeMeasurementOwnedByOtherBaby()` (`babyId = OTHER_BABY_ID`)

**Test Steps (Attack Simulation):**
1. Arrange: Mother owns `BABY_ID`; attacker (Mother, acting on her own valid baby) supplies `growthMeasurementId = OTHER_MEASUREMENT_ID`, which actually belongs to `OTHER_BABY_ID` (not owned by her, but she does not know that -- or is probing)
2. Act: call `updateGrowthMeasurement(MOTHER_ID, BABY_ID, OTHER_MEASUREMENT_ID, makeUpdateRequest())`
3. Assert: `ResourceNotFoundException` thrown with code `BABY-078`
4. Assert: `growthMeasurementRepository.save(...)` is NEVER invoked

**Expected Result (PASS = system safe):**
- `ResourceNotFoundException` with code `BABY-078`, identical HTTP status (404) and identical generic message to `BABY-076` at the API layer (per ADR-BABY-010-004, does not confirm/deny existence under another baby)

**Expected Result (FAIL = vulnerability exists):**
- Measurement belonging to `OTHER_BABY_ID` is updated because the code only checked `BABY_ID` ownership and trusted the `growthMeasurementId` blindly

**Current Status:** RED Not written

---

### BABY235-TC-009 -- No JWT -> 401

**Severity:** `CRITICAL`
**CWE:** `CWE-306 -- Missing Authentication for Critical Function`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementControllerTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- No JWT token in request headers

**Test Steps:**
1. Arrange: build `PATCH` request without `Authorization` header
2. Act: call `PATCH /api/v1/babies/{babyId}/growth-measurements/{id}` via MockMvc
3. Assert: response status is 401

**Expected Result (PASS):**
- HTTP 401 Unauthorized; no service method invoked

**Expected Result (FAIL):**
- Request reaches controller/service without authentication

**Current Status:** RED Not written

---

### BABY235-TC-010 -- Baby ARCHIVED -> 400 BABY-073 (write gate, added 2026-07-04 reconciliation)

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementService.updateGrowthMeasurement()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-010` *(new)*
**Oracle Source:** `CB-BABY-IMP-010 ADR-BABY-010-006 (Accepted 2026-07-04, reconciled with UC-234 ADR-BABY-009-002) / mirrors BABY234-TC-010`

**Preconditions:**
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeArchivedBaby()` (status=ARCHIVED, owned by MOTHER_ID)

**Test Steps:**
1. Arrange: baby ARCHIVED, owned by caller, valid update request
2. Act: call `updateGrowthMeasurement(MOTHER_ID, BABY_ID, MEASUREMENT_ID, makeUpdateRequest())`
3. Assert: `BusinessException` thrown with code `BABY-073`; `growthMeasurementRepository.findById(...)` and `.save(...)` never invoked

**Expected Result (PASS):**
- `BusinessException(BABY-073)` thrown -- consistent with UC-234's write-gate; differs from UC-38 (read), which allows ARCHIVED

**Expected Result (FAIL):**
- Update proceeds for an ARCHIVED baby profile -- violates ADR-BABY-010-006 write-gate reconciliation

**Current Status:** RED Not written
**Implementation Note:** This ADR is now `Accepted` (2026-07-04, by user/product owner), matching `ADR-BABY-009-002` (UC-234) -- same as `BABY234-TC-010`, this test case is fully confirmed/required, not conditional/pending.

---

### INTEGRATION TEST CASES

---

### BABY235-TC-INT-001 -- Integration: full flow update persists correctly

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller -> Service -> Repository -> PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/GrowthMeasurementUpdateIntegrationTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-011`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied automatically on Spring context start
- Seed: insert `baby_profiles` row (`BABY_ID`, `MOTHER_ID`, status=ACTIVE) via JPA repository
- Seed: insert `growth_measurements` row (`MEASUREMENT_ID`, `BABY_ID`, weightKg=8.4, heightCm=72, headCircumferenceCm=44) via JPA repository

**Test Steps:**
1. Seed baby_profile and growth_measurement as above
2. Call `PATCH /api/v1/babies/{BABY_ID}/growth-measurements/{MEASUREMENT_ID}` with valid JWT (MOTHER_ID) and body `{measuredDate:"2023-11-20", weightKg:8.5, heightCm:72, headCircumferenceCm:44, sourceType:"MOTHER_ENTERED", note:"..."}`
3. Assert response
4. Re-query DB row directly to confirm persistence

**Expected Result (PASS):**
- Response status 200
- `weightKg = 8.5` in both response and DB row
- `updated_at` column in DB is strictly greater than `created_at`
- `growth_measurement_id` and `baby_id` columns unchanged

**Expected Result (FAIL):**
- Response/DB mismatch, or `updated_at` not refreshed, or identity columns mutated

**DB Assertion:**
```java
GrowthMeasurement row = growthMeasurementRepository.findById(MEASUREMENT_ID).orElseThrow();
assertThat(row.getWeightKg()).isEqualByComparingTo(new BigDecimal("8.5"));
assertThat(row.getHeightCm()).isEqualByComparingTo(new BigDecimal("72"));
assertThat(row.getHeadCircumferenceCm()).isEqualByComparingTo(new BigDecimal("44"));
assertThat(row.getUpdatedAt()).isAfter(row.getCreatedAt());
assertThat(row.getGrowthMeasurementId()).isEqualTo(MEASUREMENT_ID);
assertThat(row.getBabyId()).isEqualTo(BABY_ID);
```

**Current Status:** RED Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit) | REFACTOR note |
|-------|-----------|---------------|-----------------|---------------|
| `BABY235-TC-001` | `GrowthMeasurementServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-002` | `GrowthMeasurementServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-003` | `GrowthMeasurementServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-004` | `GrowthMeasurementServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-005` | `GrowthMeasurementServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-006` | `GrowthMeasurementServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-007` | `GrowthMeasurementServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-008` | `GrowthMeasurementServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-009` | `GrowthMeasurementControllerTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-010` | `GrowthMeasurementServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY235-TC-INT-001` | `GrowthMeasurementUpdateIntegrationTest.java` | `[ ]` | `[hash]` | |

### 5.1 Red Gate Protocol (CASE 2.0 -- GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase -- implementation stub (PHAI throw)
@Service
public class GrowthMeasurementService implements IGrowthMeasurementService {

    @Override
    public GrowthMeasurementResponse updateGrowthMeasurement(
            UUID userId, UUID babyId, UUID growthMeasurementId, UpdateGrowthMeasurementRequest request) {
        throw new UnsupportedOperationException("Not implemented -- Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (neu PASS bat thuong) |
|-------|-------------|----------|--------|----------------------------------|
| `BABY235-TC-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | [ ] Tautology [ ] Shared state [ ] Hallucinated import |
| `BABY235-TC-002` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY235-TC-003` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY235-TC-004` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY235-TC-005` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY235-TC-006` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY235-TC-007` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY235-TC-008` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY235-TC-009` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY235-TC-010` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY235-TC-INT-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tat ca FAIL? [ ] Yes -> **GATE-2 PASS** (T2->T3) -> tiep tuc implement
- Log file: `[path to red-gate-evidence.log]`

> **Neu bat ky test PASS bat thuong:** Dung lai. Xac dinh root cause tu bang tren. Rewrite test tu TC-ID spec voi Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-BABY-IMP-010` da duoc review va approve
- [x] Logic Issues (Section 2) da duoc confirm -- L1/L2 nay da RESOLVED (2026-07-04); ADR-BABY-010-006 (ACTIVE-required) da duoc user/product owner Accept 2026-07-04, khong con NEEDS-DECISION
- [ ] Table `growth_measurements` da ton tai trong DB (khong can migration moi -- xac nhan TDS S5.2/S11.2)
- [ ] Test fixtures (Section 3 TDS-05) da duoc chuan bi
- [ ] Shared ownership-check helper (TDS ADR-BABY-010-002) — package thuc te da xac nhan `com.carebridge.backend.baby.repository`/`entity` (khong phai `carejourney`); `BabyOwnershipPolicy` van can duoc tao moi trong `carejourney.policy`, dong bo vi tri voi UC-38/UC-234/UC-236/UC-237

### Exit Criteria

- [ ] `./mvnw test` -- tat ca unit tests xanh (khong co skip)
- [ ] `./mvnw verify` -- tat ca integration tests xanh (Testcontainers)
- [ ] Test coverage >= 80% lines cho `GrowthMeasurementService`
- [ ] Khong co business logic trong Controller
- [ ] Khong co PII/secret xuat hien plaintext trong logs
- [ ] `BABY-077` correctly rejects all-nulled measurement values AND null measuredDate (both sub-cases covered)
- [ ] `BABY-078` correctly returns identical HTTP status/message to `BABY-076` at API layer (IDOR non-disclosure verified)

**Exit Criteria bo sung -- CASE 2.0:**

- [ ] **Red Gate (S5.1)** -- tat ca tests FAIL voi empty/throw stub truoc khi implement
- [ ] **Contract Existence** -- moi class duoc inject deu ton tai trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** -- khong co shared mutable state giua tests (moi test dung `GrowthMeasurementUpdateTestFactory` factory methods)
- [ ] **Oracle Source** -- moi expected value trong assert co ghi ro nguon (TDS section / ADR)

### Suspension Criteria

- Blocker dependency chua san sang (vi du: UC-234 field naming chua chot -- xem L2)
- Phat hien loi kien truc moi can Principal Architect review
- CI pipeline bi broken boi thay doi khac

---

## 7. Rollback Plan

```bash
# Revert implementation files (mirrors TDS S12.2)
git checkout -- src/main/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementController.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/GrowthMeasurementService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/IGrowthMeasurementService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/request/UpdateGrowthMeasurementRequest.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/response/GrowthMeasurementResponse.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/

# Gap van OPEN -> giu nguyen entry trong PHASE_GAP_ANALYSIS.md (neu co)
```

No migration rollback needed -- no schema change (TDS S5.2/S11.2).

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dau hieu trong TDD spec | Check | Gate chan |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC khong reference ADR/TDS constraint nao | [x] OK -- moi TC co `Oracle Source` trich dan TDS section/ADR cu the | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS voi empty/throw stub (S5.1) | [ ] Pending -- can chay Red Gate that su khi code duoc viet | G-2 |
| AP-AI-003 | Implicit Decision | Test assume architecture decision khong co ADR | [x] OK -- `BABY235-TC-010` test ARCHIVED status gate voi ADR-BABY-010-006 (Accepted 2026-07-04, L1 resolved) lam Oracle Source; khong test `BABY-079` (L5, reserved) | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller co business logic | [x] OK -- `BABY235-TC-009` chi assert 401 tu Security filter, khong assert business logic o Controller | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type khong ton tai trong codebase | [ ] Pending -- `GrowthMeasurementUpdateTestFactory`, `UpdateGrowthMeasurementRequest`, `GrowthMeasurementResponse` deu duoc dinh nghia trong TDS S8.1 nhung chua ton tai trong codebase (greenfield) -- se duoc verify tai `./mvnw compile` step cua Exit Criteria | G-3 |

**Ket qua review:**

- [ ] Khong phat hien anti-pattern nao -> TDD spec approved
- [x] Phat hien AP tiem an -> ghi vao bang duoi -> fix truoc khi implement (2 muc `Pending` la ky vong binh thuong cho giai doan RED, khong phai loi thiet ke)

| AP detected | TC ID | Mo ta | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| AP-AI-002 (expected-pending) | Tat ca TC | Chua chay Red Gate that su vi chua co code | Chay Red Gate ngay sau khi tao stub, truoc khi implement logic that | ☐ |
| AP-AI-005 (expected-pending) | Tat ca TC | Cac class DTO/Service chua ton tai (greenfield) | Xac nhan qua `./mvnw compile` sau khi tao skeleton class | ☐ |

---

*TDD Template v2.0 -- Tich hop CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
