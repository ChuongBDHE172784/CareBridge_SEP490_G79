-- ============================================================================
-- Migration V10: Phân công thẩm định nội dung cho Chuyên gia Hợp đồng (Contracted Expert)
-- ============================================================================

ALTER TABLE public."content_items"
    ADD COLUMN IF NOT EXISTS "assigned_expert_id" UUID REFERENCES public."users"("user_id"),
    ADD COLUMN IF NOT EXISTS "assigned_at" TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS "approved_by" UUID REFERENCES public."users"("user_id"),
    ADD COLUMN IF NOT EXISTS "approved_at" TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS "idx_content_items_assigned_expert_status"
    ON public."content_items"("assigned_expert_id", "status");

ALTER TABLE public."care_item_templates"
    ADD COLUMN IF NOT EXISTS "assigned_expert_id" UUID REFERENCES public."users"("user_id"),
    ADD COLUMN IF NOT EXISTS "assigned_at" TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS "approved_by" UUID REFERENCES public."users"("user_id"),
    ADD COLUMN IF NOT EXISTS "approved_at" TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS "idx_care_templates_assigned_expert_status"
    ON public."care_item_templates"("assigned_expert_id", "content_status");
