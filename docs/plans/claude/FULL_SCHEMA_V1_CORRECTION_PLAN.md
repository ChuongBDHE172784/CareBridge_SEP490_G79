---
title: FULL_SCHEMA_V1_CORRECTION_PLAN
project: CareBridge_SEP490_G79
Status: DRAFT
Approval: PENDING_USER_APPROVAL
Mode: READ_ONLY_SCHEMA_RECONCILIATION
Supabase Modified: NO
created: 2026-06-25
author: AI Agent
---

# Full Schema V1 Correction Plan

> **Read-only audit.** No files have been modified. This plan requires explicit user approval
> before any implementation begins.

---

## 1. Executive Decision

### Why 25 Core-only tables are insufficient

The Phase B.1 decision to reduce V1 to 25 CORE + CURRENT_IMPLEMENTED_UC tables was made
to improve local dev stability after the Phase A audit. That decision applied the wrong
framing: it optimized for "tables the current code touches" rather than "the authoritative
schema contract for the project."

The CareBridge project has five developers, each owning a distinct domain module. The
database schema is a shared contract — not a per-feature artifact. Reducing V1 to 25
tables means:

1. **Day-one blockers for other developers.** The four non-HuyND developers cannot run
   their feature implementations (care journey, consultation, payment, exercise, safety,
   etc.) without adding V2+ migrations for foundational tables that should have been in V1.

2. **V2+ proliferation for non-schema reasons.** Each new feature would require a Vn
   migration just to add a table that was always in the approved ERD. This corrupts the
   Flyway history: V2+ should represent *schema evolution*, not schema completion.

3. **Supabase reset creates an incomplete schema.** After Phase C reset, the app starts with
   only 25 tables. Any team member implementing consultation or payment immediately faces a
   Hibernate `ddl-auto: validate` failure because their entities have no tables.

4. **The approved ERD is the scope contract.** The project design documents, architecture
   decisions, and module ownership assignments all reference the full 67-table ERD. Reducing
   V1 below that scope is a unilateral schema scope reduction that conflicts with the project
   design authority.

### Why a complete V1 master schema is required

The project uses `ddl-auto: validate`. This means Hibernate only validates tables that have
`@Entity` mappings. Tables without a current entity are invisible to Hibernate — they cause
no startup failure and no test failure. Including them in V1 has zero runtime cost and
maximum project benefit:

- Future developers find their tables already provisioned on day one.
- V2+ migrations are reserved for genuine post-baseline schema changes.
- The Supabase shared dev environment is immediately usable by all five developers.
- The schema serves as living documentation of the approved ERD.

---

## 2. Source-of-Truth Hierarchy Used in This Audit

| Priority | Source | Usage |
|---|---|---|
| 1 | `03_Design/Database/CareBridge_ERD_Logical_Model_Updated.puml` | Authoritative table/column names, FKs |
| 2 | Current Java entities (12 active `@Entity` classes) | Column types, constraints for live tables |
| 3 | Original `V1__init_schema.sql` (git HEAD blob) | Column structures for future-domain tables |
| 4 | V2–V12 migration semantics (git HEAD blobs) | Corrections applied to original V1 |
| 5 | Current `V1__baseline.sql` (untracked) | Supabase-verified definitions for 25 tables |
| 6 | CLAUDE.md entity ownership table | Module boundaries, entity assignments |

---

## 3. Migration File State (Working Tree vs Git HEAD)

**Critical finding:** The `V1__baseline.sql` (25-table lean version) is an **untracked file**
that has never been committed. The original V1–V12 migration files ARE committed at HEAD but
are deleted in the working tree (unstaged deletions).

| File | Git HEAD | Working Tree | Status |
|---|---|---|---|
| `V1__init_schema.sql` | ✅ Committed (blob `3cd561c`) | ❌ Deleted (unstaged) | Recoverable from git |
| `V2__alter_community_topics.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V3__add_stage_to_content_items.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V4__create_partner_organizations.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V5__create_community_questions.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V6__add_description_to_checklist_templates.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V7__create_community_answers.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V8__add_content_search_indexes.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V9__alter_community_user_refs_to_uuid.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V10__alter_partner_representative_to_uuid.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V11__add_enabled_locked_to_users.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V12__add_role_to_users.sql` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |
| `V1__baseline.sql` (25 tables, lean) | ❌ Not committed | ✅ Present (untracked) | Would be lost on git restore |
| `.gitkeep` | ✅ Committed | ❌ Deleted (unstaged) | Recoverable from git |

**The V2–V12 content summary:**

