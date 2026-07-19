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
participant "AdminContentController" as AdminController
actor "System Admin" as SA
participant "ContentApprovalController" as ApprovalController
participant "ContentUnpublishController" as UnpublishController
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-104 Create Verified Content ==
CA -> AdminController : POST /api/v1/admin/content\n{type=ARTICLE, title, body, stage, topicId, sources[]}
AdminController -> DB : INSERT INTO content_items (status=DRAFT, versionNo=1)
AdminController -> Audit : emit(CONTENT_CREATED)
AdminController --> CA : HTTP 201 Created

== UC-105 Update Verified Content and Sources ==
CA -> AdminController : PUT /api/v1/admin/content/{id}\n{body, sources[], status=PENDING_REVIEW}
AdminController -> DB : UPDATE content_items\nSET body=?, sources=?, status='PENDING_REVIEW', version_no=version_no+1
AdminController -> Audit : emit(CONTENT_UPDATED)
AdminController --> CA : HTTP 200 OK

== UC-106 Review and Publish Content Version ==
SA -> ApprovalController : POST /api/v1/admin/content/{id}/decision\n{decision=APPROVE}
ApprovalController -> DB : SELECT * FROM content_items WHERE id=?
DB --> ApprovalController : item{status=PENDING_REVIEW}
alt decision == APPROVE
  ApprovalController -> DB : UPDATE content_items\nSET status='APPROVED', published_at=now()
else decision == REJECT
  ApprovalController -> DB : UPDATE content_items SET status='DRAFT'
end
ApprovalController -> Audit : emit(CONTENT_DECIDED)
ApprovalController --> SA : HTTP 200 OK {status}

== UC-107 Unpublish or Archive Content ==
SA -> UnpublishController : POST /api/v1/admin/content/{id}/unpublish
UnpublishController -> DB : UPDATE content_items SET status='ARCHIVED'
UnpublishController -> Audit : emit(CONTENT_UNPUBLISHED)
UnpublishController --> SA : HTTP 200 OK

== UC-108 Manage Content Categories and Stage/Topic Mapping ==
CA -> ContentCategoryController : POST /api/v1/admin/content/categories\n{name, description}
ContentCategoryController -> DB : INSERT INTO community_topics (...)\n[bảng dùng chung với MF-04]
ContentCategoryController -> Audit : emit(CONTENT_CATEGORY_MANAGED)
ContentCategoryController --> CA : HTTP 201 Created

@enduml
```

**Hình 2 — Sequence Diagram: Create Draft → Update/Submit → Review Decision → Unpublish (Main Flow)**

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

APPROVED --> PENDING_REVIEW : Content Admin sửa nội dung đã xuất bản (UC-105)\n[tạo version mới, versionNo+1]
APPROVED --> ARCHIVED : Gỡ xuất bản/lưu trữ (UC-107)

ARCHIVED --> [*]

note right of APPROVED
  Khi APPROVED bị sửa lại (UC-105), phiên bản mới quay về
  PENDING_REVIEW — nội dung ĐANG xuất bản (versionNo cũ) vẫn
  hiển thị cho User tới khi version mới được duyệt, theo đúng
  ngữ nghĩa "Review and Publish Content VERSION" (UC-106).
end note

@enduml
```

**Hình 3 — State Machine: `ContentItem.status` — Full Authoring FSM**

## 5. Business Rules Applied

- BR-RBAC — tạo/sửa nội dung thuộc Content Admin; quyết định duyệt (APPROVE/REJECT) thuộc System Admin hoặc Content Admin có quyền review riêng (không phải người tự tạo nội dung đó, separation-of-duties).
- UC-106 — chỉ nội dung `PENDING_REVIEW` mới nhận quyết định; version đã `APPROVED` trước đó không bị ảnh hưởng cho tới khi version mới được duyệt.
- UC-107 — chỉ gỡ xuất bản/lưu trữ nội dung `APPROVED`; không xoá cứng để giữ lịch sử tham chiếu.
- UC-108 — danh mục/chủ đề nội dung dùng chung taxonomy với MF-04 (`CommunityTopic`), chỉ Content Admin quản lý được (`@PreAuthorize("hasRole('CONTENT_ADMIN')")`).
