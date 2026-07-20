# MF-10 / Spec 03 — Family Care Task Assignment & Status Update

| Field | Value |
| --- | --- |
| Feature | MF-10 — Family Sync & Cooperative Care |
| Use Cases Covered | UC-99 Create, Update or Cancel Family Care Task, UC-100 Update Assigned Task Status |
| Primary Actor(s) | Mother (assigns), Family Member (executes) |
| Platform | Mother Mobile App, Family Mobile App |
| Main Flow Summary | A Mother assigns a care task to an eligible care-group member with a due time; the assignee progresses the task through an explicit finite-state machine (open → in progress → done, with a "needs support" branch) enforced server-side, not by free-form status strings. |
| Grounding (source code) | `family/entity/CareTask.java`, `family/entity/CareTaskStatus.java` (with `canTransitionTo()` guard, ADR-FAM-030/ADR-FAM-005), `family/controller/CareGroupController.java` (`/{groupId}/tasks`, `/{groupId}/tasks/{taskId}/status`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là luồng cộng tác trực tiếp nhất của MF-10. Mother tạo `CareTask` gán cho một
`assignedTo` hợp lệ trong `CareGroup` (UC-99: create/update/cancel do Mother thực hiện).
Family Member được giao cập nhật trạng thái thực thi của chính task đó (UC-100) — điểm
đáng chú ý về grounding: `family.entity.CareTaskStatus` có hẳn một **FSM tường minh trong
code** (`canTransitionTo()`), khác với entity `reminder.entity.CareTask` (dùng nội bộ cho
"Today Tasks" ở MF-09, chỉ có OPEN/COMPLETED/CANCELLED đơn giản hơn) — hai entity trùng
tên nhưng phục vụ hai mục đích khác nhau; spec này dùng đúng entity `family` vì đó là nơi
UC-99/100 thật sự được implement (`CareGroupController`/`CareTaskServiceImpl`).

## 2. Class Diagram

```plantuml
@startuml MF10_03_CareTask_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class CareGroup {
  + id: UUID
}

class CareTask {
  + id: UUID
  + careGroupId: UUID
  + assignedBy: UUID
  + assignedTo: UUID
  + title: String
  + description: String
  + dueAt: Instant
  + status: CareTaskStatus
  + completedAt: Instant
}

enum CareTaskStatus {
  OPEN
  IN_PROGRESS
  DONE
  CANCELLED
  NEEDS_SUPPORT
  + canTransitionTo(target): boolean
}

class AssignFamilyTaskRequest {
  + assignedTo: UUID
  + title: String
  + description: String
  + dueAt: Instant
}

class UpdateTaskStatusRequest {
  + status: CareTaskStatus
}

class CareGroupController {
  - careTaskService: ICareTaskService
  + assignTask(groupId, AssignFamilyTaskRequest): ResponseEntity
  + updateTask(groupId, taskId, UpdateFamilyTaskRequest): ResponseEntity
  + cancelTask(groupId, taskId): ResponseEntity
  + updateStatus(groupId, taskId, UpdateTaskStatusRequest): ResponseEntity
  + taskDetail(groupId, taskId): ResponseEntity
}

interface ICareTaskService <<interface>> {
  + assign(assignerId: UUID, groupId: UUID, request): CareTask
  + cancel(actorId: UUID, groupId: UUID, taskId: UUID): CareTask
  + updateStatus(assigneeId: UUID, taskId: UUID, newStatus: CareTaskStatus): CareTask
}

class CareTaskServiceImpl implements ICareTaskService {
  - careTaskRepository: CareTaskRepository
  - careGroupMemberRepository: CareGroupMemberRepository
  - auditService: AuditService
}

CareGroup "1" *-- "0..*" CareTask : has
CareTask --> CareTaskStatus
CareGroupController --> ICareTaskService : uses
CareTaskServiceImpl --> CareTaskStatus : validates via canTransitionTo()
CareTaskServiceImpl --> AuditService : emits CARE_TASK_ASSIGNED / STATUS_UPDATED / CANCELLED

@enduml
```

**Hình 1 — Class Diagram: Family Care Task với FSM tường minh trong `CareTaskStatus`**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF10_03_CareTask_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
actor "Family Member" as F
participant "CareGroupController" as Controller
participant "CareTaskServiceImpl" as Service
participant "CareGroupRepository" as GroupRepo
participant "CareGroupMemberRepository" as MemberRepo
participant "CareTaskRepository" as TaskRepo
participant "CareGroupAuthorizationPolicy" as AuthPolicy
participant "FcmService" as FcmSvc
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-99 Create Family Care Task ==
M -> Controller : 1. POST /api/v1/care-groups/{groupId}/tasks\n{assigneeMemberId, title, description, dueAt}
activate Controller
Controller -> Service : 2. assignFamilyTask(groupId, request, callerId)
activate Service
Service -> GroupRepo : 3. findById(groupId)
activate GroupRepo
GroupRepo -> DB : 4. SELECT * FROM care_groups WHERE id=?
activate DB
DB --> GroupRepo : 5. group row (404 FAM-005 nếu không có)
deactivate DB
GroupRepo --> Service : 6. CareGroup
deactivate GroupRepo
Service -> AuthPolicy : 7. canAssignTasks(groupId, callerId) [chỉ OWNER]
activate AuthPolicy
AuthPolicy --> Service : 8. boolean (403 FAM-031 nếu không phải owner)
deactivate AuthPolicy
Service -> MemberRepo : 9. findByCareGroupIdAndUserId(groupId, assigneeMemberId)\n[assignee phải là thành viên ACCEPTED]
activate MemberRepo
MemberRepo -> DB : 10. SELECT * FROM care_group_members\nWHERE care_group_id=? AND user_id=?
activate DB
DB --> MemberRepo : 11. member row (409 FAM-030 nếu không có/chưa ACCEPTED)
deactivate DB
MemberRepo --> Service : 12. CareGroupMember
deactivate MemberRepo
Service -> Service : 13. kiểm tra dueAt phải ở tương lai (400 FAM-032 nếu không)
Service -> TaskRepo : 14. save(CareTask{status=OPEN, assignedBy=callerId,\nassignedTo=assigneeMemberId})
activate TaskRepo
TaskRepo -> DB : 15. INSERT INTO care_tasks ...
activate DB
DB --> TaskRepo : 16. saved
deactivate DB
TaskRepo --> Service : 17. CareTask
deactivate TaskRepo
Service -> Service : 18. publishEvent(FamilyTaskAssigned)
Service -> Audit : 19. log(CARE_TASK_ASSIGNED, callerId,\n"CareTask", taskId, "Assigned to "+assigneeMemberId)
activate Audit
Audit --> Service : 20. void
deactivate Audit
Service -> FcmSvc : 21. sendToToken(assigneeDeviceTokens, "New Task Assigned", ...)\n[best-effort — lỗi KHÔNG rollback transaction]
activate FcmSvc
FcmSvc --> Service : 22. void
deactivate FcmSvc
Service --> Controller : 23. AssignFamilyTaskResponse{status=OPEN}
deactivate Service
Controller --> M : 24. HTTP 201 Created
deactivate Controller

== UC-99 Update Family Care Task (content) ==
M -> Controller : 25. PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}\n{title?, description?, dueAt?, assigneeMemberId?}
activate Controller
Controller -> Service : 26. updateFamilyTask(groupId, taskId, request, callerId)
activate Service
Service -> AuthPolicy : 27. canUpdateTask(groupId, callerId) [chỉ OWNER]
activate AuthPolicy
AuthPolicy --> Service : 28. boolean (403 FAM-072 nếu không phải owner)
deactivate AuthPolicy
Service -> TaskRepo : 29. findByIdAndCareGroupId(taskId, groupId)
activate TaskRepo
TaskRepo -> DB : 30. SELECT * FROM care_tasks WHERE id=? AND care_group_id=?
activate DB
DB --> TaskRepo : 31. task row (404 FAM-033 nếu không có)
deactivate DB
TaskRepo --> Service : 32. CareTask
deactivate TaskRepo
Service -> Service : 33. kiểm tra task đang OPEN/IN_PROGRESS\n(409 FAM-073 nếu DONE/CANCELLED/NEEDS_SUPPORT)
Service -> Service : 34. áp dụng field non-null; validate dueAt tương lai (FAM-075)\nvà assigneeMemberId mới phải ACCEPTED (FAM-074)
Service -> TaskRepo : 35. save(task{...})
activate TaskRepo
TaskRepo -> DB : 36. UPDATE care_tasks\nSET title=?, description=?, due_at=?, assigned_to=?
activate DB
DB --> TaskRepo : 37. updated
deactivate DB
TaskRepo --> Service : 38. CareTask
deactivate TaskRepo
Service -> Audit : 39. log(CARE_TASK_UPDATED, callerId, "CareTask", taskId, changedFields)
activate Audit
Audit --> Service : 40. void
deactivate Audit
Service -> Service : 41. publishEvent(CareTaskUpdated)
Service --> Controller : 42. UpdateFamilyTaskResponse
deactivate Service
Controller --> M : 43. HTTP 200 OK
deactivate Controller

