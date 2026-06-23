# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-99: View Moderation Queue

**Document ID:** `CB-MOD-TEST-001`
**Version:** `1.0`
**Date:** `2026-06-23`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/implement_artifacts/UC99_ViewModerationQueue_TDS.md` (CB-MOD-IMP-001)
- SRS Section 3.2.2.1
- CLAUDE.md §3 Architecture Rules, §7 Entity Ownership

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                           |
| ---------- | ------------------ | ----------------------------------------------------------- |
| 2026-06-23 | AI Agent — Winston | Khởi tạo tài liệu Test-Spec cho UC-99 View Moderation Queue |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field                     | Value                                                                                                           |
| ------------------------- | --------------------------------------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-99`                                                                                                         |
| **Module**                | `View Moderation Queue — content + community`                                                                   |
| **Spec gốc**              | `CB-MOD-IMP-001`                                                                                                |
| **Priority**              | P0 — High, Frequent                                                                                             |
| **Sprint**                | `S1 (2026-07-01 → 2026-07-14)`                                                                                  |
| **Milestone**             | `M1 Alpha — Admin Web Portal`                                                                                   |
| **Data Classification**   | `Internal`                                                                                                      |
| **Compliance Scope**      | `N/A`                                                                                                           |
| **Upstream Dependencies** | `security (JWT)`, `content (ContentReport, ModerationAction)`, `community (CommunityQuestion, CommunityAnswer)` |
| **Downstream Consumers**  | `Moderation Action API (UC-100)`                                                                                |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                                                                |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                                                                                |
| **Constraint Source**    | `CB-MOD-IMP-001 §17`, `ADR-001 §Decision`, `ADR-002 §Decision`, `ADR-003 §Decision`                                                                                  |
| **Constraints Injected** | `C1 (RBAC @PreAuthorize)`, `C2 (AuditService.log())`, `C3 (JPA repository)`, `C4 (preview truncate 200 chars)`, `C5 (sort reportedAt DESC)`, `C6 (page size max 50)` |
| **Model**                | `claude-sonnet-4-6`                                                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                                                         |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                           | Thực tế (schema / policy)                         | Fix áp dụng trong test                                             |
| --- | ---------------------------------------------------------------- | ------------------------------------------------- | ------------------------------------------------------------------ |
| L1  | Spec không chỉ rõ default status khi không truyền `status` param | BR-MOD-001: default = PENDING                     | Test phải verify request không có status param → vẫn query PENDING |
| L2  | Spec không đề cập truncate preview                               | BR-MOD-002 từ TDS §13: preview max 200 chars      | Test phải assert preview.length() <= 200                           |
| L3  | Spec không chỉ rõ sort order                                     | BR-MOD-003 từ TDS §4.1: sắp xếp reportedAt DESC   | Test seed data với nhiều dates, verify order                       |
| L4  | CONTENT_ADMIN không được mention trong spec gốc UC-99            | Auth matrix TDS §16: CONTENT_ADMIN không có quyền | Test: CONTENT_ADMIN bị 403                                         |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-99 View Moderation Queue bao gồm các layer:
├── Controller (ModerationController — mock service)
├── Service (ModerationServiceImpl — mock repository + mock audit)
├── Repository (ModerationRepository — Testcontainers PostgreSQL)
└── Integration (Full API flow — MockMvc + Testcontainers)
```

### TDS-02 — Test Basis

| Source                       | Items Derived                                            |
| ---------------------------- | -------------------------------------------------------- |
| `SRS 3.2.2.1`                | Moderator xem queue, filter theo contentType, pagination |
| `TDS CB-MOD-IMP-001 ADR-001` | Aggregate pattern — ContentReport trước, preview sau     |
| `TDS CB-MOD-IMP-001 ADR-002` | @PreAuthorize MODERATOR role tại controller              |
| `TDS CB-MOD-IMP-001 ADR-003` | AuditService.log() sau mỗi queue view                    |
| `BR-MOD-001`                 | Default status = PENDING                                 |
| `BR-MOD-002`                 | Filter theo contentType                                  |
| `BR-MOD-003`                 | Sort by reportedAt DESC, page size max 50                |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                  | Coverage Item                                | Test Cases       |
| ------------ | ----------------------------------------------- | -------------------------------------------- | ---------------- |
| TC-COND-001  | Moderator gọi queue với filter QUESTION/PENDING | `ModerationServiceImpl.getModerationQueue()` | `MOD-TC-001`     |
| TC-COND-002  | Không truyền contentType → trả về tất cả types  | `ModerationRepository.findByStatus()`        | `MOD-TC-002`     |
| TC-COND-003  | Preview bị truncate tại 200 chars               | `ContentPreviewService.fetchPreview()`       | `MOD-TC-003`     |
| TC-COND-004  | AuditService.log() được gọi mỗi lần xem queue   | `AuditService` mock verify                   | `MOD-TC-004`     |
| TC-COND-005  | Sort order là reportedAt DESC                   | `Pageable.sort` config                       | `MOD-TC-005`     |
| TC-COND-006  | Non-MODERATOR bị 403                            | `@PreAuthorize` Spring Security              | `MOD-TC-006`     |
| TC-COND-007  | Page size > 50 trả về MOD-002                   | `ModerationQueueFilter` validation           | `MOD-TC-007`     |
| TC-COND-008  | Invalid contentType → 400 MOD-001               | Input binding error handler                  | `MOD-TC-008`     |
| TC-COND-009  | Full integration flow DB → API                  | Testcontainers integration                   | `MOD-TC-INT-001` |
| TC-COND-010  | SQL injection trong query param                 | Security filter                              | `MOD-TC-SEC-003` |

### TDS-04 — Test Techniques

| Technique                | Applied To                                   | Rationale                                       |
| ------------------------ | -------------------------------------------- | ----------------------------------------------- |
| Equivalence Partitioning | contentType enum values                      | 4 values: QUESTION, ANSWER, CONTENT, null (all) |
| Boundary Value Analysis  | page size: 1, 50, 51                         | Max constraint at 50                            |
| State Transition Testing | ReportStatus: PENDING → RESOLVED → DISMISSED | Read-only view — state không đổi trong UC-99    |
| Error Guessing           | SQL injection, JWT tampering                 | Security test vectors                           |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                                             | Mục đích                                      |
| ---------- | ------- | ----------------------------------------------------------------------------------------- | --------------------------------------------- |
| `FX-001`   | DB seed | `ContentReport{id:1, contentType:QUESTION, status:PENDING, reportedAt:2026-06-22T14:00}`  | Happy path — QUESTION filter                  |
| `FX-002`   | DB seed | `ContentReport{id:2, contentType:ANSWER, status:PENDING, reportedAt:2026-06-22T10:00}`    | Filter by ANSWER                              |
| `FX-003`   | DB seed | `ContentReport{id:3, contentType:QUESTION, status:RESOLVED, reportedAt:2026-06-21T09:00}` | RESOLVED không xuất hiện trong PENDING filter |
| `FX-004`   | DB seed | `CommunityQuestion{id:1, content: "A".repeat(500)}`                                       | Preview truncation test                       |
| `FX-005`   | JWT     | `{sub: "mod-001", role: "ROLE_MODERATOR"}`                                                | Auth happy path                               |
| `FX-006`   | JWT     | `{sub: "user-001", role: "ROLE_MOTHER"}`                                                  | Auth failure                                  |
| `FX-007`   | JWT     | Expired token                                                                             | Auth failure (401)                            |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// === MOD-TC Props Isolation Pattern ===
// Đặt ở đầu mỗi test class — dùng factory method

private static final UUID REPORT_ID_1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
private static final UUID REPORT_ID_2 = UUID.fromString("11111111-0000-0000-0000-000000000002");

ContentReport makeReport(Consumer<ContentReport> overrides) {
    ContentReport r = new ContentReport();
    r.setId(REPORT_ID_1);
    r.setReportedContentId(UUID.randomUUID());
    r.setContentType(ContentType.QUESTION);
    r.setReason("Test reason");
    r.setStatus(ReportStatus.PENDING);
    r.setReportedBy(UUID.randomUUID());
    r.setReportedAt(LocalDateTime.of(2026, 6, 22, 10, 0));
    overrides.accept(r);
    return r;
}

ModerationQueueFilter makeFilter(Consumer<ModerationQueueFilter.Builder> overrides) {
    // Sử dụng Builder hoặc record constructor với defaults
    return ModerationQueueFilter.builder()
        .status(ReportStatus.PENDING)
        .page(0)
        .size(20)
        .applyCustomizations(overrides)
        .build();
}
```

