# Detailed Design — CareBridge Release 1 (10 Major Features)

## 1. Purpose and source baseline

This directory documents the ten Release 1 Major Features approved in
`02_Requirements/SRS/Report1_Project Introduction_new.md`, section 6.2.

The original detailed-design format is retained:

1. One folder per Major Feature.
2. Multiple Markdown Spec files inside each MF folder.
3. Every Spec contains a Field/Value metadata table, Main Flow Overview,
   Class Diagram, Sequence Diagram — Main Flow, named figure captions, and
   Business Rules Applied.

Current Web and Mobile navigation, their consumed APIs, and the supporting Backend/
AI-service code under `05_Development/` are implementation evidence. A capability is
included only when it is authorized by the ten Release 1 MFs and has a real reachable
UI or system flow. A leftover endpoint, class, route placeholder, or orphan screen is
not sufficient evidence of product scope.

State Machine diagrams are intentionally no longer part of this document set.
Lifecycle constraints remain documented as business rules and sequence branches.

## 2. Release 1 Major Features

| ID | Major Feature | Primary platforms |
| --- | --- | --- |
| MF-01 | Account, Trust & Access Control | Backend, Web, Mobile |
| MF-02 | Mother Care Journey | Backend, Mobile; Web content administration |
| MF-03 | Baby Care Journey, Growth & Vaccination | Backend, Mobile |
| MF-04 | Community Q&A & Moderation | Backend, Mobile community, Web moderation |
| MF-05 | Verified Expert Network & Contribution | Backend, Web, Mobile |
| MF-06 | AI Nurse Assistant & Risk Triage | AI service, Backend, Mobile |
| MF-07 | Emergency Map & Nearby Care Support | Backend, Mobile |
| MF-08 | Family Sync & Cooperative Care | Backend, Mobile |
| MF-09 | Verified Content & Checklist Hub | Backend, Web administration, Mobile |
| MF-10 | Smart Activity Monitoring & Safety Support | Backend, Mobile |

The `MF-9` label in the source SRS table is normalized to `MF-09`.

## 3. Scope migration from the previous design

| Previous design area | New placement |
| --- | --- |
| MF-01 through MF-07 | Retained with the original multi-Spec structure |
| Personal Health Records | Moved into MF-02 for maternal records; baby-linked records are referenced by MF-03 |
| Reminders, Tasks & Care Plan | Moved into MF-02; vaccination reminders remain integrated with MF-03 |
| Family Sync (previous MF-10) | Renumbered to MF-08 |
| Verified Content (previous MF-11) | Renumbered to MF-09 |
| Checklist implementation | Added as a dedicated MF-09 Spec because current code has an independent checklist bounded context |
| Expense Preparation Planner | Removed from Release 1 |
| Connected Device & Health Data Integration | Deferred; removed from Release 1 detailed design |
| Smart Activity Monitoring (previous MF-14) | Renumbered to MF-10 |
| Paid consultation, payment, commission, partner/sponsored content and realtime direct communication | Deferred/legacy code; excluded from these Release 1 contracts |

## 4. Feature-to-Spec map

