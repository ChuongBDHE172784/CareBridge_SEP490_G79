# Sprint 0 - Foundation And Module Skeletons

**Duration:** 2 weeks
**Goal:** Create project-wide foundation and module skeletons with empty routes/screens

---

## TV1 - Shared Foundation Skeleton

**Total Tasks:** 14 Use Cases

### Task List

| SRS ID | Use Case Name | Description | Complexity |
|--------|---------------|-------------|------------|
| 3.1.1.1 | Register Account | Creates an account using email or phone, captures initial role, starts OTP verification | Easy |
| 3.1.1.2 | Verify OTP | Verifies OTP code to activate account or confirm sensitive action | Easy |
| 3.1.1.3 | Login | Authenticates user and routes to correct role dashboard | Easy |
| 3.1.1.4 | Logout | Ends current authenticated session and revokes active token/session | Easy |
| 3.1.1.8 | View Account Profile | Displays personal information, role, account status, and basic settings | Easy |
| 3.1.1.9 | Update Account Profile | Updates non-sensitive profile info (name, avatar, phone, area) | Easy |
| 3.1.1.16 | Manage Own Sessions | Displays and revokes user's own active sessions on other devices | Easy |
| 3.1.4.1 | Manage Privacy Settings | Configures privacy preferences and data sharing options | Easy |
| 3.1.5.1 | Receive Reminder Notification | Receives and displays reminder notifications | Easy |
| 3.1.5.2 | Receive Community Reply Notification | Receives and displays community reply notifications | Easy |
| 3.1.5.3 | Receive Consultation Notification | Receives and displays consultation-related notifications | Easy |
| 3.1.5.4 | Receive Emergency Alert | Receives and displays emergency alerts with high priority | Medium |
| 3.2.5.1 | Investigate Security Incident | Reviews and investigates flagged security events | Medium |
| 3.2.5.2 | Review Security Event | Examines audit logs and security-related system events | Medium |

### Implementation Focus

**Backend Packages:**
- `auth` - Authentication, OTP, session management
- `account` - Account operations, password reset
- `profile` - User profile management
- `privacy` - Privacy settings and data permission framework
- `notification` - Notification preferences and event contracts
- `audit` - Audit logging infrastructure
- `security` - Security incident review

**Shared Contracts:**
- API response and exception structure
- Auth/session skeleton
- Role/permission model (RBAC)
- Notification event contract
- Audit event contract
- Web/mobile shared auth API client convention

**Expected Output:**
- User can register, verify OTP, login, logout, recover password
- Profile view/edit works
- Notification preferences configurable
- Shared contracts defined and documented
- Other TV members can depend on TV1 interfaces

---

## TV2 - Care Journey Skeleton

**Total Tasks:** 17 Use Cases

### Task List

| SRS ID | Use Case Name | Description | Complexity |
|--------|---------------|-------------|------------|
| 3.3.1.1 | Create Mother Journey | Creates journey for pre-pregnancy, pregnancy, postpartum, or baby-care tracking | Medium |
| 3.3.1.8 | Create Baby Profile | Creates baby profile with nickname, birth date, gender, birth weight/length | Medium |
| 3.3.1.16 | Add Health Record | Uploads ultrasound, lab results, prescriptions, vaccination forms, etc. | Medium |
| 3.3.1.22 | Create Appointment Reminder | Creates reminder for checkups, follow-ups, ultrasounds, lab tests, expert questions | Medium |
| 3.3.1.47 | Create Care Group | Creates a care group for family coordination | Medium |
| 3.3.10.1 | Upload File | Uploads files (health records, documents, images) to storage | Easy |
| 3.3.11.1 | View Maternal Health Metric Detail | Displays detailed maternal health metrics | Easy |
| 3.3.12.1 | View Baby Profile | Displays baby profile overview and basic info | Easy |
| 3.3.15.1 | View Health Record Detail | Displays record metadata, source, related person, notes, attachments | Easy |
| 3.3.16.1 | View Reminder Detail | Displays reminder type, recurrence, due time, status, notes | Easy |
| 3.3.17.1 | View Care Group Members | Lists members of a care group with roles and permissions | Easy |
| 3.3.19.1 | View Vaccination Schedule | Displays vaccination doses for baby by status and expected time | Medium |
| 3.3.1.2 | Update Mother Journey | Updates LMP, EDD, birth date, journey status | Medium |
| 3.3.1.3 | View Mother Journey Dashboard | Displays pregnancy week/postpartum stage, tasks, reminders, suggested content | Medium |
| 3.3.1.4 | Add Maternal Health Metric | Records maternal metrics (weight, BP, glucose, fetal movement, symptoms) | Medium |
| 3.3.1.5 | Update Maternal Health Metric | Corrects incorrectly entered maternal health metric | Medium |
| 3.3.1.6 | View Maternal Health Trend | Displays metric trends over time (non-diagnostic) | Medium |

### Implementation Focus

