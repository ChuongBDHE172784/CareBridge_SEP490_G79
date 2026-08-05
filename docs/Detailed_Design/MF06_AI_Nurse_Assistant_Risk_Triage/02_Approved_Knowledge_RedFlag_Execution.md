# MF-06 / Spec 02 — Approved Knowledge Retrieval & Red-Flag Execution

| Field | Value |
| --- | --- |
| Feature | MF-06 — AI Nurse Assistant & Risk Triage |
| Flows Covered | Retrieve stage-applicable approved evidence sources; run active red-flag pre-screen rules before AI-assisted triage; preserve rule/source context in the result |
| Primary Actor(s) | CareBridge Triage Runtime |
| Secondary Actors | AI Triage Service, Evidence Registry |
| Platform | CareBridge API; AI Triage Service |
| Main Flow Summary | During an authorized structured intake, the runtime loads active red-flag rules, applies the conservative pre-screen, and lets the AI service retrieve only approved evidence sources for the selected care stage. |
| Explicitly Excluded | Standalone RAG chat; community AI-moderation policy UI; autonomous diagnosis; prescription; invented Web knowledge-governance screen |
| Grounding (active runtime) | API `TriageRedFlagPreScreenPolicy`, `TriageRedFlagPolicy`, `RedFlagRuleRepository`, `InternalEvidenceSourceController`, `EvidenceSourceServiceImpl`; AI service `app/evidence_registry_client.py` |

## 1. Tổng quan luồng chính (Main Flow Overview)

Spec này mô tả control thực sự được gọi trong luồng triage, không mô tả một màn hình
quản trị giả định. Backend pre-screen đọc các `RedFlagRule` đang active trước khi tin
tưởng kết quả từ mô hình. AI service chỉ lấy danh sách `EvidenceSource` có trạng thái
`APPROVED` và phù hợp stage qua internal endpoint có khóa dịch vụ.

Màn hình `/admin/safety-rules` trên Web hiện quản lý red-flag/community AI moderation
policy; phần moderation thuộc MF-04 và không được xem là Web UI của AI Nurse. Mobile
`RagChatScreen` cũng không có route hợp lệ, nên standalone RAG chat không thuộc flow.

## 2. Class Diagram

```plantuml
@startuml MF06_02_KnowledgeRedFlagExecution_ClassDiagram
skinparam classAttributeIconSize 0

class RedFlagRule {
  + id: UUID
  + keyword: String
  + severity: RedFlagSeverity
  + action: RedFlagAction
  + active: Boolean
  + systemDefault: Boolean
}

class EvidenceSource {
  + id: UUID
  + domain: String
  + organization: String
  + category: String
  + status: String
  + applicableStages: String
}

class TriageRedFlagPreScreenPolicy {
  + evaluate(request): PreScreenDecision
}

class TriageRedFlagPolicy {
  + apply(result, rules): TriageResult
}

class InternalEvidenceSourceController {
  + approvedForStage(stage): ApiResponse
}

interface EvidenceSourceService
class EvidenceSourceServiceImpl
interface RedFlagRuleRepository
interface EvidenceSourceRepository
class EvidenceRegistryClient <<Python>>

TriageRedFlagPreScreenPolicy --> RedFlagRuleRepository
TriageRedFlagPolicy --> RedFlagRuleRepository
InternalEvidenceSourceController --> EvidenceSourceService
EvidenceSourceServiceImpl ..|> EvidenceSourceService
EvidenceSourceServiceImpl --> EvidenceSourceRepository
EvidenceRegistryClient --> InternalEvidenceSourceController : internal HTTPS
@enduml
```

**Hình 1 — Class Diagram: Approved Knowledge and Red-Flag Runtime Controls**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF06_02_KnowledgeRedFlagExecution_SequenceDiagram
actor "Triage Runtime" as Runtime
participant "TriageRedFlagPreScreenPolicy" as PreScreen
participant "InternalEvidenceSourceController" as EvidenceController
participant "EvidenceSourceServiceImpl" as EvidenceService
participant "RedFlagRuleRepository" as RuleRepo
participant "EvidenceSourceRepository" as EvidenceRepo
database "PostgreSQL" as DB
participant "AI Triage Service / EvidenceRegistryClient" as AI

Runtime -> PreScreen : 1. evaluate(structuredIntake)
activate PreScreen
PreScreen -> RuleRepo : 2. findByActiveTrue()
activate RuleRepo
RuleRepo -> DB : 3. SELECT active red_flag_rules
activate DB
DB --> RuleRepo : 4. active rules[]
deactivate DB
RuleRepo --> PreScreen : 5. active rules[]
deactivate RuleRepo
PreScreen -> PreScreen : 1a. match normalized intake conservatively
activate PreScreen
PreScreen --> PreScreen : 1b. PreScreenDecision
deactivate PreScreen
alt [blocking red flag matched]
  PreScreen --> Runtime : 6a. RED decision with safe next action
  deactivate PreScreen
else [AI-assisted evaluation may continue]
  PreScreen --> Runtime : 6b. continue with risk floor/context
  deactivate PreScreen
  Runtime -> AI : 7. request non-diagnostic triage(stage, normalized context)
  activate AI
  AI -> EvidenceController : 8. GET /internal/api/v1/triage/evidence-sources/approved?stage={stage}\nX-CareBridge-Internal-Key
  activate EvidenceController
  EvidenceController -> EvidenceService : 9. approvedForStage(stage)
  activate EvidenceService
  EvidenceService -> EvidenceRepo : 10. findByStatus(APPROVED)
  activate EvidenceRepo
  EvidenceRepo -> DB : 11. SELECT approved evidence_sources
  activate DB
  DB --> EvidenceRepo : 12. sources[]
  deactivate DB
  EvidenceRepo --> EvidenceService : 13. sources[]
  deactivate EvidenceRepo
  EvidenceService -> EvidenceService : 9a. filter applicableStages
  activate EvidenceService
  EvidenceService --> EvidenceService : 9b. stage-applicable sources[]
  deactivate EvidenceService
  EvidenceService --> EvidenceController : 14. approved sources[]
  deactivate EvidenceService
  EvidenceController --> AI : 15. 200 OK
  deactivate EvidenceController
  AI --> Runtime : 16. risk orientation, citations and safe next action
  deactivate AI
end
@enduml
```

**Hình 2 — Sequence Diagram: Red-Flag Pre-Screen and Approved Evidence Retrieval**

## 4. Business Rules Applied

- Chỉ rule `active=true` và evidence source `APPROVED` phù hợp stage được dùng.
- Red-flag policy đặt ngưỡng bảo thủ và có quyền nâng mức rủi ro; mô hình không được tự hạ risk floor do rule tạo ra.
- Internal evidence endpoint yêu cầu service key; nó không cấp quyền Admin cho AI service.
- Nếu evidence registry lỗi hoặc không có nguồn phù hợp, hệ thống phải fail safe và không tạo câu trả lời y khoa không có grounding.
- Kết quả chỉ là green/yellow/red orientation và safe next action, không chẩn đoán hoặc kê đơn.
