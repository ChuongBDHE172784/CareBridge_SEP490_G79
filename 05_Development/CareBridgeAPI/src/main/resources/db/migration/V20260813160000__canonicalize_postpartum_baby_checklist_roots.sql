-- Move Baby checklist versions created by the consolidated-stage split into
-- the explicit BABY_CARE stage. Existing instances/tasks remain pinned to
-- their original template snapshots; only future reconciliation sees clones.
SET lock_timeout = '30s';
LOCK TABLE public.care_item_templates IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE checklist_baby_stage_repair ON COMMIT DROP AS
SELECT root.template_id AS source_root_id,
       public.checklist_p2_deterministic_uuid(
           'POSTPARTUM_BABY_STAGE_REPAIR|ROOT|' || root.template_id) AS replacement_root_id,
       (root.template_id = '784bb631-1272-4f7c-9898-a01d8f1be93a'::uuid) AS known_pure,
       NOT EXISTS (
           SELECT 1 FROM public.care_item_templates item
            WHERE item.parent_template_id = root.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY' AND item.is_active
              AND NOT (
                  (coalesce(item.checklist_contract_version, 1) = 1
                   AND item.target_subject = 'BABY')
                  OR (item.checklist_contract_version = 2
                      AND item.target_subject IS NULL
                      AND item.due_anchor_type = 'BIRTH_DATE'))
       ) AS auto_approvable
  FROM public.care_item_templates root
 WHERE root.entry_type = 'TEMPLATE_ROOT'
   AND (
       (root.template_id = '784bb631-1272-4f7c-9898-a01d8f1be93a'::uuid
        AND root.stage IN ('POSTPARTUM','BABY_CARE'))
       OR (root.stage = 'POSTPARTUM'
           AND root.schedule_context_type = 'BABY'
           AND root.eligibility_anchor_type = 'BIRTH_DATE'
           AND root.template_id <> '784bb631-1272-4f7c-9898-a01d8f1be93a'::uuid)
   )
   AND NOT EXISTS (
       SELECT 1 FROM public.care_item_templates existing
        WHERE existing.template_id = public.checklist_p2_deterministic_uuid(
            'POSTPARTUM_BABY_STAGE_REPAIR|ROOT|' || root.template_id));

-- The known deployed root is repaired only when its active leaves are
-- classifiable. Ambiguous roots remain disabled and are not guessed.
INSERT INTO public.care_item_templates
SELECT (jsonb_populate_record(NULL::public.care_item_templates,
           to_jsonb(source) || jsonb_build_object(
               'template_id', repair.replacement_root_id,
               'template_lineage_id', repair.replacement_root_id,
               'template_version_id', repair.replacement_root_id,
               'title', CASE WHEN repair.known_pure THEN source.title || ' - Baby'
                             ELSE source.title || ' - Baby Care' END,
               'stage', 'BABY_CARE',
               'eligibility_anchor_type', 'BIRTH_DATE',
               'schedule_context_type', 'BABY',
               'content_status', 'DRAFT',
               'distribution_enabled', false,
               'migration_review_required', NOT repair.auto_approvable,
               'migration_reviewed_at', NULL,
               'migration_reviewed_by', NULL,
               'approved_at', NULL,
               'approved_by', NULL,
               'created_at', clock_timestamp(), 'updated_at', clock_timestamp()
           ))).*
  FROM checklist_baby_stage_repair repair
  JOIN public.care_item_templates source ON source.template_id = repair.source_root_id;

INSERT INTO public.care_item_templates
SELECT (jsonb_populate_record(NULL::public.care_item_templates,
           to_jsonb(item) || jsonb_build_object(
               'template_id', public.checklist_p2_deterministic_uuid(
                   'POSTPARTUM_BABY_STAGE_REPAIR|ITEM|' || item.template_id),
               'parent_template_id', repair.replacement_root_id,
               'stage', 'BABY_CARE',
               'target_subject', CASE WHEN item.checklist_contract_version = 2
                                      THEN NULL ELSE 'BABY' END,
               'due_anchor_type', 'BIRTH_DATE',
               'created_at', clock_timestamp(), 'updated_at', clock_timestamp()
           ))).*
  FROM checklist_baby_stage_repair repair
  JOIN public.care_item_templates item ON item.parent_template_id = repair.source_root_id
 WHERE item.entry_type = 'CHECKLIST_ENTRY';

