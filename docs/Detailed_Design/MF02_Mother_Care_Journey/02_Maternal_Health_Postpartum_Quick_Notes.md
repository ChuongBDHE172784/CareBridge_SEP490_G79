# MF-02 / Spec 02 — Maternal Metrics, Postpartum Logs and Quick Notes

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-21 Manage Maternal Health Metrics; UC-22 Manage Postpartum Logs; UC-31 Record Quick Health Notes |
| Use Case Group | Mobile App |
| Platform | Mother Mobile App; Backend |
| Primary Actors | Mother |
| In Scope | BMI, hydration, mood and fetal-movement notes use current health metric storage |
| Explicitly Excluded | Connected-device observations |
| Implementation Trace | UI: Health metric screens, postpartum screens, MotherHomeScreen quick-note actions; Controller: HealthMetricController, PostpartumLogController; Service: HealthMetricServiceImpl, PostpartumLogServiceImpl; Repository: MaternalHealthMetricRepository, PostpartumLogRepository; Entity: MaternalHealthMetric, PostpartumLog |

## 1. Tổng quan luồng chính (Main Flow Overview)

BMI, hydration, mood and fetal-movement notes use current health metric storage. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF02_02_MaternalMetricsPostpartumLogsandQuickNotes_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "Health metric screens" as UI1 <<UI>>
class "postpartum screens" as UI2 <<UI>>
class "MotherHomeScreen quick-note actions" as UI3 <<UI>>
class "HealthMetricController" as Controller1 <<Controller>> {
  - healthMetricService: IHealthMetricService
  + deleteMetric(metricId: UUID, principal: Principal): ResponseEntity<Void>
  + getMetricDetail(metricId: UUID, principal: Principal): ResponseEntity<ApiResponse<MetricDetailResponse>>
}
class "PostpartumLogController" as Controller2 <<Controller>> {
  - postpartumLogService: IPostpartumLogService
  + addLog(journeyId: UUID, request: AddPostpartumLogRequest, principal: Principal): ResponseEntity<ApiResponse<PostpartumLogResponse>>
  + deleteLog(logId: UUID, principal: Principal): ResponseEntity<Void>
  + getLogDetail(logId: UUID, principal: Principal): ResponseEntity<ApiResponse<PostpartumLogResponse>>
  + updateLog(logId: UUID, request: UpdatePostpartumLogRequest, principal: Principal): ResponseEntity<ApiResponse<PostpartumLogResponse>>
}
class "HealthMetricServiceImpl" as Service1 <<Service>> {
  - observationRepository: HealthObservationRepository
  - definitionRepository: MetricDefinitionRepository
  - journeyRepository: MotherJourneyRepository
  - auditService: AuditService
  + addMetric(userId: UUID, journeyId: UUID, request: AddMetricRequest): MetricResponse
  + deleteMetric(metricId: UUID, callerId: UUID): void
  + getMetricDetail(metricId: UUID, callerId: UUID): MetricDetailResponse
  + getMetricTrend(userId: UUID, journeyId: UUID, metricType: MetricType, ...): MetricTrendResponse
  + updateMetric(userId: UUID, journeyId: UUID, metricId: UUID, ...): MetricResponse
}
class "PostpartumLogServiceImpl" as Service2 <<Service>> {
  - logRepository: PostpartumLogRepository
  - journeyRepository: MotherJourneyRepository
  - transitionRepository: MotherJourneyTransitionRepository
  - auditService: AuditService
  + listLogs(journeyId: UUID, callerId: UUID, page: int, ...): Page<PostpartumLogResponse>
  - requireActivePostpartumOwner(journeyId: UUID, callerId: UUID, forUpdate: boolean, ...): MotherJourney
  - requirePostpartumReadOwner(journeyId: UUID, callerId: UUID, notFoundCode: String): MotherJourney
  + addLog(userId: UUID, journeyId: UUID, request: AddPostpartumLogRequest): PostpartumLogResponse
  + deleteLog(logId: UUID, callerId: UUID): void
}
interface "IHealthMetricService" as Service1Contract <<Service>>
interface "IPostpartumLogService" as Service2Contract <<Service>>
interface "MaternalHealthMetricRepository" as Repository1
interface "PostpartumLogRepository" as Repository2
class "MaternalHealthMetric" as Entity1 <<Entity>> {
  - id: UUID
  - journeyId: UUID
  - careSubjectId: UUID
  - metricType: MetricType
  - valueNumeric: BigDecimal
  - valueSecondary: BigDecimal
  - unit: String
}
class "PostpartumLog" as Entity2 <<Entity>> {
  - id: UUID
  - journeyId: UUID
  - submissionId: UUID
  - logDate: LocalDate
  - painLevel: Short
  - bleedingLevel: BleedingLevel
  - moodLevel: Short
}
interface "JpaRepository<MaternalHealthMetric, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<PostpartumLog, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller2 : invokes API
UI3 ..> Controller1 : invokes API
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

