# MF-09 — Verified Content Browse and Consumption

| Field | Value |
| --- | --- |
| Major Feature | **MF-09 — Content Hub** |
| Function package | **Verified Content Browse and Consumption** |
| Code-first use cases | `UC-CO-05` |
| Status | **Draft** |
| Baseline | Current reachable code and tests, 2026-08-23 |
| Diagram convention | `../sequence-diagram-skill.md`; explicit lifeline order, numbered messages, balanced activation |

## 1. Tổng quan luồng chính

Design consumer browse/detail access to lifecycle-eligible health content.

- **UC-CO-05 — Browse Verified Health Content:** Browse/search consumer-visible verified content and open lifecycle/checklist content eligible for the current stage.

The code is authoritative for routes, handlers, delegation, authorization, and persisted state. Report1 section 6.2 is authoritative for the Major Feature name. Historical UC numbering and obsolete triage plans are not design inputs.

## 2. Function Design — Code Traceability

| UC | Actor goal | Method / route | Exact handler | Delegated function | Authorization | Source |
| --- | --- | --- | --- | --- | --- | --- |
| `UC-CO-05` | Browse Verified Health Content | `GET /api/v1/content` | `ContentController.getContents()` | `ContentService.getContents()` → `ContentRepository.findByFilters()` | No @PreAuthorize on handler/class; effective access comes from the security chain | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `UC-CO-05` | Browse Verified Health Content | `GET /api/v1/content/checklists` | `ContentController.getChecklists()` | `ContentService.getChecklists()` → `ChecklistTemplateRepository.findAllOptionalByStatus()` | No @PreAuthorize on handler/class; effective access comes from the security chain | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `UC-CO-05` | Browse Verified Health Content | `GET /api/v1/content/lifecycle` | `ContentController.getLifecycleContents()` | `ContentService.getLifecycleContents()` → `ContentRepository.findByFilters()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `UC-CO-05` | Browse Verified Health Content | `GET /api/v1/content/lifecycle/{id}` | `ContentController.getLifecycleContentById()` | `ContentService.getLifecycleContentById()` → `ContentRepository.findByIdAndStageAndStatus()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `UC-CO-05` | Browse Verified Health Content | `GET /api/v1/content/search` | `ContentController.searchContent()` | `ContentService.searchContent()` → `ContentRepository.searchByFilters()` | No @PreAuthorize on handler/class; effective access comes from the security chain | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| `UC-CO-05` | Browse Verified Health Content | `GET /api/v1/content/{id}` | `ContentController.getContentById()` | `ContentService.getContentById()` → `ContentRepository.findByIdAndStatus()` | No @PreAuthorize on handler/class; effective access comes from the security chain | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |

## 3. Class Diagram

This design-level UML follows the current source declarations: fields become attributes, reachable methods become operations, `implements` becomes realization, repository inheritance becomes generalization, and runtime-only calls remain dependencies. A missing layer or ownership relation is not invented.

```plantuml
@startuml ClassDiagram_01VerifiedContentBrowseandConsumption
skinparam classAttributeIconSize 0
hide empty members

class "ViewContentScreen" as UIViewContentScreen <<UI>>
class "ContentController" as ControllerContentController <<Controller>> {
  - contentService: ContentService
  + getContents(type: ContentType, stage: ContentStage, topicId: UUID, page: int, size: int): ResponseEntity<PaginatedResponse<ContentListResponse>>
}
interface "ContentService" as ServiceContractContentService <<Service>> {
  + getContents(filter: ContentFilterRequest, pageable: Pageable): Page<ContentListResponse>
}
class "ContentServiceImpl" as ServiceContentServiceImpl <<Service>> {
  - contentRepository: ContentRepository
  - checklistTemplateRepository: ChecklistTemplateRepository
  - checklistItemRepository: ChecklistItemRepository
  - contentMapper: ContentMapper
  - lifecycleContentStageResolver: LifecycleContentStageResolver
  + getContents(filter: ContentFilterRequest, pageable: Pageable): Page<ContentListResponse>
}
ServiceContractContentService <|.. ServiceContentServiceImpl : implements
interface "ContentRepository" as RepositoryContentRepository <<Repository>> {
  + findByFilters(type: ContentType, stage: ContentStage, topicId: UUID, status: ContentStatus, pageable: Pageable): Page<ContentItem>
}
class "ContentItem" as EntityContentItem <<Entity>> {
  - id: UUID
  - type: ContentType
  - title: String
  - body: String
  - summary: String
  - stage: ContentStage
  - eligibleFromWeek: Short
  - eligibleToWeek: Short
}
interface "JpaRepository<ContentItem, UUID>" as RepositoryBaseContentRepository <<Framework>>
RepositoryBaseContentRepository <|-- RepositoryContentRepository : extends
class "PostgreSQL" as DB <<Database>>
UIViewContentScreen ..> ControllerContentController : invokes API
ControllerContentController --> ServiceContractContentService : delegates
ServiceContentServiceImpl --> RepositoryContentRepository : reads / writes
RepositoryContentRepository ..> EntityContentItem : maps
RepositoryContentRepository ..> DB : persists
@enduml
```

**Figure 1 — Class Diagram: Verified Content Browse and Consumption**

## 4. Sequence Diagram — Main Flow

Each group is a representative reachable main flow. The full endpoint surface remains in the Function Design table above.

