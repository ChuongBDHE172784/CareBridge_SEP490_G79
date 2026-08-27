-- Deterministic, synthetic CHK-041 reference fixture.
-- This file is test-only and may be applied only by the loopback disposable operator.

-- CHK041:PRE_EXPAND
DELETE FROM public.preparation_checklist_items;
DELETE FROM public.care_item_templates
WHERE template_id IN (
    '60000000-0000-0000-0000-000000000001'::uuid,
    '60000000-0000-0000-0000-000000000002'::uuid)
  AND entry_type = 'CHECKLIST_ENTRY'
  AND parent_template_id IS NULL;

INSERT INTO public.care_subjects
    (care_subject_id, person_id, owner_user_id, subject_type, nickname,
     birth_date, status, created_at, updated_at)
SELECT
    ('c0412000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    'BABY',
    'CHK-041 synthetic baby ' || n,
    date '2026-01-01' + n,
    'ACTIVE',
    timestamptz '2026-07-01 00:00:00+00',
    timestamptz '2026-07-01 00:00:00+00'
FROM generate_series(1, 20) AS series(n)
ON CONFLICT (care_subject_id) DO NOTHING;

INSERT INTO public.care_subjects
    (care_subject_id, person_id, owner_user_id, subject_type, nickname,
     status, created_at, updated_at)
SELECT
    ('c0412100-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    'MOTHER',
    'CHK-041 synthetic mother context ' || n,
    'ACTIVE',
    timestamptz '2026-07-01 00:00:00+00',
    timestamptz '2026-07-01 00:00:00+00'
FROM generate_series(1, 20) AS series(n)
ON CONFLICT (care_subject_id) DO NOTHING;

INSERT INTO public.mother_journeys
    (journey_id, owner_user_id, journey_type, start_date, last_menstrual_date,
     estimated_due_date, status, care_subject_id, created_at, updated_at)
SELECT
    ('c0411000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    'PREGNANCY',
    date '2025-01-01' + n,
    date '2025-01-01' + n,
    date '2025-10-08' + n,
    'COMPLETED',
    ('c0412100-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    timestamptz '2026-07-01 00:00:00+00',
    timestamptz '2026-07-01 00:00:00+00'
FROM generate_series(1, 20) AS series(n)
ON CONFLICT (journey_id) DO NOTHING;

INSERT INTO public.care_groups
    (care_group_id, owner_user_id, journey_id, baby_id, group_name, status,
     linked_journey_id, linked_baby_profile_id, care_subject_id, created_at, updated_at)
SELECT
    ('c0413000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    ('c0411000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    ('c0412000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    'CHK-041 synthetic group ' || n,
    'ACTIVE',
    ('c0411000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    ('c0412000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    ('c0412100-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    timestamptz '2026-07-01 00:00:00+00',
    timestamptz '2026-07-01 00:00:00+00'
FROM generate_series(1, 20) AS series(n)
ON CONFLICT (care_group_id) DO NOTHING;

WITH source AS (
    SELECT n,
           ((n - 1) / 500) + 1 AS group_no,
           ((n - 1) % 500) + 1 AS group_row
    FROM generate_series(1, 10002) AS series(n)
)
INSERT INTO public.preparation_checklist_items
    (checklist_item_id, owner_user_id, mother_journey_id, baby_id,
     template_entry_id, title, display_order, status, due_at, completed_at,
     category, created_at, updated_at)
SELECT
    ('c0414000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    CASE
        WHEN n = 10001 THEN 'c0411000-0000-4000-8000-000000000001'::uuid
        WHEN n <= 10000 AND group_row <= 250 THEN
            ('c0411000-0000-4000-8000-' || lpad(group_no::text, 12, '0'))::uuid
    END,
    CASE
        WHEN n = 10001 THEN 'c0412000-0000-4000-8000-000000000001'::uuid
        WHEN n <= 10000 AND group_row > 250 THEN
            ('c0412000-0000-4000-8000-' || lpad(group_no::text, 12, '0'))::uuid
    END,
    NULL,
    'CHK-041 synthetic legacy row ' || n,
    group_row,
    CASE n % 5
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'IN_PROGRESS'
        WHEN 2 THEN 'COMPLETED'
        WHEN 3 THEN 'SKIPPED'
        ELSE 'OPEN'
    END,
    CASE WHEN n % 10 = 0 THEN NULL
         ELSE timestamptz '2026-08-15 01:00:00+00' + ((n % 24) * interval '1 hour') END,
    CASE WHEN n % 5 IN (2, 3) THEN timestamptz '2026-07-15 00:00:00+00' END,
    CASE n % 4
        WHEN 0 THEN 'DELIVERY'
        WHEN 1 THEN 'PAPERWORK'
        WHEN 2 THEN 'BABY_CARE'
        ELSE 'GENERAL'
    END,
    timestamptz '2026-07-01 00:00:00+00' + (n * interval '1 second'),
    timestamptz '2026-07-01 00:00:00+00' + (n * interval '1 second')
FROM source;

-- CHK041:POST_EXPAND
UPDATE public.care_groups
SET status = 'ARCHIVED'
WHERE care_group_id::text NOT LIKE 'c0413000-0000-4000-8000-%';

INSERT INTO public.checklist_care_group_contexts
    (context_mapping_id, care_group_id, owner_user_id, care_context_type,
     care_context_id, review_status, distribution_blocked, reviewed_at, reviewed_by)
SELECT
    ('c0418000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    ('c0413000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    'JOURNEY',
    ('c0411000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    'REVIEWED', false,
    timestamptz '2026-07-01 00:00:00+00',
    '10000000-0000-0000-0000-000000000004'::uuid
FROM generate_series(1, 20) AS series(n)
ON CONFLICT DO NOTHING;

INSERT INTO public.checklist_care_group_contexts
    (context_mapping_id, care_group_id, owner_user_id, care_context_type,
     care_context_id, review_status, distribution_blocked, reviewed_at, reviewed_by)
SELECT
    ('c0418001-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    ('c0413000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    'BABY',
    ('c0412000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    'REVIEWED', false,
    timestamptz '2026-07-01 00:00:00+00',
    '10000000-0000-0000-0000-000000000004'::uuid
FROM generate_series(1, 20) AS series(n)
ON CONFLICT DO NOTHING;

INSERT INTO public.care_group_members
    (care_group_member_id, care_group_id, user_id, member_role,
     invitation_status, permission_json, joined_at, created_at, updated_at)
SELECT
    ('c0413200-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    ('c0413000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    '10000000-0000-0000-0000-000000000006'::uuid,
    'CO_CAREGIVER', 'ACCEPTED',
    '{"CHECKLIST_VIEW":true,"CHECKLIST_COMPLETE":true}'::jsonb,
    timestamptz '2026-07-01 00:00:00+00',
    timestamptz '2026-07-01 00:00:00+00',
    timestamptz '2026-07-01 00:00:00+00'
FROM generate_series(1, 20) AS series(n)
ON CONFLICT DO NOTHING;

UPDATE public.care_item_templates SET distribution_enabled = false
WHERE entry_type = 'TEMPLATE_ROOT' AND distribution_enabled = true;

INSERT INTO public.care_item_templates
    (template_id, entry_type, title, display_order, stage, is_active, version,
     template_status, content_status, template_lineage_id, template_version_id,
     substage_id, migration_review_required, distribution_enabled,
     created_at, updated_at)
SELECT
    ('c0415000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    'TEMPLATE_ROOT', 'CHK-041 reconciliation template ' || n, n,
    CASE WHEN n <= 250 THEN 'PREGNANCY' ELSE 'BABY_CARE' END,
    true, 1, 'ACTIVE', 'DRAFT',
    ('c0416000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    ('c0416000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    (SELECT substage_id FROM public.checklist_substages
     WHERE code = CASE WHEN n <= 250 THEN 'PREGNANCY_LMP_WEEK_0_12' ELSE 'BABY_CARE_DAY_0_28' END),
    false, false,
    timestamptz '2026-07-01 00:00:00+00', timestamptz '2026-07-01 00:00:00+00'
FROM generate_series(1, 500) AS series(n);

INSERT INTO public.care_item_templates
    (template_id, parent_template_id, entry_type, title, display_order,
     is_active, version, template_status, content_status, target_subject,
     is_required, created_at, updated_at)
SELECT
    ('c0417000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    ('c0415000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    'CHECKLIST_ENTRY', 'CHK-041 reconciliation item ' || n, 1,
    true, 1, 'ACTIVE', 'DRAFT',
    CASE WHEN n <= 250 THEN 'MOTHER' ELSE 'BABY' END,
    true,
    timestamptz '2026-07-01 00:00:00+00', timestamptz '2026-07-01 00:00:00+00'
FROM generate_series(1, 500) AS series(n);

INSERT INTO public.checklist_template_recipient_roles(template_version_id, recipient_role)
SELECT ('c0416000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid, 'MOTHER'
FROM generate_series(1, 500) AS series(n);

UPDATE public.care_item_templates
SET content_status = 'APPROVED', distribution_enabled = true,
    approved_at = timestamptz '2026-07-01 00:00:00+00',
    approved_by = '10000000-0000-0000-0000-000000000004'::uuid
WHERE template_id::text LIKE 'c0415000-0000-4000-8000-%';

INSERT INTO public.care_tasks
    (task_id, task_type, owner_user_id, care_group_id, creator_user_id,
     assignee_user_id, care_subject_id, journey_id, baby_id,
     title, scheduled_at, status,
     origin, target_subject, created_at, updated_at)
SELECT
    ('c0419000-0000-4000-8000-' || lpad(n::text, 12, '0'))::uuid,
    'MANUAL_TASK',
    '10000000-0000-0000-0000-000000000004'::uuid,
    ('c0413000-0000-4000-8000-' || lpad((((n - 1) % 20) + 1)::text, 12, '0'))::uuid,
    '10000000-0000-0000-0000-000000000004'::uuid,
    '10000000-0000-0000-0000-000000000006'::uuid,
    CASE WHEN n <= 250
        THEN ('c0412100-0000-4000-8000-' || lpad((((n - 1) % 20) + 1)::text, 12, '0'))::uuid
        ELSE ('c0412000-0000-4000-8000-' || lpad((((n - 1) % 20) + 1)::text, 12, '0'))::uuid
    END,
    CASE WHEN n <= 250
        THEN ('c0411000-0000-4000-8000-' || lpad((((n - 1) % 20) + 1)::text, 12, '0'))::uuid
    END,
    CASE WHEN n > 250
        THEN ('c0412000-0000-4000-8000-' || lpad((((n - 1) % 20) + 1)::text, 12, '0'))::uuid
    END,
    'CHK-041 Today task ' || n,
    timestamptz '2026-07-30 01:00:00+00' + ((n % 12) * interval '1 hour'),
    'OPEN', 'USER_CREATED', CASE WHEN n <= 250 THEN 'MOTHER' ELSE 'BABY' END,
    timestamptz '2026-07-01 00:00:00+00', timestamptz '2026-07-01 00:00:00+00'
FROM generate_series(1, 500) AS series(n);

-- CHK041:POST_EXPAND_END
-- CHK041:CHALLENGE
-- Seed one exact parent plus a foreign child owning source row 1's deterministic
-- task key. V70000 quarantines that source; V14000 creates its repaired target,
-- deliberately leaving a dual target/quarantine outcome for the abort proof.
WITH tokens AS (
    SELECT
        '10000000-0000-0000-0000-000000000004'::text AS owner_id,
        'c0413000-0000-4000-8000-000000000001'::text AS group_id,
        'c0411000-0000-4000-8000-000000000001'::text AS context_id,
        'c0414000-0000-4000-8000-000000000001'::text AS source_id
), parent_key AS (
    SELECT *, encode(sha256(convert_to(
        'v1' || length(owner_id)::text || ':' || owner_id ||
        length('MOTHER')::text || ':MOTHER' ||
        length(group_id)::text || ':' || group_id ||
        length('JOURNEY')::text || ':JOURNEY' ||
        length(context_id)::text || ':' || context_id ||
        length('<ABSENT>')::text || ':<ABSENT>' ||
        length('<ABSENT>')::text || ':<ABSENT>', 'UTF8')), 'hex') AS value
    FROM tokens
), parent_identity AS (
    SELECT *, encode(sha256(convert_to(
        'v1' || length('LEGACY_PARENT_ID')::text || ':LEGACY_PARENT_ID' ||
        length(value)::text || ':' || value, 'UTF8')), 'hex') AS identity
    FROM parent_key
), collision AS (
    SELECT *,
        (substr(identity, 1, 8) || '-' || substr(identity, 9, 4) || '-' ||
         substr(identity, 13, 4) || '-' || substr(identity, 17, 4) || '-' ||
         substr(identity, 21, 12))::uuid AS parent_id
    FROM parent_identity
), inserted_parent AS (
    INSERT INTO public.checklist_instances
        (checklist_instance_id, distribution_key, key_version,
         recipient_user_id, recipient_role, care_group_id, care_context_type,
         care_context_id, context_owner_user_id, origin, status, created_at, updated_at)
    SELECT parent_id, value, 'v1', owner_id::uuid, 'MOTHER', group_id::uuid,
           'JOURNEY', context_id::uuid, owner_id::uuid, 'USER_CREATED',
           'IN_PROGRESS', timestamptz '2026-07-01 00:00:01+00',
           timestamptz '2026-07-01 00:08:20+00'
    FROM collision
    RETURNING checklist_instance_id
)
INSERT INTO public.checklist_task_instances
    (checklist_task_instance_id, checklist_instance_id, task_key, key_version,
     title_snapshot, display_order, is_required, target_subject, status,
     created_at, updated_at)
SELECT
    'c041dead-0000-4000-8000-000000000001'::uuid,
    collision.parent_id,
    encode(sha256(convert_to(
        'v1' || length(collision.parent_id::text)::text || ':' || collision.parent_id::text ||
        length('USER_CREATED')::text || ':USER_CREATED' ||
        length(collision.source_id)::text || ':' || collision.source_id, 'UTF8')), 'hex'),
    'v1', 'CHK-041 controlled collision sentinel', 10000, false, 'MOTHER',
    'PENDING', timestamptz '2026-07-01 00:00:00+00',
    timestamptz '2026-07-01 00:00:00+00'
FROM collision
CROSS JOIN inserted_parent;

-- CHK041:END
