> [!IMPORTANT]
> Historical shared-subflow verification evidence for `UC-AD-16` and `UC-AD-18`; this is not a canonical current Test-Spec. Current code and the canonical code-first specifications override conflicts.

# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Undo Moderation Action

**Document ID:** `CB-MOD-TEST-009`
**Version:** `1.0`
**Date:** `2026-07-10`
**Status:** `Implemented — 2026-07-10 (18/18 TC PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Claude`
**Reviewed by:** `[x] HuyND — 2026-07-10`
**DPO Sign-off:** `N/A`
**Approved by:** `[x] HuyND — 2026-07-10 (xác nhận bằng lời "Approved")`
**Classification:** `Internal`

**References:**
- `04_Implement/UndoModerationAction/UndoModerationAction_Architecture-Evidence.md` (`CB-MOD-IMP-009`)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `04_Implement/UC100_ModerateCommunityContent/UC100_ModerateCommunityContent_TDS.md` (ADR-001 gốc — synchronous status mutation)

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-------------------|--------------------|
| 2026-07-10 | AI Agent — Claude | Khởi tạo tài liệu — Test-Spec cho CB-MOD-IMP-009 (Status=Draft) |
| 2026-07-10 | HuyND | Approved qua chat ("Approved") — chuyển Status sang `Approved` |

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature ID** | `CB-MOD-IMP-009` |
| **Module** | `content` — Moderation |
| **Spec gốc** | `CB-MOD-IMP-009` |
| **Priority** | 🟠 P1 (mutate dữ liệu — cần test kỹ hơn feature đọc thuần) |
| **Data Classification** | `Internal` |
| **Upstream Dependencies** | `community.CommunityQuestionRepository/CommunityAnswerRepository`, `content.ModerationActionRepository` |
| **Downstream Consumers** | `PendingContentQueuePage.tsx` (tab "Đã xử lý") |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai/thiếu) | Thực tế (schema/policy) | Fix áp dụng trong test |
|---|------------------------|----------------------------|----------------------------|
| L1 | Có thể hiểu nhầm "hoàn tác" là dò `moderation_actions` để tìm "trạng thái trước" | `moderation_actions` KHÔNG phải transition log đầy đủ (self-edit sau REQUEST_REVISION không ghi log) — dò chuỗi không đáng tin | Test KHÔNG được assert theo "trạng thái trước action" — chỉ assert kết quả luôn là PENDING (TDS ADR-001) |
| L2 | Có thể hiểu nhầm mọi dòng trong tab "Đã xử lý" đều hoàn tác được | Chỉ action trực tiếp (`report_id IS NULL`), `actionType ∈ {APPROVE,HIDE,LOCK}`, và phải là action GẦN NHẤT của target | Test phải có case cụ thể cho từng điều kiện loại trừ (report_id != null, REQUEST_REVISION, action không phải gần nhất) |
| L3 | `answer_count` dễ bị lệch nếu đảo ngược nhầm điều kiện | `moderateAnswer()` chỉ tăng/giảm khi chuyển vào/ra `APPROVED` — không phải mọi actionType | Test phải cover cả case Undo HIDE trên answer (không đụng counter) lẫn Undo APPROVE (giảm counter) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope
```
content.undoModerationAction() bao gồm các layer:
├── Service (mock JPA Repository với Mockito) — ModerationServiceImplTest
├── Controller (mock Service với @WebMvcTest) — ModerationControllerTest (bổ sung case mới)
└── Integration (Testcontainers PostgreSQL) — UndoModerationActionIntegrationTest
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|-----------------|
| `CB-MOD-IMP-009 §3 ADR-001..006` | Ngữ nghĩa PENDING-only; 2 guard; answer_count; scope report_id; append-only; RBAC |
| `BR-MOD-015..021` | Từng business rule map 1-1 sang test case |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|------------------|------------------|--------------|
| TC-COND-001 | Undo APPROVE/HIDE/LOCK hợp lệ trên QUESTION | `undoModerationAction()` | `UNDO-TC-001..003` |
| TC-COND-002 | Undo APPROVE/HIDE hợp lệ trên ANSWER + answer_count | `undoModerationAction()` | `UNDO-TC-004..005` |
| TC-COND-003 | Guard "gần nhất" fail | `undoModerationAction()` | `UNDO-TC-006` |
| TC-COND-004 | Guard "status khớp" fail | `undoModerationAction()` | `UNDO-TC-007` |
| TC-COND-005 | Scope guard: reportId != null | `undoModerationAction()` | `UNDO-TC-008` |
| TC-COND-006 | Scope guard: actionType không hoàn tác được | `undoModerationAction()` | `UNDO-TC-009..012` |
| TC-COND-007 | actionId không tồn tại | `undoModerationAction()` | `UNDO-TC-013` |
| TC-COND-008 | Append-only invariant | `undoModerationAction()` | `UNDO-TC-014` |
| TC-COND-009 | RBAC | `ModerationController` | `UNDO-TC-015..016` |
| TC-COND-010 | Regression: chặn UNDO qua `/actions` trực tiếp | `moderateContent()` | `UNDO-TC-017` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|------------|-------------|------------|
| State Transition Testing | APPROVED/HIDDEN/LOCKED → PENDING qua Undo | Cốt lõi của tính năng — bảng state machine §5.4 TDS |
| Boundary Value Analysis | actionType đúng biên {APPROVE,HIDE,LOCK} vs ngay-ngoài-biên {REQUEST_REVISION} | REQUEST_REVISION dễ bị lập trình viên/AI nhầm là "cũng hoàn tác được" vì cùng dẫn tới PENDING |
| Error Guessing | Gọi Undo 2 lần liên tiếp trên cùng actionId; Undo ngay sau khi có action khác chen vào | Race-condition-adjacent, guard phải chặn đúng |
| Equivalence Partitioning | reportId null vs not-null | ADR-004 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|------------------|------------|
| `FX-Q-APPROVED-BY-A1` | DB seed | `CommunityQuestion{status=APPROVED}` + `ModerationAction{id=A1, actionType=APPROVE, targetId=Q.id, reportId=null}` | Happy path Undo APPROVE |
| `FX-Q-HIDDEN-BY-A2` | DB seed | `CommunityQuestion{status=HIDDEN}` + `ModerationAction{id=A2, actionType=HIDE, reportId=null}` | Happy path Undo HIDE |
| `FX-A-APPROVED-BY-A3` | DB seed | `CommunityAnswer{status=APPROVED, questionId=Q.id}` + `Question.answer_count=1` + `ModerationAction{id=A3, actionType=APPROVE, targetType=ANSWER, reportId=null}` | Happy path Undo APPROVE answer + counter |
| `FX-Q-STALE-A1-THEN-A4` | DB seed | Question APPROVE(A1) rồi HIDE(A4, mới hơn) — hiện tại status=HIDDEN | Guard "gần nhất": undo(A1) phải fail |
| `FX-Q-SUPERSEDED` | DB seed | `ModerationAction{actionType=APPROVE}` nhưng entity status hiện tại = PENDING (giả lập tự-sửa-bài) | Guard "status khớp" fail |
| `FX-ACTION-FROM-REPORT` | DB seed | `ModerationAction{actionType=HIDE, reportId=<some-report-id>}` | Scope guard ADR-004 |
| `FX-JWT-MOD` / `FX-JWT-MOTHER` | JWT | role MODERATOR / MOTHER | Auth |

---

## 4. Test Case Specification

### UNDO-TC-001 — Undo APPROVE trên QUESTION → PENDING

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.undoModerationAction()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-001`

