-- Phase 2 wave 7 cutover: verified sources, moderation lifecycle, and security notes.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.moderation_cases
  ADD COLUMN IF NOT EXISTS report_source varchar(20) NOT NULL DEFAULT 'USER',
  ADD COLUMN IF NOT EXISTS reverted_at timestamptz,
  ADD COLUMN IF NOT EXISTS reverted_by uuid;

ALTER TABLE public.moderation_events
  ALTER COLUMN moderation_case_id DROP NOT NULL;

ALTER TABLE public.audit_events
  ADD COLUMN IF NOT EXISTS security_event_id bigint,
  ADD COLUMN IF NOT EXISTS note_text text;

INSERT INTO public.moderation_cases (
    moderation_case_id,reporter_user_id,assigned_moderator_id,target_type,target_id,
    reason_code,report_source,description,status,opened_at,resolved_at,updated_at,
    reverted_at,reverted_by)
SELECT report_id,reporter_user_id,assigned_moderator_id,coalesce(target_type,'CONTENT'),
       target_id,category,report_source,description,status,created_at,resolved_at,
       coalesce(updated_at,created_at),reverted_at,reverted_by
  FROM public.content_reports
ON CONFLICT (moderation_case_id) DO UPDATE SET
  reporter_user_id=excluded.reporter_user_id,
  assigned_moderator_id=excluded.assigned_moderator_id,target_type=excluded.target_type,
  target_id=excluded.target_id,reason_code=excluded.reason_code,
  report_source=excluded.report_source,description=excluded.description,status=excluded.status,
  resolved_at=excluded.resolved_at,updated_at=excluded.updated_at,
  reverted_at=excluded.reverted_at,reverted_by=excluded.reverted_by;

INSERT INTO public.moderation_events (
    moderation_event_id,moderation_case_id,moderator_user_id,action_type,
    target_type,target_id,reason,expires_at,action_at)
SELECT moderation_action_id,report_id,moderator_user_id,coalesce(action_type,'REVIEW'),
       coalesce(target_type,'CONTENT'),target_id,reason,expires_at,coalesce(action_at,now())
  FROM public.moderation_actions
ON CONFLICT (moderation_event_id) DO NOTHING;

INSERT INTO public.content_item_sources (
    content_item_id,source_title,source_url,source_publisher)
SELECT content_item_id,source_title,source_url,source_publisher
  FROM public.content_sources
ON CONFLICT DO NOTHING;

INSERT INTO public.audit_events (
    audit_event_id,actor_user_id,event_category,resource_type,purpose,
    security_event_id,note_text,occurred_at,created_at)
SELECT note_id,author_id,'SECURITY_INVESTIGATION_NOTE','SECURITY_EVENT',
       'SECURITY_INVESTIGATION',event_id,note_text,created_at,created_at
  FROM public.security_event_notes
ON CONFLICT (audit_event_id) DO NOTHING;

DO $wave7_reconcile$
BEGIN
  IF (SELECT count(*) FROM public.content_reports) <>
     (SELECT count(*) FROM public.moderation_cases) THEN
    RAISE EXCEPTION 'WAVE7_RECONCILIATION: moderation cases';
  END IF;
  IF (SELECT count(*) FROM public.moderation_actions) <>
     (SELECT count(*) FROM public.moderation_events) THEN
    RAISE EXCEPTION 'WAVE7_RECONCILIATION: moderation events';
  END IF;
  IF (SELECT count(*) FROM public.content_sources) <>
     (SELECT count(*) FROM public.content_item_sources) THEN
    RAISE EXCEPTION 'WAVE7_RECONCILIATION: content sources';
  END IF;
  IF (SELECT count(*) FROM public.security_event_notes) <>
     (SELECT count(*) FROM public.audit_events WHERE event_category='SECURITY_INVESTIGATION_NOTE') THEN
    RAISE EXCEPTION 'WAVE7_RECONCILIATION: security investigation notes';
  END IF;
  IF EXISTS (SELECT 1 FROM public.moderation_actions a
              LEFT JOIN public.content_reports r ON r.report_id=a.report_id
              WHERE a.report_id IS NOT NULL AND r.report_id IS NULL) OR
     EXISTS (SELECT 1 FROM public.content_sources s LEFT JOIN public.content_items i
              ON i.content_item_id=s.content_item_id WHERE i.content_item_id IS NULL) OR
     EXISTS (SELECT 1 FROM public.security_event_notes n LEFT JOIN public.security_events e
              ON e.id=n.event_id WHERE e.id IS NULL) THEN
    RAISE EXCEPTION 'WAVE7_ORPHAN_SOURCE';
  END IF;
END $wave7_reconcile$;

DO $wave7_retarget_fks$
DECLARE m record; c record; new_def text;
BEGIN
  FOR m IN SELECT * FROM (VALUES
    ('content_reports','moderation_cases','report_id','moderation_case_id'),
    ('moderation_actions','moderation_events','moderation_action_id','moderation_event_id'),
    ('security_event_notes','audit_events','note_id','audit_event_id')
  ) x(source_table,target_table,source_key,target_key)
  LOOP
    FOR c IN SELECT conrelid::regclass AS rel,conname,pg_get_constraintdef(oid) AS def
      FROM pg_constraint WHERE contype='f' AND confrelid=to_regclass('public.'||m.source_table)
    LOOP
      new_def:=replace(c.def,m.source_table,m.target_table);
      new_def:=replace(new_def,m.target_table||'('||m.source_key||')',m.target_table||'('||m.target_key||')');
      IF new_def=c.def THEN
        RAISE EXCEPTION 'WAVE7_DEPENDENCY: cannot retarget %.%',c.rel,c.conname;
      END IF;
      EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',c.rel,c.conname);
      EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s',c.rel,c.conname,new_def);
    END LOOP;
  END LOOP;
END $wave7_retarget_fks$;

CREATE INDEX IF NOT EXISTS moderation_cases_report_source_ix
  ON public.moderation_cases(report_source,status,opened_at DESC);
CREATE INDEX IF NOT EXISTS moderation_events_target_history_ix
  ON public.moderation_events(target_type,target_id,action_at DESC);
CREATE INDEX IF NOT EXISTS audit_events_security_note_ix
  ON public.audit_events(security_event_id,occurred_at)
  WHERE event_category='SECURITY_INVESTIGATION_NOTE';
ALTER TABLE public.audit_events
  ADD CONSTRAINT audit_events_security_note_text_ck
  CHECK (event_category <> 'SECURITY_INVESTIGATION_NOTE' OR
         (security_event_id IS NOT NULL AND length(trim(note_text)) > 0));
ALTER TABLE public.audit_events
  ADD CONSTRAINT audit_events_security_event_fk
  FOREIGN KEY (security_event_id) REFERENCES public.security_events(id);

DROP TABLE IF EXISTS public.moderation_actions;
DROP TABLE IF EXISTS public.content_reports;
DROP TABLE IF EXISTS public.content_sources;
DROP TABLE IF EXISTS public.security_event_notes;

DO $wave7_absence_gate$
DECLARE name text;
BEGIN
  FOREACH name IN ARRAY ARRAY['content_reports','moderation_actions','content_sources',
    'security_event_notes']
  LOOP
    IF to_regclass('public.'||name) IS NOT NULL THEN
      RAISE EXCEPTION 'WAVE7_DROP_FAILED: %',name;
    END IF;
  END LOOP;
END $wave7_absence_gate$;
