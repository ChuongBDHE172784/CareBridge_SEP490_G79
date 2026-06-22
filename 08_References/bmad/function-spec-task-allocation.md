
# Function Spec Task Allocation

Source: `01_Requirements/SRS/Report3_Software Requirement Specification.docx.md`, section `3. Functional Specifications`.

This document maps functional specifications to five development owners and a sprint-oriented implementation order. The goal is to reduce merge conflicts by assigning vertical slices: backend package, API contract, UI screens, integration mock/provider, and tests belong to the same domain owner.

## Ownership Summary

| Member | Ownership | Main Boundary |
|---|---|---|
| TV1 | Shared Foundation | Authentication, account/profile, privacy, notifications, audit/security, shared contracts |
| TV2 | Care Journey | Mother/baby journey (care-journey, baby-care), health records (health-record), family sync (care-coordination), reminders, vaccination, files, device health data |
| TV3 | Community & Content | Community Q&A (community), content/checklist, moderation, partner governance, RAG knowledge |
| TV4 | Expert Consultation | Expert profile (expert), booking, consultation, pricing, payment (consultation), commission, realtime |
| TV5 | AI, Location & Safety | AI triage (ai-safety), map/location (partner-location), emergency flow, safety monitoring, exercise/posture (exercise-posture) |

## Shared Development Rules

- TV1 owns shared contracts first: API response, exception format, auth guard, role/permission model, notification event contract, audit event contract.
- Domain owners should call shared interfaces instead of editing shared internals directly.
- Any change to `User`, `Role`, `Permission`, `SecurityConfig`, notification event shape, audit event shape, API response format, or shared API client should be a small PR reviewed by TV1.
- Integrations should be implemented as mock/stub first, then replaced with real providers after domain flows are stable.

## Sprint 0 - Foundation And Module Skeletons

### TV1 - Shared Foundation Skeleton

Task: create the project-wide foundation used by all other modules.

Function specs:

- `3.1.1.1 Register Account`
- `3.1.1.2 Verify OTP`
- `3.1.1.3 Login`
- `3.1.1.4 Logout`
- `3.1.1.8 View Account Profile`
- `3.1.1.9 Update Account Profile`
- `3.1.1.16 Manage Own Sessions`
- `3.1.4.1 Manage Privacy Settings`
- `3.1.5.1 Receive Reminder Notification`
- `3.1.5.2 Receive Community Reply Notification`
- `3.1.5.3 Receive Consultation Notification`
- `3.1.5.4 Receive Emergency Alert`
- `3.2.5.1 Investigate Security Incident`
- `3.2.5.2 Review Security Event`

Implementation focus:

- Backend packages: `identity-access` (auth, user, role, session), `audit` (audit-logging).
- Shared API response and exception format.
- Auth/session skeleton.
- Role/permission skeleton.
- Notification and audit interfaces.
- Web/mobile shared auth API client convention.

### TV2 - Care Journey Skeleton

Task: create care-domain packages, base entities, and empty routes/screens.

Function specs:

- `3.3.1.1 Create Mother Journey`
- `3.3.1.8 Create Baby Profile`
- `3.3.1.16 Add Health Record`
- `3.3.1.22 Create Appointment Reminder`
- `3.3.1.47 Create Care Group`
- `3.3.10.1 Upload File`
- `3.3.11.1 View Maternal Health Metric Detail`
- `3.3.12.1 View Baby Profile`
- `3.3.15.1 View Health Record Detail`
- `3.3.16.1 View Reminder Detail`
- `3.3.17.1 View Care Group Members`
- `3.3.19.1 View Vaccination Schedule`

Implementation focus:

- Backend packages: `care-journey`, `baby-care`, `health-record` (file, device), `care-coordination` (reminder, family), `vaccination`, `growth`.
- Mobile feature folders for mother, baby, health records, reminders, family care, vaccination.
- Use TV1 auth/user contract only; avoid editing auth internals.

### TV3 - Community And Content Skeleton

Task: create community, content, moderation, partner, and RAG module boundaries.

Function specs:

