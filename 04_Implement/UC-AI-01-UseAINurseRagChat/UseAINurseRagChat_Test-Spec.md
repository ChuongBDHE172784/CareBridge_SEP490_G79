# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Use AI Nurse RAG Chat

| Field | Value |
| --- | --- |
| Document ID | `UC-AI-01-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-AI-01` |
| Module | `AI Nurse and Clinical Assistance` |
| Paired TDS | `UC-AI-01-TDS` |
| Priority | `High` |
| Platforms | `Mobile / Spring Gateway / Python AI Service` |
| Data Classification | `Restricted mother-only health context; Confidential conversation history; Family-minimized context` |
| Compliance Scope | `PDPA minimization/purpose limitation; role privacy boundary; disclaimer/citation provenance; internal-key protection` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-AI-01` and exact code/test sources below |

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
| Actor goal | Ask a maternal-care question through the reachable AI Nurse chat, receive a grounded advisory response with citations/disclaimer, and follow deterministic expert/emergency guardrails. | SRS `UC-AI-01` |
| Current state | `High` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | Mobile `/rag/chat` and local chat history/session sheet | Current client/router evidence |
| Authorization boundary | `Mother / Family where allowed` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/controller/RagController.java` |
| Primary operations | Submit a supported question plus minimal allowed context.; Retrieve stage-eligible knowledge and generate through the configured model chain.; Apply deterministic safety flags, return sources/disclaimer, and render degraded mode safely if generation fails. | SRS `UC-AI-01` Normal Flow |
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
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AI-01 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-AI-01-TDS` | 0.1 | Draft design |
| `SRC-ARCH-01` | Approved AI architecture | `docs/AI/01_THIET_KE_KIEN_TRUC_AI_RAG_VA_BAO_VE_DO_AN.md` | Restored HEAD baseline | Approved immutable reference; never generator-owned |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/controller/RagController.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/service/RagPolicyServiceImpl.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/dto/RagAnswerRequest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/dto/RagAnswerResponse.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-05` | Current code | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-06` | Current code | `05_Development/CareBridgeAITriageService/app/models/schemas.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-07` | Current code | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-08` | Current code | `05_Development/CareBridgeAITriageService/app/rag/vector_store.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-09` | Current code | `05_Development/CareBridgeAITriageService/app/rag/prompts.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-10` | Current code | `05_Development/CareBridgeAITriageService/app/core/gemini.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-11` | Current code | `05_Development/CareBridgeAITriageService/app/core/security.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-12` | Current code | `05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagControllerTest.java` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-03` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Worktree `2026-08-23` | Current-state evidence |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | Structured triage session/history/handoff backend infrastructure has no reachable intake UI and remains Partial. | Could create false completed coverage or wrong contract | Keep as current limitation/Open until the cited client/server mismatch is resolved | SRS `UC-AI-01` gap plus current code | Open |
| `LI-02` | The literal `carebridge` key is accepted even when another expected production key is configured; record this as a failing production-security expectation. | Could create false completed coverage or wrong contract | Keep as current limitation/Open until the cited client/server mismatch is resolved | SRS `UC-AI-01` gap plus current code | Open |
| `LI-03` | No focused Mobile test currently proves Python failure to Spring fallback and response-shape downgrade. | Could create false completed coverage or wrong contract | Keep as current limitation/Open until the cited client/server mismatch is resolved | SRS `UC-AI-01` gap plus current code | Open |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Submit a supported question plus minimal allowed context. | Critical | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Retrieve stage-eligible knowledge and generate through the configured model chain. | Critical | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Apply deterministic safety flags, return sources/disclaimer, and render degraded mode safely if generation fails. | Critical | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
| `RISK-AUTH` | Cross-user/role/member/consent data access | Critical | Medium | Medium | Security/Integration/Contract | `COND-AUTH` |
| `RISK-GAP` | Documentation claims an unreachable or broken path as complete | High | Medium | High | Characterization/Contract/UI | `COND-GAP` |

#### Platform and Test-Level Applicability Matrix

| Platform / Layer | Unit | Integration | Contract / Component | Widget / UI | E2E | Security |
| --- | --- | --- | --- | --- | --- | --- |
| Backend | Applicable — current backend/API contracts | Applicable — current backend/API contracts | Applicable — current backend/API contracts | Not applicable — backend has no UI | Applicable — current backend/API contracts | Applicable — current backend/API contracts |
| Web | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC |
| Mobile | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points |
| AI Service | Applicable — current Python AI contracts | Applicable — current Python AI contracts | Applicable — current Python AI contracts | Not applicable — Python service has no actor UI | Applicable — current Python AI contracts | Applicable — current Python AI contracts |

### TDS-02 — Test Basis and Oracle Hierarchy

| Basis | Requirement / behavior | Exact source | Oracle | Conditions |
| --- | --- | --- | --- | --- |
| `BASIS-01` | `UC-AI-01-FR-01` — Submit a supported question plus minimal allowed context. | SRS `UC-AI-01` Normal Flow 1; TDS Section 2 | Submit a supported question plus minimal allowed context. | `COND-01` |
| `BASIS-02` | `UC-AI-01-FR-02` — Retrieve stage-eligible knowledge and generate through the configured model chain. | SRS `UC-AI-01` Normal Flow 2; TDS Section 2 | Retrieve stage-eligible knowledge and generate through the configured model chain. | `COND-02` |
| `BASIS-03` | `UC-AI-01-FR-03` — Apply deterministic safety flags, return sources/disclaimer, and render degraded mode safely if generation fails. | SRS `UC-AI-01` Normal Flow 3; TDS Section 2 | Apply deterministic safety flags, return sources/disclaimer, and render degraded mode safely if generation fails. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Submit a supported question plus minimal allowed context. | Mobile / Spring Gateway / Python AI Service | Positive + applicable boundary/state coverage | `UC-AI-01-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Retrieve stage-eligible knowledge and generate through the configured model chain. | Mobile / Spring Gateway / Python AI Service | Positive + applicable boundary/state coverage | `UC-AI-01-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Apply deterministic safety flags, return sources/disclaimer, and render degraded mode safely if generation fails. | Mobile / Spring Gateway / Python AI Service | Positive + applicable boundary/state coverage | `UC-AI-01-TC-003` |
| `COND-AI-CONTRACT` | Current AI code / safety risk | Python chat returns the documented response contract | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-001` |
| `COND-AI-VALIDATION` | Current AI code / safety risk | Transport-invalid Python request is rejected before generation | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-002` |
| `COND-AI-KEY` | Current AI code / safety risk | Strict internal-key configuration rejects missing or invalid key | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-003` |
| `COND-AI-KEY-GAP` | Current AI code / safety risk | Literal carebridge key exposes the production-security gap | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-004` |
| `COND-AI-RETRIEVAL` | Current AI code / safety risk | Retrieval is stage scoped and bounded to four candidates | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-005` |
| `COND-AI-THRESHOLD` | Current AI code / safety risk | Similarity threshold includes the 0.35 boundary | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-006` |
| `COND-AI-RANKING` | Current AI code / safety risk | Hybrid ranking uses implemented weights and boosts | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-007` |
| `COND-AI-CITATION` | Current AI code / safety risk | Citation de-duplication prefers specific sections | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-008` |
| `COND-AI-NO-CONTEXT` | Current AI code / safety risk | No relevant chunk does not fabricate citations | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-009` |
| `COND-AI-HISTORY` | Current AI code / safety risk | Prompt history is limited to the last six messages | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-010` |
| `COND-AI-QUERY-EXPANSION` | Current AI code / safety risk | Retrieval query expands with the latest recent user turn | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-011` |
| `COND-AI-QUERY-BOUNDARY` | Current AI code / safety risk | Older user turn does not expand retrieval query | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-012` |
| `COND-AI-FAMILY-PRIVACY` | Current AI code / safety risk | Family prompt excludes mother-only clinical context | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-013` |
| `COND-AI-MOTHER-CONTEXT` | Current AI code / safety risk | Mother prompt includes only supported formatted context | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-014` |
| `COND-AI-MOBILE-ISOLATION` | Current AI code / safety risk | Local chat sessions are isolated by authenticated user | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-015` |
| `COND-AI-MODEL-FALLBACK` | Current AI code / safety risk | Generation tries distinct fallback models in order | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-016` |
| `COND-AI-DEGRADED` | Current AI code / safety risk | All model failures return bounded degraded text | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-017` |
| `COND-AI-TAGS` | Current AI code / safety risk | Clinical tags are removed and mapped to response flags | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-018` |
| `COND-AI-SAFETY-FLOOR` | Current AI code / safety risk | Deterministic abnormal metrics raise the expert flag | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-019` |
| `COND-AI-DISCLAIMER` | Current AI code / safety risk | Every Python response contains medical disclaimer | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-020` |
| `COND-AI-MOBILE-FALLBACK` | Current AI code / safety risk | Mobile falls back from Python to authenticated Spring RAG | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-021` |
| `COND-AI-PREFILL` | Current AI code / safety risk | Metric yellow branch prefills chat without auto-send | Python/Spring/Mobile as cited | Boundary/contract/privacy/safety | `UC-AI-01-TC-022` |
| `COND-AI-CONTRACT` | `BR-01` | Python `RagChatRequest` carries message, stage, optional mother-only gestational age/survey/recent metrics, role, and conversation history; `RagChatResponse` returns answer, critical/expert flags, follow-ups, citations, disclaimer, and generated time. | `05_Development/CareBridgeAITriageService/app/models/schemas.py` | Boundary / contract / privacy / safety | `UC-AI-01-TC-001` |
| `COND-AI-MOBILE-FALLBACK` | `BR-02` | Mobile currently calls Python directly with literal internal key `carebridge`, then falls back to authenticated Spring `/api/v1/rag/answer`; this compiled-key boundary is not production-safe. | `05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart` | Boundary / contract / privacy / safety | `UC-AI-01-TC-021` |
| `COND-AI-RETRIEVAL` | `BR-03` | Python retrieval defaults stage to `PREGNANCY`, includes stage plus `ALL`, requests top 4, and excludes returned chunks with similarity below `0.35` before prompting. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` | Boundary / contract / privacy / safety | `UC-AI-01-TC-005` |
| `COND-AI-RANKING` | `BR-04` | Hybrid retrieval ranks by `0.35 * vector similarity + 0.20 * keyword ratio + title/phrase boosts` and de-duplicates title/section candidates. | `05_Development/CareBridgeAITriageService/app/rag/vector_store.py` | Boundary / contract / privacy / safety | `UC-AI-01-TC-007` |
| `COND-AI-QUERY-EXPANSION` | `BR-05` | Retrieval-query expansion uses the latest user/human turn among the last two history messages; the prompt includes at most the last six messages. | `05_Development/CareBridgeAITriageService/app/rag/prompts.py` | Boundary / contract / privacy / safety | `UC-AI-01-TC-011` |
| `COND-AI-FAMILY-PRIVACY` | `BR-06` | Family requests exclude gestational age, survey profile, and recent metrics; Mother requests include only supported formatted fields. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` | Boundary / contract / privacy / safety | `UC-AI-01-TC-013` |
| `COND-AI-CITATION` | `BR-07` | Citations are created only from valid retrieved chunks, de-duplicated by title/section, and omit a generic root citation when a specific section exists. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` | Boundary / contract / privacy / safety | `UC-AI-01-TC-008` |
| `COND-AI-MODEL-FALLBACK` | `BR-08` | Generation tries distinct configured/fallback Gemini models and returns a bounded static response when all provider calls fail. | `05_Development/CareBridgeAITriageService/app/core/gemini.py` | Boundary / contract / privacy / safety | `UC-AI-01-TC-016` |
| `COND-AI-SAFETY-FLOOR` | `BR-09` | Model tags are removed from visible text and mapped to critical/expert flags/follow-ups; deterministic abnormal-metric rules can raise but never lower expert consultation. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` | Boundary / contract / privacy / safety | `UC-AI-01-TC-019` |
| `COND-AI-DISCLAIMER` | `BR-10` | Every Python success/degraded response includes the configured medical disclaimer; Spring returns its own constant disclaimer and explicit fallback flag. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/dto/RagAnswerResponse.java` | Boundary / contract / privacy / safety | `UC-AI-01-TC-020` |
| `COND-AI-SAFETY-FLOOR` | `BR-11` | RAG is advisory/non-diagnostic; deterministic safety rules are the floor and citations must come from retrieved knowledge. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` | Boundary / contract / privacy / safety | `UC-AI-01-TC-019` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-AI-01-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-AI-01-TC-GAP-001` |

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
| Actor | Synthetic `Mother / Family where allowed` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagControllerTest.java`
- `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py`
- `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py`

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

