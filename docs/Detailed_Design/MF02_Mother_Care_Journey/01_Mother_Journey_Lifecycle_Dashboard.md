# MF-02 / Spec 01 — Mother Journey Lifecycle & Dashboard

| Field | Value |
| --- | --- |
| Feature | MF-02 — Mother Care Journey |
| Use Cases Covered | UC-19 Initialize Mother Care Journey, UC-20 Update Mother Journey Stage and Dates, UC-21 View Mother Journey Dashboard |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother initializes a `MotherJourney` for preconception, pregnancy or postpartum recovery, updates permitted dates/transitions, and consumes a stage-aware dashboard that aggregates metrics, reminders, checklist and reviewed content for the current stage/week. Baby care is handled independently by MF-03. |
| Grounding (source code) | `journey/entity/MotherJourney.java`, `JourneyStatus.java`, `JourneyType.java`, `journey/controller/JourneyController.java` (`/api/v1/journeys`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`MotherJourney` là entity gốc mà gần như toàn bộ MF-02, kể cả reminder ở spec-05, neo vào
(`journeyId`). Mother khởi tạo journey với `journeyType` + ngày tối thiểu cần thiết
(kỳ kinh cuối/ngày dự sinh/ngày sinh tuỳ loại) — UC-19. Khi hoàn cảnh thay đổi (ví dụ
chuyển từ PREGNANCY sang POSTPARTUM sau khi sinh), Mother cập nhật lại `journeyType`
và các ngày liên quan — UC-20. Dashboard (UC-21) không phải một entity riêng mà là một
read-model tổng hợp: tính stage/week hiện tại từ `MotherJourney`, rồi kéo dữ liệu liên
quan (metric gần nhất, reminder đến hạn, checklist, nội dung đã duyệt theo giai đoạn)
— các nguồn dữ liệu phụ này được mô hình hoá trong các spec khác (MF-02/spec-02, spec-05,
MF-09), ở đây chỉ thể hiện quan hệ tổng hợp.

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
}

enum JourneyStatus {
  ACTIVE
  COMPLETED
  ARCHIVED
}

class JourneyDashboardResponse <<read-model>> {
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
  - journeyService: IJourneyService
  + createJourney(CreateJourneyRequest): ResponseEntity
  + updateJourney(journeyId, UpdateJourneyRequest): ResponseEntity
  + getDashboard(): ResponseEntity
}

interface IJourneyService <<interface>> {
  + createJourney(request, callerId: UUID): CreateJourneyResponse
  + updateJourney(journeyId: UUID, request, callerId: UUID): JourneyResponse
  + getDashboard(userId: UUID): JourneyDashboardResponse
}

class JourneyServiceImpl implements IJourneyService {
  - journeyRepository: MotherJourneyRepository
  - metricService: IHealthMetricService
  - reminderService: ReminderService
  - checklistService: IUserChecklistItemService
  - auditService: AuditService
}

interface MotherJourneyRepository <<interface>> {
  + findByOwnerUserIdAndStatus(ownerUserId, status): List<MotherJourney>
  + save(journey): MotherJourney
}

MotherJourney --> JourneyType
MotherJourney --> JourneyStatus
JourneyController --> IJourneyService : uses
JourneyServiceImpl --> MotherJourneyRepository : uses
JourneyServiceImpl ..> JourneyDashboardResponse : builds
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
participant "Web / Mobile UI" as UI
participant "JourneyController" as Controller
participant "JourneyServiceImpl" as Service
participant "AuditService" as Audit
participant "MotherJourneyRepository" as Repo
database "PostgreSQL" as DB

