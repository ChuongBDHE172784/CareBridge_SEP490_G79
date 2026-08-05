# MF-08 / Spec 02 — Family Permission Scope & Shared Care Visibility

| Field | Value |
| --- | --- |
| Feature | MF-08 — Family Sync & Cooperative Care |
| Use Cases Covered | Update/view family permission; view scoped shared data, quick notes and family alerts |
| Primary Actor(s) | Mother (Owner), Family Member |
| Platform | Mother Mobile App, Family Mobile App, CareBridge API |
| Main Flow Summary | Mother grants granular flags on an accepted membership. Family reads only data allowed by the current membership and category; emergency notification history is account-scoped. |
| Grounding (source code) | `CareGroupController`, `SharedDataController`, `FamilyQuickNoteController`, `FamilyAlertController`, `CareGroupServiceImpl`, `SharedDataServiceImpl`, `CareGroupMember.permissionJson` |

## 1. Tổng quan luồng chính (Main Flow Overview)

Quyền Family được lưu trong `CareGroupMember.permissionJson`, gồm `calendar`, `logs`, `alerts`, `records` và nhóm quyền quick-note chi tiết. Mother cập nhật quyền theo từng membership đã chấp nhận. Khi Family đọc shared data, service nạp lại membership và kiểm tra đúng category tại thời điểm request. `SharedDataController` dùng một endpoint theo `category`; code hiện tại chưa có endpoint calendar độc lập. Lịch sử `family-alerts` đọc notification khẩn cấp theo tài khoản người nhận, không dùng cờ `alerts` của một care group cụ thể.

## 2. Class Diagram

```plantuml
@startuml MF08_02_FamilyPermission_ClassDiagram
skinparam classAttributeIconSize 0
class CareGroupMember { +id: UUID; +careGroupId: UUID; +userId: UUID; +inviteStatus: InviteStatus; +permissionJson: String }
class FamilyPermission <<JSON>> { +calendar: boolean; +logs: boolean; +alerts: boolean; +records: boolean; +quickNotes: boolean; +quickNoteWeight: boolean; +quickNoteHydration: boolean; +quickNoteEpds: boolean; +quickNoteFetalMovement: boolean }
enum SharedDataCategory { CALENDAR; LOGS; ALERTS }
class CareGroupController
class SharedDataController
class FamilyQuickNoteController
class FamilyAlertController
class CareGroupServiceImpl
class SharedDataServiceImpl
class FamilyQuickNoteService
class FamilyAlertServiceImpl
CareGroupMember *-- FamilyPermission
CareGroupController --> CareGroupServiceImpl
SharedDataController --> SharedDataServiceImpl
FamilyQuickNoteController --> FamilyQuickNoteService
FamilyAlertController --> FamilyAlertServiceImpl
SharedDataServiceImpl --> SharedDataCategory
@enduml
```

**Hình 1 — Class Diagram: Permission flags và các read model Family**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF08_02_FamilyPermission_SequenceDiagram
actor "Mother" as M
actor "Family Member" as F
participant "Mobile UI" as UI
participant "CareGroupController" as GroupController
participant "SharedDataController" as SharedController
participant "CareGroupServiceImpl" as GroupService
participant "SharedDataServiceImpl" as SharedService
participant "CareGroupMemberRepository" as MemberRepo
participant "Scoped Data Repositories" as DataRepo
database "PostgreSQL" as DB

M -> UI : 1. Chọn quyền cho thành viên
activate UI
UI -> GroupController : 2. PATCH /api/v1/care-groups/{groupId}/members/{memberId}/permissions
activate GroupController
GroupController -> GroupService : 3. updateFamilyPermission(groupId, memberId, request, ownerId)
activate GroupService
GroupService -> MemberRepo : 4. find accepted member in owner group
activate MemberRepo
MemberRepo -> DB : 5. SELECT care_group_members
activate DB
DB --> MemberRepo : 6. membership / empty
deactivate DB
MemberRepo --> GroupService : 7. Optional<CareGroupMember>
deactivate MemberRepo
alt [caller là Owner và target đã ACCEPTED]
  GroupService -> GroupService : 8a. merge non-null permission flags
  activate GroupService
  GroupService --> GroupService : 8a-1. normalized permission JSON
  deactivate GroupService
  GroupService -> MemberRepo : 8a-2. save(permissionJson)
  activate MemberRepo
  MemberRepo -> DB : 8a-3. UPDATE care_group_members
  activate DB
  DB --> MemberRepo : 8a-4. updated membership
  deactivate DB
  MemberRepo --> GroupService : 8a-5. CareGroupMember
  deactivate MemberRepo
  GroupService --> GroupController : 8a-6. FamilyPermissionResponse
  deactivate GroupService
  GroupController --> UI : 8a-7. 200 OK
  deactivate GroupController
else [không phải Owner hoặc membership không hợp lệ]
  GroupService --> GroupController : 8b. Access/NotFound exception
  deactivate GroupService
  GroupController --> UI : 8b-1. 403 Forbidden hoặc 404 Not Found
  deactivate GroupController
end
UI --> M : 9. Hiển thị quyền hiện tại
deactivate UI

F -> UI : 10. Mở dữ liệu được chia sẻ
activate UI
UI -> SharedController : 11. GET /api/v1/care-groups/{groupId}/shared-data?category=calendar
activate SharedController
SharedController -> SharedService : 12. getSharedData(groupId, familyId, CALENDAR, page, size)
activate SharedService
SharedService -> MemberRepo : 13. find accepted membership
activate MemberRepo
MemberRepo -> DB : 14. SELECT care_group_members
activate DB
DB --> MemberRepo : 15. membership / empty
deactivate DB
MemberRepo --> SharedService : 16. Optional<CareGroupMember>
deactivate MemberRepo
alt [membership hợp lệ và calendar=true]
  SharedService -> DataRepo : 17a. find scoped care data
  activate DataRepo
  DataRepo -> DB : 17a-1. SELECT scoped care data
  activate DB
  DB --> DataRepo : 17a-2. rows[]
  deactivate DB
  DataRepo --> SharedService : 17a-3. items[]
  deactivate DataRepo
  SharedService --> SharedController : 17a-4. SharedDataResponse
  deactivate SharedService
  SharedController --> UI : 17a-5. 200 OK
  deactivate SharedController
else [membership bị thu hồi hoặc thiếu quyền]
  SharedService --> SharedController : 17b. Access exception
  deactivate SharedService
  SharedController --> UI : 17b-1. 403 Forbidden
  deactivate SharedController
end
UI --> F : 18. Hiển thị dữ liệu hoặc thông báo quyền
deactivate UI
@enduml
```

**Hình 2 — Sequence Diagram: Cấp quyền và đọc dữ liệu theo scope hiện tại**

## 4. Business Rules Applied

- Chỉ Owner cập nhật quyền; Owner và chính target member được xem grant hiện tại.
- Trường `null` trong patch giữ nguyên giá trị cũ; quyền quick-note cha và quyền con đều phải thỏa khi đọc dữ liệu nhạy cảm.
- Mọi read phải kiểm tra membership `ACCEPTED` và quyền hiện tại; không cache quyền qua lần thu hồi.
- `SharedDataController` chỉ chấp nhận `calendar`, `logs`, `alerts`; category không hợp lệ trả `400 Bad Request`.
- Code hiện tại có một phần shared-data trả read model rỗng khi nguồn cross-domain chưa được nối; tài liệu không tuyên bố phần này đã hoàn tất.
- `GET /api/v1/family-alerts` là inbox notification theo account, không đồng nghĩa với shared-data category `alerts`.