**Backend Packages:**
- `motherjourney` - Mother journey management
- `baby` - Baby profile CRUD
- `healthrecord` - Health records upload and management
- `file` - File storage integration (Firebase Storage)
- `reminder` - Reminder scheduling and management
- `vaccination` - Vaccination tracking
- `growth` - Growth measurements
- `familycare` - Care groups and family coordination
- `device` - Health device connections (placeholder)

**Mobile Feature Folders:**
- mother/ (journey, dashboard)
- baby/ (profile, health records)
- healthrecord/
- reminder/
- vaccination/
- familycare/

**Notes:**
- Use TV1 auth/user contract only; avoid editing auth internals
- Implement skeleton endpoints and basic CRUD
- Wire UI screens to backend APIs (mock data acceptable)

---

## TV3 - Community And Content Skeleton

**Total Tasks:** 10 Use Cases

### Task List

| SRS ID | Use Case Name | Description | Complexity |
|--------|---------------|-------------|------------|
| 3.3.1.31 | Create Community Question | Posts community question with topic, stage, age, urgency | Medium |
| 3.3.1.33 | Post Community Answer | Shares labeled personal experience (no diagnosis/prescription) | Medium |
| 3.3.1.59 | View Content and Checklist | Browses approved articles, FAQs, checklists by stage/topic | Easy |
| 3.3.8.1 | Search Community Questions | Searches questions with keywords and filters | Medium |
| 3.3.13.1 | View Community Feed | Displays moderated community questions, answers, topics, saved content | Medium |
| 3.3.18.1 | Search Verified Content | Searches approved articles, FAQs, checklists by keyword, stage, topic | Easy |
| 3.2.2.1 | View Moderation Queue | Displays pending moderation items (questions, answers, reports) | Medium |
| 3.2.2.7 | Create Content/FAQ/Checklist | Creates short content, FAQs, checklists by stage and topic | Medium |
| 3.2.2.11 | Manage Community Topics | Creates, edits, or hides topics, tags, Q&A groups | Medium |
| 3.3.1.32 | Edit Community Post | Edits own community post/question when not locked by moderation | Medium |

### Implementation Focus

**Backend Packages:**
- `community` - Community questions and answers
- `answer` - Answer management
- `topic` - Topic taxonomy and management
- `moderation` - Moderation queue and actions
- `report` - Content reporting system
- `content` - Verified content and checklists
- `checklist` - Checklist templates and instances
- `partner` - Partner portal (skeleton)
- `sponsored` - Sponsored content (skeleton)
- `rag` - RAG knowledge service interface (mock)

**Mobile Feature Folders:**
- community/ (feed, question detail, create question, answer)
- content/ (verified content search, detail)

**Web Admin/Moderation Feature Folders:**
- moderation/ (queue, actions)
- content-admin/ (content CRUD, topic management)

**RAG Service:**
- Define interface for RAG answer generation
- Implement mock provider returning placeholder responses
- Connect to verified content data source (read-only)

---

## TV4 - Expert Consultation Skeleton

**Total Tasks:** 11 Use Cases

### Task List

| SRS ID | Use Case Name | Description | Complexity |
|--------|---------------|-------------|------------|
| 3.2.1.1 | Create Expert Profile | Creates expert profile with specialty, experience, service scope | Medium |
| 3.2.1.3 | Upload Verification Documents | Uploads credentials and supporting documents for verification | Medium |
| 3.2.1.4 | Configure Availability | Sets online status, support methods, location sharing, availability duration | Medium |
| 3.3.1.52 | Book Private Consultation | Books private consultation with expert (time, channel, consent) | Medium |
| 3.3.1.53 | Pay Consultation Fee | Processes payment for consultation (mock provider) | Medium |
| 3.3.1.54 | Join Consultation Session | Joins active consultation session (chat/voice/video placeholder) | Hard |
| 3.3.1.57 | View Expert Directory | Lists and filters verified experts by specialty, availability, location, rating | Medium |
| 3.3.1.58 | View Expert Profile | Displays expert's professional identity, scope, availability, reviews, pricing | Medium |
| 3.3.9.1 | Search Expert | Searches experts by keywords and filters | Medium |
| 3.1.2.1 | Process Payment Transaction | Processes payment through VNPay (mock implementation) | Hard |
| 3.1.2.2 | Calculate Commission | Calculates expert commission based on service type and tier | Medium |
| 3.1.2.7 | Establish Realtime Communication Session | Establishes ZegoCloud realtime session (mock implementation) | Hard |

### Implementation Focus

**Backend Packages:**
- `expert` - Expert profile CRUD
- `verification` - Expert verification documents and status
- `availability` - Expert availability management
- `consultation` - Consultation booking and lifecycle
- `booking` - Booking management
- `payment` - Payment processing (mock VNPay)
- `commission` - Commission calculation
- `pricing` - Consultation pricing tiers
- `realtime` - Realtime communication (mock ZegoCloud)
- `refund` - Refund requests (skeleton)
- `dispute` - Dispute management (skeleton)
- `review` - Expert reviews and ratings (skeleton)

