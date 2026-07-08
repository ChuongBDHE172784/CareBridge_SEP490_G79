# UC17 — Grant Data Permission: Test Specification

| Field            | Value                              |
|------------------|------------------------------------|
| Document ID      | CB-CONSENT-IMP-017-TEST            |
| Version          | 1.0                                |
| Date             | 2026-06-26                         |
| Status           | Implemented         |
| Document Owner   | PhuongNT                           |
| Author           | AI Agent                           |
| Based on EDS     | v2.0                               |
| TDS Reference    | CB-CONSENT-IMP-017                 |
| SRS Reference    | 3.1.1.17                           |

---

## 1. Scope & Objectives

### 1.1 Feature Under Test

UC-17 **Grant Data Permission** — The authenticated user (ROLE_MOTHER or ROLE_EXPERT) grants data access to a recipient by calling `POST /api/v1/consent/grants`.

### 1.2 Test Objectives

1. Verify the happy path creates a `consent_grants` DB row with correct field values
2. Verify all business rule violations are rejected with correct HTTP status and error codes
3. Verify server-side computation of `expiryAt` (not client-controlled)
4. Verify unauthenticated requests return 401
5. Verify audit event `CONSENT_GRANTED` is emitted on success

### 1.3 Test Entry Criteria

- [ ] Application deployed or test container available
- [ ] `consent_grants` table created via Flyway migration
- [ ] JWT tokens available for ROLE_MOTHER and ROLE_EXPERT test users
- [ ] Test data (user IDs, seed consent records) prepared

### 1.4 Test Exit Criteria

- [ ] All test cases executed
- [ ] All `PASS` for happy path and negative path cases
- [ ] Integration test verifies DB state
- [ ] No P1/P2 defects open

---

## 2. Logic Issues & Design Notes

### L1: Self-Grant Check Implementation

**Issue**: The self-grant check (BR-CONSENT-001) compares `recipient` (a String, max 120 chars) against the authenticated `userId` (UUID). These are different types — `recipient` can store an email address, display name, or phone number, NOT a UUID.

**Impact**: The service must use context-appropriate comparison:
- If `recipient` is an email, compare against the user's email from profile lookup
- If `recipient` is a UUID string, parse and compare directly with `userId`
- If no strict self-grant detection is possible, document the accepted gap

**Test implication**: Test CONSENT-TC-017-002 should use the mechanism the system actually implements. If the system compares by email, the test must use the authenticated user's own email as recipient. Verify the actual check in `ConsentServiceImpl` before asserting.

---

### L2: expiryAt Is Server-Computed

**Issue**: `expiryAt` MUST be computed server-side as `consentGivenAt + expiryDays days`. The client does NOT supply `expiryAt` directly. This prevents clients from backdating or extending expiry arbitrarily.

**Test implication**: Integration test CONSENT-TC-017-INT-001 must verify that the `expiry_at` column value in DB equals `consent_given_at + interval 'N days'` (not some client-supplied timestamp).

---

### L3: expiryDays Optionality

**Issue**: `expiryDays` is marked `@Positive` but NOT `@NotNull` in `GrantConsentRequest`. If `expiryDays` is null, the service may set a default expiry or leave `expiryAt` null. The `consent_grants` schema does NOT have a NOT NULL constraint on `expiry_at` explicitly documented.

**Recommendation**: Service should reject null `expiryDays` (add `@NotNull`) or define a default (e.g., 90 days). Clarify this in implementation. Tests should cover both the null case and a valid value.

---

### L4: Duplicate Grant Detection Scope

**Issue**: CONSENT-003 (duplicate active grant) requires checking whether an active grant for the same `(userId, recipient, dataType)` already exists. "Active" means: `revokedAt IS NULL AND expiryAt > NOW()`.

**Test implication**: CONSENT-TC-017-003 equivalent for 409 should pre-insert an active grant and then attempt a second grant for the same triple.

---

## 3. Test Environment

### 3.1 Infrastructure Requirements

