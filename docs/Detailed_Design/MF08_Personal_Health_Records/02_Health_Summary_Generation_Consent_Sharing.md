# MF-08 / Spec 02 — Health Summary Generation & Consent-Gated Sharing

| Field | Value |
| --- | --- |
| Feature | MF-08 — Personal Health Records & Source Labeling |
| Use Cases Covered | UC-87 Generate Health Summary, UC-88 Share Health Summary or Selected Records Under Consent |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother generates a period-based summary (24h / 7 days / consultation) from her own health records for personal review, then shares that summary with a recipient only when a valid, unexpired data-sharing permission for that recipient already exists — sharing is never implicit. |
| Grounding (source code) | `health/entity/HealthSummary.java`, `health/entity/DataPermission.java`, `health/controller/HealthSummaryController.java` (`/api/v1/health-summaries`), `health/service/impl/ShareSummaryServiceImpl.java` |

## 1. Tổng quan luồng chính (Main Flow Overview)

`HealthSummary` là bản tổng hợp do Mother tự khởi tạo từ các `HealthRecord`/`MaternalHealthMetric`/`PostpartumLog`
đã chọn trong một khoảng thời gian (`summaryPeriod` = `24H`/`7D`/`CONSULTATION`, UC-87).
Việc chia sẻ (UC-88) **luôn bị chặn bởi một `DataPermission` hợp lệ** (`existsValidPermission`
kiểm tra `granteeUserId` + `expiresAt` tại thời điểm chia sẻ) — không có đường nào share
trực tiếp mà bỏ qua permission.

**Ghi chú grounding (khác biệt SRS ↔ code hiện tại):** SRS mô tả UC-88 dùng cơ chế consent
chung của MF-01 (`ConsentGrant`, xem `MF01_Account_Trust_AccesControl/02_...`), nhưng
codebase hiện tại dùng một entity **riêng** `health.entity.DataPermission` (bảng
`data_permissions`, không liên kết trực tiếp với bảng `consent_grants`). Ngoài ra, hiện
tại chỉ tìm thấy **luồng đọc** (`existsValidPermission`) trong service layer — chưa có
endpoint tạo `DataPermission` trong `04_Implement`/backend hiện có; việc cấp quyền này
được cho là sẽ tái sử dụng cơ chế Grant Data Permission (UC-15, MF-01) khi triển khai đầy
đủ. Spec này trình bày đúng những gì code hiện có (gate kiểm tra), và đánh dấu rõ phần
"cần hoàn thiện" thay vì suy đoán một endpoint tạo quyền không có thật.

## 2. Class Diagram

```plantuml
@startuml MF08_02_SummarySharing_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class HealthSummary {
  + id: UUID
  + ownerUserId: UUID
  + journeyId: UUID
  + babyId: UUID
  + summaryPeriod: String
  + periodStart: LocalDate
  + periodEnd: LocalDate
  + summaryJson: String
  + generatedBy: String
  + status: String
}

class DataPermission {
  + id: UUID
  + ownerUserId: UUID
  + granteeUserId: UUID
  + status: String
  + expiresAt: Instant
}

note right of DataPermission
  Chưa có write-path (create/revoke) trong backend hiện tại —
  chỉ có existsValidPermission() (read-only gate). Xem ghi chú
  grounding ở mục 1.
end note

class GenerateHealthSummaryRequest {
  + journeyId: UUID
  + babyId: UUID
  + summaryPeriod: String
  + summaryJson: String
}

class ShareSummaryRequest {
  + summaryId: UUID
  + bookingId: UUID
}

class ShareSummaryResponse {
  + bookingId: UUID
  + summaryId: UUID
  + sharedAt: Instant
}

class HealthSummaryController {
  - healthSummaryService: IHealthSummaryService
  - shareSummaryService: IShareSummaryService
  + generate(GenerateHealthSummaryRequest): ResponseEntity
  + detail(summaryId): ResponseEntity
  + share(ShareSummaryRequest): ResponseEntity
}

interface IShareSummaryService <<interface>> {
  + shareSummary(request: ShareSummaryRequest, motherUserId: UUID): ShareSummaryResponse
}

class ShareSummaryServiceImpl implements IShareSummaryService {
  - summaryRepository: HealthSummaryRepository
  - permissionRepository: DataPermissionRepository
  - auditService: AuditService
}

HealthSummary "1" -- "0..*" ShareSummaryResponse : shared via
ShareSummaryServiceImpl --> DataPermission : existsValidPermission() gate
HealthSummaryController --> IShareSummaryService : uses
ShareSummaryServiceImpl --> AuditService : emits HEALTH_SUMMARY_GENERATED / SHARED

@enduml
```

