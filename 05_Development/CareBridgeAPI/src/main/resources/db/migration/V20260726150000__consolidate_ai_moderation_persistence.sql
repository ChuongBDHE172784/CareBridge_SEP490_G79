-- CB-MOD-IMP-017: consolidate AI moderation persistence.
--  1) ai_assessment_matches  -> ai_content_assessments.matches_jsonb (immutable snapshot array)
--  2) ai_assessment_feedback -> current feedback columns on moderation_cases; full history is
--     preserved as append-only moderation_events rows (action_type = AI_FEEDBACK_SUBMITTED).
-- Works on both an upgraded database holding data in the two legacy tables and a fresh
-- database where V20260726100000 just created them empty. Runs in one Flyway transaction:
-- any reconciliation mismatch RAISEs and rolls back everything — the legacy tables are only
-- dropped after every gate passes. No raw content is copied anywhere new — only the fields
-- the legacy tables already held (policy snapshot, verdict, sanitized reason).

-- ============================================================================
-- 1) matches_jsonb on ai_content_assessments
-- ============================================================================
ALTER TABLE ai_content_assessments
    ADD COLUMN IF NOT EXISTS matches_jsonb jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE ai_content_assessments
    DROP CONSTRAINT IF EXISTS chk_ai_assessment_matches_array;
ALTER TABLE ai_content_assessments
    ADD CONSTRAINT chk_ai_assessment_matches_array CHECK (jsonb_typeof(matches_jsonb) = 'array');

-- ============================================================================
-- 2) current-feedback columns on moderation_cases
-- ============================================================================
ALTER TABLE moderation_cases
    ADD COLUMN IF NOT EXISTS ai_feedback_decision varchar(20),
    ADD COLUMN IF NOT EXISTS ai_feedback_reason text,
    ADD COLUMN IF NOT EXISTS ai_feedback_by uuid REFERENCES users(user_id),
    ADD COLUMN IF NOT EXISTS ai_feedback_at timestamptz,
    ADD COLUMN IF NOT EXISTS ai_feedback_assessment_id uuid REFERENCES ai_content_assessments(assessment_id);

ALTER TABLE moderation_cases
    DROP CONSTRAINT IF EXISTS chk_moderation_cases_ai_feedback_decision;
ALTER TABLE moderation_cases
    ADD CONSTRAINT chk_moderation_cases_ai_feedback_decision
    CHECK (ai_feedback_decision IS NULL OR ai_feedback_decision IN ('AGREE','DISAGREE'));

-- ============================================================================
-- 3) Backfill + reconciliation + drop, atomically gated
-- ============================================================================
DO $$
DECLARE
    v_source_match_rows  bigint;
    v_backfilled_objects bigint;
    v_feedback_rows      bigint;
    v_feedback_events    bigint;
    v_latest_mismatches  bigint;
