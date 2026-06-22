-- =====================================================
-- TV4: Expert Consultation Domain Tables
-- Created: 2026-06-21
-- Sprint 0 - STORY-401 through STORY-411
-- =====================================================

-- Expert Profiles
CREATE TABLE IF NOT EXISTS expert_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    bio TEXT,
    expertise_areas TEXT[] NOT NULL,
    years_experience INTEGER CHECK (years_experience >= 0),
    qualifications TEXT,
    hourly_rate DECIMAL(10,2) CHECK (hourly_rate >= 0),
    avg_rating DECIMAL(3,2) DEFAULT 0.0,
    total_reviews INTEGER DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expert_profiles_user_id ON expert_profiles(user_id);
CREATE INDEX idx_expert_profiles_is_verified ON expert_profiles(is_verified);
CREATE INDEX idx_expert_profiles_is_available ON expert_profiles(is_available);

-- Expert Credentials (Verification Documents)
CREATE TABLE IF NOT EXISTS expert_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expert_profile_id UUID NOT NULL REFERENCES expert_profiles(id) ON DELETE CASCADE,
    credential_type VARCHAR(100) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    issue_date DATE,
    expiry_date DATE,
    issuing_authority VARCHAR(255),
    verification_status VARCHAR(50) DEFAULT 'PENDING',
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMP,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expert_credentials_profile_id ON expert_credentials(expert_profile_id);
CREATE INDEX idx_expert_credentials_status ON expert_credentials(verification_status);

-- Expert Availability Slots
CREATE TABLE IF NOT EXISTS expert_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expert_profile_id UUID NOT NULL REFERENCES expert_profiles(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone VARCHAR(50) DEFAULT 'Asia/Hanoi',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(expert_profile_id, day_of_week, start_time, end_time)
);

CREATE INDEX idx_expert_availability_profile_id ON expert_availability(expert_profile_id);
CREATE INDEX idx_expert_availability_active ON expert_availability(is_active);

-- Consultation Bookings
CREATE TABLE IF NOT EXISTS consultation_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_code VARCHAR(50) UNIQUE NOT NULL,
    mother_id UUID NOT NULL REFERENCES users(id),
    expert_id UUID NOT NULL REFERENCES expert_profiles(id),
    scheduled_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER DEFAULT 30,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    payment_status VARCHAR(50) DEFAULT 'UNPAID',
    consultation_fee DECIMAL(10,2) NOT NULL,
    commission_rate DECIMAL(5,2),
    commission_amount DECIMAL(10,2),
    expert_earnings DECIMAL(10,2),
    reason TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_bookings_mother_id ON consultation_bookings(mother_id);
CREATE INDEX idx_consultation_bookings_expert_id ON consultation_bookings(expert_id);
CREATE INDEX idx_consultation_bookings_status ON consultation_bookings(status);
CREATE INDEX idx_consultation_bookings_scheduled_at ON consultation_bookings(scheduled_at);
CREATE INDEX idx_consultation_bookings_payment_status ON consultation_bookings(payment_status);

-- Consultation Sessions
CREATE TABLE IF NOT EXISTS consultation_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID UNIQUE NOT NULL REFERENCES consultation_bookings(id),
    session_token VARCHAR(500) NOT NULL,
    zego_room_id VARCHAR(100),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_sessions_booking_id ON consultation_sessions(booking_id);

-- Expert Reviews
CREATE TABLE IF NOT EXISTS expert_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expert_id UUID NOT NULL REFERENCES expert_profiles(id),
    mother_id UUID NOT NULL REFERENCES users(id),
    booking_id UUID NOT NULL REFERENCES consultation_bookings(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(expert_id, mother_id, booking_id)
);

CREATE INDEX idx_expert_reviews_expert_id ON expert_reviews(expert_id);
CREATE INDEX idx_expert_reviews_booking_id ON expert_reviews(booking_id);

-- Consultation Disputes
CREATE TABLE IF NOT EXISTS consultation_disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES consultation_bookings(id),
    raised_by UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    resolution TEXT,
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_disputes_booking_id ON consultation_disputes(booking_id);
CREATE INDEX idx_consultation_disputes_status ON consultation_disputes(status);
