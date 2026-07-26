-- Batch 1: consolidate the legacy notifications table into notification_records.
-- The migration is deliberately fail-fast and transactional. No legacy table is
-- dropped until every row has been copied and the row-count reconciliation passes.

ALTER TABLE public.notification_records
    ADD COLUMN IF NOT EXISTS channel varchar(30) NOT NULL DEFAULT 'PUSH',
    ADD COLUMN IF NOT EXISTS updated_at timestamptz;

-- Existing canonical rows predate updated_at. Their only defensible historical
-- timestamp is created_at; using migration time would invent an audit event.
UPDATE public.notification_records
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE public.notification_records
    ALTER COLUMN updated_at SET DEFAULT now(),
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE public.notification_records
    DROP CONSTRAINT IF EXISTS notification_records_type_check,
    DROP CONSTRAINT IF EXISTS notification_records_status_check,
    DROP CONSTRAINT IF EXISTS notification_records_channel_check;

ALTER TABLE public.notification_records
    ADD CONSTRAINT notification_records_type_check CHECK (
        type IN ('REMINDER', 'COMMUNITY_REPLY', 'CONSULTATION', 'EMERGENCY', 'MESSAGE', 'GROUP_INVITE')
    ),
    ADD CONSTRAINT notification_records_status_check CHECK (
        status IN ('PENDING', 'PROCESSING', 'SENT', 'DELIVERED', 'FAILED')
    ),
    ADD CONSTRAINT notification_records_channel_check CHECK (
        channel IN ('PUSH', 'EMAIL', 'IN_APP')
    );

DO $$
DECLARE
    legacy_count bigint;
    canonical_count_before bigint;
    canonical_count_after bigint;
    invalid_count bigint;
    dependency_count bigint;
