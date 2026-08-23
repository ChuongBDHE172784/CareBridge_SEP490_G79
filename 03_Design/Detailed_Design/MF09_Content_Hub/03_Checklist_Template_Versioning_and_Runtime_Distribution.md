# MF-09 — Checklist Template Versioning and Runtime Distribution

| Field | Value |
| --- | --- |
| Major Feature | **MF-09 — Content Hub** |
| Function package | **Checklist Template Versioning and Runtime Distribution** |
| Code-first use cases | `UC-AD-10, UC-AD-11` |
| Status | **Draft** |
| Baseline | Current reachable code and tests, 2026-08-23 |
| Diagram convention | `../sequence-diagram-skill.md`; explicit lifeline order, numbered messages, balanced activation |

## 1. Tổng quan luồng chính

Design checklist template/version governance and distribution boundaries.

- **UC-AD-10 — Author Checklist Templates:** Create, edit, clone, import, version, and archive checklist templates before administrative approval.
- **UC-AD-11 — Review, Approve, and Activate Checklist Versions:** Review submitted checklist versions, record an approval decision, and activate an eligible approved version for distribution.

The code is authoritative for routes, handlers, delegation, authorization, and persisted state. Report1 section 6.2 is authoritative for the Major Feature name. Historical UC numbering and obsolete triage plans are not design inputs.

## 2. Function Design — Code Traceability

