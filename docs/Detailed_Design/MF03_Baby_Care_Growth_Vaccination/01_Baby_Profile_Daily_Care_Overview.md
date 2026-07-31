# MF-03 / Spec 01 — Baby Profile Lifecycle & Daily Care Overview

| Field | Value |
| --- | --- |
| Feature | MF-03 — Baby Care Journey, Growth & Vaccination |
| Use Cases Covered | UC-32 Create Baby Profile, UC-33 Update or Archive Baby Profile, UC-34 Switch Active Baby Profile, UC-35 View Baby Care Overview, UC-36 Add Baby Daily Log, UC-38 View Baby Log Summary |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother creates a `BabyProfile`, selects which baby is "active" for the current session, records daily feeding/sleep/diaper observations against that baby, and reads back both a full care overview and a rolling 24h/7-day log summary. |
| Grounding (source code) | `baby/entity/BabyProfile.java`, `BabyProfileStatus.java`, `baby/controller/BabyController.java` (`/api/v1/babies`), `carejourney/entity/BabyDailyLog.java`, `carejourney/controller/BabyDailyLogController.java` (`/api/v1/babies/{babyId}/daily-logs`), `BabyCareOverviewController.java`, `BabyLogSummaryController.java` |

## 1. Tổng quan luồng chính (Main Flow Overview)

`BabyProfile` là gốc của toàn bộ MF-03. Mother tạo hồ sơ bé (UC-32), có thể cập nhật
hoặc lưu trữ (`ARCHIVED`) khi không còn theo dõi tích cực nhưng không phá huỷ lịch sử
liên quan (UC-33), và chọn baby nào là "đang theo dõi" cho dashboard hiện tại (`active`
flag — UC-34). Ghi nhật ký hằng ngày (`BabyDailyLog` — cho bú/ngủ/tã/triệu chứng — UC-36)
là hành động lặp lại nhiều nhất trong MF-03, nên gộp chung với UC-38 (tổng hợp 24h/7 ngày)
làm luồng chính; growth/milestone/vaccination được tách thành hai spec riêng (02, 03) vì
chúng có state machine và nhịp độ nghiệp vụ khác (đo định kỳ / mốc phát triển / tiêm
chủng theo lịch tham khảo).

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
BabyDailyLog --> BabyDailyLogStatus
BabyController --> IBabyService : uses
BabyDailyLogController --> BabyDailyLogService : uses
BabyServiceImpl --> BabyAccessPolicy : enforces ownership
BabyCareOverviewController ..> BabyCareOverviewResponse : builds
BabyLogSummaryController ..> BabyLogSummaryResponse : builds

@enduml
```

**Hình 1 — Class Diagram: Baby Profile, Daily Log & Care Overview Read-Models**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF03_01_BabyProfile_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "BabyController" as BabyController
participant "BabyServiceImpl" as BabyService
participant "BabyProfileRepository" as BabyRepo
participant "BabyDailyLogController" as LogController
participant "BabyDailyLogServiceImpl" as LogService
participant "BabyDailyLogRepository" as LogRepo
participant "BabyCareOverviewController" as OverviewController
participant "BabyCareOverviewServiceImpl" as OverviewService
participant "BabyLogSummaryController" as SummaryController
participant "BabyLogSummaryServiceImpl" as SummaryService
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-32 Create Baby Profile ==
M -> BabyController : 1. POST /api/v1/babies\n{nickname, birthDate, gender}
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
BabyController --> M : 10. HTTP 201 Created
deactivate BabyController

== UC-34 Switch Active Baby Profile ==
M -> BabyController : 11. PATCH /api/v1/babies/{babyId}/active
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
BabyController --> M : 24. HTTP 200 OK
deactivate BabyController

== UC-36 Add Baby Daily Log ==
M -> LogController : 25. POST /api/v1/babies/{babyId}/daily-logs\n{logType=FEEDING, startedAt, quantity, unit}
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
LogController --> M : 34. HTTP 201 Created
deactivate LogController

== UC-35 View Baby Care Overview ==
M -> OverviewController : 35. GET /api/v1/babies/{babyId}/care-overview
activate OverviewController
OverviewController -> OverviewService : 36. overview(ownerId, babyId)
activate OverviewService
OverviewService -> DB : 37. SELECT recent logs, growth, milestones, vaccination status\nWHERE baby_id=?
activate DB
DB --> OverviewService : 38. aggregated rows
deactivate DB
OverviewService --> OverviewController : 39. BabyCareOverviewResponse
deactivate OverviewService
OverviewController --> M : 40. HTTP 200 OK {BabyCareOverviewResponse}
deactivate OverviewController

== UC-38 View Baby Log Summary ==
M -> SummaryController : 41. GET /api/v1/babies/{babyId}/daily-logs/summary?window=24h
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
SummaryController --> M : 49. HTTP 200 OK {BabyLogSummaryResponse}
deactivate SummaryController

@enduml
```

**Hình 2 — Sequence Diagram: Create Profile → Switch Active → Log Daily Care → View Overview/Summary (Main Flow)**

## 4. State Machine — `BabyProfile.status`

```plantuml
@startuml MF03_01_BabyProfileStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : POST /babies (UC-32)

ACTIVE --> ARCHIVED : POST /babies/{id}/archive (UC-33)\n[không xoá dữ liệu lịch sử liên quan]
ARCHIVED --> ACTIVE : Mother phục hồi hồ sơ (UC-33)

note right of ACTIVE
  Cờ `active` (boolean, tối đa 1 baby active tại một thời điểm
  cho mỗi ownerUserId) là độc lập với `status` — dùng cho UC-34
  để chọn baby hiển thị trên dashboard, không phải trạng thái
  vòng đời hồ sơ.
end note

@enduml
```

**Hình 3 — State Machine: `BabyProfile.status` Lifecycle**

## 5. Business Rules Applied

- BR-RBAC / ownership — chỉ `ownerUserId` (và thành viên gia đình có quyền theo MF-10) mới truy cập được hồ sơ bé.
- UC-33 postcondition — archive không được phá huỷ lịch sử nhật ký/tăng trưởng/tiêm chủng liên kết.
- UC-34 — dashboard/nhắc lịch hiện tại luôn theo baby đang `active`, không phải toàn bộ danh sách baby.
- UC-38 — summary chỉ tổng hợp log ở trạng thái `ACTIVE` trong cửa sổ thời gian yêu cầu (24h hoặc 7 ngày).