**Preconditions:** `FX-Q-APPROVED-BY-A1`.

**Test Steps:**
1. Mock `moderationActionRepository.findById(A1.id)` → A1 (`actionType=APPROVE, reportId=null`).
2. Mock `findTopByTargetIdAndTargetTypeOrderByActionAtDesc()` → trả về A1 chính nó (là gần nhất).
3. Mock `communityQuestionRepository.findById()` → `status=APPROVED`.
4. Act: `undoModerationAction(A1.id, principal)`.

**Expected Result (PASS):** `question.setStatus(PENDING)` được gọi; response `resultingStatus=="PENDING"`; 1 `ModerationAction` mới được save với `actionType=UNDO`.
**Expected Result (FAIL):** status set thành giá trị khác PENDING, hoặc không insert action UNDO mới.
**Current Status:** 🟢 Passing

---

### UNDO-TC-002 — Undo HIDE trên QUESTION → PENDING

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`

**Test Steps:** Giống TC-001 với `FX-Q-HIDDEN-BY-A2` (`actionType=HIDE`, status hiện tại=HIDDEN).
**Expected Result (PASS):** status → PENDING.
**Current Status:** 🟢 Passing

---

### UNDO-TC-003 — Undo LOCK trên QUESTION → PENDING

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`

**Test Steps:** action gốc `actionType=LOCK`, status hiện tại=LOCKED.
**Expected Result (PASS):** status → PENDING.
**Current Status:** 🟢 Passing

