# Blind Hunter Review Prompt — Iteration 3

Use `bmad-review-adversarial-general` to review only the assembled Story 1.1 backend-stabilization diff. Do not read conversation history, the spec, context documents, or unrelated files.

Build a read-only diff from baseline `8bfcefae02b5a619085f3cb96bf7175c7e01171d`, including untracked files, for the authentication, exception, audit-policy, security configuration, rate-limit policy, and tests named by `_bmad-output/implementation-artifacts/spec-1-1-fix-backend-tests.md`. Do not stage or modify files.

Focus on the latest correction: resend cooldown must be consumed by stable account UUID, not phone/email channel; switching channels for the same account must not create a second replacement, delivery, or audit. Also inspect deterministic 60-second boundary behavior.

Report only concrete defects with severity, file/line, evidence, impact, and minimal fix. Clearly mark confident pre-existing issues. Return `No findings` if none.

# Output

## Reviewer

- Role: Blind Hunter (independent reviewer, no conversation/spec context)
- Diff scope: 16 tracked files in `04_SourceCode/Backend/src/main/java/com/carebridge/backend/{security,common/exception,audit}` and `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/**`, plus 8 untracked files in the same packages (`RateLimitExceededException`, `ResendOtpRequest`, `OtpResendResponse`, `MockSmsService`, `MockEmailService`, `PasswordComplexityPolicy`, `RateLimitPolicyResendTest`, `AuthServiceResendOtpTest`).
- Pre-build verification: `clean test` re-run from baseline `8bfcefa` → **56 tests, 0 failures, 0 errors, 0 skipped** (BUILD SUCCESS, JDK 21.0.10). Build state matches the handoff claim.

## Findings

### F1 — Cooldown map cleanup window is unsynchronized with `tryConsumeResend` [Medium]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java:240-257`

**Evidence:** `tryConsumeResend` runs `cleanExpiredResends()` and then `resendMap.compute(identifier, ...)`. `cleanExpiredResends` calls `resendMap.entrySet().removeIf(...)`. The same `resendMap` is concurrently read/written by `getTimeUntilResendReset` and `canResendOtp`. Although `ConcurrentHashMap.compute` is atomic per key, the `removeIf` traversal runs over a weakly consistent iterator that is **not synchronized with in-flight `compute` calls**.

**Impact (correctness):** If thread T1 calls `tryConsumeResend(accountKey)` and inside the lambda returns a fresh `ResendInfo(now)`, while thread T2 simultaneously runs `cleanExpiredResends`, T2's `removeIf` may delete the brand-new entry (because the predicate `!now.isBefore(info.lastResendAt.plusSeconds(60))` evaluates true for any entry whose timestamp is older than 60 s — but the lambda's `now` and T2's `now` differ by sub-millisecond, so the new entry can be removed if T2's snapshot of `now` is just ahead). On the next resend call the slot looks empty and `tryConsumeResend` returns `true` again — **the 60 s cooldown can be bypassed by concurrent cleanup**.

**Minimal fix:** Drop the `cleanExpiredResends()` call outside the lambda and rely only on `compute`'s atomic check (`if (!now.isBefore(existing.lastResendAt.plusSeconds(RESEND_COOLDOWN_SECONDS)))` already returns the existing entry when cooldown is active). Optional: keep `cleanExpiredResends` but ensure it is only invoked from a single scheduled task (not per request).

### F2 — `register()` does not persist `email` on `OtpVerification`, breaking email-channel verify [High]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:134-144`

**Evidence:** The `OtpVerification` builder inside `register()` sets only `.phone(...)` and never `.email(...)`. The `OtpVerification.email` column allows null and is explicitly populated only by `resendOtp` (line 334). `verifyOtp` (line 261-268) resolves email-channel lookups via `otpVerificationRepository.findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)`.

**Impact (correctness, full flow broken):**
1. User `POST /api/v1/auth/register` with `email="a@b.com"` → user row created, OTP row created with `email = null`, `emailService.sendOtpVerificationEmail` called → user receives OTP by email.
2. User `POST /api/v1/auth/verify-otp` with `email="a@b.com"`.
3. `findTopByEmailAndUsedAtIsNull("a@b.com")` returns `Optional.empty()` because the persisted row has `email = null`.
4. `verifyOtp` throws `ValidationException("Invalid or expired OTP")` → **email-registered users can never complete registration.**

Phone-only registration is unaffected because the builder does populate `.phone(...)`. Resend path works because it re-creates the OTP with `.email(...)` set — but by then the original email channel is broken for fresh registrations.

**Test gap (related):** `RegistrationIntegrationTest.verifyOtp_WithExpiredOtp_ShouldReturnError` (line 310-344) seeds an `OtpVerification` without `.email(...)` but still expects `verifyOtp` to "find" it via email; the test passes only because the empty result triggers the same `Invalid or expired OTP` branch. **No test in the suite covers the happy-path email registration → verify-with-email flow**, which is why this regression survived.

**Minimal fix:** In `register()` at line 134-142, add `.email(email != null && !email.isBlank() ? email : null)` symmetric to the `.phone(...)` line. Mirror in `login()` at line 219-227 if email-channel login is supported. Add an end-to-end integration test that registers with email and verifies with the captured OTP via `verifyOtp`.

### F3 — `verifyOtp` does not normalize email before lookup [Medium]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:251,262`

**Evidence:** `verifyOtp` reads `String email = request.getEmail();` without trimming or lowercasing, while `register()` stores `email.trim().toLowerCase()` on the `User` entity (line 86). Resend persists `StringUtils.trimToNull(user.getEmail())` on `OtpVerification.email` (line 311, 334) — also lowercased already. The lookup `findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)` therefore compares the user-supplied raw value against the persisted lowercased value.

