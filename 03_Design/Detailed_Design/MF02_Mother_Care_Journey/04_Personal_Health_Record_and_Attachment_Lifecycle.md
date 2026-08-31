# MF-02 — Personal Health Record and Attachment Lifecycle

| Field | Value |
| --- | --- |
| Major Feature | **MF-02 — Mother Care Journey** |
| Function package | **Personal Health Record and Attachment Lifecycle** |
| Code-first use cases | `UC-MH-12` |
| Status | **Draft** |
| Baseline | Current reachable code and tests, 2026-08-23 |
| Diagram convention | `../sequence-diagram-skill.md`; explicit lifeline order, numbered messages, balanced activation |

## 1. Tổng quan luồng chính

Design owned health records and purpose-bound attachment access.

- **UC-MH-12 — Manage Health Records and Attachments:** Create, view, edit, archive, and list maternal health records and access purpose-authorized attachments.

The code is authoritative for routes, handlers, delegation, authorization, and persisted state. Report1 section 6.2 is authoritative for the Major Feature name. Historical UC numbering and obsolete triage plans are not design inputs.

## 2. Function Design — Code Traceability

| UC | Actor goal | Method / route | Exact handler | Delegated function | Authorization | Source |
| --- | --- | --- | --- | --- | --- | --- |
| `UC-MH-12` | Manage Health Records and Attachments | `POST /api/v1/files` | `FileController.uploadFile()` | `IFileService.uploadFile()` → `UploadedFileRepository.countByOwnerUserIdAndStatus()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/controller/FileController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `POST /api/v1/files/health-records` | `FileController.uploadHealthRecordFile()` | `IFileService.uploadHealthRecordFile()` → `UploadedFileRepository.countByOwnerUserIdAndStatus()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/controller/FileController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `POST /api/v1/files/upload/with-purpose` | `FileController.uploadFileWithPurpose()` | `IFileService.uploadWithPurpose()` → `UploadedFileRepository.countByOwnerUserIdAndStatus()` | hasAnyRole('EXPERT', 'ADMIN', 'SYSTEM_ADMIN', 'MODERATOR', 'CONTENT_ADMIN', 'MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/controller/FileController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `DELETE /api/v1/files/{fileId}` | `FileController.deleteFile()` | `IFileService.deleteFile()` → `UploadedFileRepository.findByIdAndStatus()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/controller/FileController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `GET /api/v1/files/{fileId}` | `FileController.viewFile()` | `IFileService.viewFile()` → `UploadedFileRepository.findByIdAndStatus()` | isAuthenticated() | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/controller/FileController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `GET /api/v1/health-records` | `HealthRecordController.getTimeline()` | `IHealthRecordService.getTimeline()` → `HealthRecordRepository.findActiveByOwnerFiltered()` | hasAnyRole('MOTHER', 'FAMILY') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `POST /api/v1/health-records` | `HealthRecordController.addHealthRecord()` | `IHealthRecordService.addHealthRecord()` → `HealthRecordRepository.saveAndFlush()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `GET /api/v1/health-records/timeline` | `HealthRecordController.getTimeline()` | `IHealthRecordService.getTimeline()` → `HealthRecordRepository.findActiveByOwnerFiltered()` | hasAnyRole('MOTHER', 'FAMILY') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `PATCH /api/v1/health-records/{id}` | `HealthRecordController.updateHealthRecord()` | `IHealthRecordService.updateHealthRecord()` → `HealthRecordRepository.findById()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `PATCH /api/v1/health-records/{id}/archive` | `HealthRecordController.archiveHealthRecord()` | `IHealthRecordService.archiveRecord()` → `HealthRecordRepository.findById()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java` |
| `UC-MH-12` | Manage Health Records and Attachments | `GET /api/v1/health-records/{recordId}` | `HealthRecordController.getHealthRecord()` | `IHealthRecordService.getHealthRecord()` → `HealthRecordRepository.findByIdAndStatus()` | isAuthenticated() | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java` |

## 3. Class Diagram

This design-level UML follows the current source declarations: fields become attributes, reachable methods become operations, `implements` becomes realization, repository inheritance becomes generalization, and runtime-only calls remain dependencies. A missing layer or ownership relation is not invented.

```plantuml
@startuml ClassDiagram_04PersonalHealthRecordandAttachmentLifecycle
skinparam classAttributeIconSize 0
hide empty members

class "HealthRecordTimelineScreen" as UIHealthRecordTimelineScreen <<UI>>
class "FileController" as ControllerFileController <<Controller>> {
  - fileService: IFileService
  + uploadFile(file: MultipartFile, principal: Principal): ResponseEntity<ApiResponse<UploadFileResponse>>
}
interface "IFileService" as ServiceContractIFileService <<Service>> {
  + uploadFile(file: MultipartFile, callerId: UUID): UploadFileResponse
}
class "FileServiceImpl" as ServiceFileServiceImpl <<Service>> {
  - fileRepository: UploadedFileRepository
  - cloudinaryStorageService: CloudinaryStorageService
  - r2StorageService: ObjectProvider<R2StorageService>
  - auditService: AuditService
  - fileAccessPolicy: FileAccessPolicy
  - fileDeletePolicy: FileDeletePolicy
  + uploadFile(file: MultipartFile, callerId: UUID): UploadFileResponse
}
ServiceContractIFileService <|.. ServiceFileServiceImpl : implements
interface "UploadedFileRepository" as RepositoryUploadedFileRepository <<Repository>> {
  + countByOwnerUserIdAndStatus(ownerUserId: UUID, status: FileStatus): long
}
class "UploadedFile" as EntityUploadedFile <<Entity>> {
  - id: UUID
  - ownerUserId: UUID
  - uploaderRole: String
  - storageKey: String
  - storageProvider: String
  - kind: FileKind
  - purpose: FilePurpose
  - accessMode: FileAccessMode
}
interface "JpaRepository<UploadedFile, UUID>" as RepositoryBaseUploadedFileRepository <<Framework>>
RepositoryBaseUploadedFileRepository <|-- RepositoryUploadedFileRepository : extends
class "PostgreSQL" as DB <<Database>>
UIHealthRecordTimelineScreen ..> ControllerFileController : invokes API
ControllerFileController --> ServiceContractIFileService : delegates
ServiceFileServiceImpl --> RepositoryUploadedFileRepository : reads / writes
RepositoryUploadedFileRepository ..> EntityUploadedFile : maps
RepositoryUploadedFileRepository ..> DB : persists
@enduml
```

**Figure 1 — Class Diagram: Personal Health Record and Attachment Lifecycle**

## 4. Sequence Diagram — Main Flow

Each group is a representative reachable main flow. The full endpoint surface remains in the Function Design table above.

```plantuml
@startuml
skinparam shadowing false
skinparam maxMessageSize 90
title Personal Health Record and Attachment Lifecycle — code-reachable representative flows

actor "Mother" as AMother
boundary "HealthRecordTimelineScreen" as UIHealthRecordTimelineScreen <<boundary>>
participant "JwtAuthenticationFilter" as JWT <<middleware>>
control "FileController" as CFileController <<control>>
participant "IFileService" as SIFileService <<service>>
participant "UploadedFileRepository" as RUploadedFileRepository <<repository>>
database "PostgreSQL" as DB

group UC-MH-12 — Manage Health Records and Attachments [uploadFile()]
AMother -> UIHealthRecordTimelineScreen : 1. selectAndUploadHealthRecord()
activate UIHealthRecordTimelineScreen
alt [authorized request succeeds]
UIHealthRecordTimelineScreen -> JWT : 2a. POST /api/v1/files with bearer token
activate JWT
JWT -> CFileController : 2a-1. uploadFile(file, principal)
activate CFileController
CFileController -> SIFileService : 2a-2. uploadFile(file, callerId)
activate SIFileService
SIFileService -> RUploadedFileRepository : 2a-3. countByOwnerUserIdAndStatus(ownerUserId, status)
activate RUploadedFileRepository
RUploadedFileRepository -> DB : 2a-4. SELECT UploadedFile via countByOwnerUserIdAndStatus()
activate DB
DB --> RUploadedFileRepository : 2a-5. uploadedFileQueryResult
deactivate DB
RUploadedFileRepository --> SIFileService : 2a-6. affectedCount
deactivate RUploadedFileRepository
SIFileService --> CFileController : 2a-7. uploadFileResponse
deactivate SIFileService
CFileController --> JWT : 2a-8. uploadFileResponse
deactivate CFileController
JWT --> UIHealthRecordTimelineScreen : 2a-9. 201 Created — uploadFileResponse
deactivate JWT
UIHealthRecordTimelineScreen --> AMother : 2a-10. displayHealthRecordAttachment()
else [authentication or role authorization fails]
UIHealthRecordTimelineScreen -> JWT : 2b. POST /api/v1/files with invalid or insufficient bearer token
activate JWT
JWT --> UIHealthRecordTimelineScreen : 2b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UIHealthRecordTimelineScreen --> AMother : 2b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UIHealthRecordTimelineScreen
end
@enduml
```

**Brief Explanation:**

1. The actor starts each grouped use case through the code-reachable UI boundary.
2. Protected requests pass through JwtAuthenticationFilter; rejected credentials or roles return 401 Unauthorized or 403 Forbidden without invoking the controller.
3. The controller receives the request and invokes the exact delegated operation resolved from the current source.
4. The service applies the business policy and coordinates downstream collaborators while its caller remains active.
5. The repository executes the represented persistence operation and returns the stored or queried result before its activation ends.
6. The HTTP response unwinds through middleware when present, and the UI renders the server-authoritative outcome to the actor.

## 5. State Chart Diagram

The lifecycle below belongs to **HealthRecord.status, with the purpose-bound FileStatus of its attachments nested inside an active record**. Its states are the persisted constants declared in the sources cited under this diagram, and every transition, guard, and action is taken from the service method that writes that constant. No unreachable state is introduced.

```plantuml
@startuml StateChartDiagram_04PersonalHealthRecordandAttachmentLifecycle
hide empty description
[*] --> NoRecord

NoRecord --> ActiveRecord : createHealthRecord()\n[caller owns the subject]\n/ persistRecord(ACTIVE)
ActiveRecord --> ActiveRecord : updateHealthRecord()\n[status != ARCHIVED]\n/ persistRevision()
ActiveRecord --> ArchivedRecord : archiveHealthRecord()\n[status != ARCHIVED]\n/ setStatus(ARCHIVED)

state ActiveRecord {
  [*] --> NoAttachment
  NoAttachment --> AttachmentActive : uploadAttachment()\n[purpose allowed for record type]\n/ persistFile(ACTIVE)
  AttachmentActive --> AttachmentActive : readAttachment()\n[FileAccessPolicy grants purpose-bound access]\n/ streamFile()
  AttachmentActive --> AttachmentDeleted : deleteAttachment()\n[caller owns the file]\n/ setFileStatus(DELETED)
}

ActiveRecord : HealthRecordStatus = ACTIVE
ArchivedRecord : HealthRecordStatus = ARCHIVED
AttachmentActive : FileStatus = ACTIVE
AttachmentDeleted : FileStatus = DELETED
@enduml
```

**Figure 2 — State Chart Diagram: Personal Health Record and Attachment Lifecycle**

**Brief Explanation:**

1. A record starts in `NoRecord`; creation is guarded on the caller owning the subject of the record.
2. The event `updateHealthRecord()` is a self-transition guarded on the record not being archived — `HealthRecordServiceImpl` rejects writes once `status == ARCHIVED`.
3. Archiving is one-way: `ArchivedRecord` has no outgoing transition in the code, so an archived record is a stable terminal condition rather than a deletion.
4. Inside `ActiveRecord`, an attachment enters `AttachmentActive` only when its `FilePurpose` is allowed for the record type.
5. Every read is a guarded self-transition, because `FileAccessPolicyImpl` re-evaluates purpose-bound access on each request rather than trusting a previously issued link.
6. Attachment deletion is soft — the action sets `FileStatus.DELETED`, keeping the row for audit while removing it from every later projection.

**State sources:**

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/entity/HealthRecordStatus.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/impl/HealthRecordServiceImpl.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/entity/FileStatus.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/policy/FileAccessPolicyImpl.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/enums/FilePurpose.java`

## 6. Business Rules Applied

| UC | Enforced business/security rules | Known boundary or gap |
| --- | --- | --- |
| `UC-MH-12` | File purpose, owner, share expiry, and consultation context determine access. Archive is the supported record lifecycle; file deletion follows explicit policy. | No additional gap recorded in the code-first baseline. |

## 7. Partial / Excluded Boundaries

- No extra release boundary beyond the code-first UC catalogue is introduced by this package.

## 8. Code and Test Evidence

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/controller/FileController.java`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/policy/FileAccessPolicy.java`
- `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_record_timeline_screen.dart`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/HealthRecordServiceImplTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/file/FileControllerUploadTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/file/policy/FileAccessPolicyTest.java`
