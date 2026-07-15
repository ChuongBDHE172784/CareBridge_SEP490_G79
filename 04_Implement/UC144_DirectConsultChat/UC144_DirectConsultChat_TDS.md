# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# UC144 (Redesign) — Direct Consult Chat & Call — Technical Design Specification

| Field | Value |
|-------|-------|
| **Document ID** | `CB-CHAT-IMP-144D` |
| **Version** | `1.2` |
| **Date** | `2026-07-15` |
| **Status** | `Approved` |
| **Document Owner** | `TV4-Lâm` |
| **Author** | `AI Agent (Technical Architect)` |
| **Reviewed by** | `User, 2026-07-15` |
| **DPO Sign-off** | `[ ] Pending` *(outstanding — proceeding for dev/test only; direct messages/calls are new PII surfaces and DPO review remains a real gap, not waived)* |
| **Approved by** | `User, 2026-07-15` |
| **Last Review** | `2026-07-15` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-15 | AI Agent — Technical Architect | v1.0 — Tạo tài liệu lần đầu (Draft). Thay thế hoàn toàn kiến trúc UC-144 cũ (session/booking-tied). |
| 2026-07-15 | AI Agent — Technical Architect | v1.1 — **Request Changes (vẫn Draft, chưa Approved).** Áp dụng 9 yêu cầu sửa đổi của user: (1) Firebase path đổi sang recipient inbox `/user-conversation-events/{recipientUserId}/{eventId}` + RTDB Rules; (2) thêm `POST /api/v1/firebase/custom-token` lấy userId từ JWT; (3) cursor timeline dùng khoá 3 phần `(sortTimestamp, kind, resourceId)`, đặc tả rõ SQL predicate cho `before`/`after`; (4) siết DDL (NOT NULL, CHECK enum, CHECK duration, CHECK timestamp ordering); (5) hoàn thiện missed-call timeout với reconciliation job + conditional update chống race; (6) thêm retention/cleanup cho Firebase event; (7) đổi `last_message_at` → `last_activity_at`; (8) sửa rollback plan — không DROP bảng/xoá `flyway_schema_history` sau khi đã áp dụng; (9) bổ sung 12 test case mới (DCC-TC-019..030, bao gồm DCC-TC-030 thêm sau review nội bộ cho `last_activity_at`). Xác nhận lại phạm vi call: **chỉ record + signaling, không có RTC audio/video thật** trong pass này (không đổi so với v1.0). Bổ sung sau review nội bộ: `last_activity_at` phải cập nhật từ cả call transitions (BR-DCC-014, không chỉ message); Firebase publish dùng `@Async` thật để đúng nghĩa "non-blocking". |
| 2026-07-15 | AI Agent — Technical Architect | v1.2 — **Request Changes #2 (vẫn Draft, chưa Approved — user xác nhận sẽ Approve sau khi áp dụng 2 thay đổi cuối này).** (1) **Thiết kế lại hành vi khi Expert mất APPROVED** (ADR-DCC-007, BR-DCC-015): thay vì "vẫn lưu message, chỉ bỏ qua publish Firebase" (BR-DCC-010 bản cũ), giờ chặn **toàn bộ** message/call mới từ **cả 2 phía trước khi persist** — không lưu, không publish; Mother vẫn đọc được lịch sử cũ (đọc không đổi); duy nhất `PATCH /end` trên call đang `ANSWERED` được miễn trừ để đóng cuộc gọi sạch sẽ; trả lỗi nghiệp vụ có tài liệu `DCC-010`; response conversation có field `expertAvailable` để client hiển thị trạng thái read-only "Expert is no longer available". `DCC-TC-029` viết lại hoàn toàn theo hành vi mới; không còn giả định "message saved but event skipped". (2) **Giới hạn `message_type` chỉ còn `TEXT`** trong pass này — bỏ `FILE`/`SYSTEM` khỏi CHECK constraint/entity/API vì bảo mật attachment, lưu trữ và vòng đời file chưa được đặc tả; ghi rõ đây là placeholder mở rộng sau (BR-DCC-016). Sửa khoảng tham chiếu tài liệu: CHANGELOG v1.1 và §13–15 nay phản ánh đúng `DCC-TC-001..030`. |
| 2026-07-15 | AI Agent — Technical Architect | **v1.2 — APPROVED.** User đã duyệt sau 3 sửa lỗi biên tập trong Test-Spec (v1.1→v1.2 ở Entry Criteria và Rollback §12; "4 kịch bản"→"5 kịch bản" ở `DCC-TC-029`). Status chuyển `Draft` → `Approved`. Phạm vi xác nhận lại: call = record + Firebase signaling only, KHÔNG có ZegoCloud RTC audio/video thật trong pass này (không đổi so với §1.1). Bắt đầu triển khai theo Red → Green → Refactor ngay sau entry này. |
| 2026-07-15 | AI Agent — Amelia (Dev Agent) | **IMPLEMENTED.** 2 migration mới (`create_direct_conversation_schema`, `widen_audit_logs_action_direct_chat`) — không sửa migration cũ. Package `directchat` + mở rộng `integration/firebase` (gateway abstraction cho FirebaseDatabase/FirebaseAuth để unit-test được mà không cần credentials thật). Code ZIM-cho-chat đã gỡ hoàn toàn (mobile: `zego_zim`, `lib/features/consultation/*`; web: `zego-zim-web`, `consultationManagement/*`); `ConsultationSession`/`ConsultationSessionRepository`/`AuditAction`/notification files revert về trạng thái trước pass cũ, xác nhận bằng `ImpactReportServiceImpl` vẫn hoạt động (`countByEndedAtIsNotNull` không đổi). Backend 47/47 unit test GREEN, mobile 45/45, web build/typecheck/eslint sạch. `DCC-TC-020` chạy thật trên Firebase Emulator (không mock). Chi tiết đầy đủ + hạng mục còn RED (integration-only, cần Docker/Postgres thật) ở Test-Spec §5 và báo cáo cuối cùng gửi trong hội thoại. |

---

## 1. Tổng quan Module

| Field | Value |
|-------|-------|
| **Module Name** | `Direct Consult Chat & Call — directchat` |
| **Bounded Context** | `Consultation / Messaging` (mới, tách biệt khỏi `consultation` booking context) |
| **Data Classification** | `Sensitive-PII` (nội dung tin nhắn có thể chứa thông tin sức khỏe do Mother tự nguyện chia sẻ) |
| **Compliance Scope** | `PDPA` |
| **Upstream Dependencies** | `users`, `expert_profiles` (verification_status), Firebase Admin SDK (đã cài, xem §1.1), ZegoCloud Token04 (`integration.zegocloud.IZegoCloudService` — tái sử dụng nguyên trạng từ UC-154) |
| **Downstream Consumers** | Mobile app (Mother, Expert), Web Expert Portal |

### 1.1 Xác nhận phạm vi & năng lực nền tảng đã kiểm chứng (không đoán)

**⚠️ Xác nhận lại phạm vi call (theo yêu cầu user 2026-07-15):** pass này build đầy đủ vòng đời `conversation_calls` (state machine §6.3, PATCH endpoints, server-computed `duration_seconds`, Zego room/token issuance qua `IZegoCloudService` đã có sẵn, missed-call reconciliation job §6.4) và hiển thị timeline (`"Cuộc gọi video/thoại — thời lượng"`). **KHÔNG** tích hợp SDK RTC thực sự (không zego_express_engine/ZegoUIKit, không có camera/mic/audio 2 chiều chạy được, không có màn hình gọi thật). Nút gọi tạo call record + phát tín hiệu Firebase "incoming call" tới inbox người nhận, nhưng người dùng chưa thể thực sự nói chuyện qua app ở pass này. Nếu mục tiêu thực tế là cho phép gọi thật, đây là một hạng mục riêng (SDK mới, quyền camera/mic, UI trong-cuộc-gọi, không thể test tự động — cần thiết bị thật) và cần một TDS/Test-Spec bổ sung, ngoài phạm vi tài liệu này.

