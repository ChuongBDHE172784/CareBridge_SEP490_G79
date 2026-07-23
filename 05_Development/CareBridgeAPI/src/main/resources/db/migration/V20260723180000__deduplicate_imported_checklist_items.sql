-- Consolidate historical template imports before enforcing scope-level idempotency.
-- Custom checklist items are intentionally excluded.
WITH ranked_imports AS (
    SELECT
        user_checklist_item_id,
        ROW_NUMBER() OVER (
            PARTITION BY
                owner_user_id,
                journey_id,
                baby_id,
                template_item_id
            ORDER BY
                is_completed DESC,
                completed_at DESC NULLS LAST,
                updated_at DESC,
                created_at DESC,
                user_checklist_item_id
        ) AS duplicate_rank
    FROM public.user_checklist_items
    WHERE template_item_id IS NOT NULL
)
DELETE FROM public.user_checklist_items AS duplicate
USING ranked_imports
WHERE duplicate.user_checklist_item_id = ranked_imports.user_checklist_item_id
  AND ranked_imports.duplicate_rank > 1;

CREATE UNIQUE INDEX uq_user_checklist_import_scope
    ON public.user_checklist_items (
        owner_user_id,
        journey_id,
        baby_id,
        template_item_id
    )
    NULLS NOT DISTINCT
    WHERE template_item_id IS NOT NULL;
