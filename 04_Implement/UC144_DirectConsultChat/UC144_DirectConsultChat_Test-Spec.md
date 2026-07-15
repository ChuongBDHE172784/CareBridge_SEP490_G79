# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC144 (Redesign) — Direct Consult Chat & Call — Test Specification

**Document ID:** `CB-CHAT-TDD-144D`
**Version:** `1.2`
**Date:** `2026-07-15`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect + Test Designer`
**Reviewed by:** `User, 2026-07-15`
**DPO Sign-off:** `[ ] Pending` *(outstanding — proceeding for dev/test only, per standing user decision; direct messages/calls are new PII surfaces, DPO review remains a real gap, not waived)*
**Approved by:** `User, 2026-07-15`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC144_DirectConsultChat/UC144_DirectConsultChat_TDS.md` (`CB-CHAT-IMP-144D`, v1.2)
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` — baseline (not modified)
- Superseded: `04_Implement/UC144_ConsultViaChat/`, relevant parts of `UC95_ManageConsultationSession/`, `UC154_EstablishRealtimeCommunicationSession/`

> **Quy ước TDD:** viết test trước → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢. Không mark ✅ nếu `./mvnw test`/`flutter test`/web build chưa xanh thật.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-15 | AI Agent — Technical Architect | v1.0 — Tạo tài liệu lần đầu (Draft). |
| 2026-07-15 | AI Agent — Technical Architect | v1.1 — **Request Changes (vẫn Draft).** Cập nhật DCC-TC-008/010/013 theo TDS v1.1; thêm 12 test case mới `DCC-TC-019..030` (custom-token isolation, RTDB Rules qua Emulator, recipient-only delivery, duplicate/expired event, same-timestamp ordering, before/after cursor semantics tường minh, call-transition authorization, answer-vs-timeout race, Expert-revoked event behavior, `last_activity_at` từ cả message/call). |
| 2026-07-15 | AI Agent — Technical Architect | v1.2 — **Request Changes #2 (vẫn Draft — user xác nhận Approve sau khi áp dụng).** Viết lại hoàn toàn `DCC-TC-029` thành 1 test case dạng ma trận (5 kịch bản: Mother read allowed, Mother send/call blocked, Expert send/call blocked, end() trên call ANSWERED vẫn cho phép, end() trên call chưa answer vẫn bị chặn) theo ADR-DCC-007/BR-DCC-015 — không còn giả định "message saved but event skipped". Thêm bước kiểm `message_type='FILE'` bị CHECK constraint từ chối vào `DCC-TC-INT-001` (BR-DCC-016) và bước 10 xác nhận DCC-010/read-allowed trên DB thật. Tổng số test case giữ nguyên `DCC-TC-001..030` (không thêm số mới — 4 kịch bản Expert-revocation gộp vào DCC-TC-029 dạng matrix, đúng yêu cầu giữ nguyên khoảng tham chiếu tài liệu). |
| 2026-07-15 | AI Agent — Technical Architect | **v1.2 — APPROVED.** User đã duyệt sau 3 sửa lỗi biên tập: (1) Entry Criteria tham chiếu TDS v1.1→v1.2; (2) Rollback §12 tham chiếu TDS v1.1→v1.2; (3) `DCC-TC-029` "4 kịch bản" → "5 kịch bản" (đúng số bước thực tế trong test). Status chuyển `Draft` → `Approved`. Phạm vi được xác nhận lại: call = record + Firebase signaling only, KHÔNG có ZegoCloud RTC audio/video thật trong pass này. Bắt đầu Red Gate ngay sau entry này. |
| 2026-07-15 | AI Agent — Amelia (Dev Agent) | **IMPLEMENTED (backend + mobile + web + Firebase).** Red Gate xác nhận (mọi service/policy stub throw `UnsupportedOperationException`, chạy `./mvnw test` → toàn bộ RED trước khi implement) → implement thật → GREEN. **Backend: 47/47 unit test passing** (`./mvnw test -Dtest=com.carebridge.backend.directchat.**,com.carebridge.backend.integration.firebase.**`), full `mvn test` xác nhận 0 regression (95 failures/errors trước đó không đổi, tất cả thuộc module không liên quan — Docker-unavailable Testcontainers + 1 bug Hibernate 7.4 có sẵn trong `CommunityQuestionRepository`, không phải do pass này). **Mobile: 45/45 test passing**, `dart analyze` sạch, **`flutter build apk --debug` THÀNH CÔNG** (gỡ `zego_zim` đã sửa luôn lỗi Gradle chặn build ở pass UC-144 cũ). **Web:** `npx vite build`/`tsc --noEmit`/`eslint` đều sạch cho code mới, `npm audit` 0 vulnerability (khác hẳn `zego-zim-web` cũ có lỗ hổng critical). **DCC-TC-020 (Firebase Rules) chạy thật trên Firebase Emulator** (`firebase emulators:start`), không phải mock — 4/4 assertion PASS, script lưu tại `05_Development/Firebase/rules-test/`. Bảng Red-Green đầy đủ ở §5; các TC còn RED (integration-only cần Postgres/Docker thật, và mobile/web widget-test) được flag rõ, không claim GREEN giả. |

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-144D` |
| **Module** | `directchat` |
| **Priority** | 🔴 P0 |
| **Data Classification** | `Sensitive-PII` |
| **Upstream Dependencies** | `users`, `expert_profiles`, Firebase Admin SDK + Emulator Suite (RTDB + Auth), `IZegoCloudService` (UC-154, unchanged) |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix áp dụng trong test |
|---|---|---|---|
| L1 | Bản UC-144 cũ giả định chat cần `consultation_sessions` | Direct chat không cần booking/session | Test không seed bất kỳ `consultation_*` row nào |
| L2 | `audit_logs.action` CHECK constraint — bản trước bỏ sót migration widen | Xác nhận qua `V20260709203900__...` | `DCC-TC-INT-001` chạy trên DB thật (Testcontainers) |
| L3 | Không rõ live call UI có trong scope | User xác nhận: chỉ record + signaling | Không có test "audio renders" |
| L4 | v1.0 dùng Firebase Rules test bằng "mock", chưa đủ chứng minh Rules thật hoạt động | User yêu cầu Emulator thật | `DCC-TC-020` bắt buộc chạy qua Firebase Emulator Suite (`firebase emulators:exec`), không chỉ mock Java |
| L5 | v1.0 chưa đặc tả conditional-update cho `answer`/timeout race | Postgres affected-row-count là oracle duy nhất đáng tin, không phải "load rồi so sánh state trong Java" (time-of-check-to-time-of-use race) | `DCC-TC-028` giả lập 2 request đồng thời qua service method gọi trực tiếp repository conditional-update |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
directchat bao gồm:
├── Policy (mock Repository — Mockito)
├── Service (mock Repository — Mockito, incl. race/idempotency/conditional-update simulation)
├── Controller (@WebMvcTest, mock Service)
├── Firebase adapter (mock FirebaseDatabase/FirebaseAuth cho unit test)
├── Firebase Rules (Firebase Emulator Suite — KHÔNG mock, test Rules thật)
├── Integration (Testcontainers PostgreSQL — CHECK constraint, FK, unique constraint thật)
├── Scheduled jobs (CallTimeoutReconciliationJob, FirebaseEventRetentionJob — mock Clock + repository)
├── Mobile (flutter test — model, service, optimistic merge, Firebase adapter với emulator/mock)
└── Web (nếu thêm vitest — xem DCC-TC-WEB, còn treo do chưa có framework, xem §6 Entry Criteria)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|---|---|
| User requirement list (2026-07-15) + Request Changes (2026-07-15) | Toàn bộ TC dưới đây |
| `UC144_DirectConsultChat_TDS.md` v1.1 §3 ADR, §6 sequence/state machine, §9.2 cursor spec | Oracle cho expected behavior |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|---|---|---|---|
| TC-COND-001 | Find-or-create idempotent dưới concurrent request | `DirectConversationServiceImpl.findOrCreate` | DCC-TC-001, DCC-TC-002 |
| TC-COND-002 | Từ chối non-Mother tạo conversation | `DirectConversationPolicy` | DCC-TC-003 |
| TC-COND-003 | Từ chối Expert chưa APPROVED | `DirectConversationPolicy.assertExpertVerified` | DCC-TC-004 |
| TC-COND-004 | Chỉ participant truy cập được | `DirectConversationPolicy.assertIsParticipant` | DCC-TC-005, DCC-TC-006, DCC-TC-017 |
| TC-COND-005 | Lịch sử bền vững, hiển thị lại đầy đủ | `DirectMessageRepository`, `GET /timeline` | DCC-TC-007, DCC-TC-016 |
| TC-COND-006 | Timeline hợp nhất MESSAGE + CALL_EVENT theo thời gian, kể cả cùng timestamp | `ConversationTimelineRepository` (native UNION ALL) | DCC-TC-008, DCC-TC-023 |
| TC-COND-007 | Idempotency gửi tin nhắn | `sendMessage` + unique constraint | DCC-TC-009, DCC-TC-014 |
| TC-COND-008 | Firebase lỗi không rollback DB | `@TransactionalEventListener(AFTER_COMMIT)` | DCC-TC-010 |
| TC-COND-009 | Payload Firebase không chứa dữ liệu nhạy cảm | `ConversationEventPayload` | DCC-TC-011 |
| TC-COND-010 | Call state machine đúng, đúng vai trò actor | `ConversationCallServiceImpl` | DCC-TC-012, DCC-TC-027 |
| TC-COND-011 | Duration tính ở server, bỏ qua client | `ConversationCallServiceImpl.end` | DCC-TC-013 |
| TC-COND-012 | Missed/declined/cancelled hiển thị đúng | timeline mapper | DCC-TC-015 |
| TC-COND-013 | UI: nút "Trò chuyện" điều hướng đúng | mobile/web widget test | DCC-TC-MOB-001, DCC-TC-WEB-001 |
| TC-COND-014 | Reopen conversation load lại lịch sử có phân trang | mobile/web integration | DCC-TC-MOB-002 |
| TC-COND-015 | Cursor `before`/`after` đúng semantics, đúng biên trang | `ConversationTimelineRepository` | DCC-TC-024, DCC-TC-025, DCC-TC-026 |
| TC-COND-016 | Custom token luôn ứng userId từ JWT, không thể xin token hộ người khác | `FirebaseTokenController` | DCC-TC-019 |
| TC-COND-017 | RTDB Rules chặn đọc chéo user | Firebase Emulator | DCC-TC-020 |
| TC-COND-018 | Event chỉ gửi recipient (counterpart), không gửi actor | `ConversationEventPublisherImpl.resolveRecipient` | DCC-TC-021 |
| TC-COND-019 | Duplicate/expired Firebase event không gây lỗi logic | client-side event handling | DCC-TC-022 |
| TC-COND-020 | Answer-vs-timeout race có đúng 1 kết quả | `ConversationCallRepository` conditional UPDATE | DCC-TC-028 |
| TC-COND-021 | `last_activity_at` cập nhật từ cả message và call | `DirectConversationRepository.touchActivity` | DCC-TC-030 |
| TC-COND-022 | Expert de-verification chặn ghi cả 2 phía, không chặn đọc của Mother, ngoại trừ end() trên call ANSWERED | `DirectConversationPolicy.assertConversationWritable` | DCC-TC-029 |
| TC-COND-023 | `message_type` chỉ chấp nhận TEXT | `chk_direct_messages_type`, request DTO | DCC-TC-INT-001 |

