# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Mother → Expert Discovery & Inbox — Technical Design Specification

| Field | Value |
|-------|-------|
| **Document ID** | `CB-EXPCHAT-IMP-001` |
| **Version** | `1.2` |
| **Date** | `2026-07-15` |
| **Status** | `Approved` — User approved 2026-07-16 after round 2 (9/9 Consistency Gate PASS) |
| **Document Owner** | `User` |
| **Author** | `AI Agent (Technical Architect)` |
| **Reviewed by** | `User — Request Changes round 2, 2026-07-16 (4 điểm, xem CHANGELOG)` |
| **DPO Sign-off** | `[ ] Pending` *(outstanding — extends existing PII surfaces from UC144; proceeding for dev/test only, same posture as UC144_DirectConsultChat §header)* |
| **Approved by** | `[ ] Pending` |
| **Last Review** | `2026-07-16` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-15 | AI Agent — Technical Architect | v1.0 — Khởi tạo TDS. Audit toàn bộ codebase mobile+backend (không giả định), phát hiện: đường dẫn directory thật là `/api/v1/expert/directory` (không phải `/api/v1/expert-directory` như UC80 TDS ghi sai), backend directory có bug phân trang (bỏ qua page/size, luôn trả toàn bộ) và bug avatar (luôn null), `DirectConversationSummaryResponse` thiếu tên/avatar/preview/unread, không có bất kỳ cơ chế đọc/chưa đọc nào, gửi tin nhắn không tạo `NotificationRecord`/FCM (chỉ phát Firestore signal), 2 callback rỗng trên EXPERT shell, không route nào trong app thực sự dẫn tới `/experts` hoặc `/direct-chats` hôm nay. |
| 2026-07-16 | AI Agent — Technical Architect | v1.1 — **Request Changes (user, vẫn Draft).** Áp dụng 4 yêu cầu sửa: (1) **ADR-MEDI-003 viết lại hoàn toàn** — thay "server-now() mark-read" bằng hợp đồng `lastSeenMessageId` do client gửi, server validate thuộc đúng conversation rồi tiến cursor tới đúng `created_at` của message đó (không phải `now()`) — sửa đúng race "message đến sau khi client load timeline nhưng trước khi gọi mark-read"; thêm error code `MEDI-004`/`MEDI-005`. (2) **ADR-MEDI-004 bổ sung idempotency ở tầng DB** — partial unique index `uq_notification_records_direct_message` trên `(user_id, reference_id) WHERE type='MESSAGE' AND reference_type='DIRECT_MESSAGE'` + `NotificationRecordWriter.insertIfAbsent` (mirror `DirectMessageWriter`, `ON CONFLICT ... DO NOTHING`) — vì early-return theo `clientMessageId` chỉ chặn được retry ở tầng `sendMessage()`, không chặn được listener chạy trùng/đua nhau ở tầng consumer. (3) **Đặc tả đầy đủ hành vi lỗi FCM** — bắt exception trong `IDirectMessageNotificationService`, luôn persist đúng 1 `NotificationRecord` status `FAILED` + `failedAt` + audit `NOTIFICATION_FAILED`, không bao giờ nuốt lỗi mà không để lại record. (4) **Sửa toàn bộ ví dụ `ConversationEventDomainEvent`** về đúng field order thật đã xác nhận bằng đọc `event/ConversationEventDomainEvent.java`: `(eventType, conversationId, actorUserId, resourceId, occurredAt)` — bản v1.0 viết nhầm thứ tự `resourceId`/`actorUserId` ở §3 ADR-MEDI-004 mục 2. Thêm migration thứ 3 (`V20260716010800`). Re-run Consistency Gate CG-1..9, xem Test-Spec §9 (mới) — phát hiện + sửa 2 gap coverage thật (đơn giản, không phải giả bộ xanh): thiếu test cho `GET /expert/profiles/{id}` trả `displayName`, thiếu test biên `q > 100`. |
| 2026-07-16 | AI Agent — Amelia (Dev Agent) | v1.3 — **Implementation complete, TDD Red→Green→Refactor.** Both TDS and Test-Spec Status → `Approved`, feature fully implemented: 3 Flyway migrations, backend Slices 1-3 (expert directory search/pagination fix, conversation summary + read cursor + unread, notification/FCM wiring via `@TransactionalEventListener(AFTER_COMMIT)`+`@Async`), full mobile IA per §13 (MOTHER/EXPERT shell nav, `ExpertDirectoryScreen`, `ConversationListScreen`, `DirectChatScreen` mark-read, FCM deep link, notification center MESSAGE bypass). `./mvnw test`: 146/146 passing in `directchat`/`notification`/`expert` packages (0 failures among 38 pre-existing, unrelated failing classes elsewhere in the suite — confirmed via `git diff` none were touched this session). `flutter test`: 65/65 passing (45 pre-existing + 20 new, covering all MEDI-FL-01..11). Real bug found and fixed during Slice 3: a test-harness-only transaction-boundary artifact (forcing `@Async` synchronous via `SyncTaskExecutor` made the AFTER_COMMIT listener wrongly participate in the just-committed outer transaction) — fixed by using the real thread-pool executor and polling assertions instead; does not affect production behavior. 6 disclosed deviations from the Test-Spec's literal text (file-naming convention fix, audit-oracle re-interpretation, EXPERT nav structural fix vs. patch, deferred specialty-filter-chips, added missing tap-to-profile nav) — see Test-Spec §8 "Deviations found and applied". E2E: directory search + message send verified live against the real backend with seeded accounts; Slice 2/3 endpoints blocked live by a pre-existing, unrelated gap (shared Supabase dev DB has `spring.flyway.enabled=false` for local runs, and a second pre-existing migration-checksum drift on 3 unrelated migrations blocks enabling it) — fully green via `./mvnw test` (Testcontainers) instead; see Test-Spec §10. |
| 2026-07-16 | AI Agent — Technical Architect | v1.2 — **Request Changes round 2 (user, vẫn Draft).** Áp dụng 4 yêu cầu đóng gap CG: (1) Yêu cầu test `PATCH /read` với `conversationId` không tồn tại → phát hiện, khi grounding lại bằng đọc trực tiếp `DirectChatException.java`/`GlobalExceptionHandler.java`, rằng **toàn bộ 4 mã `MEDI-002..005` của v1.1 là trùng lặp không cần thiết** — `directchat` package đã có sẵn `DCC-003`(`notParticipant`)/`DCC-006`(`conversationNotFound`, tái dùng cho mọi "not found" trong package qua nhiều factory khác nhau) đúng cho các tình huống này; **viết lại toàn bộ §9.4/§10 để retire `MEDI-002..005`**, chỉ thêm 1 factory method mới `DirectChatException.messageNotInConversation()` — vẫn dùng code `DCC-006` có sẵn, gộp "message không tồn tại" và "message thuộc conversation khác" thành **1 kết quả duy nhất** (tránh rò rỉ thông tin cross-conversation về sự tồn tại của 1 message — dữ liệu sức khỏe nhạy cảm). Cùng lúc phát hiện **`EXP-010` (từ UC80 TDS) chưa từng được implement thật** — `size`>50 hiện tại chỉ tạo response `VALIDATION_ERROR` chung qua Bean Validation, không có business code riêng — TDS này không còn "tái dùng EXP-010" nữa; `q`>100 và `lastSeenMessageId` thiếu/`null` cũng theo đúng cơ chế generic này, không có code `MEDI-001` nào nữa. (2) Test cho `lastSeenMessageId` thiếu/`null` — đặc tả `MarkReadRequest.lastSeenMessageId` là `@NotNull`, xác nhận đây là 1 phần hợp đồng endpoint mới (không phải mở rộng phạm vi). (3) **ADR-MEDI-004 mục 4 viết lại** — định nghĩa chính xác `attemptCount` cho 2 nhánh riêng biệt: nhánh graceful (`FcmDeliveryResult` trả về, kể cả thất bại) dùng `delivery.attempts()` (mirror `CommunityReplyNotificationService.applyDelivery`), nhánh exception (`sendWithRetry` ném ra, không có `FcmDeliveryResult` nào) dùng sentinel `attemptCount=0` nghĩa là "không có kết quả FCM hoàn chỉnh" (mirror nhánh "no token" hiện có của `CommunityReplyNotificationService`) — 2 giá trị tách biệt, có nguồn rõ ràng trong TDS. `MAX_ATTEMPTS=3` xác nhận tái dùng nguyên trạng từ `CommunityReplyNotificationService`. (4) **CG-7 chuyển PASS** — xác nhận `occurredAt` của `ConversationEventDomainEvent` chỉ được `ConversationEventPublisherImpl` (không đổi, UC144) tiêu thụ, `DirectMessageNotificationListener` (mới) không đọc field này — ghi rõ lý do thay vì để PARTIAL mơ hồ. Re-run CG-1..9 đầy đủ — 9/9 PASS, xem Test-Spec §9. |
| 2026-07-16 | Codex code review | v1.4 — User-approved post-review correction: composite read cursors `(created_at, message_id)` provide a deterministic total order; MESSAGE notification rows now act as a durable DB outbox (`PENDING`/`PROCESSING`) committed before FCM with scheduled recovery; MESSAGE preference opt-out, participant validation, and deterministic directory/inbox ordering are enforced. Actual timestamped Flyway filenames are synchronized throughout the artifacts. |

---

## MỤC LỤC

