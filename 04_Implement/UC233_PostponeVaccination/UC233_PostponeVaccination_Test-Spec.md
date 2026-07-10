# TEST-DRIVEN DEVELOPMENT SPECIFICATION — UC-233 Postpone Vaccination

**Document ID:** `CB-VAC-TEST-233`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented - 2026-07-10 (8/17 PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent — Test Designer`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — primary CareBridge database schema source (`vaccination_records` §660-672, §1609-1610, §1730-1734)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260627100500__create_vaccination_reference.sql` — reference catalog seed
- `02_Requirements/SRS/3_Functional_Specification.md` §3.3.19.6 Table 255 — UC-233 Postpone Vaccination
- `04_Implement/UC233_PostponeVaccination/UC233_PostponeVaccination_TDS.md` (CB-VAC-IMP-233) — Technical Specification, ADR-VAC-233-001..007
- `04_Implement/UC232_MarkVaccinationCompleted/UC232_MarkVaccinationCompleted_TDS.md` (CB-VAC-IMP-005) — sibling dual-path pattern (reused)
- `05_Development/Database/postgres/V1_SCHEMA_BASELINE.md` — schema baseline reference-copy doc

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-10` | `AI Agent` | Truthful sync after implementation evidence: status set to Partially Implemented, 8/17 tests PASS in VaccinationServiceImplTest; Red Gate not reconstructed because implementation pre-existed; controller/INT/E2E remain pending |
| 2026-07-03 | AI Agent (Test Designer) | Khởi tạo Test-Spec cho UC-233 Postpone Vaccination — bao phủ dual-path, repeat-postpone, already-completed-rejected, ownership-denied, reason-required |
| 2026-07-04 | AI Agent (Test Designer) | ADR-VAC-233-005 Accepted (Option A, user/product owner) — un-gated toàn bộ test cases trước đó điều kiện theo ADR này: L1 (§2), VAC233-TC-001/002/003 nay assert `postponeReason` persistence bắt buộc (ArgumentCaptor), VAC233-TC-INT-001 assert cột `postpone_reason` DB thật là bắt buộc (không còn `[NẾU Option A]`). Cập nhật Entry/Suspension Criteria (§6), Rollback Plan (§7), AP-AI-005 (§8), CG-9 (§9) để bỏ gating. Status tài liệu vẫn `Draft`. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0 — GATE-2)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)
9. [Consistency Gate (CG-1..CG-9) — TDS ↔ Test-Spec](#9-consistency-gate-cg-1cg-9--tds--test-spec) ⭐ *Bổ sung theo yêu cầu*

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `GAP-VAC-233` |
| **Module** | `PostponeVaccination — vaccination bounded context` |
| **Spec gốc** | `CB-VAC-IMP-233` |
| **Priority** | 🟠 P1 *(SRS: Priority = Medium, Frequency = Regular — Table 255)* |
| **Sprint** | `S[N] (Open — chưa gán sprint cụ thể)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `PII` (health data — kế hoạch tiêm chủng của trẻ) |
| **Compliance Scope** | `PDPA` · `BR-RBAC` *(SRS Table 255 không liệt kê BR-PRIVACY — xem TDS ADR-VAC-233-006 / OPEN-5; test vẫn enforce BabyAccessPolicy như biện pháp phòng ngừa)* |
| **Upstream Dependencies** | `BabyProfileRepository`, `BabyAccessPolicy`, `VaccinationReferenceRepository`, `VaccinationRecordRepository` |
| **Downstream Consumers** | UC-228 schedule view (đọc lại `status=POSTPONED` + `scheduled_date` mới), event subscribers của `VaccinationPostponed` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-VAC-IMP-233 §17.2` (Constraint Injection Block), ADR-VAC-233-001..007 |
| **Constraints Injected** | C1 (dual-path), C2 (repeat-postpone allowed), C3 (reject COMPLETED 409), C4 (ADR-VAC-233-005 Accepted — migration `V20260703000001` must run before entity/persist logic), C5 (BabyAccessPolicy authz) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate — §5.1)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, use `V1__init_schema.sql` and approved migrations as the final persistence oracle; ERD is only supporting evidence.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS Table 255: "Updates the new expected date **and user-entered reason**" ngụ ý `reason` là first-class stored data | `vaccination_records` (V1 §660-672) **không có sẵn** cột lưu lý do postpone — schema gap đã được resolve: ADR-VAC-233-005 **Accepted 2026-07-04** (Option A) — cột `postpone_reason TEXT` sẽ được thêm qua migration `V20260703000001` | Tests validate `reason` là **input bắt buộc** (`@NotBlank` → VAC-021), có mặt trong response/event **ngay sau request**, VÀ (bắt buộc, un-gated) DB column `postpone_reason` **PHẢI** được assert persisted đúng giá trị request trong VAC233-TC-INT-001/002 và VAC233-TC-001/002/003 — không còn `[Blocked — pending ADR]` |
| L2 | Read-side (`VaccinationServiceImpl` §50-94, UC-228) hiển thị `SCHEDULED`/`OVERDUE` cho dose **chưa có row** trong `vaccination_records` (entry ảo) | Postpone một entry ảo phải **INSERT** row mới, không phải UPDATE (không có gì để update) | TC-002 (path b) test rõ ràng: `findByBabyIdAndVaccineNameAndDoseNumber` trả `Optional.empty()` trước khi gọi postpone → phải INSERT, không throw 404 |
| L3 | Không có logic transition rõ ràng nào trong spec gốc cho việc "postpone một dose đã POSTPONED" | ADR-VAC-233-003 (TDS): SRS dùng động từ "Updates" → cho phép postpone lặp lại; khác hẳn UC-232 (one-shot COMPLETED transition) | TC-003 encode rõ: postpone một record đã `POSTPONED` → **THÀNH CÔNG** (200, `created=false`), KHÔNG throw exception — test phải fail nếu implementation coi đây là lỗi/conflict |
| L4 | SRS Table 255 chỉ liệt kê BR-RBAC, không có BR-PRIVACY (khác 4 UC sibling: UC-229/230/231/232 đều có cả hai) — **đã verify trực tiếp SRS gốc** (`3_Functional_Specification.md` dòng 5023 vs dòng 5002), xác nhận đây là khác biệt thật trong văn bản, không phải lỗi transcribe của TDS/Test-Spec | Dữ liệu vẫn là PII sức khỏe về bản chất; TDS ADR-VAC-233-006 quyết định vẫn áp dụng `BabyAccessPolicy` như phòng ngừa | TC-010 (ownership-denied) vẫn được viết và PHẢI pass dù SRS không nêu tường minh BR-PRIVACY cho UC này — không nới lỏng test vì SRS thiếu dòng. Quyết định "chủ ý hay lỗi soạn thảo" vẫn Open (Category B) cho Product/Legal — xem TDS Appendix C OPEN-5 |
| L5 | Không có quy tắc nào trong SRS về việc `newScheduledDate` có được ở quá khứ hay không | TDS ADR (VAC-024, §10) coi đây là **quy tắc suy luận (Open/Proposed)**, không sourced từ SRS/AC nào | TC-008 được viết nhưng đánh dấu Oracle Source = `[Inferred — Open, KHÔNG phải BR/AC có sẵn]`; nếu Product bác bỏ quy tắc này, TC-008 cần bị loại bỏ khỏi Exit Criteria, không tự động coi là bug nếu implementation không có check này trước khi Product xác nhận |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
PostponeVaccination bao gồm các layer:
├── Domain (VaccinationRecordStatus transitions — pure logic)
├── Service (VaccinationPostponeServiceImpl — mock JPA Repository với Mockito)
├── Controller (VaccinationController.postpone — mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — Flyway thật, bao gồm migration đề xuất
    V20260703000001 — ADR-VAC-233-005 Accepted, migration bắt buộc trước khi chạy)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-233 (Table 255) | Actor Mother; "Updates the new expected date and user-entered reason"; PRE-1..4, POST-1..3, E1..E3 |
| ADR-VAC-233-002 | Dual-path update-existing/create-new |
| ADR-VAC-233-003 | Repeat-postpone allowed |
| ADR-VAC-233-004 | Reject already-COMPLETED (409) |
| ADR-VAC-233-005 | Schema gap `postpone_reason` — **Accepted** (Option A), migration `V20260703000001` firm |
| ADR-VAC-233-007 | Authorization qua `BabyAccessPolicy.isOwner` (owner-only, cập nhật từ `canView`) |
| BR-RBAC / PDPA | Access control test coverage |
| `CB-VAC-IMP-233` §8, §9, §10 | Interface / API / Error code contract |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Postpone dose có row SCHEDULED sẵn có | `VaccinationPostponeServiceImpl.postpone()` path (a) | `VAC233-TC-001` |
| TC-COND-002 | Postpone dose entry ảo (không có row) | `VaccinationPostponeServiceImpl.postpone()` path (b) | `VAC233-TC-002` |
| TC-COND-003 | Postpone dose đã POSTPONED (lặp lại) | `postpone()` path (a), re-postpone | `VAC233-TC-003` |
| TC-COND-004 | Postpone dose đã COMPLETED | `postpone()` → 409 VAC-023 | `VAC233-TC-004` |
| TC-COND-005 | Vaccine/dose không có trong catalog | `postpone()` → 404 VAC-022 | `VAC233-TC-005` |
| TC-COND-006 | Thiếu `reason` | `@Valid` → 400 VAC-021 | `VAC233-TC-006` |
| TC-COND-007 | Thiếu `newScheduledDate` | `@Valid` → 400 VAC-021 | `VAC233-TC-007` |
| TC-COND-008 | `newScheduledDate` ở quá khứ | `postpone()` → 422 VAC-024 *(Open/Inferred)* | `VAC233-TC-008` |
| TC-COND-009 | Baby không tồn tại | `postpone()` → 404 VAC-001 | `VAC233-TC-009` |
| TC-COND-010 | Caller không sở hữu/không phải ACCEPTED member | `postpone()` → 403 VAC-002 | `VAC233-TC-010` |
| TC-COND-011 | Không có JWT | Spring Security filter → 401 | `VAC233-TC-011` |
| TC-COND-012 | IDOR — caller M2 cố truy cập baby của M1 qua babyId đoán được | `BabyAccessPolicy.isOwner` → 403 | `VAC233-TC-012` |
| TC-COND-013 | End-to-end persist path (b) | Testcontainers PostgreSQL | `VAC233-TC-INT-001` |
| TC-COND-014 | End-to-end re-postpone không tạo row trùng (INV-1) | Testcontainers PostgreSQL | `VAC233-TC-INT-002` |
| TC-COND-015 | Full API happy path (JWT thật) | `POST /postponements` | `VAC233-TC-E2E-001` |
| TC-COND-016 | Full API ownership denied | `POST /postponements` | `VAC233-TC-E2E-002` |
| TC-COND-017 | Full API already-completed rejected | `POST /postponements` | `VAC233-TC-E2E-003` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | Request field validity (blank/non-blank reason, null/non-null date) | Phân vùng input hợp lệ/không hợp lệ cho `@Valid` |
| Boundary Value Analysis | `newScheduledDate` = today vs today-1 (VAC-024 boundary) | Kiểm tra biên "quá khứ vs hiện tại" |
| State Transition Testing | `VaccinationRecordStatus` SCHEDULED/POSTPONED/COMPLETED transitions (§6.4 TDS) | Bao phủ mọi transition hợp lệ và bị chặn |
| Error Guessing | IDOR (babyId đoán được), race-condition duplicate insert | Security/edge-case coverage |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | BabyProfile `B1` — `ownerUserId=M1` | Baseline owner |
| `FX-002` | DB seed | `VaccinationRecord{babyId=B1, vaccineName="DTP-VGB-Hib", doseNumber=1, status=SCHEDULED, scheduledDate=2026-08-01}` | Path (a) update — TC-001 |
| `FX-003` | DB seed | `VaccinationRecord{babyId=B1, vaccineName="BCG", doseNumber=1, status=POSTPONED, scheduledDate=2026-07-10}` | Repeat-postpone — TC-003 |
| `FX-004` | DB seed | `VaccinationRecord{babyId=B1, vaccineName="Viem gan B", doseNumber=1, status=COMPLETED, administeredDate=2026-06-01}` | Already-completed reject — TC-004 |
| `FX-005` | DB seed (reference) | `vaccination_reference_schedules`: BCG dose1 offset0; DTP-VGB-Hib dose1 offset60 (theo `V20260627100500`) | Catalog validation |
| `FX-006` | JWT | `{ sub: 'M1', role: 'MOTHER' }` — owner của B1 | Happy path auth |
| `FX-007` | JWT | `{ sub: 'M2', role: 'MOTHER' }` — KHÔNG sở hữu/không phải ACCEPTED member của B1 | Ownership-denied — TC-010/012 |
| `FX-008` | JWT | `{ sub: 'M3', role: 'FAMILY' }` — ACCEPTED care-group member của B1 | Access-allowed-for-member — biến thể TC-001 |
| `FX-009` | env | Không có Authorization header | Unauthenticated — TC-011 |
| `FX-010` | request payload | `{vaccineName:"UNKNOWN_VAX", doseNumber:9, newScheduledDate:"2026-08-01", reason:"test"}` | Catalog-miss — TC-005 |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng factory method riêng
// ═══════════════════════════════════════════════════════════

// VaccinationPostponeTestFactory.java
class VaccinationPostponeTestFactory {

    static final UUID BABY_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID OWNER_ID  = UUID.fromString("00000000-0000-0000-0000-0000000000M1");
    static final UUID NON_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000M2");

    // FX-001 — baby profile owned by OWNER_ID
    static BabyProfile makeBaby() {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OWNER_ID)
                .nickname("Test Baby")
                .birthDate(LocalDate.of(2026, 1, 1))
                .build();
    }

    static BabyProfile makeBaby(Consumer<BabyProfile.BabyProfileBuilder> overrides) {
        var b = BabyProfile.builder().id(BABY_ID).ownerUserId(OWNER_ID)
                .nickname("Test Baby").birthDate(LocalDate.of(2026, 1, 1));
        overrides.accept(b);
        return b.build();
    }

    // FX-002 — existing SCHEDULED row (path a target)
    static VaccinationRecord makeScheduledRecord() {
        return VaccinationRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_ID)
                .vaccineName("DTP-VGB-Hib")
                .doseNumber((short) 1)
                .scheduledDate(LocalDate.of(2026, 8, 1))
                .status(VaccinationRecordStatus.SCHEDULED)
                .build();
    }

    // FX-003 — existing POSTPONED row (repeat-postpone target)
    static VaccinationRecord makePostponedRecord() {
        return VaccinationRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_ID)
                .vaccineName("BCG")
                .doseNumber((short) 1)
                .scheduledDate(LocalDate.of(2026, 7, 10))
                .status(VaccinationRecordStatus.POSTPONED)
                .build();
    }

    // FX-004 — existing COMPLETED row (rejection target)
    static VaccinationRecord makeCompletedRecord() {
        return VaccinationRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_ID)
                .vaccineName("Viem gan B")
                .doseNumber((short) 1)
                .administeredDate(LocalDate.of(2026, 6, 1))
                .status(VaccinationRecordStatus.COMPLETED)
                .build();
    }

    // FX-005 — reference catalog entries
    static VaccinationReferenceSchedule makeReference(String vaccineName, int doseNumber, int offsetDays) {
        return VaccinationReferenceSchedule.builder()
                .id(UUID.randomUUID())
                .vaccineName(vaccineName)
                .doseNumber((short) doseNumber)
                .offsetDays(offsetDays)
                .build();
    }

    // Valid baseline request — override cụ thể per test
    static PostponeVaccinationRequest makeRequest() {
        var req = new PostponeVaccinationRequest();
        req.setVaccineName("DTP-VGB-Hib");
        req.setDoseNumber((short) 1);
        req.setNewScheduledDate(LocalDate.now().plusDays(30));
        req.setReason("Be bi sot nhe, bac si khuyen hoan tiem");
        return req;
    }

    static PostponeVaccinationRequest makeRequest(Consumer<PostponeVaccinationRequest> overrides) {
        var req = makeRequest();
        overrides.accept(req);
        return req;
    }
}
```

---

### VAC233-TC-001 — Path (a): Update existing SCHEDULED row → POSTPONED

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationPostponeServiceImpl.postpone()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationPostponeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-VAC-233-002 §Quyết định` (CB-VAC-IMP-233 §3) — dual-path update branch

**Preconditions:**
- FX-001 (baby B1 owned by OWNER_ID)
- FX-002 (VaccinationRecord SCHEDULED tồn tại cho DTP-VGB-Hib dose1)
- FX-005 seed reference cho DTP-VGB-Hib dose1

**Test Steps:**
1. Arrange: mock `recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(B1, "DTP-VGB-Hib", 1)` → trả về FX-002
2. Act: gọi `postpone(B1, makeRequest(), OWNER_ID)`
3. Assert: record status = POSTPONED, `scheduled_date` = request.newScheduledDate, **`postponeReason` = request.reason** (ADR-VAC-233-005 Accepted — persisted field, captured qua `ArgumentCaptor<VaccinationRecord>` trên `save()`), `save()` được gọi đúng 1 lần, không có `insert` mới

**Expected Result (PASS):**
- Response `created=false`, `status="POSTPONED"`, `previousScheduledDate=2026-08-01` (giá trị cũ), `newScheduledDate` = request value, `reason` = request value
- Captured entity argument to `save()` has `getPostponeReason()` equal to `makeRequest().getReason()` — **assertion bắt buộc, không skip**

**Expected Result (FAIL):**
- Nếu implementation INSERT thay vì UPDATE → sai path, record trùng
- Nếu `postponeReason` trên entity đã lưu là `null`/khác request value → vi phạm ADR-VAC-233-005 (Accepted)

**Current Status:** 🟢 Passing
**Implementation Note:** dùng đúng lookup method `findByBabyIdAndVaccineNameAndDoseNumber` (bỏ filter status) theo ADR-VAC-233-002; `rec.setPostponeReason(req.getReason())` PHẢI được gọi trước `save()` (ADR-VAC-233-005 Accepted, Option A).

---

### VAC233-TC-002 — Path (b): Create new POSTPONED row (virtual entry)

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationPostponeServiceImpl.postpone()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationPostponeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-VAC-233-002` + read-side merge logic `VaccinationServiceImpl` §54-85 (CB-VAC-IMP-233 §3, ADR-VAC-233-001)

**Preconditions:**
- FX-001 (baby B1)
- **KHÔNG** có VaccinationRecord cho `("BCG", 1)` — entry ảo
- FX-005 seed reference cho BCG dose1

**Test Steps:**
1. Arrange: mock `findByBabyIdAndVaccineNameAndDoseNumber(B1, "BCG", 1)` → `Optional.empty()`
2. Act: gọi `postpone(B1, makeRequest(r -> {r.setVaccineName("BCG"); r.setDoseNumber((short)1);}), OWNER_ID)`
3. Assert: `save()` gọi với record mới có `status=POSTPONED`, `babyId=B1`, **`postponeReason` = request.reason** (ADR-VAC-233-005 Accepted — captured qua `ArgumentCaptor<VaccinationRecord>`)

**Expected Result (PASS):**
- Response `created=true`, `status="POSTPONED"`, `previousScheduledDate=null` (không có giá trị cũ vì chưa từng có row), `reason` = request value
- Captured entity argument to `save()` has `getPostponeReason()` equal to request's `reason` — **assertion bắt buộc, không skip**

**Expected Result (FAIL):**
- Nếu implementation throw 404 vì không tìm thấy record hiện có → sai — vì entry ảo hợp lệ theo L2 (§2)
- Nếu new record được `save()` với `postponeReason == null` dù request có `reason` → vi phạm ADR-VAC-233-005 (Accepted)

**Current Status:** 🟢 Passing
**Implementation Note:** không được throw VAC-001/404 khi record rỗng — chỉ VAC-001 áp dụng khi **baby** không tồn tại, không phải khi record không tồn tại; builder mới PHẢI set `.postponeReason(req.getReason())` (ADR-VAC-233-005 Accepted, Option A).

---

### VAC233-TC-003 — Repeat-postpone: POSTPONED → POSTPONED (lặp lại được phép)

**Severity:** `HIGH`
**Feature Under Test:** `VaccinationPostponeServiceImpl.postpone()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationPostponeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-VAC-233-003 §Quyết định` (CB-VAC-IMP-233 §3) — "SRS dùng động từ Updates → cho phép postpone lặp lại"

**Preconditions:**
- FX-001, FX-003 (VaccinationRecord đã `POSTPONED`, `scheduledDate=2026-07-10`)

**Test Steps:**
1. Arrange: mock lookup trả FX-003
2. Act: gọi `postpone(B1, makeRequest(r -> {r.setVaccineName("BCG"); r.setNewScheduledDate(LocalDate.of(2026, 9, 1)); r.setReason("Van chua sap xep duoc");}), OWNER_ID)`
3. Assert: **KHÔNG throw exception**; record được UPDATE với `scheduledDate=2026-09-01`, status vẫn `POSTPONED`, **`postponeReason` cập nhật thành giá trị mới nhất `"Van chua sap xep duoc"`** (ghi đè giá trị cũ, không giữ lịch sử — theo OPEN-3)

**Expected Result (PASS):**
- Response 200, `created=false`, `newScheduledDate=2026-09-01`, `previousScheduledDate=2026-07-10`, `reason="Van chua sap xep duoc"`
- Captured entity argument to `save()` has `getPostponeReason()` equal to the new request's `reason` (not the old one) — **assertion bắt buộc, không skip**

**Expected Result (FAIL):**
- Nếu implementation throw 409 hoặc bất kỳ lỗi nào khi status hiện tại đã là POSTPONED → **vi phạm ADR-VAC-233-003**, đây là anti-pattern cần reject (xem §8 AP-AI-003)
- Nếu `postponeReason` không được cập nhật (giữ giá trị cũ) khi re-postpone → vi phạm ADR-VAC-233-005 (Accepted) kết hợp ADR-VAC-233-003

**Current Status:** 🟢 Passing
**Implementation Note:** điều kiện reject DUY NHẤT là `status == COMPLETED` (VAC-023); KHÔNG được reject khi `status == POSTPONED`; `postponeReason` PHẢI được ghi đè mỗi lần re-postpone (ADR-VAC-233-005 Accepted).

---

### VAC233-TC-004 — Already-completed dose bị từ chối (409 VAC-023)

**Severity:** `CRITICAL`
**CWE:** `CWE-841 — Improper Enforcement of Behavioral Workflow`
**Legal:** `Data integrity — không cho phép "dời lịch" một sự kiện y tế đã xảy ra`
**Feature Under Test:** `VaccinationPostponeServiceImpl.postpone()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationPostponeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-VAC-233-004` (CB-VAC-IMP-233 §3, §10 — VAC-023/409)

**Preconditions:**
- FX-001, FX-004 (VaccinationRecord `status=COMPLETED`)

**Test Steps:**
1. Arrange: mock lookup trả FX-004
2. Act: gọi `postpone(B1, makeRequest(r -> r.setVaccineName("Viem gan B")), OWNER_ID)`
3. Assert: `BusinessException` với `code=VAC-023`, `httpStatus=409`; `save()` **không** được gọi

**Expected Result (PASS — hành vi đúng):**
- Exception `VAC-023` ném ra trước khi có bất kỳ ghi DB nào

**Expected Result (FAIL — dấu hiệu lỗi):**
- Record bị ghi đè `status=POSTPONED` lên một dose đã tiêm thật — vi phạm INV-2 (TDS §6.4)

**Current Status:** 🟢 Passing
**Implementation Note:** kiểm tra `status == COMPLETED` PHẢI xảy ra trước khi gọi `save()`.

---

### VAC233-TC-005 — Vaccine/dose không có trong reference catalog (404 VAC-022)

**Severity:** `MEDIUM`
**Feature Under Test:** `VaccinationPostponeServiceImpl.postpone()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationPostponeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-VAC-IMP-233 §10` (VAC-022/404), FX-010

**Preconditions:**
- FX-001; reference catalog KHÔNG chứa `("UNKNOWN_VAX", 9)`

**Test Steps:**
1. Arrange: mock `referenceRepository` catalog lookup → absent cho `UNKNOWN_VAX`/dose 9
2. Act: gọi `postpone(B1, makeRequest(r -> {r.setVaccineName("UNKNOWN_VAX"); r.setDoseNumber((short)9);}), OWNER_ID)`
3. Assert: `BusinessException(404, VAC-022)`

**Expected Result (PASS):** exception đúng code, không truy vấn `recordRepository` (fail-fast trước dual-path lookup)
**Expected Result (FAIL):** hệ thống vẫn tạo record cho vaccine không có trong danh mục chuẩn MoH VN

**Current Status:** 🟢 Passing
**Implementation Note:** validate catalog TRƯỚC lookup record (thứ tự trong Chặng 4, TDS §11.3).

---

### VAC233-TC-006 — Thiếu `reason` → 400 VAC-021 (SRS bắt buộc "user-entered reason")

**Severity:** `CRITICAL`
**CWE:** `CWE-20 — Improper Input Validation`
**Legal:** `SRS Table 255 — "user-entered reason" là input bắt buộc, không phải optional`
**Feature Under Test:** `PostponeVaccinationRequest` `@Valid` constraint / `VaccinationController.postpone()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerPostponeTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** SRS Table 255 ("... and user-entered reason") + `CB-VAC-IMP-233 §8.1, §10` (VAC-021/400)

**Preconditions:** none (unit-level validation test)

**Test Steps:**
1. Arrange: `makeRequest(r -> r.setReason(""))` hoặc `r.setReason(null)`
2. Act: `@WebMvcTest` POST `/api/v1/vaccination/babies/{B1}/postponements` với body thiếu `reason`
3. Assert: response 400, `error.code=VAC-021`, `details` chứa field `reason`

**Expected Result (PASS):** 400 VAC-021 với message field-level cho `reason`
**Expected Result (FAIL):** request được chấp nhận dù thiếu lý do — vi phạm trực tiếp SRS description

**Current Status:** 🔴 Not written
**Implementation Note:** `@NotBlank` trên `PostponeVaccinationRequest.reason` (§8.1 TDS).

---

### VAC233-TC-007 — Thiếu `newScheduledDate` → 400 VAC-021

**Severity:** `HIGH`
**Feature Under Test:** `PostponeVaccinationRequest` `@Valid`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerPostponeTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-VAC-IMP-233 §8.1, §10` (VAC-021/400)

**Preconditions:** none

**Test Steps:**
1. Arrange: `makeRequest(r -> r.setNewScheduledDate(null))`
2. Act: POST `/postponements` với `newScheduledDate=null`
3. Assert: 400 VAC-021, `details` chứa field `newScheduledDate`

**Expected Result (PASS):** 400 với field-level message
**Expected Result (FAIL):** service nhận null date, NPE hoặc lưu `scheduled_date=null` (mất mục đích của "postpone")

**Current Status:** 🔴 Not written
**Implementation Note:** `@NotNull` trên `newScheduledDate` (§8.1).

---

### VAC233-TC-008 — `newScheduledDate` ở quá khứ → 422 VAC-024 *(Open/Inferred rule)*

**Severity:** `LOW` *(hạ mức vì rule chưa được Product xác nhận — xem L5 §2, OPEN-8 TDS)*
**Feature Under Test:** `VaccinationPostponeServiceImpl.postpone()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationPostponeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-VAC-IMP-233 §10` VAC-024 — **`[Inferred/Open — KHÔNG sourced từ SRS/AC]`**

**Preconditions:** FX-001, FX-002

**Test Steps:**
1. Arrange: `makeRequest(r -> r.setNewScheduledDate(LocalDate.now().minusDays(1)))`
2. Act: gọi `postpone(B1, req, OWNER_ID)`
3. Assert: `BusinessException(422, VAC-024)`

**Expected Result (PASS — NẾU rule được Product Accepted):** exception 422 VAC-024
**Expected Result (FAIL / N/A nếu rule bị Product bác bỏ):** không coi implementation thiếu check này là bug cho tới khi OPEN-8 (TDS §Phụ lục C) được giải quyết — **test này KHÔNG nằm trong Exit Criteria bắt buộc (§6)** cho tới khi ADR liên quan được Accepted

**Current Status:** 🟢 Passing
**Implementation Note:** implement CHỈ nếu Tech Lead/Product xác nhận VAC-024 là yêu cầu thật; nếu không, xóa test này khỏi Red Gate bắt buộc.

---

### VAC233-TC-009 — Baby không tồn tại → 404 VAC-001 (reused)

**Severity:** `MEDIUM`
**Feature Under Test:** `VaccinationPostponeServiceImpl.postpone()`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationPostponeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-VAC-IMP-233 §10` VAC-001 (reused từ UC-228/232, `VaccinationServiceImpl.java` dòng 39-41)

**Preconditions:** `babyRepository.findById(UNKNOWN_BABY_ID)` → empty

**Test Steps:**
1. Act: gọi `postpone(UNKNOWN_BABY_ID, makeRequest(), OWNER_ID)`
2. Assert: `BusinessException(404, VAC-001)`

**Expected Result (PASS):** exception đúng code, message chứa babyId
**Expected Result (FAIL):** NPE hoặc lỗi 500 thay vì 404 có kiểm soát

**Current Status:** 🟢 Passing
**Implementation Note:** tái sử dụng chính xác pattern từ `VaccinationServiceImpl.getVaccinationSchedule` dòng 39-41.

---

### VAC233-TC-010 — Ownership denied → 403 VAC-002 (reused)

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC (SRS Table 255), PDPA minimum-necessary`
**Feature Under Test:** `VaccinationPostponeServiceImpl.postpone()` + `BabyAccessPolicy.isOwner()` (cập nhật từ `canView()`)
**Test File:** `src/test/java/com/carebridge/backend/vaccination/service/VaccinationPostponeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `BabyAccessPolicy.java` dòng 22-28 + `CB-VAC-IMP-233 §10, §16` (VAC-002/403, ADR-VAC-233-007)

**Preconditions:** FX-001 (baby owned by M1); caller = FX-007 (M2, không phải owner/không phải ACCEPTED member)

**Test Steps:**
1. Arrange: mock `accessPolicy.isOwner(baby, M2)` → `false`
2. Act: gọi `postpone(B1, makeRequest(), NON_OWNER_ID)`
3. Assert: `BusinessException(403, VAC-002)`; `recordRepository` **không** được truy vấn (fail-fast sau authz check)

**Expected Result (PASS):** 403 VAC-002 ngay sau baby lookup, trước khi chạm vào vaccination_records
**Expected Result (FAIL):** M2 có thể dời lịch tiêm của con người khác — vi phạm nghiêm trọng BR-RBAC/PDPA

**Current Status:** 🟢 Passing
**Implementation Note:** authz check PHẢI xảy ra trước reference-catalog và record lookup (thứ tự Chặng 4, TDS §11.3).

---

### SECURITY TEST CASES

---

### VAC233-TC-011 — Unauthenticated access → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Legal:** `BR-RBAC, PDPA`
**Feature Under Test:** `VaccinationController.postpone()` — `@PreAuthorize("isAuthenticated()")`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerPostponeSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** FX-009 (không có Authorization header)

**Test Steps (Attack Simulation):**
1. Chuẩn bị request POST `/api/v1/vaccination/babies/{B1}/postponements` không kèm JWT
2. Gửi request
3. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):** `401 Unauthorized`, không lộ thông tin baby/record nào
**Expected Result (FAIL = lỗ hổng tồn tại):** request được xử lý mà không xác thực

**Current Status:** 🔴 Not written

---

### VAC233-TC-012 — IDOR: caller đoán babyId của người khác

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Legal:** `BR-RBAC, PDPA minimum-necessary`
**Feature Under Test:** `VaccinationPostponeServiceImpl.postpone()` — path param `babyId` do client kiểm soát
**Test File:** `src/test/java/com/carebridge/backend/vaccination/controller/VaccinationControllerPostponeSecurityTest.java`
**TDD Phase:** 🔴 RED

**Preconditions:** JWT hợp lệ của M2 (FX-007); babyId = B1 (thuộc M1, không phải M2) — đoán được vì UUID không phải bí mật duy nhất bảo vệ

**Test Steps (Attack Simulation):**
1. M2 đăng nhập hợp lệ (JWT thật, không giả mạo)
2. M2 gọi `POST /api/v1/vaccination/babies/{B1}/postponements` với `B1` không thuộc về M2
3. Kiểm tra response

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden` với `VAC-002`, KHÔNG có thay đổi nào trên `vaccination_records` của B1
**Expected Result (FAIL = lỗ hổng tồn tại):** M2 dời lịch tiêm thành công cho con của M1 — điển hình lỗ hổng IDOR trên dữ liệu sức khỏe trẻ em

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer`). Timeout: 120s.

---

### VAC233-TC-INT-001 — POST postponements persists POSTPONED (path b, end-to-end)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: POST → Service → Repository → PostgreSQL thật`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/PostponeVaccinationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migrations applied tự động khi Spring context start (**bao gồm `V20260703000001` — ADR-VAC-233-005 Accepted, migration bắt buộc, xem CG-9 §9**)
- Seed: baby B1 (FX-001), reference BCG dose1 (FX-005); KHÔNG có record cho `(B1, BCG, 1)`

**Test Steps:**
1. Seed minimal data qua JPA
2. Gọi `POST /api/v1/vaccination/babies/{B1}/postponements` với `{vaccineName:"BCG", doseNumber:1, newScheduledDate:"2026-08-01", reason:"test"}`
3. Assert response 201
4. Query lại `vaccination_records` bằng `JdbcTemplate`/`recordRepository`, bao gồm cột `postpone_reason`

**Expected Result (PASS):**
- Response 201, `data.created=true`, `data.status="POSTPONED"`, `data.reason="test"`
- DB: 1 row mới với `status='POSTPONED'`, `scheduled_date='2026-08-01'`, **`postpone_reason='test'`** (ADR-VAC-233-005 Accepted — assertion bắt buộc, không còn conditional)

**Expected Result (FAIL):** không có row nào được tạo, tạo với `status` sai, hoặc `postpone_reason` là `NULL`/khác `'test'`

**DB Assertion:**
```java
VaccinationRecord record = recordRepository
        .findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1)
        .orElseThrow();
assertThat(record.getStatus()).isEqualTo(VaccinationRecordStatus.POSTPONED);
assertThat(record.getScheduledDate()).isEqualTo(LocalDate.of(2026, 8, 1));
assertThat(record.getPostponeReason()).isEqualTo("test"); // ADR-VAC-233-005 Accepted — bắt buộc

// Optional raw-column cross-check (defense-in-depth against JPA mapping bugs):
String reasonColumn = jdbcTemplate.queryForObject(
    "SELECT postpone_reason FROM vaccination_records WHERE vaccination_record_id = ?",
    String.class, record.getId());
assertThat(reasonColumn).isEqualTo("test");
```

**Current Status:** 🔴 Not written

---

### VAC233-TC-INT-002 — Re-postpone không tạo row trùng (INV-1)

**Severity:** `HIGH`
**Feature Under Test:** `Full flow re-postpone: POSTPONED → POSTPONED (ngày mới)`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/PostponeVaccinationIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-014`

**Preconditions:**
- FX-001, FX-003 seeded thật trong Testcontainers (1 row POSTPONED có sẵn cho BCG dose1)

**Test Steps:**
1. Gọi `POST /postponements` lần 2 với `newScheduledDate` khác cho cùng `(BCG, 1)`
2. Assert response 200 (không phải 201)
3. Query `COUNT(*)` cho key `(baby_id, vaccine_name, dose_number)`

**Expected Result (PASS):**
- Response 200, `data.created=false`
- `SELECT COUNT(*) FROM vaccination_records WHERE baby_id=B1 AND vaccine_name='BCG' AND dose_number=1` = **1** (không phải 2)

**Expected Result (FAIL):** COUNT = 2 → vi phạm INV-1, bug nghiêm trọng (duplicate record)

**DB Assertion:**
```java
Integer count = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM vaccination_records WHERE baby_id=? AND vaccine_name=? AND dose_number=?",
    Integer.class, BABY_ID, "BCG", 1);