| File | Purpose |
|---|---|
| V2 | Rename `topic_id` → `id`; drop `is_active/slug/risk_level`; add `is_hidden/icon/sort_order/created_by`; add lowercase unique index on name |
| V3 | Add `stage` column to `content_items`; add performance indexes |
| V4 | **DROP** and recreate `partner_organizations` with proper UUID PK and UC-118 columns |
| V5 | Create `community_questions` with UUID FK to `community_topics(id)` |
| V6 | Add `description TEXT` to `checklist_templates`; add composite indexes |
| V7 | Create `community_answers` with UUID FK to `community_questions(id)` |
| V8 | Add CONCURRENTLY search indexes on `content_items` |
| V9 | **TRUNCATE** `community_answers`, `community_questions`; alter `author_id` BIGINT → UUID; alter `community_topics.created_by` → UUID |
| V10 | **TRUNCATE** `partner_organizations`; alter `representative_user_id` BIGINT → UUID |
| V11 | Add `enabled BOOLEAN NOT NULL DEFAULT TRUE`, `locked BOOLEAN NOT NULL DEFAULT FALSE` to `users` |
| V12 | Add `role VARCHAR(50) NOT NULL DEFAULT 'MOTHER'` to `users` |

**Important:** V9 and V10 contain `TRUNCATE ... CASCADE`. This is safe only against an empty or test database. On the shared Supabase these already ran. After Phase C reset, none of V2–V12 will run (they will be consolidated into V1).

---

## 4. Full Table Inventory

### ERD Tables (67) — Authoritative Source

