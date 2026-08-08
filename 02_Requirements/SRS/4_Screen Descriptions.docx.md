#### ***4.1.2 Screen Descriptions***

This catalogue follows the ten Release 1 MFs and the UC-01 through UC-91 baseline. A row may represent a route, tab, panel, form, detail view or confirmation modal in the accepted role ScreenFlow. These screen groups support the use cases; they do not create additional use cases. Reachable Web and Mobile navigation is used as the final boundary when legacy code contains an unlinked screen.

| STT | Platform | Feature | Screen | Description |
| --- | --- | --- | --- | --- |
| 1 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-01/UC-02 | Mobile Welcome and Authentication Entry Screen | Introduces CareBridge and provides entry to registration or login. |
| 2 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-01 | Register Account Screen | Collects supported identity, credentials and initial role data for account registration. |
| 3 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-01 | OTP Verification Screen | Verifies the registration OTP and activates the pending account; resend remains an embedded subflow. |
| 4 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-02 | Login Screen | Authenticates by a supported credential or Google branch and routes the account to its role workspace. |
| 5 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-04 | Forgot and Reset Password Screens | Starts password recovery and accepts a valid recovery proof before setting a new password. |
| 6 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-02 | Role-Aware Landing Screen | Checks account state, assigned role and onboarding state before opening the Mother, Family or Expert workspace. |
| 7 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-02/UC-10 | Blocked Account and Appeal Screen | Explains the current blocked state and allows one appeal for an active administrative lock episode when the Backend issues a short-lived appeal token; other non-temporary blocked states retain the support-contact fallback. |
| 8 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-03/UC-06/UC-09 | Account Profile and Settings Screen | Shows the authenticated account profile and entry points for editing, account lifecycle actions and logout. |
| 9 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-05/UC-06 | Edit Profile and Change Password Screens | Updates permitted profile fields or changes the password after validating the current credential. |
| 10 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-07 | Notification Center and Notification Detail Screens | Lists the current user's in-app notifications and opens the authorized deep-linked detail. |
| 11 | Common Mobile App | MF-01 Account, Trust & Access Control — UC-08/UC-19 | Privacy Settings and Login Sessions Screens | Manages supported privacy preferences, consent grants and active login-session revocation. |
| 12 | Mother Mobile App | MF-02 Mother Care Journey — UC-20/UC-26/UC-33 | Mother Home Screen | Presents the active journey context, today's care items, recommendations and safe shortcuts. |
| 13 | Mother Mobile App | MF-02 Mother Care Journey — UC-20 | Mother Stage Selection and Journey Setup Screens | Creates the pre-pregnancy, pregnancy or postpartum journey from the selected stage and required dates. |
| 14 | Mother Mobile App | MF-02 Mother Care Journey — UC-20 | Mother Journey and Pregnancy Outcome Screens | Shows journey history and applies supported stage updates or pregnancy-outcome transitions. |
| 15 | Mother Mobile App | MF-02 Mother Care Journey — UC-33 | Recommendation Profile Screen | Collects the minimum preferences used to retrieve stage-appropriate reviewed care recommendations. |
| 16 | Mother Mobile App | MF-02 Mother Care Journey — UC-21 | Maternal Health Metrics and Trend Screens | Creates, updates and reviews supported maternal measurements and non-diagnostic trends. |
| 17 | Mother Mobile App | MF-02 Mother Care Journey — UC-31 | Quick Health Notes Screen | Provides fast entry for BMI-related values, hydration and fetal-movement observations. |
| 18 | Mother Mobile App | MF-02 Mother Care Journey — UC-32 | EPDS Screening Screen | Collects EPDS answers, calculates the screening score and shows the appropriate safety guidance and history. |
| 19 | Mother Mobile App | MF-02 Mother Care Journey — UC-22 | Postpartum Log List, Form and Detail Screens | Creates, updates, reviews and removes postpartum recovery observations for the active journey. |
| 20 | Mother Mobile App | MF-02 Mother Care Journey — UC-23 | Health Record Timeline, Form and Detail Screens | Manages maternal health-record metadata and protected attachments and archives supported records. |
| 21 | Mother Mobile App | MF-02 Mother Care Journey — UC-24 | Appointment Calendar, Form and Detail Screens | Lists and manages appointments and opens shared appointment details when the care-group scope permits. |
| 22 | Mother Mobile App | MF-02 Mother Care Journey — UC-25 | Reminder Schedule, Form and Detail Screens | Creates and manages general, medication and vaccination reminders, including supported update and snooze actions. |
| 23 | Mother Mobile App | MF-02 Mother Care Journey — UC-26 | Today Care Tasks and Plan Screen | Combines due reminders and care tasks for the current day without creating a separate planning feature. |
| 24 | Mother Mobile App | MF-02 Mother Care Journey — UC-27 | Pregnancy Exercise Library and Detail Screens | Browses published pregnancy exercises and reviews instructions, duration, difficulty and safety notes. |
| 25 | Mother Mobile App | MF-02 Mother Care Journey — UC-28 | Pre-Exercise Safety Check Screen | Collects the configured safety answers and blocks or cautions before a session when required. |
| 26 | Mother Mobile App | MF-02 Mother Care Journey — UC-29 | Exercise Session and Optional Camera Posture Screens | Runs the exercise session and, on supported platforms after confirmation, submits sampled posture landmarks for bounded feedback. |
| 27 | Mother Mobile App | MF-02 Mother Care Journey — UC-30 | Exercise History and Session Result Screens | Lists completed sessions and shows duration, completion and available posture-result information. |
| 28 | Mother Mobile App | MF-03 Baby Care Journey, Growth & Vaccination — UC-34 | Baby Profiles, Detail and Form Screens | Creates, updates, archives and selects the active baby profile owned by the Mother. |
| 29 | Mother Mobile App | MF-03 Baby Care Journey, Growth & Vaccination — UC-34/UC-35/UC-39 | Baby Care Hub Screen | Shows the selected baby's profile, recent care logs, health records and entry points to growth, milestones and vaccination. |
| 30 | Mother Mobile App | MF-03 Baby Care Journey, Growth & Vaccination — UC-35 | Baby Daily Log and Summary Screens | Creates, edits and reviews feeding, sleep, diaper and supported daily-care observations and summaries. |
| 31 | Mother Mobile App | MF-03 Baby Care Journey, Growth & Vaccination — UC-36 | Development Milestone Screens | Records, edits, reviews and removes caregiver-observed milestones for the selected baby. |
| 32 | Mother Mobile App | MF-03 Baby Care Journey, Growth & Vaccination — UC-37 | Growth Measurement History, Form and Detail Screens | Manages weight, length or height and head-circumference measurements and renders the remaining valid history. |
| 33 | Mother Mobile App | MF-03 Baby Care Journey, Growth & Vaccination — UC-38 | Vaccination Schedule, Record and Detail Screens | Shows the reference schedule and manages user-entered vaccination records and related reminder entry points. |
| 34 | Mother Mobile App | MF-03 Baby Care Journey, Growth & Vaccination — UC-39 | Baby Health Record Screens | Uses the protected health-record flow with the selected baby scope to create, update, view and archive baby records. |
| 35 | Mother and Family Mobile App | MF-04 Community Q&A & Moderation — UC-11 | Community Feed Screen | Browses visible moderated questions by topic, search and supported filters. |
| 36 | Mother and Family Mobile App | MF-04 Community Q&A & Moderation — UC-40 | My Questions and Create or Edit Question Screens | Lists the actor's questions and creates, updates or deletes a question with optional anonymous public display. |
| 37 | Mother and Family Mobile App | MF-04 Community Q&A & Moderation — UC-11 | Saved Community Questions Screen | Lists bookmarked visible questions for the authenticated user. |
| 38 | Mother and Family Mobile App | MF-04 Community Q&A & Moderation — UC-41 | Community Question Detail and Answer Screens | Shows the visible answer thread and lets the authenticated Mother or Family member manage an allowed answer. |
| 39 | Mother and Family Mobile App | MF-04 Community Q&A & Moderation — UC-42 | Community Report Form | Submits a report about a question, answer or account for moderator review. |
| 40 | Mother and Family Mobile App | MF-05 Verified Expert Network & Contribution — UC-43 | Verified Expert Directory and Public Profile Screens | Lists currently verified experts and shows their public professional profile and availability information. |
| 41 | Mother and Family Mobile App | MF-05 Verified Expert Network & Contribution — UC-44 | My Expert Conversation Requests Screens | Creates, lists, reviews or cancels the actor's expert conversation request. |
| 42 | Mother, Family and Expert Mobile App | MF-05 Verified Expert Network & Contribution — UC-16/UC-17/UC-18 | Direct Conversation List and Chat Room Screens | Opens an accepted participant-only conversation for text, image or file messages and authorized voice or video calls. |
| 43 | Mother Mobile App | MF-06 AI Nurse Assistant & Risk Triage — UC-45 | AI Symptom Intake Screen | Collects the structured symptom answers and consented context used for non-diagnostic risk triage. |
| 44 | Mother Mobile App | MF-06 AI Nurse Assistant & Risk Triage — UC-46 | Triage Result and History Screens | Shows GREEN, YELLOW or RED orientation, evidence-limited guidance and the user's prior triage sessions. |
| 45 | Mother Mobile App | MF-06 AI Nurse Assistant & Risk Triage — UC-47/UC-48 | AI Emergency and Expert Handoff Screens | Starts the supported emergency-map handoff or prepares an expert conversation request from a completed intake. |
| 46 | Mother Mobile App | MF-07 Emergency Map & Nearby Care Support — UC-49/UC-50/UC-51 | Emergency Map and Nearby Care Facilities Screen | Searches nearby care facilities, shows provider-limited route information and exposes device call or navigation actions including 115. |
| 47 | Mother and Family Mobile App | MF-07 Emergency Map & Nearby Care Support — UC-52/UC-53 | Emergency and Family Alert Detail Screens | Shows the minimum authorized emergency-session information and family-alert status; it does not claim dispatch or arrival. |
| 48 | Mother Mobile App | MF-08 Family Sync & Cooperative Care — UC-54 | Care Groups and Care Group Detail Screens | Creates, lists and deletes Mother-owned care groups and opens the selected group workspace. |
| 49 | Mother Mobile App | MF-08 Family Sync & Cooperative Care — UC-55 | Invite Family Member and Pending Invitations Screens | Creates or revokes pending invitations for a Mother-owned care group. |
| 50 | Mother Mobile App | MF-08 Family Sync & Cooperative Care — UC-56/UC-60 | Care Group Members and Family Permission Screens | Lists accepted members, changes granted categories and removes a member when authorized. |
| 51 | Mother and Family Mobile App | MF-08 Family Sync & Cooperative Care — UC-59 | Cooperative Care Task Screens | Creates, assigns, updates, cancels or completes care-group tasks according to group membership and ownership rules. |
| 52 | Family Mobile App | MF-08 Family Sync & Cooperative Care — UC-55 | Care Group Invitation Screen | Lets the invited Family member inspect, accept or decline an active invitation. |
| 53 | Family Mobile App | MF-08 Family Sync & Cooperative Care — UC-57/UC-58 | Family Home and Shared Care Data Screens | Shows only the Mother journey, baby, health, checklist and calendar categories granted to the active member. |
| 54 | Family Mobile App | MF-08 Family Sync & Cooperative Care — UC-61 | Family Alerts Screen | Lists authorized care-group and emergency alerts and opens the corresponding detail. |
| 55 | Mother and Family Mobile App | MF-09 Verified Content & Checklist Hub — UC-62 | Verified Content Hub and Content Detail Screens | Browses approved articles and FAQs and shows their source, update and safety information. |
| 56 | Mother and Family Mobile App | MF-09 Verified Content & Checklist Hub — UC-63 | Current Checklist, Detail and History Screens | Shows distributed personal or shared checklist tasks, applies supported actions and preserves prior history. |
| 57 | Mother Mobile App | MF-10 Smart Activity Monitoring & Safety Support — UC-64 | Safety Monitoring Settings Screen | Shows the monitoring state, emergency contacts, sensor readiness and enable or disable controls. |
| 58 | Mother Mobile App | MF-10 Smart Activity Monitoring & Safety Support — UC-64 | Enable Fall Detection Confirmation Screen | Requests the supported permissions and explicit confirmation before starting phone-IMU monitoring. |
| 59 | Mother Mobile App | MF-10 Smart Activity Monitoring & Safety Support — UC-65/UC-66 | Suspected Fall Safety Countdown Screen | Asks whether the Mother is safe and sends the supported emergency alert only after need-help or timeout handling. |
| 60 | Mother Mobile App | MF-10 Smart Activity Monitoring & Safety Support — UC-67/UC-68 | Safety Event History and Emergency Support Screens | Reviews suspected events, records false-positive feedback and opens the MF-07 emergency-support flow. |
| 61 | Expert Mobile App | MF-05 Verified Expert Network & Contribution — UC-13 | Expert Onboarding, Identity and Profile Setup Screens | Collects the professional profile, identity evidence and verification documents required for an expert application. |
| 62 | Expert Mobile App | MF-05 Verified Expert Network & Contribution — UC-13/UC-14 | Verification Status, Documents and Renewal Screens | Shows the current verification decision and supports document maintenance or a permitted renewal submission. |
| 63 | Expert Mobile App | MF-05 Verified Expert Network & Contribution — UC-14 | Expert Home and Professional Profile Screens | Shows expert work queues and lets the expert maintain the permitted public professional profile. |
| 64 | Expert Mobile App | MF-05 Verified Expert Network & Contribution — UC-15 | Expert Mobile Availability Calendar Screen | Lists, adds and removes supported availability slots. |
| 65 | Expert Mobile App | MF-04/MF-05 — UC-12 | Expert Question Queue, Question Detail and Answer Screens | Lists questions available for verified contribution and posts or edits an answer carrying the verified-expert label. |
| 66 | Expert Mobile App | MF-05 Verified Expert Network & Contribution — UC-16 | Expert Conversation Request Queue and Detail Screens | Lets the assigned expert accept or reject a request before a direct conversation becomes available. |
| 67 | Common Web App | MF-01 Account, Trust & Access Control — UC-01/UC-02 | Web Registration, Login and OTP Screens | Provides the currently routed web authentication entry for Expert and staff roles and routes unsupported roles away from portal workspaces. |
| 68 | Common Web App | MF-01 Account, Trust & Access Control — UC-02/UC-10 | Forbidden, Blocked, Appeal, No-Web-Access and Maintenance Screens | Explains authorization, account-state or environment restrictions and allows a token-authorized administrative-lock appeal without exposing protected portal content. |
| 69 | Common Web App | MF-01 Account, Trust & Access Control — UC-05/UC-06/UC-19 | Web Account Profile and Session Settings Screens | Shows and updates the authenticated portal account, password and supported session controls. |
| 70 | Verified Expert Web Portal | MF-05 Verified Expert Network & Contribution — UC-13 | Expert Portal Onboarding Screen | Captures the initial professional application and gates the remaining expert portal until the onboarding state allows access. |
| 71 | Verified Expert Web Portal | MF-05 Verified Expert Network & Contribution — UC-14/UC-15/UC-16 | Expert Portal Dashboard | Summarizes consultation requests, question queue, availability, conversations and profile status. |
| 72 | Verified Expert Web Portal | MF-05 Verified Expert Network & Contribution — UC-14 | Expert Professional Profile and Verification Documents Screens | Maintains professional information, certificates and verification evidence. |
| 73 | Verified Expert Web Portal | MF-05 Verified Expert Network & Contribution — UC-15 | Expert Availability Calendar Screen | Lists and manages the expert's available working slots. |
| 74 | Verified Expert Web Portal | MF-04/MF-05 — UC-12 | Expert Question Queue and Answer Editor Screens | Opens a community question and submits a verified-expert answer; experts cannot create questions. |
| 75 | Verified Expert Web Portal | MF-05 Verified Expert Network & Contribution — UC-16 | Expert Consultation Request Screens | Lists assigned requests and records accept or reject decisions. |
| 76 | Verified Expert Web Portal | MF-05 Verified Expert Network & Contribution — UC-17/UC-18 | Expert Direct Conversation and Call Screens | Supports participant-only chat, attachments and authorized voice or video calling after request acceptance. |
| 77 | Moderator Web Portal | MF-04 Community Q&A & Moderation — UC-75 | Moderator Dashboard | Summarizes pending content, reports, violations and moderation workload. |
| 78 | Moderator Web Portal | MF-04 Community Q&A & Moderation — UC-76 | Pending Community Content Queue and Detail Screens | Reviews content awaiting manual moderation and applies an authorized decision. |
| 79 | Moderator Web Portal | MF-04 Community Q&A & Moderation — UC-77 | Published Community Content Monitor and Post Detail Screens | Searches visible community content and opens the moderation-aware detail. |
| 80 | Moderator Web Portal | MF-04 Community Q&A & Moderation — UC-78/UC-80 | Community Reports Queue and Report Detail Screens | Claims, releases and resolves content or account reports and reviews available automated assessment. |
| 81 | Moderator Web Portal | MF-04 Community Q&A & Moderation — UC-79 | Violation History and Violation Detail Screens | Reviews account or content enforcement history and the evidence for an authorized moderation action or reversal. |
| 82 | Moderator and Content Admin Web Portal | MF-04 Community Q&A & Moderation — UC-81 | Community Topic Management Screen | Lists and maintains the topic catalogue used by community questions. |
| 83 | Content Admin Web Portal | MF-09 Verified Content & Checklist Hub — UC-84 | Content Administration Dashboard and Library Screens | Summarizes and filters the content administration workspace across supported content types. |
| 84 | Content Admin Web Portal | MF-09 Verified Content & Checklist Hub — UC-85 | Article List, Editor, Detail and Version Screens | Creates and maintains article drafts, media and versions and submits or hides content through the supported lifecycle. |
| 85 | Content Admin Web Portal | MF-09 Verified Content & Checklist Hub — UC-86 | FAQ List, Editor, Detail and Version Screens | Creates and maintains FAQ content and version history. |
| 86 | Content Admin Web Portal | MF-09 Verified Content & Checklist Hub — UC-89 | Checklist Template List, Form, Detail and Version Screens | Creates, updates and versions checklist templates before approved distribution. |
| 87 | Content Admin Web Portal | MF-09 Verified Content & Checklist Hub — UC-90 | Pregnancy Exercise Content List, Editor, Detail and Preview Screens | Creates and maintains published exercise metadata, safety guidance and media. |
| 88 | Content Admin Web Portal | MF-09 Verified Content & Checklist Hub — UC-87 | Content Topic Management Screen | Maintains the content taxonomy and topic mappings used by articles, FAQs and checklists. |
| 89 | Content Admin Web Portal | MF-09 Verified Content & Checklist Hub — UC-84/UC-85/UC-86 | Content Workflow Notification Screen | Shows content review and system notifications relevant to the Content Admin. |
| 90 | System Admin Web Portal | MF-01 Account, Trust & Access Control — UC-69 | System Administration Dashboard | Shows account, expert, content, safety and security administration summaries and links. |
| 91 | System Admin Web Portal | MF-01 Account, Trust & Access Control — UC-70 | User List, User Detail and Role Update Screens | Searches accounts and applies supported status or role governance actions. |
| 92 | System Admin Web Portal | MF-01 Account, Trust & Access Control — UC-72 | Account Lock Appeal Queue and Review Detail Screens | Filters pending or completed appeals, opens the lock episode and submitted reason, and records an audited approve or reject decision; approval clears the matching active administrative lock. |
| 93 | System Admin Web Portal | MF-01 Account, Trust & Access Control — UC-71 | Create Staff Account Screen | Creates a supported Moderator or Content Admin staff account under admin governance. |
| 94 | System Admin Web Portal | MF-05 Verified Expert Network & Contribution — UC-82 | Expert List, Verification Queue and Expert Detail Screens | Reviews expert applications and records approve, reject or supported trust decisions. |
| 95 | System Admin Web Portal | MF-09 Verified Content & Checklist Hub — UC-88 | Content Approval Queue and Read-Only Review Screens | Reviews submitted article, FAQ or checklist content and records the approval decision without exposing Content Admin editing actions. |
| 96 | System Admin Web Portal | MF-01 Account, Trust & Access Control — UC-73 | Security Events and Event Detail Screens | Searches security and audit events and opens their correlation and evidence detail. |
| 97 | System Admin Web Portal | MF-01 Account, Trust & Access Control — UC-73 | Security Incident Investigation and Resolution Screens | Groups related security events, records investigation work and appends the final resolution. |
| 98 | System Admin Web Portal | MF-06 AI Nurse Assistant & Risk Triage — UC-83 | AI Red-Flag Rule Management Screen | Lists and manages authorized red-flag rules while preserving protected defaults and audit evidence. |
| 99 | System Admin Web Portal | MF-01 Account, Trust & Access Control — UC-74 | System Configuration Screen | Reviews and updates the supported runtime configuration values through the audited admin path. |
| 100 | System Admin Web Portal | MF-09 Verified Content & Checklist Hub — UC-91 | Posture Analysis Configuration List, Detail and Editor Screens | Creates and activates versioned posture-analysis configuration for a published exercise. |
| 101 | System Admin Web Portal | MF-01 Account, Trust & Access Control — UC-07 | Admin Notification and Privacy Settings Screens | Shows administration notifications and supported privacy configuration for the authenticated System Admin. |

