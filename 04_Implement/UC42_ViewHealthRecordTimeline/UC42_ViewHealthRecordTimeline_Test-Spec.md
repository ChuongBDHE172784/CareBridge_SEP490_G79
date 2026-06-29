# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-42 View Health Record Timeline

**Document ID:** `CB-HEALTH-TDD-004`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC42_ViewHealthRecordTimeline/UC42_ViewHealthRecordTimeline_TDS.md` (CB-HEALTH-IMP-004)
- SRS: §3.3.1.19
- Schema: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-42 View Health Record Timeline |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-42` |
| **Module** | `ViewHealthRecordTimeline — health` |
| **Spec gốc** | `CB-HEALTH-IMP-004` |
| **Priority** | 🔴 P0 |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `BR-RBAC, BR-PRIVACY, PDPA` |
| **Upstream Dependencies** | `UC-39, UC-40, UC-41 (HealthRecord entity and status)` |
| **Downstream Consumers** | `Mobile UI timeline view` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-HEALTH-IMP-004 §17`, `ADR-HEALTH-007`, `ADR-HEALTH-008` |
| **Constraints Injected** | C1: ACTIVE only; C2: ownerUserId from JWT; C3: ORDER BY date DESC; C4: max size 100; C5: no audit event; C6: empty=200; C7: readOnly transaction |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS 3.3.1.19: "hiển thị theo date, stage, type, source" — "stage" không có column riêng | V1 schema: `journey_id` map sang stage concept; `source_type`, `record_type` có column riêng | Test encode filters: journey_id, record_type, source_type — không có `stage` param |
| L2 | SRS không rõ ARCHIVED records có xuất hiện không | UC-42-BR-001: chỉ ACTIVE records | Test encode: seeding ARCHIVED record → không xuất hiện trong response |
| L3 | SRS không rõ ownership validation | BR-RBAC: ownerUserId phải từ JWT | Test encode: records của ACC-999 không xuất hiện trong ACC-001's timeline |
| L4 | SRS không rõ sort order | UC-42-BR-002: record_date DESC | Test encode: verify order in response items |
| L5 | SRS không rõ empty result behavior | UC-42-BR-001: 200 với items=[] | Test encode: no 404 on empty |
| L6 | V1 schema không có `health_record_files` table | V1: file_url là text column trong health_records | Test không expect nested file array — chỉ fileUrl string |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
ViewHealthRecordTimeline bao gồm các layer:
├── Service (HealthRecordService.getTimeline — mock JPA Repository)
├── Repository (IHealthRecordRepository.findActiveByOwnerFiltered — query correctness)
├── Controller (HealthRecordController.GET /timeline — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL — query + pagination + filter correctness)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-42` | Display records by date, stage (journey_id), type, source |
| `ADR-HEALTH-007` | Query must filter owner_user_id from JWT + status='ACTIVE' |
| `ADR-HEALTH-008` | Pagination: default=20, max=100 |
| `BR-RBAC` | ownerUserId always from JWT |
| `UC-42-BR-001` | ACTIVE only in response |
| `UC-42-BR-002` | ORDER BY record_date DESC |
| `CB-HEALTH-IMP-004 §10` | Error codes: HEALTH-001, IAM-001 |

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path — return ACTIVE records sorted | `HealthRecordService.getTimeline()` | `HEALTH42-TC-001` |
| TC-COND-002 | ARCHIVED records excluded | `WHERE status='ACTIVE'` in query | `HEALTH42-TC-002` |
| TC-COND-003 | Another user's records excluded | `WHERE owner_user_id=:ownerUserId` | `HEALTH42-TC-003` |
| TC-COND-004 | Filter by record_type | Optional filter in query | `HEALTH42-TC-004` |
| TC-COND-005 | Filter by journey_id | Optional filter in query | `HEALTH42-TC-005` |
| TC-COND-006 | Filter by baby_id | Optional filter in query | `HEALTH42-TC-006` |
| TC-COND-007 | Empty result — 200 not 404 | Service returns empty page | `HEALTH42-TC-007` |
| TC-COND-008 | Pagination: page and size | Pageable construction | `HEALTH42-TC-008` |
| TC-COND-009 | size > 100 → 400 | Max size enforcement | `HEALTH42-TC-009` |
| TC-COND-010 | Invalid record_type → 400 | DTO enum validation | `HEALTH42-TC-010` |
| TC-COND-011 | No JWT → 401 | Security filter | `HEALTH42-TC-SEC-001` |
| TC-COND-012 | EXPERT role → 403 | RBAC check | `HEALTH42-TC-SEC-002` |
| TC-COND-013 | Sort order: date DESC | Order verification | `HEALTH42-TC-INT-001` |
| TC-COND-014 | Full pagination flow in DB | Full integration | `HEALTH42-TC-INT-002` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | record_type (valid / invalid); user ownership | Filter correctness |
| Boundary Value Analysis | size=0, size=1, size=100, size=101, page=0 | Pagination boundaries |
| State Transition Testing | Records: ACTIVE (visible) / ARCHIVED (hidden) | UC-42-BR-001 |
| Pairwise Testing | Optional filters: alone and combined | Coverage of filter combinations |
| Error Guessing | ownerUserId not in JWT; cross-user access | Security compliance |