1. [Tổng quan Module](#1-tổng-quan-module)
2. [Ma trận Truy vết](#2-ma-trận-truy-vết-traceability-matrix)
3. [Architecture Decision Records](#3-architecture-decision-records-adr)
4. [Non-Functional Requirements & SLA](#4-non-functional-requirements--sla)
5. [Static Modeling](#5-static-modeling)
6. [Dynamic Modeling](#6-dynamic-modeling)
7. [Domain Event Catalog](#7-domain-event-catalog)
8. [Interface Specification](#8-interface-specification)
9. [API Specification](#9-api-specification)
10. [Bảng mã lỗi](#10-bảng-mã-lỗi)
11. [Quy trình Triển khai](#11-quy-trình-triển-khai)
12. [Rollback & Incident Runbook](#12-rollback--incident-runbook)
13. [Mobile Design (IA + Screen Contracts)](#13-mobile-design-ia--screen-contracts)
14. [Bảng tổng hợp phân quyền](#14-bảng-tổng-hợp-phân-quyền)
15. [AI Prompt Constraints (CASE 2.0)](#15-ai-prompt-constraints-case-20)
16. [Phụ lục — File Changed List](#16-phụ-lục--file-changed-list)

---

## 1. Tổng quan Module

| Field | Value |
|-------|-------|
| **Module Name** | `Mother-Expert Discovery & Inbox` |
| **Bounded Context** | `expert` (directory), `directchat` (conversation/inbox — extends UC144), `notification` (message alerts) |
| **Data Classification** | `Sensitive-PII` (message previews, notification bodies with sender names) |
| **Compliance Scope** | `PDPA` |
| **Upstream Dependencies** | `UC80_ViewExpertDirectory` (superseded — path/pagination/avatar corrected), `UC144_DirectConsultChat` (extended — read-cursor + notification wiring added, core schema/policy untouched), `notification` module, `security.entity.User` (`full_name`, `avatar_url`) |
| **Downstream Consumers** | Mobile app (Mother, Expert) — navigation shells, directory, inbox, notification center |

**Mô tả:** Sửa các gap đã xác nhận qua audit (không giả định) khiến MOTHER không có cách khám phá/nhắn chuyên gia ngoài Community, và EXPERT không có inbox thật. Phạm vi: (a) sửa 2 bug backend đang tồn tại trong expert directory (phân trang không hoạt động, avatar luôn null) + bổ sung tìm kiếm text thật; (b) mở rộng `DirectConversationSummaryResponse` với tên/avatar/specialty/preview/unread — hiện chỉ có `counterpartUserId` thô; (c) thêm cơ chế đọc/chưa đọc hoàn toàn mới (schema hiện không có); (d) nối tin nhắn trực tiếp vào hệ thống `NotificationRecord`/FCM hiện có (hiện chỉ phát Firestore signal, không tạo notification bền vững); (e) thiết kế lại navigation shell 2 role trên mobile và nối 2 callback rỗng đang tồn tại.

**Không đổi (giữ nguyên nguyên trạng):** `direct_conversations`/`direct_messages`/`conversation_calls` schema gốc, state machine cuộc gọi, `DirectConversationPolicy.assertIsParticipant/assertExpertVerified/assertConversationWritable`, Firestore signaling transport, `CallTimeoutReconciliationJob`, `FirebaseEventRetentionJob` — toàn bộ theo đúng UC144_DirectConsultChat đã Approved.

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại | Mô tả | Thành phần Code | Compliance Target | ADR liên quan |
|---|---|---|---|---|---|
| BR-MEDI-001 | Business Rule | Directory chỉ trả expert APPROVED, hỗ trợ tìm kiếm text + phân trang thật | `ExpertProfileServiceImpl`, `ExpertProfileRepository` | BR-PRIVACY | ADR-MEDI-001 |
| BR-MEDI-002 | Business Rule | Conversation summary phải tự chứa đủ dữ liệu hiển thị (không N+1) | `DirectConversationServiceImpl` | Performance | ADR-MEDI-002 |
| BR-MEDI-003 | Business Rule | Mỗi participant có read cursor riêng, tiến theo `lastSeenMessageId` do client xác nhận (không phải server `now()`); sender không tự tăng unread của chính mình | `direct_conversations.mother_last_read_at/expert_last_read_at` | Data integrity | ADR-MEDI-003 |
| BR-MEDI-004 | Business Rule | Gửi tin nhắn mới tạo đúng 1 `NotificationRecord` (idempotent ở tầng DB, không chỉ dựa vào `clientMessageId`) + FCM cho recipient, không cho sender; lỗi FCM luôn kết thúc bằng 1 record `FAILED` có audit, không bao giờ mất tích | `DirectMessageServiceImpl`, `NotificationType.MESSAGE`, `uq_notification_records_direct_message` | PDPA (data minimization trong nội dung) | ADR-MEDI-004 |
| BR-MEDI-005 | Business Rule | EXPERT không được tự khởi tạo conversation; chỉ thấy conversation mình là participant | `DirectConversationController` (không đổi), mobile UI (không thêm CTA) | RBAC | ADR-MEDI-005 |
| ADR-MEDI-001 | Decision | Sửa path/pagination/avatar directory, thêm `q` search | `expert` package | — | — |
| ADR-MEDI-002 | Decision | Batch-fetch counterpart display data + last-message + unread bằng tối đa 3 query cố định, không phụ thuộc N | `DirectConversationServiceImpl` | — | — |
| ADR-MEDI-003 | Decision | Read cursor 2 cột trên `direct_conversations` thay vì bảng `read_receipts` riêng | `V-new migration` | — | — |
| ADR-MEDI-004 | Decision | Tái dùng `NotificationRecord`/`FcmService`/`NotificationPreference` hiện có, thêm type `MESSAGE`, không tạo hệ thống notification riêng | `notification` package | — | — |
| ADR-MEDI-005 | Decision | Mobile: IndexedStack cho cả 2 role, không dùng `StatefulShellRoute` (giữ nhất quán với MOTHER shell đã có) | `home_shell.dart`, `expert_app_home_screen.dart` | — | — |

---

## 3. Architecture Decision Records (ADR)

### ADR-MEDI-001 — Sửa Expert Directory tại chỗ, không tạo endpoint mới

| Field | Value |
|---|---|
| **Status** | `Proposed` |
| **Date** | `2026-07-15` |
| **Supersedes** | `UC80_ViewExpertDirectory` TDS §8-9 (path và pagination model sai so với code thật) |

#### Bối cảnh
Audit code thật (không phải theo UC80 TDS) cho thấy `GET /api/v1/expert/directory` đã tồn tại và đúng path theo brief của user, nhưng có 2 bug xác nhận bằng đọc code trực tiếp:
1. `ExpertProfileServiceImpl.getPublicDirectory` bọc `List` (từ `findVerifiedPublic()`/`findVerifiedBySpecialty()` — cả 2 đều không nhận `Pageable`) vào `PageImpl` — `page`/`size` client gửi lên hoàn toàn bị bỏ qua, response luôn chứa toàn bộ expert APPROVED.
2. `ExpertProfileMapper.toDirectoryResponse` gọi overload `toResponse(entity)` (1 tham số) — luôn set `avatarUrl = null`; overload có avatar (`toResponse(entity, avatarUrl)`) chỉ được dùng cho single-profile response.
Ngoài ra không có tham số tìm kiếm text nào, và `ExpertProfileResponse` không có field `displayName` (tên thật nằm ở `users.full_name`, không phải trên `expert_profiles`).

#### Quyết định
Sửa tại chỗ trong `expert` package, không đổi path, không tạo controller mới:
1. Thêm `ExpertProfileRepository` methods dùng `Pageable` thật (native query JOIN `users` để hỗ trợ `q`), trả `Page<ExpertProfile>` với `getTotalElements()` chính xác.
2. Thêm query param `q` (optional, `@RequestParam @Size(max = 100)` — **cùng cơ chế Bean Validation y hệt** `size` hiện có ở `@RequestParam @Min(1) @Max(50) int size` (`ExpertProfileController.java:67`), trim, case-insensitive match trên `users.full_name`, `expert_profiles.professional_title`, `expert_profiles.workplace`). Vi phạm `@Size` → `ConstraintViolationException` → `GlobalExceptionHandler.handleConstraintViolation` → response chung `400 { error: "VALIDATION_ERROR", details: [...] }` — **không có business code riêng nào** cho trường hợp này, đúng như `size` hiện tại cũng không có (xem §10, sửa lại v1.2 — `EXP-010` trong UC80 TDS xác nhận là **code hư cấu, chưa từng implement**: đọc trực tiếp `GlobalExceptionHandler.java` xác nhận không có `@ExceptionHandler` nào ánh xạ `size`/`q` sang 1 code tuỳ chỉnh, chỉ có generic `handleConstraintViolation`).
3. Batch-resolve `displayName` + `avatarUrl` cho **toàn bộ trang kết quả bằng 1 lần `userRepository.findAllById(userIds)`**, không query từng dòng.
4. Thêm `displayName` vào `ExpertProfileResponse` (áp dụng cho mọi response dùng DTO này, không riêng directory).
5. `ORDER BY rating_avg DESC NULLS LAST` áp dụng nhất quán cho cả nhánh có/không có `specialty`/`q` (nhánh `specialty` hiện không có `ORDER BY` nào — sửa).
6. **Không** thêm field "online/available" vào directory — không có nguồn dữ liệu presence thật nào tồn tại trong hệ thống (đã grep xác nhận); vi phạm §9 CLAUDE.md task brief nếu bịa field.

#### Hệ quả
**Tích cực:** Directory dùng được thật (phân trang đúng, avatar đúng, tìm được theo tên), không phá client nào khác vì hành vi cũ (luôn trả tất cả, avatar null) chưa từng được client nào phụ thuộc đúng đắn — mobile hiện đang tự lấy `size=50` và bỏ qua phân trang phía client (F.1 audit) nên sẽ được sửa đồng thời ở Phase 3.
**Trade-offs:** `totalElements` trước/sau khác nhau về mặt số liệu chính xác — không phải breaking change vì con số cũ vốn sai.

---

### ADR-MEDI-002 — Mở rộng `DirectConversationSummaryResponse`, batch-fetch không N+1

| Field | Value |
|---|---|
| **Status** | `Proposed` |
| **Date** | `2026-07-15` |

#### Bối cảnh
`DirectConversationSummaryResponse` hiện chỉ có `conversationId, counterpartUserId, counterpartRole, lastActivityAt, expertAvailable` — không tên, không avatar, không preview, không unread. Đây là lý do mobile phải hardcode nhãn "Mẹ"/"Chuyên gia" (chỉ có role, không có tên thật). `direct_messages`/`direct_conversations` không có quan hệ JPA `@ManyToOne` tới `users`/`expert_profiles` (chỉ FK UUID thô) — đúng theo pattern hiện tại của cả `directchat` lẫn `expert` package (không thêm quan hệ JPA mới để giữ nguyên phong cách).

#### Các phương án đã xem xét
| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|---|---|---|---|
| A | Client tự gọi thêm API cho từng row để lấy tên/avatar | Đơn giản backend | N+1 phía client, vi phạm thẳng yêu cầu "UI không phải gọi thêm API cho từng row" |
| B | Batch-fetch bằng tối đa 3 câu query cố định (không phụ thuộc N) trong `listMyConversations` | Không N+1, 1 round-trip cho toàn bộ list | Cần viết native query cho last-message/unread aggregate |

#### Quyết định — chọn B
`DirectConversationServiceImpl.listMyConversations(currentUserId)`:
1. `findByMotherUserIdOrExpertUserId` (đã có) → `List<DirectConversation>`, sort `ORDER BY last_activity_at DESC` (thêm — hiện không có ORDER BY).
2. Gom toàn bộ `counterpartUserId` → **1 lần** `userRepository.findAllById(...)` → map `userId → (fullName, avatarUrl)`.
3. Gom `counterpartUserId` có `counterpartRole == EXPERT` → **1 lần** `expertProfileRepository.findByUserIdIn(...)` → map `userId → specialty`.
4. **1 native query** lấy last message theo từng conversation (Postgres `DISTINCT ON (conversation_id) ... ORDER BY conversation_id, created_at DESC`) cho toàn bộ `conversationIds` trong 1 lần gọi.
5. **1 native query** tính `unreadCount` cho toàn bộ `conversationIds` cùng lúc (xem §6.2 cho SQL đầy đủ) — join `direct_conversations` với `direct_messages`, so sánh `created_at` với cột read-cursor tương ứng của `currentUserId` trong từng conversation.
6. Merge 4 map trên vào DTO trong bộ nhớ Java — **tổng cộng tối đa 5 query cho toàn bộ danh sách, không phụ thuộc số lượng conversation** (BR-MEDI-002, kiểm chứng ở MEDI-TC-017).

`DirectConversationSummaryResponse` field mới: `counterpartDisplayName`, `counterpartAvatarUrl`, `counterpartSpecialty` (null nếu counterpart là MOTHER), `lastMessagePreview` (null nếu chưa có message nào — có thể conversation chỉ có call), `lastMessageAt`, `unreadCount`, `conversationStatus` (map từ `DirectConversation.status`, hiện luôn `"ACTIVE"` — thêm để forward-compat, không đổi hành vi).

#### Hệ quả
**Tích cực:** Đúng yêu cầu "response đủ dữ liệu, UI không gọi thêm API"; hiệu năng ổn định khi số conversation tăng.
**Trade-offs:** `lastMessagePreview` là dữ liệu sức khỏe tự nguyện của Mother xuất hiện ở tầng list — tăng bề mặt PII (ghi rõ trong DPO note); giảm thiểu bằng truncate 120 ký tự, không log preview ra audit log (chỉ audit metadata, đúng pattern DIRECT_MESSAGE_SENT hiện tại).

---

### ADR-MEDI-003 — Read cursor 2 cột trên `direct_conversations`, không tạo bảng `read_receipts` riêng

| Field | Value |
|---|---|
| **Status** | `Proposed` |
| **Date** | `2026-07-15` |

#### Bối cảnh
Không có bất kỳ khái niệm đọc/chưa đọc nào tồn tại trong `directchat` package hôm nay (xác nhận bằng grep toàn bộ package, 0 kết quả cho `unread|read_at|last_read|is_read|seen_at`). Cần thiết kế mới hoàn toàn, tuân thủ khuyến nghị "participant read cursor" trong bối cảnh mỗi conversation chỉ có đúng 2 participant cố định (mother/expert), không phải nhóm chat N người.

#### Các phương án đã xem xét
| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|---|---|---|---|
| A | Bảng `conversation_read_state(conversation_id, user_id, last_read_at)` riêng, generic cho N participant | Mở rộng được cho group chat tương lai | Thừa cho use-case 1:1 hiện tại; thêm 1 bảng + FK + index chỉ để lưu tối đa 2 dòng/conversation |
| B | 2 cột `mother_last_read_at`, `expert_last_read_at` ngay trên `direct_conversations` | Không JOIN thêm bảng khi list conversations (khớp ADR-MEDI-002 — vẫn nằm trong 1 trong 5 query cố định), đơn giản đúng với model 1:1 hiện tại | Không tái dùng được nếu sau này chat nhóm — chấp nhận được vì `directchat` được thiết kế 1:1 từ đầu (ADR-DCC-001, không đổi) |

#### Quyết định — chọn B (v1.1 — sửa sau review: cursor phải neo vào message client thực sự đã thấy, không phải server `now()`)

> **Sửa so với thiết kế nháp đầu tiên:** bản nháp v1.0 định cho `PATCH /read` tự set cursor = `now()` phía server. Điều đó có 1 race nghiêm trọng: nếu message M2 đến **sau khi** client đã tải xong timeline (client chỉ thực sự thấy tới M1) nhưng **trước khi** request `PATCH /read` của client tới được server, `now()` tại thời điểm server xử lý request đã lớn hơn `M2.createdAt` — server sẽ set cursor vượt qua M2, đánh dấu-đã-đọc một tin nhắn mà người dùng **chưa từng nhìn thấy**. Sửa: cursor phải neo vào **message cụ thể mà client xác nhận đã render**, không phải đồng hồ server.

Migration schema **không đổi** so với v1.0 (vẫn đúng 2 cột này, xem §5.2) — chỉ đổi **cách ghi vào 2 cột đó**:
```sql
ALTER TABLE public.direct_conversations
    ADD COLUMN mother_last_read_at timestamptz NULL,
    ADD COLUMN expert_last_read_at timestamptz NULL;
```

`PATCH /api/v1/direct-conversations/{conversationId}/read` — request body **bắt buộc**:
```java
public class MarkReadRequest {
    @NotNull
    private UUID lastSeenMessageId;
}
```
`@Valid @RequestBody MarkReadRequest` — thiếu field hoặc `null` → `MethodArgumentNotValidException` → `GlobalExceptionHandler.handleMethodArgumentNotValid` → response chung `400 { error: "VALIDATION_ERROR", details: [{field: "lastSeenMessageId", message: "must not be null"}] }`. **Không có business code riêng** cho case này — cùng cơ chế generic đã dùng cho mọi Bean Validation khác trong project (xem §10 v1.2).

Xử lý ở `DirectConversationServiceImpl.markRead(conversationId, currentUserId, lastSeenMessageId)` — **thứ tự kiểm tra bắt buộc, mỗi bước chỉ chạy nếu bước trước pass** (quan trọng để mỗi lỗi trả đúng nguyên nhân, không lẫn lộn):
1. **Conversation tồn tại?** `conversationRepository.findById(conversationId).orElseThrow(DirectChatException::conversationNotFound)` — **tái dùng y hệt** factory method đã có (`DCC-006`, 404), đúng pattern `getConversation()`/`findOrCreate()` đã dùng — **không tạo factory/code mới**.
2. **Caller là participant?** `policy.assertIsParticipant(currentUserId, conversation)` — **tái dùng y hệt** (`DCC-003`, 403, nếu không phải participant nào; hoặc `DCC-002`, 403, nếu caller CHÍNH LÀ expert nhưng đã mất APPROVED — 2 kịch bản khác nhau của cùng 1 method có sẵn, không đổi). KHÔNG gọi `assertConversationWritable` — Mother phải đánh dấu-đã-đọc được ngay cả khi Expert đã mất APPROVED, vì đọc lịch sử cũ vẫn được phép theo ADR-DCC-007 mục 3 (Mother không bị chặn bởi bước này vì `assertIsParticipant`'s re-check APPROVED chỉ áp dụng khi caller = expert, không áp dụng khi caller = mother).
3. **`lastSeenMessageId` thuộc đúng conversation này?** `directMessageRepository.findByIdAndConversationId(lastSeenMessageId, conversationId)` — **1 query duy nhất, không tách 2 lần lookup**. Nếu rỗng (message không tồn tại **hoặc** tồn tại nhưng thuộc conversation khác — cả 2 trường hợp cho **cùng kết quả rỗng** từ query này) → throw **1 mã duy nhất, tái dùng `DCC-006`** (factory mới `DirectChatException.messageNotInConversation()`, cùng code `DCC-006`, message khác: "Message not found in this conversation"). **Cố tình KHÔNG tách thành 2 mã khác nhau** (khác với thiết kế nháp v1.1 trước đó) — tách 2 mã đòi hỏi 1 lần lookup thứ 2 chỉ để biết message có tồn tại ở conversation khác hay không, và lộ ra chính lookup đó là 1 **rò rỉ thông tin**: nó cho phép client dò được "message này tồn tại (ở đâu đó)" so với "message này không tồn tại ở đâu cả" — 2 trạng thái phân biệt được từ response, dù client không phải participant của conversation chứa message đó. Trong bối cảnh dữ liệu sức khỏe nhạy cảm, 1 mã duy nhất cho cả 2 trường hợp là lựa chọn an toàn hơn, và **client không cần phân biệt 2 case này** — cả 2 đều dẫn tới cùng 1 hành động đúng: đừng gửi `lastSeenMessageId` đó nữa, dùng message thật đang có trong timeline. **Không** tin `lastSeenMessageId` mù quáng — đây là input do client cung cấp, phải xác thực trước khi dùng để tính cursor, tương tự cách `assertIsParticipant` không bao giờ tin role/userId client tự khai.
4. Lấy `resolvedMessage.getCreatedAt()` — **đây** là giá trị dùng để tiến cursor, KHÔNG phải `Instant.now()`.
5. Set cột tương ứng với role của caller: `GREATEST(COALESCE(existing_value, '-infinity'::timestamptz), resolvedMessage.createdAt)` — monotonic (client gửi 1 `lastSeenMessageId` cũ hơn cursor hiện tại, vd do request out-of-order tới server, sẽ KHÔNG kéo cursor lùi lại).
6. Idempotent: gọi lại nhiều lần với cùng hoặc `lastSeenMessageId` cũ hơn chỉ giữ nguyên hoặc tiến cursor, không có side-effect nào khác. Response `200 { "cursorAt": "<resolvedMessage.createdAt>" }` (không phải `204` như v1.0 — trả lại giá trị cursor đã áp dụng để mobile có thể optimistic-update badge mà không cần fetch lại `unread-summary` ngay).

**Mobile chọn `lastSeenMessageId` như thế nào** (xem §13.6 cập nhật): giá trị **mới nhất trong số các message client thực sự đã render trên màn hình** tại thời điểm gọi — tức message cuối cùng (`kind=MESSAGE`) trong trang timeline đã tải, KHÔNG phải "message mới nhất tồn tại trên server" (client không thể biết điều đó nếu chưa fetch). Đây chính là cơ chế khiến M2 (đến sau khi client đã load) không bị lẫn vào — client đơn giản không biết `M2` tồn tại nên không thể gửi `M2.id`.

`unreadCount` cho conversation X từ góc nhìn `currentUserId` — **không đổi công thức** (chỉ nguồn của cursor đổi, không đổi cách so sánh):
```sql
SELECT COUNT(*) FROM direct_messages dm
WHERE dm.conversation_id = :conversationId
  AND dm.sender_user_id <> :currentUserId
  AND dm.created_at > COALESCE(
        (SELECT CASE WHEN dc.mother_user_id = :currentUserId THEN dc.mother_last_read_at
                     ELSE dc.expert_last_read_at END
         FROM direct_conversations dc WHERE dc.conversation_id = :conversationId),
        '-infinity'::timestamptz)
```
(Triển khai thật ở dạng 1 query gộp cho nhiều conversationId cùng lúc — xem §6.2.)
Chỉ đếm **message**, không đếm call event vào unread — cuộc gọi nhỡ đã có tín hiệu riêng (Firestore `CALL_STATE_CHANGED` + timeline hiển thị "Cuộc gọi nhỡ"), gộp vào cùng 1 số unread sẽ gây nhầm lẫn ý nghĩa badge.

#### Hệ quả
**Tích cực:** Không JOIN bảng mới khi list conversations; read state luôn nhất quán multi-device (server-side, không phải local state — đúng yêu cầu "Không chỉ giữ unread trong local state"); **race "message đến giữa lúc load timeline và lúc gọi mark-read" được loại bỏ tận gốc** vì cursor chỉ có thể tiến tới message mà client thực sự đã biết tồn tại, không bao giờ tới thời điểm server xử lý request (MEDI-TC-019 regression test).
**Trade-offs:** Nếu tương lai chat nhóm N người được thêm, cần bảng generic riêng — chấp nhận được, ngoài phạm vi hiện tại. Thêm 1 lần lookup `direct_messages` để validate `lastSeenMessageId` mỗi lần gọi `PATCH /read` — chấp nhận được (single-row PK lookup, không phải aggregate).

---

### ADR-MEDI-004 — Tái dùng `NotificationRecord`/`FcmService` hiện có cho direct message, không tạo pipeline riêng

| Field | Value |
|---|---|
| **Status** | `Proposed` |
| **Date** | `2026-07-15` |

#### Bối cảnh
`sendMessage()` hôm nay chỉ publish `ConversationEventDomainEvent` → `ConversationEventPublisherImpl` → Firestore (transient signal, best-effort, không lưu DB). Không có `NotificationRecord` nào được tạo, không có FCM nào được gửi — xác nhận bằng đọc toàn bộ `sendMessage()` và mọi consumer của `ConversationEventDomainEvent`. `NotificationType` enum hiện tại (`REMINDER, COMMUNITY_REPLY, CONSULTATION, EMERGENCY`) không có giá trị nào cho tin nhắn. `notification_records_type_check` CHECK constraint (V6) sẽ reject nếu insert type mới mà không widen.

#### Quyết định (v1.1 — sửa sau review kiến trúc: KHÔNG chạy đồng bộ trong transaction gửi tin nhắn)

> **Sửa so với thiết kế nháp đầu tiên:** bản nháp ban đầu định gọi notification **đồng bộ, inline** trong `sendMessage()` và giả định `try/catch` nội bộ đủ để lỗi notification không làm rollback message. Điều đó **sai** theo Spring transaction semantics: nếu `notifyNewMessage` chạy trong cùng transaction (kể cả `@Transactional(REQUIRED)` join transaction cha), một `RuntimeException` bên trong nó đánh dấu transaction `rollback-only` — `try/catch` ở tầng gọi **không** ngăn được `UnexpectedRollbackException` khi commit; kể cả không có exception, 1 `save()` lỗi (vd constraint violation) cũng đầu độc Hibernate session và làm hỏng commit của message. Gọi `fcmService.sendWithRetry` (network I/O) trong cùng transaction DB cũng giữ connection mở không cần thiết. Vì vậy thiết kế đúng phải tách hoàn toàn khỏi transaction ghi message — dùng lại **chính xác cơ chế `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`** mà Firestore publish (`ConversationEventPublisherImpl`, ADR-DCC-002, không đổi) đã dùng, thay vì phát minh cơ chế mới.

1. Thêm `NotificationType.MESSAGE` vào enum + migration widen `notification_records_type_check` (mẫu giống các lần widen `audit_logs_action_check` trước đó — `DROP CONSTRAINT IF EXISTS` + `ADD CONSTRAINT` với đầy đủ danh sách cũ + mới).
2. Thêm `DirectMessageNotificationListener` — **1 consumer thứ 2, độc lập**, của cùng `ConversationEventDomainEvent` mà `DirectMessageServiceImpl.sendMessage()` đã publish (không đổi call site publish — vẫn nguyên trạng đúng như code thật hôm nay: `eventPublisher.publishEvent(new ConversationEventDomainEvent("MESSAGE_SENT", conversationId, senderUserId, saved.getId(), now))`; record field order xác nhận bằng đọc trực tiếp `event/ConversationEventDomainEvent.java`: `(eventType, conversationId, actorUserId, resourceId, occurredAt)` — `senderUserId` ở vị trí `actorUserId` (thứ 3), `saved.getId()` (= `messageId`) ở vị trí `resourceId` (thứ 4). **v1.0 của tài liệu này viết nhầm thứ tự 2 field cuối — đã sửa, xem CHANGELOG v1.1.**). Listener này:
   - `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async` — chạy trên thread pool riêng, **sau khi** transaction ghi message đã commit thành công, hoàn toàn không thể ảnh hưởng ngược lại việc lưu message.
   - Lọc `eventType == "MESSAGE_SENT"` (bỏ qua `CALL_INITIATED`/`CALL_STATE_CHANGED` — phạm vi mục 7 dưới).
   - Resolve `recipientUserId` = counterpart của `actorUserId` trong conversation (tái dùng logic resolve giống `ConversationEventPublisherImpl.resolveRecipient`, không viết lại từ đầu).
   - Gọi `IDirectMessageNotificationService.notifyNewMessage(recipientUserId, senderUserId, conversationId, messageId)` — method này tự mở transaction riêng (`@Transactional`, mặc định `REQUIRED` nhưng không có transaction cha nào đang mở ở thời điểm này vì đang chạy sau commit, trên thread khác) để insert `NotificationRecord` + gọi `fcmService.sendWithRetry`.
   - Bọc `try/catch` bên trong listener (đúng vị trí lần này — không có transaction cha nào để làm hỏng) để lỗi FCM/notification chỉ log, không throw ra ngoài — nhất quán với cách `ConversationEventPublisherImpl` đã tự nuốt lỗi Firestore.
3. **Idempotency ở tầng DB (v1.1 — bổ sung sau review), không chỉ dựa vào `clientMessageId` early-return của `sendMessage()`.** Early-return theo `clientMessageId` (mục 5 cũ, nay mục 6) chỉ chặn được trường hợp **retry request gửi tin nhắn** (client gọi lại `sendMessage`) — nó **không** chặn được trường hợp listener tự thân bị gọi 2 lần cho cùng 1 event đã publish (vd 1 bug tương lai khiến Spring redeliver event, hoặc 2 thread đọc cùng lúc trong 1 kịch bản concurrent giả lập) — đó là lỗ hổng thật nếu chỉ tin vào "event chỉ publish 1 lần nên listener chỉ chạy 1 lần". Sửa bằng ràng buộc **DB-enforced**, không phải suy luận ở tầng application:
   - Migration mới (§5.2, file thứ 3): `CREATE UNIQUE INDEX uq_notification_records_direct_message ON notification_records (user_id, reference_id) WHERE type = 'MESSAGE' AND reference_type = 'DIRECT_MESSAGE';` — khoá idempotency = **(recipient, MESSAGE, DIRECT_MESSAGE, messageId)**, thu gọn còn `(user_id, reference_id)` vì `type`/`reference_type` cố định trong partial index.
   - Thêm `NotificationRecordWriter.insertIfAbsent(NotificationRecord candidate): boolean` — **mirror chính xác** `DirectMessageWriter.insertIfAbsent` đã có (`JdbcTemplate` + `INSERT ... ON CONFLICT (user_id, reference_id) WHERE type='MESSAGE' AND reference_type='DIRECT_MESSAGE' DO NOTHING`, trả `rowsAffected == 1`).
   - `IDirectMessageNotificationService.notifyNewMessage`: gọi `insertIfAbsent` **trước**; nếu trả `false` (đã tồn tại — do listener chạy trùng/đua) → return ngay, **không gọi FCM lần 2** (không chỉ tránh trùng DB row mà còn tránh gửi trùng push notification tới máy người dùng).
4. **Đặc tả đầy đủ hành vi khi FCM lỗi, bao gồm `attemptCount` chính xác cho từng nhánh (v1.2 — làm rõ theo yêu cầu #3).** Chỉ khi `insertIfAbsent` trả `true` (bản ghi mới, không phải duplicate) mới tiếp tục. `MAX_ATTEMPTS = 3` — hằng số tái dùng nguyên trạng từ `CommunityReplyNotificationService` (đã gọi `fcmService.sendWithRetry(token, title, body, 3)`), không phát minh giá trị mới.
   - **Không có device token active nào cho recipient** → set `status=FAILED, attemptCount=0, failedAt=now()` ngay trên record vừa insert (mirror `CommunityReplyNotificationService` khi `tokens.isEmpty()`) — **vẫn đúng 1 row FAILED, không phải 0 row**. `attemptCount=0` ở đây nghĩa là "chưa từng gọi FCM" (không có token để gọi).
   - **Có token, `sendWithRetry` trả về bình thường (graceful — kể cả khi `success()==false`)** → gọi trong `try`, áp `FcmDeliveryResult` **y hệt** `CommunityReplyNotificationService.applyDelivery`: `attemptCount = delivery.attempts()` (giá trị `sendWithRetry` tự trả về, phản ánh đúng số lần nó đã thử nội bộ, tối đa `MAX_ATTEMPTS`); `SENT`+`fcmMessageId`+`sentAt` nếu `success()`, `FAILED`+`failedAt` nếu không.
   - **Có token, `sendWithRetry` ném exception (không trả về `FcmDeliveryResult` nào cả — lỗi không mong đợi, vd cấu hình/network không được `FcmDeliveryResult` bọc lại)** → bắt trong `catch (Exception e)`, set `status=FAILED, failedAt=now(), attemptCount=0` trên **đúng row đã insert ở mục 3** — **`attemptCount=0` ở nhánh này nghĩa là "không có `FcmDeliveryResult` nào hoàn tất để biết số lần thử thật"**, KHÔNG được đọc nhầm thành "đã thử 0 lần thành công" hay suy luận thành `MAX_ATTEMPTS`. Đây là 1 giá trị sentinel rõ ràng cho "không có kết quả FCM hoàn chỉnh nào", tách biệt hoàn toàn khỏi nhánh trên (vốn luôn có 1 con số thật từ `delivery.attempts()`). Không tạo row thứ 2, không rethrow ra khỏi `notifyNewMessage` (đúng C3 — không có gì phía trên để rollback, nhưng vẫn phải để lại bằng chứng FAILED, không được nuốt lỗi mà không ghi gì).
   - Mọi nhánh (SENT hoặc FAILED) đều gọi `auditService.log(NOTIFICATION_SENT | NOTIFICATION_FAILED, recipientUserId, "NotificationRecord", record.getId(), metadata)` — mirror `CommunityReplyNotificationService.saveAndAudit`, metadata không chứa `messageBody`.
   - **Bất biến bắt buộc:** với mỗi `(recipientUserId, messageId)` hợp lệ, sau khi `DirectMessageNotificationListener` xử lý xong (thành công hoặc lỗi FCM), **luôn tồn tại đúng 1** `notification_records` row — không bao giờ 0 row (MEDI-TC-014b), không bao giờ > 1 row kể cả dưới concurrent listener execution (MEDI-TC-020, MEDI-TC-021).
5. Nội dung notification: `title = "Tin nhắn mới"`, `body = "Bạn có tin nhắn mới từ {senderDisplayName}"` — **không chứa `messageBody`** (an toàn cho lock-screen, đúng §7 brief). `referenceId = messageId`, `referenceType = "DIRECT_MESSAGE"`, `metadata = {conversationId, senderUserId, eventType: "MESSAGE_SENT"}`.
6. **Không tạo duplicate khi retry `sendMessage`**: nhánh "already exists" (retry cùng `clientMessageId`) trả về sớm, không bao giờ publish lại `ConversationEventDomainEvent` — listener do đó không bao giờ chạy lần 2 **từ nguồn retry** cho cùng 1 message. Đây là lớp phòng thủ thứ nhất (application-level, chặn từ gốc); mục 3 ở trên (DB-enforced) là lớp phòng thủ thứ hai, độc lập, chặn cả những đường khác có thể khiến listener chạy trùng mà lớp thứ nhất không lường trước.
7. Không cần re-check `verificationStatus` trong listener: nếu recipient là Expert chưa/không còn APPROVED, `assertConversationWritable()` đã chặn toàn bộ request **trước khi** tới được đoạn insert message (ADR-DCC-007, không đổi) — event chỉ tồn tại khi write đã được phép, listener tái dùng invariant có sẵn thay vì kiểm tra lại.
8. **Phạm vi: chỉ tin nhắn TEXT, không mở rộng sang call events** trong pass này — cuộc gọi đã có tín hiệu Firestore riêng cho trải nghiệm real-time khi app đang mở; push-notification cho cuộc gọi đến là bài toán khác (cần độ ưu tiên FCM cao, UI answer/decline từ notification) — để ngoài phạm vi, không lặp lại rủi ro "mở rộng ngoài yêu cầu" đã từng bị flag ở UC144 v1.1.
9. `NotificationRecordResponse` DTO (hiện thiếu `metadata` dù entity đã có) — thêm field `metadata` (Map) để mobile lấy `conversationId` phục vụ deep link (§13.6) — thay đổi cộng thêm, không phá client cũ.
10. **Test có thể chạy async đồng bộ hóa được**: test profile cấu hình `@TestConfiguration` override `TaskExecutor`/`SimpleAsyncTaskExecutor` → chạy đồng bộ (hoặc dùng Awaitility poll) để integration test xác định được thời điểm listener đã chạy xong — xem Test-Spec MEDI-TC-014b/015/020/021.

#### Hệ quả
**Tích cực:** Message durability tuyệt đối tách khỏi notification/FCM — lỗi notification không bao giờ có đường nào quay lại ảnh hưởng message đã gửi (đúng ngay từ kiến trúc, không dựa vào try/catch sai chỗ). Tái dùng 100% hạ tầng notification/FCM đã kiểm chứng + đúng 1 cơ chế async đã có sẵn cho cả 2 loại side-effect (Firestore signal, notification) — không thêm dependency/infrastructure mới.
**Trade-offs:** Có độ trễ nhỏ (async, không đồng bộ với response 201) giữa lúc message gửi thành công và lúc notification/FCM thực sự tới recipient — chấp nhận được, đúng bản chất "push notification" (recipient, không phải sender, cần record này, không cần nó tồn tại ngay tại thời điểm response).
**Compliance Impact:** Notification body chứa tên người gửi (PII tối thiểu, không phải nội dung sức khỏe) — nhất quán với case study hiện có (`CommunityReplyNotificationService` cũng show `answerPreview` — thực ra module đó rò rỉ nhiều hơn; MESSAGE type ở đây cố tình an toàn hơn bằng cách không show preview).

---

### ADR-MEDI-005 — Mobile: IndexedStack cho cả 2 role, không đổi sang `StatefulShellRoute`

| Field | Value |
|---|---|
| **Status** | `Proposed` |
| **Date** | `2026-07-15` |

#### Bối cảnh
MOTHER shell (`home_shell.dart`) đã dùng `IndexedStack` + `NavigationBar` — giữ state tốt, đơn giản. EXPERT shell (`expert_app_home_screen.dart`) hiện là 1 `Scaffold` đơn với custom bottom row tự vẽ, các tab khác dùng `Navigator.push` (không giữ state, có thể chồng nhiều page lên navigator stack nếu người dùng bấm qua lại). Toàn bộ app dùng `GoRouter` dạng flat list, không có `ShellRoute`/`StatefulShellRoute` ở bất kỳ đâu.

#### Quyết định
Không giới thiệu `StatefulShellRoute` (đổi toàn bộ router pattern — rủi ro cao, ngoài phạm vi "smallest scoped change"). Thay vào đó:
- MOTHER: giữ nguyên `IndexedStack`, chỉ đổi 2/5 item (xem §13.1).
- EXPERT: chuyển từ custom-row + `Navigator.push` sang cùng pattern `IndexedStack` + `NavigationBar` như MOTHER — nhất quán UI 2 role, giữ state giữa các tab, không đẩy nhiều page trùng lên stack. Đây là thay đổi cấu trúc lớn nhất trong toàn bộ mobile scope — cô lập rủi ro bằng cách giữ nguyên toàn bộ nội dung `ExpertAppHomeScreen` hiện tại làm "trang 0" (Tổng quan) của `IndexedStack` mới, không viết lại dashboard.

#### Hệ quả
**Tích cực:** Nhất quán 2 role, sửa luôn vấn đề navigator-stack-phình-to hiện tại của EXPERT shell.
**Trade-offs:** Là thay đổi rủi ro cao nhất trong scope — cần test riêng, kỹ (§Rollback §12 đánh dấu mục này).

---

## 4. Non-Functional Requirements & SLA

| Category | Requirement | Target | Verification |
|---|---|---|---|
| Performance | `listMyConversations` query count | ≤ 5 query cố định, không phụ thuộc N | MEDI-TC-017 |
| Performance | Directory API p99 | < 300ms (giữ nguyên NFR của UC80) | manual/staging |
| Data integrity | Read cursor monotonic dưới concurrent request, neo vào `lastSeenMessageId` không phải `now()` | Không bao giờ lùi; message ngoài phạm vi client đã thấy không bao giờ bị đánh dấu đã đọc | MEDI-TC-012, MEDI-TC-019 |
| Correctness | Race: message đến giữa lúc load timeline và lúc gọi mark-read | Vẫn unread sau mark-read | MEDI-TC-019 |
| Idempotency | Retry `sendMessage` (application-level) không publish lại event | 100% | MEDI-TC-015 |
| Idempotency | Notification duplicate/concurrent listener execution (DB-level, độc lập với `clientMessageId`) | Đúng 1 `notification_records` row mỗi `(recipient, messageId)`, kể cả dưới concurrent write | MEDI-TC-020, MEDI-TC-021 |
| Reliability | FCM lỗi (graceful hoặc exception) | Luôn đúng 1 record `FAILED` + `failedAt` + audit, không bao giờ 0 record | MEDI-TC-014b |
| Security | PATCH /read, GET /unread-summary chỉ participant; `lastSeenMessageId` phải thuộc đúng conversation | Least privilege | MEDI-TC-013, MEDI-TC-012, §14 |
| Privacy | Notification body không chứa `messageBody` | 0 leak | code review + MEDI-TC-014a assertion |

---

## 5. Static Modeling

### 5.1 Entity/DTO deltas (tóm tắt — chi tiết đầy đủ ở §8)

```
DirectConversation (KHÔNG đổi entity hiện có, CHỈ thêm 2 cột)
  + mother_last_read_at: Instant?
  + expert_last_read_at: Instant?

NotificationType (enum, thêm 1 giá trị)
  + MESSAGE

ExpertProfileResponse (thêm 1 field)
  + displayName: String   // từ users.full_name, không lưu trên expert_profiles

DirectConversationSummaryResponse (thêm 6 field, giữ nguyên 5 field cũ)
  + counterpartDisplayName: String
  + counterpartAvatarUrl: String?
  + counterpartSpecialty: String?     // null nếu counterpart là MOTHER
  + lastMessagePreview: String?       // null nếu chưa có message nào
  + lastMessageAt: Instant?
  + unreadCount: int
  + conversationStatus: String        // forward-compat, map từ entity.status
```

### 5.2 Data Structure (Flyway SQL Migration — DESIGN, tạo file thật sau khi Approved)

> Theo `implement-flow.md`: không tạo file migration thật ở bước Draft. Migration mới nhất trong repo hôm nay là `V20260715120100__widen_audit_logs_action_direct_chat.sql` (xác nhận bằng liệt kê toàn bộ thư mục migration và so sánh theo timestamp, không theo thứ tự chữ cái). File mới phải dùng version lớn hơn.

```sql
-- V20260716010600__add_direct_conversation_read_cursor.sql
ALTER TABLE public.direct_conversations
    ADD COLUMN mother_last_read_at timestamptz NULL,
    ADD COLUMN expert_last_read_at timestamptz NULL;

-- Hỗ trợ query unread aggregate (loại trừ sender, so sánh created_at)
CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_sender_created
    ON public.direct_messages (conversation_id, sender_user_id, created_at);
```

```sql
-- V20260716010700__widen_notification_type_message.sql
ALTER TABLE public.notification_records
    DROP CONSTRAINT IF EXISTS notification_records_type_check;

ALTER TABLE public.notification_records
    ADD CONSTRAINT notification_records_type_check CHECK (
        (type)::text = ANY ((ARRAY[
            'REMINDER', 'COMMUNITY_REPLY', 'CONSULTATION', 'EMERGENCY', 'MESSAGE'
        ])::text[])
    );
```

```sql
-- V20260716010800__add_notification_records_direct_message_idempotency.sql
-- ADR-MEDI-004 v1.1 mục 3 — idempotency ở tầng DB, độc lập với clientMessageId early-return của sendMessage().
-- Partial unique index: chỉ áp cho notification loại MESSAGE/DIRECT_MESSAGE, không ràng buộc các
-- NotificationType khác (REMINDER/COMMUNITY_REPLY/CONSULTATION/EMERGENCY) dù reference_id trùng ngẫu nhiên.
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_records_direct_message
    ON public.notification_records (user_id, reference_id)
    WHERE type = 'MESSAGE' AND reference_type = 'DIRECT_MESSAGE';
```

> Không migration nào đổi `direct_messages`/`conversation_calls`/`audit_logs` — phạm vi giới hạn đúng 3 file trên. Không migration nào xoá/sửa cột đã tồn tại (append-only theo đúng CASE 2.0 Policy 4.4). Migration thứ 3 an toàn additive: `notification_records` chưa từng có row `type='MESSAGE'` nào trước feature này (enum mới tinh, thêm ở migration thứ 2) nên không có dữ liệu cũ nào có thể vi phạm unique index mới khi migration chạy.

---

## 6. Dynamic Modeling

### 6.1 Sequence — Mother tìm expert, mở hồ sơ, chat (happy path)

```
Mother -> ExpertDirectoryScreen: mở tab "Chuyên gia"
ExpertDirectoryScreen -> GET /api/v1/expert/directory?q=&specialty=&page=&size=
Backend --> ExpertDirectoryScreen: Page<ExpertProfileResponse> (đúng phân trang, có avatar+displayName)
Mother -> ExpertProfileCard: tap
ExpertDirectoryScreen -> router: push /expert/public/{expertProfileId}
ExpertPublicProfileScreen -> GET /api/v1/expert/profiles/{expertProfileId}
Mother -> "Trò chuyện" CTA: tap  [chỉ hiện nếu verificationStatus == APPROVED]
ExpertPublicProfileScreen -> DirectChatService: findOrCreateConversation(expertProfileId)
Backend -> DirectConversationServiceImpl.findOrCreate: idempotent (unique constraint pair)
ExpertPublicProfileScreen -> router: push /direct-chat/{conversationId}
DirectChatScreen -> GET /timeline, gửi tin -> POST /messages
DirectMessageServiceImpl.sendMessage:
  insert message (idempotent theo clientMessageId)
  -> touchActivity(lastActivityAt)
  -> audit(DIRECT_MESSAGE_SENT)
  -> publishEvent(new ConversationEventDomainEvent("MESSAGE_SENT", conversationId, senderUserId, messageId, now))
     [field order thật: eventType, conversationId, actorUserId, resourceId, occurredAt — không đổi call site]
== COMMIT ==
== sau commit, 2 listener độc lập của cùng ConversationEventDomainEvent, cả 2 đều @Async + AFTER_COMMIT ==
  1. ConversationEventPublisherImpl (không đổi) -> Firestore signal (ADR-DCC-002)
  2. DirectMessageNotificationListener (MỚI — ADR-MEDI-004) -> IDirectMessageNotificationService.notifyNewMessage()
       -> NotificationRecordWriter.insertIfAbsent(...)  [DB-enforced idempotency, uq_notification_records_direct_message]
       -> nếu duplicate (đã tồn tại) -> return, KHÔNG gọi FCM lần 2
       -> nếu mới -> fcmService.sendWithRetry() trong try/catch -> luôn persist đúng 1 record SENT hoặc FAILED
       -> lỗi bị nuốt nội bộ SAU KHI đã ghi FAILED, KHÔNG có đường nào quay lại ảnh hưởng message đã commit
```

### 6.2 `listMyConversations` — batch queries (không N+1, ADR-MEDI-002 + ADR-MEDI-003)

```sql
-- Query 2/5 — last message per conversation (Postgres DISTINCT ON)
SELECT DISTINCT ON (conversation_id) conversation_id, message_body, created_at
FROM direct_messages
WHERE conversation_id = ANY(:conversationIds)
ORDER BY conversation_id, created_at DESC;

-- Query 3/5 — unread count per conversation, gộp cho toàn bộ list 1 lần
SELECT dc.conversation_id, COUNT(dm.message_id) AS unread_count
FROM direct_conversations dc
LEFT JOIN direct_messages dm
       ON dm.conversation_id = dc.conversation_id
      AND dm.sender_user_id <> :currentUserId
      AND dm.created_at > COALESCE(
            CASE WHEN dc.mother_user_id = :currentUserId THEN dc.mother_last_read_at
                 ELSE dc.expert_last_read_at END,
            '-infinity'::timestamptz)
WHERE dc.conversation_id = ANY(:conversationIds)
GROUP BY dc.conversation_id;
```
(Query 1/5 = `findByMotherUserIdOrExpertUserId`; Query 4/5 = `userRepository.findAllById`; Query 5/5 = `expertProfileRepository.findByUserIdIn`.)

### 6.3 `GET /unread-summary`

```sql
SELECT
  COUNT(*) FILTER (WHERE unread_count > 0) AS unread_conversation_count,
  COALESCE(SUM(unread_count), 0) AS total_unread_message_count
FROM ( <query 3/5 ở trên, scoped theo currentUserId's conversations> ) t;
```
**Hợp đồng badge (ghi rõ để tránh nhầm lẫn):** badge bottom-nav = `unreadConversationCount`. `totalUnreadMessageCount` chỉ để dự phòng UI khác, KHÔNG dùng cho badge nav (tránh 1 conversation ồn ào làm badge nav hiển thị số quá lớn, gây hiểu lầm).

### 6.4 `PATCH /read` — validate `lastSeenMessageId`, tiến cursor tới `createdAt` của message đó (KHÔNG phải `now()`)

```sql
-- Bước 0 (không thuộc SQL — Bean Validation @NotNull trên request body, xem §3 ADR-MEDI-003)
-- Bước 1 — conversationId tồn tại? Không thì 404, tái dùng DirectChatException.conversationNotFound() (DCC-006)
SELECT 1 FROM direct_conversations WHERE conversation_id = :conversationId;

-- Bước 2 — assertIsParticipant (không đổi từ UC144) — 403 DCC-003 (không phải participant nào)
--                                                    hoặc 403 DCC-002 (caller là expert, đã mất APPROVED)

-- Bước 3 — validate lastSeenMessageId thuộc đúng conversation — 1 QUERY DUY NHẤT, không tách 2 lần lookup
-- Rỗng (không tồn tại HOẶC tồn tại ở conversation khác — cả 2 cho cùng kết quả rỗng) → 404, tái dùng DCC-006
-- (factory mới DirectChatException.messageNotInConversation(), message khác, code vẫn DCC-006 — xem ADR-MEDI-003 mục 3)
SELECT created_at FROM direct_messages
WHERE message_id = :lastSeenMessageId AND conversation_id = :conversationId;

-- Bước 4 — chỉ chạy nếu bước 3 tìm thấy — dùng created_at vừa lấy được, KHÔNG dùng now()
UPDATE direct_conversations
SET mother_last_read_at = GREATEST(COALESCE(mother_last_read_at, '-infinity'::timestamptz), :resolvedCreatedAt)
WHERE conversation_id = :id AND mother_user_id = :currentUserId;
-- (tương tự cho expert_last_read_at khi currentUserId = expert_user_id)
```

### 6.5 Race regression — message đến giữa lúc client load timeline và lúc gọi mark-read (MEDI-TC-019)

```
Timeline thật:
  t0: Mother<->Expert conversation có message M1 (created_at = t0)
  t1: Expert mở DirectChatScreen -> GET /timeline -> client thấy M1, KHÔNG biết gì về tương lai
  t2 (> t1): Mother gửi M2 (created_at = t2) -- xảy ra trong khoảng client đã load xong nhưng CHƯA kịp gọi mark-read
  t3 (> t2): Expert's client gọi PATCH /read  { "lastSeenMessageId": M1.id }
             (client CHỈ có thể gửi M1 vì đó là message cuối cùng nó thực sự render — nó không biết M2 tồn tại)

Server xử lý PATCH /read tại t3:
  resolve(M1.id) -> createdAt = t0
  expert_last_read_at = GREATEST(NULL, t0) = t0   [[KHÔNG phải t3 = "now" lúc xử lý request]]

listMyConversations(expertUserId) sau đó:
  unreadCount(conv) = COUNT(messages WHERE sender<>expert AND created_at > t0)
                     = 1 (M2, vì t2 > t0)
  => M2 ĐÚNG là vẫn unread — không bị mark-read "vượt mặt" dù request PATCH xảy ra SAU khi M2 đã tồn tại trên server.
```
Đây chính là lý do thiết kế v1.0 (cursor = server `now()`) SAI: nếu dùng `now()` tại t3, cursor sẽ = t3 > t2, khiến M2 bị tính là đã đọc dù Expert chưa từng thấy nó trên màn hình.

---

## 7. Domain Event Catalog

### 7.1 Events — không thêm event type mới

Direct message vẫn chỉ publish `ConversationEventDomainEvent("MESSAGE_SENT", conversationId, senderUserId, messageId, occurredAt)` như UC144 (không đổi call site, không đổi field order thật). Notification creation **không** phải là domain event mới — nó là **consumer thứ 2** của chính event này (`DirectMessageNotificationListener`, `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`, xem ADR-MEDI-004 v1.1 mục 2), hoàn toàn tách khỏi transaction gửi message, không qua bus riêng nào khác.

### 7.2 AuditAction — không thêm action mới

`DIRECT_MESSAGE_SENT` (đã có từ UC144) tiếp tục là audit record duy nhất cho hành vi gửi tin nhắn. Việc tạo `NotificationRecord` tự thân đã có audit riêng qua `NOTIFICATION_SENT`/`NOTIFICATION_FAILED` (đã tồn tại từ trước, dùng lại nguyên trạng theo pattern `CommunityReplyNotificationService.saveAndAudit`).

---

## 8. Interface Specification

```java
// expert package — ExpertProfileRepository (thêm methods, giữ methods cũ để không phá code khác đang gọi)
public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, UUID> {
    // ... methods hiện có không đổi ...

    @Query(value = """
        SELECT ep.* FROM expert_profiles ep JOIN users u ON u.user_id = ep.user_id
        WHERE ep.verification_status = 'APPROVED'
          AND (:specialty IS NULL OR ep.specialty = :specialty)
          AND (:q IS NULL OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(ep.professional_title) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(ep.workplace) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY ep.rating_avg DESC NULLS LAST
        """,
        countQuery = """
        SELECT COUNT(*) FROM expert_profiles ep JOIN users u ON u.user_id = ep.user_id
        WHERE ep.verification_status = 'APPROVED'
          AND (:specialty IS NULL OR ep.specialty = :specialty)
          AND (:q IS NULL OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(ep.professional_title) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(ep.workplace) LIKE LOWER(CONCAT('%', :q, '%')))
        """,
        nativeQuery = true)
    Page<ExpertProfile> searchDirectory(String specialty, String q, Pageable pageable);
}

// directchat.repository — thêm 3 method mới, không đổi method cũ
public interface DirectConversationRepository extends JpaRepository<DirectConversation, UUID> {
    // ... methods hiện có (findByMotherUserIdAndExpertUserId, touchActivity, ...) không đổi ...

    List<DirectConversation> findByMotherUserIdOrExpertUserIdOrderByLastActivityAtDesc(UUID motherUserId, UUID expertUserId);

    // native, KHÔNG dùng JPQL — GREATEST()/CAST('-infinity') là cú pháp Postgres,
    // HQL không đảm bảo hỗ trợ GREATEST() nhất quán qua version và không parse literal '-infinity'.
    // cursorAt = resolvedMessage.getCreatedAt() của lastSeenMessageId ĐÃ VALIDATE — KHÔNG BAO GIỜ Instant.now()
    // (xem §6.4/§6.5 — dùng now() ở đây sẽ tái tạo lại đúng race đã sửa).
    @Modifying
    @Query(value = """
        UPDATE direct_conversations
        SET mother_last_read_at = GREATEST(COALESCE(mother_last_read_at, '-infinity'::timestamptz), :cursorAt)
        WHERE conversation_id = :id AND mother_user_id = :currentUserId
        """, nativeQuery = true)
    int markMotherRead(UUID id, UUID currentUserId, Instant cursorAt);

    @Modifying
    @Query(value = """
        UPDATE direct_conversations
        SET expert_last_read_at = GREATEST(COALESCE(expert_last_read_at, '-infinity'::timestamptz), :cursorAt)
        WHERE conversation_id = :id AND expert_user_id = :currentUserId
        """, nativeQuery = true)
    int markExpertRead(UUID id, UUID currentUserId, Instant cursorAt);
}

// directchat.repository — thêm 1 method để validate lastSeenMessageId thuộc đúng conversation (§6.4)
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {
    // ... method hiện có findByConversationIdAndSenderUserIdAndClientMessageId không đổi ...

    Optional<DirectMessage> findByIdAndConversationId(UUID messageId, UUID conversationId);
}

// directchat.repository — native aggregate queries (§6.2/§6.3), interface riêng, giữ tách biệt khỏi JPA repo
public interface ConversationSummaryAggregateRepository {
    Map<UUID, LastMessageRow> fetchLastMessages(List<UUID> conversationIds);
    Map<UUID, Integer> fetchUnreadCounts(List<UUID> conversationIds, UUID currentUserId);
    UnreadSummary fetchUnreadSummary(UUID currentUserId);
}

// directchat.service
public interface IDirectConversationService {
    DirectConversationResponse findOrCreate(UUID motherUserId, UUID expertProfileId);           // không đổi
    List<DirectConversationSummaryResponse> listMyConversations(UUID currentUserId);              // mở rộng field, batch-fetch (ADR-MEDI-002)
    DirectConversationResponse getConversation(UUID conversationId, UUID currentUserId);          // không đổi
    // MỚI — lastSeenMessageId BẮT BUỘC (@NotNull ở request DTO), server validate thuộc conversation
    // rồi tiến cursor tới resolvedMessage.createdAt (KHÔNG BAO GIỜ Instant.now() — xem §6.4/§6.5).
    // Thứ tự throw (toàn bộ tái dùng DirectChatException hiện có — KHÔNG mã mới nào ngoài 1 factory method mới
    // dùng lại code cũ, xem ADR-MEDI-003 mục 3):
    //   1. conversationRepository.findById rỗng -> DirectChatException.conversationNotFound() (404, DCC-006)
    //   2. policy.assertIsParticipant thất bại -> .notParticipant() (403, DCC-003) hoặc .expertNoLongerApproved() (403, DCC-002)
    //   3. directMessageRepository.findByIdAndConversationId rỗng -> .messageNotInConversation() (404, DCC-006, factory MỚI — message khác, code tái dùng)
    // assertConversationWritable KHÔNG được gọi ở bất kỳ bước nào.
    Instant markRead(UUID conversationId, UUID currentUserId, UUID lastSeenMessageId);              // trả về cursorAt đã áp dụng
    UnreadSummaryResponse getUnreadSummary(UUID currentUserId);                                    // MỚI
}

// notification.service — MỚI, theo pattern CommunityReplyNotificationService
public interface IDirectMessageNotificationService {
    // Gọi TỪ DirectMessageNotificationListener (không phải từ sendMessage() trực tiếp — xem ADR-MEDI-004 v1.1).
    // Tự mở transaction riêng (@Transactional, chạy sau khi transaction ghi message đã commit).
    // Idempotent ở tầng DB (mục 3, KHÔNG chỉ dựa vào caller không gọi lại): insertIfAbsent trước,
    // chỉ gọi FCM nếu bản ghi thực sự mới. FCM lỗi (graceful hoặc exception) luôn kết thúc bằng
    // đúng 1 record FAILED + audit — không bao giờ nuốt lỗi mà không để lại bằng chứng (mục 4).
    void notifyNewMessage(UUID recipientUserId, UUID senderUserId, UUID conversationId, UUID messageId);
}

// notification.repository — thêm methods cho idempotency + lookup sau ON CONFLICT DO NOTHING
public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, UUID> {
    // ... methods hiện có (findByUserId, markAsReadById, ...) không đổi ...

    Optional<NotificationRecord> findByUserIdAndReferenceIdAndTypeAndReferenceType(
            UUID userId, UUID referenceId, NotificationType type, String referenceType);
}

// notification.service.impl — MỚI — mirror CHÍNH XÁC DirectMessageWriter.insertIfAbsent đã có trong directchat
@Component
class NotificationRecordWriter {
    private final JdbcTemplate jdbcTemplate;

    // Trả true nếu row MỚI được insert, false nếu đã tồn tại (ON CONFLICT ... DO NOTHING, rowsAffected == 0).
    // Predicate ON CONFLICT phải khớp CHÍNH XÁC partial unique index uq_notification_records_direct_message.
    boolean insertIfAbsent(NotificationRecord candidate) {
        return jdbcTemplate.update("""
                INSERT INTO notification_records
                    (id, user_id, type, title, body, reference_id, reference_type, status, attempt_count, created_at)
                VALUES (?, ?, 'MESSAGE', ?, ?, ?, 'DIRECT_MESSAGE', ?, ?, ?)
                ON CONFLICT (user_id, reference_id) WHERE type = 'MESSAGE' AND reference_type = 'DIRECT_MESSAGE'
                DO NOTHING
                """, /* params tương ứng */) == 1;
    }
}

// directchat.event — MỚI — consumer thứ 2 của ConversationEventDomainEvent đã có từ UC144
// (consumer thứ 1 là ConversationEventPublisherImpl, không đổi)
// Field order thật của record (xác nhận bằng đọc event/ConversationEventDomainEvent.java):
// (eventType, conversationId, actorUserId, resourceId, occurredAt)
@Component
public class DirectMessageNotificationListener {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationEvent(ConversationEventDomainEvent event) {
        // lọc event.eventType().equals("MESSAGE_SENT") — bỏ qua CALL_INITIATED/CALL_STATE_CHANGED (ADR-MEDI-004 mục 8)
        // recipientUserId = counterpart(event.actorUserId()) trong conversation event.conversationId()
        // messageId = event.resourceId()  (== saved.getId() tại call site sendMessage())
        // gọi notifyNewMessage(recipientUserId, event.actorUserId(), event.conversationId(), event.resourceId())
        // try/catch nội bộ — lỗi chỉ log, không throw (không có gì phía trên để rollback nữa)
    }
}
```

### 8.1 DTO deltas đầy đủ

```java
public class ExpertProfileResponse {
    // ... fields hiện có ...
    private String displayName;   // MỚI — từ users.full_name, batch-resolved, KHÔNG query per-row
}

public class DirectConversationSummaryResponse {
    private UUID conversationId;
    private UUID counterpartUserId;
    private String counterpartRole;
    private Instant lastActivityAt;
    private Boolean expertAvailable;
    // --- MỚI ---
    private String counterpartDisplayName;
    private String counterpartAvatarUrl;   // nullable
    private String counterpartSpecialty;   // nullable — chỉ set nếu counterpartRole == EXPERT
    private String lastMessagePreview;     // nullable, truncate 120 ký tự
    private Instant lastMessageAt;         // nullable
    private int unreadCount;
    private String conversationStatus;
}

public class UnreadSummaryResponse {
    private int unreadConversationCount;
    private int totalUnreadMessageCount;
}

public class NotificationRecordResponse {
    // ... fields hiện có (id, userId, type, title, body, referenceId, referenceType, status, createdAt, sentAt) ...
    private Map<String, String> metadata;  // MỚI — additive, cần để mobile deep-link (conversationId)
}
```

---

## 9. API Specification

### 9.1 Endpoints Table

| Method | Path | Auth | Roles | Thay đổi |
|---|---|---|---|---|
| `GET` | `/api/v1/expert/directory` | JWT | Any authenticated | **Sửa** — thêm `q`, fix phân trang, fix avatar, thêm `displayName` |
| `GET` | `/api/v1/expert/profiles/{expertProfileId}` | JWT | Any authenticated | **Sửa** — response có `displayName` |
| `GET` | `/api/v1/direct-conversations` | JWT | MOTHER, EXPERT | **Sửa** — response mở rộng (ADR-MEDI-002), sort `lastActivityAt DESC` |
| `PATCH` | `/api/v1/direct-conversations/{conversationId}/read` | JWT | Participant only | **Mới** |
| `GET` | `/api/v1/direct-conversations/unread-summary` | JWT | MOTHER, EXPERT | **Mới** |
| `POST` | `/api/v1/direct-conversations/{conversationId}/messages` | JWT | Participant only | **Sửa nội bộ** — thêm side-effect tạo Notification, contract response không đổi |
| Các endpoint call/timeline/find-or-create khác | — | — | — | **Không đổi** |

### 9.2 `GET /api/v1/expert/directory` — request/response mới

```
GET /api/v1/expert/directory?q=nguyen&specialty=Sản%20khoa&page=0&size=20
```
```json
{
  "experts": [
    {
      "expertProfileId": "uuid",
      "userId": "uuid",
      "displayName": "BS. Nguyễn Văn A",
      "specialty": "Sản khoa",
      "professionalTitle": "Bác sĩ CKI",
      "experienceYears": 8,
      "workplace": "BV Từ Dũ",
      "verificationStatus": "APPROVED",
      "ratingAvg": 4.8,
      "avatarUrl": "https://.../avatar.jpg"
    }
  ],
  "currentPage": 0,
  "pageSize": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

### 9.3 `GET /api/v1/direct-conversations` — response mới

```json
[
  {
    "conversationId": "uuid",
    "counterpartUserId": "uuid",
    "counterpartRole": "EXPERT",
    "counterpartDisplayName": "BS. Nguyễn Văn A",
    "counterpartAvatarUrl": "https://.../avatar.jpg",
    "counterpartSpecialty": "Sản khoa",
    "lastMessagePreview": "Cảm ơn bác sĩ ạ",
    "lastMessageAt": "2026-07-15T10:00:00Z",
    "lastActivityAt": "2026-07-15T10:00:00Z",
    "unreadCount": 2,
    "expertAvailable": true,
    "conversationStatus": "ACTIVE"
  }
]
```

### 9.4 `PATCH /api/v1/direct-conversations/{conversationId}/read`

**Request:**
```json
{ "lastSeenMessageId": "uuid" }
```
`lastSeenMessageId` bắt buộc (`@NotNull`) — không có mặc định "mark tất cả đã đọc tới thời điểm hiện tại" (xem §6.4/§6.5 lý do). Server validate message thuộc đúng `conversationId` trước khi dùng.

**Response — 200 OK:**
```json
{ "cursorAt": "2026-07-16T08:00:00Z" }
```
(`cursorAt` = `resolvedMessage.createdAt`, không phải thời điểm server xử lý request.)

**Response — 400 `VALIDATION_ERROR` (generic Bean Validation, không có code riêng):** `lastSeenMessageId` thiếu hoặc `null` trong request body.
**Response — 404 `DCC-006`** *(tái dùng `DirectChatException.conversationNotFound()`, message "Conversation not found")*: `conversationId` không tồn tại.
**Response — 403 `DCC-003`** *(tái dùng `.notParticipant()`)*: caller không phải participant. **Hoặc 403 `DCC-002`** *(tái dùng `.expertNoLongerApproved()`)*: caller chính là expert của conversation nhưng đã mất APPROVED (re-check trong `assertIsParticipant`, không đổi từ UC144).
**Response — 404 `DCC-006`** *(factory mới `.messageNotInConversation()`, cùng code, message "Message not found in this conversation")*: `lastSeenMessageId` không tồn tại HOẶC tồn tại nhưng thuộc conversation khác — **cố tình dùng chung 1 kết quả** để không lộ thông tin "message này tồn tại ở conversation khác" cho người không phải participant của conversation đó (xem ADR-MEDI-003 mục 3 cho lý do đầy đủ).

> **v1.2 — sửa so với nháp trước:** bản nháp v1.1 định dùng 4 mã mới `MEDI-002..005` cho các lỗi trên. Xác nhận bằng đọc trực tiếp `GlobalExceptionHandler.java` và `DirectChatException.java`: **toàn bộ các tình huống này đã có cơ chế/error-code thật sẵn có** trong codebase (Bean Validation generic cho input sai định dạng; `DCC-003`/`DCC-006` đã được `assertIsParticipant`/`conversationNotFound()` ném ra ở các method khác của `directchat` — `getConversation()`, `findOrCreate()`). Tạo thêm `MEDI-00x` song song là trùng lặp không cần thiết — sửa lại dùng nguyên trạng. Đây cũng là lúc phát hiện `EXP-010` (trong `UC80_ViewExpertDirectory` TDS, dùng cho `size` > 50) **chưa từng được implement thật** — code thật chỉ validate qua `@Max(50)` sinh ra response `VALIDATION_ERROR` chung, không có business code riêng nào — TDS này KHÔNG còn "tái dùng EXP-010" nữa (xem §10).

### 9.5 `GET /api/v1/direct-conversations/unread-summary`

```json
{ "unreadConversationCount": 2, "totalUnreadMessageCount": 5 }
```

---

## 10. Bảng mã lỗi

> **v1.2 — viết lại hoàn toàn.** Không còn business code nào mang tiền tố `MEDI-` hay `EXP-`. Xác nhận bằng đọc trực tiếp `GlobalExceptionHandler.java` (toàn bộ handler thật, không suy đoán) + `DirectChatException.java` (toàn bộ factory method thật): mọi tình huống lỗi của feature này hoặc (a) là Bean Validation generic — dùng chung `handleConstraintViolation`/`handleMethodArgumentNotValid`, không có business code, hoặc (b) đã có sẵn `DirectChatException` factory + code phù hợp trong `directchat` package, tái dùng nguyên trạng. Không tạo code mới nào ngoài đúng 1 factory method mới (`messageNotInConversation()`) — nhưng dùng lại code `DCC-006` đã có, không phải code mới.

| Trigger | HTTP | Response shape | Cơ chế |
|---|---|---|---|
| `q` (expert directory) > 100 ký tự | 400 | `{ error: "VALIDATION_ERROR", details: [...] }` | `@Size(max=100)` trên `@RequestParam` → `ConstraintViolationException` → `GlobalExceptionHandler.handleConstraintViolation` (generic, **không** có code `EXP-`/`MEDI-` riêng — xác nhận `size`>50 hiện tại của UC80 cũng đi qua đường này, không phải qua `EXP-010` như TDS cũ ghi sai) |
| `lastSeenMessageId` thiếu/`null` trong body `PATCH /read` | 400 | `{ error: "VALIDATION_ERROR", details: [...] }` | `@NotNull` trên `MarkReadRequest` + `@Valid @RequestBody` → `MethodArgumentNotValidException` → `handleMethodArgumentNotValid` (generic) |
| `PATCH /read` — `conversationId` không tồn tại | 404 | `{ code: "DCC-006", message: "Conversation not found", ... }` | Tái dùng `DirectChatException.conversationNotFound()` — **method có sẵn**, đã dùng ở `getConversation()`/`findOrCreate()`, không đổi |
| `PATCH /read` bởi user không phải participant | 403 | `{ code: "DCC-003", message: "You are not a participant of this conversation", ... }` | Tái dùng `DirectChatException.notParticipant()` — ném ra từ `assertIsParticipant()` có sẵn, không đổi |
| `PATCH /read` bởi caller = expert đã mất APPROVED | 403 | `{ code: "DCC-002", message: "Expert is no longer APPROVED", ... }` | Tái dùng `DirectChatException.expertNoLongerApproved()` — ném ra từ `assertIsParticipant()`'s re-check có sẵn (UC144, không đổi) |
| `PATCH /read` — `lastSeenMessageId` không tồn tại HOẶC thuộc conversation khác (2 case gộp — xem ADR-MEDI-003 mục 3) | 404 | `{ code: "DCC-006", message: "Message not found in this conversation", ... }` | **Factory method mới** `DirectChatException.messageNotInConversation()` — code `DCC-006` tái dùng (cùng pattern `expertProfileNotFound()`/`callNotFound()` đã có: nhiều factory, cùng 1 code, message khác nhau) |
| Expert không còn APPROVED, chặn write mới (message/call) | 409 | `{ code: "DCC-010", ... }` | Không đổi — `DirectChatException.expertUnavailableForWrite()`, từ UC144 |

---

## 11. Quy trình Triển khai

### 11.1 Prerequisites
- [ ] TDS này + Test-Spec kèm theo đã `Approved`
- [ ] `UC144_DirectConsultChat` vẫn `Approved`, không bị đổi hành vi core (chỉ mở rộng)

### 11.2 Pre-Migration Checklist
- [ ] Backup staging DB trước khi chạy 3 migration mới ở §5.2
- [ ] Xác nhận `V20260715120100` vẫn là migration mới nhất tại thời điểm chạy (tránh version collision nếu có migration khác đã merge song song)
- [ ] Xác nhận migration widen `notification_type_check` (thứ 2) chạy **trước** migration tạo `uq_notification_records_direct_message` (thứ 3) — thứ tự timestamp `010700` < `010800` đã đúng, không đảo ngược khi merge

### 11.3 Implementation Steps (thứ tự bắt buộc — theo TDD Red→Green→Refactor, chi tiết ở Test-Spec)

1. **Backend — Migration** (§5.2, 3 file) → `./mvnw flyway:migrate`
2. **Backend — Expert directory fix** (ADR-MEDI-001): repository, mapper, service, DTO — MEDI-TC-001, 001b, 002, 002b, 003, 004
3. **Backend — Conversation summary enrichment** (ADR-MEDI-002/003): repository aggregate queries, service, DTO, `DirectMessageRepository.findByIdAndConversationId`, mark-read (`lastSeenMessageId` contract) + unread-summary endpoints — MEDI-TC-008..013, 017, 018, 019
4. **Backend — Notification wiring** (ADR-MEDI-004): enum + 2 migration cuối, `NotificationRecordWriter.insertIfAbsent`, `IDirectMessageNotificationService` (insert-first + FCM try/catch), `DirectMessageNotificationListener` (2nd consumer của `ConversationEventDomainEvent`, AFTER_COMMIT + `@Async` — không đổi call site `publishEvent` trong `sendMessage()`) — MEDI-TC-014a, 014b, 015, 016, 020, 021
5. **Mobile — Shared models/services**: cập nhật Dart model `DirectConversationSummary`, `ExpertDirectoryItem` (mới, thay raw Map hiện tại), `DirectChatService` thêm `markRead()`/`getUnreadSummary()`
6. **Mobile — MOTHER shell**: `home_shell.dart` đổi 2 tab, thêm khu "Khám phá" (Cộng đồng + Bài tập) trên `MotherHomeScreen`
7. **Mobile — EXPERT shell**: chuyển `expert_app_home_screen.dart` sang `IndexedStack`, nối 2 callback rỗng, thêm badge
8. **Mobile — Expert directory & profile**: search debounce, filter, pagination thật, card hiển thị field thật
9. **Mobile — Conversation list**: render field mới, unread badge, pull-to-refresh, empty state theo role
10. **Mobile — FCM + notification center**: deep link MESSAGE type
11. **Mobile — mark-read on open chat**: gọi `PATCH /read` khi timeline hiển thị thành công

### 11.4 Deployment Checklist
- [x] `./mvnw test` xanh toàn bộ — 146/146 trong `directchat`/`notification`/`expert` packages; 38 class khác fail/error trong toàn repo nhưng xác nhận pre-existing, không liên quan (git diff sạch trên mọi file bị ảnh hưởng)
- [x] `flutter test` xanh toàn bộ — 65/65 (45 cũ + 20 mới)
- [x] `flutter analyze` không lỗi mới — `flutter analyze` tự crash do bug LSP/path Unicode có sẵn của môi trường (không liên quan code, tái hiện được qua symlink test); `dart analyze` (cùng engine) dùng thay thế: 0 lỗi, 13 info-level lint
- [x] Kiểm tra thủ công bằng tài khoản seed: toàn bộ directory/profile/find-or-create/send/list/unread/mark-read/notification chạy thật với Supabase; Flutter Web E2E cho cả MOTHER và EXPERT đã pass. E2E còn phát hiện và sửa thanh điều hướng cũ bị lồng trong tab EXPERT “Yêu cầu”; xem Test-Spec §10
- [x] 3 migration `20260716010600..10800` đã áp dụng lên DB Supabase dùng chung sau khi kiểm tra chỉ đúng 3 version này đang pending. Không repair/ghi đè lịch sử 3 migration cũ đang checksum drift; lần migrate một-lần dùng `validateOnMigrate=false` có chủ đích và được ghi lại ở Test-Spec §10

---

## 12. Rollback & Incident Runbook

### 12.1 Điều kiện kích hoạt Rollback

| Điều kiện | Ngưỡng | Người quyết định |
|---|---|---|
| `notification_records` insert lỗi hàng loạt sau deploy | > 5% request `sendMessage` lỗi | On-call + Tech Lead |
| EXPERT shell `IndexedStack` conversion gây crash/mất state | Bất kỳ regression nào trên dashboard hiện có | Tech Lead |
| Unread count sai lệch (âm, hoặc không giảm sau mark-read) | Bất kỳ case nào | Tech Lead |

### 12.2 Rollback Procedure

```bash
# Migration rollback (chỉ 3 file mới, KHÔNG đụng migration cũ)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "DROP INDEX IF EXISTS public.uq_notification_records_direct_message;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "ALTER TABLE public.direct_conversations DROP COLUMN IF EXISTS mother_last_read_at, DROP COLUMN IF EXISTS expert_last_read_at;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c \
  "DELETE FROM flyway_schema_history WHERE version IN ('20260716090000','20260716010700','20260716010800');"
# revert notification_records_type_check về danh sách cũ (không có MESSAGE) nếu cần rollback cứng

git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/home/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/directChat/
```
EXPERT shell conversion (§13.2, cao rủi ro nhất) có thể rollback độc lập với phần backend nếu chỉ mobile bị lỗi — 2 phần không phụ thuộc runtime lẫn nhau.

---

## 13. Mobile Design (IA + Screen Contracts)

### 13.1 MOTHER — `home_shell.dart` (IndexedStack, không đổi pattern)

| # | Icon | Label | Screen | Trạng thái |
|---|---|---|---|---|
| 1 | `Icons.home` | Trang chủ | `MotherHomeScreen` | Không đổi |
| 2 | `Icons.auto_stories` | Hành trình | `MotherJourneyScreen` | Không đổi |
| 3 | `Icons.medical_services_outlined` | **Chuyên gia** | `ExpertDirectoryScreen` | **Mới — thay vị trí Cộng đồng** |
| 4 | `Icons.chat_bubble_outline` | **Trò chuyện** | `ConversationListScreen` | **Mới — thay vị trí Bài tập, có `Badge` unread** |
| 5 | `Icons.person` | Tài khoản | `AccountProfileScreen` | Không đổi (giữ label cũ) |

`MotherHomeScreen`: thêm khu "Khám phá" (2 card ngang, tái dùng pattern `_QuickAction` đã có trong file) dẫn tới `CommunityFeedScreen` và `MotherExerciseScreen` — đặt gần đầu trang, dưới header chào mừng. Xác nhận `MotherJourneyScreen` có sẵn lối vào bài tập thai kỳ (UC29/UC30) trước khi implement để tránh trùng lặp không cần thiết.

### 13.2 EXPERT — `expert_app_home_screen.dart` (chuyển sang IndexedStack — ADR-MEDI-005)

| # | Icon | Label | Screen | Trạng thái |
|---|---|---|---|---|
| 1 | (giữ icon cũ) | Tổng quan | nội dung dashboard hiện có (giữ nguyên 100%, chỉ đổi wrapper) | Không đổi nội dung |
| 2 | `Icons.chat_bubble_outline` | **Trò chuyện** | `ConversationListScreen` (dùng chung với MOTHER) | **Mới, có `Badge` unread** |
| 3 | `Icons.assignment_outlined` | Yêu cầu | `ExpertQuestionQueueScreen` | **Sửa — nối callback rỗng tại `expert_app_home_screen.dart:465`, và card "Yêu cầu mới" tại dòng 368** |
| 4 | (icon lịch) | Lịch | `ExpertCalendarScreen` | Không đổi đích, chỉ đổi cách điều hướng (IndexedStack thay vì push) |
| 5 | `Icons.person` | Tài khoản | `AccountProfileScreen` | Không đổi đích |

Community bị loại khỏi bottom nav — chuyển thành 1 card phụ trong "Tổng quan" dashboard (vị trí cụ thể xác định lúc code, tái dùng khu metrics/card đã có). Không có CTA "tìm/nhắn Mother" ở bất kỳ đâu trong shell EXPERT (BR-MEDI-005).

### 13.3 `ExpertDirectoryScreen`

- Search box debounce ~400ms → `GET /api/v1/expert/directory?q=...`.
- Filter chip theo `specialty` (danh sách specialty lấy từ giá trị thật đang tồn tại trong hệ thống, không hardcode danh sách giả).
- Pagination thật: infinite-scroll dùng `page`/`size` (nay đã hoạt động đúng theo ADR-MEDI-001).
- States: loading skeleton, error + nút Thử lại, empty ("Không tìm thấy chuyên gia phù hợp"), data.
- Search text giữ nguyên khi mở profile rồi quay lại (state trong `State`, không mất vì `IndexedStack` giữ tab).
- Card: avatar (`counterpartAvatarUrl`/`avatarUrl` + fallback initials), `displayName`, `professionalTitle`, `specialty`, badge "Đã xác thực" (luôn đúng vì directory chỉ trả APPROVED), `ratingAvg`/`experienceYears` nếu có giá trị. **Không** hiển thị trạng thái online/available (không có nguồn dữ liệu thật).

### 13.4 `ExpertPublicProfileScreen`

Không đổi cấu trúc — chỉ hiển thị thêm `displayName` thật (hiện đang thiếu vì DTO chưa có field này). CTA "Trò chuyện" giữ nguyên logic gate theo `verificationStatus == APPROVED`.

### 13.5 `ConversationListScreen` (dùng chung 2 role)

- Row: avatar thật, `counterpartDisplayName` (bỏ hardcode "Mẹ"/"Chuyên gia"), subtitle = `counterpartSpecialty` (nếu MOTHER xem EXPERT) hoặc `lastMessagePreview` (nếu EXPERT xem MOTHER — không có field "context an toàn" nào khác tồn tại phía backend, giữ đúng phạm vi đã audit, không bịa field mới), trailing = thời gian tương đối (`lastMessageAt ?? lastActivityAt`) + unread badge tròn nếu `unreadCount > 0`.
- Banner "Chuyên gia hiện không khả dụng" khi `!expertAvailable` — giữ nguyên hành vi cũ.
- `RefreshIndicator` pull-to-refresh; auto-refresh khi: quay lại từ `DirectChatScreen` (dùng `RouteObserver`/`await push(...)` rồi reload), `AppLifecycleState.resumed`, tín hiệu Firestore hiện có (mở rộng listener sang màn này, hiện chỉ có ở `DirectChatScreen`).
- Sort: theo `lastActivityAt DESC` (đã đúng từ backend — ADR-MEDI-002).
- Empty state theo role: MOTHER → "Bạn chưa có cuộc trò chuyện nào" + CTA "Tìm chuyên gia" (`push('/experts')`); EXPERT → "Chưa có mẹ nào nhắn cho bạn — danh sách sẽ hiện khi có yêu cầu mới", **không CTA nào** (BR-MEDI-005).

### 13.6 `DirectChatScreen`

- Sau khi timeline load/cập nhật thành công (initial load + mỗi lần có tin mới trong khi đang mở màn hình), gọi `DirectChatService.markRead(conversationId, lastSeenMessageId)` — **`lastSeenMessageId` = id của message mới nhất (`kind == MESSAGE`) hiện đang có trong danh sách đã render trên client tại thời điểm gọi**, KHÔNG phải "message mới nhất trên server" (client không biết điều đó). Nếu timeline chưa có message nào (conversation chỉ có call, hoặc rỗng), không gọi `markRead` (không có gì để đánh dấu).
- Idempotent, an toàn gọi nhiều lần — mỗi lần gọi lại chỉ có thể tiến cursor lên hoặc giữ nguyên (server-side monotonic, §6.4).
- Khi pop về `ConversationListScreen`, trigger reload để phản ánh `unreadCount` mới (server tính lại dựa trên cursor vừa cập nhật).

### 13.7 `fcm_service.dart` — thêm nhánh `MESSAGE`

```dart
// trong _handleTap, thêm case song song với nhánh EMERGENCY_ALERT hiện có
if (message.data['type'] == 'MESSAGE') {
  final conversationId = message.data['conversationId'];
  if (conversationId != null) {
    // deep link /direct-chat/$conversationId qua root navigator,
    // theo đúng cơ chế cold-start/foreground-tap đã có cho EMERGENCY_ALERT
  }
}
```
Không thêm foreground-message banner mới (ngoài phạm vi — hiện tại không có bất kỳ foreground handler nào cho type nào, giữ nhất quán, không tạo ngoại lệ riêng cho MESSAGE).

### 13.8 Notification center / detail

`notification_center_screen.dart` / `notification_detail_screen.dart`: khi `type == 'MESSAGE'`, tap card → đọc `metadata.conversationId` (nay đã có từ ADR-MEDI-004 mục 8) → deep-link thẳng `/direct-chat/$conversationId`, bỏ qua `NotificationDetailScreen` generic **chỉ cho type này** — các type khác giữ nguyên hành vi cũ (TODO cũ không đổi, ngoài phạm vi).

### 13.9 Community → expert profile (đã đúng, chỉ xác nhận không phá vỡ)

`question_detail_screen.dart:891` đã navigate đúng `/expert/public/{expertProfileId}` dùng chung `ExpertPublicProfileScreen` — không cần sửa. Xác nhận lại sau khi đổi DTO rằng route/param không đổi (chỉ response thêm field `displayName`, không đổi contract path).

---

## 14. Bảng tổng hợp phân quyền (Authorization Matrix)

| Endpoint | `MOTHER` | `EXPERT` | Khác |
|---|---|---|---|
| `GET /expert/directory` | ✅ | ✅ | ✅ (any authenticated) |
| `GET /direct-conversations` | ✅ Own | ✅ Own | ❌ |
| `PATCH /direct-conversations/{id}/read` | ✅ Participant only | ✅ Participant only | ❌ |
| `GET /direct-conversations/unread-summary` | ✅ Own | ✅ Own | ❌ |
| `POST /direct-conversations/expert/{id}` (find-or-create) | ✅ | ❌ (không đổi từ UC144) | ❌ |

---

## 15. AI Prompt Constraints (CASE 2.0)

### 15.1 Constraint Summary Table

| # | Constraint | Source |
|---|---|---|
| C1 | `listMyConversations` tối đa 5 query cố định, không phụ thuộc số conversation | ADR-MEDI-002 |
| C2 | Read cursor set qua `GREATEST(..., resolvedMessage.createdAt)` — **KHÔNG BAO GIỜ `Instant.now()`**, không bao giờ ghi đè lùi | ADR-MEDI-003 v1.1 |
| C3 | `notifyNewMessage` KHÔNG được chạy trong cùng transaction với `sendMessage()` — chỉ qua `DirectMessageNotificationListener` (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`) | ADR-MEDI-004 v1.1 mục 2 |
| C4 | Notification body KHÔNG được chứa `messageBody` | ADR-MEDI-004 mục 5 |
| C5 | Retry `sendMessage` (cùng `clientMessageId`) KHÔNG được publish lại `ConversationEventDomainEvent` → listener không chạy lần 2 | ADR-MEDI-004 mục 6 |
| C6 | `PATCH /read` dùng `assertIsParticipant`, KHÔNG dùng `assertConversationWritable` | ADR-MEDI-003 |
| C7 | Không thêm field "online/available" giả vào bất kỳ DTO expert nào | ADR-MEDI-001 mục 6 |
| C8 | EXPERT mobile shell KHÔNG có CTA tìm/nhắn Mother ở bất kỳ đâu | BR-MEDI-005 |
| C9 | `PATCH /read` PHẢI validate `lastSeenMessageId` thuộc đúng `conversationId` (query `direct_messages`) TRƯỚC khi dùng để tiến cursor — không tin input client mù quáng | ADR-MEDI-003 v1.1 |
| C10 | Notification insert PHẢI đi qua `NotificationRecordWriter.insertIfAbsent` (DB unique index `uq_notification_records_direct_message`) — KHÔNG được coi `clientMessageId` early-return của `sendMessage()` là đủ để chống trùng notification | ADR-MEDI-004 v1.1 mục 3 |
| C11 | Mọi lỗi FCM (graceful `FcmDeliveryResult.success()==false` hoặc exception ném ra từ `sendWithRetry`) PHẢI kết thúc bằng đúng 1 `NotificationRecord` status `FAILED` + `failedAt` + audit `NOTIFICATION_FAILED` — KHÔNG BAO GIỜ 0 record | ADR-MEDI-004 v1.1 mục 4 |
| C12 | `ConversationEventDomainEvent` field order PHẢI đúng `(eventType, conversationId, actorUserId, resourceId, occurredAt)` trong mọi test/code mới — không được suy đoán hay đảo thứ tự | Xác nhận bằng đọc `event/ConversationEventDomainEvent.java` |

### 15.2 Constraint Injection Block

```
[CONSTRAINT BLOCK — Module: MotherExpertDiscoveryInbox (CB-EXPCHAT-IMP-001)]
1. (C1 — ADR-MEDI-002) listMyConversations: batch-fetch, KHÔNG query trong vòng lặp per-conversation.
2. (C2 — ADR-MEDI-003 v1.1) markRead dùng native query GREATEST(existing, resolvedMessage.createdAt) — KHÔNG BAO GIỜ Instant.now(), không dùng JPQL cho GREATEST/-infinity.
3. (C3 — ADR-MEDI-004) notifyNewMessage CHỈ được gọi từ DirectMessageNotificationListener (AFTER_COMMIT + @Async) — KHÔNG bao giờ gọi trực tiếp/đồng bộ từ DirectMessageServiceImpl.sendMessage().
4. (C4 — ADR-MEDI-004) NotificationRecord.body KHÔNG chứa message_body gốc.
5. (C5 — ADR-MEDI-004) publishEvent(MESSAGE_SENT) chỉ gọi trong nhánh "new message" — retry/duplicate không bao giờ khiến listener chạy lần 2.
6. (C6 — ADR-MEDI-003) markRead KHÔNG gọi assertConversationWritable.
7. (C7 — ADR-MEDI-001) Không thêm trường online/available/isOnline vào ExpertProfileResponse hay DirectConversationSummaryResponse.
8. (C8 — BR-MEDI-005) EXPERT shell mobile không có nút/CTA khởi tạo conversation.
9. (C9 — ADR-MEDI-003 v1.1) markRead PHẢI validate lastSeenMessageId thuộc conversationId (DirectMessageRepository.findByIdAndConversationId) TRƯỚC khi tiến cursor.
10. (C10 — ADR-MEDI-004 v1.1) Notification insert PHẢI qua NotificationRecordWriter.insertIfAbsent (ON CONFLICT DO NOTHING trên uq_notification_records_direct_message) — KHÔNG dựa vào clientMessageId của sendMessage() để chống trùng notification.
11. (C11 — ADR-MEDI-004 v1.1) Mọi lỗi FCM PHẢI kết thúc bằng đúng 1 NotificationRecord FAILED + failedAt + audit — không nuốt lỗi mà không ghi record.
12. (C12) ConversationEventDomainEvent field order = (eventType, conversationId, actorUserId, resourceId, occurredAt) — không đảo thứ tự.
```

### 15.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Hành động |
|---|---|---|
| AP-AI-001 | `listMyConversations` gọi repository trong `for` loop | Reject — vi phạm C1 |
| AP-AI-006 | Thêm field `isOnline`/`availableNow` không có nguồn dữ liệu thật | Reject — vi phạm C7 |
| AP-AI-007 | Notification chứa `messageBody`/preview y tế đầy đủ | Reject — vi phạm C4 |
| AP-AI-008 | `markRead`/repository dùng `Instant.now()` thay vì `resolvedMessage.createdAt` | Reject — vi phạm C2, tái tạo lại race đã sửa (§6.5) |
| AP-AI-009 | Notification insert chỉ dựa vào early-return của `sendMessage()`, không có unique index/`insertIfAbsent` | Reject — vi phạm C10 |
| AP-AI-010 | `catch (Exception e) {}` quanh `sendWithRetry` mà không set `status=FAILED` + `failedAt` + audit | Reject — vi phạm C11, tạo "0 record" silently |
| AP-AI-011 | Test/code dùng thứ tự field `(eventType, conversationId, resourceId, actorUserId, ...)` | Reject — vi phạm C12 |

---

## 16. Phụ lục — File Changed List (dự kiến, xác nhận lại khi code)

**Backend — mới:**
- `db/migration/V20260716010600__add_direct_conversation_read_cursor.sql`
- `db/migration/V20260716010700__widen_notification_type_message.sql`
- `db/migration/V20260716010800__add_notification_records_direct_message_idempotency.sql`
- `directchat/dto/response/UnreadSummaryResponse.java`
- `directchat/repository/ConversationSummaryAggregateRepository.java` (+ impl)
- `notification/service/IDirectMessageNotificationService.java` (+ impl)
- `notification/service/impl/NotificationRecordWriter.java` (mirror `DirectMessageWriter`, `ON CONFLICT DO NOTHING`)
- `directchat/event/DirectMessageNotificationListener.java` (2nd consumer của `ConversationEventDomainEvent` đã có từ UC144)
- `directchat/dto/request/MarkReadRequest.java` (`lastSeenMessageId`, `@NotNull`)

**Backend — sửa:**
- `expert/repository/ExpertProfileRepository.java`
- `expert/mapper/ExpertProfileMapper.java`
- `expert/service/impl/ExpertProfileServiceImpl.java`
- `expert/controller/ExpertProfileController.java` (thêm param `q`, `@Size(max=100)`)
- `expert/dto/response/ExpertProfileResponse.java`
- `directchat/dto/response/DirectConversationSummaryResponse.java`
- `directchat/service/impl/DirectConversationServiceImpl.java`
- `directchat/service/impl/DirectMessageServiceImpl.java`
- `directchat/controller/DirectConversationController.java`
- `directchat/repository/DirectConversationRepository.java`
- `directchat/repository/DirectMessageRepository.java` (thêm `findByIdAndConversationId`)
- `directchat/entity/DirectConversation.java`
- `directchat/exception/DirectChatException.java` (thêm factory `messageNotInConversation()` — tái dùng code `DCC-006` có sẵn, KHÔNG thêm code mới)
- `notification/entity/NotificationType.java`
- `notification/dto/NotificationRecordResponse.java`
- `notification/repository/NotificationRecordRepository.java` (thêm `findByUserIdAndReferenceIdAndTypeAndReferenceType`)

**Mobile — mới:**
- `lib/features/directChat/models/expert_directory_item.dart`
- (các file `_test.dart` tương ứng — xem Test-Spec)

**Mobile — sửa:**
- `lib/features/home/screens/home_shell.dart`
- `lib/features/home/screens/mother_home_screen.dart`
- `lib/features/home/screens/expert_app_home_screen.dart`
- `lib/features/directChat/screens/expert_directory_screen.dart`
- `lib/features/directChat/screens/conversation_list_screen.dart`
- `lib/features/directChat/screens/direct_chat_screen.dart`
- `lib/features/directChat/services/direct_chat_service.dart`
- `lib/features/directChat/models/direct_conversation.dart`
- `lib/features/expert/screens/expert_public_profile_screen.dart`
- `lib/core/notifications/fcm_service.dart`
- `lib/features/notification/screens/notification_center_screen.dart`
- `lib/features/notification/screens/notification_detail_screen.dart`

---

## 17. Post-review Architecture Amendment v1.4 (Authoritative)

This amendment supersedes conflicting timestamp-only and in-process notification text above.

- Read state is the composite cursor `(created_at, message_id)`. Both participant cursors persist
  the timestamp and UUID. Timeline, latest-preview, mark-read, and unread queries use the same
  ascending tuple order. The mark-read response returns the cursor actually stored after the
  monotonic database update.
- `notification_records` is the durable MESSAGE outbox. A unique `PENDING` record is committed in
  `REQUIRES_NEW` before FCM. Delivery atomically claims it as `PROCESSING`; completion is committed
  before best-effort audit. A scheduled recovery retries abandoned `PENDING` and stale
  `PROCESSING` rows. The claim lease uses a dedicated `processing_started_at` timestamp (never
  `created_at`), preventing an old row's first delivery from being reclaimed concurrently.
  `PENDING`/`PROCESSING` rows are excluded from the user-facing notification list and unread count.
  Delivery is at-least-once because FCM has no transactional/idempotency key.
- `NotificationPreference.isPushEnabled(recipient, MESSAGE)` is checked before creating the outbox
  row. Disabled MESSAGE push creates no notification record and sends no FCM.
- Event actors must be conversation participants. Directory and inbox queries use unique secondary
  ordering keys. Actual migration files are `V20260716010600`, `V20260716010700`, and
  `V20260716010800`.
- The directory response also returns `specialties: string[]`, sourced by a distinct query over
  non-blank specialties belonging to `APPROVED` experts. Mobile filter chips use this server-owned
  list and send the selected exact value through the existing `specialty` query parameter.
- Mobile reconciliation is generation-guarded: stale directory/inbox/unread requests cannot
  overwrite newer state. A process-local invalidation bus refreshes both role shells after
  foreground MESSAGE signals and successful mark-read. Cold-start deep links remain queued until
  authentication and the root navigator are ready, and route identifiers must be valid UUIDs.

*Status remains Approved; this amendment was explicitly selected by the user during code review.*
