-- Move the exercise-correction demo posture configs from MODEL_BASED to HYBRID.
--
-- Under MODEL_BASED, any sidecar fault (unreachable, timeout, rejected contract)
-- removes posture feedback entirely: the session shows only
-- "Posture analysis is temporarily unavailable" and a red skeleton overlay.
-- HYBRID keeps a deterministic rule-based result on that path, flagged as
-- DEGRADED with a capped confidence so it is never mistaken for a model
-- assessment. Model behaviour on the healthy path is unchanged.
--
-- configuration_hash is derived from configuration_jsonb only (see
-- PostureAnalysisConfig#sha256), which this migration does not touch.

UPDATE public.care_item_templates
SET analysis_mode = 'HYBRID',
    updated_at = now()
WHERE entry_type = 'POSTURE_CONFIG'
  AND analysis_mode = 'MODEL_BASED'
  AND template_id IN (
      '66000000-0000-0000-0000-000000000001',
      '66000000-0000-0000-0000-000000000002',
      '66000000-0000-0000-0000-000000000003',
      '66000000-0000-0000-0000-000000000004'
  );
