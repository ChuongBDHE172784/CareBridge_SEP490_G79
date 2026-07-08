# UC18 — Revoke Data Permission: Test Specification

| Field            | Value                              |
|------------------|------------------------------------|
| Document ID      | CB-CONSENT-IMP-018-TEST            |
| Version          | 1.0                                |
| Date             | 2026-06-26                         |
| Status           | Implemented         |
| Document Owner   | PhuongNT                           |
| Author           | AI Agent                           |
| Based on EDS     | v2.0                               |
| TDS Reference    | CB-CONSENT-IMP-018                 |
| SRS Reference    | 3.1.1.18                           |

---

## 1. Scope & Objectives

### 1.1 Feature Under Test

UC-18 **Revoke Data Permission** — The authenticated owner of a consent grant revokes it by calling `DELETE /api/v1/consent/grants/{consentId}`. The system performs a soft revoke (sets `revokedAt` and `revokedBy`), does not delete the record, and emits `CONSENT_REVOKED` audit event.

### 1.2 Test Objectives

1. Verify soft revoke sets `revokedAt` and `revokedBy` correctly in DB
2. Verify ownership enforcement: non-owners receive 404 (existence leakage prevention)
3. Verify idempotency: second revoke on already-revoked consent returns 400 CONSENT-012
4. Verify unauthenticated requests return 401
5. Verify audit event `CONSENT_REVOKED` is emitted on success
6. Verify the record is NOT hard-deleted (still present in DB after revoke)
7. Verify revoked grants still appear in `listConsents()` response

### 1.3 Test Entry Criteria

- [ ] Application deployed or `@SpringBootTest` context available
- [ ] `consent_grants` table created via Flyway migration
- [ ] JWT tokens available for test users
- [ ] Seed consent grants created for test scenarios
- [ ] `ConsentRevokeTestFactory` available in test package

### 1.4 Test Exit Criteria

- [ ] All test cases executed
- [ ] All P1 cases `PASS`
- [ ] Integration test verifies DB state (revokedAt, revokedBy, row retained)
- [ ] No P1/P2 defects open

---

## 2. Logic Issues & Design Notes

### L1: "Not Found" and "Not Owned" Return Same 404

**Design Decision**: `findByIdAndUserId(consentId, userId)` returns `Optional.empty()` for both cases:
- `consentId` does not exist in DB
- `consentId` exists but belongs to a different `userId`

Both result in `404 CONSENT-011`. This is intentional (ADR-CONSENT-018-002) — prevents attackers from probing whether a consent ID exists by using another user's credentials.

**Test implication**: CONSENT-TC-018-002 (wrong owner) and CONSENT-TC-018-004 (not found) should both expect `404 CONSENT-011`. The test cannot distinguish which branch was taken — this is correct behavior by design.

---

### L2: revokedBy Must Come From JWT, Not Request Body

**Issue**: `revokedBy` MUST be set from the JWT-extracted `userId`, never from any client-supplied parameter or request body. A `DELETE` request has no body in standard REST.

**Test implication**: CONSENT-TC-018-INT-001 must assert that `revoked_by` in DB equals `TEST_USER_ID` (the JWT owner), not any other value.

---

### L3: Revoked Grants Still Visible in listConsents()

**Design Decision**: After revoke, `GET /api/v1/consent/grants` (listConsents) must still return the revoked grant, now with `revokedAt` populated. This serves:
- User transparency: "I can see what I revoked and when"
- Audit trail: history of all consent actions

**Test implication**: CONSENT-TC-018-INT-001 should verify the row still exists in DB. CONSENT-TC-018-VIS-001 (if implemented) should verify the revoked grant appears in the list endpoint response.

---

### L4: Idempotency Is NOT 200

**Issue**: RFC 7231 allows DELETE to be idempotent (second call = same effect). However, this project explicitly returns `400 CONSENT-012` for a second revoke attempt (ADR-CONSENT-018-003).

**Rationale**: The first revoke already changed state; a second call is semantically different and should signal the caller that the action was redundant. This is a deliberate deviation from strict RFC idempotency to prevent silent double-processing.

