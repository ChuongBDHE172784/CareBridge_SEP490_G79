---
title: 'Story 1.3 Password-Based Login'
type: 'feature'
created: '2026-06-22'
status: 'draft'
baseline_commit: '8bfcefae02b5a619085f3cb96bf7175c7e01171d'
specLoopIteration: 1
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/1-3-user-login-with-credentials.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The current `AuthService.login(LoginRequest)` is OTP-only. `LoginRequest` carries a phone number, the service generates a 6-digit OTP and sends it via SMS, and the caller must still POST to `/verify-otp` to receive tokens. That contradicts the `1.3` acceptance criteria in `epics.md` (FR8, FR10, FR11), which require an email/phone + password login that issues access and refresh JWTs on a single request and persists a `LOGIN` audit row.

In addition, the existing rate-limit infrastructure (`RateLimitPolicy.canAttempt(phone)` — 5 attempts per 15 minutes) is not wired into the login path; `verifyOtp` relies on the per-OTP `OtpVerification.attempts` counter, which does not satisfy FR11's account-level lockout requirement.

**Approach:** Replace the OTP-only login with a password-based one. Extend `LoginRequest` to accept exactly one of `phone` or `email` (canonicalized via `trim().toLowerCase()` for email), plus `password`. Rewrite `AuthServiceImpl.login(...)` to look up the user, verify the BCrypt hash, enforce account status (`enabled=true`, `locked=false`), issue access + refresh JWTs, and write a `LOGIN` audit row. Wire `RateLimitPolicy.canAttempt(identifier)` so that 5 failed attempts within 15 minutes cause the next request to return HTTP 429 with an "Account temporarily locked" envelope. Map every failure path to the same `AuthenticationException("Invalid credentials")` envelope to avoid leaking whether an account exists.

## Boundaries & Constraints

**Always:** Preserve Spring Boot 4.1, Java 21, the public `POST /api/v1/auth/login` route, the existing RSA256 access-token (15 min) and refresh-token (7 day) contracts, OTP secrecy, safe error messages, and the Story 1.1 + 1.2 stabilization behavior on `/register`, `/verify-otp`, and `/resend-otp`. Reuse `JwtTokenProvider`, `RefreshTokenRepository`, `UserMapper`, `AuditService`, `PasswordEncoder`, and `RateLimitPolicy` rather than reimplementing them.

**Ask First:** Any change to the public API contract beyond `LoginRequest` and the new response envelope (e.g. token lifetime, refresh-token format), any database schema change, any change to `RateLimitPolicy` window or counter shape, or any change to `AuditAction` enum.

**Never:** Disable or skip tests, leak which identifier failed, log the raw password or its hash, fall back to OTP-on-fail when password login is rate-limited, or rewrite unrelated user changes in the working tree.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Login happy path — phone | `LoginRequest{phone="+84...", password=<correct>}` and account active | HTTP 200 with `AuthResponse{accessToken, refreshToken, user}` | Same envelope shape as `completeLogin` returns today |
| Login happy path — email | `LoginRequest{email="user@example.com", password=<correct>}` and account active | Same as above; lookup is `findByEmailIgnoreCase(trim().toLowerCase(email))` | Same envelope |
| Email with whitespace/uppercase | `LoginRequest{email="  User@Example.COM  ", password=<correct>}` | Treated identically to the canonical lowercased form | Same envelope as above |
| Wrong password | Any identifier, wrong password | HTTP 401 with `AuthenticationException("Invalid credentials")`; rate-limit counter increments | No hint about which field failed; no email-existence leak |
| Unknown identifier | Any identifier, user not found | HTTP 401 with the same `AuthenticationException("Invalid credentials")` message | Identical to wrong-password path |
| Both identifiers provided | `LoginRequest{phone=..., email=...}` | HTTP 400 validation error from `@AssertTrue` exactly-one rule | Reuse the `ResendOtpRequest` pattern |
| Neither identifier provided | Both null/blank | HTTP 400 validation error | Same |
| Disabled account | Account `enabled=false` | HTTP 403 with `AuthenticationException("Account is disabled")` | Distinct from wrong-password to match the AC's wording |
| Locked account | Account `locked=true` | HTTP 403 with `AuthenticationException("Account is locked")` | Distinct from disabled |
| Rate-limit exhaustion | 5 failed attempts within 15 min | HTTP 429 with `RateLimitExceededException("Account temporarily locked due to multiple failed attempts")` | No secret; no identifier leak |
| Rate-limit cooldown reached | Same user, 15+ minutes later | Login attempt proceeds as if no history had happened | Counter cleared by policy `cleanExpiredWindows` |
| Successful login after lock clears | Same user, after 15 min | Tokens issued, rate-limit counter reset, audit row | Same as happy path |

