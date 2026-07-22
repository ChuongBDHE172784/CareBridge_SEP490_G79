-- Phase 2 wave 3: complete runtime/data cutover for community and expert identity.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.community_content
    ADD COLUMN IF NOT EXISTS pregnancy_week smallint,
    ADD COLUMN IF NOT EXISTS baby_age_months smallint,
    ADD COLUMN IF NOT EXISTS like_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS answer_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_expert_labeled boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_personal_experience boolean NOT NULL DEFAULT false;

ALTER TABLE public.community_interactions
    ADD COLUMN IF NOT EXISTS target_content_type varchar(20);

ALTER TABLE public.professional_profiles
    ADD COLUMN IF NOT EXISTS specialty varchar(100),
    ADD COLUMN IF NOT EXISTS facility_id uuid,
    ADD COLUMN IF NOT EXISTS trust_status varchar(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE public.expert_contribution_events ALTER COLUMN reason TYPE text;

INSERT INTO public.community_content (
    content_id,topic_id,author_user_id,content_type,title,body,stage,pregnancy_week,
    baby_age_months,urgency,is_anonymous,moderation_status,like_count,answer_count,
    created_at,updated_at)
SELECT q.id,q.topic_id,q.author_id,'QUESTION',q.title,q.body,q.stage,q.pregnancy_week,
       q.baby_age_months,q.urgency,q.is_anonymous,q.status,q.like_count,q.answer_count,
       q.created_at,q.updated_at
  FROM public.community_questions q
ON CONFLICT (content_id) DO UPDATE SET
    topic_id=excluded.topic_id,author_user_id=excluded.author_user_id,title=excluded.title,
    body=excluded.body,stage=excluded.stage,pregnancy_week=excluded.pregnancy_week,
    baby_age_months=excluded.baby_age_months,urgency=excluded.urgency,
    is_anonymous=excluded.is_anonymous,moderation_status=excluded.moderation_status,
    like_count=excluded.like_count,answer_count=excluded.answer_count,
    created_at=excluded.created_at,updated_at=excluded.updated_at;

INSERT INTO public.community_content (
    content_id,parent_content_id,author_user_id,content_type,body,is_expert_labeled,
    is_personal_experience,moderation_status,like_count,created_at,updated_at)
SELECT a.id,a.question_id,a.author_id,'ANSWER',a.body,a.is_expert_labeled,
       a.is_personal_experience,a.status,a.like_count,a.created_at,a.updated_at
  FROM public.community_answers a
ON CONFLICT (content_id) DO UPDATE SET
    parent_content_id=excluded.parent_content_id,author_user_id=excluded.author_user_id,
    body=excluded.body,is_expert_labeled=excluded.is_expert_labeled,
    is_personal_experience=excluded.is_personal_experience,
    moderation_status=excluded.moderation_status,like_count=excluded.like_count,
    created_at=excluded.created_at,updated_at=excluded.updated_at;

-- Runtime was still legacy before this migration, so rebuild the canonical projection exactly.
DELETE FROM public.community_interactions;

INSERT INTO public.community_interactions
    (interaction_id,actor_user_id,interaction_type,content_id,target_content_type,created_at)
SELECT id,user_id,'REACTION',question_id,'QUESTION',created_at FROM public.community_question_likes;
INSERT INTO public.community_interactions
    (interaction_id,actor_user_id,interaction_type,content_id,target_content_type,created_at)
SELECT id,user_id,'REACTION',answer_id,'ANSWER',created_at FROM public.community_answer_likes;
INSERT INTO public.community_interactions
    (interaction_id,actor_user_id,interaction_type,content_id,target_content_type,created_at)
SELECT id,user_id,'BOOKMARK',question_id,'QUESTION',created_at FROM public.community_bookmarks;
INSERT INTO public.community_interactions
    (interaction_id,actor_user_id,interaction_type,topic_id,created_at)
SELECT id,user_id,'FOLLOW',topic_id,created_at FROM public.user_topic_follows;
INSERT INTO public.community_interactions
    (interaction_id,actor_user_id,interaction_type,content_id,target_content_type,created_at)
SELECT id,user_id,'MUTE',question_id,'QUESTION',created_at FROM public.question_notification_mutes;

INSERT INTO public.professional_profiles (
    professional_profile_id,user_id,specialty,professional_title,experience_years,workplace,
    facility_id,consultation_scope,verification_status,trust_status,verified_at,verified_by,
    rating_avg,created_at,updated_at)
SELECT expert_profile_id,user_id,specialty,professional_title,experience_years,workplace,
       facility_id,consultation_scope,verification_status,trust_status,verified_at,verified_by,
       rating_avg,created_at,updated_at
  FROM public.expert_profiles
ON CONFLICT (professional_profile_id) DO UPDATE SET
    user_id=excluded.user_id,specialty=excluded.specialty,
    professional_title=excluded.professional_title,experience_years=excluded.experience_years,
    workplace=excluded.workplace,facility_id=excluded.facility_id,
    consultation_scope=excluded.consultation_scope,
    verification_status=excluded.verification_status,trust_status=excluded.trust_status,
    verified_at=excluded.verified_at,verified_by=excluded.verified_by,
    rating_avg=excluded.rating_avg,created_at=excluded.created_at,updated_at=excluded.updated_at;

INSERT INTO public.specialties (specialty_id,code,name,is_active,created_at)
SELECT (substr(md5('specialty:'||lower(trim(specialty))),1,8)||'-'||
        substr(md5('specialty:'||lower(trim(specialty))),9,4)||'-'||
        substr(md5('specialty:'||lower(trim(specialty))),13,4)||'-'||
        substr(md5('specialty:'||lower(trim(specialty))),17,4)||'-'||
        substr(md5('specialty:'||lower(trim(specialty))),21,12))::uuid,
       left(lower(regexp_replace(trim(specialty),'[^[:alnum:]]+','_','g')),80),
       trim(specialty),true,min(created_at)
  FROM public.expert_profiles
 WHERE specialty IS NOT NULL AND trim(specialty)<>''
 GROUP BY lower(trim(specialty)),trim(specialty)
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.professional_specialties
    (professional_profile_id,specialty_id,is_primary,created_at)
SELECT ep.expert_profile_id,s.specialty_id,true,ep.created_at
  FROM public.expert_profiles ep
  JOIN public.specialties s
    ON s.code=left(lower(regexp_replace(trim(ep.specialty),'[^[:alnum:]]+','_','g')),80)
 WHERE ep.specialty IS NOT NULL AND trim(ep.specialty)<>''
ON CONFLICT (professional_profile_id,specialty_id) DO NOTHING;

UPDATE public.expert_credentials ec SET professional_profile_id=ec.expert_profile_id
 WHERE ec.professional_profile_id IS NULL;
UPDATE public.expert_availability ea SET professional_profile_id=ea.expert_profile_id
 WHERE ea.professional_profile_id IS NULL;
UPDATE public.expert_location_shares els SET professional_profile_id=els.expert_profile_id
 WHERE els.professional_profile_id IS NULL;

INSERT INTO public.expert_contribution_events (
    contribution_event_id,professional_profile_id,actor_user_id,points,reason,
    source_type,source_id,created_at)
SELECT cp.point_record_id,pp.professional_profile_id,cp.user_id,coalesce(cp.points,0),
       coalesce(cp.reason,'LEGACY_CONTRIBUTION'),cp.source_type,cp.source_id,
       coalesce(cp.recorded_at,now())
  FROM public.contribution_points cp
  LEFT JOIN public.professional_profiles pp ON pp.user_id=cp.user_id
ON CONFLICT (contribution_event_id) DO NOTHING;

DO $wave3_reconcile$
BEGIN
  IF (SELECT count(*) FROM public.community_questions) <>
     (SELECT count(*) FROM public.community_content WHERE content_type='QUESTION') THEN
    RAISE EXCEPTION 'WAVE3_RECONCILIATION: community questions';
  END IF;
  IF (SELECT count(*) FROM public.community_answers) <>
     (SELECT count(*) FROM public.community_content WHERE content_type='ANSWER') THEN
    RAISE EXCEPTION 'WAVE3_RECONCILIATION: community answers';
  END IF;
  IF (SELECT count(*) FROM public.community_question_likes) <>
     (SELECT count(*) FROM public.community_interactions WHERE interaction_type='REACTION' AND target_content_type='QUESTION') THEN
    RAISE EXCEPTION 'WAVE3_RECONCILIATION: question reactions';
  END IF;
  IF (SELECT count(*) FROM public.community_answer_likes) <>
     (SELECT count(*) FROM public.community_interactions WHERE interaction_type='REACTION' AND target_content_type='ANSWER') THEN
    RAISE EXCEPTION 'WAVE3_RECONCILIATION: answer reactions';
  END IF;
  IF (SELECT count(*) FROM public.community_bookmarks) <>
     (SELECT count(*) FROM public.community_interactions WHERE interaction_type='BOOKMARK') THEN
    RAISE EXCEPTION 'WAVE3_RECONCILIATION: bookmarks';
  END IF;
  IF (SELECT count(*) FROM public.user_topic_follows) <>
     (SELECT count(*) FROM public.community_interactions WHERE interaction_type='FOLLOW') THEN
    RAISE EXCEPTION 'WAVE3_RECONCILIATION: follows';
  END IF;
  IF (SELECT count(*) FROM public.question_notification_mutes) <>
     (SELECT count(*) FROM public.community_interactions WHERE interaction_type='MUTE') THEN
    RAISE EXCEPTION 'WAVE3_RECONCILIATION: mutes';
  END IF;
  IF (SELECT count(*) FROM public.expert_profiles) <>
     (SELECT count(*) FROM public.professional_profiles) THEN
    RAISE EXCEPTION 'WAVE3_RECONCILIATION: professional profiles';
  END IF;
  IF (SELECT count(*) FROM public.contribution_points) <>
     (SELECT count(*) FROM public.expert_contribution_events) THEN
    RAISE EXCEPTION 'WAVE3_RECONCILIATION: contribution events';
  END IF;
END $wave3_reconcile$;

DROP INDEX IF EXISTS public.community_interactions_unique_target_uk;
CREATE UNIQUE INDEX community_interactions_content_target_uk
  ON public.community_interactions(actor_user_id,interaction_type,content_id)
  WHERE content_id IS NOT NULL;
CREATE UNIQUE INDEX community_interactions_topic_target_uk
  ON public.community_interactions(actor_user_id,interaction_type,topic_id)
  WHERE topic_id IS NOT NULL;

DO $wave3_retarget_fks$
DECLARE m record; c record; new_def text;
BEGIN
  FOR m IN SELECT * FROM (VALUES
    ('community_questions','community_content','id','content_id'),
    ('community_answers','community_content','id','content_id'),
    ('community_question_likes','community_interactions','id','interaction_id'),
    ('community_answer_likes','community_interactions','id','interaction_id'),
    ('community_bookmarks','community_interactions','id','interaction_id'),
    ('user_topic_follows','community_interactions','id','interaction_id'),
    ('question_notification_mutes','community_interactions','id','interaction_id'),
    ('expert_profiles','professional_profiles','expert_profile_id','professional_profile_id'),
    ('contribution_points','expert_contribution_events','point_record_id','contribution_event_id')
  ) x(source_table,target_table,source_key,target_key)
  LOOP
    FOR c IN SELECT conrelid::regclass AS rel,conname,pg_get_constraintdef(oid) AS def
      FROM pg_constraint WHERE contype='f' AND confrelid=to_regclass('public.'||m.source_table)
    LOOP
      new_def:=replace(c.def,m.source_table,m.target_table);
      new_def:=replace(new_def,m.target_table||'('||m.source_key||')',m.target_table||'('||m.target_key||')');
      IF new_def=c.def THEN RAISE EXCEPTION 'WAVE3_DEPENDENCY: cannot retarget %.%',c.rel,c.conname; END IF;
      EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',c.rel,c.conname);
      EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s',c.rel,c.conname,new_def);
    END LOOP;
  END LOOP;
END $wave3_retarget_fks$;

ALTER TABLE public.expert_credentials ALTER COLUMN professional_profile_id SET NOT NULL;
ALTER TABLE public.expert_availability ALTER COLUMN professional_profile_id SET NOT NULL;
ALTER TABLE public.expert_location_shares ALTER COLUMN professional_profile_id SET NOT NULL;

DO $wave3_canonical_fks$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='professional_profiles_facility_fk') THEN
    ALTER TABLE public.professional_profiles ADD CONSTRAINT professional_profiles_facility_fk
      FOREIGN KEY (facility_id) REFERENCES public.care_facilities(facility_id);
  END IF;