---

### MOD-TC-001 — getModerationQueue trả về đúng items với filter QUESTION/PENDING

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.getModerationQueue(filter)`
**Test File:** `src/test/java/com/carebridge/backend/unit/ModerationServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-MOD-001`, `BR-MOD-002`, `TDS §8.1 Service Interface`

**Preconditions:**
- `ModerationRepository` mock: `findByStatusAndContentType(PENDING, QUESTION, pageable)` trả về Page chứa `[FX-001]`
- `ContentPreviewService` mock: trả về "Preview text" cho FX-001
- `AuditService` mock: spy/verify

**Test Steps:**
1. Arrange: setup mocks như trên, filter = {contentType: QUESTION, status: PENDING, page: 0, size: 20}
2. Act: `service.getModerationQueue(filter)`
3. Assert: kết quả

**Expected Result (PASS):**
- `response.content.size()` = 1
- `response.content[0].contentType` = `QUESTION`
- `response.content[0].status` = `PENDING`
- `response.totalElements` = 1
- `ModerationRepository.findByStatusAndContentType()` được gọi đúng 1 lần
- `AuditService.log()` được gọi đúng 1 lần

**Expected Result (FAIL):**
- Nếu service không filter đúng → ANSWER items xuất hiện trong kết quả
- Nếu AuditService không được gọi → audit log missing

**Current Status:** 🔴 Not written
**Implementation Note:** `ModerationServiceImpl` phải gọi `repository.findByStatusAndContentType()` khi filter.contentType != null.

---

### MOD-TC-002 — Không truyền contentType → query tất cả types

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.getModerationQueue(filter)` — no contentType filter
**Test File:** `src/test/java/com/carebridge/backend/unit/ModerationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-MOD-001`, `TDS §8.3 ModerationQueueFilter`

