# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-75 Book Private Consultation

**Document ID:** `CB-CON-TDD-001`
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
| **Feature / Gap ID** | `UC-75` |
| **Module** | `BookPrivateConsultation — consultation` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Sensitive-PII` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "selects slot" — no concurrency protection mentioned | ADR-CON-002: SELECT FOR UPDATE on slot | Test concurrent booking returns 409 |
| L2 | SRS: no initial status mentioned | ADR-CON-001: initial = PENDING_PAYMENT | Test status = PENDING_PAYMENT |
| L3 | SRS: "shared data" — consent scope not defined | ADR-CON-003: shareHealthData persisted as consent record | Test health data sharing flag |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
BookPrivateConsultation bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-75 | Hành vi người dùng |
| ADR-CON | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-CON-IMP-001 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | BOOK-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | BOOK-TC-006 |

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

### Props Isolation Boilerplate

```java
class BookingTestFactory {
    static BookConsultationRequest makeValidRequest(UUID expertId, UUID slotId) {
        BookConsultationRequest r = new BookConsultationRequest();
        r.setExpertProfileId(expertId);
        r.setSlotId(slotId);
        r.setTopic("Prenatal checkup questions");
        r.setModality(ConsultationModalityType.VIDEO);
        r.setShareHealthData(false);
        return r;
    }
}
```

---

### BOOK-TC-001 — Successful booking → 201

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:**
- Response with `status=PENDING_PAYMENT`
- Slot status changed to `BOOKED` in DB

**Current Status:** 🔴 Not written

---

### BOOK-TC-002 — Initial status must be PENDING_PAYMENT

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-CON-001`
**TDD Phase:** 🔴 RED

```java
ConsultationResponse resp = service.bookConsultation(req, motherAccountId);
assertThat(resp.getStatus()).isEqualTo(ConsultationStatus.PENDING_PAYMENT);
```

**Current Status:** 🔴 Not written

---

### BOOK-TC-003 — Slot already BOOKED → 409

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-CON-002`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ConflictException (CON-002)

**Current Status:** 🔴 Not written

---

### BOOK-TC-004 — Expert not found → 404

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Expected Result:** throws NotFoundException (CON-003)

**Current Status:** 🔴 Not written

---

### BOOK-TC-005 — Modality not supported by expert → 400

**Severity:** `MEDIUM`
**Oracle Source:** `ADR-CON-001`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ValidationException (CON-006)

**Current Status:** 🔴 Not written

---

### BOOK-TC-006 — ROLE_EXPERT cannot book → 403

**Severity:** `CRITICAL`
**Oracle Source:** `BR-RBAC`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (CON-004)

**Current Status:** 🔴 Not written

---

### BOOK-TC-INT-001 — Full booking persisted with slot locked

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Expected Result:**
- `consultations` table has new row with `status=PENDING_PAYMENT`
- `expert_availability_slots` row has `status=BOOKED`

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `BOOK-TC-001` | `[ ]` | `___` |
| `BOOK-TC-002` | `[ ]` | `___` |
| `BOOK-TC-003` | `[ ]` | `___` |
| `BOOK-TC-006` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ConsultationBookingService implements IConsultationBookingService {
    @Override
    public ConsultationResponse bookConsultation(BookConsultationRequest input) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| BOOK-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| BOOK-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

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
  -c "DELETE FROM flyway_schema_history WHERE version = '026';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/consultation/
git checkout -- src/test/java/com/carebridge/backend/consultation/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-003 | ☐ SELECT FOR UPDATE has ADR-CON-002 backing | G-1 |
