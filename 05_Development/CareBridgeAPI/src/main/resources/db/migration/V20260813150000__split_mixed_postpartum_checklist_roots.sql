-- Repair legacy POSTPARTUM catalog aggregates that mix maternal and baby leaves.
-- Existing instances/tasks remain pinned to the archived source version; only
-- future reconciliation sees the two homogeneous replacement roots.

SET lock_timeout = '30s';
LOCK TABLE public.care_item_templates IN SHARE ROW EXCLUSIVE MODE;

-- POSTPARTUM is the runtime stage for both maternal and baby care. Its context
-- is selected by the lifecycle anchor, so BIRTH_DATE is a valid root anchor.
CREATE OR REPLACE FUNCTION public.checklist_validate_inline_template_shape()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' THEN
        IF NEW.recipient_scope IS NOT NULL OR NEW.eligibility_anchor_type IS NOT NULL
           OR NEW.eligibility_range_unit IS NOT NULL
           OR NEW.eligibility_start_inclusive IS NOT NULL
           OR NEW.eligibility_end_inclusive IS NOT NULL THEN
            RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
        END IF;
        RETURN NULL;
    END IF;
    IF NEW.recipient_scope IS NULL THEN RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED'; END IF;
    IF NEW.recipient_scope = 'FAMILY' THEN
        IF NEW.stage IS NOT NULL OR NEW.eligibility_anchor_type IS NOT NULL
           OR NEW.eligibility_range_unit IS NOT NULL
           OR NEW.eligibility_start_inclusive IS NOT NULL
           OR NEW.eligibility_end_inclusive IS NOT NULL THEN
            RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
        END IF;
        RETURN NULL;
    END IF;
    IF NEW.stage IS NULL OR NEW.eligibility_anchor_type IS NULL
       OR NEW.eligibility_range_unit IS NULL OR NEW.eligibility_start_inclusive IS NULL
       OR NEW.eligibility_end_inclusive IS NULL
       OR NEW.eligibility_start_inclusive < 0
       OR NEW.eligibility_end_inclusive < NEW.eligibility_start_inclusive
       OR NOT (
           (NEW.stage = 'PRE_PREGNANCY' AND NEW.eligibility_anchor_type = 'NONE'
            AND NEW.eligibility_range_unit = 'DAY'
            AND NEW.eligibility_start_inclusive = 0 AND NEW.eligibility_end_inclusive = 0)
           OR (NEW.migration_review_required AND NEW.eligibility_anchor_type = 'NONE'
               AND NEW.eligibility_range_unit = 'DAY'
               AND NEW.eligibility_start_inclusive = 0
               AND NEW.eligibility_end_inclusive = 2147483647)
           OR (NEW.stage = 'PREGNANCY' AND NEW.eligibility_anchor_type IN ('LMP','EDD'))
           OR (NEW.stage = 'POSTPARTUM'
               AND NEW.eligibility_anchor_type IN ('DELIVERY_DATE','BIRTH_DATE'))
           OR (NEW.stage = 'BABY_CARE' AND NEW.eligibility_anchor_type = 'BIRTH_DATE')) THEN
        RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
    END IF;
    RETURN NULL;
END $$;

CREATE TEMP TABLE checklist_postpartum_mixed_roots ON COMMIT DROP AS
SELECT root.template_id AS source_root_id,
       public.checklist_p2_deterministic_uuid('POSTPARTUM_SPLIT|MOTHER|ROOT|' || root.template_id) AS mother_root_id,
       public.checklist_p2_deterministic_uuid('POSTPARTUM_SPLIT|BABY|ROOT|' || root.template_id) AS baby_root_id,
       root.approved_at AS source_approved_at,
       root.approved_by AS source_approved_by
  FROM public.care_item_templates root
 WHERE root.entry_type = 'TEMPLATE_ROOT'
   AND root.stage = 'POSTPARTUM'
   AND root.content_status = 'APPROVED'
   AND root.distribution_enabled
   AND EXISTS (
       SELECT 1 FROM public.care_item_templates item
        WHERE item.parent_template_id = root.template_id
          AND item.entry_type = 'CHECKLIST_ENTRY' AND item.is_active
          AND ((coalesce(item.checklist_contract_version, 1) = 1
                AND item.target_subject = 'MOTHER')
            OR (item.checklist_contract_version = 2
                AND item.target_subject IS NULL
                AND item.due_anchor_type = 'DELIVERY_DATE')))
   AND EXISTS (
       SELECT 1 FROM public.care_item_templates item
        WHERE item.parent_template_id = root.template_id
          AND item.entry_type = 'CHECKLIST_ENTRY' AND item.is_active
          AND ((coalesce(item.checklist_contract_version, 1) = 1
                AND item.target_subject = 'BABY')
            OR (item.checklist_contract_version = 2
                AND item.target_subject IS NULL
                AND item.due_anchor_type = 'BIRTH_DATE')));

