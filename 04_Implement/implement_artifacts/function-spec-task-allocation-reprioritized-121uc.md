# CareBridge Function-Spec Task Allocation — Priority-First Delivery Plan (121 UC Baseline)

**Status:** Revised for the reduced-but-detailed scope.  
**Supersedes:** `function-spec-task-allocation.md` for all work not already completed.  
**Source of truth:** `3_Functional_Specification_Detailed_Scope_121UC.md`, UC-01 to UC-121.  
**Primary objective:** deliver a convincing, end-to-end core demonstration before expanding into secondary care-support functions, while keeping each developer inside a stable module boundary.

---

## 1. Scope and Priority Rules

### 1.1 Delivery baseline

The current detailed scope contains **121 use cases**:

- **P0 — 82 use cases:** core value and demonstration scope.
  - MF-01 Account, Trust & Access Control
  - MF-02 Mother Care Journey
  - MF-03 Baby Care, Growth & Vaccination
  - MF-04 Community Q&A & Moderation
  - MF-05 Verified Expert Network & Contribution
  - MF-06 AI Nurse Assistant & Risk Triage
  - MF-07 Emergency Map & Nearby Care Support
- **P1 — 39 use cases:** secondary functions that follow only after the P0 demonstration path is stable.
  - MF-08 Personal Health Records & Source Labeling
  - MF-09 Reminders, Tasks & Care Plan
  - MF-10 Family Sync & Cooperative Care
  - MF-11 Verified Content & Checklist Hub
  - MF-12 Expense & Preparation Planner
  - MF-13 Connected Device & Health Data Integration
  - MF-14 Smart Activity Monitoring & Safety Support

### 1.2 Explicitly excluded from this implementation plan

Do **not** allocate implementation time before a separate Version 2 approval for:

- Paid Direct Consultation, payment, commission, realtime chat/voice/video, refunds and expert reviews.
- Partner Clinic / Sponsored Service Management and Partner Portal.
- Operation, impact or fundraising dashboards.

Safety, compliance, source labels, consent, audit logging, dangerous-advice blocking and privacy enforcement are **cross-cutting controls**, not separate feature backlogs. Every owner must enforce them inside their own use cases.

### 1.3 Priority order

1. **Demo Gate A — Primary user value:** account access → mother journey → baby daily care → community question → verified expert answer.
2. **Demo Gate B — Safety value:** AI symptom intake → safe risk orientation → emergency map → nearby facility / route / quick action.
3. **Complete all remaining P0 flows:** exercise/posture, expert verification administration, moderation, trust controls and nearby-support request handling.
4. **P1 care support:** health records, reminders, family sync, content hub, expense, device data and IMU safety.
5. **Only then:** provider integrations and Version 2 features, if separately approved.

---

## 2. Member Ownership and Code Boundaries

| TV | Member | Primary ownership | Must not own or modify without coordination |
| --- | --- | --- | --- |
| TV1 | Phương | Shared account/access foundation, privacy/consent, notification/audit contracts, pregnancy exercise and posture support | Mother/baby business tables, community/moderation tables, expert/map tables, AI/IMU logic |
| TV2 | Bách | Mother/baby journey, growth, vaccination, health records, reminders, family sync, expense and device-health data | `user`, role, permission, auth/session, community, expert, AI and map internals |
| TV3 | Huy | Community Q&A, moderation, community topics, verified content, approved AI knowledge sources | Auth/RBAC internals, expert verification state, map/location provider and AI triage rules |
| TV4 | Lâm | Verified Expert Network, expert verification, expert contribution, Map/Location, nearby care and nearby expert support | Community post/answer storage, user/permission internals, AI risk rules and IMU detection |
| TV5 | Chương | AI symptom intake, triage/risk rules, emergency handoff, IMU smart activity monitoring and safety events | Map/route implementation, expert availability persistence, exercise/posture package and shared auth internals |

### 2.1 Mandatory integration contracts

These contracts prevent two people from editing the same business module:

