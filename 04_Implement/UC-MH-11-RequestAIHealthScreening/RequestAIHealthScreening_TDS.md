# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TECHNICAL DESIGN SPECIFICATION — Request AI Health Overview Screening

| Field | Value |
| --- | --- |
| Document ID | `UC-MH-11-TDS` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Function ID | `UC-MH-11` |
| Canonical Use Case | `UC-MH-11 — Request AI Health Overview Screening` |
| Module / Bounded Context | `Mother Journey and Health` |
| Primary Actor | `Mother` |
| Platforms | `Mobile / Python AI Service` |
| Priority | `Medium` |
| Data Classification | `Restricted maternal health, screening, journey, record, and attachment data; Confidential schedule/preferences` |
| Compliance Scope | `PDPA health-data minimization, consent/ownership enforcement, clinical disclaimer where applicable, and purpose-bound file access` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree on `2026-08-23`; SRS `UC-MH-11`; exact evidence in Section 1.4 |

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

- **Goal:** Submit a recent metric or latest multi-metric snapshot to deterministic maternal screening and follow the NORMAL, ANOMALY_MONITOR, or CRITICAL_EMERGENCY result.
- **Trigger:** The actor enters CTA in Mobile metric trend.
- **Outcome:** Display the result and route yellow to prefilled AI chat or red to emergency map.
- **Current state:** `High` confidence from reachable code/test audit; documented limitations remain visible below.
- **Target state:** Preserve current code-backed behavior and resolve only explicitly evidenced limitations through approved implementation work.

### 1.2 Scope

**In scope**

- CTA in Mobile metric trend
- Automatic check after metric save

- POST Python `/api/v1/metrics/evaluate`

**Out of scope / limitations**

- Open / current limitation: Mobile currently embeds an internal key and calls Python directly; replace with a server-side gateway before production.

### 1.3 Preconditions and Postconditions

| Type | Condition |
| --- | --- |
| Precondition | Mother is authenticated/authorized where the current contract requires it. |
| Precondition | Required ownership, membership, consent, resource state, and device/provider prerequisites pass current policies. |
| Postcondition | Display the result and route yellow to prefilled AI chat or red to emergency map. |
| Postcondition | No side effect outside the feature-owned persistence/event/provider boundary occurs. |

### 1.4 Evidence Baseline

| Source ID | Type | Exact path | Revision | Authority |
| --- | --- | --- | --- | --- |
| `SRC-SRS-01` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-11 | 2026-08-23 | Draft code-first requirement |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_metric_trend_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Worktree `2026-08-23` | Current-state evidence |

### 1.5 Open Contradictions / Questions

- Open / current limitation: Mobile currently embeds an internal key and calls Python directly; replace with a server-side gateway before production.

## 2. Traceability Matrix

| Requirement | Behavior | Exact oracle source | Component | Test condition / case |
| --- | --- | --- | --- | --- |
| `UC-MH-11-FR-01` | Build the supported single/latest metric payload. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-11 Normal Flow 1 | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | `COND-01` / `UC-MH-11-TC-001` |
| `UC-MH-11-FR-02` | Run deterministic validation/classification and optional RAG context lookup. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-11 Normal Flow 2 | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | `COND-02` / `UC-MH-11-TC-002` |
| `UC-MH-11-FR-03` | Display the result and route yellow to prefilled AI chat or red to emergency map. | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-11 Normal Flow 3 | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | `COND-03` / `UC-MH-11-TC-003` |
| `BR-01` | Deterministic thresholds establish the safety result; retrieval/generation cannot lower it. | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | `COND-BR-01` / `UC-MH-11-TC-BR-001` |
| `BR-02` | The feature is screening and guidance, not diagnosis. | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | `COND-BR-02` / `UC-MH-11-TC-BR-002` |

## 3. Architecture Decision Records (ADR)

### ADR-UC-MH-11-01 — Use a distinct actor-goal boundary

