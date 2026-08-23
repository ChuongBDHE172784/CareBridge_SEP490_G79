# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0

# TEST SPECIFICATION — Request AI Health Overview Screening

| Field | Value |
| --- | --- |
| Document ID | `UC-MH-11-TEST-SPEC` |
| Version | `0.1` |
| Date | `2026-08-23` |
| Status | `Draft` |
| Feature / Gap ID | `UC-MH-11` |
| Module | `Mother Journey and Health` |
| Paired TDS | `UC-MH-11-TDS` |
| Priority | `Medium` |
| Platforms | `Mobile / Python AI Service` |
| Data Classification | `Restricted maternal health, screening, journey, record, and attachment data; Confidential schedule/preferences` |
| Compliance Scope | `PDPA health-data minimization, consent/ownership enforcement, clinical disclaimer where applicable, and purpose-bound file access` |
| Owner | `CareBridge Team` |
| Reviewer / Approver |  |
| Source Baseline | Current worktree `2026-08-23`; SRS/TDS `UC-MH-11` and exact code/test sources below |

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
| Actor goal | Submit a recent metric or latest multi-metric snapshot to deterministic maternal screening and follow the NORMAL, ANOMALY_MONITOR, or CRITICAL_EMERGENCY result. | SRS `UC-MH-11` |
| Current state | `High` confidence; gaps are listed in Section 2 | Exact current code/test sources below |
| Entry points | CTA in Mobile metric trend; Automatic check after metric save | Current client/router evidence |
| Authorization boundary | `Mother` plus exact authentication/role/ownership/membership/consent policy | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Primary operations | Build the supported single/latest metric payload.; Run deterministic validation/classification and optional RAG context lookup.; Display the result and route yellow to prefilled AI chat or red to emergency map. | SRS `UC-MH-11` Normal Flow |
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
| `SRC-SRS` | Requirement | `02_Requirements/SRS/Report3_Functional_Specifications.md` — UC-MH-11 | 2026-08-23 | Draft code-first requirement |
| `SRC-TDS` | Design | Paired `UC-MH-11-TDS` | 0.1 | Draft design |
| `SRC-CODE-01` | Current code | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-02` | Current code | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-03` | Current code | `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_metric_trend_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-CODE-04` | Current code | `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-01` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Worktree `2026-08-23` | Current-state evidence |
| `SRC-TEST-02` | Existing test | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Worktree `2026-08-23` | Current-state evidence |

## 2. Logic Issues Resolved

| Issue ID | Discrepancy | Impact | Resolution | Oracle | Status |
| --- | --- | --- | --- | --- | --- |
| `LI-01` | Mobile currently embeds an internal key and calls Python directly; replace with a server-side gateway before production. | Could create false completed coverage or wrong contract | Keep as current limitation/Open until the cited client/server mismatch is resolved | SRS `UC-MH-11` gap plus current code | Open |

Architecture-, schema-, authorization-, and test-changing Open items must be resolved before implementation approval.

## 3. Test Design Specification

### TDS-01 — Risk-Based Scope

| Risk ID | Failure mode | Severity | Likelihood | Detectability | Levels | Conditions |
| --- | --- | --- | --- | --- | --- | --- |
| `RISK-01` | Failure of: Build the supported single/latest metric payload. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-01` |
| `RISK-02` | Failure of: Run deterministic validation/classification and optional RAG context lookup. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-02` |
| `RISK-03` | Failure of: Display the result and route yellow to prefilled AI chat or red to emergency map. | High | Medium | High | Unit/Integration/Contract/applicable UI | `COND-03` |
| `RISK-AUTH` | Cross-user/role/member/consent data access | Critical | Medium | Medium | Security/Integration/Contract | `COND-AUTH` |
| `RISK-GAP` | Documentation claims an unreachable or broken path as complete | High | Medium | High | Characterization/Contract/UI | `COND-GAP` |

#### Platform and Test-Level Applicability Matrix

| Platform / Layer | Unit | Integration | Contract / Component | Widget / UI | E2E | Security |
| --- | --- | --- | --- | --- | --- | --- |
| Backend | Not applicable — no Spring backend layer in this UC | Not applicable — no Spring backend layer in this UC | Not applicable — no Spring backend layer in this UC | Not applicable — backend has no UI | Not applicable — no Spring backend layer in this UC | Not applicable — no Spring backend layer in this UC |
| Web | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC | Not applicable — no reachable Web consumer in this UC |
| Mobile | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points | Applicable — current Mobile entry points |
| AI Service | Applicable — current Python AI contracts | Applicable — current Python AI contracts | Applicable — current Python AI contracts | Not applicable — Python service has no actor UI | Applicable — current Python AI contracts | Applicable — current Python AI contracts |

