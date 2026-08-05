# MF-03 / Spec 01 — Baby Profile Lifecycle & Daily Care Overview

| Field | Value |
| --- | --- |
| Feature | MF-03 — Baby Care Journey, Growth & Vaccination |
| Use Cases Covered | UC-32 Create Baby Profile, UC-33 Update or Archive Baby Profile, UC-34 Switch Active Baby Profile, UC-35 View Baby Care Overview, UC-36 Add Baby Daily Log, UC-38 View Baby Log Summary |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother creates a `BabyProfile`, selects which baby is active, records daily observations, and reads the care overview/summary. Medical documents for the baby reuse the shared health-record API with `babyId` and protected `fileIds`; MF-03 does not introduce a second baby-document model. |
| Grounding (source code) | `baby/entity/BabyProfile.java`, `BabyProfileStatus.java`, `baby/controller/BabyController.java` (`/api/v1/babies`), `carejourney/entity/BabyDailyLog.java`, `carejourney/controller/BabyDailyLogController.java`, `BabyCareOverviewController.java`, `BabyLogSummaryController.java`; shared implementation: `health/entity/HealthRecord.java` (`babyId`), `health/dto/AddHealthRecordRequest.java` (`babyId`, `fileIds`), `health/controller/HealthRecordController.java`, `health/service/impl/HealthRecordServiceImpl.java`, `file/entity/UploadedFile.java` |

## 1. Tổng quan luồng chính (Main Flow Overview)

`BabyProfile` là gốc của toàn bộ MF-03. Mother tạo hồ sơ bé (UC-32), có thể cập nhật
hoặc lưu trữ (`ARCHIVED`) khi không còn theo dõi tích cực nhưng không phá huỷ lịch sử
liên quan (UC-33), và chọn baby nào là "đang theo dõi" cho dashboard hiện tại (`active`
flag — UC-34). Ghi nhật ký hằng ngày (`BabyDailyLog` — cho bú/ngủ/tã/triệu chứng — UC-36)
là hành động lặp lại nhiều nhất trong MF-03, nên gộp chung với UC-38 (tổng hợp 24h/7 ngày)
làm luồng chính; growth/milestone/vaccination được tách thành hai spec riêng (02, 03) vì
chúng có vòng đời và nhịp độ nghiệp vụ khác (đo định kỳ / mốc phát triển / tiêm
chủng theo lịch tham khảo).

Hồ sơ khám, xét nghiệm hoặc đơn thuốc của bé không được lưu trong `BabyDailyLog`.
Implementation hiện tại dùng chung `HealthRecord`: request mang `babyId` để liên kết
đúng bé và `fileIds` để gắn các `UploadedFile` đã upload. Timeline có
`TimelineFilter.babyId`; attachment chỉ trả URL có thời hạn sau kiểm tra quyền. Thiết kế
API record/attachment được dùng chung ở tầng code, nhưng flow có `babyId` và quyền truy
cập của child record thuộc MF-03; MF-02/spec-04 chỉ mô tả maternal record.

## 2. Class Diagram

