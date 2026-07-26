-- Phase 2 wave 3: community and professional identity.
-- Legacy community/expert tables remain until code-cutover evidence is complete.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE TABLE IF NOT EXISTS public.community_content (
    content_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id uuid REFERENCES public.community_topics(id),
    parent_content_id uuid REFERENCES public.community_content(content_id),
    author_user_id uuid NOT NULL REFERENCES public.users(user_id),
    content_type varchar(20) NOT NULL,
    title varchar(255),
    body text NOT NULL,
    stage varchar(30),
    urgency varchar(20),
    is_anonymous boolean NOT NULL DEFAULT false,
    moderation_status varchar(30) NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT community_content_type_ck CHECK (content_type IN ('QUESTION','ANSWER','POST'))
);
CREATE INDEX IF NOT EXISTS community_content_topic_ix ON public.community_content(topic_id, created_at);
CREATE INDEX IF NOT EXISTS community_content_parent_ix ON public.community_content(parent_content_id);

CREATE TABLE IF NOT EXISTS public.community_interactions (
    interaction_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id uuid NOT NULL REFERENCES public.users(user_id),
    interaction_type varchar(30) NOT NULL,
    content_id uuid REFERENCES public.community_content(content_id),
    topic_id uuid REFERENCES public.community_topics(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT community_interactions_one_target_ck CHECK ((content_id IS NOT NULL) <> (topic_id IS NOT NULL)),
    CONSTRAINT community_interactions_type_ck CHECK (interaction_type IN ('REACTION','BOOKMARK','FOLLOW','MUTE'))
);
CREATE UNIQUE INDEX IF NOT EXISTS community_interactions_unique_target_uk
    ON public.community_interactions(actor_user_id, interaction_type, content_id, topic_id);

DO $community_mapping$
BEGIN
    IF to_regclass('public.community_questions') IS NOT NULL THEN
        INSERT INTO public.community_content
            (content_id, topic_id, author_user_id, content_type, title, body, stage, urgency,
             is_anonymous, moderation_status, created_at, updated_at)
        SELECT q.id, q.topic_id, q.author_id, 'QUESTION', q.title, q.body, q.stage, q.urgency,
               q.is_anonymous, q.status, q.created_at, q.updated_at
          FROM public.community_questions q
        ON CONFLICT (content_id) DO NOTHING;
    END IF;
    IF to_regclass('public.community_answers') IS NOT NULL THEN
        INSERT INTO public.community_content
            (content_id, parent_content_id, author_user_id, content_type, body,
             moderation_status, created_at, updated_at)
        SELECT a.id, a.question_id, a.author_id, 'ANSWER', a.body, a.status, a.created_at, a.updated_at
          FROM public.community_answers a
        ON CONFLICT (content_id) DO NOTHING;
    END IF;
    IF to_regclass('public.community_answer_likes') IS NOT NULL THEN
        INSERT INTO public.community_interactions (actor_user_id, interaction_type, content_id, created_at)
        SELECT l.user_id, 'REACTION', l.answer_id, l.created_at FROM public.community_answer_likes l
        ON CONFLICT DO NOTHING;
    END IF;
    IF to_regclass('public.community_question_likes') IS NOT NULL THEN
        INSERT INTO public.community_interactions (actor_user_id, interaction_type, content_id, created_at)
        SELECT l.user_id, 'REACTION', l.question_id, l.created_at FROM public.community_question_likes l
        ON CONFLICT DO NOTHING;
    END IF;
END
$community_mapping$;

CREATE TABLE IF NOT EXISTS public.professional_profiles (
    professional_profile_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL UNIQUE REFERENCES public.users(user_id),
    professional_title varchar(150),
    workplace varchar(200),
    experience_years smallint,
    consultation_scope text,
    verification_status varchar(30) NOT NULL DEFAULT 'PENDING',
    verified_at timestamptz,
    verified_by uuid REFERENCES public.users(user_id),
    rating_avg numeric,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.specialties (
    specialty_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(80) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    description text,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.professional_specialties (
    professional_profile_id uuid NOT NULL REFERENCES public.professional_profiles(professional_profile_id),
    specialty_id uuid NOT NULL REFERENCES public.specialties(specialty_id),
    is_primary boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (professional_profile_id, specialty_id)
);

CREATE TABLE IF NOT EXISTS public.expert_credentials (
    credential_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_profile_id uuid REFERENCES public.professional_profiles(professional_profile_id),
    credential_type varchar(50) NOT NULL,
    credential_number varchar(100),
    issuer varchar(200),
    issued_date date,
    expiry_date date,
    attachment_id uuid,
    review_status varchar(30) NOT NULL DEFAULT 'PENDING',
    review_note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.expert_credentials ADD COLUMN IF NOT EXISTS professional_profile_id uuid;

CREATE TABLE IF NOT EXISTS public.expert_availability (
    availability_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_profile_id uuid REFERENCES public.professional_profiles(professional_profile_id),
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    channel_type varchar(30) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT expert_availability_window_ck CHECK (end_at > start_at)
);
ALTER TABLE public.expert_availability ADD COLUMN IF NOT EXISTS professional_profile_id uuid;

CREATE TABLE IF NOT EXISTS public.expert_location_shares (
    location_share_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_profile_id uuid REFERENCES public.professional_profiles(professional_profile_id),
    latitude numeric NOT NULL,
    longitude numeric NOT NULL,
    accuracy_meters numeric,
    availability_status varchar(20),
    shared_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz,
    consent_reference uuid,
    created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.expert_location_shares ADD COLUMN IF NOT EXISTS professional_profile_id uuid;

CREATE TABLE IF NOT EXISTS public.expert_contribution_events (
    contribution_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_profile_id uuid REFERENCES public.professional_profiles(professional_profile_id),
    actor_user_id uuid NOT NULL REFERENCES public.users(user_id),
    points integer NOT NULL,
    reason varchar(120) NOT NULL,
    source_type varchar(60),
    source_id uuid,
    created_at timestamptz NOT NULL DEFAULT now()
);

DO $expert_mapping$
BEGIN
    IF to_regclass('public.expert_profiles') IS NOT NULL THEN
        INSERT INTO public.professional_profiles
            (professional_profile_id, user_id, professional_title, workplace, experience_years,
             consultation_scope, verification_status, verified_at, verified_by, rating_avg, created_at, updated_at)
        SELECT expert_profile_id, user_id, professional_title, workplace, experience_years,
               consultation_scope, verification_status, verified_at, verified_by, rating_avg, created_at, updated_at
          FROM public.expert_profiles
        ON CONFLICT (professional_profile_id) DO NOTHING;
    END IF;
    IF to_regclass('public.expert_credentials') IS NOT NULL THEN
        UPDATE public.expert_credentials ec
           SET professional_profile_id = ec.expert_profile_id
         WHERE professional_profile_id IS NULL
           AND EXISTS (SELECT 1 FROM public.professional_profiles pp WHERE pp.professional_profile_id = ec.expert_profile_id);
    END IF;
    IF to_regclass('public.expert_availability') IS NOT NULL THEN
        UPDATE public.expert_availability ea
           SET professional_profile_id = ea.expert_profile_id
         WHERE professional_profile_id IS NULL
           AND EXISTS (SELECT 1 FROM public.professional_profiles pp WHERE pp.professional_profile_id = ea.expert_profile_id);
    END IF;
    IF to_regclass('public.expert_location_shares') IS NOT NULL THEN
        UPDATE public.expert_location_shares els
           SET professional_profile_id = els.expert_profile_id
         WHERE professional_profile_id IS NULL
           AND EXISTS (SELECT 1 FROM public.professional_profiles pp WHERE pp.professional_profile_id = els.expert_profile_id);
    END IF;
END
$expert_mapping$;

CREATE INDEX IF NOT EXISTS professional_specialties_specialty_ix ON public.professional_specialties(specialty_id);
CREATE INDEX IF NOT EXISTS expert_credentials_profile_status_ix ON public.expert_credentials(professional_profile_id, review_status);
CREATE INDEX IF NOT EXISTS expert_availability_profile_window_ix ON public.expert_availability(professional_profile_id, start_at, end_at);
CREATE INDEX IF NOT EXISTS expert_contribution_profile_time_ix ON public.expert_contribution_events(professional_profile_id, created_at);
