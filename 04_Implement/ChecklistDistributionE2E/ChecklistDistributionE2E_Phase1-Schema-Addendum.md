# Checklist Distribution E2E — Phase 1 Schema Addendum

**Status:** Approved implementation addendum — 2026-07-29  
**Active migration:** `V20260731060000__canonical_post_20260719180000_schema.sql`  
**Historical source migration:** `V20260729030000__add_checklist_distribution_v2_foundation.sql` (test fixture only under `db/migration-legacy`)  
**Policy:** the active path is one atomic forward migration. The SQL snapshot below documents the original additive Phase 1 contract; it is not a second runtime migration and must not be copied back into the main Flyway directory.

The canonical migration creates the required support catalog only long enough to backfill, repair, quarantine, and enforce the reviewed retirement gates, then removes the nine approved support tables. The retained checklist catalog is `checklist_instances`, `checklist_task_instances`, and `checklist_action_commands`; the two manual retirement finalizers remain operator-run files beside the canonical migration.

## 1. Existing-schema binding

- Template roots and checklist entries remain in `care_item_templates`, distinguished by `entry_type`.
- Existing `template_id` remains the row identity. Template roots receive an explicit stable `template_lineage_id` and `template_version_id`; checklist items retain their existing row ID as the item-version identity.
- Existing recipient work in `preparation_checklist_items` remains authoritative until the compatibility switch. This migration does not copy it into V2 task tables.
- `care_groups`, `care_group_members`, `mother_journeys`, `care_subjects`, `users` and `audit_events` remain their current authorities.

## 2. Normative enums

| Concept | Values |
|---|---|
| Recipient role | `MOTHER`, `FAMILY` |
| Target subject | `MOTHER`, `BABY` |
| Care context | `JOURNEY`, `BABY` |
| Origin | `SYSTEM_TEMPLATE`, `USER_CREATED` |
| Template review | `UNREVIEWED`, `REVIEWED`, `BLOCKED` |
| Instance status | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| Task status | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `SKIPPED`, `CANCELLED` |
| Anchor | `NONE`, `LMP`, `EDD`, `DELIVERY_DATE`, `BIRTH_DATE` |
| Range unit | `DAY`, `WEEK`, `MONTH` |
| Reconciliation outcome | `PENDING`, `CREATED`, `EXISTING`, `CANCELLED`, `FAILED`, `QUARANTINED` |

## 3. Exact Flyway SQL

### 3.0 Approved hardening overrides — 2026-07-29

The reviewed migration file is authoritative where it differs from the original SQL snapshot below. The following corrections were approved after the three-layer Phase 1 review:

- Quarantine changes the legacy entry type only after expanding `care_item_templates_type_ck`; invalid parent/root/stage rows are never guessed as MOTHER.
- Legacy substages use a broad `0..2147483647` catch-all range and remain review-required/distribution-disabled.
- Context and template-version-item authority tables are maintained by database triggers for post-migration writes.
- SYSTEM_TEMPLATE instances enforce both lineage/version and declared recipient role; FAMILY inserts lock and revalidate accepted membership plus CHECKLIST_VIEW.
- One correlation may legitimately group multiple outbox or quarantine records, so correlation uses a normal index rather than global uniqueness.
- Quarantine source content is omitted when managed encryption material is unavailable. The stored payload is an opaque random redaction marker with a matching integrity hash; source ID and controlled reason remain operations-only. It must not be presented as recoverable ciphertext.
- Literal `NONE` is an explicit lifecycle-neutral window. `<ABSENT>` is a separate USER_CREATED/unavailable token.
- Permission backfill preserves valid existing checklist booleans, normalizes malformed/non-object JSON to default deny, and never overwrites an explicit grant.
- Review/approval, terminal timestamp, retry/counter, audit shape and task/template integrity constraints in the migration supersede the earlier minimal snapshot.

