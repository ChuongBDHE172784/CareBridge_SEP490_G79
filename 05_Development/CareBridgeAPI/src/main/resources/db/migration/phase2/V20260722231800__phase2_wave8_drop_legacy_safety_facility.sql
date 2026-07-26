-- Phase 2 wave 8 cutover: safety runtime, emergency delivery, location, and facility identities.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.safety_events
  ADD COLUMN IF NOT EXISTS record_type varchar(30) NOT NULL DEFAULT 'IMU_EVENT',
  ADD COLUMN IF NOT EXISTS magnitude numeric(10,4),
  ADD COLUMN IF NOT EXISTS user_latitude numeric(10,8),
  ADD COLUMN IF NOT EXISTS user_longitude numeric(11,8),
  ADD COLUMN IF NOT EXISTS client_detected_at timestamptz,
  ADD COLUMN IF NOT EXISTS resolved_at timestamptz,
  ADD COLUMN IF NOT EXISTS notes text,
  ADD COLUMN IF NOT EXISTS signal_key varchar(200),
  ADD COLUMN IF NOT EXISTS countdown_deadline_at timestamptz,
  ADD COLUMN IF NOT EXISTS response_reason varchar(500),
  ADD COLUMN IF NOT EXISTS escalation_started_at timestamptz,
  ADD COLUMN IF NOT EXISTS emergency_session_id uuid,
  ADD COLUMN IF NOT EXISTS created_by_text varchar(50),
  ADD COLUMN IF NOT EXISTS created_by_user_id uuid;

ALTER TABLE public.safety_events ALTER COLUMN detected_at SET DEFAULT now();

ALTER TABLE public.safety_event_actions
  ALTER COLUMN safety_event_id DROP NOT NULL,
  ADD COLUMN IF NOT EXISTS owner_user_id uuid,
  ADD COLUMN IF NOT EXISTS context_type varchar(50),
  ADD COLUMN IF NOT EXISTS context_id uuid,
  ADD COLUMN IF NOT EXISTS latitude numeric(10,8),
  ADD COLUMN IF NOT EXISTS longitude numeric(11,8),
  ADD COLUMN IF NOT EXISTS accuracy_meters numeric(6,2),
  ADD COLUMN IF NOT EXISTS captured_at timestamptz,
  ADD COLUMN IF NOT EXISTS expires_at timestamptz,
  ADD COLUMN IF NOT EXISTS consent_status varchar(20),
  ADD COLUMN IF NOT EXISTS device_token_id uuid,
  ADD COLUMN IF NOT EXISTS fcm_message_id varchar(255),
  ADD COLUMN IF NOT EXISTS failure_code varchar(120),
  ADD COLUMN IF NOT EXISTS reason varchar(500),
  ADD COLUMN IF NOT EXISTS responded_at timestamptz,
  ADD COLUMN IF NOT EXISTS created_by_user_id uuid,
  ADD COLUMN IF NOT EXISTS actor_type varchar(20),
  ADD COLUMN IF NOT EXISTS attempt_status varchar(20),
  ADD COLUMN IF NOT EXISTS started_at timestamptz,
  ADD COLUMN IF NOT EXISTS completed_at timestamptz,
  ADD COLUMN IF NOT EXISTS lease_expires_at timestamptz,
  ADD COLUMN IF NOT EXISTS successful_recipient_count integer,
  ADD COLUMN IF NOT EXISTS failed_recipient_count integer,
  ADD COLUMN IF NOT EXISTS recipient_count integer,
  ADD COLUMN IF NOT EXISTS location_included boolean,
  ADD COLUMN IF NOT EXISTS created_by_text varchar(50),
  ADD COLUMN IF NOT EXISTS triage_handoff_id uuid,
  ADD COLUMN IF NOT EXISTS risk_level varchar(20),
  ADD COLUMN IF NOT EXISTS summary text,
  ADD COLUMN IF NOT EXISTS action_status varchar(20),
  ADD COLUMN IF NOT EXISTS updated_at timestamptz;

