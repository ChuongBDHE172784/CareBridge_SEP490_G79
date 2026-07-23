-- Phase 2 wave 4: triage sessions/evidence and knowledge source lifecycle.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE TABLE IF NOT EXISTS public.triage_sessions (
    triage_session_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(user_id),
    care_subject_id uuid REFERENCES public.care_subjects(care_subject_id),
    stage varchar(30),
    profile_context_id uuid,
    risk_level varchar(20),
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    emergency boolean NOT NULL DEFAULT false,
    disclaimer_version varchar(80),
    input_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    result_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    conversation_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    schema_version varchar(30) NOT NULL DEFAULT '1',
    content_hash varchar(128),
    created_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS triage_sessions_user_time_ix ON public.triage_sessions(user_id, created_at);
CREATE INDEX IF NOT EXISTS triage_sessions_risk_ix ON public.triage_sessions(risk_level, emergency, created_at);

CREATE TABLE IF NOT EXISTS public.triage_session_evidence (
    evidence_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    triage_session_id uuid NOT NULL REFERENCES public.triage_sessions(triage_session_id),
    evidence_type varchar(40) NOT NULL,
    claim_code varchar(100),
    claim_text text NOT NULL,
    knowledge_source_id uuid,
    citation_url text,
    citation_domain varchar(255),
    source_version varchar(80),
    source_snapshot_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    content_hash varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT triage_session_evidence_uk UNIQUE (triage_session_id, evidence_type, content_hash)
);
CREATE INDEX IF NOT EXISTS triage_session_evidence_session_ix ON public.triage_session_evidence(triage_session_id);

CREATE TABLE IF NOT EXISTS public.health_context_memories (
    memory_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(user_id),
    care_subject_id uuid REFERENCES public.care_subjects(care_subject_id),
    triage_session_id uuid REFERENCES public.triage_sessions(triage_session_id),
    related_stage varchar(30) NOT NULL,
    summary_text text NOT NULL,
    memory_payload_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz,
    deleted_at timestamptz
);
CREATE INDEX IF NOT EXISTS health_context_memories_subject_expiry_ix
    ON public.health_context_memories(care_subject_id, expires_at) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS public.knowledge_sources (
    knowledge_source_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    domain varchar(255) NOT NULL,
    base_url varchar(500) NOT NULL,
    organization varchar(255) NOT NULL,
    category varchar(40) NOT NULL,
    status varchar(30) NOT NULL,
    discovery_mode varchar(40) NOT NULL,
    applicable_stages text,
    added_by uuid REFERENCES public.users(user_id),
    reviewed_by uuid REFERENCES public.users(user_id),
    reviewed_at timestamptz,
    notes text,
    source_version varchar(80),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS knowledge_sources_domain_status_ix ON public.knowledge_sources(domain, status);

CREATE TABLE IF NOT EXISTS public.knowledge_source_reviews (
    review_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_source_id uuid NOT NULL REFERENCES public.knowledge_sources(knowledge_source_id),
    previous_status varchar(30),
    new_status varchar(30) NOT NULL,
    actor_user_id uuid REFERENCES public.users(user_id),
    actor_role varchar(80),
    notes text,
    changed_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS knowledge_source_reviews_source_time_ix
    ON public.knowledge_source_reviews(knowledge_source_id, changed_at);

ALTER TABLE public.triage_session_evidence
    ADD CONSTRAINT triage_session_evidence_source_fk
    FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(knowledge_source_id);

DO $triage_mapping$
BEGIN
    IF to_regclass('public.intake_sessions') IS NOT NULL THEN
        INSERT INTO public.triage_sessions
            (triage_session_id, user_id, stage, risk_level, status, emergency,
             disclaimer_version, input_jsonb, result_jsonb, created_at, completed_at, updated_at)
        SELECT i.id, i.user_id, NULL, i.risk_level, i.status,
               lower(coalesce(i.risk_level, '')) IN ('red','emergency'),
               i.disclaimer, jsonb_build_object('symptoms', i.symptoms),
               jsonb_build_object('rawAiResponse', i.raw_ai_response),
               i.created_at, i.completed_at, i.created_at
          FROM public.intake_sessions i
        ON CONFLICT (triage_session_id) DO NOTHING;
    END IF;
    IF to_regclass('public.structured_intake_data') IS NOT NULL THEN
        UPDATE public.triage_sessions ts
           SET input_jsonb = ts.input_jsonb || jsonb_build_object(
               'structured', jsonb_build_object(
                   'symptomList', s.symptom_list,
                   'durationDays', s.duration_days,
                   'intensity', s.intensity,
                   'emergencyFlag', s.emergency_flag)),
               emergency = ts.emergency OR s.emergency_flag
          FROM public.structured_intake_data s
         WHERE s.session_id = ts.triage_session_id;
    END IF;
    IF to_regclass('public.evidence_sources') IS NOT NULL THEN
        INSERT INTO public.knowledge_sources
            (knowledge_source_id, domain, base_url, organization, category, status,
             discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at,
             notes, created_at, updated_at)
        SELECT e.id, e.domain, e.base_url, e.organization, e.category, e.status,
               e.discovery_mode, e.applicable_stages, e.added_by, e.reviewed_by,
               e.reviewed_at, e.notes, e.created_at, e.updated_at
          FROM public.evidence_sources e
        ON CONFLICT (knowledge_source_id) DO NOTHING;
    END IF;
    IF to_regclass('public.evidence_source_review_log') IS NOT NULL THEN
        INSERT INTO public.knowledge_source_reviews
            (review_id, knowledge_source_id, previous_status, new_status, actor_user_id,
             actor_role, notes, changed_at)
        SELECT r.id, r.evidence_source_id, r.previous_status, r.new_status,
               r.actor_user_id, r.actor_role, r.notes, r.changed_at
          FROM public.evidence_source_review_log r
        ON CONFLICT (review_id) DO NOTHING;
    END IF;
    IF to_regclass('public.health_memory_entries') IS NOT NULL THEN
        INSERT INTO public.health_context_memories
            (memory_id, user_id, related_stage, summary_text, created_at, expires_at, deleted_at)
        SELECT h.id, h.user_id, h.related_stage, h.summary_text, h.created_at,
               h.expires_at, h.deleted_at
          FROM public.health_memory_entries h
        ON CONFLICT (memory_id) DO NOTHING;
    END IF;
END
$triage_mapping$;

DROP TRIGGER IF EXISTS triage_session_evidence_immutable_trg ON public.triage_session_evidence;
CREATE TRIGGER triage_session_evidence_immutable_trg
BEFORE UPDATE OR DELETE ON public.triage_session_evidence
FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();
