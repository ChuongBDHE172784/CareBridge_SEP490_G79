# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-237 View Growth Measurement History

**Document ID:** `CB-BABY-IMP-013-TEST`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021 -- Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] -- Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal -- Confidential`

**References:**
- TDS: `04_Implement/UC237_ViewGrowthMeasurementHistory/UC237_ViewGrowthMeasurementHistory_TDS.md` (`CB-BABY-IMP-013`)
- SRS: `02_Requirements/SRS/3_Functional_Specification.md` S3.3.19.10 (Table 259)
- Sibling (soft-delete source of truth): `04_Implement/UC236_DeleteGrowthMeasurement/UC236_DeleteGrowthMeasurement_TDS.md` (`CB-BABY-IMP-012`)
- Baseline schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` (dong 607, 647, 1608, 1727)
- UC-236 migration (dependency): `V20260703000100__add_growth_measurement_deleted_at.sql` (Proposed -- cho DBA/Tech Lead sign-off)

---

## CHANGELOG

| Ngay | Nguoi thuc hien | Noi dung thay doi |
|------|-----------------|-------------------|
| 2026-07-03 | AI Agent | Khoi tao TDD spec cho UC-237 View Growth Measurement History |

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
| **Feature / Gap ID** | `UC-237` |
| **Module** | `ViewGrowthMeasurementHistory -- carejourney` |
| **Spec goc** | `CB-BABY-IMP-013` |
| **Priority** | P2 (SRS Table 259: Priority = Medium) |
| **Data Classification** | `PII` |
| **Compliance Scope** | `BR-RBAC` (SRS Table 259 chi liet ke BR-RBAC) |
| **Upstream Dependencies** | `auth (JWT), baby (baby_profiles), growth_measurements, UC-236 deleted_at column (V20260703000100)` |
| **Downstream Consumers** | `Mobile app history screen (CB-175)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-BABY-IMP-013 S17` |
| **Constraints Injected** | C1 (ownership), C2 (ARCHIVED allowed), C3 (deleted_at IS NULL filter at DB layer), C4 (sort DESC + pagination + empty=200), C5 (read-only, no medical interpretation) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 -> T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec goc (sai / thieu) | Thuc te (schema / policy) | Fix ap dung trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Table 259 Normal Flow chi mo ta generic 5 buoc, khong noi ro sort order | UI mockup CB-175 hien thi ban ghi moi nhat (15 Thang 10) truoc ban ghi cu hon (15 Thang 9) -> DESC. TDS S6.1/ADR-BABY-013-001 xac nhan `measured_date DESC` | `BABY237-TC-001` assert content[0] la ban ghi moi nhat |
| L2 | SRS AF2: "No matching data is available -> empty state" -- khong noi ro HTTP status | TDS S6.2 xac nhan 200 + empty Page (khong 404), nhat quan voi UC-38 pattern (GROWTH-TC-038-002) | `BABY237-TC-002` assert 200 + empty content, KHONG throw |
| L3 | Bang `growth_measurements` goc (V1, dong 647-658) KHONG co cot `deleted_at` -- UC-237 TDS ban dau ghi day la `Open` (O1) | UC-236 TDS (`CB-BABY-IMP-012`, da hoan thanh tren dia) xac nhan chinh xac: cot `deleted_at timestamptz NULL`, migration `V20260703000100__add_growth_measurement_deleted_at.sql`, repository method de xuat `findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(babyId, Pageable)` (UC-236 S8.2 + ADR-BABY-012-003) -- **khop 100% voi ten cot/method da dung trong UC-237 TDS S6.1/S8.2** | `BABY237-TC-003` va `BABY237-TC-INT-001` dung dung ten cot `deleted_at` va method nay; khong con Open cho ten cot |
| L4 | UC-237 TDS (S4.4 O1) tung coi UC-236 migration la dependency chua xac nhan | UC-236 TDS hien la `Draft` voi ADR-BABY-012-001 o trang thai **Proposed** (cho DBA/Tech Lead sign-off) -- CHUA `Accepted`, migration file CHUA duoc tao that su | Entry Criteria (S6) cua tai lieu nay yeu cau ADR-BABY-012-001 chuyen `Accepted` VA migration da apply tren staging truoc khi chay Red Gate that (khong chi truoc mock unit test) |
| L5 | SRS khong dinh nghia gioi han page size | TDS S4.1 dat baseline 20/50 (ke thua UC-202 CB-CON-IMP-003) | `BABY237-TC-007` (pagination) va `BABY237-TC-008` (invalid size) dung baseline nay lam oracle, ghi ro la baseline ke thua, khong phai SRS-derived |
| L6 | Test factory `makeBaby()`/`makeArchivedBaby()` dung `BabyProfile.builder().babyId(BABY_ID)...status("ACTIVE")` / `.setStatus("ARCHIVED")` | **RESOLVED 2026-07-04** -- xac nhan truc tiep tren dia (`baby/entity/BabyProfile.java`, `BabyProfileStatus.java`): field ID thuc te la `id` (KHONG phai `babyId`); `status` la enum `BabyProfileStatus` (ACTIVE/ARCHIVED), KHONG phai `String`. Code cu se KHONG compile | Factory da sua thanh `.id(BABY_ID)` va `.status(BabyProfileStatus.ACTIVE)` / `.setStatus(BabyProfileStatus.ARCHIVED)` (xem § Props Isolation Boilerplate) |

