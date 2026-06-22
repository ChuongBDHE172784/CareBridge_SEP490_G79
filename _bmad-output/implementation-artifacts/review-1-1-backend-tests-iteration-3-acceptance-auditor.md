# Acceptance Auditor Review Prompt — Iteration 3

Audit without conversation history. Read fully:

- `_bmad-output/implementation-artifacts/spec-1-1-fix-backend-tests.md`
- `_bmad-output/implementation-artifacts/epic-1-context.md`
- `_bmad-output/implementation-artifacts/1-1-user-registration-with-email-or-phone.md`
- the complete relevant diff from baseline `8bfcefae02b5a619085f3cb96bf7175c7e01171d`, including tracked and untracked files.

Verify every frozen boundary and acceptance criterion, especially the Iteration 2 change-log requirement that cooldown is per account across phone/email. Confirm Java 21 `clean test` has 56 passing tests and `clean package` produces the executable JAR. Confirm rejected cross-channel resend causes no second OTP, delivery, or audit.

Report current-story violations with severity, file/line, evidence, affected criterion, and minimal fix. List confident pre-existing issues separately. Return `No findings` if none. Do not modify or stage files.

# Output

## Reviewer

- Role: Acceptance Auditor (independent reviewer; reads spec, story, and the diff, but no conversation history).
- Diff scope: tracked diff vs baseline `8bfcefae02b5a619085f3cb96bf7175c7e01171d` (16 files in `04_SourceCode/Backend/src/main/java/com/carebridge/backend/{security,common/exception,audit}` and `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/**`) plus untracked files in the same packages.
- Pre-build verification:
  - `mvnw.cmd clean test` (JDK 21.0.10) → **BUILD SUCCESS, 56 tests, 0 failures, 0 errors, 0 skipped**.
  - `mvnw.cmd clean package` → **BUILD SUCCESS, 56 tests pass, executable JAR produced at `target/backend-0.0.1-SNAPSHOT.jar` (~63 MB), spring-boot:repackage OK**.
  - `git status` shows worktree dirt only in spec scope: auth/security/exception/audit files + BMAD artifacts + the unrelated user-owned design/SRS files (not modified by this fix, untouched here).

## Acceptance Criteria Audit

### AC-1 — Clean `mvnw clean test` passes with zero failures, errors, or skips introduced by this fix

**Status:** ✅ Pass (live re-verified)

**Evidence:** Output above. All 56 tests pass on JDK 21.0.10. Spec target matched exactly.

---

### AC-2 — Clean `mvnw clean package` produces the executable JAR with `BUILD SUCCESS`

**Status:** ✅ Pass (live re-verified)

**Evidence:** Output above. JAR built and repackaged by `spring-boot:4.1.0:repackage`. Same 56 tests pass before `package`.

---

### AC-3 — Cross-channel rejection: 400, no cooldown consumption, no OTP mutation, no delivery, no resend audit

**Status:** ✅ Pass

**Evidence:** `RegistrationIntegrationTest.resendOtp_WithPhoneAndEmail_ShouldRejectWithoutSideEffects` (line 414-462) covers all five sub-conditions:
- HTTP status: `.andExpect(status().isBadRequest())` (line 443)
- Cooldown not consumed: not directly asserted in this test, but `verify(rateLimitPolicy, never()).recordResendAttempt(...)` is asserted via the AuthService test `resendOtp_WhenBothIdentifiersProvided_ShouldRejectWithoutSideEffects` (line 165-175) — `verifyNoInteractions(userRepository, otpVerificationRepository, rateLimitPolicy, emailService, smsService)`.
- No OTP mutation: `assertThat(pendingAfterRejection.getId()).isEqualTo(originalOtp.getId())` (line 448) and `assertThat(pendingAfterRejection.getCodeHash()).isEqualTo(originalOtp.getCodeHash())` (line 449).
- No delivery: `verifyNoInteractions(emailService, smsService)` (line 454).
- No resend audit: `assertThat(...OTP_RESENT audit count).isEqualTo(auditCountBefore)` (line 450-453).

Defensive double-check: the controller-layer `@AssertTrue` in `ResendOtpRequest.isIdentifierPresent()` (line 28-33) also rejects before the service is reached, so the request never reaches `tryConsumeResend`. No leak.

---

### AC-4 — Valid resend outside cooldown: 1 pending replacement, delivery to stored identifier, exactly 1 `OTP_RESENT` audit

**Status:** ✅ Pass

