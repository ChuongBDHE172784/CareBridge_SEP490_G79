# MF-06 / Spec 02 — AI Knowledge Source & Red-Flag Rule Governance

| Field | Value |
| --- | --- |
| Feature | MF-06 — AI Nurse Assistant & Risk Triage |
| Use Cases Covered | UC-75 Manage Approved AI Knowledge Sources, UC-76 Configure AI Risk and Red-Flag Rules |
| Primary Actor(s) | Content Admin / System Admin |
| Platform | Admin Portal |
| Main Flow Summary | A Content Admin registers a candidate knowledge domain and a System Admin reviews it into an approved/rejected/deprecated state that gates what the AI intake pipeline (spec 01) may retrieve from; separately, a System Admin maintains conservative keyword-based red-flag rules with severity and fallback action, versioned and auditable. |
| Grounding (source code) | `triage/entity/EvidenceSource.java`, `EvidenceSourceReviewLog.java`, `triage/controller/EvidenceSourceAdminController.java` (`/admin/api/v1/evidence-sources`), `triage/controller/InternalEvidenceSourceController.java` (`/internal/api/v1/triage/evidence-sources/approved`), `triage/entity/RedFlagRule.java`, `RedFlagSeverity.java`, `RedFlagAction.java`, `triage/controller/RedFlagRuleController.java` (`/api/v1/admin/red-flag-rules`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là control quản trị đứng sau toàn bộ pipeline an toàn AI ở spec 01 (NS-05). Không
nguồn tri thức nào được AI truy xuất trừ khi `EvidenceSource.status=APPROVED` — mọi thay
đổi trạng thái được ghi lại thành `EvidenceSourceReviewLog` bất biến (`previousStatus →
newStatus`, ai đổi, khi nào). Song song, `RedFlagRule` là danh sách từ khoá/mẫu nhận diện
cấp bảo thủ (`severity` GREEN/YELLOW/RED + `action` BLOCK/WARN/ESCALATE) mà
`IntakeServiceImpl` (spec 01) áp dụng trước khi tin tưởng bất kỳ kết quả AI nào. Cả hai
đều versioned/audit theo yêu cầu NS-05 "log rule/source versions".

## 2. Class Diagram

```plantuml
@startuml MF06_02_AIGovernance_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class EvidenceSource {
  + id: UUID
  + domain: String
  + baseUrl: String
  + organization: String
  + category: String
  + status: String
  + discoveryMode: String
  + applicableStages: String
  + addedBy: UUID
  + reviewedBy: UUID
  + reviewedAt: Instant
  + notes: String
}

class EvidenceSourceReviewLog <<append-only>> {
  + id: UUID
  + evidenceSourceId: UUID
  + previousStatus: String
  + newStatus: String
  + actorUserId: UUID
  + actorRole: String
  + notes: String
  + changedAt: Instant
}

class RedFlagRule {
  + id: UUID
  + keyword: String
  + severity: RedFlagSeverity
  + action: RedFlagAction
  + active: boolean
  + systemDefault: boolean
  + createdBy: UUID
  + updatedBy: UUID
}

enum RedFlagSeverity {
  GREEN
  YELLOW
  RED
}

enum RedFlagAction {
  BLOCK
  WARN
  ESCALATE
}

class EvidenceSourceAdminController {
  - evidenceSourceService: EvidenceSourceService
  + register(request): ResponseEntity
  + approve(id, ReviewEvidenceSourceRequest): ResponseEntity
  + reject(id, ReviewEvidenceSourceRequest): ResponseEntity
  + deprecate(id, ReviewEvidenceSourceRequest): ResponseEntity
  + reviewLog(id): ResponseEntity
}

class InternalEvidenceSourceController {
  + approvedForStage(stage): ResponseEntity
}

class RedFlagRuleController {
  - redFlagRuleService: RedFlagRuleService
  + create(request): ResponseEntity
  + list(): ResponseEntity
  + update(id, request): ResponseEntity
  + delete(id): ResponseEntity
}

interface EvidenceSourceService <<interface>> {
  + register(actorId: UUID, request): EvidenceSource
  + changeStatus(id: UUID, newStatus: String, notes: String, actorId: UUID, actorRole: String): EvidenceSource
}

class EvidenceSourceServiceImpl implements EvidenceSourceService {
  - evidenceSourceRepository: EvidenceSourceRepository
  - evidenceSourceReviewLogRepository: EvidenceSourceReviewLogRepository
  - auditService: AuditService
}

EvidenceSource "1" *-- "0..*" EvidenceSourceReviewLog : audited by
RedFlagRule --> RedFlagSeverity
RedFlagRule --> RedFlagAction
EvidenceSourceAdminController --> EvidenceSourceService : uses
InternalEvidenceSourceController ..> EvidenceSource : reads (status=APPROVED only)
RedFlagRuleController --> RedFlagRuleService : uses
EvidenceSourceServiceImpl --> AuditService : emits RED_FLAG_RULE_* / evidence source review

@enduml
```

**Hình 1 — Class Diagram: Evidence Source Review Log & Red-Flag Rule**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF06_02_AIGovernance_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Content Admin" as CA
actor "System Admin" as SA
participant "EvidenceSourceAdminController" as EvController
participant "EvidenceSourceServiceImpl" as EvService
participant "EvidenceSourceRepository" as EvSourceRepo
participant "EvidenceSourceReviewLogRepository" as ReviewLogRepo
participant "InternalEvidenceSourceController" as InternalEvController
actor "AI Triage Service (external)" as AiSvc
participant "RedFlagRuleController" as RFController
participant "RedFlagRuleServiceImpl" as RFService
participant "RedFlagRuleRepository" as RFRepo
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-75 Manage Approved AI Knowledge Sources ==
CA -> EvController : 1. POST /admin/api/v1/evidence-sources\n{baseUrl, organization, category, applicableStages, notes}
activate EvController
EvController -> EvService : 2. propose(baseUrl, organization, category,\napplicableStages, notes, actorId)
activate EvService
EvService -> EvService : 3. parse HTTPS URL → derive domain;\nchặn nếu domain thuộc blocklist (facebook/tiktok/reddit/...)
EvService -> EvSourceRepo : 4. findByDomainIgnoreCase(domain)
activate EvSourceRepo
EvSourceRepo -> DB : 5. SELECT * FROM evidence_sources WHERE domain=?
activate DB
DB --> EvSourceRepo : 6. existing row | none
deactivate DB
EvSourceRepo --> EvService : 7. Optional<EvidenceSource>
deactivate EvSourceRepo
EvService -> EvSourceRepo : 8. save(source{status=PENDING_REVIEW,\ndiscoveryMode=MANUAL_ADMIN_ADD})
activate EvSourceRepo
EvSourceRepo -> DB : 9. INSERT/UPDATE evidence_sources ...
activate DB
DB --> EvSourceRepo : 10. saved
deactivate DB
EvSourceRepo --> EvService : 11. EvidenceSource
deactivate EvSourceRepo
EvService -> ReviewLogRepo : 12. save(EvidenceSourceReviewLog{previousStatus=null,\nnewStatus=PENDING_REVIEW, actorRole=PROPOSER})
activate ReviewLogRepo
ReviewLogRepo -> DB : 13. INSERT INTO evidence_source_review_logs ...
activate DB
DB --> ReviewLogRepo : 14. saved
deactivate DB
ReviewLogRepo --> EvService : 15. EvidenceSourceReviewLog
deactivate ReviewLogRepo
EvService --> EvController : 16. EvidenceSource{status=PENDING_REVIEW}
deactivate EvService
EvController --> CA : 17. HTTP 201 Created
deactivate EvController

SA -> EvController : 18. PATCH /admin/api/v1/evidence-sources/{id}/approve\n{notes}
activate EvController
EvController -> EvService : 19. changeStatus(id, "APPROVED", notes, adminId, "REVIEWER")
activate EvService
EvService -> EvSourceRepo : 20. findById(id)
activate EvSourceRepo
EvSourceRepo -> DB : 21. SELECT * FROM evidence_sources WHERE id=?
activate DB
DB --> EvSourceRepo : 22. source row
deactivate DB
EvSourceRepo --> EvService : 23. EvidenceSource
deactivate EvSourceRepo
EvService -> EvSourceRepo : 24. save(source{status=APPROVED, reviewedBy, reviewedAt})
activate EvSourceRepo
EvSourceRepo -> DB : 25. UPDATE evidence_sources\nSET status='APPROVED', reviewed_by=?, reviewed_at=?
activate DB
DB --> EvSourceRepo : 26. updated
deactivate DB
EvSourceRepo --> EvService : 27. EvidenceSource
deactivate EvSourceRepo
EvService -> ReviewLogRepo : 28. save(EvidenceSourceReviewLog{previousStatus=PENDING_REVIEW,\nnewStatus=APPROVED, actorRole=REVIEWER})
activate ReviewLogRepo
ReviewLogRepo -> DB : 29. INSERT INTO evidence_source_review_logs ...
activate DB
DB --> ReviewLogRepo : 30. saved
deactivate DB
ReviewLogRepo --> EvService : 31. EvidenceSourceReviewLog
deactivate ReviewLogRepo
EvService --> EvController : 32. EvidenceSource{status=APPROVED}
deactivate EvService
EvController --> SA : 33. HTTP 200 OK
deactivate EvController

AiSvc -> InternalEvController : 34. GET /internal/api/v1/triage/evidence-sources/approved?stage=PREGNANCY\nHeader: X-CareBridge-Internal-Key
activate InternalEvController
InternalEvController -> InternalEvController : 35. xác thực internalApiKey header (deployment secret,\nkhông cấp quyền admin cho AI service)
InternalEvController -> EvService : 36. approvedForStage(stage)
activate EvService
EvService -> EvSourceRepo : 37. findByStatus("APPROVED")
activate EvSourceRepo
EvSourceRepo -> DB : 38. SELECT * FROM evidence_sources WHERE status='APPROVED'
activate DB
DB --> EvSourceRepo : 39. rows[]
deactivate DB
EvSourceRepo --> EvService : 40. sources[]
deactivate EvSourceRepo
EvService -> EvService : 41. lọc theo applicableStages chứa stage
EvService --> InternalEvController : 42. approvedSources[]
deactivate EvService
InternalEvController --> AiSvc : 43. HTTP 200 OK {approvedSources[]}
deactivate InternalEvController

== UC-76 Configure AI Risk and Red-Flag Rules ==
SA -> RFController : 44. POST /api/v1/admin/red-flag-rules\n{keyword, severity=RED, action=BLOCK}
activate RFController
RFController -> RFService : 45. createRule(request, actorUserId)
activate RFService
RFService -> RFRepo : 46. existsByKeywordIgnoreCase(keyword)
activate RFRepo
RFRepo -> DB : 47. SELECT EXISTS(...) FROM red_flag_rules WHERE keyword=?
activate DB
DB --> RFRepo : 48. false
deactivate DB
RFRepo --> RFService : 49. boolean
deactivate RFRepo
RFService -> RFRepo : 50. save(RedFlagRule{active=true, systemDefault=false})
activate RFRepo
RFRepo -> DB : 51. INSERT INTO red_flag_rules ...
activate DB
DB --> RFRepo : 52. saved
deactivate DB
RFRepo --> RFService : 53. RedFlagRule
deactivate RFRepo
RFService -> Audit : 54. log(RED_FLAG_RULE_CREATED, actorUserId,\n"RedFlagRule", ruleId, details)
activate Audit
Audit --> RFService : 55. void
deactivate Audit
RFService --> RFController : 56. RedFlagRuleResponse
deactivate RFService
RFController --> SA : 57. HTTP 201 Created
deactivate RFController

SA -> RFController : 58. PATCH /api/v1/admin/red-flag-rules/{id}\n{active=false}
activate RFController
RFController -> RFService : 59. updateRule(id, request, actorUserId)
activate RFService
RFService -> RFRepo : 60. findById(id)
activate RFRepo
RFRepo -> DB : 61. SELECT * FROM red_flag_rules WHERE id=?
activate DB
DB --> RFRepo : 62. rule row
deactivate DB
RFRepo --> RFService : 63. RedFlagRule
deactivate RFRepo
RFService -> RFService : 64. kiểm tra rule.systemDefault && attemptsDeactivate\n(guard BR-SAFETY-RFR-003, chạy trước mọi thay đổi)
alt 64. không vi phạm guard → tiếp tục cập nhật
  RFService -> RFRepo : 65. save(rule{active=false, updatedBy})
  activate RFRepo
  RFRepo -> DB : 66. UPDATE red_flag_rules\nSET active=false, updated_by=?
  activate DB
  DB --> RFRepo : 67. updated
  deactivate DB
  RFRepo --> RFService : 68. RedFlagRule
  deactivate RFRepo
  RFService -> Audit : 69. log(RED_FLAG_RULE_UPDATED, actorUserId,\n"RedFlagRule", ruleId, details)
  activate Audit
  Audit --> RFService : 70. void
  deactivate Audit
  RFService --> RFController : 71. RedFlagRuleResponse
  deactivate RFService
  RFController --> SA : 72. HTTP 200 OK
  deactivate RFController
else 64. rule.systemDefault && attemptsDeactivate → chặn\n[BR-SAFETY-RFR-003: rule nền tảng không thể vô hiệu qua API]
  RFService --> RFController : 64a. throw RedFlagRuleException.systemDefaultProtected()
  deactivate RFService
  RFController --> SA : 64b. HTTP 409 Conflict
  deactivate RFController
end

@enduml
```

**Hình 2 — Sequence Diagram: Register → Approve Evidence Source (consumed by external AI service) → Configure Red-Flag Rule (Main Flow)**

> **Ghi chú grounding:** `EvidenceSourceServiceImpl` (`propose`/`changeStatus`/`approvedForStage`)
> **không** phụ thuộc `AuditService` — class diagram ở mục 2 vẽ cạnh `EvidenceSourceServiceImpl
> --> AuditService` mang tính khái niệm/SRS, không khớp code thật; mọi thay đổi trạng thái
> nguồn tri thức chỉ được ghi vết qua `EvidenceSourceReviewLog` (không phải audit log chung).
> Ngược lại, `RedFlagRuleServiceImpl` **có** gọi `auditService.log(...)` thật cho
> `RED_FLAG_RULE_CREATED`/`UPDATED`/`DELETED`. Endpoint đăng ký nguồn tri thức thực chất nhận
> `baseUrl` (không phải `domain` trực tiếp — domain được suy ra từ URL) và được bảo vệ bởi
> `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CONTENT_ADMIN')")` ở **toàn bộ**
> `EvidenceSourceAdminController` (kể cả approve/reject/deprecate — không giới hạn riêng cho
> System Admin như mô tả SRS). Endpoint `GET .../evidence-sources/approved` nằm ở
> `InternalEvidenceSourceController` riêng biệt (route `/internal/...`), được gọi bởi AI
> triage service ngoài (Python) qua header `X-CareBridge-Internal-Key`, **không phải** do
> `TriageService`/`IntakeController` (MF-06/01) gọi trực tiếp trong tiến trình Java.

## 4. State Machine — `EvidenceSource.status`

```plantuml
@startuml MF06_02_EvidenceSourceStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> PENDING_REVIEW : Content Admin đăng ký nguồn (UC-75)

PENDING_REVIEW --> APPROVED : System Admin duyệt\n[AI được phép truy xuất]
PENDING_REVIEW --> ARCHIVED : System Admin từ chối\n[không đủ tin cậy]
APPROVED --> DEPRECATED : System Admin ngừng dùng\n[nguồn lỗi thời/không còn phù hợp]
DEPRECATED --> PENDING_REVIEW : Đăng ký lại phiên bản mới để rà soát

APPROVED --> [*]
ARCHIVED --> [*]
DEPRECATED --> [*]

note right of APPROVED
  Mỗi lần đổi trạng thái đều tạo một EvidenceSourceReviewLog
  (previousStatus → newStatus, actorUserId, actorRole) —
  bất biến, phục vụ NS-05 "log source versions".
end note

@enduml
```

**Hình 3 — State Machine: `EvidenceSource.status` Lifecycle**

## 5. Business Rules Applied

- NS-05 — AI chỉ truy xuất nguồn có `status=APPROVED`; mọi thay đổi rule/nguồn được version hoá và ghi log.
- BR-RBAC — đăng ký nguồn thuộc Content Admin; duyệt/từ chối/deprecate và cấu hình red-flag rule thuộc System Admin.
- `systemDefault=true` trên `RedFlagRule` đánh dấu rule nền tảng không được xoá qua API thông thường (chỉ có thể `active=false`).
- UC-76 — rule mang tính "bảo thủ" (conservative): khi nghi ngờ, `action` thiên về `WARN`/`ESCALATE`/`BLOCK` hơn là bỏ qua.
