-- Personalized Content Recommendation V1
-- Forward-only: reuses the canonical journey/content/topic/consent tables.

ALTER TABLE public.mother_journeys
    ADD COLUMN IF NOT EXISTS recommendation_profile_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS recommendation_profile_version smallint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS recommendation_profile_completed_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS recommendation_profile_status character varying(24) NOT NULL DEFAULT 'NOT_STARTED';

ALTER TABLE public.content_items
    ADD COLUMN IF NOT EXISTS eligible_from_week smallint,
    ADD COLUMN IF NOT EXISTS eligible_to_week smallint,
    ADD COLUMN IF NOT EXISTS recommendation_priority smallint NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'mother_journeys_recommendation_profile_state_ck'
           AND conrelid = 'public.mother_journeys'::regclass
    ) THEN
        ALTER TABLE public.mother_journeys
            ADD CONSTRAINT mother_journeys_recommendation_profile_state_ck CHECK (
                (
                    recommendation_profile_status IN ('ACTIVE', 'REVIEW_REQUIRED')
                    AND recommendation_profile_version = 1
                    AND jsonb_typeof(recommendation_profile_jsonb) = 'object'
                    AND recommendation_profile_jsonb <> '{}'::jsonb
                    AND recommendation_profile_completed_at IS NOT NULL
                )
                OR (
                    recommendation_profile_status IN ('NOT_STARTED', 'DECLINED', 'RECONSENT_REQUIRED', 'REVOKED')
                    AND recommendation_profile_version = 0
                    AND recommendation_profile_jsonb = '{}'::jsonb
                    AND recommendation_profile_completed_at IS NULL
                )
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'content_items_recommendation_metadata_ck'
           AND conrelid = 'public.content_items'::regclass
    ) THEN
        ALTER TABLE public.content_items
            ADD CONSTRAINT content_items_recommendation_metadata_ck CHECK (
                recommendation_priority BETWEEN 0 AND 100
                AND (
                    (eligible_from_week IS NULL AND eligible_to_week IS NULL)
                    OR (
                        eligible_from_week IS NOT NULL
                        AND eligible_to_week IS NOT NULL
                        AND eligible_from_week BETWEEN 0 AND 42
                        AND eligible_to_week BETWEEN 0 AND 42
                        AND eligible_from_week <= eligible_to_week
                    )
                )
                AND (
                    stage = 'PREGNANCY'
                    OR (
                        stage IS DISTINCT FROM 'PREGNANCY'
                        AND eligible_from_week IS NULL
                        AND eligible_to_week IS NULL
                    )
                )
                AND (
                    content_type = 'ARTICLE'
                    OR (
                        content_type IS DISTINCT FROM 'ARTICLE'
                        AND eligible_from_week IS NULL
                        AND eligible_to_week IS NULL
                        AND recommendation_priority = 0
                    )
                )
            );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_content_item_topics_topic_content
    ON public.content_item_topics (topic_id, content_item_id);

CREATE INDEX IF NOT EXISTS idx_content_items_recommendation_eligible
    ON public.content_items (
        stage,
        recommendation_priority DESC,
        eligible_from_week,
        eligible_to_week,
        published_at DESC NULLS LAST,
        content_item_id ASC
    )
    WHERE status = 'APPROVED' AND content_type = 'ARTICLE';

-- A temporary catalog table keeps the seed declarative without introducing a
-- recommendation-specific persistent table. IDs are stable UUID-v3-shaped
-- values derived from the immutable slug and catalog version.
CREATE TEMP TABLE recommendation_tag_catalog_v1 (
    slug character varying(140) PRIMARY KEY,
    domain_name character varying(40) NOT NULL,
    label character varying(100) NOT NULL,
    ordinal smallint NOT NULL
) ON COMMIT DROP;

