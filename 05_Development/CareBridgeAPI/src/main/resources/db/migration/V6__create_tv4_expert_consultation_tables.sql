-- V6: Create TV4 Expert Consultation Module Tables
-- Migration: TV4 Sprint 0 Foundation
-- Date: 2026-06-23
-- References: CAREBRIDGE-TV4-TDS-S0-001, ERD Logical Model

-- ============================================
-- Table: experts
-- ============================================
CREATE TABLE IF NOT EXISTS experts (
    expert_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    specialty VARCHAR(100) NOT NULL,
    experience_years INTEGER NOT NULL CHECK (experience_years >= 0),
    professional_title VARCHAR(200),
    workplace VARCHAR(300),
    consultation_scope TEXT,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified_at TIMESTAMPTZ,
    verified_by BIGINT,
    rating_avg DECIMAL(3,2) DEFAULT 0.00 CHECK (rating_avg >= 0 AND rating_avg <= 5),
    review_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_experts_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_experts_verified_by FOREIGN KEY (verified_by)
        REFERENCES users(user_id) ON DELETE SET NULL,

    CONSTRAINT chk_expert_verification_status CHECK (
        verification_status IN ('PENDING_VERIFICATION', 'APPROVED', 'REJECTED', 'SUSPENDED')
    )
);

CREATE INDEX idx_experts_user_id ON experts(user_id);
CREATE INDEX idx_experts_verification_status ON experts(verification_status);
CREATE INDEX idx_experts_specialty ON experts(specialty);

-- ============================================
-- Table: verification_documents
-- ============================================
CREATE TABLE IF NOT EXISTS verification_documents (
    credential_id BIGSERIAL PRIMARY KEY,
    expert_id BIGINT NOT NULL,
    credential_type VARCHAR(50) NOT NULL,
    credential_number VARCHAR(200),
    issuer VARCHAR(200),
    issued_date DATE,
    expiry_date DATE,
    file_url TEXT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT,
    review_status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    review_note TEXT,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_verification_documents_expert FOREIGN KEY (expert_id)
        REFERENCES experts(expert_id) ON DELETE CASCADE,
    CONSTRAINT fk_verification_documents_reviewed_by FOREIGN KEY (reviewed_by)
        REFERENCES users(user_id) ON DELETE SET NULL,

    CONSTRAINT chk_verification_document_status CHECK (
        review_status IN ('UPLOADED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED')
    )
);

CREATE INDEX idx_verification_documents_expert_id ON verification_documents(expert_id);
CREATE INDEX idx_verification_documents_status ON verification_documents(review_status);

-- ============================================
-- Table: expert_availability
-- ============================================
CREATE TABLE IF NOT EXISTS expert_availability (
    availability_id BIGSERIAL PRIMARY KEY,
    expert_id BIGINT NOT NULL,
    slot_start TIMESTAMPTZ NOT NULL,
    slot_end TIMESTAMPTZ NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    booking_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_expert_availability_expert FOREIGN KEY (expert_id)
        REFERENCES experts(expert_id) ON DELETE CASCADE,
    CONSTRAINT fk_expert_availability_booking FOREIGN KEY (booking_id)
        REFERENCES consultations(consultation_id) ON DELETE SET NULL,

    CONSTRAINT chk_availability_status CHECK (
        status IN ('AVAILABLE', 'BOOKED', 'BLOCKED', 'UNAVAILABLE')
    ),
    CONSTRAINT chk_availability_time CHECK (
        slot_end > slot_start
    ),
    CONSTRAINT uq_expert_availability_slot UNIQUE (expert_id, slot_start, slot_end)
);

CREATE INDEX idx_expert_availability_expert_id ON expert_availability(expert_id);
CREATE INDEX idx_expert_availability_slot_times ON expert_availability(slot_start, slot_end);
CREATE INDEX idx_expert_availability_status ON expert_availability(status);