| # | Table | Module | ERD Package | Impl. Status | In 25-table V1 | In orig. V1 | Java Entity | Decision | Rationale |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `roles` | security/identity | Identity & Access | CORE_IMPLEMENTED | ✅ | ✅ | via Spring Security | KEEP | RBAC lookup table, active |
| 2 | `users` | security | Identity & Access | CORE_IMPLEMENTED | ✅ | ✅ | `User.java` | KEEP | Central auth entity |
| 3 | `user_roles` | identity | Identity & Access | CORE_IMPLEMENTED | ✅ | ✅ | no direct entity (loaded via Spring) | KEEP | Multi-role RBAC assignment |
| 4 | `user_sessions` | identity | Identity & Access | CORE_IMPLEMENTED | ✅ | ✅ | no current entity | KEEP | Session lifecycle, logout invalidation |
| 5 | `community_profiles` | identity | Identity & Access | CORE_IMPLEMENTED | ✅ | ✅ | no current entity | KEEP | Anonymous display in community |
| 6 | `notification_preferences` | identity | Identity & Access | CORE_IMPLEMENTED | ✅ | ✅ | no current entity | KEEP | Push notification opt-in |
| 7 | `notifications` | identity | Identity & Access | CORE_IMPLEMENTED | ✅ | ✅ | no current entity | KEEP | Notification inbox |
| 8 | `data_permissions` | consent | Identity & Access | CORE_IMPLEMENTED | ✅ | ✅ | no current entity | KEEP | Consent scope enforcement |
| 9 | `audit_logs` | audit | Identity & Access | CORE_IMPLEMENTED | ✅ | ✅ | `AuditLog.java` | KEEP | Append-only audit trail |
| 10 | `mother_journeys` | carejourney | Care Journey | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Core care journey module, approved ERD |
| 11 | `maternal_health_metrics` | carejourney | Care Journey | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Journey metrics tracking |
| 12 | `postpartum_logs` | carejourney | Care Journey | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Postpartum recovery logging |
| 13 | `baby_profiles` | babycare | Baby Care | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Baby profile ownership |
| 14 | `baby_daily_logs` | babycare | Baby Care | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Daily baby care logging |
| 15 | `development_milestones` | babycare | Baby Care | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Baby development tracking |
| 16 | `growth_measurements` | babycare | Baby Care | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Weight/height growth chart |
| 17 | `vaccination_records` | babycare | Baby Care | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Vaccine schedule tracking |
| 18 | `health_records` | healthrecord | Health Records | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Medical record files |
| 19 | `health_summaries` | healthrecord | Health Records | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | AI-assisted health summaries |
| 20 | `reminders` | reminder | Care Coordination | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Appointment/medication reminders |
| 21 | `care_groups` | carecoordination | Care Coordination | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Family care group |
| 22 | `care_group_members` | carecoordination | Care Coordination | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Group membership and permissions |
| 23 | `care_tasks` | carecoordination | Care Coordination | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Shared task assignment |
| 24 | `expenses` | carecoordination | Care Coordination | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Shared expense tracking |
| 25 | `community_topics` | community | Community & Content | CORE_IMPLEMENTED | ✅ | ✅ | `CommunityTopic.java` | KEEP | Active (current V1 uses PK `id`) |
| 26 | `community_questions` | community | Community & Content | EARLY_UC_IMPLEMENTED | ✅ | via V5 | `CommunityQuestion.java` | KEEP | Active (current V1 uses PK `id`) |
| 27 | `community_answers` | community | Community & Content | EARLY_UC_IMPLEMENTED | ✅ | via V7 | `CommunityAnswer.java` | KEEP | Active (current V1 uses PK `id`) |
| 28 | `content_reports` | content | Community & Content | EARLY_UC_IMPLEMENTED | ✅ | ✅ | `ContentReport.java` | KEEP | Moderation input |
| 29 | `moderation_actions` | content | Community & Content | EARLY_UC_IMPLEMENTED | ✅ | ✅ | `ModerationAction.java` | KEEP | Moderation output |
| 30 | `content_items` | content | Community & Content | EARLY_UC_IMPLEMENTED | ✅ | ✅ | `ContentItem.java` | KEEP | Articles, FAQ, checklists |
| 31 | `checklist_templates` | content | Community & Content | EARLY_UC_IMPLEMENTED | ✅ | ✅ | `ChecklistTemplate.java` | KEEP | Checklist template |
| 32 | `checklist_items` | content | Community & Content | EARLY_UC_IMPLEMENTED | ✅ | ✅ | `ChecklistItem.java` | KEEP | Checklist line items |
| 33 | `expert_profiles` | expert | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Expert verification hub |
| 34 | `expert_credentials` | expert | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | License and credential documents |
| 35 | `expert_availability` | expert | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Availability calendar |
| 36 | `expert_location_shares` | expert | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Emergency location consent |
| 37 | `consultation_bookings` | consultation | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Booking lifecycle |
| 38 | `consultation_sessions` | consultation | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Active session management |
| 39 | `consultation_messages` | consultation | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | In-session messages |
| 40 | `payment_transactions` | payment | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | VNPay payment records |
| 41 | `commission_records` | payment | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Commission calculation |
| 42 | `expert_reviews` | expert | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Post-consultation ratings |
| 43 | `contribution_points` | community | Expert & Consultation | EARLY_UC_IMPLEMENTED | ✅ | ✅ | no entity yet | KEEP | Community reward points |
| 44 | `consultation_price_bands` | payment | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Admin-set price floor/ceiling |
| 45 | `expert_consultation_prices` | payment | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Expert-set prices within bands |
| 46 | `consultation_disputes` | payment | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Dispute resolution flow |
| 47 | `refund_records` | payment | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Refund processing |
| 48 | `settlement_records` | payment | Expert & Consultation | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Expert payout settlement |
| 49 | `triage_assessments` | triage | AI & Safety | EARLY_UC_IMPLEMENTED | ✅ | ✅ | no entity (used via JPA query) | KEEP | RAG context retrieval |
| 50 | `triage_answers` | triage | AI & Safety | EARLY_UC_IMPLEMENTED | ✅ | ✅ | no entity (used via JPA query) | KEEP | Per-question triage answers |
| 51 | `partner_organizations` | partner | Partner & Location | EARLY_UC_IMPLEMENTED | ✅ | ✅ (V4 rebuilt) | `PartnerOrganization.java` | KEEP | Partner profile (UC-118 live) |
| 52 | `partner_expert_links` | partner | Partner & Location | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Expert-partner affiliation |
| 53 | `partner_services` | partner | Partner & Location | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Partner service listings |
| 54 | `sponsored_campaigns` | partner | Partner & Location | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Sponsored content governance |
| 55 | `care_facilities` | partner | Partner & Location | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | TrackAsia-linked facilities |
| 56 | `emergency_events` | emergency | Partner & Location | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Emergency flow tracking |
| 57 | `location_snapshots` | emergency | Partner & Location | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Consent-gated location data |
| 58 | `health_device_connections` | device | Device & Smart Safety | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Wearable OAuth connections |
| 59 | `device_measurements` | device | Device & Smart Safety | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Sensor reading log |
| 60 | `safety_monitoring_settings` | safety | Device & Smart Safety | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Fall/inactivity detection config |
| 61 | `safety_events` | safety | Device & Smart Safety | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Detected safety incidents |
| 62 | `safety_alerts` | safety | Device & Smart Safety | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Alerts sent to emergency contacts |
| 63 | `pregnancy_exercises` | exercise | Pregnancy Exercise | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Exercise library |
| 64 | `exercise_safety_checks` | exercise | Pregnancy Exercise | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Pre-exercise safety gate |
| 65 | `exercise_sessions` | exercise | Pregnancy Exercise | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Active exercise session |
| 66 | `posture_analysis_configs` | exercise | Pregnancy Exercise | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | MediaPipe posture rules |
| 67 | `posture_feedback_events` | exercise | Pregnancy Exercise | FUTURE_IMPLEMENTATION | ❌ | ✅ | no entity yet | ADD | Real-time posture feedback |

### Infrastructure Tables (not in ERD, but required)

| # | Table | Source | In 25-table V1 | In orig. V1 | Decision | Rationale |
|---|---|---|---|---|---|---|
| 68 | `refresh_tokens` | Auth security.entity | ✅ | ✅ | KEEP | JWT refresh flow; `RefreshToken.java` entity active |
| 69 | `otp_verifications` | Auth security.entity | ✅ | ✅ | KEEP | Phone OTP auth flow; `OtpVerification.java` entity active |
| 70 | `consent_grants` | Consent consent.entity | ✅ | ✅ | KEEP | Privacy/legal consent; `ConsentGrant.java` entity active |
| 71 | `security_events` | Audit audit.entity | ✅ | ✅ | KEEP | Security incident log; `SecurityEvent.java` entity active |
| ? | `token_blacklist` | Security session (no entity) | ❌ | ✅ | **QUESTION — see Section 10** | Was in original V1, no active Java entity, may be superseded by `user_sessions.revoked_at` approach |

