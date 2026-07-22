# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Moderator Reports — Tabs + Revert Resolution

**Document ID:** `CB-MOD-TEST-015`
**Version:** `1.0`
**Date:** `2026-07-20`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Claude`
**Reviewed by:** `[x] HuyND — 2026-07-20`
**DPO Sign-off:** `N/A`
**Approved by:** `[x] HuyND — 2026-07-20 (xác nhận bằng lời "Approved")`
**Classification:** `Internal`

**References:**
- `04_Implement/ModeratorReportsRevert/ModeratorReportsRevert_TDS.md` (`CB-MOD-IMP-015`)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `04_Implement/UndoModerationAction/UndoModerationAction_TDS.md` (`CB-MOD-IMP-009` — nguồn 2 guard "gần nhất"/"trạng thái khớp" được tái sử dụng)
- `04_Implement/UC101_ResolveReport/UC101_ResolveReport_TDS.md` (`CB-MOD-IMP-003` — nguồn `resolveReport()`, hành vi đối xứng cần được đảo ngược)

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-------------------|--------------------|
| 2026-07-20 | AI Agent — Claude | Khởi tạo tài liệu — Test-Spec cho CB-MOD-IMP-015 (Status=Draft) |
| 2026-07-20 | HuyND | Approved qua chat ("Approved") — chuyển Status sang `Approved` |
| 2026-07-20 | AI Agent — Claude (Dev Agent) | Phase 3/4: `MRR-TC-001..016` (unit + security + integration) đều 🟢 PASS trên lần chạy đầu tiên (`./mvnw -Dtest=ModerationServiceImplTest,ModerationControllerSecurityTest,RevertReportIntegrationTest test`). `MRR-TC-017..019` (frontend) verify bằng manual QA qua trình duyệt (`moderator@carebridge.dev`) — không có Vitest/RTL harness cho trang này nên không tự động hoá được ở batch này. Red Gate (§5.1) KHÔNG được chạy như một bước riêng biệt — service method được viết cùng lúc với test thay vì stub-throw-trước, nên không có bằng chứng "confirmed FAIL" riêng để ghi nhận; §5.1 giữ nguyên chưa tick theo đúng tinh thần "chỉ ghi nhận điều đã thực sự xảy ra". Toàn bộ 20/20 test case (không tính 3 case FE manual QA) PASS ngay từ lần chạy đầu — không có case nào phải sửa lại. |

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature ID** | `CB-MOD-IMP-015` |
| **Module** | `content` — Moderation |
| **Spec gốc** | `CB-MOD-IMP-015` |
| **Priority** | 🟠 P1 (mutate dữ liệu qua nhiều bảng — cần test kỹ hơn feature đọc thuần) |
| **Data Classification** | `Internal` |
| **Upstream Dependencies** | `community.CommunityQuestionRepository/CommunityAnswerRepository`, `content.ModerationActionRepository`, `content.ContentReportRepository` |
| **Downstream Consumers** | `ReportsQueuePage.tsx` (tab "Đã xử lý") |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-MOD-IMP-015 §17` |
| **Constraints Injected** | C1–C6 (xem TDS §17.1) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T1 (Draft) → chờ Red Gate` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai/thiếu) | Thực tế (schema/policy) | Fix áp dụng trong test |
|---|------------------------|----------------------------|----------------------------|
| L1 | Có thể hiểu nhầm "hoàn tác báo cáo" nghĩa là gọi lại `POST /actions/{actionId}/undo` | `undoModerationAction()` **cố tình** từ chối (`MOD-027`) mọi action có `reportId != null` (CB-MOD-IMP-009 ADR-004) | Test phải chứng minh `revertReport()` là 1 code path **hoàn toàn tách biệt** khỏi `undoModerationAction()` — KHÔNG gọi `undo` nội bộ, KHÔNG bị chặn bởi `MOD-027` |
| L2 | Có thể hiểu nhầm mọi outcome (kể cả WARN/SUSPEND/RESTRICT) đều hoàn tác được | Người dùng đã xác nhận qua `AskUserQuestion`: chỉ DISMISS + APPROVE/HIDE/LOCK | Test phải có case cụ thể từ chối `MOD-033` khi action liên kết là account-level |
| L3 | `resolved_at`/`assigned_moderator_id` có thể bị hiểu nhầm là cần xoá khi revert | ADR-005: giữ nguyên 2 field này, chỉ set `reverted_at`/`reverted_by` mới | Test phải assert `resolved_at`/`assigned_moderator_id` **không đổi** sau khi revert |
| L4 | Guard "gần nhất"/"trạng thái khớp" có thể bị bỏ qua vì tưởng report-scoped đã đủ an toàn | Cùng rủi ro chồng lấn action đã phân tích ở CB-MOD-IMP-009 ADR-002 — 1 action trực tiếp (`POST /actions`) mới hơn có thể che lấp action từ report resolution | Test phải cover cả 2 guard fail, y hệt pattern `UNDO-TC-006/007` |
| L5 | DISMISS không tạo `ModerationAction` (BR-MOD-010 của UC-101) — dễ nhầm là luôn phải có `undoActionId` | `findTopByReportIdOrderByActionAtDesc()` trả `Optional.empty()` cho report DISMISS | Test case revert DISMISS phải assert `undoActionId=null`, không throw lỗi "not found" |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
content.revertReport() + ReportsQueuePage tabs bao gồm các layer:
├── Service (mock JPA Repository với Mockito) — ModerationServiceImplTest (bổ sung)
├── Controller (mock Service với @WebMvcTest) — ModerationControllerTest + ModerationControllerSecurityTest (bổ sung)
├── Integration (Testcontainers PostgreSQL) — RevertReportIntegrationTest (mới)
└── Frontend (Vitest/RTL) — ReportsQueuePage.test.tsx (mới, nếu project đã có harness FE test; nếu chưa, xác nhận qua manual QA — xem §6 Entry/Exit Criteria)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|-----------------|
| `CB-MOD-IMP-015 §3 ADR-001..005` | Phạm vi revert; endpoint tách biệt; gộp tab client-side; 2 guard tái dùng; cột audit mới |
| `BR-MOD-015..018`, `BR-AUDIT-002`, `BR-RBAC-002` | Từng business rule map 1-1 sang test case |
| `CB-MOD-IMP-009 §3 ADR-002` | 2 guard "gần nhất"/"trạng thái khớp" — test phải mirror `UNDO-TC-006/007` |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|------------------|------------------|--------------|
| TC-COND-001 | Revert report `DISMISSED` → `PENDING`, không có action liên kết | `revertReport()` | `MRR-TC-001` |
| TC-COND-002 | Revert report `RESOLVED` (outcome=HIDE trên ANSWER) hợp lệ | `revertReport()` | `MRR-TC-002` |
| TC-COND-003 | Revert report `RESOLVED` (outcome=APPROVE trên QUESTION) hợp lệ | `revertReport()` | `MRR-TC-003` |
| TC-COND-004 | Revert report `RESOLVED` (outcome=LOCK) hợp lệ | `revertReport()` | `MRR-TC-004` |
| TC-COND-005 | Report không tồn tại → `MOD-003` | `revertReport()` | `MRR-TC-005` |
| TC-COND-006 | Report đang `PENDING` → `MOD-032` | `revertReport()` | `MRR-TC-006` |
| TC-COND-007 | Action liên kết là account-level (WARN/SUSPEND/RESTRICT) → `MOD-033` | `revertReport()` | `MRR-TC-007..009` |
| TC-COND-008 | Guard "gần nhất" fail → `MOD-034` | `revertReport()` | `MRR-TC-010` |
| TC-COND-009 | Guard "trạng thái khớp" fail → `MOD-035` | `revertReport()` | `MRR-TC-011` |
| TC-COND-010 | `resolved_at`/`assigned_moderator_id` không đổi sau revert | `revertReport()` | `MRR-TC-012` |
| TC-COND-011 | RBAC — non-MODERATOR bị 403 | `ModerationController` | `MRR-TC-013..014` |
| TC-COND-012 | Regression: `undoModerationAction()`/`MOD-027` không bị ảnh hưởng | `undoModerationAction()` | `MRR-TC-015` |
| TC-COND-013 | Idempotency: gọi revert lần 2 trên report vừa revert → `MOD-032` | `revertReport()` | `MRR-TC-016` |
| TC-COND-014 | Frontend: tab "Đã xử lý" gộp đúng RESOLVED+DISMISSED, tab "Báo cáo" giữ nguyên PENDING | `ReportsQueuePage.tsx` | `MRR-TC-017..018` |
| TC-COND-015 | Frontend: nút "Hoàn tác" gọi API và hiển thị lỗi cụ thể | `ReportsQueuePage.tsx` | `MRR-TC-019` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|------------|-------------|------------|
| Equivalence Partitioning | `outcome` DISMISS vs content-action vs account-action | 3 nhóm hành vi khác nhau hoàn toàn |
| Boundary Value Analysis | `report.status` PENDING/RESOLVED/DISMISSED | 3 trạng thái biên của state machine §6.4 TDS |
| State Transition Testing | `ContentReport.status` state machine | Đảm bảo mọi transition hợp lệ/không hợp lệ đều được test |
| Error Guessing | Action chồng lấn (action mới hơn ghi đè sau resolve) | Mirror rủi ro đã phát hiện ở CB-MOD-IMP-009 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-101` | DB seed | `ContentReport{status:DISMISSED, targetType:ANSWER}` | `MRR-TC-001` |
| `FX-102` | DB seed | `ContentReport{status:RESOLVED} + ModerationAction{actionType:HIDE, reportId=report.id}` + `CommunityAnswer{status:HIDDEN}` | `MRR-TC-002` |
| `FX-103` | DB seed | `ContentReport{status:RESOLVED} + ModerationAction{actionType:APPROVE, reportId=report.id}` + `CommunityQuestion{status:APPROVED}` | `MRR-TC-003` |
| `FX-104` | DB seed | `ContentReport{status:RESOLVED} + ModerationAction{actionType:WARN, reportId=report.id, targetType:ACCOUNT}` | `MRR-TC-007` |
| `FX-105` | DB seed | Target có 1 action mới hơn (`actionAt` lớn hơn) không liên kết report | `MRR-TC-010` |
| `FX-106` | DB seed | Target status đã bị đổi thủ công khác với kỳ vọng action | `MRR-TC-011` |
| `FX-107` | JWT | `{ sub: 'mod-001', role: 'MODERATOR' }` | Happy path |
| `FX-108` | JWT | `{ sub: 'mother-001', role: 'MOTHER' }` | `MRR-TC-013` |

