-- Consolidate content and community lifecycle stages to:
-- PRE_PREGNANCY, PREGNANCY, POSTPARTUM.

UPDATE public.content_items
SET stage = 'POSTPARTUM'
WHERE stage = 'BABY_CARE';

UPDATE public.care_item_templates
SET stage = 'POSTPARTUM'
WHERE stage = 'BABY_CARE';

UPDATE public.community_content
SET stage = 'POSTPARTUM'
WHERE stage = 'BABY_CARE';

UPDATE public.users
SET interest_stage = 'POSTPARTUM',
    updated_at = now()
WHERE interest_stage = 'BABY_CARE';

-- Keep stable IDs for the three canonical lifecycle categories. The existing
-- postpartum category becomes the combined postpartum and baby-care category.
UPDATE public.community_topics
SET name = 'Chuẩn bị mang thai',
    description = 'Chuẩn bị sức khoẻ, tâm lý trước khi mang thai',
    updated_at = now()
WHERE id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567801';

UPDATE public.community_topics
SET name = 'Thai kỳ',
    description = 'Chăm sóc và theo dõi trong thai kỳ',
    updated_at = now()
WHERE id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567802';

UPDATE public.community_topics
SET name = 'Hậu sản & Chăm bé',
    description = 'Hồi phục sau sinh và chăm sóc bé',
    updated_at = now()
WHERE id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567803';

-- Move baby-care child topics and direct content references under the combined
-- postpartum category before removing the obsolete fourth category.
UPDATE public.community_topics
SET parent_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567803',
    updated_at = now()
WHERE parent_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804';

UPDATE public.community_content
SET topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567803',
    updated_at = now()
WHERE topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804';

-- Content items also keep a direct category UUID. This column is intentionally
-- not foreign-keyed, so it must be repointed explicitly before the old category
-- disappears.
UPDATE public.content_items
SET topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567803',
    updated_at = now()
WHERE topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804';

-- A user may already follow both legacy categories. Remove only the obsolete
-- duplicate target before repointing the remaining interactions.
DELETE FROM public.community_interactions obsolete
USING public.community_interactions canonical
WHERE obsolete.topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804'
  AND canonical.topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567803'
  AND canonical.actor_user_id = obsolete.actor_user_id
  AND canonical.interaction_type = obsolete.interaction_type;

UPDATE public.community_interactions
SET topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567803'
WHERE topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804';

DELETE FROM public.community_topics
WHERE id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM public.content_items WHERE stage = 'BABY_CARE'
        UNION ALL
        SELECT 1 FROM public.care_item_templates WHERE stage = 'BABY_CARE'
        UNION ALL
        SELECT 1 FROM public.community_content WHERE stage = 'BABY_CARE'
        UNION ALL
        SELECT 1 FROM public.users WHERE interest_stage = 'BABY_CARE'
    ) THEN
        RAISE EXCEPTION 'BABY_CARE content/community stage consolidation was incomplete';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.community_topics
        WHERE id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804'
           OR parent_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804'
        UNION ALL
        SELECT 1 FROM public.community_content
        WHERE topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804'
        UNION ALL
        SELECT 1 FROM public.content_items
        WHERE topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804'
        UNION ALL
        SELECT 1 FROM public.community_interactions
        WHERE topic_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804'
    ) THEN
        RAISE EXCEPTION 'Obsolete baby-care category still has references';
    END IF;
END $$;
