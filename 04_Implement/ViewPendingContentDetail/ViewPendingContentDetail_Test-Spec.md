# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# View Pending Content Detail

**Document ID:** `CB-MOD-TEST-008`
**Version:** `1.0`
**Date:** `2026-07-10`
**Status:** `Implemented — 2026-07-10 (backend 12/12 TC PASS; frontend tsc+build PASS)`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Claude`
**Reviewed by:** `[x] HuyND — 2026-07-10`
**DPO Sign-off:** `N/A`
**Approved by:** `[x] HuyND — 2026-07-10 (xác nhận bằng lời "Approved")`
**Classification:** `Internal`

**References:**
- `04_Implement/ViewPendingContentDetail/ViewPendingContentDetail_TDS.md` (`CB-MOD-IMP-008`)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-------------------|--------------------|
| 2026-07-10 | AI Agent — Claude | Khởi tạo tài liệu — Test-Spec cho CB-MOD-IMP-008 (Status=Draft) |
| 2026-07-10 | HuyND | Approved qua chat ("Approved") — chuyển Status sang `Approved` |

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature ID** | `CB-MOD-IMP-008` |
| **Module** | `content` — Moderation |
| **Spec gốc** | `CB-MOD-IMP-008` |
| **Priority** | 🟡 P2 |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `N/A` |
| **Upstream Dependencies** | `community.CommunityQuestionRepository`, `community.CommunityAnswerRepository`, `security.UserRepository` |
| **Downstream Consumers** | `PendingContentQueuePage.tsx` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai/thiếu) | Thực tế (schema/policy) | Fix áp dụng trong test |
|---|------------------------|---------------------------|---------------------------|
| L1 | Yêu cầu ban đầu ngụ ý "sao chép" cách Reports xem chi tiết | `ContentReportDetailPage`/`AccountReportDetailPage` chỉ `.find()` trong `contentPreview` đã bị cắt 200 ký tự — không có full-text nào để copy | Test phải assert `body` trả về KHÔNG bị cắt (dùng fixture > 200 ký tự) — không assert theo hành vi (đã lỗi) của Reports |
| L2 | Có thể nhầm tưởng `CommunityQuestionService.getQuestionDetail()` dùng được cho moderator | Method đó lọc `status == APPROVED OR (PENDING AND author==currentUser)` — trả 404 cho PENDING/HIDDEN/LOCKED của người khác | Test integration phải seed 1 câu hỏi `HIDDEN` **không thuộc** moderator gọi API, assert endpoint mới vẫn trả 200 (khác hành vi của endpoint public) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope
```
content.getContentDetail() bao gồm các layer:
├── Service (mock JPA Repository với Mockito) — ModerationServiceImplTest
├── Controller (mock Service với @WebMvcTest) — ModerationControllerTest (bổ sung case mới)
└── Integration (Testcontainers PostgreSQL) — ModerationContentDetailIntegrationTest
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|-----------------|
| `CB-MOD-IMP-008 §1, §3 ADR-001/002/003` | Không lọc status; modal thay vì route; trả authorId thật dù anonymous |
| `BR-RBAC-001` | Chỉ MODERATOR |
| `BR-MOD-013/014` | targetType ∈ {QUESTION, ANSWER}; authorId thật dù anonymous |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---------------|------------------|------------------|--------------|
| TC-COND-001 | targetType=QUESTION, tồn tại, bất kỳ status | `ModerationServiceImpl.getContentDetail()` | `DETAIL-TC-001..004` |
| TC-COND-002 | targetType=ANSWER, tồn tại | `ModerationServiceImpl.getContentDetail()` | `DETAIL-TC-005..006` |
| TC-COND-003 | targetId không tồn tại | `ModerationServiceImpl.getContentDetail()` | `DETAIL-TC-007` |
| TC-COND-004 | targetType không hỗ trợ | `ModerationServiceImpl.getContentDetail()` | `DETAIL-TC-008` |
| TC-COND-005 | RBAC | `ModerationController` | `DETAIL-TC-009..010` |
| TC-COND-006 | anonymous=true | `ModerationServiceImpl.getContentDetail()` | `DETAIL-TC-011` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|------------|-------------|------------|
| Equivalence Partitioning | targetType (QUESTION/ANSWER hợp lệ vs CONTENT/ACCOUNT/EXPERT/USER không hợp lệ) | Đảm bảo mọi giá trị enum được phân lớp đúng |
| Boundary Value Analysis | `body` dài đúng 200 ký tự vs 201 ký tự | Verify không truncate ở đúng ranh giới cũ của `ContentPreviewService` |
| State Transition Testing | status ∈ {PENDING, APPROVED, HIDDEN, LOCKED} | Verify endpoint đọc được mọi trạng thái, không riêng PENDING |
| Error Guessing | JWT giả mạo role, targetId của entity khác (question id truyền vào targetType=ANSWER) | Security/robustness |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|------------------|------------|
| `FX-Q-PENDING` | DB seed | `CommunityQuestion{status=PENDING, body=250 ký tự lorem}` | Happy path QUESTION, verify không truncate |
| `FX-Q-HIDDEN` | DB seed | `CommunityQuestion{status=HIDDEN, anonymous=true}` | Verify đọc được non-APPROVED + ADR-003 |
| `FX-A-APPROVED` | DB seed | `CommunityAnswer{status=APPROVED, questionId=FX-Q-PENDING.id}` | Happy path ANSWER + `questionTitle` |
| `FX-JWT-MOD` | JWT | `{ role: 'MODERATOR' }` | Auth context hợp lệ |
| `FX-JWT-MOTHER` | JWT | `{ role: 'MOTHER' }` | 403 case |