-- Older live schemas may still retain the recipient authority table. Clone
-- authority before promotion so its validation trigger sees a complete version.
DO $$
BEGIN
    IF to_regclass('public.checklist_template_recipient_roles') IS NOT NULL THEN
        EXECUTE $sql$
            INSERT INTO public.checklist_template_recipient_roles
                (template_version_id, recipient_role, created_at)
            SELECT repair.replacement_root_id, role.recipient_role, clock_timestamp()
              FROM checklist_baby_stage_repair repair
              JOIN public.care_item_templates source
                ON source.template_id = repair.source_root_id
              JOIN public.checklist_template_recipient_roles role
                ON role.template_version_id = source.template_version_id
            ON CONFLICT (template_version_id, recipient_role) DO NOTHING
        $sql$;
    END IF;
END $$;

-- Promote only when the source carries a usable approval identity. Otherwise
-- keep the replacement DRAFT/disabled for explicit operator review.
UPDATE public.care_item_templates replacement
   SET migration_reviewed_at = clock_timestamp(),
       migration_reviewed_by = COALESCE(source.approved_by, source.author_user_id, source.created_by),
       content_status = 'PENDING_REVIEW',
       migration_review_required = false,
       updated_at = clock_timestamp()
  FROM checklist_baby_stage_repair repair
  JOIN public.care_item_templates source ON source.template_id = repair.source_root_id
 WHERE replacement.template_id = repair.replacement_root_id
   AND repair.auto_approvable
   AND COALESCE(source.approved_by, source.author_user_id, source.created_by) IS NOT NULL;

UPDATE public.care_item_templates replacement
   SET content_status = 'APPROVED', distribution_enabled = true,
       approved_at = COALESCE(source.approved_at, clock_timestamp()),
       approved_by = COALESCE(source.approved_by, source.author_user_id, source.created_by),
       updated_at = clock_timestamp()
  FROM checklist_baby_stage_repair repair
  JOIN public.care_item_templates source ON source.template_id = repair.source_root_id
 WHERE replacement.template_id = repair.replacement_root_id
   AND repair.auto_approvable
   AND replacement.content_status = 'PENDING_REVIEW'
   AND replacement.migration_review_required = false
   AND replacement.migration_reviewed_at IS NOT NULL
   AND replacement.migration_reviewed_by IS NOT NULL;