**Mobile/Expert Feature Folders:**
- expert/ (profile setup, availability, question queue, consultation)
- consultation/ (booking, payment, session)

**Mock Providers:**
- Payment mock provider (simulate VNPay callback)
- Realtime mock session provider (return dummy session token)

---

## TV5 - AI, Location & Safety Skeleton

**Total Tasks:** 9 Use Cases

### Task List

| SRS ID | Use Case Name | Description | Complexity |
|--------|---------------|-------------|------------|
| 3.3.1.37 | Run AI Symptom Intake | Collects symptoms through guided structured intake (mock Gemini) | Hard |
| 3.3.1.38 | View Risk Triage Result | Shows non-diagnostic risk level and recommended next safe action | Medium |
| 3.3.1.39 | Open Emergency Flow | Opens emergency flow with options (call, navigate, alert) | Medium |
| 3.3.1.40 | Find Nearby Care Facility | Finds nearby care facilities using map service | Medium |
| 3.3.1.41 | Quick Call or Navigate | Provides quick call button or navigation to facility | Easy |
| 3.3.1.42 | Send Family Emergency Alert | Sends emergency alert to family members | Medium |
| 3.1.3.1 | Calculate Distance, Route and ETA | Calculates route, distance, ETA using TrackAsia (mock) | Medium |
| 3.3.2.1 | View and Select Pregnancy Exercise | Displays suitable exercises and lets mother choose one | Medium |
| 3.1.2.5 | Extract Structured Intake Data | Extracts structured data from symptom text (Gemini AI - mock) | Hard |

### Implementation Focus

**Backend Packages:**
- `ai` - AI service integration
- `triage` - Symptom intake and risk triage logic
- `location` - Location services and tracking
- `map` - Map integration (mock TrackAsia)
- `emergency` - Emergency flow and alerts
- `safety` - Safety monitoring infrastructure
- `exercise` - Pregnancy exercise catalog
- `posture` - Posture analysis (MediaPipe interface - mock)

**Mobile Feature Folders:**
- ai-triage/ (symptom intake, risk result)
- emergency/ (emergency map, alerts)
- map/ (nearby facilities, experts)
- exercise/ (exercise list, detail, session)

**Mock Providers:**
- Gemini AI mock (return deterministic risk levels)
- TrackAsia map mock (return sample POI data)
- MediaPipe posture mock (return sample keypoints)
- Safety monitoring mock (simulate IMU data)

---

## Expected Sprint 0 Output

### Demo Criteria

**TV1:**
- [ ] User can register, verify OTP, login, logout
- [ ] Profile view/edit works
- [ ] Session management screen works
- [ ] Shared contracts documented (API response format, error handling, auth guard, role model)
- [ ] Notification event contract defined (types, payload structure)
- [ ] Audit event contract defined

**TV2:**
- [ ] Can create mother journey and baby profile
- [ ] Can add basic health metrics
- [ ] Can create reminders
- [ ] Can upload files
- [ ] Can create care groups
- [ ] UI screens have skeleton implementations with mock data where needed

**TV3:**
- [ ] Can create community questions and answers
- [ ] Community feed displays questions
- [ ] Can search questions and topics
- [ ] Verified content search works
- [ ] Moderation queue shows items
- [ ] Content admin can create/edit content

**TV4:**
- [ ] Can create expert profile and upload verification docs
- [ ] Can configure availability
- [ ] Can view expert directory and profiles
- [ ] Can book consultation (mock payment)
- [ ] Consultation list shows bookings
- [ ] Payment and commission mocks return deterministic results

**TV5:**
- [ ] AI symptom intake collects data and shows risk result (mock)
- [ ] Emergency flow opens and can send alerts (mock)
- [ ] Can find nearby facilities (mock map data)
- [ ] Exercise list and detail screens work
- [ ] Map route/ETA calculation works (mock)
- [ ] Safety monitoring settings screen exists

### Technical Deliverables

- [ ] Backend project structure with domain packages
- [ ] Database migrations for core tables (users, roles, auth)
- [ ] API endpoints stubbed for each domain
- [ ] Shared API client libraries (for mobile/web)
- [ ] Mock provider implementations for all external integrations
- [ ] Empty UI screens with routing configured
- [ ] Unit test skeletons
- [ ] Documentation: README with setup instructions, API contract spec

---

## Notes for All Members

1. **Do not implement full business logic** - focus on skeleton and CRUD basics
2. **Use mock data** for external services (Firebase, VNPay, TrackAsia, Gemini, ZegoCloud, MediaPipe)
3. **Define interfaces/contracts** before implementation; get review from TV1 if touching shared contracts
4. **Keep PRs small** - one feature/endpoint per PR
5. **Coordinate with dependencies** - if your feature needs another TV's contract, ask them to define it first
6. **Document API endpoints** in a shared spec (OpenAPI/Swagger or simple Markdown)
7. **Write skeleton tests** - at minimum test endpoint existence and basic response shape

---

## Next: Sprint 1

After Sprint 0 complete, move to Sprint 1 to implement first end-to-end domain flows with real user data and complete CRUD operations.
