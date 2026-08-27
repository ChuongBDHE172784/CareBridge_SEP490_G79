# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — `<Feature / Use Case Name>`

| Field | Value |
| --- | --- |
| Document ID | `<UC-ID>-TEST-SPEC` |
| Version | `0.1` |
| Date | `<YYYY-MM-DD>` |
| Status | `Draft` |
| Feature / Gap ID | `<UC-ID or GAP-ID>` |
| Function ID | `<SRS Function ID>` |
| Canonical Use Case | `<UC-ID> — <Actor goal>` |
| Module | `<Module>` |
| Bounded Context | `<Bounded context>` |
| Paired TDS | `<UC-ID>-TDS` |
| Priority | `<Critical / High / Medium / Low / Open>` |
| Sprint / Milestone | `<Value or Open>` |
| Owner | `<Team or Open>` |
| Author | `<Name>` |
| Reviewer | `<Leave blank until assigned>` |
| Approver | `<Leave blank until assigned>` |
| Platforms | `<Backend / Web / Mobile / AI Service / Not applicable>` |
| Data Classification | `<Public / Internal / Confidential / Restricted / Open>` |
| Compliance Scope | `<PDPA / healthcare / consent / retention / Not applicable — reason / Open>` |
| Upstream Dependencies | `<Exact services, providers, or Not applicable — reason>` |
| Downstream Consumers | `<Exact clients, events, or Not applicable — reason>` |
| Source Baseline | `<Exact source files and revisions>` |

## CHANGELOG

| Version | Date | Author | Change | Status |
| --- | --- | --- | --- | --- |
| 0.1 | `<YYYY-MM-DD>` | `<Author>` | Initial code-first draft | Draft |

## TABLE OF CONTENTS