```sql
-- Checklist Distribution V2 foundation. Additive only.

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

ALTER TABLE public.care_item_templates
    ADD COLUMN template_lineage_id uuid,
    ADD COLUMN template_version_id uuid,
    ADD COLUMN substage_id uuid,
    ADD COLUMN target_subject varchar(10),
    ADD COLUMN migration_review_required boolean DEFAULT false NOT NULL,
    ADD COLUMN distribution_enabled boolean DEFAULT false NOT NULL,
    ADD COLUMN approved_at timestamptz,
    ADD COLUMN approved_by uuid;

UPDATE public.care_item_templates
SET template_lineage_id = template_id,
    template_version_id = template_id,
    migration_review_required = true,
    distribution_enabled = false
WHERE entry_type = 'TEMPLATE_ROOT';

UPDATE public.care_item_templates item
SET target_subject = CASE
    WHEN root.stage = 'BABY_CARE' THEN 'BABY'
    ELSE 'MOTHER'
END
FROM public.care_item_templates root
WHERE item.entry_type = 'CHECKLIST_ENTRY'
  AND item.parent_template_id = root.template_id;

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_version_id_uk UNIQUE (template_version_id),
    ADD CONSTRAINT care_item_templates_substage_fk FOREIGN KEY (substage_id)
        REFERENCES public.checklist_substages(substage_id) ON DELETE RESTRICT,
    ADD CONSTRAINT care_item_templates_target_ck CHECK (
        (entry_type <> 'CHECKLIST_ENTRY') OR target_subject IN ('MOTHER','BABY')
    ),
    ADD CONSTRAINT care_item_templates_root_version_ck CHECK (
        (entry_type <> 'TEMPLATE_ROOT') OR
        (template_lineage_id IS NOT NULL AND template_version_id IS NOT NULL)
    );

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
SELECT template_version_id, 'MOTHER'
FROM public.care_item_templates
WHERE entry_type = 'TEMPLATE_ROOT'
ON CONFLICT DO NOTHING;

ALTER TABLE public.care_groups
    ADD CONSTRAINT care_groups_id_owner_uk UNIQUE (care_group_id, owner_user_id);

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
    CONSTRAINT checklist_care_group_contexts_group_context_uk UNIQUE
        (care_group_id, care_context_type, care_context_id),
    CONSTRAINT checklist_care_group_contexts_authority_uk UNIQUE
        (care_group_id, care_context_type, care_context_id, owner_user_id),
    CONSTRAINT checklist_care_group_contexts_type_ck CHECK (care_context_type IN ('JOURNEY','BABY')),
    CONSTRAINT checklist_care_group_contexts_review_ck CHECK (review_status IN ('UNREVIEWED','REVIEWED','BLOCKED')),
    CONSTRAINT checklist_care_group_contexts_group_owner_fk FOREIGN KEY (care_group_id, owner_user_id)
        REFERENCES public.care_groups(care_group_id, owner_user_id) ON DELETE CASCADE
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
    CONSTRAINT checklist_instances_window_ck CHECK (
        (window_start IS NULL AND window_end IS NULL) OR
        (window_start IS NOT NULL AND window_end IS NOT NULL AND window_end >= window_start)
    ),
    CONSTRAINT checklist_instances_template_pair_ck CHECK (
        (origin = 'SYSTEM_TEMPLATE' AND template_lineage_id IS NOT NULL AND template_version_id IS NOT NULL) OR
        (origin = 'USER_CREATED' AND template_lineage_id IS NULL AND template_version_id IS NULL)
    ),
    CONSTRAINT checklist_instances_recipient_fk FOREIGN KEY (recipient_user_id)
        REFERENCES public.users(user_id) ON DELETE RESTRICT,
    CONSTRAINT checklist_instances_template_version_fk FOREIGN KEY (template_version_id)
        REFERENCES public.care_item_templates(template_version_id) ON DELETE RESTRICT,
    CONSTRAINT checklist_instances_context_authority_fk FOREIGN KEY
        (care_group_id, care_context_type, care_context_id, context_owner_user_id)
        REFERENCES public.checklist_care_group_contexts
        (care_group_id, care_context_type, care_context_id, owner_user_id) ON DELETE RESTRICT
);

CREATE INDEX checklist_instances_recipient_status_ix
    ON public.checklist_instances(recipient_user_id, status, created_at DESC);
CREATE INDEX checklist_instances_context_status_ix
    ON public.checklist_instances(care_group_id, care_context_type, care_context_id, status);

CREATE TABLE public.checklist_task_instances (
    checklist_task_instance_id uuid DEFAULT gen_random_uuid() NOT NULL,
    checklist_instance_id uuid NOT NULL,
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
    CONSTRAINT checklist_task_instances_parent_fk FOREIGN KEY (checklist_instance_id)
        REFERENCES public.checklist_instances(checklist_instance_id) ON DELETE CASCADE,
    CONSTRAINT checklist_task_instances_item_fk FOREIGN KEY (template_item_version_id)
        REFERENCES public.care_item_templates(template_id) ON DELETE RESTRICT
);

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
    CONSTRAINT checklist_action_commands_actor_fk FOREIGN KEY (actor_user_id)
        REFERENCES public.users(user_id) ON DELETE RESTRICT
);

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
    CONSTRAINT checklist_distribution_outbox_correlation_uk UNIQUE (correlation_id)
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
    CONSTRAINT checklist_reconciliation_runs_status_ck CHECK (status IN ('RUNNING','SUCCEEDED','PARTIAL','FAILED'))
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
    CONSTRAINT checklist_reconciliation_candidates_run_fk FOREIGN KEY (reconciliation_run_id)
        REFERENCES public.checklist_reconciliation_runs(reconciliation_run_id) ON DELETE CASCADE
);

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
    CONSTRAINT checklist_migration_quarantine_hash_ck CHECK (payload_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT checklist_migration_quarantine_correlation_uk UNIQUE (correlation_id)
);

ALTER TABLE public.care_group_members
    ALTER COLUMN permission_json SET DEFAULT '{}'::jsonb;

UPDATE public.care_group_members
SET permission_json = jsonb_set(
    jsonb_set(COALESCE(permission_json, '{}'::jsonb), '{CHECKLIST_VIEW}', 'false'::jsonb, true),
    '{CHECKLIST_COMPLETE}', 'false'::jsonb, true
);

ALTER TABLE public.audit_events
    ADD COLUMN actor_type varchar(20),
    ADD COLUMN actor_service varchar(80),
    ADD COLUMN reason_code varchar(80),
    ADD COLUMN care_context_type varchar(10),
    ADD COLUMN care_context_id uuid,
    ADD COLUMN template_version_id uuid,
    ADD COLUMN checklist_task_instance_id uuid;

CREATE INDEX audit_events_checklist_correlation_ix
    ON public.audit_events(correlation_id, occurred_at DESC)
    WHERE event_category LIKE 'CHECKLIST_%';
```