== UC-100 Update Assigned Task Status ==
F -> Controller : 44. PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status\n{status=IN_PROGRESS}
activate Controller
Controller -> Service : 45. updateTaskStatus(groupId, taskId, request, callerId)
activate Service
Service -> GroupRepo : 46. findById(groupId)
activate GroupRepo
GroupRepo -> DB : 47. SELECT * FROM care_groups WHERE id=?
activate DB
DB --> GroupRepo : 48. group row
deactivate DB
GroupRepo --> Service : 49. CareGroup
deactivate GroupRepo
Service -> TaskRepo : 50. findByIdAndCareGroupId(taskId, groupId)
activate TaskRepo
TaskRepo -> DB : 51. SELECT * FROM care_tasks WHERE id=? AND care_group_id=?
activate DB
DB --> TaskRepo : 52. task row (404 FAM-033 nếu không có)
deactivate DB
TaskRepo --> Service : 53. CareTask{status=OPEN, assignedTo}
deactivate TaskRepo
Service -> Service : 54. kiểm tra callerId == task.assignedTo (403 FAM-034 nếu không)
Service -> Service : 55. parse status request → CareTaskStatus (400 FAM-035 nếu không hợp lệ)
alt 56. current.canTransitionTo(requested) == true [OPEN → IN_PROGRESS: hợp lệ]
  Service -> TaskRepo : 56. save(task{status=IN_PROGRESS})
  activate TaskRepo
  TaskRepo -> DB : 57. UPDATE care_tasks SET status='IN_PROGRESS'
  activate DB
  DB --> TaskRepo : 58. updated
  deactivate DB
  TaskRepo --> Service : 59. CareTask
  deactivate TaskRepo
  Service -> Audit : 60. log(CARE_TASK_STATUS_UPDATED, callerId, "CareTask",\ntaskId, "OPEN -> IN_PROGRESS")
  activate Audit
  Audit --> Service : 61. void
  deactivate Audit
  Service -> FcmSvc : 62. sendToToken(assignerDeviceTokens, "Task status updated", ...)\n[best-effort, gửi cho assignedBy — không rollback nếu lỗi]
  activate FcmSvc
  FcmSvc --> Service : 63. void
  deactivate FcmSvc
  Service --> Controller : 64. UpdateTaskStatusResponse{status=IN_PROGRESS}
  deactivate Service
  Controller --> F : 65. HTTP 200 OK
  deactivate Controller
