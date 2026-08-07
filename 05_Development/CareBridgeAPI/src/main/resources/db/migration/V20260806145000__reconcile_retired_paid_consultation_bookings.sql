-- CareBridge database consolidation — R10 reconciliation (runs before the expand)
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.11
--
-- V3 requires the paid/legacy consultation flow to be converted booking by
-- booking, and explicitly forbids waving the zero-row gate through with a written
-- exception. This migration is that conversion, kept separate from the expand so
-- the gate in V20260806150000 stays an independent check rather than validating
-- work it performed itself.
--
-- Product decision recorded in V3 §3.11: consultation booking is free-only from
-- the cutover. The pricing snapshot columns therefore describe a flow that no
-- longer exists and cannot be honoured, so they are cleared. The values are
-- printed first so they survive in the migration log and the change ticket — the
-- schema has nowhere meaningful left to keep them.

DO $$
DECLARE
    v_row record;
    v_count bigint := 0;
BEGIN
    FOR v_row IN
        SELECT booking_id, status, expert_price_id, price_band_id,
               price_snapshot_amount, commission_rate_snapshot, price_locked_at
          FROM public.consultation_bookings
         WHERE expert_price_id IS NOT NULL
            OR price_band_id IS NOT NULL
            OR price_snapshot_amount IS NOT NULL
            OR commission_rate_snapshot IS NOT NULL
            OR price_locked_at IS NOT NULL
         ORDER BY booking_id
    LOOP
        v_count := v_count + 1;
        RAISE NOTICE
            'R10_PAID_BOOKING_CONVERTED booking_id=% status=% expert_price_id=% price_band_id=% amount=% commission=% locked_at=%',
            v_row.booking_id, v_row.status, v_row.expert_price_id, v_row.price_band_id,
            v_row.price_snapshot_amount, v_row.commission_rate_snapshot, v_row.price_locked_at;
    END LOOP;

    RAISE NOTICE 'R10_PAID_BOOKING_CONVERSION_TOTAL=%', v_count;
END
$$;

UPDATE public.consultation_bookings
   SET expert_price_id           = NULL,
       price_band_id             = NULL,
       price_snapshot_amount     = NULL,
       commission_rate_snapshot  = NULL,
       price_locked_at           = NULL,
       updated_at                = now()
 WHERE expert_price_id IS NOT NULL
    OR price_band_id IS NOT NULL
    OR price_snapshot_amount IS NOT NULL
    OR commission_rate_snapshot IS NOT NULL
    OR price_locked_at IS NOT NULL;

-- Post-condition: the gate in the expand migration must now find nothing to do.
DO $$
DECLARE
    v_remaining bigint;
BEGIN
    SELECT count(*) INTO v_remaining
    FROM public.consultation_bookings
    WHERE expert_price_id IS NOT NULL
       OR price_band_id IS NOT NULL
       OR price_snapshot_amount IS NOT NULL
       OR commission_rate_snapshot IS NOT NULL
       OR price_locked_at IS NOT NULL;

    IF v_remaining > 0 THEN
        RAISE EXCEPTION
            'R10_RECONCILE_INCOMPLETE: % booking(s) still carry paid/legacy pricing fields', v_remaining;
    END IF;
END
$$;
