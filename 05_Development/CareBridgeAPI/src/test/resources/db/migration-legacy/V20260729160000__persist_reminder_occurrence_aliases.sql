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
