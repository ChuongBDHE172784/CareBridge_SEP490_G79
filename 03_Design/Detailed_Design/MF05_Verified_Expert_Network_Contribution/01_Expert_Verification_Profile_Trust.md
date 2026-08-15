# MF-05 / Spec 01 — Expert Application, Professional Profile and Trust Review

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-13 Register and Submit Expert Application; UC-14 Manage Expert Professional Profile; UC-82 Review Expert Applications and Trust |
| Use Case Group | Shared / Common and Web App |
| Platform | Expert Mobile; Expert Web; Admin Web; Backend |
| Primary Actors | Expert Applicant / Expert / System Admin |
| In Scope | Only approved active profiles may perform verified contribution actions |
| Explicitly Excluded | Badges, points, leaderboard, pricing and ratings |
| Implementation Trace | UI: ExpertOnboardingPage, ExpertProfilePage, VerificationDocumentsPage, ExpertVerificationQueuePage; Controller: ExpertProfileController; Service: ExpertProfileServiceImpl; Repository: ExpertProfileRepository, ExpertCredentialRepository; Entity: ExpertProfile, ExpertCredential |

## 1. Tổng quan luồng chính (Main Flow Overview)

Only approved active profiles may perform verified contribution actions. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF05_01_ExpertApplicationProfessionalProfileandTrustReview_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "ExpertOnboardingPage" as UI1 <<UI>>
class "ExpertProfilePage" as UI2 <<UI>>
class "VerificationDocumentsPage" as UI3 <<UI>>
class "ExpertVerificationQueuePage" as UI4 <<UI>>
class "ExpertProfileController" as Controller1 <<Controller>> {
  - expertProfileService: IExpertProfileService
  - compreFacePipelineAdapter: CompreFacePipelineAdapter
  + setTrustStatus(expertProfileId: UUID, principal: Principal, status: TrustStatus): ResponseEntity<ApiResponse<Void>>
  + approveExpert(expertProfileId: UUID, principal: Principal): ResponseEntity<ApiResponse<Void>>
  + createProfile(request: CreateExpertProfileRequest, principal: Principal): ResponseEntity<ApiResponse<ExpertProfileResponse>>
  + getMyProfile(principal: Principal): ResponseEntity<ApiResponse<ExpertProfileDetailResponse>>
  + getMyVerificationStatus(principal: Principal): ResponseEntity<ApiResponse<VerificationStatusResponse>>
  + getPublicProfile(expertProfileId: UUID): ResponseEntity<ApiResponse<ExpertProfileDetailResponse>>
  + getVerifiedExperts(): ResponseEntity<ApiResponse<List<ExpertProfileResponse>>>
}
class "ExpertProfileServiceImpl" as Service1 <<Service>> {
  - expertProfileRepository: ExpertProfileRepository
  - userRepository: UserRepository
  - expertProfileMapper: ExpertProfileMapper
  - identityVerificationRepository: ExpertIdentityVerificationRepository
  + setTrustStatus(expertProfileId: UUID, newStatus: TrustStatus, adminId: UUID): void
  + approveExpert(expertProfileId: UUID, adminId: UUID): void
  + createProfile(userId: UUID, request: CreateExpertProfileRequest): ExpertProfileResponse
  + getAllAdminExperts(status: String, keyword: String): List<ExpertProfileResponse>
  + getMyProfile(userId: UUID): ExpertProfileDetailResponse
}
interface "IExpertProfileService" as Service1Contract <<Service>>
interface "ExpertProfileRepository" as Repository1 {
  + findByVerificationStatus(status: VerificationStatus): List<ExpertProfile>
  + findVerifiedPublic(): List<ExpertProfile>
  + findApprovedSpecialties(): List<String>
}
interface "ExpertCredentialRepository" as Repository2 {
  + findByExpertProfileId(expertProfileId: UUID): List<ExpertCredential>
  + findByExpertProfileIdAndCredentialNumber(expertProfileId: UUID, credentialNumber: String): List<ExpertCredential>
  + findByExpertProfileIdAndCredentialTypeOrderByCreatedAtDesc(expertProfileId: UUID, credentialType: String): List<ExpertCredential>
  + findByCredentialTypeAndReviewStatusOrderByCreatedAtAsc(credentialType: String, reviewStatus: ReviewStatus): List<ExpertCredential>
  + findByExpertProfileIdAndReviewStatus(expertProfileId: UUID, reviewStatus: ReviewStatus): List<ExpertCredential>
  + findFirstByExpertProfileIdAndReviewStatusOrderByReviewedAtDescCreatedAtDesc(expertProfileId: UUID, reviewStatus: ReviewStatus): Optional<ExpertCredential>
}
class "ExpertProfile" as Entity1 <<Entity>> {
  - expertProfileId: UUID
  - user: com.carebridge.backend.security.entity.User
  - specialty: String
  - professionalTitle: String
  - experienceYears: Integer
  - workplace: String
  - facilityId: UUID
}
class "ExpertCredential" as Entity2 <<Entity>> {
  - credentialId: UUID
  - expertProfileId: UUID
  - credentialType: String
  - credentialNumber: String
  - issuer: String
  - issuedDate: LocalDate
  - expiryDate: LocalDate
}
interface "JpaRepository<ExpertProfile, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<ExpertCredential, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "File storage and face-verification service" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller1 : invokes API
UI4 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Repository2 ..> Entity2 : maps
Repository2 ..> DB : persists
Service1 ..> External : invokes when required
Entity1 "1" *-- "0..*" Entity2 : credentials
@enduml
```

**Figure 1 — Class Diagram: Expert Application, Professional Profile and Trust Review**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF05_01_ExpertApplicationProfessionalProfileandTrustReview_SequenceDiagram
skinparam shadowing false

actor "Expert Applicant / Expert / System Admin" as Actor
boundary ":ExpertOnboardingPage" as UI1
boundary ":ExpertProfilePage" as UI2
boundary ":ExpertVerificationQueuePage" as UI3
control ":ExpertProfileController" as Controller1
participant ":ExpertProfileServiceImpl" as Service1 <<service>>
participant ":ExpertProfileRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":File storage and face-verification service" as External1 <<external system>>
participant ":File storage" as External2 <<external system>>
participant ":Face-verification service" as External3 <<external system>>

group UC-13 Register and Submit Expert Application
  Actor -> UI1 : 1. startRegisterAndSubmitExpertApplication()
  activate UI1
  UI1 -> Controller1 : 2. createProfile() / updateProfile() / verifyFace()
  activate Controller1
  Controller1 -> Service1 : 3. createProfile() / updateProfile()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByUserId()
    activate Repository1
    Repository1 -> DB : 4a-1. SELECT
    activate DB
    DB --> Repository1 : 4a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 4a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 4a-4. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4a-5. 200 OK
    deactivate Controller1
    UI1 --> Actor : 4a-6. displayCurrentState()
    deactivate UI1
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository1 : 4b. findByUserId()
    activate Repository1
    Repository1 -> DB : 4b-1. SELECT
    activate DB
    DB --> Repository1 : 4b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 4b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 4b-4. save()
    activate Repository1
    Repository1 -> DB : 4b-5. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 4b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 4b-7. persistedEntity
    deactivate Repository1
    Service1 -> External1 : 4b-8. uploadAndVerifyDocuments()
    activate External1
    External1 --> Service1 : 4b-9. integrationResult
    deactivate External1
    Service1 --> Controller1 : 4b-10. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-11. 200 OK / 201 Created
    deactivate Controller1
    UI1 --> Actor : 4b-12. displayConfirmedState()
    deactivate UI1
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 4c. domainError
    deactivate Service1
    Controller1 --> UI1 : 4c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI1 --> Actor : 4c-2. displayActionableError()
    deactivate UI1
  end
end

group UC-14 Manage Expert Professional Profile
  Actor -> UI2 : 5. startManageExpertProfessionalProfile()
  activate UI2
  UI2 -> Controller1 : 6. getMyProfile() / updateProfile()
  activate Controller1
  Controller1 -> Service1 : 7. getMyProfile() / updateProfile()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 8a. findByUserId()
    activate Repository1
    Repository1 -> DB : 8a-1. SELECT
    activate DB
    DB --> Repository1 : 8a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 8a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 8a-4. resultDTO
    deactivate Service1
    Controller1 --> UI2 : 8a-5. 200 OK
    deactivate Controller1
    UI2 --> Actor : 8a-6. displayCurrentState()
    deactivate UI2
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository1 : 8b. findByUserId()
    activate Repository1
    Repository1 -> DB : 8b-1. SELECT
    activate DB
    DB --> Repository1 : 8b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 8b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 8b-4. save()
    activate Repository1
    Repository1 -> DB : 8b-5. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 8b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 8b-7. persistedEntity
    deactivate Repository1
    Service1 -> External2 : 8b-8. uploadCredential()
    activate External2
    External2 --> Service1 : 8b-9. integrationResult
    deactivate External2
    Service1 --> Controller1 : 8b-10. resultDTO
    deactivate Service1
    Controller1 --> UI2 : 8b-11. 200 OK / 201 Created
    deactivate Controller1
    UI2 --> Actor : 8b-12. displayConfirmedState()
    deactivate UI2
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 8c. domainError
    deactivate Service1
    Controller1 --> UI2 : 8c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI2 --> Actor : 8c-2. displayActionableError()
    deactivate UI2
  end
end

group UC-82 Review Expert Applications and Trust
  Actor -> UI3 : 9. startReviewExpertApplicationsAndTrust()
  activate UI3
  UI3 -> Controller1 : 10. getAdminProfiles() / approveExpert() / rejectExpert()
  activate Controller1
  Controller1 -> Service1 : 11. getAllAdminExperts() / approveExpert() / rejectExpert()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 12a. findByVerificationStatus()
    activate Repository1
    Repository1 -> DB : 12a-1. SELECT
    activate DB
    DB --> Repository1 : 12a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 12a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 12a-4. resultDTO
    deactivate Service1
    Controller1 --> UI3 : 12a-5. 200 OK
    deactivate Controller1
    UI3 --> Actor : 12a-6. displayCurrentState()
    deactivate UI3
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository1 : 12b. findByVerificationStatus()
    activate Repository1
    Repository1 -> DB : 12b-1. SELECT
    activate DB
    DB --> Repository1 : 12b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 12b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 12b-4. save()
    activate Repository1
    Repository1 -> DB : 12b-5. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 12b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 12b-7. persistedEntity
    deactivate Repository1
    Service1 -> External3 : 12b-8. verifyIdentityEvidence()
    activate External3
    External3 --> Service1 : 12b-9. integrationResult
    deactivate External3
    Service1 --> Controller1 : 12b-10. resultDTO
    deactivate Service1
    Controller1 --> UI3 : 12b-11. 200 OK / 201 Created
    deactivate Controller1
    UI3 --> Actor : 12b-12. displayConfirmedState()
    deactivate UI3
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 12c. domainError
    deactivate Service1
    Controller1 --> UI3 : 12c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI3 --> Actor : 12c-2. displayActionableError()
    deactivate UI3
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Expert Application, Professional Profile and Trust Review Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-13 Register and Submit Expert Application; UC-14 Manage Expert Professional Profile; UC-82 Review Expert Applications and Trust.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Only approved active profiles may perform verified contribution actions.
- The following remains outside this contract: Badges, points, leaderboard, pricing and ratings.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
