# MF-09 / Spec 01 — Verified Content and FAQ Consumption

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-62 Browse Verified Content and FAQ |
| Use Case Group | Mobile App |
| Platform | Mother and Family Mobile; Backend |
| Primary Actors | Mother / Family |
| In Scope | Only approved visible lifecycle content is returned |
| Explicitly Excluded | Unapproved drafts and sponsored partner content |
| Implementation Trace | UI: ViewContentScreen, VerifiedContentDetailScreen; Controller: ContentController; Service: ContentServiceImpl; Repository: ContentRepository; Entity: ContentItem |

## 1. Tổng quan luồng chính (Main Flow Overview)

Only approved visible lifecycle content is returned. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF09_01_VerifiedContentandFAQConsumption_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "ViewContentScreen" as UI1 <<UI>>
class "VerifiedContentDetailScreen" as UI2 <<UI>>
class "ContentController" as Controller1 <<Controller>> {
  - contentService: ContentService
  + getContentById(id: UUID): ResponseEntity<ApiResponse<ContentDetailResponse>>
  - validatePage(page: int, size: int): void
}
class "ContentServiceImpl" as Service1 <<Service>> {
  - contentRepository: ContentRepository
  - checklistTemplateRepository: ChecklistTemplateRepository
  - checklistItemRepository: ChecklistItemRepository
  - contentMapper: ContentMapper
  + getLifecycleContentById(ownerId: UUID, id: UUID): LifecycleContentEnvelope<ContentDetailResponse>
  + getContentById(id: UUID): ContentDetailResponse
  + getLifecycleChecklists(ownerId: UUID): LifecycleContentEnvelope<List<ChecklistTemplateResponse>>
  + getLifecycleContents(ownerId: UUID, type: ContentType, topicId: UUID, ...): LifecycleContentEnvelope<Page<ContentListResponse>>
  + searchContent(request: ContentSearchRequest, pageable: Pageable): Page<ContentSearchResponse>
}
interface "ContentService" as Service1Contract <<Service>>
interface "ContentRepository" as Repository1 {
  + findByTitleIgnoreCaseAndStageAndType(title: String, stage: ContentStage, type: ContentType): Optional<ContentItem>
  + findByIdAndStatus(id: UUID, status: ContentStatus): Optional<ContentItem>
  + findByIdAndStageAndStatus(id: UUID, stage: ContentStage, status: ContentStatus): Optional<ContentItem>
  + findByStatus(status: ContentStatus, pageable: Pageable): Page<ContentItem>
  + findByType(type: ContentType, pageable: Pageable): Page<ContentItem>
  + countByPublishedAtIsNotNull(): long
}
class "ContentItem" as Entity1 <<Entity>> {
  - id: UUID
  - type: ContentType
  - title: String
  - body: String
  - summary: String
  - stage: ContentStage
  - eligibleFromWeek: Short
}
interface "JpaRepository<ContentItem, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
@enduml
```

**Figure 1 — Class Diagram: Verified Content and FAQ Consumption**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF09_01_VerifiedContentandFAQConsumption_SequenceDiagram
skinparam shadowing false

actor "Mother / Family" as Actor
boundary ":ViewContentScreen" as UI1
control ":ContentController" as Controller1
participant ":ContentServiceImpl" as Service1 <<service>>
participant ":ContentRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB

group UC-62 Browse Verified Content and FAQ
  Actor -> UI1 : 1. startBrowseVerifiedContentAndFaq()
  activate UI1
  UI1 -> Controller1 : 2. searchContent(filters) / getContentById(id)
  activate Controller1
  Controller1 -> Service1 : 3. searchContent(filters) / getContentById(id)
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 4a. findApprovedTargetedArticlesForRecommendation()
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
    UI1 --> Actor : 4a-6. displayBrowseVerifiedContentAndFaqResult()
    deactivate UI1
  else [request is invalid, forbidden or unavailable]
    Service1 --> Controller1 : 4b. domainError
    deactivate Service1
    Controller1 --> UI1 : 4b-1. 400 / 401 / 403 / 404
    deactivate Controller1
    UI1 --> Actor : 4b-2. displayActionableError()
    deactivate UI1
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Verified Content and FAQ Consumption Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-62 Browse Verified Content and FAQ.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Only approved visible lifecycle content is returned.
- The following remains outside this contract: Unapproved drafts and sponsored partner content.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
