-- Checklist Cadence V2 P1: nullable same-table expand.
--
-- This migration deliberately does not import checklist content or enable any
-- V2 writer.  Existing V1 rows remain readable while the P2 finalizer assigns
-- their LEGACY contract and validates the new shape.

ALTER TABLE public.care_item_templates
    ADD COLUMN schedule_type varchar(20),
    ADD COLUMN materialization_policy varchar(30),
    ADD COLUMN schedule_group_key varchar(120),
    ADD COLUMN schedule_context_type varchar(10),
    ADD COLUMN schedule_end_mode varchar(20),
    ADD COLUMN week_boundary_rule varchar(30),
    ADD COLUMN checklist_contract_version smallint,
    ADD COLUMN checklist_metadata_jsonb jsonb,
    ADD COLUMN checklist_metadata_hash varchar(128),
    ADD COLUMN checklist_quarantine_reason_code varchar(80);

ALTER TABLE public.checklist_instances
    ADD COLUMN period_key varchar(180),
    ADD COLUMN schedule_zone_id varchar(80),
    ADD COLUMN gestational_dating_revision bigint,
    ADD COLUMN care_group_member_id uuid,
    ADD COLUMN checklist_access_epoch bigint,
    ADD COLUMN checklist_contract_version smallint,
    ADD COLUMN materialization_mode varchar(20),
    ADD COLUMN was_actionable boolean,
    ADD COLUMN checklist_quarantine_reason_code varchar(80);

ALTER TABLE public.checklist_task_instances
    ADD COLUMN checklist_contract_version smallint,
    ADD COLUMN checklist_quarantine_reason_code varchar(80);

ALTER TABLE public.checklist_task_instances
    ALTER COLUMN target_subject DROP NOT NULL;

ALTER TABLE public.mother_journeys
    ADD COLUMN gestational_dating_basis varchar(20),
    ADD COLUMN gestational_dating_revision bigint,
    ADD COLUMN gestational_dating_effective_at timestamptz,
    ADD COLUMN gestational_dating_quarantine_reason_code varchar(80);

ALTER TABLE public.care_group_members
    ADD COLUMN checklist_access_timeline_jsonb jsonb,
    ADD COLUMN checklist_access_epoch bigint,
    ADD COLUMN checklist_access_quarantine_reason_code varchar(80);

-- Runtime writer barrier used by the P2 backfill runner.  A normal application
-- session is denied while the deployment freeze is true; only the explicitly
-- tagged migration session may mutate these rows during the backfill window.
-- The canonical schema deliberately keeps Flyway's login separate from the
-- NOLOGIN schema-owner role.  A deployment runner therefore qualifies either
-- through that owner role (embedded bootstrap/SET ROLE) or through the DDL
-- privilege it already needs to run Flyway.  Runtime roles are denied even if
-- an over-broad grant accidentally gives them CREATE on public.
CREATE OR REPLACE FUNCTION public.checklist_v1_writer_barrier()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF coalesce(current_setting('carebridge.checklist_v1_writes_frozen', true), '') = 'true'
       AND (coalesce(current_setting('carebridge.checklist_p1_p2_role', true), '') <> 'MIGRATION'
            OR (current_user <> 'carebridge_checklist_schema_owner'
                AND NOT pg_has_role(current_user, 'carebridge_checklist_schema_owner', 'member')
                AND (current_user IN (
                         'carebridge_application', 'checklist_operations',
                         'carebridge_checklist_retention_owner')
                     OR NOT has_schema_privilege(current_user, 'public', 'CREATE')))) THEN
        RAISE EXCEPTION 'CHECKLIST_V1_WRITES_FROZEN';
    END IF;
    RETURN COALESCE(NEW, OLD);
END $$;

