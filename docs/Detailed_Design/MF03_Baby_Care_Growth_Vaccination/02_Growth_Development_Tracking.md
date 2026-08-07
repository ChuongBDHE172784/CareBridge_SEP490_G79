# MF-03 / Spec 02 — Baby Growth and Development Tracking

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-36 Manage Development Milestones; UC-37 Manage Baby Growth |
| Use Case Group | Mobile App |
| Platform | Mother Mobile App; Backend |
| Primary Actors | Mother |
| In Scope | Caregiver observations and reference charts are non-diagnostic |
| Explicitly Excluded | Automated developmental assessment |
| Implementation Trace | UI: Growth chart and milestone screens; Controller: GrowthMeasurementController, MilestoneController; Service: GrowthServiceImpl, MilestoneServiceImpl; Repository: GrowthMeasurementRepository, DevelopmentMilestoneRepository; Entity: GrowthMeasurement, DevelopmentMilestone |

## 1. Tổng quan luồng chính (Main Flow Overview)

Caregiver observations and reference charts are non-diagnostic. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF03_02_BabyGrowthandDevelopmentTracking_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "Growth chart and milestone screens" as UI1 <<UI>>
class "GrowthMeasurementController" as Controller1 <<Controller>> {
  - growthService: IGrowthService
  + addGrowthMeasurement(babyId: UUID, request: AddGrowthMeasurementRequest, principal: Principal): ResponseEntity<ApiResponse<GrowthMeasurementResponse>>
  + deleteGrowthMeasurement(babyId: UUID, growthMeasurementId: UUID, principal: Principal): ResponseEntity<Void>
  + updateGrowthMeasurement(babyId: UUID, growthMeasurementId: UUID, request: UpdateGrowthMeasurementRequest, ...): ResponseEntity<ApiResponse<GrowthMeasurementResponse>>
}
class "MilestoneController" as Controller2 <<Controller>> {
  - milestoneService: IMilestoneService
  + listMilestones(babyId: UUID, principal: Principal): ApiResponse<List<MilestoneResponse>>
  + addMilestone(babyId: UUID, request: AddMilestoneRequest, principal: Principal): ApiResponse<MilestoneResponse>
  + deleteMilestone(babyId: UUID, milestoneId: UUID, principal: Principal): ApiResponse<Void>
  + updateMilestone(babyId: UUID, milestoneId: UUID, request: UpdateDevelopmentMilestoneRequest, ...): ApiResponse<MilestoneResponse>
}
class "GrowthServiceImpl" as Service1 <<Service>> {
  - babyProfileRepository: BabyProfileRepository
  - growthMeasurementRepository: GrowthMeasurementRepository
  - auditService: AuditService
  - babyAccessPolicy: BabyAccessPolicy
  + addGrowthMeasurement(userId: UUID, babyId: UUID, request: AddGrowthMeasurementRequest): GrowthMeasurementResponse
  + deleteGrowthMeasurement(userId: UUID, babyId: UUID, growthMeasurementId: UUID): void
  + getGrowthChart(userId: UUID, babyId: UUID): GrowthChartResponse
  + getGrowthMeasurementHistory(userId: UUID, babyId: UUID, pageable: Pageable): Page<GrowthMeasurementHistoryItem>
  + updateGrowthMeasurement(userId: UUID, babyId: UUID, growthMeasurementId: UUID, ...): GrowthMeasurementResponse
}
class "MilestoneServiceImpl" as Service2 <<Service>> {
  - milestoneRepository: DevelopmentMilestoneRepository
  - babyProfileRepository: BabyProfileRepository
  - babyAccessPolicy: BabyAccessPolicy
  - auditService: AuditService
  + listMilestones(babyId: UUID, callerId: UUID): List<MilestoneResponse>
  - ensurePathBabyMatches(babyId: UUID, milestone: DevelopmentMilestone): void
  - findActualBabyForMilestone(milestone: DevelopmentMilestone): BabyProfile
  - parseUpdateStatus(request: UpdateDevelopmentMilestoneRequest): MilestoneAchievementStatus
  + addMilestone(userId: UUID, babyId: UUID, request: AddMilestoneRequest): MilestoneResponse
}
interface "IGrowthService" as Service1Contract <<Service>>
interface "IMilestoneService" as Service2Contract <<Service>>
interface "GrowthMeasurementRepository" as Repository1
interface "DevelopmentMilestoneRepository" as Repository2 {
  + findByBabyIdOrderByAchievedDateDesc(babyId: UUID): List<DevelopmentMilestone>
  + findByMilestoneIdAndRecordStatus(milestoneId: UUID, recordStatus: MilestoneRecordStatus): Optional<DevelopmentMilestone>
}
class "GrowthMeasurement" as Entity1 <<Entity>> {
  - growthMeasurementId: UUID
  - babyId: UUID
  - careSubjectId: UUID
  - measuredDate: LocalDate
  - weightKg: BigDecimal
  - heightCm: BigDecimal
  - headCircumferenceCm: BigDecimal
}
class "DevelopmentMilestone" as Entity2 <<Entity>> {
  - milestoneId: UUID
  - babyId: UUID
  - careSubjectId: UUID
  - milestoneType: String
  - achievedDate: LocalDate
  - note: String
  - sourceType: String
}
interface "JpaRepository<GrowthMeasurement, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<DevelopmentMilestone, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller1 : invokes API
UI1 ..> Controller2 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service2Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository2 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Repository2 ..> Entity2 : maps
Repository2 ..> DB : persists
@enduml
```

**Figure 1 — Class Diagram: Baby Growth and Development Tracking**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF03_02_BabyGrowthandDevelopmentTracking_SequenceDiagram
skinparam shadowing false

actor "Mother" as Actor
boundary ":milestone screens" as UI1
boundary ":growth chart screens" as UI2
control ":MilestoneController" as Controller1
control ":GrowthMeasurementController" as Controller2
participant ":MilestoneServiceImpl" as Service1 <<service>>
participant ":GrowthServiceImpl" as Service2 <<service>>
participant ":DevelopmentMilestoneRepository" as Repository1 <<repository>>
participant ":GrowthMeasurementRepository" as Repository2 <<repository>>
database "PostgreSQL" as DB

group UC-36 Manage Development Milestones
  Actor -> UI1 : 1. startManageDevelopmentMilestones()
  activate UI1
  UI1 -> Controller1 : 2. listMilestones() / addMilestone() / updateMilestone() / deleteMilestone()
  activate Controller1
  Controller1 -> Service1 : 3. listMilestones() / addMilestone() / updateMilestone() / deleteMilestone()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByBabyIdOrderByAchievedDateDesc()
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
    Service1 -> Repository1 : 4b. findByBabyIdOrderByAchievedDateDesc()
    activate Repository1
    Repository1 -> DB : 4b-1. SELECT
    activate DB
    DB --> Repository1 : 4b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 4b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 4b-4. save() / delete()
    activate Repository1
    Repository1 -> DB : 4b-5. INSERT / UPDATE / DELETE
    activate DB
    DB --> Repository1 : 4b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 4b-7. persistedEntity
    deactivate Repository1
    Service1 --> Controller1 : 4b-8. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-9. 200 OK / 201 Created
    deactivate Controller1
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

group UC-37 Manage Baby Growth
  Actor -> UI2 : 5. startManageBabyGrowth()
  activate UI2
  UI2 -> Controller2 : 6. getGrowthMeasurementHistory() / addGrowthMeasurement() / updateGrowthMeasurement() / deleteGrowthMeasurement()
  activate Controller2
  Controller2 -> Service2 : 7. getGrowthMeasurementHistory() / addGrowthMeasurement() / updateGrowthMeasurement() / deleteGrowthMeasurement()
  activate Service2
  alt [selected action is view or list]
    Service2 -> Repository2 : 8a. findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc()
    activate Repository2
    Repository2 -> DB : 8a-1. SELECT / DELETE
    activate DB
    DB --> Repository2 : 8a-2. queryResult
    deactivate DB
    Repository2 --> Service2 : 8a-3. domainRecords
    deactivate Repository2
    Service2 --> Controller2 : 8a-4. resultDTO
    deactivate Service2
    Controller2 --> UI2 : 8a-5. 200 OK
    deactivate Controller2
    UI2 --> Actor : 8a-6. displayCurrentState()
    deactivate UI2
  else [selected action creates, updates, archives or deletes]
    Service2 -> Repository2 : 8b. findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc()
    activate Repository2
    Repository2 -> DB : 8b-1. SELECT / DELETE
    activate DB
    DB --> Repository2 : 8b-2. currentState
    deactivate DB
    Repository2 --> Service2 : 8b-3. scopedEntity
    deactivate Repository2
    Service2 -> Repository2 : 8b-4. save() / delete()
    activate Repository2
    Repository2 -> DB : 8b-5. INSERT / UPDATE / DELETE
    activate DB
    DB --> Repository2 : 8b-6. persistedState
    deactivate DB
    Repository2 --> Service2 : 8b-7. persistedEntity
    deactivate Repository2
    Service2 --> Controller2 : 8b-8. resultDTO
    deactivate Service2
    Controller2 --> UI2 : 8b-9. 200 OK / 201 Created
    deactivate Controller2
    UI2 --> Actor : 8b-10. displayConfirmedState()
    deactivate UI2
  else [request is invalid, forbidden, not found or conflicting]
    Service2 --> Controller2 : 8c. domainError
    deactivate Service2
    Controller2 --> UI2 : 8c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller2
    UI2 --> Actor : 8c-2. displayActionableError()
    deactivate UI2
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Baby Growth and Development Tracking Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-36 Manage Development Milestones; UC-37 Manage Baby Growth.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Caregiver observations and reference charts are non-diagnostic.
- The following remains outside this contract: Automated developmental assessment.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
