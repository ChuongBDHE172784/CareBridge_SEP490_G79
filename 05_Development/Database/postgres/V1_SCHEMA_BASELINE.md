---
title: CareBridge PostgreSQL V1 Schema Baseline
created: 2026-06-25
migration_file: 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql
table_count: 71
---

# CareBridge PostgreSQL V1 Schema Baseline

## File Reference

```
05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql
```

## Coverage

| Category | Count |
|---|---|
| ERD tables covered | 67 / 67 |
| Infrastructure tables (not in ERD) | 4 |
| **Total tables** | **71** |
| FK constraints | 98 |
| Indexes | ~95 (excl. PKs) |

## Table List (alphabetical)

audit_logs, baby_daily_logs, baby_profiles, care_facilities, care_group_members,
care_groups, care_tasks, checklist_items, checklist_templates, commission_records,
community_answers, community_profiles, community_questions, community_topics,
consent_grants, consultation_bookings, consultation_disputes, consultation_messages,
consultation_price_bands, consultation_sessions, content_items, content_reports,
contribution_points, data_permissions, development_milestones, device_measurements,
emergency_events, exercise_safety_checks, exercise_sessions, expenses,
expert_availability, expert_consultation_prices, expert_credentials,
expert_location_shares, expert_profiles, expert_reviews, growth_measurements,
health_device_connections, health_records, health_summaries, location_snapshots,
maternal_health_metrics, moderation_actions, mother_journeys,
notification_preferences, notifications, otp_verifications, partner_expert_links,
partner_organizations, partner_services, payment_transactions, postpartum_logs,
posture_analysis_configs, posture_feedback_events, pregnancy_exercises,
refresh_tokens, refund_records, reminders, roles, safety_alerts, safety_events,
safety_monitoring_settings, security_events, settlement_records, sponsored_campaigns,
triage_answers, triage_assessments, user_roles, user_sessions, users,
vaccination_records

## Infrastructure Tables (not in ERD)

| Table | Purpose |
|---|---|
| `refresh_tokens` | JWT refresh token management (BIGINT identity PK) |
| `otp_verifications` | Phone OTP flow (BIGINT identity PK) |
| `consent_grants` | Privacy consent records (BIGINT identity PK) |
| `security_events` | Security incident log (BIGINT identity PK) |

## Key Constraints

- All domain tables: `UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- Infrastructure tables: `BIGINT IDENTITY` PK for high-throughput append
- `users.email` UNIQUE
- `roles.role_code` UNIQUE
- `expert_profiles.user_id` UNIQUE (one profile per user)
- `consultation_sessions.booking_id` UNIQUE (one session per booking)
- `commission_records.payment_id` UNIQUE (one commission per payment)
- `safety_monitoring_settings.user_id` UNIQUE (one setting per user)

## Applying the Baseline (fresh local DB)

```bash
# Option A — via psql
createdb carebridge
psql -d carebridge -f 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql

# Option B — via Spring Boot (requires app configured against empty DB)
./mvnw spring-boot:run   # Flyway auto-migrates on startup
```

## After Pulling This Branch (existing local DB)

> ⚠️ **DO NOT use `flyway:repair` as a schema upgrade path.**
>
> `flyway:repair` only updates checksum metadata. It does NOT add the 46 new
> tables, FKs, or indexes in the new V1. Your database would appear healthy to
> Flyway while silently missing 46 tables.

Any existing local database created from the old V1 + V2–V12 migration history
must be **recreated from a clean schema**. See the full runbook:

`05_Development/Database/postgres/LOCAL_DATABASE_REBASELINE_RUNBOOK.md`

## Supabase

Do NOT apply this migration to Supabase directly.
The shared Supabase DB requires a Phase C guarded reset before V1 can be applied.
Follow `docs/plans/claude/SUPABASE_RESET_EXECUTION_RUNBOOK.md`.