| Contract | Contract owner | Consumers | Rule |
| --- | --- | --- | --- |
| `AuthContext`, `CurrentUser`, role/permission checks, API result and exception format | TV1 | TV2–TV5 | Consumers call the contract; they do not edit auth/RBAC implementation. |
| `NotificationCommand`, `AuditEvent` | TV1 | TV2–TV5 | Domains publish an event; only TV1 owns delivery format and audit envelope. |
| `MotherJourneyReadPort`, `BabyReadPort` | TV2 | TV1, TV3, TV4, TV5 | Other domains request authorized read models; no direct query into TV2 tables. |
| `CommunityQuestionPort`, `ExpertBadgeReadPort` | TV3 / TV4 | TV4 / TV3 | TV3 owns posts and answers; TV4 only supplies verification/badge state for expert answers. |
| `EmergencyMapHandoff` and `RouteProvider` | TV4 | TV5 | TV5 opens a handoff with safe context; TV4 owns map, facility, route and navigation UI/API. |
| `TriageResultPort` | TV5 | TV4 | TV5 owns risk result and red-risk handoff decision; TV4 only renders the map action. |
| `PermissionCheckPort` | TV1 | TV2, TV4 | Health/family sharing must call it rather than implement duplicate consent rules. |

### 2.2 Merge-conflict rules

1. One domain owner per package, route prefix, database migration and controller group.
2. Every schema change is a new timestamped Flyway migration. Never amend another member’s committed migration.
3. Shared changes (`User`, `Role`, `Permission`, `SecurityConfig`, base response, exception handling, app shell, route registry) are isolated in a small PR and reviewed by TV1.
4. A cross-domain feature must use a read port, event or HTTP contract. No direct repository/entity access across domains.
5. Every PR states: affected UC IDs, module owner, migration file, API contract impact, test evidence and demo screenshot/video where relevant.
6. Mocks are allowed for the first demo gate. Replace them only after the demo flow is stable.

---

## 3. Demonstration Gates

### Demo Gate A — Community Care Core

**Target user story:** a new mother creates an account, starts her journey, creates a baby profile, records daily care, asks a community question and receives a verified expert answer.

**Required use cases:**

`UC-01, UC-02, UC-03, UC-08, UC-09, UC-10, UC-19, UC-21, UC-32, UC-35, UC-36, UC-38, UC-46, UC-47, UC-48, UC-50, UC-60, UC-62, UC-65, UC-66, UC-67, UC-68, UC-70`

**Exit condition:** a single scripted demo can run with two roles: Mother and Verified Expert. OTP and expert verification may use controlled sandbox/mock delivery, but authorization, role separation and data persistence must be real.

### Demo Gate B — AI Safety and Emergency Support

**Target user story:** the mother submits symptoms, receives a non-diagnostic red/yellow/green result and, for red risk, opens nearby care support with facility, route/ETA and quick action.

**Required use cases:**

`UC-72, UC-73, UC-74, UC-75, UC-76, UC-77, UC-78, UC-79`

**Exit condition:** the triage result is clearly non-diagnostic; red-risk handoff works; facility and route data may be mock/TrackAsia-ready but must be deterministic for the demo.

### P0 Completion Gate

**Target:** all UC-01 to UC-82 work; no P1 feature may block or destabilize the two demo gates.

---

## 4. Sprint 0 — Contract Freeze and Vertical-Slice Skeleton

**Purpose:** prepare isolated modules and stable contracts. This sprint does not attempt to finish every screen. Existing code may stay; unfinished work is retargeted to the priority plan below.

### TV1 — Phương

- Establish `auth`, `account`, `profile`, `permission`, `notification`, `audit` package boundaries.
- Freeze shared API response/error format, JWT/session convention, `AuthContext`, permission check interface and notification/audit event envelopes.
- Deliver starter endpoints/screens for `UC-01`, `UC-02`, `UC-03` only.
- Create empty exercise/posture feature modules; do not build advanced posture analysis yet.

### TV2 — Bách

- Create owned migrations and empty modules for `motherjourney`, `baby`, `growth`, `vaccination`, `healthrecord`, `reminder`, `familycare`, `expense`, `devicehealth`.
- Publish read models/interfaces required by Dashboard and AI context; do not modify TV1 user tables.
- Create Mother Home and Baby Home routing shells with fixtures.

