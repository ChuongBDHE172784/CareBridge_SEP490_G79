# Sprint 0 TV1 Code Implementation Workflow

| Field | Value |
|---|---|
| Document ID | `CB-TV1-WF-001` |
| Version | `1.0` |
| Status | `Draft - Pending Team Approval` |
| Scope | Sprint 0 - TV1 Shared Foundation Skeleton |
| Owner | TV1 - Shared Foundation |
| Last Updated | 2026-06-21 |

## 1. Purpose

This document is the single execution runbook for implementing Sprint 0 TV1. It combines requirements traceability, technical design, test-first development, implementation, review, and quality-gate activities.

The workflow applies to the following TV1 foundation areas:

- Shared API response and exception contracts.
- Authentication, OTP, JWT, refresh token, and session skeletons.
- Account/profile and role/permission foundations.
- Privacy and consent decision contracts.
- Notification and audit event contracts.
- Shared web and mobile authentication client conventions.

Sprint 0 establishes stable boundaries and contracts. Complete end-to-end features remain assigned to later sprints:

- Sprint 1: authentication and account basics.
- Sprint 2: sessions, privacy settings, and notifications.
- Sprint 3: audit/security administration and role operations.
- Sprint 4: production-like OTP, notification, and security providers.

## 2. Authoritative Inputs

Every implementation package must use the following sources. When sources conflict, implementation must stop until an ADR resolves the conflict.

| Source | Purpose |
|---|---|
| `01_Requirements/SRS/Functional-Specifications.md` | Functional behavior and business rules |
| `02_Design/Architecture/function-spec-task-allocation.md` | Sprint and TV ownership boundaries |
| `02_Design/Architecture/project-structure-design.md` | Approved module and package structure |
| `02_Design/Database/CareBridge_ERD_Description_and_Data_Dictionary_Updated.docx.md` | Authoritative data model |
| `02_Design/Database/CareBridge_ERD_Logical_Model_Updated.puml` | Logical relationships |
| `_bmad-output/project-context.md` | Mandatory implementation and quality rules |
| `_bmad-output/template/PHASE-3_TDS.md` | Technical Design Specification template |
| `_bmad-output/template/PHASE-4_Test-Spec.md` | Test-Driven Development Specification template |

Relevant functional specifications:

- `3.1.1.1` Register Account.
- `3.1.1.2` Verify OTP.
- `3.1.1.3` Login.
- `3.1.1.4` Logout.
- `3.1.1.8` View Account Profile.
- `3.1.1.9` Update Account Profile.
- `3.1.1.16` Manage Own Sessions.
- `3.1.4.1` Manage Privacy Settings.
- `3.1.5.1`-`3.1.5.4` Receive Notifications and Emergency Alerts.
- `3.2.5.1` Investigate Security Incident.
- `3.2.5.2` Review Security Event.

## 3. Approved Module Ownership

Use the existing domain map. Do not create duplicate modules with synonymous names.

| Capability | Owning Backend Package |
|---|---|
| Authentication, OTP, JWT, authorization | `security` |
| Account, profile, role, session, notification entities | `identity` |
| Consent, data permission, privacy decisions | `consent` |
| Audit logs and security events | `audit` |
| Cross-cutting response, error, configuration | `common` |

All other domains must consume TV1 interfaces and contracts rather than modifying shared internals.

## 4. Work Package Breakdown

Implementation is divided into independently reviewable work packages. Complete them in order.

| ID | Work Package | Function Specs liên quan | Depends On | Primary Output |
|---|---|---|---|---|
| WP01 | Schema and Identity Foundation | `3.1.1.1`, `3.1.1.2`, `3.1.1.3`, `3.1.1.4`, `3.1.1.8`, `3.1.1.9`, `3.1.1.16`, `3.1.4.1`, `3.1.5.1`-`3.1.5.4`, `3.2.5.1`, `3.2.5.2` | None | Aligned Flyway migrations and entities |
| WP02 | API Response, Error, and Security Contracts | `3.1.1.1`, `3.1.1.2`, `3.1.1.3`, `3.1.1.4`, `3.1.1.8`, `3.1.1.9`, `3.1.1.16`, `3.1.4.1`, `3.1.5.1`-`3.1.5.4`, `3.2.5.1`, `3.2.5.2` | WP01 | Stable response/error/RBAC contracts |
| WP03 | Authentication and Session Foundation | `3.1.1.1`, `3.1.1.2`, `3.1.1.3`, `3.1.1.4`, `3.1.1.8`, `3.1.1.9`, `3.1.1.16` | WP01, WP02 | Auth/session service skeleton |
| WP04 | Privacy, Notification, and Audit Ports | `3.1.4.1`, `3.1.5.1`, `3.1.5.2`, `3.1.5.3`, `3.1.5.4`, `3.2.5.1`, `3.2.5.2` | WP01, WP02 | Typed cross-domain interfaces/events |
| WP05 | Web/Mobile Shared Clients and Contracts | `3.1.1.1`, `3.1.1.2`, `3.1.1.3`, `3.1.1.4`, `3.1.1.8`, `3.1.1.9`, `3.1.1.16`, `3.1.4.1`, `3.1.5.1`-`3.1.5.4`, `3.2.5.1`, `3.2.5.2` | WP02, WP03, WP04 | Client conventions and contract artifacts |