```plantuml
@startuml
skinparam shadowing false
skinparam maxMessageSize 90
title Verified Content Browse and Consumption — code-reachable representative flows

actor "Authenticated User" as AAuthenticated_User
boundary "ViewContentScreen" as UIViewContentScreen <<boundary>>
participant "JwtAuthenticationFilter" as JWT <<middleware>>
control "ContentController" as CContentController <<control>>
participant "ContentService" as SContentService <<service>>
participant "ContentRepository" as RContentRepository <<repository>>
database "PostgreSQL" as DB

group UC-CO-05 — Browse Verified Health Content [getContents()]
AAuthenticated_User -> UIViewContentScreen : 1. browseVerifiedHealthContent()
activate UIViewContentScreen
alt [authorized request succeeds]
UIViewContentScreen -> JWT : 2a. GET /api/v1/content with bearer token
activate JWT
JWT -> CContentController : 2a-1. getContents(type, stage, topicId, page, ...)
activate CContentController
CContentController -> SContentService : 2a-2. getContents(filter, pageable)
activate SContentService
SContentService -> RContentRepository : 2a-3. findByFilters(type, stage, topicId, status, ...)
activate RContentRepository
RContentRepository -> DB : 2a-4. SELECT ContentItem via findByFilters()
activate DB
DB --> RContentRepository : 2a-5. contentItemQueryResult
deactivate DB
RContentRepository --> SContentService : 2a-6. contentItemPage
deactivate RContentRepository
SContentService --> CContentController : 2a-7. contentListResponsePage
deactivate SContentService
CContentController --> JWT : 2a-8. contentListResponse
deactivate CContentController
JWT --> UIViewContentScreen : 2a-9. 200 OK — contentListResponse
deactivate JWT
UIViewContentScreen --> AAuthenticated_User : 2a-10. displayVerifiedHealthContent()
else [authentication or role authorization fails]
UIViewContentScreen -> JWT : 2b. GET /api/v1/content with invalid or insufficient bearer token
activate JWT
JWT --> UIViewContentScreen : 2b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UIViewContentScreen --> AAuthenticated_User : 2b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UIViewContentScreen
end
@enduml
```

**Brief Explanation:**

1. The actor starts each grouped use case through the code-reachable UI boundary.
2. Protected requests pass through JwtAuthenticationFilter; rejected credentials or roles return 401 Unauthorized or 403 Forbidden without invoking the controller.
3. The controller receives the request and invokes the exact delegated operation resolved from the current source.
4. The service applies the business policy and coordinates downstream collaborators while its caller remains active.
5. The repository executes the represented persistence operation and returns the stored or queried result before its activation ends.
6. The HTTP response unwinds through middleware when present, and the UI renders the server-authoritative outcome to the actor.

## 5. State Chart Diagram

The lifecycle below belongs to **The consumer-visible ContentItem as seen through browse and detail reads — the authoring lifecycle itself is owned by MF-09 document 02**. Its states are the persisted constants declared in the sources cited under this diagram, and every transition, guard, and action is taken from the service method that writes that constant. No unreachable state is introduced.

```plantuml
@startuml StateChartDiagram_01VerifiedContentBrowseandConsumption
hide empty description
[*] --> NotVisible

NotVisible --> Visible : contentBecomesApproved()\n[status == APPROVED]\n/ includeInBrowseResults()
Visible --> NotVisible : contentLeavesApproved()\n[status != APPROVED]\n/ excludeFromBrowseResults()
Visible --> Visible : browseContent()\n[stage matches the reader]\n/ returnStageFilteredList()
Visible --> Opened : openContentDetail()\n[status == APPROVED]\n/ returnFullArticle()
Opened --> Visible : closeContentDetail()\n/ returnToBrowseResults()
Opened --> NotVisible : contentLeavesApproved()\n[status != APPROVED]\n/ denySubsequentDetailReads()

Visible : ContentStatus = APPROVED
NotVisible : DRAFT, PENDING_REVIEW, or ARCHIVED
@enduml
```

**Figure 2 — State Chart Diagram: Verified Content Browse and Consumption**

**Brief Explanation:**

1. This package is read-only, so the lifecycle shown is the consumer's view of a content item rather than an editorial workflow; the authoring states are owned by the Content Authoring package and are not duplicated here.
2. `Visible` corresponds exactly to `ContentStatus.APPROVED` — `ContentServiceImpl` queries `findByIdAndStatus(id, APPROVED)`, so no other status is reachable through a consumer route.
3. Browse is a guarded self-transition on `Visible`: results are additionally filtered by `ContentStage`, so a reader only sees material matching their lifecycle stage.
4. `openContentDetail()` re-checks `status == APPROVED` rather than trusting the browse result, which is why the detail read carries its own guard.
5. Unpublishing or archiving an item moves it out of `Visible` immediately, and an already-open article stops serving further detail reads.
6. There is no consumer transition that writes content state — every transition here is driven by editorial changes made elsewhere or by the reader's own navigation.

**State sources:**

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ContentStatus.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/service/ContentServiceImpl.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/service/RecommendationEligibilityPolicy.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/entity/ContentStage.java`

## 6. Business Rules Applied

| UC | Enforced business/security rules | Known boundary or gap |
| --- | --- | --- |
| `UC-CO-05` | Only publication/lifecycle-eligible content is consumer-visible. Verified content is editorial content, not community Q&A. | No additional gap recorded in the code-first baseline. |

## 7. Partial / Excluded Boundaries

- No extra release boundary beyond the code-first UC catalogue is introduced by this package.

## 8. Code and Test Evidence

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java`
- `05_Development/CareBridgeMobileApp/lib/features/community/screens/view_content_screen.dart`
- `05_Development/CareBridgeMobileApp/lib/features/community/screens/verified_content_detail_screen.dart`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentSearchServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/LifecycleContentServiceTest.java`
- `05_Development/CareBridgeMobileApp/test/features/community/services/content_service_test.dart`
