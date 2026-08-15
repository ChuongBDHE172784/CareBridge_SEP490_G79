# MF-06 / Spec 01 — AI Symptom Intake, History and Escalation

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-45 Use AI Nurse Symptom Triage; UC-46 View AI Triage History; UC-47 Escalate AI Triage to Emergency Support; UC-48 Request Expert Support from AI Triage |
| Use Case Group | Mobile App |
| Platform | Mother and authorized Family Mobile; Backend; AI Service |
| Primary Actors | Mother / Authorized Family |
| In Scope | GREEN YELLOW RED output is non-diagnostic and red flags set a minimum risk |
| Explicitly Excluded | Standalone RagChatScreen and autonomous diagnosis |
| Implementation Trace | UI: AI triage intake, result and history screens; Controller: IntakeController, EmergencyMapHandoffController; Service: TriageService, EmergencyMapHandoffServiceImpl; Repository: IIntakeSessionRepository; Entity: IntakeSession |

## 1. Tổng quan luồng chính (Main Flow Overview)

GREEN YELLOW RED output is non-diagnostic and red flags set a minimum risk. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF06_01_AISymptomIntakeHistoryandEscalation_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "AI triage intake" as UI1 <<UI>>
class "result and history screens" as UI2 <<UI>>
class "IntakeController" as Controller1 <<Controller>> {
  - triageService: ITriageService
  - continuationService: ITriageContinuationService
  + runIntake(request: RunIntakeRequest, principal: Principal): ResponseEntity<ApiResponse<IntakeSessionResponse>>
  + acknowledgeContinuation(request: ContinuationTokenRequest, principal: Principal): ResponseEntity<ApiResponse<ContinuationAcknowledgementResponse>>
  + continueConversation(request: ContinueIntakeConversationRequest, principal: Principal): ResponseEntity<ApiResponse<IntakeConversationResponse>>
  + getResult(sessionId: UUID, principal: Principal): ResponseEntity<ApiResponse<TriageResultResponse>>
  + listSessions(principal: Principal): ResponseEntity<ApiResponse<List<IntakeSessionResponse>>>
  + resolveContinuation(request: ContinuationTokenRequest, principal: Principal): ResponseEntity<ApiResponse<ContinuationDescriptor>>
  + startConversation(request: StartIntakeConversationRequest, principal: Principal): ResponseEntity<ApiResponse<IntakeConversationResponse>>
}
class "EmergencyMapHandoffController" as Controller2 <<Controller>> {
  - emergencyMapHandoffService: IEmergencyMapHandoffService
  + createHandoff(principal: Principal, request: CreateEmergencyHandoffRequest): ResponseEntity<ApiResponse<EmergencyHandoffResponse>>
  + getHandoff(principal: Principal, handoffId: UUID): ResponseEntity<ApiResponse<EmergencyHandoffResponse>>
  + getMyHandoffs(principal: Principal): ResponseEntity<ApiResponse<List<EmergencyHandoffResponse>>>
}
class "TriageService" as Service1 <<Service>> {
  - log: Logger
  - intakeSessionRepository: IIntakeSessionRepository
  - childTriageAiClient: ChildTriageAiClient
  - triageGraphService: TriageGraphService
  - toRunIntakeRequest(Map<String, intake: Object>): RunIntakeRequest
  - isBoundedValue(value: Object, depth: int): boolean
  - isPersistableRiskLevel(riskLevel: String): boolean
  + changeStatus(id: UUID, newStatus: String, notes: String, ...): EvidenceSource
  + isApprovedDeepLink(uri: URI): boolean
}
class "EmergencyMapHandoffServiceImpl" as Service2 <<Service>> {
  - handoffRepository: EmergencyMapHandoffRepository
  - handoffMapper: EmergencyMapHandoffMapper
  - intakeSessionRepository: IIntakeSessionRepository
  - careFacilityRepository: CareFacilityRepository
  + createHandoff(userId: UUID, request: CreateEmergencyHandoffRequest): EmergencyHandoffResponse
  + getHandoff(handoffId: UUID, callerId: UUID, systemAdmin: boolean): EmergencyHandoffResponse
  + getMyHandoffs(userId: UUID): List<EmergencyHandoffResponse>
  - validateCoordinates(latitude: BigDecimal, longitude: BigDecimal): void
}
interface "ITriageService" as Service1Contract <<Service>>
interface "IEmergencyMapHandoffService" as Service2Contract <<Service>>
interface "IIntakeSessionRepository" as Repository1 {
  + findByIdAndUserId(id: UUID, userId: UUID): Optional<IntakeSession>
  + findByUserIdAndClientRequestId(userId: UUID, clientRequestId: String): Optional<IntakeSession>
  + findByUserIdAndContinuationToken(userId: UUID, continuationToken: UUID): Optional<IntakeSession>
  + findByUserIdOrderByCreatedAtDesc(userId: UUID): List<IntakeSession>
}
class "IntakeSession" as Entity1 <<Entity>> {
  - id: UUID
  - userId: UUID
  - babyProfileId: UUID
  - motherProfileId: UUID
  - stage: TriageStage
  - clientRequestId: String
  - journeyId: UUID
}
interface "JpaRepository<IntakeSession, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Approved AI triage service" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI2 ..> Controller2 : emergency handoff
Controller1 --> Service1Contract : delegates
Controller2 --> Service2Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
Service2 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: AI Symptom Intake, History and Escalation**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF06_01_AISymptomIntakeHistoryandEscalation_SequenceDiagram
skinparam shadowing false

