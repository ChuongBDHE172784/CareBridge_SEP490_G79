-- Forward-only compatibility for the bidirectional checklist task action.
-- Existing applied migrations remain immutable.

ALTER TABLE public.checklist_action_commands
    DROP CONSTRAINT IF EXISTS checklist_action_commands_action_ck;

ALTER TABLE public.checklist_action_commands
    ADD CONSTRAINT checklist_action_commands_action_ck
    CHECK (action_type IN ('COMPLETE', 'SKIP', 'REOPEN'));

ALTER TABLE public.audit_events
    DROP CONSTRAINT IF EXISTS audit_events_checklist_actor_type_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_actor_shape_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_correlation_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_context_type_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_context_pair_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_subject_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_task_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_reason_code_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_reason_required_ck;

ALTER TABLE public.audit_events
    ADD CONSTRAINT audit_events_checklist_actor_type_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_REOPENED','CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED',
            'CHECKLIST_MIGRATION_QUARANTINED') OR
         (actor_type IS NOT NULL AND actor_type IN ('USER','SYSTEM','SERVICE'))),
    ADD CONSTRAINT audit_events_checklist_actor_shape_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_REOPENED','CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED',
            'CHECKLIST_MIGRATION_QUARANTINED') OR
         ((actor_type = 'USER' AND actor_user_id IS NOT NULL AND actor_service IS NULL) OR
          (actor_type IN ('SYSTEM','SERVICE') AND actor_user_id IS NULL
            AND actor_service IS NOT NULL AND btrim(actor_service) <> ''))),
    ADD CONSTRAINT audit_events_checklist_correlation_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_REOPENED','CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED',
            'CHECKLIST_MIGRATION_QUARANTINED') OR correlation_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_context_type_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_REOPENED','CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED',
            'CHECKLIST_MIGRATION_QUARANTINED') OR
         care_context_type IS NULL OR care_context_type IN ('JOURNEY','BABY')),
    ADD CONSTRAINT audit_events_checklist_context_pair_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_REOPENED','CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED',
            'CHECKLIST_MIGRATION_QUARANTINED') OR
         ((care_context_type IS NULL AND care_context_id IS NULL) OR
          (care_context_type IS NOT NULL AND care_context_id IS NOT NULL))),
    ADD CONSTRAINT audit_events_checklist_subject_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_REOPENED','CHECKLIST_CANCELLED') OR subject_user_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_task_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED','CHECKLIST_REOPENED') OR
         checklist_task_instance_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_reason_code_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_REOPENED','CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED',
            'CHECKLIST_MIGRATION_QUARANTINED') OR
         reason_code IS NULL OR reason_code ~ '^[A-Z0-9_]{1,80}$'),
    ADD CONSTRAINT audit_events_checklist_reason_required_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_SKIPPED','CHECKLIST_CANCELLED',
            'CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         reason_code IS NOT NULL);
