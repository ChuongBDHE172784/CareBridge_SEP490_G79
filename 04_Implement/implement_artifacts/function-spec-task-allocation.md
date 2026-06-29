# Function Spec Task Allocation - Rebalanced by Actual Research Ownership

Source: `01_Requirements/SRS/Report3_Software Requirement Specification.docx.md`, section `3. Functional Specifications`.

This version fixes the previous wrong allocation where TV5/Chương was assigned many Location, Google Map, Nearby Expert, and Pregnancy Exercise/Posture tasks. Because the team already started coding from Sprint 0, Sprint 0 assignments for TV1, TV2, TV3, and TV4 are kept unchanged. The Sprint 0 workload that was wrongly assigned to TV5 is redistributed into later sprints so each member continues with the domain they originally researched.

## Member Mapping

| TV | Member | Correct Main Ownership |
| --- | --- | --- |
| TV1 | Phương | Shared foundation in Sprint 0; Pregnancy Exercise & Posture Support from Sprint 1 onward |
| TV2 | Bách | Care Journey, mother/baby data, health records, reminders, family sync, device health data |
| TV3 | Huy | Community, content/checklist, moderation, partner governance, RAG knowledge |
| TV4 | Lâm | Expert Consultation plus Location/Map/Nearby Care/Nearby Expert support |
| TV5 | Chương | AI triage and IMU-based Smart Activity Monitoring & Safety Support |

## Ownership Summary

| Member | Ownership | Main Boundary |
| --- | --- | --- |
| TV1 - Phương | Shared Foundation + Pregnancy Exercise/Posture | Authentication/shared contracts in Sprint 0; exercise catalog, exercise session, posture camera/analysis from Sprint 1 onward |
| TV2 - Bách | Care Journey | Mother/baby journey, health records, family sync, reminders, vaccination, files, health device data |
| TV3 - Huy | Community & Content | Community Q&A, content/checklist, moderation, partner governance, RAG knowledge |
| TV4 - Lâm | Expert Consultation & Map/Location | Expert profile, booking, consultation, pricing, payment, realtime, nearby care, map route/ETA, expert location visibility |
| TV5 - Chương | AI & IMU Safety | AI intake/triage, structured intake extraction, IMU fall/impact detection, safety countdown, emergency alert, safety event history |

## Rebalance Rules

- Sprint 0 for TV1, TV2, TV3, and TV4 remains unchanged to avoid disrupting code already implemented.
- TV5 Sprint 0 is corrected to create only AI and IMU safety skeletons. Location/map and pregnancy exercise/posture items previously placed under TV5 are treated as handoff items.
- Location, Google Map, route/ETA, nearby care facility, nearby expert, and expert location visibility are moved to TV4/Lâm in Sprint 1 onward.
- Pregnancy exercise, exercise session, posture camera, and posture analysis are moved to TV1/Phương in Sprint 1 onward.
- TV5/Chương keeps AI triage and IMU safety monitoring, including suspected fall/impact detection, safety confirmation, emergency alert, false-positive feedback, and safety event history.
- Domain owners should call TV1 shared interfaces instead of editing shared internals directly.
- Any change to `User`, `Role`, `Permission`, `SecurityConfig`, notification event shape, audit event shape, API response format, or shared API client should be a small PR reviewed by TV1.
- Integrations should be implemented as mock/stub first, then replaced with real providers after domain flows are stable.

---

## Sprint 0 - Foundation And Module Skeletons

> Status: TV1, TV2, TV3, and TV4 are kept as originally assigned because the team has already coded. Only TV5 is corrected to prevent Chương from continuing Location/Map/Exercise work.

### TV1 - Phương - Shared Foundation Skeleton

Task: create the project-wide foundation used by all other modules.

Function specs:

- `3.1.1.1 Register Account`
- `3.1.1.2 Verify OTP`
- `3.1.1.3 Login`
- `3.1.1.4 Logout`
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

- Backend packages: `auth`, `account`, `profile`, `privacy`, `notification`, `audit`, `security`.
- Shared API response and exception structure.
- Auth/session skeleton.
- Role/permission skeleton.
- Notification and audit interfaces.
- Web/mobile shared auth API client convention.

