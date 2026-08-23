# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Review Shared Maternal Metrics and Checklists

| Field | Value |
| --- | --- |
| Document ID | `UC-EX-12-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-EX-12` |
| Module | `Expert and Consultation` |
| Paired TDS | `UC-EX-12-TDS` |
| Priority | `Medium` |
| Platforms | `Web / Backend` |
| Data Classification | `Confidential professional and consultation data; Restricted identity/credential evidence and purpose-bound attachments` |
| Compliance Scope | `PDPA purpose limitation, least privilege, purpose-bound file access, consultation confidentiality, and consent-aware recording` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-EX-12` and exact code/test sources below |

## CHANGELOG

| Version | Date | Author | Change | Status |
| --- | --- | --- | --- | --- |
| 0.1 | 2026-08-23 | CareBridge Team | Initial evidence-first full-form Draft | Draft |

## TABLE OF CONTENTS

1. Module Information and AI Generation Context
2. Logic Issues Resolved
3. Test Design Specification
4. Test Case Specification
5. Red-Green-Refactor Tracker
6. Entry, Exit, and Suspension Criteria
7. Rollback Plan
8. CASE 2.0 Anti-Pattern Detection

## 1. Module Information and AI Generation Context

### 1.1 Module Information

| Item | Specification | Oracle Source |
| --- | --- | --- |
| Actor goal | Review maternal metrics and checklist information shared through an authorized consultation relationship. | SRS `UC-EX-12` |
| Current state | `Medium` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | Web `/expert/shared-records`; Shared items in direct-chat context | Current client/router evidence |
| Authorization boundary | `Authorized Expert` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx` |
| Primary operations | Select an authorized consultation/conversation context.; Load shared metrics and checklist projections.; Review current records without mutating the mother's canonical data. | SRS `UC-EX-12` Normal Flow |
| Sensitive data | Use the classification header and exact request/response field inventories in paired TDS Sections 5 and 9; synthesize only fields exercised by the case | Paired TDS Sections 5 and 9 |

### 1.2 AI Generation Context (CASE 2.0)

- Generation mode: evidence-first; no invented field, error, SLA, accuracy, or pass result.
- Trust level: Draft until human review.
- Unknown handling: `Open — question/evidence needed`.
- Existing tests are regression evidence and must be rerun before a Green claim.
- The AI architecture source is immutable/reference-only in this workflow.

### 1.3 Reference Baseline