| Item | Decision |
| --- | --- |
| Context | The retired 43-UC catalogue grouped multiple triggers, lifecycles, and permission boundaries, making implementation/test traceability generic. |
| Options | Keep the broad catalogue; split by screen; split by actor goal plus lifecycle/security boundary. |
| Decision | Use `UC-MH-11 — Request AI Health Overview Screening` as the canonical boundary because its operations share the stated actor outcome and current implementation evidence. |
| Consequences | Related supporting screens/endpoints stay in one TDS; different lifecycle/actor outcomes have separate UCs. |
| Source / Status | SRS Section 3.1 and current code audit / Draft |

### ADR-UC-MH-11-02 — Preserve unknowns as Open

| Item | Decision |
| --- | --- |
| Context | Exact schema fields, SLA values, and some controller error codes are not fully evidenced by this manifest alone. |
| Decision | Do not invent them. Mark them `Open` and require the exact DTO/entity/migration/policy oracle before production-code changes. |
| Consequences | This document accurately characterizes current scope; unresolved design-changing items block implementation approval. |
| Source / Status | `create-specs` evidence discipline / Draft |

## 4. Non-Functional Requirements and SLA

| NFR | Target | Oracle Source | Verification |
| --- | --- | --- | --- |
| Authorization / isolation | All requests follow exact role/ownership/membership/consent policy in current code. | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | Negative security cases in paired Test-Spec |
| Protected-data handling | No secrets or unnecessary health/location/identity/conversation/file payloads in logs, fixtures, screenshots, or audit detail. | Data classification header plus exact Section 9 request/response field inventories | Log/fixture review plus security tests |
| Availability / latency | Open — no approved feature-specific numeric SLA found. | Evidence needed: approved NFR/SLA source | Measure only; do not assert a fixed threshold |
| Accessibility | Open — confirm project-standard criteria for reachable UI surfaces. | Evidence needed: approved UX/accessibility standard | Applicable Web/Mobile UI checks |
| Retry / idempotency | Apply only semantics explicitly implemented by the owning service. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics | State/duplicate/concurrency cases where applicable |

## 5. Static Modeling

### 5.1 Component Responsibilities and Change Disposition

| Exact path | Disposition | Responsibility |
| --- | --- | --- |
| `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | Reuse | Current implementation evidence for Request AI Health Overview Screening; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | Reuse | Current implementation evidence for Request AI Health Overview Screening; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_metric_trend_screen.dart` | Reuse | Current implementation evidence for Request AI Health Overview Screening; inspect the exact symbol before implementation changes. |
| `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart` | Reuse | Current implementation evidence for Request AI Health Overview Screening; inspect the exact symbol before implementation changes. |

### 5.2 Current Component Diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
class metrics as "metrics.py"
class metrics_screening_service as "metrics_screening_service.py"
metrics --> metrics_screening_service
class health_metric_trend_screen as "health_metric_trend_screen.dart"
metrics_screening_service --> health_metric_trend_screen
class add_maternal_health_metric_screen as "add_maternal_health_metric_screen.dart"
health_metric_trend_screen --> add_maternal_health_metric_screen
@enduml
```

### 5.3 Data / Schema / Migration Assessment

| Item | Assessment |
| --- | --- |
| Current stores/entities | `05_Development/CareBridgeAITriageService/app/core/database.py`; `05_Development/CareBridgeAITriageService/app/models/schemas.py`; `05_Development/CareBridgeAITriageService/app/rag/vector_store.py` |
| Sensitive fields | Restricted maternal health, screening, journey, record, and attachment data; Confidential schedule/preferences. Exact transport fields and validators are enumerated per handler in Section 9; entity-only fields require the cited service/entity source before a schema change. |
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
Actor -> Client: Enter Request AI Health Overview Screening
Client -> Domain: Build the supported single/latest metric payload.
Domain --> Client: Result for step 1
Client -> Domain: Run deterministic validation/classification and optional RAG context lookup.
Domain --> Client: Result for step 2
Client -> Domain: Display the result and route yellow to prefilled AI chat or red to emergency map.
Domain --> Client: Result for step 3
Client --> Actor: Render canonical outcome
@enduml
```

