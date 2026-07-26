-- Consolidates the five approved relational tables (53 -> 48 base tables).
-- Vaccination tables remain unchanged: their proposed consolidation is still
-- marked for consideration rather than approved for this migration.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

-- Align a drifted Supabase instance to the approved 53-table baseline before
-- applying the five approved consolidations below. The source snapshot is
-- carebridge_53 (1).sql; the target after this migration is 48 base tables.
DO $baseline_fk_cleanup$
DECLARE constraint_row record;
BEGIN
    FOR constraint_row IN
        SELECT con.conname, rel.relname AS table_name
          FROM pg_constraint con
          JOIN pg_class rel ON rel.oid = con.conrelid
          JOIN pg_namespace ns ON ns.oid = rel.relnamespace
         WHERE con.contype = 'f'
           AND ns.nspname = 'public'
           AND con.confrelid = 'public.persons'::regclass
           AND rel.relname IN ('users', 'care_subjects')
    LOOP
        EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT IF EXISTS %I',
                       constraint_row.table_name, constraint_row.conname);
    END LOOP;
END
$baseline_fk_cleanup$;

DROP TABLE IF EXISTS public.consultation_context_citations;
DROP TABLE IF EXISTS public.consultation_context_shares;
DROP TABLE IF EXISTS public.archived_consultation_records;
DROP TABLE IF EXISTS public.archived_partner_records;
DROP TABLE IF EXISTS public.archived_realtime_records;
DROP TABLE IF EXISTS public.auth_revocations;
DROP TABLE IF EXISTS public.baby_journey_link_cleanup_summary;
DROP TABLE IF EXISTS public.baby_profiles;
DROP TABLE IF EXISTS public.community_profiles;
DROP TABLE IF EXISTS public.consent_grants;
DROP TABLE IF EXISTS public.consultation_requests;
DROP TABLE IF EXISTS public.evidence_sources;
DROP TABLE IF EXISTS public.expert_consultation_requests;
DROP TABLE IF EXISTS public.expert_contribution_events;
DROP TABLE IF EXISTS public.expert_profiles;
DROP TABLE IF EXISTS public.family_tasks;
DROP TABLE IF EXISTS public.health_record_attachments;
DROP TABLE IF EXISTS public.intake_sessions;
DROP TABLE IF EXISTS public.maternal_observations;
DROP TABLE IF EXISTS public.moderation_events;
DROP TABLE IF EXISTS public.mother_journey_events;
DROP TABLE IF EXISTS public.nearby_support_requests;
DROP TABLE IF EXISTS public.nearby_support_responses;
DROP TABLE IF EXISTS public.pregnancy_outcome_evidence;
DROP TABLE IF EXISTS public.professional_profiles;
DROP TABLE IF EXISTS public.safety_event_actions;
DROP TABLE IF EXISTS public.scheduled_care_items;
DROP TABLE IF EXISTS public.security_events;
DROP TABLE IF EXISTS public.user_identities;
DROP TABLE IF EXISTS public.persons;

DO $baseline_absence_gate$
DECLARE table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'archived_consultation_records', 'archived_partner_records', 'archived_realtime_records',
        'auth_revocations', 'baby_journey_link_cleanup_summary', 'baby_profiles',
        'community_profiles', 'consent_grants', 'consultation_context_citations',
        'consultation_context_shares', 'consultation_requests', 'evidence_sources',
        'expert_consultation_requests', 'expert_contribution_events', 'expert_profiles',
        'family_tasks', 'health_record_attachments', 'intake_sessions', 'maternal_observations',
        'moderation_events', 'mother_journey_events', 'nearby_support_requests',
        'nearby_support_responses', 'persons', 'pregnancy_outcome_evidence',
        'professional_profiles', 'safety_event_actions', 'scheduled_care_items',
        'security_events', 'user_identities'
    ] LOOP
        IF to_regclass('public.' || table_name) IS NOT NULL THEN
            RAISE EXCEPTION 'BASELINE_ALIGNMENT_DROP_FAILED: %', table_name;
        END IF;
    END LOOP;
END
$baseline_absence_gate$;

-- 1. Account deletion is a soft-deactivation concern owned by users.
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS deactivation_reason text,
    ADD COLUMN IF NOT EXISTS deactivated_at timestamptz,
    ADD COLUMN IF NOT EXISTS deactivated_by uuid REFERENCES public.users(user_id);