</frozen-after-approval>

## Code Map

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/LoginRequest.java` — extend with `email` and `password`; add `@AssertTrue` exactly-one rule.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java` — rewrite `login(...)`; add private helpers to verify password, enforce status, issue tokens.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java` — already exposes `canAttempt(phone)`, `getRemainingAttempts`, `getTimeUntilReset`; reuse as-is.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java` — review `ensureCanAuthenticate(user)` and ensure distinct error paths for disabled vs locked.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/entity/User.java` — `User.locked` already exists; check whether a `lockedUntil` column is needed for non-permanent lockout. If yes, add via a migration.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/UserRepository.java` — add `findByEmailIgnoreCase(String email)` if not present.
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/service/AuthServiceLoginTest.java` — new unit tests covering the scenarios in the matrix.
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java` — extend with a register → verify → login end-to-end path.

## Tasks & Acceptance

**Execution:**
- [ ] Extend `LoginRequest` with `email` and `password` fields, exactly-one validation, and a `setEmail` setter that trims (mirrors `VerifyOtpRequest`).
- [ ] Implement `findByEmailIgnoreCase` on `UserRepository` (Spring Data derived query).
- [ ] Rewrite `AuthServiceImpl.login(LoginRequest)` per the I/O matrix.
- [ ] Confirm `authenticationPolicy.ensureCanAuthenticate` returns distinct exceptions for disabled vs locked; extend if needed.
- [ ] Add a unit-test class `AuthServiceLoginTest` covering the matrix scenarios.
- [ ] Add a focused integration scenario in `RegistrationIntegrationTest` that registers a user, verifies the OTP, then logs in with the captured password and asserts token issuance.
- [ ] Run `mvnw.cmd clean test` and `mvnw.cmd clean package` from a clean state.

**Acceptance Criteria:**
- Given a fresh Backend `target`, when `mvnw clean test` runs, then all main and test sources compile and all tests pass without failures, errors, or skips introduced by this fix.
- Given a user registered with email and verified via OTP, when they call `POST /api/v1/auth/login` with `email` and the correct password, then the response carries a 15-minute access JWT and a 7-day refresh JWT plus the `UserProfileResponse`.
- Given a wrong password on any identifier, when the request hits `/api/v1/auth/login`, then the response is HTTP 401 with body `{error: "AUTHENTICATION_FAILED", message: "Invalid credentials"}` — identical to the body returned when the identifier does not exist.
- Given 5 failed attempts within 15 minutes on the same identifier, when the 6th attempt arrives, then the response is HTTP 429 with body `{error: "RATE_LIMIT_EXCEEDED", message: "Account temporarily locked due to multiple failed attempts"}`.
- Given the working tree contained pre-existing changes from Stories 1.1 and 1.2, when this fix is reviewed, then only `LoginRequest.java`, `AuthServiceImpl.java`, `UserRepository.java`, the new `AuthServiceLoginTest.java`, the extended `RegistrationIntegrationTest.java`, and BMAD artifacts required by this story have been changed.

## Spec Change Log

- **Iteration 1 — first draft.** The current `/login` path is OTP-based and does not satisfy FR8/FR10/FR11. Replace it with a password-based login that mirrors the rate-limit and audit invariants already in place for the resend and verify flows. **Known-bad state avoided:** a green build that still hands out OTPs over SMS for what should be a credential-based login, with no account-level rate limit and no distinct error envelopes for disabled vs locked accounts. **KEEP:** preserve the public route `POST /api/v1/auth/login`, the RSA256 JWT format, the 15-minute access / 7-day refresh lifetimes, the audit `LOGIN` action, the in-memory `RateLimitPolicy` shape, and the BCrypt strength-12 password hashing.

## Verification

**Commands:**
- `04_SourceCode/Backend/mvnw.cmd clean test` — expected: `BUILD SUCCESS` with zero failures and zero errors.
- `04_SourceCode/Backend/mvnw.cmd clean package` — expected: `BUILD SUCCESS` and a packaged JAR under `target`.

**Results:**
- _to be filled in after run_