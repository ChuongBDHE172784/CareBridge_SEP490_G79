-- V9: Alter community table user references to UUID to align with users table
-- Clear old test data to prevent cast failures and NOT NULL constraint violations
TRUNCATE TABLE community_answers CASCADE;
TRUNCATE TABLE community_questions CASCADE;

ALTER TABLE community_questions ALTER COLUMN author_id TYPE UUID USING NULL;
ALTER TABLE community_answers ALTER COLUMN author_id TYPE UUID USING NULL;
ALTER TABLE community_topics ALTER COLUMN created_by TYPE UUID USING NULL;
