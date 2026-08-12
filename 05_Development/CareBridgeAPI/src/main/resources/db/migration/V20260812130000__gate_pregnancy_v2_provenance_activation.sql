-- Pregnancy V2 imported recommendations may not be activated until the
-- immutable copy/clinical provenance attestation is explicitly signed off.
-- This is a same-table gate: no new provenance or review table is introduced.
-- Technical migration review is intentionally distinct from copy sign-off.

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT checklist_pregnancy_v2_provenance_activation_ck CHECK (
        entry_type <> 'TEMPLATE_ROOT'
        OR content_status <> 'APPROVED'
        OR stage IS DISTINCT FROM 'PREGNANCY'
        OR COALESCE(checklist_contract_version, 1) <> 2
        OR distribution_enabled = false
        OR (
            checklist_metadata_jsonb IS NOT NULL
            AND jsonb_typeof(checklist_metadata_jsonb) = 'object'
            AND checklist_metadata_jsonb ->> 'schema' = 'CHECKLIST_METADATA_V1'
            AND checklist_metadata_jsonb ->> 'provenanceStatus' = 'SIGNED_OFF'
        )
    ) NOT VALID;

ALTER TABLE public.care_item_templates
    VALIDATE CONSTRAINT checklist_pregnancy_v2_provenance_activation_ck;
