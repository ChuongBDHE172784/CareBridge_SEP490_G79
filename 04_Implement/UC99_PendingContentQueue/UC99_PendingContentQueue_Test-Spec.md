# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Pending Content Queue (First-Time Moderation)

| Field                | Value                                    |
| --------------------- | ------------------------------------------ |
| **Document ID**       | `CB-MOD-TEST-004`                         |
| **Version**           | `1.0`                                      |
| **Date**              | `2026-07-03`                               |
| **Status**            | `Implemented — 2026-07-03 (9/9 test cases GREEN, verified via UI E2E)` |
| **Related TDS**       | `04_Implement/UC99_PendingContentQueue/UC99_PendingContentQueue_TDS.md` (`CB-MOD-IMP-004`) |
| **Author**            | `AI Agent — Winston (System Architect)`   |

---

## CHANGELOG

| Ngày       | Người thực hiện   | Nội dung thay đổi                                    |
| ---------- | ------------------ | ------------------------------------------------------- |
| 2026-07-03 | AI Agent — Winston | Tạo tài liệu lần đầu — Test-Spec cho Pending Content Queue |
| 2026-07-03 | HuyND              | Approved — tự duyệt để tiến hành implement ngay              |
| 2026-07-03 | AI Agent — Amelia (Dev Agent) | Implement hoàn chỉnh theo Red-Green-Refactor — 9/9 test PASS (`./mvnw test`), verify qua UI thật bằng Chrome DevTools MCP (đăng nhập moderator, duyệt câu hỏi test, xác nhận hiện trên feed của mother). Không có regression (11 lỗi pre-existing ở module exercise không liên quan). |
| 2026-07-03 | AI Agent — Amelia (Dev Agent) | Bổ sung nút "Ẩn" (HIDE) — chỉ ở frontend, tái dùng backend UC-100 không đổi nên không có unit test mới; verify bằng UI E2E thủ công (tạo câu hỏi PENDING test, bấm Ẩn, xác nhận DB `status → HIDDEN`, `reason` được ghi đúng, `report_id = NULL`). Không nằm trong 9 test case ban đầu của tài liệu này — ghi nhận trung thực, không thêm test case giả. |
| 2026-07-03 | AI Agent — Amelia (Dev Agent) | EXTENSION History (TDS §16): 6 TC mới (PCQH-TC-001…006), Red→Green xác nhận, 34/34 test PASS tổng cộng. Tab "Đã xử lý" verify UI thật với dữ liệu thật trong DB dev (không phải data test tự tạo) — hiển thị đúng preview, hành động, lý do, người xử lý, thời gian. |
| 2026-07-14 | Codex | Correction — manual UI acceptance cho tab `PENDING`: chỉ thấy `Duyệt` và `Yêu cầu sửa`; không thấy `Ẩn` hoặc `Khóa`. Bản ghi HIDE cũ là lịch sử implementation, không còn là expected behavior của first-time queue. |

---

## MUC LUC

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

| Field                | Value                                                                  |
| --------------------- | ------------------------------------------------------------------------ |
| **Module**            | `content` (ModerationService/ModerationController extension)            |
| **Test Framework**    | JUnit 5 + Mockito (unit) — không có integration tier (không có Testcontainers trong project, xem memory `project_community_module_patterns`) |
| **Coverage Target**   | Service logic (targetType routing + validation) + Controller (RBAC + shape) |

### 1.1 AI Generation Context (CASE 2.0)
- Không sinh mã lâm sàng/chẩn đoán — module này thuần túy CRUD-read + reuse hành động UC-100 đã có.
- Không test lại `moderateContent()` (UC-100) — đã có test coverage riêng, không đổi hành vi.

---

## 2. Logic Issues Resolved

| Vấn đề                                                                                          | Giải pháp                                                                 |
| --------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| Nội dung PENDING chưa từng bị report không có nơi để moderator xem                                  | Endpoint mới `GET /pending-content`, query trực tiếp `CommunityQuestion`/`CommunityAnswer` |
| Không được lẫn field "N/A" (reportCount/reportReason) vào response khi nội dung không có report      | DTO mới `PendingContentItemResponse`, không tái dùng `ModerationQueueItemResponse` |
| targetType=CONTENT/ACCOUNT không có ý nghĩa cho endpoint này                                        | Validate ở service, reject với MOD-023                                    |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi
In scope: `ModerationServiceImpl.getPendingContentQueue()`, `ModerationController.getPendingContentQueue()`, repository `findByStatus()` methods (covered indirectly via service tests with mocked repository).
Out of scope: `moderateContent()` (UC-100, không đổi), frontend unit tests (Flutter/React không có framework test cấu hình sẵn cho module này — verify bằng Chrome DevTools MCP thủ công thay thế, xem §6 Exit Criteria).

