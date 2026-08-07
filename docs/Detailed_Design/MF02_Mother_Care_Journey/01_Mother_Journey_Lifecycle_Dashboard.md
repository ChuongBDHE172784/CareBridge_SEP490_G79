# MF-02 / Spec 01 — Mother Journey Lifecycle and Dashboard

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-20 Manage Mother Journey; UC-33 View Personalized Care Recommendations |
| Use Case Group | Mobile App |
| Platform | Mother Mobile App; Backend |
| Primary Actors | Mother |
| In Scope | Preconception, pregnancy and postpartum journey state |
| Explicitly Excluded | Placeholder journey-update route as a separate flow |
| Implementation Trace | UI: MotherJourneyScreen, MotherHomeScreen, recommendation screens; Controller: JourneyController; Service: JourneyServiceImpl, RecommendationService; Repository: MotherJourneyRepository; Entity: MotherJourney |

## 1. Tổng quan luồng chính (Main Flow Overview)

Preconception, pregnancy and postpartum journey state. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF02_01_MotherJourneyLifecycleandDashboard_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "MotherJourneyScreen" as UI1 <<UI>>
class "MotherHomeScreen" as UI2 <<UI>>
class "recommendation screens" as UI3 <<UI>>
class "JourneyController" as Controller1 <<Controller>> {
  - journeyService: IJourneyService
  - journeyTransitionService: IJourneyTransitionService
  - journeyTimelineService: IJourneyTimelineService
  + createJourney(request: CreateJourneyRequest, principal: Principal): ResponseEntity<ApiResponse<CreateJourneyResponse>>
  + getDashboard(principal: Principal): ResponseEntity<ApiResponse<JourneyDashboardResponse>>
  + recordPregnancyOutcome(journeyId: UUID, request: RecordPregnancyOutcomeRequest, principal: Principal): ResponseEntity<ApiResponse<PregnancyOutcomeResponse>>
  + updateJourney(journeyId: UUID, request: UpdateJourneyRequest, principal: Principal): ResponseEntity<ApiResponse<JourneyResponse>>
}
class "JourneyServiceImpl" as Service1 <<Service>> {
  - journeyRepository: MotherJourneyRepository
  - userRepository: UserRepository
  - auditService: AuditService
  - careGroupMemberRepository: CareGroupMemberRepository
  - ensureMotherCareSubject(ownerUserId: UUID): UUID
  - resolveDashboardStatus(type: JourneyType): DashboardStatus
  + createJourney(request: CreateJourneyRequest, callerId: UUID): CreateJourneyResponse
  + getDashboard(userId: UUID): JourneyDashboardResponse
  + updateJourney(ownerId: UUID, journeyId: UUID, request: UpdateJourneyRequest): JourneyResponse
}
class "RecommendationService" as Service2 <<Service>> {
  - journeyRepository: MotherJourneyRepository
  - transitionRepository: MotherJourneyTransitionRepository
  - outcomeEvidenceRepository: PregnancyOutcomeEvidenceRepository
  - userRepository: UserRepository
  - currentPregnancyEpochVersion(journey: MotherJourney): long
  + getCatalog(): RecommendationTagCatalogResponse
  + getContent(ownerUserId: UUID, limit: int): RecommendationContentResponse
  + getProfile(ownerUserId: UUID): RecommendationProfileResponse
  + markStageReview(ownerUserId: UUID, journeyId: UUID, stage: JourneyType): void
}
interface "IJourneyService" as Service1Contract <<Service>>
interface "RecommendationConsentCleanup" as Service2Contract <<Service>>
interface "MotherJourneyRepository" as Repository1 {
  + existsByOwnerUserIdAndJourneyTypeAndStatus(ownerUserId: UUID, type: JourneyType, status: JourneyStatus): boolean
  + existsByOwnerUserIdAndStatusAndJourneyTypeIn(ownerUserId: UUID, status: JourneyStatus, journeyTypes: List<JourneyType>): boolean
  + findByOwnerUserIdAndStatusAndJourneyTypeIn(ownerUserId: UUID, status: JourneyStatus, journeyTypes: List<JourneyType>): Optional<MotherJourney>
  + findFirstByOwnerUserIdAndJourneyTypeAndStatusOrderByCreatedAtDesc(ownerUserId: UUID, journeyType: JourneyType, status: JourneyStatus): Optional<MotherJourney>
  + findByOwnerUserIdAndJourneyTypeAndStatusOrderByCreatedAtAsc(ownerUserId: UUID, type: JourneyType, status: JourneyStatus): List<MotherJourney>
  + countByOwnerUserIdAndStatus(ownerUserId: UUID, status: JourneyStatus): long
}
class "MotherJourney" as Entity1 <<Entity>> {
  - id: UUID
  - ownerUserId: UUID
  - careSubjectId: UUID
  - journeyType: JourneyType
  - startDate: LocalDate
  - lastMenstrualDate: LocalDate
  - estimatedDueDate: LocalDate
}
interface "JpaRepository<MotherJourney, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Gemini recommendation service when configured" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
Service2 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Mother Journey Lifecycle and Dashboard**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF02_01_MotherJourneyLifecycleandDashboard_SequenceDiagram
skinparam shadowing false

