-- Checklist Cadence V2 P2: deterministic legacy backfill and constraint finalizer.
--
-- The migration is intentionally data-preserving.  It stamps only nullable
-- compatibility fields, records evidence for every unavailable row, and never
-- rewrites V1 status, completion timestamps, due_at, target values, or keys.

SET lock_timeout = '5s';

DO $$
BEGIN
    -- The deployment runner (or an explicitly configured embedded-bootstrap
    -- harness) must set these session GUCs before Flyway opens the migration
    -- transaction.  P2 never self-enables its bypass: doing so would make a
    -- missing production freeze indistinguishable from an authorized runner.
    IF coalesce(current_setting('carebridge.checklist_v1_writes_frozen', true), '') <> 'true'
       OR coalesce(current_setting('carebridge.checklist_p1_p2_role', true), '') <> 'MIGRATION'
       OR (current_user <> 'carebridge_checklist_schema_owner'
           AND NOT pg_has_role(current_user, 'carebridge_checklist_schema_owner', 'member')
           AND (current_user IN (
                    'carebridge_application', 'checklist_operations',
                    'carebridge_checklist_retention_owner')
                OR NOT has_schema_privilege(current_user, 'public', 'CREATE'))) THEN
        RAISE EXCEPTION 'CHECKLIST_P1_P2_WRITER_BARRIER_MISSING';
    END IF;
    PERFORM pg_advisory_xact_lock(hashtextextended('CHECKLIST_P1_P2_BACKFILL_V1', 0));
    LOCK TABLE public.mother_journeys,
               public.care_group_members,
               public.care_groups,
               public.care_item_templates,
               public.checklist_instances,
               public.checklist_task_instances,
               public.audit_events
        IN SHARE ROW EXCLUSIVE MODE;
END $$;

CREATE OR REPLACE FUNCTION public.checklist_p2_deterministic_uuid(p_key text)
RETURNS uuid
LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    h text := md5(coalesce(p_key, ''));
BEGIN
    RETURN (substr(h, 1, 8) || '-' || substr(h, 9, 4) || '-5' || substr(h, 14, 3)
        || '-' || substr('89ab', (get_byte(convert_to(h, 'UTF8'), 0) % 4) + 1, 1)
        || substr(h, 18, 3) || '-' || substr(h, 21, 12))::uuid;
END $$;

CREATE OR REPLACE FUNCTION public.checklist_p2_quarantine_audit(
    p_resource_type text,
    p_resource_id uuid,
    p_reason_code text,
    p_source_kind text,
    p_recorded_at timestamptz
)
RETURNS uuid
LANGUAGE plpgsql AS $$
DECLARE
    v_correlation uuid := public.checklist_p2_deterministic_uuid(
        concat_ws('|', 'CHECKLIST_MIGRATION_QUARANTINED', p_resource_type,
            p_resource_id::text, p_reason_code, 'CHECKLIST_MIGRATION_QUARANTINE_V1'));
BEGIN
    INSERT INTO public.audit_events (
        audit_event_id, actor_user_id, actor_type, actor_service,
        event_category, resource_type, resource_id, reason_code,
        after_payload_jsonb, correlation_id, event_origin,
        occurred_at, created_at, severity, status)
    SELECT gen_random_uuid(), NULL, 'SYSTEM', 'CHECKLIST_P2_BACKFILL',
        'CHECKLIST_MIGRATION_QUARANTINED', p_resource_type, p_resource_id,
        p_reason_code,
        jsonb_build_object(
            'schema', 'CHECKLIST_MIGRATION_QUARANTINE_V1',
            'sourceKind', p_source_kind,
            'sourceIdHash', 'md5:' || md5(coalesce(p_resource_id::text, '')),
            'reasonCode', p_reason_code,
            'disposition', 'UNAVAILABLE',
            'correlationId', v_correlation::text,
            'metadata', 'REDACTED'),
        v_correlation, 'CHECKLIST_MIGRATION', p_recorded_at, p_recorded_at,
        'MEDIUM', 'OPEN'
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.audit_events existing
        WHERE existing.event_category = 'CHECKLIST_MIGRATION_QUARANTINED'
          AND existing.event_origin = 'CHECKLIST_MIGRATION'
          AND existing.resource_type = p_resource_type
          AND existing.resource_id = p_resource_id
          AND existing.reason_code = p_reason_code
          AND existing.after_payload_jsonb->>'schema' = 'CHECKLIST_MIGRATION_QUARANTINE_V1'
    );
    RETURN v_correlation;
END $$;

CREATE OR REPLACE FUNCTION public.checklist_p2_access_audit(
    p_member_id uuid,
    p_event_type text,
    p_reason_code text,
    p_before jsonb,
    p_after jsonb,
    p_correlation uuid,
    p_recorded_at timestamptz
)
RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO public.audit_events (
        audit_event_id, actor_user_id, actor_type, actor_service,
        event_category, resource_type, resource_id, reason_code,
        before_payload_jsonb, after_payload_jsonb, correlation_id,
        event_origin, occurred_at, created_at, severity, status)
    SELECT gen_random_uuid(), NULL, 'SYSTEM', 'CHECKLIST_P2_BACKFILL',
        CASE p_event_type
            WHEN 'LEGACY_ACCESS_BASELINE' THEN 'CHECKLIST_ACCESS_BASELINE'
            ELSE 'CHECKLIST_ACCESS_REVOKED'
        END,
        'CARE_GROUP_MEMBER', p_member_id, p_reason_code,
        p_before, p_after, p_correlation, 'CHECKLIST_ACCESS',
        p_recorded_at, p_recorded_at, 'MEDIUM', 'OPEN'
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.audit_events existing
        WHERE existing.event_category = CASE p_event_type
            WHEN 'LEGACY_ACCESS_BASELINE' THEN 'CHECKLIST_ACCESS_BASELINE'
            ELSE 'CHECKLIST_ACCESS_REVOKED'
        END
          AND existing.resource_type = 'CARE_GROUP_MEMBER'
          AND existing.resource_id = p_member_id
          AND existing.after_payload_jsonb->>'eventType' = p_event_type
          AND existing.after_payload_jsonb->>'accessEpoch' = p_after->>'accessEpoch'
          AND existing.after_payload_jsonb->>'effectiveFrom' = p_after->>'effectiveFrom'
    );
END $$;

-- P1 introduced this constraint as NOT VALID.  Preserve the immutable
-- quarantine records written by the legacy backfills while keeping the P2
-- writer shape strict.  The legacy pairs are explicit rather than accepting
-- an arbitrary service/origin combination.
ALTER TABLE public.audit_events
    DROP CONSTRAINT IF EXISTS audit_events_checklist_migration_origin_ck,
    ADD CONSTRAINT audit_events_checklist_migration_origin_ck CHECK
        (event_category <> 'CHECKLIST_MIGRATION_QUARANTINED'
         OR COALESCE(
             (event_origin = 'CHECKLIST_MIGRATION'
              AND actor_type = 'SYSTEM'
              AND actor_service = 'CHECKLIST_P2_BACKFILL'
              AND actor_user_id IS NULL)
             OR (event_origin = 'CHECKLIST_MIGRATION'
                 AND actor_type = 'SERVICE'
                 AND actor_service = 'CHECKLIST_LEGACY_BACKFILL'
                 AND actor_user_id IS NULL)
             OR (event_origin = 'AUDIT_LOG'
                 AND actor_type = 'SERVICE'
                 AND actor_service = 'CHECKLIST_CONTEXT_AUTHORITY'
                 AND actor_user_id IS NULL),
             false))
        NOT VALID;

-- Quarantine evidence is append-only.  Keep the three historical producer
-- contracts readable, but reject a malformed payload at the P2 cutover rather
-- than treating it as trustworthy forensic evidence.  The P2 writer uses the
-- redacted V1 after-payload; the two legacy producers wrote their redacted
-- object to the generic payload column.
ALTER TABLE public.audit_events
    DROP CONSTRAINT IF EXISTS audit_events_checklist_migration_payload_ck,
    ADD CONSTRAINT audit_events_checklist_migration_payload_ck CHECK
        (event_category <> 'CHECKLIST_MIGRATION_QUARANTINED'
         OR COALESCE(
             (event_origin = 'CHECKLIST_MIGRATION'
              AND actor_type = 'SYSTEM'
              AND actor_service = 'CHECKLIST_P2_BACKFILL'
              AND actor_user_id IS NULL
              AND resource_id IS NOT NULL
              AND before_payload_jsonb IS NULL
              AND payload IS NULL
              AND jsonb_typeof(after_payload_jsonb) = 'object'
              AND after_payload_jsonb ?& ARRAY[
                  'schema','sourceKind','sourceIdHash','reasonCode',
                  'disposition','correlationId','metadata']
              AND (after_payload_jsonb - ARRAY[
                  'schema','sourceKind','sourceIdHash','reasonCode',
                  'disposition','correlationId','metadata']) = '{}'::jsonb
              AND after_payload_jsonb->>'schema'
                    = 'CHECKLIST_MIGRATION_QUARANTINE_V1'
              AND after_payload_jsonb->>'sourceKind' = CASE resource_type
                  WHEN 'CARE_ITEM_TEMPLATE' THEN 'care_item_templates'
                  WHEN 'CHECKLIST_INSTANCE' THEN 'checklist_instances'
                  WHEN 'CHECKLIST_TASK_INSTANCE' THEN 'checklist_task_instances'
                  WHEN 'MOTHER_JOURNEY' THEN 'mother_journeys'
                  WHEN 'CARE_GROUP_MEMBER' THEN 'care_group_members'
                  ELSE NULL END
              AND after_payload_jsonb->>'sourceIdHash' ~ '^md5:[0-9a-f]{32}$'
              AND after_payload_jsonb->>'reasonCode' = reason_code
              AND after_payload_jsonb->>'disposition' = 'UNAVAILABLE'
              AND after_payload_jsonb->>'correlationId' = correlation_id::text
              AND after_payload_jsonb->>'metadata' = 'REDACTED')
             OR (event_origin = 'CHECKLIST_MIGRATION'
                 AND actor_type = 'SERVICE'
                 AND actor_service = 'CHECKLIST_LEGACY_BACKFILL'
                 AND actor_user_id IS NULL
                 AND resource_id IS NOT NULL
                 AND before_payload_jsonb IS NULL
                 AND after_payload_jsonb IS NULL
                 AND jsonb_typeof(payload) = 'object'
                 AND payload ?& ARRAY[
                     'sourceTable','sourceId','reasonCode','metadata']
                 AND (payload - ARRAY[
                     'sourceTable','sourceId','reasonCode','metadata']) = '{}'::jsonb
                 AND payload->>'sourceTable' = 'preparation_checklist_items'
                 AND payload->>'sourceId' = resource_id::text
                 AND payload->>'reasonCode' = reason_code
                 AND payload->>'metadata' = 'REDACTED')
             OR (event_origin = 'AUDIT_LOG'
                 AND actor_type = 'SERVICE'
                 AND actor_service = 'CHECKLIST_CONTEXT_AUTHORITY'
                 AND actor_user_id IS NULL
                 AND resource_type = 'CARE_GROUP_CONTEXT'
                 AND resource_id IS NOT NULL
                 AND before_payload_jsonb IS NULL
                 AND after_payload_jsonb IS NULL
                 AND jsonb_typeof(payload) = 'object'
                 AND payload ?& ARRAY[
                     'sourceTable','sourceId','reasonCode',
                     'contextType','contextId','metadata']
                 AND (payload - ARRAY[
                     'sourceTable','sourceId','reasonCode',
                     'contextType','contextId','metadata']) = '{}'::jsonb
                 AND payload->>'sourceTable' = 'care_groups'
                 AND payload->>'sourceId' = resource_id::text
                 AND payload->>'reasonCode' = reason_code
                 AND payload->>'contextType' = care_context_type
                 AND payload->>'contextId' = care_context_id::text
                 AND payload->>'metadata' = 'REDACTED'),
             false))
        NOT VALID;

-- Preserve the exact timeline/audit invariant at commit.  Both writes may be
-- issued in either order, but a transaction cannot commit one without the
-- other, and a later retention purge cannot manufacture availability.
CREATE OR REPLACE FUNCTION public.checklist_assert_access_timeline_audit()
RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_member_id uuid;
    v_member_marker text;
    v_timeline jsonb;
