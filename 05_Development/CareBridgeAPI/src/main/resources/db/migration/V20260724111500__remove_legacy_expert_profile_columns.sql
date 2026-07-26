-- Wave 3 moved these relationships to professional_profiles but retained the
-- former expert_profile_id columns. Their legacy NOT NULL constraints make
-- canonical JPA inserts fail even when professional_profile_id is populated.

ALTER TABLE public.expert_credentials
    DROP COLUMN IF EXISTS expert_profile_id;

ALTER TABLE public.expert_availability
    DROP COLUMN IF EXISTS expert_profile_id;

ALTER TABLE public.expert_location_shares
    DROP COLUMN IF EXISTS expert_profile_id;
