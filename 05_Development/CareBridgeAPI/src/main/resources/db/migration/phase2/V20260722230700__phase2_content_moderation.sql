-- Phase 2 wave 7: verified content relations and append-only moderation history.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE TABLE IF NOT EXISTS public.content_item_topics (
    content_item_id uuid NOT NULL REFERENCES public.content_items(content_item_id),
    topic_id uuid NOT NULL REFERENCES public.community_topics(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (content_item_id, topic_id)
);

CREATE TABLE IF NOT EXISTS public.content_item_sources (
    content_item_source_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    content_item_id uuid NOT NULL REFERENCES public.content_items(content_item_id),
    knowledge_source_id uuid REFERENCES public.knowledge_sources(knowledge_source_id),
    source_title varchar(500) NOT NULL,
    source_url varchar(2000),
    source_publisher varchar(255),
    source_snapshot_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT content_item_sources_unique_url_uk UNIQUE (content_item_id, source_url)
);

CREATE TABLE IF NOT EXISTS public.moderation_cases (
    moderation_case_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_user_id uuid REFERENCES public.users(user_id),
    assigned_moderator_id uuid REFERENCES public.users(user_id),
    target_type varchar(40) NOT NULL,
    target_id uuid NOT NULL,
    reason_code varchar(80),
    description text,
    status varchar(30) NOT NULL DEFAULT 'OPEN',
    opened_at timestamptz NOT NULL DEFAULT now(),
    resolved_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS moderation_cases_target_ix ON public.moderation_cases(target_type, target_id, status);

DO $content_mapping$
BEGIN
    IF to_regclass('public.content_items') IS NOT NULL THEN
        INSERT INTO public.content_item_topics (content_item_id, topic_id)
        SELECT c.content_item_id, c.topic_id FROM public.content_items c WHERE c.topic_id IS NOT NULL
        ON CONFLICT DO NOTHING;
    END IF;

    IF to_regclass('public.content_sources') IS NOT NULL THEN
        INSERT INTO public.content_item_sources
            (content_item_id, source_title, source_url, source_publisher)
        SELECT s.content_item_id, s.source_title, s.source_url, s.source_publisher
          FROM public.content_sources s
        ON CONFLICT DO NOTHING;
    END IF;

    IF to_regclass('public.content_reports') IS NOT NULL THEN
        INSERT INTO public.moderation_cases
            (moderation_case_id, reporter_user_id, assigned_moderator_id, target_type, target_id,
             reason_code, description, status, opened_at, resolved_at, updated_at)
        SELECT r.report_id, r.reporter_user_id, r.assigned_moderator_id,
               coalesce(r.target_type, 'CONTENT'), r.target_id, r.category,
               r.description, r.status, r.created_at, r.resolved_at, coalesce(r.updated_at, r.created_at)
          FROM public.content_reports r
        ON CONFLICT (moderation_case_id) DO NOTHING;
    END IF;

    IF to_regclass('public.moderation_actions') IS NOT NULL THEN
        INSERT INTO public.audit_events (
            audit_event_id, actor_user_id, event_category, subject_reference_id,
            resource_type, resource_id, payload, occurred_at, created_at,
            severity, status
        )
        SELECT m.moderation_action_id, m.moderator_user_id, 'MODERATION_' || coalesce(m.action_type, 'REVIEW'),
               m.report_id, coalesce(m.target_type, 'CONTENT'), m.target_id,
               jsonb_build_object(
                   'reason', m.reason,
                   'expiresAt', m.expires_at,
                   'legacySource', 'moderation_actions'
               ), coalesce(m.action_at, now()), coalesce(m.action_at, now()),
               'HIGH', 'CLOSED'
          FROM public.moderation_actions m
         WHERE m.report_id IS NOT NULL
           AND EXISTS (SELECT 1 FROM public.moderation_cases c WHERE c.moderation_case_id = m.report_id)
        ON CONFLICT (audit_event_id) DO NOTHING;
    END IF;
END
$content_mapping$;