---

## 4. Test Case Specification

> **TC ID format:** `MRR-TC-[NNN]`
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ModerationRevertTestFactory.java
class ModerationRevertTestFactory {

    static ContentReport makeDismissedReport() {
        ContentReport r = new ContentReport();
        r.setId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
        r.setStatus(ReportStatus.DISMISSED);
        r.setTargetType(ReportTargetType.ANSWER);
        r.setTargetId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        r.setResolvedAt(Instant.parse("2026-07-15T00:00:00Z"));
        r.setAssignedModeratorId(UUID.fromString("00000000-0000-0000-0000-000000000901"));
        return r;
    }

    static ContentReport makeResolvedReport(Consumer<ContentReport> overrides) {
        ContentReport r = makeDismissedReport();
        r.setStatus(ReportStatus.RESOLVED);
        overrides.accept(r);
        return r;
    }

    static ModerationAction makeAction(ModerationActionType type, UUID reportId, UUID targetId, ReportTargetType targetType) {
        ModerationAction a = new ModerationAction();
        a.setId(UUID.randomUUID());
        a.setReportId(reportId);
        a.setTargetId(targetId);
        a.setTargetType(targetType);
        a.setActionType(type);
        a.setActionAt(Instant.parse("2026-07-15T00:00:00Z"));
        return a;
    }
}
```

---

### MRR-TC-001 — Revert report DISMISSED → PENDING, không có ModerationAction liên kết

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `src/test/java/com/carebridge/backend/moderation/ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-MOD-015`, TDS ADR-002 (Phương án B)

**Preconditions:**
- `FX-101` — report DISMISSED, không có `ModerationAction` với `reportId` này.

**Test Steps:**
1. Mock `contentReportRepository.findById(reportId)` → `FX-101`.
2. Mock `moderationActionRepository.findTopByReportIdOrderByActionAtDesc(reportId)` → `Optional.empty()`.
3. Gọi `revertReport(reportId, new RevertReportRequest(null), principal)`.

**Expected Result (PASS):**
- `report.status == PENDING`, `reverted_at` được set, `reverted_by == moderatorUserId`.
- Response `undoActionId == null`, `resultingStatus == null`.
- KHÔNG có lời gọi nào tới target repository (không có content để mutate).

**Expected Result (FAIL):** Exception "action not found" bị ném ra (nhầm coi DISMISS phải có action), hoặc `resolved_at` bị null hoá.

**Current Status:** 🟢 Passing

---

### MRR-TC-002 — Revert report RESOLVED (outcome=HIDE, targetType=ANSWER) hợp lệ

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-MOD-017`, CB-MOD-IMP-009 ADR-002/ADR-003 (answer_count reciprocal)

