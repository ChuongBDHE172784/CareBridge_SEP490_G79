# IMPLEMENTATION-PLAN-STORY-002-backend-shared-domain-scaffold

Status: DONE
Approval Required: NO
Allowed To Execute: NO

## Agent Role

MAIN_DEVELOPER

## BMAD Skills Used

- bmad-agent-dev
- bmad-dev-story
- bmad-check-implementation-readiness

Use note: this is PLAN_ONLY. No implementation workflow, source edit, test edit, or story edit is allowed until the user approves this plan with:

```text
APPROVED_BY_USER: docs/plans/codex/IMPLEMENTATION-PLAN-STORY-002-backend-shared-domain-scaffold.md
```

## Goal

Create the approved implementation plan for STORY-002 only.

After approval, implement the backend shared scaffold for:

- `common/config`
- `common/exception`
- `common/response`
- `common/constants`
- `common/validation`
- `common/util`
- `security` scaffold
- `consent` scaffold
- `audit` scaffold
- Flyway database migrations for the scaffold tables

This plan preserves the current layered architecture organized by business domain under:

```text
04_SourceCode/Backend/src/main/java/com/carebridge/backend/<domain>/
```

## Input Files

Read during PLAN_ONLY:

- `AGENTS.md`
- `docs/bmad/prd.md`
- `docs/bmad/architecture.md`
- `docs/bmad/coding-standards.md`
- `docs/stories/STORY-002-backend-shared-domain-scaffold.md`
- `04_SourceCode/Backend` backend structure only, checked by file and directory listing

Backend structure checked:

- Backend root exists at `04_SourceCode/Backend`.
- Maven wrapper exists: `04_SourceCode/Backend/mvnw`, `04_SourceCode/Backend/mvnw.cmd`.
- Backend build file exists: `04_SourceCode/Backend/pom.xml`.
- Backend application config exists: `04_SourceCode/Backend/src/main/resources/application.yaml`.
- Main Java package root exists: `04_SourceCode/Backend/src/main/java/com/carebridge/backend`.
- `BackendApplication.java` exists under the main package root.
- Domain-first directories already exist for `audit`, `carejourney`, `common`, `community`, `consent`, `consultation`, `content`, `emergency`, `exercise`, `expert`, `family`, `healthrecord`, `identity`, `integration`, `partner`, `payment`, `reminder`, `safety`, `security`, and `triage`.
- `common` already contains empty scaffold directories: `config`, `constants`, `exception`, `pagination`, `response`, `util`, and `validation`.
- `security` already contains empty scaffold directories: `controller`, `dto/request`, `dto/response`, `entity`, `jwt`, `mapper`, `otp`, `rbac`, `repository`, `service`, and `session`.
- `consent` already contains empty scaffold directories: `controller`, `dto/request`, `dto/response`, `entity`, `mapper`, `policy`, `repository`, and `service`.
- `audit` already contains empty scaffold directories: `controller`, `dto/request`, `dto/response`, `entity`, `mapper`, `policy`, `repository`, and `service`.
- `src/main/resources/db/migration` exists with `.gitkeep`.
- No `api` package was found in the backend file list.
- No global `controller`, `service`, or `repository` package was found outside domain folders in the backend file list.
- No MongoDB package was found in the backend file list.

Structure conflict check:

- No structure-level conflict with `AGENTS.md`, architecture, or STORY-002 was found.
- Source file contents and dependency contents were not inspected beyond the user-approved PLAN_ONLY read scope, so dependency-level conflicts must be checked during approved execution before edits.

## Output Files

Created in PLAN_ONLY:

- `docs/plans/codex/IMPLEMENTATION-PLAN-STORY-002-backend-shared-domain-scaffold.md`

Created or modified only after approval:

- Production source files listed in `Files Allowed To Edit`
- Configuration and migration files listed in `Files Allowed To Edit`
- Codex handoff: `docs/qa/codex-handoff-STORY-002-backend-shared-domain-scaffold.md`

## Files Allowed To Edit

