-- === UC71 Invite Family Member — invite token support ===
-- Adds invite issuance columns to care_group_members. No existing column is altered/dropped.

ALTER TABLE care_group_members
    ADD COLUMN IF NOT EXISTS invite_token       VARCHAR(64),
    ADD COLUMN IF NOT EXISTS invite_channel     VARCHAR(20),
    ADD COLUMN IF NOT EXISTS invite_expires_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS invited_phone      VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uq_care_group_members_invite_token
    ON care_group_members (invite_token) WHERE invite_token IS NOT NULL;