**Table 254: Screen Descriptions**

#### ***4.1.3 Screen Authorization***

An `X` identifies a role that may enter the screen group through its supported platform flow. Authentication, account state, expert onboarding or verification, ownership, care-group membership and fine-grained permission checks still apply. Hiding a route or navigation item is not authorization; the Backend remains the enforcement authority.

| Screen | Guest | Mother | Family Member | Expert Applicant / Verified Expert | Moderator | Content Admin | System Admin |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Mobile Welcome and Authentication Entry Screen | X | X | X | X |  |  |  |
| Register Account Screen | X | X | X | X |  |  |  |
| OTP Verification Screen | X | X | X | X |  |  |  |
| Login Screen | X | X | X | X |  |  |  |
| Forgot and Reset Password Screens | X | X | X | X |  |  |  |
| Role-Aware Landing Screen |  | X | X | X |  |  |  |
| Blocked Account and Appeal Screen | X | X | X | X |  |  |  |
| Account Profile and Settings Screen |  | X | X | X |  |  |  |
| Edit Profile and Change Password Screens |  | X | X | X |  |  |  |
| Notification Center and Notification Detail Screens |  | X | X | X |  |  |  |
| Privacy Settings and Login Sessions Screens |  | X | X | X |  |  |  |
| Mother Home Screen |  | X |  |  |  |  |  |
| Mother Stage Selection and Journey Setup Screens |  | X |  |  |  |  |  |
| Mother Journey and Pregnancy Outcome Screens |  | X |  |  |  |  |  |
| Recommendation Profile Screen |  | X |  |  |  |  |  |
| Maternal Health Metrics and Trend Screens |  | X |  |  |  |  |  |
| Quick Health Notes Screen |  | X |  |  |  |  |  |
| EPDS Screening Screen |  | X |  |  |  |  |  |
| Postpartum Log List, Form and Detail Screens |  | X |  |  |  |  |  |
| Health Record Timeline, Form and Detail Screens |  | X |  |  |  |  |  |
| Appointment Calendar, Form and Detail Screens |  | X |  |  |  |  |  |
| Reminder Schedule, Form and Detail Screens |  | X |  |  |  |  |  |
| Today Care Tasks and Plan Screen |  | X |  |  |  |  |  |
| Pregnancy Exercise Library and Detail Screens |  | X |  |  |  |  |  |
| Pre-Exercise Safety Check Screen |  | X |  |  |  |  |  |
| Exercise Session and Optional Camera Posture Screens |  | X |  |  |  |  |  |
| Exercise History and Session Result Screens |  | X |  |  |  |  |  |
| Baby Profiles, Detail and Form Screens |  | X |  |  |  |  |  |
| Baby Care Hub Screen |  | X |  |  |  |  |  |
| Baby Daily Log and Summary Screens |  | X |  |  |  |  |  |
| Development Milestone Screens |  | X |  |  |  |  |  |
| Growth Measurement History, Form and Detail Screens |  | X |  |  |  |  |  |
| Vaccination Schedule, Record and Detail Screens |  | X |  |  |  |  |  |
| Baby Health Record Screens |  | X |  |  |  |  |  |
| Community Feed Screen |  | X | X |  |  |  |  |
| My Questions and Create or Edit Question Screens |  | X | X |  |  |  |  |
| Saved Community Questions Screen |  | X | X |  |  |  |  |
| Community Question Detail and Answer Screens |  | X | X |  |  |  |  |
| Community Report Form |  | X | X |  |  |  |  |
| Verified Expert Directory and Public Profile Screens |  | X | X |  |  |  |  |
| My Expert Conversation Requests Screens |  | X | X |  |  |  |  |
| Direct Conversation List and Chat Room Screens |  | X | X | X |  |  |  |
| AI Symptom Intake Screen |  | X |  |  |  |  |  |
| Triage Result and History Screens |  | X |  |  |  |  |  |
| AI Emergency and Expert Handoff Screens |  | X |  |  |  |  |  |
| Emergency Map and Nearby Care Facilities Screen |  | X |  |  |  |  |  |
| Emergency and Family Alert Detail Screens |  | X | X |  |  |  |  |
| Care Groups and Care Group Detail Screens |  | X |  |  |  |  |  |
| Invite Family Member and Pending Invitations Screens |  | X |  |  |  |  |  |
| Care Group Members and Family Permission Screens |  | X |  |  |  |  |  |
| Cooperative Care Task Screens |  | X | X |  |  |  |  |
| Care Group Invitation Screen |  |  | X |  |  |  |  |
| Family Home and Shared Care Data Screens |  |  | X |  |  |  |  |
| Family Alerts Screen |  |  | X |  |  |  |  |
| Verified Content Hub and Content Detail Screens |  | X | X |  |  |  |  |
| Current Checklist, Detail and History Screens |  | X | X |  |  |  |  |
| Safety Monitoring Settings Screen |  | X |  |  |  |  |  |
| Enable Fall Detection Confirmation Screen |  | X |  |  |  |  |  |
| Suspected Fall Safety Countdown Screen |  | X |  |  |  |  |  |
| Safety Event History and Emergency Support Screens |  | X |  |  |  |  |  |
| Expert Onboarding, Identity and Profile Setup Screens |  |  |  | X |  |  |  |
| Verification Status, Documents and Renewal Screens |  |  |  | X |  |  |  |
| Expert Home and Professional Profile Screens |  |  |  | X |  |  |  |
| Expert Mobile Availability Calendar Screen |  |  |  | X |  |  |  |
| Expert Question Queue, Question Detail and Answer Screens |  |  |  | X |  |  |  |
| Expert Conversation Request Queue and Detail Screens |  |  |  | X |  |  |  |
| Web Registration, Login and OTP Screens | X |  |  | X | X | X | X |
| Forbidden, Blocked, Appeal, No-Web-Access and Maintenance Screens | X |  |  | X | X | X | X |
| Web Account Profile and Session Settings Screens |  |  |  | X | X | X | X |
| Expert Portal Onboarding Screen |  |  |  | X |  |  |  |
| Expert Portal Dashboard |  |  |  | X |  |  |  |
| Expert Professional Profile and Verification Documents Screens |  |  |  | X |  |  |  |
| Expert Availability Calendar Screen |  |  |  | X |  |  |  |
| Expert Question Queue and Answer Editor Screens |  |  |  | X |  |  |  |
| Expert Consultation Request Screens |  |  |  | X |  |  |  |
| Expert Direct Conversation and Call Screens |  |  |  | X |  |  |  |
| Moderator Dashboard |  |  |  |  | X |  |  |
| Pending Community Content Queue and Detail Screens |  |  |  |  | X |  |  |
| Published Community Content Monitor and Post Detail Screens |  |  |  |  | X |  |  |
| Community Reports Queue and Report Detail Screens |  |  |  |  | X |  |  |
| Violation History and Violation Detail Screens |  |  |  |  | X |  |  |
| Community Topic Management Screen |  |  |  |  | X | X |  |
| Content Administration Dashboard and Library Screens |  |  |  |  |  | X |  |
| Article List, Editor, Detail and Version Screens |  |  |  |  |  | X |  |
| FAQ List, Editor, Detail and Version Screens |  |  |  |  |  | X |  |
| Checklist Template List, Form, Detail and Version Screens |  |  |  |  |  | X |  |
| Pregnancy Exercise Content List, Editor, Detail and Preview Screens |  |  |  |  |  | X |  |
| Content Topic Management Screen |  |  |  |  |  | X |  |
| Content Workflow Notification Screen |  |  |  |  |  | X |  |
| System Administration Dashboard |  |  |  |  |  |  | X |
| User List, User Detail and Role Update Screens |  |  |  |  |  |  | X |
| Account Lock Appeal Queue and Review Detail Screens |  |  |  |  |  |  | X |
| Create Staff Account Screen |  |  |  |  |  |  | X |
| Expert List, Verification Queue and Expert Detail Screens |  |  |  |  |  |  | X |
| Content Approval Queue and Read-Only Review Screens |  |  |  |  |  |  | X |
| Security Events and Event Detail Screens |  |  |  |  |  |  | X |
| Security Incident Investigation and Resolution Screens |  |  |  |  |  |  | X |
| AI Red-Flag Rule Management Screen |  |  |  |  |  |  | X |
| System Configuration Screen |  |  |  |  |  |  | X |
| Posture Analysis Configuration List, Detail and Editor Screens |  |  |  |  |  |  | X |
| Admin Notification and Privacy Settings Screens |  |  |  |  |  |  | X |

