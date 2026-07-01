# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-101: Resolve Report

**Document ID:** `CB-MOD-TEST-003`
**Version:** `1.0`
**Date:** `2026-07-01`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Winston (System Architect)`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending` *(N/A — Internal data, no PII)*
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- TDS: `04_Implement/UC101_ResolveReport/UC101_ResolveReport_TDS.md` (`CB-MOD-IMP-003`)
- SRS Section 3.2.2.3 (FS Table 69)
- Sibling (Draft, hard dependency — must implement first): `04_Implement/UC100_ModerateCommunityContent/` (`CB-MOD-IMP-002`)
- Sibling (Approved, read-only oracle): `04_Implement/UC99_ViewModerationQueue/` (`CB-MOD-IMP-001`)
- CLAUDE.md §3 Architecture Rules, §5 Delivery Rules

---

## CHANGELOG

| Ngày       | Người thực hiện    | Nội dung thay đổi                                                              |
| ---------- | -------------------- | ---------------------------------------------------------------------------------- |
| 2026-07-01 | AI Agent — Winston  | Tạo tài liệu lần đầu — Test-Spec cho UC-101 Resolve Report (Status=Draft)          |

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

| Field                     | Value                                                                                                                    |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-101`                                                                                                                |
| **Module**                | `Resolve Report — content (with cross-package reads of community)`                                                      |
| **Spec gốc**              | `CB-MOD-IMP-003`                                                                                                          |
| **Priority**              | `P0 — High, Regular` (per FS Table 69)                                                                                    |
| **Sprint**                | `Open` — not sourced; TDS does not assign one                                                                              |
| **Milestone**             | `Open`                                                                                                                    |
| **Data Classification**   | `Internal`                                                                                                                  |
| **Compliance Scope**      | `N/A`                                                                                                                      |
| **Upstream Dependencies** | `security (JWT)`, `content (ContentReport, ModerationAction, ModerationException, applyContentAction() shared primitive from UC-100)`, `community (CommunityQuestion, CommunityAnswer, QuestionStatus, AnswerStatus)`, `audit (AuditService)` |
| **Downstream Consumers**  | `UC-99 View Moderation Queue` (resolved/dismissed reports drop out of default PENDING filter), `UC-102 Warn or Suspend Account` (forward dependency — not yet built) |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **AI Assisted?**         | `Yes`                                                                                                                              |
| **Constraint Source**    | `CB-MOD-IMP-003 §17`, `ADR-001 §Decision`, `ADR-004 §Decision`, `ADR-005 §Decision`, `ADR-006 §Decision`                            |
| **Constraints Injected** | `C1 (RBAC)`, `C2 (reuse applyContentAction)`, `C3 (reportId set)`, `C4 (reject CONTENT action)`, `C5 (reject WARN/SUSPEND)`, `C6 (PENDING-only guard)`, `C7 (audit every outcome)`, `C8 (resolvedAt/assignedModeratorId set both branches)` |
| **Model**                | `claude-sonnet-5`                                                                                                                  |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                                                       |

---

## 2. Logic Issues Resolved

| #   | Spec gốc (sai / thiếu)                                                                                  | Thực tế (schema / policy)                                                                                                                  | Fix áp dụng trong test                                                                                       |
| --- | ----------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| L1  | Dossier mô tả UC-101 chỉ là "đóng report, gọi action primitive"                                            | `ContentReport.resolvedAt`/`assignedModeratorId` PHẢI set cho CẢ HAI nhánh (DISMISS lẫn RESOLVED) — không sourced rõ ràng, là design decision (ADR-001/§Mô tả) | Mọi happy-path test (DISMISS lẫn action) PHẢI assert `resolvedAt`/`assignedModeratorId` non-null sau khi resolve |
| L2  | UC-100 TDS ADR-004 gắn cờ `Open`: report `targetType=CONTENT` chưa có lối ra trong UC-100                  | UC-101 ADR-004 đóng khoảng trống: CONTENT chỉ chấp nhận `DISMISS`; "escalate" thật vẫn `Open` (không có `ReportStatus.ESCALATED`)             | Test PHẢI assert CONTENT+DISMISS thành công, CONTENT+(APPROVE/HIDE/LOCK) → `MOD-012`; KHÔNG test "escalate" (không tồn tại) |
| L3  | FS-3.2.2.3 liệt kê "warn"/"suspend" là outcome khả dụng ngay                                                | `ReportTargetType` không có `ACCOUNT`; không có service ghi `users.enabled`/`locked`; UC-102 chưa được xây dựng (ADR-005)                     | Test PHẢI assert `WARN`/`SUSPEND` → `MOD-013` (400), report KHÔNG bị mutate, KHÔNG tạo `ModerationAction`           |
| L4  | Không có nguồn nào quy định có cho phép resolve lại một report đã đóng hay không                            | Quyết định thiết kế (ADR-006, không phải sourced fact): PENDING-only guard, khác hẳn UC-100's idempotent-overwrite choice                    | Test PHẢI gắn `Oracle Source: ADR-006 (design decision, not a sourced fact)`; assert `MOD-011` (409) khi report không còn PENDING |
| L5  | FS-3.2.2.3 liệt kê "label" là một outcome                                                                   | Không có `ModerationActionType`/status nào đại diện "label" (ADR-007, mirror UC-100 ADR-005)                                                 | Không viết test cho "label" — ghi nhận `Open`, không test hành vi không tồn tại                                     |
| L6  | UC-100 §6.4 matrix (LOCK invalid trên ANSWER) — câu hỏi: có áp dụng lại y nguyên cho UC-101 không?           | Có — UC-101 ADR-001 tái sử dụng `applyContentAction()` (cùng matrix), không định nghĩa lại                                                    | Test PHẢI assert UC-101's LOCK-trên-ANSWER case cũng trả về `MOD-008` (không phải mã lỗi mới)                       |
| L7  | Không rõ ModerationAction tạo qua UC-101 có khác UC-100 ở field nào                                         | Khác biệt DUY NHẤT: `reportId` ≠ null (UC-100 luôn null) — mọi field khác giống hệt logic UC-100                                              | Test PHẢI assert `reportId == report.getId()` cho mọi `ModerationAction` tạo qua UC-101 (regression guard, đối xứng với UC-100's MOD-TC-111) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC-101 Resolve Report bao gồm các layer:
├── Controller (ModerationController.resolveReport() — mock service, @WebMvcTest)
├── Service (ModerationServiceImpl.resolveReport() — mock ContentReportRepository +
│   mock applyContentAction() dependencies (Community repos, ModerationActionRepository) + mock
│   audit, Mockito) — NOTE: depends on UC-100's applyContentAction() refactor (ADR-001) already
│   being in place; this Test-Spec mocks at the repository boundary, not at applyContentAction()
│   itself, so the shared matrix logic (MOD-007/MOD-008/MOD-010) is exercised end-to-end, not stubbed.
├── Repository (ContentReportRepository.findById/save — existing, no new finder methods)
└── Integration (Full API flow — MockMvc + Testcontainers PostgreSQL)
```

### TDS-02 — Test Basis

