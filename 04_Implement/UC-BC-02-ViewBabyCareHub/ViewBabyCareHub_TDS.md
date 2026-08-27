# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — View Baby Care Hub and Detail Overview

| Field | Value |
| --- | --- |
| Document ID | `UC-BC-02-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-BC-02` |
| Canonical Use Case | `UC-BC-02 — View Baby Care Hub and Detail Overview` |
| Module / Bounded Context | `Baby Care` |
| Primary Actor | `Mother / Authorized Caregiver` |
| Platforms | `Mobile / Web / Backend` |
| Priority | `Medium` |
| Data Classification | `Restricted child health, growth, milestone, vaccination, and daily-care data` |
| Compliance Scope | `PDPA child/health-data minimization, caregiver authorization, non-diagnostic presentation, and retention/audit controls` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-BC-02`; exact evidence in Section 1.4 |

## CHANGELOG

| Version | Date | Author | Change | Status |
| --- | --- | --- | --- | --- |
| 0.1 | 2026-08-23 | CareBridge Team | Initial evidence-first full-form draft | Draft |

## TABLE OF CONTENTS

1. Module Overview
2. Traceability Matrix
3. Architecture Decision Records
4. Non-Functional Requirements and SLA
5. Static Modeling
6. Dynamic Modeling
7. Domain Event Catalog
8. Interface Specification
9. API Specification
10. Error Codes
11. Implementation and Deployment Plan
12. Rollback and Incident Runbook
13. Verification Scenario Groups
14. Verification Methods
15. Verification Samples
16. Authorization Matrix
17. AI Prompt Constraints — CASE 2.0

## 1. Module Overview

### 1.1 Actor Goal, Trigger, and Outcome

- **Goal:** View the selected baby's care hub assembled from current daily-log, growth, milestone, and vaccination projections.
- **Trigger:** The actor enters Mobile baby profile detail.
- **Outcome:** Navigate to the corresponding detailed care flow.
- **Current state:** `Medium` confidence from reachable code/test audit; documented limitations remain visible below.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- Mobile baby profile detail
- Web `/mother/baby-care`

- Read endpoints for daily logs, growth, milestones, and vaccination records

**Out of scope / limitations**

- Open / current limitation: Web BabyCareHub has no focused test; backend `care-overview`/`care-timeline` routes have no client consumer.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother / Authorized Caregiver is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Navigate to the corresponding detailed care flow. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-BC-02 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeMobileApp/test/features/baby/baby_care_contract_test.dart` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- Open / current limitation: Web BabyCareHub has no focused test; backend `care-overview`/`care-timeline` routes have no client consumer.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-BC-02-FR-01` | Select an authorized baby. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-BC-02 Normal Flow 1 | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` | `COND-01` / `UC-BC-02-TC-001` |
| `UC-BC-02-FR-02` | Load the current care projections from their owning resources. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-BC-02 Normal Flow 2 | `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | `COND-02` / `UC-BC-02-TC-002` |
| `UC-BC-02-FR-03` | Navigate to the corresponding detailed care flow. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-BC-02 Normal Flow 3 | `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | `COND-03` / `UC-BC-02-TC-003` |
| `BR-01` | The hub is a composition; it does not create a separate canonical care-overview resource. | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` | `COND-BR-01` / `UC-BC-02-TC-BR-001` |
| `BR-02` | Baby authorization is rechecked by every underlying endpoint. | `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | `COND-BR-02` / `UC-BC-02-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-BC-02-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-BC-02 — View Baby Care Hub and Detail Overview` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-BC-02-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` | Reuse | Current implementation evidence for View Baby Care Hub and Detail Overview; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | Reuse | Current implementation evidence for View Baby Care Hub and Detail Overview; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class baby_profile_detail_screen as "baby_profile_detail_screen.dart"
class BabyCareHubPage as "BabyCareHubPage.tsx"
baby_profile_detail_screen --> BabyCareHubPage
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | No entity/repository import is present in the cited entry/source set; follow the exact handler-to-service call path before any schema change |
| Sensitive fields | Restricted child health, growth, milestone, vaccination, and daily-care data. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
| Schema delta for documentation alignment | Not applicable — this Draft does not change runtime schema. |
| Future implementation migration | Use additive, versioned migrations only when an approved behavior requires schema change. |
| V1 synchronization | Not applicable — this documentation alignment changes no schema; any future schema work must inspect current Flyway history and must never rewrite an applied migration. |

## 6. Dynamic Modeling

### 6.1 Happy Path Sequence

```plantuml
@startuml
actor Actor
participant Client
participant Domain
Actor -> Client: Enter View Baby Care Hub and Detail Overview
Client -> Domain: Select an authorized baby.
Domain --> Client: Result for step 1
Client -> Domain: Load the current care projections from their owning resources.
Domain --> Client: Result for step 2
Client -> Domain: Navigate to the corresponding detailed care flow.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-BC-02 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | Not applicable at this cited baseline — no external adapter/provider import is evidenced in the listed implementation sources |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Select an authorized baby.
InProgress --> Outcome : Navigate to the corresponding detailed care flow.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- The hub is a composition; it does not create a separate canonical care-overview resource.
- Baby authorization is rechecked by every underlying endpoint.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | Mobile baby profile detail | Mother / Authorized Caregiver | Reachable current entry point |
| 2 | Web `/mother/baby-care` | Mother / Authorized Caregiver | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | GET `/api/v1/babies` | Bearer-authenticated mother/caregiver; backend filters authorized babies | Web selects the active baby or first authorized baby; empty data yields no selected baby. Exact composition oracle: `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx`. |
| `API-02` | PATCH `/api/v1/babies/{babyId}/active` | Authorized baby relationship required | Web persists active-baby selection before reloading all hub counters. Exact composition oracle: `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx`. |
| `API-03` | GET `/api/v1/babies/{babyId}/daily-logs` | Authorized baby relationship required | Web derives the journal count from the returned data array; Mobile opens the owning daily-log flow. Exact composition oracle: `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx`. |
| `API-04` | GET `/api/v1/babies/{babyId}/growth-measurements?page=0&size=20` | Authorized baby relationship required | Web derives the growth count from page content; Mobile loads the growth history/chart through `GrowthMeasurementService`. Exact composition oracle: `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx`. |
| `API-05` | GET `/api/v1/babies/{babyId}/milestones` | Authorized baby relationship required | Web derives the milestone count; Mobile loads milestone records through the baby-log service. Exact composition oracle: `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx`. |
| `API-06` | GET `/api/v1/vaccination/babies/{babyId}/records` plus schedule | Authorized baby relationship required | Web derives the vaccination count; Mobile separately loads vaccination records and computed schedule. Exact composition oracle: `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx`. |

