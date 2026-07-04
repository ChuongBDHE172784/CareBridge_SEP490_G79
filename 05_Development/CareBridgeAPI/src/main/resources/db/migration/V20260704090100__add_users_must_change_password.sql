-- UC115 Create Staff Account — additive column.
-- Admin-provisioned staff accounts (MODERATOR/CONTENT_ADMIN/SYSTEM_ADMIN) are issued a
-- system-generated temporary password and must rotate it before their first authenticated
-- use. Self-registered accounts (OTP flow) are unaffected — default false.

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS must_change_password boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN public.users.must_change_password IS
    'UC115: true when an admin-issued temporary password has not yet been rotated by the staff member.';
