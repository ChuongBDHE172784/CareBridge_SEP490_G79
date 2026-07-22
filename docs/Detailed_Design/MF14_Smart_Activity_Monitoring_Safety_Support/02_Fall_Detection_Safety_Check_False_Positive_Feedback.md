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
participant "IImuMonitoringSessionRepository" as SessionRepo
participant "IFallDetectionAlgorithmService" as Algo
participant "ISafetyEventRepository" as EventRepo
database "PostgreSQL" as DB

== UC-120 Handle Suspected Fall or Impact and Safety Check ==
Sensor -> M : 1. Collect accelerometer/gyroscope continuously (phone sensors)
M -> Controller : 2. POST /api/v1/safety/imu-data\n{accelerometerX/Y/Z, gyroscopeX/Y/Z, timestamp}
activate Controller
Controller -> Service : 3. processImuData(userId, payload)
activate Service
Service -> SessionRepo : 4. findActiveByUserId(userId)
activate SessionRepo
SessionRepo -> DB : 5. SELECT * FROM imu_monitoring_sessions\nWHERE user_id=? AND status='ACTIVE'
activate DB
DB --> SessionRepo : 6. session row (409 SAFETY-006 if not found)
deactivate DB
SessionRepo --> Service : 7. ImuMonitoringSession{sensitivityLevel}
deactivate SessionRepo
Service -> Algo : 8. analyze(payload, sensitivityLevel)
activate Algo
Algo --> Service : 9. FallAnalysisResult{suspected, eventType, magnitude}
deactivate Algo
alt 10. suspected == true (suspected fall/collision)
  Service -> EventRepo : 10. save(SafetyEvent{status=OPEN by default, eventType,\nmagnitude, detectedAt=now(), createdBy="SYSTEM"})
  activate EventRepo
  EventRepo -> DB : 11. INSERT INTO safety_events ...
  activate DB
  DB --> EventRepo : 12. saved
  deactivate DB
  EventRepo --> Service : 13. SafetyEvent
  deactivate EventRepo
  Service -> Service : 14. publishEvent(SuspectedFallDetected)
  Service --> Controller : 15. SafetyEventResponse{status=OPEN}
  deactivate Service
  Controller --> M : 16. HTTP 200 OK → open safety confirmation countdown screen
  deactivate Controller
  M -> M : 17. safety confirmation countdown (according to UC-117 config)

  alt 18. Mother confirms safety within countdown time
    M -> Controller : 18. POST /api/v1/safety/events/{eventId}/confirm {note}
    activate Controller
    Controller -> Service : 19. confirmSafetyCheck(userId, eventId, note)
    activate Service
    Service -> EventRepo : 20. findByIdAndUserId(eventId, userId)
    activate EventRepo
    EventRepo -> DB : 21. SELECT * FROM safety_events\nWHERE id=? AND user_id=?
    activate DB
    DB --> EventRepo : 22. event row (404 SAFETY-007 if not found/not owned by caller)
    deactivate DB
    EventRepo --> Service : 23. SafetyEvent
    deactivate EventRepo
    Service -> Service : 24. set status=CONFIRMED_SAFE, resolvedAt=now(), notes\n[DO NOT check if current status is OPEN]
    Service -> EventRepo : 25. save(event{...})
    activate EventRepo
    EventRepo -> DB : 26. UPDATE safety_events\nSET status='CONFIRMED_SAFE', resolved_at=now(), notes=?
    activate DB
    DB --> EventRepo : 27. updated
    deactivate DB
    EventRepo --> Service : 28. SafetyEvent
    deactivate EventRepo
    Service --> Controller : 29. SafetyEventResponse{status=CONFIRMED_SAFE}
    deactivate Service
    Controller --> M : 30. HTTP 200 OK
    deactivate Controller
  else 18. Mother does not respond in time / selects need help
    M -> Controller : 18a. POST /api/v1/safety/events/{eventId}/emergency-alert
    activate Controller
    Controller -> Service : 18b. sendEmergencyAlert(userId, eventId)
    activate Service
    Service -> EventRepo : 18c. findByIdAndUserId(eventId, userId)
    activate EventRepo
    EventRepo -> DB : 18d. SELECT * FROM safety_events\nWHERE id=? AND user_id=?
    activate DB
    DB --> EventRepo : 18e. event row (404 SAFETY-007 if not found)
    deactivate DB
    EventRepo --> Service : 18f. SafetyEvent
    deactivate EventRepo
    Service -> Service : 18g. set status=EMERGENCY_ALERT_SENT, resolvedAt=now()
    Service -> EventRepo : 18h. save(event{...})
    activate EventRepo
    EventRepo -> DB : 18i. UPDATE safety_events\nSET status='EMERGENCY_ALERT_SENT', resolved_at=now()
    activate DB
    DB --> EventRepo : 18j. updated
    deactivate DB
    EventRepo --> Service : 18k. SafetyEvent
    deactivate EventRepo
    Service -> Service : 18l. publishEvent(SuspectedFallDetected)\n[republish — for MF-07 family alert/other consumers to handle]
    Service --> Controller : 18m. void
    deactivate Service
    Controller --> M : 18n. HTTP 202 Accepted
    deactivate Controller
  end