**Evidence:** `RegistrationIntegrationTest.resendOtp_WithValidRequest_ShouldSendNewOtpAndReturnSuccess` (line 348-411):
- Prior OTP retained as used: `assertThat(persistedOriginal.getUsedAt()).isNotNull()` (line 394).
- Exactly one distinct pending replacement: `pendingOtps` has size 1 (line 398), `replacement.getId()` ≠ `originalOtp.getId()` (line 400).
- Delivery uses stored identifier: `verify(smsService).sendOtpVerificationSms(eq(phone), anyString(), eq(5))` (line 403) where `phone` came from `userRepository.findByPhone(phone)`, not the request payload — and `verifyNoInteractions(emailService)` (line 404) confirms delivery did not also go to the alternate channel.
- Audit count incremented by exactly one: `auditCountAfter == auditCountBefore + 1` (line 410).

---

### AC-5 — In-cooldown resend → HTTP 429, non-secret envelope, no OTP mutation, no delivery

**Status:** ✅ Pass

**Evidence:** `RegistrationIntegrationTest.resendOtp_WithinCooldownPeriod_ShouldReturnTooManyRequests` (line 521-555):
- 429 status: `.andExpect(status().isTooManyRequests())` (line 551).
- Non-secret envelope: `$.message` matches `"Please wait before resending OTP\\. Cooldown: (59|60) seconds"` (line 552-554). No OTP, no user identifier, no internal exception name exposed.
- No OTP mutation: integration test asserts no second `save` was issued inside the 429 path; backed by `AuthServiceResendOtpTest.resendOtp_WhenRateLimited_ShouldThrowRateLimitExceededException` line 234: `verify(otpVerificationRepository, never()).save(any())`.
- No delivery: same test line 235: `verifyNoInteractions(emailService, smsService)`.

---

### AC-6 — Dual-identifier account: cross-channel requests must be rejected on the second channel after the first consumes the slot

**Status:** ✅ Pass

**Evidence:** `AuthServiceResendOtpTest.resendOtp_SwitchingChannelForSameAccount_ShouldRemainRateLimited` (line 239-273):
- Phone channel succeeds first: setup `tryConsumeResend(accountKey)` returns `true, false` (line 256) → first call passes, second call throws.
- `accountKey = userId.toString()` (line 243) — the stable UUID, **not** the phone or email.
- Both calls go through `tryConsumeResend(accountKey)` (line 269): `verify(rateLimitPolicy, times(2)).tryConsumeResend(accountKey)` — proving both attempts used the same key.
- Second call throws `RateLimitExceededException` (line 266-267).
- Exactly one delivery happened, on the phone channel: `verify(smsService).sendOtpVerificationSms(eq(phone), ...)` (line 271) and `verifyNoInteractions(emailService)` (line 272).

Backed by `RateLimitPolicyResendTest` (line 1-185) which exercises the boundary at 60 s deterministically via the `MutableClock`.

---

### AC-7 — Working-tree scope: only authentication test/code and BMAD artifacts required by this bugfix were changed

**Status:** ✅ Pass

**Evidence:** `git status` against baseline `8bfcefa` shows changes only in the spec-scope paths (`04_SourceCode/Backend/src/main/java/com/carebridge/backend/{security,common/exception,audit}` and `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/**`) plus the BMAD artifacts (`_bmad-output/...`). The deleted `.docx` files and modified design SRS/migration files in the worktree were **already** in that state before this stabilization round (see handoff §9 "Worktree bẩn") and were not touched here. No `git reset`, `checkout --`, or `clean` operation was performed during this review.

---

## Frozen-Boundary Audit (Spec §"Boundaries & Constraints")