assertThat(count).isEqualTo(1);
```

**Current Status:** 🔴 Not written

---

### E2E TEST CASES

---

### VAC233-TC-E2E-001 — Full happy path qua API thật (JWT Mother)

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/vaccination/babies/{babyId}/postponements`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/PostponeVaccinationE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-015`

**Test Steps:**
```gherkin
Scenario: Mother postpones a scheduled dose
  Given test data classification: SYNTHETIC
  And Mother "mother@carebridge.dev" đã đăng nhập, có JWT hợp lệ, sở hữu baby B1
  And baby B1 has a SCHEDULED dose (DTP-VGB-Hib, dose 1)
  When POST /api/v1/vaccination/babies/{B1}/postponements được gọi với:
    | Header          | Value              |
    | Authorization   | Bearer [token]     |
    | Content-Type    | application/json   |
  And body = {"vaccineName":"DTP-VGB-Hib","doseNumber":1,"newScheduledDate":"2026-09-01","reason":"Ban ron cong tac"}
  Then response status là 200
  And response body data.status = "POSTPONED"
  And response body data.newScheduledDate = "2026-09-01"
```

**Current Status:** 🔴 Not written

---

### VAC233-TC-E2E-002 — Ownership denied qua API thật

**Severity:** `CRITICAL`
**Feature Under Test:** `POST /api/v1/vaccination/babies/{babyId}/postponements`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/PostponeVaccinationE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-016`

