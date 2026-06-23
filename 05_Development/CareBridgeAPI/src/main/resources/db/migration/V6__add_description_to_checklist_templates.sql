-- V6: Add description column to checklist_templates for UC-82 View Content and Checklist
-- TDS: CB-CONTENT-IMP-001, ADR-001, ADR-002

ALTER TABLE checklist_templates ADD COLUMN IF NOT EXISTS description TEXT;

CREATE INDEX IF NOT EXISTS idx_content_items_stage_status ON content_items (stage, status);
CREATE INDEX IF NOT EXISTS idx_content_items_type_status  ON content_items (content_type, status);
CREATE INDEX IF NOT EXISTS idx_checklist_templates_stage  ON checklist_templates (stage);
