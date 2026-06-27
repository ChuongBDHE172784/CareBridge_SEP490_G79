# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Đặc tả Kiểm thử Hướng Phát triển — UC-06 Reset Password

**Document ID:** `CB-AUTH-IMP-006-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Implemented`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `AI Agent`
**Reviewed by:** `[ ] [Tech Lead] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC06_ResetPassword/UC06_ResetPassword_TDS.md` (`CB-AUTH-IMP-006`) — Technical Specification
- `04_Implement/UC05_ForgotPassword/UC05_ForgotPassword_TDS.md` (`CB-AUTH-IMP-005`) — Upstream dependency
- `01_Requirements/SRS.md` — Functional requirements UC-06 §3.1.1.6
- `ADR-AUTH-021` đến `ADR-AUTH-027` — Architecture Decision Records

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                       |
| ---------- | --------------- | ------------------------------------------------------- |
| 2026-06-26 | AI Agent        | Khởi tạo tài liệu — TDD spec cho UC-06 ResetPassword   |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field                     | Value                                                    |
| ------------------------- | -------------------------------------------------------- |
| **Feature / Gap ID**     | `UC-06`                                                 |
| **Module**               | `ResetPassword — Bounded Context: auth`                 |
| **Spec gốc**             | `CB-AUTH-IMP-006`                                       |
| **Priority**             | 🔴 P0                                                   |
| **Sprint**               | `S[N] (2026-06-26 → 2026-07-11)`                       |
| **Milestone**            | `M3 Alpha — 2026-07-11`                                 |
| **Data Classification**  | `Sensitive-PII`                                         |
| **Compliance Scope**     | `BR-RBAC, BR-SECURITY, PDPA, GDPR Art. 32`              |
| **Upstream Dependencies** | `UC-05 ForgotPassword`, `PasswordComplexityPolicy`     |
| **Downstream Consumers** | `AuditService`, `SessionService`, `RefreshTokenRepository` |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                        |
| ------------------------ | -------------------------------------------------------------------------------------------- |
| **AI Assisted?**        | `Yes`                                                                                       |
| **Constraint Source**   | `CB-AUTH-IMP-006 §17`, `ADR-AUTH-021 đến ADR-AUTH-027`                                    |
| **Constraints Injected** | C1 (constant-time hash), C3 (session revoke), C4 (append-only token), C5 (rate limit 5/token) |
| **Model**               | `Claude Sonnet 4.6`                                                                         |
| **Trust Level**         | `T2 → T3 (pending Red Gate)`                                                               |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu)                                  | Thực tế (schema / policy)                                           | Fix áp dụng trong test                                           |
| - | ------------------------------------------------------- | ------------------------------------------------------------------- | ---------------------------------------------------------------- |
| L1 | SRS không mention attempt_count per token               | ADR-AUTH-026: 5 attempts/token, Redis hoặc DB counter              | Test phải verify rate limit bị enforce ở attempt thứ 6         |
| L2 | SRS không specify session invalidation scope           | ADR-AUTH-023: revoke ALL refresh tokens, không chỉ current         | Integration test assert `refresh_tokens` count=0 sau reset      |
| L3 | ChangePasswordRequest.java tồn tại nhưng không dùng cho UC-06 | ResetPassword dùng ResetPasswordRequest riêng (token + newPassword + confirmPassword) | Dùng đúng DTO, không re-use ChangePasswordRequest |
| L4 | Token comparison method chưa rõ                        | ADR-AUTH-021: dùng `MessageDigest.isEqual()` — constant-time       | Security test verify timing variance < 5ms                       |
| L5 | Không rõ transaction boundary                          | ADR-AUTH-023: updatePassword + revokeAll + consumeToken là atomic  | Unit test verify rollback khi updatePassword fail               |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
ResetPassword module bao gồm các layer:
├── Domain (pure logic — PasswordComplexityPolicy)
├── Service (ResetPasswordService — mock repositories)
├── Controller (ResetPasswordController — @WebMvcTest)
└── Integration (Testcontainers PostgreSQL + Redis)

