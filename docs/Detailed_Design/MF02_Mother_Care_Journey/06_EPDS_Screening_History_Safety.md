# MF-02 / Spec 06 — EPDS Screening, History and Safety Response

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-32 Manage EPDS Screening |
| Use Case Group | Mobile App |
| Platform | Mother Mobile App; Backend |
| Primary Actors | Mother |
| In Scope | Ten answers are processed on device; persisted score is non-diagnostic and urgent answers keep safety guidance visible |
| Explicitly Excluded | Clinical diagnosis or storing answer payload in family history |
| Implementation Trace | UI: EpdsScreen, EpdsHistoryDetailScreen; Controller: HealthMetricController; Service: HealthMetricServiceImpl, MetricObservationValidator; Repository: MaternalHealthMetricRepository; Entity: MaternalHealthMetric with EPDS_SCORE |

## 1. Tổng quan luồng chính (Main Flow Overview)

Ten answers are processed on device; persisted score is non-diagnostic and urgent answers keep safety guidance visible. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF02_06_EPDSScreeningHistoryandSafetyResponse_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "EpdsScreen" as UI1 <<UI>>
class "EpdsHistoryDetailScreen" as UI2 <<UI>>
class "HealthMetricController" as Controller1 <<Controller>> {
  - healthMetricService: IHealthMetricService
  + deleteMetric(metricId: UUID, principal: Principal): ResponseEntity<Void>
  + getMetricDetail(metricId: UUID, principal: Principal): ResponseEntity<ApiResponse<MetricDetailResponse>>
}
class "HealthMetricServiceImpl" as Service1 <<Service>> {
  - observationRepository: HealthObservationRepository
  - definitionRepository: MetricDefinitionRepository
  - journeyRepository: MotherJourneyRepository
  - auditService: AuditService
  - toDetailResponse(observation: HealthObservation, journeyId: UUID): MetricDetailResponse
  - toMetricResponse(observation: HealthObservation, journeyId: UUID, aiInsight: String, ...): MetricResponse
  + addMetric(userId: UUID, journeyId: UUID, request: AddMetricRequest): MetricResponse
  + deleteMetric(metricId: UUID, callerId: UUID): void
  + getCapabilities(journeyId: UUID, callerId: UUID): List<MetricCapabilityResponse>
}
class "MetricObservationValidator" as Service2 <<Service>> {
  + canonicalCode(type: MetricType): String
  + mergeAndNormalize(existingType: MetricType, existingPrimary: BigDecimal, existingSecondary: BigDecimal, ...): NormalizedObservation
  + normalize(request: AddMetricRequest, definition: MetricDefinition): NormalizedObservation
  - contextNumber(Map<String, context: Object>, key: String): BigDecimal
  - copyContext(Map<String, source: Object>): Map<String, Object>
}
interface "IHealthMetricService" as Service1Contract <<Service>>
interface "MaternalHealthMetricRepository" as Repository1
class "MaternalHealthMetric with EPDS_SCORE" as Entity1 <<Entity>>
interface "JpaRepository<MaternalHealthMetric, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
@enduml
```

**Figure 1 — Class Diagram: EPDS Screening, History and Safety Response**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF02_06_EPDSScreeningHistoryandSafetyResponse_SequenceDiagram
skinparam shadowing false

actor "Mother" as Actor
boundary ":EpdsScreen" as UI1
control ":JourneyMetricController" as Controller1
participant ":HealthMetricServiceImpl" as Service1 <<service>>
participant ":MaternalHealthMetricRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB

group UC-32 Manage EPDS Screening
  Actor -> UI1 : 1. startManageEpdsScreening()
  activate UI1
  UI1 -> Controller1 : 2. addMetric(EPDS_SCORE) / getMetricTrend(EPDS_SCORE)
  activate Controller1
  Controller1 -> Service1 : 3. addMetric(EPDS_SCORE) / getMetricTrend(EPDS_SCORE)
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

@enduml
```

**Figure 2 — Sequence Diagram: EPDS Screening, History and Safety Response Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-32 Manage EPDS Screening.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Ten answers are processed on device; persisted score is non-diagnostic and urgent answers keep safety guidance visible.
- The following remains outside this contract: Clinical diagnosis or storing answer payload in family history.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
