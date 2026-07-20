# MF-13 / Spec 01 — Connected Device: Connection, Sync, Trend & Disconnect Lifecycle

| Field | Value |
| --- | --- |
| Feature | MF-13 — Connected Device & Health Data Integration |
| Use Cases Covered | UC-112 Connect Health Device or Platform, UC-113 Import or Synchronize Device Observations, UC-114 View Device Data Trend and Quality, UC-115 Disconnect Device and Delete Imported Data |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother connects a supported device/platform after reviewing permission scope, the app imports or synchronizes selected observations (heart rate, sleep, steps, SpO2, temperature, blood pressure), she reviews trend and data-quality labels, and can disconnect to stop future sync. This is presented as one continuous end-to-end pipeline rather than four separate specs because each step only makes sense in sequence. |
| Grounding (source code) | `health/device/entity/HealthDeviceConnection.java`, `DeviceConnectionStatus.java`, `health/device/entity/DeviceMeasurement.java`, `health/device/controller/DeviceConnectionController.java` (`/api/v1/health/devices/connections`), `DeviceSyncController.java` (`/{id}/sync`), `DeviceMetricController.java` (`/api/v1/health/metrics/device-import`, `/trend`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`HealthDeviceConnection` được tạo sau khi Mother xem và chấp nhận phạm vi quyền
(`scopesJson`) cho một `providerName` (UC-112). Đồng bộ dữ liệu (UC-113) tạo các
`DeviceMeasurement` gắn `connectionId`, giữ `qualityLabel` và `rawMetadataJson` để minh
bạch nguồn/chất lượng — các measurement này đồng thời được ánh xạ sang
`MaternalHealthMetric` (MF-02) với `sourceType=DEVICE` để hiển thị chung trong trend của
mẹ. UC-114 dùng lại chính `DeviceMeasurement` để vẽ xu hướng kèm cảnh báo chất lượng
(`accuracyWarning`). Ngắt kết nối (UC-115) chuyển `status=REVOKED`, ngăn đồng bộ mới.
**Ghi chú grounding:** trong code hiện tại, `disconnect()` chỉ đổi `status`, **chưa có**
bước xoá cứng `DeviceMeasurement` đã nhập đi kèm (nửa "delete imported data" của UC-115
chưa có cài đặt tự động) — spec này nêu rõ để không đánh giá sai UC-115 là đã hoàn chỉnh.

## 2. Class Diagram

```plantuml
@startuml MF13_01_DeviceIntegration_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class HealthDeviceConnection {
  + connectionId: UUID
  + userId: UUID
  + providerName: String
  + deviceName: String
  + scopesJson: String
  + tokenReference: String
  + consentGrantedAt: Instant
  + lastSyncedAt: Instant
  + status: DeviceConnectionStatus
}

enum DeviceConnectionStatus {
  ACTIVE
  INACTIVE
  REVOKED
}

class DeviceMeasurement {
  + deviceMeasurementId: UUID
  + connectionId: UUID
  + measurementType: String
  + valueNumeric: BigDecimal
  + valueSecondary: BigDecimal
  + unit: String
  + measuredAt: Instant
  + qualityLabel: String
  + rawMetadataJson: String
}

class DeviceTrendResponse <<read-model>> {
  + journeyId: UUID
  + metricType: MetricType
  + unit: String
  + hasAnyData: boolean
  + points: List<DeviceTrendPointResponse>
}

class DeviceConnectionController {
  - deviceConnectionService: IDeviceConnectionService
  + connect(ConnectDeviceRequest): ResponseEntity
  + disconnect(connectionId): ResponseEntity
  + myConnections(): ResponseEntity
}

class DeviceSyncController {
  - deviceSyncService: IDeviceSyncService
  + sync(connectionId): ResponseEntity
}

class DeviceMetricController {
  - deviceDataImportService: IDeviceDataImportService
  - deviceTrendService: IDeviceTrendService
  + importMetric(ImportDeviceMetricRequest): ResponseEntity
  + trend(DeviceTrendQuery): ResponseEntity
}

interface IDeviceConnectionService <<interface>> {
  + connect(userId: UUID, request): HealthDeviceConnection
  + disconnect(connectionId: UUID, userId: UUID): HealthDeviceConnection
}

class DeviceConnectionService implements IDeviceConnectionService {
  - connectionRepository: IHealthDeviceConnectionRepository
}

interface IDeviceTrendService <<interface>> {
  + trend(userId: UUID, query: DeviceTrendQuery): DeviceTrendResponse
}

class DeviceTrendService implements IDeviceTrendService {
  - measurementRepository: IDeviceMeasurementRepository
}

HealthDeviceConnection --> DeviceConnectionStatus
HealthDeviceConnection "1" *-- "0..*" DeviceMeasurement : produces
DeviceConnectionController --> IDeviceConnectionService : uses
DeviceSyncController --> IDeviceSyncService : uses
DeviceMetricController --> IDeviceDataImportService : uses
DeviceMetricController --> IDeviceTrendService : uses
DeviceTrendService ..> DeviceTrendResponse : builds

@enduml
```

**Hình 1 — Class Diagram: Device Connection, Measurement & Trend Read-Model**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF13_01_DeviceIntegration_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "DeviceConnectionController" as ConnController
participant "DeviceConnectionService" as ConnService
participant "DeviceSyncController" as SyncController
participant "DeviceMetricController" as MetricController
participant "DeviceTrendService" as TrendService
database "PostgreSQL" as DB

== UC-112 Connect Health Device or Platform ==
M -> ConnController : POST /api/v1/health/devices/connections\n{providerName, scopesJson}
ConnController -> ConnService : connect(userId, request)
ConnService -> DB : INSERT INTO health_device_connections (status=ACTIVE)
ConnService --> ConnController : HealthDeviceConnection{status=ACTIVE}
ConnController --> M : HTTP 201 Created

== UC-113 Import or Synchronize Device Observations ==
M -> SyncController : POST /api/v1/health/devices/connections/{id}/sync
SyncController -> DB : gọi provider API, nhận observations mới
SyncController -> DB : INSERT INTO device_measurements (qualityLabel, rawMetadataJson)
SyncController -> DB : UPDATE health_device_connections SET last_synced_at=now()
SyncController --> M : HTTP 200 OK {syncResult}

M -> MetricController : POST /api/v1/health/metrics/device-import\n{measurementType, value, measuredAt}
MetricController -> DB : ánh xạ sang maternal_health_metrics (sourceType=DEVICE)
MetricController --> M : HTTP 201 Created

== UC-114 View Device Data Trend and Quality ==
M -> MetricController : GET /api/v1/health/metrics/trend?metricType=HEART_RATE
MetricController -> TrendService : trend(userId, query)
TrendService -> DB : SELECT * FROM device_measurements\nWHERE connection.user_id=? AND measurement_type=?\nORDER BY measured_at
DB --> TrendService : measurements[]
TrendService -> TrendService : gắn accuracyWarning nếu qualityLabel thấp/có khoảng trống dữ liệu
TrendService --> MetricController : DeviceTrendResponse
MetricController --> M : HTTP 200 OK {trend}

== UC-115 Disconnect Device and Delete Imported Data ==
M -> ConnController : PATCH /api/v1/health/devices/connections/{id}/disconnect
ConnController -> ConnService : disconnect(connectionId, userId)
ConnService -> DB : UPDATE health_device_connections SET status='REVOKED'
ConnService --> ConnController : HealthDeviceConnection{status=REVOKED}
ConnController --> M : HTTP 200 OK
note right of ConnService
  Ngăn đồng bộ mới. Xoá dữ liệu đã nhập ("delete imported
  data") theo yêu cầu retention hiện là quy trình thủ công/
  chưa tự động hoá trong service này — xem ghi chú mục 1.
end note

@enduml
```

**Hình 2 — Sequence Diagram: Connect → Sync → View Trend → Disconnect (Main Flow)**

## 4. State Machine — `HealthDeviceConnection.status`

```plantuml
@startuml MF13_01_DeviceConnectionStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : Mother kết nối thiết bị/nền tảng (UC-112)

ACTIVE --> ACTIVE : Đồng bộ dữ liệu định kỳ hoặc thủ công (UC-113)\n[cập nhật lastSyncedAt]
ACTIVE --> REVOKED : Mother ngắt kết nối (UC-115)\n[disconnect(), chặn đồng bộ mới]

REVOKED --> [*]

note right of ACTIVE
  DeviceConnectionStatus khai báo cả INACTIVE, nhưng rà soát
  service layer hiện tại (DeviceConnectionService) không có
  đường chuyển nào gán INACTIVE — chỉ ACTIVE và REVOKED được
  dùng thật. Vẽ đúng 2 trạng thái có transition thật, không suy
  diễn khi nào INACTIVE được dùng.
end note

@enduml
```

**Hình 3 — State Machine: `HealthDeviceConnection.status` (2/3 giá trị enum có transition thật)**

## 5. Business Rules Applied

- UC-112 — kết nối chỉ sau khi Mother xem và đồng ý phạm vi quyền (`scopesJson`) cho từng chỉ số.
- UC-113 — mỗi observation nhập vào giữ `sourceType=DEVICE` + `qualityLabel`, không trộn lẫn với dữ liệu tự nhập (`MANUAL`).
- UC-114 — trend hiển thị rõ khoảng trống dữ liệu (`hasAnyData`) và cảnh báo độ chính xác (`accuracyWarning`), không trình bày như kết luận y khoa.
- UC-115 — ngắt kết nối chặn đồng bộ tương lai ngay lập tức; xoá dữ liệu lịch sử đã nhập cần xác nhận riêng theo chính sách retention (khoảng cách hiện tại giữa SRS và service layer, xem mục 1).
