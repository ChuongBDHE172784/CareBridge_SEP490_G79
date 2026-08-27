-- H2-only canonical override for audit_events (runs AFTER Hibernate ddl-auto, embedded DBs only).
--
-- Why: several entities share @Table("audit_events") — AuditLog, ModerationAction,
-- MotherBaselineContext, MotherJourneyTransition, and PregnancyOutcomeEvidence,
-- while ContributionPoint maps the same physical table as
-- @Table(name = "audit_events", schema = "public"). Hibernate therefore builds TWO
-- in-memory table objects and emits TWO CREATE TABLE statements for the same H2 table;
-- whichever runs first wins and the other fails ("already exists"), leaving the table
-- without the losing entities' columns (e.g. resource_id, before/after_payload_jsonb).
-- Additionally, AuditLog's @Enumerated event_category would win the merged column as a
-- native H2 ENUM, rejecting canonical non-AuditAction categories (BASELINE_CONTEXT,
-- MODERATION_HIDE, EXPERT_CONTRIBUTION, ...).
--
-- This script (spring.sql.init with defer-datasource-initialization=true) rebuilds the
-- table with the exact canonical PostgreSQL shape (see carebridge_local70.audit_events,
-- V20260727010000__canonical_schema_convergence.sql) so every entity family works,
-- exactly like on the real database.
DROP TABLE IF EXISTS audit_events;

CREATE TABLE audit_events (
    audit_event_id uuid NOT NULL DEFAULT random_uuid () PRIMARY KEY,
    actor_user_id uuid,
    actor_type varchar(20),
    actor_service varchar(80),
    event_category varchar(80) NOT NULL,
    subject_user_id uuid,
    subject_reference_id uuid,
    resource_type varchar(100),
    resource_id uuid,
    reason_code varchar(80),
    care_context_type varchar(10),
    care_context_id uuid,
    template_version_id uuid,
    checklist_task_instance_id uuid,
    purpose varchar(255),
    decision varchar(255),
    ip_hash varchar(255),
    before_payload_jsonb jsonb,
    after_payload_jsonb jsonb,
    checksum varchar(255),
    occurred_at timestamp with time zone NOT NULL DEFAULT now(),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    note_text text,
    event_origin varchar(80) NOT NULL DEFAULT 'AUDIT_LOG',
    ip_address varchar(255),
    user_agent varchar(255),
    legal_hold boolean NOT NULL DEFAULT false,
    payload jsonb,
    correlation_id uuid,
    severity varchar(30) NOT NULL DEFAULT 'MEDIUM',
    status varchar(30) NOT NULL DEFAULT 'OPEN',
    reviewed_at timestamp with time zone,
    reviewed_by uuid,
    security_event_id uuid
);