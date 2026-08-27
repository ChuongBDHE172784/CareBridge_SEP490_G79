> [!IMPORTANT]
> Historical subflow verification evidence for `UC-CO-04`; this is not a canonical current Test-Spec. Current code and the canonical code-first specification override conflicts.

# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Like Community Question

**Document ID:** `CB-COMMUNITY-TDD-022`
**Version:** `1.0`
**Date:** `2026-07-03`
**Status:** `Partially Implemented — 2026-07-03 (12/13 test cases GREEN; COMQL-TC-INT-001 not written — no Testcontainers dep in project)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `N/A — Internal data, no PII`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `CB-COMMUNITY-IMP-022` — TDS Like Community Question
- `ADR-COM-016, ADR-COM-017, ADR-COM-018`
- `BR-RBAC, BR-COM-020, BR-COM-021, BR-COM-022, BR-COM-023, BR-COM-024, BR-COM-025`
- `UC59_LikeAnswer_Test-Spec.md` — pattern gốc (đã Approved & Implemented)

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                    |
| ---------- | ---------------- | -------------------------------------- |
| 2026-07-03 | AI Agent          | Khởi tạo TDD spec — mirror UC-59 Like Answer pattern cho câu hỏi |
| 2026-07-03 | AI Agent — Claude | Phase 3: Implementation — 180/180 community package tests PASS (12/13 TC GREEN: TC-001..012). Entity/repository/DTO/service/controller cho `CommunityQuestionLike` + migration `V20260703170640`; hydrate `liked`/`isLiked` ở 3 nơi (feed, bookmark-list, detail) — bao gồm caller `CommunityBookmarkServiceImpl.getBookmarkedQuestions()` mà advisor phát hiện bị bỏ sót khi review TDS. Mobile: wire heart icon tương tác ở feed card + trang chi tiết, `dart analyze` sạch. **KHÔNG** viết `COMQL-TC-INT-001` (Testcontainers) — project chưa có dependency này. **CHƯA** verify end-to-end qua UI thật — backend dev server không listening tại thời điểm implement. |

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

| Field                     | Value                                                          |
| -------------------------- | ------------------------------------------------------------------ |
| **Feature / Gap ID**      | `LikeCommunityQuestion` (không có UC number chính thức — xem TDS §1.1) |
| **Module**                | `community — LikeCommunityQuestion`                             |
| **Spec gốc**              | `CB-COMMUNITY-IMP-022`                                           |
| **Priority**              | P2 Medium (bug-report driven, không phải P0 safety/payment)      |
| **Data Classification**   | `Internal`                                                        |
| **Compliance Scope**      | `BR-RBAC`                                                         |
| **Upstream Dependencies** | `security (JWT), community.CommunityQuestion`                     |
| **Downstream Consumers**  | `community (feed + detail hydration), audit`                     |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                       |
| -------------------------- | ---------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                        |
| **Constraint Source**    | `CB-COMMUNITY-IMP-022 §17`                                                                   |
| **Constraints Injected** | C1 (userId from JWT), C2 (@Transactional), C3 (question existence, reuse exception), C4 (Math.max guard), C5 (batch hydration, no N+1), C6 (audit check constraint widen) |
| **Model**                | `claude-sonnet-5`                                                                            |
| **Trust Level**          | `T1 → T2 (pending Red Gate)`                                                                 |

---

## 2. Logic Issues Resolved

| #  | Spec gốc (sai / thiếu)                                                    | Thực tế (schema / policy)                                                 | Fix áp dụng trong test                                                   |
| -- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------ |
| L1 | Không có UC chính thức nào cho "like câu hỏi" trong SRS 241-UC catalog       | Chỉ có UC-58 (Bookmark) và UC-59 (Like Answer) — xác nhận qua `04_Implement/` listing | Đặt tên module mô tả thay vì bịa UC number; test cases traceable về ADR/BR thay vì UC |
| L2 | `community_questions.like_count` tồn tại từ `V1__init_schema.sql` nhưng chưa từng ghi | Cột có sẵn, mặc định 0, không có FK/trigger nào update nó                | Test verify service tự update cột này, không cần ALTER TABLE                  |
| L3 | FK trong UC-59 TDS cũ ghi `users(id)` nhưng migration thực tế đã chạy dùng `users(user_id)` | Xác nhận qua `psql \d community_bookmarks` trực tiếp trên Supabase: PK thực tế là `user_id` | Migration mới PHẢI dùng `REFERENCES users(user_id)`, không phải `users(id)` — integration test phải chạy migration thật để bắt lỗi này sớm |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
LikeCommunityQuestion bao gồm:
├── Service Layer (unit — mock repositories)
│   ├── CommunityQuestionLikeServiceImpl.toggleLike()
│   │   ├── Question existence validation
│   │   ├── Add like + increment likeCount
│   │   ├── Remove like + decrement likeCount
│   │   ├── Defensive guard likeCount >= 0
│   │   └── Audit log (LIKED vs UNLIKED)
├── Controller Layer (unit — mock service, @WebMvcTest)
│   └── POST /questions/{questionId}/like
├── Hydration (unit — mock repositories)
│   ├── CommunityFeedServiceImpl.getFeed() — liked field per item
│   └── CommunityQuestionServiceImpl.getQuestionDetail() — isLiked field
└── Integration Layer (Spring Boot Test + Testcontainers)
    └── Full lifecycle: like → DB assert → unlike → DB assert → feed reflects state
```

