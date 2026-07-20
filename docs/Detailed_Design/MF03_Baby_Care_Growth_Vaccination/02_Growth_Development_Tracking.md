# MF-03 / Spec 02 — Growth & Development Milestone Tracking

| Field | Value |
| --- | --- |
| Feature | MF-03 — Baby Care Journey, Growth & Vaccination |
| Use Cases Covered | UC-39 Record Development Milestone, UC-41 Add Growth Measurement, UC-43 View Growth Trend and Measurement History |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother records an observed development milestone and periodic growth measurements (weight/height/head circumference) for a baby, then reviews the growth trend and measurement history with reference context. |
| Grounding (source code) | `carejourney/entity/DevelopmentMilestone.java`, `MilestoneAchievementStatus.java`, `MilestoneRecordStatus.java`, `carejourney/entity/GrowthMeasurement.java`, `carejourney/controller/MilestoneController.java` (`/api/v1/babies/{babyId}/milestones`), `GrowthMeasurementController.java` (`/api/v1/babies/{babyId}/growth-measurements`), `GrowthChartController.java` (`/api/v1/babies/{babyId}/growth-chart`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`DevelopmentMilestone` và `GrowthMeasurement` đều là quan sát định kỳ do caregiver ghi
nhận cho một `BabyProfile`, khác với `BabyDailyLog` (spec 01) ở tần suất thấp hơn và có
ý nghĩa theo dõi phát triển dài hạn. Milestone có hai chiều trạng thái độc lập: mức độ
đạt được thực tế (`MilestoneAchievementStatus`: đúng hạn hay chậm) và trạng thái bản ghi
(`MilestoneRecordStatus`: còn hiệu lực hay đã xoá). `GrowthMeasurement` chỉ có soft-delete
qua `deletedAt`. UC-43 (xem xu hướng tăng trưởng) là read-model tổng hợp các
`GrowthMeasurement` theo thời gian kèm ngữ cảnh tham chiếu, kèm gợi ý tìm chuyên gia khi
số đo lệch xa mốc tham chiếu — không phải chẩn đoán.

## 2. Class Diagram

```plantuml
@startuml MF03_02_GrowthMilestone_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class BabyProfile {
  + id: UUID
}

class DevelopmentMilestone {
  + milestoneId: UUID
  + babyId: UUID
  + milestoneType: String
  + achievedDate: LocalDate
  + note: String
  + sourceType: String
  + recordedBy: UUID
  + milestoneStatus: MilestoneAchievementStatus
  + recordStatus: MilestoneRecordStatus
}

enum MilestoneAchievementStatus {
  PENDING
  ACHIEVED
  DELAYED
}

enum MilestoneRecordStatus {
  ACTIVE
  DELETED
}

class GrowthMeasurement {
  + growthMeasurementId: UUID
  + babyId: UUID
  + measuredDate: LocalDate
  + weightKg: BigDecimal
  + heightCm: BigDecimal
  + headCircumferenceCm: BigDecimal
  + sourceType: String
  + note: String
  + deletedAt: Instant
}

class GrowthChartResponse <<read-model>> {
  + babyId: UUID
  + points: List<GrowthPoint>
  + referenceRange: ReferenceRangeContext
  + guidanceNote: String
}

class MilestoneController {
  - milestoneService: MilestoneService
  + record(babyId, request): ResponseEntity
  + list(babyId): ResponseEntity
}

class GrowthMeasurementController {
  - growthMeasurementService: GrowthMeasurementService
  + add(babyId, request): ResponseEntity
  + list(babyId): ResponseEntity
}

class GrowthChartController {
  + chart(babyId): ResponseEntity
}

interface GrowthMeasurementService <<interface>> {
  + add(ownerId: UUID, babyId: UUID, request): GrowthMeasurement
  + chart(ownerId: UUID, babyId: UUID): GrowthChartResponse
}

class GrowthMeasurementServiceImpl implements GrowthMeasurementService {
  - growthMeasurementRepository: GrowthMeasurementRepository
  - auditService: AuditService
}

BabyProfile "1" *-- "0..*" DevelopmentMilestone : has
BabyProfile "1" *-- "0..*" GrowthMeasurement : has
DevelopmentMilestone --> MilestoneAchievementStatus
DevelopmentMilestone --> MilestoneRecordStatus
MilestoneController --> MilestoneService : uses
GrowthMeasurementController --> GrowthMeasurementService : uses
GrowthChartController ..> GrowthChartResponse : builds
GrowthMeasurementServiceImpl --> AuditService : emits GROWTH_MEASUREMENT_ADDED

@enduml
```

**Hình 1 — Class Diagram: Development Milestone & Growth Measurement**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF03_02_GrowthMilestone_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "MilestoneController" as MilestoneController
participant "MilestoneServiceImpl" as MilestoneService
participant "DevelopmentMilestoneRepository" as MilestoneRepo
participant "GrowthMeasurementController" as GrowthController
participant "GrowthMeasurementServiceImpl" as GrowthService
participant "GrowthMeasurementRepository" as GrowthRepo
participant "GrowthChartController" as ChartController
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-39 Record Development Milestone ==
M -> MilestoneController : 1. POST /api/v1/babies/{babyId}/milestones\n{milestoneType="FIRST_SMILE", achievedDate}
activate MilestoneController
MilestoneController -> MilestoneService : 2. record(ownerId, babyId, request)
activate MilestoneService
MilestoneService -> MilestoneService : 3. check ownership của babyId
MilestoneService -> MilestoneRepo : 4. save(DevelopmentMilestone{milestoneStatus=ACHIEVED, recordStatus=ACTIVE})
activate MilestoneRepo
MilestoneRepo -> DB : 5. INSERT INTO development_milestones ...
activate DB
DB --> MilestoneRepo : 6. saved
deactivate DB
MilestoneRepo --> MilestoneService : 7. DevelopmentMilestone
deactivate MilestoneRepo
MilestoneService -> Audit : 8. log(MILESTONE_RECORDED)
activate Audit
Audit --> MilestoneService : 9. void
deactivate Audit
MilestoneService --> MilestoneController : 10. DevelopmentMilestone
deactivate MilestoneService
MilestoneController --> M : 11. HTTP 201 Created
deactivate MilestoneController

== UC-41 Add Growth Measurement ==
M -> GrowthController : 12. POST /api/v1/babies/{babyId}/growth-measurements\n{measuredDate, weightKg, heightCm, headCircumferenceCm}
activate GrowthController
GrowthController -> GrowthService : 13. add(ownerId, babyId, request)
activate GrowthService
GrowthService -> GrowthService : 14. check ownership của babyId
GrowthService -> GrowthRepo : 15. save(GrowthMeasurement{...})
activate GrowthRepo
GrowthRepo -> DB : 16. INSERT INTO growth_measurements ...
activate DB
DB --> GrowthRepo : 17. saved
deactivate DB
GrowthRepo --> GrowthService : 18. GrowthMeasurement
deactivate GrowthRepo
GrowthService -> Audit : 19. log(GROWTH_MEASUREMENT_ADDED)
activate Audit
Audit --> GrowthService : 20. void
deactivate Audit
GrowthService --> GrowthController : 21. GrowthMeasurement
deactivate GrowthService
GrowthController --> M : 22. HTTP 201 Created
deactivate GrowthController

== UC-43 View Growth Trend and Measurement History ==
M -> ChartController : 23. GET /api/v1/babies/{babyId}/growth-chart
activate ChartController
ChartController -> GrowthService : 24. chart(ownerId, babyId)
activate GrowthService
GrowthService -> GrowthRepo : 25. findByBabyIdAndDeletedAtIsNull(babyId)
activate GrowthRepo
GrowthRepo -> DB : 26. SELECT * FROM growth_measurements\nWHERE baby_id=? AND deleted_at IS NULL\nORDER BY measured_date
activate DB
DB --> GrowthRepo : 27. measurements[]
deactivate DB
GrowthRepo --> GrowthService : 28. measurements[]
deactivate GrowthRepo
GrowthService -> GrowthService : 29. plot trend + so sánh reference range\n(WHO/Bộ Y tế) + guidanceNote nếu lệch xa
GrowthService --> ChartController : 30. GrowthChartResponse
deactivate GrowthService
ChartController --> M : 31. HTTP 200 OK {GrowthChartResponse}
deactivate ChartController

@enduml
```

**Hình 2 — Sequence Diagram: Record Milestone → Add Growth Measurement → View Trend (Main Flow)**

## 4. State Machine — `DevelopmentMilestone.milestoneStatus` & `recordStatus`

```plantuml
@startuml MF03_02_Milestone_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

state "Achievement (milestoneStatus)" as Achievement {
  [*] --> PENDING : Mốc phát triển được kỳ vọng nhưng chưa ghi nhận
  PENDING --> ACHIEVED : Mother ghi nhận đạt mốc (UC-39)
  PENDING --> DELAYED : Quá thời điểm kỳ vọng mà chưa đạt
  DELAYED --> ACHIEVED : Mother ghi nhận đạt mốc muộn hơn
}

state "Record (recordStatus)" as Record {
  [*] --> ACTIVE : Bản ghi được tạo (UC-39)
  ACTIVE --> DELETED : Mother xoá bản ghi (UC-40)
  DELETED --> [*]
}

note bottom of Achievement
  GrowthMeasurement chỉ có 1 trục trạng thái đơn giản:
  ACTIVE (deletedAt = null) → DELETED (deletedAt set),
  không có khái niệm "achievement" vì là số đo, không phải mốc.
end note

@enduml
```

**Hình 3 — State Machine: `DevelopmentMilestone` — Achievement vs. Record Status (2 trục độc lập)**

## 5. Business Rules Applied

- BR-RBAC / ownership — chỉ chủ sở hữu baby (và gia đình có quyền theo MF-10) mới ghi/xem được.
- UC-39 — milestone là quan sát của caregiver, không phải đánh giá phát triển chính thức; luôn kèm `note` ngữ cảnh.
- UC-43 — trend hiển thị kèm reference range (WHO/tham chiếu) và nhắc tìm chuyên gia khi số đo lệch xa, không tự đưa ra kết luận chẩn đoán.
