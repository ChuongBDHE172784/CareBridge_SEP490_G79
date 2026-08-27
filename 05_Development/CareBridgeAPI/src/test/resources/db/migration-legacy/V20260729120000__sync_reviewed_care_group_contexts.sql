-- Explicit care-group links are canonical only when the linked context belongs to
-- the same owner. Materialize those verified links so checklist instances can use
-- the composite context-authority foreign key without trusting application input.
CREATE OR REPLACE FUNCTION public.checklist_sync_reviewed_care_group_contexts()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.linked_journey_id IS NOT NULL THEN
        INSERT INTO public.checklist_care_group_contexts
            (care_group_id, owner_user_id, care_context_type, care_context_id,
             review_status, distribution_blocked, reviewed_at, reviewed_by)
        SELECT NEW.care_group_id, NEW.owner_user_id, 'JOURNEY', NEW.linked_journey_id,
               'REVIEWED', false, now(), NEW.owner_user_id
        FROM public.checklist_context_authorities authority
        WHERE authority.care_context_type = 'JOURNEY'
          AND authority.care_context_id = NEW.linked_journey_id
          AND authority.owner_user_id = NEW.owner_user_id
        ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;
    END IF;

    IF NEW.linked_baby_profile_id IS NOT NULL THEN
        INSERT INTO public.checklist_care_group_contexts
            (care_group_id, owner_user_id, care_context_type, care_context_id,
             review_status, distribution_blocked, reviewed_at, reviewed_by)
        SELECT NEW.care_group_id, NEW.owner_user_id, 'BABY', NEW.linked_baby_profile_id,
               'REVIEWED', false, now(), NEW.owner_user_id
        FROM public.checklist_context_authorities authority
        WHERE authority.care_context_type = 'BABY'
          AND authority.care_context_id = NEW.linked_baby_profile_id
          AND authority.owner_user_id = NEW.owner_user_id
        ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER checklist_sync_reviewed_care_group_contexts_trg
AFTER INSERT OR UPDATE OF owner_user_id, linked_journey_id, linked_baby_profile_id
ON public.care_groups
FOR EACH ROW EXECUTE FUNCTION public.checklist_sync_reviewed_care_group_contexts();

INSERT INTO public.checklist_care_group_contexts
    (care_group_id, owner_user_id, care_context_type, care_context_id,
     review_status, distribution_blocked, reviewed_at, reviewed_by)
SELECT group_row.care_group_id, group_row.owner_user_id, 'JOURNEY', group_row.linked_journey_id,
       'REVIEWED', false, now(), group_row.owner_user_id
FROM public.care_groups group_row
JOIN public.checklist_context_authorities authority
  ON authority.care_context_type = 'JOURNEY'
 AND authority.care_context_id = group_row.linked_journey_id
 AND authority.owner_user_id = group_row.owner_user_id
WHERE group_row.linked_journey_id IS NOT NULL
ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;

INSERT INTO public.checklist_care_group_contexts
    (care_group_id, owner_user_id, care_context_type, care_context_id,
     review_status, distribution_blocked, reviewed_at, reviewed_by)
SELECT group_row.care_group_id, group_row.owner_user_id, 'BABY', group_row.linked_baby_profile_id,
       'REVIEWED', false, now(), group_row.owner_user_id
FROM public.care_groups group_row
JOIN public.checklist_context_authorities authority
  ON authority.care_context_type = 'BABY'
 AND authority.care_context_id = group_row.linked_baby_profile_id
 AND authority.owner_user_id = group_row.owner_user_id
WHERE group_row.linked_baby_profile_id IS NOT NULL
ON CONFLICT (care_group_id, care_context_type, care_context_id) DO NOTHING;
