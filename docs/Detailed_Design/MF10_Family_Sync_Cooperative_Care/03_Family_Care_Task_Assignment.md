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
participant "CareGroupController" as Controller
participant "CareTaskServiceImpl" as Service
actor "Family Member" as F
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-99 Create Family Care Task ==
M -> Controller : POST /api/v1/care-groups/{groupId}/tasks\n{assignedTo, title, dueAt}
Controller -> Service : assign(assignerId, groupId, request)
Service -> Service : check assignedTo là thành viên ACCEPTED của groupId
Service -> DB : INSERT INTO care_tasks (status=OPEN)
Service -> Audit : emit(CARE_TASK_ASSIGNED)
Service --> Controller : CareTask{status=OPEN}
Controller --> M : HTTP 201 Created

== UC-100 Update Assigned Task Status ==
F -> Controller : PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status\n{status=IN_PROGRESS}
Controller -> Service : updateStatus(assigneeId, taskId, IN_PROGRESS)
Service -> DB : SELECT * FROM care_tasks WHERE id=?
DB --> Service : task{status=OPEN, assignedTo=assigneeId}
Service -> Service : check assigneeId == task.assignedTo
Service -> Service : task.status.canTransitionTo(IN_PROGRESS)?\n[OPEN → IN_PROGRESS: true]
Service -> DB : UPDATE care_tasks SET status='IN_PROGRESS'
Service -> Audit : emit(CARE_TASK_STATUS_UPDATED)
Service --> Controller : CareTask{status=IN_PROGRESS}
Controller --> F : HTTP 200 OK

F -> Controller : PATCH .../status {status=DONE}
Controller -> Service : updateStatus(assigneeId, taskId, DONE)
Service -> Service : IN_PROGRESS.canTransitionTo(DONE) = true
Service -> DB : UPDATE care_tasks SET status='DONE', completed_at=now()
Service --> F : HTTP 200 OK

== UC-99 Cancel Family Care Task ==
M -> Controller : POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel
Controller -> Service : cancel(ownerId, groupId, taskId)
Service -> Service : check caller == group OWNER (FAM-079)
Service -> Service : check task.status IN (OPEN, IN_PROGRESS)\n[DONE→FAM-080, đã CANCELLED→FAM-081]
Service -> DB : UPDATE care_tasks SET status='CANCELLED'
Service -> Audit : emit(CARE_TASK_CANCELLED)
Service --> M : HTTP 200 OK

@enduml
```

**Hình 2 — Sequence Diagram: Assign Task → Update Status (OPEN→IN_PROGRESS→DONE) → Cancel (Main Flow)**

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
