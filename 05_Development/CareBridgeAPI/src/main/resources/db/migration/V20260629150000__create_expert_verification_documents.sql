CREATE TYPE verification_doc_type AS ENUM (
  'DEGREE', 'CERTIFICATE', 'LICENSE', 'IDENTITY', 'OTHER'
);

CREATE TYPE verification_doc_status AS ENUM (
  'PENDING_REVIEW', 'APPROVED', 'REJECTED'
);

CREATE TABLE expert_verification_documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  expert_id UUID NOT NULL REFERENCES expert_profiles(id) ON DELETE CASCADE,
  doc_type verification_doc_type NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  status verification_doc_status NOT NULL DEFAULT 'PENDING_REVIEW',
  reject_reason TEXT,
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  reviewed_at TIMESTAMPTZ,
  reviewed_by UUID REFERENCES accounts(id)
);

CREATE INDEX idx_expert_docs_expert ON expert_verification_documents(expert_id);