PLAN_ONLY currently allows editing only this plan file.

After approval, edit only the files listed below.

Plan status file:

- `docs/plans/codex/IMPLEMENTATION-PLAN-STORY-002-backend-shared-domain-scaffold.md`

Build and application configuration:

- `04_SourceCode/Backend/pom.xml`
- `04_SourceCode/Backend/src/main/resources/application.yaml`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/BackendApplication.java`

Common config:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/config/JacksonConfig.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/config/WebMvcConfig.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/config/SpringDocConfig.java`

Common exception:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/ResourceNotFoundException.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/ValidationException.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/ConsentException.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/AuthenticationException.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/AuthorizationException.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/AccessDeniedBusinessException.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/RedFlagException.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java`

Common response:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/response/ApiResponse.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/response/PaginatedResponse.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/response/ErrorResponse.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/response/ErrorDetail.java`

Common constants:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/constants/AppConstants.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/constants/SecurityConstants.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/constants/ConsentConstants.java`

Common validation:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/validation/VietnamesePhoneNumber.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/validation/VietnamesePhoneNumberValidator.java`

Common util:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/util/DateUtils.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/util/StringUtils.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/common/util/SecurityUtils.java`

Security config:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java`

Security entity:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/entity/User.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/entity/RefreshToken.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/entity/OtpVerification.java`

Security repository:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/UserRepository.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/RefreshTokenRepository.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/OtpVerificationRepository.java`

Security DTO request:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/LoginRequest.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/VerifyOtpRequest.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/RefreshTokenRequest.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/ChangePasswordRequest.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/request/UpdateProfileRequest.java`

Security DTO response:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/response/AuthResponse.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/response/UserProfileResponse.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/dto/response/OtpSendResponse.java`

Security controller:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/controller/AuthController.java`

Security service:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/AuthService.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/OtpService.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/CustomUserDetailsService.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/OtpServiceImpl.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/CustomUserDetailsServiceImpl.java`

Security policy, JWT, RBAC, OTP, mapper:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/rbac/Role.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/otp/OtpGenerator.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/mapper/UserMapper.java`

Consent entity:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/entity/ConsentGrant.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/entity/ConsentDataType.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/entity/ConsentPurpose.java`

Consent repository:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/repository/ConsentGrantRepository.java`

Consent DTO:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/dto/request/RevokeConsentRequest.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/dto/response/ConsentGrantResponse.java`

Consent controller, service, policy, mapper:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/service/ConsentService.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/policy/ConsentCheckPolicy.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/consent/mapper/ConsentGrantMapper.java`

Audit entity:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/entity/AuditLog.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/entity/SecurityEvent.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/entity/AuditAction.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/entity/SecurityEventType.java`

Audit repository:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/repository/AuditLogRepository.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/repository/SecurityEventRepository.java`

Audit DTO:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/dto/request/AuditQueryRequest.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/dto/response/AuditLogResponse.java`

