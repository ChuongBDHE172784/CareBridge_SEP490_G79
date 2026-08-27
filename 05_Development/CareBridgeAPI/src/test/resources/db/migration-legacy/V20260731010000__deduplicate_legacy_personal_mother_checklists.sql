-- Collapse only legacy MOTHER/SYSTEM_TEMPLATE duplicates that have no progress.
-- Logical personal identity deliberately excludes care_group_id. Historical rows
-- with parent or child progress remain untouched for explicit reconciliation.
-- Flyway keeps these locks until commit. They close the gap between candidate
-- snapshot, child cancellation, and parent cancellation during a rolling deploy.
LOCK TABLE public.checklist_instances IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE public.checklist_task_instances IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE checklist_legacy_personal_duplicates_to_cancel
ON COMMIT DROP AS
WITH candidates AS MATERIALIZED (
    SELECT instance.*,
           instance.status = 'PENDING'
               AND instance.completed_at IS NULL
               AND instance.cancelled_at IS NULL
               AND instance.cancellation_reason_code IS NULL
               AND EXISTS (
                   SELECT 1
                   FROM public.checklist_task_instances task
                   WHERE task.checklist_instance_id = instance.checklist_instance_id)
               AND NOT EXISTS (
                   SELECT 1
                   FROM public.checklist_task_instances task
                   WHERE task.checklist_instance_id = instance.checklist_instance_id
                     AND (task.status <> 'PENDING'
                          OR task.completed_at IS NOT NULL
                          OR task.skipped_at IS NOT NULL
                          OR task.cancelled_at IS NOT NULL
                          OR task.action_reason_code IS NOT NULL)) AS safely_cancellable
    FROM public.checklist_instances instance
    WHERE instance.recipient_role = 'MOTHER'
      AND instance.origin = 'SYSTEM_TEMPLATE'
      AND instance.status <> 'CANCELLED'
), annotated AS MATERIALIZED (
    SELECT candidates.*,
           count(*) OVER logical_identity AS duplicate_count,
           count(*) FILTER (WHERE NOT safely_cancellable)
               OVER logical_identity AS preserved_count
    FROM candidates
    WINDOW logical_identity AS (
        PARTITION BY template_version_id, recipient_user_id,
                     care_context_type, care_context_id, context_owner_user_id,
                     window_start, window_end)
), ranked_safe AS MATERIALIZED (
    SELECT annotated.*,
           row_number() OVER (
               PARTITION BY template_version_id, recipient_user_id,
                            care_context_type, care_context_id, context_owner_user_id,
                            window_start, window_end
               ORDER BY CASE WHEN care_group_id IS NULL THEN 0 ELSE 1 END,
                        created_at, checklist_instance_id) AS safe_rank
    FROM annotated
    WHERE safely_cancellable
)
SELECT checklist_instance_id, gen_random_uuid() AS correlation_id
FROM ranked_safe
WHERE duplicate_count > 1
  AND (preserved_count > 0 OR safe_rank > 1);

CREATE UNIQUE INDEX checklist_legacy_personal_duplicates_to_cancel_pk
    ON checklist_legacy_personal_duplicates_to_cancel(checklist_instance_id);

UPDATE public.checklist_task_instances task
SET status = 'CANCELLED',
    completed_at = NULL,
    skipped_at = NULL,
    cancelled_at = transaction_timestamp(),
    action_reason_code = 'LEGACY_PERSONAL_DUPLICATE',
    lock_version = task.lock_version + 1,
    updated_at = transaction_timestamp()
FROM checklist_legacy_personal_duplicates_to_cancel duplicate
WHERE task.checklist_instance_id = duplicate.checklist_instance_id
  AND task.status = 'PENDING'
  AND task.completed_at IS NULL
  AND task.skipped_at IS NULL
  AND task.cancelled_at IS NULL
  AND task.action_reason_code IS NULL
  AND EXISTS (
      SELECT 1
      FROM public.checklist_instances parent
      WHERE parent.checklist_instance_id = task.checklist_instance_id
        AND parent.status = 'PENDING'
        AND parent.completed_at IS NULL
        AND parent.cancelled_at IS NULL
        AND parent.cancellation_reason_code IS NULL)
  AND NOT EXISTS (
      SELECT 1
      FROM public.checklist_task_instances sibling
      WHERE sibling.checklist_instance_id = task.checklist_instance_id
        AND (sibling.status <> 'PENDING'
             OR sibling.completed_at IS NOT NULL
             OR sibling.skipped_at IS NOT NULL
             OR sibling.cancelled_at IS NOT NULL
             OR sibling.action_reason_code IS NOT NULL));

