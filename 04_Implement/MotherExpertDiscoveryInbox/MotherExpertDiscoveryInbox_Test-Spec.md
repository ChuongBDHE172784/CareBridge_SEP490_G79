# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Mother → Expert Discovery & Inbox — Test Specification

**Document ID:** `CB-EXPCHAT-IMP-001-TS`
**Version:** `1.2`
**Date:** `2026-07-15`
**Status:** `Approved` — User approved 2026-07-16 after round 2 (9/9 Consistency Gate PASS)
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect`
**Reviewed by:** `User — Request Changes round 2, 2026-07-16 (4 điểm, xem CHANGELOG)`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/MotherExpertDiscoveryInbox/MotherExpertDiscoveryInbox_TDS.md` (this feature's TDS — `CB-EXPCHAT-IMP-001`)
- `04_Implement/UC144_DirectConsultChat/UC144_DirectConsultChat_TDS.md` (extended, not replaced)
- `04_Implement/UC80_ViewExpertDirectory/UC80_ViewExpertDirectory_TDS.md` (superseded on path/pagination — see TDS §3 ADR-MEDI-001)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/` — schema oracle

> **Quy ước TDD:** viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵. Không mark ✅ nếu `./mvnw test`/`flutter test` chưa xanh thật.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-15 | AI Agent — Technical Architect | v1.0 — Khởi tạo Test-Spec cho `CB-EXPCHAT-IMP-001`, 18 backend TC + 11 Flutter TC, bám sát danh sách test tối thiểu do user yêu cầu trong brief gốc §11. |
| 2026-07-16 | AI Agent — Technical Architect | v1.1 — **Request Changes (user, vẫn Draft).** Đồng bộ với TDS v1.1: MEDI-TC-012 viết lại hoàn toàn cho hợp đồng `lastSeenMessageId` (không còn `now()`); thêm **MEDI-TC-019** (race regression — message đến giữa lúc load timeline và lúc mark-read vẫn unread); thêm **MEDI-TC-020/021** (idempotency DB-level — duplicate-listener tuần tự và concurrent); mở rộng **MEDI-TC-014b** để assert cả FAILED `NotificationRecord` khi FCM lỗi (không chỉ assert message không mất); sửa **MEDI-TC-014a** về đúng field order `ConversationEventDomainEvent`; thêm **MEDI-TC-001b** (GET single profile có `displayName`) và **MEDI-TC-002b** (`q` > 100 → 400 `MEDI-001`) — 2 gap coverage phát hiện khi tự chạy Consistency Gate CG-1/CG-2 (§9, mới), không phải theo yêu cầu gốc nhưng sửa cùng lúc vì đã đang sửa 2 test liên quan. Thêm §9 Consistency Gate (CG-1..9) — self-check TDS ↔ Test-Spec, báo cáo riêng CG-2/CG-3/CG-7/CG-9 theo yêu cầu. |
| 2026-07-16 | AI Agent — Technical Architect | v1.2 — **Request Changes round 2 (user, vẫn Draft).** Đóng 4 gap còn lại + re-run CG-1..9 tới 9/9 PASS: (1) **MEDI-TC-022 (mới)** — `PATCH /read` với `conversationId` không tồn tại → `404`, oracle chuyển từ `MEDI-003` (hư cấu) sang `DCC-006` (tái dùng `conversationNotFound()` có thật) sau khi grounding lại bằng đọc `DirectConversationServiceImpl.java`/`DirectChatException.java`/`GlobalExceptionHandler.java`; assert cả unit-level (`markMotherRead`/`markExpertRead` không bao giờ bị gọi) lẫn HTTP-level, và assert 1 conversation khác (sibling) hoàn toàn không bị ảnh hưởng. (2) **MEDI-TC-023 (mới)** — thiếu/`null` `lastSeenMessageId` → `400 VALIDATION_ERROR` generic (Bean Validation `@NotNull`, không phải business code); assert service `markRead` không bao giờ được invoke. (3) **MEDI-TC-014b viết lại 2 bước cuối** — tách 2 oracle `attemptCount` riêng biệt, có nguồn rõ từ TDS ADR-MEDI-004 mục 4: nhánh exception dùng sentinel `0` (message thứ 2), nhánh graceful-failure dùng `delivery.attempts()` cụ thể (`3`, message thứ 3) — không còn "attemptCount khớp giá trị mock trả về" mơ hồ như v1.1. (4) **CG-7 chuyển PASS dứt khoát** — `occurredAt` xác nhận không được `DirectMessageNotificationListener` tiêu thụ, chỉ `ConversationEventPublisherImpl` (UC144, không đổi) dùng. **Sửa lan toả:** MEDI-TC-012 (bước 4-5 gộp thành 1 oracle `DCC-006` duy nhất — tránh rò rỉ cross-conversation, theo phát hiện khi thiết kế MEDI-TC-022), MEDI-TC-013 (`MEDI-002`→`DCC-003`, làm rõ "true third-party" khác với expert-de-approved/`DCC-002`), MEDI-TC-002b/004 (`MEDI-001`/`EXP-010` → `VALIDATION_ERROR` generic, `EXP-010` xác nhận **chưa từng implement thật** trong `UC80_ViewExpertDirectory` — TDS Draft cũ, chưa đối chiếu code). Thêm L10/L11 vào §2 Logic Issues. §9 Consistency Gate viết lại toàn bộ — 9/9 PASS. |
| 2026-07-16 | Codex code review | v1.4 — User-approved post-review correction: composite read cursors `(created_at, message_id)` provide a deterministic total order; MESSAGE notification rows now act as a durable DB outbox (`PENDING`/`PROCESSING`) committed before FCM with scheduled recovery; MESSAGE preference opt-out, participant validation, and deterministic directory/inbox ordering are enforced. Actual timestamped Flyway filenames are synchronized throughout the artifacts. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)
9. [Consistency Gate (CG-1..CG-9) — TDS ↔ Test-Spec](#9-consistency-gate-cg-1cg-9--tds--test-spec) ⭐ *Bổ sung theo yêu cầu*

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `GAP-MEDI-001` |
| **Module** | `expert` + `directchat` + `notification` (cross-cutting) |
| **Spec gốc** | `CB-EXPCHAT-IMP-001` |
| **Priority** | 🔴 P0 |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA` |
| **Upstream Dependencies** | `UC144_DirectConsultChat` (Approved, extended), `UC80_ViewExpertDirectory` (Draft, superseded on path/pagination) |
| **Downstream Consumers** | Mobile app (Mother, Expert) |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXPCHAT-IMP-001_TDS.md §15` |
| **Constraints Injected** | C1–C12 (xem TDS §15.1 — C9–C12 thêm ở v1.1) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T1 → T2 (pending user Approve)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|---|---|---|
| L1 | `UC80_ViewExpertDirectory` TDS §9 ghi path `GET /api/v1/expert-directory` | Code thật: `GET /api/v1/expert/directory` (`ExpertProfileController.java:62`) | Toàn bộ test dùng path thật, không theo TDS cũ |
| L2 | (giả định thường gặp) phân trang directory hoạt động đúng | `ExpertProfileServiceImpl.getPublicDirectory` bọc `List` không phân trang vào `PageImpl` — `page`/`size` bị bỏ qua | MEDI-TC-003 encode hành vi ĐÚNG (sau khi sửa), không test hành vi bug cũ |
| L3 | (giả định) avatar directory có giá trị | `ExpertProfileMapper.toDirectoryResponse` luôn set `avatarUrl=null` (dùng nhầm overload) | MEDI-TC-004 assert avatar khác null khi user có `avatar_url` |
| L4 | (giả định) gửi tin nhắn tạo notification | `DirectMessageServiceImpl.sendMessage` chỉ publish Firestore event, không tạo `NotificationRecord`, không gọi FCM | MEDI-TC-014a/014b encode hành vi MỚI (sau khi thêm ADR-MEDI-004) |
| L5 | (giả định) có sẵn read/unread state | Không tồn tại ở bất kỳ đâu trong `directchat` (xác nhận grep 0 kết quả) | MEDI-TC-011, 012, 019 test schema/endpoint hoàn toàn mới |
| L6 | v1.0 của TDS này định cho `PATCH /read` set cursor = server `now()` | Race thật: message có thể đến giữa lúc client load timeline và lúc request `PATCH /read` tới server — `now()` luôn ≥ thời điểm message đó tồn tại dù client chưa từng thấy nó | MEDI-TC-012 viết lại + MEDI-TC-019 (mới) encode cursor neo vào `lastSeenMessageId.createdAt` |
| L7 | v1.0 của TDS này định gọi `notifyNewMessage` đồng bộ trong transaction `sendMessage()` | Spring transaction semantics: exception/constraint-violation trong cùng transaction có thể làm rollback message đã gửi | MEDI-TC-014b (đã có, mở rộng thêm assertion FAILED-record) |
| L8 | v1.0 của TDS này coi `clientMessageId` early-return là đủ để chống trùng notification | Early-return chỉ chặn retry ở tầng `sendMessage()`, không chặn listener chạy trùng/đua ở tầng consumer | MEDI-TC-020 (sequential), MEDI-TC-021 (concurrent) — mới |
| L9 | v1.0 của TDS này viết ví dụ `ConversationEventDomainEvent("MESSAGE_SENT", conversationId, messageId, senderUserId)` | Field order thật (đọc `event/ConversationEventDomainEvent.java`): `(eventType, conversationId, actorUserId, resourceId, occurredAt)` | MEDI-TC-014a sửa lại đúng thứ tự |
| L10 | v1.0/v1.1 của TDS này "tái dùng `EXP-010`" (theo `UC80_ViewExpertDirectory` TDS) cho `size` > 50 | Đọc trực tiếp `GlobalExceptionHandler.java`: `@Max(50)` trên `size` chỉ sinh `ConstraintViolationException` → response generic `VALIDATION_ERROR`, KHÔNG có `@ExceptionHandler` nào ánh xạ sang `EXP-010`. `EXP-010` **chưa từng được implement** — chỉ tồn tại trong văn bản Draft chưa đối chiếu code của UC80 | MEDI-TC-004 sửa oracle sang `VALIDATION_ERROR` generic, không assert code hư cấu |
| L11 | v1.1 của TDS này định dùng 4 mã mới `MEDI-002..005` cho các lỗi `PATCH /read` (non-participant, conversation-not-found, message-wrong-conversation, message-not-found) | Đọc trực tiếp `DirectChatException.java`: `directchat` package đã có sẵn `notParticipant()`(`DCC-003`) và `conversationNotFound()`(`DCC-006`, đã dùng ở `getConversation()`/`findOrCreate()`) cho đúng 2 trong 4 tình huống; 2 tình huống còn lại (message không tồn tại / thuộc conversation khác) gộp thành 1 kết quả `DCC-006` (factory mới `messageNotInConversation()`, không phải code mới) để tránh rò rỉ cross-conversation | MEDI-TC-012 (gộp bước 4-5), MEDI-TC-013 (`DCC-003`), MEDI-TC-022/023 (mới) đều dùng oracle đã verify với code thật |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
Backend:
├── Unit — ExpertProfileServiceImpl, ExpertProfileMapper (Mockito)
├── Unit — DirectConversationServiceImpl, DirectMessageServiceImpl, DirectMessageNotificationService (Mockito)
├── Integration — @SpringBootTest + Testcontainers PostgreSQL cho migration + aggregate query thật
└── Controller — @WebMvcTest cho RBAC/403/404

Mobile (Flutter):
├── Widget test — nav shells (MOTHER, EXPERT)
├── Widget test — ExpertDirectoryScreen, ConversationListScreen states
└── Unit test — DirectChatService mới (markRead, getUnreadSummary), model parsing
```

### TDS-02 — Test Basis

| Source | Items Derived |
|---|---|
| `CB-EXPCHAT-IMP-001` TDS §3 (ADR-MEDI-001..005) | Logic mỗi test case |
| `BR-MEDI-001..005` | Business rule assertions |
| BR-DCC-003/007/010 (UC144, không đổi) | Regression guard — đảm bảo không phá policy cũ |
| PDPA / BR-PRIVACY | MEDI-TC-004 (không avatar sai), MEDI-TC-014a (không leak message body) |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---|---|---|---|
| TC-COND-001 | Directory + single-profile chỉ trả APPROVED + tìm kiếm/phân trang/validation đúng | `ExpertProfileServiceImpl` | MEDI-TC-001, 001b, 002, 002b, 003, 004, SEC-001 |
| TC-COND-002 | Conversation creation/access RBAC không đổi | `DirectConversationController` | MEDI-TC-005..007 |
| TC-COND-003 | Conversation summary đủ dữ liệu, không N+1 | `DirectConversationServiceImpl.listMyConversations` | MEDI-TC-008, 009, 017 |
| TC-COND-004 | Read cursor neo vào `lastSeenMessageId` (không phải `now()`), idempotent, chống race, an toàn concurrent, validate ownership đúng thứ tự | `markRead`, `getUnreadSummary` | MEDI-TC-010..013, 018, 019, 022, 023 |
| TC-COND-005 | Notification tạo đúng, idempotent ở tầng DB (không chỉ `clientMessageId`), FCM lỗi luôn để lại đúng 1 record FAILED, không leak PII | `DirectMessageNotificationService`, `NotificationRecordWriter` | MEDI-TC-014a, 014b, 015, 016, 020, 021 |
| TC-COND-006 | Mobile navigation/UI đúng theo role | Flutter widgets | MEDI-FL-01..11 |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|---|---|---|
| Equivalence Partitioning | role (MOTHER/EXPERT/neither), verificationStatus (APPROVED/khác) | Phân vùng RBAC |
| Boundary Value Analysis | `q` = 100/101 ký tự (MEDI-TC-002b), `unreadCount` = 0/1, `lastSeenMessageId` thuộc/không thuộc conversation | Validation + badge hiển thị |
| State Transition Testing | `verificationStatus` APPROVED→SUSPENDED giữa conversation | MEDI-TC-016 |
| Error Guessing | Concurrent `PATCH /read` từ 2 request cùng lúc; message xen giữa load-timeline và mark-read; 2 listener chạy trùng cho cùng message (tuần tự + concurrent) | MEDI-TC-012, MEDI-TC-019, MEDI-TC-020, MEDI-TC-021 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|---|---|---|---|
| `FX-M1` | DB seed | Mother user, `role=MOTHER` | Actor gửi tin |
| `FX-E1` | DB seed | Expert user + `expert_profiles.verification_status=APPROVED` | Actor nhận, participant hợp lệ |
| `FX-E2` | DB seed | Expert user + `verification_status=SUSPENDED` | MEDI-TC-016, MEDI-TC-001 (loại khỏi directory) |
| `FX-C1` | DB seed | `direct_conversations` giữa FX-M1/FX-E1, `mother_last_read_at=NULL`, `expert_last_read_at=NULL` | Baseline unread test |
| `FX-MSG1..3` | DB seed | 3 `direct_messages` trong FX-C1, `sender_user_id` xen kẽ M/E | Unread count assertions |

---

## 4. Test Case Specification

> **TC ID format:** `MEDI-TC-[NNN]` (backend), `MEDI-FL-[NN]` (Flutter). **Status:** 🔴 Not written cho tới khi thật sự viết + chạy.

### Props Isolation Boilerplate

```java
class MediTestFactory {
    static DirectConversation makeConversation(Consumer<DirectConversation> overrides) {
        DirectConversation c = DirectConversation.builder()
            .id(UUID.randomUUID())
            .motherUserId(UUID.randomUUID())
            .expertUserId(UUID.randomUUID())
            .status("ACTIVE")
            .createdAt(Instant.now())
            .build();
        overrides.accept(c);
        return c;
    }

    static DirectMessage makeMessage(UUID conversationId, UUID senderId, Consumer<DirectMessage> overrides) {
        DirectMessage m = DirectMessage.builder()
            .id(UUID.randomUUID())
            .conversationId(conversationId)
            .senderUserId(senderId)
            .clientMessageId(UUID.randomUUID())
            .messageType("TEXT")
            .messageBody("test body")
            .createdAt(Instant.now())
            .build();
        overrides.accept(m);
        return m;
    }
}
```

---

### MEDI-TC-001 — Directory chỉ trả expert APPROVED, có `displayName` + `avatarUrl` thật

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileServiceImpl.getPublicDirectory()`
**Test File:** `expert/service/ExpertProfileServiceImplDirectoryTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-MEDI-001`

**Preconditions:** FX-E1 (APPROVED), FX-E2 (SUSPENDED) đã seed; cả 2 đều có `users.full_name`/`avatar_url` khác null.

**Test Steps:**
1. Gọi `getPublicDirectory(filter, pageable)` không truyền `q`/`specialty`.
2. Assert response chứa FX-E1, không chứa FX-E2.
3. Assert từng item có `displayName` khớp `users.full_name`, `avatarUrl` khớp `users.avatar_url` (không null).

**Expected Result (PASS):** Danh sách đúng 1 phần tử (FX-E1), field đầy đủ.
**Expected Result (FAIL):** FX-E2 xuất hiện, hoặc `avatarUrl`/`displayName` null dù user có dữ liệu.
**Current Status:** 🔴 Not written

---

### MEDI-TC-001b — `GET /api/v1/expert/profiles/{expertProfileId}` response có `displayName` thật (CG-2 coverage — endpoint riêng, không phải directory)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertProfileServiceImpl.getPublicProfile` → `ExpertProfileMapper.toDetailResponse`
**Test File:** `expert/controller/ExpertProfileControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §9.1 — GET /expert/profiles/{id} "Sửa — response có displayName"`

**Preconditions:** FX-E1 (APPROVED), `users.full_name = "Nguyễn Văn A"`.

**Test Steps:** `GET /api/v1/expert/profiles/{FX-E1.expertProfileId}` → assert `200`, `displayName == "Nguyễn Văn A"`.
**Expected Result (FAIL):** `displayName` null/thiếu trong response single-profile (khác với directory list — đây là endpoint riêng, dùng `toDetailResponse`, phải test độc lập, không thể suy ra từ MEDI-TC-001).
**Current Status:** 🔴 Not written

---

### MEDI-TC-002 — Tìm kiếm `q` khớp full_name/professionalTitle/workplace, case-insensitive

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertProfileRepository.searchDirectory`
**Test File:** `expert/repository/ExpertProfileRepositorySearchIT.java` (Testcontainers)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-MEDI-001 mục 2`

**Preconditions:** FX-E1 có `users.full_name = "Nguyễn Văn A"`, `workplace = "BV Từ Dũ"`.

**Test Steps:**
1. `searchDirectory(null, "nguyễn", pageable)` → assert chứa FX-E1.
2. `searchDirectory(null, "TỪ DŨ", pageable)` → assert chứa FX-E1 (khớp `workplace`, case-insensitive).
3. `searchDirectory(null, "khong-ton-tai-xyz", pageable)` → assert rỗng.

**Expected Result (PASS):** Cả 3 case đúng như mô tả.
**Expected Result (FAIL):** Case-sensitive không khớp, hoặc không tìm được theo workplace.
**Current Status:** 🔴 Not written

---

### MEDI-TC-002b — `q` > 100 ký tự → `400 VALIDATION_ERROR` chung (boundary, CG-1 coverage)

**Severity:** `LOW`
**Feature Under Test:** `ExpertProfileController`
**Test File:** `expert/controller/ExpertProfileControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §10 (v1.2)` — xác nhận bằng đọc `GlobalExceptionHandler.java`: `@Size` trên `@RequestParam` → `ConstraintViolationException` → `handleConstraintViolation` → response generic, **không có business code riêng** (v1.1 gọi đây là `MEDI-001` — đã retire, không phải code thật)

**Test Steps:**
1. `GET /api/v1/expert/directory?q=` + chuỗi 101 ký tự → assert `400`, `error == "VALIDATION_ERROR"`, `details[0].field == "getPublicDirectory.q"` (hoặc tên field thật theo `ConstraintViolationException` sinh ra — xác nhận chính xác khi viết test thật, không đoán trước tên path).
2. `GET /api/v1/expert/directory?q=` + chuỗi đúng 100 ký tự → assert `200` (boundary hợp lệ, không reject).

**Expected Result (FAIL):** Response chứa 1 business code tuỳ chỉnh (vd `EXP-`/`MEDI-` prefix) thay vì response `VALIDATION_ERROR` generic mà `GlobalExceptionHandler` thực sự sinh ra — dấu hiệu code không khớp hành vi thật.
**Current Status:** 🔴 Not written

---

### MEDI-TC-003 — Phân trang `page`/`size` hoạt động thật (regression cho bug đã xác nhận)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileServiceImpl.getPublicDirectory` + `ExpertProfileController`
**Test File:** `expert/service/ExpertProfileServiceImplDirectoryTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `ADR-MEDI-001` (Logic Issue L2)

**Preconditions:** Seed 3 expert APPROVED (FX-E1, FX-E3, FX-E4).

**Test Steps:**
1. Gọi directory với `page=0, size=2`.
2. Assert response `experts.size() == 2` (KHÔNG phải 3 — bug cũ trả toàn bộ bất kể size).
3. Gọi `page=1, size=2` → assert `experts.size() == 1`, không trùng 2 phần tử của trang trước.
4. Assert `totalElements == 3` ở cả 2 lần gọi.

**Expected Result (PASS):** Đúng số lượng theo từng trang, không trùng lặp, `totalElements` nhất quán.
**Expected Result (FAIL):** Trang nào cũng trả đủ 3 phần tử (hành vi bug cũ).
**Current Status:** 🔴 Not written

---

### MEDI-TC-004 — `size > 50` → `400 VALIDATION_ERROR` chung (regression guard, sửa oracle sai từ v1.0/v1.1)

**Severity:** `LOW`
**Feature Under Test:** `ExpertProfileController`
**Test File:** `expert/controller/ExpertProfileControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §10 (v1.2)` — `@Max(50)` trên `size` (`ExpertProfileController.java:67`, xác nhận bằng đọc code thật) không có `@ExceptionHandler` custom nào ánh xạ sang code riêng; chỉ generic `ConstraintViolationException` → `handleConstraintViolation`. **v1.0/v1.1 của tài liệu này ghi sai** ("`EXP-010`" — tái dùng theo `UC80_ViewExpertDirectory` TDS, một tài liệu Draft chưa từng đối chiếu với code thật) — đã sửa.

**Test Steps:** `GET /api/v1/expert/directory?size=51` → assert `400`, `error == "VALIDATION_ERROR"` — **không** assert bất kỳ business code nào (không tồn tại).
**Current Status:** 🔴 Not written

---

### MEDI-TC-005 — Find-or-create idempotent (regression, không đổi từ UC144)

**Severity:** `HIGH`
**Feature Under Test:** `DirectConversationServiceImpl.findOrCreate`
**Test File:** `directchat/service/DirectConversationServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-DCC-002 (UC144, không đổi)`

**Test Steps:** Gọi `findOrCreate(motherId, expertProfileId)` 2 lần liên tiếp → assert cùng `conversationId`, DB chỉ có 1 dòng `direct_conversations` cho cặp này.
**Current Status:** 🔴 Not written

---

### MEDI-TC-006 — EXPERT gọi `findOrCreate` → 403

**Severity:** `CRITICAL`
**CWE:** `CWE-863 — Incorrect Authorization`
**Feature Under Test:** `DirectConversationController`
**Test File:** `directchat/controller/DirectConversationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-MEDI-005`

**Test Steps:** JWT role=EXPERT, `POST /api/v1/direct-conversations/expert/{id}` → assert `403`.
**Expected Result (FAIL = lỗ hổng):** 200/201 — Expert tự tạo được conversation.
**Current Status:** 🔴 Not written

---

### MEDI-TC-007 — User ngoài conversation → 403 trên mọi endpoint tham gia

**Severity:** `CRITICAL`
**CWE:** `CWE-863`
**Feature Under Test:** `DirectConversationPolicy.assertIsParticipant` (regression, không đổi)
**Test File:** `directchat/policy/DirectConversationPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`

**Test Steps:** User thứ 3 (không phải mother/expert của FX-C1) gọi `GET /direct-conversations/{id}`, `GET .../timeline`, `PATCH .../read` → assert cả 3 đều `403`.
**Current Status:** 🔴 Not written

---

### MEDI-TC-008 — `listMyConversations` trả đúng counterpart cho cả 2 role, đủ field mới

**Severity:** `HIGH`
**Feature Under Test:** `DirectConversationServiceImpl.listMyConversations`
**Test File:** `directchat/service/DirectConversationServiceImplSummaryTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-MEDI-002`

**Preconditions:** FX-C1 (Mother FX-M1 ↔ Expert FX-E1, specialty="Sản khoa").

**Test Steps:**
1. Gọi `listMyConversations(FX-M1.userId)` → assert `counterpartRole=EXPERT`, `counterpartDisplayName` = FX-E1's `full_name`, `counterpartSpecialty="Sản khoa"`.
2. Gọi `listMyConversations(FX-E1.userId)` → assert `counterpartRole=MOTHER`, `counterpartDisplayName` = FX-M1's `full_name`, `counterpartSpecialty == null`.

**Expected Result (FAIL):** `counterpartSpecialty` xuất hiện khi counterpart là MOTHER, hoặc `counterpartDisplayName` null/rỗng dù user có `full_name`.
**Current Status:** 🔴 Not written

---

### MEDI-TC-009 — `listMyConversations` sắp xếp `lastActivityAt DESC`

**Severity:** `MEDIUM`
**Feature Under Test:** `DirectConversationRepository.findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc`
**Test File:** `directchat/repository/DirectConversationRepositorySortIT.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`

**Test Steps:** Seed 3 conversation với `lastActivityAt` khác nhau theo thứ tự ngẫu nhiên → gọi list → assert thứ tự trả về giảm dần đúng theo `lastActivityAt`.
**Current Status:** 🔴 Not written

---

### MEDI-TC-010 — Gửi message cập nhật `lastActivityAt` và `lastMessagePreview`

**Severity:** `MEDIUM`
**Feature Under Test:** `DirectMessageServiceImpl.sendMessage` + `listMyConversations`
**Test File:** `directchat/service/DirectMessageServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`

**Test Steps:** Gửi message body "Xin chào bác sĩ" trong FX-C1 → gọi lại `listMyConversations` → assert `lastMessagePreview` chứa nội dung, `lastMessageAt`/`lastActivityAt` cập nhật thời điểm gửi.
**Current Status:** 🔴 Not written

---

### MEDI-TC-011 — Gửi message tăng `unreadCount` của recipient, KHÔNG tăng của sender

**Severity:** `CRITICAL`
**Feature Under Test:** `sendMessage` + aggregate unread query
**Test File:** `directchat/service/DirectMessageServiceImplUnreadTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-MEDI-003`

**Test Steps:**
1. FX-M1 gửi message trong FX-C1 (`mother_last_read_at=NULL`, `expert_last_read_at=NULL`).
2. `listMyConversations(FX-E1.userId)` → assert `unreadCount == 1`.
3. `listMyConversations(FX-M1.userId)` → assert `unreadCount == 0` (sender không tự tính unread tin nhắn của chính mình).

**Expected Result (FAIL):** Sender's `unreadCount` cũng tăng, hoặc recipient's `unreadCount` không tăng.
**Current Status:** 🔴 Not written

---

### MEDI-TC-012 — `PATCH /read` neo cursor vào `lastSeenMessageId.createdAt` (KHÔNG phải `now()`), validate ownership, idempotent, monotonic

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectConversationServiceImpl.markRead(conversationId, currentUserId, lastSeenMessageId)`
**Test File:** `directchat/service/DirectConversationServiceImplReadTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-MEDI-003 v1.2`

**Preconditions:** FX-C1 (Mother FX-M1 ↔ Expert FX-E1), messages M1 (`createdAt=t0`), M2 (`createdAt=t1 > t0`), cả hai do FX-M1 gửi, `expert_last_read_at=NULL`. `M-other` = 1 message hợp lệ nhưng thuộc 1 conversation KHÁC hoàn toàn (không phải FX-C1).

**Test Steps:**
1. `markRead(FX-C1.id, FX-E1.userId, M1.id)` → assert method trả `t0` (KHÔNG phải `Instant.now()` tại thời điểm gọi), và `expert_last_read_at == t0` trong DB.
2. Gọi lại `markRead(FX-C1.id, FX-E1.userId, M2.id)` (message mới hơn) → assert `expert_last_read_at == t1` (tiến lên).
3. Gọi lại lần 3 với `M1.id` (cũ hơn cursor hiện tại `t1`) → assert `expert_last_read_at` **vẫn là `t1`**, KHÔNG bị kéo lùi về `t0` (monotonic, `GREATEST`).
4. **`lastSeenMessageId` thuộc conversation khác:** `markRead(FX-C1.id, FX-E1.userId, M-other.id)` → assert throw `DirectChatException` với `code == "DCC-006"`, `httpStatus == 404` (tái dùng, KHÔNG phải mã mới — xem TDS §10 v1.2), `expert_last_read_at` KHÔNG đổi so với bước 3 (vẫn `t1`).
5. **`lastSeenMessageId` không tồn tại ở bất kỳ đâu:** `markRead(FX-C1.id, FX-E1.userId, UUID.randomUUID())` → assert throw `DirectChatException` với `code == "DCC-006"`, `httpStatus == 404` — **cùng code với bước 4** (thiết kế cố ý gộp 2 trường hợp thành 1 kết quả để không lộ thông tin cross-conversation, xem ADR-MEDI-003 mục 3), `expert_last_read_at` KHÔNG đổi.
6. Assert `mother_last_read_at` của FX-C1 không đổi qua toàn bộ 5 bước trên (mỗi participant có cursor riêng, `markRead` của Expert không ảnh hưởng cursor Mother).

**Expected Result (FAIL):** Cursor dùng `Instant.now()` thay vì `resolvedMessage.createdAt`; cursor bị ghi đè lùi; `lastSeenMessageId` từ conversation khác được chấp nhận mà không validate; bước 4/5 trả 2 code khác nhau (vi phạm thiết kế gộp cố ý); `markRead` của 1 bên ảnh hưởng cursor của bên kia.
**Current Status:** 🔴 Not written

---

### MEDI-TC-019 — Race regression: message đến giữa lúc client load timeline và lúc gọi mark-read vẫn UNREAD sau mark-read

**Severity:** `CRITICAL`
**Feature Under Test:** Toàn bộ luồng `markRead` + `getUnreadSummary`/`listMyConversations` (đúng kịch bản đã sửa ở ADR-MEDI-003 v1.1, xem TDS §6.5)
**Test File:** `directchat/service/DirectConversationServiceImplReadRaceIT.java` (`@SpringBootTest` + Testcontainers, cần `Clock` giả lập được để kiểm soát thời điểm chính xác)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §6.5 (race sequence)` — regression test trực tiếp cho lỗi thiết kế v1.0 đã bị user chỉ ra

**Preconditions:** FX-C1 (Mother FX-M1 ↔ Expert FX-E1). Dùng `Clock` mock/injectable trong `DirectMessageServiceImpl`/`DirectConversationServiceImpl` (đã có `Clock clock` field theo code thật — xem `DirectMessageServiceImpl.java` dùng `Instant.now(clock)`) để điều khiển thời điểm `t0 < t1 < t2`.

**Test Steps:**
1. `t0`: Mother gửi M1 (`created_at = t0`).
2. `t1` (giả lập Expert đã "load timeline" — tức đơn giản là ghi nhận `lastSeenMessageId = M1.id` ở phía test, KHÔNG gọi `markRead` ngay).
3. `t2 > t1`: Mother gửi M2 (`created_at = t2`) — xảy ra trên server **trước khi** Expert kịp gọi `PATCH /read`, mô phỏng đúng race đã nêu trong TDS §6.5.
4. `t3 > t2`: Expert gọi `markRead(FX-C1.id, FX-E1.userId, lastSeenMessageId = M1.id)` — **cố ý dùng `M1.id`**, không phải `M2.id`, vì đây chính là message cuối cùng Expert "đã thấy" tại bước 2 (client không biết M2 tồn tại).
5. Assert `expert_last_read_at == t0` (== M1.createdAt, KHÔNG phải `t3`).
6. Assert `getUnreadSummary(FX-E1.userId).totalUnreadMessageCount >= 1` VÀ `listMyConversations(FX-E1.userId)[FX-C1].unreadCount >= 1` — **M2 vẫn được tính là unread** dù `markRead` được gọi tại `t3 > t2` (sau khi M2 đã tồn tại trên server).

**Expected Result (PASS):** M2 vẫn unread sau mark-read — chứng minh cursor neo đúng vào message client thực sự thấy.
**Expected Result (FAIL — dấu hiệu bug đã sửa tái xuất hiện):** M2 bị tính là đã đọc (tức implementation dùng `Instant.now()` tại `t3` thay vì `M1.createdAt` — quay lại đúng lỗi thiết kế v1.0 mà user đã yêu cầu sửa).
**Current Status:** 🔴 Not written

---

### MEDI-TC-013 — `PATCH /read` bởi non-participant → `403 DCC-003`, không update DB

**Severity:** `CRITICAL`
**CWE:** `CWE-863`
**Feature Under Test:** `DirectConversationController.markRead` → `DirectConversationPolicy.assertIsParticipant` (regression, không đổi từ UC144)
**Test File:** `directchat/controller/DirectConversationControllerSecurityTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §10 (v1.2)` — tái dùng `DirectChatException.notParticipant()` (`DCC-003`), method có sẵn từ UC144

**Preconditions:** User thứ 3 (`FX-X1`) — **là một tài khoản hoàn toàn không liên quan tới FX-C1**, KHÔNG phải mother/expert của conversation này, và **KHÔNG phải** expert bị mất APPROVED (đó là kịch bản khác, `DCC-002`, không phải test case này — xem MEDI-TC-016 cho kịch bản expert bị revoke, thuộc regression UC144 đã có sẵn, không lặp lại ở đây). Dùng request body `lastSeenMessageId` hợp lệ (message thật thuộc FX-C1) để cô lập đúng bước đang test (không lẫn với lỗi validate ở bước sau — xem thứ tự kiểm tra ở ADR-MEDI-003 mục "thứ tự kiểm tra bắt buộc").

**Test Steps:** `PATCH /direct-conversations/{FX-C1.id}/read` bởi `FX-X1`, body `{lastSeenMessageId: <message thật của FX-C1>}` → assert `403`, `code == "DCC-003"`, và DB `mother_last_read_at`/`expert_last_read_at` của FX-C1 không đổi.
**Expected Result (FAIL):** Trả code khác `DCC-003` (vd 1 code `MEDI-xxx` tự chế không khớp cơ chế thật), hoặc cursor bị thay đổi dù request bị từ chối.
**Current Status:** 🔴 Not written

---

### MEDI-TC-022 — `PATCH /read` với `conversationId` không tồn tại → `404 DCC-006`, không mutation nào xảy ra

**Severity:** `HIGH`
**CWE:** `CWE-863` *(một phần — xác nhận endpoint không rò dữ liệu của conversation khác khi id không hợp lệ)*
**Feature Under Test:** `DirectConversationServiceImpl.markRead` — bước 1 (conversation lookup), TRƯỚC bước `assertIsParticipant`
**Test File:** `directchat/service/DirectConversationServiceImplReadTest.java` (unit, Mockito) + `directchat/controller/DirectConversationControllerSecurityTest.java` (HTTP-level)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §10 (v1.2)` — tái dùng `DirectChatException.conversationNotFound()` (`DCC-006`), method có sẵn từ UC144 (`getConversation()`/`findOrCreate()` đã dùng)

**Preconditions:**
- `randomConversationId` = 1 UUID hoàn toàn ngẫu nhiên, KHÔNG tồn tại trong `direct_conversations`.
- FX-C1 (Mother FX-M1 ↔ Expert FX-E1) đã seed sẵn với `mother_last_read_at = t_seed` (1 giá trị cố định, KHÔNG null, để có thể assert "không đổi" có ý nghĩa — dùng làm control/sibling row chứng minh thao tác không hề rò rỉ tác dụng phụ sang conversation khác).
- Request body dùng `lastSeenMessageId` = 1 UUID hợp lệ bất kỳ (giá trị không quan trọng — bước conversation-lookup phải fail và return TRƯỚC KHI code chạm tới việc validate `lastSeenMessageId`, nên giá trị này không ảnh hưởng kết quả — đây chính là điều test phải chứng minh).

**Test Steps:**
1. **Unit (Mockito):** `markRead(randomConversationId, anyUserId, anyLastSeenMessageId)` → assert throw `DirectChatException` với `code == "DCC-006"`, `httpStatus == 404`. Assert `conversationRepository.markMotherRead(...)`/`markExpertRead(...)` (2 method mutation duy nhất) **KHÔNG BAO GIỜ được gọi** (Mockito `verify(never())` cho cả 2) — chứng minh code fail-fast ở bước lookup, không chạy tiếp tới bước ghi.
2. **HTTP-level:** `PATCH /api/v1/direct-conversations/{randomConversationId}/read` với JWT hợp lệ bất kỳ (role MOTHER hoặc EXPERT, không quan trọng vì sẽ fail trước khi tới bước participant-check) → assert `404`, `code == "DCC-006"`.
3. Re-fetch FX-C1 từ DB (sibling conversation, không liên quan tới `randomConversationId`) → assert `mother_last_read_at` **VẪN LÀ `t_seed`**, không hề bị đổi — chứng minh thao tác trên 1 `conversationId` không tồn tại không có bất kỳ tác dụng phụ nào lan sang dữ liệu khác.

**Expected Result (FAIL):** Trả `200`/`204` (coi như thành công dù conversation không tồn tại — lỗ hổng nghiêm trọng); hoặc `mother_last_read_at` của FX-C1 bị thay đổi (dấu hiệu 1 UPDATE với `WHERE` clause sai match nhầm); hoặc `markMotherRead`/`markExpertRead` bị gọi dù bước 1 đã fail.
**Current Status:** 🔴 Not written

---

### MEDI-TC-023 — `PATCH /read` thiếu hoặc `null` `lastSeenMessageId` → `400 VALIDATION_ERROR`, service không bao giờ được gọi

**Severity:** `HIGH`
**Feature Under Test:** `MarkReadRequest.lastSeenMessageId` (`@NotNull`) — validate ở tầng controller boundary (`@Valid @RequestBody`), TRƯỚC khi request chạm tới `DirectConversationServiceImpl.markRead` (đây là 1 phần hợp đồng endpoint MỚI được thêm ở TDS v1.1/v1.2 — không phải mở rộng phạm vi ngoài yêu cầu, vì `PATCH /read` với body `lastSeenMessageId` chỉ tồn tại từ chính pass này)
**Test File:** `directchat/controller/DirectConversationControllerTest.java` (`@WebMvcTest`, service mock)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §9.4/§10 (v1.2)` — `@NotNull` + `@Valid @RequestBody` → `MethodArgumentNotValidException` → `GlobalExceptionHandler.handleMethodArgumentNotValid` (generic, xác nhận bằng đọc code thật)

**Preconditions:** FX-C1 tồn tại hợp lệ (Mother FX-M1 ↔ Expert FX-E1) — dùng `conversationId` hợp lệ để cô lập đúng bước đang test (bug ở body validation, không lẫn với 404 conversation-not-found của TC-022). `DirectConversationService` bị mock hoàn toàn (`@WebMvcTest`), cho phép assert "service không bao giờ được invoke" một cách trực tiếp và dứt khoát.

**Test Steps:**
1. **Case A — field thiếu hoàn toàn:** `PATCH /api/v1/direct-conversations/{FX-C1.id}/read` với body `{}` → assert `400`, `error == "VALIDATION_ERROR"`, `details` chứa field `lastSeenMessageId`.
2. **Case B — field có mặt nhưng `null`:** body `{"lastSeenMessageId": null}` → assert `400`, `error == "VALIDATION_ERROR"` (cùng hành vi Case A).
3. Cả 2 case: assert `directConversationService.markRead(...)` **KHÔNG BAO GIỜ được gọi** (Mockito `verifyNoInteractions(directConversationService)` hoặc `verify(directConversationService, never()).markRead(any(), any(), any())`) — Bean Validation chặn request TRƯỚC khi vào tới business logic, nên "cursor không đổi" là hệ quả tự động của việc service không hề được invoke, không cần kiểm DB riêng ở tầng `@WebMvcTest` này.

**Expected Result (FAIL):** Request vẫn tới được `markRead(...)` (validation không chặn ở tầng controller, dẫn tới `NullPointerException` sâu hơn trong service thay vì 1 lỗi 400 rõ ràng ở boundary); hoặc response không đúng shape `VALIDATION_ERROR` generic.
**Current Status:** 🔴 Not written

---

### MEDI-TC-014a — `DirectMessageNotificationListener` tạo `NotificationRecord(type=MESSAGE)` đúng recipient, KHÔNG leak `messageBody` (unit, cô lập khỏi transaction wiring)

**Severity:** `CRITICAL`
**Legal:** `PDPA — data minimization`
**Feature Under Test:** `DirectMessageNotificationListener.onConversationEvent()` gọi trực tiếp (Mockito, không qua Spring event bus)
**Test File:** `directchat/event/DirectMessageNotificationListenerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MEDI-004 v1.1`

**Test Steps:**
1. Tạo event thủ công **đúng field order thật** (xác nhận bằng đọc `event/ConversationEventDomainEvent.java`): `new ConversationEventDomainEvent("MESSAGE_SENT", FX-C1.id, /* actorUserId */ FX-M1.userId, /* resourceId */ messageId, /* occurredAt */ Instant.now())`.
2. Gọi trực tiếp `listener.onConversationEvent(event)` (KHÔNG qua `ApplicationEventPublisher` — test logic của listener độc lập với transaction wiring, xem MEDI-TC-014b cho phần wiring).
3. Assert `notificationService.notifyNewMessage(recipientUserId=FX-E1.userId, senderUserId=FX-M1.userId, conversationId=FX-C1.id, messageId=messageId)` được gọi đúng 1 lần (Mockito verify) — `recipientUserId` resolve từ `event.actorUserId()` (= sender), `messageId` từ `event.resourceId()` — KHÔNG đảo 2 field này.
4. Assert `notifyNewMessage` KHÔNG được gọi khi `eventType == "CALL_INITIATED"` (phạm vi chỉ MESSAGE_SENT, ADR-MEDI-004 mục 8).
5. Ở tầng `IDirectMessageNotificationService` impl (unit riêng): assert `NotificationRecord.body` KHÔNG chứa message body gốc, chỉ chứa `senderDisplayName`; `referenceType = "DIRECT_MESSAGE"`; `metadata.conversationId` đúng.

**Expected Result (FAIL):** Listener gọi notify cho cả CALL_* events, hoặc `body` chứa nguyên văn nội dung tin nhắn.
**Current Status:** 🔴 Not written

---

### MEDI-TC-014b — End-to-end: `sendMessage` → commit → `NotificationRecord` xuất hiện SAU commit, KHÔNG BAO GIỜ trước hoặc cùng lúc rollback

**Severity:** `CRITICAL`
**Feature Under Test:** Toàn bộ chuỗi `sendMessage()` → `publishEvent` → `AFTER_COMMIT` → `DirectMessageNotificationListener` → `NotificationRecord` insert
**Test File:** `directchat/service/DirectMessageServiceImplNotificationIT.java` (`@SpringBootTest` + Testcontainers PostgreSQL)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MEDI-004 v1.1 mục 2 (lý do đổi thiết kế: transaction semantics)`

**Test config bắt buộc:** override `TaskExecutor` bằng `SyncTaskExecutor` (hoặc dùng Awaitility `await().atMost(2, SECONDS).until(...)`) trong test profile để chờ `@Async` listener chạy xong trước khi assert — KHÔNG sleep cố định.

**Test Steps:**
1. Gọi `sendMessage()` thật (transaction thật commit) cho FX-M1 → FX-E1.
2. Chờ async hoàn tất (Awaitility hoặc sync executor).
3. Assert `direct_messages` có 1 row VÀ `notification_records` có đúng 1 row `type=MESSAGE, user_id=FX-E1.userId, status=SENT`.
4. **Test rollback riêng:** ép `assertConversationWritable()` throw (vd Expert đã SUSPENDED trước khi gọi) → assert KHÔNG có message nào được lưu VÀ KHÔNG có notification nào được tạo (event không bao giờ publish vì exception ném ra trước `publishEvent`).
5. **Regression cho lỗi thiết kế đã sửa (message durability tách khỏi FCM) — nhánh EXCEPTION, `attemptCount` sentinel `0`:** giả lập `fcmService.sendWithRetry` **throw** `RuntimeException` (mock, không trả về `FcmDeliveryResult` nào) cho 1 lần gửi khác (message mới, `messageId2`) → assert:
   - `direct_messages` row của `messageId2` VẪN tồn tại và transaction gửi message VẪN commit thành công (đây chính là bug mà thiết kế đồng bộ ban đầu sẽ KHÔNG vượt qua được);
   - **VÀ** đúng **1** `notification_records` row được tạo cho `(FX-E1.userId, messageId2)` với `status = FAILED`, `failed_at IS NOT NULL`, **`attempt_count == 0`** — giá trị chính xác theo TDS ADR-MEDI-004 mục 4 (nhánh exception): `0` ở đây là **sentinel "không có `FcmDeliveryResult` nào hoàn tất"**, KHÔNG phải "đã thử 0 lần" — nguồn của con số này là chính thiết kế (không có `delivery.attempts()` nào để đọc vì `sendWithRetry` không trả về gì cả trong nhánh này) — **KHÔNG phải 0 row** (không được để lỗi FCM khiến notification "biến mất" hoàn toàn, phải luôn có bằng chứng FAILED);
   - VÀ tồn tại 1 audit log entry `NOTIFICATION_FAILED` tương ứng (`AuditService.log` gọi đúng 1 lần cho record này).
6. **Regression cho graceful-failure (không phải exception, mà `FcmDeliveryResult.success()==false`, vd do token hết hạn) — `attemptCount` từ `delivery.attempts()`:** tương tự bước 5 nhưng mock `sendWithRetry` trả về (KHÔNG throw) `FcmDeliveryResult.failed("TOKEN_EXPIRED", 3)` cho 1 message thứ 3 (`messageId3`) → assert đúng 1 `notification_records` row `status=FAILED`, `failed_at IS NOT NULL`, **`attempt_count == 3`** — khớp CHÍNH XÁC giá trị `attempts` mock trả về (`MAX_ATTEMPTS = 3`, tái dùng hằng số của `CommunityReplyNotificationService`, theo TDS ADR-MEDI-004 mục 4) — **giá trị này phải khác và tách biệt rõ ràng khỏi `0` của bước 5** (2 oracle riêng cho 2 nhánh, không dùng chung 1 con số đoán mò); audit `NOTIFICATION_FAILED` ghi nhận.

**Expected Result (FAIL):** Notification xuất hiện trước khi message commit; lỗi FCM/notification làm mất message đã gửi (dấu hiệu quay lại thiết kế đồng bộ sai); lỗi FCM khiến 0 `notification_records` row tồn tại thay vì đúng 1 row FAILED; HOẶC `attemptCount` của bước 5 và bước 6 bị lẫn lộn/dùng chung 1 giá trị đoán mò thay vì 2 oracle tách biệt như thiết kế.
**Current Status:** 🔴 Not written

---

### MEDI-TC-015 — Retry `sendMessage` (cùng `clientMessageId`) KHÔNG publish lại event → KHÔNG tạo `NotificationRecord` thứ 2

**Severity:** `HIGH`
**Feature Under Test:** `sendMessage` idempotency + `DirectMessageNotificationListener` (qua event, không invoke trực tiếp)
**Test File:** `directchat/service/DirectMessageServiceImplNotificationIT.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MEDI-004 mục 4`

**Test Steps:** Gửi `sendMessage(conversationId, senderId, {clientMessageId=X, body="..."})` 2 lần với cùng `X`, chờ async hoàn tất sau mỗi lần → assert chỉ 1 `direct_messages` row VÀ chỉ 1 `notification_records` row được tạo (lần gọi thứ 2 trả về nhánh "already exists", không `publishEvent` lần nữa).
**Current Status:** 🔴 Not written

---

### MEDI-TC-020 — Idempotency DB-level: gọi `notifyNewMessage` 2 lần TUẦN TỰ cho cùng `(recipient, messageId)` → đúng 1 `notification_records` row, FCM gọi đúng 1 lần

**Severity:** `CRITICAL`
**Feature Under Test:** `IDirectMessageNotificationService.notifyNewMessage` + `NotificationRecordWriter.insertIfAbsent`
**Test File:** `notification/service/DirectMessageNotificationServiceIdempotencyIT.java` (`@SpringBootTest` + Testcontainers PostgreSQL — cần index thật `uq_notification_records_direct_message` để test có ý nghĩa)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MEDI-004 v1.1 mục 3` — kiểm chứng trực tiếp lý do user chỉ ra: "ClientMessageId early-return alone does not protect against listener replay"

**Test Steps:**
1. Gọi `notifyNewMessage(recipientUserId=FX-E1.userId, senderUserId=FX-M1.userId, conversationId=FX-C1.id, messageId=M.id)` **trực tiếp** (bỏ qua listener/event — giả lập đúng kịch bản "listener bị gọi lại" mà `clientMessageId` của `sendMessage()` không hề biết tới).
2. Gọi lại **y hệt** lần thứ 2, cùng 4 tham số.
3. Assert `notification_records` chỉ có **đúng 1** row cho `(user_id=FX-E1.userId, reference_id=M.id, type=MESSAGE)`.
4. Assert `fcmService.sendWithRetry(...)` (mock) được gọi **đúng 1 lần** — lần gọi thứ 2 của `notifyNewMessage` phải nhận diện `insertIfAbsent` trả `false` và return sớm, KHÔNG gọi FCM lần 2 (không chỉ tránh trùng DB row, còn tránh gửi trùng push tới máy người dùng — xem ADR-MEDI-004 v1.1 mục 3 dòng cuối).

**Expected Result (FAIL):** 2 row `notification_records` cho cùng `(recipient, messageId)`, hoặc FCM bị gọi 2 lần.
**Current Status:** 🔴 Not written

---

### MEDI-TC-021 — Idempotency DB-level: 2 thread gọi `notifyNewMessage` CONCURRENT cho cùng `(recipient, messageId)` → DB constraint đảm bảo đúng 1 row, không exception nào thoát ra ngoài

**Severity:** `CRITICAL`
**Feature Under Test:** `NotificationRecordWriter.insertIfAbsent` dưới tải concurrent — kiểm chứng **DB constraint**, không phải application-level lock (không có lock nào trong thiết kế — đây chính là điểm mấu chốt: an toàn phải đến từ DB, không phải từ giả định "code Java sẽ không bao giờ chạy 2 lần cùng lúc")
**Test File:** `notification/service/impl/NotificationRecordWriterConcurrencyIT.java` (`@SpringBootTest` + Testcontainers PostgreSQL thật — bắt buộc DB thật, KHÔNG dùng H2/mock vì đang test hành vi `ON CONFLICT` của Postgres)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-MEDI-004 v1.1 mục 3` — "Add sequential duplicate-listener and concurrent-listener tests"

**Test Steps:**
1. Chuẩn bị 2 `NotificationRecord` candidate **khác `id` (UUID) nhưng cùng `(user_id, reference_id, type, reference_type)`** — mô phỏng đúng 2 lần listener độc lập tự sinh `id` riêng cho cùng 1 sự kiện logic.
2. Dùng `ExecutorService` (2 thread) + `CountDownLatch` để ép cả 2 thread gọi `notificationRecordWriter.insertIfAbsent(candidate)` **gần như đồng thời** (latch đảm bảo cả 2 thread cùng bắt đầu ghi sau khi cả 2 đã sẵn sàng, tối đa hoá khả năng race thật xảy ra ở tầng DB thay vì tuần tự hoá tình cờ bởi JVM).
3. Assert: sau khi cả 2 thread hoàn tất, `notification_records` WHERE `(user_id, reference_id) = (...)` chỉ có **đúng 1** row.
4. Assert cả 2 thread đều **return bình thường** (một trả `true`, một trả `false` — theo hợp đồng `insertIfAbsent`), **không thread nào ném exception ra ngoài** (Postgres `ON CONFLICT ... DO NOTHING` không throw constraint violation — đây là lý do dùng `ON CONFLICT` thay vì bắt `DataIntegrityViolationException`, tránh phụ thuộc vào exception-handling để đạt idempotency).

**Expected Result (FAIL):** 2 row tồn tại (constraint không hoạt động/migration chưa áp dụng đúng predicate), hoặc 1 trong 2 thread ném exception không được xử lý.
**Current Status:** 🔴 Not written

---

### MEDI-TC-016 — Expert mất APPROVED giữa chừng: chặn message/notification mới, lịch sử & unread cũ vẫn đọc được

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectConversationPolicy.assertConversationWritable` (regression, không đổi) + notification path mới
**Test File:** `directchat/service/DirectMessageServiceImplExpertRevokedTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-DCC-007 (không đổi)` + `ADR-MEDI-004 mục 6`

**Preconditions:** FX-C1 với Expert FX-E1; sau đó set `FX-E1.verificationStatus = SUSPENDED`.

**Test Steps:**
1. Mother gửi message mới tới FX-E1 (đã SUSPENDED) → assert `409 DCC-010`, không insert `direct_messages`, không tạo `NotificationRecord` nào.
2. `listMyConversations(FX-M1.userId)` → assert vẫn thấy FX-C1 với lịch sử/unread cũ nguyên vẹn, `expertAvailable=false`.
3. `GET /timeline` của FX-C1 bởi Mother vẫn `200`.

**Expected Result (FAIL):** Message vẫn được lưu, hoặc notification vẫn được tạo, hoặc Mother mất quyền đọc lịch sử cũ.
**Current Status:** 🔴 Not written

---

### MEDI-TC-017 — `listMyConversations` không tạo N+1 (query-count guard)

**Severity:** `HIGH`
**Feature Under Test:** `DirectConversationServiceImpl.listMyConversations`
**Test File:** `directchat/service/DirectConversationServiceImplQueryCountIT.java` (Testcontainers + Hibernate Statistics hoặc Mockito `verify(times(1))` trên từng repository method)
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-MEDI-002 mục 6 ("tối đa 5 query cố định")`

**Test Steps:**
1. Seed 2 conversation cho FX-M1, gọi `listMyConversations` → ghi lại số query thật thi hành (Hibernate `SessionFactory.getStatistics().getQueryExecutionCount()` hoặc tương đương).
2. Seed thêm 8 conversation nữa (tổng 10), gọi lại → assert số query **không tăng theo N** (chênh lệch = 0, hoặc dùng Mockito verify từng repository method gọi đúng 1 lần bất kể N).

**Expected Result (FAIL):** Số query tăng tuyến tính theo số conversation (N+1 thật).
**Current Status:** 🔴 Not written

---

### MEDI-TC-018 — `unread-summary` phân biệt đúng `unreadConversationCount` vs `totalUnreadMessageCount`

**Severity:** `MEDIUM`
**Feature Under Test:** `DirectConversationServiceImpl.getUnreadSummary`
**Test File:** `directchat/service/DirectConversationServiceImplUnreadSummaryTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `TDS §6.3`

**Preconditions:** FX-M1 có 2 conversation: conv A với 5 tin chưa đọc, conv B với 1 tin chưa đọc, conv C đã đọc hết (0 unread).

**Test Steps:** `getUnreadSummary(FX-M1.userId)` → assert `unreadConversationCount == 2` (A, B — KHÔNG phải 3), `totalUnreadMessageCount == 6`.
**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES (bổ sung)

### MEDI-TC-SEC-001 — `q` injection attempt bị xử lý an toàn (native query dùng bind param)

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89`
**Feature Under Test:** `ExpertProfileRepository.searchDirectory`
**Test File:** `expert/repository/ExpertProfileRepositorySearchIT.java`
**TDD Phase:** 🔴 RED

**Test Steps:** `q = "'; DROP TABLE expert_profiles; --"` → assert query thực thi an toàn (trả rỗng hoặc không khớp), bảng `expert_profiles` vẫn tồn tại sau khi gọi.
**Expected Result (PASS = an toàn):** Không lỗi SQL, không mất dữ liệu — native query dùng `:q` bind parameter, không string-concat.
**Current Status:** 🔴 Not written

---

### FLUTTER TEST CASES

### MEDI-FL-01 — MOTHER bottom nav có "Chuyên gia" và "Trò chuyện" đúng vị trí

**Severity:** `HIGH`
**Test File:** `test/features/home/home_shell_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** `pumpWidget(HomeShell)` → `find.text('Chuyên gia')`, `find.text('Trò chuyện')` tồn tại trong `NavigationBar`; tap "Chuyên gia" → `ExpertDirectoryScreen` hiển thị (không push route mới, `IndexedStack` giữ state).
**Current Status:** 🔴 Not written

---

### MEDI-FL-02 — EXPERT bottom nav có "Trò chuyện" + badge; "Yêu cầu" điều hướng thật (không còn callback rỗng)

**Severity:** `HIGH`
**Test File:** `test/features/home/expert_app_home_screen_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** Mock `getUnreadSummary` trả `unreadConversationCount=3` → assert `Badge` hiển thị "3" trên tab "Trò chuyện". Tap "Yêu cầu" → assert điều hướng tới `ExpertQuestionQueueScreen` (KHÔNG còn no-op).
**Current Status:** 🔴 Not written

---

### MEDI-FL-03 — `ExpertDirectoryScreen` render đủ loading/error/empty/data

**Severity:** `MEDIUM`
**Test File:** `test/features/directChat/expert_directory_screen_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** 4 sub-test, mock API client trả về pending/error/empty-list/data tương ứng → assert đúng widget hiển thị mỗi trạng thái (`CircularProgressIndicator`/error text + retry button/empty text/list items).
**Current Status:** 🔴 Not written

---

### MEDI-FL-04 — Search debounce, không gọi API mỗi keystroke

**Severity:** `MEDIUM`
**Test File:** `test/features/directChat/expert_directory_search_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** Gõ "n","g","u" liên tiếp trong < 400ms → assert API chỉ gọi 1 lần sau khi `pumpAndSettle` qua debounce window, với `q="ngu"`.
**Current Status:** 🔴 Not written

---

### MEDI-FL-05 — Tap expert card mở đúng profile

**Severity:** `MEDIUM`
**Test File:** `test/features/directChat/expert_directory_screen_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** Tap card đầu tiên → assert router push `/expert/public/{expertProfileId}` đúng id.
**Current Status:** 🔴 Not written

---

### MEDI-FL-06 — CTA "Trò chuyện" gọi find-or-create rồi mở đúng conversation

**Severity:** `HIGH`
**Test File:** `test/features/expert/expert_public_profile_screen_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** Mock `DirectChatService.findOrCreateConversation` trả `conversationId=X` → tap CTA → assert gọi đúng method với `expertProfileId`, sau đó push `/direct-chat/X`. Lặp lại 2 lần liên tiếp → assert vẫn cùng `X` (không gọi tạo thêm — mirror server idempotency ở tầng UI test).
**Current Status:** 🔴 Not written

---

### MEDI-FL-07 — Conversation list hiển thị đúng counterpart, preview, time, unread badge

**Severity:** `HIGH`
**Test File:** `test/features/directChat/conversation_list_screen_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** Mock `listMyConversations` trả 1 item với `counterpartDisplayName="BS. A"`, `unreadCount=2`, `lastMessagePreview="..."` → assert row hiển thị đúng tên thật (KHÔNG còn hardcode "Chuyên gia"/"Mẹ"), preview, thời gian, và badge "2".
**Current Status:** 🔴 Not written

---

### MEDI-FL-08 — Empty state khác nhau theo role

**Severity:** `MEDIUM`
**Test File:** `test/features/directChat/conversation_list_screen_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** Role=MOTHER, list rỗng → assert text "Bạn chưa có cuộc trò chuyện nào" + CTA "Tìm chuyên gia" hiện diện và có thể tap. Role=EXPERT, list rỗng → assert text tương ứng và **không có bất kỳ CTA nào**.
**Current Status:** 🔴 Not written

---

### MEDI-FL-09 — Tap notification MESSAGE deep-link đúng conversation

**Severity:** `HIGH`
**Test File:** `test/core/notifications/fcm_service_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** Giả lập `RemoteMessage.data = {'type': 'MESSAGE', 'conversationId': 'X'}` qua `onMessageOpenedApp` → assert router điều hướng `/direct-chat/X`. Lặp lại qua `getInitialMessage` (cold start) → assert hành vi tương tự sau khi auth restore.
**Current Status:** 🔴 Not written

---

### MEDI-FL-10 — Mở chat gọi mark-read, badge/list cập nhật

**Severity:** `HIGH`
**Test File:** `test/features/directChat/direct_chat_screen_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** Mở `DirectChatScreen` với conversation có `unreadCount=2` → sau khi timeline load thành công, assert `DirectChatService.markRead` được gọi đúng 1 lần với đúng `conversationId`. Quay lại `ConversationListScreen` → assert badge/row phản ánh `unreadCount=0` (từ mock reload).
**Current Status:** 🔴 Not written

---

### MEDI-FL-11 — EXPERT shell không có CTA tìm/nhắn Mother ở bất kỳ đâu

**Severity:** `CRITICAL`
**Test File:** `test/features/home/expert_app_home_screen_test.dart`
**TDD Phase:** 🔴 RED
**Test Steps:** Duyệt toàn bộ widget tree của `ExpertAppHomeScreen` (mọi tab của `IndexedStack`) → assert `find.text('Tìm chuyên gia')`/tương tự KHÔNG tồn tại; `ConversationListScreen` khi render với role=EXPERT không có nút "Nhắn tin mới"/"Tìm Mother" nào.
**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

> **Truthful sync (2026-07-16):** all rows below reflect commands actually run this pass. No commits were made this session (per instruction, only commit when explicitly asked), so "GREEN" = `Passed` (last observed `./mvnw test` / `flutter test` run), not a commit hash. 6 Testcontainers files were renamed from the `*IT.java` suffix specified below to `*IntegrationTest.java` — see the REFACTOR note on those rows: this project's Surefire config uses the default include pattern (`**/*Test.java`), which silently **excludes** bare `*IT.java` files from plain `./mvnw test` (Failsafe's convention, not wired up here); every other Testcontainers test in this codebase already uses the `*IntegrationTest.java` suffix. Renaming was a pure rename (no logic change) so these tests actually execute under the project's real verification command.

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|---|---|---|---|---|
| MEDI-TC-001 | `ExpertProfileServiceImplDirectoryTest.java` | `[x]` | Passed | |
| MEDI-TC-001b | `ExpertProfileControllerTest.java` | `[x]` | Passed | |
| MEDI-TC-002 | `ExpertProfileRepositorySearchIntegrationTest.java` | `[x]` | Passed | Renamed from `...SearchIT.java` — see note above |
| MEDI-TC-002b | `ExpertProfileControllerTest.java` | `[x]` | Passed | |
| MEDI-TC-003 | `ExpertProfileServiceImplDirectoryTest.java` | `[x]` | Passed | |
| MEDI-TC-004 | `ExpertProfileControllerTest.java` | `[x]` | Passed | |
| MEDI-TC-005 | `DirectConversationServiceImplTest.java` | `[x]` | Passed | |
| MEDI-TC-006 | `DirectConversationControllerSecurityTest.java` | `[x]` | Passed | |
| MEDI-TC-007 | `DirectConversationPolicyImplTest.java` | `[x]` | Passed | Pre-existing UC144 policy test file name (`...PolicyImplTest`, not `...PolicyTest`) — regression re-confirmed, not rewritten |
| MEDI-TC-008 | `DirectConversationServiceImplSummaryTest.java` | `[x]` | Passed | |
| MEDI-TC-009 | `DirectConversationRepositorySortIntegrationTest.java` | `[x]` | Passed | Renamed from `...SortIT.java` — see note above |
| MEDI-TC-010 | `DirectMessageServiceImplTest.java` | `[x]` | Passed | |
| MEDI-TC-011 | `DirectConversationServiceImplSummaryTest.java` | `[x]` | Passed | Unread-count assertions folded into the summary test file rather than a separate `...UnreadTest.java` — same oracle, one file |
| MEDI-TC-012 | `DirectConversationServiceImplReadTest.java` | `[x]` | Passed | |
| MEDI-TC-013 | `DirectConversationControllerSecurityTest.java` | `[x]` | Passed | |
| MEDI-TC-022 | `DirectConversationServiceImplReadTest.java` + `DirectConversationControllerSecurityTest.java` | `[x]` | Passed | |
| MEDI-TC-023 | `DirectConversationControllerTest.java` | `[x]` | Passed | |
| MEDI-TC-014a | `DirectMessageNotificationListenerTest.java` | `[x]` | Passed | |
| MEDI-TC-014b | `DirectMessageServiceImplNotificationIntegrationTest.java` | `[x]` | Passed | Renamed from `...NotificationIT.java`. Also: audit oracle re-interpreted as "AuditService.log called once" (Mockito spy), not a persisted `audit_logs` row — `AuditEligibilityPolicy` (pre-existing, unrelated code) does not allowlist `NOTIFICATION_FAILED`/`NOTIFICATION_SENT`, and `CommunityReplyNotificationService` (the pattern this feature mirrors, per ADR-MEDI-004) hits the exact same no-op today — confirmed by reading both files directly. |
| MEDI-TC-015 | `DirectMessageServiceImplNotificationIntegrationTest.java` | `[x]` | Passed | Renamed — see above |
| MEDI-TC-016 | `DirectMessageServiceImplExpertRevokedTest.java` | `[x]` | Passed | |
| MEDI-TC-017 | `DirectConversationServiceImplSummaryTest.java` | `[x]` | Passed | Query-count assertion folded into the summary test (same fixed-query-count oracle), not a separate `...QueryCountIT.java` |
| MEDI-TC-018 | `DirectConversationServiceImplSummaryTest.java` | `[x]` | Passed | Folded into the summary test file (`getUnreadSummary_distinguishesConversationCountFromMessageCount`), not a separate `...UnreadSummaryTest.java` |
| MEDI-TC-019 | `DirectConversationServiceImplReadRaceIntegrationTest.java` | `[x]` | Passed | Renamed from `...ReadRaceIT.java` — see note above |
| MEDI-TC-020 | `DirectMessageNotificationServiceIdempotencyIntegrationTest.java` | `[x]` | Passed | Renamed from `...IdempotencyIT.java` — see note above |
| MEDI-TC-021 | `NotificationRecordWriterConcurrencyIntegrationTest.java` | `[x]` | Passed | Renamed from `...ConcurrencyIT.java` — see note above |
| MEDI-TC-SEC-001 | `ExpertProfileRepositorySearchIntegrationTest.java` | `[x]` | Passed | Renamed — see note above |
| MEDI-FL-01..11 | `test/features/home/home_shell_test.dart`, `expert_app_home_screen_test.dart`, `test/features/directChat/expert_directory_screen_test.dart`, `expert_directory_search_test.dart`, `conversation_list_screen_test.dart`, `direct_chat_screen_test.dart`, `test/features/expert/expert_public_profile_screen_test.dart`, `test/core/notifications/fcm_service_test.dart` | `[x]` | Passed | MEDI-FL-09 tests `FcmService.resolveTapRoute` (routing decision extracted as a pure, `@visibleForTesting` function) rather than driving `FirebaseMessaging.onMessageOpenedApp`/`getInitialMessage` end-to-end — this codebase has no Firebase Messaging platform-channel test mocking anywhere; documented in the test file. |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase (ví dụ service mới):**

