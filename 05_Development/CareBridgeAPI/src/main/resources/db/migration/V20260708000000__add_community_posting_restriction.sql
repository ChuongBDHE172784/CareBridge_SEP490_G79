ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS community_posting_restricted_until timestamptz NULL;

COMMENT ON COLUMN public.users.community_posting_restricted_until IS
    'Until this time, the user may read community content but may not create questions or answers.';