| Source                                  | Items Derived                                                                                 |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| `SRS 3.2.2.3` (Table 69)                  | "Reviews a report and decides whether to keep, label, hide, warn, or suspend" — DISMISS/APPROVE/HIDE/LOCK supported v1; label/warn/suspend gapped (ADR-007/ADR-005) |
| `TDS CB-MOD-IMP-003 ADR-001`              | Report-centric orchestration reuses UC-100's `applyContentAction()` primitive; `reportId` propagated |
| `TDS CB-MOD-IMP-003 ADR-002`              | `@PreAuthorize("hasRole('MODERATOR')")` at controller (mirror UC-100)                              |
| `TDS CB-MOD-IMP-003 ADR-003`              | `AuditService.log(MODERATION_ACTION, ...)` after EVERY successful outcome, including DISMISS       |
| `TDS CB-MOD-IMP-003 ADR-004`              | `targetType=CONTENT` → DISMISS-only; APPROVE/HIDE/LOCK rejected (`MOD-012`)                        |
| `TDS CB-MOD-IMP-003 ADR-005`              | `WARN`/`SUSPEND` rejected v1 (`MOD-013`) — forward dependency on UC-102                            |
| `TDS CB-MOD-IMP-003 ADR-006`              | PENDING-only transition guard — re-resolution rejected (`MOD-011`, 409)                            |
| `TDS CB-MOD-IMP-002 §6.4` (UC-100, reused)| Action–targetType compatibility matrix (LOCK invalid for ANSWER → `MOD-008`)                       |
| `content/entity/ReportStatus.java`        | PENDING, RESOLVED, DISMISSED — oracle for state transitions                                        |
| `content/entity/ReportTargetType.java`    | QUESTION, ANSWER, CONTENT — oracle for ADR-004 scope check                                          |
| `content/exception/GlobalExceptionHandler.java` | Real 403 code = `ACCESS_DENIED`; real 401 = bodiless — oracle for security tests, same as UC-100 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                          | Coverage Item                                            | Test Cases       |
| ------------- | --------------------------------------------------------------------------- | -------------------------------------------------------------- | ------------------- |
| TC-COND-001  | DISMISS một report PENDING → status DISMISSED, không tạo ModerationAction  | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-101`        |
| TC-COND-002  | HIDE một report (targetType=QUESTION) → status RESOLVED + action reportId đúng | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-102`        |
| TC-COND-003  | APPROVE một report (targetType=ANSWER) → tương tự                          | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-103`        |
| TC-COND-004  | LOCK một report (targetType=QUESTION) → status LOCKED                      | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-104`        |
| TC-COND-005  | LOCK trên report targetType=ANSWER → MOD-008 (reused UC-100 matrix)        | `ModerationServiceImpl.resolveReport()` → `applyContentAction()` | `RES-TC-105`        |
| TC-COND-006  | targetType=CONTENT + outcome ∈ {APPROVE,HIDE,LOCK} → MOD-012               | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-106`        |
| TC-COND-007  | targetType=CONTENT + outcome=DISMISS → vẫn thành công                      | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-107`        |
| TC-COND-008  | outcome ∈ {WARN, SUSPEND} (mọi targetType) → MOD-013                       | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-108`        |
| TC-COND-009  | reportId không tồn tại → MOD-003 (404)                                     | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-109`        |
| TC-COND-010  | report đã RESOLVED/DISMISSED (không PENDING) → MOD-011 (409)               | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-110`        |
| TC-COND-011  | HIDE/LOCK thiếu reason → MOD-010 (reused)                                  | `ModerationServiceImpl.resolveReport()` → `applyContentAction()` | `RES-TC-111`        |
| TC-COND-012  | DISMISS không cần reason → vẫn thành công                                  | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-112`        |
| TC-COND-013  | `ModerationAction.reportId` luôn = report.id cho outcome hành động         | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-113`        |
| TC-COND-014  | `resolvedAt`/`assignedModeratorId` set cho nhánh DISMISS                   | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-114`        |
| TC-COND-015  | `resolvedAt`/`assignedModeratorId` set cho nhánh RESOLVED                  | `ModerationServiceImpl.resolveReport()`                         | `RES-TC-115`        |
| TC-COND-016  | `AuditService.log(MODERATION_ACTION,...)` gọi đúng 1 lần cho DISMISS       | `AuditService` mock verify                                       | `RES-TC-116`        |
| TC-COND-017  | `AuditService.log(MODERATION_ACTION,...)` gọi đúng 1 lần cho action outcome | `AuditService` mock verify                                       | `RES-TC-117`        |
| TC-COND-018  | report.targetId không tồn tại (stale) khi outcome=action → MOD-007 (reused) | `ModerationServiceImpl.resolveReport()` → `applyContentAction()` | `RES-TC-118`        |
| TC-COND-019  | Non-MODERATOR bị 403 ACCESS_DENIED                                         | `@PreAuthorize` Spring Security                                  | `RES-TC-119`        |
| TC-COND-020  | SYSTEM_ADMIN không có quyền ngầm — cũng bị 403                             | `@PreAuthorize` Spring Security                                  | `RES-TC-120`        |
| TC-COND-021  | CONTENT_ADMIN bị 403 (kể cả khi report targetType=CONTENT)                 | `@PreAuthorize` Spring Security                                  | `RES-TC-121`        |
| TC-COND-022  | Request không có JWT → 401, body rỗng                                      | `HttpStatusEntryPoint`                                            | `RES-TC-122`        |
| TC-COND-023  | Missing required field (`outcome=null`) → 400 MOD-001                      | `@Valid` bean validation                                          | `RES-TC-123`        |
| TC-COND-024  | Unexpected exception → 500 INTERNAL_ERROR (not dead-code MOD-005)          | `GlobalExceptionHandler.handleGeneric()`                          | `RES-TC-124`        |
| TC-COND-025  | Full integration flow DISMISS                                              | Testcontainers integration                                       | `RES-TC-INT-001`    |
| TC-COND-026  | Full integration flow HIDE — reportId linkage in DB                        | Testcontainers integration                                       | `RES-TC-INT-002`    |
| TC-COND-027  | Rollback khi lỗi giữa chừng (atomicity)                                    | Testcontainers integration                                       | `RES-TC-INT-003`    |
| TC-COND-028  | Race condition — 2 lệnh gọi resolve cùng reportId → đúng 1 thành công      | Testcontainers integration                                       | `RES-TC-INT-004`    |
| TC-COND-029  | SQL injection trong `reason` field                                         | Parameterized query / JPA                                        | `RES-TC-SEC-001`    |

### TDS-04 — Test Techniques

| Technique                | Applied To                                                | Rationale                                                                  |
| --------------------------- | -------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| Equivalence Partitioning  | `outcome` × `targetType` combinations                            | 6 outcomes × 3 targetTypes, partitioned into valid/MOD-012/MOD-013/MOD-008      |
| Boundary Value Analysis   | `report.status` (PENDING vs RESOLVED vs DISMISSED)               | MOD-011 boundary — only PENDING proceeds                                          |
| State Transition Testing  | `ReportStatus` PENDING → {RESOLVED, DISMISSED}, terminal states  | Verify no transition out of RESOLVED/DISMISSED is possible (ADR-006)              |
| Error Guessing            | SQL injection in `reason`, JWT tampering, unknown `reportId`, race condition | Security + robustness vectors                                                     |
| Regression (cross-UC)     | UC-100's §6.4 matrix reused, not redefined                       | Guards against UC-101 silently diverging from UC-100's validation rules           |

