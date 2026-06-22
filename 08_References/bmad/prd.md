# CareBridge Product Requirements Document (PRD)

**Status**: Draft  
**Version**: 1.0  
**Date**: 2026-06-17  
**Author**: Claude Sonnet 4.6 (Anthropic) based on SRS v1  
**Source**: Derived from `01_Requirements/SRS/Report1_Project Introduction.docx.md`, `01_Requirements/SRS/Report2_Project Management Plan.docx.md`, `01_Requirements/SRS/Report3_Software Requirement Specification.docx.md`, and `02_Design/Architecture/project-structure-design.md`

---

## 1. Product Vision

CareBridge is a **community-based maternal and early childhood health support platform** that provides a safe, accessible, and trustworthy digital ecosystem for:

- Pregnant mothers and postpartum mothers
- Infants and young children (through caregiver accounts)
- Family members involved in care
- Verified health experts
- Partner organizations (clinics, community health groups)
- Community operators and moderators

The platform bridges daily care management, trusted health education, moderated community support, expert connection, AI-assisted risk triage, family coordination, and safety-oriented digital support.

**Mission**: Empower families with safe, community-driven healthcare support while maintaining strict healthcare safety boundaries and data protection.

---

## 2. Target Users and Actors

### Primary Actors (User Roles)

| Role | Description | Primary Needs | Platform Access |
|------|-------------|---------------|-----------------|
| **Mother** | Pregnant or postpartum mother managing her health and baby's care | Journey tracking, health records, reminders, community support, expert consultation, emergency access | Mobile App |
| **Family Member** | Partner, parent, or guardian assisting with childcare | View shared mother/baby data, help with tasks, receive alerts | Mobile App (via invitation) |
| **Verified Expert** | Credentialed health professional providing guidance | Profile visibility, answer questions, manage consultations, build reputation | Mobile App + Expert Portal |
| **Moderator** | Trusted community operator overseeing content and behavior | Review reports, moderate posts, issue warnings/suspensions | Admin Portal |
| **Content Admin** | Manager of health education content | Publish articles, FAQ, checklists, exercise library | Admin Portal |
| **System Admin** | Technical operator managing system configuration | User management, RBAC, audit review, system health | Admin Portal |
| **Partner Representative** | Clinic or organization partner | Manage partner profile, sponsored services, performance metrics | Partner Portal |

### Secondary Actors

- **Guest**: Unauthenticated user (can view public content only, limited)
- **AI/Triage Engine**: Automated risk classification and safe guidance (not a decision-maker)
- **External Systems**: Gemini, Firebase, TrackAsia, VNPay, ZegoCloud, Wearable APIs

---

## 3. Business Goals

1. **Safety-First Positioning**: Establish CareBridge as a healthcare-support platform that never replaces doctors, hospitals, or emergency services.
2. **Community Trust**: Build a moderated, supportive community where mothers feel safe sharing and receiving peer support.
3. **Expert Verification**: Maintain a high bar for expert credentials and transparent labeling of expert responses.
4. **Data Protection**: Achieve user trust through transparent consent, minimal data sharing, and full auditability.
5. **Hanoi Pilot Success**: Validate product-market fit, technical architecture, and governance model within the academic timeline and Hanoi-first scope.
6. **Academic Excellence**: Deliver a capstone project that demonstrates technical competence, ethical design, and social impact readiness.

---

## 4. Non-Goals (Out of Scope for MVP)

❌ **Medical Services**
- No disease diagnosis
- No medication prescription or dosage recommendation
- No treatment planning
- No direct medical examination replacement
- No licensed telemedicine service positioning
- No emergency dispatch operation (only provide emergency contact information and routing)

❌ **Commercial Scale**
- No nationwide deployment infrastructure
- No production-grade scalability guarantees (MVP may have performance limits)
- No insurance claim processing
- No hospital HIS/EMR integration
- No commercial advertising or data selling

