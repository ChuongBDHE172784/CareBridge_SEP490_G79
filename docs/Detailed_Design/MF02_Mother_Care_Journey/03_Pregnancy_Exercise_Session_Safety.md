# MF-02 / Spec 03 — Pregnancy Exercise Session, Safety Check and Posture Feedback

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-27 Browse Pregnancy Exercises; UC-28 Complete Pre-exercise Safety Check; UC-29 Perform Camera-guided Exercise Session; UC-30 View Exercise History and Results |
| Use Case Group | Mobile App |
| Platform | Mother Mobile App; Backend; Posture AI |
| Primary Actors | Mother |
| In Scope | Optional camera feedback after permission and safety clearance |
| Explicitly Excluded | Medical fitness diagnosis |
| Implementation Trace | UI: Pregnancy exercise screens and real-time camera session; Controller: ExerciseController, ExerciseSessionController; Service: ExerciseSessionServiceImpl; Repository: ExerciseSessionRepository, ExerciseRepository; Entity: PregnancyExercise, ExerciseSession |

## 1. Tổng quan luồng chính (Main Flow Overview)

Optional camera feedback after permission and safety clearance. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF02_03_PregnancyExerciseSessionSafetyCheckandPostureFeedback_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "Pregnancy exercise screens and real-time camera session" as UI1 <<UI>>
class "ExerciseController" as Controller1 <<Controller>> {
  - exerciseQueryService: IExerciseQueryService
  - exerciseDetailQueryService: IExerciseDetailQueryService
  - safetyCheckService: IExerciseSafetyCheckService
  - sessionService: IExerciseSessionService
  + getLatestSafetyCheck(exerciseId: UUID, principal: Principal): ResponseEntity<ApiResponse<SafetyCheckResponse>>
  + submitSafetyCheck(exerciseId: UUID, request: SubmitSafetyCheckRequest, principal: Principal): ResponseEntity<ApiResponse<SafetyCheckResponse>>
  + getExerciseDetail(exerciseId: UUID): ResponseEntity<ApiResponse<ExerciseDetailResponse>>
  + getPostureConfig(exerciseId: UUID): ResponseEntity<ApiResponse<PostureConfigResponse>>
  + startSession(exerciseId: UUID, request: StartSessionRequest, principal: Principal): ResponseEntity<ApiResponse<StartSessionResponse>>
}
class "ExerciseSessionController" as Controller2 <<Controller>> {
  - sessionService: IExerciseSessionService
  - sessionResultService: IExerciseSessionResultService
  - sessionHistoryService: IExerciseSessionHistoryService
  - postureAnalysisService: IPostureAnalysisService
  + completeSession(sessionId: UUID, principal: Principal): ResponseEntity<ApiResponse<SessionResultResponse>>
  + analyzePosture(sessionId: UUID, request: PostureEventRequest, principal: Principal): ResponseEntity<ApiResponse<PostureFeedbackResponse>>
  + getSessionResult(sessionId: UUID, principal: Principal): ResponseEntity<ApiResponse<SessionResultResponse>>
  + pauseSession(sessionId: UUID, principal: Principal): ResponseEntity<ApiResponse<SessionStateResponse>>
  + resumeSession(sessionId: UUID, principal: Principal): ResponseEntity<ApiResponse<SessionStateResponse>>
}
class "ExerciseSessionServiceImpl" as Service1 <<Service>> {
  - sessionRepository: ExerciseSessionRepository
  - exerciseRepository: ExerciseRepository
  - safetyCheckRepository: ExerciseSafetyCheckRepository
  - postureFeedbackEventRepository: PostureFeedbackEventRepository
  + completeSession(sessionId: UUID, userId: UUID): SessionResultResponse
  + pauseSession(sessionId: UUID, userId: UUID): SessionStateResponse
  + resumeSession(sessionId: UUID, userId: UUID): SessionStateResponse
  + startSession(exerciseId: UUID, request: StartSessionRequest, userId: UUID): StartSessionResponse
  - computePostureScore(sessionId: UUID): BigDecimal
}
interface "IExerciseSessionService" as Service1Contract <<Service>>
interface "ExerciseSessionRepository" as Repository1 {
  + findFirstByExerciseIdAndUserIdAndSessionStatusInAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAscExerciseSessionIdAsc(exerciseId: UUID, userId: UUID, statuses: List<SessionStatus>, ...): Optional<ExerciseSession>
  + findByUserIdAndSessionStatusOrderByStartedAtDesc(userId: UUID, status: SessionStatus, pageable: Pageable): List<ExerciseSession>
}
interface "ExerciseRepository" as Repository2 {
  + findByExerciseIdAndStatus(exerciseId: UUID, status: ExerciseStatus): Optional<PregnancyExercise>
}
class "PregnancyExercise" as Entity1 <<Entity>> {
  - exerciseId: UUID
  - createdBy: UUID
  - title: String
  - description: String
  - trimesterScope: TrimesterScope
  - difficultyLevel: DifficultyLevel
  - durationMinutes: Short
}
class "ExerciseSession" as Entity2 <<Entity>> {
  - exerciseSessionId: UUID
  - exerciseId: UUID
  - journeyId: UUID
  - userId: UUID
  - safetyCheckId: UUID
  - startedAt: OffsetDateTime
  - endedAt: OffsetDateTime
}
interface "JpaRepository<ExerciseSession, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<PregnancyExercise, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "MediaPipe posture sidecar" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller1 : invokes API
UI1 ..> Controller2 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Repository1 ..> Entity2 : maps
Repository1 ..> DB : persists
Repository2 ..> Entity1 : maps
Repository2 ..> DB : persists
Entity1 "1" -- "0..*" Entity2 : exercise sessions
Service1 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Pregnancy Exercise Session, Safety Check and Posture Feedback**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF02_03_PregnancyExerciseSessionSafetyCheckandPostureFeedback_SequenceDiagram
skinparam shadowing false

