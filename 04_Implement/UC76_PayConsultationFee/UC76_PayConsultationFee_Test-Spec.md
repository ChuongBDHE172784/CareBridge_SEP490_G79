# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-76 Pay Consultation Fee

**Document ID:** `CB-PAY-TDD-001`
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
| **Feature / Gap ID** | `UC-76` |
| **Module** | `PayConsultationFee — payment` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Confidential` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "through payment gateway" — no security spec | ADR-PAY-001: HMAC-SHA512 required | Test invalid signature → rejected |
| L2 | SRS: no idempotency spec | ADR-PAY-002: duplicate callback handled | Test same callback twice |
| L3 | SRS: no timeout spec | ADR-PAY-003: 15 min payment window | Out of scope for unit test |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
PayConsultationFee bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-76 | Hành vi người dùng |
| ADR-PAY | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-PAY-IMP-001 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | PAY-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | PAY-TC-005 |

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
class PaymentTestFactory {
    static Map<String, String> makeValidVnpayCallback(String txnRef, String responseCode) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_Amount", "20000000"); // 200,000 VND × 100
        params.put("vnp_SecureHash", computeHmac(params, TEST_SECRET));
        return params;
    }
}
```

---

### PAY-TC-001 — Initiate payment URL → 200

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** Response with non-null `paymentUrl`, `expiresAt` = now + 15 min

**Current Status:** 🔴 Not written

---

### PAY-TC-002 — VNPay SUCCESS callback → consultation CONFIRMED

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-PAY-005`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Consultation in PENDING_PAYMENT
2. Receive callback with ResponseCode=00, valid signature
3. Call `processVnpayCallback(params)`

**Expected Result:**
- `consultation.status = CONFIRMED`
- `payment.status = SUCCESS`

```java
consultationRepo.findById(consultationId).orElseThrow();
assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.CONFIRMED);
```

**Current Status:** 🔴 Not written

---

### PAY-TC-003 — Invalid HMAC signature → rejected

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-PAY-001`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Modify secureHash to invalid value
2. Call `processVnpayCallback(params)`

**Expected Result:** Response `RspCode=97`, consultation NOT updated

**Current Status:** 🔴 Not written

---

### PAY-TC-004 — Duplicate callback (idempotent) → RspCode=02

**Severity:** `HIGH`
**Oracle Source:** `ADR-PAY-002`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Process callback once (SUCCESS)
2. Process same callback again

**Expected Result:** Second call returns `RspCode=02`, no double-update

**Current Status:** 🔴 Not written

---

### PAY-TC-005 — Non-owner cannot initiate payment → 403

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (PAY-004)

**Current Status:** 🔴 Not written

---

### PAY-TC-006 — Consultation not PENDING_PAYMENT → 409

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ConflictException (PAY-002)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `PAY-TC-001` | `[ ]` | `___` |
| `PAY-TC-002` | `[ ]` | `___` |
| `PAY-TC-003` | `[ ]` | `___` |
| `PAY-TC-004` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class PaymentService implements IPaymentService {
    @Override
    public PaymentUrlResponse initiatePayment(UUID consultationId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| PAY-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| PAY-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

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
  -c "DROP TABLE IF EXISTS payments CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '027';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/payment/
git checkout -- src/test/java/com/carebridge/backend/payment/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-001 | ☐ HMAC secret not hardcoded — env only | G-0 |
