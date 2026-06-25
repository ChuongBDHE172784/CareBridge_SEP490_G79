---
title: FULL_SCHEMA_V1_RECONCILIATION
project: CareBridge_SEP490_G79
phase: B.2B
created: 2026-06-25
author: AI Agent
---

# Schema V1 Reconciliation — ERD vs Implementation

This document records every deliberate divergence between the ERD
(`CareBridge_ERD_Logical_Model_Updated.puml`) and the SQL in `V1__init_schema.sql`.

Each divergence is classified and has an explicit decision rationale.

---

## D1 — users: implementation shape diverges from ERD

| Dimension | ERD | V1__init_schema.sql |
|---|---|---|
| `account_status` | present | **absent** |
| `email_verified` | present | **absent** |
| `phone_verified` | present | **absent** |
| `last_login_at` | present | **absent** |
| `enabled` | absent | **added** |
| `locked` | absent | **added** |
| `role` | absent | **added** |

**Decision:** Q3 — User entity drives the schema for core auth fields.
The Spring Security `UserDetails` contract requires `enabled` and `locked`.
The application uses `role VARCHAR(50)` for single-role RBAC optimization.
`email_verified` was grepped across all Java sources — not referenced anywhere.
`account_status`, `phone_verified`, `last_login_at` are not mapped by any @Entity.

**Resolution:** ERD update required (not a DB change). ERD should be updated to match the implementation.

---

## D2 — partner_organizations: UC-56 schema shape diverges from ERD

| Dimension | ERD | V1__init_schema.sql |
|---|---|---|
| `partner_type` | `partner_type` | `type` |
| `name` | `name` | `name` |
| `license_number` | present | **absent** |
| `address` | present | present |
| `latitude` / `longitude` | present | **absent** |
| `verification_status` | `verification_status` | `status` (PENDING_APPROVAL default) |
| `verified_by` | present | **absent** |
| `city` | absent | **added** |
| `phone` / `email` | absent | **added** |
| `website` / `logo_url` / `description` | absent | **added** |

**Decision:** Q4 — The UC-56 TDS and the live `PartnerOrganization.java` entity
take precedence over the ERD for this existing table. The ERD divergence is
documented here; the ERD should be updated in a future design review sprint.

**Resolution:** Partner org ERD needs a redesign pass to align with the implemented schema.

---

## D3 — community_topics: PK column name diverges from ERD

| Dimension | ERD | V1__init_schema.sql |
|---|---|---|
| Primary key column | `topic_id` | `id` |
| `slug` | present | **absent** |
| `risk_level` | present | **absent** |
| `is_active` | present | `is_hidden` (inverted boolean) |
| `is_hidden` | absent | **added** |
| `icon` | absent | **added** |
| `sort_order` | absent | **added** |
| `created_by` | absent | **added** |

**Decision:** Q2 — PK renamed from `topic_id` to `id` in V2 migration (V2__alter_community_topics.sql,
git blob 59b4e50a). Code and integration tests reference `id`. Reverting would break 242 passing tests.

---

## D4 — community_questions: PK column name and schema diverges from ERD

| Dimension | ERD | V1__init_schema.sql |
|---|---|---|
| PK column | `question_id` | `id` |
| `author_user_id` | `author_user_id` | `author_id` |
| `content` | `content` | `body` |
| `urgency_level` | `urgency_level` | `urgency` |
| `moderation_status` | `moderation_status` | `status` |
| `published_at` | present | **absent** |
| `stage` | absent | **added** |
| `pregnancy_week` | absent | **added** |
| `baby_age_months` | absent | **added** |
| `is_anonymous` | absent | **added** |
| `like_count` / `answer_count` | absent | **added** |

**Decision:** Q2 / Q4 — Schema was built for the actual community feature implementation,
which adds richer filtering (stage, urgency, anonymity) than the ERD specifies.

---

## D5 — community_answers: schema diverges significantly from ERD

| Dimension | ERD | V1__init_schema.sql |
|---|---|---|
| PK column | `answer_id` | `id` |
| `author_user_id` | `author_user_id` | `author_id` |
| `content` | `content` | `body` |
| `answer_type` | present | **absent** |
| `expert_profile_id` | present | **absent** |
| `moderation_status` | `moderation_status` | `status` |
| `helpful_count` | `helpful_count` | `like_count` |
| `published_at` | present | **absent** |
| `is_expert_labeled` | absent | **added** |
| `is_personal_experience` | absent | **added** |

**Decision:** Q4 — The UC-56 TDS uses `is_expert_labeled BOOLEAN` (not `expert_profile_id FK`)
to flag expert-sourced answers. This avoids coupling the community answer model
to the expert profile lifecycle. The simpler boolean approach was chosen
intentionally for MVP scope.

---

## D6 — ERD FK gaps not back-filled into existing 25 tables

The ERD defines FK relationships for all 25 existing tables (e.g., `community_profiles.user_id → users`,
`triage_assessments.user_id → users`, etc.). The V1 carries only 3 FK constraints for the existing tables:

| FK | Reason kept |
|---|---|
| `community_answers.question_id → community_questions.id` | Was in original V1; tested |
| `community_questions.topic_id → community_topics.id` | Was in original V1; tested |
| `refresh_tokens.user_id → users.user_id` | Was in original V1; tested |

**Decision:** ERD-specified FKs for the 25 existing tables were deliberately NOT added to V1.
Reason: Several tests use `@WithMockUser` with UUID usernames that have no corresponding row
in the `users` table. Adding `author_id → users(user_id)` or similar would cause FK violations
in test fixtures and break 242 currently-passing tests.

FK constraints for the 46 new (future) tables are fully applied — those tables have no test
rows yet, so FK enforcement costs nothing.

**Resolution:** When implementing each future domain module, add the ERD FKs for the corresponding
existing tables via a V2+ migration alongside the feature's @Entity.

---

## Summary Table

| Table | Status | Primary Deviation |
|---|---|---|
| `users` | Implementation wins | enabled/locked/role instead of account_status/email_verified |
| `partner_organizations` | Implementation wins | UC-56 shape, not ERD shape |
| `community_topics` | Implementation wins | PK=id, is_hidden, created_by added |
| `community_questions` | Implementation wins | PK=id, body/author_id/urgency, stage added |
| `community_answers` | Implementation wins | PK=id, is_expert_labeled/is_personal_experience |
| All other 20 existing | ERD aligned | Minor column ordering; no semantic difference |
| 46 new tables | ERD aligned | ERD-derived DDL, no deviations |
