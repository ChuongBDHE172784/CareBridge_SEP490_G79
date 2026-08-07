-- CareBridge database consolidation — Wave 5 contract (low risk)
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.3, §3.4, §5
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.4, §4.5, §4.6
--
-- Preconditions (05_Development/Deployment/database/consolidation/01_data_gates.sql):
--   GATE 3  users with role 'PARTNER'                = 0
--   GATE 6  legacy_ahead_of_cursor                   = 0
--   GATE 7  nearby_support_interactions rows         = 0
-- and the deployed application no longer maps or queries any object dropped here
-- (Partner package, nearbycare package, the four DirectConversation read fields).
--
-- No DROP ... CASCADE anywhere: every dependent object is named explicitly so an
-- unexpected dependency aborts the migration instead of being silently removed.

-- ---------------------------------------------------------------------------
-- 1. Partner programme (V3 §3.3)
-- ---------------------------------------------------------------------------

-- Fail loudly rather than orphan a real account. Role remap is R3b's job and must
-- already have happened; this is the backstop, not the remediation.
DO $$
DECLARE
    v_partner_users bigint;
BEGIN
    SELECT count(*) INTO v_partner_users FROM public.users WHERE role = 'PARTNER';
    IF v_partner_users > 0 THEN
        RAISE EXCEPTION
            'CONSOLIDATION_GATE_FAILED: % user(s) still hold role PARTNER; run R3b remap first',
            v_partner_users;
    END IF;
END
$$;

DELETE FROM public.partner_organizations;

DROP TABLE IF EXISTS public.partner_organizations;

-- Recreate the role constraint without PARTNER so the database, not just the Java
-- enum, refuses the retired value.
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE public.users ADD CONSTRAINT users_role_check CHECK (
    role IS NULL
    OR role::text = ANY (ARRAY[
        'MOTHER'::text, 'FAMILY'::text, 'EXPERT'::text, 'MODERATOR'::text,
        'CONTENT_ADMIN'::text, 'SYSTEM_ADMIN'::text, 'OPERATIONS'::text
    ])
);

-- ---------------------------------------------------------------------------
-- 2. Nearby peer support (V3 §3.4)
-- ---------------------------------------------------------------------------
-- The view has always been defined WHERE false and its trigger rejected every
-- write, so there is no data to preserve. Order matters: trigger, then view, then
-- the function the trigger referenced.

DROP TRIGGER IF EXISTS nearby_support_interactions_disabled_trg
    ON public.nearby_support_interactions;

DROP VIEW IF EXISTS public.nearby_support_interactions;

DROP FUNCTION IF EXISTS public.carebridge_reject_nearby_support_interaction();

-- ---------------------------------------------------------------------------
-- 3. Direct-chat legacy read columns (V3 §3.4)
-- ---------------------------------------------------------------------------
-- direct_conversation_read_cursors is the single read-state contract. Refuse to
-- drop while any conversation still holds read state the cursor table does not
-- already cover at least as recently — dropping then would silently mark messages
-- unread for that participant.
DO $$
DECLARE
    v_ahead bigint;
BEGIN
    SELECT count(*) INTO v_ahead
    FROM public.direct_conversations c
    WHERE (
            c.mother_last_read_at IS NOT NULL
            AND NOT EXISTS (
              SELECT 1 FROM public.direct_conversation_read_cursors r
              WHERE r.conversation_id = c.conversation_id
                AND r.reader_user_id = c.mother_user_id
                AND r.last_read_at >= c.mother_last_read_at)
          )
       OR (
            c.expert_last_read_at IS NOT NULL
            AND NOT EXISTS (
              SELECT 1 FROM public.direct_conversation_read_cursors r
              WHERE r.conversation_id = c.conversation_id
                AND r.reader_user_id = c.expert_user_id
                AND r.last_read_at >= c.expert_last_read_at)
          );

    IF v_ahead > 0 THEN
        RAISE EXCEPTION
            'CONSOLIDATION_GATE_FAILED: % conversation(s) hold legacy read state ahead of the cursor table',
            v_ahead;
    END IF;
END
$$;

ALTER TABLE public.direct_conversations
    DROP COLUMN IF EXISTS mother_last_read_at,
    DROP COLUMN IF EXISTS mother_last_read_message_id,
    DROP COLUMN IF EXISTS expert_last_read_at,
    DROP COLUMN IF EXISTS expert_last_read_message_id;

-- ---------------------------------------------------------------------------
-- 4. Negative-impact check (plan §4.14)
-- ---------------------------------------------------------------------------
-- Objects this programme must never touch. If one is missing, something in this
-- wave removed more than it was allowed to.
DO $$
DECLARE
    v_missing text;
BEGIN
    SELECT string_agg(expected, ', ' ORDER BY expected) INTO v_missing
    FROM unnest(ARRAY[
        'direct_conversation_read_cursors', 'device_tokens',
        'reminder_occurrence_aliases', 'audit_events', 'care_facilities'
    ]) AS expected
    WHERE to_regclass('public.' || expected) IS NULL;

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'CONSOLIDATION_REGRESSION: retained object(s) missing: %', v_missing;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users'
          AND column_name = 'settings_jsonb') THEN
        RAISE EXCEPTION 'CONSOLIDATION_REGRESSION: users.settings_jsonb was removed';
    END IF;
END
$$;