---

## 3. Test Design Specification (TDS)

### TDS-01 -- Scope

```
ViewGrowthMeasurementHistory bao gom cac layer:
+-- Service (mock BabyProfileRepository + GrowthMeasurementRepository voi Mockito)
+-- Mapper (pure function -- unit test truc tiep, khong mock)
+-- Controller (mock IGrowthMeasurementHistoryService voi @WebMvcTest)
+-- Integration (Testcontainers PostgreSQL voi @SpringBootTest, bao gom migration V20260703000100)
```

### TDS-02 -- Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-237 (Table 259)` | Mother xem lich su do luong tang truong; AF2 empty state; BR-RBAC |
| `CB-BABY-IMP-013 ADR-BABY-013-001` | Raw paginated list DESC, khac chart shape UC-38 |
| `CB-BABY-IMP-013 ADR-BABY-013-002` | Loai bo row soft-deleted (`deleted_at IS NULL`) |
| `CB-BABY-IMP-013 ADR-BABY-013-003` | Cho phep xem baby ACTIVE hoac ARCHIVED |
| `CB-BABY-IMP-012 (UC-236 TDS) S8.2/ADR-BABY-012-003` | Ten cot `deleted_at`, ten method repository, nghia vu cross-cutting doi voi UC-237 |
| `CB-BABY-IMP-013 S8` | Interface contract: `getHistory(userId, babyId, pageable)` |
| `CB-CON-IMP-003 (UC-202)` | Pagination default/max baseline (20/50) |

### TDS-03 -- Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid history retrieval, sorted DESC | `GrowthMeasurementHistoryService.getHistory()` | `BABY237-TC-001` |
| TC-COND-002 | No measurements -> 200 empty Page (not 404) | `GrowthMeasurementHistoryService.getHistory()` | `BABY237-TC-002` |
| TC-COND-003 | Soft-deleted rows excluded from result | `GrowthMeasurementHistoryService.getHistory()` | `BABY237-TC-003` |
| TC-COND-004 | Baby not owned by user | `GrowthMeasurementHistoryService.getHistory()` | `BABY237-TC-004` |
| TC-COND-005 | Archived baby allowed for history view | `GrowthMeasurementHistoryService.getHistory()` | `BABY237-TC-005` |
| TC-COND-006 | Baby not found | `GrowthMeasurementHistoryService.getHistory()` | `BABY237-TC-006` |
| TC-COND-007 | Pagination correctness (page/size boundaries) | `GrowthMeasurementHistoryService.getHistory()` | `BABY237-TC-007` |
| TC-COND-008 | Invalid pagination param rejected | `GrowthMeasurementHistoryController` | `BABY237-TC-008` |
| TC-COND-009 | Unauthenticated access blocked | Spring Security filter | `BABY237-TC-009` |
| TC-COND-010 | Full flow over PostgreSQL: sorted DESC + soft-delete filter + pagination | Testcontainers integration | `BABY237-TC-INT-001` |

