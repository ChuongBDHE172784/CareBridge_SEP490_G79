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
participant "RedFlagRuleController" as RFController
participant "AuditService" as Audit
database "PostgreSQL" as DB
participant "IntakeServiceImpl (MF-06/01)" as Intake

== UC-75 Manage Approved AI Knowledge Sources ==
CA -> EvController : POST /admin/api/v1/evidence-sources\n{domain, organization, category, applicableStages}
EvController -> EvService : register(actorId, request)
EvService -> DB : INSERT INTO evidence_sources (status='PENDING_REVIEW')
EvService --> EvController : EvidenceSource
EvController --> CA : HTTP 201 Created

SA -> EvController : PATCH /admin/api/v1/evidence-sources/{id}/approve\n{notes}
EvController -> EvService : changeStatus(id, "APPROVED", notes, adminId, "REVIEWER")
EvService -> DB : UPDATE evidence_sources SET status='APPROVED'
EvService -> DB : INSERT INTO evidence_source_review_logs\n(previousStatus='PENDING_REVIEW', newStatus='APPROVED')
EvService -> Audit : emit(SECURITY_EVENT, "evidence_source_reviewed")
EvService --> EvController : EvidenceSource{status=APPROVED}
EvController --> SA : HTTP 200 OK

Intake -> EvController : GET /internal/api/v1/triage/evidence-sources/approved?stage=PREGNANCY\n(chỉ nội bộ, dùng khi retrieval)
EvController --> Intake : approvedSources[]

== UC-76 Configure AI Risk and Red-Flag Rules ==
SA -> RFController : POST /api/v1/admin/red-flag-rules\n{keyword, severity=RED, action=BLOCK}
RFController -> DB : INSERT INTO red_flag_rules (active=true)
RFController -> Audit : emit(RED_FLAG_RULE_CREATED)
RFController --> SA : HTTP 201 Created

SA -> RFController : PATCH /api/v1/admin/red-flag-rules/{id}\n{active=false}
RFController -> DB : UPDATE red_flag_rules SET active=false
RFController -> Audit : emit(RED_FLAG_RULE_UPDATED)
RFController --> SA : HTTP 200 OK

@enduml
```

**Hình 2 — Sequence Diagram: Register → Approve Evidence Source (consumed by Intake) → Configure Red-Flag Rule (Main Flow)**

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
