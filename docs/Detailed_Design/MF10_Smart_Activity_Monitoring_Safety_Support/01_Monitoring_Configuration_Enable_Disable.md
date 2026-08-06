# MF-10 / Spec 01 — Safety Monitoring Configuration and Session Control

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-64 Manage Safety Monitoring Settings |
| Use Case Group | Mobile App |
| Platform | Mother Mobile; Backend; Phone IMU |
| Primary Actors | Mother |
| In Scope | Monitoring is off by default and requires consent plus device permission |
| Explicitly Excluded | Wearables and connected medical devices |
| Implementation Trace | UI: Safety monitoring settings screen; Controller: SafetyConfigController; Service: SafetyConfigService; Repository: ISafetyConfigRepository; Entity: SafetyMonitoringConfig |

## 1. Tổng quan luồng chính (Main Flow Overview)

Monitoring is off by default and requires consent plus device permission. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF10_01_SafetyMonitoringConfigurationandSessionControl_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "Safety monitoring settings screen" as UI1 <<UI>>
class "SafetyConfigController" as Controller1 <<Controller>> {
  - safetyConfigService: ISafetyConfigService
  + configure(request: SafetyConfigRequest, principal: Principal): ResponseEntity<ApiResponse<SafetyConfigResponse>>
  + getConfig(principal: Principal): ResponseEntity<ApiResponse<SafetyConfigResponse>>
}
class "SafetyConfigService" as Service1 <<Service>> {
  - configRepository: ISafetyConfigRepository
  - eventPublisher: ApplicationEventPublisher
  + configure(request: SafetyConfigRequest, userId: UUID): SafetyConfigResponse
  + getConfig(userId: UUID): SafetyConfigResponse
  - toResponse(config: SafetyMonitoringConfig): SafetyConfigResponse
}
interface "ISafetyConfigService" as Service1Contract <<Service>>
interface "ISafetyConfigRepository" as Repository1 {
  + findByUserId(userId: UUID): Optional<SafetyMonitoringConfig>
}
class "SafetyMonitoringConfig" as Entity1 <<Entity>> {
  - id: UUID
  - userId: UUID
  - fallDetectionEnabled: boolean
  - sensitivityLevel: SensitivityLevel
  - emergencyAutoAlert: boolean
  - countdownSeconds: int
  - sensorPermissionGranted: boolean
}
interface "JpaRepository<SafetyMonitoringConfig, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Phone accelerometer and gyroscope" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Safety Monitoring Configuration and Session Control**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF10_01_SafetyMonitoringConfigurationandSessionControl_SequenceDiagram
skinparam shadowing false

actor "Mother" as Actor
boundary ":Safety monitoring settings screen" as UI1
control ":SafetyConfigController" as Controller1
participant ":SafetyConfigService" as Service1 <<service>>
participant ":ISafetyConfigRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":Phone accelerometer and gyroscope" as External1 <<external system>>

group UC-64 Manage Safety Monitoring Settings
  Actor -> UI1 : 1. startManageSafetyMonitoringSettings()
  activate UI1
  UI1 -> Controller1 : 2. getConfig() / configure()
  activate Controller1
  Controller1 -> Service1 : 3. getConfig() / configure()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByUserId()
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
    Service1 -> Repository1 : 4b. findByUserId()
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
    UI1 -> External1 : 4b-10. requestSensorPermission()
    activate External1
    External1 --> UI1 : 4b-11. deviceActionResult
    deactivate External1
    UI1 --> Actor : 4b-12. displayConfirmedState()
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

@enduml
```

**Figure 2 — Sequence Diagram: Safety Monitoring Configuration and Session Control Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-64 Manage Safety Monitoring Settings.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Monitoring is off by default and requires consent plus device permission.
- The following remains outside this contract: Wearables and connected medical devices.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