Việc Function Spec xuất hiện ở nhiều work package là có chủ đích: WP01 cung cấp persistence foundation, WP02 cung cấp cross-cutting contracts, WP03/WP04 sở hữu behavior tương ứng, và WP05 cung cấp client/contract integration. Một behavior chỉ được implement tại work package sở hữu nó.

Each work package must have one approved TDS and one approved Test Spec before production implementation begins.

## 5. Required Artifacts and Locations

### 5.1 Technical Design Specifications

Create each document from `_bmad-output/template/PHASE-3_TDS.md`:

```text
02_Design/TechnicalDesign/TV1/
├── CB-TV1-WP01-Schema-Identity-TDS.md
├── CB-TV1-WP02-API-Security-TDS.md
├── CB-TV1-WP03-Auth-Session-TDS.md
├── CB-TV1-WP04-Privacy-Notification-Audit-TDS.md
└── CB-TV1-WP05-Shared-Clients-TDS.md
```

### 5.2 Test Specifications

Create each document from `_bmad-output/template/PHASE-4_Test-Spec.md`:

```text
05_Testing/TestSpecifications/TV1/
├── CB-TV1-WP01-Schema-Identity-Test-Spec.md
├── CB-TV1-WP02-API-Security-Test-Spec.md
├── CB-TV1-WP03-Auth-Session-Test-Spec.md
├── CB-TV1-WP04-Privacy-Notification-Audit-Test-Spec.md
└── CB-TV1-WP05-Shared-Clients-Test-Spec.md
```

### 5.3 Shared Contract Artifacts

```text
04_SourceCode/Contracts/
├── openapi/carebridge-api.yaml
├── events/notification-events.schema.json
├── events/audit-events.schema.json
├── permissions/rbac-matrix.md
├── permissions/consent-scope-catalog.md
└── messages/error-code-catalog.md
```

### 5.4 Test Evidence

```text
05_Testing/Evidence/TV1/<WP-ID>/
├── red-gate-evidence.log
├── test-results.log
├── build-results.log
└── traceability-report.md
```

Do not commit secrets, tokens, real OTP values, production database dumps, or real personal/health data as evidence.

## 6. Agents and Skills

Use a fresh context window for each major skill or agent handoff.

| Stage | Agent/Skill | Responsibility |
|---|---|---|
| Architecture decision | Winston / `bmad-agent-architect` | Resolve schema, ownership, security, and interface decisions |
| Implementation spec | `bmad-spec` or `bmad-create-story` | Produce actionable, testable work-package specification |
| Test design | Murat / `bmad-testarch-test-design` | Derive test conditions and risk coverage |
| Red-phase tests | `bmad-testarch-atdd` | Generate acceptance tests before production code |
| Implementation | Amelia / `bmad-dev-story` or `bmad-quick-dev` | Implement only approved scope |
| Code review | `bmad-code-review` | Adversarial implementation and acceptance review |
| Test quality review | `bmad-testarch-test-review` | Review test quality and anti-patterns |
| Final traceability | `bmad-testarch-trace` | Map requirements to tests and issue the quality gate |

Recommended sequence:

```text
Architect
  -> Spec/Story
  -> Test Design
  -> ATDD Red Gate
  -> Development
  -> Code Review
  -> Test Review
  -> Traceability Gate
```

## 7. End-to-End Execution Workflow

### Gate G0 - Workspace and Input Readiness

Before planning a work package:

- [ ] Confirm the current branch is appropriate for TV1 work.
- [ ] Commit or stash unrelated changes.
- [ ] Confirm source documents are available and versioned.
- [ ] Read `_bmad-output/project-context.md`.
- [ ] Identify the exact functional specification IDs in scope.
- [ ] Identify upstream dependencies and downstream consumers.
- [ ] Confirm that the work package is independently reviewable.