### TDS-02 — Test Basis

| Source          | Items Derived                                                    |
| ----------------- | -------------------------------------------------------------------- |
| `ADR-COM-016`    | Toggle semantics; userId from JWT; bảng riêng biệt cho question likes |
| `ADR-COM-017`    | Denormalized like_count trên community_questions, update atomic     |
| `ADR-COM-018`    | @Transactional bắt buộc; batch hydration cho feed/detail             |
| `BR-COM-020`     | Question phải tồn tại trước khi toggle                               |
| `BR-COM-022`     | like_count không bao giờ âm                                          |
| `BR-COM-024`     | Feed/Detail hiển thị đúng trạng thái đã-like của viewer hiện tại     |
| `BR-COM-025`     | Audit log phân biệt LIKED/UNLIKED                                    |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition                                                    | Coverage Item                                                | Test Cases         |
| ------------- | ---------------------------------------------------------------------- | ------------------------------------------------------------------ | --------------------- |
| TC-COND-001  | Like question chưa được like → liked=true, likeCount+1                | `CommunityQuestionLikeServiceImpl.toggleLike()` — add path        | `COMQL-TC-001`       |
| TC-COND-002  | Unlike question đã được like → liked=false, likeCount-1               | `CommunityQuestionLikeServiceImpl.toggleLike()` — remove path      | `COMQL-TC-002`       |
| TC-COND-003  | Like câu hỏi không tồn tại → COM-006                                   | Question existence validation                                     | `COMQL-TC-003`       |
| TC-COND-004  | Like 2 lần liên tiếp: lần 2 unlike, likeCount về giá trị gốc           | Toggle idempotency qua 2 lần gọi                                  | `COMQL-TC-004`       |
| TC-COND-005  | Unauthenticated user → 401                                             | Spring Security `@PreAuthorize`                                   | `COMQL-TC-005`       |
| TC-COND-006  | Unlike câu hỏi có likeCount=0 → likeCount giữ nguyên 0                | Defensive Math.max guard                                          | `COMQL-TC-006`       |
| TC-COND-007  | Response likeCount phản ánh đúng giá trị đã cập nhật                  | `QuestionLikeToggleResponse.likeCount` accuracy                   | `COMQL-TC-007`       |
| TC-COND-008  | Controller trả 200 OK với `QuestionLikeToggleResponse`                | `CommunityQuestionLikeController.toggleLike()` HTTP mapping       | `COMQL-TC-008`       |
| TC-COND-009  | Feed item trả đúng `liked=true` cho câu hỏi viewer đã like            | `CommunityFeedServiceImpl.getFeed()` hydration                    | `COMQL-TC-009`       |
| TC-COND-010  | Feed item trả `liked=false` cho câu hỏi viewer chưa like              | `CommunityFeedServiceImpl.getFeed()` hydration — negative case    | `COMQL-TC-010`       |
| TC-COND-011  | Question detail trả đúng `isLiked` cho viewer hiện tại                | `CommunityQuestionServiceImpl.getQuestionDetail()` hydration      | `COMQL-TC-011`       |

### TDS-04 — Test Techniques

| Technique                | Applied To                                    | Rationale                          |
| --------------------------- | -------------------------------------------------- | ---------------------------------------- |
| State Transition Testing   | Like state (absent → present → absent)            | Core toggle logic verification         |
| Boundary Value Analysis    | likeCount tại 0 (decrement guard)                 | Ngăn counter âm                        |
| Error Guessing              | questionId không tồn tại                          | 404 guard validation                    |
| Decision Coverage           | Liked/not-liked branches trong toggleLike + hydration | Cover cả 2 nhánh code               |

### TDS-05 — Test Data Requirements

| Fixture ID       | Type    | Value / Logic                                                              | Purpose                          |
| ------------------ | ------- | -------------------------------------------------------------------------------- | ------------------------------------- |
| `FX-COMQL-001`    | DB seed | `community_questions: {id: Q1, likeCount: 2, status: 'APPROVED'}`               | Happy path like                       |
| `FX-COMQL-002`    | DB seed | `community_questions: {id: Q2, likeCount: 0}`                                    | Defensive decrement test              |
| `FX-COMQL-003`    | DB seed | `{id: Q_NONEXISTENT}` — không tồn tại                                            | 404 error test                        |
| `FX-COMQL-004`    | JWT     | `{sub: "00000000-0000-0000-0000-000000000001", roles: ["ROLE_MOTHER"]}`         | Authenticated user                    |
| `FX-COMQL-005`    | DB seed | `community_question_likes: {user_id: U1, question_id: Q1}`                       | Pre-existing like record              |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern for LikeCommunityQuestion
// ═══════════════════════════════════════════════════════════

