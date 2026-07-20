# MF-14 / Spec 01 — Smart Activity Monitoring: Configuration & Enable/Disable

| Field | Value |
| --- | --- |
| Feature | MF-14 — Smart Activity Monitoring & Safety Support |
| Use Cases Covered | UC-116 Manage Emergency Contacts, UC-117 Configure Smart Activity Monitoring, UC-118 Enable Smart Activity Monitoring, UC-119 Disable Smart Activity Monitoring |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother configures emergency contacts and monitoring sensitivity/consent, then starts an `ImuMonitoringSession` that runs until she explicitly stops it. This is the setup half of MF-14; the detection/response half (fall/impact handling) is spec 02. |
| Grounding (source code) | `emergency/entity/EmergencyContact.java` (dùng chung với MF-07), `safety/entity/SafetyMonitoringConfig.java`, `safety/SensitivityLevel.java`, `safety/entity/ImuMonitoringSession.java`, `safety/ImuSessionStatus.java`, `safety/controller/SafetyConfigController.java` (`/api/v1/safety/config`), `safety/controller/FallDetectionController.java` (`/api/v1/safety/fall-detection/enable|disable`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`EmergencyContact` (UC-116) dùng chung entity với MF-07 (chia sẻ vị trí khẩn cấp) —
đây là danh sách người nhận cảnh báo cho **cả hai** tính năng, không phải hai danh sách
tách biệt. `SafetyMonitoringConfig` (UC-117) là cấu hình 1-1 theo `userId`: bật/tắt phát
hiện ngã (`fallDetectionEnabled`), mức nhạy (`SensitivityLevel` → ngưỡng gia tốc cụ thể
qua `getThreshold()`), và `emergencyAutoAlert` (tự động gửi cảnh báo hay chờ xác nhận).
Bật giám sát (UC-118) tạo một `ImuMonitoringSession` mới ở trạng thái `ACTIVE`, dùng
`sensitivityLevel` hiện tại của config; tắt (UC-119) dừng session (`STOPPED`) và — theo
đúng mô tả UC-119 — **ngăn tạo candidate safety event mới** sau thời điểm đó.

## 2. Class Diagram

```plantuml
@startuml MF14_01_MonitoringConfig_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class EmergencyContact <<dùng chung với MF-07>> {
  + id: UUID
  + userId: UUID
  + name: String
  + phone: String
  + relationship: String
  + primaryContact: boolean
}

class SafetyMonitoringConfig {
  + id: UUID
  + userId: UUID
  + fallDetectionEnabled: boolean
  + sensitivityLevel: SensitivityLevel
  + emergencyAutoAlert: boolean
  + updatedBy: UUID
}

enum SensitivityLevel {
  LOW
  MEDIUM
  HIGH
  + getThreshold(): double
}

class ImuMonitoringSession {
  + id: UUID
  + userId: UUID
  + status: ImuSessionStatus
  + sensitivityLevel: String
  + startedAt: Instant
  + endedAt: Instant
}

enum ImuSessionStatus {
  ACTIVE
  STOPPED
}

class EmergencyContactController {
  + myContacts(): ResponseEntity
  + updateContacts(request): ResponseEntity
}

class SafetyConfigController {
  - safetyConfigService: ISafetyConfigService
  + getConfig(): ResponseEntity
  + updateConfig(request): ResponseEntity
}

class FallDetectionController {
  - fallDetectionService: IFallDetectionService
  - safetyConfigService: ISafetyConfigService
  + enable(): ResponseEntity
  + disable(): ResponseEntity
}

interface ISafetyConfigService <<interface>> {
  + getConfig(userId: UUID): SafetyMonitoringConfig
  + updateConfig(userId: UUID, request): SafetyMonitoringConfig
}

interface IFallDetectionService <<interface>> {
  + enable(userId: UUID, sensitivityLevel: String): ImuMonitoringSessionResponse
  + disable(userId: UUID): void
}

class FallDetectionServiceImpl implements IFallDetectionService {
  - imuMonitoringSessionRepository: ImuMonitoringSessionRepository
  - safetyEventRepository: SafetyEventRepository
}

SafetyMonitoringConfig --> SensitivityLevel
ImuMonitoringSession --> ImuSessionStatus
EmergencyContactController ..> EmergencyContact : manages
SafetyConfigController --> ISafetyConfigService : uses
FallDetectionController --> ISafetyConfigService : reads sensitivityLevel
FallDetectionController --> IFallDetectionService : uses
FallDetectionServiceImpl ..> ImuMonitoringSession : creates/stops

@enduml
```

**Hình 1 — Class Diagram: Emergency Contact, Safety Monitoring Config & IMU Session**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF14_01_MonitoringConfig_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "EmergencyContactController" as ContactController
participant "EmergencyContactService" as ContactService
participant "IEmergencyContactRepository" as ContactRepo
participant "SafetyConfigController" as ConfigController
participant "SafetyConfigService" as ConfigService
participant "ISafetyConfigRepository" as ConfigRepo
participant "FallDetectionController" as FallController
participant "FallDetectionService" as FallService
participant "IImuMonitoringSessionRepository" as SessionRepo
database "PostgreSQL" as DB

== UC-116 Manage Emergency Contacts (only 1 contact per account) ==
M -> ContactController : 1. PUT /api/v1/emergency/contact\n{name, phone, relationship, primaryContact}
activate ContactController
ContactController -> ContactService : 2. upsertContact(userId, request)
activate ContactService
ContactService -> ContactRepo : 3. findByUserId(userId) [find existing record if any]
activate ContactRepo
ContactRepo -> DB : 4. SELECT * FROM emergency_contacts WHERE user_id=?
activate DB
DB --> ContactRepo : 5. existing | none
deactivate DB
ContactRepo --> ContactService : 6. Optional<EmergencyContact>
deactivate ContactRepo
ContactService -> ContactService : 7. create new if not exists, or update fields on existing record\n[1 user ONLY has 1 EmergencyContact — not a list of multiple contacts]
ContactService -> ContactRepo : 8. save(contact{...})
activate ContactRepo
ContactRepo -> DB : 9. INSERT/UPDATE emergency_contacts ...
activate DB
DB --> ContactRepo : 10. saved
deactivate DB
ContactRepo --> ContactService : 11. EmergencyContact
deactivate ContactRepo
ContactService --> ContactController : 12. EmergencyContactResponse
deactivate ContactService
ContactController --> M : 13. HTTP 200 OK {contact}
deactivate ContactController

== UC-117 Configure Smart Activity Monitoring ==
M -> ConfigController : 14. PUT /api/v1/safety/config\n{fallDetectionEnabled=true, sensitivityLevel=MEDIUM, emergencyAutoAlert=false}
activate ConfigController
ConfigController -> ConfigService : 15. configure(request, userId)
activate ConfigService
ConfigService -> ConfigRepo : 16. findByUserId(userId)
activate ConfigRepo
ConfigRepo -> DB : 17. SELECT * FROM safety_monitoring_configs WHERE user_id=?
activate DB
DB --> ConfigRepo : 18. existing | none
deactivate DB
ConfigRepo --> ConfigService : 19. Optional<SafetyMonitoringConfig>
deactivate ConfigRepo
ConfigService -> ConfigService : 20. create new if not exists, or update fields on existing record\n(upsert 1-1 by userId)
ConfigService -> ConfigRepo : 21. save(config{...})
activate ConfigRepo
ConfigRepo -> DB : 22. INSERT/UPDATE safety_monitoring_configs ...
activate DB
DB --> ConfigRepo : 23. saved
deactivate DB
ConfigRepo --> ConfigService : 24. SafetyMonitoringConfig
deactivate ConfigRepo
ConfigService -> ConfigService : 25. publishEvent(SafetyConfigChanged)
ConfigService --> ConfigController : 26. SafetyConfigResponse
deactivate ConfigService
ConfigController --> M : 27. HTTP 200 OK {config}
deactivate ConfigController

== UC-118 Enable Smart Activity Monitoring ==
M -> FallController : 28. POST /api/v1/safety/fall-detection/enable
activate FallController
FallController -> ConfigService : 29. getConfig(userId)\n[get current sensitivityLevel]
activate ConfigService
ConfigService -> ConfigRepo : 30. findByUserId(userId)
activate ConfigRepo
ConfigRepo -> DB : 31. SELECT * FROM safety_monitoring_configs WHERE user_id=?
activate DB
DB --> ConfigRepo : 32. existing | none
deactivate DB
ConfigRepo --> ConfigService : 33. Optional<SafetyMonitoringConfig>
deactivate ConfigRepo
ConfigService --> FallController : 34. SafetyConfigResponse{sensitivityLevel}\n(default fallDetectionEnabled=false, sensitivityLevel=MEDIUM,\nemergencyAutoAlert=true IF NOT yet configured — no 404)
deactivate ConfigService
FallController -> FallService : 35. enable(userId, sensitivityLevel)
activate FallService
FallService -> SessionRepo : 36. findActiveByUserId(userId)\n[idempotent — avoid 2 parallel ACTIVE sessions]
activate SessionRepo
SessionRepo -> DB : 37. SELECT * FROM imu_monitoring_sessions\nWHERE user_id=? AND status='ACTIVE'
activate DB
DB --> SessionRepo : 38. existing | none
deactivate DB
SessionRepo --> FallService : 39. Optional<ImuMonitoringSession>
deactivate SessionRepo
alt 40. no ACTIVE session yet (main flow — create new session)
  FallService -> SessionRepo : 40. save(ImuMonitoringSession{status=ACTIVE,\nsensitivityLevel, startedAt=now()})
  activate SessionRepo
  SessionRepo -> DB : 41. INSERT INTO imu_monitoring_sessions ...
  activate DB
  DB --> SessionRepo : 42. saved
  deactivate DB
  SessionRepo --> FallService : 43. ImuMonitoringSession
  deactivate SessionRepo
  FallService -> FallService : 44. publishEvent(FallDetectionEnabled)
  FallService --> FallController : 45. ImuMonitoringSessionResponse{status=ACTIVE}
  deactivate FallService
  FallController --> M : 46. HTTP 201 Created
  deactivate FallController
else 40. ACTIVE session already exists → idempotent
  FallService --> FallController : 40a. return running session (DO NOT create new)
  deactivate FallService
  FallController --> M : 40b. HTTP 201 Created (idempotent)
  deactivate FallController
end

== UC-119 Disable Smart Activity Monitoring ==
M -> FallController : 47. POST /api/v1/safety/fall-detection/disable
activate FallController
FallController -> FallService : 48. disable(userId)
activate FallService
FallService -> SessionRepo : 49. findActiveByUserId(userId)
activate SessionRepo
SessionRepo -> DB : 50. SELECT * FROM imu_monitoring_sessions\nWHERE user_id=? AND status='ACTIVE'
activate DB
DB --> SessionRepo : 51. existing | none
deactivate DB
SessionRepo --> FallService : 52. Optional<ImuMonitoringSession>
deactivate SessionRepo
opt 53. has running ACTIVE session
  FallService -> SessionRepo : 53. save(session{status=STOPPED, endedAt=now()})
  activate SessionRepo
  SessionRepo -> DB : 54. UPDATE imu_monitoring_sessions\nSET status='STOPPED', ended_at=now()
  activate DB
  DB --> SessionRepo : 55. updated
  deactivate DB
  SessionRepo --> FallService : 56. ImuMonitoringSession
  deactivate SessionRepo
  FallService -> FallService : 57. publishEvent(FallDetectionDisabled)
end
FallService --> FallController : 58. void\n[silent if there was no ACTIVE session — NOT an error]
deactivate FallService
FallController --> M : 59. HTTP 200 OK
deactivate FallController
note right of FallService
  After STOPPED, endpoint /imu-data (spec 02) no longer creates
  new SafetyEvent for this user — findActiveByUserId() returns empty
  hence processImuData() throws 409 SAFETY-006.
end note

@enduml
```

**Hình 2 — Sequence Diagram: Configure Contact/Sensitivity → Enable (idempotent) → Disable (silent no-op) Monitoring (Main Flow)**

> **Ghi chú grounding (quan trọng):**
> 1. `EmergencyContactService.upsertContact` chỉ quản lý **MỘT** `EmergencyContact` cho mỗi
>    user (tra cứu bằng `findByUserId`, 1-1) — request body là một object đơn
>    `{name, phone, relationship, primaryContact}`, **không phải mảng** `contacts: [...]`
>    như bản vẽ trước ngụ ý. UC-116 thực chất là "cấu hình một liên hệ khẩn cấp duy nhất",
>    không phải quản lý danh sách nhiều liên hệ.
> 2. Tên class thật là `SafetyConfigService` và `FallDetectionService` (không có hậu tố
>    `Impl` như class diagram mục 2 nêu). `getConfig()` trả về **giá trị mặc định hợp lý**
>    (`fallDetectionEnabled=false`, `sensitivityLevel=MEDIUM`, `emergencyAutoAlert=true`) khi
>    user chưa từng cấu hình — không ném lỗi 404.
> 3. `enable()` idempotent (trả lại session `ACTIVE` sẵn có thay vì tạo trùng);
>    `disable()` **im lặng no-op** nếu không có session `ACTIVE` nào đang chạy (không ném
>    lỗi) — khác với các "disable" khác trong hệ thống (ví dụ MF13) thường ném lỗi conflict
>    khi gọi lại trên trạng thái đã tắt.

## 4. State Machine — `ImuMonitoringSession.status`

```plantuml
@startuml MF14_01_ImuSession_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : POST /fall-detection/enable (UC-118)\n[dùng sensitivityLevel từ SafetyMonitoringConfig]

ACTIVE --> STOPPED : POST /fall-detection/disable (UC-119)\n[endedAt = now()]
STOPPED --> ACTIVE : Mother bật lại giám sát (UC-118)\n[tạo session mới]

STOPPED --> [*]

note right of ACTIVE
  Chỉ khi session ở ACTIVE, endpoint /imu-data (spec 02)
  mới xử lý và có thể tạo SafetyEvent candidate. Đây là
  2 trạng thái đúng theo ImuSessionStatus thật trong code.
end note

@enduml
```

**Hình 3 — State Machine: `ImuMonitoringSession.status` Lifecycle**

## 5. Business Rules Applied

- BR-RBAC / ownership — chỉ Mother (`hasRole('MOTHER')`) cấu hình và bật/tắt giám sát của chính mình.
- UC-116 — liên hệ khẩn cấp (1 bản ghi/user) dùng chung entity với MF-07 (gia đình) và MF-14 (an toàn cá nhân); `primaryContact` đánh dấu đây là điểm liên hệ chính.
- UC-117 — `sensitivityLevel` quyết định ngưỡng gia tốc thực tế (`getThreshold()`: LOW=15.0, MEDIUM=12.0, HIGH=9.0) dùng ở spec 02.
- UC-119 — tắt giám sát phải chặn ngay việc tạo candidate safety event mới, không chỉ ẩn UI.
- Excluded (SRS MF-14 description) — đây không phải thiết bị phát hiện ngã đã được chứng nhận y tế hay hệ thống điều phối cấp cứu.