BEGIN
    IF TG_TABLE_NAME = 'audit_events' THEN
        IF NEW.event_category NOT IN ('CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED') THEN
            RETURN NEW;
        END IF;
        v_member_id := NEW.resource_id;
    ELSE
        -- A malformed legacy row is made unavailable by P2 before the
        -- deferred trigger is flushed. Preserve it for forensics instead of
        -- rolling the quarantine update back at commit.
        IF NEW.checklist_access_quarantine_reason_code IS NOT NULL THEN
            RETURN NEW;
        END IF;
        v_member_id := NEW.care_group_member_id;
    END IF;

    SELECT checklist_access_quarantine_reason_code,
           checklist_access_timeline_jsonb
      INTO v_member_marker, v_timeline
      FROM public.care_group_members
     WHERE care_group_member_id = v_member_id;

    IF TG_TABLE_NAME = 'audit_events' AND NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_ACCESS_MEMBER_NOT_FOUND';
    END IF;

    IF TG_TABLE_NAME = 'audit_events'
       AND v_member_marker IS NOT NULL
       AND NOT (v_member_marker = 'FAMILY_MEMBER_DUPLICATE'
                AND NEW.event_category = 'CHECKLIST_ACCESS_REVOKED') THEN
        RAISE EXCEPTION 'CHECKLIST_ACCESS_AUDIT_ON_QUARANTINED_MEMBER';
    END IF;

    -- Quarantined duplicate revocations are a migration-only exception: the
    -- paired access audit is allowed to document the revoke even though the
    -- row is unavailable to checklist projection.
    IF v_member_marker = 'FAMILY_MEMBER_DUPLICATE'
       AND TG_TABLE_NAME = 'audit_events' THEN
        RETURN NEW;
    END IF;
    IF NOT public.checklist_p2_access_timeline_valid(v_member_id, v_timeline) THEN
        RAISE EXCEPTION 'CHECKLIST_ACCESS_TIMELINE_AUDIT_MISMATCH';
    END IF;

    RETURN NEW;
END $$;

-- Read-only validator used to quarantine pre-existing timelines without
-- allowing a malformed row to abort the whole upgrade.  Runtime writes are
-- still guarded by the deferred trigger above.
CREATE OR REPLACE FUNCTION public.checklist_p2_access_timeline_valid(
    p_member_id uuid,
    p_timeline jsonb
)
RETURNS boolean
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_catalog
AS $$
DECLARE
    v_event jsonb;
    v_prev_type text;
    v_prev_epoch bigint;
    v_prev_effective timestamptz;
    v_corr uuid;
    v_epoch bigint;
    v_effective timestamptz;
    v_expected_category text;
    v_audit_count bigint;
    v_event_count bigint;
    v_member_epoch bigint;
    v_before jsonb;
    v_after jsonb;
BEGIN
    IF p_timeline IS NULL THEN
        SELECT count(*) INTO v_audit_count
          FROM public.audit_events audit
         WHERE audit.resource_type = 'CARE_GROUP_MEMBER'
           AND audit.resource_id = p_member_id
           AND audit.event_category IN ('CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED');
        RETURN v_audit_count = 0;
    END IF;
    IF jsonb_typeof(p_timeline) IS DISTINCT FROM 'object'
       OR p_timeline->>'schema' IS DISTINCT FROM 'CHECKLIST_ACCESS_TIMELINE_V1'
       OR jsonb_typeof(p_timeline->'events') IS DISTINCT FROM 'array' THEN
        RETURN false;
    END IF;
    v_event_count := jsonb_array_length(p_timeline->'events');
    SELECT count(*) INTO v_audit_count
      FROM public.audit_events audit
     WHERE audit.resource_type = 'CARE_GROUP_MEMBER'
       AND audit.resource_id = p_member_id
       AND audit.event_category IN ('CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED');
    IF v_event_count <> v_audit_count THEN
        RETURN false;
    END IF;

    v_prev_type := NULL;
    v_prev_epoch := NULL;
    v_prev_effective := NULL;
    FOR v_event IN
        SELECT value FROM jsonb_array_elements(p_timeline->'events')
    LOOP
        IF jsonb_typeof(v_event) IS DISTINCT FROM 'object'
           OR (SELECT count(*) FROM jsonb_object_keys(v_event)) <> 8
           OR NOT (v_event ?& ARRAY[
               'schema','eventType','membershipStatus','checklistView',
               'checklistComplete','accessEpoch','effectiveFrom','correlationId']) THEN
            RETURN false;
        END IF;
        IF v_event->>'schema' IS DISTINCT FROM 'CHECKLIST_ACCESS_TIMELINE_V1'
           OR v_event->>'eventType' IS NULL
           OR v_event->>'eventType' NOT IN ('LEGACY_ACCESS_BASELINE','VIEW_REVOKED')
           OR jsonb_typeof(v_event->'membershipStatus') IS DISTINCT FROM 'string'
           OR jsonb_typeof(v_event->'checklistView') IS DISTINCT FROM 'boolean'
           OR jsonb_typeof(v_event->'checklistComplete') IS DISTINCT FROM 'boolean'
           OR jsonb_typeof(v_event->'accessEpoch') IS DISTINCT FROM 'number'
           OR v_event->>'accessEpoch' !~ '^[0-9]+$'
           OR (v_event->>'accessEpoch')::numeric > 9223372036854775807
           OR jsonb_typeof(v_event->'effectiveFrom') IS DISTINCT FROM 'string'
           OR v_event->>'effectiveFrom' !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}[ T]'
           OR NOT pg_input_is_valid(v_event->>'effectiveFrom', 'timestamptz')
           OR v_event->>'correlationId' !~* '^[0-9a-f-]{36}$'
           OR NOT pg_input_is_valid(v_event->>'correlationId', 'uuid') THEN
            RETURN false;
        END IF;

        v_corr := (v_event->>'correlationId')::uuid;
        v_epoch := (v_event->>'accessEpoch')::bigint;
        v_effective := (v_event->>'effectiveFrom')::timestamptz;
        IF (v_prev_type IS NULL AND v_event->>'eventType' <> 'LEGACY_ACCESS_BASELINE')
           OR (v_prev_type = 'LEGACY_ACCESS_BASELINE'
               AND v_event->>'eventType' <> 'VIEW_REVOKED')
           OR (v_prev_type = 'VIEW_REVOKED'
               AND v_event->>'eventType' <> 'LEGACY_ACCESS_BASELINE')
           OR (v_prev_epoch IS NOT NULL AND v_epoch <= v_prev_epoch)
           OR (v_prev_effective IS NOT NULL AND v_effective < v_prev_effective)
           OR (SELECT count(*) FROM jsonb_array_elements(p_timeline->'events') prior
               WHERE prior->>'correlationId' = v_corr::text) <> 1 THEN
            RETURN false;
        END IF;
        IF v_event->>'eventType' = 'LEGACY_ACCESS_BASELINE'
           AND (v_event->>'membershipStatus' <> 'ACCEPTED'
                OR NOT (v_event->>'checklistView')::boolean) THEN
            RETURN false;
        END IF;
        IF v_event->>'eventType' = 'VIEW_REVOKED'
           AND (v_event->>'membershipStatus' <> 'REVOKED'
                OR (v_event->>'checklistView')::boolean
                OR (v_event->>'checklistComplete')::boolean) THEN
            RETURN false;
        END IF;

        v_expected_category := CASE v_event->>'eventType'
            WHEN 'LEGACY_ACCESS_BASELINE' THEN 'CHECKLIST_ACCESS_BASELINE'
            ELSE 'CHECKLIST_ACCESS_REVOKED' END;
        SELECT count(*) INTO v_audit_count
          FROM public.audit_events audit
         WHERE audit.resource_type = 'CARE_GROUP_MEMBER'
           AND audit.resource_id = p_member_id
           AND audit.correlation_id = v_corr
           AND audit.event_category = v_expected_category
           AND audit.event_origin = 'CHECKLIST_ACCESS'
           AND audit.actor_type = 'SYSTEM'
           AND audit.actor_service = 'CHECKLIST_P2_BACKFILL'
           AND audit.actor_user_id IS NULL
           AND audit.reason_code = CASE v_expected_category
               WHEN 'CHECKLIST_ACCESS_BASELINE' THEN 'LEGACY_ACCESS_BASELINE'
               ELSE 'FAMILY_MEMBER_DUPLICATE' END
           AND jsonb_typeof(audit.before_payload_jsonb) = 'object'
           AND jsonb_typeof(audit.after_payload_jsonb) = 'object'
           AND audit.before_payload_jsonb->>'schema' = 'CHECKLIST_ACCESS_AUDIT_V1'
           AND audit.after_payload_jsonb->>'schema' = 'CHECKLIST_ACCESS_AUDIT_V1';
        IF v_audit_count <> 1 THEN
            RETURN false;
        END IF;

        SELECT audit.before_payload_jsonb, audit.after_payload_jsonb
          INTO v_before, v_after
          FROM public.audit_events audit
         WHERE audit.resource_type = 'CARE_GROUP_MEMBER'
           AND audit.resource_id = p_member_id
           AND audit.correlation_id = v_corr
           AND audit.event_category = v_expected_category
           AND audit.event_origin = 'CHECKLIST_ACCESS'
           AND audit.actor_type = 'SYSTEM'
           AND audit.actor_service = 'CHECKLIST_P2_BACKFILL'
           AND audit.actor_user_id IS NULL
         LIMIT 1;
        IF (SELECT count(*) FROM jsonb_object_keys(v_before)) <> 8
           OR (SELECT count(*) FROM jsonb_object_keys(v_after)) <> 8
           OR NOT (v_before ?& ARRAY[
               'schema','eventType','membershipStatus','checklistView',
               'checklistComplete','accessEpoch','effectiveFrom','correlationId'])
           OR NOT (v_after ?& ARRAY[
               'schema','eventType','membershipStatus','checklistView',
               'checklistComplete','accessEpoch','effectiveFrom','correlationId'])
           OR jsonb_typeof(v_before->'membershipStatus') IS DISTINCT FROM 'string'
           OR jsonb_typeof(v_before->'checklistView') IS DISTINCT FROM 'boolean'
           OR jsonb_typeof(v_before->'checklistComplete') IS DISTINCT FROM 'boolean'
           OR jsonb_typeof(v_before->'accessEpoch') IS DISTINCT FROM 'number'
           OR jsonb_typeof(v_after->'membershipStatus') IS DISTINCT FROM 'string'
           OR jsonb_typeof(v_after->'checklistView') IS DISTINCT FROM 'boolean'
           OR jsonb_typeof(v_after->'checklistComplete') IS DISTINCT FROM 'boolean'
           OR jsonb_typeof(v_after->'accessEpoch') IS DISTINCT FROM 'number'
           OR v_before->>'eventType' IS DISTINCT FROM v_event->>'eventType'
           OR v_after->>'eventType' IS DISTINCT FROM v_event->>'eventType'
           OR v_before->>'correlationId' IS DISTINCT FROM v_corr::text
           OR v_after->>'correlationId' IS DISTINCT FROM v_corr::text
           OR v_before->>'effectiveFrom' IS DISTINCT FROM v_event->>'effectiveFrom'
           OR v_after->>'effectiveFrom' IS DISTINCT FROM v_event->>'effectiveFrom'
           OR (v_after - 'schema') IS DISTINCT FROM (v_event - 'schema')
           OR v_before->>'accessEpoch' !~ '^[0-9]+$'
           OR (v_before->>'accessEpoch')::numeric <> v_epoch - 1
           OR v_after->>'accessEpoch' !~ '^[0-9]+$'
           OR (v_after->>'accessEpoch')::numeric <> v_epoch
           OR v_before->>'membershipStatus' IS DISTINCT FROM 'ACCEPTED'
           OR v_before->>'checklistComplete' IS NULL
           OR v_after->>'checklistComplete' IS NULL THEN
            RETURN false;
        END IF;
        v_prev_type := v_event->>'eventType';
        v_prev_epoch := v_epoch;
        v_prev_effective := v_effective;
    END LOOP;
    IF v_event_count > 0 THEN
        SELECT checklist_access_epoch INTO v_member_epoch
          FROM public.care_group_members
         WHERE care_group_member_id = p_member_id;
        IF v_member_epoch IS DISTINCT FROM v_prev_epoch THEN
            RETURN false;
        END IF;
    END IF;
    RETURN true;
END;
$$;

