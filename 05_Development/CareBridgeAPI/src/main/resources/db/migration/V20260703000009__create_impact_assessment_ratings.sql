-- =============================================================================
-- V20260703000009__create_impact_assessment_ratings.sql
-- Purpose: Create impact_assessment_ratings table missing from earlier migrations
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.impact_assessment_ratings (
    rating_id       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL,
    content_id      uuid,
    rating_value    numeric(3,2),
    feedback_text   text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_impact_assessment_ratings_user_id
    ON public.impact_assessment_ratings (user_id);

CREATE INDEX IF NOT EXISTS idx_impact_assessment_ratings_content_id
    ON public.impact_assessment_ratings (content_id);
