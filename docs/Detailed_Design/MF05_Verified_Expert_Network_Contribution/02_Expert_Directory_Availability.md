# MF-05 / Spec 02 — Expert Directory and Availability

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-15 Manage Expert Availability; UC-43 Browse Expert Directory |
| Use Case Group | Shared / Common and Mobile App |
| Platform | Mother and Family Mobile; Expert Mobile; Expert Web; Backend |
| Primary Actors | Mother / Family / Verified Expert |
| In Scope | Availability does not guarantee response; labels depend on current trust |
| Explicitly Excluded | Nearby-expert support and gamification |
| Implementation Trace | UI: ExpertDirectoryScreen, ExpertPublicProfileScreen, AvailabilityCalendarPage; Controller: ExpertProfileController, ExpertAvailabilityController; Service: ExpertProfileServiceImpl, ExpertAvailabilityServiceImpl; Repository: ExpertProfileRepository, ExpertAvailabilityRepository; Entity: ExpertProfile, ExpertAvailability |

## 1. Tổng quan luồng chính (Main Flow Overview)

Availability does not guarantee response; labels depend on current trust. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF05_02_ExpertDirectoryAvailabilityandCommunityContribution_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "ExpertDirectoryScreen" as UI1 <<UI>>
class "ExpertPublicProfileScreen" as UI2 <<UI>>
class "AvailabilityCalendarPage" as UI3 <<UI>>
class "ExpertProfileController" as Controller1 <<Controller>> {
  - expertProfileService: IExpertProfileService
  - compreFacePipelineAdapter: CompreFacePipelineAdapter
  + setTrustStatus(expertProfileId: UUID, principal: Principal, status: TrustStatus): ResponseEntity<ApiResponse<Void>>
  + approveExpert(expertProfileId: UUID, principal: Principal): ResponseEntity<ApiResponse<Void>>
  + getMyVerificationStatus(principal: Principal): ResponseEntity<ApiResponse<VerificationStatusResponse>>
  + getVerifiedExperts(): ResponseEntity<ApiResponse<List<ExpertProfileResponse>>>
  + rejectExpert(expertProfileId: UUID, principal: Principal, request: RejectExpertRequest): ResponseEntity<ApiResponse<Void>>
  + createProfile(request: CreateExpertProfileRequest, principal: Principal): ResponseEntity<ApiResponse<ExpertProfileResponse>>
  + getMyProfile(principal: Principal): ResponseEntity<ApiResponse<ExpertProfileDetailResponse>>
}
class "ExpertAvailabilityController" as Controller2 <<Controller>> {
  - availabilityService: IExpertAvailabilityService
  - expertProfileRepository: ExpertProfileRepository
  + createAvailability(principal: Principal, request: CreateAvailabilityRequest): ResponseEntity<ApiResponse<AvailabilityResponse>>
  + deleteAvailability(principal: Principal, id: UUID): ResponseEntity<ApiResponse<Void>>
  + getMyAvailability(principal: Principal): ResponseEntity<ApiResponse<List<AvailabilityResponse>>>
  + setOnlineStatus(principal: Principal, request: SetOnlineStatusRequest): ResponseEntity<ApiResponse<LocationShareResponse>>
  - resolveExpertProfileId(userId: UUID): UUID
  + shareLocation(principal: Principal, request: ShareLocationRequest): ResponseEntity<ApiResponse<LocationShareResponse>>
  + stopLocationShare(principal: Principal): ResponseEntity<ApiResponse<Void>>
}
class "ExpertProfileServiceImpl" as Service1 <<Service>> {
  - expertProfileRepository: ExpertProfileRepository
  - userRepository: UserRepository
  - expertProfileMapper: ExpertProfileMapper
  - identityVerificationRepository: ExpertIdentityVerificationRepository
  + setTrustStatus(expertProfileId: UUID, newStatus: TrustStatus, adminId: UUID): void
  + approveExpert(expertProfileId: UUID, adminId: UUID): void
  + getMyVerificationStatus(userId: UUID): VerificationStatusResponse
  + getPublicDirectory(specialty: String, q: String, page: int, ...): ExpertDirectoryResponse
  + getVerifiedExperts(): List<ExpertProfileResponse>
}
class "ExpertAvailabilityServiceImpl" as Service2 <<Service>> {
  - availabilityRepository: ExpertAvailabilityRepository
  - locationShareRepository: ExpertLocationShareRepository
  - expertProfileRepository: ExpertProfileRepository
  - availabilityMapper: ExpertAvailabilityMapper
  + createAvailability(expertProfileId: UUID, request: CreateAvailabilityRequest): AvailabilityResponse
  + deleteAvailability(availabilityId: UUID, expertProfileId: UUID): void
  + getMyAvailability(expertProfileId: UUID): List<AvailabilityResponse>
  + setOnlineStatus(expertProfileId: UUID, online: Boolean): LocationShareResponse
  + shareLocation(expertProfileId: UUID, request: ShareLocationRequest): LocationShareResponse
}
interface "IExpertProfileService" as Service1Contract <<Service>>
interface "IExpertAvailabilityService" as Service2Contract <<Service>>
interface "ExpertProfileRepository" as Repository1 {
  + findByVerificationStatus(status: VerificationStatus): List<ExpertProfile>
  + findVerifiedPublic(): List<ExpertProfile>
  + findApprovedSpecialties(): List<String>
}
interface "ExpertAvailabilityRepository" as Repository2 {
  + findByExpertProfileId(expertProfileId: UUID): List<ExpertAvailability>
  + findTopByExpertProfileIdOrderByCreatedAtDesc(expertProfileId: UUID): Optional<ExpertAvailability>
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
class "ExpertAvailability" as Entity2 <<Entity>> {
  - availabilityId: UUID
  - expertProfileId: UUID
  - professionalProfileId: UUID
  - startAt: Instant
  - endAt: Instant
  - channelType: String
  - status: AvailabilityStatus
}
interface "JpaRepository<ExpertProfile, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<ExpertAvailability, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller2 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service2Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository2 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Repository2 ..> Entity2 : maps
Repository2 ..> DB : persists
Entity1 "1" o-- "0..*" Entity2 : availability slots
@enduml
```

**Figure 1 — Class Diagram: Expert Directory and Availability**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF05_02_ExpertDirectoryAvailabilityandCommunityContribution_SequenceDiagram
skinparam shadowing false

actor "Mother / Family / Verified Expert" as Actor
boundary ":AvailabilityCalendarPage" as UI1
boundary ":ExpertDirectoryScreen" as UI2
control ":ExpertAvailabilityController" as Controller1
control ":ExpertProfileController" as Controller2
participant ":ExpertAvailabilityServiceImpl" as Service1 <<service>>
participant ":ExpertProfileServiceImpl" as Service2 <<service>>
participant ":ExpertAvailabilityRepository" as Repository1 <<repository>>
participant ":ExpertProfileRepository" as Repository2 <<repository>>
database "PostgreSQL" as DB

group UC-15 Manage Expert Availability
  Actor -> UI1 : 1. startManageExpertAvailability()
  activate UI1
  UI1 -> Controller1 : 2. getMyAvailability() / createAvailability() / deleteAvailability()
  activate Controller1
  Controller1 -> Service1 : 3. getMyAvailability() / createAvailability() / deleteAvailability()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByExpertProfileId()
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
    Service1 -> Repository1 : 4b. findByExpertProfileId()
    activate Repository1
    Repository1 -> DB : 4b-1. SELECT
    activate DB
    DB --> Repository1 : 4b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 4b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 4b-4. save() / delete()
    activate Repository1
    Repository1 -> DB : 4b-5. INSERT / UPDATE / DELETE
    activate DB
    DB --> Repository1 : 4b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 4b-7. persistedEntity
    deactivate Repository1
    Service1 --> Controller1 : 4b-8. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-9. 200 OK / 201 Created
    deactivate Controller1
    UI1 --> Actor : 4b-10. displayConfirmedState()
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

group UC-43 Browse Expert Directory
  Actor -> UI2 : 5. startBrowseExpertDirectory()
  activate UI2
  UI2 -> Controller2 : 6. getVerifiedExperts() / getPublicProfile()
  activate Controller2
  Controller2 -> Service2 : 7. getVerifiedExperts() / getPublicProfile()
  activate Service2
  alt [request is authorized and input is valid]
    Service2 -> Repository2 : 8a. findByVerificationStatus()
    activate Repository2
    Repository2 -> DB : 8a-1. SELECT
    activate DB
    DB --> Repository2 : 8a-2. queryResult
    deactivate DB
    Repository2 --> Service2 : 8a-3. domainRecords
    deactivate Repository2
    Service2 --> Controller2 : 8a-4. resultDTO
    deactivate Service2
    Controller2 --> UI2 : 8a-5. 200 OK
    deactivate Controller2
    UI2 --> Actor : 8a-6. displayBrowseExpertDirectoryResult()
    deactivate UI2
  else [request is invalid, forbidden or unavailable]
    Service2 --> Controller2 : 8b. domainError
    deactivate Service2
    Controller2 --> UI2 : 8b-1. 400 / 401 / 403 / 404
    deactivate Controller2
    UI2 --> Actor : 8b-2. displayActionableError()
    deactivate UI2
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Expert Directory and Availability Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-15 Manage Expert Availability; UC-43 Browse Expert Directory.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Availability does not guarantee response; labels depend on current trust.
- The following remains outside this contract: Nearby-expert support and gamification.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