DROP TRIGGER IF EXISTS checklist_v1_writer_barrier_templates_trg ON public.care_item_templates;
CREATE TRIGGER checklist_v1_writer_barrier_templates_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_v1_writer_barrier();
DROP TRIGGER IF EXISTS checklist_v1_writer_barrier_instances_trg ON public.checklist_instances;
CREATE TRIGGER checklist_v1_writer_barrier_instances_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.checklist_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_v1_writer_barrier();
DROP TRIGGER IF EXISTS checklist_v1_writer_barrier_tasks_trg ON public.checklist_task_instances;
CREATE TRIGGER checklist_v1_writer_barrier_tasks_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.checklist_task_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_v1_writer_barrier();
DROP TRIGGER IF EXISTS checklist_v1_writer_barrier_members_trg ON public.care_group_members;
CREATE TRIGGER checklist_v1_writer_barrier_members_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.care_group_members
FOR EACH ROW EXECUTE FUNCTION public.checklist_v1_writer_barrier();
DROP TRIGGER IF EXISTS checklist_v1_writer_barrier_groups_trg ON public.care_groups;
CREATE TRIGGER checklist_v1_writer_barrier_groups_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.care_groups
FOR EACH ROW EXECUTE FUNCTION public.checklist_v1_writer_barrier();
DROP TRIGGER IF EXISTS checklist_v1_writer_barrier_journeys_trg ON public.mother_journeys;
CREATE TRIGGER checklist_v1_writer_barrier_journeys_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.mother_journeys
FOR EACH ROW EXECUTE FUNCTION public.checklist_v1_writer_barrier();
DROP TRIGGER IF EXISTS checklist_v1_writer_barrier_audit_trg ON public.audit_events;
CREATE TRIGGER checklist_v1_writer_barrier_audit_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.audit_events
FOR EACH ROW EXECUTE FUNCTION public.checklist_v1_writer_barrier();

ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instances_member_fk
        FOREIGN KEY (care_group_member_id)
        REFERENCES public.care_group_members(care_group_member_id)
        ON DELETE RESTRICT;

-- Shape checks are deliberately NOT VALID.  P2 backfills and validates them
-- after every contradictory legacy aggregate has received a durable marker.
ALTER TABLE public.care_item_templates
    ADD CONSTRAINT checklist_template_cadence_shape_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR entry_type <> 'TEMPLATE_ROOT'
        OR COALESCE((schedule_type IS NULL AND materialization_policy IS NULL)
             OR (schedule_type = 'LEGACY'
                 AND materialization_policy = 'LEGACY_WINDOW')
             OR (schedule_type = 'SET'
                 AND materialization_policy = 'SEQUENCE_STEP')
             OR (schedule_type = 'DAILY'
                 AND materialization_policy = 'EACH_DAY')
             OR (schedule_type = 'WEEKLY'
                 AND materialization_policy = 'EACH_WEEK'), false)
    ) NOT VALID,
    ADD CONSTRAINT checklist_template_metadata_shape_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR checklist_metadata_jsonb IS NULL OR jsonb_typeof(checklist_metadata_jsonb) = 'object'
    ) NOT VALID,
    ADD CONSTRAINT checklist_template_marker_ck CHECK (
        checklist_quarantine_reason_code IS NULL
        OR checklist_quarantine_reason_code IN (
            'TEMPLATE_AGGREGATE_CONTRADICTION','INSTANCE_AGGREGATE_CONTRADICTION',
            'TASK_PARENT_CONTRACT_MISMATCH','JOURNEY_DATING_UNRESOLVED',
            'JOURNEY_DATING_CONFLICT','FAMILY_MEMBER_DUPLICATE',
            'FAMILY_MEMBER_OWNER_ROLE','FAMILY_ACCESS_TIMELINE_MISMATCH',
            'AUDIT_EVIDENCE_MISMATCH')
    ) NOT VALID;

ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instance_materialization_shape_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR materialization_mode IS NULL
        OR materialization_mode IN ('LEGACY','EVENT','INTERACTIVE','CATCH_UP')
    ) NOT VALID,
    ADD CONSTRAINT checklist_instance_contract_shape_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR checklist_contract_version IS NULL OR checklist_contract_version IN (1,2)
    ) NOT VALID,
    ADD CONSTRAINT checklist_instance_member_scope_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR (recipient_role = 'FAMILY' AND care_group_id IS NOT NULL AND care_group_member_id IS NOT NULL)
        OR (recipient_role <> 'FAMILY' AND care_group_member_id IS NULL)
    ) NOT VALID,
    ADD CONSTRAINT checklist_instance_marker_ck CHECK (
        checklist_quarantine_reason_code IS NULL
        OR checklist_quarantine_reason_code IN (
            'TEMPLATE_AGGREGATE_CONTRADICTION','INSTANCE_AGGREGATE_CONTRADICTION',
            'TASK_PARENT_CONTRACT_MISMATCH','JOURNEY_DATING_UNRESOLVED',
            'JOURNEY_DATING_CONFLICT','FAMILY_MEMBER_DUPLICATE',
            'FAMILY_MEMBER_OWNER_ROLE','FAMILY_ACCESS_TIMELINE_MISMATCH',
            'AUDIT_EVIDENCE_MISMATCH')
    ) NOT VALID;