### TDS-05 — Test Data

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: 'ACC-001', role: 'MOTHER'}` | Happy path caller |
| `FX-002` | JWT | `{sub: 'ACC-999', role: 'MOTHER'}` | Another user |
| `FX-003` | JWT | `{sub: 'ACC-001', role: 'EXPERT'}` | Wrong role |
| `FX-004` | DB seed | `HR-A: owner=ACC-001, status=ACTIVE, type=LAB_RESULT, date=2026-06-20, journey=J-001` | Primary happy path |
| `FX-005` | DB seed | `HR-B: owner=ACC-001, status=ACTIVE, type=ULTRASOUND, date=2026-05-10, journey=J-001` | Second ACTIVE record |
| `FX-006` | DB seed | `HR-C: owner=ACC-001, status=ARCHIVED, type=LAB_RESULT, date=2026-04-01` | Archived — must be excluded |
| `FX-007` | DB seed | `HR-D: owner=ACC-999, status=ACTIVE, type=LAB_RESULT, date=2026-06-25` | Another user — must be excluded |
| `FX-008` | DB seed | `HR-E: owner=ACC-001, status=ACTIVE, type=PRESCRIPTION, babyId=BABY-001, date=2026-06-15` | Baby filter test |
| `FX-009` | Input | `{page: 0, size: 20}` | Default pagination |
| `FX-010` | Input | `{record_type: "LAB_RESULT"}` | Record type filter |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// HealthRecord42TestFactory.java
class HealthRecord42TestFactory {

    static final UUID ACC_001   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID ACC_999   = UUID.fromString("00000000-0000-0000-0000-000000000999");
    static final UUID HR_A      = UUID.fromString("00000000-0000-0000-0000-00000042000A".replace("A", "1"));
    static final UUID HR_B      = UUID.fromString("00000000-0000-0000-0000-000000420002");
    static final UUID HR_C      = UUID.fromString("00000000-0000-0000-0000-000000420003");
    static final UUID HR_D      = UUID.fromString("00000000-0000-0000-0000-000000420004");
    static final UUID JOURNEY_1 = UUID.fromString("00000000-0000-0000-0001-000000000001");
    static final UUID BABY_001  = UUID.fromString("00000000-0000-0000-0002-000000000001");

    static HealthRecord makeActiveLabResult() {
        HealthRecord r = new HealthRecord();
        r.setHealthRecordId(HR_A);
        r.setOwnerUserId(ACC_001);
        r.setRecordType("LAB_RESULT");
        r.setTitle("Blood Test Q2");
        r.setRecordDate(LocalDate.of(2026, 6, 20));
        r.setJourneyId(JOURNEY_1);
        r.setStatus("ACTIVE");
        r.setCreatedAt(Instant.parse("2026-06-20T08:00:00Z"));
        r.setUpdatedAt(Instant.parse("2026-06-20T08:00:00Z"));
        return r;
    }

    static HealthRecord makeActiveUltrasound() {
        HealthRecord r = makeActiveLabResult();
        r.setHealthRecordId(HR_B);
        r.setRecordType("ULTRASOUND");
        r.setTitle("Week 20 Ultrasound");
        r.setRecordDate(LocalDate.of(2026, 5, 10));
        return r;
    }

    static HealthRecord makeArchivedRecord() {
        HealthRecord r = makeActiveLabResult();
        r.setHealthRecordId(HR_C);
        r.setStatus("ARCHIVED");
        r.setRecordDate(LocalDate.of(2026, 4, 1));
        return r;
    }

    static HealthRecord makeOtherUserRecord() {
        HealthRecord r = makeActiveLabResult();
        r.setHealthRecordId(HR_D);
        r.setOwnerUserId(ACC_999);
        r.setRecordDate(LocalDate.of(2026, 6, 25));
        return r;
    }

    static TimelineFilter makeDefaultFilter() {
        TimelineFilter f = new TimelineFilter();
        f.setPage(0);
        f.setSize(20);
        return f;
    }
}
```