| Component      | Requirement                                          |
|----------------|------------------------------------------------------|
| Application    | Spring Boot test context (`@SpringBootTest`) or live |
| Database       | PostgreSQL with `consent_grants` table               |
| Auth           | JWT tokens for ROLE_MOTHER, ROLE_EXPERT test users   |
| Flyway         | Migrations applied before test run                   |

### 3.2 Test User Accounts

```
TEST_MOTHER_USER_ID  = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
TEST_MOTHER_EMAIL    = "test.mother@carebridge.test"
TEST_EXPERT_USER_ID  = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
TEST_EXPERT_EMAIL    = "test.expert@carebridge.test"
TEST_THIRD_USER_ID   = "cccccccc-cccc-cccc-cccc-cccccccccccc"
RECIPIENT_EMAIL      = "dr.recipient@hospital.test"
```

### 3.3 Test Data Factory

```java
class GrantConsentTestFactory {

    static final UUID TEST_USER_ID =
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final String TEST_USER_EMAIL = "test.mother@carebridge.test";
    static final String RECIPIENT_EMAIL = "dr.recipient@hospital.test";

    static GrantConsentRequest makeValidRequest() {
        return GrantConsentRequest.builder()
            .dataType(ConsentDataType.HEALTH_RECORD)
            .purpose(ConsentPurpose.VIEW)
            .recipient(RECIPIENT_EMAIL)
            .scope("Pregnancy monitoring records Q2 2026")
            .expiryDays(30)
            .build();
    }

    static GrantConsentRequest makeSelfGrantRequest(String selfIdentifier) {
        return GrantConsentRequest.builder()
            .dataType(ConsentDataType.HEALTH_RECORD)
            .purpose(ConsentPurpose.VIEW)
            .recipient(selfIdentifier)  // authenticated user's own email/id
            .expiryDays(30)
            .build();
    }

    static GrantConsentRequest makeInvalidExpiryRequest() {
        return GrantConsentRequest.builder()
            .dataType(ConsentDataType.HEALTH_RECORD)
            .purpose(ConsentPurpose.VIEW)
            .recipient(RECIPIENT_EMAIL)
            .expiryDays(0)  // invalid: must be @Positive
            .build();
    }
}
```

---

## 4. Unit Test Cases

### CONSENT-TC-017-001: Happy Path — Valid Consent Grant

| Field          | Value                                                    |
|----------------|----------------------------------------------------------|
| Test ID        | CONSENT-TC-017-001                                       |
| Title          | Grant valid consent returns 200 with persisted data      |
| Priority       | P1                                                       |
| Type           | Unit / Integration                                       |
| Endpoint       | POST /api/v1/consent/grants                              |

**Preconditions:**
- Authenticated as ROLE_MOTHER (TEST_MOTHER_USER_ID)
- No existing active grant for (TEST_MOTHER_USER_ID, RECIPIENT_EMAIL, HEALTH_RECORD)

**Input:**
```json
{
  "dataType": "HEALTH_RECORD",
  "purpose": "VIEW",
  "recipient": "dr.recipient@hospital.test",
  "scope": "Pregnancy monitoring records Q2 2026",
  "expiryDays": 30
}
```

**Expected Outcome:**

| Check              | Expected Value                                              |
|--------------------|-------------------------------------------------------------|
| HTTP Status        | `200 OK`                                                    |
| `success`          | `true`                                                      |
| `data.id`          | Non-null Long                                               |
| `data.userId`      | `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`                      |
| `data.dataType`    | `"HEALTH_RECORD"`                                           |
| `data.purpose`     | `"VIEW"`                                                    |
| `data.recipient`   | `"dr.recipient@hospital.test"`                              |
| `data.consentGivenAt` | Non-null, close to NOW()                               |
| `data.expiryAt`    | `consentGivenAt + 30 days`                                  |
| `data.revokedAt`   | `null`                                                      |

**Post-conditions:**
- Row exists in `consent_grants` table with matching fields
- `CONSENT_GRANTED` audit event emitted

