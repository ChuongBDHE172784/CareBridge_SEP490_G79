-- Phase 2 application cutover, wave 1: final legacy account/auth cleanup.
-- No CASCADE is used. Every live row is copied and reconciled before removal.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

DO $wave1_copy$
BEGIN
    IF to_regclass('public.user_profiles') IS NOT NULL THEN
        INSERT INTO persons (person_id, display_name, date_of_birth, phone_number, avatar_url, area, created_at, updated_at)
        SELECT u.user_id, coalesce(up.display_name, u.full_name), up.date_of_birth,
               coalesce(up.phone_number, u.phone), coalesce(up.avatar_url, u.avatar_url), up.area,
               least(u.created_at, up.created_at), greatest(u.updated_at, up.updated_at)
          FROM user_profiles up JOIN users u ON u.user_id=up.user_id
        ON CONFLICT (person_id) DO UPDATE SET
          display_name=excluded.display_name, date_of_birth=excluded.date_of_birth,
          phone_number=excluded.phone_number, avatar_url=excluded.avatar_url,
          area=excluded.area, updated_at=excluded.updated_at;
        IF (SELECT count(*) FROM user_profiles) <>
           (SELECT count(*) FROM user_profiles up JOIN users u ON u.user_id=up.user_id JOIN persons p ON p.person_id=u.person_id) THEN
            RAISE EXCEPTION 'WAVE1_RECONCILIATION: user profile rows';
        END IF;
    END IF;

    IF to_regclass('public.baby_profiles') IS NOT NULL THEN
        INSERT INTO persons(person_id, display_name, date_of_birth, created_at, updated_at)
        SELECT (substr(md5('baby:'||baby_id),1,8)||'-'||substr(md5('baby:'||baby_id),9,4)||'-'||
                substr(md5('baby:'||baby_id),13,4)||'-'||substr(md5('baby:'||baby_id),17,4)||'-'||
                substr(md5('baby:'||baby_id),21,12))::uuid,
               nickname,birth_date,created_at,updated_at FROM baby_profiles
        ON CONFLICT (person_id) DO UPDATE SET display_name=excluded.display_name,
          date_of_birth=excluded.date_of_birth, updated_at=excluded.updated_at;

        INSERT INTO care_subjects(care_subject_id,person_id,owner_user_id,mother_journey_id,subject_type,
          nickname,birth_date,sex,birth_weight_kg,birth_length_cm,status,created_at,updated_at)
        SELECT baby_id,(substr(md5('baby:'||baby_id),1,8)||'-'||substr(md5('baby:'||baby_id),9,4)||'-'||
                substr(md5('baby:'||baby_id),13,4)||'-'||substr(md5('baby:'||baby_id),17,4)||'-'||
                substr(md5('baby:'||baby_id),21,12))::uuid,
               owner_user_id,related_journey_id,'BABY',nickname,birth_date,sex,birth_weight_kg,birth_length_cm,
               CASE WHEN is_active THEN status ELSE 'INACTIVE' END,created_at,updated_at FROM baby_profiles
        ON CONFLICT (care_subject_id) DO UPDATE SET owner_user_id=excluded.owner_user_id,
          mother_journey_id=excluded.mother_journey_id,nickname=excluded.nickname,birth_date=excluded.birth_date,
          sex=excluded.sex,birth_weight_kg=excluded.birth_weight_kg,birth_length_cm=excluded.birth_length_cm,
          status=excluded.status,updated_at=excluded.updated_at;
        IF (SELECT count(*) FROM baby_profiles) <>
           (SELECT count(*) FROM baby_profiles bp JOIN care_subjects cs ON cs.care_subject_id=bp.baby_id AND cs.subject_type='BABY') THEN
            RAISE EXCEPTION 'WAVE1_RECONCILIATION: baby rows';
        END IF;
    END IF;

    IF to_regclass('public.notification_preferences') IS NOT NULL THEN
        WITH per_user AS (
          SELECT user_id, jsonb_object_agg(notification_type,
            jsonb_build_object('pushEnabled',coalesce(push_enabled,true),'emailEnabled',coalesce(email_enabled,true),
                               'inAppEnabled',coalesce(in_app_enabled,true))) payload
          FROM notification_preferences GROUP BY user_id
        )
        UPDATE users u SET settings_jsonb=jsonb_set(coalesce(u.settings_jsonb,'{}'),'{notifications}',p.payload,true)
          FROM per_user p WHERE p.user_id=u.user_id;
        IF EXISTS (SELECT 1 FROM notification_preferences np JOIN users u ON u.user_id=np.user_id
                    WHERE NOT (u.settings_jsonb->'notifications' ? np.notification_type)) THEN
            RAISE EXCEPTION 'WAVE1_RECONCILIATION: notification settings';
        END IF;
    END IF;

    IF to_regclass('public.privacy_settings') IS NOT NULL THEN
        UPDATE users u SET settings_jsonb=jsonb_set(coalesce(u.settings_jsonb,'{}'),'{privacy}',
          jsonb_build_object('profileVisibility',p.profile_visibility,
            'locationSharingEnabled',p.location_sharing_enabled,'analyticsConsent',p.analytics_consent,
            'dataExportOptOut',p.data_export_opt_out),true)
          FROM privacy_settings p WHERE p.user_id=u.user_id;
        IF EXISTS (SELECT 1 FROM privacy_settings p JOIN users u ON u.user_id=p.user_id
                    WHERE u.settings_jsonb->'privacy' IS NULL) THEN
            RAISE EXCEPTION 'WAVE1_RECONCILIATION: privacy settings';
        END IF;
    END IF;

    IF to_regclass('public.user_sessions') IS NOT NULL THEN
        INSERT INTO auth_sessions(session_id,user_id,token_family_id,device_identifier,device_name,refresh_token_hash,
          issued_at,expires_at,last_used_at,revoked_at,status,created_ip_hash,user_agent_hash,legacy_source,legacy_id)
        SELECT session_id,user_id,session_id,coalesce(nullif(device_name,''),nullif(browser,''),session_id::text),
          device_name,refresh_token_hash,created_at,expires_at,last_activity_at,revoked_at,
          CASE WHEN coalesce(revoked,false) OR revoked_at IS NOT NULL THEN 'REVOKED'
               WHEN expires_at<=now() THEN 'EXPIRED' ELSE 'ACTIVE' END,
          CASE WHEN ip_address IS NULL THEN NULL ELSE encode(sha256(convert_to(ip_address,'UTF8')),'hex') END,
          CASE WHEN browser IS NULL THEN NULL ELSE encode(sha256(convert_to(browser,'UTF8')),'hex') END,
          'user_sessions',session_id::text FROM user_sessions
        ON CONFLICT (session_id) DO UPDATE SET refresh_token_hash=excluded.refresh_token_hash,
          expires_at=excluded.expires_at,last_used_at=excluded.last_used_at,revoked_at=excluded.revoked_at,status=excluded.status;
        IF (SELECT count(*) FROM user_sessions) <>
           (SELECT count(*) FROM auth_sessions WHERE legacy_source='user_sessions') THEN
          RAISE EXCEPTION 'WAVE1_RECONCILIATION: sessions';
        END IF;
    END IF;

    IF to_regclass('public.refresh_tokens') IS NOT NULL THEN
        INSERT INTO auth_sessions(session_id,user_id,token_family_id,device_identifier,refresh_token_hash,
          issued_at,expires_at,revoked_at,status,legacy_source,legacy_id)
        SELECT x.id,rt.user_id,x.id,'legacy-refresh-'||rt.id,coalesce(rt.token_hash,encode(sha256(convert_to(rt.token,'UTF8')),'hex')),
          rt.created_at,rt.expires_at,CASE WHEN rt.revoked THEN rt.created_at END,
          CASE WHEN rt.revoked THEN 'REVOKED' WHEN rt.expires_at<=now() THEN 'EXPIRED' ELSE 'ACTIVE' END,
          'refresh_tokens',rt.id::text FROM refresh_tokens rt CROSS JOIN LATERAL
          (SELECT (substr(md5('refresh:'||rt.id),1,8)||'-'||substr(md5('refresh:'||rt.id),9,4)||'-'||
                   substr(md5('refresh:'||rt.id),13,4)||'-'||substr(md5('refresh:'||rt.id),17,4)||'-'||
                   substr(md5('refresh:'||rt.id),21,12))::uuid id) x
        ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
        IF (SELECT count(*) FROM refresh_tokens) <>
           (SELECT count(*) FROM auth_sessions WHERE legacy_source='refresh_tokens') THEN
          RAISE EXCEPTION 'WAVE1_RECONCILIATION: refresh tokens';
        END IF;
    END IF;

    IF to_regclass('public.token_blacklist') IS NOT NULL THEN
        INSERT INTO auth_revocations(token_hash,reason,revoked_at,expires_at,legacy_source,legacy_id)
        SELECT token_hash,coalesce(reason,'LEGACY_BLACKLIST'),coalesce(revoked_at,now()),expires_at,'token_blacklist',id::text
          FROM token_blacklist ON CONFLICT (token_hash) WHERE token_hash IS NOT NULL DO NOTHING;
        IF EXISTS (SELECT 1 FROM token_blacklist t LEFT JOIN auth_revocations a ON a.token_hash=t.token_hash
                   WHERE a.revocation_id IS NULL) THEN RAISE EXCEPTION 'WAVE1_RECONCILIATION: revocations'; END IF;
    END IF;

    IF to_regclass('public.otp_verifications') IS NOT NULL THEN
        INSERT INTO auth_challenges(challenge_id,user_id,challenge_type,subject_identifier,challenge_hash,attempts,
          expires_at,used_at,status,requested_role,created_at,legacy_source,legacy_id)
        SELECT (substr(md5('otp:'||id),1,8)||'-'||substr(md5('otp:'||id),9,4)||'-'||substr(md5('otp:'||id),13,4)||'-'||
                substr(md5('otp:'||id),17,4)||'-'||substr(md5('otp:'||id),21,12))::uuid,
          user_id,purpose,coalesce(email,phone),coalesce(code_hash,encode(sha256(convert_to(coalesce(otp_code,''),'UTF8')),'hex')),
          attempts,expires_at,coalesce(used_at,verified_at),CASE WHEN used_at IS NOT NULL THEN 'USED'
          WHEN verified THEN 'VERIFIED' WHEN expires_at<=now() THEN 'EXPIRED' ELSE 'PENDING' END,
          requested_role,created_at,'otp_verifications',id::text FROM otp_verifications
        ON CONFLICT (legacy_source,legacy_id) DO UPDATE SET attempts=excluded.attempts,used_at=excluded.used_at,status=excluded.status;
        IF (SELECT count(*) FROM otp_verifications) <>
           (SELECT count(*) FROM auth_challenges WHERE legacy_source='otp_verifications') THEN
          RAISE EXCEPTION 'WAVE1_RECONCILIATION: otp challenges';
        END IF;
    END IF;

    IF to_regclass('public.password_reset_tokens') IS NOT NULL THEN
        INSERT INTO auth_challenges(challenge_id,user_id,challenge_type,challenge_hash,attempts,expires_at,used_at,status,
          created_at,legacy_source,legacy_id)
        SELECT id,user_id,'PASSWORD_RESET',token_hash,attempt_count,expires_at,used_at,
          CASE WHEN used_at IS NOT NULL THEN 'USED' WHEN expires_at<=now() THEN 'EXPIRED' ELSE 'PENDING' END,
          created_at,'password_reset_tokens',id::text FROM password_reset_tokens
        ON CONFLICT (challenge_id) DO UPDATE SET attempts=excluded.attempts,used_at=excluded.used_at,status=excluded.status;
        IF (SELECT count(*) FROM password_reset_tokens) <>
           (SELECT count(*) FROM auth_challenges WHERE legacy_source='password_reset_tokens') THEN
          RAISE EXCEPTION 'WAVE1_RECONCILIATION: password challenges';
        END IF;
    END IF;