DO $account_deletion_consolidation$
BEGIN
    IF to_regclass('public.account_deletion_requests') IS NOT NULL THEN
        WITH latest_request AS (
            SELECT DISTINCT ON (user_id)
                   user_id, reason, processed_at, processed_by, status
              FROM public.account_deletion_requests
             ORDER BY user_id, coalesce(processed_at, requested_at, updated_at) DESC, id DESC
        )
        UPDATE public.users u
           SET deactivation_reason = coalesce(l.reason, u.deactivation_reason),
               deactivated_at = coalesce(l.processed_at, u.deactivated_at),
               deactivated_by = coalesce(l.processed_by, u.deactivated_by),
               account_status = CASE
                   WHEN upper(l.status) IN ('APPROVED', 'PROCESSED', 'COMPLETED', 'DELETED', 'INACTIVE')
                       THEN 'INACTIVE'
                   ELSE u.account_status
               END,
               enabled = CASE
                   WHEN upper(l.status) IN ('APPROVED', 'PROCESSED', 'COMPLETED', 'DELETED', 'INACTIVE')
                       THEN false
                   ELSE u.enabled
               END
          FROM latest_request l
         WHERE u.user_id = l.user_id;

        DROP TABLE public.account_deletion_requests;
    END IF;
END
$account_deletion_consolidation$;

-- 2. Emergency contacts are accepted care-group members. Phone and name are
-- read from the member's users row; only the emergency designation is stored here.
ALTER TABLE public.care_group_members
    ADD COLUMN IF NOT EXISTS is_emergency_contact boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS emergency_contact_priority smallint;

DO $emergency_contact_consolidation$
BEGIN
    IF to_regclass('public.emergency_contacts') IS NULL THEN
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.emergency_contacts ec
         WHERE NOT EXISTS (
             SELECT 1
               FROM public.care_groups cg
               JOIN public.care_group_members cgm ON cgm.care_group_id = cg.care_group_id
               JOIN public.users member_user ON member_user.user_id = cgm.user_id
              WHERE cg.owner_user_id = ec.user_id
                AND regexp_replace(coalesce(member_user.phone, member_user.phone_number, ''), '\D', '', 'g') =
                    regexp_replace(coalesce(ec.phone, ''), '\D', '', 'g')
         )
    ) THEN
        RAISE EXCEPTION
            'EMERGENCY_CONTACT_MEMBER_NOT_FOUND: add each emergency contact as a care-group member before this migration';
    END IF;

    UPDATE public.care_group_members cgm
       SET is_emergency_contact = true,
           emergency_contact_priority = 1,
           member_role = coalesce(nullif(ec.relationship, ''), cgm.member_role),
           updated_at = greatest(cgm.updated_at, ec.updated_at)
      FROM public.emergency_contacts ec,
           public.care_groups cg,
           public.users member_user
     WHERE cg.owner_user_id = ec.user_id
       AND member_user.user_id = cgm.user_id
       AND cgm.care_group_id = cg.care_group_id
       AND regexp_replace(coalesce(member_user.phone, member_user.phone_number, ''), '\D', '', 'g') =
           regexp_replace(coalesce(ec.phone, ''), '\D', '', 'g');

    DROP TABLE public.emergency_contacts;
END
$emergency_contact_consolidation$;

CREATE OR REPLACE VIEW public.emergency_contacts AS
SELECT DISTINCT ON (cg.owner_user_id)
       cgm.care_group_member_id AS id,
       cg.owner_user_id AS user_id,
       coalesce(member_user.display_name, member_user.full_name) AS name,
       coalesce(member_user.phone, member_user.phone_number) AS phone,
       cgm.member_role AS relationship,
       cgm.is_emergency_contact AS primary_contact,
       cgm.updated_at,
       cgm.user_id AS updated_by
  FROM public.care_group_members cgm
  JOIN public.care_groups cg ON cg.care_group_id = cgm.care_group_id
  JOIN public.users member_user ON member_user.user_id = cgm.user_id
 WHERE cgm.is_emergency_contact
 ORDER BY cg.owner_user_id, cgm.emergency_contact_priority NULLS LAST, cgm.updated_at DESC;

CREATE OR REPLACE FUNCTION public.carebridge_emergency_contacts_view_write()
RETURNS trigger LANGUAGE plpgsql AS $$
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
       AND regexp_replace(coalesce(member_user.phone, member_user.phone_number, ''), '\D', '', 'g') =
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
$$;

DROP TRIGGER IF EXISTS emergency_contacts_view_write_trg ON public.emergency_contacts;
CREATE TRIGGER emergency_contacts_view_write_trg
INSTEAD OF INSERT OR UPDATE OR DELETE ON public.emergency_contacts
FOR EACH ROW EXECUTE FUNCTION public.carebridge_emergency_contacts_view_write();