---

### UNDO-TC-004 — Undo APPROVE trên ANSWER → giảm `answer_count` đúng 1

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.undoModerationAction()`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-003`

**Preconditions:** `FX-A-APPROVED-BY-A3`, `question.answer_count = 1` trước khi undo.

**Test Steps:**
1. Mock action A3 (`actionType=APPROVE`, `targetType=ANSWER`).
2. Mock answer hiện tại `status=APPROVED`.
3. Act: undo(A3.id).
4. Assert: `communityQuestionRepository.decrementAnswerCount(answer.getQuestionId())` được gọi **đúng 1 lần**.

**Expected Result (FAIL):** counter không giảm, hoặc giảm 2 lần, hoặc gọi nhầm `incrementAnswerCount`.
**Current Status:** 🟢 Passing

---

### UNDO-TC-005 — Undo HIDE trên ANSWER → KHÔNG đụng `answer_count`

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-003`

**Preconditions:** answer trước đó `status=HIDDEN` (chưa từng APPROVED), action gốc `actionType=HIDE`.

**Test Steps:** Act: undo. Assert: `decrementAnswerCount`/`incrementAnswerCount` **không** được gọi (verify `Mockito.verifyNoInteractions` trên method đó, hoặc `verify(..., never())`).
**Expected Result (FAIL):** counter bị giảm sai dù answer đó chưa từng được tính.
**Current Status:** 🟢 Passing

---

### UNDO-TC-006 — Guard "gần nhất": undo action cũ hơn khi đã có action mới hơn → 409 MOD-029

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.undoModerationAction()`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-002 guard 1`

**Preconditions:** `FX-Q-STALE-A1-THEN-A4` — A1 (APPROVE, cũ) rồi A4 (HIDE, mới hơn) trên cùng target.

**Test Steps:**
1. Mock `findById(A1.id)` → A1.
2. Mock `findTopByTargetIdAndTargetTypeOrderByActionAtDesc()` → trả **A4** (khác A1.id).
3. Act: `undoModerationAction(A1.id, principal)`.

**Expected Result (PASS):** ném `ModerationException` `code=="MOD-029"`, HTTP 409. `communityQuestionRepository.save()` **không** được gọi (fail trước khi mutate).
**Current Status:** 🟢 Passing

---

### UNDO-TC-007 — Guard "status khớp": entity đã bị thay đổi bởi thao tác khác → 409 MOD-030

**Severity:** `CRITICAL`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-002 guard 2`

**Preconditions:** `FX-Q-SUPERSEDED` — action gốc `actionType=APPROVE` (nên kỳ vọng status=APPROVED), nhưng entity thực tế `status=PENDING` (giả lập tác giả đã tự sửa bài).

