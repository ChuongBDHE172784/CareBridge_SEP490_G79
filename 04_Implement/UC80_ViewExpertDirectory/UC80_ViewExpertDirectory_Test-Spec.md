# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-80 View Expert Directory

**Document ID:** `CB-EXP-TDD-004`
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
| **Feature / Gap ID** | `UC-80` |
| **Module** | `ViewExpertDirectory — expert` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Public` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "displays and filters experts" — no status filter | ADR-EXP-006: only VERIFIED shown | Test PENDING_VERIFICATION not in results |
| L2 | SRS: no PII restriction | BR-PRIVACY: no email/phone | Test response has no PII |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ViewExpertDirectory bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-80 | Hành vi người dùng |
| ADR-EXP | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-EXP-IMP-004 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | DIR-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | DIR-TC-003 |

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
class DirectoryTestFactory {
    static ExpertProfile makeVerifiedExpert(String name) {
        ExpertProfile p = new ExpertProfile();
        p.setId(UUID.randomUUID());
        p.setDisplayName(name);
        p.setSpecialties(List.of("obstetrics"));
        p.setStatus(ExpertProfileStatus.VERIFIED);
        p.setIsOnline(true);
        return p;
    }
}
```

---

### DIR-TC-001 — Returns VERIFIED experts → 200

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** Page with verified experts only

**Current Status:** 🔴 Not written

---

### DIR-TC-002 — PENDING expert NOT in results

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-EXP-006`
**TDD Phase:** 🔴 RED

```java
// Seed: 1 VERIFIED, 1 PENDING_VERIFICATION
Page<ExpertDirectoryItemDto> result = service.listExperts(filter);
assertThat(result.getContent()).hasSize(1);
assertThat(result.getContent().get(0).getStatus())
    .isEqualTo(ExpertProfileStatus.VERIFIED);
```

**Current Status:** 🔴 Not written

---

### DIR-TC-003 — Response has no email/phone/accountId

**Severity:** `HIGH`
**Oracle Source:** `BR-PRIVACY`
**TDD Phase:** 🔴 RED

```java
String json = objectMapper.writeValueAsString(result);
assertThat(json).doesNotContain("@");
assertThat(json).doesNotContain("phone");
assertThat(json).doesNotContain("accountId");
```

**Current Status:** 🔴 Not written

---

### DIR-TC-004 — Page size > 50 → 400

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ValidationException (EXP-010)

**Current Status:** 🔴 Not written

---

### DIR-TC-005 — Filter by specialty works

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. 2 experts: one with specialty "obstetrics", one with "pediatrics"
2. Filter: `specialty=obstetrics`

**Expected Result:** Only the obstetrics expert returned

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `DIR-TC-001` | `[ ]` | `___` |
| `DIR-TC-002` | `[ ]` | `___` |
| `DIR-TC-003` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertDirectoryService implements IExpertDirectoryService {
    @Override
    public Page<ExpertDirectoryItemDto> listExperts(ExpertFilterRequest input) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| DIR-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| DIR-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

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
  -c "DROP TABLE IF EXISTS expert_profiles CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '029';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/expert/
git checkout -- src/test/java/com/carebridge/backend/expert/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-004 | ☐ No business logic (status filter) in controller | G-4 |
