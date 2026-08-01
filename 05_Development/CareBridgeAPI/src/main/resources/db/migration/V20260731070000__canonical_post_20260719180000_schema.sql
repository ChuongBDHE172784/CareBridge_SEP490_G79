-- Canonical forward migration rewritten from the 30 post-20260719180000 migrations.
-- Final definitions are authored once; superseded deferred sync/validation triggers are omitted.
-- The migration remains one atomic Flyway transaction.

-- BEGIN CANONICAL SOURCE: V20260727020000__add_content_review_feedback.sql
ALTER TABLE public.content_items
    ADD COLUMN IF NOT EXISTS revision_reason text,
    ADD COLUMN IF NOT EXISTS revision_requested_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS revision_requested_by uuid,
    ADD COLUMN IF NOT EXISTS revision_requested_version integer,
    ADD COLUMN IF NOT EXISTS lock_version bigint NOT NULL DEFAULT 0;

ALTER TABLE public.care_item_templates
    ADD COLUMN IF NOT EXISTS author_user_id uuid,
    ADD COLUMN IF NOT EXISTS revision_reason text,
    ADD COLUMN IF NOT EXISTS revision_requested_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS revision_requested_by uuid,
    ADD COLUMN IF NOT EXISTS revision_requested_version integer,
    ADD COLUMN IF NOT EXISTS lock_version bigint NOT NULL DEFAULT 0;

ALTER TABLE public.notification_records
    DROP CONSTRAINT IF EXISTS notification_records_type_check;

ALTER TABLE public.notification_records
    ADD CONSTRAINT notification_records_type_check CHECK (type IN (
        'REMINDER',
        'COMMUNITY_REPLY',
        'CONSULTATION',
        'EMERGENCY',
        'MESSAGE',
        'GROUP_INVITE',
        'CONTENT_REVIEW'
    ));
-- END CANONICAL SOURCE: V20260727020000__add_content_review_feedback.sql

-- BEGIN CANONICAL SOURCE: V20260727030000__add_content_summary.sql
ALTER TABLE public.content_items
    ADD COLUMN IF NOT EXISTS summary varchar(150);
-- END CANONICAL SOURCE: V20260727030000__add_content_summary.sql

-- BEGIN CANONICAL SOURCE: V20260728010000__remove_baby_journey_linkage.sql
-- Remove the retired baby-to-mother-journey relation while preserving maternal
-- lifecycle rows, immutable audit history, and typed baby safety continuations.

LOCK TABLE public.care_subjects IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    detached_before bigint;
BEGIN
    SELECT count(*) INTO detached_before
      FROM public.care_subjects
     WHERE subject_type = 'BABY' AND mother_journey_id IS NOT NULL;
    RAISE NOTICE 'baby journey relations before detach: %', detached_before;
END $$;

UPDATE public.care_subjects
   SET mother_journey_id = NULL,
       updated_at = now()
 WHERE subject_type = 'BABY'
   AND mother_journey_id IS NOT NULL;

ALTER TABLE public.care_subjects
    ADD CONSTRAINT care_subjects_baby_no_mother_journey_ck
    CHECK (subject_type <> 'BABY' OR mother_journey_id IS NULL) NOT VALID;
ALTER TABLE public.care_subjects
    VALIDATE CONSTRAINT care_subjects_baby_no_mother_journey_ck;

DO $$
DECLARE
    detached_after bigint;
BEGIN
    SELECT count(*) INTO detached_after
      FROM public.care_subjects
     WHERE subject_type = 'BABY' AND mother_journey_id IS NOT NULL;
    RAISE NOTICE 'baby journey relations after detach: %', detached_after;
END $$;

LOCK TABLE public.triage_sessions IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE public.consultation_context_shares IN SHARE ROW EXCLUSIVE MODE;

ALTER TABLE public.consultation_context_shares
    DROP CONSTRAINT IF EXISTS fk_context_intake_snapshot;

ALTER TABLE public.triage_sessions
    DROP CONSTRAINT IF EXISTS chk_triage_lifecycle_binding,
    DROP CONSTRAINT IF EXISTS chk_triage_origin_stage;

ALTER TABLE public.consultation_context_shares
    ALTER COLUMN journey_id DROP NOT NULL;

DO $$
DECLARE
    triage_before bigint;
    shares_before bigint;
BEGIN
    SELECT count(*) INTO triage_before
      FROM public.triage_sessions
     WHERE origin_dashboard = 'BABY_PROFILE' AND journey_id IS NOT NULL;
    SELECT count(*) INTO shares_before
      FROM public.consultation_context_shares
     WHERE origin_dashboard = 'BABY_PROFILE' AND journey_id IS NOT NULL;
    RAISE NOTICE 'baby safety journey ids before detach: triage=%, shares=%',
        triage_before, shares_before;
END $$;

ALTER TABLE public.triage_sessions
    DISABLE TRIGGER triage_completed_snapshot_guard_trg;
UPDATE public.triage_sessions
   SET journey_id = NULL,
       updated_at = now()
 WHERE origin_dashboard = 'BABY_PROFILE'
   AND journey_id IS NOT NULL;
ALTER TABLE public.triage_sessions
    ENABLE TRIGGER triage_completed_snapshot_guard_trg;

ALTER TABLE public.consultation_context_shares
    DISABLE TRIGGER trg_consultation_context_shares_append_only;
UPDATE public.consultation_context_shares
   SET journey_id = NULL
 WHERE origin_dashboard = 'BABY_PROFILE'
   AND journey_id IS NOT NULL;
ALTER TABLE public.consultation_context_shares
    ENABLE TRIGGER trg_consultation_context_shares_append_only;

