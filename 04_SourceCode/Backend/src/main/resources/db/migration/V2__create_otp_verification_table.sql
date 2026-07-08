CREATE TABLE otp_verifications (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    requested_role VARCHAR(40),
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_verifications_phone ON otp_verifications (phone);
CREATE INDEX idx_otp_verifications_phone_verified ON otp_verifications (phone, verified);
CREATE INDEX idx_otp_verifications_expires_at ON otp_verifications (expires_at);