| Feature folder | Spec file | Main flow |
| --- | --- | --- |
| `MF01_Account_Trust_AccesControl` | `01_Account_Registration_Authentication_Lifecycle.md` | Registration, OTP, authentication and credential lifecycle |
| | `02_Community_Identity_Privacy_Consent_Sharing.md` | Public identity separation, privacy and consent |
| | `03_Admin_Account_Governance_Security_Audit.md` | Account governance and audit |
| `MF02_Mother_Care_Journey` | `01_Mother_Journey_Lifecycle_Dashboard.md` | Canonical mother journey and dashboard |
| | `02_Maternal_Health_Postpartum_Tracking.md` | Metrics and postpartum tracking |
| | `03_Pregnancy_Exercise_Session_Safety.md` | Reviewed exercise and safe session execution |
| | `04_Personal_Health_Record_Lifecycle_Timeline.md` | Maternal record and attachment lifecycle |
| | `05_Reminder_Lifecycle_Today_Tasks.md` | Reminder lifecycle and today-task projection |
| `MF03_Baby_Care_Growth_Vaccination` | `01_Baby_Profile_Daily_Care_Overview.md` | Baby profile, daily care and linked records |
| | `02_Growth_Development_Tracking.md` | Growth measurements and milestones |
| | `03_Vaccination_Record_Schedule_Reminder.md` | Vaccination records, reference schedule and reminder linkage |
| `MF04_Community_QA_Moderation` | `01_Community_Question_Answer_Flow.md` | Question and answer flow |
| | `02_Content_Moderation_Enforcement_Pipeline.md` | Reports, moderation and enforcement |
| `MF05_Verified_Expert_Network_Contribution` | `01_Expert_Verification_Trust_Lifecycle.md` | Expert application, verification and trust |
| | `02_Expert_Directory_Availability_Community_Contribution.md` | Directory, availability and community contribution |
| `MF06_AI_Nurse_Assistant_Risk_Triage` | `01_AI_Symptom_Intake_Risk_Triage_Emergency_Handoff.md` | Intake, triage and emergency handoff |
| | `02_Approved_Knowledge_RedFlag_Execution.md` | Approved evidence retrieval and red-flag execution |
| `MF07_Emergency_Map_Nearby_Care_Support` | `01_Emergency_Map_Facility_Route_Navigation.md` | Facility search, route and navigation |
| `MF08_Family_Sync_Cooperative_Care` | `01_Care_Group_Invitation_Lifecycle.md` | Care group and invitation |
| | `02_Family_Permission_Shared_Visibility.md` | Permission-filtered shared care |
| | `03_Family_Care_Task_Assignment.md` | Family care-task assignment |
| `MF09_Verified_Content_Checklist_Hub` | `01_Content_Browse_Consumption.md` | Reviewed content consumption |
| | `02_Content_Authoring_Review_Publishing_Lifecycle.md` | Authoring, review, publishing and versioning |
| | `03_Checklist_Distribution_Current_History.md` | Template distribution, current checklist and history |
| `MF10_Smart_Activity_Monitoring_Safety_Support` | `01_Monitoring_Configuration_Enable_Disable.md` | Consent, configuration and monitoring session |
| | `02_Fall_Detection_Safety_Check_False_Positive_Feedback.md` | Suspected event, safety check, alert and feedback |

Total: 10 Major Feature folders and 26 detailed Spec files.

## 5. Diagram convention

All diagrams use PlantUML.

### Class Diagram

- Use real current entities, DTOs, controllers, services and repositories.
- Mark a planned or unresolved element explicitly; do not present it as implemented.
- Place a bold figure caption immediately after the PlantUML block.

### Sequence Diagram

Every Sequence Diagram follows `sequence-diagram-skill.md`:

- Explicit lifeline order: Actor → UI → Controller → Middleware when real →
  Service → Repository → Database → External Service.
- Every message is sequentially numbered; branches use hierarchical suffixes.
- Every processing lifeline has a matching activation bar.
- Synchronous calls have dashed return messages; asynchronous messages use
  `->>` without a fabricated return.
- `alt`, `opt`, `loop`, `par` and `ref` fragments represent real control flow only.
- HTTP responses include their status code.

### State and lifecycle behavior

No State Machine diagram is generated. Valid transitions, invalid transitions,
ownership checks, idempotency and side effects are specified in Main Flow,
Sequence Diagram branches and Business Rules Applied.

## 6. Review status

These documents are `Draft`. The active code still contains Release 1 drift that is
deliberately excluded here: partner flows, direct chat/voice/video calls, consultation
requests, fees/ratings, contribution badges or rankings, connected health devices,
health-summary sharing, standalone RAG chat and nearby-expert support. Their presence
in source code does not make them part of MF-01 through MF-10.

Verification snapshot (2026-08-05): Web production build passes. A focused Mobile suite
covering routing and MF-03/04/05/06/07/08/09/10 produced 80 passing tests and one MF-07
failure (`nearby_care_contract_test.dart`, provider-label/route case with nullable
facility ID). `flutter analyze` could not complete because the Dart analysis server
terminated on a truncated LSP JSON message; this is an incomplete quality gate, not a
source-level pass. Therefore the document set does not claim all ten MFs are stable.
