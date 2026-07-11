ALTER TABLE public.users
    ALTER COLUMN role DROP DEFAULT,
    ALTER COLUMN role DROP NOT NULL;

COMMENT ON COLUMN public.users.role IS
    'Nullable until the user completes journey setup; CB-022 assigns the consumer role.';

