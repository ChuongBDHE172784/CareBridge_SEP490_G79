-- The users.phone column is the canonical phone number for a user.
-- UserProfile now maps to that same column, so the duplicate phone_number
-- column can be removed without losing profile data.
ALTER TABLE public.users
    DROP COLUMN IF EXISTS phone_number;