### TDS-02 — Test Basis
`UC99_PendingContentQueue_TDS.md` §5–§9.

### TDS-03 — Test Conditions and Coverage Items
- targetType = QUESTION → query CommunityQuestionRepository
- targetType = ANSWER → query CommunityAnswerRepository
- targetType = CONTENT/ACCOUNT/null → MOD-023
- size > 50 → MOD-002 (tái dùng)
- Kết quả rỗng → content=[], không lỗi
- Non-MODERATOR → 403
- contentPreview không chứa PII
- Manual UI: với QUESTION/ANSWER `PENDING`, chỉ hiển thị `Duyệt` và `Yêu cầu sửa`; không hiển thị `Ẩn`/`Khóa`.

### TDS-04 — Test Techniques
Equivalence partitioning (targetType hợp lệ/không hợp lệ), boundary (size=50 vs 51), mocked repository (unit, không cần DB thật).

### TDS-05 — Test Data Requirements
Dùng factory tạo `CommunityQuestion`/`CommunityAnswer` mock objects — không cần seed DB thật cho unit test.

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// test-root: com/carebridge/backend/content/service/PendingContentTestFactory.java
package com.carebridge.backend.content.service;

import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.UrgencyLevel;
import java.time.Instant;
import java.util.UUID;

public final class PendingContentTestFactory {

    private PendingContentTestFactory() {}

