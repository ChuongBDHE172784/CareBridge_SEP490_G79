# MF-02 — Pregnancy Exercise Safety and Session

| Field | Value |
| --- | --- |
| Major Feature | **MF-02 — Mother Care Journey** |
| Function package | **Pregnancy Exercise Safety and Session** |
| Code-first use cases | `UC-MH-18, UC-MH-19` |
| Status | **Draft** |
| Baseline | Current reachable code and tests, 2026-08-23 |
| Diagram convention | `../sequence-diagram-skill.md`; explicit lifeline order, numbered messages, balanced activation |

## 1. Tổng quan luồng chính

Design exercise discovery, pre-exercise safety, session state, and posture sidecar integration.

- **UC-MH-18 — Browse Exercises and Complete Safety Check:** Browse pregnancy exercises, review an exercise detail, submit the implemented pre-session safety check, and reload its latest result before camera execution.
- **UC-MH-19 — Perform Exercise Session and Review Results:** Load the active posture configuration, start an eligible exercise session, run posture analysis, complete/abort it, and review the result or history.

The code is authoritative for routes, handlers, delegation, authorization, and persisted state. Report1 section 6.2 is authoritative for the Major Feature name. Historical UC numbering and obsolete triage plans are not design inputs.

## 2. Function Design — Code Traceability

