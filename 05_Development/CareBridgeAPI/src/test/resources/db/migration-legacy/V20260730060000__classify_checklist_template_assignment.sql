ALTER TABLE care_item_templates
    ADD COLUMN template_type varchar(20) DEFAULT 'MANDATORY' NOT NULL;

ALTER TABLE care_item_templates
    ADD CONSTRAINT ck_care_item_templates_template_type
        CHECK (template_type IN ('MANDATORY', 'OPTIONAL'));

COMMENT ON COLUMN care_item_templates.template_type IS
    'MANDATORY templates are auto-distributed; OPTIONAL templates are added explicitly by users.';

INSERT INTO public.checklist_substages
    (code, stage, anchor_type, range_unit, start_inclusive, end_inclusive)
VALUES ('PRE_PREGNANCY_ALL', 'PRE_PREGNANCY', 'NONE', 'DAY', 0, 0)
ON CONFLICT (code) DO NOTHING;

CREATE OR REPLACE FUNCTION public.checklist_guard_template_type_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.entry_type = 'TEMPLATE_ROOT'
       AND NEW.template_type IS DISTINCT FROM OLD.template_type THEN
        IF OLD.content_status IN ('APPROVED', 'ARCHIVED') THEN
            RAISE EXCEPTION 'VERSION_IMMUTABLE';
        END IF;
        IF OLD.migration_reviewed_at IS NOT NULL THEN
            RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_guard_template_type_mutation_trg
BEFORE UPDATE OF template_type ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_template_type_mutation();

CREATE OR REPLACE FUNCTION public.checklist_validate_template_approval()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    role_count integer;
    mother_count integer;
    family_count integer;
    substage_stage varchar(30);
    substage_anchor varchar(30);
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' THEN
        RETURN NEW;
    END IF;

    IF (NEW.distribution_enabled = true OR NEW.content_status = 'APPROVED')
       AND NEW.migration_review_required = true THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF TG_OP = 'UPDATE' AND NEW.distribution_enabled = true
       AND OLD.migration_review_required = true THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF TG_OP = 'UPDATE'
       AND OLD.migration_review_required = true AND NEW.migration_review_required = false
       AND (NEW.migration_reviewed_at IS NULL OR NEW.migration_reviewed_by IS NULL) THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF NEW.distribution_enabled = true
       OR NEW.content_status = 'APPROVED'
       OR (TG_OP = 'UPDATE' AND OLD.migration_review_required = true
           AND NEW.migration_review_required = false) THEN
        SELECT count(*),
               count(*) FILTER (WHERE recipient_role = 'MOTHER'),
               count(*) FILTER (WHERE recipient_role = 'FAMILY')
        INTO role_count, mother_count, family_count
        FROM public.checklist_template_recipient_roles
        WHERE template_version_id = NEW.template_version_id;

        IF role_count = 0 THEN
            RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED';
        END IF;

        IF family_count > 0 AND mother_count = 0
           AND (NEW.stage IS NOT NULL OR NEW.substage_id IS NOT NULL) THEN
            RAISE EXCEPTION 'FAMILY_STAGE_NOT_ALLOWED';
        END IF;

        IF NEW.substage_id IS NOT NULL THEN
            SELECT stage, anchor_type INTO substage_stage, substage_anchor
            FROM public.checklist_substages
            WHERE substage_id = NEW.substage_id
              AND is_active = true;
            IF substage_stage IS NULL OR substage_stage IS DISTINCT FROM NEW.stage
               OR substage_anchor IS NULL
               OR NOT (
                   (NEW.stage = 'PRE_PREGNANCY' AND substage_anchor = 'NONE') OR
                   (NEW.stage = 'PREGNANCY' AND substage_anchor IN ('LMP', 'EDD')) OR
                   (NEW.stage = 'POSTPARTUM' AND substage_anchor = 'DELIVERY_DATE') OR
                   (NEW.stage = 'BABY_CARE' AND substage_anchor = 'BIRTH_DATE')
               ) THEN
                RAISE EXCEPTION 'SUBSTAGE_STAGE_MISMATCH';
            END IF;
        ELSIF NEW.stage IS NOT NULL THEN
            RAISE EXCEPTION 'SUBSTAGE_STAGE_MISMATCH';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM public.care_item_templates item
            WHERE item.parent_template_id = NEW.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY'
              AND item.is_active = true
              AND item.target_subject IS NULL
        ) THEN
            RAISE EXCEPTION 'ITEM_TARGET_REQUIRED';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM public.care_item_templates item
            WHERE item.parent_template_id = NEW.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY'
              AND item.is_active = true
              AND NOT EXISTS (
                  SELECT 1
                  FROM public.checklist_template_version_items version_item
                  WHERE version_item.template_version_id = NEW.template_version_id
                    AND version_item.template_item_version_id = item.template_id
              )
        ) THEN
            RAISE EXCEPTION 'ITEM_TARGET_REQUIRED';
        END IF;
    END IF;
    RETURN NEW;
