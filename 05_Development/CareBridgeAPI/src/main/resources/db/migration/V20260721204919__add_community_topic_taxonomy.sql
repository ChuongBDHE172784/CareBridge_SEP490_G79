-- === Community Topic taxonomy: type / slug / parent_id (real, non-mock replacement for icon-hack) ===
-- See 04_Implement/CommunityTopicManagement/CommunityTopicManagement_TDS.md §5.2 (ADR-COM-016, ADR-COM-018)

ALTER TABLE community_topics
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'TOPIC',   -- TOPIC | CATEGORY | TAG
    ADD COLUMN slug VARCHAR(140),                            -- filled below, then NOT NULL
    ADD COLUMN parent_id UUID;                                -- self-FK, required for CATEGORY/TAG

ALTER TABLE community_topics
    ADD CONSTRAINT community_topics_type_check
        CHECK (type IN ('TOPIC', 'CATEGORY', 'TAG')),
    -- ADR-COM-016 (revised): a TOPIC can never have a parent. CATEGORY/TAG MAY optionally have a
    -- parent (parent_id is nullable for them too) — kept optional so the pre-existing
    -- ContentCategoryController (/api/v1/admin/content/categories, standalone flat categories,
    -- no parent concept) keeps working unchanged. IF a parent is set, it must be a TOPIC — that
    -- part of the rule needs a cross-row type check and is enforced in CommunityTopicServiceImpl,
    -- not here.
    ADD CONSTRAINT community_topics_parent_rule_check
        CHECK (type <> 'TOPIC' OR parent_id IS NULL),
    ADD CONSTRAINT fk_community_topics_parent
        FOREIGN KEY (parent_id) REFERENCES community_topics(id) ON DELETE RESTRICT;

-- Deterministic slug backfill for the 8 known seed topics (V10__seed_community_topics.sql) —
-- computed with the same NFD-strip algorithm SlugGenerator.java uses, not guessed.
UPDATE community_topics SET slug = 'dinh-duong-thai-ky'   WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567801' AND slug IS NULL;
UPDATE community_topics SET slug = 'suc-khoe-thai-nhi'    WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567802' AND slug IS NULL;
UPDATE community_topics SET slug = 'cham-soc-sau-sinh'    WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567803' AND slug IS NULL;
UPDATE community_topics SET slug = 'nuoi-con-bang-sua-me' WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567804' AND slug IS NULL;
UPDATE community_topics SET slug = 'giac-ngu-va-the-chat' WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567805' AND slug IS NULL;
UPDATE community_topics SET slug = 'tam-ly-va-cam-xuc'    WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567806' AND slug IS NULL;
UPDATE community_topics SET slug = 'cham-soc-be-so-sinh'  WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567807' AND slug IS NULL;
UPDATE community_topics SET slug = 'hoi-dap-chung'        WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567808' AND slug IS NULL;

-- Fallback for any other pre-existing row created outside the seed (e.g. moderator-created before
-- this migration ran): deterministic id-based placeholder; recomputed to a real slug automatically
-- the next time the topic is renamed via the app (ADR-COM-018).
UPDATE community_topics SET slug = 'topic-' || substr(id::text, 1, 8) WHERE slug IS NULL;

ALTER TABLE community_topics
    ALTER COLUMN slug SET NOT NULL,
    ADD CONSTRAINT community_topics_slug_unique UNIQUE (slug);

CREATE INDEX idx_community_topics_parent_id ON community_topics(parent_id);
CREATE INDEX idx_community_topics_type ON community_topics(type);