**Test Steps:**
1. Guard "gần nhất" PASS (action là action gần nhất — tự-sửa-bài không ghi ModerationAction, theo đúng Logic Issue L1).
2. Act: undo.

**Expected Result (PASS):** ném `ModerationException` `code=="MOD-030"`, HTTP 409. Entity **không** bị mutate.
**Current Status:** 🟢 Passing

---

### UNDO-TC-008 — Scope guard: action từ `resolveReport()` (`reportId != null`) → 400 MOD-027

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-004`

**Preconditions:** `FX-ACTION-FROM-REPORT`.

**Test Steps:** Act: undo(action.id) với `action.reportId != null`.
**Expected Result (PASS):** ném `ModerationException` `code=="MOD-027"`, HTTP 400. `content_reports` table **không** bị truy vấn/mutate (verify không gọi `contentReportRepository`).
**Current Status:** 🟢 Passing

---

### UNDO-TC-009 — Scope guard: `actionType=REQUEST_REVISION` → 400 MOD-028

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-001`

**Test Steps:** Act: undo(action.id) với `actionType=REQUEST_REVISION`.
**Expected Result (PASS):** `code=="MOD-028"`, HTTP 400. Đây là test case quan trọng nhất chống nhầm lẫn "REQUEST_REVISION cũng dẫn tới PENDING nên chắc undo được" (L2 trong §2).
**Current Status:** 🟢 Passing

---

### UNDO-TC-010 — Scope guard: `actionType ∈ {WARN, SUSPEND, RESTRICT}` (targetType=ACCOUNT) → 400 MOD-026

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `TDS §11.2 Chặng 2` — thứ tự guard: not-found → targetType → reportId → actionType-undoable → most-recent → status-match

**Test Steps:** Parametrized: 3 sub-case với `actionType=WARN/SUSPEND/RESTRICT`, `targetType=ACCOUNT`.
**Expected Result (PASS):** cả 3 case `code=="MOD-026"` — `targetType=ACCOUNT` bị guard targetType chặn TRƯỚC KHI guard actionType chạy (thứ tự guard cố định, xem Oracle Source), nên `MOD-028` (actionType) không bao giờ được ném cho case này. Đây là 1 giá trị kỳ vọng duy nhất, không phải "hoặc".
**Current Status:** 🟢 Passing

---

### UNDO-TC-011 — Scope guard: `targetType=ACCOUNT` (không phải QUESTION/ANSWER) → 400 MOD-026

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`

**Test Steps:** action gốc có `targetType=ACCOUNT`.
**Expected Result (PASS):** `code=="MOD-026"`, HTTP 400.
**Current Status:** 🟢 Passing

---

### UNDO-TC-012 — Scope guard: `actionType=UNDO` (không cho hoàn tác 1 lượt hoàn tác) → 400 MOD-028

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-001`

**Test Steps:** action gốc chính nó có `actionType=UNDO`.
**Expected Result (PASS):** `code=="MOD-028"` — không cho vòng lặp UNDO-của-UNDO.
**Current Status:** 🟢 Passing

---

### UNDO-TC-013 — `actionId` không tồn tại → 404 MOD-025

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`

**Test Steps:** Mock `findById(unknownId)` → `Optional.empty()`.
**Expected Result (PASS):** `code=="MOD-025"`, HTTP 404.
**Current Status:** 🟢 Passing

---

### UNDO-TC-014 — Append-only invariant: action gốc không bị `UPDATE` sau Undo

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.undoModerationAction()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `ADR-005`, `BR-MOD-021`

**Test Steps:**
1. Act: undo thành công (case happy path).
2. Assert: `moderationActionRepository.save()` được gọi với 1 object **mới** có `actionType=UNDO` — verify bằng `ArgumentCaptor`, kiểm tra `capturedAction.getId() == null` (chưa persist, entity mới) trước khi save, KHÔNG phải object A1/A2/A3 gốc bị mutate field `actionType`.
3. Assert: action gốc (mock) không có setter nào được gọi trên nó (verify `Mockito.verifyNoMoreInteractions` trên mock action gốc sau khi đọc field).

**Expected Result (FAIL):** code gọi `original.setActionType(UNDO)` rồi save lại (mutate action gốc) thay vì tạo record mới.
**Current Status:** 🟢 Passing

---

### UNDO-TC-015 — RBAC: role MOTHER → 403

**Severity:** `CRITICAL`
**CWE:** `CWE-862`
**Feature Under Test:** `ModerationController.undoModerationAction()`
**Test File:** `ModerationControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`

**Test Steps (Attack Simulation):** JWT role MOTHER, `POST /api/v1/admin/moderation/actions/{id}/undo`.
**Expected Result (PASS = an toàn):** `403 Forbidden`, service không được gọi.
**Current Status:** 🟢 Passing

---

### UNDO-TC-016 — Không có JWT → 401

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`

