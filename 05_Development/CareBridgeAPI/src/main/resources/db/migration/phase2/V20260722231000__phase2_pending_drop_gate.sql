-- Phase 2 wave 10: pending-drop evidence gate.
-- This migration deliberately performs no DROP. Runtime references and retention
-- decisions are not proven in database bootstrap, so all candidates remain blocked.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

DO $pending_drop_gate$
DECLARE
    candidate text;
    candidate_oid oid;
    row_count bigint;
    inbound_fk_count bigint;
BEGIN
    FOREACH candidate IN ARRAY ARRAY[
        'commission_config','commission_records','consultation_disputes','consultation_messages',
        'consultation_requests','contribution_attachments','emergency_events',
        'expert_identity_verifications','expert_reviews','expert_verification_documents',
        'impact_assessment_ratings','location_snapshots','medical_contributions',
        'partner_expert_links','partner_services','payment_transactions','refund_records',
        'roles','safety_alerts','safety_monitoring_settings','settlement_records',
        'sponsored_campaigns','triage_answers','triage_assessments','user_roles'
    ] LOOP
        candidate_oid := to_regclass('public.' || candidate);
        IF candidate_oid IS NULL THEN
            RAISE NOTICE 'BLOCKED_DROP %. table absent on this bootstrap; no destructive action', candidate;
        ELSE
            EXECUTE format('SELECT count(*) FROM public.%I', candidate) INTO row_count;
            SELECT count(*) INTO inbound_fk_count
              FROM pg_constraint c
             WHERE c.contype = 'f' AND c.confrelid = candidate_oid;
            RAISE NOTICE 'BLOCKED_DROP %. rows=%, inbound_fk=%, runtime_scan=REQUIRED, retention_decision=REQUIRED',
                candidate, row_count, inbound_fk_count;
        END IF;
    END LOOP;
END
$pending_drop_gate$;
