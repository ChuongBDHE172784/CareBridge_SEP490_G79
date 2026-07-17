# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Expert Consultation Requests

**Document ID:** `CB-CONREQ-IMP-001-TEST`
**Version:** `1.7`
**Date:** `2026-07-16`
**Status:** `Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Technical Architect`
**Reviewed by:** `User — approved in implementation handoff prompt (2026-07-16)`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `User (2026-07-16)`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`, `V20260715120000__create_direct_conversation_schema.sql`, `V20260716010800__add_notification_records_direct_message_idempotency.sql`
- `04_Implement/ExpertConsultationRequests/ExpertConsultationRequests_TDS.md` (CB-CONREQ-IMP-001) — spec gốc
- `02_Requirements/SRS/3_Functional_Specification.md` — UC-75/UC-93/UC-143/UC-160/UC-202/UC-203/UC-205 (mô tả chung, không có con số nghiệp vụ cụ thể — xem §2 Logic Issues)
- PDPA (Luật 91/2025)

> **Quy ước TDD:** viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không dùng PII thật trong test data — chỉ SYNTHETIC.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `2026-07-16` | `AI Agent — Technical Architect` | Khởi tạo Test-Spec cho Expert Consultation Requests, đi kèm TDS `CB-CONREQ-IMP-001` v1.0. |
| `2026-07-16` | `AI Agent — Technical Architect` | v1.1 — Đồng bộ theo TDS v1.1 (self-review correction): viết lại `CONREQ-TC-019` (unit, per-row expiry + publish event, bỏ qua row mất race) và `CONREQ-TC-INT-006` (integration, assert `responded_at` được set đúng theo `chk_consultation_requests_responded_fields` + có `NotificationRecord` cho `REQUEST_EXPIRED`); bổ sung assertion "publish domain event" vào `CONREQ-TC-014` (reject) và `CONREQ-TC-016` (cancel) — trước đó 2 test này chỉ assert `tryTransition`, chưa assert publish. Viết lại `CONREQ-TC-INT-008` thành 2 phần: Part 1 giữ nguyên (redelivery cùng 1 event bị chặn), Part 2 mới — 2 event khác nhau (`REQUEST_CREATED` rồi `REQUEST_CANCELLED`) cho cùng recipient+request phải **đều** được gửi (bug thật đã tìm thấy: index idempotency ban đầu khoá theo `(user_id, reference_id)` sẽ chặn nhầm event thứ 2). Bổ sung `CONREQ-TC-024` (`getById` not-found → 404 `CONREQ-007` — mã lỗi duy nhất trước đó chưa có test) và `CONREQ-TC-025` (`unreadSummary()` backend — trước đó chỉ có test mobile gọi endpoint, chưa có test backend khẳng định `pendingCount` đúng). |
| `2026-07-16` | `AI Agent — Technical Architect` | v1.2 — **Đồng bộ theo TDS v1.2 (User Request Changes, 12 vấn đề)** — vẫn `Status: Draft`. **(1)** `CONREQ-TC-002/003/004` đổi oracle từ `error.code=="CONREQ-001"` (retired) sang `error=="VALIDATION_ERROR"` + `ErrorResponse` flat shape thật; `CONREQ-TC-023` đổi sang assert `error=="INTERNAL_ERROR"` thật. **(2)** `CONREQ-TC-010` viết lại hoàn toàn: từ "403 CONREQ-003" (retired) sang "404 CONREQ-007, response byte-identical với not-found"; thêm `CONREQ-TC-026` (regression: so sánh response not-found vs not-participant phải giống hệt nhau); `CONREQ-TC-SEC-002/003` đổi code kỳ vọng từ `CONREQ-003` sang `CONREQ-007`. **(3)** Thêm `CONREQ-TC-027` (producer test: `ConsultationRequestNotificationServiceImpl` gọi `FcmService`'s overload mới với `data={"type":"CONSULTATION_REQUEST","requestId":...}`, không có `topic`/`description`); `CONREQ-TC-022` viết lại từ "deep-link string builder tự bịa" sang assert đúng data map thật được truyền cho `FcmService`. **(4)** Thêm `CONREQ-FL-15` (Notification Center mobile switch trên `type=='CONSULTATION'`, không phải `'CONSULTATION_REQUEST'`) + `CONREQ-FL-16` (tap-to-navigate dùng `referenceType`). **(5)** `CONREQ-TC-013` viết lại: `tryAccept`'s `EXISTS` predicate = false (không phải `assertExpertStillApproved` tách riêng); thêm `CONREQ-TC-INT-010` — test Testcontainers 2-thread THẬT (mirror `NotificationRecordWriterConcurrencyIntegrationTest`/`OtpRaceConditionIntegrationTest`, `CountDownLatch`+`ExecutorService`, KHÔNG Mockito) race `accept` vs `ExpertProfileServiceImpl.setTrustStatus(SUSPENDED)`. **(6)** `CONREQ-TC-007` viết lại hoàn toàn theo `clientRequestId` (rút lại rule "1 PENDING/cặp"); `CONREQ-TC-INT-001` đổi 2 thread dùng CÙNG `clientRequestId` (không phải "cùng cặp mother-expert"); thêm `CONREQ-TC-028` (2 `clientRequestId` khác nhau tới cùng 1 expert → 2 row PENDING riêng biệt — regression chứng minh rule cũ đã bị bỏ) và `CONREQ-TC-029` (cùng key, payload khác → 409 `CONREQ-009`). **(7)** Thêm `CONREQ-TC-INT-011` (loser transaction rollback đầy đủ — không còn side effect nào từ lần accept thua). **(8)** `CONREQ-TC-019`/`CONREQ-TC-INT-006` bổ sung assertion `actorUserId==null, actorType=="SYSTEM"` cho event `REQUEST_EXPIRED`. **(9)** `CONREQ-FL-09` đổi `find.byType(BottomNavigationBar)` → `find.byType(NavigationBar)` (khớp `expert_home_shell.dart:86` thật). **(10)** `CONREQ-TC-011` viết lại: test cả 2 role (Mother thấy `counterpartDisplayName`=tên expert; Expert thấy `counterpartDisplayName`=tên mother); `CONREQ-TC-INT-002` bổ sung assertion tên counterpart đúng theo role, không chỉ đếm số query. **(11)** `CONREQ-TC-025` đổi tên phương thức `unreadSummary`→`pendingSummary`; `CONREQ-FL-08`/`CONREQ-FL-11` đổi endpoint reference `/unread-summary`→`/pending-summary`. **(12)** `CONREQ-TC-004` bổ sung sub-case "1 phía window null → 400" (both-or-neither); `CONREQ-TC-INT-009` bổ sung assert cột `client_request_id` + constraint mới. Tổng test case: backend (unit/policy/controller + SEC + INT) 38→45 (+7: `CONREQ-TC-013b/026/027/028/029`, `CONREQ-TC-INT-010/011`); Flutter 14→16 (+2: `CONREQ-FL-15/16`). **Tổng cộng 52→61 test case.** |
| `2026-07-16` | `AI Agent — Technical Architect` | v1.3 — Đồng bộ theo TDS v1.3 (User review v1.2, 4 lỗi thiết kế bắt buộc). **(1) TOCTOU thật (row-lock):** `CONREQ-TC-012` bỏ mock `tryAccept`, thêm mock `expertProfileRepository.findByIdForUpdate`; `CONREQ-TC-013` viết lại — mock `findByIdForUpdate` trả `REJECTED`, assert throw `CONREQ-004` và verify ZERO interaction với `tryTransition`/`findOrCreate` (fail nhanh trước mọi side effect); `CONREQ-TC-013b` đơn giản hoá (không còn cần phân biệt ambiguity vì check APPROVED giờ tách biệt hoàn toàn). `CONREQ-TC-INT-004` bổ sung ghi chú lock-serialization side effect. **`CONREQ-TC-INT-010` viết lại hoàn toàn** thành 2 scenario ordering-controlled bằng `CountDownLatch` thật (Scenario A: revoke thắng lock trước — dùng `ExpertProfileServiceImpl.rejectExpert` THẬT, không phải `setTrustStatus` — accept bị chặn đúng `CONREQ-004`, không side effect nào; Scenario B: accept thắng lock trước — accept thành công, revoke sau đó hợp lệ), Testcontainers thật, không Mockito, không xác suất. **(2) ADR sync:** mọi oracle reference tới `tryAccept`/`assertExpertStillApproved`/"1 PENDING per pair" đã sửa sang design v1.3 (row-lock, clientRequestId, tryTransition chung) khớp TDS ADR-CONREQ-003/004/005 đã rewrite. **(3) HTTP 201/200 contract:** thêm `CONREQ-TC-030` (service: `create()` trả `CreateConsultationRequestResult`, `created=true` cho request mới + publish event đúng 1 lần), `CONREQ-TC-031` (controller: `created=true` → `201`), `CONREQ-TC-032` (controller: `created=false` → `200`, cùng response id, không insert, không publish lần 2), `CONREQ-TC-INT-012` (concurrent cùng `clientRequestId` qua HTTP thật: đúng 1 request `201`, request còn lại `200`, cùng resource id, đúng 1 `REQUEST_CREATED` event). **(4) `CONREQ-TC-INT-011` viết lại hoàn toàn** thành race có ordering kiểm soát bằng `@SpyBean`/`Mockito.spy` wrapping `IDirectConversationService` thật (`doAnswer` → `callRealMethod()` → chờ latch) để pause Thread Accept ngay sau `findOrCreate` (đã insert DirectConversation, CHƯA commit), cho Thread Cancel commit `CANCELLED` trước, rồi resume Accept → `tryTransition` trả 0 → `CONREQ-005` → toàn bộ transaction Accept rollback; assert request=CANCELLED, `direct_conversation_id`=null, KHÔNG có `direct_conversations` row mới, không publish/audit `REQUEST_ACCEPTED`. Tổng test case: backend 45→49 (+4: `CONREQ-TC-030/031/032`, `CONREQ-TC-INT-012`; rewrite tại chỗ của TC-013/INT-010/INT-011 không đổi tổng số ID); Flutter không đổi (16). **Tổng cộng 61→65 test case** (33 TC + 4 SEC + 12 INT + 16 FL). |
| `2026-07-16` | `AI Agent — Technical Architect` | v1.4 — **Đồng bộ theo TDS v1.4 (User trả lời 2 câu hỏi mở của v1.3 + 10 nhiệm vụ)** — vẫn `Status: Draft`. Predicate hợp nhất mới `ExpertProfile.isEligibleForConsultation()` (`verificationStatus==APPROVED && trustStatus==ACTIVE`) áp dụng xuyên suốt — mọi test case liên quan đến "expert hợp lệ" giờ phải cover cả 2 trục thay vì chỉ verification. **Task 3 (create):** `CONREQ-TC-006` viết lại thành ma trận đầy đủ (APPROVED+ACTIVE hợp lệ; APPROVED+SUSPENDED/REVOKED bị chặn; mọi verification khác APPROVED bị chặn bất kể trust; ghi chú null-trust fail-closed dù không khả thi qua schema). **Task 4 (accept):** thêm `CONREQ-TC-013c` (APPROVED+SUSPENDED/REVOKED → `CONREQ-004`, zero side effect, mirror cấu trúc `CONREQ-TC-013`). **Task 5 (concurrency):** thêm `CONREQ-TC-INT-013` — 2 scenario latch-controlled accept-vs-`setTrustStatus` (mirror `CONREQ-TC-INT-010`, không xác suất). **Task 6 (directory):** thêm `CONREQ-TC-INT-014` — 4 query `ExpertProfileRepository` chỉ trả APPROVED+ACTIVE, search/specialty/pagination không bypass trust filter, Testcontainers thật. **Task 7 (DirectConversation cross-domain):** thêm `CONREQ-TC-034` (unit, `DirectConversationPolicyImpl.assertExpertEligibleForConsultation` — đổi tên từ `assertExpertVerified`) + `CONREQ-TC-INT-015` (integration, `findOrCreate` thật bị chặn qua đường trực tiếp, existing conversation vẫn đọc được). **Task 8 (write/call cross-domain):** thêm `CONREQ-TC-035` (unit, `assertConversationWritable`) + `CONREQ-TC-INT-016` (integration, `sendMessage`/`initiateCall` bị chặn; **regression bắt buộc** — kết thúc call đã `ANSWERED` KHÔNG bị ảnh hưởng, ADR-DCC-007 §2 giữ nguyên; nhánh cancellable vẫn bị chặn). **Task 9 (reject/cancel/expiry không gate):** thêm `CONREQ-TC-037` — regression tường minh bằng `Mockito.verifyNoInteractions(expertProfileRepository)`, chứng minh 3 method này thực sự không phụ thuộc eligibility chứ không phải "tình cờ pass". **Task 10 (mobile contract):** thêm `CONREQ-TC-036` (unit, `ExpertProfileMapper` set đúng `isConsultationEligible` cho cả 2 DTO, không lộ `trustStatus` trực tiếp); `CONREQ-FL-01` viết lại (CTA "Trò chuyện" VÀ "Yêu cầu tư vấn" gate theo `isConsultationEligible`, không còn `isApproved`); thêm `CONREQ-FL-17` — regression chứng minh widget disable đúng khi `verificationStatus=='APPROVED'` nhưng `isConsultationEligible==false` (không tự suy đoán từ verification một mình). **Cập nhật hỗ trợ:** Props Isolation factory thêm overload `makeExpertProfile(VerificationStatus, TrustStatus)`; TDS-03 thêm `TC-COND-034..041`; TDS-05 thêm `FX-009`; §1.1 constraints thêm C15/C16; Rollback Plan (§7) bổ sung `git checkout` cho toàn bộ file cross-domain (`expert`, `directchat`) bị đụng; AP table (§8) thêm `AP-AI-013/014/015`; Red Gate Verification (§5.1) ghi rõ nhóm TC cross-domain (test method có sẵn, không phải class mới) chứng minh RED bằng cách chạy trước khi sửa code, không phải bằng stub `throw`. Tổng test case: backend (unit/policy/mapper + SEC + INT) 49→58 (+9: `CONREQ-TC-013c/034/035/036/037`, `CONREQ-TC-INT-013/014/015/016`); Flutter 16→17 (+1: `CONREQ-FL-17`; rewrite tại chỗ của `CONREQ-TC-006`/`CONREQ-FL-01` không đổi tổng số ID). **Tổng cộng 65→75 test case** (38 TC + 4 SEC + 16 INT + 17 FL). |
| `2026-07-16` | `AI Agent — Technical Architect` | v1.5 — **Đồng bộ theo TDS v1.5 (User review v1.4, 3 vấn đề còn lại)** — vẫn `Status: Draft`. **Vấn đề 1 (DirectChat vẫn TOCTOU):** `CONREQ-TC-035` **viết lại hoàn toàn** theo signature mới `assertConversationWritable(ExpertProfile lockedExpertProfile)` (không còn nhận `DirectConversation`, không còn mock `findByUserId` nội bộ) + assertion mới `Mockito.verifyNoInteractions(expertProfileRepository)` chứng minh policy không tự query (bắt AP-AI-015/AP-AI-016). Thêm 3 integration test lock-protocol ordering-controlled (mirror cấu trúc `CONREQ-TC-INT-013`, `CountDownLatch` thật, KHÔNG xác suất): `CONREQ-TC-INT-017` (`findOrCreate` vs `setTrustStatus`/`rejectExpert`, 2 scenario), `CONREQ-TC-INT-018` (`sendMessage` vs `setTrustStatus`, 2 scenario), `CONREQ-TC-INT-019` (`initiateCall` — đại diện cho `markRinging`/`answer`/`decline`/`end`-cancellable — vs `setTrustStatus`, 2 scenario, tham số hóa `SUSPENDED`/`REVOKED`). Thêm `CONREQ-TC-INT-020` — 2 test: Test A chứng minh answered-call cleanup KHÔNG bị block dù admin đang giữ lock CÙNG LÚC (đo bằng thời gian thực thi, không suy đoán — oracle phân biệt "có lock" vs "không lock"); Test B (regression) chứng minh nhánh cancellable VẪN bị chặn đúng. **Vấn đề 2 (ADR-008/009 Open):** TDS-02 thêm oracle row xác nhận `ADR-CONREQ-008/009` nay `Status: Accepted`; không cần test case mới (oracle của `CONREQ-TC-001`/`FX-006`/`CONREQ-TC-015`/`CONREQ-TC-016` được re-confirm khớp quyết định cuối, không đổi giá trị số). **Vấn đề 3 (read-contract quá rộng):** thêm `CONREQ-TC-038` (unit, `assertIsParticipant` — behavior matrix chính xác theo TỪNG bước: Mother luôn qua kể cả khi Expert mất verification/trust; Expert-participant chỉ gate bởi verification, KHÔNG BAO GIỜ bởi trust; not-participant bị chặn) + `CONREQ-TC-INT-021` (integration, cùng matrix ở tầng `getConversation`/`getTimeline`/`markRead` thật, VÀ chứng minh write/call bị chặn trong CÙNG fixture để xác lập ranh giới đọc/ghi — không chỉ dựa vào final-state suy luận). **Cập nhật hỗ trợ:** §1.1 constraints thêm C17/C18; TDS-02 thêm 3 oracle row mới (code thật `DirectMessageServiceImpl`/`ConversationCallServiceImpl`, `V1__init_schema.sql:1524` UNIQUE constraint, ADR-008/009 Accepted); Logic Issues Resolved (§2) thêm `L5` (TOCTOU v1.4→v1.5); TDS-03 thêm `TC-COND-042..048`; RGR Tracker + Red Gate Verification (§5/§5.1) thêm rows cho `CONREQ-TC-038`, `INT-017..021` — ghi rõ `CONREQ-TC-038`/`INT-021`/`INT-020`-Test-A là test XÁC NHẬN hành vi có sẵn đúng (PASS-ngay-không-qua-RED là kỳ vọng ĐÚNG, không phải vi phạm Red Gate), khác với `CONREQ-TC-035`/`INT-017/018/019`/`INT-020`-Test-B PHẢI thấy FAIL thật trước khi sửa; Entry/Exit Criteria (§6) thêm 4 DoD item mới; Rollback Plan (§7) bổ sung `git checkout` cho `DirectMessageServiceImpl.java`/`ConversationCallServiceImpl.java` + toàn bộ thư mục test `directchat/` mới (xoá dòng "Gap vẫn OPEN" không rõ nghĩa còn sót từ template); AP table (§8) thêm `AP-AI-016` (DirectChat TOCTOU Shortcut) + `AP-AI-017` (Overbroad Read-Contract Claim). Grep sweep xác nhận không còn `assertExpertVerified`/`không có TOCTOU`/`read-only là đủ`/"Open" (ngoài CHANGELOG và các câu trích dẫn tường minh mô tả điều PHẢI tránh) trong nội dung normative đang active. Tổng test case: backend (unit/policy/mapper + SEC + INT) 58→64 (+6: `CONREQ-TC-038` mới, `CONREQ-TC-INT-017/018/019/020/021` mới; rewrite tại chỗ của `CONREQ-TC-035` không đổi tổng số ID); Flutter không đổi (17, không có thay đổi mobile vòng này). **Tổng cộng 75→81 test case** (39 TC + 4 SEC + 21 INT + 17 FL — đếm chính xác qua `grep -c`). |
| `2026-07-16` | `AI Agent — Technical Architect` | v1.6 — **Đồng bộ TDS v1.6, vẫn `Status: Draft`.** Sửa create TOCTOU bằng 3 integration test mới `CONREQ-TC-INT-022/023/024`: moderation giữ Expert lock trước → create key mới bị `CONREQ-002` và zero side effect; create giữ lock trước → request PENDING commit rồi moderation chạy sau là hợp lệ; idempotent retry sau trust loss → HTTP 200 cùng resource id, `created=false`, không lock Expert và không insert/event/notification/audit lần hai. Các test dùng Testcontainers PostgreSQL, `TransactionTemplate`, `ExecutorService/Future`, `CountDownLatch`/barrier và test-only synchronization hook nếu cần; không race xác suất. Viết lại `CONREQ-TC-INT-020`: Test A dùng `trustLockAcquired`, `releaseTrust`, `endCompleted` để chứng minh answered cleanup hoàn thành/chuyển `ENDED` trước khi trust lock được nhả; Test B xác nhận cancellable branch đã bắt đầu lock rồi vẫn chưa hoàn thành trước release, sau trust commit SUSPENDED/REVOKED thì bị `DCC-010`, không transition/event/audit. Không dùng milliseconds/benchmark/sleep/`assertTimeout` làm business oracle; timeout rộng chỉ chống treo suite; mọi thread/transaction được cleanup trong `finally`. Tổng `84` test (`39 TC + 4 SEC + 24 INT + 17 FL`). ADR-CONREQ-008/009 vẫn `Accepted`. |
| `2026-07-16` | `User / AI Agent — Amelia (Dev Agent)` | v1.6 approval gate — User explicitly approved the finalized v1.6 Test-Spec for implementation. Status changed from `Draft` to `Approved`; DPO sign-off remains pending and the PDPA/privacy test obligations remain blocking. |
| `2026-07-16` | `AI Agent — Amelia (Dev Agent)` | v1.7 — **Post-implementation truthful sync; Status remains `Approved`.** All `84` contract IDs are mapped to implemented tests, with related cases consolidated into executable test methods/sub-cases rather than one Java/Dart method per document ID. Verification: scoped backend feature suite `111/111` passed; clean PostgreSQL Flyway run passed with `80` migrations; complete Flutter suite `89/89` passed, including `17/17` feature-focused tests; backend package build and Dart formatting passed. Deterministic concurrency coverage includes create-vs-moderation, accept-vs-moderation/accept/cancel, DirectChat find-or-create/message/call writes, and answered/cancellable `end()` branches using independent transactions/threads and latches. Full repository Maven suite remains red only on unrelated baseline failures; `flutter analyze` crashed before diagnostics; coverage ≥80% and universal retained RED evidence are therefore not claimed. |

---

## MỤC LỤC

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

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `CONREQ-GAP-0001` |
| **Module** | `Expert Consultation Requests — consultation (new), notification (sibling)` |
| **Spec gốc** | `CB-CONREQ-IMP-001` |
| **Priority** | 🔴 P0 |
| **Sprint** | `Phase 1 — Audit + Spec review` |
| **Milestone** | `M3 Alpha` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA (Luật 91/2025)` |
| **Upstream Dependencies** | `expert.entity.ExpertProfile`, `expert.verificationstatus.VerificationStatus`, `directchat.service.IDirectConversationService` |
| **Downstream Consumers** | Mobile MOTHER app, Mobile EXPERT app |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-CONREQ-IMP-001 TDS §15.1` (C1-C18) |
| **Constraints Injected** | C1–C18 từ v1.5; C19 (create key mới lock Expert + double-check key trước eligibility/insert); C20 (existing retry ổn định sau trust loss, HTTP 200 và zero side effect) |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> Không có spec cũ nào (UC75/UC202) được dùng làm nguồn cho module này — cả 2 tự bịa bảng `consultations`/enum `consultation_status` không tồn tại (xác nhận qua audit trực tiếp `V1__init_schema.sql`). Bảng dưới ghi các sai lệch đã phát hiện giữa các nguồn khác nhau và cách test case này xử lý.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | UC103/UC104/UC238/UC143 Draft dùng literal `verificationStatus == 'VERIFIED'` | `VerificationStatus` enum thật: `PENDING, UNDER_REVIEW, APPROVED, REJECTED, SUSPENDED, EXPIRED` — không có `VERIFIED` | Mọi test factory dùng `VerificationStatus.APPROVED`; có 1 test case riêng (CONREQ-TC-006) khẳng định `SUSPENDED`/`REJECTED`/`PENDING`/`UNDER_REVIEW`/`EXPIRED` đều bị từ chối, `APPROVED` là giá trị hợp lệ duy nhất |
| L2 | UC143 Draft: application-level check-then-update, không có DB-atomic guard | ADR-CONREQ-003 yêu cầu `UPDATE ... WHERE status = 'PENDING'` atomic | CONREQ-TC-INT-004/005 chạy 2 thread thật gọi `accept`/`cancel` đồng thời, assert chỉ 1 thắng |
| L3 | Không có business rule nào (SRS/BusinessRules rỗng) quy định số giờ expiry hay bắt buộc lý do reject | ADR-CONREQ-008/009 chọn mặc định 48h (config) + reason optional | Test dùng giá trị config thật (`carebridge.consultation-request.expiry-hours`, mặc định 48) làm oracle, không hardcode số giờ trong assertion logic ngoài việc đọc từ config bean cùng cách production đọc |
| L4 | `ConsultationNotificationService` cũ đồng bộ, không idempotent | ADR-CONREQ-006/007 dùng outbox pattern riêng (`ConsultationRequestNotificationServiceImpl` + `ConsultationRequestNotificationWriter`) | CONREQ-TC-INT-008 gọi listener 2 lần với cùng event, assert chỉ 1 `NotificationRecord` được tạo (nhờ `uq_notification_records_consultation_request`) |
| L5 (v1.5) | v1.4 chỉ mở rộng predicate `isEligibleForConsultation` cho `DirectConversationPolicyImpl` nhưng `findOrCreate`/`assertConversationWritable` vẫn đọc `ExpertProfile` bằng `findById`/`findByUserId` KHÔNG lock — TOCTOU thật với `setTrustStatus`/`approveExpert`/`rejectExpert` (đều dùng `findByIdForUpdate`) | ADR-CONREQ-013 (v1.5) yêu cầu `findByUserIdForUpdate` mới + `assertConversationWritable(ExpertProfile lockedExpertProfile)` (signature đổi) cho `findOrCreate`/`sendMessage`/`initiateCall`/`markRinging`/`answer`/`decline`/`end`(cancellable) | `CONREQ-TC-INT-017/018/019` (2 ordering scenario mỗi test, latch thật) chứng minh lock+check luôn xảy ra TRƯỚC ghi; `CONREQ-TC-035` viết lại theo signature mới |
| L6 (v1.6) | v1.5 vẫn mô tả `create()` là snapshot-read không TOCTOU, trong khi create key mới là write decision có thể insert sau khi moderation đã commit Expert ineligible | ADR-CONREQ-004/005 v1.6: existing retry return trước lock; key mới `findByIdForUpdate` → double-check key → eligibility check → insert | `CONREQ-TC-INT-022/023/024` dùng transaction thật + latch/barrier để chứng minh cả hai ordering và retry-after-trust-loss |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
consultation + notification (sibling) bao gồm các layer:
├── Domain (ConsultationRequestStatus, ConsultationRequestDomainEvent — pure)
├── Repository (ConsultationRequestRepository, ConsultationRequestWriter — Testcontainers PostgreSQL)
├── Policy (ConsultationRequestPolicy — Mockito unit)
├── Service (ConsultationRequestServiceImpl, ConsultationRequestNotificationServiceImpl — Mockito unit + Testcontainers integration cho race)
├── Controller (ConsultationRequestController — @WebMvcTest, mock Service)
└── Mobile (Flutter widget/unit — ExpertRequestQueueScreen, ConsultationRequestFormScreen, MyConsultationRequestsScreen, fcm_service.dart routing)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `CB-CONREQ-IMP-001` §3 (ADR-CONREQ-001..010) | Kiến trúc, concurrency, notification, scope boundary |
| `CB-CONREQ-IMP-001` §9-10 (API + Error Codes) | Request/response shape, mã lỗi |
| `CB-CONREQ-IMP-001` §14 (Authorization Matrix) | Test case per role × endpoint |
| `expert/verificationstatus/VerificationStatus.java` (code thật) | Oracle cho literal `APPROVED` |
| `directchat/service/impl/DirectConversationServiceImpl.java` (code thật) | Oracle cho `findOrCreate` signature/hành vi khi accept |
| `05_Development/CareBridgeAPI/.../V20260716010800__...sql` (code thật) | Oracle cho pattern partial unique index idempotency |
| PDPA | Minimization test cho FCM payload/log |
| `GlobalExceptionHandler.java`, `ErrorResponse.java`, `ErrorDetail.java` (code thật) | Oracle cho error response flat shape thật (Issue 1) |
| `directchat/exception/DirectChatException.java`, `directchat/service/impl/DirectMessageServiceImpl.java` (code thật) | Oracle cho pattern factory-method exception + idempotent check-then-insert với `clientMessageId`/`assertSameIdempotentPayload` (Issue 6), pattern IDOR-safe reuse 1 code cho nhiều nguyên nhân (`DCC-006`, Issue 2) |
| `notification/service/FcmService.java`, `FirebaseFcmServiceImpl.java`, `FcmServiceImpl.java` (code thật) | Oracle cho việc FCM hiện KHÔNG có data payload — Issue 3 overload phải additive |
| `notification/dto/NotificationRecordResponse.java`, mobile `notification_center_screen.dart`/`notifications_screen.dart`/`notification_detail_screen.dart` (code thật) | Oracle cho contract `type`/`referenceType` thật — Issue 4 |
| `expert/service/impl/ExpertProfileServiceImpl.java` (code thật) | Oracle cho `setTrustStatus`/`rejectExpert` — dùng trong test race Issue 5 |
| `NotificationRecordWriterConcurrencyIntegrationTest.java`, `OtpRaceConditionIntegrationTest.java` (code thật) | Oracle cho cấu trúc test 2-thread Testcontainers thật (`CountDownLatch`/`ExecutorService`) — mirror cho `CONREQ-TC-INT-010` |
| `V1__init_schema.sql` (`audit_logs.actor_user_id` nullable), `05_Development/CareBridgeMobileApp/lib/features/home/screens/expert_home_shell.dart:86` (code thật) | Oracle cho Issue 8 (nullable actor) và Issue 9 (`NavigationBar` thật) |
| `V20260710000000__add_trust_status_to_expert_profiles.sql` (code thật — `trust_status NOT NULL DEFAULT 'ACTIVE'` + `CHECK`), `expert/entity/ExpertProfile.java` (code thật) | Oracle cho v1.4 predicate `isEligibleForConsultation` — xác nhận không có case `trustStatus=null` khả dĩ với dữ liệu hợp lệ |
| `expert/repository/ExpertProfileRepository.java` (code thật, 4 query `searchDirectory`/`findVerifiedPublic`/`findVerifiedBySpecialty`/`findApprovedSpecialties`) | Oracle cho `CONREQ-TC-INT-014` (v1.4, ADR-CONREQ-012 — directory trust filter) |
| `directchat/policy/DirectConversationPolicyImpl.java`, `directchat/service/impl/ConversationCallServiceImpl.java` (code thật — ADR-DCC-007 §2 answered-call-end exemption), `directchat/exception/DirectChatException.java` (code thật) | Oracle cho `CONREQ-TC-034/035`, `CONREQ-TC-INT-015/016` (v1.4, ADR-CONREQ-013 — cross-domain directchat eligibility) |
| `expert/dto/response/ExpertProfileResponse.java`/`ExpertProfileDetailResponse.java`, mobile `expert_public_profile_screen.dart:85,142-163` (code thật) | Oracle cho `CONREQ-TC-036`, `CONREQ-FL-01` (rewrite), `CONREQ-FL-17` (v1.4, Task 10 — contract `isConsultationEligible`) |
| `directchat/service/impl/DirectConversationServiceImpl.java` (dòng 90-95, `findOrCreate` — code thật, v1.5 audit), `directchat/service/impl/DirectMessageServiceImpl.java` (dòng 88-94, `sendMessage`), `directchat/service/impl/ConversationCallServiceImpl.java` (dòng 70-218, `initiateCall`/`markRinging`/`answer`/`decline`/`end`) | Oracle cho v1.5 lock protocol — xác nhận CHÍNH XÁC dòng nào hiện gọi `assertConversationWritable(conversation)`/`findById` không lock, dùng làm oracle cho `CONREQ-TC-INT-017/018/019/020` |
| `V1__init_schema.sql:1524` (`expert_profiles_user_id_key UNIQUE (user_id)`) | Oracle xác nhận `findByUserIdForUpdate` khóa CÙNG 1 hàng vật lý với `findByIdForUpdate` cho cùng 1 expert — cơ sở cho assertion "2 lock cùng serialize" trong `CONREQ-TC-INT-017/018/019` |
| `ExpertConsultationRequests_TDS.md` ADR-CONREQ-008/009 (v1.5 — `Status: Accepted`, `Deciders: User/Product decision`) | Oracle cho việc xóa bỏ mọi test comment/oracle-note còn ghi "Open"/"chờ Product xác nhận" — `CONREQ-TC-001`/`FX-006` phải đọc đúng cùng config source, không hardcode độc lập |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Mother tạo request hợp lệ | `ConsultationRequestServiceImpl.create` | `CONREQ-TC-001` |
| TC-COND-002 | Validation boundary (topic/description/window) | `CreateConsultationRequestRequest` | `CONREQ-TC-002, 003, 004` |
| TC-COND-003 | Expert không tồn tại / không APPROVED | `create` | `CONREQ-TC-005, 006` |
| TC-COND-004 | Double-submit / idempotent retry (Issue 6 — theo `clientRequestId`, không phải "duplicate PENDING") | `ConsultationRequestWriter.insertIfAbsent` | `CONREQ-TC-007, CONREQ-TC-INT-001` |
| TC-COND-005 | List/filter/pagination, không N+1 | `listMine`/`listAssigned` | `CONREQ-TC-008, 009, CONREQ-TC-INT-002` |
| TC-COND-006 | Ownership / IDOR | `getById`, policy | `CONREQ-TC-010, 011, CONREQ-TC-SEC-001` |
| TC-COND-007 | Accept happy path + DirectConversation link | `accept` | `CONREQ-TC-012, CONREQ-TC-INT-003` |
| TC-COND-008 | Accept khi expert bị revoke | `accept` | `CONREQ-TC-013` |
| TC-COND-009 | Reject (có/không lý do) | `reject` | `CONREQ-TC-014, 015` |
| TC-COND-010 | Cancel hợp lệ / không hợp lệ | `cancel` | `CONREQ-TC-016, 017` |
| TC-COND-011 | Invalid state transition (đã terminal) | `tryTransition` (dùng chung cho reject/cancel/accept — v1.3) | `CONREQ-TC-018, CONREQ-TC-013b` |
| TC-COND-012 | Concurrent accept (race) | `findByIdForUpdate` + `tryTransition` (v1.3) | `CONREQ-TC-INT-004` |
| TC-COND-013 | Concurrent accept/cancel (race) | `findByIdForUpdate` + `tryTransition` (accept) / `tryTransition` (cancel) | `CONREQ-TC-INT-005` |
| TC-COND-014 | Expiry job (per-row) | `ConsultationRequestRepository.findExpiredIds` + `tryTransition` | `CONREQ-TC-019, CONREQ-TC-INT-006` |
| TC-COND-015 | Notification: recipient resolution, payload minimization, deep-link | `ConsultationRequestNotificationServiceImpl` | `CONREQ-TC-020, 021, 022` |
| TC-COND-016 | Notification idempotency (listener chạy lặp) | `ConsultationRequestNotificationWriter` | `CONREQ-TC-INT-008` |
| TC-COND-017 | FCM exception không rollback business transaction | `ConsultationRequestServiceImpl.accept` + listener | `CONREQ-TC-INT-007` |
| TC-COND-018 | Authorization theo role cho từng endpoint | Controller | `CONREQ-TC-SEC-001..004` |
| TC-COND-019 | Không lộ raw exception | `GlobalExceptionHandler` | `CONREQ-TC-023` |
| TC-COND-020 | Migration chạy trên Testcontainers PostgreSQL | Flyway | `CONREQ-TC-INT-009` |
| TC-COND-021 | Mobile: CTA/form/list/detail/double-submit/queue/badge/notification/no-nested-nav/stale-response/notification-center | Flutter | `CONREQ-FL-01..16` |
| TC-COND-022 | `getById` với id không tồn tại | `getById` | `CONREQ-TC-024` |
| TC-COND-023 | `pendingSummary()` trả đúng `pendingCount` (Issue 11 — renamed từ `unreadSummary`) | `pendingSummary` | `CONREQ-TC-025` |
| TC-COND-024 | Notification: 2 event khác nhau cho cùng recipient+request đều được gửi, không bị chặn nhầm bởi index idempotency | `ConsultationRequestNotificationWriter` | `CONREQ-TC-INT-008` |
| TC-COND-025 | IDOR regression: not-found và not-participant có cùng HTTP/error/message/details; bỏ qua `path`/`timestamp` per-request (Issue 2) | `ConsultationRequestPolicy`, `getById`/`accept`/`reject`/`cancel` | `CONREQ-TC-010, CONREQ-TC-026` |
| TC-COND-026 | FCM data payload: producer gửi đúng `{type, requestId}`, không lộ `topic`/`description` (Issue 3) | `ConsultationRequestNotificationServiceImpl`, `FcmService.sendWithRetry(...,data,...)` | `CONREQ-TC-022, CONREQ-TC-027` |
| TC-COND-027 | `clientRequestId` idempotency: same key+payload → retry an toàn; same key+payload khác → 409; key khác → request mới dù cùng expert (Issue 6, rule "1 PENDING/cặp" đã bị rút lại) | `ConsultationRequestServiceImpl.create`, `ConsultationRequestWriter` | `CONREQ-TC-007, CONREQ-TC-028, CONREQ-TC-029, CONREQ-TC-030, CONREQ-TC-INT-001, CONREQ-TC-INT-012` |
| TC-COND-028 | Accept TOCTOU: row-lock (`ExpertProfileRepository.findByIdForUpdate`, cross-domain) đóng race expert-revoke-tại-accept thật, 2 scenario ordering-controlled (v1.3 — Issue 1 vòng 2) | `ConsultationRequestServiceImpl.accept`, `ExpertProfileRepository.findByIdForUpdate` | `CONREQ-TC-013, CONREQ-TC-013b, CONREQ-TC-INT-010` |
| TC-COND-029 | Accept losing transaction rollback đầy đủ — DirectConversation MỚI insert trong tx thua phải rollback (v1.3 — controlled-ordering race thật, Issue 4 vòng 2) | `ConsultationRequestServiceImpl.accept` | `CONREQ-TC-INT-011` |
| TC-COND-030 | `REQUEST_EXPIRED` actor attribution: `actorUserId=null, actorType=SYSTEM` (Issue 8) | `ConsultationRequestExpiryJob`, domain event | `CONREQ-TC-019, CONREQ-TC-INT-006` |
| TC-COND-031 | Counterpart field role-correct: Mother thấy expert, Expert thấy mother, không N+1 (Issue 10) | `ConsultationRequestMapper` | `CONREQ-TC-011, CONREQ-TC-INT-002` |
| TC-COND-032 | Preferred window both-or-neither (không cho phép 1 phía null) (Issue 12) | `CreateConsultationRequestRequest` validator, `chk_consultation_requests_window` | `CONREQ-TC-004, CONREQ-TC-INT-009` |
| TC-COND-033 | HTTP 201 (created) vs 200 (idempotent retry) contract — Controller đọc `result.created()`, không tự suy đoán (v1.3 — Issue 3 vòng 2) | `IConsultationRequestService.create`, `ConsultationRequestController.create` | `CONREQ-TC-030, CONREQ-TC-031, CONREQ-TC-032, CONREQ-TC-INT-012` |
| TC-COND-034 | Create eligibility matrix đầy đủ: APPROVED+ACTIVE hợp lệ; APPROVED+SUSPENDED/REVOKED bị chặn; mọi verification khác APPROVED bị chặn bất kể trust (v1.4 — Task 3) | `ConsultationRequestPolicy.assertExpertEligibleForConsultation` | `CONREQ-TC-006` |
| TC-COND-035 | Accept eligibility dưới row-lock: APPROVED+SUSPENDED/REVOKED bị chặn `CONREQ-004`, zero side effect (v1.4 — Task 4) | `ConsultationRequestServiceImpl.accept` | `CONREQ-TC-013c` |
| TC-COND-036 | Concurrency accept vs `setTrustStatus`, 2 scenario ordering-controlled (v1.4 — Task 5) | `ConsultationRequestServiceImpl.accept`, `ExpertProfileServiceImpl.setTrustStatus` | `CONREQ-TC-INT-013` |
| TC-COND-037 | Public directory/verified-list chỉ trả APPROVED+ACTIVE; search/specialty/pagination không bypass trust filter (v1.4 — Task 6, ADR-CONREQ-012) | `ExpertProfileRepository` (4 query) | `CONREQ-TC-INT-014` |
| TC-COND-038 | `DirectConversation.findOrCreate` (cross-domain, kể cả đường trực tiếp) bị chặn khi expert mất eligibility; existing conversation vẫn xem được (v1.4 — Task 7, ADR-CONREQ-013) | `DirectConversationPolicyImpl.assertExpertEligibleForConsultation`, `DirectConversationServiceImpl.findOrCreate` | `CONREQ-TC-034, CONREQ-TC-INT-015` |
| TC-COND-039 | Gửi tin nhắn/khởi tạo call mới bị chặn khi expert mất eligibility; kết thúc call đã `ANSWERED` không bị ảnh hưởng (v1.4 — Task 8, ADR-CONREQ-013) | `DirectConversationPolicyImpl.assertConversationWritable`, `ConversationCallServiceImpl.end` | `CONREQ-TC-035, CONREQ-TC-INT-016` |
| TC-COND-040 | Reject/cancel/expiry KHÔNG bị gate bởi eligibility — regression tường minh, zero interaction với `ExpertProfileRepository` (v1.4 — Task 9) | `ConsultationRequestServiceImpl.reject/cancel`, `ConsultationRequestExpiryJob` | `CONREQ-TC-037` |
| TC-COND-041 | Mobile: CTA "Trò chuyện"/"Yêu cầu tư vấn" gate đúng theo `isConsultationEligible` (không phải `verificationStatus` một mình); DTO mapper set field đúng (v1.4 — Task 10) | `ExpertProfileMapper`, `ExpertPublicProfileScreen` | `CONREQ-TC-036, CONREQ-FL-01, CONREQ-FL-17` |
| TC-COND-042 | `findOrCreate` lock-protocol race: admin `setTrustStatus`/`rejectExpert` thắng lock trước → `findOrCreate` bị chặn generic, zero side effect; `findOrCreate` thắng lock trước → commit giữ nguyên, trust action sau đó hợp lệ (v1.5 — Problem 1, ADR-CONREQ-013) | `DirectConversationServiceImpl.findOrCreate`, `ExpertProfileRepository.findByIdForUpdate` | `CONREQ-TC-INT-017` |
| TC-COND-043 | `sendMessage` lock-protocol race: cùng 2 ordering scenario, chứng minh `findByUserIdForUpdate` chạy TRƯỚC insert message (v1.5 — Problem 1) | `DirectMessageServiceImpl.sendMessage`, `ExpertProfileRepository.findByUserIdForUpdate` | `CONREQ-TC-INT-018` |
| TC-COND-044 | `initiateCall` (representative call-start) lock-protocol race: cùng 2 ordering scenario; tham số hóa thêm `REVOKED` nếu test structure cho phép (v1.5 — Problem 1) | `ConversationCallServiceImpl.initiateCall` | `CONREQ-TC-INT-019` |
| TC-COND-045 | Answered-call cleanup exemption dưới trust-loss: seed `ANSWERED`, suspend/revoke expert, `end()` vẫn thành công KHÔNG lock/gate; regression cùng fixture — nhánh cancellable VẪN bị chặn (v1.5 — Problem 1, ADR-DCC-007 §2) | `ConversationCallServiceImpl.end` | `CONREQ-TC-INT-020` |
| TC-COND-046 | Read/mark-read behavior matrix chính xác: Mother luôn đọc/mark-read được bất kể trạng thái Expert; Expert-participant đọc/mark-read được khi APPROVED+SUSPENDED/REVOKED nhưng bị chặn khi verification không APPROVED; write/call bị chặn trong CÙNG fixture để chứng minh ranh giới đọc/ghi; `assertIsParticipant`/`markRead` không dùng `findByIdForUpdate` (v1.5 — Problem 3, ADR-CONREQ-013 Behavior Matrix) | `DirectConversationPolicyImpl.assertIsParticipant`, `DirectConversationServiceImpl.markRead/getConversation/getTimeline` | `CONREQ-TC-038, CONREQ-TC-INT-021` |
| TC-COND-047 | `assertConversationWritable` signature mới (`ExpertProfile lockedExpertProfile`, không tự query) — unit test xác nhận policy KHÔNG gọi `expertProfileRepository` bên trong (v1.5 — Problem 1) | `DirectConversationPolicyImpl.assertConversationWritable` | `CONREQ-TC-035` (viết lại) |
| TC-COND-048 | ADR-CONREQ-008/009 `Status: Accepted` — test đọc `expiresAt` từ CÙNG config source production dùng (`@Value` field), không hardcode độc lập; reject reason optional max 500; cancel chỉ Mother owner + chỉ PENDING (v1.5 — Problem 2) | `ConsultationRequestServiceImpl` (`expiryHours` field), `RejectConsultationRequestRequest`, `ConsultationRequestPolicy.assertCanCancel` | `CONREQ-TC-001, CONREQ-TC-015, CONREQ-TC-016` (oracle re-confirmed, không cần TC mới) |
| TC-COND-049 | Create key mới vs moderation/trust dùng cùng Expert row-lock, 2 ordering scenario; retry same-key/same-payload sau trust loss vẫn trả existing HTTP 200, zero side effect | `ConsultationRequestServiceImpl.create`, `ExpertProfileRepository.findByIdForUpdate`, `ExpertProfileServiceImpl.rejectExpert/setTrustStatus` | `CONREQ-TC-INT-022, CONREQ-TC-INT-023, CONREQ-TC-INT-024` |

### TDS-04 — Test Techniques

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | `topic`/`description` length, window ordering | Phân vùng hợp lệ/không hợp lệ |
| Boundary Value Analysis | `topic` = 200/201 ký tự, `description` = 2000/2001 | Biên `@Size(max=...)` |
| State Transition Testing | `ConsultationRequestStatus` FSM | 5 trạng thái, mỗi transition hợp lệ + bất hợp lệ |
| Concurrency / Race Testing | `tryTransition`, `insertIfAbsent` | Yêu cầu bắt buộc trong brief |
| Error Guessing / Security | IDOR, role bypass, injection trong `topic`/`description` | OWASP A01/A03 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | Mother user (`role=MOTHER`), UUID cố định | Happy path requester |
| `FX-002` | DB seed | Expert user + `ExpertProfile{verificationStatus=APPROVED}` | Happy path expert |
| `FX-003` | DB seed | `ExpertProfile{verificationStatus=SUSPENDED}` | Reject-at-create / revoked-at-accept |
| `FX-004` | DB seed | `ConsultationRequest{status=PENDING, expiresAt=now-1h}` | Expiry job |
| `FX-005` | DB seed | `ConsultationRequest{status=ACCEPTED}` | Invalid-transition / cancel-after-accept tests |
| `FX-006` | env | `carebridge.consultation-request.expiry-hours=48` (default) | Oracle cho `expiresAt` tính toán |
| `FX-007` | JWT | `{sub: motherUserId, role: MOTHER}` / `{sub: expertUserId, role: EXPERT}` | Auth context cho `@WebMvcTest`/E2E |
| `FX-008` | value | `clientRequestId = UUID.randomUUID()` per test (Issue 6) — mỗi test case sinh key riêng, KHÔNG chia sẻ giữa test case khác nhau (Props Isolation) | Oracle cho idempotency key tests |
| `FX-009` | DB seed (v1.4) | `ExpertProfile{verificationStatus=APPROVED, trustStatus=SUSPENDED}` và `{verificationStatus=APPROVED, trustStatus=REVOKED}` | Eligibility matrix (`CONREQ-TC-006/013c/034/035`), trust-race (`CONREQ-TC-INT-013`), directory filter (`CONREQ-TC-INT-014`) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ConsultationRequestTestFactory.java
class ConsultationRequestTestFactory {

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID EXPERT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    static ExpertProfile makeApprovedExpertProfile() {
        ExpertProfile p = new ExpertProfile();
        p.setExpertProfileId(EXPERT_PROFILE_ID);
        p.setUserId(EXPERT_USER_ID);
        p.setVerificationStatus(VerificationStatus.APPROVED);
        p.setTrustStatus(TrustStatus.ACTIVE); // v1.4 — default eligible on both axes
        return p;
    }

    static ExpertProfile makeExpertProfile(VerificationStatus status) {
        ExpertProfile p = makeApprovedExpertProfile();
        p.setVerificationStatus(status);
        return p;
    }

    // v1.4 (Task 3/4/5/6/7/8) — the compound-predicate factory; every new eligibility test uses
    // this overload explicitly rather than relying on makeExpertProfile(VerificationStatus)'s
    // implicit trustStatus=ACTIVE default, so the test reads as testing BOTH axes on purpose.
    static ExpertProfile makeExpertProfile(VerificationStatus verification, TrustStatus trust) {
        ExpertProfile p = makeApprovedExpertProfile();
        p.setVerificationStatus(verification);
        p.setTrustStatus(trust);
        return p;
    }

    static CreateConsultationRequestRequest makeCreateRequest(Consumer<CreateConsultationRequestRequest> overrides) {
        CreateConsultationRequestRequest r = new CreateConsultationRequestRequest();
        r.setClientRequestId(UUID.randomUUID()); // Issue 6 — every test gets its own fresh key, never shared
        r.setExpertProfileId(EXPERT_PROFILE_ID);
        r.setTopic("Tư vấn dinh dưỡng dặm");
        r.setDescription("Bé 6 tháng biếng ăn, cần tư vấn thực đơn.");
        overrides.accept(r);
        return r;
    }

    static ConsultationRequest makePendingRequest(Consumer<ConsultationRequest> overrides) {
        ConsultationRequest req = ConsultationRequest.builder()
                .id(UUID.randomUUID())
                .requesterUserId(MOTHER_ID)
                .expertProfileId(EXPERT_PROFILE_ID)
                .clientRequestId(UUID.randomUUID()) // Issue 6
                .topic("Tư vấn dinh dưỡng dặm")
                .description("Bé 6 tháng biếng ăn.")
                .status(ConsultationRequestStatus.PENDING)
                .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        overrides.accept(req);
        return req;
    }
}
```