else 56. canTransitionTo == false [transition không hợp lệ, ví dụ DONE → OPEN]
  Service --> Controller : 56a. throw 409 FAM-023\n"Cannot transition from X to Y"
  deactivate Service
  Controller --> F : 56b. HTTP 409 Conflict
  deactivate Controller
end

F -> Controller : 66. PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status\n{status=DONE}
activate Controller
Controller -> Service : 67. updateTaskStatus(groupId, taskId, request, callerId)
activate Service
Service -> Service : 68. tra cứu group/task + kiểm tra assignee\n(lặp lại các bước 46-55 ở trên — rút gọn)
Service -> Service : 69. IN_PROGRESS.canTransitionTo(DONE) = true
Service -> TaskRepo : 70. save(task{status=DONE, completedAt=now()})
activate TaskRepo
TaskRepo -> DB : 71. UPDATE care_tasks SET status='DONE', completed_at=now()
activate DB
DB --> TaskRepo : 72. updated
deactivate DB
TaskRepo --> Service : 73. CareTask
deactivate TaskRepo
Service -> Audit : 74. log(CARE_TASK_STATUS_UPDATED, callerId, "CareTask",\ntaskId, "IN_PROGRESS -> DONE")
activate Audit
Audit --> Service : 75. void
deactivate Audit
Service --> Controller : 76. UpdateTaskStatusResponse{status=DONE, completedAt}
deactivate Service
Controller --> F : 77. HTTP 200 OK
deactivate Controller