```gherkin
Scenario: Non-member Mother denied access
  Given test data classification: SYNTHETIC
  And Mother "M2" đã đăng nhập nhưng KHÔNG sở hữu baby B1 và không phải ACCEPTED member
  When POST /api/v1/vaccination/babies/{B1}/postponements được gọi bởi M2
  Then response status là 403
  And response body error.code = "VAC-002"
```

**Current Status:** 🔴 Not written

---

### VAC233-TC-E2E-003 — Already-completed rejected qua API thật

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/vaccination/babies/{babyId}/postponements`
**Test File:** `src/test/java/com/carebridge/backend/vaccination/PostponeVaccinationE2ETest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-017`

```gherkin
Scenario: Reject postponing a completed dose
  Given test data classification: SYNTHETIC
  And baby B1 has a COMPLETED dose (Viem gan B, dose 1)
  When POST /api/v1/vaccination/babies/{B1}/postponements được gọi cho dose đó
  Then response status là 409
  And response body error.code = "VAC-023"
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `VAC233-TC-001` | `VaccinationPostponeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-002` | `VaccinationPostponeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-003` | `VaccinationPostponeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-004` | `VaccinationPostponeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-005` | `VaccinationPostponeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-006` | `VaccinationControllerPostponeTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-007` | `VaccinationControllerPostponeTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-008` | `VaccinationPostponeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | `[CONDITIONAL — chờ Product xác nhận VAC-024]` |
| `VAC233-TC-009` | `VaccinationPostponeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-010` | `VaccinationPostponeServiceImplTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-011` | `VaccinationControllerPostponeSecurityTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-012` | `VaccinationControllerPostponeSecurityTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-INT-001` | `PostponeVaccinationIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-INT-002` | `PostponeVaccinationIntegrationTest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-E2E-001` | `PostponeVaccinationE2ETest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-E2E-002` | `PostponeVaccinationE2ETest.java:TBD` | `[ ]` | `[ ]` | — |
| `VAC233-TC-E2E-003` | `PostponeVaccinationE2ETest.java:TBD` | `[ ]` | `[ ]` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL (trừ TC-008 nếu bị loại khỏi scope — xem điều kiện §6). Nếu test PASS ngay → **AP-AI-002 detected**.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class VaccinationPostponeServiceImpl implements IVaccinationPostponeService {

    @Override
    public PostponeVaccinationResponse postpone(UUID babyId,
                                                 PostponeVaccinationRequest request,
                                                 UUID callerId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `VAC233-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `VAC233-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-006` | `@Valid` fail độc lập với service stub | 🔴 FAIL (400 not returned, or 500 due to stub) | ☐ FAIL ☐ PASS | |
