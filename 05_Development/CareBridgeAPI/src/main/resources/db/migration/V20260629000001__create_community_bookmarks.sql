-- UC-58: Community bookmarks — per-user question bookmarking
CREATE TABLE community_bookmarks (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    question_id UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_bookmark_user     FOREIGN KEY (user_id)     REFERENCES users(user_id)         ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_question FOREIGN KEY (question_id) REFERENCES community_questions(id) ON DELETE CASCADE,
    CONSTRAINT uq_bookmark          UNIQUE (user_id, question_id)
);

CREATE INDEX idx_community_bookmarks_user     ON community_bookmarks(user_id);
CREATE INDEX idx_community_bookmarks_question ON community_bookmarks(question_id);
