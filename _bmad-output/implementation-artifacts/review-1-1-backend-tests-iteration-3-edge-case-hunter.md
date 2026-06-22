# Edge Case Hunter Review Prompt — Iteration 3

Use `bmad-review-edge-case-hunter`. Do not use conversation history. Read the project only to trace the Story 1.1 diff from baseline `8bfcefae02b5a619085f3cb96bf7175c7e01171d`; do not modify or stage files.

Exhaustively check resend paths for a dual-identifier user, stable UUID cooldown keys, first/second atomic consumption, exactly-at-60-seconds behavior, missing user/pending OTP, stored destination selection, OTP/audit mutation after a rejected cross-channel request, null IDs, transaction rollback, concurrent callers, and test isolation.

Report only unhandled edge cases with severity, location, reproducible trigger, consequence, and minimal fix. Distinguish current-story regressions from pre-existing/distributed-architecture limitations. Return `No findings` if none.

# Output

## Reviewer

- Role: Edge-Case Hunter (independent reviewer, no conversation/spec context)
- Diff scope: same as the blind hunter — 16 tracked files in `04_SourceCode/Backend/src/main/java/com/carebridge/backend/{security,common/exception,audit}` plus the 8 untracked files in the same packages.
- Pre-build verification: `clean test` re-run from baseline `8bfcefa` → **56 tests, 0 failures, 0 errors, 0 skipped** (BUILD SUCCESS, JDK 21.0.10).

## Findings

### E1 — Boundary at exactly 60 s — verified correct, no defect

**File:** `RateLimitPolicy.java:204-220, 240-257, 265-279`

**Evidence:** `!now.isBefore(info.lastResendAt.plusSeconds(RESEND_COOLDOWN_SECONDS))` returns `true` when `now == lastResendAt + 60s` (because `now.isBefore(...)` is false). All three methods (`canResendOtp`, `tryConsumeResend`, `getTimeUntilResendReset`) use the same boundary predicate and are internally consistent. The integration assertion `matchesPattern("Please wait before resending OTP\\. Cooldown: (59|60) seconds")` in `RegistrationIntegrationTest.resendOtp_WithinCooldownPeriod_ShouldReturnTooManyRequests` line 552-554 covers the 59/60 wall-clock window.

**Verdict:** No defect. Keep this note for the audit trail.

### E2 — `findByUserIdAndUsedAtIsNull` lacks `ORDER BY`, may pick a stale row [Medium]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/OtpVerificationRepository.java:17`

**Evidence:** `Optional<OtpVerification> findByUserIdAndUsedAtIsNull(UUID userId)` — no `OrderByCreatedAtDesc` clause. Spring Data returns an arbitrary matching row. `resendOtp` (line 304-307) and `verifyOtp`'s email lookup both rely on this method to mean "the current pending OTP".

**Repro:** A user with two `OtpVerification` rows for the same `user_id` (e.g. one created by `register` left dangling, one created by `resendOtp` after the diff fix). H2/MySQL may return either. The build did not exercise this path because the happy-path tests always delete OTPs in `@AfterEach`.

**Impact:** `resendOtp` may pick an expired-but-still-unused old OTP, throw `ValidationException("No pending OTP...")`, and leave the legitimate fresh OTP untouched. Or `verifyOtp` may match a stale hash and reject a correct code.

**Minimal fix:** Rename to `findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId)`.

### E3 — Verify-attempt count is racy across concurrent threads [Medium — current-story regression]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:368-379, 409-419`

**Evidence:** `completeRegistration` and `completeLogin` both decrement `verification.setAttempts(verification.getAttempts() - 1)` and then call `otpVerificationRepository.save(...)`. No row-level lock, no `@Version`, no atomic UPDATE. Two threads verify-same-OTP at the same time and both read `attempts = 5`, both decrement to `4`, both save → lost update.

**Repro:** Two HTTP clients hit `POST /api/v1/auth/verify-otp` for the same `phone` within the same millisecond. Both pass the `findTopByPhoneAndUsedAtIsNull` filter (the row has `usedAt = null`, `verified = false`, both threads see it). Both run hash-compare. Even if one wins and commits `verified=true`, the other still mutates the row to a now-stale `attempts` value before the wins fails its lookup on the next round-trip.

**Impact:** Effective max attempts can exceed the configured 5. Security control bypass within a single JVM. Cross-JVM amplification also possible if multiple replicas run simultaneously.