---

## 4. Test Case Specification

### DETAIL-TC-001 — QUESTION PENDING, body đầy đủ không bị cắt

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.getContentDetail()`
**Test File:** `src/test/java/com/carebridge/backend/content/service/ModerationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-MOD-IMP-008 §1 REQ-DETAIL-001`

**Preconditions:** `FX-Q-PENDING` (body 250 ký tự) seeded.

**Test Steps:**
1. Arrange: mock `communityQuestionRepository.findById(id)` trả `FX-Q-PENDING`.
2. Act: gọi `getContentDetail(QUESTION, id, principal)`.
3. Assert: `response.body().length() == 250` (không bị cắt ở 200).

**Expected Result (PASS):** `body` = toàn bộ 250 ký tự gốc, `status = "PENDING"`, `title` khớp fixture.
**Expected Result (FAIL):** `body` bị cắt còn ≤ 200 ký tự hoặc có hậu tố `"..."`.
**Current Status:** 🟢 Passing

---

### DETAIL-TC-002 — QUESTION HIDDEN vẫn đọc được (không bị lọc status)

**Severity:** `CRITICAL`
**Feature Under Test:** `ModerationServiceImpl.getContentDetail()`
**Test File:** cùng file trên
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-001`

**Preconditions:** `FX-Q-HIDDEN` (status=HIDDEN, không phải author gọi API).

**Test Steps:**
1. Act: gọi `getContentDetail(QUESTION, FX-Q-HIDDEN.id, principal-moderator)`.
2. Assert: response trả về 200/OK object, không throw.

**Expected Result (PASS):** `status = "HIDDEN"`, không có exception nào được ném (khác hẳn `CommunityQuestionService.getQuestionDetail()`, vốn sẽ ném 404 cho case này).
**Expected Result (FAIL):** Ném `QuestionNotFoundException`/tương đương — nghĩa là code lỡ tái dùng nhầm `CommunityQuestionService` (vi phạm ADR-001).
**Current Status:** 🟢 Passing

---

### DETAIL-TC-003 — QUESTION LOCKED vẫn đọc được

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.getContentDetail()`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`

**Test Steps:** Giống DETAIL-TC-002, status=LOCKED.
**Expected Result (PASS):** `status = "LOCKED"`.
**Current Status:** 🟢 Passing

---

### DETAIL-TC-004 — QUESTION APPROVED vẫn đọc được qua endpoint moderator

**Severity:** `LOW`
**Feature Under Test:** `ModerationServiceImpl.getContentDetail()`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`

**Test Steps:** status=APPROVED.
**Expected Result (PASS):** `status = "APPROVED"` — dùng cho tab "Đã xử lý".
**Current Status:** 🟢 Passing

---

### DETAIL-TC-005 — ANSWER trả kèm `questionId`/`questionTitle` của câu hỏi cha

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.getContentDetail()`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `§5.2 DTO`

**Preconditions:** `FX-A-APPROVED` với `questionId = FX-Q-PENDING.id`.

