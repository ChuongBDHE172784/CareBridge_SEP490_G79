# MF-10 / Spec 02 — Suspected Fall Detection, Safety Check & Escalation

| Field | Value |
| --- | --- |
| Feature | MF-10 — Smart Activity Monitoring & Safety Support |
| Use Cases Covered | Process phone IMU signal; safety countdown; confirm safe; report false positive; request/auto-trigger emergency; view history |
| Primary Actor(s) | Mother, Phone IMU Sensor, Scheduled Countdown Job |
| Platform | Mother Mobile App, CareBridge API |
| Main Flow Summary | An active session accepts deduplicated IMU samples. A suspected fall creates an OPEN event and countdown. Mother responds safe/false-positive/help, or timeout applies configured auto-escalation into MF-07 emergency flow. |
| Grounding (source code) | `FallDetectionController`, `FallDetectionService`, `FallDetectionAlgorithmService`, `SafetyCountdownJob`, `SafetyEvent`, `SafetyEventResponseRecord`, `IEmergencyService` |

## 1. Tổng quan luồng chính (Main Flow Overview)

App gửi IMU sample kèm timestamp, signalId và location tùy consent. Service yêu cầu session `ACTIVE`, kiểm tra clock skew, khóa `sessionId:signalKey` để chống duplicate rồi chạy thuật toán theo sensitivity snapshot của session. Sample không nghi ngờ trả response rỗng; sample nghi ngờ tạo `SafetyEvent OPEN` với deadline. Mother có đúng một response đầu tiên: `I_AM_OK`, `FALSE_POSITIVE` hoặc `NEED_HELP`. Khi hết countdown, job chọn `TIMED_OUT` hoặc `ESCALATION_REQUESTED` theo `emergencyAutoAlert`. Escalation gọi `IEmergencyService.openFlow`; trạng thái chỉ thành `EMERGENCY_ALERT_SENT` sau khi emergency/family alert được liên kết thành công.

## 2. Class Diagram

```plantuml
@startuml MF10_02_FallDetection_ClassDiagram
skinparam classAttributeIconSize 0
class ImuMonitoringSession { +id: UUID; +userId: UUID; +status: ImuSessionStatus; +sensitivityLevel: String }
class SafetyEvent { +id: UUID; +userId: UUID; +imuSessionId: UUID; +signalKey: String; +eventType: SafetyEventType; +magnitude: BigDecimal; +detectedAt: Instant; +countdownDeadlineAt: Instant; +status: SafetyEventStatus; +responseType: String; +emergencySessionId: UUID }
class SafetyEventResponseRecord { +safetyEventId: UUID; +responseType: String; +reason: String; +respondedAt: Instant }
enum SafetyEventStatus { OPEN; TEST_OPEN; CONFIRMED_SAFE; FALSE_POSITIVE; TIMED_OUT; ESCALATION_REQUESTED; EMERGENCY_ALERT_SENT }
class FallDetectionController
class FallDetectionService
interface IFallDetectionAlgorithmService
interface ISafetyEventRepository
interface IEmergencyService
ImuMonitoringSession "1" --> "0..*" SafetyEvent
SafetyEvent "1" --> "0..1" SafetyEventResponseRecord
SafetyEvent --> SafetyEventStatus
FallDetectionController --> FallDetectionService
FallDetectionService --> IFallDetectionAlgorithmService
FallDetectionService --> ISafetyEventRepository
FallDetectionService --> IEmergencyService
@enduml
```

**Hình 1 — Class Diagram: Safety event, response và emergency linkage**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF10_02_FallDetection_SequenceDiagram
actor "Mother" as M
participant "Phone IMU / Safety UI" as UI
participant "FallDetectionController" as Controller
participant "FallDetectionService" as Service
participant "FallDetectionAlgorithmService" as Algorithm
participant "IEmergencyService" as EmergencyService
participant "ISafetyEventRepository" as EventRepo
database "PostgreSQL" as DB

activate UI
UI -> Controller : 1. POST /api/v1/safety/imu-data
activate Controller
Controller -> Service : 2. processImuData(userId, payload)
activate Service
Service -> Service : 2a. verify consent, config, active session, timestamp and signal key
activate Service
Service --> Service : 2a-1. validation result
deactivate Service
Service -> EventRepo : 3. lock signal and find duplicate
activate EventRepo
EventRepo -> DB : 4. lock/select by sessionId + signalKey
activate DB
DB --> EventRepo : 5. existing event / empty
deactivate DB
EventRepo --> Service : 6. Optional<SafetyEvent>
deactivate EventRepo
alt [duplicate signal]
  Service --> Controller : 7a. existing SafetyEventResponse
  deactivate Service
  Controller --> UI : 7a-1. 200 OK
  deactivate Controller
  deactivate UI
