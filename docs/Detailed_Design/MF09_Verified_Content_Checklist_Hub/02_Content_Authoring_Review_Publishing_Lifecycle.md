# MF-09 / Spec 02 — Content Administration, Authoring and Approval

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-84 View Content Administration Workspace; UC-85 Manage Verified Articles; UC-86 Manage Verified FAQs; UC-87 Manage Content Topics; UC-88 Review and Approve Content |
| Use Case Group | Web App |
| Platform | Content Admin Web; Admin Web; Backend |
| Primary Actors | Content Admin / System Admin |
| In Scope | Author and approver permissions are separated and decisions are audited |
| Explicitly Excluded | Partner or sponsored content |
| Implementation Trace | UI: ContentDashboardPage, ArticleListPage, FaqListPage, ManageTopicsPage, ContentApprovalQueuePage; Controller: AdminContentController, ContentApprovalController, ContentUnpublishController; Service: AdminContentServiceImpl, ContentApprovalServiceImpl, ContentUnpublishServiceImpl; Repository: ContentRepository; Entity: ContentItem |

## 1. Tổng quan luồng chính (Main Flow Overview)

Author and approver permissions are separated and decisions are audited. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF09_02_ContentAdministrationAuthoringandApproval_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "ContentDashboardPage" as UI1 <<UI>>
class "ArticleListPage" as UI2 <<UI>>
class "FaqListPage" as UI3 <<UI>>
class "ManageTopicsPage" as UI4 <<UI>>
class "ContentApprovalQueuePage" as UI5 <<UI>>
class "AdminContentController" as Controller1 <<Controller>> {
  - adminContentService: AdminContentService
  - contentService: ContentService
  + createContent(request: CreateContentRequest, principal: Principal): ResponseEntity<ApiResponse<CreateContentResponse>>
  + getContent(id: UUID): ResponseEntity<ApiResponse<StaffContentDetailResponse>>
  + hideContent(id: UUID, request: HideContentRequest, principal: Principal): ResponseEntity<ApiResponse<HideContentResponse>>
  + updateContent(id: UUID, request: UpdateContentRequest, principal: Principal): ResponseEntity<ApiResponse<UpdateContentResponse>>
  + getVersionHistory(id: UUID): ResponseEntity<ApiResponse<List<ContentVersionSnapshotResponse>>>
}
class "ContentApprovalController" as Controller2 <<Controller>> {
  - contentApprovalService: ContentApprovalService
  + decide(id: UUID, request: ContentDecisionRequest, principal: Principal): ResponseEntity<ApiResponse<ContentDecisionResponse>>
}
class "ContentUnpublishController" as Controller3 <<Controller>> {
  - service: ContentUnpublishService
  + unpublish(id: UUID, request: UnpublishRequest, principal: Principal): ResponseEntity<ApiResponse<UnpublishResponse>>
}
class "AdminContentServiceImpl" as Service1 <<Service>> {
  - contentRepository: ContentRepository
  - communityTopicRepository: CommunityTopicRepository
  - contentMapper: ContentMapper
  - auditService: AuditService
  + createContent(request: CreateContentRequest, authorUserId: java.util.UUID): CreateContentResponse
  + getStaffContent(id: UUID): StaffContentDetailResponse
  + hideContent(id: UUID, request: HideContentRequest, principal: Principal): HideContentResponse
  + updateContent(id: UUID, request: UpdateContentRequest, principal: Principal): UpdateContentResponse
  - clearReviewFeedback(item: ContentItem): void
}
class "ContentApprovalServiceImpl" as Service2 <<Service>> {
  - contentRepository: ContentRepository
  - auditService: AuditService
  - aiScanEnqueueService: com.carebridge.backend.aimoderation.service.AiScanEnqueueService
  - contentReviewNotificationService: ContentReviewNotificationService
  - clearReviewFeedback(item: ContentItem): void
  + decide(id: UUID, request: ContentDecisionRequest, principal: Principal): ContentDecisionResponse
}
class "ContentUnpublishServiceImpl" as Service3 <<Service>> {
  - contentRepository: ContentRepository
  - auditService: AuditService
  + unpublish(id: UUID, request: UnpublishRequest, adminId: UUID): UnpublishResponse
}
interface "AdminContentService" as Service1Contract <<Service>>
interface "ContentApprovalService" as Service2Contract <<Service>>
interface "ContentUnpublishService" as Service3Contract <<Service>>
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
class "File storage" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Service3Contract <|.. Service3 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller1 : invokes API
UI4 ..> Controller1 : invokes API
UI5 ..> Controller2 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service2Contract : delegates
Controller3 --> Service3Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository1 : reads / writes
Service3 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Content Administration, Authoring and Approval**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF09_02_ContentAdministrationAuthoringandApproval_SequenceDiagram
skinparam shadowing false

actor "Content Admin / System Admin" as Actor
boundary ":ContentDashboardPage" as UI1
boundary ":ArticleListPage" as UI2
boundary ":FaqListPage" as UI3
boundary ":ManageTopicsPage" as UI4
boundary ":ContentApprovalQueuePage" as UI5
control ":AdminContentController" as Controller1
control ":ContentApprovalController" as Controller2
participant ":AdminContentServiceImpl" as Service1 <<service>>
participant ":ContentApprovalServiceImpl" as Service2 <<service>>
participant ":ContentRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":File storage" as External1 <<external system>>
participant ":Notification service" as External2 <<external system>>

