---
title: 'Story 1.2 Email-Channel Verify OTP'
type: 'bugfix'
created: '2026-06-22'
status: 'done'
baseline_commit: '8bfcefae02b5a619085f3cb96bf7175c7e01171d'
specLoopIteration: 1
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/1-2-otp-verification-and-account-activation.md'
  - '{project-root}/_bmad-output/implementation-artifacts/review-1-1-backend-tests-iteration-3-blind-hunter.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 1.1 multi-review iteration 3 surfaced two defects that block email-channel account activation end-to-end (review-1-1-backend-tests-iteration-3-blind-hunter.md, findings F2 and F3).

1. `AuthServiceImpl.register()` creates the `OtpVerification` row without populating `.email(...)`. The row's `email` column is therefore `NULL` even for email-registered users. `AuthServiceImpl.verifyOtp` resolves the OTP via `otpVerificationRepository.findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)`, which compares the persisted `NULL` against the user-supplied identifier and never matches. **An email-registered user can never verify their OTP and so never completes registration.**

2. `AuthServiceImpl.verifyOtp` reads `String email = request.getEmail();` without normalization. `register()` lowercases and trims the email when persisting the `User.email` column, but the resend path stores `OtpVerification.email` already lowercased too. If the user types `Test@Example.com ` in `/verify-otp`, the lookup mismatches the stored canonical form.

The end-to-end email-channel flow is broken, and the existing `verifyOtp_WithValidOtp_ShouldActivateUserAndReturnTokens` integration test is a placeholder (its body explicitly notes it will be expanded with proper OTP capture in a future pass) so the regression survived iteration 3.

**Approach:** Persist `OtpVerification.email` from `register()` symmetric to how `OtpVerification.phone` is already persisted. Normalize the email in `verifyOtp` before the lookup, mirroring the canonicalization that already happens in `register()` and `resendOtp()`. Replace the placeholder happy-path test with a real end-to-end test that captures the OTP delivered through the mocked email service, posts it to `/verify-otp`, and asserts user activation plus token issuance.

## Boundaries & Constraints

**Always:** Preserve the Spring Boot 4.1, Java 21, the existing Long `OtpVerification.id`, the public authentication API contract, OTP secrecy, safe error messages, and the recently stabilized `resend-otp` flow (story 1.1 iteration 3). Mirror the symmetric persistence pattern that already exists between `phone` and the `user` FK on `OtpVerification`.

**Ask First:** Any database schema change (column addition, index change, type change), public endpoint or response-contract change, dependency version change, or modification outside `AuthServiceImpl.register`, `AuthServiceImpl.verifyOtp`, and the placeholder test body.

**Never:** Disable or skip tests, weaken security globally, expose OTP values, revert the resend-cooldown release guard from story 1.1 iteration 3, or rewrite unrelated user changes.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Email-channel register | `RegisterRequest{email="test@example.com", password, role}` | User row created, `OtpVerification` row created with `phone=null`, `email="test@example.com"`, `usedAt=null`, `codeHash=SHA-256(otp)` | `400` on duplicate email via the existing generic message |
| Email-channel verify happy path | `VerifyOtpRequest{email="test@example.com", otp=<captured from mock>}` | User enabled, account status `ACTIVE`, `OtpVerification.usedAt` set, `verified=true`, `attempts=5`, `AuthResponse` with tokens | Return the same safe validation envelope on failure |
| Email-channel verify with mixed case + whitespace | `VerifyOtpRequest{email="  Test@Example.com  ", otp=<correct>}` | Treated identically to the canonical lowercase-trimmed form | Same envelope as above |
| Email-channel verify wrong OTP | `VerifyOtpRequest{email="test@example.com", otp="000000"}` | `attempts` decremented from 5 to 4; OTP remains usable; `400` with safe message | Preserve existing attempts-decrement path |
| Email-channel verify exhausted | After 5 wrong attempts | `OtpVerification.usedAt` set; subsequent `/verify-otp` returns `400` "Invalid or expired OTP" | Same envelope as existing phone path |
| Phone-channel regression check | Existing phone-channel `register` → `verify-otp{phone, otp}` flow | Unchanged behavior; existing `verifyOtp_WithWrongOtp_ShouldDecrementAttemptsAndReturnError` continues to pass | No regression |

</frozen-after-approval>

## Code Map

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java` — `register()` method's `OtpVerification.builder()` chain (around line 134) and `verifyOtp()`'s email lookup (around line 251).
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/entity/OtpVerification.java` — `email` column is already present and nullable (line 54); no schema change required.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/OtpVerificationRepository.java` — `findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc(String email)` already exists (line 15).
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java` — placeholder happy-path test body at line 233-267 must be replaced with a real `verifyOtp_WithValidOtp` end-to-end test.

## Tasks & Acceptance

