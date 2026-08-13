-- Forward repair for databases that applied V20260813200000 before the canonical AI policy
-- migration existed. Fresh databases also run this safely: every update is deterministic and
-- scoped to the c6/c7/c8 CareBridge development-demo UUID ranges.
--
-- This repair intentionally updates existing rows rather than deleting/reinserting them because
-- moderation_cases and ai_content_assessments have a circular feedback relationship.

DO $community_ai_snapshot_repair$
DECLARE
    v_job_count integer;
    v_assessment_count integer;
    v_case_count integer;
BEGIN
    SELECT count(*) INTO v_job_count
      FROM public.ai_content_scan_jobs
     WHERE job_id::text LIKE 'c7000000-0000-4000-8000-%';
    SELECT count(*) INTO v_assessment_count
      FROM public.ai_content_assessments
     WHERE assessment_id::text LIKE 'c8000000-0000-4000-8000-%';
    SELECT count(*) INTO v_case_count
      FROM public.moderation_cases
     WHERE moderation_case_id::text LIKE 'c6000000-0000-4000-8000-%';

    IF v_job_count = 0 AND v_assessment_count = 0 AND v_case_count = 0 THEN
        RAISE NOTICE 'Skipping CareBridge community AI snapshot repair: demo data is not installed';
        RETURN;
    END IF;

    IF v_job_count <> 44 OR v_assessment_count <> 24 OR v_case_count <> 24 THEN
        RAISE EXCEPTION
            'CAREBRIDGE_COMMUNITY_AI_REPAIR_INCOMPLETE_SOURCE: jobs=%, assessments=%, cases=%',
            v_job_count, v_assessment_count, v_case_count;
    END IF;

    IF (
        SELECT count(*)
          FROM public.ai_moderation_policies
         WHERE policy_code IN (
            'CHILD_SAFETY',
            'DANGEROUS_MEDICAL_ADVICE',
            'EXPERT_IMPERSONATION',
            'HARASSMENT_BULLYING',
            'HARMFUL_MISINFORMATION',
            'HATE_SPEECH',
            'PII_DOXXING',
            'PROMPT_INJECTION',
            'SCAM_FRAUD',
            'SELF_HARM_ENCOURAGEMENT',
            'SPAM_ADVERTISING'
         )
           AND active
    ) <> 11 THEN
        RAISE EXCEPTION 'CAREBRIDGE_COMMUNITY_AI_REPAIR_MISSING_CANONICAL_POLICIES';
    END IF;

    CREATE TEMP TABLE community_ai_repair_seed (
        assessment_seq integer PRIMARY KEY,
        policy_code varchar(60) NOT NULL,
        evidence text NOT NULL
    ) ON COMMIT DROP;

    INSERT INTO community_ai_repair_seed(assessment_seq, policy_code, evidence) VALUES
        (9,  'DANGEROUS_MEDICAL_ADVICE', 'tăng gấp nhiều lần liều vitamin'),
        (10, 'EXPERT_IMPERSONATION',      'chẩn đoán từ ảnh siêu âm'),
        (11, 'DANGEROUS_MEDICAL_ADVICE', 'tự ngừng theo dõi và bỏ lịch tái khám'),
        (12, 'HARASSMENT_BULLYING',      'dùng lời lẽ miệt thị, đổ lỗi'),
        (13, 'SPAM_ADVERTISING',         'quảng cáo sản phẩm không rõ nguồn gốc'),
        (14, 'DANGEROUS_MEDICAL_ADVICE', 'bỏ lịch tái khám vàng da'),
        (15, 'HARASSMENT_BULLYING',      'công kích người đang tìm kiếm hỗ trợ'),
        (16, 'SCAM_FRAUD',               'yêu cầu chuyển khoản giữ chỗ'),
        (17, 'SPAM_ADVERTISING',         'mua gói thuốc của tôi'),
        (18, 'DANGEROUS_MEDICAL_ADVICE', 'Tăng liều càng cao càng tốt'),
        (19, 'EXPERT_IMPERSONATION',      'chẩn đoán chắc chắn nếu bạn chuyển khoản trước'),
        (20, 'HARASSMENT_BULLYING',      'quá yếu đuối, đừng đăng bài làm phiền');

    -- AiContentHasher hashes normalized current text: question title + LF + body, or answer body.
    UPDATE public.ai_content_scan_jobs j
       SET content_hash = encode(sha256(convert_to(
               CASE
                 WHEN j.target_type = 'QUESTION'
                      AND content.title IS NOT NULL
                      AND btrim(content.title) <> ''
                   THEN btrim(content.title || E'\n' || content.body)
                 ELSE btrim(content.body)
               END,
               'UTF8')), 'hex'),
           updated_at = clock_timestamp()
      FROM public.community_content content
     WHERE j.job_id::text LIKE 'c7000000-0000-4000-8000-%'
       AND content.content_id = j.target_id;

    -- Store the same per-target active policy-set fingerprint as activeSnapshotFor(targetType).
    WITH policy_set AS (
        SELECT target.target_type,
               encode(sha256(convert_to(string_agg(
                   p.policy_code || ':' || p.version::text || ':' || p.severity || ':'
                   || p.violation_category || ':'
                   || CASE
                        WHEN p.confidence_threshold = trunc(p.confidence_threshold)
                        THEN trunc(p.confidence_threshold)::text
                        ELSE trim(trailing '0' FROM p.confidence_threshold::text)
                      END || ':' || p.applicable_target_types,
                   '|' ORDER BY p.policy_code), 'UTF8')), 'hex') AS policy_set_hash
          FROM (VALUES ('QUESTION'), ('ANSWER')) AS target(target_type)
          JOIN public.ai_moderation_policies p
            ON p.active
           AND target.target_type = ANY (regexp_split_to_array(p.applicable_target_types, '\s*,\s*'))
         GROUP BY target.target_type
    )
    UPDATE public.ai_content_assessments a
       SET content_hash = j.content_hash,
           policy_set_hash = ps.policy_set_hash,
           overall_severity = COALESCE(p.severity, a.overall_severity),
           recommended_action = CASE
               WHEN seed.assessment_seq IS NULL THEN a.recommended_action
               WHEN a.classification = 'UNCERTAIN' THEN 'REVIEW'
               WHEN p.severity = 'CRITICAL' THEN 'ESCALATE'
               WHEN p.severity = 'HIGH' THEN 'PRIORITY_REVIEW'
               ELSE 'REVIEW'
           END,
           matches_jsonb = CASE
               WHEN seed.assessment_seq IS NULL THEN '[]'::jsonb
               ELSE jsonb_build_array(jsonb_build_object(
                   'policyId', p.policy_id,
                   'policyCode', p.policy_code,
                   'policyVersion', p.version,
                   'category', p.violation_category,
                   'severity', p.severity,
                   'confidence', a.confidence,
                   'evidence', jsonb_build_array(seed.evidence),
                   'explanation', a.explanation
               ))
           END
      FROM public.ai_content_scan_jobs j
      JOIN policy_set ps ON ps.target_type = j.target_type
      LEFT JOIN community_ai_repair_seed seed
        ON seed.assessment_seq = substring(j.job_id::text FROM '[0-9]+$')::integer
      LEFT JOIN public.ai_moderation_policies p
        ON p.policy_code = seed.policy_code
     WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
       AND a.job_id = j.job_id;

    -- Automated cases reflect the same category/priority matrix as AiModerationDecisionPolicy.
    UPDATE public.moderation_cases c
       SET reason_code = p.report_category,
           priority = CASE p.severity
               WHEN 'CRITICAL' THEN 'URGENT'
               WHEN 'HIGH' THEN 'HIGH'
               ELSE 'NORMAL'
           END,
           updated_at = clock_timestamp()
      FROM public.ai_content_assessments a
      CROSS JOIN LATERAL jsonb_array_elements(a.matches_jsonb) AS match(value)
      JOIN public.ai_moderation_policies p
        ON p.policy_id = (match.value ->> 'policyId')::uuid
     WHERE c.moderation_case_id::text LIKE 'c6000000-0000-4000-8000-%'
       AND c.report_source = 'AUTOMATED'
       AND a.moderation_case_id = c.moderation_case_id;

    IF (SELECT count(*) FROM community_ai_repair_seed) <> 12
       OR (
            SELECT count(*)
              FROM public.ai_content_scan_jobs j
              JOIN public.community_content content ON content.content_id = j.target_id
             WHERE j.job_id::text LIKE 'c7000000-0000-4000-8000-%'
               AND j.content_hash = encode(sha256(convert_to(
                   CASE
                     WHEN j.target_type = 'QUESTION'
                          AND content.title IS NOT NULL
                          AND btrim(content.title) <> ''
                       THEN btrim(content.title || E'\n' || content.body)
                     ELSE btrim(content.body)
                   END,
                   'UTF8')), 'hex')
       ) <> 44
       OR (
            SELECT count(*)
              FROM public.ai_content_assessments a
              JOIN public.ai_content_scan_jobs j ON j.job_id = a.job_id
             WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
               AND a.content_hash = j.content_hash
       ) <> 24
       OR (
            SELECT count(*)
              FROM public.ai_content_assessments a
             WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
               AND jsonb_array_length(a.matches_jsonb) = 1
       ) <> 12
       OR EXISTS (
            SELECT 1
              FROM public.ai_content_assessments a
              CROSS JOIN LATERAL jsonb_array_elements(a.matches_jsonb) AS match(value)
              LEFT JOIN public.ai_moderation_policies p
                ON p.policy_id = (match.value ->> 'policyId')::uuid
               AND p.policy_code = match.value ->> 'policyCode'
               AND p.version = (match.value ->> 'policyVersion')::integer
               AND p.violation_category = match.value ->> 'category'
               AND p.severity = match.value ->> 'severity'
             WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
               AND p.policy_id IS NULL
       )
       OR EXISTS (
            SELECT 1
              FROM public.ai_content_assessments a
              JOIN public.community_content content ON content.content_id = a.target_id
              CROSS JOIN LATERAL jsonb_array_elements(a.matches_jsonb) AS match(value)
              CROSS JOIN LATERAL jsonb_array_elements_text(match.value -> 'evidence') AS evidence(excerpt)
             WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
               AND strpos(content.body, evidence.excerpt) = 0
       )
       OR EXISTS (
            SELECT 1
              FROM public.ai_content_assessments a
             WHERE a.assessment_id::text LIKE 'c8000000-0000-4000-8000-%'
               AND a.policy_set_hash <> (
                    SELECT encode(sha256(convert_to(string_agg(
                               p.policy_code || ':' || p.version::text || ':' || p.severity || ':'
                               || p.violation_category || ':'
                               || CASE
                                    WHEN p.confidence_threshold = trunc(p.confidence_threshold)
                                    THEN trunc(p.confidence_threshold)::text
                                    ELSE trim(trailing '0' FROM p.confidence_threshold::text)
                                  END || ':' || p.applicable_target_types,
                               '|' ORDER BY p.policy_code), 'UTF8')), 'hex')
                      FROM public.ai_moderation_policies p
                     WHERE p.active
                       AND a.target_type = ANY (regexp_split_to_array(p.applicable_target_types, '\s*,\s*'))
               )
       )
       OR EXISTS (
            SELECT 1
              FROM public.moderation_cases c
              JOIN public.ai_content_assessments a
                ON a.moderation_case_id = c.moderation_case_id
              CROSS JOIN LATERAL jsonb_array_elements(a.matches_jsonb) AS match(value)
              JOIN public.ai_moderation_policies p
                ON p.policy_id = (match.value ->> 'policyId')::uuid
             WHERE c.moderation_case_id::text LIKE 'c6000000-0000-4000-8000-%'
               AND c.report_source = 'AUTOMATED'
               AND (
                    c.reason_code <> p.report_category
                    OR c.priority <> CASE p.severity
                        WHEN 'CRITICAL' THEN 'URGENT'
                        WHEN 'HIGH' THEN 'HIGH'
                        ELSE 'NORMAL'
                    END
               )
       )
    THEN
        RAISE EXCEPTION 'CAREBRIDGE_COMMUNITY_AI_REPAIR_VALIDATION_FAILED';
    END IF;
END
$community_ai_snapshot_repair$;