group UC-84 View Content Administration Workspace
  Actor -> UI1 : 1. startViewContentAdministrationWorkspace()
  activate UI1
  UI1 -> Controller1 : 2. getContents(filters) / getContent(id)
  activate Controller1
  Controller1 -> Service1 : 3. getStaffContents(filters) / getStaffContent(id)
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 4a. findAll(specification, pageable)
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
    UI1 --> Actor : 4a-6. displayViewContentAdministrationWorkspaceResult()
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

group UC-85 Manage Verified Articles
  Actor -> UI2 : 5. startManageVerifiedArticles()
  activate UI2
  UI2 -> Controller1 : 6. createContent(ARTICLE) / updateContent() / hideContent()
  activate Controller1
  Controller1 -> Service1 : 7. createContent(ARTICLE) / updateContent() / hideContent()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 8a. findById()
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
    Service1 -> Repository1 : 8b. findById()
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
    Service1 -> External1 : 8b-8. uploadArticleMedia()
    activate External1
    External1 --> Service1 : 8b-9. integrationResult
    deactivate External1
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

group UC-86 Manage Verified FAQs
  Actor -> UI3 : 9. startManageVerifiedFaqs()
  activate UI3
  UI3 -> Controller1 : 10. createContent(FAQ) / updateContent() / hideContent()
  activate Controller1
  Controller1 -> Service1 : 11. createContent(FAQ) / updateContent() / hideContent()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 12a. findById()
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
    Service1 -> Repository1 : 12b. findById()
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
    Service1 --> Controller1 : 12b-8. resultDTO
    deactivate Service1
    Controller1 --> UI3 : 12b-9. 200 OK / 201 Created
    deactivate Controller1
    UI3 --> Actor : 12b-10. displayConfirmedState()
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

group UC-87 Manage Content Topics
  Actor -> UI4 : 13. startManageContentTopics()
  activate UI4
  UI4 -> Controller1 : 14. getContents() / createContent() / updateContent()
  activate Controller1
  Controller1 -> Service1 : 15. getStaffContents() / createContent() / updateContent()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 16a. findByType()
    activate Repository1
    Repository1 -> DB : 16a-1. SELECT
    activate DB
    DB --> Repository1 : 16a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 16a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 16a-4. resultDTO
    deactivate Service1
    Controller1 --> UI4 : 16a-5. 200 OK
    deactivate Controller1
    UI4 --> Actor : 16a-6. displayCurrentState()
    deactivate UI4
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository1 : 16b. findByType()
    activate Repository1
    Repository1 -> DB : 16b-1. SELECT
    activate DB
    DB --> Repository1 : 16b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 16b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 16b-4. save()
    activate Repository1
    Repository1 -> DB : 16b-5. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 16b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 16b-7. persistedEntity
    deactivate Repository1
    Service1 --> Controller1 : 16b-8. resultDTO
    deactivate Service1
    Controller1 --> UI4 : 16b-9. 200 OK / 201 Created
    deactivate Controller1
    UI4 --> Actor : 16b-10. displayConfirmedState()
    deactivate UI4
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 16c. domainError
    deactivate Service1
    Controller1 --> UI4 : 16c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI4 --> Actor : 16c-2. displayActionableError()
    deactivate UI4
  end
end

group UC-88 Review and Approve Content
  Actor -> UI5 : 17. startReviewAndApproveContent()
  activate UI5
  UI5 -> Controller2 : 18. decide(approveOrReject)
  activate Controller2
  Controller2 -> Service2 : 19. decide(approveOrReject)
  activate Service2
  alt [selected action is view or list]
    Service2 -> Repository1 : 20a. findByStatus()
    activate Repository1
    Repository1 -> DB : 20a-1. SELECT
    activate DB
    DB --> Repository1 : 20a-2. queryResult
    deactivate DB
    Repository1 --> Service2 : 20a-3. domainRecords
    deactivate Repository1
    Service2 --> Controller2 : 20a-4. resultDTO
    deactivate Service2
    Controller2 --> UI5 : 20a-5. 200 OK
    deactivate Controller2
    UI5 --> Actor : 20a-6. displayCurrentState()
    deactivate UI5
  else [selected action creates, updates, archives or deletes]
    Service2 -> Repository1 : 20b. findByStatus()
    activate Repository1
    Repository1 -> DB : 20b-1. SELECT
    activate DB
    DB --> Repository1 : 20b-2. currentState
    deactivate DB
    Repository1 --> Service2 : 20b-3. scopedEntity
    deactivate Repository1
    Service2 -> Repository1 : 20b-4. save()
    activate Repository1
    Repository1 -> DB : 20b-5. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 20b-6. persistedState
    deactivate DB
    Repository1 --> Service2 : 20b-7. persistedEntity
    deactivate Repository1
    Service2 ->> External2 : 20b-8. notifyAuthorDecision()
    Service2 --> Controller2 : 20b-9. resultDTO
    deactivate Service2
    Controller2 --> UI5 : 20b-10. 200 OK / 201 Created
    deactivate Controller2
    UI5 --> Actor : 20b-11. displayConfirmedState()
    deactivate UI5
  else [request is invalid, forbidden, not found or conflicting]
    Service2 --> Controller2 : 20c. domainError
    deactivate Service2
    Controller2 --> UI5 : 20c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller2
    UI5 --> Actor : 20c-2. displayActionableError()
    deactivate UI5
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Content Administration, Authoring and Approval Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-84 View Content Administration Workspace; UC-85 Manage Verified Articles; UC-86 Manage Verified FAQs; UC-87 Manage Content Topics; UC-88 Review and Approve Content.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Author and approver permissions are separated and decisions are audited.
- The following remains outside this contract: Partner or sponsored content.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
