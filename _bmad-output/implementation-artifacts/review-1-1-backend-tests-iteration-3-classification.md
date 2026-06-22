# Review Classification — Story 1.1 Iteration 3

**Spec:** `_bmad-output/implementation-artifacts/spec-1-1-fix-backend-tests.md`
**Baseline:** `8bfcefae02b5a619085f3cb96bf7175c7e01171d`
**Build state:** ✅ `mvnw.cmd clean test` → BUILD SUCCESS, 56 tests, 0 fail/error/skip (verified live 2026-06-22 21:03); `mvnw.cmd clean package` → BUILD SUCCESS, executable JAR produced (verified live 2026-06-22 21:14).
**Current specLoopIteration:** 2 (handoff §6). 3 loopbacks remaining before HALT.

## Source files

- `review-1-1-backend-tests-iteration-3-blind-hunter.md` — 7 findings (F1–F7)
- `review-1-1-backend-tests-iteration-3-edge-case-hunter.md` — 10 findings (E1–E10)
- `review-1-1-backend-tests-iteration-3-acceptance-auditor.md` — All 7 AC pass; 4 pre-existing issues flagged for visibility only

## Cascade Order Result

Processing order: intent_gap → bad_spec → patch → defer → reject.

### intent_gap (0)

None. Spec frozen section + story AC are coherent enough to interpret every Story 1.1 stabilization finding without human renegotiation.

### bad_spec (1) — triggers loopback

| ID | File | Why bad_spec | Spec change needed |
|----|------|--------------|---------------------|
| EC-E6 | `AuthServiceImpl.java:316-345` | Diff introduces `tryConsumeResend(resendAccountKey)` at line 317, **before** the DB mutation chain (`save(existingVerification)`, `save(newVerification)`, `emailService.sendOtpVerificationEmail`, `auditService.log`). If any later step throws, the `@Transactional` rollback discards the DB writes but the in-memory `ConcurrentHashMap` slot is **not** rolled back. The user sees 5xx once and then 429 for up to 60 s with no OTP delivered. | Spec line `## I/O & Edge-Case Matrix` row "Rejected resend" only covers the *rejected* path; the *partial-failure-after-consume* path is missing. Spec must add: "If a resend is accepted by the cooldown gate but fails before the new OTP is persisted, the cooldown slot must be released so the user can retry without waiting a full window." |

**KEEP instructions (positive preservation during re-derivation):**
- Stable `user.id`-keyed cooldown (`AuthServiceImpl.java:316`).
- `tryConsumeResend` atomic check-and-record inside `ConcurrentHashMap.compute` (`RateLimitPolicy.java:240-257`).
- Exactly-one identifier validation at both DTO and service layers (`ResendOtpRequest.java:11-33`, `AuthServiceImpl.java:289-296`).
- `existingVerification.setUsedAt(now)` then persist replacement as a separate row (`AuthServiceImpl.java:327-341`).
- Audit `OTP_RESENT` with `Map.of(purpose, channel, otpVerificationId)` and exactly-one persistence invariant (`AuthServiceImpl.java:349-357`).
- 56-test baseline + `clean package` JAR.
- Existing integration tests `resendOtp_WithValidRequest_ShouldSendNewOtpAndReturnSuccess`, `resendOtp_WithPhoneAndEmail_ShouldRejectWithoutSideEffects`, `resendOtp_SwitchingChannelForSameAccount_ShouldRemainRateLimited`, `resendOtp_WithinCooldownPeriod_ShouldReturnTooManyRequests`.

**Known-bad state avoided by the spec fix:** a benign transient backend error (DB blip, SMTP timeout) converting into a spurious 429 cooldown that locks the user out of OTP delivery for up to a minute.

### patch (2) — auto-fix, no human input

| ID | File | Fix sketch | Why patch |
|----|------|-----------|-----------|
| BH-F1 | `RateLimitPolicy.java:240-257` | Drop the per-call `cleanExpiredResends()` invocation. The atomic `compute` lambda already returns the existing entry when cooldown is active, so the cleanup pass is redundant and introduces the entry-removal window described in BH-F1. | Code-only fix inside the diff scope; matches the spec's "atomic per-account resend cooldown state" requirement. |
| EC-E2 | `OtpVerificationRepository.java:17` | Rename to `findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId)`. | Deterministic lookup; single-line repo contract change; no schema change. |

