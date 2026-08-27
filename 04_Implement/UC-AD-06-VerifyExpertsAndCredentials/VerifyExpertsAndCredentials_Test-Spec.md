# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Verify Experts and Credentials

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-06-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-AD-06` |
| Module | `Administration and Operations` |
| Paired TDS | `UC-AD-06-TDS` |
| Priority | `Medium` |
| Platforms | `Web / Backend / File Storage` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-AD-06` and exact code/test sources below |

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
| Actor goal | Review expert profiles, identity and credential evidence, record review decisions, and approve/reject/trust eligible experts. | SRS `UC-AD-06` |
| Current state | `High` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | Web `/admin/experts*` and `/admin/expert-verification-queue` | Current client/router evidence |
| Authorization boundary | `System Admin / Authorized Reviewer` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Primary operations | Load the verification queue and open an applicant case.; Review purpose-authorized identity/credential evidence and registry results.; Record the eligible decision and resulting profile/trust state. | SRS `UC-AD-06` Normal Flow |
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
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-06 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-AD-06-TDS` | 0.1 | Draft design |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/registry/RegistryMatcherTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-04` | Existing test | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-ERROR-01` | Current exception advice | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java` | Worktree `2026-08-23` | Current-state evidence |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | Broad 43-UC catalogue previously obscured this boundary | Generic TCs could not map to `Verify Experts and Credentials` | Split as `UC-AD-06` using the audited current code boundary | SRS 3.1 and `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Resolved in Draft |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Load the verification queue and open an applicant case. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Review purpose-authorized identity/credential evidence and registry results. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Record the eligible decision and resulting profile/trust state. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
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
| `BASIS-01` | `UC-AD-06-FR-01` — Load the verification queue and open an applicant case. | SRS `UC-AD-06` Normal Flow 1; TDS Section 2 | Load the verification queue and open an applicant case. | `COND-01` |
| `BASIS-02` | `UC-AD-06-FR-02` — Review purpose-authorized identity/credential evidence and registry results. | SRS `UC-AD-06` Normal Flow 2; TDS Section 2 | Review purpose-authorized identity/credential evidence and registry results. | `COND-02` |
| `BASIS-03` | `UC-AD-06-FR-03` — Record the eligible decision and resulting profile/trust state. | SRS `UC-AD-06` Normal Flow 3; TDS Section 2 | Record the eligible decision and resulting profile/trust state. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Load the verification queue and open an applicant case. | Web / Backend / File Storage | Positive + applicable boundary/state coverage | `UC-AD-06-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Review purpose-authorized identity/credential evidence and registry results. | Web / Backend / File Storage | Positive + applicable boundary/state coverage | `UC-AD-06-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Record the eligible decision and resulting profile/trust state. | Web / Backend / File Storage | Positive + applicable boundary/state coverage | `UC-AD-06-TC-003` |
| `COND-API-001` | Exact handler/client composition contract | GET /api/v1/expert/admin/profiles → getAdminProfiles contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-001` |
| `COND-API-002` | Exact handler/client composition contract | GET /api/v1/expert/credentials/pending → getPendingReviews contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-002` |
| `COND-API-003` | Exact handler/client composition contract | GET /api/v1/expert/credentials/{credentialId} → getCredentialDetail contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-003` |
| `COND-API-004` | Exact handler/client composition contract | GET /api/v1/expert/credentials/{credentialId}/file → getCredentialFile contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-004` |
| `COND-API-005` | Exact handler/client composition contract | GET /api/v1/expert/credentials/{credentialId}/preview → previewCredential contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-005` |
| `COND-API-006` | Exact handler/client composition contract | PUT /api/v1/expert/credentials/{credentialId}/review → reviewCredential contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-006` |
| `COND-API-006-VAL` | Exact handler/client composition contract | PUT /api/v1/expert/credentials/{credentialId}/review rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-006-VAL` |
| `COND-API-007` | Exact handler/client composition contract | GET /api/v1/expert/identity/files/{fileId}/url → getIdentityFileUrl contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-007` |
| `COND-API-008` | Exact handler/client composition contract | GET /api/v1/expert/identity/pending → getPendingReviews contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-008` |
| `COND-API-009` | Exact handler/client composition contract | PUT /api/v1/expert/identity/{attemptId}/review → review contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-009` |
| `COND-API-009-VAL` | Exact handler/client composition contract | PUT /api/v1/expert/identity/{attemptId}/review rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-009-VAL` |
| `COND-API-010` | Exact handler/client composition contract | POST /api/v1/expert/profiles/{expertProfileId}/approve → approveExpert contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-010` |
| `COND-API-011` | Exact handler/client composition contract | PATCH /api/v1/expert/profiles/{expertProfileId}/expert-type → setExpertType contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-011` |
| `COND-API-012` | Exact handler/client composition contract | POST /api/v1/expert/profiles/{expertProfileId}/reject → rejectExpert contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-012` |
| `COND-API-012-VAL` | Exact handler/client composition contract | POST /api/v1/expert/profiles/{expertProfileId}/reject rejects a declared request-field boundary | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-012-VAL` |
| `COND-API-013` | Exact handler/client composition contract | PATCH /api/v1/expert/profiles/{expertProfileId}/trust → setTrustStatus contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-013` |
| `COND-API-014` | Exact handler/client composition contract | GET /api/v1/expert/review-cases → getReviewCases contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-014` |
| `COND-API-015` | Exact handler/client composition contract | GET /api/v1/expert/review-cases/{expertProfileId} → getReviewCase contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-06-TC-API-015` |
| `COND-BR-01` | `BR-01` | Backend verification state, not UI state, grants expert eligibility. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` | Negative / decision / state | `UC-AD-06-TC-BR-001` |
| `COND-BR-02` | `BR-02` | Sensitive evidence access is purpose-bound and review decisions are audited. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` | Negative / decision / state | `UC-AD-06-TC-BR-002` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-AD-06-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-AD-06-TC-GAP-001` |

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
| Actor | Synthetic `System Admin / Authorized Reviewer` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/registry/RegistryMatcherTest.java`
- `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx`

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

### UC-AD-06-TC-001 — Load the verification queue and open an applicant case

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-001` |
| Severity | `High` |
| Test Condition | `COND-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-06 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Load the verification queue and open an applicant case.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Load the verification queue and open an applicant case.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-AD-06 Normal Flow 1; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Load the verification queue and open an applicant case.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-002 — Review purpose-authorized identity/credential evidence and registry results

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-002` |
| Severity | `High` |
| Test Condition | `COND-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-06 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Review purpose-authorized identity/credential evidence and registry results.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Review purpose-authorized identity/credential evidence and registry results.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-AD-06 Normal Flow 2; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Review purpose-authorized identity/credential evidence and registry results.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-003 — Record the eligible decision and resulting profile/trust state

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-003` |
| Severity | `High` |
| Test Condition | `COND-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-06 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/registry/RegistryMatcherTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Record the eligible decision and resulting profile/trust state.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Record the eligible decision and resulting profile/trust state.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-AD-06 Normal Flow 3; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Record the eligible decision and resulting profile/trust state.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-001 — GET /api/v1/expert/admin/profiles → getAdminProfiles contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-001` |
| Severity | `High` |
| Test Condition | `COND-API-001` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-001`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/expert/admin/profiles` so `getAdminProfiles` receives query `status`: `String`; query `keyword`: `String`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<List<ExpertProfileResponse>>>` with payload fields `expertProfileId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `specialtyId`: `String` (no field annotation in current DTO); `specialtyIds`: `List<String>` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `workplaceProvinceId`: `String` (no field annotation in current DTO); `hospitalId`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `trustStatus`: `TrustStatus` (no field annotation in current DTO); `expertType`: `ExpertType` (no field annotation in current DTO); `contracted`: `boolean` (no field annotation in current DTO); `isConsultationEligible`: `boolean` (no field annotation in current DTO); `verifiedAt`: `LocalDateTime` (no field annotation in current DTO); `verifiedBy`: `UUID` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<List<ExpertProfileResponse>>>` with payload fields `expertProfileId`: `UUID` (no field annotation in current DTO); `userId`: `UUID` (no field annotation in current DTO); `displayName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `specialtyId`: `String` (no field annotation in current DTO); `specialtyIds`: `List<String>` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `workplaceProvinceId`: `String` (no field annotation in current DTO); `hospitalId`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `verificationStatus`: `VerificationStatus` (no field annotation in current DTO); `trustStatus`: `TrustStatus` (no field annotation in current DTO); `expertType`: `ExpertType` (no field annotation in current DTO); `contracted`: `boolean` (no field annotation in current DTO); `isConsultationEligible`: `boolean` (no field annotation in current DTO); `verifiedAt`: `LocalDateTime` (no field annotation in current DTO); `verifiedBy`: `UUID` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `consultationFeeVnd`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-002 — GET /api/v1/expert/credentials/pending → getPendingReviews contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-002` |
| Severity | `High` |
| Test Condition | `COND-API-002` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertCredentialControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/expert/credentials/pending` so `getPendingReviews` receives principal `principal`: `Principal`; query `credentialType`: `String`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<List<DocumentReviewResponse>>>` with payload fields `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `fileName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `Long` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `LocalDateTime` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<List<DocumentReviewResponse>>>` with payload fields `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `fileName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `Long` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `LocalDateTime` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-003 — GET /api/v1/expert/credentials/{credentialId} → getCredentialDetail contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-003` |
| Severity | `High` |
| Test Condition | `COND-API-003` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertCredentialControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/expert/credentials/{credentialId}` so `getCredentialDetail` receives principal `principal`: `Principal`; path `credentialId`: `UUID`; satisfy `hasRole('EXPERT')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<CredentialResponse>>` with payload fields `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<CredentialResponse>>` with payload fields `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `updatedAt`: `LocalDateTime` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-004 — GET /api/v1/expert/credentials/{credentialId}/file → getCredentialFile contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-004` |
| Severity | `High` |
| Test Condition | `COND-API-004` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/expert/credentials/{credentialId}/file` so `getCredentialFile` receives principal `principal`: `Principal`; path `credentialId`: `UUID`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<ViewFileResponse>>` with payload fields `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<ViewFileResponse>>` with payload fields `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-005 — GET /api/v1/expert/credentials/{credentialId}/preview → previewCredential contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-005` |
| Severity | `High` |
| Test Condition | `COND-API-005` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/expert/credentials/{credentialId}/preview` so `previewCredential` receives principal `principal`: `Principal`; path `credentialId`: `UUID`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<CredentialDocumentPreviewResponse>>` with payload fields No serializable fields extracted from `main/java/com/carebridge/backend/expertverification/dto/response/CredentialDocumentPreviewResponse.java`; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<CredentialDocumentPreviewResponse>>` with payload fields No serializable fields extracted from `main/java/com/carebridge/backend/expertverification/dto/response/CredentialDocumentPreviewResponse.java`; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-006 — PUT /api/v1/expert/credentials/{credentialId}/review → reviewCredential contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-006` |
| Severity | `High` |
| Test Condition | `COND-API-006` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-006`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PUT /api/v1/expert/credentials/{credentialId}/review` so `reviewCredential` receives principal `principal`: `Principal`; path `credentialId`: `UUID`; body `request`: `ReviewCredentialRequest`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `ReviewCredentialRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<DocumentReviewResponse>>` with payload fields `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `fileName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `Long` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `LocalDateTime` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); the request body is `ReviewCredentialRequest` with `reviewStatus`: `ReviewStatus` (@NotNull); `reviewNote`: `String` (@Size(max = 2000)). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<DocumentReviewResponse>>` with payload fields `credentialId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `credentialType`: `String` (no field annotation in current DTO); `credentialNumber`: `String` (no field annotation in current DTO); `issuer`: `String` (no field annotation in current DTO); `issuedDate`: `LocalDate` (no field annotation in current DTO); `expiryDate`: `LocalDate` (no field annotation in current DTO); `fileUrl`: `String` (no field annotation in current DTO); `fileId`: `UUID` (no field annotation in current DTO); `fileName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `Long` (no field annotation in current DTO); `createdAt`: `LocalDateTime` (no field annotation in current DTO); `reviewStatus`: `ReviewStatus` (no field annotation in current DTO); `reviewNote`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `LocalDateTime` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `phone`: `String` (no field annotation in current DTO); `email`: `String` (no field annotation in current DTO); `ratingAvg`: `BigDecimal` (no field annotation in current DTO); `avatarUrl`: `String` (no field annotation in current DTO); the request body is `ReviewCredentialRequest` with `reviewStatus`: `ReviewStatus` (@NotNull); `reviewNote`: `String` (@Size(max = 2000)).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-006-VAL — PUT /api/v1/expert/credentials/{credentialId}/review rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-006-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-006-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-006-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `ReviewCredentialRequest` with one field violating its cited validator: `reviewStatus`: `ReviewStatus` (@NotNull); `reviewNote`: `String` (@Size(max = 2000))

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-007 — GET /api/v1/expert/identity/files/{fileId}/url → getIdentityFileUrl contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-007` |
| Severity | `High` |
| Test Condition | `COND-API-007` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-007`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/expert/identity/files/{fileId}/url` so `getIdentityFileUrl` receives principal `principal`: `Principal`; path `fileId`: `UUID`; satisfy `hasAnyRole('EXPERT', 'SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<ViewFileResponse>>` with payload fields `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<ViewFileResponse>>` with payload fields `fileId`: `UUID` (no field annotation in current DTO); `originalName`: `String` (no field annotation in current DTO); `mimeType`: `String` (no field annotation in current DTO); `fileSizeBytes`: `long` (no field annotation in current DTO); `presignedUrl`: `String` (no field annotation in current DTO); `status`: `String` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-008 — GET /api/v1/expert/identity/pending → getPendingReviews contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-008` |
| Severity | `High` |
| Test Condition | `COND-API-008` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-008`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/expert/identity/pending` so `getPendingReviews` receives No explicit handler parameter; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<List<IdentityVerificationResponse>>>` with payload fields `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<List<IdentityVerificationResponse>>>` with payload fields `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-009 — PUT /api/v1/expert/identity/{attemptId}/review → review contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-009` |
| Severity | `High` |
| Test Condition | `COND-API-009` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-009`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PUT /api/v1/expert/identity/{attemptId}/review` so `review` receives principal `principal`: `Principal`; path `attemptId`: `UUID`; body `request`: `ReviewIdentityRequest`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `ReviewIdentityRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<IdentityVerificationResponse>>` with payload fields `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `ReviewIdentityRequest` with `reviewStatus`: `IdentityReviewStatus` (@NotNull); `reason`: `String` (@Size(max = 2000)). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<IdentityVerificationResponse>>` with payload fields `identityVerificationId`: `UUID` (no field annotation in current DTO); `expertProfileId`: `UUID` (no field annotation in current DTO); `expertName`: `String` (no field annotation in current DTO); `expertEmail`: `String` (no field annotation in current DTO); `expertPhone`: `String` (no field annotation in current DTO); `specialty`: `String` (no field annotation in current DTO); `professionalTitle`: `String` (no field annotation in current DTO); `experienceYears`: `Integer` (no field annotation in current DTO); `workplace`: `String` (no field annotation in current DTO); `consultationScope`: `String` (no field annotation in current DTO); `selfieFileId`: `UUID` (no field annotation in current DTO); `identityFrontFileId`: `UUID` (no field annotation in current DTO); `identityBackFileId`: `UUID` (no field annotation in current DTO); `selfieCropFileId`: `UUID` (no field annotation in current DTO); `idCardCropFileId`: `UUID` (no field annotation in current DTO); `faceStatus`: `FaceVerificationStatus` (no field annotation in current DTO); `faceSimilarity`: `BigDecimal` (no field annotation in current DTO); `faceThreshold`: `BigDecimal` (no field annotation in current DTO); `providerErrorCode`: `String` (no field annotation in current DTO); `reviewStatus`: `IdentityReviewStatus` (no field annotation in current DTO); `reviewReason`: `String` (no field annotation in current DTO); `reviewedBy`: `UUID` (no field annotation in current DTO); `reviewedAt`: `Instant` (no field annotation in current DTO); `createdAt`: `Instant` (no field annotation in current DTO); the request body is `ReviewIdentityRequest` with `reviewStatus`: `IdentityReviewStatus` (@NotNull); `reason`: `String` (@Size(max = 2000)).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-009-VAL — PUT /api/v1/expert/identity/{attemptId}/review rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-009-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-009-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-009-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `ReviewIdentityRequest` with one field violating its cited validator: `reviewStatus`: `IdentityReviewStatus` (@NotNull); `reason`: `String` (@Size(max = 2000))

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-010 — POST /api/v1/expert/profiles/{expertProfileId}/approve → approveExpert contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-010` |
| Severity | `High` |
| Test Condition | `COND-API-010` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-010`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/expert/profiles/{expertProfileId}/approve` so `approveExpert` receives path `expertProfileId`: `UUID`; query `expertType`: `ExpertType`; principal `principal`: `Principal`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<Void>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<Void>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-011 — PATCH /api/v1/expert/profiles/{expertProfileId}/expert-type → setExpertType contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-011` |
| Severity | `High` |
| Test Condition | `COND-API-011` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-011`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PATCH /api/v1/expert/profiles/{expertProfileId}/expert-type` so `setExpertType` receives path `expertProfileId`: `UUID`; query `type`: `ExpertType`; principal `principal`: `Principal`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<Void>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<Void>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-012 — POST /api/v1/expert/profiles/{expertProfileId}/reject → rejectExpert contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-012` |
| Severity | `High` |
| Test Condition | `COND-API-012` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-012`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/expert/profiles/{expertProfileId}/reject` so `rejectExpert` receives path `expertProfileId`: `UUID`; principal `principal`: `Principal`; body `request`: `RejectExpertRequest`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `RejectExpertRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<Void>>` with payload fields Not applicable or unresolved from the handler import; the request body is `RejectExpertRequest` with `reason`: `String` (@NotBlank, @Size(max = 2000)). | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<Void>>` with payload fields Not applicable or unresolved from the handler import; the request body is `RejectExpertRequest` with `reason`: `String` (@NotBlank, @Size(max = 2000)).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-012-VAL — POST /api/v1/expert/profiles/{expertProfileId}/reject rejects a declared request-field boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-012-VAL` |
| Severity | `High` |
| Test Condition | `COND-API-012-VAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-012-VAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit `RejectExpertRequest` with one field violating its cited validator: `reason`: `String` (@NotBlank, @Size(max = 2000))

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/exception/GlobalExceptionHandler.java#handleMethodArgumentNotValid` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP `400` returns the current `ErrorResponse` with `status=400`, `error=VALIDATION_ERROR`, `message=Invalid request`, the request path, and field details with sensitive rejected values masked; the handler/service is not invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-013 — PATCH /api/v1/expert/profiles/{expertProfileId}/trust → setTrustStatus contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-013` |
| Severity | `High` |
| Test Condition | `COND-API-013` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-013`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `PATCH /api/v1/expert/profiles/{expertProfileId}/trust` so `setTrustStatus` receives path `expertProfileId`: `UUID`; principal `principal`: `Principal`; query `status`: `TrustStatus`; satisfy `hasAnyRole('SYSTEM_ADMIN', 'CONTENT_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<Void>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<Void>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-014 — GET /api/v1/expert/review-cases → getReviewCases contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-014` |
| Severity | `High` |
| Test Condition | `COND-API-014` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-014`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/expert/review-cases` so `getReviewCases` receives principal `principal`: `Principal`; query `search`: `String`; query `status`: `String`; context `pageable`: `org.springframework.data.domain.Pageable`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<org.springframework.data.domain.Page<ExpertReviewCaseResponse>>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<org.springframework.data.domain.Page<ExpertReviewCaseResponse>>>` with payload fields Not applicable or unresolved from the handler import; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-API-015 — GET /api/v1/expert/review-cases/{expertProfileId} → getReviewCase contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-API-015` |
| Severity | `High` |
| Test Condition | `COND-API-015` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationControllerTest.java` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-015`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/expert/review-cases/{expertProfileId}` so `getReviewCase` receives principal `principal`: `Principal`; path `expertProfileId`: `UUID`; satisfy `hasRole('SYSTEM_ADMIN')` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `ResponseEntity<ApiResponse<ExpertReviewCaseResponse>>` with payload fields No serializable fields extracted from `main/java/com/carebridge/backend/expertverification/dto/response/ExpertReviewCaseResponse.java`; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `ResponseEntity<ApiResponse<ExpertReviewCaseResponse>>` with payload fields No serializable fields extracted from `main/java/com/carebridge/backend/expertverification/dto/response/ExpertReviewCaseResponse.java`; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-BR-001 — Enforce business rule 1

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-BR-001` |
| Severity | `High` |
| Test Condition | `COND-BR-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Backend verification state, not UI state, grants expert eligibility.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Backend verification state, not UI state, grants expert eligibility. No disallowed state or protected data is produced. | `TDS BR-01; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Backend verification state, not UI state, grants expert eligibility. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-BR-002 — Enforce business rule 2

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-BR-002` |
| Severity | `High` |
| Test Condition | `COND-BR-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Sensitive evidence access is purpose-bound and review decisions are audited.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Sensitive evidence access is purpose-bound and review decisions are audited. No disallowed state or protected data is produced. | `TDS BR-02; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Sensitive evidence access is purpose-bound and review decisions are audited. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-06-TC-GAP-001 — Prove the current actor entry path and owning contract remain reachable

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-06-TC-GAP-001` |
| Severity | `Medium` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Web / Backend / File Storage` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-06 Implemented Entry Points/Contracts; 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx` |
| Preconditions | Synthetic `System Admin / Authorized Reviewer` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Start from `Web `/admin/experts*` and `/admin/expert-verification-queue``, perform `Load the verification queue and open an applicant case.`, and observe the owning contract `GET `/api/v1/expert/admin/profiles`` rather than a retired or unrelated route.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The current entry path remains reachable for `Verify Experts and Credentials` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case. | `SRS UC-AD-06 Implemented Entry Points/Contracts; 05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-06` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The current entry path remains reachable for `Verify Experts and Credentials` and invokes only the documented owning contract; an unreachable, static, or wrong-method path fails this case.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Load the verification queue and open an applicant case. | `COND-01` | `UC-AD-06-TC-001` |
| Review purpose-authorized identity/credential evidence and registry results. | `COND-02` | `UC-AD-06-TC-002` |
| Record the eligible decision and resulting profile/trust state. | `COND-03` | `UC-AD-06-TC-003` |
| GET /api/v1/expert/admin/profiles → getAdminProfiles contract | `COND-API-001` | `UC-AD-06-TC-API-001` |
| GET /api/v1/expert/credentials/pending → getPendingReviews contract | `COND-API-002` | `UC-AD-06-TC-API-002` |
| GET /api/v1/expert/credentials/{credentialId} → getCredentialDetail contract | `COND-API-003` | `UC-AD-06-TC-API-003` |
| GET /api/v1/expert/credentials/{credentialId}/file → getCredentialFile contract | `COND-API-004` | `UC-AD-06-TC-API-004` |
| GET /api/v1/expert/credentials/{credentialId}/preview → previewCredential contract | `COND-API-005` | `UC-AD-06-TC-API-005` |
| PUT /api/v1/expert/credentials/{credentialId}/review → reviewCredential contract | `COND-API-006` | `UC-AD-06-TC-API-006` |
| PUT /api/v1/expert/credentials/{credentialId}/review rejects a declared request-field boundary | `COND-API-006-VAL` | `UC-AD-06-TC-API-006-VAL` |
| GET /api/v1/expert/identity/files/{fileId}/url → getIdentityFileUrl contract | `COND-API-007` | `UC-AD-06-TC-API-007` |
| GET /api/v1/expert/identity/pending → getPendingReviews contract | `COND-API-008` | `UC-AD-06-TC-API-008` |
| PUT /api/v1/expert/identity/{attemptId}/review → review contract | `COND-API-009` | `UC-AD-06-TC-API-009` |
| PUT /api/v1/expert/identity/{attemptId}/review rejects a declared request-field boundary | `COND-API-009-VAL` | `UC-AD-06-TC-API-009-VAL` |
| POST /api/v1/expert/profiles/{expertProfileId}/approve → approveExpert contract | `COND-API-010` | `UC-AD-06-TC-API-010` |
| PATCH /api/v1/expert/profiles/{expertProfileId}/expert-type → setExpertType contract | `COND-API-011` | `UC-AD-06-TC-API-011` |
| POST /api/v1/expert/profiles/{expertProfileId}/reject → rejectExpert contract | `COND-API-012` | `UC-AD-06-TC-API-012` |
| POST /api/v1/expert/profiles/{expertProfileId}/reject rejects a declared request-field boundary | `COND-API-012-VAL` | `UC-AD-06-TC-API-012-VAL` |
| PATCH /api/v1/expert/profiles/{expertProfileId}/trust → setTrustStatus contract | `COND-API-013` | `UC-AD-06-TC-API-013` |
| GET /api/v1/expert/review-cases → getReviewCases contract | `COND-API-014` | `UC-AD-06-TC-API-014` |
| GET /api/v1/expert/review-cases/{expertProfileId} → getReviewCase contract | `COND-API-015` | `UC-AD-06-TC-API-015` |
| Business-rule partitions | `COND-BR-*` | `UC-AD-06-TC-BR-*` |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-AD-06-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-AD-06-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-AD-06-TC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-003` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-002` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertCredentialControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-003` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertCredentialControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-004` | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-005` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-006` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-006-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-007` | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-008` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-009` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-009-VAL` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-010` | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-011` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-012` | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-012-VAL` | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-013` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-014` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-API-015` | Planned — `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationControllerTest.java` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-BR-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-BR-002` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-SEC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-06-TC-GAP-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |

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
| Generic test matrix | Case titles/actions reference `Verify Experts and Credentials` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Resolved broad-boundary issue recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Not applicable — no clinical/moderation generation in this UC |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
