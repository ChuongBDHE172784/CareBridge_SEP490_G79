# MF-03 / Spec 03 — Vaccination Record Management & Reference Schedule

| Field | Value |
| --- | --- |
| Feature | MF-03 — Baby Care Journey, Growth & Vaccination |
| Use Cases Covered | UC-44 Manage Vaccination Record, UC-45 View Vaccination Reference Schedule and Reminder Status |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother records a vaccination for a baby against a reference schedule entry, can mark it completed or postpone it with a reason, and views the reference schedule alongside recorded/reminder status for the selected baby. |
| Grounding (source code) | `vaccination/entity/VaccinationRecord.java`, `VaccinationRecordStatus.java`, `vaccination/entity/VaccinationReferenceSchedule.java`, `vaccination/controller/VaccinationController.java` (`/api/v1/vaccination`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`VaccinationReferenceSchedule` là dữ liệu tham chiếu dùng chung (tên vắc-xin, số mũi,
`offsetDays` kể từ ngày sinh) do hệ thống cung cấp, không do Mother chỉnh sửa.
`VaccinationRecord` là bản ghi thực tế của một baby cho một mũi tiêm cụ thể, có
`scheduledDate` (suy ra từ reference schedule + `birthDate`) và `administeredDate`
(điền khi tiêm thực tế). Mother có thể thêm bản ghi, đánh dấu hoàn thành
(`POSTMapping .../completions`) hoặc hoãn kèm lý do (`POSTMapping .../postponements`).
UC-45 kết hợp reference schedule với các `VaccinationRecord` đã có để hiển thị trạng
thái tổng hợp theo từng mũi (đã tiêm/đang chờ/quá hạn) và trạng thái reminder liên kết
(MF-09) — không tạo entity reminder riêng ở đây.

## 2. Class Diagram

```plantuml
@startuml MF03_03_Vaccination_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class BabyProfile {
  + id: UUID
  + birthDate: LocalDate
}

class VaccinationReferenceSchedule <<reference data>> {
  + id: UUID
  + vaccineName: String
  + doseNumber: short
  + offsetDays: int
  + description: String
}

class VaccinationRecord {
  + id: UUID
  + babyId: UUID
  + vaccineName: String
  + doseNumber: Short
  + scheduledDate: LocalDate
  + administeredDate: LocalDate
  + status: VaccinationRecordStatus
  + facilityName: String
  + proofRecordId: UUID
  + postponeReason: String
}

enum VaccinationRecordStatus {
  SCHEDULED
  COMPLETED
  POSTPONED
  DELETED
}

class VaccinationScheduleStatusResponse <<read-model>> {
  + babyId: UUID
  + entries: List<ScheduleEntryStatus>
}

class VaccinationController {
  - vaccinationService: VaccinationService
  + listRecords(babyId): ResponseEntity
  + referenceSchedule(babyId): ResponseEntity
  + addRecord(babyId, request): ResponseEntity
  + markCompleted(babyId, request): ResponseEntity
  + postpone(babyId, request): ResponseEntity
}

interface VaccinationService <<interface>> {
  + addRecord(ownerId: UUID, babyId: UUID, request): VaccinationRecord
  + markCompleted(ownerId: UUID, babyId: UUID, recordId: UUID): VaccinationRecord
  + postpone(ownerId: UUID, babyId: UUID, recordId: UUID, reason: String): VaccinationRecord
  + scheduleStatus(ownerId: UUID, babyId: UUID): VaccinationScheduleStatusResponse
}

class VaccinationServiceImpl implements VaccinationService {
  - vaccinationRecordRepository: VaccinationRecordRepository
  - vaccinationReferenceScheduleRepository: VaccinationReferenceScheduleRepository
  - reminderService: ReminderService
  - auditService: AuditService
}

BabyProfile "1" *-- "0..*" VaccinationRecord : has
VaccinationRecord --> VaccinationRecordStatus
VaccinationReferenceSchedule ..> VaccinationRecord : suy ra scheduledDate
VaccinationController --> VaccinationService : uses
VaccinationServiceImpl ..> VaccinationScheduleStatusResponse : builds
VaccinationServiceImpl --> ReminderService : liên kết trạng thái nhắc lịch (MF-09)

@enduml
```

**Hình 1 — Class Diagram: Vaccination Record vs. Reference Schedule**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF03_03_Vaccination_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "VaccinationController" as Controller
participant "VaccinationServiceImpl" as Service
participant "VaccinationRecordRepository" as RecordRepo
participant "VaccinationReferenceRepository" as RefRepo
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-44 Manage Vaccination Record (Add) ==
M -> Controller : 1. POST /api/v1/vaccination/babies/{babyId}/records\n{vaccineName, doseNumber, scheduledDate}
activate Controller
Controller -> Service : 2. addRecord(ownerId, babyId, request)
activate Service
Service -> RecordRepo : 3. save(VaccinationRecord{status=SCHEDULED})
activate RecordRepo
RecordRepo -> DB : 4. INSERT INTO vaccination_records ...
activate DB
DB --> RecordRepo : 5. saved
deactivate DB
RecordRepo --> Service : 6. VaccinationRecord
deactivate RecordRepo
Service -> Audit : 7. log(VACCINATION_RECORD_ADDED)
activate Audit
Audit --> Service : 8. void
deactivate Audit
Service --> Controller : 9. VaccinationRecord
deactivate Service
Controller --> M : 10. HTTP 201 Created
deactivate Controller

== UC-44 Manage Vaccination Record (Complete) ==
M -> Controller : 11. POST /api/v1/vaccination/babies/{babyId}/completions\n{recordId, administeredDate, facilityName}
activate Controller
Controller -> Service : 12. markCompleted(ownerId, babyId, recordId)
activate Service
Service -> RecordRepo : 13. save(record{status=COMPLETED, administeredDate})
activate RecordRepo
RecordRepo -> DB : 14. UPDATE vaccination_records\nSET status='COMPLETED', administered_date=?
activate DB
DB --> RecordRepo : 15. updated
deactivate DB
RecordRepo --> Service : 16. VaccinationRecord
deactivate RecordRepo
Service -> Audit : 17. log(VACCINATION_COMPLETED)
activate Audit
Audit --> Service : 18. void
deactivate Audit
Service --> Controller : 19. VaccinationRecord
deactivate Service
Controller --> M : 20. HTTP 200 OK
deactivate Controller

== UC-44 Manage Vaccination Record (Postpone) ==
M -> Controller : 21. POST /api/v1/vaccination/babies/{babyId}/postponements\n{recordId, postponeReason}
activate Controller
Controller -> Service : 22. postpone(ownerId, babyId, recordId, reason)
activate Service
Service -> RecordRepo : 23. save(record{status=POSTPONED, postponeReason})
activate RecordRepo
RecordRepo -> DB : 24. UPDATE vaccination_records\nSET status='POSTPONED', postpone_reason=?
activate DB
DB --> RecordRepo : 25. updated
deactivate DB
RecordRepo --> Service : 26. VaccinationRecord
deactivate RecordRepo
Service -> Audit : 27. log(VACCINATION_POSTPONED)
activate Audit
Audit --> Service : 28. void
deactivate Audit
Service --> Controller : 29. VaccinationRecord
deactivate Service
Controller --> M : 30. HTTP 200 OK
deactivate Controller

== UC-45 View Vaccination Reference Schedule and Reminder Status ==
M -> Controller : 31. GET /api/v1/vaccination/babies/{babyId}/schedule
activate Controller
Controller -> Service : 32. scheduleStatus(ownerId, babyId)
activate Service
Service -> RefRepo : 33. findAll()
activate RefRepo
RefRepo -> DB : 34. SELECT * FROM vaccination_reference_schedule
activate DB
DB --> RefRepo : 35. referenceEntries[]
deactivate DB
RefRepo --> Service : 36. referenceEntries[]
deactivate RefRepo
Service -> RecordRepo : 37. findByBabyId(babyId)
activate RecordRepo
RecordRepo -> DB : 38. SELECT * FROM vaccination_records WHERE baby_id=?
activate DB
DB --> RecordRepo : 39. records[]
deactivate DB
RecordRepo --> Service : 40. records[]
deactivate RecordRepo
Service -> Service : 41. ghép reference + record theo (vaccineName, doseNumber)\n+ lấy trạng thái reminder liên kết
Service --> Controller : 42. VaccinationScheduleStatusResponse
deactivate Service
Controller --> M : 43. HTTP 200 OK {entries[]}
deactivate Controller

@enduml
```

**Hình 2 — Sequence Diagram: Add → Complete / Postpone → View Schedule Status (Main Flow)**

## 4. State Machine — `VaccinationRecord.status`

```plantuml
@startuml MF03_03_VaccinationStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> SCHEDULED : Mother thêm bản ghi theo lịch tham khảo (UC-44)

SCHEDULED --> COMPLETED : POST /completions\n[administeredDate được ghi nhận]
SCHEDULED --> POSTPONED : POST /postponements\n[postponeReason bắt buộc]
POSTPONED --> SCHEDULED : Mother đặt lại lịch mới
POSTPONED --> COMPLETED : Mother ghi nhận đã tiêm sau khi hoãn

SCHEDULED --> DELETED : Mother xoá bản ghi nhập sai (UC-44)
COMPLETED --> DELETED : Mother xoá bản ghi nhập sai (UC-44)
DELETED --> [*]

@enduml
```

**Hình 3 — State Machine: `VaccinationRecord.status` Lifecycle**

## 5. Business Rules Applied

- BR-RBAC / ownership — chỉ chủ sở hữu baby (và gia đình có quyền theo MF-10) truy cập được.
- UC-44 — bản ghi tiêm chủng là dữ liệu người dùng nhập (user-entered), không phải hồ sơ tiêm chủng chính thức/EMR.
- UC-45 — trạng thái reminder hiển thị chỉ mang tính tổ chức cá nhân (personal organization), không thay thế lịch tiêm chính thức của cơ sở y tế.
- Excluded (mục 4.8 SRS) — không có chức năng dispatch/đặt lịch chính thức với cơ sở y tế, chỉ là công cụ theo dõi cá nhân.
