-- V7: Create community_answers table for UC-56 Post Community Answer
-- TDS: CB-COMMUNITY-IMP-002, ADR-COM-004, ADR-COM-005, ADR-COM-006

CREATE TABLE community_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES community_questions(id),
    author_id BIGINT NOT NULL,
    body TEXT NOT NULL,
    is_expert_labeled BOOLEAN NOT NULL DEFAULT FALSE,
    is_personal_experience BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','HIDDEN')),
    like_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_community_answers_question_id ON community_answers(question_id);
CREATE INDEX idx_community_answers_author_id ON community_answers(author_id);
CREATE INDEX idx_community_answers_status ON community_answers(status);