### TDS-04 -- Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Baby status ACTIVE vs ARCHIVED | Ca hai deu phai tra 200 (ADR-BABY-013-003) |
| Boundary Value Analysis | page size = 0, 1, 50, 51; page = -1, 0 | Bien tren/duoi cua pagination (BABY-084) |
| State Transition Testing | `deleted_at` NULL -> NOT NULL (tu goc nhin doc) | Row phai bien mat khoi ket qua ngay khi soft-delete |
| Error Guessing | Client gui `sort` param tuy y, `size` am, `size` cuc lon | Phong thu tham so dau vao |

### TDS-05 -- Test Data Requirements

| Fixture ID | Type | Value / Logic | Muc dich |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: 'MOTHER_ID', role: 'MOTHER'}` | Happy path -- Mother authenticated |
| `FX-002` | DB seed | `BabyProfile(babyId=BABY_ID, ownerUserId=MOTHER_ID, status=ACTIVE, birthDate=2026-01-15)` | Target baby |
| `FX-003` | DB seed | 3 `GrowthMeasurement` cho `BABY_ID`, tat ca `deleted_at = NULL` | Danh sach song |
| `FX-004` | DB seed | `BabyProfile(status=ARCHIVED)` | Test archived-baby-viewable |
| `FX-005` | DB seed | `BabyProfile` khong co growth_measurements | Test empty history |
| `FX-006` | DB seed | 1 `GrowthMeasurement` voi `deleted_at = now()` | Test soft-deleted-excluded |
| `FX-007` | Pageable | `PageRequest.of(0, 2)`, `PageRequest.of(1, 2)` | Test pagination boundary |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 -- BAT BUOC)

```java
// ================================================================
// CASE 2.0 -- Props Isolation Pattern
// Moi @Test dung factory method -- khong shared mutable state
// ================================================================

class GrowthHistoryTestFactory {
    static UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000237");
    static UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000299");
    static UUID BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000237");
    static UUID NON_EXISTENT_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-999999999237");

    static BabyProfile makeBaby() {
        return BabyProfile.builder()
            .id(BABY_ID)
            .ownerUserId(MOTHER_ID)
            .nickname("History Baby")
            .birthDate(LocalDate.of(2026, 1, 15))
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
        baby.setOwnerUserId(OTHER_USER_ID);
        return baby;
    }

    static GrowthMeasurement makeMeasurement(LocalDate measuredDate, Instant deletedAt) {
        return GrowthMeasurement.builder()
            .growthMeasurementId(UUID.randomUUID())
            .babyId(BABY_ID)
            .measuredDate(measuredDate)
            .weightKg(new BigDecimal("6.0"))
            .heightCm(new BigDecimal("60"))
            .headCircumferenceCm(new BigDecimal("40"))
            .sourceType("MANUAL")
            .note("checkup")
            .createdAt(measuredDate.atStartOfDay(ZoneOffset.UTC).toInstant())
            .deletedAt(deletedAt)
            .build();
    }

    // Danh sach 3 ban ghi song, chua sap xep -- dung de assert service tu sort DESC
    static List<GrowthMeasurement> makeLiveMeasurementsUnsorted() {
        return new ArrayList<>(List.of(
            makeMeasurement(LocalDate.of(2026, 2, 15), null),
            makeMeasurement(LocalDate.of(2026, 4, 15), null),
            makeMeasurement(LocalDate.of(2026, 3, 15), null)
        ));
    }

    static List<GrowthMeasurement> makeMeasurements(Consumer<List<GrowthMeasurement>> overrides) {
        List<GrowthMeasurement> list = makeLiveMeasurementsUnsorted();
        overrides.accept(list);
        return list;
    }
}
```

---

### BABY237-TC-001 -- Happy path: history populated, sorted measured_date DESC

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementHistoryService.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementHistoryServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-BABY-IMP-013 S6.1 (Sequence -- Happy View), S8 Interface, ADR-BABY-013-001`

