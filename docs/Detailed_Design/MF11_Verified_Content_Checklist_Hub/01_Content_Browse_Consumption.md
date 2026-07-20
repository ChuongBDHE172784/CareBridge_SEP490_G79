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
participant "ContentServiceImpl" as Service
participant "ContentRepository" as ContentRepo
participant "ChecklistTemplateRepository" as ChecklistTemplateRepo
participant "ChecklistItemRepository" as ChecklistItemRepo
database "PostgreSQL" as DB

== UC-102 Browse Verified Content ==
U -> Controller : 1. GET /api/v1/content?type=ARTICLE&stage=PREGNANCY&topicId=&page=&size=
activate Controller
Controller -> Service : 2. getContents(filter, pageable)
activate Service
Service -> ContentRepo : 3. findByFilters(type, stage, topicId,\nstatus=APPROVED, pageable)
activate ContentRepo
ContentRepo -> DB : 4. SELECT * FROM content_items\nWHERE status='APPROVED' AND (type=? OR ?) AND (stage=? OR ?) ...
activate DB
DB --> ContentRepo : 5. page (items + totalElements)
deactivate DB
ContentRepo --> Service : 6. Page<ContentItem>
deactivate ContentRepo
Service -> Service : 7. map → ContentListResponse[] (ContentMapper)
Service --> Controller : 8. Page<ContentListResponse>
deactivate Service
Controller --> U : 9. HTTP 200 OK {items[], totalElements}
deactivate Controller

U -> Controller : 10. GET /api/v1/content/search?keyword=...&type=&stage=&topicId=\n[separate endpoint — /content DOES NOT accept keyword parameter]
activate Controller
Controller -> Controller : 11. validate keyword is required, maximum 100 characters (400 if violates)
Controller -> Service : 12. searchContent(request, pageable)
activate Service
Service -> Service : 13. sanitizeKeyword() [trim + escape LIKE characters % and _]
Service -> ContentRepo : 14. searchByFilters(sanitizedKeyword, type, stage,\ntopicId, status=APPROVED, pageable)
activate ContentRepo
ContentRepo -> DB : 15. SELECT * FROM content_items\nWHERE status='APPROVED' AND title/body ILIKE ? AND ...
activate DB
DB --> ContentRepo : 16. page
deactivate DB
ContentRepo --> Service : 17. Page<ContentItem>
deactivate ContentRepo
Service --> Controller : 18. Page<ContentSearchResponse>
deactivate Service
Controller --> U : 19. HTTP 200 OK {items[]}
deactivate Controller

U -> Controller : 20. GET /api/v1/content/checklists?stage=PREGNANCY
activate Controller
Controller -> Service : 21. getChecklists(stage)
activate Service
Service -> ChecklistTemplateRepo : 22. findByStage(stage) [or findAll() if stage=null]
activate ChecklistTemplateRepo
ChecklistTemplateRepo -> DB : 23. SELECT * FROM checklist_templates WHERE stage=?\n[DO NOT filter by status — see grounding notes]
activate DB
DB --> ChecklistTemplateRepo : 24. templates[]
deactivate DB
ChecklistTemplateRepo --> Service : 25. templates[]
deactivate ChecklistTemplateRepo
loop 26-29. for each ChecklistTemplate
  Service -> ChecklistItemRepo : 26. findByTemplate_IdOrderByOrder(templateId)
  activate ChecklistItemRepo
  ChecklistItemRepo -> DB : 27. SELECT * FROM checklist_items\nWHERE template_id=? ORDER BY "order"
  activate DB
  DB --> ChecklistItemRepo : 28. items[]
  deactivate DB
  ChecklistItemRepo --> Service : 29. items[]
  deactivate ChecklistItemRepo
end
Service --> Controller : 30. ChecklistTemplateResponse[]
deactivate Service
Controller --> U : 31. HTTP 200 OK {checklists[]}
deactivate Controller

== UC-103 View Verified Content Detail ==
U -> Controller : 32. GET /api/v1/content/{id}
activate Controller
Controller -> Service : 33. getContentById(id)
activate Service
Service -> ContentRepo : 34. findByIdAndStatus(id, APPROVED)
activate ContentRepo
ContentRepo -> DB : 35. SELECT * FROM content_items\nWHERE id=? AND status='APPROVED'
activate DB
alt 36. found APPROVED item
  DB --> ContentRepo : 36. item row
  deactivate DB
  ContentRepo --> Service : 37. ContentItem
  deactivate ContentRepo
  Service -> Service : 38. map → ContentDetailResponse\n(sources[], versionNo, publishedAt)
  Service --> Controller : 39. ContentDetailResponse
  deactivate Service
  Controller --> U : 40. HTTP 200 OK {content detail}
  deactivate Controller
else 36. none found (does not exist OR not APPROVED)
  DB --> ContentRepo : 36a. none
  deactivate DB
  ContentRepo --> Service : 36b. Optional.empty()
  deactivate ContentRepo
  Service --> Controller : 36c. throw ContentException.contentNotFound()\n(no distinction between "does not exist" and "not approved")
  deactivate Service
  Controller --> U : 36d. HTTP 404 Not Found
  deactivate Controller
end

@enduml
```

**Hình 2 — Sequence Diagram: Browse → Search (endpoint riêng) → Checklists → View Detail (Main Flow)**

> **Ghi chú grounding (quan trọng):** Service thật là `ContentServiceImpl` (không phải
> `ContentQueryServiceImpl`), repository thật là `ContentRepository` (không phải
> `ContentItemRepository` như class diagram mục 2 nêu). `GET /api/v1/content` **không nhận**
> tham số `keyword` — tìm kiếm theo từ khoá là một endpoint hoàn toàn riêng
> (`GET /api/v1/content/search`, bắt buộc `keyword`, tối đa 100 ký tự, có sanitize chống
> LIKE-injection). Đáng chú ý nhất: `getChecklists(stage)` **không lọc theo `status`** —
> khác với `getContents`/`searchContent`/`getContentById` (luôn hardcode `APPROVED`) — nghĩa
> là checklist ở trạng thái `DRAFT`/`PENDING_REVIEW`/`ARCHIVED` **vẫn có thể bị trả về** qua
> `GET /content/checklists`, lệch với khẳng định ở mục 5 ("chỉ nội dung APPROVED được liệt
> kê") và với State Machine mục 4. Cần xác nhận với đội phát triển đây là gap cần vá hay là
> hành vi có chủ đích (ví dụ checklist không qua pipeline duyệt như article/FAQ).

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