❌ **Advanced Features**
- No full video consultation (may use chat or audio-only in MVP)
- No continuous background sensor monitoring guarantee (IMU feature is optional and device-dependent)
- No official Electronic Medical Record interoperability
- No medical imaging analysis
- No regulated medical-device certification

❌ **Financial Complexity**
- No complex payment settlement or commission distribution (VNPay sandbox only)
- No refund processing automation (manual handling)

---

## 5. MVP Scope (SEP490 Academic Timeline)

**Timeline**: 14 weeks starting 11/05/2026  
**Pilot Region**: Hanoi metropolitan area only

### 5.1 Included Functional Domains

The MVP includes the following functional domains with **minimum viable implementation**:

#### ✅ Domain 1: Authentication, Access Control, and Profile
- OTP-based mobile registration and login
- Role-based profile creation (mother, family member, expert)
- Session management with refresh tokens
- Basic RBAC: MOTHER, FAMILY, EXPERT, MODERATOR, CONTENT_ADMIN, SYSTEM_ADMIN, PARTNER
- Profile editing and onboarding flows

#### ✅ Domain 2: Consent, Privacy, and Audit Core
- Consent grant by data type and purpose
- Consent revocation and expiry tracking
- Audit logging for: consent changes, health record access, expert verification, moderation actions, AI triage events, security incidents
- Immutable audit append-only design

#### ✅ Domain 3: Mother and Baby Care Journey (MVP subset)
- Mother journey dashboard (pre-pregnancy, pregnancy, postpartum stages)
- Baby profile creation (name, birth date, gender)
- Basic health metrics tracking (weight, height, head circumference for baby; weight, blood pressure for mother)
- Simple reminder system for vaccination, checkup, medication (basic CRUD, no complex recurrence engine)

#### ✅ Domain 4: Health Records and Family Sync
- Manual health record entry (notes, measurements, files)
- Optional file attachments (images, PDFs)
- Family invitation via email/phone
- Shared view permissions for mother/baby data
- Consent-controlled sharing scope

#### ✅ Domain 5: Community Platform (MVP subset)
- Anonymous posting to community feed
- Category tags (pregnancy, newborn, mental health, nutrition, exercise)
- Comment/reply structure (simple)
- User reporting mechanism
- Moderation queue for moderators
- Basic expert response labeling (verified vs. community)
- Community governance rules display

#### ✅ Domain 6: Expert Ecosystem (MVP subset)
- Expert profile display (credentials, bio, expertise areas)
- Manual verification workflow by admin
- Expert availability setting (available/unavailable)
- Question answer submission (text-based)
- Simple rating/reputation (thumbs up/down count)

#### ✅ Domain 7: AI Safety and Risk Triage (Rule-based MVP)
- Structured symptom intake form (multi-step)
- Rule-based triage (green/yellow/red) using decision tables (Gemini may be simulated or used for text analysis in demo)
- Safe recommendation display: "If red, call emergency 115 immediately"
- Disclaimer: "This is not medical advice"
- AI logs for audit (prompt, response, risk level)
- Conservative fallback if AI unavailable: show generic safe guidance

#### ✅ Domain 8: Emergency Support and Location (MVP subset)
- Emergency button on dashboard and triage results
- Emergency contact list display (115, local hospitals)
- Nearby care search (static list or TrackAsia mock)
- Quick call functionality (tel: link)
- Location permission request before showing map or sharing location
- Location snapshot capture with explicit user action

#### ✅ Domain 9: Administration and Governance Portal (MVP subset)
- User management (list, suspend/activate, view profile)
- Expert verification review (approve/reject with notes)
- Moderation dashboard (reports queue, post deletion, user warnings)
- Basic audit log viewer (filter by user, action, date range)
- Dashboard with key metrics (user count, active users, posts, triage cases)

#### ✅ Domain 10: Content Management (MVP subset)
- CRUD for articles, FAQ, checklists (admin only)
- Pregnancy exercise library (static content with images/videos)
- Content versioning (basic, no complex workflow)

---

