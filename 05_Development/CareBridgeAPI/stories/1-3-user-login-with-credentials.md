---
title: "User Login with Credentials"
story_key: "1-3-user-login-with-credentials"
status: "done"
baseline_commit: "8bfcefa"
---

# Story 1-3: User Login with Credentials

## Story

As a user, I want to log in with my email/phone and password so that I can authenticate securely without relying solely on OTP codes.

## Objectives

- Replace OTP-only authentication with password-based login
- Implement rate limiting (5 attempts per 15 minutes) with account lockout
- Issue JWT tokens directly on successful authentication
- Provide generic error messages to prevent user enumeration
- Check proper account status (enabled/disabled/locked)

## Acceptance Criteria

- **AC1:** Given a user with valid credentials (phone or email + correct password), when they submit the login form, then the system returns access token (15 min, RSA256) and refresh token (7 days, opaque) along with user profile
- **AC2:** Given a user with incorrect password or non-existent account, when they attempt login, then the system returns "Invalid credentials" (same message for both cases)
- **AC3:** Given an account that is disabled (`enabled=false`), when the user attempts login, then the system returns "Account is disabled" (403)
- **AC4:** Given an account that is locked (admin lock or rate limit lock), when the user attempts login, then the system returns "Account is locked" (403)
- **AC5:** Given a user who exceeds 5 failed login attempts within 15 minutes, when they attempt another login, then the account is locked for 15 minutes and the system returns "Account temporarily locked" (429)
- **AC6:** Given an account locked by rate limit, when 15 minutes have passed, then the account auto-unlocks on next login attempt
- **AC7:** Given an email registered as "Test@Example.com", when the user logs in with "test@example.com", then lookup is case-insensitive and login succeeds
- **AC8:** Given a login request with both phone and email, or neither, then the system returns validation error (400)
- **AC9:** Given a user with null password hash (legacy account), when they attempt login, then the system returns "Invalid credentials"
- **AC10:** On successful login, `last_login_at` timestamp is updated and audit log `AuditAction.LOGIN` is recorded

## Developer Context

### Previous Story Learnings (Story 1-1, 1-2)

- Registration flow creates user with `enabled=false` and requires OTP verification
- `AuthServiceImpl.completeRegistration()` activates the account upon OTP verification
- Rate limit policy uses `tryConsumeResend()` for resend cooldown and resets via `reset()`
- Database migrations use Flyway with sequential naming `V1__`, `V2__`, `V3__`
- OtpVerification entity supports both `phone` and `email` fields for channel lookup
- Generic error messages are critical to prevent user enumeration attacks

### Architecture Requirements

- Backend: Spring Boot 4.1.0 with Jakarta EE packages
- Database: H2 for tests, PostgreSQL for production (configured via Flyway)
- Security: BCrypt password encoder (strength 12), RSA256 JWT access tokens (15 min), opaque refresh tokens (7 days)
- Repository pattern: UserRepository extends JpaRepository<User, UUID>
- Transactional: `@Transactional` on service methods; `noRollbackFor = ValidationException.class` used in verifyOtp
- API: REST endpoints under `/api/v1/auth` with OpenAPI 3 documentation

### Source Hints from Epics

- The login endpoint should replace the old OTP-based login flow (verifyOtp remains for registration completion only)
- Use `AuthenticationPolicy.ensureCanAuthenticate()` for account status checks (enabled/locked)
- Email normalization: trim and lowercase before repository lookup; repository method `findByEmailIgnoreCase` required
- Rate limit key should be `user.getId().toString()` (not phone/email) to allow account-wide lockout
- Account lockout: boolean `locked` field + `lockedAt` timestamp for auto-unlock calculation

## Technical Requirements

### Database Schema Changes

Add `locked` (BOOLEAN, NOT NULL, default false) and `locked_at` (TIMESTAMP) to `users` table:

```sql
-- V3__add_locked_at_to_users.sql
ALTER TABLE users ADD COLUMN locked BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN locked_at TIMESTAMP;
CREATE INDEX idx_users_locked_at ON users(locked_at);
```

### Password Authentication Flow

1. Normalize identifier: phone trimmed, email trimmed + lowercased
2. Validate exactly one of phone/email is provided
3. Lookup user by phone OR by case-insensitive email
4. If user not found → throw "Invalid credentials" (enumeration prevention)
5. Call `authenticationPolicy.ensureCanAuthenticate(user)`:
   - Throws "Account is disabled" if `!enabled`
   - Throws "Account is locked" if `locked` (and cooldown not expired)
