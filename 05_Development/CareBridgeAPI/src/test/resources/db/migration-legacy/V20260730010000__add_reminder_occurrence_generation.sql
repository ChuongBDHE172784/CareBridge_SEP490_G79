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