ALTER TABLE public.triage_sessions
    ADD CONSTRAINT chk_triage_lifecycle_binding CHECK (
        (journey_id IS NULL
            AND origin_dashboard IS NULL
            AND origin_reference_id IS NULL
            AND continuation_token IS NULL
            AND continuation_expires_at IS NULL
            AND continuation_acknowledged_at IS NULL)
        OR (origin_dashboard = 'MOTHER_JOURNEY'
            AND journey_id IS NOT NULL
            AND origin_reference_id IS NOT NULL
            AND continuation_token IS NOT NULL
            AND continuation_expires_at IS NOT NULL)
        OR (origin_dashboard = 'BABY_PROFILE'
            AND journey_id IS NULL
            AND origin_reference_id IS NOT NULL
            AND continuation_token IS NOT NULL
            AND continuation_expires_at IS NOT NULL)
    ) NOT VALID,
    ADD CONSTRAINT chk_triage_origin_stage CHECK (
        origin_dashboard IS NULL
        OR (origin_dashboard = 'MOTHER_JOURNEY'
            AND origin_reference_id = journey_id
            AND stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'))
        OR (origin_dashboard = 'BABY_PROFILE'
            AND journey_id IS NULL
            AND baby_profile_id IS NOT NULL
            AND origin_reference_id = baby_profile_id
            AND stage IN ('INFANT', 'TODDLER'))
    ) NOT VALID;
ALTER TABLE public.triage_sessions
    VALIDATE CONSTRAINT chk_triage_lifecycle_binding;
ALTER TABLE public.triage_sessions
    VALIDATE CONSTRAINT chk_triage_origin_stage;

-- Keep the complete immutable snapshot relationship for maternal shares and
-- add a non-null core relationship for baby shares (whose journey_id is null).
-- The old owner-only FK prevented orphaned intakes but allowed origin/stage/
-- risk/status snapshots to drift from the completed triage session.
CREATE UNIQUE INDEX IF NOT EXISTS triage_sessions_handoff_core_integrity_uk
    ON public.triage_sessions USING btree
       (triage_session_id, user_id, origin_dashboard, origin_reference_id,
        stage, risk_level, status);

ALTER TABLE public.consultation_context_shares
    ADD CONSTRAINT fk_context_intake_snapshot_core
        FOREIGN KEY (intake_session_id, owner_user_id, origin_dashboard,
                     origin_reference_id, triage_stage, risk_level, intake_status)
        REFERENCES public.triage_sessions (triage_session_id, user_id,
                                            origin_dashboard, origin_reference_id,
                                            stage, risk_level, status)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_context_intake_snapshot
        FOREIGN KEY (intake_session_id, owner_user_id, journey_id,
                     origin_dashboard, origin_reference_id, triage_stage,
                     risk_level, intake_status)
        REFERENCES public.triage_sessions (triage_session_id, user_id,
                                            journey_id, origin_dashboard,
                                            origin_reference_id, stage,
                                            risk_level, status)
        ON DELETE RESTRICT,
    ADD CONSTRAINT chk_context_origin_journey CHECK (
        (origin_dashboard = 'MOTHER_JOURNEY'
            AND journey_id IS NOT NULL
            AND origin_reference_id = journey_id
            AND triage_stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'))
        OR (origin_dashboard = 'BABY_PROFILE'
            AND journey_id IS NULL
            AND triage_stage IN ('INFANT', 'TODDLER'))
    ) NOT VALID;
ALTER TABLE public.consultation_context_shares
    VALIDATE CONSTRAINT chk_context_origin_journey;

DO $$
DECLARE
    triage_after bigint;
    shares_after bigint;
BEGIN
    SELECT count(*) INTO triage_after
      FROM public.triage_sessions
     WHERE origin_dashboard = 'BABY_PROFILE' AND journey_id IS NOT NULL;
    SELECT count(*) INTO shares_after
      FROM public.consultation_context_shares
     WHERE origin_dashboard = 'BABY_PROFILE' AND journey_id IS NOT NULL;
    RAISE NOTICE 'baby safety journey ids after detach: triage=%, shares=%',
        triage_after, shares_after;
END $$;
-- END CANONICAL SOURCE: V20260728010000__remove_baby_journey_linkage.sql

-- BEGIN CANONICAL SOURCE: V20260729030000__add_checklist_distribution_v2_foundation.sql
-- Checklist Distribution V2 foundation. Additive only; legacy tables remain authoritative until cutover.

CREATE TABLE public.checklist_substages (
    substage_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code varchar(80) NOT NULL,
    stage varchar(30) NOT NULL,
    anchor_type varchar(30) NOT NULL,
    range_unit varchar(10) NOT NULL,
    start_inclusive integer NOT NULL,
    end_inclusive integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_substages_pk PRIMARY KEY (substage_id),
    CONSTRAINT checklist_substages_code_uk UNIQUE (code),
    CONSTRAINT checklist_substages_stage_ck CHECK (stage IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE')),
    CONSTRAINT checklist_substages_anchor_ck CHECK (anchor_type IN ('NONE','LMP','EDD','DELIVERY_DATE','BIRTH_DATE')),
    CONSTRAINT checklist_substages_unit_ck CHECK (range_unit IN ('DAY','WEEK','MONTH')),
    CONSTRAINT checklist_substages_range_ck CHECK (start_inclusive >= 0 AND end_inclusive >= start_inclusive)
);

INSERT INTO public.checklist_substages
    (code, stage, anchor_type, range_unit, start_inclusive, end_inclusive)
VALUES
    ('LEGACY_PRE_PREGNANCY', 'PRE_PREGNANCY', 'NONE', 'DAY', 0, 2147483647),
    ('LEGACY_PREGNANCY', 'PREGNANCY', 'NONE', 'DAY', 0, 2147483647),
    ('LEGACY_POSTPARTUM', 'POSTPARTUM', 'NONE', 'DAY', 0, 2147483647),
    ('LEGACY_BABY_CARE', 'BABY_CARE', 'NONE', 'DAY', 0, 2147483647),
    ('PREGNANCY_LMP_WEEK_0_12', 'PREGNANCY', 'LMP', 'WEEK', 0, 12),
    ('PREGNANCY_EDD_WEEK_0_40', 'PREGNANCY', 'EDD', 'WEEK', 0, 40),
    ('POSTPARTUM_DAY_0_7', 'POSTPARTUM', 'DELIVERY_DATE', 'DAY', 0, 7),
    ('POSTPARTUM_WEEK_0_6', 'POSTPARTUM', 'DELIVERY_DATE', 'WEEK', 0, 6),
    ('BABY_CARE_DAY_0_28', 'BABY_CARE', 'BIRTH_DATE', 'DAY', 0, 28),
    ('BABY_CARE_MONTH_0_3', 'BABY_CARE', 'BIRTH_DATE', 'MONTH', 0, 3)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE public.checklist_migration_quarantine (
    quarantine_id uuid DEFAULT gen_random_uuid() NOT NULL,
    source_table varchar(80) NOT NULL,
    source_id uuid,
    reason_code varchar(80) NOT NULL,
    payload_ciphertext bytea NOT NULL,
    payload_hash char(64) NOT NULL,
    encryption_key_version varchar(40) NOT NULL,
    correlation_id uuid NOT NULL,
    resolved_at timestamptz,
    resolved_by uuid,
    resolution_code varchar(80),
    legal_hold boolean DEFAULT false NOT NULL,
    retain_until timestamptz NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_migration_quarantine_pk PRIMARY KEY (quarantine_id),
    CONSTRAINT checklist_migration_quarantine_hash_ck CHECK (payload_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX checklist_migration_quarantine_correlation_ix
    ON public.checklist_migration_quarantine(correlation_id, created_at DESC);

ALTER TABLE public.care_item_templates
    ADD COLUMN template_lineage_id uuid,
    ADD COLUMN template_version_id uuid,
    ADD COLUMN substage_id uuid,
    ADD COLUMN target_subject varchar(10),
    ADD COLUMN migration_review_required boolean DEFAULT false NOT NULL,
    ADD COLUMN distribution_enabled boolean DEFAULT false NOT NULL,
    ADD COLUMN approved_at timestamptz,
    ADD COLUMN approved_by uuid;

ALTER TABLE public.care_item_templates
    DROP CONSTRAINT care_item_templates_type_ck,
    ADD CONSTRAINT care_item_templates_type_ck CHECK (entry_type IN (
        'TEMPLATE_ROOT','CHECKLIST_ENTRY','QUARANTINED_CHECKLIST_ENTRY',
        'EXERCISE_TEMPLATE','POSTURE_CONFIG'));

UPDATE public.care_item_templates
SET template_lineage_id = template_id,
    template_version_id = template_id,
    migration_review_required = true,
    distribution_enabled = false
WHERE entry_type = 'TEMPLATE_ROOT';

UPDATE public.care_item_templates root
SET substage_id = substage.substage_id
FROM public.checklist_substages substage
WHERE root.entry_type = 'TEMPLATE_ROOT'
  AND substage.code = 'LEGACY_' || root.stage
  AND root.stage IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE');

WITH invalid_entries AS (
    SELECT
        item.template_id,
        CASE
            WHEN root.template_id IS NULL THEN 'LEGACY_PARENT_MISSING'
            WHEN root.entry_type <> 'TEMPLATE_ROOT' THEN 'LEGACY_PARENT_NOT_ROOT'
            ELSE 'LEGACY_PARENT_STAGE_INVALID'
        END AS reason_code
    FROM public.care_item_templates item
    LEFT JOIN public.care_item_templates root ON root.template_id = item.parent_template_id
    WHERE item.entry_type = 'CHECKLIST_ENTRY'
      AND (root.template_id IS NULL
           OR root.entry_type <> 'TEMPLATE_ROOT'
           OR root.stage IS NULL
           OR root.stage NOT IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE'))
), prepared_quarantine AS (
    SELECT
        template_id,
        reason_code,
        decode(
            md5(template_id::text || clock_timestamp()::text || random()::text) ||
            md5(gen_random_uuid()::text || random()::text),
            'hex') AS redacted_payload
    FROM invalid_entries
)
INSERT INTO public.checklist_migration_quarantine
    (source_table, source_id, reason_code, payload_ciphertext, payload_hash,
     encryption_key_version, correlation_id, retain_until)
SELECT
    'care_item_templates',
    template_id,
    reason_code,
    redacted_payload,
    md5(encode(redacted_payload, 'hex')) || md5('v2:' || encode(redacted_payload, 'hex')),
    'REDACTED_NO_PAYLOAD_V1',
    gen_random_uuid(),
    now() + interval '7 years'
FROM prepared_quarantine;

UPDATE public.care_item_templates item
SET entry_type = 'QUARANTINED_CHECKLIST_ENTRY',
    is_active = false,
    migration_review_required = true
FROM public.care_item_templates root
WHERE item.entry_type = 'CHECKLIST_ENTRY'
  AND item.parent_template_id = root.template_id
  AND (root.entry_type <> 'TEMPLATE_ROOT'
       OR root.stage IS NULL
       OR root.stage NOT IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE'));

UPDATE public.care_item_templates item
SET entry_type = 'QUARANTINED_CHECKLIST_ENTRY',
    is_active = false,
    migration_review_required = true
WHERE item.entry_type = 'CHECKLIST_ENTRY'
  AND NOT EXISTS (
      SELECT 1 FROM public.care_item_templates root
      WHERE root.template_id = item.parent_template_id
  );

UPDATE public.care_item_templates item
SET target_subject = CASE WHEN root.stage = 'BABY_CARE' THEN 'BABY' ELSE 'MOTHER' END
FROM public.care_item_templates root
WHERE item.entry_type = 'CHECKLIST_ENTRY'
  AND item.parent_template_id = root.template_id
  AND root.entry_type = 'TEMPLATE_ROOT'
  AND root.stage IN ('PRE_PREGNANCY','PREGNANCY','POSTPARTUM','BABY_CARE');

ALTER TABLE public.checklist_migration_quarantine ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.checklist_migration_quarantine FORCE ROW LEVEL SECURITY;
CREATE POLICY checklist_migration_quarantine_insert_policy
    ON public.checklist_migration_quarantine FOR INSERT WITH CHECK (true);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'checklist_operations') THEN
        EXECUTE 'GRANT SELECT, UPDATE ON public.checklist_migration_quarantine TO checklist_operations';
        EXECUTE 'CREATE POLICY checklist_migration_quarantine_operations_select
                 ON public.checklist_migration_quarantine FOR SELECT
                 TO checklist_operations USING (true)';
        EXECUTE 'CREATE POLICY checklist_migration_quarantine_operations_update
                 ON public.checklist_migration_quarantine FOR UPDATE
                 TO checklist_operations
                 USING (resolved_at IS NULL)
                 WITH CHECK (resolved_at IS NOT NULL AND resolved_by IS NOT NULL AND resolution_code IS NOT NULL)';
    END IF;
END $$;

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_version_id_uk UNIQUE (template_version_id),
    ADD CONSTRAINT care_item_templates_lineage_version_uk UNIQUE (template_lineage_id, template_version_id),
    ADD CONSTRAINT care_item_templates_version_root_uk UNIQUE (template_version_id, template_id),
    ADD CONSTRAINT care_item_templates_parent_item_uk UNIQUE (parent_template_id, template_id, entry_type),
    ADD CONSTRAINT care_item_templates_substage_fk FOREIGN KEY (substage_id)
        REFERENCES public.checklist_substages(substage_id) ON DELETE RESTRICT,
    ADD CONSTRAINT care_item_templates_target_ck CHECK
        ((entry_type <> 'CHECKLIST_ENTRY') OR
         (target_subject IS NOT NULL AND target_subject IN ('MOTHER','BABY'))),
    ADD CONSTRAINT care_item_templates_root_version_ck CHECK
        ((entry_type = 'TEMPLATE_ROOT' AND template_lineage_id IS NOT NULL AND template_version_id IS NOT NULL) OR
         (entry_type <> 'TEMPLATE_ROOT' AND template_lineage_id IS NULL AND template_version_id IS NULL)),
    ADD CONSTRAINT care_item_templates_distribution_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR distribution_enabled = false OR
         (migration_review_required = false AND approved_at IS NOT NULL AND approved_by IS NOT NULL));

CREATE INDEX care_item_templates_lineage_version_ix
    ON public.care_item_templates(template_lineage_id, version)
    WHERE entry_type = 'TEMPLATE_ROOT';

CREATE TABLE public.checklist_template_recipient_roles (
    template_version_id uuid NOT NULL,
    recipient_role varchar(10) NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_template_recipient_roles_pk PRIMARY KEY (template_version_id, recipient_role),
    CONSTRAINT checklist_template_recipient_roles_role_ck CHECK (recipient_role IN ('MOTHER','FAMILY')),
    CONSTRAINT checklist_template_recipient_roles_version_fk FOREIGN KEY (template_version_id)
        REFERENCES public.care_item_templates(template_version_id) ON DELETE CASCADE
);

INSERT INTO public.checklist_template_recipient_roles(template_version_id, recipient_role)
SELECT template_version_id, 'MOTHER' FROM public.care_item_templates
WHERE entry_type = 'TEMPLATE_ROOT' ON CONFLICT DO NOTHING;

CREATE TABLE public.checklist_template_version_items (
    template_version_id uuid NOT NULL,
    template_root_id uuid NOT NULL,
    template_item_version_id uuid NOT NULL,
    item_entry_type varchar(30) DEFAULT 'CHECKLIST_ENTRY' NOT NULL,
    CONSTRAINT checklist_template_version_items_pk
        PRIMARY KEY (template_version_id, template_item_version_id),
    CONSTRAINT checklist_template_version_items_type_ck CHECK (item_entry_type = 'CHECKLIST_ENTRY'),
    CONSTRAINT checklist_template_version_items_root_fk
        FOREIGN KEY (template_version_id, template_root_id)
        REFERENCES public.care_item_templates(template_version_id, template_id) ON DELETE RESTRICT,
    CONSTRAINT checklist_template_version_items_item_fk
        FOREIGN KEY (template_root_id, template_item_version_id, item_entry_type)
        REFERENCES public.care_item_templates(parent_template_id, template_id, entry_type) ON DELETE RESTRICT
);

INSERT INTO public.checklist_template_version_items
    (template_version_id, template_root_id, template_item_version_id)
SELECT root.template_version_id, root.template_id, item.template_id
FROM public.care_item_templates item
JOIN public.care_item_templates root ON root.template_id = item.parent_template_id
WHERE root.entry_type = 'TEMPLATE_ROOT'
  AND item.entry_type = 'CHECKLIST_ENTRY'
ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION public.checklist_sync_template_version_item()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM public.checklist_template_version_items
        WHERE template_item_version_id = OLD.template_id OR template_root_id = OLD.template_id;
        RETURN OLD;
    END IF;

    DELETE FROM public.checklist_template_version_items
    WHERE template_item_version_id = NEW.template_id OR template_root_id = NEW.template_id;

    IF NEW.entry_type = 'CHECKLIST_ENTRY' AND NEW.parent_template_id IS NOT NULL THEN
        INSERT INTO public.checklist_template_version_items
            (template_version_id, template_root_id, template_item_version_id)
        SELECT root.template_version_id, root.template_id, NEW.template_id
        FROM public.care_item_templates root
        WHERE root.template_id = NEW.parent_template_id
          AND root.entry_type = 'TEMPLATE_ROOT'
          AND root.template_version_id IS NOT NULL
        ON CONFLICT DO NOTHING;
    ELSIF NEW.entry_type = 'TEMPLATE_ROOT' AND NEW.template_version_id IS NOT NULL THEN
        INSERT INTO public.checklist_template_version_items
            (template_version_id, template_root_id, template_item_version_id)
        SELECT NEW.template_version_id, NEW.template_id, item.template_id
        FROM public.care_item_templates item
        WHERE item.parent_template_id = NEW.template_id
          AND item.entry_type = 'CHECKLIST_ENTRY'
        ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_sync_template_version_item_write_trg
AFTER INSERT OR UPDATE OF entry_type, parent_template_id, template_version_id
ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_template_version_item();

CREATE TRIGGER checklist_sync_template_version_item_delete_trg
BEFORE DELETE ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_template_version_item();

ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_id_owner_uk UNIQUE (care_group_id, owner_user_id);

ALTER TABLE public.mother_journeys
    ADD CONSTRAINT mother_journeys_id_owner_checklist_uk UNIQUE (journey_id, owner_user_id);
ALTER TABLE public.care_subjects
    ADD CONSTRAINT care_subjects_id_owner_type_checklist_uk
        UNIQUE (care_subject_id, owner_user_id, subject_type);

CREATE TABLE public.checklist_context_authorities (
    care_context_type varchar(10) NOT NULL,
    care_context_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    journey_id uuid,
    baby_id uuid,
    baby_subject_type varchar(30),
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_context_authorities_pk
        PRIMARY KEY (care_context_type, care_context_id, owner_user_id),
    CONSTRAINT checklist_context_authorities_shape_ck CHECK (
        (care_context_type = 'JOURNEY' AND journey_id = care_context_id
            AND baby_id IS NULL AND baby_subject_type IS NULL) OR
        (care_context_type = 'BABY' AND baby_id = care_context_id
            AND journey_id IS NULL AND baby_subject_type = 'BABY')
    ),
    CONSTRAINT checklist_context_authorities_journey_fk FOREIGN KEY (journey_id, owner_user_id)
        REFERENCES public.mother_journeys(journey_id, owner_user_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT checklist_context_authorities_baby_fk
        FOREIGN KEY (baby_id, owner_user_id, baby_subject_type)
        REFERENCES public.care_subjects(care_subject_id, owner_user_id, subject_type)
        ON UPDATE CASCADE ON DELETE CASCADE
);

INSERT INTO public.checklist_context_authorities
    (care_context_type, care_context_id, owner_user_id, journey_id)
SELECT 'JOURNEY', journey_id, owner_user_id, journey_id
FROM public.mother_journeys
ON CONFLICT DO NOTHING;

CREATE OR REPLACE FUNCTION public.checklist_sync_context_authority()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME = 'mother_journeys' THEN
        INSERT INTO public.checklist_context_authorities
            (care_context_type, care_context_id, owner_user_id, journey_id)
        VALUES ('JOURNEY', NEW.journey_id, NEW.owner_user_id, NEW.journey_id)
        ON CONFLICT (care_context_type, care_context_id, owner_user_id)
        DO UPDATE SET journey_id = EXCLUDED.journey_id;
    ELSIF NEW.subject_type = 'BABY' THEN
        INSERT INTO public.checklist_context_authorities
            (care_context_type, care_context_id, owner_user_id, baby_id, baby_subject_type)
        VALUES ('BABY', NEW.care_subject_id, NEW.owner_user_id, NEW.care_subject_id, 'BABY')
        ON CONFLICT (care_context_type, care_context_id, owner_user_id)
        DO UPDATE SET baby_id = EXCLUDED.baby_id, baby_subject_type = 'BABY';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_sync_journey_context_authority_trg
AFTER INSERT OR UPDATE OF owner_user_id ON public.mother_journeys
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_context_authority();

CREATE TRIGGER checklist_sync_baby_context_authority_trg
AFTER INSERT OR UPDATE OF owner_user_id, subject_type ON public.care_subjects
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_context_authority();

INSERT INTO public.checklist_context_authorities
    (care_context_type, care_context_id, owner_user_id, baby_id, baby_subject_type)
SELECT 'BABY', care_subject_id, owner_user_id, care_subject_id, 'BABY'
FROM public.care_subjects
WHERE subject_type = 'BABY'
ON CONFLICT DO NOTHING;

CREATE INDEX care_group_members_checklist_auth_ix
    ON public.care_group_members(care_group_id, user_id, invitation_status)
    WHERE invitation_status = 'ACCEPTED';

CREATE TABLE public.checklist_care_group_contexts (
    context_mapping_id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_group_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    care_context_type varchar(10) NOT NULL,
    care_context_id uuid NOT NULL,
    review_status varchar(20) DEFAULT 'UNREVIEWED' NOT NULL,
    distribution_blocked boolean DEFAULT true NOT NULL,
    block_reason_code varchar(80),
    reviewed_at timestamptz,
    reviewed_by uuid,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_care_group_contexts_pk PRIMARY KEY (context_mapping_id),
    CONSTRAINT checklist_care_group_contexts_group_context_uk UNIQUE (care_group_id, care_context_type, care_context_id),
    CONSTRAINT checklist_care_group_contexts_authority_uk UNIQUE (care_group_id, care_context_type, care_context_id, owner_user_id),
    CONSTRAINT checklist_care_group_contexts_type_ck CHECK (care_context_type IN ('JOURNEY','BABY')),
    CONSTRAINT checklist_care_group_contexts_review_ck CHECK (review_status IN ('UNREVIEWED','REVIEWED','BLOCKED')),
    CONSTRAINT checklist_care_group_contexts_review_shape_ck CHECK (
        (review_status = 'UNREVIEWED' AND distribution_blocked = true
            AND reviewed_at IS NULL AND reviewed_by IS NULL) OR
        (review_status = 'REVIEWED' AND distribution_blocked = false
            AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL AND block_reason_code IS NULL) OR
        (review_status = 'BLOCKED' AND distribution_blocked = true AND block_reason_code IS NOT NULL)
    ),
    CONSTRAINT checklist_care_group_contexts_group_owner_fk FOREIGN KEY (care_group_id, owner_user_id)
        REFERENCES public.care_groups(care_group_id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT checklist_care_group_contexts_canonical_owner_fk FOREIGN KEY
        (care_context_type, care_context_id, owner_user_id)
        REFERENCES public.checklist_context_authorities
        (care_context_type, care_context_id, owner_user_id) ON DELETE RESTRICT
);

CREATE TABLE public.checklist_instances (
    checklist_instance_id uuid DEFAULT gen_random_uuid() NOT NULL,
    distribution_key char(64) NOT NULL,
    key_version varchar(10) DEFAULT 'v1' NOT NULL,
    template_lineage_id uuid,
    template_version_id uuid,
    recipient_user_id uuid NOT NULL,
    recipient_role varchar(10) NOT NULL,
    care_group_id uuid NOT NULL,
    care_context_type varchar(10) NOT NULL,
    care_context_id uuid NOT NULL,
    context_owner_user_id uuid NOT NULL,
    origin varchar(20) NOT NULL,
    window_start date,
    window_end date,
    status varchar(20) DEFAULT 'PENDING' NOT NULL,
    lock_version bigint DEFAULT 0 NOT NULL,
    completed_at timestamptz,
    cancelled_at timestamptz,
    cancellation_reason_code varchar(80),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_instances_pk PRIMARY KEY (checklist_instance_id),
    CONSTRAINT checklist_instances_distribution_key_uk UNIQUE (distribution_key),
    CONSTRAINT checklist_instances_key_format_ck CHECK (distribution_key ~ '^[0-9a-f]{64}$'),
    CONSTRAINT checklist_instances_role_ck CHECK (recipient_role IN ('MOTHER','FAMILY')),
    CONSTRAINT checklist_instances_context_ck CHECK (care_context_type IN ('JOURNEY','BABY')),
    CONSTRAINT checklist_instances_origin_ck CHECK (origin IN ('SYSTEM_TEMPLATE','USER_CREATED')),
    CONSTRAINT checklist_instances_status_ck CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT checklist_instances_terminal_shape_ck CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL AND cancelled_at IS NULL) OR
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND completed_at IS NULL) OR
        (status IN ('PENDING','IN_PROGRESS') AND completed_at IS NULL AND cancelled_at IS NULL)
    ),
    CONSTRAINT checklist_instances_window_ck CHECK
        ((window_start IS NULL AND window_end IS NULL) OR
         (window_start IS NOT NULL AND window_end IS NOT NULL AND window_end >= window_start)),
    CONSTRAINT checklist_instances_template_pair_ck CHECK
        ((origin = 'SYSTEM_TEMPLATE' AND template_lineage_id IS NOT NULL AND template_version_id IS NOT NULL) OR
         (origin = 'USER_CREATED' AND template_lineage_id IS NULL AND template_version_id IS NULL)),
    CONSTRAINT checklist_instances_mother_recipient_ck CHECK
        ((recipient_role = 'MOTHER' AND recipient_user_id = context_owner_user_id) OR recipient_role = 'FAMILY'),
    CONSTRAINT checklist_instances_recipient_fk FOREIGN KEY (recipient_user_id)
        REFERENCES public.users(user_id) ON DELETE RESTRICT,
    CONSTRAINT checklist_instances_lineage_version_fk FOREIGN KEY (template_lineage_id, template_version_id)
        REFERENCES public.care_item_templates(template_lineage_id, template_version_id) ON DELETE RESTRICT,
    CONSTRAINT checklist_instances_template_recipient_role_fk
        FOREIGN KEY (template_version_id, recipient_role)
        REFERENCES public.checklist_template_recipient_roles(template_version_id, recipient_role)
        ON DELETE RESTRICT,
    CONSTRAINT checklist_instances_context_authority_fk FOREIGN KEY
        (care_group_id, care_context_type, care_context_id, context_owner_user_id)
        REFERENCES public.checklist_care_group_contexts
        (care_group_id, care_context_type, care_context_id, owner_user_id) ON DELETE RESTRICT
);

ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instances_id_version_uk UNIQUE (checklist_instance_id, template_version_id);

CREATE OR REPLACE FUNCTION public.checklist_validate_instance_recipient()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.recipient_role = 'FAMILY' THEN
        PERFORM 1
        FROM public.care_group_members member
        WHERE member.care_group_id = NEW.care_group_id
          AND member.user_id = NEW.recipient_user_id
          AND member.invitation_status = 'ACCEPTED'
          AND (member.invite_expires_at IS NULL OR member.invite_expires_at > now())
          AND jsonb_typeof(member.permission_json) = 'object'
          AND COALESCE(
              member.permission_json->>'CHECKLIST_VIEW',
              member.permission_json->>'checklist_view',
              'false') = 'true'
        FOR KEY SHARE;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'CHECKLIST_FAMILY_RECIPIENT_NOT_AUTHORIZED';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_validate_instance_recipient_trg
BEFORE INSERT ON public.checklist_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_instance_recipient();

CREATE INDEX checklist_instances_recipient_status_ix
    ON public.checklist_instances(recipient_user_id, status, created_at DESC);
CREATE INDEX checklist_instances_context_status_ix
    ON public.checklist_instances(care_group_id, care_context_type, care_context_id, status);

CREATE TABLE public.checklist_task_instances (
    checklist_task_instance_id uuid DEFAULT gen_random_uuid() NOT NULL,
    checklist_instance_id uuid NOT NULL,
    template_version_id uuid,
    template_item_version_id uuid,
    task_key char(64) NOT NULL,
    key_version varchar(10) DEFAULT 'v1' NOT NULL,
    title_snapshot varchar(500) NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    is_required boolean DEFAULT false NOT NULL,
    target_subject varchar(10) NOT NULL,
    due_at timestamptz,
    status varchar(20) DEFAULT 'PENDING' NOT NULL,
    lock_version bigint DEFAULT 0 NOT NULL,
    completed_at timestamptz,
    skipped_at timestamptz,
    cancelled_at timestamptz,
    action_reason_code varchar(80),
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_task_instances_pk PRIMARY KEY (checklist_task_instance_id),
    CONSTRAINT checklist_task_instances_task_key_uk UNIQUE (task_key),
    CONSTRAINT checklist_task_instances_key_format_ck CHECK (task_key ~ '^[0-9a-f]{64}$'),
    CONSTRAINT checklist_task_instances_target_ck CHECK (target_subject IN ('MOTHER','BABY')),
    CONSTRAINT checklist_task_instances_status_ck CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','SKIPPED','CANCELLED')),
    CONSTRAINT checklist_task_instances_terminal_shape_ck CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL
            AND skipped_at IS NULL AND cancelled_at IS NULL) OR
        (status = 'SKIPPED' AND skipped_at IS NOT NULL
            AND completed_at IS NULL AND cancelled_at IS NULL) OR
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL
            AND completed_at IS NULL AND skipped_at IS NULL) OR
        (status IN ('PENDING','IN_PROGRESS')
            AND completed_at IS NULL AND skipped_at IS NULL AND cancelled_at IS NULL)
    ),
    CONSTRAINT checklist_task_instances_parent_fk FOREIGN KEY (checklist_instance_id)
        REFERENCES public.checklist_instances(checklist_instance_id) ON DELETE CASCADE,
    CONSTRAINT checklist_task_instances_template_pair_ck CHECK
        ((template_version_id IS NULL) = (template_item_version_id IS NULL)),
    CONSTRAINT checklist_task_instances_parent_version_fk
        FOREIGN KEY (checklist_instance_id, template_version_id)
        REFERENCES public.checklist_instances(checklist_instance_id, template_version_id) ON DELETE CASCADE,
    CONSTRAINT checklist_task_instances_version_item_fk
        FOREIGN KEY (template_version_id, template_item_version_id)
        REFERENCES public.checklist_template_version_items(template_version_id, template_item_version_id)
        ON DELETE RESTRICT
);

CREATE OR REPLACE FUNCTION public.checklist_validate_task_template()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    parent_origin varchar(20);
    parent_version_id uuid;
BEGIN
    SELECT origin, template_version_id
    INTO parent_origin, parent_version_id
    FROM public.checklist_instances
    WHERE checklist_instance_id = NEW.checklist_instance_id;

    IF parent_origin = 'SYSTEM_TEMPLATE' AND
       (NEW.template_version_id IS NULL OR NEW.template_item_version_id IS NULL OR
        NEW.template_version_id <> parent_version_id) THEN
        RAISE EXCEPTION 'CHECKLIST_SYSTEM_TASK_TEMPLATE_REQUIRED';
    END IF;
    IF parent_origin = 'USER_CREATED' AND
       (NEW.template_version_id IS NOT NULL OR NEW.template_item_version_id IS NOT NULL) THEN
        RAISE EXCEPTION 'CHECKLIST_USER_TASK_TEMPLATE_FORBIDDEN';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_validate_task_template_trg
BEFORE INSERT OR UPDATE OF checklist_instance_id, template_version_id, template_item_version_id
ON public.checklist_task_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_task_template();

CREATE INDEX checklist_task_instances_parent_order_ix
    ON public.checklist_task_instances(checklist_instance_id, display_order);
CREATE INDEX checklist_task_instances_due_status_ix
    ON public.checklist_task_instances(due_at, status);

CREATE TABLE public.checklist_action_commands (
    checklist_action_command_id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid NOT NULL,
    task_kind varchar(30) NOT NULL,
    task_id uuid NOT NULL,
    client_request_id uuid NOT NULL,
    payload_hash char(64) NOT NULL,
    action_type varchar(30) NOT NULL,
    result_status varchar(20) NOT NULL,
    result_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    applied_at timestamptz DEFAULT now() NOT NULL,
    retain_until timestamptz NOT NULL,
    legal_hold boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT checklist_action_commands_pk PRIMARY KEY (checklist_action_command_id),
    CONSTRAINT checklist_action_commands_scope_uk UNIQUE (actor_user_id, task_kind, task_id, client_request_id),
    CONSTRAINT checklist_action_commands_payload_ck CHECK (payload_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT checklist_action_commands_task_kind_ck CHECK (task_kind IN ('CHECKLIST','CARE_TASK','REMINDER')),
    CONSTRAINT checklist_action_commands_action_ck CHECK (action_type IN ('COMPLETE','SKIP')),
    CONSTRAINT checklist_action_commands_result_ck CHECK
        (result_status IN ('APPLIED','IDEMPOTENT_REPLAY','REJECTED')),
    CONSTRAINT checklist_action_commands_actor_fk FOREIGN KEY (actor_user_id)
        REFERENCES public.users(user_id) ON DELETE RESTRICT
);

CREATE OR REPLACE FUNCTION public.checklist_validate_action_command_target()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.task_kind = 'CHECKLIST' THEN
        PERFORM 1 FROM public.checklist_task_instances task
        WHERE task.checklist_task_instance_id = NEW.task_id;
    ELSE
        PERFORM 1 FROM public.care_tasks task WHERE task.task_id = NEW.task_id;
    END IF;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_ACTION_TARGET_NOT_FOUND';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_validate_action_command_target_trg
BEFORE INSERT ON public.checklist_action_commands
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_action_command_target();

CREATE TABLE public.checklist_distribution_outbox (
    outbox_event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    event_type varchar(80) NOT NULL,
    aggregate_type varchar(50) NOT NULL,
    aggregate_id uuid NOT NULL,
    payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    correlation_id uuid NOT NULL,
    occurred_at timestamptz DEFAULT now() NOT NULL,
    processed_at timestamptz,
    attempt_count integer DEFAULT 0 NOT NULL,
    next_attempt_at timestamptz,
    last_error_code varchar(80),
    CONSTRAINT checklist_distribution_outbox_pk PRIMARY KEY (outbox_event_id),
    CONSTRAINT checklist_distribution_outbox_attempt_ck CHECK (attempt_count >= 0)
);

CREATE INDEX checklist_distribution_outbox_pending_ix
    ON public.checklist_distribution_outbox(processed_at, next_attempt_at, occurred_at)
    WHERE processed_at IS NULL;

CREATE TABLE public.checklist_reconciliation_runs (
    reconciliation_run_id uuid DEFAULT gen_random_uuid() NOT NULL,
    correlation_id uuid NOT NULL,
    trigger_type varchar(20) NOT NULL,
    started_at timestamptz DEFAULT now() NOT NULL,
    completed_at timestamptz,
    created_count integer DEFAULT 0 NOT NULL,
    existing_count integer DEFAULT 0 NOT NULL,
    cancelled_count integer DEFAULT 0 NOT NULL,
    failed_count integer DEFAULT 0 NOT NULL,
    status varchar(20) DEFAULT 'RUNNING' NOT NULL,
    CONSTRAINT checklist_reconciliation_runs_pk PRIMARY KEY (reconciliation_run_id),
    CONSTRAINT checklist_reconciliation_runs_correlation_uk UNIQUE (correlation_id),
    CONSTRAINT checklist_reconciliation_runs_trigger_ck CHECK (trigger_type IN ('EVENT','HOURLY','STARTUP','REPLAY')),
    CONSTRAINT checklist_reconciliation_runs_status_ck CHECK (status IN ('RUNNING','SUCCEEDED','PARTIAL','FAILED')),
    CONSTRAINT checklist_reconciliation_runs_counts_ck CHECK (
        created_count >= 0 AND existing_count >= 0 AND cancelled_count >= 0 AND failed_count >= 0)
);

CREATE TABLE public.checklist_reconciliation_candidates (
    reconciliation_candidate_id uuid DEFAULT gen_random_uuid() NOT NULL,
    reconciliation_run_id uuid NOT NULL,
    recipient_user_id uuid,
    care_group_id uuid,
    care_context_type varchar(10),
    care_context_id uuid,
    window_start date,
    window_end date,
    outcome varchar(20) DEFAULT 'PENDING' NOT NULL,
    retry_count integer DEFAULT 0 NOT NULL,
    failure_code varchar(80),
    completed_at timestamptz,
    CONSTRAINT checklist_reconciliation_candidates_pk PRIMARY KEY (reconciliation_candidate_id),
    CONSTRAINT checklist_reconciliation_candidates_outcome_ck CHECK
        (outcome IN ('PENDING','CREATED','EXISTING','CANCELLED','FAILED','QUARANTINED')),
    CONSTRAINT checklist_reconciliation_candidates_retry_ck CHECK (retry_count >= 0),
    CONSTRAINT checklist_reconciliation_candidates_context_ck CHECK (
        (recipient_user_id IS NULL AND care_group_id IS NULL
            AND care_context_type IS NULL AND care_context_id IS NULL) OR
        (recipient_user_id IS NOT NULL AND care_group_id IS NOT NULL
            AND care_context_type IN ('JOURNEY','BABY') AND care_context_id IS NOT NULL)
    ),
    CONSTRAINT checklist_reconciliation_candidates_window_ck CHECK (
        (window_start IS NULL AND window_end IS NULL) OR
        (window_start IS NOT NULL AND window_end IS NOT NULL AND window_end >= window_start)
    ),
    CONSTRAINT checklist_reconciliation_candidates_identity_uk UNIQUE NULLS NOT DISTINCT
        (reconciliation_run_id, recipient_user_id, care_group_id,
         care_context_type, care_context_id, window_start, window_end),
    CONSTRAINT checklist_reconciliation_candidates_run_fk FOREIGN KEY (reconciliation_run_id)
        REFERENCES public.checklist_reconciliation_runs(reconciliation_run_id) ON DELETE CASCADE
);

ALTER TABLE public.care_group_members ALTER COLUMN permission_json SET DEFAULT '{}'::jsonb;
WITH normalized_permissions AS (
    SELECT care_group_member_id,
           CASE WHEN jsonb_typeof(permission_json) = 'object'
                THEN permission_json ELSE '{}'::jsonb END AS base_permission
    FROM public.care_group_members
)
UPDATE public.care_group_members member
SET permission_json = jsonb_set(
    jsonb_set(base_permission, '{CHECKLIST_VIEW}',
        CASE WHEN jsonb_typeof(base_permission->'CHECKLIST_VIEW') = 'boolean'
             THEN base_permission->'CHECKLIST_VIEW' ELSE 'false'::jsonb END, true),
    '{CHECKLIST_COMPLETE}',
        CASE WHEN jsonb_typeof(base_permission->'CHECKLIST_COMPLETE') = 'boolean'
             THEN base_permission->'CHECKLIST_COMPLETE' ELSE 'false'::jsonb END, true)
FROM normalized_permissions normalized
WHERE normalized.care_group_member_id = member.care_group_member_id;

ALTER TABLE public.audit_events
    ADD COLUMN actor_type varchar(20),
    ADD COLUMN actor_service varchar(80),
    ADD COLUMN reason_code varchar(80),
    ADD COLUMN care_context_type varchar(10),
    ADD COLUMN care_context_id uuid,
    ADD COLUMN template_version_id uuid,
    ADD COLUMN checklist_task_instance_id uuid;

ALTER TABLE public.audit_events
    ADD CONSTRAINT audit_events_checklist_actor_type_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         (actor_type IS NOT NULL AND actor_type IN ('USER','SYSTEM','SERVICE'))),
    ADD CONSTRAINT audit_events_checklist_actor_shape_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         ((actor_type = 'USER' AND actor_user_id IS NOT NULL AND actor_service IS NULL) OR
          (actor_type IN ('SYSTEM','SERVICE') AND actor_user_id IS NULL
            AND actor_service IS NOT NULL AND btrim(actor_service) <> ''))),
    ADD CONSTRAINT audit_events_checklist_correlation_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         correlation_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_context_type_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         care_context_type IS NULL OR care_context_type IN ('JOURNEY','BABY')),
    ADD CONSTRAINT audit_events_checklist_context_pair_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         ((care_context_type IS NULL AND care_context_id IS NULL) OR
          (care_context_type IS NOT NULL AND care_context_id IS NOT NULL))),
    ADD CONSTRAINT audit_events_checklist_subject_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED',
            'CHECKLIST_SKIPPED','CHECKLIST_CANCELLED') OR subject_user_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_task_ck CHECK
        (event_category NOT IN ('CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED') OR
         checklist_task_instance_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_template_ck CHECK
        (event_category NOT IN ('CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED') OR
         template_version_id IS NOT NULL),
    ADD CONSTRAINT audit_events_checklist_reason_code_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_DISTRIBUTED','CHECKLIST_ASSIGNED','CHECKLIST_COMPLETED','CHECKLIST_SKIPPED',
            'CHECKLIST_CANCELLED','CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         reason_code IS NULL OR reason_code ~ '^[A-Z0-9_]{1,80}$'),
    ADD CONSTRAINT audit_events_checklist_reason_required_ck CHECK
        (event_category NOT IN (
            'CHECKLIST_SKIPPED','CHECKLIST_CANCELLED',
            'CHECKLIST_RECONCILIATION_FAILED','CHECKLIST_MIGRATION_QUARANTINED') OR
         reason_code IS NOT NULL);

CREATE INDEX checklist_distribution_outbox_correlation_ix
    ON public.checklist_distribution_outbox(correlation_id, occurred_at DESC);

CREATE INDEX audit_events_checklist_correlation_ix
    ON public.audit_events(correlation_id, occurred_at DESC)
    WHERE event_category LIKE 'CHECKLIST_%';
-- END CANONICAL SOURCE: V20260729030000__add_checklist_distribution_v2_foundation.sql

-- BEGIN CANONICAL SOURCE: V20260729040000__add_checklist_template_authoring_v2_guards.sql
-- Checklist Distribution V2 Phase 2: immutable approved authoring versions and activation guards.

ALTER TABLE public.care_item_templates
    ADD COLUMN migration_reviewed_at timestamptz,
    ADD COLUMN migration_reviewed_by uuid;

-- Normalize imported approved roots into the correctable review state before guards are installed.
UPDATE public.care_item_templates
SET content_status = 'PENDING_REVIEW',
    distribution_enabled = false,
    approved_at = NULL,
    approved_by = NULL
WHERE entry_type = 'TEMPLATE_ROOT'
  AND migration_review_required = true
  AND content_status = 'APPROVED';

ALTER TABLE public.care_item_templates
    DROP CONSTRAINT care_item_templates_distribution_gate_ck,
    ADD CONSTRAINT care_item_templates_distribution_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR distribution_enabled = false OR
         (content_status = 'APPROVED' AND migration_review_required = false
          AND approved_at IS NOT NULL AND approved_by IS NOT NULL)),
    ADD CONSTRAINT care_item_templates_approved_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR content_status <> 'APPROVED' OR
         (migration_review_required = false
          AND approved_at IS NOT NULL AND approved_by IS NOT NULL));

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_import_activation_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR migration_reviewed_at IS NULL
         OR content_status <> 'APPROVED' OR distribution_enabled = true);

CREATE UNIQUE INDEX care_item_templates_lineage_version_no_uk
    ON public.care_item_templates(template_lineage_id, version)
    WHERE entry_type = 'TEMPLATE_ROOT';

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_template_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
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

    -- Activation and archive may change state only; reviewed content identity must remain exact.
    reviewed_content_unchanged :=
        NEW.title IS NOT DISTINCT FROM OLD.title
        AND NEW.description IS NOT DISTINCT FROM OLD.description
        AND NEW.stage IS NOT DISTINCT FROM OLD.stage
        AND NEW.substage_id IS NOT DISTINCT FROM OLD.substage_id
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
        AND NEW.distribution_enabled = true
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
END $$;

CREATE TRIGGER checklist_guard_approved_template_mutation_trg
BEFORE UPDATE OR DELETE ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_approved_template_mutation();

CREATE OR REPLACE FUNCTION public.checklist_guard_referenced_substage_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.care_item_templates root
        WHERE root.substage_id = OLD.substage_id
          AND root.entry_type = 'TEMPLATE_ROOT'
          AND (root.content_status IN ('APPROVED', 'ARCHIVED')
               OR root.migration_reviewed_at IS NOT NULL)
    ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER checklist_guard_referenced_substage_mutation_trg
BEFORE UPDATE OR DELETE ON public.checklist_substages
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_referenced_substage_mutation();

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_item_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    parent_id uuid;
    parent_status varchar(20);
    parent_review_required boolean;
    parent_reviewed_at timestamptz;
BEGIN
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