- `3.3.1.31 Create Community Question`
- `3.3.1.33 Post Community Answer`
- `3.3.1.59 View Content and Checklist`
- `3.3.8.1 Search Community Questions`
- `3.3.13.1 View Community Feed`
- `3.3.18.1 Search Verified Content`
- `3.2.2.1 View Moderation Queue`
- `3.2.2.7 Create Content/FAQ/Checklist`
- `3.2.2.11 Manage Community Topics`
- `3.2.3.1 Create Partner Profile`
- `3.1.2.6 Generate RAG Answer`

Implementation focus:

- Backend packages: `community`, `moderation`, `report`, `community` (content, checklist), `partner-location` (partner, sponsored), RAG service interface with mock answer provider.
- Mobile community/content feature folders.
- Web admin/moderation/content feature folders.
- RAG service interface with mock answer provider.

### TV4 - Expert Consultation Skeleton

Task: create expert, consultation, payment, pricing, and realtime module boundaries.

Function specs:

- `3.2.1.1 Create Expert Profile`
- `3.2.1.3 Upload Verification Documents`
- `3.2.1.4 Configure Availability`
- `3.3.1.52 Book Private Consultation`
- `3.3.1.53 Pay Consultation Fee`
- `3.3.1.54 Join Consultation Session`
- `3.3.1.57 View Expert Directory`
- `3.3.1.58 View Expert Profile`
- `3.3.9.1 Search Expert`
- `3.3.14.1 View Consultation List`
- `3.1.2.1 Process Payment Transaction`
- `3.1.2.2 Calculate Commission`
- `3.1.2.7 Establish Realtime Communication Session`

Implementation focus:

- Backend packages: `expert`, `expert-verification`, `expert-availability`, `consultation` (booking, session, messaging), `consultation` (payment, commission, pricing, refund, dispute, review, settlement).
- Expert mobile/web feature folders.
- Payment mock provider and realtime mock session provider.

### TV5 - AI, Location, Safety Skeleton

Task: create AI triage, map, emergency, safety, and exercise module boundaries.

Function specs:

- `3.3.1.37 Run AI Symptom Intake`
- `3.3.1.38 View Risk Triage Result`
- `3.3.1.39 Open Emergency Flow`
- `3.3.1.40 Find Nearby Care Facility`
- `3.3.1.41 Quick Call or Navigate`
- `3.3.1.42 Send Family Emergency Alert`
- `3.1.3.1 Calculate Distance, Route and ETA`
- `3.3.2.1 View and Select Pregnancy Exercise`
- `3.3.4.1 Configure Safety Monitoring`
- `3.3.6.1 Share Expert Location`
- `3.3.7.1 Find Nearby Available Experts`
- `3.1.2.5 Extract Structured Intake Data`

Implementation focus:

- Backend packages: `ai-safety` (triage), `partner-location` (location, map, emergency), `ai-safety` (safety), `exercise-posture`.
- Mobile feature folders for AI triage, emergency, map, safety, exercise.
- AI, map, posture, and safety provider interfaces with mock implementations.

## Sprint 1 - First End-To-End Domain Flows

### TV1 - Authentication And Account Basics

Task: implement account access flows and profile basics.

Function specs:

- `3.1.1.1 Register Account`
- `3.1.1.2 Verify OTP`
- `3.1.1.3 Login`
- `3.1.1.4 Logout`
- `3.1.1.5 Forgot Password`
- `3.1.1.6 Reset Password`
- `3.1.1.7 Change Password`
- `3.1.1.8 View Account Profile`
- `3.1.1.9 Update Account Profile`

Expected output:

- User can register, verify OTP, log in, log out, recover password, and manage profile.
- Backend auth endpoints, mobile/web auth screens, and shared API client are usable by TV2-TV5.

### TV2 - Mother, Baby, And Health Core

Task: implement first care journey CRUD flows.

Function specs:

- `3.3.1.1 Create Mother Journey`
- `3.3.1.2 Update Mother Journey`
- `3.3.1.3 View Mother Journey Dashboard`
- `3.3.1.4 Add Maternal Health Metric`
- `3.3.1.5 Update Maternal Health Metric`
- `3.3.1.6 View Maternal Health Trend`
- `3.3.1.7 Add Postpartum Log`
- `3.3.1.8 Create Baby Profile`
- `3.3.1.9 Update Baby Profile`
- `3.3.1.10 Archive Baby Profile`

Expected output:

- Mother journey dashboard works with real authenticated user data.
- Baby profile create/update/archive works end-to-end.

### TV3 - Community Question And Answer Core

Task: implement basic community posting and answer flow.

Function specs:

- `3.3.1.31 Create Community Question`
- `3.3.1.32 Edit Community Post`
- `3.3.1.33 Post Community Answer`
- `3.3.1.34 Use Anonymous Display`
- `3.3.1.35 Bookmark Community Post`
- `3.3.1.36 Like Answer`
- `3.3.8.1 Search Community Questions`
- `3.3.8.2 Search Community Topics`
- `3.3.13.1 View Community Feed`
- `3.3.13.2 View Community Question Detail`

Expected output:

- Authenticated users can create questions, answer, like, bookmark, and browse community feed.
- Community topic/search endpoints exist.

### TV4 - Expert Profile And Directory Core

Task: implement expert profile and expert discovery basics.

Function specs:

- `3.2.1.1 Create Expert Profile`
- `3.2.1.2 Update Expert Profile`
- `3.2.1.3 Upload Verification Documents`
- `3.2.1.4 Configure Availability`
- `3.3.1.57 View Expert Directory`
- `3.3.1.58 View Expert Profile`
- `3.3.9.1 Search Expert`
- `3.3.9.2 Filter Expert`

Expected output:

- Expert profile setup and directory browsing work.
- Availability can be configured.
- Search/filter expert can be used by booking flows later.

### TV5 - AI Triage, Emergency, Exercise Basics

Task: implement mock-first AI triage, emergency, and exercise flows.

Function specs:

- `3.3.1.37 Run AI Symptom Intake`
- `3.3.1.38 View Risk Triage Result`
- `3.3.1.39 Open Emergency Flow`
- `3.3.1.40 Find Nearby Care Facility`
- `3.3.1.41 Quick Call or Navigate`
- `3.3.2.1 View and Select Pregnancy Exercise`
- `3.3.2.3 View Pregnancy Exercise Detail`
- `3.1.2.5 Extract Structured Intake Data`

Expected output:

- AI intake and risk result work with mock provider.
- Emergency flow and nearby care show mock/map-ready data.
- Exercise listing and detail screens work.

## Sprint 2 - Complete Core CRUD And UI Wiring

### TV1 - Notifications, Privacy, Sessions

Task: complete common account management and notification foundation.

Function specs:

- `3.1.1.10 Update Notification Preferences`
- `3.1.1.11 View Notifications`
- `3.1.1.12 Mark Notifications as Read`
- `3.1.1.15 Deactivate Own Account`
- `3.1.1.16 Manage Own Sessions`
- `3.1.1.17 Grant Data Permission`
- `3.1.1.18 Revoke Data Permission`
- `3.1.1.19 View Sharing History`
- `3.1.1.22 Delete Own Account`
- `3.1.2.3 Send Automated Notification`
- `3.1.4.1 Manage Privacy Settings`
- `3.1.5.1 Receive Reminder Notification`
- `3.1.5.2 Receive Community Reply Notification`
- `3.1.5.3 Receive Consultation Notification`
- `3.1.5.4 Receive Emergency Alert`

Expected output:

- Notification preferences and notification center are ready.
- Permission and privacy contracts can be used by health sharing and family sync.
- Other domains can emit notification events through TV1 contract.

### TV2 - Baby Logs, Health Records, Reminders, Files

Task: complete core health and care record flows.

Function specs:

- `3.3.1.11 Add Feeding Sleep Diaper Log`
- `3.3.1.12 Update Baby Daily Log`
- `3.3.1.13 View Baby Log Summary`
- `3.3.1.14 Record Development Milestone`
- `3.3.1.15 View Growth Chart`
- `3.3.1.16 Add Health Record`
- `3.3.1.17 Update Health Record`
- `3.3.1.18 Delete or Archive Health Record`
- `3.3.1.19 View Health Record Timeline`
- `3.3.1.22 Create Appointment Reminder`
- `3.3.1.23 Create Medication Reminder`
- `3.3.1.24 Create Vaccination Reminder`
- `3.3.1.25 Update or Snooze Reminder`
- `3.3.1.26 View Today Tasks`
- `3.3.10.1 Upload File`
- `3.3.10.2 View File`
- `3.3.10.3 Delete File`
- `3.3.15.1 View Health Record Detail`
- `3.3.16.1 View Reminder Detail`