**Figure 1 — Class Diagram: Maternal Metrics, Postpartum Logs and Quick Notes**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF02_02_MaternalMetricsPostpartumLogsandQuickNotes_SequenceDiagram
skinparam shadowing false

actor "Mother" as Actor
boundary ":Health metric screens" as UI1
boundary ":postpartum screens" as UI2
boundary ":MotherHomeScreen quick-note actions" as UI3
control ":JourneyMetricController" as Controller1
control ":PostpartumLogController" as Controller2
participant ":HealthMetricServiceImpl" as Service1 <<service>>
participant ":PostpartumLogServiceImpl" as Service2 <<service>>
participant ":MaternalHealthMetricRepository" as Repository1 <<repository>>
participant ":PostpartumLogRepository" as Repository2 <<repository>>
database "PostgreSQL" as DB

group UC-21 Manage Maternal Health Metrics
  Actor -> UI1 : 1. startManageMaternalHealthMetrics()
  activate UI1
  UI1 -> Controller1 : 2. addMetric() / updateMetric() / getMetricTrend()
  activate Controller1
  Controller1 -> Service1 : 3. addMetric() / updateMetric() / getMetricTrend()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc()
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
    Service1 -> Repository1 : 4b. findByJourneyIdAndMetricTypeAndStatusAndMeasuredAtBetweenOrderByMeasuredAtAsc()
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

group UC-22 Manage Postpartum Logs
  Actor -> UI2 : 5. startManagePostpartumLogs()
  activate UI2
  UI2 -> Controller2 : 6. listLogs() / addLog() / updateLog() / deleteLog()
  activate Controller2
  Controller2 -> Service2 : 7. listLogs() / addLog() / updateLog() / deleteLog()
  activate Service2
  alt [selected action is view or list]
    Service2 -> Repository2 : 8a. findByJourneyIdAndStatus()
    activate Repository2
    Repository2 -> DB : 8a-1. SELECT
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
    Service2 -> Repository2 : 8b. findByJourneyIdAndStatus()
    activate Repository2
    Repository2 -> DB : 8b-1. SELECT
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

group UC-31 Record Quick Health Notes
  Actor -> UI3 : 9. startRecordQuickHealthNotes()
  activate UI3
  UI3 -> Controller1 : 10. addMetric(quickNote)
  activate Controller1
  Controller1 -> Service1 : 11. addMetric(quickNote)
  activate Service1
  alt [command is valid and actor is authorized]
    Service1 -> Repository1 : 12a. save(metric)
    activate Repository1
    Repository1 -> DB : 12a-1. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 12a-2. persistedState
    deactivate DB
    Repository1 --> Service1 : 12a-3. savedEntity
    deactivate Repository1
    Service1 --> Controller1 : 12a-4. resultDTO
    deactivate Service1
    Controller1 --> UI3 : 12a-5. 200 OK / 201 Created
    deactivate Controller1
    UI3 --> Actor : 12a-6. displayConfirmedState()
    deactivate UI3
  else [validation, authorization or state check fails]
    Service1 --> Controller1 : 12b. domainError
    deactivate Service1
    Controller1 --> UI3 : 12b-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI3 --> Actor : 12b-2. displayActionableError()
    deactivate UI3
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Maternal Metrics, Postpartum Logs and Quick Notes Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-21 Manage Maternal Health Metrics; UC-22 Manage Postpartum Logs; UC-31 Record Quick Health Notes.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- BMI, hydration, mood and fetal-movement notes use current health metric storage.
- The following remains outside this contract: Connected-device observations.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