### 5.2 Deferred to Future Epic (Post-MVP)

The following are **recognized requirements** but **out of academic MVP scope**:

| Feature | Reason for Deferral |
|---------|---------------------|
| Full video consultation via ZegoCloud | Complexity, bandwidth, clinical safety review needed |
| Advanced payment gateway and refund automation | Requires financial compliance and testing |
| Continuous IMU-based safety monitoring across devices | Device fragmentation, battery impact, reliability concerns |
| Official EMR/EHR integration | Requires national healthcare system agreements |
| Advanced RAG with vector database and document ingestion | Complex ML pipeline beyond capstone scope |
| Machine learning model training and deployment | Research-heavy, requires dataset and MLOps |
| Nationwide map and partner network | Requires business development and scaling |
| Complex recurrence engine for reminders | Can be simplified to basic date-based reminders |
| Multi-language and localization beyond Vietnamese | MVP is Hanoi-focused |
| Advanced analytics and dashboards with charts | Basic metrics only in MVP |
| Commission and settlement system for experts | Financial and legal complexity |
| Advanced moderation with ML toxicity detection | Can start with human moderation only |

---

## 6. User Workflows (High-Level)

### 6.1 Mother Onboarding and Daily Use

1. **Registration**: OTP verification → role selection (mother) → pregnancy stage selection → profile completion
2. **Onboarding**: tutorial screens → consent grants → notification permissions → emergency contact setup (optional)
3. **Dashboard**: View pregnancy stage info, upcoming reminders, recent community posts, quick triage button
4. **Care Journey**: Log health metrics, add notes, upload files, view growth charts (baby) or pregnancy timeline
5. **Reminders**: View upcoming, mark complete, reschedule
6. **Community**: Browse feed, filter by category, post anonymously, answer questions, report inappropriate content
7. **Expert**: Search experts, ask question, receive answer (async), view expert consultation history
8. **AI Triage**: Open triage form → answer symptom questions → receive risk level + safe guidance → if red → emergency flow
9. **Emergency**: Open emergency screen → view contacts → tap to call → optionally share location with family
10. **Family Sync**: Invite family member → family member accepts → family member views shared data with consent

### 6.2 Expert Workflow

1. **Profile Setup**: Complete bio, upload credentials → admin verification → status changes to "verified"
2. **Availability**: Toggle available/unavailable
3. **Community**: Browse questions in expertise areas → submit answers → label as "verified expert"
4. **Consultation** (if implemented): Receive booking request → accept/decline → conduct chat/audio session → write summary

### 6.3 Moderator Workflow

1. **Moderation Queue**: View flagged posts/reports
2. **Review**: Read post and context → check community guidelines
3. **Action**: Dismiss report, edit post content, delete post, warn user, suspend user
4. **Audit**: All actions logged with reason

### 6.4 Admin Workflow

1. **User Management**: Search users, view details, suspend/reactivate
2. **Expert Verification**: Review credential submissions, approve/reject with feedback
3. **Content Management**: Create/edit articles, FAQ, exercises
4. **Audit Review**: Query audit logs, investigate security events
5. **Dashboard**: View platform metrics

---

## 7. Functional Requirements (Prioritized)

### Priority 1: Safety, Security, and Compliance

| ID | Requirement | Rationale |
|-----|-------------|-----------|
| **SEC-001** | System must NOT provide diagnosis, prescription, or treatment advice | Legal and ethical boundary for healthcare support |
| **SEC-002** | AI triage must include red-flag detection for emergency symptoms | Safety-critical: direct to emergency services |
| **SEC-003** | All medical-adjacent content must include disclaimer | Legal protection and user clarity |
| **SEC-004** | RBAC must enforce role-based access to features and data | Security and privacy |
| **SEC-005** | Consent must be obtained before collecting health data, location, or sensor data | GDPR-style privacy compliance |
| **SEC-006** | Audit logs must be append-only and tamper-evident | Compliance and incident investigation |
| **SEC-007** | Emergency flow must be reachable from triage, dashboard, and safety alerts | Safety-first design |
| **SEC-008** | Payment or paid consultation must not block emergency-first actions | Safety over commerce |

