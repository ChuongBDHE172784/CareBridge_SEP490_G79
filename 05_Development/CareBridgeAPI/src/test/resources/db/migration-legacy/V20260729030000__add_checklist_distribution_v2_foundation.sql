-- Checklist Distribution V2 foundation. Additive only; legacy tables remain authoritative until cutover.

CREATE TABLE public.checklist_substages (
    substage_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code varchar(80) NOT NULL,
    stage varchar(30) NOT NULL,
    anchor_type varchar(30) NOT NULL,
    range_unit varchar(10) NOT NULL,
    start_inclusive integer NOT NULL,
    end_inclusive integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_substages_pk PRIMARY KEY (substage_id),
    CONSTRAINT checklist_substages_code_uk UNIQUE (code),
    CONSTRAINT checklist_substages_stage_ck CHECK (stage IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE')),
    CONSTRAINT checklist_substages_anchor_ck CHECK (anchor_type IN ('NONE','LMP','EDD','DELIVERY_DATE','BIRTH_DATE')),
    CONSTRAINT checklist_substages_unit_ck CHECK (range_unit IN ('DAY','WEEK','MONTH')),
    CONSTRAINT checklist_substages_range_ck CHECK (start_inclusive >= 0 AND end_inclusive >= start_inclusive)
);

INSERT INTO public.checklist_substages
    (code, stage, anchor_type, range_unit, start_inclusive, end_inclusive)
VALUES
    ('LEGACY_PRE_PREGNANCY', 'PRE_PREGNANCY', 'NONE', 'DAY', 0, 2147483647),
    ('LEGACY_PREGNANCY', 'PREGNANCY', 'NONE', 'DAY', 0, 2147483647),
    ('LEGACY_POSTPARTUM', 'POSTPARTUM', 'NONE', 'DAY', 0, 2147483647),
    ('LEGACY_BABY_CARE', 'BABY_CARE', 'NONE', 'DAY', 0, 2147483647),
    ('PREGNANCY_LMP_WEEK_0_12', 'PREGNANCY', 'LMP', 'WEEK', 0, 12),
    ('PREGNANCY_EDD_WEEK_0_40', 'PREGNANCY', 'EDD', 'WEEK', 0, 40),
    ('POSTPARTUM_DAY_0_7', 'POSTPARTUM', 'DELIVERY_DATE', 'DAY', 0, 7),
    ('POSTPARTUM_WEEK_0_6', 'POSTPARTUM', 'DELIVERY_DATE', 'WEEK', 0, 6),
    ('BABY_CARE_DAY_0_28', 'BABY_CARE', 'BIRTH_DATE', 'DAY', 0, 28),
    ('BABY_CARE_MONTH_0_3', 'BABY_CARE', 'BIRTH_DATE', 'MONTH', 0, 3)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE public.checklist_migration_quarantine (
    quarantine_id uuid DEFAULT gen_random_uuid() NOT NULL,
    source_table varchar(80) NOT NULL,
    source_id uuid,
    reason_code varchar(80) NOT NULL,
    payload_ciphertext bytea NOT NULL,
    payload_hash char(64) NOT NULL,
    encryption_key_version varchar(40) NOT NULL,
    correlation_id uuid NOT NULL,
    resolved_at timestamptz,
    resolved_by uuid,
    resolution_code varchar(80),
    legal_hold boolean DEFAULT false NOT NULL,
    retain_until timestamptz NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_migration_quarantine_pk PRIMARY KEY (quarantine_id),
    CONSTRAINT checklist_migration_quarantine_hash_ck CHECK (payload_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX checklist_migration_quarantine_correlation_ix
    ON public.checklist_migration_quarantine(correlation_id, created_at DESC);

ALTER TABLE public.care_item_templates
    ADD COLUMN template_lineage_id uuid,
    ADD COLUMN template_version_id uuid,
    ADD COLUMN substage_id uuid,
    ADD COLUMN target_subject varchar(10),
    ADD COLUMN migration_review_required boolean DEFAULT false NOT NULL,
    ADD COLUMN distribution_enabled boolean DEFAULT false NOT NULL,
    ADD COLUMN approved_at timestamptz,
    ADD COLUMN approved_by uuid;

ALTER TABLE public.care_item_templates
    DROP CONSTRAINT care_item_templates_type_ck,
    ADD CONSTRAINT care_item_templates_type_ck CHECK (entry_type IN (
        'TEMPLATE_ROOT','CHECKLIST_ENTRY','QUARANTINED_CHECKLIST_ENTRY',
        'EXERCISE_TEMPLATE','POSTURE_CONFIG'));

UPDATE public.care_item_templates
SET template_lineage_id = template_id,
    template_version_id = template_id,
    migration_review_required = true,
    distribution_enabled = false
WHERE entry_type = 'TEMPLATE_ROOT';

UPDATE public.care_item_templates root
SET substage_id = substage.substage_id
FROM public.checklist_substages substage
WHERE root.entry_type = 'TEMPLATE_ROOT'
  AND substage.code = 'LEGACY_' || root.stage
  AND root.stage IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE');

WITH invalid_entries AS (
    SELECT
        item.template_id,
        CASE
            WHEN root.template_id IS NULL THEN 'LEGACY_PARENT_MISSING'
            WHEN root.entry_type <> 'TEMPLATE_ROOT' THEN 'LEGACY_PARENT_NOT_ROOT'
            ELSE 'LEGACY_PARENT_STAGE_INVALID'
        END AS reason_code
    FROM public.care_item_templates item
    LEFT JOIN public.care_item_templates root ON root.template_id = item.parent_template_id
    WHERE item.entry_type = 'CHECKLIST_ENTRY'
      AND (root.template_id IS NULL
           OR root.entry_type <> 'TEMPLATE_ROOT'
           OR root.stage IS NULL
           OR root.stage NOT IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE'))
), prepared_quarantine AS (
    SELECT
        template_id,
        reason_code,
        decode(
            md5(template_id::text || clock_timestamp()::text || random()::text) ||
            md5(gen_random_uuid()::text || random()::text),
            'hex') AS redacted_payload
    FROM invalid_entries
)
INSERT INTO public.checklist_migration_quarantine
    (source_table, source_id, reason_code, payload_ciphertext, payload_hash,
     encryption_key_version, correlation_id, retain_until)
SELECT
    'care_item_templates',
    template_id,
    reason_code,
    redacted_payload,
    md5(encode(redacted_payload, 'hex')) || md5('v2:' || encode(redacted_payload, 'hex')),
    'REDACTED_NO_PAYLOAD_V1',
    gen_random_uuid(),
    now() + interval '7 years'
FROM prepared_quarantine;

UPDATE public.care_item_templates item
SET entry_type = 'QUARANTINED_CHECKLIST_ENTRY',
    is_active = false,
    migration_review_required = true
FROM public.care_item_templates root
WHERE item.entry_type = 'CHECKLIST_ENTRY'
  AND item.parent_template_id = root.template_id
  AND (root.entry_type <> 'TEMPLATE_ROOT'
       OR root.stage IS NULL
       OR root.stage NOT IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE'));

UPDATE public.care_item_templates item
SET entry_type = 'QUARANTINED_CHECKLIST_ENTRY',
    is_active = false,
    migration_review_required = true
WHERE item.entry_type = 'CHECKLIST_ENTRY'
  AND NOT EXISTS (
      SELECT 1 FROM public.care_item_templates root
      WHERE root.template_id = item.parent_template_id
  );

UPDATE public.care_item_templates item
SET target_subject = CASE WHEN root.stage = 'BABY_CARE' THEN 'BABY' ELSE 'MOTHER' END
FROM public.care_item_templates root
WHERE item.entry_type = 'CHECKLIST_ENTRY'
  AND item.parent_template_id = root.template_id
  AND root.entry_type = 'TEMPLATE_ROOT'
  AND root.stage IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE');

ALTER TABLE public.checklist_migration_quarantine ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.checklist_migration_quarantine FORCE ROW LEVEL SECURITY;
CREATE POLICY checklist_migration_quarantine_insert_policy
    ON public.checklist_migration_quarantine FOR INSERT WITH CHECK (true);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'checklist_operations') THEN
        EXECUTE 'GRANT SELECT, UPDATE ON public.checklist_migration_quarantine TO checklist_operations';
        EXECUTE 'CREATE POLICY checklist_migration_quarantine_operations_select
                 ON public.checklist_migration_quarantine FOR SELECT
                 TO checklist_operations USING (true)';
        EXECUTE 'CREATE POLICY checklist_migration_quarantine_operations_update
                 ON public.checklist_migration_quarantine FOR UPDATE
                 TO checklist_operations
                 USING (resolved_at IS NULL)
                 WITH CHECK (resolved_at IS NOT NULL AND resolved_by IS NOT NULL AND resolution_code IS NOT NULL)';
    END IF;
END $$;

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_version_id_uk UNIQUE (template_version_id),
    ADD CONSTRAINT care_item_templates_lineage_version_uk UNIQUE (template_lineage_id, template_version_id),
    ADD CONSTRAINT care_item_templates_version_root_uk UNIQUE (template_version_id, template_id),
    ADD CONSTRAINT care_item_templates_parent_item_uk UNIQUE (parent_template_id, template_id, entry_type),
    ADD CONSTRAINT care_item_templates_substage_fk FOREIGN KEY (substage_id)
        REFERENCES public.checklist_substages(substage_id) ON DELETE RESTRICT,
    ADD CONSTRAINT care_item_templates_target_ck CHECK
        ((entry_type <> 'CHECKLIST_ENTRY') OR
         (target_subject IS NOT NULL AND target_subject IN ('MOTHER','BABY'))),
    ADD CONSTRAINT care_item_templates_root_version_ck CHECK
        ((entry_type = 'TEMPLATE_ROOT' AND template_lineage_id IS NOT NULL AND template_version_id IS NOT NULL) OR
         (entry_type <> 'TEMPLATE_ROOT' AND template_lineage_id IS NULL AND template_version_id IS NULL)),
    ADD CONSTRAINT care_item_templates_distribution_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR distribution_enabled = false OR
         (migration_review_required = false AND approved_at IS NOT NULL AND approved_by IS NOT NULL));

CREATE INDEX care_item_templates_lineage_version_ix
    ON public.care_item_templates(template_lineage_id, version)
    WHERE entry_type = 'TEMPLATE_ROOT';

CREATE TABLE public.checklist_template_recipient_roles (
    template_version_id uuid NOT NULL,
    recipient_role varchar(10) NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_template_recipient_roles_pk PRIMARY KEY (template_version_id, recipient_role),
    CONSTRAINT checklist_template_recipient_roles_role_ck CHECK (recipient_role IN ('MOTHER','FAMILY')),
    CONSTRAINT checklist_template_recipient_roles_version_fk FOREIGN KEY (template_version_id)
        REFERENCES public.care_item_templates(template_version_id) ON DELETE CASCADE
);

INSERT INTO public.checklist_template_recipient_roles(template_version_id, recipient_role)
SELECT template_version_id, 'MOTHER' FROM public.care_item_templates
WHERE entry_type = 'TEMPLATE_ROOT' ON CONFLICT DO NOTHING;

CREATE TABLE public.checklist_template_version_items (
    template_version_id uuid NOT NULL,
    template_root_id uuid NOT NULL,
    template_item_version_id uuid NOT NULL,
    item_entry_type varchar(30) DEFAULT 'CHECKLIST_ENTRY' NOT NULL,
    CONSTRAINT checklist_template_version_items_pk
        PRIMARY KEY (template_version_id, template_item_version_id),
    CONSTRAINT checklist_template_version_items_type_ck CHECK (item_entry_type = 'CHECKLIST_ENTRY'),
    CONSTRAINT checklist_template_version_items_root_fk
        FOREIGN KEY (template_version_id, template_root_id)
        REFERENCES public.care_item_templates(template_version_id, template_id) ON DELETE RESTRICT,
    CONSTRAINT checklist_template_version_items_item_fk
        FOREIGN KEY (template_root_id, template_item_version_id, item_entry_type)
        REFERENCES public.care_item_templates(parent_template_id, template_id, entry_type) ON DELETE RESTRICT
);

INSERT INTO public.checklist_template_version_items
    (template_version_id, template_root_id, template_item_version_id)
SELECT root.template_version_id, root.template_id, item.template_id
FROM public.care_item_templates item
JOIN public.care_item_templates root ON root.template_id = item.parent_template_id
WHERE root.entry_type = 'TEMPLATE_ROOT'
  AND item.entry_type = 'CHECKLIST_ENTRY'
ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION public.checklist_sync_template_version_item()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM public.checklist_template_version_items
        WHERE template_item_version_id = OLD.template_id OR template_root_id = OLD.template_id;
        RETURN OLD;
    END IF;

    DELETE FROM public.checklist_template_version_items
    WHERE template_item_version_id = NEW.template_id OR template_root_id = NEW.template_id;

    IF NEW.entry_type = 'CHECKLIST_ENTRY' AND NEW.parent_template_id IS NOT NULL THEN
        INSERT INTO public.checklist_template_version_items
            (template_version_id, template_root_id, template_item_version_id)
        SELECT root.template_version_id, root.template_id, NEW.template_id
        FROM public.care_item_templates root
        WHERE root.template_id = NEW.parent_template_id
          AND root.entry_type = 'TEMPLATE_ROOT'
          AND root.template_version_id IS NOT NULL
        ON CONFLICT DO NOTHING;
    ELSIF NEW.entry_type = 'TEMPLATE_ROOT' AND NEW.template_version_id IS NOT NULL THEN
        INSERT INTO public.checklist_template_version_items
            (template_version_id, template_root_id, template_item_version_id)
        SELECT NEW.template_version_id, NEW.template_id, item.template_id
        FROM public.care_item_templates item
        WHERE item.parent_template_id = NEW.template_id
          AND item.entry_type = 'CHECKLIST_ENTRY'
        ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_sync_template_version_item_write_trg
AFTER INSERT OR UPDATE OF entry_type, parent_template_id, template_version_id
ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_template_version_item();

CREATE TRIGGER checklist_sync_template_version_item_delete_trg
BEFORE DELETE ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_template_version_item();

ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_id_owner_uk UNIQUE (care_group_id, owner_user_id);

ALTER TABLE public.mother_journeys
    ADD CONSTRAINT mother_journeys_id_owner_checklist_uk UNIQUE (journey_id, owner_user_id);
ALTER TABLE public.care_subjects
    ADD CONSTRAINT care_subjects_id_owner_type_checklist_uk
        UNIQUE (care_subject_id, owner_user_id, subject_type);

CREATE TABLE public.checklist_context_authorities (
    care_context_type varchar(10) NOT NULL,
    care_context_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    journey_id uuid,
    baby_id uuid,
    baby_subject_type varchar(30),
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_context_authorities_pk
        PRIMARY KEY (care_context_type, care_context_id, owner_user_id),
    CONSTRAINT checklist_context_authorities_shape_ck CHECK (
        (care_context_type = 'JOURNEY' AND journey_id = care_context_id
            AND baby_id IS NULL AND baby_subject_type IS NULL) OR
        (care_context_type = 'BABY' AND baby_id = care_context_id
            AND journey_id IS NULL AND baby_subject_type = 'BABY')
    ),
    CONSTRAINT checklist_context_authorities_journey_fk FOREIGN KEY (journey_id, owner_user_id)
        REFERENCES public.mother_journeys(journey_id, owner_user_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT checklist_context_authorities_baby_fk
        FOREIGN KEY (baby_id, owner_user_id, baby_subject_type)
        REFERENCES public.care_subjects(care_subject_id, owner_user_id, subject_type)
        ON UPDATE CASCADE ON DELETE CASCADE
);

INSERT INTO public.checklist_context_authorities
    (care_context_type, care_context_id, owner_user_id, journey_id)
SELECT 'JOURNEY', journey_id, owner_user_id, journey_id
FROM public.mother_journeys
ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION public.checklist_sync_context_authority()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME = 'mother_journeys' THEN
        INSERT INTO public.checklist_context_authorities
            (care_context_type, care_context_id, owner_user_id, journey_id)
        VALUES ('JOURNEY', NEW.journey_id, NEW.owner_user_id, NEW.journey_id)
        ON CONFLICT (care_context_type, care_context_id, owner_user_id)
        DO UPDATE SET journey_id = EXCLUDED.journey_id;
    ELSIF NEW.subject_type = 'BABY' THEN
        INSERT INTO public.checklist_context_authorities
            (care_context_type, care_context_id, owner_user_id, baby_id, baby_subject_type)
        VALUES ('BABY', NEW.care_subject_id, NEW.owner_user_id, NEW.care_subject_id, 'BABY')
        ON CONFLICT (care_context_type, care_context_id, owner_user_id)
        DO UPDATE SET baby_id = EXCLUDED.baby_id, baby_subject_type = 'BABY';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_sync_journey_context_authority_trg
AFTER INSERT OR UPDATE OF owner_user_id ON public.mother_journeys
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_context_authority();

CREATE TRIGGER checklist_sync_baby_context_authority_trg
AFTER INSERT OR UPDATE OF owner_user_id, subject_type ON public.care_subjects
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_context_authority();

INSERT INTO public.checklist_context_authorities
    (care_context_type, care_context_id, owner_user_id, baby_id, baby_subject_type)
SELECT 'BABY', care_subject_id, owner_user_id, care_subject_id, 'BABY'
FROM public.care_subjects
WHERE subject_type = 'BABY'
ON CONFLICT DO NOTHING;

CREATE INDEX care_group_members_checklist_auth_ix
    ON public.care_group_members(care_group_id, user_id, invitation_status)
    WHERE invitation_status = 'ACCEPTED';

CREATE TABLE public.checklist_care_group_contexts (
    context_mapping_id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_group_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    care_context_type varchar(10) NOT NULL,
    care_context_id uuid NOT NULL,
    review_status varchar(20) DEFAULT 'UNREVIEWED' NOT NULL,
    distribution_blocked boolean DEFAULT true NOT NULL,
    block_reason_code varchar(80),
    reviewed_at timestamptz,
    reviewed_by uuid,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_care_group_contexts_pk PRIMARY KEY (context_mapping_id),
    CONSTRAINT checklist_care_group_contexts_group_context_uk UNIQUE (care_group_id, care_context_type, care_context_id),
    CONSTRAINT checklist_care_group_contexts_authority_uk UNIQUE (care_group_id, care_context_type, care_context_id, owner_user_id),
    CONSTRAINT checklist_care_group_contexts_type_ck CHECK (care_context_type IN ('JOURNEY','BABY')),
    CONSTRAINT checklist_care_group_contexts_review_ck CHECK (review_status IN ('UNREVIEWED','REVIEWED','BLOCKED')),
    CONSTRAINT checklist_care_group_contexts_review_shape_ck CHECK (
        (review_status = 'UNREVIEWED' AND distribution_blocked = true
            AND reviewed_at IS NULL AND reviewed_by IS NULL) OR
        (review_status = 'REVIEWED' AND distribution_blocked = false
            AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL AND block_reason_code IS NULL) OR
        (review_status = 'BLOCKED' AND distribution_blocked = true AND block_reason_code IS NOT NULL)
    ),
    CONSTRAINT checklist_care_group_contexts_group_owner_fk FOREIGN KEY (care_group_id, owner_user_id)
        REFERENCES public.care_groups(care_group_id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT checklist_care_group_contexts_canonical_owner_fk FOREIGN KEY
        (care_context_type, care_context_id, owner_user_id)
        REFERENCES public.checklist_context_authorities
        (care_context_type, care_context_id, owner_user_id) ON DELETE RESTRICT
);

CREATE TABLE public.checklist_instances (
    checklist_instance_id uuid DEFAULT gen_random_uuid() NOT NULL,
    distribution_key char(64) NOT NULL,
    key_version varchar(10) DEFAULT 'v1' NOT NULL,
    template_lineage_id uuid,
    template_version_id uuid,
    recipient_user_id uuid NOT NULL,
    recipient_role varchar(10) NOT NULL,
    care_group_id uuid NOT NULL,
    care_context_type varchar(10) NOT NULL,
    care_context_id uuid NOT NULL,
    context_owner_user_id uuid NOT NULL,
    origin varchar(20) NOT NULL,
    window_start date,
    window_end date,
    status varchar(20) DEFAULT 'PENDING' NOT NULL,
    lock_version bigint DEFAULT 0 NOT NULL,
    completed_at timestamptz,
    cancelled_at timestamptz,
    cancellation_reason_code varchar(80),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_instances_pk PRIMARY KEY (checklist_instance_id),
    CONSTRAINT checklist_instances_distribution_key_uk UNIQUE (distribution_key),
    CONSTRAINT checklist_instances_key_format_ck CHECK (distribution_key ~ '^[0-9a-f]{64}$'),
    CONSTRAINT checklist_instances_role_ck CHECK (recipient_role IN ('MOTHER','FAMILY')),
    CONSTRAINT checklist_instances_context_ck CHECK (care_context_type IN ('JOURNEY','BABY')),
    CONSTRAINT checklist_instances_origin_ck CHECK (origin IN ('SYSTEM_TEMPLATE','USER_CREATED')),
    CONSTRAINT checklist_instances_status_ck CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT checklist_instances_terminal_shape_ck CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL AND cancelled_at IS NULL) OR
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND completed_at IS NULL) OR
        (status IN ('PENDING','IN_PROGRESS') AND completed_at IS NULL AND cancelled_at IS NULL)
    ),
    CONSTRAINT checklist_instances_window_ck CHECK
        ((window_start IS NULL AND window_end IS NULL) OR
         (window_start IS NOT NULL AND window_end IS NOT NULL AND window_end >= window_start)),
    CONSTRAINT checklist_instances_template_pair_ck CHECK
        ((origin = 'SYSTEM_TEMPLATE' AND template_lineage_id IS NOT NULL AND template_version_id IS NOT NULL) OR
         (origin = 'USER_CREATED' AND template_lineage_id IS NULL AND template_version_id IS NULL)),
    CONSTRAINT checklist_instances_mother_recipient_ck CHECK
        ((recipient_role = 'MOTHER' AND recipient_user_id = context_owner_user_id) OR recipient_role = 'FAMILY'),
    CONSTRAINT checklist_instances_recipient_fk FOREIGN KEY (recipient_user_id)
        REFERENCES public.users(user_id) ON DELETE RESTRICT,
    CONSTRAINT checklist_instances_lineage_version_fk FOREIGN KEY (template_lineage_id, template_version_id)
        REFERENCES public.care_item_templates(template_lineage_id, template_version_id) ON DELETE RESTRICT,
    CONSTRAINT checklist_instances_template_recipient_role_fk
        FOREIGN KEY (template_version_id, recipient_role)
        REFERENCES public.checklist_template_recipient_roles(template_version_id, recipient_role)
        ON DELETE RESTRICT,
    CONSTRAINT checklist_instances_context_authority_fk FOREIGN KEY
        (care_group_id, care_context_type, care_context_id, context_owner_user_id)
        REFERENCES public.checklist_care_group_contexts
        (care_group_id, care_context_type, care_context_id, owner_user_id) ON DELETE RESTRICT
);

ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instances_id_version_uk UNIQUE (checklist_instance_id, template_version_id);

CREATE OR REPLACE FUNCTION public.checklist_validate_instance_recipient()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.recipient_role = 'FAMILY' THEN
        PERFORM 1
        FROM public.care_group_members member
        WHERE member.care_group_id = NEW.care_group_id
          AND member.user_id = NEW.recipient_user_id
          AND member.invitation_status = 'ACCEPTED'
          AND (member.invite_expires_at IS NULL OR member.invite_expires_at > now())
          AND jsonb_typeof(member.permission_json) = 'object'
          AND COALESCE(
              member.permission_json->>'CHECKLIST_VIEW',
              member.permission_json->>'checklist_view',
              'false') = 'true'
        FOR KEY SHARE;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'CHECKLIST_FAMILY_RECIPIENT_NOT_AUTHORIZED';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_validate_instance_recipient_trg
BEFORE INSERT ON public.checklist_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_instance_recipient();

CREATE INDEX checklist_instances_recipient_status_ix
    ON public.checklist_instances(recipient_user_id, status, created_at DESC);
CREATE INDEX checklist_instances_context_status_ix
    ON public.checklist_instances(care_group_id, care_context_type, care_context_id, status);

CREATE TABLE public.checklist_task_instances (
    checklist_task_instance_id uuid DEFAULT gen_random_uuid() NOT NULL,
    checklist_instance_id uuid NOT NULL,
    template_version_id uuid,
    template_item_version_id uuid,
    task_key char(64) NOT NULL,
    key_version varchar(10) DEFAULT 'v1' NOT NULL,
    title_snapshot varchar(500) NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    is_required boolean DEFAULT false NOT NULL,
    target_subject varchar(10) NOT NULL,
    due_at timestamptz,
    status varchar(20) DEFAULT 'PENDING' NOT NULL,
    lock_version bigint DEFAULT 0 NOT NULL,
    completed_at timestamptz,
    skipped_at timestamptz,
    cancelled_at timestamptz,
    action_reason_code varchar(80),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_task_instances_pk PRIMARY KEY (checklist_task_instance_id),
    CONSTRAINT checklist_task_instances_task_key_uk UNIQUE (task_key),
    CONSTRAINT checklist_task_instances_key_format_ck CHECK (task_key ~ '^[0-9a-f]{64}$'),
    CONSTRAINT checklist_task_instances_target_ck CHECK (target_subject IN ('MOTHER','BABY')),
    CONSTRAINT checklist_task_instances_status_ck CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','SKIPPED','CANCELLED')),
    CONSTRAINT checklist_task_instances_terminal_shape_ck CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL
            AND skipped_at IS NULL AND cancelled_at IS NULL) OR
        (status = 'SKIPPED' AND skipped_at IS NOT NULL
            AND completed_at IS NULL AND cancelled_at IS NULL) OR
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL
            AND completed_at IS NULL AND skipped_at IS NULL) OR
        (status IN ('PENDING','IN_PROGRESS')
            AND completed_at IS NULL AND skipped_at IS NULL AND cancelled_at IS NULL)
    ),
    CONSTRAINT checklist_task_instances_parent_fk FOREIGN KEY (checklist_instance_id)
        REFERENCES public.checklist_instances(checklist_instance_id) ON DELETE CASCADE,
    CONSTRAINT checklist_task_instances_template_pair_ck CHECK
        ((template_version_id IS NULL) = (template_item_version_id IS NULL)),
    CONSTRAINT checklist_task_instances_parent_version_fk
        FOREIGN KEY (checklist_instance_id, template_version_id)
        REFERENCES public.checklist_instances(checklist_instance_id, template_version_id) ON DELETE CASCADE,
    CONSTRAINT checklist_task_instances_version_item_fk
        FOREIGN KEY (template_version_id, template_item_version_id)
        REFERENCES public.checklist_template_version_items(template_version_id, template_item_version_id)
        ON DELETE RESTRICT
);

