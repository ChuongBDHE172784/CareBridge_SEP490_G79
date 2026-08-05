# MF-02 / Spec 05 — Reminder Lifecycle & Today Tasks

| Field | Value |
| --- | --- |
| Feature | MF-02 — Mother Care Journey |
| Use Cases Covered | UC-89 Create Appointment Reminder, UC-90 Create Medicine or Vitamin Reminder, UC-91 Create Vaccination Reminder, UC-92 Update, Snooze, Complete, Skip or Delete Care Reminder, UC-93 View Today Tasks and Care Plan |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother creates a reminder of one of three types (appointment/medication/vaccination) against a single shared `Reminder` entity, manages its lifecycle (snooze/complete/skip/cancel), and views a prioritized "today" view that aggregates due reminders with permitted family tasks. |
| Grounding (source code) | `reminder/entity/Reminder.java`, `ReminderStatus.java`, `ReminderType.java`, `RecurrenceType.java`, `reminder/controller/ReminderController.java` (`/api/v1/reminders`), `reminder/service/impl/TodayTaskServiceImpl.java` |

## 1. Tổng quan luồng chính (Main Flow Overview)

Ba use case tạo nhắc lịch (UC-89/90/91) đều ghi vào **cùng một entity** `Reminder`, chỉ
khác `reminderType` (`APPOINTMENT`/`MEDICATION`/`VACCINATION`) và request DTO đặc thù cho
từng loại (`CreateMedicationReminderRequest`, `CreateVaccinationReminderRequest` gắn thêm
ngữ cảnh liên kết vaccination record của MF-03) — nên được gộp làm một luồng "tạo nhắc
lịch" duy nhất thay vì ba spec riêng. UC-92 quản lý vòng đời chung
(`snooze`/`complete`/`skip`/`cancel`/`enable`) áp dụng như nhau cho cả ba loại. UC-93
("Today Tasks") là read-model tổng hợp `Reminder` đến hạn **và** `CareTask` được giao
đến hạn trong ngày — đây là điểm giao giữa MF-02 và MF-08 (Family Sync), không tạo entity
mới.

## 2. Class Diagram

```plantuml
@startuml MF02_06_Reminder_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class Reminder {
  + id: UUID
  + ownerUserId: UUID
  + journeyId: UUID
  + babyId: UUID
  + reminderType: ReminderType
  + title: String
  + scheduledAt: Instant
  + recurrenceType: RecurrenceType
  + recurrenceEndDate: Instant
  + fcmJobId: String
  + status: ReminderStatus
  + snoozedUntil: Instant
}

enum ReminderType {
  APPOINTMENT
  MEDICATION
  VACCINATION
}

enum RecurrenceType {
  NONE
  DAILY
  WEEKLY
  MONTHLY
}

enum ReminderStatus {
  PENDING
  SNOOZED
  COMPLETED
  SKIPPED
  CANCELLED
}

class TodayTaskItem <<read-model>> {
  + sourceType: String
  + sourceId: UUID
  + title: String
  + dueAt: Instant
  + priority: String
}

class CreateReminderRequest {
  + reminderType: ReminderType
  + title: String
  + scheduledAt: Instant
  + recurrenceType: RecurrenceType
}

class ReminderController {
  - reminderService: IReminderService
  - todayTaskService: ITodayTaskService
  + create(CreateReminderRequest): ResponseEntity
  + createMedication(CreateMedicationReminderRequest): ResponseEntity
  + createVaccination(CreateVaccinationReminderRequest): ResponseEntity
  + update(reminderId, UpdateReminderRequest): ResponseEntity
  + snooze(reminderId, SnoozeReminderRequest): ResponseEntity
  + complete(reminderId): ResponseEntity
  + skip(reminderId): ResponseEntity
  + delete(reminderId): ResponseEntity
  + today(): ResponseEntity
}

interface IReminderService <<interface>> {
  + create(ownerId: UUID, request): Reminder
  + snooze(ownerId: UUID, reminderId: UUID, until: Instant): Reminder
  + complete(ownerId: UUID, reminderId: UUID): Reminder
  + skip(ownerId: UUID, reminderId: UUID): Reminder
}

class ReminderServiceImpl implements IReminderService {
  - reminderRepository: ReminderRepository
  - notificationService: INotificationService
  - auditService: AuditService
}

interface ITodayTaskService <<interface>> {
  + today(ownerId: UUID): List<TodayTaskItem>
}

class TodayTaskServiceImpl implements ITodayTaskService {
  - reminderRepository: ReminderRepository
  - careTaskRepository: CareTaskRepository
}

Reminder --> ReminderType
Reminder --> RecurrenceType
Reminder --> ReminderStatus
ReminderController --> IReminderService : uses
ReminderController --> ITodayTaskService : uses
ReminderServiceImpl --> INotificationService : schedules FCM job (NS-08)
TodayTaskServiceImpl ..> TodayTaskItem : builds (Reminder + CareTask due)

@enduml
```

