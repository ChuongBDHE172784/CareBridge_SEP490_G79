# MF-09 / Spec 04 — Pregnancy Exercise Content and Posture Configuration

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-90 Manage Pregnancy Exercise Content; UC-91 Manage Exercise Posture Configuration |
| Use Case Group | Web App |
| Platform | Content Admin Web; Admin Web; Backend |
| Primary Actors | Content Admin / System Admin |
| In Scope | Content Admin owns exercise content; System Admin owns active posture configuration |
| Explicitly Excluded | Medical exercise clearance |
| Implementation Trace | UI: PregnancyExerciseListPage, CreatePregnancyExercisePage, EditPregnancyExercisePage, PostureConfigListPage; Controller: AdminExerciseController, AdminPostureConfigController; Service: AdminExerciseServiceImpl, PostureConfigServiceImpl; Repository: ExerciseRepository, PostureAnalysisConfigRepository; Entity: PregnancyExercise, PostureAnalysisConfig |

## 1. Tổng quan luồng chính (Main Flow Overview)

Content Admin owns exercise content; System Admin owns active posture configuration. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF09_04_PregnancyExerciseContentandPostureConfiguration_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "PregnancyExerciseListPage" as UI1 <<UI>>
class "CreatePregnancyExercisePage" as UI2 <<UI>>
class "EditPregnancyExercisePage" as UI3 <<UI>>
class "PostureConfigListPage" as UI4 <<UI>>
class "AdminExerciseController" as Controller1 <<Controller>> {
  - adminExerciseService: IAdminExerciseService
  + activateExercise(exerciseId: UUID, principal: Principal): ResponseEntity<ApiResponse<AdminExerciseResponse>>
  + createExercise(request: CreateExerciseRequest, principal: Principal): ResponseEntity<ApiResponse<AdminExerciseResponse>>
  + disableExercise(exerciseId: UUID, principal: Principal): ResponseEntity<ApiResponse<AdminExerciseResponse>>
  + getExercise(exerciseId: UUID): ResponseEntity<ApiResponse<AdminExerciseResponse>>
  + updateExercise(exerciseId: UUID, request: UpdateExerciseRequest, principal: Principal): ResponseEntity<ApiResponse<AdminExerciseResponse>>
}
class "AdminPostureConfigController" as Controller2 <<Controller>> {
  - postureConfigService: IPostureConfigService
  + activateVersion(postureConfigId: UUID, principal: Principal): ResponseEntity<ApiResponse<AdminPostureConfigResponse>>
  + createConfig(request: CreatePostureConfigRequest, principal: Principal): ResponseEntity<ApiResponse<AdminPostureConfigResponse>>
  + createNewVersion(exerciseId: UUID, request: UpdatePostureConfigRequest, principal: Principal): ResponseEntity<ApiResponse<AdminPostureConfigResponse>>
  + listVersions(exerciseId: UUID): ResponseEntity<ApiResponse<List<AdminPostureConfigResponse>>>
}
class "AdminExerciseServiceImpl" as Service1 <<Service>> {
  - exerciseRepository: ExerciseRepository
  - exerciseMapper: ExerciseMapper
  - auditService: AuditService
  + activate(exerciseId: UUID, adminUserId: UUID): AdminExerciseResponse
  + create(request: CreateExerciseRequest, adminUserId: UUID): AdminExerciseResponse
  + disable(exerciseId: UUID, adminUserId: UUID): AdminExerciseResponse
  + getById(exerciseId: UUID): AdminExerciseResponse
  + list(status: ExerciseStatus, trimester: TrimesterScope, difficulty: DifficultyLevel, ...): PaginatedResponse<AdminExerciseResponse>
}
class "PostureConfigServiceImpl" as Service2 <<Service>> {
  - exerciseRepository: ExerciseRepository
  - postureConfigRepository: PostureAnalysisConfigRepository
  - auditService: AuditService
  + getActiveConfig(exerciseId: UUID): ApiResponse<PostureConfigResponse>
  + activateVersion(postureConfigId: UUID, adminUserId: UUID): ApiResponse<AdminPostureConfigResponse>
  + createConfig(request: CreatePostureConfigRequest, adminUserId: UUID): ApiResponse<AdminPostureConfigResponse>
  + createNewVersion(exerciseId: UUID, request: UpdatePostureConfigRequest, adminUserId: UUID): ApiResponse<AdminPostureConfigResponse>
  + listVersions(exerciseId: UUID): ApiResponse<List<AdminPostureConfigResponse>>
}
interface "IAdminExerciseService" as Service1Contract <<Service>>
interface "IPostureConfigService" as Service2Contract <<Service>>
interface "ExerciseRepository" as Repository1 {
  + findByExerciseIdAndStatus(exerciseId: UUID, status: ExerciseStatus): Optional<PregnancyExercise>
}
interface "PostureAnalysisConfigRepository" as Repository2 {
  + findByExerciseIdAndStatus(exerciseId: UUID, status: String): Optional<PostureAnalysisConfig>
  + findAllByExerciseIdOrderByEffectiveFromDesc(exerciseId: UUID): List<PostureAnalysisConfig>
  + existsByExerciseId(exerciseId: UUID): boolean
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
class "PostureAnalysisConfig" as Entity2 <<Entity>> {
  - postureConfigId: UUID
  - exerciseId: UUID
  - configuredBy: UUID
  - analysisMode: String
  - ruleOrModelVersion: String
  - confidenceThreshold: BigDecimal
  - feedbackLevel: String
}
interface "JpaRepository<PregnancyExercise, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<PostureAnalysisConfig, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Posture AI model registry" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller1 : invokes API
UI4 ..> Controller2 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service2Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository2 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Repository2 ..> Entity2 : maps
Repository2 ..> DB : persists
Service1 ..> External : invokes when required
Service2 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Pregnancy Exercise Content and Posture Configuration**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF09_04_PregnancyExerciseContentandPostureConfiguration_SequenceDiagram
skinparam shadowing false

actor "Content Admin / System Admin" as Actor
boundary ":PregnancyExerciseListPage" as UI1
boundary ":PostureConfigListPage" as UI2
control ":AdminExerciseController" as Controller1
control ":AdminPostureConfigController" as Controller2
participant ":AdminExerciseServiceImpl" as Service1 <<service>>
participant ":PostureConfigServiceImpl" as Service2 <<service>>
participant ":ExerciseRepository" as Repository1 <<repository>>
participant ":PostureAnalysisConfigRepository" as Repository2 <<repository>>
database "PostgreSQL" as DB
participant ":File storage" as External1 <<external system>>
participant ":Posture AI model registry" as External2 <<external system>>

group UC-90 Manage Pregnancy Exercise Content
  Actor -> UI1 : 1. startManagePregnancyExerciseContent()
  activate UI1
  UI1 -> Controller1 : 2. listExercises() / createExercise() / updateExercise() / activateExercise()
  activate Controller1
  Controller1 -> Service1 : 3. list() / create() / update() / activate()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findAllByFilters()
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
    Service1 -> Repository1 : 4b. findAllByFilters()
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
    Service1 -> External1 : 4b-8. uploadExerciseMedia()
    activate External1
    External1 --> Service1 : 4b-9. integrationResult
    deactivate External1
    Service1 --> Controller1 : 4b-10. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-11. 200 OK / 201 Created
    deactivate Controller1
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

group UC-91 Manage Exercise Posture Configuration
  Actor -> UI2 : 5. startManageExercisePostureConfiguration()
  activate UI2
  UI2 -> Controller2 : 6. listVersions() / createConfig() / activateVersion()
  activate Controller2
  Controller2 -> Service2 : 7. listVersions() / createConfig() / activateVersion()
  activate Service2
  alt [selected action is view or list]
    Service2 -> Repository2 : 8a. findAllByExerciseIdOrderByEffectiveFromDesc()
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
    Service2 -> Repository2 : 8b. findAllByExerciseIdOrderByEffectiveFromDesc()
    activate Repository2
    Repository2 -> DB : 8b-1. SELECT
    activate DB
    DB --> Repository2 : 8b-2. currentState
    deactivate DB
    Repository2 --> Service2 : 8b-3. scopedEntity
    deactivate Repository2
    Service2 -> Repository2 : 8b-4. save()
    activate Repository2
    Repository2 -> DB : 8b-5. INSERT / UPDATE
    activate DB
    DB --> Repository2 : 8b-6. persistedState
    deactivate DB
    Repository2 --> Service2 : 8b-7. persistedEntity
    deactivate Repository2
    Service2 -> External2 : 8b-8. validateModelVersion()
    activate External2
    External2 --> Service2 : 8b-9. integrationResult
    deactivate External2
    Service2 --> Controller2 : 8b-10. resultDTO
    deactivate Service2
    Controller2 --> UI2 : 8b-11. 200 OK / 201 Created
    deactivate Controller2
    UI2 --> Actor : 8b-12. displayConfirmedState()
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

**Figure 2 — Sequence Diagram: Pregnancy Exercise Content and Posture Configuration Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-90 Manage Pregnancy Exercise Content; UC-91 Manage Exercise Posture Configuration.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Content Admin owns exercise content; System Admin owns active posture configuration.
- The following remains outside this contract: Medical exercise clearance.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