### TDS-04 — Test Techniques

| Technique | Applied To |
|---|---|
| Equivalence Partitioning | role (Mother/Expert/other), verification_status (APPROVED/others), call actor (caller/callee) |
| Boundary Value Analysis | `limit` pagination, `messageBody` length (1, 2000, 2001 ký tự), page boundary cursor |
| State Transition Testing | `call_status` state machine + authorization theo actor |
| Error Guessing | replay clientMessageId khác body; concurrent find-or-create; answer đúng lúc timeout job chạy; đọc inbox người khác qua Rules |

### TDS-05 — Test Data Requirements

| Fixture ID | Value |
|---|---|
| FX-001 | Mother user (SYNTHETIC), role MOTHER |
| FX-002 | Expert user + `expert_profiles.verification_status='APPROVED'` |
| FX-003 | Expert user + `verification_status='PENDING'` |
| FX-004 | Second Mother/Expert pair (non-participant negative case) |
| FX-005 | Firebase Emulator project config (`firebase.json` emulators block: `database`, `auth`) — local only, không kết nối project thật |

---

## 4. Test Case Specification

```java
class DirectChatTestFactory {
    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    static final UUID EXPERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");
    static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000303");
    static final UUID OTHER_MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000304");
    static final UUID OTHER_EXPERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000305");
}
```