DROP TRIGGER IF EXISTS checklist_access_timeline_audit_ck_trg
    ON public.care_group_members;
CREATE CONSTRAINT TRIGGER checklist_access_timeline_audit_ck_trg
AFTER INSERT OR UPDATE ON public.care_group_members
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION public.checklist_assert_access_timeline_audit();

DROP TRIGGER IF EXISTS checklist_access_audit_timeline_ck_trg
    ON public.audit_events;
CREATE CONSTRAINT TRIGGER checklist_access_audit_timeline_ck_trg
AFTER INSERT ON public.audit_events
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION public.checklist_assert_access_timeline_audit();

-- Replace the legacy recipient trigger before any P2-stamped Family parent is
-- exposed.  The retired context-mapping table is intentionally absent from the
-- current canonical schema; direct care-group links plus the canonical Journey
--/Baby owner FKs are therefore the authoritative context predicate here.
CREATE OR REPLACE FUNCTION public.checklist_validate_instance_recipient()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.recipient_role = 'MOTHER' THEN
        IF NEW.care_group_id IS NOT NULL
           OR NEW.care_group_member_id IS NOT NULL
           OR NEW.checklist_access_epoch IS NOT NULL
           OR NEW.recipient_user_id IS DISTINCT FROM NEW.context_owner_user_id THEN
            RAISE EXCEPTION 'CHECKLIST_MOTHER_RECIPIENT_NOT_AUTHORIZED';
        END IF;
        RETURN NEW;
    END IF;

    PERFORM 1
      FROM public.care_groups care_group
      JOIN public.care_group_members member
        ON member.care_group_id = care_group.care_group_id
      JOIN public.users target_user
        ON target_user.user_id = member.user_id
     WHERE care_group.care_group_id = NEW.care_group_id
       AND care_group.owner_user_id = NEW.context_owner_user_id
       AND care_group.status = 'ACTIVE'
       AND ((NEW.care_context_type = 'JOURNEY'
             AND care_group.linked_journey_id = NEW.care_context_id
             AND care_group.linked_baby_profile_id IS NULL
             AND EXISTS (
                 SELECT 1 FROM public.mother_journeys journey
                  WHERE journey.journey_id = NEW.care_context_id
                    AND journey.owner_user_id = NEW.context_owner_user_id
                    AND journey.status = 'ACTIVE'))
            OR (NEW.care_context_type = 'BABY'
                AND care_group.linked_baby_profile_id = NEW.care_context_id
                AND care_group.linked_journey_id IS NULL
                AND EXISTS (
                    SELECT 1 FROM public.care_subjects baby
                     WHERE baby.care_subject_id = NEW.care_context_id
                       AND baby.owner_user_id = NEW.context_owner_user_id
                       AND baby.subject_type = 'BABY'
                       AND baby.status = 'ACTIVE')))
       AND member.user_id = NEW.recipient_user_id
       AND target_user.role = 'FAMILY'
       AND member.care_group_member_id = NEW.care_group_member_id
        AND upper(coalesce(member.member_role, '')) IN ('MEMBER', 'VIEWER', 'CO_CAREGIVER')
       AND member.invitation_status = 'ACCEPTED'
       AND member.checklist_access_quarantine_reason_code IS NULL
       AND member.checklist_access_epoch IS NOT NULL
       AND member.checklist_access_epoch = NEW.checklist_access_epoch
       AND jsonb_typeof(member.permission_json) = 'object'
       AND jsonb_typeof(member.permission_json->'CHECKLIST_VIEW') = 'boolean'
       AND member.permission_json->>'CHECKLIST_VIEW' = 'true'
     LIMIT 1
     FOR KEY SHARE OF care_group, member;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_FAMILY_RECIPIENT_NOT_AUTHORIZED';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS checklist_validate_instance_recipient_trg
    ON public.checklist_instances;
CREATE TRIGGER checklist_validate_instance_recipient_trg
BEFORE INSERT OR UPDATE OF recipient_role, recipient_user_id, care_group_id,
    care_context_type, care_context_id, context_owner_user_id,
    care_group_member_id, checklist_access_epoch
ON public.checklist_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_instance_recipient();

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_item_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    parent_id uuid;
    parent_status varchar(20);
    parent_review_required boolean;
    parent_reviewed_at timestamptz;
