-- The expert profile records where a specialist works, but only ever as free text in
-- users.workplace. There was no way to say which province that hospital is in, so the
-- hospital picker had nothing to narrow its search by and offered results from the whole
-- country at once.
--
-- One column on the table the expert profile already maps to. No new table: ExpertProfile
-- is @Table(name = "users") with a role = 'EXPERT' restriction, and workplace and
-- facility_id already live here.
--
-- Nullable on purpose. Every expert already on the system has no province recorded, and
-- the field is optional for them; forcing a value would fail the migration and would also
-- misrepresent profiles nobody has revisited yet.
--
-- Typed to match masterdata: ProvinceResponse.provinceId is a String ("1", "4", "8"),
-- not a UUID, so this stores the same identifier the /master-data/provinces list returns.
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS workplace_province_id character varying(16);

COMMENT ON COLUMN public.users.workplace_province_id IS
    'Province of the expert''s workplace, as province_id from /api/v1/master-data/provinces. Scopes the TrackAsia hospital lookup. NULL for experts who have not set one.';
