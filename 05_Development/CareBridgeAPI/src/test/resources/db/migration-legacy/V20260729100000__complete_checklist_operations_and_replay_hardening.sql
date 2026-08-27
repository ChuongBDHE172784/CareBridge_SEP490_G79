-- Make the reconciliation operations authority persistable.
ALTER TABLE public.users
    DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE public.users
    ADD CONSTRAINT users_role_check CHECK (
        role IS NULL OR role IN (
            'MOTHER', 'FAMILY', 'EXPERT', 'MODERATOR',
            'CONTENT_ADMIN', 'SYSTEM_ADMIN', 'OPERATIONS', 'PARTNER'
        )
    );