**Recommended total: 71 tables** (if `token_blacklist` is excluded) **or 72** (if kept).

---

## 5. Gap Analysis: Earlier V1 vs Current 25-table V1 vs Recommended Final V1

| Category | Original V1 (72 tables) | Current 25-table V1 | Recommended Final V1 (71–72) |
|---|---|---|---|
| Identity & Access core (roles, users, user_roles, user_sessions, community_profiles, notification_preferences, notifications, data_permissions, audit_logs) | ✅ All 9 | ✅ All 9 | ✅ All 9 |
| Infra (refresh_tokens, otp_verifications, consent_grants, security_events) | ✅ All 4 | ✅ All 4 | ✅ All 4 |
| Care Journey (3 tables) | ✅ | ❌ All 3 missing | ✅ Restored |
| Baby Care (5 tables) | ✅ | ❌ All 5 missing | ✅ Restored |
| Health Records (2 tables) | ✅ | ❌ All 2 missing | ✅ Restored |
| Care Coordination (5 tables) | ✅ | ❌ All 5 missing | ✅ Restored |
| Community & Content (8 tables) | ✅ | ✅ All 8 present | ✅ All 8 kept |
| Expert (6 tables: profiles+credentials+availability+location+reviews+price_bands) | ✅ | ❌ All 6 missing | ✅ Restored |
| Consultation (3 tables) | ✅ | ❌ All 3 missing | ✅ Restored |
| Payment (6 tables) | ✅ | ❌ All 6 missing | ✅ Restored |
| Partner (4 tables: expert_links, services, campaigns, facilities) | ✅ | ❌ All 4 missing | ✅ Restored |
| Emergency & Location (2 tables) | ✅ | ❌ Both missing | ✅ Restored |
| Device & Smart Safety (5 tables) | ✅ | ❌ All 5 missing | ✅ Restored |
| Pregnancy Exercise (5 tables) | ✅ | ❌ All 5 missing | ✅ Restored |
| `token_blacklist` | ✅ | ❌ Missing | ❓ QUESTION |
| Total | 72 | 25 | 71 (or 72) |

### Why each missing table disappeared from the 25-table V1

The 47 table removal happened in the Phase B.1 session via a Python script that classified
all tables as CORE / CURRENT_IMPLEMENTED_UC / FUTURE_FEATURE. Tables with no active
`@Entity` mapping were classified as FUTURE_FEATURE and removed. This classification logic
was wrong for a project where:
- Database schema scope ≠ current Java entity scope.
- Future developers own and WILL implement those domains.
- The approved ERD is the schema contract, not the current Java entity set.

Every removed table:
- IS present in the approved ERD (`CareBridge_ERD_Logical_Model_Updated.puml`).
- WAS present in the original `V1__init_schema.sql` (committed, git HEAD).
- Has an assigned domain module owner in CLAUDE.md.
- Will be needed by a specific team member.

No removed table is obsolete, duplicate, or replaced.

---

## 6. Existing Schema Discrepancies Between ERD and Current Implementation

These divergences exist NOW and must be documented. They do NOT block the V1 correction,
but they are questions the team must decide on consciously.

### D1 — Community table PKs use `id` instead of ERD column names

| Table | ERD PK column | Current implementation PK | Impact |
|---|---|---|---|
| `community_topics` | `topic_id` | `id` | Entity uses `id`; ERD shows `topic_id` |
| `community_questions` | `question_id` | `id` | Entity uses `id`; ERD shows `question_id` |
| `community_answers` | `answer_id` | `id` | Entity uses `id`; ERD shows `answer_id` |

**Assessment:** The current implementation is tested and working. Renaming PKs would require
entity changes and re-test. The recommended path is to keep `id` as authoritative for these
three tables and accept the divergence from ERD column naming.

**This requires user decision (see Section 10, Q2).**

### D2 — `community_answers` schema differs from ERD

| Column | ERD | Current V1 | Current Entity |
|---|---|---|---|
| Answer content | `content` | `body` | `body` |
| Expert indicator | `answer_type` + `expert_profile_id` | `is_expert_labeled` (BOOLEAN) | `isExpertLabeled` |
| Personal flag | (not in ERD) | `is_personal_experience` (BOOLEAN) | `isPersonalExperience` |
| Moderation | `moderation_status` | `status` | `status` |
| Helpful votes | `helpful_count` | `like_count` | `likeCount` |

**Assessment:** The current implementation reflects the UC-56 TDS decisions. The ERD's
`answer_type` + `expert_profile_id` approach was replaced with a simpler boolean pattern.
Current implementation is authoritative.

### D3 — `users` table missing ERD columns

| ERD Column | In original V1 | In current V1 | Status |
|---|---|---|---|
| `account_status` | ✅ | ❌ Removed | Was in V1, removed in Phase B redesign |
| `email_verified` | ✅ | ❌ Removed | Was in V1, removed |
| `phone_verified` | ✅ | ❌ Removed | Was in V1, removed |
| `last_login_at` | ✅ | ❌ Removed | Was in V1, removed |
| `locked_at` | ✅ | ❌ Removed | Was in V1, removed |
| `enabled` | via V11 | ✅ | Added via V11 |
| `locked` | via V11 | ✅ | Added via V11 |
| `role` | via V12 | ✅ | Added via V12 |

