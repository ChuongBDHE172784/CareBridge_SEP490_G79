-- UC-59: Community answer likes — per-user idempotent toggle
CREATE TABLE community_answer_likes (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID        NOT NULL,
    answer_id UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_like_user   FOREIGN KEY (user_id)   REFERENCES users(user_id)       ON DELETE CASCADE,
    CONSTRAINT fk_like_answer FOREIGN KEY (answer_id) REFERENCES community_answers(id) ON DELETE CASCADE,
    CONSTRAINT uq_answer_like UNIQUE (user_id, answer_id)
);

CREATE INDEX idx_community_answer_likes_answer ON community_answer_likes(answer_id);
CREATE INDEX idx_community_answer_likes_user   ON community_answer_likes(user_id);
