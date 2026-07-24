-- Preserve the legacy posture_analysis_configs threshold invariant after its
-- rows are consolidated into the canonical care_item_templates table.
ALTER TABLE public.care_item_templates
    ADD CONSTRAINT chk_care_item_templates_posture_confidence_threshold
    CHECK (
        entry_type <> 'POSTURE_CONFIG'
        OR confidence_threshold IS NULL
        OR (confidence_threshold >= 0.0 AND confidence_threshold <= 1.0)
    );