Out of scope:
├── ForgotPasswordService internals (tested in UC-05 spec)
└── Email/SMS delivery (tested in UC-05)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source                       | Items Derived                                                                         |
| ---------------------------- | ------------------------------------------------------------------------------------- |
| `SRS UC-06 §3.1.1.6`        | Happy path: valid token + valid password → 200; Session invalidation after reset     |
| `ADR-AUTH-021`               | Constant-time SHA-256 hash compare (no timing leak)                                   |
| `ADR-AUTH-022`               | Delegate password complexity to `PasswordComplexityPolicy`                            |
| `ADR-AUTH-023`               | Revoke ALL refresh tokens for user after reset                                         |
| `ADR-AUTH-024`               | Append-only: mark token usedAt, never DELETE                                           |
| `ADR-AUTH-025`               | Token error → 400 AUTH-061; success → 200 generic                                    |
| `ADR-AUTH-026`               | Rate limit: 5 attempts/token → 429 AUTH-064; auto-invalidate token                    |
| `ADR-AUTH-027`               | Audit event PASSWORD_RESET_COMPLETED, no PII in payload                               |
| `BR-AUTH-022`                | confirmPassword must equal newPassword → AUTH-062                                      |
| `CB-AUTH-IMP-006 §10`       | Error codes AUTH-061 through AUTH-065                                                 |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID  | Test Condition                                           | Coverage Item                                              | Test Cases          |
| ------------- | -------------------------------------------------------- | ---------------------------------------------------------- | ------------------- |
| TC-COND-001   | Valid token + strong password + matching confirm        | `ResetPasswordService.resetPassword()` happy path          | `AUTH-TC-006-001`   |
| TC-COND-002   | Invalid / expired / used token                          | `ForgotPasswordService.validateToken()` exception handling | `AUTH-TC-006-002`   |
| TC-COND-003   | confirmPassword != newPassword                          | DTO validation / service check                             | `AUTH-TC-006-003`   |
| TC-COND-004   | Password fails complexity policy                        | `PasswordComplexityPolicy.validate()` exception            | `AUTH-TC-006-004`   |
| TC-COND-005   | attempt_count >= 5 (rate limit)                         | `RateLimitService.checkTokenAttemptLimit()` rejection      | `AUTH-TC-006-005`   |
| TC-COND-006   | DB update fails → full transaction rollback             | `@Transactional` rollback behavior                         | `AUTH-TC-006-006`   |
| TC-COND-007   | Token consumed after successful reset (single-use)      | `ForgotPasswordService.consumeToken()` + DB state          | `AUTH-TC-006-007`   |
| TC-COND-008   | All refresh tokens revoked after reset                  | `RefreshTokenRepository.revokeAllByUserId()`               | `AUTH-TC-006-008`   |
| TC-COND-009   | Timing attack on token hash compare                     | Constant-time compare (timing analysis)                    | `AUTH-TC-006-009`   |
| TC-COND-010   | No PII in audit event payload                           | `PasswordResetCompletedEvent` payload inspection           | `AUTH-TC-006-010`   |
| TC-COND-011   | Second reset attempt with same token fails              | Single-use token enforcement (integration)                 | `AUTH-TC-006-011`   |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4)   | Applied To                                       | Rationale                                                          |
| ------------------------- | ------------------------------------------------ | ------------------------------------------------------------------ |
| Equivalence Partitioning  | Token validity, password complexity               | Separate valid/invalid/expired/used partitions                     |
| Boundary Value Analysis   | Password length (8 chars min, 100 chars max)      | Edge: 7 chars (fail), 8 chars (pass), 100 chars (pass), 101 (fail)|
| State Transition Testing  | Reset token state machine (ISSUED→CONSUMED/EXPIRED/INVALIDATED) | Verify all state transitions per §6.3 |
| Error Guessing            | Token brute-force, SQL injection in token field   | OWASP A07:2021 — credential recovery vulnerabilities               |
| Decision Table Testing    | Rate limit × token validity × password complexity | All combinations of validation outcomes                            |

### TDS-05 — Test Data Requirements

