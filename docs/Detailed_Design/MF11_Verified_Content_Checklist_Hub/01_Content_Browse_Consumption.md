# MF-11 / Spec 01 — Verified Content Browse & Consumption

| Field | Value |
| --- | --- |
| Feature | MF-11 — Verified Content & Checklist Hub |
| Use Cases Covered | UC-102 Browse Verified Content, UC-103 View Verified Content Detail |
| Primary Actor(s) | User (Mother / Family / any authenticated User) |
| Platform | Mobile App |
| Main Flow Summary | A User browses reviewed articles, FAQs and checklists with embedded keyword/stage/topic filters, then opens the detail view of a selected item showing its body, sources, review/version state and safety notes. Only content that has passed the authoring/review pipeline (spec 02) is ever visible here. |
| Grounding (source code) | `content/entity/ContentItem.java`, `ContentStatus.java`, `ContentType.java`, `ContentStage.java`, `ContentSource.java`, `content/controller/ContentController.java` (`/api/v1/content`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là mặt tiêu dùng (consumer-facing) của cùng entity `ContentItem` mà spec 02 quản trị
vòng đời tạo/duyệt/xuất bản. Luồng ở đây đơn giản và có chủ đích: User chỉ **nhìn thấy**
nội dung ở `status=APPROVED` (UC-102, filter từ khoá/giai đoạn/chủ đề là điều khiển nhúng
theo D-02), rồi mở chi tiết một item để xem đầy đủ nguồn (`ContentSource[]`), số phiên bản
(`versionNo`) và ngày cập nhật (UC-103). Checklist (`ChecklistTemplate`/`ChecklistItem`)
dùng chung `ContentStage`/`ContentStatus` nên hiển thị theo đúng cơ chế lọc như
article/FAQ, không cần luồng riêng.

## 2. Class Diagram

```plantuml
@startuml MF11_01_ContentBrowse_ClassDiagram
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
  + sourceLabel: String
  + sources: List<ContentSource>
  + publishedAt: Instant
}

enum ContentType {
  ARTICLE
  FAQ
  CHECKLIST
}

enum ContentStage {
  PRE_PREGNANCY
  PREGNANCY
  POSTPARTUM
  BABY_CARE
}

enum ContentStatus {
  DRAFT
  PENDING_REVIEW
  APPROVED
  ARCHIVED
}

class ContentSource {
  + title: String
  + url: String
  + publisher: String
}

class ChecklistTemplate {
  + id: UUID
  + name: String
  + stage: ContentStage
  + status: ContentStatus
}

class ChecklistItem {
  + id: UUID
  + template: ChecklistTemplate
  + itemText: String
  + order: Integer
  + isRequired: Boolean
}

class ContentController {
  - contentQueryService: ContentQueryService
  + browse(type, stage, topicId, keyword): ResponseEntity
  + search(keyword): ResponseEntity
  + checklists(stage): ResponseEntity
  + detail(id): ResponseEntity
}

interface ContentQueryService <<interface>> {
  + browse(filter): List<ContentItem>
  + detail(id: UUID): ContentItem
}

class ContentQueryServiceImpl implements ContentQueryService {
  - contentItemRepository: ContentItemRepository
  - checklistTemplateRepository: ChecklistTemplateRepository
}

ContentItem --> ContentType
ContentItem --> ContentStage
ContentItem --> ContentStatus
ContentItem "1" *-- "0..*" ContentSource : cites
ChecklistTemplate --> ContentStage
ChecklistTemplate --> ContentStatus
ChecklistTemplate "1" *-- "0..*" ChecklistItem : contains
ContentController --> ContentQueryService : uses
ContentQueryServiceImpl ..> ContentItem : chỉ trả status=APPROVED

@enduml
```

**Hình 1 — Class Diagram: Content Item, Source & Checklist Template (Consumer View)**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF11_01_ContentBrowse_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "User" as U
participant "ContentController" as Controller
participant "ContentQueryServiceImpl" as Service
database "PostgreSQL" as DB

== UC-102 Browse Verified Content ==
U -> Controller : GET /api/v1/content?type=ARTICLE&stage=PREGNANCY&keyword=
Controller -> Service : browse(filter)
Service -> DB : SELECT * FROM content_items\nWHERE status='APPROVED' AND stage=? AND type=?
DB --> Service : items[]
Service --> Controller : items[]
Controller --> U : HTTP 200 OK {items[]}

U -> Controller : GET /api/v1/content/checklists?stage=PREGNANCY
Controller -> Service : browse(filter{type=CHECKLIST})
Service -> DB : SELECT * FROM checklist_templates\nWHERE status='APPROVED' AND stage=?
DB --> Service : checklists[]
Service --> Controller : checklists[]
Controller --> U : HTTP 200 OK {checklists[]}

== UC-103 View Verified Content Detail ==
U -> Controller : GET /api/v1/content/{id}
Controller -> Service : detail(id)
Service -> DB : SELECT * FROM content_items WHERE id=?
DB --> Service : item{status}

alt status == APPROVED
  Service --> Controller : ContentItem{sources[], versionNo, publishedAt}
  Controller --> U : HTTP 200 OK {content detail}
else status != APPROVED
  Service -> Service : throw 404 (không lộ nội dung chưa duyệt)
  Service --> Controller : NotFoundException
  Controller --> U : HTTP 404 Not Found
end

@enduml
```

**Hình 2 — Sequence Diagram: Browse (Embedded Filter) → View Detail (Main Flow)**

## 4. State Machine — `ContentItem.status` (góc nhìn hiển thị cho User)

```plantuml
@startuml MF11_01_ContentVisibility_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

state "Không hiển thị với User" as Hidden {
  state DRAFT
  state PENDING_REVIEW
  state ARCHIVED
}

state "Hiển thị với User (UC-102/103)" as Visible {
  state APPROVED
}

[*] --> Hidden
Hidden --> Visible : Content Admin/System Admin duyệt và xuất bản\n(xem MF11/spec 02, UC-106)
Visible --> Hidden : Gỡ xuất bản/lưu trữ (MF11/spec 02, UC-107)

note bottom of Hidden
  4 trạng thái đều tồn tại thật trong ContentStatus, nhưng dưới
  góc nhìn UC-102/103, hệ thống chỉ phân biệt 2 vùng:
  APPROVED (hiển thị) và tất cả còn lại (ẩn, trả 404).
  Toàn bộ transition thật giữa 4 trạng thái được vẽ chi tiết
  ở MF11/spec 02 (góc nhìn tác giả/quản trị).
end note

@enduml
```

**Hình 3 — State Machine: `ContentItem.status` — Visibility Gate cho User**

## 5. Business Rules Applied

- UC-102 — chỉ nội dung `APPROVED` được liệt kê; filter từ khoá/giai đoạn/chủ đề là điều khiển nhúng (D-02), không tách UC riêng.
- UC-103 — nội dung chưa duyệt trả 404 thay vì 403, để không tiết lộ sự tồn tại của bản nháp cho User thường.
- Excluded (SRS 4.8) — nội dung không thay thế tư vấn y khoa chính thức; mỗi item giữ `sourceLabel`/`sources[]` để minh bạch nguồn gốc.
