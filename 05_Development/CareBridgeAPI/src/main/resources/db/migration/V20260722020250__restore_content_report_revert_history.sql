-- Complete the branch-history bridge after both immutable revert-column
-- migrations have had their normal Flyway ordering opportunity.
DO $$
DECLARE
    state_count bigint;
    source_table_created boolean;
    captured_row_count bigint;
    bridge_row_count bigint;
    source_row_count bigint;
    mismatch_count bigint;
BEGIN
    IF to_regclass('carebridge_migration_bridge.content_report_revert_state') IS NULL
       OR to_regclass('carebridge_migration_bridge.content_report_revert_rows') IS NULL THEN
        RAISE EXCEPTION 'content-report revert bridge state is missing';
    END IF;

    SELECT count(*) INTO state_count
      FROM carebridge_migration_bridge.content_report_revert_state;
    IF state_count <> 1 THEN
        RAISE EXCEPTION 'expected one content-report revert bridge state row, found %', state_count;
    END IF;

    SELECT state.source_table_created, state.captured_row_count
      INTO source_table_created, captured_row_count
      FROM carebridge_migration_bridge.content_report_revert_state state
     WHERE state.migration_key = 'V20260720100000_5';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'content-report revert bridge state key is missing';
    END IF;

    SELECT count(*) INTO bridge_row_count
      FROM carebridge_migration_bridge.content_report_revert_rows;
    IF bridge_row_count <> captured_row_count THEN
        RAISE EXCEPTION 'content-report revert bridge count changed: expected %, found %',
            captured_row_count, bridge_row_count;
    END IF;

    IF to_regclass('public.content_reports') IS NULL THEN
        RAISE EXCEPTION 'content_reports is missing before revert bridge restoration';
    END IF;

    IF source_table_created THEN
        SELECT count(*) INTO source_row_count FROM public.content_reports;
        IF captured_row_count <> 0 OR source_row_count <> 0 THEN
            RAISE EXCEPTION
                'synthetic content_reports unexpectedly contains data: captured %, source %',
                captured_row_count, source_row_count;
        END IF;
        DROP TABLE public.content_reports;
    ELSE
        ALTER TABLE public.content_reports
            ADD COLUMN IF NOT EXISTS reverted_at timestamptz NULL,
            ADD COLUMN IF NOT EXISTS reverted_by uuid NULL;

        SELECT count(*) INTO mismatch_count
          FROM carebridge_migration_bridge.content_report_revert_rows bridge
          LEFT JOIN public.content_reports source USING (report_id)
         WHERE source.report_id IS NULL;
        IF mismatch_count <> 0 THEN
            RAISE EXCEPTION 'content-report revert restore has % orphan bridge rows', mismatch_count;
        END IF;

        UPDATE public.content_reports source
           SET reverted_at = bridge.reverted_at,
               reverted_by = bridge.reverted_by
          FROM carebridge_migration_bridge.content_report_revert_rows bridge
         WHERE source.report_id = bridge.report_id;

        SELECT count(*) INTO mismatch_count
          FROM carebridge_migration_bridge.content_report_revert_rows bridge
          JOIN public.content_reports source USING (report_id)
         WHERE source.reverted_at IS DISTINCT FROM bridge.reverted_at
            OR source.reverted_by IS DISTINCT FROM bridge.reverted_by;
        IF mismatch_count <> 0 THEN
            RAISE EXCEPTION 'content-report revert restore verification failed for % rows',
                mismatch_count;
        END IF;
    END IF;
END $$;

DROP TABLE carebridge_migration_bridge.content_report_revert_rows;
DROP TABLE carebridge_migration_bridge.content_report_revert_state;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_class relation
          JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
         WHERE namespace.nspname = 'carebridge_migration_bridge'
    ) THEN
        DROP SCHEMA carebridge_migration_bridge;
    END IF;
END $$;