### TDS-05 — Test Data Requirements

| Fixture ID | Type    | Value / Logic                                                                                      | Mục đích                                  |
| ----------- | -------- | -------------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| `FX-201`   | DB seed | `ContentReport{id: R1, status: PENDING, targetType: QUESTION, targetId: Q1}`                              | HIDE/LOCK happy path on QUESTION                |
| `FX-202`   | DB seed | `ContentReport{id: R2, status: PENDING, targetType: ANSWER, targetId: A1}`                                | APPROVE happy path on ANSWER, LOCK rejection    |
| `FX-203`   | DB seed | `ContentReport{id: R3, status: PENDING, targetType: CONTENT, targetId: C1}`                               | CONTENT DISMISS-only test (ADR-004)             |
| `FX-204`   | DB seed | `ContentReport{id: R4, status: RESOLVED, resolvedAt: <past>, assignedModeratorId: <other-mod>}`           | Re-resolution guard test (MOD-011)              |
| `FX-205`   | DB seed | `ContentReport{id: R5, status: DISMISSED, ...}`                                                            | Re-resolution guard test (MOD-011), dismissed variant |
| `FX-206`   | DB seed | `ContentReport{id: R6, status: PENDING, targetType: QUESTION, targetId: <unknown-question-uuid>}`         | MOD-007 — stale targetId test                   |
| `FX-101`/`FX-102`/`FX-103`/`FX-104` | DB seed | Reused from UC-100 Test-Spec (`CommunityQuestion`/`CommunityAnswer` fixtures)               | Underlying target entities for action outcomes  |
| `FX-106`   | JWT     | `{sub: "<uuid>", role: "ROLE_MODERATOR"}` (reused from UC-100)                                            | Auth happy path                                  |
| `FX-107`   | JWT     | `{sub: "<uuid>", role: "ROLE_MOTHER"}` (reused)                                                            | Auth failure (403)                               |
| `FX-108`   | JWT     | `{sub: "<uuid>", role: "ROLE_SYSTEM_ADMIN"}` (reused)                                                      | Auth failure (403 — no implicit superuser)       |
| `FX-109`   | none    | No `Authorization` header (reused)                                                                          | Auth failure (401, bodiless)                     |
| `FX-110`   | JWT     | `{sub: "<uuid>", role: "ROLE_CONTENT_ADMIN"}` (reused)                                                      | Auth failure (403 — parity check)                |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// === RES-TC Props Isolation Pattern ===
// Đặt ở đầu test class — mỗi @Test dùng factory method, không share mutable state

class ResolveReportTestFactory {

    static final UUID MODERATOR_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID REPORT_ID_QUESTION = UUID.fromString("ee000000-0000-0000-0000-000000000001");
    static final UUID REPORT_ID_ANSWER   = UUID.fromString("ee000000-0000-0000-0000-000000000002");
    static final UUID REPORT_ID_CONTENT  = UUID.fromString("ee000000-0000-0000-0000-000000000003");
    static final UUID QUESTION_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    static final UUID ANSWER_ID   = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    static final UUID CONTENT_ID  = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    static ContentReport makeReport(UUID id, ReportTargetType targetType, UUID targetId,
                                     ReportStatus status, Consumer<ContentReport> overrides) {
        ContentReport r = ContentReport.builder()
                .id(id)
                .targetId(targetId)
                .targetType(targetType)
                .status(status)
                .category("INAPPROPRIATE_CONTENT")
                .description("Reported by community member")
                .reporterUserId(UUID.randomUUID())
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        overrides.accept(r);
        return r;
    }

    static ResolveReportRequest makeRequest(ResolutionOutcome outcome, String reason) {
        return new ResolveReportRequest(outcome, reason);
    }
}
```

---

### RES-TC-101 — DISMISS một report PENDING → status DISMISSED, không tạo ModerationAction

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport(reportId, request, principal)`
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS CB-MOD-IMP-003 §1 Mô tả`, `ADR-001 §Decision`, `BR-MOD-010`

**Preconditions:**
- `ContentReportRepository` mock: `findById(REPORT_ID_QUESTION)` trả về `FX-201` (status=PENDING)
- `ModerationActionRepository`, `AuditService` mock

**Test Steps:**
1. Arrange: request = `{outcome: DISMISS, reason: "Không vi phạm chính sách"}`
2. Act: `service.resolveReport(REPORT_ID_QUESTION, request, principal)`
3. Assert kết quả

**Expected Result (PASS):**
- `response.reportStatus()` = `DISMISSED`
- `response.actionId()` = `null`, `response.actionType()` = `null`, `response.resultingStatus()` = `null`
- `response.resolvedByModeratorId()` = `MODERATOR_ID`, `response.resolvedAt()` non-null
- `contentReportRepository.save(...)` được gọi 1 lần với entity có `status == ReportStatus.DISMISSED`, `resolvedAt` non-null, `assignedModeratorId == MODERATOR_ID`
- `moderationActionRepository.save(...)` **không bao giờ được gọi** (verify no interaction)
- `auditService.log(MODERATION_ACTION, MODERATOR_ID, "QUESTION", QUESTION_ID.toString(), ...)` được gọi 1 lần

**Expected Result (FAIL):**
- `ModerationAction` bị tạo cho nhánh DISMISS → vi phạm BR-MOD-010
- `resolvedAt`/`assignedModeratorId` không được set → vi phạm BR-MOD-009/ADR-001

**Current Status:** 🔴 Not written

---

### RES-TC-102 — HIDE một report (targetType=QUESTION) → status RESOLVED + action reportId đúng

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()`
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-001 §Decision (reuse applyContentAction)`, `TDS CB-MOD-IMP-002 §6.4 matrix (reused)`

**Preconditions:**
- `ContentReportRepository.findById(REPORT_ID_QUESTION)` trả về `FX-201` (status=PENDING, targetType=QUESTION, targetId=QUESTION_ID)
- `CommunityQuestionRepository.findById(QUESTION_ID)` trả về `FX-101` (status=PENDING per UC-100 fixture)

**Test Steps:**
1. Arrange: request = `{outcome: HIDE, reason: "Nội dung chứa tư vấn y tế sai lệch"}`
2. Act: `service.resolveReport(REPORT_ID_QUESTION, request, principal)`

**Expected Result (PASS):**
- `response.reportStatus()` = `RESOLVED`
- `response.actionId()` non-null, `response.actionType()` = `HIDE`, `response.resultingStatus()` = `"HIDDEN"`
- Saved `CommunityQuestion.status == QuestionStatus.HIDDEN`
- Saved `ModerationAction.reportId == REPORT_ID_QUESTION` (≠ null — **the defining difference from UC-100**)
- Saved `ContentReport.status == RESOLVED`, `resolvedAt`/`assignedModeratorId` set

**Current Status:** 🔴 Not written
**Implementation Note:** This is the canonical regression guard distinguishing UC-101 from UC-100
(`reportId` populated vs null) — see Logic Issue L7.

---

### RES-TC-103 — APPROVE một report (targetType=ANSWER) → status RESOLVED

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()`
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `community/entity/AnswerStatus.java`, UC-100 §6.4 matrix (reused)