---

### HEALTH42-TC-001 — Happy path: ACTIVE records returned, sorted by date DESC

**Severity:** `CRITICAL`
**Feature Under Test:** `HealthRecordService.getTimeline()` — sorting and status filter
**Test File:** `src/test/java/com/carebridge/backend/health/service/HealthRecordServiceTimelineTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-42 Normal Flow, UC-42-BR-001, UC-42-BR-002`

**Preconditions:**
- Fixtures FX-004 (HR-A, date=2026-06-20) and FX-005 (HR-B, date=2026-05-10) for ACC-001

**Test Steps:**
1. Mock `recordRepository.findActiveByOwnerFiltered(ACC_001, null, null, null, null, pageable)` → `Page<HealthRecord>` containing HR-A then HR-B (date DESC order)
2. Call `service.getTimeline(ACC_001, makeDefaultFilter())`

**Expected Result (PASS):**
- Returns `TimelineResponse` with `items.size() = 2`
- `items.get(0).getHealthRecordId() == HR_A` (date=2026-06-20 — more recent)
- `items.get(1).getHealthRecordId() == HR_B` (date=2026-05-10)
- `totalElements = 2`
- All items have no ARCHIVED records

**Expected Result (FAIL):**
- Wrong sort order; or repository called without ACTIVE filter

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-002 — ARCHIVED records excluded from timeline

**Severity:** `CRITICAL`
**Feature Under Test:** `IHealthRecordRepository.findActiveByOwnerFiltered()` — status='ACTIVE' filter
**Test File:** `src/test/java/com/carebridge/backend/health/service/HealthRecordServiceTimelineTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `UC-42-BR-001, ADR-HEALTH-007`

**Preconditions:**
- FX-004 (ACTIVE) + FX-006 (ARCHIVED) both owned by ACC-001

**Test Steps:**
1. Mock `findActiveByOwnerFiltered` to return only FX-004 (simulating DB WHERE status='ACTIVE')
2. Call `service.getTimeline(ACC_001, makeDefaultFilter())`

**Expected Result (PASS):**
- Response items: only HR-A (ACTIVE); HR-C (ARCHIVED) NOT in items
- `totalElements = 1`

**Expected Result (FAIL):**
- HR-C appears in response

**Current Status:** 🔴 Not written
**Implementation Note:** The WHERE status='ACTIVE' must be in the JPA query — it cannot be done as post-filter in Service (to ensure DB-level enforcement).

---

### HEALTH42-TC-003 — Another user's records excluded

**Severity:** `CRITICAL`
**Feature Under Test:** `WHERE owner_user_id = :ownerUserId` in JPA query
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-RBAC, ADR-HEALTH-007`

**Preconditions:**
- FX-004 (ACC-001) + FX-007 (ACC-999) — both ACTIVE

**Test Steps:**
1. Mock `findActiveByOwnerFiltered(ACC_001, ...)` to return only records matching ACC_001
2. Call `service.getTimeline(ACC_001, makeDefaultFilter())`