ALTER TABLE public.checklist_task_instances
    ADD CONSTRAINT checklist_task_contract_shape_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR checklist_contract_version IS NULL OR checklist_contract_version IN (1,2)
    ) NOT VALID,
    ADD CONSTRAINT checklist_task_marker_ck CHECK (
        checklist_quarantine_reason_code IS NULL
        OR checklist_quarantine_reason_code IN (
            'TEMPLATE_AGGREGATE_CONTRADICTION','INSTANCE_AGGREGATE_CONTRADICTION',
            'TASK_PARENT_CONTRACT_MISMATCH','JOURNEY_DATING_UNRESOLVED',
            'JOURNEY_DATING_CONFLICT','FAMILY_MEMBER_DUPLICATE',
            'FAMILY_MEMBER_OWNER_ROLE','FAMILY_ACCESS_TIMELINE_MISMATCH',
            'AUDIT_EVIDENCE_MISMATCH')
    ) NOT VALID;

ALTER TABLE public.mother_journeys
    ADD CONSTRAINT mother_journey_dating_basis_ck CHECK (
        gestational_dating_quarantine_reason_code IS NOT NULL
        OR gestational_dating_basis IS NULL
        OR gestational_dating_basis IN ('LMP','EDD','LMP_DERIVED_FROM_EDD')
    ) NOT VALID,
    ADD CONSTRAINT mother_journey_dating_pair_ck CHECK (
        gestational_dating_quarantine_reason_code IS NOT NULL
        OR (gestational_dating_basis IS NULL
            AND gestational_dating_revision IS NULL
            AND gestational_dating_effective_at IS NULL)
        OR (gestational_dating_basis IS NOT NULL
            AND gestational_dating_revision IS NOT NULL
            AND gestational_dating_revision > 0
            AND gestational_dating_effective_at IS NOT NULL)
    ) NOT VALID,
    ADD CONSTRAINT mother_journey_dating_marker_ck CHECK (
        gestational_dating_quarantine_reason_code IS NULL
        OR gestational_dating_quarantine_reason_code IN (
            'JOURNEY_DATING_UNRESOLVED','JOURNEY_DATING_CONFLICT')
    ) NOT VALID;

ALTER TABLE public.care_group_members
    ADD CONSTRAINT checklist_member_timeline_shape_ck CHECK (
        checklist_access_quarantine_reason_code IS NOT NULL
        OR checklist_access_timeline_jsonb IS NULL
        OR (jsonb_typeof(checklist_access_timeline_jsonb) = 'object'
            AND checklist_access_timeline_jsonb->>'schema'
                IS NOT DISTINCT FROM 'CHECKLIST_ACCESS_TIMELINE_V1'
            AND jsonb_typeof(checklist_access_timeline_jsonb->'events')
                IS NOT DISTINCT FROM 'array')
    ) NOT VALID,
    ADD CONSTRAINT checklist_member_epoch_shape_ck CHECK (
        checklist_access_quarantine_reason_code IS NOT NULL
        OR checklist_access_epoch IS NULL OR checklist_access_epoch >= 0
    ) NOT VALID,
    ADD CONSTRAINT checklist_member_marker_ck CHECK (
        checklist_access_quarantine_reason_code IS NULL
        OR checklist_access_quarantine_reason_code IN (
            'FAMILY_MEMBER_DUPLICATE','FAMILY_MEMBER_OWNER_ROLE',
            'FAMILY_ACCESS_TIMELINE_MISMATCH','AUDIT_EVIDENCE_MISMATCH')
    ) NOT VALID;

CREATE INDEX checklist_instances_period_lookup_ix
    ON public.checklist_instances(care_context_type, care_context_id, period_key, recipient_user_id);
CREATE INDEX checklist_instances_member_epoch_ix
    ON public.checklist_instances(care_group_member_id, checklist_access_epoch, status);
CREATE INDEX checklist_templates_schedule_lookup_ix
    ON public.care_item_templates(schedule_group_key, schedule_type, schedule_context_type)
    WHERE entry_type = 'TEMPLATE_ROOT';
CREATE INDEX checklist_members_timeline_lookup_ix
    ON public.care_group_members(care_group_id, user_id, invitation_status, checklist_access_epoch);