**Preconditions:**
- `ContentReportRepository.findById(REPORT_ID_ANSWER)` trả về `FX-202` (status=PENDING, targetType=ANSWER, targetId=ANSWER_ID)
- `CommunityAnswerRepository.findById(ANSWER_ID)` trả về `FX-103` (status=PENDING per UC-100 fixture)

**Test Steps:**
1. Arrange: request = `{outcome: APPROVE, reason: null}`
2. Act + Assert

**Expected Result (PASS):**
- `response.resultingStatus()` = `"APPROVED"`, saved `AnswerStatus.APPROVED`
- `ModerationAction.reportId == REPORT_ID_ANSWER`, `reason == null` (APPROVE optional, ADR-006 of UC-100 reused)

**Current Status:** 🔴 Not written

---

### RES-TC-104 — LOCK một report (targetType=QUESTION) → status LOCKED

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()`
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `community/entity/QuestionStatus.java` (LOCKED exists)

**Preconditions:** `ContentReportRepository.findById(REPORT_ID_QUESTION)` trả về `FX-201`; `CommunityQuestionRepository.findById(QUESTION_ID)` trả về `FX-102` (status=APPROVED)

**Test Steps:**
1. Arrange: request = `{outcome: LOCK, reason: "Tranh cãi kéo dài"}`
2. Act + Assert: `resultingStatus == "LOCKED"`, `ModerationAction.reportId == REPORT_ID_QUESTION`

**Current Status:** 🔴 Not written

---

### RES-TC-105 — LOCK trên report targetType=ANSWER → 400 MOD-008 (reused UC-100 matrix)

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` → shared `applyContentAction()`
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `community/entity/AnswerStatus.java` (no LOCKED), `TDS CB-MOD-IMP-002 ADR-004 §6.4 matrix (reused via ADR-001 of this TDS)`

**Preconditions:** `ContentReportRepository.findById(REPORT_ID_ANSWER)` trả về `FX-202` (targetType=ANSWER)

**Test Steps:**
1. Arrange: request = `{outcome: LOCK, reason: "test"}`
2. Act: `service.resolveReport(REPORT_ID_ANSWER, request, principal)`

**Expected Result (PASS):**
- Throws `ModerationException` `code == "MOD-008"`, `httpStatus == 400`
- `contentReportRepository.save(...)` **never called** (report not mutated — fail before commit, verify report stays PENDING if re-fetched)
- `communityAnswerRepository.findById(...)` may or may not be called depending on validation order inside the shared primitive — assert per the SAME order documented in UC-100's `MOD-TC-105` (validate before lookup, fail fast)

**Expected Result (FAIL):** No exception thrown, or report gets marked RESOLVED despite the rejected action.

**Current Status:** 🔴 Not written
**Implementation Note:** This test doubles as a cross-UC regression guard — it proves UC-101 reuses
UC-100's exact matrix instead of redefining it (Logic Issue L6, ADR-002 of this Test-Spec section 2).

---

### RES-TC-106 — targetType=CONTENT + outcome ∈ {APPROVE, HIDE, LOCK} → 400 MOD-012

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — CONTENT scope boundary (ADR-004)
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS ADR-004 §Decision (Option A accepted)` — **design decision, flagged Open for human
review**, this is the central gap UC-100's TDS explicitly deferred to UC-101

**Preconditions:** `ContentReportRepository.findById(REPORT_ID_CONTENT)` trả về `FX-203` (status=PENDING, targetType=CONTENT)

**Test Steps:**
1. Arrange: request = `{outcome: APPROVE, reason: null}`
2. Act + Assert: throws `ModerationException` code `MOD-012`, `httpStatus == 400`
3. Repeat for `outcome = HIDE`, `LOCK` — same rejection

**Expected Result (PASS):**
- All 3 outcomes on `targetType=CONTENT` rejected with `MOD-012`
- `ContentItem`/`AdminContentService` never touched (verify no interaction)
- `report.status` remains `PENDING` after the failed call (re-verify by capturing no `save()` call, or
  by re-fetching in the integration-level equivalent `RES-TC-INT-*`)

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ Most reviewer-sensitive test in this spec — encodes ADR-004's accepted
DISMISS-only resolution for CONTENT reports. If Product later approves a true "escalate to Content Admin"
mechanism (ADR-004 Option B), this test must be rewritten, not deleted silently.

---

### RES-TC-107 — targetType=CONTENT + outcome=DISMISS → vẫn thành công

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — CONTENT DISMISS path
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-004 §Decision` — DISMISS is the only valid outcome for CONTENT in v1

**Preconditions:** `ContentReportRepository.findById(REPORT_ID_CONTENT)` trả về `FX-203`

**Test Steps:**
1. Arrange: request = `{outcome: DISMISS, reason: "Not a community-content violation"}`
2. Act + Assert: `response.reportStatus() == DISMISSED`, no exception, `ContentItem` never touched

**Current Status:** 🔴 Not written

---

### RES-TC-108 — outcome ∈ {WARN, SUSPEND} (mọi targetType) → 400 MOD-013

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — account-action forward dependency (ADR-005)
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS ADR-005 §Decision` — forward dependency on UC-102, not yet built

**Preconditions:** `ContentReportRepository.findById(REPORT_ID_QUESTION)` trả về `FX-201`

**Test Steps:**
1. Arrange: request = `{outcome: WARN, reason: "First offense"}`
2. Act + Assert: throws `ModerationException` code `MOD-013`, `httpStatus == 400`
3. Repeat for `outcome = SUSPEND`
4. Repeat both sub-cases against `FX-202` (ANSWER) and `FX-203` (CONTENT) to confirm rejection is
   independent of `targetType`

**Expected Result (PASS):**
- All combinations rejected with `MOD-013`
- `contentReportRepository.save(...)` never called — report untouched, remains `PENDING`
- `moderationActionRepository.save(...)` never called

**Expected Result (FAIL):** Any mutation to `users.enabled`/`users.locked` or `ContentReport`/
`ModerationAction` occurs — would mean someone implemented ad-hoc account-suspension logic not backed
by a UC-102 TDS, violating ADR-005/CASE-2.0 AP-AI-003.

**Current Status:** 🔴 Not written
**Implementation Note:** This test is a deliberate "scope creep" tripwire — if a future implementer adds
a `UserRepository` dependency to `ModerationServiceImpl.resolveReport()` to "make WARN/SUSPEND work,"
this test must be revisited (and a UC-102 TDS must exist) before being relaxed.

---

### RES-TC-109 — reportId không tồn tại → 404 MOD-003

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — report lookup
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `TDS §10 Error Codes`, `TDS §8.1 @throws MOD-003`, UC-99 TDS (reserved this code)

**Preconditions:** `ContentReportRepository.findById(<unknown-uuid>)` trả về `Optional.empty()`

**Test Steps:**
1. Arrange: `service.resolveReport(<unknown-uuid>, {outcome: DISMISS}, principal)`
2. Act + Assert: throws `ModerationException` code `MOD-003`, `httpStatus == 404`

**Current Status:** 🔴 Not written
**Implementation Note:** First real implementation of `MOD-003` — reserved since UC-99's TDS but never
wired into code until this UC.

---

### RES-TC-110 — report đã RESOLVED/DISMISSED → 409 MOD-011 (re-resolution guard)

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — PENDING-only guard (ADR-006)
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `TDS ADR-006 §Decision` — **explicit design decision, not a sourced FS/BR fact**

**Preconditions:**
- Sub-case a: `ContentReportRepository.findById(R4)` trả về `FX-204` (status=RESOLVED)
- Sub-case b: `ContentReportRepository.findById(R5)` trả về `FX-205` (status=DISMISSED)

**Test Steps:**
1. Arrange (a): request = `{outcome: DISMISS}` → Act + Assert: `MOD-011`, 409
2. Arrange (b): request = `{outcome: HIDE, reason: "test"}` → Act + Assert: `MOD-011`, 409

**Expected Result (PASS):** Both sub-cases throw `MOD-011`; `report.status`/`resolvedAt`/
`assignedModeratorId` remain unchanged from the original (not overwritten by the second attempt — the
core attribution-integrity guarantee of ADR-006).

**Expected Result (FAIL):** Report's `resolvedAt`/`assignedModeratorId` silently overwritten — would
indicate ADR-006 guard was bypassed.

**Current Status:** 🔴 Not written

---

### RES-TC-111 — HIDE/LOCK thiếu reason → 400 MOD-010 (reused)

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` → shared `applyContentAction()`
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `TDS CB-MOD-IMP-002 ADR-006 §Decision (reused)` — design decision, not sourced fact