### Priority 2: Core Authentication and Data Management

| ID | Requirement |
|-----|-------------|
| **AUTH-001** | OTP-based mobile registration with phone number |
| **AUTH-002** | Login/logout with session management (JWT or session token) |
| **AUTH-003** | Role-based onboarding flows (mother, family, expert, admin, moderator, partner) |
| **AUTH-004** | Profile CRUD (view, edit personal info) |
| **CONSENT-001** | Model consent by: data type, purpose, recipient, scope, expiry |
| **CONSENT-002** | Consent revocation UI with immediate effect |
| **CONSENT-003** | Enforce consent check before accessing: health records, location, family-shared data, expert-shared data, RAG context |
| **AUDIT-001** | Log sensitive actions: login, logout, consent change, health record access, expert verification, moderation action, AI triage, payment, security event |
| **AUDIT-002** | Audit viewer for admin with filters (user, action, date) |

### Priority 3: Mother & Baby Care MVP

| ID | Requirement |
|-----|-------------|
| **CARE-001** | Mother journey dashboard with stage-based information (pre-pregnancy, pregnancy, postpartum) |
| **CARE-002** | Baby profile creation (name, birth date, gender, photo optional) |
| **CARE-003** | Health metrics logging for mother (weight, blood pressure, symptoms notes) |
| **CARE-004** | Health metrics logging for baby (weight, height, head circumference, feeding/sleep notes) |
| **CARE-005** | Simple reminder CRUD (title, date/time, recurrence optional one-time or daily/weekly) |
| **CARE-006** | Health record file upload (images, PDFs) with consent and audit |
| **FAMILY-001** | Family invitation via email or phone number |
| **FAMILY-002** | Family member acceptance and account linking |
| **FAMILY-003** | Family member view of shared mother/baby data (read-only) |

### Priority 4: Community and Expert Ecosystem

| ID | Requirement |
|-----|-------------|
| **COMM-001** | Create anonymous post with title, category, body (text only MVP) |
| **COMM-002** | Community feed sorted by recency, filterable by category |
| **COMM-003** | Comment/reply on posts (nested, 2 levels max) |
| **COMM-004** | Report post or comment (spam, harmful, inappropriate) |
| **COMM-005** | Moderation queue for moderators with approve/dismiss/delete/warn/suspend actions |
| **EXPERT-001** | Expert profile: bio, credentials, expertise areas, photo |
| **EXPERT-002** | Admin verification workflow: submit → review → approve/reject with reason |
| **EXPERT-003** | Verified badge display on expert posts and profile |
| **EXPERT-004** | Expert availability toggle (online/offline) |
| **EXPERT-005** | Community Q&A: experts can answer any post, label as "verified expert" |
| **CONSULT-001** *(deferred to Epic 2)* | Consultation booking request and scheduling |

### Priority 5: AI Triage and Safety

| ID | Requirement |
|-----|-------------|
| **TRIAGE-001** | Structured symptom intake form (multi-step, checkboxes and text) |
| **TRIAGE-002** | Rule-based risk classification (green/yellow/red) using decision table |
| **TRIAGE-003** | Safe recommendation display based on risk level (green: self-monitor, yellow: consult expert, red: emergency 115) |
| **TRIAGE-004** | Disclaimer: "This is not medical advice. In emergency, call 115." |
| **TRIAGE-005** | Log triage session: user input, risk level, recommendation shown |
| **TRIAGE-006** | Fallback: if AI service unavailable, show conservative guidance (red → emergency, yellow → see expert, green → self-monitor) |
| **EMERG-001** | Emergency button on dashboard (always visible) |
| **EMERG-002** | Emergency screen: list of emergency contacts (115, local hospitals), quick-call buttons |
| **EMERG-003** | Nearby care search (mock list or TrackAsia integration if available) |
| **EMERG-004** | Location sharing with family: request permission → capture location → send alert with coordinates |