-- The existing checklist audit constraints are recreated with the two P1
-- membership actions in the same closed action vocabulary.  Existing rows are
-- retained; the new constraints are NOT VALID until P2's evidence finalizer.
ALTER TABLE public.audit_events
    DROP CONSTRAINT IF EXISTS audit_events_checklist_actor_type_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_actor_shape_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_correlation_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_context_type_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_context_pair_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_subject_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_task_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_template_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_reason_code_ck,
    DROP CONSTRAINT IF EXISTS audit_events_checklist_reason_required_ck;

ALTER TABLE public.audit_events
    ADD CONSTRAINT audit_events_checklist_actor_type_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_TEMPLATE_DECIDED','CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED',
            'CHECKLIST_COMPLETED','CHECKLIST_SKIPPED','CHECKLIST_REOPENED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED',
            'CHECKLIST_MIGRATION_QUARANTINED','CHECKLIST_ACCESS_BASELINE',
            'CHECKLIST_ACCESS_REVOKED')
         OR COALESCE(actor_type IS NOT NULL AND actor_type IN ('USER','SYSTEM','SERVICE'), false))
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_actor_shape_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_TEMPLATE_DECIDED','CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED',
            'CHECKLIST_COMPLETED','CHECKLIST_SKIPPED','CHECKLIST_REOPENED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED',
            'CHECKLIST_MIGRATION_QUARANTINED','CHECKLIST_ACCESS_BASELINE',
            'CHECKLIST_ACCESS_REVOKED')
         OR COALESCE((actor_type = 'USER' AND actor_user_id IS NOT NULL AND actor_service IS NULL)
             OR (actor_type IN ('SYSTEM','SERVICE') AND actor_user_id IS NULL
                 AND actor_service IS NOT NULL AND btrim(actor_service) <> ''), false))
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_correlation_ck CHECK
        (event_category NOT LIKE 'CHECKLIST_%' OR correlation_id IS NOT NULL)
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_context_type_ck CHECK
        (event_category NOT LIKE 'CHECKLIST_%'
         OR care_context_type IS NULL OR care_context_type IN ('JOURNEY','BABY'))
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_context_pair_ck CHECK
        (event_category NOT LIKE 'CHECKLIST_%'
         OR ((care_context_type IS NULL AND care_context_id IS NULL)
             OR (care_context_type IS NOT NULL AND care_context_id IS NOT NULL)))
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_subject_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED',
            'CHECKLIST_SKIPPED','CHECKLIST_REOPENED','CHECKLIST_CANCELLED')
         OR subject_user_id IS NOT NULL)
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_task_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED','CHECKLIST_REOPENED')
         OR checklist_task_instance_id IS NOT NULL)
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_template_ck CHECK
        (event_category NOT IN ('CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED')
         OR template_version_id IS NOT NULL)
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_reason_code_ck CHECK
        (event_category NOT LIKE 'CHECKLIST_%'
         OR reason_code IS NULL OR reason_code ~ '^[A-Z0-9_]{1,80}$')
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_reason_required_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_SKIPPED','CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED',
            'CHECKLIST_MIGRATION_QUARANTINED','CHECKLIST_ACCESS_BASELINE',
            'CHECKLIST_ACCESS_REVOKED') OR reason_code IS NOT NULL)
        NOT VALID;