**Assessment:** The b64b837 "UUID migration" commit removed these columns from the entity and
therefore from the pg_dump V1 baseline. The current User entity maps only to the columns in
the current V1. This is a conscious design simplification. It may conflict with future feature
requirements (e.g., email verification flow).

**This requires user decision (see Section 10, Q3).**

### D4 — `consent_grants`, `security_events`, `otp_verifications`, `refresh_tokens` use BIGINT identity PKs

These infrastructure tables use `BIGINT GENERATED BY DEFAULT AS IDENTITY` as their primary
key instead of UUID. Their Java entities use `@GeneratedValue(strategy = IDENTITY)`.

This is intentional for high-throughput write tables (OTP, tokens, security events). No
change recommended.

---

## 7. Migration Reconstruction Plan

### Option A — Restore V1–V12, keep individual file history (NOT recommended)

Restore all 12 files from git HEAD. This preserves the incremental history but:
- V9 and V10 contain `TRUNCATE ... CASCADE` which is a destructive operation on a non-empty DB.
- After Supabase reset the DB is empty, so TRUNCATE is technically safe, but it's bad practice.
- The incremental approach creates 12 migration files for what is fundamentally a single baseline.
- Future developers see a confusing V1 → V12 "bootstrap" sequence.

**Verdict: Not recommended.**

### Option B — Consolidated V1__baseline.sql with all 71–72 tables (RECOMMENDED)

Create a single new `V1__baseline.sql` that contains ALL 71–72 approved tables in
PostgreSQL DDL format (matching the style of the current 25-table V1 but with all tables).

Sources:
- 25 tables from current `V1__baseline.sql` (untracked, Supabase-verified pg_dump style) — use as-is.
- 47 missing tables from `V1__init_schema.sql` (git HEAD blob `3cd561c`) — apply UUID corrections.
- No V2–V12 needed (their changes are absorbed into the consolidated baseline).

The consolidated V1 is the source-of-truth for the post-Phase-C Supabase state.

**Verdict: Recommended.**

### UUID corrections needed for the 47 restored tables

The original `V1__init_schema.sql` was Hibernate-generated from entities that had BIGINT IDs.
V9 and V10 changed community and partner user references to UUID. Several other tables still
have BIGINT FK references that need to be corrected to UUID in the consolidated V1:

| Table | Column | Original type | Required type | Why |
|---|---|---|---|---|
| `community_questions` | `author_id` | BIGINT | UUID | Fixed in V9 |
| `community_answers` | `author_id` | BIGINT | UUID | Fixed in V9 |
| `community_topics` | `created_by` | BIGINT | UUID | Fixed in V9 |
| `partner_organizations` | `representative_user_id` | BIGINT | UUID | Fixed in V10 |
| All `_by` / `_user_id` columns in future tables | Any BIGINT | UUID | UUID | Consistent with `users.user_id UUID` |

Additional style corrections for the 47 restored tables:
- Column ordering: logical order (not alphabetical Hibernate order) per the ERD.
- Consistent `NOT NULL` on `created_at` and `updated_at`.
- `timestamptz` (or `timestamp with time zone`) for all timestamps.
- `UUID NOT NULL PRIMARY KEY` for all domain tables (not BIGINT).
- CHECK constraints for status/enum columns where the ERD specifies allowed values.

### Files that will change in Phase B.2B (execution phase)

```text
DELETED:
  src/main/resources/db/migration/V1__init_schema.sql    (restore then replace with consolidated)
  src/main/resources/db/migration/V2__alter_community_topics.sql
  src/main/resources/db/migration/V3__add_stage_to_content_items.sql
  src/main/resources/db/migration/V4__create_partner_organizations.sql
  src/main/resources/db/migration/V5__create_community_questions.sql
  src/main/resources/db/migration/V6__add_description_to_checklist_templates.sql
  src/main/resources/db/migration/V7__create_community_answers.sql
  src/main/resources/db/migration/V8__add_content_search_indexes.sql
  src/main/resources/db/migration/V9__alter_community_user_refs_to_uuid.sql
  src/main/resources/db/migration/V10__alter_partner_representative_to_uuid.sql
  src/main/resources/db/migration/V11__add_enabled_locked_to_users.sql
  src/main/resources/db/migration/V12__add_role_to_users.sql

REPLACED:
  src/main/resources/db/migration/V1__baseline.sql       (25 tables → 71+ tables)
```

---

## 8. Entity Alignment Plan

### Currently active Java entities (12 mapped, all in 25-table V1)

