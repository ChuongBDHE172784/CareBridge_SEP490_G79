-- === UC186 — Manage Posture Analysis Configuration ===
-- BR-SAFETY defense-in-depth: confidence_threshold must always be a valid
-- probability-like value in [0.0, 1.0] or NULL. Bean Validation on the admin
-- DTOs is the primary gate (400 PAC-002); this CHECK constraint is the
-- database-level backstop (ADR-PAC-004).

ALTER TABLE public.posture_analysis_configs
    ADD CONSTRAINT chk_posture_config_confidence_threshold
    CHECK (confidence_threshold IS NULL OR (confidence_threshold >= 0.0 AND confidence_threshold <= 1.0));
