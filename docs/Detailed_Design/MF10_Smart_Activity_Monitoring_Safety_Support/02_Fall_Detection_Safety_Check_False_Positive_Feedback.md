# MF-10 / Spec 02 — Suspected Fall Detection, Safety Check and Escalation

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-65 Respond to Suspected Fall or Impact; UC-66 Send Safety Emergency Alert; UC-67 Review Safety Events and Report False Positive; UC-68 Open Emergency Support from Safety Alert |
| Use Case Group | Mobile App |
| Platform | Mother Mobile; Backend; Phone IMU |
| Primary Actors | Mother / Phone Motion Sensors |
| In Scope | Detection creates a suspected event; first valid response wins and alerts do not guarantee assistance |
| Explicitly Excluded | Certified fall detection or emergency dispatch |
| Implementation Trace | UI: Safety countdown, event history and emergency support screens; Controller: FallDetectionController; Service: FallDetectionService; Repository: ISafetyEventRepository; Entity: SafetyEvent |

## 1. Tổng quan luồng chính (Main Flow Overview)

Detection creates a suspected event; first valid response wins and alerts do not guarantee assistance. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF10_02_SuspectedFallDetectionSafetyCheckandEscalation_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "Safety countdown" as UI1 <<UI>>
class "event history and emergency support screens" as UI2 <<UI>>
class "FallDetectionController" as Controller1 <<Controller>> {
  - fallDetectionService: IFallDetectionService
  - safetyConfigService: ISafetyConfigService
  + reportFalsePositive(eventId: UUID, request: SafetyEventActionRequest, principal: Principal): ResponseEntity<ApiResponse<SafetyEventResponse>>
  + sendEmergencyAlert(eventId: UUID, principal: Principal): ResponseEntity<ApiResponse<Void>>
  + confirmSafetyCheck(eventId: UUID, request: SafetyEventActionRequest, principal: Principal): ResponseEntity<ApiResponse<SafetyEventResponse>>
  + processImuData(request: ImuDataRequest, principal: Principal): ResponseEntity<ApiResponse<SafetyEventResponse>>
  + disable(principal: Principal): ResponseEntity<ApiResponse<Void>>
  + enable(principal: Principal): ResponseEntity<ApiResponse<ImuMonitoringSessionResponse>>
}
class "FallDetectionService" as Service1 <<Service>> {
  - log: Logger
  - imuSessionRepository: IImuMonitoringSessionRepository
  - safetyEventRepository: ISafetyEventRepository
  - algorithmService: IFallDetectionAlgorithmService
  + reportFalsePositive(userId: UUID, eventId: UUID, note: String): SafetyEventResponse
  + sendEmergencyAlert(userId: UUID, eventId: UUID): void
  - toEventResponse(event: SafetyEvent): SafetyEventResponse
  + confirmSafetyCheck(userId: UUID, eventId: UUID, note: String): SafetyEventResponse
  + listSafetyEvents(userId: UUID, pageable: org.springframework.data.domain.Pageable): List<SafetyEventResponse>
}
interface "IFallDetectionService" as Service1Contract <<Service>>
interface "ISafetyEventRepository" as Repository1 {
  + findByUserIdOrderByDetectedAtDesc(userId: UUID, pageable: Pageable): Page<SafetyEvent>
  + findByIdAndUserId(id: UUID, userId: UUID): Optional<SafetyEvent>
  + findByImuSessionIdAndSignalKey(imuSessionId: UUID, signalKey: String): Optional<SafetyEvent>
  + findTop100ByStatusAndResponseTypeIsNullAndCountdownDeadlineAtLessThanEqualOrderByCountdownDeadlineAtAsc(status: SafetyEventStatus, deadline: Instant): List<SafetyEvent>
}
class "SafetyEvent" as Entity1 <<Entity>> {
  - id: UUID
  - userId: UUID
  - imuSessionId: UUID
  - eventType: SafetyEventType
  - magnitude: BigDecimal
  - userLatitude: BigDecimal
  - userLongitude: BigDecimal
}
interface "JpaRepository<SafetyEvent, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Phone IMU and Firebase Cloud Messaging" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Suspected Fall Detection, Safety Check and Escalation**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF10_02_SuspectedFallDetectionSafetyCheckandEscalation_SequenceDiagram
skinparam shadowing false

actor "Mother / Phone Motion Sensors" as Actor
boundary ":Safety countdown" as UI1
boundary ":event history screen" as UI2
boundary ":emergency support screen" as UI3
control ":FallDetectionController" as Controller1
participant ":FallDetectionService" as Service1 <<service>>
participant ":ISafetyEventRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":Phone IMU" as External1 <<external system>>
participant ":Firebase Cloud Messaging" as External2 <<external system>>
participant ":Emergency support module" as External3 <<external system>>