---

### CONREQ-TC-001 — Create succeeds with valid input (happy path)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.create()`
**Test File:** `src/test/java/com/carebridge/backend/consultation/service/ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-CONREQ-IMP-001` §9.2 (`POST /consultation-requests` 201 response shape), §6.1 (sequence diagram)

**Preconditions:**
- `FX-001` (Mother), `FX-002` (Expert APPROVED)

**Test Steps:**
1. Arrange: lookup key lần 1 → `Optional.empty()`; mock `ExpertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID)` → `makeExpertProfile(APPROVED, ACTIVE)`; lookup key lần 2 sau lock → `Optional.empty()`; mock `writer.insertIfAbsent(...)` → `new InsertResult(newId, true)`
2. Act: `service.create(makeCreateRequest(r -> {}), MOTHER_ID)`
3. Assert: response `status == "PENDING"`, `expiresAt == createdAt + 48h` (config default), `directConversationId == null`

**Expected Result (PASS):** `findByIdForUpdate` xảy ra trước eligibility check/insert; response đúng shape §9.2; writer gọi đúng 1 lần; `REQUEST_CREATED`/audit create đúng 1 lần.
**Expected Result (FAIL):** `NullPointerException`/status sai/`expiresAt` không đúng công thức.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-002 — Validation: topic blank → 400 VALIDATION_ERROR

