-- CareBridge database consolidation — R10 expand + backfill
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.11
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.12
--
-- Product confirmed that from the cutover, a booking has at most one logical
-- consultation session and the paid flow is retired. Putting the session fields
-- directly on the booking row makes that a structural invariant rather than a
-- convention. consultation_sessions stays as the rollback path.
--
-- Both gates below are zero-row conditions, not judgement calls: V3 §3.11 is
-- explicit that a written exception does not substitute for them.

-- ---------------------------------------------------------------------------
-- 1. Gate — cardinality
-- ---------------------------------------------------------------------------
-- A session with no booking cannot be placed on a booking row at all, and a
-- booking with two sessions cannot be represented once the fields are inline.
DO $$
DECLARE
    v_orphaned bigint;
    v_multi bigint;
BEGIN
    SELECT count(*) INTO v_orphaned
    FROM public.consultation_sessions WHERE booking_id IS NULL;

    SELECT count(*) INTO v_multi
    FROM (
        SELECT booking_id
          FROM public.consultation_sessions
         WHERE booking_id IS NOT NULL
         GROUP BY booking_id
        HAVING count(*) > 1
    ) duplicated;

    IF v_orphaned > 0 OR v_multi > 0 THEN
        RAISE EXCEPTION
            'R10_CARDINALITY_GATE_FAILED: % session(s) without a booking, % booking(s) with more than one session',
            v_orphaned, v_multi;
    END IF;

    -- A session pointing at a booking that no longer exists cannot be migrated.
    SELECT count(*) INTO v_orphaned
    FROM public.consultation_sessions s
    WHERE s.booking_id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM public.consultation_bookings b
                       WHERE b.booking_id = s.booking_id);

    IF v_orphaned > 0 THEN
        RAISE EXCEPTION
            'R10_DANGLING_BOOKING: % session(s) reference a missing booking', v_orphaned;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 2. Gate — paid / legacy flow retired
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_paid bigint;
BEGIN
    SELECT count(*) INTO v_paid
    FROM public.consultation_bookings
    WHERE expert_price_id IS NOT NULL
       OR price_band_id IS NOT NULL
       OR price_snapshot_amount IS NOT NULL
       OR commission_rate_snapshot IS NOT NULL
       OR price_locked_at IS NOT NULL;

    IF v_paid > 0 THEN
        RAISE EXCEPTION
            'R10_PAID_FLOW_GATE_FAILED: % booking(s) still carry paid/legacy pricing fields',
            v_paid;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 3. Expand
-- ---------------------------------------------------------------------------
ALTER TABLE public.consultation_bookings
    ADD COLUMN IF NOT EXISTS communication_room_id varchar(255),
    ADD COLUMN IF NOT EXISTS session_started_at timestamptz,
    ADD COLUMN IF NOT EXISTS session_ended_at timestamptz,
    ADD COLUMN IF NOT EXISTS session_status varchar(30),
    ADD COLUMN IF NOT EXISTS expert_summary text,
    ADD COLUMN IF NOT EXISTS technical_log_json jsonb,
    ADD COLUMN IF NOT EXISTS session_created_at timestamptz,
    -- Kept for reconciliation and external/audit traceability. Dropped only in a
    -- later wave, once it is confirmed nothing outside the database refers to it.
    ADD COLUMN IF NOT EXISTS legacy_session_id uuid;

-- ---------------------------------------------------------------------------
-- 4. Backfill
-- ---------------------------------------------------------------------------
-- 1:1 by booking_id, guaranteed by the cardinality gate above. Idempotent: only
-- rows not yet linked are filled.
UPDATE public.consultation_bookings b
   SET communication_room_id = s.communication_room_id,
       session_started_at    = s.started_at,
       session_ended_at      = s.ended_at,
       session_status        = s.session_status,
       expert_summary        = s.expert_summary,
       technical_log_json    = s.technical_log_json,
       session_created_at    = s.created_at,
       legacy_session_id     = s.session_id
  FROM public.consultation_sessions s
 WHERE s.booking_id = b.booking_id
   AND b.legacy_session_id IS NULL;

-- ---------------------------------------------------------------------------
-- 5. Reconciliation gate
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_source bigint;
    v_migrated bigint;
    v_mismatched bigint;
BEGIN
    SELECT count(*) INTO v_source FROM public.consultation_sessions;
    SELECT count(*) INTO v_migrated
      FROM public.consultation_bookings WHERE legacy_session_id IS NOT NULL;

    IF v_source <> v_migrated THEN
        RAISE EXCEPTION
            'R10_BACKFILL_COUNT_MISMATCH: % source session(s) but % migrated booking(s)',
            v_source, v_migrated;
    END IF;

    SELECT count(*) INTO v_mismatched
    FROM public.consultation_sessions s
    JOIN public.consultation_bookings b ON b.booking_id = s.booking_id
    WHERE (b.communication_room_id, b.session_started_at, b.session_ended_at,
           b.session_status, b.expert_summary, b.session_created_at, b.legacy_session_id)
       IS DISTINCT FROM
          (s.communication_room_id, s.started_at, s.ended_at,
           s.session_status, s.expert_summary, s.created_at, s.session_id);

    IF v_mismatched > 0 THEN
        RAISE EXCEPTION
            'R10_BACKFILL_MISMATCH: % booking(s) whose session fields do not match consultation_sessions',
            v_mismatched;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 6. Shape constraints
-- ---------------------------------------------------------------------------
-- Repair first: an already-completed row with no timestamps would block the
-- constraint, and inventing a start time would be worse than recording the end.
UPDATE public.consultation_bookings
   SET session_started_at = COALESCE(session_started_at, session_ended_at)
 WHERE session_ended_at IS NOT NULL
   AND session_started_at IS NULL;

-- A session that ended before it started is a data error, not a short session.
ALTER TABLE public.consultation_bookings
    DROP CONSTRAINT IF EXISTS consultation_bookings_session_order_ck;
ALTER TABLE public.consultation_bookings
    ADD CONSTRAINT consultation_bookings_session_order_ck
    CHECK (session_started_at IS NULL
           OR session_ended_at IS NULL
           OR session_ended_at >= session_started_at);

-- An ended session must have started; a completed one must have both timestamps.
ALTER TABLE public.consultation_bookings
    DROP CONSTRAINT IF EXISTS consultation_bookings_session_shape_ck;
ALTER TABLE public.consultation_bookings
    ADD CONSTRAINT consultation_bookings_session_shape_ck
    CHECK (
        (session_ended_at IS NULL OR session_started_at IS NOT NULL)
        AND (
            session_status IS NULL
            OR upper(session_status) <> 'COMPLETED'
            OR (session_started_at IS NOT NULL AND session_ended_at IS NOT NULL)
        )
    );

-- One booking, one logical session — enforced, not assumed.
CREATE UNIQUE INDEX IF NOT EXISTS consultation_bookings_legacy_session_uk
    ON public.consultation_bookings (legacy_session_id)
    WHERE legacy_session_id IS NOT NULL;