== UC-99 Cancel Family Care Task ==
M -> Controller : 78. POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel
activate Controller
Controller -> Service : 79. cancelFamilyTask(groupId, taskId, callerId)
activate Service
Service -> AuthPolicy : 80. canCancelTask(groupId, callerId) [chỉ OWNER]
activate AuthPolicy
AuthPolicy --> Service : 81. boolean (403 FAM-079 nếu không phải owner)
deactivate AuthPolicy
Service -> TaskRepo : 82. findByIdAndCareGroupId(taskId, groupId)
activate TaskRepo
TaskRepo -> DB : 83. SELECT * FROM care_tasks WHERE id=? AND care_group_id=?
activate DB
DB --> TaskRepo : 84. task row (404 FAM-033 nếu không có)
deactivate DB
TaskRepo --> Service : 85. CareTask
deactivate TaskRepo
alt 86. task.status IN (OPEN, IN_PROGRESS)
  Service -> TaskRepo : 86. save(task{status=CANCELLED})
  activate TaskRepo
  TaskRepo -> DB : 87. UPDATE care_tasks SET status='CANCELLED'
  activate DB
  DB --> TaskRepo : 88. updated
  deactivate DB
  TaskRepo --> Service : 89. CareTask
  deactivate TaskRepo
  Service -> Audit : 90. log(CARE_TASK_CANCELLED, callerId, "CareTask", taskId, "task cancelled")
  activate Audit
  Audit --> Service : 91. void
  deactivate Audit
  Service -> Service : 92. publishEvent(CareTaskCancelled)
  Service --> Controller : 93. CancelFamilyTaskResponse{status=CANCELLED}
  deactivate Service
  Controller --> M : 94. HTTP 200 OK
  deactivate Controller
else 86. task.status == DONE hoặc đã CANCELLED
  Service --> Controller : 86a. throw 409 FAM-080/FAM-081\n"completed/already-cancelled task cannot be cancelled"
  deactivate Service
  Controller --> M : 86b. HTTP 409 Conflict
  deactivate Controller
end