| Fixture ID | Type     | Value / Logic                                                    | Mục đích                                    |
| ---------- | -------- | ---------------------------------------------------------------- | ------------------------------------------- |
| `FX-001`  | DB seed  | `User{ id=00000000-...-0001, status=ACTIVE, emailVerified=true }` | Happy path user                            |
| `FX-002`  | DB seed  | `PasswordResetToken{ tokenHash=SHA256("valid-test-token"), userId=FX-001.id, expiresAt=NOW+15min, usedAt=null, attemptCount=0 }` | Valid token |
| `FX-003`  | DB seed  | `PasswordResetToken{ tokenHash=SHA256("expired-token"), expiresAt=NOW-1min, usedAt=null }` | Expired token |
| `FX-004`  | DB seed  | `PasswordResetToken{ tokenHash=SHA256("used-token"), usedAt=NOW-5min }` | Already consumed token |
| `FX-005`  | DB seed  | `RefreshToken{ userId=FX-001.id, revokedAt=null }` × 3 rows     | Active sessions to be revoked               |
| `FX-006`  | Redis    | `rp_attempt:{SHA256("brute-token")} = 5`                        | Token at rate limit                         |
| `FX-007`  | Input    | `{ token: "valid-test-token", newPassword: "NewP@ss123!", confirmPassword: "NewP@ss123!" }` | Valid request |
| `FX-008`  | Input    | `{ token: "valid-test-token", newPassword: "weak", confirmPassword: "weak" }` | Complexity fail |
| `FX-009`  | Input    | `{ token: "valid-test-token", newPassword: "NewP@ss123!", confirmPassword: "Different!" }` | Mismatch |

---

## 4. Test Case Specification

> **TC ID format:** `AUTH-TC-006-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ResetPasswordTestFactory.java
// Đặt trong src/test/java/com/carebridge/backend/security/
class ResetPasswordTestFactory {

    // Baseline valid request — đồng bộ với FX-007 (§3 TDS-05)
    static ResetPasswordRequest makeValidRequest() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("valid-test-token");
        req.setNewPassword("NewP@ss123!");
        req.setConfirmPassword("NewP@ss123!");
        req.setIpAddress("127.0.0.1");
        req.setUserAgent("TestAgent/1.0");
        req.setCorrelationId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        req.setRequestId(UUID.randomUUID());
        return req;
    }

    // Overload để override specific fields
    static ResetPasswordRequest makeValidRequest(Consumer<ResetPasswordRequest> overrides) {
        ResetPasswordRequest req = makeValidRequest();
        overrides.accept(req);
        return req;
    }

    static User makeActiveUser() {
        User user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        return user;
    }

    static PasswordResetToken makeValidToken(UUID userId) {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        token.setUserId(userId);
        token.setTokenHash(DigestUtils.sha256Hex("valid-test-token"));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(14));
        token.setAttemptCount(0);
        return token;
    }
}
```

---

### AUTH-TC-006-001 — Happy path: valid token + strong password → reset success

**Severity:** `CRITICAL`
**Feature Under Test:** `ResetPasswordService.resetPassword()`
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordServiceTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-06 §3.1.1.6 / ADR-AUTH-021, ADR-AUTH-023, ADR-AUTH-024`

**Preconditions:**
- Fixture FX-001, FX-002, FX-005 ready
- `forgotPasswordService.validateToken("valid-test-token")` → returns `FX-001 User`
- `forgotPasswordService.consumeToken("valid-test-token")` → returns `true`
- `rateLimitService.checkTokenAttemptLimit(*)` → returns `true` (allowed)
- `passwordComplexityPolicy.validate("NewP@ss123!")` → passes (no exception)
- `refreshTokenRepository.revokeAllByUserId(user-001)` → returns `3`

**Test Steps:**
1. Arrange: setup mocks theo preconditions, tạo request via `ResetPasswordTestFactory.makeValidRequest()`
2. Act: `resetPasswordService.resetPassword(request)`
3. Assert:
   - Return value is `ResetPasswordResponse` with non-null, non-empty message
   - `userRepository.updatePasswordHash(user-001.id, anyString(), *)` called exactly once
   - `refreshTokenRepository.revokeAllByUserId(user-001.id, *)` called exactly once
   - `forgotPasswordService.consumeToken("valid-test-token")` called exactly once
   - `auditService.emit(argThat(e -> e instanceof PasswordResetCompletedEvent && ((PasswordResetCompletedEvent)e).payload().userId().equals(user-001.id)))` called once
   - No exception thrown

**Expected Result (PASS):**
- `ResetPasswordResponse.getMessage()` không null và chứa "password" (case-insensitive)
- Tất cả 4 side effects được gọi đúng thứ tự

**Expected Result (FAIL — dấu hiệu lỗi):**
- Exception được throw
- Một trong 4 side effects không được gọi

**Current Status:** 🔴 Not written
**Implementation Note:** Verify method call order (rate limit → validateToken → complexity → updateHash → revokeTokens → consumeToken → audit) bằng InOrder mock.

---

### AUTH-TC-006-002 — Invalid/expired token → AUTH-061

**Severity:** `CRITICAL`
**Feature Under Test:** `ResetPasswordService.resetPassword()` — error handling
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-AUTH-025 / CB-AUTH-IMP-006 §10`