### 6.2 Alternative, Error, Retry, and Concurrency Flows

| Flow | Expected design behavior | Oracle |
| --- | --- | --- |
| Cancel before mutation | No unintended write or provider side effect. | SRS UC-MH-11 Alternative Flow |
| Invalid input/state | Reject with the current contract; keep canonical state unchanged. | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Wrong actor/scope | Fail closed without protected resource disclosure. | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Dependency failure | Use only the implemented bounded fallback/retry; never report false success. | `05_Development/CareBridgeAITriageService/app/core/security.py`; `05_Development/CareBridgeAITriageService/app/rag/vector_store.py` |
| Duplicate/concurrent mutation | Apply only current lock/version/idempotency semantics. | No explicit lock/version/idempotency marker is evidenced in the cited implementation sources; preserve observed behavior and add characterization before changing concurrency semantics |

### 6.3 State Model and Invariants

```plantuml
@startuml
[*] --> Eligible
Eligible --> InProgress : Build the supported single/latest metric payload.
InProgress --> Outcome : Display the result and route yellow to prefilled AI chat or red to emergency map.
InProgress --> Rejected : validation / authorization / state failure
Outcome --> [*]
Rejected --> Eligible : actor corrects eligible input
@enduml
```

- Deterministic thresholds establish the safety result; retrieval/generation cannot lower it.
- The feature is screening and guidance, not diagnosis.

## 7. Domain Event Catalog

| Direction | Event | Producer / Consumer | Payload / delivery / idempotency |
| --- | --- | --- | --- |
| Publish / consume | Current event evidence | Not applicable at this cited baseline — no event type, publisher, or listener is evidenced in the listed implementation sources | Preserve only events evidenced by the cited source set; if later call-path inspection finds none, the flow remains synchronous. |

## 8. Interface Specification

### 8.1 User / Operator Interfaces

| # | Entry point | Actor | Contract |
| ---: | --- | --- | --- |
| 1 | CTA in Mobile metric trend | Mother | Reachable current entry point |
| 2 | Automatic check after metric save | Mother | Reachable current entry point |

### 8.2 Service / Repository Interfaces