| Ref ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-EX-12 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-EX-12-TDS` | 0.1 | Draft design |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Worktree `2026-08-23` | Current-state evidence |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | No dedicated shared-record backend resource exists; this UC composes existing authorized endpoints. | Could create false completed coverage or wrong contract | Keep as current limitation/Open until the cited client/server mismatch is resolved | SRS `UC-EX-12` gap plus current code | Open |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Select an authorized consultation/conversation context. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Load shared metrics and checklist projections. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Review current records without mutating the mother's canonical data. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
| `RISK-AUTH` | Cross-user/role/member/consent data access | Critical | Medium | Medium | Security/Integration/Contract | `COND-AUTH` |
| `RISK-GAP` | Documentation claims an unreachable or broken path as complete | High | Medium | High | Characterization/Contract/UI | `COND-GAP` |

#### Platform and Test-Level Applicability Matrix

| Platform / Layer | Unit | Integration | Contract / Component | Widget / UI | E2E | Security |
| --- | --- | --- | --- | --- | --- | --- |
| Backend | Applicable — current backend/API contracts | Applicable — current backend/API contracts | Applicable — current backend/API contracts | Not applicable — backend has no UI | Applicable — current backend/API contracts | Applicable — current backend/API contracts |
| Web | Applicable — current Web entry points | Applicable — current Web entry points | Applicable — current Web entry points | Applicable — current Web entry points | Applicable — current Web entry points | Applicable — current Web entry points |
| Mobile | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC |
| AI Service | Not applicable — no Python AI contract in this UC | Not applicable — no Python AI contract in this UC | Not applicable — no Python AI contract in this UC | Not applicable — Python service has no actor UI | Not applicable — no Python AI contract in this UC | Not applicable — no Python AI contract in this UC |

### TDS-02 — Test Basis and Oracle Hierarchy

| Basis | Requirement / behavior | Exact source | Oracle | Conditions |
| --- | --- | --- | --- | --- |
| `BASIS-01` | `UC-EX-12-FR-01` — Select an authorized consultation/conversation context. | SRS `UC-EX-12` Normal Flow 1; TDS Section 2 | Select an authorized consultation/conversation context. | `COND-01` |
| `BASIS-02` | `UC-EX-12-FR-02` — Load shared metrics and checklist projections. | SRS `UC-EX-12` Normal Flow 2; TDS Section 2 | Load shared metrics and checklist projections. | `COND-02` |
| `BASIS-03` | `UC-EX-12-FR-03` — Review current records without mutating the mother's canonical data. | SRS `UC-EX-12` Normal Flow 3; TDS Section 2 | Review current records without mutating the mother's canonical data. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Select an authorized consultation/conversation context. | Web / Backend | Positive + applicable boundary/state coverage | `UC-EX-12-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Load shared metrics and checklist projections. | Web / Backend | Positive + applicable boundary/state coverage | `UC-EX-12-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Review current records without mutating the mother's canonical data. | Web / Backend | Positive + applicable boundary/state coverage | `UC-EX-12-TC-003` |
| `COND-API-001` | Exact handler/client composition contract | Composition call 1: GET `/api/v1/direct-conversations` | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-EX-12-TC-API-001` |
| `COND-API-002` | Exact handler/client composition contract | Composition call 2: GET `/api/v1/direct-conversations/{conversationId}/timeline?limit=50` | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-EX-12-TC-API-002` |
| `COND-API-003` | Exact handler/client composition contract | Composition call 3: GET `/api/v1/journeys/{journeyId}/metrics?metricType={code}` | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-EX-12-TC-API-003` |
| `COND-API-004` | Exact handler/client composition contract | Composition call 4: GET `/api/v1/checklists/journeys/{journeyId}/tasks` | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-EX-12-TC-API-004` |
| `COND-API-005` | Exact handler/client composition contract | Composition call 5: GET `/api/v1/checklists/users/{motherUserId}/tasks` | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-EX-12-TC-API-005` |
| `COND-BR-01` | `BR-01` | The active sharing/consultation relationship is rechecked by the backend. | `05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` | Negative / decision / state | `UC-EX-12-TC-BR-001` |
| `COND-BR-02` | `BR-02` | Read access must not broaden to unrelated journeys or accounts. | `05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` | Negative / decision / state | `UC-EX-12-TC-BR-002` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-EX-12-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-EX-12-TC-GAP-001` |

### TDS-04 — Test Techniques

| Technique | Applied to | Rationale |
| --- | --- | --- |
| Equivalence partitioning | Eligible/ineligible actors, inputs, resources, and states | Covers supported and rejected current classes. |
| Boundary value analysis | Exact DTO ranges/lengths/time boundaries | Use the per-handler validators extracted in paired TDS Section 9; service-only bounds require their cited policy/service oracle. |
| Decision table | Role/ownership/membership/consent x state x action | Prevents UI-only authorization assumptions. |
| State-transition testing | Ordered operations and guarded lifecycle transitions | Detects stale, duplicate, and forbidden actions. |
| Contract testing | Every exact endpoint/provider boundary | Confirms status/schema/error without inventing fields. |
| Error guessing | Each recorded gap and historical broad-UC mismatch | Prevents regression to unreachable or grouped behavior. |

### TDS-05 — Test Data, Fixtures, Environment, and Isolation

| Data | Synthetic fixture | Variants | Cleanup |
| --- | --- | --- | --- |
| Actor | Synthetic `Authorized Expert` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts`

Clock, randomness/IDs, database/container, provider, event, file/media, AI model, camera/sensor/location controls must be fixed in the applicable test; irrelevant controls are `Not applicable — no such dependency in the tested operation`.

## 4. Test Case Specification

### 4.1 Props Isolation Boilerplate (CASE 2.0 — Required)

Use only the applicable platform factory; keep overrides minimal.

```java
private Request makeValidRequest(Consumer<RequestBuilder> overrides) {
    RequestBuilder builder = RequestBuilder.validDefaults();
    overrides.accept(builder);
    return builder.build();
}
```

```ts
const makeProps = (overrides: Partial<Props> = {}): Props => ({
  subject: makeSubject(),
  onAction: vi.fn(),
  ...overrides,
});
```

```dart
Widget makeSubject({Repository? repository, User? actor}) => TestApp(
  repository: repository ?? FakeRepository.withDefaults(),
  currentUser: actor ?? UserFactory.valid(),
  child: const SubjectScreen(),
);
```

For an absent platform, the corresponding factory is `Not applicable` according to the TDS-01 matrix.

### 4.2 Detailed Test Cases