**Preconditions:** `ContentReportRepository.findById(REPORT_ID_QUESTION)` trả về `FX-201`

**Test Steps:**
1. Sub-case a: request = `{outcome: HIDE, reason: null}` → Act + Assert: `MOD-010`
2. Sub-case b: request = `{outcome: HIDE, reason: "   "}` (blank) → Act + Assert: `MOD-010`
3. Sub-case c: request = `{outcome: LOCK, reason: null}` → Act + Assert: `MOD-010`

**Expected Result (PASS):** All 3 throw `MOD-010`, 400; report remains `PENDING` (not mutated).

**Current Status:** 🔴 Not written

---

### RES-TC-112 — DISMISS không cần reason → vẫn thành công

**Severity:** `LOW`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — DISMISS reason optionality
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `TDS §8.3 DTO note` — no business rule requires `reason` for DISMISS

**Preconditions:** `ContentReportRepository.findById(REPORT_ID_QUESTION)` trả về `FX-201`

**Test Steps:**
1. Arrange: request = `{outcome: DISMISS, reason: null}`
2. Act + Assert: no exception, `response.reportStatus() == DISMISSED`

**Current Status:** 🔴 Not written

---

### RES-TC-113 — `ModerationAction.reportId` luôn = report.id cho outcome hành động

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — UC-100/UC-101 separation (mirror image of UC-100's `MOD-TC-111`)
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-013`
**Oracle Source:** `BR-MOD-011`, `ADR-001 §Decision`

**Test Steps:**
1. Arrange: valid HIDE request against `FX-201`
2. Act: `service.resolveReport(REPORT_ID_QUESTION, request, principal)`
3. Assert: capture argument passed to `moderationActionRepository.save(...)`

**Expected Result (PASS):** `capturedAction.getReportId() == REPORT_ID_QUESTION` — **always** for outcomes
that produce a `ModerationAction` via this endpoint (the inverse invariant of UC-100's `MOD-TC-111`, which
asserts `reportId == null`).

**Current Status:** 🔴 Not written

---

### RES-TC-114 — `resolvedAt`/`assignedModeratorId` set cho nhánh DISMISS

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — completion metadata
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `BR-MOD-009`, `ADR-001 §Decision` — explicit assumption flagged in TDS §1 Mô tả

**Test Steps:** Same as `RES-TC-101`, focused assertion on `resolvedAt`/`assignedModeratorId`.

**Expected Result (PASS):** Both non-null and correctly populated (`assignedModeratorId == MODERATOR_ID`)
on the saved `ContentReport`, even though `status == DISMISSED` (not `RESOLVED`).

**Current Status:** 🔴 Not written

---

### RES-TC-115 — `resolvedAt`/`assignedModeratorId` set cho nhánh RESOLVED

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — completion metadata
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `BR-MOD-009`

**Test Steps:** Same as `RES-TC-102`, focused assertion.

**Expected Result (PASS):** `resolvedAt`/`assignedModeratorId` non-null on saved `ContentReport` with
`status == RESOLVED`.

**Current Status:** 🔴 Not written

---

### RES-TC-116 — `AuditService.log()` gọi đúng 1 lần cho DISMISS

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — audit side effect (ADR-003)
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-016`
**Oracle Source:** `ADR-003 §Decision` — audit log applies to DISMISS too, not just content actions

**Test Steps:**
1. Arrange: valid DISMISS request
2. Act: `service.resolveReport(...)`
3. Assert: `verify(auditService, times(1)).log(eq(AuditAction.MODERATION_ACTION), eq(MODERATOR_ID), any(), any(), any())`

**Expected Result (PASS):** Exactly 1 invocation.
**Expected Result (FAIL):** 0 invocations — would mean DISMISS decisions are silently un-audited
(accountability gap flagged explicitly by ADR-003).

**Current Status:** 🔴 Not written

---

### RES-TC-117 — `AuditService.log()` gọi đúng 1 lần cho outcome hành động

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — audit side effect
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `ADR-003 §Decision`

**Test Steps:** Same as `RES-TC-102`, focused on `auditService` verify — exactly 1 call, not 2 (i.e. the
shared `applyContentAction()` primitive must not ALSO independently call `auditService.log()` — only the
orchestrating `resolveReport()` should, to avoid double-logging the same decision).

**Expected Result (PASS):** Exactly 1 invocation total.
**Expected Result (FAIL):** 2 invocations (one from `applyContentAction()`, one from `resolveReport()`) —
would indicate the shared-primitive refactor (ADR-001) leaked UC-100's own audit call into the UC-101
orchestration path; this must be resolved by having `applyContentAction()` NOT audit-log internally,
leaving that responsibility to the caller (`moderateContent()` for UC-100, `resolveReport()` for UC-101).

**Current Status:** 🔴 Not written
**Implementation Note:** ⚠️ This test enforces a specific internal design constraint on the ADR-001
refactor — implementer must move the `auditService.log()` call OUT of the shared primitive and into each
caller, or risk double-audit-logging for UC-101.

---

### RES-TC-118 — report.targetId không tồn tại (stale) khi outcome=action → 404 MOD-007 (reused)

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` → shared `applyContentAction()`
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-018`
**Oracle Source:** `TDS CB-MOD-IMP-002 §10 (MOD-007, reused)` — handles the case where the reported
question/answer was hard-deleted after the report was filed (no FK/cascade-delete behavior verified;
flagged as a realistic edge case given `community_questions`/`community_answers` have no documented
soft-delete in the dossier)

**Preconditions:** `ContentReportRepository.findById(R6)` trả về `FX-206` (targetType=QUESTION, targetId=
`<unknown-question-uuid>`); `CommunityQuestionRepository.findById(<unknown-question-uuid>)` trả về
`Optional.empty()`

**Test Steps:**
1. Arrange: request = `{outcome: APPROVE}`
2. Act + Assert: throws `ModerationException` code `MOD-007`, `httpStatus == 404`; `report.status` remains
   `PENDING` (not mutated — fail before the report-level save)

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

### RES-TC-119 — Non-MODERATOR bị 403 `ACCESS_DENIED`

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-285 — Improper Authorization`
**Feature Under Test:** `ModerationController.resolveReport()` — `@PreAuthorize`
**Test File:** `src/test/java/com/carebridge/backend/security/ResolveReportControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `ADR-002`, `GlobalExceptionHandler.java` line ~284-287 (reused finding from UC-100)

**Preconditions:** JWT với role `ROLE_MOTHER` (FX-107)

**Test Steps:** `POST /api/v1/admin/moderation/reports/{reportId}/resolve` với MOTHER JWT, valid body

**Expected Result (PASS — hệ thống an toàn):**
- `response.status == 403`, `response.body.error.code == "ACCESS_DENIED"` *(NOT `"MOD-004"`)*
- Không có mutation nào xảy ra trên `content_reports`

**Current Status:** 🔴 Not written

---

### RES-TC-120 — SYSTEM_ADMIN không có quyền ngầm — cũng bị 403

**Severity:** `HIGH`
**Feature Under Test:** `ModerationController.resolveReport()` — verifies no `RoleHierarchy`
**Test File:** `src/test/java/com/carebridge/backend/security/ResolveReportControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-020`
**Oracle Source:** Reused finding — `grep -rln "RoleHierarchy" .` returns zero matches

**Preconditions:** JWT với role `ROLE_SYSTEM_ADMIN` (FX-108)

**Test Steps:** `POST /api/v1/admin/moderation/reports/{reportId}/resolve` với SYSTEM_ADMIN JWT

**Expected Result (PASS):** `response.status == 403`, `error.code == "ACCESS_DENIED"`.

**Current Status:** 🔴 Not written

---

### RES-TC-121 — CONTENT_ADMIN bị 403 (kể cả khi report targetType=CONTENT)

**Severity:** `HIGH`
**Feature Under Test:** `@PreAuthorize ROLE_MODERATOR` — role parity check, reinforces ADR-004's CONTENT boundary
**Test File:** `src/test/java/com/carebridge/backend/security/ResolveReportControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-021`
**Oracle Source:** `TDS §16 Auth Matrix — CONTENT_ADMIN = ❌`, ADR-004 note ("a true Content-Admin-facing
report queue/resolution path does not exist")

**Preconditions:** JWT với role `ROLE_CONTENT_ADMIN` (FX-110); request targets `REPORT_ID_CONTENT` (FX-203)

**Test Steps:** `POST /api/v1/admin/moderation/reports/{REPORT_ID_CONTENT}/resolve` với CONTENT_ADMIN JWT,
`{outcome: DISMISS}`

**Expected Result (PASS):** `response.status == 403`, `error.code == "ACCESS_DENIED"` — **even though** the
report's `targetType` is `CONTENT`, `CONTENT_ADMIN` still has no access to this endpoint at all (UC-101 is
exclusively a MODERATOR tool; ADR-004 explicitly notes no Content-Admin-facing variant exists yet).

**Current Status:** 🔴 Not written

---

### RES-TC-122 — Request không có JWT → 401, body rỗng

**Severity:** `CRITICAL`
**Feature Under Test:** JWT authentication entry point
**Test File:** `src/test/java/com/carebridge/backend/security/ResolveReportControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-022`
**Oracle Source:** `SecurityConfig.java` `HttpStatusEntryPoint` (reused finding from UC-100)

**Test Steps:** `POST /api/v1/admin/moderation/reports/{reportId}/resolve` không có `Authorization` header (FX-109)

**Expected Result (PASS):** `response.status == 401`. Body MAY be empty — test MUST NOT assert
`error.code == "IAM-001"`/`"MOD-006"`.

**Current Status:** 🔴 Not written

---

### RES-TC-123 — Missing required field (`outcome=null`) → 400 MOD-001

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationController.resolveReport()` — `@Valid` bean validation
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-023`
**Oracle Source:** `TDS §9.2 Response — 400 Bad Request`, same drift noted in UC-100 §6.3/§9.2

**Test Steps:** `POST .../resolve` với body thiếu `outcome` (JSON: `{"reason": "..."}`), MODERATOR JWT

**Expected Result (PASS):** `response.status == 400`, error references `outcome`. Exact `error.code` is
`Open` per the same MOD-001 wiring gap UC-100 documented — assert `status==400` and field name as the
stable minimum oracle.

**Current Status:** 🔴 Not written
**Implementation Note:** Flagged `Open` deliberately — do not hard-code an unverified error code string.

---

### RES-TC-124 — Unexpected exception → 500 `INTERNAL_ERROR` (not `MOD-005`)

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationController.resolveReport()` / generic fallback handler
**Test File:** `src/test/java/com/carebridge/backend/moderation/ResolveReportControllerTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-024`
**Oracle Source:** Reused finding from UC-100 §10 — `MOD-005` is dead code; real fallback is
`GlobalExceptionHandler.handleGeneric()`

**Preconditions:** `ModerationService.resolveReport(...)` mock throws an unexpected `RuntimeException`, MODERATOR JWT valid

**Test Steps:** `POST .../resolve` with valid MODERATOR auth, service mock throws `RuntimeException("simulated failure")`

**Expected Result (PASS):** `response.status == 500`, `response.body.error.code == "INTERNAL_ERROR"` (NOT `"MOD-005"`).

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

### RES-TC-INT-001 — Full API flow DISMISS với real DB (Testcontainers)

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/admin/moderation/reports/{reportId}/resolve` — end to end (DISMISS)
**Test File:** `src/test/java/com/carebridge/backend/integration/ResolveReportIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-025`

**Preconditions:** PostgreSQL Testcontainer chạy, schema applied via Flyway (no new migration); seed `FX-201` via repository; MODERATOR JWT hợp lệ

**Test Steps:**
1. Seed `FX-201` (`ContentReport`, status=PENDING)
2. `POST .../resolve` `{outcome: DISMISS, reason: "No violation found"}` với MODERATOR JWT
3. Assert response 200
4. Re-fetch `content_reports` row by id directly from DB

**Expected Result (PASS):**
- Response: `reportStatus == "DISMISSED"`
- DB: `content_reports.status == 'DISMISSED'`, `resolved_at IS NOT NULL`, `assigned_moderator_id IS NOT NULL`
- DB: 0 new rows in `moderation_actions` for this `report_id`

**Current Status:** 🔴 Not written

---

### RES-TC-INT-002 — Full API flow HIDE — reportId linkage trong DB

**Severity:** `HIGH`
**Feature Under Test:** `POST /api/v1/admin/moderation/reports/{reportId}/resolve` — end to end (action outcome)
**Test File:** `src/test/java/com/carebridge/backend/integration/ResolveReportIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-026`

**Preconditions:** Seed `FX-201` (`ContentReport`) AND its underlying `FX-101`-equivalent `CommunityQuestion`
row in the same Testcontainer DB; MODERATOR JWT hợp lệ

**Test Steps:**
1. Seed report + question
2. `POST .../resolve` `{outcome: HIDE, reason: "Policy violation"}`
3. Assert response 200
4. Re-fetch `content_reports`, `community_questions`, `moderation_actions` directly

**Expected Result (PASS):**
- DB: `content_reports.status == 'RESOLVED'`, `resolved_at`/`assigned_moderator_id` set
- DB: `community_questions.status == 'HIDDEN'` for the seeded question
- DB: exactly 1 new row in `moderation_actions` with `report_id = FX-201.id` (NOT null — the defining
  difference from UC-100's equivalent integration test, which asserts `report_id IS NULL`)

**DB Assertion:**
```java
ContentReport report = contentReportRepository.findById(FX_201_ID).orElseThrow();
assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
assertThat(report.getResolvedAt()).isNotNull();
assertThat(report.getAssignedModeratorId()).isNotNull();

List<ModerationAction> actions = moderationActionRepository.findAll();
assertThat(actions).hasSize(1);
assertThat(actions.get(0).getReportId()).isEqualTo(FX_201_ID);
```

**Current Status:** 🔴 Not written

---

### RES-TC-INT-003 — Rollback khi lỗi giữa chừng (atomicity)

**Severity:** `CRITICAL`
**Feature Under Test:** Transaction boundary in `ModerationServiceImpl.resolveReport()`
**Test File:** `src/test/java/com/carebridge/backend/integration/ResolveReportIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-027`
**Oracle Source:** `ADR-001 §Decision (inherits UC-100 ADR-001 transactional discipline)`

**Preconditions:** Seed `FX-201` + underlying question; force a failure after the `ModerationAction` insert
but before the `ContentReport.save()` step (e.g. via a test-only repository wrapper or `AuditService` mock
that throws inside the same `@Transactional` boundary)

**Test Steps:**
1. Seed
2. Trigger `resolveReport()` with a forced downstream failure
3. Assert exception propagates
4. Re-fetch `content_reports`, `community_questions`, `moderation_actions` from a new transaction/session

**Expected Result (PASS):**
- DB: `content_reports.status` is **still `PENDING`**
- DB: `community_questions.status` is **still its original value** (not `HIDDEN`)
- DB: `moderation_actions` has **0** rows for this `report_id` — the entire chain rolled back together

**Expected Result (FAIL):** Any partial state (e.g. `ModerationAction` inserted but `ContentReport` still
`PENDING`, or vice versa) — violates the atomicity guarantee inherited from UC-100 ADR-001.

**Current Status:** 🔴 Not written

---

### RES-TC-INT-004 — Race condition — 2 lệnh gọi resolve cùng reportId → đúng 1 thành công

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.resolveReport()` — concurrency / re-resolution guard (ADR-006)
**Test File:** `src/test/java/com/carebridge/backend/integration/ResolveReportIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implem ent
**Condition Ref:** `TC-COND-028`
**Oracle Source:** `TDS §4.1 NFR Concurrency` — flagged `Open` (no optimistic locking; TOCTOU race possible
between `findById` and `save`, only the explicit status guard exists)

**Preconditions:** Seed `FX-201`; 2 MODERATOR JWTs (different `sub`) issuing near-simultaneous requests

**Test Steps:**
1. Seed `FX-201` (status=PENDING)
2. Fire 2 concurrent `POST .../resolve` requests with `{outcome: DISMISS}` against the SAME `reportId`,
   different moderator JWTs
3. Assert exactly 1 returns `200`, the other returns `409 MOD-011`

**Expected Result (PASS):** Exactly 1 success, 1 conflict. `assigned_moderator_id` in the DB matches
whichever moderator's request committed first (non-deterministic which one — test only asserts mutual
exclusivity, not which wins).

**Expected Result (FAIL):** Both succeed (double-resolution, violates ADR-006) OR both fail (guard too
aggressive, false negative).

**Current Status:** 🔴 Not written
**Implementation Note:** This test is `Open`/best-effort — true TOCTOU races are timing-dependent and may
require `@Transactional(isolation = Isolation.SERIALIZABLE)` or a DB-level unique constraint trick to
make deterministic in CI; if flaky, document as a known limitation rather than deleting the coverage
intent (ADR-006's concurrency caveat already flags this as `Open`).

---

### RES-TC-SEC-001 — SQL Injection trong `reason` field không ảnh hưởng DB

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `ModerationController` — `reason` field handling
**Test File:** `src/test/java/com/carebridge/backend/security/ResolveReportControllerSecurityTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-029`

**Test Steps:** `POST .../resolve` với `reason = "x'; DROP TABLE content_reports;--"`, MODERATOR JWT,
`outcome=HIDE`, targeting `FX-201`

**Expected Result (PASS):** Request xử lý bình thường (reason lưu nguyên văn — JPA parameterized query);
`content_reports`/`moderation_actions` tables vẫn tồn tại và intact.

**Expected Result (FAIL):** 500 error từ DB hoặc bảng bị xóa → injection được thực thi.

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                       | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| ----------------- | -------------------------------------------------- | ------------------ | -------------------- | ------------------- |
| `RES-TC-101`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-102`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-103`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-104`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-105`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-106`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-107`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-108`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-109`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-110`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-111`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-112`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-113`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-114`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-115`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-116`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-117`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-118`      | `ResolveReportServiceImplTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-119`      | `ResolveReportControllerSecurityTest.java`          | `[ ]`               | —                     | —                    |
| `RES-TC-120`      | `ResolveReportControllerSecurityTest.java`          | `[ ]`               | —                     | —                    |
| `RES-TC-121`      | `ResolveReportControllerSecurityTest.java`          | `[ ]`               | —                     | —                    |
| `RES-TC-122`      | `ResolveReportControllerSecurityTest.java`          | `[ ]`               | —                     | —                    |
| `RES-TC-123`      | `ResolveReportControllerTest.java`                  | `[ ]`               | —                     | —                    |
| `RES-TC-124`      | `ResolveReportControllerTest.java`                  | `[ ]`               | —                     | —                    |
| `RES-TC-INT-001`  | `ResolveReportIntegrationTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-INT-002`  | `ResolveReportIntegrationTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-INT-003`  | `ResolveReportIntegrationTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-INT-004`  | `ResolveReportIntegrationTest.java`                 | `[ ]`               | —                     | —                    |
| `RES-TC-SEC-001`  | `ResolveReportControllerSecurityTest.java`          | `[ ]`               | —                     | —                    |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// ModerationServiceImpl.java — Red Phase stub (added method only; getModerationQueue() unchanged/
// already GREEN from UC-99; moderateContent() / applyContentAction() must already be GREEN from
// UC-100 before this Red Phase begins — see §11.1 Prerequisites, hard dependency)
@Service
public class ModerationServiceImpl implements ModerationService {

    // ... existing getModerationQueue() from UC-99, moderateContent()/applyContentAction() from
    //     UC-100, already implemented ...

    @Override
    public ResolveReportResponse resolveReport(UUID reportId, ResolveReportRequest request, Principal principal) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```java
// ModerationController.java — Red Phase: endpoint not yet mapped
// (RES-TC-119/120/121/122 exercise @PreAuthorize which CAN be wired before the service logic exists —
//  same caveat as UC-100's Red Gate note: acceptable as long as the authorized-path tests
//  RES-TC-101..118 fail via the stub's UnsupportedOperationException)
```

**Red Gate Verification:**

| TC ID            | Stub Result                            | Expected         | Actual        | Root Cause (nếu PASS bất thường) |
| ----------------- | ------------------------------------------ | ------------------- | ---------------- | ------------------------------------ |
| `RES-TC-101`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `RES-TC-102`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `RES-TC-106`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `RES-TC-108`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `RES-TC-109`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `RES-TC-110`      | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `RES-TC-119`      | `@PreAuthorize not yet present → 404/405`   | 🔴 FAIL (no 403)     | ☐ FAIL ☐ PASS     | —                                     |
| `RES-TC-INT-001`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |
| `RES-TC-INT-003`  | `throw UnsupportedOperationException`       | 🔴 FAIL              | ☐ FAIL ☐ PASS     | —                                     |

**Red Gate Evidence:**
- Stub commit hash: `___` *(to be filled when implementation starts)*
- Tất cả FAIL? ☐ Yes → GATE-2 PASS (T2→T3) → tiếp tục implement
- Log file: `Open` — to be filled during implementation

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-MOD-IMP-003` đã review và **đặc biệt ADR-004 (CONTENT DISMISS-only), ADR-005 (WARN/SUSPEND
      forward dependency) và ADR-006 (re-resolution guard) đã được Tech Lead/Product xác nhận** (cả ba
      được đánh dấu rõ là design decisions, không phải sourced facts)
- [ ] **UC-100 (`CB-MOD-IMP-002`) đã đạt GREEN** (đặc biệt `applyContentAction()` shared primitive tồn
      tại và đã pass test) — hard prerequisite kỹ thuật cho UC-101 (ADR-001)
- [ ] Logic Issues (Section 2) đã được confirm với Tech Lead
- [ ] DB migration: không cần (xác nhận — không có schema delta cho UC-101)
- [ ] Test fixtures FX-201 đến FX-206 (+ reused FX-101..110 từ UC-100) đã chuẩn bị
- [ ] Spring Security test dependencies có sẵn (kế thừa từ UC-99/UC-100)

### Exit Criteria (DoD)

- [ ] `./mvnw test -Dtest=ResolveReportServiceImplTest` — tất cả test PASS
- [ ] `./mvnw test -Dtest=ResolveReportControllerTest` — tất cả test PASS
- [ ] `./mvnw test -Dtest=ResolveReportControllerSecurityTest` — tất cả test PASS
- [ ] `./mvnw verify -Dtest=ResolveReportIntegrationTest` — tất cả test PASS (Testcontainers)
- [ ] Test coverage: `ModerationServiceImpl.resolveReport()` ≥ 80% lines
- [ ] Không có business logic trong `ModerationController` (chỉ `@Valid` + delegate)
- [ ] RES-TC-119/120/121: Non-MODERATOR, SYSTEM_ADMIN, CONTENT_ADMIN đều nhận 403 `ACCESS_DENIED` —
      VERIFIED (CRITICAL security gate)
- [ ] RES-TC-INT-003: Atomicity rollback verified — VERIFIED (CRITICAL data-integrity gate)
- [ ] RES-TC-110: Re-resolution guard (`MOD-011`) verified — VERIFIED (CRITICAL — protects attribution
      integrity, ADR-006)
- [ ] RES-TC-106: CONTENT scope boundary (`MOD-012`) verified — VERIFIED (CRITICAL — closes the gap
      UC-100's TDS explicitly deferred)
- [ ] RES-TC-108: WARN/SUSPEND forward-dependency rejection (`MOD-013`) verified — VERIFIED (CRITICAL —
      prevents unspecced account-suspension logic from being smuggled in, ADR-005)

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] Red Gate (§5.1) — tất cả tests FAIL với `throw` stub trước khi implement
- [ ] Contract Existence — `./mvnw compile` không có lỗi symbol cho mọi class mới
      (`ResolveReportRequest`, `ResolveReportResponse`, `ResolutionOutcome`, 4 factory method mới trên
      `ModerationException`)
- [ ] Props Isolation — factory methods (`makeReport()`, `makeRequest()`) đảm bảo isolation, không có
      shared mutable `static` instance bị mutate giữa test
- [ ] Oracle Source — mọi expected value có comment trỏ về BR/ADR/file code cụ thể
- [ ] Shared-primitive verification (ADR-001) — `applyContentAction()` refactor từ UC-100 không làm
      regress bất kỳ test nào của UC-100's existing suite (chạy lại `ModerateContentServiceImplTest`
      sau refactor, phải vẫn 100% GREEN)

### Suspension Criteria

- ADR-004, ADR-005, hoặc ADR-006 chưa được Tech Lead/Product xác nhận
- UC-100 chưa đạt GREEN (hard dependency, ADR-001)
- Spring Security config chưa enable `@EnableMethodSecurity`
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/content/

# Không có migration để revert (UC-101 không thay đổi schema)

# CHÚ Ý: Nếu rollback xảy ra SAU KHI ADR-001's applyContentAction() refactor đã merge, đảm bảo UC-100's
# moderateContent() vẫn hoạt động đúng sau revert — chạy lại UC-100 test suite để xác nhận không bị
# regress bởi việc revert UC-101's changes trên cùng file ModerationServiceImpl.java.

# Test spec files được giữ nguyên (không rollback test spec) — gap vẫn OPEN nếu rollback xảy ra
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern             | Dấu hiệu trong TDD spec                                                                    | Check | Gate chặn |
| --------- | ------------------------- | ---------------------------------------------------------------------------------------------- | ------- | ----------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR-001/ADR-004/ADR-005/ADR-006                                                | `[x]`   | G-0         |
| AP-AI-002 | Green-from-Birth         | RES-TC-101..118 PASS với empty/throw stub (no real report/action mutation)                       | `[x]`   | G-2 ★       |
| AP-AI-003 | Implicit Decision        | Test giả định WARN/SUSPEND đã được implement (vd. mock `UserRepository`) mà không có UC-102 TDS  | `[x]`   | G-1         |
| AP-AI-003 | Implicit Decision        | Test giả định targetType=CONTENT có thể bị APPROVE/HIDE/LOCK mà không tham chiếu ADR-004          | `[x]`   | G-1         |
| AP-AI-004 | Layer Violation           | Test verify Controller gọi trực tiếp `ContentReportRepository`/`CommunityQuestionRepository`      | `[x]`   | G-4         |
| AP-AI-005 | Hallucinated Contract    | Test import `ReportResolutionFacade`/`AccountModerationService` không có trong TDS §8             | `[x]`   | G-3         |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào trong bản thân spec này → TDD spec approved-for-RED-phase
- [ ] Phát hiện AP khi implement → fix trước khi tiếp tục (cập nhật bảng dưới)

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ------------ | ------ | ------ | ----------- | -------- |
| —            | —      | —      | —           | —        |

---

*Test-Spec v2.0 (CASE 2.0 Anti-Pattern Detection & Red Gate Protocol) — Status: Draft.*
*ADR-004 (CONTENT DISMISS-only), ADR-005 (WARN/SUSPEND forward dependency on UC-102), and ADR-006
(re-resolution guard) are all flagged Open for human reviewer confirmation before any test moves from
RED to GREEN. UC-100 must reach GREEN first — hard technical prerequisite (ADR-001).*
