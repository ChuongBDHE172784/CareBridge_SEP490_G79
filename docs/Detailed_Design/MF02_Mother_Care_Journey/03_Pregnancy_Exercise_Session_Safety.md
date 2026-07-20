# MF-02 / Spec 03 — Pregnancy Exercise Session with Safety Check & Posture Feedback

| Field | Value |
| --- | --- |
| Feature | MF-02 — Mother Care Journey |
| Use Cases Covered | UC-28 Browse Pregnancy Exercise Library, UC-29 Complete Pre-exercise Safety Check, UC-30 Conduct Pregnancy Exercise Session with Optional Posture Feedback, UC-31 View Exercise History and Session Result |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother browses the reviewed exercise library, must clear a pre-exercise safety check before a session can start, runs the session with start/pause/resume/complete controls and optional camera-based posture feedback, then reviews the completed session result and history. |
| Grounding (source code) | `exercise/entity/PregnancyExercise.java`, `ExerciseStatus.java`, `TrimesterScope.java`, `DifficultyLevel.java`, `exercise/entity/ExerciseSafetyCheck.java`, `SafetyCheckStatus.java`, `exercise/entity/ExerciseSession.java`, `SessionStatus.java`, `exercise/entity/PostureFeedbackEvent.java`, `exercise/controller/ExerciseController.java` (`/api/v1/exercises`), `exercise/controller/ExerciseSessionController.java` (`/api/v1/exercises/sessions`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là luồng an toàn nhất trong MF-02: Mother không thể vào một `ExerciseSession` nếu
chưa có một `ExerciseSafetyCheck` với kết quả `CLEARED` (BR-SAFETY, UC-29) — nếu câu trả
lời an toàn chứa cờ đỏ (`redFlagDetected=true`), hệ thống chặn (`BLOCKED`) và không cho
tạo session. Khi session đang chạy, Mother có thể `pause`/`resume`/`complete`; nếu bật
camera feedback (yêu cầu consent riêng), mỗi khung hình sinh ra `PostureFeedbackEvent`
tuỳ theo `PostureAnalysisConfig` (rule-based hoặc ML) đang active cho bài tập đó. Kết
quả cuối (điểm tư thế tổng hợp, thời lượng, cảnh báo) hiển thị ở UC-31. Thư viện bài tập
(UC-28) chỉ hiển thị exercise ở trạng thái `PUBLISHED` — quản trị nội dung bài tập
(draft/archive) không thuộc luồng chính của Mother nên không vẽ lại ở đây.

## 2. Class Diagram

```plantuml
@startuml MF02_03_ExerciseSession_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class PregnancyExercise {
  + exerciseId: UUID
  + title: String
  + trimesterScope: TrimesterScope
  + difficultyLevel: DifficultyLevel
  + durationMinutes: Short
  + safetyWarning: String
  + supportsPostureAnalysis: Boolean
  + status: ExerciseStatus
}

enum ExerciseStatus {
  DRAFT
  PUBLISHED
  ARCHIVED
}

class ExerciseSafetyCheck {
  + safetyCheckId: UUID
  + exerciseId: UUID
  + journeyId: UUID
  + userId: UUID
  + answerJson: Map<String, Boolean>
  + redFlagDetected: Boolean
  + resultStatus: SafetyCheckStatus
  + blockedReason: String
  + completedAt: OffsetDateTime
}

enum SafetyCheckStatus {
  PENDING
  CLEARED
  BLOCKED
}

class ExerciseSession {
  + exerciseSessionId: UUID
  + exerciseId: UUID
  + journeyId: UUID
  + userId: UUID
  + safetyCheckId: UUID
  + startedAt: OffsetDateTime
  + endedAt: OffsetDateTime
  + pausedSeconds: Integer
  + completionPercent: BigDecimal
  + postureScore: BigDecimal
  + sessionStatus: SessionStatus
  + warningCount: Integer
  + summaryJson: String
}

enum SessionStatus {
  IN_PROGRESS
  PAUSED
  COMPLETED
  ABANDONED
}

class PostureFeedbackEvent {
  + feedbackEventId: UUID
  + exerciseSessionId: UUID
  + postureConfigId: UUID
  + eventTimeMs: Long
  + postureCode: String
  + confidenceScore: BigDecimal
  + severity: String
  + feedbackText: String
}

class PostureAnalysisConfig {
  + postureConfigId: UUID
  + exerciseId: UUID
  + analysisMode: String
  + confidenceThreshold: BigDecimal
  + status: String
}

class ExerciseController {
  - exerciseService: ExerciseService
  + list(filter): ResponseEntity
  + submitSafetyCheck(exerciseId, answers): ResponseEntity
  + startSession(exerciseId, request): ResponseEntity
}

class ExerciseSessionController {
  - exerciseSessionService: ExerciseSessionService
  + pause(sessionId): ResponseEntity
  + resume(sessionId): ResponseEntity
  + recordPostureEvent(sessionId, event): ResponseEntity
  + complete(sessionId): ResponseEntity
  + result(sessionId): ResponseEntity
  + history(): ResponseEntity
}

interface ExerciseSessionService <<interface>> {
  + start(userId, exerciseId, safetyCheckId): ExerciseSession
  + pause(sessionId): void
  + resume(sessionId): void
  + complete(sessionId): ExerciseSession
}

class ExerciseSessionServiceImpl implements ExerciseSessionService {
  - exerciseSessionRepository: ExerciseSessionRepository
  - safetyCheckRepository: ExerciseSafetyCheckRepository
  - postureAnalyzer: PostureAnalyzer
  - auditService: AuditService
}

PregnancyExercise --> ExerciseStatus
PregnancyExercise "1" *-- "0..*" ExerciseSafetyCheck : gates
ExerciseSafetyCheck --> SafetyCheckStatus
ExerciseSafetyCheck "1" -- "0..1" ExerciseSession : clears
ExerciseSession --> SessionStatus
ExerciseSession "1" *-- "0..*" PostureFeedbackEvent : optional
PregnancyExercise "1" -- "0..1" PostureAnalysisConfig : active config
ExerciseController --> ExerciseSessionService : uses
ExerciseSessionController --> ExerciseSessionService : uses
ExerciseSessionServiceImpl --> ExerciseSafetyCheckRepository : validates

@enduml
```

**Hình 1 — Class Diagram: Pregnancy Exercise, Safety Check & Session with Posture Feedback**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF02_03_ExerciseSession_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "ExerciseController" as ExController
participant "ExerciseSessionController" as SessController
participant "ExerciseSessionServiceImpl" as Service
participant "ExerciseRepository" as ExRepo
participant "ExerciseSafetyCheckRepository" as SafetyRepo
participant "ExerciseSessionRepository" as SessRepo
participant "PostureFeedbackEventRepository" as PostureRepo
participant "PostureAnalyzer" as Posture
database "PostgreSQL" as DB

== UC-28 Browse Pregnancy Exercise Library ==
M -> ExController : 1. GET /api/v1/exercises?trimester=T2&difficulty=EASY
activate ExController
ExController -> Service : 2. list(filter)
activate Service
Service -> ExRepo : 3. findPublished(filter)
activate ExRepo
ExRepo -> DB : 4. SELECT * FROM pregnancy_exercises\nWHERE status='PUBLISHED' AND ...
activate DB
DB --> ExRepo : 5. rows[]
deactivate DB
ExRepo --> Service : 6. exercises[]
deactivate ExRepo
Service --> ExController : 7. exercises[]
deactivate Service
ExController --> M : 8. HTTP 200 OK {exercises[]}
deactivate ExController

== UC-29 Complete Pre-exercise Safety Check ==
M -> ExController : 9. POST /api/v1/exercises/{exerciseId}/safety-check\n{answers: {...}}
activate ExController
ExController -> Service : 10. submitSafetyCheck(userId, exerciseId, answers)
activate Service
Service -> Service : 11. evaluate answers → redFlagDetected?
alt 11. no red flag [happy path → CLEARED]
  Service -> SafetyRepo : 12. save(SafetyCheck{resultStatus=CLEARED})
  activate SafetyRepo
  SafetyRepo -> DB : 13. INSERT INTO exercise_safety_checks\n(resultStatus=CLEARED)
  activate DB
  DB --> SafetyRepo : 14. saved
  deactivate DB
  SafetyRepo --> Service : 15. SafetyCheck{status=CLEARED}
  deactivate SafetyRepo
  Service --> ExController : 16. SafetyCheck{status=CLEARED}
  deactivate Service
  ExController --> M : 17. HTTP 200 OK {status=CLEARED}
  deactivate ExController
else 11. red flag detected [chặn tạo session → BLOCKED]
  Service -> SafetyRepo : 11a. save(SafetyCheck{resultStatus=BLOCKED, blockedReason})
  activate SafetyRepo
  SafetyRepo -> DB : 11b. INSERT INTO exercise_safety_checks\n(resultStatus=BLOCKED, blockedReason)
  activate DB
  DB --> SafetyRepo : 11c. saved
  deactivate DB
  SafetyRepo --> Service : 11d. SafetyCheck{status=BLOCKED}
  deactivate SafetyRepo
  Service --> ExController : 11e. SafetyCheck{status=BLOCKED}
  deactivate Service
  ExController --> M : 11f. HTTP 200 OK {status=BLOCKED, safetyWarning}
  deactivate ExController
end

== UC-30 Conduct Session with Optional Posture Feedback ==
M -> ExController : 18. POST /api/v1/exercises/{exerciseId}/sessions\n{safetyCheckId}
activate ExController
ExController -> Service : 19. start(userId, exerciseId, safetyCheckId)
activate Service
Service -> Service : 20. require safetyCheck.resultStatus == CLEARED
Service -> SessRepo : 21. save(ExerciseSession{sessionStatus=IN_PROGRESS})
activate SessRepo
SessRepo -> DB : 22. INSERT INTO exercise_sessions (sessionStatus=IN_PROGRESS)
activate DB
DB --> SessRepo : 23. saved
deactivate DB
SessRepo --> Service : 24. ExerciseSession
deactivate SessRepo
Service --> ExController : 25. ExerciseSession
deactivate Service
ExController --> M : 26. HTTP 201 Created
deactivate ExController

loop 27-34. mỗi khung hình gửi lên (nếu Mother bật camera consent)
  M -> SessController : 27. POST /api/v1/exercises/sessions/{sessionId}/posture-events
  activate SessController
  SessController -> Posture : 28. analyze(frameFeatures, postureConfig)
  activate Posture
  Posture --> SessController : 29. PostureFeedbackEvent{severity, feedbackText}
  deactivate Posture
  SessController -> PostureRepo : 30. save(feedbackEvent)
  activate PostureRepo
  PostureRepo -> DB : 31. INSERT INTO posture_feedback_events ...
  activate DB
  DB --> PostureRepo : 32. saved
  deactivate DB
  PostureRepo --> SessController : 33. PostureFeedbackEvent
  deactivate PostureRepo
  SessController --> M : 34. HTTP 200 OK {feedback}
  deactivate SessController
end

M -> SessController : 35. PATCH /api/v1/exercises/sessions/{sessionId}/pause
activate SessController
SessController -> Service : 36. pause(sessionId)
activate Service
Service -> SessRepo : 37. update(session{sessionStatus=PAUSED})
activate SessRepo
SessRepo -> DB : 38. UPDATE exercise_sessions SET session_status='PAUSED'
activate DB
DB --> SessRepo : 39. updated
deactivate DB
SessRepo --> Service : 40. void
deactivate SessRepo
Service --> SessController : 41. void
deactivate Service
SessController --> M : 42. HTTP 200 OK
deactivate SessController

M -> SessController : 43. PATCH /api/v1/exercises/sessions/{sessionId}/resume
activate SessController
SessController -> Service : 44. resume(sessionId)
activate Service
Service -> SessRepo : 45. update(session{sessionStatus=IN_PROGRESS})
activate SessRepo
SessRepo -> DB : 46. UPDATE exercise_sessions SET session_status='IN_PROGRESS'
activate DB
DB --> SessRepo : 47. updated
deactivate DB
SessRepo --> Service : 48. void
deactivate SessRepo
Service --> SessController : 49. void
deactivate Service
SessController --> M : 50. HTTP 200 OK
deactivate SessController

M -> SessController : 51. PATCH /api/v1/exercises/sessions/{sessionId}/complete
activate SessController
SessController -> Service : 52. complete(sessionId)
activate Service
Service -> Service : 53. tính completionPercent, postureScore, summaryJson
Service -> SessRepo : 54. update(session{sessionStatus=COMPLETED, endedAt=now()})
activate SessRepo
SessRepo -> DB : 55. UPDATE exercise_sessions\nSET session_status='COMPLETED', ended_at=now()
activate DB
DB --> SessRepo : 56. updated
deactivate DB
SessRepo --> Service : 57. ExerciseSession{sessionStatus=COMPLETED}
deactivate SessRepo
Service --> SessController : 58. ExerciseSession{sessionStatus=COMPLETED}
deactivate Service
SessController --> M : 59. HTTP 200 OK
deactivate SessController

== UC-31 View Exercise History and Session Result ==
M -> SessController : 60. GET /api/v1/exercises/sessions/history
activate SessController
SessController -> SessRepo : 61. findByUserId(userId)
activate SessRepo
SessRepo -> DB : 62. SELECT * FROM exercise_sessions\nWHERE user_id=? ORDER BY started_at DESC
activate DB
DB --> SessRepo : 63. rows[]
deactivate DB
SessRepo --> SessController : 64. sessions[]
deactivate SessRepo
SessController --> M : 65. HTTP 200 OK {sessions[]}
deactivate SessController

@enduml
```

**Hình 2 — Sequence Diagram: Browse → Safety Check → Session (Pause/Resume/Posture) → Complete → History (Main Flow)**

## 4. State Machine — `ExerciseSession.sessionStatus` (gated by `ExerciseSafetyCheck.resultStatus`)

```plantuml
@startuml MF02_03_ExerciseSession_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

state "ExerciseSafetyCheck" as Safety {
  [*] --> PENDING : Mother mở form safety check (UC-29)
  PENDING --> CLEARED : Không phát hiện cờ đỏ
  PENDING --> BLOCKED : Phát hiện cờ đỏ\n[chặn tạo session]
  CLEARED --> [*]
  BLOCKED --> [*]
}

state "ExerciseSession" as Session {
  [*] --> IN_PROGRESS : start() chỉ khi safetyCheck = CLEARED (UC-30)
  IN_PROGRESS --> PAUSED : pause()
  PAUSED --> IN_PROGRESS : resume()
  IN_PROGRESS --> COMPLETED : complete()
  PAUSED --> ABANDONED : Mother thoát không hoàn tất\n[timeout / rời app]
  COMPLETED --> [*]
  ABANDONED --> [*]
}

Safety --> Session : CLEARED cho phép start()

@enduml
```

**Hình 3 — State Machine: Safety Check Gate → Exercise Session Lifecycle**

## 5. Business Rules Applied

- BR-SAFETY — session chỉ được bắt đầu khi có `ExerciseSafetyCheck` với `resultStatus=CLEARED`; câu trả lời cảnh báo cấu hình sẵn sẽ chặn (`BLOCKED`) việc vào bài tập.
- UC-28 — chỉ hiển thị bài tập đã được duyệt (`status=PUBLISHED`) với ghi chú an toàn theo từng bài (`safetyWarning`).
- UC-30 — camera/posture feedback là tính năng **tuỳ chọn**, yêu cầu consent riêng biệt trước khi bật (không phải luồng bắt buộc).
- UC-31 — lịch sử chỉ hiển thị session của chính Mother, kèm cảnh báo an toàn tổng hợp nếu có.
