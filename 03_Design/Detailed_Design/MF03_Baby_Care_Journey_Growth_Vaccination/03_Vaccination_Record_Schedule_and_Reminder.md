# MF-03 — Vaccination Record, Schedule, and Reminder

| Field | Value |
| --- | --- |
| Major Feature | **MF-03 — Baby Care Journey, Growth & Vaccination** |
| Function package | **Vaccination Record, Schedule, and Reminder** |
| Code-first use cases | `UC-BC-07, UC-BC-08` |
| Status | **Draft** |
| Baseline | Current reachable code and tests, 2026-08-23 |
| Diagram convention | `../sequence-diagram-skill.md`; explicit lifeline order, numbered messages, balanced activation |

## 1. Tổng quan luồng chính

Design vaccination evidence, computed schedules, and reminder creation.

- **UC-BC-07 — Manage Vaccination Records:** Create, view, update, and remove vaccination completion records for an authorized baby.
- **UC-BC-08 — Review Vaccination Schedule and Create Next-Dose Reminder:** Review the baby's vaccination schedule and create a supported reminder for an upcoming suggested dose.

The code is authoritative for routes, handlers, delegation, authorization, and persisted state. Report1 section 6.2 is authoritative for the Major Feature name. Historical UC numbering and obsolete triage plans are not design inputs.

## 2. Function Design — Code Traceability

| UC | Actor goal | Method / route | Exact handler | Delegated function | Authorization | Source |
| --- | --- | --- | --- | --- | --- | --- |
| `UC-BC-07` | Manage Vaccination Records | `GET /api/v1/vaccination/babies/{babyId}/records` | `VaccinationController.listVaccinationRecords()` | `IVaccinationService.listVaccinationRecords()` → `BabyProfileRepository.findById()` | isAuthenticated() | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java` |
| `UC-BC-07` | Manage Vaccination Records | `POST /api/v1/vaccination/babies/{babyId}/records` | `VaccinationController.addVaccinationRecord()` | `IVaccinationService.addVaccinationRecord()` → `VaccinationRecordRepository.findByBabyIdAndVaccineNameAndDoseNumberAndStatus()` | isAuthenticated() | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java` |
| `UC-BC-07` | Manage Vaccination Records | `DELETE /api/v1/vaccination/babies/{babyId}/records/{recordId}` | `VaccinationController.deleteVaccinationRecord()` | `IVaccinationService.deleteVaccinationRecord()` → `VaccinationRecordRepository.findByIdAndBabyId()` | isAuthenticated() | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java` |
| `UC-BC-07` | Manage Vaccination Records | `PATCH /api/v1/vaccination/babies/{babyId}/records/{recordId}` | `VaccinationController.updateVaccinationRecord()` | `IVaccinationService.updateVaccinationRecord()` → `VaccinationRecordRepository.findByIdAndBabyId()` | isAuthenticated() | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java` |
| `UC-BC-08` | Review Vaccination Schedule and Create Next-Dose Reminder | `POST /api/v1/reminders/vaccination` | `ReminderController.createVaccinationReminder()` | `IReminderService.createVaccinationReminder()` → `ReminderRepository.save()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/controller/ReminderController.java` |
| `UC-BC-08` | Review Vaccination Schedule and Create Next-Dose Reminder | `GET /api/v1/reminders/vaccination/suggestions` | `ReminderController.getVaccinationSuggestions()` | `IReminderService.getVaccinationSuggestions()` → `BabyProfileRepository.findByIdAndOwnerUserId()` | hasRole('MOTHER') | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/controller/ReminderController.java` |
| `UC-BC-08` | Review Vaccination Schedule and Create Next-Dose Reminder | `GET /api/v1/vaccination/babies/{babyId}/schedule` | `VaccinationController.getVaccinationSchedule()` | `IVaccinationService.getVaccinationSchedule()` → `BabyProfileRepository.findById()` | isAuthenticated() | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java` |

## 3. Class Diagram