**Test implication**: CONSENT-TC-018-003 MUST assert `400` (not `200` or `204`) for the second revoke.

---

## 3. Test Environment

### 3.1 Infrastructure Requirements

| Component      | Requirement                                              |
|----------------|----------------------------------------------------------|
| Application    | Spring Boot test context or live environment             |
| Database       | PostgreSQL with `consent_grants` table (Flyway applied)  |
| Auth           | JWT tokens for multiple test users                       |
| Test Framework | JUnit 5 + Mockito (unit) / MockMvc + Testcontainers (IT) |

### 3.2 Test User Accounts

```
TEST_USER_ID    = "00000000-0000-0000-0000-000000000018"   ← consent owner (UC-18 test user)
OTHER_USER_ID   = "00000000-0000-0000-0000-000000000099"   ← different user (ownership test)
```

### 3.3 Test Data Factory

```java
package com.carebridge.backend.consent.test;

import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Test data factory for UC18 revoke consent scenarios.
 * Props isolated — never reuse state between test cases.
 */
class ConsentRevokeTestFactory {

    static final Long TEST_CONSENT_ID = 1001L;
    static final Long OTHER_USER_CONSENT_ID = 1002L;
    static final Long ALREADY_REVOKED_ID = 1003L;
    static final Long NON_EXISTENT_ID = 99999L;

    static final UUID TEST_USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000018");
    static final UUID OTHER_USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000099");

    /**
     * Active (not revoked) consent grant owned by TEST_USER_ID.
     * Use for: happy path revoke, idempotency test setup.
     */
    static ConsentGrant makeActiveGrant() {
        return ConsentGrant.builder()
            .id(TEST_CONSENT_ID)
            .userId(TEST_USER_ID)
            .dataType(ConsentDataType.HEALTH_RECORD)
            .purpose(ConsentPurpose.VIEW)
            .recipient("dr.recipient@hospital.test")
            .scope("Test scope for UC-18")
            .consentGivenAt(Instant.now().minus(5, ChronoUnit.DAYS))
            .expiryAt(Instant.now().plus(30, ChronoUnit.DAYS))
            .revokedAt(null)
            .revokedBy(null)
            .version(1)
            .build();
    }

    /**
     * Active consent grant owned by OTHER_USER_ID.
     * Use for: unauthorized revoke test (CONSENT-TC-018-002).
     */
    static ConsentGrant makeOtherUserGrant() {
        return ConsentGrant.builder()
            .id(OTHER_USER_CONSENT_ID)
            .userId(OTHER_USER_ID)
            .dataType(ConsentDataType.LOCATION)
            .purpose(ConsentPurpose.VIEW)
            .recipient("someone@else.test")
            .consentGivenAt(Instant.now().minus(10, ChronoUnit.DAYS))
            .expiryAt(Instant.now().plus(20, ChronoUnit.DAYS))
            .revokedAt(null)
            .revokedBy(null)
            .version(1)
            .build();
    }

    /**
     * Already-revoked consent grant owned by TEST_USER_ID.
     * Use for: idempotency / double-revoke test (CONSENT-TC-018-003).
     */
    static ConsentGrant makeAlreadyRevokedGrant() {
        Instant grantTime = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant revokeTime = Instant.now().minus(2, ChronoUnit.DAYS);
        return ConsentGrant.builder()
            .id(ALREADY_REVOKED_ID)
            .userId(TEST_USER_ID)
            .dataType(ConsentDataType.HEALTH_RECORD)
            .purpose(ConsentPurpose.VIEW)
            .recipient("dr.recipient@hospital.test")
            .consentGivenAt(grantTime)
            .expiryAt(grantTime.plus(30, ChronoUnit.DAYS))
            .revokedAt(revokeTime)
            .revokedBy(TEST_USER_ID)
            .version(2)
            .build();
    }
}
```

---

## 4. Unit Test Cases

### CONSENT-TC-018-001: Happy Path — Revoke Own Consent

