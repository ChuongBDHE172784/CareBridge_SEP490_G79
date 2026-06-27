# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-164 Search Expert

**Document ID:** `CB-EXP-TDD-006`
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
| **Feature / Gap ID** | `UC-164` |
| **Module** | `SearchExpert — expert` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Public` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "searches by name, specialty, keyword" — no min length | ADR-EXP-008: min 2 chars | Test q=1 char → 400 |
| L2 | SRS: no status filter | ADR-EXP-006: VERIFIED only | Test non-VERIFIED not in results |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
SearchExpert bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-164 | Hành vi người dùng |
| ADR-EXP | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-EXP-IMP-006 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | SRCH-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | SRCH-TC-00X |

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
class SearchTestFactory {
    static ExpertProfile makeVerifiedExpert(String name, String specialty) {
        ExpertProfile p = new ExpertProfile();
        p.setDisplayName(name);
        p.setSpecialties(List.of(specialty));
        p.setStatus(ExpertProfileStatus.VERIFIED);
        return p;
    }
}
```

---

### SRCH-TC-001 — Search by name returns matches

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Seed: "Dr. Nguyen" (VERIFIED) and "Dr. Tran" (VERIFIED)
2. `q = "Nguyen"`

**Expected Result:** 1 result matching "Dr. Nguyen"

**Current Status:** 🔴 Not written

---

### SRCH-TC-002 — Search by specialty returns matches

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Seed: expert with specialty="obstetrics"
2. `q = "obstetrics"`

**Expected Result:** Returns the obstetrics expert

**Current Status:** 🔴 Not written

---

### SRCH-TC-003 — q shorter than 2 chars → 400

**Severity:** `HIGH`
**Oracle Source:** `ADR-EXP-008`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ValidationException (SRCH-001)

**Current Status:** 🔴 Not written

---

### SRCH-TC-004 — Non-VERIFIED expert not in results

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-EXP-006`
**TDD Phase:** 🔴 RED

**Expected Result:** PENDING_VERIFICATION expert NOT returned even if name matches

**Current Status:** 🔴 Not written

---

### SRCH-TC-005 — Empty results returns empty page, not 404

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Expected Result:** `content=[]`, `totalElements=0`, HTTP 200

**Current Status:** 🔴 Not written

---

### SRCH-TC-006 — Response has no email/accountId

**Severity:** `HIGH`
**Oracle Source:** `BR-PRIVACY`
**TDD Phase:** 🔴 RED

```java
String json = objectMapper.writeValueAsString(result);
assertThat(json).doesNotContain("accountId");
assertThat(json).doesNotContain("email");
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `SRCH-TC-001` | `[ ]` | `___` |
| `SRCH-TC-003` | `[ ]` | `___` |
| `SRCH-TC-004` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertSearchService implements IExpertSearchService {
    @Override
    public Page<ExpertSearchResultDto> searchExperts(ExpertSearchRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| SRCH-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| SRCH-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

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
  -c "DELETE FROM flyway_schema_history WHERE version = '035';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/expert/
git checkout -- src/test/java/com/carebridge/backend/expert/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-004 | ☐ Search logic in Service not Controller | G-4 |
