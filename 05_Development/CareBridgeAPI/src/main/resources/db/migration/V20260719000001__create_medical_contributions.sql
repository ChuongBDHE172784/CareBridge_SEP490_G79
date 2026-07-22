-- Create medical_contributions table
CREATE TABLE medical_contributions (
    contribution_id UUID PRIMARY KEY,
    expert_user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    specialty_id VARCHAR(5) REFERENCES specialties(specialty_id),
    hospital_id VARCHAR(8) REFERENCES hospitals(hospital_id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create contribution_attachments table
CREATE TABLE contribution_attachments (
    attachment_id UUID PRIMARY KEY,
    contribution_id UUID NOT NULL REFERENCES medical_contributions(contribution_id) ON DELETE CASCADE,
    file_id UUID NOT NULL UNIQUE,
    kind VARCHAR(20) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    access_mode VARCHAR(20) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    owner_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contrib_attachments_contribution_id ON contribution_attachments(contribution_id);
CREATE INDEX idx_contrib_attachments_file_id ON contribution_attachments(file_id);