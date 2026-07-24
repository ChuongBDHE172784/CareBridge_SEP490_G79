-- Batch 5: make care_facilities the canonical nearby-care persistence model.
-- PostgreSQL transactional DDL makes every fail-fast exception roll back the migration.

ALTER TABLE public.care_facilities
    ADD COLUMN IF NOT EXISTS facility_level varchar(50),
    ADD COLUMN IF NOT EXISTS ownership_type varchar(30),
    ADD COLUMN IF NOT EXISTS province_id varchar(2),
    ADD COLUMN IF NOT EXISTS district_id varchar(4),
    ADD COLUMN IF NOT EXISTS external_source_id varchar(150),
    ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS is_searchable boolean NOT NULL DEFAULT true;

ALTER TABLE public.expert_profiles
    ADD COLUMN IF NOT EXISTS facility_id uuid;

CREATE TABLE public.care_facility_legacy_ids (
    legacy_source varchar(30) NOT NULL,
    legacy_id varchar(100) NOT NULL,
    facility_id uuid NOT NULL,
    migrated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT care_facility_legacy_ids_pkey PRIMARY KEY (legacy_source, legacy_id),
    CONSTRAINT care_facility_legacy_ids_facility_fkey
        FOREIGN KEY (facility_id) REFERENCES public.care_facilities(facility_id)
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT care_facility_legacy_ids_source_check
        CHECK (legacy_source IN ('HOSPITAL'))
);

DO $migration$
DECLARE
    hospital_count bigint;
    mapped_count bigint;
    migrated_expert_count bigint;
    hospital_dependencies text[];
BEGIN
    IF to_regclass('public.hospitals') IS NULL THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
             WHERE table_schema = 'public' AND table_name = 'expert_profiles'
               AND column_name = 'hospital_id'
        ) THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: hospital_id exists without hospitals';
        END IF;
        RETURN;
    END IF;

    LOCK TABLE public.hospitals IN ACCESS EXCLUSIVE MODE;
    LOCK TABLE public.care_facilities IN SHARE ROW EXCLUSIVE MODE;
    LOCK TABLE public.expert_profiles IN SHARE ROW EXCLUSIVE MODE;

    IF NOT (
        SELECT count(*) = 9
          FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = 'hospitals'
           AND (column_name, data_type) IN (
               ('hospital_id', 'character varying'),
               ('name', 'character varying'),
               ('type', 'character varying'),
               ('level', 'character varying'),
               ('address', 'text'),
               ('province_id', 'character varying'),
               ('district_id', 'character varying'),
               ('phone', 'character varying'),
               ('is_active', 'boolean')
           )
    ) OR EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = 'hospitals'
           AND column_name IN ('hospital_id', 'name', 'province_id', 'is_active')
           AND is_nullable <> 'NO'
    ) THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: unexpected hospitals schema';
    END IF;

    SELECT array_agg(dependency ORDER BY dependency)
      INTO hospital_dependencies
      FROM (
          SELECT 'FK:' || conrelid::regclass::text || '.' || conname AS dependency
            FROM pg_constraint
           WHERE confrelid = 'public.hospitals'::regclass AND contype = 'f'
          UNION ALL
          SELECT 'VIEW:' || schemaname || '.' || viewname
            FROM pg_views
           WHERE definition ILIKE '%hospitals%'
          UNION ALL
          SELECT 'MATERIALIZED_VIEW:' || schemaname || '.' || matviewname
            FROM pg_matviews
           WHERE definition ILIKE '%hospitals%'
          UNION ALL
          SELECT 'TRIGGER:' || trigger_name
            FROM information_schema.triggers
           WHERE event_object_schema = 'public' AND event_object_table = 'hospitals'
          UNION ALL
          SELECT 'POLICY:' || policyname FROM pg_policies
           WHERE schemaname = 'public' AND tablename = 'hospitals'
          UNION ALL
          SELECT 'ROUTINE:' || n.nspname || '.' || p.proname
            FROM pg_proc p
            JOIN pg_namespace n ON n.oid = p.pronamespace
           WHERE n.nspname NOT IN ('pg_catalog', 'information_schema')
             AND p.prosrc ILIKE '%hospitals%'
      ) dependencies;

    IF coalesce(hospital_dependencies, ARRAY[]::text[])
       <> ARRAY['FK:expert_profiles.fk_expert_profile_hospital']::text[] THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: unexpected hospitals dependency';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.hospitals
         WHERE type IS NULL OR type NOT IN ('Công lập', 'Quân đội')
            OR level IS DISTINCT FROM 'Hạng I'
    ) THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: unmappable hospital ownership or level';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.hospitals h
        JOIN public.care_facilities f
          ON lower(btrim(h.name)) = lower(btrim(f.name))
         AND lower(btrim(coalesce(h.address, ''))) = lower(btrim(coalesce(f.address, '')))
    ) THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: hospital identity collision';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.hospitals h
        JOIN public.care_facilities f
          ON h.phone IS NOT NULL AND f.phone IS NOT NULL
         AND regexp_replace(h.phone, '[^0-9]', '', 'g') = regexp_replace(f.phone, '[^0-9]', '', 'g')
    ) THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: hospital phone collision';
    END IF;

    SELECT count(*) INTO hospital_count FROM public.hospitals;
    IF hospital_count <> 20
       OR (SELECT count(*) FROM public.expert_profiles WHERE hospital_id IS NOT NULL) <> 2 THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: audited row counts changed';
    END IF;

    INSERT INTO public.care_facility_legacy_ids (legacy_source, legacy_id, facility_id)
    SELECT 'HOSPITAL', h.hospital_id, gen_random_uuid()
      FROM public.hospitals h;

    INSERT INTO public.care_facilities (
        facility_id, name, facility_type, facility_level, ownership_type,
        address, province_id, district_id, latitude, longitude, phone,
        source_type, external_source_id, verification_status,
        is_active, is_searchable, created_at, updated_at
    )
    SELECT m.facility_id, h.name, 'HOSPITAL', h.level,
           CASE h.type WHEN 'Công lập' THEN 'PUBLIC' WHEN 'Quân đội' THEN 'MILITARY' END,
           h.address, h.province_id, h.district_id, NULL, NULL, h.phone,
           'LEGACY_IMPORT', h.hospital_id, 'UNVERIFIED',
           h.is_active, false, now(), now()
      FROM public.hospitals h
      JOIN public.care_facility_legacy_ids m
        ON m.legacy_source = 'HOSPITAL' AND m.legacy_id = h.hospital_id;

    UPDATE public.expert_profiles e
       SET facility_id = m.facility_id
      FROM public.care_facility_legacy_ids m
     WHERE m.legacy_source = 'HOSPITAL'
       AND m.legacy_id = e.hospital_id;

    SELECT count(*) INTO mapped_count
      FROM public.care_facility_legacy_ids WHERE legacy_source = 'HOSPITAL';
    SELECT count(*) INTO migrated_expert_count
      FROM public.expert_profiles WHERE hospital_id IS NOT NULL AND facility_id IS NOT NULL;

    IF mapped_count <> hospital_count OR migrated_expert_count <> 2
       OR EXISTS (SELECT 1 FROM public.expert_profiles WHERE hospital_id IS NOT NULL AND facility_id IS NULL)
       OR EXISTS (
           SELECT 1 FROM public.care_facility_legacy_ids m
           LEFT JOIN public.care_facilities f ON f.facility_id = m.facility_id
           WHERE m.legacy_source = 'HOSPITAL' AND f.facility_id IS NULL
       ) THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: reconciliation mismatch';
    END IF;

    ALTER TABLE public.expert_profiles DROP CONSTRAINT fk_expert_profile_hospital;
    ALTER TABLE public.expert_profiles DROP COLUMN hospital_id;
    DROP TABLE public.hospitals;