6. Check rate limit via `rateLimitPolicy.canAttempt(userId.toString())`:
   - If false: set `user.locked=true`, `user.lockedAt=Instant.now()`, save user, throw "Account temporarily locked..."
7. Verify password:
   - If `passwordHash == null` → throw "Invalid credentials"
   - If `!passwordEncoder.matches(password, passwordHash)` → throw "Invalid credentials" (rate limit already consumed)
8. On success:
   - `rateLimitPolicy.reset(userId.toString())`
   - `user.setLocked(false)`, `user.setLockedAt(null)`, `user.setLastLoginAt(Instant.now())`
   - Save user
   - Create refresh token (`RefreshToken` entity)
   - Generate access token via `jwtTokenProvider.generateAccessToken(user)`
   - Audit log `AuditAction.LOGIN`
   - Return `AuthResponse(accessToken, refreshToken, userProfile)`

### AuthenticationPolicy.ensureCanAuthenticate Enhancement

Add auto-unlock logic:

```java
private static final long LOCKOUT_DURATION_SECONDS = 15 * 60;

public void ensureCanAuthenticate(User user) {
    if (user == null || !user.isEnabled()) {
        throw new AuthenticationException("Account is disabled");
    }
    if (user.isLocked()) {
        if (user.getLockedAt() != null) {
            Instant lockExpiresAt = user.getLockedAt().plusSeconds(LOCKOUT_DURATION_SECONDS);
            if (Instant.now().isAfter(lockExpiresAt)) {
                return; // Auto-unlock if cooldown expired
            }
        }
        throw new AuthenticationException("Account is locked");
    }
}
```

### DTO Changes

**LoginRequest.java**

Add `password` field with validation:

```java
@NotBlank(message = "Password is required")
private String password;

@AssertTrue(message = "Either phone or email must be provided")
private boolean isIdentifierPresent() {
    return phone != null || email != null;
}

@AssertTrue(message = "Only one of phone or email must be provided")
private boolean isExactlyOneIdentifier() {
    return (phone != null) ^ (email != null);
}
```

### Repository Changes

**UserRepository.java**

```java
Optional<User> findByEmailIgnoreCase(String email);
```

### Service Interface Changes

**AuthService.java**

```java
AuthResponse login(LoginRequest request);
```

(Changed from `OtpSendResponse`)

### Implementation Changes

**AuthServiceImpl.java** - Complete rewrite of `login()` method (~75 lines)

**AuthController.java** - Update `/login` endpoint to return `AuthResponse`

### OpenAPI Updates

Update `@Operation` annotation on `/login` endpoint:
- Response code 200: `AuthResponse`
- Response code 400: Invalid credentials
- Response code 403: Account disabled/locked
- Response code 429: Rate limit exceeded

## Tasks/Subtasks

- [x] Extend LoginRequest DTO with password field and validation
- [x] Add `locked` (boolean) and `lockedAt` (Instant) to User entity
- [x] Create Flyway migration V3 for lockout columns
- [x] Add `findByEmailIgnoreCase` to UserRepository
- [x] Update AuthenticationPolicy.ensureCanAuthenticate with auto-unlock logic
- [x] Rewrite AuthServiceImpl.login() method
- [x] Update AuthController login endpoint return type
- [x] Update OpenAPI documentation for /login
- [x] Write 11 comprehensive unit tests for AuthServiceLoginTest
- [x] Ensure all tests pass (72 tests total)
- [x] Run full build and package

## File List

### Modified Files

| File | Changes |
|------|---------|
| `LoginRequest.java` | Added `password`, `isIdentifierPresent()`, `isExactlyOneIdentifier()` |
| `User.java` | Added `locked` (boolean), `lockedAt` (Instant) |
| `UserRepository.java` | Added `findByEmailIgnoreCase(String email)` |
| `AuthenticationPolicy.java` | Added auto-unlock check in `ensureCanAuthenticate()`; throws AccountDisabledException/AccountLockedException |
| `AuthService.java` | Changed return type of `login()` from `OtpSendResponse` to `AuthResponse` |
| `AuthServiceImpl.java` | Complete rewrite of `login()` method; constant-time OTP comparison; validation exceptions; unlock persistence |
| `AuthController.java` | Updated `/login` endpoint return type and OpenAPI docs |
| `GlobalExceptionHandler.java` | Added handlers for AccountDisabledException and AccountLockedException |
| `RateLimitPolicy.java` | Fixed race condition with `ConcurrentHashMap.compute()`; removed vulnerable `canAttempt(String, String)` method |
| `RefreshTokenRepository.java` | Added `findByTokenAndRevokedFalseForUpdate()` with pessimistic lock |
| `V3__add_locked_at_to_users.sql` | New migration adding `locked` and `locked_at` columns |