Expected output:

- Baby logs, milestones, health records, reminders, file upload/view/delete work end-to-end.
- Reminder notification events call TV1 notification contract.

### TV3 - Community Operations And Content Reader

Task: complete user-side community and verified content reader flows.

Function specs:

- `3.3.8.3 Delete Community Post`
- `3.3.8.4 Follow Topic`
- `3.3.13.3 Edit Own Answer`
- `3.3.13.4 Delete Own Answer`
- `3.3.1.59 View Content and Checklist`
- `3.3.18.1 Search Verified Content`
- `3.3.18.2 View Verified Content Detail`
- `3.2.2.11 Manage Community Topics`

Expected output:

- Community self-management is complete.
- Verified content/checklist can be searched and viewed.
- Topic management supports community and content taxonomy.

### TV4 - Booking, Payment Mock, Consultation Basics

Task: implement consultation booking and mock payment path.

Function specs:

- `3.3.1.52 Book Private Consultation`
- `3.3.1.53 Pay Consultation Fee`
- `3.3.1.54 Join Consultation Session`
- `3.3.1.56 Review Expert After Consultation`
- `3.3.14.1 View Consultation List`
- `3.3.14.2 View Consultation Detail`
- `3.3.20.1 View Expert Consultation Pricing`
- `3.1.2.1 Process Payment Transaction`
- `3.1.2.2 Calculate Commission`
- `3.1.2.7 Establish Realtime Communication Session`

Expected output:

- User can book, pay through mock provider, see consultation, and enter mock session.
- Commission calculation returns deterministic mock result.

### TV5 - Emergency, Map Mock, Device Mock, Exercise Session

Task: extend emergency and safety-related mobile flows.

Function specs:

- `3.3.1.42 Send Family Emergency Alert`
- `3.3.1.43 Connect Health Device`
- `3.3.1.44 Import Device Data Manually`
- `3.3.1.45 Disconnect Health Device`
- `3.3.1.46 View Device Data Trend`
- `3.1.3.1 Calculate Distance, Route and ETA`
- `3.3.2.4 Complete Pre-exercise Safety Check`
- `3.3.2.5 Start Exercise Session`
- `3.3.2.6 Enable Posture Camera`
- `3.3.2.7 Pause or Resume Exercise Session`
- `3.3.2.8 Complete Exercise Session`
- `3.3.2.9 View Exercise Session Result`
- `3.3.2.10 View Pregnancy Exercise History`

Expected output:

- Emergency alert emits TV1 notification event.
- Device flows run with mock/manual import.
- Exercise session can start, pause, complete, and show result.

## Sprint 3 - Cross-Domain Integration

### TV1 - Shared Account, Reporting, Audit, Role Operations

Task: harden shared user/account operations and admin-facing security contracts.

Function specs:

- `3.1.1.13 Search and Filter`
- `3.1.1.14 Report Content or Account`
- `3.1.1.20 Create Community Profile`
- `3.1.1.21 Update Community Profile`
- `3.2.2.16 Manage User Accounts`
- `3.2.2.17 Create Staff Account`
- `3.2.2.18 Update Role and Permission`
- `3.2.2.19 View Audit Logs`
- `3.2.5.1 Investigate Security Incident`
- `3.2.5.2 Review Security Event`

Expected output:

- Admin/user-management contracts support TV3 moderation and TV4/TV5 safety workflows.
- Audit/security events can be recorded and reviewed.

### TV2 - Family Sync, Sharing, Vaccination, Growth

Task: implement cooperative care and vaccination/growth flows.

Function specs:

- `3.3.1.20 Generate Health Summary`
- `3.3.1.21 Share Summary with Expert`
- `3.3.1.27 Manage Preparation Checklist`
- `3.3.1.28 Add Expense`
- `3.3.1.29 Update Expense`
- `3.3.1.30 View Expense Summary`
- `3.3.1.47 Create Care Group`
- `3.3.1.48 Invite Family Member`
- `3.3.1.49 Manage Family Permission`
- `3.3.1.50 Assign Family Task`
- `3.3.1.51 View Shared Care Calendar`
- `3.3.3.1 Accept Care Group Invitation`
- `3.3.3.2 View Shared Data`
- `3.3.3.3 Update Assigned Task Status`
- `3.3.3.4 View Family Alert`
- `3.3.17.1 View Care Group Members`
- `3.3.17.2 Revoke Family Invitation`
- `3.3.17.3 Reject Care Group Invitation`
- `3.3.17.4 Remove Family Member`
- `3.3.17.5 Leave Care Group`
- `3.3.17.6 View Assigned Task Detail`
- `3.3.17.7 Update Family Task`
- `3.3.17.8 Cancel Family Task`
- `3.3.19.1 View Vaccination Schedule`
- `3.3.19.2 Add Vaccination Record`
- `3.3.19.3 Update Vaccination Record`
- `3.3.19.4 Delete Vaccination Record`
- `3.3.19.5 Mark Vaccination Completed`
- `3.3.19.6 Postpone Vaccination`
- `3.3.19.7 Add Growth Measurement`
- `3.3.19.8 Update Growth Measurement`
- `3.3.19.9 Delete Growth Measurement`
- `3.3.19.10 View Growth Measurement History`

Expected output:

- Family members can join care groups, see shared data, and update assigned tasks.
- Vaccination and growth tracking are complete.
- Health summary sharing uses TV1 permission/privacy contract and TV4 expert-summary contract.

### TV3 - Moderation, Partner Governance, RAG Mock

Task: implement moderation/admin flows and mock knowledge answer generation.

Function specs:

- `3.2.2.1 View Moderation Queue`
- `3.2.2.2 Moderate Community Content`
- `3.2.2.3 Resolve Report`
- `3.2.2.4 Warn or Suspend Account`
- `3.2.2.7 Create Content/FAQ/Checklist`
- `3.2.2.8 Update Content/FAQ/Checklist`
- `3.2.2.9 Hide or Delete Content`
- `3.2.2.10 Approve Content Version`
- `3.2.2.12 Manage AI and Red-Flag Rules`
- `3.2.2.13 View Community Dashboard`
- `3.2.2.15 View Impact Report`
- `3.2.3.1 Create Partner Profile`
- `3.2.3.2 Update Partner Profile`
- `3.2.3.3 Submit Service Listing`
- `3.2.3.4 Submit Sponsored Content`
- `3.2.3.5 View Partner Performance`
- `3.2.3.6 Approve Partner Profile`
- `3.2.3.7 Approve Sponsored Service/Campaign`
- `3.2.3.8 Remove Partner Content`
- `3.3.18.3 Manage Content Categories`
- `3.3.18.4 Unpublish Content`
- `3.1.2.6 Generate RAG Answer`

Expected output:

- Moderator/admin can process reports and manage content.
- Partner and sponsored governance flows exist.
- RAG returns mock answers based on verified content/checklist data.

### TV4 - Consultation Lifecycle And Expert Governance

Task: complete consultation lifecycle and expert/admin operations.

Function specs:

- `3.2.1.5 View Expert Question Queue`
- `3.2.1.6 Post Expert Answer`
- `3.2.1.7 Suggest Private Consultation`
- `3.2.1.8 View Shared Health Summary`
- `3.2.1.9 Manage Consultation Session`
- `3.2.1.10 Write Consultation Summary`
- `3.2.1.11 View Revenue and Commission`
- `3.2.1.12 View Contribution Points`
- `3.2.2.5 Verify Expert Profile`
- `3.2.2.6 Revoke Expert Badge`
- `3.2.2.14 View Expert Dashboard`
- `3.2.4.1 Suspend Expert`
- `3.2.4.2 Renew Expert Verification`
- `3.2.7.1 Set Consultation Price`
- `3.2.7.2 Update Consultation Price`
- `3.2.8.1 Configure Consultation Price Bands`
- `3.3.1.55 Submit Dispute or Refund Request`
- `3.3.5.1 Update Consultation Availability Status`
- `3.3.5.2 Respond to Consultation Request`
- `3.3.5.3 Consult via Chat`
- `3.3.5.4 Consult via Voice Call`
- `3.3.5.5 Consult via Video Call`
- `3.3.14.3 Reschedule Consultation`
- `3.3.14.4 Cancel Consultation`
- `3.3.14.5 Complete Consultation`
- `3.3.14.6 Mark Consultation No-show`
- `3.3.14.7 View Consultation Summary`
- `3.3.14.8 Resolve Consultation Dispute`
- `3.3.14.9 Approve or Reject Refund`

