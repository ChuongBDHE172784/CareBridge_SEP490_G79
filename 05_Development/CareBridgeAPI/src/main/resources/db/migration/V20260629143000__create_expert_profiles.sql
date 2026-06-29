-- V20260629143000__create_expert_profiles.sql
-- UC-87: Create Expert Profile
-- Creates expert_profiles table with status enum and consultation_modality enum

-- Drop types if they exist (for idempotent re-runs in dev)
DROP TYPE IF EXISTS expert_profile_status CASCADE;
DROP TYPE IF EXISTS consultation_modality CASCADE;

CREATE TYPE expert_profile_status AS ENUM (
    'DRAFT',
    'PENDING_VERIFICATION',
    'VERIFIED',
    'SUSPENDED'
);

CREATE TYPE consultation_modality AS ENUM ('CHAT', 'VOICE', 'VIDEO');

CREATE TABLE IF NOT EXISTS expert_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    bio TEXT,
    specialties TEXT[] NOT NULL DEFAULT '{}',
    years_of_experience INTEGER NOT NULL CHECK (years_of_experience >= 0),
    consultation_fee_vnd BIGINT NOT NULL CHECK (consultation_fee_vnd >= 0),
    consultation_modalities consultation_modality[] NOT NULL DEFAULT '{}',
    status expert_profile_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expert_profiles_user ON expert_profiles(user_id);
CREATE INDEX idx_expert_profiles_status ON expert_profiles(status);