Stop when the worktree contains overlapping uncommitted changes whose ownership is unclear.

### Phase 1 - Architecture Decision

The architect inspects requirements, approved architecture, ERD, current migrations, entities, and contracts.

At minimum, WP01 must resolve:

- UUID versus `BIGSERIAL` as the authoritative identifier strategy.
- Forward migration from V1-V5 without editing applied migrations.
- Ownership of `User`, `Role`, `UserRole`, and `UserSession`.
- Canonical session and refresh-token persistence model.
- Hashing, expiry, revocation, replay, and concurrency rules.
- Entity-to-migration alignment and rollback constraints.

Record material decisions as ADR entries in the TDS. An ADR must include context, options, decision, consequences, and compliance/security impact.

#### Gate G1 - Architecture Approved

- [ ] Every unresolved conflict has an accepted ADR.
- [ ] No placeholder or `TBD` remains on a code-affecting decision.
- [ ] PII/security impact is recorded.
- [ ] DPO sign-off is recorded when required by the team governance model.
- [ ] The responsible architect marks the decision `Accepted`.

### Phase 2 - Technical Design Specification

Copy the Phase 3 TDS template for the current work package and complete all applicable sections.

Required TDS content:

- Module purpose, scope, exclusions, and data classification.
- Requirement-to-code traceability matrix.
- ADRs and invariants.
- NFRs with measurable targets; do not copy sample SLA values without approval.
- Static model using Java/JPA/Flyway terminology.
- Happy-path, failure-path, and security sequence diagrams.
- State machines for OTP, session, consent, notification, or audit states.
- Published and consumed event catalog.
- Java service/repository interfaces and DTO contracts.
- HTTP endpoint and OpenAPI contract.
- Error-code catalog.
- Step-by-step implementation plan.
- Forward migration, deployment, rollback, and incident procedures.
- Authorization matrix.
- Verification commands and expected results.

#### CareBridge Adaptation Rules for the TDS Template

- Replace Prisma examples with JPA entities and Flyway SQL.
- Replace TypeScript service examples with Java 17 interfaces and DTOs for backend work.
- Use `Instant` for persisted/API timestamps unless a domain requirement says otherwise.
- Use `jakarta.*`, constructor injection, Spring Data JPA, and domain-first packages.
- Do not assume Redis, MongoDB, or another technology unless approved by an ADR.
- Mark a non-applicable section `N/A` with a reason; do not silently remove it.
- Legal/compliance references must be verified; template examples are not project facts.

#### Gate G2 - TDS Approved

- [ ] Requirements map to concrete files/components.
- [ ] API, data, event, and error contracts agree.
- [ ] Migration and entity definitions agree.
- [ ] Security, privacy, and audit rules are explicit.
- [ ] Rollback and verification steps are executable.
- [ ] TDS status is `Approved`.

Production code must not begin before G2 passes.

### Phase 3 - Test Specification

Copy the Phase 4 Test Spec template for the current work package. Derive tests from the approved TDS, SRS, ADRs, business rules, and project context.

Required Test Spec content:

- Feature/work-package identification.
- Logic discrepancies between source documents and current code/schema.
- Test scope by domain, service, repository, controller, security, and integration layers.
- Test conditions and coverage mapping.
- Boundary-value, state-transition, equivalence-partition, and error-guessing techniques.
- Synthetic test fixtures with no real PII.
- Functional, validation, authorization, concurrency, replay, and failure cases.
- Test file path for every test case.
- Oracle source for every expected result.
- Entry, suspension, and exit criteria.
- Red-Green-Refactor tracker.

#### CareBridge Adaptation Rules for the Test Template

- Backend test files use `*Test.java` under `04_SourceCode/CamBridgeAPI/src/test/java`.
- Backend commands use Maven Wrapper, not `npm test`.
- Spring MVC/security tests use the Spring Boot test dependencies already configured.
- Persistence tests must use PostgreSQL-compatible behavior; do not assume H2 equivalence.
- Testcontainers is optional and must be introduced coherently if selected.
- Frontend currently has no test runner; use lint/build until a runner is deliberately added.
- Mobile tests use `flutter_test` and `*_test.dart`.
- Replace Prisma/Redis/NestJS examples with the actual CareBridge stack.
- Do not use destructive Git commands as rollback instructions.

#### Gate G3 - Test Spec Approved

