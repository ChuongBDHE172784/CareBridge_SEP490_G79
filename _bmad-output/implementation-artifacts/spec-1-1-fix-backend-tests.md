---
title: 'Story 1.1 Backend Test Stabilization'
type: 'bugfix'
created: '2026-06-22'
status: 'done'
baseline_commit: '8bfcefae02b5a619085f3cb96bf7175c7e01171d'
specLoopIteration: 2
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/1-1-user-registration-with-email-or-phone.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The Story 1.1 backend suite cannot complete from a clean Maven state because the registration integration test uses an invalid Spring Boot 4 test import and the OTP resend unit fixture assigns a UUID to a Long identifier. Earlier non-clean runs also exposed behavioral failures around CSRF, OTP attempt persistence, resend auditing, and rate limiting that must be revalidated after clean compilation.

**Approach:** Restore clean test compilation using the APIs and identifier types already selected by the project, then run the full suite and make only narrowly scoped corrections required by failing Story 1.1 registration, verification, and resend tests.

## Boundaries & Constraints

**Always:** Preserve Spring Boot 4.1, Java 21, the existing Long `OtpVerification.id`, the public authentication API contract, OTP secrecy, safe error messages, and all unrelated working-tree changes. Prefer correcting incorrect test setup when production behavior already matches the story; correct production code when a test exposes a genuine acceptance-criteria violation.

**Ask First:** Any database schema change, public endpoint or response-contract change, dependency version change, deletion of existing tests, or modification outside the Backend authentication/test scope.

**Never:** Disable or skip tests, weaken security globally, remove CSRF protection from production configuration, hard-code OTPs, expose OTP values, reset the working tree, or rewrite unrelated user changes.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Clean compilation | Fresh `target` directory | Main and test sources compile under Maven | Compilation errors identify the exact incompatible import/type |
| Wrong OTP | Pending registration OTP with five attempts | Request fails and persisted attempts become four | Return the existing safe validation error |
| Valid resend | Existing user and pending OTP outside cooldown | New OTP is persisted, delivered, audited, and a success envelope is returned | No null-ID dereference in mocks or persistence |
| Rejected resend | Missing identifier/user/pending OTP or active cooldown | API returns the story-defined status and does not send an OTP | Preserve safe, non-leaking messages and consistent rate-limit state |
| Partial resend failure | Cooldown gate consumed, then a later step (persist new OTP / send / audit) throws before commit | DB mutations rolled back; the cooldown slot is released so the user can retry without waiting a full window | Return the same safe 5xx envelope as any transient failure; do not leak that the cooldown was consumed |

</frozen-after-approval>

## Code Map

- `04_SourceCode/Backend/pom.xml` — Spring Boot 4 test modules and Maven compiler configuration.
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java` — registration, verification, resend, MockMvc, security, and persistence coverage.
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/service/AuthServiceResendOtpTest.java` — isolated resend behavior and repository-generated identifier fixture.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/ResendOtpRequest.java` — exactly-one identifier boundary for the public resend endpoint.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java` — OTP attempt decrement, resend rate limiting, persistence, delivery, and audit behavior.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java` — atomic per-account resend cooldown state and deterministic timing contract.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java` — public access boundary for pending-account OTP resend.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/RateLimitExceededException.java` — typed rate-limit failure mapped to HTTP 429.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` — consistent error envelope and status mapping.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/policy/AuditEligibilityPolicy.java` — persistence eligibility for the `OTP_RESENT` audit action.
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/repository/AuditLogRepository.java` — integration-test evidence for persisted resend audit events.

## Tasks & Acceptance