END
$migration$;

ALTER TABLE public.expert_profiles
    ADD CONSTRAINT expert_profiles_facility_id_fkey
    FOREIGN KEY (facility_id) REFERENCES public.care_facilities(facility_id);

CREATE INDEX idx_expert_profiles_facility_id
    ON public.expert_profiles(facility_id) WHERE facility_id IS NOT NULL;
CREATE INDEX idx_care_facilities_nearby_eligible
    ON public.care_facilities(facility_type, province_id, district_id)
    WHERE is_active = true AND is_searchable = true
      AND latitude IS NOT NULL AND longitude IS NOT NULL;
CREATE UNIQUE INDEX uq_care_facilities_external_source
    ON public.care_facilities(source_type, external_source_id)
    WHERE external_source_id IS NOT NULL;

ALTER TABLE public.care_facilities
    ADD CONSTRAINT care_facilities_ownership_type_check
        CHECK (ownership_type IS NULL OR ownership_type IN ('PUBLIC', 'MILITARY')),
    ADD CONSTRAINT care_facilities_source_type_check
        CHECK (source_type IS NULL OR source_type IN ('MANUAL', 'TRACKASIA', 'LEGACY_IMPORT')),
    ADD CONSTRAINT care_facilities_searchable_coordinates_check
        CHECK (is_searchable = false OR (latitude IS NOT NULL AND longitude IS NOT NULL));

DO $geography_fks$
BEGIN
    IF to_regclass('public.provinces') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM public.care_facilities f
            LEFT JOIN public.provinces p ON p.province_id = f.province_id
            WHERE f.province_id IS NOT NULL AND p.province_id IS NULL
        ) THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: orphan facility province';
        END IF;
        ALTER TABLE public.care_facilities
            ADD CONSTRAINT care_facilities_province_id_fkey
            FOREIGN KEY (province_id) REFERENCES public.provinces(province_id);
    END IF;
    IF to_regclass('public.districts') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM public.care_facilities f
            LEFT JOIN public.districts d ON d.district_id = f.district_id
            WHERE f.district_id IS NOT NULL AND d.district_id IS NULL
        ) THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: orphan facility district';
        END IF;
        ALTER TABLE public.care_facilities
            ADD CONSTRAINT care_facilities_district_id_fkey
            FOREIGN KEY (district_id) REFERENCES public.districts(district_id);
    END IF;
END
$geography_fks$;

DO $handoff_fk$
BEGIN
    IF to_regclass('public.emergency_map_handoffs') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM public.emergency_map_handoffs h
            LEFT JOIN public.care_facilities f ON f.facility_id = h.selected_facility_id
            WHERE h.selected_facility_id IS NOT NULL AND f.facility_id IS NULL
        ) THEN
            RAISE EXCEPTION 'BLOCKED_PARTIAL_FACILITY_MIGRATION: orphan emergency facility handoff';
        END IF;
        ALTER TABLE public.emergency_map_handoffs
            ADD CONSTRAINT emergency_map_handoffs_selected_facility_fkey
            FOREIGN KEY (selected_facility_id) REFERENCES public.care_facilities(facility_id);
    END IF;
END
$handoff_fk$;
