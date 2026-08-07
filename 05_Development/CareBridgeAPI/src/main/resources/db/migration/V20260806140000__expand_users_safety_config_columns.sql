-- CareBridge database consolidation — R8 expand + backfill
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.9
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.10
--
-- Safety configuration moves to typed columns on users, NOT into settings_jsonb:
-- these are operational values a hot path reads, and they deserve real types and
-- real constraints. safety_configs keeps existing as the rollback path.

-- ---------------------------------------------------------------------------
-- 1. Expand
-- ---------------------------------------------------------------------------
-- Prefixed names so the safety domain stays legible on a wide table. Defaults
-- match the application defaults (plan §5.4), so a user who never opened the
-- safety screen reads the same values the old service synthesised for them.
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS fall_detection_enabled boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS fall_detection_sensitivity_level varchar(10) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN IF NOT EXISTS emergency_auto_alert boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS emergency_countdown_seconds integer NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS sensor_permission_granted boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS sensor_permission_recorded_at timestamptz NULL,
    ADD COLUMN IF NOT EXISTS safety_config_updated_at timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS safety_config_updated_by uuid NULL;

-- ---------------------------------------------------------------------------
-- 2. Backfill
-- ---------------------------------------------------------------------------
-- safety_configs.user_id is UNIQUE, so this is a straight 1:1 copy. Guarded on
-- the default-shaped row so a rerun cannot overwrite values written by new code.
UPDATE public.users u
   SET fall_detection_enabled           = c.fall_detection_enabled,
       fall_detection_sensitivity_level = c.sensitivity_level,
       emergency_auto_alert             = c.emergency_auto_alert,
       emergency_countdown_seconds      = c.countdown_seconds,
       sensor_permission_granted        = c.sensor_permission_granted,
       sensor_permission_recorded_at    = c.sensor_permission_recorded_at,
       safety_config_updated_at         = c.updated_at,
       safety_config_updated_by         = c.updated_by
  FROM public.safety_configs c
 WHERE c.user_id = u.user_id
   AND u.fall_detection_enabled = false
   AND u.fall_detection_sensitivity_level = 'MEDIUM'
   AND u.emergency_auto_alert = true
   AND u.emergency_countdown_seconds = 30
   AND u.sensor_permission_granted = false
   AND u.sensor_permission_recorded_at IS NULL
   AND u.safety_config_updated_by IS NULL;

-- ---------------------------------------------------------------------------
-- 3. Reconciliation gate
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_orphaned bigint;
    v_mismatched bigint;
BEGIN
    -- A config whose user no longer exists cannot be migrated at all.
    SELECT count(*) INTO v_orphaned
    FROM public.safety_configs c
    WHERE NOT EXISTS (SELECT 1 FROM public.users u WHERE u.user_id = c.user_id);

    IF v_orphaned > 0 THEN
        RAISE EXCEPTION 'R8_ORPHANED_CONFIG: % safety_configs row(s) reference a missing user', v_orphaned;
    END IF;

    SELECT count(*) INTO v_mismatched
    FROM public.safety_configs c
    JOIN public.users u ON u.user_id = c.user_id
    WHERE (u.fall_detection_enabled,
           u.fall_detection_sensitivity_level,
           u.emergency_auto_alert,
           u.emergency_countdown_seconds,
           u.sensor_permission_granted)
       IS DISTINCT FROM
          (c.fall_detection_enabled,
           c.sensitivity_level,
           c.emergency_auto_alert,
           c.countdown_seconds,
           c.sensor_permission_granted);

    IF v_mismatched > 0 THEN
        RAISE EXCEPTION
            'R8_BACKFILL_MISMATCH: % user(s) whose safety columns do not match safety_configs',
            v_mismatched;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 4. Constraints — ported from safety_configs, not reinvented
-- ---------------------------------------------------------------------------
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_safety_countdown_ck;
ALTER TABLE public.users ADD CONSTRAINT users_safety_countdown_ck
    CHECK (emergency_countdown_seconds = ANY (ARRAY[15, 30, 60]));

ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_safety_sensitivity_ck;
ALTER TABLE public.users ADD CONSTRAINT users_safety_sensitivity_ck
    CHECK (fall_detection_sensitivity_level = ANY (ARRAY['LOW', 'MEDIUM', 'HIGH']));

-- Granted permission without a recorded timestamp is unauditable: we would be
-- claiming consent with no record of when it was given.
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_sensor_permission_ck;
ALTER TABLE public.users ADD CONSTRAINT users_sensor_permission_ck
    CHECK (sensor_permission_granted = false OR sensor_permission_recorded_at IS NOT NULL);

ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_safety_updated_by_fk;
ALTER TABLE public.users ADD CONSTRAINT users_safety_updated_by_fk
    FOREIGN KEY (safety_config_updated_by) REFERENCES public.users(user_id);