**Hình 1 — Class Diagram: Health Summary & Data Permission Gate**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF08_02_SummarySharing_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "HealthSummaryController" as Controller
participant "HealthSummaryServiceImpl" as SummaryService
participant "ShareSummaryServiceImpl" as ShareService
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-87 Generate Health Summary ==
M -> Controller : POST /api/v1/health-summaries\n{summaryPeriod="7D", journeyId, summaryJson}
Controller -> SummaryService : generate(ownerId, request)
SummaryService -> DB : INSERT INTO health_summaries (status='ACTIVE')
SummaryService -> Audit : emit(HEALTH_SUMMARY_GENERATED)
SummaryService --> Controller : HealthSummary
Controller --> M : HTTP 201 Created

== UC-88 Share Health Summary Under Consent ==
M -> Controller : POST /api/v1/health-summaries/share\n{summaryId, bookingId}
Controller -> ShareService : shareSummary(request, motherUserId)
ShareService -> DB : SELECT * FROM health_summaries\nWHERE id=? AND owner_user_id=?
DB --> ShareService : summary
ShareService -> ShareService : Gate 1: summary thuộc về Mother

ShareService -> DB : SELECT * FROM data_permissions\nWHERE owner_user_id=? AND grantee_user_id=?\nAND status='ACTIVE' AND expires_at > now()
DB --> ShareService : hasPermission?

alt permission hợp lệ
  ShareService -> DB : UPDATE ... SET shared_summary_id=?\n(liên kết vào ngữ cảnh chia sẻ)
  ShareService -> Audit : emit(HEALTH_SUMMARY_SHARED)
  ShareService --> Controller : ShareSummaryResponse{sharedAt}
  Controller --> M : HTTP 200 OK
else không có permission hợp lệ
  ShareService -> ShareService : throw 403 "HEALTH-009: No valid data permission"
  ShareService --> Controller : Exception
  Controller --> M : HTTP 403 Forbidden
end

@enduml
```

**Hình 2 — Sequence Diagram: Generate Summary → Check Permission → Share (Main Flow)**

## 4. State Machine — `DataPermission.status` (gate cho việc chia sẻ)

```plantuml
@startuml MF08_02_DataPermission_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : Quyền chia sẻ được thiết lập\n[write-path tương đương UC-15/MF-01,\nchưa có endpoint riêng trong MF-08]

ACTIVE --> EXPIRED : now() > expiresAt\n[kiểm tra tại thời điểm share, UC-88]
ACTIVE --> REVOKED : Owner thu hồi quyền

EXPIRED --> [*]
REVOKED --> [*]

note right of ACTIVE
  Chỉ ACTIVE + chưa hết hạn mới cho phép UC-88 chia sẻ
  (existsValidPermission trong ShareSummaryServiceImpl).
  Đây là state machine suy ra từ cách gate được kiểm tra,
  không phải enum tường minh trong code (status là String).
end note

@enduml
```

**Hình 3 — State Machine: `DataPermission.status` (gate điều kiện chia sẻ)**

## 5. Business Rules Applied

- UC-88 — chia sẻ **chỉ** xảy ra qua một permission hợp lệ, có `scope`/`expiry`; không có đường tắt bỏ qua gate.
- BR-PRIVACY — `HealthSummary` là dữ liệu sức khỏe tổng hợp, chỉ chủ sở hữu tạo và kiểm soát việc chia sẻ.
- UC-87 — summary chỉ tổng hợp từ record do chính Mother chọn, không tự động bao gồm toàn bộ lịch sử.
- Khoảng cách hiện tại giữa SRS và code (không có write-path cho `DataPermission`) cần được System Admin/đội phát triển xác nhận trước khi coi UC-88 là "hoàn chỉnh" — xem ghi chú mục 1.