**Backend Firebase capability đã xác minh** (đọc trực tiếp code, không giả định):
- `pom.xml` đã có `firebase-admin` (không cần dependency mới).
- `FirebaseConfig.java` hiện chỉ khởi tạo `FirebaseApp` khi `carebridge.fcm.enabled=true`, và **không** set `databaseUrl`. Cần mở rộng: (a) tách điều kiện khởi tạo `FirebaseApp` khỏi cờ `fcm.enabled` — dùng cờ mới `carebridge.firebase.realtime.enabled` (mặc định `false`, độc lập với FCM); (b) thêm `.setDatabaseUrl(...)`.
- `firebase-admin` Java SDK có sẵn `FirebaseDatabase` (ghi RTDB từ server, bypass Rules bằng Admin credentials) và `FirebaseAuth.createCustomToken()` (custom-token bridge) — **không cần dependency mới**.
- Khi `carebridge.firebase.realtime.enabled=false`: publisher hoạt động như logging stub — đúng pattern `FirebaseFcmServiceImpl`. Backend build/test được mà không cần Firebase project thật.

**Mobile Firebase capability đã xác minh:** `firebase_core: ^3.8.0`, `firebase_messaging: ^15.1.6`, `google-services.json` đã có. `firebase_database`/`firebase_auth` chưa có — cần thêm (đã duyệt).

**Web Firebase capability đã xác minh:** `package.json` không có package Firebase nào — cần thêm `firebase` (Web SDK) từ đầu + Web app config từ user.

**Config còn thiếu, cần user cung cấp trước khi chạy với Firebase project thật:**
1. `FIREBASE_DATABASE_URL` (backend `.env`)
2. Firebase Web app config 6 field (`apiKey`, `authDomain`, `projectId`, `storageBucket`, `messagingSenderId`, `appId`)
3. Xác nhận Realtime Database (không phải Firestore) đã bật trên Firebase project hiện tại

