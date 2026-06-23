# Claude Coding Task — Epic 1 Security Fixes Round 2

## Objective

Close the remaining Epic 1 authentication/session blockers found while reviewing commit `0dfcc2b`. Preserve the five valid fixes already present, repair the incomplete token/session lifecycle, validate the V1-only Flyway schema, and add tests that exercise production-shaped behavior.

## Repository and Workflow Constraints

- Work on branch `PhuongNT` from commit `0dfcc2b` or its latest descendant.
- Read `AGENTS.md`, `_bmad-output/project-context.md`, the six Epic 1 story files, and `04_Implement/implement_artifacts/bao-cao-cong-viec-epic-1-security-fixes.md` before editing.
- Do not read, print, modify, stage, or commit `.env` or any real secrets.
- Preserve unrelated working-tree changes, especially deleted files under `03_Design/Architecture/`.
- Java 21, Spring Boot 4.1, domain-first packages, constructor injection, typed exceptions, UTC `Instant` semantics.
- Flyway remains the schema source of truth. User decision: V4/V5 were never applied and every database may be reset, so a single corrected `V1__init_schema.sql` is authorized.
- Do not weaken security configuration or production behavior merely to make tests pass.

## Required Implementation

### 1. Establish one authoritative session identity

Use a stable session identifier (`sid`) to connect access JWTs, refresh tokens, and `UserSession`.

- Add a `sid` claim to access JWTs.
- A new login/token-issuing flow must create a `UserSession` first, then issue an access JWT containing that session ID.
- OTP activation/login paths that issue tokens must also create a `UserSession`; they may not produce untracked refresh credentials.
- Refresh rotation must retain the same session ID and atomically replace the session's refresh-token hash and expiry.
- `JwtAuthenticationFilter` must expose the authenticated session ID in a typed, documented way usable by controllers/services. Do not depend on credentials being the raw JWT unless that contract is explicitly implemented and tested.
- `getActiveSessions()` must mark `isCurrent` by comparing the request `sid` with `UserSession.sessionId`, not by comparing access-token text with a refresh-token hash.
- Revoking the current session through the remote-revoke endpoint must be rejected using `sid`.

### 2. Make logout and remote revoke actually invalidate credentials

- In `SessionServiceImpl.logout`, hash the raw refresh token with `TokenUtils.hashSha256()` before querying `refreshTokenHash`.
- Treat blank tokens consistently with null tokens.
- Verify the located session belongs to the authenticated user before mutation.
- Revoke the `UserSession`, add its refresh-token hash to `TokenBlacklist`, and revoke the matching authoritative refresh credential in one transaction where possible.
- Repeated logout remains idempotent.
- Remote session revoke must atomically transition only an active, non-revoked session. Include `revoked = false` in the modifying query predicate.
- A revoked session must be rejected immediately by authenticated API requests through the `sid` session check, not merely after access-token expiry.

### 3. Make refresh rotation fail closed

- `AuthServiceImpl.refresh()` is already transactional through the class-level `@Transactional`; preserve this.
- Hash the incoming token once and use that canonical hash for session/blacklist checks.
- Reject when no matching active session exists. Remove the current fail-open comment/behavior.
- Reject revoked, expired, inactive, or otherwise non-active sessions using a typed status or one canonical lowercase representation.
- Treat `expiresAt == now` as expired (`!expiresAt.isAfter(now)`). Capture one `Instant now` per operation.
- Keep the pessimistic lock on the refresh-token row.
- During rotation, revoke the old refresh row and atomically update the matching `UserSession` with the new hash and expiry before returning the new token.
- The old raw token, old hash, deleted/missing session, and blacklisted session must never mint another token pair.

### 4. Finish secret and hashing hardening

- Keep `application.yaml` as `${JWT_SECRET}` without a repository fallback.
- Change `JwtTokenProvider.init()` to throw a clear startup exception for null, blank, whitespace-only, or insufficient-strength secrets. It must not generate a process-local random key.
- Keep `TokenUtils` as the canonical SHA-256 utility for token/OTP hash compatibility in this round.
- Refactor `OtpServiceImpl` to use `TokenUtils`; remove duplicate `MessageDigest`/`HexFormat` hashing.
- Remove plaintext OTP values from INFO logs and `System.out`. Development delivery may log only masked destination, purpose, expiry, and correlation identifiers.
- Preserve constant-time comparisons.