BEGIN
    IF coalesce(current_setting('carebridge.checklist_p1_p2_role', true), '') = 'MIGRATION' THEN
        RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
    END IF;

    IF (TG_OP = 'DELETE' AND OLD.entry_type <> 'CHECKLIST_ENTRY')
       OR (TG_OP = 'INSERT' AND NEW.entry_type <> 'CHECKLIST_ENTRY')
       OR (TG_OP = 'UPDATE'
           AND OLD.entry_type <> 'CHECKLIST_ENTRY'
           AND NEW.entry_type <> 'CHECKLIST_ENTRY') THEN
        RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.parent_template_id IS DISTINCT FROM NEW.parent_template_id
       AND EXISTS (
           SELECT 1
           FROM public.care_item_templates root
           WHERE root.template_id IN (OLD.parent_template_id, NEW.parent_template_id)
             AND root.entry_type = 'TEMPLATE_ROOT'
             AND (root.content_status IN ('APPROVED', 'ARCHIVED')
                  OR root.migration_reviewed_at IS NOT NULL)
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;

    parent_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.parent_template_id ELSE NEW.parent_template_id END;
    IF parent_id IS NULL THEN
        RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
    END IF;

    SELECT content_status, migration_review_required, migration_reviewed_at
    INTO parent_status, parent_review_required, parent_reviewed_at
    FROM public.care_item_templates
    WHERE template_id = parent_id AND entry_type = 'TEMPLATE_ROOT';

    IF (parent_status IN ('APPROVED', 'ARCHIVED') AND parent_review_required = false)
       OR parent_reviewed_at IS NOT NULL THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

DO $$
DECLARE
    v_tx timestamptz := transaction_timestamp();
    v_row record;
    v_lmp date;
    v_basis text;
    v_revision bigint;
    v_effective timestamptz;
    v_effective_text text;
    v_expected_correlation uuid;
    v_correlation uuid;
    v_event_count bigint;
    v_payload_effective text;
    v_payload_recorded text;
    v_owner_duplicate boolean;
    v_new_epoch bigint;
    v_before jsonb;
    v_after jsonb;
    v_timeline_event jsonb;
    v_timeline jsonb;
    v_parent record;
    v_timeline_valid boolean;
BEGIN
    --------------------------------------------------------------------------
    -- 0. Quarantine malformed legacy aggregates before deferred checks are
    --    flushed.  The raw rows remain intact for forensic review; the marker
    --    makes them unavailable to all checklist projections.
    --------------------------------------------------------------------------
    -- Access audits are immutable and polymorphic, so they cannot carry a
    -- foreign key to care_group_members.  A legacy access event whose member
    -- was deleted (or whose resource type/id is malformed) cannot be
    -- quarantined safely: there is no aggregate row on which to place the
    -- availability marker.  Fail closed before any backfill write instead of
    -- allowing such an event to pass the NOT VALID shape checks.
    IF EXISTS (
        SELECT 1
          FROM public.audit_events audit
         WHERE audit.event_category IN (
                   'CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED')
           AND (audit.resource_type IS DISTINCT FROM 'CARE_GROUP_MEMBER'
                OR audit.resource_id IS NULL
                OR NOT EXISTS (
                    SELECT 1
                      FROM public.care_group_members member
                     WHERE member.care_group_member_id = audit.resource_id))
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_ACCESS_AUDIT_MEMBER_NOT_FOUND';
    END IF;

    FOR v_row IN
        SELECT care_group_member_id
          FROM public.care_group_members
         WHERE checklist_access_timeline_jsonb IS NOT NULL
           AND (jsonb_typeof(checklist_access_timeline_jsonb) IS DISTINCT FROM 'object'
                OR checklist_access_timeline_jsonb->>'schema'
                    IS DISTINCT FROM 'CHECKLIST_ACCESS_TIMELINE_V1'
                OR jsonb_typeof(checklist_access_timeline_jsonb->'events')
                    IS DISTINCT FROM 'array')
         ORDER BY care_group_member_id
         FOR UPDATE
    LOOP
        UPDATE public.care_group_members
           SET checklist_access_quarantine_reason_code =
                   'FAMILY_ACCESS_TIMELINE_MISMATCH'
         WHERE care_group_member_id = v_row.care_group_member_id
           AND checklist_access_quarantine_reason_code IS NULL;
        PERFORM public.checklist_p2_quarantine_audit(
            'CARE_GROUP_MEMBER', v_row.care_group_member_id,
            'FAMILY_ACCESS_TIMELINE_MISMATCH', 'care_group_members', v_tx);
    END LOOP;

    UPDATE public.mother_journeys
       SET gestational_dating_quarantine_reason_code = 'JOURNEY_DATING_CONFLICT'
     WHERE journey_type <> 'PREGNANCY'
       AND gestational_dating_quarantine_reason_code IS NULL
       AND (last_menstrual_date IS NOT NULL
            OR estimated_due_date IS NOT NULL
            OR gestational_dating_basis IS NOT NULL
            OR gestational_dating_revision IS NOT NULL
            OR gestational_dating_effective_at IS NOT NULL);
    FOR v_row IN
        SELECT journey_id
          FROM public.mother_journeys
         WHERE journey_type <> 'PREGNANCY'
           AND gestational_dating_quarantine_reason_code = 'JOURNEY_DATING_CONFLICT'
           AND (last_menstrual_date IS NOT NULL
                OR estimated_due_date IS NOT NULL
                OR gestational_dating_basis IS NOT NULL
                OR gestational_dating_revision IS NOT NULL
                OR gestational_dating_effective_at IS NOT NULL)
         ORDER BY journey_id
    LOOP
        PERFORM public.checklist_p2_quarantine_audit(
            'MOTHER_JOURNEY', v_row.journey_id,
            'JOURNEY_DATING_CONFLICT', 'mother_journeys', v_tx);
    END LOOP;

    UPDATE public.care_item_templates
       SET checklist_quarantine_reason_code = 'TEMPLATE_AGGREGATE_CONTRADICTION'
     WHERE checklist_quarantine_reason_code IS NULL
       AND ((entry_type = 'TEMPLATE_ROOT'
             AND ((schedule_type IS NULL) <> (materialization_policy IS NULL)
                  OR (schedule_type IS NOT NULL AND schedule_type NOT IN ('LEGACY','SET','DAILY','WEEKLY'))
                  OR (materialization_policy IS NOT NULL
                      AND materialization_policy NOT IN ('LEGACY_WINDOW','SEQUENCE_STEP',
                          'ONCE_PER_WINDOW','EACH_WEEK','EACH_DAY'))
                  OR (schedule_type = 'LEGACY' AND materialization_policy <> 'LEGACY_WINDOW')
                  OR (schedule_type = 'SET' AND materialization_policy <> 'SEQUENCE_STEP')
                  OR (schedule_type = 'DAILY' AND materialization_policy <> 'EACH_DAY')
                  OR (schedule_type = 'WEEKLY' AND materialization_policy <> 'EACH_WEEK')))
            OR (checklist_metadata_jsonb IS NOT NULL
                AND jsonb_typeof(checklist_metadata_jsonb) <> 'object')
            OR (schedule_group_key IS NOT NULL
                AND (btrim(schedule_group_key) = ''
                     OR schedule_group_key !~ '^[A-Za-z0-9][A-Za-z0-9_.:-]{0,119}$'))
            OR (schedule_context_type IS NOT NULL
                AND schedule_context_type NOT IN ('JOURNEY','BABY'))
            OR (schedule_end_mode IS NOT NULL
                AND schedule_end_mode NOT IN ('NONE','FIXED_OFFSET','STAGE_EXIT'))
            OR (week_boundary_rule IS NOT NULL
                AND week_boundary_rule NOT IN ('NONE','ANCHOR_RELATIVE_7D'))
            OR (checklist_metadata_hash IS NOT NULL
                AND (btrim(checklist_metadata_hash) = ''
                     OR checklist_metadata_hash !~ '^[0-9A-Fa-f]{64}([0-9A-Fa-f]{64})?$'))
            OR ((checklist_metadata_jsonb IS NULL)
                <> (checklist_metadata_hash IS NULL))
            OR (effective_from IS NOT NULL AND effective_to IS NOT NULL
                AND effective_to <= effective_from));
    FOR v_row IN
        SELECT template_id
          FROM public.care_item_templates
         WHERE checklist_quarantine_reason_code = 'TEMPLATE_AGGREGATE_CONTRADICTION'
         ORDER BY template_id
    LOOP
        PERFORM public.checklist_p2_quarantine_audit(
            'CARE_ITEM_TEMPLATE', v_row.template_id,
            'TEMPLATE_AGGREGATE_CONTRADICTION', 'care_item_templates', v_tx);
    END LOOP;

    --------------------------------------------------------------------------
    -- 1. Legacy contract/materialization stamps.  No V1 identity/status field
    --    is touched and no V2 period/key/zone is invented.
    --------------------------------------------------------------------------
    UPDATE public.care_item_templates
       SET checklist_contract_version = 1
     WHERE entry_type IN ('TEMPLATE_ROOT','CHECKLIST_ENTRY')
       AND checklist_contract_version IS NULL;

    UPDATE public.care_item_templates
       SET schedule_type = 'LEGACY',
           materialization_policy = 'LEGACY_WINDOW'
     WHERE entry_type = 'TEMPLATE_ROOT'
       AND checklist_contract_version = 1
       AND schedule_type IS NULL
       AND materialization_policy IS NULL;

    UPDATE public.checklist_instances
       SET checklist_contract_version = 1,
           materialization_mode = 'LEGACY',
           was_actionable = status IN ('PENDING','IN_PROGRESS')
     WHERE checklist_contract_version IS NULL;

    UPDATE public.checklist_task_instances task
       SET checklist_contract_version = parent.checklist_contract_version,
           checklist_quarantine_reason_code = COALESCE(
               task.checklist_quarantine_reason_code,
               parent.checklist_quarantine_reason_code)
      FROM public.checklist_instances parent
     WHERE parent.checklist_instance_id = task.checklist_instance_id
       AND task.checklist_contract_version IS NULL;

    -- Reject malformed contract/target combinations before the old V1 target
    -- checks are replaced.  Every unavailable row receives a typed evidence
    -- event so final CHECK validation cannot turn bad legacy data into a
    -- migration-wide rollback.
    FOR v_row IN
        SELECT template_id
          FROM public.care_item_templates
         WHERE checklist_quarantine_reason_code IS NULL
           AND entry_type = 'CHECKLIST_ENTRY'
           AND (checklist_contract_version IS NULL
                OR checklist_contract_version NOT IN (1,2)
                OR (checklist_contract_version = 1
                    AND (target_subject IS NULL OR target_subject NOT IN ('MOTHER','BABY')))
                OR (checklist_contract_version = 2 AND target_subject IS NOT NULL))
         ORDER BY template_id
         FOR UPDATE
    LOOP
        UPDATE public.care_item_templates
           SET checklist_quarantine_reason_code = 'TEMPLATE_AGGREGATE_CONTRADICTION'
         WHERE template_id = v_row.template_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CARE_ITEM_TEMPLATE', v_row.template_id,
            'TEMPLATE_AGGREGATE_CONTRADICTION', 'care_item_templates', v_tx);
    END LOOP;

    FOR v_row IN
        SELECT checklist_task_instance_id
          FROM public.checklist_task_instances
         WHERE checklist_quarantine_reason_code IS NULL
           AND (checklist_contract_version IS NULL
                OR checklist_contract_version NOT IN (1,2)
                OR (checklist_contract_version = 1
                    AND (target_subject IS NULL OR target_subject NOT IN ('MOTHER','BABY')))
                OR (checklist_contract_version = 2 AND target_subject IS NOT NULL))
         ORDER BY checklist_task_instance_id
         FOR UPDATE
    LOOP
        UPDATE public.checklist_task_instances
           SET checklist_quarantine_reason_code = 'TASK_PARENT_CONTRACT_MISMATCH'
         WHERE checklist_task_instance_id = v_row.checklist_task_instance_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_TASK_INSTANCE', v_row.checklist_task_instance_id,
            'TASK_PARENT_CONTRACT_MISMATCH', 'checklist_task_instances', v_tx);
    END LOOP;

    FOR v_row IN
        SELECT checklist_instance_id
          FROM public.checklist_instances
         WHERE checklist_quarantine_reason_code IS NULL
           AND (checklist_contract_version IS NULL
                OR checklist_contract_version NOT IN (1,2))
         ORDER BY checklist_instance_id
         FOR UPDATE
    LOOP
        UPDATE public.checklist_instances
           SET checklist_quarantine_reason_code = 'INSTANCE_AGGREGATE_CONTRADICTION'
         WHERE checklist_instance_id = v_row.checklist_instance_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_INSTANCE', v_row.checklist_instance_id,
            'INSTANCE_AGGREGATE_CONTRADICTION', 'checklist_instances', v_tx);
    END LOOP;

    FOR v_row IN
        SELECT checklist_instance_id
          FROM public.checklist_instances
         WHERE checklist_quarantine_reason_code IS NULL
           AND ((period_key IS NOT NULL
                 AND period_key !~ '^[A-Za-z0-9][A-Za-z0-9_.:/-]{0,179}$')
                OR (schedule_zone_id IS NOT NULL
                    AND schedule_zone_id !~ '^[A-Za-z0-9._+-]+(?:/[A-Za-z0-9._+-]+)*$')
                OR (gestational_dating_revision IS NOT NULL
                    AND gestational_dating_revision <= 0)
                OR (checklist_contract_version = 2
                    AND (period_key IS NULL
                         OR schedule_zone_id IS NULL
                         OR materialization_mode IS NULL
                         OR was_actionable IS NULL)))
         ORDER BY checklist_instance_id
         FOR UPDATE
    LOOP
        UPDATE public.checklist_instances
           SET checklist_quarantine_reason_code = 'INSTANCE_AGGREGATE_CONTRADICTION'
         WHERE checklist_instance_id = v_row.checklist_instance_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_INSTANCE', v_row.checklist_instance_id,
            'INSTANCE_AGGREGATE_CONTRADICTION', 'checklist_instances', v_tx);
    END LOOP;

    FOR v_row IN
        SELECT checklist_instance_id
          FROM public.checklist_instances
         WHERE checklist_quarantine_reason_code IS NULL
           AND materialization_mode IS NOT NULL
           AND materialization_mode NOT IN ('LEGACY','EVENT','INTERACTIVE','CATCH_UP')
         ORDER BY checklist_instance_id
         FOR UPDATE
    LOOP
        UPDATE public.checklist_instances
           SET checklist_quarantine_reason_code = 'INSTANCE_AGGREGATE_CONTRADICTION'
         WHERE checklist_instance_id = v_row.checklist_instance_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_INSTANCE', v_row.checklist_instance_id,
            'INSTANCE_AGGREGATE_CONTRADICTION', 'checklist_instances', v_tx);
    END LOOP;

    FOR v_row IN
        SELECT checklist_instance_id
          FROM public.checklist_instances
         WHERE checklist_quarantine_reason_code IS NULL
           AND recipient_role <> 'FAMILY'
           AND (care_group_member_id IS NOT NULL OR checklist_access_epoch IS NOT NULL)
         ORDER BY checklist_instance_id
         FOR UPDATE
    LOOP
        UPDATE public.checklist_instances
           SET checklist_quarantine_reason_code = 'INSTANCE_AGGREGATE_CONTRADICTION'
         WHERE checklist_instance_id = v_row.checklist_instance_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_INSTANCE', v_row.checklist_instance_id,
            'INSTANCE_AGGREGATE_CONTRADICTION', 'checklist_instances', v_tx);
    END LOOP;

    FOR v_row IN
        SELECT care_group_member_id
          FROM public.care_group_members
         WHERE checklist_access_quarantine_reason_code IS NULL
           AND checklist_access_epoch < 0
         ORDER BY care_group_member_id
         FOR UPDATE
    LOOP
        UPDATE public.care_group_members
           SET checklist_access_quarantine_reason_code =
                   'FAMILY_ACCESS_TIMELINE_MISMATCH'
         WHERE care_group_member_id = v_row.care_group_member_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CARE_GROUP_MEMBER', v_row.care_group_member_id,
            'FAMILY_ACCESS_TIMELINE_MISMATCH', 'care_group_members', v_tx);
    END LOOP;

    -- Contradictory V1 aggregates become unavailable without changing history.
    UPDATE public.checklist_instances instance
       SET checklist_quarantine_reason_code = 'INSTANCE_AGGREGATE_CONTRADICTION'
     WHERE instance.checklist_quarantine_reason_code IS NULL
       AND ((instance.status = 'COMPLETED' AND instance.completed_at IS NULL)
         OR (instance.status = 'CANCELLED' AND instance.cancelled_at IS NULL)
         OR (instance.window_start IS NULL) <> (instance.window_end IS NULL));

    -- Parent availability is authoritative.  Re-run propagation after the
    -- parent contradiction pass so no task remains actionable under an
    -- unavailable aggregate.
    UPDATE public.checklist_task_instances task
       SET checklist_quarantine_reason_code = COALESCE(
               task.checklist_quarantine_reason_code,
               parent.checklist_quarantine_reason_code)
      FROM public.checklist_instances parent
     WHERE parent.checklist_instance_id = task.checklist_instance_id
       AND parent.checklist_quarantine_reason_code IS NOT NULL
       AND task.checklist_quarantine_reason_code IS NULL;

    UPDATE public.checklist_task_instances task
       SET checklist_quarantine_reason_code = 'TASK_PARENT_CONTRACT_MISMATCH'
      FROM public.checklist_instances parent
     WHERE parent.checklist_instance_id = task.checklist_instance_id
       AND task.checklist_quarantine_reason_code IS NULL
       AND (task.checklist_contract_version IS DISTINCT FROM parent.checklist_contract_version
         OR (parent.origin = 'SYSTEM_TEMPLATE'
             AND (task.template_version_id IS DISTINCT FROM parent.template_version_id
                  OR task.template_item_version_id IS NULL)));

    --------------------------------------------------------------------------
    -- 2. Journey authority.  Existing safe authority + one exact event is a
    --    no-op; only unresolved rows allocate a new dating revision.
    --------------------------------------------------------------------------
    FOR v_row IN
        SELECT journey_id, owner_user_id, journey_type, last_menstrual_date,
               estimated_due_date, version, gestational_dating_basis,
               gestational_dating_revision, gestational_dating_effective_at
          FROM public.mother_journeys
         WHERE journey_type = 'PREGNANCY'
         ORDER BY journey_id
         FOR UPDATE
    LOOP
        v_lmp := NULL;
        v_basis := NULL;
        IF v_row.last_menstrual_date IS NOT NULL
           AND v_row.estimated_due_date IS NOT NULL
           AND v_row.estimated_due_date - v_row.last_menstrual_date = 280 THEN
            v_lmp := v_row.last_menstrual_date;
            v_basis := 'LMP';
        ELSIF v_row.last_menstrual_date IS NOT NULL
              AND v_row.estimated_due_date IS NULL THEN
            v_lmp := v_row.last_menstrual_date;
            v_basis := 'LMP';
        ELSIF v_row.last_menstrual_date IS NULL
              AND v_row.estimated_due_date IS NOT NULL THEN
            v_lmp := v_row.estimated_due_date - 280;
            v_basis := 'EDD';
        END IF;

        IF v_lmp IS NULL THEN
            UPDATE public.mother_journeys
             SET gestational_dating_quarantine_reason_code =
                   'JOURNEY_DATING_UNRESOLVED'
             WHERE journey_id = v_row.journey_id;
            PERFORM public.checklist_p2_quarantine_audit(
                'MOTHER_JOURNEY', v_row.journey_id,
                'JOURNEY_DATING_UNRESOLVED', 'mother_journeys', v_tx);
            CONTINUE;
        END IF;

        IF v_lmp > (v_tx AT TIME ZONE 'Asia/Ho_Chi_Minh')::date THEN
            UPDATE public.mother_journeys
             SET gestational_dating_quarantine_reason_code = 'JOURNEY_DATING_CONFLICT'
             WHERE journey_id = v_row.journey_id;
            PERFORM public.checklist_p2_quarantine_audit(
                'MOTHER_JOURNEY', v_row.journey_id,
                'JOURNEY_DATING_CONFLICT', 'mother_journeys', v_tx);
            CONTINUE;
        END IF;

        IF (v_row.gestational_dating_basis IS NULL
                AND (v_row.gestational_dating_revision IS NOT NULL
                     OR v_row.gestational_dating_effective_at IS NOT NULL))
           OR (v_row.gestational_dating_basis IS NOT NULL
                AND (v_row.gestational_dating_revision IS NULL
                     OR v_row.gestational_dating_effective_at IS NULL)) THEN
            UPDATE public.mother_journeys
               SET gestational_dating_quarantine_reason_code = 'JOURNEY_DATING_CONFLICT'
             WHERE journey_id = v_row.journey_id;
            PERFORM public.checklist_p2_quarantine_audit(
                'MOTHER_JOURNEY', v_row.journey_id,
                'JOURNEY_DATING_CONFLICT', 'mother_journeys', v_tx);
            CONTINUE;
        END IF;

        IF v_row.gestational_dating_basis IS NOT NULL
           AND v_row.gestational_dating_revision IS NOT NULL
           AND v_row.gestational_dating_effective_at IS NOT NULL THEN
            v_basis := v_row.gestational_dating_basis;
            v_effective := v_row.gestational_dating_effective_at;
            v_effective_text := to_char(
                v_effective AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"');
            v_expected_correlation := public.checklist_p2_deterministic_uuid(concat_ws('|',
                v_row.journey_id::text, 'PREGNANCY_EPOCH_STARTED',
                v_row.gestational_dating_revision::text,
                v_row.gestational_dating_basis, v_lmp::text, v_effective_text, 'true'));
            SELECT count(*)
              INTO v_event_count
              FROM public.audit_events event
              WHERE event.event_category = 'MOTHER_JOURNEY_TRANSITION'
                AND event.event_origin = 'JOURNEY_EVENT'
                AND event.subject_reference_id = v_row.journey_id
                AND event.resource_type = 'mother_journeys'
                AND event.resource_id = v_row.journey_id
                AND event.actor_type = 'SYSTEM'
                AND event.correlation_id = v_expected_correlation
                AND event.payload->>'eventType' = 'PREGNANCY_EPOCH_STARTED'
                AND CASE WHEN event.payload->>'gestationalDatingRevision'
                               ~ '^[0-9]+$'
                               AND (event.payload->>'gestationalDatingRevision')::numeric
                                   <= 9223372036854775807
                         THEN (event.payload->>'gestationalDatingRevision')::numeric
                         ELSE -1 END = v_row.gestational_dating_revision
                AND event.payload->>'basis' = v_row.gestational_dating_basis
                AND event.payload->>'canonicalLmp' = v_lmp::text
                AND event.payload->>'effectiveFrom' = v_effective_text
                AND event.payload->>'recordedAt' = v_effective_text
                AND event.payload->>'inferredSource' = 'true'
                AND CASE WHEN event.payload->>'journeyVersion' ~ '^[0-9]+$'
                              AND (event.payload->>'journeyVersion')::numeric
                                  <= 9223372036854775807
                         THEN (event.payload->>'journeyVersion')::numeric
                         ELSE -1 END = v_row.version;
            IF v_event_count = 1 THEN
                CONTINUE;
            END IF;
            UPDATE public.mother_journeys
               SET gestational_dating_quarantine_reason_code = 'JOURNEY_DATING_CONFLICT'
             WHERE journey_id = v_row.journey_id;
            PERFORM public.checklist_p2_quarantine_audit(
                'MOTHER_JOURNEY', v_row.journey_id,
                'JOURNEY_DATING_CONFLICT', 'mother_journeys', v_tx);
            CONTINUE;
        END IF;

        -- A prior interrupted/rolled-back authority write may have left the
        -- canonical event while the current-row tuple is still null.  Reuse
        -- that one exact candidate; never allocate a second dating revision.
        SELECT count(*)
          INTO v_event_count
          FROM public.audit_events event
         WHERE event.event_category = 'MOTHER_JOURNEY_TRANSITION'
           AND event.event_origin = 'JOURNEY_EVENT'
           AND event.subject_reference_id = v_row.journey_id
           AND event.resource_type = 'mother_journeys'
           AND event.resource_id = v_row.journey_id
           AND event.payload->>'eventType' = 'PREGNANCY_EPOCH_STARTED'
           AND event.payload->>'basis' = v_basis
           AND event.payload->>'canonicalLmp' = v_lmp::text
           AND event.payload->>'inferredSource' = 'true';
        IF v_event_count > 1 THEN
            UPDATE public.mother_journeys
               SET gestational_dating_quarantine_reason_code = 'JOURNEY_DATING_CONFLICT'
             WHERE journey_id = v_row.journey_id;
            PERFORM public.checklist_p2_quarantine_audit(
                'MOTHER_JOURNEY', v_row.journey_id,
                'JOURNEY_DATING_CONFLICT', 'mother_journeys', v_tx);
            CONTINUE;
        ELSIF v_event_count = 1 THEN
            SELECT CASE WHEN event.payload->>'gestationalDatingRevision' ~ '^[0-9]+$'
                             AND (event.payload->>'gestationalDatingRevision')::numeric
                                 <= 9223372036854775807
                        THEN (event.payload->>'gestationalDatingRevision')::bigint END,
                   event.occurred_at,
                   event.correlation_id,
                   event.payload->>'effectiveFrom',
                   event.payload->>'recordedAt'
              INTO v_revision, v_effective, v_correlation,
                   v_payload_effective, v_payload_recorded
              FROM public.audit_events event
             WHERE event.event_category = 'MOTHER_JOURNEY_TRANSITION'
               AND event.event_origin = 'JOURNEY_EVENT'
               AND event.subject_reference_id = v_row.journey_id
               AND event.resource_type = 'mother_journeys'
               AND event.resource_id = v_row.journey_id
               AND event.payload->>'eventType' = 'PREGNANCY_EPOCH_STARTED'
               AND event.payload->>'basis' = v_basis
               AND event.payload->>'canonicalLmp' = v_lmp::text
               AND event.payload->>'inferredSource' = 'true'
             LIMIT 1;
            v_effective_text := to_char(
                v_effective AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"');
            v_expected_correlation := public.checklist_p2_deterministic_uuid(concat_ws('|',
                v_row.journey_id::text, 'PREGNANCY_EPOCH_STARTED',
                v_revision::text, v_basis, v_lmp::text, v_effective_text, 'true'));
            IF v_revision IS NULL OR v_revision <= 0
               OR v_correlation IS DISTINCT FROM v_expected_correlation
               OR v_payload_effective IS DISTINCT FROM v_effective_text
               OR v_payload_recorded IS DISTINCT FROM v_effective_text THEN
                UPDATE public.mother_journeys
                   SET gestational_dating_quarantine_reason_code = 'JOURNEY_DATING_CONFLICT'
                 WHERE journey_id = v_row.journey_id;
                PERFORM public.checklist_p2_quarantine_audit(
                    'MOTHER_JOURNEY', v_row.journey_id,
                    'JOURNEY_DATING_CONFLICT', 'mother_journeys', v_tx);
                CONTINUE;
            END IF;
            UPDATE public.mother_journeys
               SET gestational_dating_basis = v_basis,
                   gestational_dating_revision = v_revision,
                   gestational_dating_effective_at = v_effective,
                   gestational_dating_quarantine_reason_code = NULL
             WHERE journey_id = v_row.journey_id;
            CONTINUE;
        END IF;

        SELECT coalesce(max(CASE
          WHEN event.payload->>'gestationalDatingRevision'
                        ~ '^[0-9]+$'
                   AND (event.payload->>'gestationalDatingRevision')::numeric
                       <= 9223372036854775807
                   THEN (event.payload->>'gestationalDatingRevision')::bigint
               END), 0) + 1
          INTO v_revision
          FROM public.audit_events event
         WHERE event.event_category = 'MOTHER_JOURNEY_TRANSITION'
            AND event.event_origin = 'JOURNEY_EVENT'
            AND event.subject_reference_id = v_row.journey_id
            AND event.resource_type = 'mother_journeys'
            AND event.resource_id = v_row.journey_id;
        v_effective := v_tx;
        v_effective_text := to_char(
            v_effective AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"');
        v_correlation := public.checklist_p2_deterministic_uuid(concat_ws('|',
            v_row.journey_id::text, 'PREGNANCY_EPOCH_STARTED', v_revision::text,
            v_basis, v_lmp::text, v_effective_text, 'true'));

        UPDATE public.mother_journeys
           SET gestational_dating_basis = v_basis,
               gestational_dating_revision = v_revision,
               gestational_dating_effective_at = v_effective,
               gestational_dating_quarantine_reason_code = NULL
         WHERE journey_id = v_row.journey_id
           AND gestational_dating_basis IS NULL;

        INSERT INTO public.audit_events (
            audit_event_id, actor_user_id, actor_type, actor_service,
            event_category, subject_user_id, subject_reference_id,
            resource_type, resource_id,
            correlation_id, event_origin, occurred_at, created_at,
            payload, severity, status)
        SELECT gen_random_uuid(), NULL, 'SYSTEM', 'CHECKLIST_P2_BACKFILL',
            'MOTHER_JOURNEY_TRANSITION', v_row.owner_user_id, v_row.journey_id,
            'mother_journeys', v_row.journey_id, v_correlation, 'JOURNEY_EVENT',
            v_effective, v_effective,
            jsonb_build_object(
                'eventType', 'PREGNANCY_EPOCH_STARTED',
                'fromStage', NULL,
                'toStage', 'PREGNANCY',
                'changes', jsonb_build_object(
                    'gestationalDatingBasis', v_basis,
                    'canonicalLmp', v_lmp::text),
                'gestationalDatingRevision', v_revision,
                'journeyVersion', v_row.version,
                'source', 'SYSTEM_DERIVED',
                'confidence', 'ESTIMATED',
                'reason', 'CHECKLIST_P2_GESTATIONAL_DATING_BACKFILL',
                'basis', v_basis,
                'canonicalLmp', v_lmp::text,
                'effectiveFrom', v_effective_text,
                'recordedAt', v_effective_text,
                'inferredSource', true,
                'correlationId', v_correlation::text),
            'MEDIUM', 'OPEN'
        WHERE NOT EXISTS (
            SELECT 1 FROM public.audit_events existing
             WHERE existing.event_category = 'MOTHER_JOURNEY_TRANSITION'
               AND existing.event_origin = 'JOURNEY_EVENT'
               AND existing.resource_type = 'mother_journeys'
               AND existing.resource_id = v_row.journey_id
               AND existing.correlation_id = v_correlation);
    END LOOP;

    --------------------------------------------------------------------------
    -- 3. Family duplicate preflight and semantic revoke/quarantine.
    --------------------------------------------------------------------------
    FOR v_row IN
        SELECT member.care_group_member_id, member.care_group_id, member.user_id,
               member.member_role, member.invitation_status, member.permission_json,
               member.checklist_access_epoch, member.checklist_access_timeline_jsonb,
               app.role AS user_role,
               member.checklist_access_quarantine_reason_code
          FROM public.care_group_members member
          JOIN public.users app ON app.user_id = member.user_id
         WHERE member.invitation_status = 'ACCEPTED'
           AND member.checklist_access_quarantine_reason_code IS NULL
           AND EXISTS (
               SELECT 1 FROM public.care_group_members duplicate
                WHERE duplicate.care_group_id = member.care_group_id
                  AND duplicate.user_id = member.user_id
                  AND duplicate.invitation_status = 'ACCEPTED'
                  AND duplicate.care_group_member_id <> member.care_group_member_id)
         ORDER BY member.care_group_id, member.user_id, member.care_group_member_id
         FOR UPDATE
    LOOP
        IF v_row.checklist_access_timeline_jsonb IS NOT NULL THEN
            UPDATE public.care_group_members
               SET checklist_access_quarantine_reason_code =
                       'FAMILY_ACCESS_TIMELINE_MISMATCH'
             WHERE care_group_member_id = v_row.care_group_member_id
               AND checklist_access_quarantine_reason_code IS NULL;
            PERFORM public.checklist_p2_quarantine_audit(
                'CARE_GROUP_MEMBER', v_row.care_group_member_id,
                'FAMILY_ACCESS_TIMELINE_MISMATCH', 'care_group_members', v_tx);
            CONTINUE;
        END IF;
        SELECT EXISTS (
            SELECT 1
              FROM public.care_group_members duplicate
              JOIN public.users duplicate_user ON duplicate_user.user_id = duplicate.user_id
             WHERE duplicate.care_group_id = v_row.care_group_id
               AND duplicate.user_id = v_row.user_id
               AND duplicate.invitation_status = 'ACCEPTED'
               AND duplicate.checklist_access_quarantine_reason_code IS NULL
               AND (upper(coalesce(duplicate.member_role, '')) IN ('OWNER','PRIMARY_CAREGIVER')
                    OR (upper(coalesce(duplicate.member_role, '')) = 'OWNER'
                        AND duplicate_user.role <> 'FAMILY')))
          INTO v_owner_duplicate;

        IF v_owner_duplicate OR v_row.user_role <> 'FAMILY' THEN
            UPDATE public.care_group_members
               SET checklist_access_quarantine_reason_code = 'FAMILY_MEMBER_OWNER_ROLE'
             WHERE care_group_id = v_row.care_group_id
               AND user_id = v_row.user_id
               AND invitation_status = 'ACCEPTED'
               AND checklist_access_quarantine_reason_code IS NULL;
            PERFORM public.checklist_p2_quarantine_audit(
                'CARE_GROUP_MEMBER', v_row.care_group_member_id,
                'FAMILY_MEMBER_OWNER_ROLE', 'care_group_members', v_tx);
            CONTINUE;
        END IF;

        v_new_epoch := coalesce(v_row.checklist_access_epoch, 0) + 1;
        v_effective_text := to_char(
            v_tx AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"');
        v_correlation := public.checklist_p2_deterministic_uuid(concat_ws('|',
            'CHECKLIST_ACCESS_REVOKED', v_row.care_group_member_id::text,
            'VIEW_REVOKED', v_new_epoch::text, v_effective_text));
        v_before := jsonb_build_object(
            'schema', 'CHECKLIST_ACCESS_AUDIT_V1',
            'eventType', 'VIEW_REVOKED',
            'membershipStatus', v_row.invitation_status,
            'checklistView', coalesce(v_row.permission_json->>'CHECKLIST_VIEW', 'false') = 'true',
            'checklistComplete', coalesce(v_row.permission_json->>'CHECKLIST_COMPLETE', 'false') = 'true',
            'accessEpoch', coalesce(v_row.checklist_access_epoch, 0),
            'effectiveFrom', v_effective_text,
            'correlationId', v_correlation::text);
        v_after := jsonb_build_object(
            'schema', 'CHECKLIST_ACCESS_AUDIT_V1',
            'eventType', 'VIEW_REVOKED',
            'membershipStatus', 'REVOKED',
            'checklistView', false,
            'checklistComplete', false,
            'accessEpoch', v_new_epoch,
            'effectiveFrom', v_effective_text,
            'correlationId', v_correlation::text);
        v_timeline_event := jsonb_build_object(
            'schema', 'CHECKLIST_ACCESS_TIMELINE_V1',
            'eventType', 'VIEW_REVOKED',
            'membershipStatus', 'REVOKED',
            'checklistView', false,
            'checklistComplete', false,
            'accessEpoch', v_new_epoch,
            'effectiveFrom', v_effective_text,
            'correlationId', v_correlation::text);
        v_timeline := jsonb_build_object(
            'schema', 'CHECKLIST_ACCESS_TIMELINE_V1',
            'events', jsonb_build_array(v_timeline_event));

        UPDATE public.care_group_members
           SET invitation_status = 'REVOKED',
               permission_json = jsonb_set(
                   jsonb_set(coalesce(permission_json, '{}'::jsonb),
                       '{CHECKLIST_VIEW}', 'false'::jsonb, true),
                   '{CHECKLIST_COMPLETE}', 'false'::jsonb, true),
               checklist_access_epoch = v_new_epoch,
               checklist_access_timeline_jsonb = v_timeline,
               checklist_access_quarantine_reason_code = 'FAMILY_MEMBER_DUPLICATE'
         WHERE care_group_member_id = v_row.care_group_member_id;
        PERFORM public.checklist_p2_access_audit(
            v_row.care_group_member_id, 'VIEW_REVOKED', 'FAMILY_MEMBER_DUPLICATE',
            v_before, v_after, v_correlation, v_tx);
    END LOOP;

    -- A Family parent is only safe when its complete identity resolves to the
    -- same accepted Family member and to the active Journey/Baby context. Do
    -- this preflight before the baseline loop updates the parent; otherwise
    -- the deferred recipient trigger would abort the whole migration instead
    -- of preserving the row as unavailable.
    FOR v_parent IN
        SELECT instance.checklist_instance_id
          FROM public.checklist_instances instance
         WHERE instance.recipient_role = 'FAMILY'
           AND instance.checklist_quarantine_reason_code IS NULL
           AND NOT EXISTS (
               SELECT 1
                 FROM public.care_groups grp
                 JOIN public.care_group_members member
                   ON member.care_group_id = grp.care_group_id
                 JOIN public.users target_user
                   ON target_user.user_id = member.user_id
                WHERE grp.care_group_id = instance.care_group_id
                  AND grp.owner_user_id = instance.context_owner_user_id
                  AND grp.status = 'ACTIVE'
                  AND ((instance.care_context_type = 'JOURNEY'
                        AND grp.linked_journey_id = instance.care_context_id
                        AND grp.linked_baby_profile_id IS NULL
                        AND EXISTS (
                            SELECT 1
                              FROM public.mother_journeys journey
                             WHERE journey.journey_id = instance.care_context_id
                               AND journey.owner_user_id = instance.context_owner_user_id
                               AND journey.status = 'ACTIVE'))
                       OR (instance.care_context_type = 'BABY'
                           AND grp.linked_baby_profile_id = instance.care_context_id
                           AND grp.linked_journey_id IS NULL
                           AND EXISTS (
                               SELECT 1
                                 FROM public.care_subjects baby
                                WHERE baby.care_subject_id = instance.care_context_id
                                  AND baby.owner_user_id = instance.context_owner_user_id
                                  AND baby.subject_type = 'BABY'
                                  AND baby.status = 'ACTIVE')))
                  AND member.user_id = instance.recipient_user_id
                  AND target_user.role = 'FAMILY'
                  AND (instance.care_group_member_id IS NULL
                       OR member.care_group_member_id = instance.care_group_member_id)
                  AND upper(coalesce(member.member_role, ''))
                      IN ('MEMBER','VIEWER','CO_CAREGIVER')
                  AND member.invitation_status = 'ACCEPTED'
                  AND member.checklist_access_quarantine_reason_code IS NULL
                  AND jsonb_typeof(member.permission_json) = 'object'
                  AND jsonb_typeof(member.permission_json->'CHECKLIST_VIEW') = 'boolean'
                  AND member.permission_json->>'CHECKLIST_VIEW' = 'true')
         ORDER BY instance.checklist_instance_id
         FOR UPDATE
    LOOP
        UPDATE public.checklist_instances
           SET checklist_quarantine_reason_code = 'FAMILY_ACCESS_TIMELINE_MISMATCH'
         WHERE checklist_instance_id = v_parent.checklist_instance_id
           AND checklist_quarantine_reason_code IS NULL;
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_INSTANCE', v_parent.checklist_instance_id,
            'FAMILY_ACCESS_TIMELINE_MISMATCH', 'checklist_instances', v_tx);
    END LOOP;

    -- Unsupported accepted Family members are unavailable even when the
    -- legacy permission flag is false or malformed.  Keeping the marker on
    -- the member prevents a future permission repair from exposing an
    -- identity that was never safe for checklist projection.
    FOR v_row IN
        SELECT member.care_group_member_id
          FROM public.care_group_members member
          LEFT JOIN public.users app ON app.user_id = member.user_id
         WHERE member.invitation_status = 'ACCEPTED'
           AND member.checklist_access_quarantine_reason_code IS NULL
           AND (app.role IS DISTINCT FROM 'FAMILY'
                OR upper(coalesce(member.member_role, ''))
                   NOT IN ('MEMBER','VIEWER','CO_CAREGIVER')
                OR jsonb_typeof(member.permission_json) IS DISTINCT FROM 'object'
                OR jsonb_typeof(member.permission_json->'CHECKLIST_VIEW') IS DISTINCT FROM 'boolean'
                OR member.permission_json->>'CHECKLIST_VIEW' IS DISTINCT FROM 'true')
         ORDER BY member.care_group_member_id
         FOR UPDATE OF member
    LOOP
        UPDATE public.care_group_members
           SET checklist_access_quarantine_reason_code = 'FAMILY_MEMBER_OWNER_ROLE'
         WHERE care_group_member_id = v_row.care_group_member_id
           AND checklist_access_quarantine_reason_code IS NULL;
        PERFORM public.checklist_p2_quarantine_audit(
            'CARE_GROUP_MEMBER', v_row.care_group_member_id,
            'FAMILY_MEMBER_OWNER_ROLE', 'care_group_members', v_tx);
    END LOOP;

    --------------------------------------------------------------------------
    -- 4. One atomic baseline for each unique accepted non-owner Family member.
    --------------------------------------------------------------------------
    FOR v_row IN
        SELECT member.care_group_member_id, member.care_group_id, member.user_id,
               member.invitation_status, member.permission_json,
               member.checklist_access_epoch, member.checklist_access_timeline_jsonb,
               app.role AS user_role
          FROM public.care_group_members member
          JOIN public.users app ON app.user_id = member.user_id
          JOIN public.care_groups grp ON grp.care_group_id = member.care_group_id
                                     AND grp.status = 'ACTIVE'
         WHERE member.invitation_status = 'ACCEPTED'
           AND app.role = 'FAMILY'
            AND upper(coalesce(member.member_role, '')) IN ('MEMBER', 'VIEWER', 'CO_CAREGIVER')
           AND member.checklist_access_quarantine_reason_code IS NULL
           AND jsonb_typeof(member.permission_json) = 'object'
           AND jsonb_typeof(member.permission_json->'CHECKLIST_VIEW') = 'boolean'
           AND member.permission_json->>'CHECKLIST_VIEW' = 'true'
            AND (member.checklist_access_timeline_jsonb IS NULL
                 OR member.checklist_access_timeline_jsonb->'events' = '[]'::jsonb)
            AND NOT EXISTS (
                SELECT 1 FROM public.audit_events access_audit
                 WHERE access_audit.resource_type = 'CARE_GROUP_MEMBER'
                   AND access_audit.resource_id = member.care_group_member_id
                   AND access_audit.event_category IN (
                       'CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED'))
            AND NOT EXISTS (
               SELECT 1 FROM public.care_group_members duplicate
                WHERE duplicate.care_group_id = member.care_group_id
                  AND duplicate.user_id = member.user_id
                  AND duplicate.invitation_status = 'ACCEPTED'
                  AND duplicate.care_group_member_id <> member.care_group_member_id
                  AND duplicate.checklist_access_quarantine_reason_code IS NULL)
         ORDER BY member.care_group_member_id
         FOR UPDATE
    LOOP
        v_new_epoch := coalesce(v_row.checklist_access_epoch, 0) + 1;
        v_effective_text := to_char(
            v_tx AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"');
        v_correlation := public.checklist_p2_deterministic_uuid(concat_ws('|',
            'CHECKLIST_ACCESS_BASELINE', v_row.care_group_member_id::text,
            'LEGACY_ACCESS_BASELINE', v_new_epoch::text, v_effective_text));
        v_before := jsonb_build_object(
            'schema', 'CHECKLIST_ACCESS_AUDIT_V1',
            'eventType', 'LEGACY_ACCESS_BASELINE',
            'membershipStatus', v_row.invitation_status,
            'checklistView', true,
            'checklistComplete', coalesce(v_row.permission_json->>'CHECKLIST_COMPLETE', 'false') = 'true',
            'accessEpoch', coalesce(v_row.checklist_access_epoch, 0),
            'effectiveFrom', v_effective_text,
            'correlationId', v_correlation::text);
        v_after := jsonb_build_object(
            'schema', 'CHECKLIST_ACCESS_AUDIT_V1',
            'eventType', 'LEGACY_ACCESS_BASELINE',
            'membershipStatus', 'ACCEPTED',
            'checklistView', true,
            'checklistComplete', coalesce(v_row.permission_json->>'CHECKLIST_COMPLETE', 'false') = 'true',
            'accessEpoch', v_new_epoch,
            'effectiveFrom', v_effective_text,
            'correlationId', v_correlation::text);
        v_timeline_event := jsonb_build_object(
            'schema', 'CHECKLIST_ACCESS_TIMELINE_V1',
            'eventType', 'LEGACY_ACCESS_BASELINE',
            'membershipStatus', 'ACCEPTED',
            'checklistView', true,
            'checklistComplete', coalesce(v_row.permission_json->>'CHECKLIST_COMPLETE', 'false') = 'true',
            'accessEpoch', v_new_epoch,
            'effectiveFrom', v_effective_text,
            'correlationId', v_correlation::text);
        v_timeline := jsonb_build_object(
            'schema', 'CHECKLIST_ACCESS_TIMELINE_V1',
            'events', jsonb_build_array(v_timeline_event));

        -- Stamp the member first.  The recipient trigger binds a parent to
        -- the member epoch, so the authority row must exist before the parent
        -- update is validated.
        UPDATE public.care_group_members
           SET checklist_access_epoch = v_new_epoch,
               checklist_access_timeline_jsonb = v_timeline
         WHERE care_group_member_id = v_row.care_group_member_id;

        -- Lock and stamp retained Family parents in deterministic UUID order.
        FOR v_parent IN
            SELECT checklist_instance_id, care_group_member_id,
                   checklist_access_epoch, checklist_quarantine_reason_code
              FROM public.checklist_instances
             WHERE care_group_id = v_row.care_group_id
               AND recipient_user_id = v_row.user_id
               AND recipient_role = 'FAMILY'
             ORDER BY checklist_instance_id
             FOR UPDATE
        LOOP
            IF v_parent.checklist_quarantine_reason_code IS NULL
               AND (v_parent.care_group_member_id IS NULL
                    OR v_parent.care_group_member_id = v_row.care_group_member_id)
               AND (v_parent.checklist_access_epoch IS NULL
                    OR v_parent.checklist_access_epoch = v_new_epoch) THEN
                UPDATE public.checklist_instances
                   SET care_group_member_id = v_row.care_group_member_id,
                       checklist_access_epoch = v_new_epoch
                 WHERE checklist_instance_id = v_parent.checklist_instance_id;
            ELSE
                 UPDATE public.checklist_instances
                   SET checklist_quarantine_reason_code = 'FAMILY_ACCESS_TIMELINE_MISMATCH'
                 WHERE checklist_instance_id = v_parent.checklist_instance_id
                   AND checklist_quarantine_reason_code IS NULL;
                PERFORM public.checklist_p2_quarantine_audit(
                    'CHECKLIST_INSTANCE', v_parent.checklist_instance_id,
                    'FAMILY_ACCESS_TIMELINE_MISMATCH', 'checklist_instances', v_tx);
            END IF;
        END LOOP;

        PERFORM public.checklist_p2_access_audit(
            v_row.care_group_member_id, 'LEGACY_ACCESS_BASELINE',
            'LEGACY_ACCESS_BASELINE', v_before, v_after, v_correlation, v_tx);
    END LOOP;

    FOR v_row IN
        SELECT member.care_group_member_id
          FROM public.care_group_members member
         WHERE member.checklist_access_quarantine_reason_code IS NULL
           AND (member.checklist_access_timeline_jsonb IS NULL
                OR CASE WHEN jsonb_typeof(member.checklist_access_timeline_jsonb->'events')
                             IS NOT DISTINCT FROM 'array'
                        THEN jsonb_array_length(member.checklist_access_timeline_jsonb->'events') = 0
                        ELSE false END)
           AND EXISTS (
               SELECT 1 FROM public.audit_events audit
                WHERE audit.resource_type = 'CARE_GROUP_MEMBER'
                  AND audit.resource_id = member.care_group_member_id
                  AND audit.event_category IN ('CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED'))
         ORDER BY member.care_group_member_id
         FOR UPDATE
    LOOP
        UPDATE public.care_group_members
           SET checklist_access_quarantine_reason_code =
                   'FAMILY_ACCESS_TIMELINE_MISMATCH'
         WHERE care_group_member_id = v_row.care_group_member_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CARE_GROUP_MEMBER', v_row.care_group_member_id,
            'FAMILY_ACCESS_TIMELINE_MISMATCH', 'care_group_members', v_tx);
    END LOOP;

    FOR v_row IN
        SELECT member.care_group_member_id
          FROM public.care_group_members member
          JOIN public.users app ON app.user_id = member.user_id
         WHERE member.invitation_status = 'ACCEPTED'
           AND member.checklist_access_quarantine_reason_code IS NULL
           AND jsonb_typeof(member.permission_json) = 'object'
           AND member.permission_json->>'CHECKLIST_VIEW' = 'true'
           AND (app.role IS DISTINCT FROM 'FAMILY'
                OR upper(coalesce(member.member_role, ''))
                   NOT IN ('MEMBER','VIEWER','CO_CAREGIVER'))
         ORDER BY member.care_group_member_id
         FOR UPDATE
    LOOP
        UPDATE public.care_group_members
           SET checklist_access_quarantine_reason_code = 'FAMILY_MEMBER_OWNER_ROLE'
         WHERE care_group_member_id = v_row.care_group_member_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CARE_GROUP_MEMBER', v_row.care_group_member_id,
            'FAMILY_MEMBER_OWNER_ROLE', 'care_group_members', v_tx);
    END LOOP;

    -- Existing non-empty timelines are valid only when every event already has
    -- exactly one typed audit row; otherwise leave them unavailable.
    FOR v_row IN
        SELECT care_group_member_id, checklist_access_timeline_jsonb
          FROM public.care_group_members
         WHERE checklist_access_timeline_jsonb IS NOT NULL
           AND jsonb_typeof(checklist_access_timeline_jsonb->'events') = 'array'
           AND jsonb_array_length(checklist_access_timeline_jsonb->'events') > 0
           AND checklist_access_quarantine_reason_code IS NULL
         FOR UPDATE
    LOOP
        v_timeline_valid := public.checklist_p2_access_timeline_valid(
            v_row.care_group_member_id, v_row.checklist_access_timeline_jsonb);
        IF NOT v_timeline_valid THEN
            UPDATE public.care_group_members
               SET checklist_access_quarantine_reason_code = 'FAMILY_ACCESS_TIMELINE_MISMATCH'
             WHERE care_group_member_id = v_row.care_group_member_id;
            PERFORM public.checklist_p2_quarantine_audit(
                'CARE_GROUP_MEMBER', v_row.care_group_member_id,
                'FAMILY_ACCESS_TIMELINE_MISMATCH', 'care_group_members', v_tx);
        END IF;
    END LOOP;

    -- A parent bound to an unavailable member must not survive the access
    -- finalizer merely because its member/epoch columns are non-null.
    FOR v_parent IN
        SELECT instance.checklist_instance_id
          FROM public.checklist_instances instance
          JOIN public.care_group_members member
            ON member.care_group_member_id = instance.care_group_member_id
         WHERE instance.recipient_role = 'FAMILY'
           AND instance.checklist_quarantine_reason_code IS NULL
           AND (member.checklist_access_quarantine_reason_code IS NOT NULL
                OR member.invitation_status <> 'ACCEPTED'
                OR member.checklist_access_epoch IS NULL
                OR instance.checklist_access_epoch IS DISTINCT FROM member.checklist_access_epoch
                OR jsonb_typeof(member.permission_json) <> 'object'
                OR member.permission_json->>'CHECKLIST_VIEW' IS DISTINCT FROM 'true')
         ORDER BY instance.checklist_instance_id
         FOR UPDATE OF instance
    LOOP
        UPDATE public.checklist_instances
           SET checklist_quarantine_reason_code = 'FAMILY_ACCESS_TIMELINE_MISMATCH'
         WHERE checklist_instance_id = v_parent.checklist_instance_id
           AND checklist_quarantine_reason_code IS NULL;
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_INSTANCE', v_parent.checklist_instance_id,
            'FAMILY_ACCESS_TIMELINE_MISMATCH', 'checklist_instances', v_tx);
    END LOOP;

    -- Any retained Family parent that could not be tied to exactly one safe
    -- member is unavailable; do not let the FK/epoch constraint turn this into
    -- a partial baseline or silently project a Mother-owned row.
    FOR v_parent IN
        SELECT checklist_instance_id
          FROM public.checklist_instances
         WHERE recipient_role = 'FAMILY'
           AND checklist_quarantine_reason_code IS NULL
           AND (care_group_member_id IS NULL OR checklist_access_epoch IS NULL)
         ORDER BY checklist_instance_id
         FOR UPDATE
    LOOP
        UPDATE public.checklist_instances
           SET checklist_quarantine_reason_code = 'FAMILY_ACCESS_TIMELINE_MISMATCH'
         WHERE checklist_instance_id = v_parent.checklist_instance_id;
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_INSTANCE', v_parent.checklist_instance_id,
            'FAMILY_ACCESS_TIMELINE_MISMATCH', 'checklist_instances', v_tx);
    END LOOP;

    UPDATE public.checklist_task_instances task
       SET checklist_quarantine_reason_code = COALESCE(
               task.checklist_quarantine_reason_code,
               parent.checklist_quarantine_reason_code)
      FROM public.checklist_instances parent
     WHERE parent.checklist_instance_id = task.checklist_instance_id
       AND parent.checklist_quarantine_reason_code IS NOT NULL
       AND task.checklist_quarantine_reason_code IS NULL;

    -- Backfill evidence for every durable marker, including markers produced
    -- by the aggregate/contract passes above.  The helper is idempotent, so a
    -- rerun never duplicates a forensic event.
    FOR v_row IN
        SELECT template_id AS resource_id, checklist_quarantine_reason_code AS reason_code
          FROM public.care_item_templates
         WHERE checklist_quarantine_reason_code IS NOT NULL
    LOOP
        PERFORM public.checklist_p2_quarantine_audit(
            'CARE_ITEM_TEMPLATE', v_row.resource_id, v_row.reason_code,
            'care_item_templates', v_tx);
    END LOOP;
    FOR v_row IN
        SELECT checklist_instance_id AS resource_id, checklist_quarantine_reason_code AS reason_code
          FROM public.checklist_instances
         WHERE checklist_quarantine_reason_code IS NOT NULL
    LOOP
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_INSTANCE', v_row.resource_id, v_row.reason_code,
            'checklist_instances', v_tx);
    END LOOP;
    FOR v_row IN
        SELECT checklist_task_instance_id AS resource_id,
               checklist_quarantine_reason_code AS reason_code
          FROM public.checklist_task_instances
         WHERE checklist_quarantine_reason_code IS NOT NULL
    LOOP
        PERFORM public.checklist_p2_quarantine_audit(
            'CHECKLIST_TASK_INSTANCE', v_row.resource_id, v_row.reason_code,
            'checklist_task_instances', v_tx);
    END LOOP;
    FOR v_row IN
        SELECT journey_id AS resource_id,
               gestational_dating_quarantine_reason_code AS reason_code
          FROM public.mother_journeys
         WHERE gestational_dating_quarantine_reason_code IS NOT NULL
    LOOP
        PERFORM public.checklist_p2_quarantine_audit(
            'MOTHER_JOURNEY', v_row.resource_id, v_row.reason_code,
            'mother_journeys', v_tx);
    END LOOP;
    FOR v_row IN
        SELECT care_group_member_id AS resource_id,
               checklist_access_quarantine_reason_code AS reason_code
          FROM public.care_group_members
         WHERE checklist_access_quarantine_reason_code IS NOT NULL
    LOOP
        PERFORM public.checklist_p2_quarantine_audit(
            'CARE_GROUP_MEMBER', v_row.resource_id, v_row.reason_code,
            'care_group_members', v_tx);
    END LOOP;