## 4. Migration invariants

1. No table/column is dropped; `preparation_checklist_items` is unchanged.
2. Every existing checklist template root receives role MOTHER, review-required true and distribution-disabled false.
3. Every existing checklist entry receives exactly one target derived from the parent stage.
4. Existing members receive explicit false values for both checklist permissions.
5. No V2 instance/task rows are created by this expand migration.
6. Distribution keys and task keys are non-null lowercase SHA-256 values; absence is represented before hashing, never by nullable key components.
7. Context mapping is blocked until service validation proves the journey/baby owner equals the care-group owner.
8. Quarantine ciphertext is never exposed through normal application roles.

## 5. Phase 1 Java package plan

New code is isolated under `com.carebridge.backend.checklist`:

- `entity`: `ChecklistSubstage`, `ChecklistInstance`, `ChecklistTaskInstance`, `ChecklistCareGroupContext`, `ChecklistActionCommand`, `ChecklistReconciliationRun`, `ChecklistReconciliationCandidate`, `ChecklistMigrationQuarantine`.
- `model`: enums matching section 2.
- `repository`: one Spring Data repository per entity; no business logic.
- `key`: versioned canonical serializer and SHA-256 key factory with golden vectors.
- `migration`: read-only verification service for template/role/target/default-deny counts.

Existing content entities receive only additive mappings for columns added to `care_item_templates`; audit and authorization behavior is implemented in later vertical slices after their RED tests exist.

## 6. Verification evidence — 2026-07-29

- RED hardening evidence: 8/8 initial hardening contracts and 3/3 added key cases failed before fixes; subsequent review-specific RED cases reproduced quarantine ordering, missing maintained authorities, stale membership grants, missing checklist permission API fields, incomplete audit shape and invalid window acceptance.
- GREEN focused evidence: 69 tests passed with 0 failures/errors across checklist schema/domain/key/audit/migration verification, Family permission policy/update, and Content Admin template compatibility.
- Build evidence: `mvn -q -DskipTests package` passed after the final hardening changes.
- Full regression was executed but is not green: PostgreSQL/Testcontainers suites cannot start because Docker/psql are unavailable; the known Flyway chain governance test also expects a missing canonical migration; unrelated H2 integration contexts fail during schema initialization.
- The PostgreSQL Flyway exit gate remains pending for this historical 2026-07-29 snapshot only. The active canonical migration is verified separately below.

## 7. Canonical squash evidence — 2026-07-31

`V20260731060000__canonical_post_20260719180000_schema.sql` is the active runtime migration. Clean PostgreSQL 18 replay/validation, retirement, and schema-upgrade evidence is recorded in `spec-squash-flyway-migrations.md`; superseded staged files remain test-only fixtures under `05_Development/CareBridgeAPI/src/test/resources/db/migration-legacy`.