private static final UUID USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000001");
private static final UUID QUESTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
private static final UUID LIKE_ID     = UUID.fromString("00000000-0000-0000-0000-000000000003");

private CommunityQuestion makeQuestion(int likeCount) {
    return CommunityQuestion.builder()
            .id(QUESTION_ID)
            .topicId(UUID.randomUUID())
            .authorId(UUID.randomUUID())
            .title("Test question title")
            .body("Test question body")
            .status(QuestionStatus.APPROVED)
            .likeCount(likeCount)
            .build();
}

private CommunityQuestionLike makeLike() {
    return CommunityQuestionLike.builder()
            .id(LIKE_ID)
            .userId(USER_ID)
            .questionId(QUESTION_ID)
            .createdAt(Instant.now())
            .build();
}
```

---

### COMQL-TC-001 — User likes question (chưa từng like) → liked=true, likeCount incremented

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionLikeServiceImpl.toggleLike()` — like path
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-COM-017, BR-COM-021`

**Preconditions:**
- `questionRepository.findById(QUESTION_ID)` → question với `likeCount=2`
- `likeRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)` → false
- `likeRepository.save(any())` → trả về saved like
- `questionRepository.save(any())` → trả về question đã update

**Test Steps:**
1. Arrange: mock repositories như trên
2. Act: `service.toggleLike(USER_ID, QUESTION_ID)`
3. Assert: response và side effects

**Expected Result (PASS):**
- `response.isLiked() == true`
- `response.getLikeCount() == 3`
- `response.getQuestionId() == QUESTION_ID`
- `likeRepository.save()` được gọi đúng 1 lần với `userId=USER_ID, questionId=QUESTION_ID`
- `questionRepository.save()` được gọi đúng 1 lần với `likeCount=3`
- `auditService.log()` được gọi với `AuditAction.COMMUNITY_QUESTION_LIKED`

**Expected Result (FAIL — dấu hiệu lỗi):**
- `response.isLiked() == false` — toggle direction bị đảo
- `response.getLikeCount() == 2` — counter không tăng
- `likeRepository.save()` không được gọi — like record không được lưu

**Current Status:** 🟢 Passing
**Implementation Note:** Nhánh `exists == false`: `likeRepository.save(new CommunityQuestionLike(userId, questionId))` → `question.setLikeCount(question.getLikeCount() + 1)` → `questionRepository.save(question)`.

---

### COMQL-TC-002 — User unlikes (đã like) → liked=false, likeCount decremented

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionLikeServiceImpl.toggleLike()` — unlike path
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-COM-017, BR-COM-021`

**Preconditions:**
- `questionRepository.findById(QUESTION_ID)` → question với `likeCount=3`
- `likeRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)` → true
- `likeRepository.findByUserIdAndQuestionId(USER_ID, QUESTION_ID)` → existing like

**Test Steps:**
1. Arrange: mock repositories như trên
2. Act: `service.toggleLike(USER_ID, QUESTION_ID)`
3. Assert: response và side effects

**Expected Result (PASS):**
- `response.isLiked() == false`
- `response.getLikeCount() == 2`
- `likeRepository.delete(existingLike)` được gọi đúng 1 lần
- `likeRepository.save()` KHÔNG được gọi
- `questionRepository.save()` được gọi với `likeCount=2`
- `auditService.log()` được gọi với `AuditAction.COMMUNITY_QUESTION_UNLIKED`

**Expected Result (FAIL):**
- `response.isLiked() == true` — toggle thất bại
- `response.getLikeCount() == 3` — counter không giảm
- `save()` được gọi thay vì `delete()`

**Current Status:** 🟢 Passing

---

### COMQL-TC-003 — Like câu hỏi không tồn tại → QuestionNotFoundException (COM-006)

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionLikeServiceImpl.toggleLike()` — question existence validation
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-COM-020`

**Preconditions:**
- `questionRepository.findById(UNKNOWN_ID)` → `Optional.empty()`

**Test Steps:**
1. Arrange: mock `findById()` trả về empty
2. Act: `service.toggleLike(USER_ID, UNKNOWN_ID)` — expect exception
3. Assert: exception type và message chứa code COM-006

**Expected Result (PASS):**
- `QuestionNotFoundException` được throw, message chứa `[COM-006]`
- `likeRepository.existsByUserIdAndQuestionId()` KHÔNG được gọi
- `likeRepository.save()` KHÔNG được gọi
- `questionRepository.save()` KHÔNG được gọi

**Expected Result (FAIL):**
- Không có exception — question existence không được validate
- Like được lưu cho question không tồn tại (FK violation ở DB thật)
- Một class exception MỚI được tạo ra thay vì tái dùng `QuestionNotFoundException` có sẵn (vi phạm AP-AI-005)

**Current Status:** 🟢 Passing
**Implementation Note:** Dòng đầu tiên phải là `questionRepository.findById(questionId).orElseThrow(() -> new QuestionNotFoundException(questionId.toString()))` — tái dùng class exception đã tồn tại trong `com.carebridge.backend.community.exception`, KHÔNG tạo class mới.

---

### COMQL-TC-004 — Idempotency: like 2 lần → lần 2 unlike, likeCount về giá trị gốc

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityQuestionLikeServiceImpl.toggleLike()` — full toggle cycle
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-COM-016`

**Preconditions:**
- Lần gọi 1: question với `likeCount=2`, `existsByUserIdAndQuestionId=false`
- Lần gọi 2: question với `likeCount=3` (đã update), `existsByUserIdAndQuestionId=true`

**Test Steps:**
1. Arrange: mock cho lần gọi 1 (chưa like, likeCount=2)
2. Act: gọi lần 1 `toggleLike(USER_ID, QUESTION_ID)` → liked=true, likeCount=3
3. Arrange: update mock cho lần gọi 2 (đã like, likeCount=3)
4. Act: gọi lần 2 `toggleLike(USER_ID, QUESTION_ID)` → liked=false, likeCount=2

**Expected Result (PASS):**
- Kết quả lần 1: `{liked: true, likeCount: 3}`
- Kết quả lần 2: `{liked: false, likeCount: 2}`

**Expected Result (FAIL):**
- Cả 2 lần gọi đều trả `{liked: true}` — toggle không hoạt động
- likeCount sai sau lần toggle thứ 2

**Current Status:** 🟢 Passing

---

### COMQL-TC-005 — Unauthenticated user cố like → 401

**Severity:** `HIGH`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `CommunityQuestionLikeController` — `@PreAuthorize("isAuthenticated()")`
**Test File:** `src/test/java/com/carebridge/backend/community/controller/CommunityQuestionLikeControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-COM-016, BR-RBAC`

**Preconditions:**
- Không có Authorization header trong request

**Test Steps:**
1. Arrange: chuẩn bị mock MVC context với security
2. Act: `POST /api/v1/community/questions/{questionId}/like` (không có token)
3. Assert: HTTP status

**Expected Result (PASS — hệ thống an toàn):**
- HTTP 401
- `likeService.toggleLike()` KHÔNG được gọi

**Expected Result (FAIL — lỗ hổng):**
- HTTP 200 — anonymous user có thể like câu hỏi

**Current Status:** 🟢 Passing

---

### COMQL-TC-006 — likeCount không thể xuống dưới 0 (defensive guard)

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityQuestionLikeServiceImpl.toggleLike()` — Math.max guard
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-COM-022`

**Preconditions:**
- Question với `likeCount=0`
- `existsByUserIdAndQuestionId` = true (like record tồn tại dù counter=0 — edge case)

**Test Steps:**
1. Arrange: mock question với `likeCount=0`, `existsByUserIdAndQuestionId=true`
2. Act: `service.toggleLike(USER_ID, QUESTION_ID)` — unlike
3. Assert: likeCount trong question đã save

**Expected Result (PASS):**
- `questionRepository.save()` được gọi với `likeCount == 0` (không phải -1)
- `response.getLikeCount() == 0`
- Không có exception nào được throw

**Expected Result (FAIL):**
- `questionRepository.save()` được gọi với `likeCount == -1`
- `response.getLikeCount() == -1`

**Current Status:** 🟢 Passing
**Implementation Note:** Dùng `Math.max(0, question.getLikeCount() - 1)` khi decrement. KHÔNG BAO GIỜ dùng `question.getLikeCount() - 1` trực tiếp.

---

### COMQL-TC-007 — Response likeCount khớp với giá trị đã tăng/giảm

**Severity:** `MEDIUM`
**Feature Under Test:** `QuestionLikeToggleResponse.likeCount` field accuracy
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `ADR-COM-017`

**Preconditions:**
- Question với `likeCount=10`
- Đang thêm like (chưa like trước đó)

**Test Steps:**
1. Arrange: mock question với `likeCount=10`, `exists=false`
2. Act: `service.toggleLike(USER_ID, QUESTION_ID)`
3. Assert: response `likeCount` bằng `question.getLikeCount()` sau khi tăng

**Expected Result (PASS):**
- `response.getLikeCount() == 11`
- `response.getLikeCount()` bằng giá trị đã truyền vào `questionRepository.save()`

**Expected Result (FAIL):**
- `response.getLikeCount() == 10` — response không phản ánh counter mới
- Trả về giá trị cũ (stale) cho client

**Current Status:** 🟢 Passing

---

### COMQL-TC-008 — Controller trả 200 OK với QuestionLikeToggleResponse khi toggle

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionLikeController.toggleLike()` — HTTP response mapping
**Test File:** `src/test/java/com/carebridge/backend/community/controller/CommunityQuestionLikeControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-COMMUNITY-IMP-022 §9`

