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
| `AuthenticationPolicy.java` | Added auto-unlock check in `ensureCanAuthenticate()` |
| `AuthService.java` | Changed return type of `login()` from `OtpSendResponse` to `AuthResponse` |
| `AuthServiceImpl.java` | Complete rewrite of `login()` method (lines 194-268) |
| `AuthController.java` | Updated `/login` endpoint return type and OpenAPI docs |
| `V3__add_locked_at_to_users.sql` | New migration adding `locked` and `locked_at` columns |

### New Files

| File | Purpose |
|------|---------|
| `AuthServiceLoginTest.java` | 11 unit tests covering all login scenarios |
| `1-3-user-login-with-credentials.md` | This story file |

## Change Log

| Date | Change | Author |
|------|--------|--------|
| 2026-06-22 | Initial implementation - password-based login | Claude Opus 4.7 |
| 2026-06-22 | Fixed test mocking: switched to `doNothing()` for void methods, fixed email normalization mock | Claude Opus 4.7 |
| 2026-06-22 | All 72 tests passing including 11 new login tests | Claude Opus 4.7 |

## Test Evidence

```
[INFO] Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Test coverage includes:
- Valid login with phone
- Valid login with email
- Email case normalization
- Wrong password rejection
- User not found handling
- Disabled account rejection
- Locked account rejection
- Rate limiting lockout (5 attempts → 15 min lock)
- No identifier validation
- Both identifiers validation
- Null password hash handling

## Status

**Done** - All acceptance criteria satisfied, tests passing, build successful.
