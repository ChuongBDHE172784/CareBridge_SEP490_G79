# CareBridge Database Consolidation: 70 -> 48 Tables

## Executive summary

CareBridge started from a fragmented Release 1 relational schema of **70 tables**. Phase 2 consolidated that design to an approved **53-table baseline**. The latest approved consolidation removes five additional physical tables, resulting in **48 base tables** in Supabase.

The count of 48 includes Flyway's technical `flyway_schema_history` table. Therefore, the deployed database has **47 business tables** and **1 migration-history table**.

| Milestone | Base tables | Change |
|---|---:|---:|
| Release 1 | 70 | Initial schema |
| Phase 2 approved baseline | 53 | -17 tables |
| Current deployed schema | 48 | -5 tables |
| Total reduction | 48 | -22 tables from Release 1 |

## Deployment status

- Supabase migration applied successfully: `V20260726000000__consolidate_requested_schema.sql`.
- Flyway records version `20260726000000` as successful.
- Verified post-deployment count: **48 base tables** in `public`.
- The migration uses transactional Flyway execution and an explicit final count gate. It fails and rolls back if the final base-table count is not exactly 48.

## Phase 1 - Release 1 (70) to Phase 2 baseline (53)

The Phase 2 baseline reduced table proliferation by moving closely related data into canonical aggregate tables.

| Area | Removed or merged tables | Canonical destination |
|---|---|---|
| Identity and expert profile | `user_identities`, `professional_profiles`, `professional_specialties` | `users` |
| Session revocation | `auth_revocations`, `token_blacklist` | `auth_sessions` |
| Domain events | `mother_journey_events`, `moderator_events`, `expert_contribution_events` | `audit_events` |
| Health observations | `maternal_observations` | `health_observations` |
| Health files | `health_record_attachments` | `attachments` |
| Family care work | `scheduled_care_items`, `family_tasks` | `care_tasks` |
| Nearby-support requests/responses | `nearby_support_requests`, `nearby_support_responses` | `nearby_support_interactions` |
| Safety and emergency records | Legacy IMU, emergency-session, alert-delivery, map-handoff, location, and monitoring tables | `safety_events`, `safety_monitoring_sessions`, `safety_configs` |

The approved 53-table snapshot is the source of truth for the next stage: `carebridge_53 (1).sql`.

## Phase 2 - 53 to 48 tables

Five tables were removed from the approved 53-table baseline.

| Removed physical table | Replacement | Data and behavior |
|---|---|---|
| `account_deletion_requests` | `users` | Account removal is now soft deactivation. `users` stores `deactivation_reason`, `deactivated_at`, and `deactivated_by`; the account becomes `INACTIVE` and disabled when a completed deletion request is migrated. |
| `emergency_contacts` | `care_group_members` + `users` | A contact must be a care-group member. The member row stores the emergency designation and priority; name and phone are read from the member's user profile. |
| `expert_credentials` | `attachments` | Credential documents use the single attachment store, categorized with `attachment_category = 'EXPERT_CREDENTIAL'` and credential/review metadata. |
| `care_logs` | `care_tasks` | Daily care logs are stored as `CARE_LOG` tasks with dynamic values in `metadata_jsonb`. |
| `nearby_support_interactions` | Removed | Peer-to-peer nearby support is disabled for safety and privacy. Expert and care-facility discovery remain separate capabilities. |

### Compatibility views

To avoid breaking existing API mappings during the transition, four names are retained as **views**, not physical tables:

- `emergency_contacts`
- `expert_credentials`
- `care_logs`
- `nearby_support_interactions`

The first three views write through to their canonical tables. The nearby-support view contains no rows and rejects write attempts, preventing renewed peer-location interactions.

## Supabase alignment cleanup

The live Supabase instance had drifted to 83 base tables, even though Flyway recorded the Phase 2 chain as successful. Before applying the five consolidations, the migration aligned Supabase to the approved 53-table snapshot by removing these 30 tables that were not in that snapshot:

`archived_consultation_records`, `archived_partner_records`, `archived_realtime_records`, `auth_revocations`, `baby_journey_link_cleanup_summary`, `baby_profiles`, `community_profiles`, `consent_grants`, `consultation_context_citations`, `consultation_context_shares`, `consultation_requests`, `evidence_sources`, `expert_consultation_requests`, `expert_contribution_events`, `expert_profiles`, `family_tasks`, `health_record_attachments`, `intake_sessions`, `maternal_observations`, `moderation_events`, `mother_journey_events`, `nearby_support_requests`, `nearby_support_responses`, `persons`, `pregnancy_outcome_evidence`, `professional_profiles`, `safety_event_actions`, `scheduled_care_items`, `security_events`, `user_identities`.

Foreign keys from `users` and `care_subjects` to the legacy `persons` table were removed before dropping that table. The migration contains an absence gate to ensure all 30 drift tables are gone.

## Current 48-table inventory

The following are the 48 physical tables currently in `public`:

| Domain | Tables |
|---|---|
| Platform and identity | `users`, `auth_sessions`, `auth_challenges`, `data_permissions`, `system_configurations`, `flyway_schema_history` |
| Care and family | `care_subjects`, `care_groups`, `care_group_members`, `care_tasks`, `care_item_templates`, `preparation_checklist_items`, `mother_journeys`, `maternal_exercise_sessions` |
| Baby and vaccination | `development_milestones`, `growth_measurements`, `vaccination_records`, `vaccination_schedules` |
| Health and files | `attachments`, `health_records`, `health_observations`, `health_context_memories`, `device_connections`, `device_tokens` |
| Expert and facilities | `expert_availability`, `expert_location_shares`, `specialties`, `care_facilities`, `administrative_areas` |
| Content and moderation | `community_content`, `community_interactions`, `community_topics`, `content_items`, `content_item_topics`, `content_item_sources`, `moderation_cases` |
| Triage and knowledge | `triage_sessions`, `triage_session_evidence`, `knowledge_sources`, `knowledge_source_reviews` |
| Safety and notifications | `safety_configs`, `safety_monitoring_sessions`, `safety_events`, `notification_records` |
| Financial, audit, archive | `expense_entries`, `audit_events`, `archived_records` |

## Explicitly deferred decisions

The following are intentionally outside this migration:

- `vaccination_schedules` and `vaccination_records` remain separate tables. Their consolidation into `care_tasks` was proposed for consideration, not approved for implementation.
- `audit_events` remains in PostgreSQL for now. Moving audit events to a logging or NoSQL platform requires a separately approved destination and retention design.
- Direct-message and call persistence is not changed here. Moving realtime history to a non-relational store requires separate infrastructure approval.

## Operational safeguards

- Emergency-contact migration validates that every existing contact matches a care-group member by normalized phone number; otherwise the migration fails rather than losing contact data.
- Expert credentials must have a canonical `user_id` before migration.
- Care logs must reference a valid care subject.
- The final migration gate requires exactly 48 base tables before commit.