### UC-EX-12-TC-001 — Select an authorized consultation/conversation context

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-001` |
| Severity | `High` |
| Test Condition | `COND-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-EX-12 Normal Flow 1; 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Select an authorized consultation/conversation context.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Select an authorized consultation/conversation context.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-EX-12 Normal Flow 1; 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Select an authorized consultation/conversation context.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-002 — Load shared metrics and checklist projections

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-002` |
| Severity | `High` |
| Test Condition | `COND-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-EX-12 Normal Flow 2; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Load shared metrics and checklist projections.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Load shared metrics and checklist projections.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-EX-12 Normal Flow 2; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Load shared metrics and checklist projections.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-003 — Review current records without mutating the mother's canonical data

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-003` |
| Severity | `High` |
| Test Condition | `COND-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-EX-12 Normal Flow 3; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Review current records without mutating the mother's canonical data.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Review current records without mutating the mother's canonical data.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-EX-12 Normal Flow 3; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Review current records without mutating the mother's canonical data.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-API-001 — Composition call 1: GET `/api/v1/direct-conversations`

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-API-001` |
| Severity | `High` |
| Test Condition | `COND-API-001` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-001`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the current client composition step `GET `/api/v1/direct-conversations`` under this authorization boundary: Bearer-authenticated expert; server conversation membership filters the list

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The client observes only this code-backed projection behavior: `fetchExpertSharedRecords` calls `listMyConversations`; response is `DirectConversationSummary[]`. | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The client observes only this code-backed projection behavior: `fetchExpertSharedRecords` calls `listMyConversations`; response is `DirectConversationSummary[]`.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-API-002 — Composition call 2: GET `/api/v1/direct-conversations/{conversationId}/timeline?limit=50`

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-API-002` |
| Severity | `High` |
| Test Condition | `COND-API-002` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the current client composition step `GET `/api/v1/direct-conversations/{conversationId}/timeline?limit=50`` under this authorization boundary: Bearer-authenticated conversation member

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The client observes only this code-backed projection behavior: Reads `TimelinePage`; only non-recalled `MESSAGE` items tagged `[CAREBRIDGE_HEALTH_SHARE]` or `[CAREBRIDGE_CHECKLIST_SHARE]` become shared-record projections. | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The client observes only this code-backed projection behavior: Reads `TimelinePage`; only non-recalled `MESSAGE` items tagged `[CAREBRIDGE_HEALTH_SHARE]` or `[CAREBRIDGE_CHECKLIST_SHARE]` become shared-record projections.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-API-003 — Composition call 3: GET `/api/v1/journeys/{journeyId}/metrics?metricType={code}`

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-API-003` |
| Severity | `High` |
| Test Condition | `COND-API-003` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the current client composition step `GET `/api/v1/journeys/{journeyId}/metrics?metricType={code}`` under this authorization boundary: Existing authorized share/conversation context; backend remains the authorization authority

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The client observes only this code-backed projection behavior: Live-syncs each shared metric; failed refresh preserves the already-shared projection instead of fabricating a new value. | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The client observes only this code-backed projection behavior: Live-syncs each shared metric; failed refresh preserves the already-shared projection instead of fabricating a new value.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-API-004 — Composition call 4: GET `/api/v1/checklists/journeys/{journeyId}/tasks`

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-API-004` |
| Severity | `High` |
| Test Condition | `COND-API-004` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the current client composition step `GET `/api/v1/checklists/journeys/{journeyId}/tasks`` under this authorization boundary: Existing authorized share/conversation context

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The client observes only this code-backed projection behavior: Live-syncs journey checklist sections `overdue`, `today`, `upcoming`, and `unscheduled`; completed state is normalized from `COMPLETED`/`DONE`. | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The client observes only this code-backed projection behavior: Live-syncs journey checklist sections `overdue`, `today`, `upcoming`, and `unscheduled`; completed state is normalized from `COMPLETED`/`DONE`.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-API-005 — Composition call 5: GET `/api/v1/checklists/users/{motherUserId}/tasks`

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-API-005` |
| Severity | `High` |
| Test Condition | `COND-API-005` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the current client composition step `GET `/api/v1/checklists/users/{motherUserId}/tasks`` under this authorization boundary: Fallback only when the shared payload lacks `journeyId` and includes the conversation-derived mother user

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The client observes only this code-backed projection behavior: Uses the same checklist projection rules; this UC creates no dedicated shared-record backend resource. | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The client observes only this code-backed projection behavior: Uses the same checklist projection rules; this UC creates no dedicated shared-record backend resource.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-BR-001 — Enforce business rule 1

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-BR-001` |
| Severity | `High` |
| Test Condition | `COND-BR-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-01; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: The active sharing/consultation relationship is rechecked by the backend.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: The active sharing/consultation relationship is rechecked by the backend. No disallowed state or protected data is produced. | `TDS BR-01; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: The active sharing/consultation relationship is rechecked by the backend. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-BR-002 — Enforce business rule 2

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-BR-002` |
| Severity | `High` |
| Test Condition | `COND-BR-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-02; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Read access must not broaden to unrelated journeys or accounts.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Read access must not broaden to unrelated journeys or accounts. No disallowed state or protected data is produced. | `TDS BR-02; 05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Read access must not broaden to unrelated journeys or accounts. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-EX-12-TC-GAP-001 — Characterize the current documented limitation

