---
title: 'Story 1.1 Cooldown Atomicity and Pending OTP Ordering'
type: 'bugfix'
created: '2026-06-22'
status: 'done'
baseline_commit: '179c1a9dc41a9ef22ff024c6736670e043ea0622'
specLoopIteration: 2
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-1-1-fix-backend-tests.md'
  - '{project-root}/_bmad-output/implementation-artifacts/review-1-1-backend-tests-iteration-3-classification.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Two patch-class findings remain after Story 1.1 stabilization. `tryConsumeResend` performs a global expired-entry cleanup outside its per-key atomic `compute`, creating a removal window that can permit more than one resend; pending OTP lookup has no explicit newest-first ordering and may return a stale unused row when multiple rows exist.

**Approach:** Keep resend consumption atomic within the target account key and make pending OTP selection deterministic by newest `createdAt`, with focused concurrency and persistence evidence.

## Boundaries & Constraints

**Always:** Preserve Java 21, Spring Boot 4.1, the public authentication API, stable `user.id` cooldown keys, the 60-second boundary, `Long` OTP identifiers, existing transaction/cooldown release behavior, and all unrelated worktree changes.

**Ask First:** Any schema or migration change, public request/response change, distributed rate-limit design, new dependency, or modification outside Backend authentication tests/code.

**Never:** Disable tests, weaken rate limits, delete OTP history, change deferred security work, reset or clean the dirty worktree, stage unrelated files, commit, or push without explicit user instruction.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Concurrent consume | Multiple callers consume the same account key inside one cooldown window | Exactly one caller succeeds; all others are rejected | No entry-removal gap may reopen the slot |
| Cooldown expiry | Same account key reaches exactly 60 seconds | Exactly one new caller atomically replaces the expired timestamp | Subsequent callers remain rejected |
| Multiple pending OTPs | One user has multiple unused OTP rows with different `createdAt` values | Repository returns the newest row only | No stale row is invalidated or resent |

</frozen-after-approval>

## Code Map

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java` — per-account in-memory resend cooldown and atomic consume operation.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/OtpVerificationRepository.java` — pending OTP derived query contract.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java` — production pending-OTP lookup call site.
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/policy/RateLimitPolicyResendTest.java` — cooldown boundary and concurrency evidence.
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/service/AuthServiceResendOtpTest.java` — resend orchestration mocks using the ordered query.
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java` — persistence and newest-row selection evidence.

## Tasks & Acceptance

**Execution:**
- [x] `RateLimitPolicy.java` — keep global cleanup out of `tryConsumeResend` and make every remaining resend cleanup re-check/removal atomic per key so it cannot delete a newly refreshed cooldown.
- [x] `OtpVerificationRepository.java` — replace the unordered lookup with `findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(UUID userId)` so equal timestamps resolve by newest generated ID.
- [x] `AuthServiceImpl.java` and test call sites — migrate to the ordered repository contract without changing endpoint behavior.
- [x] `RateLimitPolicyResendTest.java` — retain concurrent same-key evidence and add a cleanup-versus-consume race check proving the refreshed slot survives.
- [x] `RegistrationIntegrationTest.java` — prove newest pending OTP selection when multiple unused rows exist.

**Acceptance Criteria:**
- Given concurrent resend consumers for one account within a cooldown window, when they invoke atomic consumption, then no more than one succeeds.
- Given a lazy cleanup path races with an expired-key consume, when the consumer refreshes that account's cooldown, then cleanup must retain the new active timestamp and the next consume remains rejected.
- Given multiple unused OTP records for one user, when resend resolves the pending OTP, then it invalidates the newest record selected by descending `createdAt`.
- Given the focused tests pass, when Backend `clean test` and `clean package` run on Java 21, then all tests pass and the executable JAR is produced.
- Given the pre-existing dirty worktree, when the patch is complete, then no unrelated user-owned file is modified, staged, reset, or deleted.

## Spec Change Log

