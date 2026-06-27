# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-77 Join Consultation Session

**Document ID:** `CB-CON-TDD-002`
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
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-77` |
| **Module** | `JoinConsultationSession — consultation` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Confidential` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "joins session" — no eligibility rules | ADR-CON-005: CONFIRMED/IN_SESSION, join window | Test out-of-window → 400 |
| L2 | SRS: no token spec | ADR-CON-004: ZegoCloud token, TTL 1hr, not stored | Test token not in DB |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
JoinConsultationSession bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-77 | Hành vi người dùng |
| ADR-CON | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-CON-IMP-002 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | JOIN-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | JOIN-TC-003 |

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
class JoinSessionTestFactory {
    static Consultation makeConfirmedConsultation(UUID motherId, UUID expertProfileId) {
        Consultation c = new Consultation();
        c.setId(UUID.fromString("00000000-0000-0000-0000-000000000100"));
        c.setMotherAccountId(motherId);
        c.setExpertProfileId(expertProfileId);
        c.setStatus(ConsultationStatus.CONFIRMED);
        c.setScheduledAt(Instant.now().plus(Duration.ofMinutes(5)));
        c.setDurationMinutes(60);
        return c;
    }
}
```

---

### JOIN-TC-001 — Mother joins CONFIRMED session → 200 with token

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** Response with non-null `zegoToken`, `roomId = consultationId`

```java
JoinSessionResponse resp = service.joinSession(consultationId, motherAccountId);
assertThat(resp.getRoomId()).isEqualTo(consultationId.toString());
assertThat(resp.getZegoToken()).isNotNull();
```

**Current Status:** 🔴 Not written

---

### JOIN-TC-002 — Token NOT stored in DB

**Severity:** `HIGH`
**Oracle Source:** `ADR-CON-004`
**TDD Phase:** 🔴 RED

```java
service.joinSession(consultationId, motherAccountId);
// Verify no token in zego_sessions table
ZegoSession session = zegoSessionRepo.findByConsultationId(consultationId).orElseThrow();
assertThat(session).doesNotHaveField("token"); // field doesn't exist on entity
```

**Current Status:** 🔴 Not written

---

### JOIN-TC-003 — Non-participant → 403

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-CON-005`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (SES-001)

**Current Status:** 🔴 Not written

---

### JOIN-TC-004 — Consultation PENDING_PAYMENT → 409

**Severity:** `HIGH`
**Oracle Source:** `ADR-CON-005`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ConflictException (SES-003)

**Current Status:** 🔴 Not written

---

### JOIN-TC-005 — ZegoCloud unavailable → 503

**Severity:** `HIGH`
**Oracle Source:** `ADR-ZEGO-001`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Mock `zegoCloudService.generateToken()` throws RuntimeException
2. Call `joinSession()`

**Expected Result:** throws ServiceUnavailableException (SES-002); consultation status NOT changed

**Current Status:** 🔴 Not written

---

### JOIN-TC-006 — roomId = consultationId

**Severity:** `HIGH`
**Oracle Source:** `ADR-CON-004`
**TDD Phase:** 🔴 RED

```java
JoinSessionResponse resp = service.joinSession(consultationId, motherAccountId);
assertThat(resp.getRoomId()).isEqualTo(consultationId.toString());
// NOT a separate UUID
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `JOIN-TC-001` | `[ ]` | `___` |
| `JOIN-TC-003` | `[ ]` | `___` |
| `JOIN-TC-005` | `[ ]` | `___` |
| `JOIN-TC-006` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class SessionService implements ISessionService {
    @Override
    public JoinSessionResponse joinSession(UUID consultationId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| JOIN-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| JOIN-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3)

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS đã được review và approve
- [ ] Flyway migration đã chạy thành công trên staging
- [ ] Test fixtures đã được chuẩn bị

### Exit Criteria (DoD)
- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh
- [ ] Test coverage ≥ 80% cho Service class
- [ ] Không có PII trong logs
- [ ] **Red Gate (§5.1)** passed
- [ ] **Props Isolation** — no shared mutable state

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS zego_sessions CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '028';"

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
