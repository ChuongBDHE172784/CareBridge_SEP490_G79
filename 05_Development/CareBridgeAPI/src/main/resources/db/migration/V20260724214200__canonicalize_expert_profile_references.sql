-- Complete the professional-profile cutover for expert runtime tables.
-- Wave 3 backfilled the canonical key but intentionally left the legacy key in
-- place. Runtime JPA now writes only professional_profile_id, so the legacy
-- NOT NULL columns must be retired after proving both identifiers agree.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

DO $canonical_expert_reference_relation_gate$
DECLARE
    target_table text;
BEGIN
    IF to_regclass('public.professional_profiles') IS NULL THEN
        RAISE EXCEPTION 'CANONICAL_EXPERT_REFERENCE: professional_profiles is missing';
    END IF;

    FOREACH target_table IN ARRAY ARRAY[
        'expert_credentials',
        'expert_availability',
        'expert_location_shares'
    ]
    LOOP
        IF to_regclass('public.' || target_table) IS NULL THEN
            RAISE EXCEPTION 'CANONICAL_EXPERT_REFERENCE: %.% is missing', 'public', target_table;
        END IF;
    END LOOP;
END
$canonical_expert_reference_relation_gate$;

-- Freeze both sides of the reference while the divergence/orphan gates run.
-- SHARE ROW EXCLUSIVE blocks INSERT/UPDATE/DELETE, and lock_timeout keeps a
-- busy deployment fail-closed instead of validating a moving data set.
LOCK TABLE public.professional_profiles IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE public.expert_credentials IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE public.expert_availability IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE public.expert_location_shares IN SHARE ROW EXCLUSIVE MODE;

DO $canonical_expert_reference_data_gate$
DECLARE
    target_table text;
    identifier_column_count integer;
    invalid_reference boolean;
    orphaned_reference boolean;
BEGIN
    IF to_regclass('public.professional_profiles') IS NULL THEN
        RAISE EXCEPTION 'CANONICAL_EXPERT_REFERENCE: professional_profiles is missing';
    END IF;

    FOREACH target_table IN ARRAY ARRAY[
        'expert_credentials',
        'expert_availability',
        'expert_location_shares'
    ]
    LOOP
        IF to_regclass('public.' || target_table) IS NULL THEN
            RAISE EXCEPTION 'CANONICAL_EXPERT_REFERENCE: %.% is missing', 'public', target_table;
        END IF;

        SELECT count(*)
          INTO identifier_column_count
          FROM information_schema.columns c
         WHERE c.table_schema = 'public'
           AND c.table_name = target_table
           AND c.column_name IN ('expert_profile_id', 'professional_profile_id');

        IF identifier_column_count <> 2 THEN
            RAISE EXCEPTION
                'CANONICAL_EXPERT_REFERENCE: % must contain both legacy and canonical identifiers',
                target_table;
        END IF;

        EXECUTE format(
            'SELECT EXISTS ('
            'SELECT 1 FROM public.%I '
            'WHERE expert_profile_id IS NULL '
            'OR professional_profile_id IS NULL '
            'OR expert_profile_id IS DISTINCT FROM professional_profile_id)',
            target_table)
        INTO invalid_reference;

        IF invalid_reference THEN
            RAISE EXCEPTION
                'CANONICAL_EXPERT_REFERENCE: % contains divergent expert identifiers',
                target_table;
        END IF;

        EXECUTE format(
            'SELECT EXISTS ('
            'SELECT 1 FROM public.%I source '
            'LEFT JOIN public.professional_profiles profile '
            'ON profile.professional_profile_id = source.professional_profile_id '
            'WHERE profile.professional_profile_id IS NULL)',
            target_table)
        INTO orphaned_reference;

        IF orphaned_reference THEN
            RAISE EXCEPTION
                'CANONICAL_EXPERT_REFERENCE: % contains orphaned professional_profile_id values',
                target_table;
        END IF;
    END LOOP;
END
$canonical_expert_reference_data_gate$;

DO $canonical_expert_reference_add_fks$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'public.expert_credentials'::regclass
           AND conname = 'expert_credentials_professional_profile_id_fkey'
    ) THEN
        ALTER TABLE public.expert_credentials
            ADD CONSTRAINT expert_credentials_professional_profile_id_fkey
            FOREIGN KEY (professional_profile_id)
            REFERENCES public.professional_profiles(professional_profile_id)
            NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'public.expert_availability'::regclass
           AND conname = 'expert_availability_professional_profile_id_fkey'
    ) THEN
        ALTER TABLE public.expert_availability
            ADD CONSTRAINT expert_availability_professional_profile_id_fkey
            FOREIGN KEY (professional_profile_id)
            REFERENCES public.professional_profiles(professional_profile_id)
            NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'public.expert_location_shares'::regclass
           AND conname = 'expert_location_shares_professional_profile_id_fkey'
    ) THEN
        ALTER TABLE public.expert_location_shares
            ADD CONSTRAINT expert_location_shares_professional_profile_id_fkey
            FOREIGN KEY (professional_profile_id)
            REFERENCES public.professional_profiles(professional_profile_id)
            NOT VALID;
    END IF;
