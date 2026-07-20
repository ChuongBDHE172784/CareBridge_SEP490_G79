# MF-02 / Spec 01 — Mother Journey Lifecycle & Dashboard

| Field | Value |
| --- | --- |
| Feature | MF-02 — Mother Care Journey |
| Use Cases Covered | UC-19 Initialize Mother Care Journey, UC-20 Update Mother Journey Stage and Dates, UC-21 View Mother Journey Dashboard |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother initializes a `MotherJourney` (pre-pregnancy / pregnancy / postpartum / baby-care), updates stage and key dates as circumstances change, and consumes a stage-aware dashboard that aggregates metrics, reminders, checklist and reviewed content for the current stage/week. |
| Grounding (source code) | `journey/entity/MotherJourney.java`, `JourneyStatus.java`, `JourneyType.java`, `journey/controller/JourneyController.java` (`/api/v1/journeys`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`MotherJourney` là entity gốc mà gần như toàn bộ MF-02 và MF-09 (reminders) neo vào
(`journeyId`). Mother khởi tạo journey với `journeyType` + ngày tối thiểu cần thiết
(kỳ kinh cuối/ngày dự sinh/ngày sinh tuỳ loại) — UC-19. Khi hoàn cảnh thay đổi (ví dụ
chuyển từ PREGNANCY sang POSTPARTUM sau khi sinh), Mother cập nhật lại `journeyType`
và các ngày liên quan — UC-20. Dashboard (UC-21) không phải một entity riêng mà là một
read-model tổng hợp: tính stage/week hiện tại từ `MotherJourney`, rồi kéo dữ liệu liên
quan (metric gần nhất, reminder đến hạn, checklist, nội dung đã duyệt theo giai đoạn)
— các nguồn dữ liệu phụ này được mô hình hoá trong các spec khác (MF-02/02, MF-09,
MF-11), ở đây chỉ thể hiện quan hệ tổng hợp.

## 2. Class Diagram

```plantuml
@startuml MF02_01_JourneyLifecycle_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class MotherJourney {
  + id: UUID
  + ownerUserId: UUID
  + journeyType: JourneyType
  + startDate: LocalDate
  + lastMenstrualDate: LocalDate
  + estimatedDueDate: LocalDate
  + deliveryDate: LocalDate
  + notes: String
  + status: JourneyStatus
}

enum JourneyType {
  PRE_PREGNANCY
  PREGNANCY
  POSTPARTUM
  BABY_CARE
}

enum JourneyStatus {
  ACTIVE
  COMPLETED
  ARCHIVED
}

class MotherJourneyDashboardResponse <<read-model>> {
  + journeyId: UUID
  + journeyType: JourneyType
  + currentWeekOrStage: String
  + recentMetrics: List<MetricSummary>
  + dueReminders: List<ReminderSummary>
  + checklistProgress: ChecklistSummary
  + suggestedContent: List<ContentSummary>
}

class CreateJourneyRequest {
  + journeyType: JourneyType
  + startDate: LocalDate
  + lastMenstrualDate: LocalDate
  + estimatedDueDate: LocalDate
}

class UpdateJourneyRequest {
  + journeyType: JourneyType
  + estimatedDueDate: LocalDate
  + deliveryDate: LocalDate
  + notes: String
}

class JourneyController {
  - journeyService: JourneyService
  + create(CreateJourneyRequest): ResponseEntity
  + update(journeyId, UpdateJourneyRequest): ResponseEntity
  + myDashboard(): ResponseEntity
}

interface JourneyService <<interface>> {
  + create(ownerUserId: UUID, request): MotherJourney
  + update(ownerUserId: UUID, journeyId: UUID, request): MotherJourney
  + buildDashboard(ownerUserId: UUID): MotherJourneyDashboardResponse
}

class JourneyServiceImpl implements JourneyService {
  - journeyRepository: JourneyRepository
  - metricService: IHealthMetricService
  - reminderService: ReminderService
  - checklistService: IUserChecklistItemService
  - auditService: AuditService
}

interface JourneyRepository <<interface>> {
  + findByOwnerUserIdAndStatus(ownerUserId, status): List<MotherJourney>
  + save(journey): MotherJourney
}

MotherJourney --> JourneyType
MotherJourney --> JourneyStatus
JourneyController --> JourneyService : uses
JourneyServiceImpl --> JourneyRepository : uses
JourneyServiceImpl ..> MotherJourneyDashboardResponse : builds
JourneyServiceImpl --> AuditService : emits JOURNEY_CREATED / JOURNEY_UPDATED

@enduml
```

**Hình 1 — Class Diagram: Mother Journey Lifecycle & Dashboard Read-Model**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF02_01_JourneyLifecycle_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "JourneyController" as Controller
participant "JourneyServiceImpl" as Service
participant "JourneyRepository" as Repo
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-19 Initialize Mother Care Journey ==
M -> Controller : POST /api/v1/journeys\n{journeyType=PREGNANCY, lastMenstrualDate, estimatedDueDate}
Controller -> Service : create(ownerUserId, request)
Service -> Service : validate ngày tối thiểu theo journeyType
Service -> Repo : save(MotherJourney{status=ACTIVE})
Repo -> DB : INSERT INTO mother_journeys ...
Service -> Audit : emit(JOURNEY_CREATED)
Service --> Controller : MotherJourney
Controller --> M : HTTP 201 Created

== UC-20 Update Mother Journey Stage and Dates ==
M -> Controller : PUT /api/v1/journeys/{journeyId}\n{journeyType=POSTPARTUM, deliveryDate}
Controller -> Service : update(ownerUserId, journeyId, request)
Service -> Repo : findById(journeyId)
Repo -> DB : SELECT * FROM mother_journeys WHERE id=?
DB --> Repo : journey
Service -> Service : check ownerUserId == journey.ownerUserId
Service -> Repo : save(journey{journeyType, deliveryDate, updatedAt})
Repo -> DB : UPDATE mother_journeys SET ...
Service -> Audit : emit(JOURNEY_UPDATED)
Service --> Controller : MotherJourney
Controller --> M : HTTP 200 OK

== UC-21 View Mother Journey Dashboard ==
M -> Controller : GET /api/v1/journeys/me/dashboard
Controller -> Service : buildDashboard(ownerUserId)
Service -> Repo : findByOwnerUserIdAndStatus(ownerUserId, ACTIVE)
Repo -> DB : SELECT * FROM mother_journeys WHERE ...
DB --> Repo : journey
Service -> Service : compute current stage/week from journeyType + dates
Service -> Service : aggregate recent metrics, due reminders,\nchecklist progress, suggested content
Service --> Controller : MotherJourneyDashboardResponse
Controller --> M : HTTP 200 OK {dashboard}

@enduml
```

**Hình 2 — Sequence Diagram: Initialize → Update Stage → View Dashboard (Main Flow)**

## 4. State Machine — `MotherJourney.status`

```plantuml
@startuml MF02_01_JourneyStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : POST /journeys (UC-19)

ACTIVE --> ACTIVE : journeyType thay đổi\nPRE_PREGNANCY → PREGNANCY → POSTPARTUM → BABY_CARE (UC-20)
ACTIVE --> COMPLETED : Mother đánh dấu hành trình đã hoàn tất
ACTIVE --> ARCHIVED : Mother lưu trữ hành trình không còn theo dõi

COMPLETED --> ARCHIVED : dọn dẹp/lưu trữ theo thời gian
ARCHIVED --> [*]

note right of ACTIVE
  journeyType là một chiều dữ liệu độc lập với status —
  một journey ACTIVE có thể đổi journeyType nhiều lần (UC-20)
  mà không đổi status.
end note

@enduml
```

**Hình 3 — State Machine: `MotherJourney.status` Lifecycle**

## 5. Business Rules Applied

- BR-RBAC — chỉ chủ sở hữu (`ownerUserId`) mới được đọc/ghi journey của chính họ.
- UC-19 precondition — journey được tạo với "minimum dates and stage context required for stage-based support".
- UC-20 — chỉ các trường ngày/giai đoạn được liệt kê là "permitted" mới có thể cập nhật.
- UC-21 — dashboard chỉ hiển thị dữ liệu mà Mother được phép xem (không lộ dữ liệu của journey khác).
