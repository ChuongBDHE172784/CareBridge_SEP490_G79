# Detailed Design — CareBridge Release 1

## 1. Baseline and structure

This directory implements the current 91-use-case SRS baseline. It keeps one folder per MF and one or more implementation-flow Specs inside each folder. Every Spec preserves the required Field/Value metadata table, Main Flow Overview, Class Diagram, Sequence Diagram — Main Flow, named figure captions and Business Rules Applied.

Use-case groups (Shared/Common, Mobile App and Web App) are recorded in Spec metadata; they do not replace the ten MF folders. State Machine diagrams are intentionally omitted. Sequence diagrams follow [sequence-diagram-skill.md](./sequence-diagram-skill.md).

## 2. Scope rules

- User-approved scope and the current reachable Mobile/Web flow take precedence over unused legacy code.
- Free expert requests, accepted direct chat, protected attachments and voice/video calls are active MF-05 flows.
- Community identity, expert gamification, paid booking/payment/commission/refund, partner marketplace, nearby-expert request, connected devices and standalone RAG chat are excluded.
- A class or endpoint without a supported reachable product flow is not sufficient to enter a Spec.

## 3. Feature-to-Spec map

| Feature | Spec file | Use cases |
| --- | --- | --- |
| MF-01 — Account, Trust & Access Control | [01_Account_Registration_Authentication_Lifecycle.md](./MF01_Account_Trust_AccesControl/01_Account_Registration_Authentication_Lifecycle.md) | UC-01 Register Account; UC-02 Login; UC-03 Logout; UC-04 Reset Password; UC-05 Change Password |
|  | [02_Account_Profile_Notifications_Privacy_Sessions.md](./MF01_Account_Trust_AccesControl/02_Account_Profile_Notifications_Privacy_Sessions.md) | UC-06 Manage Account Profile; UC-07 Manage Notifications; UC-08 Manage Privacy and Data Permissions; UC-09 Deactivate or Delete Own Account; UC-10 Submit Account Lock Appeal; UC-19 Manage Own Login Sessions |
|  | [03_Admin_Account_Governance_Security_Audit.md](./MF01_Account_Trust_AccesControl/03_Admin_Account_Governance_Security_Audit.md) | UC-69 View Admin Dashboard; UC-70 Manage User Accounts and Roles; UC-71 Create Staff Account; UC-72 Review Account Lock Appeals; UC-73 Review Audit and Security Operations; UC-74 Manage System Configuration |
| MF-02 — Mother Care Journey | [01_Mother_Journey_Lifecycle_Dashboard.md](./MF02_Mother_Care_Journey/01_Mother_Journey_Lifecycle_Dashboard.md) | UC-20 Manage Mother Journey; UC-33 View Personalized Care Recommendations |
|  | [02_Maternal_Health_Postpartum_Quick_Notes.md](./MF02_Mother_Care_Journey/02_Maternal_Health_Postpartum_Quick_Notes.md) | UC-21 Manage Maternal Health Metrics; UC-22 Manage Postpartum Logs; UC-31 Record Quick Health Notes |
|  | [03_Pregnancy_Exercise_Session_Safety.md](./MF02_Mother_Care_Journey/03_Pregnancy_Exercise_Session_Safety.md) | UC-27 Browse Pregnancy Exercises; UC-28 Complete Pre-exercise Safety Check; UC-29 Perform Camera-guided Exercise Session; UC-30 View Exercise History and Results |
|  | [04_Personal_Health_Record_Lifecycle_Timeline.md](./MF02_Mother_Care_Journey/04_Personal_Health_Record_Lifecycle_Timeline.md) | UC-23 Manage Maternal Health Records |
|  | [05_Appointment_Reminder_Today_Tasks.md](./MF02_Mother_Care_Journey/05_Appointment_Reminder_Today_Tasks.md) | UC-24 Manage Appointments; UC-25 Manage Reminders and Schedules; UC-26 View Today Care Tasks |
|  | [06_EPDS_Screening_History_Safety.md](./MF02_Mother_Care_Journey/06_EPDS_Screening_History_Safety.md) | UC-32 Manage EPDS Screening |
| MF-03 — Baby Care Journey, Growth & Vaccination | [01_Baby_Profile_Daily_Care_Overview.md](./MF03_Baby_Care_Growth_Vaccination/01_Baby_Profile_Daily_Care_Overview.md) | UC-34 Manage Baby Profiles; UC-35 Manage Baby Daily Logs; UC-39 Manage Baby Health Records |
|  | [02_Growth_Development_Tracking.md](./MF03_Baby_Care_Growth_Vaccination/02_Growth_Development_Tracking.md) | UC-36 Manage Development Milestones; UC-37 Manage Baby Growth |
|  | [03_Vaccination_Record_Schedule_Reminder.md](./MF03_Baby_Care_Growth_Vaccination/03_Vaccination_Record_Schedule_Reminder.md) | UC-38 Manage Vaccination Journey |
| MF-04 — Community Q&A & Moderation | [01_Community_Question_Answer_Flow.md](./MF04_Community_QA_Moderation/01_Community_Question_Answer_Flow.md) | UC-11 Browse Community Q&A; UC-40 Manage Community Questions; UC-41 Manage Community Answers |
|  | [02_Content_Moderation_Enforcement_Pipeline.md](./MF04_Community_QA_Moderation/02_Content_Moderation_Enforcement_Pipeline.md) | UC-42 Report Community Content or Account; UC-75 View Moderator Dashboard; UC-76 Review Pending Community Content; UC-77 Monitor Published Community Content; UC-78 Manage Community Reports; UC-79 Apply and Review Moderation Actions; UC-80 Review AI Moderation Assessment; UC-81 Manage Community Topics |
| MF-05 — Verified Expert Network & Contribution | [01_Expert_Verification_Profile_Trust.md](./MF05_Verified_Expert_Network_Contribution/01_Expert_Verification_Profile_Trust.md) | UC-13 Register and Submit Expert Application; UC-14 Manage Expert Professional Profile; UC-82 Review Expert Applications and Trust |
|  | [02_Expert_Directory_Availability.md](./MF05_Verified_Expert_Network_Contribution/02_Expert_Directory_Availability.md) | UC-15 Manage Expert Availability; UC-43 Browse Expert Directory |
|  | [03_Expert_Conversation_Request_Lifecycle.md](./MF05_Verified_Expert_Network_Contribution/03_Expert_Conversation_Request_Lifecycle.md) | UC-16 Process Expert Conversation Requests; UC-44 Manage Own Expert Conversation Requests |
|  | [04_Direct_Chat_Attachment_Voice_Video_Call.md](./MF05_Verified_Expert_Network_Contribution/04_Direct_Chat_Attachment_Voice_Video_Call.md) | UC-17 Use Direct Expert Chat; UC-18 Conduct Direct Voice or Video Call |
| MF-06 — AI Nurse Assistant & Risk Triage | [01_AI_Symptom_Intake_Risk_Triage_Emergency_Handoff.md](./MF06_AI_Nurse_Assistant_Risk_Triage/01_AI_Symptom_Intake_Risk_Triage_Emergency_Handoff.md) | UC-45 Use AI Nurse Symptom Triage; UC-46 View AI Triage History; UC-47 Escalate AI Triage to Emergency Support; UC-48 Request Expert Support from AI Triage |
|  | [02_Approved_Knowledge_RedFlag_Execution.md](./MF06_AI_Nurse_Assistant_Risk_Triage/02_Approved_Knowledge_RedFlag_Execution.md) | UC-83 Manage AI Red-Flag Rules |
| MF-07 — Emergency Map & Nearby Care Support | [01_Emergency_Map_Facility_Route_Navigation.md](./MF07_Emergency_Map_Nearby_Care_Support/01_Emergency_Map_Facility_Route_Navigation.md) | UC-49 Find Nearby Care Facility; UC-50 Call or Navigate to Care Facility |
|  | [02_Emergency_Call_Family_Alert.md](./MF07_Emergency_Map_Nearby_Care_Support/02_Emergency_Call_Family_Alert.md) | UC-51 Call Emergency Number 115; UC-52 Alert Family During Emergency; UC-53 View Emergency or Family Alert |
| MF-08 — Family Sync & Cooperative Care | [01_Care_Group_Invitation_Lifecycle.md](./MF08_Family_Sync_Cooperative_Care/01_Care_Group_Invitation_Lifecycle.md) | UC-54 Manage Care Groups; UC-55 Manage Care Group Invitations; UC-60 Manage Care Group Membership |
|  | [02_Family_Permission_Shared_Visibility.md](./MF08_Family_Sync_Cooperative_Care/02_Family_Permission_Shared_Visibility.md) | UC-56 Manage Family Permissions; UC-57 View Shared Care Data; UC-58 View Shared Care Calendar; UC-61 View Family Alerts |
|  | [03_Family_Care_Task_Assignment.md](./MF08_Family_Sync_Cooperative_Care/03_Family_Care_Task_Assignment.md) | UC-59 Manage Cooperative Care Tasks |
| MF-09 — Verified Content & Checklist Hub | [01_Content_Browse_Consumption.md](./MF09_Verified_Content_Checklist_Hub/01_Content_Browse_Consumption.md) | UC-62 Browse Verified Content and FAQ |
|  | [02_Content_Authoring_Review_Publishing_Lifecycle.md](./MF09_Verified_Content_Checklist_Hub/02_Content_Authoring_Review_Publishing_Lifecycle.md) | UC-84 View Content Administration Workspace; UC-85 Manage Verified Articles; UC-86 Manage Verified FAQs; UC-87 Manage Content Topics; UC-88 Review and Approve Content |
|  | [03_Checklist_Template_Personal_Runtime.md](./MF09_Verified_Content_Checklist_Hub/03_Checklist_Template_Personal_Runtime.md) | UC-63 Manage Personal Care Checklist; UC-89 Manage Checklist Templates |
|  | [04_Exercise_Content_Posture_Configuration.md](./MF09_Verified_Content_Checklist_Hub/04_Exercise_Content_Posture_Configuration.md) | UC-90 Manage Pregnancy Exercise Content; UC-91 Manage Exercise Posture Configuration |
| MF-10 — Smart Activity Monitoring & Safety Support | [01_Monitoring_Configuration_Enable_Disable.md](./MF10_Smart_Activity_Monitoring_Safety_Support/01_Monitoring_Configuration_Enable_Disable.md) | UC-64 Manage Safety Monitoring Settings |
|  | [02_Fall_Detection_Safety_Check_False_Positive_Feedback.md](./MF10_Smart_Activity_Monitoring_Safety_Support/02_Fall_Detection_Safety_Check_False_Positive_Feedback.md) | UC-65 Respond to Suspected Fall or Impact; UC-66 Send Safety Emergency Alert; UC-67 Review Safety Events and Report False Positive; UC-68 Open Emergency Support from Safety Alert |

Total: 10 MF folders and 31 detailed Spec files.

## 4. Diagram convention

Class diagrams use current UI, controller, service, repository, entity and external-service names. Sequence diagrams declare lifelines in call order, number every message, use nested branch numbering, pair synchronous calls with dashed returns and mirror processing with activation bars. HTTP outcomes show status codes. No State Machine Diagram is included.

## 5. Review status

All Specs are Draft. Known implementation gaps such as the Expert Web reject-method mismatch are documented as code gaps rather than redesigned as successful flows.