**Severity:** `HIGH`
**Feature Under Test:** `CreateConsultationRequestRequest` (`@NotBlank topic`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/controller/ConsultationRequestControllerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1 DTO annotations, §9.2/§10 (Issue 1 — real `ErrorResponse` flat shape, `GlobalExceptionHandler.handleMethodArgumentNotValid`)

**Test Steps:**
1. `POST /api/v1/consultation-requests` với `topic=""`, JWT Mother hợp lệ, `clientRequestId` hợp lệ
2. Assert `400`, body **flat** `ErrorResponse` thật: `success==false`, `status==400`, `error=="VALIDATION_ERROR"` (KHÔNG phải `error.code=="CONREQ-001"` — code đó đã retire), `details[].field=="topic"`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-003 — Boundary: topic 200 ký tự OK, 201 ký tự FAIL

**Severity:** `MEDIUM`
**Feature Under Test:** `CreateConsultationRequestRequest.topic` `@Size(max=200)`
**Test File:** `ConsultationRequestControllerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1

**Test Steps:**
1. `topic` = 200 ký tự (kèm `clientRequestId` hợp lệ) → 201
2. `topic` = 201 ký tự → `400`, `error=="VALIDATION_ERROR"` (Issue 1 — không phải `CONREQ-001`)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-004 — Validation: preferred window both-or-neither (Issue 12)

**Severity:** `MEDIUM`
**Feature Under Test:** `CreateConsultationRequestRequest` cross-field validator
**Test File:** `ConsultationRequestControllerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-002, TC-COND-032`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1 (Issue 12 fix — both null hoặc cả 2 present + end > start, khớp `chk_consultation_requests_window`)

**Test Steps (parametrized, 3 sub-case):**
1. `preferredWindowStart = T`, `preferredWindowEnd = T - 1h` (end <= start) → `400 VALIDATION_ERROR`
2. `preferredWindowStart = T`, `preferredWindowEnd = null` (1 phía null — v1.1 SAI cho phép qua, Issue 12 fix) → `400 VALIDATION_ERROR`
3. `preferredWindowStart = null`, `preferredWindowEnd = T` (phía còn lại null) → `400 VALIDATION_ERROR`

**Expected Result (FAIL — dấu hiệu lỗi cũ):** nếu validator vẫn dùng logic v1.1 (`start==null || end==null || end>start`), sub-case 2/3 sẽ **pass validation sai** (không throw) — test này phát hiện đúng regression đó.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-005 — Create fails: expert profile not found → 404 CONREQ-006

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.create()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-CONREQ-IMP-001` §10 `CONREQ-006`

**Test Steps:**
1. Mock key lookup lần 1 → empty; `expertProfileRepository.findByIdForUpdate(anyId)` → `Optional.empty()`
2. Act + assert: throws `ConsultationRequestException` với code `CONREQ-006`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-006 — Create fails for every non-eligible expert (verification, trust, hoặc cả hai) → 409 CONREQ-002 (v1.4 rewrite — Task 3, predicate hợp nhất)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestPolicy.assertExpertEligibleForConsultation()` (đổi tên từ `assertExpertRequestable`, v1.4)
**Test File:** `src/test/java/com/carebridge/backend/consultation/policy/ConsultationRequestPolicyTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-003, TC-COND-034`
**Oracle Source:** `expert/verificationstatus/VerificationStatus.java`, `expert/truststatus/TrustStatus.java` (enum thật) + `CB-CONREQ-IMP-001` ADR-CONREQ-005 v1.4 (predicate hợp nhất `isEligibleForConsultation`)

**Test Steps (parametrized, đầy đủ ma trận theo yêu cầu Task 3):**
1. `verification=APPROVED, trust=ACTIVE` → `assertExpertEligibleForConsultation(makeExpertProfile(APPROVED, ACTIVE))` → KHÔNG throw (hợp lệ)
2. `verification=APPROVED, trust=SUSPENDED` → throws `CONREQ-002` (v1.3 sẽ SAI cho pass — đây chính là gap Task 3 yêu cầu đóng)
3. `verification=APPROVED, trust=REVOKED` → throws `CONREQ-002`
4. Với mỗi `verification ∈ {PENDING, UNDER_REVIEW, REJECTED, SUSPENDED, EXPIRED}` (trust=ACTIVE, giữ cố định để cô lập biến): throws `CONREQ-002` — mọi verification khác `APPROVED` đều bị chặn bất kể trust
5. **Null-trust fail-closed (ghi chú, không cần seed thật):** `trust_status` cột DB là `NOT NULL DEFAULT 'ACTIVE'` kèm `CHECK (trust_status IN ('ACTIVE','SUSPENDED','REVOKED'))` (`V20260710000000__add_trust_status_to_expert_profiles.sql`, đã audit trực tiếp) — không có dữ liệu hợp lệ nào có `trustStatus=null`; không cần case test riêng vì không khả thi qua schema, nhưng nếu (giả thuyết) entity bị set `null` thủ công trong 1 unit test, `trustStatus == TrustStatus.ACTIVE` tự nhiên `false` (fail-closed) — có thể thêm 1 assertion nhanh xác nhận hành vi này của Java `==` nếu muốn, không bắt buộc.

**Expected Result (FAIL — dấu hiệu lỗi):** nếu code kiểm tra `"VERIFIED".equals(status.name())` (bug cũ UC103/UC143 Draft) → FAIL vì `APPROVED` bị từ chối sai. **(v1.4 mới)** Nếu code chỉ check `verificationStatus == APPROVED` mà KHÔNG check `trustStatus` (mirror nguyên trạng `DirectConversationPolicyImpl` cũ — chính là gap Task 3 yêu cầu đóng) → bước 2/3 sẽ FAIL vì không throw dù trust bị suspend/revoke.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-007 — Idempotent retry: cùng `clientRequestId` + cùng payload → trả về request đã có, không tạo mới, không publish lại (Issue 6 — thay thế hoàn toàn logic "1 PENDING/cặp" của v1.1)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.create()` khi `findByRequesterUserIdAndClientRequestId` tìm thấy row đã tồn tại
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-004, TC-COND-027`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-004 (v1.2 — clientRequestId, mirror `DirectMessageServiceImpl.sendMessage`'s early-return)

**Test Steps:**
1. Mock `repository.findByRequesterUserIdAndClientRequestId(MOTHER_ID, clientRequestId)` → `Optional.of(existingRequest)` (cùng `topic`/`description`/`expertProfileId` như request mới gửi)
2. Act: `service.create(makeCreateRequest(r -> r.setClientRequestId(existingRequest.getClientRequestId())), MOTHER_ID)`
3. Assert: trả về response của `existingRequest.getId()` (KHÔNG tạo id mới); `writer.insertIfAbsent` KHÔNG được gọi; `ApplicationEventPublisher.publishEvent` KHÔNG được gọi (retry không double-publish)
4. Đặt Expert hiện tại thành `APPROVED+SUSPENDED` hoặc `APPROVED+REVOKED`; verify `expertProfileRepository.findByIdForUpdate` KHÔNG được gọi và kết quả vẫn `created=false`, cùng id. Đây là retry của resource đã commit, không phải create mới.

**Expected Result (FAIL — dấu hiệu lỗi cũ đã bị User bác bỏ):** nếu code coalesce theo `(requester, expert, status=PENDING)` thay vì `clientRequestId` — tức là trả về MỘT request PENDING bất kỳ tới cùng expert bất kể `clientRequestId` có khớp hay không — test này FAIL vì nó seed `existingRequest` với `clientRequestId` cụ thể và request mới dùng đúng key đó; một implementation sai theo rule cũ vẫn "tình cờ" pass ở test đơn lẻ này nên xem thêm `CONREQ-TC-028` (regression 2 key khác nhau) để bắt lỗi này triệt để.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-028 — Idempotency key khác nhau → LUÔN tạo request mới, kể cả cùng cặp (mother, expert) (Issue 6 regression — chứng minh rule "1 PENDING/cặp" đã bị rút lại)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.create()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-027`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-004 ("Mother được phép có nhiều request PENDING khác nhau tới cùng 1 expert nếu `clientRequestId` khác nhau" — User đã explicit bác bỏ rule cũ)

**Test Steps:**
1. Seed 1 `ConsultationRequest` PENDING của `(MOTHER_ID, EXPERT_PROFILE_ID)` với `clientRequestId = key1`
2. Mock `findByRequesterUserIdAndClientRequestId(MOTHER_ID, key2)` → `Optional.empty()` (key2 ≠ key1, chưa từng dùng)
3. Mock `findByIdForUpdate(EXPERT_PROFILE_ID)` trả Expert eligible; lookup key2 lần 2 sau lock vẫn empty.
4. Act: `service.create(makeCreateRequest(r -> r.setClientRequestId(key2)), MOTHER_ID)` — cùng `expertProfileId` với request đã seed.
5. Assert: tạo THÀNH CÔNG 1 request PENDING mới, khác `id`; verify lock Expert xảy ra trước insert.

**Expected Result (FAIL — dấu hiệu regression):** nếu code còn check "đã có PENDING nào cho cặp (mother, expert) chưa" (rule cũ) trước khi cho tạo mới, test này sẽ FAIL vì nó throw lỗi hoặc trả về `existingId` sai thay vì tạo request thứ 2.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-029 — Idempotency conflict: cùng `clientRequestId`, payload khác → 409 CONREQ-009

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.create()` — `assertSameIdempotentPayload`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-027`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-004, mirror `DirectChatException.idempotencyConflict()`/`DCC-005`

**Test Steps:**
1. Mock `findByRequesterUserIdAndClientRequestId(MOTHER_ID, key)` → `Optional.of(existingRequest)` với `topic = "Tư vấn A"`
2. Act: `service.create(makeCreateRequest(r -> { r.setClientRequestId(key); r.setTopic("Tư vấn B — khác hẳn"); }), MOTHER_ID)`
3. Assert: throws `ConsultationRequestException` code `CONREQ-009`, HTTP `409`; KHÔNG tạo request mới, KHÔNG publish event

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-030 — Service: `create()` trả `CreateConsultationRequestResult` với `created` đúng giá trị (v1.3 — Issue 3 vòng 2)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.create()` — return type `CreateConsultationRequestResult`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-004, TC-COND-027`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1 `CreateConsultationRequestResult` (v1.3 fix)

**Test Steps (parametrized, 2 sub-case):**
1. **Request mới:** lookup key lần 1 empty; `findByIdForUpdate` trả eligible; lookup lần 2 empty; writer → `created=true`. Assert `created=true`, đúng id, event/audit create đúng 1 lần.
2. **Idempotent retry:** lookup lần 1 trả existing cùng payload. Assert `created=false`, đúng id, không gọi `findByIdForUpdate`, writer/event/audit/notification không chạy.

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-031 — Controller: `create()` trả `201 Created` khi `result.created() == true`

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestController.create()`
**Test File:** `ConsultationRequestControllerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1/§9.2 (v1.3 — Controller đọc `result.created()`, không tự suy đoán)

**Test Steps:**
1. Mock `service.create(...)` → `new CreateConsultationRequestResult(responseDto, true)`
2. `POST /api/v1/consultation-requests` (JWT Mother hợp lệ, body hợp lệ)
3. Assert HTTP status `== 201`, body khớp `responseDto`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-032 — Controller: `create()` trả `200 OK` khi `result.created() == false` (idempotent retry)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestController.create()`
**Test File:** `ConsultationRequestControllerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-004, TC-COND-027`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1/§9.2

**Test Steps:**
1. Mock `service.create(...)` → `new CreateConsultationRequestResult(existingResponseDto, false)`
2. `POST /api/v1/consultation-requests` (cùng `clientRequestId` đã dùng trước đó)
3. Assert HTTP status `== 200` (KHÔNG phải `201`), body chứa đúng `id` của request đã tồn tại

**Expected Result (FAIL — dấu hiệu regression):** nếu controller luôn trả `201` bất kể `created`, hoặc tự suy đoán qua `createdAt`/timestamp thay vì đọc field `created`, test này FAIL — đúng lỗi User đã nêu ("Controller không biết resource vừa được tạo hay là retry").
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-008 — List mine: chỉ trả request của đúng requester, đúng filter status

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.listMine()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.2 `findByRequesterUserIdAndStatus`

**Test Steps:**
1. Mock repository trả 2 record cho `MOTHER_ID`, filter `status=PENDING`
2. Assert repository được gọi với đúng `requesterUserId`, `status`, `pageable`; response là `ConsultationRequestSummaryResponse` (không lộ `description` đầy đủ — chỉ field tóm tắt theo §8.1)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-009 — List assigned: pagination default size=20, max=50

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationRequestController.listAssigned()`
**Test File:** `ConsultationRequestControllerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-CONREQ-IMP-001` §4.4 (convention cùng `ExpertProfileController`/`DirectConversationController`)

**Test Steps:**
1. `GET .../assigned` không truyền `size` → service nhận `pageable.pageSize == 20`
2. `GET .../assigned?size=51` → `400` (Bean Validation `@Max(50)`, cùng cơ chế generic đã xác nhận ở `ExpertProfileController`, không có business code riêng)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-010 — getById: 404 CONREQ-007 khi người gọi không phải participant (Issue 2 — IDOR fix, KHÔNG còn 403)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ConsultationRequestPolicy.assertCanView()`
**Test File:** `ConsultationRequestPolicyTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-006, TC-COND-025`
**Oracle Source:** `CB-CONREQ-IMP-001` §10 `CONREQ-007`, ADR-CONREQ-011, §14 Auth Matrix (v1.2 — v1.1's 403 `CONREQ-003` đã retire, User bác bỏ lập luận "403 không lộ nội dung")

**Test Steps:**
1. Request thuộc `(MOTHER_ID, EXPERT_PROFILE_ID)`; gọi `assertCanView(request, otherUserId, otherExpertUserId)`
2. Assert throws `ConsultationRequestException` code **`CONREQ-007`**, HTTP **`404`** — KHÔNG `403`, KHÔNG `CONREQ-003` (code đã retire)

**Expected Result (FAIL — dấu hiệu regression):** nếu code vẫn throw 403/`CONREQ-003` cho nhánh này, test FAIL ngay — đây chính là hành vi User đã yêu cầu sửa.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-026 — IDOR regression: "id không tồn tại" và "id tồn tại nhưng không phải participant" có cùng security-relevant response fields

**Severity:** `CRITICAL`
**CWE:** `CWE-203 — Observable Discrepancy`
**Feature Under Test:** `ConsultationRequestController.getById` (full HTTP round-trip, `@WebMvcTest` hoặc `@SpringBootTest`)
**Test File:** `ConsultationRequestControllerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-025`
**Oracle Source:** `CB-CONREQ-IMP-001` §10 IDOR note, ADR-CONREQ-011 (User: "Không phân biệt hai trường hợp bằng response, timing oracle hoặc repository lookup phụ có thể quan sát")

**Test Steps:**
1. Case A: `GET /consultation-requests/{randomNonExistentId}` (JWT của người ngoài cuộc bất kỳ) → capture full response (status, body JSON, headers không kể `Date`/`traceId` nếu có)
2. Case B: `GET /consultation-requests/{realIdBelongingToOtherPair}` (cùng JWT người ngoài cuộc đó) → capture full response
3. Assert: `status` giống nhau (`404`), `error` giống nhau (`"CONREQ-007"`), `message` giống nhau nguyên văn (chuỗi giống hệt, không có biến thể theo case), `details` đều `null` ở cả 2

**Expected Result (FAIL — dấu hiệu lỗi):** nếu 2 message khác nhau (vd. "Consultation request not found" vs "Access denied"), hoặc có thêm field/log/lookup phụ chỉ chạy ở 1 trong 2 nhánh (phát hiện được qua việc mock `auditService`/repository phụ và verify KHÔNG có invocation nào khác biệt giữa 2 case) — test FAIL.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-011 — getById: counterpart field role-correct — Mother thấy expert, Expert thấy mother (Issue 10 fix)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.getById()` + `ConsultationRequestMapper`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-006, TC-COND-031`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1 `ConsultationRequestResponse` (v1.2 — `counterpartDisplayName`/`counterpartAvatarUrl`, không còn `expertDisplayName`/`expertAvatarUrl` cứng cho Mother)

**Test Steps (parametrized, 2 role):**
1. Mock `userRepository.findById(expertUser.id)` → tên "BS. Nguyễn Văn A". Gọi `getById(requestId, currentUserId=MOTHER_ID)` → assert response `counterpartDisplayName == "BS. Nguyễn Văn A"` (tên EXPERT, vì viewer là Mother), `counterpartAvatarUrl` đúng avatar expert
2. Mock `userRepository.findById(motherUser.id)` → tên "Trần Thị B". Gọi `getById(requestId, currentUserId=EXPERT_USER_ID)` → assert response `counterpartDisplayName == "Trần Thị B"` (tên MOTHER, vì viewer là Expert)

**Expected Result (FAIL — dấu hiệu lỗi v1.1):** nếu mapper luôn resolve tên EXPERT bất kể viewer là ai (bug v1.1: field cứng tên `expertDisplayName`), sub-case 2 sẽ FAIL vì response trả tên expert thay vì tên mother cho 1 Expert đang xem request của chính mình.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-012 — Accept happy path: chuyển ACCEPTED + link DirectConversation

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.accept()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-002, §6.1, `directchat.service.IDirectConversationService.findOrCreate(UUID,UUID)` (chữ ký thật đã xác nhận qua đọc code)

**Test Steps:**
1. Mock `repository.findById` → PENDING request; mock `expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID)` → `makeApprovedExpertProfile()` (v1.3 fix — row-lock read, KHÔNG `tryAccept`); mock `directConversationService.findOrCreate(MOTHER_ID, EXPERT_PROFILE_ID)` → `FindOrCreateConversationResult(...conversationId...)`; mock `repository.tryTransition(id, ACCEPTED, respondedAt, EXPERT_USER_ID, null, conversationId)` → `1`
2. Act: `service.accept(requestId, EXPERT_USER_ID)`
3. Assert thứ tự lời gọi: `expertProfileRepository.findByIdForUpdate` TRƯỚC `directConversationService.findOrCreate` TRƯỚC `repository.tryTransition` (thứ tự bắt buộc — xem §6.2, ADR-CONREQ-005); response `status="ACCEPTED"`, `directConversationId` đúng giá trị trả về; **không** có bất kỳ tương tác nào với `ConsultationBookingRepository` (verify zero interactions — bảo vệ ADR-CONREQ-002); **không** gọi `expertProfileRepository.findById` (bản KHÔNG lock) ở bất kỳ đâu trong `accept()` — chỉ `findByIdForUpdate`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-013 — Accept fails khi expert bị revoke → 409 CONREQ-004, fail NGAY sau row-lock check, KHÔNG chạm consultation_requests (v1.3 fix — Issue 1 vòng 2)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.accept()`'s row-lock check qua `ExpertProfileRepository.findByIdForUpdate`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-008, TC-COND-028`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-005 (v1.3 — thay thế hoàn toàn `tryAccept`/EXISTS của v1.2, vốn không khoá `expert_profiles` nên không đóng được race liên-transaction; cũng thay thế `assertExpertStillApproved()` tách rời của v1.1)