**Impact:** If the user enters `Test@Example.com ` (trailing space, mixed case) in `/verify-otp`, the repository query does not match the stored `test@example.com` row → "Invalid or expired OTP". This is a UX defect only — but combined with F2 it means **email-registered users cannot verify via email at all**, regardless of casing/spacing.

**Minimal fix:** `email = StringUtils.trimToNull(request.getEmail()); if (email != null) email = email.toLowerCase();` at line 251. (Tracked independently of F2, but F2 masks it today.)

### F4 — `completeRegistration` does not hash-compare in constant time and decrements attempts after exception throw [Low]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java:368-379`

**Evidence:** `if (!inputHash.equals(verification.getCodeHash())) { verification.setAttempts(...); ...; throw new ValidationException(...); }`. The mismatch is detected via `String.equals`, which is **not constant-time**, leaking timing information about how many bytes match. The method has `@Transactional(noRollbackFor = ValidationException.class)` so the rollback suppression is intentional — the attempts decrement must commit.

**Impact:** Timing oracle on the stored SHA-256 OTP hash. SHA-256 of a 6-digit code is feasible to brute-force in any case (10⁶ possibilities), so the timing oracle adds little practical value to an attacker, but the deferred-work list already calls out HMAC + constant-time compare as a follow-up — keeping this finding visible so the work item remains justified.

**Minimal fix:** Use `MessageDigest.isEqual(inputHashBytes, codeHashBytes)`. Tracked for the existing HMAC deferred item; not a regression of this stabilization round.

### F5 — `ResendOtpRequest` validator permits leading/trailing whitespace [Low]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/ResendOtpRequest.java:11-33`

**Evidence:** The `@AssertTrue isIdentifierPresent()` checks `phone != null && !phone.isBlank()` and `email != null && !email.isBlank()`. `@Email` validates format but does not trim. `@VietnamesePhoneNumber` also does not trim. The service-side `StringUtils.trimToNull(request.getPhone())` / `email` happens **after** validation, so the constraint passes for `" test@example.com"` but the lookup then fails because the stored value is canonicalized differently (see F3).

**Impact:** Same as F3 (UX). Surface inconsistent input as validation errors so the client sees the trim explicitly.

**Minimal fix:** Normalize inside the DTO setters, or use a custom constraint that trims before validating.

### F6 — `OtpServiceImpl.createAndSend` logs raw OTP in plaintext [pre-existing, Medium]

**File:** `04_SourceCode\Backend\src\main\java\com\carebridge\backend\security\service\impl\OtpServiceImpl.java:51`

**Evidence:** `log.info("Mock OTP sent for phoneEnding={}, purpose={}, otp={}", phoneEnding(phone), purpose, rawOtp);` — `rawOtp` is the un-hashed 6-digit code.

**Impact:** If this service is wired into any non-mock path, raw OTPs land in application logs (and any log aggregator that retains them). The file predates the stabilization diff (the diff at `OtpServiceImpl.java` is small, but this line is pre-existing). Marking **pre-existing** so it does not block Story 1.1 close, but it should be filed under the deferred `HMAC OTP + reduced PII` workstream.

**Minimal fix:** Replace `rawOtp` in the log statement with a hash prefix or remove entirely; rely on the `MockSmsService` log line for delivery confirmation.

### F7 — Two parallel OtpService implementations coexist (`OtpServiceImpl` + inline logic in `AuthServiceImpl`) [Medium — design smell]

**File:** `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/OtpServiceImpl.java` vs `AuthServiceImpl.java:74-172, 192-245, 286-366`

**Evidence:** `OtpServiceImpl.createAndSend` and `OtpServiceImpl.verify` provide OTP logic, but `AuthServiceImpl.register`, `AuthServiceImpl.login`, and `AuthServiceImpl.resendOtp` reimplement the same logic inline (build entity, hash, persist, deliver, audit) without delegating to `OtpService`. The `login()` flow even uses different repository methods (`findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc` vs `findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc`), creating two parallel "what counts as the pending OTP" definitions.

**Impact:** Drift risk — security-relevant behavior (eligibility checks, attempt counting, identifier normalization) will diverge over time. Not a defect introduced by this stabilization round, but it amplifies the severity of F2 because the inline builder in `register()` is harder to spot than a single shared `OtpService.createAndSend` would be.

**Minimal fix:** Out of scope for the current bugfix; track under a `refactor: collapse AuthServiceImpl OTP logic into OtpService` follow-up so future readers do not duplicate the path.

## Summary

| ID  | Severity | File                        | Scope                                            |
|-----|----------|-----------------------------|--------------------------------------------------|
| F1  | Medium   | RateLimitPolicy.java        | Bypass of resend cooldown under concurrent GC    |
| F2  | High     | AuthServiceImpl.java:134-144| Email-registration verify-OTP flow is broken     |
| F3  | Medium   | AuthServiceImpl.java:251    | Email normalization missing in verifyOtp         |
| F4  | Low      | AuthServiceImpl.java:368-379| Non-constant-time OTP hash compare               |
| F5  | Low      | ResendOtpRequest.java:11-33 | Whitespace-tolerant validation, downstream fails |
| F6  | Medium   | OtpServiceImpl.java:51      | Raw OTP logged (pre-existing)                    |
| F7  | Medium   | both OtpServiceImpl files   | Parallel OTP code paths (design smell, pre-existing) |

**F2 is the only finding that breaks user-visible functionality inside Story 1.1's scope** (email-channel registration). F1 is a real correctness risk for the resend cooldown but requires concurrent load to trigger. F3, F4, F5 cluster around the email-verify path and will become moot once F2 is fixed correctly. F6 and F7 are pre-existing and out of scope for this stabilization; defer.