-- Fail closed before touching the catalog. Every leaf (including inactive
-- leaves) must be classifiable; otherwise a clone would silently lose source
-- provenance/content and the migration would appear to succeed.
DO $$
DECLARE invalid_count bigint;
BEGIN
    SELECT count(*) INTO invalid_count
      FROM checklist_postpartum_mixed_roots mixed
     JOIN public.care_item_templates item ON item.parent_template_id = mixed.source_root_id
     WHERE item.entry_type = 'CHECKLIST_ENTRY'
       AND NOT (
           (coalesce(item.checklist_contract_version, 1) = 1
            AND ((item.target_subject = 'MOTHER'
                  AND (item.due_anchor_type IS NULL OR item.due_anchor_type = 'DELIVERY_DATE'))
              OR (item.target_subject = 'BABY'
                  AND (item.due_anchor_type IS NULL OR item.due_anchor_type = 'BIRTH_DATE'))))
           OR (item.checklist_contract_version = 2
               AND item.target_subject IS NULL
               AND item.due_anchor_type IN ('DELIVERY_DATE','BIRTH_DATE')));
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'POSTPARTUM_MIXED_ROOT_UNSPLITTABLE: invalid_leaves=%', invalid_count;
    END IF;
END $$;

-- Build replacements as non-distributable drafts so ordinary immutability and
-- approval triggers remain enabled throughout the migration. Activate only
-- after every leaf has been cloned successfully.
INSERT INTO public.care_item_templates
SELECT (jsonb_populate_record(NULL::public.care_item_templates,
           to_jsonb(root) || jsonb_build_object(
               'template_id', mixed.mother_root_id,
               'template_lineage_id', mixed.mother_root_id,
               'template_version_id', mixed.mother_root_id,
               'title', root.title || ' - Mother',
               'eligibility_anchor_type', 'DELIVERY_DATE',
               'schedule_context_type', 'JOURNEY',
               'content_status', 'DRAFT', 'distribution_enabled', false,
               'approved_at', NULL, 'approved_by', NULL,
               'migration_review_required', false,
               'migration_reviewed_at', NULL, 'migration_reviewed_by', NULL,
               'created_at', clock_timestamp(), 'updated_at', clock_timestamp()
           ))).*
  FROM checklist_postpartum_mixed_roots mixed
  JOIN public.care_item_templates root ON root.template_id = mixed.source_root_id;

INSERT INTO public.care_item_templates
SELECT (jsonb_populate_record(NULL::public.care_item_templates,
           to_jsonb(root) || jsonb_build_object(
               'template_id', mixed.baby_root_id,
               'template_lineage_id', mixed.baby_root_id,
               'template_version_id', mixed.baby_root_id,
               'title', root.title || ' - Baby',
               'eligibility_anchor_type', 'BIRTH_DATE',
               'schedule_context_type', 'BABY',
               'content_status', 'DRAFT', 'distribution_enabled', false,
               'approved_at', NULL, 'approved_by', NULL,
               'migration_review_required', false,
               'migration_reviewed_at', NULL, 'migration_reviewed_by', NULL,
               'created_at', clock_timestamp(), 'updated_at', clock_timestamp()
           ))).*
  FROM checklist_postpartum_mixed_roots mixed
  JOIN public.care_item_templates root ON root.template_id = mixed.source_root_id;

