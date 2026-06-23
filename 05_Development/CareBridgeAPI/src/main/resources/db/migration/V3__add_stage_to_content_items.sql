-- V3: Add stage column to content_items for UC-105 Create Content/FAQ/Checklist
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS stage VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_content_items_stage ON content_items (stage);
CREATE INDEX IF NOT EXISTS idx_content_items_status ON content_items (status);
CREATE INDEX IF NOT EXISTS idx_content_items_type ON content_items (content_type);