### 5. Correct the authorized V1-only Flyway schema

Because all databases may be reset and V4/V5 were never applied, keep only `V1__init_schema.sql`, but make it production-safe.

For `user_sessions` require:

- `session_id NOT NULL PRIMARY KEY`
- `user_id NOT NULL` with FK to `users(user_id)`
- `refresh_token_hash NOT NULL` and unique
- `expires_at NOT NULL`
- `is_current NOT NULL DEFAULT FALSE`
- `revoked NOT NULL DEFAULT FALSE`
- canonical constrained status representation
- indexes equivalent to the removed security migrations: user ID, last activity, refresh-token hash, and `(user_id, revoked)`

For `token_blacklist` require:

- unique `token_hash`
- non-null expiry/revocation timestamps
- index on `expires_at`

Also add a unique constraint/index for non-null `users.phone`, matching the repository's single-result lookup contract. Do not attempt to solve every cross-domain FK in this task.

### 6. Use typed error handling

- Do not use exception-message string comparisons as controller routing logic.
- Add/reuse typed domain exceptions for session-not-found, current-session revoke, ownership violation, revoked session, and invalid refresh token.
- Map them through `GlobalExceptionHandler` to stable API status/code contracts without leaking account/token existence.

## Mandatory Tests

Existing 86 tests passing is necessary but insufficient. Add tests that fail against `0dfcc2b` and pass only after the fixes.

### Unit/service tests

- Logout hashes the raw refresh token before repository lookup.
- Logout revokes session, writes blacklist, revokes refresh credential, and is idempotent.
- Logout rejects a token belonging to another user without mutation.
- Refresh rejects blacklisted, missing-session, revoked-session, expired-session, and inactive-session tokens.
- Refresh rotation updates the same `UserSession` with the new hash/expiry and rejects the old token.
- JWT provider rejects missing, blank, whitespace-only, and weak secrets.
- Current-session determination uses `sid`, not token-string comparison.

### Controller/security integration tests

- Authenticated `GET /api/v1/sessions` works with the real JWT principal shape.
- Remote revoke cannot revoke the caller's current `sid`.
- Revoked session access JWT receives 401 on the next protected request.
- Logout with a valid refresh token makes subsequent refresh fail.
- Unauthorized and malformed-token paths return the documented structured errors.

### OTP concurrency test

- Use two independent transactions/threads and synchronization barriers against the same OTP.
- Exactly one valid verification may complete and issue tokens.
- Concurrent invalid attempts must not lose decrements or exceed the maximum attempt count.
- Do not place the entire concurrency test inside one shared class-level transaction.

### PostgreSQL/Flyway validation

- Execute Flyway V1 against a clean PostgreSQL database, then start JPA with `ddl-auto=validate`.
- Verify the security constraints/indexes above through metadata or failing inserts.
- H2-only `create-drop` is not acceptable evidence for this gate. Prefer a reproducible Testcontainers PostgreSQL integration test or an equivalent CI-safe PostgreSQL fixture.

## Acceptance Criteria

- AC1: All original tests plus new targeted tests pass.
- AC2: `mvnw.cmd clean package` succeeds.
- AC3: Logout no longer succeeds as a no-op for a valid raw refresh token.
- AC4: Refresh cannot succeed without one matching active `UserSession`.
- AC5: Rotation updates the existing session and the old token cannot be reused.
- AC6: Access JWTs carry `sid`; session listing/current-session protection works through real security wiring.
- AC7: OTP verification is single-use under concurrent requests.
- AC8: Missing/blank JWT secret fails application startup.
- AC9: No plaintext OTP is written to logs/stdout.
- AC10: Clean PostgreSQL Flyway V1 migration succeeds and Hibernate validation passes.
- AC11: `user_sessions`, `token_blacklist`, and users-phone constraints/indexes match this task.
- AC12: No unrelated files or secrets are modified or committed.

## Required Delivery Evidence

- File list and concise rationale per change.
- Exact commands executed and results.
- Test count, failures/errors/skips.
- PostgreSQL/Flyway validation output.
- Explicit note confirming `.env` and unrelated `03_Design/Architecture` deletions were untouched.
- Any remaining limitation must be stated; do not mark the task complete with an unverified security path.

## Suggested Commit

`fix(security): close Epic 1 token lifecycle and migration gaps`