| UC | Actor goal | Method / route | Exact handler | Delegated function | Authorization | Source |
| --- | --- | --- | --- | --- | --- | --- |
| `UC-MH-18` | Browse Exercises and Complete Safety Check | `GET /api/v1/exercises` | `ExerciseController.listExercises()` | `IExerciseQueryService.listPublishedExercises()` → `ExerciseRepository.findPublishedByFilters()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| `UC-MH-18` | Browse Exercises and Complete Safety Check | `GET /api/v1/exercises/{exerciseId}` | `ExerciseController.getExerciseDetail()` | `IExerciseDetailQueryService.getExerciseDetail()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| `UC-MH-18` | Browse Exercises and Complete Safety Check | `POST /api/v1/exercises/{exerciseId}/safety-check` | `ExerciseController.submitSafetyCheck()` | `IExerciseSafetyCheckService.submitSafetyCheck()` → `ExerciseSafetyCheckRepository.save()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| `UC-MH-18` | Browse Exercises and Complete Safety Check | `GET /api/v1/exercises/{exerciseId}/safety-check/latest` | `ExerciseController.getLatestSafetyCheck()` | `IExerciseSafetyCheckService.getLatestSafetyCheck()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| `UC-MH-19` | Perform Exercise Session and Review Results | `GET /api/v1/exercises/sessions/history` | `ExerciseSessionController.getSessionHistory()` | `IExerciseSessionHistoryService.getSessionHistory()` → `ExerciseSessionRepository.findCompletedByUserIdAndFilters()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `UC-MH-19` | Perform Exercise Session and Review Results | `PATCH /api/v1/exercises/sessions/{sessionId}/complete` | `ExerciseSessionController.completeSession()` | `IExerciseSessionService.completeSession()` → `ExerciseSessionRepository.save()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `UC-MH-19` | Perform Exercise Session and Review Results | `PATCH /api/v1/exercises/sessions/{sessionId}/pause` | `ExerciseSessionController.pauseSession()` | `IExerciseSessionService.pauseSession()` → `ExerciseSessionRepository.save()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `UC-MH-19` | Perform Exercise Session and Review Results | `POST /api/v1/exercises/sessions/{sessionId}/posture-events` | `ExerciseSessionController.analyzePosture()` | `IPostureAnalysisService.analyzePosture()` → `PostureFeedbackEventRepository.save()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `UC-MH-19` | Perform Exercise Session and Review Results | `GET /api/v1/exercises/sessions/{sessionId}/result` | `ExerciseSessionController.getSessionResult()` | `IExerciseSessionResultService.getSessionResult()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `UC-MH-19` | Perform Exercise Session and Review Results | `PATCH /api/v1/exercises/sessions/{sessionId}/resume` | `ExerciseSessionController.resumeSession()` | `IExerciseSessionService.resumeSession()` → `ExerciseSessionRepository.save()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java` |
| `UC-MH-19` | Perform Exercise Session and Review Results | `GET /api/v1/exercises/{exerciseId}/posture-config` | `ExerciseController.getPostureConfig()` | `IPostureConfigService.getActiveConfig()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| `UC-MH-19` | Perform Exercise Session and Review Results | `POST /api/v1/exercises/{exerciseId}/sessions` | `ExerciseController.startSession()` | `IExerciseSessionService.startSession()` → `UserRepository.findByIdForUpdate()` | hasAnyRole('MOTHER', 'SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java` |
| `UC-MH-19` | Perform Exercise Session and Review Results | `POST /v1/inference/landmarks` | `main.infer()` | — | No explicit internal-key dependency on this handler; router/application policy must be checked | `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py` |

## 3. Class Diagram

This design-level UML follows the current source declarations: fields become attributes, reachable methods become operations, `implements` becomes realization, repository inheritance becomes generalization, and runtime-only calls remain dependencies. A missing layer or ownership relation is not invented.

```plantuml
@startuml ClassDiagram_03PregnancyExerciseSafetyandSession
skinparam classAttributeIconSize 0
skinparam wrapWidth 250
hide empty members

class "ExerciseSessionScreen" as UIExerciseSessionScreen <<UI>>
class "MotherExerciseScreen" as UIMotherExerciseScreen <<UI>>
class "ExerciseController" as ControllerExerciseController <<Controller>> {
  - exerciseQueryService: IExerciseQueryService
  - exerciseDetailQueryService: IExerciseDetailQueryService
  - safetyCheckService: IExerciseSafetyCheckService
  - sessionService: IExerciseSessionService
  - postureConfigService: IPostureConfigService
  + startSession(exerciseId: UUID, request: StartSessionRequest, principal: Principal): ResponseEntity<ApiResponse<StartSessionResponse>>
  + submitSafetyCheck(exerciseId: UUID, request: SubmitSafetyCheckRequest, principal: Principal): ResponseEntity<ApiResponse<SafetyCheckResponse>>
}
class "ExerciseSessionController" as ControllerExerciseSessionController <<Controller>> {
  - sessionService: IExerciseSessionService
  - sessionResultService: IExerciseSessionResultService
  - sessionHistoryService: IExerciseSessionHistoryService
  - postureAnalysisService: IPostureAnalysisService
  + analyzePosture(sessionId: UUID, request: PostureEventRequest, principal: Principal): ResponseEntity<ApiResponse<PostureFeedbackResponse>>
}
interface "IExerciseSafetyCheckService" as ServiceContractIExerciseSafetyCheckService <<Service>> {
  + submitSafetyCheck(exerciseId: UUID, request: SubmitSafetyCheckRequest, userId: UUID): ApiResponse<SafetyCheckResponse>
}
class "ExerciseSafetyCheckServiceImpl" as ServiceExerciseSafetyCheckServiceImpl <<Service>> {
  - safetyCheckRepository: ExerciseSafetyCheckRepository
  - exerciseRepository: ExerciseRepository
  - safetyCheckMapper: SafetyCheckMapper
  - safetyCheckPolicy: SafetyCheckPolicy
  - careContextResolver: ExerciseCareContextResolver
  + submitSafetyCheck(exerciseId: UUID, request: SubmitSafetyCheckRequest, userId: UUID): ApiResponse<SafetyCheckResponse>
}
ServiceContractIExerciseSafetyCheckService <|.. ServiceExerciseSafetyCheckServiceImpl : implements
interface "IExerciseSessionService" as ServiceContractIExerciseSessionService <<Service>> {
  + startSession(exerciseId: UUID, request: StartSessionRequest, userId: UUID): StartSessionResponse
}
class "ExerciseSessionServiceImpl" as ServiceExerciseSessionServiceImpl <<Service>> {
  - sessionRepository: ExerciseSessionRepository
  - exerciseRepository: ExerciseRepository
  - safetyCheckRepository: ExerciseSafetyCheckRepository
  - postureFeedbackEventRepository: PostureFeedbackEventRepository
  - sessionMapper: ExerciseSessionMapper
  - objectMapper: ObjectMapper
  - careContextResolver: ExerciseCareContextResolver
  - userRepository: UserRepository
  + startSession(exerciseId: UUID, request: StartSessionRequest, userId: UUID): StartSessionResponse
}
ServiceContractIExerciseSessionService <|.. ServiceExerciseSessionServiceImpl : implements
interface "IPostureAnalysisService" as ServiceContractIPostureAnalysisService <<Service>> {
  + analyzePosture(sessionId: UUID, userId: UUID, request: PostureEventRequest): ApiResponse<PostureFeedbackResponse>
}
class "PostureAnalysisServiceImpl" as ServicePostureAnalysisServiceImpl <<Service>> {
  - sessionRepository: ExerciseSessionRepository
  - postureConfigRepository: PostureAnalysisConfigRepository
  - postureFeedbackEventRepository: PostureFeedbackEventRepository
  - inferenceConfigResolver: PostureInferenceConfigResolver
  - careContextResolver: ExerciseCareContextResolver
  + analyzePosture(sessionId: UUID, userId: UUID, request: PostureEventRequest): ApiResponse<PostureFeedbackResponse>
}
ServiceContractIPostureAnalysisService <|.. ServicePostureAnalysisServiceImpl : implements
interface "ExerciseSafetyCheckRepository" as RepositoryExerciseSafetyCheckRepository <<Repository>> {
  + save(entity: ExerciseSafetyCheck): ExerciseSafetyCheck
}
class "ExerciseSafetyCheck" as EntityExerciseSafetyCheck <<Entity>> {
  - safetyCheckId: UUID
  - exerciseId: UUID
  - journeyId: UUID
  - userId: UUID
  - answerJson: Map<String, Boolean>
  - redFlagDetected: Boolean
  - resultStatus: SafetyCheckStatus
  - blockedReason: String
}
interface "JpaRepository<ExerciseSafetyCheck, UUID>" as RepositoryBaseExerciseSafetyCheckRepository <<Framework>>
RepositoryBaseExerciseSafetyCheckRepository <|-- RepositoryExerciseSafetyCheckRepository : extends
interface "PostureFeedbackEventRepository" as RepositoryPostureFeedbackEventRepository <<Repository>> {
  + save(entity: PostureFeedbackEvent): PostureFeedbackEvent
}
class "PostureFeedbackEvent" as EntityPostureFeedbackEvent <<Entity>> {
  - feedbackEventId: UUID
  - exerciseSessionId: UUID
  - journeyId: UUID
  - postureConfigId: UUID
  - eventTimeMs: Long
  - postureCode: String
  - confidenceScore: BigDecimal
  - severity: String
}
interface "JpaRepository<PostureFeedbackEvent, UUID>" as RepositoryBasePostureFeedbackEventRepository <<Framework>>
RepositoryBasePostureFeedbackEventRepository <|-- RepositoryPostureFeedbackEventRepository : extends
interface "UserRepository" as RepositoryUserRepository <<Repository>> {
  + findByIdForUpdate(id: java.util.UUID): Optional<User>
}
class "User" as EntityUser <<Entity>> {
  - person: Person
  - phone: String
  - email: String
  - passwordHash: String
  - name: String
  - displayName: String
  - dateOfBirth: LocalDate
  - area: String
}
interface "JpaRepository<User, java.util.UUID>" as RepositoryBaseUserRepository <<Framework>>
RepositoryBaseUserRepository <|-- RepositoryUserRepository : extends
class "PostgreSQL" as DB <<Database>>
UIExerciseSessionScreen ..> ControllerExerciseController : invokes API
UIExerciseSessionScreen ..> ControllerExerciseSessionController : invokes API
UIMotherExerciseScreen ..> ControllerExerciseController : invokes API
ControllerExerciseController --> ServiceContractIExerciseSafetyCheckService : delegates
ControllerExerciseController --> ServiceContractIExerciseSessionService : delegates
ControllerExerciseSessionController --> ServiceContractIPostureAnalysisService : delegates
ServiceExerciseSafetyCheckServiceImpl --> RepositoryExerciseSafetyCheckRepository : reads / writes
ServiceExerciseSessionServiceImpl --> RepositoryUserRepository : reads / writes
ServicePostureAnalysisServiceImpl --> RepositoryPostureFeedbackEventRepository : reads / writes
RepositoryExerciseSafetyCheckRepository ..> EntityExerciseSafetyCheck : maps
RepositoryPostureFeedbackEventRepository ..> EntityPostureFeedbackEvent : maps
RepositoryUserRepository ..> EntityUser : maps
RepositoryExerciseSafetyCheckRepository ..> DB : persists
RepositoryPostureFeedbackEventRepository ..> DB : persists
RepositoryUserRepository ..> DB : persists
@enduml
```

**Figure 1 — Class Diagram: Pregnancy Exercise Safety and Session**

## 4. Sequence Diagram — Main Flow

Each group is a representative reachable main flow. The full endpoint surface remains in the Function Design table above.

```plantuml
@startuml
skinparam shadowing false
skinparam maxMessageSize 90
title Pregnancy Exercise Safety and Session — code-reachable representative flows

actor "Mother" as AMother
boundary "MotherExerciseScreen" as UIMotherExerciseScreen <<boundary>>
boundary "ExerciseSessionScreen" as UIExerciseSessionScreen <<boundary>>
participant "JwtAuthenticationFilter" as JWT <<middleware>>
control "ExerciseController" as CExerciseController <<control>>
control "ExerciseSessionController" as CExerciseSessionController <<control>>
participant "IExerciseSafetyCheckService" as SIExerciseSafetyCheckService <<service>>
participant "IExerciseSessionService" as SIExerciseSessionService <<service>>
participant "IPostureAnalysisService" as SIPostureAnalysisService <<service>>
participant "ExerciseSafetyCheckRepository" as RExerciseSafetyCheckRepository <<repository>>
participant "UserRepository" as RUserRepository <<repository>>
participant "PostureFeedbackEventRepository" as RPostureFeedbackEventRepository <<repository>>
database "PostgreSQL" as DB

group UC-MH-18 — Browse Exercises and Complete Safety Check [submitSafetyCheck()]
AMother -> UIMotherExerciseScreen : 1. submitExerciseSafetyCheck()
activate UIMotherExerciseScreen
alt [authorized request succeeds]
UIMotherExerciseScreen -> JWT : 2a. POST /api/v1/exercises/{exerciseId}/safety-check with bearer token
activate JWT
JWT -> CExerciseController : 2a-1. submitSafetyCheck(exerciseId, request, principal)
activate CExerciseController
CExerciseController -> SIExerciseSafetyCheckService : 2a-2. submitSafetyCheck(exerciseId, request, userId)
activate SIExerciseSafetyCheckService
SIExerciseSafetyCheckService -> RExerciseSafetyCheckRepository : 2a-3. save()
activate RExerciseSafetyCheckRepository
RExerciseSafetyCheckRepository -> DB : 2a-4. INSERT / UPDATE ExerciseSafetyCheck
activate DB
DB --> RExerciseSafetyCheckRepository : 2a-5. persistedExerciseSafetyCheck
deactivate DB
RExerciseSafetyCheckRepository --> SIExerciseSafetyCheckService : 2a-6. persistedExerciseSafetyCheck
deactivate RExerciseSafetyCheckRepository
SIExerciseSafetyCheckService --> CExerciseController : 2a-7. safetyCheckResponse
deactivate SIExerciseSafetyCheckService
CExerciseController --> JWT : 2a-8. safetyCheckResponse
deactivate CExerciseController
JWT --> UIMotherExerciseScreen : 2a-9. 201 Created — safetyCheckResponse
deactivate JWT
UIMotherExerciseScreen --> AMother : 2a-10. displayExerciseSafetyDecision()
else [authentication or role authorization fails]
UIMotherExerciseScreen -> JWT : 2b. POST /api/v1/exercises/{exerciseId}/safety-check with invalid or insufficient bearer token
activate JWT
JWT --> UIMotherExerciseScreen : 2b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UIMotherExerciseScreen --> AMother : 2b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UIMotherExerciseScreen
end

group UC-MH-19 — Perform Exercise Session and Review Results [startSession()]
AMother -> UIExerciseSessionScreen : 3. startExerciseSession()
activate UIExerciseSessionScreen
alt [authorized request succeeds]
UIExerciseSessionScreen -> JWT : 4a. POST /api/v1/exercises/{exerciseId}/sessions with bearer token
activate JWT
JWT -> CExerciseController : 4a-1. startSession(exerciseId, request, principal)
activate CExerciseController
CExerciseController -> SIExerciseSessionService : 4a-2. startSession(exerciseId, request, userId)
activate SIExerciseSessionService
SIExerciseSessionService -> RUserRepository : 4a-3. findByIdForUpdate(id)
activate RUserRepository
RUserRepository -> DB : 4a-4. SELECT User via findByIdForUpdate()
activate DB
DB --> RUserRepository : 4a-5. userQueryResult
deactivate DB
RUserRepository --> SIExerciseSessionService : 4a-6. optionalUser
deactivate RUserRepository
SIExerciseSessionService --> CExerciseController : 4a-7. startSessionResponse
deactivate SIExerciseSessionService
CExerciseController --> JWT : 4a-8. startSessionResponse
deactivate CExerciseController
JWT --> UIExerciseSessionScreen : 4a-9. 201 Created — startSessionResponse
deactivate JWT
UIExerciseSessionScreen --> AMother : 4a-10. displayActiveExerciseSession()
else [authentication or role authorization fails]
UIExerciseSessionScreen -> JWT : 4b. POST /api/v1/exercises/{exerciseId}/sessions with invalid or insufficient bearer token
activate JWT
JWT --> UIExerciseSessionScreen : 4b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UIExerciseSessionScreen --> AMother : 4b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UIExerciseSessionScreen
end

group UC-MH-19 — Perform Exercise Session and Review Results [analyzePosture()]
AMother -> UIExerciseSessionScreen : 5. submitPostureFrame()
activate UIExerciseSessionScreen
alt [authorized request succeeds]
UIExerciseSessionScreen -> JWT : 6a. POST /api/v1/exercises/sessions/{sessionId}/posture-events with bearer token
activate JWT
JWT -> CExerciseSessionController : 6a-1. analyzePosture(sessionId, request, principal)
activate CExerciseSessionController
CExerciseSessionController -> SIPostureAnalysisService : 6a-2. analyzePosture(sessionId, userId, request)
activate SIPostureAnalysisService
SIPostureAnalysisService -> RPostureFeedbackEventRepository : 6a-3. save()
activate RPostureFeedbackEventRepository
RPostureFeedbackEventRepository -> DB : 6a-4. INSERT / UPDATE PostureFeedbackEvent
activate DB
DB --> RPostureFeedbackEventRepository : 6a-5. persistedPostureFeedbackEvent
deactivate DB
RPostureFeedbackEventRepository --> SIPostureAnalysisService : 6a-6. persistedPostureFeedbackEvent
deactivate RPostureFeedbackEventRepository
SIPostureAnalysisService --> CExerciseSessionController : 6a-7. postureFeedbackResponse
deactivate SIPostureAnalysisService
CExerciseSessionController --> JWT : 6a-8. postureFeedbackResponse
deactivate CExerciseSessionController
JWT --> UIExerciseSessionScreen : 6a-9. 200 OK — postureFeedbackResponse
deactivate JWT
UIExerciseSessionScreen --> AMother : 6a-10. displayPostureFeedback()
else [authentication or role authorization fails]
UIExerciseSessionScreen -> JWT : 6b. POST /api/v1/exercises/sessions/{sessionId}/posture-events with invalid or insufficient bearer token
activate JWT
JWT --> UIExerciseSessionScreen : 6b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UIExerciseSessionScreen --> AMother : 6b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UIExerciseSessionScreen
end
@enduml
```

**Brief Explanation:**

1. The actor starts each grouped use case through the code-reachable UI boundary.
2. Protected requests pass through JwtAuthenticationFilter; rejected credentials or roles return 401 Unauthorized or 403 Forbidden without invoking the controller.
3. The controller receives the request and invokes the exact delegated operation resolved from the current source.
4. The service applies the business policy and coordinates downstream collaborators while its caller remains active.
5. The repository executes the represented persistence operation and returns the stored or queried result before its activation ends.
6. The HTTP response unwinds through middleware when present, and the UI renders the server-authoritative outcome to the actor.

## 5. State Chart Diagram

The lifecycle below belongs to **ExerciseSession.sessionStatus**. Its states are the persisted constants declared in the sources cited under this diagram, and every transition, guard, and action is taken from the service method that writes that constant. No unreachable state is introduced.

```plantuml
@startuml StateChartDiagram_03PregnancyExerciseSafetyandSession
hide empty description
[*] --> NotStarted

NotStarted --> InProgress : startSession()\n[pre-exercise safety check passed]\n/ createSession(IN_PROGRESS)
InProgress --> Paused : pauseSession()\n[sessionStatus == IN_PROGRESS]\n/ setSessionStatus(PAUSED)
Paused --> InProgress : resumeSession()\n[sessionStatus == PAUSED]\n/ setSessionStatus(IN_PROGRESS)
InProgress --> Completed : completeSession()\n/ setSessionStatus(COMPLETED)
Paused --> Completed : completeSession()\n/ setSessionStatus(COMPLETED)
InProgress --> Abandoned : abandonSession()\n/ setSessionStatus(ABANDONED)
Paused --> Abandoned : abandonSession()\n/ setSessionStatus(ABANDONED)
InProgress --> InProgress : submitPostureFrame()\n[sessionStatus == IN_PROGRESS]\n/ analysePosture()
Completed --> Completed : readSessionResult()\n[sessionStatus == COMPLETED]\n/ returnStoredResult()

InProgress : sessionStatus = IN_PROGRESS
Paused : sessionStatus = PAUSED
Completed : sessionStatus = COMPLETED
Abandoned : sessionStatus = ABANDONED
@enduml
```

**Figure 2 — State Chart Diagram: Pregnancy Exercise Safety and Session**

**Brief Explanation:**

1. A session starts in `NotStarted`; the pre-exercise safety check is the guard that must pass before `startSession()` creates the row.
2. `pauseSession()` and `resumeSession()` each carry an explicit status guard, so the pair cannot be replayed out of order.
3. Both `InProgress` and `Paused` may complete or be abandoned, which is why `completeSession()` and `abandonSession()` are drawn from each of them rather than only from `InProgress`.
4. The action `analysePosture()` runs as a self-transition on `InProgress` only — `PostureAnalysisServiceImpl` rejects frames for any other status, so a paused session cannot be scored.
5. `ExerciseSessionResultServiceImpl` guards result reads on `sessionStatus == COMPLETED`, so an abandoned session never exposes a result.
6. `Completed` and `Abandoned` are terminal: no transition in the code returns a session from either state.

**State sources:**

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/entity/SessionStatus.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/ExerciseSessionServiceImpl.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/ExerciseSessionResultServiceImpl.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java`

## 6. Business Rules Applied

| UC | Enforced business/security rules | Known boundary or gap |
| --- | --- | --- |
| `UC-MH-18` | Stage/publication eligibility and safety-check policy are server authoritative. A failed/blocked safety check cannot be bypassed by client navigation. | No additional gap recorded in the code-first baseline. |
| `UC-MH-19` | Server session state is canonical; camera/model feedback is advisory. The sidecar inference contract validates sequence and landmark payloads; provider failure follows the implemented backend fallback/degraded path. Late frames and retries must not mutate a completed/aborted session. | No additional gap recorded in the code-first baseline. |

## 7. Partial / Excluded Boundaries

- No extra release boundary beyond the code-first UC catalogue is introduced by this package.

## 8. Code and Test Evidence

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/policy/SafetyCheckPolicy.java`
- `05_Development/CareBridgeMobileApp/lib/features/exercise/screens/mother_exercise_screen.dart`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckServiceTest.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapter.java`
- `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py`
- `05_Development/CareBridgeMobileApp/lib/features/exercise/screens/exercise_session_screen.dart`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryEmbeddedPostgresTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapterTest.java`
- `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/tests/test_http_contract.py`
- `05_Development/CareBridgeMobileApp/test/features/exercise/exercise_session_screen_test.dart`