-- Replace the legacy consolidated-stage guard before archiving an invalid
-- source. Canonical distributable roots remain strict, while archived source
-- versions may retain their immutable legacy shape for History reads.
CREATE OR REPLACE FUNCTION public.checklist_validate_postpartum_leaf_timing()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_stage varchar(30); parent_anchor varchar(30);
BEGIN
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    IF NEW.entry_type = 'CHECKLIST_ENTRY' THEN
        SELECT stage, eligibility_anchor_type INTO parent_stage, parent_anchor
          FROM public.care_item_templates WHERE template_id = NEW.parent_template_id;
        IF NEW.is_active AND parent_stage = 'POSTPARTUM' AND NOT (
               (NEW.checklist_contract_version = 2
                AND NEW.target_subject IS NULL AND NEW.due_anchor_type = 'DELIVERY_DATE')
            OR (coalesce(NEW.checklist_contract_version, 1) = 1
                AND NEW.target_subject = 'MOTHER'
                AND (NEW.due_anchor_type IS NULL OR NEW.due_anchor_type = 'DELIVERY_DATE')))
        THEN RAISE EXCEPTION 'POSTPARTUM_LEAF_TARGET_DUE_ANCHOR_MISMATCH'; END IF;
        IF NEW.is_active AND parent_stage = 'BABY_CARE' AND NOT (
               (NEW.checklist_contract_version = 2
                AND NEW.target_subject IS NULL AND NEW.due_anchor_type = 'BIRTH_DATE')
            OR (coalesce(NEW.checklist_contract_version, 1) = 1
                AND NEW.target_subject = 'BABY'
                AND (NEW.due_anchor_type IS NULL OR NEW.due_anchor_type = 'BIRTH_DATE')))
        THEN RAISE EXCEPTION 'BABY_CARE_LEAF_TARGET_DUE_ANCHOR_MISMATCH'; END IF;
    ELSIF NEW.entry_type = 'TEMPLATE_ROOT' THEN
        IF NEW.content_status <> 'ARCHIVED' AND NEW.distribution_enabled
           AND ((NEW.stage = 'POSTPARTUM'
                 AND NEW.eligibility_anchor_type <> 'DELIVERY_DATE')
             OR (NEW.stage = 'BABY_CARE'
                 AND NEW.eligibility_anchor_type <> 'BIRTH_DATE')) THEN
            RAISE EXCEPTION 'CHECKLIST_STAGE_ROOT_ANCHOR_MISMATCH';
        END IF;
        IF NEW.content_status <> 'ARCHIVED' AND NEW.distribution_enabled
           AND EXISTS (
           SELECT 1 FROM public.care_item_templates item
            WHERE item.parent_template_id = NEW.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY' AND item.is_active
              AND NOT (
                  (NEW.stage = 'POSTPARTUM'
                   AND NEW.eligibility_anchor_type = 'DELIVERY_DATE'
                   AND ((item.checklist_contract_version = 2
                         AND item.target_subject IS NULL
                         AND item.due_anchor_type = 'DELIVERY_DATE')
                     OR (coalesce(item.checklist_contract_version, 1) = 1
                         AND item.target_subject = 'MOTHER'
                         AND (item.due_anchor_type IS NULL
                              OR item.due_anchor_type = 'DELIVERY_DATE'))))
                  OR (NEW.stage = 'BABY_CARE'
                      AND NEW.eligibility_anchor_type = 'BIRTH_DATE'
                      AND ((item.checklist_contract_version = 2
                            AND item.target_subject IS NULL
                            AND item.due_anchor_type = 'BIRTH_DATE')
                        OR (coalesce(item.checklist_contract_version, 1) = 1
                            AND item.target_subject = 'BABY'
                            AND (item.due_anchor_type IS NULL
                                 OR item.due_anchor_type = 'BIRTH_DATE')))))) THEN
            RAISE EXCEPTION 'CHECKLIST_STAGE_LEAF_CONTRACT_MISMATCH';
        END IF;
    END IF;
    RETURN NEW;
END $$;

-- Archive only the old Baby root after its replacement is distributable. This
-- lifecycle-only update preserves the source identity and all pinned tasks.
UPDATE public.care_item_templates source
   SET content_status = 'ARCHIVED', distribution_enabled = false,
       updated_at = clock_timestamp()
  FROM checklist_baby_stage_repair repair
  JOIN public.care_item_templates replacement
    ON replacement.template_id = repair.replacement_root_id
 WHERE source.template_id = repair.source_root_id
   AND replacement.content_status = 'APPROVED'
   AND replacement.distribution_enabled;

-- A known root whose surviving leaves no longer prove Baby ownership must not
-- remain distributable. Preserve it as archived history and leave its cloned
-- replacement disabled for explicit review.
UPDATE public.care_item_templates source
   SET content_status = 'ARCHIVED', distribution_enabled = false,
       updated_at = clock_timestamp()
  FROM checklist_baby_stage_repair repair
 WHERE source.template_id = repair.source_root_id
   AND repair.known_pure
   AND NOT repair.auto_approvable;

DROP TRIGGER IF EXISTS checklist_validate_postpartum_leaf_timing_trg ON public.care_item_templates;
CREATE TRIGGER checklist_validate_postpartum_leaf_timing_trg
BEFORE INSERT OR UPDATE OF entry_type, stage, content_status, distribution_enabled,
    eligibility_anchor_type, is_active, target_subject, due_anchor_type, parent_template_id
ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_postpartum_leaf_timing();