| Field          | Value                                                         |
|----------------|---------------------------------------------------------------|
| Test ID        | CONSENT-TC-018-001                                            |
| Title          | Revoke own active consent returns 200 with revokedAt set      |
| Priority       | P1                                                            |
| Type           | Unit / Integration                                            |
| Endpoint       | DELETE /api/v1/consent/grants/{consentId}                     |

**Preconditions:**
- Authenticated as TEST_USER_ID
- Active consent grant exists with id=TEST_CONSENT_ID, userId=TEST_USER_ID, revokedAt=null

**Input:**
- Path: `/api/v1/consent/grants/1001`
- Authorization: Bearer JWT for TEST_USER_ID
- Body: None

**Expected Outcome:**

| Check              | Expected Value                                              |
|--------------------|-------------------------------------------------------------|
| HTTP Status        | `200 OK`                                                    |
| `success`          | `true`                                                      |
| `data.id`          | `1001`                                                      |
| `data.revokedAt`   | Non-null, within last 5 seconds of NOW()                    |
| `data.revokedBy`   | `"00000000-0000-0000-0000-000000000018"` (TEST_USER_ID)     |

**Post-conditions:**
- `consent_grants` row with id=1001: `revoked_at IS NOT NULL`, `revoked_by = TEST_USER_ID`
- Row still exists (NOT deleted)
- `CONSENT_REVOKED` audit event emitted

**Test Code Sketch:**
```java
@Test
void revokeConsent_ownActiveConsent_returns200() throws Exception {
    // Given: active grant in DB (or mocked via repository)
    consentGrantRepository.save(ConsentRevokeTestFactory.makeActiveGrant());
    String jwt = jwtHelper.tokenFor(TEST_USER_ID);

    // When
    mockMvc.perform(delete("/api/v1/consent/grants/{id}", TEST_CONSENT_ID)
            .header("Authorization", "Bearer " + jwt))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.revokedAt").isNotEmpty())
        .andExpect(jsonPath("$.data.revokedBy").value(TEST_USER_ID.toString()));
}
```

---

### CONSENT-TC-018-002: Revoke Another User's Consent → 404

| Field          | Value                                                         |
|----------------|---------------------------------------------------------------|
| Test ID        | CONSENT-TC-018-002                                            |
| Title          | Attempting to revoke another user's consent returns 404       |
| Priority       | P1                                                            |
| Type           | Unit                                                          |
| Endpoint       | DELETE /api/v1/consent/grants/{consentId}                     |

**Preconditions:**
- Authenticated as TEST_USER_ID
- Consent OTHER_USER_CONSENT_ID exists and belongs to OTHER_USER_ID

**Input:**
- Path: `/api/v1/consent/grants/1002` (owned by OTHER_USER_ID)
- Authorization: Bearer JWT for TEST_USER_ID (not the owner)

**Expected Outcome:**

| Check          | Expected Value                                              |
|----------------|-------------------------------------------------------------|
| HTTP Status    | `404 Not Found`                                             |
| `success`      | `false`                                                     |
| `errorCode`    | `"CONSENT-011"`                                             |
| `message`      | `"Consent not found"` (no ownership info disclosed)         |

**Post-conditions:**
- No change to DB (revokedAt remains null for OTHER_USER_CONSENT_ID)
- No audit event emitted

**Notes:**
- Same 404 as "not found" — prevents existence leakage (L1 in §2)
- Attacker cannot determine if consent 1002 exists at all

**Test Code Sketch:**
```java
@Test
void revokeConsent_anotherUsersConsent_returns404() throws Exception {
    consentGrantRepository.save(ConsentRevokeTestFactory.makeOtherUserGrant());
    String jwt = jwtHelper.tokenFor(TEST_USER_ID);  // Not the owner

    mockMvc.perform(delete("/api/v1/consent/grants/{id}", OTHER_USER_CONSENT_ID)
            .header("Authorization", "Bearer " + jwt))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("CONSENT-011"));
}
```

---

### CONSENT-TC-018-003: Revoke Already-Revoked Consent → 400

