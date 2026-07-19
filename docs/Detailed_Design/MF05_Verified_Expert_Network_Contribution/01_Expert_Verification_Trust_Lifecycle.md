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
  + consultationScope: String
  + verificationStatus: VerificationStatus
  + trustStatus: TrustStatus
  + verifiedAt: LocalDateTime
  + verifiedBy: UUID
  + ratingAvg: BigDecimal
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
participant "ExpertProfileController" as ProfileController
participant "ExpertCredentialController" as CredController
actor "System Admin" as Admin
participant "ExpertProfileServiceImpl" as Service
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-60 Submit Expert Profile ==
EA -> ProfileController : POST /api/v1/expert/profiles\n{specialty, professionalTitle, consultationScope}
ProfileController -> Service : submit(userId, request)
Service -> DB : INSERT INTO expert_profiles\n(verificationStatus=PENDING, trustStatus=ACTIVE)
Service -> Audit : emit(EXPERT_VERIFICATION, "submitted")
Service --> ProfileController : ExpertProfile
ProfileController --> EA : HTTP 201 Created

== UC-62 Submit Verification Documents ==
EA -> CredController : POST /api/v1/expert/credentials (multipart)\n{credentialType, credentialNumber, file}
CredController -> DB : INSERT INTO expert_credentials (reviewStatus=PENDING)
CredController --> EA : HTTP 201 Created

== UC-70 Review Expert Verification Submission ==
Admin -> ProfileController : GET /api/v1/expert/credentials/pending
Admin -> CredController : PUT /api/v1/expert/credentials/{credentialId}/review\n{reviewStatus=APPROVED}
CredController -> DB : UPDATE expert_credentials SET review_status='APPROVED'

Admin -> ProfileController : POST /api/v1/expert/profiles/{id}/approve
ProfileController -> Service : approve(adminId, expertProfileId)
Service -> Service : require tất cả credential bắt buộc đã APPROVED
Service -> DB : UPDATE expert_profiles\nSET verificationStatus='APPROVED', verified_at=now(), verified_by=adminId
Service -> Audit : emit(EXPERT_VERIFICATION, "approved")
Service --> ProfileController : ExpertProfile{verificationStatus=APPROVED}
ProfileController --> Admin : HTTP 200 OK

== UC-71 Restrict, Suspend or Reinstate Expert Trust Status ==
Admin -> ProfileController : PATCH /api/v1/expert/profiles/{id}/trust\n{trustStatus=SUSPENDED, reason}
ProfileController -> Service : updateTrust(adminId, expertProfileId, SUSPENDED)
Service -> DB : UPDATE expert_profiles SET trust_status='SUSPENDED'
Service -> Audit : emit(EXPERT_VERIFICATION, "trust_changed")
Service --> ProfileController : ExpertProfile{trustStatus=SUSPENDED}
ProfileController --> Admin : HTTP 200 OK

== UC-63 View Verification Status and Renew Submission ==
EA -> ProfileController : GET /api/v1/expert/profiles/me/verification-status
ProfileController --> EA : HTTP 200 OK {verificationStatus, trustStatus, expiredCredentials[]}
EA -> ProfileController : POST /api/v1/expert/profiles/me/renew
ProfileController -> Service : submit(userId, renewalRequest)
Service -> DB : UPDATE expert_profiles SET verificationStatus='PENDING'
ProfileController --> EA : HTTP 200 OK

@enduml
```

**Hình 2 — Sequence Diagram: Submit Profile → Submit Credentials → Admin Review → Trust Management → Renew (Main Flow)**

## 4. State Machine — `ExpertProfile.verificationStatus` & `trustStatus`

```plantuml
@startuml MF05_01_ExpertVerification_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

state "verificationStatus" as VS {
  [*] --> PENDING : Nộp hồ sơ (UC-60)
  PENDING --> UNDER_REVIEW : Admin bắt đầu rà soát (UC-70)
  UNDER_REVIEW --> APPROVED : Admin duyệt (UC-70)
  UNDER_REVIEW --> REJECTED : Admin từ chối, có lý do (UC-70)
  APPROVED --> EXPIRED : Credential/verification hết hạn
  REJECTED --> PENDING : Expert nộp lại sau khi bổ sung
  EXPIRED --> PENDING : Expert nộp gia hạn (UC-63)
  APPROVED --> SUSPENDED : Admin tạm ngưng do vi phạm (UC-71)
  SUSPENDED --> APPROVED : Admin khôi phục (UC-71)
}

state "trustStatus" as TS {
  [*] --> ACTIVE : Mặc định khi APPROVED lần đầu
  ACTIVE --> SUSPENDED : Admin đình chỉ (UC-71)
  SUSPENDED --> ACTIVE : Admin khôi phục (UC-71)
  ACTIVE --> REVOKED : Admin thu hồi vĩnh viễn (UC-71)
  SUSPENDED --> REVOKED : Admin thu hồi sau đình chỉ (UC-71)
}

note bottom of TS
  trustStatus quyết định expert có được hiển thị trong directory
  (spec 02) và được gắn expertLabeled trong MF-04 hay không,
  độc lập với verificationStatus (một expert APPROVED vẫn có
  thể bị SUSPENDED tạm thời).
end note

@enduml
```

**Hình 3 — State Machine: `ExpertProfile.verificationStatus` (6 trạng thái) & `trustStatus` (3 trạng thái)**

## 5. Business Rules Applied

- BR-RBAC — chỉ System Admin mới duyệt/từ chối hồ sơ và thay đổi trust status.
- UC-70 — quyết định duyệt phải dựa trên rà soát credential (`ExpertCredential.reviewStatus`), không được duyệt hồ sơ khi credential bắt buộc còn `PENDING`/`REJECTED`.
- UC-71 — chỉ System Admin thay đổi `trustStatus`; mọi thay đổi phải kèm lý do và được audit.
- Directory visibility (spec 02) chỉ hiển thị expert có `verificationStatus=APPROVED` **và** `trustStatus=ACTIVE` đồng thời.
