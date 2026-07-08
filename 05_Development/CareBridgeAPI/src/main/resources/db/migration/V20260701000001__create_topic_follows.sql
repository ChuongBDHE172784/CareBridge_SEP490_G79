-- UC-171: User follows community topics for personalized feed and future notifications
CREATE TABLE user_topic_follows (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    topic_id   UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_follow_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)      ON DELETE CASCADE,
    CONSTRAINT fk_follow_topic FOREIGN KEY (topic_id) REFERENCES community_topics(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_topic_follow UNIQUE (user_id, topic_id)
);

CREATE INDEX idx_user_topic_follows_user  ON user_topic_follows(user_id);
CREATE INDEX idx_user_topic_follows_topic ON user_topic_follows(topic_id);