**Preconditions:**
- Baby profile ACTIVE, owned by `MOTHER_ID`, `FX-002`
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBaby()`
- Mock: `growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(BABY_ID, pageable)` returns `Page` of 3 rows already sorted DESC by the repository query as per ADR-BABY-013-001 (`2026-04-15, 2026-03-15, 2026-02-15`)

**Test Steps:**
1. Arrange: baby via `makeBaby()`; repository mock trả `Page` da sort DESC (repository-level sort duoc DB dam nhiem -- unit test chi verify service khong lam xao tron thu tu do repository tra ve)
2. Act: goi `growthMeasurementHistoryService.getHistory(MOTHER_ID, BABY_ID, PageRequest.of(0, 20))`
3. Assert: response khong null
   - `content.size() == 3`
   - `content[0].measuredDate == 2026-04-15` (newest first)
   - `content[2].measuredDate == 2026-02-15`
   - moi item co du 8 field: growthMeasurementId, measuredDate, weightKg, heightCm, headCircumferenceCm, sourceType, note, createdAt

**Expected Result (PASS):**
- `Page<GrowthMeasurementHistoryItem>` voi 3 item, thu tu giu nguyen tu repository (DESC)

**Expected Result (FAIL):**
- Exception thrown, hoac service tu sap xep lai sai thu tu, hoac thieu field trong mapper

**Current Status:** RED Not written
**Implementation Note:** Service KHONG duoc tu sort trong Java -- tin tuong repository query (ADR-BABY-013-001, C4).

---

### BABY237-TC-002 -- No measurements -> 200 with empty Page (AF2, not 404)

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementHistoryService.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementHistoryServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-BABY-IMP-013 S6.2 (Sequence -- Happy Empty), SRS Table 259 AF2`

**Preconditions:**
- Baby profile ACTIVE, owned by `MOTHER_ID`, `FX-005` (no measurements)
- Mock: `growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(BABY_ID, pageable)` returns empty `Page`

**Test Steps:**
1. Arrange: baby exists, no measurement rows
2. Act: goi `getHistory(MOTHER_ID, BABY_ID, PageRequest.of(0, 20))`
3. Assert: response khong null, `content` rong (`[]`, khong null), `totalElements == 0`, khong throw exception

**Expected Result (PASS):**
- `Page` voi `content = []`, `totalElements = 0` -- semantics 200

**Expected Result (FAIL):**
- `ResourceNotFoundException` bi throw (coi empty la "not found") -- vi pham AF2
- `content` la `null` thay vi empty list

**Current Status:** RED Not written
**Implementation Note:** Service phai tra `Page.empty(pageable)` hoac tuong duong, khong bao gio throw khi khong co du lieu.

---

### BABY237-TC-003 -- Soft-deleted rows excluded from history

**Severity:** `CRITICAL`
**CWE:** `CWE-639 -- Authorization Bypass Through User-Controlled Key (data exposure variant: stale/deleted data leak)`
**Feature Under Test:** `GrowthMeasurementHistoryService.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementHistoryServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-BABY-IMP-013 ADR-BABY-013-002, S6.3 (Sequence -- Soft-Deleted Excluded); CB-BABY-IMP-012 (UC-236 TDS) ADR-BABY-012-001 (cot deleted_at) + ADR-BABY-012-003 (cross-cutting requirement, ten method repository)`

**Preconditions:**
- Baby profile ACTIVE, owned by `MOTHER_ID`
- Mock: `growthMeasurementRepository.findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(BABY_ID, pageable)` -- **method nay theo dinh nghia da loc `deleted_at IS NULL` o tang query**, nen mock chi tra ve cac row song (2 trong 3 row cua `FX-003`/`FX-006`); row co `deletedAt != null` (`FX-006`) KHONG nam trong danh sach mock tra ve

**Test Steps:**
1. Arrange: repository mock mo phong dung hanh vi cua derived query (chi tra live rows)
2. Act: goi `getHistory(MOTHER_ID, BABY_ID, PageRequest.of(0, 20))`
3. Assert: `content.size() == 2` (row da soft-delete khong xuat hien); khong co item nao trong response mang `growthMeasurementId` cua row da xoa

**Expected Result (PASS):**
- Response chi chua cac row `deleted_at IS NULL`

**Expected Result (FAIL):**
- Row da soft-delete xuat hien trong response (data leak -- vi pham ADR-BABY-013-002 va nghia vu cross-cutting cua ADR-BABY-012-003)

**Current Status:** RED Not written
**Implementation Note:** Day la test **CRITICAL** cho rang buoc cross-cutting giua UC-236 va UC-237 -- neu service goi nham method `findByBabyId(...)` (khong loc) thay vi `findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(...)`, test nay phai FAIL ro rang.

---

### BABY237-TC-004 -- Baby not owned -> 403 BABY-071

