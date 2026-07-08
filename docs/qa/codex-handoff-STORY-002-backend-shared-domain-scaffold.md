# Codex Handoff: STORY-002-backend-shared-domain-scaffold

Status: CODE_COMPLETE_WITH_TEST_ENV_BLOCKER
Story: `docs/stories/STORY-002-backend-shared-domain-scaffold.md`
Plan: `docs/plans/codex/IMPLEMENTATION-PLAN-STORY-002-backend-shared-domain-scaffold.md`
Agent Role: MAIN_DEVELOPER

## Scope Implemented

- Added shared backend scaffold under `common/config`, `common/exception`, `common/response`, `common/constants`, `common/validation`, and `common/util`.
- Added security/auth scaffold under the domain package `security`, including OTP register/login/verify flow, JWT access token generation, refresh token storage, RBAC enum, controller, service, repositories, DTOs, policy, mapper, and Spring Security config.
- Added consent scaffold under the domain package `consent`, including consent grant/revoke/list/check service, controller, DTOs, entity, repository, mapper, and policy.
- Added audit scaffold under the domain package `audit`, including audit/security event entities, repositories, services, mapper, policy, admin audit controller, and service hooks from auth/consent flows.
- Added Flyway migrations V1 through V5 for users/refresh tokens, OTP verification, consent grants, audit logs, and security events.
- Updated backend application config for PostgreSQL, Flyway, JWT, OTP, JPA validation, and stateless security.

## Architecture Compliance

- Preserved `com.carebridge.backend.<domain>` package organization.
- Did not create an `api` package.
- Did not create global controller, service, or repository packages.
- Controllers call services.
- Services use repositories, mappers, and policies.
- Repositories use Spring Data JPA only.
- Entities are not returned directly from controllers.
- MongoDB and microservices were not introduced.

## Auth Flow Compliance

- `POST /api/v1/auth/register` sends OTP for new users.
- `POST /api/v1/auth/login` sends OTP for existing users.
- `POST /api/v1/auth/verify-otp` verifies OTP and returns `accessToken`, `refreshToken`, and `user`.
- `LoginRequest` contains phone only.
- `OtpSendResponse` contains `message` and `expiresIn`.
- `AuthResponse` contains `accessToken`, `refreshToken`, and `user`.

## Validation

Compile:

```text
.\mvnw.cmd -q -DskipTests compile
Result: PASS
```

Existing tests:

```text
.\mvnw.cmd test
Result: FAIL
```

Failure reason:

```text
BackendApplicationTests.contextLoads cannot start the Spring context because PostgreSQL is not available at localhost:5432.
Root cause: org.postgresql.util.PSQLException: Connection to localhost:5432 refused.
```

No test files were created or edited, per the approved plan and user instruction.

## Files Changed

- `04_SourceCode/Backend/pom.xml`
- `04_SourceCode/Backend/src/main/resources/application.yaml`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/BackendApplication.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/config/JacksonConfig.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/config/WebMvcConfig.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/config/SpringDocConfig.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/*`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/response/*`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/constants/*`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/validation/*`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/util/*`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/**`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/**`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/**`
- `04_SourceCode/Backend/src/main/resources/db/migration/V1__create_user_table.sql`
- `04_SourceCode/Backend/src/main/resources/db/migration/V2__create_otp_verification_table.sql`
- `04_SourceCode/Backend/src/main/resources/db/migration/V3__create_consent_grants_table.sql`
- `04_SourceCode/Backend/src/main/resources/db/migration/V4__create_audit_logs_table.sql`
- `04_SourceCode/Backend/src/main/resources/db/migration/V5__create_security_events_table.sql`
- `docs/plans/codex/IMPLEMENTATION-PLAN-STORY-002-backend-shared-domain-scaffold.md`
- `docs/qa/codex-handoff-STORY-002-backend-shared-domain-scaffold.md`

## Blockers For QA

- Local PostgreSQL must be running and reachable at the configured datasource before `.\mvnw.cmd test` can pass.
- STORY-002 requested test coverage, but test creation/editing was explicitly forbidden for this execution step.

## Stop Condition

Codex implementation and handoff are complete. Stop here. Do not start Gemini QA.