| Field          | Value                                                         |
|----------------|---------------------------------------------------------------|
| Test ID        | CONSENT-TC-018-003                                            |
| Title          | Revoking an already-revoked consent returns 400 CONSENT-012   |
| Priority       | P1                                                            |
| Type           | Unit                                                          |
| Endpoint       | DELETE /api/v1/consent/grants/{consentId}                     |

**Preconditions:**
- Authenticated as TEST_USER_ID
- Consent ALREADY_REVOKED_ID exists with `revokedAt IS NOT NULL`

**Input:**
- Path: `/api/v1/consent/grants/1003`
- Authorization: Bearer JWT for TEST_USER_ID

**Expected Outcome:**

| Check          | Expected Value                                        |
|----------------|-------------------------------------------------------|
| HTTP Status    | `400 Bad Request`                                     |
| `success`      | `false`                                               |
| `errorCode`    | `"CONSENT-012"`                                       |
| `message`      | Contains "already been revoked" or equivalent         |

**Post-conditions:**
- DB state unchanged (revokedAt and revokedBy not overwritten)
- No duplicate audit event emitted

**Test Code Sketch:**
```java
@Test
void revokeConsent_alreadyRevoked_returns400() throws Exception {
    consentGrantRepository.save(ConsentRevokeTestFactory.makeAlreadyRevokedGrant());
    String jwt = jwtHelper.tokenFor(TEST_USER_ID);

    mockMvc.perform(delete("/api/v1/consent/grants/{id}", ALREADY_REVOKED_ID)
            .header("Authorization", "Bearer " + jwt))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("CONSENT-012"));
}
```

---

### CONSENT-TC-018-004: Consent ID Not Found → 404

| Field          | Value                                                         |
|----------------|---------------------------------------------------------------|
| Test ID        | CONSENT-TC-018-004                                            |
| Title          | Non-existent consentId returns 404 CONSENT-011                |
| Priority       | P1                                                            |
| Type           | Unit                                                          |
| Endpoint       | DELETE /api/v1/consent/grants/{consentId}                     |

**Preconditions:**
- Authenticated as TEST_USER_ID
- No consent exists with id=NON_EXISTENT_ID (99999)

**Input:**
- Path: `/api/v1/consent/grants/99999`
- Authorization: Bearer JWT for TEST_USER_ID

**Expected Outcome:**

| Check          | Expected Value      |
|----------------|---------------------|
| HTTP Status    | `404 Not Found`     |
| `success`      | `false`             |
| `errorCode`    | `"CONSENT-011"`     |

**Notes:**
- Same error code as CONSENT-TC-018-002 (existence leakage prevention, L1 in §2)

**Test Code Sketch:**
```java
@Test
void revokeConsent_nonExistentId_returns404() throws Exception {
    String jwt = jwtHelper.tokenFor(TEST_USER_ID);

    mockMvc.perform(delete("/api/v1/consent/grants/{id}", NON_EXISTENT_ID)
            .header("Authorization", "Bearer " + jwt))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("CONSENT-011"));
}
```

---

### CONSENT-TC-018-005: No JWT → 401

| Field          | Value                                                         |
|----------------|---------------------------------------------------------------|
| Test ID        | CONSENT-TC-018-005                                            |
| Title          | Request without JWT returns 401 Unauthorized                  |
| Priority       | P1                                                            |
| Type           | Unit                                                          |
| Endpoint       | DELETE /api/v1/consent/grants/{consentId}                     |

**Preconditions:**
- No Authorization header

**Input:**
- Path: `/api/v1/consent/grants/1001`
- No Authorization header

**Expected Outcome:**

| Check          | Expected Value                |
|----------------|-------------------------------|
| HTTP Status    | `401 Unauthorized`            |
| `errorCode`    | `"IAM-001"` (if mapped)       |

**Post-conditions:**
- No DB change
- No audit event

**Test Code Sketch:**
```java
@Test
void revokeConsent_noJwt_returns401() throws Exception {
    mockMvc.perform(delete("/api/v1/consent/grants/{id}", TEST_CONSENT_ID))
        .andExpect(status().isUnauthorized());
}
```

---