**Test Code Sketch:**
```java
@Test
void grantConsent_validRequest_returns200() throws Exception {
    String jwt = jwtHelper.tokenForMother();
    mockMvc.perform(post("/api/v1/consent/grants")
            .header("Authorization", "Bearer " + jwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                GrantConsentTestFactory.makeValidRequest())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").isNumber())
        .andExpect(jsonPath("$.data.dataType").value("HEALTH_RECORD"))
        .andExpect(jsonPath("$.data.revokedAt").doesNotExist());
}
```

---

### CONSENT-TC-017-002: Self-Grant Rejected

| Field          | Value                                                    |
|----------------|----------------------------------------------------------|
| Test ID        | CONSENT-TC-017-002                                       |
| Title          | Grant consent to self returns 400 CONSENT-001            |
| Priority       | P1                                                       |
| Type           | Unit                                                     |
| Endpoint       | POST /api/v1/consent/grants                              |

**Preconditions:**
- Authenticated as TEST_MOTHER_USER_ID with email `test.mother@carebridge.test`
- System implements self-grant check via email or UUID comparison

**Input:**
```json
{
  "dataType": "HEALTH_RECORD",
  "purpose": "VIEW",
  "recipient": "test.mother@carebridge.test",
  "expiryDays": 30
}
```

**Expected Outcome:**

| Check          | Expected Value                                           |
|----------------|----------------------------------------------------------|
| HTTP Status    | `400 Bad Request`                                        |
| `success`      | `false`                                                  |
| `errorCode`    | `"CONSENT-001"`                                          |
| `message`      | Contains "yourself" or "self"                            |

**Post-conditions:**
- No new row in `consent_grants` table
- No audit event emitted

**Test Code Sketch:**
```java
@Test
void grantConsent_selfRecipient_returns400() throws Exception {
    String jwt = jwtHelper.tokenForMother();
    var request = GrantConsentTestFactory.makeSelfGrantRequest(TEST_MOTHER_EMAIL);
    mockMvc.perform(post("/api/v1/consent/grants")
            .header("Authorization", "Bearer " + jwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("CONSENT-001"));
}
```

---

### CONSENT-TC-017-003: Invalid expiryDays = 0

| Field          | Value                                                    |
|----------------|----------------------------------------------------------|
| Test ID        | CONSENT-TC-017-003                                       |
| Title          | expiryDays=0 returns 400 CONSENT-002                     |
| Priority       | P1                                                       |
| Type           | Unit                                                     |
| Endpoint       | POST /api/v1/consent/grants                              |

**Preconditions:**
- Authenticated as ROLE_MOTHER

**Input:**
```json
{
  "dataType": "HEALTH_RECORD",
  "purpose": "VIEW",
  "recipient": "dr.recipient@hospital.test",
  "expiryDays": 0
}
```

**Expected Outcome:**

| Check          | Expected Value                            |
|----------------|-------------------------------------------|
| HTTP Status    | `400 Bad Request`                         |
| `success`      | `false`                                   |
| `errorCode`    | `"CONSENT-002"` or `"VALIDATION_ERROR"`   |

**Notes:**
- `@Positive` on `expiryDays` will catch 0 at Bean Validation level
- Error code may be `CONSENT-002` if service-level check runs first, or `VALIDATION_ERROR` if `@Valid` triggers first
- Either is acceptable; document which path the implementation uses

**Test Code Sketch:**
```java
@Test
void grantConsent_zeroExpiryDays_returns400() throws Exception {
    String jwt = jwtHelper.tokenForMother();
    var request = GrantConsentTestFactory.makeInvalidExpiryRequest();
    mockMvc.perform(post("/api/v1/consent/grants")
            .header("Authorization", "Bearer " + jwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
}
```

---

### CONSENT-TC-017-004: Invalid dataType Enum Value

| Field          | Value                                                    |
|----------------|----------------------------------------------------------|
| Test ID        | CONSENT-TC-017-004                                       |
| Title          | Invalid dataType string returns 400 validation error     |
| Priority       | P2                                                       |
| Type           | Unit                                                     |
| Endpoint       | POST /api/v1/consent/grants                              |