**Hình 1 — Class Diagram: Reminder (3 loại chung 1 entity) & Today Task Read-Model**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF02_06_Reminder_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "Web / Mobile UI" as UI
participant "ReminderController" as Controller
participant "ReminderServiceImpl" as Service
participant "INotificationService" as Notif
participant "AuditService" as Audit
participant "TodayTaskServiceImpl" as TodayService
participant "ReminderRecurrenceService" as RecurrenceService
participant "ReminderRepository" as ReminderRepo
participant "CareTaskRepository" as CareTaskRepo
database "PostgreSQL" as DB

== UC-89/90/91 Create Reminder (Appointment / Medication / Vaccination — same entity) ==
M -> UI : 1. Submit request
activate UI
UI -> Controller : 1a. POST /api/v1/reminders\n{reminderType=APPOINTMENT, title, scheduledAt, recurrenceType}
activate Controller
Controller -> Service : 2. createReminder(request, callerId)
activate Service
Service -> Service : 3. validateScheduledAt(scheduledAt) [must be in the future]
Service -> ReminderRepo : 4. save(Reminder{status=PENDING})
activate ReminderRepo
ReminderRepo -> DB : 5. INSERT INTO reminders ...
activate DB
DB --> ReminderRepo : 6. saved
deactivate DB
ReminderRepo --> Service : 7. Reminder
deactivate ReminderRepo
Service -> Notif : 8. scheduleFcmPush(callerId, title, body, scheduledAt)
activate Notif
Notif --> Service : 9. fcmJobId
deactivate Notif
Service -> ReminderRepo : 10. save(reminder{fcmJobId})\n[second write after having jobId]
activate ReminderRepo
ReminderRepo -> DB : 11. UPDATE reminders SET fcm_job_id=?
activate DB
DB --> ReminderRepo : 12. updated
deactivate DB
ReminderRepo --> Service : 13. Reminder
deactivate ReminderRepo
Service -> Audit : 14. log(REMINDER_CREATED, callerId,\n"Reminder", id, "created")
activate Audit
Audit --> Service : 15. void
deactivate Audit
Service --> Controller : 16. CreateReminderResponse
deactivate Service
Controller --> UI : 17. HTTP 201 Created
deactivate Controller
UI --> M : 17a. Display HTTP 201 Created
deactivate UI

note over Service
  createMedicationReminder / createVaccinationReminder follow the exact same
  flow above, only with fixed reminderType (MEDICATION/VACCINATION)
  and createVaccinationReminder adds requireBabyOwnership(babyId) before step 3.
end note