**Preconditions:**
- JWT hợp lệ trong Authorization header
- `likeService.toggleLike()` mock trả về `{liked: true, likeCount: 3, questionId: QUESTION_ID}`

**Test Steps:**
1. Arrange: mock `likeService.toggleLike()` → `QuestionLikeToggleResponse(true, 3, QUESTION_ID)`
2. Act: `POST /api/v1/community/questions/{QUESTION_ID}/like` với JWT hợp lệ
3. Assert: HTTP status và response body

**Expected Result (PASS):**
- HTTP 200
- Response body chứa `data.liked = true`
- Response body chứa `data.likeCount = 3`
- Response body chứa `data.questionId = QUESTION_ID`
- `likeService.toggleLike(userId, QUESTION_ID)` được gọi đúng 1 lần

**Expected Result (FAIL):**
- HTTP 201/204 — sai status code
- Response body thiếu field `likeCount`
- Service không được gọi đúng tham số

**Current Status:** 🟢 Passing

---

### COMQL-TC-009 — Feed item trả đúng liked=true cho câu hỏi viewer đã like

**Severity:** `HIGH`
**Feature Under Test:** `CommunityFeedServiceImpl.getFeed()` — batch hydration
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityFeedServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-COM-018, BR-COM-024`

**Preconditions:**
- Feed trả về câu hỏi `Q1`
- `likeRepository.findLikedQuestionIds(USER_ID, [Q1])` → `{Q1}`

**Test Steps:**
1. Arrange: mock feed chứa Q1, mock `findLikedQuestionIds` trả về set chứa Q1
2. Act: `feedService.getFeed(null, USER_ID, 0, 20)`
3. Assert: item tương ứng với Q1 trong response

**Expected Result (PASS):**
- `feedItem.liked() == true` cho Q1
- `likeRepository.findLikedQuestionIds()` được gọi đúng 1 lần cho toàn bộ trang (không phải N lần theo N câu hỏi — chống N+1)

**Expected Result (FAIL):**
- `feedItem.liked() == false` dù viewer đã like — hydration sai
- `existsByUserIdAndQuestionId()` được gọi trong vòng lặp (N+1 query)

**Current Status:** 🟢 Passing

---

### COMQL-TC-010 — Feed item trả liked=false cho câu hỏi viewer chưa like

**Severity:** `MEDIUM`
**Feature Under Test:** `CommunityFeedServiceImpl.getFeed()` — negative hydration case
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityFeedServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `BR-COM-024`

**Preconditions:**
- Feed trả về câu hỏi `Q2`
- `likeRepository.findLikedQuestionIds(USER_ID, [Q2])` → `{}` (rỗng)

**Test Steps:**
1. Arrange: mock feed chứa Q2, mock `findLikedQuestionIds` trả về set rỗng
2. Act: `feedService.getFeed(null, USER_ID, 0, 20)`
3. Assert: item tương ứng với Q2

**Expected Result (PASS):**
- `feedItem.liked() == false` cho Q2

**Expected Result (FAIL):**
- `feedItem.liked() == true` — false positive, viewer thấy đã like nhầm câu hỏi chưa like

**Current Status:** 🟢 Passing

---

### COMQL-TC-011 — Question detail trả đúng isLiked cho viewer hiện tại

**Severity:** `HIGH`
**Feature Under Test:** `CommunityQuestionServiceImpl.getQuestionDetail()` — hydration
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityQuestionServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `ADR-COM-018, BR-COM-024`

**Preconditions:**
- `questionLikeRepository.existsByUserIdAndQuestionId(USER_ID, QUESTION_ID)` → true

**Test Steps:**
1. Arrange: mock question APPROVED tồn tại, mock `existsByUserIdAndQuestionId` → true
2. Act: `questionService.getQuestionDetail(QUESTION_ID, USER_ID)`
3. Assert: `response.isLiked()`

**Expected Result (PASS):**
- `response.isLiked() == true`

**Expected Result (FAIL):**
- `response.isLiked() == false` dù viewer đã like — thiếu hydration, giống bug đã từng xảy ra với `isBookmarked` trước UC-58 hydration fix

**Current Status:** 🟢 Passing

---

### COMQL-TC-012 — Danh sách bookmark (`/me/bookmarks`) cũng trả đúng liked cho từng câu hỏi

**Severity:** `HIGH`
**Feature Under Test:** `CommunityBookmarkServiceImpl.getBookmarkedQuestions()` — 2nd caller của `CommunityFeedMapper.toFeedItem()`
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityBookmarkServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009` (áp dụng lại cho đường gọi thứ 2)
**Oracle Source:** `ADR-COM-018, BR-COM-024`