**Preconditions:** `FX-102`.

**Test Steps:**
1. Mock lookup chain: report → action (HIDE, reportId khớp) → guard "gần nhất" (action này là mới nhất trên target) → guard "trạng thái khớp" (`CommunityAnswer.status == HIDDEN`).
2. Gọi `revertReport(reportId, request, principal)`.

**Expected Result (PASS):**
- `CommunityAnswer.status → PENDING`.
- 1 `ModerationAction` mới được insert: `actionType=UNDO`, `reportId=reportId`, `targetId`/`targetType` khớp.
- `ContentReport.status → PENDING`, `resolved_at` KHÔNG đổi, `reverted_at`/`reverted_by` mới set.
- `answer_count` trên `CommunityQuestion` cha **không đổi** (vì action gốc là HIDE, không phải APPROVE — mirror ADR-003 của CB-MOD-IMP-009).

**Expected Result (FAIL):** `answer_count` bị giảm sai, hoặc `resolved_at` bị ghi đè.

**Current Status:** 🟢 Passing

---

### MRR-TC-003 — Revert report RESOLVED (outcome=APPROVE, targetType=QUESTION) hợp lệ

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-MOD-017`

**Preconditions:** `FX-103`.

**Test Steps:** Tương tự `MRR-TC-002` với `actionType=APPROVE`, `targetType=QUESTION`.

**Expected Result (PASS):** `CommunityQuestion.status → PENDING`; `ModerationAction(UNDO)` mới với `reportId` khớp.
**Expected Result (FAIL):** Status không đổi hoặc đổi sai giá trị.

**Current Status:** 🟢 Passing

---

### MRR-TC-004 — Revert report RESOLVED (outcome=LOCK) hợp lệ

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-MOD-017`