**Preconditions:**
- `ModerationRepository` mock: `findByStatus(PENDING, pageable)` trả về Page chứa items với QUESTION và ANSWER types
- filter = {contentType: null, status: PENDING, page: 0, size: 20}

**Test Steps:**
1. Arrange: filter với contentType = null
2. Act: `service.getModerationQueue(filter)`
3. Assert:

**Expected Result (PASS):**
- `repository.findByStatus(PENDING, pageable)` được gọi (không gọi `findByStatusAndContentType`)
- Response chứa cả QUESTION và ANSWER items

**Expected Result (FAIL):**
- Nếu service hard-code filter contentType → chỉ trả về 1 loại khi null

**Current Status:** 🔴 Not written

---

### MOD-TC-003 — Preview bị truncate tại 200 ký tự

**Severity:** `MEDIUM`
**Feature Under Test:** `ContentPreviewService.fetchPreview()` (hoặc ModerationMapper)
**Test File:** `src/test/java/com/carebridge/backend/unit/ContentPreviewServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-MOD-002`, `TDS §4.3 Security`

**Preconditions:**
- ContentItem với body = "A".repeat(500) (FX-004)

**Test Steps:**
1. Arrange: content với body dài 500 chars
2. Act: `previewService.fetchPreview(contentId, ContentType.QUESTION)`
3. Assert:

**Expected Result (PASS):**
- `preview.length()` <= 200
- Preview kết thúc bằng "..." (truncation indicator) nếu content bị cắt

**Expected Result (FAIL):**
- Preview dài hơn 200 chars → bộ nhớ và response quá lớn

**Current Status:** 🔴 Not written

---