| Entity class | Package | Table | Status |
|---|---|---|---|
| `User.java` | `security.entity` | `users` | Active, tested |
| `RefreshToken.java` | `security.entity` | `refresh_tokens` | Active, tested |
| `OtpVerification.java` | `security.entity` | `otp_verifications` | Active, tested |
| `AuditLog.java` | `audit.entity` | `audit_logs` | Active, tested |
| `SecurityEvent.java` | `audit.entity` | `security_events` | Active, tested |
| `ConsentGrant.java` | `consent.entity` | `consent_grants` | Active |
| `CommunityTopic.java` | `community.entity` | `community_topics` | Active, tested |
| `CommunityQuestion.java` | `community.entity` | `community_questions` | Active, tested |
| `CommunityAnswer.java` | `community.entity` | `community_answers` | Active, tested |
| `ContentItem.java` | `content.entity` | `content_items` | Active, tested |
| `ContentReport.java` | `content.entity` | `content_reports` | Active, tested |
| `ChecklistTemplate.java` | `content.entity` | `checklist_templates` | Active, tested |
| `ChecklistItem.java` | `content.entity` | `checklist_items` | Active, tested |
| `ModerationAction.java` | `content.entity` | `moderation_actions` | Active, tested |
| `PartnerOrganization.java` | `partner.entity` | `partner_organizations` | Active, tested |

### Tables in current 25-table V1 with NO current Java entity (provisioned but not mapped)

| Table | Future module owner | Purpose |
|---|---|---|
| `roles` | identity | RBAC lookup (Spring Security loads directly) |
| `user_roles` | identity | Role assignment join |
| `user_sessions` | identity | Session invalidation |
| `community_profiles` | identity | Anonymous display names |
| `notification_preferences` | identity | Push opt-in settings |
| `notifications` | identity | Notification inbox |
| `data_permissions` | consent | Data sharing grants |
| `contribution_points` | community | Community reward points |
| `triage_assessments` | triage | RAG context (used via JPA projection) |
| `triage_answers` | triage | RAG context (used via JPA projection) |

### Tables to be added (47 tables) with assigned module ownership

All 47 tables will be provisioned in the new V1 but will have no Java entity until each
module developer implements their feature. This is intentional. Hibernate `ddl-auto: validate`
will ignore tables that have no `@Entity` — they are transparent to the running application.

| Module | Tables to be provisioned |
|---|---|
| `carejourney` | `mother_journeys`, `maternal_health_metrics`, `postpartum_logs` |
| `babycare` | `baby_profiles`, `baby_daily_logs`, `development_milestones`, `growth_measurements`, `vaccination_records` |
| `healthrecord` | `health_records`, `health_summaries` |
| `carecoordination` | `care_groups`, `care_group_members`, `care_tasks`, `expenses`, `reminders` |
| `expert` | `expert_profiles`, `expert_credentials`, `expert_availability`, `expert_location_shares`, `expert_reviews` |
| `consultation` | `consultation_bookings`, `consultation_sessions`, `consultation_messages` |
| `payment` | `payment_transactions`, `commission_records`, `consultation_price_bands`, `expert_consultation_prices`, `consultation_disputes`, `refund_records`, `settlement_records` |
| `partner` | `partner_expert_links`, `partner_services`, `sponsored_campaigns`, `care_facilities` |
| `emergency` | `emergency_events`, `location_snapshots` |
| `device` | `health_device_connections`, `device_measurements` |
| `safety` | `safety_monitoring_settings`, `safety_events`, `safety_alerts` |
| `exercise` | `pregnancy_exercises`, `exercise_safety_checks`, `exercise_sessions`, `posture_analysis_configs`, `posture_feedback_events` |

---

## 9. Flyway Plan

### V1 filename

```
V1__baseline.sql
```

The filename stays `V1__baseline.sql`. The content expands from 25 to 71–72 tables.

### Migration count after correction

```
Before correction:   1 file (V1__baseline.sql, 25 tables, untracked)
                   + 12 files deleted in working tree (V1–V12 original)

After correction:    1 file (V1__baseline.sql, 71–72 tables)
```

V2–V12 are eliminated because their changes (column renames, UUID casts, index additions)
are absorbed into the consolidated V1. Future schema changes start at V2.

### Local dev impact

After Phase B.2B implementation:
1. The V1 checksum changes and 46 new tables are added.
2. Developers must **recreate their local database from scratch** — `flyway:repair` alone is NOT valid because it only updates checksum metadata and does not add the 46 new tables.
3. See `05_Development/Database/postgres/LOCAL_DATABASE_REBASELINE_RUNBOOK.md` for the exact steps.

### Empty-DB rehearsal plan (before Supabase reset)

Same pattern as Phase B.1 rehearsal:
```bash
createdb -U postgres carebridge_fulltest
./mvnw flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/carebridge_fulltest \
  -Dflyway.user=postgres \
  -Dflyway.password=password
# Verify: 71–72 tables
psql -U postgres -d carebridge_fulltest -c \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name != 'flyway_schema_history';"
dropdb -U postgres carebridge_fulltest
```

Rehearsal must pass before the audit verdict is updated to `READY_FOR_SHARED_DB_RESET`.

### Supabase impact

**None in Phase B.2A or B.2B (source only).**

The Supabase shared-dev database currently has the legacy V1–V12 schema. After Phase C reset
(separate explicit authorization), the new consolidated V1 will create all 71–72 tables
cleanly on the empty schema.