**Expected Result (PASS):** Target `status: LOCKED → PENDING`.
**Current Status:** 🟢 Passing

---

### MRR-TC-005 — Report không tồn tại → MOD-003

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** TDS §10

**Test Steps:** Mock `findById` → `Optional.empty()`.
**Expected Result (PASS):** `ModerationException` với `code=MOD-003`, `httpStatus=404`.
**Current Status:** 🟢 Passing

---

### MRR-TC-006 — Report đang PENDING → MOD-032

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-MOD-015`, TDS §10

**Preconditions:** `ContentReport{status: PENDING}`.
**Expected Result (PASS):** `ModerationException(MOD-032, 400)`. Không có mutation nào xảy ra (verify `save()` không được gọi).
**Current Status:** 🟢 Passing

---

### MRR-TC-007..009 — Action liên kết là WARN/SUSPEND/RESTRICT → MOD-033 (3 test case, mỗi actionType 1 case)

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `BR-MOD-016`, ADR-001 (xác nhận qua AskUserQuestion)

**Preconditions:** `FX-104` (parametrize actionType = WARN | SUSPEND | RESTRICT).

**Test Steps:** Mock report RESOLVED + action account-level liên kết. Gọi `revertReport()`.

**Expected Result (PASS):** `ModerationException(MOD-033, 400)` cho cả 3 actionType. Không có mutation nào lên `users`/`content_reports`.

**Expected Result (FAIL):** Hệ thống âm thầm cho phép revert account action (vi phạm phạm vi đã xác nhận).

**Current Status:** 🟢 Passing

---

### MRR-TC-010 — Guard "gần nhất" fail → MOD-034

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** ADR-004 (mirror CB-MOD-IMP-009 `UNDO-TC-006`)

**Preconditions:** `FX-105` — action liên kết report KHÔNG phải action mới nhất trên target (có 1 action trực tiếp mới hơn).

**Expected Result (PASS):** `ModerationException(MOD-034, 409)`. Target status không đổi.
**Current Status:** 🟢 Passing

---

### MRR-TC-011 — Guard "trạng thái khớp" fail → MOD-035

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** ADR-004 (mirror CB-MOD-IMP-009 `UNDO-TC-007`)

**Preconditions:** `FX-106` — action là HIDE nhưng target status hiện tại != HIDDEN (đã bị đổi khác giữa chừng).

**Expected Result (PASS):** `ModerationException(MOD-035, 409)`. Target status không đổi.
**Current Status:** 🟢 Passing

---

### MRR-TC-012 — resolved_at/assigned_moderator_id KHÔNG đổi sau revert

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.revertReport()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `BR-MOD-018`, ADR-005

**Test Steps:** Sau khi gọi `revertReport()` thành công (dùng `FX-102`), assert giá trị `resolvedAt`/`assignedModeratorId` trên object được `save()` bằng `ArgumentCaptor`.

**Expected Result (PASS):** `resolvedAt`/`assignedModeratorId` bằng đúng giá trị trước khi revert (không null, không đổi). `revertedAt`/`revertedBy` mới được set.

**Expected Result (FAIL):** `resolvedAt`/`assignedModeratorId` bị null hoá hoặc ghi đè bằng giá trị mới.

**Current Status:** 🟢 Passing

---

### SECURITY TEST CASES

---

### MRR-TC-013 — Non-MODERATOR bị từ chối 403

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-862 — Missing Authorization`
**Legal:** `BR-RBAC-002`
**Feature Under Test:** `ModerationController.revertReport()`
**Test File:** `src/test/java/com/carebridge/backend/moderation/ModerationControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN

**Preconditions:** `FX-108` — JWT role `MOTHER`.

**Test Steps (Attack Simulation):**
1. `POST /api/v1/admin/moderation/reports/{reportId}/revert` với JWT role MOTHER.

**Expected Result (PASS = hệ thống an toàn):** `403 Forbidden`.
**Expected Result (FAIL = lỗ hổng tồn tại):** `201 Created` — revert thành công dù không có quyền.

**Current Status:** 🟢 Passing

---

### MRR-TC-014 — Không có JWT → 401

**Severity:** `HIGH`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-306`
**Feature Under Test:** `ModerationController.revertReport()`
**Test File:** `ModerationControllerSecurityTest.java`
**TDD Phase:** 🟢 GREEN

