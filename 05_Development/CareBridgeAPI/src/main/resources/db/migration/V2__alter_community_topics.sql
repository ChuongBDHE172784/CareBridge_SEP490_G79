-- V2: Align community_topics with TDS CB-COMMUNITY-IMP-005 (UC-109)

-- 1. Rename primary key column
ALTER TABLE community_topics RENAME COLUMN topic_id TO id;

-- 2. Drop columns not in TDS
ALTER TABLE community_topics DROP COLUMN IF EXISTS is_active;
ALTER TABLE community_topics DROP COLUMN IF EXISTS slug;
ALTER TABLE community_topics DROP COLUMN IF EXISTS risk_level;

-- 3. Add is_hidden (replaces is_active, default visible)
ALTER TABLE community_topics ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- 4. Tighten name column: max 100, not null
ALTER TABLE community_topics ALTER COLUMN name TYPE VARCHAR(100);
ALTER TABLE community_topics ALTER COLUMN name SET NOT NULL;

-- 5. Case-insensitive unique index for name (BR-COM-015)
CREATE UNIQUE INDEX idx_community_topics_name_lower ON community_topics (LOWER(name));

-- 6. Change description to TEXT
ALTER TABLE community_topics ALTER COLUMN description TYPE TEXT USING description::text;

-- 7. Add new columns
ALTER TABLE community_topics ADD COLUMN IF NOT EXISTS icon VARCHAR(255);
ALTER TABLE community_topics ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE community_topics ADD COLUMN IF NOT EXISTS created_by BIGINT;

-- 8. Ensure updated_at has a default
ALTER TABLE community_topics ALTER COLUMN updated_at SET DEFAULT NOW();

-- 9. Indexes for query patterns
CREATE INDEX IF NOT EXISTS idx_community_topics_hidden ON community_topics (is_hidden);
CREATE INDEX IF NOT EXISTS idx_community_topics_sort_order ON community_topics (sort_order);
