-- Phase 2 wave 1: canonical care-subject, account and authentication.
-- This migration is safe on both an empty target bootstrap and the legacy upgrade path.
-- Legacy tables are intentionally retained until application cutover evidence is complete.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE TABLE IF NOT EXISTS public.users (
    user_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
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
    display_name varchar(200),
    date_of_birth date,
    phone_number varchar(40),
    area varchar(200),
    bio varchar(500),
    interest_stage varchar(30),
    is_visible boolean,
    public_avatar_url varchar(500),
    region varchar(120),
    social_identities jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.users ADD COLUMN IF NOT EXISTS settings_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS display_name varchar(200);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS date_of_birth date;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS phone_number varchar(40);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS area varchar(200);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS bio varchar(500);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS interest_stage varchar(30);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_visible boolean;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS public_avatar_url varchar(500);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS region varchar(120);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS social_identities jsonb NOT NULL DEFAULT '[]'::jsonb;

DO $profile_mapping$
BEGIN
    IF to_regclass('public.user_profiles') IS NOT NULL THEN
        UPDATE public.users u
           SET display_name = coalesce(up.display_name, u.full_name),
               date_of_birth = up.date_of_birth,
               phone_number = coalesce(up.phone_number, u.phone),
               area = up.area
          FROM public.user_profiles up
         WHERE up.user_id = u.user_id;
    END IF;
    
    IF to_regclass('public.community_profiles') IS NOT NULL THEN
        UPDATE public.users u
           SET bio = cp.bio,
               interest_stage = cp.interest_stage,
               is_visible = cp.is_visible,
               public_avatar_url = cp.public_avatar_url,
               region = cp.region
          FROM public.community_profiles cp
         WHERE cp.user_id = u.user_id;
    END IF;
END
$profile_mapping$;

UPDATE public.users SET display_name = full_name WHERE display_name IS NULL;
UPDATE public.users SET phone_number = phone WHERE phone_number IS NULL;

CREATE TABLE IF NOT EXISTS public.care_subjects (
    care_subject_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id uuid NOT NULL REFERENCES public.users(user_id),
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
        INSERT INTO public.users (
            user_id, display_name, date_of_birth, created_at, updated_at
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
        ON CONFLICT (user_id) DO NOTHING;

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

CREATE TABLE IF NOT EXISTS public.auth_sessions (
    session_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid REFERENCES public.users(user_id),
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
    detected_reuse boolean NOT NULL DEFAULT false,
    revocation_metadata_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT auth_sessions_legacy_uk UNIQUE (legacy_source, legacy_id),
    CONSTRAINT auth_sessions_status_ck CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'EXPIRED'))
);
CREATE INDEX IF NOT EXISTS auth_sessions_user_device_ix
    ON public.auth_sessions(user_id, device_identifier, status);
CREATE INDEX IF NOT EXISTS auth_sessions_family_ix ON public.auth_sessions(token_family_id);

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

        UPDATE public.auth_sessions s
           SET status = 'REVOKED',
               revoked_at = rt.created_at,
               revoke_reason = 'LEGACY_REFRESH_REVOKED'
          FROM public.refresh_tokens rt
         WHERE s.legacy_source = 'refresh_tokens'
           AND s.legacy_id = rt.id::text
           AND rt.revoked;
    END IF;

    IF to_regclass('public.token_blacklist') IS NOT NULL THEN
        INSERT INTO public.auth_sessions (
            session_id, token_family_id, device_identifier, refresh_token_hash,
            issued_at, expires_at, revoked_at, status, legacy_source, legacy_id,
            revoke_reason
        )
        SELECT (
                   substr(md5('blacklist:' || tb.id::text), 1, 8) || '-' ||
                   substr(md5('blacklist:' || tb.id::text), 9, 4) || '-' ||
                   substr(md5('blacklist:' || tb.id::text), 13, 4) || '-' ||
                   substr(md5('blacklist:' || tb.id::text), 17, 4) || '-' ||
                   substr(md5('blacklist:' || tb.id::text), 21, 12)
               )::uuid,
               (
                   substr(md5('blacklist_family:' || tb.id::text), 1, 8) || '-' ||
                   substr(md5('blacklist_family:' || tb.id::text), 9, 4) || '-' ||
                   substr(md5('blacklist_family:' || tb.id::text), 13, 4) || '-' ||
                   substr(md5('blacklist_family:' || tb.id::text), 17, 4) || '-' ||
                   substr(md5('blacklist_family:' || tb.id::text), 21, 12)
               )::uuid,
               'legacy-blacklist-' || tb.id,
               tb.token_hash,
               tb.revoked_at,
               tb.expires_at,
               tb.revoked_at,
               'REVOKED',
               'token_blacklist',
               tb.id::text,
               coalesce(tb.reason, 'LEGACY_BLACKLIST')
          FROM public.token_blacklist tb
        ON CONFLICT (legacy_source, legacy_id) DO NOTHING;
        
        IF (SELECT count(*) FROM public.token_blacklist) <>
           (SELECT count(*) FROM public.auth_sessions WHERE legacy_source = 'token_blacklist') THEN
            RAISE EXCEPTION 'PHASE2_RECONCILIATION: token_blacklist to auth_sessions mismatch';
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

CREATE TABLE IF NOT EXISTS public.audit_events (
    audit_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id uuid REFERENCES public.users(user_id),
    event_category varchar(80) NOT NULL,
    subject_user_id uuid REFERENCES public.users(user_id),
    subject_reference_id uuid,
    resource_type varchar(100),
    resource_id uuid,
    purpose varchar(255),
    decision varchar(50),
    ip_hash varchar(128),
    ip_address varchar(80),
    user_agent varchar(500),
    before_payload_jsonb jsonb,
    after_payload_jsonb jsonb,
    payload jsonb,
    correlation_id uuid,
    severity varchar(20) NOT NULL DEFAULT 'MEDIUM',
    status varchar(20) NOT NULL DEFAULT 'OPEN',
    reviewed_by uuid REFERENCES public.users(user_id),
    reviewed_at timestamptz,
    checksum varchar(128),
    note_text text,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS audit_events_subject_time_ix ON public.audit_events(subject_user_id, occurred_at);
CREATE INDEX IF NOT EXISTS audit_events_category_time_ix ON public.audit_events(event_category, occurred_at);

DROP TRIGGER IF EXISTS audit_events_immutable_trg ON public.audit_events;
CREATE TRIGGER audit_events_immutable_trg
BEFORE UPDATE OR DELETE ON public.audit_events
FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();

-- final reconciliation check is not needed since persons is merged into users
