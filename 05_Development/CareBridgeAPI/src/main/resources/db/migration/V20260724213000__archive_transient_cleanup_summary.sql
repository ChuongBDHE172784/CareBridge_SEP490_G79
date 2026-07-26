-- The Story 6.5 cleanup summary is migration evidence, not an application
-- table. Preserve any aggregate rows in canonical audit history, then remove
-- the transient relation so the deployed inventory remains 70 core tables
-- plus the three approved Release-1 consultation extensions.

DO $archive_cleanup_summary$
BEGIN
    IF to_regclass('public.baby_journey_link_cleanup_summary') IS NULL THEN
        RETURN;
    END IF;
    IF to_regclass('public.audit_events') IS NULL THEN
        RAISE EXCEPTION
            'Cannot archive baby journey cleanup summary without canonical audit_events';
    END IF;

    INSERT INTO public.audit_events (
        event_category,
        resource_type,
        purpose,
        decision,
        after_payload_jsonb,
        occurred_at,
        created_at
    )
    SELECT
        'DATA_MIGRATION',
        'baby_journey_link_cleanup_summary',
        'Archive aggregate Story 6.5 cleanup evidence before canonical inventory cutover',
        'COMPLETED',
        jsonb_build_object(
            'schemaVersion', '1',
            'rows', jsonb_agg(
                jsonb_build_object(
                    'migrationKey', migration_key,
                    'reasonCode', reason_code,
                    'affectedCount', affected_count,
                    'cleanedAt', cleaned_at
                )
                ORDER BY migration_key, reason_code
            )
        ),
        max(cleaned_at),
        now()
    FROM public.baby_journey_link_cleanup_summary
    HAVING count(*) > 0;
END
$archive_cleanup_summary$;

DROP TABLE IF EXISTS public.baby_journey_link_cleanup_summary;