---

### DCC-TC-001 — Find-or-create returns existing conversation on second call

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-002
**Test Steps:** Gọi `findOrCreate(mother, expert)` 2 lần liên tiếp.
**Expected Result:** Lần 2 trả cùng `conversationId`; insert chỉ gọi 1 lần.
**Current Status:** 🔴 Not written

---

### DCC-TC-002 — Concurrent find-or-create does not create duplicate rows (race)

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-002
**Test Steps:** Mock repository ném `DataIntegrityViolationException` ở insert thứ 2 → service bắt và SELECT lại.
**Expected Result:** Trả conversation đã tồn tại, không throw ra controller.
**Current Status:** 🔴 Not written

---

### DCC-TC-003 — Non-Mother caller rejected (403 DCC-001)

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED
**Expected Result:** 403 `DCC-001`, không tạo row.
**Current Status:** 🔴 Not written

---

### DCC-TC-004 — Expert not APPROVED rejected at creation (422 DCC-002)

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED
**Expected Result:** 422 `DCC-002`, không tạo conversation.
**Current Status:** 🔴 Not written

---

### DCC-TC-005 — Non-participant cannot read timeline (403 DCC-003)

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED
**Current Status:** 🔴 Not written

---

### DCC-TC-006 — Non-participant cannot send message (403 DCC-003)

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED
**Current Status:** 🔴 Not written

---

### DCC-TC-007 — Message history persists across requests

**Severity:** `HIGH` · **TDD Phase:** 🔴 RED
**Test Steps:** Gửi 5 message → `GET /timeline` trong lời gọi riêng biệt.
**Expected Result:** Cả 5 đúng thứ tự, đúng `sender`/`messageType`/`createdAt`.
**Current Status:** 🔴 Not written

---

### DCC-TC-008 — Unified timeline orders MESSAGE and CALL_EVENT via UNION ALL cursor, correct across page boundaries

**Severity:** `HIGH` · **TDD Phase:** 🔴 RED
**Feature Under Test:** `ConversationTimelineRepository` (native UNION ALL, TDS §9.2)
**Test Steps:** Seed xen kẽ: message(t1), call ended(t2, `initiated_at`=t2), message(t3), call ended(t4). Gọi không cursor với `limit=2`, rồi tiếp tục phân trang bằng `previousCursor` để lấy hết.
**Expected Result:** Thứ tự tổng hợp đúng `t4,t3,t2,t1` (mới nhất trước) xuyên suốt các trang; không lặp/không sót item nào ở biên trang. Đây là oracle chính cho tính đúng của `(sort_ts, kind, resource_id)` cursor, không phải merge-trong-bộ-nhớ.
**Current Status:** 🔴 Not written

---

### DCC-TC-009 — Retry with same clientMessageId does not duplicate (200, not 201)

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-005
**Expected Result:** Lần 1 → 201; lần 2 (cùng clientMessageId+body) → 200, cùng `messageId`, không row mới.
**Current Status:** 🔴 Not written

---

