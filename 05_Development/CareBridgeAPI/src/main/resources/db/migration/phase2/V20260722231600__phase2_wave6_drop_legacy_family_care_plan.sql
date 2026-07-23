-- Phase 2 wave 6 cutover: reminders, family tasks, and preparation checklists.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.scheduled_care_items
  ADD COLUMN IF NOT EXISTS journey_id uuid,
  ADD COLUMN IF NOT EXISTS baby_id uuid,
  ADD COLUMN IF NOT EXISTS recurrence_type varchar(30),
  ADD COLUMN IF NOT EXISTS recurrence_end_date timestamptz,
  ADD COLUMN IF NOT EXISTS fcm_job_id varchar(255);

ALTER TABLE public.preparation_checklist_items
  ADD COLUMN IF NOT EXISTS baby_id uuid,
  ADD COLUMN IF NOT EXISTS category varchar(50) NOT NULL DEFAULT 'GENERAL';

ALTER TABLE public.care_item_templates
  ADD COLUMN IF NOT EXISTS content_status varchar(20) NOT NULL DEFAULT 'DRAFT',
  ADD COLUMN IF NOT EXISTS is_required boolean;

INSERT INTO public.scheduled_care_items (
    care_item_id,owner_user_id,care_subject_id,journey_id,baby_id,item_type,title,
    scheduled_at,recurrence_rule,recurrence_type,recurrence_end_date,fcm_job_id,
    snoozed_until,status,created_at,updated_at)
SELECT r.reminder_id,r.owner_user_id,cs.care_subject_id,r.journey_id,r.baby_id,
       r.reminder_type,r.title,r.scheduled_at,r.recurrence_rule,r.recurrence_type,
       r.recurrence_end_date,r.fcm_job_id,r.snoozed_until,r.status,r.created_at,r.updated_at
  FROM public.reminders r
  LEFT JOIN public.care_subjects cs
    ON cs.care_subject_id=coalesce(r.baby_id,r.journey_id)
ON CONFLICT (care_item_id) DO UPDATE SET
  owner_user_id=excluded.owner_user_id,care_subject_id=excluded.care_subject_id,
  journey_id=excluded.journey_id,baby_id=excluded.baby_id,item_type=excluded.item_type,
  title=excluded.title,scheduled_at=excluded.scheduled_at,
  recurrence_rule=excluded.recurrence_rule,recurrence_type=excluded.recurrence_type,
  recurrence_end_date=excluded.recurrence_end_date,fcm_job_id=excluded.fcm_job_id,
  snoozed_until=excluded.snoozed_until,status=excluded.status,updated_at=excluded.updated_at;

INSERT INTO public.family_tasks (
    task_id,care_group_id,creator_user_id,assignee_user_id,care_subject_id,title,
    description,due_at,status,completed_at,created_at,updated_at)
SELECT t.care_task_id,t.care_group_id,t.assigned_by,t.assigned_to,cg.care_subject_id,
       t.title,t.description,t.due_at,t.status,t.completed_at,t.created_at,t.updated_at
  FROM public.care_tasks t
  JOIN public.care_groups cg ON cg.care_group_id=t.care_group_id
ON CONFLICT (task_id) DO UPDATE SET
  care_group_id=excluded.care_group_id,creator_user_id=excluded.creator_user_id,
  assignee_user_id=excluded.assignee_user_id,care_subject_id=excluded.care_subject_id,
  title=excluded.title,description=excluded.description,due_at=excluded.due_at,
  status=excluded.status,completed_at=excluded.completed_at,updated_at=excluded.updated_at;

INSERT INTO public.care_item_templates (
    template_id,entry_type,title,description,stage,is_active,content_status,version,
    created_at,updated_at)
SELECT checklist_template_id,'TEMPLATE_ROOT',coalesce(name,'Checklist template'),
       description,stage,status NOT IN ('INACTIVE','ARCHIVED'),status,
       coalesce(version_no,1),created_at,coalesce(updated_at,created_at)
  FROM public.checklist_templates
ON CONFLICT (template_id) DO UPDATE SET
  entry_type='TEMPLATE_ROOT',title=excluded.title,description=excluded.description,
  stage=excluded.stage,is_active=excluded.is_active,content_status=excluded.content_status,
  version=excluded.version,updated_at=excluded.updated_at;

INSERT INTO public.care_item_templates (
    template_id,parent_template_id,entry_type,title,description,display_order,
    is_required,is_active,content_status,created_at,updated_at)
SELECT checklist_item_id,checklist_template_id,'CHECKLIST_ENTRY',
       coalesce(item_text,'Checklist item'),note,coalesce(item_order,0),
       is_required,true,'PUBLISHED',created_at,coalesce(updated_at,created_at)
  FROM public.checklist_items
ON CONFLICT (template_id) DO UPDATE SET
  parent_template_id=excluded.parent_template_id,entry_type='CHECKLIST_ENTRY',
  title=excluded.title,description=excluded.description,display_order=excluded.display_order,
  is_required=excluded.is_required,is_active=excluded.is_active,
  content_status=excluded.content_status,updated_at=excluded.updated_at;

INSERT INTO public.preparation_checklist_items (
    checklist_item_id,owner_user_id,mother_journey_id,baby_id,template_entry_id,
    title,category,display_order,status,completed_at,created_at,updated_at)