CREATE TRIGGER checklist_guard_approved_item_mutation_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_approved_item_mutation();

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_role_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    version_id uuid;
    version_status varchar(20);
    version_review_required boolean;
    version_reviewed_at timestamptz;
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.template_version_id IS DISTINCT FROM NEW.template_version_id
       AND EXISTS (
           SELECT 1
           FROM public.care_item_templates root
           WHERE root.template_version_id IN (OLD.template_version_id, NEW.template_version_id)
             AND root.entry_type = 'TEMPLATE_ROOT'
             AND (root.content_status IN ('APPROVED', 'ARCHIVED')
                  OR root.migration_reviewed_at IS NOT NULL)
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;

    version_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.template_version_id ELSE NEW.template_version_id END;
    SELECT content_status, migration_review_required, migration_reviewed_at
    INTO version_status, version_review_required, version_reviewed_at
    FROM public.care_item_templates
    WHERE template_version_id = version_id AND entry_type = 'TEMPLATE_ROOT';

    IF (version_status IN ('APPROVED', 'ARCHIVED') AND version_review_required = false)
       OR version_reviewed_at IS NOT NULL THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER checklist_guard_approved_role_mutation_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.checklist_template_recipient_roles
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_approved_role_mutation();

CREATE OR REPLACE FUNCTION public.checklist_guard_version_item_authority_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    version_id uuid;
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.template_version_id IS DISTINCT FROM NEW.template_version_id
       AND EXISTS (
           SELECT 1 FROM public.care_item_templates root
           WHERE root.template_version_id IN (OLD.template_version_id, NEW.template_version_id)
             AND root.entry_type = 'TEMPLATE_ROOT'
             AND (root.content_status IN ('APPROVED', 'ARCHIVED')
                  OR root.migration_reviewed_at IS NOT NULL)
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;

    version_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.template_version_id ELSE NEW.template_version_id END;
    IF EXISTS (
        SELECT 1 FROM public.care_item_templates root
        WHERE root.template_version_id = version_id
          AND root.entry_type = 'TEMPLATE_ROOT'
          AND (root.content_status IN ('APPROVED', 'ARCHIVED')
               OR root.migration_reviewed_at IS NOT NULL)
    ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER checklist_guard_version_item_authority_mutation_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.checklist_template_version_items
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_version_item_authority_mutation();

CREATE OR REPLACE FUNCTION public.checklist_validate_template_approval()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    role_count integer;
    mother_count integer;
    family_count integer;
    substage_stage varchar(30);
    substage_anchor varchar(30);
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' THEN
        RETURN NEW;
    END IF;

    IF (NEW.distribution_enabled = true OR NEW.content_status = 'APPROVED')
       AND NEW.migration_review_required = true THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF TG_OP = 'UPDATE' AND NEW.distribution_enabled = true
       AND OLD.migration_review_required = true THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF TG_OP = 'UPDATE'
       AND OLD.migration_review_required = true AND NEW.migration_review_required = false
       AND (NEW.migration_reviewed_at IS NULL OR NEW.migration_reviewed_by IS NULL) THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF NEW.distribution_enabled = true
       OR NEW.content_status = 'APPROVED'
       OR (TG_OP = 'UPDATE' AND OLD.migration_review_required = true
           AND NEW.migration_review_required = false) THEN
        SELECT count(*),
               count(*) FILTER (WHERE recipient_role = 'MOTHER'),
               count(*) FILTER (WHERE recipient_role = 'FAMILY')
        INTO role_count, mother_count, family_count
        FROM public.checklist_template_recipient_roles
        WHERE template_version_id = NEW.template_version_id;

        IF role_count = 0 THEN
            RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED';
        END IF;

        IF family_count > 0 AND mother_count = 0
           AND (NEW.stage IS NOT NULL OR NEW.substage_id IS NOT NULL) THEN
            RAISE EXCEPTION 'FAMILY_STAGE_NOT_ALLOWED';
        END IF;

        IF NEW.substage_id IS NOT NULL THEN
            SELECT stage, anchor_type INTO substage_stage, substage_anchor
            FROM public.checklist_substages
            WHERE substage_id = NEW.substage_id
              AND is_active = true;
            IF substage_stage IS NULL OR substage_stage IS DISTINCT FROM NEW.stage
               OR substage_anchor IS NULL OR substage_anchor = 'NONE'
               OR NOT (
                   (NEW.stage = 'PREGNANCY' AND substage_anchor IN ('LMP', 'EDD')) OR
                   (NEW.stage = 'POSTPARTUM' AND substage_anchor = 'DELIVERY_DATE') OR
                   (NEW.stage = 'BABY_CARE' AND substage_anchor = 'BIRTH_DATE')
               ) THEN
                RAISE EXCEPTION 'SUBSTAGE_STAGE_MISMATCH';
            END IF;
        ELSIF NEW.stage IS NOT NULL THEN
            RAISE EXCEPTION 'SUBSTAGE_STAGE_MISMATCH';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM public.care_item_templates item
            WHERE item.parent_template_id = NEW.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY'
              AND item.is_active = true
              AND item.target_subject IS NULL
        ) THEN
            RAISE EXCEPTION 'ITEM_TARGET_REQUIRED';
        END IF;

        -- The maintained authority table must contain every active versioned item.
        IF EXISTS (
            SELECT 1
            FROM public.care_item_templates item
            WHERE item.parent_template_id = NEW.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY'
              AND item.is_active = true
              AND NOT EXISTS (
                  SELECT 1
                  FROM public.checklist_template_version_items version_item
                  WHERE version_item.template_version_id = NEW.template_version_id
                    AND version_item.template_item_version_id = item.template_id
              )
        ) THEN
            RAISE EXCEPTION 'ITEM_TARGET_REQUIRED';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_validate_template_approval_trg
BEFORE INSERT OR UPDATE OF content_status, distribution_enabled, migration_review_required,
    stage, substage_id, template_version_id
ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_template_approval();
-- END CANONICAL SOURCE: V20260729040000__add_checklist_template_authoring_v2_guards.sql

-- BEGIN CANONICAL SOURCE: V20260729050000__add_checklist_distribution_timing_and_reconciliation_guards.sql
-- Checklist Distribution V2 Phase 3: versioned item timing and durable reconciliation guards.

ALTER TABLE public.care_item_templates
    ADD COLUMN due_anchor_type varchar(30),
    ADD COLUMN due_offset_start integer,
    ADD COLUMN due_offset_end integer,
    ADD COLUMN due_offset_unit varchar(10);

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_due_timing_ck CHECK (
        entry_type <> 'CHECKLIST_ENTRY'
        OR (
            due_anchor_type IS NULL
            AND due_offset_start IS NULL
            AND due_offset_end IS NULL
            AND due_offset_unit IS NULL
        )
        OR (
            due_anchor_type IS NOT NULL
            AND due_offset_start IS NOT NULL
            AND due_offset_end IS NOT NULL
            AND due_offset_unit IS NOT NULL
            AND due_anchor_type IN ('LMP', 'EDD', 'DELIVERY_DATE', 'BIRTH_DATE')
            AND due_offset_start >= 0
            AND due_offset_end >= due_offset_start
            AND due_offset_unit IN ('DAY', 'WEEK', 'MONTH')
        )
    );

ALTER TABLE public.checklist_distribution_outbox
    ADD CONSTRAINT checklist_distribution_outbox_event_uk UNIQUE
        (correlation_id, event_type, aggregate_type, aggregate_id),
    ADD CONSTRAINT checklist_distribution_outbox_retry_ck CHECK (
        attempt_count BETWEEN 0 AND 5
        AND (processed_at IS NULL OR last_error_code IS NULL)
    );

CREATE INDEX checklist_distribution_outbox_retry_ix
    ON public.checklist_distribution_outbox(next_attempt_at, occurred_at, outbox_event_id)
    WHERE processed_at IS NULL AND attempt_count < 5;

CREATE INDEX checklist_distribution_outbox_exhausted_ix
    ON public.checklist_distribution_outbox(occurred_at, correlation_id)
    WHERE processed_at IS NULL AND attempt_count >= 5;

CREATE INDEX checklist_reconciliation_success_watermark_ix
    ON public.checklist_reconciliation_runs(completed_at DESC, reconciliation_run_id)
    WHERE status = 'SUCCEEDED';
-- END CANONICAL SOURCE: V20260729050000__add_checklist_distribution_timing_and_reconciliation_guards.sql

-- BEGIN CANONICAL SOURCE: V20260729060000__add_unified_task_origin_target.sql
-- Unified Today task metadata. Additive migration; legacy writes remain available
-- only through compatibility adapters until the V2 cutover.
ALTER TABLE public.care_tasks
    ADD COLUMN origin varchar(20),
    ADD COLUMN target_subject varchar(10);

UPDATE public.care_tasks
SET origin = CASE
        WHEN task_type = 'MANUAL_TASK' THEN 'USER_CREATED'
        WHEN source_reference_id IS NOT NULL OR source_reference_type IS NOT NULL THEN 'SYSTEM_TEMPLATE'
        ELSE 'USER_CREATED'
    END
WHERE origin IS NULL;

WITH resolved_targets AS (
    SELECT task.task_id,
           CASE
               WHEN subject.subject_type = 'BABY' THEN 'BABY'
               WHEN subject.subject_type = 'MOTHER' THEN 'MOTHER'
               WHEN task.baby_id IS NOT NULL THEN 'BABY'
               ELSE 'MOTHER'
           END AS target_subject
    FROM public.care_tasks task
    LEFT JOIN public.care_subjects subject
        ON subject.care_subject_id = task.care_subject_id
)
UPDATE public.care_tasks task
SET target_subject = resolved.target_subject
FROM resolved_targets resolved
WHERE task.task_id = resolved.task_id
  AND task.target_subject IS NULL;

ALTER TABLE public.care_tasks
    ALTER COLUMN origin SET DEFAULT 'USER_CREATED',
    ALTER COLUMN target_subject SET DEFAULT 'MOTHER',
    ALTER COLUMN origin SET NOT NULL,
    ALTER COLUMN target_subject SET NOT NULL,
    ADD CONSTRAINT care_tasks_origin_ck CHECK (origin IN ('SYSTEM_TEMPLATE','USER_CREATED')),
    ADD CONSTRAINT care_tasks_target_subject_ck CHECK (target_subject IN ('MOTHER','BABY'));

CREATE INDEX care_tasks_today_scope_ix
    ON public.care_tasks(assignee_user_id, status, scheduled_at, care_group_id);
-- END CANONICAL SOURCE: V20260729060000__add_unified_task_origin_target.sql

-- BEGIN CANONICAL SOURCE: V20260729070000__backfill_legacy_checklist_v2.sql
-- Backfill legacy preparation checklist rows into recipient-owned V2 projections.
-- A missing due_at remains active with due_at NULL and is projected as UNSCHEDULED.
-- Invalid, ambiguous, or conflicting rows are quarantined with redacted markers and
-- one typed audit event per newly persisted quarantine result.

-- This session-local helper is byte-for-byte compatible with
-- ChecklistDistributionKeyFactory v1: "v1" plus UTF-8 byte-length-prefixed tokens,
-- hashed once with SHA-256 and rendered as lowercase hexadecimal.
CREATE TEMP TABLE checklist_legacy_key_bootstrap (marker boolean) ON COMMIT DROP;

CREATE OR REPLACE FUNCTION pg_temp.checklist_v1_key(VARIADIC tokens text[])
RETURNS text
LANGUAGE sql
IMMUTABLE
STRICT
AS $$
    SELECT encode(sha256(convert_to(
        'v1' || COALESCE(string_agg(
            octet_length(convert_to(token, 'UTF8'))::text || ':' || token,
            '' ORDER BY ordinal), ''),
        'UTF8')), 'hex')
    FROM unnest(tokens) WITH ORDINALITY AS ordered_tokens(token, ordinal)
$$;

-- Approved runtime v1 golden vector. Abort before touching data if the SQL
-- implementation ever drifts from the Java implementation.
DO $$
BEGIN
    IF pg_temp.checklist_v1_key(
            '11111111-1111-1111-1111-111111111111',
            '22222222-2222-2222-2222-222222222222',
            'MOTHER',
            '33333333-3333-3333-3333-333333333333',
            'JOURNEY',
            '44444444-4444-4444-4444-444444444444',
            'NONE',
            'NONE') <>
            'fad7bba6cefeb717acaf887b59410cef7184b88706e67cdf828be0240678369d' THEN
        RAISE EXCEPTION 'CHECKLIST_V1_KEY_GOLDEN_VECTOR_MISMATCH';
    END IF;
END $$;

CREATE TEMP TABLE checklist_legacy_backfill_stage ON COMMIT DROP AS
SELECT
    legacy.checklist_item_id AS source_id,
    legacy.owner_user_id,
    legacy.title,
    COALESCE(legacy.display_order, 0) AS display_order,
    legacy.status AS legacy_status,
    legacy.due_at,
    legacy.completed_at,
    legacy.created_at,
    legacy.updated_at,
    legacy.template_entry_id,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN 'BABY'
        WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN 'JOURNEY'
        ELSE NULL
    END AS care_context_type,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN legacy.baby_id
        WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN legacy.mother_journey_id
        ELSE NULL
    END AS care_context_id,
    context_match.care_group_id,
    COALESCE(context_match.match_count, 0) AS context_match_count,
    root.template_lineage_id,
    root.template_version_id,
    item.template_id AS template_item_version_id,
    COALESCE(item.target_subject,
        CASE WHEN legacy.baby_id IS NOT NULL THEN 'BABY' ELSE 'MOTHER' END) AS target_subject,
    CASE WHEN legacy.template_entry_id IS NULL THEN 'USER_CREATED' ELSE 'SYSTEM_TEMPLATE' END AS origin,
    CASE WHEN legacy.template_entry_id IS NULL THEN '<ABSENT>' ELSE 'NONE' END AS occurrence_start_token,
    CASE WHEN legacy.template_entry_id IS NULL THEN '<ABSENT>' ELSE 'NONE' END AS occurrence_end_token,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NOT NULL
            THEN 'AMBIGUOUS_LEGACY_CONTEXT'
        WHEN legacy.baby_id IS NULL AND legacy.mother_journey_id IS NULL
            THEN 'UNKNOWN_LEGACY_CONTEXT'
        WHEN COALESCE(context_match.match_count, 0) = 0
            THEN 'CONTEXT_OWNER_MISMATCH'
        WHEN context_match.match_count > 1
            THEN 'MULTIPLE_CONTEXT_BINDINGS'
        WHEN legacy.template_entry_id IS NOT NULL AND
             (item.template_id IS NULL OR root.template_version_id IS NULL OR root.template_lineage_id IS NULL)
            THEN 'UNKNOWN_TEMPLATE_ROOT'
        WHEN legacy.title IS NULL OR btrim(legacy.title) = ''
            THEN 'INVALID_LEGACY_TITLE'
        WHEN legacy.status IS NULL OR upper(legacy.status) NOT IN
             ('OPEN','PENDING','IN_PROGRESS','COMPLETED','DONE','SKIPPED','CANCELLED')
            THEN 'UNKNOWN_LEGACY_STATUS'
        WHEN legacy.completed_at IS NOT NULL AND upper(legacy.status) IN ('OPEN','PENDING','IN_PROGRESS')
            THEN 'CONTRADICTORY_LEGACY_TIMESTAMPS'
        ELSE NULL
    END AS reason_code
FROM public.preparation_checklist_items legacy
LEFT JOIN public.care_item_templates item
    ON item.template_id = legacy.template_entry_id
   AND item.entry_type = 'CHECKLIST_ENTRY'
LEFT JOIN public.care_item_templates root
    ON root.template_id = item.parent_template_id
   AND root.entry_type = 'TEMPLATE_ROOT'
LEFT JOIN LATERAL (
    SELECT candidate.care_group_id, count(*) OVER () AS match_count
    FROM public.checklist_care_group_contexts candidate
    WHERE candidate.owner_user_id = legacy.owner_user_id
      AND candidate.review_status <> 'BLOCKED'
      AND (
          (legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL
              AND candidate.care_context_type = 'BABY'
              AND candidate.care_context_id = legacy.baby_id)
          OR
          (legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL
              AND candidate.care_context_type = 'JOURNEY'
              AND candidate.care_context_id = legacy.mother_journey_id)
      )
    ORDER BY candidate.care_group_id
    LIMIT 1
) context_match ON true;

CREATE TEMP TABLE checklist_legacy_quarantine_results (
    source_id uuid NOT NULL,
    reason_code varchar(80) NOT NULL,
    correlation_id uuid NOT NULL DEFAULT gen_random_uuid(),
    redacted_payload bytea NOT NULL DEFAULT sha256(convert_to(
        gen_random_uuid()::text || clock_timestamp()::text || random()::text,
        'UTF8')),
    payload_hash char(64),
    CONSTRAINT checklist_legacy_quarantine_results_uk UNIQUE (source_id, reason_code)
) ON COMMIT DROP;

INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT stage.source_id, stage.reason_code
FROM checklist_legacy_backfill_stage stage
WHERE stage.reason_code IS NOT NULL;

-- Calculate the exact runtime parent key first, then group all legacy children by
-- owner/context/version/occurrence instead of creating one parent per legacy row.
CREATE TEMP TABLE checklist_legacy_backfill_rows ON COMMIT DROP AS
WITH keyed AS (
    SELECT
        stage.*,
        CASE
            WHEN stage.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
                stage.template_version_id::text,
                stage.owner_user_id::text,
                'MOTHER',
                stage.care_group_id::text,
                stage.care_context_type,
                stage.care_context_id::text,
                stage.occurrence_start_token,
                stage.occurrence_end_token)
            ELSE pg_temp.checklist_v1_key(
                stage.owner_user_id::text,
                'MOTHER',
                stage.care_group_id::text,
                stage.care_context_type,
                stage.care_context_id::text,
                stage.occurrence_start_token,
                stage.occurrence_end_token)
        END AS legacy_parent_group_key
    FROM checklist_legacy_backfill_stage stage
    WHERE stage.reason_code IS NULL
), identified AS (
    SELECT
        keyed.*,
        pg_temp.checklist_v1_key('LEGACY_PARENT_ID', keyed.legacy_parent_group_key) AS parent_identity_hash
    FROM keyed
)
SELECT
    identified.*,
    (substr(parent_identity_hash, 1, 8) || '-' ||
     substr(parent_identity_hash, 9, 4) || '-' ||
     substr(parent_identity_hash, 13, 4) || '-' ||
     substr(parent_identity_hash, 17, 4) || '-' ||
     substr(parent_identity_hash, 21, 12))::uuid AS parent_instance_id,
    row_number() OVER (
        PARTITION BY legacy_parent_group_key
        ORDER BY display_order, source_id) AS legacy_child_order
FROM identified;

CREATE TEMP TABLE checklist_legacy_parent_stage ON COMMIT DROP AS
SELECT
    legacy_parent_group_key,
    parent_instance_id,
    template_lineage_id,
    template_version_id,
    owner_user_id,
    care_group_id,
    care_context_type,
    care_context_id,
    origin,
    CASE
        WHEN bool_and(upper(legacy_status) = 'CANCELLED') THEN 'CANCELLED'
        WHEN bool_and(upper(legacy_status) IN ('COMPLETED','DONE','SKIPPED')) THEN 'COMPLETED'
        WHEN bool_or(upper(legacy_status) IN ('IN_PROGRESS','COMPLETED','DONE','SKIPPED')) THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END AS status,
    CASE
        WHEN bool_and(upper(legacy_status) IN ('COMPLETED','DONE','SKIPPED'))
            THEN max(COALESCE(completed_at, updated_at, created_at))
    END AS completed_at,
    CASE
        WHEN bool_and(upper(legacy_status) = 'CANCELLED')
            THEN max(COALESCE(updated_at, created_at))
    END AS cancelled_at,
    min(created_at) AS created_at,
    max(updated_at) AS updated_at
FROM checklist_legacy_backfill_rows
GROUP BY
    legacy_parent_group_key,
    parent_instance_id,
    template_lineage_id,
    template_version_id,
    owner_user_id,
    care_group_id,
    care_context_type,
    care_context_id,
    origin;

-- A deterministic parent id that already names a different payload is drift.
INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT row.source_id, 'LEGACY_PARENT_PAYLOAD_DRIFT'
FROM checklist_legacy_backfill_rows row
JOIN checklist_legacy_parent_stage proposed
  ON proposed.legacy_parent_group_key = row.legacy_parent_group_key
JOIN public.checklist_instances existing
  ON existing.checklist_instance_id = proposed.parent_instance_id
WHERE existing.distribution_key IS DISTINCT FROM proposed.legacy_parent_group_key
   OR existing.template_lineage_id IS DISTINCT FROM proposed.template_lineage_id
   OR existing.template_version_id IS DISTINCT FROM proposed.template_version_id
   OR existing.recipient_user_id IS DISTINCT FROM proposed.owner_user_id
   OR existing.recipient_role IS DISTINCT FROM 'MOTHER'
   OR existing.care_group_id IS DISTINCT FROM proposed.care_group_id
   OR existing.care_context_type IS DISTINCT FROM proposed.care_context_type
   OR existing.care_context_id IS DISTINCT FROM proposed.care_context_id
   OR existing.context_owner_user_id IS DISTINCT FROM proposed.owner_user_id
   OR existing.origin IS DISTINCT FROM proposed.origin;

-- A runtime key already owned by another parent id is a collision. Never silently
-- reuse it because child identity is derived from the parent id.
INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT row.source_id, 'LEGACY_DISTRIBUTION_KEY_COLLISION'
FROM checklist_legacy_backfill_rows row
JOIN checklist_legacy_parent_stage proposed
  ON proposed.legacy_parent_group_key = row.legacy_parent_group_key
JOIN public.checklist_instances existing
  ON existing.distribution_key = proposed.legacy_parent_group_key
WHERE existing.checklist_instance_id <> proposed.parent_instance_id
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_legacy_quarantine_results quarantined
      WHERE quarantined.source_id = row.source_id
        AND quarantined.reason_code = 'LEGACY_DISTRIBUTION_KEY_COLLISION');

INSERT INTO public.checklist_instances
    (checklist_instance_id, distribution_key, key_version,
     template_lineage_id, template_version_id,
     recipient_user_id, recipient_role, care_group_id,
     care_context_type, care_context_id, context_owner_user_id,
     origin, window_start, window_end, status,
     completed_at, cancelled_at, cancellation_reason_code,
     created_at, updated_at)
SELECT
    proposed.parent_instance_id,
    proposed.legacy_parent_group_key,
    'v1',
    proposed.template_lineage_id,
    proposed.template_version_id,
    proposed.owner_user_id,
    'MOTHER',
    proposed.care_group_id,
    proposed.care_context_type,
    proposed.care_context_id,
    proposed.owner_user_id,
    proposed.origin,
    NULL,
    NULL,
    proposed.status,
    proposed.completed_at,
    proposed.cancelled_at,
    CASE WHEN proposed.status = 'CANCELLED' THEN 'LEGACY_CANCELLED' END,
    proposed.created_at,
    proposed.updated_at
FROM checklist_legacy_parent_stage proposed
WHERE NOT EXISTS (
        SELECT 1
        FROM checklist_legacy_backfill_rows row
        JOIN checklist_legacy_quarantine_results quarantined
          ON quarantined.source_id = row.source_id
        WHERE row.legacy_parent_group_key = proposed.legacy_parent_group_key
    )
  AND NOT EXISTS (
        SELECT 1
        FROM public.checklist_instances existing
        WHERE existing.checklist_instance_id = proposed.parent_instance_id
    )
  AND NOT EXISTS (
        SELECT 1
        FROM public.checklist_instances existing
        WHERE existing.distribution_key = proposed.legacy_parent_group_key
    );

CREATE TEMP TABLE checklist_legacy_child_stage ON COMMIT DROP AS
SELECT
    row.*,
    CASE
        WHEN row.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
            row.parent_instance_id::text,
            row.template_item_version_id::text)
        ELSE pg_temp.checklist_v1_key(
            row.parent_instance_id::text,
            'USER_CREATED',
            row.source_id::text)
    END AS legacy_task_key
FROM checklist_legacy_backfill_rows row
WHERE NOT EXISTS (
    SELECT 1
    FROM checklist_legacy_quarantine_results quarantined
    WHERE quarantined.source_id = row.source_id
);

-- Duplicate runtime child identities inside one parent are key collisions, even
-- when the source primary keys differ.
INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT child.source_id, 'LEGACY_TASK_KEY_COLLISION'
FROM checklist_legacy_child_stage child
JOIN (
    SELECT legacy_task_key
    FROM checklist_legacy_child_stage
    GROUP BY legacy_task_key
    HAVING count(*) > 1
) duplicate_key ON duplicate_key.legacy_task_key = child.legacy_task_key;

INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT child.source_id, 'LEGACY_TASK_PAYLOAD_DRIFT'
FROM checklist_legacy_child_stage child
JOIN public.checklist_task_instances existing
  ON existing.checklist_task_instance_id = child.source_id
WHERE existing.checklist_instance_id IS DISTINCT FROM child.parent_instance_id
   OR existing.template_version_id IS DISTINCT FROM child.template_version_id
   OR existing.template_item_version_id IS DISTINCT FROM child.template_item_version_id
   OR existing.task_key IS DISTINCT FROM child.legacy_task_key
   OR existing.title_snapshot IS DISTINCT FROM child.title
   OR existing.display_order IS DISTINCT FROM (child.legacy_child_order - 1)::integer
   OR existing.target_subject IS DISTINCT FROM child.target_subject
   OR existing.due_at IS DISTINCT FROM child.due_at;

INSERT INTO checklist_legacy_quarantine_results (source_id, reason_code)
SELECT child.source_id, 'LEGACY_TASK_KEY_COLLISION'
FROM checklist_legacy_child_stage child
JOIN public.checklist_task_instances existing
  ON existing.task_key = child.legacy_task_key
WHERE existing.checklist_task_instance_id <> child.source_id
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_legacy_quarantine_results quarantined
      WHERE quarantined.source_id = child.source_id
        AND quarantined.reason_code = 'LEGACY_TASK_KEY_COLLISION');

INSERT INTO public.checklist_task_instances
    (checklist_task_instance_id, checklist_instance_id,
     template_version_id, template_item_version_id,
     task_key, key_version, title_snapshot, display_order,
     is_required, target_subject, due_at, status,
     completed_at, skipped_at, cancelled_at, action_reason_code,
     created_at, updated_at)
