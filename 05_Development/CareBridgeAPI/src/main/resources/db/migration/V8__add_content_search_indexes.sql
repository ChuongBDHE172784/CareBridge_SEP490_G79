-- V8: Add search indexes for UC-224 Search Verified Content
-- TDS: CB-CONTENT-IMP-002, ADR-003, ADR-004
-- Use CONCURRENTLY to avoid locking table on production

-- Index for keyword search on lowercase title (ILIKE pattern)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_content_items_title_search
    ON content_items USING btree (lower(title));

-- Composite index for common filter combinations (stage, type, status=APPROVED)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_content_items_stage_type_approved
    ON content_items (stage, content_type, status)
    WHERE status = 'APPROVED';

-- Index for published_at ORDER BY in search results
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_content_items_published_at
    ON content_items (published_at DESC NULLS LAST)
    WHERE status = 'APPROVED';