== UC-92 Update, Snooze, Complete, Skip or Delete Care Reminder ==
M -> UI : 18. Submit request
activate UI
UI -> Controller : 18a. PATCH /api/v1/reminders/{id}/snooze\n{snoozedUntil}
activate Controller
Controller -> Service : 19. snoozeReminder(id, request, callerId)
activate Service
Service -> ReminderRepo : 20. findById(id)
activate ReminderRepo
ReminderRepo -> DB : 21. SELECT * FROM reminders WHERE id=?
activate DB
DB --> ReminderRepo : 22. reminder row
deactivate DB
ReminderRepo --> Service : 23. Reminder
deactivate ReminderRepo
Service -> Service : 24. check ownership + non-terminal status (REM-007)
alt [25. snoozedUntil valid — future and no more than 24 hours]
  Service -> Notif : 25. cancelFcmJob(oldFcmJobId) [if exists]
  activate Notif
  Notif --> Service : 26. void
  deactivate Notif
  Service -> Notif : 27. scheduleFcmPush(callerId, title, body, snoozedUntil)
  activate Notif
  Notif --> Service : 28. newFcmJobId
  deactivate Notif
  Service -> ReminderRepo : 29. save(reminder{status=SNOOZED,\nsnoozedUntil, fcmJobId=newFcmJobId})
  activate ReminderRepo
  ReminderRepo -> DB : 30. UPDATE reminders\nSET status='SNOOZED', snoozed_until=?, fcm_job_id=?
  activate DB
  DB --> ReminderRepo : 31. updated
  deactivate DB
  ReminderRepo --> Service : 32. Reminder
  deactivate ReminderRepo
  Service --> Controller : 33. ReminderDetailResponse{status=SNOOZED}\n(DO NOT log audit for snooze)
  deactivate Service
  Controller --> UI : 34. HTTP 200 OK
  deactivate Controller
  UI --> M : 34a. Display HTTP 200 OK
  deactivate UI
else [25. snoozedUntil invalid — past or more than 24 hours]
  Service --> Controller : 25a. throw 400 REM-005/REM-008\n"snoozedUntil must be in the future / within 24h"
  deactivate Service
  Controller --> UI : 25b. HTTP 400 Bad Request
  deactivate Controller
  UI --> M : 25c. Display HTTP 400 Bad Request
  deactivate UI
end

M -> UI : 35. Submit request
activate UI
UI -> Controller : 35a. PATCH /api/v1/reminders/{id}/complete
activate Controller
Controller -> Service : 36. completeReminder(id, callerId)
activate Service
Service -> ReminderRepo : 37. findById(id)
activate ReminderRepo
ReminderRepo -> DB : 38. SELECT * FROM reminders WHERE id=?
activate DB
DB --> ReminderRepo : 39. reminder row
deactivate DB
ReminderRepo --> Service : 40. Reminder
deactivate ReminderRepo
Service -> Service : 41. check ownership + non-terminal status (REM-007)
Service -> Notif : 42. cancelFcmJob(fcmJobId) [if exists]
activate Notif
Notif --> Service : 43. void
deactivate Notif
Service -> ReminderRepo : 44. save(reminder{status=COMPLETED, snoozedUntil=null})
activate ReminderRepo
ReminderRepo -> DB : 45. UPDATE reminders SET status='COMPLETED', snoozed_until=NULL
activate DB
DB --> ReminderRepo : 46. updated
deactivate DB
ReminderRepo --> Service : 47. Reminder
deactivate ReminderRepo
Service -> Audit : 48. log(REMINDER_COMPLETED, callerId,\n"Reminder", id, "completed")
activate Audit
Audit --> Service : 49. void
deactivate Audit
Service --> Controller : 50. ReminderDetailResponse{status=COMPLETED}
deactivate Service
Controller --> UI : 51. HTTP 200 OK
deactivate Controller
UI --> M : 51a. Display HTTP 200 OK
deactivate UI