**Preconditions:**
- Authenticated as ROLE_MOTHER

**Input:**
```json
{
  "dataType": "INVALID_TYPE",
  "purpose": "VIEW",
  "recipient": "dr.recipient@hospital.test",
  "expiryDays": 30
}
```

**Expected Outcome:**

| Check          | Expected Value                                                    |
|----------------|-------------------------------------------------------------------|
| HTTP Status    | `400 Bad Request`                                                 |
| `success`      | `false`                                                           |
| `message`      | Mentions valid enum values or "dataType" field                    |

**Notes:**
- Jackson deserialization fails with `HttpMessageNotReadableException` for unknown enum values
- Or `@NotNull` + custom deserializer returns validation error
- No row created in DB

---

### CONSENT-TC-017-005: No JWT Token

| Field          | Value                                                    |
|----------------|----------------------------------------------------------|
| Test ID        | CONSENT-TC-017-005                                       |
| Title          | Request without JWT returns 401 Unauthorized             |
| Priority       | P1                                                       |
| Type           | Unit                                                     |
| Endpoint       | POST /api/v1/consent/grants                              |

**Preconditions:**
- No Authorization header

**Input:**
```json
{
  "dataType": "HEALTH_RECORD",
  "purpose": "VIEW",
  "expiryDays": 30
}
```

**Expected Outcome:**

| Check          | Expected Value           |
|----------------|--------------------------|
| HTTP Status    | `401 Unauthorized`       |
| `errorCode`    | `"IAM-001"` (if mapped)  |

**Test Code Sketch:**
```java
@Test
void grantConsent_noJwt_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/consent/grants")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"dataType\":\"HEALTH_RECORD\",\"purpose\":\"VIEW\",\"expiryDays\":30}"))
        .andExpect(status().isUnauthorized());
}
```

---

### CONSENT-TC-017-006: Missing Required Field (dataType null)

| Field          | Value                                                    |
|----------------|----------------------------------------------------------|
| Test ID        | CONSENT-TC-017-006                                       |
| Title          | Null dataType returns 400 validation error               |
| Priority       | P2                                                       |
| Type           | Unit                                                     |
| Endpoint       | POST /api/v1/consent/grants                              |

**Input:**
```json
{
  "purpose": "VIEW",
  "recipient": "dr.recipient@hospital.test",
  "expiryDays": 30
}
```

**Expected Outcome:**

| Check          | Expected Value                    |
|----------------|-----------------------------------|
| HTTP Status    | `400 Bad Request`                 |
| `success`      | `false`                           |
| `message`      | References "dataType is required" |

---

### CONSENT-TC-017-007: ROLE_EXPERT Can Grant Consent

| Field          | Value                                                    |
|----------------|----------------------------------------------------------|
| Test ID        | CONSENT-TC-017-007                                       |
| Title          | ROLE_EXPERT can grant consent successfully               |
| Priority       | P2                                                       |
| Type           | Unit                                                     |

**Input:** Same as CONSENT-TC-017-001 but using EXPERT JWT

**Expected Outcome:**

| Check       | Expected Value |
|-------------|----------------|
| HTTP Status | `200 OK`       |
| `success`   | `true`         |

---

## 5. Integration Test Cases

### CONSENT-TC-017-INT-001: DB Row Created with Correct Fields

| Field          | Value                                                          |
|----------------|----------------------------------------------------------------|
| Test ID        | CONSENT-TC-017-INT-001                                         |
| Title          | Integration — verify DB row fields after grant                 |
| Priority       | P1                                                             |
| Type           | Integration                                                    |

**Preconditions:**
- `@SpringBootTest` with embedded PostgreSQL or testcontainer
- Flyway migrations applied

**Steps:**

1. Call `POST /api/v1/consent/grants` with valid request (expiryDays=30)
2. Capture `data.id` from response
3. Query DB directly for the record

**Verification SQL:**
```sql
SELECT
    id,
    user_id,
    data_type,
    purpose,
    recipient,
    scope_text,
    consent_given_at,
    expiry_at,
    revoked_at,
    revoked_by,
    version
FROM consent_grants
WHERE id = :returned_id;
```

