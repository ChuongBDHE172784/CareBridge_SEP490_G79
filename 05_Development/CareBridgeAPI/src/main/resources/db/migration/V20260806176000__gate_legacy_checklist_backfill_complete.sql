-- CareBridge database consolidation — R9 completion gate
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.10
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.11
--
-- The legacy checklist backfill is NOT performed here: it already ran as part of
-- V20260731070000 (which inlines V20260729070000__backfill_legacy_checklist_v2),
-- preserving each legacy checklist_item_id as the v2 checklist_task_instance_id.
-- V20260806175000 then retired the two demo rows it had left behind.
--
-- This gate is what makes removing the legacy read merge in
-- UserChecklistItemController#listItems safe: until every legacy row provably has
-- a v2 counterpart, that merge is the only thing keeping the orphans visible, and
-- dropping it would hide them rather than migrate them.
--
-- V3 §3.10 is explicit that an unresolved template must fail the migration and
-- must never be silently downgraded to USER_CREATED.

DO $$
DECLARE
    v_unmapped bigint;
    v_quarantined bigint;
    v_duplicate_keys bigint;
    v_sample text;
BEGIN
    -- 1. Every legacy row must have become a v2 task. The backfill kept the id,
    --    so this join is the mapping itself, not an approximation of it.
    SELECT count(*) INTO v_unmapped
    FROM public.preparation_checklist_items legacy
    WHERE NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances task
         WHERE task.checklist_task_instance_id = legacy.checklist_item_id);

    IF v_unmapped > 0 THEN
        SELECT string_agg(legacy.checklist_item_id::text, ', ')
          INTO v_sample
          FROM (
            SELECT checklist_item_id
              FROM public.preparation_checklist_items legacy
             WHERE NOT EXISTS (
                SELECT 1 FROM public.checklist_task_instances task
                 WHERE task.checklist_task_instance_id = legacy.checklist_item_id)
             ORDER BY checklist_item_id
             LIMIT 10) legacy;

        RAISE EXCEPTION
            'R9_UNMAPPED_LEGACY_ITEMS: % legacy checklist row(s) have no v2 task instance (first ids: %)',
            v_unmapped, v_sample;
    END IF;

    -- 2. Anything the backfill could not resolve went to quarantine. That table is
    --    scaffolding from the canonical migration and is cleaned up afterwards, so
    --    it is absent on some environments; skip the check rather than assume.
    IF to_regclass('public.checklist_migration_quarantine') IS NOT NULL THEN
        EXECUTE $q$
            SELECT count(*) FROM public.checklist_migration_quarantine
             WHERE source_table = 'preparation_checklist_items' AND resolved_at IS NULL
        $q$ INTO v_quarantined;

        IF v_quarantined > 0 THEN
            EXECUTE $q$
                SELECT string_agg(DISTINCT reason_code, ', ')
                  FROM public.checklist_migration_quarantine
                 WHERE source_table = 'preparation_checklist_items' AND resolved_at IS NULL
            $q$ INTO v_sample;

            RAISE EXCEPTION
                'R9_UNRESOLVED_QUARANTINE: % unresolved legacy checklist row(s) remain (reasons: %); reconcile them, do not downgrade to USER_CREATED',
                v_quarantined, v_sample;
        END IF;
    END IF;

    -- 3. task_key is the deterministic identity from the canonical algorithm.
    --    A collision would mean two logical tasks sharing one identity.
    SELECT count(*) INTO v_duplicate_keys
    FROM (
        SELECT checklist_instance_id, task_key
          FROM public.checklist_task_instances
         GROUP BY checklist_instance_id, task_key
        HAVING count(*) > 1
    ) collided;

    IF v_duplicate_keys > 0 THEN
        RAISE EXCEPTION
            'R9_TASK_KEY_COLLISION: % (instance, task_key) pair(s) are duplicated', v_duplicate_keys;
    END IF;

    RAISE NOTICE 'R9_BACKFILL_COMPLETE: all % legacy checklist row(s) map to a v2 task instance',
        (SELECT count(*) FROM public.preparation_checklist_items);
END
$$;