else [new signal]
  Service -> Algorithm : 7b. analyze(payload, session.sensitivityLevel)
  activate Algorithm
  Algorithm --> Service : 7b-1. FallAnalysisResult
  deactivate Algorithm
  alt [not suspected]
    Service --> Controller : 7b-2a. null event
    deactivate Service
    Controller --> UI : 7b-2a-1. 200 OK
    deactivate Controller
    deactivate UI
  else [suspected fall/impact]
    Service -> EventRepo : 7b-2b. save(SafetyEvent{OPEN, deadline})
    activate EventRepo
    EventRepo -> DB : 7b-2b-1. INSERT safety_events
    activate DB
    DB --> EventRepo : 7b-2b-2. persisted event
    deactivate DB
    EventRepo --> Service : 7b-2b-3. SafetyEvent
    deactivate EventRepo
    Service --> Controller : 7b-2b-4. SafetyEventResponse
    deactivate Service
    Controller --> UI : 7b-2b-5. 200 OK
    deactivate Controller
    UI --> M : 7b-2b-6. Hiển thị countdown safety check
    deactivate UI

    M -> UI : 7b-2b-7. Chọn I am OK / false positive / need help
    activate UI
    alt [I am OK]
      UI -> Controller : 7b-2b-8a. POST /api/v1/safety/events/{id}/confirm
      activate Controller
    else [false positive]
      UI -> Controller : 7b-2b-8b. POST /api/v1/safety/events/{id}/false-positive
      activate Controller
    else [need help]
      UI -> Controller : 7b-2b-8c. POST /api/v1/safety/events/{id}/emergency-alert
      activate Controller
    end
    Controller -> Service : 7b-2b-9. record first response(userId, eventId)
    activate Service
    Service -> EventRepo : 7b-2b-10. find owned event for update
    activate EventRepo
    EventRepo -> DB : 7b-2b-11. SELECT safety_event FOR UPDATE
    activate DB
    DB --> EventRepo : 7b-2b-12. event / empty
    deactivate DB
    EventRepo --> Service : 7b-2b-13. SafetyEvent
    deactivate EventRepo
    alt [chưa có response và chọn safe/false-positive]
      Service -> EventRepo : 7b-2b-14a. save terminal response/status
      activate EventRepo
      EventRepo -> DB : 7b-2b-14a-1. INSERT response and UPDATE event
      activate DB
      DB --> EventRepo : 7b-2b-14a-2. updated event
      deactivate DB
      EventRepo --> Service : 7b-2b-14a-3. SafetyEvent
      deactivate EventRepo
      Service --> Controller : 7b-2b-14a-4. SafetyEventResponse
      deactivate Service
      Controller --> UI : 7b-2b-14a-5. 200 OK
      deactivate Controller
    else [chọn need help]
      Service -> EmergencyService : 7b-2b-14b. openFlow(FALL_DETECTION, optional location)
      activate EmergencyService
      EmergencyService --> Service : 7b-2b-14b-1. EmergencySessionResponse
      deactivate EmergencyService
      Service -> EventRepo : 7b-2b-14b-2. link emergencySessionId and escalation status
      activate EventRepo
      EventRepo -> DB : 7b-2b-14b-3. UPDATE safety_events
      activate DB
      DB --> EventRepo : 7b-2b-14b-4. updated event
      deactivate DB
      EventRepo --> Service : 7b-2b-14b-5. SafetyEvent
      deactivate EventRepo
      Service --> Controller : 7b-2b-14b-6. accepted
      deactivate Service
      Controller --> UI : 7b-2b-14b-7. 202 Accepted
      deactivate Controller
    else [event đã có response khác]
      Service --> Controller : 7b-2b-14c. SafetyException(SAFETY-010)
      deactivate Service
      Controller --> UI : 7b-2b-14c-1. 409 Conflict
      deactivate Controller
    end
    UI --> M : 7b-2b-15. Hiển thị kết quả safety check
    deactivate UI
  end
end
@enduml
```

**Hình 2 — Sequence Diagram: IMU detection, phản hồi và emergency escalation**

## 4. Business Rules Applied

- Chỉ nhận signal khi consent còn hiệu lực, sensor permission được cấp và có session `ACTIVE`.
- Timestamp vượt quá độ lệch cho phép bị từ chối; `(sessionId, signalKey)` phải idempotent.
- Location chỉ được persist khi payload có tọa độ và policy cho phép.
- Một safety event chỉ nhận response đầu tiên; replay cùng response là idempotent, response khác trả `409 Conflict`.
- Sensor self-test không được kích hoạt emergency alert.
- Timeout tự động chỉ escalation khi `emergencyAutoAlert=true`; nếu không, event thành `TIMED_OUT`.
- `EMERGENCY_ALERT_SENT` chỉ phản ánh emergency session đã gửi alert; phát hiện nghi ngờ không tự đồng nghĩa đã gọi người thân.
- Không bao gồm wearable/connected-device ingestion hoặc dịch vụ giám sát trả phí thời gian thực.