**Expected DB State:**

| Column           | Expected Value                                   |
|------------------|--------------------------------------------------|
| user_id          | TEST_MOTHER_USER_ID                              |
| data_type        | `'HEALTH_RECORD'`                                |
| purpose          | `'VIEW'`                                         |
| recipient        | `'dr.recipient@hospital.test'`                   |
| scope_text       | `'Pregnancy monitoring records Q2 2026'`         |
| consent_given_at | NOT NULL, within last 5 seconds                  |
| expiry_at        | `consent_given_at + INTERVAL '30 days'`          |
| revoked_at       | NULL                                             |
| revoked_by       | NULL                                             |
| version          | 1                                                |

**Test Code Sketch:**
```java
@Test
@Transactional
void grantConsent_integration_dbRowCreatedCorrectly() throws Exception {
    String jwt = jwtHelper.tokenForMother();
    var result = mockMvc.perform(post("/api/v1/consent/grants")
            .header("Authorization", "Bearer " + jwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                GrantConsentTestFactory.makeValidRequest())))
        .andExpect(status().isOk())
        .andReturn();

    long id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");

    ConsentGrant saved = consentGrantRepository.findById(id).orElseThrow();

    assertAll(
        () -> assertEquals(TEST_MOTHER_USER_ID, saved.getUserId()),
        () -> assertEquals(ConsentDataType.HEALTH_RECORD, saved.getDataType()),
        () -> assertEquals(ConsentPurpose.VIEW, saved.getPurpose()),
        () -> assertEquals("dr.recipient@hospital.test", saved.getRecipient()),
        () -> assertNull(saved.getRevokedAt()),
        () -> assertNull(saved.getRevokedBy()),
        () -> assertEquals(1, saved.getVersion()),
        () -> {
            Duration diff = Duration.between(saved.getConsentGivenAt(), saved.getExpiryAt());
            assertEquals(30, diff.toDays(), "expiryAt should be exactly 30 days after consentGivenAt");
        }
    );
}
```

---

### CONSENT-TC-017-INT-002: Audit Event Emitted on Success

| Field          | Value                                              |
|----------------|----------------------------------------------------|
| Test ID        | CONSENT-TC-017-INT-002                             |
| Title          | Integration — CONSENT_GRANTED audit event emitted  |
| Priority       | P2                                                 |
| Type           | Integration                                        |

**Steps:**
1. Call `POST /api/v1/consent/grants` (valid request)
2. Verify audit log entry exists

**Verification:**
```sql
SELECT action, actor_id, resource_type, resource_id
FROM audit_logs
WHERE action = 'CONSENT_GRANTED'
  AND actor_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
ORDER BY created_at DESC
LIMIT 1;
```

**Expected:** Row exists with matching `actor_id` and `resource_id = <consentId>`

---

## 6. Negative / Edge Case Test Cases

### CONSENT-TC-017-NEG-001: Consent Scope Too Long

| Field       | Value                                                 |
|-------------|-------------------------------------------------------|
| Test ID     | CONSENT-TC-017-NEG-001                                |
| Title       | scope exceeding 1000 characters returns 400           |
| Priority    | P3                                                    |

**Input:** `scope` field = 1001 character string

**Expected:**

| Check       | Expected Value                                        |
|-------------|-------------------------------------------------------|
| HTTP Status | `400 Bad Request`                                     |
| `success`   | `false`                                               |
| `message`   | References "scope" and "1000"                         |

---

### CONSENT-TC-017-NEG-002: recipient Too Long

| Field       | Value                                                 |
|-------------|-------------------------------------------------------|
| Test ID     | CONSENT-TC-017-NEG-002                                |
| Title       | recipient exceeding 120 characters returns 400        |
| Priority    | P3                                                    |

**Input:** `recipient` = 121 character string

**Expected:** `400 Bad Request`, `@Size(max=120)` violation

---

### CONSENT-TC-017-NEG-003: Duplicate Active Grant Returns 409