END $$;

ALTER TABLE public.care_item_templates
    DROP CONSTRAINT care_item_templates_import_activation_gate_ck,
    ADD CONSTRAINT care_item_templates_import_activation_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR migration_reviewed_at IS NULL
         OR content_status <> 'APPROVED'
         OR distribution_enabled = (template_type = 'MANDATORY'));

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_template_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    allowed_migration_review boolean;
    allowed_review_invalidation boolean;
    allowed_review_activation boolean;
    allowed_review_archive boolean;
    reviewed_content_unchanged boolean;
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.entry_type = 'TEMPLATE_ROOT'
           AND OLD.content_status IN ('APPROVED', 'ARCHIVED') THEN
            RAISE EXCEPTION 'VERSION_IMMUTABLE';
        END IF;
        RETURN OLD;
    END IF;

    allowed_migration_review := OLD.entry_type = 'TEMPLATE_ROOT'
        AND OLD.content_status = 'APPROVED'
        AND OLD.migration_review_required = true
        AND OLD.distribution_enabled = false
        AND NEW.content_status = 'PENDING_REVIEW'
        AND NEW.migration_review_required = false
        AND NEW.migration_reviewed_at IS NOT NULL
        AND NEW.migration_reviewed_by IS NOT NULL
        AND NEW.distribution_enabled = false
        AND NEW.approved_at IS NULL
        AND NEW.approved_by IS NULL;

    allowed_review_invalidation := OLD.entry_type = 'TEMPLATE_ROOT'
        AND OLD.migration_reviewed_at IS NOT NULL
        AND NEW.content_status IN ('DRAFT', 'PENDING_REVIEW')
        AND NEW.migration_review_required = true
        AND NEW.migration_reviewed_at IS NULL
        AND NEW.migration_reviewed_by IS NULL
        AND NEW.distribution_enabled = false
        AND NEW.approved_at IS NULL
        AND NEW.approved_by IS NULL;

    reviewed_content_unchanged :=
        NEW.title IS NOT DISTINCT FROM OLD.title
        AND NEW.description IS NOT DISTINCT FROM OLD.description
        AND NEW.stage IS NOT DISTINCT FROM OLD.stage
        AND NEW.substage_id IS NOT DISTINCT FROM OLD.substage_id
        AND NEW.template_type IS NOT DISTINCT FROM OLD.template_type
        AND NEW.template_lineage_id IS NOT DISTINCT FROM OLD.template_lineage_id
        AND NEW.template_version_id IS NOT DISTINCT FROM OLD.template_version_id
        AND NEW.version IS NOT DISTINCT FROM OLD.version
        AND NEW.entry_type IS NOT DISTINCT FROM OLD.entry_type
        AND NEW.author_user_id IS NOT DISTINCT FROM OLD.author_user_id;

    allowed_review_activation := OLD.entry_type = 'TEMPLATE_ROOT'
        AND OLD.migration_reviewed_at IS NOT NULL
        AND reviewed_content_unchanged
        AND OLD.content_status = 'PENDING_REVIEW'
        AND NEW.content_status = 'APPROVED'
        AND NEW.migration_review_required = false
        AND NEW.migration_reviewed_at IS NOT DISTINCT FROM OLD.migration_reviewed_at
        AND NEW.migration_reviewed_by IS NOT DISTINCT FROM OLD.migration_reviewed_by
        AND NEW.distribution_enabled = (NEW.template_type = 'MANDATORY')
        AND NEW.approved_at IS NOT NULL
        AND NEW.approved_by IS NOT NULL;

    allowed_review_archive := OLD.entry_type = 'TEMPLATE_ROOT'
        AND OLD.migration_reviewed_at IS NOT NULL
        AND reviewed_content_unchanged
        AND NEW.content_status = 'ARCHIVED'
        AND NEW.migration_review_required IS NOT DISTINCT FROM OLD.migration_review_required
        AND NEW.migration_reviewed_at IS NOT DISTINCT FROM OLD.migration_reviewed_at
        AND NEW.migration_reviewed_by IS NOT DISTINCT FROM OLD.migration_reviewed_by
        AND NEW.distribution_enabled = false
        AND NEW.approved_at IS NOT DISTINCT FROM OLD.approved_at
        AND NEW.approved_by IS NOT DISTINCT FROM OLD.approved_by;

    IF OLD.entry_type = 'TEMPLATE_ROOT'
       AND OLD.migration_reviewed_at IS NOT NULL
       AND (
           NEW.title IS DISTINCT FROM OLD.title OR
           NEW.description IS DISTINCT FROM OLD.description OR
           NEW.stage IS DISTINCT FROM OLD.stage OR
           NEW.substage_id IS DISTINCT FROM OLD.substage_id OR
           NEW.template_type IS DISTINCT FROM OLD.template_type OR
           NEW.template_lineage_id IS DISTINCT FROM OLD.template_lineage_id OR
           NEW.template_version_id IS DISTINCT FROM OLD.template_version_id OR
           NEW.version IS DISTINCT FROM OLD.version OR
           NEW.entry_type IS DISTINCT FROM OLD.entry_type OR
           NEW.author_user_id IS DISTINCT FROM OLD.author_user_id OR
           NEW.content_status IS DISTINCT FROM OLD.content_status OR
           NEW.migration_review_required IS DISTINCT FROM OLD.migration_review_required OR
           NEW.migration_reviewed_at IS DISTINCT FROM OLD.migration_reviewed_at OR
           NEW.migration_reviewed_by IS DISTINCT FROM OLD.migration_reviewed_by OR
           NEW.distribution_enabled IS DISTINCT FROM OLD.distribution_enabled OR
           NEW.approved_at IS DISTINCT FROM OLD.approved_at OR
           NEW.approved_by IS DISTINCT FROM OLD.approved_by
       )
       AND NOT allowed_review_invalidation
       AND NOT allowed_review_activation
       AND NOT allowed_review_archive THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF OLD.entry_type = 'TEMPLATE_ROOT'
       AND OLD.content_status IN ('APPROVED', 'ARCHIVED')
       AND (
           NEW.title IS DISTINCT FROM OLD.title OR
           NEW.description IS DISTINCT FROM OLD.description OR
           NEW.stage IS DISTINCT FROM OLD.stage OR
           NEW.substage_id IS DISTINCT FROM OLD.substage_id OR
           NEW.template_type IS DISTINCT FROM OLD.template_type OR
           NEW.template_lineage_id IS DISTINCT FROM OLD.template_lineage_id OR
           NEW.template_version_id IS DISTINCT FROM OLD.template_version_id OR
           NEW.version IS DISTINCT FROM OLD.version OR
           NEW.entry_type IS DISTINCT FROM OLD.entry_type OR
           NEW.author_user_id IS DISTINCT FROM OLD.author_user_id OR
           (NOT allowed_migration_review AND (
               NEW.migration_review_required IS DISTINCT FROM OLD.migration_review_required OR
               NEW.migration_reviewed_at IS DISTINCT FROM OLD.migration_reviewed_at OR
               NEW.migration_reviewed_by IS DISTINCT FROM OLD.migration_reviewed_by OR
               (OLD.content_status = 'APPROVED'
                   AND NEW.content_status NOT IN ('APPROVED', 'ARCHIVED')) OR
               (OLD.content_status = 'ARCHIVED'
                   AND NEW.content_status <> 'ARCHIVED') OR
               (OLD.content_status = 'APPROVED'
                   AND NEW.distribution_enabled IS DISTINCT FROM OLD.distribution_enabled
                   AND NOT (NEW.content_status = 'ARCHIVED' AND NEW.distribution_enabled = false)) OR
               (OLD.content_status = 'ARCHIVED' AND NEW.distribution_enabled = true) OR
               NEW.approved_at IS DISTINCT FROM OLD.approved_at OR
               NEW.approved_by IS DISTINCT FROM OLD.approved_by
           ))
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN NEW;
END $$;
