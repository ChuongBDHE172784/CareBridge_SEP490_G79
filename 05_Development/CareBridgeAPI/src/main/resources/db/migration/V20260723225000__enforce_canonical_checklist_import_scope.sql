-- Carry the scope-level idempotency guarantee from the legacy checklist table to
-- its approved canonical replacement.
WITH ranked_imports AS (
    SELECT
        checklist_item_id,
        ROW_NUMBER() OVER (
            PARTITION BY
                owner_user_id,
                mother_journey_id,
                baby_id,
                template_entry_id
            ORDER BY
                (status = 'COMPLETED') DESC,
                completed_at DESC NULLS LAST,
                updated_at DESC,
                created_at DESC,
                checklist_item_id
        ) AS duplicate_rank
    FROM public.preparation_checklist_items
    WHERE template_entry_id IS NOT NULL
)
DELETE FROM public.preparation_checklist_items AS duplicate
USING ranked_imports
WHERE duplicate.checklist_item_id = ranked_imports.checklist_item_id
  AND ranked_imports.duplicate_rank > 1;

CREATE UNIQUE INDEX uq_preparation_checklist_import_scope
    ON public.preparation_checklist_items (
        owner_user_id,
        mother_journey_id,
        baby_id,
        template_entry_id
    )
    NULLS NOT DISTINCT
    WHERE template_entry_id IS NOT NULL;

DO $remove_checklist_bridge$
DECLARE
    legacy_relation oid := to_regclass('public.user_checklist_items');
BEGIN
    IF legacy_relation IS NOT NULL
       AND obj_description(legacy_relation, 'pg_class') =
           'carebridge_transient_bootstrap_bridge_for_v20260723180000' THEN
        DROP TABLE public.user_checklist_items;
    END IF;
END
$remove_checklist_bridge$;