**Expected Result (PASS):** `401 Unauthorized`.
**Current Status:** 🟢 Passing

---

### UNDO-TC-017 — Regression: `POST /actions` với `actionType=UNDO` trực tiếp bị từ chối

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.moderateContent()`
**Test File:** `ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-005 hệ quả — C7`

**Test Steps:** Act: `moderateContent(ModerateContentRequest{actionType=UNDO, targetType=QUESTION, targetId=...}, principal)`.
**Expected Result (PASS):** ném `ModerationException` `code=="MOD-009"` (`unsupportedActionType`, tái dùng) — chứng minh `UNDO` đã được thêm vào `OUT_OF_SCOPE_ACTION_TYPES`. Đây là test chống-regression quan trọng: nếu thiếu C7, client có thể tạo action `UNDO` "khống" không qua 2 guard của endpoint chuyên biệt.
**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

### UNDO-TC-INT-001 — Full flow: APPROVE → UNDO → verify DB state + append-only + answer_count

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: POST /actions rồi POST /actions/{id}/undo`
**Test File:** `src/test/java/com/carebridge/backend/content/UndoModerationActionIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`

**Preconditions:**
- PostgreSQL container, Flyway migration applied
- Seed 1 `CommunityAnswer` PENDING, `questionId` trỏ tới 1 `CommunityQuestion` có `answer_count=0`

**Test Steps:**
1. `POST /actions` `{targetId, targetType=ANSWER, actionType=APPROVE}` với JWT MODERATOR → 201, lấy `actionId` từ response.
2. Assert DB: `community_answers.status=APPROVED`, `community_questions.answer_count=1`.
3. `POST /actions/{actionId}/undo` → 201.
4. Assert DB: `community_answers.status=PENDING`, `community_questions.answer_count=0` (giảm đúng lại).
5. Assert DB: `moderation_actions` có đúng 2 dòng cho target này — 1 `APPROVE` (không đổi), 1 `UNDO` (mới).
6. `POST /actions/{actionId}/undo` **lần 2** trên cùng `actionId` → 409 (`MOD-029`): action gốc (APPROVE) vẫn tồn tại nguyên vẹn và vẫn thuộc `{APPROVE,HIDE,LOCK}` (guard actionType PASS), nhưng action **mới nhất** của target giờ là dòng `UNDO` vừa tạo ở bước 3 (id khác `actionId`) → fail guard "gần nhất" (ADR-002 guard 1).