### DCC-TC-010 — Firebase publish failure does not roll back the saved message

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-007, ADR-DCC-002
**Test Steps:** Mock `FirebaseDatabase` ném exception khi publish sau khi message đã commit (listener `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` — chạy trên thread pool riêng, không phải request thread, xem TDS §6.2/§8).
**Expected Result:** `direct_messages` row vẫn tồn tại; response 201, không bị trễ bởi việc publish (test dùng `CountDownLatch`/timing để xác nhận response trả về trước khi async publish hoàn tất); exception được log, không propagate, không rollback.
**Current Status:** 🔴 Not written

---

### DCC-TC-011 — Firebase payload never contains messageBody or health data

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-006
**Test Steps:** Reflection assertion — `ConversationEventPayload` chỉ có đúng 5 field.
**Expected Result:** Gửi message chứa từ khoá nhạy cảm → payload captured qua mock không chứa chuỗi đó.
**Current Status:** 🔴 Not written

---

### DCC-TC-012 — Call state transitions follow the state machine (§6.3)

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** TDS §6.3
**Test Steps (matrix):** INITIATED→RINGING (ok), RINGING→ANSWERED (ok), ANSWERED→ENDED (ok), ANSWERED→RINGING (invalid, 409), ENDED→ANSWER (invalid, 409).
**Current Status:** 🔴 Not written

---

### DCC-TC-013 — Duration is server-computed; EndCallRequest has no client-suppliable field

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-008
**Test Steps:** `answeredAt=T0` (mock Clock), `end()` tại `T0+184s`. Static test: `EndCallRequest` class có 0 field liên quan duration (reflection, không chỉ "field bị bỏ qua").
**Expected Result:** Persisted `duration_seconds=184`.
**Current Status:** 🔴 Not written

---

### DCC-TC-014 — Reconnect after missing Firebase events still surfaces all messages exactly once

**Severity:** `HIGH` · **TDD Phase:** 🔴 RED
**Test Steps:** Client A gửi 3 message trong khi Client B "mất kết nối" → B reconnect, `GET /timeline?after={cursor cũ}`.
**Expected Result:** Cả 3 xuất hiện đúng 1 lần; gọi lại cùng cursor không trùng lặp.
**Current Status:** 🔴 Not written

---

### DCC-TC-015 — Missed / declined / cancelled calls surface distinct status in timeline

**Severity:** `HIGH` · **TDD Phase:** 🔴 RED
**Test Steps:** Seed 1 call mỗi loại: MISSED (qua `conditionallyMarkMissed`, không qua API), DECLINED, CANCELLED, ENDED.
**Expected Result:** `callStatus` đúng; `durationSeconds=null` trừ ENDED.
**Current Status:** 🔴 Not written

---

### DCC-TC-016 — Reopen conversation reloads full paginated history (no cursor = latest page)

**Severity:** `HIGH` · **TDD Phase:** 🔴 RED
**Test Steps:** Seed 50 message → `GET /timeline` không cursor.
**Expected Result:** Trang mới nhất (`limit` items), `hasMoreOlder=true`, `previousCursor` hợp lệ.
**Current Status:** 🔴 Not written

---

### DCC-TC-017 — IDOR: Expert of a different conversation cannot access this one

**Severity:** `CRITICAL` · **OWASP:** `A01:2021` · **TDD Phase:** 🔴 RED
**Test Steps:** Expert B (APPROVED, conversation khác) gọi `GET /timeline` của conversation Mother A ↔ Expert A.
**Expected Result:** 403 `DCC-003`.
**Current Status:** 🔴 Not written

---

### DCC-TC-018 — Expert whose verification is revoked loses access to existing conversations (ongoing re-check)

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-003
**Test Steps:** Tạo conversation khi Expert APPROVED → đổi `verification_status` sang PENDING → Expert gọi lại `GET /timeline`.
**Expected Result:** 403 `DCC-002`. Mother vẫn `GET /timeline` bình thường (200).
**Current Status:** 🔴 Not written

---

### FIREBASE AUTH BRIDGE TEST CASES

---

### DCC-TC-019 — Custom token cannot be issued for another user (token isolation)

**Severity:** `CRITICAL` · **OWASP:** `A01:2021 — Broken Access Control` · **TDD Phase:** 🔴 RED
**Feature Under Test:** `FirebaseTokenController.POST /firebase/custom-token`
**Oracle Source:** BR-DCC-013

**Test Steps:**
1. Reflection/contract test: `POST /firebase/custom-token` handler method có 0 tham số ngoài `Authentication`/`Principal` — không có `@RequestBody`, không có `@RequestParam("userId")` hay tương tự.
2. User A (JWT hợp lệ) gọi endpoint → decode custom token trả về (JWT không cần verify signature để đọc claim trong test — dùng Firebase Admin SDK `verifyIdToken`-tương đương hoặc parse) → assert `uid` claim `== userA.id.toString()`.
3. User B gọi cùng endpoint → assert `uid` claim `== userB.id.toString()`, khác User A.
4. Thử gửi `{"userId": "<userB-id>"}` làm body dù controller không khai báo nhận nó → assert response vẫn là token của User A (chứng minh body bị bỏ qua hoàn toàn, không có cách nào override).

**Expected Result:** Không có đường nào để User A nhận được token có `uid` của User B.
**Current Status:** 🔴 Not written

---