### Priority 6: Administration and Governance

| ID | Requirement |
|-----|-------------|
| **ADMIN-001** | User list view with search and filter |
| **ADMIN-002** | User suspend/reactivate (soft delete) |
| **ADMIN-003** | Expert verification review panel: view submitted credentials, approve/reject |
| **ADMIN-004** | Moderation dashboard: reports queue, post/comment deletion, user warning, suspension |
| **ADMIN-005** | Content management: create/edit/delete articles, FAQ, exercise content |
| **ADMIN-006** | Basic metrics dashboard: total users, active users (DAU/MAU), posts count, triage count |
| **AUDIT-003** | Audit log viewer with date range and action type filters |

### Priority 7: Infrastructure and Cross-Cutting

| ID | Requirement |
|-----|-------------|
| **INFRA-001** | PostgreSQL database for structured data (users, profiles, health records, posts, audit) |
| **INFRA-002** | Firebase Realtime Database for approved realtime state only; must not replace PostgreSQL |
| **INFRA-002b** | Daily automated backup of PostgreSQL and object storage metadata |

| **INFRA-003** | Firebase Cloud Messaging for push notifications (reminders, alerts) |
| **INFRA-004** | Firebase Storage for file uploads (health records, credentials, media) |
| **INFRA-005** | Docker Compose local development environment |
| **INFRA-006** | OpenAPI contract for backend APIs (to be created during implementation) |
| **INFRA-007** | Internationalization (i18n) support for Vietnamese and English (MVP: Vietnamese primary) |

---

## 8. Non-Functional Requirements

### 8.1 Security and Privacy

- **Encryption**: TLS 1.2+ for all network traffic; AES-256 for sensitive data at rest
- **Authentication**: Short-lived access tokens (≤ 24h), refresh token rotation
- **Secrets Management**: No hardcoded credentials; use environment variables or secret manager
- **Input Validation**: Server-side validation on all inputs; prevent SQL injection, XSS
- **Rate Limiting**: Protect auth endpoints and AI triage from abuse
- **Privacy by Design**: Data minimization, purpose limitation, user-controlled consent

### 8.2 Performance

- **API Response Time**: 95th percentile ≤ 500ms for simple queries, ≤ 2s for complex queries (MVP target)
- **Mobile App Startup**: Cold start ≤ 3s on mid-range Android device
- **Offline Support**: Cache mother/baby profiles and recent reminders; queue edits when offline

### 8.3 Reliability

- **Availability**: MVP target 95% uptime during pilot
- **Data Backup**: Daily automated backup of PostgreSQL and object storage metadata in dev/staging
- **Graceful Degradation**: If external services (Gemini, Firebase, TrackAsia) fail, show safe fallback messages

### 8.4 Usability

- **Mobile First**: All features designed for mobile; admin portal may be web-only
- **Accessibility**: Minimum contrast ratios, readable font sizes, screen reader support for critical flows
- **Language**: Vietnamese primary; English as secondary (content may be mixed)
- **Onboarding**: Guided first-run experience for mothers and experts

### 8.5 Maintainability

- **Code Quality**: Follow coding standards (see `docs/bmad/coding-standards.md`)
- **Test Coverage**: Unit tests ≥ 70% for services and policies; integration tests for critical flows
- **Documentation**: API documentation via OpenAPI; in-code documentation for complex logic
- **CI/CD**: GitLab CI/CD pipeline with build, test, and deploy stages

---

## 9. Acceptance Criteria Framework

All user stories derived from this PRD must include:

1. **Given-When-Then format** for behavior specification
2. **Testability**: Clear pass/fail criteria
3. **Safety checks**: Does this story require consent, audit, RBAC, or red-flag validation?
4. **Cross-cutting impact**: Does this affect security, privacy, or healthcare boundaries?
5. **Validation method**: Manual test, automated test, or demo

