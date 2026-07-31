-- Expand checklist template metadata for the staged LEGACY -> DUAL -> REQUEST cutover.
-- This migration is additive: legacy metadata tables remain authoritative and no table is retired.

ALTER TABLE public.care_item_templates
ADD COLUMN recipient_scope varchar(10),
ADD COLUMN eligibility_anchor_type varchar(30),
ADD COLUMN eligibility_range_unit varchar(10),
ADD COLUMN eligibility_start_inclusive integer,
ADD COLUMN eligibility_end_inclusive integer;

ALTER TABLE public.care_item_templates
ADD CONSTRAINT care_item_templates_recipient_scope_domain_ck CHECK (
    recipient_scope IS NULL
    OR recipient_scope IN ('MOTHER', 'FAMILY', 'BOTH')
),
ADD CONSTRAINT care_item_templates_eligibility_anchor_domain_ck CHECK (
    eligibility_anchor_type IS NULL
    OR eligibility_anchor_type IN (
        'NONE',
        'LMP',
        'EDD',
        'DELIVERY_DATE',
        'BIRTH_DATE'
    )
),
ADD CONSTRAINT care_item_templates_eligibility_unit_domain_ck CHECK (
    eligibility_range_unit IS NULL
    OR eligibility_range_unit IN ('DAY', 'WEEK', 'MONTH')
),
ADD CONSTRAINT care_item_templates_eligibility_range_domain_ck CHECK (
    (
        eligibility_start_inclusive IS NULL
        AND eligibility_end_inclusive IS NULL
    )
    OR (
        eligibility_start_inclusive >= 0
        AND eligibility_end_inclusive >= eligibility_start_inclusive
    )
);

CREATE OR REPLACE FUNCTION public.checklist_sync_inline_recipient_scope()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    affected_version_id uuid;
    final_scope varchar(10);
    previous_scope varchar(10);
BEGIN
    affected_version_id := CASE WHEN TG_OP = 'DELETE'
        THEN OLD.template_version_id ELSE NEW.template_version_id END;

    SELECT CASE
               WHEN bool_or(recipient_role = 'MOTHER')
                    AND bool_or(recipient_role = 'FAMILY') THEN 'BOTH'
               WHEN bool_or(recipient_role = 'MOTHER') THEN 'MOTHER'
               WHEN bool_or(recipient_role = 'FAMILY') THEN 'FAMILY'
               ELSE NULL
           END
      INTO final_scope
      FROM public.checklist_template_recipient_roles
     WHERE template_version_id = affected_version_id;

    UPDATE public.care_item_templates root
       SET recipient_scope = final_scope
     WHERE root.entry_type = 'TEMPLATE_ROOT'
       AND root.template_version_id = affected_version_id
       AND root.recipient_scope IS DISTINCT FROM final_scope;

    IF TG_OP = 'UPDATE'
       AND OLD.template_version_id IS DISTINCT FROM NEW.template_version_id THEN
        SELECT CASE
                   WHEN bool_or(recipient_role = 'MOTHER')
                        AND bool_or(recipient_role = 'FAMILY') THEN 'BOTH'
                   WHEN bool_or(recipient_role = 'MOTHER') THEN 'MOTHER'
                   WHEN bool_or(recipient_role = 'FAMILY') THEN 'FAMILY'
                   ELSE NULL
               END
          INTO previous_scope
          FROM public.checklist_template_recipient_roles
         WHERE template_version_id = OLD.template_version_id;

        UPDATE public.care_item_templates root
           SET recipient_scope = previous_scope
         WHERE root.entry_type = 'TEMPLATE_ROOT'
           AND root.template_version_id = OLD.template_version_id
           AND root.recipient_scope IS DISTINCT FROM previous_scope;
    END IF;

    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER checklist_sync_inline_recipient_scope_trg
AFTER INSERT OR UPDATE OR DELETE ON public.checklist_template_recipient_roles
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_inline_recipient_scope();

