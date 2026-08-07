# MF-03 / Spec 03 — Vaccination Records, Schedule and Reminder Linkage

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-38 Manage Vaccination Journey |
| Use Case Group | Mobile App |
| Platform | Mother Mobile App; Backend |
| Primary Actors | Mother |
| In Scope | Reference schedule does not replace professional vaccination advice |
| Explicitly Excluded | Official immunization registry |
| Implementation Trace | UI: Vaccination schedule, record and reminder screens; Controller: VaccinationController, ReminderController; Service: VaccinationServiceImpl, ReminderServiceImpl; Repository: VaccinationRecordRepository, ReminderRepository; Entity: VaccinationRecord, Reminder |

## 1. Tổng quan luồng chính (Main Flow Overview)

Reference schedule does not replace professional vaccination advice. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF03_03_VaccinationRecordsScheduleandReminderLinkage_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "Vaccination schedule" as UI1 <<UI>>
class "record and reminder screens" as UI2 <<UI>>
class "VaccinationController" as Controller1 <<Controller>> {
  - vaccinationService: IVaccinationService
  + getVaccinationSchedule(babyId: UUID, principal: Principal): ResponseEntity<ApiResponse<VaccinationScheduleResponse>>
  + listVaccinationRecords(babyId: UUID, principal: Principal): ResponseEntity<ApiResponse<List<VaccinationRecordResponse>>>
  + addVaccinationRecord(babyId: UUID, request: AddVaccinationRecordRequest, principal: Principal): ResponseEntity<ApiResponse<AddVaccinationRecordResponse>>
  + deleteVaccinationRecord(babyId: UUID, recordId: UUID, principal: Principal): ResponseEntity<Void>
  + markVaccinationCompleted(babyId: UUID, request: MarkVaccinationCompletedRequest, principal: Principal): ResponseEntity<ApiResponse<VaccinationCompletionResponse>>
  + postponeVaccination(babyId: UUID, request: PostponeVaccinationRequest, principal: Principal): ResponseEntity<ApiResponse<PostponeVaccinationResponse>>
  + updateVaccinationRecord(babyId: UUID, recordId: UUID, request: UpdateVaccinationRecordRequest, ...): ResponseEntity<ApiResponse<VaccinationRecordResponse>>
}
class "ReminderController" as Controller2 <<Controller>> {
  - reminderService: IReminderService
  - todayTaskService: ITodayTaskService
  - taskActionFacade: UnifiedTaskActionFacade
  + createVaccinationReminder(request: CreateVaccinationReminderRequest, principal: Principal): ResponseEntity<ApiResponse<CreateReminderResponse>>
  + completeReminder(reminderId: UUID, principal: Principal): ResponseEntity<ApiResponse<ReminderDetailResponse>>
  + createMedicationReminder(request: CreateMedicationReminderRequest, principal: Principal): ResponseEntity<ApiResponse<CreateReminderResponse>>
  + createReminder(request: CreateReminderRequest, principal: Principal): ResponseEntity<ApiResponse<CreateReminderResponse>>
  + deleteReminder(reminderId: UUID, principal: Principal): ResponseEntity<Void>
  + enableReminder(reminderId: UUID, principal: Principal): ResponseEntity<ApiResponse<ReminderDetailResponse>>
  + getReminderDetail(reminderId: UUID, principal: Principal): ResponseEntity<ApiResponse<ReminderDetailResponse>>
}
class "VaccinationServiceImpl" as Service1 <<Service>> {
  - babyRepository: BabyProfileRepository
  - accessPolicy: BabyAccessPolicy
  - referenceRepository: VaccinationReferenceRepository
  - recordRepository: VaccinationRecordRepository
  + getVaccinationSchedule(babyProfileId: UUID, callerId: UUID): VaccinationScheduleResponse
  + listVaccinationRecords(babyId: UUID, callerId: UUID): List<VaccinationRecordResponse>
  + addVaccinationRecord(babyId: UUID, callerId: UUID, request: AddVaccinationRecordRequest): AddVaccinationRecordResponse
  + deleteVaccinationRecord(babyId: UUID, recordId: UUID, callerId: UUID): void
  + markVaccinationCompleted(babyId: UUID, callerId: UUID, request: MarkVaccinationCompletedRequest): VaccinationCompletionResponse
}
class "ReminderServiceImpl" as Service2 <<Service>> {
  - reminderRepository: ReminderRepository
  - notificationService: INotificationService
  - auditService: AuditService
  - babyProfileRepository: BabyProfileRepository
  + createVaccinationReminder(request: CreateVaccinationReminderRequest, callerId: UUID): CreateReminderResponse
  + completeReminder(reminderId: UUID, callerId: UUID): ReminderDetailResponse
  + createMedicationReminder(request: CreateMedicationReminderRequest, callerId: UUID): CreateReminderResponse
  + createReminder(request: CreateReminderRequest, callerId: UUID): CreateReminderResponse
  + deleteReminder(reminderId: UUID, callerId: UUID): void
}
interface "IVaccinationService" as Service1Contract <<Service>>
interface "IReminderService" as Service2Contract <<Service>>
interface "VaccinationRecordRepository" as Repository1 {
  + findAllByBabyId(babyId: UUID): List<VaccinationRecord>
  + findByBabyIdAndVaccineNameAndDoseNumberAndStatus(babyId: UUID, vaccineName: String, doseNumber: short, ...): Optional<VaccinationRecord>
  + findByBabyIdAndStatus(babyId: UUID, status: VaccinationRecordStatus): List<VaccinationRecord>
  + findByIdAndBabyId(id: UUID, babyId: UUID): Optional<VaccinationRecord>
  + findByBabyIdAndVaccineNameAndDoseNumber(babyId: UUID, vaccineName: String, doseNumber: short): Optional<VaccinationRecord>
}
interface "ReminderRepository" as Repository2 {
  + findByIdAndOwnerUserId(id: UUID, ownerUserId: UUID): Optional<Reminder>
  + findByOwnerUserIdOrderByScheduledAtDesc(ownerUserId: UUID): List<Reminder>
  + findByOwnerUserIdAndStatusNot(ownerUserId: UUID, status: ReminderStatus): List<Reminder>
  + findById(id: UUID): Optional<Reminder>
  + findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(ownerUserId: UUID, start: Instant, end: Instant, ...): List<Reminder>
  + findByOwnerUserIdAndReminderTypeAndStatusIn(ownerUserId: UUID, reminderType: ReminderType, statuses: List<ReminderStatus>): List<Reminder>
}
class "VaccinationRecord" as Entity1 <<Entity>> {
  - id: UUID
  - babyId: UUID
  - vaccineName: String
  - doseNumber: Short
  - scheduledDate: LocalDate
  - administeredDate: LocalDate
  - status: VaccinationRecordStatus
}
class "Reminder" as Entity2 <<Entity>> {
  - id: UUID
  - ownerUserId: UUID
  - journeyId: UUID
  - babyId: UUID
  - careSubjectId: UUID
  - reminderType: ReminderType
  - title: String
}
interface "JpaRepository<VaccinationRecord, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<Reminder, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "Firebase Cloud Messaging" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller2 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service2Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository2 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Repository2 ..> Entity2 : maps
Repository2 ..> DB : persists
Service1 ..> External : invokes when required
Service2 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Vaccination Records, Schedule and Reminder Linkage**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF03_03_VaccinationRecordsScheduleandReminderLinkage_SequenceDiagram
skinparam shadowing false