## 5. Integration Test Cases

### CONSENT-TC-018-INT-001: DB revokedAt and revokedBy Verified After Revoke

| Field          | Value                                                          |
|----------------|----------------------------------------------------------------|
| Test ID        | CONSENT-TC-018-INT-001                                         |
| Title          | Integration — verify DB state after soft revoke               |
| Priority       | P1                                                             |
| Type           | Integration                                                    |

**Preconditions:**
- `@SpringBootTest` with embedded PostgreSQL or Testcontainers
- Flyway migrations applied
- Active consent grant persisted via factory

**Steps:**

1. Persist active grant via `consentGrantRepository.save(makeActiveGrant())`
2. Call `DELETE /api/v1/consent/grants/1001` with TEST_USER_ID JWT
3. Assert HTTP 200
4. Query DB for the row

**Verification SQL:**
```sql
SELECT
    id,
    user_id,
    revoked_at,
    revoked_by,
    version
FROM consent_grants
WHERE id = 1001;
```

**Expected DB State:**

| Column      | Expected Value                                         |
|-------------|--------------------------------------------------------|
| id          | 1001                                                   |
| user_id     | `00000000-0000-0000-0000-000000000018`                 |
| revoked_at  | NOT NULL; within last 5 seconds                        |
| revoked_by  | `00000000-0000-0000-0000-000000000018` (TEST_USER_ID)  |
| version     | 2 (incremented from 1 by optimistic locking)           |

**Also verify: row NOT deleted**
```sql
SELECT COUNT(*) FROM consent_grants WHERE id = 1001;
-- Expected: 1 (NOT 0)
```

**Test Code Sketch:**
```java
@Test
@Transactional
void revokeConsent_integration_dbStateCorrectAfterRevoke() throws Exception {
    // Setup
    ConsentGrant grant = ConsentRevokeTestFactory.makeActiveGrant();
    consentGrantRepository.save(grant);
    entityManager.flush();
    entityManager.clear();

    String jwt = jwtHelper.tokenFor(TEST_USER_ID);

    // Execute
    mockMvc.perform(delete("/api/v1/consent/grants/{id}", TEST_CONSENT_ID)
            .header("Authorization", "Bearer " + jwt))
        .andExpect(status().isOk());

    // Verify DB
    entityManager.flush();
    ConsentGrant revoked = consentGrantRepository.findById(TEST_CONSENT_ID)
        .orElseThrow(() -> new AssertionError("Row must not be deleted — soft revoke only"));

    assertAll(
        () -> assertNotNull(revoked.getRevokedAt(),
            "revokedAt must be set after revoke"),
        () -> assertEquals(TEST_USER_ID, revoked.getRevokedBy(),
            "revokedBy must equal JWT userId (L2: never from request body)"),
        () -> assertTrue(
            Duration.between(revoked.getRevokedAt(), Instant.now()).getSeconds() < 10,
            "revokedAt must be close to NOW()"),
        () -> assertEquals(2, revoked.getVersion(),
            "version must be incremented by optimistic locking")
    );
}
```

---

### CONSENT-TC-018-INT-002: Revoked Grant Still Appears in listConsents

| Field          | Value                                                          |
|----------------|----------------------------------------------------------------|
| Test ID        | CONSENT-TC-018-INT-002                                         |
| Title          | Integration — revoked grant is still returned by GET /grants   |
| Priority       | P2                                                             |
| Type           | Integration                                                    |

**Preconditions:**
- Active grant seeded
- Revoke executed (or pre-seed revoked grant)

**Steps:**
1. Persist already-revoked grant via `makeAlreadyRevokedGrant()`
2. Call `GET /api/v1/consent/grants` with TEST_USER_ID JWT

**Expected Outcome:**
- Response includes the revoked grant in the list
- `data[].revokedAt` is non-null for the revoked entry

**Verification:**
```java
mockMvc.perform(get("/api/v1/consent/grants")
        .header("Authorization", "Bearer " + jwt))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data[?(@.id == 1003)].revokedAt").isNotEmpty());
```

---

