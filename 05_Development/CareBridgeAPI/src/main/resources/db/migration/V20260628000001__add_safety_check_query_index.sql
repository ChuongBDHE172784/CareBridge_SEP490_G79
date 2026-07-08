-- UC178: index supporting "latest safety check for (exercise, user)" lookup.
CREATE INDEX IF NOT EXISTS idx_safety_checks_exercise_user_created
    ON public.exercise_safety_checks (exercise_id, user_id, created_at DESC);