BEGIN
    ------------------------------------------------------------------
    -- 3a) matches backfill: one JSON object per legacy row, grouped per
    -- assessment in a stable order (policy_code, match_id). policy_version
    -- is the best available snapshot for legacy rows (the live policy's
    -- current version — the legacy table never stored one); new writes
    -- snapshot the true version at scan time.
    ------------------------------------------------------------------
    UPDATE ai_content_assessments a
       SET matches_jsonb = sub.matches
      FROM (
        SELECT m.assessment_id,
               jsonb_agg(
                   jsonb_build_object(
                       'policyId',      p.policy_id,
                       'policyCode',    m.policy_code,
                       'policyVersion', COALESCE(p.version, 1),
                       'category',      m.category,
                       'severity',      m.severity,
                       'confidence',    m.confidence,
                       'evidence',      CASE
                                            WHEN m.evidence IS NULL OR m.evidence = '' THEN '[]'::jsonb
                                            ELSE to_jsonb(string_to_array(m.evidence, E'\n'))
                                        END,
                       'explanation',   m.explanation
                   )
                   ORDER BY m.policy_code, m.match_id
               ) AS matches
          FROM ai_assessment_matches m
          LEFT JOIN ai_moderation_policies p ON p.policy_code = m.policy_code
         GROUP BY m.assessment_id
      ) sub
     WHERE a.assessment_id = sub.assessment_id;

    -- Gate 1: every legacy match row became exactly one JSON object
    SELECT count(*) INTO v_source_match_rows FROM ai_assessment_matches;
    SELECT COALESCE(sum(jsonb_array_length(a.matches_jsonb)), 0)
      INTO v_backfilled_objects
      FROM ai_content_assessments a
     WHERE a.assessment_id IN (SELECT DISTINCT assessment_id FROM ai_assessment_matches);
    IF v_source_match_rows <> v_backfilled_objects THEN
        RAISE EXCEPTION
            'AI_CONSOLIDATION_MATCH_BACKFILL_MISMATCH: source=% backfilled=%',
            v_source_match_rows, v_backfilled_objects;
    END IF;

    ------------------------------------------------------------------
    -- 3b) latest feedback per case -> moderation_cases columns.
    -- Case resolution: the feedback row's own case, else the assessment's.
    -- Latest = created_at DESC, feedback_id DESC as tie-breaker.
    ------------------------------------------------------------------
    UPDATE moderation_cases mc
       SET ai_feedback_decision      = latest.verdict,
           ai_feedback_reason        = left(latest.note, 500),
           ai_feedback_by            = latest.moderator_user_id,
           ai_feedback_at            = latest.created_at,
           ai_feedback_assessment_id = latest.assessment_id
      FROM (
        SELECT DISTINCT ON (case_id)
               COALESCE(f.moderation_case_id, a.moderation_case_id) AS case_id,
               f.verdict, f.note, f.moderator_user_id, f.created_at, f.assessment_id
          FROM ai_assessment_feedback f
          JOIN ai_content_assessments a ON a.assessment_id = f.assessment_id
         WHERE COALESCE(f.moderation_case_id, a.moderation_case_id) IS NOT NULL
         ORDER BY case_id, f.created_at DESC, f.feedback_id DESC
      ) latest
     WHERE mc.moderation_case_id = latest.case_id;

    ------------------------------------------------------------------
    -- 3c) preserve EVERY feedback record as an append-only moderation event.
    -- Deterministic event id (md5 of the legacy feedback_id) makes the insert
    -- idempotent — a retry/re-run of this logic can never duplicate events.
    -- Payload carries only the fields the legacy table held; never raw content.
    ------------------------------------------------------------------
    INSERT INTO moderation_events
        (moderation_event_id, moderation_case_id, moderator_user_id, action_type,
         target_type, target_id, reason, action_at, event_payload_jsonb)
    SELECT uuid_in(md5('ai-feedback-evt:' || f.feedback_id::text)::cstring),
           COALESCE(f.moderation_case_id, a.moderation_case_id),
           f.moderator_user_id,
           'AI_FEEDBACK_SUBMITTED',
           a.target_type,
           a.target_id,
           NULL,
           f.created_at,
           jsonb_build_object(
               'decision',     f.verdict,
               'assessmentId', f.assessment_id,
               'reason',       left(f.note, 500),
               'submittedAt',  f.created_at,
               'migratedFrom', 'ai_assessment_feedback'
           )
      FROM ai_assessment_feedback f
      JOIN ai_content_assessments a ON a.assessment_id = f.assessment_id
     WHERE NOT EXISTS (
           SELECT 1 FROM moderation_events e
            WHERE e.moderation_event_id = uuid_in(md5('ai-feedback-evt:' || f.feedback_id::text)::cstring));

    -- Gate 2: every legacy feedback row is represented by its deterministic event
    SELECT count(*) INTO v_feedback_rows FROM ai_assessment_feedback;
    SELECT count(*) INTO v_feedback_events
      FROM ai_assessment_feedback f
     WHERE EXISTS (
           SELECT 1 FROM moderation_events e
            WHERE e.moderation_event_id = uuid_in(md5('ai-feedback-evt:' || f.feedback_id::text)::cstring));
    IF v_feedback_rows <> v_feedback_events THEN
        RAISE EXCEPTION
            'AI_CONSOLIDATION_FEEDBACK_HISTORY_MISMATCH: source=% events=%',
            v_feedback_rows, v_feedback_events;
    END IF;

    -- Gate 3: every case with feedback carries exactly the latest decision/by/at/assessment
    SELECT count(*) INTO v_latest_mismatches
      FROM (
        SELECT DISTINCT ON (COALESCE(f.moderation_case_id, a.moderation_case_id))
               COALESCE(f.moderation_case_id, a.moderation_case_id) AS case_id,
               f.verdict, f.moderator_user_id, f.created_at, f.assessment_id
          FROM ai_assessment_feedback f
          JOIN ai_content_assessments a ON a.assessment_id = f.assessment_id
         WHERE COALESCE(f.moderation_case_id, a.moderation_case_id) IS NOT NULL
         ORDER BY COALESCE(f.moderation_case_id, a.moderation_case_id),
                  f.created_at DESC, f.feedback_id DESC
      ) latest
      JOIN moderation_cases mc ON mc.moderation_case_id = latest.case_id
     WHERE mc.ai_feedback_decision      IS DISTINCT FROM latest.verdict
        OR mc.ai_feedback_by            IS DISTINCT FROM latest.moderator_user_id
        OR mc.ai_feedback_at            IS DISTINCT FROM latest.created_at
        OR mc.ai_feedback_assessment_id IS DISTINCT FROM latest.assessment_id;
    IF v_latest_mismatches <> 0 THEN
        RAISE EXCEPTION
            'AI_CONSOLIDATION_LATEST_FEEDBACK_MISMATCH: % case(s) inconsistent', v_latest_mismatches;
    END IF;

    ------------------------------------------------------------------
    -- 3d) all gates passed — retire the legacy tables (their indexes/FKs drop with them)
    ------------------------------------------------------------------
    DROP TABLE IF EXISTS ai_assessment_matches;
    DROP TABLE IF EXISTS ai_assessment_feedback;
END $$;

-- No GIN index on matches_jsonb and no index on the ai_feedback_* columns on purpose: no
-- repository query filters on them today (matches are read back whole per assessment; the
-- current feedback is loaded with the case row by primary key). Add them together with the
-- first real consumer instead of speculatively.