INSERT INTO recommendation_tag_catalog_v1 (slug, domain_name, label, ordinal) VALUES
    ('rec-age-under-18', 'AGE', 'Age: under 18', 1),
    ('rec-age-18-24', 'AGE', 'Age: 18-24', 2),
    ('rec-age-25-34', 'AGE', 'Age: 25-34', 3),
    ('rec-age-35-39', 'AGE', 'Age: 35-39', 4),
    ('rec-age-40-plus', 'AGE', 'Age: 40 and above', 5),
    ('rec-bmi-underweight', 'BMI', 'BMI: underweight', 6),
    ('rec-bmi-healthy-range', 'BMI', 'BMI: healthy range', 7),
    ('rec-bmi-overweight', 'BMI', 'BMI: overweight', 8),
    ('rec-bmi-obesity', 'BMI', 'BMI: obesity', 9),
    ('rec-reproductive-no-prior-pregnancy', 'REPRODUCTIVE_HISTORY', 'Reproductive history: no prior pregnancy', 10),
    ('rec-reproductive-prior-live-birth', 'REPRODUCTIVE_HISTORY', 'Reproductive history: prior live birth', 11),
    ('rec-reproductive-prior-pregnancy-loss', 'REPRODUCTIVE_HISTORY', 'Reproductive history: prior pregnancy loss', 12),
    ('rec-reproductive-prior-stillbirth', 'REPRODUCTIVE_HISTORY', 'Reproductive history: prior stillbirth', 13),
    ('rec-reproductive-prior-preterm-birth', 'REPRODUCTIVE_HISTORY', 'Reproductive history: prior preterm birth', 14),
    ('rec-reproductive-prior-multiple-pregnancy', 'REPRODUCTIVE_HISTORY', 'Reproductive history: prior multiple pregnancy', 15),
    ('rec-reproductive-other-history', 'REPRODUCTIVE_HISTORY', 'Reproductive history: other', 16),
    ('rec-condition-hypertension', 'UNDERLYING_CONDITION', 'Condition: hypertension', 17),
    ('rec-condition-diabetes', 'UNDERLYING_CONDITION', 'Condition: diabetes', 18),
    ('rec-condition-thyroid-disorder', 'UNDERLYING_CONDITION', 'Condition: thyroid disorder', 19),
    ('rec-condition-cardiovascular-disease', 'UNDERLYING_CONDITION', 'Condition: cardiovascular disease', 20),
    ('rec-condition-asthma', 'UNDERLYING_CONDITION', 'Condition: asthma', 21),
    ('rec-condition-epilepsy', 'UNDERLYING_CONDITION', 'Condition: epilepsy', 22),
    ('rec-condition-kidney-disease', 'UNDERLYING_CONDITION', 'Condition: kidney disease', 23),
    ('rec-condition-autoimmune-disease', 'UNDERLYING_CONDITION', 'Condition: autoimmune disease', 24),
    ('rec-condition-mental-health', 'UNDERLYING_CONDITION', 'Condition: mental health', 25),
    ('rec-condition-other-clinician-confirmed', 'UNDERLYING_CONDITION', 'Condition: other clinician confirmed', 26),
    ('rec-smoking-never', 'LIFESTYLE', 'Smoking: never', 27),
    ('rec-smoking-former', 'LIFESTYLE', 'Smoking: former', 28),
    ('rec-smoking-current', 'LIFESTYLE', 'Smoking: current', 29),
    ('rec-alcohol-none', 'LIFESTYLE', 'Alcohol: none', 30),
    ('rec-alcohol-less-than-weekly', 'LIFESTYLE', 'Alcohol: less than weekly', 31),
    ('rec-alcohol-weekly-or-more', 'LIFESTYLE', 'Alcohol: weekly or more', 32),
    ('rec-activity-low', 'LIFESTYLE', 'Physical activity: low', 33),
    ('rec-activity-moderate', 'LIFESTYLE', 'Physical activity: moderate', 34),
    ('rec-activity-high', 'LIFESTYLE', 'Physical activity: high', 35),
    ('rec-sleep-no-concern', 'LIFESTYLE', 'Sleep: no concern', 36),
    ('rec-sleep-concern', 'LIFESTYLE', 'Sleep: concern', 37),
    ('rec-nutrition-vegetarian', 'NUTRITION', 'Nutrition: vegetarian', 38),
    ('rec-nutrition-vegan', 'NUTRITION', 'Nutrition: vegan', 39),
    ('rec-nutrition-food-insecurity', 'NUTRITION', 'Nutrition: food insecurity', 40),
    ('rec-nutrition-low-appetite', 'NUTRITION', 'Nutrition: low appetite', 41),
    ('rec-nutrition-nausea-or-vomiting', 'NUTRITION', 'Nutrition: nausea or vomiting', 42),
    ('rec-nutrition-iron-or-folate-concern', 'NUTRITION', 'Nutrition: iron or folate concern', 43),
    ('rec-nutrition-other-concern', 'NUTRITION', 'Nutrition: other concern', 44),
    ('rec-vaccination-influenza-due', 'VACCINATION', 'Vaccination: influenza due', 45),
    ('rec-vaccination-covid-19-due', 'VACCINATION', 'Vaccination: COVID-19 due', 46),
    ('rec-vaccination-tdap-due', 'VACCINATION', 'Vaccination: Tdap due', 47),
    ('rec-vaccination-hepatitis-b-due', 'VACCINATION', 'Vaccination: hepatitis B due', 48),
    ('rec-vaccination-rubella-immunity-review', 'VACCINATION', 'Vaccination: rubella immunity review', 49),
    ('rec-medication-prenatal-vitamin', 'CURRENT_MEDICATION', 'Medication: prenatal vitamin', 50),
    ('rec-medication-folic-acid', 'CURRENT_MEDICATION', 'Medication: folic acid', 51),
    ('rec-medication-iron', 'CURRENT_MEDICATION', 'Medication: iron', 52),
    ('rec-medication-thyroid', 'CURRENT_MEDICATION', 'Medication: thyroid', 53),
    ('rec-medication-diabetes', 'CURRENT_MEDICATION', 'Medication: diabetes', 54),
    ('rec-medication-antihypertensive', 'CURRENT_MEDICATION', 'Medication: antihypertensive', 55),
    ('rec-medication-anticoagulant', 'CURRENT_MEDICATION', 'Medication: anticoagulant', 56),
    ('rec-medication-antiepileptic', 'CURRENT_MEDICATION', 'Medication: antiepileptic', 57),
    ('rec-medication-mental-health', 'CURRENT_MEDICATION', 'Medication: mental health', 58),
    ('rec-medication-other-prescribed', 'CURRENT_MEDICATION', 'Medication: other prescribed', 59),
    ('rec-sexual-health-general-information', 'SEXUAL_HEALTH', 'Sexual health: general information', 60),
    ('rec-sexual-health-contraception-or-fertility', 'SEXUAL_HEALTH', 'Sexual health: contraception or fertility', 61),
    ('rec-sexual-health-intimacy-during-lifecycle', 'SEXUAL_HEALTH', 'Sexual health: intimacy during lifecycle', 62),
    ('rec-sexual-health-other-non-urgent', 'SEXUAL_HEALTH', 'Sexual health: other non-urgent', 63),
    ('rec-sti-screening-information', 'STI', 'STI: screening information', 64),
    ('rec-sti-past-history', 'STI', 'STI: past history', 65),
    ('rec-sti-current-or-treatment', 'STI', 'STI: current or under treatment', 66),
    ('rec-sti-hiv', 'STI', 'STI: HIV', 67),
    ('rec-sti-syphilis', 'STI', 'STI: syphilis', 68),
    ('rec-sti-hepatitis-b', 'STI', 'STI: hepatitis B', 69),
    ('rec-sti-hepatitis-c', 'STI', 'STI: hepatitis C', 70),
    ('rec-sti-chlamydia', 'STI', 'STI: chlamydia', 71),
    ('rec-sti-gonorrhea', 'STI', 'STI: gonorrhea', 72),
    ('rec-sti-herpes', 'STI', 'STI: herpes', 73),
    ('rec-sti-hpv', 'STI', 'STI: HPV', 74),
    ('rec-sti-other', 'STI', 'STI: other', 75),
    ('rec-preference-nutrition', 'SUPPORT_PREFERENCE', 'Preference: nutrition', 76),
    ('rec-preference-mental-wellbeing', 'SUPPORT_PREFERENCE', 'Preference: mental wellbeing', 77),
    ('rec-preference-physical-activity', 'SUPPORT_PREFERENCE', 'Preference: physical activity', 78),
    ('rec-preference-appointment-reminders', 'SUPPORT_PREFERENCE', 'Preference: appointment reminders', 79);

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
FROM recommendation_tag_catalog_v1
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
          FROM recommendation_tag_catalog_v1 c
          LEFT JOIN public.community_topics t ON t.slug = c.slug
         WHERE t.id IS DISTINCT FROM md5('RECOMMENDATION_TAG_CATALOG_V1:' || c.slug)::uuid
            OR t.type IS DISTINCT FROM 'TAG'
            OR t.parent_id IS NOT NULL
            OR t.is_hidden IS DISTINCT FROM false
    ) THEN
        RAISE EXCEPTION 'RECOMMENDATION_TAG_CATALOG_V1 contains a conflicting or invalid rec-* row';
    END IF;
END $$;

DROP TABLE recommendation_tag_catalog_v1;