== UC-93 View Today Tasks and Care Plan ==
M -> UI : 52. Submit request
activate UI
UI -> Controller : 52a. GET /api/v1/reminders/today\nHeader: X-User-Timezone
activate Controller
Controller -> Controller : 53. resolveTimezone(header)\n[default to Asia/Ho_Chi_Minh if missing/invalid]
Controller -> TodayService : 54. getTodayTasks(callerId, timezone)
activate TodayService
TodayService -> ReminderRepo : 55. findByOwnerUserIdAndStatusNot(callerId, CANCELLED)
activate ReminderRepo
ReminderRepo -> DB : 56. SELECT * FROM reminders\nWHERE owner_user_id=? AND status<>'CANCELLED'
activate DB
DB --> ReminderRepo : 57. reminders[] (all days — not filtered by today yet)
deactivate DB
ReminderRepo --> TodayService : 58. reminders[]
deactivate ReminderRepo
TodayService -> CareTaskRepo : 59. findByAssignedToAndStatusInAndDueAtBetween(callerId,\n[OPEN,COMPLETED], startOfDay, endOfDay)
activate CareTaskRepo
CareTaskRepo -> DB : 60. SELECT * FROM care_tasks\nWHERE assigned_to=? AND status IN (...) AND due_at BETWEEN ...
activate DB
DB --> CareTaskRepo : 61. careTasks[]
deactivate DB
CareTaskRepo --> TodayService : 62. careTasks[]
deactivate CareTaskRepo
loop [63. for each Reminder, including recurring]
  TodayService -> RecurrenceService : 63. occurrenceForDate(reminder, today, timezone)\n[calculate occurrence today — support DAILY/WEEKLY/MONTHLY]
  activate RecurrenceService
  RecurrenceService --> TodayService : 64. Optional<Occurrence>\n(empty if not due today)
  deactivate RecurrenceService
end
TodayService -> TodayService : 65. combine reminder-occurrences + careTasks, sort by\npriority (VACCINATION=1 > MEDICATION=2 > APPOINTMENT=3 > CARE_TASK=4), then dueAt
TodayService --> Controller : 66. TodayTaskItem[]
deactivate TodayService
Controller --> UI : 67. HTTP 200 OK {todayTasks[]}
deactivate Controller
UI --> M : 67a. Display HTTP 200 OK {todayTasks[]}
deactivate UI

@enduml
```

**Hình 2 — Sequence Diagram: Create Reminder → Manage Lifecycle (Snooze/Complete) → View Today Tasks (Main Flow)**

> **Ghi chú grounding (quan trọng):** Bean thật sự implement `INotificationService` trong
> code hiện tại là `DummyNotificationService` — `scheduleFcmPush(...)` luôn trả về chuỗi
> cố định `"dummy-job-id"` và `cancelFcmJob(...)` là no-op. Nghĩa là pipeline "lên lịch đẩy
> FCM" (NS-08) hiện **chưa có tích hợp thật** với Firebase; toàn bộ lời gọi `Notif` trong
> sơ đồ trên phản ánh đúng cấu trúc code (interface + call sites thật), nhưng hành vi runtime
> hiện tại là stub, không gửi push thông báo thật. `updateReminder` và `snoozeReminder`
> **không** gọi `AuditService` (khác với `createReminder`/`completeReminder`/`skipReminder`/
> `deleteReminder`/`enableReminder` — các hành động này có audit). `snoozeReminder` có ràng
> buộc nghiệp vụ chưa từng được vẽ: `snoozedUntil` phải ở tương lai **và** không được vượt
> quá 24 giờ kể từ hiện tại (`REM-005`/`REM-008`). UC-93 không lọc "hôm nay" bằng SQL date —
> nó lấy **toàn bộ** reminder chưa `CANCELLED` rồi giao cho `ReminderRecurrenceService`
> tính occurrence theo timezone người dùng (hỗ trợ lịch lặp DAILY/WEEKLY/MONTHLY), khác với
> câu `SELECT ... scheduled_at::date = current_date` đơn giản ở bản vẽ cũ.


## 4. Business Rules Applied

- BR-RBAC / ownership — chỉ chủ sở hữu (`ownerUserId`) quản lý được reminder của chính họ.
- UC-90/91 — nhắc thuốc/vitamin dựa trên tư vấn chuyên môn trước đó hoặc thói quen cá nhân; nhắc tiêm liên kết `VaccinationRecord` (MF-03) nhưng không thay thế lịch chính thức.
- NS-08 — chỉ nhắc lịch đến hạn mới kích hoạt thông báo được phép (theo `NotificationPreferences`, MF-01).
- UC-93 — "Today Tasks" là công cụ tổ chức cá nhân, không phải kế hoạch điều trị hay đơn thuốc chính thức.