BEGIN
    IF to_regclass('public.notifications') IS NULL THEN
        RAISE EXCEPTION 'Legacy table public.notifications is missing; refusing an unverified consolidation';
    END IF;

    -- Freeze the legacy source before taking the reconciliation snapshot. Without
    -- this lock, a concurrent legacy writer could commit after the copy and then
    -- have its row removed by the table drop.
    LOCK TABLE public.notifications IN ACCESS EXCLUSIVE MODE;

    SELECT count(*) INTO legacy_count FROM public.notifications;
    SELECT count(*) INTO canonical_count_before FROM public.notification_records;

    SELECT count(*) INTO invalid_count
    FROM public.notifications n
    WHERE n.notification_id IS NULL
       OR n.recipient_user_id IS NULL
       OR n.notification_type IS NULL
       OR n.notification_type NOT IN (
            'REMINDER', 'COMMUNITY_REPLY', 'CONSULTATION', 'EMERGENCY', 'MESSAGE', 'GROUP_INVITE'
       )
       OR n.title IS NULL
       OR char_length(n.title) > 255
       OR n.body IS NULL
       OR char_length(n.reference_type) > 50
       OR char_length(n.fcm_message_id) > 255
       OR n.delivery_status IS NULL
       OR n.delivery_status NOT IN ('PENDING', 'PROCESSING', 'SENT', 'DELIVERED', 'FAILED')
       OR n.channel IS NULL
       OR n.channel NOT IN ('PUSH', 'EMAIL', 'IN_APP')
       OR n.attempt_count IS NULL
       OR n.attempt_count < 0
       OR n.created_at IS NULL
       OR n.is_read IS NULL
       OR (n.is_read AND n.read_at IS NULL)
       OR (NOT n.is_read AND n.read_at IS NOT NULL)
       OR (n.metadata IS NOT NULL AND jsonb_typeof(n.metadata) <> 'object')
       OR EXISTS (
            SELECT 1
            FROM jsonb_each(
                CASE WHEN jsonb_typeof(n.metadata) = 'object' THEN n.metadata ELSE '{}'::jsonb END
            ) metadata_entry
            WHERE jsonb_typeof(metadata_entry.value) <> 'string'
       );
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: % row(s) have invalid required/type/status/channel/read data', invalid_count;
    END IF;

    SELECT count(*) INTO invalid_count
    FROM public.notifications n
    LEFT JOIN public.users u ON u.user_id = n.recipient_user_id
    WHERE u.user_id IS NULL;
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: % row(s) reference a missing user', invalid_count;
    END IF;

    SELECT count(*) INTO invalid_count
    FROM public.notifications n
    JOIN public.notification_records r ON r.id = n.notification_id;
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: % notification ID conflict(s)', invalid_count;
    END IF;

    SELECT count(*) INTO invalid_count
    FROM (
        SELECT recipient_user_id, reference_id
        FROM public.notifications
        WHERE notification_type = 'MESSAGE'
          AND reference_type = 'DIRECT_MESSAGE'
        GROUP BY recipient_user_id, reference_id
        HAVING count(*) > 1
    ) duplicates;
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: duplicate MESSAGE business keys';
    END IF;

    SELECT count(*) INTO invalid_count
    FROM public.notifications n
    JOIN public.notification_records r
      ON r.user_id = n.recipient_user_id
     AND r.reference_id IS NOT DISTINCT FROM n.reference_id
     AND r.type = 'MESSAGE'
     AND r.reference_type = 'DIRECT_MESSAGE'
    WHERE n.notification_type = 'MESSAGE'
      AND n.reference_type = 'DIRECT_MESSAGE';
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: MESSAGE business-key conflict with notification_records';
    END IF;

    SELECT count(*) INTO invalid_count
    FROM (
        SELECT recipient_user_id, reference_id, metadata ->> 'eventType' AS event_type
        FROM public.notifications
        WHERE notification_type = 'CONSULTATION'
          AND reference_type = 'CONSULTATION_REQUEST'
        GROUP BY recipient_user_id, reference_id, metadata ->> 'eventType'
        HAVING count(*) > 1
    ) duplicates;
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: duplicate CONSULTATION_REQUEST business keys';
    END IF;

    SELECT count(*) INTO invalid_count
    FROM public.notifications n
    JOIN public.notification_records r
      ON r.user_id = n.recipient_user_id
     AND r.reference_id IS NOT DISTINCT FROM n.reference_id
     AND (r.metadata ->> 'eventType') IS NOT DISTINCT FROM (n.metadata ->> 'eventType')
     AND r.type = 'CONSULTATION'
     AND r.reference_type = 'CONSULTATION_REQUEST'
    WHERE n.notification_type = 'CONSULTATION'
      AND n.reference_type = 'CONSULTATION_REQUEST';
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: CONSULTATION_REQUEST business-key conflict with notification_records';
    END IF;

    SELECT count(*) INTO dependency_count
    FROM pg_constraint c
    WHERE c.contype = 'f'
      AND c.confrelid = 'public.notifications'::regclass
      AND c.conrelid <> 'public.notifications'::regclass;
    IF dependency_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: % inbound foreign key(s)', dependency_count;
    END IF;

    SELECT count(DISTINCT dependent_view.oid) INTO dependency_count
    FROM pg_depend d
    JOIN pg_rewrite rewrite ON rewrite.oid = d.objid
    JOIN pg_class dependent_view ON dependent_view.oid = rewrite.ev_class
    WHERE d.refobjid = 'public.notifications'::regclass
      AND dependent_view.relkind IN ('v', 'm')
      AND dependent_view.oid <> 'public.notifications'::regclass;
    IF dependency_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: % dependent view(s)', dependency_count;
    END IF;

    SELECT count(DISTINCT procedure_row.oid) INTO dependency_count
    FROM pg_proc procedure_row
    JOIN pg_namespace procedure_namespace ON procedure_namespace.oid = procedure_row.pronamespace
    WHERE procedure_row.prokind IN ('f', 'p')
      AND procedure_namespace.nspname NOT IN ('pg_catalog', 'information_schema')
      AND procedure_namespace.nspname NOT LIKE 'pg_toast%'
      AND pg_get_functiondef(procedure_row.oid)
          ~* '(^|[^a-zA-Z0-9_])("?public"?[.])?"?notifications"?([^a-zA-Z0-9_]|$)';
    IF dependency_count > 0 THEN
        RAISE EXCEPTION 'Legacy notifications preflight failed: % function/procedure text reference(s)', dependency_count;
    END IF;

    INSERT INTO public.notification_records (
        id, user_id, type, title, body, reference_id, reference_type, status,
        fcm_message_id, attempt_count, created_at, sent_at, failed_at,
        is_read, read_at, metadata, channel, updated_at
    )
    SELECT
        n.notification_id,
        n.recipient_user_id,
        n.notification_type,
        n.title,
        n.body,
        n.reference_id,
        n.reference_type,
        n.delivery_status,
        n.fcm_message_id,
        n.attempt_count,
        n.created_at,
        n.sent_at,
        n.failed_at,
        n.is_read,
        n.read_at,
        n.metadata,
        n.channel,
        COALESCE(n.updated_at, n.created_at)
    FROM public.notifications n;

    SELECT count(*) INTO canonical_count_after FROM public.notification_records;
    IF canonical_count_after <> canonical_count_before + legacy_count THEN
        RAISE EXCEPTION
            'Notification reconciliation failed: expected % canonical rows, found %',
            canonical_count_before + legacy_count,
            canonical_count_after;
    END IF;
END $$;

DROP TABLE public.notifications;
