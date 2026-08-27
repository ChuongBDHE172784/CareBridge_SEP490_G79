# MF-10 — Detected Impact, Safety Check, Alert, and Feedback

| Field | Value |
| --- | --- |
| Major Feature | **MF-10 — Smart Activity Monitoring & Safety Support** |
| Function package | **Detected Impact, Safety Check, Alert, and Feedback** |
| Code-first use cases | `UC-ES-05` |
| Status | **Draft** |
| Baseline | Current reachable code and tests, 2026-08-23 |
| Diagram convention | `../sequence-diagram-skill.md`; explicit lifeline order, numbered messages, balanced activation |

## 1. Tổng quan luồng chính

Design IMU ingestion, event history, safety confirmation, emergency alert, and false-positive feedback.

- **UC-ES-05 — Respond to Detected Fall or Impact:** Monitor device signals, display the suspected-fall countdown, and confirm safe/false-positive or escalate to emergency.

The code is authoritative for routes, handlers, delegation, authorization, and persisted state. Report1 section 6.2 is authoritative for the Major Feature name. Historical UC numbering and obsolete triage plans are not design inputs.

## 2. Function Design — Code Traceability

| UC | Actor goal | Method / route | Exact handler | Delegated function | Authorization | Source |
| --- | --- | --- | --- | --- | --- | --- |
| `UC-ES-05` | Respond to Detected Fall or Impact | `GET /api/v1/safety/events` | `FallDetectionController.listEvents()` | `IFallDetectionService.listSafetyEvents()` → `ISafetyEventRepository.findByUserIdOrderByDetectedAtDesc()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
| `UC-ES-05` | Respond to Detected Fall or Impact | `POST /api/v1/safety/events/{eventId}/confirm` | `FallDetectionController.confirmSafetyCheck()` | `IFallDetectionService.confirmSafetyCheck()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
| `UC-ES-05` | Respond to Detected Fall or Impact | `POST /api/v1/safety/events/{eventId}/emergency-alert` | `FallDetectionController.sendEmergencyAlert()` | `IFallDetectionService.sendEmergencyAlert()` → `ISafetyEventRepository.save()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
| `UC-ES-05` | Respond to Detected Fall or Impact | `POST /api/v1/safety/events/{eventId}/false-positive` | `FallDetectionController.reportFalsePositive()` | `IFallDetectionService.reportFalsePositive()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |
| `UC-ES-05` | Respond to Detected Fall or Impact | `POST /api/v1/safety/imu-data` | `FallDetectionController.processImuData()` | `IFallDetectionService.processImuData()` → `IImuMonitoringSessionRepository.findActiveForUpdateByUserId()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |

## 3. Class Diagram

This design-level UML follows the current source declarations: fields become attributes, reachable methods become operations, `implements` becomes realization, repository inheritance becomes generalization, and runtime-only calls remain dependencies. A missing layer or ownership relation is not invented.

```plantuml
@startuml ClassDiagram_02DetectedImpactSafetyCheckAlertandFeedback
skinparam classAttributeIconSize 0
hide empty members