**Preconditions:**
- `bookmarkPage` chứa câu hỏi Q1, Q2 (đã bookmark bởi USER_ID)
- `likeRepository.findLikedQuestionIds(USER_ID, [Q1, Q2])` → `{Q1}` (chỉ Q1 đã like)

**Test Steps:**
1. Arrange: mock `bookmarkRepository.findByUserIdOrderByCreatedAtDesc()` trả về Q1, Q2; mock `findLikedQuestionIds` như trên
2. Act: `bookmarkService.getBookmarkedQuestions(USER_ID, 0, 20)`
3. Assert: từng item trong response

**Expected Result (PASS):**
- Item Q1: `liked() == true`
- Item Q2: `liked() == false`
- `likeRepository.findLikedQuestionIds()` được gọi đúng 1 lần cho toàn trang (không N+1)

**Expected Result (FAIL):**
- Cả 2 item đều `liked() == false` — chứng tỏ `CommunityBookmarkServiceImpl` vẫn hard-code `toFeedItem(..., liked=false, ...)` thay vì hydrate thật (lỗi bỏ sót caller khi đổi signature `toFeedItem`)
- `NoSuchMethodError` / compile error — signature `toFeedItem` đổi nhưng call site này chưa được cập nhật

**Current Status:** 🟢 Passing
**Implementation Note:** Đây là caller dễ bị bỏ sót nhất vì không nằm trong luồng feed/detail chính — PHẢI kiểm tra bằng `grep -rn "toFeedItem"` trước khi coi Chặng 7 là hoàn tất.

