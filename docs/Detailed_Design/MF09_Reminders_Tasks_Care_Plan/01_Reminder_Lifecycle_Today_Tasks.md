# MF-09 / Spec 01 — Reminder Lifecycle & Today Tasks

| Field | Value |
| --- | --- |
| Feature | MF-09 — Reminders, Tasks & Care Plan |
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
đến hạn trong ngày — đây là điểm giao giữa MF-09 và MF-10 (Family Sync), không tạo entity
mới.

## 2. Class Diagram

```plantuml
@startuml MF09_01_Reminder_ClassDiagram
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
@startuml MF09_01_Reminder_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "ReminderController" as Controller
participant "ReminderServiceImpl" as Service
participant "INotificationService" as Notif
participant "TodayTaskServiceImpl" as TodayService
database "PostgreSQL" as DB

== UC-89/90/91 Create Reminder (Appointment / Medication / Vaccination) ==
M -> Controller : POST /api/v1/reminders\n{reminderType=APPOINTMENT, title, scheduledAt}
Controller -> Service : create(ownerId, request)
Service -> DB : INSERT INTO reminders (status=PENDING)
Service -> Notif : scheduleJob(reminderId, scheduledAt) → NS-08
Notif --> Service : fcmJobId
Service -> DB : UPDATE reminders SET fcm_job_id=?
Service --> Controller : Reminder{status=PENDING}
Controller --> M : HTTP 201 Created

== UC-92 Update, Snooze, Complete, Skip or Delete Care Reminder ==
M -> Controller : PATCH /api/v1/reminders/{id}/snooze\n{snoozedUntil}
Controller -> Service : snooze(ownerId, id, until)
Service -> DB : UPDATE reminders SET status='SNOOZED', snoozed_until=?
Service --> Controller : Reminder{status=SNOOZED}
Controller --> M : HTTP 200 OK

M -> Controller : PATCH /api/v1/reminders/{id}/complete
Controller -> Service : complete(ownerId, id)
Service -> DB : UPDATE reminders SET status='COMPLETED'
Service --> Controller : Reminder{status=COMPLETED}
Controller --> M : HTTP 200 OK

== UC-93 View Today Tasks and Care Plan ==
M -> Controller : GET /api/v1/reminders/today
Controller -> TodayService : today(ownerId)
TodayService -> DB : SELECT * FROM reminders\nWHERE owner_user_id=? AND status<>'CANCELLED'\nAND scheduled_at::date = current_date
TodayService -> DB : SELECT * FROM care_tasks\nWHERE assigned_to=? AND status IN (...) AND due_at BETWEEN ...
DB --> TodayService : reminders[], careTasks[]
TodayService -> TodayService : merge + sắp xếp theo priority/dueAt
TodayService --> Controller : TodayTaskItem[]
Controller --> M : HTTP 200 OK {todayTasks[]}

@enduml
```

**Hình 2 — Sequence Diagram: Create Reminder → Manage Lifecycle (Snooze/Complete) → View Today Tasks (Main Flow)**

## 4. State Machine — `Reminder.status`

```plantuml
@startuml MF09_01_ReminderStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> PENDING : Tạo nhắc lịch (UC-89 / UC-90 / UC-91)

PENDING --> SNOOZED : PATCH /snooze (UC-92)
SNOOZED --> PENDING : Hết thời gian hoãn\n[quay lại chờ đến hạn]
PENDING --> COMPLETED : PATCH /complete (UC-92)
SNOOZED --> COMPLETED : PATCH /complete (UC-92)
PENDING --> SKIPPED : POST hoặc PATCH /skip (UC-92)
SNOOZED --> SKIPPED : POST hoặc PATCH /skip (UC-92)
PENDING --> CANCELLED : DELETE /{id} (UC-92)
SNOOZED --> CANCELLED : DELETE /{id} (UC-92)

COMPLETED --> [*]
SKIPPED --> [*]
CANCELLED --> [*]

note right of PENDING
  Nhắc lịch có recurrenceType khác NONE sẽ tự tạo occurrence
  PENDING tiếp theo sau khi occurrence hiện tại COMPLETED/SKIPPED
  (không đổi status của bản ghi gốc — xử lý ở tầng scheduler NS-08).
end note

@enduml
```

**Hình 3 — State Machine: `Reminder.status` Lifecycle**

## 5. Business Rules Applied

- BR-RBAC / ownership — chỉ chủ sở hữu (`ownerUserId`) quản lý được reminder của chính họ.
- UC-90/91 — nhắc thuốc/vitamin dựa trên tư vấn chuyên môn trước đó hoặc thói quen cá nhân; nhắc tiêm liên kết `VaccinationRecord` (MF-03) nhưng không thay thế lịch chính thức.
- NS-08 — chỉ nhắc lịch đến hạn mới kích hoạt thông báo được phép (theo `NotificationPreferences`, MF-01).
- UC-93 — "Today Tasks" là công cụ tổ chức cá nhân, không phải kế hoạch điều trị hay đơn thuốc chính thức.