END $$;

-- These helpers are migration-only primitives.  Keep them callable by the
-- migration session while the backfill runs, then close the boundary before
-- the migration commits so application roles cannot manufacture evidence.
REVOKE ALL ON FUNCTION public.checklist_p2_deterministic_uuid(text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_p2_quarantine_audit(text, uuid, text, text, timestamptz)
    FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_p2_access_audit(uuid, text, text, jsonb, jsonb, uuid, timestamptz)
    FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_p2_access_timeline_valid(uuid, jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.checklist_p2_deterministic_uuid(text)
    TO carebridge_checklist_schema_owner;
GRANT EXECUTE ON FUNCTION public.checklist_p2_quarantine_audit(text, uuid, text, text, timestamptz)
    TO carebridge_checklist_schema_owner;
GRANT EXECUTE ON FUNCTION public.checklist_p2_access_audit(uuid, text, text, jsonb, jsonb, uuid, timestamptz)
    TO carebridge_checklist_schema_owner;
GRANT EXECUTE ON FUNCTION public.checklist_p2_access_timeline_valid(uuid, jsonb)
    TO carebridge_checklist_schema_owner;
-- The validator is also called by the deferred runtime trigger.  Keep the
-- migration-only writers private, but let the application identity execute
-- this read-only predicate when it writes a member or access audit row.
GRANT EXECUTE ON FUNCTION public.checklist_p2_access_timeline_valid(uuid, jsonb)
    TO carebridge_application;

-- Flush the deferred timeline/audit checks before building the partial unique
-- index. PostgreSQL rejects CREATE INDEX while a table has pending trigger
-- events in the same transaction.
SET CONSTRAINTS checklist_access_timeline_audit_ck_trg,
               checklist_access_audit_timeline_ck_trg IMMEDIATE;

CREATE UNIQUE INDEX care_group_members_checklist_accepted_uk
    ON public.care_group_members(care_group_id, user_id)
    WHERE invitation_status = 'ACCEPTED'
      AND checklist_access_quarantine_reason_code IS NULL;

-- The V1 target checks were created before the targetless V2 contract existed.
-- Replace them instead of layering a V2-only check on top of a still-NOT-NULL
-- semantic requirement.
ALTER TABLE public.care_item_templates
    DROP CONSTRAINT IF EXISTS care_item_templates_target_ck;
ALTER TABLE public.checklist_task_instances
    DROP CONSTRAINT IF EXISTS checklist_task_instances_target_ck;
ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_target_contract_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR entry_type <> 'CHECKLIST_ENTRY'
        OR (
            (coalesce(checklist_contract_version, 1) = 2 AND target_subject IS NULL)
            OR (coalesce(checklist_contract_version, 1) = 1
                AND target_subject IS NOT NULL
                AND target_subject IN ('MOTHER','BABY')))
    ) NOT VALID;
ALTER TABLE public.checklist_task_instances
    ADD CONSTRAINT checklist_task_target_contract_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR (
            (coalesce(checklist_contract_version, 1) = 2 AND target_subject IS NULL)
            OR (coalesce(checklist_contract_version, 1) = 1
                AND target_subject IS NOT NULL
                AND target_subject IN ('MOTHER','BABY')))
    ) NOT VALID;