**Preconditions:**
- `rateLimitService.checkTokenAttemptLimit(*)` → allowed
- `forgotPasswordService.validateToken("expired-uuid")` → throws `InvalidTokenException("AUTH-061")`

**Test Steps:**
1. Arrange: mock validateToken để throw `InvalidTokenException("AUTH-061")`
2. Act: `resetPasswordService.resetPassword(makeValidRequest(r -> r.setToken("expired-uuid")))`
3. Assert:
   - `InvalidTokenException` with errorCode `AUTH-061` is thrown
   - `userRepository.updatePasswordHash(*)` NOT called (verify(repo, never())...)
   - `refreshTokenRepository.revokeAllByUserId(*)` NOT called
   - `auditService.emit(argThat(e -> e instanceof PasswordResetAttemptFailedEvent))` called once

**Expected Result (PASS):** `InvalidTokenException` thrown, zero DB writes.

**Expected Result (FAIL):** No exception, or DB methods invoked despite invalid token.

**Current Status:** 🔴 Not written

---

### AUTH-TC-006-003 — Confirm password mismatch → AUTH-062

**Severity:** `HIGH`
**Feature Under Test:** `ResetPasswordService.resetPassword()` validation
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-AUTH-022 / CB-AUTH-IMP-006 §10`

**Preconditions:**
- `rateLimitService.checkTokenAttemptLimit(*)` → allowed

**Test Steps:**
1. Arrange: create request via `makeValidRequest(r -> r.setConfirmPassword("NotMatching!"))`
2. Act: `resetPasswordService.resetPassword(request)`
3. Assert:
   - `ValidationException` with errorCode `AUTH-062` is thrown
   - `forgotPasswordService.validateToken(*)` NOT called
   - `passwordComplexityPolicy.validate(*)` NOT called (fail fast before token lookup)

**Expected Result (PASS):** `ValidationException(AUTH-062)` before token validation.

**Expected Result (FAIL):** Token validation called despite mismatch.

**Current Status:** 🔴 Not written
**Implementation Note:** Mismatch check phải diễn ra TRƯỚC khi gọi validateToken() để tránh unnecessary token consumption attempt.

---

### AUTH-TC-006-004 — Password complexity fail → AUTH-063

**Severity:** `HIGH`
**Feature Under Test:** `PasswordComplexityPolicy.validate()` integration
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `ADR-AUTH-022 / BR-SECURITY`

**Preconditions:**
- `rateLimitService.checkTokenAttemptLimit(*)` → allowed
- `forgotPasswordService.validateToken(*)` → returns valid User
- `passwordComplexityPolicy.validate("weak")` → throws `ValidationException("AUTH-063")`

**Test Steps:**
1. Arrange: setup mocks; create request with `newPassword = "weak"`, `confirmPassword = "weak"`
2. Act: `resetPasswordService.resetPassword(request)`
3. Assert:
   - `ValidationException` with errorCode `AUTH-063` thrown
   - `userRepository.updatePasswordHash(*)` NOT called
   - `refreshTokenRepository.revokeAllByUserId(*)` NOT called

**Expected Result (PASS):** `ValidationException(AUTH-063)`, no DB writes.

**Expected Result (FAIL):** Password hash updated despite weak password.

**Current Status:** 🔴 Not written

---

### AUTH-TC-006-004B — Password boundary: exactly 8 chars → pass; 7 chars → fail

**Severity:** `MEDIUM`
**Feature Under Test:** `PasswordComplexityPolicy.validate()` boundary
**Test File:** `src/test/java/com/carebridge/backend/security/PasswordComplexityPolicyTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-SECURITY / ChangePasswordRequest @Size(min=8, max=100)`

**Test Steps:**
1. `passwordComplexityPolicy.validate("Short1!")` (7 chars) → expect ValidationException
2. `passwordComplexityPolicy.validate("Short1!X")` (8 chars, with uppercase + digit + special) → expect no exception
3. `passwordComplexityPolicy.validate("A".repeat(100) + "@1")` (102 chars) → expect ValidationException

**Current Status:** 🔴 Not written

---

### AUTH-TC-006-005 — Rate limit: 5 attempts/token → AUTH-064

**Severity:** `CRITICAL`
**Feature Under Test:** `RateLimitService.checkTokenAttemptLimit()` + token auto-invalidation
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `ADR-AUTH-026`

**Preconditions:**
- `rateLimitService.checkTokenAttemptLimit(tokenHash, 5)` → returns `false` (limit reached)

**Test Steps:**
1. Arrange: mock rateLimitService to reject; create valid request
2. Act: `resetPasswordService.resetPassword(request)`
3. Assert:
   - `RateLimitException` with errorCode `AUTH-064` thrown
   - `forgotPasswordService.validateToken(*)` NOT called
   - `userRepository.updatePasswordHash(*)` NOT called

**Expected Result (PASS):** `RateLimitException(AUTH-064)` before any token or DB operation.

**Expected Result (FAIL):** Service proceeds past rate limit check.

**Current Status:** 🔴 Not written

---

### AUTH-TC-006-006 — Atomicity: DB fail → full transaction rollback

**Severity:** `CRITICAL`
**Feature Under Test:** `@Transactional` rollback on `ResetPasswordService`
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-AUTH-023 / BR-AUTH-024`

