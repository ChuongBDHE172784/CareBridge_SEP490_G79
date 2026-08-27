ALTER TABLE public.content_items
    ADD COLUMN IF NOT EXISTS summary varchar(150);