SELECT
    child.source_id,
    child.parent_instance_id,
    child.template_version_id,
    child.template_item_version_id,
    child.legacy_task_key,
    'v1',
    child.title,
    (child.legacy_child_order - 1)::integer,
    false,
    child.target_subject,
    child.due_at,
    CASE
        WHEN upper(child.legacy_status) IN ('COMPLETED','DONE') THEN 'COMPLETED'
        WHEN upper(child.legacy_status) = 'SKIPPED' THEN 'SKIPPED'
        WHEN upper(child.legacy_status) = 'CANCELLED' THEN 'CANCELLED'
        WHEN upper(child.legacy_status) = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END,
    CASE WHEN upper(child.legacy_status) IN ('COMPLETED','DONE')
        THEN COALESCE(child.completed_at, child.updated_at, child.created_at) END,
    CASE WHEN upper(child.legacy_status) = 'SKIPPED'
        THEN COALESCE(child.completed_at, child.updated_at, child.created_at) END,
    CASE WHEN upper(child.legacy_status) = 'CANCELLED'
        THEN COALESCE(child.updated_at, child.created_at) END,
    CASE
        WHEN upper(child.legacy_status) = 'SKIPPED' THEN 'LEGACY_SKIPPED'
        WHEN upper(child.legacy_status) = 'CANCELLED' THEN 'LEGACY_CANCELLED'
    END,
    child.created_at,
    child.updated_at
FROM checklist_legacy_child_stage child
WHERE NOT EXISTS (
        SELECT 1
        FROM checklist_legacy_quarantine_results quarantined
        WHERE quarantined.source_id = child.source_id
    )
  AND NOT EXISTS (
        SELECT 1
        FROM public.checklist_task_instances existing
        WHERE existing.checklist_task_instance_id = child.source_id
    )
  AND NOT EXISTS (
        SELECT 1
        FROM public.checklist_task_instances existing
        WHERE existing.task_key = child.legacy_task_key
    );

UPDATE checklist_legacy_quarantine_results
SET payload_hash = encode(sha256(redacted_payload), 'hex')
WHERE payload_hash IS NULL;

-- Persist quarantine and its typed audit record in one data-modifying statement so
-- the exact generated correlation id is shared by both rows. The audit payload is
-- controlled metadata only; legacy title/body text is never copied.
WITH inserted_quarantine AS (
    INSERT INTO public.checklist_migration_quarantine
        (source_table, source_id, reason_code, payload_ciphertext, payload_hash,
         encryption_key_version, correlation_id, retain_until)
    SELECT
        'preparation_checklist_items',
        result.source_id,
        result.reason_code,
        result.redacted_payload,
        result.payload_hash,
        'REDACTED_NO_PAYLOAD_V1',
        result.correlation_id,
        now() + interval '7 years'
    FROM checklist_legacy_quarantine_results result
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.checklist_migration_quarantine existing
        WHERE existing.source_table = 'preparation_checklist_items'
          AND existing.source_id = result.source_id
          AND existing.reason_code = result.reason_code
    )
    RETURNING source_id, reason_code, correlation_id
)
INSERT INTO public.audit_events
    (actor_user_id, event_category, subject_reference_id,
     resource_type, resource_id, purpose, decision,
     occurred_at, created_at, event_origin, payload,
     correlation_id, severity, status,
     actor_type, actor_service, reason_code)
SELECT
    NULL,
    'CHECKLIST_MIGRATION_QUARANTINED',
    inserted.source_id,
    'LEGACY_CHECKLIST_ITEM',
    inserted.source_id,
    'LEGACY_CHECKLIST_V2_BACKFILL',
    'QUARANTINED',
    now(),
    now(),
    'CHECKLIST_MIGRATION',
    jsonb_build_object(
        'sourceTable', 'preparation_checklist_items',
        'sourceId', inserted.source_id,
        'reasonCode', inserted.reason_code,
        'metadata', 'REDACTED'),
    inserted.correlation_id,
    'HIGH',
    'OPEN',
    'SERVICE',
    'CHECKLIST_LEGACY_BACKFILL',
    inserted.reason_code
FROM inserted_quarantine inserted;
-- END CANONICAL SOURCE: V20260729070000__backfill_legacy_checklist_v2.sql

-- BEGIN CANONICAL SOURCE: V20260729080000__support_reminder_occurrence_command_identity.sql
-- Preserve a reminder occurrence as the idempotency task identity while retaining
-- the canonical reminder definition needed for database-level target validation.
ALTER TABLE public.checklist_action_commands
    ADD COLUMN reminder_definition_id uuid;

UPDATE public.checklist_action_commands command
SET reminder_definition_id = command.task_id
WHERE command.task_kind = 'REMINDER'
  AND command.reminder_definition_id IS NULL
  AND EXISTS (
      SELECT 1
      FROM public.care_tasks task
      WHERE task.task_id = command.task_id
        AND task.task_type = 'SCHEDULED_REMINDER'
  );

ALTER TABLE public.checklist_action_commands
    ADD CONSTRAINT checklist_action_commands_reminder_definition_fk
        FOREIGN KEY (reminder_definition_id)
        REFERENCES public.care_tasks(task_id) ON DELETE RESTRICT,
    ADD CONSTRAINT checklist_action_commands_reminder_definition_ck CHECK (
        (task_kind = 'REMINDER' AND reminder_definition_id IS NOT NULL)
        OR (task_kind <> 'REMINDER' AND reminder_definition_id IS NULL)
    );

CREATE OR REPLACE FUNCTION public.checklist_validate_action_command_target()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.task_kind = 'CHECKLIST' THEN
        PERFORM 1 FROM public.checklist_task_instances task
        WHERE task.checklist_task_instance_id = NEW.task_id;
    ELSIF NEW.task_kind = 'REMINDER' THEN
        PERFORM 1 FROM public.care_tasks task
        WHERE task.task_id = COALESCE(NEW.reminder_definition_id, NEW.task_id)
          AND task.task_type = 'SCHEDULED_REMINDER';
    ELSE
        PERFORM 1 FROM public.care_tasks task WHERE task.task_id = NEW.task_id;
    END IF;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_ACTION_TARGET_NOT_FOUND';
    END IF;
    RETURN NEW;
END $$;
-- END CANONICAL SOURCE: V20260729080000__support_reminder_occurrence_command_identity.sql

-- BEGIN CANONICAL SOURCE: V20260729090000__harden_checklist_reconciliation_lifecycle.sql
-- Reconciliation lifecycle hardening. This feature remains disabled until the migration chain completes.
ALTER TABLE public.checklist_reconciliation_runs
    DROP CONSTRAINT checklist_reconciliation_runs_trigger_ck,
    ADD CONSTRAINT checklist_reconciliation_runs_trigger_ck
        CHECK (trigger_type IN ('EVENT','HOURLY','STARTUP','MANUAL','REPLAY'));

ALTER TABLE public.checklist_reconciliation_candidates
    ADD COLUMN template_version_id uuid,
    ADD COLUMN source_correlation_id uuid;

-- Earlier phase candidates predate source/template identity. The run correlation is
-- recoverable; template identity is intentionally left NULL instead of fabricated.
UPDATE public.checklist_reconciliation_candidates AS candidate
SET source_correlation_id = run.correlation_id
FROM public.checklist_reconciliation_runs AS run
WHERE run.reconciliation_run_id = candidate.reconciliation_run_id
  AND candidate.source_correlation_id IS NULL;

ALTER TABLE public.checklist_reconciliation_candidates
    DROP CONSTRAINT checklist_reconciliation_candidates_outcome_ck,
    ADD CONSTRAINT checklist_reconciliation_candidates_outcome_ck CHECK
        (outcome IN ('PENDING','CREATED','EXISTING','CANCELLED','INELIGIBLE','FAILED','QUARANTINED')),
    DROP CONSTRAINT checklist_reconciliation_candidates_identity_uk,
    ADD CONSTRAINT checklist_reconciliation_candidates_identity_uk UNIQUE NULLS NOT DISTINCT
        (reconciliation_run_id, template_version_id, source_correlation_id,
         recipient_user_id, care_group_id, care_context_type, care_context_id, window_start, window_end);
-- END CANONICAL SOURCE: V20260729090000__harden_checklist_reconciliation_lifecycle.sql

-- BEGIN CANONICAL SOURCE: V20260729100000__complete_checklist_operations_and_replay_hardening.sql
-- Make the reconciliation operations authority persistable.
ALTER TABLE public.users
    DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE public.users
    ADD CONSTRAINT users_role_check CHECK (
        role IS NULL OR role IN (
            'MOTHER', 'FAMILY', 'EXPERT', 'MODERATOR',
            'CONTENT_ADMIN', 'SYSTEM_ADMIN', 'OPERATIONS', 'PARTNER'
        )
    );
-- END CANONICAL SOURCE: V20260729100000__complete_checklist_operations_and_replay_hardening.sql

-- BEGIN CANONICAL SOURCE: V20260729110000__avoid_noop_template_item_authority_resync.sql
-- Hibernate includes relationship columns in ordinary template updates even when
-- their values are unchanged. Avoid rewriting the immutable version-item authority
-- rows for those no-op relationship updates (for example, approval metadata changes).
CREATE OR REPLACE FUNCTION public.checklist_sync_template_version_item()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM public.checklist_template_version_items
        WHERE template_item_version_id = OLD.template_id OR template_root_id = OLD.template_id;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE'
       AND NEW.entry_type IS NOT DISTINCT FROM OLD.entry_type
       AND NEW.parent_template_id IS NOT DISTINCT FROM OLD.parent_template_id
       AND NEW.template_version_id IS NOT DISTINCT FROM OLD.template_version_id THEN
        RETURN NEW;
    END IF;

    DELETE FROM public.checklist_template_version_items
    WHERE template_item_version_id = NEW.template_id OR template_root_id = NEW.template_id;

    IF NEW.entry_type = 'CHECKLIST_ENTRY' AND NEW.parent_template_id IS NOT NULL THEN
        INSERT INTO public.checklist_template_version_items
            (template_version_id, template_root_id, template_item_version_id)
        SELECT root.template_version_id, root.template_id, NEW.template_id
        FROM public.care_item_templates root
        WHERE root.template_id = NEW.parent_template_id
          AND root.entry_type = 'TEMPLATE_ROOT'
          AND root.template_version_id IS NOT NULL
        ON CONFLICT DO NOTHING;
    ELSIF NEW.entry_type = 'TEMPLATE_ROOT' AND NEW.template_version_id IS NOT NULL THEN
        INSERT INTO public.checklist_template_version_items
            (template_version_id, template_root_id, template_item_version_id)
        SELECT NEW.template_version_id, NEW.template_id, item.template_id
        FROM public.care_item_templates item
        WHERE item.parent_template_id = NEW.template_id
          AND item.entry_type = 'CHECKLIST_ENTRY'
        ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
END $$;
-- END CANONICAL SOURCE: V20260729110000__avoid_noop_template_item_authority_resync.sql

-- BEGIN CANONICAL SOURCE: V20260729120000__sync_reviewed_care_group_contexts.sql
-- Explicit care-group links are canonical only when the linked context belongs to
-- the same owner. Materialize those verified links so checklist instances can use
-- the composite context-authority foreign key without trusting application input.
CREATE OR REPLACE FUNCTION public.checklist_sync_reviewed_care_group_contexts()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.linked_journey_id IS NOT NULL THEN
        INSERT INTO public.checklist_care_group_contexts
            (care_group_id, owner_user_id, care_context_type, care_context_id,
             review_status, distribution_blocked, reviewed_at, reviewed_by)
        SELECT NEW.care_group_id, NEW.owner_user_id, 'JOURNEY', NEW.linked_journey_id,
               'REVIEWED', false, now(), NEW.owner_user_id
        FROM public.checklist_context_authorities authority
        WHERE authority.care_context_type = 'JOURNEY'
          AND authority.care_context_id = NEW.linked_journey_id
          AND authority.owner_user_id = NEW.owner_user_id
        ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;
    END IF;

    IF NEW.linked_baby_profile_id IS NOT NULL THEN
        INSERT INTO public.checklist_care_group_contexts
            (care_group_id, owner_user_id, care_context_type, care_context_id,
             review_status, distribution_blocked, reviewed_at, reviewed_by)
        SELECT NEW.care_group_id, NEW.owner_user_id, 'BABY', NEW.linked_baby_profile_id,
               'REVIEWED', false, now(), NEW.owner_user_id
        FROM public.checklist_context_authorities authority
        WHERE authority.care_context_type = 'BABY'
          AND authority.care_context_id = NEW.linked_baby_profile_id
          AND authority.owner_user_id = NEW.owner_user_id
        ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_sync_reviewed_care_group_contexts_trg
AFTER INSERT OR UPDATE OF owner_user_id, linked_journey_id, linked_baby_profile_id
ON public.care_groups
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_reviewed_care_group_contexts();

INSERT INTO public.checklist_care_group_contexts
    (care_group_id, owner_user_id, care_context_type, care_context_id,
     review_status, distribution_blocked, reviewed_at, reviewed_by)
SELECT group_row.care_group_id, group_row.owner_user_id, 'JOURNEY', group_row.linked_journey_id,
       'REVIEWED', false, now(), group_row.owner_user_id
FROM public.care_groups group_row
JOIN public.checklist_context_authorities authority
  ON authority.care_context_type = 'JOURNEY'
 AND authority.care_context_id = group_row.linked_journey_id
 AND authority.owner_user_id = group_row.owner_user_id
WHERE group_row.linked_journey_id IS NOT NULL
ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;

INSERT INTO public.checklist_care_group_contexts
    (care_group_id, owner_user_id, care_context_type, care_context_id,
     review_status, distribution_blocked, reviewed_at, reviewed_by)
SELECT group_row.care_group_id, group_row.owner_user_id, 'BABY', group_row.linked_baby_profile_id,
       'REVIEWED', false, now(), group_row.owner_user_id
FROM public.care_groups group_row
JOIN public.checklist_context_authorities authority
  ON authority.care_context_type = 'BABY'
 AND authority.care_context_id = group_row.linked_baby_profile_id
 AND authority.owner_user_id = group_row.owner_user_id
WHERE group_row.linked_baby_profile_id IS NOT NULL
ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;
-- END CANONICAL SOURCE: V20260729120000__sync_reviewed_care_group_contexts.sql

-- BEGIN CANONICAL SOURCE: V20260729130000__quarantine_invalid_care_group_context_links.sql
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
-- END CANONICAL SOURCE: V20260729130000__quarantine_invalid_care_group_context_links.sql

-- BEGIN CANONICAL SOURCE: V20260729140000__repair_legacy_checklist_occurrences.sql
-- CHK-022 roll-forward repair. V20260729070000 is intentionally immutable.
-- Rebuild legacy V2 projections from the retained source rows using a real
-- occurrence token, preserve progressed task state, and quarantine only genuine
-- same-item/same-occurrence collisions.

CREATE TEMP TABLE checklist_occurrence_repair_bootstrap (marker boolean) ON COMMIT DROP;

CREATE OR REPLACE FUNCTION pg_temp.checklist_v1_key(VARIADIC tokens text[])
RETURNS text
LANGUAGE sql
IMMUTABLE
STRICT
AS $$
    SELECT encode(sha256(convert_to(
        'v1' || COALESCE(string_agg(
            octet_length(convert_to(token, 'UTF8'))::text || ':' || token,
            '' ORDER BY ordinal), ''),
        'UTF8')), 'hex')
    FROM unnest(tokens) WITH ORDINALITY AS ordered_tokens(token, ordinal)
$$;

DO $$
BEGIN
    IF pg_temp.checklist_v1_key(
            '11111111-1111-1111-1111-111111111111',
            '22222222-2222-2222-2222-222222222222',
            'MOTHER',
            '33333333-3333-3333-3333-333333333333',
            'JOURNEY',
            '44444444-4444-4444-4444-444444444444',
            'NONE',
            'NONE') <>
            'fad7bba6cefeb717acaf887b59410cef7184b88706e67cdf828be0240678369d' THEN
        RAISE EXCEPTION 'CHECKLIST_V1_KEY_GOLDEN_VECTOR_MISMATCH';
    END IF;
END $$;

CREATE TEMP TABLE checklist_occurrence_source_stage ON COMMIT DROP AS
SELECT
    legacy.checklist_item_id AS source_id,
    legacy.owner_user_id,
    legacy.title,
    COALESCE(legacy.display_order, 0) AS source_display_order,
    legacy.status AS legacy_status,
    legacy.due_at,
    legacy.completed_at,
    legacy.created_at,
    legacy.updated_at,
    legacy.template_entry_id,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN 'BABY'
        WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN 'JOURNEY'
    END AS care_context_type,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL THEN legacy.baby_id
        WHEN legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL THEN legacy.mother_journey_id
    END AS care_context_id,
    context_match.care_group_id,
    COALESCE(context_match.match_count, 0) AS context_match_count,
    root.template_lineage_id,
    root.template_version_id,
    item.template_id AS template_item_version_id,
    COALESCE(item.target_subject,
        CASE WHEN legacy.baby_id IS NOT NULL THEN 'BABY' ELSE 'MOTHER' END) AS target_subject,
    CASE WHEN legacy.template_entry_id IS NULL THEN 'USER_CREATED' ELSE 'SYSTEM_TEMPLATE' END AS origin,
    CASE
        WHEN legacy.template_entry_id IS NOT NULL THEN
            (legacy.created_at
                AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
    END AS occurrence_date,
    CASE
        WHEN legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NOT NULL
            THEN 'AMBIGUOUS_LEGACY_CONTEXT'
        WHEN legacy.baby_id IS NULL AND legacy.mother_journey_id IS NULL
            THEN 'UNKNOWN_LEGACY_CONTEXT'
        WHEN COALESCE(context_match.match_count, 0) = 0
            THEN 'CONTEXT_OWNER_MISMATCH'
        WHEN context_match.match_count > 1
            THEN 'MULTIPLE_CONTEXT_BINDINGS'
        WHEN legacy.template_entry_id IS NOT NULL AND
             (item.template_id IS NULL OR root.template_version_id IS NULL
              OR root.template_lineage_id IS NULL)
            THEN 'UNKNOWN_TEMPLATE_ROOT'
        WHEN legacy.title IS NULL OR btrim(legacy.title) = ''
            THEN 'INVALID_LEGACY_TITLE'
        WHEN legacy.status IS NULL OR upper(legacy.status) NOT IN
             ('OPEN','PENDING','IN_PROGRESS','COMPLETED','DONE','SKIPPED','CANCELLED')
            THEN 'UNKNOWN_LEGACY_STATUS'
        WHEN legacy.completed_at IS NOT NULL
             AND upper(legacy.status) IN ('OPEN','PENDING','IN_PROGRESS')
            THEN 'CONTRADICTORY_LEGACY_TIMESTAMPS'
    END AS validation_reason
FROM public.preparation_checklist_items legacy
LEFT JOIN public.care_item_templates item
  ON item.template_id = legacy.template_entry_id
 AND item.entry_type = 'CHECKLIST_ENTRY'
LEFT JOIN public.care_item_templates root
  ON root.template_id = item.parent_template_id
 AND root.entry_type = 'TEMPLATE_ROOT'
LEFT JOIN LATERAL (
    SELECT candidate.care_group_id, count(*) OVER () AS match_count
    FROM public.checklist_care_group_contexts candidate
    WHERE candidate.owner_user_id = legacy.owner_user_id
      AND candidate.review_status = 'REVIEWED'
      AND candidate.distribution_blocked = false
      AND (
          (legacy.baby_id IS NOT NULL AND legacy.mother_journey_id IS NULL
              AND candidate.care_context_type = 'BABY'
              AND candidate.care_context_id = legacy.baby_id)
          OR
          (legacy.mother_journey_id IS NOT NULL AND legacy.baby_id IS NULL
              AND candidate.care_context_type = 'JOURNEY'
              AND candidate.care_context_id = legacy.mother_journey_id)
      )
    ORDER BY candidate.care_group_id
    LIMIT 1
) context_match ON true;

CREATE TEMP TABLE checklist_occurrence_repair_quarantine (
    source_id uuid NOT NULL,
    reason_code varchar(80) NOT NULL,
    correlation_id uuid NOT NULL DEFAULT gen_random_uuid(),
    redacted_payload bytea NOT NULL DEFAULT sha256(convert_to(
        gen_random_uuid()::text || clock_timestamp()::text || random()::text,
        'UTF8')),
    CONSTRAINT checklist_occurrence_repair_quarantine_uk UNIQUE (source_id, reason_code)
) ON COMMIT DROP;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT source.source_id, source.validation_reason
FROM checklist_occurrence_source_stage source
WHERE source.validation_reason IS NOT NULL;

-- A collision is scoped to one recipient/context/version/date and one versioned
-- item. Different dates are different occurrences and must never collide.
INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT source.source_id, 'LEGACY_OCCURRENCE_COLLISION'
FROM checklist_occurrence_source_stage source
JOIN (
    SELECT owner_user_id, care_group_id, care_context_type, care_context_id,
           template_version_id, occurrence_date, template_item_version_id
    FROM checklist_occurrence_source_stage
    WHERE validation_reason IS NULL
      AND origin = 'SYSTEM_TEMPLATE'
    GROUP BY owner_user_id, care_group_id, care_context_type, care_context_id,
             template_version_id, occurrence_date, template_item_version_id
    HAVING count(*) > 1
) collision
  ON collision.owner_user_id = source.owner_user_id
 AND collision.care_group_id = source.care_group_id
 AND collision.care_context_type = source.care_context_type
 AND collision.care_context_id = source.care_context_id
 AND collision.template_version_id = source.template_version_id
 AND collision.occurrence_date = source.occurrence_date
 AND collision.template_item_version_id = source.template_item_version_id;

CREATE TEMP TABLE checklist_occurrence_repair_rows ON COMMIT DROP AS
WITH eligible AS (
    SELECT source.*
    FROM checklist_occurrence_source_stage source
    WHERE source.validation_reason IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM checklist_occurrence_repair_quarantine quarantined
          WHERE quarantined.source_id = source.source_id
      )
), keyed AS (
    SELECT
        eligible.*,
        CASE
            WHEN eligible.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
                eligible.template_version_id::text,
                eligible.owner_user_id::text,
                'MOTHER',
                eligible.care_group_id::text,
                eligible.care_context_type,
                eligible.care_context_id::text,
                eligible.occurrence_date::text,
                eligible.occurrence_date::text)
            ELSE pg_temp.checklist_v1_key(
                'LEGACY_USER_CREATED_PARENT',
                eligible.owner_user_id::text,
                'MOTHER',
                eligible.care_group_id::text,
                eligible.care_context_type,
                eligible.care_context_id::text,
                eligible.source_id::text)
        END AS repaired_parent_key,
        CASE
            WHEN eligible.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
                eligible.template_version_id::text,
                eligible.owner_user_id::text,
                'MOTHER',
                eligible.care_group_id::text,
                eligible.care_context_type,
                eligible.care_context_id::text,
                'NONE',
                'NONE')
            ELSE pg_temp.checklist_v1_key(
                eligible.owner_user_id::text,
                'MOTHER',
                eligible.care_group_id::text,
                eligible.care_context_type,
                eligible.care_context_id::text,
                '<ABSENT>',
                '<ABSENT>')
        END AS old_parent_key
    FROM eligible
), identified AS (
    SELECT
        keyed.*,
        pg_temp.checklist_v1_key('LEGACY_PARENT_ID', repaired_parent_key)
            AS repaired_parent_identity_hash,
        pg_temp.checklist_v1_key('LEGACY_PARENT_ID', old_parent_key)
            AS old_parent_identity_hash
    FROM keyed
), with_ids AS (
    SELECT
        identified.*,
        (substr(repaired_parent_identity_hash, 1, 8) || '-' ||
         substr(repaired_parent_identity_hash, 9, 4) || '-' ||
         substr(repaired_parent_identity_hash, 13, 4) || '-' ||
         substr(repaired_parent_identity_hash, 17, 4) || '-' ||
         substr(repaired_parent_identity_hash, 21, 12))::uuid AS repaired_parent_id,
        (substr(old_parent_identity_hash, 1, 8) || '-' ||
         substr(old_parent_identity_hash, 9, 4) || '-' ||
         substr(old_parent_identity_hash, 13, 4) || '-' ||
         substr(old_parent_identity_hash, 17, 4) || '-' ||
         substr(old_parent_identity_hash, 21, 12))::uuid AS old_parent_id
    FROM identified
)
SELECT
    with_ids.*,
    row_number() OVER (
        PARTITION BY repaired_parent_id
        ORDER BY source_display_order, source_id) - 1 AS repaired_display_order
FROM with_ids;

CREATE UNIQUE INDEX checklist_occurrence_repair_rows_source_ix
    ON checklist_occurrence_repair_rows(source_id);
CREATE INDEX checklist_occurrence_repair_rows_parent_ix
    ON checklist_occurrence_repair_rows(repaired_parent_id);
CREATE INDEX checklist_occurrence_repair_rows_old_parent_source_ix
    ON checklist_occurrence_repair_rows(old_parent_id, source_id);
ANALYZE checklist_occurrence_repair_rows;

CREATE TEMP TABLE checklist_occurrence_repair_parents ON COMMIT DROP AS
SELECT
    repaired_parent_id,
    repaired_parent_key,
    template_lineage_id,
    template_version_id,
    owner_user_id,
    care_group_id,
    care_context_type,
    care_context_id,
    origin,
    occurrence_date,
    CASE
        WHEN bool_and(upper(legacy_status) = 'CANCELLED') THEN 'CANCELLED'
        WHEN bool_and(upper(legacy_status) IN
                ('COMPLETED','DONE','SKIPPED','CANCELLED')) THEN 'COMPLETED'
        WHEN bool_or(upper(legacy_status) IN
                ('IN_PROGRESS','COMPLETED','DONE','SKIPPED','CANCELLED'))
            THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END AS status,
    CASE
        WHEN NOT bool_and(upper(legacy_status) = 'CANCELLED')
             AND bool_and(upper(legacy_status) IN
                 ('COMPLETED','DONE','SKIPPED','CANCELLED'))
            THEN max(COALESCE(completed_at, updated_at, created_at))
    END AS completed_at,
    CASE
        WHEN bool_and(upper(legacy_status) = 'CANCELLED')
            THEN max(COALESCE(updated_at, created_at))
    END AS cancelled_at,
    min(created_at) AS created_at,
    max(updated_at) AS updated_at