**Preconditions:**
- `forgotPasswordService.validateToken(*)` → returns valid User
- `passwordComplexityPolicy.validate(*)` → passes
- `userRepository.updatePasswordHash(*)` → throws `DataAccessException`

**Test Steps:**
1. Arrange: mock updatePasswordHash to throw DataAccessException
2. Act: `resetPasswordService.resetPassword(validRequest)` → expect exception
3. Assert:
   - `refreshTokenRepository.revokeAllByUserId(*)` NOT called (rolled back)
   - `forgotPasswordService.consumeToken(*)` NOT called (rolled back)
   - `auditService.emit(PasswordResetCompletedEvent)` NOT called

**Expected Result (PASS):** RuntimeException thrown, no side effects committed.

**Expected Result (FAIL):** Partial state committed (sessions revoked but password not updated).

**Current Status:** 🔴 Not written
**Implementation Note:** Cần `@Transactional` trên service method. auditService.emit() PHẢI gọi sau transaction commit — dùng Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)` hoặc async emit.

---

### AUTH-TC-006-007 — Controller: valid request → 200 JSON

**Severity:** `HIGH`
**Feature Under Test:** `ResetPasswordController.resetPassword()` — request/response mapping
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `CB-AUTH-IMP-006 §9`

**Preconditions:**
- `@WebMvcTest(ResetPasswordController.class)` context
- `resetPasswordService.resetPassword(*)` mocked to return `ResetPasswordResponse("Your password has been reset...")`

**Test Steps:**
1. Arrange: MockMvc setup, mock service
2. Act:
   ```java
   mockMvc.perform(post("/api/v1/auth/reset-password")
     .contentType(MediaType.APPLICATION_JSON)
     .content("{\"token\":\"valid-uuid\",\"newPassword\":\"NewP@ss123!\",\"confirmPassword\":\"NewP@ss123!\"}")
     .header("X-Correlation-Id", UUID.randomUUID().toString()))
   ```
3. Assert:
   - HTTP status 200
   - Response body JSON contains `"message"` key
   - Content-Type: `application/json`

**Expected Result (PASS):** 200 with `{"message": "..."}`.

**Current Status:** 🔴 Not written

---

### AUTH-TC-006-008 — Controller: AUTH-061 → 400 JSON

**Severity:** `HIGH`
**Feature Under Test:** `ResetPasswordController` exception mapping
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-AUTH-IMP-006 §9, §10`

**Test Steps:**
1. Arrange: mock service to throw `InvalidTokenException("AUTH-061")`
2. Act: POST `/api/v1/auth/reset-password` with any body
3. Assert:
   - HTTP status 400
   - Response body: `{"error":{"code":"AUTH-061","message":"..."}}`

**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### AUTH-TC-006-009 — Timing attack: constant-time token hash compare

