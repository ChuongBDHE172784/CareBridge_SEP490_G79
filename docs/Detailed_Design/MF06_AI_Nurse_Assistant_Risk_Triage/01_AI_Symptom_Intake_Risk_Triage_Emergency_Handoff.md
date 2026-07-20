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
participant "TriageService" as Service
participant "IIntakeSessionRepository" as SessionRepo
participant "HttpChildTriageAiClient" as AiClient
participant "EvidenceSourceService" as Evidence
participant "EmergencyMapHandoffController" as HandoffController
participant "EmergencyMapHandoffServiceImpl" as HandoffService
participant "EmergencyMapHandoffRepository" as HandoffRepo
database "PostgreSQL" as DB

== UC-72 Run AI Symptom Intake (Start Conversation) ==
M -> Controller : 1. POST /api/v1/triage/intake/conversation/start\n{stage=PREGNANCY, initialText, currentIntake}
activate Controller
Controller -> Service : 2. startConversation(request, userId)
activate Service
Service -> SessionRepo : 3. save(IntakeSession{status=PROCESSING,\nsymptoms="CONVERSATION_INTAKE"})
activate SessionRepo
SessionRepo -> DB : 4. INSERT INTO intake_sessions ...
activate DB
DB --> SessionRepo : 5. saved
deactivate DB
SessionRepo --> Service : 6. IntakeSession
deactivate SessionRepo
alt 7. AI triage service phản hồi bình thường
  Service -> AiClient : 7. startIntake(canonicalRequest)
  activate AiClient
  AiClient --> Service : 8. envelope JSON\n{status=ASK_MORE|TRIAGE_COMPLETE, questions[] | triageResult}
  deactivate AiClient
else 7. AI service lỗi/timeout (network/5xx) → fallback nội bộ
  Service -> Service : 7a. fallbackConversation()\nsinh câu hỏi/risk bảo thủ cục bộ (không gọi AI ngoài)
end
Service -> SessionRepo : 9. save(session{rawAiResponse=envelope,\nstatus=NEED_MORE_INFO|COMPLETED})
activate SessionRepo
SessionRepo -> DB : 10. UPDATE intake_sessions\nSET raw_ai_response=?, status=?, risk_level=?
activate DB
DB --> SessionRepo : 11. updated
deactivate DB
SessionRepo --> Service : 12. IntakeSession
deactivate SessionRepo
opt 13. status vừa chuyển COMPLETED && riskLevel != null
  Service -> Service : 13a. publishEvent(IntakeSessionCompleted) [async, subscribers khác module nếu có]
end
Service --> Controller : 14. IntakeConversationResponse{questions[] | riskLevel}
deactivate Service
Controller --> M : 15. HTTP 200 OK
deactivate Controller

loop 16-30. mỗi lượt hội thoại tiếp theo cho tới khi đủ ngữ cảnh (status=COMPLETED)
  M -> Controller : 16. POST /api/v1/triage/intake/conversation/continue\n{intakeSessionId, newAnswers}
  activate Controller
  Controller -> Service : 17. continueConversation(request, userId)
  activate Service
  Service -> SessionRepo : 18. findForUpdateByIdAndUserId(sessionId, userId)
  activate SessionRepo
  SessionRepo -> DB : 19. SELECT ... FOR UPDATE FROM intake_sessions\nWHERE id=? AND user_id=?
  activate DB
  DB --> SessionRepo : 20. session row
  deactivate DB
  SessionRepo --> Service : 21. IntakeSession
  deactivate SessionRepo
  alt 22. AI triage service phản hồi bình thường
    Service -> AiClient : 22. continueIntake(canonical)
    activate AiClient
    AiClient --> Service : 23. envelope JSON\n{status=TRIAGE_COMPLETE, triageResult{riskLevel, disclaimer, redFlags[]}}
    deactivate AiClient
  else 22. AI service lỗi/timeout → fallback nội bộ
    Service -> Service : 22a. fallbackConversation()\nsinh câu hỏi/risk bảo thủ cục bộ
  end
  Service -> SessionRepo : 24. save(session{rawAiResponse=envelope, riskLevel, status})
  activate SessionRepo
  SessionRepo -> DB : 25. UPDATE intake_sessions\nSET raw_ai_response=?, risk_level=?, status=?
  activate DB
  DB --> SessionRepo : 26. updated
  deactivate DB
  SessionRepo --> Service : 27. IntakeSession
  deactivate SessionRepo
  opt 28. status vừa chuyển COMPLETED && riskLevel != null
    Service -> Service : 28a. publishEvent(IntakeSessionCompleted) [async]
  end
  Service --> Controller : 29. IntakeConversationResponse{riskLevel nếu COMPLETED | questions[] tiếp theo}
  deactivate Service
  Controller --> M : 30. HTTP 200 OK
  deactivate Controller
