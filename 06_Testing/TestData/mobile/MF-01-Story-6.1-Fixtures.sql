-- MF-01 / Story 6.1 mobile manual-test fixtures.
--
-- SAFETY:
--   Run only against a disposable/local PostgreSQL database.
--   The script resets only accounts whose email starts with mf01.story61.
--
-- Shared password for every synthetic account: Test@1234

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Remove prior sessions first so every manual-test login starts clean.
DELETE FROM public.refresh_tokens
WHERE user_id IN (
    SELECT user_id
    FROM public.users
    WHERE email LIKE 'mf01.story61.%@carebridge.test'
);

-- Reset lifecycle data created by a previous manual-test run.
DELETE FROM public.mother_journey_transitions
WHERE journey_id IN (
    SELECT journey_id
    FROM public.mother_journeys
    WHERE owner_user_id IN (
        SELECT user_id
        FROM public.users
        WHERE email LIKE 'mf01.story61.%@carebridge.test'
    )
);

DELETE FROM public.mother_journeys
WHERE owner_user_id IN (
    SELECT user_id
    FROM public.users
    WHERE email LIKE 'mf01.story61.%@carebridge.test'
);

WITH fixture_accounts(user_id, email, full_name, role) AS (
    VALUES
        ('61000000-0000-0000-0000-000000000001'::uuid, 'mf01.story61.new-a@carebridge.test', 'MF01 Mother New A', NULL),
        ('61000000-0000-0000-0000-000000000002'::uuid, 'mf01.story61.new-b@carebridge.test', 'MF01 Mother New B', NULL),
        ('61000000-0000-0000-0000-000000000003'::uuid, 'mf01.story61.new-c@carebridge.test', 'MF01 Mother New C', NULL),
        ('61000000-0000-0000-0000-000000000004'::uuid, 'mf01.story61.new-d@carebridge.test', 'MF01 Mother New D', NULL),
        ('61000000-0000-0000-0000-000000000005'::uuid, 'mf01.story61.new-e@carebridge.test', 'MF01 Mother New E', NULL),
        ('61000000-0000-0000-0000-000000000006'::uuid, 'mf01.story61.empty@carebridge.test', 'MF01 Mother Empty Existing', 'MOTHER'),
        ('61000000-0000-0000-0000-000000000007'::uuid, 'mf01.story61.preg@carebridge.test', 'MF01 Mother Pregnancy', 'MOTHER'),
        ('61000000-0000-0000-0000-000000000008'::uuid, 'mf01.story61.other@carebridge.test', 'MF01 Mother Other', 'MOTHER')
),
fixture_password AS (
    SELECT crypt('Test@1234', gen_salt('bf', 10)) AS password_hash
)
INSERT INTO public.users (
    user_id,
    email,
    full_name,
    password_hash,
    role,
    enabled,
    locked,
    account_status,
    email_verified,
    phone_verified,
    failed_login_count,
    must_change_password,
    created_at,
    updated_at
)
SELECT
    account.user_id,
    account.email,
    account.full_name,
    fixture_password.password_hash,
    account.role,
    true,
    false,
    'ACTIVE',
    true,
    false,
    0,
    false,
    now(),
    now()
FROM fixture_accounts account
CROSS JOIN fixture_password
ON CONFLICT (email) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    full_name = EXCLUDED.full_name,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    enabled = true,
    locked = false,
    locked_at = NULL,
    account_status = 'ACTIVE',
    email_verified = true,
    failed_login_count = 0,
    must_change_password = false,
    suspended_until = NULL,
    community_posting_restricted_until = NULL,
    updated_at = now();

-- MOTHER_PREG: canonical pregnancy with provenance and two history events.
INSERT INTO public.mother_journeys (
    journey_id,
    owner_user_id,
    journey_type,
    start_date,
    last_menstrual_date,
    estimated_due_date,
    status,
    notes,
    version,
    date_source,
    date_confidence,
    created_at,
    updated_at
) VALUES (
    '62000000-0000-0000-0000-000000000001'::uuid,
    '61000000-0000-0000-0000-000000000007'::uuid,
    'PREGNANCY',
    DATE '2026-04-01',
    DATE '2026-04-01',
    DATE '2027-01-08',
    'ACTIVE',
    'MF-01 Story 6.1 synthetic pregnancy fixture',
    1,
    'CLINICIAN_CONFIRMED',
    'CONFIRMED',
    TIMESTAMPTZ '2026-04-01 08:00:00+07',
    now()
);

INSERT INTO public.mother_journey_transitions (
    transition_id,
    journey_id,
    event_type,
    from_stage,
    to_stage,
    changes_json,
    source,
    confidence,
    reason,
    actor_user_id,
    effective_at,
    recorded_at,
    journey_version
) VALUES
(
    '63000000-0000-0000-0000-000000000001'::uuid,
    '62000000-0000-0000-0000-000000000001'::uuid,
    'CREATED',
    NULL,
    'PREGNANCY',
    '{"estimatedDueDate":{"new":"2027-01-06"}}'::jsonb,
    'SELF_REPORTED',
    'ESTIMATED',
    'MF01_FIXTURE_CREATED',
    '61000000-0000-0000-0000-000000000007'::uuid,
    TIMESTAMPTZ '2026-04-01 08:00:00+07',
    TIMESTAMPTZ '2026-04-01 08:00:00+07',
    0
),
(
    '63000000-0000-0000-0000-000000000002'::uuid,
    '62000000-0000-0000-0000-000000000001'::uuid,
    'DATES_CHANGED',
    'PREGNANCY',
    'PREGNANCY',
    '{"estimatedDueDate":{"previous":"2027-01-06","new":"2027-01-08"}}'::jsonb,
    'CLINICIAN_CONFIRMED',
    'CONFIRMED',
    'MF01_FIXTURE_CLINICIAN_CONFIRMATION',
    '61000000-0000-0000-0000-000000000007'::uuid,
    TIMESTAMPTZ '2026-07-17 09:00:00+07',
    TIMESTAMPTZ '2026-07-17 09:00:00+07',
    1
);

COMMIT;

SELECT
    email,
    COALESCE(role, '<UNASSIGNED>') AS role
FROM public.users
WHERE email LIKE 'mf01.story61.%@carebridge.test'
ORDER BY email;

SELECT
    u.email,
    j.journey_type,
    j.status,
    j.version,
    j.date_source,
    j.date_confidence,
    count(t.transition_id) AS transition_count
FROM public.users u
LEFT JOIN public.mother_journeys j ON j.owner_user_id = u.user_id
LEFT JOIN public.mother_journey_transitions t ON t.journey_id = j.journey_id
WHERE u.email LIKE 'mf01.story61.%@carebridge.test'
GROUP BY u.email, j.journey_type, j.status, j.version, j.date_source, j.date_confidence
ORDER BY u.email;
