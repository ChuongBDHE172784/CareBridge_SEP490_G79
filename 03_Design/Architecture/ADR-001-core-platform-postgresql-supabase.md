---
title: "ADR-001: Core Platform Database — PostgreSQL via Supabase"
status: Accepted
date: 2026-06-25
deciders: HuyND (lead developer)
---

# ADR-001: Core Platform Database — PostgreSQL via Supabase

## Context

CareBridge is a maternal and early childhood healthcare platform. The backend
requires structured relational storage for identity, consent, health records,
community, expert consultation, payment, partner governance, device safety,
and exercise data.

The team evaluated PostgreSQL-on-Supabase against MongoDB Atlas and a raw
self-managed PostgreSQL instance.

## Decision

**Use PostgreSQL hosted on Supabase as the primary relational store.**

Schema versioning is managed with **Flyway 12.4.0**.
The baseline is a single `V1__init_schema.sql` covering all 71 approved tables.

## Rationale

| Factor | PostgreSQL/Supabase | Alternative |
|---|---|---|
| Schema integrity | FK constraints, transactions, typed columns | MongoDB: flexible but no FK enforcement |
| Healthcare data | ACID compliance critical for consent/audit/payment | Event sourcing adds complexity |
| Team familiarity | Spring Data JPA + Hibernate | MongoDB ODM less familiar |
| Supabase features | Row-Level Security, realtime, storage, auth | Raw PG: no managed services |
| MVP timeline | Supabase managed → less ops overhead | Self-hosted PG: needs DevOps setup |
| Flyway integration | Spring Boot Flyway auto-configure | MongoDB: no SQL migration tooling |

## Schema Strategy: Complete Baseline in V1

The V1 migration contains **all 71 approved ERD tables**, not just currently
implemented ones. This is the "Master Database Schema Baseline" pattern:

- Database schema scope ≠ Java entity implementation scope
- Future feature tables exist in PostgreSQL before their @Entity is written
- Hibernate `ddl-auto: validate` only validates mapped @Entity tables
  — future tables are invisible to Hibernate and cause zero startup failures
- Other domain developers can reference the table structure without waiting
  for another developer to create a V2+ migration

## Key Architecture Constraints

1. **Flyway `clean-disabled: true`** — Flyway clean must never run in any profile
2. **`ddl-auto: validate`** — Hibernate must never auto-modify the schema
3. **No `baseline-on-migrate`** — The V1 must apply cleanly to an empty schema
4. **UUID PKs for all domain tables** — Domain entities use `gen_random_uuid()`
4. **BIGINT identity for infrastructure tables** — `refresh_tokens`, `otp_verifications`,
   `consent_grants`, `security_events` use BIGINT sequences for high-throughput append
5. **`token_blacklist` excluded** — Session revocation handled by `user_sessions.revoked_at`

## Schema Divergences from ERD

The following implementation decisions diverge from the ERD. The ERD should
be updated in a future design review sprint to match the implementation:

| Table | Divergence | Decision |
|---|---|---|
| `users` | `enabled/locked/role` instead of `account_status/email_verified` | Spring Security UserDetails contract |
| `partner_organizations` | UC-56 shape (city, phone, email) instead of ERD shape | Active @Entity takes precedence |
| `community_topics/questions/answers` | PK=`id`, not `topic_id/question_id/answer_id` | V2 migration renamed PK; tests locked in |
| `community_answers` | `is_expert_labeled` boolean instead of `expert_profile_id FK` | MVP simplification |

## Consequences

**Positive:**
- All 71 tables available from day 1 for cross-team DB queries and FK references
- No table-creation migrations needed when implementing future features
- Hibernate validation unchanged — mapped entities validate normally
- Single migration file is easy to audit and review

**Negative:**
- V1 is longer and harder to understand without this ADR
- Existing 25-table FK gaps (e.g., `author_id → users`) not back-filled —
  tests using `@WithMockUser` with no `users` row would fail if these FKs were added
- ERD and SQL diverge in 5 documented areas (see FULL_SCHEMA_V1_RECONCILIATION.md)

## Related Documents

- `docs/plans/claude/FULL_SCHEMA_V1_REBASELINE_REPORT.md` — execution evidence
- `docs/plans/claude/FULL_SCHEMA_V1_RECONCILIATION.md` — divergence table
- `docs/CORE_PLATFORM_TABLE_OWNERSHIP.md` — table → module ownership map
- `05_Development/Database/postgres/V1_SCHEMA_BASELINE.md` — schema summary