```plantuml
@startuml MF03_01_BabyProfile_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class BabyProfile {
  + id: UUID
  + ownerUserId: UUID
  + nickname: String
  + birthDate: LocalDate
  + gender: Gender
  + birthWeightKg: BigDecimal
  + birthLengthCm: BigDecimal
  + status: BabyProfileStatus
  + active: Boolean
}

enum BabyProfileStatus {
  ACTIVE
  ARCHIVED
}

enum Gender {
  MALE
  FEMALE
  UNKNOWN
}

class BabyDailyLog {
  + babyLogId: UUID
  + babyId: UUID
  + logType: String
  + startedAt: Instant
  + endedAt: Instant
  + quantity: BigDecimal
  + unit: String
  + note: String
  + recordedBy: UUID
  + status: BabyDailyLogStatus
}

enum BabyDailyLogStatus {
  ACTIVE
  DELETED
}

class BabyCareOverviewResponse <<read-model>> {
  + babyId: UUID
  + recentLogs: List<BabyDailyLog>
  + growthSummary: GrowthSummary
  + milestoneSummary: MilestoneSummary
  + vaccinationStatusSummary: VaccinationSummary
}

class BabyLogSummaryResponse <<read-model>> {
  + babyId: UUID
  + windowHours: int
  + feedingCount: int
  + sleepTotalMinutes: int
  + diaperCount: int
}

class HealthRecord <<shared API; MF-03 child record>> {
  + id: UUID
  + ownerUserId: UUID
  + babyId: UUID
  + recordType: RecordType
  + title: String
  + status: HealthRecordStatus
}

class UploadedFile <<attachments table>> {
  + id: UUID
  + ownerUserId: UUID
  + healthRecordId: UUID
  + status: FileStatus
}

class BabyController {
  - babyService: IBabyService
  + create(CreateBabyProfileRequest): ResponseEntity
  + update(babyId, UpdateBabyProfileRequest): ResponseEntity
  + archive(babyId): ResponseEntity
  + switchActive(babyId): ResponseEntity
}

class BabyDailyLogController {
  - babyDailyLogService: BabyDailyLogService
  + add(babyId, request): ResponseEntity
}

class BabyCareOverviewController {
  + overview(babyId): ResponseEntity
}

class BabyLogSummaryController {
  + summary(babyId): ResponseEntity
}

interface IBabyService <<interface>> {
  + create(ownerId: UUID, request): BabyProfile
  + archive(ownerId: UUID, babyId: UUID): BabyProfile
  + switchActive(ownerId: UUID, babyId: UUID): void
}

class BabyServiceImpl implements IBabyService {
  - babyProfileRepository: BabyProfileRepository
  - babyAccessPolicy: BabyAccessPolicy
  - auditService: AuditService
}

BabyProfile --> BabyProfileStatus
BabyProfile --> Gender
BabyProfile "1" *-- "0..*" BabyDailyLog : has
BabyProfile "1" <-- "0..*" HealthRecord : babyId
HealthRecord "1" <-- "0..*" UploadedFile : healthRecordId
BabyDailyLog --> BabyDailyLogStatus
BabyController --> IBabyService : uses
BabyDailyLogController --> BabyDailyLogService : uses
BabyServiceImpl --> BabyAccessPolicy : enforces ownership
BabyCareOverviewController ..> BabyCareOverviewResponse : builds
BabyLogSummaryController ..> BabyLogSummaryResponse : builds

@enduml
```

**Hình 1 — Class Diagram: Baby Profile, Daily Log, Care Overview & Shared Baby Health Records**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF03_01_BabyProfile_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "Web / Mobile UI" as UI
participant "BabyController" as BabyController
participant "BabyDailyLogController" as LogController
participant "BabyCareOverviewController" as OverviewController
participant "BabyLogSummaryController" as SummaryController
participant "HealthRecordController" as HealthController
participant "BabyServiceImpl" as BabyService
participant "BabyDailyLogServiceImpl" as LogService
participant "BabyCareOverviewServiceImpl" as OverviewService
participant "BabyLogSummaryServiceImpl" as SummaryService
participant "HealthRecordServiceImpl" as HealthService
participant "AuditService" as Audit
participant "BabyProfileRepository" as BabyRepo
participant "BabyDailyLogRepository" as LogRepo
participant "HealthRecordRepository" as HealthRepo
participant "UploadedFileRepository" as FileRepo
database "PostgreSQL" as DB