### TV2 - Bách - Care Journey Skeleton

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

- Backend packages: `motherjourney`, `baby`, `healthrecord`, `file`, `reminder`, `vaccination`, `growth`, `familycare`, `device`.
- Mobile feature folders for mother, baby, health records, reminders, family care, vaccination.
- Use TV1 auth/user contract only; avoid editing auth internals.

### TV3 - Huy - Community And Content Skeleton

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

- Backend packages: `community`, `answer`, `topic`, `moderation`, `report`, `content`, `checklist`, `partner`, `sponsored`, `rag`.
- Mobile community/content feature folders.
- Web admin/moderation/content feature folders.
- RAG service interface with mock answer provider.

### TV4 - Lâm - Expert Consultation Skeleton

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

- Backend packages: `expert`, `verification`, `availability`, `consultation`, `booking`, `payment`, `commission`, `pricing`, `realtime`, `refund`, `dispute`, `review`.
- Expert mobile/web feature folders.
- Payment mock provider and realtime mock session provider.

### TV5 - Chương - AI And IMU Safety Skeleton

Task: create only AI triage and IMU safety monitoring module boundaries. Location/map and exercise/posture tasks are removed from TV5.

Function specs:

- `3.3.1.37 Run AI Symptom Intake`
- `3.3.1.38 View Risk Triage Result`
- `3.3.1.39 Open Emergency Flow`
- `3.3.1.42 Send Family Emergency Alert`
- `3.3.4.1 Configure Safety Monitoring`
- `3.3.4.2 Enable Fall Detection`
- `3.3.4.3 Disable Fall Detection`
- `3.3.4.4 Detect Suspected Fall or Impact`
- `3.1.2.5 Extract Structured Intake Data`

Implementation focus:

- Backend packages: `ai`, `triage`, `safety`, `imu`, `emergency`.
- Mobile feature folders for AI triage, emergency handoff, IMU safety monitoring.
- AI provider interface with mock implementation.
- IMU sensor provider abstraction for accelerometer/gyroscope data.
- No `map`, `location`, `exercise`, or `posture` package ownership in Sprint 0 for TV5.

Sprint 0 handoff items removed from TV5:

| Removed from TV5 | New owner from Sprint 1 onward | Reason |
| --- | --- | --- |
| `3.3.1.40 Find Nearby Care Facility` | TV4 - Lâm | Location/map/nearby care domain |
| `3.3.1.41 Quick Call or Navigate` | TV4 - Lâm | Map/navigation domain |
| `3.1.3.1 Calculate Distance, Route and ETA` | TV4 - Lâm | Route/ETA provider domain |
| `3.3.2.1 View and Select Pregnancy Exercise` | TV1 - Phương | Pregnancy exercise domain |
| `3.3.6.1 Share Expert Location` | TV4 - Lâm | Expert location visibility domain |
| `3.3.7.1 Find Nearby Available Experts` | TV4 - Lâm | Nearby expert/map domain |
| `exercise`, `posture` packages | TV1 - Phương | Exercise/posture support domain |
| `location`, `map` packages | TV4 - Lâm | Google Map/TrackAsia/location domain |

---

## Sprint 1 - First End-To-End Domain Flows

### TV1 - Phương - Authentication And Exercise Catalog Basics

Task: complete account access flows and start the pregnancy exercise catalog that was wrongly placed under TV5.

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
- `3.3.2.1 View and Select Pregnancy Exercise`
- `3.3.2.3 View Pregnancy Exercise Detail`

Expected output:

- User can register, verify OTP, log in, log out, recover password, and manage profile.
- Exercise listing and detail screens work under Phương ownership.
- Backend auth endpoints, mobile/web auth screens, and shared API client are usable by TV2-TV5.

### TV2 - Bách - Mother, Baby, And Health Core

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

### TV3 - Huy - Community Question And Answer Core

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

### TV4 - Lâm - Expert Profile, Directory, And Map Basics

Task: implement expert profile/discovery basics and take over map/location tasks that were wrongly placed under TV5.

Function specs:

- `3.2.1.1 Create Expert Profile`
- `3.2.1.2 Update Expert Profile`
- `3.2.1.3 Upload Verification Documents`
- `3.2.1.4 Configure Availability`
- `3.3.1.40 Find Nearby Care Facility`
- `3.3.1.41 Quick Call or Navigate`
- `3.3.1.57 View Expert Directory`
- `3.3.1.58 View Expert Profile`
- `3.3.9.1 Search Expert`
- `3.3.9.2 Filter Expert`
- `3.1.3.1 Calculate Distance, Route and ETA`

Expected output:

- Expert profile setup and directory browsing work.
- Availability can be configured.
- Search/filter expert can be used by booking flows later.
- Nearby care facility and quick navigation show mock/map-ready data.
- Route/ETA provider abstraction exists for Google Map/TrackAsia replacement.

### TV5 - Chương - AI Triage And IMU Safety Basics

Task: implement mock-first AI triage and IMU safety monitoring basics.

Function specs:

- `3.3.1.37 Run AI Symptom Intake`
- `3.3.1.38 View Risk Triage Result`
- `3.3.1.39 Open Emergency Flow`
- `3.3.1.42 Send Family Emergency Alert`
- `3.3.4.1 Configure Safety Monitoring`
- `3.3.4.2 Enable Fall Detection`
- `3.3.4.3 Disable Fall Detection`
- `3.1.2.5 Extract Structured Intake Data`

Expected output:

- AI intake and risk result work with mock provider.
- Emergency flow opens from AI red/yellow routing.
- User can configure IMU-based safety monitoring and enable/disable fall detection.
- No Location/Map/Exercise work is assigned to Chương in Sprint 1.

---

## Sprint 2 - Complete Core CRUD And UI Wiring

### TV1 - Phương - Notifications, Privacy, And Exercise Session

Task: complete common account management and implement exercise session flows.

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
- `3.3.2.4 Complete Pre-exercise Safety Check`
- `3.3.2.5 Start Exercise Session`
- `3.3.2.6 Enable Posture Camera`
- `3.3.2.7 Pause or Resume Exercise Session`
- `3.3.2.8 Complete Exercise Session`
- `3.3.2.9 View Exercise Session Result`
- `3.3.2.10 View Pregnancy Exercise History`

Expected output:

- Notification preferences and notification center are ready.
- Permission and privacy contracts can be used by health sharing and family sync.
- Exercise session can start, pause, complete, and show result.
- Posture camera entry point exists under Phương ownership.

### TV2 - Bách - Baby Logs, Health Records, Reminders, Files

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

### TV3 - Huy - Community Operations And Content Reader

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

### TV4 - Lâm - Booking, Payment Mock, Consultation, And Map Mock

Task: implement consultation booking and continue map/location flows.

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
- `3.1.3.1 Calculate Distance, Route and ETA`
- `3.3.7.1 Find Nearby Available Experts`
- `3.3.7.3 View Nearby Experts on Map`

Expected output:

- User can book, pay through mock provider, see consultation, and enter mock session.
- Commission calculation returns deterministic mock result.
- Nearby expert search/map uses the same map/location provider abstraction.

### TV5 - Chương - IMU Detection And Emergency Alert

Task: extend AI and IMU safety flows without taking map or exercise work.

Function specs:

- `3.3.1.42 Send Family Emergency Alert`
- `3.3.4.4 Detect Suspected Fall or Impact`
- `3.3.4.5 Confirm Safety Check`
- `3.3.4.6 Send Emergency Alert`
- `3.3.4.7 View Safety Event History`
- `3.3.4.8 Report False Positive Detection`
- `3.3.4.9 Open Emergency Support from Safety Alert`
- `3.3.4.10 Configure Emergency Contact`

Expected output:

- IMU fall/impact detection works with mock or device sensor provider.
- Countdown safety confirmation works.
- Emergency alert emits TV1 notification event.
- Safety event history and false-positive feedback are recorded.

---

## Sprint 3 - Cross-Domain Integration

### TV1 - Phương - Shared Account, Reporting, Audit, Role Operations, And Posture Analysis

