-- Invalid explicit care-group links must never become distribution authorities.
-- Persist only an opaque random marker and controlled identifiers for operations.
ALTER TABLE public.checklist_migration_quarantine
    ADD COLUMN care_context_type varchar(10),
    ADD COLUMN care_context_id uuid;

CREATE UNIQUE INDEX checklist_quarantine_open_group_context_uk
    ON public.checklist_migration_quarantine
       (source_table, source_id, reason_code, care_context_type, care_context_id)
    WHERE resolved_at IS NULL
      AND source_table = 'care_groups';

CREATE OR REPLACE FUNCTION public.checklist_quarantine_invalid_group_context(
    group_id uuid,
    context_type varchar,
    context_id uuid,
    reason varchar)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    -- gen_random_bytes() is extension-owned and is not visible under the
    -- constrained SECURITY DEFINER search_path.  gen_random_uuid() is a
    -- core function already used by the canonical schema; hash it to keep
    -- the stored marker opaque without depending on extension resolution.
    marker bytea := sha256(convert_to(gen_random_uuid()::text, 'UTF8'));
    quarantine_correlation uuid := gen_random_uuid();
BEGIN
    WITH inserted AS (
        INSERT INTO public.checklist_migration_quarantine
            (source_table, source_id, reason_code, payload_ciphertext, payload_hash,
             encryption_key_version, correlation_id, retain_until,
             care_context_type, care_context_id)
        VALUES (
            'care_groups', group_id, reason, marker,
            encode(sha256(marker), 'hex'), 'REDACTED_NO_PAYLOAD_V1',
            quarantine_correlation, now() + interval '7 years',
            context_type, context_id)
        ON CONFLICT
            (source_table, source_id, reason_code, care_context_type, care_context_id)
            WHERE resolved_at IS NULL AND source_table = 'care_groups'
        DO NOTHING
        RETURNING source_id, reason_code, correlation_id, care_context_type, care_context_id
    )
    INSERT INTO public.audit_events
        (actor_user_id, event_category, subject_reference_id,
         resource_type, resource_id, purpose, decision,
         occurred_at, created_at, event_origin, payload,
         correlation_id, severity, status,
         actor_type, actor_service, reason_code,
         care_context_type, care_context_id)
    SELECT
        NULL,
        'CHECKLIST_MIGRATION_QUARANTINED',
        inserted.source_id,
        'CARE_GROUP_CONTEXT',
        inserted.source_id,
        'CHECKLIST_CONTEXT_AUTHORITY_VALIDATION',
        'QUARANTINED',
        now(),
        now(),
        'AUDIT_LOG',
        jsonb_build_object(
            'sourceTable', 'care_groups',
            'sourceId', inserted.source_id,
            'reasonCode', inserted.reason_code,
            'contextType', inserted.care_context_type,
            'contextId', inserted.care_context_id,
            'metadata', 'REDACTED'),
        inserted.correlation_id,
        'HIGH',
        'OPEN',
        'SERVICE',
        'CHECKLIST_CONTEXT_AUTHORITY',
        inserted.reason_code,
        inserted.care_context_type,
        inserted.care_context_id
    FROM inserted;
END $$;

REVOKE ALL ON FUNCTION public.checklist_quarantine_invalid_group_context(
    uuid, varchar, uuid, varchar) FROM PUBLIC;

CREATE OR REPLACE FUNCTION public.checklist_sync_reviewed_care_group_contexts()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    mismatch_reason varchar(80);
BEGIN
    IF NEW.linked_journey_id IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM public.checklist_context_authorities authority
            WHERE authority.care_context_type = 'JOURNEY'
              AND authority.care_context_id = NEW.linked_journey_id
              AND authority.owner_user_id = NEW.owner_user_id) THEN
            INSERT INTO public.checklist_care_group_contexts
                (care_group_id, owner_user_id, care_context_type, care_context_id,
                 review_status, distribution_blocked, reviewed_at, reviewed_by)
            VALUES (NEW.care_group_id, NEW.owner_user_id, 'JOURNEY', NEW.linked_journey_id,
                    'REVIEWED', false, now(), NEW.owner_user_id)
            ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;
        ELSE
            mismatch_reason := CASE WHEN EXISTS (
                SELECT 1 FROM public.checklist_context_authorities authority
                WHERE authority.care_context_type = 'JOURNEY'
                  AND authority.care_context_id = NEW.linked_journey_id)
                THEN 'CONTEXT_OWNER_MISMATCH' ELSE 'CONTEXT_NOT_FOUND' END;
            PERFORM public.checklist_quarantine_invalid_group_context(
                NEW.care_group_id, 'JOURNEY', NEW.linked_journey_id, mismatch_reason);
        END IF;
    END IF;

    IF NEW.linked_baby_profile_id IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM public.checklist_context_authorities authority
            WHERE authority.care_context_type = 'BABY'
              AND authority.care_context_id = NEW.linked_baby_profile_id
              AND authority.owner_user_id = NEW.owner_user_id) THEN
            INSERT INTO public.checklist_care_group_contexts
                (care_group_id, owner_user_id, care_context_type, care_context_id,
                 review_status, distribution_blocked, reviewed_at, reviewed_by)
            VALUES (NEW.care_group_id, NEW.owner_user_id, 'BABY', NEW.linked_baby_profile_id,
                    'REVIEWED', false, now(), NEW.owner_user_id)
            ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;
        ELSE
            mismatch_reason := CASE WHEN EXISTS (
                SELECT 1 FROM public.checklist_context_authorities authority
                WHERE authority.care_context_type = 'BABY'
                  AND authority.care_context_id = NEW.linked_baby_profile_id)
                THEN 'CONTEXT_OWNER_MISMATCH' ELSE 'CONTEXT_NOT_FOUND' END;
            PERFORM public.checklist_quarantine_invalid_group_context(
                NEW.care_group_id, 'BABY', NEW.linked_baby_profile_id, mismatch_reason);
        END IF;
    END IF;
    RETURN NEW;
END $$;

REVOKE ALL ON FUNCTION public.checklist_sync_reviewed_care_group_contexts()
    FROM PUBLIC;