| `VAC233-TC-007` | tương tự TC-006 | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-008` | `throw('Not implemented')` | 🔴 FAIL *(conditional)* | ☐ FAIL ☐ PASS | |
| `VAC233-TC-009` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-010` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-011` | Controller chưa map endpoint → 404/405 thay vì 401 | 🔴 FAIL (sai status code cho tới khi có endpoint) | ☐ FAIL ☐ PASS | |
| `VAC233-TC-012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-INT-001` | 500 (stub throw) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-INT-002` | 500 (stub throw) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-E2E-001` | 500 (stub throw) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-E2E-002` | 500 (stub throw, chưa có authz check thật) | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `VAC233-TC-E2E-003` | 500 (stub throw) | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `N/A - Red Gate not reconstructed because production implementation already existed before this execution.`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]` *(chưa có — Draft phase)*

> **Nếu bất kỳ test PASS bất thường:** Dừng lại, xác định root cause, rewrite theo Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-VAC-IMP-233` đã được review và approve
- [ ] Logic Issues (§2) đã được confirm với Tech Lead/Principal Architect
- [x] ~~**ADR-VAC-233-005 đã được quyết định Option A hay B** (blocking — xem OPEN-9 TDS)~~ **RESOLVED — Accepted 2026-07-04, Option A.** Trước khi chạy integration/E2E tests: `V20260703000001__add_vaccination_postpone_reason.sql` phải được tạo và chạy thành công trên staging (không còn là quyết định mở, chỉ còn là bước thực thi)
- [x] ~~OPEN-1 (coordination method dùng chung với UC-232) đã được Tech Lead phân xử thứ tự implement~~ **RESOLVED** — UC-232 là canonical owner của `findByBabyIdAndVaccineNameAndDoseNumber` (xem TDS ADR-VAC-233-002)
- [ ] Test fixtures (§3 TDS-05) đã được chuẩn bị
- [ ] VAC-024 (TC-008) đã được Product xác nhận có cần implement hay không (nếu không, loại khỏi Red Gate bắt buộc) — *(lưu ý: đây là ADR/OPEN-8 khác, KHÔNG liên quan ADR-VAC-233-005, vẫn còn Open độc lập)*

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không skip trừ TC-008 nếu bị loại theo Entry Criteria)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `VaccinationPostponeServiceImpl`
- [ ] Không có business logic trong Controller (chỉ validation + mapping + status code)
- [ ] Không có PII/secret plaintext trong logs
- [ ] Không sinh record trùng `(baby_id, vaccine_name, dose_number)` (VAC233-TC-INT-002 pass)
- [ ] Migration `V20260703000001` (ADR-VAC-233-005 Accepted, bắt buộc) áp dụng thành công, có trong `flyway_schema_history`
- [ ] `postpone_reason` persistence được assert PASS trong VAC233-TC-001/002/003 (unit, qua ArgumentCaptor) và VAC233-TC-INT-001 (integration, qua DB thật) — không skip

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence:**
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (mọi instance qua `VaccinationPostponeTestFactory`)
- [ ] **Oracle Source** — mọi expected value có ghi rõ nguồn (BR/AC/ADR); TC-008 rõ ràng đánh dấu `[Inferred/Open]`

