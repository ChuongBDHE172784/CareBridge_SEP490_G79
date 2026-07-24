-- Phase 2 wave 6: family groups and care-plan lifecycle separation.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.care_groups ADD COLUMN IF NOT EXISTS care_subject_id uuid;
ALTER TABLE public.care_group_members ADD COLUMN IF NOT EXISTS data_permission_id uuid;

CREATE TABLE IF NOT EXISTS public.scheduled_care_items (
    care_item_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id uuid NOT NULL REFERENCES public.users(user_id),
    care_subject_id uuid REFERENCES public.care_subjects(care_subject_id),
    item_type varchar(40) NOT NULL,
    title varchar(255) NOT NULL,
    scheduled_at timestamptz NOT NULL,
    recurrence_rule varchar(255),
    snoozed_until timestamptz,
    completed_at timestamptz,
    skipped_at timestamptz,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    source_reference_type varchar(60),
    source_reference_id uuid,
    vaccination_record_id uuid REFERENCES public.vaccination_records(vaccination_record_id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT scheduled_care_items_vaccination_ck CHECK (item_type <> 'VACCINATION' OR vaccination_record_id IS NOT NULL OR care_subject_id IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS scheduled_care_items_owner_status_ix ON public.scheduled_care_items(owner_user_id, status, scheduled_at);

CREATE TABLE IF NOT EXISTS public.family_tasks (
    task_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    care_group_id uuid NOT NULL REFERENCES public.care_groups(care_group_id),
    creator_user_id uuid REFERENCES public.users(user_id),
    assignee_user_id uuid REFERENCES public.users(user_id),
    care_subject_id uuid REFERENCES public.care_subjects(care_subject_id),
    title varchar(255) NOT NULL,
    description text,
    due_at timestamptz,
    status varchar(30) NOT NULL DEFAULT 'OPEN',
    completed_at timestamptz,
    cancelled_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS family_tasks_assignee_status_ix ON public.family_tasks(assignee_user_id, status, due_at);

CREATE TABLE IF NOT EXISTS public.care_item_templates (
    template_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_template_id uuid REFERENCES public.care_item_templates(template_id),
    entry_type varchar(30) NOT NULL,
    title varchar(255) NOT NULL,
    description text,
    display_order integer NOT NULL DEFAULT 0,
    stage varchar(30),
    is_active boolean NOT NULL DEFAULT true,
    version integer NOT NULL DEFAULT 1,
    effective_from timestamptz,
    effective_to timestamptz,
    configuration_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    configuration_hash varchar(128),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT care_item_templates_type_ck CHECK (entry_type IN ('TEMPLATE_ROOT','CHECKLIST_ENTRY','EXERCISE_TEMPLATE','POSTURE_CONFIG'))
);
CREATE INDEX IF NOT EXISTS care_item_templates_parent_order_ix ON public.care_item_templates(parent_template_id, display_order);

CREATE TABLE IF NOT EXISTS public.preparation_checklist_items (
    checklist_item_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id uuid NOT NULL REFERENCES public.users(user_id),
    mother_journey_id uuid REFERENCES public.mother_journeys(journey_id),
    template_entry_id uuid REFERENCES public.care_item_templates(template_id),
    title varchar(500) NOT NULL,
    display_order integer NOT NULL DEFAULT 0,
    status varchar(30) NOT NULL DEFAULT 'OPEN',
    due_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS preparation_checklist_owner_journey_ix ON public.preparation_checklist_items(owner_user_id, mother_journey_id, display_order);

DO $care_plan_mapping$
BEGIN
    IF to_regclass('public.reminders') IS NOT NULL THEN
        INSERT INTO public.scheduled_care_items
            (care_item_id, owner_user_id, item_type, title, scheduled_at, recurrence_rule,
             snoozed_until, status, created_at, updated_at)
        SELECT r.reminder_id, r.owner_user_id, r.reminder_type, r.title, r.scheduled_at,
               r.recurrence_rule, r.snoozed_until, r.status, r.created_at, r.updated_at
          FROM public.reminders r
        ON CONFLICT (care_item_id) DO NOTHING;
    END IF;
    IF to_regclass('public.care_tasks') IS NOT NULL THEN
        INSERT INTO public.family_tasks
            (task_id, care_group_id, creator_user_id, assignee_user_id, title, description,
             due_at, status, completed_at, created_at, updated_at)
        SELECT t.care_task_id, t.care_group_id, t.assigned_by, t.assigned_to, t.title,
               t.description, t.due_at, t.status, t.completed_at, t.created_at, t.updated_at
          FROM public.care_tasks t
        ON CONFLICT (task_id) DO NOTHING;
    END IF;
    IF to_regclass('public.checklist_templates') IS NOT NULL THEN
        INSERT INTO public.care_item_templates
            (template_id, entry_type, title, description, stage, is_active, version, created_at, updated_at)
        SELECT t.checklist_template_id, 'TEMPLATE_ROOT', coalesce(t.name, 'Checklist template'),
               t.description, t.stage, t.status <> 'INACTIVE', coalesce(t.version_no, 1),
               t.created_at, coalesce(t.updated_at, t.created_at)
          FROM public.checklist_templates t
        ON CONFLICT (template_id) DO NOTHING;
    END IF;
    IF to_regclass('public.checklist_items') IS NOT NULL THEN
        INSERT INTO public.care_item_templates
            (template_id, parent_template_id, entry_type, title, description, display_order, is_active, created_at, updated_at)
        SELECT i.checklist_item_id, i.checklist_template_id, 'CHECKLIST_ENTRY',
               coalesce(i.item_text, 'Checklist item'), i.note, coalesce(i.item_order, 0), true,
               i.created_at, coalesce(i.updated_at, i.created_at)
          FROM public.checklist_items i
        ON CONFLICT (template_id) DO NOTHING;
    END IF;
    IF to_regclass('public.user_checklist_items') IS NOT NULL THEN
        INSERT INTO public.preparation_checklist_items
            (checklist_item_id, owner_user_id, mother_journey_id, template_entry_id, title,
             display_order, status, completed_at, created_at, updated_at)
        SELECT u.user_checklist_item_id, u.owner_user_id, u.journey_id,
               u.template_item_id, u.item_text, u.item_order,
               CASE WHEN u.is_completed THEN 'COMPLETED' ELSE 'OPEN' END,
               u.completed_at, u.created_at, u.updated_at
          FROM public.user_checklist_items u
        ON CONFLICT (checklist_item_id) DO NOTHING;
    END IF;
END
$care_plan_mapping$;
