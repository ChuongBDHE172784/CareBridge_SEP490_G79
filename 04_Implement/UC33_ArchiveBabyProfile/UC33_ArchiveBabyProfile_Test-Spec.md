# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-33 Archive Baby Profile

**Document ID:** `CB-BABY-IMP-003-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`
**Classification:** `Internal — Confidential`

**References:**
- TDS: `04_Implement/UC33_ArchiveBabyProfile/UC33_ArchiveBabyProfile_TDS.md` (CB-BABY-IMP-003)
- SRS: S3.3.1.10

---

## CHANGELOG

| Ngay | Nguoi thuc hien | Noi dung thay doi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khoi tao TDD spec cho UC-33 Archive Baby Profile |

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
| **Feature / Gap ID** | `UC-33` |
| **Module** | `ArchiveBabyProfile — carejourney` |
| **Spec goc** | `CB-BABY-IMP-003` |
| **Priority** | P1 |
| **Data Classification** | `Sensitive-PII` |
| **Upstream Dependencies** | `auth, UC-31 (baby_profiles)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-BABY-IMP-003 S17` |
| **Constraints Injected** | C1 (ownership), C2 (set status only), C3 (linked data preserved), C4 (audit), C5 (not idempotent) |
| **Model** | `Claude Sonnet 4.6` |
| **Trust Level** | `T2 -> T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec goc (sai / thieu) | Thuc te (schema / policy) | Fix ap dung trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS: "hides or archives" — ambiguous about data deletion | ADR-BABY-005/006: soft archive only, NO data deletion | Test verifies baby_daily_logs rows preserved after archive |
| L2 | SRS: does not specify idempotency for archive | ADR-BABY-005: NOT idempotent — already archived returns BABY-022 | Test verifies 400 for already archived baby |
| L3 | SRS: does not specify HTTP method | ADR-BABY-005: POST (not DELETE) — archive is state transition | Test uses POST method |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
ArchiveBabyProfile bao gom cac layer:
+-- Service (mock BabyProfileRepository voi Mockito)
+-- Controller (mock BabyService voi @WebMvcTest)
+-- Integration (Testcontainers PostgreSQL voi @SpringBootTest)
```

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Valid archive of ACTIVE baby | `BabyService.archiveBabyProfile()` | `BABY-TC-033-001` |
| TC-COND-002 | Already archived baby | `BabyService.checkNotArchived()` | `BABY-TC-033-002` |
| TC-COND-003 | Baby not owned by JWT user | `BabyService.checkOwnership()` | `BABY-TC-033-003` |
| TC-COND-004 | Baby not found | `BabyService.archiveBabyProfile()` | `BABY-TC-033-004` |
| TC-COND-005 | No JWT token | Spring Security filter | `BABY-TC-033-005` |
| TC-COND-006 | DB: status=ARCHIVED + linked data preserved | Full flow | `BABY-TC-033-INT-001` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| State Transition Testing | ACTIVE -> ARCHIVED | Core state machine for baby profile |
| Equivalence Partitioning | Ownership: match vs mismatch | Binary authorization check |
| Error Guessing | Double archive, missing JWT | Edge cases and security vectors |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Muc dich |
|-----------|------|---------------|---------|
| `FX-001` | JWT | `{sub: MOTHER_ID, role: 'MOTHER'}` | Happy path auth |
| `FX-002` | DB seed | `{babyId: BABY_ID, ownerUserId: MOTHER_ID, status: 'ACTIVE'}` | Active baby to archive |
| `FX-003` | DB seed | `{babyId: ARCHIVED_BABY_ID, ownerUserId: MOTHER_ID, status: 'ARCHIVED'}` | Already archived baby |
| `FX-004` | DB seed | `{babyId: OTHER_BABY_ID, ownerUserId: OTHER_MOTHER_ID, status: 'ACTIVE'}` | Baby owned by another |
| `FX-005` | DB seed | `{babyLogId: LOG_ID, babyId: BABY_ID, logType: 'FEEDING'}` | Linked daily log for preservation check |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// BabyProfileArchiveTestFactory.java
class BabyProfileArchiveTestFactory {

    static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000033");
    static final UUID BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000033");
    static final UUID OTHER_MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID OTHER_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000099");
    static final UUID ARCHIVED_BABY_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000088");
    static final UUID LOG_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000033");