actor "Mother" as Actor
boundary ":Pregnancy exercise screens" as UI1
boundary ":real-time camera session" as UI2
control ":ExerciseController" as Controller1
control ":ExerciseSessionController" as Controller2
participant ":ExerciseQueryServiceImpl" as Service1 <<service>>
participant ":ExerciseSafetyCheckServiceImpl" as Service2 <<service>>
participant ":PostureAnalysisServiceImpl" as Service3 <<service>>
participant ":ExerciseSessionHistoryServiceImpl" as Service4 <<service>>
participant ":ExerciseRepository" as Repository1 <<repository>>
participant ":ExerciseSafetyCheckRepository" as Repository2 <<repository>>
participant ":ExerciseSessionRepository" as Repository3 <<repository>>
database "PostgreSQL" as DB
participant ":MediaPipe posture sidecar" as External1 <<external system>>

group UC-27 Browse Pregnancy Exercises
  Actor -> UI1 : 1. startBrowsePregnancyExercises()
  activate UI1
  UI1 -> Controller1 : 2. listExercises(filters)
  activate Controller1
  Controller1 -> Service1 : 3. listPublishedExercises(filters)
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 4a. findPublishedByFilters()
    activate Repository1
    Repository1 -> DB : 4a-1. SELECT
    activate DB
    DB --> Repository1 : 4a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 4a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 4a-4. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4a-5. 200 OK
    deactivate Controller1
    UI1 --> Actor : 4a-6. displayBrowsePregnancyExercisesResult()
    deactivate UI1
  else [request is invalid, forbidden or unavailable]
    Service1 --> Controller1 : 4b. domainError
    deactivate Service1
    Controller1 --> UI1 : 4b-1. 400 / 401 / 403 / 404
    deactivate Controller1
    UI1 --> Actor : 4b-2. displayActionableError()
    deactivate UI1
  end
end

group UC-28 Complete Pre-exercise Safety Check
  Actor -> UI1 : 5. startCompletePreExerciseSafetyCheck()
  activate UI1
  UI1 -> Controller1 : 6. submitSafetyCheck(exerciseId, answers)
  activate Controller1
  Controller1 -> Service2 : 7. submitSafetyCheck(exerciseId, answers)
  activate Service2
  alt [command is valid and actor is authorized]
    Service2 -> Repository2 : 8a. save(safetyCheck)
    activate Repository2
    Repository2 -> DB : 8a-1. INSERT / UPDATE
    activate DB
    DB --> Repository2 : 8a-2. persistedState
    deactivate DB
    Repository2 --> Service2 : 8a-3. savedEntity
    deactivate Repository2
    Service2 --> Controller1 : 8a-4. resultDTO
    deactivate Service2
    Controller1 --> UI1 : 8a-5. 200 OK / 201 Created
    deactivate Controller1
    UI1 --> Actor : 8a-6. displayConfirmedState()
    deactivate UI1
  else [validation, authorization or state check fails]
    Service2 --> Controller1 : 8b. domainError
    deactivate Service2
    Controller1 --> UI1 : 8b-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI1 --> Actor : 8b-2. displayActionableError()
    deactivate UI1
  end