FROM checklist_occurrence_repair_rows
GROUP BY repaired_parent_id, repaired_parent_key, template_lineage_id,
         template_version_id, owner_user_id, care_group_id, care_context_type,
         care_context_id, origin, occurrence_date;

CREATE UNIQUE INDEX checklist_occurrence_repair_parents_id_ix
    ON checklist_occurrence_repair_parents(repaired_parent_id);
CREATE UNIQUE INDEX checklist_occurrence_repair_parents_key_ix
    ON checklist_occurrence_repair_parents(repaired_parent_key);
ANALYZE checklist_occurrence_repair_parents;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT row.source_id, 'LEGACY_PARENT_PAYLOAD_DRIFT'
FROM checklist_occurrence_repair_rows row
JOIN checklist_occurrence_repair_parents proposed
  ON proposed.repaired_parent_id = row.repaired_parent_id
JOIN public.checklist_instances existing
  ON existing.checklist_instance_id = proposed.repaired_parent_id
WHERE existing.distribution_key IS DISTINCT FROM proposed.repaired_parent_key
   OR existing.key_version IS DISTINCT FROM 'v1'
   OR existing.template_lineage_id IS DISTINCT FROM proposed.template_lineage_id
   OR existing.template_version_id IS DISTINCT FROM proposed.template_version_id
   OR existing.recipient_user_id IS DISTINCT FROM proposed.owner_user_id
   OR existing.recipient_role IS DISTINCT FROM 'MOTHER'
   OR existing.care_group_id IS DISTINCT FROM proposed.care_group_id
   OR existing.care_context_type IS DISTINCT FROM proposed.care_context_type
   OR existing.care_context_id IS DISTINCT FROM proposed.care_context_id
   OR existing.context_owner_user_id IS DISTINCT FROM proposed.owner_user_id
   OR existing.origin IS DISTINCT FROM proposed.origin
   OR existing.window_start IS DISTINCT FROM CASE
       WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
   OR existing.window_end IS DISTINCT FROM CASE
       WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
ON CONFLICT DO NOTHING;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT row.source_id, 'LEGACY_DISTRIBUTION_KEY_COLLISION'
FROM checklist_occurrence_repair_rows row
JOIN public.checklist_instances existing
  ON existing.distribution_key = row.repaired_parent_key
WHERE existing.checklist_instance_id <> row.repaired_parent_id
ON CONFLICT DO NOTHING;

INSERT INTO public.checklist_instances
    (checklist_instance_id, distribution_key, key_version,
     template_lineage_id, template_version_id,
     recipient_user_id, recipient_role, care_group_id,
     care_context_type, care_context_id, context_owner_user_id,
     origin, window_start, window_end, status,
     completed_at, cancelled_at, cancellation_reason_code,
     created_at, updated_at)
SELECT
    proposed.repaired_parent_id,
    proposed.repaired_parent_key,
    'v1',
    proposed.template_lineage_id,
    proposed.template_version_id,
    proposed.owner_user_id,
    'MOTHER',
    proposed.care_group_id,
    proposed.care_context_type,
    proposed.care_context_id,
    proposed.owner_user_id,
    proposed.origin,
    CASE WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END,
    CASE WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END,
    proposed.status,
    proposed.completed_at,
    proposed.cancelled_at,
    CASE WHEN proposed.status = 'CANCELLED' THEN 'LEGACY_CANCELLED' END,
    proposed.created_at,
    proposed.updated_at
FROM checklist_occurrence_repair_parents proposed
WHERE NOT EXISTS (
        SELECT 1
        FROM checklist_occurrence_repair_rows row
        JOIN checklist_occurrence_repair_quarantine quarantined
          ON quarantined.source_id = row.source_id
        WHERE row.repaired_parent_id = proposed.repaired_parent_id
    )
  AND NOT EXISTS (
        SELECT 1 FROM public.checklist_instances existing
        WHERE existing.checklist_instance_id = proposed.repaired_parent_id
    )
  AND NOT EXISTS (
        SELECT 1 FROM public.checklist_instances existing
        WHERE existing.distribution_key = proposed.repaired_parent_key
    );
CREATE TEMP TABLE checklist_occurrence_repair_tasks ON COMMIT DROP AS
SELECT
    row.*,
    CASE
        WHEN row.origin = 'SYSTEM_TEMPLATE' THEN pg_temp.checklist_v1_key(
            row.repaired_parent_id::text,
            row.template_item_version_id::text)
        ELSE pg_temp.checklist_v1_key(
            row.repaired_parent_id::text,
            'USER_CREATED',
            row.source_id::text)
    END AS repaired_task_key
FROM checklist_occurrence_repair_rows row
WHERE NOT EXISTS (
    SELECT 1
    FROM checklist_occurrence_repair_quarantine quarantined
    WHERE quarantined.source_id = row.source_id
);

CREATE UNIQUE INDEX checklist_occurrence_repair_tasks_source_ix
    ON checklist_occurrence_repair_tasks(source_id);
CREATE INDEX checklist_occurrence_repair_tasks_parent_ix
    ON checklist_occurrence_repair_tasks(repaired_parent_id);
ANALYZE checklist_occurrence_repair_tasks;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT proposed.source_id, 'LEGACY_OCCURRENCE_COLLISION'
FROM checklist_occurrence_repair_tasks proposed
JOIN public.checklist_task_instances existing
  ON existing.task_key = proposed.repaired_task_key
WHERE existing.checklist_task_instance_id <> proposed.source_id
ON CONFLICT DO NOTHING;

INSERT INTO checklist_occurrence_repair_quarantine (source_id, reason_code)
SELECT proposed.source_id, 'LEGACY_TASK_PAYLOAD_DRIFT'
FROM checklist_occurrence_repair_tasks proposed
JOIN public.checklist_task_instances existing
  ON existing.checklist_task_instance_id = proposed.source_id
WHERE existing.template_version_id IS DISTINCT FROM proposed.template_version_id
   OR existing.template_item_version_id IS DISTINCT FROM proposed.template_item_version_id
   OR existing.key_version IS DISTINCT FROM 'v1'
   OR existing.title_snapshot IS DISTINCT FROM proposed.title
   OR existing.target_subject IS DISTINCT FROM proposed.target_subject
   OR existing.due_at IS DISTINCT FROM proposed.due_at
   OR existing.is_required IS DISTINCT FROM false
ON CONFLICT DO NOTHING;

-- Re-parent existing V70000 projections without touching status or terminal/action
-- timestamps. These rows may have progressed after the original backfill.
UPDATE public.checklist_task_instances existing
SET checklist_instance_id = proposed.repaired_parent_id,
    template_version_id = proposed.template_version_id,
    template_item_version_id = proposed.template_item_version_id,
    task_key = proposed.repaired_task_key,
    display_order = proposed.repaired_display_order::integer
FROM checklist_occurrence_repair_tasks proposed
WHERE existing.checklist_task_instance_id = proposed.source_id
  AND EXISTS (
      SELECT 1 FROM public.checklist_instances parent
      WHERE parent.checklist_instance_id = proposed.repaired_parent_id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_occurrence_repair_quarantine quarantined
      WHERE quarantined.source_id = proposed.source_id
  );

INSERT INTO public.checklist_task_instances
    (checklist_task_instance_id, checklist_instance_id,
     template_version_id, template_item_version_id,
     task_key, key_version, title_snapshot, display_order,
     is_required, target_subject, due_at, status,
     completed_at, skipped_at, cancelled_at, action_reason_code,
     created_at, updated_at)
SELECT
    proposed.source_id,
    proposed.repaired_parent_id,
    proposed.template_version_id,
    proposed.template_item_version_id,
    proposed.repaired_task_key,
    'v1',
    proposed.title,
    proposed.repaired_display_order::integer,
    false,
    proposed.target_subject,
    proposed.due_at,
    CASE
        WHEN upper(proposed.legacy_status) IN ('COMPLETED','DONE') THEN 'COMPLETED'
        WHEN upper(proposed.legacy_status) = 'SKIPPED' THEN 'SKIPPED'
        WHEN upper(proposed.legacy_status) = 'CANCELLED' THEN 'CANCELLED'
        WHEN upper(proposed.legacy_status) = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END,
    CASE WHEN upper(proposed.legacy_status) IN ('COMPLETED','DONE')
        THEN COALESCE(proposed.completed_at, proposed.updated_at, proposed.created_at) END,
    CASE WHEN upper(proposed.legacy_status) = 'SKIPPED'
        THEN COALESCE(proposed.completed_at, proposed.updated_at, proposed.created_at) END,
    CASE WHEN upper(proposed.legacy_status) = 'CANCELLED'
        THEN COALESCE(proposed.updated_at, proposed.created_at) END,
    CASE
        WHEN upper(proposed.legacy_status) = 'SKIPPED' THEN 'LEGACY_SKIPPED'
        WHEN upper(proposed.legacy_status) = 'CANCELLED' THEN 'LEGACY_CANCELLED'
    END,
    proposed.created_at,
    proposed.updated_at
FROM checklist_occurrence_repair_tasks proposed
WHERE EXISTS (
        SELECT 1 FROM public.checklist_instances parent
        WHERE parent.checklist_instance_id = proposed.repaired_parent_id
    )
  AND NOT EXISTS (
        SELECT 1
        FROM checklist_occurrence_repair_quarantine quarantined
        WHERE quarantined.source_id = proposed.source_id
    )
  AND NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances existing
        WHERE existing.checklist_task_instance_id = proposed.source_id
    )
  AND NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances existing
        WHERE existing.task_key = proposed.repaired_task_key
    );

-- Parent state follows the repaired children, not stale source status. This keeps
-- completions/actions that happened after V70000 while fixing the grouping only.
WITH aggregate_state AS (
    SELECT
        parent.checklist_instance_id,
        CASE
            WHEN bool_and(task.status = 'CANCELLED') THEN 'CANCELLED'
            WHEN bool_and(task.status IN ('COMPLETED','SKIPPED','CANCELLED'))
                THEN 'COMPLETED'
            WHEN bool_or(task.status IN
                    ('IN_PROGRESS','COMPLETED','SKIPPED','CANCELLED'))
                THEN 'IN_PROGRESS'
            ELSE 'PENDING'
        END AS status,
        CASE WHEN NOT bool_and(task.status = 'CANCELLED')
                  AND bool_and(task.status IN ('COMPLETED','SKIPPED','CANCELLED'))
            THEN max(COALESCE(
                task.completed_at, task.skipped_at, task.cancelled_at, task.updated_at))
        END AS completed_at,
        CASE WHEN bool_and(task.status = 'CANCELLED')
            THEN max(COALESCE(task.cancelled_at, task.updated_at)) END AS cancelled_at
    FROM checklist_occurrence_repair_parents proposed
    JOIN public.checklist_instances parent
      ON parent.checklist_instance_id = proposed.repaired_parent_id
     AND parent.distribution_key = proposed.repaired_parent_key
     AND parent.key_version = 'v1'
     AND parent.template_lineage_id IS NOT DISTINCT FROM proposed.template_lineage_id
     AND parent.template_version_id IS NOT DISTINCT FROM proposed.template_version_id
     AND parent.recipient_user_id = proposed.owner_user_id
     AND parent.recipient_role = 'MOTHER'
     AND parent.care_group_id = proposed.care_group_id
     AND parent.care_context_type = proposed.care_context_type
     AND parent.care_context_id = proposed.care_context_id
     AND parent.context_owner_user_id = proposed.owner_user_id
     AND parent.origin = proposed.origin
     AND parent.window_start IS NOT DISTINCT FROM CASE
         WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
     AND parent.window_end IS NOT DISTINCT FROM CASE
         WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
    JOIN public.checklist_task_instances task
      ON task.checklist_instance_id = parent.checklist_instance_id
    WHERE NOT EXISTS (
        SELECT 1
        FROM checklist_occurrence_repair_rows row
        JOIN checklist_occurrence_repair_quarantine quarantined
          ON quarantined.source_id = row.source_id
        WHERE row.repaired_parent_id = proposed.repaired_parent_id
    )
    GROUP BY parent.checklist_instance_id
)
UPDATE public.checklist_instances parent
SET status = aggregate.status,
    completed_at = aggregate.completed_at,
    cancelled_at = aggregate.cancelled_at,
    cancellation_reason_code = CASE
        WHEN aggregate.status = 'CANCELLED' THEN 'LEGACY_CANCELLED'
    END
FROM aggregate_state aggregate
WHERE parent.checklist_instance_id = aggregate.checklist_instance_id;
-- Tombstone deterministic V70000 parents that became empty after re-parenting.
-- Updating instead of deleting avoids same-transaction FK scans over the old
-- task index versions and retains an explicit, audited migration boundary.
CREATE TEMP TABLE checklist_occurrence_repair_audited_old_parents ON COMMIT DROP AS
SELECT DISTINCT history.old_parent_id
FROM public.audit_events audit
JOIN checklist_occurrence_repair_rows history
  ON history.source_id = audit.checklist_task_instance_id
WHERE audit.checklist_task_instance_id IS NOT NULL
  AND history.old_parent_id IS NOT NULL;
CREATE UNIQUE INDEX checklist_occurrence_repair_audited_old_parents_ix
    ON checklist_occurrence_repair_audited_old_parents(old_parent_id);

WITH distinct_old_parent_keys AS MATERIALIZED (
    SELECT DISTINCT repaired.old_parent_id, repaired.old_parent_key,
           repaired.owner_user_id, repaired.care_group_id,
           repaired.care_context_type, repaired.care_context_id, repaired.origin
    FROM checklist_occurrence_repair_rows repaired
), empty_old_parents AS MATERIALIZED (
    SELECT repaired.*
    FROM distinct_old_parent_keys repaired
    JOIN public.checklist_instances old_parent
      ON old_parent.checklist_instance_id = repaired.old_parent_id
     AND old_parent.distribution_key = repaired.old_parent_key
     AND old_parent.recipient_user_id = repaired.owner_user_id
     AND old_parent.recipient_role = 'MOTHER'
     AND old_parent.care_group_id = repaired.care_group_id
     AND old_parent.care_context_type = repaired.care_context_type
     AND old_parent.care_context_id = repaired.care_context_id
     AND old_parent.origin = repaired.origin
     AND old_parent.status = 'PENDING'
     AND old_parent.completed_at IS NULL
     AND old_parent.cancelled_at IS NULL
    WHERE NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances child
        WHERE child.checklist_instance_id = old_parent.checklist_instance_id
    )
), tombstoned AS (
    UPDATE public.checklist_instances old_parent
    SET status = 'CANCELLED',
        cancelled_at = clock_timestamp(),
        cancellation_reason_code = 'LEGACY_OCCURRENCE_REPAIRED',
        updated_at = clock_timestamp()
    FROM empty_old_parents repaired
    WHERE old_parent.checklist_instance_id = repaired.old_parent_id
      AND old_parent.distribution_key = repaired.old_parent_key
      AND NOT EXISTS (
          SELECT 1 FROM public.audit_events audit
          WHERE audit.resource_type = 'CHECKLIST_INSTANCE'
            AND audit.resource_id = old_parent.checklist_instance_id
      )
      AND NOT EXISTS (
          SELECT 1
          FROM checklist_occurrence_repair_audited_old_parents audited
          WHERE audited.old_parent_id = old_parent.checklist_instance_id
      )
    RETURNING old_parent.checklist_instance_id, old_parent.recipient_user_id,
              old_parent.care_context_type, old_parent.care_context_id,
              old_parent.template_version_id
)
INSERT INTO public.audit_events
    (actor_user_id, event_category, subject_user_id,
     resource_type, resource_id, purpose, decision,
     occurred_at, created_at, event_origin, payload,
     correlation_id, severity, status,
     actor_type, actor_service, reason_code,
     care_context_type, care_context_id, template_version_id,
     before_payload_jsonb, after_payload_jsonb)
SELECT
    NULL,
    'CHECKLIST_CANCELLED',
    tombstoned.recipient_user_id,
    'CHECKLIST_INSTANCE',
    tombstoned.checklist_instance_id,
    'LEGACY_CHECKLIST_OCCURRENCE_REPAIR',
    'CANCELLED',
    now(),
    now(),
    'CHECKLIST_MIGRATION',
    jsonb_build_object(
        'reasonCode', 'LEGACY_OCCURRENCE_REPAIRED',
        'metadata', 'REDACTED'),
    gen_random_uuid(),
    'MEDIUM',
    'CLOSED',
    'SERVICE',
    'CHECKLIST_LEGACY_OCCURRENCE_REPAIR',
    'LEGACY_OCCURRENCE_REPAIRED',
    tombstoned.care_context_type,
    tombstoned.care_context_id,
    tombstoned.template_version_id,
    jsonb_build_object('status', 'PENDING'),
    jsonb_build_object(
        'status', 'CANCELLED',
        'reasonCode', 'LEGACY_OCCURRENCE_REPAIRED')
FROM tombstoned;

-- A repaired parent can be empty only when every candidate task was quarantined.
WITH empty_repaired_parents AS MATERIALIZED (
    SELECT proposed.repaired_parent_id
    FROM checklist_occurrence_repair_parents proposed
    JOIN public.checklist_instances parent
      ON parent.checklist_instance_id = proposed.repaired_parent_id
     AND parent.distribution_key = proposed.repaired_parent_key
     AND parent.key_version = 'v1'
     AND parent.template_lineage_id IS NOT DISTINCT FROM proposed.template_lineage_id
     AND parent.template_version_id IS NOT DISTINCT FROM proposed.template_version_id
     AND parent.recipient_user_id = proposed.owner_user_id
     AND parent.recipient_role = 'MOTHER'
     AND parent.care_group_id = proposed.care_group_id
     AND parent.care_context_type = proposed.care_context_type
     AND parent.care_context_id = proposed.care_context_id
     AND parent.context_owner_user_id = proposed.owner_user_id
     AND parent.origin = proposed.origin
     AND parent.status = 'PENDING'
     AND parent.status = proposed.status
     AND parent.window_start IS NOT DISTINCT FROM CASE
         WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
     AND parent.window_end IS NOT DISTINCT FROM CASE
         WHEN proposed.origin = 'SYSTEM_TEMPLATE' THEN proposed.occurrence_date END
     AND parent.completed_at IS NOT DISTINCT FROM proposed.completed_at
     AND parent.cancelled_at IS NOT DISTINCT FROM proposed.cancelled_at
     AND parent.cancellation_reason_code IS NOT DISTINCT FROM CASE
         WHEN proposed.status = 'CANCELLED' THEN 'LEGACY_CANCELLED' END
     AND parent.created_at = proposed.created_at
     AND parent.updated_at = proposed.updated_at
    WHERE NOT EXISTS (
        SELECT 1 FROM public.checklist_task_instances child
        WHERE child.checklist_instance_id = parent.checklist_instance_id
    )
)
DELETE FROM public.checklist_instances parent
USING empty_repaired_parents empty
WHERE parent.checklist_instance_id = empty.repaired_parent_id
  AND NOT EXISTS (
      SELECT 1 FROM public.audit_events audit
      WHERE audit.resource_type = 'CHECKLIST_INSTANCE'
        AND audit.resource_id = parent.checklist_instance_id
  );

WITH prepared AS (
    SELECT
        result.source_id,
        result.reason_code,
        result.correlation_id,
        result.redacted_payload,
        encode(sha256(result.redacted_payload), 'hex') AS payload_hash
    FROM checklist_occurrence_repair_quarantine result
), inserted_quarantine AS (
    INSERT INTO public.checklist_migration_quarantine
        (source_table, source_id, reason_code, payload_ciphertext, payload_hash,
         encryption_key_version, correlation_id, retain_until)
    SELECT
        'preparation_checklist_items',
        prepared.source_id,
        prepared.reason_code,
        prepared.redacted_payload,
        prepared.payload_hash,
        'REDACTED_NO_PAYLOAD_V1',
        prepared.correlation_id,
        now() + interval '7 years'
    FROM prepared
    WHERE NOT EXISTS (
        SELECT 1
        FROM public.checklist_migration_quarantine existing
        WHERE existing.source_table = 'preparation_checklist_items'
          AND existing.source_id = prepared.source_id
          AND existing.reason_code = prepared.reason_code
    )
    RETURNING source_id, reason_code, correlation_id
)
INSERT INTO public.audit_events
    (actor_user_id, event_category, subject_reference_id,
     resource_type, resource_id, purpose, decision,
     occurred_at, created_at, event_origin, payload,
     correlation_id, severity, status,
     actor_type, actor_service, reason_code)
SELECT
    NULL,
    'CHECKLIST_MIGRATION_QUARANTINED',
    inserted.source_id,
    'LEGACY_CHECKLIST_ITEM',
    inserted.source_id,
    'LEGACY_CHECKLIST_OCCURRENCE_REPAIR',
    'QUARANTINED',
    now(),
    now(),
    'CHECKLIST_MIGRATION',
    jsonb_build_object(
        'sourceTable', 'preparation_checklist_items',
        'sourceId', inserted.source_id,
        'reasonCode', inserted.reason_code,
        'metadata', 'REDACTED'),
    inserted.correlation_id,
    'HIGH',
    'OPEN',
    'SERVICE',
    'CHECKLIST_LEGACY_OCCURRENCE_REPAIR',
    inserted.reason_code
FROM inserted_quarantine inserted;
-- END CANONICAL SOURCE: V20260729140000__repair_legacy_checklist_occurrences.sql

-- BEGIN CANONICAL SOURCE: V20260729150000__add_checklist_operations_retention.sql
-- CHK-039: operations-only quarantine surface and audited seven-year retention.
-- Application authorization is enforced at the controller boundary. Database
-- policies expose quarantine rows only to the migration/application role.

ALTER TABLE public.audit_events
    ADD COLUMN legal_hold boolean DEFAULT false NOT NULL;

CREATE INDEX audit_events_checklist_retention_ix
    ON public.audit_events(created_at)
    WHERE event_category LIKE 'CHECKLIST\_%' ESCAPE '\' AND legal_hold = false;

CREATE INDEX checklist_migration_quarantine_retention_ix
    ON public.checklist_migration_quarantine(created_at, retain_until)
    WHERE legal_hold = false;

CREATE INDEX checklist_action_commands_retention_ix
    ON public.checklist_action_commands(created_at, retain_until, task_kind, task_id)
    WHERE legal_hold = false;

-- Deployment must pre-provision both roles. Flyway deliberately has no
-- CREATEROLE dependency and never receives membership in the NOLOGIN owner.
-- A separate privileged finalizer transfers only the purge function after this
-- migration and grants checklist_operations last.
DO $$
DECLARE
    owner_role_oid oid;
