-- Remove the retired baby-to-mother-journey relation while preserving maternal
-- lifecycle rows, immutable audit history, and typed baby safety continuations.

LOCK TABLE public.care_subjects IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    detached_before bigint;
BEGIN
    SELECT count(*) INTO detached_before
      FROM public.care_subjects
     WHERE subject_type = 'BABY' AND mother_journey_id IS NOT NULL;
    RAISE NOTICE 'baby journey relations before detach: %', detached_before;
END $$;

UPDATE public.care_subjects
   SET mother_journey_id = NULL,
       updated_at = now()
 WHERE subject_type = 'BABY'
   AND mother_journey_id IS NOT NULL;

ALTER TABLE public.care_subjects
    ADD CONSTRAINT care_subjects_baby_no_mother_journey_ck
    CHECK (subject_type <> 'BABY' OR mother_journey_id IS NULL) NOT VALID;
ALTER TABLE public.care_subjects
    VALIDATE CONSTRAINT care_subjects_baby_no_mother_journey_ck;

DO $$
DECLARE
    detached_after bigint;
BEGIN
    SELECT count(*) INTO detached_after
      FROM public.care_subjects
     WHERE subject_type = 'BABY' AND mother_journey_id IS NOT NULL;
    RAISE NOTICE 'baby journey relations after detach: %', detached_after;
END $$;

LOCK TABLE public.triage_sessions IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE public.consultation_context_shares IN SHARE ROW EXCLUSIVE MODE;

ALTER TABLE public.consultation_context_shares
    DROP CONSTRAINT IF EXISTS fk_context_intake_snapshot;

ALTER TABLE public.triage_sessions
    DROP CONSTRAINT IF EXISTS chk_triage_lifecycle_binding,
    DROP CONSTRAINT IF EXISTS chk_triage_origin_stage;

ALTER TABLE public.consultation_context_shares
    ALTER COLUMN journey_id DROP NOT NULL;

DO $$
DECLARE
    triage_before bigint;
    shares_before bigint;
BEGIN
    SELECT count(*) INTO triage_before
      FROM public.triage_sessions
     WHERE origin_dashboard = 'BABY_PROFILE' AND journey_id IS NOT NULL;
    SELECT count(*) INTO shares_before
      FROM public.consultation_context_shares
     WHERE origin_dashboard = 'BABY_PROFILE' AND journey_id IS NOT NULL;
    RAISE NOTICE 'baby safety journey ids before detach: triage=%, shares=%',
        triage_before, shares_before;
END $$;

ALTER TABLE public.triage_sessions
    DISABLE TRIGGER triage_completed_snapshot_guard_trg;
UPDATE public.triage_sessions
   SET journey_id = NULL,
       updated_at = now()
 WHERE origin_dashboard = 'BABY_PROFILE'
   AND journey_id IS NOT NULL;
ALTER TABLE public.triage_sessions
    ENABLE TRIGGER triage_completed_snapshot_guard_trg;

ALTER TABLE public.consultation_context_shares
    DISABLE TRIGGER trg_consultation_context_shares_append_only;
UPDATE public.consultation_context_shares
   SET journey_id = NULL
 WHERE origin_dashboard = 'BABY_PROFILE'
   AND journey_id IS NOT NULL;
ALTER TABLE public.consultation_context_shares
    ENABLE TRIGGER trg_consultation_context_shares_append_only;

ALTER TABLE public.triage_sessions
    ADD CONSTRAINT chk_triage_lifecycle_binding CHECK (
        (journey_id IS NULL
            AND origin_dashboard IS NULL
            AND origin_reference_id IS NULL
            AND continuation_token IS NULL
            AND continuation_expires_at IS NULL
            AND continuation_acknowledged_at IS NULL)
        OR (origin_dashboard = 'MOTHER_JOURNEY'
            AND journey_id IS NOT NULL
            AND origin_reference_id IS NOT NULL
            AND continuation_token IS NOT NULL
            AND continuation_expires_at IS NOT NULL)
        OR (origin_dashboard = 'BABY_PROFILE'
            AND journey_id IS NULL
            AND origin_reference_id IS NOT NULL
            AND continuation_token IS NOT NULL
            AND continuation_expires_at IS NOT NULL)
    ) NOT VALID,
    ADD CONSTRAINT chk_triage_origin_stage CHECK (
        origin_dashboard IS NULL
        OR (origin_dashboard = 'MOTHER_JOURNEY'
            AND origin_reference_id = journey_id
            AND stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'))
        OR (origin_dashboard = 'BABY_PROFILE'
            AND journey_id IS NULL
            AND baby_profile_id IS NOT NULL
            AND origin_reference_id = baby_profile_id
            AND stage IN ('INFANT', 'TODDLER'))
    ) NOT VALID;
ALTER TABLE public.triage_sessions
    VALIDATE CONSTRAINT chk_triage_lifecycle_binding;
ALTER TABLE public.triage_sessions
    VALIDATE CONSTRAINT chk_triage_origin_stage;

-- Keep the complete immutable snapshot relationship for maternal shares and
-- add a non-null core relationship for baby shares (whose journey_id is null).
-- The old owner-only FK prevented orphaned intakes but allowed origin/stage/
-- risk/status snapshots to drift from the completed triage session.
CREATE UNIQUE INDEX IF NOT EXISTS triage_sessions_handoff_core_integrity_uk
    ON public.triage_sessions USING btree
       (triage_session_id, user_id, origin_dashboard, origin_reference_id,
        stage, risk_level, status);

ALTER TABLE public.consultation_context_shares
    ADD CONSTRAINT fk_context_intake_snapshot_core
        FOREIGN KEY (intake_session_id, owner_user_id, origin_dashboard,
                     origin_reference_id, triage_stage, risk_level, intake_status)
        REFERENCES public.triage_sessions (triage_session_id, user_id,
                                            origin_dashboard, origin_reference_id,
                                            stage, risk_level, status)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_context_intake_snapshot
        FOREIGN KEY (intake_session_id, owner_user_id, journey_id,
                     origin_dashboard, origin_reference_id, triage_stage,
                     risk_level, intake_status)
        REFERENCES public.triage_sessions (triage_session_id, user_id,
                                            journey_id, origin_dashboard,
                                            origin_reference_id, stage,
                                            risk_level, status)
        ON DELETE RESTRICT,
    ADD CONSTRAINT chk_context_origin_journey CHECK (
        (origin_dashboard = 'MOTHER_JOURNEY'
            AND journey_id IS NOT NULL
            AND origin_reference_id = journey_id
            AND triage_stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'))
        OR (origin_dashboard = 'BABY_PROFILE'
            AND journey_id IS NULL
            AND triage_stage IN ('INFANT', 'TODDLER'))
    ) NOT VALID;
ALTER TABLE public.consultation_context_shares
    VALIDATE CONSTRAINT chk_context_origin_journey;

DO $$
DECLARE
    triage_after bigint;
    shares_after bigint;
BEGIN
    SELECT count(*) INTO triage_after
      FROM public.triage_sessions
     WHERE origin_dashboard = 'BABY_PROFILE' AND journey_id IS NOT NULL;
    SELECT count(*) INTO shares_after
      FROM public.consultation_context_shares
     WHERE origin_dashboard = 'BABY_PROFILE' AND journey_id IS NOT NULL;
    RAISE NOTICE 'baby safety journey ids after detach: triage=%, shares=%',
        triage_after, shares_after;
END $$;