INSERT INTO public.care_item_templates
SELECT (jsonb_populate_record(NULL::public.care_item_templates,
           to_jsonb(item) || jsonb_build_object(
               'template_id', public.checklist_p2_deterministic_uuid(
                   'POSTPARTUM_SPLIT|MOTHER|ITEM|' || item.template_id),
               'parent_template_id', mixed.mother_root_id,
               'created_at', clock_timestamp(), 'updated_at', clock_timestamp()
           ))).*
  FROM checklist_postpartum_mixed_roots mixed
  JOIN public.care_item_templates item ON item.parent_template_id = mixed.source_root_id
 WHERE item.entry_type = 'CHECKLIST_ENTRY'
   AND ((coalesce(item.checklist_contract_version, 1) = 1 AND item.target_subject = 'MOTHER')
     OR (item.checklist_contract_version = 2 AND item.target_subject IS NULL
         AND item.due_anchor_type = 'DELIVERY_DATE'));

INSERT INTO public.care_item_templates
SELECT (jsonb_populate_record(NULL::public.care_item_templates,
           to_jsonb(item) || jsonb_build_object(
               'template_id', public.checklist_p2_deterministic_uuid(
                   'POSTPARTUM_SPLIT|BABY|ITEM|' || item.template_id),
               'parent_template_id', mixed.baby_root_id,
               'created_at', clock_timestamp(), 'updated_at', clock_timestamp()
           ))).*
 FROM checklist_postpartum_mixed_roots mixed
 JOIN public.care_item_templates item ON item.parent_template_id = mixed.source_root_id
 WHERE item.entry_type = 'CHECKLIST_ENTRY'
   AND ((coalesce(item.checklist_contract_version, 1) = 1 AND item.target_subject = 'BABY')
     OR (item.checklist_contract_version = 2 AND item.target_subject IS NULL
         AND item.due_anchor_type = 'BIRTH_DATE'));

UPDATE public.care_item_templates replacement
   SET content_status = 'APPROVED', distribution_enabled = true,
       approved_at = mixed.source_approved_at,
       approved_by = mixed.source_approved_by,
       updated_at = clock_timestamp()
  FROM checklist_postpartum_mixed_roots mixed
 WHERE replacement.template_id IN (mixed.mother_root_id, mixed.baby_root_id);

UPDATE public.care_item_templates source
   SET content_status = 'ARCHIVED', distribution_enabled = false,
       updated_at = clock_timestamp()
  FROM checklist_postpartum_mixed_roots mixed
 WHERE source.template_id = mixed.source_root_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM checklist_postpartum_mixed_roots mixed
        JOIN public.care_item_templates source ON source.template_id = mixed.source_root_id
        WHERE source.content_status <> 'ARCHIVED' OR source.distribution_enabled) THEN
        RAISE EXCEPTION 'POSTPARTUM_SPLIT_SOURCE_STILL_DISTRIBUTABLE';
    END IF;
    IF EXISTS (
        SELECT 1 FROM checklist_postpartum_mixed_roots mixed
        CROSS JOIN LATERAL (VALUES
            (mixed.mother_root_id, 'DELIVERY_DATE'::text, 'MOTHER'::text),
            (mixed.baby_root_id, 'BIRTH_DATE'::text, 'BABY'::text)
        ) expected(root_id, anchor_type, target_subject)
        LEFT JOIN public.care_item_templates root ON root.template_id = expected.root_id
        WHERE root.template_id IS NULL OR root.content_status <> 'APPROVED'
           OR NOT root.distribution_enabled
           OR root.eligibility_anchor_type <> expected.anchor_type
           OR NOT EXISTS (
               SELECT 1 FROM public.care_item_templates item
                WHERE item.parent_template_id = expected.root_id
                  AND item.entry_type = 'CHECKLIST_ENTRY' AND item.is_active)
           OR EXISTS (
               SELECT 1 FROM public.care_item_templates item
                WHERE item.parent_template_id = expected.root_id
                  AND item.entry_type = 'CHECKLIST_ENTRY'
                  AND NOT (
                      (coalesce(item.checklist_contract_version, 1) = 1
                       AND item.target_subject = expected.target_subject
                       AND (item.due_anchor_type IS NULL
                            OR item.due_anchor_type = expected.anchor_type))
                      OR (item.checklist_contract_version = 2
                          AND item.target_subject IS NULL
                          AND item.due_anchor_type = expected.anchor_type)))) THEN
        RAISE EXCEPTION 'POSTPARTUM_SPLIT_INVARIANT_FAILED';
    END IF;
    IF EXISTS (
        SELECT 1
          FROM checklist_postpartum_mixed_roots mixed
         WHERE (SELECT count(*) FROM public.care_item_templates source_item
                 WHERE source_item.parent_template_id = mixed.source_root_id
                   AND source_item.entry_type = 'CHECKLIST_ENTRY')
             <> (SELECT count(*) FROM public.care_item_templates cloned_item
                 WHERE cloned_item.parent_template_id IN (mixed.mother_root_id, mixed.baby_root_id)
                   AND cloned_item.entry_type = 'CHECKLIST_ENTRY')) THEN
        RAISE EXCEPTION 'POSTPARTUM_SPLIT_SOURCE_CLONE_COUNT_MISMATCH';
    END IF;
