-- Preserve canonical checklist entry identity and make each import scope explicit.
ALTER TABLE public.care_item_templates
    ALTER COLUMN title TYPE varchar(500);

LOCK TABLE public.preparation_checklist_items IN SHARE ROW EXCLUSIVE MODE;

DROP INDEX IF EXISTS public.uq_preparation_checklist_import_scope;

-- Historical baby imports sometimes retained their mother's journey id. A baby is the
-- complete import scope, so clear that legacy discriminator before deduplicating.
UPDATE public.preparation_checklist_items
   SET mother_journey_id = NULL,
       updated_at = now()
 WHERE baby_id IS NOT NULL
   AND mother_journey_id IS NOT NULL
   AND template_entry_id IS NOT NULL;

WITH ranked_baby_imports AS (
    SELECT checklist_item_id,
           row_number() OVER (
               PARTITION BY owner_user_id, baby_id, template_entry_id
               ORDER BY (status = 'COMPLETED') DESC,
                        completed_at DESC NULLS LAST,
                        updated_at DESC,
                        created_at DESC,
                        checklist_item_id
           ) AS duplicate_rank
      FROM public.preparation_checklist_items
     WHERE baby_id IS NOT NULL
       AND template_entry_id IS NOT NULL
)
DELETE FROM public.preparation_checklist_items duplicate
 USING ranked_baby_imports ranked
 WHERE duplicate.checklist_item_id = ranked.checklist_item_id
   AND ranked.duplicate_rank > 1;

WITH ranked_journey_imports AS (
    SELECT checklist_item_id,
           row_number() OVER (
               PARTITION BY owner_user_id, mother_journey_id, template_entry_id
               ORDER BY (status = 'COMPLETED') DESC,
                        completed_at DESC NULLS LAST,
                        updated_at DESC,
                        created_at DESC,
                        checklist_item_id
           ) AS duplicate_rank
      FROM public.preparation_checklist_items
     WHERE baby_id IS NULL
       AND template_entry_id IS NOT NULL
)
DELETE FROM public.preparation_checklist_items duplicate
 USING ranked_journey_imports ranked
 WHERE duplicate.checklist_item_id = ranked.checklist_item_id
   AND ranked.duplicate_rank > 1;

CREATE UNIQUE INDEX uq_preparation_checklist_baby_import_scope
    ON public.preparation_checklist_items (owner_user_id, baby_id, template_entry_id)
    WHERE baby_id IS NOT NULL AND template_entry_id IS NOT NULL;

CREATE UNIQUE INDEX uq_preparation_checklist_journey_import_scope
    ON public.preparation_checklist_items (
        owner_user_id,
        mother_journey_id,
        template_entry_id
    ) NULLS NOT DISTINCT
    WHERE baby_id IS NULL AND template_entry_id IS NOT NULL;

-- All account-scoped settings writers assume an object root and preserve sibling keys.
ALTER TABLE public.users
    DROP CONSTRAINT IF EXISTS users_settings_jsonb_object_ck;

ALTER TABLE public.users
    ADD CONSTRAINT users_settings_jsonb_object_ck
    CHECK (jsonb_typeof(settings_jsonb) = 'object') NOT VALID;

ALTER TABLE public.users
    VALIDATE CONSTRAINT users_settings_jsonb_object_ck;

-- Claim ownership fences completion by workers that outlive the claim lease.
ALTER TABLE public.notification_records
    ADD COLUMN IF NOT EXISTS claim_token uuid;