Refer to `AGENTS.md` for story structure and approval workflow.

---

## 10. Traceability Matrix (Complete per ERD)

Based on the **CareBridge ERD Logical Model** (`02_Design/Database/CareBridge_ERD_Logical_Model_Updated.puml`) with 67 entities across 11 domains:

| Domain (ERD Package) | PRD Requirement ID | Requirement Description | Priority | Epic |
|----------------------|-------------------|-------------------------|----------|------|
| **Identity & Access** | AUTH-001 through AUTH-004 | Authentication, registration, login | P1 | EPIC-001 |
| | RBAC-001 through RBAC-004 | Role-based access control (9 roles) | P1 | EPIC-001 |
| | PROFILE-001 | Community profiles (public display) | P2 | Future Epic |
| | NOTIF-001 through NOTIF-003 | Notification preferences and delivery | P2 | Future Epic |
| | PERM-001 through PERM-004 | Data permissions with scope, purpose, expiry | P1 | EPIC-001 |
| | AUDIT-001 through AUDIT-003 | Immutable audit logging (audit_logs entity) | P1 | EPIC-001 |
| **Care Journey** | CARE-001 through CARE-006 | Mother journey dashboard and tracking | P2 | EPIC-002 |
| | MATERNAL-001 through MATERNAL-004 | Maternal health metrics recording | P2 | EPIC-002 |
| | POSTPARTUM-001 through POSTPARTUM-004 | Postpartum recovery logs | P2 | EPIC-002 |
| **Baby Care** | BABY-001 through BABY-004 | Baby profile management | P2 | EPIC-002 |
| | DAILYLOG-001 through DAILYLOG-005 | Baby daily logs (feeding, sleep, diaper) | P2 | EPIC-002 |
| | MILESTONE-001 through MILESTONE-003 | Development milestones tracking | P2 | EPIC-002 |
| | GROWTH-001 through GROWTH-003 | Growth measurements and charts | P2 | EPIC-002 |
| | VACCINE-001 through VACCINE-004 | Vaccination records and schedule | P2 | EPIC-002 |
| **Health Records** | RECORD-001 through RECORD-005 | Health record file upload and management | P2 | EPIC-003 |
| | SUMMARY-001 through SUMMARY-003 | Health summaries with time periods | P3 | Future Epic |
| | DEVICE-001 through DEVICE-004 | Health device connections and measurements | P3 | Future Epic |
| **Care Coordination** | GROUP-001 through GROUP-005 | Care groups creation and management | P3 | Future Epic |
| | MEMBER-001 through MEMBER-004 | Care group members and invitations | P3 | Future Epic |
| | TASK-001 through TASK-004 | Care task assignment and tracking | P3 | Future Epic |
| | REMINDER-001 through REMINDER-005 | Reminder CRUD with recurrence rules | P2 | EPIC-002 |
| | EXPENSE-001 through EXPENSE-004 | Expense tracking for care costs | P3 | Future Epic |
| **Community & Content** | COMM-001 through COMM-005 | Community feed, posts, comments | P3 | EPIC-004 |
| | TOPIC-001 through TOPIC-003 | Community topics and categorization | P3 | EPIC-004 |
| | CONTENT-001 through CONTENT-006 | Content items (articles, FAQ) with versioning | P3 | EPIC-004 |
| | CHECKLIST-001 through CHECKLIST-004 | Checklist templates and items | P3 | EPIC-004 |
| | REPORT-001 through REPORT-004 | Content reporting and moderation | P3 | EPIC-004 |
| | MOD-001 through MOD-004 | Moderation actions and audit | P3 | EPIC-004 |
| **Expert Ecosystem** | EXPERT-001 through EXPERT-005 | Expert profiles and verification workflow | P3 | EPIC-005 |
| | CRED-001 through CRED-004 | Expert credentials upload and review | P3 | EPIC-005 |
| | AVAIL-001 through AVAIL-003 | Expert availability configuration | P3 | EPIC-005 |
| | LOCSHARE-001 through LOCSHARE-003 | Expert location sharing with consent | P3 | Future Epic |
| | POINTS-001 through POINTS-003 | Contribution points and reputation | P3 | Future Epic |
| **Consultation** | BOOK-001 through BOOK-006 | Consultation booking with price lock | P3 | EPIC-005 |
| | SESSION-001 through SESSION-004 | Consultation sessions and messaging | P3 | Future Epic |
| | PAY-001 through PAY-007 | Payment transactions, refunds, commissions | P3 | EPIC-006 |
| | REVIEW-001 through REVIEW-003 | Expert reviews and ratings | P3 | EPIC-005 |
| | PRICE-001 through PRICE-005 | Consultation price bands and expert pricing | P3 | Future Epic |
| | DISPUTE-001 through DISPUTE-005 | Consultation disputes and resolution | P4 | Future Epic |
| | SETTLE-001 through SETTLE-004 | Settlement records for experts | P4 | Future Epic |
| **AI & Safety** | TRIAGE-001 through TRIAGE-006 | Symptom intake and rule-based triage | P4 | EPIC-007 |
| | SAFE-001 through SAFE-008 | Safety monitoring (IMU) and alerts | P4 | EPIC-008 |
| **Partner & Location** | PARTNER-001 through PARTNER-005 | Partner organization registration | P3 | Future Epic |
| | SERVICE-001 through SERVICE-004 | Partner services and campaigns | P3 | Future Epic |
| | FACILITY-001 through FACILITY-004 | Care facilities directory | P3 | Future Epic |
| | EMERG-001 through EMERG-004 | Emergency events and facility selection | P4 | EPIC-008 |
| | LOC-001 through LOC-004 | Location snapshots with consent | P4 | EPIC-008 |
| **Exercise & Posture** | EXERCISE-001 through EXERCISE-006 | Pregnancy exercise library (third trimester) | P3 | Future Epic |
| | SAFETYCHECK-001 through SAFETYCHECK-004 | Exercise safety checks before session | P3 | Future Epic |
| | EXSESSION-001 through EXSESSION-005 | Exercise session tracking and posture score | P3 | Future Epic |
| | POSTURECFG-001 through POSTURECFG-004 | Posture analysis configuration (angle-based) | P3 | Future Epic |
| | FEEDBACK-001 through FEEDBACK-004 | Real-time posture feedback events | P3 | Future Epic |

