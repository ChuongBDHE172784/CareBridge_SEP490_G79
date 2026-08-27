-- Hibernate includes relationship columns in ordinary template updates even when
-- their values are unchanged. Avoid rewriting the immutable version-item authority
-- rows for those no-op relationship updates (for example, approval metadata changes).
CREATE OR REPLACE FUNCTION public.checklist_sync_template_version_item()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM public.checklist_template_version_items
        WHERE template_item_version_id = OLD.template_id OR template_root_id = OLD.template_id;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE'
       AND NEW.entry_type IS NOT DISTINCT FROM OLD.entry_type
       AND NEW.parent_template_id IS NOT DISTINCT FROM OLD.parent_template_id
       AND NEW.template_version_id IS NOT DISTINCT FROM OLD.template_version_id THEN
        RETURN NEW;
    END IF;

    DELETE FROM public.checklist_template_version_items
    WHERE template_item_version_id = NEW.template_id OR template_root_id = NEW.template_id;

    IF NEW.entry_type = 'CHECKLIST_ENTRY' AND NEW.parent_template_id IS NOT NULL THEN
        INSERT INTO public.checklist_template_version_items
            (template_version_id, template_root_id, template_item_version_id)
        SELECT root.template_version_id, root.template_id, NEW.template_id
        FROM public.care_item_templates root
        WHERE root.template_id = NEW.parent_template_id
          AND root.entry_type = 'TEMPLATE_ROOT'
          AND root.template_version_id IS NOT NULL
        ON CONFLICT DO NOTHING;
    ELSIF NEW.entry_type = 'TEMPLATE_ROOT' AND NEW.template_version_id IS NOT NULL THEN
        INSERT INTO public.checklist_template_version_items
            (template_version_id, template_root_id, template_item_version_id)
        SELECT NEW.template_version_id, NEW.template_id, item.template_id
        FROM public.care_item_templates item
        WHERE item.parent_template_id = NEW.template_id
          AND item.entry_type = 'CHECKLIST_ENTRY'
        ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
END $$;