actor "Mother" as Actor
boundary ":vaccination screens" as UI1
control ":VaccinationController" as Controller1
participant ":VaccinationServiceImpl" as Service1 <<service>>
participant ":VaccinationRecordRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":Firebase Cloud Messaging" as External1 <<external system>>

group UC-38 Manage Vaccination Journey
  Actor -> UI1 : 1. startManageVaccinationJourney()
  activate UI1
  UI1 -> Controller1 : 2. getVaccinationSchedule() / addVaccinationRecord() / updateVaccinationRecord() / deleteVaccinationRecord()
  activate Controller1
  Controller1 -> Service1 : 3. getVaccinationSchedule() / addVaccinationRecord() / updateVaccinationRecord() / deleteVaccinationRecord()
  activate Service1
  alt [selected action is view or list]
    Service1 -> Repository1 : 4a. findAllByBabyId()
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
    Service1 -> Repository1 : 4b. findAllByBabyId()
    activate Repository1
    Repository1 -> DB : 4b-1. SELECT
    activate DB
    DB --> Repository1 : 4b-2. currentState
    deactivate DB
    Repository1 --> Service1 : 4b-3. scopedEntity
    deactivate Repository1
    Service1 -> Repository1 : 4b-4. save() / delete()
    activate Repository1
    Repository1 -> DB : 4b-5. INSERT / UPDATE / DELETE
    activate DB
    DB --> Repository1 : 4b-6. persistedState
    deactivate DB
    Repository1 --> Service1 : 4b-7. persistedEntity
    deactivate Repository1
    Service1 ->> External1 : 4b-8. scheduleVaccinationReminder()
    Service1 --> Controller1 : 4b-9. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4b-10. 200 OK / 201 Created
    deactivate Controller1
    UI1 --> Actor : 4b-11. displayConfirmedState()
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

**Figure 2 — Sequence Diagram: Vaccination Records, Schedule and Reminder Linkage Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-38 Manage Vaccination Journey.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Reference schedule does not replace professional vaccination advice.
- The following remains outside this contract: Official immunization registry.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