CREATE OR REPLACE FUNCTION public.checklist_sync_inline_eligibility()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' OR NEW.substage_id IS NULL THEN
        NEW.eligibility_anchor_type := NULL;
        NEW.eligibility_range_unit := NULL;
        NEW.eligibility_start_inclusive := NULL;
        NEW.eligibility_end_inclusive := NULL;
        RETURN NEW;
    END IF;

    SELECT substage.anchor_type,
           substage.range_unit,
           substage.start_inclusive,
           substage.end_inclusive
      INTO NEW.eligibility_anchor_type,
           NEW.eligibility_range_unit,
           NEW.eligibility_start_inclusive,
           NEW.eligibility_end_inclusive
      FROM public.checklist_substages substage
     WHERE substage.substage_id = NEW.substage_id;

    RETURN NEW;
END $$;

CREATE TRIGGER checklist_sync_inline_eligibility_trg
BEFORE INSERT OR UPDATE OF substage_id, entry_type ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_inline_eligibility();

CREATE OR REPLACE FUNCTION public.checklist_sync_inline_eligibility_from_substage()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE public.care_item_templates root
       SET eligibility_anchor_type = NEW.anchor_type,
           eligibility_range_unit = NEW.range_unit,
           eligibility_start_inclusive = NEW.start_inclusive,
           eligibility_end_inclusive = NEW.end_inclusive
     WHERE root.entry_type = 'TEMPLATE_ROOT'
       AND root.substage_id = NEW.substage_id
       AND (root.eligibility_anchor_type,
            root.eligibility_range_unit,
            root.eligibility_start_inclusive,
            root.eligibility_end_inclusive) IS DISTINCT FROM
           (NEW.anchor_type, NEW.range_unit, NEW.start_inclusive, NEW.end_inclusive);
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_sync_inline_eligibility_from_substage_trg
AFTER UPDATE OF stage, anchor_type, range_unit, start_inclusive, end_inclusive
ON public.checklist_substages
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_inline_eligibility_from_substage();

WITH
    role_scope AS (
        SELECT
            role.template_version_id,
            CASE
                WHEN bool_or(
                    role.recipient_role = 'MOTHER'
                )
                AND bool_or(
                    role.recipient_role = 'FAMILY'
                ) THEN 'BOTH'
                WHEN bool_or(
                    role.recipient_role = 'MOTHER'
                ) THEN 'MOTHER'
                WHEN bool_or(
                    role.recipient_role = 'FAMILY'
                ) THEN 'FAMILY'
            END AS recipient_scope
        FROM public.checklist_template_recipient_roles role
        GROUP BY
            role.template_version_id
    )
UPDATE public.care_item_templates root
SET
    recipient_scope = role_scope.recipient_scope
FROM role_scope
WHERE
    root.entry_type = 'TEMPLATE_ROOT'
    AND root.template_version_id = role_scope.template_version_id;

UPDATE public.care_item_templates root
SET
    eligibility_anchor_type = substage.anchor_type,
    eligibility_range_unit = substage.range_unit,
    eligibility_start_inclusive = substage.start_inclusive,
    eligibility_end_inclusive = substage.end_inclusive
FROM public.checklist_substages substage
WHERE
    root.entry_type = 'TEMPLATE_ROOT'
    AND root.substage_id = substage.substage_id;

UPDATE public.care_item_templates item
SET
    recipient_scope = NULL,
    eligibility_anchor_type = NULL,
    eligibility_range_unit = NULL,
    eligibility_start_inclusive = NULL,
    eligibility_end_inclusive = NULL
WHERE
    item.entry_type <> 'TEMPLATE_ROOT';

CREATE OR REPLACE FUNCTION public.checklist_validate_inline_template_shape()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    current_root public.care_item_templates%ROWTYPE;
    expected_scope varchar(10);
    expected_anchor varchar(30);
    expected_unit varchar(10);
    expected_start integer;
    expected_end integer;