| Interface evidence | Required responsibility |
| --- | --- |
| `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_metric_trend_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |
| `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart` | Support the mapped operations without broadening authorization or lifecycle semantics. |

## 9. API Specification

| ID | Method / path or grouped controller surface | Auth / role | Request / response / errors |
| --- | --- | --- | --- |
| `API-01` | `POST /api/v1/metrics/evaluate` | Internal API key via `verify_internal_api_key` dependency | Handler `evaluate_maternal_metrics`; parameters: body `request`: `HealthMetricsLogRequest`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; request body: `HealthMetricsLogRequest`; request fields/validation: `user_id`: `Optional[str]` (`Field(default=None, description="ID của mẹ bầu")`); `stage`: `MaternalStage` (`Field(default=MaternalStage.PREGNANCY, description="Giai đoạn: PRECONCEPTION, PREGNANCY, POSTPARTUM")`); `gestational_age_weeks`: `Optional[int]` (`Field(default=None, ge=1, le=44, description="Tuần thai (1 - 42 tuần)")`); `systolic_bp`: `Optional[int]` (`Field(default=None, ge=50, le=260, description="Huyết áp tâm thu (mmHg)")`); `diastolic_bp`: `Optional[int]` (`Field(default=None, ge=30, le=160, description="Huyết áp tâm trương (mmHg)")`); `blood_glucose`: `Optional[float]` (`Field(default=None, ge=1.0, le=600.0, description="Chỉ số đường huyết (mmol/L hoặc mg/dL)")`); `glucose_context`: `Optional[GlucoseMeasurementContext \| str]` (`Field(default=None, description="Ngữ cảnh đo đường huyết: FASTING, PRE_MEAL, POST_MEAL_1H, POST_MEAL_2H, RANDOM, OTHER_APPROVED" )`); `is_fasting_glucose`: `Optional[bool]` (`Field(default=None, description="Đo lúc đói hay sau ăn (Backward-compatible)")`); `temperature`: `Optional[float]` (`Field(default=None, ge=34.0, le=43.0, description="Thân nhiệt (°C)")`); `heart_rate`: `Optional[int]` (`Field(default=None, ge=30, le=250, description="Nhịp tim (lần/phút)")`); `weight_kg`: `Optional[float]` (`Field(default=None, ge=20.0, le=300.0, description="Cân nặng hiện tại (kg)")`); `height_cm`: `Optional[float]` (`Field(default=None, ge=50.0, le=250.0, description="Chiều cao (cm)")`); `bmi`: `Optional[float]` (`Field(default=None, ge=10.0, le=250.0, description="Chỉ số khối cơ thể BMI (kg/m²)")`); `fetal_movements_count`: `Optional[int]` (`Field(default=None, ge=0, le=100, description="Số lần thai cử động")`); `fetal_movements_duration_hours`: `Optional[int]` (`Field(default=2, ge=1, le=12, description="Khoảng thời gian đếm (mặc định 2 giờ)")`); `water_intake_ml`: `Optional[int]` (`Field(default=None, ge=0, le=10000, description="Lượng nước uống trong ngày (ml)")`); `epds_score`: `Optional[int]` (`Field(default=None, ge=0, le=30, description="Điểm sàng lọc trầm cảm/tâm trạng EPDS (0-30)")`); `epds_question_10_score`: `Optional[int]` (`Field(default=None, ge=0, le=3, description="Điểm câu hỏi số 10 EPDS về ý nghĩ tự gây hại (0-3)")`); `sleep_hours`: `Optional[float]` (`Field(default=None, ge=0.0, le=24.0, description="Thời lượng giấc ngủ (giờ)")`); `symptoms`: `List[str]` (`Field(default_factory=list, description="Danh sách triệu chứng chọn (nhức đầu, phù chân, đau bụng...)")`); `free_text_notes`: `Optional[str]` (`Field(default=None, description="Ghi chú mô tả thêm của mẹ bầu")`); response: `HealthMetricsEvaluationResponse`; response payload fields: `status`: `TriageRiskStatus` (`Field(description="Mức độ rủi ro: NORMAL, ANOMALY_MONITOR, CRITICAL_EMERGENCY")`); `emergency_mode`: `bool` (`Field(description="True nếu cần kích hoạt Emergency Mode (Gọi 115, SOS, Bệnh viện)")`); `headline`: `str` (`Field(description="Tiêu đề thông báo nhanh cho mẹ bầu")`); `summary`: `str` (`Field(description="Đánh giá chi tiết và phân tích các chỉ số sức khỏe")`); `risk_factors`: `List[str]` (`Field(default_factory=list, description="Các yếu tố nguy cơ phát hiện được")`); `suggested_action`: `str` (`Field(description="Hành động khuyến nghị (Kích hoạt cấp cứu / Chat AI Nurse / Tiếp tục theo dõi)")`); `relevant_sources`: `List[SourceCitation]` (`Field(default_factory=list, description="Tài liệu cẩm nang y tế đối soát")`); `evaluated_at`: `datetime` (`Field(default_factory=datetime.utcnow)`); `disclaimer`: `str` (`Field(description="Cảnh báo y tế pháp lý")`); explicit/documented statuses: `200`; source: `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |

Method-level Spring handlers, authorization annotations, request DTO fields/validators, response payload fields, and explicit/documented statuses above were extracted from the cited current source. Service/advice-only application error codes are not claimed where the controller does not declare them.

### 9.1 Handler Contract — `POST /api/v1/metrics/evaluate`

