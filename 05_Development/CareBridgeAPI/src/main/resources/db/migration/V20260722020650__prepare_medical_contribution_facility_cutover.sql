-- Make the legacy facility cutover data-driven. The following audited migration was
-- written for one specific clone (20 hospitals and two linked profiles), so a clean
-- bootstrap must complete the same lossless mapping before reaching that clone gate.
DO $facility_cutover$
BEGIN
    IF to_regclass('public.medical_contributions') IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM public.medical_contributions)
           OR (to_regclass('public.contribution_attachments') IS NOT NULL
               AND EXISTS (SELECT 1 FROM public.contribution_attachments)) THEN
            RAISE EXCEPTION
                'BLOCKED_MEDICAL_CONTRIBUTION_ARCHIVE: live rows require explicit archive mapping';
        END IF;
        DROP TABLE IF EXISTS public.contribution_attachments;
        DROP TABLE public.medical_contributions;
    END IF;

    IF to_regclass('public.hospitals') IS NULL THEN
        RETURN;
    END IF;

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

    INSERT INTO public.care_facilities (
        facility_id, name, facility_type, facility_level, ownership_type,
        address, province_id, district_id, latitude, longitude, phone,
        source_type, external_source_id, verification_status,
        is_active, is_searchable, created_at, updated_at
    )
    SELECT (
               substr(md5('hospital:' || h.hospital_id), 1, 8) || '-' ||
               substr(md5('hospital:' || h.hospital_id), 9, 4) || '-' ||
               substr(md5('hospital:' || h.hospital_id), 13, 4) || '-' ||
               substr(md5('hospital:' || h.hospital_id), 17, 4) || '-' ||
               substr(md5('hospital:' || h.hospital_id), 21, 12)
           )::uuid,
           h.name, 'HOSPITAL', h.level, NULL, h.address, h.province_id,
           h.district_id, NULL, NULL, h.phone, 'LEGACY_IMPORT', h.hospital_id,
           'UNVERIFIED', h.is_active, false, now(), now()
      FROM public.hospitals h
     WHERE NOT EXISTS (
           SELECT 1 FROM public.care_facilities f
            WHERE f.source_type = 'LEGACY_IMPORT'
              AND f.external_source_id = h.hospital_id
     );

    UPDATE public.expert_profiles e
       SET facility_id = f.facility_id
      FROM public.care_facilities f
     WHERE f.source_type = 'LEGACY_IMPORT'
       AND f.external_source_id = e.hospital_id
       AND e.hospital_id IS NOT NULL;

    IF EXISTS (
        SELECT 1 FROM public.expert_profiles
         WHERE hospital_id IS NOT NULL AND facility_id IS NULL
    ) THEN
        RAISE EXCEPTION 'FACILITY_CUTOVER_RECONCILIATION: unmapped expert hospital';
    END IF;

    ALTER TABLE public.expert_profiles
        DROP CONSTRAINT IF EXISTS fk_expert_profile_hospital;
    ALTER TABLE public.expert_profiles DROP COLUMN hospital_id;
    DROP TABLE public.hospitals;
END
$facility_cutover$;
