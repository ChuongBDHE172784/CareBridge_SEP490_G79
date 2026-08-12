-- Test-only fixture. Never load this file through production Flyway.
-- The test replaces __ROOT_ID__ and __APPROVER__ before execution.
update public.care_item_templates
   set checklist_metadata_jsonb = jsonb_set(
           checklist_metadata_jsonb,
           '{provenanceStatus}',
           to_jsonb('SIGNED_OFF'::text),
           true),
       content_status='APPROVED',
       migration_review_required=false,
       migration_reviewed_at=now(),
       migration_reviewed_by='__APPROVER__'::uuid,
       distribution_enabled=true,
       approved_at=now(),
       approved_by='__APPROVER__'::uuid
 where template_id='__ROOT_ID__'::uuid;