-- ============================================
-- Table: consultations
-- ============================================
CREATE TABLE IF NOT EXISTS consultations (
    consultation_id BIGSERIAL PRIMARY KEY,
    booking_ref VARCHAR(100) NOT NULL UNIQUE,
    expert_id BIGINT NOT NULL,
    requester_user_id BIGINT NOT NULL,
    availability_id BIGINT,
    channel_type VARCHAR(50) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    scheduled_start TIMESTAMPTZ NOT NULL,
    scheduled_end TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    price_snapshot_amount INTEGER NOT NULL CHECK (price_snapshot_amount >= 0),
    commission_rate_snapshot DECIMAL(5,4) CHECK (commission_rate_snapshot >= 0 AND commission_rate_snapshot <= 1),
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    cancellation_policy_snapshot TEXT,
    price_locked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    payment_transaction_id BIGINT,
    session_token TEXT,
    expert_summary TEXT,
    technical_log_json JSONB,
    dispute_status VARCHAR(50) DEFAULT 'NONE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_consultations_expert FOREIGN KEY (expert_id)
        REFERENCES experts(expert_id) ON DELETE RESTRICT,
    CONSTRAINT fk_consultations_requester FOREIGN KEY (requester_user_id)
        REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_consultations_availability FOREIGN KEY (availability_id)
        REFERENCES expert_availability(availability_id) ON DELETE SET NULL,
    CONSTRAINT fk_consultations_payment FOREIGN KEY (payment_transaction_id)
        REFERENCES payment_transactions(payment_id) ON DELETE SET NULL,

    CONSTRAINT chk_consultation_status CHECK (
        status IN (
            'PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED',
            'IN_PROGRESS', 'COMPLETED', 'NO_SHOW', 'RESCHEDULED'
        )
    ),
    CONSTRAINT chk_consultation_dispute_status CHECK (
        dispute_status IN ('NONE', 'PENDING', 'RESOLVED_REFUND', 'RESOLVED_NO_REFUND')
    ),
    CONSTRAINT chk_consultation_scheduled_time CHECK (
        scheduled_end > scheduled_start
    )
);

CREATE INDEX idx_consultations_expert_id ON consultations(expert_id);
CREATE INDEX idx_consultations_requester_id ON consultations(requester_user_id);
CREATE INDEX idx_consultations_status ON consultations(status);
CREATE INDEX idx_consultations_scheduled_start ON consultations(scheduled_start);
CREATE INDEX idx_consultations_booking_ref ON consultations(booking_ref);

-- ============================================
-- Table: consultation_sessions
-- ============================================
CREATE TABLE IF NOT EXISTS consultation_sessions (
    session_id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    communication_room_id VARCHAR(200) NOT NULL,
    session_token TEXT NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    session_status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    expert_summary TEXT,
    technical_log_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_consultation_sessions_consultation FOREIGN KEY (consultation_id)
        REFERENCES consultations(consultation_id) ON DELETE CASCADE,

    CONSTRAINT chk_session_status CHECK (
        session_status IN ('CREATED', 'ACTIVE', 'ENDED', 'FAILED', 'EXPIRED')
    )
);

CREATE INDEX idx_consultation_sessions_consultation_id ON consultation_sessions(consultation_id);
CREATE INDEX idx_consultation_sessions_status ON consultation_sessions(session_status);

-- ============================================
-- Table: consultation_messages
-- ============================================
CREATE TABLE IF NOT EXISTS consultation_messages (
    message_id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    message_body TEXT NOT NULL,
    file_url TEXT,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL DEFAULT 'SENT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_consultation_messages_session FOREIGN KEY (session_id)
        REFERENCES consultation_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_consultation_messages_sender FOREIGN KEY (sender_user_id)
        REFERENCES users(user_id) ON DELETE CASCADE,

    CONSTRAINT chk_message_type CHECK (
        message_type IN ('TEXT', 'IMAGE', 'FILE', 'AUDIO', 'VIDEO', 'SYSTEM')
    ),
    CONSTRAINT chk_message_status CHECK (
        status IN ('SENT', 'DELIVERED', 'READ', 'FAILED')
    )
);

CREATE INDEX idx_consultation_messages_session_id ON consultation_messages(session_id);
CREATE INDEX idx_consultation_messages_sender_id ON consultation_messages(sender_user_id);
CREATE INDEX idx_consultation_messages_sent_at ON consultation_messages(sent_at);