---

## 10. Impact on Current Core APIs and UI

The 47 restored tables have no current Java entity mappings. Adding them to V1 does not
change the running application in any way. Hibernate `ddl-auto: validate` will not fail
because it only validates tables it knows about (via `@Entity`).

**No existing implemented use case will be broken by this change.**

Specific verification:
- All 242 backend tests will continue to pass (no entity additions required).
- `npm run build` for web will continue to pass.
- Flutter build will continue to pass.
- Developers must recreate their local DB from scratch (not `flyway:repair`). See `LOCAL_DATABASE_REBASELINE_RUNBOOK.md`.

---

## 11. Handoff Plan for Five Developers

### Module-to-table ownership

| Developer | Module | Tables to implement |
|---|---|---|
| **HuyND** (this branch) | security, identity, audit, consent, community, content, partner, triage | All 25 current + infra |
| **Developer 2** (carejourney/babycare) | carejourney, babycare, healthrecord, reminder | `mother_journeys`, `maternal_health_metrics`, `postpartum_logs`, `baby_profiles`, `baby_daily_logs`, `development_milestones`, `growth_measurements`, `vaccination_records`, `health_records`, `health_summaries`, `reminders` (11 tables) |
| **Developer 3** (carecoordination/consultation) | carecoordination, expert, consultation | `care_groups`, `care_group_members`, `care_tasks`, `expenses`, `expert_profiles`, `expert_credentials`, `expert_availability`, `expert_location_shares`, `expert_reviews`, `consultation_bookings`, `consultation_sessions`, `consultation_messages` (12 tables) |
| **Developer 4** (payment/partner) | payment, partner.extended, emergency | `payment_transactions`, `commission_records`, `consultation_price_bands`, `expert_consultation_prices`, `consultation_disputes`, `refund_records`, `settlement_records`, `partner_expert_links`, `partner_services`, `sponsored_campaigns`, `care_facilities`, `emergency_events`, `location_snapshots` (13 tables) |
| **Developer 5** (device/safety/exercise) | device, safety, exercise | `health_device_connections`, `device_measurements`, `safety_monitoring_settings`, `safety_events`, `safety_alerts`, `pregnancy_exercises`, `exercise_safety_checks`, `exercise_sessions`, `posture_analysis_configs`, `posture_feedback_events` (10 tables) |

### Rule for creating V2+ migrations

```text
V2+ migrations are for SCHEMA EVOLUTION, not schema completion.

A developer should create a V(n+1) migration ONLY when they need to:
- Add a column to an existing table.
- Change a column type or constraint.
- Add a new index for performance.
- Add a genuinely NEW table that was not approved in the ERD
  (requires a design review decision first).

A developer should NOT create a V(n+1) migration merely to:
- Create a table that is already in V1.
- Add a table that was in the approved ERD but "not implemented yet".
```

### Developer onboarding steps after Phase B.2B merges

```bash
git pull
# ⚠️ DO NOT run flyway:repair — it does not add the 46 new tables
# Recreate your local DB instead:
dropdb carebridge && createdb carebridge
./mvnw flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/carebridge \
  -Dflyway.user=postgres \
  -Dflyway.password=password
./mvnw test           # should pass 242/242
```
See `05_Development/Database/postgres/LOCAL_DATABASE_REBASELINE_RUNBOOK.md` for backup and data preservation options.

---

## 12. Risks, Ambiguities, and Required User Decisions

### Q1 — `token_blacklist` table: keep or remove?

**Background:** The original `V1__init_schema.sql` contained a `token_blacklist` table with
columns `id UUID`, `token_hash VARCHAR(255) UNIQUE`, `expires_at TIMESTAMPTZ`, `revoked_at TIMESTAMPTZ`.
This table is intended for JWT token invalidation (e.g., after user logout or security event).

**Current state:** No Java entity or repository exists for it. The `user_sessions` table
already has `revoked_at` and `status` columns that support logout invalidation. However,
if the access token itself (not just the session) needs to be blacklisted mid-TTL
(e.g., emergency account suspend), `user_sessions` alone is insufficient.

**Question for user:** Should `token_blacklist` be included in the master V1?

- Option A: **Include** — future security hardening may need per-token revocation.
- Option B: **Exclude** — current 15-minute TTL + `user_sessions.revoked_at` is sufficient;
  add in V2+ only if a genuine security requirement arises.

### Q2 — Community table PK column names: align with ERD or keep current `id`?

**Background:** The ERD defines `topic_id`, `question_id`, `answer_id` as PK column names.
The current implementation uses `id` (introduced in V2 for `community_topics`; built with `id`
for `community_questions` and `community_answers` from the start).

**Changing to `topic_id` etc. would require:**
- Entity changes to all three community entities.
- Repository query updates.
- DTO mapping updates.
- Re-running all community-related tests.
- A V2 migration after V1 to rename the column (or rebuild in the consolidated V1 with the ERD names).

**Question for user:** Align PKs with ERD names (breaking change to entities) or keep `id`?

