-- Re-express the immutable Story 6.5 baby/journey cleanup against the Phase 2
-- canonical schema. A database that applied the GitHub cutover before seeing
-- V20260722020000/V20260722020100 no longer has baby_profiles or
-- pregnancy_outcome_evidence, but it retains the same facts in care_subjects
-- and mother_journey_events.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

DO $$
BEGIN
    IF to_regclass('public.care_subjects') IS NULL
       OR to_regclass('public.mother_journeys') IS NULL
       OR to_regclass('public.mother_journey_events') IS NULL THEN
        RAISE EXCEPTION 'canonical baby/journey repair requires Phase 2 mother/baby tables';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.baby_journey_link_cleanup_summary (
    migration_key varchar(30) NOT NULL,
    reason_code varchar(50) NOT NULL,
    affected_count bigint NOT NULL CHECK (affected_count > 0),
    cleaned_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_baby_journey_link_cleanup_summary
        PRIMARY KEY (migration_key, reason_code)
);

ALTER TABLE public.baby_journey_link_cleanup_summary
    DROP CONSTRAINT IF EXISTS chk_baby_journey_link_cleanup_reason;
ALTER TABLE public.baby_journey_link_cleanup_summary
    ADD CONSTRAINT chk_baby_journey_link_cleanup_reason CHECK (reason_code IN (
        'MISSING_JOURNEY',
        'OWNER_MISMATCH',
        'NON_CANONICAL_OR_INACTIVE',
        'MISSING_OUTCOME_EVIDENCE',
        'EVIDENCE_OWNER_MISMATCH',
        'INCONSISTENT_OUTCOME_EVIDENCE',
        'INCOMPATIBLE_OUTCOME',
        'OUTCOME_DATE_MISMATCH'
    ));

LOCK TABLE public.care_subjects,
    public.mother_journeys,
    public.mother_journey_events
IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP VIEW story65_canonical_link_classification AS
WITH latest_evidence AS (
    SELECT DISTINCT ON (event.mother_journey_id)
        event.event_id,
        event.mother_journey_id,
        event.owner_user_id,
        event.outcome_type,
        event.outcome_date
    FROM public.mother_journey_events event
    WHERE event.event_type = 'PREGNANCY_OUTCOME_EVIDENCE'
      AND event.legacy_source = 'PREGNANCY_OUTCOME'
    ORDER BY event.mother_journey_id,
        event.revision_number DESC NULLS LAST,
        event.effective_at DESC,
        event.recorded_at DESC,
        event.event_id DESC
)
SELECT
    subject.care_subject_id,
    CASE
        WHEN journey.journey_id IS NULL THEN 'MISSING_JOURNEY'
        WHEN subject.owner_user_id IS DISTINCT FROM journey.owner_user_id
            THEN 'OWNER_MISMATCH'
        WHEN journey.status <> 'ACTIVE' OR journey.journey_type <> 'POSTPARTUM'
            THEN 'NON_CANONICAL_OR_INACTIVE'
        WHEN evidence.event_id IS NULL THEN 'MISSING_OUTCOME_EVIDENCE'
        WHEN evidence.owner_user_id IS DISTINCT FROM journey.owner_user_id
            THEN 'EVIDENCE_OWNER_MISMATCH'
        WHEN journey.pregnancy_outcome IS DISTINCT FROM evidence.outcome_type
            THEN 'INCONSISTENT_OUTCOME_EVIDENCE'
        WHEN evidence.outcome_type IS DISTINCT FROM 'LIVE_BIRTH'
            THEN 'INCOMPATIBLE_OUTCOME'
        WHEN journey.pregnancy_outcome_date IS DISTINCT FROM evidence.outcome_date
            THEN 'OUTCOME_DATE_MISMATCH'
        ELSE 'VALID'
    END AS reason_code
FROM public.care_subjects subject
LEFT JOIN public.mother_journeys journey
    ON journey.journey_id = subject.mother_journey_id
LEFT JOIN latest_evidence evidence
    ON evidence.mother_journey_id = journey.journey_id
WHERE subject.subject_type = 'BABY'
  AND subject.mother_journey_id IS NOT NULL;

CREATE TEMP TABLE story65_invalid_canonical_links
ON COMMIT DROP
AS
SELECT care_subject_id, reason_code
FROM story65_canonical_link_classification
WHERE reason_code <> 'VALID';

DO $$
DECLARE
    missing_evidence_count bigint;
    unapproved_reason_count bigint;
    drift_count bigint;
    invalid_count bigint;
    cleared_count bigint;
BEGIN
    SELECT count(*),
           count(*) FILTER (WHERE reason_code = 'MISSING_OUTCOME_EVIDENCE'),
           count(*) FILTER (
               WHERE reason_code NOT IN (
                   'MISSING_OUTCOME_EVIDENCE', 'OUTCOME_DATE_MISMATCH'))
      INTO invalid_count, missing_evidence_count, unapproved_reason_count
      FROM story65_invalid_canonical_links;

    -- Product/Data approved one missing-evidence quarantine and the separate
    -- outcome-date repair. Any other drift stops the release for review.
    IF missing_evidence_count > 1 OR unapproved_reason_count > 0 THEN
        RAISE EXCEPTION
            'canonical Story 6.5 cleanup exceeds approval: missing evidence %, unapproved %',
            missing_evidence_count, unapproved_reason_count;
    END IF;

    SELECT count(*)
      INTO drift_count
      FROM story65_invalid_canonical_links snapshot
      FULL JOIN (
          SELECT care_subject_id, reason_code
          FROM story65_canonical_link_classification
          WHERE reason_code <> 'VALID'
      ) current_state USING (care_subject_id)
     WHERE snapshot.care_subject_id IS NULL
        OR current_state.care_subject_id IS NULL
        OR snapshot.reason_code IS DISTINCT FROM current_state.reason_code;
    IF drift_count > 0 THEN
        RAISE EXCEPTION 'canonical Story 6.5 cleanup changed during locked revalidation';
    END IF;

    UPDATE public.care_subjects subject
       SET mother_journey_id = NULL,
           updated_at = now()
      FROM story65_invalid_canonical_links invalid
     WHERE subject.care_subject_id = invalid.care_subject_id
       AND subject.mother_journey_id IS NOT NULL;
    GET DIAGNOSTICS cleared_count = ROW_COUNT;
    IF cleared_count <> invalid_count THEN
        RAISE EXCEPTION 'canonical Story 6.5 cleanup count mismatch: expected %, cleared %',
            invalid_count, cleared_count;
    END IF;
END $$;

INSERT INTO public.baby_journey_link_cleanup_summary (
    migration_key, reason_code, affected_count, cleaned_at
)
SELECT
    'V20260724212500_CANONICAL',
    reason_code,
    count(*),
    now()
FROM story65_invalid_canonical_links
GROUP BY reason_code
ON CONFLICT (migration_key, reason_code) DO UPDATE
SET affected_count = EXCLUDED.affected_count,
    cleaned_at = EXCLUDED.cleaned_at;

DO $$
DECLARE
    remaining_invalid_count bigint;
BEGIN
    SELECT count(*) INTO remaining_invalid_count
      FROM story65_canonical_link_classification
     WHERE reason_code <> 'VALID';
    IF remaining_invalid_count <> 0 THEN
        RAISE EXCEPTION 'canonical Story 6.5 cleanup left % invalid baby/journey links',
            remaining_invalid_count;
    END IF;
END $$;