### TDS-02 — Test Basis and Oracle Hierarchy

| Basis | Requirement / behavior | Exact source | Oracle | Conditions |
| --- | --- | --- | --- | --- |
| `BASIS-01` | `UC-MH-11-FR-01` — Build the supported single/latest metric payload. | SRS `UC-MH-11` Normal Flow 1; TDS Section 2 | Build the supported single/latest metric payload. | `COND-01` |
| `BASIS-02` | `UC-MH-11-FR-02` — Run deterministic validation/classification and optional RAG context lookup. | SRS `UC-MH-11` Normal Flow 2; TDS Section 2 | Run deterministic validation/classification and optional RAG context lookup. | `COND-02` |
| `BASIS-03` | `UC-MH-11-FR-03` — Display the result and route yellow to prefilled AI chat or red to emergency map. | SRS `UC-MH-11` Normal Flow 3; TDS Section 2 | Display the result and route yellow to prefilled AI chat or red to emergency map. | `COND-03` |

Oracle precedence: approved user decision → approved BR/ADR/security policy → paired TDS → current implementation for characterization → existing test as regression evidence.

### TDS-03 — Test Conditions and Coverage Items

| Condition | Basis / risk | Behavior | Layer | Coverage | Test cases |
| --- | --- | --- | --- | --- | --- |
| `COND-01` | `BASIS-01` / `RISK-01` | Build the supported single/latest metric payload. | Mobile / Python AI Service | Positive + applicable boundary/state coverage | `UC-MH-11-TC-001` |
| `COND-02` | `BASIS-02` / `RISK-02` | Run deterministic validation/classification and optional RAG context lookup. | Mobile / Python AI Service | Positive + applicable boundary/state coverage | `UC-MH-11-TC-002` |
| `COND-03` | `BASIS-03` / `RISK-03` | Display the result and route yellow to prefilled AI chat or red to emergency map. | Mobile / Python AI Service | Positive + applicable boundary/state coverage | `UC-MH-11-TC-003` |
| `COND-API-001` | Exact handler/client composition contract | POST /api/v1/metrics/evaluate → evaluate_maternal_metrics contract | Applicable backend/client contract layer | Contract + DTO/status/authorization evidence | `UC-MH-11-TC-API-001` |
| `COND-BR-01` | `BR-01` | Deterministic thresholds establish the safety result; retrieval/generation cannot lower it. | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | Negative / decision / state | `UC-MH-11-TC-BR-001` |
| `COND-BR-02` | `BR-02` | The feature is screening and guidance, not diagnosis. | `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` | Negative / decision / state | `UC-MH-11-TC-BR-002` |
| `COND-AUTH` | `RISK-AUTH` | Reject wrong authentication, role, ownership, membership, consent, or state scope | All protected layers | Security | `UC-MH-11-TC-SEC-001` |
| `COND-GAP` | `RISK-GAP` | Characterize current limitation/reachability without false completion | Applicable layer | Gap/Regression | `UC-MH-11-TC-GAP-001` |

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
| Actor | Synthetic `Mother` plus closest wrong-role/cross-owner identities | Authenticated, unauthenticated, wrong scope, consent revoked | Reset principals/tokens |
| Resource | Minimum valid feature-owned object | Missing, malformed, boundary, stale, already-final, cross-owner | Transaction rollback or isolated repository cleanup |
| Provider/device | Deterministic fake only when applicable | Success, timeout, malformed, permission denied | Reset fake/timers/device state |
| Protected fields | Synthetic non-production values only | Redaction and disclosure checks | Never persist in snapshots/log fixtures |

Existing test evidence:

- `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py`
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