**Severity:** `CRITICAL`
**Feature Under Test:** `GrowthMeasurementHistoryService.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementHistoryServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-RBAC; CB-BABY-IMP-013 S10 (BABY-071, reused from CB-BABY-IMP-008)`

**Preconditions:**
- Baby profile ton tai nhung owned by `OTHER_USER_ID`
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeBabyOwnedByOther()`

**Test Steps:**
1. Arrange: baby owned by `OTHER_USER_ID`
2. Act: goi `getHistory(MOTHER_ID, BABY_ID, pageable)`
3. Assert: `ForbiddenException` thrown voi `code == "BABY-071"`; `growthMeasurementRepository` KHONG duoc goi (fail-fast truoc khi truy van measurements)

**Expected Result (PASS):**
- `ForbiddenException` voi `BABY-071`; khong data measurement nao bi lo

**Expected Result (FAIL):**
- Du lieu measurement tra ve cho baby khong so huu -- vi pham RBAC

**Current Status:** RED Not written

---

### BABY237-TC-005 -- Archived baby -> 200 (read-side leniency)

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementHistoryService.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementHistoryServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-BABY-IMP-013 ADR-BABY-013-003 (reuse ADR-BABY-008-002)`

**Preconditions:**
- Baby profile ARCHIVED, owned by `MOTHER_ID`
- Mock: `babyProfileRepository.findById(BABY_ID)` returns `makeArchivedBaby()`
- Mock: repository returns live measurements

**Test Steps:**
1. Arrange: baby status = ARCHIVED
2. Act: goi `getHistory(MOTHER_ID, BABY_ID, pageable)`
3. Assert: response tra ve thanh cong voi measurements -- khong throw

**Expected Result (PASS):**
- History tra ve du (khong co status gate cho read)

**Expected Result (FAIL):**
- Exception bi throw cho baby ARCHIVED -- vi pham ADR-BABY-013-003

**Current Status:** RED Not written

---

### BABY237-TC-006 -- Baby not found -> 404 BABY-070

**Severity:** `HIGH`
**Feature Under Test:** `GrowthMeasurementHistoryService.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementHistoryServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-BABY-IMP-013 S10 (BABY-070, reused from CB-BABY-IMP-008)`

**Preconditions:**
- Mock: `babyProfileRepository.findById(NON_EXISTENT_BABY_ID)` returns `Optional.empty()`

**Test Steps:**
1. Arrange: babyId khong ton tai
2. Act: goi `getHistory(MOTHER_ID, NON_EXISTENT_BABY_ID, pageable)`
3. Assert: `ResourceNotFoundException` thrown voi `code == "BABY-070"`

**Expected Result (PASS):**
- `ResourceNotFoundException` voi `BABY-070`

**Expected Result (FAIL):**
- `NullPointerException` hoac loi khong xu ly

**Current Status:** RED Not written

---

### BABY237-TC-007 -- Pagination correctness (page/size boundaries)

**Severity:** `MEDIUM`
**Feature Under Test:** `GrowthMeasurementHistoryService.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/GrowthMeasurementHistoryServiceTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-BABY-IMP-013 S9.2 (query params page/size), S4.1 (pagination baseline 20/50 -- ke thua CB-CON-IMP-003 UC-202)`

**Preconditions:**
- Baby co 3 measurements song (`FX-003`)
- `FX-007`: `PageRequest.of(0, 2)` va `PageRequest.of(1, 2)`

**Test Steps:**
1. Arrange: mock repository tra `Page` voi `totalElements=3` tuong ung tung `Pageable` (page 0 -> 2 items, page 1 -> 1 item)
2. Act: goi `getHistory(MOTHER_ID, BABY_ID, PageRequest.of(0, 2))` roi `getHistory(MOTHER_ID, BABY_ID, PageRequest.of(1, 2))`
3. Assert:
   - Page 0: `content.size() == 2`, `number == 0`, `totalElements == 3`, `totalPages == 2`
   - Page 1: `content.size() == 1`, `number == 1`

**Expected Result (PASS):**
- Metadata phan trang (`number`, `size`, `totalElements`, `totalPages`) dung voi input `Pageable`

**Expected Result (FAIL):**
- Metadata sai lech, hoac service tu cat/gop trang trong Java thay vi de DB/`Pageable` xu ly

