# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-81 View Expert Profile

**Document ID:** `CB-EXP-TDD-005`
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
| **Feature / Gap ID** | `UC-81` |
| **Module** | `ViewExpertProfile — expert` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Public` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "displays qualifications" — no access control | ADR-EXP-007: SUSPENDED visible to admin only | Test SUSPENDED → 403 for mother |
| L2 | SRS: "availability" — no time range | ADR-EXP-007: next 7 days AVAILABLE slots only | Test slots filtered |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ViewExpertProfile bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-81 | Hành vi người dùng |
| ADR-EXP | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-EXP-IMP-005 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | PROFILE-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | PROFILE-TC-00X |

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

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
class ExpertProfileDetailTestFactory {
    static ExpertProfile makeVerifiedProfile() {
        // Baseline valid entity — synced with TDS-05 fixtures
        return new ExpertProfile.ExpertProfileBuilder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            // .field(value)
            .build();
    }

    static ExpertProfile makeVerifiedProfile(Consumer<ExpertProfile> overrides) {
        var entity = makeVerifiedProfile();
        overrides.accept(entity);
        return entity;
    }
}
```

---

### PROFILE-TC-001 — VERIFIED expert visible to mother → 200

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** Response with `status=VERIFIED`, bio, specialties, availableSlots

**Current Status:** 🔴 Not written

---

### PROFILE-TC-002 — SUSPENDED expert → 403 for ROLE_MOTHER

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-EXP-007`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (EXP-013)

**Current Status:** 🔴 Not written

---

### PROFILE-TC-003 — Response has no email/accountId

**Severity:** `HIGH`
**Oracle Source:** `BR-PRIVACY`
**TDD Phase:** 🔴 RED

```java
String json = objectMapper.writeValueAsString(response);
assertThat(json).doesNotContain("accountId");
assertThat(json).doesNotContain("email");
assertThat(json).doesNotContain("@");
```

**Current Status:** 🔴 Not written

---

### PROFILE-TC-004 — Not found → 404

**Severity:** `MEDIUM`
**TDD Phase:** 🔴 RED

**Expected Result:** throws NotFoundException (EXP-012)

**Current Status:** 🔴 Not written

---

### PROFILE-TC-005 — Only AVAILABLE slots in next 7 days returned

**Severity:** `HIGH`
**Oracle Source:** `ADR-EXP-007`
**TDD Phase:** 🔴 RED

```java
// BOOKED slot and slot 8 days from now should not appear
ExpertProfileDetailDto dto = service.getExpertProfile(expertId, callerAccountId);
dto.getAvailableSlots().forEach(slot -> {
    assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
});
```

**Current Status:** 🔴 Not written

---

### PROFILE-TC-006 — Response has no medical diagnosis

**Severity:** `HIGH`
**Oracle Source:** `BR-SAFETY`
**TDD Phase:** 🔴 RED

```java
String json = objectMapper.writeValueAsString(response);
assertThat(json).doesNotContain("diagnosis");
assertThat(json).doesNotContain("prescription");
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `PROFILE-TC-001` | `[ ]` | `___` |
| `PROFILE-TC-002` | `[ ]` | `___` |
| `PROFILE-TC-003` | `[ ]` | `___` |
| `PROFILE-TC-005` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertProfileService implements IExpertProfileService {
    @Override
    public ExpertProfileDetailDto getExpertProfile(UUID expertId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| PROFILE-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| PROFILE-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

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
  -c "DELETE FROM flyway_schema_history WHERE version = '030';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/expert/
git checkout -- src/test/java/com/carebridge/backend/expert/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-003 | ☐ ADR-EXP-007 visibility rules cited | G-1 |