**Expected Result (PASS):**
- Response contains only HR-A
- HR-D (ACC-999) NOT in response

**Expected Result (FAIL):**
- HR-D appears in response → data isolation breach

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-004 — Filter by record_type

**Severity:** `HIGH`
**Feature Under Test:** `TimelineFilter.recordType` → optional WHERE clause
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Preconditions:**
- FX-004 (LAB_RESULT) + FX-005 (ULTRASOUND), both ACTIVE, ACC-001

**Test Steps:**
1. Set filter: `recordType = "LAB_RESULT"`
2. Mock `findActiveByOwnerFiltered(ACC_001, "LAB_RESULT", null, null, null, pageable)` → return only HR-A
3. Call `service.getTimeline(ACC_001, filter)`

**Expected Result (PASS):**
- Response contains only HR-A (LAB_RESULT)
- HR-B (ULTRASOUND) NOT in response
- Repository called with `recordType = "LAB_RESULT"` (verify mock arg)

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-005 — Filter by journey_id

**Severity:** `MEDIUM`
**Feature Under Test:** `TimelineFilter.journeyId` optional filter
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Set filter: `journeyId = JOURNEY_1`
2. Mock repository to return records matching journey_id
3. Call `service.getTimeline(ACC_001, filter)`

**Expected Result (PASS):**
- Repository called with `journeyId = JOURNEY_1` argument
- Response contains only records with matching journey_id

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-006 — Filter by baby_id

**Severity:** `MEDIUM`
**Feature Under Test:** `TimelineFilter.babyId` optional filter
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:**
1. Set filter: `babyId = BABY_001`
2. Mock repository to return FX-008 (HR-E with babyId=BABY_001)
3. Verify response contains HR-E; HR-A (no babyId) excluded

**Expected Result (PASS):**
- Repository called with `babyId = BABY_001`

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-007 — Empty result → 200 OK (not 404)

**Severity:** `HIGH`
**Feature Under Test:** `HealthRecordService.getTimeline()` — empty page handling
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `UC-42-BR-001`

**Test Steps:**
1. Mock `findActiveByOwnerFiltered(...)` → empty page (`Page.empty()`)
2. Call `service.getTimeline(ACC_001, makeDefaultFilter())`

**Expected Result (PASS):**
- Returns `TimelineResponse` with `items = []`, `totalElements = 0`, `totalPages = 0`
- No exception thrown
- Status would be 200 (verified in controller test)

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-008 — Pagination: page and size params forwarded correctly

**Severity:** `MEDIUM`
**Feature Under Test:** `Pageable` construction in Service
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`

**Test Steps:**
1. Set filter: `page=2, size=5`
2. Capture `Pageable` argument passed to `findActiveByOwnerFiltered`
3. Call `service.getTimeline(ACC_001, filter)`

**Expected Result (PASS):**
- `Pageable.getPageNumber() == 2`
- `Pageable.getPageSize() == 5`

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-009 — size > 100 → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@Max(100)` on `TimelineFilter.size` — DTO validation
**Test File:** `src/test/java/com/carebridge/backend/health/controller/HealthRecordControllerTimelineTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-HEALTH-008`

**Test Steps:**
1. GET `/api/v1/health-records/timeline?size=200` with FX-001 JWT

**Expected Result (PASS):**
- HTTP 400
- Response contains `"code": "HEALTH-001"`

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-010 — Invalid record_type → 400

**Severity:** `MEDIUM`
**Feature Under Test:** `@Pattern` on `TimelineFilter.recordType`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`

**Test Steps:**
1. GET `/api/v1/health-records/timeline?record_type=INVALID_TYPE` with FX-001 JWT

**Expected Result (PASS):**
- HTTP 400
- Response contains `"code": "HEALTH-001"`, field=`record_type`

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### HEALTH42-TC-SEC-001 — No JWT → 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306 — Missing Authentication for Critical Function`
**Feature Under Test:** `HealthRecordController` — JWT filter on timeline endpoint
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Test Steps:**
1. GET `/api/v1/health-records/timeline` without Authorization header