**Severity:** `CRITICAL`
**OWASP:** `A07:2021 — Identification and Authentication Failures`
**CWE:** `CWE-208 — Observable Timing Discrepancy`
**Legal:** `GDPR Art. 32 — appropriate technical security measures`
**Feature Under Test:** Token hash comparison in `ForgotPasswordService.validateToken()` / `IPasswordResetTokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter()`
**Test File:** `src/test/java/com/carebridge/backend/security/TokenTimingTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `ADR-AUTH-021`

**Preconditions:**
- Database with one valid token hash stored
- JMH or System.nanoTime() measurement setup

**Test Steps (Attack Simulation):**
1. Prepare: store valid tokenHash = SHA256("correct-token") in DB
2. Measure: 1000 lookups with SHA256("correct-token") (correct hash → match)
3. Measure: 1000 lookups with SHA256("xxxxx-wrong--") (wrong hash → no match)
4. Calculate: abs(avg(correct) - avg(wrong)) timing difference

**Expected Result (PASS = hệ thống an toàn):**
- Timing difference < 5ms (95th percentile)
- `findByTokenHash*` uses DB index lookup, not in-memory character-by-character compare

**Expected Result (FAIL = lỗ hổng tồn tại):**
- Timing difference > 10ms → timing oracle attack possible

**Current Status:** 🔴 Not written
**Implementation Note:** DB-level hash lookup via index is inherently constant-time (index scan). The `MessageDigest.isEqual()` in application level is for cases where secondary validation is needed after DB lookup. Ensure no early-return comparison in Java code.

---

### AUTH-TC-006-010 — No PII in audit event payload

**Severity:** `HIGH`
**OWASP:** `A09:2021 — Security Logging and Monitoring Failures`
**CWE:** `CWE-532 — Insertion of Sensitive Information into Log File`
**Legal:** `GDPR Art. 32 — security of processing`
**Feature Under Test:** `PasswordResetCompletedEvent` payload construction in `ResetPasswordService`
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-AUTH-027`

**Preconditions:**
- Mock setup for happy path
- Capture emitted audit event via ArgumentCaptor

**Test Steps:**
1. Arrange: happy path setup with ArgumentCaptor for auditService.emit()
2. Act: `resetPasswordService.resetPassword(validRequest)`
3. Assert captured event:

```java
ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
verify(auditService).emit(eventCaptor.capture());
PasswordResetCompletedEvent event = (PasswordResetCompletedEvent) eventCaptor.getValue();
PasswordResetCompletedEvent.Payload payload = event.payload();

// Must NOT contain
assertThat(payload).hasNoNullFieldsOrProperties(); // eventId, userId, etc. must be set
// Verify via reflection that no field name contains "token", "password", "hash", "email", "phone"
Field[] fields = PasswordResetCompletedEvent.Payload.class.getDeclaredFields();
for (Field f : fields) {
    assertThat(f.getName().toLowerCase())
        .doesNotContain("token", "password", "hash", "email", "phone");
}
```

**Expected Result (PASS = hệ thống an toàn):**
- Payload contains only: `userId`, `ipAddress`, `userAgent`, `requestId`, `sessionsRevoked`
- No sensitive fields

**Expected Result (FAIL):**
- Payload contains tokenHash, passwordHash, email, or phone

**Current Status:** 🔴 Not written

---

### AUTH-TC-006-011 — SQL injection in token field

**Severity:** `HIGH`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-89 — SQL Injection`
**Feature Under Test:** `IPasswordResetTokenRepository` query parameter handling
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordSecurityTest.java`
**TDD Phase:** 🔴 RED

**Test Steps (Attack Simulation):**
1. Arrange: integration test with PostgreSQL Testcontainer
2. Act: POST `/api/v1/auth/reset-password` with malicious token:
   ```json
   { "token": "' OR '1'='1", "newPassword": "NewP@ss123!", "confirmPassword": "NewP@ss123!" }
   ```
3. Assert:
   - HTTP 400 (invalid token — AUTH-061, not 500)
   - Database unchanged (no password updates)
   - No SQL error in logs

**Expected Result (PASS = hệ thống an toàn):**
- 400 AUTH-061 — parameterized query handles injection safely
- Spring Data JPA uses prepared statements by default