**Current Status:** RED Not written

---

### BABY237-TC-008 -- Invalid pagination parameter -> 400 BABY-084

**Severity:** `MEDIUM`
**Feature Under Test:** `GrowthMeasurementHistoryController.getHistory()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementHistoryControllerTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-BABY-IMP-013 S10 (BABY-084), S9.2`

**Preconditions:**
- Valid JWT for `MOTHER_ID`

**Test Steps:**
1. Arrange: build GET request voi `size=999` (vuot boundary 50)
2. Act: goi `GET /api/v1/babies/{babyId}/growth-measurements?size=999` via MockMvc
3. Assert: response status `400`, body `error.code == "BABY-084"`; service KHONG duoc goi

**Expected Result (PASS):**
- HTTP 400, `BABY-084`, khong goi xuong service layer

**Expected Result (FAIL):**
- Request duoc chap nhan voi size=999 (khong co gioi han) -- rui ro tai nguyen/DoS nho

**Current Status:** RED Not written
**Implementation Note:** Kiem tra bien: `size=0` va `size=51` cung phai bi tu choi (Boundary Value Analysis, TDS-04).

---

### SECURITY TEST CASES

---

### BABY237-TC-009 -- No JWT -> 401 Unauthorized

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 -- Identification and Authentication Failures`
**CWE:** `CWE-306 -- Missing Authentication for Critical Function`
**Legal:** `BR-RBAC -- vi pham neu endpoint PII bi truy cap khong xac thuc`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementHistoryControllerTest.java`
**TDD Phase:** RED -- chua implement

**Preconditions:**
- Khong co JWT trong request headers

**Test Steps (Attack Simulation):**
1. Chuan bi GET request khong co `Authorization` header
2. Goi `GET /api/v1/babies/{babyId}/growth-measurements` qua MockMvc
3. Kiem tra response status va rang buoc service khong bi goi

**Expected Result (PASS = he thong an toan):**
- HTTP `401`; `growthMeasurementHistoryService` KHONG duoc invoke

**Expected Result (FAIL = lo hong ton tai):**
- Request tiep can duoc controller/service ma khong xac thuc -- lo PII cua tre em

**Current Status:** RED Not written

---

### INTEGRATION TEST CASES

> Dung Testcontainers (`PostgreSqlContainer`). Timeout: 120s. Yeu cau migration `V20260703000100__add_growth_measurement_deleted_at.sql` (UC-236) da duoc apply tu dong khi Spring context start (Flyway).

---

### BABY237-TC-INT-001 -- Full flow: sorted DESC + soft-delete filter + pagination over real PostgreSQL

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller -> Service -> Repository -> PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/GrowthMeasurementHistoryIntegrationTest.java`
**TDD Phase:** RED -- chua implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-BABY-IMP-013 S13.2 (TC-INT-001); CB-BABY-IMP-012 (UC-236) migration V20260703000100`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration `V1__init_schema.sql` + `V20260703000100__add_growth_measurement_deleted_at.sql` applied
- Seed: `baby_profiles` row (`BABY_ID`, `MOTHER_ID`, `birthDate=2026-01-15`, `status=ACTIVE`)
- Seed: 4 `growth_measurements` rows voi `measured_date`: `2026-01-15, 2026-02-15, 2026-03-15, 2026-04-15`
- Seed: row `2026-03-15` co `deleted_at = now()` (da soft-delete)

**Test Steps:**
1. Seed `baby_profiles` qua JPA repository
2. Seed 4 `growth_measurements` qua JPA repository (1 row voi `deleted_at` set)
3. Goi `GET /api/v1/babies/{BABY_ID}/growth-measurements?page=0&size=2` voi JWT hop le (`MOTHER_ID`)
4. Assert response trang 0
5. Goi tiep `page=1&size=2`, assert response trang 1

**Expected Result (PASS):**
- Trang 0: `content.size() == 2`, thu tu `[2026-04-15, 2026-02-15]` (DESC, bo qua row `2026-03-15` da xoa mem)
- Trang 1: `content.size() == 1`, `content[0].measuredDate == 2026-01-15`
- `totalElements == 3` (4 seeded - 1 soft-deleted)
- Row `2026-03-15` (da soft-delete) KHONG xuat hien o bat ky trang nao

