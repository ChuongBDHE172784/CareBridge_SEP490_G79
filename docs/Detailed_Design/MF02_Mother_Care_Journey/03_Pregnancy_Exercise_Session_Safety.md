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
participant "PostureAnalyzer" as Posture
database "PostgreSQL" as DB

== UC-28 Browse Pregnancy Exercise Library ==
M -> ExController : GET /api/v1/exercises?trimester=T2&difficulty=EASY
ExController -> DB : SELECT * FROM pregnancy_exercises\nWHERE status='PUBLISHED' AND ...
DB --> ExController : exercises[]
ExController --> M : HTTP 200 OK {exercises[]}

== UC-29 Complete Pre-exercise Safety Check ==
M -> ExController : POST /api/v1/exercises/{exerciseId}/safety-check\n{answers: {...}}
ExController -> Service : submitSafetyCheck(userId, exerciseId, answers)
Service -> Service : evaluate answers → redFlagDetected?
alt no red flag
  Service -> DB : INSERT INTO exercise_safety_checks\n(resultStatus=CLEARED)
  Service --> ExController : SafetyCheck{status=CLEARED}
  ExController --> M : HTTP 200 OK {status=CLEARED}
else red flag detected
  Service -> DB : INSERT INTO exercise_safety_checks\n(resultStatus=BLOCKED, blockedReason)
  Service --> ExController : SafetyCheck{status=BLOCKED}
  ExController --> M : HTTP 200 OK {status=BLOCKED, safetyWarning}
end

== UC-30 Conduct Session with Optional Posture Feedback ==
M -> ExController : POST /api/v1/exercises/{exerciseId}/sessions\n{safetyCheckId}
ExController -> Service : start(userId, exerciseId, safetyCheckId)
Service -> Service : require safetyCheck.resultStatus == CLEARED
Service -> DB : INSERT INTO exercise_sessions (sessionStatus=IN_PROGRESS)
Service --> ExController : ExerciseSession
ExController --> M : HTTP 201 Created

loop mỗi khung hình (nếu bật camera consent)
  M -> SessController : POST /api/v1/exercises/sessions/{sessionId}/posture-events
  SessController -> Posture : analyze(frameFeatures, postureConfig)
  Posture --> SessController : PostureFeedbackEvent{severity, feedbackText}
  SessController -> DB : INSERT INTO posture_feedback_events ...
  SessController --> M : HTTP 200 OK {feedback}
end

M -> SessController : PATCH /api/v1/exercises/sessions/{sessionId}/pause
SessController -> Service : pause(sessionId)
Service -> DB : UPDATE exercise_sessions SET sessionStatus='PAUSED'

M -> SessController : PATCH /api/v1/exercises/sessions/{sessionId}/resume
SessController -> Service : resume(sessionId)
Service -> DB : UPDATE exercise_sessions SET sessionStatus='IN_PROGRESS'

M -> SessController : PATCH /api/v1/exercises/sessions/{sessionId}/complete
SessController -> Service : complete(sessionId)
Service -> Service : tính completionPercent, postureScore, summaryJson
Service -> DB : UPDATE exercise_sessions\nSET sessionStatus='COMPLETED', endedAt=now()
Service --> SessController : ExerciseSession{sessionStatus=COMPLETED}
SessController --> M : HTTP 200 OK

== UC-31 View Exercise History and Session Result ==
M -> SessController : GET /api/v1/exercises/sessions/history
SessController -> DB : SELECT * FROM exercise_sessions\nWHERE user_id=? ORDER BY started_at DESC
DB --> SessController : sessions[]
SessController --> M : HTTP 200 OK {sessions[]}

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
