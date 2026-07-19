# MF-14 / Spec 02 — Suspected Fall/Impact Detection, Safety Check & False-Positive Feedback

| Field | Value |
| --- | --- |
| Feature | MF-14 — Smart Activity Monitoring & Safety Support |
| Use Cases Covered | UC-120 Handle Suspected Fall or Impact and Safety Check, UC-121 Review Safety Event History and Report False Positive Detection |
| Primary Actor(s) | Mother, Phone IMU Sensor (system) |
| Platform | Mother Mobile App |
| Main Flow Summary | The phone's IMU stream is analyzed against the active monitoring session's sensitivity threshold; a suspected fall/impact creates a candidate `SafetyEvent` and opens a safety confirmation on-device, which the Mother resolves as "I am OK" (confirmed safe), escalates to an emergency alert, or — afterwards, from history — marks as a false detection with a reason. |
| Grounding (source code) | `safety/entity/SafetyEvent.java`, `SafetyEventStatus.java`, `SafetyEventType.java`, `safety/service/impl/FallDetectionService.java` (`processImuData`, `confirmSafetyCheck`, `reportFalsePositive`, `sendEmergencyAlert`), `safety/controller/FallDetectionController.java` (`/api/v1/safety/imu-data`, `/events`, `/events/{id}/confirm`, `/events/{id}/false-positive`, `/events/{id}/emergency-alert`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`processImuData()` chỉ phân tích và tạo `SafetyEvent` khi có **session `ACTIVE`** (spec
01) — nếu không có, trả lỗi `SAFETY-006` thay vì âm thầm bỏ qua. Khi thuật toán
(`IFallDetectionAlgorithmService.analyze()`) nghi ngờ có ngã/va chạm dựa trên ngưỡng
`sensitivityLevel`, một `SafetyEvent` mới được tạo ở `status=OPEN` (mặc định) và phát sự
kiện `SuspectedFallDetected` — đây chính là lúc app mở màn hình đếm ngược xác nhận an toàn
(UC-120). Mother phản hồi một trong hai hướng: **xác nhận an toàn**
(`CONFIRMED_SAFE`) hoặc **gửi cảnh báo khẩn cấp** (`EMERGENCY_ALERT_SENT` — tái phát sự
kiện `SuspectedFallDetected` để các consumer khác, ví dụ MF-07 family alert, xử lý tiếp).
Sau đó, từ lịch sử sự kiện, Mother có thể đánh dấu một sự kiện là phát hiện nhầm
(`FALSE_POSITIVE`, kèm lý do — UC-121) để cải thiện đánh giá ngưỡng trong tương lai.

## 2. Class Diagram

```plantuml
@startuml MF14_02_FallDetection_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class ImuMonitoringSession {
  + id: UUID
  + userId: UUID
  + status: ImuSessionStatus
  + sensitivityLevel: String
}

class SafetyEvent {
  + id: UUID
  + userId: UUID
  + imuSessionId: UUID
  + eventType: SafetyEventType
  + magnitude: BigDecimal
  + userLatitude: BigDecimal
  + userLongitude: BigDecimal
  + detectedAt: Instant
  + status: SafetyEventStatus
  + resolvedAt: Instant
  + notes: String
  + createdBy: String
}

enum SafetyEventType {
  SUSPECTED_FALL
  SUSPECTED_IMPACT
  FALSE_ALARM
}

enum SafetyEventStatus {
  OPEN
  CONFIRMED_SAFE
  FALSE_POSITIVE
  EMERGENCY_ALERT_SENT
}

class ImuDataPayload {
  + accelerometerX/Y/Z: double
  + gyroscopeX/Y/Z: double
  + timestamp: Instant
}

class FallAnalysisResult {
  + suspected: boolean
  + eventType: SafetyEventType
  + magnitude: double
}

class FallDetectionController {
  - fallDetectionService: IFallDetectionService
  + processImuData(ImuDataRequest): ResponseEntity
  + listEvents(page, size): ResponseEntity
  + confirmSafetyCheck(eventId, SafetyEventActionRequest): ResponseEntity
  + reportFalsePositive(eventId, SafetyEventActionRequest): ResponseEntity
  + sendEmergencyAlert(eventId): ResponseEntity
}

interface IFallDetectionAlgorithmService <<interface>> {
  + analyze(payload: ImuDataPayload, sensitivityLevel: String): FallAnalysisResult
}

interface IFallDetectionService <<interface>> {
  + processImuData(userId: UUID, payload: ImuDataPayload): SafetyEventResponse
  + confirmSafetyCheck(userId: UUID, eventId: UUID, note: String): SafetyEventResponse
  + reportFalsePositive(userId: UUID, eventId: UUID, note: String): SafetyEventResponse
  + sendEmergencyAlert(userId: UUID, eventId: UUID): void
}

class FallDetectionService implements IFallDetectionService {
  - imuSessionRepository: IImuMonitoringSessionRepository
  - safetyEventRepository: ISafetyEventRepository
  - algorithmService: IFallDetectionAlgorithmService
  - eventPublisher: ApplicationEventPublisher
}

ImuMonitoringSession "1" *-- "0..*" SafetyEvent : detected during
SafetyEvent --> SafetyEventType
SafetyEvent --> SafetyEventStatus
FallDetectionController --> IFallDetectionService : uses
FallDetectionService --> IFallDetectionAlgorithmService : analyze(payload, sensitivityLevel)
FallDetectionService ..> FallAnalysisResult : consumes
FallDetectionService --> ApplicationEventPublisher : publishes SuspectedFallDetected

@enduml
```

**Hình 1 — Class Diagram: IMU Session, Safety Event & Fall Analysis**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF14_02_FallDetection_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

participant "Phone IMU Sensor" as Sensor
actor "Mother" as M
participant "FallDetectionController" as Controller
participant "FallDetectionService" as Service
participant "IFallDetectionAlgorithmService" as Algo
participant "ApplicationEventPublisher" as Events
database "PostgreSQL" as DB

== UC-120 Handle Suspected Fall or Impact and Safety Check ==
Sensor -> M : Thu thập accelerometer/gyroscope liên tục
M -> Controller : POST /api/v1/safety/imu-data\n{accelerometerX/Y/Z, gyroscopeX/Y/Z, timestamp}
Controller -> Service : processImuData(userId, payload)
Service -> Service : require active ImuMonitoringSession\n[nếu không có → SAFETY-006]
Service -> Algo : analyze(payload, sensitivityLevel)
Algo --> Service : FallAnalysisResult{suspected, eventType, magnitude}

alt suspected == true
  Service -> DB : INSERT INTO safety_events\n(status=OPEN, eventType, magnitude, detectedAt)
  Service -> Events : publish(SuspectedFallDetected)
  Service --> Controller : SafetyEventResponse{status=OPEN}
  Controller --> M : HTTP 200 OK → mở màn hình đếm ngược an toàn
  M -> M : Đếm ngược xác nhận an toàn (theo cấu hình UC-117)

  alt Mother xác nhận an toàn
    M -> Controller : POST /api/v1/safety/events/{eventId}/confirm\n{note}
    Controller -> Service : confirmSafetyCheck(userId, eventId, note)
    Service -> DB : UPDATE safety_events\nSET status='CONFIRMED_SAFE', resolved_at=now()
    Service --> M : HTTP 200 OK
  else Mother không phản hồi kịp / chọn cần trợ giúp
    M -> Controller : POST /api/v1/safety/events/{eventId}/emergency-alert
    Controller -> Service : sendEmergencyAlert(userId, eventId)
    Service -> DB : UPDATE safety_events\nSET status='EMERGENCY_ALERT_SENT', resolved_at=now()
    Service -> Events : publish(SuspectedFallDetected) [để MF-07 gửi family alert]
    Service --> M : HTTP 202 Accepted
  end
else suspected == false
  Service --> Controller : null (không tạo event)
end

== UC-121 Review Safety Event History and Report False Positive ==
M -> Controller : GET /api/v1/safety/events?page=0&size=20
Controller -> Service : listSafetyEvents(userId, pageable)
Service -> DB : SELECT * FROM safety_events\nWHERE user_id=? ORDER BY detected_at DESC
DB --> Service : events[]
Service --> Controller : SafetyEventResponse[]
Controller --> M : HTTP 200 OK {events[]}

M -> Controller : POST /api/v1/safety/events/{eventId}/false-positive\n{note="Đặt điện thoại xuống bàn mạnh"}
Controller -> Service : reportFalsePositive(userId, eventId, note)
Service -> DB : UPDATE safety_events\nSET status='FALSE_POSITIVE', resolved_at=now(), notes=?
Service --> Controller : SafetyEventResponse{status=FALSE_POSITIVE}
Controller --> M : HTTP 200 OK

@enduml
```

**Hình 2 — Sequence Diagram: IMU Data → Detect → Safety Check (Confirm/Alert) → History → False-Positive Feedback (Main Flow)**

## 4. State Machine — `SafetyEvent.status`

```plantuml
@startuml MF14_02_SafetyEventStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> OPEN : algorithmService.analyze() nghi ngờ ngã/va chạm\n[chỉ khi ImuMonitoringSession đang ACTIVE] (UC-120)

OPEN --> CONFIRMED_SAFE : Mother xác nhận "Tôi ổn" (UC-120)
OPEN --> EMERGENCY_ALERT_SENT : Mother chọn cần trợ giúp /\nkhông phản hồi trong thời gian đếm ngược (UC-120)
OPEN --> FALSE_POSITIVE : Mother đánh dấu phát hiện nhầm\ntừ lịch sử, kèm lý do (UC-121)

CONFIRMED_SAFE --> [*]
EMERGENCY_ALERT_SENT --> [*]
FALSE_POSITIVE --> [*]

note right of OPEN
  Cả 3 trạng thái đích đều là terminal (resolvedAt được set) —
  đúng theo 4 giá trị thật của SafetyEventStatus, không có
  trạng thái "IN_REVIEW" hay tương tự không có trong code.
end note

@enduml
```

**Hình 3 — State Machine: `SafetyEvent.status` Lifecycle**

## 5. Business Rules Applied

- BR-SAFETY — chỉ session `ACTIVE` (spec 01) mới xử lý IMU data; tắt giám sát ngăn tạo `SafetyEvent` mới ngay lập tức (UC-119).
- UC-120 — đây không phải thiết bị y tế được chứng nhận; kết quả là "nghi ngờ" (`suspected`), không phải chẩn đoán chấn thương.
- UC-120 — `sendEmergencyAlert` phát lại sự kiện `SuspectedFallDetected` để các luồng khác (ví dụ family alert ở MF-07) có thể phản ứng — không tự dispatch cấp cứu.
- UC-121 — phản hồi phát hiện nhầm luôn yêu cầu lý do (`notes`), dùng để cải thiện đánh giá ngưỡng trong tương lai, không xoá lịch sử sự kiện gốc.