CREATE OR REPLACE FUNCTION public.checklist_validate_task_template()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    parent_origin varchar(20);
    parent_version_id uuid;
BEGIN
    SELECT origin, template_version_id
    INTO parent_origin, parent_version_id
    FROM public.checklist_instances
    WHERE checklist_instance_id = NEW.checklist_instance_id;

    IF parent_origin = 'SYSTEM_TEMPLATE' AND
       (NEW.template_version_id IS NULL OR NEW.template_item_version_id IS NULL OR
        NEW.template_version_id <> parent_version_id) THEN
        RAISE EXCEPTION 'CHECKLIST_SYSTEM_TASK_TEMPLATE_REQUIRED';
    END IF;
    IF parent_origin = 'USER_CREATED' AND
       (NEW.template_version_id IS NOT NULL OR NEW.template_item_version_id IS NOT NULL) THEN
        RAISE EXCEPTION 'CHECKLIST_USER_TASK_TEMPLATE_FORBIDDEN';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_validate_task_template_trg
BEFORE INSERT OR UPDATE OF checklist_instance_id, template_version_id, template_item_version_id
ON public.checklist_task_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_task_template();

CREATE INDEX checklist_task_instances_parent_order_ix
    ON public.checklist_task_instances(checklist_instance_id, display_order);
