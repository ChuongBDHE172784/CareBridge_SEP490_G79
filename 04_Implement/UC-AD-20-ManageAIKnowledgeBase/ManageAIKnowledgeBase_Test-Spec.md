# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Manage AI Knowledge Base

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-20-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-AD-20` |
| Module | `Administration and Operations` |
| Paired TDS | `UC-AD-20-TDS` |
| Priority | `Medium` |
| Platforms | `Python AI Service / FastAPI Swagger / Database` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-AD-20` and exact code/test sources below |

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
| Actor goal | Inspect knowledge/chunks, upload and ingest supported documents, synchronize/rebuild eligible sources, and delete obsolete knowledge. | SRS `UC-AD-20` |
| Current state | `High` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | FastAPI Swagger `/docs`; no dedicated Web Admin route | Current client/router evidence |
| Authorization boundary | `Authorized Technical Operator` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Primary operations | Authenticate with the configured internal operator key.; Inspect/upload/ingest/synchronize supported knowledge sources.; Delete an eligible source and verify future retrieval state. | SRS `UC-AD-20` Normal Flow |
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
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-20 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-AD-20-TDS` | 0.1 | Draft design |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAITriageService/scripts/ingest_documents.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Worktree `2026-08-23` | Current-state evidence |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | Current operation is API/Swagger-based rather than a role-authenticated Web administration page. | Could create false completed coverage or wrong contract | Keep as current limitation/Open until the cited client/server mismatch is resolved | SRS `UC-AD-20` gap plus current code | Open |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Authenticate with the configured internal operator key. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Inspect/upload/ingest/synchronize supported knowledge sources. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Delete an eligible source and verify future retrieval state. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
| `RISK-AUTH` | Cross-user/role/member/consent data access | Critical | Medium | Medium | Security/Integration/Contract | `COND-AUTH` |
| `RISK-GAP` | Documentation claims an unreachable or broken path as complete | High | Medium | High | Characterization/Contract/UI | `COND-GAP` |

#### Platform and Test-Level Applicability Matrix

| Platform / Layer | Unit | Integration | Contract / Component | Widget / UI | E2E | Security |
| --- | --- | --- | --- | --- | --- | --- |
| Backend | Not applicable — no Spring backend layer in this UC | Not applicable — no Spring backend layer in this UC | Not applicable — no Spring backend layer in this UC | Not applicable — backend has no UI | Not applicable — no Spring backend layer in this UC | Not applicable — no Spring backend layer in this UC |
| Web | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC |
| Mobile | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC | Not applicable — no reachable Mobile consumer in this UC |
| AI Service | Applicable — current Python AI contracts | Applicable — current Python AI contracts | Applicable — current Python AI contracts | Not applicable — Python service has no actor UI | Applicable — current Python AI contracts | Applicable — current Python AI contracts |

### TDS-02 — Test Basis and Oracle Hierarchy

| Basis | Requirement / behavior | Exact source | Oracle | Conditions |
| --- | --- | --- | --- | --- |
| `BASIS-01` | `UC-AD-20-FR-01` — Authenticate with the configured internal operator key. | SRS `UC-AD-20` Normal Flow 1; TDS Section 2 | Authenticate with the configured internal operator key. | `COND-01` |
| `BASIS-02` | `UC-AD-20-FR-02` — Inspect/upload/ingest/synchronize supported knowledge sources. | SRS `UC-AD-20` Normal Flow 2; TDS Section 2 | Inspect/upload/ingest/synchronize supported knowledge sources. | `COND-02` |
| `BASIS-03` | `UC-AD-20-FR-03` — Delete an eligible source and verify future retrieval state. | SRS `UC-AD-20` Normal Flow 3; TDS Section 2 | Delete an eligible source and verify future retrieval state. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Authenticate with the configured internal operator key. | Python AI Service / FastAPI Swagger / Database | Positive + applicable boundary/state coverage | `UC-AD-20-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Inspect/upload/ingest/synchronize supported knowledge sources. | Python AI Service / FastAPI Swagger / Database | Positive + applicable boundary/state coverage | `UC-AD-20-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Delete an eligible source and verify future retrieval state. | Python AI Service / FastAPI Swagger / Database | Positive + applicable boundary/state coverage | `UC-AD-20-TC-003` |
| `COND-API-001` | Exact handler/client composition contract | DELETE /api/v1/documents/by-title → delete_document_by_title contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-20-TC-API-001` |
| `COND-API-002` | Exact handler/client composition contract | DELETE /api/v1/documents/clear-all → clear_all_knowledge contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-20-TC-API-002` |
| `COND-API-003` | Exact handler/client composition contract | GET /api/v1/documents/files → list_raw_files contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-20-TC-API-003` |
| `COND-API-004` | Exact handler/client composition contract | POST /api/v1/documents/ingest-text → ingest_raw_text contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-20-TC-API-004` |
| `COND-API-005` | Exact handler/client composition contract | GET /api/v1/documents/list → list_knowledge_chunks contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-20-TC-API-005` |
| `COND-API-006` | Exact handler/client composition contract | POST /api/v1/documents/search-vector → simulate_vector_search contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-20-TC-API-006` |
| `COND-API-007` | Exact handler/client composition contract | GET /api/v1/documents/stats → get_knowledge_statistics contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-20-TC-API-007` |
| `COND-API-008` | Exact handler/client composition contract | POST /api/v1/documents/sync-directory → sync_raw_documents_directory contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-20-TC-API-008` |
| `COND-API-009` | Exact handler/client composition contract | POST /api/v1/documents/upload → upload_document_file contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-AD-20-TC-API-009` |
| `COND-BR-01` | `BR-01` | All operations require the configured internal API key. | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | Negative / decision / state | `UC-AD-20-TC-BR-001` |
| `COND-BR-02` | `BR-02` | File type/size/name validation and curated source metadata are required. | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | Negative / decision / state | `UC-AD-20-TC-BR-002` |
| `COND-BR-03` | `BR-03` | Deleting knowledge changes future retrieval but does not prove generated answers are error-free. | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | Negative / decision / state | `UC-AD-20-TC-BR-003` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-AD-20-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-AD-20-TC-GAP-001` |

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
| Actor | Synthetic `Authorized Technical Operator` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py`
- `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py`

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