**Minimal fix:** Use an atomic UPDATE — `UPDATE otp_verifications SET attempts = attempts - 1 WHERE id = ? AND attempts > 0` — and branch on the row count. Or add `@Version` to `OtpVerification` and let JPA optimistic locking reject the loser.

### E4 — `verifyOtp` does not consult `RateLimitPolicy` for attempt-window protection [Medium — current-story regression]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:247-284`

**Evidence:** The verify path relies solely on `OtpVerification.attempts` decremented inside `completeRegistration`/`completeLogin`. The `RateLimitPolicy.canAttempt(phone, otpHash)` overload and the `attemptMap` window are never invoked from `verifyOtp`. `OtpServiceImpl.verify` (line 57-76, pre-existing) also does not call `canAttempt`. The `RateLimitPolicy.canAttempt` paths are dead code in the verify flow.

**Impact:** The "5 attempts per 15 minutes" rule (which the policy exists to enforce) is not actually enforced through `RateLimitPolicy`. E3 above is the consequence: attempts are an unsynchronized counter rather than a rate-limit gate.

**Minimal fix:** Either wire `rateLimitPolicy.canAttempt(phone, inputOtpHash)` into `completeRegistration`/`completeLogin`, or fold the rate-limit policy into the atomic UPDATE in E3. Tracked as a `bad_spec` candidate unless the intent was always to skip the in-memory policy and rely on the DB counter.

### E5 — Cross-channel rejection does consume nothing — verified correct, no defect

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:289-303`

**Evidence:** The "exactly one identifier" check (`hasPhone == hasEmail`) runs before any DB lookup or rate-limit consume. The integration test `resendOtp_WithPhoneAndEmail_ShouldRejectWithoutSideEffects` (line 414-462) verifies: pending OTP unchanged, audit count unchanged, no email/SMS interactions. Same applies for the case where the identifier targets a non-existent user (`resendOtp_WithNonExistentUser_ShouldReturnNotFound` line 480-492) — `findByPhone`/`findByEmail` is the only side-effect and it returns `Optional.empty()` cleanly.

**Verdict:** No defect.

### E6 — Rollback after `tryConsumeResend` success leaves a 60 s cooldown with no OTP mutation [Medium — current-story regression]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:316-345`

**Evidence:** `tryConsumeResend(resendAccountKey)` is called on line 317 *before* the DB mutations (existing.setUsedAt, save existing, save new, deliver, audit). If any of those later steps throw (e.g. DB constraint violation, email service I/O error, audit failure), the `@Transactional` rollback discards the DB writes — but `RateLimitPolicy` is an in-memory `ConcurrentHashMap` and is **not** rolled back. The cooldown slot stays consumed.

**Repro:** Send a resend request, intercept between line 317 and line 341, force a `DataIntegrityViolationException` (e.g. by saving an `OtpVerification` with a duplicate primary key — synthetic but achievable in the test harness). The user sees 5xx; on retry they get 429 for up to 60 s even though no OTP was delivered.

**Impact:** Innocent users see spurious 429 after transient backend errors.

**Minimal fix:** Reorder so cooldown is consumed only after `save(newVerification)` succeeds. Or wrap the rate-limit call in a finally block that releases on exception. Or do the DB writes first and call `recordResendAttempt` after the commit boundary.

### E7 — `verifyOtp_WithValidOtp_ShouldActivateUserAndReturnTokens` is a placeholder test [Medium — current-story regression]

**File:** `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java:233-267`

**Evidence:** Method body comments explicitly say "Without intercepting the actual OTP sent, we cannot verify correct OTP / This test will be expanded with proper OTP capture in future". The test asserts only `assertThat(otpOpt).isPresent()` — it never posts to `/verify-otp` and never asserts the user becomes active or tokens are returned. **It does not exercise the happy path it claims to cover.**

**Repro:** Run the suite — the test passes because the assertions it makes are trivially true after `register`.

**Impact:** The canonical happy path (register → verify → activate → tokens) is unverified by automated tests. This is exactly why blind-hunter F2 (email-channel OTP not persisted) survived to iteration 3.

**Minimal fix:** Capture the OTP delivered via a `@Captor ArgumentCaptor<String>` on `emailService.sendOtpVerificationEmail` / `smsService.sendOtpVerificationSms`, then build the `VerifyOtpRequest` with that captured OTP and assert 200 + tokens + `user.isEnabled() == true`. Add a separate test for the email-channel happy path.