**Expected Result (FAIL):**
- Row da soft-delete xuat hien trong bat ky response nao
- Thu tu khong dung DESC
- `totalElements` tinh sai (vi du dem ca row da xoa)

**DB Assertion:**
```java
GrowthMeasurementHistoryResponse page0 = objectMapper.readValue(
    result0.getResponse().getContentAsString(),
    new TypeReference<ApiResponse<PageDto<GrowthMeasurementHistoryItem>>>() {}
).getData();

assertThat(page0.getContent()).hasSize(2);
assertThat(page0.getContent().get(0).getMeasuredDate()).isEqualTo(LocalDate.of(2026, 4, 15));
assertThat(page0.getContent().get(1).getMeasuredDate()).isEqualTo(LocalDate.of(2026, 2, 15));
assertThat(page0.getTotalElements()).isEqualTo(3);
assertThat(page0.getContent())
    .extracting(GrowthMeasurementHistoryItem::getMeasuredDate)
    .doesNotContain(LocalDate.of(2026, 3, 15)); // soft-deleted row must never leak
```

**Current Status:** RED Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit) | REFACTOR note |
|-------|-----------|---------------|-----------------|-----------------|
| `BABY237-TC-001` | `GrowthMeasurementHistoryServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY237-TC-002` | `GrowthMeasurementHistoryServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY237-TC-003` | `GrowthMeasurementHistoryServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY237-TC-004` | `GrowthMeasurementHistoryServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY237-TC-005` | `GrowthMeasurementHistoryServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY237-TC-006` | `GrowthMeasurementHistoryServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY237-TC-007` | `GrowthMeasurementHistoryServiceTest.java` | `[ ]` | `[hash]` | |
| `BABY237-TC-008` | `GrowthMeasurementHistoryControllerTest.java` | `[ ]` | `[hash]` | |
| `BABY237-TC-009` | `GrowthMeasurementHistoryControllerTest.java` | `[ ]` | `[hash]` | |
| `BABY237-TC-INT-001` | `GrowthMeasurementHistoryIntegrationTest.java` | `[ ]` | `[hash]` | |

### 5.1 Red Gate Protocol (CASE 2.0 -- GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase -- implementation stub (PHAI throw)
@Service
public class GrowthMeasurementHistoryService implements IGrowthMeasurementHistoryService {