== UC-32 Create Baby Profile ==
M -> UI : 1. Submit request
activate UI
UI -> BabyController : 1a. POST /api/v1/babies\n{nickname, birthDate, gender}
activate BabyController
BabyController -> BabyService : 2. create(ownerId, request)
activate BabyService
BabyService -> BabyRepo : 3. save(BabyProfile{status=ACTIVE, active=true})
activate BabyRepo
BabyRepo -> DB : 4. INSERT INTO baby_profiles ...
activate DB
DB --> BabyRepo : 5. saved
deactivate DB
BabyRepo --> BabyService : 6. BabyProfile
deactivate BabyRepo
BabyService -> Audit : 7. log(BABY_PROFILE_CREATED)
activate Audit
Audit --> BabyService : 8. void
deactivate Audit
BabyService --> BabyController : 9. BabyProfile
deactivate BabyService
BabyController --> UI : 10. HTTP 201 Created
deactivate BabyController
UI --> M : 10a. Display HTTP 201 Created
deactivate UI

== UC-34 Switch Active Baby Profile ==
M -> UI : 11. Submit request
activate UI
UI -> BabyController : 11a. PATCH /api/v1/babies/{babyId}/active
activate BabyController
BabyController -> BabyService : 12. switchActive(ownerId, babyId)
activate BabyService
BabyService -> BabyRepo : 13. clearActiveForOwner(ownerId)
activate BabyRepo
BabyRepo -> DB : 14. UPDATE baby_profiles SET active=false\nWHERE owner_user_id=? AND active=true
activate DB
DB --> BabyRepo : 15. updated
deactivate DB
BabyRepo --> BabyService : 16. void
deactivate BabyRepo
BabyService -> BabyRepo : 17. setActive(babyId)
activate BabyRepo
BabyRepo -> DB : 18. UPDATE baby_profiles SET active=true\nWHERE id=?
activate DB
DB --> BabyRepo : 19. updated
deactivate DB
BabyRepo --> BabyService : 20. void
deactivate BabyRepo
BabyService -> Audit : 21. log(BABY_ACTIVE_PROFILE_SWITCHED)
activate Audit
Audit --> BabyService : 22. void
deactivate Audit
BabyService --> BabyController : 23. void
deactivate BabyService
BabyController --> UI : 24. HTTP 200 OK
deactivate BabyController
UI --> M : 24a. Display HTTP 200 OK
deactivate UI

== UC-36 Add Baby Daily Log ==
M -> UI : 25. Submit request
activate UI
UI -> LogController : 25a. POST /api/v1/babies/{babyId}/daily-logs\n{logType=FEEDING, startedAt, quantity, unit}
activate LogController
LogController -> LogService : 26. add(ownerId, babyId, request)
activate LogService
LogService -> LogRepo : 27. save(BabyDailyLog{status=ACTIVE})
activate LogRepo
LogRepo -> DB : 28. INSERT INTO baby_daily_logs ...
activate DB
DB --> LogRepo : 29. saved
deactivate DB
LogRepo --> LogService : 30. BabyDailyLog
deactivate LogRepo
LogService -> Audit : 31. log(BABY_LOG_ADDED)
activate Audit
Audit --> LogService : 32. void
deactivate Audit
LogService --> LogController : 33. BabyDailyLog
deactivate LogService
LogController --> UI : 34. HTTP 201 Created
deactivate LogController
UI --> M : 34a. Display HTTP 201 Created
deactivate UI

== UC-35 View Baby Care Overview ==
M -> UI : 35. Submit request
activate UI
UI -> OverviewController : 35a. GET /api/v1/babies/{babyId}/care-overview
activate OverviewController
OverviewController -> OverviewService : 36. overview(ownerId, babyId)
activate OverviewService
OverviewService -> DB : 37. SELECT recent logs, growth, milestones, vaccination status\nWHERE baby_id=?
activate DB
DB --> OverviewService : 38. aggregated rows
deactivate DB
OverviewService --> OverviewController : 39. BabyCareOverviewResponse
deactivate OverviewService
OverviewController --> UI : 40. HTTP 200 OK {BabyCareOverviewResponse}
deactivate OverviewController
UI --> M : 40a. Display HTTP 200 OK {BabyCareOverviewResponse}
deactivate UI

