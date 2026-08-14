# MF-09 / Spec 03 — Checklist Template Governance and Personal Runtime

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-63 Manage Personal Care Checklist; UC-89 Manage Checklist Templates |
| Use Case Group | Mobile App and Web App |
| Platform | Mother and authorized Family Mobile; Content Admin Web; Admin Web; Backend |
| Primary Actors | Mother / Family / Content Admin / System Admin |
| In Scope | Only approved active distributable versions materialize for eligible recipients |
| Explicitly Excluded | Checklist as prescription |
| Implementation Trace | UI: Checklist screens, ChecklistListPage, ChecklistFormPage, ChecklistDetailPage; Controller: AdminChecklistTemplateController, CurrentChecklistController, ChecklistHistoryController; Service: AdminChecklistTemplateServiceImpl, CurrentChecklistServiceImpl; Repository: ChecklistTemplateRepository, ChecklistInstanceRepository; Entity: ChecklistTemplate, ChecklistInstance, ChecklistTaskInstance |

## 1. Tổng quan luồng chính (Main Flow Overview)

Only approved active distributable versions materialize for eligible recipients. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF09_03_ChecklistTemplateGovernanceandPersonalRuntime_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "Checklist screens" as UI1 <<UI>>
class "ChecklistListPage" as UI2 <<UI>>
class "ChecklistFormPage" as UI3 <<UI>>
class "ChecklistDetailPage" as UI4 <<UI>>
class "AdminChecklistTemplateController" as Controller1 <<Controller>> {
  - adminChecklistTemplateService: AdminChecklistTemplateService
  + cloneVersionInLineage(lineageId: UUID, versionId: UUID, principal: Principal): ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>
  + archive(id: UUID, request: HideChecklistTemplateRequest, principal: Principal): ResponseEntity<ApiResponse<HideChecklistTemplateResponse>>
  + cloneVersion(id: UUID, principal: Principal): ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>
  + create(request: CreateChecklistTemplateRequest, principal: Principal): ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>
  + getById(id: UUID): ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>
  + getVersionHistory(id: UUID): ResponseEntity<ApiResponse<List<ChecklistTemplateVersionSnapshotResponse>>>
  + update(id: UUID, request: UpdateChecklistTemplateRequest, principal: Principal): ResponseEntity<ApiResponse<AdminChecklistTemplateDetailResponse>>
}
class "CurrentChecklistController" as Controller2 <<Controller>> {
  - checklistService: CurrentChecklistService
  - actionFacade: UnifiedTaskActionFacade
  + applyAction(taskId: UUID, request: TaskActionRequest, principal: Principal): CurrentChecklistActionResponse
}
class "ChecklistHistoryController" as Controller3 <<Controller>> {
  - historyService: ChecklistHistoryService
}
class "AdminChecklistTemplateServiceImpl" as Service1 <<Service>> {
  - checklistTemplateRepository: ChecklistTemplateRepository
  - checklistItemRepository: ChecklistItemRepository
  - contentMapper: ContentMapper
  - auditService: AuditService
  + cloneVersionInLineage(lineageId: UUID, id: UUID, adminUserId: UUID): AdminChecklistTemplateDetailResponse
  - normalizeTemplateType(requestedType: ChecklistTemplateType): ChecklistTemplateType
  - toRecipientScope(roles: Set<ChecklistRecipientRole>): ChecklistRecipientScope
  + archive(id: UUID, request: HideChecklistTemplateRequest, adminUserId: UUID): HideChecklistTemplateResponse
  + cloneVersion(id: UUID, adminUserId: UUID): AdminChecklistTemplateDetailResponse
}
class "CurrentChecklistServiceImpl" as Service2 <<Service>> {
  - unifiedTodayTaskService: UnifiedTodayTaskService
  + getCurrentTasks(actorUserId: UUID, date: LocalDate, timezoneHeader: String): CurrentChecklistResponse
  - map(items: java.util.List<TodayTaskItemResponse>): java.util.List<CurrentChecklistTaskResponse>
}
interface "AdminChecklistTemplateService" as Service1Contract <<Service>>
interface "CurrentChecklistService" as Service2Contract <<Service>>
interface "ChecklistTemplateRepository" as Repository1 {
  + findByTemplateVersionId(templateVersionId: UUID): Optional<ChecklistTemplate>
  + findByTemplateLineageId(templateLineageId: UUID): List<ChecklistTemplate>
  + findAllByTemplateVersionIdIn(templateVersionIds: Collection<UUID>): List<ChecklistTemplate>
  + acquirePreconceptionSequenceCohortLock(): void
  + findByStage(stage: ContentStage): List<ChecklistTemplate>
  + findByStatusOrderByUpdatedAtDesc(status: ChecklistTemplateStatus): List<ChecklistTemplate>
}
interface "ChecklistInstanceRepository" as Repository2 {
  + findByDistributionKey(distributionKey: String): Optional<ChecklistInstance>
  + findByRecipientUserId(recipientUserId: UUID): List<ChecklistInstance>
  + existsByTemplateLineageId(templateLineageId: UUID): boolean
  + findByRecipientUserIdAndHistoricalAtIsNull(recipientUserId: UUID): List<ChecklistInstance>
  + findByContextOwnerUserIdAndRecipientRoleAndOriginAndHistoricalAtIsNull(contextOwnerUserId: UUID, recipientRole: ChecklistRecipientRole, origin: ChecklistOrigin): List<ChecklistInstance>
  + findAllByRecipientUserIdAndRecipientRoleAndCareGroupIdAndCareContextTypeAndCareContextIdAndTemplateVersionId(recipientUserId: UUID, recipientRole: ChecklistRecipientRole, careGroupId: UUID, ...): List<ChecklistInstance>
}
class "ChecklistTemplate" as Entity1 <<Entity>> {
  - id: UUID
  - name: String
  - templateLineageId: UUID
  - templateVersionId: UUID
  - substageId: UUID
  - sequencePosition: Integer
  - recipientScope: ChecklistRecipientScope
}
class "ChecklistInstance" as Entity2 <<Entity>> {
  - id: UUID
  - distributionKey: String
  - keyVersion: String
  - templateLineageId: UUID
  - templateVersionId: UUID
  - recipientUserId: UUID
  - recipientRole: ChecklistRecipientRole
}
class "ChecklistTaskInstance" as Entity3 <<Entity>> {
  - id: UUID
  - checklistInstanceId: UUID
  - templateVersionId: UUID
  - templateItemVersionId: UUID
  - taskKey: String
  - keyVersion: String
  - titleSnapshot: String
}
interface "JpaRepository<ChecklistTemplate, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<ChecklistInstance, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller2 : invokes API
UI1 ..> Controller3 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller1 : invokes API
UI4 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service2Contract : delegates
Controller3 --> Service2Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository2 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Repository2 ..> Entity2 : maps
Repository2 ..> DB : persists
Entity1 "1" -- "0..*" Entity2 : materializes
Entity2 "1" *-- "1..*" Entity3 : tasks
@enduml
```

**Figure 1 — Class Diagram: Checklist Template Governance and Personal Runtime**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF09_03_ChecklistTemplateGovernanceandPersonalRuntime_SequenceDiagram
skinparam shadowing false

actor "Mother / Family / Content Admin / System Admin" as Actor
boundary ":Checklist screens" as UI1
boundary ":ChecklistListPage" as UI2
control ":CurrentChecklistController" as Controller1
control ":AdminChecklistTemplateController" as Controller2
participant ":CurrentChecklistServiceImpl" as Service1 <<service>>
participant ":AdminChecklistTemplateServiceImpl" as Service2 <<service>>
participant ":ChecklistInstanceRepository" as Repository1 <<repository>>
participant ":ChecklistTemplateRepository" as Repository2 <<repository>>
database "PostgreSQL" as DB

group UC-63 Manage Personal Care Checklist
  Actor -> UI1 : 1. startManagePersonalCareChecklist()
  activate UI1
  UI1 -> Controller1 : 2. getCurrentTasks()
  activate Controller1
  Controller1 -> Service1 : 3. getCurrentTasks()
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 4a. findByRecipientUserIdAndHistoricalAtIsNull()
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
    UI1 --> Actor : 4a-6. displayManagePersonalCareChecklistResult()
    deactivate UI1
  else [request is invalid, forbidden or unavailable]
    Service1 --> Controller1 : 4b. domainError
    deactivate Service1
    Controller1 --> UI1 : 4b-1. 400 / 401 / 403 / 404
    deactivate Controller1
    UI1 --> Actor : 4b-2. displayActionableError()
    deactivate UI1
  end
end

group UC-89 Manage Checklist Templates
  Actor -> UI2 : 5. startManageChecklistTemplates()
  activate UI2
  UI2 -> Controller2 : 6. list() / create() / update() / cloneVersion()
  activate Controller2
  Controller2 -> Service2 : 7. list() / create() / update() / cloneVersion()
  activate Service2
  alt [selected action is view or list]
    Service2 -> Repository2 : 8a. findAllOptionalByStatus()
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
    Service2 -> Repository2 : 8b. findAllOptionalByStatus()
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

**Figure 2 — Sequence Diagram: Checklist Template Governance and Personal Runtime Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-63 Manage Personal Care Checklist; UC-89 Manage Checklist Templates.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Only approved active distributable versions materialize for eligible recipients.
- The following remains outside this contract: Checklist as prescription.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
