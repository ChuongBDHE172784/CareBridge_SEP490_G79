-- A care-group relink must revoke the previous checklist authority without
-- deleting it because historical checklist instances retain a restrictive FK.
UPDATE public.checklist_care_group_contexts mapping
SET review_status = 'BLOCKED',
    distribution_blocked = true,
    block_reason_code = 'CARE_GROUP_CONTEXT_RELINKED',
    updated_at = now()
FROM public.care_groups group_row
WHERE mapping.care_group_id = group_row.care_group_id
  AND mapping.owner_user_id = group_row.owner_user_id
  AND mapping.review_status = 'REVIEWED'
  AND mapping.distribution_blocked = false
  AND (
      (mapping.care_context_type = 'JOURNEY'
          AND mapping.care_context_id IS DISTINCT FROM group_row.linked_journey_id)
      OR
      (mapping.care_context_type = 'BABY'
          AND mapping.care_context_id IS DISTINCT FROM group_row.linked_baby_profile_id)
  );

CREATE UNIQUE INDEX checklist_care_group_contexts_single_active_type_ux
    ON public.checklist_care_group_contexts(care_group_id, care_context_type)
    WHERE review_status = 'REVIEWED' AND distribution_blocked = false;

CREATE OR REPLACE FUNCTION public.checklist_block_previous_care_group_context()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
BEGIN
    IF OLD.linked_journey_id IS DISTINCT FROM NEW.linked_journey_id
       AND OLD.linked_journey_id IS NOT NULL THEN
        UPDATE public.checklist_care_group_contexts
        SET review_status = 'BLOCKED',
            distribution_blocked = true,
            block_reason_code = 'CARE_GROUP_CONTEXT_RELINKED',
            updated_at = now()
        WHERE care_group_id = OLD.care_group_id
          AND owner_user_id = OLD.owner_user_id
          AND care_context_type = 'JOURNEY'
          AND care_context_id = OLD.linked_journey_id
          AND review_status <> 'BLOCKED';
    END IF;

    IF OLD.linked_baby_profile_id IS DISTINCT FROM NEW.linked_baby_profile_id
       AND OLD.linked_baby_profile_id IS NOT NULL THEN
        UPDATE public.checklist_care_group_contexts
        SET review_status = 'BLOCKED',
            distribution_blocked = true,
            block_reason_code = 'CARE_GROUP_CONTEXT_RELINKED',
            updated_at = now()
        WHERE care_group_id = OLD.care_group_id
          AND owner_user_id = OLD.owner_user_id
          AND care_context_type = 'BABY'
          AND care_context_id = OLD.linked_baby_profile_id
          AND review_status <> 'BLOCKED';
    END IF;
    RETURN NEW;
END $$;

REVOKE ALL ON FUNCTION public.checklist_block_previous_care_group_context()
    FROM PUBLIC;

CREATE TRIGGER checklist_block_previous_care_group_context_trg
BEFORE UPDATE OF linked_journey_id, linked_baby_profile_id
ON public.care_groups
FOR EACH ROW EXECUTE FUNCTION public.checklist_block_previous_care_group_context();

-- Reactivate a previously blocked mapping when a later relink returns to it.
CREATE OR REPLACE FUNCTION public.checklist_sync_reviewed_care_group_contexts()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    mismatch_reason varchar(80);
BEGIN
    IF NEW.linked_journey_id IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM public.checklist_context_authorities authority
            WHERE authority.care_context_type = 'JOURNEY'
              AND authority.care_context_id = NEW.linked_journey_id
              AND authority.owner_user_id = NEW.owner_user_id) THEN
            INSERT INTO public.checklist_care_group_contexts
                (care_group_id, owner_user_id, care_context_type, care_context_id,
                 review_status, distribution_blocked, block_reason_code,
                 reviewed_at, reviewed_by)
            VALUES (NEW.care_group_id, NEW.owner_user_id, 'JOURNEY', NEW.linked_journey_id,
                    'REVIEWED', false, NULL, now(), NEW.owner_user_id)
            ON CONFLICT (care_group_id, care_context_type, care_context_id) DO UPDATE
            SET owner_user_id = EXCLUDED.owner_user_id,
                review_status = 'REVIEWED',
                distribution_blocked = false,
                block_reason_code = NULL,
                reviewed_at = now(),
                reviewed_by = EXCLUDED.reviewed_by,
                updated_at = now();
        ELSE
            mismatch_reason := CASE WHEN EXISTS (
                SELECT 1 FROM public.checklist_context_authorities authority
                WHERE authority.care_context_type = 'JOURNEY'
                  AND authority.care_context_id = NEW.linked_journey_id)
                THEN 'CONTEXT_OWNER_MISMATCH' ELSE 'CONTEXT_NOT_FOUND' END;
            PERFORM public.checklist_quarantine_invalid_group_context(
                NEW.care_group_id, 'JOURNEY', NEW.linked_journey_id, mismatch_reason);
        END IF;
    END IF;

    IF NEW.linked_baby_profile_id IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM public.checklist_context_authorities authority
            WHERE authority.care_context_type = 'BABY'
              AND authority.care_context_id = NEW.linked_baby_profile_id
              AND authority.owner_user_id = NEW.owner_user_id) THEN
            INSERT INTO public.checklist_care_group_contexts
                (care_group_id, owner_user_id, care_context_type, care_context_id,
                 review_status, distribution_blocked, block_reason_code,
                 reviewed_at, reviewed_by)
            VALUES (NEW.care_group_id, NEW.owner_user_id, 'BABY', NEW.linked_baby_profile_id,
                    'REVIEWED', false, NULL, now(), NEW.owner_user_id)
            ON CONFLICT (care_group_id, care_context_type, care_context_id) DO UPDATE
            SET owner_user_id = EXCLUDED.owner_user_id,
                review_status = 'REVIEWED',
                distribution_blocked = false,
                block_reason_code = NULL,
                reviewed_at = now(),
                reviewed_by = EXCLUDED.reviewed_by,
                updated_at = now();
        ELSE
            mismatch_reason := CASE WHEN EXISTS (
                SELECT 1 FROM public.checklist_context_authorities authority
                WHERE authority.care_context_type = 'BABY'
                  AND authority.care_context_id = NEW.linked_baby_profile_id)
                THEN 'CONTEXT_OWNER_MISMATCH' ELSE 'CONTEXT_NOT_FOUND' END;
            PERFORM public.checklist_quarantine_invalid_group_context(
                NEW.care_group_id, 'BABY', NEW.linked_baby_profile_id, mismatch_reason);
        END IF;
    END IF;
    RETURN NEW;
END $$;

REVOKE ALL ON FUNCTION public.checklist_sync_reviewed_care_group_contexts()
    FROM PUBLIC;