### MOD-TC-004 — AuditService.log() được gọi sau mỗi queue view

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.getModerationQueue()` — audit side effect
**Test File:** `src/test/java/com/carebridge/backend/unit/ModerationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-003`, `BR-AUDIT-001`

**Preconditions:**
- `AuditService` là mock/spy
- `ModerationRepository` trả về kết quả bình thường

**Test Steps:**
1. Arrange: setup mocks
2. Act: `service.getModerationQueue(filter)` — gọi 3 lần
3. Assert: `auditService.log()` được gọi đúng 3 lần

**Expected Result (PASS):**
- `verify(auditService, times(3)).log(any(ModerationQueueViewedEvent.class))`
- Event payload chứa `actorId`, `resultCount`, `contentType`

**Expected Result (FAIL):**
- AuditService không được gọi → audit trail missing

**Current Status:** 🔴 Not written

---

### MOD-TC-005 — Kết quả sắp xếp theo reportedAt DESC

**Severity:** `HIGH`
**Feature Under Test:** `ModerationRepository.findByStatus()` + Pageable sort config
**Test File:** `src/test/java/com/carebridge/backend/unit/ModerationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-MOD-003`, `TDS §8.1 Interface`

**Preconditions:**
- `ModerationRepository` mock: verify Pageable argument

**Test Steps:**
1. Arrange: filter = {status: PENDING, page: 0, size: 20}
2. Act: `service.getModerationQueue(filter)`
3. Assert: capture Pageable argument passed to repository

**Expected Result (PASS):**
- `pageableCaptor.getValue().getSort()` = `Sort.by(Sort.Direction.DESC, "reportedAt")`

**Expected Result (FAIL):**
- Sort không được truyền vào repository → items theo thứ tự sai

**Current Status:** 🔴 Not written

---

### MOD-TC-006 — Non-MODERATOR bị 403 Forbidden

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `ModerationController.getQueue()` — @PreAuthorize
**Test File:** `src/test/java/com/carebridge/backend/security/ModerationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-002`, `BR-RBAC-001`, `TDS §16 Auth Matrix`

**Preconditions:**
- MockMvc với Spring Security test support
- JWT token với role ROLE_MOTHER (FX-006)

**Test Steps:**
1. Arrange: JWT với ROLE_MOTHER
2. Act: `GET /api/v1/admin/moderation/queue` với MOTHER token
3. Assert:

**Expected Result (PASS — hệ thống an toàn):**
- `response.status` = 403
- `response.body.error.code` = "MOD-004"
- Không có moderation data trong response body

**Expected Result (FAIL = lỗ hổng):**
- Response trả về 200 với queue data cho MOTHER user → broken access control

**Current Status:** 🔴 Not written

---

### MOD-TC-007 — Page size > 50 trả về 400 MOD-002

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationQueueFilter` validation
**Test File:** `src/test/java/com/carebridge/backend/unit/ModerationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-MOD-003`, `TDS §10 Error Codes`

**Test Steps:**
1. Act: `GET /api/v1/admin/moderation/queue?size=51` với MODERATOR JWT

**Expected Result (PASS):**
- `response.status` = 400
- `response.body.error.code` = "MOD-002"

**Current Status:** 🔴 Not written

---

### MOD-TC-008 — Invalid contentType → 400 MOD-001

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationController.getQueue()` — binding error
**Test File:** `src/test/java/com/carebridge/backend/unit/ModerationControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §10 Error Codes`

**Test Steps:**
1. Act: `GET /api/v1/admin/moderation/queue?contentType=INVALID_VALUE` với MODERATOR JWT

**Expected Result (PASS):**
- `response.status` = 400
- `response.body.error.code` = "MOD-001"
- `response.body.error.details[0].field` = "contentType"

**Current Status:** 🔴 Not written

---

### MOD-TC-009 — Request không có JWT trả về 401

**Severity:** `CRITICAL`
**Feature Under Test:** JWT authentication filter
**Test File:** `src/test/java/com/carebridge/backend/security/ModerationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Act: `GET /api/v1/admin/moderation/queue` không có Authorization header

**Expected Result (PASS):**
- `response.status` = 401
- `response.body.error.code` = "MOD-006" hoặc "IAM-001"

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### MOD-TC-INT-001 — Full API flow với real DB (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** `GET /api/v1/admin/moderation/queue` — end to end
**Test File:** `src/test/java/com/carebridge/backend/integration/ModerationQueueIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL Testcontainer đang chạy
- Schema đã apply (migration)
- Seed data: FX-001 (QUESTION/PENDING), FX-002 (ANSWER/PENDING), FX-003 (QUESTION/RESOLVED)
- MODERATOR JWT hợp lệ

**Test Steps:**
1. Seed FX-001, FX-002, FX-003 vào Testcontainer DB
2. `GET /api/v1/admin/moderation/queue?status=PENDING&page=0&size=20` với MODERATOR JWT
3. Assert response
4. `GET /api/v1/admin/moderation/queue?contentType=QUESTION&status=PENDING` với MODERATOR JWT
5. Assert filtered response

**Expected Result (PASS):**
- Step 2: `response.content.size()` = 2 (FX-001 và FX-002)
- Step 2: `response.totalElements` = 2
- Step 2: FX-003 (RESOLVED) không có trong kết quả
- Step 4: `response.content.size()` = 1 (chỉ FX-001 QUESTION/PENDING)
- Items sắp xếp theo reportedAt DESC

**Expected Result (FAIL):**
- FX-003 (RESOLVED) xuất hiện → filter bị bug
- Sort order sai → items cũ hơn xuất hiện đầu

**DB Assertion:**
```java
// Verify trong DB sau test (read-only — không có mutation trong UC-99)
long pendingCount = moderationRepository.countByStatus(ReportStatus.PENDING);
assertThat(pendingCount).isEqualTo(2); // FX-001 + FX-002
```

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### MOD-TC-SEC-001 — Expired JWT trả về 401

**Severity:** `CRITICAL`
**Feature Under Test:** JWT authentication filter
**Test File:** `src/test/java/com/carebridge/backend/security/ModerationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Tạo JWT đã expired (FX-007)
2. `GET /api/v1/admin/moderation/queue` với expired JWT