Task: harden shared user/account operations and complete posture analysis flows.

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
- `3.3.2.2 Analyze Exercise Posture`
- `3.2.6.1 Manage Pregnancy Exercises`
- `3.2.6.2 Manage Posture Analysis Configuration`

Expected output:

- Admin/user-management contracts support TV3 moderation and TV4/TV5 safety workflows.
- Audit/security events can be recorded and reviewed.
- Exercise/posture analysis is managed by Phương, not Chương.

### TV2 - Bách - Family Sync, Sharing, Vaccination, Growth

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

### TV3 - Huy - Moderation, Partner Governance, RAG Mock

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

### TV4 - Lâm - Consultation Lifecycle, Expert Governance, And Location Visibility

Task: complete consultation lifecycle, expert/admin operations, and map/location support.

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
- `3.3.5.6 Contact Nearby User`
- `3.3.6.1 Share Expert Location`
- `3.3.6.2 Manage Location Visibility`
- `3.3.6.3 View Nearby Support Requests`
- `3.3.6.4 Navigate to Support Location`
- `3.3.7.1 Find Nearby Available Experts`
- `3.3.7.2 Contact Nearby Expert`
- `3.3.7.3 View Nearby Experts on Map`
- `3.3.7.4 Search Nearby Support`
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
- Location visibility, nearby support, nearby experts, and map navigation are owned by Lâm.

### TV5 - Chương - AI Safety And IMU Emergency Integration

Task: complete AI triage and IMU safety integration.

Function specs:

- `3.3.1.37 Run AI Symptom Intake`
- `3.3.1.38 View Risk Triage Result`
- `3.3.1.39 Open Emergency Flow`
- `3.3.1.42 Send Family Emergency Alert`
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
- `3.1.2.5 Extract Structured Intake Data`

Expected output:

- AI triage produces safe green/yellow/red routing.
- IMU safety monitoring and emergency handoff work with mock/device provider.
- Emergency notifications use TV1 event contract.
- Map display/navigation after emergency handoff is integrated with TV4/Lâm provider, not owned by TV5.

---

## Sprint 4 - Real Providers And Admin Polish

### TV1 - Phương - Real Notification, OTP, Security Logging, Exercise/Posture Polish

Task: replace critical shared mocks with real or production-like providers and polish exercise/posture.

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
- `3.2.6.1 Manage Pregnancy Exercises`
- `3.2.6.2 Manage Posture Analysis Configuration`
- `3.3.2.2 Analyze Exercise Posture`
- `3.3.2.6 Enable Posture Camera`

Expected output:

- OTP/email/push provider is real or sandbox-ready.
- Notification fallback behavior is clear.
- Security event logging is usable for admin review.
- Exercise/posture and camera flow are demo-ready under Phương ownership.

### TV2 - Bách - Device Sync And Care Edge Cases

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

### TV3 - Huy - RAG Provider And Admin Content Governance

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

### TV4 - Lâm - Payment, Realtime, And Map Providers

Task: replace consultation payment/realtime mocks and connect real or near-real map/location provider.

Function specs:

- `3.1.2.1 Process Payment Transaction`
- `3.1.2.2 Calculate Commission`
- `3.1.2.7 Establish Realtime Communication Session`
- `3.1.3.1 Calculate Distance, Route and ETA`
- `3.3.1.40 Find Nearby Care Facility`
- `3.3.1.41 Quick Call or Navigate`
- `3.3.1.53 Pay Consultation Fee`
- `3.3.1.54 Join Consultation Session`
- `3.3.5.3 Consult via Chat`
- `3.3.5.4 Consult via Voice Call`
- `3.3.5.5 Consult via Video Call`
- `3.3.6.1 Share Expert Location`
- `3.3.6.2 Manage Location Visibility`
- `3.3.7.3 View Nearby Experts on Map`
- `3.3.7.4 Search Nearby Support`
- `3.2.1.9 Manage Consultation Session`
- `3.2.1.11 View Revenue and Commission`

Expected output:

- Payment provider is real/sandbox-ready or mock remains reliable.
- Realtime session flow is stable for demo.
- Commission and revenue views match payment records.
- Map route/ETA uses TrackAsia/Google Map or a stable provider abstraction.

### TV5 - Chương - AI And IMU Safety Providers

