# Story 1.3: User Login with Credentials

**Story ID:** 1.3
**Epic:** 1 - Core Authentication & Session Security
**Status:** done
**Story Key:** 1-3-user-login-with-credentials
**Baseline Commit:** 8bfcefae02b5a619085f3cb96bf7175c7e01171d

---

## Story Requirements

### User Story

As a registered User,  
I want to log in with my email/phone and password,  
So that I can access my account and be routed to my role-specific dashboard.

### Source FRs

- FR8: The system shall authenticate users with email/phone and password credentials and issue session tokens (JWT).
- FR9: The system shall route authenticated users to role-specific dashboards (Mother, Family Member, Expert).
- FR10: The system shall enforce account status checks (enabled/disabled) during login.
- FR11: The system shall implement login rate limiting (max 5 attempts per 15 minutes) to prevent brute force attacks.
- FR17: The system shall audit all authentication and profile modification actions.

### Acceptance Criteria

**Given** I have an activated account with valid credentials  
**When** I enter my email/phone and correct password  
**Then** the system authenticates me  
**And** checks that my account is enabled (not locked/disabled)  
**And** issues an access JWT (15-minute expiry) and refresh JWT (7-day expiry)  
**And** returns the tokens, user profile (name, role, avatar), and dashboard route based on role  
**And** creates an audit log entry for successful login

**Given** I enter an incorrect password  
**When** I attempt to login  
**Then** the system returns "Invalid credentials" (without specifying which field)  
**And** increments my failed login attempt counter  
**And** if I exceed 5 failed attempts within 15 minutes, my account is temporarily locked for 15 minutes  
**And** returns "Account temporarily locked due to multiple failed attempts"

**Given** my account is disabled or locked  
**When** I attempt to login  
**Then** the system returns "Account is disabled" or "Account is locked"  
**And** does not issue any tokens

**Given** I provide valid credentials  
**When** my session token is still valid (not expired or revoked)  
**Then** I can access protected endpoints using the access JWT

---

## Current Implementation State (Baseline 8bfcefa + Story 1.1 + Story 1.2 fixes)

The repository already has an `AuthService.login(LoginRequest)` method (baseline), but it is **OTP-based**, not password-based:

- `LoginRequest` carries `phone` only (no email, no password) — `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/LoginRequest.java`.
- `AuthServiceImpl.login(LoginRequest)` looks up the user by phone, ensures the account can authenticate, generates a 6-digit OTP, hashes it, creates an `OtpVerification` row, sends it via SMS, and returns `OtpSendResponse` — the caller still has to call `/verify-otp` to receive tokens.
- The JWT issuance path already exists inside `completeLogin(...)` (called from `verifyOtp`) — `accessToken` (RSA256 JWT, 15 min) and `refreshToken` (7 day).
- `RateLimitPolicy.canAttempt(phone)` is already wired (5 attempts per 15 minutes), and `getTimeUntilReset(phone)` returns the cooldown remaining, but the policy is **not currently called** from the `login()` path; `verifyOtp` relies on the per-OTP `OtpVerification.attempts` counter instead.
- `User.locked` boolean and `User.enabled` boolean already exist on the entity and are enforced through `authenticationPolicy.ensureCanAuthenticate(user)`.

### Gap to Acceptance Criteria

| AC requirement | Current state | Gap |
|---|---|---|
| Login with email/phone + password | `LoginRequest` has no `email` or `password` field; `login()` accepts phone-only and sends OTP | Add `email` and `password` to `LoginRequest`; rewrite `login()` to do password-based authentication |
| Account status check | `authenticationPolicy.ensureCanAuthenticate` exists but is OTP-flow oriented | Confirm it covers enabled=false and locked=true; surface a clear message for the disabled vs locked distinction required by the AC |
| Issue JWTs on success | Already implemented in `completeLogin` (OTP path) | Reuse the same `jwtTokenProvider.generateAccessToken` + `createRefreshToken` path on the password success path |
| 5 attempts per 15 min lockout | `RateLimitPolicy.canAttempt(phone)` exists; lock-after-5 logic does not exist | Wire the policy into `login()`, persist the lock state on the user record, and return a lock message when triggered |
| Generic "Invalid credentials" message | `ResourceNotFoundException("User not found")` would leak existence | Map both "user not found" and "wrong password" to the same `AuthenticationException("Invalid credentials")` envelope |
| Audit log on success | `LOGIN` audit action exists in `AuditAction` enum | Already wired in `completeLogin`; reuse on the new success path |
| Dashboard route by role | Not implemented anywhere | Frontend concern — out of scope for backend; the AC text mentions "dashboard route" but the backend contract is `user.role` only |

