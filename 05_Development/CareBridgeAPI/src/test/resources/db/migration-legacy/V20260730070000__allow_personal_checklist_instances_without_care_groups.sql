-- A mother's checklist is owned by its canonical JOURNEY/BABY context.
-- A care group is optional and is retained only for FAMILY sharing.
ALTER TABLE public.checklist_instances
    ALTER COLUMN care_group_id DROP NOT NULL;

ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instances_personal_context_authority_fk
        FOREIGN KEY (care_context_type, care_context_id, context_owner_user_id)
        REFERENCES public.checklist_context_authorities
            (care_context_type, care_context_id, owner_user_id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT checklist_instances_family_group_scope_ck
        CHECK (recipient_role <> 'FAMILY' OR care_group_id IS NOT NULL);

-- The original trigger only validated INSERT. Re-check authorization whenever
-- an existing instance is moved to another recipient or group scope as well.
DROP TRIGGER IF EXISTS checklist_validate_instance_recipient_trg
    ON public.checklist_instances;

CREATE TRIGGER checklist_validate_instance_recipient_trg
BEFORE INSERT OR UPDATE OF recipient_role, recipient_user_id, care_group_id
ON public.checklist_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_instance_recipient();