| Field       | Value                                                       |
|-------------|-------------------------------------------------------------|
| Test ID     | CONSENT-TC-017-NEG-003                                      |
| Title       | Second grant for same user+recipient+dataType returns 409   |
| Priority    | P2                                                          |

**Preconditions:** Active grant exists for (TEST_MOTHER_USER_ID, RECIPIENT_EMAIL, HEALTH_RECORD)

**Expected:**

| Check       | Expected Value    |
|-------------|-------------------|
| HTTP Status | `409 Conflict`    |
| `errorCode` | `"CONSENT-003"`   |

---

## 7. Performance Test Cases

### CONSENT-TC-017-PERF-001: P95 Latency Under Load

| Field          | Value                              |
|----------------|------------------------------------|
| Test ID        | CONSENT-TC-017-PERF-001            |
| Title          | P95 latency < 200ms under 50 RPS   |
| Priority       | P3 (NFR validation)                |
| Type           | Performance                        |

**Method:** JMeter / k6 load test
- Duration: 60 seconds
- Virtual Users: 10
- Target RPS: 50

**Acceptance Criteria:**
- P95 response time < 200ms
- P99 response time < 500ms
- Error rate < 0.1%

---

## 8. Test Summary & Traceability Matrix

### 8.1 Test Coverage Matrix

| Test Case ID            | BR-CONSENT-001 | BR-CONSENT-002 | BR-CONSENT-003 | NFR-SEC-001 | NFR-PERF-001 |
|-------------------------|:--------------:|:--------------:|:--------------:|:-----------:|:------------:|
| CONSENT-TC-017-001      |                |                |                |             |              |
| CONSENT-TC-017-002      | ✅              |                |                |             |              |
| CONSENT-TC-017-003      |                | ✅              |                |             |              |
| CONSENT-TC-017-004      |                |                |                |             |              |
| CONSENT-TC-017-005      |                |                |                | ✅           |              |
| CONSENT-TC-017-006      |                |                |                |             |              |
| CONSENT-TC-017-INT-001  | ✅              | ✅              |                |             |              |
| CONSENT-TC-017-INT-002  |                |                | ✅              |             |              |
| CONSENT-TC-017-NEG-003  |                |                | ✅              |             |              |
| CONSENT-TC-017-PERF-001 |                |                |                |             | ✅            |

### 8.2 Defect Severity Classification

| Severity | Definition                                              | Examples                              |
|----------|---------------------------------------------------------|---------------------------------------|
| P1       | Data corruption, security breach, compliance violation  | Self-grant succeeds, 401 not returned |
| P2       | Functional failure blocking user flow                   | 200 returned for invalid enum         |
| P3       | Minor UX issues, edge case handling                     | Error message wording off             |

### 8.3 Execution Summary Template

| Test Case ID            | Status | Date       | Tester  | Notes                  |
|-------------------------|--------|------------|---------|------------------------|
| CONSENT-TC-017-001      | -      | -          | -       | -                      |
| CONSENT-TC-017-002      | -      | -          | -       | Verify self-grant check|
| CONSENT-TC-017-003      | -      | -          | -       | Check error code source|
| CONSENT-TC-017-004      | -      | -          | -       | -                      |
| CONSENT-TC-017-005      | -      | -          | -       | -                      |
| CONSENT-TC-017-006      | -      | -          | -       | -                      |
| CONSENT-TC-017-007      | -      | -          | -       | -                      |
| CONSENT-TC-017-INT-001  | -      | -          | -       | Requires testcontainer |
| CONSENT-TC-017-INT-002  | -      | -          | -       | Check audit table name |
| CONSENT-TC-017-NEG-001  | -      | -          | -       | -                      |
| CONSENT-TC-017-NEG-002  | -      | -          | -       | -                      |
| CONSENT-TC-017-NEG-003  | -      | -          | -       | -                      |
| CONSENT-TC-017-PERF-001 | -      | -          | -       | Optional               |

---

*End of UC17_GrantDataPermission_Test-Spec.md — Document ID: CB-CONSENT-IMP-017-TEST — Version 1.0*
