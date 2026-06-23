-- V5: Create community_questions table for UC-54 Create Community Question
-- TDS: CB-COMMUNITY-IMP-001, ADR-COM-001, ADR-COM-002, ADR-COM-003

CREATE TABLE community_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES community_topics(id),
    author_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    stage VARCHAR(30) NOT NULL CHECK (stage IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE')),
    pregnancy_week SMALLINT CHECK (pregnancy_week BETWEEN 1 AND 42),
    baby_age_months SMALLINT CHECK (baby_age_months BETWEEN 0 AND 72),
    urgency VARCHAR(20) NOT NULL CHECK (urgency IN ('LOW','NORMAL','URGENT')),
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','HIDDEN','LOCKED')),
    like_count INT NOT NULL DEFAULT 0,
    answer_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_community_questions_topic_id ON community_questions(topic_id);
CREATE INDEX idx_community_questions_author_id ON community_questions(author_id);
CREATE INDEX idx_community_questions_status ON community_questions(status);
CREATE INDEX idx_community_questions_stage ON community_questions(stage);
