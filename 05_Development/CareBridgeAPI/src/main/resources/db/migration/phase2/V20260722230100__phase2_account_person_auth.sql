-- Phase 2 wave 1: canonical person, care-subject, account and authentication.
-- This migration is safe on both an empty target bootstrap and the legacy upgrade path.
-- Legacy tables are intentionally retained until application cutover evidence is complete.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE TABLE IF NOT EXISTS public.persons (
    person_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name varchar(200),
    date_of_birth date,
    phone_number varchar(40),
    avatar_url text,
    area varchar(200),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.users (
    user_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id uuid,
    email varchar(255),
    full_name varchar(200),
    avatar_url text,
    password_hash varchar(255),
    phone varchar(40),
    role varchar(40),
    enabled boolean NOT NULL DEFAULT true,
    locked boolean NOT NULL DEFAULT false,
    email_verified boolean NOT NULL DEFAULT false,
    phone_verified boolean NOT NULL DEFAULT false,
    account_status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    settings_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.users ADD COLUMN IF NOT EXISTS person_id uuid;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS settings_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb;

DO $profile_mapping$
BEGIN
    IF to_regclass('public.user_profiles') IS NOT NULL THEN
        INSERT INTO public.persons (
            person_id, display_name, date_of_birth, phone_number, avatar_url, area, created_at, updated_at
        )
        SELECT u.user_id,
               coalesce(up.display_name, u.full_name),
               up.date_of_birth,
               coalesce(up.phone_number, u.phone),
               coalesce(up.avatar_url, u.avatar_url),
               up.area,
               least(u.created_at, coalesce(up.created_at, u.created_at)),
               greatest(u.updated_at, coalesce(up.updated_at, u.updated_at))
          FROM public.users u
          LEFT JOIN public.user_profiles up ON up.user_id = u.user_id
        ON CONFLICT (person_id) DO NOTHING;
    END IF;
END
$profile_mapping$;

-- The clean target path has no legacy user_profiles table.
INSERT INTO public.persons (person_id, display_name, phone_number, avatar_url, created_at, updated_at)
SELECT u.user_id, u.full_name, u.phone, u.avatar_url, u.created_at, u.updated_at
  FROM public.users u
 WHERE NOT EXISTS (SELECT 1 FROM public.persons p WHERE p.person_id = u.user_id)
ON CONFLICT (person_id) DO NOTHING;

UPDATE public.users SET person_id = user_id WHERE person_id IS NULL;

DO $constraints$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'users_person_fk') THEN
        ALTER TABLE public.users
            ADD CONSTRAINT users_person_fk FOREIGN KEY (person_id) REFERENCES public.persons(person_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'users_person_uk') THEN
        ALTER TABLE public.users ADD CONSTRAINT users_person_uk UNIQUE (person_id);
    END IF;
END
$constraints$;
ALTER TABLE public.users ALTER COLUMN person_id SET NOT NULL;

CREATE TABLE IF NOT EXISTS public.care_subjects (
    care_subject_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id uuid NOT NULL REFERENCES public.persons(person_id),
    owner_user_id uuid NOT NULL REFERENCES public.users(user_id),
    mother_journey_id uuid,
    subject_type varchar(30) NOT NULL,
    nickname varchar(200),
    birth_date date,
    sex varchar(30),
    birth_weight_kg numeric(6,3),
    birth_length_cm numeric(6,2),
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT care_subjects_type_ck CHECK (subject_type IN ('MOTHER', 'BABY', 'DEPENDENT')),
    CONSTRAINT care_subjects_owner_person_uk UNIQUE (owner_user_id, person_id, subject_type)
);

DO $baby_mapping$
BEGIN
    IF to_regclass('public.baby_profiles') IS NOT NULL THEN
        INSERT INTO public.persons (
            person_id, display_name, date_of_birth, created_at, updated_at
        )
        SELECT (
                   substr(md5('baby:' || bp.baby_id::text), 1, 8) || '-' ||
                   substr(md5('baby:' || bp.baby_id::text), 9, 4) || '-' ||
                   substr(md5('baby:' || bp.baby_id::text), 13, 4) || '-' ||
                   substr(md5('baby:' || bp.baby_id::text), 17, 4) || '-' ||
                   substr(md5('baby:' || bp.baby_id::text), 21, 12)
               )::uuid,
               bp.nickname, bp.birth_date, bp.created_at, bp.updated_at
          FROM public.baby_profiles bp
        ON CONFLICT (person_id) DO NOTHING;

        INSERT INTO public.care_subjects (
            care_subject_id, person_id, owner_user_id, mother_journey_id, subject_type,
            nickname, birth_date, sex, birth_weight_kg, birth_length_cm, status,
            created_at, updated_at
        )
        SELECT bp.baby_id,
               (
                   substr(md5('baby:' || bp.baby_id::text), 1, 8) || '-' ||
                   substr(md5('baby:' || bp.baby_id::text), 9, 4) || '-' ||
                   substr(md5('baby:' || bp.baby_id::text), 13, 4) || '-' ||
                   substr(md5('baby:' || bp.baby_id::text), 17, 4) || '-' ||
                   substr(md5('baby:' || bp.baby_id::text), 21, 12)
               )::uuid,
               bp.owner_user_id, bp.related_journey_id, 'BABY', bp.nickname,
               bp.birth_date, bp.sex, bp.birth_weight_kg, bp.birth_length_cm,
               CASE WHEN bp.is_active THEN bp.status ELSE 'INACTIVE' END,
               bp.created_at, bp.updated_at
          FROM public.baby_profiles bp
        ON CONFLICT (care_subject_id) DO NOTHING;

        IF (SELECT count(*) FROM public.baby_profiles) <>
           (SELECT count(*) FROM public.care_subjects WHERE subject_type = 'BABY') THEN
            RAISE EXCEPTION 'PHASE2_RECONCILIATION: baby_profiles to care_subjects mismatch';
        END IF;
    END IF;
END
$baby_mapping$;

CREATE TABLE IF NOT EXISTS public.user_identities (
    identity_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(user_id),
    provider varchar(50) NOT NULL,
    provider_subject varchar(255) NOT NULL,
    provider_email varchar(255),
    provider_phone varchar(40),
    created_at timestamptz NOT NULL DEFAULT now(),
    last_used_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_identities_provider_subject_uk UNIQUE (provider, provider_subject)
);

CREATE TABLE IF NOT EXISTS public.auth_sessions (
    session_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(user_id),
    token_family_id uuid NOT NULL,
    device_identifier varchar(255) NOT NULL,
    device_name varchar(255),
    refresh_token_hash varchar(255),
    issued_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    last_used_at timestamptz,
    rotated_at timestamptz,
    revoked_at timestamptz,
    revoke_reason varchar(100),
    status varchar(30) NOT NULL,
    created_ip_hash varchar(255),
    user_agent_hash varchar(255),
    legacy_source varchar(40),
    legacy_id varchar(100),
    CONSTRAINT auth_sessions_legacy_uk UNIQUE (legacy_source, legacy_id),
    CONSTRAINT auth_sessions_status_ck CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'EXPIRED'))
);
CREATE INDEX IF NOT EXISTS auth_sessions_user_device_ix
    ON public.auth_sessions(user_id, device_identifier, status);