| Boundary | Status | Evidence |
|----------|--------|----------|
| Spring Boot 4.1 preserved | ✅ | `pom.xml` retains `spring-boot-starter-parent` 4.1.0; tests use `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` and `MockitoBean` (line 25 of `RegistrationIntegrationTest`) |
| Java 21 preserved | ✅ | `java -version` → 21.0.10 LTS; `clean test` and `clean package` both succeed on JDK 21 |
| Long `OtpVerification.id` preserved | ✅ | `@Id @GeneratedValue(strategy = IDENTITY) private Long id;` (`OtpVerification.java:36-38`); test fixture `AtomicLong otpIdSequence = new AtomicLong(10L)` (`AuthServiceResendOtpTest.java:44`) |
| Public auth API contract preserved | ✅ | `AuthController.java:37-49` exposes exactly the routes listed in spec; no new public endpoints; `/api/v1/**` fallback still authenticated |
| OTP secrecy preserved | ✅ | `MockEmailService`/`MockSmsService` log only `[MOCK EMAIL] To: ..., OTP: ..., Expires in: ... minutes`. Raw OTP logging exists in `OtpServiceImpl.java:51` (pre-existing, see Pre-existing Issues) but that path is not exercised by any current Story 1.1 code path in `clean test` |
| Safe error messages preserved | ✅ | `RateLimitExceededException` message: `"Please wait before resending OTP. Cooldown: N seconds"` — no secret; `GlobalExceptionHandler` returns typed envelopes without internal stack traces |
| Unrelated worktree changes preserved | ✅ | `git status` confirms none of the pre-existing dirty files were modified by this fix |
| Tests not disabled or skipped | ✅ | 56 tests, 0 skipped; no `@Disabled` annotations added by the diff |
| No global security weakening | ✅ | `SecurityConfig` still requires `.authenticated()` for `/api/v1/**` fallback; CSRF helper `csrfCustomizer` (line 42-47) applies only inside the integration test slice |

All frozen boundaries intact.

---

## Iteration 2 Change-Log Re-Confirmation

> "cooldown is keyed by stable `user.id`, not by phone or email channel; switching channels for the same account must not create a second replacement, delivery, or audit"

**Re-confirmed:** `AuthServiceImpl.resendOtp` line 316 `String resendAccountKey = user.getId().toString();` and line 317 `rateLimitPolicy.tryConsumeResend(resendAccountKey)`. The `verify(rateLimitPolicy, times(2)).tryConsumeResend(accountKey)` assertion in the cross-channel test (line 269) is direct evidence. Both `verify(smsService).sendOtpVerificationSms(...)` and `verifyNoInteractions(emailService)` together prove only one delivery happened despite two channel-switched requests.

## Pre-existing Issues (Not Story 1.1 Scope — flagging for visibility only)

These were surfaced by the blind-hunter and edge-case reviews of the same diff. They are **not Story 1.1 stabilization regressions** and do **not** block acceptance; they are recorded so the next story can pick them up cleanly:

| ID  | Severity | File | Summary |
|-----|----------|------|---------|
| BH-F2 / AC-Note-A | High | `AuthServiceImpl.java:134-144` | `register()` does not populate `.email(...)` on the OtpVerification row. Email-channel `verify-otp` returns "Invalid or expired OTP" for email-registered users. **Pre-existing in Story 1.1 baseline (task list Phase 4 already marked complete with this state)**, but the diff did not fix it. Flag as `intent_gap` for product review or `defer` to Story 1.2 (OTP Verification & Account Activation) which already exists in `_bmad-output/implementation-artifacts/1-2-otp-verification-and-account-activation.md`. |
| EC-E7 | Medium | `RegistrationIntegrationTest.java:233-267` | `verifyOtp_WithValidOtp_ShouldActivateUserAndReturnTokens` is a placeholder that asserts nothing about the happy path. Pre-existing; the diff did not add a real assertion. Defer to a "test coverage gap" follow-up. |
| EC-E10 | Low | `AuthServiceImpl.java:409-446` | Per-OTP login attempt limiter can be bypassed by re-issuing `/login`. Pre-existing; defer. |
| BH-F6 | Medium | `OtpServiceImpl.java:51` | `log.info("...otp={}", rawOtp)` writes raw OTP to log. Pre-existing; not exercised by current flow; defer. |

These are flagged here per the prompt's "List confident pre-existing issues separately" instruction. They do not affect Story 1.1's frozen acceptance criteria above.

---

## Summary

**Acceptance status:** **All 7 acceptance criteria from `spec-1-1-fix-backend-tests.md` pass.** The fix is ready for the next step in `bmad-quick-dev` (review classification → step-05 present).

**Verification commands run during this audit (results above):**
- `JAVA_HOME=C:/Program Files/Java/jdk-21.0.10 PATH=$JAVA_HOME/bin:$PATH ./mvnw.cmd clean test`
- `JAVA_HOME=C:/Program Files/Java/jdk-21.0.10 PATH=$JAVA_HOME/bin:$PATH ./mvnw.cmd clean package -DskipTests=false`

**No current-story violations found.** The 4 pre-existing issues are explicitly out of scope and should be tracked via `deferred-work.md` (BH-F2 is the most important to escalate to product, since it blocks email-channel registration).