**Expected Result (PASS = hệ thống an toàn):**
- HTTP 401

**Expected Result (FAIL = lỗ hổng):**
- Timeline accessible without JWT

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-SEC-002 — EXPERT role → 403

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `@PreAuthorize("hasRole('MOTHER')")` on GET /timeline
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-012`

**Test Steps:**
1. GET with FX-003 JWT (role=EXPERT)

**Expected Result (PASS):**
- HTTP 403

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### HEALTH42-TC-INT-001 — Sort order verified in real DB query

**Severity:** `CRITICAL`
**Feature Under Test:** `ORDER BY record_date DESC` in repository query — real DB
**Test File:** `src/test/java/com/carebridge/backend/health/HealthRecordTimelineIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-013`

**Preconditions:**
- PostgreSQL Testcontainer
- Seed 3 ACTIVE records: HR-A (date=2026-06-20), HR-B (date=2026-05-10), HR-E (date=2026-06-26)

**Test Steps:**
1. Seed 3 records with different `record_date`
2. GET `/api/v1/health-records/timeline` (no filter)
3. Verify order

**Expected Result (PASS):**
- `items[0].recordDate = "2026-06-26"` (HR-E, most recent)
- `items[1].recordDate = "2026-06-20"` (HR-A)
- `items[2].recordDate = "2026-05-10"` (HR-B, oldest)

```java
List<HealthRecordTimelineItem> items = response.getItems();
assertThat(items).hasSize(3);
assertThat(items.get(0).getRecordDate()).isEqualTo(LocalDate.of(2026, 6, 26));
assertThat(items.get(1).getRecordDate()).isEqualTo(LocalDate.of(2026, 6, 20));
assertThat(items.get(2).getRecordDate()).isEqualTo(LocalDate.of(2026, 5, 10));
```

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-INT-002 — ARCHIVED records truly excluded in real DB

**Severity:** `CRITICAL`
**Feature Under Test:** `WHERE status='ACTIVE'` in real DB query
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002` (integration-level)

**Test Steps:**
1. Seed: HR-A (ACTIVE), HR-C (ARCHIVED) — both for ACC-001
2. GET `/api/v1/health-records/timeline`

**Expected Result (PASS):**
- Response: `totalElements = 1`, items contains only HR-A
- HR-C NOT in items

```java
TimelineResponse response = callTimeline(motherJwt);
assertThat(response.getTotalElements()).isEqualTo(1);
assertThat(response.getItems()).extracting(i -> i.getHealthRecordId())
    .containsExactly(HR_A)
    .doesNotContain(HR_C);
```

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-INT-003 — Pagination: page 2 of 3 returns correct subset

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008` (integration)

**Test Steps:**
1. Seed 5 ACTIVE records for ACC-001 with dates 2026-06-01 through 2026-06-05
2. GET `?page=1&size=2` (second page, size 2)

**Expected Result (PASS):**
- HTTP 200
- `totalElements = 5`, `totalPages = 3`, `page = 1`
- `items.size() = 2`
- Items are 3rd and 4th records in date DESC order

```java
assertThat(response.getTotalElements()).isEqualTo(5);
assertThat(response.getTotalPages()).isEqualTo(3);
assertThat(response.getPage()).isEqualTo(1);
assertThat(response.getItems()).hasSize(2);
```

**Current Status:** 🔴 Not written

---

### HEALTH42-TC-INT-004 — Cross-user isolation: ACC-001 cannot see ACC-999's records

**Severity:** `CRITICAL`
**Feature Under Test:** `WHERE owner_user_id = :ownerUserId` in real DB
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003` (integration)

**Test Steps:**
1. Seed HR-D (ACTIVE, owner=ACC-999)
2. Seed HR-A (ACTIVE, owner=ACC-001)
3. Login as ACC-001, GET `/api/v1/health-records/timeline`

**Expected Result (PASS):**
- Response contains HR-A only
- HR-D NOT in items
- `totalElements = 1` (not 2)