BEGIN
    SELECT oid INTO owner_role_oid
    FROM pg_catalog.pg_roles
    WHERE rolname = 'carebridge_checklist_retention_owner'
      AND rolcanlogin = false
      AND rolsuper = false
      AND rolcreatedb = false
      AND rolcreaterole = false
      AND rolinherit = false
      AND rolreplication = false
      AND rolbypassrls = false;
    IF owner_role_oid IS NULL THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_OWNER_ROLE_REQUIRED'
            USING ERRCODE = '42501';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles
        WHERE rolname = 'checklist_operations'
          AND rolcanlogin = true
          AND rolsuper = false
          AND rolcreatedb = false
          AND rolcreaterole = false
          AND rolinherit = false
          AND rolreplication = false
          AND rolbypassrls = false
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_OPERATIONS_ROLE_REQUIRED'
            USING ERRCODE = '42501';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles
        WHERE rolname = 'carebridge_checklist_schema_owner'
          AND rolcanlogin = false
          AND rolsuper = false
          AND rolcreatedb = false
          AND rolcreaterole = false
          AND rolinherit = false
          AND rolreplication = false
          AND rolbypassrls = false
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_SCHEMA_OWNER_ROLE_REQUIRED'
            USING ERRCODE = '42501';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles
        WHERE rolname = 'carebridge_application'
          AND rolcanlogin = true
          AND rolsuper = false
          AND rolcreatedb = false
          AND rolcreaterole = false
          AND rolinherit = false
          AND rolreplication = false
          AND rolbypassrls = false
    ) THEN
        RAISE EXCEPTION 'CAREBRIDGE_APPLICATION_ROLE_REQUIRED'
            USING ERRCODE = '42501';
    END IF;

    IF current_user IN (
        'carebridge_application',
        'checklist_operations',
        'carebridge_checklist_retention_owner',
        'carebridge_checklist_schema_owner'
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_FLYWAY_ROLE_MUST_BE_SEPARATE'
            USING ERRCODE = '42501';
    END IF;

    -- Supabase manages the postgres membership for this owner role through
    -- supabase_admin. The deployment-time isolation check is therefore not
    -- enforceable from the application connection. Runtime purge checks remain
    -- fail-closed when the owner role is reachable by an operational caller.
END $$;

GRANT USAGE ON SCHEMA public TO carebridge_checklist_retention_owner;
GRANT SELECT ON public.users,
                public.care_tasks,
                public.checklist_task_instances
    TO carebridge_checklist_retention_owner;
GRANT SELECT, INSERT, DELETE ON public.audit_events
    TO carebridge_checklist_retention_owner;
GRANT SELECT, DELETE ON public.checklist_migration_quarantine,
                        public.checklist_action_commands
    TO carebridge_checklist_retention_owner;

-- Runtime DML is granted to the explicit application login, never to the
-- Flyway current_user. Retained records remain non-deletable at table ACL level.
REVOKE ALL ON public.audit_events,
              public.checklist_migration_quarantine,
              public.checklist_action_commands
    FROM carebridge_application;
GRANT SELECT, INSERT ON public.audit_events TO carebridge_application;
GRANT SELECT, UPDATE ON public.checklist_migration_quarantine TO carebridge_application;
GRANT SELECT, INSERT, UPDATE ON public.checklist_action_commands TO carebridge_application;

-- FORCE ROW LEVEL SECURITY was enabled when the quarantine table was created.
-- Application connections retain read/resolve access only. A dedicated
-- checklist_operations database login invokes the audited SECURITY DEFINER
-- function below; it never receives raw DELETE privileges.
CREATE POLICY checklist_migration_quarantine_application_select
    ON public.checklist_migration_quarantine FOR SELECT
    TO carebridge_application USING (true);
CREATE POLICY checklist_migration_quarantine_application_resolve
    ON public.checklist_migration_quarantine FOR UPDATE
    TO carebridge_application
    USING (resolved_at IS NULL)
    WITH CHECK (resolved_at IS NOT NULL AND resolved_by IS NOT NULL
                AND resolution_code IS NOT NULL);

CREATE POLICY checklist_migration_quarantine_retention_owner_delete
    ON public.checklist_migration_quarantine FOR DELETE
    TO carebridge_checklist_retention_owner
    USING (legal_hold = false
           AND created_at < clock_timestamp() - interval '7 years'
           AND retain_until <= clock_timestamp());

-- PostgreSQL evaluates SELECT visibility for DELETE predicates/RETURNING under
-- FORCE RLS. Keep that visibility owner-only and limited to the same eligible
-- rows; without it an authorized purge is silently filtered to zero rows.
CREATE POLICY checklist_migration_quarantine_retention_owner_select
    ON public.checklist_migration_quarantine FOR SELECT
    TO carebridge_checklist_retention_owner
    USING (legal_hold = false
           AND created_at < clock_timestamp() - interval '7 years'
           AND retain_until <= clock_timestamp());

REVOKE DELETE ON public.audit_events,
                 public.checklist_migration_quarantine,
                 public.checklist_action_commands
    FROM PUBLIC, carebridge_application, checklist_operations;

-- Forensic quarantine fields are immutable. Resolution is the only permitted
-- update and may change only resolved_at/resolved_by/resolution_code.
CREATE OR REPLACE FUNCTION public.checklist_quarantine_forensic_guard() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF current_user = 'carebridge_checklist_retention_owner'
           AND OLD.legal_hold = false
           AND OLD.created_at < clock_timestamp() - interval '7 years'
           AND OLD.retain_until <= clock_timestamp() THEN
            RETURN OLD;
        END IF;
        RAISE EXCEPTION
            'RETENTION_DELETE_NOT_AUTHORIZED: checklist quarantine is not eligible or caller is not the retention owner'
            USING ERRCODE = '42501';
    END IF;

    IF OLD.quarantine_id IS DISTINCT FROM NEW.quarantine_id
       OR OLD.source_table IS DISTINCT FROM NEW.source_table
       OR OLD.source_id IS DISTINCT FROM NEW.source_id
       OR OLD.reason_code IS DISTINCT FROM NEW.reason_code
       OR OLD.payload_ciphertext IS DISTINCT FROM NEW.payload_ciphertext
       OR OLD.payload_hash IS DISTINCT FROM NEW.payload_hash
       OR OLD.encryption_key_version IS DISTINCT FROM NEW.encryption_key_version
       OR OLD.correlation_id IS DISTINCT FROM NEW.correlation_id
       OR OLD.legal_hold IS DISTINCT FROM NEW.legal_hold
       OR OLD.retain_until IS DISTINCT FROM NEW.retain_until
       OR OLD.created_at IS DISTINCT FROM NEW.created_at
       OR OLD.care_context_type IS DISTINCT FROM NEW.care_context_type
       OR OLD.care_context_id IS DISTINCT FROM NEW.care_context_id THEN
        RAISE EXCEPTION 'IMMUTABLE_FORENSIC_FIELDS: checklist quarantine metadata is append-only';
    END IF;
    RETURN NEW;
END
$$;

REVOKE ALL ON FUNCTION public.checklist_quarantine_forensic_guard() FROM PUBLIC;

DROP TRIGGER IF EXISTS checklist_quarantine_forensic_guard_trg
    ON public.checklist_migration_quarantine;
CREATE TRIGGER checklist_quarantine_forensic_guard_trg
    BEFORE UPDATE OR DELETE ON public.checklist_migration_quarantine
    FOR EACH ROW EXECUTE FUNCTION public.checklist_quarantine_forensic_guard();

-- Action-command rows are also forensic/idempotency records. A raw DELETE must
-- be impossible for the shared application role; the owner-only purge path is
-- allowed only for expired, non-held rows whose task is terminal or orphaned.
CREATE OR REPLACE FUNCTION public.checklist_action_command_retention_guard()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF current_user = 'carebridge_checklist_retention_owner'
       AND OLD.legal_hold = false
       AND OLD.created_at < clock_timestamp() - interval '7 years'
       AND OLD.retain_until <= clock_timestamp()
       AND (
           (OLD.task_kind = 'CHECKLIST' AND EXISTS (
               SELECT 1
               FROM public.checklist_task_instances task
               WHERE task.checklist_task_instance_id = OLD.task_id
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
           ))
           OR
           (OLD.task_kind = 'CARE_TASK' AND EXISTS (
               SELECT 1
               FROM public.care_tasks task
               WHERE task.task_id = OLD.task_id
                 AND task.status IN ('DONE', 'CANCELLED')
           ))
           OR
           (OLD.task_kind = 'REMINDER' AND EXISTS (
               SELECT 1
               FROM public.care_tasks task
               WHERE task.task_id = OLD.reminder_definition_id
                 AND task.task_type = 'SCHEDULED_REMINDER'
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
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
       ) THEN
        RETURN OLD;
    END IF;

    RAISE EXCEPTION
        'RETENTION_DELETE_NOT_AUTHORIZED: checklist action command is not eligible or caller is not the retention owner'
        USING ERRCODE = '42501';
END
$$;

REVOKE ALL ON FUNCTION public.checklist_action_command_retention_guard() FROM PUBLIC;

DROP TRIGGER IF EXISTS checklist_action_command_retention_guard_trg
    ON public.checklist_action_commands;
CREATE TRIGGER checklist_action_command_retention_guard_trg
    BEFORE DELETE ON public.checklist_action_commands
    FOR EACH ROW EXECUTE FUNCTION public.checklist_action_command_retention_guard();

-- Keep audit_events immutable for every ordinary UPDATE/DELETE. Only statements
-- executing as the non-login retention owner may delete an eligible row.
CREATE OR REPLACE FUNCTION public.carebridge_reject_mutation() RETURNS trigger
    LANGUAGE plpgsql
AS $$
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
$$;

REVOKE ALL ON FUNCTION public.carebridge_reject_mutation() FROM PUBLIC;

CREATE OR REPLACE FUNCTION public.checklist_purge_retained_records(p_actor_user_id uuid)
RETURNS TABLE (
    audit_events_purged bigint,
    quarantines_purged bigint,
    action_commands_purged bigint
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
BEGIN
    IF session_user <> 'checklist_operations' THEN
        RAISE EXCEPTION 'PURGE_DATABASE_CALLER_NOT_TRUSTED'
            USING ERRCODE = '42501';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_catalog.pg_auth_members membership
        JOIN pg_catalog.pg_roles owner_role
          ON owner_role.oid = membership.roleid
        WHERE owner_role.rolname = 'carebridge_checklist_retention_owner'
    ) THEN
        RAISE EXCEPTION 'PURGE_OWNER_ROLE_REACHABLE'
            USING ERRCODE = '42501';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM public.users actor
        WHERE actor.user_id = p_actor_user_id
          AND actor.role IN ('SYSTEM_ADMIN', 'OPERATIONS')
          AND actor.account_status = 'ACTIVE'
          AND actor.enabled = true
          AND actor.locked = false
    ) THEN
        RAISE EXCEPTION 'PURGE_ACTOR_NOT_TRUSTED' USING ERRCODE = '42501';
    END IF;

    WITH deleted AS (
        DELETE FROM public.audit_events event
        WHERE event.event_category LIKE 'CHECKLIST\_%' ESCAPE '\'
          AND event.created_at < clock_timestamp() - interval '7 years'
          AND event.legal_hold = false
        RETURNING 1
    )
    SELECT count(*) INTO audit_events_purged FROM deleted;

    WITH deleted AS (
        DELETE FROM public.checklist_migration_quarantine quarantine
        WHERE quarantine.created_at < clock_timestamp() - interval '7 years'
          AND quarantine.retain_until <= clock_timestamp()
          AND quarantine.legal_hold = false
        RETURNING 1
    )
    SELECT count(*) INTO quarantines_purged FROM deleted;

    WITH deleted AS (
        DELETE FROM public.checklist_action_commands command
        WHERE command.created_at < clock_timestamp() - interval '7 years'
          AND command.retain_until <= clock_timestamp()
          AND command.legal_hold = false
          AND (
              (command.task_kind = 'CHECKLIST' AND EXISTS (
                  SELECT 1
                  FROM public.checklist_task_instances task
                  WHERE task.checklist_task_instance_id = command.task_id
                    AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
              ))
              OR
              (command.task_kind = 'CARE_TASK' AND EXISTS (
                  SELECT 1
                  FROM public.care_tasks task
                  WHERE task.task_id = command.task_id
                    AND task.status IN ('DONE', 'CANCELLED')
              ))
              OR
              (command.task_kind = 'REMINDER' AND EXISTS (
                  SELECT 1
                  FROM public.care_tasks task
                  WHERE task.task_id = command.reminder_definition_id
                    AND task.task_type = 'SCHEDULED_REMINDER'
                    AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
              ))
              OR (command.task_kind = 'CHECKLIST' AND NOT EXISTS (
                  SELECT 1
                  FROM public.checklist_task_instances checklist_task
                  WHERE checklist_task.checklist_task_instance_id = command.task_id
              ))
              OR (command.task_kind = 'CARE_TASK' AND NOT EXISTS (
                  SELECT 1
                  FROM public.care_tasks care_task
                  WHERE care_task.task_id = command.task_id
              ))
              OR (command.task_kind = 'REMINDER' AND NOT EXISTS (
                  SELECT 1
                  FROM public.care_tasks reminder_definition
                  WHERE reminder_definition.task_id = command.reminder_definition_id
              ))
          )
        RETURNING 1
    )
    SELECT count(*) INTO action_commands_purged FROM deleted;

    -- This insert is in the same transaction as all deletes. Any failure rolls
    -- the purge back, making the operations endpoint fail closed.
    INSERT INTO public.audit_events (
        actor_user_id,
        actor_type,
        event_category,
        resource_type,
        reason_code,
        correlation_id,
        after_payload_jsonb,
        occurred_at,
        created_at,
        event_origin,
        legal_hold
    ) VALUES (
        p_actor_user_id,
        'USER',
        'CHECKLIST_RETENTION_PURGED',
        'ChecklistRetention',
        'SEVEN_YEAR_RETENTION',
        gen_random_uuid(),
        jsonb_build_object(
            'auditEventsPurged', audit_events_purged,
            'quarantinesPurged', quarantines_purged,
            'actionCommandsPurged', action_commands_purged),
        clock_timestamp(),
        clock_timestamp(),
        'AUDIT_LOG',
        false
    );

    RETURN QUERY SELECT
        audit_events_purged,
        quarantines_purged,
        action_commands_purged;
END
$$;

REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM CURRENT_USER;

-- No runtime role can execute the intermediate function. A separate privileged
-- versioned deployment finalizer transfers ONLY this function to the NOLOGIN
-- owner, validates catalog invariants, and grants checklist_operations last.
-- END CANONICAL SOURCE: V20260729150000__add_checklist_operations_retention.sql

-- BEGIN CANONICAL SOURCE: V20260729160000__persist_reminder_occurrence_aliases.sql
-- Durable reminder occurrence identity. This table is intentionally independent
-- of retained action-command data so lawful command retention cannot erase the
-- occurrence-to-definition link required by terminal CAS and new request IDs.
CREATE TABLE public.reminder_occurrence_aliases (
    occurrence_id uuid NOT NULL,
    reminder_definition_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    scheduled_at timestamptz NOT NULL,
    created_at timestamptz DEFAULT clock_timestamp() NOT NULL,
    PRIMARY KEY (occurrence_id),
    CONSTRAINT reminder_occurrence_alias_definition_schedule_uk
        UNIQUE (reminder_definition_id, scheduled_at)
);

CREATE INDEX reminder_occurrence_alias_owner_ix
    ON public.reminder_occurrence_aliases(owner_user_id, occurrence_id);
CREATE INDEX reminder_occurrence_alias_definition_ix
    ON public.reminder_occurrence_aliases(reminder_definition_id);

-- Match Java UUID.nameUUIDFromBytes over
-- "reminder-occurrence-v1|<lowercase uuid>|<Instant.toString()>". PostgreSQL
-- timestamps have microsecond precision; ISO_INSTANT emits 0, 3 or 6 fraction
-- digits for those values.
CREATE OR REPLACE FUNCTION public.reminder_occurrence_id_v1(
    p_reminder_definition_id uuid,
    p_scheduled_at timestamptz
) RETURNS uuid
LANGUAGE plpgsql
IMMUTABLE STRICT
SET search_path = pg_catalog, public
AS $$
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
$$;

CREATE OR REPLACE FUNCTION public.capture_reminder_occurrence_alias()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
BEGIN
    IF NEW.task_type = 'SCHEDULED_REMINDER' AND NEW.scheduled_at IS NOT NULL THEN
        INSERT INTO public.reminder_occurrence_aliases (
            occurrence_id,
            reminder_definition_id,
            owner_user_id,
            scheduled_at
        ) VALUES (
            public.reminder_occurrence_id_v1(NEW.task_id, NEW.scheduled_at),
            NEW.task_id,
            NEW.owner_user_id,
            NEW.scheduled_at
        ) ON CONFLICT (occurrence_id) DO NOTHING;
    END IF;
    RETURN NEW;
END
$$;

INSERT INTO public.reminder_occurrence_aliases (
    occurrence_id,
    reminder_definition_id,
    owner_user_id,
    scheduled_at,
    created_at
)
SELECT
    public.reminder_occurrence_id_v1(task.task_id, task.scheduled_at),
    task.task_id,
    task.owner_user_id,
    task.scheduled_at,
    COALESCE(task.created_at, clock_timestamp())
FROM public.care_tasks task
WHERE task.task_type = 'SCHEDULED_REMINDER'
  AND task.scheduled_at IS NOT NULL
ON CONFLICT (occurrence_id) DO NOTHING;

CREATE TRIGGER care_tasks_reminder_occurrence_alias_trg
    AFTER INSERT OR UPDATE OF scheduled_at, owner_user_id ON public.care_tasks
    FOR EACH ROW EXECUTE FUNCTION public.capture_reminder_occurrence_alias();

REVOKE ALL ON FUNCTION public.reminder_occurrence_id_v1(uuid, timestamptz) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.capture_reminder_occurrence_alias() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.reminder_occurrence_id_v1(uuid, timestamptz) TO CURRENT_USER;
REVOKE ALL ON TABLE public.reminder_occurrence_aliases FROM PUBLIC;
GRANT SELECT, INSERT ON public.reminder_occurrence_aliases TO CURRENT_USER;
GRANT SELECT ON public.reminder_occurrence_aliases TO carebridge_application;
-- END CANONICAL SOURCE: V20260729160000__persist_reminder_occurrence_aliases.sql

-- BEGIN CANONICAL SOURCE: V20260730010000__add_reminder_occurrence_generation.sql
-- A reminder definition may be cancelled and re-enabled at the same schedule.
-- Generation keeps the re-enabled occurrence distinct from retained commands
-- while preserving every legacy generation-0 occurrence UUID byte-for-byte.
ALTER TABLE public.care_tasks
    ADD COLUMN reminder_occurrence_generation bigint NOT NULL DEFAULT 0;

ALTER TABLE public.care_tasks
    ADD CONSTRAINT care_tasks_reminder_occurrence_generation_ck
        CHECK (reminder_occurrence_generation >= 0);

ALTER TABLE public.reminder_occurrence_aliases
    ADD COLUMN occurrence_generation bigint NOT NULL DEFAULT 0;

ALTER TABLE public.reminder_occurrence_aliases
    DROP CONSTRAINT reminder_occurrence_alias_definition_schedule_uk;

ALTER TABLE public.reminder_occurrence_aliases
    ADD CONSTRAINT reminder_occurrence_alias_definition_generation_schedule_uk
        UNIQUE (reminder_definition_id, occurrence_generation, scheduled_at);

CREATE OR REPLACE FUNCTION public.reminder_occurrence_id_v2(
    p_reminder_definition_id uuid,
    p_scheduled_at timestamptz,
    p_occurrence_generation bigint
) RETURNS uuid
LANGUAGE plpgsql
IMMUTABLE STRICT
SET search_path = pg_catalog, public
AS $$
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
$$;

CREATE OR REPLACE FUNCTION public.capture_reminder_occurrence_alias()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
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
$$;

DROP TRIGGER care_tasks_reminder_occurrence_alias_trg ON public.care_tasks;
CREATE TRIGGER care_tasks_reminder_occurrence_alias_trg
    AFTER INSERT OR UPDATE OF scheduled_at, owner_user_id, reminder_occurrence_generation
    ON public.care_tasks
    FOR EACH ROW EXECUTE FUNCTION public.capture_reminder_occurrence_alias();

REVOKE ALL ON FUNCTION public.reminder_occurrence_id_v2(uuid, timestamptz, bigint)
    FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.reminder_occurrence_id_v2(uuid, timestamptz, bigint)
    TO CURRENT_USER;
-- END CANONICAL SOURCE: V20260730010000__add_reminder_occurrence_generation.sql

-- BEGIN CANONICAL SOURCE: V20260730020000__add_appointment_notification_scheduling.sql
CREATE TABLE public.appointment_notification_configs (
    reminder_id uuid NOT NULL,
    time_zone varchar(80) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    config_revision bigint NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT appointment_notification_configs_pkey PRIMARY KEY (reminder_id),
    CONSTRAINT appointment_notification_configs_reminder_fk
        FOREIGN KEY (reminder_id) REFERENCES public.care_tasks(task_id) ON DELETE CASCADE,
    CONSTRAINT appointment_notification_configs_revision_ck CHECK (config_revision > 0),
    CONSTRAINT appointment_notification_configs_timezone_ck CHECK (length(trim(time_zone)) > 0)
);

CREATE TABLE public.appointment_notification_rules (
    rule_id uuid NOT NULL,
    reminder_id uuid NOT NULL,
    offset_minutes integer NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT appointment_notification_rules_pkey PRIMARY KEY (rule_id),
    CONSTRAINT appointment_notification_rules_config_fk
        FOREIGN KEY (reminder_id) REFERENCES public.appointment_notification_configs(reminder_id) ON DELETE CASCADE,
    CONSTRAINT appointment_notification_rules_reminder_offset_uk UNIQUE (reminder_id, offset_minutes),
    CONSTRAINT appointment_notification_rules_offset_ck
        CHECK (offset_minutes BETWEEN -43200 AND 10080),
    CONSTRAINT appointment_notification_rules_sort_ck CHECK (sort_order >= 0)
);

CREATE TABLE public.appointment_notification_jobs (
    job_id uuid NOT NULL,
    reminder_id uuid NOT NULL,
    occurrence_id uuid NOT NULL,
    occurrence_generation bigint NOT NULL DEFAULT 0,
    occurrence_scheduled_at timestamptz NOT NULL,
    config_revision bigint NOT NULL,
    offset_minutes integer NOT NULL,
    due_at timestamptz NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    attempt_count integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL,
    locked_by varchar(120),
    locked_at timestamptz,
    notification_record_id uuid,
    last_error_code varchar(80),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT appointment_notification_jobs_pkey PRIMARY KEY (job_id),
    CONSTRAINT appointment_notification_jobs_reminder_fk
        FOREIGN KEY (reminder_id) REFERENCES public.care_tasks(task_id) ON DELETE CASCADE,
    CONSTRAINT appointment_notification_jobs_record_fk
        FOREIGN KEY (notification_record_id) REFERENCES public.notification_records(id) ON DELETE SET NULL,
    CONSTRAINT appointment_notification_jobs_identity_uk
        UNIQUE (reminder_id, occurrence_id, config_revision, offset_minutes),
    CONSTRAINT appointment_notification_jobs_generation_ck CHECK (occurrence_generation >= 0),
    CONSTRAINT appointment_notification_jobs_revision_ck CHECK (config_revision > 0),
    CONSTRAINT appointment_notification_jobs_offset_ck
        CHECK (offset_minutes BETWEEN -43200 AND 10080),
    CONSTRAINT appointment_notification_jobs_attempt_ck CHECK (attempt_count >= 0),
    CONSTRAINT appointment_notification_jobs_status_ck CHECK (status IN (
        'PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SUPPRESSED', 'CANCELLED'
    )),
    CONSTRAINT appointment_notification_jobs_lock_ck CHECK (
        (status = 'PROCESSING' AND locked_by IS NOT NULL AND locked_at IS NOT NULL)
        OR status <> 'PROCESSING'
    )
);

CREATE INDEX appointment_notification_jobs_due_ix
    ON public.appointment_notification_jobs(status, next_attempt_at, due_at);
CREATE INDEX appointment_notification_jobs_reminder_revision_ix
    ON public.appointment_notification_jobs(reminder_id, config_revision, status);
CREATE INDEX appointment_notification_jobs_occurrence_ix
    ON public.appointment_notification_jobs(occurrence_id, status);

CREATE UNIQUE INDEX uq_notification_records_appointment_milestone
    ON public.notification_records(user_id, reference_id, ((metadata ->> 'milestoneJobId')))
    WHERE type = 'REMINDER'
      AND reference_type = 'APPOINTMENT'
      AND metadata ? 'milestoneJobId';
-- END CANONICAL SOURCE: V20260730020000__add_appointment_notification_scheduling.sql

-- BEGIN CANONICAL SOURCE: V20260730030000__accepted_membership_ignores_invitation_expiry.sql
-- Invitation expiry controls whether a PENDING invitation can be accepted.
-- Once membership is ACCEPTED, the original invitation timestamp must not revoke access.
CREATE OR REPLACE FUNCTION public.checklist_validate_instance_recipient()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.recipient_role = 'FAMILY' THEN
        PERFORM 1
        FROM public.care_group_members member
        WHERE member.care_group_id = NEW.care_group_id
          AND member.user_id = NEW.recipient_user_id
          AND member.invitation_status = 'ACCEPTED'
          AND jsonb_typeof(member.permission_json) = 'object'
          AND CASE
              WHEN member.permission_json ? 'CHECKLIST_VIEW' THEN
                  jsonb_typeof(member.permission_json->'CHECKLIST_VIEW') = 'boolean'
                  AND member.permission_json->>'CHECKLIST_VIEW' = 'true'
              WHEN member.permission_json ? 'checklist_view' THEN
                  jsonb_typeof(member.permission_json->'checklist_view') = 'boolean'
                  AND member.permission_json->>'checklist_view' = 'true'
              ELSE false
          END
        FOR KEY SHARE;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'CHECKLIST_FAMILY_RECIPIENT_NOT_AUTHORIZED';
        END IF;
    END IF;
    RETURN NEW;
END $$;
-- END CANONICAL SOURCE: V20260730030000__accepted_membership_ignores_invitation_expiry.sql

-- BEGIN CANONICAL SOURCE: V20260730040000__block_stale_care_group_contexts_on_relink.sql
-- A care-group relink must revoke the previous checklist authority without
-- deleting it because historical checklist instances retain a restrictive FK.
UPDATE public.checklist_care_group_contexts mapping
SET review_status = 'BLOCKED',
    distribution_blocked = true,
    block_reason_code = 'CARE_GROUP_CONTEXT_RELINKED',
    updated_at = now()
FROM public.care_groups group_row
WHERE mapping.care_group_id = group_row.care_group_id
  AND mapping.owner_user_id = group_row.owner_user_id
  AND mapping.review_status = 'REVIEWED'
  AND mapping.distribution_blocked = false
  AND (
      (mapping.care_context_type = 'JOURNEY'
          AND mapping.care_context_id IS DISTINCT FROM group_row.linked_journey_id)
      OR
      (mapping.care_context_type = 'BABY'
          AND mapping.care_context_id IS DISTINCT FROM group_row.linked_baby_profile_id)
  );

CREATE UNIQUE INDEX checklist_care_group_contexts_single_active_type_ux
    ON public.checklist_care_group_contexts(care_group_id, care_context_type)
    WHERE review_status = 'REVIEWED' AND distribution_blocked = false;

CREATE OR REPLACE FUNCTION public.checklist_block_previous_care_group_context()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
BEGIN
    IF OLD.linked_journey_id IS DISTINCT FROM NEW.linked_journey_id
       AND OLD.linked_journey_id IS NOT NULL THEN
        UPDATE public.checklist_care_group_contexts
        SET review_status = 'BLOCKED',
            distribution_blocked = true,
            block_reason_code = 'CARE_GROUP_CONTEXT_RELINKED',
            updated_at = now()
        WHERE care_group_id = OLD.care_group_id
          AND owner_user_id = OLD.owner_user_id
          AND care_context_type = 'JOURNEY'
          AND care_context_id = OLD.linked_journey_id
          AND review_status <> 'BLOCKED';
    END IF;

    IF OLD.linked_baby_profile_id IS DISTINCT FROM NEW.linked_baby_profile_id
       AND OLD.linked_baby_profile_id IS NOT NULL THEN
        UPDATE public.checklist_care_group_contexts
        SET review_status = 'BLOCKED',
            distribution_blocked = true,
            block_reason_code = 'CARE_GROUP_CONTEXT_RELINKED',
            updated_at = now()
        WHERE care_group_id = OLD.care_group_id
          AND owner_user_id = OLD.owner_user_id
          AND care_context_type = 'BABY'
          AND care_context_id = OLD.linked_baby_profile_id
          AND review_status <> 'BLOCKED';
    END IF;
    RETURN NEW;
END $$;

REVOKE ALL ON FUNCTION public.checklist_block_previous_care_group_context()
    FROM PUBLIC;

CREATE TRIGGER checklist_block_previous_care_group_context_trg
BEFORE UPDATE OF linked_journey_id, linked_baby_profile_id
ON public.care_groups
FOR EACH ROW EXECUTE FUNCTION public.checklist_block_previous_care_group_context();

-- Reactivate a previously blocked mapping when a later relink returns to it.
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
                 review_status, distribution_blocked, block_reason_code,
                 reviewed_at, reviewed_by)
            VALUES (NEW.care_group_id, NEW.owner_user_id, 'JOURNEY', NEW.linked_journey_id,
                    'REVIEWED', false, NULL, now(), NEW.owner_user_id)
            ON CONFLICT (care_group_id, care_context_type, care_context_id) DO UPDATE
            SET owner_user_id = EXCLUDED.owner_user_id,
                review_status = 'REVIEWED',
                distribution_blocked = false,
                block_reason_code = NULL,
                reviewed_at = now(),
                reviewed_by = EXCLUDED.reviewed_by,
                updated_at = now();
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
                 review_status, distribution_blocked, block_reason_code,
                 reviewed_at, reviewed_by)
            VALUES (NEW.care_group_id, NEW.owner_user_id, 'BABY', NEW.linked_baby_profile_id,
                    'REVIEWED', false, NULL, now(), NEW.owner_user_id)
            ON CONFLICT (care_group_id, care_context_type, care_context_id) DO UPDATE
            SET owner_user_id = EXCLUDED.owner_user_id,
                review_status = 'REVIEWED',
                distribution_blocked = false,
                block_reason_code = NULL,
                reviewed_at = now(),
                reviewed_by = EXCLUDED.reviewed_by,
                updated_at = now();
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
-- END CANONICAL SOURCE: V20260730040000__block_stale_care_group_contexts_on_relink.sql

