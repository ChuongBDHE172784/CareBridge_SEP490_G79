-- =============================================================================
-- V3__add_user_checklist_items.sql
-- Purpose: Add user_checklist_items table to support UC50 Manage Preparation
--          Checklist (SRS 3.3.1.27).
-- Gap identified in: 04_Implement/UC50_ManagePreparationChecklist/UC50_ManagePreparationChecklist_TDS.md (CB-CHECKLIST-IMP-001)
-- Decision: ADR-001 — separate table from admin-managed checklist_templates/checklist_items.
-- Applied: additive only — no existing tables modified.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- UC50 — Manage Preparation Checklist
-- Personal checklist tracking per Mother user.
-- Supports:
--   - Custom items (template_item_id IS NULL)
--   - Template-imported items (template_item_id → checklist_items.checklist_item_id)
-- ---------------------------------------------------------------------------

CREATE TABLE public.user_checklist_items (
    user_checklist_item_id  uuid         NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id           uuid         NOT NULL,
    journey_id              uuid,
    baby_id                 uuid,
    template_item_id        uuid,
    item_text               varchar(500) NOT NULL,
    category                varchar(50)  NOT NULL DEFAULT 'GENERAL',
    is_completed            boolean      NOT NULL DEFAULT false,
    completed_at            timestamptz,
    item_order              integer      NOT NULL DEFAULT 0,
    created_at              timestamptz  NOT NULL DEFAULT now(),
    updated_at              timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT user_checklist_items_pkey
        PRIMARY KEY (user_checklist_item_id),
    CONSTRAINT fk_user_checklist_items_user
        FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id),
    CONSTRAINT fk_user_checklist_items_journey
        FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id),
    CONSTRAINT fk_user_checklist_items_baby
        FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id),
    CONSTRAINT fk_user_checklist_items_template
        FOREIGN KEY (template_item_id) REFERENCES public.checklist_items(checklist_item_id),
    CONSTRAINT chk_user_checklist_items_category
        CHECK (category IN ('DELIVERY', 'PAPERWORK', 'BABY_CARE', 'GENERAL'))
);

COMMENT ON TABLE public.user_checklist_items IS
    'Personal preparation checklist items per Mother user (UC50). '
    'Custom items have template_item_id = NULL. '
    'Template-imported items reference checklist_items.checklist_item_id.';

COMMENT ON COLUMN public.user_checklist_items.template_item_id IS
    'NULL = custom item entered by user. Non-null = imported from admin checklist_items template.';

COMMENT ON COLUMN public.user_checklist_items.completed_at IS
    'Set to NOW() when is_completed flips to true; reset to NULL when toggled back to false.';

-- Index for listing a user''s own checklist items
CREATE INDEX idx_user_checklist_items_owner_user_id
    ON public.user_checklist_items(owner_user_id);

-- Index for filtering by journey (pregnancy-stage checklist)
CREATE INDEX idx_user_checklist_items_owner_journey
    ON public.user_checklist_items(owner_user_id, journey_id);

-- Index for filtering by baby (baby-care checklist)
CREATE INDEX idx_user_checklist_items_owner_baby
    ON public.user_checklist_items(owner_user_id, baby_id);