**Test Steps:**
1. Mock `repository.findById` → PENDING request; `policy.assertCanRespond` pass (ownership OK)
2. Mock `expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID)` → `makeExpertProfile(VerificationStatus.REJECTED)` (giả lập `rejectExpert` đã chạy trước — KHÔNG phải `SUSPENDED`/`setTrustStatus`, xem ADR-CONREQ-005 audit)
3. Act + assert: `accept()` throws `ConsultationRequestException` code `CONREQ-004`, HTTP `409`
4. **Verify ZERO interaction** với `directConversationService.findOrCreate` VÀ `repository.tryTransition` — fail xảy ra NGAY sau bước lock+check, TRƯỚC mọi side effect khác (không như v1.2's thiết kế, nơi `tryAccept` phải chạy rồi mới biết fail)

**Expected Result (FAIL — dấu hiệu regression về pattern cũ):** nếu code viết lại thành `tryAccept`/EXISTS-in-UPDATE (v1.2, rejected) hoặc `assertExpertStillApproved()` không lock (v1.1, rejected), test này sẽ mock sai method (không có `findByIdForUpdate` nào được gọi, hoặc `tryTransition`/`findOrCreate` VẪN bị gọi trước khi fail) và FAIL — chính là tín hiệu code đã quay lại 1 trong 2 pattern bị cấm (C9, §15.1).
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-013b — Accept fails vì request đã transition (không phải vì expert revoke) → 409 CONREQ-005, không còn ambiguous (v1.3 — đơn giản hoá)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.accept()` khi row-lock check PASS nhưng `tryTransition` trả 0
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-028`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1 `accept()` javadoc (v1.3 — check APPROVED giờ LUÔN xảy ra trước `tryTransition`, nên 0 rows chỉ có 1 nghĩa duy nhất, không cần follow-up read để phân biệt như thiết kế `tryAccept` v1.2)

**Test Steps:**
1. Mock `expertProfileRepository.findByIdForUpdate` → `APPROVED` (pass — không phải nguyên nhân revoke)
2. Mock `directConversationService.findOrCreate(...)` → conversationId
3. Mock `repository.tryTransition(id, ACCEPTED, ..., conversationId)` → `0` (request đã bị 1 request khác transition trước — vd đã `ACCEPTED`/`CANCELLED`)
4. Act + assert: throws `CONREQ-005` (KHÔNG phải `CONREQ-004`) — vì bước lock+check đã pass, `tryTransition` trả 0 giờ chỉ có thể do "không còn PENDING", không cần đọc lại status để phân biệt nguyên nhân như v1.2

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-013c — Accept fails khi `trustStatus` SUSPENDED/REVOKED (verification vẫn APPROVED) → 409 CONREQ-004, zero side effect (v1.4 mới — Task 4)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.accept()`'s row-lock check — nhánh trust-caused ineligibility
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-035`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-005 v1.4 (predicate hợp nhất `isEligibleForConsultation`)

**Test Steps (parametrized, 2 sub-case, mirror cấu trúc `CONREQ-TC-013`):**
1. Mock `repository.findById` → PENDING request; `policy.assertCanRespond` pass (ownership OK)
2. Với `trust ∈ {SUSPENDED, REVOKED}`: mock `expertProfileRepository.findByIdForUpdate(EXPERT_PROFILE_ID)` → `makeExpertProfile(VerificationStatus.APPROVED, trust)` (verification vẫn hợp lệ — chỉ trust bị suspend/revoke, giả lập `setTrustStatus` đã chạy trước)
3. Act + assert: `accept()` throws `ConsultationRequestException` code `CONREQ-004`, HTTP `409`, message KHÔNG tiết lộ đây là verification hay trust
4. **Verify ZERO interaction** với `directConversationService.findOrCreate` VÀ `repository.tryTransition` (giống hệt `CONREQ-TC-013` — fail ngay sau lock+check)

**Expected Result (FAIL — dấu hiệu gap v1.3/mirror `DirectConversationPolicyImpl` cũ):** nếu code chỉ check `expertProfile.getVerificationStatus() != APPROVED` (không đọc `trustStatus`) — đúng hành vi v1.3 mà User đã yêu cầu đóng ở vòng này — test này FAIL vì `accept()` sẽ KHÔNG throw dù trust đã bị suspend/revoke (verification vẫn `APPROVED`).
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-014 — Reject với lý do: persist đúng `rejectReason`

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationRequestServiceImpl.reject()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-009 (reason optional)

**Test Steps:**
1. `service.reject(requestId, EXPERT_USER_ID, "Ngoài chuyên môn")`
2. Assert `repository.tryTransition(..., rejectReason="Ngoài chuyên môn", ...)` được gọi đúng giá trị
3. Assert `ApplicationEventPublisher.publishEvent` được gọi đúng 1 lần với `ConsultationRequestDomainEvent(eventType="REQUEST_REJECTED", requestId, actorUserId=EXPERT_USER_ID, ...)`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-015 — Reject không có lý do vẫn thành công (reason = null)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationRequestServiceImpl.reject()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-009

**Test Steps:**
1. `service.reject(requestId, EXPERT_USER_ID, null)` → thành công, `status=REJECTED`, `rejectReason=null`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-016 — Cancel hợp lệ khi vẫn PENDING, bởi đúng requester

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.cancel()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-009, §14

**Test Steps:**
1. `service.cancel(requestId, MOTHER_ID)` với request đang PENDING → `status=CANCELLED`
2. Assert `ApplicationEventPublisher.publishEvent` được gọi đúng 1 lần với `ConsultationRequestDomainEvent(eventType="REQUEST_CANCELLED", requestId, actorUserId=MOTHER_ID, ...)`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-017 — Cancel bị từ chối khi request đã ACCEPTED → 409 CONREQ-005

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.cancel()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-002/009 (cancel chỉ khi PENDING — sau ACCEPTED thuộc phạm vi booking tương lai)

**Test Steps:**
1. Mock `repository.tryTransition(...)` → `0` (vì entity đã ACCEPTED, WHERE status='PENDING' không match)
2. Assert throws `CONREQ-005`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-018 — Reject/cancel trên request đã terminal → 409 CONREQ-005 (mọi tổ hợp); accept xem `CONREQ-TC-013b`

**Severity:** `HIGH`
**Feature Under Test:** `tryTransition` return 0 handling trong `reject()`/`cancel()` (accept cũng dùng CHUNG `tryTransition` — v1.3 — nhưng tách riêng thành `CONREQ-TC-013b` vì accept có thêm bước row-lock check TRƯỚC đó, cần seed khác đi để đảm bảo 0 rows là do "không còn PENDING", không phải do expert không APPROVED)
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-011`
**Oracle Source:** `CB-CONREQ-IMP-001` §6.3 State Machine (mọi trạng thái ngoài PENDING là terminal)

**Test Steps (parametrized qua {ACCEPTED, REJECTED, CANCELLED, EXPIRED} × {reject, cancel}):**
1. Mock `tryTransition` → `0`
2. Assert throws `CONREQ-005` cho mọi tổ hợp

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-019 — Expiry job: per-row transition + publish `REQUEST_EXPIRED` cho mỗi id, bỏ qua id đã mất race

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.expireOverdueRequests()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-008 (Phương án A đã sửa — per-row, KHÔNG bulk UPDATE, vì bulk vi phạm `chk_consultation_requests_responded_fields` và không thể publish event per-row)

**Test Steps:**
1. Mock `repository.findExpiredIds(now, pageable)` → `[id1, id2, id3]`
2. Mock `repository.tryTransition(id1, EXPIRED, ...)` → `1`; `tryTransition(id2, EXPIRED, ...)` → `1`; `tryTransition(id3, EXPIRED, ...)` → `0` (id3 vừa bị accept/cancel bởi thread khác, mất race — mô phỏng đúng ghi chú trong ADR-CONREQ-008/§6.2)
3. Act: `service.expireOverdueRequests()`

**Expected Result (PASS):** trả về `2` (chỉ đếm id1, id2 — thành công thật); `ApplicationEventPublisher.publishEvent` được gọi đúng 2 lần với `eventType="REQUEST_EXPIRED"` cho `id1`/`id2`, **mỗi lần với `actorUserId == null` và `actorType == "SYSTEM"`** (Issue 8 fix — KHÔNG dùng `request.requesterUserId` làm actor), **không** publish cho `id3` (đã mất race, không có gì để báo).
**Expected Result (FAIL — dấu hiệu regression v1.1):** nếu event publish với `actorUserId == request.getRequesterUserId()` (impersonation, hành vi sai của v1.1) thay vì `null`, test FAIL.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-020 — Notification recipient resolution đúng theo từng event type

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestNotificationListener` switch logic
**Test File:** `src/test/java/com/carebridge/backend/consultation/event/ConsultationRequestNotificationListenerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-006 mục "Chi tiết implement" điểm 3 (recipient theo eventType)

**Test Steps (parametrized):**
1. `REQUEST_CREATED` → `notifyCreated` gọi với recipient = expert
2. `REQUEST_ACCEPTED`/`REQUEST_REJECTED`/`REQUEST_EXPIRED` → recipient = mother
3. `REQUEST_CANCELLED` → recipient = expert

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-021 — Notification payload không chứa nguyên văn topic/description

**Severity:** `CRITICAL`
**CWE:** `CWE-200 — Exposure of Sensitive Information`
**Legal:** `PDPA Art. minimization`
**Feature Under Test:** `ConsultationRequestNotificationServiceImpl` (body builder)
**Test File:** `src/test/java/com/carebridge/backend/notification/service/impl/ConsultationRequestNotificationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `CB-CONREQ-IMP-001` BR-CONREQ-009, mirror `DirectMessageNotificationServiceImpl` C4 (không đưa `message_body` vào push — ở đây tương đương không đưa `description`)

**Test Steps:**
1. Gọi `notifyCreated(expertUserId, requesterUserId, requestId)` với request có `description` chứa văn bản test đặc trưng (`"XYZ_SENSITIVE_MARKER"`)
2. Assert `NotificationRecord.body` và `metadata` KHÔNG chứa chuỗi `"XYZ_SENSITIVE_MARKER"`
3. **(Issue 3 bổ sung)** Assert data map truyền cho `fcmService.sendWithRetry(token, title, body, data, maxAttempts)` cũng KHÔNG chứa `"XYZ_SENSITIVE_MARKER"` — chỉ chứa đúng 2 key `type`/`requestId` (xem `CONREQ-TC-027`)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-022 — FCM data map cho tap-routing chứa đúng `type`/`requestId`, KHÔNG phải deep-link string tự bịa (Issue 3 fix)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationRequestNotificationServiceImpl.deliver()` — data map truyền vào `FcmService.sendWithRetry(..., data, ...)`
**Test File:** `ConsultationRequestNotificationServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-015, TC-COND-026`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.5 (v1.2 — v1.1's `metadata.get("deepLink")` là thiết kế tự bịa, không khớp `FcmService` thật vốn không có method nhận deep-link string; thiết kế thật là data map 2 key)

**Test Steps:**
1. Mock `fcmService.sendWithRetry(anyString(), anyString(), anyString(), anyMap(), anyInt())` (overload mới — Issue 3), capture đối số `data`
2. Gọi `notifyCreated(expertUserId, requesterUserId, requestId=X)` (device token tồn tại, preference bật)
3. Assert `data` truyền vào đúng bằng `Map.of("type", "CONSULTATION_REQUEST", "requestId", X.toString())` — không hơn không kém

**Expected Result (FAIL — dấu hiệu lỗi v1.1):** nếu code gọi bản `sendWithRetry` 3-arg cũ (text-only, không data) thay vì overload mới, mock verify sẽ không match — test FAIL, phát hiện đúng việc chưa build overload mới.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-027 — `FcmService` overload mới: `FirebaseFcmServiceImpl` gọi `.putAllData(data)`, không phá vỡ 3 method cũ (Issue 3)

**Severity:** `MEDIUM`
**Feature Under Test:** `FcmService.sendWithRetry(token, title, body, Map<String,String> data, int maxAttempts)` (additive overload)
**Test File:** `src/test/java/com/carebridge/backend/notification/service/impl/FcmServiceOverloadTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-026`
**Oracle Source:** `CB-CONREQ-IMP-001` §11.3 Chặng 3 mục 1, `FirebaseFcmServiceImpl.java`/`FcmServiceImpl.java` (code thật — 3 method text-only hiện có, không method data)

**Test Steps:**
1. `FcmServiceImpl` (stub, dùng khi FCM disabled) — gọi overload mới với `data={"type":"CONSULTATION_REQUEST","requestId":"..."}"` → không throw, trả sentinel giống 3 method cũ (log + null/0)
2. Regression: 3 method cũ (`sendToToken`/`sendToTokens`/`sendWithRetry` 3-arg) vẫn compile & hoạt động không đổi sau khi thêm overload (kiểm tra bằng cách gọi lại 1 test hiện có cho method cũ, nếu có — nếu chưa có test nào cho `FcmServiceImpl`/`FirebaseFcmServiceImpl` từ trước, ghi rõ đây là lần đầu, không phải regression check)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-023 — Global exception handler không lộ raw exception → 500 INTERNAL_ERROR thật (Issue 1 fix)

**Severity:** `HIGH`
**CWE:** `CWE-209 — Information Exposure Through an Error Message`
**Feature Under Test:** `GlobalExceptionHandler.handleGeneric` + `ConsultationRequestException`
**Test File:** `ConsultationRequestControllerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-019`
**Oracle Source:** `CB-CONREQ-IMP-001` §10, `GlobalExceptionHandler.java` (code thật — `handleGeneric` → `500 INTERNAL_ERROR`)

**Test Steps:**
1. Ép `service.accept(...)` throw `RuntimeException("internal db detail")` bất kỳ (giả lập lỗi không mong đợi)
2. Assert response `500`, body **flat** `ErrorResponse` thật: `error=="INTERNAL_ERROR"` (KHÔNG phải `CONREQ-008` — code đó đã retire), `message=="An unexpected error occurred"` (message thật của `handleGeneric`, không lộ `"internal db detail"` hay stacktrace)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-024 — getById: id không tồn tại → 404 CONREQ-007

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationRequestServiceImpl.getById()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-022`
**Oracle Source:** `CB-CONREQ-IMP-001` §10 `CONREQ-007`

**Test Steps:**
1. Mock `repository.findById(anyId)` → `Optional.empty()`
2. Act + assert: `getById(id, currentUserId)` throws `ConsultationRequestException` với code `CONREQ-007`, HTTP `404`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-025 — pendingSummary: `pendingCount` đúng bằng số request PENDING của đúng expert (Issue 11 — renamed từ `unreadSummary`)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.pendingSummary()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-023`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1/§8.2 `countByExpertProfileIdAndStatus`, §9.2 (`GET /pending-summary` response shape — v1.2 renamed từ `/unread-summary`, không có read-cursor)

**Test Steps:**
1. Mock `repository.countByExpertProfileIdAndStatus(EXPERT_PROFILE_ID, PENDING)` → `3`
2. Act: `service.pendingSummary(EXPERT_USER_ID)`
3. Assert: response là `ConsultationRequestPendingSummaryResponse` với `pendingCount == 3`; repository được gọi với đúng `expertProfileId` resolve từ `expertUserId` (không lộ số của expert khác — cùng nguyên tắc ownership §14)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-034 — `DirectConversationPolicyImpl.assertExpertEligibleForConsultation` chặn đúng trust-caused ineligibility (v1.4 mới — Task 7, cross-domain `directchat`)

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectConversationPolicyImpl.assertExpertEligibleForConsultation()` (đổi tên từ `assertExpertVerified`)
**Test File:** `src/test/java/com/carebridge/backend/directchat/policy/DirectConversationPolicyImplTest.java` (cross-domain — file có sẵn, thêm test method mới)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-038`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-013 (v1.4), §8.4

**Test Steps (parametrized):**
1. `verification=APPROVED, trust=ACTIVE` → không throw
2. `verification=APPROVED, trust=SUSPENDED` → throws `DirectChatException` code `DCC-002`, message `"Expert is not eligible for consultation"` (KHÔNG còn `"Expert is not APPROVED"` — message đó sẽ sai vì lý do thật là trust)
3. `verification=APPROVED, trust=REVOKED` → throws `DCC-002` cùng message
4. `verification=REJECTED, trust=ACTIVE` → throws `DCC-002` (regression — verification-only case vẫn phải tiếp tục hoạt động đúng)

**Expected Result (FAIL — dấu hiệu gap trước v1.4):** nếu code vẫn chỉ check `getVerificationStatus() != APPROVED` (hành vi `assertExpertVerified` cũ) → bước 2/3 FAIL vì không throw.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-035 — `DirectConversationPolicyImpl.assertConversationWritable` nhận `ExpertProfile` ĐÃ LOCK, KHÔNG tự query, chặn write/call mới khi mất eligibility (v1.4 predicate; **v1.5 rewrite — signature đổi, Problem 1, ADR-CONREQ-013**)

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectConversationPolicyImpl.assertConversationWritable(ExpertProfile lockedExpertProfile)` — **signature đổi từ `(DirectConversation conversation)` sang `(ExpertProfile lockedExpertProfile)` (v1.5)**
**Test File:** `DirectConversationPolicyImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-039, TC-COND-047`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-013 (v1.4 predicate; v1.5 signature + lock protocol), ADR-DCC-007 §2 (exemption giữ nguyên)

**Test Steps (parametrized — Arrange/Act/Assert dùng trực tiếp `ExpertProfile`, KHÔNG mock `DirectConversation`/`expertProfileRepository.findByUserId` nữa vì method không còn tự query):**
1. `lockedExpertProfile = makeExpertProfile(APPROVED, ACTIVE)` → `assertConversationWritable(lockedExpertProfile)` không throw
2. `lockedExpertProfile = makeExpertProfile(APPROVED, SUSPENDED)` → throws `DirectChatException` code `DCC-010`, message `"Expert is no longer available for this conversation"` (message KHÔNG đổi — đã generic sẵn từ trước)
3. `lockedExpertProfile = makeExpertProfile(APPROVED, REVOKED)` → throws `DCC-010`
4. **Assertion mới v1.5 (bằng chứng "không tự query"):** `Mockito.verify(expertProfileRepository, Mockito.never()).findByUserId(Mockito.any())` VÀ `Mockito.verifyNoInteractions(expertProfileRepository)` toàn bộ test method — nếu policy vẫn tự query nội bộ (hành vi cũ trước v1.5), assertion này FAIL ngay cả khi bước 1-3 pass đúng giá trị (bắt đúng regression "policy silent query", AP-AI-015 ở TDS §15.4)
5. **Regression — không đổi hành vi ngoài phạm vi:** `assertIsParticipant` (method khác, vẫn nhận `DirectConversation` + tự query — xem `CONREQ-TC-038`) — verify method này KHÔNG check `trustStatus`, chỉ `verificationStatus`
6. **Regression — end-of-ANSWERED-call exemption (ADR-DCC-007 §2):** không phải test của `assertConversationWritable` trực tiếp — xem `CONREQ-TC-INT-020` cho bằng chứng ở tầng `ConversationCallServiceImpl.end()` không gọi `assertConversationWritable`/không lock ở nhánh `answered`

**Expected Result (FAIL — dấu hiệu gap trước v1.5):** nếu code vẫn giữ signature `(DirectConversation)` và tự `findByUserId` bên trong → compile-fail hoặc bước 4 FAIL (repository bị gọi); nếu vẫn chỉ check `verificationStatus` → bước 2/3 FAIL vì không throw dù trust bị suspend/revoke.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-038 — `DirectConversationPolicyImpl.assertIsParticipant` — behavior matrix chính xác theo actor: Mother luôn qua, Expert chỉ gate bởi verification (v1.5 mới — Problem 3, ADR-CONREQ-013 Behavior Matrix)

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectConversationPolicyImpl.assertIsParticipant(UUID currentUserId, DirectConversation conversation)` — **KHÔNG đổi code, chỉ audit + test chính xác hành vi thật thay vì tuyên bố chung chung**
**Test File:** `DirectConversationPolicyImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-046`
**Oracle Source:** `DirectConversationPolicyImpl.java` dòng 19-33 (code thật), `CB-CONREQ-IMP-001` ADR-CONREQ-013 Behavior Matrix (v1.5)

**Test Steps (parametrized):**
1. `currentUserId == conversation.motherUserId`, Expert của conversation là `REJECTED`/`SUSPENDED`(verification)/`EXPIRED` → KHÔNG throw (Mother luôn qua, không kiểm gì về Expert)
2. `currentUserId == conversation.motherUserId`, Expert là `APPROVED, trustStatus=SUSPENDED/REVOKED` → KHÔNG throw (giống bước 1 — Mother không bao giờ bị ảnh hưởng bởi trạng thái Expert)
3. `currentUserId == conversation.expertUserId`, `ExpertProfile{verificationStatus=APPROVED, trustStatus=SUSPENDED}` → KHÔNG throw (trust KHÔNG được đọc ở method này — đúng chủ đích, không phải bỏ sót)
4. `currentUserId == conversation.expertUserId`, `ExpertProfile{verificationStatus=APPROVED, trustStatus=REVOKED}` → KHÔNG throw (giống bước 3)
5. `currentUserId == conversation.expertUserId`, `ExpertProfile{verificationStatus=REJECTED}` → throws `DirectChatException.expertNoLongerApproved()` (mã DCC hiện có trong code)
6. `currentUserId` không phải Mother lẫn Expert của conversation → throws `DirectChatException.notParticipant()`
7. **Assertion mới:** verify method KHÔNG gọi `findByIdForUpdate`/`findByUserIdForUpdate` (chỉ `findByUserId` thường, hoặc không gọi gì nếu currentUserId là Mother) — chứng minh đọc không lock

**Expected Result (FAIL trước khi có test):** test chưa tồn tại — assertions trên chưa từng được verify tường minh theo TỪNG bước 1-6, dễ bị nhầm với tuyên bố chung "lịch sử luôn đọc được" không đủ chính xác cho reviewer.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-036 — `ExpertProfileMapper` set đúng `isConsultationEligible` cho cả `ExpertProfileResponse` và `ExpertProfileDetailResponse` (v1.4 mới — Task 10, cross-domain `expert`)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileMapper.toResponse()` / `toDetailResponse()`
**Test File:** `src/test/java/com/carebridge/backend/expert/mapper/ExpertProfileMapperTest.java` (file có sẵn — thêm test method mới)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-041`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-010 bổ sung (v1.4, Task 10), §8.3

**Test Steps (parametrized, cả 2 phương thức mapper):**
1. `entity = makeExpertProfile(APPROVED, ACTIVE)` → `toResponse(entity, ...).isConsultationEligible() == true`; `toDetailResponse(entity, ...).isConsultationEligible() == true`
2. `entity = makeExpertProfile(APPROVED, SUSPENDED)` → cả 2 method trả `isConsultationEligible() == false`
3. `entity = makeExpertProfile(REJECTED, ACTIVE)` → cả 2 method trả `isConsultationEligible() == false`
4. Assert field này KHÔNG lộ `trustStatus` trực tiếp trong response (chỉ boolean dẫn xuất — không thêm field `trustStatus` nào vào DTO)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-037 — Reject/cancel/expiry KHÔNG bị gate bởi eligibility — zero interaction với `ExpertProfileRepository` (v1.4 mới — Task 9, regression tường minh)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.reject()`, `.cancel()`, `.expireOverdueRequests()`
**Test File:** `ConsultationRequestServiceImplTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-040`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-003 Hệ quả (v1.4 note), §6.3 invariant 5, §14

**Test Steps:**
1. Mock `repository.findById` → PENDING request; KHÔNG stub `expertProfileRepository` ở bất kỳ method nào (không cần vì không được gọi)
2. Act: `service.reject(requestId, EXPERT_USER_ID, "lý do bất kỳ")` → thành công, `status=REJECTED`
3. Act: `service.cancel(requestId2, MOTHER_ID)` (request PENDING khác) → thành công, `status=CANCELLED`
4. Act: `service.expireOverdueRequests()` (với 1 request PENDING quá hạn đã mock qua `findExpiredIds`) → thành công, `status=EXPIRED`
5. **Assert cho cả 3:** `Mockito.verifyNoInteractions(expertProfileRepository)` — 3 method này KHÔNG BAO GIỜ đọc `ExpertProfile` ở bất kỳ hình thức nào (không `findById`, không `findByIdForUpdate`) — chứng minh chúng thực sự không phụ thuộc eligibility, không phải "tình cờ pass" vì mock trả giá trị hợp lệ

**Expected Result (FAIL — dấu hiệu regression nếu ai đó "sửa nhầm" thêm eligibility gate vào đây):** nếu 1 sửa đổi tương lai vô tình thêm check `expertProfileRepository.findBy...` vào `reject`/`cancel`/`expireOverdueRequests` (lẫn logic từ `accept`), test này FAIL ngay vì `verifyNoInteractions` phát hiện lời gọi không mong đợi — bảo vệ đúng invariant "expert mất eligibility vẫn phải reject/cancel/expire được".
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

## SECURITY TEST CASES

### CONREQ-TC-SEC-001 — Non-MOTHER role bị chặn tại `POST /consultation-requests`

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**CWE:** `CWE-863`
**Feature Under Test:** `ConsultationRequestController` `@PreAuthorize`
**Test File:** `ConsultationRequestControllerTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Test Steps (Attack Simulation):**
1. JWT role `EXPERT` gọi `POST /consultation-requests`
2. Assert `403`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-SEC-002 — Expert A không accept được request được gán cho Expert B → 404 CONREQ-007 (Issue 2)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**CWE:** `CWE-639`
**Feature Under Test:** `ConsultationRequestPolicy.assertCanRespond()`
**Test File:** `ConsultationRequestPolicyTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Test Steps:**
1. Request gán cho `expertProfileId=A`; gọi `assertCanRespond(request, expertUserOfB)`
2. Assert throws `CONREQ-007` (v1.2 — KHÔNG phải `CONREQ-003`, code đã retire theo ADR-CONREQ-011), HTTP `404`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-SEC-003 — Mother B không cancel được request của Mother A → 404 CONREQ-007 (Issue 2)

**Severity:** `CRITICAL`
**OWASP:** `A01:2021`
**CWE:** `CWE-639`
**Feature Under Test:** `ConsultationRequestPolicy.assertCanCancel()`
**Test File:** `ConsultationRequestPolicyTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Test Steps:**
1. Request thuộc Mother A; gọi `assertCanCancel(request, motherB)`
2. Assert throws `CONREQ-007` (v1.2 — KHÔNG phải `CONREQ-003`), HTTP `404`

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-SEC-004 — Injection trong `topic`/`description` không phá vỡ truy vấn / lưu nguyên văn an toàn

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89`
**Feature Under Test:** `ConsultationRequestRepository` (JPA parametrized — không raw SQL concatenation)
**Test File:** `src/test/java/com/carebridge/backend/consultation/repository/ConsultationRequestRepositoryIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Test Steps:**
1. Tạo request với `topic = "'; DROP TABLE consultation_requests; --"`
2. Assert lưu thành công nguyên văn (JPA parametrized query), bảng vẫn tồn tại, `findById` trả đúng chuỗi

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

## INTEGRATION TEST CASES

> Testcontainers PostgreSQL. Timeout 120s. Migration chạy tự động khi Spring context start.

### CONREQ-TC-INT-001 — Double-submit thật: 2 request đồng thời CÙNG `clientRequestId` → chỉ 1 row (Issue 6 — không còn khoá theo cặp mother-expert)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestWriter.insertIfAbsent` + `consultation_requests_client_request_id_key` (`UNIQUE (requester_user_id, client_request_id)`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationRequestConcurrencyIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-004, TC-COND-027`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-004 (v1.2)

**Preconditions:**
- PostgreSQL container chạy, Flyway đã áp dụng `V20260716200500` (kiểm tra cột `client_request_id` tồn tại)
- Seed: Mother, Expert APPROVED

**Test Steps:**
1. 1 `clientRequestId` duy nhất (`sharedKey`) được dùng bởi CẢ 2 thread — mô phỏng double-tap client thật gửi 2 request HTTP với cùng key (`CountDownLatch` đồng bộ start)
2. 2 thread gọi service thật đồng thời. Cả hai lookup đầu có thể miss; chúng phải serialize qua `findByIdForUpdate`. Thread chờ lock phải double-check key sau khi lấy lock và trả existing nếu thread kia đã commit.
3. `SELECT COUNT(*) FROM consultation_requests WHERE requester_user_id=? AND client_request_id=?`

**Expected Result (PASS):** đúng 1 row; cả 2 thread cùng `id`; đúng một `created=true`, một `created=false`; đúng một event/audit/notification create; không exception lạ.
**DB Assertion:**
```java
List<ConsultationRequest> rows = repository.findAll();
assertThat(rows).hasSize(1);
```
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-002 — List assigned không N+1 (đo số query cố định)

**Severity:** `MEDIUM`
**Feature Under Test:** `ConsultationRequestServiceImpl.listAssigned` (batch-resolve `counterpartDisplayName`)
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationRequestListIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-CONREQ-IMP-001` §4.4, mirror ADR-MEDI-002's "batch-fetch cố định" convention

**Test Steps:**
1. Seed 20 request cho cùng 1 expert, từ 20 mother khác nhau (mỗi mother có tên hiển thị khác nhau)
2. Đếm số query SQL thực thi (Hibernate statistics) khi gọi `listAssigned`
3. Assert số query là hằng số cố định (không tăng tuyến tính theo số request — ví dụ ≤ 3 query bất kể 20 hay 200 row)
4. **(Issue 10 bổ sung)** Assert mỗi `ConsultationRequestSummaryResponse.counterpartDisplayName` trong response khớp đúng tên MOTHER tương ứng với `requester_user_id` của row đó (viewer là Expert → counterpart luôn là mother, batch-resolved qua `findAllById` — không phải 1 tên cố định lặp lại cho mọi row)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-003 — Accept thật: `direct_conversations` được tạo/liên kết đúng

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.accept()` + `IDirectConversationService.findOrCreate` thật (không mock)
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationRequestAcceptIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-007`

**Test Steps:**
1. Seed request PENDING
2. `service.accept(requestId, expertUserId)`
3. `SELECT direct_conversation_id FROM consultation_requests WHERE id=?` và `SELECT * FROM direct_conversations WHERE conversation_id=?`

**Expected Result (PASS):** `direct_conversation_id` không null, tồn tại row tương ứng trong `direct_conversations` với đúng `mother_user_id`/`expert_user_id`. Gọi `accept` lần 2 (idempotent-retry giả lập bằng gọi `findOrCreate` lại) không tạo thêm row `direct_conversations` mới (unique constraint `uq_direct_conversations_pair`).
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-004 — Concurrent accept: 2 thread accept cùng request, chỉ 1 thắng

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestRepository.tryTransition` (v1.3 — accept dùng lại method chung, không còn `tryAccept`) + `ExpertProfileRepository.findByIdForUpdate` (row-lock side effect)
**Test File:** `ConsultationRequestConcurrencyIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-012`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-003/005, §6.2 Scenario 2

**Test Steps:**
1. Seed 1 request PENDING, expert APPROVED
2. 2 thread gọi `service.accept(id, expertUserId)` đồng thời qua `ExecutorService` + `CountDownLatch`
3. Assert: đúng 1 thread nhận `200`, thread còn lại nhận exception với code `CONREQ-005`; DB có đúng 1 row `direct_conversations` cho cặp đó (không tạo trùng — xem `CONREQ-TC-INT-011` cho assertion rollback chi tiết của thread thua)

**Ghi chú thiết kế (v1.3):** vì cả 2 thread đều gọi `findByIdForUpdate` trên CÙNG `expertProfileId`, chúng serialize qua row-lock đó TRƯỚC KHI tới `tryTransition` — thread thua bị block ở bước lock cho tới khi thread thắng commit, rồi mới lock được, thấy request đã `ACCEPTED`, và `tryTransition` của nó trả 0. Đây là hệ quả phụ chấp nhận được của thiết kế lock (xem ADR-CONREQ-005 Hệ quả) — không phải bug, chỉ là 2 lệnh accept trên CÙNG 1 expert giờ tuần tự hoá thêm 1 lớp nữa so với v1.2.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-005 — Concurrent accept + cancel: chỉ 1 transition thắng

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestRepository.tryTransition` — dùng CHUNG cho cả accept (sau khi row-lock check pass) và cancel
**Test File:** `ConsultationRequestConcurrencyIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-013`

**Test Steps:**
1. Seed 1 request PENDING, expert APPROVED
2. Thread A: `service.accept(id, expertUserId)`; Thread B (đồng thời): `service.cancel(id, motherUserId)`
3. Assert: đúng 1 trong 2 thành công, cái kia nhận `CONREQ-005`; trạng thái cuối cùng trong DB khớp với bên thắng (nếu accept thắng: `ACCEPTED` + `direct_conversation_id` set; nếu cancel thắng: `CANCELLED`, `direct_conversation_id` vẫn null)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-010 — TOCTOU thật: accept đồng thời với revoke expert — row-lock đóng đúng race, 2 scenario ordering-controlled (v1.3 rewrite — Issue 1 vòng 2, mandatory)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.accept()`'s row-lock check (`ExpertProfileRepository.findByIdForUpdate`) đồng bộ THẬT với `ExpertProfileServiceImpl.rejectExpert` (KHÔNG phải `setTrustStatus` — xem audit v1.3: `setTrustStatus` mutate field `trustStatus`, không phải `verificationStatus` mà check này đọc)
**Test File:** `ConsultationRequestConcurrencyIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-028`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-005 (v1.3), §6.2 Scenario 1a/1b; mirror cấu trúc test thật `NotificationRecordWriterConcurrencyIntegrationTest`/`OtpRaceConditionIntegrationTest` (`CountDownLatch` + `ExecutorService`, service thật, KHÔNG Mockito) — nhưng dùng **latch để kiểm soát THỨ TỰ tường minh** (không chỉ "start cùng lúc rồi xem ai thắng" như 2 test mẫu đó), vì mục tiêu ở đây là chứng minh 2 outcome CỤ THỂ theo đúng thứ tự, không phải 1 invariant chung chung.

**Preconditions:**
- PostgreSQL container thật (Testcontainers), KHÔNG mock `ExpertProfileRepository`/`ConsultationRequestRepository`/`ExpertProfileServiceImpl`/`ConsultationRequestServiceImpl`
- Seed (mỗi scenario riêng, Props Isolation — KHÔNG chia sẻ request/expert giữa 2 scenario): 1 `ConsultationRequest` PENDING, `ExpertProfile.verificationStatus = APPROVED`

**Scenario A — Revoke thắng lock trước (accept phải bị chặn đúng):**
1. `CountDownLatch revokeDone = new CountDownLatch(1)`
2. Thread Revoke: gọi `expertProfileService.rejectExpert(expertProfileId, adminId, "test revoke")` (service thật, transaction thật, tự commit khi method return vì method là `@Transactional` public) → `revokeDone.countDown()`
3. Thread Accept: `revokeDone.await()` rồi mới gọi `consultationRequestService.accept(requestId, expertUserId)` — đảm bảo thứ tự: revoke ĐÃ commit (lock đã release) trước khi accept bắt đầu
4. `pool.awaitTermination(...)`

**Expected Result (PASS — Scenario A):**
- Thread Accept ném `ConsultationRequestException` code `CONREQ-004` (KHÔNG phải exception lạ)
- `SELECT status, direct_conversation_id FROM consultation_requests WHERE id=?` → `status='PENDING'`, `direct_conversation_id IS NULL` (không đổi)
- `SELECT COUNT(*) FROM direct_conversations WHERE mother_user_id=? AND expert_user_id=?` → `0` (không tạo)
- `SELECT verification_status FROM expert_profiles WHERE expert_profile_id=?` → `'REJECTED'`
- Không có `NotificationRecord`/domain event nào loại `REQUEST_ACCEPTED` được tạo cho request này (chỉ có tác dụng phụ của `rejectExpert`, nếu có, không thuộc phạm vi assert ở đây)

**Scenario B — Accept thắng lock trước (accept thành công hợp lệ, revoke chạy sau là đúng, không phải TOCTOU):**
1. `CountDownLatch acceptDone = new CountDownLatch(1)`
2. Thread Accept: gọi `consultationRequestService.accept(requestId, expertUserId)` → `acceptDone.countDown()`
3. Thread Revoke: `acceptDone.await()` rồi mới gọi `expertProfileService.rejectExpert(expertProfileId, adminId, "test revoke after accept")`
4. `pool.awaitTermination(...)`

**Expected Result (PASS — Scenario B):**
- Thread Accept không throw, trả `ConsultationRequestResponse(status="ACCEPTED", directConversationId=<not null>)`
- Thread Revoke không throw
- `SELECT status FROM consultation_requests WHERE id=?` → `'ACCEPTED'`
- `SELECT verification_status FROM expert_profiles WHERE expert_profile_id=?` → `'REJECTED'`
- Kết hợp `ACCEPTED` + `REJECTED` này là HỢP LỆ (ordering đã chứng minh bằng latch: revoke chỉ bắt đầu SAU khi accept's `acceptDone.countDown()` chạy, tức accept's method đã return — transaction đã commit, lock đã release)

**Expected Result (FAIL — dấu hiệu lỗi thiết kế cũ, cả 2 scenario):** nếu code accept vẫn dùng `tryAccept`/`EXISTS` (v1.2, rejected) hoặc `assertExpertStillApproved()` không lock (v1.1, rejected) thay vì `findByIdForUpdate` thật, Scenario A có xác suất > 0 FAIL (accept đọc thấy `APPROVED` dù revoke đã commit trước đó, do không có lock nào buộc accept phải đợi) — đây chính là race mà `EXISTS`/read-không-lock không đóng được, khác với row-lock đóng được **tất định** (không xác suất).
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-011 — Accept losing transaction rollback đầy đủ: DirectConversation vừa insert trong transaction thua PHẢI rollback (v1.3 rewrite — Issue 4 vòng 2, controlled-ordering race thật)

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestServiceImpl.accept()` — `@Transactional` REQUIRED rollback khi `tryTransition` trả 0 SAU KHI `findOrCreate` đã insert 1 row MỚI (chưa commit) trong CÙNG transaction
**Test File:** `ConsultationRequestAcceptIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-029`
**Oracle Source:** `CB-CONREQ-IMP-001` §6.2 Scenario 2 (v1.2 sửa tuyên bố sai của v1.1; v1.3 — test trước đó (v1.2) chỉ chứng minh `findOrCreate` idempotent khi conversation ĐÃ TỒN TẠI, KHÔNG chứng minh rollback của 1 INSERT mới trong transaction thua — User đã yêu cầu sửa)

**Vì sao cần kỹ thuật mới:** để chứng minh rollback của 1 row MỚI (không phải đọc lại row cũ), `findOrCreate`'s insert phải xảy ra TRƯỚC khi request bị 1 thread khác chuyển sang trạng thái non-PENDING, nhưng VẪN TRONG transaction Accept (chưa commit). Vì `accept()` là 1 lời gọi service đồng bộ, cần 1 điểm dừng có kiểm soát ngay sau `findOrCreate` return và trước `tryTransition` chạy — dùng `@MockitoSpyBean` (hoặc `@SpyBean`, tuỳ version Spring Boot Test) wrap bean thật `IDirectConversationService`, `doAnswer` gọi `invocation.callRealMethod()` (insert THẬT xảy ra) rồi `pauseLatch.await()` trước khi return. Đây KHÔNG phải Mockito-only test — mọi thao tác DB đều thật (Testcontainers), chỉ ĐIỂM DỪNG được điều khiển qua spy.

**Preconditions:**
- Request PENDING; CHƯA có `direct_conversations` row nào cho cặp (mother, expert) này
- `directConversationService` là `@MockitoSpyBean` wrap bean thật (không mock hành vi, chỉ chèn latch)

**Test Steps:**
1. `CountDownLatch pauseLatch = new CountDownLatch(1)`, `CountDownLatch findOrCreateDone = new CountDownLatch(1)`
2. Cấu hình spy: `doAnswer(inv -> { Object r = inv.callRealMethod(); findOrCreateDone.countDown(); pauseLatch.await(); return r; }).when(directConversationServiceSpy).findOrCreate(any(), any())`
3. Thread Accept: gọi `consultationRequestService.accept(requestId, expertUserId)` — chạy tới `findOrCreate`, INSERT thật xảy ra (chưa commit, transaction Accept vẫn mở), rồi bị pause tại `pauseLatch.await()`
4. Main thread: `findOrCreateDone.await()` (đảm bảo INSERT đã xảy ra), rồi gọi `consultationRequestService.cancel(requestId, motherUserId)` — chạy trong transaction RIÊNG, commit thành công, request → `CANCELLED`
5. Main thread: `pauseLatch.countDown()` — thả Thread Accept tiếp tục
6. Thread Accept resume: `tryTransition(id, ACCEPTED, ...)` chạy → `rowsAffected=0` (status đã `CANCELLED`, không còn `PENDING`) → service throws `CONREQ-005` → toàn bộ transaction Accept ROLLBACK (bao gồm INSERT của `findOrCreate` ở bước 3, dù đã thực thi thật trong DB, chưa commit nên rollback xoá sạch)
7. `pool.awaitTermination(...)`; đọc DB qua `JdbcTemplate`

**Expected Result (PASS):**
- Thread Accept nhận `ConsultationRequestException` code `CONREQ-005` (không phải lỗi DB lạ)
- `SELECT status, direct_conversation_id FROM consultation_requests WHERE id=?` → `status='CANCELLED'`, `direct_conversation_id IS NULL`
- `SELECT COUNT(*) FROM direct_conversations WHERE mother_user_id=? AND expert_user_id=?` → `0` (row insert ở bước 3 đã bị rollback — KHÔNG còn tồn tại, dù đã thực thi thật trong DB tại 1 thời điểm)
- Không có `NotificationRecord`/domain event `REQUEST_ACCEPTED` nào tồn tại cho request này; không có `AuditAction` accept nào được ghi (chỉ có `REQUEST_CANCELLED`/audit cancel, từ bước 4)

**Xác nhận transaction boundary (ghi vào TDS, không chỉ test):** `ConsultationRequestServiceImpl.accept()` là `@Transactional` (REQUIRED, mặc định class-level, không `REQUIRES_NEW`); `IDirectConversationService.findOrCreate` cũng chạy REQUIRED (join transaction của caller — xác nhận qua code thật, không có `@Transactional(propagation=...)` khác trên method đó); `ConsultationRequestException` là unchecked (`RuntimeException` subclass, mirror `DirectChatException`) nên Spring's default rollback rule (rollback trên mọi unchecked exception) áp dụng tự động — không cần `@Transactional(rollbackFor=...)` tường minh.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-006 — Expiry job thật: chỉ EXPIRE row PENDING quá hạn, set đúng `responded_at`, phát `REQUEST_EXPIRED` notification

**Severity:** `HIGH`
**Feature Under Test:** `ConsultationRequestExpiryJob` + `ConsultationRequestRepository.findExpiredIds`/`tryTransition` + `ConsultationRequestNotificationServiceImpl.notifyExpired`
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationRequestExpiryIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-014`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-008 (per-row), `chk_consultation_requests_responded_fields` (§5.2 migration)

**Test Steps:**
1. Seed: request A (PENDING, `expires_at` = 1h trước), request B (PENDING, `expires_at` = 1h sau — chưa hết hạn), request C (ACCEPTED, `expires_at` = 1h trước — đã terminal)
2. `job.dispatchExpiry()` (chạy `service.expireOverdueRequests()` thật, không mock)
3. `SELECT status, responded_at FROM consultation_requests WHERE id IN (A,B,C)`; poll `notification_records WHERE reference_id = A AND reference_type='CONSULTATION_REQUEST'`

**Expected Result (PASS):** A → `status=EXPIRED`, `responded_at IS NOT NULL` (constraint `chk_consultation_requests_responded_fields` không bị vi phạm — nếu code còn dùng bulk UPDATE không set `responded_at`, câu UPDATE sẽ throw ngay tại bước này, test FAIL rõ ràng); B vẫn `PENDING`; C vẫn `ACCEPTED` (không bị đụng); đúng 1 `NotificationRecord` mới xuất hiện cho A với `reference_type='CONSULTATION_REQUEST'` (không có cho B/C). **(Issue 8 bổ sung)** Domain event captured qua `@EventListener` test helper (hoặc `ApplicationEvents` của Spring Test) cho `REQUEST_EXPIRED` của A có `actorUserId == null` và `actorType == "SYSTEM"` — KHÔNG phải `actorUserId == A.getRequesterUserId()`.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-007 — FCM exception không rollback business transaction

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.accept()` + AFTER_COMMIT listener + `FcmService` ném exception
**Test File:** `src/test/java/com/carebridge/backend/consultation/ConsultationRequestNotificationIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-017`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-006, mirror `DirectMessageNotificationServiceImpl.deliver()` C11 (sentinel `attemptCount=0`, never rethrown)

**Test Steps:**
1. Seed request PENDING; mock `FcmService.sendWithRetry` để ném `RuntimeException` bất kỳ
2. `service.accept(id, expertUserId)` — chạy trong Spring context thật (transaction thật commit + AFTER_COMMIT listener thật chạy async)
3. Poll DB (không sleep cố định — poll có timeout) cho tới khi `NotificationRecord` xuất hiện

**Expected Result (PASS):** `consultation_requests.status = ACCEPTED` (transaction chính đã commit thành công, KHÔNG bị rollback bởi lỗi FCM); `NotificationRecord.status = FAILED`, `attemptCount = 0`, `failedAt` không null — lỗi được ghi nhận, không biến mất.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-008 — Notification: redelivery của cùng 1 event bị chặn trùng; 2 event khác nhau cho cùng recipient+request KHÔNG bị chặn nhầm

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestNotificationWriter.insertIfAbsent` + `uq_notification_records_consultation_request` (functional index theo `(user_id, reference_id, metadata->>'eventType')`)
**Test File:** `ConsultationRequestNotificationIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-016, TC-COND-024`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-007 (v1.1 — sửa lỗi grain sau self-review)

**Test Steps (Part 1 — redelivery của cùng 1 event, phải bị chặn):**
1. Gọi `listener.onRequestEvent(sameRequestCreatedEvent)` 2 lần liên tiếp thủ công (mô phỏng redelivery)
2. `SELECT COUNT(*) FROM notification_records WHERE reference_id=? AND reference_type='CONSULTATION_REQUEST' AND metadata->>'eventType'='REQUEST_CREATED'`

**Expected Result (PASS — Part 1):** đúng 1 row (lần gọi thứ 2 bị `ON CONFLICT DO NOTHING` chặn, `insertIfAbsent` trả `false`, không gọi FCM lần 2).

**Test Steps (Part 2 — 2 event khác nhau cho cùng recipient+request, KHÔNG được chặn nhầm — đây là bug đã tìm thấy và sửa ở ADR-CONREQ-007):**
1. Seed request PENDING; expert nhận `REQUEST_CREATED` (qua `listener.onRequestEvent`)
2. Mother cancel request → publish `REQUEST_CANCELLED` (expert cũng là recipient — xem ADR-CONREQ-006 resolveRecipients: CANCELLED → expert)
3. `SELECT COUNT(*), array_agg(metadata->>'eventType') FROM notification_records WHERE user_id=<expertUserId> AND reference_id=<requestId>`

**Expected Result (PASS — Part 2):** đúng **2** row, `eventType` gồm cả `REQUEST_CREATED` và `REQUEST_CANCELLED`, cả 2 đều `status` khác `PENDING` (đã qua `deliver()`). **Expected Result (FAIL — dấu hiệu lỗi):** chỉ có 1 row (`REQUEST_CANCELLED` bị `ON CONFLICT` chặn nhầm bởi row `REQUEST_CREATED` đã tồn tại) — đây chính là bug đã phát hiện khi index không có discriminator `eventType`.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-009 — Migration chạy thành công trên Testcontainers PostgreSQL sạch

**Severity:** `HIGH`
**Feature Under Test:** `V20260716200500__create_consultation_requests.sql`, `V20260716200501__add_notification_records_consultation_request_idempotency.sql`
**Test File:** bất kỳ `@SpringBootTest` Testcontainers integration test nào trong package (context load kéo theo Flyway chạy toàn bộ)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-020`

**Test Steps:**
1. Khởi động Spring context với `PostgreSqlContainer` sạch (không có data cũ)
2. Flyway tự động chạy toàn bộ migration kể cả 2 migration mới

**Expected Result (PASS):** context load thành công, `flyway_schema_history` chứa 2 version mới với `success=true` (⚠️ version thật phải re-verify tại thời điểm implement theo §11.1 TDS — không hardcode giả định `V20260717*` nếu migration khác đã được áp dụng trước đó); `\d consultation_requests` có đủ cột/constraint/index như §5.2 TDS — **đặc biệt (Issue 6/12 bổ sung):** cột `client_request_id` (NOT NULL), constraint `consultation_requests_client_request_id_key` (`UNIQUE (requester_user_id, client_request_id)`) tồn tại, **và** `uq_consultation_requests_pending` (index cũ của v1.1) **KHÔNG** tồn tại (đã bị bỏ theo Issue 6); `chk_consultation_requests_window` reject được 1 INSERT thử nghiệm có `preferred_window_start` không null nhưng `preferred_window_end` null (Issue 12 — DB-level, độc lập với Bean Validation phía service).
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-012 — Concurrent HTTP thật, cùng `clientRequestId`: đúng 1 request `201`, request còn lại `200`, cùng resource id, đúng 1 event (v1.3 — Issue 3 vòng 2, full-stack)

**Severity:** `HIGH`
**Feature Under Test:** Toàn bộ stack `ConsultationRequestController` → `ConsultationRequestServiceImpl` → `ConsultationRequestWriter` dưới tải HTTP đồng thời thật
**Test File:** `ConsultationRequestConcurrencyIntegrationTest.java` (hoặc `@SpringBootTest(webEnvironment=RANDOM_PORT)` riêng nếu cần `TestRestTemplate`/`MockMvc` thật)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-004, TC-COND-027`
**Oracle Source:** `CB-CONREQ-IMP-001` §8.1/§9 (v1.3)

**Test Steps:**
1. Seed: Mother, Expert `APPROVED+ACTIVE`. 1 `clientRequestId` duy nhất (`sharedKey`)
2. 2 thread gọi `POST /api/v1/consultation-requests` (qua `MockMvc`/`TestRestTemplate`) với CÙNG `sharedKey` + CÙNG payload, đồng thời (`CountDownLatch`)
3. Assert: đúng 1 response có status `201`, response còn lại có status `200`; cả 2 response có CÙNG `id` trong body; `SELECT COUNT(*) FROM consultation_requests WHERE client_request_id=?` → `1`
4. Assert: đúng 1 `REQUEST_CREATED` event/audit/notification; request thua trả existing sau early lookup hoặc double-check sau Expert lock.

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-013 — Concurrency THẬT: accept vs `setTrustStatus`, 2 scenario ordering-controlled (v1.4 mới — Task 5, mandatory)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.accept()`'s row-lock đồng bộ THẬT với `ExpertProfileServiceImpl.setTrustStatus` (v1.4 — mở rộng `findByIdForUpdate` sang method này, xem ADR-CONREQ-005 Task 2)
**Test File:** `ConsultationRequestConcurrencyIntegrationTest.java` (file có sẵn — thêm 2 method mới, mirror cấu trúc `CONREQ-TC-INT-010`)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-036`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-005 v1.4, §6.2 Scenario 3a/3b; cùng kỹ thuật latch-ordering như `CONREQ-TC-INT-010` (không xác suất)

**Preconditions:** PostgreSQL container thật, KHÔNG mock; seed riêng cho mỗi scenario (Props Isolation): 1 `ConsultationRequest` PENDING, `ExpertProfile{verificationStatus=APPROVED, trustStatus=ACTIVE}`

**Scenario A — `setTrustStatus(SUSPENDED)` thắng lock trước (accept phải bị chặn đúng):**
1. `CountDownLatch trustDone = new CountDownLatch(1)`
2. Thread Trust: `expertProfileService.setTrustStatus(expertProfileId, TrustStatus.SUSPENDED, adminId)` (service thật, tự commit) → `trustDone.countDown()`
3. Thread Accept: `trustDone.await()` rồi mới gọi `consultationRequestService.accept(requestId, expertUserId)`
4. **Expected (PASS):** Thread Accept ném `CONREQ-004`; `SELECT status, direct_conversation_id FROM consultation_requests WHERE id=?` → `PENDING`, `NULL`; `SELECT COUNT(*) FROM direct_conversations WHERE ...` → `0`; không `NotificationRecord`/audit `REQUEST_ACCEPTED` nào

**Scenario B — Accept thắng lock trước (accept thành công, suspend sau đó hợp lệ):**
1. `CountDownLatch acceptDone = new CountDownLatch(1)`
2. Thread Accept: `consultationRequestService.accept(requestId, expertUserId)` → `acceptDone.countDown()`
3. Thread Trust: `acceptDone.await()` rồi mới gọi `expertProfileService.setTrustStatus(expertProfileId, TrustStatus.SUSPENDED, adminId)`
4. **Expected (PASS):** Thread Accept không throw, `status="ACCEPTED"`, `directConversationId` không null; Thread Trust không throw; `SELECT status FROM consultation_requests WHERE id=?` → `'ACCEPTED'`; `SELECT trust_status FROM expert_profiles WHERE id=?` → `'SUSPENDED'` — kết hợp này HỢP LỆ (suspend chạy sau accept-commit-boundary, chứng minh bằng lock)

**Tham số hoá cho REVOKED (nếu parameterize được cùng test, theo gợi ý Task 5):** lặp lại cả 2 scenario với `TrustStatus.REVOKED` thay vì `SUSPENDED` — cùng oracle, chỉ đổi giá trị enum.

**Expected Result (FAIL — dấu hiệu lỗi thiết kế thiếu Task 2/5):** nếu `setTrustStatus` vẫn dùng `findById` không lock (chưa áp dụng Task 2), Scenario A có xác suất > 0 FAIL (accept đọc thấy `ACTIVE` dù suspend đã commit trước đó) — race không tất định, khác hẳn hành vi lock thật.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-014 — Directory/verified-list THẬT: chỉ trả APPROVED+ACTIVE, search/specialty/pagination không bypass trust filter (v1.4 mới — Task 6, ADR-CONREQ-012)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileRepository.searchDirectory`/`findVerifiedPublic`/`findVerifiedBySpecialty`/`findApprovedSpecialties`
**Test File:** `src/test/java/com/carebridge/backend/expert/repository/ExpertProfileRepositoryIntegrationTest.java` (Testcontainers PostgreSQL — file mới hoặc thêm vào file repository test có sẵn nếu tồn tại)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-037`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-012

**Test Steps:**
1. Seed 3 expert cùng specialty `"Sản khoa"`: E1 (`APPROVED`, `ACTIVE`), E2 (`APPROVED`, `SUSPENDED`), E3 (`APPROVED`, `REVOKED`); seed thêm E4 (`APPROVED`, `SUSPENDED`) với specialty riêng `"Nhi khoa — chỉ E4"` (không expert nào khác có specialty này)
2. `findVerifiedPublic()` → chỉ chứa E1, KHÔNG chứa E2/E3
3. `searchDirectory(null, null, pageable)` → `content` chỉ chứa E1; `searchDirectory("Sản khoa", null, pageable)` → chỉ E1; `searchDirectory(null, "E2-tên-thật", pageable)` (search theo tên của E2) → rỗng (search `q` KHÔNG bypass trust filter dù tên khớp)
4. `findApprovedSpecialties()` → KHÔNG chứa `"Nhi khoa — chỉ E4"` (specialty đó chỉ tồn tại ở 1 expert ineligible, không được liệt kê)
5. Pagination: seed thêm đủ E1-biến-thể để có > 1 trang, assert `totalElements`/`totalPages` đúng SAU khi lọc trust (không đếm luôn E2/E3/E4 rồi lọc sau — phải lọc tại tầng SQL)

**Expected Result (FAIL — dấu hiệu gap Task 6):** nếu 4 query chỉ lọc `verification_status='APPROVED'` (hành vi trước v1.4) → bước 2/3/4 FAIL vì E2/E3/specialty-của-E4 vẫn xuất hiện.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-015 — `findOrCreate` THẬT bị chặn khi expert mất eligibility, kể cả đường trực tiếp ngoài consultation request (v1.4 mới — Task 7, cross-domain `directchat`)

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectConversationServiceImpl.findOrCreate()` với `DirectConversationPolicyImpl.assertExpertEligibleForConsultation` thật
**Test File:** `src/test/java/com/carebridge/backend/directchat/service/impl/DirectConversationServiceImplIntegrationTest.java` (file có sẵn — thêm test method mới, cross-domain)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-038`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-013

**Test Steps:**
1. Seed expert `APPROVED`+`SUSPENDED`, chưa có `DirectConversation` nào với mother
2. Act: `directConversationService.findOrCreate(motherUserId, expertProfileId)` (gọi trực tiếp, KHÔNG qua `ConsultationRequestServiceImpl.accept` — mô phỏng đường "Trò chuyện" trực tiếp từ hồ sơ chuyên gia)
3. Assert: throws `DirectChatException` code `DCC-002`; `SELECT COUNT(*) FROM direct_conversations WHERE ...` → `0` (không tạo)
4. **Regression — existing conversation vẫn đọc được:** seed 1 `DirectConversation` ĐÃ TỒN TẠI trước đó (tạo khi expert còn eligible), rồi expert bị suspend; `getConversation`/`markRead` (method đọc, KHÔNG qua `findOrCreate`) vẫn thành công — xem `assertIsParticipant` không đổi (Task 8, đọc lịch sử không bị chặn)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-016 — Gửi tin nhắn/khởi tạo call mới bị chặn khi expert mất eligibility; kết thúc call đã ANSWERED KHÔNG bị ảnh hưởng (v1.4 mới — Task 8, cross-domain `directchat`)

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectMessageServiceImpl.sendMessage()`, `ConversationCallServiceImpl.initiateCall()`/`.end()`
**Test File:** `src/test/java/com/carebridge/backend/directchat/service/impl/DirectMessageServiceImplIntegrationTest.java` + `ConversationCallServiceImplIntegrationTest.java` (file có sẵn — thêm test method mới)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-039`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-013, ADR-DCC-007 §2

**Test Steps:**
1. Seed `DirectConversation` đã tồn tại; expert bị suspend (`APPROVED`+`SUSPENDED`)
2. `sendMessage(...)` → throws `DirectChatException` code `DCC-010`; không insert `direct_messages` row mới
3. `initiateCall(...)` → throws `DCC-010`; không tạo `ConversationCall` row mới
4. **Regression — end-of-ANSWERED-call exemption (ADR-DCC-007 §2, KHÔNG đổi):** seed 1 `ConversationCall{status=ANSWERED}` cho conversation đó; `end(conversationId, callId, callerUserId)` → **KHÔNG throw** dù expert đang `SUSPENDED` (nhánh `answered` cố ý bỏ qua `assertConversationWritable` — session không bị kẹt); call chuyển `status=ENDED` thành công
5. **Regression — cancellable-call vẫn bị chặn:** seed 1 `ConversationCall{status=INITIATED}` (chưa `ANSWERED`); `end(...)` bởi caller → throws `DCC-010` (nhánh "cancellable" VẪN gọi `assertConversationWritable`, không phải exemption)

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-017 — Concurrency THẬT: `findOrCreate` vs `setTrustStatus`/`rejectExpert`, 2 scenario ordering-controlled (v1.5 mới — Problem 1, ADR-CONREQ-013, mandatory)

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectConversationServiceImpl.findOrCreate()`'s row-lock THẬT (v1.5 — `findByIdForUpdate`, đóng TOCTOU với `approveExpert`/`rejectExpert`/`setTrustStatus`)
**Test File:** `src/test/java/com/carebridge/backend/directchat/service/impl/DirectConversationConcurrencyIntegrationTest.java` (file mới — mirror cấu trúc `ConsultationRequestConcurrencyIntegrationTest`/`CONREQ-TC-INT-013`)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-042`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-013 (v1.5), §6.2.1 Scenario A/B; cùng kỹ thuật latch-ordering như `CONREQ-TC-INT-013`

**Preconditions:** PostgreSQL container thật, KHÔNG mock; seed riêng cho mỗi scenario: `ExpertProfile{verificationStatus=APPROVED, trustStatus=ACTIVE}`, chưa có `DirectConversation` nào giữa cặp (mother, expert) này.

**Scenario A — `setTrustStatus(SUSPENDED)` thắng lock trước (findOrCreate phải bị chặn đúng):**
1. `CountDownLatch trustDone = new CountDownLatch(1)`
2. Thread Trust: `expertProfileService.setTrustStatus(expertProfileId, TrustStatus.SUSPENDED, adminId)` (tự commit) → `trustDone.countDown()`
3. Thread Interaction: `trustDone.await()` rồi mới gọi `directConversationService.findOrCreate(motherUserId, expertProfileId)`
4. **Expected (PASS):** Thread Interaction ném `DirectChatException` (`expertNotEligibleForConsultation`, `DCC-002`); `SELECT COUNT(*) FROM direct_conversations WHERE mother_user_id=? AND expert_user_id=?` → `0`; không audit `DIRECT_CONVERSATION_OPENED` nào

**Scenario B — `findOrCreate` thắng lock trước (thành công, suspend sau đó hợp lệ):**
1. `CountDownLatch createDone = new CountDownLatch(1)`
2. Thread Interaction: `directConversationService.findOrCreate(motherUserId, expertProfileId)` → `createDone.countDown()`
3. Thread Trust: `createDone.await()` rồi mới gọi `setTrustStatus(expertProfileId, TrustStatus.SUSPENDED, adminId)`
4. **Expected (PASS):** Thread Interaction không throw, trả về `conversationId` hợp lệ; `SELECT COUNT(*) FROM direct_conversations WHERE ...` → `1`; Thread Trust không throw — kết hợp "conversation đã tạo + expert SUSPENDED" là HỢP LỆ vì suspend chạy sau commit-boundary

**Expected Result (FAIL — dấu hiệu thiếu Problem 1 fix):** nếu `findOrCreate` vẫn dùng `findById` không lock, Scenario A có xác suất > 0 FAIL (đọc thấy `ACTIVE` dù suspend đã commit trước đó) — race không tất định.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-018 — Concurrency THẬT: `sendMessage` vs `setTrustStatus`, 2 scenario ordering-controlled (v1.5 mới — Problem 1, ADR-CONREQ-013, mandatory)

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectMessageServiceImpl.sendMessage()`'s row-lock THẬT (v1.5 — `findByUserIdForUpdate`, MỚI thêm vào method này)
**Test File:** `src/test/java/com/carebridge/backend/directchat/service/impl/DirectMessageServiceConcurrencyIntegrationTest.java` (file mới)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-043`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-013 (v1.5), §6.2.1 Scenario A/B

**Preconditions:** PostgreSQL container thật; seed 1 `DirectConversation` đã tồn tại giữa (mother, expert); `ExpertProfile{verificationStatus=APPROVED, trustStatus=ACTIVE}`.

**Scenario A — `setTrustStatus(SUSPENDED)` thắng lock trước (sendMessage phải bị chặn đúng):**
1. `CountDownLatch trustDone = new CountDownLatch(1)`
2. Thread Trust: `setTrustStatus(expertProfileId, SUSPENDED, adminId)` → commit → `trustDone.countDown()`
3. Thread Interaction: `trustDone.await()` rồi `directMessageService.sendMessage(conversationId, senderUserId, request)`
4. **Expected (PASS):** throws `DirectChatException` (`expertUnavailableForWrite`, `DCC-010`); `SELECT COUNT(*) FROM direct_messages WHERE conversation_id=?` không tăng; `direct_conversations.last_activity_at` KHÔNG đổi; không publish `MESSAGE_SENT`/audit `DIRECT_MESSAGE_SENT`

**Scenario B — `sendMessage` thắng lock trước (thành công, suspend sau đó hợp lệ):**
1. `CountDownLatch sendDone = new CountDownLatch(1)`
2. Thread Interaction: `sendMessage(...)` → commit → `sendDone.countDown()`
3. Thread Trust: `sendDone.await()` rồi `setTrustStatus(expertProfileId, SUSPENDED, adminId)`
4. **Expected (PASS):** Thread Interaction không throw, message persist thành công; Thread Trust không throw — message đã gửi TRƯỚC khi suspend là hợp lệ

**Expected Result (FAIL — dấu hiệu thiếu Problem 1 fix):** nếu `sendMessage` chưa thêm `findByUserIdForUpdate`, Scenario A có xác suất > 0 FAIL.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-019 — Concurrency THẬT: `initiateCall` (representative call-start) vs `setTrustStatus`/`REVOKED`, ordering-controlled (v1.5 mới — Problem 1, ADR-CONREQ-013, mandatory)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConversationCallServiceImpl.initiateCall()`'s row-lock THẬT (v1.5 — `findByUserIdForUpdate`, MỚI thêm vào method này; đại diện cho `markRinging`/`answer`/`decline`/`end`-cancellable, cùng pattern)
**Test File:** `src/test/java/com/carebridge/backend/directchat/service/impl/ConversationCallConcurrencyIntegrationTest.java` (file mới)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-044`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-013 (v1.5), §6.2.1 Scenario A/B

**Preconditions:** PostgreSQL container thật; seed 1 `DirectConversation` đã tồn tại; `ExpertProfile{verificationStatus=APPROVED, trustStatus=ACTIVE}`.

**Scenario A — trust action thắng lock trước, tham số hóa `SUSPENDED`/`REVOKED`:**
1. `CountDownLatch trustDone = new CountDownLatch(1)`
2. Thread Trust: `setTrustStatus(expertProfileId, {SUSPENDED|REVOKED}, adminId)` → commit → `trustDone.countDown()`
3. Thread Interaction: `trustDone.await()` rồi `conversationCallService.initiateCall(conversationId, callerUserId, CallType.VIDEO)`
4. **Expected (PASS, cả 2 tham số):** throws `DirectChatException` (`DCC-010`); `SELECT COUNT(*) FROM conversation_calls WHERE conversation_id=?` không tăng; không Zego token nào được sinh (verify `zegoCloudService` không bị gọi hoặc token không persist); không publish `CALL_INITIATED`/audit

**Scenario B — `initiateCall` thắng lock trước (thành công, trust action sau đó hợp lệ):**
1. `CountDownLatch callDone = new CountDownLatch(1)`
2. Thread Interaction: `initiateCall(...)` → commit → `callDone.countDown()`
3. Thread Trust: `callDone.await()` rồi `setTrustStatus(expertProfileId, SUSPENDED, adminId)`
4. **Expected (PASS):** Thread Interaction không throw, `ConversationCall{status=INITIATED}` persist; Thread Trust không throw

**Expected Result (FAIL — dấu hiệu thiếu Problem 1 fix):** nếu `initiateCall` chưa thêm `findByUserIdForUpdate`, Scenario A có xác suất > 0 FAIL cho cả 2 tham số.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-020 — Deterministic lock sequencing: answered cleanup không lấy Expert lock; cancellable branch phải lấy lock và bị gate (v1.6 rewrite)

**Severity:** `CRITICAL`
**Feature Under Test:** `ConversationCallServiceImpl.end()` — nhánh `answered` PHẢI không lock/không gate; nhánh `cancellable` PHẢI lock/gate (v1.5 — MỚI thêm lock ở nhánh này)
**Test File:** `ConversationCallConcurrencyIntegrationTest.java` (2 test method)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-045`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-013 §6.2.1 Scenario C, ADR-DCC-007 §2

**Harness bắt buộc cho cả hai test:** PostgreSQL Testcontainers; Spring `TransactionTemplate`; service transaction thật; `ExecutorService/Future`; transaction/thread độc lập. Nếu cần biết chính xác một repository lock invocation đã bắt đầu, dùng test-only spy/hook quanh method repository trong test context; không thêm sleep/debug hook vào production và không đổi transaction propagation. Mọi test dùng `try/finally` để luôn nhả latch, resolve/rollback helper transaction và shutdown executor.

**Test A — Answered-call cleanup không lấy Expert lock:**

1. Seed conversation + `ConversationCall{status=ANSWERED}`; Expert `APPROVED+ACTIVE`.
2. Tạo `trustLockAcquired`, `releaseTrust`, `endCompleted`.
3. Thread Trust chạy trong `TransactionTemplate`: gọi `findByIdForUpdate(expertProfileId)`, signal `trustLockAcquired`, giữ transaction chưa commit bằng `releaseTrust.await()`.
4. Sau `trustLockAcquired`, Thread End gọi service thật `end(...)` cho call ANSWERED; chỉ sau khi service return thành công mới `endCompleted.countDown()`.
5. Main thread chờ `endCompleted` bằng timeout rộng chỉ để chống treo suite; trong khi `releaseTrust.getCount()==1`, assert completion đã xảy ra và query transaction độc lập thấy call chuyển `ENDED`.
6. Chỉ sau các assertion trên mới `releaseTrust.countDown()`.

**Business oracle:** completion/`ENDED` xảy ra trong lúc trust transaction vẫn sở hữu Expert lock. Nếu answered branch vô tình gọi `findByUserIdForUpdate`, nó không thể signal completion trước release và test fail theo sequencing. Không so milliseconds, không yêu cầu “chạy dưới X ms”.

**Test B — Cancellable branch phải lấy lock và bị gate:**

1. Seed call `INITIATED` hoặc `RINGING`; Expert `APPROVED+ACTIVE`.
2. Thread Trust trong transaction riêng khóa Expert, đổi trust thành `SUSPENDED` hoặc `REVOKED` nhưng giữ chưa commit; signal `trustLockAcquired`.
3. Thread Cancel-call gọi service thật `end(...)`. Test-only spy/hook signal `cancelLockAttempted` khi `findByUserIdForUpdate` được gọi.
4. Sau khi cả `trustLockAcquired` và `cancelLockAttempted` đã signal, assert `cancelCompleted` chưa signal/Future chưa done; query độc lập xác nhận call chưa `CANCELLED`, không event/audit interaction.
5. `releaseTrust.countDown()` cho Trust commit. Cancel-call tiếp tục dưới lock, đọc trạng thái mới và phải hoàn tất bằng `DirectChatException DCC-010`.
6. Assert call vẫn `INITIATED`/`RINGING`; không `CANCELLED`, không event/audit interaction, không deadlock hoặc DB exception lạ.

**Business oracle:** ownership + latch sequencing chứng minh cancellable branch thật sự chờ cùng Expert lock rồi gate theo trạng thái đã commit. Timeout rộng chỉ là safety guard; không benchmark, fixed sleep, latency threshold hoặc `assertTimeout` làm oracle.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-021 — Read/mark-read behavior matrix THẬT: Mother luôn qua; Expert chỉ gate bởi verification (KHÔNG BAO GIỜ trust); write/call bị chặn trong CÙNG fixture (v1.5 mới — Problem 3, ADR-CONREQ-013 Behavior Matrix, mandatory)

**Severity:** `CRITICAL`
**Feature Under Test:** `DirectConversationServiceImpl.getConversation()`/`getTimeline()`(qua `DirectMessageServiceImpl`)/`markRead()` — tất cả qua `assertIsParticipant`, KHÔNG qua `assertConversationWritable`
**Test File:** `src/test/java/com/carebridge/backend/directchat/service/impl/DirectConversationReadBoundaryIntegrationTest.java` (file mới)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-046`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-013 Behavior Matrix (v1.5), `DirectConversationPolicyImpl.java` dòng 19-33

**Test Steps (mỗi fixture dưới đây dùng CHUNG 1 `DirectConversation` đã có message):**
1. `ExpertProfile{verificationStatus=REJECTED}` (verification mất): Mother gọi `getConversation`/`getTimeline`/`markRead` → tất cả PASS (không throw); Expert gọi cùng 3 method → tất cả throws `expertNoLongerApproved()`
2. `ExpertProfile{verificationStatus=APPROVED, trustStatus=SUSPENDED}`: Mother gọi 3 method → PASS; **Expert gọi 3 method → PASS (KHÔNG throw — đúng chủ đích, trust không ảnh hưởng đọc)**
3. `ExpertProfile{verificationStatus=APPROVED, trustStatus=REVOKED}`: giống bước 2 — Mother và Expert đều đọc/mark-read được
4. **Ranh giới đọc/ghi trong CÙNG fixture bước 2/3:** cùng conversation, cùng `ExpertProfile{APPROVED, SUSPENDED/REVOKED}` — gọi `sendMessage`/`initiateCall` (Mother hoặc Expert) → PHẢI throw `DCC-010` (chứng minh đọc PASS nhưng ghi tương tác mới BỊ CHẶN trong CÙNG state, không phải do fixture khác nhau)
5. **Assertion lock:** verify không method đọc nào (`getConversation`/`getTimeline`/`markRead`) từng gọi `findByIdForUpdate`/`findByUserIdForUpdate` (đọc thường, không lock — dùng Testcontainers + query `pg_locks` hoặc Spy trên repository để xác nhận không có lời gọi phương thức lock nào)

**Expected Result (FAIL trước khi có test):** test chưa tồn tại; nếu 1 implementation sai vô tình mở rộng `assertIsParticipant` để check `trustStatus` (vi phạm Task 8/Problem 3), bước 2/3 phần Expert sẽ FAIL (throw nhầm) — bắt đúng regression.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-022 — Create key mới: moderation giữ Expert lock trước, commit ineligible, create bị CONREQ-002 và zero side effect

**Severity:** `CRITICAL`
**Feature Under Test:** `ConsultationRequestServiceImpl.create()` + `ExpertProfileServiceImpl.setTrustStatus/rejectExpert`
**Test File:** `ConsultationRequestConcurrencyIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-049`
**Oracle Source:** TDS ADR-CONREQ-004/005 v1.6, BR-CONREQ-001

**Harness:** Testcontainers PostgreSQL; service thật; `TransactionTemplate`; 2 thread/transaction độc lập; `CountDownLatch adminLockAcquired`, `createLockAttempted`, `releaseAdmin`; test-only spy/hook quanh `findByIdForUpdate` nếu cần xác nhận create đã tới điểm chờ lock.

**Steps:**
1. Seed Mother + Expert `APPROVED+ACTIVE`; chưa có row với `newKey`.
2. Thread Admin khóa Expert trước, đổi thành `SUSPENDED`/`REVOKED` hoặc `REJECTED`, signal `adminLockAcquired`, chờ `releaseAdmin`.
3. Thread Create gọi service thật với `newKey`; signal `createLockAttempted` khi bắt đầu `findByIdForUpdate`.
4. Khi Admin vẫn giữ lock, assert không có `consultation_requests` row cho key, không `REQUEST_CREATED`, notification hoặc audit create.
5. Release Admin để commit ineligible. Create tiếp tục, đọc trạng thái mới dưới lock và trả `CONREQ-002`.
6. Assert cuối: zero row/event/notification/audit create; không DB exception/deadlock.

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-023 — Create key mới giữ Expert lock trước, insert/commit rồi moderation chạy sau

**Severity:** `CRITICAL`
**Feature Under Test:** cùng lock protocol, ordering ngược với INT-022
**Test File:** `ConsultationRequestConcurrencyIntegrationTest.java`
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-049`
**Oracle Source:** TDS ADR-CONREQ-005 v1.6

**Harness:** Testcontainers + transaction thật + latches. Dùng test-only synchronization seam quanh `ConsultationRequestWriter.insertIfAbsent` sau lock/check để signal `createReachedInsert` và giữ bằng `releaseCreate`; Admin thread signal `adminLockAttempted` khi gọi workflow thật.

**Steps:**
1. Seed Expert `APPROVED+ACTIVE`, key mới.
2. Thread Create lock Expert, double-check key empty, check eligible, tới writer seam và signal `createReachedInsert` trong transaction chưa commit.
3. Thread Admin bắt đầu `setTrustStatus`/`rejectExpert`; xác nhận đã attempt lock nhưng chưa hoàn thành khi Create còn giữ lock.
4. Release Create; create insert + commit PENDING, `created=true`, HTTP/service result tương ứng; đúng 1 `REQUEST_CREATED` event/audit/notification.
5. Admin tiếp tục sau commit và đổi Expert thành ineligible.
6. Assert request PENDING đã tạo giữ nguyên; Expert cuối ineligible. Đây là hợp lệ vì moderation xảy ra sau create commit boundary.

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-TC-INT-024 — Idempotent retry sau trust loss trả cùng resource HTTP 200, không lock hoặc side effect lần hai

**Severity:** `CRITICAL`
**Feature Under Test:** full stack Controller → Service idempotency ordering
**Test File:** `ConsultationRequestConcurrencyIntegrationTest.java` hoặc HTTP integration class cùng fixture
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-033, TC-COND-049`
**Oracle Source:** TDS ADR-CONREQ-004 v1.6, API §9

**Steps:**
1. Expert eligible; POST key `stableKey` + payload P → HTTP 201, lưu `resourceId`; assert một row/event/audit/notification.
2. Admin workflow thật commit `SUSPENDED`/`REVOKED` hoặc `REJECTED`.
3. POST lại cùng `stableKey` + đúng payload P.
4. Assert HTTP 200, body cùng `resourceId`, service result `created=false`.
5. Spy/verify retry không gọi `findByIdForUpdate`; DB/event assertions giữ đúng một row, một `REQUEST_CREATED`, một notification và một audit create.
6. Sub-case cùng key + payload khác vẫn `409 CONREQ-009`, cũng không lock/insert/side effect.

**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

## 5. Red-Green-Refactor Tracker

### 5.0. Implementation Evidence — v1.7

- `84/84` contract IDs có coverage mapping trong test implementation; các contract liên quan được gom thành test method/sub-case có cùng fixture và oracle.
- Scoped backend command chạy `111` test methods, `0` failure/error.
- Flutter feature-focused chạy `17/17`; full Flutter suite chạy `89/89`.
- Clean PostgreSQL Flyway run áp dụng thành công `80` migrations, gồm 2 migration feature.
- Concurrency tests dùng transaction/thread độc lập và `CountDownLatch`/controlled seam; timeout chỉ chống treo, không dùng latency làm business oracle.
- Không có retained log đầy đủ chứng minh trạng thái RED riêng cho toàn bộ `84` contract IDs. Vì vậy cột RED và Red Gate tổng quát không được backfill thành PASS; GREEN hiện tại là bằng chứng thực thi đáng tin cậy.

> Cột **Test File** trong tracker giữ tên file dự kiến của v1.6 để bảo toàn traceability thiết kế. Implementation thực tế hợp nhất các contract vào các file executable chính: `ConsultationRequestServiceImplCreateTest`, `ConsultationRequestServiceImplLifecycleTest`, `ConsultationRequestServiceImplListTest`, `ConsultationRequestControllerTest`, `ConsultationRequestApiIntegrationTest`, `ConsultationRequestConcurrencyIntegrationTest`, `ConsultationRequestAcceptLockConcurrencyIntegrationTest`, `AnsweredCallCleanupLockIntegrationTest`, `DirectChatWriteLockConcurrencyIntegrationTest`, `DirectChatReadBoundaryIntegrationTest`, cùng 3 file Flutter trong `test/features/consultation/`.

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `CONREQ-TC-001` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-002` | `ConsultationRequestControllerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-003` | `ConsultationRequestControllerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-004` | `ConsultationRequestControllerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-005` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-006` | `ConsultationRequestPolicyTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-007` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-008` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-009` | `ConsultationRequestControllerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-010` | `ConsultationRequestPolicyTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-011` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-012` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-013` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-013b` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-013c` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-014` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-015` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-016` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-017` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-018` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-019` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-020` | `ConsultationRequestNotificationListenerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-021` | `ConsultationRequestNotificationServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-022` | `ConsultationRequestNotificationServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-023` | `ConsultationRequestControllerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-024` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-025` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-026` | `ConsultationRequestControllerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-027` | `FcmServiceOverloadTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-028` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-029` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-030` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-031` | `ConsultationRequestControllerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-032` | `ConsultationRequestControllerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-034` | `directchat/policy/DirectConversationPolicyImplTest.java` (cross-domain) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-035` | `directchat/policy/DirectConversationPolicyImplTest.java` (cross-domain) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-036` | `expert/mapper/ExpertProfileMapperTest.java` (cross-domain) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-037` | `ConsultationRequestServiceImplTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-038` | `directchat/policy/DirectConversationPolicyImplTest.java` (cross-domain, v1.5) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-SEC-001` | `ConsultationRequestControllerTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-SEC-002` | `ConsultationRequestPolicyTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-SEC-003` | `ConsultationRequestPolicyTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-SEC-004` | `ConsultationRequestRepositoryIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-001` | `ConsultationRequestConcurrencyIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-002` | `ConsultationRequestListIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-003` | `ConsultationRequestAcceptIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-004` | `ConsultationRequestConcurrencyIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-005` | `ConsultationRequestConcurrencyIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-006` | `ConsultationRequestExpiryIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-007` | `ConsultationRequestNotificationIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-008` | `ConsultationRequestNotificationIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-009` | (any Testcontainers integration test) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-010` | `ConsultationRequestConcurrencyIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-011` | `ConsultationRequestAcceptIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-012` | `ConsultationRequestConcurrencyIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-013` | `ConsultationRequestConcurrencyIntegrationTest.java` | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-014` | `expert/repository/ExpertProfileRepositoryIntegrationTest.java` (cross-domain) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-015` | `directchat/service/impl/DirectConversationServiceImplIntegrationTest.java` (cross-domain) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-016` | `directchat/service/impl/DirectMessageServiceImplIntegrationTest.java` + `ConversationCallServiceImplIntegrationTest.java` (cross-domain) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-017` | `directchat/service/impl/DirectConversationConcurrencyIntegrationTest.java` (cross-domain, v1.5) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-018` | `directchat/service/impl/DirectMessageServiceConcurrencyIntegrationTest.java` (cross-domain, v1.5) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-019` | `directchat/service/impl/ConversationCallConcurrencyIntegrationTest.java` (cross-domain, v1.5) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-020` | `directchat/service/impl/ConversationCallConcurrencyIntegrationTest.java` (cross-domain, v1.5) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-021` | `directchat/service/impl/DirectConversationReadBoundaryIntegrationTest.java` (cross-domain, v1.5) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-022` | `ConsultationRequestConcurrencyIntegrationTest.java` (create, v1.6) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-023` | `ConsultationRequestConcurrencyIntegrationTest.java` (create, v1.6) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-TC-INT-024` | `ConsultationRequestConcurrencyIntegrationTest.java` / HTTP integration (retry, v1.6) | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |
| `CONREQ-FL-01`..`17` | xem §4-mobile bên dưới | N/A — complete retained RED log unavailable | 🟢 Passed (v1.7 scoped suite) | Consolidated implementation tests |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ConsultationRequestServiceImpl implements IConsultationRequestService {
    @Override public CreateConsultationRequestResult create(CreateConsultationRequestRequest r, UUID u) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override public Page<ConsultationRequestSummaryResponse> listMine(UUID u, ConsultationRequestStatus s, Pageable p) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override public Page<ConsultationRequestSummaryResponse> listAssigned(UUID u, ConsultationRequestStatus s, Pageable p) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override public ConsultationRequestResponse getById(UUID id, UUID u) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override public ConsultationRequestResponse accept(UUID id, UUID u) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override public ConsultationRequestResponse reject(UUID id, UUID u, String reason) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override public ConsultationRequestResponse cancel(UUID id, UUID u) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override public ConsultationRequestPendingSummaryResponse pendingSummary(UUID u) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
    @Override public int expireOverdueRequests() {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `CONREQ-TC-001..032 (trừ 013c), 037, SEC-001..004, INT-001..013` | `throw('Not implemented')` (class mới `ConsultationRequestServiceImpl` chưa tồn tại) | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `CONREQ-TC-013c` | `throw('Not implemented')` (cùng class mới) | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `CONREQ-TC-034, 035, INT-015, INT-016` | Chạy test MỚI đối với code HIỆN TẠI, CHƯA sửa của `DirectConversationPolicyImpl`/`ConversationCallServiceImpl` (v1.4 — đây là sửa đổi method có sẵn, không phải stub-throws, vì class đã tồn tại từ trước feature này) — method hiện tại chỉ check `verificationStatus`, chưa check `trustStatus` | 🔴 FAIL (case `trust=SUSPENDED/REVOKED` phải fail vì code hiện tại sai cho pass) | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Test không thực sự exercise nhánh trust |
| `CONREQ-TC-036, INT-014` | Chạy test MỚI đối với code HIỆN TẠI, CHƯA sửa của `ExpertProfileMapper`/`ExpertProfileRepository` (cùng lý do — sửa đổi method có sẵn) | 🔴 FAIL (field/filter chưa tồn tại) | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state |
| `CONREQ-FL-01, 17` | Chạy test MỚI đối với widget HIỆN TẠI (đọc `verificationStatus`, chưa có `isConsultationEligible`) | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology |
| `CONREQ-TC-035 (rewrite), INT-017, INT-018, INT-019` | Chạy test MỚI đối với code HIỆN TẠI, CHƯA sửa của `DirectConversationServiceImpl.findOrCreate`/`DirectMessageServiceImpl.sendMessage`/`ConversationCallServiceImpl.initiateCall` — hiện dùng `findById`/`findByUserId` KHÔNG lock, `assertConversationWritable` vẫn nhận `DirectConversation` (signature cũ) — chưa có `findByUserIdForUpdate` (v1.5, chưa tồn tại trong `ExpertProfileRepository`) | 🔴 FAIL (Scenario A của mỗi INT test phải fail/không tất định vì thiếu lock; `CONREQ-TC-035` fail vì signature/behavior chưa đổi) | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Race giả (thiếu latch thật) |
| `CONREQ-TC-INT-020` | Test A dùng transaction/thread độc lập và latch để yêu cầu `endCompleted` trước `releaseTrust`; Test B xác nhận `cancelLockAttempted` rồi vẫn blocked trước release, sau commit ineligible bị DCC-010 | 🔴 FAIL Test B trên code hiện tại; Test A có thể PASS ngay | ☐ FAIL ☐ PASS | ☐ Không được thay sequencing bằng milliseconds/benchmark/sleep |
| `CONREQ-TC-INT-022, 023, 024` | Chạy trên code hiện tại chưa có consultation create implementation/row-lock protocol. Ordering phải được điều khiển bằng transaction thật + latch/barrier; INT-024 phải exercise HTTP 201/200 | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Stub-only ☐ Race xác suất ☐ Retry bị re-gate |
| `CONREQ-TC-038, INT-021` | Chạy test MỚI đối với `DirectConversationPolicyImpl.assertIsParticipant`/`markRead`/`getConversation`/`getTimeline` HIỆN TẠI — các method này KHÔNG đổi code ở v1.5, chỉ audit hành vi thật; **kỳ vọng PASS ngay** vì hành vi hiện tại đã đúng theo matrix (không phải RED thật — xem ghi chú) | 🟢 PASS ngay (không cần sửa code — đây là test XÁC NHẬN hành vi có sẵn đúng, không phải test chờ implementation mới) | ☐ FAIL ☐ PASS | ☐ Nếu FAIL → code hiện tại KHÔNG khớp Behavior Matrix đã audit, cần điều tra lại (không phải "chưa implement") |

**Red Gate Evidence — post-implementation record:**
- Stub commit hash: `N/A — không commit theo yêu cầu User`
- Tất cả FAIL trước implementation? ☐ Không đủ retained log để xác nhận toàn bộ `84` IDs; **không claim GATE-2 PASS hồi tố**
- GREEN evidence: scoped backend `111/111`, Flutter feature `17/17`, Flutter full `89/89`
- **Ghi chú v1.4:** các TC cross-domain (`034/035/036/INT-014/015/016`, `FL-01/17`) test method ĐÃ CÓ SẴN trong codebase (không phải class mới) — Red phase của chúng được chứng minh bằng cách chạy test mới trước khi sửa code, không phải bằng 1 stub `throw`. Đây là TDD chuẩn cho "sửa hành vi có sẵn", không sai lệch so với Red Gate Protocol — chỉ khác cơ chế tạo ra trạng thái RED.
- **Ghi chú v1.5/v1.6:** `CONREQ-TC-038`/`INT-021` và `INT-020` Test A xác nhận behavior có sẵn; PASS ngay có thể đúng. `INT-020` Test B và `INT-022/023/024` phải RED trước implementation. Mọi concurrency test phải chứng minh ordering bằng lock ownership/latch, không bằng latency.

---

## 4-mobile. Flutter Test Cases

> Test file gốc: `05_Development/CareBridgeMobileApp/test/features/consultation/`. Widget test dùng `flutter_test` + mock HTTP layer (theo convention hiện có của project — không Dio, dùng `http` package qua `apiGet/apiPost/apiPatch`, mock qua injectable client theo pattern đã dùng ở test hiện có của `DirectChatService`/`CommunityService`).

### CONREQ-FL-01 — Mother: CTA "Yêu cầu tư vấn" + "Trò chuyện" hiện đúng điều kiện `isConsultationEligible` (v1.4 rewrite — Task 10, KHÔNG còn `isApproved`)

**Test File:** `test/features/expert/screens/expert_public_profile_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.1, ADR-CONREQ-010 bổ sung (v1.4)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-041`

**Steps:** Render `ExpertPublicProfileScreen` với `profile['isConsultationEligible'] == false` (fixture — không quan tâm `verificationStatus` là gì) → CẢ 2 nút "Yêu cầu tư vấn" VÀ "Trò chuyện" (nút có sẵn, sửa nhất quán tối thiểu — Task 10) đều bị disable. Với `profile['isConsultationEligible'] == true` → cả 2 nút enabled.
**Expected Result (FAIL — dấu hiệu regression):** nếu widget vẫn đọc `profile['verificationStatus'] == 'APPROVED'` (hành vi trước v1.4), test FAIL ở case `verificationStatus='APPROVED'` nhưng `isConsultationEligible=false` (xem `CONREQ-FL-17`).
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-17 — Mother: CTA vẫn disable đúng khi `verificationStatus=='APPROVED'` nhưng `isConsultationEligible==false` (v1.4 mới — Task 10, regression chứng minh mobile không tự suy đoán từ verification)

**Test File:** `test/features/expert/screens/expert_public_profile_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.1, ADR-CONREQ-013 (backend đóng đường bypass), ADR-CONREQ-010 bổ sung (mobile contract)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)
**Condition Ref:** `TC-COND-041`

**Steps:** Render `ExpertPublicProfileScreen` với fixture `{ verificationStatus: 'APPROVED', isConsultationEligible: false }` (mô phỏng expert `APPROVED`+`SUSPENDED/REVOKED` — DTO thật không expose `trustStatus`, chỉ boolean dẫn xuất) → CẢ 2 CTA vẫn disable (KHÔNG được enable chỉ vì `verificationStatus=='APPROVED'`). Đây chính là regression test chứng minh widget đọc đúng field mới, không quay lại đọc `verificationStatus` một mình.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-02 — Mother: form validate topic/description required trước khi submit

**Test File:** `test/features/consultation/screens/consultation_request_form_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.1
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Submit form rỗng → hiển thị lỗi validate, KHÔNG gọi API.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-03 — Mother: double-submit guard — nút disable ngay sau lần nhấn đầu

**Test File:** `consultation_request_form_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.1, §13.6
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Tap nút Gửi 2 lần liên tiếp (trước khi Future đầu resolve) → API chỉ được gọi đúng 1 lần (mock client `verify(callCount == 1)`).
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-04 — Mother: danh sách request — loading/empty/error/retry

**Test File:** `test/features/consultation/screens/my_consultation_requests_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.2
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** 3 sub-case: (a) mock API pending → hiện `CircularProgressIndicator`; (b) mock API trả `[]` → hiện empty state đúng text; (c) mock API throw → hiện error state + nút "Thử lại" → tap gọi lại API.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-05 — Mother: detail screen hiện nút "Hủy yêu cầu" chỉ khi PENDING

**Test File:** `test/features/consultation/screens/consultation_request_detail_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.2
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Render với `status=PENDING` → nút hiện; `status=ACCEPTED` → nút "Mở hội thoại" hiện thay vào, nút hủy KHÔNG hiện.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-06 — Expert: queue lấy dữ liệu thật, không hardcode

**Test File:** `test/features/consultation/screens/expert_request_queue_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.3 (khác biệt với `ExpertHomeService` cũ — không fallback dữ liệu giả)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Mock API trả 2 request thật với tên/topic cụ thể → UI hiển thị đúng field từ response, KHÔNG chứa bất kỳ chuỗi hardcode nào (`"Mẹ bé An Nhiên"` không xuất hiện trong widget tree khi mock trả dữ liệu khác).
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-07 — Expert: Accept/Reject double-tap guard

**Test File:** `expert_request_queue_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.3
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Tap "Chấp nhận" 2 lần liên tiếp trên cùng item → API accept chỉ gọi đúng 1 lần.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-08 — Expert: sau accept/reject, item biến mất khỏi filter PENDING + badge refresh

**Test File:** `expert_request_queue_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.3, §13.5
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Accept 1 item khi đang filter PENDING → item biến mất khỏi list hiện tại; mock `pending-summary` (Issue 11 — đổi tên từ `unread-summary`) gọi lại (verify invocation) để cập nhật badge tab.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-09 — Không có nested bottom navigation trong segment "Tư vấn"/"Cộng đồng" (Issue 9 fix — đúng widget type thật)

**Test File:** `test/features/home/screens/expert_home_shell_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` ADR-CONREQ-010, §13.3 (v1.2 — `expert_home_shell.dart:86` dùng Material 3 `NavigationBar` thật, KHÔNG phải `BottomNavigationBar`)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Render `ExpertHomeShell`, chuyển tới tab "Yêu cầu tư vấn" → `find.byType(NavigationBar)` (v1.2 — KHÔNG phải `find.byType(BottomNavigationBar)`, widget đó không tồn tại trong code thật) trả về đúng **1** widget trong toàn bộ tree (chỉ của shell, không có `NavigationBar`/`BottomNavigationBar` thứ 2 bên trong `ExpertRequestsTabScreen`/segment nào — chỉ có `TabBar`, một widget type hoàn toàn khác).
**Expected Result (FAIL — dấu hiệu lỗi v1.1):** `find.byType(BottomNavigationBar)` sẽ trả về **0** widget (không phải 1) vì code thật không dùng widget này ở đâu cả — test v1.1 sẽ luôn fail sai lý do (tìm nhầm widget), không thật sự kiểm chứng được gì.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-10 — Dashboard: card "Yêu cầu tư vấn" dùng endpoint thật, xoá fallback giả

**Test File:** `test/features/home/screens/expert_app_home_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.4
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Mock `GET /consultation-requests/assigned` trả lỗi → card hiện trạng thái rỗng/lỗi thật, KHÔNG hiện `"Mẹ bé An Nhiên"`/`"Tư vấn dinh dưỡng dặm"` (chuỗi hardcode cũ) trong widget tree.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-11 — FCM foreground: `type=CONSULTATION_REQUEST` trigger refresh badge

**Test File:** `test/core/notifications/fcm_service_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.5
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Giả lập `RemoteMessage.data['type'] = 'CONSULTATION_REQUEST'` qua `onMessage` stream → assert refresh-bus notify được gọi.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-12 — FCM tap routing (cold start + background) → mở đúng request detail

**Test File:** `fcm_service_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.5
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** `resolveTapRoute({'type': 'CONSULTATION_REQUEST', 'requestId': uuid})` → trả về `/consultation-requests/{uuid}`; test riêng cho `getInitialMessage()` (cold start) dẫn tới cùng route qua `_schedulePendingFlush`.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-13 — Role-correct empty state / labels (Mother vs Expert)

**Test File:** `my_consultation_requests_screen_test.dart`, `expert_request_queue_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.2, §13.3
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Mother empty-state text khác Expert empty-state text (không dùng chung 1 string generic "Không có dữ liệu").
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-14 — Stale response không ghi đè filter/state mới (generation guard)

**Test File:** `expert_request_queue_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.6 (mirror `_unreadLoadGeneration` pattern)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Đổi filter từ PENDING → ACCEPTED trước khi request PENDING cũ resolve (dùng `Completer` để kiểm soát thứ tự) → khi request cũ resolve trễ, KHÔNG ghi đè danh sách ACCEPTED hiện tại bằng dữ liệu PENDING cũ.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-15 — Notification Center: icon/label switch trên `type=='CONSULTATION'`, KHÔNG phải `'CONSULTATION_REQUEST'` (Issue 4 fix)

**Test File:** `test/features/notification/screens/notification_center_screen_test.dart`, `notifications_screen_test.dart`, `notification_detail_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.5 (v1.2 — 2 contract độc lập; đây là contract #2, đọc `NotificationRecordResponse.type` thật, giá trị `"CONSULTATION"`, KHÔNG phải FCM push data map's `type` key dùng bởi `fcm_service.dart`)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Render list với 1 `NotificationRecordResponse` có `type: "CONSULTATION"`, `referenceType: "CONSULTATION_REQUEST"` → icon/label hiển thị đúng case mới (`Icons.medical_services_outlined` hoặc tương đương), KHÔNG rơi vào fallback default (chứng minh switch đã match đúng case `'CONSULTATION'`).
**Expected Result (FAIL — dấu hiệu lỗi v1.1):** nếu code thêm nhầm `case 'CONSULTATION_REQUEST':` (khớp `referenceType`, không khớp `type` thật) thay vì `case 'CONSULTATION':`, switch sẽ rơi vào `default` fallback icon — test phát hiện đúng sai lệch này vì nó assert ĐÚNG icon mới, không phải fallback.
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

### CONREQ-FL-16 — Notification Center: tap-to-navigate dùng `referenceType`/`referenceId`, mở đúng request detail (Issue 4 fix)

**Test File:** `test/features/notification/screens/notification_center_screen_test.dart`
**Oracle Source:** `CB-CONREQ-IMP-001` §13.5 (`notification_center_screen.dart:236`)
**TDD Phase:** 🟢 GREEN — implemented and passing (v1.7 scoped suite)

**Steps:** Tap 1 item có `referenceType: "CONSULTATION_REQUEST"`, `referenceId: X` → điều hướng tới `/consultation-requests/X` (dùng `referenceType` làm discriminator, KHÔNG dùng `type` — vì `type` chỉ là `"CONSULTATION"`, coarse-grained, không đủ chính xác nếu domain booking sau này tái dùng cùng giá trị).
**Current Status:** 🟢 Passing — covered by executable test method or consolidated sub-case

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] TDS `CB-CONREQ-IMP-001` đã được review và approve
- [x] Logic Issues (§2) đã được confirm
- [x] Migration `V20260716200500`/`V20260716200501` đã được approved và validate trên PostgreSQL sạch; chưa chạy shared Supabase
- [x] Test fixtures (§3 TDS-05) đã chuẩn bị

### Exit Criteria (DoD)
- [x] Scoped Maven suite — tất cả `111/111` unit/integration tests liên quan `consultation`, `notification`, `expert`, `directchat` xanh
- [x] Các integration tests feature dùng PostgreSQL/Testcontainers xanh; clean Flyway migration cũng xanh
- [x] `flutter test` — tất cả `CONREQ-FL-*` (01-17) xanh; full suite `89/89`
- [ ] `flutter analyze` — 0 lỗi mới trong file đã sửa
- [ ] Test coverage ≥ 80% lines cho `ConsultationRequestServiceImpl`, `ConsultationRequestPolicy`
- [x] Không có business logic trong `ConsultationRequestController`
- [x] Không có PII/secret plaintext trong FCM data/log contract; payload chỉ có `type` và `requestId`
- [x] Không có nested bottom navigation (CONREQ-FL-09 pass)
- [x] Race conditions bắt buộc (§TDS-03 TC-COND-012/013/028/036/049) có integration test pass thật với transaction/thread và latch/controlled seam
- [x] IDOR regression (`CONREQ-TC-026`) pass — stable business response fields giống nhau cho not-found/not-participant
- [x] (v1.4) Eligibility predicate nhất quán (`CONREQ-TC-006/013c/034/035/036, INT-013/014/015/016`) pass
- [x] (v1.4) End-of-ANSWERED-call exemption pass
- [x] (v1.5) DirectChat write-path lock protocol pass
- [x] (v1.6) `INT-020` deterministic sequencing pass với transaction/thread độc lập và cleanup `finally`
- [x] (v1.5) Read/mark-read behavior matrix pass
- [x] (v1.5) ADR-CONREQ-008/009 `Status: Accepted`; normative grep sweep sạch
- [x] (v1.6) Create key mới dùng `findByIdForUpdate` + double-check key; cả hai lock ordering scenario pass
- [x] (v1.6) Retry sau trust loss trả cùng id HTTP 200/`created=false`, không side effect lần hai

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1) — tất cả test FAIL với stub trước khi implement
- [x] Contract Existence — backend package/compile không lỗi
- [x] Props Isolation — test dùng fixture độc lập, transaction rollback/cleanup, không shared mutable state
- [x] Oracle Source — expected values được map về TDS/ADR và contract IDs

> **Verification limitation:** full repository `./mvnw test` không xanh do baseline failures ngoài feature; `flutter analyze` crash với `FormatException: Unexpected end of input` trước khi trả diagnostics; line coverage chưa được đo. Ba gate này giữ unchecked và không được suy diễn từ scoped PASS.

### Suspension Criteria
- Migration bị chặn bởi checksum drift cũ trên shared Supabase dev DB (không được `repair`) — dừng, báo User, KHÔNG tự ý sửa 3 migration cũ
- Phát hiện lỗi kiến trúc mới cần Tech Lead review

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "DROP INDEX IF EXISTS uq_notification_records_consultation_request;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "DROP TABLE IF EXISTS consultation_requests CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "DELETE FROM flyway_schema_history WHERE version IN ('20260716200500','20260716200501');"

# Revert implementation
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/ConsultationRequestNotificationServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/job/ConsultationRequestNotificationOutboxJob.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/consultation/
git checkout -- 05_Development/CareBridgeMobileApp/test/features/consultation/

# v1.4 — cross-domain files (expert + directchat), revert riêng từng file, KHÔNG cả thư mục
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/entity/ExpertProfile.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/repository/ExpertProfileRepository.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/service/impl/ExpertProfileServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/dto/response/ExpertProfileResponse.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/dto/response/ExpertProfileDetailResponse.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/mapper/ExpertProfileMapper.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/policy/IDirectConversationPolicy.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/policy/DirectConversationPolicyImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/service/impl/DirectConversationServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/exception/DirectChatException.java
git checkout -- 05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_public_profile_screen.dart

# v1.5 — cross-domain lock protocol (Problem 1): 2 file directchat MỚI bị đụng vòng này
# (không bị đụng bởi v1.4) — revert riêng, cùng với test file mới của các test case v1.5
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/service/impl/DirectMessageServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/service/impl/ConversationCallServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR (vd: test giả định có `consultation_bookings` row sau accept) | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase (vd: `ConsultationNotificationService` bị nhầm dùng thay vì `ConsultationRequestNotificationServiceImpl`; hoặc assert shape lỗi `{"error":{"code":...}}` thay vì `ErrorResponse` thật) | ☐ | G-3 |
| AP-AI-006 | TOCTOU Shortcut | Test mock `findById`/`assertExpertStillApproved()` (không lock) hoặc `tryAccept`'s EXISTS thay vì `findByIdForUpdate` row-lock trước `tryTransition` (v1.3 — Issue 1 vòng 2) | ☐ | G-1 |
| AP-AI-007 | Idempotency Coalescing | Test còn oracle theo "1 PENDING/cặp" thay vì `clientRequestId` (Issue 6 — rule đã bị User bác bỏ) | ☐ | G-1 |
| AP-AI-008 | Actor Impersonation | Test không assert `actorUserId==null, actorType==SYSTEM` cho `REQUEST_EXPIRED` (Issue 8) | ☐ | G-1 |
| AP-AI-009 | IDOR Leak | Test còn oracle 403 `CONREQ-003` cho not-participant thay vì 404 `CONREQ-007` (Issue 2) | ☐ | G-1 |
| AP-AI-010 | Lock Order Inversion | Test không verify thứ tự `findByIdForUpdate` TRƯỚC `findOrCreate`/`tryTransition`, hoặc mock cho phép lock ngược (v1.3 — Issue 1 vòng 2) | ☐ | G-1 |
| AP-AI-011 | Fake Concurrency | `CONREQ-TC-INT-010`/`CONREQ-TC-INT-011` dùng sequential call thay vì `CountDownLatch`/spy+latch ordering-controlled thật, hoặc race với `setTrustStatus` thay vì `rejectExpert` (v1.3 — Issue 1/4 vòng 2) | ☐ | G-2 ★ |
| AP-AI-012 | Silent Status Guess | `CONREQ-TC-031`/`CONREQ-TC-032` không verify Controller đọc `result.created()` — chấp nhận controller luôn trả 201 hoặc tự suy đoán (v1.3 — Issue 3 vòng 2) | ☐ | G-1 |
| AP-AI-013 | Partial Eligibility Check (v1.4) | `CONREQ-TC-006`/`013c`/`034`/`035`/`INT-013`/`INT-014` chỉ mock/assert `verificationStatus`, không có case `trust=SUSPENDED/REVOKED` nào — hoặc `setTrustStatus` trong test/impl vẫn `findById` không lock | ☐ | G-1 |
| AP-AI-014 | Leaky Eligibility Message (v1.4) | Test oracle cho `CONREQ-002`/`CONREQ-004`/`DCC-002` còn assert message cũ tiết lộ verification-vs-trust (vd. `"not APPROVED"`) thay vì message generic mới | ☐ | G-1 |
| AP-AI-015 | Missing Exemption Regression (v1.4) | `CONREQ-TC-INT-016` thiếu case "end call ANSWERED KHÔNG bị chặn" — nếu thiếu, 1 implementation sai (chặn luôn cả exemption ADR-DCC-007 §2) vẫn có thể pass toàn bộ suite mà không bị phát hiện | ☐ | G-1 |
| AP-AI-016 | DirectChat TOCTOU Shortcut (v1.5) | `CONREQ-TC-INT-017/018/019` dùng sequential call thay vì `CountDownLatch` ordering-controlled thật (race xác suất); hoặc `CONREQ-TC-035` vẫn mock `expertProfileRepository.findByUserId`/nhận `DirectConversation` (signature cũ) thay vì `ExpertProfile` đã lock | ☐ | G-2 ★ |
| AP-AI-017 | Overbroad Read-Contract Claim (v1.5) | `CONREQ-TC-038`/`CONREQ-TC-INT-021` chỉ assert chung "đọc được"/"mark-read được" mà KHÔNG phân biệt riêng từng trường hợp Mother/Expert × verification/trust (theo Behavior Matrix ADR-CONREQ-013) — hoặc thiếu assertion "write/call bị chặn trong CÙNG fixture" để chứng minh ranh giới đọc/ghi | ☐ | G-1 |
| AP-AI-018 | Create Race by Probability (v1.6) | `INT-022/023` chỉ start 2 thread gần nhau, dùng sleep hoặc hy vọng scheduler tạo đúng ordering; không có latch xác nhận lock owner/attempt | ☐ | G-2 ★ |
| AP-AI-019 | Latency as Lock Oracle (v1.6) | `INT-020` dùng milliseconds, “under X ms”, benchmark, fixed sleep hoặc `assertTimeout` làm business oracle; hoặc giữ lock/call service trong cùng thread/transaction | ☐ | G-2 ★ |
| AP-AI-020 | Retry Re-gated (v1.6) | `TC-007`/`INT-024` không chứng minh retry sau trust loss trả existing trước Expert lock, hoặc cho phép event/notification/audit lần hai | ☐ | G-1 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| | | | | |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol.*
*Document ID `CB-CONREQ-IMP-001-TEST`. Version `1.7`. Status: Approved — post-implementation sync; DPO sign-off pending.*