-- ============================================
-- Table: payment_transactions
-- ============================================
CREATE TABLE IF NOT EXISTS payment_transactions (
    payment_id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    booking_ref VARCHAR(100) NOT NULL,
    payer_user_id BIGINT NOT NULL,
    gateway_name VARCHAR(100) NOT NULL,
    gateway_transaction_id VARCHAR(200),
    gross_amount INTEGER NOT NULL CHECK (gross_amount >= 0),
    gateway_fee INTEGER DEFAULT 0 CHECK (gateway_fee >= 0),
    refund_amount INTEGER DEFAULT 0 CHECK (refund_amount >= 0),
    net_paid_amount INTEGER NOT NULL CHECK (net_paid_amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(50) NOT NULL,
    paid_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payment_transactions_consultation FOREIGN KEY (consultation_id)
        REFERENCES consultations(consultation_id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_transactions_payer FOREIGN KEY (payer_user_id)
        REFERENCES users(user_id) ON DELETE RESTRICT,

    CONSTRAINT chk_payment_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED')
    ),
    CONSTRAINT uq_payment_gateway_transaction UNIQUE (gateway_name, gateway_transaction_id)
);

CREATE INDEX idx_payment_transactions_consultation_id ON payment_transactions(consultation_id);
CREATE INDEX idx_payment_transactions_payer_id ON payment_transactions(payer_user_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX idx_payment_transactions_booking_ref ON payment_transactions(booking_ref);

-- ============================================
-- Table: commission_records
-- ============================================
CREATE TABLE IF NOT EXISTS commission_records (
    commission_id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    expert_id BIGINT NOT NULL,
    original_price INTEGER NOT NULL CHECK (original_price >= 0),
    commission_rate DECIMAL(5,4) NOT NULL CHECK (commission_rate >= 0 AND commission_rate <= 1),
    commission_amount INTEGER NOT NULL CHECK (commission_amount >= 0),
    gateway_fee INTEGER DEFAULT 0 CHECK (gateway_fee >= 0),
    refund_amount INTEGER DEFAULT 0 CHECK (refund_amount >= 0),
    expert_net_amount INTEGER NOT NULL CHECK (expert_net_amount >= 0),
    eligible_at TIMESTAMPTZ,
    settlement_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    settled_at TIMESTAMPTZ,
    settlement_reference VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_commission_records_payment FOREIGN KEY (payment_id)
        REFERENCES payment_transactions(payment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_commission_records_expert FOREIGN KEY (expert_id)
        REFERENCES experts(expert_id) ON DELETE RESTRICT,

    CONSTRAINT chk_settlement_status CHECK (
        settlement_status IN ('PENDING', 'ELIGIBLE', 'SETTLED', 'CANCELLED')
    )
);

CREATE INDEX idx_commission_records_expert_id ON commission_records(expert_id);
CREATE INDEX idx_commission_records_payment_id ON commission_records(payment_id);
CREATE INDEX idx_commission_records_settlement_status ON commission_records(settlement_status);
CREATE INDEX idx_commission_records_eligible_at ON commission_records(eligible_at);

-- ============================================
-- Table: expert_reviews
-- ============================================
CREATE TABLE IF NOT EXISTS expert_reviews (
    review_id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    reviewer_user_id BIGINT NOT NULL,
    expert_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    moderation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_expert_reviews_consultation FOREIGN KEY (consultation_id)
        REFERENCES consultations(consultation_id) ON DELETE RESTRICT,
    CONSTRAINT fk_expert_reviews_reviewer FOREIGN KEY (reviewer_user_id)
        REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_expert_reviews_expert FOREIGN KEY (expert_id)
        REFERENCES experts(expert_id) ON DELETE CASCADE,

    CONSTRAINT uq_expert_reviews_consultation UNIQUE (consultation_id),
    CONSTRAINT chk_review_moderation_status CHECK (
        moderation_status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN')
    )
);

CREATE INDEX idx_expert_reviews_expert_id ON expert_reviews(expert_id);
CREATE INDEX idx_expert_reviews_reviewer_id ON expert_reviews(reviewer_user_id);
CREATE INDEX idx_expert_reviews_rating ON expert_reviews(rating);
CREATE INDEX idx_expert_reviews_moderation_status ON expert_reviews(moderation_status);

-- ============================================
-- Table: consultation_price_bands
-- ============================================
CREATE TABLE IF NOT EXISTS consultation_price_bands (
    price_band_id BIGSERIAL PRIMARY KEY,
    configured_by BIGINT NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    specialty_scope VARCHAR(200),
    minimum_price INTEGER NOT NULL CHECK (minimum_price >= 0),
    maximum_price INTEGER NOT NULL CHECK (maximum_price >= 0),
    commission_rate DECIMAL(5,4) NOT NULL CHECK (commission_rate >= 0 AND commission_rate <= 1),
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    effective_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    effective_to TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_consultation_price_bands_configured_by FOREIGN KEY (configured_by)
        REFERENCES users(user_id) ON DELETE RESTRICT,

    CONSTRAINT chk_price_band_status CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'DEPRECATED')
    ),
    CONSTRAINT chk_price_band_range CHECK (
        maximum_price >= minimum_price
    )
);

CREATE INDEX idx_consultation_price_bands_status ON consultation_price_bands(status);
CREATE INDEX idx_consultation_price_bands_duration ON consultation_price_bands(duration_minutes);
CREATE INDEX idx_consultation_price_bands_effective_range ON consultation_price_bands(effective_from, effective_to);

-- ============================================
-- Table: expert_consultation_prices
-- ============================================
CREATE TABLE IF NOT EXISTS expert_consultation_prices (
    expert_price_id BIGSERIAL PRIMARY KEY,
    expert_id BIGINT NOT NULL,
    price_band_id BIGINT,
    channel_type VARCHAR(50) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    price_amount INTEGER NOT NULL CHECK (price_amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    cancellation_policy TEXT,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    effective_to TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version_no INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_expert_consultation_prices_expert FOREIGN KEY (expert_id)
        REFERENCES experts(expert_id) ON DELETE CASCADE,
    CONSTRAINT fk_expert_consultation_prices_band FOREIGN KEY (price_band_id)
        REFERENCES consultation_price_bands(price_band_id) ON DELETE SET NULL,

    CONSTRAINT chk_expert_price_status CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'DEPRECATED')
    ),
    CONSTRAINT uq_expert_price_active UNIQUE (expert_id, channel_type, duration_minutes, status)
        WHERE status = 'ACTIVE'
);

