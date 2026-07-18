# MF-01 / Spec 03 — Admin Account Governance & Security Audit

| Field | Value |
| --- | --- |
| Feature | MF-01 — Account, Trust & Access Control |
| Use Cases Covered | UC-14 Deactivate or Delete Own Account, UC-17 Administer User Accounts and Role Access, UC-18 Review Sensitive Access and Security Events |
| Primary Actor(s) | User (self-service deletion request), System Admin |
| Platform | Mobile App / Web Portal (UC-14), Admin Portal (UC-17, UC-18) |
| Main Flow Summary | A User requests deactivation/deletion of their own account; a System Admin reviews and processes that request (or independently changes a user's status/role under separation-of-duties), while every sensitive action and abnormal security signal is captured as an auditable `SecurityEvent` the admin can triage. |
| Grounding (source code) | `account/entity/AccountDeletionRequest.java`, `DeletionStatus.java`, `account/controller/AccountDeletionController.java`, `security/entity/User.java` (role/accountStatus), admin user controller (`/api/v1/admin/users`), `audit/entity/SecurityEvent.java`, `audit/entity/AuditLog.java`, `audit/controller/SecurityIncidentController.java` (`/api/v1/admin/security-events`), `audit/controller/AuditController.java` (`/api/v1/admin/audit-logs`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là lớp "trust & access control" cấp quản trị của MF-01. User tự khởi tạo yêu cầu
xoá/vô hiệu hoá tài khoản (UC-14) — hệ thống không xoá ngay mà tạo một
`AccountDeletionRequest` chờ xử lý theo nghĩa vụ lưu trữ/audit. System Admin xử lý các
yêu cầu đó và độc lập có quyền thay đổi trạng thái/role của bất kỳ tài khoản nào (UC-17)
dưới ràng buộc phân quyền (separation-of-duties: admin không tự duyệt yêu cầu chính
mình tạo ra). Song song đó, mọi hành động nhạy cảm được ghi vào `AuditLog`
(bất biến, chỉ đọc) còn các tín hiệu bất thường (đăng nhập sai nhiều lần, truy cập bị
từ chối, hoạt động khả nghi...) được hệ thống tự sinh thành `SecurityEvent` để admin rà
soát và xử lý (UC-18) — đây là control trung tâm cho toàn bộ hệ thống, không riêng MF-01.

## 2. Class Diagram

```plantuml
@startuml MF01_03_AdminGovernance_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class User {
  + id: UUID
  + role: Role
  + accountStatus: String
  + enabled: boolean
  + locked: boolean
}

class AccountDeletionRequest {
  + id: UUID
  + userId: UUID
  + status: DeletionStatus
  + reason: String
  + requestedAt: Instant
  + scheduledFor: Instant
  + processedAt: Instant
  + processedBy: UUID
  + notes: String
}

enum DeletionStatus {
  PENDING
  SCHEDULED
  COMPLETED
  CANCELLED
}

class SecurityEvent {
  + id: Long
  + occurredAt: Instant
  + eventType: SecurityEventType
  + userId: UUID
  + ipAddress: String
  + severity: String
  + status: String
  + reviewedBy: UUID
  + reviewedAt: Instant
}

enum SecurityEventType {
  LOGIN_FAILED
  PERMISSION_DENIED
  SUSPICIOUS_ACTIVITY
  TOKEN_REVOKED
  OTP_ATTEMPT_LIMIT_EXCEEDED
}

class SecurityEventNote {
  + id: Long
  + eventId: Long
  + note: String
  + addedBy: UUID
  + addedAt: Instant
}

class AuditLog <<append-only>> {
  + auditLogId: UUID
  + actorUserId: UUID
  + action: AuditAction
  + entityType: String
  + entityId: UUID
  + oldValueJson: String
  + newValueJson: String
  + createdAt: Instant
}

enum AuditAction {
  USER_ACCOUNT_STATUS_CHANGED
  ROLE_PERMISSION_UPDATED
  STAFF_ACCOUNT_CREATED
  SECURITY_EVENT_REVIEWED
  ' + ~140 giá trị khác theo domain (xem audit/entity/AuditAction.java)
}

class AccountDeletionController {
  + requestDeletion(request): ResponseEntity
  + cancelDeletion(): ResponseEntity
}

class AdminUserController {
  - adminUserService: AdminUserService
  + listUsers(filter): ResponseEntity
  + updateStatus(userId, request): ResponseEntity
  + updateRole(userId, request): ResponseEntity
}

class SecurityIncidentController {
  - securityIncidentService: SecurityIncidentService
  + listEvents(filter): ResponseEntity
  + reviewEvent(eventId, ReviewSecurityEventRequest): ResponseEntity
  + addNote(eventId, request): ResponseEntity
}

interface AdminUserService <<interface>> {
  + changeStatus(adminId: UUID, userId: UUID, status: String): void
  + changeRole(adminId: UUID, userId: UUID, role: Role): void
}

class AdminUserServiceImpl implements AdminUserService {
  - userRepository: UserRepository
  - auditService: AuditService
  - accountDeletionRequestRepository: AccountDeletionRequestRepository
}

User "1" -- "0..1" AccountDeletionRequest : requests
AccountDeletionRequest --> DeletionStatus
SecurityEvent --> SecurityEventType
SecurityEvent "1" *-- "0..*" SecurityEventNote : has
AuditLog --> AuditAction
AccountDeletionController --> AdminUserService : uses (cancel/self-request path)
AdminUserController --> AdminUserService : uses
AdminUserServiceImpl --> AuditService : emits AuditLog
SecurityIncidentController --> SecurityIncidentService : uses

@enduml
```

**Hình 1 — Class Diagram: Account Deletion Request, Admin User Governance & Security Audit**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF01_03_AdminGovernance_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "User" as U
participant "AccountDeletionController" as DelController
actor "System Admin" as Admin
participant "AdminUserController" as AdminController
participant "AdminUserServiceImpl" as AdminService
participant "SecurityIncidentController" as SecController
participant "SecurityIncidentService" as SecService
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-14 Deactivate or Delete Own Account ==
U -> DelController : POST /api/v1/account/deletion-request\n{reason}
DelController -> DB : INSERT INTO account_deletion_requests\n(status=PENDING, requested_at=now)
DelController -> Audit : emit(SECURITY_EVENT, "deletion_requested")
DelController --> U : HTTP 201 Created {status=PENDING}

== UC-17 Administer User Accounts and Role Access ==
Admin -> AdminController : GET /api/v1/admin/users?status=PENDING_DELETION
AdminController -> AdminService : listUsers(filter)
AdminService -> DB : SELECT ... FROM users JOIN account_deletion_requests
DB --> AdminService : users[]
AdminService --> AdminController : users[]
AdminController --> Admin : HTTP 200 OK {users[]}

Admin -> AdminController : PATCH /api/v1/admin/users/{userId}/status\n{status="DEACTIVATED"}
AdminController -> AdminService : changeStatus(adminId, userId, "DEACTIVATED")
AdminService -> AdminService : check adminId != userId\n(separation-of-duties)
AdminService -> DB : UPDATE users SET account_status='DEACTIVATED'
AdminService -> DB : UPDATE account_deletion_requests\nSET status='COMPLETED', processed_by=adminId
AdminService -> Audit : emit(USER_ACCOUNT_STATUS_CHANGED)
AdminService --> AdminController : void
AdminController --> Admin : HTTP 200 OK

== UC-18 Review Sensitive Access and Security Events ==
Admin -> SecController : GET /api/v1/admin/security-events?status=OPEN
SecController -> SecService : listEvents(filter)
SecService -> DB : SELECT * FROM security_events WHERE status='OPEN'
DB --> SecService : events[]
SecService --> SecController : events[]
SecController --> Admin : HTTP 200 OK {events[]}

Admin -> SecController : PUT /api/v1/admin/security-events/{eventId}/review\n{status="RESOLVED"}
SecController -> SecService : reviewEvent(adminId, eventId, "RESOLVED")
SecService -> DB : UPDATE security_events\nSET status='RESOLVED', reviewed_by=adminId, reviewed_at=now()
SecService -> Audit : emit(SECURITY_EVENT_REVIEWED)
SecService --> SecController : void
SecController --> Admin : HTTP 200 OK

@enduml
```

**Hình 2 — Sequence Diagram: Deletion Request → Admin Processing → Security Event Review (Main Flow)**

## 4. State Machine — `AccountDeletionRequest.status` & `SecurityEvent.status`

```plantuml
@startuml MF01_03_AdminGovernance_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

state "AccountDeletionRequest" as ADR {
  [*] --> PENDING : User requests (UC-14)
  PENDING --> SCHEDULED : Admin schedules xử lý theo retention policy (UC-17)
  PENDING --> CANCELLED : User huỷ yêu cầu trước khi xử lý
  SCHEDULED --> COMPLETED : Admin hoàn tất xử lý (UC-17)
  SCHEDULED --> CANCELLED : Admin/User huỷ trong thời hạn chờ
  COMPLETED --> [*]
  CANCELLED --> [*]
}

state "SecurityEvent" as SE {
  [*] --> OPEN : Hệ thống tự phát sinh sự kiện\n(login fail, permission denied, ...)
  OPEN --> UNDER_REVIEW : Admin bắt đầu rà soát (UC-18)
  UNDER_REVIEW --> RESOLVED : Admin xác nhận đã xử lý
  UNDER_REVIEW --> FALSE_POSITIVE : Admin xác nhận không phải sự cố thật
  RESOLVED --> [*]
  FALSE_POSITIVE --> [*]
}

@enduml
```

**Hình 3 — State Machine: `AccountDeletionRequest.status` và `SecurityEvent.status`**

## 5. Business Rules Applied

- BR-RBAC — chỉ System Admin mới có quyền thay đổi role/status của tài khoản khác.
- Separation-of-duties — admin không tự xử lý yêu cầu do chính họ tạo hoặc tự thay đổi role của chính mình qua kênh này.
- UC-14 postcondition — deletion/deactivation phải tuân thủ nghĩa vụ retention, care-group và audit trước khi hoàn tất.
- NS-01 — mọi sự kiện bảo mật bất thường (đăng nhập sai, truy cập bị từ chối...) được ghi nhận tự động.
- AuditLog là append-only: `oldValueJson`/`newValueJson` phục vụ truy vết, không được sửa/xoá sau khi ghi.