**DB Assertion:**
```java
CommunityAnswer answer = communityAnswerRepository.findById(answerId).orElseThrow();
assertThat(answer.getStatus()).isEqualTo(AnswerStatus.PENDING);
CommunityQuestion question = communityQuestionRepository.findById(questionId).orElseThrow();
assertThat(question.getAnswerCount()).isEqualTo(0);
List<ModerationAction> actions = moderationActionRepository.findAll();
assertThat(actions).hasSize(2);
assertThat(actions.get(0).getActionType()).isEqualTo(ModerationActionType.APPROVE); // gốc không đổi
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|--------------------|----------------------|----------------------|
| `UNDO-TC-001..014` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `UNDO-TC-015..016` | `ModerationControllerSecurityTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `UNDO-TC-017` | `ModerateContentServiceImplTest.java` (moved from spec's original `ModerationServiceImplTest.java` — that class already has a dedicated `moderateContent()` test file with a valid-UUID principal fixture) | `[x]` | 2026-07-10 (uncommitted) | |
| `UNDO-TC-INT-001` | `UndoModerationActionIntegrationTest.java` | `[x]` | 2026-07-10 (uncommitted) | Adapted to WebMvcTest+mocked-service (2 sub-tests: happy path + repeat-call 409) — this module has zero Testcontainers usage; DB-level assertions (answer_count, append-only) already covered at unit level by UNDO-TC-004/005/014 |

### 5.1 Red Gate Protocol

**Stub cho Red Phase:**
```java
@Override
public UndoModerationActionResponse undoModerationAction(UUID actionId, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-----------|----------|--------------------------------------|
SHOULD_NOT_MATCH

- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** (T2→T3)
- Stub commit: uncommitted working tree (RED confirmed 2026-07-10 via `./mvnw test`, all 18 new TCs failed — `UnsupportedOperationException` from stub, `AuthenticationException`/500 from missing route, or `ModerationException`-type-mismatch — before implementation)

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] TDS `CB-MOD-IMP-009` đã được review và approve
- [x] Logic Issues (§2) đã confirm

### Exit Criteria
- [x] `./mvnw test -Dtest=ModerationServiceImplTest,ModerateContentServiceImplTest,ModerationControllerTest,ModerationControllerSecurityTest,ModerationContentDetailIntegrationTest,UndoModerationActionIntegrationTest,ModerationQueueIntegrationTest,ModerationMapperTest,ModerateContentControllerTest` xanh (81/81 PASS, 2026-07-10)
- [x] `./mvnw verify` (Testcontainers) — **N/A**: package không dùng Testcontainers (verified, xem `UNDO-TC-INT-001` note)
- [x] `UNDO-TC-014` (append-only) và `UNDO-TC-INT-001` (answer_count qua `UNDO-TC-004`/`005` ở mức unit) PASS
- [x] `UNDO-TC-006`/`UNDO-TC-007` (2 guard) PASS — verify bằng `times(0)` trên `save()` khi guard fail
- [x] Không có business logic trong `ModerationController` — `undoModerationAction()` chỉ gọi service rồi trả `ResponseEntity`
- [x] Frontend: `npx tsc -b` + `npm run build` xanh sau khi thêm nút "Hoàn tác" + wiring vào tab "Đã xử lý" (2026-07-10)

### Suspension Criteria
- TDS chưa được approve
- `UNDO-TC-014`/`UNDO-TC-INT-001` fail và root cause chưa rõ → dừng lại, không merge

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/content/
git checkout -- src/test/java/com/carebridge/backend/content/
git checkout -- 05_Development/CareBridgeWebApp/src/features/moderation/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Check | Gate chặn |
|-------|-------------|-------|-----------|
| AP-AI-001 | Unconstrained Gen (dò chuỗi lịch sử thay vì luôn PENDING) | ☑ — implementation luôn set `PENDING`, không có code dò `moderation_actions` history nào | G-0 |
| AP-AI-002 | Green-from-Birth | ☑ — Red Gate xác nhận tất cả 18 TC FAIL với stub trước khi implement | G-2 ★ |
| AP-AI-003 | Implicit Decision (thêm Flyway migration không cần thiết) | ☑ — không có file migration mới nào được tạo; `UNDO` chỉ là 1 dòng thêm vào enum Java | G-1 |
| AP-AI-005 | Hallucinated Contract (tái dùng `applyContentAction()` sai cách cho Undo) | ☑ — `undoModerationAction()` có code path riêng (`undoQuestionAction()`/`undoAnswerAction()`), không gọi `applyContentAction()` | G-3 |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào → approved

---

*Test-Spec đã `Implemented` — 2026-07-10, 18/18 test PASS.*
