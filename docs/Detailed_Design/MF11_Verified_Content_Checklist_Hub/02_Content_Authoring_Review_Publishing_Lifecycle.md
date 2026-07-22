# MF-11 / Spec 02 — Content Authoring, Review & Publishing Lifecycle

| Field | Value |
| --- | --- |
| Feature | MF-11 — Verified Content & Checklist Hub |
| Use Cases Covered | UC-104 Create Verified Content, UC-105 Update Verified Content and Sources, UC-106 Review and Publish Content Version, UC-107 Unpublish or Archive Content, UC-108 Manage Content Categories and Stage/Topic Mapping |
| Primary Actor(s) | Content Admin, System Admin |
| Platform | Admin Portal |
| Main Flow Summary | A Content Admin drafts an article/FAQ/checklist mapped to a stage and topic, a System Admin (or Content Admin with review rights) reviews the version and approves or rejects it before it becomes publicly visible, and either admin can later unpublish/archive outdated or unsafe content. Category/topic taxonomy is managed through the same topic service that backs MF-04's community topics. |
| Grounding (source code) | `content/entity/ContentItem.java`, `ContentStatus.java`, `ContentDecision.java`, `content/controller/AdminContentController.java` (`/api/v1/admin/content`), `ContentApprovalController.java` (`/{id}/decision`), `ContentUnpublishController.java` (`/{id}/unpublish`), `ContentCategoryController.java` (`/api/v1/admin/content/categories`, reuses `CommunityTopicService`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`ContentItem.status` là trục chính của toàn bộ vòng đời quản trị nội dung:
`DRAFT` (Content Admin tạo — UC-104) → `PENDING_REVIEW` (gửi duyệt sau khi sửa nội dung/
nguồn — UC-105) → `APPROVED`/quay lại `DRAFT` (System Admin hoặc Content Admin có quyền
review quyết định qua `ContentDecision` APPROVE/REJECT — UC-106) → `ARCHIVED` (gỡ xuất
bản khi lỗi thời/không an toàn — UC-107). **Ghi chú grounding:** UC-108 (quản lý danh
mục/stage-topic mapping) trong code hiện tại **không có entity `ContentCategory` riêng**
— `ContentCategoryController` gọi thẳng `CommunityTopicService` (cùng bảng
`community_topics` mà MF-04 dùng cho chủ đề cộng đồng). Nghĩa là "danh mục nội dung" và
"chủ đề cộng đồng" hiện là **cùng một taxonomy dùng chung**, không phải hai khái niệm
tách biệt như tên UC gợi ý — spec này trình bày đúng thực tế đó thay vì vẽ một
`ContentCategory` không tồn tại.

## 2. Class Diagram

```plantuml
@startuml MF11_02_ContentLifecycle_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class ContentItem {
  + id: UUID
  + type: ContentType
  + title: String
  + body: String
  + stage: ContentStage
  + topicId: UUID
  + status: ContentStatus
  + versionNo: Integer
  + authorUserId: UUID
  + sourceLabel: String
  + sources: List<ContentSource>
  + publishedAt: Instant
}

enum ContentStatus {
  DRAFT
  PENDING_REVIEW
  APPROVED
  ARCHIVED
}

enum ContentDecision {
  APPROVE
  REJECT
}

class ContentSource {
  + title: String
  + url: String
  + publisher: String
}

class CommunityTopic <<dùng chung với MF-04>> {
  + id: UUID
  + name: String
  + isHidden: boolean
  + sortOrder: int
}

class AdminContentController {
  - contentAdminService: ContentAdminService
  + create(CreateContentRequest): ResponseEntity
  + update(id, UpdateContentRequest): ResponseEntity
  + archive(id): ResponseEntity
}

class ContentApprovalController {
  - contentApprovalService: ContentApprovalService
  + decide(id, ContentDecisionRequest): ResponseEntity
}

class ContentUnpublishController {
  + unpublish(id): ResponseEntity
}

class ContentCategoryController {
  - topicService: CommunityTopicService
  + listCategories(keyword, includeHidden): ResponseEntity
  + createCategory(request): ResponseEntity
  + updateCategory(id, request): ResponseEntity
}

interface ContentAdminService <<interface>> {
  + create(authorId: UUID, request): ContentItem
  + update(authorId: UUID, id: UUID, request): ContentItem
  + archive(actorId: UUID, id: UUID): ContentItem
}

class ContentAdminServiceImpl implements ContentAdminService {
  - contentItemRepository: ContentItemRepository
  - auditService: AuditService
}

interface ContentApprovalService <<interface>> {
  + decide(reviewerId: UUID, id: UUID, decision: ContentDecision, reason: String): ContentItem
}

class ContentApprovalServiceImpl implements ContentApprovalService {
  - contentItemRepository: ContentItemRepository
  - auditService: AuditService
}

ContentItem --> ContentStatus
ContentItem "1" *-- "0..*" ContentSource : cites
ContentItem "0..*" --> "1" CommunityTopic : topicId (UC-108 dùng chung taxonomy)
AdminContentController --> ContentAdminService : uses
ContentApprovalController --> ContentApprovalService : uses
ContentApprovalServiceImpl --> ContentDecision : applies
ContentUnpublishController --> ContentAdminService : uses (archive path)
ContentCategoryController --> CommunityTopicService : uses (không có ContentCategory riêng)

@enduml
```

**Hình 1 — Class Diagram: Content Item Lifecycle & Shared Topic Taxonomy**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF11_02_ContentLifecycle_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Content Admin" as CA
actor "System Admin" as SA
participant "AdminContentController" as AdminController
participant "AdminContentServiceImpl" as AdminService
participant "ContentRepository" as ContentRepo
participant "CommunityTopicRepository" as TopicRepo
participant "ContentApprovalController" as ApprovalController
participant "ContentApprovalServiceImpl" as ApprovalService
participant "ContentUnpublishController" as UnpublishController
participant "ContentUnpublishServiceImpl" as UnpublishService
participant "ContentCategoryController" as CategoryController
participant "CommunityTopicService" as TopicService
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-104 Create Verified Content ==
CA -> AdminController : 1. POST /api/v1/admin/content\n{type=ARTICLE, title, body, stage, topicId, sources[]}
activate AdminController
AdminController -> AdminService : 2. createContent(request, authorUserId)
activate AdminService
AdminService -> TopicRepo : 3. existsById(topicId) [if topicId != null]
activate TopicRepo
TopicRepo -> DB : 4. SELECT EXISTS(...) FROM community_topics WHERE id=?
activate DB
DB --> TopicRepo : 5. boolean (404 if topicId does not exist)
deactivate DB
TopicRepo --> AdminService : 6. boolean
deactivate TopicRepo
AdminService -> ContentRepo : 7. findByTitleIgnoreCaseAndStageAndType(title, stage, type)\n[prevent duplicate content]
activate ContentRepo
ContentRepo -> DB : 8. SELECT * FROM content_items\nWHERE LOWER(title)=? AND stage=? AND type=?
activate DB
DB --> ContentRepo : 9. existing | none (409 if already exists)
deactivate DB
ContentRepo --> AdminService : 10. Optional<ContentItem>
deactivate ContentRepo
AdminService -> ContentRepo : 11. save(ContentItem{status=DRAFT, versionNo=1, authorUserId})
activate ContentRepo
ContentRepo -> DB : 12. INSERT INTO content_items ...
activate DB
DB --> ContentRepo : 13. saved
deactivate DB
ContentRepo --> AdminService : 14. ContentItem
deactivate ContentRepo
AdminService -> Audit : 15. log(CONTENT_CREATED, authorUserId, "ContentItem", id, "created")
activate Audit
Audit --> AdminService : 16. void
deactivate Audit
AdminService --> AdminController : 17. CreateContentResponse{status=DRAFT}
deactivate AdminService
AdminController --> CA : 18. HTTP 201 Created
deactivate AdminController

== UC-105 Update Verified Content and Sources ==
CA -> AdminController : 19. PUT /api/v1/admin/content/{id}\n{title, body, stage, topicId, sources[], status=PENDING_REVIEW}
activate AdminController
AdminController -> AdminService : 20. updateContent(id, request, principal)
activate AdminService
AdminService -> ContentRepo : 21. findById(id)
activate ContentRepo
ContentRepo -> DB : 22. SELECT * FROM content_items WHERE id=?
activate DB
DB --> ContentRepo : 23. item row
deactivate DB
ContentRepo --> AdminService : 24. ContentItem
deactivate ContentRepo
alt 25. item.status AND request.status are both in {DRAFT, PENDING_REVIEW}\n(separation-of-duties — Content Admin CANNOT set APPROVED via this endpoint)
  AdminService -> ContentRepo : 25. findByTitleIgnoreCaseAndStageAndType(...)\n[only if title/stage changes]
  activate ContentRepo
  ContentRepo -> DB : 26. SELECT * FROM content_items\nWHERE LOWER(title)=? AND stage=? AND type=? AND id<>?
  activate DB
  DB --> ContentRepo : 27. different existing | none (409 if duplicate)
  deactivate DB
  ContentRepo --> AdminService : 28. Optional<ContentItem>
  deactivate ContentRepo
  AdminService -> AdminService : 29. assign editable fields (title/body/stage/topicId/\nstatus/sourceLabel/sources); versionNo += 1
  AdminService -> ContentRepo : 30. save(item{...})
  activate ContentRepo
  ContentRepo -> DB : 31. UPDATE content_items\nSET title=?, body=?, status=?, version_no=version_no+1, ...
  activate DB
  DB --> ContentRepo : 32. updated
  deactivate DB
  ContentRepo --> AdminService : 33. ContentItem
  deactivate ContentRepo
  AdminService -> Audit : 34. log(CONTENT_UPDATED, adminUserId, "ContentItem",\nid, "versionNo="+newVersionNo)
  activate Audit
  Audit --> AdminService : 35. void
  deactivate Audit
  AdminService --> AdminController : 36. UpdateContentResponse{status=PENDING_REVIEW, versionNo}
  deactivate AdminService
  AdminController --> CA : 37. HTTP 200 OK
  deactivate AdminController
else 25. item is APPROVED/ARCHIVED, or request.status is not DRAFT/PENDING_REVIEW
  AdminService --> AdminController : 25a. throw ContentException.invalidContentStatusTransition()\n[Content Admin cannot self-publish/edit APPROVED content via this endpoint]
  deactivate AdminService
  AdminController --> CA : 25b. HTTP 409/400
  deactivate AdminController
end

== UC-106 Review and Publish Content Version ==
SA -> ApprovalController : 38. POST /api/v1/admin/content/{id}/decision\n{decision=APPROVE, reason?}
activate ApprovalController
ApprovalController -> ApprovalService : 39. decide(id, request, principal)
activate ApprovalService
ApprovalService -> ContentRepo : 40. findById(id)
activate ContentRepo
ContentRepo -> DB : 41. SELECT * FROM content_items WHERE id=?
activate DB
DB --> ContentRepo : 42. item row
deactivate DB
ContentRepo --> ApprovalService : 43. ContentItem
deactivate ContentRepo
ApprovalService -> ApprovalService : 44. check status == PENDING_REVIEW\n(409 notPendingReview if otherwise)
alt 45. decision == APPROVE
  ApprovalService -> ApprovalService : 45. set status=APPROVED;\nif publishedAt not set yet → assign now()\n(keep old publishedAt if this is a re-approval)
else 45. decision == REJECT
  ApprovalService -> ApprovalService : 45a. check reason is required, not blank\n(400 decisionReasonRequired if missing); set status=DRAFT
end
ApprovalService -> ContentRepo : 46. save(item{status, publishedAt?})
activate ContentRepo
ContentRepo -> DB : 47. UPDATE content_items SET status=?, published_at=?
activate DB
DB --> ContentRepo : 48. updated
deactivate DB
ContentRepo --> ApprovalService : 49. ContentItem
deactivate ContentRepo
ApprovalService -> Audit : 50. log(CONTENT_DECIDED, adminUserId, "ContentItem",\nid, "decision="+decision+" versionNo="+versionNo)
activate Audit
Audit --> ApprovalService : 51. void
deactivate Audit
ApprovalService --> ApprovalController : 52. ContentDecisionResponse{previousStatus, newStatus}
deactivate ApprovalService
ApprovalController --> SA : 53. HTTP 200 OK
deactivate ApprovalController

== UC-107 Unpublish or Archive Content ==
CA -> UnpublishController : 54. POST /api/v1/admin/content/{id}/unpublish {reason}
activate UnpublishController
UnpublishController -> UnpublishController : 55. check reason is required (400 if blank)
UnpublishController -> UnpublishService : 56. unpublish(id, request, adminId)
activate UnpublishService
UnpublishService -> ContentRepo : 57. findById(id)
activate ContentRepo
ContentRepo -> DB : 58. SELECT * FROM content_items WHERE id=?
activate DB
DB --> ContentRepo : 59. item row
deactivate DB
ContentRepo --> UnpublishService : 60. ContentItem
deactivate ContentRepo
UnpublishService -> UnpublishService : 61. check status == APPROVED\n(409 notCurrentlyPublished if not)
UnpublishService -> ContentRepo : 62. save(item{status=ARCHIVED})
activate ContentRepo
ContentRepo -> DB : 63. UPDATE content_items SET status='ARCHIVED'
activate DB
DB --> ContentRepo : 64. updated
deactivate DB
ContentRepo --> UnpublishService : 65. ContentItem
deactivate ContentRepo
UnpublishService -> Audit : 66. log(CONTENT_UNPUBLISHED, adminId,\n"CONTENT_ITEM", id, "reason="+reason)
activate Audit
Audit --> UnpublishService : 67. void
deactivate Audit
UnpublishService --> UnpublishController : 68. UnpublishResponse{previousStatus=APPROVED, newStatus=ARCHIVED}
deactivate UnpublishService
UnpublishController --> CA : 69. HTTP 200 OK
deactivate UnpublishController

== UC-108 Manage Content Categories and Stage/Topic Mapping ==
CA -> CategoryController : 70. POST /api/v1/admin/content/categories\n{name, description}\n[essentially calls CommunityTopicService directly — shared table MF-04]
activate CategoryController
CategoryController -> TopicService : 71. createTopic(actorId, request)
activate TopicService
TopicService -> DB : 72. INSERT INTO community_topics ...
activate DB
DB --> TopicService : 73. saved
deactivate DB
TopicService --> CategoryController : 74. CommunityTopicResponse
deactivate TopicService
CategoryController -> Audit : 75. log(CONTENT_CATEGORY_MANAGED, actorId,\n"COMMUNITY_TOPIC", topicId, "action=CREATE")\n[audit logged directly from controller — not via separate service]
activate Audit
Audit --> CategoryController : 76. void
deactivate Audit
CategoryController --> CA : 77. HTTP 201 Created
deactivate CategoryController

@enduml
```

**Hình 2 — Sequence Diagram: Create Draft (dedup + topic check) → Update/Submit (guarded) → Review Decision → Unpublish → Manage Categories (Main Flow)**

> **Ghi chú grounding (quan trọng — sửa lại State Machine mục 4):**
> 1. `AdminContentServiceImpl.updateContent` (PUT, UC-105) **từ chối** nếu
>    `item.getStatus() == APPROVED` — cả trạng thái hiện tại của item lẫn `request.status()`
>    đều bắt buộc thuộc `{DRAFT, PENDING_REVIEW}`, nếu không sẽ `throw
>    invalidContentStatusTransition()`. Do đó transition `APPROVED --> PENDING_REVIEW` vẽ ở
>    State Machine mục 4 (note "Content Admin sửa nội dung đã xuất bản") **không đúng với
>    code hiện tại** — Content Admin không thể sửa trực tiếp một content đã `APPROVED` qua
>    endpoint này; cần một quyết định `REJECT` (UC-106) hoặc endpoint riêng chưa tồn tại để
>    đưa nó về `DRAFT`/`PENDING_REVIEW` trước.
> 2. UC-107 ("Unpublish or Archive") thực chất ánh xạ tới **hai endpoint riêng biệt** với
>    guard khác nhau: `ContentUnpublishController` (`POST /{id}/unpublish`, chỉ
>    `CONTENT_ADMIN`, yêu cầu `status` hiện tại phải đang `APPROVED`) và
>    `AdminContentController.hideContent` (`POST /{id}/archive`, cũng `CONTENT_ADMIN`, cho
>    phép archive từ **bất kỳ** status khác `ARCHIVED`, không riêng `APPROVED`). Sơ đồ trên
>    chỉ vẽ đường `/unpublish`; `/archive` có cùng shape nhưng thiếu guard "phải đang
>    APPROVED".
> 3. `ContentApprovalServiceImpl.decide` bắt buộc `reason` không rỗng khi `REJECT` (APPROVE
>    thì `reason` tuỳ chọn), và chỉ gán `publishedAt` nếu trước đó **chưa từng** có giá trị —
>    giữ nguyên ngày xuất bản gốc qua các lần duyệt lại sau này. Cả hai chi tiết này chưa
>    từng được vẽ ở bản trước.

## 4. State Machine — `ContentItem.status` (góc nhìn tác giả/quản trị)

```plantuml
@startuml MF11_02_ContentStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> DRAFT : Content Admin tạo bản nháp (UC-104)

DRAFT --> PENDING_REVIEW : Content Admin gửi duyệt (UC-105)
PENDING_REVIEW --> APPROVED : Reviewer quyết định APPROVE (UC-106)\n[publishedAt = now(), hiển thị công khai]
PENDING_REVIEW --> DRAFT : Reviewer quyết định REJECT (UC-106)\n[quay lại chỉnh sửa]

APPROVED --> ARCHIVED : Gỡ xuất bản/lưu trữ (UC-107 — /unpublish hoặc /archive)

ARCHIVED --> [*]

note right of APPROVED
  KHÔNG có transition APPROVED --> PENDING_REVIEW qua UC-105:
  AdminContentServiceImpl.updateContent() từ chối (throw
  invalidContentStatusTransition()) nếu item đang APPROVED —
  cả status hiện tại lẫn request.status() đều phải thuộc
  {DRAFT, PENDING_REVIEW}. Từ APPROVED, đường duy nhất trong
  code là sang ARCHIVED (UC-107); không có cách sửa nội dung
  đã xuất bản để tạo version mới qua endpoint hiện có.
end note

@enduml
```

**Hình 3 — State Machine: `ContentItem.status` — Full Authoring FSM**

## 5. Business Rules Applied

- BR-RBAC — tạo/sửa nội dung thuộc Content Admin; quyết định duyệt (APPROVE/REJECT) thuộc System Admin hoặc Content Admin có quyền review riêng (không phải người tự tạo nội dung đó, separation-of-duties).
- UC-106 — chỉ nội dung `PENDING_REVIEW` mới nhận quyết định; version đã `APPROVED` trước đó không bị ảnh hưởng cho tới khi version mới được duyệt.
- UC-107 — chỉ gỡ xuất bản/lưu trữ nội dung `APPROVED`; không xoá cứng để giữ lịch sử tham chiếu.
- UC-108 — danh mục/chủ đề nội dung dùng chung taxonomy với MF-04 (`CommunityTopic`), chỉ Content Admin quản lý được (`@PreAuthorize("hasRole('CONTENT_ADMIN')")`).