    static BabyProfile makeActiveBaby() {
        BabyProfile p = new BabyProfile();
        p.setBabyId(BABY_ID);
        p.setOwnerUserId(MOTHER_ID);
        p.setNickname("Bean");
        p.setStatus("ACTIVE");
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    static BabyProfile makeArchivedBaby() {
        BabyProfile p = makeActiveBaby();
        p.setBabyId(ARCHIVED_BABY_ID);
        p.setStatus("ARCHIVED");
        return p;
    }

    static BabyProfile makeOtherMotherBaby() {
        BabyProfile p = makeActiveBaby();
        p.setBabyId(OTHER_BABY_ID);
        p.setOwnerUserId(OTHER_MOTHER_ID);
        return p;
    }

    static BabyDailyLog makeLinkedLog() {
        BabyDailyLog log = new BabyDailyLog();
        log.setBabyLogId(LOG_ID);
        log.setBabyId(BABY_ID);
        log.setLogType("FEEDING");
        log.setQuantity(new BigDecimal("120"));
        log.setUnit("ml");
        log.setCreatedAt(Instant.now());
        return log;
    }
}
```

---

### BABY-TC-033-001 — Happy path archive -> 200

**Severity:** `CRITICAL`
**Feature Under Test:** `BabyService.archiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyServiceArchiveTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-33 Normal Flow, ADR-BABY-005`

**Preconditions:**
- Mother authenticated (FX-001)
- Baby profile exists with status ACTIVE and owner = MOTHER_ID (FX-002)

**Test Steps:**
1. Arrange: Mock `babyRepository.findById(BABY_ID)` -> returns FX-002 active profile
2. Arrange: Mock `babyRepository.save()` -> returns profile with status ARCHIVED
3. Act: Call `babyService.archiveBabyProfile(BABY_ID, MOTHER_ID)`
4. Assert: Response status = "ARCHIVED"
5. Assert: `babyRepository.save()` called with profile having status = "ARCHIVED"
6. Assert: `auditService.emit(BABY_PROFILE_ARCHIVED)` called once

**Expected Result (PASS):**
- Returns ArchiveBabyProfileResponse with status = "ARCHIVED"
- Profile saved with status = "ARCHIVED"
- Audit event emitted

**Expected Result (FAIL):**
- Status not changed, or data deleted, or audit not emitted

**Current Status:** RED — Not written

---

### BABY-TC-033-002 — Already archived -> 400 BABY-022

**Severity:** `HIGH`
**Feature Under Test:** `BabyService.checkNotArchived()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyServiceArchiveTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-BABY-022, ADR-BABY-005`

**Preconditions:**
- Mother authenticated with MOTHER_ID
- Baby profile exists with status ARCHIVED and owner = MOTHER_ID (FX-003)

**Test Steps:**
1. Arrange: Mock `babyRepository.findById(ARCHIVED_BABY_ID)` -> returns FX-003 archived profile
2. Act: Call `babyService.archiveBabyProfile(ARCHIVED_BABY_ID, MOTHER_ID)`
3. Assert: Throws exception with error code BABY-022

**Expected Result (PASS):**
- Throws BadRequestException with code BABY-022
- `babyRepository.save()` NOT called
- No audit event emitted

**Expected Result (FAIL):**
- Archive succeeds silently (idempotent behavior not desired)

**Current Status:** RED — Not written

---

### BABY-TC-033-003 — Baby not owned -> 403 BABY-021

**Severity:** `CRITICAL`
**OWASP:** `A01:2021 — Broken Access Control`
**Feature Under Test:** `BabyService.checkOwnership()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyServiceArchiveTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-BABY-021, BR-RBAC`

**Preconditions:**
- Mother authenticated with MOTHER_ID
- Baby profile exists but owned by OTHER_MOTHER_ID (FX-004)

**Test Steps:**
1. Arrange: Mock `babyRepository.findById(OTHER_BABY_ID)` -> returns FX-004 profile
2. Act: Call `babyService.archiveBabyProfile(OTHER_BABY_ID, MOTHER_ID)`
3. Assert: Throws exception with error code BABY-021

**Expected Result (PASS):**
- Throws ForbiddenException with code BABY-021
- `babyRepository.save()` NOT called

**Expected Result (FAIL):**
- Archive succeeds despite ownership mismatch (access control bypass)

**Current Status:** RED — Not written

---

### BABY-TC-033-004 — Baby not found -> 404 BABY-020

**Severity:** `HIGH`
**Feature Under Test:** `BabyService.archiveBabyProfile()`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/service/BabyServiceArchiveTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-BABY-020`

**Preconditions:**
- Mother authenticated

**Test Steps:**
1. Arrange: Mock `babyRepository.findById(NON_EXISTENT_ID)` -> returns Optional.empty()
2. Act: Call `babyService.archiveBabyProfile(NON_EXISTENT_ID, MOTHER_ID)`
3. Assert: Throws exception with error code BABY-020

**Expected Result (PASS):**
- Throws NotFoundException with code BABY-020
- `babyRepository.save()` NOT called

**Current Status:** RED — Not written

---

### BABY-TC-033-005 — No JWT -> 401

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**Feature Under Test:** `Spring Security filter chain`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/controller/BabyControllerArchiveTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-RBAC`

**Preconditions:**
- No JWT token in request

**Test Steps:**
1. Arrange: @WebMvcTest with no JWT mock
2. Act: POST /api/v1/babies/{babyId}/archive without Authorization header
3. Assert: Response status 401

**Expected Result (PASS):**
- 401 Unauthorized

**Current Status:** RED — Not written

---

### INTEGRATION TEST CASES

---

### BABY-TC-033-INT-001 — Full DB: status=ARCHIVED + linked data NOT deleted

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: Controller -> Service -> Repository -> DB`
**Test File:** `src/test/java/com/carebridge/backend/carejourney/BabyProfileArchiveIntegrationTest.java`
**TDD Phase:** RED
**Condition Ref:** `TC-COND-006`

**Preconditions:**
- PostgreSQL container running (@Testcontainers)
- Flyway migration applied
- Seed: Insert baby profile with BABY_ID, MOTHER_ID, status=ACTIVE
- Seed: Insert baby_daily_log with LOG_ID, baby_id=BABY_ID, log_type='FEEDING' (FX-005)

**Test Steps:**
1. Seed baby profile and linked daily log via repository
2. POST /api/v1/babies/{BABY_ID}/archive with JWT(MOTHER_ID)
3. Assert response 200 with status "ARCHIVED"
4. Assert DB: baby_profiles.status = 'ARCHIVED' for BABY_ID
5. Assert DB: baby_daily_logs row with LOG_ID still exists (NOT deleted)
6. Assert DB: baby_profiles.updated_at > created_at

**Expected Result (PASS):**
- Baby profile status changed to ARCHIVED in database
- Linked baby_daily_logs row still exists (soft archive — no cascading delete)
- updated_at timestamp refreshed

**DB Assertion:**
```java
// Verify baby archived
BabyProfile archived = babyRepo.findById(BABY_ID).orElseThrow();
assertThat(archived.getStatus()).isEqualTo("ARCHIVED");
assertThat(archived.getUpdatedAt()).isAfter(archived.getCreatedAt());

// Verify linked data NOT deleted
Optional<BabyDailyLog> log = dailyLogRepo.findById(LOG_ID);
assertThat(log).isPresent();
assertThat(log.get().getBabyId()).isEqualTo(BABY_ID);
```

**Current Status:** RED — Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | RED confirmed | GREEN (commit) | REFACTOR note |
|-------|-----------|---------------|----------------|---------------|
| `BABY-TC-033-001` | `BabyServiceArchiveTest.java` | `[ ]` | `___` | — |
| `BABY-TC-033-002` | `BabyServiceArchiveTest.java` | `[ ]` | `___` | — |
| `BABY-TC-033-003` | `BabyServiceArchiveTest.java` | `[ ]` | `___` | — |
| `BABY-TC-033-004` | `BabyServiceArchiveTest.java` | `[ ]` | `___` | — |
| `BABY-TC-033-005` | `BabyControllerArchiveTest.java` | `[ ]` | `___` | — |
| `BABY-TC-033-INT-001` | `BabyProfileArchiveIntegrationTest.java` | `[ ]` | `___` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class BabyService implements IBabyService {

    @Override
    public ArchiveBabyProfileResponse archiveBabyProfile(UUID babyId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause |
|-------|-------------|----------|--------|------------|
| `BABY-TC-033-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-033-002` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-033-003` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-033-004` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-033-005` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |
| `BABY-TC-033-INT-001` | `throw('Not implemented')` | FAIL | [ ] FAIL [ ] PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tat ca FAIL? [ ] Yes -> **GATE-2 PASS** (T2->T3) -> tiep tuc implement

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-BABY-IMP-003` da duoc review va approve
- [ ] baby_profiles table exists (from UC-31 migration)
- [ ] Test fixtures (Section 3 TDS-05) da duoc chuan bi

### Exit Criteria
- [ ] `./mvnw test` — tat ca unit tests xanh
- [ ] `./mvnw verify` — tat ca integration tests xanh
- [ ] Test coverage >= 80% cho BabyService archive methods
- [ ] Khong co business logic trong Controller
- [ ] Khong co data deletion — chi status change
- [ ] Red Gate (S5.1) — tat ca tests FAIL voi stub truoc khi implement
- [ ] Linked data (baby_daily_logs) preserved after archive in integration test

---

## 7. Rollback Plan

```bash
git checkout -- src/main/java/com/carebridge/backend/carejourney/dto/ArchiveBabyProfileResponse.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/service/BabyServiceArchiveTest.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/controller/BabyControllerArchiveTest.java
git checkout -- src/test/java/com/carebridge/backend/carejourney/BabyProfileArchiveIntegrationTest.java
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dau hieu trong TDD spec | Check | Gate chan |
|-------|-------------|--------------------------|-------|----------|
| AP-AI-001 | Unconstrained Generation | TC khong reference ADR/TDS constraint nao | [ ] | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS voi stub (S5.1) | [ ] | G-2 |
| AP-AI-003 | Implicit Decision | Test assume DELETE instead of status change | [ ] | G-1 |
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