**Execution:**
- [x] `RegistrationIntegrationTest.java` — use the Spring Boot 4 MockMvc API without a competing catch-all security chain; assert mixed identifiers are rejected with no mutation/delivery, successful resend leaves exactly one distinct pending OTP, and exactly one `OTP_RESENT` audit row is persisted.
- [x] `AuthServiceResendOtpTest.java` — align generated test IDs with `OtpVerification.id`, inject the configured OTP lifetime into the manually constructed service, and verify rejected requests do not consume cooldown.
- [x] `ResendOtpRequest.java` — require exactly one nonblank email or phone so the public endpoint cannot combine a victim lookup identifier with an attacker-controlled delivery identifier.
- [x] `AuthServiceImpl.java` — resolve the user and pending OTP before atomically reserving cooldown by stable `user.id` rather than delivery channel; invalidate the old OTP, persist one replacement with the resolved user's matching identifier, deliver only to that stored identifier, and audit using a stable compatible entity target.
- [x] `RateLimitPolicy.java` and resend tests — enforce the 60-second boundary deterministically, prove expiry with a controllable clock, and prove switching between a user's stored phone and email cannot bypass the account cooldown.
- [x] `RateLimitExceededException.java` and `GlobalExceptionHandler.java` — map resend cooldown rejection to the existing error envelope with HTTP 429.
- [x] `SecurityConfig.java` — permit only the exact POST resend endpoint while preserving authentication on the `/api/v1/**` fallback.
- [x] `AuditEligibilityPolicy.java` — persist `OTP_RESENT` and cover the eligibility behavior through integration evidence.
- [x] Maven verification — run from a clean state and record final test/build totals.

**Acceptance Criteria:**
- Given a fresh Backend `target`, when `mvnw clean test` runs, then all main and test sources compile and all tests pass without failures, errors, or skips introduced by this fix.
- Given the test suite succeeds, when `mvnw clean package` runs, then Maven produces the Backend JAR with `BUILD SUCCESS`.
- Given a resend request supplies both email and phone, when it reaches the public endpoint, then it returns 400 without consuming cooldown, changing OTP state, sending a message, or writing a resend audit event.
- Given a valid email or phone resend outside cooldown, when it succeeds, then the prior OTP is retained as used, exactly one distinct replacement remains pending for the resolved stored identifier, delivery uses only that identifier, and exactly one `OTP_RESENT` audit record is persisted.
- Given a valid account is inside cooldown, when resend is requested, then the API returns HTTP 429 with a non-secret error envelope and no OTP mutation or delivery.
- Given one account has both email and phone, when one channel consumes its resend slot and the other channel is requested immediately, then the second request returns HTTP 429 because cooldown is keyed by stable account identity, with no additional OTP, delivery, or audit mutation.
- Given the resend cooldown gate consumes a slot, when any subsequent step in the resend transaction fails before commit, then the rollback must also release the cooldown slot so the user can retry without waiting a full window.
- Given the working tree contained pre-existing changes, when the fix is reviewed, then only authentication test/code and BMAD artifacts required by this bugfix have been changed by Codex.

## Spec Change Log

- **Iteration 1 — multi-review bad-spec loopback:** Review found that opening resend publicly without an exactly-one identifier boundary enabled OTP redirection, the planned audit check stopped at a service call rather than persisted evidence, and cooldown was recorded before user/pending-OTP validation. The code map, tasks, and acceptance criteria now require stored-destination delivery, no mutation for rejected requests, one pending replacement OTP, and a persisted `OTP_RESENT` audit row. **Known-bad state avoided:** a green suite with an exploitable public resend path, discarded audits, or poisoned cooldowns. **KEEP:** retain the verified Spring Boot 4 imports, Long OTP test IDs, real production security chain, narrow POST matcher, typed HTTP 429 mapping, prior-OTP audit history, 52-test baseline, and successful executable-JAR packaging.
- **Human environment update:** The user selected Java 21, installed JDK 21.0.10, and updated `JAVA_HOME`; the frozen runtime constraint was updated accordingly.
- **Iteration 2 — multi-review bad-spec loopback:** Review found the resend slot was keyed by the stored delivery identifier, allowing a dual-identifier account to switch channels and bypass the per-account cooldown. The code map, tasks, and acceptance criteria now require stable `user.id` cooldown keys and cross-channel evidence; timing tests must use a controllable clock. **Known-bad state avoided:** two immediate replacements and deliveries for one account under different channel keys, plus wall-clock-flaky cooldown tests. **KEEP:** preserve exactly-one request validation, stored-destination delivery, validation before cooldown consumption, typed HTTP 429 mapping, retained prior OTP, one replacement/audit on success, public exact-POST security matching, Java 21, and the successful 55-test/JAR baseline.
- **Iteration 3 — multi-review bad-spec loopback (EC-E6):** Review found that `AuthServiceImpl.resendOtp` calls `rateLimitPolicy.tryConsumeResend(...)` before any DB write. If a later step (persist replacement, send, audit) throws, the `@Transactional` rollback discards the DB writes but the in-memory cooldown slot is not released, producing a spurious 429 for up to 60 s after a transient backend error. The I/O & Edge-Case Matrix and Acceptance Criteria now require that any rollback after the cooldown gate also release the cooldown slot. **Known-bad state avoided:** a single transient error converting into a spurious 60 s lockout for an innocent user. **KEEP:** preserve stable `user.id`-keyed cooldown, atomic check-and-record inside `ConcurrentHashMap.compute`, exactly-one identifier validation at both DTO and service layers, prior-OTP invalidation, replacement-row delivery and audit invariants, public exact-POST security matching, the 56-test baseline, and the successful JAR baseline.