### E8 — Race between concurrent `verify-otp` and `resend-otp` for the same user [Low — current-story regression]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:247-366`

**Evidence:** Two transactions T1=verify and T2=resend can both observe `existing.usedAt = null` before either commits. T1 may complete registration against the OTP T2 is about to invalidate, and T2 may still write `auditService.log(OTP_RESENT, ...)` after the registration is already done. The user finishes registration, but the audit trail records a "resend" they never saw and never needed.

**Repro:** Submit `verify-otp` with the correct OTP and `resend-otp` simultaneously (within a few ms of each other).

**Impact:** Audit log inconsistency; harmless but pollutes downstream analytics.

**Minimal fix:** Either take a pessimistic lock on the OTP row (`SELECT ... FOR UPDATE`) at the start of both flows, or document the eventual-consistency caveat in the audit taxonomy.

### E9 — Email-channel login is unreachable [Medium — current-story regression]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:192-244`

**Evidence:** `login()` builds `OtpVerification` with `.phone(phone)` only — no `.email(...)`. `OtpServiceImpl.createAndSend` (line 36-53) accepts an `email` parameter but no caller passes one. The login API exposes no email path (the controller route requires phone). This is consistent with the existing phone-only login contract.

**Impact:** Documented behavior; not a bug. Flagged because the matching story / spec may have intended email login. If email-channel login is desired, the same fix as blind-hunter F2 applies (populate `.email(...)` on the OTP row). If not desired, close the gap by adding test coverage that asserts email login is rejected with a clear 400.

### E10 — `verifyOtp` for `LOGIN` purpose silently allows resending after lockout [Low]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:409-446`

**Evidence:** `completeLogin` decrements attempts the same way as `completeRegistration`. When `attempts <= 0`, it sets `usedAt`. `resendOtp` then refuses with "No pending OTP verification found" because the row is no longer pending. But the user can call `POST /api/v1/auth/login` again to receive a brand new OTP with `attempts = 5`, bypassing the lockout window for the old OTP.

**Repro:** Fail login verification 5 times, then call `/login` again — a fresh OTP with full attempt budget is issued.

**Impact:** Login attempt limiter is per-OTP, not per-account, so an attacker can sidestep the lockout by re-requesting login. This is also pre-existing but the diff actively relies on this behavior (it does not change `completeLogin`).

**Minimal fix:** Apply `attempts` decrements at the `User` level too, or block `/login` for `cooldown` seconds after `OtpVerification.attempts == 0`. Defer until product clarifies intent.

## Summary

| ID  | Severity   | File                                              | Scope                                          |
|-----|------------|---------------------------------------------------|------------------------------------------------|
| E1  | —          | RateLimitPolicy                                   | 60 s boundary: correct, no defect              |
| E2  | Medium     | OtpVerificationRepository                         | Stale-row risk, no ORDER BY                    |
| E3  | Medium     | AuthServiceImpl:368-419                           | Concurrent verify lost update                  |
| E4  | Medium     | AuthServiceImpl:247-284                           | verifyOtp bypasses RateLimitPolicy             |
| E5  | —          | AuthServiceImpl:289-303                           | Cross-channel rejection: correct               |
| E6  | Medium     | AuthServiceImpl:316-345                           | Cooldown consumed before DB write, no rollback |
| E7  | Medium     | RegistrationIntegrationTest:233-267               | Happy-path test is a placeholder               |
| E8  | Low        | AuthServiceImpl:247-366                           | verify-vs-resend audit inconsistency           |
| E9  | Medium     | AuthServiceImpl:192-244                           | Email-channel login unreachable                |
| E10 | Low        | AuthServiceImpl:409-446                           | Per-OTP lockout bypassed via re-login          |

**Highest-impact inside this stabilization's scope:** E3 (concurrent verify lost update), E4 (verifyOtp never calls RateLimitPolicy), E6 (cooldown leak on rollback), and the placeholder test E7 that masked the email-channel bug blind-hunter F2 surfaced. E2 is a latent risk that should be fixed in the same pass because it overlaps with E3 and E8.

**Out-of-scope pre-existing:** E9 (depends on intent for email login), E10 (login lockout policy), and the timing-oracle and design-smell items already flagged by the blind hunter.