This is a code-backed composition contract, not a missing aggregate API. The rows above enumerate the exact currently called owning endpoints and projection rules; request/response/error ownership remains with those underlying resources.

### 9.1 Composition Contract — GET `/api/v1/babies`

| Item | Exact current contract |
| --- | --- |
| Contract kind | Client-side composition of an existing owning resource; no aggregate write resource is introduced |
| Authorization | Bearer-authenticated mother/caregiver; backend filters authorized babies |
| Projection behavior | Web selects the active baby or first authorized baby; empty data yields no selected baby. |
| Client oracle | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |
| Failure boundary | Preserve only authorized/current data, show the implemented error/degraded state, and never invent a count or record |
| Test mapping | `COND-API-001` / `UC-BC-02-TC-API-001` |

### 9.2 Composition Contract — PATCH `/api/v1/babies/{babyId}/active`

| Item | Exact current contract |
| --- | --- |
| Contract kind | Client-side composition of an existing owning resource; no aggregate write resource is introduced |
| Authorization | Authorized baby relationship required |
| Projection behavior | Web persists active-baby selection before reloading all hub counters. |
| Client oracle | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |
| Failure boundary | Preserve only authorized/current data, show the implemented error/degraded state, and never invent a count or record |
| Test mapping | `COND-API-002` / `UC-BC-02-TC-API-002` |

### 9.3 Composition Contract — GET `/api/v1/babies/{babyId}/daily-logs`

| Item | Exact current contract |
| --- | --- |
| Contract kind | Client-side composition of an existing owning resource; no aggregate write resource is introduced |
| Authorization | Authorized baby relationship required |
| Projection behavior | Web derives the journal count from the returned data array; Mobile opens the owning daily-log flow. |
| Client oracle | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |
| Failure boundary | Preserve only authorized/current data, show the implemented error/degraded state, and never invent a count or record |
| Test mapping | `COND-API-003` / `UC-BC-02-TC-API-003` |

### 9.4 Composition Contract — GET `/api/v1/babies/{babyId}/growth-measurements?page=0&size=20`

| Item | Exact current contract |
| --- | --- |
| Contract kind | Client-side composition of an existing owning resource; no aggregate write resource is introduced |
| Authorization | Authorized baby relationship required |
| Projection behavior | Web derives the growth count from page content; Mobile loads the growth history/chart through `GrowthMeasurementService`. |
| Client oracle | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |
| Failure boundary | Preserve only authorized/current data, show the implemented error/degraded state, and never invent a count or record |
| Test mapping | `COND-API-004` / `UC-BC-02-TC-API-004` |

### 9.5 Composition Contract — GET `/api/v1/babies/{babyId}/milestones`

| Item | Exact current contract |
| --- | --- |
| Contract kind | Client-side composition of an existing owning resource; no aggregate write resource is introduced |
| Authorization | Authorized baby relationship required |
| Projection behavior | Web derives the milestone count; Mobile loads milestone records through the baby-log service. |
| Client oracle | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |
| Failure boundary | Preserve only authorized/current data, show the implemented error/degraded state, and never invent a count or record |
| Test mapping | `COND-API-005` / `UC-BC-02-TC-API-005` |

### 9.6 Composition Contract — GET `/api/v1/vaccination/babies/{babyId}/records` plus schedule

