# MF-07 / Spec 02 — Emergency Call and Family Alert

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-51 Call Emergency Number 115; UC-52 Alert Family During Emergency; UC-53 View Emergency or Family Alert |
| Use Case Group | Mobile App |
| Platform | Mother and Family Mobile; Backend; Device Services |
| Primary Actors | Mother / Family |
| In Scope | CareBridge does not dispatch help or guarantee delivery |
| Explicitly Excluded | Automatic ambulance dispatch |
| Implementation Trace | UI: EmergencyMapScreen, EmergencyAlertDetailScreen, FamilyAlertDetailScreen; Controller: EmergencyController, FamilyAlertController; Service: EmergencyService, FamilyAlertServiceImpl; Repository: IEmergencySessionRepository; Entity: EmergencySession |

## 1. Tổng quan luồng chính (Main Flow Overview)

CareBridge does not dispatch help or guarantee delivery. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF07_02_EmergencyCallandFamilyAlert_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "EmergencyMapScreen" as UI1 <<UI>>
class "EmergencyAlertDetailScreen" as UI2 <<UI>>
class "FamilyAlertDetailScreen" as UI3 <<UI>>
class "EmergencyController" as Controller1 <<Controller>> {
  - emergencyService: IEmergencyService
  + getAlertDetail(id: UUID, principal: Principal): ResponseEntity<ApiResponse<FamilyAlertDetailResponse>>
  + getActive(principal: Principal): ResponseEntity<ApiResponse<EmergencySessionResponse>>
  + openFlow(request: OpenEmergencyRequest, principal: Principal): ResponseEntity<ApiResponse<EmergencySessionResponse>>
  + resolve(id: UUID, principal: Principal): ResponseEntity<ApiResponse<EmergencySessionResponse>>
}
class "FamilyAlertController" as Controller2 <<Controller>> {
  - familyAlertService: IFamilyAlertService
}
class "EmergencyService" as Service1 <<Service>> {
  - log: Logger
  - emergencySessionRepository: IEmergencySessionRepository
  - intakeSessionRepository: IIntakeSessionRepository
  - triageEscalationLinkRepository: TriageEmergencyEscalationLinkRepository
  + getAlertDetail(sessionId: UUID, callerId: UUID): FamilyAlertDetailResponse
  + getActiveSession(userId: UUID): EmergencySessionResponse
  + openFlow(request: OpenEmergencyRequest, userId: UUID): EmergencySessionResponse
  + openOrReuseFromTriage(intakeSessionId: UUID, userId: UUID): EmergencySessionResponse
  + resolveSession(sessionId: UUID, userId: UUID): EmergencySessionResponse
}
class "FamilyAlertServiceImpl" as Service2 <<Service>> {
  - notificationRepository: NotificationRecordRepository
  - auditService: AuditService
  + listFamilyAlerts(callerId: UUID, page: int, size: int): FamilyAlertListResponse
  - toDto(record: NotificationRecord): FamilyAlertItemDto
}
interface "IEmergencyService" as Service1Contract <<Service>>
interface "IFamilyAlertService" as Service2Contract <<Service>>
interface "IEmergencySessionRepository" as Repository1
class "EmergencySession" as Entity1 <<Entity>> {
  - id: UUID
  - userId: UUID
  - sourceEventId: UUID
  - status: EmergencyStatus
  - triggerSource: String
  - userLatitude: BigDecimal
  - userLongitude: BigDecimal
}
interface "JpaRepository<EmergencySession, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Device dialer and Firebase Cloud Messaging" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller2 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service2Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
Service2 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Emergency Call and Family Alert**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF07_02_EmergencyCallandFamilyAlert_SequenceDiagram
skinparam shadowing false

actor "Mother / Family" as Actor
boundary ":EmergencyMapScreen" as UI1
boundary ":EmergencyAlertDetailScreen" as UI2
control ":EmergencyController" as Controller1
participant ":EmergencyService" as Service1 <<service>>
participant ":IEmergencySessionRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":Device dialer" as External1 <<external system>>
participant ":Firebase Cloud Messaging" as External2 <<external system>>

group UC-51 Call Emergency Number 115
  Actor -> UI1 : 1. startCallEmergencyNumber115()
  activate UI1
  UI1 -> Controller1 : 2. openFlow() / resolve()
  activate Controller1
  Controller1 -> Service1 : 3. openFlow() / resolveSession()
  activate Service1
  alt [command is valid and actor is authorized]
    Service1 -> Repository1 : 4a. save(emergencySession)
    activate Repository1
    Repository1 -> DB : 4a-1. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 4a-2. persistedState
    deactivate DB
    Repository1 --> Service1 : 4a-3. savedEntity
    deactivate Repository1
    Service1 --> Controller1 : 4a-4. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4a-5. 200 OK / 201 Created
    deactivate Controller1
    UI1 -> External1 : 4a-6. call(115)
    activate External1
    External1 --> UI1 : 4a-7. deviceActionResult
    deactivate External1
    UI1 --> Actor : 4a-8. displayConfirmedState()
    deactivate UI1
  else [validation, authorization or state check fails]
    Service1 --> Controller1 : 4b. domainError
    deactivate Service1
    Controller1 --> UI1 : 4b-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI1 --> Actor : 4b-2. displayActionableError()
    deactivate UI1
  end
end

group UC-52 Alert Family During Emergency
  Actor -> UI1 : 5. startAlertFamilyDuringEmergency()
  activate UI1
  UI1 -> Controller1 : 6. openFlow() / getAlertDetail()
  activate Controller1
  Controller1 -> Service1 : 7. openFlow() / getAlertDetail()
  activate Service1
  alt [command is valid and actor is authorized]
    Service1 -> Repository1 : 8a. findActiveByUserId()
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
    Service1 ->> External2 : 8a-8. notifyCareGroupMembers()
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

group UC-53 View Emergency or Family Alert
  Actor -> UI2 : 9. startViewEmergencyOrFamilyAlert()
  activate UI2
  UI2 -> Controller1 : 10. getAlertDetail()
  activate Controller1
  Controller1 -> Service1 : 11. getAlertDetail()
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 12a. findActiveByUserId()
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
    UI2 --> Actor : 12a-6. displayViewEmergencyOrFamilyAlertResult()
    deactivate UI2
  else [request is invalid, forbidden or unavailable]
    Service1 --> Controller1 : 12b. domainError
    deactivate Service1
    Controller1 --> UI2 : 12b-1. 400 / 401 / 403 / 404
    deactivate Controller1
    UI2 --> Actor : 12b-2. displayActionableError()
    deactivate UI2
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Emergency Call and Family Alert Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-51 Call Emergency Number 115; UC-52 Alert Family During Emergency; UC-53 View Emergency or Family Alert.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- CareBridge does not dispatch help or guarantee delivery.
- The following remains outside this contract: Automatic ambulance dispatch.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