actor "Mother" as Actor
boundary ":MotherJourneyScreen" as UI1
boundary ":MotherHomeScreen" as UI2
control ":JourneyController" as Controller1
control ":RecommendationController" as Controller2
participant ":JourneyServiceImpl" as Service1 <<service>>
participant ":RecommendationService" as Service2 <<service>>
participant ":MotherJourneyRepository" as Repository1 <<repository>>
participant ":ContentRepository" as Repository2 <<repository>>
database "PostgreSQL" as DB
participant ":Gemini recommendation service" as External1 <<external system>>

group UC-20 Manage Mother Journey
  Actor -> UI1 : 1. startManageMotherJourney()
  activate UI1
  UI1 -> Controller1 : 2. createJourney() / updateJourney() / getHistory()
  activate Controller1
  Controller1 -> Service1 : 3. createJourney() / updateJourney() / getHistory()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findCanonical()
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
    Service1 -> Repository1 : 4b. findCanonical()
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

group UC-33 View Personalized Care Recommendations
  Actor -> UI2 : 5. startViewPersonalizedCareRecommendations()
  activate UI2
  UI2 -> Controller2 : 6. getContent(ownerUserId, limit)
  activate Controller2
  Controller2 -> Service2 : 7. getContent(ownerUserId, limit)
  activate Service2
  alt [request is authorized and input is valid]
    Service2 -> Repository2 : 8a. findApprovedTargetedArticlesForRecommendation()
    activate Repository2
    Repository2 -> DB : 8a-1. SELECT
    activate DB
    DB --> Repository2 : 8a-2. queryResult
    deactivate DB
    Repository2 --> Service2 : 8a-3. domainRecords
    deactivate Repository2
    Service2 -> External1 : 8a-4. generateRecommendations()
    activate External1
    External1 --> Service2 : 8a-5. integrationResult
    deactivate External1
    Service2 --> Controller2 : 8a-6. resultDTO
    deactivate Service2
    Controller2 --> UI2 : 8a-7. 200 OK
    deactivate Controller2
    UI2 --> Actor : 8a-8. displayViewPersonalizedCareRecommendationsResult()
    deactivate UI2
  else [request is invalid, forbidden or unavailable]
    Service2 --> Controller2 : 8b. domainError
    deactivate Service2
    Controller2 --> UI2 : 8b-1. 400 / 401 / 403 / 404
    deactivate Controller2
    UI2 --> Actor : 8b-2. displayActionableError()
    deactivate UI2
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Mother Journey Lifecycle and Dashboard Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-20 Manage Mother Journey; UC-33 View Personalized Care Recommendations.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Preconception, pregnancy and postpartum journey state.
- The following remains outside this contract: Placeholder journey-update route as a separate flow.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