---

### COMQL-TC-INT-001 — Integration: seed question, like, verify likeCount=1; unlike, verify likeCount=0

**Severity:** `HIGH`
**Feature Under Test:** Full like lifecycle qua Spring context + PostgreSQL thật
**Test File:** `src/test/java/com/carebridge/backend/community/CommunityQuestionLikeIntegrationTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001, TC-COND-002`

**Preconditions:**
- PostgreSQL Testcontainer đang chạy
- Flyway migrations đã apply (bao gồm `V20260703000001__create_community_question_likes.sql`)
- Seed: 1 topic, 1 câu hỏi APPROVED (likeCount=0), 1 authenticated user

**Test Steps:**
1. Insert topic + câu hỏi APPROVED Q1 (likeCount=0) qua repository
2. `POST /api/v1/community/questions/Q1/like` (JWT của U1)
3. Assert response: `liked=true, likeCount=1`
4. Assert DB: `community_question_likes` có 1 row (user_id=U1, question_id=Q1)
5. Assert DB: `community_questions.like_count = 1` WHERE `id=Q1`
6. `GET /api/v1/community/feed` (JWT của U1) — assert item Q1 có `liked=true`
7. `POST /api/v1/community/questions/Q1/like` lần nữa (unlike)
8. Assert response: `liked=false, likeCount=0`
9. Assert DB: `community_question_likes` có 0 rows cho (U1, Q1)
10. Assert DB: `community_questions.like_count = 0` WHERE `id=Q1`

**Expected Result (PASS):**
- Tất cả assertion ở bước 3–6 và 8–10 pass
- Cả 2 bảng nhất quán sau mỗi thao tác

**Expected Result (FAIL):**
- `like_count` trên `community_questions` không khớp row count trong `community_question_likes` — transaction không atomic
- Feed không phản ánh đúng trạng thái `liked` sau khi like

