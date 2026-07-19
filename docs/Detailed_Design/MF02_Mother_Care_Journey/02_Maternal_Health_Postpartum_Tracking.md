# MF-02 / Spec 02 — Maternal Health Metric & Postpartum Recovery Tracking

| Field | Value |
| --- | --- |
| Feature | MF-02 — Mother Care Journey |
| Use Cases Covered | UC-22 Add Maternal Health Metric, UC-24 View Maternal Health Trend, UC-25 Add Postpartum Recovery Log |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother records a maternal indicator (weight, blood pressure, glucose, fetal movement...) or a postpartum recovery observation (pain, mood, sleep, bleeding...) against the active journey, and views the resulting trend over time with clear source labels and non-diagnostic framing. |
| Grounding (source code) | `health/entity/MaternalHealthMetric.java`, `MetricType.java`, `MetricStatus.java`, `health/entity/PostpartumLog.java`, `PostpartumLogStatus.java`, `BleedingLevel.java`, `health/controller/JourneyMetricController.java` (`/api/v1/journeys/{journeyId}/metrics`), `health/controller/PostpartumLogController.java` (`/api/v1/journeys/{journeyId}/postpartum-logs`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Hai entity `MaternalHealthMetric` và `PostpartumLog` chia sẻ cùng một khuôn mẫu vòng
đời đơn giản (ghi nhận do người dùng nhập → ACTIVE → có thể sửa/xoá mềm), nên được gộp
vào một spec thay vì tách UC-22/23 và UC-25/26 thành bốn spec riêng. Cả hai đều gắn với
`journeyId` của `MotherJourney` (MF-02/01) và đều có `sourceType`/`sourceLabel` để phân
biệt dữ liệu người dùng tự nhập với dữ liệu đồng bộ từ thiết bị (MF-13). UC-24 (xem xu
hướng) là read-model tổng hợp nhiều `MaternalHealthMetric` theo `metricType` theo thời
gian — không phải entity riêng.

## 2. Class Diagram

```plantuml
@startuml MF02_02_MetricPostpartum_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class MotherJourney {
  + id: UUID
}

class MaternalHealthMetric {
  + id: UUID
  + journeyId: UUID
  + metricType: MetricType
  + valueNumeric: BigDecimal
  + valueSecondary: BigDecimal
  + unit: String
  + measuredAt: Instant
  + sourceType: DataSource
  + note: String
  + status: MetricStatus
}

enum MetricType {
  WEIGHT
  BLOOD_PRESSURE_SYSTOLIC
  BLOOD_PRESSURE_DIASTOLIC
  BLOOD_GLUCOSE
  FETAL_MOVEMENT_COUNT
  HEART_RATE
  SLEEP_DURATION
  STEPS_COUNT
  SPO2
  TEMPERATURE
  OTHER
}

enum MetricStatus {
  ACTIVE
  DELETED
}

class PostpartumLog {
  + id: UUID
  + journeyId: UUID
  + logDate: LocalDate
  + painLevel: Short
  + bleedingLevel: BleedingLevel
  + moodLevel: Short
  + sleepHours: BigDecimal
  + breastfeedingNote: String
  + symptomNote: String
  + status: PostpartumLogStatus
}

enum PostpartumLogStatus {
  ACTIVE
  DELETED
}

class MetricTrendResponse <<read-model>> {
  + metricType: MetricType
  + points: List<MetricDataPoint>
  + sourceLabels: Map<String, DataSource>
}

class AddMetricRequest {
  + metricType: MetricType
  + valueNumeric: BigDecimal
  + measuredAt: Instant
  + note: String
}

class JourneyMetricController {
  - healthMetricService: IHealthMetricService
  + add(journeyId, AddMetricRequest): ResponseEntity
  + trend(journeyId, metricType): ResponseEntity
}

class PostpartumLogController {
  - postpartumLogService: IPostpartumLogService
  + add(journeyId, AddPostpartumLogRequest): ResponseEntity
}

interface IHealthMetricService <<interface>> {
  + add(ownerId: UUID, journeyId: UUID, request): MaternalHealthMetric
  + trend(ownerId: UUID, journeyId: UUID, metricType): MetricTrendResponse
}

class HealthMetricServiceImpl implements IHealthMetricService {
  - maternalHealthMetricRepository: MaternalHealthMetricRepository
  - auditService: AuditService
}

MotherJourney "1" *-- "0..*" MaternalHealthMetric : records
MotherJourney "1" *-- "0..*" PostpartumLog : records
MaternalHealthMetric --> MetricType
MaternalHealthMetric --> MetricStatus
PostpartumLog --> PostpartumLogStatus
JourneyMetricController --> IHealthMetricService : uses
HealthMetricServiceImpl ..> MetricTrendResponse : builds
PostpartumLogController --> IPostpartumLogService : uses

@enduml
```

**Hình 1 — Class Diagram: Maternal Health Metric & Postpartum Recovery Log**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF02_02_MetricPostpartum_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "JourneyMetricController" as MetricController
participant "HealthMetricServiceImpl" as MetricService
participant "PostpartumLogController" as LogController
participant "PostpartumLogServiceImpl" as LogService
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-22 Add Maternal Health Metric ==
M -> MetricController : POST /api/v1/journeys/{journeyId}/metrics\n{metricType=BLOOD_PRESSURE_SYSTOLIC, valueNumeric=120}
MetricController -> MetricService : add(ownerId, journeyId, request)
MetricService -> MetricService : check ownership của journeyId
MetricService -> DB : INSERT INTO maternal_health_metrics\n(status=ACTIVE, source_type=USER_ENTERED)
MetricService -> Audit : emit(HEALTH_METRIC_ADDED)
MetricService --> MetricController : MaternalHealthMetric
MetricController --> M : HTTP 201 Created

== UC-24 View Maternal Health Trend ==
M -> MetricController : GET /api/v1/journeys/{journeyId}/metrics?metricType=BLOOD_PRESSURE_SYSTOLIC
MetricController -> MetricService : trend(ownerId, journeyId, metricType)
MetricService -> DB : SELECT * FROM maternal_health_metrics\nWHERE journey_id=? AND metric_type=? AND status='ACTIVE'\nORDER BY measured_at
DB --> MetricService : rows[]
MetricService --> MetricController : MetricTrendResponse{points[], sourceLabels}
MetricController --> M : HTTP 200 OK {trend}

== UC-25 Add Postpartum Recovery Log ==
M -> LogController : POST /api/v1/journeys/{journeyId}/postpartum-logs\n{logDate, painLevel, bleedingLevel, moodLevel, sleepHours}
LogController -> LogService : add(ownerId, journeyId, request)
LogService -> DB : INSERT INTO postpartum_logs (status=ACTIVE)
LogService -> Audit : emit(POSTPARTUM_LOG_ADDED)
LogService --> LogController : PostpartumLog
LogController --> M : HTTP 201 Created

@enduml
```

**Hình 2 — Sequence Diagram: Add Metric → View Trend → Add Postpartum Log (Main Flow)**

## 4. State Machine — `MaternalHealthMetric.status` / `PostpartumLog.status`

```plantuml
@startuml MF02_02_MetricStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : Mother ghi nhận chỉ số/nhật ký (UC-22 / UC-25)
ACTIVE --> ACTIVE : Mother sửa giá trị (UC-23 / UC-26)\n[không đổi status]
ACTIVE --> DELETED : Mother xoá bản ghi (UC-23 / UC-26)
DELETED --> [*]

note right of ACTIVE
  MaternalHealthMetric và PostpartumLog dùng chung khuôn
  mẫu 2 trạng thái: ACTIVE / DELETED (soft-delete). Không có
  trạng thái duyệt/kiểm duyệt — đây là dữ liệu riêng tư do
  chính chủ nhập, chỉ chủ sở hữu mới thấy (BR-PRIVACY).
end note

@enduml
```

**Hình 3 — State Machine: Owner-entered Record Lifecycle (`ACTIVE` → `DELETED`)**

## 5. Business Rules Applied

- BR-RBAC / BR-PRIVACY — chỉ chủ sở hữu journey mới đọc/ghi được metric và log của chính họ.
- UC-24 — trend hiển thị kèm nhãn nguồn dữ liệu (`sourceType`) và khung diễn giải phi chẩn đoán, không được trình bày như kết luận y khoa.
- UC-23/UC-26 — chỉ được sửa/xoá khi còn `ownership` và trạng thái bản ghi cho phép (không sửa được bản ghi đã `DELETED`).