- **Iteration 1 review loopback — resend cleanup race:** Blind Hunter and Edge Case Hunter agreed that removing `cleanExpiredResends()` from `tryConsumeResend` did not fully close BH-F1 because `canResendOtp` and `cleanExpiredWindows` can still invoke a map-wide `removeIf` while another thread refreshes the same key. Tasks and design notes now require per-key atomic cleanup via current-value re-evaluation. **Known-bad state avoided:** a newly refreshed cooldown being deleted by concurrent cleanup, reopening the resend slot. **KEEP:** preserve the ordered newest-pending-OTP query and all migrated call sites, stable account UUID keys, exact 60-second boundary, cooldown reset behavior on delivery failure, public API shape, focused 38-test result, full 60-test/JAR baseline, and existing concurrent consume tests.

## Design Notes

`tryConsumeResend` must not call a map-wide cleanup before `compute`. Expired entries are replaced atomically inside its existing `compute` lambda. Any periodic/lazy resend cleanup retained on inspection paths must use `computeIfPresent` (or an equivalent conditional update) so expiration is re-evaluated against the current value while holding the key-level map operation.

The repository method retains the current `userId` property traversal style and adds `Top` plus `OrderByCreatedAtDescIdDesc`, avoiding schema or entity changes while making equal-timestamp selection deterministic.

## Verification

**Commands:**
- `mvnw.cmd -Dtest=RateLimitPolicyResendTest,AuthServiceResendOtpTest,RegistrationIntegrationTest test` — expected: focused tests pass.
- `mvnw.cmd clean test` — expected: `BUILD SUCCESS`, zero failures/errors/skips.
- `mvnw.cmd clean package` — expected: `BUILD SUCCESS` and executable `target/backend-0.0.1-SNAPSHOT.jar`.
- `git diff --check -- <spec-scoped paths>` — expected: no whitespace errors.

**Results:**
- Initial focused suite: `BUILD SUCCESS`; 38 tests, 0 failures, 0 errors, 0 skipped.
- Final focused suite after equal-timestamp tie-breaker patch: `BUILD SUCCESS`; 39 tests, 0 failures, 0 errors, 0 skipped.
- Post-loopback policy suite: `BUILD SUCCESS`; 13 tests, 0 failures, 0 errors, 0 skipped.
- `clean test`: `BUILD SUCCESS` on Java 21.0.10; 61 tests, 0 failures, 0 errors, 0 skipped.
- `clean package`: `BUILD SUCCESS` with the same 61 passing tests; executable JAR produced at `target/backend-0.0.1-SNAPSHOT.jar`.
- Scoped `git diff --check`: no whitespace errors; only expected LF/CRLF warnings.

## Suggested Review Order

**Atomic resend cooldown**

- Per-key compute keeps the consume decision atomic at the exact expiry boundary.
  [`RateLimitPolicy.java:240`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java#L240)

- Conditional cleanup re-evaluates current values before removing expired entries.
  [`RateLimitPolicy.java:292`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java#L292)

- Concurrent first-consume evidence permits exactly one winner.
  [`RateLimitPolicyResendTest.java:51`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/policy/RateLimitPolicyResendTest.java#L51)

- Cleanup-versus-refresh stress evidence protects the newly active slot.
  [`RateLimitPolicyResendTest.java:70`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/policy/RateLimitPolicyResendTest.java#L70)

**Deterministic pending OTP selection**

- Timestamp ordering uses generated ID as a deterministic tie-breaker.
  [`OtpVerificationRepository.java:17`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/OtpVerificationRepository.java#L17)

- Resend orchestration resolves the latest pending row through the ordered contract.
  [`AuthServiceImpl.java:314`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java#L314)

- Integration evidence proves equal timestamps select and invalidate the higher ID.
  [`RegistrationIntegrationTest.java:594`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java#L594)

- Unit mocks enforce the same repository contract across rejection paths.
  [`AuthServiceResendOtpTest.java:94`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/service/AuthServiceResendOtpTest.java#L94)