Task: connect real or near-real integrations for AI intake extraction and IMU safety.

Function specs:

- `3.1.2.5 Extract Structured Intake Data`
- `3.3.1.37 Run AI Symptom Intake`
- `3.3.1.38 View Risk Triage Result`
- `3.3.4.4 Detect Suspected Fall or Impact`
- `3.3.4.5 Confirm Safety Check`
- `3.3.4.6 Send Emergency Alert`
- `3.3.4.7 View Safety Event History`
- `3.3.4.8 Report False Positive Detection`
- `3.3.4.9 Open Emergency Support from Safety Alert`

Expected output:

- AI intake extraction is real/sandbox or mock with clear fallback.
- IMU fall/impact detection is demo-ready.
- Emergency alert integrates with TV1 notification contract and TV4 map handoff.
- TV5 does not own map route/ETA, nearby expert map, exercise catalog, or posture camera.

---

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

### TV1 - Phương Final Checks

- Auth, account, session, privacy, notification, audit, role/permission.
- Shared error/response consistency.
- Exercise catalog, exercise session, posture camera, posture analysis.
- Security-sensitive flows: reset password, delete account, deactivate account, revoke permission.
- Demo path: login -> select exercise -> complete pre-exercise safety check -> start session -> enable posture camera -> view exercise result.

### TV2 - Bách Final Checks

- Mother journey, baby journey, health record, file, reminder, vaccination, growth, family sync, device data.
- Demo path: create mother journey -> add baby -> add health record -> create reminder -> share summary.

### TV3 - Huy Final Checks

- Community, answer, topic, moderation, content, checklist, partner, RAG fallback.
- Demo path: create question -> answer -> moderate report -> view verified content -> ask RAG question.

### TV4 - Lâm Final Checks

- Expert profile, verification, availability, booking, payment, consultation session, summary, dispute/refund, commission.
- Location/map, nearby care facility, route/ETA, quick call/navigation, expert location visibility, nearby expert map.
- Demo path: search expert -> book consultation -> pay -> join session -> share expert location -> view nearby expert/facility on map.

### TV5 - Chương Final Checks

- AI intake, risk triage, structured intake extraction, IMU safety monitoring, suspected fall/impact detection, safety confirmation, emergency alert, safety event history.
- Demo path: run AI triage -> get high-risk result -> open emergency flow -> enable safety monitoring -> simulate suspected fall/impact -> confirm/not confirm safety -> send family alert.

---

## Merge Conflict Reduction Checklist

- Keep PRs small and tied to one task or one SRS cluster.
- Avoid editing another member's package unless agreed in chat/meeting first.
- Shared file changes should be isolated into separate PRs.
- Database migrations should be named by domain and timestamp/story, for example `V20260619_1200__community_question_tables.sql`.
- API contracts should be written before UI depends on them.
- Mock providers should be committed before real provider work starts.
- Each PR should include affected SRS IDs, affected modules, test evidence, and screenshots for UI changes.

## Quick Handoff Summary

| Domain / Task Group | Old wrong owner | Correct owner | Sprint applied |
| --- | --- | --- | --- |
| Pregnancy exercise catalog | TV5 - Chương | TV1 - Phương | Sprint 1 onward |
| Exercise session and result | TV5 - Chương | TV1 - Phương | Sprint 2 onward |
| Posture camera and posture analysis | TV5 - Chương | TV1 - Phương | Sprint 2 onward |
| Nearby care facility | TV5 - Chương | TV4 - Lâm | Sprint 1 onward |
| Quick call / navigate | TV5 - Chương | TV4 - Lâm | Sprint 1 onward |
| Distance, route, ETA | TV5 - Chương | TV4 - Lâm | Sprint 1 onward |
| Nearby available experts | TV5 - Chương | TV4 - Lâm | Sprint 2 onward |
| Expert location sharing / visibility | TV5 - Chương | TV4 - Lâm | Sprint 3 onward |
| AI symptom intake / risk triage | TV5 - Chương | TV5 - Chương | Unchanged |
| IMU fall detection / safety monitoring | TV5 - Chương | TV5 - Chương | Unchanged, emphasized |