class "SafetyMonitoringScreen" as UISafetyMonitoringScreen <<UI>>
class "FallDetectionController" as ControllerFallDetectionController <<Controller>> {
  - fallDetectionService: IFallDetectionService
  - safetyConfigService: ISafetyConfigService
  + confirmSafetyCheck(eventId: UUID, request: SafetyEventActionRequest, principal: Principal): ResponseEntity<ApiResponse<SafetyEventResponse>>
  + processImuData(request: ImuDataRequest, principal: Principal): ResponseEntity<ApiResponse<SafetyEventResponse>>
  + sendEmergencyAlert(eventId: UUID, principal: Principal): ResponseEntity<ApiResponse<Void>>
}
interface "IFallDetectionService" as ServiceContractIFallDetectionService <<Service>> {
  + confirmSafetyCheck(userId: UUID, eventId: UUID, note: String): SafetyEventResponse
  + processImuData(userId: UUID, payload: ImuDataPayload): SafetyEventResponse
  + sendEmergencyAlert(userId: UUID, eventId: UUID): void
}
class "FallDetectionService" as ServiceFallDetectionService <<Service>> {
  - imuSessionRepository: IImuMonitoringSessionRepository
  - safetyEventRepository: ISafetyEventRepository
  - algorithmService: IFallDetectionAlgorithmService
  - eventPublisher: ApplicationEventPublisher
  - safetyConfigStore: SafetyConfigStore
  - responseRepository: SafetyEventResponseRepository
  - consentPolicy: SafetyConsentPolicy
  - emergencyService: IEmergencyService
  + confirmSafetyCheck(userId: UUID, eventId: UUID, note: String): SafetyEventResponse
  + processImuData(userId: UUID, payload: ImuDataPayload): SafetyEventResponse
  + sendEmergencyAlert(userId: UUID, eventId: UUID): void
}
ServiceContractIFallDetectionService <|.. ServiceFallDetectionService : implements
interface "IImuMonitoringSessionRepository" as RepositoryIImuMonitoringSessionRepository <<Repository>> {
  + findActiveForUpdateByUserId(userId: UUID): Optional<ImuMonitoringSession>
}
class "ImuMonitoringSession" as EntityImuMonitoringSession <<Entity>> {
  - id: UUID
  - userId: UUID
  - status: ImuSessionStatus
  - sensitivityLevel: String
  - startedAt: Instant
  - endedAt: Instant
  - createdBy: UUID
}
interface "JpaRepository<ImuMonitoringSession, UUID>" as RepositoryBaseIImuMonitoringSessionRepository <<Framework>>
RepositoryBaseIImuMonitoringSessionRepository <|-- RepositoryIImuMonitoringSessionRepository : extends
interface "ISafetyEventRepository" as RepositoryISafetyEventRepository <<Repository>> {
  + save(entity: SafetyEvent): SafetyEvent
}
class "SafetyEvent" as EntitySafetyEvent <<Entity>> {
  - id: UUID
  - userId: UUID
  - imuSessionId: UUID
  - eventType: SafetyEventType
  - magnitude: BigDecimal
  - userLatitude: BigDecimal
  - userLongitude: BigDecimal
  - detectedAt: Instant
}
interface "JpaRepository<SafetyEvent, UUID>" as RepositoryBaseISafetyEventRepository <<Framework>>
RepositoryBaseISafetyEventRepository <|-- RepositoryISafetyEventRepository : extends
class "PostgreSQL" as DB <<Database>>
UISafetyMonitoringScreen ..> ControllerFallDetectionController : invokes API
ControllerFallDetectionController --> ServiceContractIFallDetectionService : delegates
ServiceFallDetectionService --> RepositoryIImuMonitoringSessionRepository : reads / writes
ServiceFallDetectionService --> RepositoryISafetyEventRepository : reads / writes
RepositoryIImuMonitoringSessionRepository ..> EntityImuMonitoringSession : maps
RepositoryISafetyEventRepository ..> EntitySafetyEvent : maps
RepositoryIImuMonitoringSessionRepository ..> DB : persists
RepositoryISafetyEventRepository ..> DB : persists
@enduml
```

**Figure 1 — Class Diagram: Detected Impact, Safety Check, Alert, and Feedback**

## 4. Sequence Diagram — Main Flow

Each group is a representative reachable main flow. The full endpoint surface remains in the Function Design table above.

```plantuml
@startuml
skinparam shadowing false
skinparam maxMessageSize 90
title Detected Impact, Safety Check, Alert, and Feedback — code-reachable representative flows

actor "Mother" as AMother
boundary "SafetyMonitoringScreen" as UISafetyMonitoringScreen <<boundary>>
participant "JwtAuthenticationFilter" as JWT <<middleware>>
control "FallDetectionController" as CFallDetectionController <<control>>
participant "IFallDetectionService" as SIFallDetectionService <<service>>
participant "IImuMonitoringSessionRepository" as RIImuMonitoringSessionRepository <<repository>>
participant "ISafetyEventRepository" as RISafetyEventRepository <<repository>>
database "PostgreSQL" as DB