### CONSENT-TC-018-INT-003: Audit Event Emitted on Revoke

| Field          | Value                                                          |
|----------------|----------------------------------------------------------------|
| Test ID        | CONSENT-TC-018-INT-003                                         |
| Title          | Integration — CONSENT_REVOKED audit event emitted after revoke |
| Priority       | P2                                                             |
| Type           | Integration                                                    |

**Steps:**
1. Revoke active consent (CONSENT-TC-018-001 setup)
2. Query audit log table

**Verification SQL:**
```sql
SELECT action, actor_id, resource_type, resource_id, created_at
FROM audit_logs
WHERE action = 'CONSENT_REVOKED'
  AND actor_id = '00000000-0000-0000-0000-000000000018'
  AND resource_id = '1001'
ORDER BY created_at DESC
LIMIT 1;
-- Expected: 1 row, created_at within last 10 seconds
```

---

## 6. Negative / Edge Case Test Cases

### CONSENT-TC-018-NEG-001: Invalid consentId Format

| Field       | Value                                                    |
|-------------|----------------------------------------------------------|
| Test ID     | CONSENT-TC-018-NEG-001                                   |
| Title       | Non-numeric consentId path variable returns 400          |
| Priority    | P3                                                       |

**Input:**
- Path: `/api/v1/consent/grants/abc`
- Authorization: Bearer JWT

**Expected Outcome:**

| Check       | Expected Value                          |
|-------------|------------------------------------------|
| HTTP Status | `400 Bad Request`                        |
| Message     | Type conversion error or path var error  |

**Notes:**
- Spring MVC returns 400 for `MethodArgumentTypeMismatchException` when `abc` cannot be parsed as `Long`

---

### CONSENT-TC-018-NEG-002: Revoke Expired (but not revoked) Consent

| Field       | Value                                                         |
|-------------|---------------------------------------------------------------|
| Test ID     | CONSENT-TC-018-NEG-002                                        |
| Title       | Revoking an expired (but not yet revoked) consent succeeds    |
| Priority    | P2                                                            |

**Preconditions:**
- Consent grant with `expiry_at < NOW()` but `revoked_at IS NULL`
- Grant owned by TEST_USER_ID

**Expected Outcome:**

| Check          | Expected Value                            |
|----------------|-------------------------------------------|
| HTTP Status    | `200 OK`                                  |
| `data.revokedAt` | Non-null                               |

**Notes:**
- Business logic should allow revoking expired grants for audit completeness
- An expired grant is NOT the same as a revoked grant
- Verify that `isRevoked()` checks `revokedAt != null`, NOT `expiryAt < NOW()`

---

### CONSENT-TC-018-NEG-003: Double Revoke — State Not Overwritten

| Field       | Value                                                         |
|-------------|---------------------------------------------------------------|
| Test ID     | CONSENT-TC-018-NEG-003                                        |
| Title       | Second revoke attempt does not overwrite original revokedAt   |
| Priority    | P2                                                            |

**Preconditions:**
- Consent 1001 was revoked at T1 (revokedAt = T1)
- Another DELETE called at T2 (T2 > T1)

**Expected Outcome:**
- Second call returns 400 CONSENT-012
- DB `revoked_at` remains T1 (NOT updated to T2)

**Verification SQL:**
```sql
SELECT revoked_at FROM consent_grants WHERE id = 1001;
-- Expected: = T1 (original revoke time, NOT T2)
```

---

## 7. Performance Test Cases

### CONSENT-TC-018-PERF-001: P95 Latency Under Load

| Field          | Value                              |
|----------------|------------------------------------|
| Test ID        | CONSENT-TC-018-PERF-001            |
| Title          | P95 latency < 200ms under 50 RPS   |
| Priority       | P3 (NFR validation)                |
| Type           | Performance                        |

**Method:** JMeter / k6 load test
- Duration: 60 seconds
- Virtual Users: 10
- Target RPS: 50 (DELETE requests on pre-seeded consent IDs)

**Acceptance Criteria:**
- P95 response time < 200ms
- P99 response time < 500ms
- Error rate (excluding 404/400) < 0.1%

