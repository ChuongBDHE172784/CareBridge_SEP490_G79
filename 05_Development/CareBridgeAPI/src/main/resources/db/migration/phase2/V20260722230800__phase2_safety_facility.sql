-- Phase 2 wave 8: safety runtime and facility/nearby care boundaries.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE TABLE IF NOT EXISTS public.safety_configs (
    safety_config_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL UNIQUE REFERENCES public.users(user_id),
    fall_detection_enabled boolean NOT NULL DEFAULT false,
    sensitivity_level varchar(10) NOT NULL DEFAULT 'MEDIUM',
    emergency_auto_alert boolean NOT NULL DEFAULT true,
    countdown_seconds integer NOT NULL DEFAULT 30 CHECK (countdown_seconds IN (15,30,60)),
    sensor_permission_granted boolean NOT NULL DEFAULT false,
    sensor_permission_recorded_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid REFERENCES public.users(user_id),
    CONSTRAINT safety_configs_sensor_permission_ck CHECK (sensor_permission_granted = false OR sensor_permission_recorded_at IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS public.safety_monitoring_sessions (
    monitoring_session_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(user_id),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    sensitivity_level varchar(10) NOT NULL DEFAULT 'MEDIUM',
    started_at timestamptz NOT NULL DEFAULT now(),
    ended_at timestamptz,
    created_by uuid REFERENCES public.users(user_id)
);
CREATE INDEX IF NOT EXISTS safety_monitoring_sessions_user_status_ix ON public.safety_monitoring_sessions(user_id, status);

CREATE TABLE IF NOT EXISTS public.safety_events (
    safety_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(user_id),
    care_subject_id uuid REFERENCES public.care_subjects(care_subject_id),
    monitoring_session_id uuid REFERENCES public.safety_monitoring_sessions(monitoring_session_id),
    source_event_id uuid,
    detected_at timestamptz NOT NULL,
    event_type varchar(50) NOT NULL,
    confidence_score numeric,
    peak_acceleration numeric,
    angular_velocity numeric,
    inactivity_seconds integer,
    response_type varchar(30),
    response_at timestamptz,
    false_positive_reason text,
    status varchar(20) NOT NULL DEFAULT 'DETECTED',
    location_snapshot_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS safety_events_user_status_time_ix ON public.safety_events(user_id, status, detected_at);

CREATE TABLE IF NOT EXISTS public.safety_event_actions (
    safety_event_action_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    safety_event_id uuid NOT NULL REFERENCES public.safety_events(safety_event_id),
    action_type varchar(40) NOT NULL,
    recipient_user_id uuid REFERENCES public.users(user_id),
    device_identifier varchar(255),
    notification_record_id uuid REFERENCES public.notification_records(id),
    care_facility_id uuid REFERENCES public.care_facilities(facility_id),
    attempt_number integer NOT NULL DEFAULT 1,
    idempotency_key varchar(255) NOT NULL,
    response_type varchar(30),
    delivery_status varchar(30),
    created_at timestamptz NOT NULL DEFAULT now(),
    delivered_at timestamptz,
    CONSTRAINT safety_event_actions_idempotency_uk UNIQUE (idempotency_key),
    CONSTRAINT safety_event_actions_attempt_ck CHECK (attempt_number > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS safety_event_actions_terminal_response_uk
    ON public.safety_event_actions(safety_event_id) WHERE response_type IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS safety_event_actions_delivery_device_uk
    ON public.safety_event_actions(safety_event_id, recipient_user_id, device_identifier)
    WHERE action_type IN ('DELIVERY','FAMILY_ALERT');
CREATE INDEX IF NOT EXISTS safety_event_actions_event_status_ix ON public.safety_event_actions(safety_event_id, delivery_status, created_at);

CREATE TABLE IF NOT EXISTS public.administrative_areas (
    administrative_area_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_area_id uuid REFERENCES public.administrative_areas(administrative_area_id),
    area_type varchar(30) NOT NULL,
    code varchar(80) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    legacy_code varchar(80),
    created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.care_facilities ADD COLUMN IF NOT EXISTS administrative_area_id uuid;
ALTER TABLE public.nearby_support_requests ADD COLUMN IF NOT EXISTS care_subject_id uuid;
ALTER TABLE public.nearby_support_responses ADD COLUMN IF NOT EXISTS professional_profile_id uuid;

DO $safety_facility_mapping$
BEGIN
    IF to_regclass('public.safety_monitoring_config') IS NOT NULL THEN
        INSERT INTO public.safety_configs
            (safety_config_id, user_id, fall_detection_enabled, sensitivity_level,
             emergency_auto_alert, updated_at, updated_by)
        SELECT s.id, s.user_id, s.fall_detection_enabled, s.sensitivity_level,
               s.emergency_auto_alert, coalesce(s.updated_at, now()), s.updated_by
          FROM public.safety_monitoring_config s
        ON CONFLICT (safety_config_id) DO NOTHING;
    END IF;
    IF to_regclass('public.imu_monitoring_sessions') IS NOT NULL THEN
        INSERT INTO public.safety_monitoring_sessions
            (monitoring_session_id, user_id, status, sensitivity_level, started_at, ended_at, created_by)
        SELECT i.id, i.user_id, i.status, i.sensitivity_level, i.started_at, i.ended_at, i.created_by
          FROM public.imu_monitoring_sessions i
        ON CONFLICT (monitoring_session_id) DO NOTHING;
    END IF;
    IF to_regclass('public.imu_safety_events') IS NOT NULL THEN
        INSERT INTO public.safety_events
            (safety_event_id, user_id, monitoring_session_id, source_event_id, detected_at,
             event_type, confidence_score, peak_acceleration, response_type, response_at,
             status, created_at, updated_at)
        SELECT e.id, e.user_id, e.imu_session_id, e.id, e.detected_at, e.event_type,
               e.magnitude, e.magnitude, e.response_type, e.responded_at,
               e.status, e.detected_at, coalesce(e.detected_at, now())
          FROM public.imu_safety_events e
        ON CONFLICT (safety_event_id) DO NOTHING;
    END IF;
    IF to_regclass('public.safety_event_responses') IS NOT NULL THEN
        INSERT INTO public.safety_event_actions
            (safety_event_action_id, safety_event_id, action_type, recipient_user_id,
             attempt_number, idempotency_key, response_type, delivery_status, created_at)
        SELECT r.id, r.safety_event_id, 'RESPONSE', r.owner_user_id,
               1, 'response:' || r.id::text, r.response_type, 'RECORDED', r.responded_at
          FROM public.safety_event_responses r
        ON CONFLICT (safety_event_action_id) DO NOTHING;
    END IF;
    IF to_regclass('public.emergency_alert_deliveries') IS NOT NULL THEN
        INSERT INTO public.safety_event_actions
            (safety_event_id, action_type, recipient_user_id, device_identifier,
             notification_record_id, attempt_number, idempotency_key, delivery_status,
             created_at, delivered_at)
        SELECT e.safety_event_id, 'DELIVERY', d.recipient_user_id, d.device_token_id::text,
               d.notification_record_id, greatest(d.attempt_count, 1), 'delivery:' || d.id::text,
               d.delivery_status, d.created_at, d.delivered_at
          FROM public.emergency_alert_deliveries d
          JOIN public.emergency_sessions s ON s.id = d.emergency_session_id
          JOIN public.safety_events e ON e.user_id = s.user_id
        ON CONFLICT (idempotency_key) DO NOTHING;
    END IF;
END
$safety_facility_mapping$;

CREATE INDEX IF NOT EXISTS care_facilities_area_ix ON public.care_facilities(administrative_area_id);
CREATE INDEX IF NOT EXISTS nearby_support_responses_professional_ix ON public.nearby_support_responses(professional_profile_id);