group UC-65 Respond to Suspected Fall or Impact
  Actor -> UI1 : 1. startRespondToSuspectedFallOrImpact()
  activate UI1
  UI1 -> Controller1 : 2. processImuData() / confirmSafetyCheck()
  activate Controller1
  Controller1 -> Service1 : 3. processImuData() / confirmSafetyCheck()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByIdAndUserId()
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
    UI1 --> Actor : 4a-6. displayCurrentState()
    deactivate UI1
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository1 : 4b. findByIdAndUserId()
    activate Repository1
    Repository1 -> DB : 4b-1. SELECT
    activate DB
    DB --> Repository1 : 4b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 4b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 4b-4. save()
    activate Repository1
    Repository1 -> DB : 4b-5. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 4b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 4b-7. persistedEntity
    deactivate Repository1
    Service1 --> Controller1 : 4b-8. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-9. 200 OK / 201 Created
    deactivate Controller1
    External1 ->> UI1 : 4b-10. streamMotionSamples()
    UI1 --> Actor : 4b-10. displayConfirmedState()
    deactivate UI1
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 4c. domainError
    deactivate Service1
    Controller1 --> UI1 : 4c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI1 --> Actor : 4c-2. displayActionableError()
    deactivate UI1
  end
end

group UC-66 Send Safety Emergency Alert
  Actor -> UI1 : 5. startSendSafetyEmergencyAlert()
  activate UI1
  UI1 -> Controller1 : 6. sendEmergencyAlert(eventId)
  activate Controller1
  Controller1 -> Service1 : 7. sendEmergencyAlert(eventId)
  activate Service1
  alt [command is valid and actor is authorized]
    Service1 -> Repository1 : 8a. findByIdAndUserId()
    activate Repository1
    Repository1 -> DB : 8a-1. SELECT
    activate DB
    DB --> Repository1 : 8a-2. currentState
    deactivate DB
    Repository1 --> Service1 : 8a-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 8a-4. save()
    activate Repository1
    Repository1 -> DB : 8a-5. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 8a-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 8a-7. savedEntity
    deactivate Repository1
    Service1 ->> External2 : 8a-8. notifyEmergencyContacts()
    Service1 --> Controller1 : 8a-9. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 8a-10. 200 OK / 201 Created
    deactivate Controller1
    UI1 --> Actor : 8a-11. displayConfirmedState()
    deactivate UI1
  else [validation, authorization or state check fails]
    Service1 --> Controller1 : 8b. domainError
    deactivate Service1
    Controller1 --> UI1 : 8b-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI1 --> Actor : 8b-2. displayActionableError()
    deactivate UI1
  end
end

group UC-67 Review Safety Events and Report False Positive
  Actor -> UI2 : 9. startReviewSafetyEventsAndReportFalsePositive()
  activate UI2
  UI2 -> Controller1 : 10. listEvents() / reportFalsePositive()
  activate Controller1
  Controller1 -> Service1 : 11. listSafetyEvents() / reportFalsePositive()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 12a. findByUserIdOrderByDetectedAtDesc()
    activate Repository1
    Repository1 -> DB : 12a-1. SELECT
    activate DB
    DB --> Repository1 : 12a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 12a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 12a-4. resultDTO
    deactivate Service1
    Controller1 --> UI2 : 12a-5. 200 OK
    deactivate Controller1
    UI2 --> Actor : 12a-6. displayCurrentState()
    deactivate UI2
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository1 : 12b. findByUserIdOrderByDetectedAtDesc()
    activate Repository1
    Repository1 -> DB : 12b-1. SELECT
    activate DB
    DB --> Repository1 : 12b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 12b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 12b-4. save()
    activate Repository1
    Repository1 -> DB : 12b-5. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 12b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 12b-7. persistedEntity
    deactivate Repository1
    Service1 --> Controller1 : 12b-8. resultDTO
    deactivate Service1
    Controller1 --> UI2 : 12b-9. 200 OK / 201 Created
    deactivate Controller1
    UI2 --> Actor : 12b-10. displayConfirmedState()
    deactivate UI2
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 12c. domainError
    deactivate Service1
    Controller1 --> UI2 : 12c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI2 --> Actor : 12c-2. displayActionableError()
    deactivate UI2
  end
end

group UC-68 Open Emergency Support from Safety Alert
  Actor -> UI3 : 13. startOpenEmergencySupportFromSafetyAlert()
  activate UI3
  UI3 -> Controller1 : 14. listEvents() / confirmSafetyCheck()
  activate Controller1
  Controller1 -> Service1 : 15. listSafetyEvents() / confirmSafetyCheck()
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 16a. findByIdAndUserId()
    activate Repository1
    Repository1 -> DB : 16a-1. SELECT
    activate DB
    DB --> Repository1 : 16a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 16a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 16a-4. resultDTO
    deactivate Service1
    Controller1 --> UI3 : 16a-5. 200 OK
    deactivate Controller1
    UI3 -> External3 : 16a-6. openEmergencySupport()
    activate External3
    External3 --> UI3 : 16a-7. deviceActionResult
    deactivate External3
    UI3 --> Actor : 16a-8. displayOpenEmergencySupportFromSafetyAlertResult()
    deactivate UI3
  else [request is invalid, forbidden or unavailable]
    Service1 --> Controller1 : 16b. domainError
    deactivate Service1
    Controller1 --> UI3 : 16b-1. 400 / 401 / 403 / 404
    deactivate Controller1
    UI3 --> Actor : 16b-2. displayActionableError()
    deactivate UI3
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Suspected Fall Detection, Safety Check and Escalation Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-65 Respond to Suspected Fall or Impact; UC-66 Send Safety Emergency Alert; UC-67 Review Safety Events and Report False Positive; UC-68 Open Emergency Support from Safety Alert.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Detection creates a suspected event; first valid response wins and alerts do not guarantee assistance.
- The following remains outside this contract: Certified fall detection or emergency dispatch.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