| Item | Exact current contract |
| --- | --- |
| Contract kind | Client-side composition of an existing owning resource; no aggregate write resource is introduced |
| Authorization | Authorized baby relationship required |
| Projection behavior | Web derives the vaccination count; Mobile separately loads vaccination records and computed schedule. |
| Client oracle | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |
| Failure boundary | Preserve only authorized/current data, show the implemented error/degraded state, and never invent a count or record |
| Test mapping | `COND-API-006` / `UC-BC-02-TC-API-006` |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Underlying endpoint rejection | Owning endpoint's current `4xx` mapping | Authentication, membership, ownership, baby access, journey access, or malformed identifier fails | Keep the hub/shared projection closed or unchanged; never merge cross-user data | Exact client composition in `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart` plus owning endpoint TDS |
| Partial composition failure | Client-local degraded/partial state as implemented | One of several parallel/read-refresh calls fails | Show current error or preserve only previously authorized shared data; do not invent counts/measurements | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |
| No dedicated aggregate resource | Not applicable | These UCs compose existing resources and intentionally have no hub/shared-record write endpoint | Route mutations to the owning resource UC only | SRS `UC-BC-02` Known Gaps / Exclusions |

## 11. Implementation and Deployment Plan

1. Preserve this current code-backed boundary and resolve every `Open` item that changes tests, schema, auth, API, or state.
2. Map exact DTO fields, service/repository symbols, migrations, events, and error codes from the listed evidence.
3. Write paired Test-Spec Red cases before production changes.
4. Implement only the approved gap; reuse current components listed in Section 5.
5. Run targeted and affected suites; record commands/counts only after execution.
6. Deploy compatible server/schema changes before clients that depend on them; preserve old-client compatibility where required.

## 12. Rollback and Incident Runbook

| Trigger | Safe rollback / containment | Verification |
| --- | --- | --- |
| Documentation error | Revert only this generated pair/manifest change to the last reviewed version. | Regenerate and run document validators. |
| Client regression | Disable/revert the feature-owned client change while keeping compatible server contracts. | Targeted route/widget/component tests. |
| Server regression | Revert the feature-owned change or deploy a forward corrective fix. | Targeted backend/AI suite and smoke contract. |
| Schema issue | Stop rollout; restore through additive corrective migration or isolated backup procedure. Never edit applied Flyway history. | Migration validation and data-integrity checks. |
| Provider incident | Disable optional integration or use only the approved degraded mode. | Provider-fake/sandbox contract tests. |

## 13. Verification Scenario Groups

| Group | Behavior | Condition | Test case |
| --- | --- | --- | --- |
| `VG-01` | Select an authorized baby. | `COND-01` | `UC-BC-02-TC-001` |
| `VG-02` | Load the current care projections from their owning resources. | `COND-02` | `UC-BC-02-TC-002` |
| `VG-03` | Navigate to the corresponding detailed care flow. | `COND-03` | `UC-BC-02-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-BC-02-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-BC-02-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeMobileApp/test/features/baby/baby_care_contract_test.dart`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeMobileApp && flutter test test/features/baby/baby_care_contract_test.dart`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | Read endpoints for daily logs, growth, milestones, and vaccination records |
| Request | Composition call GET `/api/v1/babies`; authorization: Bearer-authenticated mother/caregiver; backend filters authorized babies. |
| Success response | Web selects the active baby or first authorized baby; empty data yields no selected baby. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother / Authorized Caregiver | GET `/api/v1/babies` | Bearer-authenticated mother/caregiver; backend filters authorized babies |
| Mother / Authorized Caregiver | PATCH `/api/v1/babies/{babyId}/active` | Authorized baby relationship required |
| Mother / Authorized Caregiver | GET `/api/v1/babies/{babyId}/daily-logs` | Authorized baby relationship required |
| Mother / Authorized Caregiver | GET `/api/v1/babies/{babyId}/growth-measurements?page=0&size=20` | Authorized baby relationship required |
| Mother / Authorized Caregiver | GET `/api/v1/babies/{babyId}/milestones` | Authorized baby relationship required |
| Mother / Authorized Caregiver | GET `/api/v1/vaccination/babies/{babyId}/records` plus schedule | Authorized baby relationship required |
| Unauthenticated / wrong role / wrong owner-member | All protected operations | Deny without resource disclosure |

## 17. AI Prompt Constraints — CASE 2.0

- Not applicable — this UC does not generate clinical AI output. Generic documentation assistance remains evidence-first.

### Quality and Anti-Pattern Checklist

- [ ] All 17 sections remain present.
- [ ] Every known semantic value has an exact source; unresolved values are `Open` with evidence needed.
- [ ] Requirement → component → condition → test traceability is preserved.
- [ ] No historical pass count, SLA, accuracy, or provider claim is copied without current evidence.
- [ ] No generic endpoint group is treated as an implementation-ready field/error contract.
- [ ] No production code, schema, or immutable AI architecture source is modified by spec generation.