    @Override
    public Page<GrowthMeasurementHistoryItem> getHistory(UUID userId, UUID babyId, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented -- Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (neu PASS bat thuong) |
|-------|-------------|----------|--------|----------------------------------|
| `BABY237-TC-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | [ ] Tautology [ ] Shared state [ ] Hallucinated import |
| `BABY237-TC-002` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY237-TC-003` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY237-TC-004` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY237-TC-005` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY237-TC-006` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY237-TC-007` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY237-TC-008` | `throw('Not implemented')` (via controller -> service stub) | FAIL | [ ] FAIL [ ] PASS | |
| `BABY237-TC-009` | N/A -- Security filter blocks before reaching stub | FAIL (401 expected regardless of stub) | [ ] FAIL [ ] PASS | |
| `BABY237-TC-INT-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tat ca FAIL? [ ] Yes -> **GATE-2 PASS** (T2->T3) -> tiep tuc implement
- Log file: `[path to red-gate-evidence.log]`

> **Neu bat ky test PASS bat thuong voi stub throw:** dung lai, xac dinh root cause tu bang tren truoc khi implement that.

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-BABY-IMP-013` da duoc review va approve (Status chuyen tu `Draft`)
- [ ] Logic Issues (Section 2) da duoc confirm voi Principal Architect, dac biet L3/L4 (dependency UC-236)
- [ ] **UC-236 ADR-BABY-012-001 da duoc Tech Lead + DBA chuyen `Proposed -> Accepted`** va migration `V20260703000100__add_growth_measurement_deleted_at.sql` da apply thanh cong tren staging (block cung -- neu chua, KHONG the chay Red Gate that voi Testcontainers vi cot `deleted_at` chua ton tai)
- [ ] Table `growth_measurements`, `baby_profiles` da ton tai trong DB (V1 -- da xac nhan)
- [ ] Test fixtures (Section 3 TDS-05) da duoc chuan bi

### Exit Criteria

- [ ] `./mvnw test` -- tat ca unit tests xanh (khong co skip)
- [ ] `./mvnw verify` -- tat ca integration tests xanh (Testcontainers)
- [ ] Test coverage >= 80% lines cho `GrowthMeasurementHistoryService`
- [ ] Khong co business logic trong Controller (chi validation pagination + mapping)
- [ ] Khong co PII/secret xuat hien plaintext trong logs
- [ ] Soft-deleted rows KHONG bao gio xuat hien trong bat ky test case nao (BABY237-TC-003, INT-001)
- [ ] Pagination metadata (`totalElements`, `totalPages`, `number`) dung 100% cac test case boundary

**Exit Criteria bo sung -- CASE 2.0:**

- [ ] **Red Gate (S5.1)** -- tat ca tests FAIL voi empty/throw stub truoc khi implement
- [ ] **Contract Existence** -- moi class duoc inject deu ton tai trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** -- khong co shared mutable state giua tests (moi test dung `GrowthHistoryTestFactory`)
- [ ] **Oracle Source** -- moi expected value trong assert co ghi ro nguon (TDS section / ADR / SRS)

### Suspension Criteria

- UC-236 migration (`deleted_at`) chua duoc apply tren moi truong test -- **suspend toan bo Integration Tests** (BABY237-TC-INT-001) cho den khi resolved
- Phat hien loi kien truc moi can Principal Architect review
- CI pipeline bi broken boi thay doi khac

---

## 7. Rollback Plan

```bash
# Revert implementation files (UC-237 khong tao migration rieng -- chi tieu thu cot cua UC-236)
git checkout -- src/main/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementHistoryController.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/IGrowthMeasurementHistoryService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/service/GrowthMeasurementHistoryService.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/response/GrowthMeasurementHistoryItem.java
git checkout -- src/main/java/com/carebridge/backend/carejourney/mapper/GrowthMeasurementMapper.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/

# KHONG rollback migration V20260703000100 tai day -- migration do thuoc so huu cua UC-236;
# neu can rollback cot deleted_at, thuc hien theo Rollback Plan cua UC-236 TDS (S12.2), khong phai tai day.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dau hieu trong TDD spec | Check | Gate chan |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC khong reference ADR/TDS constraint nao | [x] OK -- moi TC co Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS voi empty/throw stub (S5.1) | [ ] Pending -- xac nhan sau khi chay Red Gate that | G-2 (Gate quan trong nhat) |
| AP-AI-003 | Implicit Decision | Test assume architecture decision khong co ADR (vd: gia dinh ten cot `deleted_at` ma khong doi chieu UC-236) | [x] OK -- da doi chieu truc tiep voi `CB-BABY-IMP-012` S8.2/ADR-BABY-012-001, khop 100% | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller co business logic | [x] OK -- controller test (TC-008/009) chi kiem tra validation/auth, khong business logic | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type khong ton tai trong codebase | [ ] Pending -- can xac nhan sau khi `IGrowthMeasurementHistoryService`/`GrowthMeasurementRepository` duoc tao that (hien la greenfield, chua co code) | G-3 |

**Ket qua review:**

- [ ] Khong phat hien anti-pattern nao -> TDD spec approved
- [x] Phat hien AP tiem an -> ghi vao bang duoi -> fix truoc khi implement

| AP detected | TC ID | Mo ta | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| AP-AI-005 (tiem an) | Tat ca TC | Repository method `findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc` va entity field `deletedAt` **chua ton tai trong codebase** (greenfield, phu thuoc UC-236 tao truoc) | Verify contract existence (`./mvnw compile`) ngay sau khi UC-236 va UC-237 code duoc viet, TRUOC khi mark bat ky TC nao la GREEN | [ ] |
| AP-AI-002 (tiem an) | `BABY237-TC-009` | Test security co the "PASS" ngay ca khi service stub throw, vi Spring Security filter chan truoc khi toi controller -- can phan biet ro day la PASS hop le (do filter chain), khong phai Green-from-Birth cua business logic | Ghi chu ro trong Red Gate table (S5.1) rang TC-009 PASS boi security filter la ky vong dung, khac voi cac TC con lai phai FAIL boi stub throw | [x] Da ghi chu trong S5.1 |

---

*TDD Template v2.0 -- Tich hop CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