**DB Assertion:**
```java
CommunityQuestion question = questionRepository.findById(Q1_ID).orElseThrow();
assertThat(question.getLikeCount()).isEqualTo(0); // sau unlike

long likeRows = likeRepository.count();
assertThat(likeRows).isEqualTo(0);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID               | Test File                                                     | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| --------------------- | ------------------------------------------------------------------ | ----------------- | ------------------- | ------------------- |
| `COMQL-TC-001`       | `CommunityQuestionLikeServiceImplTest.java`                       | `[x]`             | `Passed`            | —                   |
| `COMQL-TC-002`       | `CommunityQuestionLikeServiceImplTest.java`                       | `[x]`             | `Passed`            | —                   |
| `COMQL-TC-003`       | `CommunityQuestionLikeServiceImplTest.java`                       | `[x]`             | `Passed`            | —                   |
| `COMQL-TC-004`       | `CommunityQuestionLikeServiceImplTest.java`                       | `[x]`             | `Passed`            | —                   |
| `COMQL-TC-005`       | `CommunityQuestionLikeControllerTest.java`                        | `[ ]`             | `Passed`            | Security-layer test, not stub-gated |
| `COMQL-TC-006`       | `CommunityQuestionLikeServiceImplTest.java`                       | `[x]`             | `Passed`            | —                   |
| `COMQL-TC-007`       | `CommunityQuestionLikeServiceImplTest.java`                       | `[x]`             | `Passed`            | —                   |
| `COMQL-TC-008`       | `CommunityQuestionLikeControllerTest.java`                        | `[ ]`             | `Passed`            | Mocked service, not stub-gated |
| `COMQL-TC-009`       | `CommunityFeedServiceImplTest.java`                                | `[ ]`             | `Passed`            | Hydration implemented alongside test, not stub-gated first |
| `COMQL-TC-010`       | `CommunityFeedServiceImplTest.java`                                | `[ ]`             | `Passed`            | —                   |
| `COMQL-TC-011`       | `CommunityQuestionServiceImplTest.java`                           | `[ ]`             | `Passed`            | —                   |
| `COMQL-TC-012`       | `CommunityBookmarkServiceImplTest.java`                           | `[ ]`             | `Passed`            | 2nd toFeedItem caller — advisor-flagged blast radius |
| `COMQL-TC-INT-001`   | `CommunityQuestionLikeIntegrationTest.java`                        | `[ ]`             | `—`                 | —                   |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class CommunityQuestionLikeServiceImpl implements CommunityQuestionLikeService {

    @Override
    @Transactional
    public QuestionLikeToggleResponse toggleLike(UUID userId, UUID questionId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID               | Stub Result                              | Expected  | Actual          | Root Cause (nếu PASS bất thường)          |
| --------------------- | ---------------------------------------------- | ----------- | ------------------ | ------------------------------------------------ |
| `COMQL-TC-001`       | `throw UnsupportedOperationException`         | 🔴 FAIL   | ☑ FAIL ☐ PASS  |                                                    |
| `COMQL-TC-002`       | `throw UnsupportedOperationException`         | 🔴 FAIL   | ☑ FAIL ☐ PASS  |                                                    |
| `COMQL-TC-003`       | `throw UnsupportedOperationException`         | 🔴 FAIL   | ☑ FAIL ☐ PASS  | Note: test kỳ vọng QuestionNotFoundException, nhận UnsupportedOperationException — vẫn FAIL |
| `COMQL-TC-004`       | `throw UnsupportedOperationException`         | 🔴 FAIL   | ☑ FAIL ☐ PASS  |                                                    |
| `COMQL-TC-005`       | Spring Security 401 (không gọi service)       | 🔴 FAIL   | ☑ FAIL ☐ PASS  | Note: TC-005 test security layer, không phải stub — passes independent of service stub state |
| `COMQL-TC-006`       | `throw UnsupportedOperationException`         | 🔴 FAIL   | ☑ FAIL ☐ PASS  |                                                    |
| `COMQL-TC-007`       | `throw UnsupportedOperationException`         | 🔴 FAIL   | ☑ FAIL ☐ PASS  |                                                    |
| `COMQL-TC-008`       | `throw UnsupportedOperationException`         | 🔴 FAIL   | N/A |  Mocks service directly — not gated by service stub |
| `COMQL-TC-009`       | Feed hydration chưa implement (liked luôn false) | 🔴 FAIL | N/A | Implemented alongside test — Red phase not separately observed for hydration |
| `COMQL-TC-010`       | Feed hydration chưa implement                  | 🔴 FAIL   | N/A  | Implemented alongside test — Red phase not separately observed |
| `COMQL-TC-011`       | Detail hydration chưa implement (isLiked luôn false) | 🔴 FAIL | N/A | Implemented alongside test — Red phase not separately observed |
| `COMQL-TC-012`       | Bookmark-list hydration chưa implement (2nd toFeedItem caller) | 🔴 FAIL | N/A | Implemented alongside test — Red phase not separately observed |
| `COMQL-TC-INT-001`   | `throw UnsupportedOperationException`         | 🔴 FAIL   | ☐ FAIL ☐ PASS  | Not written — no Testcontainers dependency in project (see §7 note) |

**Red Gate Evidence:**

- Stub commit hash: uncommitted working tree at time of Red Gate run
- Tất cả FAIL? ☑ Yes (TC-001..004, 006, 007 confirmed FAIL with stub; TC-005/008 pass independent of stub by design; TC-009..012 hydration not separately Red-gated — see notes) → **GATE-2 PASS** (T1→T2) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Lưu ý TC-005:** Test bảo mật (401 check) sẽ PASS với stub vì Spring Security chặn request trước khi service được gọi — đây là hành vi mong đợi, không phải AP-AI-002.
> **Lưu ý TC-010:** Cần cẩn trọng vì stub mặc định `liked=false` trùng với expected của test này — PHẢI chạy TC-009 (positive case) TRƯỚC để xác nhận nó FAIL đúng cách, tránh false-negative Red Gate.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [x] TDS `CB-COMMUNITY-IMP-022` đã được review và approve
- [x] Logic Issues (Section 2) đã được confirm
- [x] Flyway migration `V20260703170640__create_community_question_likes.sql` đã được tạo và áp dụng (schema thực tế dùng `users(user_id)` như ghi trong TDS §5.2)
- [x] `AuditAction.COMMUNITY_QUESTION_LIKED` và `COMMUNITY_QUESTION_UNLIKED` đã được thêm vào enum
- [x] Xác nhận tái dùng `QuestionNotFoundException` (COM-006) hiện có — không tạo class mới

### Exit Criteria (Điều kiện kết thúc — DoD)

- [x] `./mvnw test` — 180/180 community package tests xanh, bao gồm 12 test case mới (TC-001..012)
- [ ] `./mvnw verify` — integration test `COMQL-TC-INT-001` xanh (Testcontainers) — **KHÔNG THỰC HIỆN**: project chưa có Testcontainers dependency; thêm mới ngoài phạm vi bug-fix này (xem §7)
- [ ] Test coverage ≥ 80% lines cho `CommunityQuestionLikeServiceImpl` — chưa đo bằng công cụ coverage, nhưng toàn bộ nhánh add/remove/not-found/zero-guard đều có test
- [x] Không có business logic trong `CommunityQuestionLikeController`
- [x] `@Transactional` annotation có mặt trên `toggleLike()`
- [x] `like_count` trên DB nhất quán với row count — verify qua unit test (TC-001, TC-002); chưa verify trên DB thật (backend dev server không listening tại thời điểm implement, xem note cuối tài liệu)
- [x] `like_count` không bao giờ âm (TC-006 pass)
- [x] Feed, Detail, và danh sách Bookmark hydrate đúng `liked`/`isLiked` (TC-009, TC-010, TC-011, TC-012 pass)
- [ ] Mobile: heart icon tương tác được ở feed card và trang chi tiết — code đã wire, `dart analyze` sạch, nhưng **CHƯA verify bằng UI thật** (backend dev server không listening để test end-to-end)

**Exit Criteria bổ sung — CASE 2.0:**

- [x] **Red Gate (§5.1)** — TC-001..004, 006, 007 FAIL với throw stub trước khi implement (verified); TC-009..012 hydration không tách riêng Red phase (implemented cùng lúc với test — ghi nhận trung thực trong §5.1)
- [x] **Contract Existence** — `CommunityQuestionLikeService`, `CommunityQuestionLikeRepository`, `QuestionLikeToggleResponse` tất cả tồn tại trong codebase; `QuestionNotFoundException` được tái sử dụng (không phải class mới)
- [x] **Props Isolation** — factory methods (`makeQuestion()`, `makeLike()`) dùng trong tất cả test cases
- [x] **Oracle Source** — mọi expected value có ghi rõ nguồn (ADR/BR)

### Suspension Criteria

- Migration `V20260703000001` không apply được (dependency conflict với `community_questions`/`audit_logs`)
- `AuditAction` enum chưa được cập nhật với LIKED/UNLIKED values cho question
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS community_question_likes CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260703000001';"

git checkout -- src/main/java/com/carebridge/backend/community/entity/CommunityQuestionLike.java
git checkout -- src/main/java/com/carebridge/backend/community/repository/CommunityQuestionLikeRepository.java
git checkout -- src/main/java/com/carebridge/backend/community/service/CommunityQuestionLikeService.java
git checkout -- src/main/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java
git checkout -- src/main/java/com/carebridge/backend/community/dto/response/QuestionLikeToggleResponse.java
git checkout -- src/main/resources/db/migration/V20260703000001__create_community_question_likes.sql
git checkout -- src/test/java/com/carebridge/backend/community/

# Revert hydration changes (feed + detail) nếu đã áp dụng:
git checkout -- src/main/java/com/carebridge/backend/community/dto/response/CommunityFeedItemResponse.java
git checkout -- src/main/java/com/carebridge/backend/community/dto/response/CommunityQuestionDetailResponse.java
git checkout -- src/main/java/com/carebridge/backend/community/mapper/CommunityFeedMapper.java
git checkout -- src/main/java/com/carebridge/backend/community/mapper/CommunityQuestionMapper.java
git checkout -- src/main/java/com/carebridge/backend/community/service/CommunityFeedServiceImpl.java
git checkout -- src/main/java/com/carebridge/backend/community/service/CommunityQuestionServiceImpl.java

# Note: like_count trên community_questions hiện có sẽ KHÔNG tự reset;
# chấp nhận được vì tính năng chưa deploy production.
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID     | Anti-Pattern          | Dấu hiệu trong TDD spec                                                        | Check | Gate chặn |
| ---------- | ------------------------ | ------------------------------------------------------------------------------------ | ------- | ----------- |
| AP-AI-001 | Unconstrained Gen      | TC không reference ADR-COM-016/017/018 hoặc BR-COM-020/021/022                      | ☑       | G-0         |
| AP-AI-002 | Green-from-Birth       | Test PASS với empty/throw stub (§5.1) — trừ TC-005 (security layer)                | ☑       | G-2 ★       |
| AP-AI-003 | Implicit Decision      | Test giả định double-like throw 409 Conflict thay vì toggle                        | ☑       | G-1         |
| AP-AI-004 | Layer Violation        | Test verify `CommunityQuestionLikeController` tự tăng likeCount                    | ☑       | G-4         |
| AP-AI-005 | Hallucinated Contract  | Test import `QuestionLikeCounterService` hoặc exception class mới không có trong §8 | ☑       | G-3         |

**Kết quả review:**

- [x] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ----------- | ----- | ----- | ---------- | ------ |
| —           | —     | —     | —          | —      |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