- Option A: **Align with ERD** — use `topic_id`, `question_id`, `answer_id`. More consistent with ERD documentation; more work now.
- Option B: **Keep `id`** — existing code works; accept divergence from ERD column naming. Recommended as the lower-risk path.

### Q3 — `users` table: restore missing ERD columns?

**Background:** The ERD defines `account_status`, `email_verified`, `phone_verified`,
`last_login_at` on `users`. These were in the original `V1__init_schema.sql` but removed in
the b64b837 simplification commit. The current User entity doesn't have them.

**Restoring them would require:**
- Adding fields to `User.java`.
- Updating auth flows (email verification, account status transitions).
- New tests.

**Question for user:** Restore ERD columns to `users` now (full scope) or defer to V2+ when those flows are implemented?

- Option A: **Restore now** — complete users table per ERD; implement email verification in a future sprint.
- Option B: **Defer** — keep current minimalist users table; add columns in V2+ when email verification is implemented. Recommended.

### Q4 — `community_answers` schema: accept ERD divergence or reconcile?

**Background:** The ERD defines `answer_type`, `expert_profile_id`, `moderation_status`,
`helpful_count`. The current implementation uses `is_expert_labeled`, `is_personal_experience`,
`status`, `like_count`. The UC-56 TDS was the authoritative decision point.

**Question for user:** Should the ERD be updated to match the implementation, or should the implementation be migrated to match the ERD?

- Option A: **Update ERD** (recommended) — ERD is a living document; the TDS decision is the implementation authority for this table.
- Option B: **Migrate implementation** — rename columns and update entities; high disruption.

### Q5 — Column structure for 47 restored tables: Hibernate-generated or ERD-aligned?

The original 47 tables were Hibernate-generated (alphabetical columns, minimal constraints).
The recommended approach is to rewrite them in the pg_dump style of the current 25-table V1,
deriving columns from the ERD PlantUML.

**Question for user:** Should the 47 restored tables use:

- Option A: **ERD-aligned columns** (recommended) — derive column names, types, and constraints from `CareBridge_ERD_Logical_Model_Updated.puml`. More accurate; more work in Phase B.2B.
- Option B: **Hibernate-generated as-is** — restore from original V1__init_schema.sql blobs with only UUID fixes. Faster but columns are in alphabetical order with minimal constraints.

---

## 13. Exact Files That Will Change in Phase B.2B (Execution Phase)

> **None of these files are modified in this audit phase. This section describes what Phase B.2B will do upon approval.**

```text
DELETED from working tree:
  (no deletion — V1__baseline.sql is untracked and will be replaced in-place)

WRITTEN (new content):
  src/main/resources/db/migration/V1__baseline.sql
    FROM: 952 lines, 25 tables (current untracked file)
    TO:   ~3500-4200 lines, 71-72 tables (consolidated master schema)

DISCARDED (untracked deletion confirmed):
  The in-working-tree deletions of V1__init_schema.sql and V2-V12 remain deleted.
  They do not need to be restored because their content is absorbed into the new V1.
  git restore src/main/resources/db/migration/.gitkeep is needed (or included in V1).
```

**No other application source files change in Phase B.2B.**

---

## Appendix: ERD Table Count Verification

Source: `03_Design/Database/CareBridge_ERD_Logical_Model_Updated.puml`

| Package | Tables | Count |
|---|---|---|
| Identity & Access | roles, users, user_roles, user_sessions, community_profiles, notification_preferences, notifications, data_permissions, audit_logs | 9 |
| Care Journey | mother_journeys, maternal_health_metrics, postpartum_logs | 3 |
| Baby Care | baby_profiles, baby_daily_logs, development_milestones, growth_measurements, vaccination_records | 5 |
| Health Records | health_records, health_summaries | 2 |
| Care Coordination | reminders, care_groups, care_group_members, care_tasks, expenses | 5 |
| Community & Content | community_topics, community_questions, community_answers, content_reports, moderation_actions, content_items, checklist_templates, checklist_items | 8 |
| Expert & Consultation | expert_profiles, expert_credentials, expert_availability, expert_location_shares, consultation_bookings, consultation_sessions, consultation_messages, payment_transactions, commission_records, expert_reviews, contribution_points, consultation_price_bands, expert_consultation_prices, consultation_disputes, refund_records, settlement_records | 16 |
| AI & Safety | triage_assessments, triage_answers | 2 |
| Partner & Location | partner_organizations, partner_expert_links, partner_services, sponsored_campaigns, care_facilities, emergency_events, location_snapshots | 7 |
| Device & Smart Safety | health_device_connections, device_measurements, safety_monitoring_settings, safety_events, safety_alerts | 5 |
| Pregnancy Exercise | pregnancy_exercises, exercise_safety_checks, exercise_sessions, posture_analysis_configs, posture_feedback_events | 5 |
| **ERD Total** | | **67** |
| Infrastructure (not in ERD) | refresh_tokens, otp_verifications, consent_grants, security_events | 4 |
| **Recommended V1 Total** | | **71** |
| Optional | token_blacklist | +1 if approved |
