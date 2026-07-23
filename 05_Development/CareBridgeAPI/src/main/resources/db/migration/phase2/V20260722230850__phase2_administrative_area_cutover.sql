-- Consolidate legacy province/district catalogs into the approved hierarchical
-- administrative-area catalog without changing the target table count.
DO $administrative_area_cutover$
BEGIN
IF to_regclass('public.provinces') IS NULL
   AND to_regclass('public.districts') IS NULL THEN
    RETURN;
END IF;
IF to_regclass('public.provinces') IS NULL
   OR to_regclass('public.districts') IS NULL THEN
    RAISE EXCEPTION 'ADMINISTRATIVE_AREA_CUTOVER: partial legacy geography schema';
END IF;

INSERT INTO public.administrative_areas (
    administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at
)
SELECT (
           substr(md5('province:' || province_id), 1, 8) || '-' ||
           substr(md5('province:' || province_id), 9, 4) || '-' ||
           substr(md5('province:' || province_id), 13, 4) || '-' ||
           substr(md5('province:' || province_id), 17, 4) || '-' ||
           substr(md5('province:' || province_id), 21, 12)
       )::uuid,
       NULL, 'PROVINCE', 'PROVINCE:' || province_id, name, province_id, now()
  FROM public.provinces
ON CONFLICT (code) DO UPDATE SET
    name = excluded.name, legacy_code = excluded.legacy_code;

INSERT INTO public.administrative_areas (
    administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at
)
SELECT (
           substr(md5('district:' || district_id), 1, 8) || '-' ||
           substr(md5('district:' || district_id), 9, 4) || '-' ||
           substr(md5('district:' || district_id), 13, 4) || '-' ||
           substr(md5('district:' || district_id), 17, 4) || '-' ||
           substr(md5('district:' || district_id), 21, 12)
       )::uuid,
       p.administrative_area_id, 'DISTRICT', 'DISTRICT:' || d.district_id,
       d.name, d.district_id, now()
  FROM public.districts d
  JOIN public.administrative_areas p
    ON p.code = 'PROVINCE:' || d.province_id
ON CONFLICT (code) DO UPDATE SET
    parent_area_id = excluded.parent_area_id,
    name = excluded.name,
    legacy_code = excluded.legacy_code;

UPDATE public.care_facilities f
   SET administrative_area_id = coalesce(
       (SELECT d.administrative_area_id
          FROM public.administrative_areas d
         WHERE d.code = 'DISTRICT:' || f.district_id),
       (SELECT p.administrative_area_id
          FROM public.administrative_areas p
         WHERE p.code = 'PROVINCE:' || f.province_id)
   )
 WHERE f.administrative_area_id IS NULL
   AND (f.district_id IS NOT NULL OR f.province_id IS NOT NULL);

ALTER TABLE public.care_facilities
    DROP CONSTRAINT IF EXISTS care_facilities_district_id_fkey;
ALTER TABLE public.care_facilities
    DROP CONSTRAINT IF EXISTS care_facilities_province_id_fkey;
ALTER TABLE public.districts
    DROP CONSTRAINT IF EXISTS districts_province_id_fkey;

IF EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE contype = 'f'
           AND confrelid IN (
               'public.provinces'::regclass,
               'public.districts'::regclass
           )
    ) THEN
    RAISE EXCEPTION 'ADMINISTRATIVE_AREA_CUTOVER: retained inbound foreign key';
END IF;

DROP TABLE public.districts;
DROP TABLE public.provinces;
END
$administrative_area_cutover$;