-- 3. Expert credentials become typed attachments. The view is retained only
-- as an application compatibility facade; its backing table is attachments.
ALTER TABLE public.attachments
    ADD COLUMN IF NOT EXISTS attachment_category varchar(40) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN IF NOT EXISTS credential_type varchar(50),
    ADD COLUMN IF NOT EXISTS credential_number varchar(100),
    ADD COLUMN IF NOT EXISTS issuer varchar(200),
    ADD COLUMN IF NOT EXISTS issued_date date,
    ADD COLUMN IF NOT EXISTS expiry_date date,
    ADD COLUMN IF NOT EXISTS review_status varchar(30),
    ADD COLUMN IF NOT EXISTS review_note text,
    ADD COLUMN IF NOT EXISTS reviewed_by uuid REFERENCES public.users(user_id),
    ADD COLUMN IF NOT EXISTS reviewed_at timestamp,
    ADD COLUMN IF NOT EXISTS file_url text;

CREATE INDEX IF NOT EXISTS attachments_owner_category_review_ix
    ON public.attachments(owner_user_id, attachment_category, review_status);

DO $expert_credential_consolidation$
BEGIN
    IF to_regclass('public.expert_credentials') IS NULL THEN
        RETURN;
    END IF;

    IF EXISTS (SELECT 1 FROM public.expert_credentials WHERE user_id IS NULL) THEN
        RAISE EXCEPTION 'EXPERT_CREDENTIAL_OWNER_MISSING: credential rows must have a canonical user_id before consolidation';
    END IF;

    INSERT INTO public.attachments (
        attachment_id, owner_user_id, storage_key, original_name, mime_type,
        file_size_bytes, status, attachment_category, credential_type,
        credential_number, issuer, issued_date, expiry_date, review_status,
        review_note, reviewed_by, reviewed_at, file_url, created_at, updated_at
    )
    SELECT ec.credential_id,
           ec.user_id,
           coalesce(nullif(ec.file_url, ''), 'expert-credential/' || ec.credential_id::text),
           coalesce(nullif(ec.credential_type, ''), 'expert-credential') || '.document',
           'application/octet-stream',
           0,
           'ACTIVE',
           'EXPERT_CREDENTIAL',
           ec.credential_type,
           ec.credential_number,
           ec.issuer,
           ec.issued_date,
           ec.expiry_date,
           ec.review_status,
           ec.review_note,
           ec.reviewed_by,
           ec.reviewed_at,
           ec.file_url,
           ec.created_at,
           ec.updated_at
      FROM public.expert_credentials ec
    ON CONFLICT (attachment_id) DO UPDATE
        SET attachment_category = 'EXPERT_CREDENTIAL',
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
            updated_at = excluded.updated_at;

    DROP TABLE public.expert_credentials;
END
$expert_credential_consolidation$;

CREATE OR REPLACE VIEW public.expert_credentials AS
SELECT attachment_id AS credential_id,
       owner_user_id AS user_id,
       credential_type,
       credential_number,
       issuer,
       issued_date,
       expiry_date,
       file_url,
       review_status,
       review_note,
       reviewed_by,
       reviewed_at,
       created_at,
       updated_at
  FROM public.attachments
 WHERE attachment_category = 'EXPERT_CREDENTIAL';