END $wave3_canonical_fks$;

CREATE OR REPLACE FUNCTION public.fill_contribution_profile()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.professional_profile_id IS NULL THEN
    SELECT professional_profile_id INTO NEW.professional_profile_id
      FROM public.professional_profiles WHERE user_id=NEW.actor_user_id;
  END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS expert_contribution_fill_profile_trg ON public.expert_contribution_events;
CREATE TRIGGER expert_contribution_fill_profile_trg
BEFORE INSERT ON public.expert_contribution_events
FOR EACH ROW EXECUTE FUNCTION public.fill_contribution_profile();

DROP TRIGGER IF EXISTS expert_contribution_events_immutable_trg ON public.expert_contribution_events;
CREATE TRIGGER expert_contribution_events_immutable_trg
BEFORE UPDATE OR DELETE ON public.expert_contribution_events
FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();

DROP TABLE IF EXISTS public.community_question_likes;
DROP TABLE IF EXISTS public.community_answer_likes;
DROP TABLE IF EXISTS public.community_bookmarks;
DROP TABLE IF EXISTS public.user_topic_follows;
DROP TABLE IF EXISTS public.question_notification_mutes;
DROP TABLE IF EXISTS public.community_answers;
DROP TABLE IF EXISTS public.community_questions;
DROP TABLE IF EXISTS public.contribution_points;
DROP TABLE IF EXISTS public.expert_profiles;

DO $wave3_absence_gate$
DECLARE name text;
BEGIN
  FOREACH name IN ARRAY ARRAY[
    'community_questions','community_answers','community_question_likes',
    'community_answer_likes','community_bookmarks','user_topic_follows',
    'question_notification_mutes','expert_profiles','contribution_points']
  LOOP
    IF to_regclass('public.'||name) IS NOT NULL THEN
      RAISE EXCEPTION 'WAVE3_DROP_FAILED: %',name;
    END IF;
  END LOOP;
END $wave3_absence_gate$;