This design-level UML follows the current source declarations: fields become attributes, reachable methods become operations, `implements` becomes realization, repository inheritance becomes generalization, and runtime-only calls remain dependencies. A missing layer or ownership relation is not invented.

```plantuml
@startuml ClassDiagram_03VaccinationRecordScheduleandReminder
skinparam classAttributeIconSize 0
hide empty members

class "CreateVaccinationReminderScreen" as UICreateVaccinationReminderScreen <<UI>>
class "VaccinationRecordFormScreen" as UIVaccinationRecordFormScreen <<UI>>
class "ReminderController" as ControllerReminderController <<Controller>> {
  - reminderService: IReminderService
  - todayTaskService: ITodayTaskService
  + createVaccinationReminder(request: CreateVaccinationReminderRequest, principal: Principal): ResponseEntity<ApiResponse<CreateReminderResponse>>
}
class "VaccinationController" as ControllerVaccinationController <<Controller>> {
  - vaccinationService: IVaccinationService
  + addVaccinationRecord(babyId: UUID, request: AddVaccinationRecordRequest, principal: Principal): ResponseEntity<ApiResponse<AddVaccinationRecordResponse>>
}
interface "IReminderService" as ServiceContractIReminderService <<Service>> {
  + createVaccinationReminder(request: CreateVaccinationReminderRequest, callerId: UUID): CreateReminderResponse
}
class "ReminderServiceImpl" as ServiceReminderServiceImpl <<Service>> {
  - reminderRepository: ReminderRepository
  - notificationService: INotificationService
  - auditService: AuditService
  - babyProfileRepository: BabyProfileRepository
  - motherJourneyRepository: MotherJourneyRepository
  - vaccinationRecordRepository: VaccinationRecordRepository
  - entityManager: EntityManager
  - appointmentNotificationScheduleService: AppointmentNotificationScheduleService
  + createVaccinationReminder(request: CreateVaccinationReminderRequest, callerId: UUID): CreateReminderResponse
}
ServiceContractIReminderService <|.. ServiceReminderServiceImpl : implements
interface "IVaccinationService" as ServiceContractIVaccinationService <<Service>> {
  + addVaccinationRecord(babyId: UUID, callerId: UUID, request: AddVaccinationRecordRequest): AddVaccinationRecordResponse
}
class "VaccinationServiceImpl" as ServiceVaccinationServiceImpl <<Service>> {
  - babyRepository: BabyProfileRepository
  - accessPolicy: BabyAccessPolicy
  - referenceRepository: VaccinationReferenceRepository
  - recordRepository: VaccinationRecordRepository
  - healthRecordRepository: HealthRecordRepository
  - auditService: AuditService
  + addVaccinationRecord(babyId: UUID, callerId: UUID, request: AddVaccinationRecordRequest): AddVaccinationRecordResponse
}
ServiceContractIVaccinationService <|.. ServiceVaccinationServiceImpl : implements
interface "ReminderRepository" as RepositoryReminderRepository <<Repository>> {
  + save(entity: Reminder): Reminder
}
class "Reminder" as EntityReminder <<Entity>> {
  - id: UUID
  - ownerUserId: UUID
  - journeyId: UUID
  - babyId: UUID
  - careSubjectId: UUID
  - reminderType: ReminderType
  - title: String
  - scheduledAt: Instant
}
interface "JpaRepository<Reminder, UUID>" as RepositoryBaseReminderRepository <<Framework>>
RepositoryBaseReminderRepository <|-- RepositoryReminderRepository : extends
interface "VaccinationRecordRepository" as RepositoryVaccinationRecordRepository <<Repository>> {
  + findByBabyIdAndVaccineNameAndDoseNumberAndStatus(babyId: UUID, vaccineName: String, doseNumber: short, status: VaccinationRecordStatus): Optional<VaccinationRecord>
}
class "VaccinationRecord" as EntityVaccinationRecord <<Entity>> {
  - id: UUID
  - babyId: UUID
  - careSubjectId: UUID
  - vaccinationScheduleId: UUID
  - vaccineName: String
  - doseNumber: Short
  - scheduledDate: LocalDate
  - administeredDate: LocalDate
}
interface "JpaRepository<VaccinationRecord, UUID>" as RepositoryBaseVaccinationRecordRepository <<Framework>>
RepositoryBaseVaccinationRecordRepository <|-- RepositoryVaccinationRecordRepository : extends
class "PostgreSQL" as DB <<Database>>
UICreateVaccinationReminderScreen ..> ControllerReminderController : invokes API
UIVaccinationRecordFormScreen ..> ControllerVaccinationController : invokes API
ControllerReminderController --> ServiceContractIReminderService : delegates
ControllerVaccinationController --> ServiceContractIVaccinationService : delegates
ServiceReminderServiceImpl --> RepositoryReminderRepository : reads / writes
ServiceVaccinationServiceImpl --> RepositoryVaccinationRecordRepository : reads / writes
RepositoryReminderRepository ..> EntityReminder : maps
RepositoryVaccinationRecordRepository ..> EntityVaccinationRecord : maps
RepositoryReminderRepository ..> DB : persists
RepositoryVaccinationRecordRepository ..> DB : persists
@enduml
```