### DCC-TC-020 — Firebase RTDB Rules deny reading another user's inbox (Firebase Emulator)

**Severity:** `CRITICAL` · **OWASP:** `A01:2021` · **TDD Phase:** 🔴 RED
**Feature Under Test:** `database.rules.json` (ADR-DCC-004 §4)
**Test File:** chạy qua Firebase Emulator Suite — **không** phải unit test Java thông thường (Rules không thể verify bằng mock).
**Precondition:** `firebase emulators:exec` khởi động RTDB + Auth emulator cục bộ, nạp `database.rules.json` thật.

**Test Steps:**
1. Sign in emulator Auth với custom token có `uid=userA`.
2. Cố đọc `/user-conversation-events/{userB}` (khác `userA`) → assert bị **PERMISSION_DENIED**.
3. Đọc `/user-conversation-events/{userA}` (chính mình) → assert **thành công**.
4. Cố **ghi** trực tiếp (không qua Admin SDK) vào `/user-conversation-events/{userA}/fake-event` bằng client SDK đã sign-in → assert bị từ chối (`.write: false` tuyệt đối, kể cả ghi vào node của chính mình).

**Expected Result:** Rules đúng như ADR-DCC-004 §4 — đọc chỉ chính mình, ghi luôn bị chặn từ client.
**Current Status:** 🔴 Not written
**Ghi chú môi trường:** Cần Firebase CLI + Java/Node runtime cho Emulator — xác minh khả dụng trong CI/sandbox trước khi implement; nếu không khả dụng, ghi rõ RED/deferred, không claim GREEN giả.

---

### DCC-TC-021 — Firebase event is delivered only to the recipient, never to the sender/actor

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-010, ADR-DCC-004

**Test Steps:** Mother gửi message → capture mọi lời gọi tới `FirebaseDatabase` (mock) → assert **chỉ 1** lời ghi, tới path `/user-conversation-events/{expertUserId}/...` — không có bất kỳ ghi nào tới `/user-conversation-events/{motherUserId}/...`.
**Expected Result:** Sender không bao giờ nhận signal về hành động của chính mình.
**Current Status:** 🔴 Not written

---

### DCC-TC-022 — Duplicate or expired Firebase event does not break client reconciliation

**Severity:** `MEDIUM` · **TDD Phase:** 🔴 RED
**Feature Under Test:** client-side event handler (mobile) — nhận cùng `eventId` 2 lần (RTDB có thể replay khi reconnect), hoặc nhận event mà `occurredAt` đã cũ hơn cursor client hiện có.
**Test Steps:** Đưa 2 event giống hệt `eventId` vào handler → assert chỉ gọi `GET /timeline` reconcile 1 lần hiệu quả (dedupe theo `eventId` phía client) hoặc gọi nhiều lần nhưng không tạo duplicate UI item (dedupe cuối cùng theo `messageId` từ REST, không theo `eventId`). Event có `occurredAt` < cursor hiện tại của client → bị bỏ qua, không kích hoạt fetch lùi về trước.
**Expected Result:** Không duplicate UI item, không lỗi, không loop fetch vô hạn.
**Current Status:** 🔴 Not written

---

### CURSOR / PAGINATION TEST CASES

---

### DCC-TC-023 — Multiple items with identical sort_ts are ordered deterministically via (kind, resourceId) tie-break

**Severity:** `HIGH` · **TDD Phase:** 🔴 RED · **Oracle:** TDS §9.2
**Test Steps:** Seed 1 message và 1 call với `created_at`/`initiated_at` **giống hệt nhau** (cùng millisecond, ép bằng fixed Clock/raw SQL insert), cùng conversation. Gọi `GET /timeline` (không cursor) rồi lặp lại nhiều lần.
**Expected Result:** Thứ tự trả về **giống nhau tuyệt đối** ở mọi lần gọi (deterministic), theo đúng tie-break `kind DESC, resource_id DESC` (hoặc ASC tuỳ hướng) — không phụ thuộc thứ tự vật lý trên đĩa.
**Current Status:** 🔴 Not written

---

### DCC-TC-024 — `before` cursor semantics: loads strictly older items in correct order

**Severity:** `HIGH` · **TDD Phase:** 🔴 RED · **Oracle:** TDS §9.2
**Test Steps:** Seed 10 item liên tiếp → lấy trang mới nhất (5 item) → dùng `previousCursor` gọi `before=` → assert 5 item tiếp theo đều **cũ hơn nghiêm ngặt** (`<`) so với item cũ nhất của trang trước, đúng thứ tự.
**Current Status:** 🔴 Not written

---

### DCC-TC-025 — `after` cursor semantics: syncs strictly newer items on reconnect

**Severity:** `HIGH` · **TDD Phase:** 🔴 RED · **Oracle:** TDS §9.2
**Test Steps:** Client giữ cursor tại item thứ 5/10 → 3 item mới được thêm trong lúc offline → reconnect, gọi `after=cursor` → assert nhận đúng các item **mới hơn nghiêm ngặt** (`>`) cursor, đúng thứ tự tăng dần, không lặp item thứ 5.
**Current Status:** 🔴 Not written

---

### DCC-TC-026 — Cursor round-trip: `before` then `after` returns to the same boundary without gap or duplicate

