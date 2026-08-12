-- Firebase Phone Auth returns Vietnamese numbers in canonical E.164 form.
-- Fail before mutation if legacy data cannot be converted safely or would
-- collapse multiple accounts onto the same authentication factor.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.users
         WHERE phone IS NOT NULL
           AND regexp_replace(phone, '[[:space:]().-]', '', 'g')
               !~ '^(\+84|84|0)[35789][0-9]{8}$'
    ) THEN
        RAISE EXCEPTION 'USERS_PHONE_CANONICALIZATION_INVALID_VALUE';
    END IF;

    IF EXISTS (
        WITH normalized AS (
            SELECT CASE
                     WHEN compact LIKE '+84%' THEN compact
                     WHEN compact LIKE '84%' THEN '+' || compact
                     ELSE '+84' || substring(compact FROM 2)
                   END AS canonical_phone
              FROM (
                  SELECT regexp_replace(phone, '[[:space:]().-]', '', 'g') AS compact
                    FROM public.users
                   WHERE phone IS NOT NULL
              ) source
        )
        SELECT 1
          FROM normalized
         GROUP BY canonical_phone
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'USERS_PHONE_CANONICALIZATION_DUPLICATE';
    END IF;
END
$$;

UPDATE public.users
   SET phone = CASE
       WHEN regexp_replace(phone, '[[:space:]().-]', '', 'g') LIKE '+84%'
           THEN regexp_replace(phone, '[[:space:]().-]', '', 'g')
       WHEN regexp_replace(phone, '[[:space:]().-]', '', 'g') LIKE '84%'
           THEN '+' || regexp_replace(phone, '[[:space:]().-]', '', 'g')
       ELSE '+84' || substring(regexp_replace(phone, '[[:space:]().-]', '', 'g') FROM 2)
   END,
       updated_at = now()
 WHERE phone IS NOT NULL
   AND phone IS DISTINCT FROM CASE
       WHEN regexp_replace(phone, '[[:space:]().-]', '', 'g') LIKE '+84%'
           THEN regexp_replace(phone, '[[:space:]().-]', '', 'g')
       WHEN regexp_replace(phone, '[[:space:]().-]', '', 'g') LIKE '84%'
           THEN '+' || regexp_replace(phone, '[[:space:]().-]', '', 'g')
       ELSE '+84' || substring(regexp_replace(phone, '[[:space:]().-]', '', 'g') FROM 2)
       END;

-- auth_challenges is the canonical OTP store. Older rows may still carry a
-- local-format phone (for example 0901234567) or mixed-case email. Runtime
-- verification always queries canonical E.164/lowercase subjects, so migrate
-- those historical identifiers in the same release as users.phone.
UPDATE public.auth_challenges
   SET subject_identifier = lower(btrim(subject_identifier))
 WHERE subject_identifier IS NOT NULL
   AND subject_identifier LIKE '%@%'
   AND subject_identifier IS DISTINCT FROM lower(btrim(subject_identifier));

UPDATE public.auth_challenges
   SET subject_identifier = CASE
       WHEN compact LIKE '+84%' THEN compact
       WHEN compact LIKE '84%' THEN '+' || compact
       ELSE '+84' || substring(compact FROM 2)
   END
  FROM (
      SELECT challenge_id,
             regexp_replace(subject_identifier, '[[:space:]().-]', '', 'g') AS compact
        FROM public.auth_challenges
       WHERE subject_identifier IS NOT NULL
  ) normalized
 WHERE public.auth_challenges.challenge_id = normalized.challenge_id
   AND normalized.compact ~ '^(\+84|84|0)[35789][0-9]{8}$'
   AND public.auth_challenges.subject_identifier IS DISTINCT FROM CASE
       WHEN normalized.compact LIKE '+84%' THEN normalized.compact
       WHEN normalized.compact LIKE '84%' THEN '+' || normalized.compact
       ELSE '+84' || substring(normalized.compact FROM 2)
   END;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.auth_challenges
         WHERE subject_identifier IS NOT NULL
           AND subject_identifier LIKE '%@%'
           AND subject_identifier IS DISTINCT FROM lower(btrim(subject_identifier))
    ) THEN
        RAISE EXCEPTION 'AUTH_CHALLENGES_EMAIL_CANONICALIZATION_FAILED';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.auth_challenges
         WHERE subject_identifier IS NOT NULL
           AND regexp_replace(subject_identifier, '[[:space:]().-]', '', 'g')
               ~ '^(\+84|84|0)[35789][0-9]{8}$'
           AND subject_identifier !~ '^\+84[35789][0-9]{8}$'
    ) THEN
        RAISE EXCEPTION 'AUTH_CHALLENGES_PHONE_CANONICALIZATION_FAILED';
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS users_phone_canonical_uk
    ON public.users (phone)
    WHERE phone IS NOT NULL;
