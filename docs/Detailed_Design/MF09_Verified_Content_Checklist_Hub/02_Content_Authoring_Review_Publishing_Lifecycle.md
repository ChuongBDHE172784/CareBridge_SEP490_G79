# MF-09 / Spec 02 — Content & Checklist Template Authoring, Review and Publishing

| Field | Value |
| --- | --- |
| Feature | MF-09 — Verified Content & Checklist Hub |
| Use Cases Covered | Create/update/version/review/approve/archive content and checklist templates |
| Primary Actor(s) | Content Admin, System Admin/Reviewer |
| Platform | Web Admin Portal, CareBridge API |
| Main Flow Summary | Staff creates a draft with sources, submits or updates it, and an authorized reviewer approves/rejects the version. Only approved versions become consumer-visible/distributable. |
| Grounding (source code) | `AdminContentController`, `ContentApprovalController`, `ContentUnpublishController`, `AdminChecklistTemplateController`, `ChecklistTemplateApprovalController`, Web `features/contentManagement` |

## 1. Tổng quan luồng chính (Main Flow Overview)

Article/FAQ và checklist template có pipeline quản trị tương tự nhưng là hai aggregate riêng. Content dùng `ContentItem` và decision endpoint `/api/v1/admin/content/{id}/decision`. Checklist template có lineage/version, review/approve/activate endpoint dưới `/api/v1/admin/checklist-templates`. Archive không xóa lịch sử phiên bản. Template chỉ được phân phối khi phiên bản phù hợp đã approved/active và `distributionEnabled=true`.

## 2. Class Diagram

```plantuml
@startuml MF09_02_ContentLifecycle_ClassDiagram
skinparam classAttributeIconSize 0
class ContentItem { +id: UUID; +title: String; +body: String; +status: ContentStatus; +versionNo: Integer; +authorUserId: UUID; +publishedAt: Instant }
class ContentSource { +title: String; +url: String; +publisher: String }
class ChecklistTemplate { +id: UUID; +templateLineageId: UUID; +templateVersionId: UUID; +status: ChecklistTemplateStatus; +distributionEnabled: boolean; +versionNo: Integer }
class ChecklistItem { +id: UUID; +templateId: UUID; +title: String; +displayOrder: Integer; +required: boolean }
enum ContentDecision { APPROVE; REJECT }
class AdminContentController
class ContentApprovalController
class AdminChecklistTemplateController
class ChecklistTemplateApprovalController
class ContentAdminService
interface ContentRepository
interface ChecklistTemplateRepository
ContentItem "1" *-- "0..*" ContentSource
ChecklistTemplate "1" *-- "1..*" ChecklistItem
AdminContentController --> ContentAdminService
ContentApprovalController --> ContentAdminService
AdminChecklistTemplateController --> ContentAdminService
ChecklistTemplateApprovalController --> ContentAdminService
ContentAdminService --> ContentRepository
ContentAdminService --> ChecklistTemplateRepository
@enduml
```

**Hình 1 — Class Diagram: Authoring và review cho content/checklist template**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF09_02_ContentLifecycle_SequenceDiagram
actor "Content Admin" as A
actor "Reviewer" as R
participant "Admin Web UI" as UI
participant "AdminContentController" as AdminController
participant "ContentApprovalController" as ApprovalController
participant "ContentAdminService" as Service
participant "ContentRepository" as Repo
database "PostgreSQL" as DB

A -> UI : 1. Nhập draft và nguồn tham khảo
activate UI
UI -> AdminController : 2. POST /api/v1/admin/content
activate AdminController
AdminController -> Service : 3. createContent(request, authorId)
activate Service
Service -> Service : 3a. validate source, stage, topic and payload
activate Service
Service --> Service : 3a-1. normalized draft
deactivate Service
Service -> Repo : 4. save(ContentItem{DRAFT})
activate Repo
Repo -> DB : 5. INSERT content item, sources and version snapshot
activate DB
DB --> Repo : 6. persisted draft
deactivate DB
Repo --> Service : 7. ContentItem
deactivate Repo
Service --> AdminController : 8. CreateContentResponse
deactivate Service
AdminController --> UI : 9. 201 Created
deactivate AdminController
UI --> A : 10. Hiển thị draft
deactivate UI

R -> UI : 11. Chọn approve/reject
activate UI
UI -> ApprovalController : 12. POST /api/v1/admin/content/{id}/decision
activate ApprovalController
ApprovalController -> Service : 13. decide(id, decision, reviewerId)
activate Service
Service -> Repo : 14. find current version for update
activate Repo
Repo -> DB : 15. SELECT content item FOR UPDATE
activate DB
DB --> Repo : 16. item / empty
deactivate DB
Repo --> Service : 17. Optional<ContentItem>
deactivate Repo
alt [pending version hợp lệ và reviewer được phép]
  Service -> Repo : 18a. save(APPROVED hoặc DRAFT khi reject)
  activate Repo
  Repo -> DB : 18a-1. UPDATE item and append decision audit/version
  activate DB
  DB --> Repo : 18a-2. updated item
  deactivate DB
  Repo --> Service : 18a-3. ContentItem
  deactivate Repo
  Service --> ApprovalController : 18a-4. ContentDecisionResponse
  deactivate Service
  ApprovalController --> UI : 18a-5. 200 OK
  deactivate ApprovalController
else [không tồn tại, stale version hoặc sai quyền]
  Service --> ApprovalController : 18b. NotFound/Conflict/Access exception
  deactivate Service
  ApprovalController --> UI : 18b-1. 404 Not Found, 409 Conflict hoặc 403 Forbidden
  deactivate ApprovalController
end
UI --> R : 19. Hiển thị quyết định
deactivate UI
@enduml
```

**Hình 2 — Sequence Diagram: Tạo draft và quyết định review**

## 4. Business Rules Applied

- Tạo/sửa content và checklist template yêu cầu role quản trị tương ứng; review yêu cầu quyền reviewer.
- Approval áp dụng cho đúng version hiện hành; stale decision không được ghi đè phiên bản mới.
- Archive/unpublish giữ lịch sử và loại item khỏi consumer/distribution, không hard-delete aggregate.
- Checklist template version đã dùng để phân phối là snapshot tham chiếu; sửa nội dung tạo/clone version mới.
- `distributionEnabled` chỉ có hiệu lực cùng trạng thái review/activation hợp lệ.
