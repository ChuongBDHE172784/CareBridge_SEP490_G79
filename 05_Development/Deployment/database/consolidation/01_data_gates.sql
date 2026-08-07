-- CareBridge database consolidation — Wave 0 data gates (R0a / R0b)
-- Source: 08_References/Database_Table_Audit_And_Consolidation V3.md §6 and
--         08_References/Database_Consolidation_Source_Code_Refactor_Plan.md §6
--
-- Read-only. Every gate below has an explicit expected result. A gate that does
-- not meet its expectation blocks the release named in the `blocks` column; it
-- is not a warning and cannot be waived by a written exception (V3 §7).
--
-- Run order: this file first, then reconcile with 02_r0b_reconciliation.sql,
-- then re-run this file and attach both outputs to the change ticket.

\set ON_ERROR_STOP on

\echo '== GATE 1 | blocks R1b | account deletion queue must be empty =='
-- V3 §3.1.1: every PENDING request must be resolved into exactly one outcome
-- (deactivate / cancel / temporarily retain) before the queue code is removed.
SELECT status, count(*) AS rows
FROM public.account_deletion_requests
GROUP BY status
ORDER BY status;
-- expected: no row with status = 'PENDING'

SELECT count(*) AS pending_deletion_requests
FROM public.account_deletion_requests
WHERE status = 'PENDING';
-- expected: 0

\echo '== GATE 2 | blocks R2b | lock appeal queue must be empty =='
-- V3 §3.1.2: no PENDING appeal, and every transferred case has a CSKH ticket
-- id or an audit_events row naming actor/user/lock episode/reason/timestamp.
SELECT status, count(*) AS rows
FROM public.account_lock_appeals
GROUP BY status
ORDER BY status;

SELECT count(*) AS pending_lock_appeals
FROM public.account_lock_appeals
WHERE status = 'PENDING';
-- expected: 0

\echo '== GATE 3 | blocks R3c | no live PARTNER users =='
-- V3 §3.3: roles are remapped by business decision, never defaulted to NULL
-- while a real user exists. Sessions and tokens of remapped users must be
-- revoked before the enum value disappears from the deployed code (plan §4.4).
SELECT count(*) AS partner_users
FROM public.users
WHERE role = 'PARTNER';
-- expected: 0

SELECT u.user_id, u.email, u.enabled, u.account_status
FROM public.users u
WHERE u.role = 'PARTNER'
ORDER BY u.created_at;
-- expected: 0 rows; any row here needs a documented remap target

SELECT count(*) AS partner_organizations
FROM public.partner_organizations;
-- informational: seed rows only, snapshot before drop

\echo '== GATE 4 | blocks R4c | device observation provenance =='
-- V3 §3.2: if this is > 0, provenance must be snapshotted into
-- health_observations.raw_payload_jsonb under the "deviceProvenance" namespace
-- BEFORE the column is dropped. Never copy token_reference / OAuth secrets.
SELECT count(*) AS linked_observations
FROM public.health_observations
WHERE device_connection_id IS NOT NULL;
-- expected before contract: 0, or 100% reconciled provenance backfill

SELECT count(*) AS observations_missing_provenance
FROM public.health_observations
WHERE device_connection_id IS NOT NULL
  AND (raw_payload_jsonb -> 'deviceProvenance') IS NULL;
-- expected after backfill: 0

SELECT count(*) AS provenance_with_leaked_secret
FROM public.health_observations
WHERE raw_payload_jsonb -> 'deviceProvenance' ?| ARRAY[
        'tokenReference', 'token_reference', 'accessToken', 'refreshToken',
        'clientSecret', 'credential'
      ];
-- expected: 0 (plan §4.3 exit gate)

\echo '== GATE 5 | blocks R5b/R6 | care_facilities legacy archive link =='
-- V3 §3.1.3: care_facilities.partner_id FKs to archived_records(archive_id),
-- so archived_records cannot be dropped until this column is gone.
SELECT count(*) AS facilities_with_partner_id
FROM public.care_facilities
WHERE partner_id IS NOT NULL;
-- expected: 0

SELECT count(*) AS archived_records_rows
FROM public.archived_records;
-- informational: snapshot/backup required before drop

\echo '== GATE 6 | blocks R5c | direct chat legacy read columns =='
-- V3 §3.4: the four legacy columns must carry no state the cursor table lacks.
SELECT count(*) AS conversations_with_legacy_read_state
FROM public.direct_conversations
WHERE mother_last_read_at IS NOT NULL
   OR mother_last_read_message_id IS NOT NULL
   OR expert_last_read_at IS NOT NULL
   OR expert_last_read_message_id IS NOT NULL;
-- informational: rows here must already be represented in the cursor table

SELECT count(*) AS legacy_ahead_of_cursor
FROM public.direct_conversations c
WHERE (
        c.mother_last_read_at IS NOT NULL
        AND NOT EXISTS (
          SELECT 1 FROM public.direct_conversation_read_cursors r
          WHERE r.conversation_id = c.conversation_id
            AND r.reader_user_id = c.mother_user_id
            AND r.last_read_at >= c.mother_last_read_at
        )
      )
   OR (
        c.expert_last_read_at IS NOT NULL
        AND NOT EXISTS (
          SELECT 1 FROM public.direct_conversation_read_cursors r
          WHERE r.conversation_id = c.conversation_id
            AND r.reader_user_id = c.expert_user_id
            AND r.last_read_at >= c.expert_last_read_at
        )
      );
-- expected: 0 — otherwise the cursor table would lose read state on drop

\echo '== GATE 7 | blocks R4b | nearby support view is dead =='
SELECT count(*) AS nearby_support_rows
FROM public.nearby_support_interactions;
-- expected: 0 (the view is defined WHERE false)

\echo '== GATE 8 | negative-impact check | retained objects must survive =='
-- Plan §4.14: these must still exist after every consolidation migration.
SELECT c.relname, c.relkind
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relname IN (
        'device_tokens', 'reminder_occurrence_aliases',
        'direct_conversation_read_cursors', 'audit_events',
        'auth_sessions', 'auth_challenges', 'care_groups',
        'care_group_members', 'vaccination_records', 'vaccination_schedules',
        'development_milestones', 'data_permissions', 'moderation_cases',
        'triage_sessions', 'triage_session_evidence',
        'knowledge_source_reviews', 'content_item_topics',
        'content_item_sources', 'professional_specialties'
      )
ORDER BY c.relname;
-- expected: 19 rows

SELECT count(*) AS users_settings_jsonb_column
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'users'
  AND column_name = 'settings_jsonb';
-- expected: 1 — V3 §3.5 keeps this column in the current program
