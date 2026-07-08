# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-90 Configure Availability

**Document ID:** `CB-EXP-TDD-003`
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
| **Feature / Gap ID** | `UC-90` |
| **Module** | `ConfigureAvailability — expert` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Internal` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "configures slots" — no slot count limit | ADR-EXP-004: max 30 slots | Test > 30 → 400 |
| L2 | SRS: no overlap check mentioned | ADR-EXP-004: overlapping slots per day rejected | Test overlap → 400 |
| L3 | SRS: no duration constraint | ADR-EXP-004: must be 30/45/60 min only | Test invalid duration |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ConfigureAvailability bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-90 | Hành vi người dùng |
| ADR-EXP | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-EXP-IMP-003 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | AVAIL-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | AVAIL-TC-00X |

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
class AvailabilityTestFactory {
    static AvailabilitySlotRequest makeMonSlot(LocalTime start, LocalTime end, int duration) {
        AvailabilitySlotRequest r = new AvailabilitySlotRequest();
        r.setDayOfWeek(DayOfWeekEnum.MON);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setDurationMinutes(duration);
        return r;
    }
}
```

---

### AVAIL-TC-001 — Valid slots saved → 200

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Request with 3 valid non-overlapping MON slots
2. Call `configureAvailability(expertId, accountId, request)`

**Expected Result:** 200 with `totalSlots=3`, all AVAILABLE

**Current Status:** 🔴 Not written

---

### AVAIL-TC-002 — Replace strategy: old slots deleted

**Severity:** `HIGH`
**Oracle Source:** `ADR-EXP-004`
**TDD Phase:** 🔴 RED

```java
// First configure: 2 slots
service.configureAvailability(expertId, accountId, req2slots);
// Then configure: 1 slot (should replace)
service.configureAvailability(expertId, accountId, req1slot);
// DB should have exactly 1 slot
long count = slotRepo.countByExpertId(expertId);
assertThat(count).isEqualTo(1);
```

**Current Status:** 🔴 Not written

---

### AVAIL-TC-003 — Overlapping slots on same day → 400

**Severity:** `HIGH`
**Oracle Source:** `ADR-EXP-004`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Slots: MON 09:00–12:00 and MON 11:00–14:00 (overlap)
2. Call configure

**Expected Result:** throws ValidationException (AVAIL-002)

**Current Status:** 🔴 Not written

---

### AVAIL-TC-004 — More than 30 slots → 400

**Severity:** `HIGH`
**Oracle Source:** `ADR-EXP-004`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ValidationException (AVAIL-001)

**Current Status:** 🔴 Not written

---

### AVAIL-TC-005 — Duration not in (30, 45, 60) → 400

**Severity:** `MEDIUM`
**Oracle Source:** `ADR-EXP-004`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ValidationException (AVAIL-003)

**Current Status:** 🔴 Not written

---

### AVAIL-TC-006 — Non-owner → 403

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (AVAIL-004)

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `AVAIL-TC-001` | `[ ]` | `___` |
| `AVAIL-TC-002` | `[ ]` | `___` |
| `AVAIL-TC-003` | `[ ]` | `___` |
| `AVAIL-TC-004` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class AvailabilityService implements IAvailabilityService {
    @Override
    public AvailabilityResponse configureAvailability(ConfigureAvailabilityRequest input) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| AVAIL-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| AVAIL-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

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
  -c "DROP TABLE IF EXISTS expert_availability_slots CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '031';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/expert/
git checkout -- src/test/java/com/carebridge/backend/expert/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-003 | ☐ Replace-strategy has ADR backing | G-1 |