end

== UC-73 View Risk Triage Result ==
M -> Controller : 31. GET /api/v1/triage/intake/{sessionId}
activate Controller
Controller -> Service : 32. getResult(sessionId, userId)
activate Service
Service -> SessionRepo : 33. findByIdAndUserId(sessionId, userId)
activate SessionRepo
SessionRepo -> DB : 34. SELECT * FROM intake_sessions\nWHERE id=? AND user_id=?
activate DB
DB --> SessionRepo : 35. session row
deactivate DB
SessionRepo --> Service : 36. IntakeSession
deactivate SessionRepo
loop 37-39. với mỗi citation đọc được từ rawAiResponse
  Service -> Evidence : 37. isApprovedDeepLink(citationUrl)
  activate Evidence
  Evidence --> Service : 38. boolean approved
  deactivate Evidence
  Service -> Service : 39. loại citation nếu domain\nkhông thuộc nguồn đã duyệt (spec 02)
end
Service --> Controller : 40. TriageResultResponse{riskLevel, disclaimer,\ncitations[], recommendedAction, redFlags[]}
deactivate Service
Controller --> M : 41. HTTP 200 OK {riskLevel, guidance, disclaimer}
deactivate Controller

== UC-74 Open Emergency Support from a Red Risk Result ==
M -> HandoffController : 42. POST /api/v1/map/emergency/handoff\n{triageHandoffId=sessionId, riskLevel=RED, userLatitude, userLongitude}
activate HandoffController
HandoffController -> HandoffService : 43. createHandoff(userId, request)
activate HandoffService
HandoffService -> HandoffService : 44. map request → EmergencyMapHandoff{status=OPEN}\n(handoffMapper.toEntity)
alt 45. riskLevel == RED (auto-accept khẩn cấp)
  HandoffService -> HandoffService : 46. set status=ACCEPTED
else 45. riskLevel != RED (hiếm gặp — UI chỉ hiện lối tắt khi RED)
  HandoffService -> HandoffService : 45a. giữ nguyên status=OPEN
end
HandoffService -> HandoffRepo : 47. save(handoff)
activate HandoffRepo
HandoffRepo -> DB : 48. INSERT INTO emergency_map_handoffs ...
activate DB
DB --> HandoffRepo : 49. saved
deactivate DB
HandoffRepo --> HandoffService : 50. EmergencyMapHandoff
deactivate HandoffRepo
HandoffService --> HandoffController : 51. EmergencyHandoffResponse{status}
deactivate HandoffService
HandoffController --> M : 52. HTTP 201 Created\n→ điều hướng sang Emergency Map (MF-07)
deactivate HandoffController

@enduml
```

**Hình 2 — Sequence Diagram: Start Intake → Conversation → View Result → Emergency Handoff (Main Flow)**

> **Ghi chú grounding:** Class Diagram ở mục 2 mô tả `IntakeServiceImpl` với các phụ thuộc
> khái niệm (`RedFlagRuleService`, `EvidenceRetrievalService`, `AuditService`) theo tinh
> thần SRS. Trong code thực tế, service duy nhất xử lý luồng này là `TriageService`
> (implements `ITriageService`), phụ thuộc `IIntakeSessionRepository`, `ChildTriageAiClient`
> (bean thật `HttpChildTriageAiClient`, gọi AI service ngoài qua HTTP) và
> `EvidenceSourceService` (chỉ dùng để validate `citation` domain đã duyệt khi xem kết quả,
> **không** chủ động "retrieve approved knowledge" trong lúc hội thoại như class diagram mô
> tả). `RedFlagRuleService`/`RedFlagRuleServiceImpl` là service quản trị rule cờ đỏ
> (CRUD, xem spec 02) — không được `TriageService` gọi trực tiếp trong luồng intake chính.
> **Không có lệnh gọi `AuditService` nào** trong `TriageService` hay
> `EmergencyMapHandoffServiceImpl` — luồng UC-72/73/74 hiện không phát sinh audit log ở
> tầng service (khác với giả định trong class diagram).

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
