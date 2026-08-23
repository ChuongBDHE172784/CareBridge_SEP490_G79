# CareBridge Detailed Design — Code-First Baseline

> Status: **Draft**
>
> Baseline date: **2026-08-23**
>
> Feature authority: `02_Requirements/SRS/Report1_Project Introduction.md`, section **6.2 Major Features**
>
> Functional authority: current reachable Mobile/Web/API/AI code and tests, represented by the 88-use-case code-first catalogue
>
> Sequence convention: [`sequence-diagram-skill.md`](sequence-diagram-skill.md)

## Scope

This directory contains exactly **10 release-1 Major Features**, **31 function-design documents**, and **88 uniquely owned code-first use cases**. It does not preserve the retired 91-UC structure when that structure conflicts with reachable code.

The same UC is owned by one MF only. Cross-feature consumers are noted as references rather than duplicated ownership. MF-11, MF-12, and CC-01 from Report1 are Version-2/cross-cutting scope and therefore do not create extra release-1 folders here.

## Major Feature Index

| Major Feature | Code-aligned responsibility | Owned UCs | Function-design documents |
| --- | --- | ---: | --- |
| **MF-01 — Account, Trust & Access Control** | Identity, authentication, account lifecycle, consent, security governance, and audit controls. | 16 | [Account Registration, Authentication, and Sessions](MF01_Account_Trust_Access_Control/01_Account_Registration_Authentication_and_Sessions.md) — `UC-AC-01, UC-AC-02, UC-AC-03, UC-AC-04, UC-AC-05, UC-AC-07`<br>[Profile, Notifications, Privacy, and Account State](MF01_Account_Trust_Access_Control/02_Profile_Notifications_Privacy_and_Account_State.md) — `UC-AC-06, UC-AC-08, UC-AC-09, UC-AC-10, UC-AC-11`<br>[Admin Account Governance, Security, and Audit](MF01_Account_Trust_Access_Control/03_Admin_Account_Governance_Security_and_Audit.md) — `UC-AD-01, UC-AD-02, UC-AD-03, UC-AD-04, UC-AD-05` |
| **MF-02 — Mother Care Journey** | Maternal journey onboarding, health tracking, screening, records, planning, checklists, and safe exercise. | 19 | [Mother Journey Onboarding, Lifecycle, and Recommendations](MF02_Mother_Care_Journey/01_Mother_Journey_Onboarding_Lifecycle_and_Recommendations.md) — `UC-MH-01, UC-MH-02, UC-MH-03, UC-MH-04, UC-MH-05, UC-MH-06`<br>[Maternal Metrics, Fetal Movement, Hydration, and AI Screening](MF02_Mother_Care_Journey/02_Maternal_Metrics_Fetal_Movement_Hydration_and_AI_Screening.md) — `UC-MH-07, UC-MH-08, UC-MH-09, UC-MH-11`<br>[Pregnancy Exercise Safety and Session](MF02_Mother_Care_Journey/03_Pregnancy_Exercise_Safety_and_Session.md) — `UC-MH-18, UC-MH-19`<br>[Personal Health Record and Attachment Lifecycle](MF02_Mother_Care_Journey/04_Personal_Health_Record_and_Attachment_Lifecycle.md) — `UC-MH-12`<br>[Appointments, Reminders, Checklists, and Today Tasks](MF02_Mother_Care_Journey/05_Appointments_Reminders_Checklists_and_Today_Tasks.md) — `UC-MH-13, UC-MH-14, UC-MH-15, UC-MH-16, UC-MH-17`<br>[EPDS Screening, History, and Family Notification](MF02_Mother_Care_Journey/06_EPDS_Screening_History_and_Family_Notification.md) — `UC-MH-10` |
| **MF-03 — Baby Care Journey, Growth & Vaccination** | Baby profile and daily care, growth/development tracking, vaccination records, schedules, and reminders. | 8 | [Baby Profile, Daily Care, and Summary](MF03_Baby_Care_Journey_Growth_Vaccination/01_Baby_Profile_Daily_Care_and_Summary.md) — `UC-BC-01, UC-BC-02, UC-BC-03, UC-BC-04`<br>[Growth and Development Tracking](MF03_Baby_Care_Journey_Growth_Vaccination/02_Growth_and_Development_Tracking.md) — `UC-BC-05, UC-BC-06`<br>[Vaccination Record, Schedule, and Reminder](MF03_Baby_Care_Journey_Growth_Vaccination/03_Vaccination_Record_Schedule_and_Reminder.md) — `UC-BC-07, UC-BC-08` |
| **MF-04 — Community Q&A & Moderation** | Community discovery, questions, answers, engagement, reporting, taxonomy, moderation, and AI policy controls. | 10 | [Community Q&A, Search, and Engagement](MF04_Community_QA_Moderation/01_Community_QA_Search_and_Engagement.md) — `UC-CO-01, UC-CO-02, UC-CO-03, UC-CO-04`<br>[Reporting, Moderation, Taxonomy, and AI Policy](MF04_Community_QA_Moderation/02_Reporting_Moderation_Taxonomy_and_AI_Policy.md) — `UC-CO-06, UC-AD-09, UC-AD-16, UC-AD-17, UC-AD-18, UC-AD-19` |
| **MF-05 — Verified Expert Network** | Expert onboarding and trust, directory/availability, consultation requests, shared-care access, messaging, and calls. | 14 | [Expert Onboarding, Identity, Credentials, and Trust](MF05_Verified_Expert_Network/01_Expert_Onboarding_Identity_Credentials_and_Trust.md) — `UC-EX-01, UC-EX-02, UC-EX-03, UC-EX-04, UC-EX-05, UC-AD-06`<br>[Expert Directory and Availability](MF05_Verified_Expert_Network/02_Expert_Directory_and_Availability.md) — `UC-EX-06, UC-EX-07`<br>[Consultation Request and Shared Care Lifecycle](MF05_Verified_Expert_Network/03_Consultation_Request_and_Shared_Care_Lifecycle.md) — `UC-EX-08, UC-EX-09, UC-EX-12`<br>[Direct Messaging, Attachments, and Calls](MF05_Verified_Expert_Network/04_Direct_Messaging_Attachments_and_Calls.md) — `UC-EX-10, UC-EX-11, UC-AD-07` |
| **MF-06 — AI Nurse Assistant** | RAG-backed conversational assistance with audience minimization, citations, clinical safety floor, and operational knowledge controls. | 3 | [AI Nurse RAG Chat and Safety Guardrails](MF06_AI_Nurse_Assistant/01_AI_Nurse_RAG_Chat_and_Safety_Guardrails.md) — `UC-AI-01`<br>[AI Knowledge Base and Diagnostics Operations](MF06_AI_Nurse_Assistant/02_AI_Knowledge_Base_and_Diagnostics_Operations.md) — `UC-AD-20, UC-AD-21` |
| **MF-07 — Emergency Map & Nearby Care Support** | Nearby care discovery, route handoff, emergency sessions, location sharing, family alerts, and response. | 3 | [Emergency Map, Facility, Route, and Navigation](MF07_Emergency_Map_Nearby_Care_Support/01_Emergency_Map_Facility_Route_and_Navigation.md) — `UC-ES-01`<br>[Emergency Session, Family Alert, and Response](MF07_Emergency_Map_Nearby_Care_Support/02_Emergency_Session_Family_Alert_and_Response.md) — `UC-ES-02, UC-ES-03` |
| **MF-08 — Family Sync & Cooperative Care** | Care groups, membership, permissions, care tasks, shared records, checklists, notes, and family dashboard views. | 5 | [Care Group and Membership Lifecycle](MF08_Family_Sync_Cooperative_Care/01_Care_Group_and_Membership_Lifecycle.md) — `UC-FM-01, UC-FM-02`<br>[Family Permissions and Shared Care Monitoring](MF08_Family_Sync_Cooperative_Care/02_Family_Permissions_and_Shared_Care_Monitoring.md) — `UC-FM-03, UC-FM-05`<br>[Family Care Task Assignment and Status](MF08_Family_Sync_Cooperative_Care/03_Family_Care_Task_Assignment_and_Status.md) — `UC-FM-04` |
| **MF-09 — Content Hub** | Verified content lifecycle, checklist template versioning/distribution, exercise catalogue, and posture configuration. | 8 | [Verified Content Browse and Consumption](MF09_Content_Hub/01_Verified_Content_Browse_and_Consumption.md) — `UC-CO-05`<br>[Content Authoring, Review, Publishing, and Archive](MF09_Content_Hub/02_Content_Authoring_Review_Publishing_and_Archive.md) — `UC-AD-08, UC-AD-14, UC-AD-15`<br>[Checklist Template Versioning and Runtime Distribution](MF09_Content_Hub/03_Checklist_Template_Versioning_and_Runtime_Distribution.md) — `UC-AD-10, UC-AD-11`<br>[Exercise Catalogue and Posture Configuration](MF09_Content_Hub/04_Exercise_Catalogue_and_Posture_Configuration.md) — `UC-AD-12, UC-AD-13` |
| **MF-10 — Smart Activity Monitoring & Safety Support** | Consent-bound fall-detection configuration, sensor self-test, impact events, safety checks, alerts, and feedback. | 2 | [Fall Detection Consent, Configuration, and Self-Test](MF10_Smart_Activity_Monitoring_Safety_Support/01_Fall_Detection_Consent_Configuration_and_Self_Test.md) — `UC-ES-04`<br>[Detected Impact, Safety Check, Alert, and Feedback](MF10_Smart_Activity_Monitoring_Safety_Support/02_Detected_Impact_Safety_Check_Alert_and_Feedback.md) — `UC-ES-05` |

## Design Rules

1. Report1 section 6.2 controls the 10 MF names; code controls functions and runtime logic.
2. Every function table cites exact routes, handlers, delegated functions, authorization, and source files when resolvable.
3. Class diagrams do not invent layers absent from code.
4. Sequence diagrams keep actor → UI → middleware → controller/route → service/policy → repository/data store → external-system order, sequential numbering, balanced activation, and dashed returns.
5. Images are not generator-owned. Existing `images/` files remain untouched so they can be cropped or replaced manually.
6. `docs/AI/01_THIET_KE_KIEN_TRUC_AI_RAG_VA_BAO_VE_DO_AN.md` is an approved reference for MF-06 and is never rewritten by this generator.
7. Structured AI triage session/history/handoff is Partial until a reachable client owns that lifecycle; AI Nurse release-1 design is the current RAG chat plus its safety guardrails and separate operator endpoints.

## Regeneration and Validation

```bash
python3 scripts/docs/generate_code_first_detailed_design.py
python3 scripts/docs/generate_code_first_detailed_design.py --check
```

`--check` verifies folder names, 31 documents, unique 88-UC ownership, required sections, PlantUML boundaries, numbered sequence messages, and exact generated content. It does not modify or validate image crops.