Không có các giá trị trên, code/test vẫn hoàn thiện bằng stub (`carebridge.firebase.realtime.enabled=false`) + Firebase Emulator Suite (RTDB + Auth emulator) cho adapter/Rules test — bắt buộc cho §1.1 Rules test (mục 9 dưới).

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại | Mô tả yêu cầu | Thành phần Code | Compliance Target | ADR liên quan |
|---|---|---|---|---|---|
| BR-DCC-001 | Business Rule | Chat trực tiếp không phụ thuộc booking/payment/session | `DirectConversation` entity, không FK tới `consultation_*` | — | ADR-DCC-001 |
| BR-DCC-002 | Business Rule | Mỗi cặp (mother, expert) chỉ có đúng 1 conversation | `uq_direct_conversations_pair`, `findOrCreate()` | — | ADR-DCC-001 |
| BR-DCC-003 | Business Rule | Expert phải APPROVED — kiểm tra liên tục mỗi lần truy cập | `DirectConversationPolicy.assertExpertVerified` + `assertIsParticipant` | — | ADR-DCC-001 |
| BR-DCC-004 | Business Rule | Lịch sử tin nhắn bền vững trong PostgreSQL | `DirectMessageRepository` cursor pagination | PDPA lưu trữ | ADR-DCC-001 |
| BR-DCC-005 | Business Rule | Retry cùng clientMessageId không tạo duplicate | `uq_direct_messages_client_id` (NOT NULL, unique thật) | — | ADR-DCC-003 |
| BR-DCC-006 | Business Rule | Firebase chỉ mang tín hiệu tối thiểu, không chứa nội dung/dữ liệu sức khỏe | `ConversationEventPayload` (5 field cố định) | PDPA (data minimization) | ADR-DCC-002 |
| BR-DCC-007 | Business Rule | Firebase publish sau commit, lỗi Firebase không rollback/mất message | `@TransactionalEventListener(AFTER_COMMIT)` | — | ADR-DCC-002 |
| BR-DCC-008 | Business Rule | duration call tính ở server, không tin client | `ConversationCallServiceImpl.end()` | — | ADR-DCC-003 |
| BR-DCC-009 | Business Rule | Chỉ Mother thuộc conversation và Expert tương ứng truy cập được | `DirectConversationPolicy.assertIsParticipant` | RBAC | ADR-DCC-001 |
| BR-DCC-010 | Business Rule | Firebase event chỉ ghi vào inbox của **counterpart** (người không phải actor) | `ConversationEventPublisherImpl.resolveRecipient()` | PDPA, RBAC | ADR-DCC-004 |
| BR-DCC-011 | Business Rule | MISSED chỉ được set bởi reconciliation job phía server; không có endpoint client nào set MISSED trực tiếp | `CallTimeoutReconciliationJob` + conditional `UPDATE ... WHERE call_status='RINGING'` | — | ADR-DCC-005 |
| BR-DCC-012 | Business Rule | Firebase RTDB event có TTL, bị dọn định kỳ; PostgreSQL là lưu trữ vĩnh viễn duy nhất | `FirebaseEventRetentionJob` | Data minimization | ADR-DCC-006 |
| BR-DCC-013 | Business Rule | Firebase custom token luôn ứng với `userId` lấy từ JWT hiện tại, không bao giờ nhận từ request body/param | `FirebaseTokenController.POST /firebase/custom-token` | Security (broken access control) | ADR-DCC-004 |
| BR-DCC-014 | Business Rule | `last_activity_at` cập nhật khi có message MỚI **hoặc** call MỚI/đổi trạng thái — không chỉ message | `DirectConversationRepository.touchActivity()`, gọi từ cả `DirectMessageServiceImpl` và `ConversationCallServiceImpl` | — | ADR-DCC-001 |
| BR-DCC-015 | Business Rule | Khi Expert của conversation không còn APPROVED: chặn **mọi** message/call mới từ **cả 2** phía trước khi persist (không lưu, không publish); Mother vẫn đọc được lịch sử cũ; ngoại lệ duy nhất: `PATCH /end` trên call đang `ANSWERED` vẫn cho phép để đóng cuộc gọi sạch sẽ | `DirectConversationPolicy.assertConversationWritable()` | RBAC, Data integrity | ADR-DCC-007 |
| BR-DCC-016 | Business Rule | Chỉ hỗ trợ `message_type = TEXT` trong pass này — không FILE/SYSTEM (bảo mật attachment/lifecycle chưa được đặc tả) | `chk_direct_messages_type`, `MessageType` enum | — | ADR-DCC-001 |
| ADR-DCC-001 | Decision | Direct conversation model, decoupled từ booking | `directchat` package | — | — |
| ADR-DCC-002 | Decision | Firebase RTDB làm transport, REST là nguồn sự thật | `integration.firebase.*` | — | Superseded (path) by ADR-DCC-004 |
| ADR-DCC-003 | Decision | ZegoCloud chỉ dùng cho call; call record độc lập DB | `conversation_calls` | — | Supersedes ADR-ZEGO-001 (UC-154) |
| ADR-DCC-004 | Decision | Recipient-scoped inbox path + custom-token bridge + RTDB Rules | `integration.firebase.*` | — | Amends ADR-DCC-002 |
| ADR-DCC-005 | Decision | Missed-call timeout: reconciliation job + conditional update | `CallTimeoutReconciliationJob` | — | Extends ADR-DCC-003 |
| ADR-DCC-006 | Decision | Firebase event retention/cleanup | `FirebaseEventRetentionJob` | — | Extends ADR-DCC-004 |
| ADR-DCC-007 | Decision | Expert de-verification blocks new writes (not Mother's reads); pre-persistence check; `/end`-on-`ANSWERED` exemption | `DirectConversationPolicy.assertConversationWritable()` | — | Supersedes BR-DCC-010's old "skip publish" sub-clause |

---

## 3. Architecture Decision Records (ADR)

### ADR-DCC-001 — Direct conversation model, tách khỏi booking/session

*(không đổi so với v1.0 — xem lịch sử trong bản trước; giữ nguyên: bảng mới hoàn toàn độc lập, không FK tới `consultation_*`.)*

---

### ADR-DCC-002 — Firebase Realtime Database cho signal, không dùng ZegoCloud ZIM cho chat

| Field | Value |
|-------|-------|
| **Status** | `Superseded by ADR-DCC-004` (chỉ phần *path*/*routing* — phần "publish sau commit / best-effort / payload tối giản" vẫn giữ nguyên, không đổi) |
| **Date** | `2026-07-15` |

Quyết định gốc: publish tín hiệu tối thiểu lên Firebase **sau khi PostgreSQL commit thành công**, payload chỉ `{eventId, eventType, conversationId, resourceId, occurredAt}`, lỗi publish không rollback/không throw ra request gốc, client luôn fetch lại qua REST. **Phần path/routing** (`/conversation-events/{conversationId}/...`, mọi participant đọc chung 1 node) được thay thế bởi ADR-DCC-004 bên dưới theo yêu cầu bảo mật của user — xem đó để biết thiết kế hiện hành.

---

### ADR-DCC-003 — ZegoCloud giữ lại nguyên trạng cho call; call record độc lập với session

*(không đổi so với v1.0 — `IZegoCloudService.generateToken(roomId, userId, userName)` tái dùng nguyên trạng, `roomId` = `conversation_calls.zego_room_id`.)*

---

### ADR-DCC-004 — Recipient-scoped Firebase inbox + custom-token auth bridge + RTDB Rules

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `User, 2026-07-15` |
| **Date** | `2026-07-15` |
| **Supersedes** | ADR-DCC-002 (path/routing only) |

#### Bối cảnh
Thiết kế v1.0 dùng 1 node Firebase dùng chung cho cả conversation (`/conversation-events/{conversationId}/...`), đọc được bởi cả 2 participant. User chỉ ra đây là bề mặt rò rỉ không cần thiết: sender không cần nhận lại tín hiệu về chính message mình vừa gửi (đã có response 201 rồi), và mô hình "mọi participant đọc chung 1 node" không thể áp Firebase Rules chi tiết theo từng người dùng — chỉ có thể mở cho "ai là participant của conversationId" (phải duy trì danh sách participant trong Rules hoặc dùng Firebase custom claims phức tạp). Mô hình **inbox theo user** (`/user-conversation-events/{recipientUserId}/{eventId}`) cho phép Rule đơn giản, chỉ dựa vào `auth.uid`.

#### Quyết định
1. **Path:** `/user-conversation-events/{recipientUserId}/{eventId}`. `recipientUserId` = participant còn lại không phải actor gây ra event (`resolveRecipient(conversation, actorUserId)` — nếu `actorUserId == motherUserId` thì recipient = `expertUserId`, ngược lại recipient = `motherUserId`). Sender/actor **không bao giờ** nhận event của chính hành động mình vừa thực hiện (BR-DCC-010).
2. **Không ghi tới Expert đã mất APPROVED:** trước khi publish, nếu recipient là Expert, `ConversationEventPublisherImpl` kiểm `verification_status` hiện tại; nếu không còn `APPROVED` thì **bỏ qua publish** (không ghi, không lỗi) — message/call vẫn lưu Postgres bình thường, chỉ không đẩy tín hiệu realtime cho Expert đã bị thu hồi (phòng thủ song song với BR-DCC-003's request-time re-check).
3. **Firebase Auth custom-token bridge:** `POST /api/v1/firebase/custom-token` (JWT Bearer bắt buộc) → backend gọi `FirebaseAuth.getInstance(app).createCustomToken(careBridgeUserId.toString())` — `careBridgeUserId` lấy từ `SecurityUtils.requireCurrentUserId()`, **không bao giờ** từ request body/param (không có input nào khác ngoài JWT — endpoint không có `@RequestBody`). Client dùng custom token này để `signInWithCustomToken` vào Firebase, khiến `auth.uid` trong RTDB Rules == CareBridge `user_id` (dạng string UUID).
4. **RTDB Rules** (`database.rules.json`, deploy thủ công qua Firebase CLI — không tự động từ backend):
   ```json
   {
     "rules": {
       "user-conversation-events": {
         "$uid": {
           ".read": "auth != null && auth.uid === $uid",
           ".write": false
         }
       }
     }
   }
   ```
   Client **không bao giờ** ghi trực tiếp vào RTDB (`.write: false` tuyệt đối) — chỉ Admin SDK backend (dùng service-account credentials, bypass Rules) mới ghi được. Rules test bắt buộc chạy qua Firebase Emulator Suite (`@firebase/rules-unit-testing` phía test, hoặc REST Emulator API phía backend nếu cần) — không thể verify Rules bằng unit test Java thông thường.

#### Hệ quả
**Tích cực:** Rule đơn giản, không rò rỉ dữ liệu chéo user; sender không nhận noise về hành động của chính mình; Expert bị revoke ngừng nhận tín hiệu mới ngay cả khi vẫn còn network connection cũ.
**Tiêu cực / Trade-offs:** Với sự kiện cần cả 2 phía biết (vd `CALL_STATE_CHANGED` do 1 bên PATCH), chỉ phía còn lại nhận — điều này đúng ý muốn (actor luôn tự biết qua response của chính request họ gọi).
**Compliance Impact:** Custom-token bridge là điểm truy cập nhạy cảm — endpoint không nhận input ngoài JWT, giảm tối đa bề mặt tấn công (không thể yêu cầu token cho user khác vì không có tham số nào để chỉ định user khác).

---

### ADR-DCC-005 — Missed-call timeout: reconciliation job + conditional update

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `User, 2026-07-15` |
| **Date** | `2026-07-15` |

#### Bối cảnh
v1.0 chỉ ghi "RINGING --> MISSED : timeout" mà không đặc tả cơ chế. User yêu cầu: timeout cụ thể, scheduler/reconciliation job, conditional update chống race với `PATCH /answer` xảy ra gần như đồng thời, và cấm client tự đánh dấu MISSED.

#### Quyết định
- **Timeout:** `carebridge.directchat.call-ring-timeout-seconds` (mặc định `45`) — cấu hình được, không hardcode.
- **Reconciliation job:** `CallTimeoutReconciliationJob` (`@Scheduled(fixedDelay = 10_000)`, mỗi 10s) quét:
  ```sql
  SELECT call_id FROM conversation_calls
  WHERE call_status = 'RINGING' AND initiated_at < now() - make_interval(secs => :timeoutSeconds)
  ```
  với mỗi `call_id`, thực hiện **conditional UPDATE** (không phải load-entity-rồi-save — tránh lost-update):
  ```sql
  UPDATE conversation_calls SET call_status = 'MISSED'
  WHERE call_id = :id AND call_status = 'RINGING'
  ```
  Chỉ audit `DIRECT_CALL_MISSED_BY_TIMEOUT` + publish Firebase event nếu `UPDATE` thực sự ảnh hưởng 1 dòng (`rowsAffected == 1`) — nếu 0 dòng nghĩa là request khác (vd `PATCH /answer`) đã thắng race trước đó, job bỏ qua, không log lỗi.
- **`PATCH /answer` cũng dùng conditional UPDATE đối xứng:**
  ```sql
  UPDATE conversation_calls SET call_status='ANSWERED', answered_at=now()
  WHERE call_id=:id AND call_status='RINGING'
  ```
  Nếu `rowsAffected == 0` (job đã kịp chuyển MISSED trước) → trả `409 DCC-007`, không có ngoại lệ nào được nuốt âm thầm.
- **Không có endpoint `PATCH .../missed`** — MISSED chỉ đến từ job này (BR-DCC-011). `PATCH /end` gọi trong lúc RINGING vẫn map sang `CANCELLED` (caller tự huỷ), không phải MISSED.

#### Hệ quả
**Tích cực:** Loại bỏ hoàn toàn khả năng client tự ý đánh dấu cuộc gọi nhỡ (không thể giả mạo); race giữa answer và timeout luôn có đúng 1 người thắng, xác định bằng affected-row-count của Postgres, không cần lock ứng dụng.
**Tiêu cực / Trade-offs:** Độ trễ phát hiện MISSED tối đa = chu kỳ job (10s) sau khi hết timeout — chấp nhận được cho signal-only feature.

---

### ADR-DCC-006 — Firebase RTDB event retention/cleanup

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `User, 2026-07-15` |
| **Date** | `2026-07-15` |

#### Bối cảnh
Firebase RTDB không phải nơi lưu trữ lâu dài — PostgreSQL mới là system of record (ADR-DCC-002). Event node dưới `/user-conversation-events/{uid}/{eventId}` cần được dọn định kỳ, không được tích luỹ vô hạn, và không được coi là nguồn "lịch sử" (client luôn phải fetch lại từ REST, không bao giờ dùng RTDB làm cache lâu dài).

#### Quyết định
`FirebaseEventRetentionJob` (`@Scheduled(cron = "0 0 * * * *")`, mỗi giờ) xoá mọi node con dưới `/user-conversation-events/{uid}` có `occurredAt` cũ hơn `carebridge.directchat.firebase-event-retention-hours` (mặc định `24`), dùng Admin SDK (`FirebaseDatabase.getReference(...).removeValueAsync()`), quét theo `orderByChild("occurredAt")` + `endAt(cutoffEpochMillis)`. Việc xoá **không ảnh hưởng** dữ liệu PostgreSQL — chỉ dọn transport layer. Client không có trách nhiệm tự xoá event đã đọc (server-side cleanup là đảm bảo duy nhất, không phụ thuộc client có ack hay không).

#### Hệ quả
**Tích cực:** RTDB storage bị chặn trên (bounded), không phình vô hạn; rõ ràng về vai trò "transport-only, không phải storage" của Firebase.
**Tiêu cực / Trade-offs:** Job chạy mỗi giờ — nếu client offline > 24h, một số signal cũ có thể bị dọn trước khi client kịp đọc; **không sao** vì client luôn reconcile qua REST `GET /timeline?after=cursor` khi reconnect (không phụ thuộc RTDB còn event hay không).

---

### ADR-DCC-007 — Expert de-verification blocks new writes (not Mother's reads); pre-persistence check; `/end`-on-`ANSWERED` exemption

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `User, 2026-07-15` |
| **Date** | `2026-07-15` |
| **Supersedes** | BR-DCC-010's cũ sub-clause "skip publish nếu recipient Expert unverified" (nay vô nghĩa — không có gì để persist/publish nữa) |

#### Bối cảnh
Thiết kế v1.1 (BR-DCC-010) vẫn cho **lưu** message/call khi Expert đã mất APPROVED, chỉ bỏ qua bước publish Firebase cho riêng Expert. User chỉ ra đây chưa đủ chặt: một Mother vẫn có thể tiếp tục gửi tin nhắn/gọi vào một conversation với Expert đã ngừng hoạt động, tạo dữ liệu "treo" (message không ai đọc được vì Expert bị chặn đọc theo BR-DCC-003) và không có tín hiệu rõ ràng nào cho UI biết cần chuyển sang chế độ chỉ đọc.

#### Quyết định
1. **Chặn ở tầng ghi, trước khi persist — áp dụng cho cả 2 phía:** `DirectConversationPolicy.assertConversationWritable(conversation)` được gọi **đầu tiên**, trước bất kỳ insert/update nào, trong:
   - `DirectMessageServiceImpl.sendMessage()` (Mother hoặc Expert gửi)
   - `ConversationCallServiceImpl.initiateCall()` (Mother hoặc Expert khởi tạo)
   - `ConversationCallServiceImpl.markRinging()/answer()/decline()` (tiếp diễn một call đang chờ)

   Kiểm tra: `expertProfile.verificationStatus == APPROVED` tại conversation này. Nếu không → **không insert/update gì cả**, không publish Firebase (không có gì để publish), trả lỗi nghiệp vụ `409 DCC-010` ("Expert không còn khả dụng").
2. **Ngoại lệ duy nhất — đóng cuộc gọi đang diễn ra:** `ConversationCallServiceImpl.end()` **bỏ qua** `assertConversationWritable()` khi (và chỉ khi) `call.callStatus == ANSWERED` — cho phép 2 bên kết thúc sạch một cuộc gọi đã kết nối, tính đúng `duration_seconds` như bình thường. `end()` gọi trên call ở `INITIATED`/`RINGING` (huỷ trước khi answer) **vẫn bị chặn** bởi `assertConversationWritable()` như mọi write khác — đây không phải "đóng cuộc gọi đang diễn ra", chỉ là huỷ ý định gọi mới.
3. **Đọc không đổi:** `GET /timeline`, `GET /direct-conversations` **không** gọi `assertConversationWritable()` — Mother tiếp tục đọc lịch sử cũ bình thường (theo đúng yêu cầu "Mother may continue reading existing history"); Expert vẫn bị chặn đọc hoàn toàn như BR-DCC-003 đã quy định từ trước (không đổi).
4. **Tín hiệu cho UI:** `DirectConversationResponse`/`DirectConversationSummaryResponse` có thêm field `expertAvailable: boolean` — **tính tại thời điểm response**, không lưu trong DB (không cần job đồng bộ, luôn tươi). Client dùng field này để hiển thị banner "Expert is no longer available" và khoá input gửi tin nhắn/nút gọi — nhưng **server luôn là nguồn chặn thật sự** (`DCC-010`), UI read-only chỉ là UX, không phải security boundary.

#### Hệ quả
**Tích cực:** Không còn dữ liệu "treo" — mọi message/call trong DB đều có khả năng được cả 2 bên nhìn thấy tại thời điểm tạo; UI có tín hiệu rõ ràng để chuyển read-only mà không cần đoán từ lỗi 403 của Expert.
**Tiêu cực / Trade-offs:** Mother mất khả năng gửi tin nhắn "chờ Expert quay lại" — chấp nhận được, đúng yêu cầu; nếu Expert được APPROVED lại sau này, `assertConversationWritable()` tự động cho phép ghi lại (không cần thao tác thủ công nào khác, vì check luôn tính động).
**Compliance Impact:** Giảm rủi ro Mother chia sẻ thêm dữ liệu sức khỏe vào một kênh không còn ai (đủ tư cách) xử lý.

---

## 4. Non-Functional Requirements & SLA (rút gọn)

| Category | Requirement | Target | Verification |
|---|---|---|---|
| Data Integrity | Zero message loss kể cả khi Firebase lỗi | RPO = 0 cho `direct_messages` | DCC-TC-010 |
| Idempotency | Retry cùng `clientMessageId` không tạo dòng mới | 100% | DCC-TC-009 |
| Security | Firebase event không chứa PII, chỉ recipient đọc được | 0 field ngoài whitelist; Rules deny cross-user read | DCC-TC-011, DCC-TC-020 |
| Access control | Participant-only, Expert phải APPROVED liên tục | Least privilege | §16, DCC-TC-018 |
| Concurrency | answer-vs-timeout race luôn có đúng 1 kết quả | 100% xác định | DCC-TC-028 |

---

## 5. Static Modeling

### 5.1 Entities (tóm tắt)

```
DirectConversation (conversation_id PK, mother_user_id FK users, expert_user_id FK users,
                     status, created_at, last_activity_at)
DirectMessage       (message_id PK, conversation_id FK, sender_user_id FK users,
                     client_message_id NOT NULL, message_type ('TEXT' only — BR-DCC-016), message_body NOT NULL, created_at)
ConversationCall     (call_id PK, conversation_id FK, initiated_by_user_id FK users,
                     call_type, call_status, zego_room_id, initiated_at, answered_at,
                     ended_at, duration_seconds, created_at)
```

### 5.2 Data Structure (Flyway SQL Migration — DESIGN, tạo file thật sau khi Approved)

> Theo `implement-flow.md`: không tạo file migration thật ở bước Draft.

```sql
-- === DIRECT CONVERSATION & CALL SCHEMA (v1.1 — siết DDL) ===
-- Độc lập hoàn toàn với consultation_bookings/consultation_sessions/consultation_messages.

CREATE TABLE public.direct_conversations (
    conversation_id  uuid         NOT NULL DEFAULT gen_random_uuid(),
    mother_user_id   uuid         NOT NULL,
    expert_user_id   uuid         NOT NULL,
    status           varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       timestamptz  NOT NULL DEFAULT now(),
    last_activity_at timestamptz,
    CONSTRAINT direct_conversations_pkey PRIMARY KEY (conversation_id),
    CONSTRAINT direct_conversations_mother_user_id_fkey
        FOREIGN KEY (mother_user_id) REFERENCES public.users(user_id),
    CONSTRAINT direct_conversations_expert_user_id_fkey
        FOREIGN KEY (expert_user_id) REFERENCES public.users(user_id),
    CONSTRAINT uq_direct_conversations_pair UNIQUE (mother_user_id, expert_user_id),
    CONSTRAINT chk_direct_conversations_status
        CHECK (status = ANY (ARRAY['ACTIVE']::varchar[])),  -- placeholder — widen qua migration mới khi thêm BLOCKED/ARCHIVED
    CONSTRAINT chk_direct_conversations_activity_after_created
        CHECK (last_activity_at IS NULL OR last_activity_at >= created_at)
);

CREATE TABLE public.direct_messages (
    message_id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    conversation_id      uuid         NOT NULL,
    sender_user_id        uuid         NOT NULL,
    client_message_id    uuid         NOT NULL,           -- siết: bắt buộc mọi message có idempotency key
    message_type          varchar(30)  NOT NULL DEFAULT 'TEXT',
    message_body           text         NOT NULL,
    created_at              timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT direct_messages_pkey PRIMARY KEY (message_id),
    CONSTRAINT direct_messages_conversation_id_fkey
        FOREIGN KEY (conversation_id) REFERENCES public.direct_conversations(conversation_id),
    CONSTRAINT direct_messages_sender_user_id_fkey
        FOREIGN KEY (sender_user_id) REFERENCES public.users(user_id),
    CONSTRAINT uq_direct_messages_client_id
        UNIQUE (conversation_id, sender_user_id, client_message_id),  -- không còn cần partial index vì NOT NULL
    CONSTRAINT chk_direct_messages_type
        CHECK (message_type = ANY (ARRAY['TEXT']::varchar[])),  -- BR-DCC-016: chỉ TEXT trong pass này; widen migration riêng khi thêm FILE/SYSTEM (đặc tả attachment security/lifecycle trước)
    CONSTRAINT chk_direct_messages_body_length
        CHECK (length(btrim(message_body)) > 0 AND length(message_body) <= 2000)
);

CREATE INDEX idx_direct_messages_conversation_id
    ON public.direct_messages (conversation_id, created_at DESC);

CREATE TABLE public.conversation_calls (
    call_id                uuid         NOT NULL DEFAULT gen_random_uuid(),
    conversation_id         uuid         NOT NULL,
    initiated_by_user_id    uuid         NOT NULL,
    call_type               varchar(10)  NOT NULL,
    call_status             varchar(20)  NOT NULL DEFAULT 'INITIATED',
    zego_room_id            varchar(255) NOT NULL,
    initiated_at             timestamptz  NOT NULL DEFAULT now(),
    answered_at               timestamptz,
    ended_at                  timestamptz,
    duration_seconds          integer,
    created_at                 timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT conversation_calls_pkey PRIMARY KEY (call_id),
    CONSTRAINT conversation_calls_conversation_id_fkey
        FOREIGN KEY (conversation_id) REFERENCES public.direct_conversations(conversation_id),
    CONSTRAINT conversation_calls_initiated_by_user_id_fkey
        FOREIGN KEY (initiated_by_user_id) REFERENCES public.users(user_id),
    CONSTRAINT chk_conversation_calls_type
        CHECK (call_type = ANY (ARRAY['VOICE','VIDEO']::varchar[])),
    CONSTRAINT chk_conversation_calls_status
        CHECK (call_status = ANY (ARRAY['INITIATED','RINGING','ANSWERED','DECLINED','MISSED','CANCELLED','ENDED','FAILED']::varchar[])),
    CONSTRAINT chk_conversation_calls_duration_non_negative
        CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    CONSTRAINT chk_conversation_calls_answered_after_initiated
        CHECK (answered_at IS NULL OR answered_at >= initiated_at),
    CONSTRAINT chk_conversation_calls_ended_after_initiated
        CHECK (ended_at IS NULL OR ended_at >= initiated_at),
    CONSTRAINT chk_conversation_calls_ended_requires_answered
        CHECK (call_status <> 'ENDED' OR answered_at IS NOT NULL)  -- ENDED chỉ tới từ ANSWERED, xem §6.3
);

CREATE INDEX idx_conversation_calls_conversation_id
    ON public.conversation_calls (conversation_id, initiated_at DESC);

CREATE INDEX idx_conversation_calls_ringing_timeout
    ON public.conversation_calls (initiated_at) WHERE call_status = 'RINGING';  -- phục vụ CallTimeoutReconciliationJob
```

> **Migration #2 (bắt buộc):** widen `audit_logs_action_check` cho các `AuditAction` mới ở §7.2 — theo đúng mẫu `DROP CONSTRAINT IF EXISTS` + `ADD CONSTRAINT ... ANY(ARRAY[...toàn bộ danh sách cũ + mới...])`. Đây là lỗi thật đã phát hiện ở pass UC-95/144 cũ (thiếu bước này) — không được lặp lại.

---

## 6. Dynamic Modeling

### 6.1 Sequence — Find-or-create conversation

*(không đổi so với v1.0, chỉ đổi `last_message_at` → `last_activity_at` trong mọi tham chiếu.)*

### 6.1.1 `last_activity_at` — cập nhật cho cả message và call (BR-DCC-014)

`DirectConversationRepository.touchActivity(conversationId, timestamp)` được gọi từ **cả hai** service, không chỉ khi gửi message:
- `DirectMessageServiceImpl.sendMessage()` — sau insert thành công (đã có ở §6.2).
- `ConversationCallServiceImpl` — sau **mỗi** transition thành công của `conversation_calls` (initiate, ringing, answer, decline, end) **và** sau khi `CallTimeoutReconciliationJob` chuyển một call sang MISSED (job cũng phải touch conversation của call đó). Dùng `timestamp` = thời điểm transition thực tế (không phải `now()` gọi lại lần 2 — tránh lệch nano-giây giữa 2 lời gọi `now()` trong cùng transaction).
- Không touch khi request bị policy/validation từ chối (403/404/409) — chỉ khi có thay đổi trạng thái thực sự được ghi nhận.

### 6.2 Sequence — Send message + Firebase signal (recipient inbox) + reconnect reconcile

```
Sender -> Controller: POST /direct-conversations/{id}/messages {clientMessageId, body}
Controller -> Policy: assertIsParticipant(currentUserId, conversation)  [re-check APPROVED nếu actor=Expert]
Controller -> Service: sendMessage(...)
Service -> MessageRepo: findByConversationAndSenderAndClientId(...)
alt already exists (retry)
    Service --> Controller: 200 + existing (created=false)
else new
    Service -> MessageWriter: insertIsolated(message)  [REQUIRES_NEW, unique constraint guards race]
    Service -> ConversationRepo: touch(lastActivityAt = now())
    Service -> Audit: emit(DIRECT_MESSAGE_SENT)  [metadata only]
    Service --> Controller: 201 + new (created=true)
end
Controller --> Sender: response
== ngay sau commit — listener method có @Async (xem §8), chạy trên thread pool riêng, KHÔNG kéo dài thời gian phản hồi HTTP ==
Service -> FirebaseEventPublisher: publishAfterCommit(MESSAGE_SENT, conversationId, messageId, actorUserId=senderUserId)
FirebaseEventPublisher -> FirebaseEventPublisher: recipientUserId = resolveRecipient(conversation, actorUserId)
alt recipient is Expert AND verification_status != APPROVED
    FirebaseEventPublisher -> FirebaseEventPublisher: SKIP publish (BR-DCC-010) — log, không lỗi
else
    FirebaseEventPublisher -> Firebase RTDB: SET /user-conversation-events/{recipientUserId}/{eventId}
end
note right: sender KHÔNG BAO GIỜ nhận event của chính message mình vừa gửi (BR-DCC-010)
Recipient (đã signInWithCustomToken, auth.uid=recipientUserId) -> Firebase RTDB: onChildAdded tại node của chính mình
Recipient -> Controller: GET /direct-conversations/{id}/timeline?after=cursor
Controller --> Recipient: real message from PostgreSQL (never trust Firebase payload)
```

### 6.3 State Machine — conversation_calls.call_status

```
[*] --> INITIATED : POST /calls (caller tạo room + token)
INITIATED --> RINGING : callee client ack đã nhận tín hiệu (PATCH /ringing, actor=callee)
INITIATED --> CANCELLED : PATCH /end (actor=caller, trước khi callee thấy)
INITIATED --> FAILED : Zego token generation lỗi
RINGING --> ANSWERED : PATCH /answer (actor=callee; conditional UPDATE WHERE call_status='RINGING', xem ADR-DCC-005)
RINGING --> DECLINED : PATCH /decline (actor=callee)
RINGING --> MISSED : CHỈ bởi CallTimeoutReconciliationJob (ADR-DCC-005) — không có endpoint client nào set trạng thái này
RINGING --> CANCELLED : PATCH /end (actor=caller, huỷ khi vẫn đang đổ chuông)
ANSWERED --> ENDED : PATCH /end (actor=caller HOẶC callee; endedAt=now(); durationSeconds=endedAt-answeredAt, server tính)
note right of ENDED
  Invariant: durationSeconds CHỈ được set khi call đã ANSWERED (chk_conversation_calls_ended_requires_answered).
  Client KHÔNG được gửi durationSeconds — EndCallRequest DTO không có field này.
end note
```

**Authorization theo transition (BR mới — xem DCC-TC-027):**
| Transition | Actor hợp lệ |
|---|---|
| `POST /calls` (INITIATED) | caller (bất kỳ participant nào khởi tạo) |
| `PATCH /ringing` | callee only |
| `PATCH /answer` | callee only |
| `PATCH /decline` | callee only |
| `PATCH /end` (từ INITIATED/RINGING) | caller only (huỷ cuộc gọi mình khởi tạo) |
| `PATCH /end` (từ ANSWERED) | caller hoặc callee (cả 2 đều có thể kết thúc cuộc gọi đang diễn ra) |

**Invariant bất biến:**
- Không trạng thái nào cho phép quay lại `INITIATED`/`RINGING` sau khi đã vào trạng thái kết thúc (append-only transition).
- `duration_seconds` luôn = `endedAt - answeredAt` tính tại server; `NULL` nếu chưa từng `ANSWERED`.
- MISSED chỉ đến từ reconciliation job (BR-DCC-011) — không có `PATCH .../missed`.

### 6.4 Missed-call reconciliation — xem ADR-DCC-005 cho chi tiết job + conditional update.

---

## 7. Domain Event Catalog

### 7.1 Events Published (Firebase Realtime Database)

| Event Type | Trigger | Path | Payload |
|---|---|---|---|
| `MESSAGE_SENT` | Sau commit `direct_messages` insert | `/user-conversation-events/{recipientUserId}/{eventId}` | `{eventId, eventType, conversationId, resourceId=messageId, occurredAt}` |
| `CALL_INITIATED` | Sau commit `conversation_calls` insert | cùng path (recipient=callee) | `resourceId=callId` |
| `CALL_STATE_CHANGED` | Sau mỗi PATCH thành công | cùng path (recipient=counterpart của actor PATCH) | `resourceId=callId` |

> Không field nào khác ngoài 5 field cố định (BR-DCC-006). `recipientUserId` được resolve theo BR-DCC-010, không bao giờ ghi vào inbox của actor. Retention: xem ADR-DCC-006.

### 7.2 AuditAction mới

`DIRECT_CONVERSATION_OPENED`, `DIRECT_MESSAGE_SENT`, `DIRECT_CHAT_ACCESS_DENIED`, `DIRECT_CALL_INITIATED`, `DIRECT_CALL_STATE_CHANGED`, `DIRECT_CALL_ACCESS_DENIED`, `DIRECT_CALL_MISSED_BY_TIMEOUT`, `FIREBASE_CUSTOM_TOKEN_ISSUED` — audit chỉ ghi metadata, không bao giờ `message_body`.

---

## 8. Interface Specification

```java
public interface IDirectConversationService {
    DirectConversationResponse findOrCreate(UUID motherUserId, UUID expertProfileId);
    List<DirectConversationSummaryResponse> listMyConversations(UUID currentUserId);
    DirectConversationResponse getConversation(UUID conversationId, UUID currentUserId);
}

public interface IDirectMessageService {
    // sendMessage() calls DirectConversationPolicy.assertConversationWritable() BEFORE any persistence (ADR-DCC-007)
    SendDirectMessageResult sendMessage(UUID conversationId, UUID senderUserId, SendDirectMessageRequest request);
    TimelinePageResponse getTimeline(UUID conversationId, UUID currentUserId, String after, String before, int limit);
    // MessageType is TEXT-only this pass (BR-DCC-016) — SendDirectMessageRequest has no messageType
    // field at all (server always persists 'TEXT'); response DTOs still expose messageType for
    // forward-compat with a future FILE/SYSTEM widen.
}

public interface IConversationCallService {
    // initiateCall/markRinging/answer/decline all call assertConversationWritable() first (ADR-DCC-007)
    ConversationCallResponse initiateCall(UUID conversationId, UUID callerUserId, CallType type);
    ConversationCallResponse markRinging(UUID callId, UUID currentUserId);   // callee only
    ConversationCallResponse answer(UUID callId, UUID currentUserId);        // callee only, conditional UPDATE
    ConversationCallResponse decline(UUID callId, UUID currentUserId);      // callee only
    ConversationCallResponse end(UUID callId, UUID currentUserId);
    // end(): skips assertConversationWritable() ONLY when call.callStatus == ANSWERED (ADR-DCC-007 exemption);
    // ending an INITIATED/RINGING call while Expert unverified is still blocked (DCC-010), it is not
    // "closing an in-progress call", it is a new cancel attempt.
    // EndCallRequest DTO: KHÔNG có field durationSeconds — server luôn tự tính
}

// directchat.policy
public interface IDirectConversationPolicy {
    void assertIsParticipant(UUID currentUserId, DirectConversation conversation);  // re-checks Expert APPROVED (BR-DCC-003)
    void assertExpertVerified(ExpertProfile expertProfile);                          // creation-time gate (BR-DCC-003)
    void assertConversationWritable(DirectConversation conversation);
    // throws DCC-010 (409) if conversation.expertUserId's expert_profiles.verification_status != APPROVED.
    // Called by every write path EXCEPT: GET endpoints, and ConversationCallServiceImpl.end() when
    // the call being ended is already ANSWERED (ADR-DCC-007).
}

// integration.firebase
public interface IConversationEventPublisher {
    @Async  // truly non-blocking — request thread returns to client without waiting on RTDB I/O
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void publishAfterCommit(ConversationEventDomainEvent event);
    // implementation resolves recipientUserId = counterpart(actorUserId) — BR-DCC-010.
    // No verification-status check here anymore (ADR-DCC-007 superseded that sub-clause): if the
    // Expert isn't APPROVED, assertConversationWritable() already blocked persistence upstream, so
    // this method is simply never invoked for that attempt — nothing left to skip.
}

public interface IFirebaseAuthBridgeService {
    String createCustomToken(UUID careBridgeUserId);  // uid claim == careBridgeUserId.toString(); no other input
}

// scheduled jobs — no public HTTP surface
class CallTimeoutReconciliationJob {
    @Scheduled(fixedDelay = 10_000)
    void reconcileMissedCalls();  // conditional UPDATE ... WHERE call_status='RINGING', see ADR-DCC-005
}

class FirebaseEventRetentionJob {
    @Scheduled(cron = "0 0 * * * *")
    void purgeExpiredEvents();  // deletes RTDB nodes older than retention window, see ADR-DCC-006
}
```

### 8.1 Controller — Firebase token bridge

```java
// FirebaseTokenController.java
// POST /api/v1/firebase/custom-token — no @RequestBody at all (BR-DCC-013)
@PostMapping("/api/v1/firebase/custom-token")
public FirebaseCustomTokenResponse issueCustomToken(Authentication auth) {
    UUID currentUserId = SecurityUtils.requireCurrentUserId(auth);
    String token = firebaseAuthBridgeService.createCustomToken(currentUserId);
    auditService.emit(AuditAction.FIREBASE_CUSTOM_TOKEN_ISSUED, currentUserId, Map.of());
    return new FirebaseCustomTokenResponse(token);
}
```

### 8.2 Repository Interface

```java
public interface DirectConversationRepository extends JpaRepository<DirectConversation, UUID> {
    Optional<DirectConversation> findByMotherUserIdAndExpertUserId(UUID motherUserId, UUID expertUserId);
    List<DirectConversation> findByMotherUserIdOrExpertUserId(UUID motherUserId, UUID expertUserId);

    @Modifying
    @Query("UPDATE DirectConversation c SET c.lastActivityAt = :timestamp WHERE c.id = :conversationId")
    void touchActivity(UUID conversationId, Instant timestamp);  // called from both message send and every call transition — BR-DCC-014
}

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {
    Optional<DirectMessage> findByConversationIdAndSenderUserIdAndClientMessageId(UUID conversationId, UUID senderUserId, UUID clientMessageId);
}

// Native query bắt buộc (UNION ALL không biểu diễn được bằng JPQL) — xem §9.2 cho SQL đầy đủ
public interface ConversationTimelineRepository {
    List<TimelineRow> fetchAfter(UUID conversationId, Instant afterTs, String afterKind, UUID afterResourceId, int limit);
    List<TimelineRow> fetchBefore(UUID conversationId, Instant beforeTs, String beforeKind, UUID beforeResourceId, int limit);
    List<TimelineRow> fetchLatest(UUID conversationId, int limit);  // no-cursor initial load
}

// conditional-update methods — trả về affected row count, KHÔNG load-entity-rồi-save
public interface ConversationCallRepository extends JpaRepository<ConversationCall, UUID> {
    @Modifying
    @Query("UPDATE ConversationCall c SET c.callStatus='ANSWERED', c.answeredAt=:now WHERE c.id=:id AND c.callStatus='RINGING'")
    int conditionallyAnswer(UUID id, Instant now);

    @Modifying
    @Query("UPDATE ConversationCall c SET c.callStatus='MISSED' WHERE c.id=:id AND c.callStatus='RINGING'")
    int conditionallyMarkMissed(UUID id);
}
```

---

## 9. API Specification

### 9.1 Endpoints Table

| Method | Path | Auth | Roles | Idempotent? |
|---|---|---|---|---|
| `POST` | `/api/v1/direct-conversations/expert/{expertId}` | JWT | `MOTHER` | Yes (find-or-create) |
| `GET` | `/api/v1/direct-conversations` | JWT | `MOTHER`, `EXPERT` | Yes |
| `GET` | `/api/v1/direct-conversations/{conversationId}/timeline` | JWT | Participant only | Yes |
| `POST` | `/api/v1/direct-conversations/{conversationId}/messages` | JWT | Participant only | Yes (clientMessageId) |
| `POST` | `/api/v1/direct-conversations/{conversationId}/calls` | JWT | Participant only | No |
| `PATCH` | `/api/v1/direct-conversations/{conversationId}/calls/{callId}/ringing` | JWT | Callee only | Yes |
| `PATCH` | `/api/v1/direct-conversations/{conversationId}/calls/{callId}/answer` | JWT | Callee only | Yes (conditional) |
| `PATCH` | `/api/v1/direct-conversations/{conversationId}/calls/{callId}/decline` | JWT | Callee only | Yes |
| `PATCH` | `/api/v1/direct-conversations/{conversationId}/calls/{callId}/end` | JWT | Caller (any) / Callee (ANSWERED only) | Yes |
| `POST` | `/api/v1/firebase/custom-token` | JWT | any authenticated user | Yes |

> **`{expertId}`** = `expert_profiles.expert_profile_id`, resolve → `expertProfile.userId` trước khi dùng (xem note v1.0, không đổi).

### 9.2 Unified timeline — cursor 3 phần `(sortTimestamp, kind, resourceId)`

**Nguồn dữ liệu (native SQL, UNION ALL — không dùng merge-trong-bộ-nhớ):**
```sql
SELECT 'MESSAGE' AS kind, message_id AS resource_id, created_at AS sort_ts
FROM direct_messages WHERE conversation_id = :conversationId
UNION ALL
SELECT 'CALL_EVENT' AS kind, call_id AS resource_id, initiated_at AS sort_ts
FROM conversation_calls WHERE conversation_id = :conversationId
```

**`after` (đồng bộ item mới hơn khi reconnect) — ORDER ASC, trả nguyên thứ tự tăng dần:**
```sql
SELECT * FROM (<UNION ALL trên>) t
WHERE (sort_ts, kind, resource_id) > (:afterTs, :afterKind, :afterResourceId)
ORDER BY sort_ts ASC, kind ASC, resource_id ASC
LIMIT :limit
```

**`before` (tải lịch sử cũ hơn khi cuộn lên) — ORDER DESC rồi đảo ngược list trước khi trả về client:**
```sql
SELECT * FROM (<UNION ALL trên>) t
WHERE (sort_ts, kind, resource_id) < (:beforeTs, :beforeKind, :beforeResourceId)
ORDER BY sort_ts DESC, kind DESC, resource_id DESC
LIMIT :limit
-- app code: Collections.reverse(rows) trước khi map sang response (client luôn thấy ASC)
```

**Không cursor (mở lại conversation lần đầu) — coi như `before` từ +∞:**
```sql
SELECT * FROM (<UNION ALL trên>) t
ORDER BY sort_ts DESC, kind DESC, resource_id DESC
LIMIT :limit
-- app code: reverse như trên
```

**Cursor encoding:** base64(`sortTsEpochMillis|kind|resourceId`) — dùng Postgres **row-value comparison** (`(a,b,c) > (x,y,z)`) để so sánh 3 phần trong 1 predicate, tránh bug so sánh từng phần riêng lẻ. `kind` so sánh dạng string (`'CALL_EVENT' > 'MESSAGE'` theo thứ tự bảng chữ cái) — đây là tie-break xác định khi 2 item có `sort_ts` giống hệt nhau (xem DCC-TC-023).

**`POST /direct-conversations/expert/{expertId}` — 200/201:**
```json
{ "conversationId": "uuid", "motherUserId": "uuid", "expertUserId": "uuid",
  "status": "ACTIVE", "createdAt": "2026-07-15T00:00:00Z", "lastActivityAt": null,
  "expertAvailable": true }
```
`expertAvailable` = `expert_profiles.verification_status == APPROVED` **tính tại thời điểm response**, không phải cột lưu trong `direct_conversations` (ADR-DCC-007 §4). Khi `false`, client hiển thị banner "Expert is no longer available" và khoá input gửi tin nhắn/nút gọi — server (`DCC-010`) vẫn là chốt chặn thật, UI chỉ là UX.

**`POST /messages` / `POST|PATCH /calls/*` khi Expert không còn APPROVED — 409:**
```json
{ "error": { "code": "DCC-010", "message": "Expert không còn khả dụng cho cuộc trò chuyện này" } }
```

**`GET /timeline?after=cursor&limit=30`:**
```json
{ "items": [
    { "kind": "MESSAGE", "messageId": "uuid", "senderUserId": "uuid", "messageType": "TEXT",
      "messageBody": "Chào bác sĩ", "createdAt": "2026-07-15T08:00:00Z" },
    { "kind": "CALL_EVENT", "callId": "uuid", "callType": "VOICE", "callStatus": "ENDED",
      "initiatedByUserId": "uuid", "durationSeconds": 184, "initiatedAt": "...", "endedAt": "..." }
  ], "nextCursor": "base64", "hasMoreNewer": false, "previousCursor": "base64", "hasMoreOlder": true }
```

**`POST /firebase/custom-token` — 200 (không có request body):**
```json
{ "firebaseCustomToken": "eyJhbGciOi..." }
```

**`POST /messages` — 409 idempotency conflict:**
```json
{ "error": { "code": "DCC-005", "message": "clientMessageId đã dùng với nội dung khác" } }
```

**`PATCH /answer` — 409 khi race thua trước reconciliation job:**
```json
{ "error": { "code": "DCC-007", "message": "Call không còn ở trạng thái RINGING (có thể đã bị đánh dấu MISSED)" } }
```

---

## 10. Bảng mã lỗi (Error Codes)

| Code | HTTP | Trigger |
|---|---|---|
| `DCC-001` | 403 | Không phải Mother khi gọi find-or-create |
| `DCC-002` | 403/422 | Expert chưa/không còn `APPROVED` (creation: 422; ongoing access re-check: 403) |
| `DCC-003` | 403 | Không phải participant của conversation |
| `DCC-004` | 400 | messageBody rỗng/quá dài (khớp `chk_direct_messages_body_length`) |
| `DCC-005` | 409 | clientMessageId trùng nhưng nội dung khác |
| `DCC-006` | 404 | Conversation/Call không tồn tại |
| `DCC-007` | 409 | Chuyển trạng thái call không hợp lệ hoặc thua race (answer-vs-timeout) |
| `DCC-008` | 503 | ZegoCloud token generation lỗi khi initiate call |
| `DCC-009` | 403 | Actor không đúng vai trò cho transition (vd Mother gọi `/answer` trên call mà Mother là caller) |
| `DCC-010` | 409 | Expert của conversation không còn `APPROVED` — chặn message/call mới (ADR-DCC-007). Không áp dụng cho `PATCH /end` trên call đang `ANSWERED` |

---

## 11. Quy trình Triển khai (chỉ thực hiện sau khi Approved)

1. Migration #1: 3 bảng + toàn bộ CHECK constraint (§5.2). Migration #2: widen `audit_logs_action_check`.
2. Backend: package `directchat` + mở rộng `integration/firebase` (`FirebaseConfig` tách cờ, `ConversationEventPublisherImpl` với `resolveRecipient`, `FirebaseAuthBridgeServiceImpl`, `FirebaseTokenController`, `CallTimeoutReconciliationJob`, `FirebaseEventRetentionJob`).
3. `database.rules.json` + `firebase.json` + `.firebaserc` template (không commit secret) — deploy thủ công qua Firebase CLI, ngoài phạm vi backend deploy pipeline.
4. Xoá code ZIM-cho-chat + revert `ConsultationSession`/`ConsultationSessionRepository`/`AuditAction`/notification files/`DevDataSeeder`.
5. Mobile: `firebase_database`/`firebase_auth`, `directChat` feature module, custom-token sign-in flow.
6. Web: Firebase Web SDK (chờ config), `directChat` feature module.
7. Test theo Test-Spec kèm theo tài liệu này — bao gồm Firebase Emulator Suite cho Rules test.

---

## 12. Rollback Plan

> **Nguyên tắc (theo yêu cầu user):** một khi migration đã được `flyway migrate` thành công ở bất kỳ môi trường chia sẻ nào (staging trở lên), **không bao giờ** `DROP TABLE`/xoá dòng `flyway_schema_history` để "undo" — điều đó phá vỡ tính bất biến lịch sử migration mà Flyway dựa vào, và có thể làm mất dữ liệu đã ghi. Rollback schema sau khi đã áp dụng **chỉ được thực hiện bằng một migration xuôi mới** (vd `V{ts+2}__revert_direct_conversation_schema.sql` chứa `DROP TABLE ... CASCADE` nếu thực sự cần huỷ tính năng) — không sửa/xoá migration cũ.

**Trước khi migration từng được áp dụng ở bất kỳ đâu ngoài máy dev cá nhân** (tức migration file vẫn chỉ tồn tại uncommitted/local): có thể an toàn xoá file và không tạo migration bù trừ — vì Flyway `schema_history` ở môi trường chia sẻ chưa từng ghi nhận nó.

```bash
# Chỉ áp dụng khi migration CHƯA từng chạy ở bất kỳ môi trường chia sẻ nào:
git checkout -- 05_Development/CareBridgeAPI/src/main/resources/db/migration/V{ts}__create_direct_conversation_schema.sql
git checkout -- 05_Development/CareBridgeAPI/src/main/resources/db/migration/V{ts+1}__widen_audit_logs_action_direct_chat.sql
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/firebase/

# Nếu migration ĐÃ áp dụng ở staging/production: KHÔNG dùng lệnh trên cho phần DB.
# Thay vào đó tạo migration xuôi mới:
#   V{ts+2}__revert_direct_conversation_schema.sql
#     DROP TABLE IF EXISTS conversation_calls, direct_messages, direct_conversations CASCADE;
# rồi review/approve như mọi migration khác trước khi chạy.
```

---

## 13–15. Kịch bản kiểm thử / Xác minh / Mẫu API

Xem `UC144_DirectConsultChat_Test-Spec.md` — `DCC-TC-001..030`.

---

## 16. Bảng tổng hợp phân quyền (Authorization Matrix)

| Endpoint | `MOTHER` — Expert APPROVED | `MOTHER` — Expert đã revoke | `EXPERT` (không phải participant) | `EXPERT` (participant, APPROVED) | `EXPERT` (participant, đã revoke) |
|---|---|---|---|---|---|
| `POST /expert/{expertId}` (find-or-create) | ✅ | *(N/A — check ở creation, không phải trên conversation đã tồn tại)* | ❌ | — | — |
| `GET /direct-conversations` | ✅ Own | ✅ Own | ✅ Own | ✅ Own | ✅ Own *(list vẫn hiển thị `expertAvailable`, không chặn list)* |
| `GET /{id}/timeline` | ✅ nếu participant | ✅ nếu participant *(đọc không bị chặn — BR-DCC-015)* | ❌ | ✅ | ❌ `DCC-002` |
| `POST /{id}/messages` | ✅ nếu participant | ❌ `DCC-010` | ❌ | ✅ | ❌ `DCC-002` *(re-check truy cập chặn trước cả DCC-010)* |
| `POST /{id}/calls`, `PATCH .../ringing`, `.../answer`, `.../decline` | ✅ nếu participant | ❌ `DCC-010` | ❌ | ✅ (theo bảng transition §6.3) | ❌ `DCC-002` |
| `PATCH .../end` — call đang `ANSWERED` | ✅ nếu participant | ✅ **được miễn trừ** (ADR-DCC-007 §2) | ❌ | ✅ | ❌ `DCC-002` *(Expert vẫn bị chặn truy cập nói chung, miễn trừ chỉ áp dụng phía có quyền truy cập)* |
| `PATCH .../end` — call `INITIATED`/`RINGING` | ✅ nếu participant | ❌ `DCC-010` *(không phải "đóng call đang chạy")* | ❌ | ✅ | ❌ `DCC-002` |
| `POST /firebase/custom-token` | ✅ | ✅ | ✅ | ✅ | ✅ *(token không tự nó cấp quyền đọc conversation — Rules + REST vẫn chặn)* |

**Chú thích:** "Verified" = `expert_profiles.verification_status = APPROVED`, kiểm tra **liên tục ở mọi lần truy cập** (BR-DCC-003, quyết định 403/422 `DCC-002` cho chính Expert đó). Khi Expert **đã có quyền truy cập** (APPROVED) nhưng **conversation đối tác** không còn APPROVED — trường hợp này không xảy ra vì `DCC-002` đã chặn Expert trước; cột "MOTHER — Expert đã revoke" mới là nơi `DCC-010`/ADR-DCC-007 áp dụng thực tế, vì Mother luôn có quyền truy cập (không phụ thuộc verification), chỉ bị chặn *ghi* khi đối tác Expert mất APPROVED. Firebase custom token issuance không phải là một quyền truy cập dữ liệu — nó chỉ cho phép `signInWithCustomToken`; quyền đọc thực tế do RTDB Rules (`auth.uid === $uid`, ADR-DCC-004) và REST policy quyết định độc lập.

---

## 17. AI Prompt Constraints (CASE 2.0)

| # | Constraint | Source |
|---|---|---|
| C1 | Không FK bảng mới nào tới `consultation_*` | ADR-DCC-001 |
| C2 | Firebase publish luôn sau commit, best-effort, không throw ra request gốc | ADR-DCC-002 |
| C3 | Payload Firebase chỉ 5 field cố định | ADR-DCC-002, BR-DCC-006 |
| C4 | Idempotent insert dùng `REQUIRES_NEW` isolated writer | BR-DCC-005 |
| C5 | `duration_seconds` luôn tính ở server; `EndCallRequest` không có field này | BR-DCC-008 |
| C6 | Mọi AuditAction mới phải có migration widen CHECK constraint đi kèm | §5.2 |
| C7 | Firebase path = `/user-conversation-events/{recipientUserId}/{eventId}`; recipient = counterpart của actor, không bao giờ actor | ADR-DCC-004, BR-DCC-010 |
| C8 | `POST /firebase/custom-token` không nhận input nào ngoài JWT | BR-DCC-013 |
| C9 | MISSED chỉ set bởi `CallTimeoutReconciliationJob` qua conditional UPDATE; không có endpoint client | BR-DCC-011, ADR-DCC-005 |
| C10 | Rollback sau khi migration đã áp dụng ở môi trường chia sẻ = migration xuôi mới, không DROP/xoá schema_history | §12 |
| C11 | `assertConversationWritable()` chạy TRƯỚC mọi persist trong mọi write path trừ `end()` khi call đang `ANSWERED`; không bao giờ persist rồi mới kiểm tra | BR-DCC-015, ADR-DCC-007 |
| C12 | `message_type` chỉ nhận `'TEXT'`; không thêm FILE/SYSTEM vào entity/DTO/CHECK trong pass này | BR-DCC-016 |