    public static CommunityQuestion pendingQuestion() {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .title("Em bị đau bụng dưới ở tuần 20")
                .body("Em bị đau bụng dưới nhẹ 2 ngày nay, có đáng lo không ạ?")
                .stage(PregnancyStage.PREGNANT)
                .urgency(UrgencyLevel.LOW)
                .status(QuestionStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public static CommunityAnswer pendingAnswer() {
        return CommunityAnswer.builder()
                .id(UUID.randomUUID())
                .questionId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .body("Bạn nên đi khám sớm để bác sĩ kiểm tra trực tiếp nhé.")
                .status(AnswerStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }
}
```

---

### PCQ-TC-001 — targetType=QUESTION trả về danh sách CommunityQuestion PENDING

**Feature under test:** `ModerationServiceImpl.getPendingContentQueue()`
**Test file:** `content/service/ModerationServiceImplTest.java`

**Arrange:** Mock `CommunityQuestionRepository.findByStatus(PENDING, pageable)` trả về Page chứa 2 `pendingQuestion()`. Mock `ContentPreviewService.batchFetchPreviews()` trả preview tương ứng.
**Act:** Gọi `getPendingContentQueue(filter(QUESTION, 0, 20), principal)`.
**Expected:** Response có `content.size() == 2`, mỗi item có `targetType == QUESTION`, `contentPreview` khớp giá trị mock, `CommunityAnswerRepository` **không** được gọi.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

### PCQ-TC-002 — targetType=ANSWER trả về danh sách CommunityAnswer PENDING

**Test file:** `content/service/ModerationServiceImplTest.java`

**Arrange:** Mock `CommunityAnswerRepository.findByStatus(PENDING, pageable)` trả về Page chứa 1 `pendingAnswer()`.
**Act:** Gọi `getPendingContentQueue(filter(ANSWER, 0, 20), principal)`.
**Expected:** `content.size() == 1`, `targetType == ANSWER`, `CommunityQuestionRepository` không được gọi.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

### PCQ-TC-003 — targetType=CONTENT → ModerationException MOD-023

**Test file:** `content/service/ModerationServiceImplTest.java`

**Arrange:** filter với `targetType = ReportTargetType.CONTENT`.
**Act & Assert:** `assertThrows(ModerationException.class, ...)`, `exception.getCode() == "MOD-023"`. Không repository nào được gọi.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

### PCQ-TC-004 — targetType=ACCOUNT → ModerationException MOD-023

**Test file:** `content/service/ModerationServiceImplTest.java`

Giống PCQ-TC-003, với `targetType = ACCOUNT`.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

### PCQ-TC-005 — size > 50 → ModerationException MOD-002 (Controller layer)

**Test file:** `content/controller/ModerationControllerTest.java`

**Arrange:** MockMvc request `GET /pending-content?targetType=QUESTION&size=51` với MODERATOR token.
**Expected:** 400, body chứa `"MOD-002"`. Tái dùng guard code có sẵn trong controller (giống `getQueue()`).

**TDD Phase:** 🟢 GREEN (N/A Red Gate — controller test mocks the service layer, same convention as existing UC-99 controller tests)
**Current Status:** 🟢 Passing

---

### PCQ-TC-006 — contentPreview không chứa PII (authorId)

**Test file:** `content/service/ModerationServiceImplTest.java`

**Arrange:** `pendingQuestion()` có `authorId` set. Mock `ContentPreviewService.batchFetchPreviews()` trả về preview chỉ chứa `body` (đúng hành vi thật của `ContentPreviewServiceImpl`, không đổi).
**Act:** Gọi `getPendingContentQueue()`.
**Assert:** `PendingContentItemResponse` **không có field authorId** (kiểm tra bằng reflection hoặc đơn giản là DTO record không khai báo field này — assert biên dịch, không cần runtime check) — mục đích: khẳng định lại design constraint C2, tránh regression trong tương lai nếu ai đó thêm authorId vào DTO.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

### PCQ-TC-007 — Non-MODERATOR bị từ chối 403 (Controller)

**Test file:** `content/controller/ModerationControllerTest.java`

**Arrange:** MockMvc request với token role `MOTHER` (hoặc không có role MODERATOR).
**Act:** `GET /pending-content?targetType=QUESTION`.
**Expected:** 403 Forbidden.

**TDD Phase:** 🟢 GREEN (N/A Red Gate — controller test mocks the service layer, same convention as existing UC-99 controller tests)
**Current Status:** 🟢 Passing

---

### PCQ-TC-008 — Happy path Controller trả 200 với đúng JSON shape

**Test file:** `content/controller/ModerationControllerTest.java`

**Arrange:** Mock service trả về `PendingContentQueueResponse` với 1 item. MODERATOR token.
**Act:** `GET /pending-content?targetType=QUESTION&page=0&size=20`.
**Expected:** 200, JSON path `$.content[0].targetType == "QUESTION"`, `$.totalElements == 1`, `$.page == 0`, `$.size == 20`.

**TDD Phase:** 🟢 GREEN (N/A Red Gate — controller test mocks the service layer, same convention as existing UC-99 controller tests)
**Current Status:** 🟢 Passing

---

### PCQ-TC-009 — Không có nội dung PENDING nào → content rỗng, không lỗi

**Test file:** `content/service/ModerationServiceImplTest.java`

**Arrange:** Mock repository trả về `Page.empty()`.
**Act:** Gọi `getPendingContentQueue(filter(QUESTION, 0, 20), principal)`.
**Expected:** `content.isEmpty() == true`, `totalElements == 0`, không throw exception.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

## 4.1 EXTENSION (2026-07-03) — Moderation History Test Cases (TDS §16)

### PCQH-TC-001 — targetType=null trả về cả QUESTION và ANSWER, sort actionAt DESC

**Test file:** `content/service/ModerationServiceImplTest.java`

**Arrange:** Mock `ModerationActionRepository.findByTargetTypeInOrderByActionAtDesc(List.of(QUESTION, ANSWER), pageable)` trả về Page gồm 1 action QUESTION + 1 action ANSWER.
**Act:** Gọi `getModerationHistory(filter(null, 0, 20), principal)`.
**Expected:** `content.size() == 2`, đúng cả 2 targetType.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

### PCQH-TC-002 — targetType=QUESTION filter đúng

**Test file:** `content/service/ModerationServiceImplTest.java`

**Arrange:** Mock repo trả Page chỉ gồm action QUESTION.
**Act:** Gọi `getModerationHistory(filter(QUESTION, 0, 20), principal)`.
**Expected:** `content.size() == 1`, `targetType == QUESTION`.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

### PCQH-TC-003 — moderatorName được resolve đúng qua batch UserRepository.findAllById()

**Test file:** `content/service/ModerationServiceImplTest.java`

**Arrange:** Mock action có `moderatorUserId = X`; mock `userRepository.findAllById(...)` trả về `User(id=X, name="Moderator Test")`.
**Act:** Gọi `getModerationHistory(...)`.
**Expected:** `content.get(0).moderatorName() == "Moderator Test"`.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

### PCQH-TC-004 — reason được trả đúng nguyên văn (không truncate)

**Test file:** `content/service/ModerationServiceImplTest.java`

**Arrange:** Mock action có `reason = "Nội dung không phù hợp"`.
**Act:** Gọi `getModerationHistory(...)`.
**Expected:** `content.get(0).reason() == "Nội dung không phù hợp"` — khác `contentPreview` (bị truncate 200 ký tự), `reason` không qua truncate vì đã ngắn theo thiết kế nhập liệu.

**TDD Phase:** 🟢 GREEN
**Current Status:** 🟢 Passing

---

### PCQH-TC-005 — size > 50 → MOD-002 (Controller)

**Test file:** `content/controller/ModerationControllerTest.java`

**Arrange:** MockMvc `GET /history?size=51`, MODERATOR token.
**Expected:** 400, `$.error == "MOD-002"`.

**TDD Phase:** 🟢 GREEN (N/A Red Gate — controller test mocks service layer, cùng convention PCQ-TC-005/008)
**Current Status:** 🟢 Passing

---

### PCQH-TC-006 — Non-MODERATOR → 403

**Test file:** `content/controller/ModerationControllerSecurityTest.java`

**Arrange:** MockMvc `GET /history`, role MOTHER.
**Expected:** 403.

**TDD Phase:** 🟢 GREEN (N/A Red Gate — RBAC test mocks service layer, cùng convention PCQ-TC-007)
**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID       | 🔴 RED confirmed | 🟢 GREEN (commit) | Ghi chú |
| ----------- | :--------------: | :----------------: | -------- |
| PCQ-TC-001  |        [x]        |     Passed          |          |
| PCQ-TC-002  |        [x]        |     Passed          |          |
| PCQ-TC-003  |        [x]        |     Passed          |          |
| PCQ-TC-004  |        [x]        |     Passed          |          |
| PCQ-TC-005  |        N/A        |     Passed          | Controller test, mocked service — không phụ thuộc stub |
| PCQ-TC-006  |        [x]        |     Passed          |          |
| PCQ-TC-007  |        N/A        |     Passed          | RBAC test, mocked service — không phụ thuộc stub |
| PCQ-TC-008  |        N/A        |     Passed          | Controller test, mocked service — không phụ thuộc stub |
| PCQ-TC-009  |        [x]        |     Passed          |          |
| PCQH-TC-001 |        [x]        |     Passed          | EXTENSION History |
| PCQH-TC-002 |        [x]        |     Passed          | EXTENSION History |
| PCQH-TC-003 |        [x]        |     Passed          | EXTENSION History |
| PCQH-TC-004 |        [x]        |     Passed          | EXTENSION History |
| PCQH-TC-005 |        N/A        |     Passed          | Controller test, mocked service — không phụ thuộc stub |
| PCQH-TC-006 |        N/A        |     Passed          | RBAC test, mocked service — không phụ thuộc stub |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

| TC ID       | Expected  | Actual        |
| ----------- | :-------: | :------------: |
| PCQ-TC-001  | 🔴 FAIL   | ☑ FAIL ☐ PASS  |
| PCQ-TC-002  | 🔴 FAIL   | ☑ FAIL ☐ PASS  |
| PCQ-TC-003  | 🔴 FAIL   | ☑ FAIL ☐ PASS  |
| PCQ-TC-004  | 🔴 FAIL   | ☑ FAIL ☐ PASS  |
| PCQ-TC-005  | N/A — controller test mocks service, không exercise stub | N/A |
| PCQ-TC-006  | 🔴 FAIL   | ☑ FAIL ☐ PASS  |
| PCQ-TC-007  | N/A — RBAC test mocks service, không exercise stub | N/A |
| PCQ-TC-008  | N/A — controller test mocks service, không exercise stub | N/A |
| PCQ-TC-009  | 🔴 FAIL   | ☑ FAIL ☐ PASS  |

Tất cả FAIL (trong số các TC thực sự exercise stub)? `[x] Yes` `[ ] No`

Stub: `ModerationServiceImpl.getPendingContentQueue()` throw `UnsupportedOperationException`. Chạy thật: `./mvnw test -Dtest=ModerationServiceImplTest,...` trước khi implement — 6/6 TC ở service layer FAIL với `UnsupportedOperationException` (xác nhận Red Gate PASS), 3 TC ở controller/security layer PASS ngay vì chúng mock `ModerationService` hoàn toàn (đúng theo convention có sẵn của `getQueue()`/`getModerationQueue()`), không phải Green-from-Birth (AP-AI-002) vì các TC đó không test logic mới, chỉ test routing/RBAC đã khai báo bằng annotation.

**EXTENSION History (2026-07-03):** stub `getModerationHistory()` throw `UnsupportedOperationException`. Chạy `./mvnw test -Dtest=ModerationServiceImplTest,ModerationControllerTest,ModerationControllerSecurityTest` trước khi implement — 4/4 TC (PCQH-TC-001…004) FAIL đúng với `UnsupportedOperationException`, 2 TC controller/security (PCQH-TC-005/006) PASS ngay vì mock service (cùng lý do như trên, không phải AP-AI-002). Sau implement: `./mvnw test` cùng bộ 3 file → 28 test PASS (0 fail). Full suite kèm cả extension: 34/34 PASS.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)
- [x] TDS `CB-MOD-IMP-004` Status = `Approved`
- [x] Test-Spec này Status = `Approved`
- [x] Backend dev server compile thành công trước khi bắt đầu RED phase

### Exit Criteria (Điều kiện kết thúc — DoD)
- [x] Tất cả 9 test case ở §4 đều 🟢 GREEN — xác nhận qua `./mvnw test -Dtest=ModerationServiceImplTest,ModerationControllerTest,ModerationControllerSecurityTest` (28 tests, 0 failures)
- [x] `./mvnw test` toàn bộ project PASS, không regression từ thay đổi này — 11 lỗi pre-existing không liên quan (`ExerciseControllerDetailSecurityTest`/`ExerciseDetailIntegrationTest`, thiếu bean `IExerciseSafetyCheckService`) xác nhận có trước khi bắt đầu (kiểm tra bằng `git stash` + chạy lại trên code cũ)
- [x] **Verify qua UI thật, cả 2 targetType** — đăng nhập `moderator@carebridge.dev` trên Admin Web Portal qua Chrome DevTools MCP, xác nhận trang "Nội dung mới" (`/moderator/pending-content`) truy cập được từ sidebar.
  - QUESTION: hiển thị đúng câu hỏi PENDING test, bấm "Duyệt" thành công (item biến mất khỏi danh sách), xác nhận `community_questions.status` chuyển `PENDING → APPROVED` trong DB, `moderation_actions` có row mới với `report_id = NULL`, và câu hỏi xuất hiện ở đầu `GET /api/v1/community/feed` khi gọi bằng token của `mother@carebridge.dev`.
  - ANSWER: chuyển tab "Câu trả lời mới", danh sách hiển thị đúng cả câu trả lời test lẫn 1 câu trả lời PENDING có sẵn trong DB dev ("google đi b" — dữ liệu thật, không phải do tôi tạo), bấm "Duyệt" trên câu trả lời test thành công, xác nhận `community_answers.status` chuyển `PENDING → APPROVED`, `moderation_actions.report_id = NULL`.
  - Dữ liệu test (cả question và answer) đã được xoá sau khi verify.
- [x] TDS + Test-Spec được Truthful Sync theo kết quả THẬT (không bulk-mark) — xem §4/§5 ở trên

### Suspension Criteria
Nếu Red Gate không đạt (có TC pass ngay từ đầu — "Green-from-Birth", AP-AI-002) → dừng lại, sửa stub/test trước khi tiếp tục.

---

## 7. Rollback Plan

Không có migration DB — rollback chỉ cần revert code deploy về version trước (không xóa endpoint mới nếu đã có traffic thật dùng nó; theo Rollback Runbook trong TDS §12: chỉ redeploy version cũ, không cần thao tác DB).

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP ID       | Check                                                                                     | Status |
| ----------- | -------------------------------------------------------------------------------------------- | :-----: |
| AP-AI-001   | TC không test triviality (vd. "getter trả đúng giá trị đã set") — mọi TC đều test business logic thật (routing theo targetType, validation, RBAC) | [x] |
| AP-AI-002   | Green-from-Birth — Red Gate phải xác nhận method thật sự dùng stub trước khi bắt đầu implement | [x] — 6/6 TC ở service layer xác nhận FAIL với `UnsupportedOperationException` trước khi implement; 3 TC controller/security PASS ngay là đúng vì chúng mock service hoàn toàn (không phải AP-AI-002, xem §5.1) |
| AP-AI-003   | Không dùng ngôn ngữ lâm sàng/chẩn đoán trong bất kỳ đâu (N/A — module không liên quan y tế)   | [x] N/A |
| AP-AI-004   | Không lộ PII trong test assertion (PCQ-TC-006 kiểm tra đúng field, không assert giá trị authorId thật) | [x] |