---

## 8. Test Summary & Traceability Matrix

### 8.1 Test Coverage Matrix

| Test Case ID             | BR-CONSENT-010 | BR-CONSENT-011 | BR-CONSENT-012 | NFR-SEC-001 | NFR-COMP-001 | NFR-PERF-001 |
|--------------------------|:--------------:|:--------------:|:--------------:|:-----------:|:------------:|:------------:|
| CONSENT-TC-018-001       | ✅              |                |                |             | ✅            |              |
| CONSENT-TC-018-002       | ✅              |                |                |             |              |              |
| CONSENT-TC-018-003       |                | ✅              | ✅              |             |              |              |
| CONSENT-TC-018-004       | ✅              | ✅              |                |             |              |              |
| CONSENT-TC-018-005       |                |                |                | ✅           |              |              |
| CONSENT-TC-018-INT-001   | ✅              |                |                |             | ✅            |              |
| CONSENT-TC-018-INT-002   |                |                |                |             | ✅            |              |
| CONSENT-TC-018-INT-003   |                |                | ✅              |             | ✅            |              |
| CONSENT-TC-018-NEG-001   |                |                |                |             |              |              |
| CONSENT-TC-018-NEG-002   |                |                |                |             | ✅            |              |
| CONSENT-TC-018-NEG-003   |                |                | ✅              |             | ✅            |              |
| CONSENT-TC-018-PERF-001  |                |                |                |             |              | ✅            |

### 8.2 CASE 2.0 Constraint Coverage

| CASE Constraint | Test Case(s) Covering It                              |
|-----------------|-------------------------------------------------------|
| C1 — Ownership  | CONSENT-TC-018-002, CONSENT-TC-018-004                |
| C2 — revokedAt + revokedBy from JWT | CONSENT-TC-018-INT-001               |
| C3 — Audit emit | CONSENT-TC-018-INT-003                                |
| C4 — 400 not 200 for double revoke | CONSENT-TC-018-003, NEG-003          |
| C5 — Row not deleted | CONSENT-TC-018-INT-001                           |
| C6 — List includes revoked | CONSENT-TC-018-INT-002                     |

### 8.3 Defect Severity Classification

| Severity | Definition                                                    | Examples for UC-18                          |
|----------|---------------------------------------------------------------|---------------------------------------------|
| P1       | Data corruption, security breach, compliance violation         | Hard delete, revokedBy from request body    |
| P2       | Functional failure blocking user flow                          | 200 returned for wrong owner revoke         |
| P3       | Minor UX / edge case handling                                  | Error message wording off                   |

### 8.4 Execution Summary Template

| Test Case ID             | Status | Date | Tester | Notes                                  |
|--------------------------|--------|------|--------|----------------------------------------|
| CONSENT-TC-018-001       | -      | -    | -      | Core happy path                        |
| CONSENT-TC-018-002       | -      | -    | -      | Verify same 404 as not-found           |
| CONSENT-TC-018-003       | -      | -    | -      | Verify 400 not 200/204                 |
| CONSENT-TC-018-004       | -      | -    | -      | -                                      |
| CONSENT-TC-018-005       | -      | -    | -      | -                                      |
| CONSENT-TC-018-INT-001   | -      | -    | -      | Testcontainer required; check version  |
| CONSENT-TC-018-INT-002   | -      | -    | -      | Verify list endpoint behavior          |
| CONSENT-TC-018-INT-003   | -      | -    | -      | Check audit table name in project      |
| CONSENT-TC-018-NEG-001   | -      | -    | -      | -                                      |
| CONSENT-TC-018-NEG-002   | -      | -    | -      | Confirm expired ≠ revoked              |
| CONSENT-TC-018-NEG-003   | -      | -    | -      | Verify revokedAt not overwritten       |
| CONSENT-TC-018-PERF-001  | -      | -    | -      | Optional NFR validation                |

---

*End of UC18_RevokeDataPermission_Test-Spec.md — Document ID: CB-CONSENT-IMP-018-TEST — Version 1.0*