else 10. suspected == false (not suspected, no event created)
  Service --> Controller : 10a. null
  deactivate Service
  Controller --> M : 10b. HTTP 200 OK (no safety event)
  deactivate Controller
end

== UC-121 Review Safety Event History and Report False Positive ==
M -> Controller : 31. GET /api/v1/safety/events?page=0&size=20
activate Controller
Controller -> Service : 32. listSafetyEvents(userId, pageable)
activate Service
Service -> EventRepo : 33. findByUserIdOrderByDetectedAtDesc(userId, pageable)
activate EventRepo
EventRepo -> DB : 34. SELECT * FROM safety_events\nWHERE user_id=? ORDER BY detected_at DESC
activate DB
DB --> EventRepo : 35. events[]
deactivate DB
EventRepo --> Service : 36. events[]
deactivate EventRepo
Service --> Controller : 37. SafetyEventResponse[]
deactivate Service
Controller --> M : 38. HTTP 200 OK {events[]}
deactivate Controller

M -> Controller : 39. POST /api/v1/safety/events/{eventId}/false-positive\n{note="Placing the phone on the table heavily"}
activate Controller
Controller -> Service : 40. reportFalsePositive(userId, eventId, note)
activate Service
Service -> EventRepo : 41. findByIdAndUserId(eventId, userId)
activate EventRepo
EventRepo -> DB : 42. SELECT * FROM safety_events\nWHERE id=? AND user_id=?
activate DB
DB --> EventRepo : 43. event row (404 SAFETY-007 if not found)
deactivate DB
EventRepo --> Service : 44. SafetyEvent
deactivate EventRepo
Service -> Service : 45. set status=FALSE_POSITIVE, resolvedAt=now(), notes
Service -> EventRepo : 46. save(event{...})
activate EventRepo
EventRepo -> DB : 47. UPDATE safety_events\nSET status='FALSE_POSITIVE', resolved_at=now(), notes=?
activate DB
DB --> EventRepo : 48. updated
deactivate DB
EventRepo --> Service : 49. SafetyEvent
deactivate EventRepo
Service --> Controller : 50. SafetyEventResponse{status=FALSE_POSITIVE}
deactivate Service
Controller --> M : 51. HTTP 200 OK
deactivate Controller

@enduml
```

**Hình 2 — Sequence Diagram: IMU Data → Detect → Safety Check (Confirm/Alert) → History → False-Positive Feedback (Main Flow)**

> **Ghi chú grounding:** `confirmSafetyCheck`, `reportFalsePositive` và `sendEmergencyAlert`
> đều dùng chung `findOwnedEvent()` (`findByIdAndUserId`, 404 `SAFETY-007` nếu không thuộc
> caller) nhưng **không kiểm tra `status` hiện tại của event trước khi chuyển trạng thái**
> — về mặt code, có thể gọi `/confirm` trên một event đã `FALSE_POSITIVE`/
> `EMERGENCY_ALERT_SENT` và nó vẫn ghi đè `status`/`resolvedAt`/`notes` bình thường, không có
> guard "chỉ áp dụng khi đang OPEN" như nhiều luồng khác trong hệ thống. Bản vẽ trước ngụ ý
> các nhánh này chỉ áp dụng khi event đang `OPEN`, nhưng ràng buộc đó chỉ đúng về mặt UX
> (app chỉ hiện màn hình xác nhận ngay sau khi tạo event), không phải ràng buộc được server
> thực thi.

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