CREATE INDEX IF NOT EXISTS auth_sessions_family_ix ON public.auth_sessions(token_family_id);

CREATE TABLE IF NOT EXISTS public.auth_revocations (
    revocation_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid REFERENCES public.users(user_id),
    session_id uuid REFERENCES public.auth_sessions(session_id),
    token_family_id uuid,
    token_hash varchar(255),
    reason varchar(100) NOT NULL,
    revoked_at timestamptz NOT NULL,
    expires_at timestamptz,
    detected_reuse boolean NOT NULL DEFAULT false,
    metadata_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    legacy_source varchar(40),
    legacy_id varchar(100),
    CONSTRAINT auth_revocations_legacy_uk UNIQUE (legacy_source, legacy_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS auth_revocations_token_hash_uk
    ON public.auth_revocations(token_hash) WHERE token_hash IS NOT NULL;
CREATE INDEX IF NOT EXISTS auth_revocations_user_expiry_ix
    ON public.auth_revocations(user_id, expires_at);

CREATE TABLE IF NOT EXISTS public.auth_challenges (
    challenge_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid REFERENCES public.users(user_id),
    challenge_type varchar(40) NOT NULL,
    subject_identifier varchar(255),
    challenge_hash varchar(255) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    status varchar(30) NOT NULL,
    requested_role varchar(40),
    created_at timestamptz NOT NULL DEFAULT now(),
    legacy_source varchar(40),
    legacy_id varchar(100),
    CONSTRAINT auth_challenges_legacy_uk UNIQUE (legacy_source, legacy_id),
    CONSTRAINT auth_challenges_status_ck CHECK (status IN ('PENDING', 'VERIFIED', 'USED', 'EXPIRED', 'REVOKED'))
);
CREATE INDEX IF NOT EXISTS auth_challenges_subject_expiry_ix
    ON public.auth_challenges(subject_identifier, challenge_type, expires_at);

CREATE TABLE IF NOT EXISTS public.account_deletion_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(user_id),
    status varchar(30) NOT NULL,
    reason text,
    requested_at timestamptz NOT NULL,
    scheduled_for timestamptz,
    processed_at timestamptz,
    processed_by uuid REFERENCES public.users(user_id),
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

DO $auth_mapping$
BEGIN
    IF to_regclass('public.user_sessions') IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM public.user_sessions WHERE user_id IS NULL) THEN
            RAISE EXCEPTION 'PHASE2_MAPPING: user_sessions contains null user owner';
        END IF;
        INSERT INTO public.auth_sessions (
            session_id, user_id, token_family_id, device_identifier, device_name,
            refresh_token_hash, issued_at, expires_at, last_used_at, revoked_at,
            status, created_ip_hash, user_agent_hash, legacy_source, legacy_id
        )
        SELECT us.session_id, us.user_id, us.session_id,
               coalesce(nullif(us.device_name, ''), nullif(us.browser, ''), us.session_id::text),
               us.device_name, us.refresh_token_hash, us.created_at,
               coalesce(us.expires_at, us.created_at + interval '30 days'),
               us.last_activity_at, us.revoked_at,
               CASE WHEN coalesce(us.revoked, false) OR us.revoked_at IS NOT NULL THEN 'REVOKED'
                    WHEN us.expires_at IS NOT NULL AND us.expires_at <= now() THEN 'EXPIRED'
                    ELSE 'ACTIVE' END,
               CASE WHEN us.ip_address IS NULL THEN NULL ELSE encode(sha256(convert_to(us.ip_address, 'UTF8')), 'hex') END,
               CASE WHEN us.browser IS NULL THEN NULL ELSE encode(sha256(convert_to(us.browser, 'UTF8')), 'hex') END,
               'user_sessions', us.session_id::text
          FROM public.user_sessions us
        ON CONFLICT (session_id) DO NOTHING;
        IF (SELECT count(*) FROM public.user_sessions) <>
           (SELECT count(*) FROM public.auth_sessions WHERE legacy_source = 'user_sessions') THEN
            RAISE EXCEPTION 'PHASE2_RECONCILIATION: user_sessions to auth_sessions mismatch';
        END IF;
    END IF;

    IF to_regclass('public.refresh_tokens') IS NOT NULL THEN
        INSERT INTO public.auth_sessions (
            session_id, user_id, token_family_id, device_identifier, refresh_token_hash,
            issued_at, expires_at, revoked_at, status, legacy_source, legacy_id
        )
        SELECT x.synthetic_id, rt.user_id, x.synthetic_id, 'legacy-refresh-' || rt.id,
               coalesce(rt.token_hash, encode(sha256(convert_to(rt.token, 'UTF8')), 'hex')),
               rt.created_at, rt.expires_at,
               CASE WHEN rt.revoked THEN rt.created_at ELSE NULL END,
               CASE WHEN rt.revoked THEN 'REVOKED'
                    WHEN rt.expires_at <= now() THEN 'EXPIRED' ELSE 'ACTIVE' END,
               'refresh_tokens', rt.id::text
          FROM public.refresh_tokens rt
          CROSS JOIN LATERAL (
              SELECT (
                  substr(md5('refresh:' || rt.id::text), 1, 8) || '-' ||
                  substr(md5('refresh:' || rt.id::text), 9, 4) || '-' ||
                  substr(md5('refresh:' || rt.id::text), 13, 4) || '-' ||
                  substr(md5('refresh:' || rt.id::text), 17, 4) || '-' ||
                  substr(md5('refresh:' || rt.id::text), 21, 12)
              )::uuid synthetic_id
          ) x
        ON CONFLICT (legacy_source, legacy_id) DO NOTHING;

        INSERT INTO public.auth_revocations (
            user_id, session_id, token_family_id, token_hash, reason, revoked_at,
            expires_at, legacy_source, legacy_id
        )
        SELECT rt.user_id, s.session_id, s.token_family_id, s.refresh_token_hash,
               'LEGACY_REFRESH_REVOKED', rt.created_at, rt.expires_at,
               'refresh_tokens', rt.id::text
          FROM public.refresh_tokens rt
          JOIN public.auth_sessions s
            ON s.legacy_source = 'refresh_tokens' AND s.legacy_id = rt.id::text
         WHERE rt.revoked
        ON CONFLICT (legacy_source, legacy_id) DO NOTHING;
    END IF;

    IF to_regclass('public.token_blacklist') IS NOT NULL THEN
        INSERT INTO public.auth_revocations (
            token_hash, reason, revoked_at, expires_at, legacy_source, legacy_id
        )
        SELECT tb.token_hash, coalesce(tb.reason, 'LEGACY_BLACKLIST'), tb.revoked_at,
               tb.expires_at, 'token_blacklist', tb.id::text
          FROM public.token_blacklist tb
        ON CONFLICT (token_hash) WHERE token_hash IS NOT NULL DO NOTHING;
        IF (SELECT count(*) FROM public.token_blacklist) <>
           (SELECT count(*) FROM public.auth_revocations WHERE legacy_source = 'token_blacklist') THEN
            RAISE EXCEPTION 'PHASE2_RECONCILIATION: token_blacklist to auth_revocations mismatch';
        END IF;
    END IF;

    IF to_regclass('public.otp_verifications') IS NOT NULL THEN
        INSERT INTO public.auth_challenges (
            challenge_id, user_id, challenge_type, subject_identifier, challenge_hash,
            attempts, expires_at, used_at, status, requested_role, created_at,
            legacy_source, legacy_id
        )
        SELECT (
                   substr(md5('otp:' || ov.id::text), 1, 8) || '-' ||
                   substr(md5('otp:' || ov.id::text), 9, 4) || '-' ||
                   substr(md5('otp:' || ov.id::text), 13, 4) || '-' ||
                   substr(md5('otp:' || ov.id::text), 17, 4) || '-' ||
                   substr(md5('otp:' || ov.id::text), 21, 12)
               )::uuid,
               ov.user_id, ov.purpose, coalesce(ov.email, ov.phone),
               coalesce(ov.code_hash, encode(sha256(convert_to(coalesce(ov.otp_code, ''), 'UTF8')), 'hex')),
               ov.attempts, ov.expires_at, coalesce(ov.used_at, ov.verified_at),
               CASE WHEN ov.used_at IS NOT NULL THEN 'USED'
                    WHEN ov.verified THEN 'VERIFIED'
                    WHEN ov.expires_at <= now() THEN 'EXPIRED' ELSE 'PENDING' END,
               ov.requested_role, ov.created_at, 'otp_verifications', ov.id::text
          FROM public.otp_verifications ov
        ON CONFLICT (legacy_source, legacy_id) DO NOTHING;
    END IF;

    IF to_regclass('public.password_reset_tokens') IS NOT NULL THEN
        INSERT INTO public.auth_challenges (
            challenge_id, user_id, challenge_type, challenge_hash, attempts,
            expires_at, used_at, status, created_at, legacy_source, legacy_id
        )
        SELECT prt.id, prt.user_id, 'PASSWORD_RESET', prt.token_hash, prt.attempt_count,
               prt.expires_at, prt.used_at,
               CASE WHEN prt.used_at IS NOT NULL THEN 'USED'
                    WHEN prt.expires_at <= now() THEN 'EXPIRED' ELSE 'PENDING' END,
               prt.created_at, 'password_reset_tokens', prt.id::text
          FROM public.password_reset_tokens prt
        ON CONFLICT (challenge_id) DO NOTHING;
    END IF;
END
$auth_mapping$;

CREATE OR REPLACE FUNCTION public.carebridge_reject_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: %.% does not allow UPDATE or DELETE', TG_TABLE_SCHEMA, TG_TABLE_NAME;
END
$$;

DROP TRIGGER IF EXISTS auth_revocations_immutable_trg ON public.auth_revocations;
CREATE TRIGGER auth_revocations_immutable_trg
BEFORE UPDATE OR DELETE ON public.auth_revocations
FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();

DO $final_reconcile$
BEGIN
    IF (SELECT count(*) FROM public.users) <>
       (SELECT count(*) FROM public.persons p JOIN public.users u ON u.person_id = p.person_id) THEN
        RAISE EXCEPTION 'PHASE2_RECONCILIATION: users without canonical persons';
    END IF;
END
$final_reconcile$;