| Field | Specification |
| --- | --- |
| Stable ID | `UC-EX-12-TC-GAP-001` |
| Severity | `High` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-EX-12 Known Gaps / Exclusions; 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx` |
| Preconditions | Synthetic `Authorized Expert` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. No dedicated shared-record backend resource exists; this UC composes existing authorized endpoints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The test records the current limitation without inventing a completed path: No dedicated shared-record backend resource exists; this UC composes existing authorized endpoints. | `SRS UC-EX-12 Known Gaps / Exclusions; 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-EX-12` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The test records the current limitation without inventing a completed path: No dedicated shared-record backend resource exists; this UC composes existing authorized endpoints.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Select an authorized consultation/conversation context. | `COND-01` | `UC-EX-12-TC-001` |
| Load shared metrics and checklist projections. | `COND-02` | `UC-EX-12-TC-002` |
| Review current records without mutating the mother's canonical data. | `COND-03` | `UC-EX-12-TC-003` |
| Composition call 1: GET `/api/v1/direct-conversations` | `COND-API-001` | `UC-EX-12-TC-API-001` |
| Composition call 2: GET `/api/v1/direct-conversations/{conversationId}/timeline?limit=50` | `COND-API-002` | `UC-EX-12-TC-API-002` |
| Composition call 3: GET `/api/v1/journeys/{journeyId}/metrics?metricType={code}` | `COND-API-003` | `UC-EX-12-TC-API-003` |
| Composition call 4: GET `/api/v1/checklists/journeys/{journeyId}/tasks` | `COND-API-004` | `UC-EX-12-TC-API-004` |
| Composition call 5: GET `/api/v1/checklists/users/{motherUserId}/tasks` | `COND-API-005` | `UC-EX-12-TC-API-005` |
| Business-rule partitions | `COND-BR-*` | `UC-EX-12-TC-BR-*` |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-EX-12-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-EX-12-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-EX-12-TC-001` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-002` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-003` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-API-001` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-API-002` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-API-003` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-API-004` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-API-005` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-BR-001` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-BR-002` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-SEC-001` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-EX-12-TC-GAP-001` | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` | Not run | Not run | Not run | 🔴 Not written / rerun required |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

1. Write the narrowest applicable test from Section 4.
2. Run the exact repository-supported command.
3. Confirm the intended behavioral failure, not setup/environment noise.
4. Record command, revision, time, environment, and failure signature.
5. Implement the smallest approved change during implementation, not specification.
6. Rerun for Green and then run affected suites after refactor.

No row above is Green until current execution evidence is recorded.

## 6. Entry, Exit, and Suspension Criteria

### Entry

- [ ] Paired TDS has all 17 sections.
- [ ] Exact DTO fields, error codes, states, auth, events, and schema used by a test are sourced or explicitly Open.
- [ ] Design-changing contradictions/gaps are resolved before implementation.
- [ ] Synthetic fixtures and deterministic provider/device controls are available.

### Exit

- [ ] Every operation and business rule maps to a condition and detailed TC.
- [ ] Every applicable critical/high case has current Red/Green/refactor evidence.
- [ ] Targeted and affected suites pass with recorded commands/counts.
- [ ] No real protected data/secrets appear in fixtures, logs, or snapshots.
- [ ] Reviewer/approver sign-off is recorded.

### Suspension

Suspend when the expected behavior/source conflicts, the environment cannot distinguish product failure, testing risks real credentials/protected data, or a destructive/shared migration procedure would be required. Resume only after the oracle/environment/safe procedure is restored.

## 7. Rollback Plan

| Artifact / risk | Safe action | Verification |
| --- | --- | --- |
| Test code | Revert only feature-owned tests/factories on the working branch. | Existing focused baseline returns to its prior result. |
| Fixtures/config | Restore versioned test config; remove only feature-owned synthetic data. | Unrelated suites remain isolated. |
| Client/server change | Use the paired TDS rollback runbook and preserve compatible contracts. | Targeted contract/UI tests. |
| Schema | Use additive corrective migration or recreate isolated test DB; never edit applied Flyway history. | Migration validation and integrity checks. |
| Provider | Disable optional integration/use approved degraded behavior only. | Fake/sandbox contract test. |

## 8. CASE 2.0 Anti-Pattern Detection

| Anti-pattern | Required evidence | Draft result |
| --- | --- | --- |
| Hallucinated oracle | Every expected row cites SRS/TDS/exact current source. | Pass for extracted handler/DTO/status expectations; unresolved service-only codes remain explicitly identified in paired TDS Section 10. |
| Generic test matrix | Case titles/actions reference `Review Shared Maternal Metrics and Checklists` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Open gaps recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Not applicable — no clinical/moderation generation in this UC |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