- [ ] Every acceptance criterion maps to at least one test.
- [ ] Security-sensitive paths include unauthenticated, unauthorized, replay, expiry, and ownership cases.
- [ ] Migration tests cover both a clean database and an existing V1-V5 database.
- [ ] Test fixtures are isolated and contain no real PII or secrets.
- [ ] Every expected result cites a requirement, ADR, or approved contract.
- [ ] Test Spec status is `Approved`.

### Phase 4 - Red Gate

Write automated tests before implementing new production behavior.

1. Create compilable interfaces/contracts or a minimal throwing stub when needed.
2. Write the tests specified by the approved Test Spec.
3. Run the targeted test suite.
4. Confirm the tests fail for the intended missing behavior.
5. Record command, timestamp, failure summary, and test IDs in `red-gate-evidence.log`.

The Red Gate fails when:

- A test passes against an empty or throwing implementation.
- Failure is caused by a broken import, invalid test configuration, or unavailable unrelated dependency.
- Expected results are based on an AI assumption instead of an approved source.
- Tests share mutable state or depend on execution order.

#### Gate G4 - Red Confirmed

- [ ] Each new behavior has a meaningful failing test.
- [ ] All failures are caused by missing/incorrect production behavior.
- [ ] Tests compile and required imports resolve.
- [ ] Red evidence is saved.

### Phase 5 - Implementation and Green Gate

The developer receives only the approved TDS, approved Test Spec, relevant source documents, and project context.

Implementation rules:

- Implement work-package tasks in dependency order.
- Keep controllers limited to HTTP mapping, validation, authentication context, and response wrapping.
- Keep business workflows and transactions in services.
- Never return JPA entities from controllers.
- Route errors through the shared exception contract.
- New `/api/v1/**` endpoints are authenticated unless explicitly approved as public.
- Enforce consent at the service boundary before protected-data access.
- Keep audit records append-only and sanitized.
- Never log or expose OTPs, token values, password hashes, JWT secrets, or sensitive payloads.
- Add forward Flyway migrations; never edit an applied migration.
- Preserve unrelated user changes and avoid broad formatting rewrites.

After the minimum implementation:

1. Run targeted tests until green.
2. Run the affected module suite.
3. Refactor without changing behavior.
4. Rerun tests after refactoring.
5. Update the Red-Green-Refactor tracker with evidence or commit references.

#### Gate G5 - Implementation Green

- [ ] All work-package tests pass.
- [ ] No test is skipped or weakened to achieve green.
- [ ] Implementation matches the approved contracts.
- [ ] Entity and migration definitions remain aligned.
- [ ] No new secret or sensitive data is committed/logged.

### Phase 6 - Code and Test Review

Run `bmad-code-review` after implementation. Review findings are triaged into:

- Must Fix: correctness, security, data integrity, acceptance failure, migration risk.
- Should Fix: maintainability, resilience, test weakness, contract clarity.
- Follow-up: valid work explicitly outside the current approved scope.

Review focus for TV1:

- JWT/authentication failure handling.
- OTP expiry, attempt limits, and single-use behavior.
- Refresh-token rotation, hashing, replay, and concurrent requests.
- Public endpoint matcher scope.
- Role and permission expansion.
- Consent fail-closed behavior.
- Audit append-only behavior and payload sanitization.
- Notification idempotency and sensitive-payload minimization.
- API response/error consistency across Spring MVC and Spring Security.

Run `bmad-testarch-test-review` after code review fixes to detect weak assertions, duplicated tests, order dependency, missing boundaries, and Green-from-Birth tests.

#### Gate G6 - Review Approved

- [ ] All Must Fix findings are resolved and retested.
- [ ] Should Fix findings are resolved or explicitly deferred with an owner.
- [ ] Tests still match the approved Test Spec.
- [ ] Code review and test review are approved.

### Phase 7 - Full Verification

Run validation from each affected application directory.

Backend:

```powershell
cd 04_SourceCode/CamBridgeAPI
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Frontend:

```powershell
cd 04_SourceCode/CareBridgeWebApp
npm run lint
npm run build
```

Mobile:

```powershell
cd 04_SourceCode/CareBridgeMobileApp
dart format lib test
flutter test
flutter analyze
```

Database verification must include:

- A clean PostgreSQL database applying all migrations.
- A PostgreSQL database already at V1-V5 applying forward migrations.
- Hibernate schema validation.
- Key constraints, indexes, and foreign-key checks.
- Rollback/runbook review; do not perform destructive production rollback tests.

Environment failures must be reported separately from code failures. Do not weaken Flyway, JPA validation, security, or production behavior to make tests pass.

### Phase 8 - Traceability and Final Quality Gate

Run `bmad-testarch-trace` and produce a traceability report containing:

```text
Functional Spec ID
  -> TDS section / ADR
  -> Production code file
  -> Test Spec condition
  -> Automated test ID/file
  -> Verification evidence