UPDATE public.checklist_instances instance
SET status = 'CANCELLED',
    completed_at = NULL,
    cancelled_at = transaction_timestamp(),
    cancellation_reason_code = 'LEGACY_PERSONAL_DUPLICATE',
    lock_version = instance.lock_version + 1,
    updated_at = transaction_timestamp()
FROM checklist_legacy_personal_duplicates_to_cancel duplicate
WHERE instance.checklist_instance_id = duplicate.checklist_instance_id
  AND instance.status = 'PENDING'
  AND instance.completed_at IS NULL
  AND instance.cancelled_at IS NULL
  AND instance.cancellation_reason_code IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM public.checklist_task_instances task
      WHERE task.checklist_instance_id = instance.checklist_instance_id
        AND task.status <> 'CANCELLED');

INSERT INTO public.audit_events
    (actor_user_id, event_category, subject_user_id,
     resource_type, resource_id, purpose, decision,
     occurred_at, created_at, event_origin, payload,
     correlation_id, severity, status,
     actor_type, actor_service, reason_code,
     care_context_type, care_context_id, template_version_id,
     checklist_task_instance_id, before_payload_jsonb, after_payload_jsonb)
SELECT
    NULL,
    'CHECKLIST_CANCELLED',
    parent.recipient_user_id,
    'CHECKLIST_TASK_INSTANCE',
    task.checklist_task_instance_id,
    'LEGACY_PERSONAL_CHECKLIST_DEDUPLICATION',
    'CANCELLED',
    transaction_timestamp(),
    transaction_timestamp(),
    'CHECKLIST_MIGRATION',
    jsonb_build_object('reasonCode', 'LEGACY_PERSONAL_DUPLICATE', 'metadata', 'REDACTED'),
    duplicate.correlation_id,
    'MEDIUM',
    'CLOSED',
    'SERVICE',
    'CHECKLIST_LEGACY_PERSONAL_DEDUP',
    'LEGACY_PERSONAL_DUPLICATE',
    parent.care_context_type,
    parent.care_context_id,
    parent.template_version_id,
    task.checklist_task_instance_id,
    jsonb_build_object('status', 'PENDING'),
    jsonb_build_object('status', 'CANCELLED', 'reasonCode', 'LEGACY_PERSONAL_DUPLICATE')
FROM checklist_legacy_personal_duplicates_to_cancel duplicate
JOIN public.checklist_instances parent
  ON parent.checklist_instance_id = duplicate.checklist_instance_id
JOIN public.checklist_task_instances task
  ON task.checklist_instance_id = parent.checklist_instance_id
 AND task.status = 'CANCELLED'
 AND task.action_reason_code = 'LEGACY_PERSONAL_DUPLICATE';

INSERT INTO public.audit_events
    (actor_user_id, event_category, subject_user_id,
     resource_type, resource_id, purpose, decision,
     occurred_at, created_at, event_origin, payload,
     correlation_id, severity, status,
     actor_type, actor_service, reason_code,
     care_context_type, care_context_id, template_version_id,
     before_payload_jsonb, after_payload_jsonb)
SELECT
    NULL,
    'CHECKLIST_CANCELLED',
    instance.recipient_user_id,
    'CHECKLIST_INSTANCE',
    instance.checklist_instance_id,
    'LEGACY_PERSONAL_CHECKLIST_DEDUPLICATION',
    'CANCELLED',
    transaction_timestamp(),
    transaction_timestamp(),
    'CHECKLIST_MIGRATION',
    jsonb_build_object('reasonCode', 'LEGACY_PERSONAL_DUPLICATE', 'metadata', 'REDACTED'),
    duplicate.correlation_id,
    'MEDIUM',
    'CLOSED',
    'SERVICE',
    'CHECKLIST_LEGACY_PERSONAL_DEDUP',
    'LEGACY_PERSONAL_DUPLICATE',
    instance.care_context_type,
    instance.care_context_id,
    instance.template_version_id,
    jsonb_build_object('status', 'PENDING'),
    jsonb_build_object('status', 'CANCELLED', 'reasonCode', 'LEGACY_PERSONAL_DUPLICATE')
FROM checklist_legacy_personal_duplicates_to_cancel duplicate
JOIN public.checklist_instances instance
  ON instance.checklist_instance_id = duplicate.checklist_instance_id
WHERE instance.status = 'CANCELLED'
  AND instance.cancellation_reason_code = 'LEGACY_PERSONAL_DUPLICATE';
