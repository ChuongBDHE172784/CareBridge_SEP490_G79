# MF-01 / Spec 02 — Community Identity, Privacy & Consent-Based Data Sharing

| Field | Value |
| --- | --- |
| Feature | MF-01 — Account, Trust & Access Control |
| Use Cases Covered | UC-15 Grant Data Permission, UC-16 Review and Revoke Data Permission |
| Primary Actor(s) | User (owner of private data) |
| Platform | Mobile App / Web Portal |
| Main Flow Summary | A User maintains a unified `UserProfile` (managing account details and community presentation attributes such as display name and avatar), and separately grants a purpose-scoped, time-limited `ConsentGrant` so a family member or verified expert may access selected private data — with the owner able to review and revoke that grant at any time. |
| Grounding (source code) | `profile/entity/UserProfile.java`, `consent/entity/ConsentGrant.java`, `ConsentPurpose.java`, `ConsentDataType.java`, `consent/controller/*` (`/api/v1/consent/grants`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

MF-01 quản lý danh tính người dùng thông qua `UserProfile` (chứa dữ liệu vận hành tài khoản và thông tin hiển thị cộng đồng như display name, avatar). Việc chia sẻ dữ liệu riêng tư (hồ sơ sức khỏe mẹ/bé, vị trí, dữ liệu gia đình...) cho người khác **không bao giờ** xảy ra ngầm định — nó luôn đi qua một `ConsentGrant` tường minh có `dataType` + `purpose` + `recipient` + `expiryAt`, do chính chủ dữ liệu tạo (UC-15) và có thể thu hồi bất cứ lúc nào (UC-16). Đây là cơ chế nền tảng mà MF-02/spec-04 (maternal health records), MF-03 (baby records) và MF-08 (Family Sync) tái sử dụng, nên được mô hình hoá tại MF-01 thay vì lặp lại ở từng feature tiêu dùng nó.

## 2. Class Diagram

```plantuml
@startuml MF01_02_ConsentIdentity_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class UserProfile <<profile>> {
  + profileId: UUID
  + userId: UUID
  + phoneNumber: String
  + dateOfBirth: LocalDate
  + area: String
  + avatarUrl: String
}

class ConsentGrant {
  + id: Long
  + userId: UUID
  + dataType: ConsentDataType
  + purpose: ConsentPurpose
  + recipient: String
  + scope: String
  + consentGivenAt: Instant
  + expiryAt: Instant
  + revokedAt: Instant
  + revokedBy: UUID
  + version: int
}

enum ConsentDataType {
  HEALTH_RECORD
  LOCATION
  FAMILY_DATA
  COMMUNITY_POST
  SENSITIVE_DATA
  RAG_CONTEXT
  EXPERT_SHARED_DATA
}

enum ConsentPurpose {
  VIEW
  CREATE
  UPDATE
  SHARE
  DELETE
}

class ConsentController {
  - consentService: ConsentService
  + grant(request): ResponseEntity
  + list(): ResponseEntity
  + revoke(consentId): ResponseEntity
}

interface ConsentService <<interface>> {
  + grantConsent(userId: UUID, request): ConsentGrantResponse
  + listConsents(userId: UUID): List<ConsentGrantResponse>
  + revokeConsent(userId: UUID, consentId: Long): ConsentGrantResponse
}

class ConsentServiceImpl implements ConsentService {
  - consentGrantRepository: ConsentGrantRepository
  - auditService: AuditService
}

interface ConsentGrantRepository <<interface>> {
  + findByUserId(userId: UUID): List<ConsentGrant>
  + save(grant: ConsentGrant): ConsentGrant
}

UserProfile "1" -- "*" ConsentGrant : owner data protected by >
ConsentGrant --> ConsentDataType
ConsentGrant --> ConsentPurpose
ConsentController --> ConsentService : uses
ConsentServiceImpl --> ConsentGrantRepository : uses

note bottom of UserProfile
  BR-PRIVACY: UserProfile thống nhất danh tính
  cho cả tài khoản cá nhân và hiển thị cộng đồng.
end note

@enduml
```

**Hình 1 — Class Diagram: User Profile & Consent Grant**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF01_02_ConsentIdentity_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "User (Owner)" as Owner
actor "Family Member / Verified Expert" as Recipient
participant "Web / Mobile UI" as UI
participant "ConsentController" as CGController
participant "ConsentServiceImpl" as Service
participant "AuditService" as Audit
participant "ConsentGrantRepository" as Repo
database "PostgreSQL" as DB

== UC-15 Grant Data Permission ==
Owner -> UI : 1. Submit request
activate UI
UI -> CGController : 1a. POST /api/v1/consent/grants\n{dataType, purpose, recipient, scope, expiryAt}
activate CGController
CGController -> Service : 2. grantConsent(ownerId, request)
activate Service
Service -> Repo : 3. save(ConsentGrant{consentGivenAt=now})
activate Repo
Repo -> DB : 4. INSERT INTO consent_grants ...
activate DB
DB --> Repo : 5. saved
deactivate DB
Repo --> Service : 6. ConsentGrant
deactivate Repo
Service -> Audit : 7. log(CONSENT_GRANTED)
activate Audit
Audit --> Service : 8. void
deactivate Audit
Service --> CGController : 9. ConsentGrant
deactivate Service
CGController --> UI : 10. HTTP 201 Created
deactivate CGController
UI --> Owner : 10a. Display HTTP 201 Created
deactivate UI

Recipient -> Recipient : 11. accesses shared data\n(checked via NS-02 consent enforcement\nin the consuming feature, e.g. MF-02/MF-08)

== UC-16 Review and Revoke Data Permission ==
Owner -> UI : 12. Submit request
activate UI
UI -> CGController : 12a. GET /api/v1/consent/grants
activate CGController
CGController -> Service : 13. listConsents(ownerId)
activate Service
Service -> Repo : 14. findByUserId(ownerId)
activate Repo
Repo -> DB : 15. SELECT * FROM consent_grants WHERE user_id=?
activate DB
DB --> Repo : 16. grants[]
deactivate DB
Repo --> Service : 17. grants[]
deactivate Repo
Service --> CGController : 18. grants[]
deactivate Service
CGController --> UI : 19. HTTP 200 OK {grants[]}
deactivate CGController
UI --> Owner : 19a. Display HTTP 200 OK {grants[]}
deactivate UI

Owner -> UI : 20. Submit request
activate UI
UI -> CGController : 20a. DELETE /api/v1/consent/grants/{consentId}
activate CGController
CGController -> Service : 21. revokeConsent(ownerId, consentId)
activate Service
Service -> Repo : 22. setRevokedAt(consentId, now, revokedBy=ownerId)
activate Repo
Repo -> DB : 23. UPDATE consent_grants SET revoked_at=now()
activate DB
DB --> Repo : 24. updated
deactivate DB
Repo --> Service : 25. void
deactivate Repo
Service -> Audit : 26. log(CONSENT_REVOKED)
activate Audit
Audit --> Service : 27. void
deactivate Audit
Service --> CGController : 28. void
deactivate Service
CGController --> UI : 29. HTTP 204 No Content
deactivate CGController
UI --> Owner : 29a. Display HTTP 204 No Content
deactivate UI

@enduml
```

**Hình 2 — Sequence Diagram: Grant → Consume → Revoke Data Permission (Main Flow)**


## 4. Business Rules Applied

- BR-PRIVACY — dữ liệu riêng tư (mẹ/bé/gia đình/sức khỏe) được bảo vệ và quản lý truy cập tường minh qua ConsentGrant.
- BR-COMMUNITY-01 — display name/avatar hiển thị công khai trên hệ thống chịu kiểm duyệt nội dung.
- NS-02 — mọi truy cập dữ liệu bảo vệ phải kiểm tra recipient, purpose, scope, expiry và tình trạng thu hồi trước khi cho phép.