### defer (8) — pre-existing or out-of-scope; appended to `_bmad-output/implementation-artifacts/deferred-work.md`

| ID | File | Reason for defer |
|----|------|------------------|
| BH-F2 | `AuthServiceImpl.java:134-144` | `register()` does not persist `.email(...)` on `OtpVerification`. Affects email-channel `verify-otp`, which is **Story 1.2** scope (`1-2-otp-verification-and-account-activation.md`). Out of scope for Story 1.1 stabilization. |
| BH-F3 | `AuthServiceImpl.java:251` | `verifyOtp` does not normalize email. Same scope as BH-F2. |
| BH-F4 | `AuthServiceImpl.java:368-379` | Non-constant-time OTP hash compare. Already in deferred-work under "thay SHA-256 OTP bằng HMAC/slow hash và constant-time comparison". |
| BH-F6 | `OtpServiceImpl.java:51` | Raw OTP logged at INFO. Already in deferred-work under "giảm raw PII trong audit metadata". |
| BH-F7 | both OtpService files | Parallel OTP code paths. Design refactor, not a regression. |
| EC-E3 | `AuthServiceImpl.java:368-419` | Concurrent verify lost update. Pre-existing in `completeRegistration`/`completeLogin`. Already in deferred-work under "atomic verification-attempt limiting". |
| EC-E4 | `AuthServiceImpl.java:247-284` | `verifyOtp` does not use `RateLimitPolicy`. Intent is DB counter, not in-memory policy; same deferred item as EC-E3. |
| EC-E7 | `RegistrationIntegrationTest.java:233-267` | Placeholder happy-path test. Test coverage gap; pre-existing. |
| EC-E9 | `AuthServiceImpl.java:192-244` | Email-channel login unreachable. Login is not Story 1.1 scope. |
| EC-E10 | `AuthServiceImpl.java:409-446` | Per-OTP login lockout bypassed via re-login. Login is not Story 1.1 scope. |

### reject (3) — noise, dropped silently

| ID | Reason for reject |
|----|-------------------|
| BH-F5 | `ResendOtpRequest` whitespace tolerance is a UX nit, not a defect. Service-side `StringUtils.trimToNull` handles it; downstream behavior is consistent. |
| EC-E1 | 60 s boundary at exactly `lastResendAt + 60s` — verified correct, documented in review only. |
| EC-E5 | Cross-channel rejection verified to leave no side effects. |

## Loopback Decision

The 1 **bad_spec** finding (EC-E6) requires a spec amendment before the code is re-derived. Per `step-04-review.md`:

1. Extract KEEP instructions — listed above.
2. Revert code changes (no destructive git ops; revert only the order-of-operations in `resendOtp` after spec update).
3. Append a change-log entry to `spec-1-1-fix-backend-tests.md` `## Spec Change Log`:
   - **Triggering finding:** EC-E6 — cooldown consumed before DB write, no rollback on partial failure.
   - **What was amended:** added a new row to `## I/O & Edge-Case Matrix`: "Partial resend failure | Resend accepted by cooldown, later step throws | DB mutations rolled back; cooldown slot released so user can retry without waiting" + acceptance criterion: "Given the cooldown gate consumes a slot, when any subsequent step in the resend transaction fails before commit, then the rollback must also release the cooldown slot."
   - **Known-bad state avoided:** a single transient backend error converting into a spurious 60 s lockout for an innocent user.
   - **KEEP instructions:** see list above.
4. Run `step-03-implement.md` to re-derive the resend transaction order.
5. Run `step-04-review.md` again. `specLoopIteration` becomes 3.

## Human Escalation Needed

The bad_spec loopback touches code that currently passes all 7 AC. Before executing the loopback, the human should confirm:

1. **Proceed with the bad_spec loopback** as written above (recommended — clean spec → coherent code, fits the frozen "atomic" wording).
2. **Convert EC-E6 to `defer`** instead — accept the spurious-cooldown risk as documented behavior; the leak only triggers on a partial backend failure (rare in dev, possible in production).
3. **Convert EC-E6 to `patch`** — keep the spec as-is and just reorder `tryConsumeResend` to happen after `save(newVerification)` so rollback semantics follow naturally. Note: this contradicts the strict reading of "When in doubt between bad_spec and patch, prefer bad_spec" because the spec's existing "atomic" wording is too weak to unambiguously require this reordering.

`specLoopIteration` would increment only on option (1). Options (2) and (3) close the review loop and let the story proceed to step-05.