**Expected Result (PASS):** HTTP 401

**Current Status:** 🔴 Not written

---

### MOD-TC-SEC-002 — CONTENT_ADMIN bị 403 (không phải MODERATOR)

**Severity:** `HIGH`
**Feature Under Test:** @PreAuthorize ROLE_MODERATOR
**Test File:** `src/test/java/com/carebridge/backend/security/ModerationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Oracle Source:** `TDS §16 Auth Matrix — CONTENT_ADMIN = ❌`

**Test Steps:**
1. JWT với role ROLE_CONTENT_ADMIN
2. `GET /api/v1/admin/moderation/queue`

**Expected Result (PASS):** HTTP 403 với MOD-004

**Current Status:** 🔴 Not written

---

### MOD-TC-SEC-003 — SQL Injection không ảnh hưởng query

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `ModerationController` — contentType param
**Test File:** `src/test/java/com/carebridge/backend/security/ModerationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. `GET /api/v1/admin/moderation/queue?contentType=QUESTION'; DROP TABLE content_reports;--` với MODERATOR JWT
2. Kiểm tra DB sau request

**Expected Result (PASS):**
- Response là 400 (invalid enum value — không phải SQL execution)
- DB table `content_reports` vẫn tồn tại và intact
- Spring Data JPA (parameterized queries) ngăn injection

**Expected Result (FAIL):**
- 500 error từ DB — injection được thực thi

**Current Status:** 🔴 Not written

---

### MOD-TC-SEC-004 — Response không expose full content body

**Severity:** `HIGH`
**Feature Under Test:** `ModerationMapper.toQueueItemResponse()`
**Test File:** `src/test/java/com/carebridge/backend/unit/ModerationMapperTest.java`
**TDD Phase:** 🔴 RED
**Oracle Source:** `TDS §4.3 — preview max 200 chars`, `C4 constraint`

**Test Steps:**
1. Arrange: ContentItem với body = "A".repeat(1000)
2. Act: map to ModerationQueueItemResponse
3. Assert response.contentPreview

