-- READ ONLY. Run and obtain Product/Data approval before any cleanup migration.
WITH latest_evidence AS (
  SELECT DISTINCT ON (mother_journey_id)
         mother_journey_id AS journey_id, owner_user_id, outcome_type
  FROM mother_journey_events
  WHERE legacy_source = 'PREGNANCY_OUTCOME'
  ORDER BY mother_journey_id, revision_number DESC
), classified AS (
  SELECT CASE
    WHEN j.journey_id IS NULL THEN 'MISSING_JOURNEY'
    WHEN b.owner_user_id <> j.owner_user_id THEN 'OWNER_MISMATCH'
    WHEN j.status <> 'ACTIVE' OR j.journey_type <> 'POSTPARTUM' THEN 'NON_CANONICAL_OR_INACTIVE'
    WHEN e.journey_id IS NULL THEN 'MISSING_OUTCOME_EVIDENCE'
    WHEN e.owner_user_id IS DISTINCT FROM j.owner_user_id THEN 'EVIDENCE_OWNER_MISMATCH'
    WHEN j.pregnancy_outcome IS DISTINCT FROM e.outcome_type THEN 'INCONSISTENT_OUTCOME_EVIDENCE'
    WHEN e.outcome_type <> 'LIVE_BIRTH' THEN 'INCOMPATIBLE_OUTCOME'
    ELSE 'VALID'
  END reason
  FROM care_subjects b
  LEFT JOIN mother_journeys j ON j.journey_id=b.mother_journey_id
  LEFT JOIN latest_evidence e ON e.journey_id=j.journey_id
  WHERE b.subject_type = 'BABY' AND b.mother_journey_id IS NOT NULL
)
SELECT reason, count(*) AS row_count FROM classified GROUP BY reason ORDER BY reason;
