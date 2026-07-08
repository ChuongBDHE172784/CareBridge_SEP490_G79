-- UC-102 Warn or Suspend Account (CB-MOD-IMP-004 ADR-001, Option D)
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS suspended_until timestamptz NULL;

COMMENT ON COLUMN public.users.suspended_until IS
    'Moderation-driven time-bound account suspension (UC-102). NULL = not suspended. '
    'Non-null = suspended until this timestamp. Decoupled from locked/locked_at '
    '(security-domain brute-force lockout — see UC-102 TDS ADR-001) and from enabled '
    '(permanent account disable/deactivation).';