### Suspension Criteria (Điều kiện tạm dừng)

- ~~ADR-VAC-233-005 chưa được quyết định (Option A/B) — blocker chính~~ **RESOLVED — Accepted 2026-07-04 (Option A)**, không còn blocker
- ~~Xung đột merge với UC-232 trên `VaccinationRecordRepository.java` (OPEN-1) chưa giải quyết~~ **RESOLVED** — UC-232 là canonical owner, thêm method một lần
- Phát hiện lỗi kiến trúc mới cần Principal Architect review

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — ADR-VAC-233-005 Accepted, Option A; áp dụng SAU KHI migration đã chạy)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE public.vaccination_records DROP COLUMN IF EXISTS postpone_reason;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260703000001';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/vaccination/
git checkout -- src/main/resources/db/migration/V20260703000001__add_vaccination_postpone_reason.sql
git checkout -- src/test/java/com/carebridge/backend/vaccination/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mọi TC có Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ Chờ verify khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test giả định `postpone` là one-shot (giống UC-232) thay vì repeatable — **đã tránh** bằng TC-003 tường minh test repeat-allowed | ☑ Không phát hiện | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic (vd tự tính dual-path trong controller) | ☑ Không phát hiện — mọi logic thuộc Service | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong TDS §8 (vd `canManage`), hoặc assert cột `postpone_reason` TRƯỚC KHI migration `V20260703000001` thực sự chạy trên môi trường test | ☑ TC-INT-001 nay assert `postpone_reason` **firm** (ADR-VAC-233-005 Accepted) nhưng chỉ SAU KHI Flyway migration chạy trong `@Testcontainers` setup — không hallucinate cột chưa tồn tại | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào ở mức đặc tả (spec-level) → TDD spec approved cho giai đoạn Draft
- [ ] Review lại sau khi code thật được viết (runtime AP-AI-002 chỉ verify được sau Red Gate thật)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | Không có AP nào phát hiện ở giai đoạn spec | — | N/A |