### UC-MH-11-TC-001 — Build the supported single/latest metric payload

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-11-TC-001` |
| Severity | `High` |
| Test Condition | `COND-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-MH-11 Normal Flow 1; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Build the supported single/latest metric payload.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Build the supported single/latest metric payload.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-MH-11 Normal Flow 1; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-11` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Build the supported single/latest metric payload.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-11-TC-002 — Run deterministic validation/classification and optional RAG context lookup

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-11-TC-002` |
| Severity | `High` |
| Test Condition | `COND-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-MH-11 Normal Flow 2; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Run deterministic validation/classification and optional RAG context lookup.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Run deterministic validation/classification and optional RAG context lookup.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-MH-11 Normal Flow 2; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-11` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Run deterministic validation/classification and optional RAG context lookup.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-11-TC-003 — Display the result and route yellow to prefilled AI chat or red to emergency map

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-11-TC-003` |
| Severity | `High` |
| Test Condition | `COND-03` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-MH-11 Normal Flow 3; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-03`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Display the result and route yellow to prefilled AI chat or red to emergency map.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The observable outcome is exactly `Display the result and route yellow to prefilled AI chat or red to emergency map.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed. | `SRS UC-MH-11 Normal Flow 3; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-11` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The observable outcome is exactly `Display the result and route yellow to prefilled AI chat or red to emergency map.` The applicable handler, request/response type, status, and authorization evidence are those enumerated in paired TDS Sections 2, 9, 10, and 16; no additional state is claimed.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-11-TC-API-001 — POST /api/v1/metrics/evaluate → evaluate_maternal_metrics contract

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-11-TC-API-001` |
| Severity | `High` |
| Test Condition | `COND-API-001` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-API-001`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke `POST /api/v1/metrics/evaluate` so `evaluate_maternal_metrics` receives body `request`: `HealthMetricsLogRequest`; dependency `_auth`: `str`; dependency `db`: `AsyncSession`; satisfy `Internal API key via `verify_internal_api_key` dependency` and the extracted `HealthMetricsLogRequest` constraints.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | Success status `200` returns `HealthMetricsEvaluationResponse` with payload fields `status`: `TriageRiskStatus` (`Field(description="Mức độ rủi ro: NORMAL, ANOMALY_MONITOR, CRITICAL_EMERGENCY")`); `emergency_mode`: `bool` (`Field(description="True nếu cần kích hoạt Emergency Mode (Gọi 115, SOS, Bệnh viện)")`); `headline`: `str` (`Field(description="Tiêu đề thông báo nhanh cho mẹ bầu")`); `summary`: `str` (`Field(description="Đánh giá chi tiết và phân tích các chỉ số sức khỏe")`); `risk_factors`: `List[str]` (`Field(default_factory=list, description="Các yếu tố nguy cơ phát hiện được")`); `suggested_action`: `str` (`Field(description="Hành động khuyến nghị (Kích hoạt cấp cứu / Chat AI Nurse / Tiếp tục theo dõi)")`); `relevant_sources`: `List[SourceCitation]` (`Field(default_factory=list, description="Tài liệu cẩm nang y tế đối soát")`); `evaluated_at`: `datetime` (`Field(default_factory=datetime.utcnow)`); `disclaimer`: `str` (`Field(description="Cảnh báo y tế pháp lý")`); the request body is `HealthMetricsLogRequest` with `user_id`: `Optional[str]` (`Field(default=None, description="ID của mẹ bầu")`); `stage`: `MaternalStage` (`Field(default=MaternalStage.PREGNANCY, description="Giai đoạn: PRECONCEPTION, PREGNANCY, POSTPARTUM")`); `gestational_age_weeks`: `Optional[int]` (`Field(default=None, ge=1, le=44, description="Tuần thai (1 - 42 tuần)")`); `systolic_bp`: `Optional[int]` (`Field(default=None, ge=50, le=260, description="Huyết áp tâm thu (mmHg)")`); `diastolic_bp`: `Optional[int]` (`Field(default=None, ge=30, le=160, description="Huyết áp tâm trương (mmHg)")`); `blood_glucose`: `Optional[float]` (`Field(default=None, ge=1.0, le=600.0, description="Chỉ số đường huyết (mmol/L hoặc mg/dL)")`); `glucose_context`: `Optional[GlucoseMeasurementContext \| str]` (`Field(default=None, description="Ngữ cảnh đo đường huyết: FASTING, PRE_MEAL, POST_MEAL_1H, POST_MEAL_2H, RANDOM, OTHER_APPROVED" )`); `is_fasting_glucose`: `Optional[bool]` (`Field(default=None, description="Đo lúc đói hay sau ăn (Backward-compatible)")`); `temperature`: `Optional[float]` (`Field(default=None, ge=34.0, le=43.0, description="Thân nhiệt (°C)")`); `heart_rate`: `Optional[int]` (`Field(default=None, ge=30, le=250, description="Nhịp tim (lần/phút)")`); `weight_kg`: `Optional[float]` (`Field(default=None, ge=20.0, le=300.0, description="Cân nặng hiện tại (kg)")`); `height_cm`: `Optional[float]` (`Field(default=None, ge=50.0, le=250.0, description="Chiều cao (cm)")`); `bmi`: `Optional[float]` (`Field(default=None, ge=10.0, le=250.0, description="Chỉ số khối cơ thể BMI (kg/m²)")`); `fetal_movements_count`: `Optional[int]` (`Field(default=None, ge=0, le=100, description="Số lần thai cử động")`); `fetal_movements_duration_hours`: `Optional[int]` (`Field(default=2, ge=1, le=12, description="Khoảng thời gian đếm (mặc định 2 giờ)")`); `water_intake_ml`: `Optional[int]` (`Field(default=None, ge=0, le=10000, description="Lượng nước uống trong ngày (ml)")`); `epds_score`: `Optional[int]` (`Field(default=None, ge=0, le=30, description="Điểm sàng lọc trầm cảm/tâm trạng EPDS (0-30)")`); `epds_question_10_score`: `Optional[int]` (`Field(default=None, ge=0, le=3, description="Điểm câu hỏi số 10 EPDS về ý nghĩ tự gây hại (0-3)")`); `sleep_hours`: `Optional[float]` (`Field(default=None, ge=0.0, le=24.0, description="Thời lượng giấc ngủ (giờ)")`); `symptoms`: `List[str]` (`Field(default_factory=list, description="Danh sách triệu chứng chọn (nhức đầu, phù chân, đau bụng...)")`); `free_text_notes`: `Optional[str]` (`Field(default=None, description="Ghi chú mô tả thêm của mẹ bầu")`). | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-11` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (Success status `200` returns `HealthMetricsEvaluationResponse` with payload fields `status`: `TriageRiskStatus` (`Field(description="Mức độ rủi ro: NORMAL, ANOMALY_MONITOR, CRITICAL_EMERGENCY")`); `emergency_mode`: `bool` (`Field(description="True nếu cần kích hoạt Emergency Mode (Gọi 115, SOS, Bệnh viện)")`); `headline`: `str` (`Field(description="Tiêu đề thông báo nhanh cho mẹ bầu")`); `summary`: `str` (`Field(description="Đánh giá chi tiết và phân tích các chỉ số sức khỏe")`); `risk_factors`: `List[str]` (`Field(default_factory=list, description="Các yếu tố nguy cơ phát hiện được")`); `suggested_action`: `str` (`Field(description="Hành động khuyến nghị (Kích hoạt cấp cứu / Chat AI Nurse / Tiếp tục theo dõi)")`); `relevant_sources`: `List[SourceCitation]` (`Field(default_factory=list, description="Tài liệu cẩm nang y tế đối soát")`); `evaluated_at`: `datetime` (`Field(default_factory=datetime.utcnow)`); `disclaimer`: `str` (`Field(description="Cảnh báo y tế pháp lý")`); the request body is `HealthMetricsLogRequest` with `user_id`: `Optional[str]` (`Field(default=None, description="ID của mẹ bầu")`); `stage`: `MaternalStage` (`Field(default=MaternalStage.PREGNANCY, description="Giai đoạn: PRECONCEPTION, PREGNANCY, POSTPARTUM")`); `gestational_age_weeks`: `Optional[int]` (`Field(default=None, ge=1, le=44, description="Tuần thai (1 - 42 tuần)")`); `systolic_bp`: `Optional[int]` (`Field(default=None, ge=50, le=260, description="Huyết áp tâm thu (mmHg)")`); `diastolic_bp`: `Optional[int]` (`Field(default=None, ge=30, le=160, description="Huyết áp tâm trương (mmHg)")`); `blood_glucose`: `Optional[float]` (`Field(default=None, ge=1.0, le=600.0, description="Chỉ số đường huyết (mmol/L hoặc mg/dL)")`); `glucose_context`: `Optional[GlucoseMeasurementContext | str]` (`Field(default=None, description="Ngữ cảnh đo đường huyết: FASTING, PRE_MEAL, POST_MEAL_1H, POST_MEAL_2H, RANDOM, OTHER_APPROVED" )`); `is_fasting_glucose`: `Optional[bool]` (`Field(default=None, description="Đo lúc đói hay sau ăn (Backward-compatible)")`); `temperature`: `Optional[float]` (`Field(default=None, ge=34.0, le=43.0, description="Thân nhiệt (°C)")`); `heart_rate`: `Optional[int]` (`Field(default=None, ge=30, le=250, description="Nhịp tim (lần/phút)")`); `weight_kg`: `Optional[float]` (`Field(default=None, ge=20.0, le=300.0, description="Cân nặng hiện tại (kg)")`); `height_cm`: `Optional[float]` (`Field(default=None, ge=50.0, le=250.0, description="Chiều cao (cm)")`); `bmi`: `Optional[float]` (`Field(default=None, ge=10.0, le=250.0, description="Chỉ số khối cơ thể BMI (kg/m²)")`); `fetal_movements_count`: `Optional[int]` (`Field(default=None, ge=0, le=100, description="Số lần thai cử động")`); `fetal_movements_duration_hours`: `Optional[int]` (`Field(default=2, ge=1, le=12, description="Khoảng thời gian đếm (mặc định 2 giờ)")`); `water_intake_ml`: `Optional[int]` (`Field(default=None, ge=0, le=10000, description="Lượng nước uống trong ngày (ml)")`); `epds_score`: `Optional[int]` (`Field(default=None, ge=0, le=30, description="Điểm sàng lọc trầm cảm/tâm trạng EPDS (0-30)")`); `epds_question_10_score`: `Optional[int]` (`Field(default=None, ge=0, le=3, description="Điểm câu hỏi số 10 EPDS về ý nghĩ tự gây hại (0-3)")`); `sleep_hours`: `Optional[float]` (`Field(default=None, ge=0.0, le=24.0, description="Thời lượng giấc ngủ (giờ)")`); `symptoms`: `List[str]` (`Field(default_factory=list, description="Danh sách triệu chứng chọn (nhức đầu, phù chân, đau bụng...)")`); `free_text_notes`: `Optional[str]` (`Field(default=None, description="Ghi chú mô tả thêm của mẹ bầu")`).), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-11-TC-BR-001 — Enforce business rule 1

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-11-TC-BR-001` |
| Severity | `High` |
| Test Condition | `COND-BR-01` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-01; 05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-01`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: Deterministic thresholds establish the safety result; retrieval/generation cannot lower it.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: Deterministic thresholds establish the safety result; retrieval/generation cannot lower it. No disallowed state or protected data is produced. | `TDS BR-01; 05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-11` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: Deterministic thresholds establish the safety result; retrieval/generation cannot lower it. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-11-TC-BR-002 — Enforce business rule 2

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-11-TC-BR-002` |
| Severity | `High` |
| Test Condition | `COND-BR-02` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS BR-02; 05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-BR-02`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Exercise the eligible and ineligible partitions for this rule: The feature is screening and guidance, not diagnosis.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The server enforces: The feature is screening and guidance, not diagnosis. No disallowed state or protected data is produced. | `TDS BR-02; 05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-11` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The server enforces: The feature is screening and guidance, not diagnosis. No disallowed state or protected data is produced.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-11-TC-SEC-001 — Reject wrong authentication, role, ownership, membership, or consent scope

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-11-TC-SEC-001` |
| Severity | `Critical` |
| Test Condition | `COND-AUTH` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `TDS Sections 4 and 16; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-AUTH`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Invoke every protected operation with an unauthenticated principal and the closest disallowed role/scope partition.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects. | `TDS Sections 4 and 16; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-11` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The request fails closed using the exact current mapped status/code and returns no protected resource fields or side effects.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### UC-MH-11-TC-GAP-001 — Characterize the current documented limitation

| Field | Specification |
| --- | --- |
| Stable ID | `UC-MH-11-TC-GAP-001` |
| Severity | `High` |
| Test Condition | `COND-GAP` |
| Test Level | `Unit / Integration / Contract / applicable UI` |
| Platform / Layer | `Mobile / Python AI Service` |
| Technique | `Requirement coverage + state/authorization partition as applicable` |
| Oracle Source | `SRS UC-MH-11 Known Gaps / Exclusions; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Preconditions | Synthetic `Mother` identity; eligible current resource state; isolated provider/database fixtures |
| Intended Test File | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` |
| Initial Status | `🔴 Not written / existing evidence must be rerun` |

**Arrange**

1. Create the minimum synthetic actor and feature-owned resource state required by `COND-GAP`.
2. Fix authentication, clock/provider response, ownership/membership/consent, and current lifecycle state explicitly where applicable.

**Act**

1. Mobile currently embeds an internal key and calls Python directly; replace with a server-side gateway before production.

**Assert — observable result**

| Assertion area | Expected result | Oracle Source |
| --- | --- | --- |
| Response / return | The test records the current limitation without inventing a completed path: Mobile currently embeds an internal key and calls Python directly; replace with a server-side gateway before production. | `SRS UC-MH-11 Known Gaps / Exclusions; 05_Development/CareBridgeAITriageService/app/api/v1/metrics.py` |
| Persistence | Only the state mutation required by this behavior occurs; read/rejection paths assert zero writes. | Paired TDS Sections 5–6 plus the exact handler/service oracle cited by this case; use repository state/spy evidence only when that call path mutates |
| Audit | Feature-owned audit entry only where current code defines it; otherwise `Not applicable — no audited mutation evidenced`. | Paired TDS Sections 7 and 10; current implementation source |
| Event / notification | Only an event/notification explicitly reachable from the cited call path; otherwise `Not applicable — no publisher/listener evidenced`. | Paired TDS Section 7 and current publisher/listener source when present |
| Provider side effect | Exact call count/payload only when the cited call path uses a provider; otherwise `Not applicable — no provider in this operation`. | Current adapter source cited by the paired TDS when present |
| UI state | Reachable client displays the canonical outcome/error without false success. | SRS `UC-MH-11` entry point and current client evidence |
| Privacy / logging | No secret or unnecessary protected payload appears in logs, snapshots, fixtures, or error bodies. | TDS Section 4 security/privacy requirement |

**Failure signature**

The contract differs from the cited expected result (The test records the current limitation without inventing a completed path: Mobile currently embeds an internal key and calls Python directly; replace with a server-side gateway before production.), mutates data on a rejected/read path, leaks protected data, or triggers duplicate/unauthorized side effects.

**Cleanup / isolation**

Rollback the feature-owned transaction/fixture; reset mocks, fake timers, provider fakes, and client state created by this test.


### 4.3 Coverage Families

| Behavior family | Conditions | Cases |
| --- | --- | --- |
| Build the supported single/latest metric payload. | `COND-01` | `UC-MH-11-TC-001` |
| Run deterministic validation/classification and optional RAG context lookup. | `COND-02` | `UC-MH-11-TC-002` |
| Display the result and route yellow to prefilled AI chat or red to emergency map. | `COND-03` | `UC-MH-11-TC-003` |
| POST /api/v1/metrics/evaluate → evaluate_maternal_metrics contract | `COND-API-001` | `UC-MH-11-TC-API-001` |
| Business-rule partitions | `COND-BR-*` | `UC-MH-11-TC-BR-*` |
| Authentication / authorization / ownership / consent | `COND-AUTH` | `UC-MH-11-TC-SEC-001` |
| Current gap / reachability boundary | `COND-GAP` | `UC-MH-11-TC-GAP-001` |

## 5. Red-Green-Refactor Tracker

| TC ID | Intended file | Red evidence | Green evidence | Refactor verification | Status |
| --- | --- | --- | --- | --- | --- |
| `UC-MH-11-TC-001` | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-11-TC-002` | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-11-TC-003` | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-11-TC-API-001` | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-11-TC-BR-001` | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-11-TC-BR-002` | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-11-TC-SEC-001` | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |
| `UC-MH-11-TC-GAP-001` | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` | Not run | Not run | Not run | 🔴 Not written / rerun required |

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
| Generic test matrix | Case titles/actions reference `Request AI Health Overview Screening` operations and rules. | Pass |
| False Green claim | Current command/time/count evidence is required. | Pass — all rows remain Red/not rerun. |
| Hidden contradiction | Section 2 records each known gap. | Open gaps recorded |
| Missing Props Isolation | Applicable Java/TS/Dart factory pattern is present. | Pass at specification level |
| Cross-test pollution | TDS-05 defines actor/resource/provider cleanup. | Draft gate — implementation review must prove teardown/rollback before Green evidence is accepted |
| Wrong-layer test | Applicability matrix marks absent consumers/layers Not applicable. | Pass |
| Uncovered contract | Operations/rules/auth/gap map to conditions and detailed TCs. | Pass for handler/DTO/status/operation/rule/auth/gap mappings; service-only events/codes remain visible in paired TDS |
| Unsafe data | Synthetic-only rule; no production credentials/protected data. | Pass at specification level |
| AI safety bypass | Deterministic policy cannot be lowered by model output when AI applies. | Required |

- [ ] Human reviewer confirms all eight sections, oracle sources, detailed TCs, applicability, Red Gate, rollback, and paired-TDS traceability before approval.
