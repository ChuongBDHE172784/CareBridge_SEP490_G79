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
participant "UploadedFileRepository" as FileRepo
participant "HealthRecordRepository" as RecordRepo
participant "HealthRecordFileRepository" as RecordFileRepo
participant "IStorageService" as Storage
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-83 Add Personal Health Record and Attachment ==
M -> Controller : 1. POST /api/v1/health-records\n{recordType=LAB_RESULT, title, recordDate, facilityName, fileIds[]}
activate Controller
Controller -> Service : 2. addHealthRecord(request, callerId)
activate Service
opt 3. fileIds is not empty
  Service -> FileRepo : 3. findAllByIdInAndOwnerUserIdAndStatus(fileIds, callerId, ACTIVE)
  activate FileRepo
  FileRepo -> DB : 4. SELECT * FROM uploaded_files\nWHERE id IN (...) AND owner_user_id=? AND status='ACTIVE'
  activate DB
  DB --> FileRepo : 5. ownedFiles[]
  deactivate DB
  FileRepo --> Service : 6. ownedFiles[]
  deactivate FileRepo
  Service -> Service : 7. if ownedFiles.size() != fileIds.size()\n→ throw 403 HEALTH-005 (file does not belong to caller)
end
Service -> RecordRepo : 8. save(HealthRecord{ownerUserId=callerId, status=ACTIVE by default})
activate RecordRepo
RecordRepo -> DB : 9. INSERT INTO health_records ...
activate DB
DB --> RecordRepo : 10. saved
deactivate DB
RecordRepo --> Service : 11. HealthRecord
deactivate RecordRepo
loop 12-15. for each fileId (displayOrder increases sequentially)
  Service -> RecordFileRepo : 12. save(HealthRecordFile{healthRecordId, fileId, displayOrder})
  activate RecordFileRepo
  RecordFileRepo -> DB : 13. INSERT INTO health_record_files ...
  activate DB
  DB --> RecordFileRepo : 14. saved
  deactivate DB
  RecordFileRepo --> Service : 15. HealthRecordFile
  deactivate RecordFileRepo
end
Service -> Audit : 16. log(HEALTH_RECORD_ADDED, callerId,\n"HealthRecord", recordId, "created")
activate Audit
Audit --> Service : 17. void
deactivate Audit
Service --> Controller : 18. AddHealthRecordResponse
deactivate Service
Controller --> M : 19. HTTP 201 Created
deactivate Controller

== UC-84 Update Health Record Metadata ==
M -> Controller : 20. PATCH /api/v1/health-records/{id}\n{title, recordType, sourceType, sourceName, ...}
activate Controller
Controller -> Service : 21. updateHealthRecord(id, request, ownerUserId)
activate Service
Service -> RecordRepo : 22. findById(id)
activate RecordRepo
RecordRepo -> DB : 23. SELECT * FROM health_records WHERE id=?
activate DB
DB --> RecordRepo : 24. record row
deactivate DB
RecordRepo --> Service : 25. HealthRecord
deactivate RecordRepo
Service -> Service : 26. check ownerUserId matches (403 if not)\n&& status != ARCHIVED (409 HEALTH-006 if already archived)
Service -> Service : 27. apply PATCH — only override non-null fields in request
Service -> RecordRepo : 28. save(record{...})
activate RecordRepo
RecordRepo -> DB : 29. UPDATE health_records\nSET title=?, record_type=?, source_type=?, updated_at=now()
activate DB
DB --> RecordRepo : 30. updated
deactivate DB
RecordRepo --> Service : 31. HealthRecord
deactivate RecordRepo
Service -> Audit : 32. log(HEALTH_RECORD_UPDATED, ownerUserId,\n"HealthRecord", id, "updated")
activate Audit
Audit --> Service : 33. void
deactivate Audit
Service --> Controller : 34. UpdateHealthRecordResponse
deactivate Service
Controller --> M : 35. HTTP 200 OK
deactivate Controller

== UC-85 Archive or Delete User-entered Health Record ==
M -> Controller : 36. PATCH /api/v1/health-records/{id}/archive
activate Controller
Controller -> Service : 37. archiveRecord(id, ownerUserId)
activate Service
Service -> RecordRepo : 38. findById(id)
activate RecordRepo
RecordRepo -> DB : 39. SELECT * FROM health_records WHERE id=?
activate DB
DB --> RecordRepo : 40. record row
deactivate DB
RecordRepo --> Service : 41. HealthRecord
deactivate RecordRepo
Service -> Service : 42. check ownerUserId matches (403 if not)
alt 43. status already ARCHIVED (idempotent)
  Service --> Controller : 43a. ArchiveHealthRecordResponse{status=ARCHIVED}\n(return early, DO NOT save(), DO NOT log audit)
  deactivate Service
  Controller --> M : 43b. HTTP 200 OK
  deactivate Controller
