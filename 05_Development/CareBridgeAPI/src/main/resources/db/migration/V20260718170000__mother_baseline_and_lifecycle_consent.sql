CREATE TABLE public.mother_baseline_contexts (
    baseline_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id uuid NOT NULL REFERENCES public.users(user_id),
    submission_id uuid NOT NULL,
    revision bigint NOT NULL,
    schema_version varchar(40) NOT NULL,
    lifecycle_goal varchar(40) NOT NULL,
    locale varchar(20) NOT NULL,
    time_zone varchar(80) NOT NULL,
    preferences varchar(300) NOT NULL,
    recorded_at timestamptz NOT NULL,
    CONSTRAINT uq_mother_baseline_revision UNIQUE (owner_user_id, revision),
    CONSTRAINT uq_mother_baseline_submission UNIQUE (owner_user_id, submission_id),
    CONSTRAINT chk_mother_baseline_goal CHECK (lifecycle_goal IN (
        'PREPARING_FOR_PREGNANCY', 'CURRENTLY_PREGNANT', 'POSTPARTUM_RECOVERY'))
);

CREATE INDEX idx_mother_baseline_owner_recorded
    ON public.mother_baseline_contexts(owner_user_id, recorded_at DESC);

ALTER TABLE public.consent_grants
    ADD COLUMN policy_version varchar(60),
    ADD COLUMN evidence_key uuid,
    ADD COLUMN locale varchar(20);

ALTER TABLE public.consent_grants DROP CONSTRAINT consent_grants_data_type_check;
ALTER TABLE public.consent_grants ADD CONSTRAINT consent_grants_data_type_check
    CHECK (data_type IN ('HEALTH_RECORD', 'LOCATION', 'FAMILY_DATA',
        'COMMUNITY_POST', 'SENSITIVE_DATA', 'RAG_CONTEXT', 'EXPERT_SHARED_DATA',
        'MOTHER_BASELINE'));

ALTER TABLE public.consent_grants DROP CONSTRAINT consent_grants_purpose_check;
ALTER TABLE public.consent_grants ADD CONSTRAINT consent_grants_purpose_check
    CHECK (purpose IN ('VIEW', 'CREATE', 'UPDATE', 'SHARE', 'DELETE', 'PERSONALIZE'));

CREATE UNIQUE INDEX uq_lifecycle_consent_evidence_key
    ON public.consent_grants(user_id, evidence_key)
    WHERE evidence_key IS NOT NULL;