| Item | Exact current contract |
| --- | --- |
| Handler | `evaluate_maternal_metrics` |
| Source | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Authorization annotation / boundary | Internal API key via `verify_internal_api_key` dependency |
| Parameters | body `request`: `HealthMetricsLogRequest`; dependency `_auth`: `str`; dependency `db`: `AsyncSession` |
| Request body type | `HealthMetricsLogRequest` |
| Request fields and validators | `user_id`: `Optional[str]` (`Field(default=None, description="ID của mẹ bầu")`); `stage`: `MaternalStage` (`Field(default=MaternalStage.PREGNANCY, description="Giai đoạn: PRECONCEPTION, PREGNANCY, POSTPARTUM")`); `gestational_age_weeks`: `Optional[int]` (`Field(default=None, ge=1, le=44, description="Tuần thai (1 - 42 tuần)")`); `systolic_bp`: `Optional[int]` (`Field(default=None, ge=50, le=260, description="Huyết áp tâm thu (mmHg)")`); `diastolic_bp`: `Optional[int]` (`Field(default=None, ge=30, le=160, description="Huyết áp tâm trương (mmHg)")`); `blood_glucose`: `Optional[float]` (`Field(default=None, ge=1.0, le=600.0, description="Chỉ số đường huyết (mmol/L hoặc mg/dL)")`); `glucose_context`: `Optional[GlucoseMeasurementContext \| str]` (`Field(default=None, description="Ngữ cảnh đo đường huyết: FASTING, PRE_MEAL, POST_MEAL_1H, POST_MEAL_2H, RANDOM, OTHER_APPROVED" )`); `is_fasting_glucose`: `Optional[bool]` (`Field(default=None, description="Đo lúc đói hay sau ăn (Backward-compatible)")`); `temperature`: `Optional[float]` (`Field(default=None, ge=34.0, le=43.0, description="Thân nhiệt (°C)")`); `heart_rate`: `Optional[int]` (`Field(default=None, ge=30, le=250, description="Nhịp tim (lần/phút)")`); `weight_kg`: `Optional[float]` (`Field(default=None, ge=20.0, le=300.0, description="Cân nặng hiện tại (kg)")`); `height_cm`: `Optional[float]` (`Field(default=None, ge=50.0, le=250.0, description="Chiều cao (cm)")`); `bmi`: `Optional[float]` (`Field(default=None, ge=10.0, le=250.0, description="Chỉ số khối cơ thể BMI (kg/m²)")`); `fetal_movements_count`: `Optional[int]` (`Field(default=None, ge=0, le=100, description="Số lần thai cử động")`); `fetal_movements_duration_hours`: `Optional[int]` (`Field(default=2, ge=1, le=12, description="Khoảng thời gian đếm (mặc định 2 giờ)")`); `water_intake_ml`: `Optional[int]` (`Field(default=None, ge=0, le=10000, description="Lượng nước uống trong ngày (ml)")`); `epds_score`: `Optional[int]` (`Field(default=None, ge=0, le=30, description="Điểm sàng lọc trầm cảm/tâm trạng EPDS (0-30)")`); `epds_question_10_score`: `Optional[int]` (`Field(default=None, ge=0, le=3, description="Điểm câu hỏi số 10 EPDS về ý nghĩ tự gây hại (0-3)")`); `sleep_hours`: `Optional[float]` (`Field(default=None, ge=0.0, le=24.0, description="Thời lượng giấc ngủ (giờ)")`); `symptoms`: `List[str]` (`Field(default_factory=list, description="Danh sách triệu chứng chọn (nhức đầu, phù chân, đau bụng...)")`); `free_text_notes`: `Optional[str]` (`Field(default=None, description="Ghi chú mô tả thêm của mẹ bầu")`) |
| Response type | `HealthMetricsEvaluationResponse` |
| Response payload fields | `status`: `TriageRiskStatus` (`Field(description="Mức độ rủi ro: NORMAL, ANOMALY_MONITOR, CRITICAL_EMERGENCY")`); `emergency_mode`: `bool` (`Field(description="True nếu cần kích hoạt Emergency Mode (Gọi 115, SOS, Bệnh viện)")`); `headline`: `str` (`Field(description="Tiêu đề thông báo nhanh cho mẹ bầu")`); `summary`: `str` (`Field(description="Đánh giá chi tiết và phân tích các chỉ số sức khỏe")`); `risk_factors`: `List[str]` (`Field(default_factory=list, description="Các yếu tố nguy cơ phát hiện được")`); `suggested_action`: `str` (`Field(description="Hành động khuyến nghị (Kích hoạt cấp cứu / Chat AI Nurse / Tiếp tục theo dõi)")`); `relevant_sources`: `List[SourceCitation]` (`Field(default_factory=list, description="Tài liệu cẩm nang y tế đối soát")`); `evaluated_at`: `datetime` (`Field(default_factory=datetime.utcnow)`); `disclaimer`: `str` (`Field(description="Cảnh báo y tế pháp lý")`) |
| Explicit/documented statuses | `200` |
| Positive test mapping | `COND-API-001` / `UC-MH-11-TC-API-001` |
| Negative test mapping | `COND-AUTH`; DTO/service-only negative paths require their exact cited oracle |

