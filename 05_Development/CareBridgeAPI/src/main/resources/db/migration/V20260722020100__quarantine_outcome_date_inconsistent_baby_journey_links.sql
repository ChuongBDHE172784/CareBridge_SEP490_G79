-- Story 6.5 review hardening: quarantine links whose current journey outcome
-- date differs from the latest immutable outcome evidence. The table locks
-- make classification and mutation one exclusive write window; the UPDATE
-- also re-evaluates the predicate at the final mutation.

ALTER TABLE public.baby_journey_link_cleanup_summary
    DROP CONSTRAINT chk_baby_journey_link_cleanup_reason;

ALTER TABLE public.baby_journey_link_cleanup_summary
    ADD CONSTRAINT chk_baby_journey_link_cleanup_reason
        CHECK (reason_code IN (
            'MISSING_JOURNEY',
            'OWNER_MISMATCH',
            'NON_CANONICAL_OR_INACTIVE',
            'MISSING_OUTCOME_EVIDENCE',
            'EVIDENCE_OWNER_MISMATCH',
            'INCONSISTENT_OUTCOME_EVIDENCE',
            'INCOMPATIBLE_OUTCOME',
            'OUTCOME_DATE_MISMATCH'
        ));

LOCK TABLE public.mother_journeys,
    public.pregnancy_outcome_evidence,
    public.baby_profiles
    IN SHARE ROW EXCLUSIVE MODE;

WITH latest_evidence AS (
    SELECT DISTINCT ON (journey_id)
        journey_id,
        outcome_date
    FROM public.pregnancy_outcome_evidence
    ORDER BY journey_id, revision_number DESC
), cleared AS (
    UPDATE public.baby_profiles b
    SET related_journey_id = NULL
    FROM public.mother_journeys j
    JOIN latest_evidence e
        ON e.journey_id = j.journey_id
    WHERE b.related_journey_id = j.journey_id
      AND j.pregnancy_outcome_date IS DISTINCT FROM e.outcome_date
    RETURNING 1
), summarized AS (
    SELECT count(*) AS affected_count
    FROM cleared
)
INSERT INTO public.baby_journey_link_cleanup_summary (
    migration_key,
    reason_code,
    affected_count
)
SELECT
    'V20260722020100',
    'OUTCOME_DATE_MISMATCH',
    affected_count
FROM summarized
WHERE affected_count > 0
ON CONFLICT (migration_key, reason_code) DO NOTHING;
