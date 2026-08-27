-- ============================================================================
-- Migration V2: Clean up legacy AI triage evidence table and obsolete triggers
-- ============================================================================

-- 1. Drop immutable trigger if exists
DROP TRIGGER IF EXISTS triage_session_evidence_immutable_trg ON public.triage_session_evidence;

-- 2. Drop legacy triage_session_evidence table and its constraints/indexes
DROP TABLE IF EXISTS public.triage_session_evidence CASCADE;
