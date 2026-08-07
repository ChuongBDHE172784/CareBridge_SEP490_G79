-- CareBridge database consolidation — R9 reconciliation (runs before the gate)
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.10
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.11
--
-- The legacy checklist backfill (inlined in V20260731070000) migrated every row it
-- could resolve and left two behind. Measured on the linked project on 2026-08-06,
-- preparation_checklist_items holds exactly those two rows and nothing else:
--
--   61000000-0000-0000-0000-000000000001  'Uống vi chất dinh dưỡng hàng ngày'  IN_PROGRESS
--   61000000-0000-0000-0000-000000000002  'Chuẩn bị giỏ đồ đi sinh'            PENDING
--
-- Both belong to the seeded demo mother 10000000-0000-0000-0000-000000000004, both
-- are SYSTEM_TEMPLATE origin, neither was ever completed, and no foreign key
-- references the table. Checklist v2 meanwhile holds a healthy 144 tasks across 33
-- instances, none of which depend on these rows.
--
-- Product decision (option 2 of the two recorded in the consolidation README):
-- retire them as demo data rather than build a SYSTEM_TEMPLATE parent instance for
-- two rows that carry no user value. This is NOT the USER_CREATED downgrade V3
-- §3.10 forbids — no lineage is rewritten, the rows are removed outright and their
-- full payload is printed first so it survives in the migration log.
--
-- Deliberately ID-specific. A blanket "delete anything unmigrated" would silently
-- destroy rows nobody has inspected; anything outside this list must instead fail
-- the gate in the next migration so a human looks at it.

DO $$
DECLARE
    v_row record;
    v_count bigint := 0;
    v_completed bigint;
BEGIN
    -- Refuse if either row turns out to carry completion state. Nothing observed
    -- had any, and a row that does is evidence of real use, not demo data.
    SELECT count(*) INTO v_completed
    FROM public.preparation_checklist_items
    WHERE checklist_item_id IN (
            '61000000-0000-0000-0000-000000000001'::uuid,
            '61000000-0000-0000-0000-000000000002'::uuid)
      AND (completed_at IS NOT NULL OR upper(status) IN ('COMPLETED', 'DONE'));

    IF v_completed > 0 THEN
        RAISE EXCEPTION
            'R9_RECONCILE_REFUSED: % legacy checklist row(s) carry completion state and are not demo data',
            v_completed;
    END IF;

    FOR v_row IN
        SELECT checklist_item_id, owner_user_id, title, status, category,
               mother_journey_id, template_entry_id, created_at
          FROM public.preparation_checklist_items
         WHERE checklist_item_id IN (
                 '61000000-0000-0000-0000-000000000001'::uuid,
                 '61000000-0000-0000-0000-000000000002'::uuid)
         ORDER BY checklist_item_id
    LOOP
        v_count := v_count + 1;
        RAISE NOTICE
            'R9_LEGACY_ITEM_RETIRED id=% owner=% title=% status=% category=% journey=% template=% created=%',
            v_row.checklist_item_id, v_row.owner_user_id, v_row.title, v_row.status,
            v_row.category, v_row.mother_journey_id, v_row.template_entry_id, v_row.created_at;
    END LOOP;

    RAISE NOTICE 'R9_LEGACY_ITEM_RETIRED_TOTAL=%', v_count;
END
$$;

DELETE FROM public.preparation_checklist_items
 WHERE checklist_item_id IN (
         '61000000-0000-0000-0000-000000000001'::uuid,
         '61000000-0000-0000-0000-000000000002'::uuid);

-- Post-condition. Anything still unmigrated is outside the inspected set and must
-- reach a human through the gate rather than be swept up here.
DO $$
DECLARE
    v_remaining bigint;
BEGIN
    SELECT count(*) INTO v_remaining
    FROM public.preparation_checklist_items legacy
    WHERE NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances task
         WHERE task.checklist_task_instance_id = legacy.checklist_item_id);

    IF v_remaining > 0 THEN
        RAISE WARNING
            'R9_RECONCILE_PARTIAL: % legacy checklist row(s) remain unmigrated and are not in the inspected set; the next migration will fail until they are resolved',
            v_remaining;
    END IF;
END
$$;
