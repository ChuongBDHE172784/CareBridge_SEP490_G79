# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-32 Update Baby Profile

**Document ID:** `CB-BABY-IMP-002-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC32_UpdateBabyProfile/UC32_UpdateBabyProfile_TDS.md` (CB-BABY-IMP-002)
- SRS: S3.3.1.9

---

## CHANGELOG

| Ngay | Nguoi thuc hien | Noi dung thay doi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khoi tao TDD spec cho UC-32 Update Baby Profile |

---

## MUC LUC

1. [Thong tin Module](#1-thong-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thong tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-32` |
| **Module** | `UpdateBabyProfile — baby` *(sửa 2026-07-03: package thật là `com.carebridge.backend.baby`, không phải `carejourney`)* |
| **Spec goc** | `CB-BABY-IMP-002` |
| **Priority** | P1 |
| **Data Classification** | `Sensitive-PII` |
| **Upstream Dependencies** | `auth, UC-31 (baby_profiles)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-BABY-IMP-002 S17` |
| **Constraints Injected** | C1 (ownership), C2 (ACTIVE check), C3 (immutable fields), C4 (audit) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 -> T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec goc (sai / thieu) | Thuc te (schema / policy) | Fix ap dung trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS does not specify which fields are immutable | ADR-BABY-003: owner_user_id, status, related_journey_id are immutable | Test verifies these fields remain unchanged after update |
| L2 | SRS does not specify behavior for ARCHIVED baby | ADR-BABY-004: reject update on ARCHIVED baby with BABY-012 | Test verifies 400 response for archived baby |
| L3 | SRS does not specify partial update behavior | PUT replaces all updatable fields; null fields are set to null | Test verifies null fields are written as null |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
UpdateBabyProfile bao gom cac layer:
+-- Service (mock BabyProfileRepository voi Mockito)
+-- Controller (mock BabyService voi @WebMvcTest)
+-- Integration (Testcontainers PostgreSQL voi @SpringBootTest)
```

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid update with all updatable fields | `BabyService.updateBabyProfile()` | `BABY-TC-032-001` |
| TC-COND-002 | Baby profile not found | `BabyService.updateBabyProfile()` | `BABY-TC-032-002` |
| TC-COND-003 | Baby not owned by JWT user | `BabyService.checkOwnership()` | `BABY-TC-032-003` |
| TC-COND-004 | Baby is ARCHIVED | `BabyService.checkActiveStatus()` | `BABY-TC-032-004` |
| TC-COND-005 | No JWT token | Spring Security filter | `BABY-TC-032-005` |
| TC-COND-006 | DB persistence verified | Full flow | `BABY-TC-032-INT-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | Status: ACTIVE vs ARCHIVED | Two states with different behavior |
| Boundary Value Analysis | Ownership: match vs mismatch | Binary authorization check |
| Error Guessing | Missing JWT, invalid babyId | Security attack vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Muc dich |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: MOTHER_ID, role: 'MOTHER'}` | Happy path auth |
| `FX-002` | DB seed | `{babyId: BABY_ID, ownerUserId: MOTHER_ID, status: 'ACTIVE', nickname: 'Bean'}` | Existing baby |
| `FX-003` | DB seed | `{babyId: ARCHIVED_BABY_ID, ownerUserId: MOTHER_ID, status: 'ARCHIVED'}` | Archived baby |
| `FX-004` | DB seed | `{babyId: OTHER_BABY_ID, ownerUserId: OTHER_MOTHER_ID, status: 'ACTIVE'}` | Baby owned by another |
| `FX-005` | Input | `{nickname: "Updated Name", birthDate: "2026-02-10", sex: "FEMALE"}` | Update request |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// BabyProfileUpdateTestFactory.java
class BabyProfileUpdateTestFactory {

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000032");
    static final UUID BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000032");
    static final UUID OTHER_MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID OTHER_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000099");
    static final UUID ARCHIVED_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000088");

    // (sửa 2026-07-03: khớp field thật của BabyProfile.java — id/status là BabyProfileStatus enum, gender không phải sex/String)
    static BabyProfile makeActiveBaby() {
        BabyProfile p = new BabyProfile();
        p.setId(BABY_ID);
        p.setOwnerUserId(MOTHER_ID);
        p.setNickname("Bean");
        p.setBirthDate(LocalDate.of(2026, 1, 15));
        p.setGender(Gender.MALE);
        p.setBirthWeightKg(new BigDecimal("3.20"));
        p.setBirthLengthCm(new BigDecimal("50.0"));
        p.setStatus(BabyProfileStatus.ACTIVE);
        return p;
    }

    static BabyProfile makeArchivedBaby() {
        BabyProfile p = makeActiveBaby();
        p.setId(ARCHIVED_BABY_ID);
        p.setStatus(BabyProfileStatus.ARCHIVED);
        return p;
    }

    static BabyProfile makeOtherMotherBaby() {
        BabyProfile p = makeActiveBaby();
        p.setId(OTHER_BABY_ID);
        p.setOwnerUserId(OTHER_MOTHER_ID);
        return p;
    }

    static UpdateBabyProfileRequest makeUpdateRequest() {
        UpdateBabyProfileRequest req = new UpdateBabyProfileRequest();
        req.setNickname("Updated Name");
        req.setBirthDate(LocalDate.of(2026, 2, 10));
        req.setGender(Gender.FEMALE);
        req.setBirthWeightKg(new BigDecimal("3.50"));
        req.setBirthLengthCm(new BigDecimal("51.0"));
        return req;
    }
}
```