CREATE OR REPLACE FUNCTION public.checklist_validate_task_contract_match()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_parent_contract smallint;
    v_parent_marker varchar(80);
BEGIN
    IF NEW.checklist_quarantine_reason_code IS NOT NULL THEN
        RETURN NEW;
    END IF;
    SELECT parent.checklist_contract_version, parent.checklist_quarantine_reason_code
      INTO v_parent_contract, v_parent_marker
      FROM public.checklist_instances parent
     WHERE parent.checklist_instance_id = NEW.checklist_instance_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_TASK_PARENT_MISSING';
    END IF;
    IF v_parent_marker IS NULL
       AND coalesce(NEW.checklist_contract_version, 1)
           IS DISTINCT FROM coalesce(v_parent_contract, 1) THEN
        RAISE EXCEPTION 'CHECKLIST_TASK_PARENT_CONTRACT_MISMATCH';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS checklist_validate_task_contract_match_trg
    ON public.checklist_task_instances;
CREATE TRIGGER checklist_validate_task_contract_match_trg
BEFORE INSERT OR UPDATE OF checklist_instance_id, checklist_contract_version,
    checklist_quarantine_reason_code
ON public.checklist_task_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_task_contract_match();

CREATE OR REPLACE FUNCTION public.checklist_validate_template_approval()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' THEN RETURN NEW; END IF;
    IF (NEW.distribution_enabled OR NEW.content_status = 'APPROVED')
       AND NEW.migration_review_required THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;
    IF NEW.distribution_enabled OR NEW.content_status = 'APPROVED' THEN
        IF NEW.recipient_scope IS NULL THEN
            RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED';
        END IF;
        IF EXISTS (
            SELECT 1
              FROM public.care_item_templates item
             WHERE item.parent_template_id = NEW.template_id
               AND item.entry_type = 'CHECKLIST_ENTRY'
               AND item.is_active
               AND item.checklist_quarantine_reason_code IS NOT NULL) THEN
            RAISE EXCEPTION 'ITEM_QUARANTINED';
        END IF;
        IF EXISTS (
            SELECT 1
              FROM public.care_item_templates item
             WHERE item.parent_template_id = NEW.template_id
               AND item.entry_type = 'CHECKLIST_ENTRY'
               AND item.is_active
               AND coalesce(item.checklist_contract_version, 1) <> 2
               AND item.target_subject IS NULL) THEN
            RAISE EXCEPTION 'ITEM_TARGET_REQUIRED';
        END IF;
    END IF;
    RETURN NEW;
