-- CB-MOD-IMP-015: revert-audit columns for content_reports.
-- reverted_at/reverted_by record the most recent revert-to-PENDING event; resolved_at/
-- assigned_moderator_id are deliberately left untouched by revert so the original resolution
-- is never lost (see TDS ADR-005).
ALTER TABLE content_reports
  ADD COLUMN reverted_at TIMESTAMPTZ NULL,
  ADD COLUMN reverted_by UUID NULL;
