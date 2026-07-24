-- Story 6.5 / PD-8: Product/Data-approved containment of legacy links.
-- This migration stores aggregate reason counts only. It never persists baby,
-- journey, account, outcome, or health identifiers in the cleanup record.

CREATE TABLE IF NOT EXISTS public.baby_journey_link_cleanup_summary (
    migration_key varchar(30) NOT NULL,
    reason_code varchar(50) NOT NULL,
    affected_count bigint NOT NULL CHECK (affected_count > 0),
    cleaned_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_baby_journey_link_cleanup_summary
        PRIMARY KEY (migration_key, reason_code),
    CONSTRAINT chk_baby_journey_link_cleanup_reason
        CHECK (reason_code IN (
            'MISSING_JOURNEY',
            'OWNER_MISMATCH',
            'NON_CANONICAL_OR_INACTIVE',
            'MISSING_OUTCOME_EVIDENCE',
            'EVIDENCE_OWNER_MISMATCH',
            'INCONSISTENT_OUTCOME_EVIDENCE',
            'INCOMPATIBLE_OUTCOME'
        ))
);

-- Freeze every source used by the classification before taking the approved
-- snapshot. Reads remain available, while application repairs wait until this
-- migration either commits the quarantine or rolls back.
LOCK TABLE
    public.baby_profiles,
    public.mother_journeys,
    public.pregnancy_outcome_evidence
IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE story65_invalid_legacy_links
ON COMMIT DROP
AS
WITH latest_evidence AS (
    SELECT DISTINCT ON (journey_id)
        journey_id,
        owner_user_id,
        outcome_type
    FROM public.pregnancy_outcome_evidence
    ORDER BY journey_id, revision_number DESC
), classified AS (
    SELECT
        b.baby_id,
        CASE
            WHEN j.journey_id IS NULL THEN 'MISSING_JOURNEY'
            WHEN b.owner_user_id <> j.owner_user_id THEN 'OWNER_MISMATCH'
            WHEN j.status <> 'ACTIVE' OR j.journey_type <> 'POSTPARTUM'
                THEN 'NON_CANONICAL_OR_INACTIVE'
            WHEN e.journey_id IS NULL THEN 'MISSING_OUTCOME_EVIDENCE'
            WHEN e.owner_user_id IS DISTINCT FROM j.owner_user_id
                THEN 'EVIDENCE_OWNER_MISMATCH'
            WHEN j.pregnancy_outcome IS DISTINCT FROM e.outcome_type
                THEN 'INCONSISTENT_OUTCOME_EVIDENCE'
            WHEN e.outcome_type <> 'LIVE_BIRTH' THEN 'INCOMPATIBLE_OUTCOME'
            ELSE 'VALID'
        END AS reason_code
    FROM public.baby_profiles b
    LEFT JOIN public.mother_journeys j
        ON j.journey_id = b.related_journey_id
    LEFT JOIN latest_evidence e
        ON e.journey_id = j.journey_id
    WHERE b.related_journey_id IS NOT NULL
)
SELECT baby_id, reason_code
FROM classified
WHERE reason_code <> 'VALID';

DO $$
DECLARE
    invalid_count bigint;
    approved_reason_count bigint;
    revalidation_drift_count bigint;
BEGIN
    SELECT
        count(*),
        count(*) FILTER (WHERE reason_code = 'MISSING_OUTCOME_EVIDENCE')
    INTO invalid_count, approved_reason_count
    FROM story65_invalid_legacy_links;

    -- A clean environment is a valid no-op. Any non-empty cleanup must still
    -- match the exact sanitized report approved by Product/Data on 2026-07-21.
    IF invalid_count > 0
       AND (invalid_count <> 1 OR approved_reason_count <> 1) THEN
        RAISE EXCEPTION
            'Story 6.5 approved cleanup counts changed; expected one MISSING_OUTCOME_EVIDENCE row';
    END IF;

    -- Reclassify the locked targets immediately before clearing them. This is
    -- intentionally redundant with the table locks: if the lock contract is
    -- weakened later, repaired or otherwise changed links still fail closed.
    WITH latest_evidence AS (
        SELECT DISTINCT ON (journey_id)
            journey_id,
            owner_user_id,
            outcome_type
        FROM public.pregnancy_outcome_evidence
        ORDER BY journey_id, revision_number DESC
    ), reclassified AS (
        SELECT
            invalid.baby_id,
            invalid.reason_code AS snapshot_reason_code,
            CASE
                WHEN b.baby_id IS NULL THEN 'MISSING_BABY'
                WHEN b.related_journey_id IS NULL THEN 'VALID'
                WHEN j.journey_id IS NULL THEN 'MISSING_JOURNEY'
                WHEN b.owner_user_id <> j.owner_user_id THEN 'OWNER_MISMATCH'
                WHEN j.status <> 'ACTIVE' OR j.journey_type <> 'POSTPARTUM'
                    THEN 'NON_CANONICAL_OR_INACTIVE'
                WHEN e.journey_id IS NULL THEN 'MISSING_OUTCOME_EVIDENCE'
                WHEN e.owner_user_id IS DISTINCT FROM j.owner_user_id
                    THEN 'EVIDENCE_OWNER_MISMATCH'
                WHEN j.pregnancy_outcome IS DISTINCT FROM e.outcome_type
                    THEN 'INCONSISTENT_OUTCOME_EVIDENCE'
                WHEN e.outcome_type <> 'LIVE_BIRTH' THEN 'INCOMPATIBLE_OUTCOME'
                ELSE 'VALID'
            END AS current_reason_code
        FROM story65_invalid_legacy_links invalid
        LEFT JOIN public.baby_profiles b
            ON b.baby_id = invalid.baby_id
        LEFT JOIN public.mother_journeys j
            ON j.journey_id = b.related_journey_id
        LEFT JOIN latest_evidence e
            ON e.journey_id = j.journey_id
    )
    SELECT count(*)
    INTO revalidation_drift_count
    FROM reclassified
    WHERE current_reason_code IS DISTINCT FROM snapshot_reason_code;

    IF revalidation_drift_count > 0 THEN
        RAISE EXCEPTION
            'Story 6.5 cleanup targets changed during locked revalidation';
    END IF;
END $$;

WITH cleared AS (
    UPDATE public.baby_profiles b
    SET related_journey_id = NULL
    FROM story65_invalid_legacy_links invalid
    WHERE b.baby_id = invalid.baby_id
    RETURNING invalid.reason_code
)
INSERT INTO public.baby_journey_link_cleanup_summary (
    migration_key,
    reason_code,
    affected_count
)
SELECT
    'V20260722020000',
    reason_code,
    count(*)
FROM cleared
GROUP BY reason_code
ON CONFLICT (migration_key, reason_code) DO NOTHING;