### TV3 — Huy

- Create owned modules for `community`, `topic`, `answer`, `moderation`, `report`, `verifiedcontent`, `aiknowledge`.
- Publish the community-question API contract and the `ExpertBadgeReadPort` consumer contract.
- Seed a minimal topic set for the first demonstration.

### TV4 — Lâm

- Create owned modules for `expert`, `expertverification`, `expertavailability`, `map`, `location`, `nearbycare`.
- Publish `ExpertBadgeReadPort`, `RouteProvider` and `EmergencyMapHandoff` contracts.
- Create a deterministic mock facility provider and seed one verified expert for Demo Gate A.
- Do not create consultation, payment, realtime, commission or partner packages.

### TV5 — Chương

- Create owned modules for `triage`, `airiskrule`, `safety`, `imu`, `safetyevent`.
- Publish `TriageResultPort` and a red-risk emergency handoff payload that TV4 can consume.
- Implement a rule/mock triage provider skeleton and an IMU sensor abstraction only.
- Do not implement map routes, nearby expert persistence, exercise or posture features.

**Sprint 0 exit condition:** all five modules compile together; each vertical slice can use seeded/mock data; shared contracts are merged before independent UI expansion begins.

---

## 5. Sprint 1 — Deliver Demo Gate A First

### TV1 — Phương: Account Entry and Identity

**Use cases:** `UC-01, UC-02, UC-03, UC-08, UC-09, UC-10`

- Complete register, OTP, login, profile and separate community identity.
- Provide stable role routing for Mother, Expert and Admin test users.
- Publish test-account and token setup guidance for the team.

### TV2 — Bách: Mother and Baby Daily-Care Core

**Use cases:** `UC-19, UC-21, UC-32, UC-35, UC-36, UC-38`

- Initialize journey and show its dashboard.
- Create a baby profile, show baby overview, add a daily log and show its summary.
- Return only the signed-in mother’s data through TV1 authorization context.

### TV3 — Huy: Community Question and Answer Core

**Use cases:** `UC-46, UC-47, UC-48, UC-50`

- Browse feed/topics, view a question, post a question and post an answer.
- TV3 owns answer persistence; expert-badge display is read from TV4 through `ExpertBadgeReadPort`.

### TV4 — Lâm: Expert Verification, Directory and Expert Answer Enablement

**Use cases:** `UC-60, UC-62, UC-65, UC-66, UC-67, UC-68, UC-70`

- Deliver expert profile submission, document upload, verification review and a seeded/approved verified-expert lifecycle.
- Deliver public directory/profile and question queue.
- Post expert answers through TV3’s community API; do not create a second answer table.

### TV5 — Chương: Triage Slice Preparation

**Use cases:** `UC-72, UC-73`

- Deliver a guided structured symptom intake and a deterministic red/yellow/green result using mock/rule provider.
- Build the result UI with explicit non-diagnosis, uncertainty and safe-next-action language.

**Sprint 1 exit condition — Demo Gate A is runnable:**

`Register → Verify OTP → Login as Mother → Initialize Journey → Create Baby → Add Daily Log → Ask Community Question → Login as Verified Expert → Post Verified Answer → Mother views answer.`

---

## 6. Sprint 2 — Deliver Demo Gate B and Finish the P0 Core

### TV1 — Phương: Shared Trust Controls and Pregnancy Exercise

**Use cases:** `UC-04, UC-05, UC-06, UC-07, UC-11, UC-12, UC-13, UC-14, UC-15, UC-16, UC-17, UC-18, UC-28, UC-29, UC-30, UC-31`

- Finish logout, password recovery, notification preferences, notifications, sessions, account lifecycle, consent grant/revoke and admin access/security review.
- Deliver pregnancy exercise library, pre-exercise safety check, session lifecycle and exercise result/history.
- Camera consent and posture feedback are optional inside `UC-30`; raw video/image retention is out of scope.

