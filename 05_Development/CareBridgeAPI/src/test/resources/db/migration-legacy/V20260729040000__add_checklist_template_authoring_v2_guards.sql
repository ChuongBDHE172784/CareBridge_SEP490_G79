-- Checklist Distribution V2 Phase 2: immutable approved authoring versions and activation guards.

ALTER TABLE public.care_item_templates
    ADD COLUMN migration_reviewed_at timestamptz,
    ADD COLUMN migration_reviewed_by uuid;

-- Normalize imported approved roots into the correctable review state before guards are installed.
UPDATE public.care_item_templates
SET content_status = 'PENDING_REVIEW',
    distribution_enabled = false,
    approved_at = NULL,
    approved_by = NULL
WHERE entry_type = 'TEMPLATE_ROOT'
  AND migration_review_required = true
  AND content_status = 'APPROVED';

ALTER TABLE public.care_item_templates
    DROP CONSTRAINT care_item_templates_distribution_gate_ck,
    ADD CONSTRAINT care_item_templates_distribution_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR distribution_enabled = false OR
         (content_status = 'APPROVED' AND migration_review_required = false
          AND approved_at IS NOT NULL AND approved_by IS NOT NULL)),
    ADD CONSTRAINT care_item_templates_approved_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR content_status <> 'APPROVED' OR
         (migration_review_required = false
          AND approved_at IS NOT NULL AND approved_by IS NOT NULL));

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_import_activation_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR migration_reviewed_at IS NULL
         OR content_status <> 'APPROVED' OR distribution_enabled = true);

CREATE UNIQUE INDEX care_item_templates_lineage_version_no_uk
    ON public.care_item_templates(template_lineage_id, version)
    WHERE entry_type = 'TEMPLATE_ROOT';

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

    -- Activation and archive may change state only; reviewed content identity must remain exact.
    reviewed_content_unchanged :=
        NEW.title IS NOT DISTINCT FROM OLD.title
        AND NEW.description IS NOT DISTINCT FROM OLD.description
        AND NEW.stage IS NOT DISTINCT FROM OLD.stage
        AND NEW.substage_id IS NOT DISTINCT FROM OLD.substage_id
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
        AND NEW.distribution_enabled = true
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

CREATE TRIGGER checklist_guard_approved_template_mutation_trg
BEFORE UPDATE OR DELETE ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_approved_template_mutation();

CREATE OR REPLACE FUNCTION public.checklist_guard_referenced_substage_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.care_item_templates root
        WHERE root.substage_id = OLD.substage_id
          AND root.entry_type = 'TEMPLATE_ROOT'
          AND (root.content_status IN ('APPROVED', 'ARCHIVED')
               OR root.migration_reviewed_at IS NOT NULL)
    ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER checklist_guard_referenced_substage_mutation_trg
BEFORE UPDATE OR DELETE ON public.checklist_substages
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_referenced_substage_mutation();

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_item_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    parent_id uuid;
    parent_status varchar(20);
    parent_review_required boolean;
    parent_reviewed_at timestamptz;
BEGIN
    IF (TG_OP = 'DELETE' AND OLD.entry_type <> 'CHECKLIST_ENTRY')
       OR (TG_OP = 'INSERT' AND NEW.entry_type <> 'CHECKLIST_ENTRY')
       OR (TG_OP = 'UPDATE'
           AND OLD.entry_type <> 'CHECKLIST_ENTRY'
           AND NEW.entry_type <> 'CHECKLIST_ENTRY') THEN
        RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.parent_template_id IS DISTINCT FROM NEW.parent_template_id
       AND EXISTS (
           SELECT 1
           FROM public.care_item_templates root
           WHERE root.template_id IN (OLD.parent_template_id, NEW.parent_template_id)
             AND root.entry_type = 'TEMPLATE_ROOT'
             AND (root.content_status IN ('APPROVED', 'ARCHIVED')
                  OR root.migration_reviewed_at IS NOT NULL)
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;

    parent_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.parent_template_id ELSE NEW.parent_template_id END;
    IF parent_id IS NULL THEN
        RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
    END IF;

    SELECT content_status, migration_review_required, migration_reviewed_at
    INTO parent_status, parent_review_required, parent_reviewed_at
    FROM public.care_item_templates
    WHERE template_id = parent_id AND entry_type = 'TEMPLATE_ROOT';

    IF (parent_status IN ('APPROVED', 'ARCHIVED') AND parent_review_required = false)
       OR parent_reviewed_at IS NOT NULL THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER checklist_guard_approved_item_mutation_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_approved_item_mutation();

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_role_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    version_id uuid;
    version_status varchar(20);
    version_review_required boolean;
    version_reviewed_at timestamptz;
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.template_version_id IS DISTINCT FROM NEW.template_version_id
       AND EXISTS (
           SELECT 1
           FROM public.care_item_templates root
           WHERE root.template_version_id IN (OLD.template_version_id, NEW.template_version_id)
             AND root.entry_type = 'TEMPLATE_ROOT'
             AND (root.content_status IN ('APPROVED', 'ARCHIVED')
                  OR root.migration_reviewed_at IS NOT NULL)
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;

    version_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.template_version_id ELSE NEW.template_version_id END;
    SELECT content_status, migration_review_required, migration_reviewed_at
    INTO version_status, version_review_required, version_reviewed_at
    FROM public.care_item_templates
    WHERE template_version_id = version_id AND entry_type = 'TEMPLATE_ROOT';

    IF (version_status IN ('APPROVED', 'ARCHIVED') AND version_review_required = false)
       OR version_reviewed_at IS NOT NULL THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER checklist_guard_approved_role_mutation_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.checklist_template_recipient_roles
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_approved_role_mutation();

CREATE OR REPLACE FUNCTION public.checklist_guard_version_item_authority_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    version_id uuid;
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.template_version_id IS DISTINCT FROM NEW.template_version_id
       AND EXISTS (
           SELECT 1 FROM public.care_item_templates root
           WHERE root.template_version_id IN (OLD.template_version_id, NEW.template_version_id)
             AND root.entry_type = 'TEMPLATE_ROOT'
             AND (root.content_status IN ('APPROVED', 'ARCHIVED')
                  OR root.migration_reviewed_at IS NOT NULL)
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;

    version_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.template_version_id ELSE NEW.template_version_id END;
    IF EXISTS (
        SELECT 1 FROM public.care_item_templates root
        WHERE root.template_version_id = version_id
          AND root.entry_type = 'TEMPLATE_ROOT'
          AND (root.content_status IN ('APPROVED', 'ARCHIVED')
               OR root.migration_reviewed_at IS NOT NULL)
    ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER checklist_guard_version_item_authority_mutation_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.checklist_template_version_items
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_version_item_authority_mutation();

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
               OR substage_anchor IS NULL OR substage_anchor = 'NONE'
               OR NOT (
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

        -- The maintained authority table must contain every active versioned item.
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

CREATE TRIGGER checklist_validate_template_approval_trg
BEFORE INSERT OR UPDATE OF content_status, distribution_enabled, migration_review_required,
    stage, substage_id, template_version_id
ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_template_approval();
