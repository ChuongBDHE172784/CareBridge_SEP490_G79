-- TV4: Create emergency_map_handoffs table for UC-40 emergency handoff feature
-- Migration: V20260703000004

CREATE TABLE IF NOT EXISTS public.emergency_map_handoffs (
  handoff_id UUID NOT NULL DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  triage_handoff_id UUID,
  risk_level VARCHAR(20),
  user_latitude DECIMAL(10,8),
  user_longitude DECIMAL(11,8),
  selected_facility_id UUID,
  summary TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT emergency_map_handoffs_pkey PRIMARY KEY (handoff_id),
  CONSTRAINT emergency_map_handoffs_status_check
    CHECK (status IN ('OPEN','ACCEPTED','COMPLETED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_emergency_map_handoffs_user
  ON emergency_map_handoffs (user_id);
CREATE INDEX IF NOT EXISTS idx_emergency_map_handoffs_status
  ON emergency_map_handoffs (status, created_at);