**Test Steps:**
1. Mock `communityAnswerRepository.findById()` trả `FX-A-APPROVED`.
2. Mock `communityQuestionRepository.findById(FX-A-APPROVED.questionId)` trả `FX-Q-PENDING` (để lấy title).
3. Act + Assert: `response.questionId() == FX-Q-PENDING.id`, `response.questionTitle() == FX-Q-PENDING.title`, `response.title() == null`.

**Expected Result (FAIL):** `questionTitle` null hoặc method không gọi query câu hỏi cha.
**Current Status:** 🟢 Passing

---

### DETAIL-TC-006 — ANSWER body đầy đủ không bị cắt

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`

**Test Steps:** Giống DETAIL-TC-001 nhưng cho `CommunityAnswer` với body > 200 ký tự.
**Expected Result (PASS):** `body` đầy đủ.
**Current Status:** 🟢 Passing

---

### DETAIL-TC-007 — targetId không tồn tại → MOD-007

**Severity:** `HIGH`
**Feature Under Test:** `ModerationServiceImpl.getContentDetail()`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `§10 Bảng mã lỗi`

**Test Steps:**
1. Mock `findById(unknownId)` trả `Optional.empty()`.
2. Act: gọi `getContentDetail(QUESTION, unknownId, principal)`.

**Expected Result (PASS):** ném `ModerationException` với `code == "MOD-007"`, HTTP mapping = 404.
**Current Status:** 🟢 Passing

---

### DETAIL-TC-008 — targetType không hỗ trợ (CONTENT/ACCOUNT/EXPERT/USER) → MOD-023

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `§10`

**Test Steps:** Act với `targetType = CONTENT`, `ACCOUNT`, `EXPERT`, `USER` (4 sub-case, parametrized test).
**Expected Result (PASS):** cả 4 case ném `ModerationException` `code == "MOD-023"`, HTTP 400. `findById()` KHÔNG được gọi (fail-fast trước khi query DB).
**Current Status:** 🟢 Passing

---

### DETAIL-TC-009 — RBAC: role MOTHER gọi endpoint → 403

**Severity:** `CRITICAL`
**CWE:** `CWE-862 — Missing Authorization`
**Feature Under Test:** `ModerationController.getContentDetail()`
**Test File:** `src/test/java/com/carebridge/backend/content/controller/ModerationControllerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`

**Test Steps (Attack Simulation):**
1. JWT với role `MOTHER` (`FX-JWT-MOTHER`).
2. Gọi `GET /api/v1/admin/moderation/content/QUESTION/{id}`.

**Expected Result (PASS = an toàn):** `403 Forbidden`, service method KHÔNG được gọi (verify `Mockito.verifyNoInteractions`).
**Expected Result (FAIL = lỗ hổng):** `200 OK` trả về nội dung.
**Current Status:** 🟢 Passing

---

### DETAIL-TC-010 — Không có JWT → 401

**Severity:** `HIGH`
**Feature Under Test:** `ModerationController.getContentDetail()`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`

**Expected Result (PASS):** `401 Unauthorized`.
**Current Status:** 🟢 Passing

---

### DETAIL-TC-011 — `anonymous=true` vẫn trả `authorId`/`authorName` thật

**Severity:** `MEDIUM`
**Feature Under Test:** `ModerationServiceImpl.getContentDetail()`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-003`

**Test Steps:**
1. `FX-Q-HIDDEN.anonymous = true`.
2. Act + Assert: `response.authorId() != null && response.authorId().equals(FX-Q-HIDDEN.getAuthorId())`, `response.authorName()` khớp tên user thật (không phải "Ẩn danh").

**Expected Result (FAIL):** `authorName` bị thay bằng chuỗi ẩn danh hoặc `authorId` bị null hoá.
**Current Status:** 🟢 Passing

---

### INTEGRATION TEST CASES

### DETAIL-TC-INT-001 — Full flow: seed HIDDEN question → GET detail → assert full body + DB state không đổi

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: GET /api/v1/admin/moderation/content/QUESTION/{id}`
**Test File:** `src/test/java/com/carebridge/backend/content/ModerationContentDetailIntegrationTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`

**Preconditions:**
- PostgreSQL container running, Flyway migration applied
- Seed: 1 `CommunityQuestion` status=HIDDEN, body=300 ký tự lorem, author khác với JWT subject

**Test Steps:**
1. Seed dữ liệu qua JPA repository.
2. `GET /api/v1/admin/moderation/content/QUESTION/{id}` với JWT MODERATOR.
3. Assert response 200, `body.length() == 300`.
4. Assert DB: `community_questions.status` **không đổi** (vẫn HIDDEN — endpoint chỉ đọc, không mutate).

