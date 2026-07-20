# MF-01 / Spec 02 — Community Identity, Privacy & Consent-Based Data Sharing

| Field | Value |
| --- | --- |
| Feature | MF-01 — Account, Trust & Access Control |
| Use Cases Covered | UC-10 Manage Community Identity, UC-15 Grant Data Permission, UC-16 Review and Revoke Data Permission |
| Primary Actor(s) | User (owner of private data) |
| Platform | Mobile App / Web Portal |
| Main Flow Summary | A User maintains a public `CommunityProfile` that is structurally separate from the private `UserProfile`, and separately grants a purpose-scoped, time-limited `ConsentGrant` so a family member or verified expert may access selected private data — with the owner able to review and revoke that grant at any time. |
| Grounding (source code) | `community/entity/CommunityProfile.java`, `profile/entity/UserProfile.java`, `consent/entity/ConsentGrant.java`, `ConsentPurpose.java`, `ConsentDataType.java`, `consent/controller/*` (`/api/v1/consent/grants`), `community/controller/CommunityProfileController.java` (`/api/v1/community/profiles`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

MF-01 tách rõ hai lớp danh tính (BR-PRIVACY): `UserProfile` là dữ liệu riêng tư dùng
để vận hành tài khoản, còn `CommunityProfile` là danh tính công khai hiển thị trong cộng
đồng (display name, avatar công khai, có thể ẩn danh — BR-PRIVACY-02). Việc chia sẻ dữ
liệu riêng tư (hồ sơ sức khỏe mẹ/bé, vị trí, dữ liệu gia đình...) cho người khác **không
bao giờ** xảy ra ngầm định — nó luôn đi qua một `ConsentGrant` tường minh có
`dataType` + `purpose` + `recipient` + `expiryAt`, do chính chủ dữ liệu tạo (UC-15) và
có thể thu hồi bất cứ lúc nào (UC-16). Đây là cơ chế nền tảng mà MF-08 (Health Records)
và MF-10 (Family Sync) tái sử dụng, nên được mô hình hoá tại MF-01 thay vì lặp lại ở
từng feature tiêu dùng nó.

## 2. Class Diagram

```plantuml
@startuml MF01_02_ConsentIdentity_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class UserProfile <<private>> {
  + profileId: UUID
  + userId: UUID
  + phoneNumber: String
  + dateOfBirth: LocalDate
  + area: String
}

class CommunityProfile <<public>> {
  + communityProfileId: UUID
  + userId: UUID
  + displayName: String
  + bio: String
  + publicAvatarUrl: String
  + interestStage: String
  + visible: boolean
  + region: String
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

class CommunityProfileController {
  - communityProfileService: CommunityProfileService
  + create(request): ResponseEntity
  + updateMine(request): ResponseEntity
}

class ConsentGrantController {
  - consentGrantService: ConsentGrantService
  + create(request): ResponseEntity
  + listMine(): ResponseEntity
  + revoke(consentId): ResponseEntity
}

interface ConsentGrantService <<interface>> {
  + grant(ownerId: UUID, request): ConsentGrant
  + listForOwner(ownerId: UUID): List<ConsentGrant>
  + revoke(ownerId: UUID, consentId: Long): void
}

class ConsentGrantServiceImpl implements ConsentGrantService {
  - consentGrantRepository: ConsentGrantRepository
  - auditService: AuditService
}

interface ConsentGrantRepository <<interface>> {
  + findByUserId(userId: UUID): List<ConsentGrant>
  + save(grant: ConsentGrant): ConsentGrant
}

UserProfile "1" -- "1" ConsentGrant : owner data protected by >
ConsentGrant --> ConsentDataType
ConsentGrant --> ConsentPurpose
ConsentGrantController --> ConsentGrantService : uses
ConsentGrantServiceImpl --> ConsentGrantRepository : uses
CommunityProfileController --> CommunityProfileService : uses

note bottom of CommunityProfile
  BR-PRIVACY-02: CommunityProfile không bao giờ
  chứa health/baby/family record — chỉ danh tính công khai.
end note

@enduml
```

**Hình 1 — Class Diagram: Community Identity vs. Private Profile & Consent Grant**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF01_02_ConsentIdentity_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "User (Owner)" as Owner
participant "CommunityProfileController" as CPController
participant "CommunityProfileServiceImpl" as CPService
participant "CommunityProfileRepository" as CPRepo
participant "ConsentGrantController" as CGController
participant "ConsentGrantServiceImpl" as Service
participant "ConsentGrantRepository" as Repo
participant "AuditService" as Audit
database "PostgreSQL" as DB
actor "Family Member / Verified Expert" as Recipient

== UC-10 Manage Community Identity ==
Owner -> CPController : 1. PUT /api/v1/community/profiles/me\n{displayName, bio, avatarUrl, visible}
activate CPController
CPController -> CPService : 2. updateMine(ownerId, request)
activate CPService
CPService -> CPService : 3. validate against moderation rules\n(BR-COMMUNITY-01)
CPService -> CPRepo : 4. save(CommunityProfile{displayName, bio, visible})
activate CPRepo
CPRepo -> DB : 5. UPDATE community_profiles SET ...
activate DB
DB --> CPRepo : 6. updated
deactivate DB
CPRepo --> CPService : 7. CommunityProfile
deactivate CPRepo
CPService --> CPController : 8. CommunityProfile
deactivate CPService
CPController --> Owner : 9. HTTP 200 OK {CommunityProfile}
deactivate CPController

== UC-15 Grant Data Permission ==
Owner -> CGController : 10. POST /api/v1/consent/grants\n{dataType, purpose, recipient, scope, expiryAt}
activate CGController
CGController -> Service : 11. grant(ownerId, request)
activate Service
Service -> Repo : 12. save(ConsentGrant{consentGivenAt=now})
activate Repo
Repo -> DB : 13. INSERT INTO consent_grants ...
activate DB
DB --> Repo : 14. saved
deactivate DB
Repo --> Service : 15. ConsentGrant
deactivate Repo
Service -> Audit : 16. log(CONSENT_GRANTED)
activate Audit
Audit --> Service : 17. void
deactivate Audit
Service --> CGController : 18. ConsentGrant
deactivate Service
CGController --> Owner : 19. HTTP 201 Created
deactivate CGController

Recipient -> Recipient : 20. accesses shared data\n(checked via NS-02 consent enforcement\nin the consuming feature, e.g. MF-08/MF-10)

== UC-16 Review and Revoke Data Permission ==
Owner -> CGController : 21. GET /api/v1/consent/grants
activate CGController
CGController -> Service : 22. listForOwner(ownerId)
activate Service
Service -> Repo : 23. findByUserId(ownerId)
activate Repo
Repo -> DB : 24. SELECT * FROM consent_grants WHERE user_id=?
activate DB
DB --> Repo : 25. grants[]
deactivate DB
Repo --> Service : 26. grants[]
deactivate Repo
Service --> CGController : 27. grants[]
deactivate Service
CGController --> Owner : 28. HTTP 200 OK {grants[]}
deactivate CGController

Owner -> CGController : 29. DELETE /api/v1/consent/grants/{consentId}
activate CGController
CGController -> Service : 30. revoke(ownerId, consentId)
activate Service
Service -> Repo : 31. setRevokedAt(consentId, now, revokedBy=ownerId)
activate Repo
Repo -> DB : 32. UPDATE consent_grants SET revoked_at=now()
activate DB
DB --> Repo : 33. updated
deactivate DB
Repo --> Service : 34. void
deactivate Repo
Service -> Audit : 35. log(CONSENT_REVOKED)
activate Audit
Audit --> Service : 36. void
deactivate Audit
Service --> CGController : 37. void
deactivate Service
CGController --> Owner : 38. HTTP 204 No Content
deactivate CGController

@enduml
```

**Hình 2 — Sequence Diagram: Grant → Consume → Revoke Data Permission (Main Flow)**

## 4. State Machine — `ConsentGrant` Lifecycle

```plantuml
@startuml MF01_02_ConsentGrant_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : POST /consent/grants (UC-15)\nconsentGivenAt = now

ACTIVE --> EXPIRED : now() > expiryAt\n[hệ thống tự đánh giá khi kiểm tra quyền truy cập]
ACTIVE --> REVOKED : DELETE /consent/grants/{id} (UC-16)\nrevokedAt = now(), revokedBy = owner

EXPIRED --> [*]
REVOKED --> [*]

note right of ACTIVE
  ConsentGrant không có cột status riêng — trạng thái
  được suy ra (derived) từ consentGivenAt / expiryAt / revokedAt
  mỗi lần NS-02 kiểm tra quyền truy cập dữ liệu bảo vệ.
end note

@enduml
```

**Hình 3 — State Machine: `ConsentGrant` Lifecycle (derived state)**

## 5. Business Rules Applied

- BR-PRIVACY — dữ liệu riêng tư (mẹ/bé/gia đình/sức khỏe) tách biệt khỏi danh tính cộng đồng công khai.
- BR-PRIVACY-02 — danh tính cộng đồng không mặc định để lộ hồ sơ sức khỏe riêng tư.
- BR-COMMUNITY-01 — display name/avatar công khai vẫn chịu kiểm duyệt.
- NS-02 — mọi truy cập dữ liệu bảo vệ phải kiểm tra recipient, purpose, scope, expiry và tình trạng thu hồi trước khi cho phép.
