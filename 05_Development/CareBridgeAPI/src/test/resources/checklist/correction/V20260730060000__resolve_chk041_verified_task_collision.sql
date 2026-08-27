-- Test-only forward correction for the disposable CHK-041 rehearsal.
DO $$
DECLARE
    source_uuid constant uuid := 'c0414000-0000-4000-8000-000000000001';
    source_count bigint;
    unresolved_count bigint;
    repaired_count bigint;
BEGIN
    SELECT count(*) INTO source_count
    FROM public.preparation_checklist_items
    WHERE checklist_item_id = source_uuid;

    SELECT count(*) INTO unresolved_count
    FROM public.checklist_migration_quarantine
    WHERE source_table = 'preparation_checklist_items'
      AND source_id = source_uuid
      AND reason_code = 'LEGACY_TASK_KEY_COLLISION'
      AND resolved_at IS NULL;

    SELECT count(*) INTO repaired_count
    FROM public.checklist_task_instances task
    JOIN public.preparation_checklist_items source
      ON source.checklist_item_id = task.checklist_task_instance_id
    WHERE source.checklist_item_id = source_uuid
      AND task.title_snapshot = source.title
      AND task.status = CASE upper(source.status)
          WHEN 'OPEN' THEN 'PENDING'
          WHEN 'DONE' THEN 'COMPLETED'
          ELSE upper(source.status)
      END
      AND task.created_at = source.created_at
      AND task.updated_at = source.updated_at;

    IF source_count = 0 THEN
        IF unresolved_count <> 0 OR repaired_count <> 0 THEN
            RAISE EXCEPTION 'CHK041_CORRECTION_CLEAN_CHAIN_DRIFT: quarantine=%, repaired=%',
                unresolved_count, repaired_count;
        END IF;
        RETURN;
    END IF;

    IF source_count <> 1 OR unresolved_count <> 1 OR repaired_count <> 1 THEN
        RAISE EXCEPTION 'CHK041_CORRECTION_PRECONDITION_FAILED: quarantine=%, repaired=%',
            unresolved_count, repaired_count;
    END IF;

    UPDATE public.checklist_migration_quarantine
    SET resolved_at = timestamptz '2026-07-30 12:00:00+00',
        resolved_by = '10000000-0000-0000-0000-000000000004'::uuid,
        resolution_code = 'FORWARD_REPAIRED_TARGET_VERIFIED'
    WHERE source_table = 'preparation_checklist_items'
      AND source_id = source_uuid
      AND reason_code = 'LEGACY_TASK_KEY_COLLISION'
      AND resolved_at IS NULL;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'CHK041_CORRECTION_NOT_APPLIED';
    END IF;
END
$$;
