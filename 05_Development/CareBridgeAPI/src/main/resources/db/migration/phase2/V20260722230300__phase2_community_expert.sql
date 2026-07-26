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

ALTER TABLE public.users ADD COLUMN IF NOT EXISTS professional_title varchar(150);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS workplace varchar(200);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS experience_years smallint;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS consultation_scope text;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS verification_status varchar(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS verified_at timestamptz;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS verified_by uuid REFERENCES public.users(user_id);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS rating_avg numeric;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS specialty_ids uuid[];

CREATE TABLE IF NOT EXISTS public.specialties (
    specialty_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(80) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    description text,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.expert_credentials (
    credential_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid REFERENCES public.users(user_id),
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
ALTER TABLE public.expert_credentials ADD COLUMN IF NOT EXISTS user_id uuid;

CREATE TABLE IF NOT EXISTS public.expert_availability (
    availability_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid REFERENCES public.users(user_id),
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    channel_type varchar(30) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT expert_availability_window_ck CHECK (end_at > start_at)
);
ALTER TABLE public.expert_availability ADD COLUMN IF NOT EXISTS user_id uuid;

CREATE TABLE IF NOT EXISTS public.expert_location_shares (
    location_share_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid REFERENCES public.users(user_id),
    latitude numeric NOT NULL,
    longitude numeric NOT NULL,
    accuracy_meters numeric,
    availability_status varchar(20),
    shared_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz,
    consent_reference uuid,
    created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.expert_location_shares ADD COLUMN IF NOT EXISTS user_id uuid;

DO $expert_mapping$
BEGIN
    IF to_regclass('public.expert_profiles') IS NOT NULL THEN
        UPDATE public.users u
           SET professional_title = ep.professional_title,
               workplace = ep.workplace,
               experience_years = ep.experience_years,
               consultation_scope = ep.consultation_scope,
               verification_status = ep.verification_status,
               verified_at = ep.verified_at,
               verified_by = ep.verified_by,
               rating_avg = ep.rating_avg
          FROM public.expert_profiles ep
         WHERE u.user_id = ep.user_id;
    END IF;

    IF to_regclass('public.expert_credentials') IS NOT NULL THEN
        UPDATE public.expert_credentials ec
           SET user_id = ep.user_id
          FROM public.expert_profiles ep
         WHERE ec.expert_profile_id = ep.expert_profile_id;
    END IF;

    IF to_regclass('public.expert_availability') IS NOT NULL THEN
        UPDATE public.expert_availability ea
           SET user_id = ep.user_id
          FROM public.expert_profiles ep
         WHERE ea.expert_profile_id = ep.expert_profile_id;
    END IF;

    IF to_regclass('public.expert_location_shares') IS NOT NULL THEN
        UPDATE public.expert_location_shares els
           SET user_id = ep.user_id
          FROM public.expert_profiles ep
         WHERE els.expert_profile_id = ep.expert_profile_id;
    END IF;

    IF to_regclass('public.expert_specialties') IS NOT NULL THEN
        UPDATE public.users u
           SET specialty_ids = x.spec_ids
          FROM (
              SELECT ep.user_id, array_agg(es.specialty_id) as spec_ids
                FROM public.expert_specialties es
                JOIN public.expert_profiles ep ON ep.expert_profile_id = es.expert_profile_id
               GROUP BY ep.user_id
          ) x
         WHERE u.user_id = x.user_id;
    END IF;
END
$expert_mapping$;

CREATE INDEX IF NOT EXISTS expert_credentials_user_status_ix ON public.expert_credentials(user_id, review_status);
CREATE INDEX IF NOT EXISTS expert_availability_user_window_ix ON public.expert_availability(user_id, start_at, end_at);