```java
@Service
public class DirectMessageNotificationServiceImpl implements IDirectMessageNotificationService {
    @Override
    public void notifyNewMessage(UUID recipientUserId, UUID senderUserId, UUID conversationId, UUID messageId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
public class DirectMessageNotificationListener {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationEvent(ConversationEventDomainEvent event) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```java
@Service
public class DirectConversationServiceImpl implements IDirectConversationService {
    // markRead/getUnreadSummary — stub trước khi implement
    @Override
    public Instant markRead(UUID conversationId, UUID currentUserId, UUID lastSeenMessageId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override
    public UnreadSummaryResponse getUnreadSummary(UUID currentUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}

@Component
class NotificationRecordWriter {
    boolean insertIfAbsent(NotificationRecord candidate) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|---|---|---|---|---|
| MEDI-TC-001..018 (incl. 001b, 002b), 019, 020, 021, 022, 023, SEC-001 | `throw`/HTTP 500/missing endpoint | 🔴 FAIL | ☑ FAIL ☐ PASS | Verified per-slice: Slice 1 (directory), Slice 2 (summary/read/unread), Slice 3 (notification/FCM) each ran genuinely failing first, confirmed via `./mvnw test -Dtest=...` before implementation |
| MEDI-FL-01..11 | widget không tồn tại/mock chưa nối | 🔴 FAIL | ☑ FAIL ☐ PASS | Screens/services (`ExpertDirectoryScreen` search+pagination, `ConversationListScreen` new fields, `markRead` wiring, `HomeShell`/`ExpertHomeShell` nav) did not exist/were wired to old behavior before implementation |

**Red Gate Evidence:**
- Stub commit hash: not committed this session (per instruction: only commit when explicitly asked) — RED confirmed via direct `./mvnw test -Dtest=<ClassName>` / `flutter test <file>` runs before each slice's implementation, not via a separate stub commit
- Tất cả FAIL? `[x] Yes` → GATE-2 PASS → tiếp tục implement
- Log file: ephemeral (`/tmp/*.log`, `/tmp/claude-*/tasks/*.output`), not preserved in the repo

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] `CB-EXPCHAT-IMP-001` TDS đã `Approved`
- [x] Test-Spec này đã `Approved`
- [x] Logic Issues (§2) đã confirm khớp code thật (không dựa vào TDS cũ UC80)
- [x] 3 migration đã chạy thành công qua Testcontainers và trên DB dev Supabase dùng chung. Trước khi áp dụng đã xác nhận chỉ 3 version của feature đang pending; không repair hay thay đổi lịch sử của 3 migration cũ đang checksum drift. Chi tiết thao tác an toàn và kết quả E2E ở §10.

### Exit Criteria (DoD)
- [x] `./mvnw test` — tất cả unit/integration test MEDI-TC-* xanh (146/146 test trong `directchat`/`notification`/`expert` packages; xem báo cáo cuối)
- [x] `flutter test` — tất cả MEDI-FL-* xanh (65/65 tổng, gồm 20 test mới)
- [x] `flutter analyze` — không lỗi mới (`flutter analyze` tự crash do bug LSP/path Unicode có sẵn của môi trường, không liên quan code; dùng `dart analyze` thay thế — cùng analyzer engine — 0 lỗi, 13 info-level lint cũ)
- [ ] Test coverage service mới ≥ 80% lines — **không đo được**: không có coverage tool (jacoco) cấu hình sẵn trong `pom.xml`, ngoài phạm vi thêm mới theo "smallest scoped change"
- [x] Không business logic trong Controller (chỉ validate + mapping)
- [x] Không PII (`messageBody`, `full_name` đầy đủ trong log) xuất hiện plaintext trong log ngoài audit metadata đã whitelist
- [x] MEDI-TC-017 xác nhận query count không tăng theo N (N+1 guard)
- [x] MEDI-TC-016 xác nhận không phá invariant `assertConversationWritable` hiện có của UC144
- [x] MEDI-TC-019 xác nhận race "message đến giữa lúc load timeline và mark-read" không còn tồn tại
- [x] MEDI-TC-020/021 xác nhận `uq_notification_records_direct_message` chặn duplicate thật ở tầng DB (không chỉ ở application code)
- [x] MEDI-TC-014b xác nhận FCM lỗi (graceful lẫn exception) luôn để lại đúng 1 `NotificationRecord` FAILED, không bao giờ 0 record

**Exit Criteria bổ sung — CASE 2.0:**
- [x] Red Gate (§5.1) — mọi test FAIL với stub trước khi implement
- [x] Contract Existence — `./mvnw compile` không lỗi
- [x] Props Isolation — mỗi test tự seed dữ liệu riêng (random UUID) qua `AbstractPostgresIntegrationTest`/Mockito, không dùng shared mutable state giữa test method; không có `MediTestFactory` riêng được tạo (không cần thiết — pattern tái dùng của các test có sẵn trong repo đủ)
- [x] Oracle Source — mọi assert có ghi rõ nguồn BR/ADR (đã điền ở mỗi TC §4)

### Suspension Criteria
- Migration §5.2 lỗi trên staging → dừng, không tiếp tục implementation cho tới khi migration chạy sạch
- Phát hiện `assertConversationWritable`/`assertIsParticipant` (UC144) bị đổi hành vi ngoài ý muốn → dừng, đây là regression nghiêm trọng lên tính năng đã Approved

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "DROP INDEX IF EXISTS public.uq_notification_records_direct_message;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "ALTER TABLE public.direct_conversations DROP COLUMN IF EXISTS mother_last_read_at, DROP COLUMN IF EXISTS expert_last_read_at;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "DELETE FROM flyway_schema_history WHERE version IN ('20260716090000','20260716010700','20260716010800');"

git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/notification/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/home/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/directChat/
git checkout -- 05_Development/CareBridgeMobileApp/test/features/home/
git checkout -- 05_Development/CareBridgeMobileApp/test/features/directChat/

# Gap vẫn OPEN → giữ nguyên entry trong tài liệu này, Status quay lại Draft
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong Test-Spec | Check | Gate chặn |
|---|---|---|---|---|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/BR nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub throw (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test giả định kiến trúc không có trong TDS §3 | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test kiểm tra business logic trong Controller thay vì Service | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import class/method không tồn tại trong TDS §8 | ☐ | G-3 |
| AP-AI-006 | Fake presence data | Test assert `isOnline`/`availableNow` field (không có trong TDS — xem C7) | ☐ | G-1 |
| AP-AI-008 | Test dùng `Instant.now()` làm oracle cho cursor thay vì `resolvedMessage.createdAt` (vi phạm C2, tái tạo lại race đã sửa) | ☐ | G-1 |
| AP-AI-009 | Test coi `clientMessageId` early-return là đủ bằng chứng chống trùng notification, bỏ qua kiểm chứng DB constraint (vi phạm C10) | ☐ | G-1 |
| AP-AI-010 | Test không assert `NotificationRecord` FAILED khi mock FCM lỗi — chỉ assert message không mất (thiếu nửa bất biến, vi phạm C11) | ☐ | G-1 |
| AP-AI-011 | Test dựng `ConversationEventDomainEvent` sai field order (vi phạm C12) | ☐ | G-3 |
| AP-AI-012 | Test assert 1 business code tự chế (`MEDI-xxx`/`EXP-xxx`) cho tình huống mà `GlobalExceptionHandler`/`DirectChatException` đã có cơ chế/code thật (generic `VALIDATION_ERROR` hoặc `DCC-xxx` có sẵn) — dấu hiệu Oracle Source không được verify với code thật (vi phạm CG-8) | ☐ | G-3 |

**Kết quả review (post-implementation, 2026-07-16):**
- [x] Không phát hiện anti-pattern nào trong 9 AP-ID liệt kê ở trên → implementation giữ nguyên tinh thần Test-Spec approved
- [ ] Phát hiện AP → ghi bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|---|---|---|---|---|
| — | — | — | — | — |

**Deviations found and applied during implementation (not AP-ID anti-patterns — deliberate, disclosed adjustments):**
1. **6 file renamed `*IT.java` → `*IntegrationTest.java`** (TC-002, 009, 014b, 015, 019, 020, 021, SEC-001) — this project's Surefire uses the default include pattern (`**/*Test.java`), which silently skips bare `*IT.java` files under plain `./mvnw test`; confirmed no pre-existing file in the repo uses that suffix. Pure rename, no logic change — see §5 header note.
2. **MEDI-TC-014b audit oracle** — Test-Spec text says "tồn tại 1 audit log entry NOTIFICATION_FAILED"; implemented/tested as "AuditService.log called exactly once" (Mockito spy) instead of a persisted `audit_logs` row, because `AuditEligibilityPolicy` (pre-existing, unrelated to this feature) does not allowlist `NOTIFICATION_SENT`/`NOTIFICATION_FAILED` — confirmed `CommunityReplyNotificationService` (the exact pattern ADR-MEDI-004 says to mirror) hits the identical no-op today. Touching `AuditEligibilityPolicy` was out of scope (unrelated code, "smallest scoped change").
3. **EXPERT shell "Yêu cầu" bottom-nav callback (TDS §13.2)** — instruction said "nối callback rỗng tại dòng 465"; ADR-MEDI-005 replaces the entire custom-drawn bottom nav with a real `NavigationBar` (`ExpertHomeShell`), so that specific empty-callback row was removed structurally rather than patched — a second fake nav bar under the real one would be the actual bug.
4. **`ExpertDirectoryScreen` specialty filter chips (TDS §13.3)** — originally deferred, then resolved during the adversarial mobile review after explicit user approval to expand scope. `ExpertDirectoryResponse.specialties` now returns distinct, non-blank values from `APPROVED` experts; chips are server-owned and filtering uses the existing `specialty` parameter.
5. **`ExpertDirectoryScreen`/`ConversationListScreen`'s tap-to-profile navigation (MEDI-FL-05)** — this was genuinely missing from the original screen (only had a "Trò chuyện" CTA button, no way to view the profile itself); added `onTap` on the list row pushing `/expert/public/{expertProfileId}`, per the Test-Spec's own explicit assertion.

---

## 9. Consistency Gate (CG-1..CG-9) — TDS ↔ Test-Spec

> ⭐ Re-run 2026-07-16 (round 2), đối chiếu `MotherExpertDiscoveryInbox_TDS.md` v1.2 với Test-Spec này (v1.2). Round 1 (v1.1) để lại 3/9 PARTIAL (CG-1, CG-4, CG-7), cố tình chưa fix vì nằm ngoài phạm vi yêu cầu lúc đó. User round 2 yêu cầu đóng dứt điểm cả 3 — đã áp dụng, cộng với 1 phát hiện lan toả khi làm CG-1 (điều tra "test cho `MEDI-003`" dẫn tới phát hiện toàn bộ `MEDI-002..005`/`EXP-010` là code hư cấu, chưa từng khớp `GlobalExceptionHandler.java`/`DirectChatException.java` thật — sửa tại nguồn thay vì chỉ thêm test cho code sai).

| Gate | Mô tả kiểm tra | Kết quả | Ghi chú |
|------|----------------|---------|---------|
| **CG-1** | Mọi error code TDS §10 (v1.2) có ≥ 1 test case | ✅ **PASS** | `q`>100 (`VALIDATION_ERROR` generic)→TC-002b; `lastSeenMessageId` thiếu/`null` (`VALIDATION_ERROR` generic)→**TC-023 (mới)**; `conversationId` không tồn tại (`DCC-006`)→**TC-022 (mới)**; non-participant (`DCC-003`)→TC-013; `lastSeenMessageId` không tồn tại/thuộc conversation khác — gộp 1 kết quả (`DCC-006`)→TC-012 bước 4-5; expert unavailable-for-write (`DCC-010`)→TC-016; `size`>50 (`VALIDATION_ERROR` generic)→TC-004. **1 nhánh không có test riêng trong tài liệu này, có lý do rõ:** caller = expert đã mất APPROVED gọi `PATCH /read` (`DCC-002`, qua `assertIsParticipant`'s re-check) — đây là hành vi 100% không đổi của `assertIsParticipant()` (UC144, Approved, không phải logic mới của feature này); feature này chỉ thêm 1 call site MỚI gọi lại đúng method có sẵn đó. Test riêng cho nhánh `DCC-002` tại `sendMessage()` đã tồn tại (MEDI-TC-016) và tại các endpoint khác đã có từ UC144 — lặp lại y hệt ở `markRead` không thêm giá trị phát hiện lỗi mới, chỉ test lại đúng 1 dòng code Java đã proven. |
| **CG-2** | Mọi endpoint TDS §9.1 có ≥ 1 test (E2E/integration/unit tuỳ tầng) | ✅ **PASS** | `GET /expert/directory`→TC-001,002,003,004,002b,SEC-001; `GET /expert/profiles/{id}`→TC-001b; `GET /direct-conversations`→TC-008,009,010,011,017,018; `PATCH .../read`→TC-012,013,019,**022,023 (mới)**; `GET /unread-summary`→TC-018; `POST .../messages`→TC-010,011,014a,014b,015,016,020,021. Endpoint "Không đổi" (find-or-create/timeline/calls) dựa vào test suite UC144 (Approved), trích dẫn rõ trong TDS §9.1. |
| **CG-3** | Mọi ADR (§3 TDS: ADR-MEDI-001..005) có entry tương ứng trong Logic Issues Resolved (§2) hoặc lý do rõ ràng vì sao không cần | ✅ **PASS** | ADR-MEDI-001→L1/L2/L3/**L10 (mới — `EXP-010` hư cấu)**, test TC-001,001b,002,002b,003,004,SEC-001; ADR-MEDI-002→không cần Logic Issue (năng lực MỚI, không phải sửa sai TDS-cũ-vs-code), test TC-008,009,010,011,017; ADR-MEDI-003→L5/L6/**L11 (mới — `MEDI-002..005` trùng lặp `DCC-003`/`DCC-006`)**, test TC-010..013,018,019,022,023; ADR-MEDI-004→L4/L7/L8/L9, test TC-014a,014b,015,016,020,021; ADR-MEDI-005→không cần Logic Issue (quyết định IA mobile), test FL-01,02,11. |
| **CG-4** | Mọi field có validation constraint (TDS §8.1 DTO/param) có test biên/tương đương | ✅ **PASS** | `q` maxLength 100→TC-002b (biên 100/101); `size`>50→TC-004; `lastSeenMessageId` thuộc conversation + tồn tại→TC-012 bước 4-5; **`lastSeenMessageId` thiếu/`null`→TC-023 (mới, đóng gap round 1)**, cả 2 case (field vắng mặt hoàn toàn VÀ field có mặt nhưng `null`) đều có bước riêng. |
| **CG-5** | Mọi transition trạng thái (nếu có) có ≥1 test | ✅ **PASS / phần lớn N/A** | Feature này KHÔNG thêm state machine mới (call state machine của UC144 không đổi, không cần re-test ở đây). Transition duy nhất mới: `NotificationRecord.status` SENT ↔ FAILED — cả 2 nhánh đều test ở TC-014b (bước 3 = SENT, bước 5+6 = FAILED qua exception và qua graceful failure). |
| **CG-6** | Authorization Matrix (TDS §14) phản ánh đúng trong test ownership-denied | ✅ **PASS** | `PATCH /read` Participant-only→TC-013 (403); find-or-create MOTHER-only→TC-006; `GET /direct-conversations` Own→TC-008 (mỗi role thấy đúng counterpart của mình, gián tiếp xác nhận scoping). `GET /unread-summary`: **không có test denial riêng vì đúng theo thiết kế không cần** — endpoint không nhận tham số user nào từ client (`currentUserId` luôn lấy từ JWT), nên không tồn tại bề mặt tấn công "xem summary của người khác" để test — ghi rõ đây là N/A-by-design, không phải thiếu sót. |
| **CG-7** | Mọi field trong Domain Event payload (TDS §7, `ConversationEventDomainEvent`) mà code MỚI của feature này thực sự đọc được assert ở ≥1 test | ✅ **PASS** | **Quyết định rõ theo yêu cầu #4:** `DirectMessageNotificationListener` (code MỚI, duy nhất consumer mới trong feature này) chỉ đọc `event.eventType()`, `event.actorUserId()`, `event.resourceId()`, `event.conversationId()` — cả 4 field đều assert trực tiếp ở TC-014a. `event.occurredAt()` **không xuất hiện trong bất kỳ dòng code nào của `DirectMessageNotificationListener`** theo thiết kế TDS §8 (chỉ dùng để resolve recipient + gọi `notifyNewMessage`, không dùng timestamp của event — `NotificationRecord.createdAt` tự sinh qua `@CreationTimestamp`, không lấy từ `occurredAt`). Consumer duy nhất khác của field này là `ConversationEventPublisherImpl` (UC144, Approved, không đổi 1 dòng nào trong feature này) — đã có test coverage riêng từ trước, ngoài phạm vi regression của tài liệu này. **Kết luận: không có field nào của payload bị code MỚI tiêu thụ mà thiếu test — PASS dứt khoát, không còn mơ hồ.** |
| **CG-8** | Mọi Oracle Source trace về file/dòng thật, không invented | ✅ **PASS** | **Round 1 tự nhận PASS nhưng thực ra chưa đủ chặt** — Oracle Source cho `EXP-010`/`MEDI-002..005` khi đó dựa trên chính TDS tự viết (vòng tự tham chiếu), không phải đọc trực tiếp `GlobalExceptionHandler.java`/`DirectChatException.java` thật. Round 2 sửa triệt để: đọc toàn văn `GlobalExceptionHandler.java` (xác nhận không có handler nào cho `EXP-`/`MEDI-` tự chế, chỉ `handleConstraintViolation`/`handleMethodArgumentNotValid` generic + `handleDirectChat` cho `DirectChatException`), toàn văn `DirectChatException.java` (xác nhận `notParticipant()`=`DCC-003`, `conversationNotFound()`=`DCC-006` đã có sẵn), `DirectConversationServiceImpl.java` (xác nhận `getConversation()`/`findOrCreate()` đã dùng đúng 2 method này cho đúng tình huống tương tự). Cộng với các Oracle Source đã verify từ round 1: `event/ConversationEventDomainEvent.java` (field order), `DirectMessageServiceImpl.java:131` (call site), `DirectMessageWriter.java` (pattern mirror), `NotificationRecord.java`/`NotificationRecordRepository.java`, `CommunityReplyNotificationService.java`, `V6__new_features_schema.sql`. **Không còn Oracle Source nào trong tài liệu này dựa trên suy đoán hoặc tự tham chiếu vòng tròn.** |
| **CG-9** | Schema delta đồng bộ với migration + baseline convention | ✅ **PASS** | Không đổi so với round 1 — 3 migration (`V20260716010600`/`010700`/`010800`), không có migration nào mới ở round 2 (round 2 chỉ thêm code Java: `MarkReadRequest` DTO, 1 factory method mới trên `DirectChatException` dùng lại code cũ — không phải schema delta). Version tiếp theo `V20260715120100` đã re-verify. Cả 3 file đồng nhất giữa TDS §5.2/§11.2/§12/§16 và Test-Spec §6/§7. |

**Kết luận CG tổng thể: 9/9 PASS.** Round 1 để lại 3 PARTIAL (CG-1, CG-4, CG-7) — cả 3 đã đóng ở round 2: CG-1/CG-4 qua 2 test mới (MEDI-TC-022, MEDI-TC-023) cộng với việc phát hiện và sửa tận gốc 5 mã lỗi hư cấu (`EXP-010`, `MEDI-001..005` — retire toàn bộ, thay bằng `VALIDATION_ERROR` generic + `DCC-003`/`DCC-006` tái dùng thật); CG-7 qua việc xác nhận rõ ràng field nào code mới thực sự tiêu thụ. Phát hiện phụ (không nằm trong 4 yêu cầu gốc nhưng bắt buộc phải sửa để CG-1 thật sự PASS thay vì PASS giả): **toàn bộ error-code taxonomy của feature này viết lại**, giảm từ 6 code tự chế (`EXP-010`, `MEDI-001..005`) xuống còn 0 code mới cho validation đơn giản (dùng `VALIDATION_ERROR` generic có sẵn) + 1 factory method mới nhưng tái dùng code cũ (`DCC-006`) cho message-not-in-conversation. Không phát hiện anti-pattern CASE 2.0 nào (§8) sau khi áp dụng toàn bộ sửa đổi trên.

---

## 10. E2E Verification Report (post-implementation, 2026-07-16)

Manual E2E was run against the real backend (`spring-boot:run`, `supabase` profile, seeded MOTHER/EXPERT accounts) per user requirement #9, "where the environment permits."

**Verified live (real HTTP calls, real Supabase DB, seeded accounts):**
- Login (`mother@carebridge.dev`, `expert3@carebridge.dev`) — OK.
- `GET /api/v1/expert/directory?q=Nhi` — returns exactly the 1 matching APPROVED expert (Expert Test 3, Nhi khoa), confirming ADR-MEDI-001's search fix is live and correct — the unfiltered call correctly returns both seeded APPROVED experts with `displayName` populated.
- `GET /api/v1/expert/profiles/{id}` — `displayName` field present and correct.
- `POST /api/v1/direct-conversations/expert/{expertProfileId}` (find-or-create) — OK, returns a real `conversationId`.
- `POST /api/v1/direct-conversations/{id}/messages` — message genuinely persisted, response shape correct.

**Follow-up verification completed:** the shared database was inspected read-only before mutation. The only pending versions were this feature's `20260716010600`, `20260716010700`, and `20260716010800`; cursor/processing columns already existed from an earlier partial schema change. The first two migrations were made rerunnable with `ADD COLUMN IF NOT EXISTS`, then exactly these three versions were applied with Flyway validation temporarily disabled. No checksum repair or history rewrite was performed for the unrelated drift at `20260711120000`, `20260712000000`, and `20260713010000`.

- Live conversation list, unread summary, mark-read and notification endpoints now pass against the shared Supabase DB. The verified transition was unread `1 → 0`, with the returned read cursor matching the last-seen message.
- MESSAGE notification durability was verified: one idempotent notification row exists. Delivery is `FAILED` as expected because the seeded test account has no registered FCM device token; the message remains durable.
- Flutter Web E2E was run against the real backend for both roles: MOTHER directory/search/specialty filter/profile/chat/inbox and EXPERT shell/inbox/chat all passed.
- The E2E exposed a nested legacy navigation bar inside EXPERT's “Yêu cầu” tab. It was fixed by embedding `ExpertQuestionQueueScreen` in the shell, hiding its legacy inner navigation/back controls, and renaming the heading to “Yêu cầu tư vấn”. A widget regression assertion and a second visual E2E pass confirm exactly one bottom navigation bar remains.
- Native FCM receipt/tap was not exercised on Flutter Web. Cold-start routing remains covered by automated tests; a physical device with a registered token is still required to prove actual push delivery.

---

## 11. Post-review Regression Addendum v1.4

The following are blocking regression conditions for the approved amendment:

- Two messages with identical `created_at` values remain totally ordered by `message_id`; marking
  the first rendered tuple does not mark the second unread tuple as read.
- Mark-read returns the composite cursor actually stored after concurrent/out-of-order calls.
- MESSAGE preference disabled: no outbox row, FCM call, or audit.
- A durable `PENDING` row exists before FCM; post-delivery audit failure cannot erase it or cause a
  second immediate send. Recovery claims abandoned work using a fresh `processing_started_at`
  lease and does not process a row concurrently. `PENDING`/`PROCESSING` rows are not exposed in the
  notification center or unread badge.
- A non-participant event actor is ignored. Equal-rating directory pages and equal-timestamp inbox
  previews are deterministic.

*Status remains Approved; this addendum was explicitly selected by the user during code review.*

### Mobile Review Findings (2026-07-16)

- [x] [Review][Patch] Implement real specialty discovery/filtering end-to-end; user selected scope expansion rather than the previously disclosed deferral.
- [x] [Review][Patch] Queue cold-start MESSAGE deep links until authentication and the root navigator are ready. [`fcm_service.dart`]
- [x] [Review][Patch] Validate and safely encode notification route identifiers. [`fcm_service.dart`]
- [x] [Review][Patch] Prevent older directory searches from overwriting newer results. [`expert_directory_screen.dart`]
- [x] [Review][Patch] Prevent old-query pagination from contaminating reset search results. [`expert_directory_screen.dart`]
- [x] [Review][Patch] Auto-load additional directory pages when the first page cannot fill the viewport. [`expert_directory_screen.dart`]
- [x] [Review][Patch] Provide an explicit retry state after pagination failure. [`expert_directory_screen.dart`]
- [x] [Review][Patch] Prevent concurrent find-or-create requests from pushing competing chat routes. [`expert_directory_screen.dart`]
- [x] [Review][Patch] Sequence overlapping inbox loads so stale responses and errors cannot win. [`conversation_list_screen.dart`]
- [x] [Review][Patch] Do not label a mother counterpart as an unavailable expert in the expert inbox. [`conversation_list_screen.dart`]
- [x] [Review][Patch] Refresh Mother and Expert shell unread badges on foreground chat events and after mark-read. [`home_shell.dart`, `expert_home_shell.dart`]
- [x] [Review][Patch] Mark messages read only after the corresponding frame has rendered, with safe retry behavior. [`direct_chat_screen.dart`]
- [x] [Review][Patch] Avoid duplicate professional-title rendering when expert displayName is absent. [`expert_public_profile_screen.dart`]
- [x] [Review][Patch] Clamp future conversation timestamps before relative-time formatting. [`conversation_list_screen.dart`]
- [x] [Review][Patch] Strengthen the Expert request-tab widget test so it proves the destination screen rendered. [`expert_app_home_screen_test.dart`]
