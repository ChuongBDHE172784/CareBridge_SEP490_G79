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
participant "SafetyConfigController" as ConfigController
participant "FallDetectionController" as FallController
participant "FallDetectionServiceImpl" as Service
database "PostgreSQL" as DB

== UC-116 Manage Emergency Contacts ==
M -> ContactController : PUT /api/v1/emergency/contact\n{contacts: [{name, phone, primaryContact}]}
ContactController -> DB : UPSERT INTO emergency_contacts ...
ContactController --> M : HTTP 200 OK {contacts[]}

== UC-117 Configure Smart Activity Monitoring ==
M -> ConfigController : PUT /api/v1/safety/config\n{fallDetectionEnabled=true, sensitivityLevel=MEDIUM, emergencyAutoAlert=false}
ConfigController -> DB : UPSERT INTO safety_monitoring_configs ...
ConfigController --> M : HTTP 200 OK {config}

== UC-118 Enable Smart Activity Monitoring ==
M -> FallController : POST /api/v1/safety/fall-detection/enable
FallController -> FallController : lấy sensitivityLevel từ SafetyMonitoringConfig hiện tại
FallController -> Service : enable(userId, sensitivityLevel)
Service -> DB : INSERT INTO imu_monitoring_sessions\n(status=ACTIVE, sensitivityLevel)
Service --> FallController : ImuMonitoringSessionResponse{status=ACTIVE}
FallController --> M : HTTP 201 Created

== UC-119 Disable Smart Activity Monitoring ==
M -> FallController : POST /api/v1/safety/fall-detection/disable
FallController -> Service : disable(userId)
Service -> DB : UPDATE imu_monitoring_sessions\nSET status='STOPPED', ended_at=now()\nWHERE user_id=? AND status='ACTIVE'
Service --> FallController : void
FallController --> M : HTTP 200 OK
note right of Service
  Sau khi STOPPED, endpoint /imu-data (spec 02) không còn
  tạo SafetyEvent mới cho user này (UC-119 postcondition).
end note

@enduml
```

**Hình 2 — Sequence Diagram: Configure Contacts/Sensitivity → Enable → Disable Monitoring (Main Flow)**

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
- UC-116 — danh sách liên hệ khẩn cấp dùng chung cho cả MF-07 (gia đình) và MF-14 (an toàn cá nhân); phải xác thực trước khi dùng làm điểm liên hệ chính (`primaryContact`).
- UC-117 — `sensitivityLevel` quyết định ngưỡng gia tốc thực tế (`getThreshold()`: LOW=15.0, MEDIUM=12.0, HIGH=9.0) dùng ở spec 02.
- UC-119 — tắt giám sát phải chặn ngay việc tạo candidate safety event mới, không chỉ ẩn UI.
- Excluded (SRS MF-14 description) — đây không phải thiết bị phát hiện ngã đã được chứng nhận y tế hay hệ thống điều phối cấp cứu.