Expected output:

- Expert can answer questions, suggest consultation, manage sessions, and write summary.
- Admin can verify/suspend experts and manage pricing bands.
- Dispute/refund flow is represented end-to-end.

### TV5 - Safety, Nearby Support, Location Visibility

Task: implement safety monitoring and nearby support flows.

Function specs:

- `3.3.4.1 Configure Safety Monitoring`
- `3.3.4.2 Enable Fall Detection`
- `3.3.4.3 Disable Fall Detection`
- `3.3.4.4 Detect Suspected Fall or Impact`
- `3.3.4.5 Confirm Safety Check`
- `3.3.4.6 Send Emergency Alert`
- `3.3.4.7 View Safety Event History`
- `3.3.4.8 Report False Positive Detection`
- `3.3.4.9 Open Emergency Support from Safety Alert`
- `3.3.4.10 Configure Emergency Contact`
- `3.3.5.6 Contact Nearby User`
- `3.3.6.1 Share Expert Location`
- `3.3.6.2 Manage Location Visibility`
- `3.3.6.3 View Nearby Support Requests`
- `3.3.6.4 Navigate to Support Location`
- `3.3.7.1 Find Nearby Available Experts`
- `3.3.7.2 Contact Nearby Expert`
- `3.3.7.3 View Nearby Experts on Map`
- `3.3.7.4 Search Nearby Support`

Expected output:

- Safety monitoring and nearby support flows work with mock provider.
- Emergency notifications use TV1 event contract.
- Expert location visibility respects role/privacy contract.

## Sprint 4 - Real Providers And Admin Polish

### TV1 - Real Notification, OTP, Security Logging

Task: replace critical shared mocks with real or production-like providers.

Function specs:

- `3.1.1.2 Verify OTP`
- `3.1.1.5 Forgot Password`
- `3.1.1.6 Reset Password`
- `3.1.2.3 Send Automated Notification`
- `3.1.5.1 Receive Reminder Notification`
- `3.1.5.2 Receive Community Reply Notification`
- `3.1.5.3 Receive Consultation Notification`
- `3.1.5.4 Receive Emergency Alert`
- `3.2.5.1 Investigate Security Incident`
- `3.2.5.2 Review Security Event`

Expected output:

- OTP/email/push provider is real or sandbox-ready.
- Notification fallback behavior is clear.
- Security event logging is usable for admin review.

### TV2 - Device Sync And Care Edge Cases

Task: stabilize files, devices, deletion/archive flows, and care journey edge cases.

Function specs:

- `3.1.2.4 Sync Health Device Data`
- `3.3.1.43 Connect Health Device`
- `3.3.1.44 Import Device Data Manually`
- `3.3.1.45 Disconnect Health Device`
- `3.3.1.46 View Device Data Trend`
- `3.3.11.1 View Maternal Health Metric Detail`
- `3.3.11.2 Delete Maternal Health Metric`
- `3.3.11.3 View Postpartum Logs`
- `3.3.11.4 Update Postpartum Log`
- `3.3.11.5 Delete Postpartum Log`
- `3.3.12.1 View Baby Profile`
- `3.3.12.2 Switch Active Baby Profile`
- `3.3.12.3 View Baby Daily Log Detail`
- `3.3.12.4 Delete Baby Daily Log`
- `3.3.12.5 Update Development Milestone`
- `3.3.12.6 Delete Development Milestone`
- `3.3.16.2 Complete Reminder`
- `3.3.16.3 Skip Reminder`
- `3.3.16.4 Delete Reminder`

Expected output:

- Device sync works through real provider if available, otherwise manual import plus stable mock remains.
- Care journey delete/archive/update detail flows are complete.

### TV3 - RAG Provider And Admin Content Governance

Task: connect RAG to verified content and finish admin content governance.

Function specs:

