-- Migration to expand invited_phone column in care_group_members to 255 characters
-- to support invitation by email address (which can exceed 20 characters)
ALTER TABLE care_group_members ALTER COLUMN invited_phone TYPE VARCHAR(255);
