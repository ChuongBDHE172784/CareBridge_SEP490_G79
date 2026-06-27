-- UC-70: CreateCareGroup — description + optional profile links
ALTER TABLE care_groups
    ADD COLUMN IF NOT EXISTS description            VARCHAR(500),
    ADD COLUMN IF NOT EXISTS linked_journey_id      UUID,
    ADD COLUMN IF NOT EXISTS linked_baby_profile_id UUID;