**Severity:** `MEDIUM` · **TDD Phase:** 🔴 RED
**Test Steps:** Từ trang giữa, lấy `before` rồi lấy `after` bằng cursor vừa nhận được → assert quay lại đúng bộ item ban đầu, không thiếu không thừa.
**Current Status:** 🔴 Not written

---

### CALL AUTHORIZATION & CONCURRENCY TEST CASES

---

### DCC-TC-027 — Call transition authorization: only the correct role (caller/callee) may perform each transition

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** TDS §6.3 bảng authorization
**Test Steps (matrix):**
- Caller gọi `PATCH /answer`/`/ringing`/`/decline` trên call của chính mình → 403 `DCC-009`.
- Callee gọi `PATCH /end` khi call còn `INITIATED`/`RINGING` (chưa answer) → 403 `DCC-009` (chỉ caller được huỷ trước khi answer).
- Callee gọi `PATCH /end` khi `ANSWERED` → cho phép (200).
- Caller gọi `PATCH /end` khi `ANSWERED` → cho phép (200).
**Current Status:** 🔴 Not written

---

### DCC-TC-028 — Answer-vs-timeout race: exactly one outcome, no swallowed exception

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** ADR-DCC-005

**Test Steps:**
1. Seed call ở `RINGING`, `initiated_at` đã vượt quá `call-ring-timeout-seconds`.
2. Gọi đồng thời (2 thread hoặc tuần tự sát nhau mô phỏng race): `ConversationCallRepository.conditionallyAnswer(callId, now)` và `CallTimeoutReconciliationJob.reconcileMissedCalls()` (gọi `conditionallyMarkMissed(callId)`).
3. Assert: đúng 1 trong 2 lời gọi trả `rowsAffected==1`, lời còn lại trả `0` — không có exception nào ném ra ở phía "thua".
4. Assert trạng thái cuối trong DB nhất quán với thao tác đã thắng (`ANSWERED` hoặc `MISSED`, không phải giá trị lẫn lộn).
5. Nếu `answer` thua (đã bị `MISSED`) → controller trả `409 DCC-007` cho request `PATCH /answer` gốc, không phải 500.

**Expected Result:** Không bao giờ có cả 2 cùng thắng; không bao giờ mất update do race.
**Current Status:** 🔴 Not written
**Implementation Note:** Không dùng `@Version` optimistic locking (đủ nhưng phức tạp hơn cần thiết) — dùng trực tiếp conditional `UPDATE ... WHERE call_status='RINGING'` + kiểm `rowsAffected`, như TDS §8.2 đặc tả.

---

### DCC-TC-029 — Expert de-verification blocks new writes from both participants, but preserves Mother's read access and in-flight answered-call cleanup

**Severity:** `CRITICAL` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-015, ADR-DCC-007 (thay thế hoàn toàn phiên bản DCC-TC-029 cũ — không còn giả định "message saved but event skipped")

**Precondition chung:** Conversation tồn tại giữa Mother và Expert đang `APPROVED` → đổi `expert_profiles.verification_status` sang `PENDING`.

**Test Steps (matrix — 5 kịch bản bắt buộc):**

1. **Mother read allowed:** Mother gọi `GET /{id}/timeline` → assert `200`, trả đúng lịch sử cũ đầy đủ (không bị cắt bớt).
2. **Mother send/call blocked:** Mother gọi `POST /{id}/messages` (body hợp lệ) → assert `409 DCC-010`; assert **không có row mới** trong `direct_messages` (query lại đếm số dòng trước/sau); assert `FirebaseDatabase` mock **không** nhận bất kỳ lời gọi ghi nào. Lặp lại với `POST /{id}/calls` → cùng kết quả (409, không tạo `conversation_calls` row, không publish).
3. **Expert send/call blocked:** Expert gọi `POST /{id}/messages` và `POST /{id}/calls` → assert `403 DCC-002` (Expert đã bị chặn truy cập từ `assertIsParticipant`/BR-DCC-003 trước cả khi chạm tới `assertConversationWritable` — thứ tự chặn không quan trọng với Expert vì cả 2 đều dẫn tới từ chối, nhưng test phải xác nhận không có row nào được tạo bất kể lỗi 403 hay 409).
4. **Ending an already-ANSWERED call remains allowed:** Seed 1 call ở trạng thái `ANSWERED` (answered trước khi Expert bị revoke, mô phỏng cuộc gọi đang diễn ra) → Mother hoặc Expert gọi `PATCH .../end` → assert `200`, `call_status=ENDED`, `duration_seconds` tính đúng như bình thường (không bị `assertConversationWritable` chặn — ngoại lệ ADR-DCC-007 §2).
5. **Phản chứng cho bước 4:** Seed 1 call khác ở trạng thái `RINGING` (chưa answer) sau khi Expert đã bị revoke → gọi `PATCH .../end` → assert `409 DCC-010` (đây KHÔNG phải "đóng call đang chạy", ngoại lệ không áp dụng).

**Expected Result:** Đúng cả 5 assertion trên trong 1 test method (hoặc 5 `@Test` riêng cùng `@Nested` class) — không còn kỳ vọng "message vẫn lưu, chỉ publish bị bỏ qua" như thiết kế BR-DCC-010 cũ.
**Current Status:** 🔴 Not written

---

### DCC-TC-030 — `last_activity_at` updates on both message send and call transitions

