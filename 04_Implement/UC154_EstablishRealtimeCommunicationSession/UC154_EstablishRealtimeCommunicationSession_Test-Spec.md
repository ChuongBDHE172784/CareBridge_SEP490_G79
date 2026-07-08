# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-154 Establish Realtime Communication Session

**Document ID:** `CB-CON-TDD-004`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo Test-Spec |

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
| L1 | SRS: "creates rooms or tokens" — no implementation detail | ADR-ZEGO-001: lazy room creation, roomId=consultationId | Test roomId format |
| L2 | SRS: "maintains chat, voice, video status" — no DB spec | ADR-ZEGO-002: zego_sessions tracks join timestamps | Test session recorded |

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
class ZegoSessionTestFactory {
    static Consultation makeConfirmedConsultation() {
        Consultation c = new Consultation();
        c.setId(UUID.fromString("00000000-0000-0000-0000-000000000200"));
        c.setStatus(ConsultationStatus.CONFIRMED);
        c.setMotherAccountId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        c.setScheduledAt(Instant.now().plus(Duration.ofMinutes(2)));
        c.setDurationMinutes(60);
        return c;
    }
}
```

---

### ZEGO-TC-001 — Token generated with consultationId as roomId

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-ZEGO-001`
**TDD Phase:** 🔴 RED

```java
// Mock ZegoCloud SDK
when(zegoCloudService.generateToken(consultationId.toString(), userId, displayName))
    .thenReturn(new ZegoTokenDto("04AAAxx...", Instant.now().plusSeconds(3600)));

JoinSessionResponse resp = sessionService.joinSession(consultationId, motherAccountId);
assertThat(resp.getRoomId()).isEqualTo(consultationId.toString());
// NOT UUID.randomUUID()
```

**Current Status:** 🔴 Not written

---

### ZEGO-TC-002 — Token NOT persisted to DB

**Severity:** `HIGH`
**Oracle Source:** `ADR-ZEGO-001`
**TDD Phase:** 🔴 RED

```java
sessionService.joinSession(consultationId, motherAccountId);
ZegoSession session = zegoSessionRepo.findByConsultationId(consultationId).orElseThrow();
// ZegoSession entity has no 'token' field
assertThat(session).hasNoNullFieldsOrPropertiesExcept("endedAt", "expertJoinedAt");
// Verify token is not in session entity
```

**Current Status:** 🔴 Not written

---

### ZEGO-TC-003 — Join records mother_joined_at

**Severity:** `HIGH`
**Oracle Source:** `ADR-ZEGO-002`
**TDD Phase:** 🔴 RED

```java
sessionService.joinSession(consultationId, motherAccountId);
ZegoSession session = zegoSessionRepo.findByConsultationId(consultationId).orElseThrow();
assertThat(session.getMotherJoinedAt()).isNotNull();
```

**Current Status:** 🔴 Not written

---

### ZEGO-TC-004 — First joiner → consultation IN_SESSION

**Severity:** `HIGH`
**Oracle Source:** `ADR-ZEGO-002`
**TDD Phase:** 🔴 RED

```java
sessionService.joinSession(consultationId, motherAccountId);
Consultation c = consultationRepo.findById(consultationId).orElseThrow();
assertThat(c.getStatus()).isEqualTo(ConsultationStatus.IN_SESSION);
```

**Current Status:** 🔴 Not written

---

### ZEGO-TC-005 — ZegoCloud SDK failure → 503, status NOT changed

**Severity:** `HIGH`
**Oracle Source:** `ADR-ZEGO-001`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Mock SDK to throw RuntimeException
2. Call `joinSession()`

**Expected Result:**
- throws ServiceUnavailableException (ZEGO-001)
- consultation.status still CONFIRMED (not IN_SESSION)

**Current Status:** 🔴 Not written

---

### ZEGO-TC-006 — Non-participant → 403

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (ZEGO-002)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `ZEGO-TC-001` | `[ ]` | `___` |
| `ZEGO-TC-002` | `[ ]` | `___` |
| `ZEGO-TC-004` | `[ ]` | `___` |
| `ZEGO-TC-005` | `[ ]` | `___` |

---

## 6. Exit Criteria

- [ ] roomId = consultationId enforced
- [ ] Token never persisted
- [ ] ZegoCloud failure leaves consultation unaffected
- [ ] Red Gate confirmed

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS zego_sessions CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '037';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/consultation/
git checkout -- src/test/java/com/carebridge/backend/consultation/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-001 | ☐ ZEGO credentials env-only | G-0 |
| AP-AI-003 | ☐ roomId=consultationId has ADR-ZEGO-001 backing | G-1 |
