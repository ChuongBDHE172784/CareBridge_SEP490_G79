# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Configure AI Moderation Policies

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-19-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-AD-19` |
| Module | `Administration and Operations` |
| Paired TDS | `UC-AD-19-TDS` |
| Priority | `Medium` |
| Platforms | `Web / Backend / Gemini` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-AD-19` and exact code/test sources below |

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
| Actor goal | Manage AI moderation policy versions/status, test policy behavior, and request supported rescans without allowing the model to enforce penalties directly. | SRS `UC-AD-19` |
| Current state | `High` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | Web `/admin/safety-rules` | Current client/router evidence |
| Authorization boundary | `System Admin` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Primary operations | Load current AI moderation policy/status.; Create/update/test an eligible policy version.; Activate or request a bounded rescan and inspect the result. | SRS `UC-AD-19` Normal Flow |
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
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-19 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-AD-19-TDS` | 0.1 | Draft design |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeWebApp/src/features/aiRuleManagement/pages/SafetyRuleManagementPage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationDecisionPolicyTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiContentScanWorkerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-SUPP-01` | Historical architecture/design evidence | `04_Implement/AIContentModeration/AIContentModeration_Architecture-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |
| `SRC-SUPP-02` | Historical verification evidence | `04_Implement/AIContentModeration/AIContentModeration_Verification-Evidence.md` | Worktree `2026-08-23` | Historical evidence only; current code and canonical SRS/TDS override conflicts |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | Broad 43-UC catalogue previously obscured this boundary | Generic TCs could not map to `Configure AI Moderation Policies` | Split as `UC-AD-19` using the audited current code boundary | SRS 3.1 and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` | Resolved in Draft |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Load current AI moderation policy/status. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Create/update/test an eligible policy version. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Activate or request a bounded rescan and inspect the result. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
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
| `BASIS-01` | `UC-AD-19-FR-01` — Load current AI moderation policy/status. | SRS `UC-AD-19` Normal Flow 1; TDS Section 2 | Load current AI moderation policy/status. | `COND-01` |
| `BASIS-02` | `UC-AD-19-FR-02` — Create/update/test an eligible policy version. | SRS `UC-AD-19` Normal Flow 2; TDS Section 2 | Create/update/test an eligible policy version. | `COND-02` |
| `BASIS-03` | `UC-AD-19-FR-03` — Activate or request a bounded rescan and inspect the result. | SRS `UC-AD-19` Normal Flow 3; TDS Section 2 | Activate or request a bounded rescan and inspect the result. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Load current AI moderation policy/status. | Web / Backend / Gemini | Positive + applicable boundary/state coverage | `UC-AD-19-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Create/update/test an eligible policy version. | Web / Backend / Gemini | Positive + applicable boundary/state coverage | `UC-AD-19-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Activate or request a bounded rescan and inspect the result. | Web / Backend / Gemini | Positive + applicable boundary/state coverage | `UC-AD-19-TC-003` |
| `COND-API-001` | Exact handler/client composition contract | GET /api/v1/admin/ai-moderation/policies → listPolicies contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-001` |
| `COND-API-002` | Exact handler/client composition contract | POST /api/v1/admin/ai-moderation/policies → createPolicy contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-002` |
| `COND-API-002-VAL` | Exact handler/client composition contract | POST /api/v1/admin/ai-moderation/policies rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-002-VAL` |
| `COND-API-003` | Exact handler/client composition contract | PUT /api/v1/admin/ai-moderation/policies/{id} → updatePolicy contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-003` |
| `COND-API-003-VAL` | Exact handler/client composition contract | PUT /api/v1/admin/ai-moderation/policies/{id} rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-003-VAL` |
| `COND-API-004` | Exact handler/client composition contract | PATCH /api/v1/admin/ai-moderation/policies/{id}/status → updatePolicyStatus contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-004` |
| `COND-API-004-VAL` | Exact handler/client composition contract | PATCH /api/v1/admin/ai-moderation/policies/{id}/status rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-004-VAL` |
| `COND-API-005` | Exact handler/client composition contract | POST /api/v1/admin/ai-moderation/rescan → rescan contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-005` |
| `COND-API-005-VAL` | Exact handler/client composition contract | POST /api/v1/admin/ai-moderation/rescan rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-005-VAL` |
| `COND-API-006` | Exact handler/client composition contract | GET /api/v1/admin/ai-moderation/status → status contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-006` |
| `COND-API-007` | Exact handler/client composition contract | POST /api/v1/admin/ai-moderation/test → testPolicies contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-007` |
| `COND-API-007-VAL` | Exact handler/client composition contract | POST /api/v1/admin/ai-moderation/test rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-19-TC-API-007-VAL` |
| `COND-BR-01` | `BR-01` | Only System Admin manages policies. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | Negative / decision / state | `UC-AD-19-TC-BR-001` |
| `COND-BR-02` | `BR-02` | Deterministic server policy decides cases; Gemini output is advisory signal and medical symptom text is not automatically a violation. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` | Negative / decision / state | `UC-AD-19-TC-BR-002` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-AD-19-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-AD-19-TC-GAP-001` |

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
| Actor | Synthetic `System Admin` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationDecisionPolicyTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiContentScanWorkerTest.java`

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

### UC-AD-19-TC-001 — Load current AI moderation policy/status

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-001` |
| Severity | `High` |
| Test Condition | `COND-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-19 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Load current AI moderation policy/status.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Load current AI moderation policy/status.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-AD-19 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Load current AI moderation policy/status.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-002 — Create/update/test an eligible policy version

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-002` |
| Severity | `High` |
| Test Condition | `COND-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-19 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Create/update/test an eligible policy version.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Create/update/test an eligible policy version.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-AD-19 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Create/update/test an eligible policy version.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-003 — Activate or request a bounded rescan and inspect the result

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-003` |
| Severity | `High` |
| Test Condition | `COND-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-19 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationDecisionPolicyTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Activate or request a bounded rescan and inspect the result.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Activate or request a bounded rescan and inspect the result.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-AD-19 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Activate or request a bounded rescan and inspect the result.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-001 — GET /api/v1/admin/ai-moderation/policies → listPolicies contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-001` |
| Severity | `High` |
| Test Condition | `COND-API-001` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-001`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/admin/ai-moderation/policies` so `listPolicies` receives query `active`: `Boolean`; query `page`: `int`; query `size`: `int`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<AiPolicyPageResponse>>` with payload fields `content`: `List<AiPolicyResponse>` (no field annotation in current DTO); `totalElements`: `long` (no field annotation in current DTO); `page`: `int` (no field annotation in current DTO); `size`: `int` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<AiPolicyPageResponse>>` with payload fields `content`: `List<AiPolicyResponse>` (no field annotation in current DTO); `totalElements`: `long` (no field annotation in current DTO); `page`: `int` (no field annotation in current DTO); `size`: `int` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-002 — POST /api/v1/admin/ai-moderation/policies → createPolicy contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-002` |
| Severity | `High` |
| Test Condition | `COND-API-002` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/admin/ai-moderation/policies` so `createPolicy` receives body `request`: `CreateAiPolicyRequest`; principal `principal`: `Principal`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `CreateAiPolicyRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `201` returns `ResponseEntity<ApiResponse<AiPolicyResponse>>` with payload fields `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `CreateAiPolicyRequest` with `policyCode`: `String` (@NotBlank, @Pattern(regexp = "[A-Z0-9_]{3,60}")); `name`: `String` (@NotBlank, @Size(max = 150)); `detectionGuidance`: `String` (@NotBlank, @Size(max = 3000)); `violationCategory`: `AiViolationCategory` (@NotNull); `reportCategory`: `ReportCategory` (@NotNull); `severity`: `AiPolicySeverity` (@NotNull); `applicableTargetTypes`: `List<ReportTargetType>` (@NotEmpty); `confidenceThreshold`: `BigDecimal` (@NotNull); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `201` returns `ResponseEntity<ApiResponse<AiPolicyResponse>>` with payload fields `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `CreateAiPolicyRequest` with `policyCode`: `String` (@NotBlank, @Pattern(regexp = "[A-Z0-9_]{3,60}")); `name`: `String` (@NotBlank, @Size(max = 150)); `detectionGuidance`: `String` (@NotBlank, @Size(max = 3000)); `violationCategory`: `AiViolationCategory` (@NotNull); `reportCategory`: `ReportCategory` (@NotNull); `severity`: `AiPolicySeverity` (@NotNull); `applicableTargetTypes`: `List<ReportTargetType>` (@NotEmpty); `confidenceThreshold`: `BigDecimal` (@NotNull); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-002-VAL — POST /api/v1/admin/ai-moderation/policies rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-002-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-002-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `CreateAiPolicyRequest` with one field violating its cited validator: `policyCode`: `String` (@NotBlank, @Pattern(regexp = "[A-Z0-9_]{3,60}")); `name`: `String` (@NotBlank, @Size(max = 150)); `detectionGuidance`: `String` (@NotBlank, @Size(max = 3000)); `violationCategory`: `AiViolationCategory` (@NotNull); `reportCategory`: `ReportCategory` (@NotNull); `severity`: `AiPolicySeverity` (@NotNull); `applicableTargetTypes`: `List<ReportTargetType>` (@NotEmpty); `confidenceThreshold`: `BigDecimal` (@NotNull); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO)

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-003 — PUT /api/v1/admin/ai-moderation/policies/{id} → updatePolicy contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-003` |
| Severity | `High` |
| Test Condition | `COND-API-003` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PUT /api/v1/admin/ai-moderation/policies/{id}` so `updatePolicy` receives path `id`: `UUID`; body `request`: `UpdateAiPolicyRequest`; principal `principal`: `Principal`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `UpdateAiPolicyRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<AiPolicyResponse>>` with payload fields `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateAiPolicyRequest` with `name`: `String` (@Size(max = 150)); `detectionGuidance`: `String` (@Size(max = 3000)); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<AiPolicyResponse>>` with payload fields `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateAiPolicyRequest` with `name`: `String` (@Size(max = 150)); `detectionGuidance`: `String` (@Size(max = 3000)); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-003-VAL — PUT /api/v1/admin/ai-moderation/policies/{id} rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-003-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-003-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `UpdateAiPolicyRequest` with one field violating its cited validator: `name`: `String` (@Size(max = 150)); `detectionGuidance`: `String` (@Size(max = 3000)); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `Boolean` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO)

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-004 — PATCH /api/v1/admin/ai-moderation/policies/{id}/status → updatePolicyStatus contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-004` |
| Severity | `High` |
| Test Condition | `COND-API-004` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PATCH /api/v1/admin/ai-moderation/policies/{id}/status` so `updatePolicyStatus` receives path `id`: `UUID`; body `request`: `UpdateAiPolicyStatusRequest`; principal `principal`: `Principal`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `UpdateAiPolicyStatusRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<AiPolicyResponse>>` with payload fields `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateAiPolicyStatusRequest` with `active`: `Boolean` (@NotNull). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<AiPolicyResponse>>` with payload fields `id`: `UUID` (no field annotation in current DTO); `policyCode`: `String` (no field annotation in current DTO); `name`: `String` (no field annotation in current DTO); `detectionGuidance`: `String` (no field annotation in current DTO); `violationCategory`: `AiViolationCategory` (no field annotation in current DTO); `reportCategory`: `ReportCategory` (no field annotation in current DTO); `severity`: `AiPolicySeverity` (no field annotation in current DTO); `applicableTargetTypes`: `List<ReportTargetType>` (no field annotation in current DTO); `confidenceThreshold`: `BigDecimal` (no field annotation in current DTO); `active`: `boolean` (no field annotation in current DTO); `systemDefault`: `boolean` (no field annotation in current DTO); `version`: `int` (no field annotation in current DTO); `referenceLinks`: `List<PolicyReferenceLink>` (no field annotation in current DTO); `referenceFiles`: `List<PolicyReferenceFile>` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); `updatedAt`: `Instant` (no field annotation in current DTO); the request body is `UpdateAiPolicyStatusRequest` with `active`: `Boolean` (@NotNull).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-004-VAL — PATCH /api/v1/admin/ai-moderation/policies/{id}/status rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-004-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-004-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `UpdateAiPolicyStatusRequest` with one field violating its cited validator: `active`: `Boolean` (@NotNull)

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-005 — POST /api/v1/admin/ai-moderation/rescan → rescan contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-005` |
| Severity | `High` |
| Test Condition | `COND-API-005` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/admin/ai-moderation/rescan` so `rescan` receives body `request`: `AiRescanRequest`; principal `principal`: `Principal`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `AiRescanRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `202` returns `ResponseEntity<ApiResponse<AiRescanResponse>>` with payload fields `jobId`: `UUID` (no field annotation in current DTO); `queued`: `boolean` (no field annotation in current DTO); the request body is `AiRescanRequest` with `targetType`: `ReportTargetType` (@NotNull); `targetId`: `UUID` (@NotNull). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `202` returns `ResponseEntity<ApiResponse<AiRescanResponse>>` with payload fields `jobId`: `UUID` (no field annotation in current DTO); `queued`: `boolean` (no field annotation in current DTO); the request body is `AiRescanRequest` with `targetType`: `ReportTargetType` (@NotNull); `targetId`: `UUID` (@NotNull).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-005-VAL — POST /api/v1/admin/ai-moderation/rescan rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-005-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-005-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `AiRescanRequest` with one field violating its cited validator: `targetType`: `ReportTargetType` (@NotNull); `targetId`: `UUID` (@NotNull)

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-006 — GET /api/v1/admin/ai-moderation/status → status contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-006` |
| Severity | `High` |
| Test Condition | `COND-API-006` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-006`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/admin/ai-moderation/status` so `status` receives No explicit handler parameter; satisfy `hasAnyRole('SYSTEM_ADMIN', 'MODERATOR')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<AiModerationStatusResponse>>` with payload fields `enabled`: `boolean` (no field annotation in current DTO); `configured`: `boolean` (no field annotation in current DTO); `model`: `String` (no field annotation in current DTO); `resolvedModel`: `String` (no field annotation in current DTO); `state`: `String` (no field annotation in current DTO); `businessToggleEnabled`: `boolean` (no field annotation in current DTO); `queuedJobs`: `long` (no field annotation in current DTO); `processingJobs`: `long` (no field annotation in current DTO); `failedJobs`: `long` (no field annotation in current DTO); `lastCompletedAt`: `Instant` (no field annotation in current DTO); `policySetHash`: `String` (no field annotation in current DTO); `activePolicies`: `long` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<AiModerationStatusResponse>>` with payload fields `enabled`: `boolean` (no field annotation in current DTO); `configured`: `boolean` (no field annotation in current DTO); `model`: `String` (no field annotation in current DTO); `resolvedModel`: `String` (no field annotation in current DTO); `state`: `String` (no field annotation in current DTO); `businessToggleEnabled`: `boolean` (no field annotation in current DTO); `queuedJobs`: `long` (no field annotation in current DTO); `processingJobs`: `long` (no field annotation in current DTO); `failedJobs`: `long` (no field annotation in current DTO); `lastCompletedAt`: `Instant` (no field annotation in current DTO); `policySetHash`: `String` (no field annotation in current DTO); `activePolicies`: `long` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-007 — POST /api/v1/admin/ai-moderation/test → testPolicies contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-007` |
| Severity | `High` |
| Test Condition | `COND-API-007` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-007`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/admin/ai-moderation/test` so `testPolicies` receives body `request`: `AiPolicyTestRequest`; principal `principal`: `Principal`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `AiPolicyTestRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<AiPolicyTestResponse>>` with payload fields `classification`: `AiClassification` (no field annotation in current DTO); `overallSeverity`: `AiPolicySeverity` (no field annotation in current DTO); `confidence`: `BigDecimal` (no field annotation in current DTO); `recommendedAction`: `AiRecommendedAction` (no field annotation in current DTO); `explanation`: `String` (no field annotation in current DTO); `matches`: `List<AiAssessmentMatchResponse>` (no field annotation in current DTO); `wouldCreateCase`: `boolean` (no field annotation in current DTO); `wouldCreatePriority`: `CasePriority` (no field annotation in current DTO); `model`: `String` (no field annotation in current DTO); `latencyMs`: `long` (no field annotation in current DTO); the request body is `AiPolicyTestRequest` with `targetType`: `ReportTargetType` (@NotNull); `sampleText`: `String` (@NotBlank, @Size(max = 5000)). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<AiPolicyTestResponse>>` with payload fields `classification`: `AiClassification` (no field annotation in current DTO); `overallSeverity`: `AiPolicySeverity` (no field annotation in current DTO); `confidence`: `BigDecimal` (no field annotation in current DTO); `recommendedAction`: `AiRecommendedAction` (no field annotation in current DTO); `explanation`: `String` (no field annotation in current DTO); `matches`: `List<AiAssessmentMatchResponse>` (no field annotation in current DTO); `wouldCreateCase`: `boolean` (no field annotation in current DTO); `wouldCreatePriority`: `CasePriority` (no field annotation in current DTO); `model`: `String` (no field annotation in current DTO); `latencyMs`: `long` (no field annotation in current DTO); the request body is `AiPolicyTestRequest` with `targetType`: `ReportTargetType` (@NotNull); `sampleText`: `String` (@NotBlank, @Size(max = 5000)).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-API-007-VAL — POST /api/v1/admin/ai-moderation/test rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-API-007-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-007-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-007-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `AiPolicyTestRequest` with one field violating its cited validator: `targetType`: `ReportTargetType` (@NotNull); `sampleText`: `String` (@NotBlank, @Size(max = 5000))

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-BR-001 — Enforce business rule 1

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-BR-001` |
| Severity | `High` |
| Test Condition | `COND-BR-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Only System Admin manages policies.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Only System Admin manages policies. No disallowed state or protected data is produced. | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Only System Admin manages policies. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-BR-002 — Enforce business rule 2

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-BR-002` |
| Severity | `High` |
| Test Condition | `COND-BR-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Deterministic server policy decides cases; Gemini output is advisory signal and medical symptom text is not automatically a violation.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Deterministic server policy decides cases; Gemini output is advisory signal and medical symptom text is not automatically a violation. No disallowed state or protected data is produced. | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Deterministic server policy decides cases; Gemini output is advisory signal and medical symptom text is not automatically a violation. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-19-TC-GAP-001 — Prove the current actor entry path and owning contract remain reachable

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-19-TC-GAP-001` |
| Severity | `Medium` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / Gemini` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-19 Implemented Entry Points/Contracts; 05_Development/CareBridgeWebApp/src/features/aiRuleManagement/pages/SafetyRuleManagementPage.tsx` |
| Preconditions | Synthetic `System Admin` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Start from `Web `/admin/safety-rules``, perform `Load current AI moderation policy/status.`, and observe the owning contract `Policy/status/test/rescan endpoints under `/api/v1/admin/ai-moderation/**`` rather than a retired or unrelated route.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The current entry path remains reachable for `Configure AI Moderation Policies` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case. | `SRS UC-AD-19 Implemented Entry Points/Contracts; 05_Development/CareBridgeWebApp/src/features/aiRuleManagement/pages/SafetyRuleManagementPage.tsx` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-19` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The current entry path remains reachable for `Configure AI Moderation Policies` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Load current AI moderation policy/status. | `COND-01` | `UC-AD-19-TC-001` |
| Create/update/test an eligible policy version. | `COND-02` | `UC-AD-19-TC-002` |
| Activate or request a bounded rescan and inspect the result. | `COND-03` | `UC-AD-19-TC-003` |
| GET /api/v1/admin/ai-moderation/policies → listPolicies contract | `COND-API-001` | `UC-AD-19-TC-API-001` |
| POST /api/v1/admin/ai-moderation/policies → createPolicy contract | `COND-API-002` | `UC-AD-19-TC-API-002` |
| POST /api/v1/admin/ai-moderation/policies rejects a declared request-field boundary | `COND-API-002-VAL` | `UC-AD-19-TC-API-002-VAL` |
| PUT /api/v1/admin/ai-moderation/policies/{id} → updatePolicy contract | `COND-API-003` | `UC-AD-19-TC-API-003` |
| PUT /api/v1/admin/ai-moderation/policies/{id} rejects a declared request-field boundary | `COND-API-003-VAL` | `UC-AD-19-TC-API-003-VAL` |
| PATCH /api/v1/admin/ai-moderation/policies/{id}/status → updatePolicyStatus contract | `COND-API-004` | `UC-AD-19-TC-API-004` |
| PATCH /api/v1/admin/ai-moderation/policies/{id}/status rejects a declared request-field boundary | `COND-API-004-VAL` | `UC-AD-19-TC-API-004-VAL` |
| POST /api/v1/admin/ai-moderation/rescan → rescan contract | `COND-API-005` | `UC-AD-19-TC-API-005` |
| POST /api/v1/admin/ai-moderation/rescan rejects a declared request-field boundary | `COND-API-005-VAL` | `UC-AD-19-TC-API-005-VAL` |
| GET /api/v1/admin/ai-moderation/status → status contract | `COND-API-006` | `UC-AD-19-TC-API-006` |
| POST /api/v1/admin/ai-moderation/test → testPolicies contract | `COND-API-007` | `UC-AD-19-TC-API-007` |
| POST /api/v1/admin/ai-moderation/test rejects a declared request-field boundary | `COND-API-007-VAL` | `UC-AD-19-TC-API-007-VAL` |
| Business-rule partitions | `COND-BR-*` | `UC-AD-19-TC-BR-*` |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-AD-19-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-AD-19-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-AD-19-TC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-003` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-002-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-003` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-003-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-004` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-004-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-005` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-005-VAL` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-006` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-007` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-API-007-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-BR-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-BR-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-SEC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-19-TC-GAP-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |

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
| Generic test matrix | Case titles/actions reference `Configure AI Moderation Policies` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Resolved broad-boundary issue recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Required |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