---

## 9. Consistency Gate (CG-1..CG-9) — TDS ↔ Test-Spec

> ⭐ Bổ sung theo yêu cầu review — đối chiếu `UC233_PostponeVaccination_TDS.md` (CB-VAC-IMP-233) với Test-Spec này.

| Gate | Mô tả kiểm tra | Kết quả | Ghi chú |
|------|----------------|---------|---------|
| **CG-1** | Mọi error code TDS §10 (`VAC-001, VAC-002, VAC-021..024`) có ≥ 1 test case | ✅ PASS | VAC-001→TC-009, VAC-002→TC-010/012/E2E-002, VAC-021→TC-006/TC-007, VAC-022→TC-005, VAC-023→TC-004/E2E-003, VAC-024→TC-008 (conditional — OPEN-8, riêng biệt với ADR-VAC-233-005 đã Resolved) |
| **CG-2** | Endpoint TDS §9.1 (`POST .../postponements`) có ≥ 1 E2E test | ✅ PASS | TC-E2E-001/002/003 |
| **CG-3** | Mọi ADR (§3 TDS) có entry tương ứng trong Logic Issues Resolved (§2) | ✅ PASS | ADR-233-001→(nền context, không cần TC riêng); 002→L2; 003→L3; 004→(L trong §2 ngầm qua TC-004, bổ sung); 005→L1; 006→L4; 007→(authz, covered qua L4/TC-010) |
| **CG-4** | Mọi field có validation constraint (§8.1 DTO) có test biên/tương đương | ✅ PASS | `reason`→TC-006, `newScheduledDate`→TC-007/TC-008, `vaccineName`+`doseNumber`→TC-005 |
| **CG-5** | Mọi transition trong State Machine (TDS §6.4) có ≥1 test (happy + rejected) | ✅ PASS | VIRTUAL→POSTPONED: TC-002; SCHEDULED→POSTPONED: TC-001; POSTPONED→POSTPONED: TC-003; COMPLETED (rejected): TC-004 |
| **CG-6** | Authorization Matrix (TDS §16) phản ánh đúng trong test ownership-denied | ✅ PASS | TC-010 (service-level), TC-012 (IDOR/API-level), TC-011 (401) |
| **CG-7** | Mọi field trong Domain Event payload (TDS §7.3) được assert ở ≥1 integration test | ⚠️ PARTIAL | TC-INT-001/002 assert DB state nhưng **không** assert trực tiếp payload event `VaccinationPostponed` (vd qua `ApplicationListener` test hoặc log). Ghi nhận: cần bổ sung 1 test lắng nghe event khi implement (không blocking cho Draft, nhưng flag cho Test-Spec v1.1) |
| **CG-8** | Mọi Oracle Source trace về file/dòng thật, không invented | ✅ PASS | Tất cả TC trích dẫn `CB-VAC-IMP-233 §X`, `BabyAccessPolicy.java` dòng cụ thể, hoặc SRS Table 255; TC-008 minh bạch đánh dấu `[Inferred/Open]` thay vì giả vờ có nguồn |
| **CG-9** | Schema delta (nếu có) đồng bộ với migration + baseline doc convention | ✅ PASS | TDS §5.2 xác nhận `V20260703000001__add_vaccination_postpone_reason.sql` (**ADR-VAC-233-005 Accepted 2026-07-04, Option A**; version re-verified 2026-07-04 bằng cách liệt kê lại toàn bộ 33 file migration thật, vẫn không trùng `V20260702002000` mới nhất); Entry Criteria (§6 Test-Spec) không còn chặn ở mức quyết định — chỉ còn chờ migration này chạy thành công trước integration test; `V1_SCHEMA_BASELINE.md` được xác nhận **không cần sửa** vì tài liệu đó chỉ track bảng (table_count), không track cột — kết luận ghi rõ trong TDS ADR-VAC-233-005 |

**Kết luận CG tổng thể:** 8/9 PASS, 1/9 PARTIAL (CG-7 — event-payload assertion chưa có test riêng, không chặn Draft nhưng cần bổ sung trước khi Approved). Không phát hiện vi phạm nghiêm trọng nào chặn việc tiếp tục review. **Cập nhật 2026-07-04:** ADR-VAC-233-005 Accepted đã un-gate toàn bộ test cases liên quan (`postpone_reason` persistence nay là assertion bắt buộc trong TC-001/002/003/INT-001/INT-002); không phát sinh CG mới cần re-check.

---

*TDD Spec v1.0 — Draft, chưa Approved (Status field không đổi bởi cập nhật 2026-07-04). ADR-VAC-233-005 nay **Accepted** (Option A, 2026-07-04) — cột `postpone_reason` không còn là schema gap mở; toàn bộ test cases liên quan đã un-gated và assert persistence là bắt buộc.*
