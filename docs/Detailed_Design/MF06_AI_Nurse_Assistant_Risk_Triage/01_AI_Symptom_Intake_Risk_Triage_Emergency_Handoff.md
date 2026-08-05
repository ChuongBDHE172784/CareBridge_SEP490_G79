# MF-06 / Spec 01 — AI Symptom Intake, Risk Triage & Emergency Handoff

| Field | Value |
| --- | --- |
| Feature | MF-06 — AI Nurse Assistant & Risk Triage |
| Use Cases Covered | UC-72 Run AI Symptom Intake, UC-73 View Risk Triage Result, UC-74 Open Emergency Support from a Red Risk Result |
| Primary Actor(s) | Mother, Family Caregiver |
| Platform | Mobile App |
| Main Flow Summary | An authorized caregiver describes maternal or child symptoms through guided intake; the system returns a non-diagnostic GREEN/YELLOW/RED orientation using approved knowledge and red-flag rules, and for RED offers a user-initiated handoff into the Emergency Map flow (MF-07), never treating the output as a diagnosis. |
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

interface ITriageService <<interface>> {
  + start(userId: UUID, request): IntakeSession
  + continueConversation(sessionId: UUID, request): IntakeSession
}

class TriageService implements ITriageService {
  - intakeSessionRepository: IntakeSessionRepository
  - childTriageAiClient: ChildTriageAiClient
  - evidenceSourceService: EvidenceSourceService
  - triageConsentService: ITriageConsentService
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
IntakeController --> ITriageService : uses
TriageService --> ChildTriageAiClient : calls Python triage service
TriageService --> EvidenceSourceService : validates citation domains
TriageService --> ITriageConsentService : enforces disclaimer consent
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

actor "Mother / Family Caregiver" as M
participant "CareBridge AI Nurse UI" as UI
participant "IntakeController" as Controller
participant "EmergencyMapHandoffController" as HandoffController
participant "TriageService" as Service
participant "EvidenceSourceService" as Evidence
participant "EmergencyMapHandoffServiceImpl" as HandoffService
participant "IIntakeSessionRepository" as SessionRepo
participant "EmergencyMapHandoffRepository" as HandoffRepo
database "PostgreSQL" as DB
participant "HttpChildTriageAiClient" as AiClient

== UC-72 Run AI Symptom Intake (Start Conversation) ==
M -> UI : 1. Start symptom intake conversation
activate UI
UI -> Controller : 1a. POST /api/v1/triage/intake/conversation/start\n{stage=PREGNANCY, initialText, currentIntake}
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
alt [AI triage service responds normally]
  Service -> AiClient : 7. startIntake(canonicalRequest)
  activate AiClient
  AiClient --> Service : 8. envelope JSON\n{status=ASK_MORE|TRIAGE_COMPLETE, questions[] | triageResult}
  deactivate AiClient
else 7. AI service error/timeout (network/5xx) → internal fallback
  Service -> Service : 7a. fallbackConversation()\ngenerate questions/conservative risk locally (no external AI call)
  activate Service
  Service --> Service : 7b. fallback envelope
  deactivate Service
end
Service -> SessionRepo : 9. save(session{rawAiResponse=envelope,\nstatus=NEED_MORE_INFO|COMPLETED})
activate SessionRepo
SessionRepo -> DB : 10. UPDATE intake_sessions\nSET raw_ai_response=?, status=?, risk_level=?
activate DB
DB --> SessionRepo : 11. updated
deactivate DB
SessionRepo --> Service : 12. IntakeSession
deactivate SessionRepo
opt [status just transitioned to COMPLETED and riskLevel is present]
  Service ->> Service : 13a. publish IntakeSessionCompleted [async]
end
Service --> Controller : 14. IntakeConversationResponse{questions[] | riskLevel}
deactivate Service
Controller --> UI : 15. HTTP 200 OK
deactivate Controller
UI --> M : 15a. Display questions or triage result
deactivate UI

loop [each subsequent conversation turn until sufficient context (status=COMPLETED)]
  M -> UI : 16. Submit symptom follow-up answers
  activate UI
  UI -> Controller : 16a. POST /api/v1/triage/intake/conversation/continue\n{intakeSessionId, newAnswers}
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
  alt [AI triage service responds normally]
    Service -> AiClient : 22. continueIntake(canonical)
    activate AiClient
    AiClient --> Service : 23. envelope JSON\n{status=TRIAGE_COMPLETE, triageResult{riskLevel, disclaimer, redFlags[]}}
    deactivate AiClient
  else 22. AI service error/timeout → internal fallback
    Service -> Service : 22a. fallbackConversation()\ngenerate questions/conservative risk locally
    activate Service
    Service --> Service : 22b. fallback envelope
    deactivate Service
  end
  Service -> SessionRepo : 24. save(session{rawAiResponse=envelope, riskLevel, status})
  activate SessionRepo
  SessionRepo -> DB : 25. UPDATE intake_sessions\nSET raw_ai_response=?, risk_level=?, status=?
  activate DB
  DB --> SessionRepo : 26. updated
  deactivate DB
  SessionRepo --> Service : 27. IntakeSession
  deactivate SessionRepo
  opt [status just transitioned to COMPLETED and riskLevel is present]
    Service ->> Service : 28a. publish IntakeSessionCompleted [async]
  end
  Service --> Controller : 29. IntakeConversationResponse{riskLevel if COMPLETED | next questions[]}
  deactivate Service
  Controller --> UI : 30. HTTP 200 OK
  deactivate Controller
  UI --> M : 30a. Display next questions or completed triage
  deactivate UI
end

== UC-73 View Risk Triage Result ==
M -> UI : 31. View triage result
activate UI
UI -> Controller : 31a. GET /api/v1/triage/intake/{sessionId}
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
loop [for each citation read from rawAiResponse]
  Service -> Evidence : 37. isApprovedDeepLink(citationUrl)
  activate Evidence
  Evidence --> Service : 38. boolean approved
  deactivate Evidence
  Service -> Service : 32a. exclude citation if domain\nis not in the approved sources (spec 02)
  activate Service
  Service --> Service : 32b. filtered citations
  deactivate Service
end
Service --> Controller : 39. TriageResultResponse{riskLevel, disclaimer,\ncitations[], recommendedAction, redFlags[]}
deactivate Service
Controller --> UI : 40. HTTP 200 OK {riskLevel, guidance, disclaimer}
deactivate Controller
UI --> M : 40a. Display risk level, guidance, and disclaimer
deactivate UI

== UC-74 Open Emergency Support from a Red Risk Result ==
M -> UI : 41. Request emergency-map handoff
activate UI
UI -> HandoffController : 41a. POST /api/v1/map/emergency/handoff\n{triageHandoffId=sessionId, riskLevel=RED, userLatitude, userLongitude}
activate HandoffController
HandoffController -> HandoffService : 42. createHandoff(userId, request)
activate HandoffService
HandoffService -> HandoffService : 42a. map request → EmergencyMapHandoff{status=OPEN}\n(handoffMapper.toEntity)
activate HandoffService
HandoffService --> HandoffService : 42b. mapped handoff
deactivate HandoffService
alt [riskLevel == RED]
  HandoffService -> HandoffService : 42c. set status=ACCEPTED
  activate HandoffService
  HandoffService --> HandoffService : 42d. accepted handoff
  deactivate HandoffService
else [riskLevel != RED]
  HandoffService -> HandoffService : 42e. keep status=OPEN
  activate HandoffService
  HandoffService --> HandoffService : 42f. open handoff
  deactivate HandoffService
end
HandoffService -> HandoffRepo : 43. save(handoff)
activate HandoffRepo
HandoffRepo -> DB : 44. INSERT INTO emergency_map_handoffs ...
activate DB
DB --> HandoffRepo : 45. saved
deactivate DB
HandoffRepo --> HandoffService : 46. EmergencyMapHandoff
deactivate HandoffRepo
HandoffService --> HandoffController : 47. EmergencyHandoffResponse{status}
deactivate HandoffService
HandoffController --> UI : 48. HTTP 201 Created
deactivate HandoffController
UI --> M : 48a. Navigate to Emergency Map (MF-07)
deactivate UI

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


## 4. Business Rules Applied

- CC-01 / Excluded (SRS mục 4.8) — AI không chẩn đoán, không kê đơn, không tư vấn liều lượng; luôn kèm `disclaimer`.
- NS-05 — chuẩn hoá input, chỉ truy xuất tri thức đã duyệt (spec 02), áp fallback bảo thủ khi không chắc chắn, ghi log phiên bản rule/nguồn dùng.
- UC-74 — handoff sang Emergency Map là hành động **do người dùng chủ động chọn**, hệ thống không tự dispatch hay đảm bảo có chuyên gia/xe cấp cứu đến.
- BR-PRIVACY — nội dung intake (`symptoms`, `rawAiResponse`) là dữ liệu sức khỏe nhạy cảm, chỉ chủ sở hữu truy cập được.