**Note**: This matrix maps all 67 ERD entities to PRD requirements. Priorities align with MVP scope (P1-P4). Future Epic indicates post-MVP features.

---

## 11. Definitions and Acronyms

| Term | Definition |
|------|------------|
| **MVP** | Minimum Viable Product - the smallest set of features that demonstrates core value |
| **RBAC** | Role-Based Access Control - permission system based on user roles |
| **OTP** | One-Time Password - used for mobile verification |
| **JWT** | JSON Web Token - stateless session token format |
| **RAG** | Retrieval-Augmented Generation - AI technique combining document retrieval with LLM |
| **IMU** | Inertial Measurement Unit - device sensor for motion detection |
| **SRS** | Software Requirements Specification |
| **TDS** | Technical Design Specification |
| **EDC** | Electronic Data Capture (health records) |
| **Gemini** | Google's AI model, used for symptom intake and triage |
| **Firebase** | Google's mobile development platform (FCM, Firestore, Storage) |
| **TrackAsia** | Map and location service provider for Vietnam |
| **ZegoCloud** | Real-time communication service for video/audio |
| **VNPay** | Vietnamese payment gateway |
| **PostgreSQL** | Open-source relational database, source of truth for all business data |
| **Firebase Realtime Database** | Google's realtime database, used only for approved realtime state |

---

## 12. Approval and Sign-Off

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Product Owner / Supervisor | TBD | | |
| Technical Lead | TBD | | |
| Business Analyst | TBD | | |
| QA Lead | TBD | | |

---

**Document End**
