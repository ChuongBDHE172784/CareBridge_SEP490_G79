-- Clinical/content provenance is informational for this student-project flow.
-- Content Admin submits a checklist for System Admin approval; publication is
-- controlled by the normal status/contract checks, not an external sign-off.
-- Keep this as a forward migration so environments that already applied
-- V20260812130000 can remove its constraint without rewriting Flyway history.

ALTER TABLE public.care_item_templates
    DROP CONSTRAINT IF EXISTS checklist_pregnancy_v2_provenance_activation_ck;
