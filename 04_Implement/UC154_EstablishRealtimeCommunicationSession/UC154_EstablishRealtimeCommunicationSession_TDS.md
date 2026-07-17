# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# UC-154 Establish Realtime Communication Session

| Field | Value |
|-------|-------|
| **Document ID** | `CB-CON-IMP-004` |
| **Version** | `1.0` |
| **Date** | `2026-06-26` |
| **Status** | `Draft` *(reverted 2026-07-15 — repurposed for voice/video calls, see CHANGELOG)* |
| **Author** | `AI Agent` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDS cho UC-154 |
| 2026-07-15 | AI Agent — Amelia (Dev Agent) | **Implemented & tested:** `IZegoCloudService`/`ZegoCloudServiceImpl`/`ZegoToken04Generator` (`com.carebridge.backend.integration.zegocloud`) — Token04 generation using JDK crypto + Jackson (no new pom dependency; the project's own prior `TokenServerAssistant.java`/`Base64.java` reference was used as the byte-format source of truth and rewritten to remove the `org.json.simple` dependency it required). 10 unit tests passing (`ZegoToken04GeneratorTest` x7, `ZegoCloudServiceImplTest` x3): roomId=sessionId, TTL/appId correctness, validation rejections, no dependency on any repository (pure token generator), token never contains the secret as plaintext. Wired as a collaborator into UC-95's join endpoint (only integration point — this service exposes no HTTP endpoint of its own, per §9). Env config added: `carebridge.zego.app-id`/`server-secret`/`token-ttl-seconds` in `application.yaml`, `ZEGO_APP_ID`/`ZEGO_SERVER_SECRET` in `.env.example` (placeholders only). |
| 2026-07-15 | AI Agent — Technical Architect | **Schema realignment (blocking correction):** original draft was written against a single `consultations` table with `mother_joined_at`/`expert_joined_at` columns and a `zego_sessions` table — none of which exist in the actual schema (confirmed via `V1__init_schema.sql`). Realigned to the real `consultation_bookings` → `consultation_sessions` split: `roomId` = `consultation_sessions.session_id` (stored in `communication_room_id`), no `zego_sessions` table, no per-participant join timestamps (not modeled in schema — session-level `session_status`/`started_at`/`ended_at` only, owned by UC-95). Narrowed `IZegoCloudService` to token generation only; join/end state mutation moves to UC-95's `ConsultationSessionService` (single owner of `session_status`, avoids two services racing to mutate the same row). Confirmed ZIM (ZegoCloud in-app messaging/signaling) reuses the identical Token04 format as the RTC token already specified here — one token endpoint serves both voice/video (UC-145/146) and chat signaling (UC-144). Resolved per user decision 2026-07-15: build UC-154→UC-95→UC-144 as one vertical slice; ZegoCloud SDK confirmed as transport (not custom WebSocket). |
| 2026-07-15 | AI Agent — Technical Architect | **REPURPOSED — reverted Approved → Draft.** User decision: ZegoCloud ZIM must **not** be used for chat delivery at all (chat now uses Firebase Realtime Database for the realtime signal, PostgreSQL REST for durable history); ZegoCloud is reserved **exclusively for voice/video calls**, and calls are no longer session-scoped — they belong to a `direct_conversations`/`conversation_calls` model (see `04_Implement/UC144_DirectConsultChat/`), not `consultation_sessions`. `IZegoCloudService.generateToken(roomId, userId, userName)` (the token-generation core built and tested in this pass) is **kept and reused as-is** — its interface is already `sessionId`-agnostic (just a `roomId` string), so it now accepts `conversation_calls.zego_room_id` instead of `consultation_sessions.session_id`. `ZegoZimSignalingPort` (mobile/web ZIM client adapters) is **deleted** — it was chat-signaling code, not call code, and is unused by the RTC-only redesign. This TDS's ADR-ZEGO-001 (roomId format) needs a follow-up ADR update to reference `conversation_calls` once the new TDS is approved; tracked there, not rewritten here. |

---

## MỤC LỤC

1. [Tổng quan Module](#1-tổng-quan-module)
2. [Ma trận Truy vết](#2-ma-trận-truy-vết-traceability-matrix)
3. [Architecture Decision Records (ADR)](#3-architecture-decision-records-adr)
4. [Non-Functional Requirements & SLA](#4-non-functional-requirements--sla)
5. [Static Modeling](#5-static-modeling)
6. [Dynamic Modeling](#6-dynamic-modeling)
7. [Domain Event Catalog](#7-domain-event-catalog)
8. [Interface Specification](#8-interface-specification)
9. [API Specification](#9-api-specification)
10. [Bảng mã lỗi](#10-bảng-mã-lỗi)
11. [Quy trình Triển khai](#11-quy-trình-triển-khai-step-by-step)
12. [Rollback & Incident Runbook](#12-rollback--incident-runbook)
13. [Kịch bản Kiểm thử Chi tiết](#13-kịch-bản-kiểm-thử-chi-tiết)
14. [Phương pháp Xác minh](#14-phương-pháp-xác-minh)
15. [Mẫu thử thực tế](#15-mẫu-thử-thực-tế-api-verification-samples)
16. [Bảng tổng hợp phân quyền](#16-bảng-tổng-hợp-phân-quyền-authorization-matrix)
17. [AI Prompt Constraints](#17-ai-prompt-constraints-case-20)

---

## 1. Tổng quan Module

| Field | Value |
|-------|-------|
| **Module Name** | `EstablishRealtimeCommunicationSession` |
| **Bounded Context** | `consultation` |
| **Data Classification** | `Confidential` |
| **Upstream Dependencies** | `consultations (CONFIRMED), ZegoCloud SDK` |
| **Downstream Consumers** | `audit, zego_sessions` |

**Mô tả:** Tạo phòng (room) và token ZegoCloud cho phiên tư vấn đã được xác nhận. Duy trì trạng thái chat/voice/video. Endpoint được gọi khi UC-77 (JoinConsultationSession) được trigger. Chức năng này là supporting service cho session lifecycle.

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại | Mô tả | Thành phần Code | Compliance Target | ADR liên quan |
|----------------|------|-------|-----------------|-------------------|---------------|
| UC-154 | Use Case | Thiết lập phiên giao tiếp realtime qua ZegoCloud | `RealtimeSessionController`, `ZegoCloudService` | BR-RBAC | ADR-ZEGO-001 |
| BR-TOKEN-SERVER | Business Rule | Token generate server-side, không lưu DB | `ZegoCloudService` | BR-SECURITY | ADR-ZEGO-001 |
| BR-ROOM-ID | Business Rule | roomId = consultation UUID | `ZegoCloudService` | BR-CONSISTENCY | ADR-ZEGO-001 |
| BR-SESSION-TRACK | Business Rule | Track join/leave timestamps | `ZegoCloudService` | BR-AUDIT | ADR-ZEGO-002 |

---

## 3. Architecture Decision Records (ADR)

### ADR-ZEGO-001 — ZegoCloud room lifecycle management (realigned to `consultation_sessions`)

| Field | Value |
|-------|-------|
| **Status** | `Accepted` (realigned 2026-07-15 — see CHANGELOG) |
| **Date** | `2026-06-26`, realigned `2026-07-15` |

#### Quyết định
- `roomId` = `consultation_sessions.session_id` (UUID string) — **not** a separate "consultation" identifier. Persisted into `consultation_sessions.communication_room_id` on first successful token issuance (owned/written by UC-95's `ConsultationSessionService`, not by this service — see ADR-ZEGO-002).
- Room created lazily on first join — no pre-create room API call.
- Token: generate using ZegoCloud Server SDK `generateToken04()` (Token04 format) with user ID = the caller's `users.user_id` (UUID string).
- **Token04 is shared across ZegoCloud products** — the same `IZegoCloudService.generateToken()` call issues tokens for both the RTC room (UC-145 voice / UC-146 video, via ZegoExpressEngine) and the ZIM (in-app messaging/signaling) login used for chat realtime delivery (UC-144). One token endpoint, two SDK surfaces on the client.
- Token TTL: 3600 seconds (1 hour).
- When the session reaches a terminal `session_status` (`COMPLETED`/`NO_SHOW`/`CANCELLED`, per UC-95 ADR-SESSION-001): ZegoCloud room simply auto-expires from inactivity; this service does not need to explicitly tear down the room.

### ADR-ZEGO-002 — Session state tracking (superseded — ownership moved to UC-95)

| Field | Value |
|-------|-------|
| **Status** | `Superseded by UC-95 ADR-SESSION-001/003` (realigned 2026-07-15) |
| **Date** | `2026-06-26`, superseded `2026-07-15` |

#### Quyết định (original, now superseded)
~~`zego_sessions` table tracks `mother_joined_at`, `expert_joined_at`, `ended_at`.~~ **This table does not exist and will not be created.** The actual schema (`consultation_sessions`) has no per-participant join-timestamp columns — only session-level `session_status`, `started_at`, `ended_at`. Introducing a new `zego_sessions` table purely to track join timestamps would be a new migration for data UC-95 already models at the session level; rejected as unnecessary schema surface.

**Corrected ownership boundary:** `IZegoCloudService` (this TDS) is scoped **strictly to stateless token generation** — it does not read or write `session_status`, `started_at`, or `ended_at`. All session-state transitions (`WAITING → IN_SESSION` on first join, `→ COMPLETED` on explicit end, `→ NO_SHOW` on grace-period timeout) are owned exclusively by UC-95's `ConsultationSessionService`, which **calls** `IZegoCloudService.generateToken()` as a collaborator and then itself updates `consultation_sessions` (see UC-95 TDS §ADR-SESSION-001/003, §6.1 sequence). This prevents two services from racing to mutate the same row.

---

## 4. Non-Functional Requirements & SLA

### 4.1. Performance & Availability

| Category | Requirement | Target SLA |
|----------|-------------|------------|
| Latency | Token generation (p99) | < 200ms |
| Availability | Uptime (monthly) | 99.9% |
| Token TTL | ZegoCloud token | 3600 seconds (1 hour) |

### 4.2. Security

| Category | Requirement | Target |
|----------|-------------|--------|
| Access control | Consultation participants only | Least privilege (§16) |
| Token storage | NOT persisted in DB | Ephemeral only |
| Secret management | appId, serverSecret from env | Never hardcoded |

---

## 5. Static Modeling

> Class diagram tham chiếu §8 Interface Specification.

### 5.1. Key Components

- `RealtimeSessionController` — API endpoint for session establishment
- `ZegoCloudService` — token generation, session lifecycle management
- `ConsultationRepository` — consultation status and participant verification

---

## 6. Dynamic Modeling

### 6.1. Establish Session Sequence

```plantuml
@startuml
actor Participant
participant RealtimeSessionController
participant ZegoCloudService
participant ConsultationRepository
participant ZegoCloud

Participant -> RealtimeSessionController: POST /sessions/{consultationId}/establish
RealtimeSessionController -> ZegoCloudService: establishSession(consultationId, accountId)
ZegoCloudService -> ConsultationRepository: findById(consultationId)
ZegoCloudService -> ZegoCloudService: validateParticipant(consultation, accountId)
ZegoCloudService -> ZegoCloud: generateToken04(appId, accountId, serverSecret, 3600)
ZegoCloud --> ZegoCloudService: token
ZegoCloudService -> ConsultationRepository: updateJoinTimestamp(accountId)
ZegoCloudService --> RealtimeSessionController: {roomId=consultationId, token}
RealtimeSessionController --> Participant: 200 OK
@enduml
```

---

## 7. Domain Event Catalog

### 7.1. Events Published

| Event Name | Trigger | Publisher | Async? |
|------------|---------|-----------|--------|
| SessionEstablished | Realtime session created for consultation | ZegoCloudService | No |
| SessionEnded | Consultation completed/cancelled | ZegoCloudService | No |

### 7.2. Events Consumed

| Event Name | Source | Handler | Action |
|------------|--------|---------|--------|
| PaymentConfirmed | PaymentService | — | Consultation becomes eligible for session |

---

## 8. Interface Specification

```java
public interface IZegoCloudService {
    /**
     * Generates a stateless ZegoCloud Token04 for a valid, already-authorized
     * consultation-session participant. Does NOT read or write session state —
     * callers (UC-95 ConsultationSessionService, UC-144 ConsultationChatService)
     * are responsible for their own authorization checks before calling this.
     * Same token format serves both RTC (voice/video) and ZIM (chat signaling).
     * @param sessionId  consultation_sessions.session_id (= roomId, string form)
     * @param userId     caller's users.user_id as string
     * @param userName   display name for UI
     * @return ZegoTokenDto with roomId and token string
     */
    ZegoTokenDto generateToken(String sessionId, String userId, String userName);
}
```

> **Narrowed from the original draft:** `recordJoin()`/`endSession()` are
> **removed** from this interface (realigned 2026-07-15). Session-state
> mutation (`session_status`, `started_at`, `ended_at`,
> `communication_room_id`) is owned exclusively by UC-95's
> `ConsultationSessionService`. This service is a pure, stateless token
> generator — this is deliberate (see ADR-ZEGO-002) to avoid two services
> racing to write the same row.

---

## 9. API Specification

This service does not expose its own HTTP endpoint. `generateToken()` is
called internally as a collaborator by:
- UC-95's `POST /api/v1/consultations/sessions/{sessionId}/join` (session join → RTC/ZIM token bundle, see UC-95 TDS §9)
- UC-144's message-send/list flow, when a client requests a fresh chat-signaling token (see UC-144 TDS §9)

**Token structure returned (embedded in the caller's response, e.g. UC-95's `JoinSessionResponse`):**
```json
{
  "sessionId": "uuid",
  "roomId": "uuid",
  "zegoToken": "04AAAAAGxxxxxxxx...",
  "tokenExpiresAt": "2026-06-26T11:00:00Z",
  "zegoAppId": 12345678
}
```

---

## 10. Bảng mã lỗi

| Code | HTTP | Trigger |
|------|------|---------|
| `ZEGO-001` | 503 | ZegoCloud SDK call failed |
| `ZEGO-002` | 403 | Caller not a participant |
| `ZEGO-003` | 409 | Session already ended |

---

## 11. Quy trình Triển khai (Step-by-Step)

### 11.1. Prerequisites

- [ ] consultations table tồn tại (V31)
- [ ] ZEGO_APP_ID và ZEGO_SERVER_SECRET env vars đã set
- [ ] ZegoCloud server SDK dependency đã thêm

### 11.2. Pre-Migration Checklist

Không có migration mới — UC-154 dùng consultations table hiện tại.

### 11.3. Implementation Steps

#### Chặng 1 — Implement ZegoTokenService

```java
@Service
public class ZegoTokenService {
    @Value("${zego.app-id}") private long appId;       // from env only
    @Value("${zego.server-secret}") private String secret; // from env only

    public String generateToken(String roomId, String userId) {
        // ZegoCloud SDK token generation
        // Token TTL = 1 hour
        // Token NOT persisted to DB
        return TokenServerAssistant.generateToken04(appId, userId, secret, 3600, "");
    }
}
```

#### Chặng 2 — Caller-side usage (owned by UC-95/UC-144, shown here for reference only)

`IZegoCloudService` itself does **not** implement join/session logic — that
is owned by UC-95's `ConsultationSessionService` (RTC join) and UC-144's
`ConsultationChatService` (chat signaling token). Illustrative caller usage:

```java
// Inside UC-95's ConsultationSessionService.joinSession(sessionId, currentUserId) —
// authorization + session_status transition are UC-95's own responsibility,
// this service is called ONLY after that succeeds:
try {
    ZegoTokenDto token = zegoCloudService.generateToken(
        sessionId.toString(), currentUserId.toString(), displayName);
    // session_status mutation happens in UC-95's own code, not here
} catch (Exception e) {
    throw new SessionServiceUnavailableException("SES-005"); // session_status NOT changed
}
```

### 11.4. Deployment Checklist

- [ ] ZEGO_APP_ID/ZEGO_SERVER_SECRET env vars set
- [ ] Test join → 200 với zegoToken
- [ ] Verify token NOT in DB
- [ ] Verify roomId == consultationId
- [ ] Test ZegoCloud failure → 503, status unchanged

---

## 12. Rollback & Incident Runbook

### 12.1. Điều kiện kích hoạt Rollback

| Điều kiện | Ngưỡng | Người quyết định |
|-----------|--------|------------------|
| Token persisted to DB | Bất kỳ | Tech Lead + DPO |
| Non-participant can join | Bất kỳ | Tech Lead |
| ZEGO credentials in code | Bất kỳ | Tech Lead |

### 12.2. Rollback Procedure

```bash
# Code-only rollback
kubectl rollout undo deployment/carebridge-api
```

---

## 13. Kịch bản Kiểm thử Chi tiết

```gherkin
Feature: Establish Realtime Communication Session
  Scenario: roomId = consultationId
    When joinSession(CON-001, MOTHER-001)
    Then response.roomId == CON-001.toString()

  Scenario: Token NOT in DB
    When joinSession() thành công
    Then verify zegoToken KHÔNG tồn tại trong bất kỳ DB table nào

  Scenario: mother_joined_at recorded
    When MOTHER-001 join session lần đầu
    Then consultation.mother_joined_at NOT NULL

  Scenario: First joiner → IN_SESSION
    Given consultation.status = CONFIRMED
    When joinSession()
    Then consultation.status = IN_SESSION

  Scenario: ZegoCloud SDK failure → 503
    Given zegoTokenService throws exception
    When joinSession()
    Then response 503 ZEGO-001
    And consultation.status KHÔNG thay đổi

  Scenario: Non-participant → 403
    Given ACC-OTHER không phải participant
    When joinSession(CON-001, ACC-OTHER)
    Then throws ForbiddenException ZEGO-002
```

---

## 14. Phương pháp Xác minh

```sql
-- Verify no token column in consultations
SELECT column_name FROM information_schema.columns
WHERE table_name = 'consultations' AND column_name LIKE '%token%';
-- Expected: 0 rows

-- Verify IN_SESSION status after join
SELECT id, status, mother_joined_at FROM consultations WHERE id = '<uuid>';
```

---

## 15. Mẫu thử thực tế (API Verification Samples)

```bash
curl -X POST https://[host]/api/v1/consultations/CON-UUID/join \
  -H "Authorization: Bearer <PARTICIPANT_JWT>"
# Expected 200:
# { "consultationId": "CON-UUID", "roomId": "CON-UUID",
#   "zegoToken": "04A...", "zegoAppId": 12345678,
#   "tokenExpiresAt": "2026-06-26T11:00:00Z" }
```

---

## 16. Bảng tổng hợp phân quyền (Authorization Matrix)

| Endpoint | `ROLE_MOTHER` | `ROLE_EXPERT` | `ROLE_ADMIN` |
|----------|---------------|---------------|--------------|
| `POST /consultations/{id}/join` | ✅ Participant | ✅ Participant | ✅ |
| `POST /consultations/{id}/end` | ❌ | ✅ Participant | ✅ |

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source |
|---|-----------|--------|
| C1 | ZEGO_APP_ID and ZEGO_SERVER_SECRET from environment only — never in code | ADR-ZEGO-001 |
| C2 | roomId = consultationId.toString() — do NOT generate separate room ID | ADR-ZEGO-001 |
| C3 | Token NOT stored in DB — stateless generation | ADR-CON-004 |
| C4 | On join: update consultation.status=IN_SESSION if first joiner | ADR-ZEGO-002 |
| C5 | ZegoCloud failure → HTTP 503 (SES-002); consultation status NOT changed | ADR-ZEGO-001 |

### 17.2 Constraint Injection Block

```
[CONSTRAINT BLOCK — Module: EstablishRealtimeCommunicationSession (CB-CON-IMP-004)]
1. (C1) ZEGO_APP_ID/ZEGO_SERVER_SECRET từ @Value env — KHÔNG hardcode.
2. (C2) roomId = consultationId.toString() — KHÔNG UUID.randomUUID().
3. (C3) zegoToken KHÔNG lưu DB — generate mỗi lần gọi, return trong response.
4. (C4) First joiner: status CONFIRMED → IN_SESSION + set mother_joined_at.
5. (C5) ZegoCloud exception → 503 ZEGO-001; KHÔNG thay đổi consultation status.
```

### 17.3 Constraint Quality Checklist

- [x] Mỗi constraint traceable về ADR
- [x] Constraint block có ≥ 5 constraints

### 17.4 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Hành động |
|-------|-------------|----------|
| AP-AI-001 | Token saved to DB column | Reject — C3, privacy risk |
| AP-AI-003 | roomId = UUID.randomUUID() | Reject — C2, session mismatch |
| AP-AI-005 | Status changed before token generation succeeds | Reject — C5 |

---

## PHỤ LỤC

### A. Glossary

| Thuật ngữ | Định nghĩa |
|-----------|------------|
| ZegoCloud | Video/audio conferencing SDK |
| roomId | ZegoCloud room identifier = consultationId |
| Stateless token | Token generated mỗi lần gọi, không lưu trữ |
| IN_SESSION | Consultation status khi ít nhất 1 participant đã join |

### B. Tài liệu tham chiếu

| Document | Path |
|----------|------|
| ZegoCloud SDK | [link] |
| EDS v2.0 Template | `08_References/Template/PHASE-3_TDS.md` |

---

*EDS v2.1 — Tích hợp CASE 2.0 AI Prompt Constraints (§17).*
