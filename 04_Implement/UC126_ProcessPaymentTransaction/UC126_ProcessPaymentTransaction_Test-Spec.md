# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-126 Process Payment Transaction

**Document ID:** `CB-PAY-TDD-002`
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
| **Feature / Gap ID** | `UC-126` |
| **Module** | `ProcessPaymentTransaction — payment` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Confidential` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "receives transaction requests" — no security spec | ADR-PAY-001: HMAC-SHA512 before any processing | Test invalid sig → RspCode=97 |
| L2 | SRS: "returns success, failure, cancellation" — VNPay convention | ADR-PAY-004: always return HTTP 200 with RspCode | Test HTTP status always 200 |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ProcessPaymentTransaction bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-126 | Hành vi người dùng |
| ADR-PAY | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-PAY-IMP-002 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | IPN-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | IPN-TC-00X |

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
class VnpayCallbackTestFactory {
    static final String TEST_HASH_SECRET = "test-secret-32-chars-for-testing";

    static Map<String, String> makeSuccessCallback(String txnRef) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_Amount", "20000000");
        params.put("vnp_OrderInfo", "Pay for consultation " + txnRef);
        // Sign with test secret
        String data = params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));
        params.put("vnp_SecureHash", HmacUtils.hmacSha512Hex(TEST_HASH_SECRET, data));
        return params;
    }
}
```

---

### IPN-TC-001 — Valid SUCCESS callback → consultation CONFIRMED

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-PAY-005`
**TDD Phase:** 🔴 RED

**Expected Result:**
- Response `RspCode=00`
- consultation.status = CONFIRMED
- payment.status = SUCCESS

**Current Status:** 🔴 Not written

---

### IPN-TC-002 — Invalid signature → RspCode=97, no DB update

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-PAY-001`
**TDD Phase:** 🔴 RED

**Expected Result:**
- Response `RspCode=97`
- HTTP 200
- DB NOT changed

```java
VnpayIpnResponse resp = service.processIpn(badSigParams);
assertThat(resp.getRspCode()).isEqualTo("97");
// Verify consultation still PENDING_PAYMENT
Consultation c = consultationRepo.findById(consultationId).orElseThrow();
assertThat(c.getStatus()).isEqualTo(ConsultationStatus.PENDING_PAYMENT);
```

**Current Status:** 🔴 Not written

---

### IPN-TC-003 — Always HTTP 200 (even on error)

**Severity:** `HIGH`
**Oracle Source:** `ADR-PAY-004`
**TDD Phase:** 🔴 RED

```java
// Even with bad signature, HTTP must be 200
mockMvc.perform(get("/api/v1/payment/vnpay/ipn").params(badSigParams))
    .andExpect(status().isOk());
```

**Current Status:** 🔴 Not written

---

### IPN-TC-004 — Duplicate callback → RspCode=02

**Severity:** `HIGH`
**Oracle Source:** `ADR-PAY-002`
**TDD Phase:** 🔴 RED

**Expected Result:** Second call returns `RspCode=02`

**Current Status:** 🔴 Not written

---

### IPN-TC-005 — FAILED callback → slot released

**Severity:** `HIGH`
**Oracle Source:** `ADR-PAY-005`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Consultation PENDING_PAYMENT, slot BOOKED
2. Callback with ResponseCode=24 (user cancel)

**Expected Result:**
- consultation.status = CANCELLED
- slot.status = AVAILABLE

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `IPN-TC-001` | `[ ]` | `___` |
| `IPN-TC-002` | `[ ]` | `___` |
| `IPN-TC-003` | `[ ]` | `___` |
| `IPN-TC-004` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class VnpayIpnService implements IVnpayIpnService {
    @Override
    public VnpayIpnResponse processIpn(Map<String, String> input) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| IPN-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| IPN-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

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
  -c "DELETE FROM flyway_schema_history WHERE version = '032';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/payment/
git checkout -- src/test/java/com/carebridge/backend/payment/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-001 | ☐ HMAC secret env-only verified | G-0 |
