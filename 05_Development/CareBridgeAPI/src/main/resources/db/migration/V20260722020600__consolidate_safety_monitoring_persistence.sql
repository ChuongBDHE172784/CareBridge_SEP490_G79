-- Batch 4: complete the canonical safety aggregate and remove only proven-empty
-- legacy persistence. PostgreSQL executes this Flyway migration transactionally;
-- every preflight failure therefore rolls back the additive schema changes too.

DO $$
DECLARE
    existing_definition text;
    existing_expression text;
BEGIN
    SELECT pg_get_constraintdef(constraint_row.oid)
      INTO existing_definition
      FROM pg_constraint constraint_row
     WHERE constraint_row.conrelid = 'public.audit_logs'::regclass
       AND constraint_row.conname = 'audit_logs_action_check';
    IF existing_definition IS NULL THEN
        RAISE EXCEPTION 'Batch4 preflight failed: audit_logs_action_check is missing';
    END IF;

    existing_expression := substring(
        existing_definition FROM 8 FOR char_length(existing_definition) - 8);
    ALTER TABLE public.audit_logs DROP CONSTRAINT audit_logs_action_check;
    EXECUTE 'ALTER TABLE public.audit_logs ADD CONSTRAINT audit_logs_action_check CHECK (('
        || existing_expression
        || ') OR action IN (''SAFETY_MONITORING_ENABLED'', ''SAFETY_MONITORING_DISABLED'', '
        || '''SAFETY_EVENT_RECORDED'', ''SAFETY_EVENT_RESPONDED'', ''SAFETY_EVENT_ESCALATED'', '
        || '''EMERGENCY_ALERT_DELIVERY''))';
END $$;

ALTER TABLE public.consent_grants
    DROP CONSTRAINT consent_grants_data_type_check;

ALTER TABLE public.consent_grants
    ADD CONSTRAINT consent_grants_data_type_check CHECK (
        data_type IN ('HEALTH_RECORD', 'LOCATION', 'FAMILY_DATA', 'COMMUNITY_POST',
                      'SENSITIVE_DATA', 'RAG_CONTEXT', 'EXPERT_SHARED_DATA',
                      'MOTHER_BASELINE', 'SENSOR_DATA')
    );

ALTER TABLE public.safety_monitoring_config
    ADD COLUMN countdown_seconds integer NOT NULL DEFAULT 30,
    ADD COLUMN sensor_permission_granted boolean NOT NULL DEFAULT false,
    ADD COLUMN sensor_permission_recorded_at timestamptz;

ALTER TABLE public.safety_monitoring_config
    ADD CONSTRAINT chk_safety_countdown_seconds
        CHECK (countdown_seconds IN (15, 30, 60)),
    ADD CONSTRAINT chk_safety_sensor_permission_evidence
        CHECK (sensor_permission_granted = false OR sensor_permission_recorded_at IS NOT NULL);

ALTER TABLE public.imu_safety_events
    ADD COLUMN signal_key varchar(200),
    ADD COLUMN client_detected_at timestamptz,
    ADD COLUMN countdown_deadline_at timestamptz,
    ADD COLUMN response_type varchar(30),
    ADD COLUMN response_reason varchar(500),
    ADD COLUMN responded_at timestamptz,
    ADD COLUMN escalation_started_at timestamptz,
    ADD COLUMN emergency_session_id uuid REFERENCES public.emergency_sessions(id);

ALTER TABLE public.imu_safety_events
    ADD CONSTRAINT chk_safety_event_response_type CHECK (
        response_type IS NULL OR response_type IN ('I_AM_OK', 'FALSE_POSITIVE', 'NEED_HELP', 'TIMEOUT')
    ),
    ADD CONSTRAINT chk_safety_event_response_evidence CHECK (
        (response_type IS NULL AND responded_at IS NULL)
        OR (response_type IS NOT NULL AND responded_at IS NOT NULL)
    );

CREATE UNIQUE INDEX uq_imu_safety_events_session_signal
    ON public.imu_safety_events(imu_session_id, signal_key)
    WHERE signal_key IS NOT NULL;

CREATE INDEX idx_imu_safety_events_pending_countdown
    ON public.imu_safety_events(countdown_deadline_at)
    WHERE status = 'OPEN' AND response_type IS NULL;

CREATE TABLE public.safety_event_responses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    safety_event_id uuid NOT NULL REFERENCES public.imu_safety_events(id),
    owner_user_id uuid NOT NULL REFERENCES public.users(user_id),
    response_type varchar(30) NOT NULL,
    reason varchar(500),
    responded_at timestamptz NOT NULL,
    created_by uuid REFERENCES public.users(user_id),
    actor_type varchar(20) NOT NULL,
    CONSTRAINT chk_safety_event_responses_type CHECK (
        response_type IN ('I_AM_OK', 'FALSE_POSITIVE', 'NEED_HELP', 'TIMEOUT')
    ),
    CONSTRAINT chk_safety_event_response_actor CHECK (
        (actor_type = 'OWNER' AND created_by IS NOT NULL)
        OR (actor_type = 'SYSTEM' AND created_by IS NULL)
    ),
    CONSTRAINT uq_safety_event_terminal_response UNIQUE (safety_event_id)
);