### TV2 — Bách: Remaining P0 Mother/Baby, Growth and Vaccination

**Use cases:** `UC-20, UC-22, UC-23, UC-24, UC-25, UC-26, UC-27, UC-33, UC-34, UC-37, UC-39, UC-40, UC-41, UC-42, UC-43, UC-44, UC-45`

- Complete journey date/stage updates, maternal metrics, trends, postpartum logs and preparation checklist.
- Complete baby profile archive/switch, journal updates, milestones, growth measurements/trends and vaccination record/reference schedule.

### TV3 — Huy: Community Safety and Moderation Completion

**Use cases:** `UC-49, UC-51, UC-52, UC-53, UC-54, UC-55, UC-56, UC-57, UC-58, UC-59`

- Complete own post/answer editing, reactions, bookmarks and topic following.
- Deliver report submission, moderation queue, moderation decisions, enforcement and topic/visibility management.
- Use TV1 audit/notification contracts; do not build a separate shared audit system.

### TV4 — Lâm: Advanced Expert Trust and Nearby Care

**Use cases:** `UC-61, UC-63, UC-64, UC-69, UC-71, UC-77, UC-78, UC-79, UC-80, UC-81, UC-82`

- Complete expert profile updates, verification renewal/status, availability/service scope, contribution points and trust restriction/suspension/reinstatement.
- Deliver map opening, nearby facility search, route/ETA, quick call/navigation, time-limited emergency location alert, nearby support request and expert response.
- Map/location stays entirely inside TV4; TV5 only hands off a triage result or safety event.

### TV5 — Chương: AI Safety Governance and Emergency Handoff

**Use cases:** `UC-74, UC-75, UC-76`

- Complete red-risk handoff to TV4 Emergency Map.
- Create AI risk-rule administration and versioning under the `triage` module.
- TV3 owns approved knowledge-source content metadata for `UC-75`; TV5 owns the AI rule execution and configuration UI/API for `UC-76`.

**Sprint 2 exit condition — Demo Gate B and P0 completion:**

`Mother logs symptoms → receives non-diagnostic risk result → red-risk handoff opens map → nearby facility appears → route/ETA and quick action work → optional family location alert can be sent with consent.`

At this point, all **UC-01 to UC-82** are implemented or have a controlled mock/provider fallback suitable for a capstone demonstration.

---

## 7. Sprint 3 — P1 Care Continuity, Verified Content and Smart Safety Start

### TV1 — Phương: Shared Integration Hardening

**No new primary UC ownership.**

- Stabilize consent enforcement, notification delivery, audit events, shared API error behavior and automated contract tests for P1 consumers.
- Support TV2/TV5 through published interfaces only; do not absorb their domain business logic.
- Add seeded test accounts/permissions used for family-sync and safety demo cases.

### TV2 — Bách: Health Records and Reminders

**Use cases:** `UC-83, UC-84, UC-85, UC-86, UC-87, UC-88, UC-89, UC-90, UC-91, UC-92, UC-93`

- Implement health record upload/metadata/archive/timeline, health summaries and consent-based selected record sharing.
- Implement appointment, medicine/vitamin and vaccination reminders; lifecycle actions and Today Tasks view.
- Notification scheduling emits TV1 `NotificationCommand`; consent sharing calls TV1 `PermissionCheckPort`.

### TV3 — Huy: Verified Content and AI Knowledge Sources

**Use cases:** `UC-75, UC-102, UC-103, UC-104, UC-105, UC-106, UC-107, UC-108`

- Complete approved AI knowledge-source lifecycle from content metadata perspective.
- Deliver user content browse/detail plus admin create, update, review/publish, archive and category/stage/topic mapping.
- TV5 may retrieve only approved content/read models; it must not alter publication state.

### TV4 — Lâm: P0 Regression and Map/Expert Hardening

**No new P1 feature scope.**

- Harden expert verification, directory, availability, emergency-map and nearby-support P0 flows.
- Replace only stable mocks with TrackAsia/real provider configurations where safe; preserve fallback behavior for demo.
- Prepare contract tests for triage-to-map and safety-to-map handoffs.