```

#### Gate G7 - Definition of Done

- [ ] TDS and Test Spec are `Approved` and changelogs are current.
- [ ] All acceptance criteria are implemented.
- [ ] All Red-Green-Refactor evidence is recorded.
- [ ] Backend tests/package pass when backend is affected.
- [ ] Frontend lint/build pass when frontend is affected.
- [ ] Mobile test/analyze pass when mobile is affected.
- [ ] OpenAPI, event schemas, RBAC matrix, consent scopes, and error catalog match code.
- [ ] Traceability has no unexplained gaps.
- [ ] No unresolved Must Fix review finding remains.
- [ ] Migration, configuration, environment-variable, and security changes are documented.
- [ ] PR includes story/spec link, affected modules, test evidence, and screenshots where applicable.

## 8. Git and Pull Request Rules

Use one small PR per work package or independently reviewable contract change. Shared contracts should merge before dependent domain implementations.

Commit format:

```text
type(scope): imperative summary
```

Examples:

```text
feat(identity): add TV1 session foundation
fix(security): reject refresh-token replay
docs(tv1): add WP03 TDS and test specification
test(audit): cover append-only event behavior
```

Every PR must state:

- Functional specification IDs.
- TDS and Test Spec paths.
- Affected backend/frontend/mobile/contracts modules.
- Database migrations and rollback considerations.
- Configuration or environment-variable changes.
- Security/privacy impact.
- Validation commands and results.
- Deferred findings and owners.

## 9. Standard Agent Prompts

### Architect Prompt

```text
Review Sprint 0 TV1 work package <WP-ID>. Use the SRS functional specifications,
task allocation, project structure design, ERD data dictionary, existing code,
migrations, project-context.md, and PHASE-3_TDS.md. Create the work-package TDS,
resolve every code-affecting conflict through an ADR, adapt examples to Java 17,
Spring Boot, JPA, Flyway, React, and Flutter, and stop for approval before coding.
```

### Test Architect Prompt

```text
Create the Test Spec for <WP-ID> from the approved TDS using
PHASE-4_Test-Spec.md. Adapt the template to Spring Boot/JUnit, PostgreSQL,
React lint/build, and Flutter tests. Define requirement-backed test oracles,
security boundaries, synthetic fixtures, and Red Gate evidence requirements.
Do not generate production implementation.
```

### Developer Prompt

```text
Implement only <WP-ID> from the approved TDS and Test Spec. Read
project-context.md first. Verify meaningful Red Gate evidence before changing
production behavior. Follow Red-Green-Refactor, preserve unrelated changes,
run all affected validation commands, and update traceability and evidence.
Stop if an implementation decision is missing from the approved TDS/ADR.
```

### Reviewer Prompt

```text
Review <WP-ID> against its approved TDS, Test Spec, project-context.md, and Git
diff. Prioritize security, authorization, consent, token replay, entity-migration
alignment, audit immutability, sensitive-data exposure, contract compatibility,
and missing acceptance coverage. Report actionable findings with file locations.
```

## 10. Stop Conditions

Stop implementation and return to the responsible owner when any of the following occurs:

- Authoritative documents disagree without an accepted ADR.
- An applied migration would need to be edited.
- Schema conversion risks data loss without an approved migration/runbook.
- A public endpoint, role, permission, privacy scope, or audit payload is ambiguous.
- Required TDS or Test Spec is not approved.
- Red tests cannot fail meaningfully.
- A required external dependency or test environment is unavailable.
- The worktree contains overlapping changes of unclear ownership.
- Completion would require expanding beyond the approved work-package scope.

## 11. Workflow Summary

```text
G0  Clean workspace and authoritative inputs
 ↓
G1  Architecture decisions accepted
 ↓
G2  Technical Design Specification approved
 ↓
G3  Test Specification approved
 ↓
G4  Automated tests meaningfully RED
 ↓
G5  Implementation GREEN and refactored
 ↓
G6  Code and test review approved
 ↓
G7  Full validation, traceability, and Definition of Done
```

No work package is complete merely because its code compiles. Completion requires approved design, approved test intent, meaningful Red Gate evidence, passing verification, review closure, and end-to-end traceability.