## 10. Error Codes

| Error class | HTTP / code | Trigger | Client behavior | Oracle |
| --- | --- | --- | --- | --- |
| Validation/business rejection | No 4xx declared by selected handler syntax; framework/service/advice mapping applies | Invalid field/range/state/ownership input | No write or false success; show only current mapped error | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` plus exact exception advice/service characterization |
| Authentication/authorization | `401/403` only where the security chain or handler policy maps them | Missing credential or disallowed role/scope | Fail closed with no protected response fields | Security configuration and `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
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
| `VG-01` | Build the supported single/latest metric payload. | `COND-01` | `UC-MH-11-TC-001` |
| `VG-02` | Run deterministic validation/classification and optional RAG context lookup. | `COND-02` | `UC-MH-11-TC-002` |
| `VG-03` | Display the result and route yellow to prefilled AI chat or red to emergency map. | `COND-03` | `UC-MH-11-TC-003` |
| `VG-AUTH` | Reject wrong authentication/role/ownership/membership/consent scope | `COND-AUTH` | `UC-MH-11-TC-SEC-001` |
| `VG-GAP` | Characterize each known gap without claiming a false completed path | `COND-GAP` | `UC-MH-11-TC-GAP-001` |

## 14. Verification Methods

Existing focused evidence:

- `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py`
- `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py`

Exact supported commands derived from the audited test paths:

- `cd 05_Development/CareBridgeAITriageService && pytest tests/test_metrics_screening.py`
- `cd 05_Development/CareBridgeAITriageService && pytest tests/test_api_endpoints.py`

Record pass/fail/skip counts only after executing these commands on the exact revision.

## 15. Verification Samples

