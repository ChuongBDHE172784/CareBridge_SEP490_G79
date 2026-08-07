# MF-08 / Spec 03 — Cooperative Care Task Management

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-59 Manage Cooperative Care Tasks |
| Use Case Group | Mobile App |
| Platform | Mother and Family Mobile; Backend |
| Primary Actors | Mother / Family |
| In Scope | Owner and assignee actions differ and membership is rechecked |
| Explicitly Excluded | Clinical treatment plan |
| Implementation Trace | UI: AssignedTasksScreen, FamilyTaskDetailScreen, UpdateFamilyTaskScreen; Controller: CareGroupController; Service: CareTaskServiceImpl; Repository: CareTaskRepository; Entity: CareTask |

## 1. Tổng quan luồng chính (Main Flow Overview)

Owner and assignee actions differ and membership is rechecked. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF08_03_CooperativeCareTaskManagement_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "AssignedTasksScreen" as UI1 <<UI>>
class "FamilyTaskDetailScreen" as UI2 <<UI>>
class "UpdateFamilyTaskScreen" as UI3 <<UI>>
class "CareGroupController" as Controller1 <<Controller>> {
  - careGroupService: ICareGroupService
  - careTaskService: ICareTaskService
  + createCareGroup(request: CreateCareGroupRequest, principal: Principal): ResponseEntity<ApiResponse<CreateCareGroupResponse>>
  + deleteCareGroup(groupId: UUID, principal: Principal): ResponseEntity<ApiResponse<Void>>
  + leaveCareGroup(groupId: UUID, principal: Principal): ResponseEntity<ApiResponse<LeaveCareGroupResponse>>
  + updateTaskStatus(groupId: UUID, taskId: UUID, request: UpdateTaskStatusRequest, ...): ResponseEntity<ApiResponse<UpdateTaskStatusResponse>>
  + assignTask(groupId: UUID, request: AssignFamilyTaskRequest, principal: Principal): ResponseEntity<ApiResponse<AssignFamilyTaskResponse>>
  + cancelTask(groupId: UUID, taskId: UUID, principal: Principal): ResponseEntity<ApiResponse<CancelFamilyTaskResponse>>
  + getFamilyPermission(groupId: UUID, memberId: UUID, principal: Principal): ResponseEntity<ApiResponse<FamilyPermissionResponse>>
}
class "CareTaskServiceImpl" as Service1 <<Service>> {
  - groupRepository: CareGroupRepository
  - memberRepository: CareGroupMemberRepository
  - taskRepository: CareTaskRepository
  - authorizationPolicy: CareGroupAuthorizationPolicy
  - auditCareTaskStatus(task: CareTask, callerId: UUID, previousStatus: CareTaskStatus, ...): void
  + assignFamilyTask(groupId: UUID, request: AssignFamilyTaskRequest, callerId: UUID): AssignFamilyTaskResponse
  + cancelFamilyTask(groupId: UUID, taskId: UUID, callerId: UUID): CancelFamilyTaskResponse
  + updateFamilyTask(groupId: UUID, taskId: UUID, request: UpdateFamilyTaskRequest, ...): UpdateFamilyTaskResponse
  + updateTaskStatus(groupId: UUID, taskId: UUID, request: UpdateTaskStatusRequest, ...): UpdateTaskStatusResponse
}
interface "ICareTaskService" as Service1Contract <<Service>>
interface "CareTaskRepository" as Repository1 {
  + findByCareGroupId(careGroupId: UUID): List<CareTask>
  + findByAssignedTo(assignedTo: UUID): List<CareTask>
  + deleteByCareGroupId(careGroupId: UUID): void
  + findByIdAndCareGroupId(id: UUID, careGroupId: UUID): Optional<CareTask>
  + findByCareGroupIdAndDueAtBetween(careGroupId: UUID, rangeStart: Instant, rangeEnd: Instant): List<CareTask>
}
class "CareTask" as Entity1 <<Entity>> {
  - id: UUID
  - careGroupId: UUID
  - assignedBy: UUID
  - assignedTo: UUID
  - title: String
  - description: String
  - dueAt: Instant
}
interface "JpaRepository<CareTask, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "NotificationRecord service" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Cooperative Care Task Management**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF08_03_CooperativeCareTaskManagement_SequenceDiagram
skinparam shadowing false

actor "Mother / Family" as Actor
boundary ":AssignedTasksScreen" as UI1
control ":CareGroupController" as Controller1
participant ":CareTaskServiceImpl" as Service1 <<service>>
participant ":CareTaskRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":NotificationRecord service" as External1 <<external system>>

group UC-59 Manage Cooperative Care Tasks
  Actor -> UI1 : 1. startManageCooperativeCareTasks()
  activate UI1
  UI1 -> Controller1 : 2. listTasks() / assignTask() / updateTask() / cancelTask() / updateTaskStatus()
  activate Controller1
  Controller1 -> Service1 : 3. listTasks() / assignFamilyTask() / updateFamilyTask() / cancelFamilyTask() / updateTaskStatus()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByCareGroupId() / findByAssignedTo()
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
    Service1 -> Repository1 : 4b. findByCareGroupId() / findByAssignedTo()
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
    Service1 ->> External1 : 4b-8. notifyTaskAssignmentOrCompletion()
    Service1 --> Controller1 : 4b-9. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-10. 200 OK / 201 Created
    deactivate Controller1
    UI1 --> Actor : 4b-11. displayConfirmedState()
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

**Figure 2 — Sequence Diagram: Cooperative Care Task Management Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-59 Manage Cooperative Care Tasks.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Owner and assignee actions differ and membership is rechecked.
- The following remains outside this contract: Clinical treatment plan.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
