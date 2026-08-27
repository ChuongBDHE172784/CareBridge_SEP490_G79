-- Invitation expiry controls whether a PENDING invitation can be accepted.
-- Once membership is ACCEPTED, the original invitation timestamp must not revoke access.
CREATE OR REPLACE FUNCTION public.checklist_validate_instance_recipient()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.recipient_role = 'FAMILY' THEN
        PERFORM 1
        FROM public.care_group_members member
        WHERE member.care_group_id = NEW.care_group_id
          AND member.user_id = NEW.recipient_user_id
          AND member.invitation_status = 'ACCEPTED'
          AND jsonb_typeof(member.permission_json) = 'object'
          AND CASE
              WHEN member.permission_json ? 'CHECKLIST_VIEW' THEN
                  jsonb_typeof(member.permission_json->'CHECKLIST_VIEW') = 'boolean'
                  AND member.permission_json->>'CHECKLIST_VIEW' = 'true'
              WHEN member.permission_json ? 'checklist_view' THEN
                  jsonb_typeof(member.permission_json->'checklist_view') = 'boolean'
                  AND member.permission_json->>'checklist_view' = 'true'
              ELSE false
          END
        FOR KEY SHARE;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'CHECKLIST_FAMILY_RECIPIENT_NOT_AUTHORIZED';
        END IF;
    END IF;
    RETURN NEW;
END $$;
