-- V20260723180000 was authored against the legacy checklist table. On a canonical
-- bootstrap, phase 2 has already migrated and removed that table. Preserve the
-- immutable migration checksum by providing a marked, empty bridge only when needed.
DO $checklist_bridge$
BEGIN
    IF to_regclass('public.user_checklist_items') IS NULL THEN
        CREATE TABLE public.user_checklist_items (
            user_checklist_item_id uuid PRIMARY KEY,
            owner_user_id uuid NOT NULL,
            journey_id uuid,
            baby_id uuid,
            template_item_id uuid,
            is_completed boolean NOT NULL DEFAULT false,
            completed_at timestamptz,
            created_at timestamptz NOT NULL DEFAULT now(),
            updated_at timestamptz NOT NULL DEFAULT now()
        );

        COMMENT ON TABLE public.user_checklist_items IS
            'carebridge_transient_bootstrap_bridge_for_v20260723180000';
    ELSIF obj_description(to_regclass('public.user_checklist_items'), 'pg_class') IS DISTINCT FROM
          'carebridge_transient_bootstrap_bridge_for_v20260723180000' THEN
        RAISE EXCEPTION
            'CHECKLIST_BRIDGE_CONFLICT: unexpected public.user_checklist_items relation survived canonical cutover';
    END IF;
END
$checklist_bridge$;