@enduml
```

**Hình 2 — Sequence Diagram: Assign Task → Update Content → Update Status (OPEN→IN_PROGRESS→DONE, FSM guard) → Cancel (Main Flow)**

> **Ghi chú grounding:** Tên method thật là `assignFamilyTask`/`updateFamilyTask`/
> `cancelFamilyTask`/`updateTaskStatus` trên `CareTaskServiceImpl` (không phải `assign`/
> `cancel`/`updateStatus` như class diagram mục 2 mô tả). Ba tác vụ **có phát sinh side
> effect chưa từng vẽ**: publish domain event (`FamilyTaskAssigned`/`CareTaskUpdated`/
> `CareTaskCancelled`) và gửi FCM best-effort (assign → thông báo cho assignee; cập nhật
> status → thông báo ngược lại cho `assignedBy`/người giao việc). `assignFamilyTask` còn có
> ràng buộc `dueAt` phải ở tương lai (`FAM-032`) chưa từng được vẽ. `updateFamilyTask` (nội
> dung task) chỉ cho phép khi task đang `OPEN`/`IN_PROGRESS` (`FAM-073`) — khác hoàn toàn với
> `updateTaskStatus` (chuyển trạng thái, chỉ assignee gọi được, luôn qua
> `CareTaskStatus.canTransitionTo()`).

## 4. State Machine — `CareTaskStatus` (FSM thật trong code, `canTransitionTo()`)

```plantuml
@startuml MF10_03_CareTaskStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> OPEN : Mother giao việc (UC-99)

OPEN --> IN_PROGRESS : Family Member bắt đầu (UC-100)
OPEN --> DONE : Family Member hoàn tất trực tiếp (UC-100)
OPEN --> NEEDS_SUPPORT : Family Member cần hỗ trợ ngay (UC-100)

IN_PROGRESS --> DONE : Hoàn tất (UC-100)
IN_PROGRESS --> NEEDS_SUPPORT : Gặp khó khăn, cần hỗ trợ (UC-100)

NEEDS_SUPPORT --> IN_PROGRESS : Tiếp tục xử lý sau khi được hỗ trợ (UC-100)
NEEDS_SUPPORT --> DONE : Hoàn tất sau hỗ trợ (UC-100)

OPEN --> CANCELLED : Mother (owner) huỷ nhiệm vụ (UC-99)\n[owner-only, KHÔNG qua canTransitionTo()]
IN_PROGRESS --> CANCELLED : Mother (owner) huỷ nhiệm vụ (UC-99)\n[owner-only, KHÔNG qua canTransitionTo()]

DONE --> [*]
CANCELLED --> [*]

note right of DONE
  DONE và CANCELLED là trạng thái kết thúc (terminal) —
  canTransitionTo() trả về false cho mọi target từ hai state này,
  trừ self-transition (idempotent, theo SRS exception E3).
end note

note right of CANCELLED
  Huỷ nhiệm vụ (UC-99) đi qua cancelFamilyTask(), một code path
  RIÊNG chỉ Owner gọi được — không dùng canTransitionTo() của
  assignee. Chỉ cho phép huỷ từ OPEN hoặc IN_PROGRESS; task đã
  DONE hoặc đang NEEDS_SUPPORT không huỷ được trực tiếp
  (FAM-080/FAM-081 trong CareTaskServiceImpl).
end note

@enduml
```

**Hình 3 — State Machine: `CareTaskStatus` Lifecycle (đúng theo `canTransitionTo()` thật trong code)**

## 5. Business Rules Applied

- BR-RBAC — chỉ Mother (assigner/owner) tạo/sửa/huỷ task; chỉ đúng `assignedTo` mới cập nhật được status của task đó.
- UC-99/UC-100 — mọi chuyển trạng thái phải hợp lệ theo `CareTaskStatus.canTransitionTo()`; request chuyển trạng thái không hợp lệ (ví dụ `DONE → OPEN`) bị từ chối.
- Self-transition (cùng trạng thái) được cho phép — idempotent theo đặc tả lỗi E3 của SRS, tránh lỗi khi client gọi lại do mất kết nối.
- UC-99 postcondition — huỷ task (`CANCELLED`) chỉ áp dụng được từ `OPEN` hoặc `IN_PROGRESS`, chỉ do Owner thực hiện; task đã `DONE`, đã `CANCELLED`, hoặc đang `NEEDS_SUPPORT` không huỷ trực tiếp được (mã lỗi FAM-080/FAM-081).
