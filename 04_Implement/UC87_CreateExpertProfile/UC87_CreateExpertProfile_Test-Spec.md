# TEST-DRIVEN DEVELOPMENT SPECIFICATION

# UC-87 Create Expert Profile

**Document D:** `CB-EXP-TDD-001`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Approved`
**Author:** `AI Agent`

**References:**

- TDS: `04_Implement/UC87_CreateExpertProfile/UC87_CreateExpertProfile_TDS.md` (CB-EXP-IMP-001)
- SRS: §3.2.1.1

---

## CHANGELOG

| Ngày      | Người thực hiện | Nội dung thay đổi          |
| ---------- | ------------------- | ----------------------------- |
| 2026-06-26 | AI Agent            | Khởi tạo TDD spec cho UC-87 |

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

| Field                         | Value                             |
| ----------------------------- | --------------------------------- |
| **Feature / Gap ID**    | `UC-87`                         |
| **Module**              | `CreateExpertProfile — expert` |
| **Priority**            | 🟠 P1                             |
| **Data Classification** | `PII`                           |

---

## 2. Logic Issues Resolved

| #  | Spec gốc                                                          | Thực tế                                          | Fix                                             |
| -- | ------------------------------------------------------------------ | -------------------------------------------------- | ----------------------------------------------- |
| L1 | SRS: "creates a professional profile" — không rõ initial status | ADR-EXP-002: initial status = PENDING_VERIFICATION | Test status = PENDING_VERIFICATION after create |
| L2 | SRS: không rõ duplicate handling                                 | ADR-EXP-001: 1 active profile per account          | Test duplicate → 409                           |

---

## 3. Test Design

### TDS-03 — Test Conditions

| Condition ID | Test Condition                         | Coverage Item                            | Test Cases     |
| ------------ | -------------------------------------- | ---------------------------------------- | -------------- |
| TC-COND-001  | ROLE_EXPERT creates profile            | `ExpertProfileService.createProfile()` | `EXP-TC-001` |
| TC-COND-002  | Duplicate profile → 409               | `existsByAccountId()`                  | `EXP-TC-002` |
| TC-COND-003  | ROLE_MOTHER → 403                     | RBAC check                               | `EXP-TC-003` |
| TC-COND-004  | Missing displayName → 400             | validation                               | `EXP-TC-004` |
| TC-COND-005  | Initial status is PENDING_VERIFICATION | status guard                             | `EXP-TC-005` |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class ExpertProfileTestFactory {
    static CreateExpertProfileRequest makeValidRequest() {
        CreateExpertProfileRequest r = new CreateExpertProfileRequest();
        r.setDisplayName("Dr. Test Expert");
        r.setBio("10 years in obstetrics");
        r.setSpecialties(List.of("obstetrics", "prenatal_care"));
        r.setYearsOfExperience(10);
        r.setConsultationFeeVnd(200000L);
        r.setConsultationModalities(List.of(ConsultationModality.VIDEO));
        return r;
    }
}
```

---

### EXP-TC-001 — ROLE_EXPERT creates profile → 201

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Test Steps:**

1. Caller = ACC-EXPERT-001 with ROLE_EXPERT
2. Call `createProfile(makeValidRequest(), ACC-EXPERT-001)`

**Expected Result:** `ExpertProfileResponse` with `status=PENDING_VERIFICATION`, non-null id

**Current Status:** 🔴 Not written

---

### EXP-TC-002 — Duplicate profile → 409

**Severity:** `CRITICAL`
**Oracle Source:** `ADR-EXP-001`
**TDD Phase:** 🔴 RED

**Expected Result:** throws BusinessException (EXP-002)

**Current Status:** 🔴 Not written

---

### EXP-TC-003 — ROLE_MOTHER → 403

**Severity:** `CRITICAL`
**Oracle Source:** `BR-RBAC`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (EXP-003)

**Current Status:** 🔴 Not written

---

### EXP-TC-004 — Missing displayName → 400

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ValidationException (EXP-001)

**Current Status:** 🔴 Not written

---

### EXP-TC-005 — Status must be PENDING_VERIFICATION

**Severity:** `HIGH`
**Oracle Source:** `ADR-EXP-002`
**TDD Phase:** 🔴 RED

```java
ExpertProfileResponse resp = service.createProfile(req, userId);
assertThat(resp.getStatus()).isEqualTo(ExpertProfileStatus.PENDING_VERIFICATION);
// NOT VERIFIED, NOT DRAFT
```

**Current Status:** 🔴 Not written

---

### EXP-TC-INT-001 — Profile persisted in DB

**Severity:** `HIGH`
**TDD Phase:** 🔴 RED

**Expected Result:** `expert_profiles` table has 1 row with correct userId and status

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID          | 🔴 RED  | 🟢 GREEN | 🔵 REFACTOR |
| -------------- | ------- | -------- | ----------- |
| `EXP-TC-001` | `[ ]` | `___`  | —          |
| `EXP-TC-002` | `[ ]` | `___`  | —          |
| `EXP-TC-003` | `[ ]` | `___`  | —          |
| `EXP-TC-005` | `[ ]` | `___`  | —          |

---

## 6. Exit Criteria

- [ ] Initial status = PENDING_VERIFICATION enforced
- [ ] Duplicate blocked at application layer
- [ ] ROLE_EXPERT required
- [ ] Red Gate confirmed

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS expert_profiles CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '041';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/expert/
git checkout -- src/test/java/com/carebridge/backend/expert/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Check                                              | Gate   |
| --------- | -------------------------------------------------- | ------ |
| AP-AI-002 | ☐ Tests FAIL with stub                            | G-2 ★ |
| AP-AI-003 | ☐ ADR-EXP-001/002 cited for status and uniqueness | G-1    |