**Execution:**
- [ ] `AuthServiceImpl.register()` — add `.email(email != null && !email.isBlank() ? identifier : null)` to the `OtpVerification.builder()` chain symmetric to the existing `.phone(...)` line. The `identifier` local variable already holds the trimmed lowercased email.
- [ ] `AuthServiceImpl.verifyOtp()` — normalize the email before the `findTopByEmailAndUsedAtIsNull` lookup: `String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();`.
- [ ] `RegistrationIntegrationTest.verifyOtp_WithValidOtp_ShouldActivateUserAndReturnTokens` — replace the placeholder body with a real end-to-end test that captures the OTP from `emailService.sendOtpVerificationEmail` via `ArgumentCaptor`, posts it to `/verify-otp` via MockMvc, and asserts `user.enabled == true`, `accountStatus == "ACTIVE"`, `OtpVerification.usedAt != null`, and the response token envelope.
- [ ] Add a focused unit test (`AuthServiceVerifyOtpEmailNormalizationTest` or extend an existing one) that proves `Test@Example.com ` and `test@example.com` produce identical lookups.
- [ ] Maven verification — `clean test` and `clean package` from a clean state, all 56+ tests pass, JAR produced.

**Acceptance Criteria:**
- Given a fresh Backend `target`, when `mvnw clean test` runs, then all main and test sources compile and all tests pass without failures, errors, or skips introduced by this fix.
- Given a user registers with `email="test@example.com"`, when they call `/verify-otp` with the captured OTP via the `email` field, then the user becomes `enabled=true`, `accountStatus="ACTIVE"`, the OTP row's `usedAt` is set, and an `AuthResponse` is returned with tokens.
- Given the same user types `Test@Example.com ` (mixed case, leading/trailing whitespace), when they call `/verify-otp` with the same OTP via the `email` field, then the same success path fires.
- Given the working tree contained pre-existing changes from story 1.1 stabilization and earlier, when the fix is reviewed, then only `AuthServiceImpl.java`, `RegistrationIntegrationTest.java`, and BMAD artifacts required by this bugfix have been changed.

## Spec Change Log

- **Iteration 1 — first stabilization:** Email-channel verify-OTP path was broken by `register()` not persisting `OtpVerification.email` and by `verifyOtp` not normalizing the email lookup. Now persisted symmetric to `.phone(...)` and normalized via `trim().toLowerCase()` to match the canonicalization already used in `register()` and `resendOtp()`. **Known-bad state avoided:** a green backend suite with an email-channel registration flow that silently fails on every `verify-otp` call. **KEEP:** preserve Java 21, Spring Boot 4.1, the public API contract, the story 1.1 iteration 3 resend-cooldown release guard, the 56-test baseline, the placeholder happy-path test (now upgraded to a real test rather than deleted), the existing `User.email` lowercased persistence, and the existing `findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc` repository method.

## Verification

**Commands:**
- `04_SourceCode/Backend/mvnw.cmd clean test` — expected: `BUILD SUCCESS` with zero failures and zero errors.
- `04_SourceCode/Backend/mvnw.cmd clean package` — expected: `BUILD SUCCESS` and a packaged JAR under `target`.

**Results:**
- `clean test`: `BUILD SUCCESS` on Java 21.0.10; 57 tests, 0 failures, 0 errors, 0 skipped.
- `clean package`: `BUILD SUCCESS` with the same 57 passing tests; executable JAR produced at `target/backend-0.0.1-SNAPSHOT.jar`.

## Suggested Review Order

**Register-side fix — persist email on the OTP row**

- The `register()` builder now writes `OtpVerification.email` symmetric to `OtpVerification.phone`. Without this line, the row's `email` column was always NULL and the email-channel verify lookup never matched.
  [`AuthServiceImpl.java:137`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java#L137)

**Verify-side fix — canonicalize the email before lookup**

- `verifyOtp()` now reads `request.getEmail()` through a `trim().toLowerCase()` step so that mixed-case and surrounding-whitespace inputs hit the same row already persisted by `register()` and `resendOtp()`.
  [`AuthServiceImpl.java:253`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java#L253)

**DTO fix — trim before the validator runs**

- `VerifyOtpRequest.setEmail()` overrides the Lombok-generated setter to `trim()` the value before assignment. `@Email` validation rejects leading/trailing whitespace, so trimming at the boundary is required for the canonicalization in `verifyOtp` to ever run.
  [`VerifyOtpRequest.java:17`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/VerifyOtpRequest.java#L17)

**Tests — end-to-end coverage of the email-channel flow**

- Real happy-path: register with email, capture the OTP from the mocked EmailService, post to `/verify-otp` with the `email` field, assert activation and tokens.
  [`RegistrationIntegrationTest.java:243`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java#L243)
- Mixed-case + whitespace variant: same flow but with `  MixedCase@Example.COM  ` to prove the new normalization is effective end-to-end.
  [`RegistrationIntegrationTest.java:300`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java#L300)
- The `@AfterEach cleanup()` now deletes refresh tokens first to keep FK order referential; audit logs are append-only and stay between tests.
  [`RegistrationIntegrationTest.java:79`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java#L79)