**Expected Result (FAIL):**
- 500 error (SQL exception) — indicates injection attempt reached DB
- Any unauthorized data modification

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### AUTH-TC-006-INT-001 — Full reset flow with real PostgreSQL

**Severity:** `HIGH`
**Feature Under Test:** Full flow: valid token → reset → DB state
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-007, TC-COND-008`

**Preconditions:**
- `@Testcontainers` PostgreSQL auto-start
- Flyway migration applied automatically when Spring context starts
- Seed: FX-001 (user), FX-002 (valid token), FX-005 (3 active refresh tokens)
- Redis container running (for rate limit service)

**Test Steps:**
1. POST `/api/v1/auth/reset-password` with FX-007 (valid request, token = "valid-test-token")
2. Assert HTTP 200
3. Query DB assertions:

```java
// Assert token consumed
PasswordResetToken token = tokenRepo.findByTokenHash(SHA256("valid-test-token")).orElseThrow();
assertThat(token.getUsedAt()).isNotNull();

// Assert password updated
User user = userRepo.findById(FX-001.id).orElseThrow();
assertThat(passwordEncoder.matches("NewP@ss123!", user.getPasswordHash())).isTrue();

// Assert all sessions revoked
long activeTokenCount = refreshTokenRepo.countByUserIdAndRevokedAtIsNull(FX-001.id);
assertThat(activeTokenCount).isZero();

// Assert audit event in event log
List<SecurityEventLog> events = eventLogRepo.findByEventType("PasswordResetCompleted");
assertThat(events).hasSize(1);
assertThat(events.get(0).getPayload().get("userId")).isEqualTo(FX-001.id.toString());
```

**Expected Result (PASS):**
- HTTP 200, token used_at set, password changed, all refresh tokens revoked, audit log entry present

**Expected Result (FAIL):**
- Any assertion fails — partial state committed

**Current Status:** 🔴 Not written

---

### AUTH-TC-006-INT-002 — Token single-use enforcement (integration)

**Severity:** `CRITICAL`
**Feature Under Test:** Single-use token: second use attempt after first success
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Test Steps:**
1. First POST `/api/v1/auth/reset-password` → 200 success
2. Second POST `/api/v1/auth/reset-password` with same token → assert 400 AUTH-061
3. Assert: password not changed a second time (still matches first reset's hash)

**DB Assertion:**
```java
PasswordResetToken token = tokenRepo.findByTokenHash(SHA256("valid-test-token")).orElseThrow();
assertThat(token.getUsedAt()).isNotNull(); // consumed after first use
// Second request must fail with AUTH-061
```

**Current Status:** 🔴 Not written

---

### AUTH-TC-006-INT-003 — Rate limit enforcement: 5 attempts → auto-invalidate

**Severity:** `HIGH`
**Feature Under Test:** Rate limit per token + auto-invalidation after 5 attempts
**Test File:** `src/test/java/com/carebridge/backend/security/ResetPasswordIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Seed: FX-002 (valid token, attemptCount=0)
2. Make 5 POST requests with wrong password (or invalid passwords → trigger increment)
3. 6th POST with CORRECT password → assert 429 AUTH-064
4. Assert: password NOT changed, token NOT consumed (or consumed due to auto-invalidate), audit log shows rate limit event

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID                   | Test File                                                               | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note            |
| ----------------------- | ----------------------------------------------------------------------- | --------------- | ----------------- | --------------------------- |
| `AUTH-TC-006-001`      | `ResetPasswordServiceTest.java`                                        | `[ ]`           | `[hash]`          | Extract password encoding   |
| `AUTH-TC-006-002`      | `ResetPasswordServiceTest.java`                                        | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-003`      | `ResetPasswordServiceTest.java`                                        | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-004`      | `ResetPasswordServiceTest.java`                                        | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-004B`     | `PasswordComplexityPolicyTest.java`                                    | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-005`      | `ResetPasswordServiceTest.java`                                        | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-006`      | `ResetPasswordServiceTest.java`                                        | `[ ]`           | `[hash]`          | Verify @Transactional       |
| `AUTH-TC-006-007`      | `ResetPasswordControllerTest.java`                                     | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-008`      | `ResetPasswordControllerTest.java`                                     | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-009`      | `TokenTimingTest.java`                                                 | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-010`      | `ResetPasswordServiceTest.java`                                        | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-011`      | `ResetPasswordSecurityTest.java`                                       | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-INT-001`  | `ResetPasswordIntegrationTest.java`                                    | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-INT-002`  | `ResetPasswordIntegrationTest.java`                                    | `[ ]`           | `[hash]`          | —                           |
| `AUTH-TC-006-INT-003`  | `ResetPasswordIntegrationTest.java`                                    | `[ ]`           | `[hash]`          | —                           |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// ResetPasswordService.java — Red Phase stub
@Service
public class ResetPasswordService implements IResetPasswordService {

    @Override
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID                  | Stub Result                     | Expected | Actual      | Root Cause (nếu PASS bất thường) |
| ---------------------- | ------------------------------- | -------- | ----------- | --------------------------------- |
| `AUTH-TC-006-001`     | `throw('Not implemented')`      | 🔴 FAIL  | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `AUTH-TC-006-002`     | `throw('Not implemented')`      | 🔴 FAIL  | ☐ FAIL ☐ PASS | |
| `AUTH-TC-006-003`     | `throw('Not implemented')`      | 🔴 FAIL  | ☐ FAIL ☐ PASS | |
| `AUTH-TC-006-004`     | `throw('Not implemented')`      | 🔴 FAIL  | ☐ FAIL ☐ PASS | |
| `AUTH-TC-006-005`     | `throw('Not implemented')`      | 🔴 FAIL  | ☐ FAIL ☐ PASS | |
| `AUTH-TC-006-006`     | `throw('Not implemented')`      | 🔴 FAIL  | ☐ FAIL ☐ PASS | |
| `AUTH-TC-006-INT-001` | `throw('Not implemented')`      | 🔴 FAIL  | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `target/surefire-reports/red-gate-evidence.log`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-AUTH-IMP-006` đã được review và approve
- [ ] UC-05 ForgotPassword đã deploy thành công (bao gồm `password_reset_tokens` table)
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Flyway migration `V{n}__add_attempt_count_to_password_reset_tokens.sql` đã approve và chạy staging
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho `ResetPasswordService`
- [ ] Không có business logic trong `ResetPasswordController`
- [ ] Không có PII/secret xuất hiện plaintext trong logs (verified via TC-006-010)
- [ ] Security tests AUTH-TC-006-009 (timing), AUTH-TC-006-011 (SQLi) đều PASS
- [ ] Integration test AUTH-TC-006-INT-002 (single-use) PASS

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests:
  ```bash
  grep -n "static.*ResetPasswordRequest\|static.*User " src/test/java/**/*Test.java
  # Mọi instance PHẢI nằm trong @Test hoặc dùng factory method
  ```
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria

- UC-05 ForgotPassword chưa deploy (missing `password_reset_tokens` table)
- `PasswordComplexityPolicy.validate()` API thay đổi breaking change
- CI pipeline broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert attempt_count migration
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE password_reset_tokens DROP COLUMN IF EXISTS attempt_count;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '{n}';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/security/service/ResetPasswordService.java
git checkout -- src/main/java/com/carebridge/backend/security/controller/ResetPasswordController.java
git checkout -- src/main/resources/db/migration/V{n}__add_attempt_count_to_password_reset_tokens.sql

# Revert test files
git checkout -- src/test/java/com/carebridge/backend/security/ResetPassword*Test.java

# UC-06 gap remains OPEN → update PHASE_GAP_ANALYSIS.md
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID     | Anti-Pattern            | Dấu hiệu trong TDD spec                                              | Check | Gate chặn |
| --------- | ----------------------- | -------------------------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào                            | ☐     | G-0       |
| AP-AI-002 | Green-from-Birth        | Test PASS với empty/throw stub (§5.1)                                | ☐     | G-2 ★    |
| AP-AI-003 | Implicit Decision       | Test assume architecture decision không có ADR (e.g., token format)  | ☐     | G-1       |
| AP-AI-004 | Layer Violation         | Test verify controller có business logic (hash, rate limit)           | ☐     | G-4       |
| AP-AI-005 | Hallucinated Contract   | Test import `InvalidTokenException` không tồn tại trong codebase    | ☐     | G-3       |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ----------- | ----- | ----- | ---------- | ------ |
| —           | —    | —    | —          | —     |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*UC-06 ResetPassword — CB-AUTH-IMP-006-TEST v1.0 — 2026-06-26*