```java
TimelineResponse response = callTimeline(acc001Jwt);
assertThat(response.getItems())
    .extracting(HealthRecordTimelineItem::getHealthRecordId)
    .doesNotContain(HR_D);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `HEALTH42-TC-001` | `HealthRecordServiceTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-002` | `HealthRecordServiceTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-003` | `HealthRecordServiceTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-004` | `HealthRecordServiceTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-007` | `HealthRecordServiceTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-008` | `HealthRecordServiceTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-009` | `HealthRecordControllerTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-010` | `HealthRecordControllerTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-SEC-001` | `HealthRecordControllerTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-SEC-002` | `HealthRecordControllerTimelineTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-INT-001` | `HealthRecordTimelineIntegrationTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-INT-002` | `HealthRecordTimelineIntegrationTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-INT-003` | `HealthRecordTimelineIntegrationTest.java` | `[ ]` | `___` | — |
| `HEALTH42-TC-INT-004` | `HealthRecordTimelineIntegrationTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// HealthRecordService.java — Red Phase stub
@Override
public TimelineResponse getTimeline(UUID ownerUserId, TimelineFilter filter) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}

// IHealthRecordRepository — no findActiveByOwnerFiltered method yet
// Stub: method body throws UnsupportedOperationException or does not exist
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|----------------------------------|
| `HEALTH42-TC-001` | throw | 🔴 FAIL | — |
| `HEALTH42-TC-002` | throw | 🔴 FAIL | — |
| `HEALTH42-TC-INT-001` | throw | 🔴 FAIL | — |
| `HEALTH42-TC-INT-004` | throw | 🔴 FAIL | — |

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-HEALTH-IMP-004` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm — đặc biệt L1 (stage → journey_id) và L2 (ACTIVE only)
- [ ] `HealthRecord` entity từ UC-39 đã tồn tại
- [ ] UC-41 đã implement để có ARCHIVED records trong DB để test

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh (Testcontainers)
- [ ] ARCHIVED records không xuất hiện trong bất kỳ timeline response nào
- [ ] Cross-user isolation: ACC-001 không thấy ACC-999's records
- [ ] Sort order: record_date DESC confirmed in integration test
- [ ] Pagination: totalElements, totalPages, page, size chính xác
- [ ] Empty result: 200 với items=[], không phải 404
- [ ] No audit event emitted for GET /timeline

**Exit Criteria CASE 2.0:**

- [ ] **Red Gate** — tất cả tests FAIL với throw stub
- [ ] **Contract Existence** — `TimelineFilter`, `TimelineResponse`, `HealthRecordTimelineItem`, `findActiveByOwnerFiltered` tồn tại
- [ ] **No N+1** — chỉ 1 SELECT query per GET request (verify via Hibernate SQL log in test)

```bash
# Verify no N+1 — set show-sql=true in test properties and count SELECT statements
grep -c "select.*health_records" target/surefire-reports/*.txt
# Expected: 1 per integration test scenario
```

### Suspension Criteria

- `HealthRecord` entity chưa tồn tại (UC-39 chưa implement)
- Testcontainers không khởi động được

---

## 7. Rollback Plan

```bash
# Không có migration — revert code
git checkout -- src/main/java/com/carebridge/backend/health/service/HealthRecordService.java
git checkout -- src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java
git checkout -- src/main/java/com/carebridge/backend/health/repository/IHealthRecordRepository.java
git checkout -- src/test/java/com/carebridge/backend/health/service/HealthRecordServiceTimelineTest.java
git checkout -- src/test/java/com/carebridge/backend/health/controller/HealthRecordControllerTimelineTest.java
git checkout -- src/test/java/com/carebridge/backend/health/HealthRecordTimelineIntegrationTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-HEALTH-007 (status filter) | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | TC-001 PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | ownerUserId từ query param thay vì JWT | ☐ | G-1 |
| AP-AI-004 | Layer Violation | status filter trong Service post-fetch thay vì JPA query | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Query join `health_record_files` (không có trong V1) | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Template v2.0 — UC-42 View Health Record Timeline*
*Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
