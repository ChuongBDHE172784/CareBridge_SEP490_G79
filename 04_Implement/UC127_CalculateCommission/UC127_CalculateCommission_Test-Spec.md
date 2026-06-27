# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-127 Calculate Commission

**Document ID:** `CB-PAY-TDD-003`
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
| **Feature / Gap ID** | `UC-127` |
| **Module** | `CalculateCommission — payment` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Confidential` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "calculates platform fee, expert revenue" — rate not defined | ADR-PAY-006: from commission_config table, default 20% | Test rate from DB |
| L2 | SRS: no idempotency | ADR-PAY-007: append-only, unique on payment_id | Test duplicate → 409 |
| L3 | SRS: no arithmetic spec | ADR-PAY-006: integer VND arithmetic (no float) | Test rounding |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
CalculateCommission bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-127 | Hành vi người dùng |
| ADR-PAY | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-PAY-IMP-003 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | COM-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | COM-TC-00X |

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
class CommissionTestFactory {
    static long computePlatformFee(long gross, BigDecimal rate) {
        // VND integer arithmetic: truncate (floor)
        return gross * rate.numerator / rate.denominator;
    }
}
```

---

### COM-TC-001 — Commission calculated at 20% rate

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-PAY-006`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Mock commission_config: platform_rate = 0.20
2. grossAmount = 200,000 VND
3. Call `calculateAndRecord(paymentId, consultationId, 200000L)`

**Expected Result:**
- `platform_fee_vnd = 40000`
- `expert_revenue_vnd = 160000`
- `platform_rate = 0.20`

```java
CommissionRecord record = service.calculateAndRecord(paymentId, consultationId, 200000L);
assertThat(record.getPlatformFeeVnd()).isEqualTo(40000L);
assertThat(record.getExpertRevenueVnd()).isEqualTo(160000L);
```

**Current Status:** 🔴 Not written

---

### COM-TC-002 — Rate from DB, not hardcoded

**Severity:** `HIGH`
**Oracle Source:** `ADR-PAY-006`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Set commission_config to 30% in test DB
2. Calculate commission

**Expected Result:** platform_fee = 30% of gross (not 20%)

**Current Status:** 🔴 Not written

---

### COM-TC-003 — Duplicate commission → 409

**Severity:** `HIGH`
**Oracle Source:** `ADR-PAY-007`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. calculateAndRecord(paymentId) → success
2. calculateAndRecord(same paymentId) again

**Expected Result:** throws ConflictException (COM-002)

**Current Status:** 🔴 Not written

---

### COM-TC-004 — Commission record NOT updated (append-only)

**Severity:** `HIGH`
**Oracle Source:** `ADR-PAY-007`
**TDD Phase:** 🔴 RED

```java
// Verify no UPDATE SQL is called on consultation_commissions
// Use @Sql to check row count before/after — should be same
long before = commissionRepo.countByPaymentId(paymentId);
// attempt duplicate → catches exception
long after = commissionRepo.countByPaymentId(paymentId);
assertThat(after).isEqualTo(before);
```

**Current Status:** 🔴 Not written

---

### COM-TC-005 — Non-admin cannot view all commissions → 403

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (COM-003)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `COM-TC-001` | `[ ]` | `___` |
| `COM-TC-002` | `[ ]` | `___` |
| `COM-TC-003` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class CommissionService implements ICommissionService {
    @Override
    public CommissionRecord calculateAndRecord(UUID paymentId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| COM-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| COM-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

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
  -c "DROP TABLE IF EXISTS consultation_commissions CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '033';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/payment/
git checkout -- src/test/java/com/carebridge/backend/payment/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-001 | ☐ Rate from DB only — not hardcoded | G-0 |
