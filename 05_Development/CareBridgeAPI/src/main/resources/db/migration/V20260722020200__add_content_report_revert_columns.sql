-- CB-MOD-IMP-015: revert-audit columns for content_reports.
-- This migration was renumbered before deployment to resolve the repository collision
-- with the already-applied secure-baby migration V20260720100000.
DO $$
DECLARE
    existing_type text;
BEGIN
    IF to_regclass('public.content_reports') IS NULL THEN
        RAISE EXCEPTION 'public.content_reports is missing';
    END IF;

    SELECT data_type INTO existing_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'content_reports'
      AND column_name = 'reverted_at';
    IF existing_type IS NOT NULL AND existing_type <> 'timestamp with time zone' THEN
        RAISE EXCEPTION 'content_reports.reverted_at has unexpected type: %', existing_type;
    END IF;

    SELECT data_type INTO existing_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'content_reports'
      AND column_name = 'reverted_by';
    IF existing_type IS NOT NULL AND existing_type <> 'uuid' THEN
        RAISE EXCEPTION 'content_reports.reverted_by has unexpected type: %', existing_type;
    END IF;
END $$;

-- reverted_at/reverted_by record the most recent revert-to-PENDING event;
-- resolved_at/assigned_moderator_id retain the original resolution audit trail.
ALTER TABLE public.content_reports
    ADD COLUMN IF NOT EXISTS reverted_at timestamptz NULL,
    ADD COLUMN IF NOT EXISTS reverted_by uuid NULL;