CREATE OR REPLACE FUNCTION public.carebridge_expert_credentials_view_write()
RETURNS trigger LANGUAGE plpgsql AS $$
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
        review_note, reviewed_by, reviewed_at, file_url, created_at, updated_at
    ) VALUES (
        row_id, NEW.user_id,
        coalesce(nullif(NEW.file_url, ''), 'expert-credential/' || row_id::text),
        coalesce(nullif(NEW.credential_type, ''), 'expert-credential') || '.document',
        'application/octet-stream', 0, 'ACTIVE', 'EXPERT_CREDENTIAL',
        NEW.credential_type, NEW.credential_number, NEW.issuer, NEW.issued_date,
        NEW.expiry_date, coalesce(NEW.review_status, 'PENDING'), NEW.review_note,
        NEW.reviewed_by, NEW.reviewed_at, NEW.file_url,
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
            updated_at = excluded.updated_at;
    NEW.credential_id := row_id;
    RETURN NEW;
END
$$;

DROP TRIGGER IF EXISTS expert_credentials_view_write_trg ON public.expert_credentials;
CREATE TRIGGER expert_credentials_view_write_trg
INSTEAD OF INSERT OR UPDATE OR DELETE ON public.expert_credentials
FOR EACH ROW EXECUTE FUNCTION public.carebridge_expert_credentials_view_write();

-- 4. Daily care logs become typed care tasks.
ALTER TABLE public.care_tasks
    ADD COLUMN IF NOT EXISTS metadata_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE public.care_tasks DROP CONSTRAINT IF EXISTS care_tasks_type_ck;
ALTER TABLE public.care_tasks
    ADD CONSTRAINT care_tasks_type_ck CHECK (
        task_type IN ('SCHEDULED_REMINDER', 'MANUAL_TASK', 'CARE_LOG')
    );

DO $care_log_consolidation$
BEGIN
    IF to_regclass('public.care_logs') IS NOT NULL THEN
        INSERT INTO public.care_tasks (
            task_id, task_type, owner_user_id, creator_user_id, care_subject_id,
            title, description, scheduled_at, completed_at, status,
            source_reference_type, source_reference_id, metadata_jsonb, created_at, updated_at
        )
        SELECT cl.care_log_id, 'CARE_LOG', cs.owner_user_id, cl.recorded_by, cl.care_subject_id,
               'Care log: ' || cl.log_type, cl.note, cl.started_at, cl.ended_at, cl.status,
               'CARE_LOG', cl.care_log_id,
               coalesce(cl.payload_jsonb, '{}'::jsonb) || jsonb_build_object(
                   'logType', cl.log_type,
                   'endedAt', cl.ended_at,
                   'quantity', cl.quantity,
                   'unit', cl.unit,
                   'recordedBy', cl.recorded_by
               ),
               cl.created_at, cl.updated_at
          FROM public.care_logs cl
          JOIN public.care_subjects cs ON cs.care_subject_id = cl.care_subject_id
        ON CONFLICT (task_id) DO NOTHING;

        DROP TABLE public.care_logs;
    END IF;
END
$care_log_consolidation$;

CREATE OR REPLACE VIEW public.care_logs AS
SELECT task_id AS care_log_id,
       care_subject_id,
       coalesce(metadata_jsonb ->> 'logType', replace(title, 'Care log: ', '')) AS log_type,
       scheduled_at AS started_at,
       coalesce((metadata_jsonb ->> 'endedAt')::timestamptz, completed_at) AS ended_at,
       (metadata_jsonb ->> 'quantity')::numeric AS quantity,
       metadata_jsonb ->> 'unit' AS unit,
       description AS note,
       coalesce((metadata_jsonb ->> 'recordedBy')::uuid, creator_user_id) AS recorded_by,
       status,
       metadata_jsonb AS payload_jsonb,
       created_at,
       updated_at
  FROM public.care_tasks
 WHERE task_type = 'CARE_LOG';

CREATE OR REPLACE FUNCTION public.carebridge_care_logs_view_write()
RETURNS trigger LANGUAGE plpgsql AS $$
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
$$;

DROP TRIGGER IF EXISTS care_logs_view_write_trg ON public.care_logs;
CREATE TRIGGER care_logs_view_write_trg
INSTEAD OF INSERT OR UPDATE OR DELETE ON public.care_logs
FOR EACH ROW EXECUTE FUNCTION public.carebridge_care_logs_view_write();

-- 5. Nearby peer-support persistence is disabled. Care-facility and expert
-- discovery remain separate features; no peer-location interaction is retained.
DO $nearby_support_removal$
BEGIN
    IF to_regclass('public.nearby_support_interactions') IS NOT NULL THEN
        DROP TABLE public.nearby_support_interactions;
    END IF;
END
$nearby_support_removal$;

CREATE OR REPLACE VIEW public.nearby_support_interactions AS
SELECT NULL::uuid AS interaction_id,
       NULL::uuid AS parent_interaction_id,
       NULL::uuid AS user_id,
       NULL::varchar(30) AS interaction_type,
       NULL::varchar(30) AS status,
       NULL::text AS message,
       NULL::numeric AS latitude,
       NULL::numeric AS longitude,
       NULL::timestamptz AS created_at,
       NULL::timestamptz AS updated_at
 WHERE false;

CREATE OR REPLACE FUNCTION public.carebridge_reject_nearby_support_interaction()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'NEARBY_PEER_SUPPORT_DISABLED';
END
$$;

DROP TRIGGER IF EXISTS nearby_support_interactions_disabled_trg ON public.nearby_support_interactions;
CREATE TRIGGER nearby_support_interactions_disabled_trg
INSTEAD OF INSERT OR UPDATE OR DELETE ON public.nearby_support_interactions
FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_nearby_support_interaction();

DO $final_table_count_gate$
DECLARE actual_count integer;
BEGIN
    SELECT count(*) INTO actual_count
      FROM information_schema.tables
     WHERE table_schema = 'public'
       AND table_type = 'BASE TABLE';

    IF actual_count <> 48 THEN
        RAISE EXCEPTION 'FINAL_TABLE_COUNT_MISMATCH: expected 48 base tables, found %', actual_count;
    END IF;
END
$final_table_count_gate$;
