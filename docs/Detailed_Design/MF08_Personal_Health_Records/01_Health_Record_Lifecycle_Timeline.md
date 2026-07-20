# MF-08 / Spec 01 — Personal Health Record Lifecycle & Timeline

| Field | Value |
| --- | --- |
| Feature | MF-08 — Personal Health Records & Source Labeling |
| Use Cases Covered | UC-83 Add Personal Health Record and Attachment, UC-84 Update Health Record Metadata, UC-85 Archive or Delete User-entered Health Record, UC-86 View Health Record Timeline and Detail |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother adds a maternal or baby health record (ultrasound, lab result, prescription, examination result...) with an optional protected attachment and source label, may correct its metadata or archive/delete it later, and reviews all authorized records as a time-ordered, filterable timeline. |
| Grounding (source code) | `health/entity/HealthRecord.java`, `HealthRecordStatus.java`, `RecordType.java`, `DataSource.java`, `health/entity/HealthRecordFile.java`, `health/controller/HealthRecordController.java` (`/api/v1/health-records`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

`HealthRecord` là dữ liệu do người dùng tự nhập (`sourceType`/`sourceName` luôn được gắn
nhãn — BR-PRIVACY/UC-83), có thể gắn 0..n tệp đính kèm bảo vệ qua `HealthRecordFile`
(liên kết tới `file` module dùng chung, không lưu file trực tiếp trong entity này). Mother
có thể sửa siêu dữ liệu (tiêu đề, loại, ngày, nhãn nguồn — UC-84) mà **không đổi file gốc
đã upload trừ khi được phép**, và có thể lưu trữ (`ARCHIVED`) khi hồ sơ không còn theo dõi
tích cực (UC-85) — hệ thống dùng soft-status thay vì xoá cứng để giữ nghĩa vụ lưu trữ.
UC-86 (xem timeline) là read-model tổng hợp record theo thời gian/loại/nguồn với filter
nhúng sẵn (D-02) — không phải entity riêng.

## 2. Class Diagram

```plantuml
@startuml MF08_01_HealthRecord_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class HealthRecord {
  + id: UUID
  + ownerUserId: UUID
  + journeyId: UUID
  + babyId: UUID
  + recordType: RecordType
  + title: String
  + fileUrl: String
  + recordDate: LocalDate
  + sourceType: String
  + sourceName: String
  + status: HealthRecordStatus
}

enum RecordType {
  ULTRASOUND
  LAB_RESULT
  PRESCRIPTION
  VACCINATION_FORM
  EXAMINATION_RESULT
  NOTE
}

enum HealthRecordStatus {
  ACTIVE
  ARCHIVED
}

class HealthRecordFile {
  + id: UUID
  + healthRecordId: UUID
  + fileId: UUID
  + displayOrder: int
}

class HealthRecordTimelineItem <<read-model>> {
  + recordId: UUID
  + recordType: RecordType
  + recordDate: LocalDate
  + sourceLabel: String
}

class HealthRecordController {
  - healthRecordService: IHealthRecordService
  + add(AddHealthRecordRequest): ResponseEntity
  + updateMetadata(recordId, UpdateHealthRecordRequest): ResponseEntity
  + archive(recordId): ResponseEntity
  + timeline(TimelineFilter): ResponseEntity
  + detail(recordId): ResponseEntity
}

interface IHealthRecordService <<interface>> {
  + add(ownerId: UUID, request): HealthRecord
  + updateMetadata(ownerId: UUID, recordId: UUID, request): HealthRecord
  + archive(ownerId: UUID, recordId: UUID): HealthRecord
  + timeline(ownerId: UUID, filter): List<HealthRecordTimelineItem>
}

class HealthRecordServiceImpl implements IHealthRecordService {
  - healthRecordRepository: HealthRecordRepository
  - healthRecordFileRepository: HealthRecordFileRepository
  - auditService: AuditService
}

HealthRecord --> RecordType
HealthRecord --> HealthRecordStatus
HealthRecord "1" *-- "0..*" HealthRecordFile : has attachments
HealthRecordController --> IHealthRecordService : uses
HealthRecordServiceImpl ..> HealthRecordTimelineItem : builds
HealthRecordServiceImpl --> AuditService : emits HEALTH_RECORD_ADDED / UPDATED / ARCHIVED

@enduml
```

**Hình 1 — Class Diagram: Health Record, Attachment & Timeline Read-Model**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF08_01_HealthRecord_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "HealthRecordController" as Controller
participant "HealthRecordServiceImpl" as Service
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-83 Add Personal Health Record and Attachment ==
M -> Controller : POST /api/v1/health-records\n{recordType=LAB_RESULT, title, recordDate, sourceType, fileId}
Controller -> Service : add(ownerId, request)
Service -> DB : INSERT INTO health_records (status=ACTIVE)
Service -> DB : INSERT INTO health_record_files (fileId, displayOrder)
Service -> Audit : emit(HEALTH_RECORD_ADDED)
Service --> Controller : HealthRecord
Controller --> M : HTTP 201 Created

== UC-84 Update Health Record Metadata ==
M -> Controller : PATCH /api/v1/health-records/{id}\n{title, sourceType, note}
Controller -> Service : updateMetadata(ownerId, recordId, request)
Service -> Service : check ownership + status != ARCHIVED
Service -> DB : UPDATE health_records SET title=?, source_type=?, updated_at=now()
Service -> Audit : emit(HEALTH_RECORD_UPDATED)
Service --> Controller : HealthRecord
Controller --> M : HTTP 200 OK

== UC-85 Archive or Delete User-entered Health Record ==
M -> Controller : PATCH /api/v1/health-records/{id}/archive
Controller -> Service : archive(ownerId, recordId)
Service -> DB : UPDATE health_records SET status='ARCHIVED'
Service -> Audit : emit(HEALTH_RECORD_ARCHIVED)
Service --> Controller : HealthRecord{status=ARCHIVED}
Controller --> M : HTTP 200 OK

== UC-86 View Health Record Timeline and Detail ==
M -> Controller : GET /api/v1/health-records/timeline?recordType=&from=&to=
Controller -> Service : timeline(ownerId, filter)
Service -> DB : SELECT * FROM health_records\nWHERE owner_user_id=? ORDER BY record_date DESC
DB --> Service : records[]
Service --> Controller : HealthRecordTimelineItem[]
Controller --> M : HTTP 200 OK {timeline[]}

M -> Controller : GET /api/v1/health-records/{recordId}
Controller -> DB : SELECT * FROM health_records\nJOIN health_record_files WHERE id=?
DB --> Controller : record + files[]
Controller --> M : HTTP 200 OK {record, files[]}

@enduml
```

**Hình 2 — Sequence Diagram: Add Record → Update Metadata → Archive → View Timeline/Detail (Main Flow)**

## 4. State Machine — `HealthRecord.status`

```plantuml
@startuml MF08_01_HealthRecordStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> ACTIVE : Mother thêm hồ sơ (UC-83)

ACTIVE --> ACTIVE : Sửa siêu dữ liệu (UC-84)\n[không đổi status]
ACTIVE --> ARCHIVED : Mother lưu trữ/yêu cầu xoá (UC-85)\n[giữ lại theo nghĩa vụ retention]

ARCHIVED --> [*]

note right of ARCHIVED
  Chỉ 2 trạng thái đúng như HealthRecordStatus thật trong code —
  "delete" ở mức UI thực chất là chuyển sang ARCHIVED (soft-status),
  không có DELETED riêng và không hard-delete file gốc.
end note

@enduml
```

**Hình 3 — State Machine: `HealthRecord.status` Lifecycle**

## 5. Business Rules Applied

- BR-RBAC / BR-PRIVACY — chỉ chủ sở hữu (`ownerUserId`) truy cập được, trừ khi có `ConsentGrant`/`DataPermission` hợp lệ (MF-01, MF-08/spec-02).
- UC-84 — chỉ sửa siêu dữ liệu (title/category/date/source/tag/note); **không** thay đổi file gốc trừ khi được phép rõ ràng.
- UC-85 — archive/delete phải tuân thủ nghĩa vụ retention và không phá vỡ bằng chứng đang được chia sẻ hợp lệ.
- UC-86 — timeline chỉ hiển thị record được phép xem, filter (loại/nguồn/khoảng ngày) là điều khiển nhúng, không tách UC riêng (D-02).