END $wave1_copy$;

-- Preserve every legacy baby FK by retargeting the identical UUID key to care_subjects.
DO $retarget_baby_fks$
DECLARE c record; new_def text;
BEGIN
  IF to_regclass('public.baby_profiles') IS NOT NULL THEN
    FOR c IN SELECT oid,conrelid::regclass AS rel,conname,pg_get_constraintdef(oid) AS def
      FROM pg_constraint WHERE contype='f' AND confrelid='public.baby_profiles'::regclass
    LOOP
      new_def:=replace(c.def,'REFERENCES baby_profiles(baby_id)','REFERENCES care_subjects(care_subject_id)');
      new_def:=replace(new_def,'REFERENCES public.baby_profiles(baby_id)','REFERENCES public.care_subjects(care_subject_id)');
      IF new_def=c.def THEN RAISE EXCEPTION 'WAVE1_DEPENDENCY: cannot retarget %.%',c.rel,c.conname; END IF;
      EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',c.rel,c.conname);
      EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s',c.rel,c.conname,new_def);
    END LOOP;
  END IF;
END $retarget_baby_fks$;

DO $dependency_gate$
DECLARE legacy regclass; name text;
BEGIN
  FOREACH name IN ARRAY ARRAY['user_profiles','baby_profiles','refresh_tokens','user_sessions','token_blacklist',
    'notification_preferences','privacy_settings','otp_verifications','password_reset_tokens','roles','user_roles']
  LOOP
    legacy:=to_regclass('public.'||name);
    IF legacy IS NOT NULL AND EXISTS (SELECT 1 FROM pg_constraint WHERE confrelid=legacy) THEN
      RAISE EXCEPTION 'WAVE1_DEPENDENCY: inbound FK remains for %',name;
    END IF;
  END LOOP;
END $dependency_gate$;

DROP TABLE IF EXISTS public.user_profiles;
DROP TABLE IF EXISTS public.baby_profiles;
DROP TABLE IF EXISTS public.refresh_tokens;
DROP TABLE IF EXISTS public.user_sessions;
DROP TABLE IF EXISTS public.token_blacklist;
DROP TABLE IF EXISTS public.notification_preferences;
DROP TABLE IF EXISTS public.privacy_settings;
DROP TABLE IF EXISTS public.otp_verifications;
DROP TABLE IF EXISTS public.password_reset_tokens;
DROP TABLE IF EXISTS public.user_roles;
DROP TABLE IF EXISTS public.roles;

DO $absence_gate$
DECLARE name text;
BEGIN
  FOREACH name IN ARRAY ARRAY['user_profiles','baby_profiles','refresh_tokens','user_sessions','token_blacklist',
    'notification_preferences','privacy_settings','otp_verifications','password_reset_tokens','roles','user_roles']
  LOOP
    IF to_regclass('public.'||name) IS NOT NULL THEN RAISE EXCEPTION 'WAVE1_DROP_FAILED: %',name; END IF;
  END LOOP;
END $absence_gate$;
