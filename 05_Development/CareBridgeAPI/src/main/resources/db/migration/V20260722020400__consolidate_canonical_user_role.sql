-- Batch 2: users.role is the only Release 1 persistence model for RBAC.
-- PostgreSQL executes this Flyway migration transactionally. Every preflight runs
-- before backfill/drop so an unsafe legacy mapping leaves both tables untouched.

LOCK TABLE public.users, public.roles, public.user_roles IN ACCESS EXCLUSIVE MODE;

DO $$
DECLARE
    dependency_count bigint;
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.users
         WHERE role IS NOT NULL
           AND role NOT IN (
               'MOTHER', 'FAMILY', 'EXPERT', 'MODERATOR',
               'CONTENT_ADMIN', 'SYSTEM_ADMIN', 'PARTNER'
           )
    ) THEN
        RAISE EXCEPTION 'Canonical role preflight failed: users.role contains an unknown value';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.user_roles ur
          LEFT JOIN public.users u ON u.user_id = ur.user_id
          LEFT JOIN public.roles r ON r.role_id = ur.role_id
          LEFT JOIN public.users assigner ON assigner.user_id = ur.assigned_by
         WHERE ur.user_id IS NULL
            OR ur.role_id IS NULL
            OR u.user_id IS NULL
            OR r.role_id IS NULL
            OR (ur.assigned_by IS NOT NULL AND assigner.user_id IS NULL)
    ) THEN
        RAISE EXCEPTION 'Canonical role preflight failed: orphan or incomplete user_roles mapping';
    END IF;

    -- Any expiry carries temporal authorization semantics that users.role cannot
    -- preserve after user_roles is removed. Require manual reconciliation instead
    -- of silently turning a temporary assignment into a permanent role.
    IF EXISTS (
        SELECT 1
          FROM public.user_roles ur
          JOIN public.roles r ON r.role_id = ur.role_id
         WHERE ur.status <> 'ACTIVE'
            OR ur.expires_at IS NOT NULL
            OR r.is_active IS DISTINCT FROM true
    ) THEN
        RAISE EXCEPTION 'Canonical role preflight failed: inactive, expiring, or historical role mapping requires manual review';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.roles
         WHERE role_code IS NULL
            OR role_code NOT IN (
                'MOTHER', 'FAMILY', 'EXPERT', 'MODERATOR',
                'CONTENT_ADMIN', 'SYSTEM_ADMIN', 'PARTNER'
            )
    ) THEN
        RAISE EXCEPTION 'Canonical role preflight failed: roles contains an unknown role code';
    END IF;

    IF EXISTS (
        SELECT user_id
          FROM public.user_roles
         GROUP BY user_id
        HAVING count(*) <> 1
    ) THEN
        RAISE EXCEPTION 'Canonical role preflight failed: multi-role mapping cannot be represented by users.role';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.user_roles ur
          JOIN public.roles r ON r.role_id = ur.role_id
          JOIN public.users u ON u.user_id = ur.user_id
         WHERE u.role IS NOT NULL
           AND u.role <> r.role_code
    ) THEN
        RAISE EXCEPTION 'Canonical role preflight failed: users.role conflicts with legacy mapping';
    END IF;

    SELECT count(*)
      INTO dependency_count
      FROM pg_constraint c
     WHERE c.contype = 'f'
       AND c.confrelid IN ('public.roles'::regclass, 'public.user_roles'::regclass)
       AND c.conrelid NOT IN ('public.roles'::regclass, 'public.user_roles'::regclass);
    IF dependency_count > 0 THEN
        RAISE EXCEPTION 'Canonical role preflight failed: % external foreign key(s) reference legacy role tables', dependency_count;
    END IF;

    SELECT count(*)
      INTO dependency_count
      FROM pg_rewrite rw
      JOIN pg_class dependent ON dependent.oid = rw.ev_class
      JOIN pg_depend d ON d.objid = rw.oid
     WHERE d.refobjid IN ('public.roles'::regclass, 'public.user_roles'::regclass)
       AND dependent.relkind IN ('v', 'm')
       AND dependent.oid NOT IN ('public.roles'::regclass, 'public.user_roles'::regclass);
    IF dependency_count > 0 THEN
        RAISE EXCEPTION 'Canonical role preflight failed: % view dependency/dependencies reference legacy role tables', dependency_count;
    END IF;

    SELECT count(*)
      INTO dependency_count
      FROM pg_proc p
      JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE p.prokind IN ('f', 'p')
      AND n.nspname NOT LIKE 'pg\_%' ESCAPE '\'
      AND n.nspname <> 'information_schema'
      AND (
          (
              n.nspname = 'public'
              AND pg_get_functiondef(p.oid) ~*
                  '(^|[^a-z0-9_])("?public"?[.])?"?(user_roles|roles)"?([^a-z0-9_]|$)'
          )
          OR (
              n.nspname <> 'public'
              AND pg_get_functiondef(p.oid) ~*
                  '(^|[^a-z0-9_])"?public"?[.]"?(user_roles|roles)"?([^a-z0-9_]|$)'
          )
      );
    IF dependency_count > 0 THEN
        RAISE EXCEPTION 'Canonical role preflight failed: % function/procedure reference(s) require manual review', dependency_count;
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_trigger
         WHERE tgrelid IN ('public.roles'::regclass, 'public.user_roles'::regclass)
           AND NOT tgisinternal
    ) OR EXISTS (
        SELECT 1 FROM pg_policy
         WHERE polrelid IN ('public.roles'::regclass, 'public.user_roles'::regclass)
    ) THEN
        RAISE EXCEPTION 'Canonical role preflight failed: trigger or policy dependency requires manual review';
    END IF;