ALTER TABLE public.audit_events
    ADD CONSTRAINT audit_events_checklist_access_actor_ck CHECK
        (event_category NOT IN ('CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED')
         OR COALESCE(actor_type = 'SYSTEM' AND actor_service = 'CHECKLIST_P2_BACKFILL'
             AND actor_user_id IS NULL AND resource_type = 'CARE_GROUP_MEMBER'
             AND resource_id IS NOT NULL, false))
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_migration_origin_ck CHECK
        (event_category <> 'CHECKLIST_MIGRATION_QUARANTINED'
         OR COALESCE((event_origin = 'CHECKLIST_MIGRATION'
             AND actor_type = 'SYSTEM' AND actor_service = 'CHECKLIST_P2_BACKFILL'
             AND actor_user_id IS NULL), false))
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_access_payload_ck CHECK
        (event_category NOT IN ('CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED')
         OR COALESCE((resource_type = 'CARE_GROUP_MEMBER'
             AND resource_id IS NOT NULL
             AND jsonb_typeof(before_payload_jsonb) = 'object'
             AND jsonb_typeof(after_payload_jsonb) = 'object'
             AND before_payload_jsonb->>'schema' = 'CHECKLIST_ACCESS_AUDIT_V1'
             AND after_payload_jsonb->>'schema' = 'CHECKLIST_ACCESS_AUDIT_V1'
             AND before_payload_jsonb ?& ARRAY[
                 'schema','eventType','membershipStatus','checklistView',
                 'checklistComplete','accessEpoch','effectiveFrom','correlationId']
             AND after_payload_jsonb ?& ARRAY[
                 'schema','eventType','membershipStatus','checklistView',
                 'checklistComplete','accessEpoch','effectiveFrom','correlationId']
             AND before_payload_jsonb->>'eventType' = CASE event_category
                 WHEN 'CHECKLIST_ACCESS_BASELINE' THEN 'LEGACY_ACCESS_BASELINE'
                 ELSE 'VIEW_REVOKED' END
             AND after_payload_jsonb->>'eventType' = before_payload_jsonb->>'eventType'
             AND jsonb_typeof(before_payload_jsonb->'membershipStatus') = 'string'
             AND jsonb_typeof(after_payload_jsonb->'membershipStatus') = 'string'
             AND jsonb_typeof(before_payload_jsonb->'checklistView') = 'boolean'
             AND jsonb_typeof(after_payload_jsonb->'checklistView') = 'boolean'
             AND jsonb_typeof(before_payload_jsonb->'checklistComplete') = 'boolean'
             AND jsonb_typeof(after_payload_jsonb->'checklistComplete') = 'boolean'
             AND jsonb_typeof(before_payload_jsonb->'accessEpoch') = 'number'
             AND jsonb_typeof(after_payload_jsonb->'accessEpoch') = 'number'
             AND before_payload_jsonb->>'accessEpoch' ~ '^[0-9]+$'
             AND after_payload_jsonb->>'accessEpoch' ~ '^[0-9]+$'
             AND (before_payload_jsonb->>'accessEpoch')::numeric >= 0
             AND (after_payload_jsonb->>'accessEpoch')::numeric >= 0
             AND jsonb_typeof(before_payload_jsonb->'effectiveFrom') = 'string'
             AND jsonb_typeof(after_payload_jsonb->'effectiveFrom') = 'string'
             AND before_payload_jsonb->>'effectiveFrom' ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}[ T]'
             AND after_payload_jsonb->>'effectiveFrom' ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}[ T]'
             AND pg_input_is_valid(before_payload_jsonb->>'effectiveFrom', 'timestamptz')
             AND pg_input_is_valid(after_payload_jsonb->>'effectiveFrom', 'timestamptz')
             AND before_payload_jsonb->>'correlationId' = correlation_id::text
             AND after_payload_jsonb->>'correlationId' = correlation_id::text
             AND (CASE WHEN event_category = 'CHECKLIST_ACCESS_BASELINE' THEN
                    before_payload_jsonb->>'membershipStatus' = 'ACCEPTED'
                    AND after_payload_jsonb->>'membershipStatus' = 'ACCEPTED'
                    AND (before_payload_jsonb->>'checklistView')::boolean
                    AND (after_payload_jsonb->>'checklistView')::boolean
                 ELSE
                    before_payload_jsonb->>'membershipStatus' = 'ACCEPTED'
                    AND after_payload_jsonb->>'membershipStatus' = 'REVOKED'
                    AND NOT (after_payload_jsonb->>'checklistView')::boolean
                    AND NOT (after_payload_jsonb->>'checklistComplete')::boolean
                 END)), false))
        NOT VALID;

ALTER TABLE public.audit_events
    ADD CONSTRAINT audit_events_checklist_access_origin_ck CHECK
        (event_category NOT IN ('CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED')
         OR COALESCE(event_origin = 'CHECKLIST_ACCESS', false))
        NOT VALID,
    ADD CONSTRAINT audit_events_checklist_access_reason_ck CHECK
        (event_category NOT IN ('CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED')
         OR COALESCE(((event_category = 'CHECKLIST_ACCESS_BASELINE'
               AND reason_code = 'LEGACY_ACCESS_BASELINE')
             OR (event_category = 'CHECKLIST_ACCESS_REVOKED'
                  AND reason_code = 'FAMILY_MEMBER_DUPLICATE')), false))
        NOT VALID;
