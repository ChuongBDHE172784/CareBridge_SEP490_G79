-- Convert the legacy short-code specialty catalog to the approved UUID catalog
-- before Phase 2 creates the professional-specialties relation.
DO $specialty_cutover$
BEGIN
    IF to_regclass('public.specialties') IS NULL
       OR EXISTS (
           SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'specialties'
              AND column_name = 'specialty_id' AND data_type = 'uuid'
       ) THEN
        RETURN;
    END IF;

    CREATE TABLE public.specialties_canonical (
        specialty_id uuid PRIMARY KEY,
        code varchar(80) NOT NULL UNIQUE,
        name varchar(150) NOT NULL,
        description text,
        is_active boolean NOT NULL DEFAULT true,
        created_at timestamptz NOT NULL DEFAULT now()
    );

    INSERT INTO public.specialties_canonical (
        specialty_id, code, name, description, is_active, created_at
    )
    SELECT (
               substr(md5('specialty-code:' || specialty_id), 1, 8) || '-' ||
               substr(md5('specialty-code:' || specialty_id), 9, 4) || '-' ||
               substr(md5('specialty-code:' || specialty_id), 13, 4) || '-' ||
               substr(md5('specialty-code:' || specialty_id), 17, 4) || '-' ||
               substr(md5('specialty-code:' || specialty_id), 21, 12)
           )::uuid,
           specialty_id, name, description, is_active, now()
      FROM public.specialties;

    ALTER TABLE public.expert_profiles
        DROP CONSTRAINT IF EXISTS fk_expert_profile_specialty;
    ALTER TABLE public.expert_profiles DROP COLUMN IF EXISTS specialty_id;
    DROP TABLE public.specialties;
    ALTER TABLE public.specialties_canonical RENAME TO specialties;
END
$specialty_cutover$;