---

### BABY-TC-032-001 — Happy path update -> 200

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyService.updateBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyServiceUpdateTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-32 Normal Flow, ADR-BABY-003`

**Preconditions:**
- Mother authenticated (FX-001)
- Baby profile exists with status ACTIVE and owner = MOTHER_ID (FX-002)

**Test Steps:**
1. Arrange: Mock `babyRepository.findById(BABY_ID)` -> returns FX-002 profile
2. Arrange: Mock `babyRepository.save()` -> returns saved profile
3. Act: Call `babyService.updateBabyProfile(BABY_ID, FX-005 request, MOTHER_ID)`
4. Assert: Response nickname = "Updated Name", sex = "FEMALE", birthDate = "2026-02-10"
5. Assert: `auditService.emit(BABY_PROFILE_UPDATED)` called once
6. Assert: `babyRepository.save()` called once

**Expected Result (PASS):**
- Returns UpdateBabyProfileResponse with updated fields
- status remains "ACTIVE" (immutable)
- auditService.emit() called with BABY_PROFILE_UPDATED

**Expected Result (FAIL):**
- Throws unexpected exception, or response contains old values, or audit not emitted

**Current Status:** RED — Not written

---

### BABY-TC-032-002 — Baby not found -> 404 BABY-010

**Severity:** `HIGH`
**Feature Under Test:** `BabyService.updateBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyServiceUpdateTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-BABY-010`

**Preconditions:**
- Mother authenticated

**Test Steps:**
1. Arrange: Mock `babyRepository.findById(NON_EXISTENT_ID)` -> returns Optional.empty()
2. Act: Call `babyService.updateBabyProfile(NON_EXISTENT_ID, request, MOTHER_ID)`
3. Assert: Throws exception with error code BABY-010

**Expected Result (PASS):**
- Throws NotFoundException with code BABY-010
- `babyRepository.save()` NOT called

**Current Status:** RED — Not written

---

### BABY-TC-032-003 — Baby not owned -> 403 BABY-011

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `BabyService.checkOwnership()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyServiceUpdateTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-BABY-011, BR-RBAC`

**Preconditions:**
- Mother authenticated with MOTHER_ID
- Baby profile exists but owned by OTHER_MOTHER_ID (FX-004)

**Test Steps:**
1. Arrange: Mock `babyRepository.findById(OTHER_BABY_ID)` -> returns FX-004 profile
2. Act: Call `babyService.updateBabyProfile(OTHER_BABY_ID, request, MOTHER_ID)`
3. Assert: Throws exception with error code BABY-011

**Expected Result (PASS):**
- Throws ForbiddenException with code BABY-011
- `babyRepository.save()` NOT called
- No audit event emitted

**Expected Result (FAIL):**
- Update succeeds despite ownership mismatch (access control bypass)

**Current Status:** RED — Not written

---

### BABY-TC-032-004 — Update archived baby -> 400 BABY-012

**Severity:** `HIGH`
**Feature Under Test:** `BabyService.checkActiveStatus()`
**Test File:** `src/test/java/com/carebridge/backend/baby/service/BabyServiceUpdateTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-BABY-004, BR-BABY-012`

**Preconditions:**
- Mother authenticated with MOTHER_ID
- Baby profile exists with status ARCHIVED and owner = MOTHER_ID (FX-003)

**Test Steps:**
1. Arrange: Mock `babyRepository.findById(ARCHIVED_BABY_ID)` -> returns FX-003 archived profile
2. Act: Call `babyService.updateBabyProfile(ARCHIVED_BABY_ID, request, MOTHER_ID)`
3. Assert: Throws exception with error code BABY-012

**Expected Result (PASS):**
- Throws BadRequestException with code BABY-012
- `babyRepository.save()` NOT called

**Expected Result (FAIL):**
- Update succeeds on archived baby

**Current Status:** RED — Not written

---

### BABY-TC-032-005 — No JWT -> 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/baby/controller/BabyControllerUpdateTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- No JWT token in request

**Test Steps:**
1. Arrange: @WebMvcTest with no JWT mock
2. Act: PUT /api/v1/babies/{babyId} without Authorization header
3. Assert: Response status 401

**Expected Result (PASS):**
- 401 Unauthorized

**Expected Result (FAIL):**
- Request reaches controller without authentication

**Current Status:** RED — Not written

---

### INTEGRATION TEST CASES

---

### BABY-TC-032-INT-001 — Full DB persistence after update

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: Controller -> Service -> Repository -> DB`
**Test File:** `src/test/java/com/carebridge/backend/baby/BabyProfileUpdateIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-006`

