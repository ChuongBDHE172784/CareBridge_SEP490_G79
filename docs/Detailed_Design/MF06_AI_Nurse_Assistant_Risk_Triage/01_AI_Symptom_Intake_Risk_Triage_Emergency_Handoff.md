# MF-06 / Spec 01 — AI Symptom Intake, Risk Triage & Emergency Handoff

| Field | Value |
| --- | --- |
| Feature | MF-06 — AI Nurse Assistant & Risk Triage |
| Use Cases Covered | UC-72 Run AI Symptom Intake, UC-73 View Risk Triage Result, UC-74 Open Emergency Support from a Red Risk Result |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother describes symptoms through a guided AI intake conversation; the system classifies the result into a non-diagnostic GREEN/YELLOW/RED orientation using approved knowledge and red-flag rules, and — only for RED — offers a one-tap handoff into the Emergency Map flow (MF-07), never treating the AI output as a diagnosis. |
| Grounding (source code) | `triage/entity/IntakeSession.java`, `triage/IntakeStatus.java`, `triage/RiskLevel.java`, `triage/TriageStage.java`, `triage/controller/IntakeController.java` (`/api/v1/triage/intake`), `emergency/entity/EmergencyMapHandoff.java`, `HandoffStatus.java`, `emergency/controller/EmergencyMapHandoffController.java` (`/api/v1/map/emergency/handoff`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là luồng an toàn quan trọng nhất của MF-06 (CC-01, NS-05). Mother mô tả triệu chứng
qua hội thoại có cấu trúc (`POST /conversation/start` → `/conversation/continue`), hệ
thống chuẩn hoá input, truy xuất tri thức đã duyệt (spec 02) và áp cờ đỏ bảo thủ để suy
ra `riskLevel` (GREEN/YELLOW/RED — UC-72). Kết quả luôn kèm `disclaimer` phi chẩn đoán
và hướng dẫn bước tiếp theo an toàn (UC-73). Khi `riskLevel=RED`, giao diện hiển thị lối
tắt sang bản đồ khẩn cấp; Mother chủ động bấm để tạo `EmergencyMapHandoff` tham chiếu lại
`IntakeSession` gốc (`triageHandoffId`), **không tự động điều hướng và không phải là một
cuộc gọi cấp cứu** (UC-74) — quyền quyết định luôn thuộc về người dùng.

## 2. Class Diagram

```plantuml
@startuml MF06_01_AITriage_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class IntakeSession {
  + id: UUID
  + userId: UUID
  + babyProfileId: UUID
  + motherProfileId: UUID
  + stage: TriageStage
  + symptoms: String
  + rawAiResponse: String
  + riskLevel: RiskLevel
  + status: IntakeStatus
  + disclaimer: String
  + completedAt: Instant
}

enum TriageStage {
  PRECONCEPTION
  PREGNANCY
  INFANT
  TODDLER
}

enum IntakeStatus {
  PENDING
  PROCESSING
  NEED_MORE_INFO
  COMPLETED
  FAILED
}

enum RiskLevel {
  GREEN
  YELLOW
  RED
}

class EmergencyMapHandoff {
  + handoffId: UUID
  + userId: UUID
  + triageHandoffId: UUID
  + riskLevel: String
  + userLatitude: BigDecimal
  + userLongitude: BigDecimal
  + selectedFacilityId: UUID
  + summary: String
  + status: HandoffStatus
}

class IntakeController {
  - intakeService: IntakeService
  + start(request): ResponseEntity
  + continueConversation(sessionId, request): ResponseEntity
  + get(sessionId): ResponseEntity
}

interface IntakeService <<interface>> {
  + start(userId: UUID, request): IntakeSession
  + continueConversation(sessionId: UUID, request): IntakeSession
}

class IntakeServiceImpl implements IntakeService {
  - intakeSessionRepository: IntakeSessionRepository
  - redFlagRuleService: RedFlagRuleService
  - evidenceRetrievalService: EvidenceRetrievalService
  - auditService: AuditService
}

class EmergencyMapHandoffController {
  + createHandoff(request): ResponseEntity
  + myHandoffs(): ResponseEntity
}

IntakeSession --> TriageStage
IntakeSession --> IntakeStatus
IntakeSession --> RiskLevel
IntakeSession "1" -- "0..1" EmergencyMapHandoff : triageHandoffId (UC-74)
EmergencyMapHandoff --> HandoffStatus
IntakeController --> IntakeService : uses
IntakeServiceImpl --> RedFlagRuleService : applies conservative fallback
EmergencyMapHandoffController ..> IntakeSession : references by triageHandoffId

@enduml
```

**Hình 1 — Class Diagram: AI Intake Session, Risk Level & Emergency Handoff Reference**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF06_01_AITriage_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "IntakeController" as Controller
participant "IntakeServiceImpl" as Service
participant "RedFlagRuleService" as RedFlag
participant "EvidenceRetrievalService" as Evidence
participant "AuditService" as Audit
participant "EmergencyMapHandoffController" as HandoffController
database "PostgreSQL" as DB

== UC-72 Run AI Symptom Intake ==
M -> Controller : POST /api/v1/triage/intake/conversation/start\n{stage=PREGNANCY, initialSymptoms}
Controller -> Service : start(userId, request)
Service -> DB : INSERT INTO intake_sessions (status=PENDING)
Service --> Controller : IntakeSession{status=PENDING}
Controller --> M : HTTP 201 Created

loop hội thoại thu thập thêm ngữ cảnh
  M -> Controller : POST /api/v1/triage/intake/conversation/continue\n{sessionId, answer}
  Controller -> Service : continueConversation(sessionId, answer)
  Service -> DB : UPDATE intake_sessions SET status='PROCESSING'
  Service -> RedFlag : evaluate(symptoms) → cờ đỏ?
  Service -> Evidence : retrieveApprovedKnowledge(symptoms, stage)
  Evidence --> Service : approvedContext[]
  Service -> Service : compose AI answer trong khung an toàn\n(fallback bảo thủ nếu không chắc chắn)
  Service -> DB : UPDATE intake_sessions\nSET status='COMPLETED', risk_level=?, disclaimer=?
  Service -> Audit : emit(AI_TRIAGE)
  Service --> Controller : IntakeSession{status=COMPLETED, riskLevel}
  Controller --> M : HTTP 200 OK
end

== UC-73 View Risk Triage Result ==
M -> Controller : GET /api/v1/triage/intake/{sessionId}
Controller -> DB : SELECT * FROM intake_sessions WHERE id=?
DB --> Controller : session{riskLevel, disclaimer}
Controller --> M : HTTP 200 OK {riskLevel, guidance, disclaimer}

== UC-74 Open Emergency Support from a Red Risk Result ==
alt riskLevel == RED
  M -> HandoffController : POST /api/v1/map/emergency/handoff\n{triageHandoffId=sessionId, userLatitude, userLongitude}
  HandoffController -> DB : INSERT INTO emergency_map_handoffs (status=OPEN)
  HandoffController --> M : HTTP 201 Created\n→ điều hướng sang Emergency Map (MF-07)
end

@enduml
```

**Hình 2 — Sequence Diagram: Start Intake → Conversation → View Result → Emergency Handoff (Main Flow)**

## 4. State Machine — `IntakeSession.status` gating `RiskLevel`

```plantuml
@startuml MF06_01_IntakeStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> PENDING : POST /conversation/start (UC-72)

PENDING --> PROCESSING : POST /conversation/continue
PROCESSING --> NEED_MORE_INFO : Thông tin chưa đủ để phân loại rủi ro\n[hệ thống hỏi thêm]
NEED_MORE_INFO --> PROCESSING : Mother cung cấp thêm

PROCESSING --> COMPLETED : Đủ ngữ cảnh → gán riskLevel (GREEN/YELLOW/RED)
PROCESSING --> FAILED : Lỗi hệ thống/AI service không khả dụng\n[fallback an toàn, không suy diễn rủi ro]

COMPLETED --> [*]
FAILED --> [*]

note right of COMPLETED
  riskLevel chỉ được gán khi status=COMPLETED.
  RED → hiển thị lối tắt UC-74 (không tự động điều hướng).
end note

@enduml
```

**Hình 3 — State Machine: `IntakeSession.status` Lifecycle**

## 5. Business Rules Applied

- CC-01 / Excluded (SRS mục 4.8) — AI không chẩn đoán, không kê đơn, không tư vấn liều lượng; luôn kèm `disclaimer`.
- NS-05 — chuẩn hoá input, chỉ truy xuất tri thức đã duyệt (spec 02), áp fallback bảo thủ khi không chắc chắn, ghi log phiên bản rule/nguồn dùng.
- UC-74 — handoff sang Emergency Map là hành động **do người dùng chủ động chọn**, hệ thống không tự dispatch hay đảm bảo có chuyên gia/xe cấp cứu đến.
- BR-PRIVACY — nội dung intake (`symptoms`, `rawAiResponse`) là dữ liệu sức khỏe nhạy cảm, chỉ chủ sở hữu truy cập được.