SELECT user_checklist_item_id,owner_user_id,journey_id,baby_id,template_item_id,
       item_text,category,item_order,CASE WHEN is_completed THEN 'COMPLETED' ELSE 'OPEN' END,
       completed_at,created_at,updated_at
  FROM public.user_checklist_items
ON CONFLICT (checklist_item_id) DO UPDATE SET
  owner_user_id=excluded.owner_user_id,mother_journey_id=excluded.mother_journey_id,
  baby_id=excluded.baby_id,template_entry_id=excluded.template_entry_id,
  title=excluded.title,category=excluded.category,display_order=excluded.display_order,
  status=excluded.status,completed_at=excluded.completed_at,updated_at=excluded.updated_at;

DO $wave6_reconcile$
BEGIN
  IF (SELECT count(*) FROM public.reminders) <>
     (SELECT count(*) FROM public.scheduled_care_items) THEN
    RAISE EXCEPTION 'WAVE6_RECONCILIATION: scheduled care items';
  END IF;
  IF (SELECT count(*) FROM public.care_tasks) <>
     (SELECT count(*) FROM public.family_tasks) THEN
    RAISE EXCEPTION 'WAVE6_RECONCILIATION: family tasks';
  END IF;
  IF (SELECT count(*) FROM public.checklist_templates) <>
     (SELECT count(*) FROM public.care_item_templates WHERE entry_type='TEMPLATE_ROOT') THEN
    RAISE EXCEPTION 'WAVE6_RECONCILIATION: checklist templates';
  END IF;
  IF (SELECT count(*) FROM public.checklist_items) <>
     (SELECT count(*) FROM public.care_item_templates WHERE entry_type='CHECKLIST_ENTRY') THEN
    RAISE EXCEPTION 'WAVE6_RECONCILIATION: checklist entries';
  END IF;
  IF (SELECT count(*) FROM public.user_checklist_items) <>
     (SELECT count(*) FROM public.preparation_checklist_items) THEN
    RAISE EXCEPTION 'WAVE6_RECONCILIATION: preparation checklist';
  END IF;
  IF EXISTS (SELECT 1 FROM public.care_tasks t LEFT JOIN public.care_groups g
              ON g.care_group_id=t.care_group_id WHERE g.care_group_id IS NULL) OR
     EXISTS (SELECT 1 FROM public.checklist_items i LEFT JOIN public.checklist_templates t
              ON t.checklist_template_id=i.checklist_template_id
              WHERE i.checklist_template_id IS NOT NULL AND t.checklist_template_id IS NULL) OR
     EXISTS (SELECT 1 FROM public.user_checklist_items u LEFT JOIN public.checklist_items i
              ON i.checklist_item_id=u.template_item_id
              WHERE u.template_item_id IS NOT NULL AND i.checklist_item_id IS NULL) THEN
    RAISE EXCEPTION 'WAVE6_ORPHAN_SOURCE';
  END IF;
END $wave6_reconcile$;

DO $wave6_retarget_fks$
DECLARE m record; c record; new_def text;
BEGIN
  FOR m IN SELECT * FROM (VALUES
    ('reminders','scheduled_care_items','reminder_id','care_item_id'),
    ('care_tasks','family_tasks','care_task_id','task_id'),
    ('checklist_templates','care_item_templates','checklist_template_id','template_id'),
    ('checklist_items','care_item_templates','checklist_item_id','template_id'),
    ('user_checklist_items','preparation_checklist_items','user_checklist_item_id','checklist_item_id')
  ) x(source_table,target_table,source_key,target_key)
  LOOP
    FOR c IN SELECT conrelid::regclass AS rel,conname,pg_get_constraintdef(oid) AS def
      FROM pg_constraint WHERE contype='f' AND confrelid=to_regclass('public.'||m.source_table)
    LOOP
      new_def:=replace(c.def,m.source_table,m.target_table);
      new_def:=replace(new_def,m.target_table||'('||m.source_key||')',m.target_table||'('||m.target_key||')');
      IF new_def=c.def THEN
        RAISE EXCEPTION 'WAVE6_DEPENDENCY: cannot retarget %.%',c.rel,c.conname;
      END IF;
      EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',c.rel,c.conname);
      EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s',c.rel,c.conname,new_def);
    END LOOP;
  END LOOP;
END $wave6_retarget_fks$;

CREATE INDEX IF NOT EXISTS scheduled_care_items_context_ix
  ON public.scheduled_care_items(owner_user_id,journey_id,baby_id,status,scheduled_at);
CREATE INDEX IF NOT EXISTS preparation_checklist_filter_ix
  ON public.preparation_checklist_items(owner_user_id,mother_journey_id,baby_id,status,display_order);
CREATE INDEX IF NOT EXISTS care_item_templates_content_status_ix
  ON public.care_item_templates(entry_type,content_status,stage,display_order);

DROP TABLE IF EXISTS public.user_checklist_items;
DROP TABLE IF EXISTS public.checklist_items;
DROP TABLE IF EXISTS public.checklist_templates;
DROP TABLE IF EXISTS public.care_tasks;
DROP TABLE IF EXISTS public.reminders;

DO $wave6_absence_gate$
DECLARE name text;
BEGIN
  FOREACH name IN ARRAY ARRAY['reminders','care_tasks','user_checklist_items',
    'checklist_templates','checklist_items']
  LOOP
    IF to_regclass('public.'||name) IS NOT NULL THEN
      RAISE EXCEPTION 'WAVE6_DROP_FAILED: %',name;
    END IF;
  END LOOP;
END $wave6_absence_gate$;