### TV5 — Chương: IMU Safety Setup

**Use cases:** `UC-116, UC-117, UC-118, UC-119`

- Implement emergency contacts, safety-monitor configuration, enable and disable monitoring.
- Validate OS permission, battery/foreground limitations and explicit consent.
- Do not promise continuous certified monitoring.

**Sprint 3 exit condition:** health records/reminders/content are demoable independently; smart safety can be configured and enabled; P0 demo flows remain regression-safe.

---

## 8. Sprint 4 — P1 Cooperative Care, Device Data and Smart Safety Completion

### TV1 — Phương: Security and Release Support

**No new primary UC ownership.**

- Finalize audit/security review support, permission expiry behavior, notification fallback behavior and release-check automation.
- Review all cross-domain permission decisions before merge freeze.

### TV2 — Bách: Family Sync, Expense and Connected Device Data

**Use cases:** `UC-94, UC-95, UC-96, UC-97, UC-98, UC-99, UC-100, UC-101, UC-109, UC-110, UC-111, UC-112, UC-113, UC-114, UC-115`

- Complete care groups, invitations, membership, family permission scope, shared tasks and permitted shared calendar/data/alerts.
- Complete expense entry and summary.
- Complete device connection, import/sync, data quality/trends, disconnect and imported-data deletion.
- Device integrations may remain manual-import/mock if real wearable access threatens the delivery date; source labels and data-quality cautions remain mandatory.

### TV3 — Huy: Content/Moderation Regression and Data Preparation

**No new primary UC ownership.**

- Curate demo content and verified AI knowledge sources.
- Execute moderation, source-label and content-version regression tests.
- Ensure the community still blocks unsafe content paths after P1 modules are added.

### TV4 — Lâm: Expert/Map Regression and Provider Fallback

**No new primary UC ownership.**

- Validate location-expiry, accuracy, route/ETA fallback, no-dispatch disclaimers and nearby support request scope.
- Verify expert trust status drives directory, answer badge and nearby-availability behavior consistently.

### TV5 — Chương: Smart Activity Event Lifecycle

**Use cases:** `UC-120, UC-121`

- Implement suspected fall/impact candidate handling, safety countdown, I-am-OK / need-help outcome, emergency handoff and event history.
- Implement false-positive feedback without presenting it as medical diagnosis or certified fall detection.
- Emergency alert delivery goes through TV1; map handoff goes through TV4.

**Sprint 4 exit condition:** all **UC-83 to UC-121** are implemented, controlled-mock complete or explicitly deferred with a documented fallback that still preserves safety and privacy boundaries.

---

## 9. Sprint 5 — Stabilization, Test Evidence and Merge Freeze

### All members

- Stop adding new feature scope.
- Fix only P0/P1 defects, integration gaps, accessibility/usability defects and data/security issues.
- Each owner validates their assigned UC list with test evidence.
- Run backend build/tests, web lint/build and mobile analyze/tests.
- Verify `.env.example` and provider fallback documentation.
- Run the two end-to-end demo scripts without live-provider dependency where a sandbox/mock is permitted.

### Required final demo scripts

1. **Community Care Core:** register → OTP → mother journey → baby log → community question → verified expert answer.
2. **AI Safety Support:** symptom intake → risk result → emergency map → nearby facility → route/quick action.
3. **Care Continuity:** health record → reminder → family sharing → content/checklist.
4. **Smart Safety:** configure monitoring → simulate impact → safety check → family alert/map handoff → false-positive review.

---

## 10. Complete UC Traceability Matrix