**Preconditions:**
- PostgreSQL container running (@Testcontainers)
- Flyway migration applied
- Seed: Insert baby profile with BABY_ID, MOTHER_ID, status=ACTIVE, nickname="Bean"

**Test Steps:**
1. Seed baby profile via repository.save()
2. PUT /api/v1/babies/{BABY_ID} with JWT(MOTHER_ID) and body {nickname: "Updated Bean"}
3. Assert response 200
4. Assert DB: nickname = "Updated Bean"
5. Assert DB: owner_user_id unchanged = MOTHER_ID
6. Assert DB: status unchanged = "ACTIVE"
7. Assert DB: updated_at > created_at

**Expected Result (PASS):**
- Database row updated with new nickname
- Immutable fields (owner_user_id, status) remain unchanged
- updated_at timestamp refreshed

**DB Assertion:**
```java
BabyProfile updated = babyRepo.findById(BABY_ID).orElseThrow();
assertThat(updated.getNickname()).isEqualTo("Updated Bean");
assertThat(updated.getOwnerUserId()).isEqualTo(MOTHER_ID);
assertThat(updated.getStatus()).isEqualTo("ACTIVE");
assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
```

**Current Status:** RED — Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit) | REFACTOR note |
|-------|-----------|---------------|----------------|---------------|
| `BABY-TC-032-001` | `BabyServiceUpdateTest.java` | `[ ]` | `___` | — |
| `BABY-TC-032-002` | `BabyServiceUpdateTest.java` | `[ ]` | `___` | — |
| `BABY-TC-032-003` | `BabyServiceUpdateTest.java` | `[ ]` | `___` | — |
| `BABY-TC-032-004` | `BabyServiceUpdateTest.java` | `[ ]` | `___` | — |
| `BABY-TC-032-005` | `BabyControllerUpdateTest.java` | `[ ]` | `___` | — |
| `BABY-TC-032-INT-001` | `BabyProfileUpdateIntegrationTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class BabyService implements IBabyService {

    @Override
    public UpdateBabyProfileResponse updateBabyProfile(UUID babyId, UpdateBabyProfileRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause |
|-------|-------------|----------|--------|------------|
| `BABY-TC-032-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-032-002` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-032-003` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-032-004` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-032-005` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-032-INT-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tat ca FAIL? [ ] Yes -> **GATE-2 PASS** (T2->T3) -> tiep tuc implement

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-BABY-IMP-002` da duoc review va approve
- [ ] baby_profiles table exists (from UC-31 migration)
- [ ] Test fixtures (Section 3 TDS-05) da duoc chuan bi

### Exit Criteria
- [ ] `./mvnw test` — tat ca unit tests xanh
- [ ] `./mvnw verify` — tat ca integration tests xanh
- [ ] Test coverage >= 80% cho BabyService update methods
- [ ] Khong co business logic trong Controller
- [ ] Khong co PII trong logs
- [ ] Red Gate (S5.1) — tat ca tests FAIL voi stub truoc khi implement
- [ ] Immutable fields (owner_user_id, status) remain unchanged in integration test

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/baby/dto/UpdateBabyProfileRequest.java
git checkout -- src/main/java/com/carebridge/backend/baby/dto/UpdateBabyProfileResponse.java
git checkout -- src/test/java/com/carebridge/backend/baby/service/BabyServiceUpdateTest.java
git checkout -- src/test/java/com/carebridge/backend/baby/controller/BabyControllerUpdateTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dau hieu trong TDD spec | Check | Gate chan |
|-------|-------------|--------------------------|-------|----------|
| AP-AI-001 | Unconstrained Generation | TC khong reference ADR/TDS constraint nao | [ ] | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS voi stub (S5.1) | [ ] | G-2 |
| AP-AI-003 | Implicit Decision | Test assume architecture khong co ADR | [ ] | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller co business logic | [ ] | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type khong ton tai | [ ] | G-3 |

**Ket qua review:**
- [ ] Khong phat hien anti-pattern nao -> TDD spec approved
- [ ] Phat hien AP -> ghi vao bang duoi -> fix truoc khi implement

| AP detected | TC ID | Mo ta | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| | | | | |

---

*TDD Template v2.0 — Tich hop CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
