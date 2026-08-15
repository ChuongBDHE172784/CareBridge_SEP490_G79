-- CareBridge production schema baseline generated from the final PostgreSQL 18 catalog.
-- Reference data is intentionally isolated in V2__production_reference_data.sql.

SET check_function_bodies = false;

SET client_min_messages = warning;

-- Deployment roles are external prerequisites.  Validate their complete shape before
-- creating any object so an over-privileged or partially provisioned deployment fails closed.
DO $carebridge_role_contract$
DECLARE
    invalid_roles text;
BEGIN
    SELECT string_agg(expected.role_name, ', ' ORDER BY expected.role_name)
      INTO invalid_roles
      FROM (VALUES
          ('carebridge_checklist_schema_owner', false, false),
          ('carebridge_checklist_retention_owner', false, false),
          ('checklist_operations', true, false),
          ('carebridge_application', true, false)
      ) AS expected(role_name, can_login, inherits_roles)
      LEFT JOIN pg_catalog.pg_roles actual ON actual.rolname = expected.role_name
     WHERE actual.oid IS NULL
        OR actual.rolcanlogin <> expected.can_login
        OR actual.rolinherit <> expected.inherits_roles
        OR actual.rolsuper
        OR actual.rolcreatedb
        OR actual.rolcreaterole
        OR actual.rolreplication
        OR actual.rolbypassrls;

    IF invalid_roles IS NOT NULL THEN
        RAISE EXCEPTION 'CAREBRIDGE_ROLE_SHAPE_INVALID:%', invalid_roles;
    END IF;
    IF current_user IN (
        'carebridge_checklist_schema_owner',
        'carebridge_checklist_retention_owner',
        'checklist_operations',
        'carebridge_application') THEN
        RAISE EXCEPTION 'CAREBRIDGE_FLYWAY_RUNNER_ROLE_INVALID:%', current_user;
    END IF;
    IF EXISTS (
        SELECT 1
          FROM pg_catalog.pg_auth_members membership
         WHERE membership.roleid = to_regrole('carebridge_checklist_retention_owner')
           AND (membership.inherit_option OR membership.set_option)) THEN
        RAISE EXCEPTION 'CAREBRIDGE_RETENTION_OWNER_PREEXISTING_MEMBER';
    END IF;
    IF NOT EXISTS (
        SELECT 1
          FROM pg_catalog.pg_roles runner
         WHERE runner.rolname = current_user
           AND (runner.rolsuper OR runner.rolcreaterole)) THEN
        RAISE EXCEPTION 'CAREBRIDGE_PRIVILEGED_FINALIZER_REQUIRED:%', current_user;
    END IF;
    IF NOT pg_has_role(current_user, 'carebridge_checklist_schema_owner', 'MEMBER') THEN
        EXECUTE format(
            'GRANT carebridge_checklist_schema_owner TO %I',
            current_user);
    END IF;
    EXECUTE format(
        'GRANT carebridge_checklist_retention_owner TO %I',
        current_user);
    EXECUTE 'GRANT CREATE ON SCHEMA public TO carebridge_checklist_retention_owner';
END
$carebridge_role_contract$;

-- Functions

CREATE OR REPLACE FUNCTION public.capture_reminder_occurrence_alias()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public'
AS $function$
BEGIN
    IF NEW.task_type = 'SCHEDULED_REMINDER' AND NEW.scheduled_at IS NOT NULL THEN
        INSERT INTO public.reminder_occurrence_aliases (
            occurrence_id,
            reminder_definition_id,
            owner_user_id,
            scheduled_at,
            occurrence_generation
        ) VALUES (
            public.reminder_occurrence_id_v2(
                NEW.task_id,
                NEW.scheduled_at,
                NEW.reminder_occurrence_generation),
            NEW.task_id,
            NEW.owner_user_id,
            NEW.scheduled_at,
            NEW.reminder_occurrence_generation
        ) ON CONFLICT (occurrence_id) DO NOTHING;
    END IF;
    RETURN NEW;
END
$function$;

CREATE OR REPLACE FUNCTION public.carebridge_care_logs_view_write()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE row_id uuid;
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM public.care_tasks WHERE task_id = OLD.care_log_id AND task_type = 'CARE_LOG';
        RETURN OLD;
    END IF;
    row_id := CASE WHEN TG_OP = 'INSERT'
                  THEN coalesce(NEW.care_log_id, gen_random_uuid())
                  ELSE coalesce(NEW.care_log_id, OLD.care_log_id, gen_random_uuid()) END;
    INSERT INTO public.care_tasks (
        task_id, task_type, creator_user_id, care_subject_id, title, description,
        scheduled_at, completed_at, status, source_reference_type, source_reference_id,
        metadata_jsonb, created_at, updated_at
    ) VALUES (
        row_id, 'CARE_LOG', NEW.recorded_by, NEW.care_subject_id,
        'Care log: ' || NEW.log_type, NEW.note, NEW.started_at, NEW.ended_at,
        coalesce(NEW.status, 'ACTIVE'), 'CARE_LOG', row_id,
        coalesce(NEW.payload_jsonb, '{}'::jsonb) || jsonb_build_object(
            'logType', NEW.log_type, 'endedAt', NEW.ended_at, 'quantity', NEW.quantity,
            'unit', NEW.unit, 'recordedBy', NEW.recorded_by
        ),
        coalesce(NEW.created_at, now()), coalesce(NEW.updated_at, now())
    ) ON CONFLICT (task_id) DO UPDATE
        SET creator_user_id = excluded.creator_user_id,
            care_subject_id = excluded.care_subject_id,
            title = excluded.title,
            description = excluded.description,
            scheduled_at = excluded.scheduled_at,
            completed_at = excluded.completed_at,
            status = excluded.status,
            metadata_jsonb = excluded.metadata_jsonb,
            updated_at = excluded.updated_at;
    NEW.care_log_id := row_id;
    RETURN NEW;
END
$function$;

CREATE OR REPLACE FUNCTION public.carebridge_emergency_contacts_view_write()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE member_id uuid;
BEGIN
    IF TG_OP = 'DELETE' THEN
        UPDATE public.care_group_members
           SET is_emergency_contact = false,
               emergency_contact_priority = NULL,
               updated_at = now()
         WHERE care_group_member_id = OLD.id;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF NEW.name IS DISTINCT FROM OLD.name OR NEW.phone IS DISTINCT FROM OLD.phone THEN
            RAISE EXCEPTION 'EMERGENCY_CONTACT_PROFILE_MANAGED_BY_CARE_GROUP_MEMBER';
        END IF;
        UPDATE public.care_group_members
           SET is_emergency_contact = coalesce(NEW.primary_contact, false),
               emergency_contact_priority = CASE WHEN coalesce(NEW.primary_contact, false) THEN 1 ELSE NULL END,
               member_role = coalesce(nullif(NEW.relationship, ''), member_role),
               updated_at = now()
         WHERE care_group_member_id = OLD.id;
        RETURN NEW;
    END IF;

    SELECT cgm.care_group_member_id
      INTO member_id
      FROM public.care_groups cg
      JOIN public.care_group_members cgm ON cgm.care_group_id = cg.care_group_id
      JOIN public.users member_user ON member_user.user_id = cgm.user_id
     WHERE cg.owner_user_id = NEW.user_id
       AND regexp_replace(coalesce(member_user.phone, ''), '\D', '', 'g') =
           regexp_replace(coalesce(NEW.phone, ''), '\D', '', 'g')
     ORDER BY cgm.updated_at DESC
     LIMIT 1;

    IF member_id IS NULL THEN
        RAISE EXCEPTION 'EMERGENCY_CONTACT_MEMBER_NOT_FOUND: invite the contact to the care group first';
    END IF;

    UPDATE public.care_group_members
       SET is_emergency_contact = coalesce(NEW.primary_contact, true),
           emergency_contact_priority = CASE WHEN coalesce(NEW.primary_contact, true) THEN 1 ELSE NULL END,
           member_role = coalesce(nullif(NEW.relationship, ''), member_role),
           updated_at = now()
     WHERE care_group_member_id = member_id;
    NEW.id := member_id;
    RETURN NEW;
END
$function$;

CREATE OR REPLACE FUNCTION public.carebridge_expert_credentials_view_write()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE row_id uuid;
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM public.attachments
         WHERE attachment_id = OLD.credential_id
           AND attachment_category = 'EXPERT_CREDENTIAL';
        RETURN OLD;
    END IF;

    row_id := CASE WHEN TG_OP = 'INSERT'
                  THEN coalesce(NEW.credential_id, gen_random_uuid())
                  ELSE coalesce(NEW.credential_id, OLD.credential_id, gen_random_uuid()) END;
    INSERT INTO public.attachments (
        attachment_id, owner_user_id, storage_key, original_name, mime_type,
        file_size_bytes, status, attachment_category, credential_type,
        credential_number, issuer, issued_date, expiry_date, review_status,
        review_note, reviewed_by, reviewed_at, file_url, file_id, created_at, updated_at
    ) VALUES (
        row_id, NEW.user_id,
        coalesce(nullif(NEW.file_url, ''), 'expert-credential/' || row_id::text),
        coalesce(nullif(NEW.credential_type, ''), 'expert-credential') || '.document',
        'application/octet-stream', 0, 'ACTIVE', 'EXPERT_CREDENTIAL',
        NEW.credential_type, NEW.credential_number, NEW.issuer, NEW.issued_date,
        NEW.expiry_date, coalesce(NEW.review_status, 'PENDING'), NEW.review_note,
        NEW.reviewed_by, NEW.reviewed_at, NEW.file_url, NEW.file_id,
        coalesce(NEW.created_at, now()), coalesce(NEW.updated_at, now())
    ) ON CONFLICT (attachment_id) DO UPDATE
        SET owner_user_id = excluded.owner_user_id,
            credential_type = excluded.credential_type,
            credential_number = excluded.credential_number,
            issuer = excluded.issuer,
            issued_date = excluded.issued_date,
            expiry_date = excluded.expiry_date,
            review_status = excluded.review_status,
            review_note = excluded.review_note,
            reviewed_by = excluded.reviewed_by,
            reviewed_at = excluded.reviewed_at,
            file_url = excluded.file_url,
            file_id = excluded.file_id,
            updated_at = excluded.updated_at;
    NEW.credential_id := row_id;
    RETURN NEW;
END
$function$;

CREATE OR REPLACE FUNCTION public.carebridge_guard_completed_triage_snapshot()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.status = 'COMPLETED' THEN
            RAISE EXCEPTION 'completed triage snapshot is immutable';
        END IF;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.status = 'COMPLETED' THEN
        IF (to_jsonb(NEW) - ARRAY[
                'continuation_acknowledged_at','updated_at',
                'symptom_list','duration_days','intensity','emergency_flag',
                'extracted_at','structured_created_by'])
                IS DISTINCT FROM
           (to_jsonb(OLD) - ARRAY[
                'continuation_acknowledged_at','updated_at',
                'symptom_list','duration_days','intensity','emergency_flag',
                'extracted_at','structured_created_by']) THEN
            RAISE EXCEPTION 'completed triage snapshot is immutable';
        END IF;
        IF NEW.continuation_acknowledged_at IS DISTINCT FROM
               OLD.continuation_acknowledged_at
           AND NOT (
               OLD.continuation_acknowledged_at IS NULL
               AND NEW.continuation_acknowledged_at IS NOT NULL) THEN
            RAISE EXCEPTION 'completed triage acknowledgement is one-way';
        END IF;
    END IF;

    IF NEW.status = 'COMPLETED' THEN
        NEW.result_jsonb := coalesce(NEW.result_jsonb, '{}'::jsonb);
        NEW.schema_version := coalesce(nullif(NEW.schema_version, ''), '1');
        NEW.content_hash := coalesce(
            nullif(NEW.content_hash, ''), md5(NEW.result_jsonb::text));
        IF NEW.risk_level = 'RED' THEN
            NEW.emergency := true;
        END IF;
    END IF;
    RETURN NEW;
END;
$function$;

CREATE OR REPLACE FUNCTION public.carebridge_reject_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF TG_TABLE_NAME = 'audit_events'
       AND TG_OP = 'DELETE'
       AND current_user = 'carebridge_checklist_retention_owner'
       AND OLD.event_category LIKE 'CHECKLIST\_%' ESCAPE '\'
       AND COALESCE(OLD.legal_hold, false) = false
       AND OLD.created_at < clock_timestamp() - interval '7 years' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'IMMUTABLE_TABLE: %.% does not allow UPDATE or DELETE',
        TG_TABLE_SCHEMA, TG_TABLE_NAME;
END
$function$;

CREATE OR REPLACE FUNCTION public.carebridge_validate_appointment_rules(p_rules jsonb)
 RETURNS boolean
 LANGUAGE sql
 IMMUTABLE PARALLEL SAFE STRICT
AS $function$
    SELECT
        jsonb_typeof(p_rules) = 'array'
        -- Same ceiling the application applies (MAX_RULES).
        AND jsonb_array_length(p_rules) <= 10
        AND NOT EXISTS (
            SELECT 1
            FROM jsonb_array_elements(p_rules) AS element
            WHERE jsonb_typeof(element) <> 'object'
               OR (element -> 'offsetMinutes') IS NULL
               -- Closed schema: offsetMinutes and nothing else.
               OR (SELECT count(*) FROM jsonb_object_keys(element)) <> 1
               OR jsonb_typeof(element -> 'offsetMinutes') <> 'number'
               -- Integer, not 30.5: a fractional offset has no meaning in minutes.
               OR (element ->> 'offsetMinutes') !~ '^-?[0-9]+$'
               OR ((element ->> 'offsetMinutes')::bigint < -43200)
               OR ((element ->> 'offsetMinutes')::bigint > 10080)
        )
        -- No duplicate offset: two identical offsets would produce two identical
        -- jobs for one occurrence, which the partial unique index would then reject.
        AND (
            SELECT count(DISTINCT element ->> 'offsetMinutes')
            FROM jsonb_array_elements(p_rules) AS element
        ) = jsonb_array_length(p_rules)
$function$;

CREATE OR REPLACE FUNCTION public.carebridge_validate_expert_request_conversation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF NEW.direct_conversation_id IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
             FROM public.direct_conversations conversation
            WHERE conversation.conversation_id = NEW.direct_conversation_id) THEN
        RAISE EXCEPTION 'STORY68_DIRECT_CONVERSATION_SOURCE_MISMATCH';
    END IF;
    RETURN NEW;
END
$function$;

CREATE OR REPLACE FUNCTION public.carebridge_validate_reminder_local_times(p_times time without time zone[])
 RETURNS boolean
 LANGUAGE sql
 IMMUTABLE PARALLEL SAFE STRICT
AS $function$
    SELECT
        -- No NULL element: a reminder with an unknown time cannot be scheduled.
        array_position(p_times, NULL) IS NULL
        -- No duplicate: the source table enforced this with a UNIQUE constraint,
        -- and a duplicate would materialise two identical jobs per occurrence.
        AND cardinality(p_times) = (
            SELECT count(DISTINCT t) FROM unnest(p_times) AS t
        )
        -- Same ceiling the application applies (MAX_TIMES).
        AND cardinality(p_times) <= 96
$function$;

CREATE OR REPLACE FUNCTION public.checklist_action_command_retention_guard()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF current_user = 'carebridge_checklist_retention_owner'
       AND OLD.legal_hold = false
       AND OLD.created_at < clock_timestamp() - interval '7 years'
       AND OLD.retain_until <= clock_timestamp()
       AND (
           (OLD.task_kind = 'CHECKLIST' AND EXISTS (
               SELECT 1 FROM public.checklist_task_instances task
               WHERE task.checklist_task_instance_id = OLD.task_id
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
           ))
           OR (OLD.task_kind = 'CARE_TASK' AND EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.task_id
                 AND task.status IN ('DONE', 'CANCELLED')
           ))
           OR (OLD.task_kind = 'REMINDER' AND EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.reminder_definition_id
                 AND task.task_type = 'SCHEDULED_REMINDER'
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
           ))
           OR (OLD.task_kind = 'CHECKLIST_SEQUENCE' AND EXISTS (
               SELECT 1 FROM public.checklist_instances instance
               WHERE instance.checklist_instance_id = OLD.task_id
                 AND instance.historical_at IS NOT NULL
           ))
           OR (OLD.task_kind = 'CHECKLIST' AND NOT EXISTS (
               SELECT 1 FROM public.checklist_task_instances task
               WHERE task.checklist_task_instance_id = OLD.task_id
           ))
           OR (OLD.task_kind = 'CARE_TASK' AND NOT EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.task_id
           ))
           OR (OLD.task_kind = 'REMINDER' AND NOT EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.reminder_definition_id
           ))
           OR (OLD.task_kind = 'CHECKLIST_SEQUENCE' AND NOT EXISTS (
               SELECT 1 FROM public.checklist_instances instance
               WHERE instance.checklist_instance_id = OLD.task_id
           ))
       ) THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION
        'RETENTION_DELETE_NOT_AUTHORIZED: checklist action command is not eligible or caller is not the retention owner'
        USING ERRCODE = '42501';
END
$function$;