actor "Mother / Authorized Family" as Actor
boundary ":AI triage intake" as UI1
boundary ":result and history screens" as UI2
boundary ":result screen" as UI3
control ":IntakeController" as Controller1
control ":EmergencyMapHandoffController" as Controller2
control ":TriageExpertHandoffController" as Controller3
participant ":TriageService" as Service1 <<service>>
participant ":EmergencyMapHandoffServiceImpl" as Service2 <<service>>
participant ":ConsultationRequestServiceImpl" as Service3 <<service>>
participant ":IIntakeSessionRepository" as Repository1 <<repository>>
participant ":ConsultationRequestRepository" as Repository2 <<repository>>
database "PostgreSQL" as DB
participant ":Approved AI triage service" as External1 <<external system>>
participant ":Firebase Cloud Messaging" as External2 <<external system>>

group UC-45 Use AI Nurse Symptom Triage
  Actor -> UI1 : 1. startUseAiNurseSymptomTriage()
  activate UI1
  UI1 -> Controller1 : 2. startConversation() / runIntake()
  activate Controller1
  Controller1 -> Service1 : 3. startConversation() / runIntake()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByUserIdAndClientRequestId()
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
    Service1 -> Repository1 : 4b. findByUserIdAndClientRequestId()
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
    Service1 -> External1 : 4b-8. evaluateSymptomsAndRisk()
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

group UC-46 View AI Triage History
  Actor -> UI2 : 5. startViewAiTriageHistory()
  activate UI2
  UI2 -> Controller1 : 6. listSessions() / getResult()
  activate Controller1
  Controller1 -> Service1 : 7. listSessions() / getResult()
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 8a. findByUserIdOrderByCreatedAtDesc()
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
    UI2 --> Actor : 8a-6. displayViewAiTriageHistoryResult()
    deactivate UI2
  else [request is invalid, forbidden or unavailable]
    Service1 --> Controller1 : 8b. domainError
    deactivate Service1
    Controller1 --> UI2 : 8b-1. 400 / 401 / 403 / 404
    deactivate Controller1
    UI2 --> Actor : 8b-2. displayActionableError()
    deactivate UI2
  end
end

group UC-47 Escalate AI Triage to Emergency Support
  Actor -> UI3 : 9. startEscalateAiTriageToEmergencySupport()
  activate UI3
  UI3 -> Controller2 : 10. createHandoff(intakeSessionId)
  activate Controller2
  Controller2 -> Service2 : 11. createHandoff(intakeSessionId)
  activate Service2
  alt [command is valid and actor is authorized]
    Service2 -> Repository1 : 12a. findByIdAndUserId()
    activate Repository1
    Repository1 -> DB : 12a-1. SELECT
    activate DB
    DB --> Repository1 : 12a-2. currentState
    deactivate DB
    Repository1 --> Service2 : 12a-3. scopedEntity
    deactivate Repository1
    Service2 -> Repository1 : 12a-4. findByIdAndUserId()
    activate Repository1
    Repository1 -> DB : 12a-5. SELECT
    activate DB
    DB --> Repository1 : 12a-6. persistedState
    deactivate DB
    Repository1 --> Service2 : 12a-7. savedEntity
    deactivate Repository1
    Service2 --> Controller2 : 12a-8. resultDTO
    deactivate Service2
    Controller2 --> UI3 : 12a-9. 200 OK / 201 Created
    deactivate Controller2
    UI3 --> Actor : 12a-10. displayConfirmedState()
    deactivate UI3
  else [validation, authorization or state check fails]
    Service2 --> Controller2 : 12b. domainError
    deactivate Service2
    Controller2 --> UI3 : 12b-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller2
    UI3 --> Actor : 12b-2. displayActionableError()
    deactivate UI3
  end
end

group UC-48 Request Expert Support from AI Triage
  Actor -> UI3 : 13. startRequestExpertSupportFromAiTriage()
  activate UI3
  UI3 -> Controller3 : 14. create(request)
  activate Controller3
  Controller3 -> Service3 : 15. create(request)
  activate Service3
  alt [command is valid and actor is authorized]
    Service3 -> Repository2 : 16a. save(consultationRequest)
    activate Repository2
    Repository2 -> DB : 16a-1. INSERT / UPDATE
    activate DB
    DB --> Repository2 : 16a-2. persistedState
    deactivate DB
    Repository2 --> Service3 : 16a-3. savedEntity
    deactivate Repository2
    Service3 ->> External2 : 16a-4. notifyExpertRequest()
    Service3 --> Controller3 : 16a-5. resultDTO
    deactivate Service3
    Controller3 --> UI3 : 16a-6. 200 OK / 201 Created
    deactivate Controller3
    UI3 --> Actor : 16a-7. displayConfirmedState()
    deactivate UI3
  else [validation, authorization or state check fails]
    Service3 --> Controller3 : 16b. domainError
    deactivate Service3
    Controller3 --> UI3 : 16b-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller3
    UI3 --> Actor : 16b-2. displayActionableError()
    deactivate UI3
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: AI Symptom Intake, History and Escalation Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-45 Use AI Nurse Symptom Triage; UC-46 View AI Triage History; UC-47 Escalate AI Triage to Emergency Support; UC-48 Request Expert Support from AI Triage.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- GREEN YELLOW RED output is non-diagnostic and red flags set a minimum risk.
- The following remains outside this contract: Standalone RagChatScreen and autonomous diagnosis.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
