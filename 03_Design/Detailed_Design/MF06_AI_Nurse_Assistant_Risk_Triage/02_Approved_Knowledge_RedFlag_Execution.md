# MF-06 / Spec 02 — Approved Evidence and Red-Flag Rule Administration

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-83 Manage AI Red-Flag Rules |
| Use Case Group | Web App and internal AI flow |
| Platform | Admin Web; Backend; AI Service |
| Primary Actors | System Admin |
| In Scope | System-default rules cannot be deleted and approved evidence only may ground output |
| Explicitly Excluded | Standalone RAG chat UI |
| Implementation Trace | UI: SafetyRuleManagementPage, SystemConfigurationPage; Controller: RedFlagRuleController, InternalEvidenceSourceController; Service: RedFlagRuleServiceImpl; Repository: RedFlagRuleRepository; Entity: RedFlagRule |

## 1. Tổng quan luồng chính (Main Flow Overview)

System-default rules cannot be deleted and approved evidence only may ground output. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF06_02_ApprovedEvidenceandRedFlagRuleAdministration_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "SafetyRuleManagementPage" as UI1 <<UI>>
class "SystemConfigurationPage" as UI2 <<UI>>
class "RedFlagRuleController" as Controller1 <<Controller>> {
  - redFlagRuleService: RedFlagRuleService
  + create(request: CreateRedFlagRuleRequest, principal: Principal): ResponseEntity<ApiResponse<RedFlagRuleResponse>>
  + delete(id: UUID, principal: Principal): ResponseEntity<Void>
  + update(id: UUID, request: UpdateRedFlagRuleRequest, principal: Principal): ResponseEntity<ApiResponse<RedFlagRuleResponse>>
}
class "InternalEvidenceSourceController" as Controller2 <<Controller>> {
  - evidenceSourceService: EvidenceSourceService
  - internalApiKey: String
}
class "RedFlagRuleServiceImpl" as Service1 <<Service>> {
  - redFlagRuleRepository: RedFlagRuleRepository
  - auditService: AuditService
  + createRule(request: CreateRedFlagRuleRequest, actorUserId: UUID): RedFlagRuleResponse
  + deleteRule(ruleId: UUID, actorUserId: UUID): void
  + listRules(filter: RedFlagRuleFilter): RedFlagRulePageResponse
  + updateRule(ruleId: UUID, request: UpdateRedFlagRuleRequest, actorUserId: UUID): RedFlagRuleResponse
  - toAuditDetails(rule: RedFlagRule, changeType: String): RedFlagRuleAuditDetails
}
interface "RedFlagRuleService" as Service1Contract <<Service>>
interface "RedFlagRuleRepository" as Repository1 {
  + findBySeverityAndActiveTrue(severity: RedFlagSeverity): List<RedFlagRule>
  + existsByKeywordIgnoreCase(keyword: String): boolean
  + findBySeverityAndActive(severity: RedFlagSeverity, active: Boolean, pageable: Pageable): Page<RedFlagRule>
  + findByActiveTrue(): List<RedFlagRule>
}
class "RedFlagRule" as Entity1 <<Entity>> {
  - id: UUID
  - keyword: String
  - severity: RedFlagSeverity
  - action: RedFlagAction
  - active: boolean
  - systemDefault: boolean
  - createdBy: UUID
}
interface "JpaRepository<RedFlagRule, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Approved evidence service" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller2 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Approved Evidence and Red-Flag Rule Administration**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF06_02_ApprovedEvidenceandRedFlagRuleAdministration_SequenceDiagram
skinparam shadowing false

actor "System Admin" as Actor
boundary ":SafetyRuleManagementPage" as UI1
control ":RedFlagRuleController" as Controller1
participant ":RedFlagRuleServiceImpl" as Service1 <<service>>
participant ":RedFlagRuleRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":Approved evidence service" as External1 <<external system>>

group UC-83 Manage AI Red-Flag Rules
  Actor -> UI1 : 1. startManageAiRedFlagRules()
  activate UI1
  UI1 -> Controller1 : 2. list() / create() / update() / delete()
  activate Controller1
  Controller1 -> Service1 : 3. listRules() / createRule() / updateRule() / deleteRule()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findByActiveTrue()
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
    Service1 -> Repository1 : 4b. findByActiveTrue()
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
    Service1 -> External1 : 4b-8. validateApprovedEvidence()
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

@enduml
```

**Figure 2 — Sequence Diagram: Approved Evidence and Red-Flag Rule Administration Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-83 Manage AI Red-Flag Rules.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- System-default rules cannot be deleted and approved evidence only may ground output.
- The following remains outside this contract: Standalone RAG chat UI.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
