-- Amendment 2 (ADR-COM-020): CATEGORY roots contain TOPIC children; TAG stays flat.
-- Existing topic, question, and follow identifiers are never rewritten or deleted.

-- The applied v1 constraint enforces the opposite direction, so it must be removed first.
ALTER TABLE community_topics
    DROP CONSTRAINT IF EXISTS community_topics_parent_rule_check;

-- Normalize legacy CATEGORY/TAG rows before applying the inverted rule.
UPDATE community_topics
SET parent_id = NULL
WHERE type IN ('CATEGORY', 'TAG')
  AND parent_id IS NOT NULL;

-- The approved top-level categories use deterministic IDs so every environment receives the
-- same taxonomy metadata while the eight existing TOPIC IDs remain untouched.
INSERT INTO community_topics
    (id, name, description, icon, type, slug, parent_id, is_hidden, sort_order,
     created_by, created_at, updated_at)
VALUES
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567801', 'Chuẩn bị mang thai',
     'Chuẩn bị sức khoẻ, tâm lý trước khi mang thai', 'favorite', 'CATEGORY',
     'chuan-bi-mang-thai', NULL, false, 1, NULL, NOW(), NOW()),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567802', 'Mang thai',
     'Chăm sóc và theo dõi trong thai kỳ', 'pregnant_woman', 'CATEGORY',
     'mang-thai', NULL, false, 2, NULL, NOW(), NOW()),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567803', 'Sau sinh',
     'Hồi phục và chăm sóc sau khi sinh', 'healing', 'CATEGORY',
     'sau-sinh', NULL, false, 3, NULL, NOW(), NOW()),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567804', 'Chăm bé',
     'Chăm sóc và nuôi dạy bé sơ sinh', 'child_care', 'CATEGORY',
     'cham-be', NULL, false, 4, NULL, NOW(), NOW()),
    ('b1b2c3d4-e5f6-7890-abcd-ef1234567805', 'Khác',
     'Các chủ đề khác không thuộc nhóm trên', 'more_horiz', 'CATEGORY',
     'khac', NULL, false, 5, NULL, NOW(), NOW());

-- Re-parent the eight original seed TOPIC rows without changing their IDs or dependent FKs.
UPDATE community_topics
SET parent_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567802'
WHERE id IN (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567801',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567802',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567805'
);

UPDATE community_topics
SET parent_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567803'
WHERE id IN (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567803',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567804'
);

UPDATE community_topics
SET parent_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567804'
WHERE id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567807';

UPDATE community_topics
SET parent_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567805'
WHERE id IN (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567806',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567808'
);

-- Any unclassified or legacy TOPIC falls back to "Khác". A non-null parent that is not a
-- CATEGORY is equally invalid and therefore treated as ambiguous.
UPDATE community_topics AS topic
SET parent_id = 'b1b2c3d4-e5f6-7890-abcd-ef1234567805'
WHERE topic.type = 'TOPIC'
  AND (
      topic.parent_id IS NULL
      OR NOT EXISTS (
          SELECT 1
          FROM community_topics AS parent
          WHERE parent.id = topic.parent_id
            AND parent.type = 'CATEGORY'
      )
  );

-- CHECK constraints cannot inspect another row, so this constraint enforces the nullability
-- half of the invariant and the trigger below enforces that a TOPIC parent is a CATEGORY.
ALTER TABLE community_topics
    ADD CONSTRAINT community_topics_parent_rule_check_v2
        CHECK (
            (type = 'CATEGORY' AND parent_id IS NULL)
            OR (type = 'TOPIC' AND parent_id IS NOT NULL)
            OR (type = 'TAG' AND parent_id IS NULL)
        );

CREATE OR REPLACE FUNCTION enforce_community_topic_parent_category()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.type = 'TOPIC'
       AND NOT EXISTS (
           SELECT 1
           FROM community_topics AS parent
           WHERE parent.id = NEW.parent_id
             AND parent.type = 'CATEGORY'
       ) THEN
        RAISE EXCEPTION 'TOPIC parent_id must reference a CATEGORY'
            USING ERRCODE = '23514', CONSTRAINT = 'community_topics_parent_category_check';
    END IF;

    IF TG_OP = 'UPDATE'
       AND OLD.type = 'CATEGORY'
       AND NEW.type <> 'CATEGORY'
       AND EXISTS (
           SELECT 1
           FROM community_topics AS child
           WHERE child.parent_id = NEW.id
       ) THEN
        RAISE EXCEPTION 'A referenced CATEGORY cannot change type'
            USING ERRCODE = '23514', CONSTRAINT = 'community_topics_parent_category_check';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_community_topic_parent_category
BEFORE INSERT OR UPDATE OF type, parent_id ON community_topics
FOR EACH ROW
EXECUTE FUNCTION enforce_community_topic_parent_category();