END $$;

-- Keep the target/anchor contract enforced for direct SQL writers as well as
-- the Java approval service. Historical archived/inactive rows are preserved;
-- only active POSTPARTUM leaves are rejected.
CREATE OR REPLACE FUNCTION public.checklist_validate_postpartum_leaf_timing()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_stage varchar(30);
        parent_anchor varchar(30);
BEGIN
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    IF NEW.entry_type = 'CHECKLIST_ENTRY' THEN
        SELECT stage, eligibility_anchor_type INTO parent_stage, parent_anchor
          FROM public.care_item_templates
         WHERE template_id = NEW.parent_template_id;
        IF NEW.is_active AND COALESCE(parent_stage, NEW.stage) = 'POSTPARTUM'
           AND NOT (
               (coalesce(NEW.checklist_contract_version, 1) = 1
                AND ((parent_anchor = 'DELIVERY_DATE' AND NEW.target_subject = 'MOTHER')
                  OR (parent_anchor = 'BIRTH_DATE' AND NEW.target_subject = 'BABY'))
                AND (NEW.due_anchor_type IS NULL OR NEW.due_anchor_type = parent_anchor))
               OR (NEW.checklist_contract_version = 2
                   AND NEW.target_subject IS NULL
                   AND NEW.due_anchor_type = parent_anchor)) THEN
            RAISE EXCEPTION 'POSTPARTUM_LEAF_TARGET_DUE_ANCHOR_MISMATCH';
        END IF;
    ELSIF NEW.entry_type = 'TEMPLATE_ROOT' AND NEW.stage = 'POSTPARTUM'
       AND EXISTS (
           SELECT 1 FROM public.care_item_templates item
            WHERE item.parent_template_id = NEW.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY' AND item.is_active
              AND NOT (
                  (coalesce(item.checklist_contract_version, 1) = 1
                   AND ((NEW.eligibility_anchor_type = 'DELIVERY_DATE'
                         AND item.target_subject = 'MOTHER')
                     OR (NEW.eligibility_anchor_type = 'BIRTH_DATE'
                         AND item.target_subject = 'BABY'))
                   AND (item.due_anchor_type IS NULL
                        OR item.due_anchor_type = NEW.eligibility_anchor_type))
                  OR (item.checklist_contract_version = 2
                      AND item.target_subject IS NULL
                      AND item.due_anchor_type = NEW.eligibility_anchor_type)))
    THEN
        RAISE EXCEPTION 'POSTPARTUM_LEAF_TARGET_DUE_ANCHOR_MISMATCH';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS checklist_validate_postpartum_leaf_timing_trg ON public.care_item_templates;
CREATE TRIGGER checklist_validate_postpartum_leaf_timing_trg
BEFORE INSERT OR UPDATE OF entry_type, stage, content_status, distribution_enabled,
    eligibility_anchor_type, is_active, target_subject, due_anchor_type, parent_template_id
ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_postpartum_leaf_timing();