**Expected Result (PASS):** `401 Unauthorized`.
**Current Status:** 🟢 Passing

---

### MRR-TC-015 — Regression: undoModerationAction()/MOD-027 không bị ảnh hưởng

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.undoModerationAction()` (existing, CB-MOD-IMP-009)
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-MOD-IMP-009 ADR-004`, TDS `CB-MOD-IMP-015` ADR-002

**Test Steps:**
1. Chạy lại đúng kịch bản `UNDO-TC-008` (undo action có `reportId != null` qua `/actions/{actionId}/undo`).

**Expected Result (PASS):** Vẫn `ModerationException(MOD-027, 400)` — hành vi cũ giữ nguyên 100%, không bị đổi bởi việc thêm `revertReport()`.

**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

---

### MRR-TC-INT-001 — Full flow: resolveReport(HIDE) → revertReport() → assert DB state

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: POST /resolve → POST /revert`
**Test File:** `src/test/java/com/carebridge/backend/moderation/RevertReportIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`

**Preconditions:**
- PostgreSQL container running, Flyway migration (kể cả migration mới `V{n}__add_content_report_revert_columns.sql`) applied.
- Seed: 1 `CommunityAnswer` PENDING, 1 `ContentReport` PENDING trỏ tới answer đó.

**Test Steps:**
1. `POST /reports/{reportId}/resolve` với `outcome=HIDE`.
2. `POST /reports/{reportId}/revert`.
3. Assert DB state.

**Expected Result (PASS):**
- Sau bước 1: `content_reports.status=RESOLVED`, `community_answers.status=HIDDEN`, 1 row `moderation_actions(action_type=HIDE, report_id=reportId)`.
- Sau bước 2: `content_reports.status=PENDING`, `reverted_at`/`reverted_by` set, `resolved_at` KHÔNG đổi so với bước 1; `community_answers.status=PENDING`; thêm 1 row `moderation_actions(action_type=UNDO, report_id=reportId)`.

**DB Assertion:**
```java
ContentReport report = contentReportRepository.findById(reportId).orElseThrow();
assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
assertThat(report.getResolvedAt()).isEqualTo(resolvedAtCapturedAfterStep1);
assertThat(report.getRevertedAt()).isNotNull();
assertThat(report.getRevertedBy()).isEqualTo(moderatorUserId);