CREATE OR REPLACE FUNCTION public.checklist_assert_access_timeline_audit()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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

    IF TG_TABLE_NAME = 'audit_events' THEN
        IF v_member_marker IS NOT NULL
           AND NOT (v_member_marker = 'FAMILY_MEMBER_DUPLICATE'
                    AND NEW.event_category = 'CHECKLIST_ACCESS_REVOKED') THEN
            RAISE EXCEPTION 'CHECKLIST_ACCESS_AUDIT_ON_QUARANTINED_MEMBER';
        END IF;
        -- Quarantined duplicate revocations are a migration-only exception:
        -- the paired audit is retained as forensic evidence.
        IF v_member_marker = 'FAMILY_MEMBER_DUPLICATE' THEN
            RETURN NEW;
        END IF;
    END IF;

    IF NOT public.checklist_p2_access_timeline_valid(v_member_id, v_timeline) THEN
        RAISE EXCEPTION 'CHECKLIST_ACCESS_TIMELINE_AUDIT_MISMATCH';
    END IF;

    RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_item_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_template_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE
    allowed_migration_review boolean;
    allowed_review_invalidation boolean;
    allowed_review_activation boolean;
    allowed_review_archive boolean;
    reviewed_content_unchanged boolean;
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.entry_type = 'TEMPLATE_ROOT'
           AND OLD.content_status IN ('APPROVED', 'ARCHIVED') THEN
            RAISE EXCEPTION 'VERSION_IMMUTABLE';
        END IF;
        RETURN OLD;
    END IF;

    allowed_migration_review := OLD.entry_type = 'TEMPLATE_ROOT'
        AND OLD.content_status = 'APPROVED'
        AND OLD.migration_review_required = true
        AND OLD.distribution_enabled = false
        AND NEW.content_status = 'PENDING_REVIEW'
        AND NEW.migration_review_required = false
        AND NEW.migration_reviewed_at IS NOT NULL
        AND NEW.migration_reviewed_by IS NOT NULL
        AND NEW.distribution_enabled = false
        AND NEW.approved_at IS NULL
        AND NEW.approved_by IS NULL;

    allowed_review_invalidation := OLD.entry_type = 'TEMPLATE_ROOT'
        AND OLD.migration_reviewed_at IS NOT NULL
        AND NEW.content_status IN ('DRAFT', 'PENDING_REVIEW')
        AND NEW.migration_review_required = true
        AND NEW.migration_reviewed_at IS NULL
        AND NEW.migration_reviewed_by IS NULL
        AND NEW.distribution_enabled = false
        AND NEW.approved_at IS NULL
        AND NEW.approved_by IS NULL;

    reviewed_content_unchanged :=
        NEW.title IS NOT DISTINCT FROM OLD.title
        AND NEW.description IS NOT DISTINCT FROM OLD.description
        AND NEW.stage IS NOT DISTINCT FROM OLD.stage
        AND NEW.substage_id IS NOT DISTINCT FROM OLD.substage_id
        AND NEW.template_type IS NOT DISTINCT FROM OLD.template_type
        AND NEW.template_lineage_id IS NOT DISTINCT FROM OLD.template_lineage_id
        AND NEW.template_version_id IS NOT DISTINCT FROM OLD.template_version_id
        AND NEW.version IS NOT DISTINCT FROM OLD.version
        AND NEW.entry_type IS NOT DISTINCT FROM OLD.entry_type
        AND NEW.author_user_id IS NOT DISTINCT FROM OLD.author_user_id;

    allowed_review_activation := OLD.entry_type = 'TEMPLATE_ROOT'
        AND OLD.migration_reviewed_at IS NOT NULL
        AND reviewed_content_unchanged
        AND OLD.content_status = 'PENDING_REVIEW'
        AND NEW.content_status = 'APPROVED'
        AND NEW.migration_review_required = false
        AND NEW.migration_reviewed_at IS NOT DISTINCT FROM OLD.migration_reviewed_at
        AND NEW.migration_reviewed_by IS NOT DISTINCT FROM OLD.migration_reviewed_by
        AND NEW.distribution_enabled = (NEW.template_type = 'MANDATORY')
        AND NEW.approved_at IS NOT NULL
        AND NEW.approved_by IS NOT NULL;

    allowed_review_archive := OLD.entry_type = 'TEMPLATE_ROOT'
        AND OLD.migration_reviewed_at IS NOT NULL
        AND reviewed_content_unchanged
        AND NEW.content_status = 'ARCHIVED'
        AND NEW.migration_review_required IS NOT DISTINCT FROM OLD.migration_review_required
        AND NEW.migration_reviewed_at IS NOT DISTINCT FROM OLD.migration_reviewed_at
        AND NEW.migration_reviewed_by IS NOT DISTINCT FROM OLD.migration_reviewed_by
        AND NEW.distribution_enabled = false
        AND NEW.approved_at IS NOT DISTINCT FROM OLD.approved_at
        AND NEW.approved_by IS NOT DISTINCT FROM OLD.approved_by;

    IF OLD.entry_type = 'TEMPLATE_ROOT'
       AND OLD.migration_reviewed_at IS NOT NULL
       AND (
           NEW.title IS DISTINCT FROM OLD.title OR
           NEW.description IS DISTINCT FROM OLD.description OR
           NEW.stage IS DISTINCT FROM OLD.stage OR
           NEW.substage_id IS DISTINCT FROM OLD.substage_id OR
           NEW.template_type IS DISTINCT FROM OLD.template_type OR
           NEW.template_lineage_id IS DISTINCT FROM OLD.template_lineage_id OR
           NEW.template_version_id IS DISTINCT FROM OLD.template_version_id OR
           NEW.version IS DISTINCT FROM OLD.version OR
           NEW.entry_type IS DISTINCT FROM OLD.entry_type OR
           NEW.author_user_id IS DISTINCT FROM OLD.author_user_id OR
           NEW.content_status IS DISTINCT FROM OLD.content_status OR
           NEW.migration_review_required IS DISTINCT FROM OLD.migration_review_required OR
           NEW.migration_reviewed_at IS DISTINCT FROM OLD.migration_reviewed_at OR
           NEW.migration_reviewed_by IS DISTINCT FROM OLD.migration_reviewed_by OR
           NEW.distribution_enabled IS DISTINCT FROM OLD.distribution_enabled OR
           NEW.approved_at IS DISTINCT FROM OLD.approved_at OR
           NEW.approved_by IS DISTINCT FROM OLD.approved_by
       )
       AND NOT allowed_review_invalidation
       AND NOT allowed_review_activation
       AND NOT allowed_review_archive THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF OLD.entry_type = 'TEMPLATE_ROOT'
       AND OLD.content_status IN ('APPROVED', 'ARCHIVED')
       AND (
           NEW.title IS DISTINCT FROM OLD.title OR
           NEW.description IS DISTINCT FROM OLD.description OR
           NEW.stage IS DISTINCT FROM OLD.stage OR
           NEW.substage_id IS DISTINCT FROM OLD.substage_id OR
           NEW.template_type IS DISTINCT FROM OLD.template_type OR
           NEW.template_lineage_id IS DISTINCT FROM OLD.template_lineage_id OR
           NEW.template_version_id IS DISTINCT FROM OLD.template_version_id OR
           NEW.version IS DISTINCT FROM OLD.version OR
           NEW.entry_type IS DISTINCT FROM OLD.entry_type OR
           NEW.author_user_id IS DISTINCT FROM OLD.author_user_id OR
           (NOT allowed_migration_review AND (
               NEW.migration_review_required IS DISTINCT FROM OLD.migration_review_required OR
               NEW.migration_reviewed_at IS DISTINCT FROM OLD.migration_reviewed_at OR
               NEW.migration_reviewed_by IS DISTINCT FROM OLD.migration_reviewed_by OR
               (OLD.content_status = 'APPROVED'
                   AND NEW.content_status NOT IN ('APPROVED', 'ARCHIVED')) OR
               (OLD.content_status = 'ARCHIVED'
                   AND NEW.content_status <> 'ARCHIVED') OR
               (OLD.content_status = 'APPROVED'
                   AND NEW.distribution_enabled IS DISTINCT FROM OLD.distribution_enabled
                   AND NOT (NEW.content_status = 'ARCHIVED' AND NEW.distribution_enabled = false)) OR
               (OLD.content_status = 'ARCHIVED' AND NEW.distribution_enabled = true) OR
               NEW.approved_at IS DISTINCT FROM OLD.approved_at OR
               NEW.approved_by IS DISTINCT FROM OLD.approved_by
           ))
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_guard_preconception_requiredness()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE
    expected_required boolean;
BEGIN
    SELECT item.is_required
      INTO expected_required
      FROM public.checklist_instances parent
      JOIN public.care_item_templates item
        ON item.template_id = NEW.template_item_version_id
       AND item.entry_type = 'CHECKLIST_ENTRY'
      JOIN public.care_item_templates root
        ON root.template_id = item.parent_template_id
       AND root.entry_type = 'TEMPLATE_ROOT'
     WHERE parent.checklist_instance_id = NEW.checklist_instance_id
       AND NEW.template_version_id = root.template_version_id
       AND item.is_required IS NOT NULL
       AND NEW.checklist_quarantine_reason_code IS NULL
       AND parent.checklist_quarantine_reason_code IS NULL
       AND item.checklist_quarantine_reason_code IS NULL
       AND root.checklist_quarantine_reason_code IS NULL
       AND parent.historical_at IS NULL
       AND parent.status <> 'CANCELLED'
       AND parent.origin = 'SYSTEM_TEMPLATE'
       AND parent.recipient_role = 'MOTHER'
       AND parent.care_context_type = 'JOURNEY'
       AND root.stage = 'PRE_PREGNANCY'
       AND root.template_type = 'MANDATORY'
       AND root.recipient_scope = 'MOTHER'
       AND root.display_order > 0;
    IF FOUND AND NEW.is_required IS DISTINCT FROM expected_required THEN
        RAISE EXCEPTION
            'CHECKLIST_PRECONCEPTION_REQUIREDNESS_MISMATCH: expected=% actual=%',
            expected_required, NEW.is_required;
    END IF;
    RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_guard_sequence_position_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF OLD.entry_type = 'TEMPLATE_ROOT'
       AND NEW.display_order IS DISTINCT FROM OLD.display_order
       AND (
           OLD.content_status IN ('APPROVED', 'ARCHIVED')
           OR EXISTS (
               SELECT 1 FROM public.checklist_instances instance
               WHERE instance.template_lineage_id = OLD.template_lineage_id
           )
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_guard_template_type_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF OLD.entry_type = 'TEMPLATE_ROOT'
       AND NEW.template_type IS DISTINCT FROM OLD.template_type THEN
        IF OLD.content_status IN ('APPROVED', 'ARCHIVED') THEN
            RAISE EXCEPTION 'VERSION_IMMUTABLE';
        END IF;
        IF OLD.migration_reviewed_at IS NOT NULL THEN
            RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
        END IF;
    END IF;
    RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_p2_access_audit(p_member_id uuid, p_event_type text, p_reason_code text, p_before jsonb, p_after jsonb, p_correlation uuid, p_recorded_at timestamp with time zone)
 RETURNS void
 LANGUAGE plpgsql
AS $function$
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
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_p2_access_timeline_valid(p_member_id uuid, p_timeline jsonb)
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_catalog'
AS $function$
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
$function$;

CREATE OR REPLACE FUNCTION public.checklist_p2_deterministic_uuid(p_key text)
 RETURNS uuid
 LANGUAGE plpgsql
 IMMUTABLE
AS $function$
DECLARE
    h text := md5(coalesce(p_key, ''));
BEGIN
    RETURN (substr(h, 1, 8) || '-' || substr(h, 9, 4) || '-5' || substr(h, 14, 3)
        || '-' || substr('89ab', (get_byte(convert_to(h, 'UTF8'), 0) % 4) + 1, 1)
        || substr(h, 18, 3) || '-' || substr(h, 21, 12))::uuid;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_p2_quarantine_audit(p_resource_type text, p_resource_id uuid, p_reason_code text, p_source_kind text, p_recorded_at timestamp with time zone)
 RETURNS uuid
 LANGUAGE plpgsql
AS $function$
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
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_purge_retained_records(p_actor_user_id uuid)
 RETURNS TABLE(audit_events_purged bigint, quarantines_purged bigint, action_commands_purged bigint)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public'
AS $function$
            DECLARE
                retirement_marker constant text := 'CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1';
            BEGIN
                IF session_user <> 'checklist_operations' THEN
                    RAISE EXCEPTION 'PURGE_DATABASE_CALLER_NOT_TRUSTED' USING ERRCODE = '42501';
                END IF;
                IF EXISTS (
                    SELECT 1 FROM pg_catalog.pg_auth_members membership
                    JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = membership.roleid
                    WHERE owner_role.rolname = 'carebridge_checklist_retention_owner'
                      AND (membership.inherit_option OR membership.set_option)) THEN
                    RAISE EXCEPTION 'PURGE_OWNER_ROLE_REACHABLE' USING ERRCODE = '42501';
                END IF;
                IF NOT EXISTS (
                    SELECT 1 FROM public.users actor
                    WHERE actor.user_id = p_actor_user_id
                      AND actor.role IN ('SYSTEM_ADMIN', 'OPERATIONS')
                      AND actor.account_status = 'ACTIVE' AND actor.enabled AND NOT actor.locked) THEN
                    RAISE EXCEPTION 'PURGE_ACTOR_NOT_TRUSTED' USING ERRCODE = '42501';
                END IF;

                WITH deleted AS (
                    DELETE FROM public.audit_events event
                    WHERE event.event_category LIKE 'CHECKLIST\_%' ESCAPE '\'
                      AND event.created_at < clock_timestamp() - interval '7 years'
                      AND event.legal_hold = false RETURNING 1)
                SELECT count(*) INTO audit_events_purged FROM deleted;
                quarantines_purged := 0;

                WITH deleted AS (
                    DELETE FROM public.checklist_action_commands command
                    WHERE command.created_at < clock_timestamp() - interval '7 years'
                      AND command.retain_until <= clock_timestamp()
                      AND command.legal_hold = false
                      AND (
                          (command.task_kind = 'CHECKLIST' AND EXISTS (
                              SELECT 1 FROM public.checklist_task_instances task
                              WHERE task.checklist_task_instance_id = command.task_id
                                AND task.status IN ('COMPLETED','SKIPPED','CANCELLED')))
                          OR (command.task_kind = 'CARE_TASK' AND EXISTS (
                              SELECT 1 FROM public.care_tasks task
                              WHERE task.task_id = command.task_id
                                AND task.status IN ('DONE','CANCELLED')))
                          OR (command.task_kind = 'REMINDER' AND EXISTS (
                              SELECT 1 FROM public.care_tasks task
                              WHERE task.task_id = command.reminder_definition_id
                                AND task.task_type = 'SCHEDULED_REMINDER'
                                AND task.status IN ('COMPLETED','SKIPPED','CANCELLED')))
                          OR (command.task_kind = 'CHECKLIST' AND NOT EXISTS (
                              SELECT 1 FROM public.checklist_task_instances task
                              WHERE task.checklist_task_instance_id = command.task_id))
                          OR (command.task_kind = 'CARE_TASK' AND NOT EXISTS (
                              SELECT 1 FROM public.care_tasks task WHERE task.task_id = command.task_id))
                          OR (command.task_kind = 'REMINDER' AND NOT EXISTS (
                              SELECT 1 FROM public.care_tasks task
                              WHERE task.task_id = command.reminder_definition_id)))
                    RETURNING 1)
                SELECT count(*) INTO action_commands_purged FROM deleted;

                INSERT INTO public.audit_events (
                    actor_user_id, actor_type, event_category, resource_type, reason_code,
                    correlation_id, after_payload_jsonb, occurred_at, created_at, event_origin, legal_hold)
                VALUES (
                    p_actor_user_id, 'USER', 'CHECKLIST_RETENTION_PURGED', 'ChecklistRetention',
                    'SEVEN_YEAR_RETENTION', gen_random_uuid(),
                    jsonb_build_object('auditEventsPurged', audit_events_purged,
                                       'quarantinesPurged', 0,
                                       'actionCommandsPurged', action_commands_purged,
                                       'retirementMarker', retirement_marker),
                    clock_timestamp(), clock_timestamp(), 'AUDIT_LOG', false);
                RETURN QUERY SELECT audit_events_purged, quarantines_purged, action_commands_purged;
            END
            $function$;

CREATE OR REPLACE FUNCTION public.checklist_v1_writer_barrier()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_validate_action_command_target()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF NEW.task_kind = 'CHECKLIST' THEN
        PERFORM 1 FROM public.checklist_task_instances task
        WHERE task.checklist_task_instance_id = NEW.task_id;
    ELSIF NEW.task_kind = 'REMINDER' THEN
        PERFORM 1 FROM public.care_tasks task
        WHERE task.task_id = COALESCE(NEW.reminder_definition_id, NEW.task_id)
          AND task.task_type = 'SCHEDULED_REMINDER';
    ELSIF NEW.task_kind = 'CHECKLIST_SEQUENCE' THEN
        PERFORM 1 FROM public.checklist_instances instance
        WHERE instance.checklist_instance_id = NEW.task_id;
    ELSE
        PERFORM 1 FROM public.care_tasks task WHERE task.task_id = NEW.task_id;
    END IF;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_ACTION_TARGET_NOT_FOUND';
    END IF;
    RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_validate_inline_template_shape()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' THEN
        IF NEW.recipient_scope IS NOT NULL OR NEW.eligibility_anchor_type IS NOT NULL
           OR NEW.eligibility_range_unit IS NOT NULL
           OR NEW.eligibility_start_inclusive IS NOT NULL
           OR NEW.eligibility_end_inclusive IS NOT NULL THEN
            RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
        END IF;
        RETURN NULL;
    END IF;
    IF NEW.recipient_scope IS NULL THEN RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED'; END IF;
    IF NEW.recipient_scope = 'FAMILY' THEN
        IF NEW.stage IS NOT NULL OR NEW.eligibility_anchor_type IS NOT NULL
           OR NEW.eligibility_range_unit IS NOT NULL
           OR NEW.eligibility_start_inclusive IS NOT NULL
           OR NEW.eligibility_end_inclusive IS NOT NULL THEN
            RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
        END IF;
        RETURN NULL;
    END IF;
    IF NEW.stage IS NULL OR NEW.eligibility_anchor_type IS NULL
       OR NEW.eligibility_range_unit IS NULL OR NEW.eligibility_start_inclusive IS NULL
       OR NEW.eligibility_end_inclusive IS NULL
       OR NEW.eligibility_start_inclusive < 0
       OR NEW.eligibility_end_inclusive < NEW.eligibility_start_inclusive
       OR NOT (
           (NEW.stage = 'PRE_PREGNANCY' AND NEW.eligibility_anchor_type = 'NONE'
            AND NEW.eligibility_range_unit = 'DAY'
            AND NEW.eligibility_start_inclusive = 0 AND NEW.eligibility_end_inclusive = 0)
           OR (NEW.migration_review_required AND NEW.eligibility_anchor_type = 'NONE'
               AND NEW.eligibility_range_unit = 'DAY'
               AND NEW.eligibility_start_inclusive = 0
               AND NEW.eligibility_end_inclusive = 2147483647)
           OR (NEW.stage = 'PREGNANCY' AND NEW.eligibility_anchor_type IN ('LMP','EDD'))
           OR (NEW.stage = 'POSTPARTUM'
               AND NEW.eligibility_anchor_type IN ('DELIVERY_DATE','BIRTH_DATE'))
           OR (NEW.stage = 'BABY_CARE' AND NEW.eligibility_anchor_type = 'BIRTH_DATE')) THEN
        RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
    END IF;
    RETURN NULL;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_validate_instance_contract_match()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_validate_instance_recipient()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_validate_postpartum_leaf_timing()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE parent_stage varchar(30); parent_anchor varchar(30);
BEGIN
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    IF NEW.entry_type = 'CHECKLIST_ENTRY' THEN
        SELECT stage, eligibility_anchor_type INTO parent_stage, parent_anchor
          FROM public.care_item_templates WHERE template_id = NEW.parent_template_id;
        IF NEW.is_active AND parent_stage = 'POSTPARTUM' AND NOT (
               (NEW.checklist_contract_version = 2
                AND NEW.target_subject IS NULL AND NEW.due_anchor_type = 'DELIVERY_DATE')
            OR (coalesce(NEW.checklist_contract_version, 1) = 1
                AND NEW.target_subject = 'MOTHER'
                AND (NEW.due_anchor_type IS NULL OR NEW.due_anchor_type = 'DELIVERY_DATE')))
        THEN RAISE EXCEPTION 'POSTPARTUM_LEAF_TARGET_DUE_ANCHOR_MISMATCH'; END IF;
        IF NEW.is_active AND parent_stage = 'BABY_CARE' AND NOT (
               (NEW.checklist_contract_version = 2
                AND NEW.target_subject IS NULL AND NEW.due_anchor_type = 'BIRTH_DATE')
            OR (coalesce(NEW.checklist_contract_version, 1) = 1
                AND NEW.target_subject = 'BABY'
                AND (NEW.due_anchor_type IS NULL OR NEW.due_anchor_type = 'BIRTH_DATE')))
        THEN RAISE EXCEPTION 'BABY_CARE_LEAF_TARGET_DUE_ANCHOR_MISMATCH'; END IF;
    ELSIF NEW.entry_type = 'TEMPLATE_ROOT' THEN
        IF NEW.content_status <> 'ARCHIVED' AND NEW.distribution_enabled
           AND ((NEW.stage = 'POSTPARTUM'
                 AND NEW.eligibility_anchor_type <> 'DELIVERY_DATE')
             OR (NEW.stage = 'BABY_CARE'
                 AND NEW.eligibility_anchor_type <> 'BIRTH_DATE')) THEN
            RAISE EXCEPTION 'CHECKLIST_STAGE_ROOT_ANCHOR_MISMATCH';
        END IF;
        -- Only enforce leaf contract mismatch for stages that have per-leaf
        -- anchor requirements (POSTPARTUM and BABY_CARE). PREGNANCY and
        -- PRE_PREGNANCY items do not carry due anchors, so the guard must be
        -- skipped for those stages entirely.
        IF NEW.content_status <> 'ARCHIVED' AND NEW.distribution_enabled
           AND NEW.stage IN ('POSTPARTUM', 'BABY_CARE')
           AND EXISTS (
           SELECT 1 FROM public.care_item_templates item
            WHERE item.parent_template_id = NEW.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY' AND item.is_active
              AND NOT (
                  (NEW.stage = 'POSTPARTUM'
                   AND NEW.eligibility_anchor_type = 'DELIVERY_DATE'
                   AND ((item.checklist_contract_version = 2
                         AND item.target_subject IS NULL
                         AND item.due_anchor_type = 'DELIVERY_DATE')
                     OR (coalesce(item.checklist_contract_version, 1) = 1
                         AND item.target_subject = 'MOTHER'
                         AND (item.due_anchor_type IS NULL
                              OR item.due_anchor_type = 'DELIVERY_DATE'))))
                  OR (NEW.stage = 'BABY_CARE'
                      AND NEW.eligibility_anchor_type = 'BIRTH_DATE'
                      AND ((item.checklist_contract_version = 2
                            AND item.target_subject IS NULL
                            AND item.due_anchor_type = 'BIRTH_DATE')
                        OR (coalesce(item.checklist_contract_version, 1) = 1
                            AND item.target_subject = 'BABY'
                            AND (item.due_anchor_type IS NULL
                                 OR item.due_anchor_type = 'BIRTH_DATE')))))) THEN
            RAISE EXCEPTION 'CHECKLIST_STAGE_LEAF_CONTRACT_MISMATCH';
        END IF;
    END IF;
    RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_validate_task_contract_match()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_validate_task_template()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE
    parent_origin varchar(20);
    parent_version_id uuid;
    root_id uuid;
    item_parent_id uuid;
BEGIN
    SELECT parent.origin, parent.template_version_id
      INTO parent_origin, parent_version_id
      FROM public.checklist_instances parent
     WHERE parent.checklist_instance_id = NEW.checklist_instance_id
     FOR KEY SHARE;
    IF parent_origin = 'SYSTEM_TEMPLATE' THEN
        SELECT root.template_id INTO root_id
          FROM public.care_item_templates root
         WHERE root.template_version_id = NEW.template_version_id
           AND root.entry_type = 'TEMPLATE_ROOT';
        SELECT item.parent_template_id INTO item_parent_id
          FROM public.care_item_templates item
         WHERE item.template_id = NEW.template_item_version_id
           AND item.entry_type = 'CHECKLIST_ENTRY';
        IF NEW.template_version_id IS NULL OR NEW.template_item_version_id IS NULL
           OR NEW.template_version_id IS DISTINCT FROM parent_version_id
           OR root_id IS NULL OR item_parent_id IS DISTINCT FROM root_id THEN
            RAISE EXCEPTION 'CHECKLIST_SYSTEM_TASK_TEMPLATE_REQUIRED';
        END IF;
    ELSIF NEW.template_version_id IS NOT NULL OR NEW.template_item_version_id IS NOT NULL THEN
        RAISE EXCEPTION 'CHECKLIST_USER_TASK_TEMPLATE_FORBIDDEN';
    END IF;
    RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_validate_template_approval()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
END $function$;

CREATE OR REPLACE FUNCTION public.checklist_validate_v2_requiredness()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT'
       OR (NOT NEW.distribution_enabled AND NEW.content_status <> 'APPROVED') THEN
        RETURN NEW;
    END IF;
    IF EXISTS (
        SELECT 1
          FROM public.care_item_templates item
         WHERE item.parent_template_id = NEW.template_id
           AND item.entry_type = 'CHECKLIST_ENTRY'
           AND item.is_active
           AND coalesce(item.checklist_contract_version, 1) = 2
           AND item.is_required IS NULL
           AND item.checklist_quarantine_reason_code IS NULL) THEN
        RAISE EXCEPTION 'ITEM_REQUIREDNESS_REQUIRED';
    END IF;
    RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.enforce_community_topic_parent_category()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF NEW.type = 'TOPIC'
       AND NOT EXISTS (
           SELECT 1
           FROM community_topics AS parent
           WHERE parent.id = NEW.parent_id
             AND parent.type = 'CATEGORY'
       ) THEN
        RAISE EXCEPTION 'TOPIC parent_id must reference a CATEGORY'
            USING ERRCODE = '23514', CONSTRAINT = 'community_topics_parent_category_check';
    END IF;

    IF TG_OP = 'UPDATE'
       AND OLD.type = 'CATEGORY'
       AND NEW.type <> 'CATEGORY'
       AND EXISTS (
           SELECT 1
           FROM community_topics AS child
           WHERE child.parent_id = NEW.id
       ) THEN
        RAISE EXCEPTION 'A referenced CATEGORY cannot change type'
            USING ERRCODE = '23514', CONSTRAINT = 'community_topics_parent_category_check';
    END IF;

    RETURN NEW;
END;
$function$;

CREATE OR REPLACE FUNCTION public.reject_consultation_context_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END
$function$;

CREATE OR REPLACE FUNCTION public.reminder_occurrence_id_v1(p_reminder_definition_id uuid, p_scheduled_at timestamp with time zone)
 RETURNS uuid
 LANGUAGE plpgsql
 IMMUTABLE STRICT
 SET search_path TO 'pg_catalog', 'public'
AS $function$
DECLARE
    v_micros integer;
    v_instant text;
    v_payload text;
    v_digest bytea;
BEGIN
    v_micros := floor(extract(microseconds FROM p_scheduled_at))::integer % 1000000;
    v_instant := to_char(
        p_scheduled_at AT TIME ZONE 'UTC',
        'YYYY-MM-DD"T"HH24:MI:SS');
    IF v_micros = 0 THEN
        v_instant := v_instant || 'Z';
    ELSIF v_micros % 1000 = 0 THEN
        v_instant := v_instant || '.' || lpad((v_micros / 1000)::text, 3, '0') || 'Z';
    ELSE
        v_instant := v_instant || '.' || lpad(v_micros::text, 6, '0') || 'Z';
    END IF;

    v_payload := 'reminder-occurrence-v1|'
        || lower(p_reminder_definition_id::text) || '|' || v_instant;
    -- The canonical input is ASCII-only, so PostgreSQL md5(text) hashes the
    -- exact same bytes as Java UUID.nameUUIDFromBytes(... UTF_8).
    v_digest := decode(md5(v_payload), 'hex');
    v_digest := set_byte(v_digest, 6, (get_byte(v_digest, 6) & 15) | 48);
    v_digest := set_byte(v_digest, 8, (get_byte(v_digest, 8) & 63) | 128);
    RETURN encode(v_digest, 'hex')::uuid;
END
$function$;

CREATE OR REPLACE FUNCTION public.reminder_occurrence_id_v2(p_reminder_definition_id uuid, p_scheduled_at timestamp with time zone, p_occurrence_generation bigint)
 RETURNS uuid
 LANGUAGE plpgsql
 IMMUTABLE STRICT
 SET search_path TO 'pg_catalog', 'public'
AS $function$
DECLARE
    v_micros integer;
    v_instant text;
    v_payload text;
    v_digest bytea;
BEGIN
    IF p_occurrence_generation = 0 THEN
        RETURN public.reminder_occurrence_id_v1(
            p_reminder_definition_id, p_scheduled_at);
    END IF;

    v_micros := floor(extract(microseconds FROM p_scheduled_at))::integer % 1000000;
    v_instant := to_char(
        p_scheduled_at AT TIME ZONE 'UTC',
        'YYYY-MM-DD"T"HH24:MI:SS');
    IF v_micros = 0 THEN
        v_instant := v_instant || 'Z';
    ELSIF v_micros % 1000 = 0 THEN
        v_instant := v_instant || '.' || lpad((v_micros / 1000)::text, 3, '0') || 'Z';
    ELSE
        v_instant := v_instant || '.' || lpad(v_micros::text, 6, '0') || 'Z';
    END IF;

    v_payload := 'reminder-occurrence-v2|'
        || lower(p_reminder_definition_id::text) || '|'
        || v_instant || '|' || p_occurrence_generation::text;
    v_digest := decode(md5(v_payload), 'hex');
    v_digest := set_byte(v_digest, 6, (get_byte(v_digest, 6) & 15) | 48);
    v_digest := set_byte(v_digest, 8, (get_byte(v_digest, 8) & 63) | 128);
    RETURN encode(v_digest, 'hex')::uuid;
END
$function$;

-- Standalone and serial sequences

-- Tables

CREATE TABLE public."account_lock_appeals" (
    "appeal_id" uuid DEFAULT gen_random_uuid (),
    "user_id" uuid,
    "lock_episode_id" uuid,
    "reason" character varying(1000),
    "status" character varying(30) DEFAULT 'PENDING'::character varying,
    "submitted_at" timestamp with time zone DEFAULT now(),
    "reviewed_by" uuid,
    "reviewed_at" timestamp with time zone,
    "review_note" character varying(1000)
);

CREATE TABLE public."administrative_areas" (
    "administrative_area_id" uuid DEFAULT gen_random_uuid (),
    "parent_area_id" uuid,
    "area_type" character varying(30),
    "code" character varying(80),
    "name" character varying(255),
    "legacy_code" character varying(80),
    "created_at" timestamp with time zone DEFAULT now(),
    "name_en" character varying(255)
);

CREATE TABLE public."ai_content_assessments" (
    "assessment_id" uuid DEFAULT gen_random_uuid (),
    "job_id" uuid,
    "target_type" character varying(20),
    "target_id" uuid,
    "content_hash" character varying(64),
    "policy_set_hash" character varying(64),
    "provider" character varying(30) DEFAULT 'GEMINI'::character varying,
    "model" character varying(60),
    "status" character varying(20),
    "classification" character varying(20),
    "overall_severity" character varying(20),
    "confidence" numeric(4, 3),
    "recommended_action" character varying(30),
    "explanation" character varying(1000),
    "error_code" character varying(80),
    "attempt_count" integer DEFAULT 1,
    "latency_ms" bigint,
    "prompt_tokens" integer,
    "output_tokens" integer,
    "moderation_case_id" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "completed_at" timestamp with time zone,
    "matches_jsonb" jsonb DEFAULT '[]'::jsonb
);

CREATE TABLE public."ai_content_scan_jobs" (
    "job_id" uuid DEFAULT gen_random_uuid (),
    "target_type" character varying(20),
    "target_id" uuid,
    "content_hash" character varying(64),
    "status" character varying(20) DEFAULT 'QUEUED'::character varying,
    "attempt_count" integer DEFAULT 0,
    "next_attempt_at" timestamp with time zone DEFAULT now(),
    "locked_by" character varying(100),
    "locked_at" timestamp with time zone,
    "last_error_code" character varying(80),
    "force_rescan" boolean DEFAULT false,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "completed_at" timestamp with time zone
);

CREATE TABLE public."ai_moderation_policies" (
    "policy_id" uuid DEFAULT gen_random_uuid (),
    "policy_code" character varying(60),
    "name" character varying(150),
    "detection_guidance" text,
    "violation_category" character varying(40),
    "report_category" character varying(40),
    "severity" character varying(20),
    "applicable_target_types" character varying(100),
    "confidence_threshold" numeric(4, 3) DEFAULT 0.700,
    "active" boolean DEFAULT true,
    "system_default" boolean DEFAULT false,
    "version" integer DEFAULT 1,
    "created_by" uuid,
    "updated_by" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "reference_links" text,
    "reference_files" text
);

CREATE TABLE public."appointment_notification_configs" (
    "reminder_id" uuid,
    "time_zone" character varying(80) DEFAULT 'Asia/Ho_Chi_Minh'::character varying,
    "config_revision" bigint DEFAULT 1,
    "created_at" timestamp with time zone DEFAULT clock_timestamp(),
    "updated_at" timestamp with time zone DEFAULT clock_timestamp(),
    "rules_jsonb" jsonb DEFAULT '[]'::jsonb
);

CREATE TABLE public."attachments" (
    "attachment_id" uuid DEFAULT gen_random_uuid (),
    "owner_user_id" uuid,
    "uploader_role" character varying(30) DEFAULT 'PATIENT'::character varying,
    "storage_key" character varying(500),
    "original_name" character varying(255),
    "mime_type" character varying(100),
    "file_size_bytes" bigint,
    "status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "checksum" character varying(64),
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "attachment_category" character varying(40) DEFAULT 'GENERAL'::character varying,
    "credential_type" character varying(50),
    "credential_number" character varying(100),
    "issuer" character varying(200),
    "issued_date" date,
    "expiry_date" date,
    "review_status" character varying(30),
    "review_note" text,
    "reviewed_by" uuid,
    "reviewed_at" timestamp without time zone,
    "file_url" text,
    "file_id" uuid,
    "health_record_id" uuid
);

CREATE TABLE public."audit_events" (
    "audit_event_id" uuid DEFAULT gen_random_uuid (),
    "actor_user_id" uuid,
    "event_category" character varying(80),
    "subject_user_id" uuid,
    "subject_reference_id" uuid,
    "resource_type" character varying(100),
    "resource_id" uuid,
    "purpose" character varying(255),
    "decision" character varying(50),
    "ip_hash" character varying(128),
    "before_payload_jsonb" jsonb,
    "after_payload_jsonb" jsonb,
    "checksum" character varying(128),
    "occurred_at" timestamp with time zone DEFAULT now(),
    "created_at" timestamp with time zone DEFAULT now(),
    "note_text" text,
    "event_origin" character varying(255) DEFAULT 'AUDIT_LOG'::character varying,
    "ip_address" character varying(80),
    "user_agent" character varying(500),
    "payload" jsonb,
    "correlation_id" uuid,
    "severity" character varying(20) DEFAULT 'MEDIUM'::character varying,
    "status" character varying(20) DEFAULT 'OPEN'::character varying,
    "reviewed_at" timestamp with time zone,
    "reviewed_by" uuid,
    "security_event_id" uuid,
    "actor_type" character varying(20),
    "actor_service" character varying(80),
    "reason_code" character varying(80),
    "care_context_type" character varying(10),
    "care_context_id" uuid,
    "template_version_id" uuid,
    "checklist_task_instance_id" uuid,
    "legal_hold" boolean DEFAULT false
);

CREATE TABLE public."auth_challenges" (
    "challenge_id" uuid DEFAULT gen_random_uuid (),
    "user_id" uuid,
    "challenge_type" character varying(40),
    "subject_identifier" character varying(255),
    "challenge_hash" character varying(255),
    "attempts" integer DEFAULT 0,
    "expires_at" timestamp with time zone,
    "used_at" timestamp with time zone,
    "status" character varying(30),
    "requested_role" character varying(40),
    "created_at" timestamp with time zone DEFAULT now(),
    "legacy_source" character varying(40),
    "legacy_id" character varying(100)
);

CREATE TABLE public."auth_sessions" (
    "session_id" uuid DEFAULT gen_random_uuid (),
    "user_id" uuid,
    "token_family_id" uuid,
    "device_identifier" character varying(255),
    "device_name" character varying(150),
    "refresh_token_hash" character varying(255),
    "issued_at" timestamp with time zone,
    "expires_at" timestamp with time zone,
    "last_used_at" timestamp with time zone,
    "rotated_at" timestamp with time zone,
    "revoked_at" timestamp with time zone,
    "revoke_reason" character varying(100),
    "status" character varying(20),
    "created_ip_hash" character varying(255),
    "user_agent_hash" character varying(255),
    "legacy_source" character varying(40),
    "legacy_id" character varying(100),
    "detected_reuse" boolean DEFAULT false,
    "revocation_metadata_jsonb" jsonb DEFAULT '{}'::jsonb
);

CREATE TABLE public."care_facilities" (
    "facility_id" uuid DEFAULT gen_random_uuid (),
    "name" character varying(255),
    "facility_type" character varying(50),
    "address" character varying(500),
    "latitude" numeric(10, 8),
    "longitude" numeric(11, 8),
    "phone" character varying(30),
    "opening_hours_json" jsonb,
    "source_type" character varying(30),
    "verification_status" character varying(30) DEFAULT 'UNVERIFIED'::character varying,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "facility_level" character varying(50),
    "ownership_type" character varying(30),
    "province_id" character varying(2),
    "district_id" character varying(4),
    "external_source_id" character varying(150),
    "is_active" boolean DEFAULT true,
    "is_searchable" boolean DEFAULT true,
    "administrative_area_id" uuid
);

CREATE TABLE public."care_group_members" (
    "care_group_member_id" uuid DEFAULT gen_random_uuid (),
    "care_group_id" uuid,
    "user_id" uuid,
    "member_role" character varying(50),
    "invitation_status" character varying(20) DEFAULT 'PENDING'::character varying,
    "permission_json" jsonb DEFAULT '{}'::jsonb,
    "joined_at" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "invite_token" character varying(64),
    "invite_channel" character varying(20),
    "invite_expires_at" timestamp with time zone,
    "invited_phone" character varying(255),
    "data_permission_id" uuid,
    "is_emergency_contact" boolean DEFAULT false,
    "emergency_contact_priority" smallint,
    "family_relationship_role" character varying(50),
    "custom_family_relationship_role" character varying(100),
    "checklist_access_timeline_jsonb" jsonb,
    "checklist_access_epoch" bigint,
    "checklist_access_quarantine_reason_code" character varying(80)
);

CREATE TABLE public."care_groups" (
    "care_group_id" uuid DEFAULT gen_random_uuid (),
    "owner_user_id" uuid,
    "journey_id" uuid,
    "baby_id" uuid,
    "group_name" character varying(200),
    "status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "description" character varying(500),
    "linked_journey_id" uuid,
    "linked_baby_profile_id" uuid,
    "care_subject_id" uuid,
    "linked_baby_subject_type" character varying(30) GENERATED ALWAYS AS (
        CASE
            WHEN (
                linked_baby_profile_id IS NULL
            ) THEN NULL::text
            ELSE 'BABY'::text
        END
    ) STORED
);

CREATE TABLE public."care_item_templates" (
    "template_id" uuid DEFAULT gen_random_uuid (),
    "parent_template_id" uuid,
    "entry_type" character varying(30),
    "title" character varying(500),
    "description" text,
    "display_order" integer DEFAULT 0,
    "stage" character varying(30),
    "is_active" boolean DEFAULT true,
    "version" integer DEFAULT 1,
    "effective_from" timestamp with time zone,
    "effective_to" timestamp with time zone,
    "configuration_jsonb" jsonb DEFAULT '{}'::jsonb,
    "configuration_hash" character varying(128),
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "created_by" uuid,
    "difficulty_level" character varying(30),
    "duration_minutes" smallint,
    "instruction_content" text,
    "media_url" text,
    "safety_warning" text,
    "supports_posture_analysis" boolean,
    "template_status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "configured_by" uuid,
    "analysis_mode" character varying(30),
    "rule_or_model_version" character varying(80),
    "confidence_threshold" numeric(38, 2),
    "feedback_level" character varying(30),
    "content_status" character varying(20) DEFAULT 'DRAFT'::character varying,
    "is_required" boolean,
    "author_user_id" uuid,
    "revision_reason" text,
    "revision_requested_at" timestamp with time zone,
    "revision_requested_by" uuid,
    "revision_requested_version" integer,
    "lock_version" bigint DEFAULT 0,
    "template_lineage_id" uuid,
    "template_version_id" uuid,
    "substage_id" uuid,
    "target_subject" character varying(10),
    "migration_review_required" boolean DEFAULT false,
    "distribution_enabled" boolean DEFAULT false,
    "approved_at" timestamp with time zone,
    "approved_by" uuid,
    "migration_reviewed_at" timestamp with time zone,
    "migration_reviewed_by" uuid,
    "due_anchor_type" character varying(30),
    "due_offset_start" integer,
    "due_offset_end" integer,
    "due_offset_unit" character varying(10),
    "template_type" character varying(20) DEFAULT 'MANDATORY'::character varying,
    "recipient_scope" character varying(10),
    "eligibility_anchor_type" character varying(30),
    "eligibility_range_unit" character varying(10),
    "eligibility_start_inclusive" integer,
    "eligibility_end_inclusive" integer,
    "support_function_code" character varying(40),
    "schedule_type" character varying(20),
    "materialization_policy" character varying(30),
    "schedule_group_key" character varying(120),
    "schedule_context_type" character varying(10),
    "schedule_end_mode" character varying(20),
    "week_boundary_rule" character varying(30),
    "checklist_contract_version" smallint,
    "checklist_metadata_jsonb" jsonb,
    "checklist_metadata_hash" character varying(128),
    "checklist_quarantine_reason_code" character varying(80)
);

CREATE TABLE public."care_subjects" (
    "care_subject_id" uuid DEFAULT gen_random_uuid (),
    "person_id" uuid,
    "owner_user_id" uuid,
    "mother_journey_id" uuid,
    "subject_type" character varying(30),
    "nickname" character varying(100),
    "birth_date" date,
    "sex" character varying(10),
    "birth_weight_kg" numeric(4, 2),
    "birth_length_cm" numeric(4, 1),
    "status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."care_tasks" (
    "task_id" uuid DEFAULT gen_random_uuid (),
    "task_type" character varying(40),
    "owner_user_id" uuid,
    "care_group_id" uuid,
    "creator_user_id" uuid,
    "assignee_user_id" uuid,
    "care_subject_id" uuid,
    "title" character varying(255),
    "description" text,
    "scheduled_at" timestamp with time zone,
    "recurrence_rule" character varying(255),
    "snoozed_until" timestamp with time zone,
    "completed_at" timestamp with time zone,
    "cancelled_at" timestamp with time zone,
    "skipped_at" timestamp with time zone,
    "status" character varying(30) DEFAULT 'PENDING'::character varying,
    "source_reference_type" character varying(60),
    "source_reference_id" uuid,
    "vaccination_record_id" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "metadata_jsonb" jsonb DEFAULT '{}'::jsonb,
    "journey_id" uuid,
    "baby_id" uuid,
    "recurrence_type" character varying(30),
    "recurrence_end_date" timestamp with time zone,
    "fcm_job_id" character varying(255),
    "item_type" character varying(60),
    "origin" character varying(20) DEFAULT 'USER_CREATED'::character varying,
    "target_subject" character varying(10) DEFAULT 'MOTHER'::character varying,
    "reminder_occurrence_generation" bigint DEFAULT 0
);

CREATE TABLE public."checklist_action_commands" (
    "checklist_action_command_id" uuid DEFAULT gen_random_uuid (),
    "actor_user_id" uuid,
    "task_kind" character varying(30),
    "task_id" uuid,
    "client_request_id" uuid,
    "payload_hash" character(64),
    "action_type" character varying(30),
    "result_status" character varying(20),
    "result_jsonb" jsonb DEFAULT '{}'::jsonb,
    "applied_at" timestamp with time zone DEFAULT now(),
    "retain_until" timestamp with time zone,
    "legal_hold" boolean DEFAULT false,
    "created_at" timestamp with time zone DEFAULT now(),
    "reminder_definition_id" uuid
);

CREATE TABLE public."checklist_instances" (
    "checklist_instance_id" uuid DEFAULT gen_random_uuid (),
    "distribution_key" character(64),
    "key_version" character varying(10) DEFAULT 'v1'::character varying,
    "template_lineage_id" uuid,
    "template_version_id" uuid,
    "recipient_user_id" uuid,
    "recipient_role" character varying(10),
    "care_group_id" uuid,
    "care_context_type" character varying(10),
    "care_context_id" uuid,
    "context_owner_user_id" uuid,
    "origin" character varying(20),
    "window_start" date,
    "window_end" date,
    "status" character varying(20) DEFAULT 'PENDING'::character varying,
    "lock_version" bigint DEFAULT 0,
    "completed_at" timestamp with time zone,
    "cancelled_at" timestamp with time zone,
    "cancellation_reason_code" character varying(80),
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "journey_context_id" uuid GENERATED ALWAYS AS (
        CASE
            WHEN (
                (care_context_type)::text = 'JOURNEY'::text
            ) THEN care_context_id
            ELSE NULL::uuid
        END
    ) STORED,
    "baby_context_id" uuid GENERATED ALWAYS AS (
        CASE
            WHEN (
                (care_context_type)::text = 'BABY'::text
            ) THEN care_context_id
            ELSE NULL::uuid
        END
    ) STORED,
    "baby_context_subject_type" character varying(30) GENERATED ALWAYS AS (
        CASE
            WHEN (
                (care_context_type)::text = 'BABY'::text
            ) THEN 'BABY'::text
            ELSE NULL::text
        END
    ) STORED,
    "historical_at" timestamp with time zone,
    "history_reason_code" character varying(80),
    "period_key" character varying(180),
    "schedule_zone_id" character varying(80),
    "gestational_dating_revision" bigint,
    "care_group_member_id" uuid,
    "checklist_access_epoch" bigint,
    "checklist_contract_version" smallint,
    "materialization_mode" character varying(20),
    "was_actionable" boolean,
    "checklist_quarantine_reason_code" character varying(80)
);

CREATE TABLE public."checklist_task_instances" (
    "checklist_task_instance_id" uuid DEFAULT gen_random_uuid (),
    "checklist_instance_id" uuid,
    "template_version_id" uuid,
    "template_item_version_id" uuid,
    "task_key" character(64),
    "key_version" character varying(10) DEFAULT 'v1'::character varying,
    "title_snapshot" character varying(500),
    "display_order" integer DEFAULT 0,
    "is_required" boolean DEFAULT false,
    "target_subject" character varying(10),
    "due_at" timestamp with time zone,
    "status" character varying(20) DEFAULT 'PENDING'::character varying,
    "lock_version" bigint DEFAULT 0,
    "completed_at" timestamp with time zone,
    "skipped_at" timestamp with time zone,
    "cancelled_at" timestamp with time zone,
    "action_reason_code" character varying(80),
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "category" character varying(20) DEFAULT 'GENERAL'::character varying,
    "description_snapshot" text,
    "support_function_code" character varying(40),
    "checklist_contract_version" smallint,
    "checklist_quarantine_reason_code" character varying(80)
);

CREATE TABLE public."community_content" (
    "content_id" uuid DEFAULT gen_random_uuid (),
    "topic_id" uuid,
    "parent_content_id" uuid,
    "author_user_id" uuid,
    "content_type" character varying(20),
    "title" character varying(255),
    "body" text,
    "stage" character varying(30),
    "urgency" character varying(20),
    "is_anonymous" boolean DEFAULT false,
    "moderation_status" character varying(30) DEFAULT 'PENDING'::character varying,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "pregnancy_week" smallint,
    "baby_age_months" smallint,
    "like_count" integer DEFAULT 0,
    "answer_count" integer DEFAULT 0,
    "is_expert_labeled" boolean DEFAULT false,
    "is_personal_experience" boolean DEFAULT false,
    "image_urls" jsonb DEFAULT '[]'::jsonb,
    "experience_tag" character varying(80)
);

CREATE TABLE public."community_interactions" (
    "interaction_id" uuid DEFAULT gen_random_uuid (),
    "actor_user_id" uuid,
    "interaction_type" character varying(255),
    "content_id" uuid,
    "topic_id" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "target_content_type" character varying(255)
);

CREATE TABLE public."community_topics" (
    "id" uuid,
    "created_at" timestamp with time zone,
    "description" text,
    "name" character varying(100),
    "updated_at" timestamp with time zone DEFAULT now(),
    "is_hidden" boolean DEFAULT false,
    "icon" character varying(255),
    "sort_order" integer DEFAULT 0,
    "created_by" uuid,
    "type" character varying(20) DEFAULT 'TOPIC'::character varying,
    "slug" character varying(140),
    "parent_id" uuid
);

CREATE TABLE public."consultation_bookings" (
    "booking_id" uuid DEFAULT gen_random_uuid (),
    "requester_user_id" uuid,
    "expert_profile_id" uuid,
    "availability_id" uuid,
    "expert_price_id" uuid,
    "price_band_id" uuid,
    "shared_summary_id" uuid,
    "topic" character varying(500),
    "scheduled_start" timestamp with time zone,
    "scheduled_end" timestamp with time zone,
    "price_snapshot_amount" numeric,
    "commission_rate_snapshot" numeric,
    "cancellation_policy_snapshot" text,
    "price_locked_at" timestamp with time zone,
    "status" character varying(30),
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "communication_room_id" character varying(255),
    "session_started_at" timestamp with time zone,
    "session_ended_at" timestamp with time zone,
    "session_status" character varying(30),
    "expert_summary" text,
    "technical_log_json" jsonb,
    "session_created_at" timestamp with time zone,
    "legacy_session_id" uuid
);

CREATE TABLE public."consultation_context_citations" (
    "citation_snapshot_id" uuid DEFAULT gen_random_uuid (),
    "context_share_id" uuid,
    "evidence_source_id" uuid,
    "organization" character varying(255),
    "source_url" character varying(1000),
    "source_status_at_share" character varying(30),
    "reviewed_at" timestamp with time zone,
    "ordinal" smallint,
    "created_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."consultation_context_shares" (
    "context_share_id" uuid DEFAULT gen_random_uuid (),
    "consultation_request_id" uuid,
    "owner_user_id" uuid,
    "intake_session_id" uuid,
    "expert_profile_id" uuid,
    "consent_grant_id" bigint,
    "idempotency_key" uuid,
    "journey_id" uuid,
    "origin_dashboard" character varying(30),
    "origin_reference_id" uuid,
    "triage_stage" character varying(20),
    "risk_level" character varying(10),
    "intake_status" character varying(20),
    "risk_summary" character varying(500),
    "share_policy_version" character varying(60),
    "created_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."content_item_sources" (
    "content_item_source_id" uuid DEFAULT gen_random_uuid (),
    "content_item_id" uuid,
    "knowledge_source_id" uuid,
    "source_title" character varying(500),
    "source_url" character varying(2000),
    "source_publisher" character varying(255),
    "source_snapshot_jsonb" jsonb DEFAULT '{}'::jsonb,
    "created_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."content_item_topics" (
    "content_item_id" uuid,
    "topic_id" uuid,
    "created_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."content_items" (
    "content_item_id" uuid,
    "author_user_id" uuid,
    "body" text,
    "content_type" character varying(30),
    "created_at" timestamp(6) with time zone,
    "published_at" timestamp(6) with time zone,
    "source_label" character varying(255),
    "status" character varying(20),
    "title" character varying(250),
    "topic_id" uuid,
    "updated_at" timestamp(6) with time zone,
    "version_no" integer,
    "stage" character varying(30),
    "revision_reason" text,
    "revision_requested_at" timestamp with time zone,
    "revision_requested_by" uuid,
    "revision_requested_version" integer,
    "lock_version" bigint DEFAULT 0,
    "summary" character varying(150),
    "eligible_from_week" smallint,
    "eligible_to_week" smallint,
    "recommendation_priority" smallint DEFAULT 0
);

CREATE TABLE public."conversation_calls" (
    "call_id" uuid DEFAULT gen_random_uuid (),
    "conversation_id" uuid,
    "initiated_by_user_id" uuid,
    "call_type" character varying(10),
    "call_status" character varying(20) DEFAULT 'INITIATED'::character varying,
    "zego_room_id" character varying(255),
    "initiated_at" timestamp with time zone,
    "answered_at" timestamp with time zone,
    "ended_at" timestamp with time zone,
    "duration_seconds" integer,
    "created_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."data_permissions" (
    "permission_id" uuid DEFAULT gen_random_uuid (),
    "created_at" timestamp(6) with time zone,
    "expires_at" timestamp(6) with time zone,
    "granted_at" timestamp(6) with time zone,
    "grantee_user_id" uuid,
    "owner_user_id" uuid,
    "purpose" character varying(60),
    "revoked_at" timestamp(6) with time zone,
    "scope_reference_id" uuid,
    "scope_type" character varying(60),
    "status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "updated_at" timestamp(6) with time zone,
    "permission_series_id" uuid,
    "version_number" integer,
    "supersedes_permission_id" uuid,
    "revoked_by" uuid,
    "policy_version" character varying(60),
    "consent_evidence_key" character varying(255),
    "legacy_consent_id" bigint GENERATED BY DEFAULT AS IDENTITY,
    "permission_kind" character varying(255) DEFAULT 'DATA_PERMISSION'::character varying,
    "recipient" character varying(120),
    "scope_text" text,
    "evidence_key" uuid,
    "locale" character varying(20)
);

CREATE TABLE public."development_milestones" (
    "milestone_id" uuid DEFAULT gen_random_uuid (),
    "baby_id" uuid,
    "milestone_type" character varying(80),
    "achieved_date" date,
    "note" text,
    "source_type" character varying(30),
    "recorded_by" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "milestone_status" character varying(20) DEFAULT 'ACHIEVED'::character varying,
    "record_status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "care_subject_id" uuid
);

CREATE TABLE public."device_tokens" (
    "id" uuid DEFAULT gen_random_uuid (),
    "user_id" uuid,
    "token" character varying(512),
    "platform" character varying(30),
    "active" boolean DEFAULT true,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."direct_conversation_read_cursors" (
    "conversation_id" uuid,
    "reader_user_id" uuid,
    "last_read_at" timestamp with time zone,
    "last_read_message_id" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."direct_conversations" (
    "conversation_id" uuid DEFAULT gen_random_uuid (),
    "mother_user_id" uuid,
    "expert_user_id" uuid,
    "status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "created_at" timestamp with time zone DEFAULT now(),
    "last_activity_at" timestamp with time zone
);

CREATE TABLE public."direct_messages" (
    "message_id" uuid DEFAULT gen_random_uuid (),
    "conversation_id" uuid,
    "sender_user_id" uuid,
    "client_message_id" uuid,
    "message_type" character varying(30) DEFAULT 'TEXT'::character varying,
    "message_body" text,
    "created_at" timestamp with time zone DEFAULT now(),
    "attachment_id" uuid,
    "recalled_at" timestamp with time zone,
    "recalled_by_user_id" uuid,
    "location_latitude" double precision,
    "location_longitude" double precision,
    "location_label" character varying(200)
);

CREATE TABLE public."expense_entries" (
    "expense_entry_id" uuid DEFAULT gen_random_uuid (),
    "owner_user_id" uuid,
    "care_subject_id" uuid,
    "mother_journey_id" uuid,
    "category" character varying(80),
    "amount" numeric(38, 2),
    "currency" character varying(10) DEFAULT 'VND'::character varying,
    "expense_date" date,
    "note" text,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."expert_availability" (
    "availability_id" uuid DEFAULT gen_random_uuid (),
    "start_at" timestamp with time zone,
    "end_at" timestamp with time zone,
    "channel_type" character varying(30),
    "status" character varying(20) DEFAULT 'AVAILABLE'::character varying,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "professional_profile_id" uuid,
    "user_id" uuid
);

CREATE TABLE public."expert_consultation_requests" (
    "id" uuid DEFAULT gen_random_uuid (),
    "requester_user_id" uuid,
    "expert_profile_id" uuid,
    "client_request_id" uuid,
    "topic" character varying(200),
    "description" character varying(2000),
    "preferred_window_start" timestamp with time zone,
    "preferred_window_end" timestamp with time zone,
    "status" character varying(20) DEFAULT 'PENDING'::character varying,
    "reject_reason" character varying(500),
    "direct_conversation_id" uuid,
    "responded_at" timestamp with time zone,
    "responded_by" uuid,
    "expires_at" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."expert_location_shares" (
    "location_share_id" uuid DEFAULT gen_random_uuid (),
    "latitude" numeric(10, 8),
    "longitude" numeric(11, 8),
    "accuracy_meters" numeric(6, 2),
    "availability_status" character varying(20),
    "shared_at" timestamp with time zone DEFAULT now(),
    "expires_at" timestamp with time zone,
    "consent_reference" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "professional_profile_id" uuid,
    "user_id" uuid
);

CREATE TABLE public."health_context_memories" (
    "memory_id" uuid DEFAULT gen_random_uuid (),
    "user_id" uuid,
    "care_subject_id" uuid,
    "triage_session_id" uuid,
    "related_stage" character varying(20),
    "summary_text" text,
    "memory_payload_jsonb" jsonb DEFAULT '{}'::jsonb,
    "created_at" timestamp with time zone DEFAULT now(),
    "expires_at" timestamp with time zone,
    "deleted_at" timestamp with time zone,
    "mother_profile_id" uuid,
    "baby_profile_id" uuid
);

CREATE TABLE public."health_metric_definitions" (
    "metric_definition_id" uuid DEFAULT gen_random_uuid (),
    "metric_code" character varying(60),
    "version" integer,
    "display_name" character varying(120),
    "observation_shape" character varying(30),
    "subject_type" character varying(30) DEFAULT 'MOTHER'::character varying,
    "manual_entry_supported" boolean DEFAULT false,
    "device_import_supported" boolean DEFAULT false,
    "canonical_unit" character varying(30),
    "accepted_input_units_jsonb" jsonb DEFAULT '[]'::jsonb,
    "precision_scale" smallint,
    "required_context_schema_jsonb" jsonb DEFAULT '{}'::jsonb,
    "plausibility_policy_jsonb" jsonb DEFAULT '{}'::jsonb,
    "aggregation_policy_jsonb" jsonb DEFAULT '{}'::jsonb,
    "chart_policy_jsonb" jsonb DEFAULT '{}'::jsonb,
    "quality_policy_jsonb" jsonb DEFAULT '{}'::jsonb,
    "safety_policy_version" character varying(40),
    "allowed_journey_stages_jsonb" jsonb DEFAULT '[]'::jsonb,
    "is_active" boolean DEFAULT true,
    "effective_from" timestamp with time zone DEFAULT now(),
    "effective_until" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."health_observations" (
    "health_observation_id" uuid DEFAULT gen_random_uuid (),
    "care_subject_id" uuid,
    "observation_type" character varying(50),
    "value_numeric" numeric(10, 2),
    "value_secondary" numeric(10, 2),
    "unit" character varying(30),
    "observed_at" timestamp with time zone,
    "source_record_id" uuid,
    "quality_label" character varying(30),
    "raw_payload_jsonb" jsonb DEFAULT '{}'::jsonb,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "legacy_source" character varying(60),
    "legacy_id" character varying(100),
    "severity" character varying(30),
    "source_type" character varying(60) DEFAULT 'SYSTEM'::character varying,
    "subject_type" character varying(30),
    "text_value" text,
    "period_start" timestamp with time zone,
    "period_end" timestamp with time zone,
    "context_jsonb" jsonb DEFAULT '{}'::jsonb,
    "original_unit" character varying(30),
    "definition_version" integer,
    "observation_shape" character varying(30),
    "measurement_group_id" uuid,
    "deleted_at" timestamp with time zone
);

CREATE TABLE public."health_records" (
    "health_record_id" uuid DEFAULT gen_random_uuid (),
    "owner_user_id" uuid,
    "journey_id" uuid,
    "baby_id" uuid,
    "record_type" character varying(50),
    "title" character varying(255),
    "file_url" text,
    "record_date" date,
    "source_type" character varying(30),
    "source_name" character varying(200),
    "status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "care_subject_id" uuid,
    "summary_period" character varying(30),
    "period_start" date,
    "summary_json" jsonb
);

CREATE TABLE public."knowledge_source_reviews" (
    "review_id" uuid DEFAULT gen_random_uuid (),
    "knowledge_source_id" uuid,
    "previous_status" character varying(30),
    "new_status" character varying(30),
    "actor_user_id" uuid,
    "actor_role" character varying(80),
    "notes" text,
    "changed_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."knowledge_sources" (
    "knowledge_source_id" uuid DEFAULT gen_random_uuid (),
    "domain" character varying(255),
    "base_url" character varying(500),
    "organization" character varying(255),
    "category" character varying(40),
    "status" character varying(30),
    "discovery_mode" character varying(40),
    "applicable_stages" text,
    "added_by" uuid,
    "reviewed_by" uuid,
    "reviewed_at" timestamp with time zone,
    "notes" text,
    "source_version" character varying(80),
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."maternal_exercise_sessions" (
    "exercise_session_id" uuid DEFAULT gen_random_uuid (),
    "mother_journey_id" uuid,
    "owner_user_id" uuid,
    "exercise_template_id" uuid,
    "posture_config_id" uuid,
    "started_at" timestamp with time zone,
    "ended_at" timestamp with time zone,
    "paused_seconds" integer DEFAULT 0,
    "completion_percent" numeric(38, 2),
    "posture_score" numeric(38, 2),
    "session_status" character varying(20),
    "warning_count" integer DEFAULT 0,
    "summary_jsonb" jsonb DEFAULT '{}'::jsonb,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "safety_observation_id" uuid
);

CREATE TABLE public."moderation_cases" (
    "moderation_case_id" uuid DEFAULT gen_random_uuid (),
    "reporter_user_id" uuid,
    "assigned_moderator_id" uuid,
    "target_type" character varying(30),
    "target_id" uuid,
    "reason_code" character varying(80),
    "description" text,
    "status" character varying(20) DEFAULT 'OPEN'::character varying,
    "opened_at" timestamp with time zone DEFAULT now(),
    "resolved_at" timestamp with time zone,
    "updated_at" timestamp with time zone DEFAULT now(),
    "report_source" character varying(20) DEFAULT 'USER'::character varying,
    "reverted_at" timestamp with time zone,
    "reverted_by" uuid,
    "priority" character varying(20) DEFAULT 'NORMAL'::character varying,
    "claimed_at" timestamp with time zone,
    "ai_feedback_decision" character varying(20),
    "ai_feedback_reason" text,
    "ai_feedback_by" uuid,
    "ai_feedback_at" timestamp with time zone,
    "ai_feedback_assessment_id" uuid
);

CREATE TABLE public."mother_journeys" (
    "journey_id" uuid DEFAULT gen_random_uuid (),
    "owner_user_id" uuid,
    "journey_type" character varying(20),
    "start_date" date,
    "last_menstrual_date" date,
    "estimated_due_date" date,
    "delivery_date" date,
    "status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "notes" text,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "version" bigint DEFAULT 0,
    "date_source" character varying(30),
    "date_confidence" character varying(20),
    "pregnancy_outcome" character varying(30),
    "pregnancy_outcome_date" date,
    "care_subject_id" uuid,
    "baseline_revision" bigint,
    "baseline_schema_version" character varying(40),
    "baseline_source" character varying(30),
    "baseline_lifecycle_goal" character varying(40),
    "baseline_locale" character varying(20),
    "baseline_time_zone" character varying(80),
    "baseline_preferences" character varying(300),
    "baseline_submission_id" uuid,
    "baseline_recorded_at" timestamp with time zone,
    "recommendation_profile_jsonb" jsonb DEFAULT '{}'::jsonb,
    "recommendation_profile_version" smallint DEFAULT 0,
    "recommendation_profile_completed_at" timestamp with time zone,
    "recommendation_profile_status" character varying(24) DEFAULT 'NOT_STARTED'::character varying,
    "gestational_dating_basis" character varying(20),
    "gestational_dating_revision" bigint,
    "gestational_dating_effective_at" timestamp with time zone,
    "gestational_dating_quarantine_reason_code" character varying(80)
);

CREATE TABLE public."notification_jobs" (
    "job_id" uuid DEFAULT gen_random_uuid (),
    "job_type" character varying(20),
    "due_at" timestamp with time zone,
    "status" character varying(20) DEFAULT 'PENDING'::character varying,
    "attempt_count" integer DEFAULT 0,
    "next_attempt_at" timestamp with time zone,
    "locked_by" character varying(120),
    "locked_at" timestamp with time zone,
    "notification_record_id" uuid,
    "last_error_code" character varying(80),
    "created_at" timestamp with time zone DEFAULT clock_timestamp(),
    "updated_at" timestamp with time zone DEFAULT clock_timestamp(),
    "schedule_id" uuid,
    "schedule_revision" bigint,
    "occurrence_date" date,
    "local_time" time without time zone,
    "time_zone" character varying(80),
    "reminder_id" uuid,
    "occurrence_id" uuid,
    "occurrence_generation" bigint,
    "occurrence_scheduled_at" timestamp with time zone,
    "config_revision" bigint,
    "offset_minutes" integer
);

CREATE TABLE public."notification_records" (
    "id" uuid DEFAULT gen_random_uuid (),
    "user_id" uuid,
    "type" character varying(50),
    "title" character varying(255),
    "body" text,
    "reference_id" uuid,
    "reference_type" character varying(50),
    "status" character varying(20) DEFAULT 'SENT'::character varying,
    "fcm_message_id" character varying(255),
    "attempt_count" integer DEFAULT 1,
    "created_at" timestamp with time zone DEFAULT now(),
    "sent_at" timestamp with time zone,
    "failed_at" timestamp with time zone,
    "is_read" boolean DEFAULT false,
    "read_at" timestamp with time zone,
    "metadata" jsonb,
    "processing_started_at" timestamp with time zone,
    "channel" character varying(30) DEFAULT 'PUSH'::character varying,
    "updated_at" timestamp with time zone DEFAULT now(),
    "claim_token" uuid,
    "care_group_id" uuid
);

CREATE TABLE public."professional_specialties" (
    "professional_profile_id" uuid,
    "specialty_id" uuid,
    "is_primary" boolean DEFAULT false,
    "created_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."red_flag_rules" (
    "id" uuid DEFAULT gen_random_uuid (),
    "keyword" character varying(255),
    "severity" character varying(20),
    "action" character varying(20),
    "is_active" boolean DEFAULT true,
    "is_system_default" boolean DEFAULT false,
    "created_by" uuid,
    "updated_by" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."reminder_occurrence_aliases" (
    "occurrence_id" uuid,
    "reminder_definition_id" uuid,
    "owner_user_id" uuid,
    "scheduled_at" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT clock_timestamp(),
    "occurrence_generation" bigint DEFAULT 0
);

CREATE TABLE public."reminder_schedules" (
    "schedule_id" uuid DEFAULT gen_random_uuid (),
    "owner_user_id" uuid,
    "title" character varying(255),
    "time_zone" character varying(80),
    "recurrence" character varying(20) DEFAULT 'NONE'::character varying,
    "start_date" date,
    "end_date" date,
    "active" boolean DEFAULT true,
    "revision" bigint DEFAULT 1,
    "lock_version" bigint DEFAULT 0,
    "created_at" timestamp with time zone DEFAULT clock_timestamp(),
    "updated_at" timestamp with time zone DEFAULT clock_timestamp(),
    "local_times" time without time zone [] DEFAULT '{}'::time without time zone []
);

CREATE TABLE public."safety_events" (
    "safety_event_id" uuid DEFAULT gen_random_uuid (),
    "user_id" uuid,
    "care_subject_id" uuid,
    "monitoring_session_id" uuid,
    "source_event_id" uuid,
    "detected_at" timestamp with time zone DEFAULT now(),
    "event_type" character varying(50),
    "confidence_score" numeric,
    "peak_acceleration" numeric,
    "angular_velocity" numeric,
    "inactivity_seconds" integer,
    "response_type" character varying(30),
    "response_at" timestamp with time zone,
    "false_positive_reason" text,
    "status" character varying(20) DEFAULT 'DETECTED'::character varying,
    "location_snapshot_jsonb" jsonb DEFAULT '{}'::jsonb,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "record_type" character varying(255) DEFAULT 'IMU_EVENT'::character varying,
    "magnitude" numeric(10, 4),
    "user_latitude" numeric(10, 7),
    "user_longitude" numeric(10, 7),
    "client_detected_at" timestamp with time zone,
    "resolved_at" timestamp with time zone,
    "notes" character varying(255),
    "signal_key" character varying(200),
    "countdown_deadline_at" timestamp with time zone,
    "response_reason" character varying(500),
    "escalation_started_at" timestamp with time zone,
    "emergency_session_id" uuid,
    "created_by_text" character varying(255),
    "created_by_user_id" uuid,
    "alert_generation" bigint DEFAULT 0,
    "alert_status" character varying(20),
    "alert_claim_token" uuid,
    "alert_claimed_at" timestamp(6) with time zone,
    "alert_lease_expires_at" timestamp(6) with time zone,
    "alert_completed_at" timestamp(6) with time zone,
    "alert_successful_recipient_count" integer DEFAULT 0,
    "alert_failed_recipient_count" integer DEFAULT 0,
    "alert_updated_at" timestamp(6) with time zone,
    "action_type" character varying(40),
    "action_status" character varying(20),
    "actor_type" character varying(20),
    "attempt_number" integer,
    "accuracy_meters" numeric(6, 2),
    "captured_at" timestamp with time zone,
    "care_facility_id" uuid,
    "consent_status" character varying(20),
    "context_id" uuid,
    "context_type" character varying(50),
    "delivered_at" timestamp with time zone,
    "delivery_status" character varying(30),
    "device_token_id" uuid,
    "expires_at" timestamp with time zone,
    "failure_code" character varying(120),
    "fcm_message_id" character varying(255),
    "idempotency_key" character varying(255),
    "latitude" numeric(10, 8),
    "longitude" numeric(11, 8),
    "location_included" boolean,
    "notification_record_id" uuid,
    "parent_event_id" uuid,
    "reason" character varying(500),
    "recipient_count" integer,
    "recipient_user_id" uuid,
    "responded_at" timestamp with time zone,
    "risk_level" character varying(20),
    "triage_handoff_id" uuid,
    "device_identifier" character varying(255),
    "attempt_status" character varying(20),
    "started_at" timestamp with time zone,
    "completed_at" timestamp with time zone,
    "lease_expires_at" timestamp with time zone,
    "successful_recipient_count" integer,
    "failed_recipient_count" integer,
    "summary" text,
    "action_phase" character varying(30),
    "fence_token" uuid,
    "related_action_id" uuid,
    "owner_user_id" uuid
);

CREATE TABLE public."safety_monitoring_sessions" (
    "monitoring_session_id" uuid DEFAULT gen_random_uuid (),
    "user_id" uuid,
    "status" character varying(10) DEFAULT 'ACTIVE'::character varying,
    "sensitivity_level" character varying(10) DEFAULT 'MEDIUM'::character varying,
    "started_at" timestamp with time zone DEFAULT now(),
    "ended_at" timestamp with time zone,
    "created_by" uuid
);

CREATE TABLE public."specialties" (
    "specialty_id" uuid,
    "code" character varying(80),
    "name" character varying(150),
    "description" text,
    "is_active" boolean DEFAULT true,
    "created_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."system_configurations" (
    "system_configuration_id" uuid DEFAULT gen_random_uuid (),
    "api_rate_limit" integer,
    "connection_timeout_ms" integer,
    "max_upload_size_mb" integer,
    "administrator_email" character varying(254),
    "email_alerts" boolean DEFAULT true,
    "sms_alerts" boolean DEFAULT true,
    "webhook_alerts" boolean DEFAULT false,
    "ai_moderation_enabled" boolean DEFAULT true,
    "maintenance_mode_enabled" boolean DEFAULT false,
    "updated_by" uuid,
    "row_version" bigint DEFAULT 0,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."triage_session_evidence" (
    "evidence_id" uuid DEFAULT gen_random_uuid (),
    "triage_session_id" uuid,
    "evidence_type" character varying(40),
    "claim_code" character varying(100),
    "claim_text" text,
    "knowledge_source_id" uuid,
    "citation_url" text,
    "citation_domain" character varying(255),
    "source_version" character varying(80),
    "source_snapshot_jsonb" jsonb DEFAULT '{}'::jsonb,
    "content_hash" character varying(128),
    "created_at" timestamp with time zone DEFAULT now()
);

CREATE TABLE public."triage_sessions" (
    "triage_session_id" uuid DEFAULT gen_random_uuid (),
    "user_id" uuid,
    "care_subject_id" uuid,
    "stage" character varying(20),
    "profile_context_id" uuid,
    "risk_level" character varying(10),
    "status" character varying(20) DEFAULT 'PENDING'::character varying,
    "emergency" boolean DEFAULT false,
    "disclaimer_version" character varying(80),
    "input_jsonb" jsonb DEFAULT '{}'::jsonb,
    "result_jsonb" jsonb DEFAULT '{}'::jsonb,
    "conversation_jsonb" jsonb DEFAULT '{}'::jsonb,
    "schema_version" character varying(30) DEFAULT '1'::character varying,
    "content_hash" character varying(128),
    "created_at" timestamp with time zone DEFAULT now(),
    "completed_at" timestamp with time zone,
    "updated_at" timestamp with time zone DEFAULT now(),
    "baby_profile_id" uuid,
    "mother_profile_id" uuid,
    "client_request_id" character varying(64),
    "symptoms" text,
    "raw_ai_response" text,
    "disclaimer_text" text,
    "created_by" uuid,
    "symptom_list" jsonb,
    "duration_days" integer,
    "intensity" character varying(20),
    "emergency_flag" boolean,
    "extracted_at" timestamp with time zone,
    "structured_created_by" character varying(255),
    "journey_id" uuid,
    "origin_dashboard" character varying(30),
    "origin_reference_id" uuid,
    "continuation_token" uuid,
    "continuation_expires_at" timestamp(6) with time zone,
    "continuation_acknowledged_at" timestamp(6) with time zone
);

CREATE TABLE public."users" (
    "user_id" uuid,
    "avatar_url" character varying(500),
    "created_at" timestamp(6) with time zone,
    "email" character varying(255),
    "full_name" character varying(120),
    "password_hash" character varying(255),
    "phone" character varying(20),
    "updated_at" timestamp(6) with time zone,
    "enabled" boolean DEFAULT true,
    "locked" boolean DEFAULT false,
    "lock_type" character varying(30),
    "lock_reason" character varying(500),
    "locked_by" uuid,
    "lock_episode_id" uuid,
    "role" character varying(50),
    "failed_login_count" integer DEFAULT 0,
    "locked_at" timestamp with time zone,
    "email_verified" boolean DEFAULT false,
    "phone_verified" boolean DEFAULT false,
    "account_status" character varying(30),
    "last_login_at" timestamp with time zone,
    "suspended_until" timestamp with time zone,
    "must_change_password" boolean DEFAULT false,
    "community_posting_restricted_until" timestamp with time zone,
    "person_id" uuid,
    "settings_jsonb" jsonb DEFAULT '{}'::jsonb,
    "deactivation_reason" text,
    "deactivated_at" timestamp with time zone,
    "deactivated_by" uuid,
    "display_name" character varying(200),
    "date_of_birth" date,
    "area" character varying(200),
    "professional_title" character varying(150),
    "workplace" character varying(200),
    "experience_years" smallint,
    "consultation_scope" text,
    "verification_status" character varying(30) DEFAULT 'PENDING'::character varying,
    "verified_at" timestamp with time zone,
    "verified_by" uuid,
    "rating_avg" numeric,
    "specialty" character varying(100),
    "facility_id" uuid,
    "trust_status" character varying(20) DEFAULT 'ACTIVE'::character varying,
    "consultation_fee_vnd" bigint,
    "bio" character varying(500),
    "interest_stage" character varying(30),
    "is_visible" boolean,
    "public_avatar_url" character varying(500),
    "region" character varying(120),
    "social_identities" jsonb DEFAULT '[]'::jsonb,
    "specialty_ids" uuid [],
    "fall_detection_enabled" boolean DEFAULT false,
    "fall_detection_sensitivity_level" character varying(10) DEFAULT 'MEDIUM'::character varying,
    "emergency_auto_alert" boolean DEFAULT true,
    "emergency_countdown_seconds" integer DEFAULT 30,
    "sensor_permission_granted" boolean DEFAULT false,
    "sensor_permission_recorded_at" timestamp with time zone,
    "safety_config_updated_at" timestamp with time zone DEFAULT now(),
    "safety_config_updated_by" uuid,
    "safety_location_sharing_enabled" boolean DEFAULT false
);

CREATE TABLE public."vaccination_records" (
    "vaccination_record_id" uuid DEFAULT gen_random_uuid (),
    "baby_id" uuid,
    "vaccine_name" character varying(200),
    "dose_number" smallint,
    "scheduled_date" date,
    "administered_date" date,
    "status" character varying(20) DEFAULT 'SCHEDULED'::character varying,
    "facility_name" character varying(200),
    "proof_record_id" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "postpone_reason" text,
    "care_subject_id" uuid,
    "vaccination_schedule_id" uuid
);

CREATE TABLE public."vaccination_schedules" (
    "vaccination_schedule_id" uuid DEFAULT gen_random_uuid (),
    "vaccine_name" character varying(200),
    "dose_number" smallint,
    "offset_days" integer,
    "description" text,
    "schedule_version" character varying(30) DEFAULT '1'::character varying,
    "active_from" date,
    "active_to" date,
    "created_at" timestamp with time zone DEFAULT now()
);

-- Primary, unique, check and nullability constraints

ALTER TABLE ONLY public."account_lock_appeals"
ADD CONSTRAINT "account_lock_appeals_pkey" PRIMARY KEY (appeal_id);

ALTER TABLE ONLY public."administrative_areas"
ADD CONSTRAINT "administrative_areas_pkey" PRIMARY KEY (administrative_area_id);

ALTER TABLE ONLY public."ai_content_assessments"
ADD CONSTRAINT "ai_content_assessments_pkey" PRIMARY KEY (assessment_id);

ALTER TABLE ONLY public."ai_content_scan_jobs"
ADD CONSTRAINT "ai_content_scan_jobs_pkey" PRIMARY KEY (job_id);

ALTER TABLE ONLY public."ai_moderation_policies"
ADD CONSTRAINT "ai_moderation_policies_pkey" PRIMARY KEY (policy_id);

ALTER TABLE ONLY public."appointment_notification_configs"
ADD CONSTRAINT "appointment_notification_configs_pkey" PRIMARY KEY (reminder_id);

ALTER TABLE ONLY public."attachments"
ADD CONSTRAINT "attachments_pkey" PRIMARY KEY (attachment_id);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_pkey" PRIMARY KEY (audit_event_id);

ALTER TABLE ONLY public."auth_challenges"
ADD CONSTRAINT "auth_challenges_pkey" PRIMARY KEY (challenge_id);

ALTER TABLE ONLY public."auth_sessions"
ADD CONSTRAINT "auth_sessions_pkey" PRIMARY KEY (session_id);

ALTER TABLE ONLY public."care_facilities"
ADD CONSTRAINT "care_facilities_pkey" PRIMARY KEY (facility_id);

ALTER TABLE ONLY public."care_group_members"
ADD CONSTRAINT "care_group_members_pkey" PRIMARY KEY (care_group_member_id);

ALTER TABLE ONLY public."care_groups"
ADD CONSTRAINT "care_groups_pkey" PRIMARY KEY (care_group_id);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_pkey" PRIMARY KEY (template_id);

ALTER TABLE ONLY public."care_subjects"
ADD CONSTRAINT "care_subjects_pkey" PRIMARY KEY (care_subject_id);

ALTER TABLE ONLY public."care_tasks"
ADD CONSTRAINT "care_tasks_pkey1" PRIMARY KEY (task_id);

ALTER TABLE ONLY public."checklist_action_commands"
ADD CONSTRAINT "checklist_action_commands_pk" PRIMARY KEY (checklist_action_command_id);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_pk" PRIMARY KEY (checklist_instance_id);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_instances_pk" PRIMARY KEY (checklist_task_instance_id);

ALTER TABLE ONLY public."community_content"
ADD CONSTRAINT "community_content_pkey" PRIMARY KEY (content_id);

ALTER TABLE ONLY public."community_interactions"
ADD CONSTRAINT "community_interactions_pkey" PRIMARY KEY (interaction_id);

ALTER TABLE ONLY public."community_topics"
ADD CONSTRAINT "community_topics_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY public."consultation_bookings"
ADD CONSTRAINT "consultation_bookings_pkey" PRIMARY KEY (booking_id);

ALTER TABLE ONLY public."consultation_context_citations"
ADD CONSTRAINT "consultation_context_citations_pkey" PRIMARY KEY (citation_snapshot_id);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "consultation_context_shares_pkey" PRIMARY KEY (context_share_id);

ALTER TABLE ONLY public."content_item_sources"
ADD CONSTRAINT "content_item_sources_pkey" PRIMARY KEY (content_item_source_id);

ALTER TABLE ONLY public."content_item_topics"
ADD CONSTRAINT "content_item_topics_pkey" PRIMARY KEY (content_item_id, topic_id);

ALTER TABLE ONLY public."content_items"
ADD CONSTRAINT "content_items_pkey" PRIMARY KEY (content_item_id);

ALTER TABLE ONLY public."conversation_calls"
ADD CONSTRAINT "conversation_calls_pkey" PRIMARY KEY (call_id);

ALTER TABLE ONLY public."data_permissions"
ADD CONSTRAINT "data_permissions_pkey" PRIMARY KEY (permission_id);

ALTER TABLE ONLY public."development_milestones"
ADD CONSTRAINT "development_milestones_pkey" PRIMARY KEY (milestone_id);

ALTER TABLE ONLY public."device_tokens"
ADD CONSTRAINT "device_tokens_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY public."direct_conversation_read_cursors"
ADD CONSTRAINT "direct_conversation_read_cursors_pkey" PRIMARY KEY (
    conversation_id,
    reader_user_id
);

ALTER TABLE ONLY public."direct_conversations"
ADD CONSTRAINT "direct_conversations_pkey" PRIMARY KEY (conversation_id);

ALTER TABLE ONLY public."direct_messages"
ADD CONSTRAINT "direct_messages_pkey" PRIMARY KEY (message_id);

ALTER TABLE ONLY public."expense_entries"
ADD CONSTRAINT "expense_entries_pkey" PRIMARY KEY (expense_entry_id);

ALTER TABLE ONLY public."expert_availability"
ADD CONSTRAINT "expert_availability_pkey" PRIMARY KEY (availability_id);

ALTER TABLE ONLY public."expert_consultation_requests"
ADD CONSTRAINT "expert_consultation_requests_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY public."expert_location_shares"
ADD CONSTRAINT "expert_location_shares_pkey" PRIMARY KEY (location_share_id);

ALTER TABLE ONLY public."health_context_memories"
ADD CONSTRAINT "health_context_memories_pkey" PRIMARY KEY (memory_id);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_pk" PRIMARY KEY (metric_definition_id);

ALTER TABLE ONLY public."health_observations"
ADD CONSTRAINT "health_observations_pkey" PRIMARY KEY (health_observation_id);

ALTER TABLE ONLY public."health_records"
ADD CONSTRAINT "health_records_pkey" PRIMARY KEY (health_record_id);

ALTER TABLE ONLY public."knowledge_source_reviews"
ADD CONSTRAINT "knowledge_source_reviews_pkey" PRIMARY KEY (review_id);

ALTER TABLE ONLY public."knowledge_sources"
ADD CONSTRAINT "knowledge_sources_pkey" PRIMARY KEY (knowledge_source_id);

ALTER TABLE ONLY public."maternal_exercise_sessions"
ADD CONSTRAINT "maternal_exercise_sessions_pkey" PRIMARY KEY (exercise_session_id);

ALTER TABLE ONLY public."moderation_cases"
ADD CONSTRAINT "moderation_cases_pkey" PRIMARY KEY (moderation_case_id);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "mother_journeys_pkey" PRIMARY KEY (journey_id);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_pkey" PRIMARY KEY (job_id);

ALTER TABLE ONLY public."notification_records"
ADD CONSTRAINT "notification_records_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY public."professional_specialties"
ADD CONSTRAINT "professional_specialties_pkey" PRIMARY KEY (
    professional_profile_id,
    specialty_id
);

ALTER TABLE ONLY public."red_flag_rules"
ADD CONSTRAINT "red_flag_rules_pkey" PRIMARY KEY (id);

ALTER TABLE ONLY public."reminder_occurrence_aliases"
ADD CONSTRAINT "reminder_occurrence_aliases_pkey" PRIMARY KEY (occurrence_id);

ALTER TABLE ONLY public."reminder_schedules"
ADD CONSTRAINT "reminder_schedules_pkey" PRIMARY KEY (schedule_id);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_pkey" PRIMARY KEY (safety_event_id);

ALTER TABLE ONLY public."safety_monitoring_sessions"
ADD CONSTRAINT "safety_monitoring_sessions_pkey" PRIMARY KEY (monitoring_session_id);

ALTER TABLE ONLY public."specialties"
ADD CONSTRAINT "specialties_canonical_pkey" PRIMARY KEY (specialty_id);

ALTER TABLE ONLY public."system_configurations"
ADD CONSTRAINT "system_configurations_pkey" PRIMARY KEY (system_configuration_id);

ALTER TABLE ONLY public."triage_session_evidence"
ADD CONSTRAINT "triage_session_evidence_pkey" PRIMARY KEY (evidence_id);

ALTER TABLE ONLY public."triage_sessions"
ADD CONSTRAINT "triage_sessions_pkey" PRIMARY KEY (triage_session_id);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_pkey" PRIMARY KEY (user_id);

ALTER TABLE ONLY public."vaccination_records"
ADD CONSTRAINT "vaccination_records_pkey" PRIMARY KEY (vaccination_record_id);

ALTER TABLE ONLY public."vaccination_schedules"
ADD CONSTRAINT "vaccination_schedules_pkey" PRIMARY KEY (vaccination_schedule_id);

ALTER TABLE ONLY public."administrative_areas"
ADD CONSTRAINT "administrative_areas_code_key" UNIQUE (code);

ALTER TABLE ONLY public."ai_moderation_policies"
ADD CONSTRAINT "uq_ai_moderation_policies_code" UNIQUE (policy_code);

ALTER TABLE ONLY public."attachments"
ADD CONSTRAINT "attachments_storage_key_key" UNIQUE (storage_key);

ALTER TABLE ONLY public."auth_challenges"
ADD CONSTRAINT "auth_challenges_legacy_uk" UNIQUE (legacy_source, legacy_id);

ALTER TABLE ONLY public."auth_sessions"
ADD CONSTRAINT "auth_sessions_legacy_uk" UNIQUE (legacy_source, legacy_id);

ALTER TABLE ONLY public."care_groups"
ADD CONSTRAINT "care_groups_id_owner_uk" UNIQUE (care_group_id, owner_user_id);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_lineage_version_uk" UNIQUE (
    template_lineage_id,
    template_version_id
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_parent_item_uk" UNIQUE (
    parent_template_id,
    template_id,
    entry_type
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_version_id_uk" UNIQUE (template_version_id);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_version_root_uk" UNIQUE (
    template_version_id,
    template_id
);

ALTER TABLE ONLY public."care_subjects"
ADD CONSTRAINT "care_subjects_id_owner_type_checklist_uk" UNIQUE (
    care_subject_id,
    owner_user_id,
    subject_type
);

ALTER TABLE ONLY public."checklist_action_commands"
ADD CONSTRAINT "checklist_action_commands_scope_uk" UNIQUE (
    actor_user_id,
    task_kind,
    task_id,
    client_request_id
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_distribution_key_uk" UNIQUE (distribution_key);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_id_version_uk" UNIQUE (
    checklist_instance_id,
    template_version_id
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_instances_task_key_uk" UNIQUE (task_key);

ALTER TABLE ONLY public."community_interactions"
ADD CONSTRAINT "question_notification_mutes_user_question_unique" UNIQUE (
    actor_user_id,
    interaction_type,
    content_id
);

ALTER TABLE ONLY public."community_interactions"
ADD CONSTRAINT "uq_question_like" UNIQUE (
    actor_user_id,
    interaction_type,
    content_id
);

ALTER TABLE ONLY public."community_interactions"
ADD CONSTRAINT "uq_user_topic_follow" UNIQUE (
    actor_user_id,
    interaction_type,
    topic_id
);

ALTER TABLE ONLY public."community_topics"
ADD CONSTRAINT "community_topics_slug_unique" UNIQUE (slug);

ALTER TABLE ONLY public."consultation_context_citations"
ADD CONSTRAINT "uq_context_citation_source" UNIQUE (
    context_share_id,
    evidence_source_id
);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "consultation_context_shares_consent_grant_id_key" UNIQUE (consent_grant_id);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "consultation_context_shares_consultation_request_id_key" UNIQUE (consultation_request_id);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "uq_context_intake_expert" UNIQUE (
    owner_user_id,
    intake_session_id,
    expert_profile_id
);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "uq_context_owner_key" UNIQUE (
    owner_user_id,
    idempotency_key
);

ALTER TABLE ONLY public."content_item_sources"
ADD CONSTRAINT "content_item_sources_unique_url_uk" UNIQUE (content_item_id, source_url);

ALTER TABLE ONLY public."device_tokens"
ADD CONSTRAINT "device_tokens_unique" UNIQUE (user_id, token);

ALTER TABLE ONLY public."direct_conversations"
ADD CONSTRAINT "direct_conversations_pair_uk" UNIQUE (
    mother_user_id,
    expert_user_id
);

ALTER TABLE ONLY public."direct_messages"
ADD CONSTRAINT "direct_messages_client_uk" UNIQUE (
    conversation_id,
    sender_user_id,
    client_message_id
);

ALTER TABLE ONLY public."expert_consultation_requests"
ADD CONSTRAINT "expert_consultation_requests_owner_client_uk" UNIQUE (
    requester_user_id,
    client_request_id
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_code_version_uk" UNIQUE (metric_code, version);

ALTER TABLE ONLY public."health_observations"
ADD CONSTRAINT "health_observations_legacy_uk" UNIQUE (legacy_source, legacy_id);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "mother_journeys_id_owner_checklist_uk" UNIQUE (journey_id, owner_user_id);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "mother_journeys_subject_uk" UNIQUE (care_subject_id);

ALTER TABLE ONLY public."red_flag_rules"
ADD CONSTRAINT "uq_red_flag_rules_keyword" UNIQUE (keyword);

ALTER TABLE ONLY public."reminder_occurrence_aliases"
ADD CONSTRAINT "reminder_occurrence_alias_definition_generation_schedule_uk" UNIQUE (
    reminder_definition_id,
    occurrence_generation,
    scheduled_at
);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_idempotency_key_key" UNIQUE (idempotency_key);

ALTER TABLE ONLY public."specialties"
ADD CONSTRAINT "specialties_canonical_code_key" UNIQUE (code);

ALTER TABLE ONLY public."triage_session_evidence"
ADD CONSTRAINT "triage_session_evidence_uk" UNIQUE (
    triage_session_id,
    evidence_type,
    content_hash
);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "uk6dotkott2kjsp8vw4d0m25fb7" UNIQUE (email);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "uk97ih1g5lcdf1s3fg7oo4e18jw" UNIQUE (person_id);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_person_uk" UNIQUE (person_id);

ALTER TABLE ONLY public."vaccination_schedules"
ADD CONSTRAINT "vaccination_schedules_key_uk" UNIQUE (
    vaccine_name,
    dose_number,
    schedule_version
);

ALTER TABLE ONLY public."account_lock_appeals"
ADD CONSTRAINT "account_lock_appeals_reason_ck" CHECK (
    length(btrim(reason::text)) > 0
);

ALTER TABLE ONLY public."account_lock_appeals"
ADD CONSTRAINT "account_lock_appeals_review_ck" CHECK (
    status::text = 'PENDING'::text
    AND reviewed_by IS NULL
    AND reviewed_at IS NULL
    OR status::text <> 'PENDING'::text
    AND reviewed_at IS NOT NULL
);

ALTER TABLE ONLY public."account_lock_appeals"
ADD CONSTRAINT "account_lock_appeals_status_ck" CHECK (
    status::text = ANY (
        ARRAY[
            'PENDING'::character varying,
            'APPROVED'::character varying,
            'REJECTED'::character varying,
            'CANCELLED'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."ai_content_assessments"
ADD CONSTRAINT "chk_ai_assessment_classification" CHECK (
    classification IS NULL
    OR (
        classification::text = ANY (
            ARRAY[
                'SAFE'::character varying::text,
                'VIOLATION'::character varying::text,
                'UNCERTAIN'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."ai_content_assessments"
ADD CONSTRAINT "chk_ai_assessment_confidence" CHECK (
    confidence IS NULL
    OR confidence >= 0::numeric
    AND confidence <= 1::numeric
);

ALTER TABLE ONLY public."ai_content_assessments"
ADD CONSTRAINT "chk_ai_assessment_matches_array" CHECK (
    jsonb_typeof(matches_jsonb) = 'array'::text
);

ALTER TABLE ONLY public."ai_content_assessments"
ADD CONSTRAINT "chk_ai_assessment_status" CHECK (
    status::text = ANY (
        ARRAY[
            'COMPLETED'::character varying::text,
            'FAILED'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."ai_content_scan_jobs"
ADD CONSTRAINT "chk_ai_scan_job_status" CHECK (
    status::text = ANY (
        ARRAY[
            'QUEUED'::character varying::text,
            'PROCESSING'::character varying::text,
            'COMPLETED'::character varying::text,
            'FAILED'::character varying::text,
            'SKIPPED'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."ai_content_scan_jobs"
ADD CONSTRAINT "chk_ai_scan_job_target_type" CHECK (
    target_type::text = ANY (
        ARRAY[
            'QUESTION'::character varying::text,
            'ANSWER'::character varying::text,
            'CONTENT'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."ai_moderation_policies"
ADD CONSTRAINT "chk_ai_policy_confidence" CHECK (
    confidence_threshold >= 0::numeric
    AND confidence_threshold <= 1::numeric
);

ALTER TABLE ONLY public."ai_moderation_policies"
ADD CONSTRAINT "chk_ai_policy_severity" CHECK (
    severity::text = ANY (
        ARRAY[
            'LOW'::character varying::text,
            'MEDIUM'::character varying::text,
            'HIGH'::character varying::text,
            'CRITICAL'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."appointment_notification_configs"
ADD CONSTRAINT "appointment_notification_configs_revision_ck" CHECK (config_revision > 0);

ALTER TABLE ONLY public."appointment_notification_configs"
ADD CONSTRAINT "appointment_notification_configs_rules_ck" CHECK (
    carebridge_validate_appointment_rules (rules_jsonb)
);

ALTER TABLE ONLY public."appointment_notification_configs"
ADD CONSTRAINT "appointment_notification_configs_timezone_ck" CHECK (
    length(
        TRIM(
            BOTH
            FROM time_zone
        )
    ) > 0
);

ALTER TABLE ONLY public."attachments"
ADD CONSTRAINT "attachments_file_size_bytes_check" CHECK (file_size_bytes >= 0);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_access_actor_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_ACCESS_BASELINE'::character varying,
                'CHECKLIST_ACCESS_REVOKED'::character varying
            ]::text []
        )
    )
    OR COALESCE(
        actor_type::text = 'SYSTEM'::text
        AND actor_service::text = 'CHECKLIST_P2_BACKFILL'::text
        AND actor_user_id IS NULL
        AND resource_type::text = 'CARE_GROUP_MEMBER'::text
        AND resource_id IS NOT NULL,
        false
    )
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_access_origin_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_ACCESS_BASELINE'::character varying,
                'CHECKLIST_ACCESS_REVOKED'::character varying
            ]::text []
        )
    )
    OR COALESCE(
        event_origin::text = 'CHECKLIST_ACCESS'::text,
        false
    )
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_access_payload_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_ACCESS_BASELINE'::character varying,
                'CHECKLIST_ACCESS_REVOKED'::character varying
            ]::text []
        )
    )
    OR COALESCE(
        resource_type::text = 'CARE_GROUP_MEMBER'::text
        AND resource_id IS NOT NULL
        AND jsonb_typeof(before_payload_jsonb) = 'object'::text
        AND jsonb_typeof(after_payload_jsonb) = 'object'::text
        AND (
            before_payload_jsonb ->> 'schema'::text
        ) = 'CHECKLIST_ACCESS_AUDIT_V1'::text
        AND (
            after_payload_jsonb ->> 'schema'::text
        ) = 'CHECKLIST_ACCESS_AUDIT_V1'::text
        AND before_payload_jsonb ?& ARRAY[
            'schema'::text,
            'eventType'::text,
            'membershipStatus'::text,
            'checklistView'::text,
            'checklistComplete'::text,
            'accessEpoch'::text,
            'effectiveFrom'::text,
            'correlationId'::text
        ]
        AND after_payload_jsonb ?& ARRAY[
            'schema'::text,
            'eventType'::text,
            'membershipStatus'::text,
            'checklistView'::text,
            'checklistComplete'::text,
            'accessEpoch'::text,
            'effectiveFrom'::text,
            'correlationId'::text
        ]
        AND (
            before_payload_jsonb ->> 'eventType'::text
        ) = CASE event_category
            WHEN 'CHECKLIST_ACCESS_BASELINE'::text THEN 'LEGACY_ACCESS_BASELINE'::text
            ELSE 'VIEW_REVOKED'::text
        END
        AND (
            after_payload_jsonb ->> 'eventType'::text
        ) = (
            before_payload_jsonb ->> 'eventType'::text
        )
        AND jsonb_typeof(
            before_payload_jsonb -> 'membershipStatus'::text
        ) = 'string'::text
        AND jsonb_typeof(
            after_payload_jsonb -> 'membershipStatus'::text
        ) = 'string'::text
        AND jsonb_typeof(
            before_payload_jsonb -> 'checklistView'::text
        ) = 'boolean'::text
        AND jsonb_typeof(
            after_payload_jsonb -> 'checklistView'::text
        ) = 'boolean'::text
        AND jsonb_typeof(
            before_payload_jsonb -> 'checklistComplete'::text
        ) = 'boolean'::text
        AND jsonb_typeof(
            after_payload_jsonb -> 'checklistComplete'::text
        ) = 'boolean'::text
        AND jsonb_typeof(
            before_payload_jsonb -> 'accessEpoch'::text
        ) = 'number'::text
        AND jsonb_typeof(
            after_payload_jsonb -> 'accessEpoch'::text
        ) = 'number'::text
        AND (
            before_payload_jsonb ->> 'accessEpoch'::text
        ) ~ '^[0-9]+$'::text
        AND (
            after_payload_jsonb ->> 'accessEpoch'::text
        ) ~ '^[0-9]+$'::text
        AND (
            (
                before_payload_jsonb ->> 'accessEpoch'::text
            )::numeric
        ) >= 0::numeric
        AND (
            (
                after_payload_jsonb ->> 'accessEpoch'::text
            )::numeric
        ) >= 0::numeric
        AND jsonb_typeof(
            before_payload_jsonb -> 'effectiveFrom'::text
        ) = 'string'::text
        AND jsonb_typeof(
            after_payload_jsonb -> 'effectiveFrom'::text
        ) = 'string'::text
        AND (
            before_payload_jsonb ->> 'effectiveFrom'::text
        ) ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}[ T]'::text
        AND (
            after_payload_jsonb ->> 'effectiveFrom'::text
        ) ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}[ T]'::text
        AND pg_input_is_valid (
            before_payload_jsonb ->> 'effectiveFrom'::text,
            'timestamptz'::text
        )
        AND pg_input_is_valid (
            after_payload_jsonb ->> 'effectiveFrom'::text,
            'timestamptz'::text
        )
        AND (
            before_payload_jsonb ->> 'correlationId'::text
        ) = correlation_id::text
        AND (
            after_payload_jsonb ->> 'correlationId'::text
        ) = correlation_id::text
        AND CASE
            WHEN event_category::text = 'CHECKLIST_ACCESS_BASELINE'::text THEN (
                before_payload_jsonb ->> 'membershipStatus'::text
            ) = 'ACCEPTED'::text
            AND (
                after_payload_jsonb ->> 'membershipStatus'::text
            ) = 'ACCEPTED'::text
            AND (
                (
                    before_payload_jsonb ->> 'checklistView'::text
                )::boolean
            )
            AND (
                (
                    after_payload_jsonb ->> 'checklistView'::text
                )::boolean
            )
            ELSE (
                before_payload_jsonb ->> 'membershipStatus'::text
            ) = 'ACCEPTED'::text
            AND (
                after_payload_jsonb ->> 'membershipStatus'::text
            ) = 'REVOKED'::text
            AND NOT (
                (
                    after_payload_jsonb ->> 'checklistView'::text
                )::boolean
            )
            AND NOT (
                (
                    after_payload_jsonb ->> 'checklistComplete'::text
                )::boolean
            )
        END,
        false
    )
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_access_reason_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_ACCESS_BASELINE'::character varying,
                'CHECKLIST_ACCESS_REVOKED'::character varying
            ]::text []
        )
    )
    OR COALESCE(
        event_category::text = 'CHECKLIST_ACCESS_BASELINE'::text
        AND reason_code::text = 'LEGACY_ACCESS_BASELINE'::text
        OR event_category::text = 'CHECKLIST_ACCESS_REVOKED'::text
        AND reason_code::text = 'FAMILY_MEMBER_DUPLICATE'::text,
        false
    )
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_actor_shape_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_TEMPLATE_DECIDED'::character varying,
                'CHECKLIST_DISTRIBUTED'::character varying,
                'CHECKLIST_ASSIGNED'::character varying,
                'CHECKLIST_COMPLETED'::character varying,
                'CHECKLIST_SKIPPED'::character varying,
                'CHECKLIST_REOPENED'::character varying,
                'CHECKLIST_CANCELLED'::character varying,
                'CHECKLIST_RECONCILIATION_FAILED'::character varying,
                'CHECKLIST_MIGRATION_QUARANTINED'::character varying,
                'CHECKLIST_ACCESS_BASELINE'::character varying,
                'CHECKLIST_ACCESS_REVOKED'::character varying
            ]::text []
        )
    )
    OR COALESCE(
        actor_type::text = 'USER'::text
        AND actor_user_id IS NOT NULL
        AND actor_service IS NULL
        OR (
            actor_type::text = ANY (
                ARRAY[
                    'SYSTEM'::character varying,
                    'SERVICE'::character varying
                ]::text []
            )
        )
        AND actor_user_id IS NULL
        AND actor_service IS NOT NULL
        AND btrim(actor_service::text) <> ''::text,
        false
    )
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_actor_type_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_TEMPLATE_DECIDED'::character varying,
                'CHECKLIST_DISTRIBUTED'::character varying,
                'CHECKLIST_ASSIGNED'::character varying,
                'CHECKLIST_COMPLETED'::character varying,
                'CHECKLIST_SKIPPED'::character varying,
                'CHECKLIST_REOPENED'::character varying,
                'CHECKLIST_CANCELLED'::character varying,
                'CHECKLIST_RECONCILIATION_FAILED'::character varying,
                'CHECKLIST_MIGRATION_QUARANTINED'::character varying,
                'CHECKLIST_ACCESS_BASELINE'::character varying,
                'CHECKLIST_ACCESS_REVOKED'::character varying
            ]::text []
        )
    )
    OR COALESCE(
        actor_type IS NOT NULL
        AND (
            actor_type::text = ANY (
                ARRAY[
                    'USER'::character varying,
                    'SYSTEM'::character varying,
                    'SERVICE'::character varying
                ]::text []
            )
        ),
        false
    )
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_context_pair_ck" CHECK (
    event_category::text !~~ 'CHECKLIST_%'::text
    OR care_context_type IS NULL
    AND care_context_id IS NULL
    OR care_context_type IS NOT NULL
    AND care_context_id IS NOT NULL
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_context_type_ck" CHECK (
    event_category::text !~~ 'CHECKLIST_%'::text
    OR care_context_type IS NULL
    OR (
        care_context_type::text = ANY (
            ARRAY[
                'JOURNEY'::character varying,
                'BABY'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_correlation_ck" CHECK (
    event_category::text !~~ 'CHECKLIST_%'::text
    OR correlation_id IS NOT NULL
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_migration_origin_ck" CHECK (
    event_category::text <> 'CHECKLIST_MIGRATION_QUARANTINED'::text
    OR COALESCE(
        event_origin::text = 'CHECKLIST_MIGRATION'::text
        AND actor_type::text = 'SYSTEM'::text
        AND actor_service::text = 'CHECKLIST_P2_BACKFILL'::text
        AND actor_user_id IS NULL
        OR event_origin::text = 'CHECKLIST_MIGRATION'::text
        AND actor_type::text = 'SERVICE'::text
        AND actor_service::text = 'CHECKLIST_LEGACY_BACKFILL'::text
        AND actor_user_id IS NULL
        OR event_origin::text = 'AUDIT_LOG'::text
        AND actor_type::text = 'SERVICE'::text
        AND actor_service::text = 'CHECKLIST_CONTEXT_AUTHORITY'::text
        AND actor_user_id IS NULL,
        false
    )
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_migration_payload_ck" CHECK (
    event_category::text <> 'CHECKLIST_MIGRATION_QUARANTINED'::text
    OR COALESCE(
        event_origin::text = 'CHECKLIST_MIGRATION'::text
        AND actor_type::text = 'SYSTEM'::text
        AND actor_service::text = 'CHECKLIST_P2_BACKFILL'::text
        AND actor_user_id IS NULL
        AND resource_id IS NOT NULL
        AND before_payload_jsonb IS NULL
        AND payload IS NULL
        AND jsonb_typeof(after_payload_jsonb) = 'object'::text
        AND after_payload_jsonb ?& ARRAY[
            'schema'::text,
            'sourceKind'::text,
            'sourceIdHash'::text,
            'reasonCode'::text,
            'disposition'::text,
            'correlationId'::text,
            'metadata'::text
        ]
        AND (
            after_payload_jsonb - ARRAY[
                'schema'::text,
                'sourceKind'::text,
                'sourceIdHash'::text,
                'reasonCode'::text,
                'disposition'::text,
                'correlationId'::text,
                'metadata'::text
            ]
        ) = '{}'::jsonb
        AND (
            after_payload_jsonb ->> 'schema'::text
        ) = 'CHECKLIST_MIGRATION_QUARANTINE_V1'::text
        AND (
            after_payload_jsonb ->> 'sourceKind'::text
        ) = CASE resource_type
            WHEN 'CARE_ITEM_TEMPLATE'::text THEN 'care_item_templates'::text
            WHEN 'CHECKLIST_INSTANCE'::text THEN 'checklist_instances'::text
            WHEN 'CHECKLIST_TASK_INSTANCE'::text THEN 'checklist_task_instances'::text
            WHEN 'MOTHER_JOURNEY'::text THEN 'mother_journeys'::text
            WHEN 'CARE_GROUP_MEMBER'::text THEN 'care_group_members'::text
            ELSE NULL::text
        END
        AND (
            after_payload_jsonb ->> 'sourceIdHash'::text
        ) ~ '^md5:[0-9a-f]{32}$'::text
        AND (
            after_payload_jsonb ->> 'reasonCode'::text
        ) = reason_code::text
        AND (
            after_payload_jsonb ->> 'disposition'::text
        ) = 'UNAVAILABLE'::text
        AND (
            after_payload_jsonb ->> 'correlationId'::text
        ) = correlation_id::text
        AND (
            after_payload_jsonb ->> 'metadata'::text
        ) = 'REDACTED'::text
        OR event_origin::text = 'CHECKLIST_MIGRATION'::text
        AND actor_type::text = 'SERVICE'::text
        AND actor_service::text = 'CHECKLIST_LEGACY_BACKFILL'::text
        AND actor_user_id IS NULL
        AND resource_id IS NOT NULL
        AND before_payload_jsonb IS NULL
        AND after_payload_jsonb IS NULL
        AND jsonb_typeof(payload) = 'object'::text
        AND payload ?& ARRAY[
            'sourceTable'::text,
            'sourceId'::text,
            'reasonCode'::text,
            'metadata'::text
        ]
        AND (
            payload - ARRAY[
                'sourceTable'::text,
                'sourceId'::text,
                'reasonCode'::text,
                'metadata'::text
            ]
        ) = '{}'::jsonb
        AND (
            payload ->> 'sourceTable'::text
        ) = 'preparation_checklist_items'::text
        AND (payload ->> 'sourceId'::text) = resource_id::text
        AND (
            payload ->> 'reasonCode'::text
        ) = reason_code::text
        AND (payload ->> 'metadata'::text) = 'REDACTED'::text
        OR event_origin::text = 'AUDIT_LOG'::text
        AND actor_type::text = 'SERVICE'::text
        AND actor_service::text = 'CHECKLIST_CONTEXT_AUTHORITY'::text
        AND actor_user_id IS NULL
        AND resource_type::text = 'CARE_GROUP_CONTEXT'::text
        AND resource_id IS NOT NULL
        AND before_payload_jsonb IS NULL
        AND after_payload_jsonb IS NULL
        AND jsonb_typeof(payload) = 'object'::text
        AND payload ?& ARRAY[
            'sourceTable'::text,
            'sourceId'::text,
            'reasonCode'::text,
            'contextType'::text,
            'contextId'::text,
            'metadata'::text
        ]
        AND (
            payload - ARRAY[
                'sourceTable'::text,
                'sourceId'::text,
                'reasonCode'::text,
                'contextType'::text,
                'contextId'::text,
                'metadata'::text
            ]
        ) = '{}'::jsonb
        AND (
            payload ->> 'sourceTable'::text
        ) = 'care_groups'::text
        AND (payload ->> 'sourceId'::text) = resource_id::text
        AND (
            payload ->> 'reasonCode'::text
        ) = reason_code::text
        AND (
            payload ->> 'contextType'::text
        ) = care_context_type::text
        AND (payload ->> 'contextId'::text) = care_context_id::text
        AND (payload ->> 'metadata'::text) = 'REDACTED'::text,
        false
    )
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_reason_code_ck" CHECK (
    event_category::text !~~ 'CHECKLIST_%'::text
    OR reason_code IS NULL
    OR reason_code::text ~ '^[A-Z0-9_]{1,80}$'::text
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_reason_required_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_SKIPPED'::character varying,
                'CHECKLIST_CANCELLED'::character varying,
                'CHECKLIST_RECONCILIATION_FAILED'::character varying,
                'CHECKLIST_MIGRATION_QUARANTINED'::character varying,
                'CHECKLIST_ACCESS_BASELINE'::character varying,
                'CHECKLIST_ACCESS_REVOKED'::character varying
            ]::text []
        )
    )
    OR reason_code IS NOT NULL
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_subject_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_DISTRIBUTED'::character varying,
                'CHECKLIST_ASSIGNED'::character varying,
                'CHECKLIST_COMPLETED'::character varying,
                'CHECKLIST_SKIPPED'::character varying,
                'CHECKLIST_REOPENED'::character varying,
                'CHECKLIST_CANCELLED'::character varying
            ]::text []
        )
    )
    OR subject_user_id IS NOT NULL
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_task_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_ASSIGNED'::character varying,
                'CHECKLIST_COMPLETED'::character varying,
                'CHECKLIST_SKIPPED'::character varying,
                'CHECKLIST_REOPENED'::character varying
            ]::text []
        )
    )
    OR checklist_task_instance_id IS NOT NULL
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_checklist_template_ck" CHECK (
    (
        event_category::text <> ALL (
            ARRAY[
                'CHECKLIST_DISTRIBUTED'::character varying,
                'CHECKLIST_ASSIGNED'::character varying
            ]::text []
        )
    )
    OR template_version_id IS NOT NULL
);

ALTER TABLE ONLY public."audit_events"
ADD CONSTRAINT "audit_events_security_note_text_ck" CHECK (
    event_category::text <> 'SECURITY_INVESTIGATION_NOTE'::text
    OR security_event_id IS NOT NULL
    AND length(
        TRIM(
            BOTH
            FROM note_text
        )
    ) > 0
);

ALTER TABLE ONLY public."auth_challenges"
ADD CONSTRAINT "auth_challenges_status_ck" CHECK (
    status::text = ANY (
        ARRAY[
            'PENDING'::character varying::text,
            'VERIFIED'::character varying::text,
            'USED'::character varying::text,
            'EXPIRED'::character varying::text,
            'REVOKED'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."auth_sessions"
ADD CONSTRAINT "auth_sessions_status_ck" CHECK (
    status::text = ANY (
        ARRAY[
            'ACTIVE'::character varying::text,
            'ROTATED'::character varying::text,
            'REVOKED'::character varying::text,
            'EXPIRED'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."care_facilities"
ADD CONSTRAINT "care_facilities_ownership_type_check" CHECK (
    ownership_type IS NULL
    OR (
        ownership_type::text = ANY (
            ARRAY[
                'PUBLIC'::character varying::text,
                'MILITARY'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."care_facilities"
ADD CONSTRAINT "care_facilities_searchable_coordinates_check" CHECK (
    is_searchable = false
    OR latitude IS NOT NULL
    AND longitude IS NOT NULL
);

ALTER TABLE ONLY public."care_facilities"
ADD CONSTRAINT "care_facilities_source_type_check" CHECK (
    source_type IS NULL
    OR (
        source_type::text = ANY (
            ARRAY[
                'MANUAL'::character varying::text,
                'TRACKASIA'::character varying::text,
                'LEGACY_IMPORT'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."care_group_members"
ADD CONSTRAINT "checklist_member_epoch_shape_ck" CHECK (
    checklist_access_quarantine_reason_code IS NOT NULL
    OR checklist_access_epoch IS NULL
    OR checklist_access_epoch >= 0
);

ALTER TABLE ONLY public."care_group_members"
ADD CONSTRAINT "checklist_member_marker_ck" CHECK (
    checklist_access_quarantine_reason_code IS NULL
    OR (
        checklist_access_quarantine_reason_code::text = ANY (
            ARRAY[
                'FAMILY_MEMBER_DUPLICATE'::character varying,
                'FAMILY_MEMBER_OWNER_ROLE'::character varying,
                'FAMILY_ACCESS_TIMELINE_MISMATCH'::character varying,
                'AUDIT_EVIDENCE_MISMATCH'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."care_group_members"
ADD CONSTRAINT "checklist_member_timeline_shape_ck" CHECK (
    checklist_access_quarantine_reason_code IS NOT NULL
    OR checklist_access_timeline_jsonb IS NULL
    OR jsonb_typeof(
        checklist_access_timeline_jsonb
    ) = 'object'::text
    AND NOT (
        checklist_access_timeline_jsonb ->> 'schema'::text
    ) IS DISTINCT FROM 'CHECKLIST_ACCESS_TIMELINE_V1'::text
    AND NOT jsonb_typeof(
        checklist_access_timeline_jsonb -> 'events'::text
    ) IS DISTINCT FROM 'array'::text
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_approved_gate_ck" CHECK (
    entry_type::text <> 'TEMPLATE_ROOT'::text
    OR content_status::text <> 'APPROVED'::text
    OR migration_review_required = false
    AND approved_at IS NOT NULL
    AND approved_by IS NOT NULL
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_distribution_gate_ck" CHECK (
    entry_type::text <> 'TEMPLATE_ROOT'::text
    OR distribution_enabled = false
    OR content_status::text = 'APPROVED'::text
    AND migration_review_required = false
    AND approved_at IS NOT NULL
    AND approved_by IS NOT NULL
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_due_timing_ck" CHECK (
    entry_type::text <> 'CHECKLIST_ENTRY'::text
    OR due_anchor_type IS NULL
    AND due_offset_start IS NULL
    AND due_offset_end IS NULL
    AND due_offset_unit IS NULL
    OR due_anchor_type IS NOT NULL
    AND due_offset_start IS NOT NULL
    AND due_offset_end IS NOT NULL
    AND due_offset_unit IS NOT NULL
    AND (
        due_anchor_type::text = ANY (
            ARRAY[
                'LMP'::character varying,
                'EDD'::character varying,
                'DELIVERY_DATE'::character varying,
                'BIRTH_DATE'::character varying
            ]::text []
        )
    )
    AND due_offset_start >= 0
    AND due_offset_end >= due_offset_start
    AND (
        due_offset_unit::text = ANY (
            ARRAY[
                'DAY'::character varying,
                'WEEK'::character varying,
                'MONTH'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_eligibility_anchor_domain_ck" CHECK (
    eligibility_anchor_type IS NULL
    OR (
        eligibility_anchor_type::text = ANY (
            ARRAY[
                'NONE'::character varying,
                'LMP'::character varying,
                'EDD'::character varying,
                'DELIVERY_DATE'::character varying,
                'BIRTH_DATE'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_eligibility_range_domain_ck" CHECK (
    eligibility_start_inclusive IS NULL
    AND eligibility_end_inclusive IS NULL
    OR eligibility_start_inclusive >= 0
    AND eligibility_end_inclusive >= eligibility_start_inclusive
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_eligibility_unit_domain_ck" CHECK (
    eligibility_range_unit IS NULL
    OR (
        eligibility_range_unit::text = ANY (
            ARRAY[
                'DAY'::character varying,
                'WEEK'::character varying,
                'MONTH'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_import_activation_gate_ck" CHECK (
    entry_type::text <> 'TEMPLATE_ROOT'::text
    OR migration_reviewed_at IS NULL
    OR content_status::text <> 'APPROVED'::text
    OR distribution_enabled = (
        template_type::text = 'MANDATORY'::text
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_recipient_scope_domain_ck" CHECK (
    recipient_scope IS NULL
    OR (
        recipient_scope::text = ANY (
            ARRAY[
                'MOTHER'::character varying,
                'FAMILY'::character varying,
                'BOTH'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_root_version_ck" CHECK (
    entry_type::text = 'TEMPLATE_ROOT'::text
    AND template_lineage_id IS NOT NULL
    AND template_version_id IS NOT NULL
    OR entry_type::text <> 'TEMPLATE_ROOT'::text
    AND template_lineage_id IS NULL
    AND template_version_id IS NULL
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_sequence_position_ck" CHECK (
    entry_type::text <> 'TEMPLATE_ROOT'::text
    OR display_order >= 0
    AND display_order <= 1000
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_sequence_scope_ck" CHECK (
    entry_type::text <> 'TEMPLATE_ROOT'::text
    OR display_order <= 0
    OR stage::text = 'PRE_PREGNANCY'::text
    AND template_type::text = 'MANDATORY'::text
    AND recipient_scope::text = 'MOTHER'::text
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_support_function_ck" CHECK (
    support_function_code IS NULL
    OR (
        support_function_code::text = ANY (
            ARRAY[
                'HEALTH_RECORDS'::character varying,
                'MATERNAL_HEALTH_METRICS'::character varying,
                'MATERNAL_EXERCISES'::character varying,
                'APPOINTMENTS'::character varying,
                'REMINDERS'::character varying,
                'JOURNEY'::character varying,
                'BABY_CARE'::character varying,
                'EXPERT_CONSULTATION'::character varying,
                'CONTENT_LIBRARY'::character varying,
                'AI_TRIAGE'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_target_contract_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR entry_type::text <> 'CHECKLIST_ENTRY'::text
    OR COALESCE(
        checklist_contract_version::integer,
        1
    ) = 2
    AND target_subject IS NULL
    OR COALESCE(
        checklist_contract_version::integer,
        1
    ) = 1
    AND target_subject IS NOT NULL
    AND (
        target_subject::text = ANY (
            ARRAY[
                'MOTHER'::character varying,
                'BABY'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "care_item_templates_type_ck" CHECK (
    entry_type::text = ANY (
        ARRAY[
            'TEMPLATE_ROOT'::character varying,
            'CHECKLIST_ENTRY'::character varying,
            'QUARANTINED_CHECKLIST_ENTRY'::character varying,
            'EXERCISE_TEMPLATE'::character varying,
            'POSTURE_CONFIG'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "checklist_template_cadence_shape_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR entry_type::text <> 'TEMPLATE_ROOT'::text
    OR COALESCE(
        schedule_type IS NULL
        AND materialization_policy IS NULL
        OR schedule_type::text = 'LEGACY'::text
        AND materialization_policy::text = 'LEGACY_WINDOW'::text
        OR schedule_type::text = 'SET'::text
        AND materialization_policy::text = 'SEQUENCE_STEP'::text
        OR schedule_type::text = 'WEEKLY'::text
        AND (
            materialization_policy::text = ANY (
                ARRAY[
                    'ONCE_PER_WINDOW'::character varying,
                    'EACH_WEEK'::character varying
                ]::text []
            )
        )
        OR schedule_type::text = 'DAILY'::text
        AND materialization_policy::text = 'EACH_DAY'::text,
        false
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "checklist_template_effective_bounds_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR effective_from IS NULL
    OR effective_to IS NULL
    OR effective_to > effective_from
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "checklist_template_marker_ck" CHECK (
    checklist_quarantine_reason_code IS NULL
    OR (
        checklist_quarantine_reason_code::text = ANY (
            ARRAY[
                'TEMPLATE_AGGREGATE_CONTRADICTION'::character varying,
                'INSTANCE_AGGREGATE_CONTRADICTION'::character varying,
                'TASK_PARENT_CONTRACT_MISMATCH'::character varying,
                'JOURNEY_DATING_UNRESOLVED'::character varying,
                'JOURNEY_DATING_CONFLICT'::character varying,
                'FAMILY_MEMBER_DUPLICATE'::character varying,
                'FAMILY_MEMBER_OWNER_ROLE'::character varying,
                'FAMILY_ACCESS_TIMELINE_MISMATCH'::character varying,
                'AUDIT_EVIDENCE_MISMATCH'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "checklist_template_metadata_shape_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR checklist_metadata_jsonb IS NULL
    OR jsonb_typeof(checklist_metadata_jsonb) = 'object'::text
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "checklist_template_schedule_fields_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR (
        schedule_group_key IS NULL
        OR btrim(schedule_group_key::text) <> ''::text
        AND schedule_group_key::text ~ '^[A-Za-z0-9][A-Za-z0-9_.:-]{0,119}$'::text
    )
    AND (
        schedule_context_type IS NULL
        OR (
            schedule_context_type::text = ANY (
                ARRAY[
                    'JOURNEY'::character varying,
                    'BABY'::character varying
                ]::text []
            )
        )
    )
    AND (
        schedule_end_mode IS NULL
        OR (
            schedule_end_mode::text = ANY (
                ARRAY[
                    'NONE'::character varying,
                    'FIXED_OFFSET'::character varying,
                    'STAGE_EXIT'::character varying
                ]::text []
            )
        )
    )
    AND (
        week_boundary_rule IS NULL
        OR (
            week_boundary_rule::text = ANY (
                ARRAY[
                    'NONE'::character varying,
                    'ANCHOR_RELATIVE_7D'::character varying
                ]::text []
            )
        )
    )
    AND (
        checklist_metadata_jsonb IS NULL
    ) = (
        checklist_metadata_hash IS NULL
    )
    AND (
        checklist_metadata_hash IS NULL
        OR checklist_metadata_hash::text ~ '^[0-9A-Fa-f]{64}([0-9A-Fa-f]{64})?$'::text
    )
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "checklist_template_v2_requiredness_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR entry_type::text <> 'CHECKLIST_ENTRY'::text
    OR COALESCE(
        checklist_contract_version::integer,
        1
    ) <> 2
    OR is_required IS NOT NULL
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "chk_care_item_templates_posture_confidence_threshold" CHECK (
    entry_type::text <> 'POSTURE_CONFIG'::text
    OR confidence_threshold IS NULL
    OR confidence_threshold >= 0.0
    AND confidence_threshold <= 1.0
);

ALTER TABLE ONLY public."care_item_templates"
ADD CONSTRAINT "ck_care_item_templates_template_type" CHECK (
    template_type::text = ANY (
        ARRAY[
            'MANDATORY'::character varying,
            'OPTIONAL'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."care_subjects"
ADD CONSTRAINT "care_subjects_baby_no_mother_journey_ck" CHECK (
    subject_type::text <> 'BABY'::text
    OR mother_journey_id IS NULL
);

ALTER TABLE ONLY public."care_subjects"
ADD CONSTRAINT "care_subjects_type_ck" CHECK (
    subject_type::text = ANY (
        ARRAY[
            'MOTHER'::character varying::text,
            'BABY'::character varying::text,
            'DEPENDENT'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."care_tasks"
ADD CONSTRAINT "care_tasks_origin_ck" CHECK (
    origin::text = ANY (
        ARRAY[
            'SYSTEM_TEMPLATE'::character varying,
            'USER_CREATED'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."care_tasks"
ADD CONSTRAINT "care_tasks_reminder_occurrence_generation_ck" CHECK (
    reminder_occurrence_generation >= 0
);

ALTER TABLE ONLY public."care_tasks"
ADD CONSTRAINT "care_tasks_target_subject_ck" CHECK (
    target_subject::text = ANY (
        ARRAY[
            'MOTHER'::character varying,
            'BABY'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."care_tasks"
ADD CONSTRAINT "care_tasks_type_ck" CHECK (
    task_type::text = ANY (
        ARRAY[
            'SCHEDULED_REMINDER'::character varying::text,
            'MANUAL_TASK'::character varying::text,
            'CARE_LOG'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."care_tasks"
ADD CONSTRAINT "care_tasks_vaccination_ck" CHECK (
    task_type::text <> 'SCHEDULED_REMINDER'::text
    OR source_reference_type::text <> 'VACCINATION'::text
    OR vaccination_record_id IS NOT NULL
    OR care_subject_id IS NOT NULL
);

ALTER TABLE ONLY public."checklist_action_commands"
ADD CONSTRAINT "checklist_action_commands_action_ck" CHECK (
    action_type::text = ANY (
        ARRAY[
            'COMPLETE'::character varying,
            'SKIP'::character varying,
            'REOPEN'::character varying,
            'ADVANCE'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."checklist_action_commands"
ADD CONSTRAINT "checklist_action_commands_payload_ck" CHECK (
    payload_hash ~ '^[0-9a-f]{64}$'::text
);

ALTER TABLE ONLY public."checklist_action_commands"
ADD CONSTRAINT "checklist_action_commands_reminder_definition_ck" CHECK (
    task_kind::text = 'REMINDER'::text
    AND reminder_definition_id IS NOT NULL
    OR task_kind::text <> 'REMINDER'::text
    AND reminder_definition_id IS NULL
);

ALTER TABLE ONLY public."checklist_action_commands"
ADD CONSTRAINT "checklist_action_commands_result_ck" CHECK (
    result_status::text = ANY (
        ARRAY[
            'APPLIED'::character varying,
            'IDEMPOTENT_REPLAY'::character varying,
            'REJECTED'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."checklist_action_commands"
ADD CONSTRAINT "checklist_action_commands_sequence_action_ck" CHECK (
    task_kind::text = 'CHECKLIST_SEQUENCE'::text
    AND action_type::text = 'ADVANCE'::text
    OR task_kind::text <> 'CHECKLIST_SEQUENCE'::text
    AND action_type::text <> 'ADVANCE'::text
);

ALTER TABLE ONLY public."checklist_action_commands"
ADD CONSTRAINT "checklist_action_commands_task_kind_ck" CHECK (
    task_kind::text = ANY (
        ARRAY[
            'CHECKLIST'::character varying,
            'CARE_TASK'::character varying,
            'REMINDER'::character varying,
            'CHECKLIST_SEQUENCE'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instance_contract_shape_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR checklist_contract_version IS NULL
    OR (
        checklist_contract_version = ANY (ARRAY[1, 2])
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instance_marker_ck" CHECK (
    checklist_quarantine_reason_code IS NULL
    OR (
        checklist_quarantine_reason_code::text = ANY (
            ARRAY[
                'TEMPLATE_AGGREGATE_CONTRADICTION'::character varying,
                'INSTANCE_AGGREGATE_CONTRADICTION'::character varying,
                'TASK_PARENT_CONTRACT_MISMATCH'::character varying,
                'JOURNEY_DATING_UNRESOLVED'::character varying,
                'JOURNEY_DATING_CONFLICT'::character varying,
                'FAMILY_MEMBER_DUPLICATE'::character varying,
                'FAMILY_MEMBER_OWNER_ROLE'::character varying,
                'FAMILY_ACCESS_TIMELINE_MISMATCH'::character varying,
                'AUDIT_EVIDENCE_MISMATCH'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instance_materialization_shape_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR materialization_mode IS NULL
    OR (
        materialization_mode::text = ANY (
            ARRAY[
                'LEGACY'::character varying,
                'EVENT'::character varying,
                'INTERACTIVE'::character varying,
                'CATCH_UP'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instance_member_epoch_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR recipient_role::text <> 'FAMILY'::text
    OR care_group_member_id IS NOT NULL
    AND checklist_access_epoch IS NOT NULL
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instance_member_scope_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR recipient_role::text = 'FAMILY'::text
    AND care_group_id IS NOT NULL
    AND care_group_member_id IS NOT NULL
    OR recipient_role::text <> 'FAMILY'::text
    AND care_group_member_id IS NULL
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instance_period_shape_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR (
        period_key IS NULL
        OR period_key::text ~ '^[A-Za-z0-9][A-Za-z0-9_.:/-]{0,179}$'::text
    )
    AND (
        schedule_zone_id IS NULL
        OR schedule_zone_id::text ~ '^[A-Za-z0-9._+-]+(?:/[A-Za-z0-9._+-]+)*$'::text
    )
    AND (
        gestational_dating_revision IS NULL
        OR gestational_dating_revision > 0
    )
    AND (
        COALESCE(
            checklist_contract_version::integer,
            1
        ) <> 2
        OR period_key IS NOT NULL
        AND schedule_zone_id IS NOT NULL
        AND materialization_mode IS NOT NULL
        AND was_actionable IS NOT NULL
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instance_task_contract_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR (
        COALESCE(
            checklist_contract_version::integer,
            1
        ) = ANY (ARRAY[1, 2])
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_context_ck" CHECK (
    care_context_type::text = ANY (
        ARRAY[
            'JOURNEY'::character varying,
            'BABY'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_family_group_scope_ck" CHECK (
    recipient_role::text <> 'FAMILY'::text
    OR care_group_id IS NOT NULL
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_history_pair_ck" CHECK (
    historical_at IS NULL
    AND history_reason_code IS NULL
    OR historical_at IS NOT NULL
    AND history_reason_code IS NOT NULL
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_history_reason_ck" CHECK (
    history_reason_code IS NULL
    OR length(history_reason_code::text) <= 80
    AND (
        history_reason_code::text ~~ 'LIFECYCLE_STAGE_OBSOLETE%'::text
        OR history_reason_code::text = 'SEQUENCE_STEP_COMPLETED'::text
        OR (
            history_reason_code::text = ANY (
                ARRAY[
                    'CADENCE_PERIOD_CLOSED'::character varying,
                    'CADENCE_SCOPE_EXIT'::character varying,
                    'DATING_CORRECTED'::character varying,
                    'ACCESS_REVOKED'::character varying
                ]::text []
            )
        )
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_key_format_ck" CHECK (
    distribution_key ~ '^[0-9a-f]{64}$'::text
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_mother_recipient_ck" CHECK (
    recipient_role::text = 'MOTHER'::text
    AND recipient_user_id = context_owner_user_id
    OR recipient_role::text = 'FAMILY'::text
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_origin_ck" CHECK (
    origin::text = ANY (
        ARRAY[
            'SYSTEM_TEMPLATE'::character varying,
            'USER_CREATED'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_role_ck" CHECK (
    recipient_role::text = ANY (
        ARRAY[
            'MOTHER'::character varying,
            'FAMILY'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_status_ck" CHECK (
    status::text = ANY (
        ARRAY[
            'PENDING'::character varying,
            'IN_PROGRESS'::character varying,
            'COMPLETED'::character varying,
            'CANCELLED'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_template_pair_ck" CHECK (
    origin::text = 'SYSTEM_TEMPLATE'::text
    AND template_lineage_id IS NOT NULL
    AND template_version_id IS NOT NULL
    OR origin::text = 'USER_CREATED'::text
    AND template_lineage_id IS NULL
    AND template_version_id IS NULL
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_terminal_shape_ck" CHECK (
    status::text = 'COMPLETED'::text
    AND completed_at IS NOT NULL
    AND cancelled_at IS NULL
    OR status::text = 'CANCELLED'::text
    AND cancelled_at IS NOT NULL
    AND completed_at IS NULL
    OR (
        status::text = ANY (
            ARRAY[
                'PENDING'::character varying,
                'IN_PROGRESS'::character varying
            ]::text []
        )
    )
    AND completed_at IS NULL
    AND cancelled_at IS NULL
);

ALTER TABLE ONLY public."checklist_instances"
ADD CONSTRAINT "checklist_instances_window_ck" CHECK (
    window_start IS NULL
    AND window_end IS NULL
    OR window_start IS NOT NULL
    AND window_end IS NOT NULL
    AND window_end >= window_start
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_contract_shape_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR checklist_contract_version IS NULL
    OR (
        checklist_contract_version = ANY (ARRAY[1, 2])
    )
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_instances_category_ck" CHECK (
    category::text = ANY (
        ARRAY[
            'DELIVERY'::character varying,
            'PAPERWORK'::character varying,
            'BABY_CARE'::character varying,
            'GENERAL'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_instances_key_format_ck" CHECK (
    task_key ~ '^[0-9a-f]{64}$'::text
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_instances_status_ck" CHECK (
    status::text = ANY (
        ARRAY[
            'PENDING'::character varying,
            'IN_PROGRESS'::character varying,
            'COMPLETED'::character varying,
            'SKIPPED'::character varying,
            'CANCELLED'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_instances_support_function_ck" CHECK (
    support_function_code IS NULL
    OR (
        support_function_code::text = ANY (
            ARRAY[
                'HEALTH_RECORDS'::character varying,
                'MATERNAL_HEALTH_METRICS'::character varying,
                'MATERNAL_EXERCISES'::character varying,
                'APPOINTMENTS'::character varying,
                'REMINDERS'::character varying,
                'JOURNEY'::character varying,
                'BABY_CARE'::character varying,
                'EXPERT_CONSULTATION'::character varying,
                'CONTENT_LIBRARY'::character varying,
                'AI_TRIAGE'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_instances_template_pair_ck" CHECK (
    (template_version_id IS NULL) = (
        template_item_version_id IS NULL
    )
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_instances_terminal_shape_ck" CHECK (
    status::text = 'COMPLETED'::text
    AND completed_at IS NOT NULL
    AND skipped_at IS NULL
    AND cancelled_at IS NULL
    OR status::text = 'SKIPPED'::text
    AND skipped_at IS NOT NULL
    AND completed_at IS NULL
    AND cancelled_at IS NULL
    OR status::text = 'CANCELLED'::text
    AND cancelled_at IS NOT NULL
    AND completed_at IS NULL
    AND skipped_at IS NULL
    OR (
        status::text = ANY (
            ARRAY[
                'PENDING'::character varying,
                'IN_PROGRESS'::character varying
            ]::text []
        )
    )
    AND completed_at IS NULL
    AND skipped_at IS NULL
    AND cancelled_at IS NULL
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_marker_ck" CHECK (
    checklist_quarantine_reason_code IS NULL
    OR (
        checklist_quarantine_reason_code::text = ANY (
            ARRAY[
                'TEMPLATE_AGGREGATE_CONTRADICTION'::character varying,
                'INSTANCE_AGGREGATE_CONTRADICTION'::character varying,
                'TASK_PARENT_CONTRACT_MISMATCH'::character varying,
                'JOURNEY_DATING_UNRESOLVED'::character varying,
                'JOURNEY_DATING_CONFLICT'::character varying,
                'FAMILY_MEMBER_DUPLICATE'::character varying,
                'FAMILY_MEMBER_OWNER_ROLE'::character varying,
                'FAMILY_ACCESS_TIMELINE_MISMATCH'::character varying,
                'AUDIT_EVIDENCE_MISMATCH'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_parent_contract_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR (
        COALESCE(
            checklist_contract_version::integer,
            1
        ) = ANY (ARRAY[1, 2])
    )
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_target_contract_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR COALESCE(
        checklist_contract_version::integer,
        1
    ) = 2
    AND target_subject IS NULL
    OR COALESCE(
        checklist_contract_version::integer,
        1
    ) = 1
    AND target_subject IS NOT NULL
    AND (
        target_subject::text = ANY (
            ARRAY[
                'MOTHER'::character varying,
                'BABY'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_v2_requiredness_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR COALESCE(
        checklist_contract_version::integer,
        1
    ) <> 2
    OR is_required IS NOT NULL
);

ALTER TABLE ONLY public."checklist_task_instances"
ADD CONSTRAINT "checklist_task_v2_targetless_ck" CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR COALESCE(
        checklist_contract_version::integer,
        1
    ) <> 2
    OR target_subject IS NULL
);

ALTER TABLE ONLY public."community_content"
ADD CONSTRAINT "community_content_image_urls_ck" CHECK (
    jsonb_typeof(image_urls) = 'array'::text
    AND jsonb_array_length(image_urls) <= 3
);

ALTER TABLE ONLY public."community_content"
ADD CONSTRAINT "community_content_type_ck" CHECK (
    content_type::text = ANY (
        ARRAY[
            'QUESTION'::character varying::text,
            'ANSWER'::character varying::text,
            'POST'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."community_interactions"
ADD CONSTRAINT "community_interactions_one_target_ck" CHECK (
    (content_id IS NOT NULL) <> (topic_id IS NOT NULL)
);

ALTER TABLE ONLY public."community_interactions"
ADD CONSTRAINT "community_interactions_type_ck" CHECK (
    interaction_type::text = ANY (
        ARRAY[
            'REACTION'::character varying::text,
            'BOOKMARK'::character varying::text,
            'FOLLOW'::character varying::text,
            'MUTE'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."community_topics"
ADD CONSTRAINT "community_topics_parent_rule_check_v2" CHECK (
    type::text = 'CATEGORY'::text
    AND parent_id IS NULL
    OR type::text = 'TOPIC'::text
    AND parent_id IS NOT NULL
    OR type::text = 'TAG'::text
    AND parent_id IS NULL
);

ALTER TABLE ONLY public."community_topics"
ADD CONSTRAINT "community_topics_type_check" CHECK (
    type::text = ANY (
        ARRAY[
            'TOPIC'::character varying::text,
            'CATEGORY'::character varying::text,
            'TAG'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."consultation_bookings"
ADD CONSTRAINT "consultation_bookings_session_order_ck" CHECK (
    session_started_at IS NULL
    OR session_ended_at IS NULL
    OR session_ended_at >= session_started_at
);

ALTER TABLE ONLY public."consultation_bookings"
ADD CONSTRAINT "consultation_bookings_session_shape_ck" CHECK (
    (
        session_ended_at IS NULL
        OR session_started_at IS NOT NULL
    )
    AND (
        session_status IS NULL
        OR upper(session_status::text) <> 'COMPLETED'::text
        OR session_started_at IS NOT NULL
        AND session_ended_at IS NOT NULL
    )
);

ALTER TABLE ONLY public."consultation_context_citations"
ADD CONSTRAINT "chk_context_citation_approved" CHECK (
    source_status_at_share::text = 'APPROVED'::text
);

ALTER TABLE ONLY public."consultation_context_citations"
ADD CONSTRAINT "chk_context_citation_https" CHECK (
    source_url::text ~~ 'https://%'::text
);

ALTER TABLE ONLY public."consultation_context_citations"
ADD CONSTRAINT "chk_context_citation_ordinal" CHECK (ordinal >= 0);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "chk_context_completed" CHECK (
    intake_status::text = 'COMPLETED'::text
);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "chk_context_origin" CHECK (
    origin_dashboard::text = ANY (
        ARRAY[
            'MOTHER_JOURNEY'::character varying::text,
            'BABY_PROFILE'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "chk_context_origin_journey" CHECK (
    origin_dashboard::text = 'MOTHER_JOURNEY'::text
    AND journey_id IS NOT NULL
    AND origin_reference_id = journey_id
    AND (
        triage_stage::text = ANY (
            ARRAY[
                'PRECONCEPTION'::character varying,
                'PREGNANCY'::character varying,
                'POSTPARTUM'::character varying
            ]::text []
        )
    )
    OR origin_dashboard::text = 'BABY_PROFILE'::text
    AND journey_id IS NULL
    AND (
        triage_stage::text = ANY (
            ARRAY[
                'INFANT'::character varying,
                'TODDLER'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "chk_context_policy" CHECK (
    share_policy_version::text = 'YELLOW_EXPERT_CONTEXT_V1'::text
);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "chk_context_stage" CHECK (
    triage_stage::text = ANY (
        ARRAY[
            'PRECONCEPTION'::character varying::text,
            'PREGNANCY'::character varying::text,
            'POSTPARTUM'::character varying::text,
            'INFANT'::character varying::text,
            'TODDLER'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "chk_context_summary" CHECK (
    length(btrim(risk_summary::text)) >= 1
    AND length(btrim(risk_summary::text)) <= 500
);

ALTER TABLE ONLY public."consultation_context_shares"
ADD CONSTRAINT "chk_context_yellow" CHECK (
    risk_level::text = 'YELLOW'::text
);

ALTER TABLE ONLY public."content_items"
ADD CONSTRAINT "content_items_recommendation_metadata_ck" CHECK (
    recommendation_priority >= 0
    AND recommendation_priority <= 100
    AND (
        eligible_from_week IS NULL
        AND eligible_to_week IS NULL
        OR eligible_from_week IS NOT NULL
        AND eligible_to_week IS NOT NULL
        AND eligible_from_week >= 0
        AND eligible_from_week <= 42
        AND eligible_to_week >= 0
        AND eligible_to_week <= 42
        AND eligible_from_week <= eligible_to_week
    )
    AND (
        stage::text = 'PREGNANCY'::text
        OR stage::text IS DISTINCT FROM 'PREGNANCY'::text
        AND eligible_from_week IS NULL
        AND eligible_to_week IS NULL
    )
    AND (
        content_type::text = 'ARTICLE'::text
        OR content_type::text IS DISTINCT FROM 'ARTICLE'::text
        AND eligible_from_week IS NULL
        AND eligible_to_week IS NULL
        AND recommendation_priority = 0
    )
);

ALTER TABLE ONLY public."conversation_calls"
ADD CONSTRAINT "conversation_calls_answered_ck" CHECK (
    answered_at IS NULL
    OR answered_at >= initiated_at
);

ALTER TABLE ONLY public."conversation_calls"
ADD CONSTRAINT "conversation_calls_duration_ck" CHECK (
    duration_seconds IS NULL
    OR duration_seconds >= 0
);

ALTER TABLE ONLY public."conversation_calls"
ADD CONSTRAINT "conversation_calls_ended_answered_ck" CHECK (
    call_status::text <> 'ENDED'::text
    OR answered_at IS NOT NULL
);

ALTER TABLE ONLY public."conversation_calls"
ADD CONSTRAINT "conversation_calls_ended_ck" CHECK (
    ended_at IS NULL
    OR ended_at >= initiated_at
);

ALTER TABLE ONLY public."conversation_calls"
ADD CONSTRAINT "conversation_calls_status_ck" CHECK (
    call_status::text = ANY (
        ARRAY[
            'INITIATED'::character varying::text,
            'RINGING'::character varying::text,
            'ANSWERED'::character varying::text,
            'DECLINED'::character varying::text,
            'MISSED'::character varying::text,
            'CANCELLED'::character varying::text,
            'ENDED'::character varying::text,
            'FAILED'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."conversation_calls"
ADD CONSTRAINT "conversation_calls_type_ck" CHECK (
    call_type::text = ANY (
        ARRAY[
            'VOICE'::character varying::text,
            'VIDEO'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."device_tokens"
ADD CONSTRAINT "device_tokens_platform_check" CHECK (
    platform::text = ANY (
        ARRAY[
            'ANDROID'::character varying::text,
            'IOS'::character varying::text,
            'WEB'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."direct_conversations"
ADD CONSTRAINT "direct_conversations_activity_ck" CHECK (
    last_activity_at IS NULL
    OR last_activity_at >= created_at
);

ALTER TABLE ONLY public."direct_conversations"
ADD CONSTRAINT "direct_conversations_status_ck" CHECK (status::text = 'ACTIVE'::text);

ALTER TABLE ONLY public."direct_messages"
ADD CONSTRAINT "direct_messages_payload_check" CHECK (
    (
        message_type::text = ANY (
            ARRAY[
                'TEXT'::character varying,
                'IMAGE'::character varying,
                'FILE'::character varying,
                'LOCATION'::character varying
            ]::text []
        )
    )
    AND (
        message_type::text = 'TEXT'::text
        AND message_body IS NOT NULL
        AND attachment_id IS NULL
        AND location_latitude IS NULL
        AND location_longitude IS NULL
        AND location_label IS NULL
        AND recalled_at IS NULL
        OR (
            message_type::text = ANY (
                ARRAY[
                    'IMAGE'::character varying,
                    'FILE'::character varying
                ]::text []
            )
        )
        AND attachment_id IS NOT NULL
        AND location_latitude IS NULL
        AND location_longitude IS NULL
        AND location_label IS NULL
        AND recalled_at IS NULL
        OR message_type::text = 'LOCATION'::text
        AND attachment_id IS NULL
        AND message_body IS NULL
        AND location_latitude >= '-90'::integer::double precision
        AND location_latitude <= 90::double precision
        AND location_longitude >= '-180'::integer::double precision
        AND location_longitude <= 180::double precision
        AND recalled_at IS NULL
        OR recalled_at IS NOT NULL
        AND message_body IS NULL
        AND attachment_id IS NULL
        AND location_latitude IS NULL
        AND location_longitude IS NULL
        AND location_label IS NULL
    )
);

ALTER TABLE ONLY public."expert_consultation_requests"
ADD CONSTRAINT "expert_consultation_requests_expiry_ck" CHECK (expires_at > created_at);

ALTER TABLE ONLY public."expert_consultation_requests"
ADD CONSTRAINT "expert_consultation_requests_responded_ck" CHECK (
    status::text = 'PENDING'::text
    OR responded_at IS NOT NULL
);

ALTER TABLE ONLY public."expert_consultation_requests"
ADD CONSTRAINT "expert_consultation_requests_status_ck" CHECK (
    status::text = ANY (
        ARRAY[
            'PENDING'::character varying::text,
            'ACCEPTED'::character varying::text,
            'REJECTED'::character varying::text,
            'CANCELLED'::character varying::text,
            'EXPIRED'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."expert_consultation_requests"
ADD CONSTRAINT "expert_consultation_requests_window_ck" CHECK (
    preferred_window_start IS NULL
    AND preferred_window_end IS NULL
    OR preferred_window_start IS NOT NULL
    AND preferred_window_end IS NOT NULL
    AND preferred_window_end > preferred_window_start
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_aggregation_json_ck" CHECK (
    jsonb_typeof(aggregation_policy_jsonb) = 'object'::text
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_chart_json_ck" CHECK (
    jsonb_typeof(chart_policy_jsonb) = 'object'::text
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_code_ck" CHECK (
    btrim(metric_code::text) <> ''::text
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_context_json_ck" CHECK (
    jsonb_typeof(required_context_schema_jsonb) = 'object'::text
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_display_name_ck" CHECK (
    btrim(display_name::text) <> ''::text
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_effective_period_ck" CHECK (
    effective_until IS NULL
    OR effective_until > effective_from
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_plausibility_json_ck" CHECK (
    jsonb_typeof(plausibility_policy_jsonb) = 'object'::text
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_precision_ck" CHECK (
    precision_scale IS NULL
    OR precision_scale >= 0
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_quality_json_ck" CHECK (
    jsonb_typeof(quality_policy_jsonb) = 'object'::text
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_shape_ck" CHECK (
    observation_shape::text = ANY (
        ARRAY[
            'POINT'::character varying,
            'PAIRED_POINT'::character varying,
            'SESSION'::character varying,
            'INTERVAL_AGGREGATE'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_stages_json_ck" CHECK (
    jsonb_typeof(allowed_journey_stages_jsonb) = 'array'::text
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_subject_ck" CHECK (
    subject_type::text = ANY (
        ARRAY[
            'MOTHER'::character varying,
            'BABY'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_units_json_ck" CHECK (
    jsonb_typeof(accepted_input_units_jsonb) = 'array'::text
);

ALTER TABLE ONLY public."health_metric_definitions"
ADD CONSTRAINT "health_metric_definitions_version_ck" CHECK (version > 0);

ALTER TABLE ONLY public."health_observations"
ADD CONSTRAINT "health_observations_p0_context_json_ck" CHECK (
    jsonb_typeof(context_jsonb) = 'object'::text
);

ALTER TABLE ONLY public."health_observations"
ADD CONSTRAINT "health_observations_p0_period_ck" CHECK (
    period_end IS NULL
    OR period_start IS NOT NULL
    AND period_end > period_start
);

ALTER TABLE ONLY public."health_observations"
ADD CONSTRAINT "health_observations_type_ck" CHECK (
    subject_type::text = ANY (
        ARRAY[
            'MOTHER'::character varying::text,
            'BABY'::character varying::text,
            'DEPENDENT'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."moderation_cases"
ADD CONSTRAINT "chk_moderation_cases_ai_feedback_decision" CHECK (
    ai_feedback_decision IS NULL
    OR (
        ai_feedback_decision::text = ANY (
            ARRAY[
                'AGREE'::character varying::text,
                'DISAGREE'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "chk_mother_journeys_date_confidence" CHECK (
    date_confidence IS NULL
    OR (
        date_confidence::text = ANY (
            ARRAY[
                'CONFIRMED'::character varying::text,
                'ESTIMATED'::character varying::text,
                'UNKNOWN'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "chk_mother_journeys_date_source" CHECK (
    date_source IS NULL
    OR (
        date_source::text = ANY (
            ARRAY[
                'SELF_REPORTED'::character varying::text,
                'CLINICIAN_CONFIRMED'::character varying::text,
                'ULTRASOUND'::character varying::text,
                'SYSTEM_DERIVED'::character varying::text,
                'MIGRATION'::character varying::text,
                'UNKNOWN'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "ck_mother_journey_live_birth_date" CHECK (
    pregnancy_outcome::text <> 'LIVE_BIRTH'::text
    OR pregnancy_outcome_date IS NOT NULL
);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "ck_mother_journey_pregnancy_outcome" CHECK (
    pregnancy_outcome IS NULL
    OR (
        pregnancy_outcome::text = ANY (
            ARRAY[
                'ONGOING'::character varying::text,
                'UNKNOWN'::character varying::text,
                'LIVE_BIRTH'::character varying::text,
                'PREGNANCY_LOSS'::character varying::text,
                'STILLBIRTH'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "mother_journey_dating_basis_ck" CHECK (
    gestational_dating_quarantine_reason_code IS NOT NULL
    OR gestational_dating_basis IS NULL
    OR (
        gestational_dating_basis::text = ANY (
            ARRAY[
                'LMP'::character varying,
                'EDD'::character varying,
                'LMP_DERIVED_FROM_EDD'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "mother_journey_dating_marker_ck" CHECK (
    gestational_dating_quarantine_reason_code IS NULL
    OR (
        gestational_dating_quarantine_reason_code::text = ANY (
            ARRAY[
                'JOURNEY_DATING_UNRESOLVED'::character varying,
                'JOURNEY_DATING_CONFLICT'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "mother_journey_dating_pair_ck" CHECK (
    gestational_dating_quarantine_reason_code IS NOT NULL
    OR gestational_dating_basis IS NULL
    AND gestational_dating_revision IS NULL
    AND gestational_dating_effective_at IS NULL
    OR gestational_dating_basis IS NOT NULL
    AND gestational_dating_revision IS NOT NULL
    AND gestational_dating_revision > 0
    AND gestational_dating_effective_at IS NOT NULL
);

ALTER TABLE ONLY public."mother_journeys"
ADD CONSTRAINT "mother_journeys_recommendation_profile_state_ck" CHECK (
    (
        recommendation_profile_status::text = ANY (
            ARRAY[
                'ACTIVE'::character varying,
                'REVIEW_REQUIRED'::character varying
            ]::text []
        )
    )
    AND recommendation_profile_version = 1
    AND jsonb_typeof(recommendation_profile_jsonb) = 'object'::text
    AND recommendation_profile_jsonb <> '{}'::jsonb
    AND recommendation_profile_completed_at IS NOT NULL
    OR (
        recommendation_profile_status::text = ANY (
            ARRAY[
                'NOT_STARTED'::character varying,
                'DECLINED'::character varying,
                'RECONSENT_REQUIRED'::character varying,
                'REVOKED'::character varying
            ]::text []
        )
    )
    AND recommendation_profile_version = 0
    AND recommendation_profile_jsonb = '{}'::jsonb
    AND recommendation_profile_completed_at IS NULL
);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_attempt_ck" CHECK (attempt_count >= 0);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_branch_ck" CHECK (
    job_type::text = 'REMINDER_SCHEDULE'::text
    AND schedule_id IS NOT NULL
    AND schedule_revision IS NOT NULL
    AND occurrence_date IS NOT NULL
    AND local_time IS NOT NULL
    AND time_zone IS NOT NULL
    AND reminder_id IS NULL
    AND occurrence_id IS NULL
    AND occurrence_generation IS NULL
    AND occurrence_scheduled_at IS NULL
    AND config_revision IS NULL
    AND offset_minutes IS NULL
    OR job_type::text = 'APPOINTMENT'::text
    AND reminder_id IS NOT NULL
    AND occurrence_id IS NOT NULL
    AND occurrence_generation IS NOT NULL
    AND occurrence_scheduled_at IS NOT NULL
    AND config_revision IS NOT NULL
    AND offset_minutes IS NOT NULL
    AND schedule_id IS NULL
    AND schedule_revision IS NULL
    AND occurrence_date IS NULL
    AND local_time IS NULL
    AND time_zone IS NULL
);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_config_revision_ck" CHECK (
    config_revision IS NULL
    OR config_revision > 0
);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_generation_ck" CHECK (
    occurrence_generation IS NULL
    OR occurrence_generation >= 0
);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_lock_ck" CHECK (
    status::text = 'PROCESSING'::text
    AND locked_by IS NOT NULL
    AND locked_at IS NOT NULL
    OR status::text <> 'PROCESSING'::text
);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_offset_ck" CHECK (
    offset_minutes IS NULL
    OR offset_minutes >= '-43200'::integer
    AND offset_minutes <= 10080
);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_schedule_revision_ck" CHECK (
    schedule_revision IS NULL
    OR schedule_revision > 0
);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_status_ck" CHECK (
    status::text = ANY (
        ARRAY[
            'PENDING'::character varying,
            'PROCESSING'::character varying,
            'SENT'::character varying,
            'FAILED'::character varying,
            'SUPPRESSED'::character varying,
            'CANCELLED'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."notification_jobs"
ADD CONSTRAINT "notification_jobs_type_ck" CHECK (
    job_type::text = ANY (
        ARRAY[
            'REMINDER_SCHEDULE'::character varying,
            'APPOINTMENT'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."notification_records"
ADD CONSTRAINT "notification_records_channel_check" CHECK (
    channel::text = ANY (
        ARRAY[
            'PUSH'::character varying::text,
            'EMAIL'::character varying::text,
            'IN_APP'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."notification_records"
ADD CONSTRAINT "notification_records_status_check" CHECK (
    status::text = ANY (
        ARRAY[
            'PENDING'::character varying::text,
            'PROCESSING'::character varying::text,
            'SENT'::character varying::text,
            'DELIVERED'::character varying::text,
            'FAILED'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."notification_records"
ADD CONSTRAINT "notification_records_type_check" CHECK (
    type::text = ANY (
        ARRAY[
            'REMINDER'::character varying,
            'COMMUNITY_REPLY'::character varying,
            'CONSULTATION'::character varying,
            'EMERGENCY'::character varying,
            'LOCATION_SHARE'::character varying,
            'MESSAGE'::character varying,
            'GROUP_INVITE'::character varying,
            'CONTENT_REVIEW'::character varying,
            'EPDS_RESULT'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."red_flag_rules"
ADD CONSTRAINT "chk_red_flag_rules_action" CHECK (
    action::text = ANY (
        ARRAY[
            'BLOCK'::character varying::text,
            'WARN'::character varying::text,
            'ESCALATE'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."red_flag_rules"
ADD CONSTRAINT "chk_red_flag_rules_severity" CHECK (
    severity::text = ANY (
        ARRAY[
            'GREEN'::character varying::text,
            'YELLOW'::character varying::text,
            'RED'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."reminder_schedules"
ADD CONSTRAINT "reminder_schedules_active_times_ck" CHECK (
    active = false
    OR cardinality(local_times) > 0
);

ALTER TABLE ONLY public."reminder_schedules"
ADD CONSTRAINT "reminder_schedules_dates_ck" CHECK (
    end_date IS NULL
    OR end_date >= start_date
);

ALTER TABLE ONLY public."reminder_schedules"
ADD CONSTRAINT "reminder_schedules_local_times_ck" CHECK (
    carebridge_validate_reminder_local_times (local_times)
);

ALTER TABLE ONLY public."reminder_schedules"
ADD CONSTRAINT "reminder_schedules_lock_version_ck" CHECK (lock_version >= 0);

ALTER TABLE ONLY public."reminder_schedules"
ADD CONSTRAINT "reminder_schedules_recurrence_ck" CHECK (
    recurrence::text = ANY (
        ARRAY[
            'NONE'::character varying,
            'DAILY'::character varying
        ]::text []
    )
);

ALTER TABLE ONLY public."reminder_schedules"
ADD CONSTRAINT "reminder_schedules_revision_ck" CHECK (revision > 0);

ALTER TABLE ONLY public."reminder_schedules"
ADD CONSTRAINT "reminder_schedules_timezone_ck" CHECK (
    length(
        TRIM(
            BOTH
            FROM time_zone
        )
    ) > 0
);

ALTER TABLE ONLY public."reminder_schedules"
ADD CONSTRAINT "reminder_schedules_title_ck" CHECK (
    length(
        TRIM(
            BOTH
            FROM title
        )
    ) > 0
);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_action_type_ck" CHECK (
    action_type IS NULL
    OR (
        action_type::text = ANY (
            ARRAY[
                'RESPONSE'::character varying,
                'DELIVERY'::character varying,
                'FAMILY_ALERT'::character varying,
                'ALERT_ATTEMPT'::character varying,
                'MAP_HANDOFF'::character varying,
                'LOCATION_SNAPSHOT'::character varying,
                'TRIAGE_ESCALATION'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_alert_claim_ck" CHECK (
    alert_status::text = 'PROCESSING'::text
    AND alert_claim_token IS NOT NULL
    AND alert_lease_expires_at IS NOT NULL
    OR alert_status::text IS DISTINCT FROM 'PROCESSING'::text
);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_alert_generation_ck" CHECK (alert_generation >= 0);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_alert_recipient_counts_ck" CHECK (
    alert_successful_recipient_count >= 0
    AND alert_failed_recipient_count >= 0
);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_alert_status_ck" CHECK (
    alert_status IS NULL
    OR (
        alert_status::text = ANY (
            ARRAY[
                'PROCESSING'::character varying::text,
                'FAILED'::character varying::text,
                'PARTIAL'::character varying::text,
                'NO_RECIPIENTS'::character varying::text,
                'SENT'::character varying::text,
                'SUPPRESSED'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_attempt_ck" CHECK (
    attempt_number IS NULL
    OR attempt_number >= 0
);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_parent_ck" CHECK (
    action_type IS NULL
    OR (
        action_type::text = ANY (
            ARRAY[
                'MAP_HANDOFF'::character varying::text,
                'LOCATION_SNAPSHOT'::character varying::text
            ]
        )
    )
    OR parent_event_id IS NOT NULL
);

ALTER TABLE ONLY public."safety_events"
ADD CONSTRAINT "safety_events_record_type_ck" CHECK (
    record_type::text = ANY (
        ARRAY[
            'IMU_EVENT'::character varying::text,
            'EMERGENCY_SESSION'::character varying::text,
            'SAFETY_ACTION'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."system_configurations"
ADD CONSTRAINT "system_configurations_api_rate_limit_check" CHECK (
    api_rate_limit >= 1
    AND api_rate_limit <= 100000
);

ALTER TABLE ONLY public."system_configurations"
ADD CONSTRAINT "system_configurations_connection_timeout_ms_check" CHECK (
    connection_timeout_ms >= 1000
    AND connection_timeout_ms <= 300000
);

ALTER TABLE ONLY public."system_configurations"
ADD CONSTRAINT "system_configurations_max_upload_size_mb_check" CHECK (
    max_upload_size_mb >= 1
    AND max_upload_size_mb <= 1024
);

ALTER TABLE ONLY public."triage_sessions"
ADD CONSTRAINT "chk_triage_completed_snapshot" CHECK (
    status::text <> 'COMPLETED'::text
    OR NULLIF(
        schema_version::text,
        ''::text
    ) IS NOT NULL
    AND NULLIF(content_hash::text, ''::text) IS NOT NULL
    AND jsonb_typeof(result_jsonb) = 'object'::text
);

ALTER TABLE ONLY public."triage_sessions"
ADD CONSTRAINT "chk_triage_lifecycle_binding" CHECK (
    journey_id IS NULL
    AND origin_dashboard IS NULL
    AND origin_reference_id IS NULL
    AND continuation_token IS NULL
    AND continuation_expires_at IS NULL
    AND continuation_acknowledged_at IS NULL
    OR origin_dashboard::text = 'MOTHER_JOURNEY'::text
    AND journey_id IS NOT NULL
    AND origin_reference_id IS NOT NULL
    AND continuation_token IS NOT NULL
    AND continuation_expires_at IS NOT NULL
    OR origin_dashboard::text = 'BABY_PROFILE'::text
    AND journey_id IS NULL
    AND origin_reference_id IS NOT NULL
    AND continuation_token IS NOT NULL
    AND continuation_expires_at IS NOT NULL
);

ALTER TABLE ONLY public."triage_sessions"
ADD CONSTRAINT "chk_triage_origin_dashboard" CHECK (
    origin_dashboard IS NULL
    OR (
        origin_dashboard::text = ANY (
            ARRAY[
                'MOTHER_JOURNEY'::character varying::text,
                'BABY_PROFILE'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."triage_sessions"
ADD CONSTRAINT "chk_triage_origin_stage" CHECK (
    origin_dashboard IS NULL
    OR origin_dashboard::text = 'MOTHER_JOURNEY'::text
    AND origin_reference_id = journey_id
    AND (
        stage::text = ANY (
            ARRAY[
                'PRECONCEPTION'::character varying,
                'PREGNANCY'::character varying,
                'POSTPARTUM'::character varying
            ]::text []
        )
    )
    OR origin_dashboard::text = 'BABY_PROFILE'::text
    AND journey_id IS NULL
    AND baby_profile_id IS NOT NULL
    AND origin_reference_id = baby_profile_id
    AND (
        stage::text = ANY (
            ARRAY[
                'INFANT'::character varying,
                'TODDLER'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."triage_sessions"
ADD CONSTRAINT "chk_triage_red_emergency" CHECK (
    risk_level::text <> 'RED'::text
    OR emergency
);

ALTER TABLE ONLY public."triage_sessions"
ADD CONSTRAINT "triage_sessions_intensity_ck" CHECK (
    intensity IS NULL
    OR (
        intensity::text = ANY (
            ARRAY[
                'LOW'::character varying::text,
                'MEDIUM'::character varying::text,
                'HIGH'::character varying::text
            ]
        )
    )
);

ALTER TABLE ONLY public."triage_sessions"
ADD CONSTRAINT "triage_sessions_origin_dashboard_check" CHECK (
    origin_dashboard::text = ANY (
        ARRAY[
            'MOTHER_JOURNEY'::character varying::text,
            'BABY_PROFILE'::character varying::text
        ]
    )
);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_deactivation_shape_ck" CHECK (
    account_status::text IS DISTINCT FROM 'DEACTIVATED'::text
    OR enabled = false
    AND deactivated_at IS NOT NULL
);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_lock_state_ck" CHECK (
    locked = false
    AND lock_type IS NULL
    AND lock_reason IS NULL
    AND locked_by IS NULL
    AND lock_episode_id IS NULL
    OR locked = true
    AND lock_type::text = 'TEMPORARY'::text
    AND lock_reason IS NULL
    AND locked_by IS NULL
    AND lock_episode_id IS NULL
    AND locked_at IS NOT NULL
    OR locked = true
    AND lock_type::text = 'ADMIN'::text
    AND lock_reason IS NOT NULL
    AND locked_by IS NOT NULL
    AND lock_episode_id IS NOT NULL
    AND locked_at IS NOT NULL
);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_lock_type_ck" CHECK (
    lock_type IS NULL
    OR (
        lock_type::text = ANY (
            ARRAY[
                'TEMPORARY'::character varying,
                'ADMIN'::character varying
            ]::text []
        )
    )
);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_role_check" CHECK (
    role IS NULL
    OR (
        role::text = ANY (
            ARRAY[
                'MOTHER'::text,
                'FAMILY'::text,
                'EXPERT'::text,
                'MODERATOR'::text,
                'CONTENT_ADMIN'::text,
                'SYSTEM_ADMIN'::text,
                'OPERATIONS'::text
            ]
        )
    )
);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_safety_countdown_ck" CHECK (
    emergency_countdown_seconds = ANY (ARRAY[15, 30, 60])
);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_safety_sensitivity_ck" CHECK (
    fall_detection_sensitivity_level::text = ANY (
        ARRAY[
            'LOW'::text,
            'MEDIUM'::text,
            'HIGH'::text
        ]
    )
);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_sensor_permission_ck" CHECK (
    sensor_permission_granted = false
    OR sensor_permission_recorded_at IS NOT NULL
);

ALTER TABLE ONLY public."users"
ADD CONSTRAINT "users_settings_jsonb_object_ck" CHECK (
    jsonb_typeof(settings_jsonb) = 'object'::text
);

ALTER TABLE ONLY public."account_lock_appeals" ALTER COLUMN appeal_id SET NOT NULL;

ALTER TABLE ONLY public."account_lock_appeals" ALTER COLUMN lock_episode_id SET NOT NULL;

ALTER TABLE ONLY public."account_lock_appeals" ALTER COLUMN reason SET NOT NULL;

ALTER TABLE ONLY public."account_lock_appeals" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."account_lock_appeals" ALTER COLUMN submitted_at SET NOT NULL;

ALTER TABLE ONLY public."account_lock_appeals" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."administrative_areas" ALTER COLUMN administrative_area_id SET NOT NULL;

ALTER TABLE ONLY public."administrative_areas" ALTER COLUMN area_type SET NOT NULL;

ALTER TABLE ONLY public."administrative_areas" ALTER COLUMN code SET NOT NULL;

ALTER TABLE ONLY public."administrative_areas" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."administrative_areas" ALTER COLUMN name SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN assessment_id SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN attempt_count SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN content_hash SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN matches_jsonb SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN model SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN policy_set_hash SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN provider SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN target_id SET NOT NULL;

ALTER TABLE ONLY public."ai_content_assessments" ALTER COLUMN target_type SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN attempt_count SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN content_hash SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN force_rescan SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN job_id SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN next_attempt_at SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN target_id SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN target_type SET NOT NULL;

ALTER TABLE ONLY public."ai_content_scan_jobs" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN active SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN applicable_target_types SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN confidence_threshold SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN detection_guidance SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN name SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN policy_code SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN policy_id SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN report_category SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN severity SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN system_default SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN version SET NOT NULL;

ALTER TABLE ONLY public."ai_moderation_policies" ALTER COLUMN violation_category SET NOT NULL;

ALTER TABLE ONLY public."appointment_notification_configs" ALTER COLUMN config_revision SET NOT NULL;

ALTER TABLE ONLY public."appointment_notification_configs" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."appointment_notification_configs" ALTER COLUMN reminder_id SET NOT NULL;

ALTER TABLE ONLY public."appointment_notification_configs" ALTER COLUMN rules_jsonb SET NOT NULL;

ALTER TABLE ONLY public."appointment_notification_configs" ALTER COLUMN time_zone SET NOT NULL;

ALTER TABLE ONLY public."appointment_notification_configs" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN attachment_category SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN attachment_id SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN file_size_bytes SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN mime_type SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN original_name SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN storage_key SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."attachments" ALTER COLUMN uploader_role SET NOT NULL;

ALTER TABLE ONLY public."audit_events" ALTER COLUMN audit_event_id SET NOT NULL;

ALTER TABLE ONLY public."audit_events" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."audit_events" ALTER COLUMN event_category SET NOT NULL;

ALTER TABLE ONLY public."audit_events" ALTER COLUMN event_origin SET NOT NULL;

ALTER TABLE ONLY public."audit_events" ALTER COLUMN legal_hold SET NOT NULL;

ALTER TABLE ONLY public."audit_events" ALTER COLUMN occurred_at SET NOT NULL;

ALTER TABLE ONLY public."audit_events" ALTER COLUMN severity SET NOT NULL;

ALTER TABLE ONLY public."audit_events" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."auth_challenges" ALTER COLUMN attempts SET NOT NULL;

ALTER TABLE ONLY public."auth_challenges" ALTER COLUMN challenge_hash SET NOT NULL;

ALTER TABLE ONLY public."auth_challenges" ALTER COLUMN challenge_id SET NOT NULL;

ALTER TABLE ONLY public."auth_challenges" ALTER COLUMN challenge_type SET NOT NULL;

ALTER TABLE ONLY public."auth_challenges" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."auth_challenges" ALTER COLUMN expires_at SET NOT NULL;

ALTER TABLE ONLY public."auth_challenges" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."auth_sessions" ALTER COLUMN detected_reuse SET NOT NULL;

ALTER TABLE ONLY public."auth_sessions" ALTER COLUMN device_identifier SET NOT NULL;

ALTER TABLE ONLY public."auth_sessions" ALTER COLUMN expires_at SET NOT NULL;

ALTER TABLE ONLY public."auth_sessions" ALTER COLUMN issued_at SET NOT NULL;

ALTER TABLE ONLY public."auth_sessions" ALTER COLUMN revocation_metadata_jsonb SET NOT NULL;

ALTER TABLE ONLY public."auth_sessions" ALTER COLUMN session_id SET NOT NULL;

ALTER TABLE ONLY public."auth_sessions" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."auth_sessions" ALTER COLUMN token_family_id SET NOT NULL;

ALTER TABLE ONLY public."auth_sessions" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."care_facilities" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."care_facilities" ALTER COLUMN facility_id SET NOT NULL;

ALTER TABLE ONLY public."care_facilities" ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE ONLY public."care_facilities" ALTER COLUMN is_searchable SET NOT NULL;

ALTER TABLE ONLY public."care_facilities" ALTER COLUMN name SET NOT NULL;

ALTER TABLE ONLY public."care_facilities" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."care_facilities" ALTER COLUMN verification_status SET NOT NULL;

ALTER TABLE ONLY public."care_group_members" ALTER COLUMN care_group_id SET NOT NULL;

ALTER TABLE ONLY public."care_group_members" ALTER COLUMN care_group_member_id SET NOT NULL;

ALTER TABLE ONLY public."care_group_members" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."care_group_members" ALTER COLUMN invitation_status SET NOT NULL;

ALTER TABLE ONLY public."care_group_members" ALTER COLUMN is_emergency_contact SET NOT NULL;

ALTER TABLE ONLY public."care_group_members" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."care_group_members" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."care_groups" ALTER COLUMN care_group_id SET NOT NULL;

ALTER TABLE ONLY public."care_groups" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."care_groups" ALTER COLUMN group_name SET NOT NULL;

ALTER TABLE ONLY public."care_groups" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."care_groups" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."care_groups" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN configuration_jsonb SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN content_status SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN display_order SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN distribution_enabled SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN entry_type SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN lock_version SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN migration_review_required SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN template_id SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN template_status SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN template_type SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN title SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."care_item_templates" ALTER COLUMN version SET NOT NULL;

ALTER TABLE ONLY public."care_subjects" ALTER COLUMN care_subject_id SET NOT NULL;

ALTER TABLE ONLY public."care_subjects" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."care_subjects" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."care_subjects" ALTER COLUMN person_id SET NOT NULL;

ALTER TABLE ONLY public."care_subjects" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."care_subjects" ALTER COLUMN subject_type SET NOT NULL;

ALTER TABLE ONLY public."care_subjects" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN metadata_jsonb SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN origin SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN reminder_occurrence_generation SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN target_subject SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN task_id SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN task_type SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN title SET NOT NULL;

ALTER TABLE ONLY public."care_tasks" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN action_type SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN actor_user_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN applied_at SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN checklist_action_command_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN client_request_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN legal_hold SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN payload_hash SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN result_jsonb SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN result_status SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN retain_until SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN task_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_action_commands" ALTER COLUMN task_kind SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN care_context_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN care_context_type SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN checklist_instance_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN context_owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN distribution_key SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN key_version SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN lock_version SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN origin SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN recipient_role SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN recipient_user_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."checklist_instances" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN category SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN checklist_instance_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN checklist_task_instance_id SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN display_order SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN is_required SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN key_version SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN lock_version SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN task_key SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN title_snapshot SET NOT NULL;

ALTER TABLE ONLY public."checklist_task_instances" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN answer_count SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN author_user_id SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN body SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN content_id SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN content_type SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN image_urls SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN is_anonymous SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN is_expert_labeled SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN is_personal_experience SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN like_count SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN moderation_status SET NOT NULL;

ALTER TABLE ONLY public."community_content" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."community_interactions" ALTER COLUMN actor_user_id SET NOT NULL;

ALTER TABLE ONLY public."community_interactions" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."community_interactions" ALTER COLUMN interaction_id SET NOT NULL;

ALTER TABLE ONLY public."community_interactions" ALTER COLUMN interaction_type SET NOT NULL;

ALTER TABLE ONLY public."community_topics" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."community_topics" ALTER COLUMN id SET NOT NULL;

ALTER TABLE ONLY public."community_topics" ALTER COLUMN is_hidden SET NOT NULL;

ALTER TABLE ONLY public."community_topics" ALTER COLUMN name SET NOT NULL;

ALTER TABLE ONLY public."community_topics" ALTER COLUMN slug SET NOT NULL;

ALTER TABLE ONLY public."community_topics" ALTER COLUMN sort_order SET NOT NULL;

ALTER TABLE ONLY public."community_topics" ALTER COLUMN type SET NOT NULL;

ALTER TABLE ONLY public."consultation_bookings" ALTER COLUMN booking_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_bookings" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."consultation_bookings" ALTER COLUMN expert_profile_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_bookings" ALTER COLUMN requester_user_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_bookings" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."consultation_bookings" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_citations" ALTER COLUMN citation_snapshot_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_citations" ALTER COLUMN context_share_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_citations" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_citations" ALTER COLUMN evidence_source_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_citations" ALTER COLUMN ordinal SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_citations" ALTER COLUMN organization SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_citations" ALTER COLUMN reviewed_at SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_citations" ALTER COLUMN source_status_at_share SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_citations" ALTER COLUMN source_url SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN consent_grant_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN consultation_request_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN context_share_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN expert_profile_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN idempotency_key SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN intake_session_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN intake_status SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN origin_dashboard SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN origin_reference_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN risk_level SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN risk_summary SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN share_policy_version SET NOT NULL;

ALTER TABLE ONLY public."consultation_context_shares" ALTER COLUMN triage_stage SET NOT NULL;

ALTER TABLE ONLY public."content_item_sources" ALTER COLUMN content_item_id SET NOT NULL;

ALTER TABLE ONLY public."content_item_sources" ALTER COLUMN content_item_source_id SET NOT NULL;

ALTER TABLE ONLY public."content_item_sources" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."content_item_sources" ALTER COLUMN source_snapshot_jsonb SET NOT NULL;

ALTER TABLE ONLY public."content_item_sources" ALTER COLUMN source_title SET NOT NULL;

ALTER TABLE ONLY public."content_item_topics" ALTER COLUMN content_item_id SET NOT NULL;

ALTER TABLE ONLY public."content_item_topics" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."content_item_topics" ALTER COLUMN topic_id SET NOT NULL;

ALTER TABLE ONLY public."content_items" ALTER COLUMN content_item_id SET NOT NULL;

ALTER TABLE ONLY public."content_items" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."content_items" ALTER COLUMN lock_version SET NOT NULL;

ALTER TABLE ONLY public."content_items" ALTER COLUMN recommendation_priority SET NOT NULL;

ALTER TABLE ONLY public."content_items" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."conversation_calls" ALTER COLUMN call_id SET NOT NULL;

ALTER TABLE ONLY public."conversation_calls" ALTER COLUMN call_status SET NOT NULL;

ALTER TABLE ONLY public."conversation_calls" ALTER COLUMN call_type SET NOT NULL;

ALTER TABLE ONLY public."conversation_calls" ALTER COLUMN conversation_id SET NOT NULL;

ALTER TABLE ONLY public."conversation_calls" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."conversation_calls" ALTER COLUMN initiated_at SET NOT NULL;

ALTER TABLE ONLY public."conversation_calls" ALTER COLUMN initiated_by_user_id SET NOT NULL;

ALTER TABLE ONLY public."conversation_calls" ALTER COLUMN zego_room_id SET NOT NULL;

ALTER TABLE ONLY public."data_permissions" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."data_permissions" ALTER COLUMN legacy_consent_id SET NOT NULL;

ALTER TABLE ONLY public."data_permissions" ALTER COLUMN permission_id SET NOT NULL;

ALTER TABLE ONLY public."data_permissions" ALTER COLUMN permission_kind SET NOT NULL;

ALTER TABLE ONLY public."data_permissions" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."development_milestones" ALTER COLUMN baby_id SET NOT NULL;

ALTER TABLE ONLY public."development_milestones" ALTER COLUMN care_subject_id SET NOT NULL;

ALTER TABLE ONLY public."development_milestones" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."development_milestones" ALTER COLUMN milestone_id SET NOT NULL;

ALTER TABLE ONLY public."development_milestones" ALTER COLUMN milestone_status SET NOT NULL;

ALTER TABLE ONLY public."development_milestones" ALTER COLUMN milestone_type SET NOT NULL;

ALTER TABLE ONLY public."development_milestones" ALTER COLUMN record_status SET NOT NULL;

ALTER TABLE ONLY public."development_milestones" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."device_tokens" ALTER COLUMN active SET NOT NULL;

ALTER TABLE ONLY public."device_tokens" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."device_tokens" ALTER COLUMN id SET NOT NULL;

ALTER TABLE ONLY public."device_tokens" ALTER COLUMN platform SET NOT NULL;

ALTER TABLE ONLY public."device_tokens" ALTER COLUMN token SET NOT NULL;

ALTER TABLE ONLY public."device_tokens" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."device_tokens" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."direct_conversation_read_cursors" ALTER COLUMN conversation_id SET NOT NULL;

ALTER TABLE ONLY public."direct_conversation_read_cursors" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."direct_conversation_read_cursors" ALTER COLUMN last_read_at SET NOT NULL;

ALTER TABLE ONLY public."direct_conversation_read_cursors" ALTER COLUMN last_read_message_id SET NOT NULL;

ALTER TABLE ONLY public."direct_conversation_read_cursors" ALTER COLUMN reader_user_id SET NOT NULL;

ALTER TABLE ONLY public."direct_conversation_read_cursors" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."direct_conversations" ALTER COLUMN conversation_id SET NOT NULL;

ALTER TABLE ONLY public."direct_conversations" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."direct_conversations" ALTER COLUMN expert_user_id SET NOT NULL;

ALTER TABLE ONLY public."direct_conversations" ALTER COLUMN mother_user_id SET NOT NULL;

ALTER TABLE ONLY public."direct_conversations" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."direct_messages" ALTER COLUMN client_message_id SET NOT NULL;

ALTER TABLE ONLY public."direct_messages" ALTER COLUMN conversation_id SET NOT NULL;

ALTER TABLE ONLY public."direct_messages" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."direct_messages" ALTER COLUMN message_id SET NOT NULL;

ALTER TABLE ONLY public."direct_messages" ALTER COLUMN message_type SET NOT NULL;

ALTER TABLE ONLY public."direct_messages" ALTER COLUMN sender_user_id SET NOT NULL;

ALTER TABLE ONLY public."expense_entries" ALTER COLUMN amount SET NOT NULL;

ALTER TABLE ONLY public."expense_entries" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."expense_entries" ALTER COLUMN currency SET NOT NULL;

ALTER TABLE ONLY public."expense_entries" ALTER COLUMN expense_date SET NOT NULL;

ALTER TABLE ONLY public."expense_entries" ALTER COLUMN expense_entry_id SET NOT NULL;

ALTER TABLE ONLY public."expense_entries" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."expense_entries" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."expert_availability" ALTER COLUMN availability_id SET NOT NULL;

ALTER TABLE ONLY public."expert_availability" ALTER COLUMN channel_type SET NOT NULL;

ALTER TABLE ONLY public."expert_availability" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."expert_availability" ALTER COLUMN end_at SET NOT NULL;

ALTER TABLE ONLY public."expert_availability" ALTER COLUMN professional_profile_id SET NOT NULL;

ALTER TABLE ONLY public."expert_availability" ALTER COLUMN start_at SET NOT NULL;

ALTER TABLE ONLY public."expert_availability" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."expert_availability" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."expert_availability" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN client_request_id SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN description SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN expert_profile_id SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN expires_at SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN id SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN requester_user_id SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN topic SET NOT NULL;

ALTER TABLE ONLY public."expert_consultation_requests" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."expert_location_shares" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."expert_location_shares" ALTER COLUMN latitude SET NOT NULL;

ALTER TABLE ONLY public."expert_location_shares" ALTER COLUMN location_share_id SET NOT NULL;

ALTER TABLE ONLY public."expert_location_shares" ALTER COLUMN longitude SET NOT NULL;

ALTER TABLE ONLY public."expert_location_shares" ALTER COLUMN professional_profile_id SET NOT NULL;

ALTER TABLE ONLY public."expert_location_shares" ALTER COLUMN shared_at SET NOT NULL;

ALTER TABLE ONLY public."expert_location_shares" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."expert_location_shares" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."health_context_memories" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."health_context_memories" ALTER COLUMN memory_id SET NOT NULL;

ALTER TABLE ONLY public."health_context_memories" ALTER COLUMN memory_payload_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_context_memories" ALTER COLUMN related_stage SET NOT NULL;

ALTER TABLE ONLY public."health_context_memories" ALTER COLUMN summary_text SET NOT NULL;

ALTER TABLE ONLY public."health_context_memories" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN accepted_input_units_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN aggregation_policy_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN allowed_journey_stages_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN chart_policy_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN device_import_supported SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN display_name SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN effective_from SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN manual_entry_supported SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN metric_code SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN metric_definition_id SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN observation_shape SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN plausibility_policy_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN quality_policy_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN required_context_schema_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN subject_type SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."health_metric_definitions" ALTER COLUMN version SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN care_subject_id SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN context_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN health_observation_id SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN observation_type SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN observed_at SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN raw_payload_jsonb SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN source_type SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN subject_type SET NOT NULL;

ALTER TABLE ONLY public."health_observations" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."health_records" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."health_records" ALTER COLUMN health_record_id SET NOT NULL;

ALTER TABLE ONLY public."health_records" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."health_records" ALTER COLUMN record_type SET NOT NULL;

ALTER TABLE ONLY public."health_records" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."health_records" ALTER COLUMN title SET NOT NULL;

ALTER TABLE ONLY public."health_records" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."knowledge_source_reviews" ALTER COLUMN changed_at SET NOT NULL;

ALTER TABLE ONLY public."knowledge_source_reviews" ALTER COLUMN knowledge_source_id SET NOT NULL;

ALTER TABLE ONLY public."knowledge_source_reviews" ALTER COLUMN new_status SET NOT NULL;

ALTER TABLE ONLY public."knowledge_source_reviews" ALTER COLUMN review_id SET NOT NULL;

ALTER TABLE ONLY public."knowledge_sources" ALTER COLUMN base_url SET NOT NULL;

ALTER TABLE ONLY public."knowledge_sources" ALTER COLUMN category SET NOT NULL;

ALTER TABLE ONLY public."knowledge_sources" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."knowledge_sources" ALTER COLUMN discovery_mode SET NOT NULL;

ALTER TABLE ONLY public."knowledge_sources" ALTER COLUMN domain SET NOT NULL;

ALTER TABLE ONLY public."knowledge_sources" ALTER COLUMN knowledge_source_id SET NOT NULL;

ALTER TABLE ONLY public."knowledge_sources" ALTER COLUMN organization SET NOT NULL;

ALTER TABLE ONLY public."knowledge_sources" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."knowledge_sources" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN exercise_session_id SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN exercise_template_id SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN paused_seconds SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN session_status SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN started_at SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN summary_jsonb SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."maternal_exercise_sessions" ALTER COLUMN warning_count SET NOT NULL;

ALTER TABLE ONLY public."moderation_cases" ALTER COLUMN moderation_case_id SET NOT NULL;

ALTER TABLE ONLY public."moderation_cases" ALTER COLUMN opened_at SET NOT NULL;

ALTER TABLE ONLY public."moderation_cases" ALTER COLUMN priority SET NOT NULL;

ALTER TABLE ONLY public."moderation_cases" ALTER COLUMN report_source SET NOT NULL;

ALTER TABLE ONLY public."moderation_cases" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."moderation_cases" ALTER COLUMN target_id SET NOT NULL;

ALTER TABLE ONLY public."moderation_cases" ALTER COLUMN target_type SET NOT NULL;

ALTER TABLE ONLY public."moderation_cases" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN care_subject_id SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN journey_id SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN journey_type SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN recommendation_profile_jsonb SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN recommendation_profile_status SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN recommendation_profile_version SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."mother_journeys" ALTER COLUMN version SET NOT NULL;

ALTER TABLE ONLY public."notification_jobs" ALTER COLUMN attempt_count SET NOT NULL;

ALTER TABLE ONLY public."notification_jobs" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."notification_jobs" ALTER COLUMN due_at SET NOT NULL;

ALTER TABLE ONLY public."notification_jobs" ALTER COLUMN job_id SET NOT NULL;

ALTER TABLE ONLY public."notification_jobs" ALTER COLUMN job_type SET NOT NULL;

ALTER TABLE ONLY public."notification_jobs" ALTER COLUMN next_attempt_at SET NOT NULL;

ALTER TABLE ONLY public."notification_jobs" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."notification_jobs" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN attempt_count SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN body SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN channel SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN id SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN is_read SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN title SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN type SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."notification_records" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."professional_specialties" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."professional_specialties" ALTER COLUMN is_primary SET NOT NULL;

ALTER TABLE ONLY public."professional_specialties" ALTER COLUMN professional_profile_id SET NOT NULL;

ALTER TABLE ONLY public."professional_specialties" ALTER COLUMN specialty_id SET NOT NULL;

ALTER TABLE ONLY public."red_flag_rules" ALTER COLUMN action SET NOT NULL;

ALTER TABLE ONLY public."red_flag_rules" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."red_flag_rules" ALTER COLUMN id SET NOT NULL;

ALTER TABLE ONLY public."red_flag_rules" ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE ONLY public."red_flag_rules" ALTER COLUMN is_system_default SET NOT NULL;

ALTER TABLE ONLY public."red_flag_rules" ALTER COLUMN keyword SET NOT NULL;

ALTER TABLE ONLY public."red_flag_rules" ALTER COLUMN severity SET NOT NULL;

ALTER TABLE ONLY public."red_flag_rules" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."reminder_occurrence_aliases" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."reminder_occurrence_aliases" ALTER COLUMN occurrence_generation SET NOT NULL;

ALTER TABLE ONLY public."reminder_occurrence_aliases" ALTER COLUMN occurrence_id SET NOT NULL;

ALTER TABLE ONLY public."reminder_occurrence_aliases" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."reminder_occurrence_aliases" ALTER COLUMN reminder_definition_id SET NOT NULL;

ALTER TABLE ONLY public."reminder_occurrence_aliases" ALTER COLUMN scheduled_at SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN active SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN local_times SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN lock_version SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN recurrence SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN revision SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN schedule_id SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN start_date SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN time_zone SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN title SET NOT NULL;

ALTER TABLE ONLY public."reminder_schedules" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN alert_failed_recipient_count SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN alert_generation SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN alert_successful_recipient_count SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN detected_at SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN event_type SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN location_snapshot_jsonb SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN record_type SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN safety_event_id SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."safety_events" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."safety_monitoring_sessions" ALTER COLUMN monitoring_session_id SET NOT NULL;

ALTER TABLE ONLY public."safety_monitoring_sessions" ALTER COLUMN sensitivity_level SET NOT NULL;

ALTER TABLE ONLY public."safety_monitoring_sessions" ALTER COLUMN started_at SET NOT NULL;

ALTER TABLE ONLY public."safety_monitoring_sessions" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."safety_monitoring_sessions" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."specialties" ALTER COLUMN code SET NOT NULL;

ALTER TABLE ONLY public."specialties" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."specialties" ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE ONLY public."specialties" ALTER COLUMN name SET NOT NULL;

ALTER TABLE ONLY public."specialties" ALTER COLUMN specialty_id SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN administrator_email SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN ai_moderation_enabled SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN api_rate_limit SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN connection_timeout_ms SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN email_alerts SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN maintenance_mode_enabled SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN max_upload_size_mb SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN row_version SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN sms_alerts SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN system_configuration_id SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN updated_by SET NOT NULL;

ALTER TABLE ONLY public."system_configurations" ALTER COLUMN webhook_alerts SET NOT NULL;

ALTER TABLE ONLY public."triage_session_evidence" ALTER COLUMN claim_text SET NOT NULL;

ALTER TABLE ONLY public."triage_session_evidence" ALTER COLUMN content_hash SET NOT NULL;

ALTER TABLE ONLY public."triage_session_evidence" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."triage_session_evidence" ALTER COLUMN evidence_id SET NOT NULL;

ALTER TABLE ONLY public."triage_session_evidence" ALTER COLUMN evidence_type SET NOT NULL;

ALTER TABLE ONLY public."triage_session_evidence" ALTER COLUMN source_snapshot_jsonb SET NOT NULL;

ALTER TABLE ONLY public."triage_session_evidence" ALTER COLUMN triage_session_id SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN conversation_jsonb SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN emergency SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN input_jsonb SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN result_jsonb SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN schema_version SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN symptoms SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN triage_session_id SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."triage_sessions" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN email_verified SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN emergency_auto_alert SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN emergency_countdown_seconds SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN enabled SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN failed_login_count SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN fall_detection_enabled SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN fall_detection_sensitivity_level SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN locked SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN must_change_password SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN person_id SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN phone_verified SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN safety_config_updated_at SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN safety_location_sharing_enabled SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN sensor_permission_granted SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN settings_jsonb SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN social_identities SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN trust_status SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE ONLY public."users" ALTER COLUMN verification_status SET NOT NULL;

ALTER TABLE ONLY public."vaccination_records" ALTER COLUMN baby_id SET NOT NULL;

ALTER TABLE ONLY public."vaccination_records" ALTER COLUMN care_subject_id SET NOT NULL;

ALTER TABLE ONLY public."vaccination_records" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."vaccination_records" ALTER COLUMN status SET NOT NULL;

ALTER TABLE ONLY public."vaccination_records" ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE ONLY public."vaccination_records" ALTER COLUMN vaccination_record_id SET NOT NULL;

ALTER TABLE ONLY public."vaccination_records" ALTER COLUMN vaccine_name SET NOT NULL;

ALTER TABLE ONLY public."vaccination_schedules" ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE ONLY public."vaccination_schedules" ALTER COLUMN dose_number SET NOT NULL;

ALTER TABLE ONLY public."vaccination_schedules" ALTER COLUMN offset_days SET NOT NULL;

ALTER TABLE ONLY public."vaccination_schedules" ALTER COLUMN schedule_version SET NOT NULL;

ALTER TABLE ONLY public."vaccination_schedules" ALTER COLUMN vaccination_schedule_id SET NOT NULL;

ALTER TABLE ONLY public."vaccination_schedules" ALTER COLUMN vaccine_name SET NOT NULL;

-- Sequence ownership

-- Indexes

CREATE UNIQUE INDEX account_lock_appeals_pending_episode_uq ON public.account_lock_appeals USING btree (user_id, lock_episode_id)
WHERE (
        (status)::text = 'PENDING'::text
    );

CREATE INDEX account_lock_appeals_queue_ix ON public.account_lock_appeals USING btree (status, submitted_at DESC);

CREATE INDEX account_lock_appeals_user_ix ON public.account_lock_appeals USING btree (user_id, submitted_at DESC);

CREATE INDEX ai_content_assessments_case_ix ON public.ai_content_assessments USING btree (moderation_case_id);

CREATE UNIQUE INDEX ai_content_assessments_completed_uq ON public.ai_content_assessments USING btree (
    target_type,
    target_id,
    content_hash,
    policy_set_hash,
    model
)
WHERE (
        (status)::text = 'COMPLETED'::text
    );

CREATE INDEX ai_content_assessments_target_ix ON public.ai_content_assessments USING btree (
    target_type,
    target_id,
    created_at DESC
);

CREATE UNIQUE INDEX ai_content_scan_jobs_active_uq ON public.ai_content_scan_jobs USING btree (
    target_type,
    target_id,
    content_hash
)
WHERE (
        (status)::text = ANY (
            ARRAY[
                ('QUEUED'::character varying)::text,
                (
                    'PROCESSING'::character varying
                )::text
            ]
        )
    );

CREATE INDEX ai_content_scan_jobs_claim_ix ON public.ai_content_scan_jobs USING btree (status, next_attempt_at);

CREATE INDEX ai_content_scan_jobs_target_ix ON public.ai_content_scan_jobs USING btree (
    target_type,
    target_id,
    status
);

CREATE INDEX ai_moderation_policies_active_ix ON public.ai_moderation_policies USING btree (active, violation_category);

CREATE INDEX attachments_owner_category_review_ix ON public.attachments USING btree (
    owner_user_id,
    attachment_category,
    review_status
);

CREATE INDEX audit_events_category_time_ix ON public.audit_events USING btree (event_category, occurred_at);

CREATE INDEX audit_events_checklist_correlation_ix ON public.audit_events USING btree (
    correlation_id,
    occurred_at DESC
)
WHERE (
        (event_category)::text ~~ 'CHECKLIST_%'::text
    );

CREATE INDEX audit_events_checklist_retention_ix ON public.audit_events USING btree (created_at) WHERE (((event_category)::text ~~ like_escape('CHECKLIST\_%'::text, '\'::text)) AND (legal_hold = false));
CREATE INDEX audit_events_origin_time_ix ON public.audit_events USING btree (event_origin, occurred_at DESC);
CREATE INDEX audit_events_security_note_ix ON public.audit_events USING btree (security_event_id, occurred_at) WHERE ((event_category)::text = 'SECURITY_INVESTIGATION_NOTE'::text);
CREATE INDEX audit_events_subject_time_ix ON public.audit_events USING btree (subject_user_id, occurred_at);
CREATE INDEX auth_challenges_subject_expiry_ix ON public.auth_challenges USING btree (subject_identifier, challenge_type, expires_at);
CREATE INDEX auth_sessions_family_ix ON public.auth_sessions USING btree (token_family_id);
CREATE INDEX auth_sessions_user_device_ix ON public.auth_sessions USING btree (user_id, device_identifier, status);
CREATE INDEX care_facilities_area_ix ON public.care_facilities USING btree (administrative_area_id);
CREATE UNIQUE INDEX care_group_members_checklist_accepted_uk ON public.care_group_members USING btree (care_group_id, user_id) WHERE (((invitation_status)::text = 'ACCEPTED'::text) AND (checklist_access_quarantine_reason_code IS NULL));
CREATE INDEX care_group_members_checklist_auth_ix ON public.care_group_members USING btree (care_group_id, user_id, invitation_status) WHERE ((invitation_status)::text = 'ACCEPTED'::text);
CREATE INDEX care_item_templates_content_status_ix ON public.care_item_templates USING btree (entry_type, content_status, stage, display_order);
CREATE INDEX care_item_templates_exercise_filter_ix ON public.care_item_templates USING btree (template_status, stage, difficulty_level, created_at DESC) WHERE ((entry_type)::text = 'EXERCISE_TEMPLATE'::text);
CREATE INDEX care_item_templates_lineage_version_ix ON public.care_item_templates USING btree (template_lineage_id, version) WHERE ((entry_type)::text = 'TEMPLATE_ROOT'::text);
CREATE UNIQUE INDEX care_item_templates_lineage_version_no_uk ON public.care_item_templates USING btree (template_lineage_id, version) WHERE ((entry_type)::text = 'TEMPLATE_ROOT'::text);
CREATE INDEX care_item_templates_parent_order_ix ON public.care_item_templates USING btree (parent_template_id, display_order);
CREATE INDEX care_item_templates_posture_version_ix ON public.care_item_templates USING btree (parent_template_id, template_status, effective_from DESC) WHERE ((entry_type)::text = 'POSTURE_CONFIG'::text);
CREATE INDEX care_item_templates_preconception_sequence_lookup_ix ON public.care_item_templates USING btree (stage, template_type, recipient_scope, display_order, content_status, distribution_enabled, template_lineage_id, template_version_id) WHERE (((entry_type)::text = 'TEMPLATE_ROOT'::text) AND (display_order > 0));
CREATE UNIQUE INDEX care_item_templates_preconception_sequence_position_uk ON public.care_item_templates USING btree (display_order) WHERE (((entry_type)::text = 'TEMPLATE_ROOT'::text) AND ((stage)::text = 'PRE_PREGNANCY'::text) AND ((template_type)::text = 'MANDATORY'::text) AND ((recipient_scope)::text = 'MOTHER'::text) AND (distribution_enabled = true) AND ((content_status)::text = 'APPROVED'::text) AND (display_order > 0));
CREATE INDEX care_tasks_assignee_status_ix ON public.care_tasks USING btree (assignee_user_id, status, scheduled_at) WHERE (assignee_user_id IS NOT NULL);
CREATE INDEX care_tasks_context_ix ON public.care_tasks USING btree (owner_user_id, journey_id, baby_id, status, scheduled_at) WHERE (owner_user_id IS NOT NULL);
CREATE INDEX care_tasks_owner_status_ix ON public.care_tasks USING btree (owner_user_id, status, scheduled_at) WHERE (owner_user_id IS NOT NULL);
CREATE INDEX care_tasks_today_scope_ix ON public.care_tasks USING btree (assignee_user_id, status, scheduled_at, care_group_id);
CREATE INDEX checklist_action_commands_retention_ix ON public.checklist_action_commands USING btree (created_at, retain_until, task_kind, task_id) WHERE (legal_hold = false);
CREATE INDEX checklist_instances_context_status_ix ON public.checklist_instances USING btree (care_group_id, care_context_type, care_context_id, status);
CREATE INDEX checklist_instances_member_epoch_ix ON public.checklist_instances USING btree (care_group_member_id, checklist_access_epoch, status);
CREATE INDEX checklist_instances_owner_current_ix ON public.checklist_instances USING btree (context_owner_user_id, updated_at DESC) WHERE (historical_at IS NULL);
CREATE INDEX checklist_instances_owner_history_ix ON public.checklist_instances USING btree (context_owner_user_id, historical_at DESC) WHERE (historical_at IS NOT NULL);
CREATE INDEX checklist_instances_period_lookup_ix ON public.checklist_instances USING btree (care_context_type, care_context_id, period_key, recipient_user_id);
CREATE INDEX checklist_instances_recipient_status_ix ON public.checklist_instances USING btree (recipient_user_id, status, created_at DESC);
CREATE INDEX checklist_instances_sequence_scope_ix ON public.checklist_instances USING btree (context_owner_user_id, recipient_user_id, recipient_role, care_context_type, care_context_id, historical_at, template_version_id);
CREATE INDEX checklist_members_timeline_lookup_ix ON public.care_group_members USING btree (care_group_id, user_id, invitation_status, checklist_access_epoch);
CREATE INDEX checklist_task_instances_due_status_ix ON public.checklist_task_instances USING btree (due_at, status);
CREATE INDEX checklist_task_instances_parent_order_ix ON public.checklist_task_instances USING btree (checklist_instance_id, display_order);
CREATE INDEX checklist_templates_schedule_lookup_ix ON public.care_item_templates USING btree (schedule_group_key, schedule_type, schedule_context_type) WHERE ((entry_type)::text = 'TEMPLATE_ROOT'::text);
CREATE INDEX community_content_author_ix ON public.community_content USING btree (author_user_id);
CREATE INDEX community_content_parent_ix ON public.community_content USING btree (parent_content_id);
CREATE INDEX community_content_stage_ix ON public.community_content USING btree (stage);
CREATE INDEX community_content_status_ix ON public.community_content USING btree (moderation_status);
CREATE INDEX community_content_topic_ix ON public.community_content USING btree (topic_id, created_at);
CREATE INDEX community_interactions_actor_ix ON public.community_interactions USING btree (actor_user_id);
CREATE INDEX community_interactions_content_ix ON public.community_interactions USING btree (content_id);
CREATE UNIQUE INDEX community_interactions_content_target_uk ON public.community_interactions USING btree (actor_user_id, interaction_type, content_id) WHERE (content_id IS NOT NULL);
CREATE INDEX community_interactions_topic_ix ON public.community_interactions USING btree (topic_id);
CREATE UNIQUE INDEX community_interactions_topic_target_uk ON public.community_interactions USING btree (actor_user_id, interaction_type, topic_id) WHERE (topic_id IS NOT NULL);
CREATE UNIQUE INDEX consultation_bookings_legacy_session_uk ON public.consultation_bookings USING btree (legacy_session_id) WHERE (legacy_session_id IS NOT NULL);
CREATE INDEX consultation_bookings_requester_ix ON public.consultation_bookings USING btree (requester_user_id, status, created_at DESC);
CREATE INDEX conversation_calls_timeline_ix ON public.conversation_calls USING btree (conversation_id, initiated_at DESC);
CREATE INDEX data_permissions_consent_owner_ix ON public.data_permissions USING btree (owner_user_id, granted_at DESC) WHERE ((permission_kind)::text = 'CONSENT_GRANT'::text);
CREATE UNIQUE INDEX data_permissions_handoff_integrity_uk ON public.data_permissions USING btree (legacy_consent_id, owner_user_id, evidence_key);
CREATE UNIQUE INDEX data_permissions_legacy_consent_id_uk ON public.data_permissions USING btree (legacy_consent_id) WHERE (legacy_consent_id IS NOT NULL);
CREATE INDEX development_milestones_subject_ix ON public.development_milestones USING btree (care_subject_id, milestone_type, achieved_date);
CREATE INDEX direct_messages_timeline_ix ON public.direct_messages USING btree (conversation_id, created_at DESC);
CREATE INDEX expense_entries_owner_date_ix ON public.expense_entries USING btree (owner_user_id, expense_date);
CREATE INDEX expert_availability_profile_window_ix ON public.expert_availability USING btree (professional_profile_id, start_at, end_at);
CREATE INDEX expert_availability_user_window_ix ON public.expert_availability USING btree (user_id, start_at, end_at);
CREATE INDEX expert_consultation_requests_expert_status_ix ON public.expert_consultation_requests USING btree (expert_profile_id, status, created_at DESC);
CREATE INDEX expert_consultation_requests_expiry_ix ON public.expert_consultation_requests USING btree (expires_at) WHERE ((status)::text = 'PENDING'::text);
CREATE UNIQUE INDEX expert_consultation_requests_integrity_uk ON public.expert_consultation_requests USING btree (id, requester_user_id, expert_profile_id, client_request_id);
CREATE INDEX expert_consultation_requests_owner_status_ix ON public.expert_consultation_requests USING btree (requester_user_id, status, created_at DESC);
CREATE INDEX health_context_memories_baby_ix ON public.health_context_memories USING btree (baby_profile_id, created_at DESC) WHERE (deleted_at IS NULL);
CREATE INDEX health_context_memories_mother_ix ON public.health_context_memories USING btree (mother_profile_id, created_at DESC) WHERE (deleted_at IS NULL);
CREATE INDEX health_context_memories_subject_expiry_ix ON public.health_context_memories USING btree (care_subject_id, expires_at) WHERE (deleted_at IS NULL);
CREATE UNIQUE INDEX health_metric_definitions_active_code_uk ON public.health_metric_definitions USING btree (metric_code) WHERE (is_active = true);
CREATE INDEX health_metric_definitions_active_display_ix ON public.health_metric_definitions USING btree (is_active, display_name);
CREATE INDEX health_metric_definitions_effective_period_ix ON public.health_metric_definitions USING btree (effective_from, effective_until);
CREATE INDEX health_observations_live_subject_ix ON public.health_observations USING btree (care_subject_id, observed_at DESC) WHERE (deleted_at IS NULL);
CREATE INDEX health_observations_measurement_group_ix ON public.health_observations USING btree (measurement_group_id) WHERE (measurement_group_id IS NOT NULL);
CREATE INDEX health_observations_p0_subject_metric_time_ix ON public.health_observations USING btree (care_subject_id, observation_type, observed_at) WHERE ((legacy_source)::text = 'maternal_health_observations'::text);
CREATE INDEX health_observations_severity_ix ON public.health_observations USING btree (severity, observed_at) WHERE (severity IS NOT NULL);
CREATE INDEX health_observations_subject_chart_ix ON public.health_observations USING btree (care_subject_id, observation_type, observed_at);
CREATE INDEX health_records_summary_filter_ix ON public.health_records USING btree (owner_user_id, summary_period, record_date DESC) WHERE (((record_type)::text = 'SUMMARY'::text) AND ((status)::text = 'ACTIVE'::text));
CREATE INDEX idx_care_facilities_facility_type ON public.care_facilities USING btree (facility_type);
CREATE INDEX idx_care_facilities_nearby_eligible ON public.care_facilities USING btree (facility_type, province_id, district_id) WHERE ((is_active = true) AND (is_searchable = true) AND (latitude IS NOT NULL) AND (longitude IS NOT NULL));
CREATE INDEX idx_care_group_members_care_group_id ON public.care_group_members USING btree (care_group_id);
CREATE INDEX idx_care_group_members_user_id ON public.care_group_members USING btree (user_id);
CREATE INDEX idx_care_groups_owner_user_id ON public.care_groups USING btree (owner_user_id);
CREATE INDEX idx_community_topics_hidden ON public.community_topics USING btree (is_hidden);
CREATE UNIQUE INDEX idx_community_topics_name_lower ON public.community_topics USING btree (lower((name)::text));
CREATE INDEX idx_community_topics_parent_id ON public.community_topics USING btree (parent_id);
CREATE INDEX idx_community_topics_sort_order ON public.community_topics USING btree (sort_order);
CREATE INDEX idx_community_topics_type ON public.community_topics USING btree (type);
CREATE INDEX idx_content_item_topics_topic_content ON public.content_item_topics USING btree (topic_id, content_item_id);
CREATE INDEX idx_content_items_published_at ON public.content_items USING btree (published_at DESC NULLS LAST) WHERE ((status)::text = 'APPROVED'::text);
CREATE INDEX idx_content_items_recommendation_eligible ON public.content_items USING btree (stage, recommendation_priority DESC, eligible_from_week, eligible_to_week, published_at DESC NULLS LAST, content_item_id) WHERE (((status)::text = 'APPROVED'::text) AND ((content_type)::text = 'ARTICLE'::text));
CREATE INDEX idx_content_items_stage ON public.content_items USING btree (stage);
CREATE INDEX idx_content_items_stage_status ON public.content_items USING btree (stage, status);
CREATE INDEX idx_content_items_stage_type_approved ON public.content_items USING btree (stage, content_type, status) WHERE ((status)::text = 'APPROVED'::text);
CREATE INDEX idx_content_items_status ON public.content_items USING btree (status);
CREATE INDEX idx_content_items_title_search ON public.content_items USING btree (lower((title)::text));
CREATE INDEX idx_content_items_type ON public.content_items USING btree (content_type);
CREATE INDEX idx_content_items_type_status ON public.content_items USING btree (content_type, status);
CREATE INDEX idx_context_citations_share_ordinal ON public.consultation_context_citations USING btree (context_share_id, ordinal, citation_snapshot_id);
CREATE INDEX idx_context_shares_expert_created ON public.consultation_context_shares USING btree (expert_profile_id, created_at DESC);
CREATE INDEX idx_context_shares_owner_created ON public.consultation_context_shares USING btree (owner_user_id, created_at DESC);
CREATE INDEX idx_context_shares_participant_request ON public.consultation_context_shares USING btree (consultation_request_id, owner_user_id, expert_profile_id);
CREATE INDEX idx_development_milestones_baby_id ON public.development_milestones USING btree (baby_id);
CREATE INDEX idx_development_milestones_baby_record_status ON public.development_milestones USING btree (baby_id, record_status);
CREATE INDEX idx_device_tokens_user_id ON public.device_tokens USING btree (user_id);
CREATE INDEX idx_direct_messages_attachment_id ON public.direct_messages USING btree (attachment_id);
CREATE INDEX idx_expert_availability_start_at ON public.expert_availability USING btree (start_at);
CREATE INDEX idx_expert_availability_status ON public.expert_availability USING btree (status);
CREATE INDEX idx_health_records_baby_id ON public.health_records USING btree (baby_id);
CREATE INDEX idx_health_records_journey_id ON public.health_records USING btree (journey_id);
CREATE INDEX idx_health_records_owner_user_id ON public.health_records USING btree (owner_user_id);
CREATE INDEX idx_mother_journeys_owner_user_id ON public.mother_journeys USING btree (owner_user_id);
CREATE INDEX idx_mother_journeys_status ON public.mother_journeys USING btree (status);
CREATE INDEX idx_notification_records_care_group_id ON public.notification_records USING btree (care_group_id);
CREATE INDEX idx_notification_records_type_status ON public.notification_records USING btree (type, status);
CREATE INDEX idx_notification_records_user_id ON public.notification_records USING btree (user_id, created_at DESC);
CREATE INDEX idx_notification_records_user_unread ON public.notification_records USING btree (user_id, is_read) WHERE (is_read = false);
CREATE INDEX idx_red_flag_rules_active_severity ON public.red_flag_rules USING btree (is_active, severity);
CREATE INDEX idx_red_flag_rules_is_system_default ON public.red_flag_rules USING btree (is_system_default);
CREATE INDEX idx_triage_sessions_journey ON public.triage_sessions USING btree (journey_id, completed_at DESC) WHERE (journey_id IS NOT NULL);
CREATE INDEX idx_vaccination_records_baby_id ON public.vaccination_records USING btree (baby_id);
CREATE INDEX idx_vaccination_records_status ON public.vaccination_records USING btree (status);
CREATE INDEX knowledge_source_reviews_source_time_ix ON public.knowledge_source_reviews USING btree (knowledge_source_id, changed_at);
CREATE INDEX knowledge_sources_domain_status_ix ON public.knowledge_sources USING btree (domain, status);
CREATE UNIQUE INDEX knowledge_sources_domain_uk ON public.knowledge_sources USING btree (lower((domain)::text));
CREATE INDEX moderation_cases_priority_ix ON public.moderation_cases USING btree (status, priority, opened_at DESC);
CREATE INDEX moderation_cases_report_source_ix ON public.moderation_cases USING btree (report_source, status, opened_at DESC);
CREATE INDEX moderation_cases_target_ix ON public.moderation_cases USING btree (target_type, target_id, status);
CREATE UNIQUE INDEX mother_journeys_handoff_owner_uk ON public.mother_journeys USING btree (journey_id, owner_user_id);
CREATE UNIQUE INDEX notification_jobs_appointment_identity_uk ON public.notification_jobs USING btree (reminder_id, occurrence_id, config_revision, offset_minutes) WHERE ((job_type)::text = 'APPOINTMENT'::text);
CREATE INDEX notification_jobs_claim_ix ON public.notification_jobs USING btree (job_type, status, next_attempt_at);
CREATE UNIQUE INDEX notification_jobs_schedule_identity_uk ON public.notification_jobs USING btree (schedule_id, schedule_revision, occurrence_date, local_time) WHERE ((job_type)::text = 'REMINDER_SCHEDULE'::text);
CREATE INDEX professional_specialties_specialty_ix ON public.professional_specialties USING btree (specialty_id);
CREATE INDEX reminder_occurrence_alias_definition_ix ON public.reminder_occurrence_aliases USING btree (reminder_definition_id);
CREATE INDEX reminder_occurrence_alias_owner_ix ON public.reminder_occurrence_aliases USING btree (owner_user_id, occurrence_id);
CREATE INDEX reminder_schedules_owner_active_ix ON public.reminder_schedules USING btree (owner_user_id, active, start_date);
CREATE INDEX safety_events_alert_retry_ix ON public.safety_events USING btree (alert_status, alert_updated_at, alert_lease_expires_at) WHERE (((record_type)::text = 'EMERGENCY_SESSION'::text) AND ((status)::text = 'ACTIVE'::text));
CREATE UNIQUE INDEX safety_events_attempt_event_uk ON public.safety_events USING btree (parent_event_id, alert_generation, action_phase) WHERE ((action_type)::text = 'ALERT_ATTEMPT'::text);
CREATE UNIQUE INDEX safety_events_delivery_token_uk ON public.safety_events USING btree (parent_event_id, alert_generation, device_token_id, action_phase) WHERE ((action_type)::text = 'DELIVERY'::text);
CREATE UNIQUE INDEX safety_events_event_owner_uk ON public.safety_events USING btree (safety_event_id, user_id);
CREATE UNIQUE INDEX safety_events_family_alert_uk ON public.safety_events USING btree (parent_event_id, alert_generation) WHERE ((action_type)::text = 'FAMILY_ALERT'::text);
CREATE INDEX safety_events_handoff_status_ix ON public.safety_events USING btree (action_status, created_at DESC) WHERE ((action_type)::text = 'MAP_HANDOFF'::text);
CREATE UNIQUE INDEX safety_events_imu_signal_uk ON public.safety_events USING btree (monitoring_session_id, signal_key) WHERE (((record_type)::text = 'IMU_EVENT'::text) AND (signal_key IS NOT NULL));
CREATE UNIQUE INDEX safety_events_one_active_emergency_user_uk ON public.safety_events USING btree (user_id) WHERE (((record_type)::text = 'EMERGENCY_SESSION'::text) AND ((status)::text = 'ACTIVE'::text));
CREATE INDEX safety_events_owner_location_ix ON public.safety_events USING btree (owner_user_id, captured_at DESC) WHERE ((action_type)::text = 'LOCATION_SNAPSHOT'::text);
CREATE INDEX safety_events_parent_ix ON public.safety_events USING btree (parent_event_id);
CREATE INDEX safety_events_pending_countdown_ix ON public.safety_events USING btree (countdown_deadline_at) WHERE (((record_type)::text = 'IMU_EVENT'::text) AND ((status)::text = 'OPEN'::text) AND (response_type IS NULL));
CREATE INDEX safety_events_user_status_time_ix ON public.safety_events USING btree (user_id, status, detected_at);
CREATE UNIQUE INDEX safety_monitoring_sessions_one_active_user_uk ON public.safety_monitoring_sessions USING btree (user_id) WHERE ((status)::text = 'ACTIVE'::text);
CREATE INDEX safety_monitoring_sessions_user_status_ix ON public.safety_monitoring_sessions USING btree (user_id, status);
CREATE INDEX triage_session_evidence_session_ix ON public.triage_session_evidence USING btree (triage_session_id);
CREATE UNIQUE INDEX triage_sessions_handoff_core_integrity_uk ON public.triage_sessions USING btree (triage_session_id, user_id, origin_dashboard, origin_reference_id, stage, risk_level, status);
CREATE UNIQUE INDEX triage_sessions_handoff_integrity_uk ON public.triage_sessions USING btree (triage_session_id, user_id, journey_id, origin_dashboard, origin_reference_id, stage, risk_level, status);
CREATE UNIQUE INDEX triage_sessions_owner_request_uk ON public.triage_sessions USING btree (user_id, client_request_id) WHERE (client_request_id IS NOT NULL);
CREATE INDEX triage_sessions_risk_ix ON public.triage_sessions USING btree (risk_level, emergency, created_at);
CREATE UNIQUE INDEX triage_sessions_session_owner_uk ON public.triage_sessions USING btree (triage_session_id, user_id);
CREATE INDEX triage_sessions_stage_ix ON public.triage_sessions USING btree (stage, created_at);
CREATE INDEX triage_sessions_user_time_ix ON public.triage_sessions USING btree (user_id, created_at);
CREATE UNIQUE INDEX uq_care_facilities_external_source ON public.care_facilities USING btree (source_type, external_source_id) WHERE (external_source_id IS NOT NULL);
CREATE UNIQUE INDEX uq_care_group_members_invite_token ON public.care_group_members USING btree (invite_token) WHERE (invite_token IS NOT NULL);
CREATE UNIQUE INDEX uq_mother_journeys_id_owner ON public.mother_journeys USING btree (journey_id, owner_user_id);
CREATE UNIQUE INDEX uq_mother_journeys_one_canonical_active ON public.mother_journeys USING btree (owner_user_id) WHERE (((status)::text = 'ACTIVE'::text) AND ((journey_type)::text = ANY (ARRAY[('PRE_PREGNANCY'::character varying)::text, ('PREGNANCY'::character varying)::text, ('POSTPARTUM'::character varying)::text])));
CREATE UNIQUE INDEX uq_notification_records_appointment_milestone ON public.notification_records USING btree (user_id, reference_id, ((metadata ->> 'milestoneJobId'::text))) WHERE (((type)::text = 'REMINDER'::text) AND ((reference_type)::text = 'APPOINTMENT'::text) AND (metadata ? 'milestoneJobId'::text));
CREATE UNIQUE INDEX uq_notification_records_consultation_request ON public.notification_records USING btree (user_id, reference_id, ((metadata ->> 'eventType'::text))) WHERE (((type)::text = 'CONSULTATION'::text) AND ((reference_type)::text = 'CONSULTATION_REQUEST'::text));
CREATE UNIQUE INDEX uq_notification_records_direct_message ON public.notification_records USING btree (user_id, reference_id) WHERE (((type)::text = 'MESSAGE'::text) AND ((reference_type)::text = 'DIRECT_MESSAGE'::text));
CREATE UNIQUE INDEX uq_notification_records_reminder_schedule_job ON public.notification_records USING btree (user_id, reference_id, ((metadata ->> 'scheduleJobId'::text))) WHERE (((type)::text = 'REMINDER'::text) AND ((reference_type)::text = 'REMINDER_SCHEDULE'::text) AND (metadata ? 'scheduleJobId'::text));
CREATE UNIQUE INDEX uq_triage_sessions_continuation_token ON public.triage_sessions USING btree (continuation_token) WHERE (continuation_token IS NOT NULL);
CREATE UNIQUE INDEX users_phone_canonical_uk ON public.users USING btree (phone) WHERE (phone IS NOT NULL);
CREATE INDEX vaccination_records_subject_status_ix ON public.vaccination_records USING btree (care_subject_id, status, scheduled_date);


-- Foreign keys

ALTER TABLE ONLY public."account_lock_appeals" ADD CONSTRAINT "account_lock_appeals_reviewer_fk" FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL;
ALTER TABLE ONLY public."account_lock_appeals" ADD CONSTRAINT "account_lock_appeals_user_fk" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."administrative_areas" ADD CONSTRAINT "administrative_areas_parent_area_id_fkey" FOREIGN KEY (parent_area_id) REFERENCES administrative_areas(administrative_area_id);
ALTER TABLE ONLY public."ai_content_assessments" ADD CONSTRAINT "ai_content_assessments_job_id_fkey" FOREIGN KEY (job_id) REFERENCES ai_content_scan_jobs(job_id);
ALTER TABLE ONLY public."ai_content_assessments" ADD CONSTRAINT "ai_content_assessments_moderation_case_id_fkey" FOREIGN KEY (moderation_case_id) REFERENCES moderation_cases(moderation_case_id);
ALTER TABLE ONLY public."ai_moderation_policies" ADD CONSTRAINT "ai_moderation_policies_created_by_fkey" FOREIGN KEY (created_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."ai_moderation_policies" ADD CONSTRAINT "ai_moderation_policies_updated_by_fkey" FOREIGN KEY (updated_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."appointment_notification_configs" ADD CONSTRAINT "appointment_notification_configs_reminder_fk" FOREIGN KEY (reminder_id) REFERENCES care_tasks(task_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."attachments" ADD CONSTRAINT "attachments_health_record_id_fkey" FOREIGN KEY (health_record_id) REFERENCES health_records(health_record_id);
ALTER TABLE ONLY public."attachments" ADD CONSTRAINT "attachments_owner_user_id_fkey" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."attachments" ADD CONSTRAINT "attachments_reviewed_by_fkey" FOREIGN KEY (reviewed_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."audit_events" ADD CONSTRAINT "audit_events_actor_user_id_fkey" FOREIGN KEY (actor_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."audit_events" ADD CONSTRAINT "audit_events_reviewed_by_fkey" FOREIGN KEY (reviewed_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."audit_events" ADD CONSTRAINT "audit_events_security_event_fk" FOREIGN KEY (security_event_id) REFERENCES audit_events(audit_event_id);
ALTER TABLE ONLY public."audit_events" ADD CONSTRAINT "audit_events_subject_user_id_fkey" FOREIGN KEY (subject_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."auth_challenges" ADD CONSTRAINT "auth_challenges_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."auth_sessions" ADD CONSTRAINT "auth_sessions_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."care_group_members" ADD CONSTRAINT "care_group_members_care_group_id_fkey" FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id);
ALTER TABLE ONLY public."care_group_members" ADD CONSTRAINT "care_group_members_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."care_groups" ADD CONSTRAINT "care_groups_baby_id_fkey" FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."care_groups" ADD CONSTRAINT "care_groups_journey_id_fkey" FOREIGN KEY (journey_id) REFERENCES mother_journeys(journey_id);
ALTER TABLE ONLY public."care_groups" ADD CONSTRAINT "care_groups_linked_baby_owner_fk" FOREIGN KEY (linked_baby_profile_id, owner_user_id, linked_baby_subject_type) REFERENCES care_subjects(care_subject_id, owner_user_id, subject_type) ON DELETE RESTRICT;
ALTER TABLE ONLY public."care_groups" ADD CONSTRAINT "care_groups_linked_journey_owner_fk" FOREIGN KEY (linked_journey_id, owner_user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."care_groups" ADD CONSTRAINT "care_groups_owner_user_id_fkey" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."care_item_templates" ADD CONSTRAINT "care_item_templates_parent_template_id_fkey" FOREIGN KEY (parent_template_id) REFERENCES care_item_templates(template_id);
ALTER TABLE ONLY public."care_subjects" ADD CONSTRAINT "care_subjects_journey_fk" FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id);
ALTER TABLE ONLY public."care_subjects" ADD CONSTRAINT "care_subjects_owner_user_id_fkey" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."care_subjects" ADD CONSTRAINT "care_subjects_person_id_fkey" FOREIGN KEY (person_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."care_tasks" ADD CONSTRAINT "care_tasks_assignee_user_id_fkey" FOREIGN KEY (assignee_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."care_tasks" ADD CONSTRAINT "care_tasks_care_group_id_fkey" FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id);
ALTER TABLE ONLY public."care_tasks" ADD CONSTRAINT "care_tasks_care_group_id_fkey1" FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id);
ALTER TABLE ONLY public."care_tasks" ADD CONSTRAINT "care_tasks_care_subject_id_fkey" FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."care_tasks" ADD CONSTRAINT "care_tasks_creator_user_id_fkey" FOREIGN KEY (creator_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."care_tasks" ADD CONSTRAINT "care_tasks_owner_user_id_fkey" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."care_tasks" ADD CONSTRAINT "care_tasks_vaccination_record_id_fkey" FOREIGN KEY (vaccination_record_id) REFERENCES vaccination_records(vaccination_record_id);
ALTER TABLE ONLY public."checklist_action_commands" ADD CONSTRAINT "checklist_action_commands_actor_fk" FOREIGN KEY (actor_user_id) REFERENCES users(user_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."checklist_action_commands" ADD CONSTRAINT "checklist_action_commands_reminder_definition_fk" FOREIGN KEY (reminder_definition_id) REFERENCES care_tasks(task_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."checklist_instances" ADD CONSTRAINT "checklist_instances_baby_owner_fk" FOREIGN KEY (baby_context_id, context_owner_user_id, baby_context_subject_type) REFERENCES care_subjects(care_subject_id, owner_user_id, subject_type) ON DELETE RESTRICT;
ALTER TABLE ONLY public."checklist_instances" ADD CONSTRAINT "checklist_instances_journey_owner_fk" FOREIGN KEY (journey_context_id, context_owner_user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."checklist_instances" ADD CONSTRAINT "checklist_instances_lineage_version_fk" FOREIGN KEY (template_lineage_id, template_version_id) REFERENCES care_item_templates(template_lineage_id, template_version_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."checklist_instances" ADD CONSTRAINT "checklist_instances_member_fk" FOREIGN KEY (care_group_member_id) REFERENCES care_group_members(care_group_member_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."checklist_instances" ADD CONSTRAINT "checklist_instances_recipient_fk" FOREIGN KEY (recipient_user_id) REFERENCES users(user_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."checklist_task_instances" ADD CONSTRAINT "checklist_task_instances_parent_fk" FOREIGN KEY (checklist_instance_id) REFERENCES checklist_instances(checklist_instance_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."checklist_task_instances" ADD CONSTRAINT "checklist_task_instances_parent_version_fk" FOREIGN KEY (checklist_instance_id, template_version_id) REFERENCES checklist_instances(checklist_instance_id, template_version_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."checklist_task_instances" ADD CONSTRAINT "checklist_task_instances_template_item_fk" FOREIGN KEY (template_item_version_id) REFERENCES care_item_templates(template_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."checklist_task_instances" ADD CONSTRAINT "checklist_task_instances_template_version_fk" FOREIGN KEY (template_version_id) REFERENCES care_item_templates(template_version_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."community_content" ADD CONSTRAINT "community_content_author_user_id_fkey" FOREIGN KEY (author_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."community_content" ADD CONSTRAINT "community_content_parent_content_id_fkey" FOREIGN KEY (parent_content_id) REFERENCES community_content(content_id);
ALTER TABLE ONLY public."community_content" ADD CONSTRAINT "community_content_topic_id_fkey" FOREIGN KEY (topic_id) REFERENCES community_topics(id);
ALTER TABLE ONLY public."community_interactions" ADD CONSTRAINT "community_interactions_actor_user_id_fkey" FOREIGN KEY (actor_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."community_interactions" ADD CONSTRAINT "community_interactions_content_id_fkey" FOREIGN KEY (content_id) REFERENCES community_content(content_id);
ALTER TABLE ONLY public."community_interactions" ADD CONSTRAINT "community_interactions_topic_id_fkey" FOREIGN KEY (topic_id) REFERENCES community_topics(id);
ALTER TABLE ONLY public."community_topics" ADD CONSTRAINT "fk_community_topics_parent" FOREIGN KEY (parent_id) REFERENCES community_topics(id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."consultation_bookings" ADD CONSTRAINT "consultation_bookings_availability_id_fkey" FOREIGN KEY (availability_id) REFERENCES expert_availability(availability_id);
ALTER TABLE ONLY public."consultation_bookings" ADD CONSTRAINT "consultation_bookings_expert_profile_id_fkey" FOREIGN KEY (expert_profile_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."consultation_bookings" ADD CONSTRAINT "consultation_bookings_requester_user_id_fkey" FOREIGN KEY (requester_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."consultation_context_citations" ADD CONSTRAINT "fk_context_citation_share" FOREIGN KEY (context_share_id) REFERENCES consultation_context_shares(context_share_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."consultation_context_citations" ADD CONSTRAINT "fk_context_citation_source" FOREIGN KEY (evidence_source_id) REFERENCES knowledge_sources(knowledge_source_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."consultation_context_shares" ADD CONSTRAINT "fk_context_consent_integrity" FOREIGN KEY (consent_grant_id, owner_user_id, idempotency_key) REFERENCES data_permissions(legacy_consent_id, owner_user_id, evidence_key) ON DELETE RESTRICT;
ALTER TABLE ONLY public."consultation_context_shares" ADD CONSTRAINT "fk_context_expert" FOREIGN KEY (expert_profile_id) REFERENCES users(user_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."consultation_context_shares" ADD CONSTRAINT "fk_context_intake_snapshot" FOREIGN KEY (intake_session_id, owner_user_id, journey_id, origin_dashboard, origin_reference_id, triage_stage, risk_level, intake_status) REFERENCES triage_sessions(triage_session_id, user_id, journey_id, origin_dashboard, origin_reference_id, stage, risk_level, status) ON DELETE RESTRICT;
ALTER TABLE ONLY public."consultation_context_shares" ADD CONSTRAINT "fk_context_intake_snapshot_core" FOREIGN KEY (intake_session_id, owner_user_id, origin_dashboard, origin_reference_id, triage_stage, risk_level, intake_status) REFERENCES triage_sessions(triage_session_id, user_id, origin_dashboard, origin_reference_id, stage, risk_level, status) ON DELETE RESTRICT;
ALTER TABLE ONLY public."consultation_context_shares" ADD CONSTRAINT "fk_context_journey_owner" FOREIGN KEY (journey_id, owner_user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."consultation_context_shares" ADD CONSTRAINT "fk_context_request_integrity" FOREIGN KEY (consultation_request_id, owner_user_id, expert_profile_id, idempotency_key) REFERENCES expert_consultation_requests(id, requester_user_id, expert_profile_id, client_request_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."content_item_sources" ADD CONSTRAINT "content_item_sources_content_item_id_fkey" FOREIGN KEY (content_item_id) REFERENCES content_items(content_item_id);
ALTER TABLE ONLY public."content_item_sources" ADD CONSTRAINT "content_item_sources_knowledge_source_id_fkey" FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id);
ALTER TABLE ONLY public."content_item_topics" ADD CONSTRAINT "content_item_topics_content_item_id_fkey" FOREIGN KEY (content_item_id) REFERENCES content_items(content_item_id);
ALTER TABLE ONLY public."content_item_topics" ADD CONSTRAINT "content_item_topics_topic_id_fkey" FOREIGN KEY (topic_id) REFERENCES community_topics(id);
ALTER TABLE ONLY public."conversation_calls" ADD CONSTRAINT "conversation_calls_conversation_id_fkey" FOREIGN KEY (conversation_id) REFERENCES direct_conversations(conversation_id);
ALTER TABLE ONLY public."conversation_calls" ADD CONSTRAINT "conversation_calls_initiated_by_user_id_fkey" FOREIGN KEY (initiated_by_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."data_permissions" ADD CONSTRAINT "data_permissions_supersedes_fk" FOREIGN KEY (supersedes_permission_id) REFERENCES data_permissions(permission_id);
ALTER TABLE ONLY public."development_milestones" ADD CONSTRAINT "development_milestones_baby_id_fkey" FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."development_milestones" ADD CONSTRAINT "development_milestones_recorded_by_fkey" FOREIGN KEY (recorded_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."development_milestones" ADD CONSTRAINT "development_milestones_subject_fk" FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."device_tokens" ADD CONSTRAINT "device_tokens_user_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."direct_conversation_read_cursors" ADD CONSTRAINT "direct_conversation_read_cursors_conversation_id_fkey" FOREIGN KEY (conversation_id) REFERENCES direct_conversations(conversation_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."direct_conversation_read_cursors" ADD CONSTRAINT "direct_conversation_read_cursors_last_read_message_id_fkey" FOREIGN KEY (last_read_message_id) REFERENCES direct_messages(message_id);
ALTER TABLE ONLY public."direct_conversation_read_cursors" ADD CONSTRAINT "direct_conversation_read_cursors_reader_user_id_fkey" FOREIGN KEY (reader_user_id) REFERENCES users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."direct_conversations" ADD CONSTRAINT "direct_conversations_expert_user_id_fkey" FOREIGN KEY (expert_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."direct_conversations" ADD CONSTRAINT "direct_conversations_mother_user_id_fkey" FOREIGN KEY (mother_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."direct_messages" ADD CONSTRAINT "direct_messages_attachment_id_fkey" FOREIGN KEY (attachment_id) REFERENCES attachments(attachment_id);
ALTER TABLE ONLY public."direct_messages" ADD CONSTRAINT "direct_messages_conversation_id_fkey" FOREIGN KEY (conversation_id) REFERENCES direct_conversations(conversation_id);
ALTER TABLE ONLY public."direct_messages" ADD CONSTRAINT "direct_messages_sender_user_id_fkey" FOREIGN KEY (sender_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."expense_entries" ADD CONSTRAINT "expense_entries_care_subject_id_fkey" FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."expense_entries" ADD CONSTRAINT "expense_entries_mother_journey_id_fkey" FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id);
ALTER TABLE ONLY public."expense_entries" ADD CONSTRAINT "expense_entries_owner_user_id_fkey" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."expert_consultation_requests" ADD CONSTRAINT "expert_consultation_requests_direct_conversation_id_fkey" FOREIGN KEY (direct_conversation_id) REFERENCES direct_conversations(conversation_id);
ALTER TABLE ONLY public."expert_consultation_requests" ADD CONSTRAINT "expert_consultation_requests_expert_profile_id_fkey" FOREIGN KEY (expert_profile_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."expert_consultation_requests" ADD CONSTRAINT "expert_consultation_requests_requester_user_id_fkey" FOREIGN KEY (requester_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."expert_consultation_requests" ADD CONSTRAINT "expert_consultation_requests_responded_by_fkey" FOREIGN KEY (responded_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."health_context_memories" ADD CONSTRAINT "health_context_memories_care_subject_id_fkey" FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."health_context_memories" ADD CONSTRAINT "health_context_memories_triage_session_id_fkey" FOREIGN KEY (triage_session_id) REFERENCES triage_sessions(triage_session_id);
ALTER TABLE ONLY public."health_context_memories" ADD CONSTRAINT "health_context_memories_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."health_observations" ADD CONSTRAINT "health_observations_care_subject_id_fkey" FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."health_observations" ADD CONSTRAINT "health_observations_session_fk" FOREIGN KEY (source_record_id) REFERENCES maternal_exercise_sessions(exercise_session_id);
ALTER TABLE ONLY public."health_records" ADD CONSTRAINT "health_records_baby_id_fkey" FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."health_records" ADD CONSTRAINT "health_records_journey_id_fkey" FOREIGN KEY (journey_id) REFERENCES mother_journeys(journey_id);
ALTER TABLE ONLY public."health_records" ADD CONSTRAINT "health_records_owner_user_id_fkey" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."knowledge_source_reviews" ADD CONSTRAINT "knowledge_source_reviews_actor_user_id_fkey" FOREIGN KEY (actor_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."knowledge_source_reviews" ADD CONSTRAINT "knowledge_source_reviews_knowledge_source_id_fkey" FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id);
ALTER TABLE ONLY public."knowledge_sources" ADD CONSTRAINT "knowledge_sources_added_by_fkey" FOREIGN KEY (added_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."knowledge_sources" ADD CONSTRAINT "knowledge_sources_reviewed_by_fkey" FOREIGN KEY (reviewed_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."maternal_exercise_sessions" ADD CONSTRAINT "maternal_exercise_sessions_mother_journey_id_fkey" FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id);
ALTER TABLE ONLY public."maternal_exercise_sessions" ADD CONSTRAINT "maternal_exercise_sessions_owner_user_id_fkey" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."maternal_exercise_sessions" ADD CONSTRAINT "maternal_exercise_sessions_posture_config_fk" FOREIGN KEY (posture_config_id) REFERENCES care_item_templates(template_id);
ALTER TABLE ONLY public."maternal_exercise_sessions" ADD CONSTRAINT "maternal_exercise_sessions_safety_fk" FOREIGN KEY (safety_observation_id) REFERENCES health_observations(health_observation_id);
ALTER TABLE ONLY public."maternal_exercise_sessions" ADD CONSTRAINT "maternal_exercise_sessions_template_fk" FOREIGN KEY (exercise_template_id) REFERENCES care_item_templates(template_id);
ALTER TABLE ONLY public."moderation_cases" ADD CONSTRAINT "moderation_cases_ai_feedback_assessment_id_fkey" FOREIGN KEY (ai_feedback_assessment_id) REFERENCES ai_content_assessments(assessment_id);
ALTER TABLE ONLY public."moderation_cases" ADD CONSTRAINT "moderation_cases_ai_feedback_by_fkey" FOREIGN KEY (ai_feedback_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."moderation_cases" ADD CONSTRAINT "moderation_cases_assigned_moderator_id_fkey" FOREIGN KEY (assigned_moderator_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."moderation_cases" ADD CONSTRAINT "moderation_cases_reporter_user_id_fkey" FOREIGN KEY (reporter_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."mother_journeys" ADD CONSTRAINT "mother_journeys_owner_user_id_fkey" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."mother_journeys" ADD CONSTRAINT "mother_journeys_subject_fk" FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."notification_jobs" ADD CONSTRAINT "notification_jobs_record_fk" FOREIGN KEY (notification_record_id) REFERENCES notification_records(id) ON DELETE SET NULL;
ALTER TABLE ONLY public."notification_jobs" ADD CONSTRAINT "notification_jobs_reminder_fk" FOREIGN KEY (reminder_id) REFERENCES care_tasks(task_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."notification_jobs" ADD CONSTRAINT "notification_jobs_schedule_fk" FOREIGN KEY (schedule_id) REFERENCES reminder_schedules(schedule_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."notification_records" ADD CONSTRAINT "fk_notification_records_care_group" FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id);
ALTER TABLE ONLY public."notification_records" ADD CONSTRAINT "fk_notification_records_user" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."professional_specialties" ADD CONSTRAINT "professional_specialties_professional_profile_id_fkey" FOREIGN KEY (professional_profile_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."professional_specialties" ADD CONSTRAINT "professional_specialties_specialty_id_fkey" FOREIGN KEY (specialty_id) REFERENCES specialties(specialty_id);
ALTER TABLE ONLY public."red_flag_rules" ADD CONSTRAINT "fk_red_flag_rules_created_by" FOREIGN KEY (created_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."red_flag_rules" ADD CONSTRAINT "fk_red_flag_rules_updated_by" FOREIGN KEY (updated_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."reminder_schedules" ADD CONSTRAINT "reminder_schedules_owner_fk" FOREIGN KEY (owner_user_id) REFERENCES users(user_id) ON DELETE CASCADE;
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_care_facility_id_fkey" FOREIGN KEY (care_facility_id) REFERENCES care_facilities(facility_id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_care_subject_id_fkey" FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_created_by_user_fk" FOREIGN KEY (created_by_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_device_token_fk" FOREIGN KEY (device_token_id) REFERENCES device_tokens(id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_emergency_session_fk" FOREIGN KEY (emergency_session_id) REFERENCES safety_events(safety_event_id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_monitoring_session_id_fkey" FOREIGN KEY (monitoring_session_id) REFERENCES safety_monitoring_sessions(monitoring_session_id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_notification_record_id_fkey" FOREIGN KEY (notification_record_id) REFERENCES notification_records(id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_owner_fk" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_owner_user_id_fkey" FOREIGN KEY (owner_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_parent_event_id_fkey" FOREIGN KEY (parent_event_id) REFERENCES safety_events(safety_event_id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_recipient_user_id_fkey" FOREIGN KEY (recipient_user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."safety_events" ADD CONSTRAINT "safety_events_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."safety_monitoring_sessions" ADD CONSTRAINT "safety_monitoring_sessions_created_by_fkey" FOREIGN KEY (created_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."safety_monitoring_sessions" ADD CONSTRAINT "safety_monitoring_sessions_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."system_configurations" ADD CONSTRAINT "system_configurations_updated_by_fkey" FOREIGN KEY (updated_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."triage_session_evidence" ADD CONSTRAINT "triage_session_evidence_source_fk" FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id);
ALTER TABLE ONLY public."triage_session_evidence" ADD CONSTRAINT "triage_session_evidence_triage_session_id_fkey" FOREIGN KEY (triage_session_id) REFERENCES triage_sessions(triage_session_id);
ALTER TABLE ONLY public."triage_sessions" ADD CONSTRAINT "fk_triage_journey_owner" FOREIGN KEY (journey_id, user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."triage_sessions" ADD CONSTRAINT "triage_sessions_care_subject_id_fkey" FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."triage_sessions" ADD CONSTRAINT "triage_sessions_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id);
ALTER TABLE ONLY public."users" ADD CONSTRAINT "users_deactivated_by_fk" FOREIGN KEY (deactivated_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."users" ADD CONSTRAINT "users_deactivated_by_fkey" FOREIGN KEY (deactivated_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."users" ADD CONSTRAINT "users_facility_id_fkey" FOREIGN KEY (facility_id) REFERENCES care_facilities(facility_id);
ALTER TABLE ONLY public."users" ADD CONSTRAINT "users_locked_by_fk" FOREIGN KEY (locked_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."users" ADD CONSTRAINT "users_safety_updated_by_fk" FOREIGN KEY (safety_config_updated_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."users" ADD CONSTRAINT "users_verified_by_fkey" FOREIGN KEY (verified_by) REFERENCES users(user_id);
ALTER TABLE ONLY public."vaccination_records" ADD CONSTRAINT "vaccination_records_baby_id_fkey" FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id);
ALTER TABLE ONLY public."vaccination_records" ADD CONSTRAINT "vaccination_records_proof_record_id_fkey" FOREIGN KEY (proof_record_id) REFERENCES health_records(health_record_id);
ALTER TABLE ONLY public."vaccination_records" ADD CONSTRAINT "vaccination_records_schedule_fk" FOREIGN KEY (vaccination_schedule_id) REFERENCES vaccination_schedules(vaccination_schedule_id);
ALTER TABLE ONLY public."vaccination_records" ADD CONSTRAINT "vaccination_records_subject_fk" FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id);


-- Views

CREATE VIEW public."care_logs" AS
 SELECT task_id AS care_log_id,
    care_subject_id,
    COALESCE(metadata_jsonb ->> 'logType'::text, replace(title::text, 'Care log: '::text, ''::text)) AS log_type,
    scheduled_at AS started_at,
    COALESCE((metadata_jsonb ->> 'endedAt'::text)::timestamp with time zone, completed_at) AS ended_at,
    (metadata_jsonb ->> 'quantity'::text)::numeric AS quantity,
    metadata_jsonb ->> 'unit'::text AS unit,
    description AS note,
    COALESCE((metadata_jsonb ->> 'recordedBy'::text)::uuid, creator_user_id) AS recorded_by,
    status,
    metadata_jsonb AS payload_jsonb,
    created_at,
    updated_at
   FROM care_tasks
  WHERE task_type::text = 'CARE_LOG'::text;;

CREATE VIEW public."emergency_contacts" AS
 SELECT DISTINCT ON (cg.owner_user_id) cgm.care_group_member_id AS id,
    cg.owner_user_id AS user_id,
    member_user.full_name AS name,
    member_user.phone,
    cgm.member_role AS relationship,
    cgm.is_emergency_contact AS primary_contact,
    cgm.updated_at,
    cgm.user_id AS updated_by
   FROM care_group_members cgm
     JOIN care_groups cg ON cg.care_group_id = cgm.care_group_id
     JOIN users member_user ON member_user.user_id = cgm.user_id
  WHERE cgm.is_emergency_contact
  ORDER BY cg.owner_user_id, cgm.emergency_contact_priority, cgm.updated_at DESC;;

CREATE VIEW public."expert_credentials" AS
 SELECT attachment_id AS credential_id,
    owner_user_id AS user_id,
    credential_type,
    credential_number,
    issuer,
    issued_date,
    expiry_date,
    file_url,
    file_id,
    review_status,
    review_note,
    reviewed_by,
    reviewed_at,
    created_at,
    updated_at
   FROM attachments
  WHERE attachment_category::text = 'EXPERT_CREDENTIAL'::text;;


-- Triggers

CREATE TRIGGER audit_events_immutable_trg BEFORE DELETE OR UPDATE ON audit_events FOR EACH ROW EXECUTE FUNCTION carebridge_reject_mutation();
CREATE CONSTRAINT TRIGGER checklist_access_audit_timeline_ck_trg AFTER INSERT ON audit_events DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION checklist_assert_access_timeline_audit();
CREATE TRIGGER checklist_v1_writer_barrier_audit_trg BEFORE INSERT OR DELETE OR UPDATE ON audit_events FOR EACH ROW EXECUTE FUNCTION checklist_v1_writer_barrier();
CREATE CONSTRAINT TRIGGER checklist_access_timeline_audit_ck_trg AFTER INSERT OR UPDATE ON care_group_members DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION checklist_assert_access_timeline_audit();
CREATE TRIGGER checklist_v1_writer_barrier_members_trg BEFORE INSERT OR DELETE OR UPDATE ON care_group_members FOR EACH ROW EXECUTE FUNCTION checklist_v1_writer_barrier();
CREATE TRIGGER checklist_v1_writer_barrier_groups_trg BEFORE INSERT OR DELETE OR UPDATE ON care_groups FOR EACH ROW EXECUTE FUNCTION checklist_v1_writer_barrier();
CREATE TRIGGER checklist_guard_approved_item_mutation_trg BEFORE INSERT OR DELETE OR UPDATE ON care_item_templates FOR EACH ROW EXECUTE FUNCTION checklist_guard_approved_item_mutation();
CREATE TRIGGER checklist_guard_approved_template_mutation_trg BEFORE DELETE OR UPDATE ON care_item_templates FOR EACH ROW EXECUTE FUNCTION checklist_guard_approved_template_mutation();
CREATE TRIGGER checklist_guard_sequence_position_mutation_trg BEFORE UPDATE OF display_order ON care_item_templates FOR EACH ROW EXECUTE FUNCTION checklist_guard_sequence_position_mutation();
CREATE TRIGGER checklist_guard_template_type_mutation_trg BEFORE UPDATE OF template_type ON care_item_templates FOR EACH ROW EXECUTE FUNCTION checklist_guard_template_type_mutation();
CREATE TRIGGER checklist_v1_writer_barrier_templates_trg BEFORE INSERT OR DELETE OR UPDATE ON care_item_templates FOR EACH ROW EXECUTE FUNCTION checklist_v1_writer_barrier();
CREATE CONSTRAINT TRIGGER checklist_validate_inline_template_shape_trg AFTER INSERT OR UPDATE OF entry_type, stage, content_status, distribution_enabled, migration_review_required, recipient_scope, eligibility_anchor_type, eligibility_range_unit, eligibility_start_inclusive, eligibility_end_inclusive ON care_item_templates DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION checklist_validate_inline_template_shape();
CREATE TRIGGER checklist_validate_postpartum_leaf_timing_trg BEFORE INSERT OR UPDATE OF entry_type, stage, content_status, distribution_enabled, eligibility_anchor_type, is_active, target_subject, due_anchor_type, parent_template_id ON care_item_templates FOR EACH ROW EXECUTE FUNCTION checklist_validate_postpartum_leaf_timing();
CREATE TRIGGER checklist_validate_template_approval_trg BEFORE INSERT OR UPDATE OF content_status, distribution_enabled, migration_review_required, stage, recipient_scope, eligibility_anchor_type, eligibility_range_unit, eligibility_start_inclusive, eligibility_end_inclusive, template_version_id ON care_item_templates FOR EACH ROW EXECUTE FUNCTION checklist_validate_template_approval();
CREATE TRIGGER checklist_validate_v2_requiredness_trg BEFORE INSERT OR UPDATE OF content_status, distribution_enabled, template_id ON care_item_templates FOR EACH ROW EXECUTE FUNCTION checklist_validate_v2_requiredness();
CREATE TRIGGER care_logs_view_write_trg INSTEAD OF INSERT OR DELETE OR UPDATE ON care_logs FOR EACH ROW EXECUTE FUNCTION carebridge_care_logs_view_write();
CREATE TRIGGER care_tasks_reminder_occurrence_alias_trg AFTER INSERT OR UPDATE OF scheduled_at, owner_user_id, reminder_occurrence_generation ON care_tasks FOR EACH ROW EXECUTE FUNCTION capture_reminder_occurrence_alias();
CREATE TRIGGER checklist_action_command_retention_guard_trg BEFORE DELETE ON checklist_action_commands FOR EACH ROW EXECUTE FUNCTION checklist_action_command_retention_guard();
CREATE TRIGGER checklist_validate_action_command_target_trg BEFORE INSERT ON checklist_action_commands FOR EACH ROW EXECUTE FUNCTION checklist_validate_action_command_target();
CREATE TRIGGER checklist_v1_writer_barrier_instances_trg BEFORE INSERT OR DELETE OR UPDATE ON checklist_instances FOR EACH ROW EXECUTE FUNCTION checklist_v1_writer_barrier();
CREATE TRIGGER checklist_validate_instance_contract_match_trg BEFORE UPDATE OF checklist_contract_version, checklist_quarantine_reason_code ON checklist_instances FOR EACH ROW EXECUTE FUNCTION checklist_validate_instance_contract_match();
CREATE TRIGGER checklist_validate_instance_recipient_trg BEFORE INSERT OR UPDATE OF recipient_role, recipient_user_id, care_group_id, care_context_type, care_context_id, context_owner_user_id, care_group_member_id, checklist_access_epoch ON checklist_instances FOR EACH ROW EXECUTE FUNCTION checklist_validate_instance_recipient();
CREATE TRIGGER checklist_guard_preconception_requiredness_trg BEFORE INSERT OR UPDATE OF is_required, template_item_version_id, template_version_id, checklist_instance_id, checklist_quarantine_reason_code ON checklist_task_instances FOR EACH ROW EXECUTE FUNCTION checklist_guard_preconception_requiredness();
CREATE TRIGGER checklist_v1_writer_barrier_tasks_trg BEFORE INSERT OR DELETE OR UPDATE ON checklist_task_instances FOR EACH ROW EXECUTE FUNCTION checklist_v1_writer_barrier();
CREATE TRIGGER checklist_validate_task_contract_match_trg BEFORE INSERT OR UPDATE OF checklist_instance_id, checklist_contract_version, checklist_quarantine_reason_code ON checklist_task_instances FOR EACH ROW EXECUTE FUNCTION checklist_validate_task_contract_match();
CREATE TRIGGER checklist_validate_task_template_trg BEFORE INSERT OR UPDATE OF checklist_instance_id, template_version_id, template_item_version_id ON checklist_task_instances FOR EACH ROW EXECUTE FUNCTION checklist_validate_task_template();
CREATE TRIGGER trg_community_topic_parent_category BEFORE INSERT OR UPDATE OF type, parent_id ON community_topics FOR EACH ROW EXECUTE FUNCTION enforce_community_topic_parent_category();
CREATE TRIGGER trg_consultation_context_citations_append_only BEFORE DELETE OR UPDATE ON consultation_context_citations FOR EACH ROW EXECUTE FUNCTION reject_consultation_context_mutation();
CREATE TRIGGER trg_consultation_context_shares_append_only BEFORE DELETE OR UPDATE ON consultation_context_shares FOR EACH ROW EXECUTE FUNCTION reject_consultation_context_mutation();
CREATE TRIGGER emergency_contacts_view_write_trg INSTEAD OF INSERT OR DELETE OR UPDATE ON emergency_contacts FOR EACH ROW EXECUTE FUNCTION carebridge_emergency_contacts_view_write();
CREATE TRIGGER expert_consultation_request_conversation_source_trg BEFORE INSERT OR UPDATE OF direct_conversation_id ON expert_consultation_requests FOR EACH ROW EXECUTE FUNCTION carebridge_validate_expert_request_conversation();
CREATE TRIGGER expert_credentials_view_write_trg INSTEAD OF INSERT OR DELETE OR UPDATE ON expert_credentials FOR EACH ROW EXECUTE FUNCTION carebridge_expert_credentials_view_write();
CREATE TRIGGER knowledge_source_reviews_immutable_trg BEFORE DELETE OR UPDATE ON knowledge_source_reviews FOR EACH ROW EXECUTE FUNCTION carebridge_reject_mutation();
CREATE TRIGGER checklist_v1_writer_barrier_journeys_trg BEFORE INSERT OR DELETE OR UPDATE ON mother_journeys FOR EACH ROW EXECUTE FUNCTION checklist_v1_writer_barrier();
CREATE TRIGGER triage_session_evidence_immutable_trg BEFORE DELETE OR UPDATE ON triage_session_evidence FOR EACH ROW EXECUTE FUNCTION carebridge_reject_mutation();
CREATE TRIGGER triage_completed_snapshot_guard_trg BEFORE INSERT OR DELETE OR UPDATE ON triage_sessions FOR EACH ROW EXECUTE FUNCTION carebridge_guard_completed_triage_snapshot();


-- Row security and policies



-- Comments

COMMENT ON COLUMN public.care_item_templates.template_type IS 'MANDATORY templates are auto-distributed;

OPTIONAL templates are added explicitly by users.';
COMMENT ON COLUMN public.health_observations.deleted_at IS 'Soft-delete marker mirroring growth_measurements.deleted_at. Read paths must filter on this before wave 13 backfills any soft-deleted growth row (V3 §3.12).';
COMMENT ON COLUMN public.health_observations.measurement_group_id IS 'Groups the observations produced by a single measuring session. For rows migrated from growth_measurements this is the source growth_measurement_id (V3 §3.12).';
COMMENT ON COLUMN public.users.safety_location_sharing_enabled IS 'Explicit mother opt-in to attach current location to fall emergency alerts.';
COMMENT ON TABLE public.notification_jobs IS 'R11: typed-polymorphic notification queue. job_type discriminates;

each branch keeps its own typed FK and its own partial unique identity (V3 §3.8).';


-- Ownership



-- Explicit access control

GRANT USAGE ON SCHEMA public TO PUBLIC;
GRANT USAGE ON SCHEMA public TO carebridge_checklist_retention_owner;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO checklist_operations;
GRANT USAGE ON SCHEMA public TO carebridge_application;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public
    TO carebridge_application;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public
    TO carebridge_application;
REVOKE ALL ON TABLE public.flyway_schema_history FROM carebridge_application;
REVOKE UPDATE, DELETE ON TABLE public.audit_events FROM carebridge_application;
REVOKE DELETE ON TABLE public.checklist_action_commands FROM carebridge_application;
REVOKE INSERT, UPDATE, DELETE ON TABLE public.reminder_occurrence_aliases
    FROM carebridge_application;
DO $carebridge_optional_quarantine_acl$
BEGIN
    IF to_regclass('public.checklist_migration_quarantine') IS NOT NULL THEN
        EXECUTE 'REVOKE INSERT, DELETE ON TABLE public.checklist_migration_quarantine '
             || 'FROM carebridge_application';
        EXECUTE 'GRANT SELECT, UPDATE ON TABLE public.checklist_migration_quarantine '
             || 'TO carebridge_application';
    END IF;
END
$carebridge_optional_quarantine_acl$;
GRANT DELETE, INSERT, SELECT ON TABLE public.audit_events TO carebridge_checklist_retention_owner;
GRANT DELETE, SELECT ON TABLE public.checklist_action_commands TO carebridge_checklist_retention_owner;
GRANT EXECUTE ON FUNCTION public.checklist_p2_access_audit(p_member_id uuid, p_event_type text, p_reason_code text, p_before jsonb, p_after jsonb, p_correlation uuid, p_recorded_at timestamp with time zone) TO carebridge_checklist_schema_owner;
GRANT EXECUTE ON FUNCTION public.checklist_p2_access_timeline_valid(p_member_id uuid, p_timeline jsonb) TO carebridge_application;
GRANT EXECUTE ON FUNCTION public.checklist_p2_access_timeline_valid(p_member_id uuid, p_timeline jsonb) TO carebridge_checklist_schema_owner;
GRANT EXECUTE ON FUNCTION public.checklist_p2_deterministic_uuid(p_key text) TO carebridge_checklist_schema_owner;
GRANT EXECUTE ON FUNCTION public.checklist_p2_quarantine_audit(p_resource_type text, p_resource_id uuid, p_reason_code text, p_source_kind text, p_recorded_at timestamp with time zone) TO carebridge_checklist_schema_owner;
GRANT INSERT, SELECT ON TABLE public.audit_events TO carebridge_application;
GRANT INSERT, SELECT, UPDATE ON TABLE public.checklist_action_commands TO carebridge_application;
GRANT SELECT ON TABLE public.care_tasks TO carebridge_checklist_retention_owner;
GRANT SELECT ON TABLE public.checklist_task_instances TO carebridge_checklist_retention_owner;
GRANT SELECT ON TABLE public.reminder_occurrence_aliases TO carebridge_application;
GRANT SELECT ON TABLE public.users TO carebridge_checklist_retention_owner;
REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.checklist_purge_retained_records(uuid) TO checklist_operations;
ALTER FUNCTION public.checklist_purge_retained_records(uuid)
    OWNER TO carebridge_checklist_retention_owner;
REVOKE CREATE ON SCHEMA public FROM carebridge_checklist_retention_owner;
REVOKE ALL ON FUNCTION public.capture_reminder_occurrence_alias() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.carebridge_reject_mutation() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_action_command_retention_guard() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_p2_access_audit(p_member_id uuid, p_event_type text, p_reason_code text, p_before jsonb, p_after jsonb, p_correlation uuid, p_recorded_at timestamp with time zone) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_p2_access_timeline_valid(p_member_id uuid, p_timeline jsonb) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_p2_deterministic_uuid(p_key text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_p2_quarantine_audit(p_resource_type text, p_resource_id uuid, p_reason_code text, p_source_kind text, p_recorded_at timestamp with time zone) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.reminder_occurrence_id_v1(p_reminder_definition_id uuid, p_scheduled_at timestamp with time zone) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.reminder_occurrence_id_v2(p_reminder_definition_id uuid, p_scheduled_at timestamp with time zone, p_occurrence_generation bigint) FROM PUBLIC;

-- Ownership transfer requires a temporary deployment-time membership created by the
-- privileged Flyway runner. Remove it in the same migration and attest the final owner/ACL.
DO $carebridge_retention_final_state$
DECLARE
    purge_owner text;
BEGIN
    IF pg_has_role(current_user, 'carebridge_checklist_retention_owner', 'MEMBER') THEN
        EXECUTE format(
            'REVOKE carebridge_checklist_retention_owner FROM %I',
            current_user);
    END IF;

    SELECT owner_role.rolname
      INTO purge_owner
      FROM pg_catalog.pg_proc routine
      JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
     WHERE routine.oid = 'public.checklist_purge_retained_records(uuid)'::regprocedure;
    IF purge_owner <> 'carebridge_checklist_retention_owner' THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PURGE_OWNER_INVALID:%', purge_owner;
    END IF;
    IF EXISTS (
        SELECT 1
          FROM pg_catalog.pg_proc routine
          CROSS JOIN LATERAL pg_catalog.aclexplode(
              COALESCE(routine.proacl, pg_catalog.acldefault('f', routine.proowner))) acl
         WHERE routine.oid = 'public.checklist_purge_retained_records(uuid)'::regprocedure
           AND acl.grantee = 0
           AND acl.privilege_type = 'EXECUTE') THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PURGE_PUBLIC_EXECUTE_INVALID';
    END IF;
    IF NOT has_function_privilege(
           'checklist_operations',
           'public.checklist_purge_retained_records(uuid)',
           'EXECUTE') THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PURGE_OPERATIONS_EXECUTE_MISSING';
    END IF;
    IF EXISTS (
        SELECT 1
          FROM pg_catalog.pg_auth_members membership
         WHERE membership.roleid = to_regrole('carebridge_checklist_retention_owner')
           AND (membership.inherit_option OR membership.set_option)) THEN
        RAISE EXCEPTION 'PURGE_OWNER_ROLE_REACHABLE';
    END IF;
END
$carebridge_retention_final_state$;


RESET check_function_bodies;
RESET client_min_messages;