CREATE INDEX checklist_task_instances_due_status_ix
    ON public.checklist_task_instances(due_at, status);

CREATE TABLE public.checklist_action_commands (
    checklist_action_command_id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid NOT NULL,
    task_kind varchar(30) NOT NULL,
    task_id uuid NOT NULL,
    client_request_id uuid NOT NULL,
    payload_hash char(64) NOT NULL,
    action_type varchar(30) NOT NULL,
    result_status varchar(20) NOT NULL,
    result_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    applied_at timestamptz DEFAULT now() NOT NULL,
    retain_until timestamptz NOT NULL,
    legal_hold boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_action_commands_pk PRIMARY KEY (checklist_action_command_id),
    CONSTRAINT checklist_action_commands_scope_uk UNIQUE (actor_user_id, task_kind, task_id, client_request_id),
    CONSTRAINT checklist_action_commands_payload_ck CHECK (payload_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT checklist_action_commands_task_kind_ck CHECK (task_kind IN ('CHECKLIST','CARE_TASK','REMINDER')),
    CONSTRAINT checklist_action_commands_action_ck CHECK (action_type IN ('COMPLETE','SKIP')),
    CONSTRAINT checklist_action_commands_result_ck CHECK
        (result_status IN ('APPLIED','IDEMPOTENT_REPLAY','REJECTED')),
    CONSTRAINT checklist_action_commands_actor_fk FOREIGN KEY (actor_user_id)
        REFERENCES public.users(user_id) ON DELETE RESTRICT
);

CREATE OR REPLACE FUNCTION public.checklist_validate_action_command_target()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.task_kind = 'CHECKLIST' THEN
        PERFORM 1 FROM public.checklist_task_instances task
        WHERE task.checklist_task_instance_id = NEW.task_id;
    ELSE
        PERFORM 1 FROM public.care_tasks task WHERE task.task_id = NEW.task_id;
    END IF;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_ACTION_TARGET_NOT_FOUND';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_validate_action_command_target_trg
BEFORE INSERT ON public.checklist_action_commands
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_action_command_target();

CREATE TABLE public.checklist_distribution_outbox (
    outbox_event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_type varchar(80) NOT NULL,
    aggregate_type varchar(50) NOT NULL,
    aggregate_id uuid NOT NULL,
    payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    correlation_id uuid NOT NULL,
    occurred_at timestamptz DEFAULT now() NOT NULL,
    processed_at timestamptz,
    attempt_count integer DEFAULT 0 NOT NULL,
    next_attempt_at timestamptz,
    last_error_code varchar(80),
    CONSTRAINT checklist_distribution_outbox_pk PRIMARY KEY (outbox_event_id),
    CONSTRAINT checklist_distribution_outbox_attempt_ck CHECK (attempt_count >= 0)
);

CREATE INDEX checklist_distribution_outbox_pending_ix
    ON public.checklist_distribution_outbox(processed_at, next_attempt_at, occurred_at)
    WHERE processed_at IS NULL;

CREATE TABLE public.checklist_reconciliation_runs (
    reconciliation_run_id uuid DEFAULT gen_random_uuid() NOT NULL,
    correlation_id uuid NOT NULL,
    trigger_type varchar(20) NOT NULL,
    started_at timestamptz DEFAULT now() NOT NULL,
    completed_at timestamptz,
    created_count integer DEFAULT 0 NOT NULL,
    existing_count integer DEFAULT 0 NOT NULL,
    cancelled_count integer DEFAULT 0 NOT NULL,
    failed_count integer DEFAULT 0 NOT NULL,
    status varchar(20) DEFAULT 'RUNNING' NOT NULL,
    CONSTRAINT checklist_reconciliation_runs_pk PRIMARY KEY (reconciliation_run_id),
    CONSTRAINT checklist_reconciliation_runs_correlation_uk UNIQUE (correlation_id),
    CONSTRAINT checklist_reconciliation_runs_trigger_ck CHECK (trigger_type IN ('EVENT','HOURLY','STARTUP','REPLAY')),
    CONSTRAINT checklist_reconciliation_runs_status_ck CHECK (status IN ('RUNNING','SUCCEEDED','PARTIAL','FAILED')),
    CONSTRAINT checklist_reconciliation_runs_counts_ck CHECK (
        created_count >= 0 AND existing_count >= 0 AND cancelled_count >= 0 AND failed_count >= 0)
);

CREATE TABLE public.checklist_reconciliation_candidates (
    reconciliation_candidate_id uuid DEFAULT gen_random_uuid() NOT NULL,
    reconciliation_run_id uuid NOT NULL,
    recipient_user_id uuid,
    care_group_id uuid,
    care_context_type varchar(10),
    care_context_id uuid,
    window_start date,
    window_end date,
    outcome varchar(20) DEFAULT 'PENDING' NOT NULL,
    retry_count integer DEFAULT 0 NOT NULL,
    failure_code varchar(80),
    completed_at timestamptz,
    CONSTRAINT checklist_reconciliation_candidates_pk PRIMARY KEY (reconciliation_candidate_id),
    CONSTRAINT checklist_reconciliation_candidates_outcome_ck CHECK
        (outcome IN ('PENDING','CREATED','EXISTING','CANCELLED','FAILED','QUARANTINED')),
    CONSTRAINT checklist_reconciliation_candidates_retry_ck CHECK (retry_count >= 0),
    CONSTRAINT checklist_reconciliation_candidates_context_ck CHECK (
        (recipient_user_id IS NULL AND care_group_id IS NULL
            AND care_context_type IS NULL AND care_context_id IS NULL) OR
        (recipient_user_id IS NOT NULL AND care_group_id IS NOT NULL
            AND care_context_type IN ('JOURNEY','BABY') AND care_context_id IS NOT NULL)
    ),
    CONSTRAINT checklist_reconciliation_candidates_window_ck CHECK (
        (window_start IS NULL AND window_end IS NULL) OR
        (window_start IS NOT NULL AND window_end IS NOT NULL AND window_end >= window_start)
    ),
    CONSTRAINT checklist_reconciliation_candidates_identity_uk UNIQUE NULLS NOT DISTINCT
        (reconciliation_run_id, recipient_user_id, care_group_id,
         care_context_type, care_context_id, window_start, window_end),
    CONSTRAINT checklist_reconciliation_candidates_run_fk FOREIGN KEY (reconciliation_run_id)
        REFERENCES public.checklist_reconciliation_runs(reconciliation_run_id) ON DELETE CASCADE
);

ALTER TABLE public.care_group_members ALTER COLUMN permission_json SET DEFAULT '{}'::jsonb;
WITH normalized_permissions AS (
    SELECT care_group_member_id,
           CASE WHEN jsonb_typeof(permission_json) = 'object'
                THEN permission_json ELSE '{}'::jsonb END AS base_permission
    FROM public.care_group_members
)
UPDATE public.care_group_members member
SET permission_json = jsonb_set(
    jsonb_set(base_permission, '{CHECKLIST_VIEW}',
        CASE WHEN jsonb_typeof(base_permission->'CHECKLIST_VIEW') = 'boolean'
             THEN base_permission->'CHECKLIST_VIEW' ELSE 'false'::jsonb END, true),
    '{CHECKLIST_COMPLETE}',
        CASE WHEN jsonb_typeof(base_permission->'CHECKLIST_COMPLETE') = 'boolean'
             THEN base_permission->'CHECKLIST_COMPLETE' ELSE 'false'::jsonb END, true)
FROM normalized_permissions normalized
WHERE normalized.care_group_member_id = member.care_group_member_id;

ALTER TABLE public.audit_events
    ADD COLUMN actor_type varchar(20),
    ADD COLUMN actor_service varchar(80),
    ADD COLUMN reason_code varchar(80),
    ADD COLUMN care_context_type varchar(10),
    ADD COLUMN care_context_id uuid,
    ADD COLUMN template_version_id uuid,
    ADD COLUMN checklist_task_instance_id uuid;

ALTER TABLE public.audit_events
    ADD CONSTRAINT audit_events_checklist_actor_type_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         (actor_type IS NOT NULL AND actor_type IN ('USER','SYSTEM','SERVICE'))),
    ADD CONSTRAINT audit_events_checklist_actor_shape_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         ((actor_type = 'USER' AND actor_user_id IS NOT NULL AND actor_service IS NULL) OR
          (actor_type IN ('SYSTEM','SERVICE') AND actor_user_id IS NULL
            AND actor_service IS NOT NULL AND btrim(actor_service) <> ''))),
    ADD CONSTRAINT audit_events_checklist_correlation_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         correlation_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_context_type_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         care_context_type IS NULL OR care_context_type IN ('JOURNEY','BABY')),
    ADD CONSTRAINT audit_events_checklist_context_pair_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         ((care_context_type IS NULL AND care_context_id IS NULL) OR
          (care_context_type IS NOT NULL AND care_context_id IS NOT NULL))),
    ADD CONSTRAINT audit_events_checklist_subject_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED',
            'CHECKLIST_SKIPPED','CHECKLIST_CANCELLED') OR subject_user_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_task_ck CHECK
        (event_category NOT IN ('CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED') OR
         checklist_task_instance_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_template_ck CHECK
        (event_category NOT IN ('CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED') OR
         template_version_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_reason_code_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         reason_code IS NULL OR reason_code ~ '^[A-Z0-9_]{1,80}$'),
    ADD CONSTRAINT audit_events_checklist_reason_required_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_SKIPPED','CHECKLIST_CANCELLED',
            'CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         reason_code IS NOT NULL);

CREATE INDEX checklist_distribution_outbox_correlation_ix
    ON public.checklist_distribution_outbox(correlation_id, occurred_at DESC);

CREATE INDEX audit_events_checklist_correlation_ix
    ON public.audit_events(correlation_id, occurred_at DESC)
    WHERE event_category LIKE 'CHECKLIST_%';