## Verification

**Commands:**
- `04_SourceCode/Backend/mvnw.cmd clean test` — expected: `BUILD SUCCESS` with zero failures and zero errors.
- `04_SourceCode/Backend/mvnw.cmd clean package` — expected: `BUILD SUCCESS` and a packaged JAR under `target`.

**Results:**
- `clean test`: `BUILD SUCCESS` on Java 21.0.10; 56 tests, 0 failures, 0 errors, 0 skipped.
- `clean package`: `BUILD SUCCESS` with the same 56 passing tests; executable JAR produced at `target/backend-0.0.1-SNAPSHOT.jar`.

## Suggested Review Order

**Resend flow — entry point and transactional ordering**

- The Iteration 3 hot spot: validation, DB writes, then cooldown consume; cooldown slot is explicitly released on delivery/audit failure so a transient backend error does not poison the in-memory state.
  [`AuthServiceImpl.java:286`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java#L286)
- The new in-method ordering rule: existing OTP is invalidated, replacement row is built and saved, and only then does the cooldown gate run.
  [`AuthServiceImpl.java:316`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java#L316)
- Delivery + audit wrapped in a guarded try so a runtime failure still triggers `resetResend(resendAccountKey)` before the transaction rolls back.
  [`AuthServiceImpl.java:352`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java#L352)

**Cooldown policy — atomic per-account gate**

- `tryConsumeResend` is the only function called by the resend path; it is intentionally atomic via `ConcurrentHashMap.compute` and treats the boundary at exactly 60 s as "elapsed".
  [`RateLimitPolicy.java:240`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java#L240)
- `resetResend` is the explicit release hook used by the new guarded try block above.
  [`RateLimitPolicy.java:286`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java#L286)

**Request boundary — exactly-one identifier**

- DTO-level guard: the `@AssertTrue` rejects both-neither and both-present before the service is ever invoked.
  [`ResendOtpRequest.java:28`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/ResendOtpRequest.java#L28)
- Service-level mirror: same rule re-checked in code so internal callers cannot bypass the DTO validator.
  [`AuthServiceImpl.java:289`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java#L289)

**HTTP error envelope and security wiring**

- Typed rate-limit failure: maps the cooldown rejection to HTTP 429 via the existing error envelope, no secret leakage.
  [`GlobalExceptionHandler.java:64`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#L64)
- Exception carrier: minimal `RuntimeException` subclass with a safe message; never exposes cooldown internals.
  [`RateLimitExceededException.java:3`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/RateLimitExceededException.java#L3)
- Public resend matcher: only the exact POST `/api/v1/auth/resend-otp` is unauthenticated; the rest of `/api/v1/**` still requires auth.
  [`SecurityConfig.java:41`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java#L41)

**Audit eligibility — persistence of the resend event**

- Adds `OTP_RESENT` to the persisted-eligibility set so the audit row lands in the database, not just in the in-memory log.
  [`AuditEligibilityPolicy.java:11`](../../04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/policy/AuditEligibilityPolicy.java#L11)

**Tests — behavioral evidence for the reviewer**

- Resend service unit tests with the new save counts (2 saves on rate-limit path; 4 saves on cross-channel path).
  [`AuthServiceResendOtpTest.java:214`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/service/AuthServiceResendOtpTest.java#L214)
- Boundary behavior over a controllable clock: deterministic 60 s transition and cross-channel cooldown lockout.
  [`RateLimitPolicyResendTest.java:14`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/policy/RateLimitPolicyResendTest.java#L14)
- End-to-end coverage of the public resend endpoint via MockMvc: 400 on mixed identifiers, 200 with exactly one replacement and one audit, 429 inside cooldown.
  [`RegistrationIntegrationTest.java:348`](../../04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java#L348)
