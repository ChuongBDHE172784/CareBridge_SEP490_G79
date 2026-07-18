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
participant "EmergencyServiceImpl" as EmService
participant "NotificationService" as Notif
participant "NearbySupportController" as NearController
participant "NearbySupportServiceImpl" as NearService
actor "Verified Expert" as Exp
database "PostgreSQL" as DB

== UC-80 Share Time-Limited Location and Send Family Emergency Alert ==
M -> EmController : POST /api/v1/emergency/sessions\n{userLatitude, userLongitude, triggerSource="MANUAL"}
EmController -> EmService : open(userId, request)
EmService -> DB : INSERT INTO emergency_sessions (status=ACTIVE)
EmService -> DB : SELECT * FROM emergency_contacts WHERE user_id=?
EmService -> Notif : notify(contacts, minimumLocationContext, expiresInMinutes)
Notif --> EmService : recipientCount
EmService -> DB : INSERT INTO family_alert_logs\n(recipientCount, locationIncluded=true)
EmService --> EmController : EmergencySession
EmController --> M : HTTP 201 Created

== UC-81 Create or Cancel Nearby Support Request ==
M -> NearController : POST /api/v1/nearbycare/support-requests\n{supportType, latitude, longitude, description}
NearController -> NearService : create(requesterId, request)
NearService -> DB : INSERT INTO nearby_support_requests\n(status=OPEN, consentStatus="GRANTED")
NearService --> NearController : NearbySupportRequest{status=OPEN}
NearController --> M : HTTP 201 Created

M -> NearController : DELETE /api/v1/nearbycare/support-requests/{requestId}
NearController -> NearService : cancel(requesterId, requestId)
NearService -> DB : UPDATE nearby_support_requests SET status='CANCELLED'
NearController --> M : HTTP 204 No Content

== UC-82 Manage Expert Nearby Availability and Respond to Nearby Support Request ==
Exp -> NearController : GET /api/v1/nearbycare/support-requests/open
NearController -> NearService : listOpen(expertProfileId)
NearService -> DB : SELECT * FROM nearby_support_requests r\nJOIN expert_availability a ON ... WHERE r.status='OPEN'\nAND ST_DWithin(r.location, expert.location, radius)
DB --> NearService : requests[] (minimal context only)
NearService --> Exp : HTTP 200 OK {requests[]}

Exp -> NearController : POST /api/v1/nearbycare/support-requests/{requestId}/respond\n{action=ACCEPT}
NearController -> NearService : respond(expertProfileId, requestId, ACCEPT)
NearService -> DB : INSERT INTO nearby_support_responses (action=ACCEPT)
NearService -> DB : UPDATE nearby_support_requests\nSET status='ACCEPTED', responded_at=now()
NearService --> NearController : NearbySupportResponse
NearController --> Exp : HTTP 200 OK

@enduml
```

**Hình 2 — Sequence Diagram: Send Family Alert → Create Nearby Support Request → Expert Responds (Main Flow)**

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
