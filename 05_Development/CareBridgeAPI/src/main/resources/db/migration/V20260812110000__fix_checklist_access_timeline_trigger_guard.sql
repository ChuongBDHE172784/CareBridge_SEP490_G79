-- The access-timeline validator is shared by care_group_members and audit_events.
-- PostgreSQL PL/pgSQL does not guarantee short-circuit evaluation of AND
-- expressions, so a reference to NEW.event_category in a combined predicate
-- can be evaluated while NEW is a care_group_members record.  Keep the
-- audit-only field access inside an explicit table branch.

CREATE OR REPLACE FUNCTION public.checklist_assert_access_timeline_audit()
RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_member_id uuid;
    v_member_marker text;
    v_timeline jsonb;
BEGIN
    IF TG_TABLE_NAME = 'audit_events' THEN
        IF NEW.event_category NOT IN ('CHECKLIST_ACCESS_BASELINE','CHECKLIST_ACCESS_REVOKED') THEN
            RETURN NEW;
        END IF;
        v_member_id := NEW.resource_id;
    ELSE
        IF NEW.checklist_access_quarantine_reason_code IS NOT NULL THEN
            RETURN NEW;
        END IF;
        v_member_id := NEW.care_group_member_id;
    END IF;

    SELECT checklist_access_quarantine_reason_code,
           checklist_access_timeline_jsonb
      INTO v_member_marker, v_timeline
      FROM public.care_group_members
     WHERE care_group_member_id = v_member_id;

    IF TG_TABLE_NAME = 'audit_events' AND NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_ACCESS_MEMBER_NOT_FOUND';
    END IF;

    IF TG_TABLE_NAME = 'audit_events' THEN
        IF v_member_marker IS NOT NULL
           AND NOT (v_member_marker = 'FAMILY_MEMBER_DUPLICATE'
                    AND NEW.event_category = 'CHECKLIST_ACCESS_REVOKED') THEN
            RAISE EXCEPTION 'CHECKLIST_ACCESS_AUDIT_ON_QUARANTINED_MEMBER';
        END IF;
        -- Quarantined duplicate revocations are a migration-only exception:
        -- the paired audit is retained as forensic evidence.
        IF v_member_marker = 'FAMILY_MEMBER_DUPLICATE' THEN
            RETURN NEW;
        END IF;
    END IF;

    IF NOT public.checklist_p2_access_timeline_valid(v_member_id, v_timeline) THEN
        RAISE EXCEPTION 'CHECKLIST_ACCESS_TIMELINE_AUDIT_MISMATCH';
    END IF;

    RETURN NEW;
END $$;
