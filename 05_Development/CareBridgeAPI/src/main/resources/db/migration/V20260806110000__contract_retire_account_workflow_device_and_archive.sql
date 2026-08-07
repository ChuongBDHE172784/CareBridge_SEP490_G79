-- CareBridge database consolidation — Wave 6 contract (account / device / archive)
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.1, §3.2, §5
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.1, §4.2, §4.3
--
-- Preconditions (05_Development/Deployment/database/consolidation/01_data_gates.sql):
--   GATE 1  account_deletion_requests with status PENDING   = 0
--   GATE 2  account_lock_appeals with status PENDING        = 0
--   GATE 4  health_observations.device_connection_id set    = 0, or provenance
--           backfilled with no leaked credential
--   GATE 5  care_facilities.partner_id set                  = 0
-- and a tested restore point exists: after this commits, rollback is forward-fix
-- or PITR only (V3 §7.1).
--
-- Runs after V20260806100000. Ordering inside the file matters: inbound foreign
-- keys and columns go before the tables they point at, so nothing needs CASCADE.

-- ---------------------------------------------------------------------------
-- 1. Account deletion queue → direct deactivation (V3 §3.1.1)
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_pending bigint;
BEGIN
    SELECT count(*) INTO v_pending
    FROM public.account_deletion_requests WHERE status = 'PENDING';

    IF v_pending > 0 THEN
        RAISE EXCEPTION
            'CONSOLIDATION_GATE_FAILED: % deletion request(s) still PENDING; reconcile via R0b first',
            v_pending;
    END IF;
END
$$;

DROP TABLE IF EXISTS public.account_deletion_requests;

-- Deactivation is now the whole record of an account closure, so the row must be
-- internally consistent: a DEACTIVATED account is disabled and carries a timestamp.
-- Existing rows are repaired first, otherwise the constraint cannot be added.
UPDATE public.users
   SET enabled = false,
       deactivated_at = COALESCE(deactivated_at, now())
 WHERE account_status = 'DEACTIVATED'
   AND (enabled = true OR deactivated_at IS NULL);

ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_deactivation_shape_ck;
ALTER TABLE public.users ADD CONSTRAINT users_deactivation_shape_ck CHECK (
    account_status IS DISTINCT FROM 'DEACTIVATED'
    OR (enabled = false AND deactivated_at IS NOT NULL)
);

ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_deactivated_by_fk;
ALTER TABLE public.users ADD CONSTRAINT users_deactivated_by_fk
    FOREIGN KEY (deactivated_by) REFERENCES public.users(user_id);

-- ---------------------------------------------------------------------------
-- 2. Account lock appeals → customer support (V3 §3.1.2)
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_pending bigint;
BEGIN
    SELECT count(*) INTO v_pending
    FROM public.account_lock_appeals WHERE status = 'PENDING';

    IF v_pending > 0 THEN
        RAISE EXCEPTION
            'CONSOLIDATION_GATE_FAILED: % lock appeal(s) still PENDING; reconcile via R0b first',
            v_pending;
    END IF;
END
$$;

ALTER TABLE public.account_lock_appeals
    DROP CONSTRAINT IF EXISTS account_lock_appeals_user_fk,
    DROP CONSTRAINT IF EXISTS account_lock_appeals_reviewer_fk;

DROP TABLE IF EXISTS public.account_lock_appeals;

-- ---------------------------------------------------------------------------
-- 3. Health-device integration (V3 §3.2)
-- ---------------------------------------------------------------------------
-- device_tokens is push-notification state and is deliberately untouched here.
DO $$
DECLARE
    v_linked bigint;
    v_leaked bigint;
BEGIN
    SELECT count(*) INTO v_linked
    FROM public.health_observations
    WHERE device_connection_id IS NOT NULL
      AND (raw_payload_jsonb -> 'deviceProvenance') IS NULL;

    IF v_linked > 0 THEN
        RAISE EXCEPTION
            'CONSOLIDATION_GATE_FAILED: % observation(s) reference a device connection with no provenance snapshot',
            v_linked;
    END IF;

    SELECT count(*) INTO v_leaked
    FROM public.health_observations
    WHERE raw_payload_jsonb -> 'deviceProvenance' ?| ARRAY[
            'tokenReference', 'token_reference', 'accessToken', 'refreshToken',
            'clientSecret', 'credential'];

    IF v_leaked > 0 THEN
        RAISE EXCEPTION
            'CONSOLIDATION_GATE_FAILED: % observation(s) carry a credential inside deviceProvenance',
            v_leaked;
    END IF;
END
$$;

ALTER TABLE public.health_observations
    DROP CONSTRAINT IF EXISTS health_observations_device_connection_id_fkey,
    DROP CONSTRAINT IF EXISTS health_observations_device_fk;

DROP INDEX IF EXISTS public.health_observations_device_time_ix;

ALTER TABLE public.health_observations DROP COLUMN IF EXISTS device_connection_id;

DELETE FROM public.device_connections;

DROP TABLE IF EXISTS public.device_connections;

-- ---------------------------------------------------------------------------
-- 4. care_facilities.partner_id and archived_records (V3 §3.1.3)
-- ---------------------------------------------------------------------------
-- The column is the only inbound FK to archived_records, so it must go first.
DO $$
DECLARE
    v_linked bigint;
BEGIN
    SELECT count(*) INTO v_linked
    FROM public.care_facilities WHERE partner_id IS NOT NULL;

    IF v_linked > 0 THEN
        RAISE EXCEPTION
            'CONSOLIDATION_GATE_FAILED: % care facility row(s) still carry partner_id',
            v_linked;
    END IF;
END
$$;

ALTER TABLE public.care_facilities
    DROP CONSTRAINT IF EXISTS care_facilities_partner_archive_fk;

DROP INDEX IF EXISTS public.idx_care_facilities_partner_id;

ALTER TABLE public.care_facilities DROP COLUMN IF EXISTS partner_id;

ALTER TABLE public.archived_records
    DROP CONSTRAINT IF EXISTS archived_records_owner_user_id_fkey,
    DROP CONSTRAINT IF EXISTS archived_records_source_uk;

DROP TABLE IF EXISTS public.archived_records;

-- ---------------------------------------------------------------------------
-- 5. Negative-impact check (plan §4.14)
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_missing text;
BEGIN
    SELECT string_agg(expected, ', ' ORDER BY expected) INTO v_missing
    FROM unnest(ARRAY[
        'device_tokens', 'care_facilities', 'health_observations',
        'growth_measurements', 'safety_events', 'reminder_occurrence_aliases',
        'direct_conversation_read_cursors', 'auth_sessions', 'audit_events'
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
