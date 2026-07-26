-- Restore consultation-request notification and audit discriminators immediately
-- after V22020800 retires the legacy consultation parent.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

DO $restore_story68_references$
BEGIN
    IF to_regclass(
        'carebridge_migration_bridge.story68_notification_reference_bridge')
       IS NULL
       OR to_regclass(
        'carebridge_migration_bridge.story68_audit_reference_bridge') IS NULL THEN
        RAISE EXCEPTION 'STORY68_REFERENCE_BRIDGE_MISSING';
    END IF;

    IF to_regclass('public.notification_records') IS NULL THEN
        RAISE EXCEPTION 'STORY68_NOTIFICATION_REFERENCE_TARGET_MISSING';
    END IF;

    LOCK TABLE public.notification_records IN SHARE ROW EXCLUSIVE MODE;

    UPDATE public.notification_records notification
       SET reference_type = bridge.snapshot_jsonb ->> 'reference_type'
      FROM carebridge_migration_bridge.story68_notification_reference_bridge bridge
     WHERE notification.id = bridge.notification_id;

    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.story68_notification_reference_bridge bridge
          LEFT JOIN public.notification_records notification
            ON notification.id = bridge.notification_id
           AND to_jsonb(notification) = bridge.snapshot_jsonb
         WHERE notification.id IS NULL
    ) THEN
        RAISE EXCEPTION 'STORY68_NOTIFICATION_REFERENCE_RESTORE_RECONCILIATION_FAILED';
    END IF;

    IF to_regclass('public.audit_logs') IS NOT NULL THEN
        LOCK TABLE public.audit_logs IN SHARE ROW EXCLUSIVE MODE;

        UPDATE public.audit_logs audit
           SET entity_type = bridge.snapshot_jsonb ->> 'entity_type'
          FROM carebridge_migration_bridge.story68_audit_reference_bridge bridge
         WHERE audit.audit_log_id = bridge.audit_log_id;

        IF EXISTS (
            SELECT 1
              FROM carebridge_migration_bridge.story68_audit_reference_bridge bridge
              LEFT JOIN public.audit_logs audit
                ON audit.audit_log_id = bridge.audit_log_id
               AND to_jsonb(audit) = bridge.snapshot_jsonb
             WHERE audit.audit_log_id IS NULL
        ) THEN
            RAISE EXCEPTION 'STORY68_AUDIT_REFERENCE_RESTORE_RECONCILIATION_FAILED';
        END IF;
    ELSIF EXISTS (
        SELECT 1 FROM carebridge_migration_bridge.story68_audit_reference_bridge
    ) OR NOT EXISTS (
        SELECT 1
          FROM public.flyway_schema_history
         WHERE version = '20260722231900'
           AND success
    ) THEN
        RAISE EXCEPTION 'STORY68_AUDIT_REFERENCE_TARGET_MISSING';
    END IF;
END
$restore_story68_references$;

DO $restore_story68_notification_index$
BEGIN
    IF to_regclass('public.notification_records') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_records_consultation_request
            ON public.notification_records (
                user_id, reference_id, ((metadata ->> 'eventType')))
            WHERE type = 'CONSULTATION'
              AND reference_type = 'CONSULTATION_REQUEST';
    END IF;
END
$restore_story68_notification_index$;

DROP TABLE carebridge_migration_bridge.story68_notification_reference_bridge;
DROP TABLE carebridge_migration_bridge.story68_audit_reference_bridge;
