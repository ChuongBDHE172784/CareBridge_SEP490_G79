-- CareBridge database consolidation — Wave 0 preflight (dependency catalog)
-- Source: 08_References/Database_Table_Audit_And_Consolidation V3.md §6
--
-- Read-only. Run against the live Supabase catalog immediately before every
-- contract migration and paste the output into the change ticket. FK is not the
-- only dependency kind a PostgreSQL object can have, so all five sections must
-- be captured, not just the first.
--
-- Scope of this file: the "feature retirement" objects (R0a..R5c / V3 Wave 5-6).
-- Persistence-consolidation objects (reminder times, appointment rules, safety
-- config, checklist legacy, consultation sessions, job tables) get their own
-- preflight file when those waves start.

\set ON_ERROR_STOP on

-- ---------------------------------------------------------------------------
-- 0. Target object list. Any object missing here is already dropped; regclass
--    casts fail loudly rather than silently skipping, which is intended.
-- ---------------------------------------------------------------------------
-- public.account_deletion_requests   (table, drop  — V3 §3.1.1)
-- public.account_lock_appeals        (table, drop  — V3 §3.1.2)
-- public.archived_records            (table, drop  — V3 §3.1.3)
-- public.device_connections          (table, drop  — V3 §3.2)
-- public.partner_organizations       (table, drop  — V3 §3.3)
-- public.nearby_support_interactions (view,  drop  — V3 §3.4)
-- public.care_facilities.partner_id  (column, drop — V3 §3.1.3)
-- public.direct_conversations.*_last_read_* (4 columns, drop — V3 §3.4)
-- public.health_observations.device_connection_id (column, drop — V3 §3.2)

-- ---------------------------------------------------------------------------
-- 1. Constraint dependencies in both directions
-- ---------------------------------------------------------------------------
SELECT conname,
       conrelid::regclass  AS on_relation,
       confrelid::regclass AS references_relation,
       pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = ANY (ARRAY[
        'public.account_deletion_requests'::regclass,
        'public.account_lock_appeals'::regclass,
        'public.archived_records'::regclass,
        'public.device_connections'::regclass,
        'public.partner_organizations'::regclass
      ])
   OR confrelid = ANY (ARRAY[
        'public.account_deletion_requests'::regclass,
        'public.account_lock_appeals'::regclass,
        'public.archived_records'::regclass,
        'public.device_connections'::regclass,
        'public.partner_organizations'::regclass
      ])
ORDER BY on_relation::text, conname;

-- ---------------------------------------------------------------------------
-- 2. General pg_depend graph (catches non-FK dependencies: defaults, rules,
--    sequences, functions, RLS expressions)
-- ---------------------------------------------------------------------------
SELECT pg_describe_object(d.classid, d.objid, d.objsubid)          AS dependent,
       pg_describe_object(d.refclassid, d.refobjid, d.refobjsubid) AS referenced,
       d.deptype
FROM pg_depend d
WHERE d.refobjid = ANY (ARRAY[
        'public.account_deletion_requests'::regclass::oid,
        'public.account_lock_appeals'::regclass::oid,
        'public.archived_records'::regclass::oid,
        'public.device_connections'::regclass::oid,
        'public.partner_organizations'::regclass::oid,
        'public.nearby_support_interactions'::regclass::oid
      ])
  AND d.deptype <> 'i'
ORDER BY referenced, dependent;

-- ---------------------------------------------------------------------------
-- 3. Views / materialized views whose rewrite rule reads a target object
-- ---------------------------------------------------------------------------
SELECT dependent_view.oid::regclass AS dependent_view,
       pg_get_viewdef(dependent_view.oid, true) AS definition
FROM pg_rewrite rewrite
JOIN pg_class dependent_view ON dependent_view.oid = rewrite.ev_class
WHERE dependent_view.relkind IN ('v', 'm')
  AND EXISTS (
    SELECT 1
    FROM pg_depend d
    WHERE d.classid = 'pg_rewrite'::regclass
      AND d.objid = rewrite.oid
      AND d.refobjid = ANY (ARRAY[
            'public.account_deletion_requests'::regclass::oid,
            'public.account_lock_appeals'::regclass::oid,
            'public.archived_records'::regclass::oid,
            'public.device_connections'::regclass::oid,
            'public.partner_organizations'::regclass::oid,
            'public.care_facilities'::regclass::oid,
            'public.direct_conversations'::regclass::oid,
            'public.health_observations'::regclass::oid
          ])
  );

-- ---------------------------------------------------------------------------
-- 4. Non-internal triggers on target objects
-- ---------------------------------------------------------------------------
SELECT tgrelid::regclass AS relation,
       tgname,
       pg_get_triggerdef(oid) AS definition
FROM pg_trigger
WHERE NOT tgisinternal
  AND tgrelid = ANY (ARRAY[
        'public.account_deletion_requests'::regclass::oid,
        'public.account_lock_appeals'::regclass::oid,
        'public.archived_records'::regclass::oid,
        'public.device_connections'::regclass::oid,
        'public.partner_organizations'::regclass::oid,
        'public.nearby_support_interactions'::regclass::oid
      ])
ORDER BY relation::text, tgname;

-- ---------------------------------------------------------------------------
-- 5. Indexes on target objects and on the columns being dropped
-- ---------------------------------------------------------------------------
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND (
    tablename IN ('account_deletion_requests', 'account_lock_appeals',
                  'archived_records', 'device_connections',
                  'partner_organizations')
    OR indexdef ~ '\m(partner_id|device_connection_id|mother_last_read_at|expert_last_read_at|mother_last_read_message_id|expert_last_read_message_id)\M'
  )
ORDER BY tablename, indexname;

-- ---------------------------------------------------------------------------
-- 6. RLS policies
-- ---------------------------------------------------------------------------
SELECT schemaname, tablename, policyname, roles, cmd, qual, with_check
FROM pg_policies
WHERE schemaname = 'public'
  AND tablename IN ('account_deletion_requests', 'account_lock_appeals',
                    'archived_records', 'device_connections',
                    'partner_organizations', 'care_facilities',
                    'direct_conversations', 'health_observations')
ORDER BY tablename, policyname;

-- ---------------------------------------------------------------------------
-- 7. Grants
-- ---------------------------------------------------------------------------
SELECT table_schema, table_name, grantee, privilege_type
FROM information_schema.table_privileges
WHERE table_schema = 'public'
  AND table_name IN ('account_deletion_requests', 'account_lock_appeals',
                     'archived_records', 'device_connections',
                     'partner_organizations', 'nearby_support_interactions')
ORDER BY table_name, grantee, privilege_type;