CREATE INDEX idx_expert_consultation_prices_expert_id ON expert_consultation_prices(expert_id);
CREATE INDEX idx_expert_consultation_prices_status ON expert_consultation_prices(status);
CREATE INDEX idx_expert_consultation_prices_effective_range ON expert_consultation_prices(effective_from, effective_to);

-- ============================================
-- Table: consultation_disputes
-- ============================================
CREATE TABLE IF NOT EXISTS consultation_disputes (
    dispute_id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    submitted_by BIGINT NOT NULL,
    resolved_by BIGINT,
    reason_code VARCHAR(100) NOT NULL,
    description TEXT,
    evidence_json JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    resolution_type VARCHAR(50),
    resolution_note TEXT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_consultation_disputes_consultation FOREIGN KEY (consultation_id)
        REFERENCES consultations(consultation_id) ON DELETE RESTRICT,
    CONSTRAINT fk_consultation_disputes_submitted_by FOREIGN KEY (submitted_by)
        REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_consultation_disputes_resolved_by FOREIGN KEY (resolved_by)
        REFERENCES users(user_id) ON DELETE SET NULL,

    CONSTRAINT chk_dispute_status CHECK (
        status IN ('PENDING', 'INVESTIGATING', 'RESOLVED', 'DISMISSED')
    ),
    CONSTRAINT chk_dispute_resolution_type CHECK (
        resolution_type IS NULL OR resolution_type IN ('REFUND', 'CREDIT', 'NO_ACTION')
    )
);

CREATE INDEX idx_consultation_disputes_consultation_id ON consultation_disputes(consultation_id);
CREATE INDEX idx_consultation_disputes_status ON consultation_disputes(status);
CREATE INDEX idx_consultation_disputes_submitted_by ON consultation_disputes(submitted_by);

