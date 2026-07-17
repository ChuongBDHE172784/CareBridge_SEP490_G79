DO $$
DECLARE
    current_definition text;
    current_expression text;
BEGIN
    SELECT pg_get_constraintdef(oid)
      INTO current_definition
      FROM pg_constraint
     WHERE conrelid = 'public.audit_logs'::regclass
       AND conname = 'audit_logs_action_check';

    IF current_definition IS NULL THEN
        RAISE EXCEPTION 'audit_logs_action_check constraint is missing';
    END IF;

    -- Preserve every action allowed by the latest applied migration and append
    -- the federated actions without duplicating the long historical allow-list.
    current_expression := substring(
            current_definition
            FROM 8
            FOR char_length(current_definition) - 8);

    ALTER TABLE public.audit_logs
        DROP CONSTRAINT audit_logs_action_check;

    EXECUTE format(
            'ALTER TABLE public.audit_logs ADD CONSTRAINT audit_logs_action_check '
            'CHECK ((%s) OR action IN (''FEDERATED_LOGIN'', '
            '''FEDERATED_REGISTRATION'', ''FEDERATED_IDENTITY_LINKED''))',
            current_expression);
END
$$;
