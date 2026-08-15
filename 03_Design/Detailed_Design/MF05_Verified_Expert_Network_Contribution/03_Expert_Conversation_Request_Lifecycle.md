# MF-05 / Spec 03 — Free Expert Conversation Request Lifecycle

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-16 Process Expert Conversation Requests; UC-44 Manage Own Expert Conversation Requests |
| Use Case Group | Shared / Common and Mobile App |
| Platform | Mother and Family Mobile; Expert Mobile; Expert Web; Backend |
| Primary Actors | Mother / Family / Expert |
| In Scope | Accept locks an eligible expert and atomically creates or links the direct conversation |
| Explicitly Excluded | Booking, pricing, payment, commission and refund |
| Known Implementation Gap | Mobile accept/reject follows the Backend contract. Expert Web accept is aligned, but its reject call currently uses POST while the Backend requires PATCH; the Web reject branch must not be documented as operational success. |
| Implementation Trace | UI: ExpertPublicProfileScreen, consultation request screens, ExpertConsultationRequestsPage; Controller: ConsultationRequestController; Service: ConsultationRequestServiceImpl, ConsultationRequestPolicy; Repository: ConsultationRequestRepository; Entity: ConsultationRequest |

## 1. Tổng quan luồng chính (Main Flow Overview)

Accept locks an eligible expert and atomically creates or links the direct conversation. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF05_03_FreeExpertConversationRequestLifecycle_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "ExpertPublicProfileScreen" as UI1 <<UI>>
class "consultation request screens" as UI2 <<UI>>
class "ExpertConsultationRequestsPage" as UI3 <<UI>>
class "ConsultationRequestController" as Controller1 <<Controller>> {
  - service: IConsultationRequestService
  + accept(id: UUID, principal: Principal): ResponseEntity<ApiResponse<ConsultationRequestResponse>>
  + reject(id: UUID, request: RejectConsultationRequestRequest, principal: Principal): ResponseEntity<ApiResponse<ConsultationRequestResponse>>
  + cancel(id: UUID, principal: Principal): ResponseEntity<ApiResponse<ConsultationRequestResponse>>
  + create(request: CreateConsultationRequestRequest, principal: Principal): ResponseEntity<ApiResponse<ConsultationRequestResponse>>
  + getById(id: UUID, principal: Principal): ResponseEntity<ApiResponse<ConsultationRequestResponse>>
  + pendingSummary(principal: Principal): ResponseEntity<ApiResponse<ConsultationRequestPendingSummaryResponse>>
}
class "ConsultationRequestServiceImpl" as Service1 <<Service>> {
  - repository: ConsultationRequestRepository
  - writer: ConsultationRequestWriter
  - expertProfileRepository: ExpertProfileRepository
  - userRepository: UserRepository
  + accept(id: UUID, expertUserId: UUID): ConsultationRequestResponse
  + expireOverdueRequests(): int
  + reject(id: UUID, expertUserId: UUID, reason: String): ConsultationRequestResponse
  - assignedExpertUserId(request: ConsultationRequest): UUID
  - findRequest(id: UUID): ConsultationRequest
}
class "ConsultationRequestPolicy" as Service2 <<Service>> {
  + assertExpertEligibleForConsultation(expertProfile: ExpertProfile): void
  + assertExpertStillEligibleForConsultation(expertProfile: ExpertProfile, expertAccount: User, now: Instant): void
  + assertCanCancel(request: ConsultationRequest, requesterUserId: UUID): void
  + assertCanRespond(request: ConsultationRequest, expertUserId: UUID, assignedExpertUserId: UUID): void
  + assertCanView(request: ConsultationRequest, currentUserId: UUID, assignedExpertUserId: UUID): void
}
interface "IConsultationRequestService" as Service1Contract <<Service>>
interface "ConsultationRequestRepository" as Repository1 {
  + findByRequesterUserIdAndClientRequestId(requesterUserId: UUID, clientRequestId: UUID): Optional<ConsultationRequest>
  + findByRequesterUserIdAndStatus(requesterUserId: UUID, status: ConsultationRequestStatus, pageable: Pageable): Page<ConsultationRequest>
  + findByRequesterUserId(requesterUserId: UUID, pageable: Pageable): Page<ConsultationRequest>
  + findByExpertProfileIdAndStatus(expertProfileId: UUID, status: ConsultationRequestStatus, pageable: Pageable): Page<ConsultationRequest>
  + findByExpertProfileId(expertProfileId: UUID, pageable: Pageable): Page<ConsultationRequest>
  + countByExpertProfileIdAndStatus(expertProfileId: UUID, status: ConsultationRequestStatus): long
}
class "ConsultationRequest" as Entity1 <<Entity>> {
  - id: UUID
  - requesterUserId: UUID
  - expertProfileId: UUID
  - clientRequestId: UUID
  - topic: String
  - description: String
  - preferredWindowStart: Instant
}
interface "JpaRepository<ConsultationRequest, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Firebase notification outbox" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
Service2 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Free Expert Conversation Request Lifecycle**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF05_03_FreeExpertConversationRequestLifecycle_SequenceDiagram
skinparam shadowing false

actor "Mother / Family / Expert" as Actor
boundary ":ExpertConsultationRequestsPage" as UI1
boundary ":consultation request screens" as UI2
control ":ConsultationRequestController" as Controller1
participant ":ConsultationRequestServiceImpl" as Service1 <<service>>
participant ":ConsultationRequestRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":Firebase Cloud Messaging" as External1 <<external system>>

group UC-16 Process Expert Conversation Requests
  Actor -> UI1 : 1. startProcessExpertConversationRequests()
  activate UI1
  UI1 -> Controller1 : 2. listAssigned() / accept() / reject()
  activate Controller1
  Controller1 -> Service1 : 3. listAssigned() / accept() / reject()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByExpertProfileIdAndStatus()
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
    Service1 -> Repository1 : 4b. findByExpertProfileIdAndStatus()
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
    Service1 ->> External1 : 4b-8. notifyRequesterDecision()
    Service1 --> Controller1 : 4b-9. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-10. 200 OK / 201 Created
    deactivate Controller1
    UI1 --> Actor : 4b-11. displayConfirmedState()
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

group UC-44 Manage Own Expert Conversation Requests
  Actor -> UI2 : 5. startManageOwnExpertConversationRequests()
  activate UI2
  UI2 -> Controller1 : 6. create() / listMine() / cancel()
  activate Controller1
  Controller1 -> Service1 : 7. create() / listMine() / cancel()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 8a. findByRequesterUserId()
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
    Service1 -> Repository1 : 8b. findByRequesterUserId()
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
    Service1 ->> External1 : 8b-8. notifyExpertRequest()
    Service1 --> Controller1 : 8b-9. resultDTO
    deactivate Service1
    Controller1 --> UI2 : 8b-10. 200 OK / 201 Created
    deactivate Controller1
    UI2 --> Actor : 8b-11. displayConfirmedState()
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

@enduml
```

**Figure 2 — Sequence Diagram: Free Expert Conversation Request Lifecycle Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-16 Process Expert Conversation Requests; UC-44 Manage Own Expert Conversation Requests.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Accept locks an eligible expert and atomically creates or links the direct conversation.
- The Expert Web reject-method mismatch is an implementation gap; the authoritative Backend transition is PATCH and the Mobile client follows that contract.
- The following remains outside this contract: Booking, pricing, payment, commission and refund.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