END
$$;

UPDATE public.users u
   SET role = r.role_code
  FROM public.user_roles ur
  JOIN public.roles r ON r.role_id = ur.role_id
 WHERE ur.user_id = u.user_id
   AND u.role IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.user_roles ur
          JOIN public.roles r ON r.role_id = ur.role_id
          JOIN public.users u ON u.user_id = ur.user_id
         WHERE u.role IS DISTINCT FROM r.role_code
    ) THEN
        RAISE EXCEPTION 'Canonical role reconciliation failed: legacy mapping was not preserved exactly';
    END IF;
END
$$;

ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE public.users
    ADD CONSTRAINT users_role_check CHECK (
        role IS NULL OR role IN (
            'MOTHER', 'FAMILY', 'EXPERT', 'MODERATOR',
            'CONTENT_ADMIN', 'SYSTEM_ADMIN', 'PARTNER'
        )
    );

DO $$
DECLARE
    item record;
BEGIN
    FOR item IN
        SELECT conrelid::regclass AS table_name, conname
          FROM pg_constraint
         WHERE conrelid IN ('public.user_roles'::regclass, 'public.roles'::regclass)
         ORDER BY CASE WHEN conrelid = 'public.user_roles'::regclass THEN 0 ELSE 1 END
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', item.table_name, item.conname);
    END LOOP;

    FOR item IN
        SELECT schemaname, tablename, indexname
          FROM pg_indexes
         WHERE schemaname = 'public'
           AND tablename IN ('user_roles', 'roles')
    LOOP
        EXECUTE format('DROP INDEX %I.%I', item.schemaname, item.indexname);
    END LOOP;
END
$$;

DROP TABLE public.user_roles;
DROP TABLE public.roles;

DO $$
BEGIN
    IF to_regclass('public.user_roles') IS NOT NULL
       OR to_regclass('public.roles') IS NOT NULL THEN
        RAISE EXCEPTION 'Canonical role final verification failed: legacy role table still exists';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.users
         WHERE role IS NOT NULL
           AND role NOT IN (
               'MOTHER', 'FAMILY', 'EXPERT', 'MODERATOR',
               'CONTENT_ADMIN', 'SYSTEM_ADMIN', 'PARTNER'
           )
    ) THEN
        RAISE EXCEPTION 'Canonical role final verification failed: invalid users.role';
    END IF;
END
$$;

COMMENT ON COLUMN public.users.role IS
    'Canonical nullable Release 1 RBAC role: MOTHER, FAMILY, EXPERT, MODERATOR, CONTENT_ADMIN, SYSTEM_ADMIN, or PARTNER.';
