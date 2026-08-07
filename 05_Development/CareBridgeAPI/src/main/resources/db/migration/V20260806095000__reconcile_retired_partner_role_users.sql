-- CareBridge database consolidation — R3b reconciliation (runs before the Partner contract)
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.3
-- Code: Database_Consolidation_Source_Code_Refactor_Plan.md §4.4 (data-first gate)
--
-- The deployed code no longer has Role.PARTNER, and V20260806100000 rebuilds
-- users_role_check without it. Any row still holding 'PARTNER' would fail JPA/JWT
-- deserialization at request time and would abort that migration's gate, so the
-- remap has to happen first — this is the "data release before the enum release"
-- that plan §4.4 requires.
--
-- Measured on the linked project on 2026-08-06: exactly one such row, the dev seed
-- account partner@carebridge.dev ("Partner Test"), with 0 live auth sessions and 0
-- active device tokens. DevDataSeeder no longer creates it.
--
-- V3 §3.3 forbids defaulting a **real** user's role to NULL. A seeded demo account
-- with no owner is the carve-out that rule exists for: it is deactivated outright
-- and its role cleared, rather than being silently promoted into another role where
-- it would keep working.

DO $$
DECLARE
    v_row record;
    v_count bigint := 0;
    v_real_users bigint;
BEGIN
    -- Refuse to touch anything that looks like a genuine account. A Partner user
    -- with a real login history is a business decision, not a migration's call.
    SELECT count(*) INTO v_real_users
    FROM public.users u
    WHERE u.role = 'PARTNER'
      AND u.email IS DISTINCT FROM 'partner@carebridge.dev';

    IF v_real_users > 0 THEN
        RAISE EXCEPTION
            'R3B_REAL_PARTNER_USERS: % user(s) hold role PARTNER and are not the dev seed account; decide their target role before running this',
            v_real_users;
    END IF;

    FOR v_row IN
        SELECT user_id, email, full_name, enabled, account_status, created_at
          FROM public.users WHERE role = 'PARTNER' ORDER BY created_at
    LOOP
        v_count := v_count + 1;
        RAISE NOTICE
            'R3B_PARTNER_USER_RETIRED id=% email=% name=% enabled=% status=% created=%',
            v_row.user_id, v_row.email, v_row.full_name, v_row.enabled,
            v_row.account_status, v_row.created_at;
    END LOOP;

    RAISE NOTICE 'R3B_PARTNER_USER_RETIRED_TOTAL=%', v_count;
END
$$;

-- Revoke every credential first, so nothing can authenticate as the account
-- between this statement and the role change.
UPDATE public.auth_sessions s
   SET revoked_at = now(), status = 'REVOKED'
  FROM public.users u
 WHERE u.user_id = s.user_id AND u.role = 'PARTNER' AND s.revoked_at IS NULL;

UPDATE public.device_tokens d
   SET active = false, updated_at = now()
  FROM public.users u
 WHERE u.user_id = d.user_id AND u.role = 'PARTNER' AND d.active;

UPDATE public.users
   SET role = NULL,
       enabled = false,
       account_status = 'DEACTIVATED',
       deactivated_at = COALESCE(deactivated_at, now()),
       deactivation_reason = COALESCE(deactivation_reason,
           'Partner programme retired (consolidation R3b)')
 WHERE role = 'PARTNER';

DO $$
DECLARE
    v_remaining bigint;
BEGIN
    SELECT count(*) INTO v_remaining FROM public.users WHERE role = 'PARTNER';
    IF v_remaining > 0 THEN
        RAISE EXCEPTION 'R3B_RECONCILE_INCOMPLETE: % user(s) still hold role PARTNER', v_remaining;
    END IF;
END
$$;
