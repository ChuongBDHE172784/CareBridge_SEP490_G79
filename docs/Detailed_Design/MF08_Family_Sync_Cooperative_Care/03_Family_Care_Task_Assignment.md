# MF-08 / Spec 03 — Family Care Task Assignment & Status Update

| Field | Value |
| --- | --- |
| Feature | MF-08 — Family Sync & Cooperative Care |
| Use Cases Covered | Assign/list/view/update/cancel family task; assignee updates task status |
| Primary Actor(s) | Mother (Owner), assigned Family Member |
| Platform | Mother Mobile App, Family Mobile App, CareBridge API |
| Main Flow Summary | Mother assigns a task to an accepted member. The assignee updates status through server-enforced transitions; Mother may update content or cancel an incomplete task. |
| Grounding (source code) | `CareGroupController`, `CareTaskServiceImpl`, `family/entity/CareTask.java`, `family/entity/CareTaskStatus.java`, `ICareTaskService` |

## 1. Tổng quan luồng chính (Main Flow Overview)

`family.entity.CareTask` là công việc cộng tác trong care group, khác checklist task và reminder task. Mother tạo task cho một member `ACCEPTED`. Service kiểm tra group ownership, assignee và hạn xử lý trước khi lưu. Chỉ assignee được đổi trạng thái thực hiện; `CareTaskStatus.canTransitionTo()` bảo vệ chuyển trạng thái. Mother có thể sửa nội dung task chưa kết thúc hoặc hủy task chưa hoàn tất.

## 2. Class Diagram

```plantuml
@startuml MF08_03_CareTask_ClassDiagram
skinparam classAttributeIconSize 0
class CareTask { +id: UUID; +careGroupId: UUID; +assignedBy: UUID; +assignedTo: UUID; +title: String; +description: String; +dueAt: Instant; +status: CareTaskStatus; +completedAt: Instant }
enum CareTaskStatus { OPEN; IN_PROGRESS; DONE; CANCELLED; NEEDS_SUPPORT; +canTransitionTo(target): boolean }
class CareGroupController
interface ICareTaskService
class CareTaskServiceImpl
interface CareTaskRepository
interface CareGroupMemberRepository
CareTask --> CareTaskStatus
CareGroupController --> ICareTaskService
CareTaskServiceImpl ..|> ICareTaskService
CareTaskServiceImpl --> CareTaskRepository
CareTaskServiceImpl --> CareGroupMemberRepository
@enduml
```

**Hình 1 — Class Diagram: Family Care Task và transition guard**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF08_03_CareTask_SequenceDiagram
actor "Mother" as M
actor "Family Member" as F
participant "Mobile UI" as UI
participant "CareGroupController" as Controller
participant "CareTaskServiceImpl" as Service
participant "CareGroupMemberRepository" as MemberRepo
participant "CareTaskRepository" as TaskRepo
database "PostgreSQL" as DB

M -> UI : 1. Nhập task và chọn người phụ trách
activate UI
UI -> Controller : 2. POST /api/v1/care-groups/{groupId}/tasks
activate Controller
Controller -> Service : 3. assignFamilyTask(groupId, request, ownerId)
activate Service
Service -> MemberRepo : 4. find accepted assignee
activate MemberRepo
MemberRepo -> DB : 5. SELECT care_group_members
activate DB
DB --> MemberRepo : 6. membership / empty
deactivate DB
MemberRepo --> Service : 7. Optional<CareGroupMember>
deactivate MemberRepo
alt [caller là Owner và assignee hợp lệ]
  Service -> TaskRepo : 8a. save(CareTask{OPEN})
  activate TaskRepo
  TaskRepo -> DB : 8a-1. INSERT care_tasks
  activate DB
  DB --> TaskRepo : 8a-2. task row
  deactivate DB
  TaskRepo --> Service : 8a-3. CareTask
  deactivate TaskRepo
  Service --> Controller : 8a-4. AssignFamilyTaskResponse
  deactivate Service
  Controller --> UI : 8a-5. 201 Created
  deactivate Controller
else [không phải Owner hoặc assignee chưa ACCEPTED]
  Service --> Controller : 8b. Access/Validation exception
  deactivate Service
  Controller --> UI : 8b-1. 403 Forbidden hoặc 400 Bad Request
  deactivate Controller
end
UI --> M : 9. Hiển thị kết quả giao việc
deactivate UI

F -> UI : 10. Chọn trạng thái mới
activate UI
UI -> Controller : 11. PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status
activate Controller
Controller -> Service : 12. updateTaskStatus(groupId, taskId, request, familyId)
activate Service
Service -> TaskRepo : 13. find task for update
activate TaskRepo
TaskRepo -> DB : 14. SELECT care_tasks FOR UPDATE
activate DB
DB --> TaskRepo : 15. task row / empty
deactivate DB
TaskRepo --> Service : 16. Optional<CareTask>
deactivate TaskRepo
Service -> Service : 12a. verify assignee and canTransitionTo(target)
activate Service
Service --> Service : 12a-1. transition decision
deactivate Service
alt [đúng assignee và transition hợp lệ]
  Service -> TaskRepo : 17a. save(new status)
  activate TaskRepo
  TaskRepo -> DB : 17a-1. UPDATE care_tasks
  activate DB
  DB --> TaskRepo : 17a-2. updated task
  deactivate DB
  TaskRepo --> Service : 17a-3. CareTask
  deactivate TaskRepo
  Service --> Controller : 17a-4. UpdateTaskStatusResponse
  deactivate Service
  Controller --> UI : 17a-5. 200 OK
  deactivate Controller
else [không phải assignee hoặc transition không hợp lệ]
  Service --> Controller : 17b. Access/Conflict exception
  deactivate Service
  Controller --> UI : 17b-1. 403 Forbidden hoặc 409 Conflict
  deactivate Controller
end
UI --> F : 18. Hiển thị trạng thái hiện tại
deactivate UI
@enduml
```

**Hình 2 — Sequence Diagram: Giao việc và cập nhật trạng thái bởi assignee**

## 4. Business Rules Applied

- Chỉ Owner giao, sửa hoặc hủy task; assignee phải là member `ACCEPTED` của cùng group.
- Chỉ `assignedTo` được cập nhật trạng thái thực hiện.
- Transition phải qua `CareTaskStatus.canTransitionTo()`; trạng thái kết thúc không được mở lại tùy ý.
- Task hoàn tất không được hủy; update/cancel phải khóa hoặc kiểm tra lại bản ghi hiện tại để tránh ghi đè cạnh tranh.
- `family.entity.CareTask` không được trộn với `ChecklistTaskInstance` hoặc reminder task.
