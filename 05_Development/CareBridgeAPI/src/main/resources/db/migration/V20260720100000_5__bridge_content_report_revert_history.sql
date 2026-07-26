-- Preserve immutable Flyway history while reconciling the two branch timelines.
--
-- On the feature branch V20260720100001 may already be applied. On the canonical
-- branch V20260722020200 may already be applied while V20260720100001 is newly
-- discovered out of order. Capture any existing values before the immutable
-- migration is allowed to add the columns again. If Phase 2 already removed the
-- source table, create a zero-row compatibility table solely for that DDL.
CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

CREATE TABLE carebridge_migration_bridge.content_report_revert_state (
    migration_key text PRIMARY KEY,
    source_table_created boolean NOT NULL,
    captured_row_count bigint NOT NULL CHECK (captured_row_count >= 0),
    CONSTRAINT chk_content_report_revert_state_key
        CHECK (migration_key = 'V20260720100000_5')
);

CREATE TABLE carebridge_migration_bridge.content_report_revert_rows (
    report_id uuid PRIMARY KEY,
    reverted_at timestamptz,
    reverted_by uuid
);

DO $$
DECLARE
    source_exists boolean;
    has_reverted_at boolean;
    has_reverted_by boolean;
    report_id_type text;
    reverted_at_type text;
    reverted_by_type text;
    source_count bigint;
    bridge_count bigint;
    mismatch_count bigint;
BEGIN
    IF EXISTS (SELECT 1 FROM carebridge_migration_bridge.content_report_revert_state)
       OR EXISTS (SELECT 1 FROM carebridge_migration_bridge.content_report_revert_rows) THEN
        RAISE EXCEPTION 'content-report revert bridge is not empty before capture';
    END IF;

    source_exists := to_regclass('public.content_reports') IS NOT NULL;
    IF NOT source_exists THEN
        CREATE TABLE public.content_reports (
            report_id uuid PRIMARY KEY
        );
        INSERT INTO carebridge_migration_bridge.content_report_revert_state (
            migration_key, source_table_created, captured_row_count
        ) VALUES ('V20260720100000_5', true, 0);
        RETURN;
    END IF;

    -- Hold the source stable through capture verification and DROP COLUMN.
    -- The lock is dynamic because the canonical-cutover path legitimately has
    -- no content_reports relation and creates only an empty compatibility table.
    EXECUTE 'LOCK TABLE public.content_reports IN SHARE ROW EXCLUSIVE MODE';

    SELECT data_type
      INTO report_id_type
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'content_reports'
       AND column_name = 'report_id';
    IF report_id_type IS DISTINCT FROM 'uuid' THEN
        RAISE EXCEPTION 'content_reports.report_id must be uuid, found %', report_id_type;
    END IF;

    SELECT EXISTS (
               SELECT 1 FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'content_reports'
                  AND column_name = 'reverted_at'
           ),
           EXISTS (
               SELECT 1 FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'content_reports'
                  AND column_name = 'reverted_by'
           )
      INTO has_reverted_at, has_reverted_by;

    IF has_reverted_at THEN
        SELECT data_type INTO reverted_at_type
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'content_reports'
           AND column_name = 'reverted_at';
        IF reverted_at_type IS DISTINCT FROM 'timestamp with time zone' THEN
            RAISE EXCEPTION 'content_reports.reverted_at has unexpected type: %', reverted_at_type;
        END IF;
    END IF;
    IF has_reverted_by THEN
        SELECT data_type INTO reverted_by_type
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'content_reports'
           AND column_name = 'reverted_by';
        IF reverted_by_type IS DISTINCT FROM 'uuid' THEN
            RAISE EXCEPTION 'content_reports.reverted_by has unexpected type: %', reverted_by_type;
        END IF;
    END IF;

    IF has_reverted_at AND has_reverted_by THEN
        INSERT INTO carebridge_migration_bridge.content_report_revert_rows (
            report_id, reverted_at, reverted_by
        )
        SELECT report_id, reverted_at, reverted_by FROM public.content_reports;
    ELSIF has_reverted_at THEN
        INSERT INTO carebridge_migration_bridge.content_report_revert_rows (
            report_id, reverted_at, reverted_by
        )
        SELECT report_id, reverted_at, NULL FROM public.content_reports;
    ELSIF has_reverted_by THEN
        INSERT INTO carebridge_migration_bridge.content_report_revert_rows (
            report_id, reverted_at, reverted_by
        )
        SELECT report_id, NULL, reverted_by FROM public.content_reports;
    ELSE
        INSERT INTO carebridge_migration_bridge.content_report_revert_rows (
            report_id, reverted_at, reverted_by
        )
        SELECT report_id, NULL, NULL FROM public.content_reports;
    END IF;

    SELECT count(*) INTO source_count FROM public.content_reports;
    SELECT count(*) INTO bridge_count
      FROM carebridge_migration_bridge.content_report_revert_rows;
    IF source_count <> bridge_count THEN
        RAISE EXCEPTION 'content-report revert capture count mismatch: source %, bridge %',
            source_count, bridge_count;
    END IF;

    IF has_reverted_at AND has_reverted_by THEN
        SELECT count(*) INTO mismatch_count
          FROM public.content_reports source
          FULL JOIN carebridge_migration_bridge.content_report_revert_rows bridge
            USING (report_id)
         WHERE source.report_id IS NULL
            OR bridge.report_id IS NULL
            OR source.reverted_at IS DISTINCT FROM bridge.reverted_at
            OR source.reverted_by IS DISTINCT FROM bridge.reverted_by;
    ELSIF has_reverted_at THEN
        SELECT count(*) INTO mismatch_count
          FROM public.content_reports source
          FULL JOIN carebridge_migration_bridge.content_report_revert_rows bridge
            USING (report_id)
         WHERE source.report_id IS NULL
            OR bridge.report_id IS NULL
            OR source.reverted_at IS DISTINCT FROM bridge.reverted_at
            OR bridge.reverted_by IS NOT NULL;
    ELSIF has_reverted_by THEN
        SELECT count(*) INTO mismatch_count
          FROM public.content_reports source
          FULL JOIN carebridge_migration_bridge.content_report_revert_rows bridge
            USING (report_id)
         WHERE source.report_id IS NULL
            OR bridge.report_id IS NULL
            OR bridge.reverted_at IS NOT NULL
            OR source.reverted_by IS DISTINCT FROM bridge.reverted_by;
    ELSE
        SELECT count(*) INTO mismatch_count
          FROM public.content_reports source
          FULL JOIN carebridge_migration_bridge.content_report_revert_rows bridge
            USING (report_id)
         WHERE source.report_id IS NULL
            OR bridge.report_id IS NULL
            OR bridge.reverted_at IS NOT NULL
            OR bridge.reverted_by IS NOT NULL;
    END IF;
    IF mismatch_count <> 0 THEN
        RAISE EXCEPTION 'content-report revert capture verification failed for % rows',
            mismatch_count;
    END IF;

    INSERT INTO carebridge_migration_bridge.content_report_revert_state (
        migration_key, source_table_created, captured_row_count
    ) VALUES ('V20260720100000_5', false, source_count);

    IF has_reverted_at THEN
        ALTER TABLE public.content_reports DROP COLUMN reverted_at;
    END IF;
    IF has_reverted_by THEN
        ALTER TABLE public.content_reports DROP COLUMN reverted_by;
    END IF;
END $$;