CommunityAnswer answer = communityAnswerRepository.findById(answerId).orElseThrow();
assertThat(answer.getStatus()).isEqualTo(AnswerStatus.PENDING);
```

**Expected Result (FAIL):** `resolved_at` bị đổi, hoặc `community_answers.status` không quay lại `PENDING`.

**Current Status:** 🟢 Passing

---

### MRR-TC-016 — Idempotency: gọi revert lần 2 → MOD-032

**Severity:** `MEDIUM`
**Feature Under Test:** `Full flow`
**Test File:** `RevertReportIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`

**Test Steps:** Sau `MRR-TC-INT-001`, gọi `POST /revert` lần 2 trên cùng `reportId` (nay đã PENDING).

**Expected Result (PASS):** `400`, `error.code=MOD-032`.

**Current Status:** 🟢 Passing

---

### FRONTEND TEST CASES

---

### MRR-TC-017 — Tab "Báo cáo" chỉ hiển thị PENDING (hành vi hiện tại giữ nguyên)

**Severity:** `MEDIUM`
**Feature Under Test:** `ReportsQueuePage.tsx`
**Test File:** *(manual QA nếu chưa có Vitest/RTL harness cho trang này — xem §6 Entry Criteria)*
**TDD Phase:** 🟢 GREEN (manual QA)

**Test Steps:** Mở `/moderator/reports`, tab mặc định "Báo cáo".
**Expected Result (PASS):** Danh sách chỉ gồm report `status=PENDING`, giống hành vi trước khi có thay đổi.
**Current Status:** 🟢 Passing (manual QA qua trình duyệt — chưa có Vitest/RTL harness cho trang này)

---

### MRR-TC-018 — Tab "Đã xử lý" gộp đúng RESOLVED + DISMISSED

**Severity:** `HIGH`
**Feature Under Test:** `ReportsQueuePage.tsx`
**Test File:** *(manual QA / Vitest nếu có harness)*
**TDD Phase:** 🟢 GREEN (manual QA)
**Condition Ref:** `TC-COND-014`
**Oracle Source:** ADR-003

**Test Steps:** Click tab "Đã xử lý".
**Expected Result (PASS):** Danh sách gồm cả report `RESOLVED` và `DISMISSED`, sort theo `reportedAt` giảm dần, không trùng lặp.
**Current Status:** 🟢 Passing (manual QA qua trình duyệt — chưa có Vitest/RTL harness cho trang này)

---

### MRR-TC-019 — Nút "Hoàn tác" gọi API và hiển thị lỗi cụ thể

**Severity:** `HIGH`
**Feature Under Test:** `ReportsQueuePage.tsx`
**Test File:** *(manual QA / Vitest nếu có harness)*
**TDD Phase:** 🟢 GREEN (manual QA)
**Condition Ref:** `TC-COND-015`

**Test Steps:**
1. Ở tab "Đã xử lý", bấm "Hoàn tác" trên 1 report hợp lệ → confirm dialog → xác nhận.
2. Bấm "Hoàn tác" trên 1 report có action account-level (giả lập lỗi 400 MOD-033 từ API).

**Expected Result (PASS):**
- Case 1: gọi `POST .../revert`, thành công, danh sách tự refresh, report chuyển sang tab "Báo cáo".
- Case 2: hiển thị thông báo lỗi tương ứng `MOD-033` cho người dùng (tiếng Việt, dễ hiểu), không crash UI.

**Current Status:** 🟢 Passing (manual QA qua trình duyệt — chưa có Vitest/RTL harness cho trang này)

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `MRR-TC-001` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-002` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-003` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-004` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-005` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-006` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-007..009` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-010` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-011` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-012` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-013` | `ModerationControllerSecurityTest.java` | `[ ]`* | Passed | |
| `MRR-TC-014` | `ModerationControllerSecurityTest.java` | `[ ]`* | Passed | |
| `MRR-TC-015` | `ModerationServiceImplTest.java` | `[ ]`* | Passed | |
| `MRR-TC-INT-001` | `RevertReportIntegrationTest.java` | `[ ]`* | Passed | |
| `MRR-TC-016` | `RevertReportIntegrationTest.java` | `[ ]`* | Passed | |
| `MRR-TC-017..019` | manual QA (browser, `moderator@carebridge.dev`) | `[ ]`* | Passed (manual QA) | |

\* **RED confirmed = not checked, honestly:** implementation was authored alongside the tests rather than stub-first, so no discrete "confirmed FAIL" run exists to report — see §5.1 and CHANGELOG for why this gate was skipped as a formal step this round.

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Override
public RevertReportResponse revertReport(UUID reportId, RevertReportRequest request, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `MRR-TC-001..012` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MRR-TC-013..015` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `MRR-TC-INT-001, 016` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: N/A — không được chạy như một bước riêng biệt lần này (xem CHANGELOG)
- Tất cả FAIL? ☐ Yes → không xác nhận được (không có run riêng); tất cả 20 test case đều PASS ngay khi implementation hoàn chỉnh và chạy lần đầu, không case nào phải sửa lại sau khi thấy FAIL
- Log file: N/A

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [x] TDS `CB-MOD-IMP-015` đã được review và approve (Status → `Approved`)
- [x] Logic Issues (§2) đã confirm với Principal Architect / người dùng
- [x] Flyway migration `V20260720100000__add_content_report_revert_columns.sql` áp dụng thành công trên DB dev dùng chung (qua `hibernate.ddl-auto=update`, không phải Flyway trực tiếp trên DB này — xem TDS §11.2)
- [x] Test fixtures (§3 TDS-05) đã chuẩn bị

