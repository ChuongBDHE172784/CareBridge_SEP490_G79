# MF-04 / Spec 02 — Content Moderation & Enforcement Pipeline

| Field | Value |
| --- | --- |
| Feature | MF-04 — Community Q&A & Moderation |
| Use Cases Covered | UC-55 Report Unsafe Community Content or Account, UC-56 View Moderation Queue, UC-57 Moderate Community Content, UC-58 Resolve Content or Account Report and Apply Enforcement |
| Primary Actor(s) | User (reporter), Moderator, System Admin |
| Platform | Mobile App / Web Portal (report), Admin Portal (moderate/resolve) |
| Main Flow Summary | A User reports unsafe content or account behavior (or the system auto-flags it), a Moderator reviews the moderation queue and applies a content decision (approve/hide/lock/label/warn/suspend/restrict), then resolves the underlying report with the enforcement outcome — closing the loop from report to community-visible action. |
| Grounding (source code) | `content/entity/ContentReport.java`, `ReportStatus.java`, `ReportSource.java`, `ReportTargetType.java`, `content/entity/ModerationAction.java`, `ModerationActionType.java`, `content/controller/ReportController.java` (`/api/v1/reports`), `content/controller/ModerationController.java` (`/api/v1/admin/moderation`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là control an toàn trung tâm của MF-04 (BR-COMMUNITY-01, CC-01). `ContentReport`
được tạo bởi User (`reportSource=USER`, UC-55) hoặc tự động bởi hệ thống
(`reportSource=AUTOMATED`, ví dụ NS-04/NS-05 phát hiện nội dung nguy hiểm). Moderator
xem hàng đợi tổng hợp report + nội dung chờ duyệt trước xuất bản (UC-56), rồi ra quyết
định trên chính nội dung bị báo cáo (`ModerationAction` — APPROVE/HIDE/LOCK/
REQUEST_REVISION/LABEL/WARN/SUSPEND/RESTRICT/ESCALATE, UC-57). Cuối cùng, report được
đóng lại với kết quả xử lý (`ReportStatus`: RESOLVED hoặc DISMISSED, UC-58) — hành động
lên tài khoản (warn/suspend) nằm ngoài phạm vi phân quyền moderator có thể bị `ESCALATE`
lên System Admin. Quản lý topic (UC-59) là cấu hình phụ, không thuộc luồng thực thi
chính này.

## 2. Class Diagram

```plantuml
@startuml MF04_02_Moderation_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class ContentReport {
  + id: UUID
  + targetId: UUID
  + targetType: ReportTargetType
  + status: ReportStatus
  + category: String
  + reportSource: ReportSource
  + description: String
  + reporterUserId: UUID
  + assignedModeratorId: UUID
  + resolvedAt: Instant
}

enum ReportStatus {
  PENDING
  RESOLVED
  DISMISSED
}

enum ReportSource {
  USER
  AUTOMATED
}

enum ReportTargetType {
  QUESTION
  ANSWER
  CONTENT
  ACCOUNT
  EXPERT
  USER
}

class ModerationAction {
  + id: UUID
  + reportId: UUID
  + targetId: UUID
  + targetType: ReportTargetType
  + actionType: ModerationActionType
  + moderatorUserId: UUID
  + reason: String
  + actionAt: Instant
  + expiresAt: Instant
}

enum ModerationActionType {
  APPROVE
  HIDE
  LOCK
  REQUEST_REVISION
  LABEL
  WARN
  SUSPEND
  RESTRICT
  ESCALATE
  UNDO
}

class ReportController {
  - reportService: ReportService
  + submit(request): ResponseEntity
}

class ModerationController {
  - moderationService: ModerationService
  + queue(filter): ResponseEntity
  + applyAction(request): ResponseEntity
  + resolveReport(reportId, request): ResponseEntity
}

interface ModerationService <<interface>> {
  + queue(moderatorId: UUID, filter): List<QueueItem>
  + applyAction(moderatorId: UUID, request): ModerationAction
  + resolveReport(actorId: UUID, reportId: UUID, outcome): ContentReport
}

class ModerationServiceImpl implements ModerationService {
  - contentReportRepository: ContentReportRepository
  - moderationActionRepository: ModerationActionRepository
  - communityQuestionRepository: CommunityQuestionRepository
  - communityAnswerRepository: CommunityAnswerRepository
  - auditService: AuditService
}

ContentReport --> ReportStatus
ContentReport --> ReportSource
ContentReport --> ReportTargetType
ContentReport "1" *-- "0..*" ModerationAction : resolved by
ModerationAction --> ModerationActionType
ReportController --> ReportService : uses
ModerationController --> ModerationService : uses
ModerationServiceImpl --> AuditService : emits MODERATION_ACTION

@enduml
```

**Hình 1 — Class Diagram: Content Report & Moderation Action**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF04_02_Moderation_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "User (Reporter)" as U
actor "Moderator" as Mod
participant "CareBridge Community UI" as UI
participant "ReportController" as ReportController
participant "ModerationController" as ModController
participant "ReportServiceImpl" as ReportService
participant "ModerationServiceImpl" as Service
participant "AuditService" as Audit
participant "ContentReportRepository" as ReportRepo
participant "ModerationActionRepository" as ActionRepo
database "PostgreSQL" as DB

== UC-55 Report Unsafe Community Content or Account ==
U -> UI : 1. Submit unsafe-content report
activate UI
UI -> ReportController : 1a. POST /api/v1/reports\n{targetId, targetType=ANSWER, category, description}
activate ReportController
ReportController -> ReportService : 2. submit(reporterId, request)
activate ReportService
ReportService -> ReportRepo : 3. save(ContentReport{status=PENDING, reportSource=USER})
activate ReportRepo
ReportRepo -> DB : 4. INSERT INTO content_reports ...
activate DB
DB --> ReportRepo : 5. saved
deactivate DB
ReportRepo --> ReportService : 6. ContentReport
deactivate ReportRepo
ReportService -> Audit : 7. log(CONTENT_REPORTED)
activate Audit
Audit --> ReportService : 8. void
deactivate Audit
ReportService --> ReportController : 9. ContentReport
deactivate ReportService
ReportController --> UI : 10. HTTP 201 Created
deactivate ReportController
UI --> U : 10a. Display report confirmation
deactivate UI

== UC-56 View Moderation Queue ==
Mod -> UI : 11. Open moderation queue
activate UI
UI -> ModController : 11a. GET /api/v1/admin/moderation/queue
activate ModController
ModController -> Service : 12. queue(moderatorId, filter)
activate Service
Service -> ReportRepo : 13. findByStatus(PENDING)
activate ReportRepo
ReportRepo -> DB : 14. SELECT * FROM content_reports WHERE status='PENDING'\nUNION pending-publication content
activate DB
DB --> ReportRepo : 15. queueItems[]
deactivate DB
ReportRepo --> Service : 16. queueItems[]
deactivate ReportRepo
Service --> ModController : 17. queueItems[]
deactivate Service
ModController --> UI : 18. HTTP 200 OK {queueItems[]}
deactivate ModController
UI --> Mod : 18a. Display moderation queue
deactivate UI

== UC-57 Moderate Community Content ==
Mod -> UI : 19. Submit moderation action
activate UI
UI -> ModController : 19a. POST /api/v1/admin/moderation/actions\n{targetId, targetType=ANSWER, actionType=HIDE, reason}
activate ModController
ModController -> Service : 20. applyAction(moderatorId, request)
activate Service
Service -> DB : 21. UPDATE community_answers SET status='HIDDEN'\nWHERE id = targetId
activate DB
DB --> Service : 22. updated
deactivate DB
Service -> ActionRepo : 23. save(ModerationAction{actionType=HIDE, reason})
activate ActionRepo
ActionRepo -> DB : 24. INSERT INTO moderation_actions ...
activate DB
DB --> ActionRepo : 25. saved
deactivate DB
ActionRepo --> Service : 26. ModerationAction
deactivate ActionRepo
Service -> Audit : 27. log(MODERATION_ACTION)
activate Audit
Audit --> Service : 28. void
deactivate Audit
Service --> ModController : 29. ModerationAction
deactivate Service
ModController --> UI : 30. HTTP 201 Created
deactivate ModController
UI --> Mod : 30a. Display applied moderation action
deactivate UI

== UC-58 Resolve Content or Account Report and Apply Enforcement ==
Mod -> UI : 31. Resolve moderation report
activate UI
UI -> ModController : 31a. POST /api/v1/admin/moderation/reports/{reportId}/resolve\n{outcome=RESOLVED, linkedActionId}
activate ModController
ModController -> Service : 32. resolveReport(moderatorId, reportId, outcome)
activate Service
Service -> ReportRepo : 33. save(report{status=RESOLVED, resolvedAt=now()})
activate ReportRepo
ReportRepo -> DB : 34. UPDATE content_reports\nSET status='RESOLVED', resolved_at=now()
activate DB
DB --> ReportRepo : 35. updated
deactivate DB
ReportRepo --> Service : 36. ContentReport
deactivate ReportRepo
Service -> Audit : 37. log(MODERATION_ACTION, note="report_resolved")
activate Audit
Audit --> Service : 38. void
deactivate Audit
Service --> ModController : 39. ContentReport{status=RESOLVED}
deactivate Service
ModController --> UI : 40. HTTP 200 OK
deactivate ModController
UI --> Mod : 40a. Display resolved report status
deactivate UI

@enduml
```

**Hình 2 — Sequence Diagram: Report → Queue → Moderate Content → Resolve Report (Main Flow)**


## 4. Business Rules Applied

- BR-COMMUNITY-01 / CC-01 — mọi nội dung công khai chịu kiểm duyệt trước/sau xuất bản; bằng chứng kiểm duyệt được lưu giữ (`ModerationAction`, append-only theo `actionAt`).
- Separation-of-duties (UC-58) — hành động lên tài khoản (WARN/SUSPEND/RESTRICT) vượt phạm vi Moderator phải qua `ESCALATE` để System Admin xử lý.
- NS-04 — pipeline an toàn cộng đồng áp dụng kiểm tra trước/sau xuất bản, đưa nội dung nghi vấn vào hàng đợi và giữ lại bằng chứng kiểm duyệt.
- `UNDO` (ModerationActionType) chỉ được tạo bởi chính service xử lý hoàn tác, không lộ qua endpoint tạo action chung.