### UC-AI-01-TC-001 — Python chat returns the documented response contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AI-CONTRACT` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/models/schemas.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-CONTRACT`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. POST a valid MOTHER RagChatRequest to `/api/v1/chat/message` with a valid internal key.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP 200 contains answer, critical/expert flags, suggested follow-ups, sources, non-empty disclaimer, and generated timestamp. | `05_Development/CareBridgeAITriageService/app/models/schemas.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP 200 contains answer, critical/expert flags, suggested follow-ups, sources, non-empty disclaimer, and generated timestamp.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-002 — Transport-invalid Python request is rejected before generation

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-002` |
| Severity | `High` |
| Test Condition | `COND-AI-VALIDATION` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/models/schemas.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-VALIDATION`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Omit message or submit malformed stage/history/recent-metric shapes.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | FastAPI validation rejects the request and no retrieval/provider generation is invoked. | `05_Development/CareBridgeAITriageService/app/models/schemas.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (FastAPI validation rejects the request and no retrieval/provider generation is invoked.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-003 — Strict internal-key configuration rejects missing or invalid key

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-003` |
| Severity | `Critical` |
| Test Condition | `COND-AI-KEY` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/core/security.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-KEY`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Run non-debug strict configuration and omit or alter all accepted internal-key headers.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | HTTP 401 is returned before retrieval or generation. | `05_Development/CareBridgeAITriageService/app/core/security.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (HTTP 401 is returned before retrieval or generation.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-004 — Literal carebridge key exposes the production-security gap

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-004` |
| Severity | `Critical` |
| Test Condition | `COND-AI-KEY-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/core/security.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-KEY-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Configure a different non-debug expected key, then submit literal `carebridge`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Current code accepts the literal key; the characterization must remain a failing production-security expectation until the bypass is removed. | `05_Development/CareBridgeAITriageService/app/core/security.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Current code accepts the literal key; the characterization must remain a failing production-security expectation until the bypass is removed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-005 — Retrieval is stage scoped and bounded to four candidates

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-005` |
| Severity | `High` |
| Test Condition | `COND-AI-RETRIEVAL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-RETRIEVAL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Provide chunks for the requested stage, `ALL`, and an unrelated stage, then ask a matching question.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Only requested-stage/ALL candidates are eligible and at most four ranked chunks are returned to the chat service. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Only requested-stage/ALL candidates are eligible and at most four ranked chunks are returned to the chat service.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-006 — Similarity threshold includes the 0.35 boundary

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-006` |
| Severity | `Critical` |
| Test Condition | `COND-AI-THRESHOLD` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-THRESHOLD`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Return chunks immediately below, exactly at, and immediately above similarity `0.35`.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Below-threshold chunk is excluded; boundary and above-threshold chunks remain valid. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Below-threshold chunk is excluded; boundary and above-threshold chunks remain valid.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-007 — Hybrid ranking uses implemented weights and boosts

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-007` |
| Severity | `High` |
| Test Condition | `COND-AI-RANKING` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/rag/vector_store.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-RANKING`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Vary vector similarity, keyword hits, title phrase, and content phrase matches across candidates.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Ordering follows `0.35 * vector + 0.20 * keyword + 0.50 title boost + 0.25 content phrase boost` per matching phrase. | `05_Development/CareBridgeAITriageService/app/rag/vector_store.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Ordering follows `0.35 * vector + 0.20 * keyword + 0.50 title boost + 0.25 content phrase boost` per matching phrase.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-008 — Citation de-duplication prefers specific sections

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-008` |
| Severity | `High` |
| Test Condition | `COND-AI-CITATION` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-CITATION`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Return duplicate title/section chunks plus a root section equal to title and a specific section.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Citations are unique by normalized title/section and the generic root citation is omitted when a specific section exists. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Citations are unique by normalized title/section and the generic root citation is omitted when a specific section exists.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-009 — No relevant chunk does not fabricate citations

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-009` |
| Severity | `Critical` |
| Test Condition | `COND-AI-NO-CONTEXT` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-NO-CONTEXT`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Return no chunk at or above the similarity threshold.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Sources may be empty, no fabricated citation object is returned, and the disclaimer remains present. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Sources may be empty, no fabricated citation object is returned, and the disclaimer remains present.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-010 — Prompt history is limited to the last six messages

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-010` |
| Severity | `High` |
| Test Condition | `COND-AI-HISTORY` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/rag/prompts.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-HISTORY`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Provide more than six prior messages with distinct sentinels.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Only the last six messages appear in the generated prompt history section. | `05_Development/CareBridgeAITriageService/app/rag/prompts.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Only the last six messages appear in the generated prompt history section.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-011 — Retrieval query expands with the latest recent user turn

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-011` |
| Severity | `High` |
| Test Condition | `COND-AI-QUERY-EXPANSION` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-QUERY-EXPANSION`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Place a user/human turn within the last two history messages, followed by the current question.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The latest such user turn and current message form the retrieval query. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The latest such user turn and current message form the retrieval query.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-012 — Older user turn does not expand retrieval query

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-012` |
| Severity | `Medium` |
| Test Condition | `COND-AI-QUERY-BOUNDARY` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-QUERY-BOUNDARY`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Place a user turn outside the last two history messages.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | That older turn is absent from the retrieval query, while prompt history still follows its six-message rule. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (That older turn is absent from the retrieval query, while prompt history still follows its six-message rule.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-013 — Family prompt excludes mother-only clinical context

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-013` |
| Severity | `Critical` |
| Test Condition | `COND-AI-FAMILY-PRIVACY` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-FAMILY-PRIVACY`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit a FAMILY request containing gestational age, survey profile, and recent metrics.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | All three mother-only context groups are absent from the generated prompt. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (All three mother-only context groups are absent from the generated prompt.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-014 — Mother prompt includes only supported formatted context

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-014` |
| Severity | `High` |
| Test Condition | `COND-AI-MOTHER-CONTEXT` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-MOTHER-CONTEXT`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Submit a MOTHER request with supported and unrelated survey/metric fields.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Only supported formatted gestational, survey, BP, temperature, glucose, fetal-movement, symptom, and EPDS context enters the prompt; unrelated raw data is excluded. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Only supported formatted gestational, survey, BP, temperature, glucose, fetal-movement, symptom, and EPDS context enters the prompt; unrelated raw data is excluded.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-015 — Local chat sessions are isolated by authenticated user

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-015` |
| Severity | `Critical` |
| Test Condition | `COND-AI-MOBILE-ISOLATION` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeMobileApp/test/features/aiTriage/rag_chat_screen_test.dart` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-MOBILE-ISOLATION`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Create local AI chat sessions for two user IDs and switch or sign out accounts.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | History from the first user is not exposed to the second user. | `05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (History from the first user is not exposed to the second user.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-016 — Generation tries distinct fallback models in order

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-016` |
| Severity | `High` |
| Test Condition | `COND-AI-MODEL-FALLBACK` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/core/gemini.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-MODEL-FALLBACK`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Fail the configured primary model and allow the next distinct fallback model to succeed.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Models are attempted in configured distinct order and the first successful text is returned. | `05_Development/CareBridgeAITriageService/app/core/gemini.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Models are attempted in configured distinct order and the first successful text is returned.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-017 — All model failures return bounded degraded text

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-017` |
| Severity | `Critical` |
| Test Condition | `COND-AI-DEGRADED` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/core/gemini.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-DEGRADED`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Make every configured generation model unavailable or return no text.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The bounded static fallback is returned without provider internals, fabricated uptime, or fabricated citations. | `05_Development/CareBridgeAITriageService/app/core/gemini.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The bounded static fallback is returned without provider internals, fabricated uptime, or fabricated citations.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-018 — Clinical tags are removed and mapped to response flags

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-018` |
| Severity | `Critical` |
| Test Condition | `COND-AI-TAGS` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-TAGS`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Return Gemini text containing critical/expert and suggested-question tags.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Tags are absent from visible answer text; booleans/follow-ups reflect parsed values and follow-ups are capped at three. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Tags are absent from visible answer text; booleans/follow-ups reflect parsed values and follow-ups are capped at three.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-019 — Deterministic abnormal metrics raise the expert flag

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-019` |
| Severity | `Critical` |
| Test Condition | `COND-AI-SAFETY-FLOOR` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-SAFETY-FLOOR`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Set each boundary abnormal metric while the model expert flag is false.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Expert consultation is true at systolic >=140, diastolic >=90, temperature >=38.5, glucose >=7.8, EPDS >=10, or fetal movements <4; model output cannot lower it. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Expert consultation is true at systolic >=140, diastolic >=90, temperature >=38.5, glucose >=7.8, EPDS >=10, or fetal movements <4; model output cannot lower it.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-020 — Every Python response contains medical disclaimer

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-020` |
| Severity | `Critical` |
| Test Condition | `COND-AI-DISCLAIMER` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-DISCLAIMER`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise normal, no-context, warning, and provider-degraded responses.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The disclaimer is always non-empty and comes from configured server text rather than model output. | `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The disclaimer is always non-empty and comes from configured server text rather than model output.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-021 — Mobile falls back from Python to authenticated Spring RAG

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-021` |
| Severity | `Critical` |
| Test Condition | `COND-AI-MOBILE-FALLBACK` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeMobileApp/test/features/aiTriage/rag_chat_screen_test.dart` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-MOBILE-FALLBACK`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Make all Python candidates fail or return empty, then return a valid Spring `/api/v1/rag/answer` envelope.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Mobile calls Spring, renders its answer/sources, and does not claim Python-only flags, follow-ups, or citation sections. | `05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Mobile calls Spring, renders its answer/sources, and does not claim Python-only flags, follow-ups, or citation sections.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-022 — Metric yellow branch prefills chat without auto-send

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-022` |
| Severity | `High` |
| Test Condition | `COND-AI-PREFILL` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | Planned — `05_Development/CareBridgeMobileApp/test/features/aiTriage/rag_chat_screen_test.dart` (not present at Draft baseline) |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AI-PREFILL`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Open `/rag/chat` from the anomaly-monitor metric action with prepared question/context.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The input is prefilled and no request is sent until the user explicitly submits. | `05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The input is prefilled and no request is sent until the user explicitly submits.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/controller/RagController.java` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagControllerTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/controller/RagController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-AI-01-TC-GAP-001 — Characterize the current documented limitation

| Field | Specification |
| --- | --- |
| Stable ID | `UC-AI-01-TC-GAP-001` |
| Severity | `High` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Spring Gateway / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-AI-01 Known Gaps / Exclusions; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/controller/RagController.java` |
| Preconditions | Synthetic `Mother / Family where allowed` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagControllerTest.java` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Structured triage session/history/handoff backend infrastructure has no reachable intake UI and remains Partial.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The test records the current limitation without inventing a completed path: Structured triage session/history/handoff backend infrastructure has no reachable intake UI and remains Partial. | `SRS UC-AI-01 Known Gaps / Exclusions; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/controller/RagController.java` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-AI-01` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The test records the current limitation without inventing a completed path: Structured triage session/history/handoff backend infrastructure has no reachable intake UI and remains Partial.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Python chat returns the documented response contract | `COND-AI-CONTRACT` | `UC-AI-01-TC-001` |
| Transport-invalid Python request is rejected before generation | `COND-AI-VALIDATION` | `UC-AI-01-TC-002` |
| Strict internal-key configuration rejects missing or invalid key | `COND-AI-KEY` | `UC-AI-01-TC-003` |
| Literal carebridge key exposes the production-security gap | `COND-AI-KEY-GAP` | `UC-AI-01-TC-004` |
| Retrieval is stage scoped and bounded to four candidates | `COND-AI-RETRIEVAL` | `UC-AI-01-TC-005` |
| Similarity threshold includes the 0.35 boundary | `COND-AI-THRESHOLD` | `UC-AI-01-TC-006` |
| Hybrid ranking uses implemented weights and boosts | `COND-AI-RANKING` | `UC-AI-01-TC-007` |
| Citation de-duplication prefers specific sections | `COND-AI-CITATION` | `UC-AI-01-TC-008` |
| No relevant chunk does not fabricate citations | `COND-AI-NO-CONTEXT` | `UC-AI-01-TC-009` |
| Prompt history is limited to the last six messages | `COND-AI-HISTORY` | `UC-AI-01-TC-010` |
| Retrieval query expands with the latest recent user turn | `COND-AI-QUERY-EXPANSION` | `UC-AI-01-TC-011` |
| Older user turn does not expand retrieval query | `COND-AI-QUERY-BOUNDARY` | `UC-AI-01-TC-012` |
| Family prompt excludes mother-only clinical context | `COND-AI-FAMILY-PRIVACY` | `UC-AI-01-TC-013` |
| Mother prompt includes only supported formatted context | `COND-AI-MOTHER-CONTEXT` | `UC-AI-01-TC-014` |
| Local chat sessions are isolated by authenticated user | `COND-AI-MOBILE-ISOLATION` | `UC-AI-01-TC-015` |
| Generation tries distinct fallback models in order | `COND-AI-MODEL-FALLBACK` | `UC-AI-01-TC-016` |
| All model failures return bounded degraded text | `COND-AI-DEGRADED` | `UC-AI-01-TC-017` |
| Clinical tags are removed and mapped to response flags | `COND-AI-TAGS` | `UC-AI-01-TC-018` |
| Deterministic abnormal metrics raise the expert flag | `COND-AI-SAFETY-FLOOR` | `UC-AI-01-TC-019` |
| Every Python response contains medical disclaimer | `COND-AI-DISCLAIMER` | `UC-AI-01-TC-020` |
| Mobile falls back from Python to authenticated Spring RAG | `COND-AI-MOBILE-FALLBACK` | `UC-AI-01-TC-021` |
| Metric yellow branch prefills chat without auto-send | `COND-AI-PREFILL` | `UC-AI-01-TC-022` |
| Business-rule partitions | Exact `COND-AI-*` rows above | Existing `UC-AI-01-TC-001` through `UC-AI-01-TC-022` mappings in TDS Section 2 |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-AI-01-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-AI-01-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-AI-01-TC-001` | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-002` | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-003` | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-004` | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-005` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-006` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-007` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-008` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-009` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-010` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-011` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-012` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-013` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-014` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-015` | Planned — `05_Development/CareBridgeMobileApp/test/features/aiTriage/rag_chat_screen_test.dart` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-016` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-017` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-018` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-019` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-020` | `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-021` | Planned — `05_Development/CareBridgeMobileApp/test/features/aiTriage/rag_chat_screen_test.dart` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-022` | Planned — `05_Development/CareBridgeMobileApp/test/features/aiTriage/rag_chat_screen_test.dart` (not present at Draft baseline) | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-SEC-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagControllerTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-AI-01-TC-GAP-001` | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagControllerTest.java` | Not run | Not run | Not run | 🔴 Not written / rerun required |

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
| Generic test matrix | Case titles/actions reference `Use AI Nurse RAG Chat` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Open gaps recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Required |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