| Sample | Value |
| --- | --- |
| Primary contract | POST Python `/api/v1/metrics/evaluate` |
| Request | `POST /api/v1/metrics/evaluate` → `evaluate_maternal_metrics`; `HealthMetricsLogRequest` with `user_id`: `Optional[str]` (`Field(default=None, description="ID của mẹ bầu")`); `stage`: `MaternalStage` (`Field(default=MaternalStage.PREGNANCY, description="Giai đoạn: PRECONCEPTION, PREGNANCY, POSTPARTUM")`); `gestational_age_weeks`: `Optional[int]` (`Field(default=None, ge=1, le=44, description="Tuần thai (1 - 42 tuần)")`); `systolic_bp`: `Optional[int]` (`Field(default=None, ge=50, le=260, description="Huyết áp tâm thu (mmHg)")`); `diastolic_bp`: `Optional[int]` (`Field(default=None, ge=30, le=160, description="Huyết áp tâm trương (mmHg)")`); `blood_glucose`: `Optional[float]` (`Field(default=None, ge=1.0, le=600.0, description="Chỉ số đường huyết (mmol/L hoặc mg/dL)")`); `glucose_context`: `Optional[GlucoseMeasurementContext | str]` (`Field(default=None, description="Ngữ cảnh đo đường huyết: FASTING, PRE_MEAL, POST_MEAL_1H, POST_MEAL_2H, RANDOM, OTHER_APPROVED" )`); `is_fasting_glucose`: `Optional[bool]` (`Field(default=None, description="Đo lúc đói hay sau ăn (Backward-compatible)")`); `temperature`: `Optional[float]` (`Field(default=None, ge=34.0, le=43.0, description="Thân nhiệt (°C)")`); `heart_rate`: `Optional[int]` (`Field(default=None, ge=30, le=250, description="Nhịp tim (lần/phút)")`); `weight_kg`: `Optional[float]` (`Field(default=None, ge=20.0, le=300.0, description="Cân nặng hiện tại (kg)")`); `height_cm`: `Optional[float]` (`Field(default=None, ge=50.0, le=250.0, description="Chiều cao (cm)")`); `bmi`: `Optional[float]` (`Field(default=None, ge=10.0, le=250.0, description="Chỉ số khối cơ thể BMI (kg/m²)")`); `fetal_movements_count`: `Optional[int]` (`Field(default=None, ge=0, le=100, description="Số lần thai cử động")`); `fetal_movements_duration_hours`: `Optional[int]` (`Field(default=2, ge=1, le=12, description="Khoảng thời gian đếm (mặc định 2 giờ)")`); `water_intake_ml`: `Optional[int]` (`Field(default=None, ge=0, le=10000, description="Lượng nước uống trong ngày (ml)")`); `epds_score`: `Optional[int]` (`Field(default=None, ge=0, le=30, description="Điểm sàng lọc trầm cảm/tâm trạng EPDS (0-30)")`); `epds_question_10_score`: `Optional[int]` (`Field(default=None, ge=0, le=3, description="Điểm câu hỏi số 10 EPDS về ý nghĩ tự gây hại (0-3)")`); `sleep_hours`: `Optional[float]` (`Field(default=None, ge=0.0, le=24.0, description="Thời lượng giấc ngủ (giờ)")`); `symptoms`: `List[str]` (`Field(default_factory=list, description="Danh sách triệu chứng chọn (nhức đầu, phù chân, đau bụng...)")`); `free_text_notes`: `Optional[str]` (`Field(default=None, description="Ghi chú mô tả thêm của mẹ bầu")`); authorization: Internal API key via `verify_internal_api_key` dependency. |
| Success response | `HealthMetricsEvaluationResponse` with `status`: `TriageRiskStatus` (`Field(description="Mức độ rủi ro: NORMAL, ANOMALY_MONITOR, CRITICAL_EMERGENCY")`); `emergency_mode`: `bool` (`Field(description="True nếu cần kích hoạt Emergency Mode (Gọi 115, SOS, Bệnh viện)")`); `headline`: `str` (`Field(description="Tiêu đề thông báo nhanh cho mẹ bầu")`); `summary`: `str` (`Field(description="Đánh giá chi tiết và phân tích các chỉ số sức khỏe")`); `risk_factors`: `List[str]` (`Field(default_factory=list, description="Các yếu tố nguy cơ phát hiện được")`); `suggested_action`: `str` (`Field(description="Hành động khuyến nghị (Kích hoạt cấp cứu / Chat AI Nurse / Tiếp tục theo dõi)")`); `relevant_sources`: `List[SourceCitation]` (`Field(default_factory=list, description="Tài liệu cẩm nang y tế đối soát")`); `evaluated_at`: `datetime` (`Field(default_factory=datetime.utcnow)`); `disclaimer`: `str` (`Field(description="Cảnh báo y tế pháp lý")`); explicit/documented statuses `200`. |
| Negative sample | Use wrong role/owner/state and assert the exact mapped error without protected payload. |

## 16. Authorization Matrix

| Actor / role | Operation | Decision |
| --- | --- | --- |
| Mother | `POST /api/v1/metrics/evaluate` | Internal API key via `verify_internal_api_key` dependency; handler oracle `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
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