-- BEGIN CANONICAL SOURCE: V20260730050000__persist_checklist_task_category.sql
ALTER TABLE public.checklist_task_instances
    ADD COLUMN category varchar(20) DEFAULT 'GENERAL' NOT NULL;

ALTER TABLE public.checklist_task_instances
    ADD CONSTRAINT checklist_task_instances_category_ck CHECK
        (category IN ('DELIVERY','PAPERWORK','BABY_CARE','GENERAL'));

-- V20260729070000 preserves legacy checklist_item_id as the V2 task id. Restore the
-- persisted category before the compatibility GET starts preferring canonical V2 rows.
UPDATE public.checklist_task_instances task
SET category = legacy.category
FROM public.preparation_checklist_items legacy
WHERE task.checklist_task_instance_id = legacy.checklist_item_id
  AND legacy.category IN ('DELIVERY','PAPERWORK','BABY_CARE','GENERAL')
  AND task.category IS DISTINCT FROM legacy.category;
-- END CANONICAL SOURCE: V20260730050000__persist_checklist_task_category.sql

-- BEGIN CANONICAL SOURCE: V20260730060000__classify_checklist_template_assignment.sql
ALTER TABLE care_item_templates
    ADD COLUMN template_type varchar(20) DEFAULT 'MANDATORY' NOT NULL;

ALTER TABLE care_item_templates
    ADD CONSTRAINT ck_care_item_templates_template_type
        CHECK (template_type IN ('MANDATORY', 'OPTIONAL'));

COMMENT ON COLUMN care_item_templates.template_type IS
    'MANDATORY templates are auto-distributed; OPTIONAL templates are added explicitly by users.';

INSERT INTO public.checklist_substages
    (code, stage, anchor_type, range_unit, start_inclusive, end_inclusive)
VALUES ('PRE_PREGNANCY_ALL', 'PRE_PREGNANCY', 'NONE', 'DAY', 0, 0)
ON CONFLICT (code) DO NOTHING;

CREATE OR REPLACE FUNCTION public.checklist_guard_template_type_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
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
END $$;

CREATE TRIGGER checklist_guard_template_type_mutation_trg
BEFORE UPDATE OF template_type ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_template_type_mutation();

CREATE OR REPLACE FUNCTION public.checklist_validate_template_approval()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    role_count integer;
    mother_count integer;
    family_count integer;
    substage_stage varchar(30);
    substage_anchor varchar(30);
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' THEN
        RETURN NEW;
    END IF;

    IF (NEW.distribution_enabled = true OR NEW.content_status = 'APPROVED')
       AND NEW.migration_review_required = true THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF TG_OP = 'UPDATE' AND NEW.distribution_enabled = true
       AND OLD.migration_review_required = true THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF TG_OP = 'UPDATE'
       AND OLD.migration_review_required = true AND NEW.migration_review_required = false
       AND (NEW.migration_reviewed_at IS NULL OR NEW.migration_reviewed_by IS NULL) THEN
        RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED';
    END IF;

    IF NEW.distribution_enabled = true
       OR NEW.content_status = 'APPROVED'
       OR (TG_OP = 'UPDATE' AND OLD.migration_review_required = true
           AND NEW.migration_review_required = false) THEN
        SELECT count(*),
               count(*) FILTER (WHERE recipient_role = 'MOTHER'),
               count(*) FILTER (WHERE recipient_role = 'FAMILY')
        INTO role_count, mother_count, family_count
        FROM public.checklist_template_recipient_roles
        WHERE template_version_id = NEW.template_version_id;

        IF role_count = 0 THEN
            RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED';
        END IF;

        IF family_count > 0 AND mother_count = 0
           AND (NEW.stage IS NOT NULL OR NEW.substage_id IS NOT NULL) THEN
            RAISE EXCEPTION 'FAMILY_STAGE_NOT_ALLOWED';
        END IF;

        IF NEW.substage_id IS NOT NULL THEN
            SELECT stage, anchor_type INTO substage_stage, substage_anchor
            FROM public.checklist_substages
            WHERE substage_id = NEW.substage_id
              AND is_active = true;
            IF substage_stage IS NULL OR substage_stage IS DISTINCT FROM NEW.stage
               OR substage_anchor IS NULL
               OR NOT (
                   (NEW.stage = 'PRE_PREGNANCY' AND substage_anchor = 'NONE') OR
                   (NEW.stage = 'PREGNANCY' AND substage_anchor IN ('LMP', 'EDD')) OR
                   (NEW.stage = 'POSTPARTUM' AND substage_anchor = 'DELIVERY_DATE') OR
                   (NEW.stage = 'BABY_CARE' AND substage_anchor = 'BIRTH_DATE')
               ) THEN
                RAISE EXCEPTION 'SUBSTAGE_STAGE_MISMATCH';
            END IF;
        ELSIF NEW.stage IS NOT NULL THEN
            RAISE EXCEPTION 'SUBSTAGE_STAGE_MISMATCH';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM public.care_item_templates item
            WHERE item.parent_template_id = NEW.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY'
              AND item.is_active = true
              AND item.target_subject IS NULL
        ) THEN
            RAISE EXCEPTION 'ITEM_TARGET_REQUIRED';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM public.care_item_templates item
            WHERE item.parent_template_id = NEW.template_id
              AND item.entry_type = 'CHECKLIST_ENTRY'
              AND item.is_active = true
              AND NOT EXISTS (
                  SELECT 1
                  FROM public.checklist_template_version_items version_item
                  WHERE version_item.template_version_id = NEW.template_version_id
                    AND version_item.template_item_version_id = item.template_id
              )
        ) THEN
            RAISE EXCEPTION 'ITEM_TARGET_REQUIRED';
        END IF;
    END IF;
    RETURN NEW;
END $$;

ALTER TABLE public.care_item_templates
    DROP CONSTRAINT care_item_templates_import_activation_gate_ck,
    ADD CONSTRAINT care_item_templates_import_activation_gate_ck CHECK
        (entry_type <> 'TEMPLATE_ROOT' OR migration_reviewed_at IS NULL
         OR content_status <> 'APPROVED'
         OR distribution_enabled = (template_type = 'MANDATORY'));

CREATE OR REPLACE FUNCTION public.checklist_guard_approved_template_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
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
END $$;
-- END CANONICAL SOURCE: V20260730060000__classify_checklist_template_assignment.sql

-- BEGIN CANONICAL SOURCE: V20260730070000__allow_personal_checklist_instances_without_care_groups.sql
-- A mother's checklist is owned by its canonical JOURNEY/BABY context.
-- A care group is optional and is retained only for FAMILY sharing.
ALTER TABLE public.checklist_instances
    ALTER COLUMN care_group_id DROP NOT NULL;

ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instances_personal_context_authority_fk
        FOREIGN KEY (care_context_type, care_context_id, context_owner_user_id)
        REFERENCES public.checklist_context_authorities
            (care_context_type, care_context_id, owner_user_id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT checklist_instances_family_group_scope_ck
        CHECK (recipient_role <> 'FAMILY' OR care_group_id IS NOT NULL);

-- The original trigger only validated INSERT. Re-check authorization whenever
-- an existing instance is moved to another recipient or group scope as well.
DROP TRIGGER IF EXISTS checklist_validate_instance_recipient_trg
    ON public.checklist_instances;

CREATE TRIGGER checklist_validate_instance_recipient_trg
BEFORE INSERT OR UPDATE OF recipient_role, recipient_user_id, care_group_id
ON public.checklist_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_instance_recipient();
-- END CANONICAL SOURCE: V20260730070000__allow_personal_checklist_instances_without_care_groups.sql

-- BEGIN CANONICAL SOURCE: V20260730235900__allow_personal_checklist_instances_without_care_groups.sql
-- Reconciliation candidates mirror the recipient context. Personal recipients
-- have a canonical context but intentionally have no group.
ALTER TABLE public.checklist_reconciliation_candidates
    DROP CONSTRAINT IF EXISTS checklist_reconciliation_candidates_context_ck,
    ADD CONSTRAINT checklist_reconciliation_candidates_context_ck CHECK (
        (recipient_user_id IS NULL AND care_group_id IS NULL
            AND care_context_type IS NULL AND care_context_id IS NULL) OR
        (recipient_user_id IS NOT NULL
            AND care_context_type IN ('JOURNEY','BABY') AND care_context_id IS NOT NULL)
    );
-- END CANONICAL SOURCE: V20260730235900__allow_personal_checklist_instances_without_care_groups.sql

-- BEGIN CANONICAL SOURCE: V20260731010000__deduplicate_legacy_personal_mother_checklists.sql
-- Collapse only legacy MOTHER/SYSTEM_TEMPLATE duplicates that have no progress.
-- Logical personal identity deliberately excludes care_group_id. Historical rows
-- with parent or child progress remain untouched for explicit reconciliation.
-- Flyway keeps these locks until commit. They close the gap between candidate
-- snapshot, child cancellation, and parent cancellation during a rolling deploy.
LOCK TABLE public.checklist_instances IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE public.checklist_task_instances IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE checklist_legacy_personal_duplicates_to_cancel
ON COMMIT DROP AS
WITH candidates AS MATERIALIZED (
    SELECT instance.*,
           instance.status = 'PENDING'
               AND instance.completed_at IS NULL
               AND instance.cancelled_at IS NULL
               AND instance.cancellation_reason_code IS NULL
               AND EXISTS (
                   SELECT 1
                   FROM public.checklist_task_instances task
                   WHERE task.checklist_instance_id = instance.checklist_instance_id)
               AND NOT EXISTS (
                   SELECT 1
                   FROM public.checklist_task_instances task
                   WHERE task.checklist_instance_id = instance.checklist_instance_id
                     AND (task.status <> 'PENDING'
                          OR task.completed_at IS NOT NULL
                          OR task.skipped_at IS NOT NULL
                          OR task.cancelled_at IS NOT NULL
                          OR task.action_reason_code IS NOT NULL)) AS safely_cancellable
    FROM public.checklist_instances instance
    WHERE instance.recipient_role = 'MOTHER'
      AND instance.origin = 'SYSTEM_TEMPLATE'
      AND instance.status <> 'CANCELLED'
), annotated AS MATERIALIZED (
    SELECT candidates.*,
           count(*) OVER logical_identity AS duplicate_count,
           count(*) FILTER (WHERE NOT safely_cancellable)
               OVER logical_identity AS preserved_count
    FROM candidates
    WINDOW logical_identity AS (
        PARTITION BY template_version_id, recipient_user_id,
                     care_context_type, care_context_id, context_owner_user_id,
                     window_start, window_end)
), ranked_safe AS MATERIALIZED (
    SELECT annotated.*,
           row_number() OVER (
               PARTITION BY template_version_id, recipient_user_id,
                            care_context_type, care_context_id, context_owner_user_id,
                            window_start, window_end
               ORDER BY CASE WHEN care_group_id IS NULL THEN 0 ELSE 1 END,
                        created_at, checklist_instance_id) AS safe_rank
    FROM annotated
    WHERE safely_cancellable
)
SELECT checklist_instance_id, gen_random_uuid() AS correlation_id
FROM ranked_safe
WHERE duplicate_count > 1
  AND (preserved_count > 0 OR safe_rank > 1);

CREATE UNIQUE INDEX checklist_legacy_personal_duplicates_to_cancel_pk
    ON checklist_legacy_personal_duplicates_to_cancel(checklist_instance_id);

UPDATE public.checklist_task_instances task
SET status = 'CANCELLED',
    completed_at = NULL,
    skipped_at = NULL,
    cancelled_at = transaction_timestamp(),
    action_reason_code = 'LEGACY_PERSONAL_DUPLICATE',
    lock_version = task.lock_version + 1,
    updated_at = transaction_timestamp()
FROM checklist_legacy_personal_duplicates_to_cancel duplicate
WHERE task.checklist_instance_id = duplicate.checklist_instance_id
  AND task.status = 'PENDING'
  AND task.completed_at IS NULL
  AND task.skipped_at IS NULL
  AND task.cancelled_at IS NULL
  AND task.action_reason_code IS NULL
  AND EXISTS (
      SELECT 1
      FROM public.checklist_instances parent
      WHERE parent.checklist_instance_id = task.checklist_instance_id
        AND parent.status = 'PENDING'
        AND parent.completed_at IS NULL
        AND parent.cancelled_at IS NULL
        AND parent.cancellation_reason_code IS NULL)
  AND NOT EXISTS (
      SELECT 1
      FROM public.checklist_task_instances sibling
      WHERE sibling.checklist_instance_id = task.checklist_instance_id
        AND (sibling.status <> 'PENDING'
             OR sibling.completed_at IS NOT NULL
             OR sibling.skipped_at IS NOT NULL
             OR sibling.cancelled_at IS NOT NULL
             OR sibling.action_reason_code IS NOT NULL));

UPDATE public.checklist_instances instance
SET status = 'CANCELLED',
    completed_at = NULL,
    cancelled_at = transaction_timestamp(),
    cancellation_reason_code = 'LEGACY_PERSONAL_DUPLICATE',
    lock_version = instance.lock_version + 1,
    updated_at = transaction_timestamp()
FROM checklist_legacy_personal_duplicates_to_cancel duplicate
WHERE instance.checklist_instance_id = duplicate.checklist_instance_id
  AND instance.status = 'PENDING'
  AND instance.completed_at IS NULL
  AND instance.cancelled_at IS NULL
  AND instance.cancellation_reason_code IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM public.checklist_task_instances task
      WHERE task.checklist_instance_id = instance.checklist_instance_id
        AND task.status <> 'CANCELLED');

INSERT INTO public.audit_events
    (actor_user_id, event_category, subject_user_id,
     resource_type, resource_id, purpose, decision,
     occurred_at, created_at, event_origin, payload,
     correlation_id, severity, status,
     actor_type, actor_service, reason_code,
     care_context_type, care_context_id, template_version_id,
     checklist_task_instance_id, before_payload_jsonb, after_payload_jsonb)
SELECT
    NULL,
    'CHECKLIST_CANCELLED',
    parent.recipient_user_id,
    'CHECKLIST_TASK_INSTANCE',
    task.checklist_task_instance_id,
    'LEGACY_PERSONAL_CHECKLIST_DEDUPLICATION',
    'CANCELLED',
    transaction_timestamp(),
    transaction_timestamp(),
    'CHECKLIST_MIGRATION',
    jsonb_build_object('reasonCode', 'LEGACY_PERSONAL_DUPLICATE', 'metadata', 'REDACTED'),
    duplicate.correlation_id,
    'MEDIUM',
    'CLOSED',
    'SERVICE',
    'CHECKLIST_LEGACY_PERSONAL_DEDUP',
    'LEGACY_PERSONAL_DUPLICATE',
    parent.care_context_type,
    parent.care_context_id,
    parent.template_version_id,
    task.checklist_task_instance_id,
    jsonb_build_object('status', 'PENDING'),
    jsonb_build_object('status', 'CANCELLED', 'reasonCode', 'LEGACY_PERSONAL_DUPLICATE')
FROM checklist_legacy_personal_duplicates_to_cancel duplicate
JOIN public.checklist_instances parent
  ON parent.checklist_instance_id = duplicate.checklist_instance_id
JOIN public.checklist_task_instances task
  ON task.checklist_instance_id = parent.checklist_instance_id
 AND task.status = 'CANCELLED'
 AND task.action_reason_code = 'LEGACY_PERSONAL_DUPLICATE';

INSERT INTO public.audit_events
    (actor_user_id, event_category, subject_user_id,
     resource_type, resource_id, purpose, decision,
     occurred_at, created_at, event_origin, payload,
     correlation_id, severity, status,
     actor_type, actor_service, reason_code,
     care_context_type, care_context_id, template_version_id,
     before_payload_jsonb, after_payload_jsonb)
SELECT
    NULL,
    'CHECKLIST_CANCELLED',
    instance.recipient_user_id,
    'CHECKLIST_INSTANCE',
    instance.checklist_instance_id,
    'LEGACY_PERSONAL_CHECKLIST_DEDUPLICATION',
    'CANCELLED',
    transaction_timestamp(),
    transaction_timestamp(),
    'CHECKLIST_MIGRATION',
    jsonb_build_object('reasonCode', 'LEGACY_PERSONAL_DUPLICATE', 'metadata', 'REDACTED'),
    duplicate.correlation_id,
    'MEDIUM',
    'CLOSED',
    'SERVICE',
    'CHECKLIST_LEGACY_PERSONAL_DEDUP',
    'LEGACY_PERSONAL_DUPLICATE',
    instance.care_context_type,
    instance.care_context_id,
    instance.template_version_id,
    jsonb_build_object('status', 'PENDING'),
    jsonb_build_object('status', 'CANCELLED', 'reasonCode', 'LEGACY_PERSONAL_DUPLICATE')
FROM checklist_legacy_personal_duplicates_to_cancel duplicate
JOIN public.checklist_instances instance
  ON instance.checklist_instance_id = duplicate.checklist_instance_id
WHERE instance.status = 'CANCELLED'
  AND instance.cancellation_reason_code = 'LEGACY_PERSONAL_DUPLICATE';
-- END CANONICAL SOURCE: V20260731010000__deduplicate_legacy_personal_mother_checklists.sql

-- BEGIN CANONICAL SOURCE: V20260731020000__prepare_checklist_schema_simplification.sql
-- Expand checklist template metadata for the staged LEGACY -> DUAL -> REQUEST cutover.
-- This migration is additive: legacy metadata tables remain authoritative and no table is retired.

ALTER TABLE public.care_item_templates
ADD COLUMN recipient_scope varchar(10),
ADD COLUMN eligibility_anchor_type varchar(30),
ADD COLUMN eligibility_range_unit varchar(10),
ADD COLUMN eligibility_start_inclusive integer,
ADD COLUMN eligibility_end_inclusive integer;

ALTER TABLE public.care_item_templates
ADD CONSTRAINT care_item_templates_recipient_scope_domain_ck CHECK (
    recipient_scope IS NULL
    OR recipient_scope IN ('MOTHER', 'FAMILY', 'BOTH')
),
ADD CONSTRAINT care_item_templates_eligibility_anchor_domain_ck CHECK (
    eligibility_anchor_type IS NULL
    OR eligibility_anchor_type IN (
        'NONE',
        'LMP',
        'EDD',
        'DELIVERY_DATE',
        'BIRTH_DATE'
    )
),
ADD CONSTRAINT care_item_templates_eligibility_unit_domain_ck CHECK (
    eligibility_range_unit IS NULL
    OR eligibility_range_unit IN ('DAY', 'WEEK', 'MONTH')
),
ADD CONSTRAINT care_item_templates_eligibility_range_domain_ck CHECK (
    (
        eligibility_start_inclusive IS NULL
        AND eligibility_end_inclusive IS NULL
    )
    OR (
        eligibility_start_inclusive >= 0
        AND eligibility_end_inclusive >= eligibility_start_inclusive
    )
);


WITH
    role_scope AS (
        SELECT
            role.template_version_id,
            CASE
                WHEN bool_or(
                    role.recipient_role = 'MOTHER'
                )
                AND bool_or(
                    role.recipient_role = 'FAMILY'
                ) THEN 'BOTH'
                WHEN bool_or(
                    role.recipient_role = 'MOTHER'
                ) THEN 'MOTHER'
                WHEN bool_or(
                    role.recipient_role = 'FAMILY'
                ) THEN 'FAMILY'
            END AS recipient_scope
        FROM public.checklist_template_recipient_roles role
        GROUP BY
            role.template_version_id
    )
UPDATE public.care_item_templates root
SET
    recipient_scope = role_scope.recipient_scope
FROM role_scope
WHERE
    root.entry_type = 'TEMPLATE_ROOT'
    AND root.template_version_id = role_scope.template_version_id;

UPDATE public.care_item_templates root
SET
    eligibility_anchor_type = substage.anchor_type,
    eligibility_range_unit = substage.range_unit,
    eligibility_start_inclusive = substage.start_inclusive,
    eligibility_end_inclusive = substage.end_inclusive
FROM public.checklist_substages substage
WHERE
    root.entry_type = 'TEMPLATE_ROOT'
    AND root.substage_id = substage.substage_id;

UPDATE public.care_item_templates item
SET
    recipient_scope = NULL,
    eligibility_anchor_type = NULL,
    eligibility_range_unit = NULL,
    eligibility_start_inclusive = NULL,
    eligibility_end_inclusive = NULL
WHERE
    item.entry_type <> 'TEMPLATE_ROOT';
-- END CANONICAL SOURCE: V20260731020000__prepare_checklist_schema_simplification.sql

-- BEGIN CANONICAL SOURCE: V20260731030000__retire_checklist_support_tables.sql
-- Checklist three-table retirement. This migration is intentionally non-reversible.
-- Privileged finalized targets must run checklist_retirement_pre_finalizer.sql first.

DO $$
DECLARE
    missing_tables text;
BEGIN
    SELECT string_agg(required.name, ', ' ORDER BY required.name)
      INTO missing_tables
      FROM (VALUES
          ('checklist_reconciliation_candidates'),
          ('checklist_reconciliation_runs'),
          ('checklist_distribution_outbox'),
          ('checklist_migration_quarantine'),
          ('checklist_care_group_contexts'),
          ('checklist_context_authorities'),
          ('checklist_template_version_items'),
          ('checklist_template_recipient_roles'),
          ('checklist_substages')) AS required(name)
     WHERE to_regclass('public.' || required.name) IS NULL;
    IF missing_tables IS NOT NULL THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_REQUIRED_TABLE_MISSING:%', missing_tables;
    END IF;
END $$;

-- Writers are frozen by operator attestation before a finalized live upgrade.
-- Acquire every retirement lock in one fixed order before inspecting mutable state;
-- Flyway's PostgreSQL transaction holds these locks through the final DROP TABLE.
SET LOCAL lock_timeout = '30s';

LOCK TABLE public.checklist_reconciliation_runs IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_reconciliation_candidates IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_distribution_outbox IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_migration_quarantine IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_care_group_contexts IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_context_authorities IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_template_version_items IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_template_recipient_roles IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_substages IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.care_item_templates IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.mother_journeys IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.care_subjects IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.care_groups IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_instances IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_task_instances IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_action_commands IN ACCESS EXCLUSIVE MODE;

DO $$
DECLARE
    purge_owner name;
    purge_definition text;
