# MF-10 / Spec 02 — Family Permission Scope & Shared Care Visibility

| Field | Value |
| --- | --- |
| Feature | MF-10 — Family Sync & Cooperative Care |
| Use Cases Covered | UC-98 Manage Family Permission Scope, UC-101 View Shared Care Calendar, Data and Alerts |
| Primary Actor(s) | Mother (grants scope), Family Member (consumes within scope) |
| Platform | Mother Mobile App, Family Mobile App |
| Main Flow Summary | A Mother defines which categories of data (calendar, logs, alerts, records) a specific family member may see, stored as a per-membership permission scope; the family member then views only the calendar items, shared data and alerts allowed by their current scope — never more. |
| Grounding (source code) | `family/entity/CareGroupMember.java` (`permissionJson`), `family/dto/FamilyPermission.java`, `family/entity/PermissionFlag.java`, `family/entity/SharedDataCategory.java`, `family/controller/CareGroupController.java` (`/{groupId}/members/{memberId}/permissions`, `/{groupId}/calendar`), `family/controller/SharedDataController.java` (`/{groupId}/shared-data`), `family/controller/FamilyAlertController.java` (`/api/v1/family-alerts`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là mô hình "consent nội bộ nhóm" của MF-10 — khác với `ConsentGrant` (MF-01, dùng cho
chia sẻ điểm-tới-điểm có hạn) hoặc `DataPermission` (MF-08, dùng cho chia sẻ với
chuyên gia). Ở đây, phạm vi quyền được lưu **trực tiếp trên `CareGroupMember.permissionJson`**
dưới dạng 4 cờ boolean (`calendar`/`logs`/`alerts`/`records` — DTO `FamilyPermission`),
do Owner cập nhật cho từng thành viên (UC-98). Mọi truy vấn dữ liệu chia sẻ của Family
Member (lịch chăm sóc UC-101, dữ liệu chia sẻ, cảnh báo gia đình) đều lọc lại theo đúng
scope hiện tại của `CareGroupMember` gọi request đó — không có bảng "shared calendar"
hay "shared data" vật lý riêng, tất cả là read-model được lọc lại từ dữ liệu gốc
(`CareTask`, `BabyDailyLog`, `FamilyAlertLog`...) theo permission.

## 2. Class Diagram

```plantuml
@startuml MF10_02_FamilyPermission_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class CareGroupMember {
  + id: UUID
  + careGroupId: UUID
  + userId: UUID
  + memberRole: GroupMemberRole
  + permissionJson: String
}

class FamilyPermission <<embedded JSON>> {
  + calendar: boolean
  + logs: boolean
  + alerts: boolean
  + records: boolean
}

enum SharedDataCategory {
  CALENDAR
  LOGS
  ALERTS
}

class CalendarItemDto <<read-model>> {
  + taskId: UUID
  + title: String
  + dueAt: Instant
  + status: String
  + assignedTo: UUID
}

class SharedDataItemDto <<read-model>> {
  + itemId: UUID
  + itemType: String
  + title: String
  + occurredAt: Instant
}

class FamilyAlertItemDto <<read-model>> {
  + alertId: UUID
  + message: String
  + sentAt: Instant
}

class CareGroupController {
  - careGroupService: ICareGroupService
  + updatePermission(groupId, memberId, UpdateFamilyPermissionRequest): ResponseEntity
  + getPermission(groupId, memberId): ResponseEntity
  + calendar(groupId): ResponseEntity
}

class SharedDataController {
  - sharedDataService: ISharedDataService
  + sharedData(groupId, category): ResponseEntity
}

class FamilyAlertController {
  - familyAlertService: IFamilyAlertService
  + myAlerts(): ResponseEntity
}

interface ICareCalendarService <<interface>> {
  + calendar(requesterId: UUID, groupId: UUID): List<CalendarItemDto>
}

class CareCalendarServiceImpl implements ICareCalendarService {
  - careGroupMemberRepository: CareGroupMemberRepository
  - careTaskRepository: CareTaskRepository
}

interface ISharedDataService <<interface>> {
  + sharedData(requesterId: UUID, groupId: UUID, category: SharedDataCategory): List<SharedDataItemDto>
}

class SharedDataServiceImpl implements ISharedDataService {
  - careGroupMemberRepository: CareGroupMemberRepository
}

CareGroupMember "1" -- "1" FamilyPermission : permissionJson (embedded)
SharedDataItemDto ..> SharedDataCategory
CareGroupController --> ICareCalendarService : uses
SharedDataController --> ISharedDataService : uses
CareCalendarServiceImpl --> CareGroupMemberRepository : kiểm tra scope trước khi trả dữ liệu
CareCalendarServiceImpl ..> CalendarItemDto : builds
SharedDataServiceImpl ..> SharedDataItemDto : builds
FamilyAlertController ..> FamilyAlertItemDto : builds (theo scope alerts=true)

@enduml
```

**Hình 1 — Class Diagram: Family Permission Scope & Shared Read-Models**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF10_02_FamilyPermission_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother (Owner)" as M
participant "CareGroupController" as GroupController
actor "Family Member" as F
participant "CareCalendarServiceImpl" as CalService
participant "SharedDataController" as SharedController
participant "FamilyAlertController" as AlertController
database "PostgreSQL" as DB

== UC-98 Manage Family Permission Scope ==
M -> GroupController : PATCH /api/v1/care-groups/{groupId}/members/{memberId}/permissions\n{calendar=true, logs=true, alerts=false, records=false}
GroupController -> DB : UPDATE care_group_members\nSET permission_json = '{"calendar":true,"logs":true,"alerts":false,"records":false}'
GroupController --> M : HTTP 200 OK

== UC-101 View Shared Care Calendar, Data and Alerts ==
F -> GroupController : GET /api/v1/care-groups/{groupId}/calendar
GroupController -> CalService : calendar(requesterId, groupId)
CalService -> DB : SELECT permission_json FROM care_group_members\nWHERE care_group_id=? AND user_id=?
DB --> CalService : permission{calendar=true}
alt calendar == true
  CalService -> DB : SELECT * FROM care_tasks WHERE care_group_id=?
  DB --> CalService : tasks[]
  CalService --> GroupController : CalendarItemDto[]
  GroupController --> F : HTTP 200 OK {calendar[]}
else calendar == false
  GroupController --> F : HTTP 200 OK {calendar: []}\n[rỗng, không lộ dữ liệu]
end

F -> SharedController : GET /api/v1/care-groups/{groupId}/shared-data?category=LOGS
SharedController -> DB : kiểm tra permission_json.logs == true trước khi truy vấn
SharedController --> F : HTTP 200 OK {sharedData[]} hoặc 403 nếu scope không cho phép

F -> AlertController : GET /api/v1/family-alerts
AlertController -> DB : kiểm tra permission_json.alerts theo từng group của Family Member
AlertController --> F : HTTP 200 OK {alerts[]} (chỉ nhóm có alerts=true)

@enduml
```

**Hình 2 — Sequence Diagram: Owner Sets Scope → Family Member Views Calendar/Data/Alerts Within Scope (Main Flow)**

## 4. State Machine — Phạm vi quyền hiệu lực theo `SharedDataCategory` (per-category toggle)

```plantuml
@startuml MF10_02_PermissionScope_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

state "Mỗi cờ trong FamilyPermission\n(calendar / logs / alerts / records)" as Flag {
  [*] --> DENIED : Mặc định khi mời thành viên\n[FamilyPermission.defaults() = tất cả false]
  DENIED --> GRANTED : Owner bật cờ tương ứng (UC-98)
  GRANTED --> DENIED : Owner tắt cờ tương ứng (UC-98)
}

note right of Flag
  Đây không phải state machine nhiều bước mà là 4 toggle độc lập
  (đúng theo FamilyPermission DTO thật: 4 boolean rời rạc) —
  UC-101 kiểm tra lại từng cờ tại thời điểm truy vấn, không cache.
end note

@enduml
```

**Hình 3 — State Machine: Family Permission Flag Toggle (`DENIED` ↔ `GRANTED`, per category)**

## 5. Business Rules Applied

- BR-RBAC — chỉ `OWNER` của care group được thay đổi `permissionJson` của thành viên.
- UC-98 — phạm vi quyền là theo từng thành viên (`memberId`), không áp dụng chung cho cả nhóm.
- UC-101 — mọi endpoint đọc dữ liệu chia sẻ đều kiểm tra lại cờ tương ứng tại thời điểm truy vấn; không trả dữ liệu vượt scope kể cả khi request hợp lệ về mặt xác thực.
- BR-PRIVACY — hồ sơ sức khỏe chi tiết (`records`) là cờ nhạy cảm nhất, mặc định `false`.
