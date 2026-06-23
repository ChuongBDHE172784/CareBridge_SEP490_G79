-- V4: Create user_sessions table for active session management (Story 1-4)
-- Created: 2026-06-23

CREATE TABLE IF NOT EXISTS user_sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    refresh_token_hash VARCHAR(255),
    device_name VARCHAR(150),
    browser VARCHAR(150),
    ip_address VARCHAR(64),
    location VARCHAR(200),
    last_activity_at TIMESTAMP,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_last_activity ON user_sessions(last_activity_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_token ON user_sessions(refresh_token_hash);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_revoked ON user_sessions(user_id, revoked);

-- Foreign key constraint (optional, requires users table to exist)
-- ALTER TABLE user_sessions ADD CONSTRAINT fk_user_sessions_user
--   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
