-- UC-170 (Delete Community Post) + UC-201 (Delete Own Answer)
-- Adds a DELETED soft-delete status to community_questions and community_answers.
-- V1__init_schema.sql is not modified; this migration is authoritative for the new value.

ALTER TABLE community_questions
  DROP CONSTRAINT community_questions_status_check;

ALTER TABLE community_questions
  ADD CONSTRAINT community_questions_status_check
  CHECK (status IN ('PENDING', 'APPROVED', 'HIDDEN', 'LOCKED', 'DELETED'));

ALTER TABLE community_answers
  DROP CONSTRAINT community_answers_status_check;

ALTER TABLE community_answers
  ADD CONSTRAINT community_answers_status_check
  CHECK (status IN ('PENDING', 'APPROVED', 'HIDDEN', 'DELETED'));