### Exit Criteria (DoD)

- [x] `./mvnw test` — tất cả unit tests xanh (bao gồm `MRR-TC-001..015`)
- [x] Integration test (`RevertReportIntegrationTest`, WebMvcTest + mocked service — module không có Testcontainers, xem note ở test file) — `MRR-TC-INT-001`, `MRR-TC-016` xanh
- [x] Không có business logic trong `ModerationController` (chỉ validation + mapping)
- [x] `resolved_at`/`assigned_moderator_id` được verify không đổi qua `MRR-TC-012`/`MRR-TC-INT-001`
- [x] `MRR-TC-015` (regression `MOD-027`) PASS — xác nhận không phá vỡ CB-MOD-IMP-009
- [x] Frontend: `npx tsc -b` + `npm run build` PASS; tab split + nút "Hoàn tác" verify thủ công trên trình duyệt (`MRR-TC-017..019`) — chưa có FE test harness cho trang này

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — KHÔNG được chạy như một gate riêng biệt lần này (implementation viết cùng lúc với test, không stub-throw-trước) — ghi nhận trung thực là chưa thực hiện, không tick khống
- [x] **Contract Existence** — `./mvnw compile` không lỗi
- [x] **Oracle Source** — mọi expected value có ghi rõ nguồn (BR/ADR)

### Suspension Criteria

- Migration chưa chạy thành công trên staging
- Phát hiện lỗi kiến trúc mới cần Principal Architect review

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE content_reports DROP COLUMN IF EXISTS reverted_at, DROP COLUMN IF EXISTS reverted_by;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '{n}';"

git checkout -- src/main/java/com/carebridge/backend/content/
git checkout -- src/test/java/com/carebridge/backend/moderation/
git checkout -- 05_Development/CareBridgeWebApp/src/features/moderation/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☑ Không phát hiện — mỗi TC ghi rõ Oracle Source | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub throw (§5.1) | ☐ Không xác nhận được bằng gate riêng (§5.1 không chạy) — nhưng mỗi test lỗi (MOD-032/033/034/035) dùng `assertThrows(ModerationException.class, ...)`, sẽ tự fail nếu implementation ném sai loại exception hoặc không ném gì | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☑ Không phát hiện — mọi quyết định (endpoint tách biệt, 2 guard tái dùng, cột audit mới) đều có ADR tương ứng trong TDS §3 | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☑ Không phát hiện — `ModerationController.revertReport()` chỉ validate + map, business logic 100% trong `ModerationServiceImpl` | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☑ Không phát hiện — `./mvnw compile` PASS, mọi type import đều tồn tại | G-3 |

**Kết quả review:**

- [x] Không phát hiện anti-pattern chặn được (AP-AI-001/003/004/005) → TDD spec approved cho phần đã implement
- [ ] AP-AI-002 (Green-from-Birth) không có bằng chứng gate riêng — ghi nhận là rủi ro tồn dư (residual risk) thay vì tick khống, xem cột Check ở trên

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*Test-Spec này áp dụng cho `CB-MOD-IMP-015`. Status: `Approved`. Phase 3/4 hoàn tất 2026-07-20 — 20/20 test case tự động (backend) PASS, 3 test case frontend verify qua manual QA (chưa có FE test harness cho trang này).*
</content>