ALTER TABLE public.safety_event_actions
  DROP CONSTRAINT IF EXISTS safety_event_actions_attempt_ck;
ALTER TABLE public.safety_event_actions
  ADD CONSTRAINT safety_event_actions_attempt_ck CHECK (attempt_number >= 0);

DO $wave8_collision_gate$
BEGIN
  IF EXISTS (
    SELECT id FROM public.imu_safety_events
    INTERSECT
    SELECT id FROM public.emergency_sessions
  ) THEN
    RAISE EXCEPTION 'WAVE8_ID_COLLISION: safety events and emergency sessions';
  END IF;

  IF EXISTS (
    SELECT legacy_id FROM (
      SELECT id AS legacy_id FROM public.safety_event_responses
      UNION ALL SELECT id FROM public.emergency_alert_deliveries
      UNION ALL SELECT emergency_session_id FROM public.emergency_alert_attempts
      UNION ALL SELECT id FROM public.family_alert_log
      UNION ALL SELECT handoff_id FROM public.emergency_map_handoffs
      UNION ALL SELECT location_snapshot_id FROM public.location_snapshots
    ) ids GROUP BY legacy_id HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION 'WAVE8_ID_COLLISION: safety action source identifiers';
  END IF;

  IF EXISTS (
    SELECT 1 FROM public.care_facility_legacy_ids m
    LEFT JOIN public.care_facilities f ON f.facility_id=m.facility_id
    WHERE f.facility_id IS NULL OR f.source_type <> 'LEGACY_IMPORT'
       OR f.external_source_id IS DISTINCT FROM m.legacy_id
  ) OR EXISTS (
    SELECT facility_id FROM public.care_facility_legacy_ids
    GROUP BY facility_id HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION 'WAVE8_FACILITY_IDENTITY_RECONCILIATION';
  END IF;
END $wave8_collision_gate$;

INSERT INTO public.safety_configs (
    safety_config_id,user_id,fall_detection_enabled,sensitivity_level,
    emergency_auto_alert,countdown_seconds,sensor_permission_granted,
    sensor_permission_recorded_at,updated_at,updated_by)
SELECT id,user_id,fall_detection_enabled,sensitivity_level,emergency_auto_alert,
       countdown_seconds,sensor_permission_granted,sensor_permission_recorded_at,
       coalesce(updated_at,now()),updated_by
  FROM public.safety_monitoring_config
ON CONFLICT (safety_config_id) DO UPDATE SET
  user_id=excluded.user_id,fall_detection_enabled=excluded.fall_detection_enabled,
  sensitivity_level=excluded.sensitivity_level,
  emergency_auto_alert=excluded.emergency_auto_alert,
  countdown_seconds=excluded.countdown_seconds,
  sensor_permission_granted=excluded.sensor_permission_granted,
  sensor_permission_recorded_at=excluded.sensor_permission_recorded_at,
  updated_at=excluded.updated_at,updated_by=excluded.updated_by;

INSERT INTO public.safety_monitoring_sessions (
    monitoring_session_id,user_id,status,sensitivity_level,started_at,ended_at,created_by)
SELECT id,user_id,status,sensitivity_level,started_at,ended_at,created_by
  FROM public.imu_monitoring_sessions
ON CONFLICT (monitoring_session_id) DO UPDATE SET
  user_id=excluded.user_id,status=excluded.status,
  sensitivity_level=excluded.sensitivity_level,started_at=excluded.started_at,
  ended_at=excluded.ended_at,created_by=excluded.created_by;

INSERT INTO public.safety_events (
    safety_event_id,record_type,user_id,monitoring_session_id,source_event_id,
    detected_at,event_type,magnitude,confidence_score,peak_acceleration,
    user_latitude,user_longitude,client_detected_at,status,resolved_at,notes,
    signal_key,countdown_deadline_at,response_type,response_reason,response_at,
    escalation_started_at,emergency_session_id,created_by_text,created_at,updated_at)
SELECT id,'IMU_EVENT',user_id,imu_session_id,id,detected_at,event_type,magnitude,
       magnitude,magnitude,user_latitude,user_longitude,client_detected_at,status,
       resolved_at,notes,signal_key,countdown_deadline_at,response_type,response_reason,
       responded_at,escalation_started_at,emergency_session_id,created_by,
       detected_at,coalesce(resolved_at,responded_at,detected_at)
  FROM public.imu_safety_events
ON CONFLICT (safety_event_id) DO UPDATE SET
  record_type='IMU_EVENT',user_id=excluded.user_id,
  monitoring_session_id=excluded.monitoring_session_id,detected_at=excluded.detected_at,
  event_type=excluded.event_type,magnitude=excluded.magnitude,
  confidence_score=excluded.confidence_score,peak_acceleration=excluded.peak_acceleration,
  user_latitude=excluded.user_latitude,user_longitude=excluded.user_longitude,
  client_detected_at=excluded.client_detected_at,status=excluded.status,
  resolved_at=excluded.resolved_at,notes=excluded.notes,signal_key=excluded.signal_key,
  countdown_deadline_at=excluded.countdown_deadline_at,response_type=excluded.response_type,
  response_reason=excluded.response_reason,response_at=excluded.response_at,
  escalation_started_at=excluded.escalation_started_at,
  emergency_session_id=excluded.emergency_session_id,created_by_text=excluded.created_by_text,
  updated_at=excluded.updated_at;

INSERT INTO public.safety_events (
    safety_event_id,record_type,user_id,source_event_id,detected_at,event_type,
    user_latitude,user_longitude,status,resolved_at,created_by_user_id,created_at,updated_at)
SELECT id,'EMERGENCY_SESSION',user_id,id,created_at,trigger_source,user_latitude,
       user_longitude,status,resolved_at,created_by,created_at,coalesce(resolved_at,created_at)
  FROM public.emergency_sessions
ON CONFLICT (safety_event_id) DO UPDATE SET
  record_type='EMERGENCY_SESSION',user_id=excluded.user_id,detected_at=excluded.detected_at,
  event_type=excluded.event_type,user_latitude=excluded.user_latitude,
  user_longitude=excluded.user_longitude,status=excluded.status,
  resolved_at=excluded.resolved_at,created_by_user_id=excluded.created_by_user_id,
  created_at=excluded.created_at,updated_at=excluded.updated_at;

INSERT INTO public.safety_event_actions (
    safety_event_action_id,safety_event_id,action_type,owner_user_id,recipient_user_id,
    attempt_number,idempotency_key,response_type,delivery_status,reason,responded_at,
    created_by_user_id,actor_type,created_at)
SELECT id,safety_event_id,'RESPONSE',owner_user_id,owner_user_id,1,
       'response:'||id::text,response_type,'RECORDED',reason,responded_at,
       created_by,actor_type,responded_at
  FROM public.safety_event_responses
ON CONFLICT (safety_event_action_id) DO UPDATE SET
  safety_event_id=excluded.safety_event_id,action_type='RESPONSE',
  owner_user_id=excluded.owner_user_id,recipient_user_id=excluded.recipient_user_id,
  response_type=excluded.response_type,delivery_status=excluded.delivery_status,
  reason=excluded.reason,responded_at=excluded.responded_at,
  created_by_user_id=excluded.created_by_user_id,actor_type=excluded.actor_type,
  created_at=excluded.created_at;

-- Remove the provisional Wave 8 delivery mapping, which did not yet have the
-- canonical emergency-session event available, then restore the exact source identity.
DELETE FROM public.safety_event_actions a
USING public.emergency_alert_deliveries d
WHERE a.idempotency_key='delivery:'||d.id::text;

INSERT INTO public.safety_event_actions (
    safety_event_action_id,safety_event_id,action_type,recipient_user_id,
    device_identifier,device_token_id,notification_record_id,attempt_number,
    idempotency_key,delivery_status,fcm_message_id,failure_code,created_at,delivered_at)
SELECT id,emergency_session_id,'DELIVERY',recipient_user_id,device_token_id::text,
       device_token_id,notification_record_id,attempt_count,'delivery:'||id::text,
       delivery_status,fcm_message_id,failure_code,created_at,delivered_at
  FROM public.emergency_alert_deliveries;

INSERT INTO public.safety_event_actions (
    safety_event_action_id,safety_event_id,action_type,attempt_number,idempotency_key,
    attempt_status,started_at,completed_at,lease_expires_at,
    successful_recipient_count,failed_recipient_count,created_at,updated_at)
SELECT emergency_session_id,emergency_session_id,'ALERT_ATTEMPT',attempt_number,
       'attempt:'||emergency_session_id::text,status,started_at,completed_at,
       lease_expires_at,successful_recipient_count,failed_recipient_count,started_at,updated_at
  FROM public.emergency_alert_attempts
ON CONFLICT (safety_event_action_id) DO UPDATE SET
  safety_event_id=excluded.safety_event_id,action_type='ALERT_ATTEMPT',
  attempt_number=excluded.attempt_number,attempt_status=excluded.attempt_status,
  started_at=excluded.started_at,completed_at=excluded.completed_at,
  lease_expires_at=excluded.lease_expires_at,
  successful_recipient_count=excluded.successful_recipient_count,
  failed_recipient_count=excluded.failed_recipient_count,updated_at=excluded.updated_at;

INSERT INTO public.safety_event_actions (
    safety_event_action_id,safety_event_id,action_type,attempt_number,idempotency_key,
    recipient_count,location_included,created_by_text,created_at,delivered_at)
SELECT id,session_id,'FAMILY_ALERT',1,'family-alert:'||id::text,recipient_count,
       location_included,created_by,sent_at,sent_at
  FROM public.family_alert_log
ON CONFLICT (safety_event_action_id) DO NOTHING;

INSERT INTO public.safety_event_actions (
    safety_event_action_id,action_type,owner_user_id,attempt_number,idempotency_key,
    triage_handoff_id,risk_level,latitude,longitude,care_facility_id,summary,
    action_status,created_at,updated_at)
SELECT handoff_id,'MAP_HANDOFF',user_id,1,'map-handoff:'||handoff_id::text,
       triage_handoff_id,risk_level,user_latitude,user_longitude,selected_facility_id,
       summary,status,created_at,updated_at
  FROM public.emergency_map_handoffs
ON CONFLICT (safety_event_action_id) DO NOTHING;

INSERT INTO public.safety_event_actions (
    safety_event_action_id,action_type,owner_user_id,attempt_number,idempotency_key,
    context_type,context_id,latitude,longitude,accuracy_meters,captured_at,expires_at,
    consent_status,created_at)
SELECT location_snapshot_id,'LOCATION_SNAPSHOT',user_id,1,
       'location:'||location_snapshot_id::text,context_type,context_id,latitude,
       longitude,accuracy_meters,captured_at,expires_at,consent_status,captured_at
  FROM public.location_snapshots
ON CONFLICT (safety_event_action_id) DO NOTHING;

DO $wave8_reconcile$
BEGIN
  IF (SELECT count(*) FROM public.safety_monitoring_config) <>
     (SELECT count(*) FROM public.safety_configs) THEN
    RAISE EXCEPTION 'WAVE8_RECONCILIATION: safety configs';
  END IF;
  IF (SELECT count(*) FROM public.imu_monitoring_sessions) <>
     (SELECT count(*) FROM public.safety_monitoring_sessions) THEN
    RAISE EXCEPTION 'WAVE8_RECONCILIATION: monitoring sessions';
  END IF;
  IF (SELECT count(*) FROM public.imu_safety_events) <>
     (SELECT count(*) FROM public.safety_events WHERE record_type='IMU_EVENT') THEN
    RAISE EXCEPTION 'WAVE8_RECONCILIATION: IMU events';
  END IF;
  IF (SELECT count(*) FROM public.emergency_sessions) <>
     (SELECT count(*) FROM public.safety_events WHERE record_type='EMERGENCY_SESSION') THEN
    RAISE EXCEPTION 'WAVE8_RECONCILIATION: emergency sessions';
  END IF;
  IF (SELECT count(*) FROM public.safety_event_responses) <>
     (SELECT count(*) FROM public.safety_event_actions WHERE action_type='RESPONSE') OR
     (SELECT count(*) FROM public.emergency_alert_deliveries) <>
     (SELECT count(*) FROM public.safety_event_actions WHERE action_type='DELIVERY') OR
     (SELECT count(*) FROM public.emergency_alert_attempts) <>
     (SELECT count(*) FROM public.safety_event_actions WHERE action_type='ALERT_ATTEMPT') OR
     (SELECT count(*) FROM public.family_alert_log) <>
     (SELECT count(*) FROM public.safety_event_actions WHERE action_type='FAMILY_ALERT') OR
     (SELECT count(*) FROM public.emergency_map_handoffs) <>
     (SELECT count(*) FROM public.safety_event_actions WHERE action_type='MAP_HANDOFF') OR
     (SELECT count(*) FROM public.location_snapshots) <>
     (SELECT count(*) FROM public.safety_event_actions WHERE action_type='LOCATION_SNAPSHOT') THEN
    RAISE EXCEPTION 'WAVE8_RECONCILIATION: safety actions';
  END IF;

  IF EXISTS (SELECT 1 FROM public.safety_event_actions a
             LEFT JOIN public.safety_events e ON e.safety_event_id=a.safety_event_id
             WHERE a.safety_event_id IS NOT NULL AND e.safety_event_id IS NULL) OR
     EXISTS (SELECT 1 FROM public.safety_events e
             LEFT JOIN public.safety_monitoring_sessions s
               ON s.monitoring_session_id=e.monitoring_session_id
             WHERE e.monitoring_session_id IS NOT NULL AND s.monitoring_session_id IS NULL) THEN
    RAISE EXCEPTION 'WAVE8_ORPHAN_TARGET';
  END IF;
END $wave8_reconcile$;

DO $wave8_dependency_gate$
DECLARE source_oid oid; source_name text; dependency_count bigint;
BEGIN
  FOREACH source_name IN ARRAY ARRAY[
    'care_facility_legacy_ids','emergency_alert_attempts','emergency_alert_deliveries',
    'emergency_map_handoffs','emergency_sessions','family_alert_log',
    'imu_monitoring_sessions','imu_safety_events','location_snapshots',
    'safety_event_responses','safety_monitoring_config'
  ] LOOP
    source_oid := to_regclass('public.'||source_name);
    SELECT count(*) INTO dependency_count FROM pg_constraint
     WHERE contype='f' AND confrelid=source_oid
       AND conrelid <> ALL (ARRAY[
         'public.emergency_alert_attempts'::regclass,
         'public.emergency_alert_deliveries'::regclass,
         'public.family_alert_log'::regclass,
         'public.imu_safety_events'::regclass,
         'public.safety_event_responses'::regclass
       ]);
    IF dependency_count > 0 THEN
      RAISE EXCEPTION 'WAVE8_RETAINED_INBOUND_FK: % has %',source_name,dependency_count;
    END IF;
    SELECT count(DISTINCT v.oid) INTO dependency_count
      FROM pg_depend d JOIN pg_rewrite r ON r.oid=d.objid
      JOIN pg_class v ON v.oid=r.ev_class
     WHERE d.refobjid=source_oid AND v.relkind IN ('v','m');
    IF dependency_count > 0 THEN
      RAISE EXCEPTION 'WAVE8_DEPENDENT_VIEW: % has %',source_name,dependency_count;
    END IF;
  END LOOP;
END $wave8_dependency_gate$;

CREATE UNIQUE INDEX IF NOT EXISTS safety_events_imu_signal_uk
  ON public.safety_events(monitoring_session_id,signal_key)
  WHERE record_type='IMU_EVENT' AND signal_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS safety_events_pending_countdown_ix
  ON public.safety_events(countdown_deadline_at)
  WHERE record_type='IMU_EVENT' AND status='OPEN' AND response_type IS NULL;
CREATE INDEX IF NOT EXISTS safety_event_actions_owner_location_ix
  ON public.safety_event_actions(owner_user_id,captured_at DESC)
  WHERE action_type='LOCATION_SNAPSHOT';
CREATE INDEX IF NOT EXISTS safety_event_actions_handoff_status_ix
  ON public.safety_event_actions(action_status,created_at DESC)
  WHERE action_type='MAP_HANDOFF';
CREATE UNIQUE INDEX IF NOT EXISTS safety_event_actions_delivery_token_uk
  ON public.safety_event_actions(safety_event_id,device_token_id)
  WHERE action_type='DELIVERY';
CREATE UNIQUE INDEX IF NOT EXISTS safety_event_actions_family_alert_uk
  ON public.safety_event_actions(safety_event_id)
  WHERE action_type='FAMILY_ALERT';
CREATE UNIQUE INDEX IF NOT EXISTS safety_event_actions_attempt_event_uk
  ON public.safety_event_actions(safety_event_id)
  WHERE action_type='ALERT_ATTEMPT';

ALTER TABLE public.safety_events
  ADD CONSTRAINT safety_events_emergency_session_fk
  FOREIGN KEY (emergency_session_id) REFERENCES public.safety_events(safety_event_id),
  ADD CONSTRAINT safety_events_created_by_user_fk
  FOREIGN KEY (created_by_user_id) REFERENCES public.users(user_id),
  ADD CONSTRAINT safety_events_record_type_ck
  CHECK (record_type IN ('IMU_EVENT','EMERGENCY_SESSION'));
ALTER TABLE public.safety_event_actions
  ADD CONSTRAINT safety_event_actions_owner_fk
  FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id),
  ADD CONSTRAINT safety_event_actions_device_token_fk
  FOREIGN KEY (device_token_id) REFERENCES public.device_tokens(id),
  ADD CONSTRAINT safety_event_actions_created_by_user_fk
  FOREIGN KEY (created_by_user_id) REFERENCES public.users(user_id),
  ADD CONSTRAINT safety_event_actions_type_ck
  CHECK (action_type IN ('RESPONSE','DELIVERY','FAMILY_ALERT','ALERT_ATTEMPT',
                         'MAP_HANDOFF','LOCATION_SNAPSHOT')),
  ADD CONSTRAINT safety_event_actions_parent_ck
  CHECK (action_type IN ('MAP_HANDOFF','LOCATION_SNAPSHOT') OR safety_event_id IS NOT NULL);

DROP TABLE public.safety_event_responses;
DROP TABLE public.emergency_alert_deliveries;
DROP TABLE public.emergency_alert_attempts;
DROP TABLE public.family_alert_log;
DROP TABLE public.imu_safety_events;
DROP TABLE public.emergency_map_handoffs;
DROP TABLE public.location_snapshots;
DROP TABLE public.safety_monitoring_config;
DROP TABLE public.imu_monitoring_sessions;
DROP TABLE public.care_facility_legacy_ids;
DROP TABLE public.emergency_sessions;

DO $wave8_absence_gate$
DECLARE name text;
BEGIN
  FOREACH name IN ARRAY ARRAY[
    'care_facility_legacy_ids','emergency_alert_attempts','emergency_alert_deliveries',
    'emergency_map_handoffs','emergency_sessions','family_alert_log',
    'imu_monitoring_sessions','imu_safety_events','location_snapshots',
    'safety_event_responses','safety_monitoring_config'
  ] LOOP
    IF to_regclass('public.'||name) IS NOT NULL THEN
      RAISE EXCEPTION 'WAVE8_DROP_FAILED: %',name;
    END IF;
  END LOOP;
END $wave8_absence_gate$;