BEGIN

    IF EXISTS (SELECT 1 FROM public.checklist_migration_quarantine WHERE legal_hold) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_LEGAL_HOLD_PRESENT';
    END IF;

    IF (SELECT count(*) FROM public.checklist_migration_quarantine WHERE NOT legal_hold) > 6 THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_QUARANTINE_LIMIT_EXCEEDED';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.checklist_distribution_outbox
        WHERE processed_at IS NULL) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_OUTBOX_NOT_DRAINED';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.checklist_reconciliation_runs
        WHERE status = 'RUNNING') THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_RECONCILIATION_RUN_ACTIVE';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.checklist_reconciliation_candidates
        WHERE outcome = 'PENDING') THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_RECONCILIATION_CANDIDATE_PENDING';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.care_item_templates root
         WHERE root.entry_type = 'TEMPLATE_ROOT'
           AND root.recipient_scope IS DISTINCT FROM (
               SELECT CASE
                   WHEN bool_or(role.recipient_role = 'MOTHER')
                        AND bool_or(role.recipient_role = 'FAMILY') THEN 'BOTH'
                   WHEN bool_or(role.recipient_role = 'MOTHER') THEN 'MOTHER'
                   WHEN bool_or(role.recipient_role = 'FAMILY') THEN 'FAMILY'
                   ELSE NULL
               END
               FROM public.checklist_template_recipient_roles role
               WHERE role.template_version_id = root.template_version_id)) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_RECIPIENT_SCOPE_MISMATCH';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.care_item_templates root
          JOIN public.checklist_substages substage ON substage.substage_id = root.substage_id
         WHERE root.entry_type = 'TEMPLATE_ROOT'
           AND (root.eligibility_anchor_type, root.eligibility_range_unit,
                root.eligibility_start_inclusive, root.eligibility_end_inclusive)
               IS DISTINCT FROM
               (substage.anchor_type, substage.range_unit,
                substage.start_inclusive, substage.end_inclusive)) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_ELIGIBILITY_MISMATCH';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.checklist_task_instances task
          JOIN public.checklist_instances parent
            ON parent.checklist_instance_id = task.checklist_instance_id
          LEFT JOIN public.care_item_templates root
            ON root.template_version_id = task.template_version_id
           AND root.entry_type = 'TEMPLATE_ROOT'
          LEFT JOIN public.care_item_templates item
            ON item.template_id = task.template_item_version_id
           AND item.entry_type = 'CHECKLIST_ENTRY'
         WHERE parent.origin = 'SYSTEM_TEMPLATE'
           AND (root.template_id IS NULL OR item.template_id IS NULL
                OR item.parent_template_id IS DISTINCT FROM root.template_id
                OR parent.template_version_id IS DISTINCT FROM task.template_version_id)) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_TEMPLATE_ITEM_MISMATCH';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.checklist_instances instance
         WHERE (instance.care_context_type = 'JOURNEY' AND NOT EXISTS (
                   SELECT 1 FROM public.mother_journeys journey
                    WHERE journey.journey_id = instance.care_context_id
                      AND journey.owner_user_id = instance.context_owner_user_id))
            OR (instance.care_context_type = 'BABY' AND NOT EXISTS (
                   SELECT 1 FROM public.care_subjects baby
                    WHERE baby.care_subject_id = instance.care_context_id
                      AND baby.owner_user_id = instance.context_owner_user_id
                      AND baby.subject_type = 'BABY'))) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_DIRECT_CONTEXT_MISMATCH';
    END IF;

    SELECT owner_role.rolname, pg_get_functiondef(routine.oid)
      INTO purge_owner, purge_definition
      FROM pg_catalog.pg_proc routine
      JOIN pg_catalog.pg_namespace namespace ON namespace.oid = routine.pronamespace
      JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
     WHERE namespace.nspname = 'public'
       AND routine.proname = 'checklist_purge_retained_records'
       AND pg_get_function_identity_arguments(routine.oid) = 'p_actor_user_id uuid';
    IF purge_owner IS NULL THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PURGE_FUNCTION_MISSING';
    END IF;
    IF purge_owner <> current_user
       AND purge_definition NOT LIKE '%CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1%' THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PRE_FINALIZER_REQUIRED';
    END IF;
    IF (SELECT owner_role.rolname
          FROM pg_catalog.pg_class relation
          JOIN pg_catalog.pg_namespace namespace ON namespace.oid = relation.relnamespace
          JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = relation.relowner
         WHERE namespace.nspname = 'public'
           AND relation.relname = 'checklist_migration_quarantine') <> current_user THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PRE_FINALIZER_REQUIRED';
    END IF;

    IF (SELECT count(*)
          FROM (VALUES
              ('checklist_action_command_retention_guard_trg',
               'public.checklist_action_command_retention_guard()', 11::smallint),
              ('checklist_validate_action_command_target_trg',
               'public.checklist_validate_action_command_target()', 7::smallint)
          ) AS expected(trigger_name, function_signature, trigger_type)
          JOIN pg_catalog.pg_trigger trigger_entry
            ON trigger_entry.tgname = expected.trigger_name
           AND trigger_entry.tgrelid = 'public.checklist_action_commands'::regclass
           AND trigger_entry.tgfoid = to_regprocedure(expected.function_signature)
           AND trigger_entry.tgtype = expected.trigger_type
           AND trigger_entry.tgenabled = 'O'
           AND trigger_entry.tgisinternal = false
           AND trigger_entry.tgqual IS NULL) <> 2 THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_ACTION_LEDGER_GUARD_MISSING';
    END IF;
END $$;

-- On a clean Flyway chain the migration role still owns the routine. Finalized
-- targets arrive with the action-only body already installed by the privileged pre-finalizer.
DO $retirement$
DECLARE
    purge_owner name;
BEGIN
    SELECT owner_role.rolname INTO purge_owner
      FROM pg_catalog.pg_proc routine
      JOIN pg_catalog.pg_namespace namespace ON namespace.oid = routine.pronamespace
      JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
     WHERE namespace.nspname = 'public'
       AND routine.proname = 'checklist_purge_retained_records'
       AND pg_get_function_identity_arguments(routine.oid) = 'p_actor_user_id uuid';
    IF purge_owner = current_user
       AND NOT EXISTS (
           SELECT 1
           FROM pg_catalog.pg_auth_members membership
           JOIN pg_catalog.pg_roles owner_role
             ON owner_role.oid = membership.roleid
           WHERE owner_role.rolname = 'carebridge_checklist_retention_owner'
       ) THEN
        -- On managed Supabase, the existing function owner can be protected
        -- from CREATE OR REPLACE by the platform-owned membership. The body
        -- created above is already canonical, so leave it untouched there.
        EXECUTE $function$
            CREATE OR REPLACE FUNCTION public.checklist_purge_retained_records(p_actor_user_id uuid)
            RETURNS TABLE (
                audit_events_purged bigint,
                quarantines_purged bigint,
                action_commands_purged bigint)
            LANGUAGE plpgsql SECURITY DEFINER
            SET search_path = pg_catalog, public
            AS $body$
            DECLARE
                retirement_marker constant text := 'CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1';
            BEGIN
                IF session_user <> 'checklist_operations' THEN
                    RAISE EXCEPTION 'PURGE_DATABASE_CALLER_NOT_TRUSTED' USING ERRCODE = '42501';
                END IF;
                IF EXISTS (
                    SELECT 1 FROM pg_catalog.pg_auth_members membership
                    JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = membership.roleid
                    WHERE owner_role.rolname = 'carebridge_checklist_retention_owner') THEN
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
            $body$
        $function$;
        REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM PUBLIC;
        REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM CURRENT_USER;
    END IF;
END
$retirement$;

-- Replace mirrored authorization with canonical-domain constraints and validation.
ALTER TABLE public.care_groups
    ADD COLUMN linked_baby_subject_type varchar(30)
        GENERATED ALWAYS AS (CASE WHEN linked_baby_profile_id IS NULL THEN NULL ELSE 'BABY' END) STORED,
    ADD CONSTRAINT care_groups_linked_journey_owner_fk
        FOREIGN KEY (linked_journey_id, owner_user_id)
        REFERENCES public.mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT,
    ADD CONSTRAINT care_groups_linked_baby_owner_fk
        FOREIGN KEY (linked_baby_profile_id, owner_user_id, linked_baby_subject_type)
        REFERENCES public.care_subjects(care_subject_id, owner_user_id, subject_type) ON DELETE RESTRICT;

ALTER TABLE public.checklist_instances
    ADD COLUMN journey_context_id uuid GENERATED ALWAYS AS
        (CASE WHEN care_context_type = 'JOURNEY' THEN care_context_id END) STORED,
    ADD COLUMN baby_context_id uuid GENERATED ALWAYS AS
        (CASE WHEN care_context_type = 'BABY' THEN care_context_id END) STORED,
    ADD COLUMN baby_context_subject_type varchar(30) GENERATED ALWAYS AS
        (CASE WHEN care_context_type = 'BABY' THEN 'BABY' END) STORED,
    DROP CONSTRAINT checklist_instances_template_recipient_role_fk,
    DROP CONSTRAINT checklist_instances_context_authority_fk,
    DROP CONSTRAINT checklist_instances_personal_context_authority_fk,
    ADD CONSTRAINT checklist_instances_journey_owner_fk
        FOREIGN KEY (journey_context_id, context_owner_user_id)
        REFERENCES public.mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT,
    ADD CONSTRAINT checklist_instances_baby_owner_fk
        FOREIGN KEY (baby_context_id, context_owner_user_id, baby_context_subject_type)
        REFERENCES public.care_subjects(care_subject_id, owner_user_id, subject_type) ON DELETE RESTRICT;

CREATE OR REPLACE FUNCTION public.checklist_validate_instance_recipient()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.recipient_role = 'MOTHER' THEN
        IF NEW.care_group_id IS NOT NULL
           OR NEW.recipient_user_id IS DISTINCT FROM NEW.context_owner_user_id THEN
            RAISE EXCEPTION 'CHECKLIST_MOTHER_RECIPIENT_NOT_AUTHORIZED';
        END IF;
    ELSE
        PERFORM 1
          FROM public.care_groups care_group
          JOIN public.care_group_members member
            ON member.care_group_id = care_group.care_group_id
         WHERE care_group.care_group_id = NEW.care_group_id
           AND care_group.owner_user_id = NEW.context_owner_user_id
           AND care_group.status = 'ACTIVE'
           AND ((NEW.care_context_type = 'JOURNEY'
                 AND care_group.linked_journey_id = NEW.care_context_id)
                OR (NEW.care_context_type = 'BABY'
                    AND care_group.linked_baby_profile_id = NEW.care_context_id))
           AND member.user_id = NEW.recipient_user_id
           AND member.member_role <> 'OWNER'
           AND member.invitation_status = 'ACCEPTED'
           AND jsonb_typeof(member.permission_json) = 'object'
           AND jsonb_typeof(member.permission_json->'CHECKLIST_VIEW') = 'boolean'
           AND member.permission_json->>'CHECKLIST_VIEW' = 'true'
         FOR KEY SHARE OF care_group, member;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'CHECKLIST_FAMILY_RECIPIENT_NOT_AUTHORIZED';
        END IF;
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER checklist_validate_instance_recipient_trg ON public.checklist_instances;
CREATE TRIGGER checklist_validate_instance_recipient_trg
BEFORE INSERT OR UPDATE OF recipient_role, recipient_user_id, care_group_id,
    care_context_type, care_context_id, context_owner_user_id
ON public.checklist_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_instance_recipient();

ALTER TABLE public.checklist_task_instances
    DROP CONSTRAINT checklist_task_instances_version_item_fk,
    ADD CONSTRAINT checklist_task_instances_template_version_fk
        FOREIGN KEY (template_version_id)
        REFERENCES public.care_item_templates(template_version_id) ON DELETE RESTRICT,
    ADD CONSTRAINT checklist_task_instances_template_item_fk
        FOREIGN KEY (template_item_version_id)
        REFERENCES public.care_item_templates(template_id) ON DELETE RESTRICT;

CREATE OR REPLACE FUNCTION public.checklist_validate_task_template()
RETURNS trigger LANGUAGE plpgsql AS $$
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
END $$;

-- Inline metadata is now the sole template authority.
ALTER TABLE public.care_item_templates DROP CONSTRAINT care_item_templates_substage_fk;
DROP TRIGGER checklist_guard_referenced_substage_mutation_trg ON public.checklist_substages;
DROP TRIGGER checklist_guard_approved_role_mutation_trg ON public.checklist_template_recipient_roles;
DROP TRIGGER checklist_guard_version_item_authority_mutation_trg ON public.checklist_template_version_items;
DROP TRIGGER checklist_sync_template_version_item_write_trg ON public.care_item_templates;
DROP TRIGGER checklist_sync_template_version_item_delete_trg ON public.care_item_templates;

CREATE OR REPLACE FUNCTION public.checklist_validate_inline_template_shape()
RETURNS trigger LANGUAGE plpgsql AS $$
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
           OR (NEW.stage = 'POSTPARTUM' AND NEW.eligibility_anchor_type = 'DELIVERY_DATE')
           OR (NEW.stage = 'BABY_CARE' AND NEW.eligibility_anchor_type = 'BIRTH_DATE')) THEN
        RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER checklist_validate_inline_template_shape_trg
AFTER INSERT OR UPDATE OF entry_type, stage, content_status, distribution_enabled,
    migration_review_required, recipient_scope, eligibility_anchor_type,
    eligibility_range_unit, eligibility_start_inclusive, eligibility_end_inclusive
ON public.care_item_templates DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_inline_template_shape();

DROP TRIGGER checklist_validate_template_approval_trg ON public.care_item_templates;
CREATE OR REPLACE FUNCTION public.checklist_validate_template_approval()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' THEN RETURN NEW; END IF;
    IF (NEW.distribution_enabled OR NEW.content_status = 'APPROVED')
       AND NEW.migration_review_required THEN RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED'; END IF;
    IF NEW.distribution_enabled OR NEW.content_status = 'APPROVED' THEN
        IF NEW.recipient_scope IS NULL THEN RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED'; END IF;
        IF EXISTS (
            SELECT 1 FROM public.care_item_templates item
             WHERE item.parent_template_id = NEW.template_id
               AND item.entry_type = 'CHECKLIST_ENTRY' AND item.is_active
               AND item.target_subject IS NULL) THEN
            RAISE EXCEPTION 'ITEM_TARGET_REQUIRED';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER checklist_validate_template_approval_trg
BEFORE INSERT OR UPDATE OF content_status, distribution_enabled, migration_review_required,
    stage, recipient_scope, eligibility_anchor_type, eligibility_range_unit,
    eligibility_start_inclusive, eligibility_end_inclusive, template_version_id
ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_template_approval();

-- Stop every writer/maintainer for retired mirrors.
DROP TRIGGER checklist_sync_journey_context_authority_trg ON public.mother_journeys;
DROP TRIGGER checklist_sync_baby_context_authority_trg ON public.care_subjects;
DROP TRIGGER checklist_sync_reviewed_care_group_contexts_trg ON public.care_groups;
DROP TRIGGER checklist_block_previous_care_group_context_trg ON public.care_groups;

-- Quarantine security dependencies are named explicitly before table retirement.
DROP TRIGGER checklist_quarantine_forensic_guard_trg ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_insert_policy ON public.checklist_migration_quarantine;
DROP POLICY IF EXISTS checklist_migration_quarantine_operations_select ON public.checklist_migration_quarantine;
DROP POLICY IF EXISTS checklist_migration_quarantine_operations_update ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_application_select ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_application_resolve ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_retention_owner_delete ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_retention_owner_select ON public.checklist_migration_quarantine;
REVOKE ALL ON public.checklist_migration_quarantine FROM PUBLIC, carebridge_application,
    checklist_operations, carebridge_checklist_retention_owner;

DROP INDEX public.checklist_migration_quarantine_correlation_ix;
DROP INDEX public.checklist_quarantine_open_group_context_uk;
DROP INDEX public.checklist_migration_quarantine_retention_ix;
DROP INDEX public.checklist_distribution_outbox_pending_ix;
DROP INDEX public.checklist_distribution_outbox_correlation_ix;
DROP INDEX public.checklist_distribution_outbox_retry_ix;
DROP INDEX public.checklist_distribution_outbox_exhausted_ix;
DROP INDEX public.checklist_reconciliation_success_watermark_ix;
DROP INDEX public.checklist_care_group_contexts_single_active_type_ux;

-- Retired-table functions are dropped only after all calling triggers/functions are replaced.
-- The CHK-039 verifier embeds quarantine regclass references in its PL/pgSQL body;
-- retire it explicitly before the table disappears so no broken callable remains.
DROP FUNCTION IF EXISTS public.checklist_assert_retention_security();
DROP FUNCTION public.checklist_quarantine_forensic_guard();
DROP FUNCTION public.checklist_quarantine_invalid_group_context(uuid, varchar, uuid, varchar);
DROP FUNCTION public.checklist_sync_reviewed_care_group_contexts();
DROP FUNCTION public.checklist_block_previous_care_group_context();
DROP FUNCTION public.checklist_sync_context_authority();
DROP FUNCTION public.checklist_sync_template_version_item();
DROP FUNCTION public.checklist_guard_referenced_substage_mutation();
DROP FUNCTION public.checklist_guard_approved_role_mutation();
DROP FUNCTION public.checklist_guard_version_item_authority_mutation();

-- Exact approved dependency order. Never add CASCADE here.
DROP TABLE public.checklist_reconciliation_candidates;
DROP TABLE public.checklist_reconciliation_runs;
DROP TABLE public.checklist_distribution_outbox;
DROP TABLE public.checklist_migration_quarantine;
DROP TABLE public.checklist_care_group_contexts;
DROP TABLE public.checklist_context_authorities;
DROP TABLE public.checklist_template_version_items;
DROP TABLE public.checklist_template_recipient_roles;
DROP TABLE public.checklist_substages;
-- END CANONICAL SOURCE: V20260731030000__retire_checklist_support_tables.sql

-- BEGIN CANONICAL SOURCE: V20260731040000__add_health_metric_definitions.sql
CREATE TABLE public.health_metric_definitions (
    metric_definition_id uuid DEFAULT gen_random_uuid() NOT NULL,
    metric_code varchar(60) NOT NULL,
    version integer NOT NULL,
    display_name varchar(120) NOT NULL,
    observation_shape varchar(30) NOT NULL,
    subject_type varchar(30) DEFAULT 'MOTHER' NOT NULL,
    manual_entry_supported boolean DEFAULT false NOT NULL,
    device_import_supported boolean DEFAULT false NOT NULL,
    canonical_unit varchar(30),
    accepted_input_units_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    precision_scale smallint,
    required_context_schema_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    plausibility_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    aggregation_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    chart_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    quality_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    safety_policy_version varchar(40),
    allowed_journey_stages_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    effective_from timestamptz DEFAULT now() NOT NULL,
    effective_until timestamptz,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT health_metric_definitions_pk PRIMARY KEY (metric_definition_id),
    CONSTRAINT health_metric_definitions_code_version_uk UNIQUE (metric_code, version),
    CONSTRAINT health_metric_definitions_code_ck CHECK (btrim(metric_code) <> ''),
    CONSTRAINT health_metric_definitions_display_name_ck CHECK (btrim(display_name) <> ''),
    CONSTRAINT health_metric_definitions_version_ck CHECK (version > 0),
    CONSTRAINT health_metric_definitions_shape_ck CHECK (observation_shape IN (
        'POINT', 'PAIRED_POINT', 'SESSION', 'INTERVAL_AGGREGATE'
    )),
    CONSTRAINT health_metric_definitions_subject_ck CHECK (subject_type = 'MOTHER'),
    CONSTRAINT health_metric_definitions_precision_ck CHECK (
        precision_scale IS NULL OR precision_scale >= 0
    ),
    CONSTRAINT health_metric_definitions_effective_period_ck CHECK (
        effective_until IS NULL OR effective_until > effective_from
    ),
    CONSTRAINT health_metric_definitions_units_json_ck CHECK (
        jsonb_typeof(accepted_input_units_jsonb) = 'array'
    ),
    CONSTRAINT health_metric_definitions_context_json_ck CHECK (
        jsonb_typeof(required_context_schema_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_plausibility_json_ck CHECK (
        jsonb_typeof(plausibility_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_aggregation_json_ck CHECK (
        jsonb_typeof(aggregation_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_chart_json_ck CHECK (
        jsonb_typeof(chart_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_quality_json_ck CHECK (
        jsonb_typeof(quality_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_stages_json_ck CHECK (
        jsonb_typeof(allowed_journey_stages_jsonb) = 'array'
    )
);

CREATE UNIQUE INDEX health_metric_definitions_active_code_uk
    ON public.health_metric_definitions(metric_code)
    WHERE is_active = true;

CREATE INDEX health_metric_definitions_active_display_ix
    ON public.health_metric_definitions(is_active, display_name);

CREATE INDEX health_metric_definitions_effective_period_ix
    ON public.health_metric_definitions(effective_from, effective_until);

INSERT INTO public.health_metric_definitions (
    metric_code,
    version,
    display_name,
    observation_shape,
    manual_entry_supported,
    device_import_supported,
    canonical_unit,
    accepted_input_units_jsonb,
    precision_scale,
    required_context_schema_jsonb,
    aggregation_policy_jsonb,
    chart_policy_jsonb,
    quality_policy_jsonb,
    allowed_journey_stages_jsonb
)
VALUES
    (
        'WEIGHT', 1, 'Cân nặng', 'POINT', true, false, 'kg',
        '["kg", "lb"]'::jsonb, 2, '{}'::jsonb,
        '{"method":"REPRESENTATIVE_DAILY_POINT","baselineAware":true}'::jsonb,
        '{"type":"LINE","xAxis":"GESTATIONAL_WEEK_OR_DATE"}'::jsonb,
        '{"required":false}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'BLOOD_PRESSURE', 1, 'Huyết áp', 'PAIRED_POINT', true, true, 'mmHg',
        '["mmHg"]'::jsonb, 0,
        '{"required":["systolic","diastolic"]}'::jsonb,
        '{"method":"NONE"}'::jsonb,
        '{"type":"DUAL_LINE","series":["systolic","diastolic"]}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'BLOOD_GLUCOSE', 1, 'Đường huyết', 'POINT', true, false, 'mg/dL',
        '["mg/dL", "mmol/L"]'::jsonb, 2,
        '{"required":["measurementContext"],"measurementContextValues":["FASTING","PRE_MEAL","POST_MEAL_1H","POST_MEAL_2H","RANDOM","OTHER_APPROVED"]}'::jsonb,
        '{"method":"PARTITION_BY_CONTEXT"}'::jsonb,
        '{"type":"CONTEXT_SERIES","partitionBy":"measurementContext"}'::jsonb,
        '{"required":false}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'FETAL_MOVEMENT_SESSION', 1, 'Cử động thai', 'SESSION', true, false, 'count',
        '["count"]'::jsonb, 0,
        '{"required":["periodStart","periodEnd","protocolCode","completionStatus","gestationalAgeSnapshot"]}'::jsonb,
        '{"method":"SESSION_HISTORY"}'::jsonb,
        '{"type":"SESSION_TIMELINE"}'::jsonb,
        '{"required":false}'::jsonb,
        '["PREGNANCY"]'::jsonb
    ),
    (
        'MATERNAL_HEART_RATE', 1, 'Nhịp tim mẹ', 'POINT', true, true, 'bpm',
        '["bpm"]'::jsonb, 0,
        '{"required":["measurementState"],"measurementStateValues":["RESTING","ACTIVE","POST_EXERCISE","UNKNOWN"]}'::jsonb,
        '{"method":"PARTITION_BY_CONTEXT","partitionBy":"measurementState"}'::jsonb,
        '{"type":"CONTEXT_SERIES","partitionBy":"measurementState"}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'SLEEP_SESSION', 1, 'Giấc ngủ', 'INTERVAL_AGGREGATE', false, true, 'min',
        '["min", "h"]'::jsonb, 2,
        '{"required":["periodStart","periodEnd","timeZone","sleepType"]}'::jsonb,
        '{"method":"MERGE_PROVIDER_INTERVALS","requiresCompleteness":true}'::jsonb,
        '{"type":"INTERVAL_TIMELINE","showDataGaps":true}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'STEPS', 1, 'Số bước', 'INTERVAL_AGGREGATE', false, true, 'count',
        '["count"]'::jsonb, 0,
        '{"required":["periodStart","periodEnd","timeZone","aggregationLevel"]}'::jsonb,
        '{"method":"DAILY_TOTAL_AFTER_DEDUPLICATION","requiresCompleteness":true}'::jsonb,
        '{"type":"DAILY_BAR","showPartialDay":true}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'SPO2', 1, 'SpO2', 'POINT', false, true, '%',
        '["%"]'::jsonb, 2,
        '{"required":[]}'::jsonb,
        '{"method":"QUALITY_FILTERED_SERIES"}'::jsonb,
        '{"type":"LINE","showDataGaps":true,"showQuality":true}'::jsonb,
        '{"requiredForDevice":true,"allowedLabels":["VALID","LOW_QUALITY","INCOMPLETE","UNKNOWN","REJECTED"]}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'TEMPERATURE', 1, 'Nhiệt độ', 'POINT', true, true, 'Cel',
        '["Cel", "°C", "°F"]'::jsonb, 2,
        '{"required":["measurementSite"]}'::jsonb,
        '{"method":"SAME_METHOD_TIME_SERIES"}'::jsonb,
        '{"type":"METHOD_SERIES","partitionBy":"measurementSite"}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    );
-- END CANONICAL SOURCE: V20260731040000__add_health_metric_definitions.sql

-- BEGIN CANONICAL SOURCE: V20260731050000__extend_health_observations_for_p0_metrics.sql
ALTER TABLE public.health_observations
    ADD COLUMN IF NOT EXISTS period_start timestamptz,
    ADD COLUMN IF NOT EXISTS period_end timestamptz,
    ADD COLUMN IF NOT EXISTS context_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    ADD COLUMN IF NOT EXISTS original_unit varchar(30),
    ADD COLUMN IF NOT EXISTS definition_version integer,
    ADD COLUMN IF NOT EXISTS observation_shape varchar(30);

ALTER TABLE public.health_observations
    ADD CONSTRAINT health_observations_p0_period_ck CHECK (
        period_end IS NULL OR (period_start IS NOT NULL AND period_end > period_start)
    );

ALTER TABLE public.health_observations
    ADD CONSTRAINT health_observations_p0_context_json_ck CHECK (
        jsonb_typeof(context_jsonb) = 'object'
    );

CREATE INDEX IF NOT EXISTS health_observations_p0_subject_metric_time_ix
    ON public.health_observations(care_subject_id, observation_type, observed_at)
    WHERE legacy_source = 'maternal_health_observations';
-- END CANONICAL SOURCE: V20260731050000__extend_health_observations_for_p0_metrics.sql
