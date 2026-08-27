# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Run AI Diagnostic and Clinical Simulators

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-21-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AD-21` |
| Canonical Use Case | `UC-AD-21 — Run AI Diagnostic and Clinical Simulators` |
| Module / Bounded Context | `Administration and Operations` |
| Primary Actor | `Authorized Technical Operator` |
| Platforms | `Python AI Service / FastAPI Swagger` |
| Priority | `Medium` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AD-21`; exact evidence in Section 1.4 |

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

- **Goal:** Test prompt/model configuration and run deterministic metric simulation to inspect actual versus expected screening behavior.
- **Trigger:** The actor enters FastAPI Swagger `/docs`; no dedicated Web Admin route.
- **Outcome:** Review actual versus expected results without promoting them to production guarantees.
- **Current state:** `High` confidence from reachable code/test audit; no manifest-level exclusion is recorded.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- FastAPI Swagger `/docs`; no dedicated Web Admin route

- POST `/api/v1/chat/test-prompt`
- GET `/api/v1/chat/models`
- POST `/api/v1/metrics/simulate-batch`

**Out of scope / limitations**

- Not applicable — no manifest-level exclusion is recorded. Scope is limited to the entry points and exact contracts listed above; unrelated handlers in the same module are excluded.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Authorized Technical Operator is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Review actual versus expected results without promoting them to production guarantees. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-21 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- No material contradiction was found among the exact sources cited in Section 1.4. Behavior not evidenced by those sources is not approved or implied by this Draft.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AD-21-FR-01` | Authenticate with the configured internal operator key. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-21 Normal Flow 1 | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` | `COND-01` / `UC-AD-21-TC-001` |
| `UC-AD-21-FR-02` | Inspect model configuration or submit a bounded diagnostic prompt/simulation batch. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-21 Normal Flow 2 | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` | `COND-02` / `UC-AD-21-TC-002` |
| `UC-AD-21-FR-03` | Review actual versus expected results without promoting them to production guarantees. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-21 Normal Flow 3 | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | `COND-03` / `UC-AD-21-TC-003` |
| `BR-01` | Diagnostic endpoints are operational tools, not consumer clinical flows. | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | `COND-BR-01` / `UC-AD-21-TC-BR-001` |
| `BR-02` | Historical counts, latency, uptime, or accuracy are not current requirements unless rerun and dated. | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | `COND-BR-02` / `UC-AD-21-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AD-21-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AD-21 — Run AI Diagnostic and Clinical Simulators` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AD-21-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` | Reuse | Current implementation evidence for Run AI Diagnostic and Clinical Simulators; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | Reuse | Current implementation evidence for Run AI Diagnostic and Clinical Simulators; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | Reuse | Current implementation evidence for Run AI Diagnostic and Clinical Simulators; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class chat as "chat.py"
class metrics as "metrics.py"
chat --> metrics
class metrics_screening_service as "metrics_screening_service.py"
metrics --> metrics_screening_service
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAITriageService/app/core/database.py`; `05_Development/CareBridgeAITriageService/app/models/schemas.py`; `05_Development/CareBridgeAITriageService/app/rag/vector_store.py` |
| Sensitive fields | Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Run AI Diagnostic and Clinical Simulators
Client -> Domain: Authenticate with the configured internal operator key.
Domain --> Client: Result for step 1
Client -> Domain: Inspect model configuration or submit a bounded diagnostic prompt/simulation batch.
Domain --> Client: Result for step 2
Client -> Domain: Review actual versus expected results without promoting them to production guarantees.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AD-21 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAITriageService/app/core/gemini.py`; `05_Development/CareBridgeAITriageService/app/core/security.py`; `05_Development/CareBridgeAITriageService/app/rag/vector_store.py`; `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Authenticate with the configured internal operator key.
InProgress --> Outcome : Review actual versus expected results without promoting them to production guarantees.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Diagnostic endpoints are operational tools, not consumer clinical flows.
- Historical counts, latency, uptime, or accuracy are not current requirements unless rerun and dated.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | FastAPI Swagger `/docs`; no dedicated Web Admin route | Authorized Technical Operator | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `GET /api/v1/chat/models` | Internal API key via `verify_internal_api_key` dependency | Handler `list_available_models`; parameters: dependency `_auth`: `str`; request body: `None`; request fields/validation: Not applicable — no request body; response: `dict`; response payload fields: Not applicable or primitive/framework-managed payload; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` |
| `API-02` | `POST /api/v1/chat/test-prompt` | Internal API key via `verify_internal_api_key` dependency | Handler `test_custom_prompt`; parameters: body `request`: `CustomPromptTestRequest`; dependency `_auth`: `str`; request body: `CustomPromptTestRequest`; request fields/validation: `user_message`: `str` (`required`); `system_instruction`: `Optional[str]` (`Field(default=None, description="System prompt tùy chỉnh để thử nghiệm")`); `temperature`: `Optional[float]` (`Field(default=0.3, ge=0.0, le=1.0)`); `model`: `Optional[str]` (`Field(default=None, description="Model muốn thử (gemini-flash-lite-latest, gemini-2.5-flash, gemini-3.7-flash)")`); response: `CustomPromptTestResponse`; response payload fields: `model_used`: `str` (`required`); `temperature`: `float` (`required`); `answer`: `str` (`required`); `generated_at`: `datetime` (`Field(default_factory=datetime.utcnow)`); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` |
| `API-03` | `POST /api/v1/metrics/simulate-batch` | Internal API key via `verify_internal_api_key` dependency | Handler `simulate_clinical_cases`; parameters: dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `None`; request fields/validation: Not applicable — no request body; response: `BatchSimulationResponse`; response payload fields: `total_cases`: `int` (`required`); `passed_cases`: `int` (`required`); `all_passed`: `bool` (`required`); `results`: `List[BatchSimulationCase]` (`required`); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `GET /api/v1/chat/models`

| Item | Exact current contract |
| --- | --- |
| Handler | `list_available_models` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | dependency `_auth`: `str` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `dict` |
| Response payload fields | Not applicable or primitive/framework-managed payload |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-AD-21-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `POST /api/v1/chat/test-prompt`

| Item | Exact current contract |
| --- | --- |
| Handler | `test_custom_prompt` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | body `request`: `CustomPromptTestRequest`; dependency `_auth`: `str` |
| Request body type | `CustomPromptTestRequest` |
| Request fields and validators | `user_message`: `str` (`required`); `system_instruction`: `Optional[str]` (`Field(default=None, description="System prompt tùy chỉnh để thử nghiệm")`); `temperature`: `Optional[float]` (`Field(default=0.3, ge=0.0, le=1.0)`); `model`: `Optional[str]` (`Field(default=None, description="Model muốn thử (gemini-flash-lite-latest, gemini-2.5-flash, gemini-3.7-flash)")`) |
| Response type | `CustomPromptTestResponse` |
| Response payload fields | `model_used`: `str` (`required`); `temperature`: `float` (`required`); `answer`: `str` (`required`); `generated_at`: `datetime` (`Field(default_factory=datetime.utcnow)`) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-AD-21-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `POST /api/v1/metrics/simulate-batch`

| Item | Exact current contract |
| --- | --- |
| Handler | `simulate_clinical_cases` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `BatchSimulationResponse` |
| Response payload fields | `total_cases`: `int` (`required`); `passed_cases`: `int` (`required`); `all_passed`: `bool` (`required`); `results`: `List[BatchSimulationCase]` (`required`) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-AD-21-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rejection | No 4xx declared by selected handler syntax; framework/service/advice mapping applies | Invalid field/range/state/ownership input | No write or false success; show only current mapped error | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` plus exact exception advice/service characterization |
| Authentication/authorization | `401/403` only where the security chain or handler policy maps them | Missing credential or disallowed role/scope | Fail closed with no protected response fields | Security configuration and `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` |
| Dependency/internal failure | No feature-specific status declared by selected handler syntax | Provider/storage/network/internal failure | Only implemented retry/degraded/terminal behavior | Owning adapter/advice source characterization required |

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
| `VG-01` | Authenticate with the configured internal operator key. | `COND-01` | `UC-AD-21-TC-001` |
| `VG-02` | Inspect model configuration or submit a bounded diagnostic prompt/simulation batch. | `COND-02` | `UC-AD-21-TC-002` |
| `VG-03` | Review actual versus expected results without promoting them to production guarantees. | `COND-03` | `UC-AD-21-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AD-21-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AD-21-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py`
- `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAITriageService && pytest tests/test_api_endpoints.py`
- `cd 05_Development/CareBridgeAITriageService && pytest tests/test_metrics_screening.py`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | POST `/api/v1/chat/test-prompt` |
| Request | `GET /api/v1/chat/models` → `list_available_models`; `None` with Not applicable — no request body; authorization: Internal API key via `verify_internal_api_key` dependency. |
| Success response | `dict` with Not applicable or primitive/framework-managed payload; explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Authorized technical operator using the internal API key | `GET /api/v1/chat/models` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` |
| Authorized technical operator using the internal API key | `POST /api/v1/chat/test-prompt` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/chat.py` |
| Authorized technical operator using the internal API key | `POST /api/v1/metrics/simulate-batch` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
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
