# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-202 View Consultation List

**Document ID:** `CB-CON-TDD-003`
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
| **Feature / Gap ID** | `UC-202` |
| **Module** | `ViewConsultationList — consultation` |
| **Priority** | 🟡 P2 |
| **Data Classification** | `PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: Mother and Expert can view — no scoping | ADR-CON-006: participant-scoped | Test other user's consultations not returned |
| L2 | SRS: no PII restriction on counterpart | BR-PRIVACY: displayName only, not email | Test no email in response |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ViewConsultationList bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-202 | Hành vi người dùng |
| ADR-CON | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-CON-IMP-003 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | LIST-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | LIST-TC-00X |

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
class ConsultationListTestFactory {
    static Consultation makeConsultation(UUID motherId, UUID expertProfileId, ConsultationStatus status) {
        Consultation c = new Consultation();
        c.setId(UUID.randomUUID());
        c.setMotherAccountId(motherId);
        c.setExpertProfileId(expertProfileId);
        c.setStatus(status);
        c.setScheduledAt(Instant.now().plus(Duration.ofDays(1)));
        return c;
    }
}
```

---

### LIST-TC-001 — Mother sees own consultations only

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-CON-006`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Seed: 2 consultations for ACC-MOTHER-001, 1 for ACC-MOTHER-002
2. Call as ACC-MOTHER-001

**Expected Result:** 2 results, none belong to ACC-MOTHER-002

**Current Status:** 🔴 Not written

---

### LIST-TC-002 — Expert sees own consultations only

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-CON-006`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Seed: consultation for EXPERT-001 and consultation for EXPERT-002
2. Call as EXPERT-001

**Expected Result:** 1 result, only EXPERT-001's consultation

**Current Status:** 🔴 Not written

---

### LIST-TC-003 — Filter by status works

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Mother has 1 CONFIRMED and 1 CANCELLED consultation
2. Call with `status=CONFIRMED`

**Expected Result:** 1 result

**Current Status:** 🔴 Not written

---

### LIST-TC-004 — Response has no email

**Severity:** `HIGH`
**Oracle Source:** `BR-PRIVACY`
**TDD Phase:** 🔴 RED

```java
String json = objectMapper.writeValueAsString(result);
assertThat(json).doesNotContain("@");
assertThat(json).doesNotContain("email");
// counterpartName is displayName string only
```

**Current Status:** 🔴 Not written

---

### LIST-TC-005 — Empty list returns 200, not 404

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Expected Result:** `content=[]`, HTTP 200

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `LIST-TC-001` | `[ ]` | `___` |
| `LIST-TC-002` | `[ ]` | `___` |
| `LIST-TC-004` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ConsultationListService implements IConsultationListService {
    @Override
    public Page<ConsultationListItemDto> listConsultations(UUID accountId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| LIST-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| LIST-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

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
  -c "DROP TABLE IF EXISTS consultations CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '036';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/consultation/
git checkout -- src/test/java/com/carebridge/backend/consultation/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-003 | ☐ Participant scoping has ADR-CON-006 backing | G-1 |