CREATE TABLE public.emergency_alert_deliveries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    emergency_session_id uuid NOT NULL REFERENCES public.emergency_sessions(id),
    recipient_user_id uuid NOT NULL REFERENCES public.users(user_id),
    device_token_id uuid NOT NULL REFERENCES public.device_tokens(id),
    notification_record_id uuid NOT NULL REFERENCES public.notification_records(id),
    delivery_status varchar(20) NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    fcm_message_id varchar(255),
    failure_code varchar(120),
    created_at timestamptz NOT NULL DEFAULT now(),
    delivered_at timestamptz,
    CONSTRAINT chk_emergency_alert_delivery_status CHECK (
        delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED')
    ),
    CONSTRAINT chk_emergency_alert_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT uq_emergency_alert_recipient_device UNIQUE (emergency_session_id, device_token_id)
);

CREATE INDEX idx_emergency_alert_deliveries_session
    ON public.emergency_alert_deliveries(emergency_session_id, created_at);

CREATE TABLE public.emergency_alert_attempts (
    emergency_session_id uuid PRIMARY KEY REFERENCES public.emergency_sessions(id),
    status varchar(20) NOT NULL,
    started_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    lease_expires_at timestamptz NOT NULL,
    attempt_number integer NOT NULL DEFAULT 1,
    successful_recipient_count integer NOT NULL DEFAULT 0,
    failed_recipient_count integer NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_emergency_alert_attempt_status CHECK (
        status IN ('PROCESSING', 'NO_RECIPIENTS', 'FAILED', 'PARTIAL', 'SENT')
    ),
    CONSTRAINT chk_emergency_alert_attempt_counts CHECK (
        successful_recipient_count >= 0 AND failed_recipient_count >= 0 AND attempt_number > 0
    )
);

DO $$
DECLARE
    candidate text;
    target_schema text := 'public';
    candidate_oid oid;
    candidate_rows bigint;
    dependency_count bigint;
BEGIN
    FOREACH candidate IN ARRAY ARRAY[
        'safety_alerts', 'emergency_events', 'safety_events', 'safety_monitoring_settings'
    ] LOOP
        candidate_oid := to_regclass(format('%I.%I', target_schema, candidate));
        IF candidate_oid IS NULL THEN
            RAISE EXCEPTION 'Batch4 preflight failed: required legacy table public.% is missing', candidate;
        END IF;

        EXECUTE format('LOCK TABLE %I.%I IN ACCESS EXCLUSIVE MODE', target_schema, candidate);
        EXECUTE format('SELECT count(*) FROM %I.%I', target_schema, candidate) INTO candidate_rows;
        IF candidate_rows <> 0 THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_SAFETY_MIGRATION: public.% contains % row(s)',
                candidate, candidate_rows;
        END IF;

        SELECT count(*) INTO dependency_count
        FROM pg_constraint c
        WHERE c.contype = 'f'
          AND c.confrelid = candidate_oid
          AND c.conrelid <> ALL (ARRAY[
              'public.safety_alerts'::regclass,
              'public.emergency_events'::regclass,
              'public.safety_events'::regclass,
              'public.safety_monitoring_settings'::regclass
          ]);
        IF dependency_count > 0 THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_SAFETY_MIGRATION: public.% has % retained inbound foreign key(s)',
                candidate, dependency_count;
        END IF;

        SELECT count(DISTINCT dependent_view.oid) INTO dependency_count
        FROM pg_depend d
        JOIN pg_rewrite rewrite ON rewrite.oid = d.objid
        JOIN pg_class dependent_view ON dependent_view.oid = rewrite.ev_class
        WHERE d.refobjid = candidate_oid
          AND dependent_view.relkind IN ('v', 'm');
        IF dependency_count > 0 THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_SAFETY_MIGRATION: public.% has % dependent view(s)',
                candidate, dependency_count;
        END IF;

        SELECT count(*) INTO dependency_count
        FROM pg_trigger trigger_row
        WHERE trigger_row.tgrelid = candidate_oid
          AND NOT trigger_row.tgisinternal;
        IF dependency_count > 0 THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_SAFETY_MIGRATION: public.% has % user trigger(s)',
                candidate, dependency_count;
        END IF;

        SELECT count(*) INTO dependency_count
        FROM pg_policy policy_row
        WHERE policy_row.polrelid = candidate_oid;
        IF dependency_count > 0 THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_SAFETY_MIGRATION: public.% has % RLS policy/policies',
                candidate, dependency_count;
        END IF;

        SELECT count(*) INTO dependency_count
        FROM information_schema.role_table_grants grant_row
        WHERE grant_row.table_schema = target_schema
          AND grant_row.table_name = candidate
          AND grant_row.grantee <> current_user;
        IF dependency_count > 0 THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_SAFETY_MIGRATION: public.% has % external grant(s)',
                candidate, dependency_count;
        END IF;

        SELECT count(DISTINCT procedure_row.oid) INTO dependency_count
        FROM pg_proc procedure_row
        JOIN pg_namespace procedure_namespace ON procedure_namespace.oid = procedure_row.pronamespace
        WHERE procedure_row.prokind IN ('f', 'p')
          AND procedure_namespace.nspname NOT IN ('pg_catalog', 'information_schema')
          AND procedure_namespace.nspname NOT LIKE 'pg_toast%'
          AND pg_get_functiondef(procedure_row.oid)
              ~* format('(^|[^a-zA-Z0-9_])("?public"?[.])?"?%s"?([^a-zA-Z0-9_]|$)', candidate);
        IF dependency_count > 0 THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_SAFETY_MIGRATION: public.% has % function/procedure reference(s)',
                candidate, dependency_count;
        END IF;
    END LOOP;
END $$;

DROP TABLE public.safety_alerts;
DROP TABLE public.emergency_events;
DROP TABLE public.safety_events;
DROP TABLE public.safety_monitoring_settings;