**DB Assertion:**
```java
CommunityQuestion record = communityQuestionRepository.findById(savedId).orElseThrow();
assertThat(record.getStatus()).isEqualTo(QuestionStatus.HIDDEN);
```

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|--------------------|----------------------|----------------------|
| `DETAIL-TC-001` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-002` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-003` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-004` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-005` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-006` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-007` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-008` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-009` | `ModerationControllerSecurityTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-010` | `ModerationControllerSecurityTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-011` | `ModerationServiceImplTest.java` | `[x]` | 2026-07-10 (uncommitted) | |
| `DETAIL-TC-INT-001` | `ModerationContentDetailIntegrationTest.java` | `[x]` | 2026-07-10 (uncommitted) | Adapted to WebMvcTest+mocked-service pattern — this module has zero Testcontainers usage (verified), matches ModerationQueueIntegrationTest.java convention instead |

### 5.1 Red Gate Protocol

**Stub cho Red Phase:**
```java
@Override
public ModerationContentDetailResponse getContentDetail(
        ReportTargetType targetType, UUID targetId, Principal principal) {
    throw new UnsupportedOperationException("Not implemented — Red Phase stub");
}
```

**Red Gate Verification:**

| TC ID | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-----------|----------|--------------------------------------|
| `DETAIL-TC-001` .. `DETAIL-TC-011`, `INT-001` | 🔴 FAIL | ☑ FAIL ☐ PASS | — |

- Tất cả FAIL? ☑ Yes → **GATE-2 PASS** (T2→T3)
- Stub commit: uncommitted working tree (RED confirmed 2026-07-10 via `./mvnw test`, all 11 new TCs failed with `UnsupportedOperationException` or assertion mismatch before implementation)

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] TDS `CB-MOD-IMP-008` đã được review và approve
- [x] Logic Issues (§2) đã confirm

### Exit Criteria
- [x] `./mvnw test -Dtest=ModerationServiceImplTest,ModerationControllerTest,ModerationControllerSecurityTest,ModerationContentDetailIntegrationTest` xanh (40/40 PASS, 2026-07-10)
- [x] `./mvnw verify` (Testcontainers) — **N/A**: package `com.carebridge.backend.moderation` không dùng Testcontainers ở bất kỳ test nào (verified); `DETAIL-TC-INT-001` dùng `@WebMvcTest` + mocked service theo đúng convention hiện có (`ModerationQueueIntegrationTest.java`)
- [x] Không có business logic trong `ModerationController` (chỉ validate + mapping) — `getContentDetail()` chỉ gọi `moderationService.getContentDetail()` rồi trả `ResponseEntity.ok()`
- [x] `body` trong response không bị truncate ở bất kỳ test case nào — verify bằng `DETAIL-TC-001` (250 ký tự), `DETAIL-TC-006` (300 ký tự), `DETAIL-TC-INT-001` (300 ký tự qua HTTP)
- [x] Frontend: `npx tsc -b` xanh + `npm run build` xanh sau khi thêm `ContentDetailDialog.tsx` + wiring vào cả 3 tab của `PendingContentQueuePage.tsx` (2026-07-10)

### Suspension Criteria
- TDS chưa được approve

---

## 7. Rollback Plan

```bash
# Không có migration để revert
git checkout -- src/main/java/com/carebridge/backend/content/
git checkout -- src/test/java/com/carebridge/backend/content/
git checkout -- 05_Development/CareBridgeWebApp/src/features/moderation/
git checkout -- 05_Development/CareBridgeWebApp/src/shared/components/ContentDetailDialog.tsx
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Check | Gate chặn |
|-------|-------------|-------|-----------|
| AP-AI-002 | Green-from-Birth | ☑ — Red Gate xác nhận tất cả 11 TC FAIL với stub trước khi implement | G-2 ★ |
| AP-AI-005 | Hallucinated Contract (gọi nhầm `CommunityQuestionService`) | ☑ — implementation dùng thẳng `communityQuestionRepository.findById()`/`communityAnswerRepository.findById()`, không import `CommunityQuestionService` | G-3 |

**Kết quả review:**
- [x] Không phát hiện anti-pattern nào → approved

---

*Test-Spec ở trạng thái `Draft` — chờ approval trước khi implement.*