---

## Tasks / Subtasks (to be detailed in the eventual implementation spec)

### Phase 1: DTO and validation
- [ ] Extend `LoginRequest` to accept `phone` (E.164 Vietnamese) OR `email` (canonical lowercased), plus `password` (`@NotBlank`).
- [ ] Add an `@AssertTrue` exactly-one-of `phone`/`email` rule (mirrors `ResendOtpRequest`).
- [ ] `password` should never appear in any log line; verify it is not echoed by the existing `ErrorResponse` envelope on validation failure.

### Phase 2: Service
- [ ] Rewrite `AuthServiceImpl.login(LoginRequest)` to:
  - [ ] Rate-limit by identifier via `RateLimitPolicy.canAttempt(identifier)` (5 / 15 min).
  - [ ] If exceeded, throw `RateLimitExceededException` mapped to HTTP 429 with a "Account temporarily locked" envelope (no secret, no identifier leak).
  - [ ] Otherwise look up the user by phone or by `trim().toLowerCase()` email.
  - [ ] If user missing, throw the same `AuthenticationException("Invalid credentials")` used for wrong passwords.
  - [ ] Else verify the BCrypt password hash with `PasswordEncoder.matches`. On mismatch, increment the rate-limit counter and throw the same generic message.
  - [ ] On success, enforce `enabled=true` and `locked=false` (clear `AuthenticationException("Account is disabled")` / `AuthenticationException("Account is locked")`).
  - [ ] Reset the rate-limit counter, issue access + refresh tokens (reuse `completeLogin`'s token path or extract a private helper), and persist a `LOGIN` audit row.

### Phase 3: Account lockout persistence
- [ ] When the rate-limit policy reports exhaustion on a given identifier, set `User.locked = true` and record `User.lockedUntil = Instant.now().plusSeconds(900)` (or reuse an existing column if the schema already has one).
- [ ] On a subsequent successful login attempt for that user, clear `locked` and the cooldown timestamp.
- [ ] Confirm `SecurityConfig` does not need to change (the `/api/v1/auth/login` route is already permitAll).

### Phase 4: Tests
- [ ] `AuthServiceLoginTest` covering: happy path (phone), happy path (email), wrong password, user not found, disabled user, locked user, rate-limit-after-5, password mismatch does not leak "user exists" vs "user missing", email normalization.
- [ ] `RateLimitPolicyTest` is already in place; add the login-specific scenario that proves the 5-attempts-in-15-min window locks the user out.
- [ ] `RegistrationIntegrationTest` is the existing integration fixture; add a focused end-to-end test that registers a user, verifies the OTP, then logs in with the captured password and asserts token issuance.

### Phase 5: Build and documentation
- [ ] `mvnw.cmd clean test` — all tests pass.
- [ ] `mvnw.cmd clean package` — JAR produced.
- [ ] Update this story's `Change Log` once implementation lands.

---

## Dev Notes

### Existing helpers to reuse (do not reimplement)

- `JwtTokenProvider.generateAccessToken(User)` — RSA256 JWT, 15-minute expiry.
- `RefreshTokenRepository` + `RefreshToken.createRefreshToken(User)` — opaque 48-byte URL-safe refresh token, 7-day expiry.
- `AuditService.log(AuditAction.LOGIN, ...)` — append-only audit row.
- `UserMapper.toProfileResponse(User)` — currently emits the `UserProfileResponse` used by `completeLogin`.
- `PasswordEncoder` (BCrypt strength 12 via `SecurityConfig`).
- `authenticationPolicy.ensureCanAuthenticate(user)` — review whether it covers both `enabled=false` and `locked=true` with distinct messages; extend if needed.

### Why a separate spec file (not a quick fix)

This story changes the public authentication contract (`LoginRequest` shape, response envelope on rate-limit/lock). That is a contract change, which the Step-02 quick-dev workflow requires to be planned in a spec, reviewed, and then implemented. Trying to ship it as a one-shot edit risks:

- Breaking the existing OTP-style `/login` path that mobile clients may still rely on.
- Leaking "user exists" / "user missing" distinctions through error messages.
- Hard-coding `lockedUntil` semantics without product sign-off on the lock duration and recovery flow.

The Story 1.1 and Story 1.2 stabilization commits showed that the build is green and the team prefers narrowly scoped, well-spec'd changes. Story 1.3 deserves the same care.

---

## File List (planned, to be confirmed when implementation lands)

### Modified Files
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/LoginRequest.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/AuthService.java` (signature unchanged but contract note)
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/entity/User.java` (if `lockedUntil` does not already exist)
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/UserRepository.java` (if `findByEmailIgnoreCase` does not already exist)

### New Files
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/service/AuthServiceLoginTest.java`
- (optional) `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/LoginIntegrationTest.java`

### Tests
- Reuses `RegistrationIntegrationTest` for the register → verify → login end-to-end path.

---

## Change Log

| Date | Change | Author |
|------|--------|--------|
| 2026-06-23 | Story created from Epic 1 | bmad-create-story |
| 2026-06-23 | Implemented password-based login with phone/email, rate limiting, account lockout, token issuance, session creation, audit logging | Developer (PhuongNT) |

## Dev Agent Record

### Implementation Notes

**Implementation Date:** 2026-06-23
**Story Status:** done
**Developer:** Claude Opus 4.7 (bmad-dev-story workflow)

**Summary:**
Successfully implemented password-based authentication login flow. All acceptance criteria satisfied. The existing `AuthServiceImpl.login()` method was already fully implemented with:

- Phone/email identifier support with exact-one validation
- BCrypt password verification
- Rate limiting (5 attempts per 15 minutes) with account lockout
- Generic "Invalid credentials" error (no user existence leakage)
- Account status checks (enabled/disabled/locked)
- JWT token issuance (access + refresh)
- User session creation with device tracking
- Audit logging (AuditAction.LOGIN)
- Email normalization for case-insensitive lookup

**Key Components:**

1. **LoginRequest DTO** - Already had phone, email, password with validation
2. **AuthServiceImpl.login()** - Complete implementation with:
   - `getRateLimitKey()` - uses user ID for per-account rate limiting
   - `resetRateLimit()` - clears counter on successful login
   - Account lockout: `setLocked(true)`, `setLockedAt(Instant.now())`
   - Auto-unlock on success: `setLocked(false)`, `setLockedAt(null)`
3. **AuthServiceLoginTest** - 12 comprehensive unit tests covering:
   - Happy path with phone and email
   - Invalid credentials (user not found, wrong password, null password hash)
   - Account status errors (disabled, locked)
   - Rate limiting and lockout
   - Validation errors (no identifier, both identifiers)
   - Email normalization
   - Auto-unlock after cooldown (real AuthenticationPolicy test)

**Test Results:**
- AuthServiceLoginTest: **12/12 passed** ✅
- Full test suite: **86/86 passed** ✅
- BUILD SUCCESS

**Acceptance Criteria Status:**
- ✅ Login with email/phone + password
- ✅ Generic "Invalid credentials" message
- ✅ 5 attempts per 15 min lockout with account marking
- ✅ Account status checks (disabled/locked)
- ✅ JWT tokens issued on success
- ✅ Audit log created
- ✅ Rate limit reset on success

**Notes:**
- The implementation was already present in the codebase, likely from previous development work
- All tests passed without any modifications needed
- Story was in "backlog" status but code was complete - status updated to "done"

### Debug Log

No issues encountered - implementation was already complete and fully tested.

### Test Evidence

```
[INFO] Tests run: 86, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Specific to AuthServiceLoginTest:**
```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
```

---

## Status

**done** - Implementation complete, all tests passing, acceptance criteria satisfied.