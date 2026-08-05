# MF-05 / Spec 01 — Expert Verification & Trust Lifecycle

| Field | Value |
| --- | --- |
| Feature | MF-05 — Verified Expert Network & Contribution |
| Use Cases Covered | UC-60 Submit Expert Profile, UC-62 Submit or Replace Verification Documents, UC-63 View Verification Status and Renew Submission, UC-70 Review Expert Verification Submission, UC-71 Restrict, Suspend or Reinstate Expert Trust Status |
| Primary Actor(s) | Expert Applicant / Verified Expert, System Admin |
| Platform | Expert Portal / Expert App, Admin Portal |
| Main Flow Summary | An Expert Applicant submits a professional profile and supporting credentials; a System Admin reviews and approves or rejects the submission; once approved the expert is trusted (`TrustStatus=ACTIVE`) until credentials expire, are renewed, or the admin restricts/suspends/reinstates trust for policy reasons. |
| Grounding (source code) | `expert/entity/ExpertProfile.java`, `expert/verificationstatus/VerificationStatus.java`, `expert/truststatus/TrustStatus.java`, `expertverification/entity/ExpertCredential.java`, `expertverification/reviewstatus/ReviewStatus.java`, `expert/controller/ExpertProfileController.java` (`/api/v1/expert/profiles/*`), `expertverification/controller/ExpertCredentialController.java` (`/api/v1/expert/credentials`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là control "trust" trung tâm của MF-05 và là điều kiện tiên quyết để một expert
xuất hiện trong directory (spec 02), được gắn `expertLabeled` trong cộng đồng (MF-04),
hoặc tham gia nearby support (MF-07). `ExpertProfile.verificationStatus` bắt đầu ở
`PENDING` khi nộp hồ sơ (UC-60), kèm một hoặc nhiều `ExpertCredential` minh chứng
(UC-62, mỗi credential có `reviewStatus` riêng). System Admin rà soát và chuyển
`UNDER_REVIEW → APPROVED/REJECTED` (UC-70). Sau khi `APPROVED`, `trustStatus` mặc định
`ACTIVE`; System Admin có thể hạ xuống `SUSPENDED`/`REVOKED` khi vi phạm chính sách hoặc
tín chỉ hết hạn, và khôi phục khi hợp lệ trở lại (UC-71). Khi `verificationStatus`
chuyển `EXPIRED`, expert nộp lại để gia hạn (UC-63) — quay lại đúng chu trình rà soát.

## 2. Class Diagram

```plantuml
@startuml MF05_01_ExpertVerification_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class ExpertProfile {
  + expertProfileId: UUID
  + userId: UUID
  + specialty: String
  + professionalTitle: String
  + experienceYears: Integer
  + workplace: String
  + serviceScope: String
  + verificationStatus: VerificationStatus
  + trustStatus: TrustStatus
  + verifiedAt: LocalDateTime
  + verifiedBy: UUID
}

enum VerificationStatus {
  PENDING
  UNDER_REVIEW
  APPROVED
  REJECTED
  SUSPENDED
  EXPIRED
}

enum TrustStatus {
  ACTIVE
  SUSPENDED
  REVOKED
}

class ExpertCredential {
  + credentialId: UUID
  + expertProfileId: UUID
  + credentialType: String
  + credentialNumber: String
  + issuer: String
  + issuedDate: LocalDate
  + expiryDate: LocalDate
  + fileUrl: String
  + reviewStatus: ReviewStatus
  + reviewNote: String
  + reviewedBy: UUID
}

enum ReviewStatus {
  PENDING
  APPROVED
  REJECTED
  EXPIRED
}

class ExpertProfileController {
  - expertProfileService: ExpertProfileService
  + submit(request): ResponseEntity
  + renew(): ResponseEntity
  + verificationStatus(): ResponseEntity
  + approve(expertProfileId, request): ResponseEntity
  + reject(expertProfileId, request): ResponseEntity
  + updateTrust(expertProfileId, request): ResponseEntity
}

class ExpertCredentialController {
  - expertCredentialService: ExpertCredentialService
  + upload(multipart): ResponseEntity
  + review(credentialId, ReviewCredentialRequest): ResponseEntity
  + pending(): ResponseEntity
}

interface ExpertProfileService <<interface>> {
  + submit(userId: UUID, request): ExpertProfile
  + approve(adminId: UUID, expertProfileId: UUID): ExpertProfile
  + reject(adminId: UUID, expertProfileId: UUID, reason): ExpertProfile
  + updateTrust(adminId: UUID, expertProfileId: UUID, trustStatus): ExpertProfile
}

class ExpertProfileServiceImpl implements ExpertProfileService {
  - expertProfileRepository: ExpertProfileRepository
  - expertCredentialRepository: ExpertCredentialRepository
  - auditService: AuditService
}

ExpertProfile --> VerificationStatus
ExpertProfile --> TrustStatus
ExpertProfile "1" *-- "0..*" ExpertCredential : supported by
ExpertCredential --> ReviewStatus
ExpertProfileController --> ExpertProfileService : uses
ExpertCredentialController --> ExpertCredentialService : uses
ExpertProfileServiceImpl --> AuditService : emits EXPERT_VERIFICATION

@enduml
```

**Hình 1 — Class Diagram: Expert Profile, Credential & Verification/Trust Status**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF05_01_ExpertVerification_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Expert Applicant" as EA
actor "System Admin" as Admin
participant "CareBridge Expert Network UI" as UI
participant "ExpertProfileController" as ProfileController
participant "ExpertCredentialController" as CredController
participant "ExpertProfileServiceImpl" as Service
participant "ExpertCredentialServiceImpl" as CredService
participant "ExpertProfileRepository" as ProfileRepo
participant "ExpertCredentialRepository" as CredRepo
database "PostgreSQL" as DB

== UC-60 Submit Expert Profile ==
EA -> UI : 1. Submit expert profile
activate UI
UI -> ProfileController : 1a. POST /api/v1/expert/profiles\n{specialty, professionalTitle, serviceScope}
activate ProfileController
ProfileController -> Service : 2. submit(userId, request)
activate Service
Service -> ProfileRepo : 3. save(ExpertProfile{verificationStatus=PENDING, trustStatus=ACTIVE})
activate ProfileRepo
ProfileRepo -> DB : 4. INSERT INTO expert_profiles ...
activate DB
DB --> ProfileRepo : 5. saved
deactivate DB
ProfileRepo --> Service : 6. ExpertProfile
deactivate ProfileRepo
Service --> ProfileController : 7. ExpertProfile
deactivate Service
ProfileController --> UI : 8. HTTP 201 Created
deactivate ProfileController
UI --> EA : 8a. Display profile submission status
deactivate UI

== UC-62 Submit Verification Documents ==
EA -> UI : 9. Upload verification document
activate UI
UI -> CredController : 9a. POST /api/v1/expert/credentials (multipart)\n{credentialType, credentialNumber, file}
activate CredController
CredController -> CredService : 10. upload(userId, request)
activate CredService
CredService -> CredRepo : 11. save(ExpertCredential{reviewStatus=PENDING})
activate CredRepo
CredRepo -> DB : 12. INSERT INTO expert_credentials ...
activate DB
DB --> CredRepo : 13. saved
deactivate DB
CredRepo --> CredService : 14. ExpertCredential
deactivate CredRepo
CredService --> CredController : 15. ExpertCredential
deactivate CredService
CredController --> UI : 16. HTTP 201 Created
deactivate CredController
UI --> EA : 16a. Display credential submission status
deactivate UI

== UC-70 Review Expert Verification Submission ==
Admin -> UI : 17. Open pending credential queue
activate UI
UI -> CredController : 17a. GET /api/v1/expert/credentials/pending
activate CredController
CredController -> CredService : 18. pending()
activate CredService
CredService -> CredRepo : 19. findByReviewStatus(PENDING)
activate CredRepo
CredRepo -> DB : 20. SELECT * FROM expert_credentials WHERE review_status='PENDING'
activate DB
DB --> CredRepo : 21. rows[]
deactivate DB
CredRepo --> CredService : 22. credentials[]
deactivate CredRepo
CredService --> CredController : 23. credentials[]
deactivate CredService
CredController --> UI : 24. HTTP 200 OK {credentials[]}
deactivate CredController
UI --> Admin : 24a. Display pending credentials
deactivate UI

Admin -> UI : 25. Approve credential
activate UI
UI -> CredController : 25a. PUT /api/v1/expert/credentials/{credentialId}/review\n{reviewStatus=APPROVED}
activate CredController
CredController -> CredService : 26. review(adminId, credentialId, APPROVED)
activate CredService
CredService -> CredRepo : 27. save(credential{reviewStatus=APPROVED, reviewedBy=adminId})
activate CredRepo
CredRepo -> DB : 28. UPDATE expert_credentials SET review_status='APPROVED'
activate DB
DB --> CredRepo : 29. updated
deactivate DB
CredRepo --> CredService : 30. void
deactivate CredRepo
CredService --> CredController : 31. void
deactivate CredService
CredController --> UI : 32. HTTP 200 OK
deactivate CredController
UI --> Admin : 32a. Display credential review result
deactivate UI

Admin -> UI : 33. Approve expert profile
activate UI
UI -> ProfileController : 33a. POST /api/v1/expert/profiles/{id}/approve
activate ProfileController
ProfileController -> Service : 34. approve(adminId, expertProfileId)
activate Service
Service -> CredRepo : 35. findByExpertProfileId(expertProfileId)
activate CredRepo
CredRepo -> DB : 36. SELECT * FROM expert_credentials WHERE expert_profile_id=?
activate DB
DB --> CredRepo : 37. credentials[]
deactivate DB
CredRepo --> Service : 38. credentials[]
deactivate CredRepo
Service -> Service : 34a. require profile/identity/credential review preconditions
activate Service
Service --> Service : 34b. preconditions satisfied
deactivate Service
Service -> ProfileRepo : 39. save(profile{verificationStatus=APPROVED, verifiedAt=now(), verifiedBy=adminId})
activate ProfileRepo
ProfileRepo -> DB : 40. UPDATE expert_profiles\nSET verification_status='APPROVED', verified_at=now(), verified_by=adminId
activate DB
DB --> ProfileRepo : 41. updated
deactivate DB
ProfileRepo --> Service : 42. ExpertProfile
deactivate ProfileRepo
Service --> ProfileController : 43. ExpertProfile{verificationStatus=APPROVED}
deactivate Service
ProfileController --> UI : 44. HTTP 200 OK
deactivate ProfileController
UI --> Admin : 44a. Display profile approval result
deactivate UI

== UC-71 Restrict, Suspend or Reinstate Expert Trust Status ==
Admin -> UI : 45. Change expert trust status
activate UI
UI -> ProfileController : 45a. PATCH /api/v1/expert/profiles/{id}/trust\n{trustStatus=SUSPENDED, reason}
activate ProfileController
ProfileController -> Service : 46. updateTrust(adminId, expertProfileId, SUSPENDED)
activate Service
Service -> ProfileRepo : 47. save(profile{trustStatus=SUSPENDED})
activate ProfileRepo
ProfileRepo -> DB : 48. UPDATE expert_profiles SET trust_status='SUSPENDED'
activate DB
DB --> ProfileRepo : 49. updated
deactivate DB
ProfileRepo --> Service : 50. ExpertProfile
deactivate ProfileRepo
Service --> ProfileController : 51. ExpertProfile{trustStatus=SUSPENDED}
deactivate Service
ProfileController --> UI : 52. HTTP 200 OK
deactivate ProfileController
UI --> Admin : 52a. Display updated trust status
deactivate UI

== UC-63 View Verification Status and Renew Submission ==
EA -> UI : 53. View verification status
activate UI
UI -> ProfileController : 53a. GET /api/v1/expert/profiles/me/verification-status
activate ProfileController
ProfileController -> Service : 54. verificationStatus(userId)
activate Service
Service -> ProfileRepo : 55. findByUserId(userId)
activate ProfileRepo
ProfileRepo -> DB : 56. SELECT * FROM expert_profiles WHERE user_id=?
activate DB
DB --> ProfileRepo : 57. profile
deactivate DB
ProfileRepo --> Service : 58. profile
deactivate ProfileRepo
Service --> ProfileController : 59. {verificationStatus, trustStatus, expiredCredentials[]}
deactivate Service
ProfileController --> UI : 60. HTTP 200 OK {verificationStatus, trustStatus, expiredCredentials[]}
deactivate ProfileController
UI --> EA : 60a. Display verification and credential status
deactivate UI

EA -> UI : 61. Submit expert profile renewal
activate UI
UI -> ProfileController : 61a. POST /api/v1/expert/profiles/me/renew
activate ProfileController
ProfileController -> Service : 62. submit(userId, renewalRequest)
activate Service
Service -> ProfileRepo : 63. save(profile{verificationStatus=PENDING})
activate ProfileRepo
ProfileRepo -> DB : 64. UPDATE expert_profiles SET verification_status='PENDING'
activate DB
DB --> ProfileRepo : 65. updated
deactivate DB
ProfileRepo --> Service : 66. void
deactivate ProfileRepo
Service --> ProfileController : 67. void
deactivate Service
ProfileController --> UI : 68. HTTP 200 OK
deactivate ProfileController
UI --> EA : 68a. Display renewal submission status
deactivate UI

@enduml
```

> Ghi chú grounding: rà soát `ExpertProfileServiceImpl` và `ExpertCredentialServiceImpl`
> không tìm thấy lệnh gọi `auditService.log(...)` nào — `AuditAction.EXPERT_VERIFICATION`
> hiện chỉ xuất hiện trong danh sách hành động đủ điều kiện audit
> (`AuditEligibilityPolicy`), chưa được service nào thực sự phát ra. Vì vậy sequence diagram
> này **không vẽ** lệnh gọi `AuditService` để không suy diễn hành vi chưa có thật trong code.

**Hình 2 — Sequence Diagram: Submit Profile → Submit Credentials → Admin Review → Trust Management → Renew (Main Flow)**


## 4. Business Rules Applied

- BR-RBAC — chỉ System Admin mới duyệt/từ chối hồ sơ và thay đổi trust status.
- UC-70 — quyết định duyệt phải dựa trên rà soát credential (`ExpertCredential.reviewStatus`), không được duyệt hồ sơ khi credential bắt buộc còn `PENDING`/`REJECTED`.
- UC-71 — chỉ System Admin thay đổi `trustStatus`; mọi thay đổi phải kèm lý do và được audit.
- Directory visibility (spec 02) chỉ hiển thị expert có `verificationStatus=APPROVED` **và** `trustStatus=ACTIVE` đồng thời.
