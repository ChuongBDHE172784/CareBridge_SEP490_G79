# MF-10 / Spec 01 — Smart Activity Monitoring Configuration & Session Control

| Field | Value |
| --- | --- |
| Feature | MF-10 — Smart Activity Monitoring & Safety Support |
| Use Cases Covered | Configure monitoring; record sensor permission; enable/disable phone IMU monitoring |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App, CareBridge API |
| Main Flow Summary | Mother configures sensitivity, countdown and auto-alert consent, grants sensor permission, then starts one active phone-IMU session. Disable stops the active session. |
| Grounding (source code) | `SafetyConfigController`, `SafetyConfigService`, `FallDetectionController`, `FallDetectionService`, `SafetyMonitoringConfig`, `ImuMonitoringSession`, Mobile `features/safety` |

## 1. Tổng quan luồng chính (Main Flow Overview)

Cấu hình an toàn là bản ghi 1-1 theo Mother, gồm bật phát hiện ngã, sensitivity, auto alert, countdown 15/30/60 giây và trạng thái quyền cảm biến. Bật monitoring chỉ thành công khi policy consent còn hiệu lực, cấu hình active và sensor permission đã được ghi nhận. Service khóa theo user và trả session đang active nếu request enable bị lặp; không tạo session song song. Tắt monitoring chuyển session sang `STOPPED`. Phạm vi này chỉ dùng cảm biến IMU của điện thoại; không bao gồm wearable/connected device hoặc paid real-time monitoring.

## 2. Class Diagram

```plantuml
@startuml MF10_01_MonitoringConfig_ClassDiagram
skinparam classAttributeIconSize 0
class SafetyMonitoringConfig { +id: UUID; +userId: UUID; +fallDetectionEnabled: boolean; +sensitivityLevel: SensitivityLevel; +emergencyAutoAlert: boolean; +countdownSeconds: int; +sensorPermissionGranted: boolean; +sensorPermissionRecordedAt: Instant }
class ImuMonitoringSession { +id: UUID; +userId: UUID; +status: ImuSessionStatus; +sensitivityLevel: String; +startedAt: Instant; +endedAt: Instant }
enum SensitivityLevel { LOW; MEDIUM; HIGH; +getThreshold(): double }
enum ImuSessionStatus { ACTIVE; STOPPED }
class SafetyConfigController
class SafetyConfigService
class FallDetectionController
class FallDetectionService
interface ISafetyConfigRepository
interface IImuMonitoringSessionRepository
SafetyMonitoringConfig --> SensitivityLevel
ImuMonitoringSession --> ImuSessionStatus
SafetyConfigController --> SafetyConfigService
SafetyConfigService --> ISafetyConfigRepository
FallDetectionController --> FallDetectionService
FallDetectionService --> IImuMonitoringSessionRepository
FallDetectionService --> ISafetyConfigRepository
@enduml
```

**Hình 1 — Class Diagram: Safety configuration và IMU monitoring session**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF10_01_MonitoringConfig_SequenceDiagram
actor "Mother" as M
participant "Safety Mobile UI" as UI
participant "SafetyConfigController" as ConfigController
participant "FallDetectionController" as FallController
participant "SafetyConfigService" as ConfigService
participant "FallDetectionService" as FallService
participant "ISafetyConfigRepository" as ConfigRepo
participant "IImuMonitoringSessionRepository" as SessionRepo
database "PostgreSQL" as DB

