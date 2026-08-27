# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Manage AI Knowledge Base

| Field | Value |
| --- | --- |
| Document ID | `UC-AD-20-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-AD-20` |
| Canonical Use Case | `UC-AD-20 — Manage AI Knowledge Base` |
| Module / Bounded Context | `Administration and Operations` |
| Primary Actor | `Authorized Technical Operator` |
| Platforms | `Python AI Service / FastAPI Swagger / Database` |
| Priority | `Medium` |
| Data Classification | `Confidential administrative configuration/audit/moderation data; Restricted account, identity, credential, and report evidence where applicable` |
| Compliance Scope | `Least-privilege administration, immutable/auditable decisions, reason capture, protected-evidence minimization, and secret redaction` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-AD-20`; exact evidence in Section 1.4 |

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

- **Goal:** Inspect knowledge/chunks, upload and ingest supported documents, synchronize/rebuild eligible sources, and delete obsolete knowledge.
- **Trigger:** The actor enters FastAPI Swagger `/docs`; no dedicated Web Admin route.
- **Outcome:** Delete an eligible source and verify future retrieval state.
- **Current state:** `High` confidence from reachable code/test audit; documented limitations remain visible below.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- FastAPI Swagger `/docs`; no dedicated Web Admin route

- Python document/knowledge endpoints under `/api/v1/documents/**`

**Out of scope / limitations**

- Open / current limitation: Current operation is API/Swagger-based rather than a role-authenticated Web administration page.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Authorized Technical Operator is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Delete an eligible source and verify future retrieval state. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-20 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeAITriageService/scripts/ingest_documents.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- Open / current limitation: Current operation is API/Swagger-based rather than a role-authenticated Web administration page.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-AD-20-FR-01` | Authenticate with the configured internal operator key. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-20 Normal Flow 1 | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` | `COND-01` / `UC-AD-20-TC-001` |
| `UC-AD-20-FR-02` | Inspect/upload/ingest/synchronize supported knowledge sources. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-20 Normal Flow 2 | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` | `COND-02` / `UC-AD-20-TC-002` |
| `UC-AD-20-FR-03` | Delete an eligible source and verify future retrieval state. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-AD-20 Normal Flow 3 | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` | `COND-03` / `UC-AD-20-TC-003` |
| `BR-01` | All operations require the configured internal API key. | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | `COND-BR-01` / `UC-AD-20-TC-BR-001` |
| `BR-02` | File type/size/name validation and curated source metadata are required. | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | `COND-BR-02` / `UC-AD-20-TC-BR-002` |
| `BR-03` | Deleting knowledge changes future retrieval but does not prove generated answers are error-free. | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | `COND-BR-03` / `UC-AD-20-TC-BR-003` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-AD-20-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-AD-20 — Manage AI Knowledge Base` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-AD-20-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` | Reuse | Current implementation evidence for Manage AI Knowledge Base; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | Reuse | Current implementation evidence for Manage AI Knowledge Base; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAITriageService/scripts/ingest_documents.py` | Reuse | Current implementation evidence for Manage AI Knowledge Base; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class documents as "documents.py"
class ingestion_service as "ingestion_service.py"
documents --> ingestion_service
class ingest_documents as "ingest_documents.py"
ingestion_service --> ingest_documents
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
Actor -> Client: Enter Manage AI Knowledge Base
Client -> Domain: Authenticate with the configured internal operator key.
Domain --> Client: Result for step 1
Client -> Domain: Inspect/upload/ingest/synchronize supported knowledge sources.
Domain --> Client: Result for step 2
Client -> Domain: Delete an eligible source and verify future retrieval state.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-AD-20 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAITriageService/app/core/security.py`; `05_Development/CareBridgeAITriageService/app/rag/chunker.py`; `05_Development/CareBridgeAITriageService/app/rag/vector_store.py`; `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Authenticate with the configured internal operator key.
InProgress --> Outcome : Delete an eligible source and verify future retrieval state.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- All operations require the configured internal API key.
- File type/size/name validation and curated source metadata are required.
- Deleting knowledge changes future retrieval but does not prove generated answers are error-free.

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
| `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAITriageService/scripts/ingest_documents.py` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `DELETE /api/v1/documents/by-title` | Internal API key via `verify_internal_api_key` dependency | Handler `delete_document_by_title`; parameters: query/path/context `title`: `str`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `None`; request fields/validation: Not applicable — no request body; response: `dict`; response payload fields: Not applicable or primitive/framework-managed payload; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| `API-02` | `DELETE /api/v1/documents/clear-all` | Internal API key via `verify_internal_api_key` dependency | Handler `clear_all_knowledge`; parameters: dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `None`; request fields/validation: Not applicable — no request body; response: `dict`; response payload fields: Not applicable or primitive/framework-managed payload; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| `API-03` | `GET /api/v1/documents/files` | Internal API key via `verify_internal_api_key` dependency | Handler `list_raw_files`; parameters: dependency `_auth`: `str`; request body: `None`; request fields/validation: Not applicable — no request body; response: `dict`; response payload fields: Not applicable or primitive/framework-managed payload; explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| `API-04` | `POST /api/v1/documents/ingest-text` | Internal API key via `verify_internal_api_key` dependency | Handler `ingest_raw_text`; parameters: body `request`: `IngestDocumentRequest`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `IngestDocumentRequest`; request fields/validation: `title`: `str` (`required`); `stage`: `MaternalStage` (`MaternalStage.ALL`); `topic`: `str` (`"GENERAL"`); `source`: `str` (`"Bộ Y Tế / Cẩm nang Y khoa"`); `section`: `Optional[str]` (`None`); `text_content`: `str` (`required`); response: `IngestDocumentResponse`; response payload fields: `success`: `bool` (`required`); `message`: `str` (`required`); `total_chunks`: `int` (`required`); `document_title`: `str` (`required`); `stage`: `str` (`required`); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| `API-05` | `GET /api/v1/documents/list` | Internal API key via `verify_internal_api_key` dependency | Handler `list_knowledge_chunks`; parameters: query/path/context `stage`: `Optional[MaternalStage]`; query/path/context `topic`: `Optional[str]`; query/path/context `keyword`: `Optional[str]`; query/path/context `page`: `int`; query/path/context `page_size`: `int`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `None`; request fields/validation: Not applicable — no request body; response: `KnowledgeListResponse`; response payload fields: `total`: `int` (`required`); `page`: `int` (`required`); `page_size`: `int` (`required`); `items`: `List[ChunkDetailItem]` (`required`); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| `API-06` | `POST /api/v1/documents/search-vector` | Internal API key via `verify_internal_api_key` dependency | Handler `simulate_vector_search`; parameters: body `request`: `VectorSearchTestRequest`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `VectorSearchTestRequest`; request fields/validation: `query`: `str` (`Field(description="Câu hỏi hoặc triệu chứng cần tìm kiếm trong CSDL vector")`); `stage`: `Optional[MaternalStage]` (`Field(default=None, description="Lọc theo giai đoạn (Tùy chọn)")`); `top_k`: `int` (`Field(default=4, ge=1, le=20, description="Số lượng đoạn tài liệu cần lấy")`); response: `VectorSearchTestResponse`; response payload fields: `query`: `str` (`required`); `total_retrieved`: `int` (`required`); `results`: `List[SourceCitation]` (`required`); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| `API-07` | `GET /api/v1/documents/stats` | Internal API key via `verify_internal_api_key` dependency | Handler `get_knowledge_statistics`; parameters: dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `None`; request fields/validation: Not applicable — no request body; response: `KnowledgeStatsResponse`; response payload fields: `total_chunks`: `int` (`required`); `total_documents`: `int` (`required`); `stage_distribution`: `Dict[str, int]` (`required`); `topic_distribution`: `Dict[str, int]` (`required`); `files_in_disk`: `List[str]` (`required`); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| `API-08` | `POST /api/v1/documents/sync-directory` | Internal API key via `verify_internal_api_key` dependency | Handler `sync_raw_documents_directory`; parameters: dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `None`; request fields/validation: Not applicable — no request body; response: `BatchIngestResponse`; response payload fields: `success`: `bool` (`required`); `total_files_processed`: `int` (`required`); `total_chunks_created`: `int` (`required`); `processed_files`: `List[str]` (`required`); `errors`: `List[str]` (`Field(default_factory=list)`); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| `API-09` | `POST /api/v1/documents/upload` | Internal API key via `verify_internal_api_key` dependency | Handler `upload_document_file`; parameters: file `file`: `UploadFile`; query/path/context `stage`: `MaternalStage`; query/path/context `topic`: `str`; query/path/context `source`: `str`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `UploadFile`; request fields/validation: Not applicable or primitive/framework-managed payload; response: `IngestDocumentResponse`; response payload fields: `success`: `bool` (`required`); `message`: `str` (`required`); `total_chunks`: `int` (`required`); `document_title`: `str` (`required`); `stage`: `str` (`required`); explicit/documented statuses: `200, 500`; source: `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `DELETE /api/v1/documents/by-title`

| Item | Exact current contract |
| --- | --- |
| Handler | `delete_document_by_title` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | query/path/context `title`: `str`; dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `dict` |
| Response payload fields | Not applicable or primitive/framework-managed payload |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-AD-20-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.2 Handler Contract — `DELETE /api/v1/documents/clear-all`

| Item | Exact current contract |
| --- | --- |
| Handler | `clear_all_knowledge` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `dict` |
| Response payload fields | Not applicable or primitive/framework-managed payload |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-002` / `UC-AD-20-TC-API-002` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.3 Handler Contract — `GET /api/v1/documents/files`

| Item | Exact current contract |
| --- | --- |
| Handler | `list_raw_files` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | dependency `_auth`: `str` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `dict` |
| Response payload fields | Not applicable or primitive/framework-managed payload |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-003` / `UC-AD-20-TC-API-003` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.4 Handler Contract — `POST /api/v1/documents/ingest-text`

| Item | Exact current contract |
| --- | --- |
| Handler | `ingest_raw_text` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | body `request`: `IngestDocumentRequest`; dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `IngestDocumentRequest` |
| Request fields and validators | `title`: `str` (`required`); `stage`: `MaternalStage` (`MaternalStage.ALL`); `topic`: `str` (`"GENERAL"`); `source`: `str` (`"Bộ Y Tế / Cẩm nang Y khoa"`); `section`: `Optional[str]` (`None`); `text_content`: `str` (`required`) |
| Response type | `IngestDocumentResponse` |
| Response payload fields | `success`: `bool` (`required`); `message`: `str` (`required`); `total_chunks`: `int` (`required`); `document_title`: `str` (`required`); `stage`: `str` (`required`) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-004` / `UC-AD-20-TC-API-004` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.5 Handler Contract — `GET /api/v1/documents/list`

| Item | Exact current contract |
| --- | --- |
| Handler | `list_knowledge_chunks` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | query/path/context `stage`: `Optional[MaternalStage]`; query/path/context `topic`: `Optional[str]`; query/path/context `keyword`: `Optional[str]`; query/path/context `page`: `int`; query/path/context `page_size`: `int`; dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `KnowledgeListResponse` |
| Response payload fields | `total`: `int` (`required`); `page`: `int` (`required`); `page_size`: `int` (`required`); `items`: `List[ChunkDetailItem]` (`required`) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-005` / `UC-AD-20-TC-API-005` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.6 Handler Contract — `POST /api/v1/documents/search-vector`

| Item | Exact current contract |
| --- | --- |
| Handler | `simulate_vector_search` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | body `request`: `VectorSearchTestRequest`; dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `VectorSearchTestRequest` |
| Request fields and validators | `query`: `str` (`Field(description="Câu hỏi hoặc triệu chứng cần tìm kiếm trong CSDL vector")`); `stage`: `Optional[MaternalStage]` (`Field(default=None, description="Lọc theo giai đoạn (Tùy chọn)")`); `top_k`: `int` (`Field(default=4, ge=1, le=20, description="Số lượng đoạn tài liệu cần lấy")`) |
| Response type | `VectorSearchTestResponse` |
| Response payload fields | `query`: `str` (`required`); `total_retrieved`: `int` (`required`); `results`: `List[SourceCitation]` (`required`) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-006` / `UC-AD-20-TC-API-006` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.7 Handler Contract — `GET /api/v1/documents/stats`

| Item | Exact current contract |
| --- | --- |
| Handler | `get_knowledge_statistics` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `KnowledgeStatsResponse` |
| Response payload fields | `total_chunks`: `int` (`required`); `total_documents`: `int` (`required`); `stage_distribution`: `Dict[str, int]` (`required`); `topic_distribution`: `Dict[str, int]` (`required`); `files_in_disk`: `List[str]` (`required`) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-007` / `UC-AD-20-TC-API-007` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.8 Handler Contract — `POST /api/v1/documents/sync-directory`

| Item | Exact current contract |
| --- | --- |
| Handler | `sync_raw_documents_directory` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `None` |
| Request fields and validators | Not applicable — no request body |
| Response type | `BatchIngestResponse` |
| Response payload fields | `success`: `bool` (`required`); `total_files_processed`: `int` (`required`); `total_chunks_created`: `int` (`required`); `processed_files`: `List[str]` (`required`); `errors`: `List[str]` (`Field(default_factory=list)`) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-008` / `UC-AD-20-TC-API-008` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

### 9.9 Handler Contract — `POST /api/v1/documents/upload`

| Item | Exact current contract |
| --- | --- |
| Handler | `upload_document_file` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | file `file`: `UploadFile`; query/path/context `stage`: `MaternalStage`; query/path/context `topic`: `str`; query/path/context `source`: `str`; dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `UploadFile` |
| Request fields and validators | Not applicable or primitive/framework-managed payload |
| Response type | `IngestDocumentResponse` |
| Response payload fields | `success`: `bool` (`required`); `message`: `str` (`required`); `total_chunks`: `int` (`required`); `document_title`: `str` (`required`); `stage`: `str` (`required`) |
| Explicit/documented statuses | `200, 500` |
| Positive test mapping | `COND-API-009` / `UC-AD-20-TC-API-009` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Internal failure | `500` | Unhandled internal processing failure | Return the current stable error envelope without protected data or unintended mutation | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py`; application error code is limited to what those sources/advice explicitly declare |

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
| `VG-01` | Authenticate with the configured internal operator key. | `COND-01` | `UC-AD-20-TC-001` |
| `VG-02` | Inspect/upload/ingest/synchronize supported knowledge sources. | `COND-02` | `UC-AD-20-TC-002` |
| `VG-03` | Delete an eligible source and verify future retrieval state. | `COND-03` | `UC-AD-20-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-AD-20-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-AD-20-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py`
- `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAITriageService && pytest tests/test_ingestion_and_chunker.py`
- `cd 05_Development/CareBridgeAITriageService && pytest tests/test_api_endpoints.py`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | Python document/knowledge endpoints under `/api/v1/documents/**` |
| Request | `DELETE /api/v1/documents/by-title` → `delete_document_by_title`; `None` with Not applicable — no request body; authorization: Internal API key via `verify_internal_api_key` dependency. |
| Success response | `dict` with Not applicable or primitive/framework-managed payload; explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Authorized technical operator using the internal API key | `DELETE /api/v1/documents/by-title` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorized technical operator using the internal API key | `DELETE /api/v1/documents/clear-all` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorized technical operator using the internal API key | `GET /api/v1/documents/files` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorized technical operator using the internal API key | `POST /api/v1/documents/ingest-text` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorized technical operator using the internal API key | `GET /api/v1/documents/list` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorized technical operator using the internal API key | `POST /api/v1/documents/search-vector` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorized technical operator using the internal API key | `GET /api/v1/documents/stats` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorized technical operator using the internal API key | `POST /api/v1/documents/sync-directory` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
| Authorized technical operator using the internal API key | `POST /api/v1/documents/upload` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/documents.py` |
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
