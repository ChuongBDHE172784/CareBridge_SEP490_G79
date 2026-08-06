# MF-02 / Spec 04 — Maternal Health Record Lifecycle and Attachments

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-23 Manage Maternal Health Records |
| Use Case Group | Mobile App |
| Platform | Mother Mobile App; Backend; File Storage |
| Primary Actors | Mother |
| In Scope | Mother-owned protected records and files |
| Explicitly Excluded | Generated health-summary sharing |
| Implementation Trace | UI: Health record list, form, detail and attachment screens; Controller: HealthRecordController; Service: HealthRecordServiceImpl; Repository: HealthRecordRepository; Entity: HealthRecord, HealthRecordFile |

## 1. Tổng quan luồng chính (Main Flow Overview)

Mother-owned protected records and files. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF02_04_MaternalHealthRecordLifecycleandAttachments_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "Health record list" as UI1 <<UI>>
class "form" as UI2 <<UI>>
class "detail and attachment screens" as UI3 <<UI>>
class "HealthRecordController" as Controller1 <<Controller>> {
  - healthRecordService: IHealthRecordService
  + addHealthRecord(request: AddHealthRecordRequest, principal: Principal): ResponseEntity<ApiResponse<AddHealthRecordResponse>>
  + archiveHealthRecord(id: UUID, principal: Principal): ResponseEntity<ApiResponse<ArchiveHealthRecordResponse>>
  + getHealthRecord(recordId: UUID, principal: Principal): ResponseEntity<ApiResponse<HealthRecordDetailResponse>>
  + updateHealthRecord(id: UUID, request: UpdateHealthRecordRequest, principal: Principal): ResponseEntity<ApiResponse<UpdateHealthRecordResponse>>
  + getTimeline(filter: TimelineFilter, principal: Principal): ResponseEntity<ApiResponse<TimelineResponse>>
}
class "HealthRecordServiceImpl" as Service1 <<Service>> {
  - recordRepository: HealthRecordRepository
  - recordFileRepository: HealthRecordFileRepository
  - uploadedFileRepository: UploadedFileRepository
  - fileService: IFileService
  + addHealthRecord(request: AddHealthRecordRequest, callerId: UUID): AddHealthRecordResponse
  + getHealthRecord(recordId: UUID, callerId: UUID): HealthRecordDetailResponse
  + updateHealthRecord(id: UUID, request: UpdateHealthRecordRequest, ownerUserId: UUID): UpdateHealthRecordResponse
  + archiveRecord(id: UUID, ownerUserId: UUID): ArchiveHealthRecordResponse
  + getTimeline(ownerUserId: UUID, filter: TimelineFilter): TimelineResponse
}
interface "IHealthRecordService" as Service1Contract <<Service>>
interface "HealthRecordRepository" as Repository1 {
  + findByIdAndStatus(id: UUID, status: HealthRecordStatus): Optional<HealthRecord>
  + countByOwnerUserIdAndStatus(ownerUserId: UUID, status: HealthRecordStatus): long
}
class "HealthRecord" as Entity1 <<Entity>> {
  - id: UUID
  - ownerUserId: UUID
  - journeyId: UUID
  - babyId: UUID
  - recordType: RecordType
  - title: String
  - fileUrl: String
}
class "HealthRecordFile" as Entity2 <<Entity>> {
  - id: UUID
  - healthRecordId: UUID
  - fileId: UUID
  - displayOrder: int
  - createdAt: Instant
}
interface "JpaRepository<HealthRecord, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Cloudinary or configured file storage" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller1 : invokes API
UI3 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
Entity1 "1" *-- "0..*" Entity2 : attachments
@enduml
```

**Figure 1 — Class Diagram: Maternal Health Record Lifecycle and Attachments**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF02_04_MaternalHealthRecordLifecycleandAttachments_SequenceDiagram
skinparam shadowing false

actor "Mother" as Actor
boundary ":Health record screens" as UI1
control ":HealthRecordController" as Controller1
participant ":HealthRecordServiceImpl" as Service1 <<service>>
participant ":HealthRecordRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":Cloudinary file storage" as External1 <<external system>>

group UC-23 Manage Maternal Health Records
  Actor -> UI1 : 1. startManageMaternalHealthRecords()
  activate UI1
  UI1 -> Controller1 : 2. getTimeline() / addHealthRecord() / updateHealthRecord() / archiveHealthRecord()
  activate Controller1
  Controller1 -> Service1 : 3. getTimeline() / addHealthRecord() / updateHealthRecord() / archiveRecord()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findActiveByOwnerFiltered() / findById()
    activate Repository1
    Repository1 -> DB : 4a-1. SELECT
    activate DB
    DB --> Repository1 : 4a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 4a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 4a-4. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4a-5. 200 OK
    deactivate Controller1
    UI1 --> Actor : 4a-6. displayCurrentState()
    deactivate UI1
  else [selected action creates, updates, archives or deletes]
    Service1 -> Repository1 : 4b. findActiveByOwnerFiltered() / findById()
    activate Repository1
    Repository1 -> DB : 4b-1. SELECT
    activate DB
    DB --> Repository1 : 4b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 4b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 4b-4. save()
    activate Repository1
    Repository1 -> DB : 4b-5. INSERT / UPDATE
    activate DB
    DB --> Repository1 : 4b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 4b-7. persistedEntity
    deactivate Repository1
    Service1 -> External1 : 4b-8. uploadOrDeleteAttachment()
    activate External1
    External1 --> Service1 : 4b-9. integrationResult
    deactivate External1
    Service1 --> Controller1 : 4b-10. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-11. 200 OK / 201 Created
    deactivate Controller1
    UI1 --> Actor : 4b-12. displayConfirmedState()
    deactivate UI1
  else [request is invalid, forbidden, not found or conflicting]
    Service1 --> Controller1 : 4c. domainError
    deactivate Service1
    Controller1 --> UI1 : 4c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller1
    UI1 --> Actor : 4c-2. displayActionableError()
    deactivate UI1
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Maternal Health Record Lifecycle and Attachments Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-23 Manage Maternal Health Records.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Mother-owned protected records and files.
- The following remains outside this contract: Generated health-summary sharing.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
