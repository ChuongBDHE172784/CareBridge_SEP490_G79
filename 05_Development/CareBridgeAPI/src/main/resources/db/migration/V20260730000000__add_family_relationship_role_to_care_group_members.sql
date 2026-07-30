ALTER TABLE care_group_members
    ADD COLUMN IF NOT EXISTS family_relationship_role VARCHAR(50),
    ADD COLUMN IF NOT EXISTS custom_family_relationship_role VARCHAR(100);