**Figure 1 — Class Diagram: Vaccination Record, Schedule, and Reminder**

## 4. Sequence Diagram — Main Flow

Each group is a representative reachable main flow. The full endpoint surface remains in the Function Design table above.

```plantuml
@startuml
skinparam shadowing false
skinparam maxMessageSize 90
title Vaccination Record, Schedule, and Reminder — code-reachable representative flows

actor "Mother" as AMother
boundary "VaccinationRecordFormScreen" as UIVaccinationRecordFormScreen <<boundary>>
boundary "CreateVaccinationReminderScreen" as UICreateVaccinationReminderScreen <<boundary>>
participant "JwtAuthenticationFilter" as JWT <<middleware>>
control "VaccinationController" as CVaccinationController <<control>>
control "ReminderController" as CReminderController <<control>>
participant "IVaccinationService" as SIVaccinationService <<service>>
participant "IReminderService" as SIReminderService <<service>>
participant "VaccinationRecordRepository" as RVaccinationRecordRepository <<repository>>
participant "ReminderRepository" as RReminderRepository <<repository>>
database "PostgreSQL" as DB

group UC-BC-07 — Manage Vaccination Records [addVaccinationRecord()]
AMother -> UIVaccinationRecordFormScreen : 1. submitVaccinationRecord()
activate UIVaccinationRecordFormScreen
alt [authorized request succeeds]
UIVaccinationRecordFormScreen -> JWT : 2a. POST /api/v1/vaccination/babies/{babyId}/records with bearer token
activate JWT
JWT -> CVaccinationController : 2a-1. addVaccinationRecord(babyId, request, principal)
activate CVaccinationController
CVaccinationController -> SIVaccinationService : 2a-2. addVaccinationRecord(babyId, callerId, request)
activate SIVaccinationService
SIVaccinationService -> RVaccinationRecordRepository : 2a-3. findByBabyIdAndVaccineNameAndDoseNumberAndStatus(babyId, vaccineName, doseNumber, status)
activate RVaccinationRecordRepository
RVaccinationRecordRepository -> DB : 2a-4. SELECT VaccinationRecord via findByBabyIdAndVaccineNameAndDoseNumberAndStatus()
activate DB
DB --> RVaccinationRecordRepository : 2a-5. vaccinationRecordQueryResult
deactivate DB
RVaccinationRecordRepository --> SIVaccinationService : 2a-6. optionalVaccinationRecord
deactivate RVaccinationRecordRepository
SIVaccinationService --> CVaccinationController : 2a-7. addVaccinationRecordResponse
deactivate SIVaccinationService
CVaccinationController --> JWT : 2a-8. addVaccinationRecordResponse
deactivate CVaccinationController
JWT --> UIVaccinationRecordFormScreen : 2a-9. 201 Created — addVaccinationRecordResponse
deactivate JWT
UIVaccinationRecordFormScreen --> AMother : 2a-10. displaySavedVaccinationRecord()
else [authentication or role authorization fails]
UIVaccinationRecordFormScreen -> JWT : 2b. POST /api/v1/vaccination/babies/{babyId}/records with invalid or insufficient bearer token
activate JWT
JWT --> UIVaccinationRecordFormScreen : 2b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UIVaccinationRecordFormScreen --> AMother : 2b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UIVaccinationRecordFormScreen
end

group UC-BC-08 — Review Vaccination Schedule and Create Next-Dose Reminder [createVaccinationReminder()]
AMother -> UICreateVaccinationReminderScreen : 3. submitVaccinationReminder()
activate UICreateVaccinationReminderScreen
alt [authorized request succeeds]
UICreateVaccinationReminderScreen -> JWT : 4a. POST /api/v1/reminders/vaccination with bearer token
activate JWT
JWT -> CReminderController : 4a-1. createVaccinationReminder(request, principal)
activate CReminderController
CReminderController -> SIReminderService : 4a-2. createVaccinationReminder(request, callerId)
activate SIReminderService
SIReminderService -> RReminderRepository : 4a-3. save()
activate RReminderRepository
RReminderRepository -> DB : 4a-4. INSERT / UPDATE Reminder
activate DB
DB --> RReminderRepository : 4a-5. persistedReminder
deactivate DB
RReminderRepository --> SIReminderService : 4a-6. persistedReminder
deactivate RReminderRepository
SIReminderService --> CReminderController : 4a-7. createReminderResponse
deactivate SIReminderService
CReminderController --> JWT : 4a-8. createReminderResponse
deactivate CReminderController
JWT --> UICreateVaccinationReminderScreen : 4a-9. 201 Created — createReminderResponse
deactivate JWT
UICreateVaccinationReminderScreen --> AMother : 4a-10. displayVaccinationReminder()
else [authentication or role authorization fails]
UICreateVaccinationReminderScreen -> JWT : 4b. POST /api/v1/reminders/vaccination with invalid or insufficient bearer token
activate JWT
JWT --> UICreateVaccinationReminderScreen : 4b-1. 401 Unauthorized / 403 Forbidden
deactivate JWT
UICreateVaccinationReminderScreen --> AMother : 4b-2. showAuthenticationOrAuthorizationError(message)
end
deactivate UICreateVaccinationReminderScreen
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

## 5. Business Rules Applied

| UC | Enforced business/security rules | Known boundary or gap |
| --- | --- | --- |
| `UC-BC-07` | Dose identity, chronology, duplication, and baby authorization are server authoritative. A record does not change the vaccine catalogue itself. | No additional gap recorded in the code-first baseline. |
| `UC-BC-08` | Schedule/suggestion state comes from canonical vaccination records and catalogue rules. Creating a reminder does not mark a dose completed. | No additional gap recorded in the code-first baseline. |

## 6. Partial / Excluded Boundaries

- No extra release boundary beyond the code-first UC catalogue is introduced by this package.

## 7. Code and Test Evidence

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java`
- `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/vaccination_record_form_screen.dart`
- `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/VaccinationServiceImplTest.java`
- `05_Development/CareBridgeMobileApp/test/features/healthRecords/vaccination_model_test.dart`
- `05_Development/CareBridgeMobileApp/lib/features/healthRecords/services/vaccination_service.dart`
- `05_Development/CareBridgeMobileApp/lib/features/reminder/screens/create_vaccination_reminder_screen.dart`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/controller/ReminderController.java`
- `05_Development/CareBridgeMobileApp/lib/features/reminder/services/reminder_service.dart`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/VaccinationBookServiceTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/VaccinationReminderDispatchTest.java`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/VaccinationReminderServiceTest.java`