**Severity:** `MEDIUM` · **TDD Phase:** 🔴 RED · **Oracle:** BR-DCC-014

**Test Steps:**
1. Tạo conversation → assert `last_activity_at IS NULL`.
2. Gửi message → assert `last_activity_at` = thời điểm insert message.
3. Initiate call → assert `last_activity_at` cập nhật lại (mới hơn bước 2).
4. Answer rồi end call → assert `last_activity_at` cập nhật lại lần nữa (mới hơn bước 3).
5. Gọi `PATCH /answer` với call ở sai trạng thái (409, bị policy từ chối) → assert `last_activity_at` **không đổi** so với trước lời gọi này (chỉ transition thành công mới touch).

**Expected Result:** `last_activity_at` phản ánh đúng hoạt động gần nhất trên conversation, từ cả 2 nguồn (message, call), và không bị touch bởi request thất bại.
**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### DCC-TC-INT-001 — Full flow against real PostgreSQL: schema constraints + find-or-create → send → timeline → call lifecycle

**Severity:** `CRITICAL`
**Test File:** `src/test/java/com/carebridge/backend/directchat/DirectChatIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Preconditions:** PostgreSQL container, Flyway chạy migration mới tự động.

**Test Steps:**
1. find-or-create → 1 row `direct_conversations`
2. Thử insert `direct_messages` với `client_message_id=NULL` trực tiếp qua JDBC → assert **NOT NULL violation** (xác nhận DDL siết đúng như thiết kế, không chỉ ở tầng Java)
3. Thử insert `message_body=''` (rỗng sau trim) → assert **CHECK violation** (`chk_direct_messages_body_length`)
4. Thử insert `direct_messages` với `message_type='FILE'` trực tiếp qua JDBC → assert **CHECK violation** (`chk_direct_messages_type`, TC-COND-023 — BR-DCC-016: chỉ `'TEXT'` được phép trong pass này)
5. Thử insert `conversation_calls` với `call_status='BOGUS'` → assert **CHECK violation** (`chk_conversation_calls_status`)
6. Thử insert `conversation_calls` với `call_status='ENDED', answered_at=NULL` → assert **CHECK violation** (`chk_conversation_calls_ended_requires_answered`)
7. send message hợp lệ → audit log ghi thành công (bắt lỗi CHECK constraint trên `audit_logs` nếu migration widen bị thiếu)
8. initiate → ringing → answer (qua `conditionallyAnswer`) → end call → assert `conversation_calls` đúng field, đúng `duration_seconds`
9. `GET /timeline` → assert thứ tự đúng qua UNION ALL thật (không phải H2/mock)
10. Đổi `expert_profiles.verification_status` sang `PENDING` → thử `POST /messages` từ Mother → assert **409 DCC-010**, không có row mới; `GET /timeline` từ Mother vẫn `200` (đối chứng DCC-TC-029 trên DB thật, không chỉ mock)

**Expected Result:** Toàn bộ thành công; mọi CHECK/NOT NULL constraint ở bước 2–5 đúng như thiết kế bị Postgres từ chối.
**Current Status:** 🔴 Not written
**Ghi chú môi trường:** Sandbox hiện tại không có Docker — compile nhưng không chạy được; phải chạy ở máy có Docker trước khi merge.

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED confirmed | 🟢 GREEN (commit) | Ghi chú |
|---|---|---|---|
| DCC-TC-001 | `[x]` | Passed | `DirectConversationServiceImplTest` |
| DCC-TC-002 | `[x]` | Passed | idem |
| DCC-TC-003 | `[ ]` | — | Enforced declaratively (`@PreAuthorize("hasRole('MOTHER')")`), không re-test riêng — RED/deferred |
| DCC-TC-004 | `[x]` | Passed | `DirectConversationPolicyImplTest` + service test |
| DCC-TC-005 | `[x]` | Passed | `assertIsParticipant_nonParticipant_throws403` (cơ chế dùng chung timeline/message) |
| DCC-TC-006 | `[x]` | Passed | idem |
| DCC-TC-007 | `[ ]` | — | Cần DB thật để chứng minh persistence qua nhiều request — integration-only (xem INT-001) |
| DCC-TC-008 | `[ ]` | — | UNION ALL cần Postgres thật — integration-only |
| DCC-TC-009 | `[x]` | Passed | `DirectMessageServiceImplTest` (retry + race) |
| DCC-TC-010 | `[x]` | Passed | `ConversationEventPublisherImplTest.publish_gatewayThrows_doesNotPropagate` |
| DCC-TC-011 | `[x]` | Passed | `publish_payloadContainsOnlyFiveFixedFields` |
| DCC-TC-012 | `[x]` | Passed | `ConversationCallServiceImplTest` (transition matrix) |
| DCC-TC-013 | `[x]` | Passed | `end_answeredCall_computesDurationFromClock` |
| DCC-TC-014 | `[ ]` | — | Cursor thật cần Postgres — integration-only |
| DCC-TC-015 | `[ ]` | — | idem |
| DCC-TC-016 | `[ ]` | — | idem |
| DCC-TC-017 | `[x]` | Passed | IDOR = trường hợp riêng của non-participant, cùng cơ chế đã test |
| DCC-TC-018 | `[x]` | Passed | `assertIsParticipant_expertParticipantRevoked_throws403` |
| DCC-TC-019 | `[x]` | Passed | `FirebaseAuthBridgeServiceImplTest` + `FirebaseTokenControllerTest` |
| DCC-TC-020 | `[x]` | **Passed — Firebase Emulator thật** | `05_Development/Firebase/rules-test/rtdb-rules.test.mjs`, 4/4 assertion PASS chạy trực tiếp trên `firebase emulators:start`, không phải mock |
| DCC-TC-021 | `[x]` | Passed | `publish_actorIsMother_writesToExpertInbox` + đối xứng |
| DCC-TC-022 | `[ ]` | — | Client-side dedup mobile chưa có test riêng (an toàn nhờ merge idempotent, nhưng chưa test tường minh) |
| DCC-TC-023 | `[ ]` | — | Same-timestamp tie-break cần Postgres — integration-only |
| DCC-TC-024 | `[ ]` | — | idem |
| DCC-TC-025 | `[ ]` | — | idem |
| DCC-TC-026 | `[ ]` | — | idem |
| DCC-TC-027 | `[x]` | Passed | `ConversationCallServiceImplTest` (caller/callee matrix) |
| DCC-TC-028 | `[x]` | Passed (service-level) | `answer_raceLostToTimeoutJob_throws409`; **`CallTimeoutReconciliationJob` tự nó chưa có unit test riêng** (logic thật, compile sạch, chưa test) |
| DCC-TC-029 | `[x]` | Passed | Trải trên nhiều test method (`sendMessage_expertUnavailable_blocksBeforePersistence`, `end_answeredCall_exemptFromWritableCheck_evenWhenExpertUnavailable`, `end_ringingCall_byCaller_stillChecksWritable`) thay vì 1 test ma trận duy nhất |
| DCC-TC-030 | `[x]` | Passed | Trải trên nhiều test (message + call transitions đều verify `touchActivity`) |
| DCC-TC-INT-001 | `[ ]` | — | Compile sạch, **không chạy được** — sandbox không có Docker (`docker info` xác nhận daemon không kết nối được) |
| DCC-TC-MOB-001 (nav từ expert profile) | `[ ]` | — | Điều hướng thật đã xây (`ExpertPublicProfileScreen` → "Trò chuyện" → `DirectChatScreen`), chưa có widget test riêng |
| DCC-TC-MOB-002 (reopen loads paginated history) | `[ ]` | — | Logic thật đã xây (`_loadInitial`/`_loadOlder`), chưa có widget test riêng — chỉ có unit test ở tầng model (`mergeTimelineItems`) |
| DCC-TC-WEB-001/002 | `[ ]` | — | Dự án web vẫn chưa có test framework nào (vitest/jest) — không tự ý thêm, giữ nguyên gap đã biết |

### 5.1 Red Gate Protocol

Chạy ngay sau khi implement stub (`throw UnsupportedOperationException`) cho từng service method — bắt buộc tất cả FAIL trước khi viết implementation thật.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] `UC144_DirectConsultChat_TDS.md` (v1.2) Status = `Approved`
- [ ] Migration DDL (§5.2 TDS) đã được review
- [ ] Xác nhận Firebase CLI + Emulator Suite khả dụng trong môi trường chạy test trước khi implement `DCC-TC-020` — nếu không khả dụng, phải quyết định thay thế (test Rules ở CI riêng, hay bằng cách khác) trước khi bắt đầu, không phát hiện giữa chừng
- [ ] Quyết định về Web test framework còn treo (xem §6 bản v1.0)

### Exit Criteria (DoD)
- [ ] `./mvnw test` xanh cho toàn bộ `directchat` package
- [ ] `flutter test` xanh cho `directChat` feature
- [ ] Không business logic trong Controller
- [ ] Không PII/message body trong log hoặc Firebase payload
- [ ] Migration CHECK-constraint widen đã áp dụng và verify bằng integration test thật
- [ ] Toàn bộ CHECK/NOT NULL mới trong §5.2 TDS có ít nhất 1 test ở `DCC-TC-INT-001` xác nhận DB thật từ chối giá trị vi phạm
- [ ] `DCC-TC-020` (Rules) chạy qua Firebase Emulator thật, không mock

### Suspension Criteria
- Docker không khả dụng → `DCC-TC-INT-001` compile nhưng không chạy — flag rõ, không claim GREEN.
- Firebase CLI/Emulator không khả dụng → `DCC-TC-020` flag RED/deferred tương tự, không tự ý thay bằng mock rồi báo GREEN.

---

## 7. Rollback Plan

> Theo nguyên tắc §12 TDS v1.2: chỉ `git checkout` xoá code/migration file khi migration **chưa từng** áp dụng ở môi trường chia sẻ. Nếu đã áp dụng, dùng migration xuôi mới — không DROP/xoá `flyway_schema_history`.

```bash
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|---|---|---|
| AP-AI-001 | Mọi TC reference BR-DCC-xxx/ADR-DCC-xxx | G-0 |
| AP-AI-002 | Red Gate — stub throw phải FAIL toàn bộ trước implement | G-2 ★ |
| AP-AI-003 | Không TC nào giả định kiến trúc ngoài §3/§6/§9.2 TDS | G-1 |
| AP-AI-005 | Không import class chưa tồn tại trong §8 Interface Spec | G-3 |

**Kết quả review:** Chưa review (Draft) — chờ Approve trước khi bắt đầu Red Gate.
