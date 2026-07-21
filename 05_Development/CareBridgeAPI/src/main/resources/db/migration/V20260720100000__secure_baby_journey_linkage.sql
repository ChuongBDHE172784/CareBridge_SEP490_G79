CREATE TABLE baby_link_submissions (
    link_submission_id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL REFERENCES users(user_id),
    operation_type varchar(30) NOT NULL CHECK (operation_type IN ('CREATE_WITH_LINK','LINK_EXISTING')),
    submission_id uuid NOT NULL,
    semantic_intent varchar(1000) NOT NULL,
    baby_id uuid NOT NULL REFERENCES baby_profiles(baby_id),
    journey_id uuid NOT NULL REFERENCES mother_journeys(journey_id),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_baby_link_submission UNIQUE(owner_user_id, operation_type, submission_id)
);

CREATE INDEX idx_baby_link_submissions_baby ON baby_link_submissions(baby_id);

DO $$
DECLARE current_definition text; current_expression text;
BEGIN
  SELECT pg_get_constraintdef(oid) INTO current_definition FROM pg_constraint
   WHERE conrelid='public.audit_logs'::regclass AND conname='audit_logs_action_check';
  IF current_definition IS NULL THEN RAISE EXCEPTION 'audit_logs_action_check constraint is missing'; END IF;
  current_expression := substring(current_definition FROM 8 FOR char_length(current_definition)-8);
  ALTER TABLE public.audit_logs DROP CONSTRAINT audit_logs_action_check;
  EXECUTE format('ALTER TABLE public.audit_logs ADD CONSTRAINT audit_logs_action_check CHECK ((%s) OR action IN (''BABY_JOURNEY_LINK_ACCEPTED'',''BABY_JOURNEY_LINK_REJECTED''))', current_expression);
END $$;