else 43. status is ACTIVE → change to ARCHIVED
  Service -> RecordRepo : 44. save(record{status=ARCHIVED})
  activate RecordRepo
  RecordRepo -> DB : 45. UPDATE health_records SET status='ARCHIVED'
  activate DB
  DB --> RecordRepo : 46. updated
  deactivate DB
  RecordRepo --> Service : 47. HealthRecord
  deactivate RecordRepo
  Service -> Audit : 48. log(HEALTH_RECORD_ARCHIVED, ownerUserId,\n"HealthRecord", id, "archived")
  activate Audit
  Audit --> Service : 49. void
  deactivate Audit
  Service --> Controller : 50. ArchiveHealthRecordResponse{status=ARCHIVED}
  deactivate Service
  Controller --> M : 51. HTTP 200 OK
  deactivate Controller
end

== UC-86 View Health Record Timeline and Detail ==
M -> Controller : 52. GET /api/v1/health-records/timeline?recordType=&journeyId=&babyId=&sourceType=&page=&size=
activate Controller
Controller -> Service : 53. getTimeline(ownerUserId, filter)
activate Service
Service -> RecordRepo : 54. findActiveByOwnerFiltered(ownerUserId, recordType,\njourneyId, babyId, sourceType, pageable)
activate RecordRepo
RecordRepo -> DB : 55. SELECT * FROM health_records\nWHERE owner_user_id=? AND status='ACTIVE' AND ... (dynamic filter)
activate DB
DB --> RecordRepo : 56. page (rows + totalElements)
deactivate DB
RecordRepo --> Service : 57. Page<HealthRecord>
deactivate RecordRepo
Service -> Service : 58. map → HealthRecordTimelineItem[]
Service --> Controller : 59. TimelineResponse{items[], totalElements, totalPages}
deactivate Service
Controller --> M : 60. HTTP 200 OK {timeline[]}
deactivate Controller

M -> Controller : 61. GET /api/v1/health-records/{recordId}
activate Controller
Controller -> Service : 62. getHealthRecord(recordId, callerId)
activate Service
Service -> RecordRepo : 63. findByIdAndStatus(recordId, ACTIVE)\n[record ARCHIVED → 404, even for owner]
activate RecordRepo
RecordRepo -> DB : 64. SELECT * FROM health_records\nWHERE id=? AND status='ACTIVE'
activate DB
DB --> RecordRepo : 65. record row | none
deactivate DB
RecordRepo --> Service : 66. HealthRecord
deactivate RecordRepo
Service -> Service : 67. check ownerUserId == callerId (403 if not)
Service -> RecordFileRepo : 68. findByHealthRecordIdOrderByDisplayOrderAsc(recordId)
activate RecordFileRepo
RecordFileRepo -> DB : 69. SELECT * FROM health_record_files\nWHERE health_record_id=? ORDER BY display_order
activate DB
DB --> RecordFileRepo : 70. links[]
deactivate DB
RecordFileRepo --> Service : 71. links[]
deactivate RecordFileRepo
loop 72-77. for each attachment link
  Service -> FileRepo : 72. findByIdAndStatus(link.fileId, ACTIVE)
  activate FileRepo
  FileRepo -> DB : 73. SELECT * FROM uploaded_files WHERE id=? AND status='ACTIVE'
  activate DB
  DB --> FileRepo : 74. file row | none (skip if file was deleted)
  deactivate DB
  FileRepo --> Service : 75. UploadedFile
  deactivate FileRepo
  Service -> Storage : 76. generatePresignedUrl(storageKey, ttlMinutes=15)
  activate Storage
  Storage --> Service : 77. presignedUrl (expires after 15 minutes)
  deactivate Storage
end
Service --> Controller : 78. HealthRecordDetailResponse{record, attachments[]}
deactivate Service
Controller --> M : 79. HTTP 200 OK {record, files[]}
deactivate Controller

@enduml
```

**Hình 2 — Sequence Diagram: Add Record → Update Metadata → Archive → View Timeline/Detail (Main Flow)**

> **Ghi chú grounding:** Tên method thật trên `IHealthRecordService`/`HealthRecordServiceImpl`
> là `addHealthRecord`/`updateHealthRecord`/`archiveRecord`/`getTimeline`/`getHealthRecord`
> (không phải `add`/`updateMetadata`/`archive`/`timeline`/`detail` như vẽ ở Class Diagram
> mục 2 — mang tính khái niệm). Hai chi tiết an toàn quan trọng không có trong bản vẽ cũ:
> (1) `addHealthRecord` xác thực **quyền sở hữu file** (`UploadedFileRepository`, status
> `ACTIVE`) trước khi liên kết `fileIds` vào record — từ chối 403 nếu bất kỳ file nào không
> thuộc caller; (2) `getHealthRecord` (xem chi tiết) sinh **presigned URL có TTL 15 phút**
> qua `IStorageService` cho từng tệp đính kèm thay vì trả `fileUrl` tĩnh, và chỉ trả về
> record đang `ACTIVE` — record đã `ARCHIVED` trả `404` ngay cả cho chính chủ sở hữu.
> `archiveRecord` cũng idempotent: gọi lại trên record đã `ARCHIVED` sẽ trả sớm, không
> `save()` lại và không phát sinh audit log trùng lặp.

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