- `3.1.2.6 Generate RAG Answer`
- `3.2.2.7 Create Content/FAQ/Checklist`
- `3.2.2.8 Update Content/FAQ/Checklist`
- `3.2.2.9 Hide or Delete Content`
- `3.2.2.10 Approve Content Version`
- `3.2.2.12 Manage AI and Red-Flag Rules`
- `3.3.18.1 Search Verified Content`
- `3.3.18.2 View Verified Content Detail`
- `3.3.18.3 Manage Content Categories`
- `3.3.18.4 Unpublish Content`

Expected output:

- RAG has real or sandbox provider with fallback.
- Content lifecycle is complete from draft/update/approval/unpublish.

### TV4 - Payment And Realtime Providers

Task: replace consultation payment and realtime mocks with provider-backed implementation when available.

Function specs:

- `3.1.2.1 Process Payment Transaction`
- `3.1.2.2 Calculate Commission`
- `3.1.2.7 Establish Realtime Communication Session`
- `3.3.1.53 Pay Consultation Fee`
- `3.3.1.54 Join Consultation Session`
- `3.3.5.3 Consult via Chat`
- `3.3.5.4 Consult via Voice Call`
- `3.3.5.5 Consult via Video Call`
- `3.2.1.9 Manage Consultation Session`
- `3.2.1.11 View Revenue and Commission`

Expected output:

- Payment provider is real/sandbox-ready or mock remains reliable.
- Realtime session flow is stable for demo.
- Commission and revenue views match payment records.

### TV5 - Map, AI, Posture, Safety Providers

Task: connect real or near-real integrations for AI, map, posture, and safety.

Function specs:

- `3.1.3.1 Calculate Distance, Route and ETA`
- `3.1.2.5 Extract Structured Intake Data`
- `3.2.6.1 Manage Pregnancy Exercises`
- `3.2.6.2 Manage Posture Analysis Configuration`
- `3.3.2.2 Analyze Exercise Posture`
- `3.3.2.6 Enable Posture Camera`
- `3.3.4.4 Detect Suspected Fall or Impact`
- `3.3.4.6 Send Emergency Alert`
- `3.3.7.3 View Nearby Experts on Map`
- `3.3.7.4 Search Nearby Support`

Expected output:

- Map route/ETA uses TrackAsia or a stable provider abstraction.
- AI intake extraction is real/sandbox or mock with clear fallback.
- Exercise/posture and safety flows are demo-ready.

## Sprint 5 - Stabilization And Merge Freeze

### All Members

Task: stop adding large features, stabilize demo paths, and reduce merge risk.

Required checks:

- Backend: run tests/build from `04_SourceCode/Backend`.
- Frontend: run lint/build from `04_SourceCode/Frontend`.
- Mobile: run analyze/test from `04_SourceCode/MobileApp`.
- Verify `.env.example` documents every required new environment variable.
- Verify every PR references SRS function IDs.
- Verify every integration has fallback behavior.

### TV1 Final Checks

- Auth, account, session, privacy, notification, audit, role/permission.
- Shared error/response consistency.
- Security-sensitive flows: reset password, delete account, deactivate account, revoke permission.

### TV2 Final Checks

- Mother journey, baby journey, health record, file, reminder, vaccination, growth, family sync, device data.
- Demo path: create mother journey -> add baby -> add health record -> create reminder -> share summary.

### TV3 Final Checks

- Community, answer, topic, moderation, content, checklist, partner, RAG fallback.
- Demo path: create question -> answer -> moderate report -> view verified content -> ask RAG question.

### TV4 Final Checks

- Expert profile, verification, availability, booking, payment, consultation session, summary, dispute/refund, commission.
- Demo path: search expert -> book consultation -> pay -> join session -> write/view summary.

### TV5 Final Checks

- AI intake, map, emergency, nearby support, exercise/posture, safety monitoring.
- Demo path: run triage -> high risk -> emergency flow -> nearby facility/expert -> send alert.

## Merge Conflict Reduction Checklist

- Keep PRs small and tied to one task or one SRS cluster.
- Avoid editing another member's package unless agreed in chat/meeting first.
- Shared file changes should be isolated into separate PRs.
- Database migrations should be named by domain and timestamp/story, for example `V20260619_1200__community_question_tables.sql`.
- API contracts should be written before UI depends on them.
- Mock providers should be committed before real provider work starts.
- Each PR should include affected SRS IDs, affected modules, test evidence, and screenshots for UI changes.
