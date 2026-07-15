# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-154 Establish Realtime Communication Session

**Document ID:** `CB-CON-TDD-004`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft` *(reverted 2026-07-15 — token-generation core retained and reused for calls; ZIM-for-chat scenarios n/a, see UC144_DirectConsultChat)*
**Author:** `AI Agent`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo Test-Spec |
| 2026-07-15 | AI Agent — Amelia (Dev Agent) | **GREEN:** ZEGO-TC-001 through ZEGO-TC-005 all implemented and passing (10 JUnit tests total across `ZegoToken04GeneratorTest`/`ZegoCloudServiceImplTest` — some scenarios split into multiple assertions). Verified via `./mvnw test -Dtest=com.carebridge.backend.integration.zegocloud.**`: 10/10 passed, 0 failures. |
| 2026-07-15 | AI Agent — Technical Architect | Realigned to actual schema: no `zego_sessions` table, no `mother_joined_at`/`expert_joined_at`, no single `consultations` table. Scope narrowed to `IZegoCloudService.generateToken()` (stateless token generation only) per TDS realignment; join/session-state tests moved to UC-95's own Test-Spec. |
| 2026-07-15 | AI Agent — Technical Architect | **REPURPOSED — reverted Approved → Draft.** ZEGO-TC-001..005 (roomId format, TTL, no repository dependency, typed failure, env-only secrets) all still hold structurally and their 10 passing JUnit tests are **kept as-is** — only the semantic meaning of `roomId` changes (now `conversation_calls.zego_room_id`, not `consultation_sessions.session_id`), tracked in the new `UC144_DirectConsultChat` Test-Spec's call-token test cases rather than rewritten here. |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Exit Criteria](#6-exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-154` |
| **Module** | `EstablishRealtimeCommunicationSession — consultation` |
| **Priority** | 🟡 P2 |
| **Data Classification** | `Confidential` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "creates rooms or tokens" — no implementation detail | ADR-ZEGO-001 (realigned): lazy room creation, roomId=`consultation_sessions.session_id` | Test roomId format |
| L2 | SRS: "maintains chat, voice, video status" — no DB spec | ADR-ZEGO-002 (superseded): no `zego_sessions` table exists; session-state tracking (`session_status`/`started_at`/`ended_at`) is owned by UC-95, tested in UC-95's own Test-Spec | Test scope narrowed to token generation only here |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
EstablishRealtimeCommunicationSession bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-154 | Hành vi người dùng |
| ADR-CON | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-CON-IMP-004 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | ZEGO-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | ZEGO-TC-00X |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | Input validation | Valid/invalid input classes |
| Boundary Value Analysis | Limits (pagination, size) | Edge values |
| Error Guessing | Security vectors | OWASP Top 10 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| FX-001 | DB seed | Valid entity | Happy path |
| FX-002 | JWT | ROLE_MOTHER token | Auth context |

---

## 4. Test Case Specification

```java
class ZegoTokenTestFactory {
    static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
}
```

---

### ZEGO-TC-001 — Token generated with session_id as roomId

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-ZEGO-001`
**TDD Phase:** 🟢 GREEN

```java
ZegoTokenDto token = zegoCloudService.generateToken(SESSION_ID.toString(), USER_ID.toString(), "Dr. Lam");
assertThat(token.getRoomId()).isEqualTo(SESSION_ID.toString());
// NOT UUID.randomUUID() — roomId is always the session_id, never a separately generated id
```

**Current Status:** 🟢 Passing

---

### ZEGO-TC-002 — Token TTL is 3600 seconds and never persisted to any DB column

**Severity:** `HIGH`
**Oracle Source:** `ADR-ZEGO-001`
**TDD Phase:** 🟢 GREEN

```java
ZegoTokenDto token = zegoCloudService.generateToken(SESSION_ID.toString(), USER_ID.toString(), "Dr. Lam");
assertThat(token.getExpiresAt()).isCloseTo(Instant.now().plusSeconds(3600), within(5, ChronoUnit.SECONDS));
// Static check (code review / architecture test): no repository/entity field of type String named
// "token"/"zegoToken" exists anywhere under consultation.entity — token is generated fresh per call.
```

**Current Status:** 🟢 Passing

---

### ZEGO-TC-003 — generateToken() never reads or writes consultation_sessions

**Severity:** `HIGH`
**Oracle Source:** `ADR-ZEGO-002 (superseded — ownership boundary)`
**TDD Phase:** 🟢 GREEN

```java
// Verify ZegoCloudServiceImpl has no dependency on ConsultationSessionRepository —
// architecture/unit test asserting the class only depends on env-configured
// appId/serverSecret, not on any repository. Prevents an AI implementation from
// re-introducing session-state mutation into this service (see UC-95 for that ownership).
```

**Current Status:** 🟢 Passing

---

### ZEGO-TC-004 — ZegoCloud SDK failure surfaces as a checked exception, does not throw for the caller to swallow silently

**Severity:** `HIGH`
**Oracle Source:** `ADR-ZEGO-001`
**TDD Phase:** 🟢 GREEN

**Test Steps:**
1. Configure invalid/malformed server secret (simulates SDK-level failure)
2. Call `generateToken()`

**Expected Result:**
- throws `ZegoTokenGenerationException` — caller (UC-95/UC-144) is responsible for mapping this to `503` and leaving its own state unchanged (tested in UC-95/UC-144's own Test-Specs)

**Current Status:** 🟢 Passing

---

### ZEGO-TC-005 — appId/serverSecret sourced only from environment, never hardcoded

**Severity:** `CRITICAL`
**TDD Phase:** 🟢 GREEN

**Expected Result:** `@Value("${zego.app-id}")`/`@Value("${zego.server-secret}")` — grep for literal numeric appId or 32-char secret string anywhere in source finds none

**Current Status:** 🟢 Passing

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED confirmed | 🟢 GREEN (commit) |
|-------|--------|----------|
| `ZEGO-TC-001` | `[x]` | `Passed` |
| `ZEGO-TC-002` | `[x]` | `Passed` |
| `ZEGO-TC-003` | `[x]` | `Passed` |
| `ZEGO-TC-004` | `[x]` | `Passed` |
| `ZEGO-TC-005` | `[x]` | `Passed` |

---

## 6. Exit Criteria

- [x] roomId = session_id enforced
- [x] Token never persisted
- [x] `IZegoCloudService` has zero dependency on any repository (pure token generator)
- [x] ZegoCloud failure surfaces as a typed exception, caller state unaffected
- [x] Red Gate confirmed

---

## 7. Rollback Plan

```bash
# No migration owned by this module (see TDS §11.2) — code-only rollback:
git checkout -- src/main/java/com/carebridge/backend/consultation/integration/zegocloud/
git checkout -- src/test/java/com/carebridge/backend/consultation/integration/zegocloud/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-001 | ☐ ZEGO credentials env-only | G-0 |
| AP-AI-003 | ☐ roomId=consultationId has ADR-ZEGO-001 backing | G-1 |