### UC-AD-20-TC-001 — Authenticate with the configured internal operator key

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-001` |
| Severity | `High` |
| Test Condition | `COND-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-20 Normal Flow 1; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Authenticate with the configured internal operator key.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Authenticate with the configured internal operator key.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-AD-20 Normal Flow 1; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Authenticate with the configured internal operator key.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-002 — Inspect/upload/ingest/synchronize supported knowledge sources

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-002` |
| Severity | `High` |
| Test Condition | `COND-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-20 Normal Flow 2; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Inspect/upload/ingest/synchronize supported knowledge sources.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Inspect/upload/ingest/synchronize supported knowledge sources.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-AD-20 Normal Flow 2; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Inspect/upload/ingest/synchronize supported knowledge sources.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-003 — Delete an eligible source and verify future retrieval state

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-003` |
| Severity | `High` |
| Test Condition | `COND-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-20 Normal Flow 3; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Delete an eligible source and verify future retrieval state.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Delete an eligible source and verify future retrieval state.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-AD-20 Normal Flow 3; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Delete an eligible source and verify future retrieval state.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-API-001 — DELETE /api/v1/documents/by-title → delete_document_by_title contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-API-001` |
| Severity | `High` |
| Test Condition | `COND-API-001` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-001`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `DELETE /api/v1/documents/by-title` so `delete_document_by_title` receives query/path/context `title`: `str`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `dict` with payload fields Not applicable or primitive/framework-managed payload; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `dict` with payload fields Not applicable or primitive/framework-managed payload; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-API-002 — DELETE /api/v1/documents/clear-all → clear_all_knowledge contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-API-002` |
| Severity | `High` |
| Test Condition | `COND-API-002` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-002`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `DELETE /api/v1/documents/clear-all` so `clear_all_knowledge` receives dependency `_auth`: `str`; dependency `db`: `AsyncSession`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `dict` with payload fields Not applicable or primitive/framework-managed payload; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `dict` with payload fields Not applicable or primitive/framework-managed payload; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-API-003 — GET /api/v1/documents/files → list_raw_files contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-API-003` |
| Severity | `High` |
| Test Condition | `COND-API-003` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-003`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/documents/files` so `list_raw_files` receives dependency `_auth`: `str`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `dict` with payload fields Not applicable or primitive/framework-managed payload; the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `dict` with payload fields Not applicable or primitive/framework-managed payload; the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-API-004 — POST /api/v1/documents/ingest-text → ingest_raw_text contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-API-004` |
| Severity | `High` |
| Test Condition | `COND-API-004` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-004`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/documents/ingest-text` so `ingest_raw_text` receives body `request`: `IngestDocumentRequest`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `IngestDocumentRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `IngestDocumentResponse` with payload fields `success`: `bool` (`required`); `message`: `str` (`required`); `total_chunks`: `int` (`required`); `document_title`: `str` (`required`); `stage`: `str` (`required`); the request body is `IngestDocumentRequest` with `title`: `str` (`required`); `stage`: `MaternalStage` (`MaternalStage.ALL`); `topic`: `str` (`"GENERAL"`); `source`: `str` (`"Bộ Y Tế / Cẩm nang Y khoa"`); `section`: `Optional[str]` (`None`); `text_content`: `str` (`required`). | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `IngestDocumentResponse` with payload fields `success`: `bool` (`required`); `message`: `str` (`required`); `total_chunks`: `int` (`required`); `document_title`: `str` (`required`); `stage`: `str` (`required`); the request body is `IngestDocumentRequest` with `title`: `str` (`required`); `stage`: `MaternalStage` (`MaternalStage.ALL`); `topic`: `str` (`"GENERAL"`); `source`: `str` (`"Bộ Y Tế / Cẩm nang Y khoa"`); `section`: `Optional[str]` (`None`); `text_content`: `str` (`required`).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-API-005 — GET /api/v1/documents/list → list_knowledge_chunks contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-API-005` |
| Severity | `High` |
| Test Condition | `COND-API-005` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-005`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/documents/list` so `list_knowledge_chunks` receives query/path/context `stage`: `Optional[MaternalStage]`; query/path/context `topic`: `Optional[str]`; query/path/context `keyword`: `Optional[str]`; query/path/context `page`: `int`; query/path/context `page_size`: `int`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `KnowledgeListResponse` with payload fields `total`: `int` (`required`); `page`: `int` (`required`); `page_size`: `int` (`required`); `items`: `List[ChunkDetailItem]` (`required`); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `KnowledgeListResponse` with payload fields `total`: `int` (`required`); `page`: `int` (`required`); `page_size`: `int` (`required`); `items`: `List[ChunkDetailItem]` (`required`); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-API-006 — POST /api/v1/documents/search-vector → simulate_vector_search contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-API-006` |
| Severity | `High` |
| Test Condition | `COND-API-006` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-006`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/documents/search-vector` so `simulate_vector_search` receives body `request`: `VectorSearchTestRequest`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `VectorSearchTestRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `VectorSearchTestResponse` with payload fields `query`: `str` (`required`); `total_retrieved`: `int` (`required`); `results`: `List[SourceCitation]` (`required`); the request body is `VectorSearchTestRequest` with `query`: `str` (`Field(description="Câu hỏi hoặc triệu chứng cần tìm kiếm trong CSDL vector")`); `stage`: `Optional[MaternalStage]` (`Field(default=None, description="Lọc theo giai đoạn (Tùy chọn)")`); `top_k`: `int` (`Field(default=4, ge=1, le=20, description="Số lượng đoạn tài liệu cần lấy")`). | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `VectorSearchTestResponse` with payload fields `query`: `str` (`required`); `total_retrieved`: `int` (`required`); `results`: `List[SourceCitation]` (`required`); the request body is `VectorSearchTestRequest` with `query`: `str` (`Field(description="Câu hỏi hoặc triệu chứng cần tìm kiếm trong CSDL vector")`); `stage`: `Optional[MaternalStage]` (`Field(default=None, description="Lọc theo giai đoạn (Tùy chọn)")`); `top_k`: `int` (`Field(default=4, ge=1, le=20, description="Số lượng đoạn tài liệu cần lấy")`).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-API-007 — GET /api/v1/documents/stats → get_knowledge_statistics contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-API-007` |
| Severity | `High` |
| Test Condition | `COND-API-007` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-007`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `GET /api/v1/documents/stats` so `get_knowledge_statistics` receives dependency `_auth`: `str`; dependency `db`: `AsyncSession`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `KnowledgeStatsResponse` with payload fields `total_chunks`: `int` (`required`); `total_documents`: `int` (`required`); `stage_distribution`: `Dict[str, int]` (`required`); `topic_distribution`: `Dict[str, int]` (`required`); `files_in_disk`: `List[str]` (`required`); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `KnowledgeStatsResponse` with payload fields `total_chunks`: `int` (`required`); `total_documents`: `int` (`required`); `stage_distribution`: `Dict[str, int]` (`required`); `topic_distribution`: `Dict[str, int]` (`required`); `files_in_disk`: `List[str]` (`required`); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-API-008 — POST /api/v1/documents/sync-directory → sync_raw_documents_directory contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-API-008` |
| Severity | `High` |
| Test Condition | `COND-API-008` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-008`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/documents/sync-directory` so `sync_raw_documents_directory` receives dependency `_auth`: `str`; dependency `db`: `AsyncSession`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `None` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `BatchIngestResponse` with payload fields `success`: `bool` (`required`); `total_files_processed`: `int` (`required`); `total_chunks_created`: `int` (`required`); `processed_files`: `List[str]` (`required`); `errors`: `List[str]` (`Field(default_factory=list)`); the request body is `None` with Not applicable — no request body. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `BatchIngestResponse` with payload fields `success`: `bool` (`required`); `total_files_processed`: `int` (`required`); `total_chunks_created`: `int` (`required`); `processed_files`: `List[str]` (`required`); `errors`: `List[str]` (`Field(default_factory=list)`); the request body is `None` with Not applicable — no request body.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-API-009 — POST /api/v1/documents/upload → upload_document_file contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-API-009` |
| Severity | `High` |
| Test Condition | `COND-API-009` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-009`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/documents/upload` so `upload_document_file` receives file `file`: `UploadFile`; query/path/context `stage`: `MaternalStage`; query/path/context `topic`: `str`; query/path/context `source`: `str`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `UploadFile` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `IngestDocumentResponse` with payload fields `success`: `bool` (`required`); `message`: `str` (`required`); `total_chunks`: `int` (`required`); `document_title`: `str` (`required`); `stage`: `str` (`required`); the request body is `UploadFile` with Not applicable or primitive/framework-managed payload. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `IngestDocumentResponse` with payload fields `success`: `bool` (`required`); `message`: `str` (`required`); `total_chunks`: `int` (`required`); `document_title`: `str` (`required`); `stage`: `str` (`required`); the request body is `UploadFile` with Not applicable or primitive/framework-managed payload.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-BR-001 — Enforce business rule 1

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-BR-001` |
| Severity | `High` |
| Test Condition | `COND-BR-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-01; 05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: All operations require the configured internal API key.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: All operations require the configured internal API key. No disallowed state or protected data is produced. | `TDS BR-01; 05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: All operations require the configured internal API key. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-BR-002 — Enforce business rule 2

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-BR-002` |
| Severity | `High` |
| Test Condition | `COND-BR-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-02; 05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: File type/size/name validation and curated source metadata are required.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: File type/size/name validation and curated source metadata are required. No disallowed state or protected data is produced. | `TDS BR-02; 05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: File type/size/name validation and curated source metadata are required. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-BR-003 — Enforce business rule 3

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-BR-003` |
| Severity | `High` |
| Test Condition | `COND-BR-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-03; 05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Deleting knowledge changes future retrieval but does not prove generated answers are error-free.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Deleting knowledge changes future retrieval but does not prove generated answers are error-free. No disallowed state or protected data is produced. | `TDS BR-03; 05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Deleting knowledge changes future retrieval but does not prove generated answers are error-free. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AD-20-TC-GAP-001 — Characterize the current documented limitation

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AD-20-TC-GAP-001` |
| Severity | `High` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Python AI Service / FastAPI Swagger / Database` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AD-20 Known Gaps / Exclusions; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Preconditions | Synthetic authorized technical operator; configured internal API key; isolated AI-service fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Current operation is API/Swagger-based rather than a role-authenticated Web administration page.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The test records the current limitation without inventing a completed path: Current operation is API/Swagger-based rather than a role-authenticated Web administration page. | `SRS UC-AD-20 Known Gaps / Exclusions; 05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AD-20` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The test records the current limitation without inventing a completed path: Current operation is API/Swagger-based rather than a role-authenticated Web administration page.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Authenticate with the configured internal operator key. | `COND-01` | `UC-AD-20-TC-001` |
| Inspect/upload/ingest/synchronize supported knowledge sources. | `COND-02` | `UC-AD-20-TC-002` |
| Delete an eligible source and verify future retrieval state. | `COND-03` | `UC-AD-20-TC-003` |
| DELETE /api/v1/documents/by-title → delete_document_by_title contract | `COND-API-001` | `UC-AD-20-TC-API-001` |
| DELETE /api/v1/documents/clear-all → clear_all_knowledge contract | `COND-API-002` | `UC-AD-20-TC-API-002` |
| GET /api/v1/documents/files → list_raw_files contract | `COND-API-003` | `UC-AD-20-TC-API-003` |
| POST /api/v1/documents/ingest-text → ingest_raw_text contract | `COND-API-004` | `UC-AD-20-TC-API-004` |
| GET /api/v1/documents/list → list_knowledge_chunks contract | `COND-API-005` | `UC-AD-20-TC-API-005` |
| POST /api/v1/documents/search-vector → simulate_vector_search contract | `COND-API-006` | `UC-AD-20-TC-API-006` |
| GET /api/v1/documents/stats → get_knowledge_statistics contract | `COND-API-007` | `UC-AD-20-TC-API-007` |
| POST /api/v1/documents/sync-directory → sync_raw_documents_directory contract | `COND-API-008` | `UC-AD-20-TC-API-008` |
| POST /api/v1/documents/upload → upload_document_file contract | `COND-API-009` | `UC-AD-20-TC-API-009` |
| Business-rule partitions | `COND-BR-*` | `UC-AD-20-TC-BR-*` |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-AD-20-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-AD-20-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-AD-20-TC-001` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-002` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-003` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-API-001` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-API-002` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-API-003` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-API-004` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-API-005` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-API-006` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-API-007` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-API-008` | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-API-009` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-BR-001` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-BR-002` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-BR-003` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-SEC-001` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AD-20-TC-GAP-001` | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |

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
| Generic test matrix | Case titles/actions reference `Manage AI Knowledge Base` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Open gaps recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Not applicable — no clinical/moderation generation in this UC |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