group UC-ES-05 — Respond to Detected Fall or Impact [processImuData()]
AMother -> UISafetyMonitoringScreen : 1. submitImuSample(sensorData)
activate UISafetyMonitoringScreen
alt [authorized request succeeds]
UISafetyMonitoringScreen -> JWT : 2a. POST /api/v1/safety/imu-data with bearer token
activate JWT
JWT -> CFallDetectionController : 2a-1. processImuData(request, principal)
activate CFallDetectionController
CFallDetectionController -> SIFallDetectionService : 2a-2. processImuData(userId, payload)
activate SIFallDetectionService
SIFallDetectionService -> RIImuMonitoringSessionRepository : 2a-3. findActiveForUpdateByUserId(userId)
activate RIImuMonitoringSessionRepository
RIImuMonitoringSessionRepository -> DB : 2a-4. SELECT ImuMonitoringSession via findActiveForUpdateByUserId()
activate DB
DB --> RIImuMonitoringSessionRepository : 2a-5. imuMonitoringSessionQueryResult
deactivate DB
RIImuMonitoringSessionRepository --> SIFallDetectionService : 2a-6. optionalImuMonitoringSession
deactivate RIImuMonitoringSessionRepository
SIFallDetectionService --> CFallDetectionController : 2a-7. safetyEventResponse
deactivate SIFallDetectionService
CFallDetectionController --> JWT : 2a-8. safetyEventResponse
deactivate CFallDetectionController
JWT --> UISafetyMonitoringScreen : 2a-9. 200 OK — safetyEventResponse
deactivate JWT
UISafetyMonitoringScreen --> AMother : 2a-10. displayDetectedSafetyState()
else [authentication or role authorization fails]
UISafetyMonitoringScreen -> JWT : 2b. POST /api/v1/safety/imu-data with invalid or insufficient bearer token
activate JWT
JWT --> UISafetyMonitoringScreen : 2b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UISafetyMonitoringScreen --> AMother : 2b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UISafetyMonitoringScreen
end

group UC-ES-05 — Respond to Detected Fall or Impact [confirmSafetyCheck()]
AMother -> UISafetyMonitoringScreen : 3. confirmSafetyStatus(eventId)
activate UISafetyMonitoringScreen
alt [authorized request succeeds]
UISafetyMonitoringScreen -> JWT : 4a. POST /api/v1/safety/events/{eventId}/confirm with bearer token
activate JWT
JWT -> CFallDetectionController : 4a-1. confirmSafetyCheck(eventId, request, principal)
activate CFallDetectionController
CFallDetectionController -> SIFallDetectionService : 4a-2. confirmSafetyCheck(userId, eventId, note)
activate SIFallDetectionService
SIFallDetectionService --> CFallDetectionController : 4a-3. safetyEventResponse
deactivate SIFallDetectionService
CFallDetectionController --> JWT : 4a-4. safetyEventResponse
deactivate CFallDetectionController
JWT --> UISafetyMonitoringScreen : 4a-5. 200 OK — safetyEventResponse
deactivate JWT
UISafetyMonitoringScreen --> AMother : 4a-6. displayConfirmedSafetyState()
else [authentication or role authorization fails]
UISafetyMonitoringScreen -> JWT : 4b. POST /api/v1/safety/events/{eventId}/confirm with invalid or insufficient bearer token
activate JWT
JWT --> UISafetyMonitoringScreen : 4b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UISafetyMonitoringScreen --> AMother : 4b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UISafetyMonitoringScreen
end

group UC-ES-05 — Respond to Detected Fall or Impact [sendEmergencyAlert()]
AMother -> UISafetyMonitoringScreen : 5. requestEmergencyAlert(eventId)
activate UISafetyMonitoringScreen
alt [authorized request succeeds]
UISafetyMonitoringScreen -> JWT : 6a. POST /api/v1/safety/events/{eventId}/emergency-alert with bearer token
activate JWT
JWT -> CFallDetectionController : 6a-1. sendEmergencyAlert(eventId, principal)
activate CFallDetectionController
CFallDetectionController -> SIFallDetectionService : 6a-2. sendEmergencyAlert(userId, eventId)
activate SIFallDetectionService
SIFallDetectionService -> RISafetyEventRepository : 6a-3. save()
activate RISafetyEventRepository
RISafetyEventRepository -> DB : 6a-4. INSERT / UPDATE SafetyEvent
activate DB
DB --> RISafetyEventRepository : 6a-5. persistedSafetyEvent
deactivate DB
RISafetyEventRepository --> SIFallDetectionService : 6a-6. persistedSafetyEvent
deactivate RISafetyEventRepository
SIFallDetectionService --> CFallDetectionController : 6a-7. completed
deactivate SIFallDetectionService
CFallDetectionController --> JWT : 6a-8. completed
deactivate CFallDetectionController
JWT --> UISafetyMonitoringScreen : 6a-9. 202 Accepted — completed
deactivate JWT
UISafetyMonitoringScreen --> AMother : 6a-10. displayEmergencyAlertAccepted()
else [authentication or role authorization fails]
UISafetyMonitoringScreen -> JWT : 6b. POST /api/v1/safety/events/{eventId}/emergency-alert with invalid or insufficient bearer token
activate JWT
JWT --> UISafetyMonitoringScreen : 6b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UISafetyMonitoringScreen --> AMother : 6b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UISafetyMonitoringScreen
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

