-- V10: Alter partner_organizations representative_user_id to UUID to align with users table
TRUNCATE TABLE partner_organizations CASCADE;

ALTER TABLE partner_organizations ALTER COLUMN representative_user_id TYPE UUID USING NULL;
