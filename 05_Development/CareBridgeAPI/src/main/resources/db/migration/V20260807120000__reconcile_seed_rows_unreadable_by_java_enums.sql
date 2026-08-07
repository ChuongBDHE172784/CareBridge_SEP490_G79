-- Correct four V2 seed rows carrying values no Java enum can deserialize.
--
-- Every JPA read covering one of these rows fails with "No enum constant", which is how
-- they were found: several integration suites passed or failed purely according to whether
-- an earlier class had truncated the offending row first.
--
-- V2__seed_reference_data.sql is already applied everywhere, so it cannot be edited without
-- breaking Flyway's checksum; the rows are corrected forward here instead.
--
-- Direction of the fix differs from the audit_events case handled in the same change. There,
-- the table is append-only and its history is authoritative, so AuditAction grew the missing
-- constants. Here the columns carry no CHECK constraint — the schema does not police them —
-- and the values are simply wrong for the domain, so the data is corrected to the canonical
-- constants the application already understands:
--
--   content_items.status         'PUBLISHED'         -> 'APPROVED'
--       ContentStatus is {DRAFT, PENDING_REVIEW, APPROVED, ARCHIVED}; APPROVED is the
--       publicly visible state, and every other seeded content row already uses it.
--   content_items.stage          'INFANT'            -> 'POSTPARTUM'
--       ContentStage is {PRE_PREGNANCY, PREGNANCY, POSTPARTUM}. The row is infant
--       vaccination guidance, and ContentStage.fromApiValue already folds the sibling
--       alias 'BABY_CARE' onto POSTPARTUM, so baby-stage content belongs there.
--   content_items.content_type   'GUIDE'             -> 'ARTICLE'
--       ContentType is {ARTICLE, FAQ, CHECKLIST}. FAQ and CHECKLIST carry specific
--       structure this row does not have; ARTICLE is the general prose type.
--   moderation_cases.target_type 'COMMUNITY_CONTENT' -> 'QUESTION'
--       ReportTargetType has no COMMUNITY_CONTENT. The referenced row
--       81000000-0000-0000-0000-000000000001 is a community_content row whose
--       content_type is 'QUESTION', so QUESTION is the specific, correct target type.
--   auth_challenges.challenge_type 'PHONE_OTP'       -> 'REGISTER'
--       OtpPurpose is {REGISTER, LOGIN}. PHONE_OTP names a delivery channel, not a
--       purpose; the seed conflated the two. Every other seeded challenge is REGISTER.
--
-- Each statement is keyed to the exact seed id and to the bad value, so a rerun is a no-op
-- and a row an operator has since corrected by hand is left alone.

UPDATE public.content_items
   SET status = 'APPROVED'
 WHERE content_item_id IN (
           '80000000-0000-0000-0000-000000000001',
           '80000000-0000-0000-0000-000000000002')
   AND status = 'PUBLISHED';

UPDATE public.content_items
   SET stage = 'POSTPARTUM'
 WHERE content_item_id = '80000000-0000-0000-0000-000000000002'
   AND stage = 'INFANT';

UPDATE public.content_items
   SET content_type = 'ARTICLE'
 WHERE content_item_id = '80000000-0000-0000-0000-000000000002'
   AND content_type = 'GUIDE';

UPDATE public.moderation_cases
   SET target_type = 'QUESTION'
 WHERE moderation_case_id = 'd0000000-0000-0000-0000-000000000001'
   AND target_type = 'COMMUNITY_CONTENT';

UPDATE public.auth_challenges
   SET challenge_type = 'REGISTER'
 WHERE challenge_id = 'a0000000-0000-0000-0000-000000000001'
   AND challenge_type = 'PHONE_OTP';

-- Gate: nothing anywhere in these columns may still hold a value the enums cannot read.
-- Scoped to the three columns this migration owns; audit_events is deliberately excluded
-- because its history is immutable and AuditAction was widened to match it instead.
DO $$
DECLARE
    v_bad text;
BEGIN
    SELECT string_agg(detail, '; ' ORDER BY detail) INTO v_bad FROM (
        SELECT 'content_items.status=' || status AS detail
          FROM public.content_items
         WHERE status NOT IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'ARCHIVED')
         GROUP BY status
        UNION ALL
        SELECT 'content_items.stage=' || stage
          FROM public.content_items
         WHERE stage IS NOT NULL
           AND stage NOT IN ('PRE_PREGNANCY', 'PREGNANCY', 'POSTPARTUM')
         GROUP BY stage
        UNION ALL
        SELECT 'content_items.content_type=' || content_type
          FROM public.content_items
         WHERE content_type NOT IN ('ARTICLE', 'FAQ', 'CHECKLIST')
         GROUP BY content_type
        UNION ALL
        SELECT 'moderation_cases.target_type=' || target_type
          FROM public.moderation_cases
         WHERE target_type NOT IN ('QUESTION', 'ANSWER', 'CONTENT', 'ACCOUNT', 'EXPERT', 'USER')
         GROUP BY target_type
        UNION ALL
        SELECT 'auth_challenges.challenge_type=' || challenge_type
          FROM public.auth_challenges
         WHERE challenge_type NOT IN ('REGISTER', 'LOGIN')
         GROUP BY challenge_type
    ) AS offenders;

    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION
            'SEED_RECONCILE_FAILED: values remain that no Java enum can read: %', v_bad;
    END IF;
END
$$;