**Expected Result (PASS):**
- `response.contentPreview.length()` <= 200
- Full body không expose trong response

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                               | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ---------------- | --------------------------------------- | --------------- | ---------------- | --------------- |
| `MOD-TC-001`     | `ModerationServiceImplTest.java`        | `[ ]`           | —                | —               |
| `MOD-TC-002`     | `ModerationServiceImplTest.java`        | `[ ]`           | —                | —               |
| `MOD-TC-003`     | `ContentPreviewServiceTest.java`        | `[ ]`           | —                | —               |
| `MOD-TC-004`     | `ModerationServiceImplTest.java`        | `[ ]`           | —                | —               |
| `MOD-TC-005`     | `ModerationServiceImplTest.java`        | `[ ]`           | —                | —               |
| `MOD-TC-006`     | `ModerationControllerSecurityTest.java` | `[ ]`           | —                | —               |
| `MOD-TC-007`     | `ModerationControllerTest.java`         | `[ ]`           | —                | —               |
| `MOD-TC-008`     | `ModerationControllerTest.java`         | `[ ]`           | —                | —               |
| `MOD-TC-009`     | `ModerationControllerSecurityTest.java` | `[ ]`           | —                | —               |
| `MOD-TC-INT-001` | `ModerationQueueIntegrationTest.java`   | `[ ]`           | —                | —               |
| `MOD-TC-SEC-001` | `ModerationControllerSecurityTest.java` | `[ ]`           | —                | —               |
| `MOD-TC-SEC-002` | `ModerationControllerSecurityTest.java` | `[ ]`           | —                | —               |
| `MOD-TC-SEC-003` | `ModerationControllerSecurityTest.java` | `[ ]`           | —                | —               |
| `MOD-TC-SEC-004` | `ModerationMapperTest.java`             | `[ ]`           | —                | —               |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// ModerationServiceImpl.java — Red Phase stub
@Service
public class ModerationServiceImpl implements ModerationService {
    @Override
    public ModerationQueueResponse getModerationQueue(ModerationQueueFilter filter) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID            | Stub Result                           | Expected        | Actual        | Root Cause (nếu PASS bất thường) |
| ---------------- | ------------------------------------- | --------------- | ------------- | -------------------------------- |
| `MOD-TC-001`     | `throw UnsupportedOperationException` | 🔴 FAIL          | ☐ FAIL ☐ PASS | —                                |
| `MOD-TC-002`     | `throw UnsupportedOperationException` | 🔴 FAIL          | ☐ FAIL ☐ PASS | —                                |
| `MOD-TC-003`     | `throw UnsupportedOperationException` | 🔴 FAIL          | ☐ FAIL ☐ PASS | —                                |
| `MOD-TC-004`     | `throw UnsupportedOperationException` | 🔴 FAIL          | ☐ FAIL ☐ PASS | —                                |
| `MOD-TC-005`     | `throw UnsupportedOperationException` | 🔴 FAIL          | ☐ FAIL ☐ PASS | —                                |
| `MOD-TC-006`     | `no-op controller (no @PreAuthorize)` | 🔴 FAIL (no 403) | ☐ FAIL ☐ PASS | —                                |
| `MOD-TC-INT-001` | `throw UnsupportedOperationException` | 🔴 FAIL          | ☐ FAIL ☐ PASS | —                                |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → GATE-2 PASS (T2→T3) → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-MOD-IMP-001` đã review
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead
- [ ] DB migration `V20260623__create_content_reports_moderation_actions.sql` đã approved
- [ ] Test fixtures FX-001 đến FX-007 đã chuẩn bị
- [ ] Spring Security test dependencies đã có trong `pom.xml` (`spring-security-test`)

### Exit Criteria (DoD)

- [ ] `mvn test -Dtest=ModerationServiceImplTest` — tất cả unit tests xanh
- [ ] `mvn test -Dtest=ModerationControllerTest` — tất cả controller tests xanh
- [ ] `mvn test -Dtest=ModerationControllerSecurityTest` — tất cả security tests xanh
- [ ] `mvn test -Dtest=ModerationQueueIntegrationTest` — integration tests xanh
- [ ] Test coverage ≥ 80% lines cho `ModerationServiceImpl`, `ModerationMapper`, `ContentPreviewService`
- [ ] Không có shared mutable state giữa test cases
- [ ] MOD-TC-006: Non-MODERATOR nhận 403 (CRITICAL security gate)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] Red Gate (§5.1) — tất cả tests FAIL với throw stub
- [ ] Contract Existence — mọi import trong test files resolve:
  ```bash
  cd 05_Development/CareBridgeAPI
  ./mvnw compile -pl . 2>&1 | grep "cannot find symbol"
  # Expected: no output
  ```
- [ ] Props Isolation — không có shared mutable ContentReport instance giữa test methods
- [ ] Oracle Source — mọi `assertThat(...).isEqualTo(...)` đều có comment chỉ nguồn BR/ADR

### Suspension Criteria

- DB migration bị block
- Spring Security config chưa enable `@EnableMethodSecurity`
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/content/

# Revert DB migration nếu cần
# ./mvnw flyway:undo -Dflyway.target=[previous_version]

# Test spec files được giữ nguyên (không rollback test spec)
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                              | Check | Gate chặn |
| --------- | ------------------------ | ---------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-002 (RBAC)                    | ☐     | G-0       |
| AP-AI-002 | Green-from-Birth         | MOD-TC-006 PASS với empty stub (no @PreAuthorize)    | ☐     | G-2       |
| AP-AI-003 | Implicit Decision        | Test assume controller có business logic             | ☐     | G-1       |
| AP-AI-004 | Layer Violation          | Test verify controller gọi repository trực tiếp      | ☐     | G-4       |
| AP-AI-005 | Hallucinated Contract    | Test import `ModerationFacade` không có trong §8 TDS | ☐     | G-3       |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ----------- | ----- | ----- | ---------- | ------ |
| —           | —     | —     | —          | —      |