end

group UC-29 Perform Camera-guided Exercise Session
  Actor -> UI2 : 9. startPerformCameraGuidedExerciseSession()
  activate UI2
  UI2 -> Controller2 : 10. analyzePosture()
  activate Controller2
  Controller2 -> Service3 : 11. analyzePosture()
  activate Service3
  alt [selected action is view or list]
    Service3 -> Repository3 : 12a. findByUserIdAndSessionStatusOrderByStartedAtDesc()
    activate Repository3
    Repository3 -> DB : 12a-1. SELECT
    activate DB
    DB --> Repository3 : 12a-2. queryResult
    deactivate DB
    Repository3 --> Service3 : 12a-3. domainRecords
    deactivate Repository3
    Service3 --> Controller2 : 12a-4. resultDTO
    deactivate Service3
    Controller2 --> UI2 : 12a-5. 200 OK
    deactivate Controller2
    UI2 --> Actor : 12a-6. displayCurrentState()
    deactivate UI2
  else [selected action creates, updates, archives or deletes]
    Service3 -> Repository3 : 12b. findByUserIdAndSessionStatusOrderByStartedAtDesc()
    activate Repository3
    Repository3 -> DB : 12b-1. SELECT
    activate DB
    DB --> Repository3 : 12b-2. currentState
    deactivate DB
    Repository3 --> Service3 : 12b-3. scopedEntity
    deactivate Repository3
    Service3 -> Repository3 : 12b-4. save()
    activate Repository3
    Repository3 -> DB : 12b-5. INSERT / UPDATE
    activate DB
    DB --> Repository3 : 12b-6. persistedState
    deactivate DB
    Repository3 --> Service3 : 12b-7. persistedEntity
    deactivate Repository3
    Service3 -> External1 : 12b-8. analyzePosture(frame)
    activate External1
    External1 --> Service3 : 12b-9. integrationResult
    deactivate External1
    Service3 --> Controller2 : 12b-10. resultDTO
    deactivate Service3
    Controller2 --> UI2 : 12b-11. 200 OK / 201 Created
    deactivate Controller2
    UI2 --> Actor : 12b-12. displayConfirmedState()
    deactivate UI2
  else [request is invalid, forbidden, not found or conflicting]
    Service3 --> Controller2 : 12c. domainError
    deactivate Service3
    Controller2 --> UI2 : 12c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller2
    UI2 --> Actor : 12c-2. displayActionableError()
    deactivate UI2
  end
end

group UC-30 View Exercise History and Results
  Actor -> UI1 : 13. startViewExerciseHistoryAndResults()
  activate UI1
  UI1 -> Controller2 : 14. getSessionHistory()
  activate Controller2
  Controller2 -> Service4 : 15. getSessionHistory()
  activate Service4
  alt [request is authorized and input is valid]
    Service4 -> Repository3 : 16a. findByUserIdAndSessionStatusOrderByStartedAtDesc()
    activate Repository3
    Repository3 -> DB : 16a-1. SELECT
    activate DB
    DB --> Repository3 : 16a-2. queryResult
    deactivate DB
    Repository3 --> Service4 : 16a-3. domainRecords
    deactivate Repository3
    Service4 --> Controller2 : 16a-4. resultDTO
    deactivate Service4
    Controller2 --> UI1 : 16a-5. 200 OK
    deactivate Controller2
    UI1 --> Actor : 16a-6. displayViewExerciseHistoryAndResultsResult()
    deactivate UI1
  else [request is invalid, forbidden or unavailable]
    Service4 --> Controller2 : 16b. domainError
    deactivate Service4
    Controller2 --> UI1 : 16b-1. 400 / 401 / 403 / 404
    deactivate Controller2
    UI1 --> Actor : 16b-2. displayActionableError()
    deactivate UI1
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Pregnancy Exercise Session, Safety Check and Posture Feedback Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-27 Browse Pregnancy Exercises; UC-28 Complete Pre-exercise Safety Check; UC-29 Perform Camera-guided Exercise Session; UC-30 View Exercise History and Results.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Optional camera feedback after permission and safety clearance.
- The following remains outside this contract: Medical fitness diagnosis.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