1. [Module Information and AI Generation Context](#1-module-information-and-ai-generation-context)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry, Exit, and Suspension Criteria](#6-entry-exit-and-suspension-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Module Information and AI Generation Context

### 1.1 Module Information

| Item | Specification | Oracle Source |
| --- | --- | --- |
| Actor goal | `<Actor initiates trigger and obtains observable outcome>` | `<SRS path + locator>` |
| Current implementation state | `<Implemented / Partial / API-only / UI-only / Not implemented>` | `<Exact code paths and symbols>` |
| Supported entry points | `<Exact Web/Mobile route, API endpoint, event, or job>` | `<Router/controller source>` |
| In-scope layers | `<Backend / Web / Mobile / AI Service>` | `<Repository evidence>` |
| Out-of-scope layers | `<Layer — reason>` | `<Approved decision or current-code evidence>` |
| Protected or sensitive data | `<Fields and classification, or Not applicable — reason>` | `<Schema/policy source>` |
| Authorization boundary | `<Roles, ownership, membership, consent>` | `<Security config/policy/controller source>` |
| Primary state transitions | `<From -> action -> to, or Not applicable — reason>` | `<Entity/service/migration source>` |
| External dependencies | `<Provider and failure behavior, or Not applicable — reason>` | `<Adapter/config/source>` |

### 1.2 AI Generation Context (CASE 2.0)

This document may be drafted with AI assistance, but every expected value must be
grounded in an explicit oracle. Current code is evidence of current behavior; it is
not automatically an approved business requirement. Contradictions remain `Open`
until a recorded decision selects the authoritative behavior.

| Control | Required value |
| --- | --- |
| Generation mode | Evidence-first; no invented contracts or pass results |
| Permitted sources | `<Approved SRS/BR/ADR, paired TDS, exact code/schema/tests, user decisions>` |
| Trust level | `<Draft until human review>` |
| Unknown handling | `Open — <question>; evidence needed: <source/decision>` |
| Non-applicable handling | `Not applicable — <feature-specific reason>` |
| Existing test status | Evidence only; rerun before recording current pass/fail |
| Safety constraint | Do not use production credentials, real protected data, or destructive migrations |

### 1.3 Reference Baseline

| Ref ID | Type | Exact path / locator / symbol | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-REQ-01` | Requirement | `<path#section>` | `<revision>` | `<Approved / Draft / Open>` |
| `SRC-TDS-01` | Design | `<paired TDS#section>` | `<version>` | Draft |
| `SRC-CODE-01` | Current code | `<path::symbol>` | `<commit/worktree>` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `<path::test name>` | `<commit/worktree>` | Regression evidence |
| `SRC-DEC-01` | User decision | `<decision record>` | `<date>` | Approved |

---

## 2. Logic Issues Resolved

Record every discrepancy that changes architecture, schema, authorization, API
contract, state semantics, or expected test behavior. Do not silently pick a side.

| Issue ID | Competing sources / observed discrepancy | Impact | Resolution | Decision / Oracle Source | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | `<Requirement says X; code does Y>` | `<Tests/design affected>` | `<Approved resolution, or Open question>` | `<SRC-...>` | `<Resolved / Open>` |

If none were found, write `No contradiction found after reviewing <exact source
set>` and list that source set. Never use a generic statement without evidence.

### 2.1 Open Questions Blocking Test Oracles

| Open ID | Question | Why it matters | Evidence / decision needed | Owner | Status |
| --- | --- | --- | --- | --- | --- |
| `OPEN-01` | `<Question>` | `<Affected contract or TC>` | `<Exact missing evidence>` | `<Owner or Open>` | Open |

---

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Risk / failure mode | Severity | Likelihood | Detectability | In-scope test levels | Mitigation / Test Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | `<Feature-specific failure>` | `<Critical/High/Medium/Low>` | `<H/M/L>` | `<H/M/L>` | `<Unit/Integration/Contract/UI/E2E/Security>` | `<COND-...>` |

#### Platform and Test-Level Applicability Matrix

Every cell must be populated. Use `Applicable — <specific scope>` or `Not
applicable — <feature-specific reason>`; do not generate tests for an absent layer.

| Platform / Layer | Unit | Integration | Contract / Component | Widget / UI | E2E | Security |
| --- | --- | --- | --- | --- | --- | --- |
| Backend | `<...>` | `<...>` | `<...>` | `Not applicable — backend has no UI` | `<...>` | `<...>` |
| Web | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| Mobile | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| AI Service | `<...>` | `<...>` | `<...>` | `Not applicable — reason` | `<...>` | `<...>` |

### TDS-02 — Test Basis and Oracle Hierarchy

| Basis ID | Requirement / ADR / Rule / Contract | Exact source | Authoritative oracle | Covered by conditions |
| --- | --- | --- | --- | --- |
| `BASIS-01` | `<FR/BR/AC/ADR/API rule>` | `<path#locator or path::symbol>` | `<Expected behavior/value>` | `<COND-...>` |

Oracle precedence for this feature:

1. `<Approved user decision or signed requirement>`
2. `<Approved BR/ADR/security policy>`
3. `<Paired TDS contract>`
4. `<Current implementation evidence for current-state characterization>`
5. `<Existing automated test as regression evidence, not requirement authority>`

Any conflict between higher and lower sources must appear in Section 2.

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Requirement / risk | Condition | Layer / platform | Coverage type | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `<BASIS-01 / RISK-01>` | `<Happy-path behavior>` | `<Backend/Web/Mobile/AI>` | Positive | `<TC-...>` |
| `COND-02` | `<BASIS-...>` | `<Validation/boundary behavior>` | `<...>` | Negative / Boundary | `<TC-...>` |
| `COND-03` | `<BASIS-...>` | `<Role/ownership/consent behavior>` | `<...>` | Security | `<TC-...>` |
| `COND-04` | `<BASIS-...>` | `<Persistence/event/side-effect behavior>` | `<...>` | Integration | `<TC-...>` |
| `COND-05` | `<BASIS-...>` | `<Timeout/retry/dependency behavior, or N/A>` | `<...>` | Resilience | `<TC-...>` |

#### State and Transition Coverage

| State / invariant | Allowed transition or observation | Forbidden transition | Oracle Source | Test cases |
| --- | --- | --- | --- | --- |
| `<State>` | `<Action -> state>` | `<Rejected transition>` | `<SRC-...>` | `<TC-...>` |

#### API and Error Coverage

| Endpoint / interface | Auth / role | Success contract | Validation / domain errors | Ownership / security errors | Test cases |
| --- | --- | --- | --- | --- | --- |
| `<METHOD /path>` | `<policy>` | `<status + schema>` | `<status/code>` | `<status/code>` | `<TC-...>` |

### TDS-04 — Test Techniques

| Technique | Applied to | Rationale | Conditions / Test cases |
| --- | --- | --- | --- |
| Equivalence partitioning | `<Input/role/state classes>` | `<Why>` | `<COND/TC>` |
| Boundary value analysis | `<Lengths/ranges/time boundaries>` | `<Why or N/A reason>` | `<COND/TC>` |
| Decision table | `<Role x state x action>` | `<Why or N/A reason>` | `<COND/TC>` |
| State-transition testing | `<Lifecycle>` | `<Why or N/A reason>` | `<COND/TC>` |
| Pairwise / combinatorial | `<Parameters>` | `<Why or N/A reason>` | `<COND/TC>` |
| Error guessing | `<Historical/high-risk failures>` | `<Evidence>` | `<COND/TC>` |
| Contract testing | `<API/provider boundary>` | `<Why or N/A reason>` | `<COND/TC>` |

### TDS-05 — Test Data, Fixtures, Environment, and Isolation

#### Data Requirements

| Data ID | Purpose | Minimal synthetic fields | Boundary / variants | Source / factory | Cleanup |
| --- | --- | --- | --- | --- | --- |
| `DATA-01` | `<Scenario>` | `<Fields; no real protected data>` | `<Values>` | `<Factory/fixture path>` | `<Rollback/delete/transaction>` |

#### Determinism and Isolation Controls

| Concern | Required control | Exact implementation / intended path |
| --- | --- | --- |
| Clock | `<Fixed clock / fake timers / Not applicable — reason>` | `<path or planned helper>` |
| Randomness / IDs | `<Seed/factory/injected generator>` | `<path>` |
| Authentication | `<Test principal/token factory>` | `<path>` |
| Database | `<Transaction/container/isolated schema>` | `<path/config>` |
| External providers | `<Fake/mock/approved sandbox>` | `<path/config>` |
| Event delivery | `<Captured publisher/fake bus/outbox assertion>` | `<path/config>` |
| Files / media | `<Synthetic fixture and cleanup>` | `<path or N/A>` |
| AI model / embeddings | `<Deterministic fake/fixed corpus>` | `<path or N/A>` |
| Sensors / camera / location | `<Synthetic frames/readings/provider fake>` | `<path or N/A>` |

#### Environment Matrix

| Environment | Purpose | Dependencies | Secrets/data policy | Supported command |
| --- | --- | --- | --- | --- |
| Local isolated | Unit/component | `<Dependencies>` | Synthetic only | `<Exact repository-supported command>` |
| Test container | Persistence/integration | `<DB/broker>` | Synthetic only | `<Command>` |
| Approved sandbox | Provider contract | `<Provider>` | Non-production credentials | `<Command or Open>` |

---

## 4. Test Case Specification

### 4.1 Props Isolation Boilerplate (CASE 2.0 — Required)

Create a factory only for platforms that are applicable. Keep each test's override
set minimal so adding a new production field does not break unrelated cases.

#### Java / Kotlin Example

```java
private CreateRequest makeCreateRequest(Consumer<CreateRequestBuilder> overrides) {
    CreateRequestBuilder builder = CreateRequestBuilder.validDefaults();
    overrides.accept(builder);
    return builder.build();
}
```

#### TypeScript / React Example

```ts
const makeProps = (overrides: Partial<Props> = {}): Props => ({
  item: makeItem(),
  onSubmit: vi.fn(),
  ...overrides,
});
```

#### Dart / Flutter Example

```dart
Widget makeSubject({
  Repository? repository,
  User? currentUser,
}) {
  return TestApp(
    repository: repository ?? FakeRepository.withDefaults(),
    currentUser: currentUser ?? UserFactory.valid(),
    child: const SubjectScreen(),
  );
}
```

If a platform is not applicable, write `Not applicable — <specific reason>`.

### 4.2 Detailed Test Cases

Copy the following complete block for every test case. Do not replace it with a
one-line generic matrix.

### `<UC-ID>-TC-001` — `<Behavior-focused title>`

| Field | Specification |
| --- | --- |
| Stable ID | `<UC-ID>-TC-001` |
| Severity | `<Critical / High / Medium / Low>` |
| Test Condition | `<COND-...>` |
| Test Level | `<Unit / Integration / Contract / Widget / E2E / Security>` |
| Platform / Layer | `<Backend / Web / Mobile / AI Service>` |
| Technique | `<Technique from TDS-04>` |
| Oracle Source | `<BASIS/SRC ID plus exact locator>` |
| Preconditions | `<Actor, role, state, data, environment>` |
| Intended Test File | `<Existing exact repository path, or Planned — path (not present at Draft baseline)>` |
| Initial Status | `🔴 Not written` |

**Arrange**

1. `<Create only the required synthetic actor/data/dependencies.>`
2. `<Set clock/provider/auth/state explicitly.>`

**Act**

1. `<Invoke exact method, endpoint, route, UI action, or event.>`

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | `<Exact status, error code, type, fields, or value>` | `<SRC/BASIS locator>` |
| Persistence | `<Exact row/state/version change, or no write>` | `<schema/TDS source>` |
| Audit | `<Exact audit action/payload, or Not applicable — reason>` | `<policy/TDS source>` |
| Event / notification | `<Exact event and delivery expectation, or N/A>` | `<event/TDS source>` |
| Provider side effect | `<Exact call count/payload, or N/A>` | `<adapter contract>` |
| UI state | `<Visible content/navigation/error/retry state, or N/A>` | `<UX/current-code source>` |
| Privacy / logging | `<No sensitive leakage / exact redaction, or N/A>` | `<policy source>` |

**Failure signature**

`<What a failing assertion/log/status looks like and which contract it indicates>`

**Cleanup / isolation**

`<Transaction rollback, fixture reset, fake cleanup, or Not applicable — reason>`

### 4.3 Required Case Families

Populate only evidence-backed and applicable families; mark the rest with a reason.

| Family | Minimum coverage expectation | Case IDs / applicability |
| --- | --- | --- |
| Happy path | Primary actor outcome | `<TC IDs>` |
| Validation and boundaries | Each validated field and critical boundary | `<TC IDs>` |
| Authentication and RBAC | Unauthenticated and disallowed roles | `<TC IDs or N/A reason>` |
| Ownership / membership / consent | Cross-actor isolation and revocation | `<TC IDs or N/A reason>` |
| State transitions | Allowed, duplicate, stale, forbidden transitions | `<TC IDs or N/A reason>` |
| Persistence and migration | Constraints, indexes, compatibility | `<TC IDs or N/A reason>` |
| Events / notifications / audit | Payload and exactly-once/idempotency semantics only if sourced | `<TC IDs or N/A reason>` |
| External failure | Timeout, unavailable, malformed response | `<TC IDs or N/A reason>` |
| Concurrency / retries | Only when semantics are sourced | `<TC IDs or N/A reason>` |
| Empty / loading / error / recovery UI | Reachable actor-visible states | `<TC IDs or N/A reason>` |
| Accessibility | Sourced or project-standard checks | `<TC IDs or Open>` |
| Data protection | Minimization, redaction, retention, access | `<TC IDs or N/A reason>` |

---

## 5. Red-Green-Refactor Tracker

### 5.1 Tracker

Do not mark a row green without executing the exact command and recording evidence.

| TC ID | Intended test file | Red evidence | Green evidence | Refactor verification | Current status |
| --- | --- | --- | --- | --- | --- |
| `<TC-ID>` | `<existing path, or Planned — path (not present at Draft baseline)>` | `<Not run / exact failing command+signature>` | `<Not run / exact passing command+count>` | `<Not run / command>` | `🔴 Not written` |

### 5.2 Red Gate Protocol (CASE 2.0 — GATE-2)

For each new or changed behavior:

1. Write the narrowest applicable test from Section 4.
2. Execute the exact supported command.
3. Confirm failure for the intended missing or incorrect behavior, not setup noise.
4. Record the command, timestamp, environment, and failure signature.
5. Implement the smallest production change only in the implementation phase.
6. Rerun and record green evidence.
7. Refactor while keeping targeted and affected suites green.

This specification phase must not claim completion of steps that were not executed.

### 5.3 Verification Evidence Table

| Evidence ID | Date/time | Environment | Command | Result/counts | Artifact/log | Recorded by |
| --- | --- | --- | --- | --- | --- | --- |
| `EVD-01` | `<Not run>` | `<...>` | `<Exact command>` | `<Not run>` | `<path or N/A>` | `<name>` |

---

## 6. Entry, Exit, and Suspension Criteria

### 6.1 Entry Criteria

- [ ] Paired TDS is `Draft` or `In Review` and all 17 sections are populated.
- [ ] Requirement, API, state, authorization, data, and error oracles are explicit.
- [ ] Architecture/test-changing contradictions in Section 2 are resolved.
- [ ] Applicable test levels and environments are available.
- [ ] Synthetic fixtures and provider fakes are identified.
- [ ] Schema/migration requirements are known or explicitly `Open`.

### 6.2 Exit Criteria

- [ ] Every in-scope requirement maps to at least one Test Condition and TC.
- [ ] Every TDS field/state/error/auth/event/side-effect contract has coverage.
- [ ] All Critical and High TCs have current execution evidence.
- [ ] All applicable automated suites pass with recorded commands/counts.
- [ ] No Critical/High unresolved defect remains.
- [ ] No secrets or real protected data exist in fixtures, logs, or snapshots.
- [ ] Rollback checks are executable and reviewed.
- [ ] Reviewer and approver sign-off are recorded.

### 6.3 Suspension and Resumption Criteria

| Trigger | Suspend when | Resume when |
| --- | --- | --- |
| Oracle ambiguity | Expected behavior changes with unresolved source conflict | Recorded decision updates TDS/Test-Spec |
| Environment | Required dependency or migration is unreliable | Reproducible isolated environment is restored |
| Security/privacy | Test risks real credentials or protected data | Approved sandbox/synthetic substitute exists |
| Destructive behavior | Test may corrupt shared state or migration history | Recoverable isolated procedure is approved |
| Provider instability | Failure cannot distinguish product defect from provider outage | Fake/sandbox contract is stable or outage resolved |

---

## 7. Rollback Plan

### 7.1 Test Artifact Rollback

| Artifact | Safe rollback action | Verification |
| --- | --- | --- |
| New/changed tests | Revert the focused test change on the working branch | Targeted baseline suite returns to prior result |
| Fixtures/factories | Restore prior factory contract; remove only feature-owned synthetic data | Unrelated suites remain green |
| Test configuration | Restore versioned config; never delete shared secrets/state | Supported smoke command succeeds |
| Schema fixture/migration | Use forward-compatible corrective migration or recreate isolated DB | Migration history and schema validation pass |

### 7.2 Production-Change Rollback Verification

Define tests that prove the paired TDS rollback runbook is safe:

| Rollback risk | Verification case | Oracle Source | Status |
| --- | --- | --- | --- |
| `<Compatibility/data/event risk>` | `<TC or operational check>` | `<TDS section/source>` | `🔴 Not written` |

Never recommend editing or deleting applied Flyway history in a shared environment.

---

## 8. CASE 2.0 Anti-Pattern Detection

| Anti-pattern | Detection question | Required evidence | Result |
| --- | --- | --- | --- |
| Hallucinated oracle | Does any expected value lack an exact source? | All assertion rows cite `SRC/BASIS` | `<Pass / Fail / Open>` |
| Generic test matrix | Could the same cases be pasted into an unrelated UC unchanged? | Feature-specific states, fields, roles, errors, paths | `<...>` |
| False green claim | Is any test marked passing without current execution evidence? | Command, timestamp, counts, failure/pass artifact | `<...>` |
| Hidden contradiction | Was code chosen over requirement without a decision? | Section 2 ledger | `<...>` |
| Missing Props Isolation | Do tests construct large shared objects inline? | Applicable `makeXxx()` factories | `<...>` |
| Over-mocking | Does the test bypass the contract or state being verified? | Mock-boundary rationale | `<...>` |
| Brittle implementation assertion | Does the test assert private call order instead of observable behavior? | Behavior-focused oracle | `<...>` |
| Cross-test pollution | Can order, clock, DB, provider, or global state change the result? | TDS-05 isolation controls | `<...>` |
| Unsafe data | Are real health/location/identity/conversation values used? | Synthetic fixture audit | `<...>` |
| Wrong-layer test | Is a UI/E2E test generated for an absent consumer? | Applicability matrix | `<...>` |
| Uncovered contract | Is any field/state/error/auth/event missing a TC? | Traceability comparison | `<...>` |
| AI safety bypass | Can model output directly mutate clinical/safety state without deterministic policy? | Deterministic guardrail cases or N/A reason | `<...>` |

### 8.1 Final Self-Check

- [ ] Exactly 8 top-level sections are present.
- [ ] All metadata and reference fields are populated or explicitly `Open`/`Not applicable`.
- [ ] Each expected result cites an oracle source.
- [ ] Each applicable TC has stable ID, severity, condition, preconditions, AAA,
      persistence/audit/event/provider/UI assertions, failure signature, intended path,
      cleanup, and initial status.
- [ ] The applicability matrix prevents irrelevant boilerplate tests.
- [ ] The Red Gate is usable without claiming unexecuted evidence.
- [ ] Contradictions and research gaps remain visible.
- [ ] Paired TDS and this Test-Spec are bidirectionally traceable.