**Table 255: Screen Authorization**

#### ***4.1.4 Non-Screen Functions***

Non-screen functions are background, policy, integration, persistence or coordination behaviors invoked by the 91 use cases. They are not separate actor goals and therefore do not introduce additional use cases.

| STT | Feature | System Function | Description |
| :---: | --- | --- | --- |
| 1 | MF-01 Account, Trust & Access Control | OTP and Account Activation Processing | Generates expiring OTP challenges, enforces attempts and resend limits, activates verified accounts and records security outcomes. |
| 2 | MF-01 Account, Trust & Access Control | Credential and Federated Authentication | Validates password or supported Firebase identity, checks account state and creates the CareBridge authentication result. |
| 3 | MF-01 Account, Trust & Access Control | Access Token, Refresh Token and Session Management | Issues, rotates, revokes and expires access, refresh and user-session state and supports logout or selected-session revocation. |
| 4 | MF-01 Account, Trust & Access Control | Password Recovery and Credential Revocation | Creates single-use password-reset proof, applies rate limits, changes the password and revokes affected active credentials. |
| 5 | MF-01 Account, Trust & Access Control | RBAC and Account-State Enforcement | Rechecks role, ownership, membership, account status and request context before protected Backend operations. |
| 6 | MF-01 Account, Trust & Access Control | Consent Grant Validation | Stores and rechecks supported purpose, scope, recipient and expiry conditions and blocks revoked or expired access. |
| 7 | MF-01 Account, Trust & Access Control | Account Lock Appeal and Administrative Governance | Creates a short-lived appeal capability only after credential verification, accepts at most one appeal for the active administrative lock episode, exposes the System Admin review queue and applies an audited approve or reject decision. |
| 8 | MF-02 Mother Care Journey | Mother Journey Lifecycle Processing | Creates the canonical Mother journey, validates stage transitions and preserves journey and pregnancy-outcome history. |
| 9 | MF-02 Mother Care Journey | Journey Dashboard and Recommendation Assembly | Combines current stage, due care items and approved content recommendations without creating clinical advice. |
| 10 | MF-02 Mother Care Journey | Maternal Metric and Trend Processing | Validates supported metric values and times, persists changes and calculates scoped trend series. |
| 11 | MF-02 Mother Care Journey | Quick Note and EPDS Processing | Stores hydration, BMI-related, fetal-movement and EPDS observations and produces the configured non-diagnostic guidance. |
| 12 | MF-02 Mother Care Journey | Postpartum Log Lifecycle Processing | Validates the active journey and manages postpartum observations while retaining required audit evidence. |
| 13 | MF-02 Mother Care Journey | Health Record and Protected Attachment Processing | Validates ownership and file purpose, stores health-record metadata and authorizes short-lived attachment access. |
| 14 | MF-02 Mother Care Journey | Appointment, Reminder and Today-Task Scheduling | Creates and updates schedules, derives today's due items and coordinates delivery jobs without duplicating confirmed actions. |
| 15 | MF-02 Mother Care Journey | Exercise Eligibility and Safety Evaluation | Returns published exercises and evaluates the configured pre-exercise answers before a session begins. |
| 16 | MF-02 Mother Care Journey | Exercise Session and Posture Analysis Processing | Tracks session state and processes sampled landmarks through the configured posture rules without retaining camera video. |
| 17 | MF-03 Baby Care Journey, Growth & Vaccination | Active Baby Scope Enforcement | Resolves the selected baby and rechecks Mother ownership before baby-scoped reads or mutations. |
| 18 | MF-03 Baby Care Journey, Growth & Vaccination | Baby Profile and Daily Log Lifecycle Processing | Manages baby profiles and daily logs and excludes archived or removed state from normal views. |
| 19 | MF-03 Baby Care Journey, Growth & Vaccination | Milestone and Growth History Processing | Validates caregiver-entered milestones and growth measurements and rebuilds the remaining authorized history. |
| 20 | MF-03 Baby Care Journey, Growth & Vaccination | Vaccination Schedule and Reminder Processing | Combines the reference schedule with user-entered vaccination records and schedules supported reminders. |
| 21 | MF-04 Community Q&A & Moderation | Community Feed and Anonymous Identity Projection | Returns only visible questions and masks anonymous author identity from public viewers while retaining moderator accountability. |
| 22 | MF-04 Community Q&A & Moderation | Question and Answer Lifecycle Processing | Enforces author ownership and moderation locks for create, update and delete actions and labels verified-expert answers. |
| 23 | MF-04 Community Q&A & Moderation | Community Report and Moderation Case Processing | Creates reports, supports claim or release and records authorized resolution and moderation actions. |
| 24 | MF-04 Community Q&A & Moderation | Automated Moderation Assessment | Processes supported content asynchronously, stores bounded assessment evidence and routes uncertain cases to human moderation. |
| 25 | MF-04 Community Q&A & Moderation | Community Topic Catalogue Processing | Maintains the active topic catalogue used by question creation, filtering and moderator or Content Admin management. |
| 26 | MF-05 Verified Expert Network & Contribution | Expert Application and Verification Processing | Stores professional profile and credential evidence and applies audited approval, rejection and renewal decisions. |
| 27 | MF-05 Verified Expert Network & Contribution | Verified Expert Directory Projection | Returns only eligible verified expert profiles and their currently supported public professional and availability data. |
| 28 | MF-05 Verified Expert Network & Contribution | Expert Availability Processing | Validates and stores the expert's supported working slots for directory and request context. |
| 29 | MF-05 Verified Expert Network & Contribution | Conversation Request State Processing | Creates requester-owned requests and permits only the assigned expert to accept or reject before conversation access. |
| 30 | MF-05 Verified Expert Network & Contribution | Direct Message and Attachment Processing | Enforces participant-only access, idempotent message submission, authorized attachment access and recipient notification. |
| 31 | MF-05 Verified Expert Network & Contribution | Voice and Video Call Signaling | Creates call state and short-lived ZegoCloud joining credentials only for authorized conversation participants. |
| 32 | MF-06 AI Nurse Assistant & Risk Triage | Triage Consent and Structured Intake Processing | Gates elective intake with the active disclaimer consent and converts answers into the canonical structured session. |
| 33 | MF-06 AI Nurse Assistant & Risk Triage | Red-Flag Risk Floor and Triage Workflow | Applies approved GREEN, YELLOW and RED rules, prevents unsafe risk downgrades and keeps the result non-diagnostic. |
| 34 | MF-06 AI Nurse Assistant & Risk Triage | Approved Evidence and Response Validation | Validates internal-service credentials, response shape, evidence references and safe fallback before accepting AI output. |
| 35 | MF-06 AI Nurse Assistant & Risk Triage | Triage History and Handoff Processing | Stores completed results and creates supported emergency or expert-request handoffs without dispatching care. |
| 36 | MF-07 Emergency Map & Nearby Care Support | Nearby Facility and Route Integration | Validates coordinates, calls the TrackAsia boundary with timeout handling and returns provider-limited facility and route information. |
| 37 | MF-07 Emergency Map & Nearby Care Support | Emergency Session and 115 Call Handoff | Creates or reuses the emergency session and opens the device call action without claiming that assistance was dispatched. |
| 38 | MF-07 Emergency Map & Nearby Care Support | Family Emergency Alert Distribution | Sends the minimum emergency context to authorized care-group members and records alert state and delivery evidence. |
| 39 | MF-08 Family Sync & Cooperative Care | Care Group Invitation and Membership Processing | Creates, accepts, declines or revokes invitations and immediately re-evaluates membership after removal or leave. |
| 40 | MF-08 Family Sync & Cooperative Care | Family Permission Enforcement | Rechecks accepted membership and exact granted categories before returning Mother or baby care information. |
| 41 | MF-08 Family Sync & Cooperative Care | Shared Care Dashboard Assembly | Builds the Family view from only the permitted journey, health, baby, calendar, checklist, task and alert projections. |
| 42 | MF-08 Family Sync & Cooperative Care | Cooperative Care Task Processing | Validates assignment and status transitions, persists task changes and notifies the authorized assignee. |
| 43 | MF-09 Verified Content & Checklist Hub | Content Authoring, Version and Approval Processing | Maintains article and FAQ drafts, immutable history and approval state and exposes only approved visible content. |
| 44 | MF-09 Verified Content & Checklist Hub | Checklist Template Version and Distribution Processing | Versions approved templates, materializes eligible instances and prevents duplicate distribution. |
| 45 | MF-09 Verified Content & Checklist Hub | Current Checklist Action Processing | Applies complete or reopen actions with an idempotency key and returns the confirmed current task state. |
| 46 | MF-09 Verified Content & Checklist Hub | Exercise Content Publication Processing | Validates exercise content and media before published exercises become available to the Mother catalogue. |
| 47 | MF-09 Verified Content & Checklist Hub | Posture Configuration Version Processing | Creates, validates and activates the applicable posture-analysis configuration version for an exercise. |
| 48 | MF-10 Smart Activity Monitoring & Safety Support | Sensor Permission and Monitoring Configuration | Keeps monitoring off by default and starts or stops supported phone-IMU collection according to permission and active configuration. |
| 49 | MF-10 Smart Activity Monitoring & Safety Support | On-Device IMU Sampling and Suspected Event Processing | Samples accelerometer and gyroscope input, detects a suspected event and sends bounded event data rather than retaining continuous raw streams. |
| 50 | MF-10 Smart Activity Monitoring & Safety Support | Safety Countdown and First-Response Processing | Reconciles safe, need-help or timeout outcomes so the first valid response wins and duplicate escalation is prevented. |
| 51 | MF-10 Smart Activity Monitoring & Safety Support | Safety Alert and False-Positive Processing | Sends the supported emergency alert, records delivery state and stores authorized false-positive feedback and event history. |
| 52 | Cross-Cutting | Audit and Correlation Processing | Records sensitive actor, action, target, time, result and reason evidence and links related events without exposing raw secrets. |
| 53 | Cross-Cutting | Notification Scheduling and Delivery | Creates in-app and FCM notification records, processes due jobs with bounded retry and prevents duplicate recipient side effects. |
| 54 | Cross-Cutting | Private File Storage and Access | Routes supported files to protected storage, applies purpose and ownership policies and issues short-lived authorized access. |
| 55 | Cross-Cutting | Safe External-Service Degradation | Applies timeout, response validation and safe fallback for AI, map, storage, notification and call integrations. |

**Table 256: Non-Screen Functions**