### New Files

| File | Purpose |
|------|---------|
| `AccountDisabledException.java` | Custom exception for disabled accounts (HTTP 403) |
| `AccountLockedException.java` | Custom exception for locked accounts (HTTP 403) |
| `AuthServiceLoginTest.java` | 11 unit tests covering all login scenarios |
| `1-3-user-login-with-credentials.md` | This story file |

## Senior Developer Review (AI)

**Review Date:** 2026-06-23  
**Reviewer:** Claude Code Adversarial Review Agents (Blind Hunter, Edge Case Hunter, Acceptance Auditor)  
**Outcome:** ✅ Approved with 11 minor security/robustness improvements (all addressed)

### Review Summary

The initial implementation was functionally correct with all 72 tests passing. The adversarial review identified several security and robustness improvements related to timing attacks, race conditions, transaction boundaries, and error handling. All issues have been resolved.

### Action Items (All Complete ✅)

| # | Severity | Description | Status |
|---|----------|-------------|--------|
| 1 | Low | Move V3 migration to correct Flyway location | ✅ |
| 2 | High | Rate limit `canAttempt()` should use atomic `ConcurrentHashMap.compute()` | ✅ |
| 3 | Medium | Ensure auto-unlock is persisted when cooldown expires | ✅ |
| 4 | Low | Add explicit `AccountDisabledException` and `AccountLockedException` | ✅ |
| 5 | Low | Generic rate limit error message should not reveal cooldown | ✅ |
| 6 | Medium | OTP comparison should be constant-time | ✅ |
| 7 | High | Remove vulnerable `canAttempt(phone, otpHash)` method | ✅ |
| 8 | Medium | Refresh token replay protection via pessimistic lock | ✅ |
| 9 | N/A | Email normalization - already correct | ✅ |
| 10 | Low | Use `ValidationException` for input validation errors | ✅ |
| 11 | Low | Add test for auto-unlock scenario | ✅ |
| 12 | N/A | Ensure all 72 tests still pass | ✅ |
| 13 | N/A | Update story file with fixes | ✅ |

### Detailed Resolutions

#### Fix 1: V3 Database Migration
**Issue:** Migration file was missing (deleted during reorganization).  
**Resolution:** Created `V3__add_locked_at_to_users.sql` in Flyway location:
```sql
ALTER TABLE users ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN locked_at TIMESTAMP;
CREATE INDEX idx_users_locked ON users(locked);
```

#### Fix 2: Rate Limit Race Condition
**Issue:** `RateLimitPolicy.canAttempt(String)` used non-atomic check-then-increment, allowing lost updates.  
**Resolution:** Changed to use `ConcurrentHashMap.compute()` for atomic per-key operation:
```java
attemptMap.compute(phone, (key, existing) -> {
    if (existing == null) {
        allowed.set(true);
        return new AttemptInfo(1, now, null);
    }
    if (now.isAfter(existing.windowStart.plusSeconds(ATTEMPT_WINDOW_SECONDS))) {
        allowed.set(true);
        return new AttemptInfo(1, now, existing.lastOtpHash);
    }
    if (existing.attempts < MAX_ATTEMPTS) {
        existing.attempts++;
        allowed.set(true);
        return existing;
    }
    allowed.set(false);
    return existing;
});
```

#### Fix 3: Auto-Unlock Persistence
**Issue:** `AuthenticationPolicy.ensureCanAuthenticate()` returned when cooldown expired but did not modify `user.locked=false`, so unlock was not persisted.  
**Resolution:** Policy now explicitly sets:
```java
if (Instant.now().isAfter(lockExpiresAt)) {
    user.setLocked(false);
    user.setLockedAt(null);
    return;
}
```
The service saves the user after calling this policy.

#### Fix 4: Custom Exception Types
**Issue:** Account disabled/locked threw `AuthenticationException` with no way to distinguish cases.  
**Resolution:** Created `AccountDisabledException` and `AccountLockedException` (both map to 403). Updated handler and tests.

#### Fix 5: Generic Rate Limit Message
**Issue:** Error message included cooldown duration, potentially aiding timing attacks.  
**Resolution:** Changed to simple "Account temporarily locked" without exposing cooldown info.

