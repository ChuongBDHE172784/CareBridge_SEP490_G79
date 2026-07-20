# MF-07 / Spec 02 — Time-Limited Family Alert & Nearby Support Request

| Field | Value |
| --- | --- |
| Feature | MF-07 — Emergency Map & Nearby Care Support |
| Use Cases Covered | UC-80 Share Time-Limited Location and Send Family Emergency Alert, UC-81 Create or Cancel Nearby Support Request, UC-82 Manage Expert Nearby Availability and Respond to Nearby Support Request |
| Primary Actor(s) | Mother, Verified Expert |
| Platform | Mother Mobile App, Expert App / Expert Portal |
| Main Flow Summary | A Mother opens an `EmergencySession` and notifies pre-configured emergency contacts with her minimum-necessary location (time-limited), and/or creates a `NearbySupportRequest` visible only to eligible nearby verified experts, who may accept, decline or stop responding — with no guarantee of expert arrival. |
| Grounding (source code) | `emergency/entity/EmergencySession.java`, `EmergencyStatus.java`, `emergency/entity/EmergencyContact.java`, `emergency/entity/FamilyAlertLog.java`, `emergency/controller/EmergencyController.java` (`/api/v1/emergency/sessions`), `EmergencyContactController.java` (`/api/v1/emergency/contact`), `nearbycare/entity/NearbySupportRequest.java` (`SupportStatus`), `NearbySupportResponse.java` (`ResponseAction`), `nearbycare/controller/NearbySupportController.java` (`/api/v1/nearbycare`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là luồng có tác động cao nhất của MF-07: một `EmergencySession` được mở với vị trí
người dùng, hệ thống gửi cảnh báo tới `EmergencyContact` đã cấu hình trước (ghi vết bằng
`FamilyAlertLog` — số người nhận, có kèm vị trí hay không — UC-80). Song song, Mother có
thể tạo một `NearbySupportRequest` độc lập với toạ độ + loại hỗ trợ cần; request này
**chỉ hiển thị cho verified expert đủ điều kiện** (đã bật nearby availability — MF-05/02)
trong bán kính phù hợp, và Mother có thể huỷ bất kỳ lúc nào (UC-81). Expert xem request
tối thiểu ngữ cảnh cần thiết và phản hồi ACCEPT/DECLINE/STOP (`NearbySupportResponse` —
UC-82). Toàn bộ luồng **không đảm bảo có expert nhận** và **không phải dịch vụ điều
phối xe cấp cứu** (đúng phạm vi loại trừ SRS 4.8).

## 2. Class Diagram

```plantuml
@startuml MF07_02_FamilyAlertNearby_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class EmergencyContact {
  + id: UUID
  + userId: UUID
  + name: String
  + phone: String
  + relationship: String
  + primaryContact: boolean
}

class EmergencySession {
  + id: UUID
  + userId: UUID
  + status: EmergencyStatus
  + triggerSource: String
  + userLatitude: BigDecimal
  + userLongitude: BigDecimal
  + resolvedAt: Instant
}

enum EmergencyStatus {
  ACTIVE
  RESOLVED
  CANCELLED
}

class FamilyAlertLog {
  + id: UUID
  + sessionId: UUID
  + sentAt: Instant
  + recipientCount: int
  + locationIncluded: boolean
}

class NearbySupportRequest {
  + requestId: UUID
  + requesterUserId: UUID
  + supportType: String
  + description: String
  + latitude: BigDecimal
  + longitude: BigDecimal
  + consentStatus: String
  + status: SupportStatus
  + respondedAt: LocalDateTime
  + completedAt: LocalDateTime
}

enum SupportStatus {
  OPEN
  ACCEPTED
  CANCELLED
  COMPLETED
}

class NearbySupportResponse {
  + responseId: UUID
  + requestId: UUID
  + expertProfileId: UUID
  + action: ResponseAction
  + note: String
  + respondedAt: LocalDateTime
}

enum ResponseAction {
  ACCEPT
  DECLINE
  STOP
}

class EmergencyController {
  - emergencyService: EmergencyService
  + open(request): ResponseEntity
  + resolve(id): ResponseEntity
  + alertLog(id): ResponseEntity
}

class EmergencyContactController {
  + myContacts(): ResponseEntity
  + updateContacts(request): ResponseEntity
}

class NearbySupportController {
  - nearbySupportService: NearbySupportService
  + create(request): ResponseEntity
  + cancel(requestId): ResponseEntity
  + openRequests(): ResponseEntity
  + respond(requestId, request): ResponseEntity
}

interface EmergencyService <<interface>> {
  + open(userId: UUID, request): EmergencySession
  + notifyContacts(sessionId: UUID): FamilyAlertLog
}

class EmergencyServiceImpl implements EmergencyService {
  - emergencySessionRepository: EmergencySessionRepository
  - emergencyContactRepository: EmergencyContactRepository
  - notificationService: NotificationService
  - auditService: AuditService
}

interface NearbySupportService <<interface>> {
  + create(requesterId: UUID, request): NearbySupportRequest
  + respond(expertProfileId: UUID, requestId: UUID, action: ResponseAction): NearbySupportResponse
}

class NearbySupportServiceImpl implements NearbySupportService {
  - nearbySupportRequestRepository: NearbySupportRequestRepository
  - expertAvailabilityRepository: ExpertAvailabilityRepository
}

EmergencySession "1" *-- "0..*" FamilyAlertLog : notifies
EmergencySession --> EmergencyStatus
EmergencyContact "0..*" --> "1" EmergencyContact : configured per user
NearbySupportRequest --> SupportStatus
NearbySupportRequest "1" *-- "0..*" NearbySupportResponse : receives
NearbySupportResponse --> ResponseAction
EmergencyController --> EmergencyService : uses
EmergencyContactController ..> EmergencyContact : manages
NearbySupportController --> NearbySupportService : uses
NearbySupportServiceImpl --> ExpertAvailabilityRepository : lọc expert đủ điều kiện (MF-05)

@enduml
```

**Hình 1 — Class Diagram: Emergency Session/Family Alert & Nearby Support Request/Response**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF07_02_FamilyAlertNearby_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "EmergencyController" as EmController
participant "EmergencyService" as EmService
participant "IEmergencySessionRepository" as SessionRepo
participant "EmergencySessionOpenedHandler" as Handler
participant "FamilyAlertService" as FamilyAlertSvc
participant "FamilyMemberPort" as FamMember
participant "LocationConsentPort" as Consent
participant "FcmNotificationPort" as Fcm
participant "SmsFallbackPort" as Sms
participant "IFamilyAlertLogRepository" as AlertLogRepo
participant "NearbySupportController" as NearController
participant "NearbySupportServiceImpl" as NearService
participant "NearbySupportRequestRepository" as ReqRepo
participant "NearbySupportResponseRepository" as RespRepo
participant "ExpertProfileRepository" as ExpertRepo
actor "Verified Expert" as Exp
database "PostgreSQL" as DB

== UC-80 Share Time-Limited Location and Send Family Emergency Alert ==
M -> EmController : 1. POST /api/v1/emergency/sessions\n{userLatitude, userLongitude, triggerSource="MANUAL"}
activate EmController
EmController -> EmService : 2. openFlow(request, userId)
activate EmService
EmService -> SessionRepo : 3. findActiveByUserId(userId)\n[idempotent — return old ACTIVE session if any]
activate SessionRepo
SessionRepo -> DB : 4. SELECT * FROM emergency_sessions\nWHERE user_id=? AND status='ACTIVE'
activate DB
DB --> SessionRepo : 5. none
deactivate DB
SessionRepo --> EmService : 6. Optional.empty()
deactivate SessionRepo
EmService -> SessionRepo : 7. save(EmergencySession{status=ACTIVE,\nuserLatitude, userLongitude})
activate SessionRepo
SessionRepo -> DB : 8. INSERT INTO emergency_sessions ...
activate DB
DB --> SessionRepo : 9. saved
deactivate DB
SessionRepo --> EmService : 10. EmergencySession
deactivate SessionRepo
EmService -> Handler : 11. publishEvent(EmergencySessionOpened)\n→ onEmergencySessionOpened(event)\n[Spring @EventListener synchronous, same transaction]
activate Handler
Handler -> FamilyAlertSvc : 12. sendAlert(event)
activate FamilyAlertSvc
FamilyAlertSvc -> AlertLogRepo : 13. existsBySessionId(sessionId)\n[guard idempotent]
activate AlertLogRepo
AlertLogRepo -> DB : 14. SELECT EXISTS(...) FROM family_alert_logs\nWHERE session_id=?
activate DB
DB --> AlertLogRepo : 15. false
deactivate DB
AlertLogRepo --> FamilyAlertSvc : 16. boolean
deactivate AlertLogRepo
FamilyAlertSvc -> FamMember : 17. getFamilyFcmTokens(userId)\n[CareGroup ACTIVE → CareGroupMember ACCEPTED\n→ DeviceToken active, DO NOT use EmergencyContact]
activate FamMember
FamMember --> FamilyAlertSvc : 18. fcmTokens[]
deactivate FamMember
FamilyAlertSvc -> Consent : 19. hasLocationConsent(userId)\n[ConsentGrantRepository: LOCATION/SHARE still valid?]
activate Consent
Consent --> FamilyAlertSvc : 20. boolean hasConsent
deactivate Consent
FamilyAlertSvc -> FamilyAlertSvc : 21. build payload\n(include latitude/longitude ONLY when hasConsent=true — PDPA)
alt 22. FCM sent successfully
  FamilyAlertSvc -> Fcm : 22. sendBatch(fcmTokens, payload)
  activate Fcm
  Fcm --> FamilyAlertSvc : 23. void
  deactivate Fcm
else 22. FCM error → fallback SMS (must not block service)
  FamilyAlertSvc -> Sms : 22a. sendFallback(userId, sessionId,\n"Emergency alert fallback triggered...")
  activate Sms
  Sms --> FamilyAlertSvc : 22b. void
  deactivate Sms
end
FamilyAlertSvc -> AlertLogRepo : 24. save(FamilyAlertLog{recipientCount=fcmTokens.size(),\nlocationIncluded})
activate AlertLogRepo
AlertLogRepo -> DB : 25. INSERT INTO family_alert_logs ...
activate DB
DB --> AlertLogRepo : 26. saved
deactivate DB
AlertLogRepo --> FamilyAlertSvc : 27. FamilyAlertLog
deactivate AlertLogRepo
FamilyAlertSvc -> FamilyAlertSvc : 28. publishEvent(FamilyAlertSent) [internal, no UC currently subscribed]
FamilyAlertSvc --> Handler : 29. void
deactivate FamilyAlertSvc
Handler --> EmService : 30. void (listener completed, same thread/transaction)
deactivate Handler
EmService --> EmController : 31. EmergencySessionResponse{status=ACTIVE}
deactivate EmService
EmController --> M : 32. HTTP 201 Created
deactivate EmController

== UC-81 Create or Cancel Nearby Support Request ==
M -> NearController : 33. POST /api/v1/nearbycare/support-requests\n{supportType, latitude, longitude, description, consentStatus}
activate NearController
NearController -> NearService : 34. createRequest(userId, request)
activate NearService
NearService -> NearService : 35. map request → NearbySupportRequest{status=OPEN}\n(NearbySupportMapper.toEntity)
NearService -> ReqRepo : 36. save(entity)
activate ReqRepo
ReqRepo -> DB : 37. INSERT INTO nearby_support_requests ...
activate DB
DB --> ReqRepo : 38. saved
deactivate DB
ReqRepo --> NearService : 39. NearbySupportRequest
deactivate ReqRepo
NearService --> NearController : 40. NearbySupportRequest{status=OPEN}
deactivate NearService
NearController --> M : 41. HTTP 201 Created
deactivate NearController

M -> NearController : 42. DELETE /api/v1/nearbycare/support-requests/{requestId}
activate NearController
NearController -> NearService : 43. cancelRequest(requestId, userId)
activate NearService
NearService -> ReqRepo : 44. findById(requestId)
activate ReqRepo
ReqRepo -> DB : 45. SELECT * FROM nearby_support_requests WHERE id=?
activate DB
DB --> ReqRepo : 46. request row
deactivate DB
ReqRepo --> NearService : 47. NearbySupportRequest
deactivate ReqRepo
NearService -> NearService : 48. check requesterUserId matches\n&& status==OPEN (if not → 403/400)
NearService -> ReqRepo : 49. save(request{status=CANCELLED})
activate ReqRepo
ReqRepo -> DB : 50. UPDATE nearby_support_requests SET status='CANCELLED'
activate DB
DB --> ReqRepo : 51. updated
deactivate DB
ReqRepo --> NearService : 52. NearbySupportRequest
deactivate ReqRepo
NearService --> NearController : 53. NearbySupportRequest{status=CANCELLED}
deactivate NearService
NearController --> M : 54. HTTP 200 OK
deactivate NearController

== UC-82 Manage Expert Nearby Availability and Respond to Nearby Support Request ==
Exp -> NearController : 55. GET /api/v1/nearbycare/support-requests/open
activate NearController
NearController -> ExpertRepo : 56. findByUserId(expertUserId)\n[only to retrieve expertProfileId — not used to filter results]
activate ExpertRepo
ExpertRepo -> DB : 57. SELECT * FROM expert_profiles WHERE user_id=?
activate DB
DB --> ExpertRepo : 58. profile row
deactivate DB
ExpertRepo --> NearController : 59. ExpertProfile
deactivate ExpertRepo
NearController -> NearService : 60. getOpenRequests()
activate NearService
NearService -> ReqRepo : 61. findByStatus(OPEN)
activate ReqRepo
ReqRepo -> DB : 62. SELECT * FROM nearby_support_requests WHERE status='OPEN'
activate DB
DB --> ReqRepo : 63. rows[]
deactivate DB
ReqRepo --> NearService : 64. requests[]
deactivate ReqRepo
NearService --> NearController : 65. requests[] (all OPEN requests in system)
deactivate NearService
NearController --> Exp : 66. HTTP 200 OK {requests[]}
deactivate NearController

Exp -> NearController : 67. POST /api/v1/nearbycare/support-requests/{requestId}/respond\n{action=ACCEPT, note}
activate NearController
NearController -> ExpertRepo : 68. findByUserId(expertUserId)
activate ExpertRepo
ExpertRepo -> DB : 69. SELECT * FROM expert_profiles WHERE user_id=?
activate DB
DB --> ExpertRepo : 70. profile row
deactivate DB
ExpertRepo --> NearController : 71. ExpertProfile{expertProfileId}
deactivate ExpertRepo
NearController -> NearService : 72. respondToRequest(requestId, expertProfileId, request)
activate NearService
NearService -> ReqRepo : 73. findById(requestId)
activate ReqRepo
ReqRepo -> DB : 74. SELECT * FROM nearby_support_requests WHERE id=?
activate DB
DB --> ReqRepo : 75. request row (status must be == OPEN)
deactivate DB
ReqRepo --> NearService : 76. NearbySupportRequest
deactivate ReqRepo
NearService -> ExpertRepo : 77. findById(expertProfileId)
activate ExpertRepo
ExpertRepo -> DB : 78. SELECT * FROM expert_profiles WHERE id=?
activate DB
DB --> ExpertRepo : 79. profile row
deactivate DB
ExpertRepo --> NearService : 80. ExpertProfile{verificationStatus}
deactivate ExpertRepo
alt 81. verificationStatus == APPROVED (verified expert)
  NearService -> RespRepo : 81. save(NearbySupportResponse{action=ACCEPT, note})
  activate RespRepo
  RespRepo -> DB : 82. INSERT INTO nearby_support_responses ...
  activate DB
  DB --> RespRepo : 83. saved
  deactivate DB
  RespRepo --> NearService : 84. NearbySupportResponse
  deactivate RespRepo
  opt 85. action == ACCEPT
    NearService -> ReqRepo : 85a. save(request{status=ACCEPTED, respondedAt=now()})
    activate ReqRepo
    ReqRepo -> DB : 85b. UPDATE nearby_support_requests\nSET status='ACCEPTED', responded_at=now()
    activate DB
    DB --> ReqRepo : 85c. updated
    deactivate DB
    ReqRepo --> NearService : 85d. NearbySupportRequest
    deactivate ReqRepo
  end
  NearService --> NearController : 86. NearbySupportResponse
  deactivate NearService
  NearController --> Exp : 87. HTTP 200 OK
  deactivate NearController
else 81. verificationStatus != APPROVED → block
  NearService --> NearController : 81a. throw ExpertException(FORBIDDEN,\n"Only verified experts can respond")
  deactivate NearService
  NearController --> Exp : 81b. HTTP 403 Forbidden
  deactivate NearController
end

@enduml
```

**Hình 2 — Sequence Diagram: Send Family Alert (event-driven) → Create/Cancel Nearby Support Request → Expert Responds (Main Flow)**

> **Ghi chú grounding (quan trọng — lệch với Class Diagram & Business Rules):**
> 1. `EmergencyContact` (entity/`EmergencyContactController`) **không** được dùng để gửi
>    cảnh báo khẩn cấp. Danh sách người nhận thật sự lấy từ thành viên `CareGroup` (module
>    Family, MF-10) đang `ACTIVE`/`ACCEPTED` qua `FamilyMemberPort` (device token FCM), hoàn
>    toàn độc lập với entity `EmergencyContact`. Cạnh `EmergencySession *-- FamilyAlertLog`
>    trong class diagram đúng, nhưng liên hệ tới `EmergencyContact` ở đó là khái niệm, không
>    khớp pipeline thật.
> 2. `NearbySupportServiceImpl.getOpenRequests()` trả về **toàn bộ** request `OPEN` của hệ
>    thống — không có join `ExpertAvailability`, không lọc bán kính (`ST_DWithin`), không
>    kiểm tra `verificationStatus` ở bước liệt kê. Việc chỉ "verified expert" mới thao tác
>    được chỉ enforce ở bước `respondToRequest` (kiểm tra `VerificationStatus.APPROVED`) —
>    khác với mô tả mục 5 ("chỉ hiển thị cho verified expert đủ điều kiện trong bán kính phù
>    hợp"). Cạnh `NearbySupportServiceImpl --> ExpertAvailabilityRepository` trong class
>    diagram không tồn tại trong code (`ExpertAvailabilityRepository` không được inject).
> 3. Hành động `STOP` (`ResponseAction`) chỉ lưu một `NearbySupportResponse`, **không** đổi
>    `NearbySupportRequest.status` — transition `ACCEPTED --> OPEN` do "Expert phản hồi STOP"
>    ở State Machine (mục 4) hiện **không được code thực thi**; chỉ `ACCEPT` mới đổi status
>    (sang `ACCEPTED`).

## 4. State Machine — `NearbySupportRequest.status`

```plantuml
@startuml MF07_02_NearbySupportStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> OPEN : Mother tạo yêu cầu (UC-81)

OPEN --> ACCEPTED : Verified Expert phản hồi ACCEPT (UC-82)
OPEN --> CANCELLED : Mother huỷ yêu cầu (UC-81)
ACCEPTED --> COMPLETED : Hỗ trợ hoàn tất
ACCEPTED --> OPEN : Expert phản hồi STOP\n[quay lại mở cho expert khác]

CANCELLED --> [*]
COMPLETED --> [*]

note right of OPEN
  DECLINE (ResponseAction) không đổi status của request —
  chỉ ghi nhận một NearbySupportResponse để request tiếp tục
  hiển thị cho các expert đủ điều kiện khác.
end note

@enduml
```

**Hình 3 — State Machine: `NearbySupportRequest.status` Lifecycle**

## 5. Business Rules Applied

- UC-80 — chỉ chia sẻ vị trí/ngữ cảnh tối thiểu cần thiết (minimum necessary), có thời hạn, gửi tới danh sách `EmergencyContact` đã cấu hình trước (MF-14 liên kết `EmergencyContact` dùng chung).
- UC-81 — request chỉ hiển thị cho verified expert đủ điều kiện (đã APPROVED + ACTIVE trust status + bật nearby availability), không public toàn hệ thống.
- UC-82 — expert chỉ thấy "minimal request context" (không thấy toàn bộ hồ sơ sức khỏe của Mother) trước khi quyết định phản hồi.
- Excluded (SRS 4.8) — không đảm bảo expert đến, không phải dịch vụ điều phối xe cấp cứu/y tế chính thức.