END $$;

ALTER TABLE public.checklist_instances
    DROP CONSTRAINT IF EXISTS checklist_instance_task_contract_ck,
    ADD CONSTRAINT checklist_instance_task_contract_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR coalesce(checklist_contract_version, 1) IN (1,2)
    ) NOT VALID,
    ADD CONSTRAINT checklist_instance_member_epoch_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR recipient_role <> 'FAMILY'
        OR (care_group_member_id IS NOT NULL AND checklist_access_epoch IS NOT NULL)
    ) NOT VALID;

ALTER TABLE public.checklist_task_instances
    DROP CONSTRAINT IF EXISTS checklist_task_parent_contract_ck,
    DROP CONSTRAINT IF EXISTS checklist_task_v2_targetless_ck,
    ADD CONSTRAINT checklist_task_parent_contract_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR coalesce(checklist_contract_version, 1) IN (1,2)
    ) NOT VALID,
    ADD CONSTRAINT checklist_task_v2_targetless_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR (coalesce(checklist_contract_version, 1) <> 2 OR target_subject IS NULL)
    ) NOT VALID;

CREATE OR REPLACE FUNCTION public.checklist_validate_instance_contract_match()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.checklist_quarantine_reason_code IS NOT NULL THEN
        RETURN NEW;
    END IF;
    IF EXISTS (
        SELECT 1
          FROM public.checklist_task_instances task
         WHERE task.checklist_instance_id = NEW.checklist_instance_id
           AND task.checklist_quarantine_reason_code IS NULL
           AND coalesce(task.checklist_contract_version, 1)
               IS DISTINCT FROM coalesce(NEW.checklist_contract_version, 1)) THEN
        RAISE EXCEPTION 'CHECKLIST_INSTANCE_TASK_CONTRACT_MISMATCH';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS checklist_validate_instance_contract_match_trg
    ON public.checklist_instances;