M -> UI : 1. Chọn sensitivity, countdown, auto-alert và cấp quyền sensor
activate UI
UI -> ConfigController : 2. PUT /api/v1/safety/config
activate ConfigController
ConfigController -> ConfigService : 3. configure(request, userId)
activate ConfigService
ConfigService -> ConfigRepo : 4. findByUserId(userId)
activate ConfigRepo
ConfigRepo -> DB : 5. SELECT safety_configs
activate DB
DB --> ConfigRepo : 6. config / empty
deactivate DB
ConfigRepo --> ConfigService : 7. Optional<SafetyMonitoringConfig>
deactivate ConfigRepo
ConfigService -> ConfigService : 3a. validate countdown and normalize config
activate ConfigService
ConfigService --> ConfigService : 3a-1. valid config
deactivate ConfigService
ConfigService -> ConfigRepo : 8. save(config)
activate ConfigRepo
ConfigRepo -> DB : 9. INSERT/UPDATE safety_configs
activate DB
DB --> ConfigRepo : 10. saved config
deactivate DB
ConfigRepo --> ConfigService : 11. SafetyMonitoringConfig
deactivate ConfigRepo
ConfigService --> ConfigController : 12. SafetyConfigResponse
deactivate ConfigService
ConfigController --> UI : 13. 200 OK
deactivate ConfigController
UI --> M : 14. Hiển thị cấu hình
deactivate UI

M -> UI : 15. Bật monitoring
activate UI
UI -> FallController : 16. POST /api/v1/safety/fall-detection/enable
activate FallController
FallController -> ConfigService : 17. getConfig(userId)
activate ConfigService
ConfigService -> ConfigRepo : 18. findByUserId(userId)
activate ConfigRepo
ConfigRepo -> DB : 19. SELECT safety_configs
activate DB
DB --> ConfigRepo : 20. config / empty
deactivate DB
ConfigRepo --> ConfigService : 21. Optional<SafetyMonitoringConfig>
deactivate ConfigRepo
ConfigService --> FallController : 22. SafetyConfigResponse
deactivate ConfigService
FallController -> FallService : 23. enable(userId, sensitivityLevel)
activate FallService
FallService -> FallService : 23a. require active config, consent and sensor permission
activate FallService
FallService --> FallService : 23a-1. authorization decision
deactivate FallService
alt [đủ điều kiện]
  FallService -> SessionRepo : 24a. lock user and find active session
  activate SessionRepo
  SessionRepo -> DB : 24a-1. lock/select active session
  activate DB
  DB --> SessionRepo : 24a-2. active session / empty
  deactivate DB
  SessionRepo --> FallService : 24a-3. Optional<ImuMonitoringSession>
  deactivate SessionRepo
  alt [đã có active session]
    FallService --> FallController : 24a-4a. ExistingImuMonitoringSessionResponse
    deactivate FallService
    FallController --> UI : 24a-4a-1. 200 OK
    deactivate FallController
  else [chưa có active session]
    FallService -> SessionRepo : 24a-4b. save(ImuMonitoringSession{ACTIVE})
    activate SessionRepo
    SessionRepo -> DB : 24a-4b-1. INSERT imu_monitoring_sessions
    activate DB
    DB --> SessionRepo : 24a-4b-2. created session
    deactivate DB
    SessionRepo --> FallService : 24a-4b-3. ImuMonitoringSession
    deactivate SessionRepo
    FallService --> FallController : 24a-4b-4. NewImuMonitoringSessionResponse
    deactivate FallService
    FallController --> UI : 24a-4b-5. 201 Created
    deactivate FallController
  end
else [thiếu consent/permission hoặc config tắt]
  FallService --> FallController : 24b. SafetyException
  deactivate FallService
  FallController --> UI : 24b-1. 403 Forbidden hoặc 409 Conflict
  deactivate FallController
end
UI --> M : 25. Hiển thị trạng thái monitoring
deactivate UI
@enduml
```

**Hình 2 — Sequence Diagram: Lưu cấu hình và bật phone-IMU monitoring**

## 4. Business Rules Applied

- Chỉ Mother được cấu hình và điều khiển monitoring.
- `countdownSeconds` chỉ nhận 15, 30 hoặc 60; mặc định 30.
- Enable yêu cầu consent sensor, `sensorPermissionGranted=true` và cấu hình fall detection đang active.
- Mỗi user tối đa một session `ACTIVE`; enable lặp là idempotent theo active session.
- Disable chuyển active session sang `STOPPED`; IMU payload sau đó bị từ chối vì không có active session.
- Không thu thập wearable/connected-device stream và không có phạm vi paid 24/7 monitoring trong release này.