END
$canonical_expert_reference_add_fks$;

ALTER TABLE public.expert_credentials
    VALIDATE CONSTRAINT expert_credentials_professional_profile_id_fkey;
ALTER TABLE public.expert_availability
    VALIDATE CONSTRAINT expert_availability_professional_profile_id_fkey;
ALTER TABLE public.expert_location_shares
    VALIDATE CONSTRAINT expert_location_shares_professional_profile_id_fkey;

DO $canonical_expert_reference_fk_gate$
DECLARE
    target_table text;
    canonical_fk_present boolean;
    unsafe_canonical_fk_present boolean;
BEGIN
    FOREACH target_table IN ARRAY ARRAY[
        'expert_credentials',
        'expert_availability',
        'expert_location_shares'
    ]
    LOOP
        SELECT EXISTS (
            SELECT 1
              FROM pg_constraint constraint_row
             WHERE constraint_row.contype = 'f'
               AND constraint_row.convalidated
               AND constraint_row.confdeltype IN ('a', 'r')
               AND constraint_row.conrelid = to_regclass('public.' || target_table)
               AND constraint_row.confrelid = 'public.professional_profiles'::regclass
               AND constraint_row.conkey = ARRAY[
                   (SELECT attribute.attnum
                      FROM pg_attribute attribute
                     WHERE attribute.attrelid = to_regclass('public.' || target_table)
                       AND attribute.attname = 'professional_profile_id'
                       AND NOT attribute.attisdropped)
               ]::smallint[]
               AND constraint_row.confkey = ARRAY[
                   (SELECT attribute.attnum
                      FROM pg_attribute attribute
                     WHERE attribute.attrelid = 'public.professional_profiles'::regclass
                       AND attribute.attname = 'professional_profile_id'
                       AND NOT attribute.attisdropped)
               ]::smallint[]
        ) INTO canonical_fk_present;

        SELECT EXISTS (
            SELECT 1
              FROM pg_constraint constraint_row
             WHERE constraint_row.contype = 'f'
               AND constraint_row.confdeltype NOT IN ('a', 'r')
               AND constraint_row.conrelid = to_regclass('public.' || target_table)
               AND constraint_row.confrelid = 'public.professional_profiles'::regclass
               AND constraint_row.conkey = ARRAY[
                   (SELECT attribute.attnum
                      FROM pg_attribute attribute
                     WHERE attribute.attrelid = to_regclass('public.' || target_table)
                       AND attribute.attname = 'professional_profile_id'
                       AND NOT attribute.attisdropped)
               ]::smallint[]
               AND constraint_row.confkey = ARRAY[
                   (SELECT attribute.attnum
                      FROM pg_attribute attribute
                     WHERE attribute.attrelid = 'public.professional_profiles'::regclass
                       AND attribute.attname = 'professional_profile_id'
                       AND NOT attribute.attisdropped)
               ]::smallint[]
        ) INTO unsafe_canonical_fk_present;

        IF NOT canonical_fk_present THEN
            RAISE EXCEPTION
                'CANONICAL_EXPERT_REFERENCE: % lacks a validated non-cascading canonical foreign key',
                target_table;
        END IF;

        IF unsafe_canonical_fk_present THEN
            RAISE EXCEPTION
                'CANONICAL_EXPERT_REFERENCE: % contains an unsafe canonical foreign key delete action',
                target_table;
        END IF;
    END LOOP;
END
$canonical_expert_reference_fk_gate$;

-- Deliberately omit CASCADE: an unexpected dependency must stop this cutover.
ALTER TABLE public.expert_credentials DROP COLUMN expert_profile_id;
ALTER TABLE public.expert_availability DROP COLUMN expert_profile_id;
ALTER TABLE public.expert_location_shares DROP COLUMN expert_profile_id;

DO $canonical_expert_reference_final_gate$
DECLARE
    target_table text;
    legacy_column_present boolean;
    canonical_column_required boolean;
BEGIN
    FOREACH target_table IN ARRAY ARRAY[
        'expert_credentials',
        'expert_availability',
        'expert_location_shares'
    ]
    LOOP
        SELECT EXISTS (
            SELECT 1
              FROM information_schema.columns c
             WHERE c.table_schema = 'public'
               AND c.table_name = target_table
               AND c.column_name = 'expert_profile_id'
        ) INTO legacy_column_present;

        SELECT EXISTS (
            SELECT 1
              FROM information_schema.columns c
             WHERE c.table_schema = 'public'
               AND c.table_name = target_table
               AND c.column_name = 'professional_profile_id'
               AND c.is_nullable = 'NO'
        ) INTO canonical_column_required;

        IF legacy_column_present OR NOT canonical_column_required THEN
            RAISE EXCEPTION
                'CANONICAL_EXPERT_REFERENCE: % did not complete the canonical cutover',
                target_table;
        END IF;
    END LOOP;
END
$canonical_expert_reference_final_gate$;