The lifecycle below belongs to **SafetyEvent.status for a detected impact (the SENSOR_SELF_TEST variant is modelled in document 01)**. Its states are the persisted constants declared in the sources cited under this diagram, and every transition, guard, and action is taken from the service method that writes that constant. No unreachable state is introduced.

```plantuml
@startuml StateChartDiagram_02DetectedImpactSafetyCheckAlertandFeedback
hide empty description
[*] --> NoEvent

NoEvent --> Open : processImuData() [fall analysis confirms a suspected impact] / persistEvent(OPEN) and armCountdown()
Open --> ConfirmedSafe : confirmSafetyCheck() [event has no response yet] / recordResponse(I_AM_OK)
Open --> FalsePositive : reportFalsePositive() [event has no response yet] / recordResponse(FALSE_POSITIVE)
Open --> EscalationRequested : sendEmergencyAlert() [event has no response yet] / recordResponse(NEED_HELP)
Open --> TimedOut : processExpiredCountdowns() [deadline passed && emergencyAutoAlert == false] / recordSystemTimeout()
Open --> EscalationRequested : processExpiredCountdowns() [deadline passed && emergencyAutoAlert == true] / autoEscalate()
EscalationRequested --> EmergencyAlertSent : familyAlertDispatched() [status == ESCALATION_REQUESTED] / setStatus(EMERGENCY_ALERT_SENT)
ConfirmedSafe --> ConfirmedSafe : submitFeedback() / recordFalsePositiveFeedback()
FalsePositive --> FalsePositive : submitFeedback() / recordFalsePositiveFeedback()

Open : SafetyEventStatus = OPEN
ConfirmedSafe : SafetyEventStatus = CONFIRMED_SAFE
FalsePositive : SafetyEventStatus = FALSE_POSITIVE
TimedOut : SafetyEventStatus = TIMED_OUT
EscalationRequested : SafetyEventStatus = ESCALATION_REQUESTED
EmergencyAlertSent : SafetyEventStatus = EMERGENCY_ALERT_SENT
@enduml
```

**Figure 2 — State Chart Diagram: Detected Impact, Safety Check, Alert, and Feedback**

**Brief Explanation:**

1. An event is only created when the fall-analysis service confirms a suspected impact, and it enters `OPEN` with a countdown deadline already armed.
2. The three user responses are mutually exclusive: each is guarded on the event not already carrying a response, so the first answer wins and cannot be overwritten.
3. When the countdown expires without an answer, the guard on `emergencyAutoAlert` decides the outcome — auto-alert escalates, otherwise the event settles as `TIMED_OUT`.
4. `TIMED_OUT` is deliberately not treated as safe; it records that no confirmation arrived rather than asserting the user is well.
5. `EMERGENCY_ALERT_SENT` is reached only from `ESCALATION_REQUESTED` and only once the family alert has actually been dispatched, so the state reflects delivery rather than intent.
6. False-positive feedback is a self-transition on the already-resolved states: it tunes future detection without rewriting the recorded outcome of a past event.

**State sources:**

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/SafetyEventStatus.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/SafetyEventType.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/FallDetectionService.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/SafetyCountdownJob.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/FamilyAlertSentHandler.java`

## 6. Business Rules Applied

| UC | Enforced business/security rules | Known boundary or gap |
| --- | --- | --- |
| `UC-ES-05` | Server event state and deterministic detector rules are canonical. Late/repeated signals must not reopen a resolved event or duplicate emergency alerts. | No additional gap recorded in the code-first baseline. |

## 7. Partial / Excluded Boundaries

- No extra release boundary beyond the code-first UC catalogue is introduced by this package.

## 8. Code and Test Evidence

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/FallDetectionService.java`
- `05_Development/CareBridgeMobileApp/lib/features/safety/screens/safety_monitoring_screen.dart`
- `05_Development/CareBridgeMobileApp/lib/features/safety/widgets/safety_countdown_sheet.dart`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/FallDetectionControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SuspectedFallDetectedHandlerTest.java`
- `05_Development/CareBridgeMobileApp/test/features/safety/fall_detection_sensor_service_test.dart`