| UC | Actor goal | Method / route | Exact handler | Delegated function | Authorization | Source |
| --- | --- | --- | --- | --- | --- | --- |
| `UC-AD-10` | Author Checklist Templates | `GET /api/v1/admin/checklist-templates` | `AdminChecklistTemplateController.list()` | `AdminChecklistTemplateService.list()` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `UC-AD-10` | Author Checklist Templates | `POST /api/v1/admin/checklist-templates` | `AdminChecklistTemplateController.create()` | `AdminChecklistTemplateService.create()` → `ChecklistTemplateRepository.save()` | hasRole('CONTENT_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `UC-AD-10` | Author Checklist Templates | `POST /api/v1/admin/checklist-templates/import-batch` | `AdminChecklistTemplateController.importBatch()` | `ChecklistTemplateBatchImportService.importBatch()` | hasRole('CONTENT_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `UC-AD-10` | Author Checklist Templates | `GET /api/v1/admin/checklist-templates/{id}` | `AdminChecklistTemplateController.getById()` | `AdminChecklistTemplateService.getById()` → `ChecklistTemplateRepository.findById()` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `UC-AD-10` | Author Checklist Templates | `PUT /api/v1/admin/checklist-templates/{id}` | `AdminChecklistTemplateController.update()` | `AdminChecklistTemplateService.update()` → `ChecklistTemplateRepository.findById()` | hasRole('CONTENT_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `UC-AD-10` | Author Checklist Templates | `POST /api/v1/admin/checklist-templates/{id}/archive` | `AdminChecklistTemplateController.archive()` | `AdminChecklistTemplateService.archive()` → `ChecklistTemplateRepository.findById()` | hasRole('CONTENT_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `UC-AD-10` | Author Checklist Templates | `POST /api/v1/admin/checklist-templates/{id}/clone` | `AdminChecklistTemplateController.cloneVersion()` | `AdminChecklistTemplateService.cloneVersion()` → `ChecklistTemplateRepository.findById()` | hasRole('CONTENT_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `UC-AD-10` | Author Checklist Templates | `GET /api/v1/admin/checklist-templates/{id}/versions` | `AdminChecklistTemplateController.getVersionHistory()` | `AdminChecklistTemplateService.getVersionHistory()` → `ChecklistTemplateRepository.existsById()` | hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `UC-AD-10` | Author Checklist Templates | `POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/clone` | `AdminChecklistTemplateController.cloneVersionInLineage()` | — | hasRole('CONTENT_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java` |
| `UC-AD-11` | Review, Approve, and Activate Checklist Versions | `POST /api/v1/admin/checklist-templates/{id}/decision` | `ChecklistTemplateApprovalController.decide()` | `ChecklistTemplateApprovalService.decide()` → `ChecklistTemplateRepository.findById()` | hasRole('SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ChecklistTemplateApprovalController.java` |
| `UC-AD-11` | Review, Approve, and Activate Checklist Versions | `POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/activate` | `ChecklistTemplateApprovalController.activateImportedVersion()` | `ChecklistTemplateApprovalService.activateImportedInLineage()` → `ChecklistTemplateRepository.findById()` | hasRole('SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ChecklistTemplateApprovalController.java` |
| `UC-AD-11` | Review, Approve, and Activate Checklist Versions | `POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/approve` | `ChecklistTemplateApprovalController.approveVersion()` | `ChecklistTemplateApprovalService.decideInLineage()` → `ChecklistTemplateRepository.findById()` | hasRole('SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ChecklistTemplateApprovalController.java` |
| `UC-AD-11` | Review, Approve, and Activate Checklist Versions | `POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/review` | `ChecklistTemplateApprovalController.reviewImportedVersion()` | `ChecklistTemplateApprovalService.reviewImportedInLineage()` → `ChecklistTemplateRepository.findById()` | hasRole('SYSTEM_ADMIN') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ChecklistTemplateApprovalController.java` |

## 3. Class Diagram

This design-level UML follows the current source declarations: fields become attributes, reachable methods become operations, `implements` becomes realization, repository inheritance becomes generalization, and runtime-only calls remain dependencies. A missing layer or ownership relation is not invented.

```plantuml
@startuml ClassDiagram_03ChecklistTemplateVersioningandRuntimeDistribution
skinparam classAttributeIconSize 0
hide empty members

class "ChecklistDetailPage" as UIChecklistDetailPage <<UI>>
class "ChecklistListPage" as UIChecklistListPage <<UI>>
class "AdminChecklistTemplateController" as ControllerAdminChecklistTemplateController <<Controller>> {
  - adminChecklistTemplateService: AdminChecklistTemplateService
  - checklistTemplateBatchImportService: ChecklistTemplateBatchImportService
  + list(status: ChecklistTemplateStatus, stage: ContentStage, keyword: String, page: int, size: int): ResponseEntity<ApiResponse<Page<AdminChecklistTemplateDetailResponse>>>
}
class "ChecklistTemplateApprovalController" as ControllerChecklistTemplateApprovalController <<Controller>> {
  - checklistTemplateApprovalService: ChecklistTemplateApprovalService
  + decide(id: UUID, request: ContentDecisionRequest, principal: Principal): ResponseEntity<ApiResponse<ChecklistTemplateDecisionResponse>>
}
interface "AdminChecklistTemplateService" as ServiceContractAdminChecklistTemplateService <<Service>> {
  + list(status: ChecklistTemplateStatus, stage: ContentStage, keyword: String, pageable: Pageable): Page<AdminChecklistTemplateDetailResponse>
}
class "AdminChecklistTemplateServiceImpl" as ServiceAdminChecklistTemplateServiceImpl <<Service>> {
  - checklistTemplateRepository: ChecklistTemplateRepository
  - checklistItemRepository: ChecklistItemRepository
  - contentMapper: ContentMapper
  - auditService: AuditService
  - auditLogRepository: AuditLogRepository
  - objectMapper: ObjectMapper
  + list(status: ChecklistTemplateStatus, stage: ContentStage, keyword: String, pageable: Pageable): Page<AdminChecklistTemplateDetailResponse>
}
ServiceContractAdminChecklistTemplateService <|.. ServiceAdminChecklistTemplateServiceImpl : implements
interface "ChecklistTemplateApprovalService" as ServiceContractChecklistTemplateApprovalService <<Service>> {
  + decide(id: UUID, request: ContentDecisionRequest, principal: Principal): ChecklistTemplateDecisionResponse
}
class "ChecklistTemplateApprovalServiceImpl" as ServiceChecklistTemplateApprovalServiceImpl <<Service>> {
  - checklistTemplateRepository: ChecklistTemplateRepository
  - contentReviewNotificationService: ContentReviewNotificationService
  - checklistItemRepository: ChecklistItemRepository
  - checklistInstanceRepository: ChecklistInstanceRepository
  - objectMapper: ObjectMapper
  + decide(id: UUID, request: ContentDecisionRequest, principal: Principal): ChecklistTemplateDecisionResponse
}
ServiceContractChecklistTemplateApprovalService <|.. ServiceChecklistTemplateApprovalServiceImpl : implements
interface "ChecklistTemplateRepository" as RepositoryChecklistTemplateRepository <<Repository>> {
  + findById(id: UUID): Optional<ChecklistTemplate>
}
class "ChecklistTemplate" as EntityChecklistTemplate <<Entity>> {
  - id: UUID
  - name: String
  - templateLineageId: UUID
  - templateVersionId: UUID
  - substageId: UUID
  - sequencePosition: Integer
  - recipientScope: ChecklistRecipientScope
  - eligibilityAnchorType: ChecklistAnchorType
}
interface "JpaRepository<ChecklistTemplate, UUID>" as RepositoryBaseChecklistTemplateRepository <<Framework>>
RepositoryBaseChecklistTemplateRepository <|-- RepositoryChecklistTemplateRepository : extends
class "PostgreSQL" as DB <<Database>>
UIChecklistDetailPage ..> ControllerChecklistTemplateApprovalController : invokes API
UIChecklistListPage ..> ControllerAdminChecklistTemplateController : invokes API
ControllerAdminChecklistTemplateController --> ServiceContractAdminChecklistTemplateService : delegates
ControllerChecklistTemplateApprovalController --> ServiceContractChecklistTemplateApprovalService : delegates
ServiceChecklistTemplateApprovalServiceImpl --> RepositoryChecklistTemplateRepository : reads / writes
RepositoryChecklistTemplateRepository ..> EntityChecklistTemplate : maps
RepositoryChecklistTemplateRepository ..> DB : persists
@enduml
```

**Figure 1 — Class Diagram: Checklist Template Versioning and Runtime Distribution**

## 4. Sequence Diagram — Main Flow

Each group is a representative reachable main flow. The full endpoint surface remains in the Function Design table above.

```plantuml
@startuml
skinparam shadowing false
skinparam maxMessageSize 90
title Checklist Template Versioning and Runtime Distribution — code-reachable representative flows

actor "Content Admin" as AContent_Admin
actor "System Admin" as ASystem_Admin
boundary "ChecklistListPage" as UIChecklistListPage <<boundary>>
boundary "ChecklistDetailPage" as UIChecklistDetailPage <<boundary>>
participant "JwtAuthenticationFilter" as JWT <<middleware>>
control "AdminChecklistTemplateController" as CAdminChecklistTemplateController <<control>>
control "ChecklistTemplateApprovalController" as CChecklistTemplateApprovalController <<control>>
participant "AdminChecklistTemplateService" as SAdminChecklistTemplateService <<service>>
participant "ChecklistTemplateApprovalService" as SChecklistTemplateApprovalService <<service>>
participant "ChecklistTemplateRepository" as RChecklistTemplateRepository <<repository>>
database "PostgreSQL" as DB

group UC-AD-10 — Author Checklist Templates [list()]
AContent_Admin -> UIChecklistListPage : 1. openChecklistTemplates()
activate UIChecklistListPage
alt [authorized request succeeds]
UIChecklistListPage -> JWT : 2a. GET /api/v1/admin/checklist-templates with bearer token
activate JWT
JWT -> CAdminChecklistTemplateController : 2a-1. list(status, stage, keyword, page, ...)
activate CAdminChecklistTemplateController
CAdminChecklistTemplateController -> SAdminChecklistTemplateService : 2a-2. list(status, stage, keyword, pageable)
activate SAdminChecklistTemplateService
SAdminChecklistTemplateService --> CAdminChecklistTemplateController : 2a-3. adminChecklistTemplateDetailResponsePage
deactivate SAdminChecklistTemplateService
CAdminChecklistTemplateController --> JWT : 2a-4. adminChecklistTemplateDetailResponse
deactivate CAdminChecklistTemplateController
JWT --> UIChecklistListPage : 2a-5. 200 OK — adminChecklistTemplateDetailResponse
deactivate JWT
UIChecklistListPage --> AContent_Admin : 2a-6. displayChecklistTemplates()
else [authentication or role authorization fails]
UIChecklistListPage -> JWT : 2b. GET /api/v1/admin/checklist-templates with invalid or insufficient bearer token
activate JWT
JWT --> UIChecklistListPage : 2b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UIChecklistListPage --> AContent_Admin : 2b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UIChecklistListPage
end

group UC-AD-11 — Review, Approve, and Activate Checklist Versions [decide()]
ASystem_Admin -> UIChecklistDetailPage : 3. submitChecklistVersionDecision(versionId, decision)
activate UIChecklistDetailPage
alt [authorized request succeeds]
UIChecklistDetailPage -> JWT : 4a. POST /api/v1/admin/checklist-templates/{id}/decision with bearer token
activate JWT
JWT -> CChecklistTemplateApprovalController : 4a-1. decide(id, request, principal)
activate CChecklistTemplateApprovalController
CChecklistTemplateApprovalController -> SChecklistTemplateApprovalService : 4a-2. decide(id, request, principal)
activate SChecklistTemplateApprovalService
SChecklistTemplateApprovalService -> RChecklistTemplateRepository : 4a-3. findById()
activate RChecklistTemplateRepository
RChecklistTemplateRepository -> DB : 4a-4. SELECT ChecklistTemplate via findById()
activate DB
DB --> RChecklistTemplateRepository : 4a-5. checklistTemplateQueryResult
deactivate DB
RChecklistTemplateRepository --> SChecklistTemplateApprovalService : 4a-6. checklistTemplateQueryResult
deactivate RChecklistTemplateRepository
SChecklistTemplateApprovalService --> CChecklistTemplateApprovalController : 4a-7. checklistTemplateDecisionResponse
deactivate SChecklistTemplateApprovalService
CChecklistTemplateApprovalController --> JWT : 4a-8. checklistTemplateDecisionResponse
deactivate CChecklistTemplateApprovalController
JWT --> UIChecklistDetailPage : 4a-9. 200 OK — checklistTemplateDecisionResponse
deactivate JWT
UIChecklistDetailPage --> ASystem_Admin : 4a-10. displayChecklistVersionDecision()
else [authentication or role authorization fails]
UIChecklistDetailPage -> JWT : 4b. POST /api/v1/admin/checklist-templates/{id}/decision with invalid or insufficient bearer token
activate JWT
JWT --> UIChecklistDetailPage : 4b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UIChecklistDetailPage --> ASystem_Admin : 4b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UIChecklistDetailPage
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

## 5. Business Rules Applied

| UC | Enforced business/security rules | Known boundary or gap |
| --- | --- | --- |
| `UC-AD-10` | Content Admin authors; approval/activation is a separate System Admin UC. Import/version/template validation is server authoritative. | No additional gap recorded in the code-first baseline. |
| `UC-AD-11` | System Admin approval is distinct from Content Admin authoring. Version transition and distribution idempotency are server authoritative. | No additional gap recorded in the code-first baseline. |

## 6. Partial / Excluded Boundaries

- Personal checklist execution and today tasks are owned by MF-02 and are referenced consumers, not duplicated UC ownership here.

## 7. Code and Test Evidence

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java`
- `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistListPage.tsx`
- `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistFormPage.tsx`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminChecklistTemplateControllerTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminChecklistTemplateServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/ChecklistImportControllerTest.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ChecklistTemplateApprovalController.java`
- `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistDetailPage.tsx`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/ChecklistTemplateApprovalServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/distribution/ChecklistApprovalDistributionAuditContractTest.java`