-- ============================================
-- Table: refund_records
-- ============================================
CREATE TABLE IF NOT EXISTS refund_records (
    refund_id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    dispute_id BIGINT,
    approved_by BIGINT NOT NULL,
    refund_amount INTEGER NOT NULL CHECK (refund_amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    reason VARCHAR(200) NOT NULL,
    gateway_refund_id VARCHAR(200),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_refund_records_payment FOREIGN KEY (payment_id)
        REFERENCES payment_transactions(payment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_refund_records_dispute FOREIGN KEY (dispute_id)
        REFERENCES consultation_disputes(dispute_id) ON DELETE SET NULL,
    CONSTRAINT fk_refund_records_approved_by FOREIGN KEY (approved_by)
        REFERENCES users(user_id) ON DELETE RESTRICT,

    CONSTRAINT chk_refund_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')
    )
);

CREATE INDEX idx_refund_records_payment_id ON refund_records(payment_id);
CREATE INDEX idx_refund_records_dispute_id ON refund_records(dispute_id);
CREATE INDEX idx_refund_records_status ON refund_records(status);

-- ============================================
-- Table: settlement_records
-- ============================================
CREATE TABLE IF NOT EXISTS settlement_records (
    settlement_id BIGSERIAL PRIMARY KEY,
    commission_id BIGINT NOT NULL,
    expert_id BIGINT NOT NULL,
    settlement_period_start DATE NOT NULL,
    settlement_period_end DATE NOT NULL,
    gross_amount INTEGER NOT NULL CHECK (gross_amount >= 0),
    commission_amount INTEGER NOT NULL CHECK (commission_amount >= 0),
    gateway_fee INTEGER DEFAULT 0 CHECK (gateway_fee >= 0),
    refund_amount INTEGER DEFAULT 0 CHECK (refund_amount >= 0),
    expert_net_amount INTEGER NOT NULL CHECK (expert_net_amount >= 0),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    settled_at TIMESTAMPTZ,
    reference_code VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_settlement_records_commission FOREIGN KEY (commission_id)
        REFERENCES commission_records(commission_id) ON DELETE RESTRICT,
    CONSTRAINT fk_settlement_records_expert FOREIGN KEY (expert_id)
        REFERENCES experts(expert_id) ON DELETE CASCADE,

    CONSTRAINT chk_settlement_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT chk_settlement_period CHECK (
        settlement_period_end >= settlement_period_start
    )
);

CREATE INDEX idx_settlement_records_expert_id ON settlement_records(expert_id);
CREATE INDEX idx_settlement_records_commission_id ON settlement_records(commission_id);
CREATE INDEX idx_settlement_records_status ON settlement_records(status);
CREATE INDEX idx_settlement_records_period ON settlement_records(settlement_period_start, settlement_period_end);

-- ============================================
-- Add contribution_points entries for TV4
-- ============================================
INSERT INTO contribution_point_sources (source_type, source_id, description, points_per_action, created_at, updated_at)
VALUES
    ('CONSULTATION_BOOKED', 'consultation_booked', 'Points for booking a consultation', 10, NOW(), NOW()),
    ('CONSULTATION_COMPLETED', 'consultation_completed', 'Points for completing a consultation', 50, NOW(), NOW()),
    ('EXPERT_REVIEW_SUBMITTED', 'expert_review_submitted', 'Points for submitting an expert review', 20, NOW(), NOW()),
    ('VERIFICATION_DOCUMENT_UPLOADED', 'verification_document_uploaded', 'Points for uploading verification document', 5, NOW(), NOW())
ON CONFLICT (source_type) DO NOTHING;

-- ============================================
-- Add notification types for TV4
-- ============================================
INSERT INTO notification_types (type_code, name, description, category, is_system_generated, created_at, updated_at)
VALUES
    ('EXPERT_PROFILE_VERIFICATION', 'Expert Profile Verification', 'Notification about expert profile verification status', 'EXPERT', true, NOW(), NOW()),
    ('CONSULTATION_BOOKING_CONFIRMED', 'Consultation Booking Confirmed', 'Notification when consultation booking is confirmed', 'BOOKING', true, NOW(), NOW()),
    ('CONSULTATION_REMINDER', 'Consultation Reminder', 'Reminder before consultation starts', 'BOOKING', true, NOW(), NOW()),
    ('PAYMENT_SUCCESS', 'Payment Successful', 'Notification after successful payment', 'PAYMENT', true, NOW(), NOW()),
    ('COMMISSION_SETTLED', 'Commission Settled', 'Notification when commission is settled', 'EXPERT', true, NOW(), NOW())
ON CONFLICT (type_code) DO NOTHING;

-- ============================================
-- End of Migration V6
-- ============================================
