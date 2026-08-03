-- Extend the V1 recommendation vocabulary for the approved nine-group questionnaire.
-- Existing slugs and IDs remain unchanged; this migration is forward-only.

CREATE TEMP TABLE recommendation_tag_catalog_v1_extension (
    slug character varying(140) PRIMARY KEY,
    domain_name character varying(40) NOT NULL,
    label character varying(100) NOT NULL,
    ordinal smallint NOT NULL
) ON COMMIT DROP;

INSERT INTO recommendation_tag_catalog_v1_extension (slug, domain_name, label, ordinal) VALUES
    ('rec-reproductive-recurrent-pregnancy-loss', 'REPRODUCTIVE_HISTORY', 'Reproductive history: recurrent pregnancy loss', 80),
    ('rec-reproductive-ectopic-pregnancy', 'REPRODUCTIVE_HISTORY', 'Reproductive history: ectopic pregnancy', 81),
    ('rec-reproductive-preeclampsia', 'REPRODUCTIVE_HISTORY', 'Reproductive history: preeclampsia', 82),
    ('rec-reproductive-gestational-diabetes', 'REPRODUCTIVE_HISTORY', 'Reproductive history: gestational diabetes', 83),
    ('rec-condition-lupus', 'UNDERLYING_CONDITION', 'Condition: lupus', 85),
    ('rec-condition-anemia', 'UNDERLYING_CONDITION', 'Condition: anemia', 86),
    ('rec-condition-pcos', 'UNDERLYING_CONDITION', 'Condition: PCOS', 87),
    ('rec-condition-endometriosis', 'UNDERLYING_CONDITION', 'Condition: endometriosis', 88),
    ('rec-condition-infertility', 'UNDERLYING_CONDITION', 'Condition: infertility', 89),
    ('rec-alcohol-use', 'LIFESTYLE', 'Alcohol: any use', 90),
    ('rec-lifestyle-substance-use', 'LIFESTYLE', 'Lifestyle: substance use', 91),
    ('rec-lifestyle-stress', 'LIFESTYLE', 'Lifestyle: stress', 92),
    ('rec-lifestyle-unhealthy-diet', 'LIFESTYLE', 'Lifestyle: unhealthy diet', 93),
    ('rec-nutrition-folic-acid-needed', 'NUTRITION', 'Nutrition: folic acid not started', 94),
    ('rec-nutrition-iodine-review', 'NUTRITION', 'Nutrition: iodine assessment needed', 95),
    ('rec-nutrition-vitamin-d-review', 'NUTRITION', 'Nutrition: vitamin D review', 96),
    ('rec-nutrition-iron-review', 'NUTRITION', 'Nutrition: iron review', 97),
    ('rec-nutrition-calcium-review', 'NUTRITION', 'Nutrition: calcium review', 98),
    ('rec-vaccination-assessment-needed', 'VACCINATION', 'Vaccination: assessment needed', 99),
    ('rec-medication-high-risk-or-contraindicated', 'CURRENT_MEDICATION', 'Medication: high risk or contraindicated', 100),
    ('rec-medication-adjustment-needed', 'CURRENT_MEDICATION', 'Medication: adjustment needed', 101),
    ('rec-sexual-health-safe-sex-counseling', 'SEXUAL_HEALTH', 'Sexual health: safe-sex counseling', 102),
    ('rec-sexual-health-sti-risk', 'SEXUAL_HEALTH', 'Sexual health: STI risk', 103),
    ('rec-sexual-health-reproductive-tract-infection', 'SEXUAL_HEALTH', 'Sexual health: reproductive-tract infection', 104),
    ('rec-sexual-health-sti-suspected-or-known', 'SEXUAL_HEALTH', 'Sexual health: suspected or known STI', 105),
    ('rec-sexual-health-no-pregnancy-plan', 'SEXUAL_HEALTH', 'Sexual health: no pregnancy plan', 106),
    ('rec-sti-risk', 'STI', 'STI: at risk', 107),
    ('rec-sti-suspected-or-known', 'STI', 'STI: suspected or known', 108);

INSERT INTO public.community_topics (
    id, created_at, description, name, updated_at, is_hidden, icon, sort_order,
    created_by, type, slug, parent_id
)
SELECT
    md5('RECOMMENDATION_TAG_CATALOG_V1:' || slug)::uuid,
    now(),
    'RECOMMENDATION_TAG_CATALOG_V1|' || domain_name,
    label,
    now(),
    false,
    'recommendation',
    1000 + ordinal,
    NULL,
    'TAG',
    slug,
    NULL
FROM recommendation_tag_catalog_v1_extension
ON CONFLICT (slug) DO UPDATE
    SET name = EXCLUDED.name,
        description = EXCLUDED.description,
        updated_at = EXCLUDED.updated_at,
        is_hidden = false,
        sort_order = EXCLUDED.sort_order
    WHERE community_topics.id = EXCLUDED.id
      AND community_topics.type = 'TAG'
      AND community_topics.parent_id IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM recommendation_tag_catalog_v1_extension c
          LEFT JOIN public.community_topics t ON t.slug = c.slug
         WHERE t.id IS DISTINCT FROM md5('RECOMMENDATION_TAG_CATALOG_V1:' || c.slug)::uuid
            OR t.type IS DISTINCT FROM 'TAG'
            OR t.parent_id IS NOT NULL
            OR t.is_hidden IS DISTINCT FROM false
    ) THEN
        RAISE EXCEPTION 'RECOMMENDATION_TAG_CATALOG_V1 extension contains a conflicting or invalid rec-* row';
    END IF;
END $$;

DROP TABLE recommendation_tag_catalog_v1_extension;