Audit controller, service, policy, mapper:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/controller/AuditController.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/service/AuditService.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/service/SecurityEventService.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/service/impl/SecurityEventServiceImpl.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/policy/AuditEligibilityPolicy.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/audit/mapper/AuditLogMapper.java`

Database migrations:

- `04_SourceCode/Backend/src/main/resources/db/migration/V1__create_user_table.sql`
- `04_SourceCode/Backend/src/main/resources/db/migration/V2__create_otp_verification_table.sql`
- `04_SourceCode/Backend/src/main/resources/db/migration/V3__create_consent_grants_table.sql`
- `04_SourceCode/Backend/src/main/resources/db/migration/V4__create_audit_logs_table.sql`
- `04_SourceCode/Backend/src/main/resources/db/migration/V5__create_security_events_table.sql`

Handoff:

- `docs/qa/codex-handoff-STORY-002-backend-shared-domain-scaffold.md`

## Files Forbidden To Edit

- `docs/source/`
- `docs/bmad/prd.md`
- `docs/bmad/architecture.md`
- `docs/bmad/coding-standards.md`
- `docs/stories/`
- `05_Testing/`
- `tests/`
- `04_SourceCode/Backend/src/test/`
- `.env`
- credentials
- tokens
- secrets
- any package named `api`
- any global package named `controller`, `service`, or `repository` outside domain folders
- any file not explicitly listed in `Files Allowed To Edit`

Important blocker: test files are forbidden by the current user instruction and AGENTS.md unless separately approved. This plan will run existing tests but will not create or edit test files.

## Step-by-Step Tasks

1. Stop after this plan is created and wait for user approval.
2. After approval, update this plan header to:

```text
Status: APPROVED_BY_USER
Approval Required: NO
Allowed To Execute: YES
```

3. Re-check only the allowed source/config files before editing:
   - confirm current `pom.xml` dependencies
   - confirm current `application.yaml` keys
   - confirm current `BackendApplication.java` annotations
   - confirm no same-named source files already exist with conflicting contents
4. If dependency or source conflicts are found, stop and write the blocker in this plan or the handoff. Do not overwrite unrelated work.
5. Update `pom.xml` only if required dependencies are missing:
   - Spring Boot starter web
   - Spring Boot starter validation
   - Spring Boot starter data JPA
   - Spring Boot starter security
   - PostgreSQL driver
   - Flyway core
   - JJWT API, implementation, and Jackson integration
   - springdoc OpenAPI starter for WebMVC UI, if not already present
   - Lombok or MapStruct only if already used or clearly needed and compatible
6. Update `application.yaml` only with STORY-002 configuration:
   - JWT secret environment placeholder
   - access token expiry: 15 minutes
   - refresh token expiry: 7 days
   - OTP expiry: 300 seconds
   - OTP max attempts: 5
   - Flyway enabled
   - no hardcoded secret values
7. Update `BackendApplication.java` only if needed for JPA auditing and transaction support.
8. Implement common config:
   - `JacksonConfig` for Java time serialization and null handling
   - `WebMvcConfig` for CORS and MVC customization aligned with architecture
   - `SpringDocConfig` for OpenAPI metadata
9. Implement common response and exception foundation:
   - `ApiResponse<T>` with `success`, `data`, `message`, `timestamp`
   - `PaginatedResponse<T>`
   - `ErrorResponse` with `success`, `error`, `message`, `timestamp`, `path`, `details`
   - `ErrorDetail`
   - domain runtime exceptions
   - `GlobalExceptionHandler` mappings required by STORY-002
10. Implement common constants, validation, and utility classes:
   - application, security, and consent constants
   - Vietnamese phone number annotation and validator
   - date, string sanitization, and security principal helpers
11. Implement security scaffold:
   - JPA entities for `User`, `RefreshToken`, `OtpVerification`
   - Spring Data JPA repositories only
   - request/response DTOs
   - `Role` enum
   - `AuthController` under `security/controller`
   - `AuthService`, `OtpService`, and implementations
   - OTP generator and OTP logging mock
   - JWT provider and authentication filter
   - Spring Security config with architecture-approved endpoint rules
12. Enforce auth flow rules:
   - `POST /api/v1/auth/register` sends OTP for new users
   - `POST /api/v1/auth/login` sends OTP for existing users
   - `POST /api/v1/auth/verify-otp` returns `accessToken`, `refreshToken`, and `user`
   - `LoginRequest` contains phone only
   - `OtpSendResponse` contains `message` and `expiresIn`
   - `AuthResponse` contains `accessToken`, `refreshToken`, and `user`
13. Implement consent scaffold:
   - `ConsentGrant` entity and enum values for MVP data types and purposes
   - repository using Spring Data JPA
   - grant, revoke, list, and `ensureConsent` service methods
   - `ConsentCheckPolicy`
   - DTOs and mapper
   - `ConsentController` under `/api/v1/consent/grants`
14. Implement audit scaffold:
   - immutable append-only `AuditLog`
   - `SecurityEvent`
   - action and security event enums
   - repositories using Spring Data JPA
   - audit and security event services
   - `AuditEligibilityPolicy`
   - mapper and admin-only controller under `/api/v1/admin/audit-logs`
15. Create Flyway migrations V1 through V5:
   - user table
   - OTP verification table
   - consent grants table
   - audit logs table
   - security events table
   - include primary keys, constraints, timestamps, and indexes on user/time/status lookup columns
16. Run build and validation commands listed in `Validation Method`.
17. Do not create or edit tests unless the user separately approves test edits.
18. Create Codex handoff at `docs/qa/codex-handoff-STORY-002-backend-shared-domain-scaffold.md`.
19. Update this plan header to:

```text
Status: DONE
Approval Required: NO
Allowed To Execute: NO
```

20. Stop after the handoff. Do not start Gemini QA.

## Package Structure To Use

Use only domain-first packages under `com.carebridge.backend`.

Common shared packages:

```text
com.carebridge.backend.common.config
com.carebridge.backend.common.constants
com.carebridge.backend.common.exception
com.carebridge.backend.common.response
com.carebridge.backend.common.validation
com.carebridge.backend.common.util
```

Security domain packages:

```text
com.carebridge.backend.security.config
com.carebridge.backend.security.controller
com.carebridge.backend.security.dto.request
com.carebridge.backend.security.dto.response
com.carebridge.backend.security.entity
com.carebridge.backend.security.jwt
com.carebridge.backend.security.mapper
com.carebridge.backend.security.otp
com.carebridge.backend.security.policy
com.carebridge.backend.security.rbac
com.carebridge.backend.security.repository
com.carebridge.backend.security.service
com.carebridge.backend.security.service.impl
```

Consent domain packages:

```text
com.carebridge.backend.consent.controller
com.carebridge.backend.consent.dto.request
com.carebridge.backend.consent.dto.response
com.carebridge.backend.consent.entity
com.carebridge.backend.consent.mapper
com.carebridge.backend.consent.policy
com.carebridge.backend.consent.repository
com.carebridge.backend.consent.service
com.carebridge.backend.consent.service.impl
```

Audit domain packages:

```text
com.carebridge.backend.audit.controller
com.carebridge.backend.audit.dto.request
com.carebridge.backend.audit.dto.response
com.carebridge.backend.audit.entity
com.carebridge.backend.audit.mapper
com.carebridge.backend.audit.policy
com.carebridge.backend.audit.repository
com.carebridge.backend.audit.service
com.carebridge.backend.audit.service.impl
```

Forbidden package structures:

- `com.carebridge.backend.api`
- `com.carebridge.backend.controller`
- `com.carebridge.backend.service`
- `com.carebridge.backend.repository`
- any MongoDB package or repository
- any microservice split

## Layered Architecture Impact

- Controllers stay inside each business domain.
- Controllers call services only.
- Services contain business workflows and call repositories, mappers, policies, and integration adapters only when needed.
- Repositories extend Spring Data JPA repositories only.
- Entities stay inside domain `entity` packages and are never returned directly from controllers.
- DTOs stay inside domain `dto/request` and `dto/response` packages.
- Common code is limited to cross-cutting concerns and must not become a global business domain.
- Security endpoint configuration stays in `security/config/SecurityConfig.java`, not in an `api` package.
- Consent checks are exposed by the consent service/policy for later domains to call.
- Audit is append-only and designed for service-level sensitive action logging.

## Requirement Traceability

- STORY-002 2.1 Common Utilities:
  - maps to common config, exception, response, constants, validation, and util files listed above.
- STORY-002 2.2 Security Domain:
  - maps to `security` entity, repository, DTO, controller, service, policy, JWT, RBAC, OTP, and config files listed above.
- STORY-002 auth flow rules:
  - maps to `RegisterRequest`, `LoginRequest`, `VerifyOtpRequest`, `OtpSendResponse`, `AuthResponse`, `AuthController`, `AuthServiceImpl`, `OtpServiceImpl`, and `JwtTokenProvider`.
- STORY-002 2.3 Consent Domain:
  - maps to consent entity, repository, DTO, controller, service, policy, and mapper files listed above.
- STORY-002 2.4 Audit Domain:
  - maps to audit entity, repository, DTO, controller, service, policy, and mapper files listed above.
- STORY-002 2.5 Database Migrations:
  - maps to V1 through V5 Flyway migration files listed above.
- PRD AUTH-001 through AUTH-004:
  - maps to OTP register/login/verify, JWT access/refresh token handling, profile response basics, and role enum.
- PRD CONSENT-001 through CONSENT-003:
  - maps to consent grant, revoke, expiry, scope, and `ensureConsent`.
- PRD AUDIT-001 through AUDIT-003:
  - maps to audit log and security event services/entities and admin audit query scaffold.
- Architecture backend layered rules:
  - maps to domain-first package structure and controller-service-repository dependency direction.
- Coding standards:
  - maps to Java package naming, DTO validation, JPA repository use, exception handling, logging safety, and transaction boundaries.

## Validation Method

Run from:

```text
04_SourceCode/Backend
```

Build and compile:

```powershell
.\mvnw.cmd -q -DskipTests compile
```

Run existing tests only:

```powershell
.\mvnw.cmd test
```

Optional local application smoke check, only if local database configuration is available:

```powershell
.\mvnw.cmd spring-boot:run
```

Manual smoke targets after the app starts:

- `POST /api/v1/auth/register` with a new phone sends OTP and returns `OtpSendResponse`.
- `POST /api/v1/auth/login` with existing phone only sends OTP and returns `OtpSendResponse`.
- `POST /api/v1/auth/verify-otp` returns `AuthResponse` with `accessToken`, `refreshToken`, and `user`.
- `POST /api/v1/consent/grants` creates a consent grant.
- `GET /api/v1/consent/grants` lists current user's consent grants.
- `DELETE /api/v1/consent/grants/{id}` revokes consent.
- `GET /api/v1/admin/audit-logs` is admin-only and paginated.
- Flyway applies V1 through V5 without migration errors.

Validation limitation:

- New unit or integration tests are required by the story validation approach but are forbidden by the current instruction unless the user explicitly approves test edits.

## Risks

- `pom.xml` contents were not inspected during PLAN_ONLY due the user-approved read scope; dependency additions may need adjustment during approved execution.
- `application.yaml` contents were not inspected during PLAN_ONLY due the user-approved read scope; configuration conflicts may appear during approved execution.
- JWT dependency or Spring Security version mismatch may require small dependency/version adjustments in `pom.xml`.
- Migration ordering V1 through V5 may conflict if existing Flyway migrations appear before approved execution.
- Adding Spring Security can cause existing endpoints to become protected; allowed endpoint rules must be configured carefully.
- OTP mock logging must not expose real OTP in production profiles.
- Audit append-only behavior must avoid update/delete service methods for audit rows.
- Test coverage cannot be expanded under the current no-test-edit restriction.

## Blockers

- BLOCKER: test file edits are forbidden by the current request and AGENTS.md unless separately approved, while STORY-002 validation asks for unit and integration tests.
- POTENTIAL BLOCKER: dependency/config conflicts may exist in `pom.xml` or `application.yaml`, but file contents were not inspected in PLAN_ONLY.
- POTENTIAL BLOCKER: if same-named source files already exist by the time approval is given, implementation must stop before overwriting unrelated work.

## Stop Condition

Stop now after creating this plan. Wait for user approval.

After approval:

- Stop if any required file conflicts with docs, architecture, coding standards, or existing code structure.
- Stop if implementation would require editing a forbidden file.
- Stop if implementation requires tests to be created or changed without explicit test-edit approval.
- Stop if a new `api` package, global layer package, MongoDB, or microservice structure would be required.
- Stop after creating `docs/qa/codex-handoff-STORY-002-backend-shared-domain-scaffold.md`.
- Do not start Gemini QA.
