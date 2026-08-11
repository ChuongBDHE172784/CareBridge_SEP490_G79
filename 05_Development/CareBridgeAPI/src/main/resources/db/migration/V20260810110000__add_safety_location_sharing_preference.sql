ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS safety_location_sharing_enabled boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN public.users.safety_location_sharing_enabled IS
    'Explicit mother opt-in to attach current location to fall emergency alerts.';