== UC-19 Initialize Mother Care Journey ==
M -> UI : 1. Submit request
activate UI
UI -> Controller : 1a. POST /api/v1/journeys\n{journeyType=PREGNANCY, lastMenstrualDate, estimatedDueDate}
activate Controller
Controller -> Service : 2. create(ownerUserId, request)
activate Service
Service -> Service : 3. validate minimum days according to journeyType
Service -> Repo : 4. save(MotherJourney{status=ACTIVE})
activate Repo
Repo -> DB : 5. INSERT INTO mother_journeys ...
activate DB
DB --> Repo : 6. saved
deactivate DB
Repo --> Service : 7. MotherJourney
deactivate Repo
Service -> Audit : 8. log(JOURNEY_CREATED)
activate Audit
Audit --> Service : 9. void
deactivate Audit
Service --> Controller : 10. MotherJourney
deactivate Service
Controller --> UI : 11. HTTP 201 Created
deactivate Controller
UI --> M : 11a. Display HTTP 201 Created
deactivate UI

== UC-20 Update Mother Journey Stage and Dates ==
M -> UI : 12. Submit request
activate UI
UI -> Controller : 12a. PUT /api/v1/journeys/{journeyId}\n{journeyType=POSTPARTUM, deliveryDate}
activate Controller
Controller -> Service : 13. update(ownerUserId, journeyId, request)
activate Service
Service -> Repo : 14. findById(journeyId)
activate Repo
Repo -> DB : 15. SELECT * FROM mother_journeys WHERE id=?
activate DB
DB --> Repo : 16. journey
deactivate DB
Repo --> Service : 17. journey
deactivate Repo
Service -> Service : 18. check ownerUserId == journey.ownerUserId
Service -> Repo : 19. save(journey{journeyType, deliveryDate, updatedAt})
activate Repo
Repo -> DB : 20. UPDATE mother_journeys SET ...
activate DB
DB --> Repo : 21. updated
deactivate DB
Repo --> Service : 22. MotherJourney
deactivate Repo
Service -> Audit : 23. log(JOURNEY_UPDATED)
activate Audit
Audit --> Service : 24. void
deactivate Audit
Service --> Controller : 25. MotherJourney
deactivate Service
Controller --> UI : 26. HTTP 200 OK
deactivate Controller
UI --> M : 26a. Display HTTP 200 OK
deactivate UI

== UC-21 View Mother Journey Dashboard ==
M -> UI : 27. Submit request
activate UI
UI -> Controller : 27a. GET /api/v1/journeys/me/dashboard
activate Controller
Controller -> Service : 28. buildDashboard(ownerUserId)
activate Service
Service -> Repo : 29. findByOwnerUserIdAndStatus(ownerUserId, ACTIVE)
activate Repo
Repo -> DB : 30. SELECT * FROM mother_journeys WHERE ...
activate DB
DB --> Repo : 31. journey
deactivate DB
Repo --> Service : 32. journey
deactivate Repo
Service -> Service : 33. compute current stage/week from journeyType + dates
Service -> Service : 34. aggregate recent metrics, due reminders,\nchecklist progress, suggested content
Service --> Controller : 35. JourneyDashboardResponse
deactivate Service
Controller --> UI : 36. HTTP 200 OK {dashboard}
deactivate Controller
UI --> M : 36a. Display HTTP 200 OK {dashboard}
deactivate UI

@enduml
```

**Hình 2 — Sequence Diagram: Initialize → Update Stage → View Dashboard (Main Flow)**


## 4. Business Rules Applied

- BR-RBAC — chỉ chủ sở hữu (`ownerUserId`) mới được đọc/ghi journey của chính họ.
- UC-19 precondition — journey được tạo với "minimum dates and stage context required for stage-based support".
- UC-20 — chỉ các trường ngày/giai đoạn được liệt kê là "permitted" mới có thể cập nhật.
- UC-21 — dashboard chỉ hiển thị dữ liệu mà Mother được phép xem (không lộ dữ liệu của journey khác).
- `BABY_CARE` còn tồn tại trong enum/API cũ không phải stage của MF-02; hồ sơ và dashboard của bé thuộc MF-03.