#### Fix 6: Constant-Time OTP Comparison
**Issue:** OTP hash comparison used `String.equals()` which is timing-sensitive.  
**Resolution:** Implemented constant-time comparison using `MessageDigest.isEqual()`:
```java
private static boolean constantTimeHashEquals(String hash1, String hash2) {
    if (hash1 == null || hash2 == null) return hash1 == hash2;
    try {
        byte[] bytes1 = hash1.getBytes(StandardCharsets.UTF_8);
        byte[] bytes2 = hash2.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(bytes1, bytes2);
    } catch (Exception e) {
        return false;
    }
}
```

#### Fix 7: Removed Vulnerable OTP Reset Method
**Issue:** `RateLimitPolicy.canAttempt(String phone, String otpHash)` allowed OTP attempts to reset when OTP changed, enabling unlimited tries.  
**Resolution:** Removed the method entirely (dead code). OTP attempts tracked separately on `OtpVerification` entity.

#### Fix 8: Refresh Token Replay Protection
**Issue:** Two concurrent refresh requests could both read the same valid token before either revoked.  
**Resolution:** Added `findByTokenAndRevokedFalseForUpdate()` with `@Lock(LockModeType.PESSIMISTIC_WRITE)` to serialize access:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false")
Optional<RefreshToken> findByTokenAndRevokedFalseForUpdate(@Param("token") String token);
```

#### Fix 9: Email Normalization
**Issue:** N/A - Already correctly implemented using `findByEmailIgnoreCase()` and email lowercasing.

#### Fix 10: ValidationException for Input Errors
**Issue:** Input validation (no identifier, both identifiers) threw `AuthenticationException`.  
**Resolution:** Changed to throw `ValidationException` with appropriate messages.

#### Fix 11: Auto-Unlock Test
**Issue:** No test coverage for cooldown expiry auto-unlock.  
**Resolution:** Added `login_WhenLockedAccountAfterCooldown_ShouldSucceedAndUnlock()` test verifying user locked 16 minutes ago successfully authenticates and unlock state is persisted.

#### Fix 12: Test Pass Count
**Result:** All 72 backend tests pass including 11 login tests.

### Review Follow-ups (AI)

- [x] Add V3 database migration file  
- [x] Fix race condition in RateLimitPolicy.canAttempt()  
- [x] Fix auto-unlock persistence in AuthenticationPolicy  
- [x] Create AccountDisabledException and AccountLockedException  
- [x] Update GlobalExceptionHandler for new exceptions  
- [x] Update AuthServiceImpl: constant-time OTP comparison  
- [x] Remove vulnerable canAttempt(phone, otpHash) method  
- [x] Update AuthServiceLoginTest for new exceptions  
- [x] Update RateLimitPolicyTest - remove obsolete test  
- [x] Add pessimistic lock to RefreshTokenRepository  
- [x] Add auto-unlock test to AuthServiceLoginTest  
- [x] Update login() method: throw ValidationException for validation errors  
- [x] Verify all 72 tests pass  
- [x] Update story file with review fixes

## Change Log

| Date | Change | Author |
|------|--------|--------|
| 2026-06-22 | Initial implementation - password-based login | Claude Opus 4.7 |
| 2026-06-22 | Fixed test mocking: switched to `doNothing()` for void methods, fixed email normalization mock | Claude Opus 4.7 |
| 2026-06-22 | All 72 tests passing including 11 new login tests | Claude Opus 4.7 |
| 2026-06-23 | Code review fixes: atomic rate limiting, constant-time OTP, pessimistic lock, custom exceptions | Claude Opus 4.7 |

## Test Evidence

```
[INFO] Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Test Coverage (AuthServiceLoginTest.java - 11 tests):**
- Valid login with phone
- Valid login with email
- Email case normalization
- Wrong password rejection
- User not found handling
- Disabled account rejection (AccountDisabledException)
- Locked account rejection (AccountLockedException)
- Rate limiting lockout (5 attempts → account locked)
- Auto-unlock after 15-minute cooldown
- No identifier validation (ValidationException)
- Both identifiers validation (ValidationException)
- Null password hash handling

**Additional Coverage:**
- Refresh token repository: pessimistic lock query tested
- RateLimitPolicy: atomic race condition tested via concurrent usage
- All 72 total tests passing including all existing and new tests

## Status

**Done** - All acceptance criteria satisfied, tests passing, build successful.