| UC IDs | Feature | Owner | Delivery sprint | Notes |
| --- | --- | --- | --- | --- |
| UC-01–UC-18 | MF-01 Account, Trust & Access Control | TV1 — Phương | Sprint 1–2 | Shared foundation; all domains consume contracts only. |
| UC-19–UC-27 | MF-02 Mother Care Journey — journey, metrics, postpartum, checklist | TV2 — Bách | Sprint 1–2 | Exercise is intentionally separated to TV1. |
| UC-28–UC-31 | MF-02 Mother Care Journey — exercise and posture | TV1 — Phương | Sprint 2 | Required P0, but not needed to pass Demo Gate A. |
| UC-32–UC-45 | MF-03 Baby Care, Growth & Vaccination | TV2 — Bách | Sprint 1–2 | Daily log is in Gate A; remaining growth/vaccination finishes in Sprint 2. |
| UC-46–UC-59 | MF-04 Community Q&A & Moderation | TV3 — Huy | Sprint 1–2 | Community question/answer first; moderation follows immediately. |
| UC-60–UC-71 | MF-05 Verified Expert Network & Contribution | TV4 — Lâm | Sprint 1–2 | Expert answer is delivered via TV3 community contract. |
| UC-72–UC-74 | MF-06 AI symptom intake and risk result | TV5 — Chương | Sprint 1–2 | Red-risk handoff is part of Demo Gate B. |
| UC-75 | MF-06 Approved AI knowledge sources | TV3 — Huy | Sprint 3 | Content source lifecycle; TV5 consumes approved read model. |
| UC-76 | MF-06 AI risk/red-flag rule configuration | TV5 — Chương | Sprint 2 | Triaging module owns execution and rule versioning. |
| UC-77–UC-82 | MF-07 Emergency Map & Nearby Care Support | TV4 — Lâm | Sprint 1–2 | TV5 passes handoff context; TV4 owns map/location. |
| UC-83–UC-88 | MF-08 Personal Health Records & Source Labeling | TV2 — Bách | Sprint 3 | Permission enforcement through TV1. |
| UC-89–UC-93 | MF-09 Reminders, Tasks & Care Plan | TV2 — Bách | Sprint 3 | Notification delivery through TV1. |
| UC-94–UC-101 | MF-10 Family Sync & Cooperative Care | TV2 — Bách | Sprint 4 | Requires stable consent and notification contracts. |
| UC-102–UC-108 | MF-11 Verified Content & Checklist Hub | TV3 — Huy | Sprint 3 | Supplies reviewed content to user and AI source governance. |
| UC-109–UC-111 | MF-12 Expense & Preparation Planner | TV2 — Bách | Sprint 4 | P1 household-preparation feature. |
| UC-112–UC-115 | MF-13 Connected Device & Health Data Integration | TV2 — Bách | Sprint 4 | Mock/manual import is acceptable; quality labels are mandatory. |
| UC-116–UC-121 | MF-14 Smart Activity Monitoring & Safety Support | TV5 — Chương | Sprint 3–4 | Alert through TV1; map handoff through TV4. |

---

## 11. Explicit No-Conflict Handoffs

| Scenario | Producer | Consumer | Handoff rule |
| --- | --- | --- | --- |
| Verified expert answers a community question | TV4 | TV3 | TV4 supplies `verifiedExpertId` and badge state. TV3 creates the answer record. |
| Red-risk AI result opens emergency support | TV5 | TV4 | TV5 creates a `triageHandoffId`; TV4 reads only minimum permitted context. |
| Smart safety event sends a family emergency alert | TV5 | TV1 / TV4 | TV5 creates safety event; TV1 delivers notification; TV4 handles location/map only after consent. |
| Health summary or record is shared | TV2 | TV1 / TV4 | TV2 owns the source record; TV1 validates permission; TV4 may consume only authorized read model. |
| Reminder becomes a push/in-app notification | TV2 | TV1 | TV2 publishes a reminder event; TV1 controls notification delivery and read state. |
| Community content becomes AI knowledge | TV3 | TV5 | TV3 exposes only approved/versioned source chunks; TV5 never writes content publication state. |

---

## 12. Definition of Done for Every Assigned Use Case

A use case is not “done” until it has:

1. An authorized API/service flow owned by the assigned domain.
2. A working mobile/web screen or explicitly documented non-screen trigger.
3. Validation and error states, including denied/expired consent where applicable.
4. Required audit/notification events through TV1 contracts.
5. Unit/integration tests for core business rules.
6. A small demo script or evidence showing the use case works with seeded data.
7. No direct repository/entity dependency on another member’s domain.

