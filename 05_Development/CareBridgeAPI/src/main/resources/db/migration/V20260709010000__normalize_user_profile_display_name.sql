-- users.full_name is the single source of truth for the account display name.
-- Preserve the latest profile-edited value before removing the duplicate column.
UPDATE public.users AS u
SET full_name = btrim(up.display_name),
    updated_at = CURRENT_TIMESTAMP
FROM public.user_profiles AS up
WHERE up.user_id = u.user_id
  AND up.display_name IS NOT NULL
  AND btrim(up.display_name) <> '';

DROP INDEX IF EXISTS public.idx_user_profiles_display_name_search;

ALTER TABLE public.user_profiles
    DROP CONSTRAINT IF EXISTS chk_display_name_length;

ALTER TABLE public.user_profiles
    DROP COLUMN IF EXISTS display_name;
