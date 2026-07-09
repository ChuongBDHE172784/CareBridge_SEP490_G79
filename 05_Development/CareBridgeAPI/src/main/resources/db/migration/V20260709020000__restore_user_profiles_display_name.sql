-- Repair migration: user_profiles.display_name (originally created by V2__spec_sync_from_tds.sql)
-- was found missing from the live database on 2026-07-09 - Hibernate schema validation was failing
-- with "Schema validation: missing column [display_name] in table [user_profiles]", blocking backend
-- startup entirely. Additive/idempotent restore; V2 is intentionally left unmodified per project rules.

ALTER TABLE public.user_profiles
    ADD COLUMN IF NOT EXISTS display_name varchar(100);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_display_name_length'
    ) THEN
        ALTER TABLE public.user_profiles
            ADD CONSTRAINT chk_display_name_length
            CHECK ((display_name IS NULL) OR (length(display_name) >= 2 AND length(display_name) <= 100));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_profiles_display_name_search
    ON public.user_profiles USING btree (display_name text_pattern_ops);