BEGIN
    SELECT * INTO current_root
      FROM public.care_item_templates template
     WHERE template.template_id = NEW.template_id;

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    IF current_root.entry_type <> 'TEMPLATE_ROOT' THEN
        IF current_root.recipient_scope IS NOT NULL
           OR current_root.eligibility_anchor_type IS NOT NULL
           OR current_root.eligibility_range_unit IS NOT NULL
           OR current_root.eligibility_start_inclusive IS NOT NULL
           OR current_root.eligibility_end_inclusive IS NOT NULL THEN
            RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
        END IF;
        RETURN NULL;
    END IF;

    SELECT CASE
               WHEN bool_or(role.recipient_role = 'MOTHER')
                    AND bool_or(role.recipient_role = 'FAMILY') THEN 'BOTH'
               WHEN bool_or(role.recipient_role = 'MOTHER') THEN 'MOTHER'
               WHEN bool_or(role.recipient_role = 'FAMILY') THEN 'FAMILY'
               ELSE NULL
           END
      INTO expected_scope
      FROM public.checklist_template_recipient_roles role
     WHERE role.template_version_id = current_root.template_version_id;

    IF expected_scope IS NULL THEN
        RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED';
    END IF;

    IF current_root.recipient_scope IS DISTINCT FROM expected_scope THEN
        UPDATE public.care_item_templates
           SET recipient_scope = expected_scope
         WHERE template_id = current_root.template_id;
        current_root.recipient_scope := expected_scope;
    END IF;

    IF current_root.recipient_scope = 'FAMILY' THEN
        IF current_root.stage IS NOT NULL
           OR current_root.substage_id IS NOT NULL
           OR current_root.eligibility_anchor_type IS NOT NULL
           OR current_root.eligibility_range_unit IS NOT NULL
           OR current_root.eligibility_start_inclusive IS NOT NULL
           OR current_root.eligibility_end_inclusive IS NOT NULL THEN
            RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
        END IF;
        RETURN NULL;
    END IF;

    IF current_root.stage IS NULL
       OR current_root.substage_id IS NULL
       OR current_root.eligibility_anchor_type IS NULL
       OR current_root.eligibility_range_unit IS NULL
       OR current_root.eligibility_start_inclusive IS NULL
       OR current_root.eligibility_end_inclusive IS NULL
       OR current_root.eligibility_start_inclusive < 0
       OR current_root.eligibility_end_inclusive < current_root.eligibility_start_inclusive THEN
        RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
    END IF;

    SELECT substage.anchor_type,
           substage.range_unit,
           substage.start_inclusive,
           substage.end_inclusive
      INTO expected_anchor, expected_unit, expected_start, expected_end
      FROM public.checklist_substages substage
     WHERE substage.substage_id = current_root.substage_id;

    IF NOT FOUND
       OR current_root.eligibility_anchor_type IS DISTINCT FROM expected_anchor
       OR current_root.eligibility_range_unit IS DISTINCT FROM expected_unit
       OR current_root.eligibility_start_inclusive IS DISTINCT FROM expected_start
       OR current_root.eligibility_end_inclusive IS DISTINCT FROM expected_end THEN
        RAISE EXCEPTION 'INLINE_TEMPLATE_METADATA_MISMATCH';
    END IF;

    -- Imported roots keep their historical all-stage window while they remain explicitly
    -- review-blocked. Clearing the review flag re-enters the strict target-shape checks below.
    IF current_root.migration_review_required = true
       AND current_root.eligibility_anchor_type = 'NONE'
       AND current_root.eligibility_range_unit = 'DAY'
       AND current_root.eligibility_start_inclusive = 0
       AND current_root.eligibility_end_inclusive = 2147483647 THEN
        RETURN NULL;
    END IF;

    IF NOT (
        (current_root.stage = 'PRE_PREGNANCY'
         AND current_root.eligibility_anchor_type = 'NONE'
         AND current_root.eligibility_range_unit = 'DAY'
         AND current_root.eligibility_start_inclusive = 0
         AND current_root.eligibility_end_inclusive = 0)
        OR (current_root.stage = 'PREGNANCY'
            AND current_root.eligibility_anchor_type IN ('LMP', 'EDD'))
        OR (current_root.stage = 'POSTPARTUM'
            AND current_root.eligibility_anchor_type = 'DELIVERY_DATE')
        OR (current_root.stage = 'BABY_CARE'
            AND current_root.eligibility_anchor_type = 'BIRTH_DATE')
    ) THEN
        RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
    END IF;

    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER checklist_validate_inline_template_shape_trg
AFTER INSERT OR UPDATE OF entry_type, template_version_id, stage, substage_id,
    content_status, distribution_enabled, migration_review_required,
    recipient_scope, eligibility_anchor_type, eligibility_range_unit,
    eligibility_start_inclusive, eligibility_end_inclusive
ON public.care_item_templates
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_inline_template_shape();

-- Queue deferred validation for the backfilled final state without adding an immediate
-- CHECK that would reject old binaries which insert a template root before its role rows.
UPDATE public.care_item_templates
SET
    recipient_scope = recipient_scope;