== UC-38 View Baby Log Summary ==
M -> UI : 41. Submit request
activate UI
UI -> SummaryController : 41a. GET /api/v1/babies/{babyId}/daily-logs/summary?window=24h
activate SummaryController
SummaryController -> SummaryService : 42. summary(ownerId, babyId, window)
activate SummaryService
SummaryService -> LogRepo : 43. findByBabyIdAndStartedAtAfter(babyId, since)
activate LogRepo
LogRepo -> DB : 44. SELECT * FROM baby_daily_logs\nWHERE baby_id=? AND started_at >= now()-interval '24h'
activate DB
DB --> LogRepo : 45. logs[]
deactivate DB
LogRepo --> SummaryService : 46. logs[]
deactivate LogRepo
SummaryService -> SummaryService : 47. aggregate feeding/sleep/diaper counts
SummaryService --> SummaryController : 48. BabyLogSummaryResponse
deactivate SummaryService
SummaryController --> UI : 49. HTTP 200 OK {BabyLogSummaryResponse}
deactivate SummaryController
UI --> M : 49a. Display HTTP 200 OK {BabyLogSummaryResponse}
deactivate UI

== Baby-linked Health Record with Attachments (shared API, MF-03 flow) ==
M -> UI : 50. Submit request
activate UI
UI -> HealthController : 50a. POST /api/v1/health-records\n{babyId, recordType, title, fileIds[]}
activate HealthController
HealthController -> HealthService : 51. addHealthRecord(request, ownerId)
activate HealthService
HealthService -> FileRepo : 52. findAllByIdInAndOwnerUserIdAndStatus(fileIds, ownerId, ACTIVE)
activate FileRepo
FileRepo -> DB : 53. SELECT owned active attachments
activate DB
DB --> FileRepo : 54. uploadedFiles[]
deactivate DB
FileRepo --> HealthService : 55. uploadedFiles[]
deactivate FileRepo
HealthService -> HealthRepo : 56. save(HealthRecord{ownerUserId, babyId})
activate HealthRepo
HealthRepo -> DB : 57. INSERT INTO health_records ...
activate DB
DB --> HealthRepo : 58. savedRecord
deactivate DB
HealthRepo --> HealthService : 59. savedRecord
deactivate HealthRepo
HealthService -> FileRepo : 60. link each attachment to healthRecordId
activate FileRepo
FileRepo -> DB : 61. UPDATE attachments SET health_record_id=?
activate DB
DB --> FileRepo : 62. linked
deactivate DB
FileRepo --> HealthService : 63. void
deactivate FileRepo
HealthService --> HealthController : 64. AddHealthRecordResponse{healthRecordId, fileIds}
deactivate HealthService
HealthController --> UI : 65. HTTP 201 Created
deactivate HealthController
UI --> M : 65a. Display HTTP 201 Created
deactivate UI

@enduml
```

**Hình 2 — Sequence Diagram: Baby Profile, Daily Care & Baby-linked Health Record (Main Flow)**


## 4. Business Rules Applied

- BR-RBAC / ownership — chỉ `ownerUserId` (và thành viên gia đình có quyền theo MF-08) mới truy cập được hồ sơ bé.
- UC-33 postcondition — archive không được phá huỷ lịch sử nhật ký/tăng trưởng/tiêm chủng liên kết.
- UC-34 — dashboard/nhắc lịch hiện tại luôn theo baby đang `active`, không phải toàn bộ danh sách baby.
- UC-38 — summary chỉ tổng hợp log ở trạng thái `ACTIVE` trong cửa sổ thời gian yêu cầu (24h hoặc 7 ngày).
- Baby-linked record — `babyId` phải trỏ tới bé mà caller được phép truy cập; `fileIds` chỉ nhận file `ACTIVE` thuộc chính caller và chỉ hỗ trợ image/PDF theo `HealthRecordServiceImpl`.
- Attachment access — file vật lý vẫn thuộc module `file`; `HealthRecordFile` chỉ là compatibility projection trên cột `attachments.health_record_id`, không phải bảng liên kết mới.