CREATE TRIGGER checklist_validate_instance_contract_match_trg
BEFORE UPDATE OF checklist_contract_version, checklist_quarantine_reason_code
ON public.checklist_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_instance_contract_match();

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT checklist_template_effective_bounds_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR effective_from IS NULL OR effective_to IS NULL OR effective_to > effective_from
    ) NOT VALID,
    ADD CONSTRAINT checklist_template_schedule_fields_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR ((schedule_group_key IS NULL
             OR (btrim(schedule_group_key) <> ''
                 AND schedule_group_key ~ '^[A-Za-z0-9][A-Za-z0-9_.:-]{0,119}$'))
            AND (schedule_context_type IS NULL
                 OR schedule_context_type IN ('JOURNEY','BABY'))
            AND (schedule_end_mode IS NULL
                 OR schedule_end_mode IN ('NONE','FIXED_OFFSET','STAGE_EXIT'))
            AND (week_boundary_rule IS NULL
                 OR week_boundary_rule IN ('NONE','ANCHOR_RELATIVE_7D'))
            AND ((checklist_metadata_jsonb IS NULL)
                 = (checklist_metadata_hash IS NULL))
            AND (checklist_metadata_hash IS NULL
                 OR checklist_metadata_hash ~ '^[0-9A-Fa-f]{64}([0-9A-Fa-f]{64})?$'))
    ) NOT VALID;
ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instance_period_shape_ck CHECK (
        checklist_quarantine_reason_code IS NOT NULL
        OR ((period_key IS NULL
             OR period_key ~ '^[A-Za-z0-9][A-Za-z0-9_.:/-]{0,179}$')
            AND (schedule_zone_id IS NULL
                 OR schedule_zone_id ~ '^[A-Za-z0-9._+-]+(?:/[A-Za-z0-9._+-]+)*$')
            AND (gestational_dating_revision IS NULL OR gestational_dating_revision > 0)
            AND (coalesce(checklist_contract_version, 1) <> 2
                 OR (period_key IS NOT NULL
                     AND schedule_zone_id IS NOT NULL
                     AND materialization_mode IS NOT NULL
                     AND was_actionable IS NOT NULL)))
    ) NOT VALID;

-- Validation is intentionally last: all malformed aggregates have already been
-- marked, and a failure rolls back the entire migration including audit rows.
-- Audit events are immutable and have no quarantine marker.  A pre-existing
-- malformed non-access CHECKLIST_* audit therefore fails closed at this gate;
-- it is never silently made available by marking only its resource row.
ALTER TABLE public.care_item_templates
    VALIDATE CONSTRAINT checklist_template_cadence_shape_ck,
    VALIDATE CONSTRAINT checklist_template_metadata_shape_ck,
    VALIDATE CONSTRAINT checklist_template_marker_ck,
    VALIDATE CONSTRAINT checklist_template_effective_bounds_ck,
    VALIDATE CONSTRAINT checklist_template_schedule_fields_ck,
    VALIDATE CONSTRAINT care_item_templates_target_contract_ck;
ALTER TABLE public.checklist_instances
    VALIDATE CONSTRAINT checklist_instance_materialization_shape_ck,
    VALIDATE CONSTRAINT checklist_instance_contract_shape_ck,
    VALIDATE CONSTRAINT checklist_instance_member_scope_ck,
    VALIDATE CONSTRAINT checklist_instance_marker_ck,
    VALIDATE CONSTRAINT checklist_instance_task_contract_ck,
    VALIDATE CONSTRAINT checklist_instance_member_epoch_ck,
    VALIDATE CONSTRAINT checklist_instance_period_shape_ck;
ALTER TABLE public.checklist_task_instances
    VALIDATE CONSTRAINT checklist_task_contract_shape_ck,
    VALIDATE CONSTRAINT checklist_task_marker_ck,
    VALIDATE CONSTRAINT checklist_task_parent_contract_ck,
    VALIDATE CONSTRAINT checklist_task_v2_targetless_ck,
    VALIDATE CONSTRAINT checklist_task_target_contract_ck;
ALTER TABLE public.mother_journeys
    VALIDATE CONSTRAINT mother_journey_dating_basis_ck,
    VALIDATE CONSTRAINT mother_journey_dating_pair_ck,
    VALIDATE CONSTRAINT mother_journey_dating_marker_ck;
ALTER TABLE public.care_group_members
    VALIDATE CONSTRAINT checklist_member_timeline_shape_ck,
    VALIDATE CONSTRAINT checklist_member_epoch_shape_ck,
    VALIDATE CONSTRAINT checklist_member_marker_ck;
ALTER TABLE public.audit_events
    VALIDATE CONSTRAINT audit_events_checklist_actor_type_ck,
    VALIDATE CONSTRAINT audit_events_checklist_actor_shape_ck,
    VALIDATE CONSTRAINT audit_events_checklist_correlation_ck,
    VALIDATE CONSTRAINT audit_events_checklist_context_type_ck,
    VALIDATE CONSTRAINT audit_events_checklist_context_pair_ck,
    VALIDATE CONSTRAINT audit_events_checklist_subject_ck,
    VALIDATE CONSTRAINT audit_events_checklist_task_ck,
    VALIDATE CONSTRAINT audit_events_checklist_template_ck,
    VALIDATE CONSTRAINT audit_events_checklist_reason_code_ck,
    VALIDATE CONSTRAINT audit_events_checklist_reason_required_ck,
    VALIDATE CONSTRAINT audit_events_checklist_access_actor_ck,
    VALIDATE CONSTRAINT audit_events_checklist_migration_origin_ck,
    VALIDATE CONSTRAINT audit_events_checklist_migration_payload_ck,
    VALIDATE CONSTRAINT audit_events_checklist_access_payload_ck,
    VALIDATE CONSTRAINT audit_events_checklist_access_origin_ck,
    VALIDATE CONSTRAINT audit_events_checklist_access_reason_ck;
